package com.ajikmeoreosseo.app.story

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * ElevenLabs Text to Speech로 문장을 읽습니다.
 *
 * - 한 번 만든 음성은 기기에 저장해 두고 다시 씁니다(같은 문장은 다시 요금이 들지 않습니다).
 * - 통신이 안 되면 [RETRY_AFTER_MS] 동안은 요청하지 않고 곧바로 실패를 알려,
 *   호출한 쪽이 차량 기본 음성으로 넘어갈 수 있게 합니다.
 * - [configuredVoiceId]가 비어 있으면 계정의 목소리 중 귀여운 쪽을 골라 씁니다.
 */
class ElevenLabsVoice(
    context: Context,
    private val apiKey: String,
    configuredVoiceId: String,
    private val modelId: String,
) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val speakIo = Executors.newSingleThreadExecutor()
    private val prefetchIo = Executors.newSingleThreadExecutor()
    private val cacheDir = File(appContext.cacheDir, "elevenlabs").apply { mkdirs() }
    private val downloadLocks = ConcurrentHashMap<String, Any>()

    private val audioManager = appContext.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    /**
     * 내비 음성 안내처럼 다른 소리가 끼어들면(포커스 일시 상실) 읽던 말을 멈췄다가, 끝나면 그 자리부터 이어 읽습니다.
     * 말 위에 말을 겹치면 아이가 둘 다 못 알아들으므로 소리를 줄이는 대신 멈춥니다(willPauseWhenDucked).
     */
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change -> onFocusChange(change) }
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attributes)
        .setWillPauseWhenDucked(true)
        .setOnAudioFocusChangeListener(focusListener, main)
        .build()

    /** 포커스를 잃어 멈춰 있는 동안 true. 낭독부는 이때 시간 초과로 넘어가지 않습니다. */
    @Volatile
    var pausedByFocus = false
        private set

    /**
     * 포커스를 아주 잃었는지(운전자가 음악·라디오를 틂). 이때는 시스템이 "되찾았다"는 신호를 주지 않으므로,
     * 다른 소리가 멎었는지 [tryRegainFocus]로 직접 살펴 다시 받아야 합니다.
     */
    private var focusLostForGood = false

    private val regainCheck: Runnable = object : Runnable {
        override fun run() {
            if (!pausedByFocus || !focusLostForGood) return
            if (!tryRegainFocus()) main.postDelayed(this, REGAIN_CHECK_MS)
        }
    }

    /** 포커스를 잃은 사이에 시작하려던 재생. 되찾으면 시작합니다. */
    private var startWhenFocusReturns: (() -> Unit)? = null

    /** 다른 소리(내비 안내 등)가 끝나 다시 읽기 시작할 때 호출됩니다. */
    var onInterruptionEnded: (() -> Unit)? = null

    /** 포커스가 바뀌었을 때(또는 시연용으로 흉내 낼 때). 메인 스레드. */
    fun onFocusChange(change: Int) {
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK, AudioManager.AUDIOFOCUS_LOSS -> {
                if (change == AudioManager.AUDIOFOCUS_LOSS) {
                    focusLostForGood = true
                    // 시스템이 되찾았다고 알려 주지 않으므로, 다른 소리가 멎었는지 몇 초마다 직접 살핍니다.
                    main.removeCallbacks(regainCheck)
                    main.postDelayed(regainCheck, REGAIN_CHECK_MS)
                }
                if (pausedByFocus) return
                pausedByFocus = true
                Log.d(TAG, "audio focus lost ($change): pausing")
                player?.let { if (runCatching { it.isPlaying }.getOrDefault(false)) runCatching { it.pause() } }
                cuePlayer?.let { if (runCatching { it.isPlaying }.getOrDefault(false)) runCatching { it.pause() } }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (!pausedByFocus) return
                pausedByFocus = false
                focusLostForGood = false
                Log.d(TAG, "audio focus regained: resuming")
                // 끼어든 소리의 끝과 겹치지 않도록 잠깐 두고 이어 읽습니다.
                main.postDelayed({
                    if (pausedByFocus) return@postDelayed
                    cuePlayer?.let { runCatching { it.start() } }
                    player?.let { if (cuePlayer == null) runCatching { it.start() } }
                    startWhenFocusReturns?.invoke()
                    startWhenFocusReturns = null
                    onInterruptionEnded?.invoke()
                }, RESUME_DELAY_MS)
            }
        }
    }

    @Volatile
    private var voiceId: String? = configuredVoiceId.ifBlank { null }

    /** 자동으로 고른 목소리의 이름. 직접 지정했거나 아직 고르기 전이면 null. */
    @Volatile
    var voiceName: String? = null
        private set

    @Volatile
    private var blockedUntil = 0L

    @Volatile
    private var lastFailure = "연결 실패"

    // 아래는 메인 스레드에서만 만집니다.
    private var player: MediaPlayer? = null
    private var generation = 0

    /** [stop]이 불린 횟수. 끼어든 말은 다음 문장이 준비돼도([generation]이 바뀌어도) 끝까지 말하고, 멈췄을 때만 그만둡니다. */
    private var stops = 0
    private var cuePlayer: MediaPlayer? = null
    private var interjecting = false
    private var startWhenCueEnds: (() -> Unit)? = null

    /** 곧 읽을 문장을 미리 받아 둡니다. 실패해도 조용히 넘어갑니다. */
    fun prefetch(text: String) {
        prefetchIo.execute { runCatching { audioFor(text) } }
    }

    /**
     * [text]를 읽습니다. 끝나면 [onDone], 못 읽으면 [onFailed] 중 하나가 메인 스레드에서 한 번 호출됩니다.
     * [slow]이면 아이가 알아듣기 쉽게 천천히 들려줍니다(영어 낱말용).
     */
    fun speak(text: String, slow: Boolean, onDone: () -> Unit, onFailed: (reason: String) -> Unit) {
        val mine = ++generation
        speakIo.execute {
            val result = runCatching { audioFor(text) }
            main.post {
                if (mine != generation) return@post
                result.fold(
                    onSuccess = { play(it, slow, mine, onDone, onFailed) },
                    onFailure = { onFailed(it.message ?: it.javaClass.simpleName) },
                )
            }
        }
    }

    /**
     * 읽던 말을 잠깐 멈추고 짧은 말들([clips]: 글과 '천천히' 여부)을 바로 말한 뒤, 멈춘 곳부터 이어 읽습니다.
     * 지체 없이 말해야 하므로 모든 음성이 이미 저장돼 있을 때만 합니다.
     * 하나라도 없으면 다음을 위해 받아 두기만 하고 false를 돌려줍니다.
     */
    fun interject(clips: List<Pair<String, Boolean>>, onDone: () -> Unit): Boolean {
        val voice = voiceId
        val files = voice?.let { v -> clips.map { (text, _) -> fileFor(v, text) } }
        if (files == null || files.any { it.length() == 0L }) {
            clips.forEach { (text, _) -> prefetch(text) }
            return false
        }
        if (interjecting) return false
        // 내비가 말하는 중이면 그 위에 외치지 않습니다(엔진이 안내가 끝난 뒤에 말하도록 넘깁니다).
        if (pausedByFocus) return false

        interjecting = true
        val paused = player?.takeIf { runCatching { it.isPlaying }.getOrDefault(false) }
        paused?.let { runCatching { it.pause() } }
        if (player == null) audioManager.requestAudioFocus(focusRequest)
        val mine = stops

        fun finish() {
            cuePlayer = null
            interjecting = false
            if (mine != stops) return
            if (player == null) audioManager.abandonAudioFocusRequest(focusRequest)
            // 외치는 사이에 다음 문장이 준비됐으면(문장 사이에 깜빡이를 켠 경우) 멈췄던 문장 대신 그 문장을 시작합니다.
            paused?.let { if (player === it) runCatching { it.start() } }
            startWhenCueEnds?.invoke()
            startWhenCueEnds = null
            onDone()
        }

        fun playFrom(index: Int) {
            if (index >= files.size || mine != stops) {
                finish()
                return
            }
            val clip = MediaPlayer()
            cuePlayer = clip
            try {
                clip.setAudioAttributes(attributes)
                clip.setDataSource(files[index].path)
                clip.setOnCompletionListener {
                    it.release()
                    playFrom(index + 1)
                }
                clip.setOnErrorListener { failed, _, _ ->
                    failed.release()
                    finish()
                    true
                }
                clip.prepare()
                start(clip, slow = clips[index].second)
            } catch (e: Exception) {
                runCatching { clip.release() }
                finish()
            }
        }
        playFrom(0)
        return true
    }

    /**
     * 음악·라디오에 포커스를 아주 내준 뒤, 그 소리가 멎었으면 포커스를 다시 받아 멈춘 자리부터 이어 읽습니다.
     * 낭독부가 멈춰 있는 동안 주기적으로 부릅니다. 다른 소리가 아직 나고 있으면 빼앗지 않고 기다립니다.
     */
    fun tryRegainFocus(): Boolean {
        if (!pausedByFocus || !focusLostForGood) return false
        if (audioManager.isMusicActive) return false
        if (audioManager.requestAudioFocus(focusRequest) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return false
        Log.d(TAG, "other audio ended: taking focus back")
        onFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        return true
    }

    /** 읽던 것을 멈춥니다. 대기 중이던 콜백은 호출되지 않습니다. */
    fun stop() {
        generation++
        stops++
        // 멈춘 뒤에는 포커스를 내놓으므로 "되찾았다"는 신호가 오지 않습니다. 다음에 읽을 때 막히지 않게 풉니다.
        pausedByFocus = false
        focusLostForGood = false
        main.removeCallbacks(regainCheck)
        cuePlayer?.let { runCatching { it.release() } }
        cuePlayer = null
        interjecting = false
        startWhenCueEnds = null
        releasePlayer()
    }

    fun release() {
        stop()
        speakIo.shutdownNow()
        prefetchIo.shutdownNow()
    }

    // region 음성 파일 준비 (백그라운드 스레드)

    private fun audioFor(text: String): File {
        voiceId?.let { known -> fileFor(known, text).takeIf { it.length() > 0 }?.let { return it } }
        if (SystemClock.elapsedRealtime() < blockedUntil) throw IOException(lastFailure)

        try {
            val voice = voiceId ?: resolveVoice().also { voiceId = it }
            val file = fileFor(voice, text)
            synchronized(downloadLocks.getOrPut(file.name) { Any() }) {
                if (file.length() == 0L) download(voice, text, file)
            }
            return file
        } catch (e: IOException) {
            blockedUntil = SystemClock.elapsedRealtime() + RETRY_AFTER_MS
            lastFailure = e.message ?: "연결 실패"
            Log.w(TAG, "ElevenLabs request failed: ${e.message}")
            throw e
        }
    }

    private fun fileFor(voice: String, text: String): File {
        val digest = MessageDigest.getInstance("SHA-1").digest("$modelId|$voice|$text".toByteArray())
        return File(cacheDir, digest.joinToString("") { "%02x".format(it) } + ".mp3")
    }

    private fun download(voice: String, text: String, target: File) {
        val connection = open("$API/v1/text-to-speech/$voice?output_format=mp3_44100_128").apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "audio/mpeg")
        }
        try {
            val body = JSONObject().put("text", text).put("model_id", modelId).toString()
            connection.outputStream.use { it.write(body.toByteArray()) }
            requireOk(connection)

            val partial = File(target.path + ".part")
            connection.inputStream.use { input -> partial.outputStream().use { input.copyTo(it) } }
            if (partial.length() == 0L || !partial.renameTo(target)) {
                partial.delete()
                throw IOException("빈 음성 응답")
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * 읽어 줄 목소리를 정합니다. 계정의 목소리 목록을 볼 수 있으면 그중에서 고르고,
     * API 키에 목록 읽기 권한(voices_read)이 없으면 [FALLBACK_VOICE_ID]를 씁니다.
     */
    private fun resolveVoice(): String = try {
        pickVoice()
    } catch (e: IOException) {
        if (e.message != REASON_NO_PERMISSION) throw e
        Log.w(TAG, "cannot list voices with this API key; using the built-in default voice")
        voiceName = FALLBACK_VOICE_NAME
        FALLBACK_VOICE_ID
    }

    /** 계정에서 쓸 수 있는 목소리 중 이름·설명·라벨이 가장 '귀여운' 것을 고릅니다. */
    private fun pickVoice(): String {
        val connection = open("$API/v2/voices?page_size=100")
        try {
            requireOk(connection)
            val voices = JSONObject(connection.inputStream.bufferedReader().readText()).optJSONArray("voices")
            var best: JSONObject? = null
            var bestScore = -1
            for (i in 0 until (voices?.length() ?: 0)) {
                val voice = voices!!.getJSONObject(i)
                val score = cutenessOf(voice)
                if (score > bestScore) {
                    best = voice
                    bestScore = score
                }
            }
            val chosen = best ?: throw IOException("계정에 쓸 수 있는 목소리가 없음")
            voiceName = chosen.optString("name")
            Log.i(TAG, "picked voice '${voiceName}' (${chosen.getString("voice_id")}), score $bestScore")
            return chosen.getString("voice_id")
        } finally {
            connection.disconnect()
        }
    }

    private fun cutenessOf(voice: JSONObject): Int {
        val labels = voice.optJSONObject("labels")
        val words = buildString {
            append(voice.optString("name")).append(' ').append(voice.optString("description"))
            labels?.keys()?.forEach { append(' ').append(labels.optString(it)) }
        }.lowercase()
        var score = CUTE_WORDS.count { it in words } * 3
        if ("female" in words) score += 1
        if ("korean" in words || labels?.optString("language") == "ko") score += 2
        return score
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("xi-api-key", apiKey)
        }

    private fun requireOk(connection: HttpURLConnection) {
        val code = connection.responseCode
        if (code == HttpURLConnection.HTTP_OK) return
        val detail = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        Log.w(TAG, "HTTP $code: ${detail.take(300)}")
        throw IOException(reasonFor(code, detail))
    }

    /** 화면에 보여 줄 짧은 실패 이유. ElevenLabs 오류 응답의 detail.status를 우리말로 바꿉니다. */
    private fun reasonFor(code: Int, body: String): String {
        val status = runCatching { JSONObject(body).optJSONObject("detail")?.optString("status") }.getOrNull()
        return when (status) {
            "quota_exceeded" -> "크레딧 소진"
            "invalid_api_key" -> "API 키 오류"
            "missing_permissions" -> REASON_NO_PERMISSION
            "voice_not_found" -> "목소리 ID 오류"
            null, "" -> "HTTP $code"
            else -> "$status (HTTP $code)"
        }
    }

    // endregion

    // region 재생 (메인 스레드)

    private fun play(file: File, slow: Boolean, mine: Int, onDone: () -> Unit, onFailed: (String) -> Unit) {
        releasePlayer()
        audioManager.requestAudioFocus(focusRequest)
        val mediaPlayer = MediaPlayer()
        player = mediaPlayer

        fun end(report: () -> Unit) {
            if (player === mediaPlayer) releasePlayer()
            if (mine == generation) report()
        }

        try {
            mediaPlayer.setAudioAttributes(attributes)
            mediaPlayer.setDataSource(file.path)
            mediaPlayer.setOnPreparedListener { prepared ->
                // 끼어든 말이 끝난 뒤에 시작해야 두 목소리가 겹치지 않습니다. 내비가 말하는 중이면 끝난 뒤에.
                when {
                    interjecting -> startWhenCueEnds = { if (player === prepared) runCatching { start(prepared, slow) } }
                    pausedByFocus -> startWhenFocusReturns = { if (player === prepared) runCatching { start(prepared, slow) } }
                    else -> start(prepared, slow)
                }
            }
            mediaPlayer.setOnCompletionListener { end(onDone) }
            mediaPlayer.setOnErrorListener { _, what, extra ->
                file.delete()
                end { onFailed("재생 오류 $what/$extra") }
                true
            }
            mediaPlayer.prepareAsync()
        } catch (e: Exception) {
            file.delete()
            end { onFailed("재생 준비 실패: ${e.message}") }
        }
    }

    /**
     * 재생을 시작합니다. [slow]이면 음높이는 그대로 두고 빠르기만 늦춥니다.
     * (ElevenLabs의 빠르기 설정은 이 모델에서 듣지 않아, 받은 음성을 차에서 늦춰 들려줍니다.)
     */
    private fun start(mediaPlayer: MediaPlayer, slow: Boolean) {
        if (slow) {
            // 빠르기를 정하면 재생도 함께 시작됩니다.
            mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(SLOW_PLAYBACK).setPitch(1f)
        } else {
            mediaPlayer.start()
        }
    }

    private fun releasePlayer() {
        val current = player ?: return
        player = null
        startWhenFocusReturns = null
        runCatching { current.release() }
        audioManager.abandonAudioFocusRequest(focusRequest)
    }

    // endregion

    private companion object {
        const val REGAIN_CHECK_MS = 3_000L
        const val TAG = "ElevenLabsVoice"
        const val API = "https://api.elevenlabs.io"
        const val CONNECT_TIMEOUT_MS = 4_000
        const val RESUME_DELAY_MS = 350L
        const val READ_TIMEOUT_MS = 15_000

        /**
         * 목소리 목록을 읽을 수 없을 때 쓰는 ElevenLabs 기본 제공 목소리(밝고 장난스러운 젊은 여성 목소리).
         * 기본 제공 목소리는 2026-12-31에 종료된다고 안내되어 있으므로,
         * 그 뒤에는 secrets.properties의 ELEVENLABS_VOICE_ID에 원하는 목소리를 넣어야 합니다.
         */
        const val FALLBACK_VOICE_ID = "cgSgspJ2msm6clMCkdW9"
        const val FALLBACK_VOICE_NAME = "Jessica"
        const val REASON_NO_PERMISSION = "API 키 권한 부족"

        /** 한 번 실패하면 이 시간 동안은 다시 요청하지 않습니다(터널, 음영 지역 대비). */
        const val RETRY_AFTER_MS = 60_000L

        /** 영어를 들려주는 빠르기(1.0이 보통). 원어민 속도는 아이에게 너무 빨라 느리게 들려줍니다. */
        const val SLOW_PLAYBACK = 0.8f

        val CUTE_WORDS = listOf(
            "cute", "child", "kid", "young", "playful", "bright", "sweet",
            "cheerful", "animated", "cartoon", "귀여", "아이", "어린",
        )
    }
}

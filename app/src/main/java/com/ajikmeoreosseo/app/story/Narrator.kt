package com.ajikmeoreosseo.app.story

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.ajikmeoreosseo.app.BuildConfig
import java.util.Locale

/**
 * 한 번에 한 문장씩 읽어 줍니다.
 *
 * 1순위는 ElevenLabs 목소리입니다(secrets.properties에 API 키가 있을 때).
 * 키가 없거나 통신이 안 되면 Pleos TextToSpeech SDK의 ON_DEVICE 음성으로 읽습니다.
 * 둘 다 안 되는 환경에서도 이야기가 멈추지 않도록, 완료 신호가 오지 않으면
 * 글자 수로 어림한 시간이 지난 뒤 다음 문장으로 넘어갑니다.
 */
class Narrator(context: Context) {

    private val main = Handler(Looper.getMainLooper())
    private val carTts = TextToSpeech(context.applicationContext, Mode.ON_DEVICE)
    private val cloud: ElevenLabsVoice? = BuildConfig.ELEVENLABS_API_KEY.takeIf { it.isNotBlank() }?.let { key ->
        ElevenLabsVoice(
            context,
            apiKey = key,
            configuredVoiceId = BuildConfig.ELEVENLABS_VOICE_ID,
            modelId = BuildConfig.ELEVENLABS_MODEL.ifBlank { DEFAULT_MODEL },
        )
    }

    private var carReady = false
    private var carLang = Lang.KO
    private var speakingWithCar = false
    private var cloudProblem: String? = null
    private var onFinished: (() -> Unit)? = null
    private val watchdog = Runnable { finish("watchdog") }

    /** 지금 어떤 목소리로 읽는지 화면에 보여 줄 한 줄. 바뀔 때마다 호출됩니다. */
    var onStatus: ((String) -> Unit)? = null

    /** 내비 안내 같은 다른 소리가 끝나 다시 읽기 시작할 때. 이야기 엔진이 그 내용을 동화로 받아 주는 데 씁니다. */
    var onInterruptionEnded: (() -> Unit)? = null
        set(value) {
            field = value
            cloud?.onInterruptionEnded = value
        }

    private val carEvents = object : EventListener {
        override fun onReady() = Unit
        override fun onStart() = Unit
        override fun onUpdatedRms(rms: Double) = Unit
        override fun onDone() { main.post { finish("car") } }
        override fun onStop() = Unit
        override fun onError(errorType: ErrorType) {
            Log.w(TAG, "car TTS error: $errorType")
            // 자막이 읽힐 시간은 남겨 두고 워치독이 다음으로 넘깁니다.
        }
    }

    fun start() {
        publishStatus()
        carTts.initialize(object : OnInitializeListener {
            override fun onSuccess() {
                main.post {
                    carTts.addEventListener(carEvents)
                    carTts.setLocale(Locale.KOREA)
                    carReady = true
                    publishStatus()
                }
            }

            override fun onFailure(errorType: ErrorType) {
                Log.w(TAG, "car TTS init failed: $errorType")
                main.post { publishStatus() }
            }
        })
    }

    /** 곧 읽을 문장을 미리 준비합니다(ElevenLabs를 쓸 때만 의미가 있습니다). */
    fun prefetch(text: String) {
        if (!forceCarVoice) cloud?.prefetch(text)
    }

    /**
     * 읽던 말을 잠깐 멈추고 짧은 말들을 바로 말한 뒤 이어 읽습니다(차가 움직인 순간의 한마디).
     * 지금 바로 말할 수 없으면(음성이 아직 준비되지 않았거나 차량 기본 음성으로 읽는 중) false를 돌려줍니다.
     */
    fun interject(clips: List<Pair<String, Lang>>, onDone: () -> Unit): Boolean {
        val voice = cloud ?: return false
        if (speakingWithCar || forceCarVoice) return false
        return voice.interject(clips.map { (text, lang) -> text to (lang == Lang.EN) }, onDone)
    }

    /** [text]를 읽고, 끝나면 [onDone]을 메인 스레드에서 한 번 호출합니다. */
    fun speak(text: String, lang: Lang = Lang.KO, onDone: () -> Unit) {
        main.removeCallbacks(watchdog)
        onFinished = onDone
        speakingWithCar = false

        val voice = cloud.takeUnless { forceCarVoice }
        if (voice == null) {
            speakWithCar(text, lang)
            return
        }
        main.postDelayed(watchdog, estimateMillis(text) + CLOUD_WATCHDOG_MARGIN_MS)
        voice.speak(
            text,
            slow = lang == Lang.EN,
            onDone = {
                setCloudProblem(null)
                finish("cloud")
            },
            onFailed = { reason ->
                setCloudProblem(reason)
                if (onFinished != null) speakWithCar(text, lang)
            },
        )
    }

    /** 읽던 것을 멈춥니다. 대기 중이던 onDone은 호출되지 않습니다. */
    fun stop() {
        main.removeCallbacks(watchdog)
        onFinished = null
        speakingWithCar = false
        cloud?.stop()
        if (carReady) runCatching { carTts.stop() }
    }

    fun release() {
        stop()
        cloud?.release()
        runCatching {
            carTts.removeEventListener(carEvents)
            carTts.release()
        }
        carReady = false
    }

    private fun speakWithCar(text: String, lang: Lang) {
        main.removeCallbacks(watchdog)
        val estimate = estimateMillis(text)
        if (carReady) {
            // 차량 기본 음성은 언어를 바꿔 줘야 영어를 영어답게 읽습니다.
            if (carLang != lang) {
                carLang = lang
                runCatching { carTts.setLocale(if (lang == Lang.EN) Locale.US else Locale.KOREA) }
            }
            speakingWithCar = true
            runCatching { carTts.speak(text, true) }.onFailure { Log.w(TAG, "car TTS speak failed", it) }
            main.postDelayed(watchdog, estimate + CAR_WATCHDOG_MARGIN_MS)
        } else {
            main.postDelayed(watchdog, estimate)
        }
    }

    /** 시험용: true면 ElevenLabs를 건너뛰고 차량 기본 음성으로만 읽습니다(통신이 끊긴 상황 확인). */
    var forceCarVoice = false

    /** 시연용: 내비가 말하기 시작했다/끝났다를 흉내 냅니다. */
    fun simulateFocus(lost: Boolean) {
        cloud?.onFocusChange(if (lost) android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT else android.media.AudioManager.AUDIOFOCUS_GAIN)
    }

    private fun finish(reason: String) {
        // 내비 안내 때문에 멈춰 있는 동안에는 기다립니다.
        if (reason == "watchdog" && cloud?.pausedByFocus == true) {
            main.postDelayed(watchdog, PAUSED_RECHECK_MS)
            return
        }
        main.removeCallbacks(watchdog)
        val callback = onFinished ?: return
        onFinished = null
        speakingWithCar = false
        if (reason == "watchdog") Log.d(TAG, "no completion signal; advanced by timer")
        callback()
    }

    private fun setCloudProblem(problem: String?) {
        if (cloudProblem == problem) return
        cloudProblem = problem
        publishStatus()
    }

    private fun publishStatus() {
        val problem = cloudProblem
        val status = when {
            cloud != null && problem == null ->
                "목소리: ElevenLabs" + (cloud.voiceName?.let { " · $it" } ?: "")
            cloud != null && carReady -> "차량 기본 음성으로 읽는 중 (ElevenLabs $problem)"
            cloud != null -> "ElevenLabs $problem · 자막으로만 진행해요"
            carReady -> "목소리: 차량 기본 음성"
            else -> "목소리를 준비하는 중…"
        }
        onStatus?.invoke(status)
    }

    companion object {
        private const val TAG = "Narrator"
        private const val DEFAULT_MODEL = "eleven_v3"
        private const val CAR_WATCHDOG_MARGIN_MS = 6_000L
        private const val CLOUD_WATCHDOG_MARGIN_MS = 30_000L
        private const val PAUSED_RECHECK_MS = 5_000L

        /** 한국어 낭독 속도를 글자당 약 0.16초로 어림합니다. */
        fun estimateMillis(text: String): Long = 800L + text.length * 160L
    }
}

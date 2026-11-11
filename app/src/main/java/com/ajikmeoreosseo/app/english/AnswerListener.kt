package com.ajikmeoreosseo.app.english

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.constant.Mode
import ai.pleos.playground.stt.listener.OnInitializeListener
import ai.pleos.playground.stt.listener.ResultListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.Locale

/**
 * 아이가 영어를 따라 말하는지 차량 마이크로 듣습니다(Pleos SpeechToText SDK, 차량 안에서만 인식).
 *
 * 들은 말이 기대한 영어와 비슷하면 통과입니다. 발음이 서툴러도 통과하도록 느슨하게 비교합니다.
 * 음성 인식을 쓸 수 없는 차(권한 없음, 엔진 없음, 영어 미지원)에서는 [available]이 false가 되고,
 * 이야기 엔진은 예전처럼 잠깐 기다렸다가 통과로 처리합니다. 모든 메서드는 메인 스레드에서 호출합니다.
 */
class AnswerListener(context: Context) {

    /** 들은 결과. [heard]는 인식된 글(없으면 null). */
    class Result(val passed: Boolean, val heard: String?)

    private val main = Handler(Looper.getMainLooper())
    private val stt = SpeechToText(context.applicationContext, Mode.ON_DEVICE)

    /**
     * 시험용(디버그 빌드): 이 파일(16kHz·16비트·모노 PCM)이 있으면 마이크 대신 이 소리를 인식기에 넣습니다. 한 번 쓰고 지웁니다.
     * 마이크가 없는 에뮬레이터에서 "아이가 말했을 때"를 확인하는 데 씁니다.
     */
    private val debugAudioFile = java.io.File(context.applicationContext.filesDir, "debug_answer.pcm")

    /** 시험용(디버그 빌드): 말마다 따로 둔 소리. 레벨 테스트처럼 무엇을 물을지 미리 알 수 없을 때 씁니다. */
    private val debugAnswersDir = java.io.File(context.applicationContext.filesDir, "debug_answers")

    /** 지금 차에서 들을 수 있는지. 초기화 실패나 언어 미지원이면 false. */
    var available = false
        private set

    var onListening: ((Boolean) -> Unit)? = null

    /** 초기화가 끝났는지(성공이든 실패든). 끝나기 전에는 [available]이 아직 정해지지 않은 것입니다. */
    var initialized = false
        private set

    /** 초기화가 끝났을 때 한 번(성공이든 실패든). 화면의 안내를 바꾸고 로비의 간식 주기를 시작하는 데 씁니다. */
    var onReady: (() -> Unit)? = null

    private var expected: Word? = null
    private var onResult: ((Result) -> Unit)? = null
    private var heardSoFar: String? = null
    private val timeout = Runnable { finish(matched = false, reason = "timeout") }

    private val results = object : ResultListener {
        override fun onReady() = Unit
        override fun onStartedRecognition() = Unit
        override fun onUpdatedEpdData(on: Long, off: Long) = Unit
        override fun onUpdatedRms(rms: Double) = Unit

        override fun onUpdated(stt: String, completed: Boolean) {
            main.post {
                val word = expected ?: return@post
                Log.d(TAG, "heard: \"$stt\" completed=$completed")
                if (stt.isNotBlank()) heardSoFar = stt
                if (matches(stt, word)) {
                    finish(matched = true, reason = "matched")
                } else if (completed) {
                    finish(matched = false, reason = "final: $stt")
                }
            }
        }

        override fun onEndedRecognition() {
            // 인식이 끝났다는 신호가 마지막 글보다 먼저 올 때가 있어 잠깐 더 기다립니다.
            main.postDelayed({
                val word = expected ?: return@postDelayed
                finish(matched = heardSoFar?.let { matches(it, word) } == true, reason = "ended")
            }, END_GRACE_MS)
        }

        override fun onError(errorType: ErrorType) {
            main.post {
                Log.w(TAG, "stt error: $errorType")
                when (errorType) {
                    ErrorType.UNAVAILABLE_LANGUAGE, ErrorType.PERMISSION_DENIED, ErrorType.NOT_SUPPORTED_API,
                    ErrorType.ON_DEVICE_ENGINE_ERROR -> available = false
                    else -> Unit
                }
                if (expected != null) finish(matched = false, reason = "error $errorType", unavailable = !available)
            }
        }
    }

    fun start() {
        runCatching {
            stt.initialize(object : OnInitializeListener {
                override fun onSuccess() {
                    main.post {
                        available = true
                        stt.addListener(results)
                        Log.d(TAG, "stt ready")
                        initialized = true
                        onReady?.invoke()
                    }
                }

                override fun onFailure(errorType: ErrorType) {
                    main.post {
                        available = false
                        Log.w(TAG, "stt unavailable: $errorType")
                        initialized = true
                        onReady?.invoke()
                    }
                }
            })
        }.onFailure {
            available = false
            Log.w(TAG, "stt init failed", it)
            initialized = true
            main.post { onReady?.invoke() }
        }
    }

    /**
     * [word]를 따라 말하는지 최대 [timeoutMs] 동안 듣습니다. 결과는 [onResult]로 한 번만 옵니다.
     * 들을 수 없으면 바로 통과로 돌려줍니다(아이가 손해 보지 않도록).
     */
    fun listen(word: Word, timeoutMs: Long, onResult: (Result) -> Unit) {
        if (!available) {
            onResult(Result(passed = true, heard = null))
            return
        }
        cancel()
        expected = word
        this.onResult = onResult
        heardSoFar = null
        // 시험용(디버그 빌드): 그 말에 맞춰 둔 소리(debug_answers/<id>.pcm)가 있으면 그것을, 없으면 한 번 쓰는 소리(debug_answer.pcm)를 넣습니다.
        val perWord = java.io.File(debugAnswersDir, word.id + ".pcm")
        val debugAudio = when {
            !com.ajikmeoreosseo.app.BuildConfig.DEBUG -> null
            perWord.exists() -> runCatching { perWord.readBytes() }.getOrNull()
            debugAudioFile.exists() -> runCatching { debugAudioFile.readBytes() }.getOrNull().also { debugAudioFile.delete() }
            else -> null
        }
        val started = runCatching {
            stt.setLocale(Locale.US)
            if (debugAudio == null) stt.request() else feed(debugAudio)
        }
        if (started.isFailure) {
            Log.w(TAG, "stt request failed", started.exceptionOrNull())
            expected = null
            this.onResult = null
            onResult(Result(passed = true, heard = null))
            return
        }
        onListening?.invoke(true)
        main.postDelayed(timeout, timeoutMs)
    }

    /** 시험용 소리를 0.1초 분량(3,200바이트)씩 넣고, 다 넣으면 끝을 알립니다. */
    private fun feed(audio: ByteArray) {
        Log.d(TAG, "feeding ${audio.size} bytes of test audio")
        var offset = 0
        val step = object : Runnable {
            override fun run() {
                if (expected == null) return
                if (offset >= audio.size) {
                    runCatching { stt.completeAudioSend() }
                    return
                }
                val end = minOf(offset + 3_200, audio.size)
                runCatching { stt.sendAudio(audio.copyOfRange(offset, end)) }
                offset = end
                main.postDelayed(this, 100L)
            }
        }
        main.post(step)
    }

    /** 듣기를 그만둡니다. 결과 콜백은 호출되지 않습니다. */
    fun cancel() {
        if (expected == null) return
        main.removeCallbacks(timeout)
        expected = null
        onResult = null
        runCatching { stt.stop() }
        onListening?.invoke(false)
    }

    fun release() {
        cancel()
        runCatching {
            stt.removeListener(results)
            stt.release()
        }
    }

    private fun finish(matched: Boolean, reason: String, unavailable: Boolean = false) {
        val callback = onResult ?: return
        main.removeCallbacks(timeout)
        val heard = heardSoFar
        expected = null
        onResult = null
        runCatching { stt.stop() }
        onListening?.invoke(false)
        Log.d(TAG, "listen result: matched=$matched heard=$heard ($reason)")
        // 인식 자체가 안 되는 차에서는 아이 잘못이 아니므로 통과시킵니다.
        callback(Result(passed = matched || unavailable, heard = heard))
    }

    companion object {
        private const val TAG = "AnswerListener"
        private const val END_GRACE_MS = 700L

        /** 들은 말이 기대한 영어와 비슷한지. 낱말은 글자 거리로, 여러 낱말은 겹치는 낱말 비율로 봅니다. */
        fun matches(heard: String, word: Word): Boolean {
            val said = normalize(heard)
            val target = normalize(word.english)
            if (said.isEmpty() || target.isEmpty()) return false
            if (said.contains(target)) return true
            val targetWords = target.split(' ').filter { it.isNotEmpty() }
            val saidWords = said.split(' ').filter { it.isNotEmpty() }
            if (targetWords.size == 1) {
                return saidWords.any { similar(it, target) }
            }
            // 짧은 기능어(a, the, is)는 빼고, 남은 낱말의 6할 이상이 들리면 통과
            val content = targetWords.filter { it.length > 2 }
            if (content.isEmpty()) return saidWords.any { similar(it, targetWords.last()) }
            val hit = content.count { t -> saidWords.any { similar(it, t) } }
            return hit * 10 >= content.size * 6
        }

        private fun normalize(text: String) =
            text.lowercase().replace(Regex("[^a-z' ]"), " ").replace(Regex("\\s+"), " ").trim()

        private fun similar(a: String, b: String): Boolean {
            if (a == b) return true
            val longest = maxOf(a.length, b.length)
            if (longest < 3) return false
            return levenshtein(a, b) <= (longest + 2) / 3
        }

        private fun levenshtein(a: String, b: String): Int {
            val prev = IntArray(b.length + 1) { it }
            val cur = IntArray(b.length + 1)
            for (i in 1..a.length) {
                cur[0] = i
                for (j in 1..b.length) {
                    val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                    cur[j] = minOf(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
                }
                cur.copyInto(prev)
            }
            return prev[b.length]
        }
    }
}

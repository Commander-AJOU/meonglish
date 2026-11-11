package com.ajikmeoreosseo.app.english

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.ajikmeoreosseo.app.ai.ClaudeApi
import org.json.JSONObject
import java.util.concurrent.Executors
import kotlin.random.Random

/** 레벨 테스트에서 아이가 한 대답 하나. [heard]는 차가 알아들은 글(없으면 null), [byAdult]는 어른이 눌러서 정했는지. */
class TestAnswer(val word: Word, val level: Int, val passed: Boolean, val heard: String?, val byAdult: Boolean)

/** 레벨 테스트의 다음 할 일. */
sealed class TestStep {
    /** [word]를 묻습니다. */
    class Ask(val word: Word, val level: Int) : TestStep()

    /** 끝. [level]이 아이가 배울 레벨이고, [comment]는 부모에게 보여 줄 한두 문장(없을 수 있음). */
    class Finish(val level: Int, val comment: String?, val byClaude: Boolean) : TestStep()
}

/**
 * 레벨 테스트의 출제자. 지금까지의 대답([TestAnswer])을 보고 다음 문제를 내거나 끝냅니다.
 * [next]의 결과는 메인 스레드에서 [onStep]으로 한 번 옵니다.
 */
interface LevelExaminer {
    val maxQuestions: Int

    /** Claude가 출제하는지(아니면 앱에 든 기본 테스트). */
    val usesClaude: Boolean

    fun next(history: List<TestAnswer>, onStep: (TestStep) -> Unit)

    fun close() {}
}

/**
 * 앱에 든 기본 테스트([LevelTest]): 한 레벨에 두 문제씩, 둘 다 맞히면 올라갑니다.
 * [skip]은 이 출제자가 맡기 전에 이미 끝난 대답의 수입니다(Claude가 하다가 넘겨받았을 때).
 */
class LocalExaminer(seen: Set<String>, startLevel: Int = 1, private val skip: Int = 0, random: Random = Random.Default) : LevelExaminer {
    private val test = LevelTest({ level -> LevelTest.questions(level, seen) }, random, startLevel)
    private var fed = 0

    override val maxQuestions = LevelTest.MAX_QUESTIONS
    override val usesClaude = false

    override fun next(history: List<TestAnswer>, onStep: (TestStep) -> Unit) {
        val mine = history.drop(skip)
        while (fed < mine.size) test.answer(mine[fed++].passed)
        val word = test.current
        onStep(if (word != null) TestStep.Ask(word, test.level) else TestStep.Finish(test.result ?: 1, null, byClaude = false))
    }
}

/**
 * Claude가 출제하는 레벨 테스트.
 *
 * 문제마다 Claude에게 "지금까지의 대답"과 "낼 수 있는 문제 목록"을 보내면, Claude가 아이의 대답을 보고 다음 문제를 고르거나
 * (잘하면 레벨을 건너뛰고, 헷갈리면 같은 레벨을 한 번 더 확인) 끝내면서 레벨과 부모에게 보여 줄 한두 문장을 정합니다.
 * 문제는 앱의 단어장에서 뽑은 목록 안에서만 고르게 해서, 아이에게 맞지 않는 말이 나오지 않게 합니다.
 *
 * 보내는 것은 문제(우리말·영어), 맞았는지, 차가 알아들은 글뿐입니다. 아이 이름과 목소리는 보내지 않습니다.
 * 통신이 안 되거나 응답이 이상하면 그 자리에서 기본 테스트([LocalExaminer])가 이어받습니다.
 */
class ClaudeExaminer(private val seen: Set<String>, random: Random = Random.Default) : LevelExaminer {

    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private val client = ClaudeApi.client(TIMEOUT_SECONDS)

    /** 낼 수 있는 문제: 레벨마다 [PER_LEVEL]개씩 무작위로. */
    private val candidates: List<Word> =
        (1..Levels.MAX).flatMap { level -> LevelTest.questions(level, seen).shuffled(random).take(PER_LEVEL) }

    /** Claude가 실패한 뒤 이어받은 기본 테스트. */
    private var fallback: LocalExaminer? = null
    private var closed = false

    /** 기본 테스트가 이어받은 뒤에는 그쪽의 문제 수를 따릅니다. */
    override val maxQuestions: Int get() = fallback?.maxQuestions ?: MAX_QUESTIONS
    override var usesClaude = true
        private set

    override fun next(history: List<TestAnswer>, onStep: (TestStep) -> Unit) {
        fallback?.let { return it.next(history, onStep) }
        val asked = history.toList()
        worker.execute {
            val step = runCatching { askClaude(asked) }
                .onFailure { Log.w(TAG, "claude failed: ${ClaudeApi.reason(it)} (${it.javaClass.simpleName})") }
                .getOrNull()
            main.post {
                if (closed) return@post
                if (step != null) {
                    onStep(step)
                } else {
                    // 기본 테스트가 이어받습니다: 지금까지 맞힌 가장 높은 레벨부터.
                    usesClaude = false
                    val local = LocalExaminer(seen + asked.map { it.word.id }, resumeLevel(asked), skip = asked.size)
                    fallback = local
                    local.next(asked, onStep)
                }
            }
        }
    }

    override fun close() {
        closed = true
        worker.shutdownNow()
    }

    private fun askClaude(history: List<TestAnswer>): TestStep? {
        // 문제 하나를 고르는 짧은 판단이고 아이가 기다리므로 가장 가벼운 단계로 둡니다.
        val text = ClaudeApi.json(client, SYSTEM, prompt(candidates, history, MAX_QUESTIONS - history.size), SCHEMA, ClaudeApi.Effort.LOW)
        val json = JSONObject(text)
        Log.d(TAG, "claude: $text")
        return decide(
            action = json.optString("action"),
            questionId = json.optString("question_id"),
            level = json.optInt("level", 0),
            comment = json.optString("comment"),
            candidates = candidates,
            history = history,
            maxQuestions = MAX_QUESTIONS,
        )
    }

    companion object {
        private const val TAG = "LevelExaminer"
        const val MAX_QUESTIONS = 8
        private const val PER_LEVEL = 8
        private const val TIMEOUT_SECONDS = 20L
        private const val MAX_COMMENT = 240

        private val SYSTEM = """
            You run a short spoken English level test inside a car app for young Korean children (about 4 to 9 years old).
            The child sits in the back seat. A puppy character asks each question aloud in Korean (for example "‘사과’. 영어로 뭐라고 할까?") and the child answers in English.
            The car's speech recognizer, or the parent pressing a button, tells us whether the answer was right. The child cannot see the screen, so every question is one item to say aloud.

            The app teaches at five levels:
            1. Easy concrete words: story things, things seen outside, animals, food, colors, numbers (Puppy, Apple, Red)
            2. All other single words: adjectives, verbs, body, home, nature, people (Big, Jump, Happy)
            3. Short phrases (Let's fly!, A red dog)
            4. Sentences (I see a dog.)
            5. Questions and sentences of five words or more (Where is the dog?)

            Your goal is to find the level where this child should START learning: the lowest level whose items the child does not yet reliably know. A child who knows nothing yet starts at level 1; a child who answers level 5 items starts at level 5.

            Each turn you get the questions asked so far with their results, and a list of candidate questions. Reply with exactly one decision:
            - "ask": choose the next question. question_id must be the id of a candidate that has not been asked yet.
            - "finish": the test is over. Give the starting level (1 to 5) and a comment for the parent.

            How to test well:
            - Be adaptive. Start at level 1 or 2. When the child answers correctly, move up, and skip a level if the answers came easily. When the child misses, ask one more item at that level or the level below to confirm before deciding.
            - Keep it short. The child is small and the test should feel like a game. Finish as soon as you are confident, usually after 4 to 6 questions, and never ask more questions than questions_left allows. When questions_left is 0 you must finish.
            - One lucky or unlucky answer should not decide the level; two answers at a level usually do.
            - "heard" is what the speech recognizer understood. It often mishears small children. If the heard text is clearly the right answer misrecognized (for example "flour" for Flower), you may count the item as known when choosing the level. If the result was set by the parent (by_adult), trust it.
            - Prefer common, concrete items over rare ones when you have a choice.

            The comment is shown to the parent on the result screen. Write one or two short sentences in polite Korean (해요체): what the child already knows and where learning will start. Be warm and specific, never use words like "틀렸어요" or "부족해요", and do not mention scores. When the action is "ask", set comment to an empty string and level to the level of the question you chose. When the action is "finish", set question_id to an empty string.
        """.trimIndent()

        /** 응답 형식: 한 번의 결정. */
        private val SCHEMA: Map<String, Any> = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "action" to mapOf("type" to "string", "enum" to listOf("ask", "finish")),
                "question_id" to mapOf("type" to "string"),
                "level" to mapOf("type" to "integer"),
                "comment" to mapOf("type" to "string"),
            ),
            "required" to listOf("action", "question_id", "level", "comment"),
            "additionalProperties" to false,
        )

        /** Claude에게 보내는 이번 차례의 글: 지금까지의 대답, 남은 문제 수, 낼 수 있는 문제. */
        fun prompt(candidates: List<Word>, history: List<TestAnswer>, questionsLeft: Int): String = buildString {
            appendLine("questions_left: ${questionsLeft.coerceAtLeast(0)}")
            appendLine()
            appendLine("Asked so far:")
            if (history.isEmpty()) appendLine("(none yet)")
            history.forEachIndexed { index, answer ->
                append("${index + 1}. id=${answer.word.id} level=${answer.level} korean=\"${answer.word.korean}\" answer=\"${answer.word.english}\" ")
                append("result=${if (answer.passed) "correct" else "missed"} heard=${answer.heard?.let { "\"$it\"" } ?: "nothing"}")
                appendLine(if (answer.byAdult) " by_adult=true" else "")
            }
            appendLine()
            appendLine("Candidates (id | level | Korean prompt | English answer):")
            val asked = history.map { it.word.id }.toSet()
            candidates.filter { it.id !in asked }.forEach { appendLine("${it.id} | ${Levels.of(it)} | ${it.korean} | ${it.english}") }
        }

        /**
         * Claude의 결정을 확인해 다음 할 일로 바꿉니다. 목록에 없는 문제를 고르거나, 이미 낸 문제를 또 고르거나,
         * 문제 수를 넘겨 더 내려 하면 null(기본 테스트가 이어받음)입니다.
         */
        fun decide(
            action: String,
            questionId: String,
            level: Int,
            comment: String,
            candidates: List<Word>,
            history: List<TestAnswer>,
            maxQuestions: Int,
        ): TestStep? = when (action) {
            "finish" ->
                if (level in 1..Levels.MAX) TestStep.Finish(level, comment.trim().take(MAX_COMMENT).ifEmpty { null }, byClaude = true) else null
            "ask" -> {
                val word = candidates.firstOrNull { it.id == questionId }
                if (word == null || history.size >= maxQuestions || history.any { it.word.id == questionId }) null
                else TestStep.Ask(word, Levels.of(word))
            }
            else -> null
        }

        /** 기본 테스트가 이어받을 레벨: 지금까지 맞힌 가장 높은 레벨(없으면 1). */
        fun resumeLevel(history: List<TestAnswer>): Int = history.filter { it.passed }.maxOfOrNull { it.level } ?: 1
    }
}

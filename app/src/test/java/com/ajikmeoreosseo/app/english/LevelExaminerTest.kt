package com.ajikmeoreosseo.app.english

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LevelExaminerTest {

    private fun word(id: String) = WordBank.find(id)!!
    private val candidates = listOf(word("puppy"), word("dog"), word("lets_fly"), word("s_let_s_clean_up"))
    private fun answered(id: String, passed: Boolean, heard: String? = null, byAdult: Boolean = false) =
        TestAnswer(word(id), Levels.of(word(id)), passed, heard, byAdult)

    private fun decide(action: String, id: String = "", level: Int = 0, comment: String = "", history: List<TestAnswer> = emptyList(), max: Int = 8) =
        ClaudeExaminer.decide(action, id, level, comment, candidates, history, max)

    @Test
    fun `Claude가 목록에서 고른 문제만 낸다`() {
        val step = decide("ask", "lets_fly", level = 1) as TestStep.Ask
        assertEquals("lets_fly", step.word.id)
        // 레벨은 Claude가 적은 값이 아니라 단어장의 규칙을 따른다
        assertEquals(3, step.level)
        assertNull(decide("ask", "banana_split"))
        assertNull(decide("ask", "puppy", history = listOf(answered("puppy", true))))
        assertNull(decide("ask", "dog", history = listOf(answered("puppy", true)), max = 1))
        assertNull(decide("sing", "dog"))
    }

    @Test
    fun `끝낼 때는 레벨이 1에서 5 사이여야 하고 부모에게 보여 줄 말을 다듬는다`() {
        val step = decide("finish", level = 3, comment = "  단어는 잘 알아요. 짧은 표현부터 시작할게요.  ") as TestStep.Finish
        assertEquals(3, step.level)
        assertEquals("단어는 잘 알아요. 짧은 표현부터 시작할게요.", step.comment)
        assertTrue(step.byClaude)
        assertNull((decide("finish", level = 2, comment = "") as TestStep.Finish).comment)
        assertNull(decide("finish", level = 0))
        assertNull(decide("finish", level = 6))
    }

    @Test
    fun `Claude에게는 지금까지의 대답과 아직 안 낸 문제만 보낸다`() {
        val history = listOf(answered("puppy", true, heard = "puppy"), answered("dog", false, heard = "duck", byAdult = true))
        val prompt = ClaudeExaminer.prompt(candidates, history, questionsLeft = 6)
        assertTrue(prompt.startsWith("questions_left: 6"))
        assertTrue(prompt.contains("id=puppy level=1") && prompt.contains("result=correct heard=\"puppy\""))
        assertTrue(prompt.contains("result=missed heard=\"duck\" by_adult=true"))
        val list = prompt.substringAfter("Candidates")
        assertTrue(list.contains("lets_fly | 3 | 날아가자 | Let's fly!"))
        assertFalse(list.contains("puppy |"))
        assertFalse(list.contains("dog |"))
        assertTrue(ClaudeExaminer.prompt(candidates, emptyList(), 8).contains("(none yet)"))
    }

    @Test
    fun `통신이 끊기면 기본 테스트가 맞힌 가장 높은 레벨부터 이어받는다`() {
        assertEquals(1, ClaudeExaminer.resumeLevel(emptyList()))
        assertEquals(1, ClaudeExaminer.resumeLevel(listOf(answered("puppy", false))))
        val history = listOf(answered("puppy", true), answered("lets_fly", true), answered("s_let_s_clean_up", false))
        assertEquals(3, ClaudeExaminer.resumeLevel(history))

        val local = LocalExaminer(history.map { it.word.id }.toSet(), startLevel = 3, skip = history.size, random = Random(3))
        var step: TestStep? = null
        local.next(history) { step = it }
        val first = step as TestStep.Ask
        assertEquals(3, first.level)
        assertFalse(first.word.id in history.map { it.word.id })
        // 레벨 3의 두 문제 가운데 하나를 틀리면 레벨 3에서 끝난다
        val more = history + TestAnswer(first.word, 3, true, null, false)
        local.next(more) { step = it }
        val second = step as TestStep.Ask
        local.next(more + TestAnswer(second.word, 3, false, null, false)) { step = it }
        assertEquals(3, (step as TestStep.Finish).level)
        assertFalse((step as TestStep.Finish).byClaude)
    }
}

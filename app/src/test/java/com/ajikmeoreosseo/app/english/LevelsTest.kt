package com.ajikmeoreosseo.app.english

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LevelsTest {

    private fun word(id: String) = WordBank.find(id)!!

    @Test
    fun `난이도는 다섯 단계이고 단계마다 배울 말이 있다`() {
        val counts = WordBank.words.filter { it.cue == null }.groupingBy { Levels.of(it) }.eachCount()
        assertEquals(setOf(1, 2, 3, 4, 5), counts.keys)
        for (level in 1..Levels.MAX) assertTrue("레벨 $level: ${counts[level]}", counts.getValue(level) >= 20)
        assertEquals(Levels.MAX, Levels.NAMES.size)
        assertEquals(Levels.MAX, Levels.HINTS.size)
    }

    @Test
    fun `쉬운 단어는 1, 꾸미는 말과 움직이는 말은 2, 표현은 3, 문장은 4, 묻는 말과 긴 문장은 5`() {
        assertEquals(1, Levels.of(word("puppy")))
        assertEquals(1, Levels.of(word("dog")))
        assertEquals(2, Levels.of(WordBank.words.first { it.tag == Tag.VERB }))
        assertEquals(2, Levels.of(WordBank.words.first { it.tag == Tag.ADJ }))
        assertEquals(3, Levels.of(word("lets_fly")))
        assertEquals(4, Levels.of(word("s_let_s_clean_up")))
        assertEquals(5, Levels.of(word("s_i_read_a_book_every_night")))
        // 익힌 낱말로 만든 문장에도 같은 규칙
        val dog = word("dog")
        val byKey = Sentences.templates.associateBy { it.key }
        assertEquals(3, Levels.of(Sentences.make(byKey.getValue("my"), dog, null)))
        assertEquals(4, Levels.of(Sentences.make(byKey.getValue("see"), dog, null)))
        assertEquals(5, Levels.of(Sentences.make(byKey.getValue("where"), dog, null)))
    }

    @Test
    fun `레벨 테스트 문제는 레벨마다 넉넉하고 차의 움직임 말과 이미 만난 말은 내지 않는다`() {
        for (level in 1..Levels.MAX) {
            val questions = LevelTest.questions(level)
            assertTrue("레벨 $level: ${questions.size}", questions.size >= 10)
            assertTrue(questions.all { it.cue == null && Levels.of(it) == level })
        }
        assertTrue(LevelTest.questions(1, seen = setOf("puppy")).none { it.id == "puppy" })
    }

    private fun run(answers: List<Boolean>): LevelTest {
        val test = LevelTest({ LevelTest.questions(it) }, Random(7))
        for (passed in answers) {
            if (test.current == null) break
            test.answer(passed)
        }
        return test
    }

    @Test
    fun `두 문제를 다 맞히면 올라가고 하나라도 틀리면 그 레벨에서 끝난다`() {
        // 아무것도 모르면 2문제 만에 레벨 1
        run(listOf(false, false)).let {
            assertEquals(1, it.result)
            assertEquals(2, it.asked)
            assertTrue(it.known.isEmpty())
        }
        // 레벨 1·2를 다 맞히고 레벨 3에서 하나 틀리면 레벨 3
        run(listOf(true, true, true, true, true, false)).let {
            assertEquals(3, it.result)
            assertEquals(6, it.asked)
            assertEquals(5, it.known.size)
            assertNull(it.current)
        }
        // 한 문제만 답했으면 아직 끝나지 않음
        run(listOf(true)).let {
            assertNull(it.result)
            assertEquals(1, it.level)
        }
        // 열 문제를 다 맞히면 레벨 5
        run(List(10) { true }).let {
            assertEquals(5, it.result)
            assertEquals(LevelTest.MAX_QUESTIONS, it.asked)
            assertEquals(10, it.known.size)
            assertEquals(10, it.known.map { w -> w.id }.toSet().size)
        }
    }
}

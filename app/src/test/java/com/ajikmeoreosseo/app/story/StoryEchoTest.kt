package com.ajikmeoreosseo.app.story

import com.ajikmeoreosseo.app.english.WordBank
import org.junit.Assert.assertEquals
import org.junit.Test

class StoryEchoTest {

    private val moon = WordBank.find("moon")!!
    private val star = WordBank.find("star")!!

    private fun echoes(script: List<Beat>) = script.filterIsInstance<Beat.Echo>().map { it.english }

    @Test
    fun echoFollowsTheSentenceThatHasTheWord() {
        val script = listOf(
            Beat.Chapter("1장"),
            Beat.Say("저기 하늘에 달이 둥실 떠 있어."),
            Beat.Say("별도 반짝반짝 빛나."),
        )
        val woven = StoryEcho.weave(script, listOf(moon, star), perChapter = 2)
        assertEquals(listOf("Moon", "Star"), echoes(woven))
        assertEquals(Beat.Echo("Moon"), woven[2])
    }

    @Test
    fun wordInsideAnotherWordIsNotAnEcho() {
        val script = listOf(Beat.Chapter("1장"), Beat.Say("우리 같이 달리자!"), Beat.Say("별사탕이 쏟아졌어."))
        assertEquals(emptyList<String>(), echoes(StoryEcho.weave(script, listOf(moon, star))))
    }

    @Test
    fun limitsPerChapterAndNoRepeats() {
        val script = listOf(
            Beat.Chapter("1장"), Beat.Say("달이 떴어."), Beat.Say("별이 떴어."),
            Beat.Chapter("2장"), Beat.Say("달이 웃어."), Beat.Say("별이 웃어."),
        )
        assertEquals(listOf("Moon", "Star"), echoes(StoryEcho.weave(script, listOf(moon, star), perChapter = 1)))
    }
}

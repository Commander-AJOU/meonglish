package com.ajikmeoreosseo.app.english

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 익힌 낱말로 만든 문장이 틀린 영어를 가르치지 않는지 확인합니다. */
class SentencesTest {

    private val nouns = WordBank.words.filter { it.level == 1 && (it.tag == Tag.NOUN || it.tag == Tag.MASS) }
    private val adjectives = WordBank.words.filter { it.level == 1 && it.tag == Tag.ADJ }
    private val colors = WordBank.words.filter { it.level == 1 && it.tag == Tag.COLOR }

    private fun word(id: String) = WordBank.find(id) ?: error("단어장에 $id 가 없습니다")
    private fun template(key: String) = Sentences.templates.first { it.key == key }
    private fun english(key: String, noun: String, mod: String? = null) =
        Sentences.make(template(key), word(noun), mod?.let(::word)).english

    @Test
    fun articleFollowsTheNextWord() {
        assertEquals("A red apple", english("color", "apple", "red"))
        assertEquals("An orange car", english("color", "car", "orange"))
        assertEquals("A big elephant", english("adj", "elephant", "big"))
        assertEquals("An old house", english("adj", "house", "old"))
        assertEquals("A big dog", english("adj", "dog", "big"))
        assertEquals("I see an apple.", english("see", "apple"))
        assertEquals("What an old house!", english("what", "house", "old"))
    }

    @Test
    fun articleFollowsTheSoundNotTheLetter() {
        assertEquals("I see a unicorn.", english("see", "unicorn"))
        assertEquals("This is a uniform.", english("thisis", "uniform"))
        assertEquals("I have an umbrella.", english("have", "umbrella"))
        assertTrue(Sentences.startsWithVowelSound("hour"))
        assertTrue(!Sentences.startsWithVowelSound("one"))
    }

    /** 모든 낱말·꾸밈말 짝에서 a/an이 바로 뒤 낱말의 첫소리와 맞는지. */
    @Test
    fun everyGeneratedArticleIsRight() {
        val article = Regex("""\b([Aa]n?) ([A-Za-z-]+)""")
        var checked = 0
        for (template in Sentences.templates) {
            val mods = when (template.needs) {
                Sentences.Needs.NONE -> listOf(null)
                Sentences.Needs.ADJ -> adjectives
                Sentences.Needs.COLOR -> colors
            }
            for (noun in nouns.filter(Sentences::usable)) for (mod in mods) {
                val text = Sentences.make(template, noun, mod).english
                for (match in article.findAll(text)) {
                    val wantsAn = Sentences.startsWithVowelSound(match.groupValues[2])
                    assertEquals("틀린 관사: $text", wantsAn, match.groupValues[1].lowercase() == "an")
                    checked++
                }
            }
        }
        assertTrue("검사한 문장이 너무 적습니다: $checked", checked > 10_000)
    }

    /** 만든 문장이 손으로 쓴 표현·문장과 겹치지 않는지. */
    @Test
    fun generatedSentencesDoNotRepeatHandWrittenOnes() {
        fun plain(s: String) = s.lowercase().filter { it.isLetterOrDigit() || it == ' ' }.trim()
        val handWritten = WordBank.words.filter { it.level >= 2 }.map { plain(it.english) }.toSet()
        val made = Sentences.candidates(nouns, adjectives, colors, level = 3, seen = emptySet()).toList()
        assertTrue(made.size > 1_000)
        val repeated = made.filter { plain(it.english) in handWritten }
        assertTrue("겹치는 문장: ${repeated.take(5).map { it.english }}", repeated.isEmpty())
        assertTrue("My family" !in made.map { it.english })
    }
}

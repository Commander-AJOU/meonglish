package com.ajikmeoreosseo.app.english

import kotlin.random.Random

/**
 * 영어 난이도 다섯 단계(레벨 1~5).
 *
 *   1 첫 단어        이야기·창밖·동물·먹을 것·색깔·수의 쉬운 단어 (Puppy, Apple, Red)
 *   2 단어 넓히기    꾸미는 말, 움직이는 말, 몸·집·자연·사람까지 모든 단어 (Big, Jump, Happy)
 *   3 짧은 표현      Let's fly!, A red dog
 *   4 문장           I see a dog.
 *   5 묻고 답하기    Where is the dog? 같은 묻는 말과 다섯 낱말 이상의 긴 문장
 *
 * 단어장([WordBank])의 말에는 종류(1 낱말, 2 짧은 표현, 3 문장)만 적혀 있고, 난이도는 여기서 규칙으로 정합니다.
 * 그래서 말을 더해도 난이도를 따로 적을 필요가 없고, 익힌 낱말로 만든 문장([Sentences])에도 같은 규칙이 통합니다.
 */
object Levels {
    const val MAX = 5

    val NAMES = listOf("첫 단어", "단어 넓히기", "짧은 표현", "문장", "묻고 답하기")

    val HINTS = listOf(
        "레벨 1: Puppy, Apple, Red 같은 쉬운 단어부터 배워요.",
        "레벨 2: Big, Jump, Happy처럼 꾸미는 말과 움직이는 말까지 배워요.",
        "레벨 3: Let's fly!, A red dog 같은 짧은 표현을 배워요.",
        "레벨 4: I see a dog. 같은 문장을 배워요.",
        "레벨 5: Where is the dog? 같은 묻는 말과 긴 문장을 배워요.",
    )

    /** 아이가 가장 먼저 만나는, 눈에 보이고 셀 수 있는 것들의 주제. */
    private val EASY_TOPICS = setOf("이야기에 나오는 것", "창밖에 보이는 것", "동물", "먹을 것", "색깔", "수")

    /** [word]의 난이도(1~5). */
    fun of(word: Word): Int = when (word.level) {
        1 -> if (word.topic.isEmpty() || word.topic in EASY_TOPICS) 1 else 2
        2 -> 3
        else -> if (word.english.trimEnd().endsWith("?") || word.english.trim().split(' ').size >= 5) 5 else 4
    }

    /** [level]에서 주로 배우는 말의 종류: 1 낱말, 2 짧은 표현, 3 문장. */
    fun kind(level: Int): Int = when {
        level <= 2 -> 1
        level == 3 -> 2
        else -> 3
    }
}

/**
 * 레벨 테스트: 아이의 영어 수준을 짧게 알아봅니다.
 *
 * 레벨 1부터 한 레벨에 두 문제씩, 강아지가 우리말을 말하면 아이가 영어로 대답합니다.
 * 두 문제를 다 맞히면 다음 레벨로 올라가고, 하나라도 못 맞히면 거기서 멈춥니다. 멈춘 레벨이 아이가 배울 레벨입니다
 * (레벨 5까지 다 맞히면 레벨 5). 그래서 가장 짧으면 2문제, 가장 길면 10문제입니다.
 * 맞힌 말은 이미 아는 말이므로 [known]에 모아, 끝난 뒤 다시 가르치지 않게 합니다.
 *
 * [pool]은 레벨마다 낼 수 있는 문제들입니다([questions]). [startLevel]부터 시작할 수도 있습니다(Claude가 출제하다가
 * 통신이 끊겨 이어받을 때, [ClaudeExaminer]).
 *
 * 설정에 Anthropic API 키가 있으면 이 규칙 대신 Claude가 아이의 대답을 보고 문제를 고릅니다([LevelExaminer]).
 */
class LevelTest(
    private val pool: (level: Int) -> List<Word>,
    private val random: Random = Random.Default,
    startLevel: Int = 1,
) {

    // init의 enter(1)보다 먼저 만들어져야 하므로 맨 위에 둡니다.
    private var pending = ArrayDeque<Word>()
    private var passedHere = 0

    /** 지금 묻는 레벨. */
    var level = 1
        private set

    /** 지금까지 물은 문제 수. */
    var asked = 0
        private set

    /** 끝났으면 아이에게 맞는 레벨, 아직이면 null. */
    var result: Int? = null
        private set

    /** 아이가 맞힌 말. */
    val known = ArrayList<Word>()

    /** 지금 물을 말. 끝났으면 null. */
    var current: Word? = null
        private set

    init {
        enter(startLevel.coerceIn(1, Levels.MAX))
    }

    /** 지금 문제의 결과를 적고 다음 문제로 넘어갑니다. */
    fun answer(passed: Boolean) {
        val word = current ?: return
        asked += 1
        if (passed) {
            passedHere += 1
            known += word
        }
        if (pending.isNotEmpty()) {
            current = pending.removeFirst()
            return
        }
        if (passedHere >= PER_LEVEL && level < Levels.MAX) enter(level + 1) else finish(level)
    }

    private fun enter(next: Int) {
        level = next
        passedHere = 0
        pending = ArrayDeque(pool(next).shuffled(random).take(PER_LEVEL))
        // 낼 문제가 없는 레벨이면(다 배웠으면) 거기서 끝냅니다.
        current = pending.removeFirstOrNull() ?: return finish(next)
    }

    private fun finish(at: Int) {
        current = null
        result = at
    }

    companion object {
        const val PER_LEVEL = 2
        const val MAX_QUESTIONS = PER_LEVEL * Levels.MAX

        /** 주제마다 앞쪽(쉬운 쪽)에서 이만큼만 문제로 냅니다. 낯선 낱말로 수준을 낮게 재지 않으려는 것입니다. */
        private const val PER_TOPIC = 6

        /**
         * [level]에서 문제로 낼 수 있는 말. 차의 움직임과 묶인 말과 이미 만난 말([seen])은 빼고,
         * 낱말은 주제마다 앞쪽의 흔한 것만, 표현과 문장은 손으로 쓴 것만 냅니다.
         */
        fun questions(level: Int, seen: Set<String> = emptySet()): List<Word> {
            val words = WordBank.words.filter { it.cue == null && it.id !in seen && Levels.of(it) == level }
            if (level > 2) return words
            return words.groupBy { it.topic }.values.flatMap { it.take(PER_TOPIC) }
        }
    }
}

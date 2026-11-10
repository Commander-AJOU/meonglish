package com.ajikmeoreosseo.app.english

/**
 * 지금 차 안팎의 상황. 새로 배울 낱말을 고를 때 씁니다.
 *
 * @param raining 와이퍼가 켜져 있는지
 * @param hour 지금 시각(0~23)
 * @param month 지금 달(1~12)
 * @param theme 오늘의 이야기 주제
 * @param destination 내비 목적지 이름(없으면 빈 문자열)
 */
data class Situation(
    val raining: Boolean = false,
    val hour: Int = 12,
    val month: Int = 6,
    val theme: String = "",
    val destination: String = "",
)

/**
 * 새로 배울 낱말의 순서를 정합니다. 천 개가 넘는 낱말을 적힌 순서대로 내지 않고,
 *
 *  1) 지금 상황에 맞는 낱말을 먼저 냅니다(최대 [MAX_SITUATIONAL]개, 이유마다 하나씩):
 *     가는 곳(목적지 이름에 든 말) → 비 → 오늘 이야기에 나오는 말 → 시간대 → 계절.
 *     아이가 지금 보고 듣는 것과 이어져야 귀에 들어오기 때문입니다.
 *  2) 나머지는 주제를 돌아가며 냅니다. 지금까지 가장 적게 배운 주제에서 하나씩 꺼내므로
 *     동물만 서른 개씩 이어지지 않고, 색깔·형용사가 일찍 섞여 익힌 낱말로 문장([Sentences])을 만들 수 있게 됩니다.
 */
object WordPicker {

    /** 고른 낱말과 고른 이유(상황에 맞아서 골랐을 때만). */
    class Pick(val word: Word, val reason: String?)

    private const val MAX_SITUATIONAL = 2

    fun order(unseen: List<Word>, learnedPerTopic: Map<String, Int>, situation: Situation): List<Pick> {
        if (unseen.isEmpty()) return emptyList()
        val byId = unseen.associateBy { it.id }
        val chosen = LinkedHashMap<String, Pick>()

        fun take(ids: List<String>, reason: String) {
            if (chosen.size >= MAX_SITUATIONAL) return
            val word = ids.firstNotNullOfOrNull { id -> byId[id].takeIf { id !in chosen } } ?: return
            chosen[word.id] = Pick(word, reason)
        }

        placeWord(unseen, situation.destination)?.let { take(listOf(it.id), "${Korean.plain(it.korean)}에 가는 길이라서") }
        if (situation.raining) take(RAIN, "비가 와서")
        take(themeWords(unseen, situation.theme).map { it.id }, "오늘 이야기에 나와서")
        when (situation.hour) {
            in 6..9 -> take(MORNING, "아침이라서")
            in 17..18 -> take(EVENING, "저녁이라서")
            in 19..23, in 0..5 -> take(NIGHT, "밤이라서")
        }
        when (situation.month) {
            12, 1, 2 -> take(WINTER, "겨울이라서")
            in 3..5 -> take(SPRING, "봄이라서")
            in 6..8 -> take(SUMMER, "여름이라서")
            else -> take(AUTUMN, "가을이라서")
        }

        return chosen.values + rotate(unseen.filter { it.id !in chosen }, learnedPerTopic).map { Pick(it, null) }
    }

    /** 가장 적게 배운 주제에서 하나씩 번갈아 꺼냅니다. 주제 안에서는 적힌 순서(쉬운 것부터)를 지킵니다. */
    private fun rotate(words: List<Word>, learnedPerTopic: Map<String, Int>): List<Word> {
        val queues = LinkedHashMap<String, ArrayDeque<Word>>()
        for (word in words) queues.getOrPut(word.topic) { ArrayDeque() }.addLast(word)
        val counts = HashMap(learnedPerTopic)
        val out = ArrayList<Word>(words.size)
        while (queues.isNotEmpty()) {
            val topic = queues.keys.minByOrNull { counts[it] ?: 0 } ?: break
            val queue = queues.getValue(topic)
            out += queue.removeFirst()
            counts[topic] = (counts[topic] ?: 0) + 1
            if (queue.isEmpty()) queues.remove(topic)
        }
        return out
    }

    /** 목적지 이름에 들어 있는 낱말("서울숲공원" → 공원, "할머니 댁" → 할머니). 가장 긴 것을 고릅니다. */
    private fun placeWord(unseen: List<Word>, destination: String): Word? {
        if (destination.isBlank()) return null
        return unseen
            .filter { it.tag == Tag.NOUN && Korean.plain(it.korean).length >= 2 && destination.contains(Korean.plain(it.korean)) }
            .maxByOrNull { Korean.plain(it.korean).length }
    }

    /** 오늘 이야기 주제에 나오는 낱말("눈 내리는 구름 마을의 눈사람" → 마을, 눈사람). 주제에 나온 순서대로. */
    private fun themeWords(unseen: List<Word>, theme: String): List<Word> {
        if (theme.isBlank()) return emptyList()
        val byKorean = unseen.groupBy { Korean.plain(it.korean) }
        val found = LinkedHashSet<Word>()
        for (raw in theme.split(' ')) {
            val token = raw.trim().trim(',', '.', '!', '?')
            val stem = PARTICLES.firstOrNull { token.length > it.length && token.endsWith(it) }?.let { token.dropLast(it.length) } ?: token
            for (candidate in listOf(token, stem)) byKorean[candidate]?.let { found += it }
            // 붙여 쓴 말 안의 두 글자 이상 낱말(기차역 → 기차, 생일잔치 → 생일)
            if (stem.length >= 3) {
                byKorean.keys.filter { it.length >= 2 && it != stem && (stem.startsWith(it) || stem.endsWith(it)) }
                    .forEach { key -> found += byKorean.getValue(key) }
            }
        }
        return found.toList()
    }

    private val PARTICLES = listOf("에서", "으로", "이랑", "들의", "들이", "들", "의", "이", "가", "은", "는", "을", "를", "에", "로", "와", "과")

    private val RAIN = listOf("umbrella", "puddle", "raincoat", "boot", "cloud", "wet", "rainy", "thunder", "wiper", "storm", "water")
    private val MORNING = listOf("sun", "morning", "sunrise", "breakfast", "sunshine", "kindergarten", "school", "bright", "backpack")
    private val EVENING = listOf("sunset", "evening", "dinner", "streetlight", "tired")
    private val NIGHT = listOf("moon", "star", "night", "moonlight", "dark", "sleepy", "streetlight", "lantern", "owl", "firefly", "dream", "light")
    private val SPRING = listOf("flower", "butterfly", "spring", "warm", "cherry_blossom", "tulip", "bee", "seed", "picnic", "frog")
    private val SUMMER = listOf("sea", "hot", "ice_cream", "watermelon", "summer", "beach", "swimsuit", "fan", "mosquito", "swim", "shade")
    private val AUTUMN = listOf("leaf", "fall", "acorn", "chestnut", "persimmon", "wind", "dragonfly", "pumpkin", "sweet_potato", "squirrel", "cool")
    private val WINTER = listOf("snow", "snowman", "cold", "coat", "glove", "scarf", "mitten", "winter", "ice", "snowflake", "sled", "icicle", "chilly")
}

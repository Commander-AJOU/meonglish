package com.ajikmeoreosseo.app.english

/**
 * 아이가 이미 익힌 낱말(꿈 구슬)을 엮어 새 표현과 문장을 만듭니다.
 *
 * 낱말은 끝이 있지만, 익힌 낱말끼리 짝지은 문장은 거의 끝이 없습니다. 예를 들어 Dog와 Big을 익혔다면
 * "A big dog", "The dog is big.", "I see a dog." 같은 말이 새로 나옵니다.
 * 만든 말의 id는 "g:틀:낱말[:꾸밈말]" 꼴이라 저장된 진도에서 다시 만들어 낼 수 있습니다([decode]).
 */
object Sentences {

    /** 꾸밈말이 필요한지. */
    enum class Needs { NONE, ADJ, COLOR }

    class Template(
        val key: String,
        val level: Int,
        val needs: Needs,
        val english: (noun: Word, mod: Word?) -> String,
        val korean: (noun: Word, mod: Word?) -> String,
    )

    /**
     * [noun] 앞에 붙는 관사("a " 또는 "an ", 셀 수 없는 명사면 없음).
     * a/an은 바로 뒤에 오는 말의 첫소리로 정하므로, 꾸밈말이 사이에 끼면 [next]에 그 꾸밈말을 넘깁니다
     * (a red apple, an old house).
     */
    private fun a(noun: Word, next: Word = noun): String {
        if (noun.tag == Tag.MASS) return ""
        return if (startsWithVowelSound(next.english)) "an " else "a "
    }

    /** 글자가 아니라 소리로 봅니다: a unicorn, a uniform, a one / an hour. */
    internal fun startsWithVowelSound(english: String): Boolean {
        val word = english.trim().lowercase()
        if (word.isEmpty()) return false
        if (CONSONANT_SOUND.any { word.startsWith(it) }) return false
        if (VOWEL_SOUND.any { word.startsWith(it) }) return true
        return word[0] in "aeiou"
    }

    private val CONSONANT_SOUND = listOf("uni", "use", "usu", "ute", "eu", "one", "once", "ukulele", "ufo")
    private val VOWEL_SOUND = listOf("hour", "honest", "honor", "heir", "x-ray")

    /** 문장 속 낱말: 보통 소문자, TV처럼 약자는 그대로. */
    private fun n(noun: Word): String =
        if (noun.english == noun.english.uppercase()) noun.english else noun.english.lowercase()

    private fun ko(noun: Word): String = Korean.plain(noun.korean)

    val templates: List<Template> = listOf(
        // ── 레벨 2: 짧은 표현 ──
        Template("color", 2, Needs.COLOR,
            { noun, mod -> "${a(noun, mod!!)}${n(mod)} ${n(noun)}".trim().replaceFirstChar { it.uppercase() } },
            { noun, mod -> "${mod!!.attr} ${ko(noun)}" }),
        Template("adj", 2, Needs.ADJ,
            { noun, mod -> "${a(noun, mod!!)}${n(mod)} ${n(noun)}".trim().replaceFirstChar { it.uppercase() } },
            { noun, mod -> "${mod!!.attr} ${ko(noun)}" }),
        Template("my", 2, Needs.NONE,
            { noun, _ -> "My ${n(noun)}" },
            { noun, _ -> "내 ${ko(noun)}" }),
        Template("look", 2, Needs.NONE,
            { noun, _ -> "Look, ${a(noun)}${n(noun)}!" },
            { noun, _ -> "봐, ${ko(noun)}야!".replace("야!", if (Korean.hasBatchim(noun.korean)) "이야!" else "야!") }),

        // ── 레벨 3: 문장 ──
        Template("see", 3, Needs.NONE,
            { noun, _ -> "I see ${a(noun)}${n(noun)}." },
            { noun, _ -> "${Korean.subject(noun.korean)} 보여요." }),
        Template("is", 3, Needs.ADJ,
            { noun, mod -> "The ${n(noun)} is ${n(mod!!)}." },
            { noun, mod -> "${Korean.subject(noun.korean)} ${mod!!.pred}." }),
        Template("iscolor", 3, Needs.COLOR,
            { noun, mod -> "The ${n(noun)} is ${n(mod!!)}." },
            { noun, mod -> "${Korean.topic(noun.korean)} ${colorName(mod!!)}이에요." }),
        Template("like", 3, Needs.NONE,
            { noun, _ -> "I like the ${n(noun)}." },
            { noun, _ -> "나는 ${Korean.subject(noun.korean)} 좋아요." }),
        Template("where", 3, Needs.NONE,
            { noun, _ -> "Where is the ${n(noun)}?" },
            { noun, _ -> "${Korean.topic(noun.korean)} 어디 있어?" }),
        Template("lookat", 3, Needs.NONE,
            { noun, _ -> "Look at the ${n(noun)}!" },
            { noun, _ -> "${Korean.obj(noun.korean)} 봐!" }),
        Template("canyousee", 3, Needs.NONE,
            { noun, _ -> "Can you see the ${n(noun)}?" },
            { noun, _ -> "${Korean.subject(noun.korean)} 보여?" }),
        Template("have", 3, Needs.NONE,
            { noun, _ -> "I have ${a(noun)}${n(noun)}." },
            { noun, _ -> "나는 ${Korean.subject(noun.korean)} 있어요." }),

        // ── 더한 틀 ──
        Template("very", 2, Needs.ADJ,
            { noun, mod -> (if (noun.tag == Tag.MASS) "Very ${n(mod!!)} ${n(noun)}" else "A very ${n(mod!!)} ${n(noun)}") },
            { noun, mod -> "아주 ${mod!!.attr} ${ko(noun)}" }),
        Template("thisis", 3, Needs.NONE,
            { noun, _ -> "This is ${a(noun)}${n(noun)}." },
            { noun, _ -> "이것은 ${ko(noun)}${if (Korean.hasBatchim(noun.korean)) "이에요" else "예요"}." }),
        Template("thereis", 3, Needs.NONE,
            { noun, _ -> "There is ${a(noun)}${n(noun)}." },
            { noun, _ -> "${Korean.subject(noun.korean)} 있어요." }),
        Template("want", 3, Needs.NONE,
            { noun, _ -> "I want ${a(noun)}${n(noun)}." },
            { noun, _ -> "나는 ${Korean.obj(noun.korean)} 갖고 싶어요." }),
        Template("here", 3, Needs.NONE,
            { noun, _ -> "Here is the ${n(noun)}." },
            { noun, _ -> "여기 ${Korean.subject(noun.korean)} 있어요." }),
        Template("find", 3, Needs.NONE,
            { noun, _ -> "Let's find the ${n(noun)}." },
            { noun, _ -> "${Korean.obj(noun.korean)} 찾아보자." }),
        Template("doyoulike", 3, Needs.NONE,
            { noun, _ -> "Do you like the ${n(noun)}?" },
            { noun, _ -> "${Korean.obj(noun.korean)} 좋아해?" }),
        Template("what", 3, Needs.ADJ,
            { noun, mod -> (if (noun.tag == Tag.MASS) "What ${n(mod!!)} ${n(noun)}!" else "What ${a(noun, mod!!)}${n(mod)} ${n(noun)}!") },
            { noun, mod -> "정말 ${mod!!.attr} ${ko(noun)}${if (Korean.hasBatchim(noun.korean)) "이다" else "다"}!" }),
        Template("isit", 3, Needs.COLOR,
            { noun, mod -> "Is the ${n(noun)} ${n(mod!!)}?" },
            { noun, mod -> "${Korean.topic(noun.korean)} ${colorName(mod!!)}이야?" }),
    )

    /** 색깔의 이름꼴: 빨간 → 빨간색, 갈색 → 갈색. */
    private fun colorName(color: Word): String = if (color.attr.endsWith("색")) color.attr else color.attr + "색"

    /**
     * 문장에 넣을 수 있는 낱말인지. 때를 나타내는 말(Monday, Time), 셀 수 없는 생각(Weather, Magic),
     * 늘 여럿으로 쓰는 말(Pants, Scissors)은 "I see a Monday.", "The pants is big."처럼 틀린 문장이 되므로 뺍니다.
     */
    fun usable(noun: Word): Boolean = noun.topic !in SKIP_TOPICS && noun.id !in SKIP_IDS

    private val SKIP_TOPICS = setOf("때와 학교", "수")
    private val SKIP_IDS = setOf(
        "pants", "scissors", "shorts", "pajamas", "glasses", "sunglasses", "headphones", "chopsticks", "clothes",
        "french_fries", "fireworks", "weather", "season", "air", "speed", "traffic_jam", "magic", "secret", "wish",
        "adventure", "night", "half", "pair", "voice", "skin", "country", "world", "space", "earth", "team",
        "navigation", "breakfast", "lunch", "dinner", "people",
    )

    private val byKey = templates.associateBy { it.key }

    /** 손으로 쓴 표현·문장의 영어(대소문자와 문장부호를 뺀 것). 만든 문장이 이와 같으면 내지 않습니다. */
    private val handWritten: Set<String> by lazy {
        WordBank.words.filter { it.level >= 2 }.map { plain(it.english) }.toSet()
    }

    private fun plain(english: String): String = english.lowercase().filter { it.isLetterOrDigit() || it == ' ' }.trim()

    fun make(template: Template, noun: Word, mod: Word?): Word {
        val id = buildString {
            append("g:").append(template.key).append(':').append(noun.id)
            if (mod != null) append(':').append(mod.id)
        }
        return Word(id, template.english(noun, mod), template.korean(noun, mod), template.level, tag = Tag.SENTENCE)
    }

    /** 저장된 id("g:틀:낱말[:꾸밈말]")에서 문장을 다시 만듭니다. 낱말이 단어장에서 사라졌으면 null. */
    fun decode(id: String): Word? {
        if (!id.startsWith("g:")) return null
        val parts = id.split(':')
        if (parts.size !in 3..4) return null
        val template = byKey[parts[1]] ?: return null
        val noun = WordBank.find(parts[2]) ?: return null
        val mod = parts.getOrNull(3)?.let { WordBank.find(it) ?: return null }
        if ((template.needs != Needs.NONE) != (mod != null)) return null
        return make(template, noun, mod)
    }

    /**
     * 아직 안 만난 새 문장을 차례로 내놓습니다. 익힌 낱말 순서대로, 틀을 돌려 가며 짝짓습니다.
     * [level]보다 어려운 틀은 쓰지 않습니다.
     */
    fun candidates(
        nouns: List<Word>,
        adjectives: List<Word>,
        colors: List<Word>,
        level: Int,
        seen: Set<String>,
    ): Sequence<Word> = sequence {
        val usable = templates.filter { it.level <= level && it.level >= 2 }
        @Suppress("NAME_SHADOWING") val nouns = nouns.filter(::usable)
        if (usable.isEmpty() || nouns.isEmpty()) return@sequence
        // 같은 낱말이 연달아 나오지 않도록, 바깥 고리는 틀, 안쪽 고리는 낱말로 돕니다.
        for (round in usable.indices) {
            for ((i, noun) in nouns.withIndex()) {
                val template = usable[(i + round) % usable.size]
                val mod = when (template.needs) {
                    Needs.NONE -> null
                    Needs.ADJ -> adjectives.getOrNull((i + round) % adjectives.size.coerceAtLeast(1)) ?: continue
                    Needs.COLOR -> colors.getOrNull((i + round) % colors.size.coerceAtLeast(1)) ?: continue
                }
                if (template.needs != Needs.NONE && (mod == null || mod.tag == Tag.MASS)) continue
                // 색깔 낱말에 색깔을 또 붙이거나, 수식어를 명사처럼 쓰지 않습니다.
                if (noun.tag != Tag.NOUN && noun.tag != Tag.MASS) continue
                val word = make(template, noun, mod)
                // 손으로 쓴 말과 같은 문장("My family", "Where is the puppy?")은 두 번 배우지 않습니다.
                if (word.id !in seen && plain(word.english) !in handWritten) yield(word)
            }
        }
    }
}

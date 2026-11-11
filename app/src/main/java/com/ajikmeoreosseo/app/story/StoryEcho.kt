package com.ajikmeoreosseo.app.story

import com.ajikmeoreosseo.app.english.Korean
import com.ajikmeoreosseo.app.english.Tag
import com.ajikmeoreosseo.app.english.Word

/**
 * 우리말 이야기 속에 아이가 아는 영어를 메아리처럼 섞습니다.
 *
 * 이야기 문장에 아이가 배운(또는 오늘 배울) 낱말이 나오면, 그 문장 바로 뒤에 영어 한마디를 덧붙입니다.
 *   "저기 하늘에 달이 둥실 떠 있어." → "Moon!"
 * 문장 안의 낱말을 영어로 바꾸지 않고 따로 말하므로 조사가 깨지지 않고, 차량 기본 음성도 영어 문장으로 읽습니다.
 * 수업 시간 말고도 이야기를 듣는 내내 영어를 만나게 하려는 것입니다. 익힌 낱말이 늘수록 메아리도 늘어납니다.
 */
object StoryEcho {

    /** 한 장에 메아리는 [perChapter]개까지, 한 이야기에 [total]개까지. 같은 낱말은 한 번만. */
    fun weave(script: List<Beat>, words: List<Word>, perChapter: Int = 1, total: Int = 8): List<Beat> {
        val candidates = words.filter { it.tag == Tag.NOUN || it.tag == Tag.MASS }
            .distinctBy { it.id }
            .map { it to pattern(Korean.plain(it.korean)) }
            .filter { (word, _) -> Korean.plain(word.korean).isNotEmpty() }
        if (candidates.isEmpty()) return script

        val out = ArrayList<Beat>(script.size + total)
        val used = HashSet<String>()
        var inChapter = 0
        var count = 0
        for (beat in script) {
            out += beat
            when (beat) {
                is Beat.Chapter -> inChapter = 0
                is Beat.Say -> {
                    if (beat.needsName || count >= total || inChapter >= perChapter) continue
                    val hit = candidates.firstOrNull { (word, regex) -> word.id !in used && regex.containsMatchIn(beat.text) }
                        ?: continue
                    used += hit.first.id
                    inChapter += 1
                    count += 1
                    out += Beat.Echo(hit.first.english)
                }
                else -> Unit
            }
        }
        return out
    }

    /**
     * 낱말이 이야기 문장에 낱말로서 나왔는지. 앞은 띄어쓰기나 따옴표, 뒤는 조사가 붙거나 바로 끝나야 합니다.
     * ("달이 떴어"는 맞고 "달리자"는 아님.)
     */
    private fun pattern(korean: String): Regex {
        val word = Regex.escape(korean)
        return Regex("""(^|[\s"'‘“(])$word($PARTICLES)?(?=[\s.,!?~…'"’”)]|$)""")
    }

    private const val PARTICLES =
        "이랑|랑|에서|에게|한테|처럼|까지|보다|부터|으로|로|이가|이는|이를|이야|이에요|예요|이|가|은|는|을|를|에|의|도|와|과|아|야|만"
}

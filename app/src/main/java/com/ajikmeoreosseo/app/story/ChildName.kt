package com.ajikmeoreosseo.app.story

/**
 * 이름을 이야기 문장에 자연스럽게 끼워 넣습니다.
 *
 * 한국어는 이름 끝에 받침이 있느냐에 따라 부르는 말과 조사가 달라집니다.
 *   서준 → 서준아, 서준이가, 서준이는     (받침 있음: 이름 뒤에 '이'를 붙여 부름)
 *   지우 → 지우야, 지우가, 지우는         (받침 없음)
 * 아이 이름을 정하지 않았으면 "우리 친구"로 부릅니다.
 *
 * 대본에서는 아래 표시를 씁니다([token]이 "이름"일 때. 강아지 이름은 "멍"을 씁니다: {멍}, {멍아} …).
 *   {이름}   서준이 / 지우 / 우리 친구       {이름아}   서준아 / 지우야 / 친구야
 *   {이름이} 서준이가 / 지우가               {이름은}   서준이는 / 지우는
 *   {이름을} 서준이를 / 지우를               {이름의}   서준이의 / 지우의
 *   {이름이랑} 서준이랑 / 지우랑             {이름한테} 서준이한테 / 지우한테
 *   {첫글자} 이름의 첫 글자                  {글자수}   이름 글자 수(한, 두, 세 …)
 */
class ChildName(
    raw: String?,
    private val token: String = "이름",
    fallback: String = "우리 친구",
    fallbackCalling: String = "친구야",
) {

    /** 화면에 보여 줄 이름. 정하지 않았으면 빈 문자열. */
    val given: String = clean(raw)

    val isSet: Boolean get() = given.isNotEmpty()

    private val endsWithBatchim = given.lastOrNull()?.let { it in '가'..'힣' && (it.code - 0xAC00) % 28 != 0 } == true

    /** 서준이 / 지우 / 우리 친구. 항상 모음으로 끝나므로 뒤에 붙는 조사가 하나로 정해집니다. */
    val friendly = when {
        !isSet -> fallback
        endsWithBatchim -> given + "이"
        else -> given
    }

    private val calling = when {
        !isSet -> fallbackCalling
        endsWithBatchim -> given + "아"
        else -> given + "야"
    }

    /** 대본의 표시를 실제 이름으로 바꿉니다. */
    fun render(template: String): String {
        if (!template.contains('{')) return template
        val named = template
            .replace("{${token}아}", calling)
            .replace("{${token}이랑}", friendly + "랑")
            .replace("{${token}한테}", friendly + "한테")
            .replace("{${token}이}", friendly + "가")
            .replace("{${token}은}", friendly + "는")
            .replace("{${token}을}", friendly + "를")
            .replace("{${token}의}", friendly + "의")
            .replace("{$token}", friendly)
        // 글자 놀이는 아이 이름으로만 합니다.
        if (token != "이름") return named
        return named
            .replace("{첫글자}", given.take(1))
            .replace("{글자수}", countWord(given.length))
    }

    private fun countWord(n: Int): String = when (n) {
        1 -> "한"; 2 -> "두"; 3 -> "세"; 4 -> "네"; 5 -> "다섯"
        6 -> "여섯"; 7 -> "일곱"; 8 -> "여덟"; 9 -> "아홉"; 10 -> "열"
        else -> n.toString()
    }

    companion object {
        const val MAX_LENGTH = 10

        /** 글자와 숫자만 남기고 길이를 제한합니다. 음성 합성에 이상한 문자가 들어가지 않게 합니다. */
        fun clean(raw: String?): String =
            raw.orEmpty().filter { it.isLetterOrDigit() }.take(MAX_LENGTH)
    }
}

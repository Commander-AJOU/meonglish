package com.ajikmeoreosseo.app.story

import com.ajikmeoreosseo.app.english.Word

/** 문장을 읽을 언어. 차량 기본 음성은 언어에 맞춰 발음을 바꿉니다. */
enum class Lang { KO, EN }

/**
 * 구름 강아지가 한 번에 말하는 한 마디.
 *
 * @param pauseAfterMs 말한 뒤 기다리는 시간. 아이가 따라 말하거나 대답할 틈을 줍니다.
 * @param listenFor 말한 뒤 아이가 이 말을 따라 하거나 대답하는지 듣습니다(음성 인식). 들을 수 없는 차에서는 [pauseAfterMs]만큼 기다립니다.
 */
data class Line(
    val text: String,
    val lang: Lang = Lang.KO,
    val pauseAfterMs: Long = 0,
    val listenFor: Word? = null,
)

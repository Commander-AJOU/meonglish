package com.ajikmeoreosseo.app.english

/** 우리말 조사 붙이기. 낱말 끝 글자의 받침에 따라 이/가, 은/는, 을/를을 고릅니다. */
object Korean {

    /** "눈(얼굴)"처럼 괄호 설명이 붙은 낱말에서 낱말만 남깁니다. */
    fun plain(word: String): String = word.substringBefore('(').trim()

    fun hasBatchim(word: String): Boolean {
        val last = plain(word).lastOrNull { it in '가'..'힣' } ?: return false
        return (last.code - 0xAC00) % 28 != 0
    }

    fun subject(word: String): String = plain(word) + if (hasBatchim(word)) "이" else "가"
    fun topic(word: String): String = plain(word) + if (hasBatchim(word)) "은" else "는"
    fun obj(word: String): String = plain(word) + if (hasBatchim(word)) "을" else "를"
}

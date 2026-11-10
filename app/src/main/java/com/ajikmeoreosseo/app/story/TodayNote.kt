package com.ajikmeoreosseo.app.story

import android.content.Context
import java.time.LocalDate

/**
 * 어른이 설정에서 적어 둔 '오늘 있었던 일'. 그날의 이야기에 녹여 넣고([StoryGenerator]), 날짜가 바뀌면 저절로 사라집니다.
 */
class TodayNote(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** 오늘 적은 글. 없거나 지난 날짜의 글이면 null. */
    val text: String?
        get() = prefs.getString(KEY_TEXT, null)?.takeIf { prefs.getString(KEY_DATE, null) == LocalDate.now().toString() }

    /** 저장합니다. 비어 있으면 지웁니다. */
    fun save(raw: String?) {
        val cleaned = clean(raw)
        prefs.edit().putString(KEY_TEXT, cleaned.ifEmpty { null }).putString(KEY_DATE, LocalDate.now().toString()).apply()
    }

    companion object {
        const val MAX_LENGTH = 100
        private const val KEY_TEXT = "todayNote"
        private const val KEY_DATE = "todayNoteDate"

        /** 대본 표시나 따옴표와 헷갈릴 글자를 빼고 한 줄로 만듭니다. */
        fun clean(raw: String?): String =
            raw.orEmpty().replace(Regex("[{}\"<>]"), "").replace(Regex("\\s+"), " ").trim().take(MAX_LENGTH)
    }
}

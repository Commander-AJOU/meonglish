package com.ajikmeoreosseo.app.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.ajikmeoreosseo.app.BuildConfig
import com.ajikmeoreosseo.app.R
import com.ajikmeoreosseo.app.english.EnglishCoach
import com.ajikmeoreosseo.app.english.Levels

/**
 * 어른이 보는 영어 단어장. 차를 세웠을 때만 엽니다.
 * 아이 수준을 고르고, 배우는 말과 꿈 구슬(완전히 익힌 말)을 보고, 잘못 판단된 말을 바로잡습니다.
 */
object WordBookDialog {

    fun show(activity: Activity, coach: EnglishCoach) {
        val builder = AlertDialog.Builder(activity)
        val context = builder.context
        val density = context.resources.displayMetrics.density
        val pad = (28 * density).toInt()

        // 대화상자 테마가 밝든 어둡든 읽히도록 바탕과 글자색을 직접 정합니다.
        val sheet = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_sheet)
            setPadding(pad, pad, pad, pad)
        }
        val scroll = ScrollView(context).apply { addView(sheet) }

        fun render() {
            sheet.removeAllViews()
            val summary = coach.summary()
            val due = summary.learning.filter { it.dueToday }
            val later = summary.learning.filter { !it.dueToday }

            sheet.addView(text(context, "영어 단어장", 40f, bold = true))
            sheet.addView(
                text(
                    context,
                    "레벨 ${summary.level}(${Levels.NAMES[summary.level - 1]}) · 꿈 구슬 ${summary.mastered.size}개 · 배우는 중 ${summary.learning.size}개",
                    28f,
                )
            )
            sheet.addView(
                text(
                    context,
                    "서로 다른 날에 네 번 만나면 꿈 구슬이 돼요. 꿈 구슬이 된 말은 날마다의 복습에서 빠지고, 2주 뒤와 한 달 뒤에 한 번씩 다시 물어봐요.",
                    24f,
                    color = R.color.hint,
                )
            )

            sheet.addView(
                pill(context, if (summary.enabled) "영어 수업 켜짐" else "영어 수업 꺼짐", summary.enabled) {
                    coach.setEnabled(!summary.enabled)
                    render()
                }
            )

            sheet.addView(text(context, "아이 수준 (레벨 1~${Levels.MAX})", 28f, bold = true, topMargin = pad))
            val levels = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            Levels.NAMES.forEachIndexed { index, name ->
                val level = index + 1
                levels.addView(
                    pill(context, "$level $name", summary.level == level) {
                        coach.setLevel(level)
                        render()
                    },
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        if (index > 0) marginStart = pad / 3
                        topMargin = pad / 3
                    },
                )
            }
            sheet.addView(levels)
            sheet.addView(text(context, Levels.HINTS[summary.level - 1], 24f, color = R.color.hint))
            sheet.addView(
                text(context, "설정의 레벨 테스트로 알아볼 수도 있어요. 강아지가 새 모습으로 자랄 때마다 한 레벨씩 올라가요.", 24f, color = R.color.hint)
            )

            section(activity, context, sheet, "오늘 복습할 말", due, coach, ::render)
            section(activity, context, sheet, "배우는 중", later, coach, ::render)
            section(activity, context, sheet, "꿈 구슬이 된 말", summary.mastered, coach, ::render)
            if (summary.learning.isEmpty() && summary.mastered.isEmpty()) {
                sheet.addView(text(context, "아직 배운 말이 없어요. 이야기를 시작하면 구름 강아지가 가르쳐 줘요.", 26f, topMargin = pad))
            }

            if (BuildConfig.DEBUG) {
                sheet.addView(
                    pill(context, "시연용: 다음 날로 넘기기 (지금 +${summary.demoDayOffset}일)", false) {
                        coach.skipDayForDemo()
                        render()
                    }.also { (it.layoutParams as LinearLayout.LayoutParams).topMargin = pad }
                )
            }
        }

        render()
        builder.setView(scroll).setPositiveButton("닫기", null).show()
    }

    private fun section(
        activity: Activity,
        context: Context,
        sheet: LinearLayout,
        title: String,
        items: List<EnglishCoach.WordState>,
        coach: EnglishCoach,
        refresh: () -> Unit,
    ) {
        if (items.isEmpty()) return
        val pad = (28 * context.resources.displayMetrics.density).toInt()
        sheet.addView(text(context, "$title (${items.size})", 28f, bold = true, topMargin = pad))
        for (item in items) {
            val dots = "●".repeat(item.stage.coerceAtMost(EnglishCoach.STAGES_TO_MASTER)) +
                "○".repeat((EnglishCoach.STAGES_TO_MASTER - item.stage).coerceAtLeast(0))
            val row = text(context, "${item.word.english}   ${item.word.korean}   $dots", 26f, topMargin = pad / 3)
            row.setBackgroundResource(R.drawable.bg_pill)
            row.setPadding(pad / 2, pad / 3, pad / 2, pad / 3)
            row.setOnClickListener {
                AlertDialog.Builder(activity)
                    .setTitle(item.word.english)
                    .setMessage("${item.word.korean} · 레벨 ${Levels.of(item.word)}")
                    .setPositiveButton("이미 알아요") { _, _ ->
                        coach.markMastered(item.word)
                        refresh()
                    }
                    .setNeutralButton("다시 배우기") { _, _ ->
                        coach.forget(item.word)
                        refresh()
                    }
                    .setNegativeButton("닫기", null)
                    .show()
            }
            sheet.addView(row)
        }
    }

    private fun text(
        context: Context,
        value: String,
        size: Float,
        bold: Boolean = false,
        color: Int = R.color.ink,
        topMargin: Int = 0,
    ): TextView = TextView(context).apply {
        text = value
        textSize = size
        setTextColor(context.getColor(color))
        if (bold) typeface = Typeface.DEFAULT_BOLD
        setLineSpacing(0f, 1.2f)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { this.topMargin = topMargin }
    }

    private fun pill(context: Context, label: String, selected: Boolean, onClick: (View) -> Unit): TextView {
        val pad = (20 * context.resources.displayMetrics.density).toInt()
        return text(context, label, 26f, bold = selected, topMargin = pad / 2).apply {
            gravity = Gravity.CENTER
            setBackgroundResource(if (selected) R.drawable.bg_pill_selected else R.drawable.bg_pill)
            setPadding(pad / 2, pad / 2, pad / 2, pad / 2)
            setOnClickListener(onClick)
        }
    }

}

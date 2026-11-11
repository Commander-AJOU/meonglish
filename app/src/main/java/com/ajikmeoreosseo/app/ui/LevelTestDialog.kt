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
import com.ajikmeoreosseo.app.R
import com.ajikmeoreosseo.app.english.AnswerListener
import com.ajikmeoreosseo.app.english.EnglishCoach
import com.ajikmeoreosseo.app.english.ClaudeExaminer
import com.ajikmeoreosseo.app.english.LevelExaminer
import com.ajikmeoreosseo.app.english.LevelTest
import com.ajikmeoreosseo.app.english.TestAnswer
import com.ajikmeoreosseo.app.english.TestStep
import com.ajikmeoreosseo.app.english.Levels
import com.ajikmeoreosseo.app.english.Word
import com.ajikmeoreosseo.app.story.Narrator

/**
 * 레벨 테스트. 어른이 설정에서 열고(차를 세웠을 때만), 강아지가 묻고 아이가 영어로 대답합니다.
 *
 * 강아지가 우리말을 말하면("‘사과’. 영어로 뭐라고 할까?") 차가 아이의 대답을 듣고 맞았는지 봅니다.
 * 차 마이크가 뒷자리 소리를 못 들을 수 있으므로, 어른이 언제든 "알아요" / "몰라요"를 눌러 대신 정할 수 있습니다.
 * 못 들었을 때는 틀린 것으로 넘기지 않고 어른이 누를 때까지 기다립니다.
 *
 * 다음에 무엇을 물을지는 출제자([LevelExaminer])가 정합니다. Anthropic API 키가 있으면 Claude가 아이의 대답을 보고
 * 문제를 고르고 레벨과 부모에게 보여 줄 말을 정하고([ClaudeExaminer]), 없거나 통신이 안 되면 앱에 든 기본 테스트로 진행합니다.
 * 끝나면 결과 레벨을 바로 적용하고([EnglishCoach.applyLevelTest]), 맞힌 말은 꿈 구슬로 넣습니다.
 */
object LevelTestDialog {

    /**
     * [render]는 대사의 {이름}·{멍} 표시를 아이 이름과 강아지 이름으로 바꿉니다.
     * [examiner]는 이미 만난 말(문제로 내지 않을 말)을 받아 출제자를 만듭니다. 닫히면 [onClosed]를 부릅니다.
     */
    fun show(
        activity: Activity,
        coach: EnglishCoach,
        narrator: Narrator,
        listener: AnswerListener,
        render: (String) -> String,
        claudeReady: Boolean,
        examiner: (seen: Set<String>) -> LevelExaminer,
        onClosed: () -> Unit,
    ): AlertDialog {
        val builder = AlertDialog.Builder(activity)
        val context = builder.context
        val pad = (28 * context.resources.displayMetrics.density).toInt()
        val sheet = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_sheet)
            setPadding(pad, pad, pad, pad)
        }
        val dialog = builder.setView(ScrollView(context).apply { addView(sheet) }).create()

        var running: LevelExaminer? = null
        val history = ArrayList<TestAnswer>()
        // 화면이 바뀔 때마다 올려, 지난 화면의 말하기·듣기·출제 결과가 뒤늦게 와도 무시합니다.
        var turn = 0
        var closed = false
        var asking: TestStep.Ask? = null
        var lastHeard: String? = null

        fun quiet() {
            turn += 1
            listener.cancel()
            narrator.stop()
        }

        fun showResult(step: TestStep.Finish, known: List<Word>) {
            sheet.removeAllViews()
            sheet.addView(text(context, "레벨 테스트 결과", 40f, bold = true))
            sheet.addView(text(context, "레벨 ${step.level} · ${Levels.NAMES[step.level - 1]}", 56f, bold = true, topMargin = pad / 2, color = R.color.kid_blue))
            sheet.addView(text(context, Levels.HINTS[step.level - 1], 28f, topMargin = pad / 3))
            step.comment?.let { sheet.addView(text(context, "“$it”", 30f, topMargin = pad / 2)) }
            sheet.addView(
                text(
                    context,
                    (if (step.byClaude) "Claude가 아이의 대답을 보고 문제를 고르고 레벨을 정했어요. " else "") +
                        "이제 이 레벨에 맞춰 가르칠게요. 타면서 잘 따라 하면 조금 더, 힘들어하면 조금 덜 배워요." +
                        if (known.isEmpty()) "" else " 맞힌 ${known.size}개(${known.joinToString(", ") { it.english }})는 이미 아는 말이라 꿈 구슬로 넣었어요.",
                    26f,
                    color = R.color.hint,
                    topMargin = pad / 2,
                )
            )
            sheet.addView(button(context, "닫기", R.drawable.bg_pill_selected) { dialog.dismiss() }.also { it.topMargin(pad) })
        }

        fun showWaiting(message: String) {
            sheet.removeAllViews()
            sheet.addView(text(context, message, 32f, center = true, topMargin = pad))
            sheet.addView(button(context, "그만하기", R.drawable.bg_pill) { dialog.dismiss() }.also { it.topMargin(pad) })
        }

        // 아래 함수들이 서로 부르므로 먼저 이름만 만들어 둡니다.
        var handle: (TestStep) -> Unit = {}

        /** 강아지가 [line]을 말하는 동안 출제자에게 다음 할 일을 묻고, 둘 다 끝나면 넘어갑니다. */
        fun advance(line: String, waiting: String) {
            val examinerNow = running ?: return
            quiet()
            val mine = turn
            var spoken = false
            var step: TestStep? = null
            fun go() {
                val ready = step
                if (mine != turn || closed) return
                if (spoken && ready != null) handle(ready) else if (spoken) showWaiting(waiting)
            }
            narrator.speak(render(line)) { spoken = true; go() }
            examinerNow.next(history.toList()) { step = it; go() }
        }

        fun answer(passed: Boolean, byAdult: Boolean) {
            val question = asking ?: return
            asking = null
            history += TestAnswer(question.word, question.level, passed, lastHeard, byAdult)
            advance(if (passed) RIGHT else WRONG, "다음 문제를 고르고 있어요…")
        }

        fun ask(step: TestStep.Ask) {
            val examinerNow = running ?: return
            val word = step.word
            asking = step
            lastHeard = null
            sheet.removeAllViews()
            sheet.addView(
                text(
                    context,
                    "레벨 ${step.level} · ${history.size + 1}번째 문제 (최대 ${examinerNow.maxQuestions}문제)" + if (examinerNow.usesClaude) " · Claude 출제" else "",
                    28f,
                    color = R.color.hint,
                )
            )
            sheet.addView(text(context, "‘${word.korean}’", 64f, bold = true, topMargin = pad / 2, center = true))
            sheet.addView(text(context, "영어로 뭐라고 할까요?", 30f, center = true))
            sheet.addView(text(context, "정답: ${word.english}", 28f, color = R.color.hint, topMargin = pad / 2, center = true))
            val status = text(context, "강아지가 물어보고 있어요…", 28f, topMargin = pad / 2, center = true, color = R.color.kid_blue)
            sheet.addView(status)

            val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(button(context, "알아요", R.drawable.bg_pill_selected) { answer(true, byAdult = true) }, weight(pad, first = true))
            row.addView(button(context, "몰라요", R.drawable.bg_pill) { answer(false, byAdult = true) }, weight(pad, first = false))
            sheet.addView(row)
            sheet.addView(
                text(context, "차가 아이 목소리를 못 들으면 어른이 눌러 주세요.", 24f, color = R.color.hint, topMargin = pad / 3, center = true)
            )
            sheet.addView(button(context, "그만하기", R.drawable.bg_pill) { dialog.dismiss() }.also { it.topMargin(pad) })

            quiet()
            val mine = turn
            narrator.speak(render("‘${word.korean}’. 영어로 뭐라고 할까?")) {
                if (mine != turn || closed) return@speak
                if (!listener.available) {
                    status.text = "아이가 대답하면 알아요 / 몰라요를 눌러 주세요."
                    return@speak
                }
                status.text = "듣고 있어요…"
                listener.listen(word, LISTEN_MS) { result ->
                    if (mine != turn || closed) return@listen
                    lastHeard = result.heard
                    if (result.passed) {
                        answer(true, byAdult = false)
                    } else {
                        status.text = if (result.heard == null) "못 들었어요. 아이가 맞게 말했나요?" else "“${result.heard}”라고 들었어요. 아이가 맞게 말했나요?"
                    }
                }
            }
        }

        handle = { step ->
            when (step) {
                is TestStep.Ask -> ask(step)
                is TestStep.Finish -> {
                    quiet()
                    val known = history.filter { it.passed }.map { it.word }
                    coach.applyLevelTest(step.level, known)
                    showResult(step, known)
                    narrator.speak(render(DONE)) { }
                }
            }
        }

        fun showIntro() {
            sheet.removeAllViews()
            sheet.addView(text(context, "레벨 테스트", 40f, bold = true))
            sheet.addView(
                text(
                    context,
                    "강아지가 우리말을 말하면 아이가 영어로 대답해요. 쉬운 단어부터 문장까지 물어보고, 아이에게 맞는 레벨(1~${Levels.MAX})을 찾아요. 1~3분쯤 걸려요.",
                    28f,
                    topMargin = pad / 2,
                )
            )
            sheet.addView(
                text(
                    context,
                    if (claudeReady) {
                        "Claude(AI)가 아이의 대답을 보고 다음 문제를 골라요. 잘하면 레벨을 건너뛰고, 헷갈리면 한 번 더 확인해서 " +
                            "4~${ClaudeExaminer.MAX_QUESTIONS}문제 안에 끝내요. 문제와 정답 여부, 차가 알아들은 글자만 Claude에 보내고 아이 이름과 목소리는 보내지 않아요. " +
                            "통신이 안 되면 기본 테스트로 이어 가요."
                    } else {
                        "한 레벨에 두 문제씩 물어보고, 두 문제를 다 맞히면 다음 레벨로 올라가요. 하나라도 모르면 거기서 끝나요(2~${LevelTest.MAX_QUESTIONS}문제)."
                    },
                    26f,
                    topMargin = pad / 3,
                )
            )
            Levels.HINTS.forEach { sheet.addView(text(context, it, 24f, color = R.color.hint, topMargin = pad / 6)) }
            sheet.addView(
                text(
                    context,
                    "아이에게는 강아지랑 하는 영어 놀이라고 말해 주세요. 몰라도 괜찮아요. 차가 아이 목소리를 못 들으면 어른이 " +
                        "‘알아요’ / ‘몰라요’를 눌러 주면 돼요. 끝나면 결과 레벨로 바로 맞춰지고, 언제든 다시 할 수 있어요.",
                    26f,
                    color = R.color.hint,
                    topMargin = pad / 2,
                )
            )
            val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(
                button(context, "시작하기", R.drawable.bg_pill_selected) {
                    // 이미 배우는 중이거나 익힌 말은 문제로 내지 않습니다.
                    val seen = coach.summary().let { s -> (s.mastered + s.learning).map { it.word.id }.toSet() }
                    running?.close()
                    running = examiner(seen)
                    history.clear()
                    showWaiting("강아지가 인사하고 있어요…")
                    advance(INTRO, "첫 문제를 고르고 있어요…")
                },
                weight(pad, first = true),
            )
            row.addView(button(context, "닫기", R.drawable.bg_pill) { dialog.dismiss() }, weight(pad, first = false))
            sheet.addView(row)
        }

        dialog.setOnDismissListener {
            closed = true
            quiet()
            running?.close()
            onClosed()
        }
        showIntro()
        dialog.show()
        // 기본 대화상자는 좁아서 글이 여러 줄로 꺾입니다. 앱 화면의 3분의 2쯤으로 넓힙니다.
        dialog.window?.setLayout((activity.window.decorView.width * 0.66f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        return dialog
    }

    private fun View.topMargin(value: Int) {
        (layoutParams as LinearLayout.LayoutParams).topMargin = value
    }

    private fun weight(pad: Int, first: Boolean) =
        LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            topMargin = pad
            if (!first) marginStart = pad / 2
        }

    private fun text(
        context: Context,
        value: String,
        size: Float,
        bold: Boolean = false,
        color: Int = R.color.ink,
        topMargin: Int = 0,
        center: Boolean = false,
    ): TextView = TextView(context).apply {
        text = value
        textSize = size
        setTextColor(context.getColor(color))
        if (bold) typeface = Typeface.DEFAULT_BOLD
        if (center) gravity = Gravity.CENTER
        setLineSpacing(0f, 1.2f)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { this.topMargin = topMargin }
    }

    private fun button(context: Context, label: String, background: Int, onClick: () -> Unit): TextView {
        val pad = (22 * context.resources.displayMetrics.density).toInt()
        return text(context, label, 32f, bold = true, center = true).apply {
            setBackgroundResource(background)
            setPadding(pad, pad, pad, pad)
            setOnClickListener { onClick() }
        }
    }

    private const val LISTEN_MS = 4_500L
    private const val INTRO = "{멍이랑} 영어 놀이 해 볼까? 아는 것만 말하면 돼. 몰라도 괜찮아!"
    private const val RIGHT = "맞아!"
    private const val WRONG = "괜찮아! 이건 같이 배우자."
    private const val DONE = "다 했어! 정말 잘했어. 이제 {멍이} 딱 맞는 영어를 준비할게."
}

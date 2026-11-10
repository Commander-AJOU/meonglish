package com.ajikmeoreosseo.app.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import com.ajikmeoreosseo.app.R
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * 이야기 속으로 들어가고 나올 때의 장면 전환.
 *
 * 만화 영화처럼 화면이 강아지를 향해 동그랗게 닫히고, 이야기 제목이 잠깐 떴다가,
 * 다시 동그랗게 열리면서 이야기 속 장면이 나타납니다.
 * 전환 중에는 화면 터치를 막습니다.
 */
class IrisView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    /** 화면이 닫혀 있는 동안 보여 줄 제목 카드. */
    class Card(val kicker: String, val title: String, val subtitle: String)

    /** 화면이 닫혀 있는 동안 기다릴 일. 준비가 끝나면 받은 함수를 불러 다시 열게 합니다. */
    fun interface Gate {
        fun waitThen(open: () -> Unit)
    }

    private val cover = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF120E4A.toInt() }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFFFFE9A8.toInt()
    }
    private val kickerPaint = textPaint(0xFFFFE9A8.toInt())
    private val titlePaint = textPaint(0xFFFFFFFF.toInt())
    private val subtitlePaint = textPaint(0xFFD6DBFF.toInt())
    private val hole = Path().apply { fillType = Path.FillType.EVEN_ODD }

    private var open = 1f
    private var textAlpha = 0f
    private var centerX = 0f
    private var centerY = 0f
    private var card: Card? = null
    private var running: ValueAnimator? = null

    init {
        isClickable = true
    }

    /**
     * 전환을 한 번 재생합니다.
     *
     * @param onCovered 화면이 완전히 가려졌을 때. 여기서 뒤 화면을 바꾸고 [moveCenter]로 열릴 중심을 옮깁니다.
     * @param onOpening 다시 열리기 시작할 때.
     * @param onDone 다 열렸을 때.
     * @param gate 있으면 제목 카드를 띄운 채 기다렸다가, 준비가 끝났다고 알려 줄 때 다시 엽니다(오늘의 이야기를 쓰는 동안 등).
     */
    fun play(
        x: Float,
        y: Float,
        card: Card?,
        onCovered: () -> Unit,
        onOpening: () -> Unit,
        onDone: () -> Unit,
        gate: Gate? = null,
    ) {
        running?.cancel()
        this.card = card
        centerX = x
        centerY = y
        textAlpha = 0f
        visibility = VISIBLE

        fun reopen() {
            onOpening()
            animate(0f, 1f, OPEN_MS, DecelerateInterpolator(1.6f), { open = it }) {
                visibility = GONE
                onDone()
            }
        }

        fun fadeOutThenReopen() {
            animate(1f, 0f, FADE_MS, LinearInterpolator(), { textAlpha = it }) { reopen() }
        }

        animate(1f, 0f, CLOSE_MS, AccelerateInterpolator(1.4f), { open = it }) {
            onCovered()
            if (card == null) {
                reopen()
                return@animate
            }
            animate(0f, 1f, FADE_MS, LinearInterpolator(), { textAlpha = it }) {
                animate(1f, 1f, HOLD_MS, LinearInterpolator(), { }) {
                    if (gate == null) fadeOutThenReopen() else gate.waitThen { fadeOutThenReopen() }
                }
            }
        }
    }

    /** 닫혀 있는 동안 카드의 글을 바꿉니다(준비가 끝나 제목이 정해졌을 때). */
    fun updateCard(card: Card) {
        this.card = card
        invalidate()
    }

    /** 다시 열릴 때의 중심(이야기 속 강아지의 자리)을 정합니다. */
    fun moveCenter(x: Float, y: Float) {
        centerX = x
        centerY = y
    }

    private fun animate(
        from: Float,
        to: Float,
        duration: Long,
        interpolator: TimeInterpolator,
        update: (Float) -> Unit,
        end: () -> Unit,
    ) {
        running = ValueAnimator.ofFloat(from, to).apply {
            this.duration = duration
            this.interpolator = interpolator
            addUpdateListener {
                update(it.animatedValue as Float)
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                private var cancelled = false
                override fun onAnimationCancel(animation: Animator) {
                    cancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (!cancelled) end()
                }
            })
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (open >= 1f) return
        val w = width.toFloat()
        val h = height.toFloat()
        val unit = min(w, h) / 100f
        val reach = hypot(max(centerX, w - centerX), max(centerY, h - centerY))
        val radius = reach * open

        hole.reset()
        hole.addRect(0f, 0f, w, h, Path.Direction.CW)
        if (radius > 0f) hole.addCircle(centerX, centerY, radius, Path.Direction.CW)
        canvas.drawPath(hole, cover)
        if (radius > 0f) {
            ring.strokeWidth = unit * 1.2f
            canvas.drawCircle(centerX, centerY, radius, ring)
        }

        val shown = card ?: return
        if (textAlpha <= 0f) return
        val alpha = (textAlpha * 255).toInt()
        drawLine(canvas, shown.kicker, kickerPaint, unit * 4.4f, h * 0.40f, alpha)
        drawLine(canvas, shown.title, titlePaint, unit * 8.4f, h * 0.53f, alpha)
        drawLine(canvas, shown.subtitle, subtitlePaint, unit * 4.4f, h * 0.64f, alpha)
    }

    private fun drawLine(canvas: Canvas, text: String, paint: Paint, size: Float, y: Float, alpha: Int) {
        paint.textSize = size
        // 좁은 화면에서는 한 줄에 들어가도록 줄입니다.
        val available = width * 0.9f
        val measured = paint.measureText(text)
        if (measured > available) paint.textSize = size * available / measured
        paint.alpha = alpha
        canvas.drawText(text, width / 2f, y, paint)
    }

    private fun textPaint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textAlign = Paint.Align.CENTER
        typeface = resources.getFont(R.font.jua)
    }

    private companion object {
        const val CLOSE_MS = 520L
        const val FADE_MS = 260L
        const val HOLD_MS = 1_300L
        const val OPEN_MS = 680L
    }
}

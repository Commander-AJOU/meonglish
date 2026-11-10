package com.ajikmeoreosseo.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

/** 강아지가 타고 다니는 폭신한 구름. 그림자와 하이라이트만 넣은 단순한 모양입니다. */
class CloudView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF4F7FF.toInt() }
    private val shade = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33AEB8E6 }
    private val shadow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val oval = RectF()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        shadow.shader = RadialGradient(
            w / 2f, h * 0.92f, w * 0.5f,
            intArrayOf(0x55120E4A, 0x00120E4A), floatArrayOf(0.2f, 1f), Shader.TileMode.CLAMP,
        )
        path.reset()
        // 아래는 둥글넓적한 바탕, 위에는 봉긋한 구름 봉우리 네 개
        oval.set(w * 0.04f, h * 0.42f, w * 0.96f, h * 0.86f)
        path.addOval(oval, Path.Direction.CW)
        bump(w * 0.26f, h * 0.50f, h * 0.30f)
        bump(w * 0.46f, h * 0.40f, h * 0.38f)
        bump(w * 0.66f, h * 0.46f, h * 0.32f)
        bump(w * 0.82f, h * 0.58f, h * 0.22f)
    }

    private fun bump(cx: Float, cy: Float, r: Float) {
        oval.set(cx - r, cy - r, cx + r, cy + r)
        path.addOval(oval, Path.Direction.CW)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        oval.set(w * 0.05f, h * 0.70f, w * 0.95f, h)
        canvas.drawOval(oval, shadow)
        canvas.drawPath(path, body)
        // 아랫부분을 살짝 어둡게 해서 두께감을 줍니다.
        oval.set(w * 0.10f, h * 0.62f, w * 0.90f, h * 0.86f)
        canvas.drawOval(oval, shade)
    }
}

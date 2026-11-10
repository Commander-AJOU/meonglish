package com.ajikmeoreosseo.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

/**
 * 장면의 바탕: 밤하늘 그라데이션, 별, 강아지가 날아갈 점선 길.
 * 움직이지 않는 그림이라 크기가 바뀔 때만 다시 그립니다. 강아지·달·비는 [SceneView]가 그 위에 올립니다.
 */
class SkyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val sky = Paint(Paint.ANTI_ALIAS_FLAG)
    private val star = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val trail = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0x4DFFFFFF
    }

    private val route = Path()
    private val measure = PathMeasure()
    /** 강아지가 날아갈 점선 길을 그릴지. 홈 화면에서는 숨깁니다. */
    var showRoute = true
        set(value) {
            field = value
            invalidate()
        }

    private val stars = Random(7).let { r -> List(90) { Triple(r.nextFloat(), r.nextFloat(), r.nextFloat()) } }

    /** 이야기 진행도(0~1)에 해당하는 길 위의 점을 [out]에 x, y 순서로 담습니다. */
    fun routePoint(progress: Float, out: FloatArray) {
        measure.getPosTan(measure.length * progress.coerceIn(0f, 1f), out, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val width = w.toFloat()
        val height = h.toFloat()
        sky.shader = LinearGradient(
            0f, 0f, 0f, height,
            // 칙칙하지 않게, 보랏빛이 도는 환한 밤하늘
            intArrayOf(0xFF17135F.toInt(), 0xFF3B2FB0.toInt(), 0xFF7C5CF0.toInt()),
            null, Shader.TileMode.CLAMP,
        )
        route.reset()
        route.moveTo(width * ROUTE_START_X, height * ROUTE_START_Y)
        route.quadTo(width * 0.46f, height * 0.24f, width * ROUTE_END_X, height * ROUTE_END_Y)
        measure.setPath(route, false)

        val unit = height / 100f
        trail.strokeWidth = unit * 0.5f
        trail.pathEffect = DashPathEffect(floatArrayOf(unit * 1.6f, unit * 3f), 0f)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val unit = h / 100f

        canvas.drawRect(0f, 0f, w, h, sky)
        for ((x, y, size) in stars) {
            star.alpha = (90 + size * 165).toInt()
            canvas.drawCircle(x * w, y * h * 0.8f, unit * (0.2f + size * 0.4f), star)
        }
        if (showRoute) canvas.drawPath(route, trail)
    }

    private companion object {
        // 강아지 그림이 화면 왼쪽과 아래 자막에 걸리지 않도록 잡은 출발점과 도착점(화면 비율).
        const val ROUTE_START_X = 0.32f
        const val ROUTE_START_Y = 0.52f
        const val ROUTE_END_X = 0.60f
        const val ROUTE_END_Y = 0.36f
    }
}

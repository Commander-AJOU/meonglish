package com.ajikmeoreosseo.app.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import com.ajikmeoreosseo.app.R
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * 이야기 장면. 밤하늘([SkyView]) 위에 달빛 섬, 구름을 탄 강아지, 비를 겹쳐 보여 줍니다.
 *
 * 강아지는 단계별 그림(res/drawable-nodpi/puppy_N*.webp)이고, 숨쉬기·눈 깜빡임·폴짝 뛰기·
 * 자라나는 연출은 여기서 붙입니다. 달·비는 animation/ 폴더의 Remotion 장면을 뽑은 움직이는 WebP입니다.
 * 주행 중에는 반복 움직임을 멈추고, 차량 신호에 따른 기울임과 이동만 남깁니다.
 */
class SceneView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val sky = SkyView(context)
    private val moon = ImageView(context)
    private val rain = ImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        alpha = 0f
    }

    /** 구름과 강아지를 함께 옮기고 기울이는 틀. */
    private val holder = FrameLayout(context)
    private val cloud = CloudView(context)

    /** 강아지 그림(평소 모습 + 표정 덧그림). 숨쉬기와 폴짝 뛰기는 이 틀에 겁니다. */
    private val figure = FrameLayout(context)
    private val body = ImageView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
    private val face = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
        alpha = 0f
    }

    /** 자랄 때 번쩍이는 빛. */
    private val glow = View(context).apply {
        background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0xFFFFF4C2.toInt(), 0x00FFF4C2),
        ).apply {
            gradientType = GradientDrawable.RADIAL_GRADIENT
            shape = GradientDrawable.OVAL
            gradientRadius = 1f
        }
        alpha = 0f
    }

    /** 날아가는 간식. */
    private val treat = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
        visibility = GONE
        setImageResource(R.drawable.treat)
    }

    private val main = Handler(Looper.getMainLooper())
    private val loops = HashMap<ImageView, AnimatedImageDrawable>()
    private val point = FloatArray(2)

    private var progress = 0f
    private var tilt = 0f
    private var raining = false
    private var driving = false
    private var shown = false
    private var home = true
    private var stage = 1
    private var busyFace = false

    private var progressAnimator: ValueAnimator? = null
    private var tiltAnimator: ValueAnimator? = null
    private var idle: ValueAnimator? = null
    private val blink = Runnable { blinkNow() }

    private var blinkBitmap: Bitmap? = null
    private var happyBitmap: Bitmap? = null

    init {
        addView(sky, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(moon)
        addView(glow)
        holder.addView(cloud)
        figure.addView(body, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        figure.addView(face, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        holder.addView(figure)
        addView(holder)
        addView(treat)
        addView(rain, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        loadLoop(R.drawable.moon_island, moon)
        loadLoop(R.drawable.rain_layer, rain)
        startIdle()
    }

    // region 화면에서 호출

    /** 강아지의 모습(1~5)을 바로 바꿉니다(처음 보여 줄 때). 자라는 연출은 [evolve]. */
    fun setStage(value: Int) {
        stage = value.coerceIn(1, 5)
        loadStage(stage) { idleBitmap, blinkB, happyB ->
            body.setImageBitmap(idleBitmap)
            blinkBitmap = blinkB
            happyBitmap = happyB
        }
    }

    /** 이야기 진행도(0~1). 강아지가 달빛 섬 쪽으로 움직입니다. */
    fun setProgress(target: Float) {
        progressAnimator?.cancel()
        progressAnimator = animate(progress, target.coerceIn(0f, 1f), 900L) {
            progress = it
            place()
        }
    }

    /** 방향지시등에 맞춰 구름과 강아지를 기울입니다. -1 왼쪽, 0 가운데, 1 오른쪽. */
    fun setTilt(target: Float) {
        tiltAnimator?.cancel()
        tiltAnimator = animate(tilt, target, 350L) {
            tilt = it
            holder.rotation = tilt * TILT_DEGREES
        }
    }

    fun setRaining(value: Boolean) {
        if (raining == value) return
        raining = value
        rain.animate().alpha(if (value) 1f else 0f).setDuration(400L).start()
        updatePlayback()
    }

    /**
     * 홈 화면에서는 강아지가 가운데에 크게 앉아 있고, 이야기 속에서는 길을 따라 날아갑니다.
     * 장면 전환이 화면을 가린 사이에 부르므로 바로 바꿉니다.
     */
    fun setHome(value: Boolean) {
        if (home == value) return
        home = value
        sky.showRoute = !value
        requestLayout()
    }

    /** 지금 강아지 몸통의 중심(이 뷰 안의 좌표)을 [out]에 x, y 순서로 담습니다. */
    fun puppyCenter(out: FloatArray) {
        out[0] = holder.translationX + holder.pivotX
        out[1] = holder.translationY + holder.pivotY
    }

    /** 주행 중에는 반복 움직임을 멈춥니다(운전자 시선 분산과 화면 부하를 줄이기 위해). */
    fun setDriving(value: Boolean) {
        if (driving == value) return
        driving = value
        updatePlayback()
    }

    /**
     * 간식이 [fromX], [fromY](이 뷰 안의 좌표)에서 강아지 입으로 날아갑니다.
     * 다 먹으면 [onEaten]을 부릅니다. 기뻐하는 연출은 따로 [celebrate]로 합니다.
     */
    fun feed(fromX: Float, fromY: Float, onEaten: () -> Unit) {
        val size = figure.width * TREAT_SIZE
        if (size <= 0f) {
            onEaten()
            return
        }
        treat.layoutParams = LayoutParams(size.toInt(), size.toInt())
        treat.visibility = VISIBLE
        treat.alpha = 1f
        val toX = holder.translationX + figure.left + figure.width * MOUTH_X
        val toY = holder.translationY + figure.top + figure.height * MOUTH_Y
        val arc = figure.height * 0.35f
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = FEED_FLY_MS
            interpolator = AccelerateInterpolator(0.8f)
            addUpdateListener {
                val t = it.animatedValue as Float
                treat.translationX = fromX + (toX - fromX) * t - size / 2f
                treat.translationY = fromY + (toY - fromY) * t - arc * sin(t * PI).toFloat() - size / 2f
                treat.rotation = 360f * t
                val s = 1f - 0.55f * t
                treat.scaleX = s
                treat.scaleY = s
                if (t > 0.9f) treat.alpha = (1f - t) * 10f
            }
            doOnEnd {
                treat.visibility = GONE
                onEaten()
            }
            start()
        }
    }

    /** 기뻐하는 표정으로 폴짝 뜁니다(간식을 먹었을 때). */
    fun celebrate() {
        val happy = happyBitmap
        if (happy != null) showFace(happy, CELEBRATE_MS)
        val jump = figure.height * 0.14f
        figure.animate().cancel()
        figure.animate().translationY(-jump).scaleX(0.94f).scaleY(1.08f).setDuration(220L)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                figure.animate().translationY(0f).scaleX(1f).scaleY(1f).setDuration(520L)
                    .setInterpolator(BounceInterpolator())
                    .withEndAction { if (!driving) startIdle() }
                    .start()
            }
            .start()
        idle?.cancel()
    }

    /** 빛이 번쩍이며 다음 모습으로 자랍니다. */
    fun evolve(toStage: Int) {
        val next = toStage.coerceIn(1, 5)
        loadStage(next) { idleBitmap, blinkB, happyB ->
            stage = next
            busyFace = true
            face.animate().cancel()
            face.alpha = 0f
            idle?.cancel()
            glow.animate().cancel()
            glow.scaleX = 0.5f
            glow.scaleY = 0.5f
            glow.animate().alpha(1f).scaleX(1.4f).scaleY(1.4f).setDuration(EVOLVE_FLASH_MS)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    body.setImageBitmap(idleBitmap)
                    blinkBitmap = blinkB
                    happyBitmap = happyB
                    figure.scaleX = 0.6f
                    figure.scaleY = 0.6f
                    figure.animate().scaleX(1f).scaleY(1f).setDuration(700L)
                        .setInterpolator(OvershootInterpolator(2.2f))
                        .withEndAction {
                            busyFace = false
                            if (!driving) startIdle()
                        }
                        .start()
                    glow.animate().alpha(0f).setDuration(EVOLVE_FADE_MS).start()
                }
                .start()
        }
    }

    // endregion

    // region 배치

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val unit = min(measuredWidth, measuredHeight)
        val moonSize = exactly(unit * MOON_SIZE)
        moon.measure(moonSize, moonSize)

        val size = (unit * (if (home) HOME_PUPPY_SIZE else STORY_PUPPY_SIZE)).toInt()
        val holderW = (size * HOLDER_W).toInt()
        val holderH = (size * HOLDER_H).toInt()
        if (size != figureSize) {
            figureSize = size
            figure.layoutParams = LayoutParams(size, size).apply { leftMargin = (holderW - size) / 2 }
            cloud.layoutParams = LayoutParams(holderW, (size * CLOUD_H).toInt()).apply {
                topMargin = (size * CLOUD_TOP).toInt()
            }
            (glow.background as GradientDrawable).gradientRadius = size * 0.8f
        }
        holder.measure(exactly(holderW), exactly(holderH))
        val glowSize = exactly(size * 1.6f)
        glow.measure(glowSize, glowSize)
    }

    private var figureSize = 0

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        moon.translationX = width * MOON_CENTER_X - moon.width / 2f
        moon.translationY = height * MOON_CENTER_Y - moon.height / 2f
        // 강아지 몸통의 중심을 축으로 삼아 옮기고 기울입니다.
        holder.pivotX = holder.width / 2f
        holder.pivotY = figure.top + figure.height * BODY_Y
        figure.pivotX = figure.width / 2f
        figure.pivotY = figure.height * PAWS_Y
        sky.showRoute = !home
        place()
    }

    private fun place() {
        if (width == 0) return
        if (home) {
            point[0] = width * HOME_X
            point[1] = height * HOME_Y
        } else {
            sky.routePoint(progress, point)
        }
        holder.translationX = point[0] - holder.pivotX
        holder.translationY = point[1] - holder.pivotY
        glow.translationX = point[0] - glow.width / 2f
        glow.translationY = point[1] - glow.height / 2f
    }

    private fun exactly(size: Float) = MeasureSpec.makeMeasureSpec(size.toInt(), MeasureSpec.EXACTLY)
    private fun exactly(size: Int) = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)

    // endregion

    // region 강아지 그림과 움직임

    private fun loadStage(value: Int, onReady: (Bitmap, Bitmap?, Bitmap?) -> Unit) {
        val index = value - 1
        decoder.execute {
            val idleBitmap = decode(IDLE[index]) ?: return@execute
            val blinkB = BLINK[index]?.let(::decode)
            val happyB = HAPPY[index]?.let(::decode)
            main.post { onReady(idleBitmap, blinkB, happyB) }
        }
    }

    private fun decode(resId: Int): Bitmap? = runCatching {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(resources, resId)) { decoder, info, _ ->
            if (info.size.width > MAX_DECODE) decoder.setTargetSize(MAX_DECODE, MAX_DECODE)
        }
    }.onFailure { Log.w(TAG, "could not decode puppy image", it) }.getOrNull()

    /** 숨쉬기: 발을 땅에 붙인 채 몸이 천천히 부풀었다 가라앉습니다. 구름도 살짝 떠다닙니다. */
    private fun startIdle() {
        idle?.cancel()
        idle = ValueAnimator.ofFloat(0f, (2 * PI).toFloat()).apply {
            duration = BREATH_MS
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                val t = it.animatedValue as Float
                val breath = sin(t)
                figure.scaleY = 1f + 0.03f * breath
                figure.scaleX = 1f - 0.012f * breath
                cloud.translationY = figure.height * 0.012f * sin(t * 0.5f + 1f)
            }
            start()
        }
        scheduleBlink()
    }

    private fun stopIdle() {
        idle?.cancel()
        idle = null
        main.removeCallbacks(blink)
        figure.scaleX = 1f
        figure.scaleY = 1f
    }

    private fun scheduleBlink() {
        main.removeCallbacks(blink)
        main.postDelayed(blink, BLINK_MIN_MS + Random.nextLong(BLINK_RANGE_MS))
    }

    private fun blinkNow() {
        val bitmap = blinkBitmap
        if (bitmap != null && !busyFace && shown && !driving) showFace(bitmap, BLINK_HOLD_MS)
        scheduleBlink()
    }

    /** 표정 덧그림을 살짝 겹쳐 보였다가 거둡니다. 두 그림의 자세가 조금 달라도 겹침으로 가려집니다. */
    private fun showFace(bitmap: Bitmap, holdMs: Long) {
        busyFace = true
        face.animate().cancel()
        face.setImageBitmap(bitmap)
        face.animate().alpha(1f).setDuration(FACE_FADE_MS).withEndAction {
            main.postDelayed({
                face.animate().alpha(0f).setDuration(FACE_FADE_MS).withEndAction { busyFace = false }.start()
            }, holdMs)
        }.start()
    }

    private fun loadLoop(resId: Int, into: ImageView) {
        decoder.execute {
            val drawable = runCatching {
                ImageDecoder.decodeDrawable(ImageDecoder.createSource(resources, resId))
            }.onFailure { Log.w(TAG, "could not decode scene image", it) }.getOrNull() ?: return@execute
            main.post {
                into.setImageDrawable(drawable)
                if (drawable is AnimatedImageDrawable) {
                    drawable.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                    loops[into] = drawable
                    updatePlayback()
                }
            }
        }
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        shown = isVisible
        updatePlayback()
    }

    /** 화면에 보이고 차가 서 있을 때만 반복 움직임을 돌립니다. 비는 비가 올 때만. */
    private fun updatePlayback() {
        val play = shown && !driving
        for ((view, loop) in loops) {
            val shouldRun = play && (view !== rain || raining)
            if (shouldRun && !loop.isRunning) loop.start()
            if (!shouldRun && loop.isRunning) loop.stop()
        }
        if (play && idle == null) startIdle()
        if (!play && idle != null) stopIdle()
    }

    // endregion

    private fun animate(from: Float, to: Float, duration: Long, apply: (Float) -> Unit): ValueAnimator =
        ValueAnimator.ofFloat(from, to).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            addUpdateListener { apply(it.animatedValue as Float) }
            start()
        }

    private fun ValueAnimator.doOnEnd(action: () -> Unit) {
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) = action()
        })
    }

    private companion object {
        const val TAG = "SceneView"
        val decoder = Executors.newSingleThreadExecutor()

        val IDLE = intArrayOf(
            R.drawable.puppy_1, R.drawable.puppy_2, R.drawable.puppy_3, R.drawable.puppy_4, R.drawable.puppy_5,
        )
        val BLINK = arrayOf(
            null, R.drawable.puppy_2_blink, R.drawable.puppy_3_blink, R.drawable.puppy_4_blink, R.drawable.puppy_5_blink,
        )
        val HAPPY = arrayOf(
            null, R.drawable.puppy_2_happy, R.drawable.puppy_3_happy, R.drawable.puppy_4_happy, R.drawable.puppy_5_happy,
        )

        /** 그림은 정사각형이고 강아지가 가운데에 있습니다. 더 크게 풀어도 화면에서는 차이가 없어 이 크기로 줄입니다. */
        const val MAX_DECODE = 768

        // 크기는 화면의 짧은 변, 위치는 화면 너비·높이에 대한 비율입니다.
        const val MOON_SIZE = 0.46f
        const val MOON_CENTER_X = 0.82f
        const val MOON_CENTER_Y = 0.25f
        const val STORY_PUPPY_SIZE = 0.40f
        const val HOME_PUPPY_SIZE = 0.56f

        // 강아지 그림 한 변을 1로 본 비율
        const val HOLDER_W = 1.24f
        const val HOLDER_H = 1.08f
        const val CLOUD_TOP = 0.76f
        const val CLOUD_H = 0.32f
        const val BODY_Y = 0.56f
        const val PAWS_Y = 0.90f
        const val MOUTH_X = 0.52f
        const val MOUTH_Y = 0.50f
        const val TREAT_SIZE = 0.26f

        const val TILT_DEGREES = 12f

        // 홈 화면에서 강아지가 앉아 있는 자리
        const val HOME_X = 0.5f
        const val HOME_Y = 0.49f

        const val BREATH_MS = 2_800L
        const val BLINK_MIN_MS = 2_600L
        const val BLINK_RANGE_MS = 2_800L
        const val BLINK_HOLD_MS = 110L
        const val FACE_FADE_MS = 70L
        const val CELEBRATE_MS = 1_500L
        const val FEED_FLY_MS = 620L
        const val EVOLVE_FLASH_MS = 420L
        const val EVOLVE_FADE_MS = 700L
    }
}

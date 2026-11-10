package com.ajikmeoreosseo.app.ui

import android.content.Context
import android.graphics.Outline
import android.graphics.ImageDecoder
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import com.ajikmeoreosseo.app.R
import com.ajikmeoreosseo.app.pet.House
import java.util.concurrent.Executors
import kotlin.math.min

/**
 * 강아지 집. 방 그림을 둥근 카드로 보여 주고, 그 위에 놓인 물건과 강아지를 정해진 자리에 겹칩니다.
 * 방 그림은 1024×740이고, 물건 위치는 그 안의 비율로 적혀 있습니다([House.ITEMS]).
 */
class HouseView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val room = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_XY
        setImageResource(R.drawable.room)
    }
    private val puppy = ImageView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
    private val items = LinkedHashMap<House.Item, ImageView>()
    private val main = Handler(Looper.getMainLooper())

    private var house: House? = null
    private var stage = 0

    // 방 그림이 실제로 그려지는 영역(이 뷰 안의 좌표)
    private var roomLeft = 0f
    private var roomTop = 0f
    private var roomWidth = 0f
    private var roomHeight = 0f

    init {
        clipChildren = true
        addView(room)
        // 물건과 강아지는 겹치는 순서대로 추가합니다.
        val ordered = House.ITEMS.sortedBy { it.layer }
        var puppyAdded = false
        for (item in ordered) {
            if (!puppyAdded && item.layer > House.PUPPY_LAYER) {
                addView(puppy)
                puppyAdded = true
            }
            val view = ImageView(context).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageResource(item.drawable)
                visibility = GONE
            }
            items[item] = view
            addView(view)
        }
        if (!puppyAdded) addView(puppy)
    }

    fun bind(house: House) {
        this.house = house
        refresh()
    }

    /** 강아지 모습(1~5). 집에서는 평소 모습만 씁니다. */
    fun setStage(value: Int) {
        if (stage == value) return
        stage = value
        decoder.execute {
            val drawable = runCatching {
                ImageDecoder.decodeDrawable(ImageDecoder.createSource(resources, PUPPY[value.coerceIn(1, 5) - 1])) { d, info, _ ->
                    if (info.size.width > MAX_DECODE) d.setTargetSize(MAX_DECODE, MAX_DECODE)
                }
            }.onFailure { Log.w(TAG, "could not decode puppy", it) }.getOrNull() ?: return@execute
            main.post { puppy.setImageDrawable(drawable) }
        }
    }

    /** 놓인 물건을 다시 보여 줍니다. 방금 놓인 물건은 통통 튀며 나타납니다. */
    fun refresh(bounce: House.Item? = null) {
        val house = house ?: return
        for ((item, view) in items) {
            val shown = house.isPlaced(item)
            if (shown && view.visibility != VISIBLE && item == bounce) {
                view.scaleX = 0.3f
                view.scaleY = 0.3f
                view.animate().scaleX(1f).scaleY(1f).setDuration(420L).setInterpolator(OvershootInterpolator(2.5f)).start()
            }
            view.visibility = if (shown) VISIBLE else GONE
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // 방 그림을 비율대로 가운데에 최대한 크게
        val w = measuredWidth.toFloat()
        val h = measuredHeight.toFloat()
        val scale = min(w / ROOM_W, h / ROOM_H)
        roomWidth = ROOM_W * scale
        roomHeight = ROOM_H * scale
        roomLeft = (w - roomWidth) / 2f
        roomTop = (h - roomHeight) / 2f
        room.measure(exactly(roomWidth), exactly(roomHeight))
        for ((item, view) in items) {
            val size = exactly(roomWidth * item.width)
            view.measure(size, size)
        }
        val puppySize = exactly(roomWidth * House.PUPPY_WIDTH)
        puppy.measure(puppySize, puppySize)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        room.layout(roomLeft.toInt(), roomTop.toInt(), (roomLeft + roomWidth).toInt(), (roomTop + roomHeight).toInt())
        room.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, roomWidth * CORNER)
            }
        }
        room.clipToOutline = true
        for ((item, view) in items) place(view, item.x, item.y)
        place(puppy, House.PUPPY_X, House.PUPPY_Y)
    }

    private fun place(view: View, fx: Float, fy: Float) {
        val cx = roomLeft + roomWidth * fx
        val cy = roomTop + roomHeight * fy
        val half = view.measuredWidth / 2
        view.layout((cx - half).toInt(), (cy - half).toInt(), (cx + half).toInt(), (cy + half).toInt())
    }

    private fun exactly(size: Float) = MeasureSpec.makeMeasureSpec(size.toInt(), MeasureSpec.EXACTLY)

    private companion object {
        const val TAG = "HouseView"
        const val ROOM_W = 1024f
        const val ROOM_H = 740f
        const val CORNER = 0.05f
        const val MAX_DECODE = 512
        val decoder = Executors.newSingleThreadExecutor()
        val PUPPY = intArrayOf(
            R.drawable.puppy_1, R.drawable.puppy_2, R.drawable.puppy_3, R.drawable.puppy_4, R.drawable.puppy_5,
        )
    }
}

package com.ajikmeoreosseo.app.ui

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator

/** 점토 버튼의 손맛: 누르면 살짝 눌리고, 떼면 통 하고 되돌아옵니다. */
object Clay {

    @SuppressLint("ClickableViewAccessibility")
    fun pressable(view: View, onClick: () -> Unit) {
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN ->
                    v.animate().scaleX(PRESSED).scaleY(PRESSED).setDuration(PRESS_MS)
                        .setInterpolator(DecelerateInterpolator()).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(RELEASE_MS)
                        .setInterpolator(OvershootInterpolator(3f)).start()
            }
            false
        }
        view.setOnClickListener { onClick() }
    }

    private const val PRESSED = 0.94f
    private const val PRESS_MS = 120L
    private const val RELEASE_MS = 260L
}

package com.ajikmeoreosseo.app.pet

import android.content.Context
import com.ajikmeoreosseo.app.story.ChildName
import com.ajikmeoreosseo.app.story.Tone

/**
 * 아이가 키우는 구름 강아지.
 *
 * 이야기 속에서 영어를 한 가지 배울 때마다 간식이 하나 생깁니다. 간식은 바로 먹이지 않고 모아 두었다가,
 * 차를 세운 뒤 아이가 직접 줍니다. 간식 하나에 한 단계씩 자라고, 단계가 쌓이면 모습이 바뀝니다.
 *
 *   1단계  갓 태어난 강아지(눈 감고 잠)   →  10단계  아기 강아지  →  30단계  꼬마 강아지(빨간 목걸이)
 *   60단계 소년 강아지(파란 스카프)        →  100단계 다 자란 강아지(빨간 망토와 별 메달)
 *
 * 모든 메서드는 메인 스레드에서 호출해야 합니다.
 */
class Pet(context: Context) {

    /** 간식을 한 번 준 결과. [grewTo]는 모습이 바뀌었을 때의 새 단계. */
    class Feed(val level: Int, val grewTo: Int?)

    private val prefs = context.applicationContext.getSharedPreferences("pet", Context.MODE_PRIVATE)

    var level: Int = prefs.getInt(KEY_LEVEL, 1).coerceIn(1, MAX_LEVEL)
        private set

    var treats: Int = prefs.getInt(KEY_TREATS, 0).coerceAtLeast(0)
        private set

    /** 강아지 이름. 대본의 {멍}, {멍아}, {멍이} … 표시를 이 이름으로 바꿉니다. */
    var name: ChildName = petName(prefs.getString(KEY_NAME, null))
        private set

    /** 진도나 이름이 바뀔 때마다 호출됩니다. */
    var onChanged: (() -> Unit)? = null

    /** 지금 모습(1~5). */
    val stage: Int get() = stageOf(level)

    val stageName: String get() = STAGE_NAMES[stage - 1]

    val isGrown: Boolean get() = level >= MAX_LEVEL

    /**
     * 강아지가 자란 만큼 영어도 어려워집니다. 강아지의 다섯 모습이 영어의 다섯 레벨과 짝을 이룹니다:
     * 아기 강아지(10)부터 모든 단어, 꼬마 강아지(30)부터 짧은 표현, 소년 강아지(60)부터 문장, 다 자란 강아지(100)부터 묻는 말과 긴 문장.
     */
    val englishFloor: Int get() = stage

    /** 말투: 아기(1~2단계), 꼬마(3~4단계), 다 자람(5단계). */
    val tone: Tone
        get() = when (stage) {
            1, 2 -> Tone.BABY
            3, 4 -> Tone.KID
            else -> Tone.GROWN
        }

    /** 다음 모습까지 얼마나 왔는지(0~1). 다 자랐으면 1. */
    val stageProgress: Float
        get() {
            if (isGrown) return 1f
            val from = STAGE_FROM[stage - 1]
            val to = STAGE_FROM[stage]
            return (level - from).toFloat() / (to - from)
        }

    /** 다음 모습이 되기까지 남은 간식 수. 다 자랐으면 0. */
    val treatsToNextStage: Int get() = if (isGrown) 0 else STAGE_FROM[stage] - level

    /** 영어를 배워서 간식을 얻습니다. */
    fun earn(count: Int = 1) {
        if (count <= 0) return
        treats += count
        save()
    }

    /** 간식 반 조각이 남아 있는지. 반 조각 둘이 모이면 하나가 됩니다. */
    var hasHalfTreat: Boolean = prefs.getBoolean(KEY_HALF, false)
        private set

    /**
     * 간식 반 조각을 얻습니다(영어를 만났지만 아이 목소리가 들리지 않았을 때).
     * 마이크가 뒷자리 소리를 못 듣는 차에서도 강아지가 계속 자라게 하기 위한 것입니다. 하나가 채워졌으면 true.
     */
    fun earnHalf(): Boolean {
        val completed = hasHalfTreat
        hasHalfTreat = !completed
        if (completed) treats += 1
        prefs.edit().putBoolean(KEY_HALF, hasHalfTreat).apply()
        save()
        return completed
    }

    /** 간식을 하나 줍니다. 줄 간식이 없으면 null. 다 자란 뒤에도 간식은 먹지만 더 자라지는 않습니다. */
    fun feed(): Feed? {
        if (treats <= 0) return null
        treats -= 1
        val before = stage
        if (level < MAX_LEVEL) level += 1
        save()
        return Feed(level, stage.takeIf { it != before })
    }

    fun setName(raw: String?) {
        name = petName(raw)
        prefs.edit().putString(KEY_NAME, name.given).apply()
        onChanged?.invoke()
    }

    private fun save() {
        prefs.edit().putInt(KEY_LEVEL, level).putInt(KEY_TREATS, treats).apply()
        onChanged?.invoke()
    }

    companion object {
        const val MAX_LEVEL = 100
        const val DEFAULT_NAME = "콩이"

        /** 각 모습이 시작되는 단계. */
        val STAGE_FROM = intArrayOf(1, 10, 30, 60, 100)
        val STAGE_NAMES = listOf("갓 태어난 강아지", "아기 강아지", "꼬마 강아지", "소년 강아지", "다 자란 강아지")

        fun stageOf(level: Int): Int = STAGE_FROM.count { level >= it }.coerceIn(1, STAGE_FROM.size)

        private fun petName(raw: String?): ChildName {
            val cleaned = ChildName.clean(raw).ifEmpty { DEFAULT_NAME }
            return ChildName(cleaned, token = "멍", fallback = DEFAULT_NAME, fallbackCalling = "${DEFAULT_NAME}야")
        }

        private const val KEY_LEVEL = "level"
        private const val KEY_TREATS = "treats"
        private const val KEY_HALF = "halfTreat"
        private const val KEY_NAME = "name"
    }
}

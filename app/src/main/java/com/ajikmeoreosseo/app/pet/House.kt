package com.ajikmeoreosseo.app.pet

import android.content.Context
import com.ajikmeoreosseo.app.R

/**
 * 강아지 집 꾸미기.
 *
 * 방 안 물건은 강아지의 단계가 오를 때마다 하나씩 열립니다. 열린 물건은 아이가 눌러서 방의 정해진 자리에
 * 놓거나 다시 치웁니다. 무엇을 꺼내 둘지는 아이가 정합니다.
 *
 * 모든 메서드는 메인 스레드에서 호출해야 합니다.
 */
class House(context: Context, private val pet: Pet) {

    /**
     * 방 안 물건 하나.
     * @param x, y 방 그림(1024×740) 안에서 물건 중심의 위치 비율
     * @param width 방 그림 너비에 대한 물건 그림(정사각형) 한 변의 비율
     * @param layer 겹치는 순서. 작을수록 뒤.
     */
    class Item(
        val id: String,
        val name: String,
        val unlockLevel: Int,
        val drawable: Int,
        val x: Float,
        val y: Float,
        val width: Float,
        val layer: Int,
    )

    private val prefs = context.applicationContext.getSharedPreferences("house", Context.MODE_PRIVATE)
    private val placed = LinkedHashSet<String>(prefs.getStringSet(KEY_PLACED, emptySet()).orEmpty())

    /** 아이가 집 화면에서 마지막으로 본 단계. 그 뒤에 열린 물건은 '새 물건'으로 표시합니다. */
    private var seenLevel = prefs.getInt(KEY_SEEN_LEVEL, 0)

    var onChanged: (() -> Unit)? = null

    fun isUnlocked(item: Item) = pet.level >= item.unlockLevel

    fun isPlaced(item: Item) = item.id in placed

    /** 집 화면을 연 뒤 아직 보지 않은, 새로 열린 물건들. */
    val newItems: List<Item> get() = ITEMS.filter { it.unlockLevel in (seenLevel + 1)..pet.level }

    /** 단계가 [from]에서 [to]로 오르며 새로 열린 물건들. */
    fun unlockedBetween(from: Int, to: Int): List<Item> = ITEMS.filter { it.unlockLevel in (from + 1)..to }

    /** 집 화면을 열었을 때: 지금까지 열린 물건은 모두 본 것으로 합니다. */
    fun markSeen() {
        if (seenLevel == pet.level) return
        seenLevel = pet.level
        prefs.edit().putInt(KEY_SEEN_LEVEL, seenLevel).apply()
        onChanged?.invoke()
    }

    /** 물건을 놓거나 치웁니다. 아직 안 열린 물건이면 아무것도 하지 않고 false. */
    fun toggle(item: Item): Boolean {
        if (!isUnlocked(item)) return false
        if (!placed.remove(item.id)) placed += item.id
        prefs.edit().putStringSet(KEY_PLACED, placed.toSet()).apply()
        onChanged?.invoke()
        return true
    }

    companion object {
        /** 열리는 순서대로. 위치는 방 그림(res/drawable-nodpi/room.webp) 기준입니다. */
        val ITEMS: List<Item> = listOf(
            Item("bed", "폭신한 방석", 5, R.drawable.item_bed, x = 0.50f, y = 0.70f, width = 0.28f, layer = 1),
            Item("bowl", "밥그릇", 10, R.drawable.item_bowl, x = 0.69f, y = 0.80f, width = 0.13f, layer = 3),
            Item("ball", "빨간 공", 20, R.drawable.item_ball, x = 0.40f, y = 0.86f, width = 0.10f, layer = 3),
            Item("rug", "동그란 러그", 30, R.drawable.item_rug, x = 0.50f, y = 0.76f, width = 0.36f, layer = 0),
            Item("lamp", "별 램프", 40, R.drawable.item_lamp, x = 0.71f, y = 0.58f, width = 0.15f, layer = 1),
            Item("frame", "뼈다귀 액자", 50, R.drawable.item_frame, x = 0.35f, y = 0.34f, width = 0.11f, layer = 0),
            Item("house", "작은 개집", 60, R.drawable.item_house, x = 0.29f, y = 0.57f, width = 0.21f, layer = 1),
            Item("plant", "화분", 70, R.drawable.item_plant, x = 0.78f, y = 0.86f, width = 0.13f, layer = 3),
            Item("toybox", "장난감 상자", 85, R.drawable.item_toybox, x = 0.24f, y = 0.86f, width = 0.17f, layer = 2),
            Item("trophy", "황금 트로피", 100, R.drawable.item_trophy, x = 0.65f, y = 0.35f, width = 0.09f, layer = 0),
        )

        /** 강아지가 앉는 자리(방석 위). */
        const val PUPPY_X = 0.50f
        const val PUPPY_Y = 0.63f
        const val PUPPY_WIDTH = 0.25f
        const val PUPPY_LAYER = 2

        private const val KEY_PLACED = "placed"
        private const val KEY_SEEN_LEVEL = "seenLevel"
    }
}

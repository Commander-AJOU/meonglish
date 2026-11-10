package com.ajikmeoreosseo.app.english

import android.content.Context
import com.ajikmeoreosseo.app.story.Lang
import com.ajikmeoreosseo.app.story.Line
import org.json.JSONObject
import java.time.LocalDate

/**
 * 아이의 영어 진도를 맡습니다.
 *
 * 한 가지 말은 서로 다른 날에 네 번 만나면 '꿈 구슬'(완전히 익힌 말)이 됩니다.
 *   1일째 새로 배움 → 다음 날 복습(우리말→영어) → 그다음 날 복습(영어→우리말) → 이틀 뒤 마지막 확인
 * 꿈 구슬이 된 말은 다시 나오지 않고, 그 자리는 아직 안 배운 새 말이 채웁니다.
 * 같은 말을 하루에 두 번 묻지 않습니다.
 *
 * 새 말은 세 갈래에서 번갈아 꺼냅니다: 지금 레벨의 손으로 쓴 말, 낱말(레벨이 올라도 계속), 그리고
 * 익힌 낱말을 엮어 만든 문장([Sentences]). 그래서 낱말이 늘수록 배울 문장도 늘어 거의 끝이 없습니다.
 *
 * 무엇을 얼마나 배울지는 아이와 상황에 맞춥니다.
 *  - 어떤 낱말을: 지금 상황(비, 시간대, 계절, 가는 곳, 오늘 이야기)에 맞는 말을 먼저, 나머지는 주제를 돌아가며([WordPicker]).
 *  - 얼마나: 요즘 따라 말하기를 잘 통과하면 새 말을 하나 더, 절반도 못 하면 줄이고 익숙한 복습부터([newPerTrip]).
 *  - 이동 중에도: 연달아 못 하면 그날 새 말을 그만 내고, 연달아 맞히면 하나를 더 냅니다([record]).
 *  - 단계: 절반도 못 맞히는 동안에는 강아지가 자라도 어려운 말로 넘어가지 않습니다([setMinLevel]).
 *
 * 어른은 단어장 화면에서 "이미 알아요" / "다시 배우기"로 바로잡을 수 있습니다.
 *
 * 모든 메서드는 메인 스레드에서 호출해야 합니다.
 */
class EnglishCoach(context: Context) {

    /** 한 번의 수업. [lines]를 차례로 말한 뒤 [EnglishCoach.finish]를 불러 진도를 올립니다. */
    class Lesson(val word: Word, val lines: List<Line>)

    data class WordState(val word: Word, val stage: Int, val mastered: Boolean, val dueToday: Boolean)

    data class Summary(
        val enabled: Boolean,
        val level: Int,
        val minLevel: Int,
        val mastered: List<WordState>,
        val learning: List<WordState>,
        val demoDayOffset: Int,
        /** 요즘 따라 말하기 통과율(0~1). 아직 잰 것이 적으면 null. */
        val successRate: Float?,
        /** 다음에 탈 때 새로 배울 말의 수와 그 말들(상황에 맞아 고른 말은 이유가 붙습니다). */
        val newPerTrip: Int,
        val nextNew: List<WordPicker.Pick>,
    )

    private class Progress(var stage: Int, var day: Long, var mastered: Boolean)

    private val prefs = context.applicationContext.getSharedPreferences("english", Context.MODE_PRIVATE)
    private val progress = HashMap<String, Progress>()
    private val tripQueue = ArrayDeque<Word>()
    private val metThisTrip = LinkedHashSet<Word>()

    var enabled = true
        private set
    var level = 1
        private set

    /** 강아지가 자란 만큼의 레벨 최소값. 강아지가 꼬마가 되면 2, 소년이 되면 3. 저장하지 않고 매번 강아지에서 받습니다. */
    var minLevel = 1
        private set

    /** 수업을 여는 한마디. 강아지 말투에 따라 바뀝니다. */
    var opening = DEFAULT_OPENING

    private var dayOffset = 0

    /** 지금 차 안팎의 상황을 알려 주는 방법. 새 말을 고를 때마다 물어봅니다. */
    var situation: () -> Situation = { Situation() }

    /** 최근 따라 말하기 결과(통과 여부). 오래된 것부터, 최대 [RECENT_WINDOW]개. */
    private val recent = ArrayDeque<Boolean>()

    // 이번 이동의 흐름
    private var tripMinutes: Int? = null
    private var missStreak = 0
    private var passStreak = 0
    private var easedOff = false
    private var bonusGiven = false

    /** 진도나 설정이 바뀔 때마다 호출됩니다. */
    var onChanged: (() -> Unit)? = null

    init {
        load()
    }

    // region 이야기 엔진에서 호출

    /** 이야기를 시작할 때 오늘의 수업 순서를 정합니다: 복습할 말을 먼저, 그 사이사이에 새 말. */
    fun beginTrip(remainingMinutes: Int? = null) {
        tripQueue.clear()
        metThisTrip.clear()
        tripMinutes = remainingMinutes
        missStreak = 0
        passStreak = 0
        easedOff = false
        bonusGiven = false
        if (!enabled) return
        levelUpIfReady()

        val reviews = dueReviews()
        val picks = freshPicks()
        val fresh = picks.map { it.word }
        android.util.Log.d(
            "EnglishCoach",
            "trip: rate=${successRate()} new=${newPerTrip()} reviews=${reviews.map { it.english }} " +
                "fresh=${picks.map { it.word.english + (it.reason?.let { r -> "($r)" } ?: "") }}",
        )

        val a = ArrayDeque(reviews)
        val b = ArrayDeque(fresh)
        while (a.isNotEmpty() || b.isNotEmpty()) {
            a.removeFirstOrNull()?.let(tripQueue::add)
            b.removeFirstOrNull()?.let(tripQueue::add)
        }
    }

    /** 오늘 타면 만날 말(복습 + 새 말)을 진도에 손대지 않고 미리 봅니다. 그날의 이야기에 이 말들을 녹여 넣을 때 씁니다. */
    fun plannedWords(): List<Word> {
        if (!enabled) return emptyList()
        return dueReviews() + freshPicks().map { it.word }
    }

    /**
     * 따라 말하기 결과를 적고, 이번 이동의 양을 조절합니다. 덧붙일 말이 있으면 돌려줍니다.
     * [measured]가 false면(차가 들을 수 없어 그냥 넘어간 경우) 아무것도 바꾸지 않습니다.
     *  - 두 번 연달아 못 하면: 오늘 남은 새 말은 그만 내고 복습만 합니다(지치지 않게).
     *  - 세 번 연달아 맞히면: 새 말을 하나 더 냅니다(한 번만).
     */
    fun record(word: Word, passed: Boolean, measured: Boolean): List<Line> {
        if (!enabled || !measured) return emptyList()
        recent.addLast(passed)
        while (recent.size > RECENT_WINDOW) recent.removeFirst()
        prefs.edit().putString(KEY_RECENT, recent.joinToString("") { if (it) "1" else "0" }).apply()

        if (passed) {
            passStreak += 1
            missStreak = 0
        } else {
            missStreak += 1
            passStreak = 0
        }
        android.util.Log.d("EnglishCoach", "record: ${word.english} passed=$passed rate=${successRate()} passStreak=$passStreak missStreak=$missStreak")
        if (missStreak >= EASE_OFF_AFTER_MISSES && !easedOff) {
            easedOff = true
            val before = tripQueue.size
            tripQueue.removeAll { stageOf(it) == 0 }
            if (tripQueue.size < before) return listOf(Line(EASE_OFF))
        }
        if (passStreak >= BONUS_AFTER_PASSES && !bonusGiven && !easedOff) {
            val extra = freshPicks(limit = MAX_NEW_PER_TRIP + 2).map { it.word }
                .firstOrNull { it.id != word.id && it !in tripQueue && it !in metThisTrip }
            if (extra != null) {
                bonusGiven = true
                tripQueue.addLast(extra)
                return listOf(Line(BONUS))
            }
        }
        return emptyList()
    }

    /** 요즘 따라 말하기 통과율. 잰 것이 [MIN_SAMPLES]개가 안 되면 null(아직 모름). */
    fun successRate(): Float? = if (recent.size < MIN_SAMPLES) null else recent.count { it } / recent.size.toFloat()

    private fun struggling(): Boolean = successRate()?.let { it < STRUGGLING_BELOW } == true

    /**
     * 한 번 탈 때 새로 배울 말의 수. 잘 통과하면 4, 보통이면 3, 절반도 못 하면 2.
     * 5분이 안 되는 짧은 이동에는 2개까지만 냅니다.
     */
    fun newPerTrip(): Int {
        val rate = successRate()
        val base = when {
            rate == null -> MAX_NEW_PER_TRIP
            rate >= DOING_WELL_FROM -> MAX_NEW_PER_TRIP + 1
            rate < STRUGGLING_BELOW -> MAX_NEW_PER_TRIP - 1
            else -> MAX_NEW_PER_TRIP
        }
        val minutes = tripMinutes
        return if (minutes != null && minutes < SHORT_TRIP_MINUTES) minOf(base, 2) else base
    }

    /** 오늘 복습할 말. 요즘 힘들어하면 거의 다 익힌(가장 익숙한) 말부터 물어 성공으로 시작하게 합니다. */
    private fun dueReviews(): List<Word> {
        val due = progress.keys.mapNotNull(WordBank::find).filter { isDue(it) }
        val ordered = if (struggling()) {
            due.sortedWith(compareByDescending<Word> { progress.getValue(it.id).stage }.thenBy { progress.getValue(it.id).day })
        } else {
            due.sortedBy { progress.getValue(it.id).day }
        }
        val minutes = tripMinutes
        return ordered.take(if (minutes != null && minutes < SHORT_TRIP_MINUTES) 2 else MAX_REVIEWS_PER_TRIP)
    }

    /** 동화 장과 장 사이에 넣을 다음 수업. 오늘 할 것이 더 없으면 null. */
    fun nextLesson(): Lesson? {
        if (!enabled) return null
        while (true) {
            val word = tripQueue.removeFirstOrNull() ?: return null
            if (practicedToday(word) || isMastered(word)) continue
            return lessonFor(word, short = false)
        }
    }

    /** 차가 실제로 [cue]처럼 움직였을 때 그 자리에서 할 짧은 수업. 오늘 이미 했으면 null. */
    fun cueLesson(cue: Cue): Lesson? {
        val word = cueWord(cue) ?: return null
        if (practicedToday(word)) return null
        return lessonFor(word, short = true)
    }

    /**
     * 차가 [cue]처럼 움직일 때 들려줄 말: 아직 꿈 구슬이 되지 않은 것 중 가장 쉬운 것.
     * 그 움직임이 일어날 때마다 들려주고, 진도는 [finishWord]가 하루에 한 번만 올립니다.
     */
    fun cueWord(cue: Cue): Word? {
        if (!enabled) return null
        return WordBank.words
            .filter { it.cue == cue && (it.level >= level || cue == Cue.HOW_FAR) }
            .firstOrNull { !isMastered(it) }
    }

    /**
     * 수업을 끝까지 말했을 때 진도를 올립니다.
     * 덧붙일 말(꿈 구슬 축하, 레벨 업)이 있으면 돌려줍니다.
     */
    fun finish(lesson: Lesson): List<Line> = finishWord(lesson.word)

    /** [word]를 서로 다른 날에 몇 번 만났는지(0이면 처음). */
    fun stageOf(word: Word): Int = progress[word.id]?.stage ?: 0

    /** 지금 [word]를 끝내면 진도에 들어가는지(오늘 처음 만났고 아직 꿈 구슬이 아닌지). 간식을 줄지 정할 때 씁니다. */
    fun willCount(word: Word): Boolean {
        val state = progress[word.id] ?: return true
        return state.day != today() && !state.mastered
    }

    /** [word]를 오늘 만났다고 적습니다. 같은 날 여러 번 만나도 한 번만 셉니다. */
    fun finishWord(word: Word): List<Line> {
        val today = today()
        val state = progress.getOrPut(word.id) { Progress(0, NEVER, false) }
        if (state.day == today || state.mastered) return emptyList()

        state.stage += 1
        state.day = today
        metThisTrip += word

        val extra = ArrayList<Line>()
        if (state.stage >= STAGES_TO_MASTER) {
            state.mastered = true
            extra += Line("이 말은 이제 완전히 알았네! 꿈 구슬이 하나 생겼어.")
        }
        if (levelUpIfReady()) {
            extra += Line("영어 실력이 쑥 자랐어! 다음부터는 조금 더 긴 말을 배워 보자.")
        }
        save()
        return extra
    }

    /** 도착했을 때 오늘 만난 말을 한 번 더 들려줍니다. */
    fun recap(): List<Line> {
        if (!enabled || metThisTrip.isEmpty()) return emptyList()
        return buildList {
            add(Line("오늘 만난 영어는,"))
            metThisTrip.take(MAX_RECAP_WORDS).forEach { add(Line(it.english, Lang.EN, pauseAfterMs = 500)) }
            add(Line("내일 또 물어볼게!"))
        }
    }

    /** 어떤 수업에서든 쓰는 말. 이야기를 시작할 때 미리 받아 두면 수업이 끊기지 않습니다. */
    fun commonLines(): List<String> =
        if (enabled) listOf(opening, EASE_OFF, BONUS, REPEAT, PRAISE_PASS, REMEMBER, RIGHT, REVERSE, MEANING, LAST_CHECK, TRY_AGAIN, TRY_AGAIN_SILENT, NEXT_TIME, ANSWER_IS, NEXT_TIME_REVIEW) else emptyList()

    // endregion

    // region 단어장 화면에서 호출

    fun summary(): Summary {
        // 단어장 순서대로, 만든 문장은 그 뒤에 만난 순서대로
        val order = WordBank.words.withIndex().associate { it.value.id to it.index }
        val states = progress.entries
            .sortedWith(compareBy({ order[it.key] ?: Int.MAX_VALUE }, { it.value.day }))
            .mapNotNull { (id, state) ->
                val word = WordBank.find(id) ?: return@mapNotNull null
                WordState(word, state.stage, state.mastered, isDue(word))
            }
        return Summary(
            enabled = enabled,
            level = level,
            minLevel = minLevel,
            mastered = states.filter { it.mastered },
            learning = states.filter { !it.mastered }.sortedByDescending { it.dueToday },
            demoDayOffset = dayOffset,
            successRate = successRate(),
            newPerTrip = newPerTrip(),
            nextNew = if (enabled) freshPicks() else emptyList(),
        )
    }

    fun setEnabled(value: Boolean) {
        enabled = value
        save()
    }

    /** 어른이 아이 수준에 맞춰 시작 레벨을 고릅니다. 강아지가 자라서 정해진 최소값 아래로는 내려가지 않습니다. */
    fun setLevel(value: Int) {
        level = value.coerceIn(minLevel, WordBank.MAX_LEVEL)
        save()
    }

    /**
     * 강아지 단계에 따른 레벨 최소값을 정합니다. 지금 레벨이 그보다 낮으면 끌어올립니다.
     * 레벨이 실제로 올랐으면 true.
     */
    fun setMinLevel(value: Int): Boolean {
        val wanted = value.coerceIn(1, WordBank.MAX_LEVEL)
        // 요즘 절반도 못 맞히면 강아지가 자라도 어려운 말로 넘어가지 않습니다. 다시 잘하게 되면 그때 올립니다
        // (이야기를 시작할 때마다 다시 확인합니다).
        minLevel = if (wanted > level && struggling()) level else wanted
        if (level >= minLevel) return false
        level = minLevel
        save()
        return true
    }

    /** "이미 알아요": 꿈 구슬로 만들어 다시 나오지 않게 합니다. */
    fun markMastered(word: Word) {
        progress[word.id] = Progress(STAGES_TO_MASTER, today(), true)
        levelUpIfReady()
        save()
    }

    /** "다시 배우기": 처음 보는 말로 되돌립니다. */
    fun forget(word: Word) {
        progress.remove(word.id)
        save()
    }

    /** 시연용: 날짜를 하루 넘겨 다음 날의 복습을 바로 볼 수 있게 합니다. */
    fun skipDayForDemo() {
        dayOffset += 1
        save()
    }

    // endregion

    // region 수업 만들기

    /**
     * 수업의 앞부분. 아이가 말할 차례인 줄([Line.listenFor])에서 엔진이 듣고, 결과에 따라 [feedback]을 이어 붙입니다.
     *   처음 만남: 뜻 → 영어 → "따라 해 볼까?" → 영어 (듣기)
     *   복습:     질문 (듣기) → 정답은 feedback에서
     */
    private fun lessonFor(word: Word, short: Boolean): Lesson {
        val stage = progress[word.id]?.stage ?: 0
        val meaning = "‘${word.korean}’."
        val english = word.english
        val lines = when {
            stage == 0 && short -> listOf(
                Line("$meaning 영어로는,"),
                Line(english, Lang.EN, pauseAfterMs = THINK_SHORT_MS, listenFor = word),
            )
            stage == 0 -> listOf(
                Line(opening),
                Line("$meaning 영어로는,"),
                Line(english, Lang.EN, pauseAfterMs = 600),
                Line(REPEAT),
                Line(english, Lang.EN, pauseAfterMs = THINK_MS, listenFor = word),
            )
            short -> listOf(
                Line("$meaning 영어로 뭐였지?", pauseAfterMs = THINK_SHORT_MS, listenFor = word),
            )
            stage == 1 -> listOf(
                Line(REMEMBER),
                Line("$meaning 영어로 뭐였지?", pauseAfterMs = THINK_MS, listenFor = word),
            )
            stage == 2 -> listOf(
                Line(REVERSE),
                Line(english, Lang.EN, pauseAfterMs = 400),
                Line(MEANING, pauseAfterMs = THINK_MS),
                Line("‘${word.korean}’!"),
                Line("영어로 한 번 더,", pauseAfterMs = 300),
                Line(english, Lang.EN, pauseAfterMs = THINK_MS, listenFor = word),
            )
            else -> listOf(
                Line(LAST_CHECK),
                Line("$meaning 영어로?", pauseAfterMs = THINK_MS, listenFor = word),
            )
        }
        return Lesson(word, lines)
    }

    /**
     * 아이가 말할 차례가 끝난 뒤 이어 말할 것.
     * 통과하면 칭찬, 아니면 정답을 들려줍니다(처음 배우는 말은 한 번 더 따라 하게 하고, 그래도 안 되면 다음에).
     * [heard]가 null이면 아무 말도 못 들은 것입니다.
     */
    fun feedback(word: Word, passed: Boolean, heard: String?, retry: Boolean): List<Line> {
        val stage = progress[word.id]?.stage ?: 0
        return when {
            passed && stage == 0 -> listOf(Line(PRAISE_PASS))
            passed -> listOf(Line(RIGHT), Line(word.english, Lang.EN))
            retry -> listOf(Line(if (heard == null) TRY_AGAIN_SILENT else TRY_AGAIN), Line(word.english, Lang.EN, pauseAfterMs = THINK_MS, listenFor = word))
            stage == 0 -> listOf(Line(NEXT_TIME))
            else -> listOf(Line(ANSWER_IS), Line(word.english, Lang.EN, pauseAfterMs = 400), Line(NEXT_TIME_REVIEW))
        }
    }
    // endregion

    // region 진도 규칙

    private fun today(): Long = LocalDate.now().toEpochDay() + dayOffset

    private fun isMastered(word: Word) = progress[word.id]?.mastered == true

    private fun practicedToday(word: Word) = progress[word.id]?.day == today()

    /** 배우는 중인 말이 복습할 날이 됐는지. */
    private fun isDue(word: Word): Boolean {
        val state = progress[word.id] ?: return false
        if (state.mastered || state.stage == 0) return false
        val wait = REVIEW_AFTER_DAYS.getOrElse(state.stage - 1) { REVIEW_AFTER_DAYS.last() }
        return today() >= state.day + wait
    }

    /** 손으로 쓴 말 가운데 아직 안 만난 것(차의 움직임과 묶인 말 제외). */
    private fun unseenFixed(atLevel: Int) =
        WordBank.words.filter { it.level == atLevel && it.cue == null && it.id !in progress }

    /** 익힌 낱말 가운데 [tags]에 해당하는 것. 먼저 익힌 순서대로. */
    private fun masteredOf(vararg tags: Tag): List<Word> =
        WordBank.words
            .filter { it.tag in tags && progress[it.id]?.mastered == true }
            .sortedBy { progress.getValue(it.id).day }

    /**
     * 이번에 새로 배울 말. 세 갈래에서 하나씩 번갈아 꺼냅니다.
     *   1) 지금 레벨의 손으로 쓴 말   2) 낱말(레벨 2부터도 계속)   3) 익힌 낱말로 만든 문장(레벨 2부터)
     * 낱말은 상황에 맞는 것을 먼저, 나머지는 주제를 돌아가며 고릅니다([WordPicker]).
     */
    private fun freshPicks(limit: Int = newPerTrip()): List<WordPicker.Pick> {
        val learned = progress.keys.mapNotNull(WordBank::find).filter { it.topic.isNotEmpty() }.groupingBy { it.topic }.eachCount()
        val vocabulary = WordPicker.order(unseenFixed(1), learned, situation())
        val sources = ArrayList<Iterator<WordPicker.Pick>>()
        if (level == 1) {
            sources += vocabulary.iterator()
        } else {
            sources += unseenFixed(level).map { WordPicker.Pick(it, null) }.iterator()
            sources += vocabulary.iterator()
            sources += Sentences.candidates(
                nouns = masteredOf(Tag.NOUN, Tag.MASS),
                adjectives = masteredOf(Tag.ADJ),
                colors = masteredOf(Tag.COLOR),
                level = level,
                seen = progress.keys,
            ).map { WordPicker.Pick(it, null) }.iterator()
        }
        val fresh = ArrayList<WordPicker.Pick>()
        while (fresh.size < limit && sources.any { it.hasNext() }) {
            for (source in sources) {
                if (fresh.size < limit && source.hasNext()) fresh += source.next()
            }
        }
        return fresh
    }

    /** 지금 레벨의 손으로 쓴 말을 다 꺼냈으면 다음 레벨로 올라갑니다(낱말은 많아서 보통 강아지가 자라며 올라갑니다). */
    private fun levelUpIfReady(): Boolean {
        if (level >= WordBank.MAX_LEVEL || unseenFixed(level).isNotEmpty()) return false
        level += 1
        return true
    }

    // endregion

    // region 저장

    private fun load() {
        enabled = prefs.getBoolean(KEY_ENABLED, true)
        level = prefs.getInt(KEY_LEVEL, 1).coerceIn(1, WordBank.MAX_LEVEL)
        // 최소값은 저장하지 않습니다. 앱이 켜질 때 강아지 단계에서 다시 받습니다.
        dayOffset = prefs.getInt(KEY_DAY_OFFSET, 0)
        prefs.getString(KEY_RECENT, "").orEmpty().takeLast(RECENT_WINDOW).forEach { recent.addLast(it == '1') }
        val saved = runCatching { JSONObject(prefs.getString(KEY_WORDS, "{}") ?: "{}") }.getOrDefault(JSONObject())
        for (id in saved.keys()) {
            if (WordBank.find(id) == null) continue
            val item = saved.optJSONObject(id) ?: continue
            progress[id] = Progress(item.optInt("stage"), item.optLong("day", NEVER), item.optBoolean("mastered"))
        }
    }

    private fun save() {
        val words = JSONObject()
        for ((id, state) in progress) {
            words.put(id, JSONObject().put("stage", state.stage).put("day", state.day).put("mastered", state.mastered))
        }
        prefs.edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putInt(KEY_LEVEL, level)
            .putInt(KEY_DAY_OFFSET, dayOffset)
            .putString(KEY_WORDS, words.toString())
            .apply()
        onChanged?.invoke()
    }

    // endregion

    companion object {
        /** 서로 다른 날에 이만큼 만나면 꿈 구슬이 됩니다. */
        const val STAGES_TO_MASTER = 4

        /** 1번째, 2번째, 3번째 만남 뒤에 며칠 있다가 다시 물을지. */
        private val REVIEW_AFTER_DAYS = listOf(1, 1, 2)

        private const val MAX_REVIEWS_PER_TRIP = 4
        private const val MAX_NEW_PER_TRIP = 3
        private const val MAX_RECAP_WORDS = 4

        // 아이 실력에 맞춘 양 조절
        private const val RECENT_WINDOW = 10
        private const val MIN_SAMPLES = 4
        private const val DOING_WELL_FROM = 0.8f
        private const val STRUGGLING_BELOW = 0.5f
        private const val SHORT_TRIP_MINUTES = 5
        private const val EASE_OFF_AFTER_MISSES = 2
        private const val BONUS_AFTER_PASSES = 3
        private const val THINK_MS = 2_800L
        private const val THINK_SHORT_MS = 2_000L
        private const val NEVER = Long.MIN_VALUE

        const val DEFAULT_OPENING = "{멍의} 영어 한 마디!"
        private const val REPEAT = "따라 해 볼까?"
        private const val EASE_OFF = "오늘 새 말은 여기까지 하고, 아는 말로 놀자!"
        private const val BONUS = "척척 맞히네! 새 말을 하나 더 배워 보자."
        private const val PRAISE_PASS = "잘했어! 통과!"
        private const val TRY_AGAIN = "거의 다 왔어! 한 번 더 같이 해 볼까?"
        private const val TRY_AGAIN_SILENT = "소리가 작았나 봐. 조금 더 크게 한 번 더!"
        private const val NEXT_TIME = "괜찮아, 다음에 또 해 보자."
        private const val ANSWER_IS = "정답은,"
        private const val NEXT_TIME_REVIEW = "내일 또 물어볼게!"
        private const val REMEMBER = "지난번에 배운 말, 기억나?"
        private const val RIGHT = "맞아!"
        private const val REVERSE = "이번엔 거꾸로 물어볼게."
        private const val MEANING = "무슨 뜻이었지?"
        private const val LAST_CHECK = "마지막 확인!"

        private const val KEY_ENABLED = "enabled"
        private const val KEY_LEVEL = "level"
        private const val KEY_DAY_OFFSET = "dayOffset"
        private const val KEY_WORDS = "words"
        private const val KEY_RECENT = "recent"
    }
}

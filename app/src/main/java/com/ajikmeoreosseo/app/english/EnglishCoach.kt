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
 * 꿈 구슬이 된 말은 날마다의 복습에서 빠지고, 그 자리는 아직 안 배운 새 말이 채웁니다.
 * 다만 잊지 않았는지 2주 뒤와 그로부터 한 달 뒤에 한 번씩 다시 묻습니다("꿈 구슬 닦기", [isCheck]).
 * 맞히면 구슬이 그대로 남고, 틀리게 말하면 구슬이 흐려져 배우는 말로 돌아갑니다. 두 번 다 맞힌 말은 더 묻지 않습니다.
 * 같은 말을 하루에 두 번 묻지 않습니다.
 *
 * 새 말은 세 갈래에서 번갈아 꺼냅니다: 지금 레벨의 손으로 쓴 말, 낱말(레벨이 올라도 계속), 그리고
 * 익힌 낱말을 엮어 만든 문장([Sentences]). 그래서 낱말이 늘수록 배울 문장도 늘어 거의 끝이 없습니다.
 *
 * 무엇을 얼마나 배울지는 아이와 상황에 맞춥니다(레벨 테스트 + 타면서 맞추기).
 *  - 레벨: 난이도는 다섯 단계([Levels])입니다. 어른이 설정에서 레벨 테스트([LevelTest], [applyLevelTest])를 하거나
 *    직접 고르고, 강아지가 자랄 때마다 한 단계씩 올라갑니다([setMinLevel]). 절반도 못 맞히는 동안에는 올리지 않습니다.
 *  - 어떤 낱말을: 지금 상황(비, 시간대, 계절, 가는 곳, 오늘 이야기)에 맞는 말을 먼저, 나머지는 주제를 돌아가며([WordPicker]).
 *  - 얼마나: 어른이 설정에서 "10분마다 몇 개"([perTen], 1~5)를 고릅니다. 이동이 길수록 그만큼 더 배웁니다([nextLesson]).
 *    요즘 절반도 못 맞히면 새 말을 줄이고 익숙한 복습부터 묻습니다.
 *  - 이동 중에도: 연달아 못 하면 그날 새 말을 그만 내고, 연달아 맞히면 하나를 더 냅니다([record]).
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
        val mastered: List<WordState>,
        val learning: List<WordState>,
        val demoDayOffset: Int,
        /** 요즘 따라 말하기 통과율(0~1). 아직 잰 것이 적으면 null. */
        val successRate: Float?,
        /** 10분마다 배우는 영어의 수(복습 포함, 1~5). */
        val perTen: Int,
        /** 다음에 탈 때 새로 배울 말들(상황에 맞아 고른 말은 이유가 붙습니다). */
        val nextNew: List<WordPicker.Pick>,
        /** 마지막으로 레벨 테스트를 한 날(없으면 null). */
        val testedOn: LocalDate?,
    )

    /** [checks]: 꿈 구슬이 된 뒤 오랜만의 확인을 통과한 횟수. */
    private class Progress(var stage: Int, var day: Long, var mastered: Boolean, var checks: Int = 0)

    private val prefs = context.applicationContext.getSharedPreferences("english", Context.MODE_PRIVATE)
    private val progress = HashMap<String, Progress>()
    private val tripQueue = ArrayDeque<Word>()
    private val metThisTrip = LinkedHashSet<Word>()

    var enabled = true
        private set
    /** 지금 난이도(1~[Levels.MAX]). */
    var level = 1
        private set

    /** 강아지가 자라며 이미 올려 준 레벨. 강아지가 더 자라 이 값보다 커질 때만 레벨을 다시 올립니다. */
    private var appliedFloor = 0

    /** 10분마다 배우는 영어의 수(복습 포함). 어른이 설정에서 고릅니다. */
    var perTen = DEFAULT_PER_TEN
        private set

    /** 마지막으로 레벨 테스트를 한 날(epoch day). 안 했으면 [NEVER]. */
    private var testedDay = NEVER

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
    private var tripStartedAt = 0L
    private var lessonsGiven = 0
    private var bonusQuota = 0

    /** 지금 시각(ms). 이동을 시작한 뒤 지난 시간으로 "10분마다 몇 개"를 맞춥니다. */
    var clock: () -> Long = { android.os.SystemClock.elapsedRealtime() }

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
        tripStartedAt = clock()
        lessonsGiven = 0
        bonusQuota = 0
        if (!enabled) return
        levelUpIfReady()

        // 긴 이동에도 모자라지 않게 넉넉히 줄을 세워 두고, 실제로 얼마나 할지는 지난 시간이 정합니다([nextLesson]).
        val reviews = dueReviews(MAX_REVIEWS_QUEUED)
        val picks = freshPicks(if (struggling()) MAX_NEW_STRUGGLING else MAX_NEW_QUEUED)
        tripQueue += interleave(reviews, picks.map { it.word })
        android.util.Log.d(
            "EnglishCoach",
            "trip: level=$level perTen=$perTen rate=${successRate()} reviews=${reviews.map { it.english }} " +
                "fresh=${picks.map { it.word.english + (it.reason?.let { r -> "($r)" } ?: "") }}",
        )
    }

    /** 복습할 말을 먼저, 그 사이사이에 새 말. 요즘 힘들어하면 복습 둘에 새 말 하나꼴로 섞습니다. */
    private fun interleave(reviews: List<Word>, fresh: List<Word>): List<Word> {
        val a = ArrayDeque(reviews)
        val b = ArrayDeque(fresh)
        val perNew = if (struggling()) 2 else 1
        return buildList {
            while (a.isNotEmpty() || b.isNotEmpty()) {
                repeat(perNew) { a.removeFirstOrNull()?.let(::add) }
                b.removeFirstOrNull()?.let(::add)
            }
        }
    }

    /**
     * [minutes]분 가는 동안 하게 될 수업의 수: 출발하자마자 하나, 그 뒤로 10분마다 [perTen]개꼴.
     * 가는 시간을 모르면 [DEFAULT_TRIP_MINUTES]분으로 봅니다.
     */
    fun expectedLessons(minutes: Int? = tripMinutes): Int = 1 + (minutes ?: DEFAULT_TRIP_MINUTES).coerceAtLeast(0) * perTen / 10

    /** 오늘 타면 만날 말(복습 + 새 말)을 진도에 손대지 않고 미리 봅니다. 그날의 이야기에 이 말들을 녹여 넣을 때 씁니다. */
    fun plannedWords(): List<Word> {
        if (!enabled) return emptyList()
        val count = expectedLessons()
        return interleave(dueReviews(count), freshPicks(count).map { it.word }).take(count)
    }

    /**
     * 이야기 속에 영어 메아리로 섞을 낱말: 이미 만난 낱말(꿈 구슬 포함)과 이번에 배울 낱말.
     * 이번에 배울 말을 앞에 두어, 수업 전에 이야기에서 먼저 한 번 듣게 합니다.
     */
    fun echoWords(): List<Word> {
        if (!enabled) return emptyList()
        val met = progress.entries.filter { it.value.stage > 0 || it.value.mastered }
            .sortedByDescending { it.value.day }
            .mapNotNull { WordBank.find(it.key) }
        return (tripQueue.toList() + met).filter { it.level == 1 }
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
            val extra = freshPicks(limit = MAX_NEW_QUEUED + 2).map { it.word }
                .firstOrNull { it.id != word.id && it !in tripQueue && it !in metThisTrip }
            if (extra != null) {
                bonusGiven = true
                // 덤으로 하나 더: 바로 다음 차례에, 10분마다 정한 수와 따로 셉니다.
                tripQueue.addFirst(extra)
                bonusQuota += 1
                return listOf(Line(BONUS))
            }
        }
        return emptyList()
    }

    /** 요즘 따라 말하기 통과율. 잰 것이 [MIN_SAMPLES]개가 안 되면 null(아직 모름). */
    fun successRate(): Float? = if (recent.size < MIN_SAMPLES) null else recent.count { it } / recent.size.toFloat()

    private fun struggling(): Boolean = successRate()?.let { it < STRUGGLING_BELOW } == true

    /** 오늘 복습할 말을 [limit]개까지. 요즘 힘들어하면 거의 다 익힌(가장 익숙한) 말부터 물어 성공으로 시작하게 합니다. */
    private fun dueReviews(limit: Int): List<Word> {
        // 오랜만에 다시 확인할 꿈 구슬은 한 번 탈 때 하나만, 가장 오래된 것부터 맨 앞에 둡니다.
        val polish = progress.keys.mapNotNull(WordBank::find).filter { isCheck(it) }
            .sortedBy { progress.getValue(it.id).day }.take(MAX_CHECKS_PER_TRIP)
        val due = polish + progress.keys.mapNotNull(WordBank::find).filter { isDue(it) }
        val ordered = if (struggling()) {
            due.sortedWith(compareByDescending<Word> { progress.getValue(it.id).stage }.thenBy { progress.getValue(it.id).day })
        } else {
            polish + due.drop(polish.size).sortedBy { progress.getValue(it.id).day }
        }
        return ordered.take(limit)
    }

    /** 이동을 시작한 뒤 지금까지 해도 되는 수업의 수: 출발하자마자 하나, 그 뒤로 10분마다 [perTen]개꼴. */
    private fun allowedSoFar(): Int {
        val minutes = (clock() - tripStartedAt).coerceAtLeast(0L) / 60_000f
        return 1 + (minutes * perTen / 10f).toInt() + bonusQuota
    }

    /**
     * 동화 장과 장 사이에 넣을 다음 수업. 오늘 할 것이 더 없거나, 10분마다 정한 수([perTen])를 이미 채웠으면 null
     * (그때는 쉬어 가고, 시간이 지나면 다시 나옵니다).
     */
    fun nextLesson(): Lesson? {
        if (!enabled || lessonsGiven >= allowedSoFar()) return null
        while (true) {
            val word = tripQueue.removeFirstOrNull() ?: return null
            if (practicedToday(word) || (isMastered(word) && !isCheck(word))) continue
            lessonsGiven += 1
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
            .filter { it.cue == cue && (it.level >= Levels.kind(level) || cue == Cue.HOW_FAR) }
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
            extra += Line("영어 실력이 쑥 자랐어! 다음부터는 조금 더 어려운 영어를 배워 보자.")
        }
        save()
        return extra
    }

    /** 꿈 구슬이 된 지 오래되어 잊지 않았는지 다시 확인할 차례인지. */
    fun isCheck(word: Word): Boolean {
        val state = progress[word.id] ?: return false
        if (!state.mastered) return false
        val wait = CHECK_AFTER_DAYS.getOrNull(state.checks) ?: return false
        return today() >= state.day + wait
    }

    /**
     * 꿈 구슬 닦기를 끝냈을 때. 맞혔으면 구슬이 남고(두 번 맞히면 더 묻지 않음), 틀리게 말했으면([misheard])
     * 구슬이 흐려져 배우는 말로 돌아갑니다. 아무 말도 못 들었으면 차가 못 들었을 수 있으므로 그대로 두고 다음에 다시 묻습니다.
     */
    fun finishCheck(word: Word, passed: Boolean, misheard: Boolean): List<Line> {
        val state = progress[word.id] ?: return emptyList()
        if (!isCheck(word)) return emptyList()
        state.day = today()
        metThisTrip += word
        val lines = when {
            passed -> {
                state.checks += 1
                listOf(Line(CHECK_KEPT))
            }
            misheard -> {
                state.mastered = false
                state.stage = STAGES_TO_MASTER - 2
                state.checks = 0
                listOf(Line(CHECK_FADED))
            }
            else -> emptyList()
        }
        save()
        return lines
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
        if (enabled) listOf(opening, CHECK_OPEN, CHECK_KEPT, CHECK_FADED, CHECK_LATER, EASE_OFF, BONUS, REPEAT, PRAISE_PASS, REMEMBER, RIGHT, REVERSE, MEANING, LAST_CHECK, TRY_AGAIN, TRY_AGAIN_SILENT, NEXT_TIME, ANSWER_IS, NEXT_TIME_REVIEW) else emptyList()

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
            mastered = states.filter { it.mastered },
            learning = states.filter { !it.mastered }.sortedByDescending { it.dueToday },
            demoDayOffset = dayOffset,
            successRate = successRate(),
            perTen = perTen,
            nextNew = if (enabled) freshPicks(NEXT_NEW_SHOWN) else emptyList(),
            testedOn = if (testedDay == NEVER) null else LocalDate.ofEpochDay(testedDay - dayOffset),
        )
    }

    fun setEnabled(value: Boolean) {
        enabled = value
        save()
    }

    /** 어른이 아이 수준에 맞춰 레벨을 고릅니다(1~[Levels.MAX]). 어른이 고른 값을 그대로 따릅니다. */
    fun setLevel(value: Int) {
        level = value.coerceIn(1, Levels.MAX)
        save()
    }

    /** 10분마다 배우는 영어의 수(복습 포함)를 고릅니다(1~[MAX_PER_TEN]). */
    fun setPerTen(value: Int) {
        perTen = value.coerceIn(1, MAX_PER_TEN)
        save()
    }

    /**
     * 레벨 테스트 결과를 반영합니다: 레벨을 [result]로 맞추고, 아이가 맞힌 말([known])은 이미 아는 말이므로
     * 꿈 구슬로 넣어 다시 가르치지 않습니다(2주 뒤 꿈 구슬 닦기에서 한 번 더 확인합니다).
     */
    fun applyLevelTest(result: Int, known: List<Word>) {
        level = result.coerceIn(1, Levels.MAX)
        testedDay = today()
        val today = today()
        known.forEach { progress[it.id] = Progress(STAGES_TO_MASTER, today, true) }
        save()
    }

    /**
     * 강아지가 자란 만큼 레벨을 올립니다([floor]는 강아지 모습에 따른 레벨). 강아지가 새 모습으로 자랐을 때 한 번만 올리므로,
     * 그 뒤에 어른이 직접 고르거나 레벨 테스트로 낮춘 레벨은 그대로 남습니다. 레벨이 실제로 올랐으면 true.
     */
    fun setMinLevel(floor: Int): Boolean {
        val wanted = floor.coerceIn(1, Levels.MAX)
        if (wanted <= appliedFloor) return false
        // 요즘 절반도 못 맞히면 강아지가 자라도 어려운 말로 넘어가지 않습니다. 다시 잘하게 되면 그때 올립니다
        // (이야기를 시작할 때마다 다시 확인합니다).
        if (wanted > level && struggling()) return false
        appliedFloor = wanted
        val raised = level < wanted
        if (raised) level = wanted
        save()
        return raised
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
            isCheck(word) -> listOf(
                Line(CHECK_OPEN),
                Line("$meaning 영어로 뭐였지?", pauseAfterMs = THINK_MS, listenFor = word),
            )
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
            // 꿈 구슬 닦기: 결과에 따른 말은 [finishCheck]가 덧붙입니다.
            isCheck(word) && !passed ->
                listOf(Line(ANSWER_IS), Line(word.english, Lang.EN, pauseAfterMs = 400)) +
                    if (heard == null) listOf(Line(CHECK_LATER)) else emptyList()
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

    /** 난이도가 [difficulty]인 단어장의 말 가운데 아직 안 만난 것(차의 움직임과 묶인 말 제외). */
    private fun unseenAt(difficulty: Int) =
        WordBank.words.filter { it.cue == null && it.id !in progress && Levels.of(it) == difficulty }

    /** 익힌 낱말 가운데 [tags]에 해당하는 것. 먼저 익힌 순서대로. */
    private fun masteredOf(vararg tags: Tag): List<Word> =
        WordBank.words
            .filter { it.tag in tags && progress[it.id]?.mastered == true }
            .sortedBy { progress.getValue(it.id).day }

    /**
     * 이번에 새로 배울 말을 [limit]개까지. 여러 갈래에서 하나씩 번갈아 꺼냅니다.
     *   레벨 1: 쉬운 주제의 단어만          레벨 2: 모든 단어
     *   레벨 3~5: 그 레벨까지의 손으로 쓴 표현·문장 + 단어(계속) + 익힌 낱말로 만든 문장([Sentences])
     * 단어는 상황에 맞는 것을 먼저, 나머지는 주제를 돌아가며 고릅니다([WordPicker]).
     */
    private fun freshPicks(limit: Int): List<WordPicker.Pick> {
        val learned = progress.keys.mapNotNull(WordBank::find).filter { it.topic.isNotEmpty() }.groupingBy { it.topic }.eachCount()
        val unseenWords = WordBank.words.filter { it.level == 1 && it.cue == null && it.id !in progress }
        // 레벨 1에서는 쉬운 주제만. 다 배웠으면 나머지 단어로 넘어갑니다.
        val pool = if (level == 1) unseenWords.filter { Levels.of(it) == 1 }.ifEmpty { unseenWords } else unseenWords
        val vocabulary = WordPicker.order(pool, learned, situation())
        val sources = ArrayList<Iterator<WordPicker.Pick>>()
        if (level < FIRST_PHRASE_LEVEL) {
            sources += vocabulary.iterator()
        } else {
            // 지금 레벨의 말과 함께, 아래 레벨에서 아직 못 만난 표현도 계속 냅니다. 강아지가 빨리 자라 레벨이
            // 먼저 올라가도 짧은 표현이 통째로 건너뛰어지지 않게 하려는 것입니다.
            for (difficulty in level downTo FIRST_PHRASE_LEVEL) {
                sources += unseenAt(difficulty).map { WordPicker.Pick(it, null) }.iterator()
            }
            sources += vocabulary.iterator()
            sources += Sentences.candidates(
                nouns = masteredOf(Tag.NOUN, Tag.MASS),
                adjectives = masteredOf(Tag.ADJ),
                colors = masteredOf(Tag.COLOR),
                level = Levels.kind(level),
                seen = progress.keys,
            ).filter { Levels.of(it) <= level }.map { WordPicker.Pick(it, null) }.iterator()
        }
        val fresh = ArrayList<WordPicker.Pick>()
        while (fresh.size < limit && sources.any { it.hasNext() }) {
            for (source in sources) {
                if (fresh.size < limit && source.hasNext()) fresh += source.next()
            }
        }
        return fresh
    }

    /** 지금 레벨의 말을 다 꺼냈으면 다음 레벨로 올라갑니다(단어는 많아서 보통 강아지가 자라며 올라갑니다). */
    private fun levelUpIfReady(): Boolean {
        if (level >= Levels.MAX || unseenAt(level).isNotEmpty()) return false
        level += 1
        return true
    }

    // endregion

    // region 저장

    private fun load() {
        enabled = prefs.getBoolean(KEY_ENABLED, true)
        // 예전(세 단계: 낱말·짧은 표현·문장)에 저장된 레벨은 다섯 단계의 1·3·4로 옮깁니다.
        level = if (prefs.contains(KEY_STAGE)) prefs.getInt(KEY_STAGE, 1) else listOf(1, 3, 4)[prefs.getInt(KEY_LEVEL, 1).coerceIn(1, 3) - 1]
        level = level.coerceIn(1, Levels.MAX)
        appliedFloor = prefs.getInt(KEY_FLOOR, 0)
        perTen = prefs.getInt(KEY_PER_TEN, DEFAULT_PER_TEN).coerceIn(1, MAX_PER_TEN)
        testedDay = prefs.getLong(KEY_TESTED, NEVER)
        dayOffset = prefs.getInt(KEY_DAY_OFFSET, 0)
        prefs.getString(KEY_RECENT, "").orEmpty().takeLast(RECENT_WINDOW).forEach { recent.addLast(it == '1') }
        val saved = runCatching { JSONObject(prefs.getString(KEY_WORDS, "{}") ?: "{}") }.getOrDefault(JSONObject())
        for (id in saved.keys()) {
            if (WordBank.find(id) == null) continue
            val item = saved.optJSONObject(id) ?: continue
            progress[id] = Progress(item.optInt("stage"), item.optLong("day", NEVER), item.optBoolean("mastered"), item.optInt("checks"))
        }
    }

    private fun save() {
        val words = JSONObject()
        for ((id, state) in progress) {
            val item = JSONObject().put("stage", state.stage).put("day", state.day).put("mastered", state.mastered)
            if (state.checks > 0) item.put("checks", state.checks)
            words.put(id, item)
        }
        prefs.edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putInt(KEY_STAGE, level)
            .putInt(KEY_FLOOR, appliedFloor)
            .putInt(KEY_PER_TEN, perTen)
            .putLong(KEY_TESTED, testedDay)
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

        /** 표현과 문장을 배우기 시작하는 레벨. 그 아래(1·2)는 단어만 배웁니다. */
        private const val FIRST_PHRASE_LEVEL = 3

        // 얼마나 배울지: 10분마다 [perTen]개(복습 포함)
        const val DEFAULT_PER_TEN = 3
        const val MAX_PER_TEN = 5
        /** 가는 시간을 모를 때(목적지를 안 정했을 때) 이만큼 간다고 봅니다. */
        const val DEFAULT_TRIP_MINUTES = 15
        /** 한 번 탈 때 줄 세워 두는 복습과 새 말의 최대 수. 1시간 넘게 가도 새 말은 이만큼까지만 배웁니다. */
        private const val MAX_REVIEWS_QUEUED = 16
        private const val MAX_NEW_QUEUED = 12
        private const val MAX_NEW_STRUGGLING = 4
        private const val NEXT_NEW_SHOWN = 3

        /** 꿈 구슬이 된 뒤 다시 확인하기까지의 날수: 2주 뒤, 그리고 그로부터 한 달 뒤. */
        private val CHECK_AFTER_DAYS = listOf(14, 30)
        private const val MAX_CHECKS_PER_TRIP = 1
        private const val MAX_RECAP_WORDS = 4

        // 아이 실력에 맞춘 양 조절
        private const val RECENT_WINDOW = 10
        private const val MIN_SAMPLES = 4
        private const val DOING_WELL_FROM = 0.8f
        private const val STRUGGLING_BELOW = 0.5f
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
        private const val CHECK_OPEN = "오랜만에 꿈 구슬 하나 닦아 볼까?"
        private const val CHECK_KEPT = "아직도 기억하네! 구슬이 더 반짝반짝해졌어."
        private const val CHECK_FADED = "구슬이 조금 흐려졌네. 괜찮아, 다시 배우면 금방 반짝일 거야."
        private const val CHECK_LATER = "다음에 또 닦아 보자."

        private const val KEY_ENABLED = "enabled"
        private const val KEY_LEVEL = "level"
        private const val KEY_STAGE = "stage"
        private const val KEY_FLOOR = "floor"
        private const val KEY_PER_TEN = "perTen"
        private const val KEY_TESTED = "testedDay"
        private const val KEY_DAY_OFFSET = "dayOffset"
        private const val KEY_WORDS = "words"
        private const val KEY_RECENT = "recent"
    }
}

package com.ajikmeoreosseo.app.story

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.ajikmeoreosseo.app.car.CarEvent
import com.ajikmeoreosseo.app.car.CarEvent.Gear.Position
import com.ajikmeoreosseo.app.car.NaviSnapshot
import com.ajikmeoreosseo.app.english.AnswerListener
import com.ajikmeoreosseo.app.english.Cue
import com.ajikmeoreosseo.app.english.EnglishCoach
import com.ajikmeoreosseo.app.english.Word
import com.ajikmeoreosseo.app.pet.Pet
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 이야기 진행을 맡습니다.
 *
 * - 본문([Story.script])을 한 문장씩 읽습니다. 대본은 강아지 말투([Tone])에 따라 조금 다릅니다.
 * - 장과 장 사이에 영어 수업([EnglishCoach])을 한 가지씩 끼워 넣습니다. 수업을 마칠 때마다 강아지 간식([Pet])이 생깁니다.
 * - 차량 신호가 오면 반응 대사를 먼저 말하고, 그 움직임에 해당하는 영어를 그 자리에서 가르칩니다.
 * - 영어를 물은 뒤에는 아이가 따라 말하는지 듣고([AnswerListener]), 통과해야 간식을 줍니다.
 * - 내비 음성 안내가 나오면 낭독부가 멈췄다가 이어 읽고, 그 뒤 안내 내용을 이야기 속 말로 받아 줍니다
 *   ("길 안내 요정이 오른쪽으로 가래!", "여기 하늘 길은 60까지만 날 수 있대"). 내비 값은 이야기 길이와 "아직 멀었어?"에도 씁니다.
 * - 뒷자리 안전벨트가 풀려 있으면 맬 때까지 본문을 멈춥니다.
 * - 도착하면(내비 도착 또는 주행 후 P단) 마무리 대사로 넘어갑니다.
 *
 * 모든 메서드는 메인 스레드에서 호출해야 합니다.
 */
class StoryEngine(
    private val narrator: Narrator,
    private val coach: EnglishCoach,
    private val pet: Pet,
    private val listener: AnswerListener,
    private val ui: Ui,
) {

    interface Ui {
        /** 강아지가 방금 한 말. [english]이면 가르치는 영어입니다. */
        fun onCaption(text: String, english: Boolean)
        fun onScene(chapterTitle: String, progress: Float)
        fun onPhase(phase: Phase)
        /** 아이가 말할 차례를 듣고 있는지. */
        fun onListening(active: Boolean)
    }

    enum class Phase { IDLE, WAITING_BELT, TELLING, HOLDING, ENDING, ENDED }

    var phase = Phase.IDLE
        private set

    /** 이야기의 주인공. 바꾸면 다음 문장부터 반영됩니다. */
    var childName = ChildName(null)

    /** 오늘 들려줄 이야기를 고르는 방법. 기본은 고정 이야기이고, 화면이 그날 만든 이야기로 바꿔 끼웁니다. */
    var packProvider: (Tone) -> StoryPack = { Story.pack(it) }

    // 이번 이야기의 대본. 시작할 때 강아지 말투에 맞춰 고릅니다.
    private var script: List<Beat> = Story.script(Tone.KID)
    private var interludes: List<Beat.Say> = Story.interludes(Tone.KID)
    private var ending: List<String> = Story.ending(Tone.KID)

    /**
     * 이어서 말할 한 묶음(반응 대사와 그에 딸린 영어, 또는 영어 수업 하나).
     *
     * @param urgent 차량 신호처럼 바로 말해야 하는 묶음. 수업 도중에도 문장 사이에 끼어듭니다.
     * @param expiresAt 이 시각이 지나면 때를 놓친 것으로 보고 말하지 않습니다.
     * @param onDone 다 말한 뒤 호출됩니다. 덧붙일 말이 있으면 돌려줍니다.
     */
    private class Sequence(
        val key: String,
        lines: List<Line>,
        val urgent: Boolean,
        val expiresAt: Long = Long.MAX_VALUE,
        val onDone: (() -> List<Line>)? = null,
    ) {
        val lines = ArrayDeque(lines)
    }

    private val handler = Handler(Looper.getMainLooper())
    private val pump = Runnable { pumpNow() }
    private val stopCheck = Runnable { onStoppedForAWhile() }

    private val urgent = ArrayDeque<Sequence>()
    private var current: Sequence? = null
    private var suspended: Sequence? = null
    private val lastFiredAt = HashMap<String, Long>()
    private val variantIndex = HashMap<String, Int>()
    private val lastShoutAt = HashMap<Cue, Long>()
    private var lastCaption: Pair<String, Boolean>? = null
    private var lastCalmAt = -CALM_DEBOUNCE_MS
    private var calmIndex = 0
    private var calming = false
    private val calmTimeout = Runnable { endCalm() }

    private var speaking = false

    /** 아이가 말할 차례를 듣는 중. 그동안은 다음 말을 하지 않습니다. */
    private var listening = false

    /** 이번 이야기에서 말마다 몇 번 시도했는지와 통과 여부. */
    private val attempts = HashMap<String, Int>()
    private val passed = HashMap<String, Boolean>()
    private var cursor = 0
    private var endingCursor = 0
    private var interludeCursor = 0
    private var holdTurn = 0
    private var recapDone = false
    private var chapterTitle = ""

    /** 이 시각 전에는 아무 말도 하지 않습니다(아이가 따라 말할 틈). */
    private var resumeAt = 0L

    /** 이 시각 전에는 본문과 수업을 이어 가지 않습니다(장 사이 쉼). 차량 신호 반응은 예외입니다. */
    private var nextStoryAt = 0L

    // 차량 상태
    private var gear = Position.UNKNOWN
    private var wiperOn = false
    private var rearOccupied = false
    private var rearBuckled = false
    private var beltConfirmedByAdult = false
    private var leftParkDuringStory = false
    private var announcedStop = false
    private var doorLineSaid = false

    // 내비
    private var navi = NaviSnapshot()
    private var tripUpdatedAt = 0L
    private var lastNaviReactionAt = 0L
    private var reactedTurn: String? = null
    private var reactedLimit: Int? = null
    private var reactedCamera: String? = null

    init {
        narrator.onInterruptionEnded = { onNaviSpoke() }
    }

    // region 화면에서 호출

    fun start() {
        if (phase != Phase.IDLE && phase != Phase.ENDED) return
        reset()
        // 강아지가 자란 만큼 말투와 영어 난이도가 바뀝니다. 이야기는 그날의 것(생성) 또는 기본 이야기입니다.
        val tone = pet.tone
        val pack = packProvider(tone)
        script = pack.script
        interludes = pack.interludes
        ending = pack.ending
        coach.opening = Story.lessonOpening(tone)
        coach.setMinLevel(pet.englishFloor)
        coach.beginTrip(navi.remainingSec?.let { it / 60 })
        // 이미 달리는 중에 시작했다면, 다음 P단을 도착으로 봅니다.
        leftParkDuringStory = gear != Position.PARK && gear != Position.UNKNOWN
        setPhase(Phase.TELLING)
        pumpNow()
        warmUp()
    }

    fun stop() {
        handler.removeCallbacks(pump)
        handler.removeCallbacks(stopCheck)
        listener.cancel()
        narrator.stop()
        reset()
        setPhase(Phase.IDLE)
    }

    /** 아이가 "아직 멀었어?" 하고 물었을 때. 이야기 속 말로 남은 길을 알려 줍니다. */
    fun askHowFar() {
        urgent.removeAll { it.key == KEY_HOW_FAR }
        urgent.addFirst(aside(KEY_HOW_FAR, howFarAnswer(), Cue.HOW_FAR))
        pumpNow()
    }

    /**
     * 부모의 비밀 버튼: 아이가 울 때 강아지가 달랩니다. 읽던 문장을 잠깐 멈추고 바로 말한 뒤 이어 읽습니다.
     * 음성이 아직 준비되지 않았거나 차량 기본 음성이면 지금 문장이 끝나는 대로 말합니다.
     */
    fun calmChild() {
        if (!isStoryRunning()) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastCalmAt < CALM_DEBOUNCE_MS) return
        lastCalmAt = now
        val lines = Reactions.calm[calmIndex++ % Reactions.calm.size]
        val texts = lines.map { render(it) }
        val started = narrator.interject(texts.map { it to Lang.KO }) { endCalm() }
        if (started) {
            Log.d(TAG, "calm: ${texts.first()}")
            // 달래는 동안에는 다음 문장을 시작하지 않습니다. 소리가 끝났다는 신호가 오지 않아도 이야기가 멈추지 않게 합니다.
            calming = true
            handler.postDelayed(calmTimeout, texts.sumOf { Narrator.estimateMillis(it) } + CALM_TIMEOUT_MARGIN_MS)
            ui.onCaption(texts.first(), false)
        } else {
            enqueue(Sequence(KEY_CALM, lines.map { Line(it) }, urgent = true))
        }
    }

    private fun endCalm() {
        handler.removeCallbacks(calmTimeout)
        calming = false
        lastCaption?.let { (text, english) -> ui.onCaption(text, english) }
        pumpNow()
    }

    /** 어른이 화면에서 "벨트 맸어요"를 눌렀을 때. 벨트 신호가 없는 차에서도 진행할 수 있게 합니다. */
    fun confirmBeltByAdult() {
        beltConfirmedByAdult = true
        resumeFromBeltIfReady()
    }

    // endregion

    // region 차량·내비에서 호출

    fun onCarEvent(event: CarEvent) {
        when (event) {
            is CarEvent.TurnSignal -> when (event.side) {
                // 방향지시등만 그 순간에 바로 반응합니다. 나머지 신호는 읽던 문장이 끝난 뒤에 반응합니다.
                CarEvent.TurnSignal.Side.LEFT ->
                    react(KEY_TURN, COOLDOWN_TURN, Reactions.turnLeft, Cue.LEFT, shoutNow = true)
                CarEvent.TurnSignal.Side.RIGHT ->
                    react(KEY_TURN, COOLDOWN_TURN, Reactions.turnRight, Cue.RIGHT, shoutNow = true)
                CarEvent.TurnSignal.Side.OFF -> Unit
            }

            is CarEvent.Wiper -> {
                if (event.on && !wiperOn) react(KEY_RAIN, COOLDOWN_RAIN, Reactions.rainStart, Cue.RAIN)
                if (!event.on && wiperOn) react(KEY_RAIN_END, COOLDOWN_RAIN, Reactions.rainEnd, null)
                wiperOn = event.on
            }

            is CarEvent.Brake -> {
                handler.removeCallbacks(stopCheck)
                if (event.pressed) {
                    handler.postDelayed(stopCheck, STOP_HOLD_MS)
                } else if (announcedStop) {
                    announcedStop = false
                    react(KEY_GO, 0, Reactions.goAgain, Cue.GO)
                }
            }

            is CarEvent.Gear -> onGear(event.position)

            is CarEvent.RearSeat -> {
                rearOccupied = event.occupied
                if (!rearOccupied) beltConfirmedByAdult = false
            }

            is CarEvent.RearBelt -> onRearBelt(event.buckled)

            is CarEvent.RearDoor -> {
                val arrived = phase == Phase.ENDING || phase == Phase.ENDED
                if (event.open && arrived && gear == Position.PARK && !doorLineSaid) {
                    doorLineSaid = true
                    enqueue(aside(KEY_DOOR, Reactions.DOOR_AFTER_ARRIVAL, null))
                }
            }
        }
    }

    /** 내비 상태가 바뀔 때마다. 남은 시간은 장 사이 쉼과 "아직 멀었어?" 대답에 씁니다. */
    fun onNavi(snapshot: NaviSnapshot) {
        if (snapshot.remainingSec != navi.remainingSec) tripUpdatedAt = SystemClock.elapsedRealtime()
        navi = snapshot
        // 내비 소리가 오디오 포커스를 안 가져가는 차를 위한 보조: 다음 안내가 코앞이면 안내 뒤 반응과 같은 말을 합니다.
        val meters = snapshot.nextTurnMeters
        if (snapshot.nextTurn != null && snapshot.nextTurn != reactedTurn && meters != null && meters in 1..NAVI_TURN_NEAR_METERS) {
            onNaviSpoke()
        }
    }

    /**
     * 내비 안내(또는 다른 소리)가 끝나 이야기가 다시 시작될 때. 방금 들은 안내를 이야기 속 말로 받아 줍니다.
     * 우선순위: 과속 카메라 → 다음 안내(방향) → 제한 속도 → 남은 시간. 같은 내용은 한 번만, 너무 잦지 않게.
     */
    private fun onNaviSpoke() {
        if (!isStoryRunning() || !navi.hasRoute) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastNaviReactionAt < NAVI_REACTION_COOLDOWN_MS) return
        val line = naviStoryLine() ?: return
        lastNaviReactionAt = now
        enqueue(aside(KEY_NAVI, line, null, expiresAt = now + NAVI_FRESH_MS))
    }

    private fun naviStoryLine(): String? {
        val camera = navi.cameraId
        val cameraMeters = navi.cameraMeters
        if (camera != null && camera != reactedCamera && cameraMeters != null && cameraMeters <= CAMERA_WARN_METERS) {
            reactedCamera = camera
            return pick(KEY_NAVI_CAMERA, Reactions.naviCamera)
        }
        val turn = navi.nextTurn
        if (turn != null && turn != reactedTurn) {
            reactedTurn = turn
            val words = Reactions.naviTurnWords[turn] ?: return null
            return pick(KEY_NAVI_TURN, Reactions.naviTurn).replace("{안내}", words)
        }
        val limit = navi.speedLimit
        if (limit != null && limit != reactedLimit) {
            reactedLimit = limit
            return pick(KEY_NAVI_SPEED, Reactions.naviSpeed).replace("{속도}", limit.toString())
        }
        val remaining = currentRemainingSec() ?: return null
        val minutes = max(1, (remaining / 60f).roundToInt())
        val meters = navi.remainingMeters ?: return null
        return pick(KEY_NAVI_REMAINING, Reactions.naviRemaining)
            .replace("{분}", minutes.toString())
            .replace("{거리}", if (meters >= 1000) String.format(java.util.Locale.KOREA, "%.1f킬로미터", meters / 1000f) else "${(meters / 10) * 10}미터")
    }

    /** 같은 종류의 말을 돌아가며 고릅니다. */
    private fun pick(key: String, variants: List<String>): String {
        val index = variantIndex[key] ?: 0
        variantIndex[key] = index + 1
        return variants[index % variants.size]
    }

    fun onTripArrived() = arrive()

    // endregion

    private fun onGear(position: Position) {
        val previous = gear
        gear = position
        if (!isStoryRunning()) return
        when (position) {
            Position.PARK -> if (leftParkDuringStory) arrive()
            Position.REVERSE -> {
                leftParkDuringStory = true
                react(KEY_REVERSE, COOLDOWN_REVERSE, Reactions.reverse, Cue.BACK)
            }
            Position.DRIVE, Position.NEUTRAL -> leftParkDuringStory = true
            Position.UNKNOWN -> Unit
        }
        // 주행 중에 벨트가 풀린 채로 출발하지 않도록 한 번 더 확인합니다.
        if (previous == Position.PARK && position != Position.PARK && needsBelt()) pauseForBelt(Reactions.BELT_ASK)
    }

    private fun onRearBelt(buckled: Boolean) {
        val wasBuckled = rearBuckled
        rearBuckled = buckled
        if (buckled) {
            resumeFromBeltIfReady()
        } else if (wasBuckled && rearOccupied && isStoryRunning() && gear != Position.PARK) {
            beltConfirmedByAdult = false
            pauseForBelt(Reactions.BELT_LOST)
        }
    }

    private fun needsBelt() = rearOccupied && !rearBuckled && !beltConfirmedByAdult

    private fun pauseForBelt(line: String) {
        if (phase == Phase.WAITING_BELT) return
        setPhase(Phase.WAITING_BELT)
        enqueue(aside(KEY_BELT, line, null))
    }

    private fun resumeFromBeltIfReady() {
        if (phase != Phase.WAITING_BELT || needsBelt()) return
        setPhase(if (cursor >= script.size) Phase.HOLDING else Phase.TELLING)
        enqueue(aside(KEY_BELT, Reactions.BELT_OK, Cue.BELT))
    }

    private fun onStoppedForAWhile() {
        if (!isStoryRunning() || gear == Position.PARK) return
        if (react(KEY_STOP, COOLDOWN_STOP, Reactions.stopped, Cue.STOP)) announcedStop = true
    }

    private fun arrive() {
        if (!isStoryRunning()) return
        urgent.clear()
        suspended = null
        if (current?.urgent == false) current = null
        endingCursor = 0
        nextStoryAt = 0L
        setPhase(Phase.ENDING)
        ui.onScene("도착", 1f)
        pumpNow()
    }

    private fun isStoryRunning() =
        phase == Phase.TELLING || phase == Phase.HOLDING || phase == Phase.WAITING_BELT

    // region 진행 루프

    /**
     * 지금 말할 것이 있으면 말합니다.
     * 순서: 차량 신호 반응 → 하던 영어 수업 → 본문(장 사이에서는 새 영어 수업).
     */
    private fun pumpNow() {
        handler.removeCallbacks(pump)
        if (speaking || listening || calming) return

        val now = SystemClock.elapsedRealtime()
        if (now < resumeAt) {
            handler.postDelayed(pump, resumeAt - now)
            return
        }

        while (true) {
            // 급한 말이 생기면 하던 수업은 잠깐 접어 둡니다.
            if (urgent.isNotEmpty() && current?.urgent != true) {
                current?.let { suspended = it }
                current = urgent.removeFirst().takeIf { now < it.expiresAt }
                continue
            }

            val sequence = current
            if (sequence != null) {
                // 벨트를 기다리는 동안에는 수업도 멈춥니다.
                if (!sequence.urgent && !mayTeach()) return
                val line = sequence.lines.removeFirstOrNull()
                if (line != null) {
                    sayLine(line, isStory = false)
                    return
                }
                val extra = sequence.onDone?.invoke().orEmpty()
                current = if (extra.isEmpty()) null else Sequence(sequence.key, extra, sequence.urgent)
                continue
            }

            if (suspended != null) {
                current = suspended
                suspended = null
                continue
            }
            break
        }

        if (phase == Phase.IDLE || phase == Phase.ENDED || phase == Phase.WAITING_BELT) return
        if (now < nextStoryAt) {
            handler.postDelayed(pump, nextStoryAt - now)
            return
        }

        when (phase) {
            Phase.TELLING -> advanceScript(now)
            Phase.HOLDING -> {
                // 본문이 끝나고 도착을 기다리는 동안에는 놀이와 영어를 번갈아 합니다.
                val lesson = if (holdTurn++ % 2 == 0) coach.nextLesson() else null
                if (lesson != null) startLesson(lesson, now) else say(nextInterlude(), isStory = true)
                nextStoryAt = max(nextStoryAt, now + INTERLUDE_GAP_MS)
            }
            Phase.ENDING -> when {
                endingCursor < ending.size -> say(ending[endingCursor++], isStory = true)
                !recapDone -> {
                    recapDone = true
                    val recap = coach.recap() + treatsAtArrival()
                    if (recap.isEmpty()) {
                        setPhase(Phase.ENDED)
                    } else {
                        recap.forEach { prefetch(it.text) }
                        current = Sequence(KEY_LESSON, recap, urgent = false)
                        pumpNow()
                    }
                }
                else -> setPhase(Phase.ENDED)
            }
            else -> Unit
        }
    }

    private fun mayTeach() = phase == Phase.TELLING || phase == Phase.HOLDING || phase == Phase.ENDING

    private fun advanceScript(now: Long) {
        while (cursor < script.size) {
            when (val beat = script[cursor++]) {
                is Beat.Chapter -> {
                    chapterTitle = beat.title
                    ui.onScene(chapterTitle, progress())
                }
                is Beat.Say -> {
                    if (!applies(beat)) continue
                    ui.onScene(chapterTitle, progress())
                    sayLine(Line(beat.text, pauseAfterMs = beat.pauseAfterMs), isStory = true)
                    return
                }
                Beat.Gap -> {
                    // 장과 장 사이: 오늘 할 영어가 남아 있으면 한 가지 하고, 없으면 쉬어 갑니다.
                    val lesson = coach.nextLesson()
                    if (lesson != null) {
                        startLesson(lesson, now)
                    } else {
                        nextStoryAt = now + chapterGapMillis()
                        handler.postDelayed(pump, nextStoryAt - now)
                    }
                    return
                }
                Beat.BeltGate -> if (needsBelt()) {
                    pauseForBelt(Reactions.BELT_ASK)
                    pumpNow()
                    return
                }
            }
        }
        // 본문은 끝났지만 아직 도착 전이면 놀이로 시간을 채웁니다.
        setPhase(Phase.HOLDING)
        nextStoryAt = now + INTERLUDE_FIRST_GAP_MS
        handler.postDelayed(pump, INTERLUDE_FIRST_GAP_MS)
    }

    private fun startLesson(lesson: EnglishCoach.Lesson, now: Long) {
        lesson.lines.forEach { prefetch(it.text) }
        resumeAt = now + BEFORE_LESSON_MS
        current = Sequence(KEY_LESSON, lesson.lines, urgent = false) {
            nextStoryAt = max(nextStoryAt, SystemClock.elapsedRealtime() + AFTER_LESSON_MS)
            finishLesson(lesson.word, passed = passed[lesson.word.id] ?: true)
        }
        pumpNow()
    }

    private fun say(template: String, isStory: Boolean) = sayLine(Line(template), isStory)

    private fun sayLine(line: Line, isStory: Boolean) {
        val text = render(line.text)
        speaking = true
        lastCaption = text to (line.lang == Lang.EN)
        Log.d(TAG, "say(${if (isStory) "story" else if (line.lang == Lang.EN) "english" else "aside"}): $text")
        ui.onCaption(text, line.lang == Lang.EN)
        if (isStory) prefetchUpcoming()
        narrator.speak(text, line.lang) {
            val word = line.listenFor
            if (word != null) {
                listenFor(word, line.pauseAfterMs)
                return@speak
            }
            speaking = false
            val now = SystemClock.elapsedRealtime()
            resumeAt = now + max(line.pauseAfterMs, MIN_PAUSE_MS)
            nextStoryAt = max(nextStoryAt, now + if (isStory) LINE_GAP_MS else REACTION_GAP_MS)
            pumpNow()
        }
    }

    /**
     * 아이가 [word]를 말하는지 듣습니다. 들을 수 없는 차에서는 [pauseMs]만큼 기다린 뒤 통과로 봅니다.
     * 결과에 따라 칭찬이나 정답([EnglishCoach.feedback])을 지금 말하던 묶음의 맨 앞에 끼워 넣습니다.
     */
    private fun listenFor(word: Word, pauseMs: Long) {
        listening = true
        ui.onListening(true)
        val tries = (attempts[word.id] ?: 0) + 1
        attempts[word.id] = tries
        val handle: (AnswerListener.Result) -> Unit = { result ->
            listening = false
            ui.onListening(false)
            val retry = !result.passed && tries == 1 && listener.available && isFirstMeeting(word)
            if (result.passed) passed[word.id] = true else if (!retry) passed.putIfAbsent(word.id, false)
            Log.d(TAG, "listen: ${word.english} passed=${result.passed} heard=${result.heard} retry=$retry")
            lastCaption?.let { (text, english) -> ui.onCaption(text, english) }
            // 다시 해 볼 차례가 아니면 결과를 적어, 아이 실력에 맞게 오늘의 양을 조절합니다(차가 들을 수 있을 때만).
            val pace = if (retry) emptyList() else coach.record(word, result.passed, measured = listener.available)
            pace.forEach { prefetch(it.text) }
            val feedback = coach.feedback(word, result.passed, result.heard, retry) + pace
            current?.let { seq -> feedback.asReversed().forEach { seq.lines.addFirst(it) } }
            speaking = false
            resumeAt = SystemClock.elapsedRealtime() + MIN_PAUSE_MS
            pumpNow()
        }
        if (!listener.available) {
            handler.postDelayed({ handle(AnswerListener.Result(passed = true, heard = null)) }, max(pauseMs, MIN_PAUSE_MS))
            return
        }
        pendingAnswer = handle
        listener.listen(word, LISTEN_MS) { result ->
            pendingAnswer = null
            handle(result)
        }
    }

    /** 어른이 화면에서 "잘 말했어요"를 눌렀을 때(정차 중). 차가 못 들었어도 통과로 칩니다. */
    fun confirmAnswerByAdult() {
        val handle = pendingAnswer ?: return
        pendingAnswer = null
        listener.cancel()
        handle(AnswerListener.Result(passed = true, heard = null))
    }

    private var pendingAnswer: ((AnswerListener.Result) -> Unit)? = null

    /** 오늘 처음 배우는 말인지(복습이 아닌지). 처음 배우는 말은 한 번 더 따라 하게 합니다. */
    private fun isFirstMeeting(word: Word): Boolean = coach.stageOf(word) == 0

    private fun applies(beat: Beat.Say) = !beat.needsName || childName.isSet

    private fun nextInterlude(): String {
        repeat(interludes.size) {
            val beat = interludes[interludeCursor++ % interludes.size]
            if (applies(beat)) return beat.text
        }
        return interludes.first().text
    }

    /** 곧 읽을 본문을 미리 받아 둡니다. 차량 기본 음성만 쓸 때는 아무 일도 하지 않습니다. */
    private fun prefetchUpcoming() {
        when (phase) {
            Phase.TELLING -> script.drop(cursor).filterIsInstance<Beat.Say>().filter(::applies)
                .take(PREFETCH_AHEAD).forEach { prefetch(it.text) }
            Phase.ENDING -> ending.drop(endingCursor).forEach { prefetch(it) }
            else -> Unit
        }
    }

    /** 언제 필요할지 모르는 마무리·반응·수업 대사를 시작할 때 받아 둡니다. */
    private fun warmUp() {
        // 방향지시등을 켠 순간 바로 말하려면 그 한마디가 미리 준비돼 있어야 하므로 가장 먼저 받습니다.
        listOf(Cue.LEFT, Cue.RIGHT).mapNotNull(coach::cueWord).forEach { word ->
            prefetch(cueShout(word.korean))
            prefetch(word.english)
        }
        (Reactions.calm.flatten() + ending + Reactions.warmUp + coach.commonLines()).forEach { prefetch(it) }
    }

    private fun prefetch(template: String) = narrator.prefetch(render(template))

    /** 대본의 {이름}·{멍} 표시를 아이 이름과 강아지 이름으로 바꿉니다. */
    private fun render(template: String) = childName.render(pet.name.render(template))

    /**
     * 영어 한 가지를 끝냈을 때: 진도를 올리고, 오늘 처음 만난 말이면 간식을 하나 줍니다.
     * 덧붙여 말할 것(꿈 구슬, 레벨 업, 간식)을 돌려줍니다.
     */
    private fun finishLesson(word: Word, quiet: Boolean = false, passed: Boolean = true): List<Line> {
        val counts = coach.willCount(word)
        val extra = coach.finishWord(word)
        if (!counts) return extra
        // 듣기에서 통과하지 못했으면 진도는 이어 가고 간식은 반 조각입니다(마이크가 아이 소리를 못 듣는 차에서도 자라도록).
        if (!passed) {
            val completed = pet.earnHalf()
            return if (quiet) extra else extra + Line(if (completed) Reactions.TREAT_HALF_COMPLETED else Reactions.TREAT_HALF)
        }
        pet.earn(if (extra.isNotEmpty()) TREATS_FOR_MASTERY else 1)
        if (quiet) return extra
        val index = variantIndex[KEY_TREAT] ?: 0
        variantIndex[KEY_TREAT] = index + 1
        return extra + Line(Reactions.treatEarned[index % Reactions.treatEarned.size])
    }

    private fun treatsAtArrival(): List<Line> =
        if (pet.treats > 0) listOf(Line(Reactions.TREATS_AT_ARRIVAL.replace("{개수}", pet.treats.toString()))) else emptyList()

    // endregion

    // region 차량 신호 반응

    /**
     * 차량 신호에 반응합니다. 읽던 문장이 끝난 뒤에 이야기 속 반응 대사를 말하고,
     * 그 움직임의 영어([cue])가 오늘 남아 있으면 이어서 가르칩니다.
     * 같은 반응은 [cooldownMs] 안에 다시 하지 않습니다.
     *
     * [shoutNow]이면(방향지시등) 기다리지 않고 그 순간에 바로 외칩니다("왼쪽! Left!").
     * 바로 외쳤으면 긴 대사는 하지 않고, 바로 외칠 수 없을 때만 위의 방식으로 반응합니다.
     *
     * 아무것도 하지 않았으면 false를 돌려줍니다.
     */
    private fun react(
        key: String,
        cooldownMs: Long,
        variants: List<String>,
        cue: Cue?,
        shoutNow: Boolean = false,
    ): Boolean {
        if (phase != Phase.TELLING && phase != Phase.HOLDING) return false
        if (shoutNow && cue != null && shout(cue)) return true

        val now = SystemClock.elapsedRealtime()
        val last = lastFiredAt[key]
        if (last != null && now - last < cooldownMs) return false
        lastFiredAt[key] = now

        val index = variantIndex[key] ?: 0
        variantIndex[key] = index + 1
        enqueue(aside(key, variants[index % variants.size], cue, expiresAt = now + REACTION_FRESH_MS))
        return true
    }

    /**
     * 차가 움직인 바로 그 순간에 그 움직임을 우리말과 영어로 외칩니다.
     * 지금 바로 말할 수 없으면(음성이 준비되지 않음, 영어 수업 꺼짐 등) false를 돌려줍니다.
     */
    private fun shout(cue: Cue): Boolean {
        val word = coach.cueWord(cue) ?: return false
        val now = SystemClock.elapsedRealtime()
        val last = lastShoutAt[cue]
        if (last != null && now - last < SHOUT_DEBOUNCE_MS) return true

        val clips = listOf(render(cueShout(word.korean)) to Lang.KO, word.english to Lang.EN)
        val started = narrator.interject(clips) {
            // 끼어든 말이 끝나면 읽던 문장의 말풍선으로 되돌립니다.
            lastCaption?.let { (text, english) -> ui.onCaption(text, english) }
            // 바로 외친 말은 짧게 끝내야 하므로 간식 알림은 생략합니다(간식은 그대로 생깁니다).
            val extra = finishLesson(word, quiet = true)
            if (extra.isNotEmpty()) enqueue(Sequence(KEY_LESSON, extra, urgent = true))
        }
        if (!started) return false

        lastShoutAt[cue] = now
        Log.d(TAG, "shout: ${word.korean}! ${word.english}")
        ui.onCaption(word.english, true)
        return true
    }

    private fun cueShout(korean: String) = "$korean!"

    /** 한마디([text])와, 그 움직임([cue])에 해당하는 영어가 오늘 남아 있으면 그 수업까지 묶습니다. */
    private fun aside(key: String, text: String, cue: Cue?, expiresAt: Long = Long.MAX_VALUE): Sequence {
        val lesson = cue?.let(coach::cueLesson)
        val lines = listOf(Line(text)) + lesson?.lines.orEmpty()
        lesson?.lines?.forEach { prefetch(it.text) }
        return Sequence(key, lines, urgent = true, expiresAt = expiresAt) {
            lesson?.let { finishLesson(it.word, passed = passed[it.word.id] ?: true) }.orEmpty()
        }
    }

    private fun enqueue(sequence: Sequence) {
        urgent.removeAll { it.key == sequence.key }
        while (urgent.size >= MAX_PENDING_REACTIONS) urgent.removeFirst()
        urgent.addLast(sequence)
        pumpNow()
    }

    // endregion

    /**
     * 장 사이 쉼의 길이. 내비의 남은 시간을 알면, 이야기가 도착 즈음에 끝나도록 쉼을 늘립니다.
     */
    private fun chapterGapMillis(): Long {
        val remainingMs = (currentRemainingSec() ?: return DEFAULT_CHAPTER_GAP_MS) * 1000L
        val rest = script.drop(cursor)
        val speechMs = rest.filterIsInstance<Beat.Say>().sumOf { Narrator.estimateMillis(it.text) + LINE_GAP_MS }
        val gapsLeft = rest.count { it is Beat.Gap } + 1
        return ((remainingMs - speechMs - ENDING_RESERVE_MS) / gapsLeft)
            .coerceIn(MIN_CHAPTER_GAP_MS, MAX_CHAPTER_GAP_MS)
    }

    private fun currentRemainingSec(): Int? {
        val base = navi.remainingSec ?: return null
        val elapsed = ((SystemClock.elapsedRealtime() - tripUpdatedAt) / 1000L).toInt()
        return max(0, base - elapsed)
    }

    private fun howFarAnswer(): String {
        val seconds = currentRemainingSec()
        if (seconds != null) {
            val minutes = max(1, (seconds / 60f).roundToInt())
            return when {
                minutes <= 2 -> "{이름아}, 거의 다 왔어! 달빛 섬이 바로 코앞이야. 숫자를 천천히 백까지 세면 도착할 거야."
                minutes <= 30 -> "{이름아}, 달빛 섬까지 ${minutes}분쯤 남았어. 노래 ${koreanCount(max(1, minutes / 3))} 곡만 부르면 도착이야."
                else -> "아직 ${minutes}분쯤 더 날아가야 해. 그래도 걱정 마, 가는 길에 재미있는 일이 많이 남았거든."
            }
        }
        // 경로 안내가 없으면 이야기 진행도로 대답합니다.
        return when {
            phase == Phase.IDLE -> "아직 출발 전이야. 이야기를 시작하면 {멍이} 달빛 섬까지 데려다줄게."
            phase == Phase.ENDING || phase == Phase.ENDED -> "다 왔어! 여기가 달빛 섬이야."
            progress() < 0.35f -> "이제 막 날아올랐어. 별 물고기도 만나고 구름 터널도 지나야 해. 조금만 기다려 줘."
            progress() < 0.75f -> "{이름아}, 반쯤 왔어! 바람 언덕만 넘으면 달빛 섬이 보일 거야."
            else -> "거의 다 왔어. 저기 은빛으로 반짝이는 게 달빛 섬이야."
        }
    }

    private fun koreanCount(n: Int): String = when (n) {
        1 -> "한"; 2 -> "두"; 3 -> "세"; 4 -> "네"; 5 -> "다섯"
        6 -> "여섯"; 7 -> "일곱"; 8 -> "여덟"; 9 -> "아홉"; 10 -> "열"
        else -> n.toString()
    }

    private fun progress(): Float = (cursor.toFloat() / script.size).coerceIn(0f, 1f)

    private fun setPhase(next: Phase) {
        if (phase == next) return
        Log.d(TAG, "phase: $phase -> $next")
        phase = next
        ui.onPhase(next)
    }

    private fun reset() {
        handler.removeCallbacks(calmTimeout)
        calming = false
        urgent.clear()
        current = null
        suspended = null
        lastFiredAt.clear()
        lastShoutAt.clear()
        lastCaption = null
        speaking = false
        listening = false
        pendingAnswer = null
        attempts.clear()
        passed.clear()
        lastNaviReactionAt = 0L
        reactedTurn = null
        reactedLimit = null
        reactedCamera = null
        cursor = 0
        endingCursor = 0
        interludeCursor = 0
        holdTurn = 0
        recapDone = false
        resumeAt = 0L
        nextStoryAt = 0L
        chapterTitle = ""
        beltConfirmedByAdult = false
        leftParkDuringStory = false
        announcedStop = false
        doorLineSaid = false
    }

    private companion object {
        const val TAG = "StoryEngine"
        const val KEY_TURN = "turn"
        const val KEY_RAIN = "rain"
        const val KEY_RAIN_END = "rainEnd"
        const val KEY_STOP = "stop"
        const val KEY_GO = "go"
        const val KEY_REVERSE = "reverse"
        const val KEY_BELT = "belt"
        const val KEY_DOOR = "door"
        const val KEY_HOW_FAR = "howFar"
        const val KEY_LESSON = "lesson"
        const val KEY_NAVI = "navi"
        const val KEY_NAVI_TURN = "naviTurn"
        const val KEY_NAVI_SPEED = "naviSpeed"
        const val KEY_NAVI_CAMERA = "naviCamera"
        const val KEY_NAVI_REMAINING = "naviRemaining"
        const val KEY_TREAT = "treat"
        const val KEY_CALM = "calm"

        /** 비밀 버튼을 연달아 눌러도 이 시간 안에는 다시 말하지 않습니다. */
        const val CALM_DEBOUNCE_MS = 6_000L
        const val CALM_TIMEOUT_MARGIN_MS = 4_000L

        /** 한 말을 완전히 익혀 꿈 구슬이 되면 간식을 이만큼 줍니다. */
        const val TREATS_FOR_MASTERY = 3

        // 같은 반응을 너무 자주 하지 않도록 두는 쉬는 시간
        const val COOLDOWN_TURN = 25_000L
        const val COOLDOWN_RAIN = 60_000L
        const val COOLDOWN_STOP = 90_000L
        const val COOLDOWN_REVERSE = 60_000L

        /** 브레이크를 이만큼 계속 밟고 있으면 '신호 대기 중'으로 봅니다. */
        const val STOP_HOLD_MS = 4_000L

        /** 차량 신호 반응은 이 시간 안에 말하지 못하면 때를 놓친 것으로 보고 건너뜁니다. */
        const val REACTION_FRESH_MS = 12_000L

        /** 같은 움직임의 한마디를 이 시간 안에 또 외치지 않습니다(깜빡이 신호가 연달아 들어올 때). */
        const val SHOUT_DEBOUNCE_MS = 4_000L

        const val MIN_PAUSE_MS = 250L

        /** 아이가 말할 차례에 듣는 시간. */
        const val LISTEN_MS = 4_500L

        // 내비 안내 뒤 이야기 속 반응: 40초에 한 번까지, 20초 안에 말하지 못하면 건너뜀. 다음 안내가 400m 안이면 보조로.
        const val NAVI_REACTION_COOLDOWN_MS = 40_000L
        const val NAVI_FRESH_MS = 20_000L
        const val NAVI_TURN_NEAR_METERS = 400
        const val CAMERA_WARN_METERS = 700

        const val LINE_GAP_MS = 900L
        const val REACTION_GAP_MS = 1_500L
        const val BEFORE_LESSON_MS = 1_500L
        const val AFTER_LESSON_MS = 3_000L
        const val DEFAULT_CHAPTER_GAP_MS = 6_000L
        const val MIN_CHAPTER_GAP_MS = 4_000L
        const val MAX_CHAPTER_GAP_MS = 120_000L
        const val ENDING_RESERVE_MS = 20_000L
        const val INTERLUDE_FIRST_GAP_MS = 20_000L
        const val INTERLUDE_GAP_MS = 45_000L
        const val MAX_PENDING_REACTIONS = 2
        const val PREFETCH_AHEAD = 2
    }
}

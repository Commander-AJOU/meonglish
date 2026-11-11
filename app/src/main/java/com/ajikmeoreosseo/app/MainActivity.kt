package com.ajikmeoreosseo.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.InputFilter
import android.text.InputType
import android.util.TypedValue
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.ajikmeoreosseo.app.car.CarEvent
import com.ajikmeoreosseo.app.car.NaviSnapshot
import com.ajikmeoreosseo.app.car.CarSignals
import com.ajikmeoreosseo.app.car.TripInfo
import com.ajikmeoreosseo.app.english.AnswerListener
import com.ajikmeoreosseo.app.english.EnglishCoach
import com.ajikmeoreosseo.app.english.Situation
import com.ajikmeoreosseo.app.pet.House
import com.ajikmeoreosseo.app.pet.Pet
import com.ajikmeoreosseo.app.english.Word
import com.ajikmeoreosseo.app.story.ChildName
import com.ajikmeoreosseo.app.story.Lang
import com.ajikmeoreosseo.app.story.Narrator
import com.ajikmeoreosseo.app.story.Reactions
import com.ajikmeoreosseo.app.story.Story
import com.ajikmeoreosseo.app.story.StoryEngine
import com.ajikmeoreosseo.app.story.StoryGenerator
import com.ajikmeoreosseo.app.story.StoryPack
import com.ajikmeoreosseo.app.story.TodayNote
import com.ajikmeoreosseo.app.story.StoryEngine.Phase
import com.ajikmeoreosseo.app.story.Tone
import com.ajikmeoreosseo.app.ui.Clay
import com.ajikmeoreosseo.app.ui.HouseView
import com.ajikmeoreosseo.app.ui.IrisView
import com.ajikmeoreosseo.app.ui.SceneView
import com.ajikmeoreosseo.app.ui.WordBookDialog

/**
 * 화면은 셋입니다.
 *  - 홈: 구름 강아지의 인사, 강아지 카드(단계·성장 막대), 간식 주기, 큰 "이야기 시작" 버튼
 *  - 이야기 속: 장 제목, 말풍선, "아직 멀었어?" 버튼. 동그란 장면 전환으로 들어가고 나옵니다.
 *  - 강아지 집: 단계가 오르며 열린 물건을 아이가 놓고 치우는 꾸미기. 차를 세웠을 때만 열립니다.
 *  - 꿈 구슬 도감: 아이용. 배운 말이 카드로 모이고, 누르면 강아지가 읽어 줍니다. 차를 세웠을 때만 열립니다.
 *  - 설정: 어른용. 주인공 이름, 강아지 이름, 영어, 목소리. 차를 세웠을 때만 열립니다.
 */
class MainActivity : Activity(), StoryEngine.Ui {

    private enum class Screen { HOME, STORY }

    private lateinit var sky: SceneView
    private lateinit var iris: IrisView
    private lateinit var home: View
    private lateinit var story: View
    private lateinit var settings: View
    private lateinit var houseScreen: View
    private lateinit var houseView: HouseView
    private lateinit var houseTray: LinearLayout
    private lateinit var houseTitle: TextView
    private lateinit var houseButton: View
    private lateinit var houseLabel: TextView
    private lateinit var houseBadge: View
    private lateinit var wordbookScreen: View
    private lateinit var wordbookTitle: TextView
    private lateinit var wordbookSummary: TextView
    private lateinit var wordbookHint: TextView
    private lateinit var wordGrid: LinearLayout

    private lateinit var greeting: TextView
    private lateinit var rewardText: TextView
    private lateinit var startLabel: TextView
    private lateinit var settingsButton: View
    private lateinit var petName: TextView
    private lateinit var petLevel: TextView
    private lateinit var petBar: View
    private lateinit var petNext: TextView
    private lateinit var feedHint: TextView
    private lateinit var feedButton: View
    private lateinit var feedLabel: TextView
    private lateinit var feedCount: TextView

    private lateinit var chapter: TextView
    private lateinit var caption: TextView
    private lateinit var beltCard: View
    private lateinit var beltHint: TextView
    private lateinit var listeningTag: View

    private lateinit var settingsNameValue: TextView
    private lateinit var settingsPetNameValue: TextView
    private lateinit var settingsEnglishValue: TextView
    private lateinit var settingsVoiceValue: TextView
    private lateinit var settingsStoryValue: TextView
    private lateinit var settingsNoteValue: TextView
    private lateinit var storyTag: TextView

    private lateinit var narrator: Narrator
    private lateinit var coach: EnglishCoach
    private lateinit var pet: Pet
    private lateinit var house: House
    private lateinit var engine: StoryEngine
    private lateinit var stories: StoryGenerator
    private lateinit var note: TodayNote
    private lateinit var car: CarSignals
    private lateinit var trip: TripInfo
    private lateinit var answers: AnswerListener

    private val main = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("settings", Context.MODE_PRIVATE) }
    private val point = FloatArray(2)
    private val location = IntArray(2)
    private val skyLocation = IntArray(2)

    private var screen = Screen.HOME
    private var transitioning = false
    private var heardStory = false
    private var driving = false
    private var raining = false
    private var voiceStatus = ""
    private var lastCaption: String? = null
    private var lastCaptionEnglish = false
    private var feedingUntil = 0L
    private var yumIndex = 0
    private var grewJustNow = false
    private var newItemJustNow = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sky = findViewById(R.id.sky)
        iris = findViewById(R.id.iris)
        home = findViewById(R.id.home)
        story = findViewById(R.id.story)
        settings = findViewById(R.id.settings)
        houseScreen = findViewById(R.id.house)
        houseView = findViewById(R.id.houseView)
        houseTray = findViewById(R.id.houseTray)
        houseTitle = findViewById(R.id.houseTitle)
        houseButton = findViewById(R.id.houseButton)
        houseLabel = findViewById(R.id.houseLabel)
        houseBadge = findViewById(R.id.houseBadge)
        wordbookScreen = findViewById(R.id.wordbook)
        wordbookTitle = findViewById(R.id.wordbookTitle)
        wordbookSummary = findViewById(R.id.wordbookSummary)
        wordbookHint = findViewById(R.id.wordbookHint)
        wordGrid = findViewById(R.id.wordGrid)
        greeting = findViewById(R.id.greeting)
        rewardText = findViewById(R.id.rewardText)
        startLabel = findViewById(R.id.startLabel)
        settingsButton = findViewById(R.id.settingsButton)
        petName = findViewById(R.id.petName)
        petLevel = findViewById(R.id.petLevel)
        petBar = findViewById(R.id.petBar)
        petNext = findViewById(R.id.petNext)
        feedHint = findViewById(R.id.feedHint)
        feedButton = findViewById(R.id.feedButton)
        feedLabel = findViewById(R.id.feedLabel)
        feedCount = findViewById(R.id.feedCount)
        chapter = findViewById(R.id.chapter)
        caption = findViewById(R.id.caption)
        beltCard = findViewById(R.id.beltCard)
        beltHint = findViewById(R.id.beltHint)
        listeningTag = findViewById(R.id.listeningTag)
        settingsNameValue = findViewById(R.id.settingsNameValue)
        settingsPetNameValue = findViewById(R.id.settingsPetNameValue)
        settingsEnglishValue = findViewById(R.id.settingsEnglishValue)
        settingsVoiceValue = findViewById(R.id.settingsVoiceValue)
        settingsStoryValue = findViewById(R.id.settingsStoryValue)
        settingsNoteValue = findViewById(R.id.settingsNoteValue)
        storyTag = findViewById(R.id.storyTag)

        narrator = Narrator(this)
        coach = EnglishCoach(this)
        pet = Pet(this)
        house = House(this, pet)
        answers = AnswerListener(this)
        engine = StoryEngine(narrator, coach, pet, answers, this)
        stories = StoryGenerator(this)
        note = TodayNote(this)
        stories.todayNote = { note.text }
        engine.packProvider = { todayPack() }
        engine.childName = ChildName(prefs.getString(KEY_CHILD_NAME, null))
        car = CarSignals(this, ::onCarEvent)
        trip = TripInfo(this, ::onNavi, engine::onTripArrived)
        // 새로 배울 낱말은 지금 상황(비, 시간대, 계절, 가는 곳, 오늘 이야기)에 맞는 것부터 고릅니다.
        coach.situation = {
            Situation(
                raining = raining,
                hour = java.time.LocalTime.now().hour,
                month = java.time.LocalDate.now().monthValue,
                theme = StoryGenerator.themeOf(java.time.LocalDate.now()),
                destination = trip.snapshot.destinationName.orEmpty(),
            )
        }

        Clay.pressable(findViewById(R.id.startButton), ::enterStory)
        Clay.pressable(settingsButton, ::openSettings)
        Clay.pressable(feedButton) { feed() }
        Clay.pressable(houseButton, ::openHouse)
        Clay.pressable(findViewById(R.id.houseBack), ::closeHouse)
        Clay.pressable(findViewById(R.id.reward), ::openWordBook)
        Clay.pressable(findViewById(R.id.wordbookBack), ::closeWordBook)
        Clay.pressable(findViewById(R.id.stopButton)) { engine.stop() }
        Clay.pressable(findViewById(R.id.howFarButton)) { engine.askHowFar() }
        Clay.pressable(findViewById(R.id.beltButton)) { engine.confirmBeltByAdult() }
        Clay.pressable(findViewById(R.id.listenConfirm)) { engine.confirmAnswerByAdult() }
        Clay.pressable(findViewById(R.id.settingsBack), ::closeSettings)
        Clay.pressable(findViewById(R.id.settingsNameButton), ::showNameDialog)
        Clay.pressable(findViewById(R.id.settingsPetNameButton), ::showPetNameDialog)
        Clay.pressable(findViewById(R.id.settingsEnglishButton)) { WordBookDialog.show(this, coach) }
        Clay.pressable(findViewById(R.id.settingsStoryButton)) { prepareStory(force = true) }
        Clay.pressable(findViewById(R.id.settingsNoteButton), ::showNoteDialog)
        // 부모의 비밀 버튼: 평범한 장 제목처럼 보이지만 길게 누르면 강아지가 우는 아이를 달랩니다.
        chapter.setOnLongClickListener {
            engine.calmChild()
            true
        }

        coach.onChanged = {
            renderHome()
            renderSettings()
        }
        pet.onChanged = {
            renderHome()
            renderSettings()
        }
        house.onChanged = { renderHome() }
        houseView.bind(house)
        buildTray()
        narrator.onStatus = {
            voiceStatus = it
            renderSettings()
        }
        stories.onChanged = {
            renderHome()
            renderSettings()
        }

        coach.setMinLevel(pet.englishFloor)
        sky.setStage(pet.stage)
        sky.setHome(true)
        renderHome()
        renderSettings()
        renderCaption()

        narrator.start()
        car.start()
        trip.start()
        answers.start()
        prepareStory()
        if (BuildConfig.DEBUG) {
            registerReceiver(debugNavi, android.content.IntentFilter(DEBUG_NAVI_ACTION), RECEIVER_EXPORTED)
            registerReceiver(debugFocus, android.content.IntentFilter(DEBUG_FOCUS_ACTION), RECEIVER_EXPORTED)
            registerReceiver(debugTools, android.content.IntentFilter(DEBUG_TOOLS_ACTION), RECEIVER_EXPORTED)
        }
    }

    /**
     * 시험용 도구. adb shell am broadcast -a com.ajikmeoreosseo.app.DEBUG_TOOLS --es do <명령>
     *   carVoice / cloudVoice  차량 기본 음성만 쓰기 / 되돌리기
     *   story                  오늘의 이야기 다시 쓰기
     *   note --es text ..      오늘 있었던 일 저장(비우면 지움)
     *   route --ef lat .. --ef lon .. --es name ..   내비에 실제 경로 요청,  cancelRoute  취소
     *   turn --es side left|right|off                  방향지시등 반응(촬영용)
     */
    private val debugTools = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: android.content.Intent) {
            when (intent.getStringExtra("do")) {
                "carVoice" -> narrator.forceCarVoice = true
                "cloudVoice" -> narrator.forceCarVoice = false
                "story" -> prepareStory(force = true)
                // 와이퍼 신호 흉내(비 올 때 고르는 낱말 확인용)
                "wiper" -> onCarEvent(CarEvent.Wiper(intent.getBooleanExtra("on", true)))
                "note" -> saveNote(intent.getStringExtra("text").orEmpty())
                "fixedStory" -> { forceFixedStory = true; renderHome() }
                "dailyStory" -> { forceFixedStory = false; renderHome() }
                "route" -> trip.requestRouteForDebug(
                    intent.getFloatExtra("lat", 37.4979f).toDouble(),
                    intent.getFloatExtra("lon", 127.0276f).toDouble(),
                    intent.getStringExtra("name") ?: "유치원",
                )
                "cancelRoute" -> trip.cancelRouteForDebug()
                // 시험용 차량 신호: --es pos P|D|R / --ez occupied true|false / --ez buckled true|false
                "gear" -> onCarEvent(
                    CarEvent.Gear(
                        when (intent.getStringExtra("pos")) {
                            "D" -> CarEvent.Gear.Position.DRIVE
                            "R" -> CarEvent.Gear.Position.REVERSE
                            else -> CarEvent.Gear.Position.PARK
                        },
                    ),
                )
                "seat" -> onCarEvent(CarEvent.RearSeat(intent.getBooleanExtra("occupied", true)))
                "belt" -> onCarEvent(CarEvent.RearBelt(intent.getBooleanExtra("buckled", true)))
                // 촬영용: 차량 신호 없이 방향지시등 반응만 일으킵니다(실제 신호는 플레오스의 측면 카메라 화면이 앱을 덮습니다).
                "turn" -> onCarEvent(
                    CarEvent.TurnSignal(
                        when (intent.getStringExtra("side")) {
                            "left" -> CarEvent.TurnSignal.Side.LEFT
                            "right" -> CarEvent.TurnSignal.Side.RIGHT
                            else -> CarEvent.TurnSignal.Side.OFF
                        },
                    ),
                )
            }
        }
    }

    /** 시연용: 내비 안내가 시작/종료된 것처럼 오디오 포커스를 흉내 냅니다. adb shell am broadcast -a com.ajikmeoreosseo.app.DEBUG_FOCUS --ez lost true */
    private val debugFocus = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: android.content.Intent) {
            // --ez forGood true: 음악을 튼 것처럼 포커스를 아주 잃음(다른 소리가 없으면 몇 초 뒤 스스로 이어 읽어야 함)
            if (intent.getBooleanExtra("forGood", false)) narrator.simulateFocusLostForGood()
            else narrator.simulateFocus(intent.getBooleanExtra("lost", true))
        }
    }

    /** 시연용: 내비 없이 값을 넣어 봅니다. adb shell am broadcast -a com.ajikmeoreosseo.app.DEBUG_NAVI --ei remaining 600 --ei distance 3400 --es name 유치원 --ei limit 50 --es turn 좌회전 --ei turnDistance 300 --ei cameraLimit 50 --ei cameraDistance 600 */
    private val debugNavi = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: android.content.Intent) {
            trip.inject(
                NaviSnapshot(
                    remainingSec = intent.getIntExtra("remaining", -1).takeIf { it > 0 },
                    remainingMeters = intent.getIntExtra("distance", -1).takeIf { it > 0 },
                    destinationName = intent.getStringExtra("name"),
                    speedLimit = intent.getIntExtra("limit", -1).takeIf { it > 0 },
                    nextTurn = intent.getStringExtra("turn"),
                    nextTurnMeters = intent.getIntExtra("turnDistance", -1).takeIf { it > 0 },
                    cameraSpeedLimit = intent.getIntExtra("cameraLimit", -1).takeIf { it > 0 },
                    cameraMeters = intent.getIntExtra("cameraDistance", -1).takeIf { it > 0 },
                    cameraId = intent.getStringExtra("cameraId") ?: intent.getIntExtra("cameraDistance", -1).takeIf { it > 0 }?.let { "debug-$it" },
                ),
            )
        }
    }

    /**
     * 내비 상태가 바뀔 때마다. 이야기 엔진에 전하고, 홈 화면에서 경로가 잡히면 남은 시간에 맞는 길이의 이야기를
     * 미리 써 둡니다("이야기 시작"을 눌렀을 때 기다리지 않도록).
     */
    private fun onNavi(snapshot: NaviSnapshot) {
        engine.onNavi(snapshot)
        if (screen != Screen.HOME || transitioning) return
        if (engine.phase != Phase.IDLE && engine.phase != Phase.ENDED) return
        val minutes = snapshot.remainingSec?.let { it / 60 } ?: return
        val wanted = StoryGenerator.sizeFor(minutes)
        if (!needsRefit(wanted) || wanted == requestedSize) return
        requestedSize = wanted
        stories.prepareToday(pet.tone, pet.name.given, coach.plannedWords(), size = wanted, force = true)
    }

    /** 오늘의 이야기가 [wanted] 크기와 많이 다른지(장 수가 둘 이상 차이). */
    private fun needsRefit(wanted: StoryGenerator.Size): Boolean {
        if (stories.state == StoryGenerator.State.NO_KEY) return false
        val have = stories.todaySize() ?: return true
        return kotlin.math.abs(have.chapters - wanted.chapters) >= 2
    }

    private var requestedSize: StoryGenerator.Size? = null

    /** 오늘 들려줄 이야기: 그날 만든 것이 있으면 그것, 없으면 기본 이야기. */
    private fun todayPack(): StoryPack =
        if (forceFixedStory) Story.pack(pet.tone) else stories.todayPack() ?: Story.pack(pet.tone)

    /** 시험·촬영용: 그날 만든 이야기 대신 기본 이야기를 씁니다. */
    private var forceFixedStory = false

    /** 오늘의 이야기가 없으면 만들어 둡니다. 이야기를 듣는 중에는 바꾸지 않습니다. */
    private fun prepareStory(force: Boolean = false) {
        if (engine.phase != Phase.IDLE && engine.phase != Phase.ENDED) return
        val size = StoryGenerator.sizeFor(trip.snapshot.remainingSec?.let { it / 60 })
        stories.prepareToday(pet.tone, pet.name.given, coach.plannedWords(), size = size, force = force)
    }

    override fun onDestroy() {
        main.removeCallbacksAndMessages(null)
        StoryService.stop(this)
        if (BuildConfig.DEBUG) runCatching { unregisterReceiver(debugNavi); unregisterReceiver(debugFocus); unregisterReceiver(debugTools) }
        engine.stop()
        car.release()
        trip.release()
        answers.release()
        narrator.release()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            settings.visibility == View.VISIBLE -> closeSettings()
            houseScreen.visibility == View.VISIBLE -> closeHouse()
            wordbookScreen.visibility == View.VISIBLE -> closeWordBook()
            else -> super.onBackPressed()
        }
    }

    /** 대본의 {이름}·{멍} 표시를 아이 이름과 강아지 이름으로 바꿉니다. */
    private fun render(template: String): String = engine.childName.render(pet.name.render(template))

    // region 홈 ↔ 이야기 속

    /** 화면이 강아지를 향해 동그랗게 닫히고, 제목이 뜬 뒤, 이야기 속에서 다시 열립니다. */
    private fun enterStory() {
        if (transitioning || screen != Screen.HOME) return
        transitioning = true
        sky.puppyCenter(point)

        // 목적지까지 남은 시간에 맞는 길이의 이야기를 씁니다. 보통은 경로가 잡힐 때 미리 써 두므로([onNavi]) 기다리지 않고,
        // 아직 쓰는 중이거나 길이가 많이 다를 때만 제목 카드를 띄운 채 잠깐 기다립니다.
        val remainingMinutes = trip.snapshot.remainingSec?.let { it / 60 }
        val wanted = StoryGenerator.sizeFor(remainingMinutes)
        val writing = stories.state == StoryGenerator.State.WORKING
        val refit = writing || (remainingMinutes != null && needsRefit(wanted))

        val titleCard = { IrisView.Card(getString(R.string.intro_kicker), render(todayPack().title), render(getString(R.string.intro_with))) }
        val card = if (refit) {
            IrisView.Card(getString(R.string.intro_kicker), getString(R.string.intro_writing), getString(R.string.intro_writing_sub))
        } else {
            titleCard()
        }
        val gate = if (!refit) null else IrisView.Gate { open ->
            var opened = false
            val openOnce = {
                if (!opened && !isFinishing && !isDestroyed) {
                    opened = true
                    iris.updateCard(titleCard())
                    main.postDelayed(open, 900L)
                }
            }
            requestedSize = wanted
            stories.prepareToday(pet.tone, pet.name.given, coach.plannedWords(), size = wanted, force = !writing) { openOnce() }
            // 너무 오래 걸리면 있는 이야기로 시작합니다.
            main.postDelayed(openOnce, REFIT_TIMEOUT_MS)
        }
        iris.play(
            point[0], point[1], card,
            onCovered = { show(Screen.STORY) },
            // 전환 중에 앱을 닫았으면(뒤로 가기) 이야기를 시작하지 않습니다.
            onOpening = { if (!isFinishing && !isDestroyed) engine.start() },
            onDone = { transitioning = false },
            gate = gate,
        )
    }

    private fun leaveStory() {
        if (transitioning || screen != Screen.STORY || isFinishing || isDestroyed) return
        transitioning = true
        sky.puppyCenter(point)
        iris.play(
            point[0], point[1], card = null,
            onCovered = { show(Screen.HOME) },
            onOpening = { },
            onDone = { transitioning = false },
        )
    }

    /** 장면 전환이 화면을 가린 사이에 뒤 화면을 바꿉니다. */
    private fun show(next: Screen) {
        screen = next
        home.visibility = if (next == Screen.HOME) View.VISIBLE else View.GONE
        story.visibility = if (next == Screen.STORY) View.VISIBLE else View.GONE
        sky.setHome(next == Screen.HOME)
        if (next == Screen.HOME) {
            sky.setProgress(0f)
            renderHome()
        } else {
            lastCaption = null
            chapter.visibility = View.INVISIBLE
            renderCaption()
        }
        // 크기가 바뀐 뒤의 자리로 전환 중심을 옮깁니다.
        sky.post {
            sky.puppyCenter(point)
            iris.moveCenter(point[0], point[1])
        }
    }

    private fun renderHome() {
        val message = when {
            grewJustNow -> R.string.greeting_grown
            newItemJustNow -> R.string.greeting_new_item
            pet.treats > 0 -> R.string.greeting_treats
            heardStory -> R.string.greeting_after
            pet.tone == Tone.BABY -> R.string.greeting_baby
            pet.tone == Tone.GROWN -> R.string.greeting_adult
            else -> R.string.greeting
        }
        greeting.text = keepWordsTogether(render(getString(message, pet.treats)))
        startLabel.setText(if (heardStory) R.string.restart_story else R.string.start_story)

        val summary = coach.summary()
        rewardText.text =
            if (summary.enabled) getString(R.string.reward, summary.mastered.size, summary.level)
            else getString(R.string.reward_off, summary.mastered.size)

        petName.text = pet.name.given
        petLevel.text = getString(R.string.pet_level, pet.level, pet.stageName)
        petNext.text =
            if (pet.isGrown) getString(R.string.pet_grown) else getString(R.string.pet_next, pet.treatsToNextStage)
        petBar.post {
            val track = petBar.parent as View
            val width = (track.width * pet.stageProgress).toInt().coerceAtLeast(if (pet.stageProgress > 0f) 24 else 0)
            if (petBar.layoutParams.width != width) {
                petBar.layoutParams = petBar.layoutParams.apply { this.width = width }
            }
        }

        val canFeed = pet.treats > 0 && !driving
        feedCount.text = when {
            pet.treats > 0 && pet.hasHalfTreat -> getString(R.string.feed_count_half, pet.treats)
            pet.treats > 0 -> getString(R.string.feed_count, pet.treats)
            else -> ""
        }
        // 반 조각만 있을 때는 "간식이 없어요" 옆에 "반 조각"이 붙어 어색하므로 한마디로 보여 줍니다.
        feedLabel.setText(
            when {
                pet.treats > 0 -> R.string.feed
                pet.hasHalfTreat -> R.string.feed_half_only
                else -> R.string.feed_none
            },
        )
        feedButton.isEnabled = canFeed
        feedButton.alpha = if (canFeed) 1f else 0.45f
        feedHint.text = getString(
            when {
                pet.treats == 0 -> R.string.feed_hint
                driving -> R.string.feed_driving
                else -> R.string.feed_hint
            }
        )
        feedHint.visibility = if (canFeed) View.INVISIBLE else View.VISIBLE

        val pack = todayPack()
        storyTag.text = getString(R.string.home_story_today, render(pack.title))
        storyTag.visibility = if (pack.generated) View.VISIBLE else View.GONE

        houseLabel.text = render(getString(R.string.house_button))
        houseBadge.visibility = if (house.newItems.isNotEmpty()) View.VISIBLE else View.GONE
        houseButton.isEnabled = !driving
        houseButton.alpha = if (driving) 0.35f else 1f
    }

    /**
     * 아이가 뒷자리에서 "Here you are!"라고 말해 간식을 줬을 때(도착해서 차를 세운 뒤).
     * 모은 간식을 하나씩 차례로 먹입니다. 강아지 대사는 이야기 엔진이 하므로 여기서는 말하지 않습니다.
     */
    override fun onFeedByVoice() {
        fun next() {
            if (isFinishing || isDestroyed || driving || pet.treats <= 0) return
            feed(byVoice = true)
            main.postAtTime(::next, VOICE_FEED, SystemClock.uptimeMillis() + FEED_LOCK_MS)
        }
        main.removeCallbacksAndMessages(VOICE_FEED)
        next()
    }

    /** 간식 하나가 강아지에게 날아가고, 강아지가 기뻐하며 한 단계 자랍니다. 모습이 바뀌면 빛이 번쩍입니다. */
    private fun feed(byVoice: Boolean = false) {
        if (driving || transitioning) return
        if (!byVoice && screen != Screen.HOME) return
        val now = SystemClock.elapsedRealtime()
        if (now < feedingUntil) return
        val levelBefore = pet.level
        val result = pet.feed() ?: return
        feedingUntil = now + FEED_LOCK_MS
        // 다 자란 뒤에는 먹어도 단계가 그대로이므로, 실제로 오른 단계에서만 새 물건을 찾습니다.
        val unlocked = house.unlockedBetween(levelBefore, result.level)
        grewJustNow = result.grewTo != null
        newItemJustNow = unlocked.isNotEmpty()
        renderHome()

        // 간식 버튼의 그림 자리에서 출발합니다(장면 뷰 안의 좌표로 바꿔서). 말로 줬으면 화면 아래 가운데(뒷자리 쪽)에서.
        val fromX: Float
        val fromY: Float
        if (byVoice || feedButton.visibility != View.VISIBLE || !feedButton.isShown) {
            fromX = sky.width * 0.5f
            fromY = sky.height * 0.95f
        } else {
            feedButton.getLocationInWindow(location)
            sky.getLocationInWindow(skyLocation)
            fromX = location[0] - skyLocation[0] + feedButton.width * 0.25f
            fromY = location[1] - skyLocation[1] + feedButton.height * 0.5f
        }
        sky.feed(fromX, fromY) {
            sky.celebrate()
            val grewTo = result.grewTo
            if (byVoice) {
                if (grewTo != null) {
                    main.postDelayed({ sky.evolve(grewTo) }, EVOLVE_DELAY_MS)
                    coach.setMinLevel(pet.englishFloor)
                }
                return@feed
            }
            if (grewTo != null) {
                main.postDelayed({ sky.evolve(grewTo) }, EVOLVE_DELAY_MS)
                // 꼬마·소년이 되면 영어 레벨 최소값도 오릅니다. 올랐으면 한마디 덧붙입니다.
                val harder = coach.setMinLevel(pet.englishFloor)
                narrator.speak(render(if (pet.isGrown) Reactions.FULLY_GROWN else Reactions.GROWN)) {
                    if (harder) narrator.speak(render(Reactions.GROWN_HARDER_ENGLISH)) { }
                }
            } else if (unlocked.isNotEmpty()) {
                narrator.speak(render(Reactions.NEW_ITEM)) { }
            } else {
                narrator.speak(render(Reactions.yum[yumIndex++ % Reactions.yum.size])) { }
            }
        }
    }

    // endregion

    // region 차량 신호 → 화면 + 이야기

    private fun onCarEvent(event: CarEvent) {
        when (event) {
            is CarEvent.TurnSignal -> sky.setTilt(
                when (event.side) {
                    CarEvent.TurnSignal.Side.LEFT -> -1f
                    CarEvent.TurnSignal.Side.RIGHT -> 1f
                    CarEvent.TurnSignal.Side.OFF -> 0f
                }
            )
            is CarEvent.Wiper -> {
                raining = event.on
                sky.setRaining(event.on)
                if (settings.visibility == View.VISIBLE) renderSettings()
            }
            is CarEvent.Gear -> {
                val position = event.position
                driving = position != CarEvent.Gear.Position.PARK && position != CarEvent.Gear.Position.UNKNOWN
                sky.setDriving(driving)
                // 설정, 글자 입력, 간식 주기는 차를 세웠을 때만 합니다.
                settingsButton.isEnabled = !driving
                settingsButton.alpha = if (driving) 0.35f else 1f
                if (driving && settings.visibility == View.VISIBLE) closeSettings()
                if (driving && houseScreen.visibility == View.VISIBLE) closeHouse()
                if (driving && wordbookScreen.visibility == View.VISIBLE) closeWordBook()
                renderHome()
                renderCaption()
            }
            else -> Unit
        }
        engine.onCarEvent(event)
    }

    // endregion

    // region StoryEngine.Ui

    override fun onCaption(text: String, english: Boolean) {
        lastCaption = text
        lastCaptionEnglish = english
        renderCaption()
    }

    override fun onScene(chapterTitle: String, progress: Float) {
        chapter.text = chapterTitle
        chapter.visibility = if (chapterTitle.isEmpty()) View.INVISIBLE else View.VISIBLE
        sky.setProgress(progress)
    }

    override fun onListening(active: Boolean) {
        listeningTag.visibility = if (active && !driving) View.VISIBLE else View.GONE
    }

    override fun onPhase(phase: Phase) {
        beltCard.visibility = if (phase == Phase.WAITING_BELT) View.VISIBLE else View.GONE
        if (phase == Phase.WAITING_BELT) beltHint.text = render(getString(R.string.belt_hint))
        // 이야기가 흐르는 동안에는 앱이 뒤로 가도(내비 화면) 멈추지 않도록 포그라운드 서비스를 켭니다.
        if (phase == Phase.IDLE || phase == Phase.ENDED) StoryService.stop(this) else StoryService.start(this)
        when (phase) {
            // "그만"을 눌렀을 때
            Phase.IDLE -> leaveStory()
            // 이야기가 끝났을 때: 마지막 말의 여운을 잠깐 둔 뒤 홈으로 돌아갑니다.
            Phase.ENDED -> {
                heardStory = true
                grewJustNow = false
                newItemJustNow = false
                main.postDelayed({ if (engine.phase == Phase.ENDED) leaveStory() }, STAY_AFTER_ENDING_MS)
            }
            else -> Unit
        }
    }

    // endregion

    /**
     * 강아지의 말풍선. 가르치는 영어는 크게 보여 주고,
     * 주행 중에는 긴 글을 숨기고 짧은 안내만 보여 줍니다(이야기는 소리로만 전달).
     */
    private fun renderCaption() {
        val text = lastCaption
        if (text == null) {
            caption.visibility = View.INVISIBLE
            return
        }
        caption.visibility = View.VISIBLE
        when {
            driving -> {
                caption.text = getString(R.string.caption_driving)
                caption.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40f)
                caption.setTextColor(getColor(R.color.hint))
            }
            lastCaptionEnglish -> {
                caption.text = text
                caption.setTextSize(TypedValue.COMPLEX_UNIT_SP, 84f)
                caption.setTextColor(getColor(R.color.kid_blue))
            }
            else -> {
                caption.text = keepWordsTogether(text)
                caption.setTextSize(TypedValue.COMPLEX_UNIT_SP, 46f)
                caption.setTextColor(getColor(R.color.ink))
            }
        }
    }

    /** 한글은 글자 사이 어디서나 줄이 바뀌므로, 낱말 안에 줄바꿈 금지 문자를 넣어 띄어쓰기에서만 바뀌게 합니다. */
    private fun keepWordsTogether(text: String): String =
        text.split(' ').joinToString(" ") { word -> word.toList().joinToString("⁠") }

    // region 강아지 집

    private fun openHouse() {
        if (driving || transitioning || screen != Screen.HOME) return
        newItemJustNow = false
        houseTitle.text = render(getString(R.string.house_button))
        houseView.setStage(pet.stage)
        houseView.refresh()
        house.markSeen()
        refreshTray()
        slideIn(houseScreen)
    }

    private fun closeHouse() {
        if (houseScreen.visibility != View.VISIBLE) return
        slideOut(houseScreen)
        renderHome()
    }

    /** 물건 상자: 두 칸씩, 열리는 순서대로. 놓인 물건은 금색 테두리, 안 열린 물건은 흐리게. */
    private fun buildTray() {
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        houseTray.removeAllViews()
        House.ITEMS.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply { bottomMargin = dp(14) }
            }
            pair.forEachIndexed { index, item ->
                val cell = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    setBackgroundResource(R.drawable.bg_tray_item)
                    setPadding(dp(10), dp(14), dp(10), dp(12))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        .apply { if (index == 1) marginStart = dp(14) }
                    tag = item
                }
                cell.addView(ImageView(this).apply {
                    setImageResource(item.drawable)
                    layoutParams = LinearLayout.LayoutParams(dp(118), dp(118))
                })
                cell.addView(TextView(this).apply {
                    textSize = 24f
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        .apply { topMargin = dp(6) }
                })
                Clay.pressable(cell) {
                    if (house.toggle(item)) {
                        houseView.refresh(bounce = item)
                        refreshTray()
                    }
                }
                row.addView(cell)
            }
            houseTray.addView(row)
        }
        refreshTray()
    }

    private fun refreshTray() {
        for (r in 0 until houseTray.childCount) {
            val row = houseTray.getChildAt(r) as LinearLayout
            for (c in 0 until row.childCount) {
                val cell = row.getChildAt(c) as LinearLayout
                val item = cell.tag as House.Item
                val unlocked = house.isUnlocked(item)
                val placed = house.isPlaced(item)
                cell.isSelected = placed
                cell.alpha = if (unlocked) 1f else 0.38f
                val label = cell.getChildAt(1) as TextView
                label.text = if (unlocked) item.name else getString(R.string.house_locked, item.unlockLevel)
                label.setTextColor(getColor(if (placed) R.color.ink else R.color.text))
            }
        }
    }

    // endregion

    // region 꿈 구슬 도감 (아이용)

    private fun openWordBook() {
        if (driving || transitioning || screen != Screen.HOME) return
        wordbookTitle.text = render(getString(R.string.wordbook_title))
        wordbookHint.text = render(getString(R.string.wordbook_hint))
        buildWordGrid()
        slideIn(wordbookScreen)
    }

    private fun closeWordBook() {
        if (wordbookScreen.visibility != View.VISIBLE) return
        narrator.stop()
        slideOut(wordbookScreen)
    }

    /** 배운 말을 카드로 늘어놓습니다. 꿈 구슬이 된 말은 금색 카드, 배우는 중인 말은 흰 카드에 구슬 점. */
    private fun buildWordGrid() {
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val summary = coach.summary()
        wordbookSummary.text = getString(R.string.wordbook_summary, summary.mastered.size, summary.learning.size)
        wordGrid.removeAllViews()

        if (summary.mastered.isEmpty() && summary.learning.isEmpty()) {
            wordGrid.addView(TextView(this).apply {
                setText(R.string.wordbook_empty)
                textSize = 34f
                setTextColor(getColor(R.color.text_dim))
                gravity = Gravity.CENTER
                setPadding(0, dp(120), 0, 0)
            })
            return
        }

        fun section(title: Int, items: List<EnglishCoach.WordState>, mastered: Boolean) {
            if (items.isEmpty()) return
            wordGrid.addView(TextView(this).apply {
                setText(title)
                textSize = 34f
                setTextColor(getColor(R.color.moon))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply { topMargin = dp(18); bottomMargin = dp(10) }
            })
            items.chunked(WORD_COLUMNS).forEach { rowItems ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        .apply { bottomMargin = dp(20) }
                }
                for (index in 0 until WORD_COLUMNS) {
                    val state = rowItems.getOrNull(index)
                    val cell = if (state == null) View(this) else wordCard(state, mastered, dp(1))
                    cell.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        .apply { if (index > 0) marginStart = dp(20) }
                    row.addView(cell)
                }
                wordGrid.addView(row)
            }
        }
        section(R.string.wordbook_section_mastered, summary.mastered, mastered = true)
        section(R.string.wordbook_section_learning, summary.learning, mastered = false)
    }

    private fun wordCard(state: EnglishCoach.WordState, mastered: Boolean, dp: Int): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundResource(if (mastered) R.drawable.clay_yellow else R.drawable.clay_white)
            setPadding(16 * dp, 26 * dp, 16 * dp, 30 * dp)
            minimumHeight = 190 * dp
        }
        card.addView(TextView(this).apply {
            text = state.word.english
            textSize = if (state.word.english.length > 14) 30f else 40f
            setTextColor(getColor(R.color.ink))
            gravity = Gravity.CENTER
        })
        card.addView(TextView(this).apply {
            text = state.word.korean
            textSize = 26f
            setTextColor(getColor(R.color.hint))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = 4 * dp }
        })
        card.addView(TextView(this).apply {
            text = if (mastered) {
                getString(R.string.wordbook_mastered)
            } else {
                "●".repeat(state.stage.coerceIn(0, EnglishCoach.STAGES_TO_MASTER)) +
                    "○".repeat((EnglishCoach.STAGES_TO_MASTER - state.stage).coerceAtLeast(0))
            }
            textSize = 24f
            setTextColor(getColor(if (mastered) R.color.ink else R.color.kid_blue))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = 10 * dp }
        })
        Clay.pressable(card) { readWord(state.word) }
        return card
    }

    /** 카드를 누르면 영어, 이어서 우리말을 읽어 줍니다. */
    private fun readWord(word: Word) {
        narrator.stop()
        narrator.speak(word.english, Lang.EN) {
            narrator.speak("‘${word.korean}’.") { }
        }
    }

    // endregion

    // region 설정 (어른용)

    private fun openSettings() {
        if (driving || transitioning || screen != Screen.HOME) return
        renderSettings()
        slideIn(settings)
    }

    private fun closeSettings() {
        if (settings.visibility != View.VISIBLE) return
        slideOut(settings)
        renderHome()
    }

    private fun slideIn(view: View) {
        view.translationX = view.width.toFloat().takeIf { it > 0f } ?: resources.displayMetrics.widthPixels.toFloat()
        view.visibility = View.VISIBLE
        view.animate().translationX(0f).setDuration(SLIDE_MS).setInterpolator(DecelerateInterpolator(1.5f)).start()
    }

    private fun slideOut(view: View) {
        view.animate().translationX(view.width.toFloat()).setDuration(SLIDE_MS)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .withEndAction { view.visibility = View.GONE }
            .start()
    }

    private fun renderSettings() {
        val name = engine.childName
        settingsNameValue.text =
            if (name.isSet) render(getString(R.string.settings_name_set, name.given))
            else getString(R.string.settings_name_unset)

        settingsPetNameValue.text = getString(R.string.settings_pet_name_set, pet.name.given, pet.level, pet.stageName)

        val summary = coach.summary()
        settingsEnglishValue.text =
            if (summary.enabled) {
                val next = summary.nextNew.joinToString(", ") { pick ->
                    pick.word.english + (pick.reason?.let { " ($it)" } ?: "")
                }
                val pace = when (val rate = summary.successRate) {
                    null -> getString(R.string.settings_english_pace_unknown, summary.newPerTrip)
                    else -> getString(R.string.settings_english_pace, (rate * 100).toInt(), summary.newPerTrip)
                }
                getString(R.string.settings_english_on, summary.level, summary.mastered.size, summary.learning.size) +
                    "\n" + getString(R.string.settings_english_next, next.ifEmpty { "-" }) + "\n" + pace
            } else {
                getString(R.string.settings_english_off)
            }

        settingsVoiceValue.text = voiceStatus

        settingsStoryValue.text = when (stories.state) {
            StoryGenerator.State.NO_KEY -> getString(R.string.settings_story_no_key)
            StoryGenerator.State.WORKING -> getString(R.string.settings_story_working)
            StoryGenerator.State.READY -> getString(R.string.settings_story_ready, render(todayPack().title))
            StoryGenerator.State.IDLE -> getString(R.string.settings_story_idle)
            StoryGenerator.State.FAILED -> getString(R.string.settings_story_failed, stories.problem ?: "")
        }

        val today = note.text
        settingsNoteValue.text = when {
            stories.state == StoryGenerator.State.NO_KEY -> getString(R.string.settings_note_no_key)
            today == null -> getString(R.string.settings_note_empty)
            else -> getString(R.string.settings_note_set, today)
        }
    }

    private fun showNameDialog() = showNameDialog(
        title = R.string.name_title,
        message = R.string.name_message,
        hint = R.string.name_hint,
        current = engine.childName.given,
        allowClear = true,
        onSave = ::setChildName,
    )

    private fun showPetNameDialog() = showNameDialog(
        title = R.string.pet_name_title,
        message = R.string.pet_name_message,
        hint = R.string.pet_name_hint,
        current = pet.name.given,
        allowClear = false,
        onSave = pet::setName,
    )

    private fun showNameDialog(
        title: Int,
        message: Int,
        hint: Int,
        current: String,
        allowClear: Boolean,
        onSave: (String) -> Unit,
    ) {
        val padding = (32 * resources.displayMetrics.density).toInt()
        val builder = AlertDialog.Builder(this)
        val input = EditText(builder.context).apply {
            // 대화상자 테마가 밝든 어둡든 글자가 보이도록 바탕과 글자색을 직접 정합니다.
            setBackgroundResource(R.drawable.bg_input)
            setTextColor(getColor(R.color.ink))
            setHintTextColor(getColor(R.color.hint))
            setPadding(padding, padding / 2, padding, padding / 2)
            setText(current)
            setSelection(text.length)
            this.hint = getString(hint)
            textSize = 40f
            isSingleLine = true
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PERSON_NAME
            filters = arrayOf(InputFilter.LengthFilter(ChildName.MAX_LENGTH))
        }
        val container = FrameLayout(builder.context).apply {
            setPadding(padding, padding / 2, padding, 0)
            addView(input)
        }
        builder
            .setTitle(title)
            .setMessage(keepWordsTogether(getString(message)))
            .setView(container)
            .setPositiveButton(R.string.name_save) { _, _ -> onSave(input.text.toString()) }
            .setNegativeButton(android.R.string.cancel, null)
        if (allowClear) builder.setNeutralButton(R.string.name_clear) { _, _ -> onSave("") }
        builder.show()
    }

    /** 오늘 있었던 일을 적는 대화상자. 저장하면 그 일을 넣어 오늘의 이야기를 다시 씁니다. */
    private fun showNoteDialog() {
        val padding = (32 * resources.displayMetrics.density).toInt()
        val builder = AlertDialog.Builder(this)
        val input = EditText(builder.context).apply {
            setBackgroundResource(R.drawable.bg_input)
            setTextColor(getColor(R.color.ink))
            setHintTextColor(getColor(R.color.hint))
            setPadding(padding, padding / 2, padding, padding / 2)
            setText(note.text.orEmpty())
            setSelection(text.length)
            hint = getString(R.string.note_hint)
            textSize = 32f
            minLines = 3
            gravity = Gravity.TOP or Gravity.START
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            filters = arrayOf(InputFilter.LengthFilter(TodayNote.MAX_LENGTH))
        }
        val container = FrameLayout(builder.context).apply {
            setPadding(padding, padding / 2, padding, 0)
            addView(input)
        }
        builder
            .setTitle(R.string.settings_note)
            .setMessage(keepWordsTogether(getString(R.string.note_message)))
            .setView(container)
            .setPositiveButton(R.string.name_save) { _, _ -> saveNote(input.text.toString()) }
            .setNeutralButton(R.string.note_clear) { _, _ -> saveNote("") }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun saveNote(raw: String) {
        if (TodayNote.clean(raw).ifEmpty { null } == note.text) return
        note.save(raw)
        renderSettings()
        prepareStory(force = true)
    }

    private fun setChildName(raw: String) {
        val name = ChildName(raw)
        prefs.edit().putString(KEY_CHILD_NAME, name.given).apply()
        engine.childName = name
        renderSettings()
        renderHome()
    }

    // endregion

    private companion object {
        const val KEY_CHILD_NAME = "childName"
        const val STAY_AFTER_ENDING_MS = 2_500L
        const val SLIDE_MS = 280L
        const val FEED_LOCK_MS = 1_100L
        private val VOICE_FEED = Any()
        const val EVOLVE_DELAY_MS = 900L
        const val REFIT_TIMEOUT_MS = 15_000L
        const val DEBUG_NAVI_ACTION = "com.ajikmeoreosseo.app.DEBUG_NAVI"
        const val DEBUG_FOCUS_ACTION = "com.ajikmeoreosseo.app.DEBUG_FOCUS"
        const val DEBUG_TOOLS_ACTION = "com.ajikmeoreosseo.app.DEBUG_TOOLS"
        const val WORD_COLUMNS = 4
    }
}

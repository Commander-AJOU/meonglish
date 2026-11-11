package com.ajikmeoreosseo.app.story

/** 이야기 대본의 한 조각. 엔진이 위에서부터 차례로 처리합니다. */
sealed interface Beat {
    /** 새 장의 시작. 화면의 장 제목을 바꿉니다. */
    data class Chapter(val title: String) : Beat

    /**
     * 내레이션 한 문장. 문장 단위로 끊어야 차량 신호가 사이에 끼어들 수 있습니다.
     * [needsName]이 true면 아이 이름을 정했을 때만 읽습니다(이름 글자로 하는 놀이 등).
     */
    data class Say(val text: String, val needsName: Boolean = false, val pauseAfterMs: Long = 0) : Beat

    /** 바로 앞 문장에 나온 낱말의 영어 메아리("Moon!"). [StoryEcho]가 넣습니다. */
    data class Echo(val english: String) : Beat

    /** 장과 장 사이의 쉼. 길이는 남은 주행 시간에 맞춰 엔진이 정합니다. */
    data object Gap : Beat

    /** 뒷자리에 아이가 앉았는데 안전벨트를 안 맸다면 여기서 기다립니다. */
    data object BeltGate : Beat
}

/** 강아지의 말투. 아기일 때는 서툴고, 다 자라면 든든합니다. 인사·놀이·결말·수업 첫마디가 이에 따라 바뀝니다. */
enum class Tone { BABY, KID, GROWN }

/** 한 번의 이야기에 필요한 전부: 제목, 본문, 기다릴 때의 놀이, 결말. 기본 이야기([Story.pack])이거나 그날 만든 이야기입니다. */
class StoryPack(
    val title: String,
    val script: List<Beat>,
    val interludes: List<Beat.Say>,
    val ending: List<String>,
    /** 그날 만든 이야기면 true. 홈과 설정에서 표시에 씁니다. */
    val generated: Boolean = false,
)

/**
 * 「구름 강아지와 달빛 섬」
 *
 * 차가 폭신한 구름이 되어, 구름 강아지와 뒷자리 아이를 태우고 달빛 섬으로 날아가는 이야기입니다.
 * 주인공은 아이 자신이고, 길동무는 아이가 키우는 구름 강아지입니다.
 * {이름} 표시는 아이 이름으로, {멍} 표시는 강아지 이름으로 바뀝니다([ChildName.render]).
 * 본문은 고정이고, 차량 신호에 따른 반응 대사([Reactions])가 그 사이에 끼어듭니다.
 */
object Story {
    const val TITLE = "구름 강아지 {멍의} 달빛 섬 여행"

    /** 첫 장의 인사 두 마디. 말투에 따라 다릅니다. */
    private fun opening(tone: Tone): List<Beat> = when (tone) {
        Tone.BABY -> listOf(
            Beat.Say("안녕, {이름아}! 나는 구름 강아지 {멍}야. 아직 아기라서 멍 소리도 조그매. 멍!"),
            Beat.Say("오늘은 이 차가 폭신폭신한 구름이 될 거야. {이름이} 나랑 같이 가 주면 하나도 안 무서워. 달빛 섬까지 날아가 보자."),
        )
        Tone.KID -> listOf(
            Beat.Say("안녕, {이름아}! 나는 구름 강아지 {멍}야. 멍멍!"),
            Beat.Say("오늘은 이 차가 폭신폭신한 구름이 될 거야. {이름이랑} 같이 달빛 섬까지 날아가 보자."),
        )
        Tone.GROWN -> listOf(
            Beat.Say("안녕, {이름아}! 구름 강아지 {멍}야. 이제 하늘 길은 눈 감고도 알아. 멍멍!"),
            Beat.Say("오늘은 이 차가 폭신폭신한 구름이 될 거야. {이름은} 편하게 앉아 있어. 달빛 섬까지 내가 안내할게."),
        )
    }

    fun script(tone: Tone): List<Beat> = listOf(Beat.Chapter("만나서 반가워")) + opening(tone) + body

    private val body: List<Beat> = listOf(
        Beat.BeltGate,
        Beat.Gap,

        Beat.Chapter("하늘 길로 출발"),
        Beat.Say("{멍이} 꼬리를 살랑살랑 크게 흔들었어요."),
        Beat.Say("{이름을} 태운 구름은 하늘 길로 스르르 떠올랐어요."),
        Beat.Say("저 아래 집들이 장난감처럼 작아졌어요."),
        Beat.Say("달빛 섬에는 잃어버린 꿈들이 모여 산대요. 오늘 {이름은} 그 꿈 하나를 찾으러 가요."),
        Beat.Gap,

        Beat.Chapter("별 물고기 떼"),
        Beat.Say("그때, 반짝반짝 별 물고기 떼가 나타났어요."),
        Beat.Say("별 물고기들이 물었어요. 구름을 탄 친구는 누구야? {멍이} 대답했어요. 내 친구 {이름}야! 멍멍!"),
        Beat.Say(
            "별 물고기들이 하늘에 '{첫글자}' 자를 반짝반짝 그렸어요. {이름의} 이름 첫 글자래요.",
            needsName = true,
        ),
        Beat.Say("별 물고기들은 길을 아는 친구들이에요. 구름이 방향을 바꿀 때마다 같이 헤엄쳐 준대요."),
        Beat.Say("깜빡깜빡 소리가 나면, 별 물고기들이 신호를 보내는 거야. {멍}처럼 귀를 쫑긋 세워 봐."),
        Beat.Gap,

        Beat.Chapter("구름 터널"),
        Beat.Say("앞에 커다란 구름 터널이 보여요."),
        Beat.Say("터널 안은 솜사탕처럼 폭신폭신해요."),
        Beat.Say("{멍이} 속삭였어요. {이름아}, 여기서는 작은 목소리로 말해야 해. 구름이 자고 있거든."),
        Beat.Say("하나, 둘, 셋. 조용히, 조용히 지나가요."),
        Beat.Gap,

        Beat.Chapter("바람 언덕"),
        Beat.Say("터널을 나오니 바람 언덕이에요."),
        Beat.Say("바람 아저씨가 휘이 하고 휘파람을 불어요."),
        Beat.Say("{멍은} 귀를 펄럭이며 바람을 타고 더 높이, 더 멀리 날아가요."),
        Beat.Say("{이름아}, 지금 창밖에 뭐가 보여? 마음속으로 하나만 골라서 기억해 둬. 달빛 섬에 가면 쓸 데가 있어."),
        Beat.Gap,

        Beat.Chapter("달빛 섬이 보여요"),
        Beat.Say("저 멀리 은빛으로 빛나는 섬이 보여요. 달빛 섬이에요!"),
        Beat.Say("섬 위에는 꿈 구슬들이 둥둥 떠 있어요."),
        Beat.Say(
            "그중에 '{이름}'라고 적힌 구슬이 하나 있어요. {이름이} 오기만 기다리고 있었대요.",
            needsName = true,
        ),
        Beat.Say("아까 창밖에서 고른 것, 기억나? 그걸 떠올리면 꿈 구슬이 {이름한테} 날아온대."),
        Beat.Say("{멍은} 천천히, 아주 천천히 내려갈 준비를 해요."),
    )

    /** 본문이 끝났는데 아직 도착하지 않았을 때 번갈아 들려주는 놀이. 말투별 놀이 하나가 앞에 붙습니다. */
    fun interludes(tone: Tone): List<Beat.Say> = listOf(
        when (tone) {
            Tone.BABY -> Beat.Say("{멍이} 하품을 해요. 하아암. {이름아}, 졸리면 {멍이랑} 같이 눈을 감고 구름 냄새를 맡아 봐.")
            Tone.KID -> Beat.Say("{멍이} 꼬리로 북을 쳐요. 둥둥! {이름아}, 손뼉으로 같이 박자를 맞춰 볼까?")
            Tone.GROWN -> Beat.Say("{멍이} 멀리 내다봐요. {이름아}, 저 앞에 뭐가 있을지 맞혀 볼래? 둥근 거야, 긴 거야?")
        }
    ) + commonInterludes

    private val commonInterludes: List<Beat.Say> = listOf(
        Beat.Say("달빛 섬이 조금씩 가까워지고 있어요. 그동안 창밖에서 빨간색을 세 개 찾아볼까?"),
        Beat.Say("{이름의} 이름은 {글자수} 글자지? 창밖에서 똑같은 걸 {글자수} 개 찾아볼까?", needsName = true),
        Beat.Say("{멍이} 콧노래를 불러요. 멍멍, 멍멍. {이름이} 좋아하는 노래를 한 곡 불러 줄래?"),
        Beat.Say("구름 모양 맞히기 놀이를 해 볼까? 지금 보이는 구름은 뭘 닮았어?"),
        Beat.Say("'{첫글자}' 자로 시작하는 말을 세 개 말해 볼까? {멍은} 벌써 하나 생각났어.", needsName = true),
        Beat.Say("{멍이} 물어봐요. {이름은} 달빛 섬에 도착하면 제일 먼저 뭘 하고 싶어?"),
        Beat.Say("이번에는 동그란 것을 찾아볼까? 바퀴, 표지판, 또 뭐가 있을까?"),
    )

    fun ending(tone: Tone): List<String> = listOf(
        "도착! {이름이} 달빛 섬에 내렸어요.",
        "오늘 찾은 꿈 구슬은 {이름의} 주머니에 쏙 넣어 줄게.",
        when (tone) {
            Tone.BABY -> "{멍은} 여기서 낮잠 한숨 자고 있을게. {이름아}, 다음에 차를 타면 또 같이 날자. 멍!"
            Tone.KID -> "{멍은} 여기서 기다릴게. {이름아}, 다음에 차를 타면 또 같이 날자. 멍멍!"
            Tone.GROWN -> "{멍은} 여기서 달빛 섬을 지키고 있을게. {이름아}, 다음에 차를 타면 또 같이 날자. 멍멍!"
        },
    )

    /** 영어 수업을 여는 한마디. */
    fun lessonOpening(tone: Tone): String = when (tone) {
        Tone.BABY -> "{멍의} 아기 영어 한 마디!"
        Tone.KID -> "{멍의} 영어 한 마디!"
        Tone.GROWN -> "{멍의} 멋진 영어 한 마디!"
    }

    /** 기본 이야기 한 벌. 그날 만든 이야기가 없거나 만들 수 없을 때 씁니다. */
    fun pack(tone: Tone): StoryPack = StoryPack(TITLE, script(tone), interludes(tone), ending(tone))
}

/** 차량 신호에 반응하는 대사. 같은 말을 반복하지 않도록 여러 개 중에서 돌아가며 씁니다. */
object Reactions {
    val turnLeft = listOf(
        "깜빡깜빡! {멍이} 왼쪽으로 몸을 기울여요. 별 물고기들도 왼쪽으로 휘익!",
        "{이름아}, 왼쪽으로 돌아요. 왼쪽 창밖에 뭐가 지나가는지 볼까?",
        "별 물고기들이 왼쪽 길을 알려 줬어요. {멍이} 꼬리를 살랑 흔들어요.",
    )
    val turnRight = listOf(
        "깜빡깜빡! {멍이} 오른쪽으로 몸을 기울여요. 별 물고기들도 오른쪽으로 휘익!",
        "{이름아}, 오른쪽으로 돌아요. 오른쪽 창밖에 뭐가 지나가는지 볼까?",
        "별 물고기들이 오른쪽 길을 알려 줬어요. {멍이} 꼬리를 살랑 흔들어요.",
    )
    val rainStart = listOf(
        "어, 빗방울이다! 빗방울 요정들이 창문에서 미끄럼을 타요. 쓱싹쓱싹, {멍이} 앞발로 닦아 줘요.",
        "비가 와요. 구름 강아지는 빗소리를 제일 좋아해요. {이름아}, 빗소리에 맞춰 발가락을 꼼지락거려 볼까?",
    )
    val rainEnd = listOf(
        "비가 그쳤어요. 어딘가에 무지개가 숨어 있을지도 몰라. 한번 찾아볼까?",
    )
    val stopped = listOf(
        "{멍이} 잠깐 멈춰서 숨을 골라요. {이름아}, 우리도 같이 숨을 크게 들이쉬고, 후우 내쉬어 볼까?",
        "잠깐 쉬어 가요. 멈춰 있는 동안 창밖에 있는 걸 세 가지만 말해 볼까?",
    )
    val goAgain = listOf(
        "다시 출발! {멍이} 꼬리를 힘차게 흔들어요.",
        "자, 다시 날아가자! 멍멍!",
    )
    val reverse = listOf(
        "{멍이} 살금살금 뒷걸음질해요. 강아지도 뒤로 갈 줄 안다니까.",
    )

    const val BELT_ASK =
        "그런데 잠깐, {이름아}. 구름을 타려면 안전벨트를 찰칵 매야 해. 찰칵 소리가 나면 출발할게."
    const val BELT_LOST =
        "어, {이름아}! 안전벨트가 풀렸어. {멍은} 벨트를 매야 날 수 있어. 다시 찰칵 해 줄래?"
    const val BELT_OK = "찰칵! 좋아, 준비 끝. {이름아}, 꽉 잡아!"
    const val DOOR_AFTER_ARRIVAL = "문이 열렸네. {이름아}, 발밑 조심하고 천천히 내려요."

    /**
     * 내비 안내가 끝난 뒤, 그 내용을 이야기 속 말로 받아 줍니다("길 안내 요정이 오른쪽으로 가래!").
     * {안내}는 [naviTurnWords]의 말, {속도}·{분}·{거리}는 숫자로 바뀝니다.
     */
    val naviTurn = listOf(
        "{멍이} 귀를 쫑긋 세웠어요. 길 안내 요정이 {안내}. 우리 달빛 섬은 그쪽인가 봐!",
        "방금 들었어? 길 안내 요정이 {안내}. {멍이} 꼬리로 그쪽을 가리켜요.",
    )
    val naviSpeed = listOf(
        "아, 여기 하늘 길은 {속도}까지만 날 수 있대. {멍이} 구름 속도를 살짝 줄여요. 천천히 가도 달빛 섬은 안 도망가.",
        "길 안내 요정이 속삭였어요. 이 길은 {속도}가 딱 좋대. 구름이 살랑살랑 천천히 날아가요.",
    )
    val naviCamera = listOf(
        "저 앞에 반짝이는 별 카메라가 있대. 사진 찍히지 않게 살금살금, 조용히 지나가자.",
        "{멍이} 속삭여요. 별 카메라가 지켜보고 있대. 우리 구름은 얌전하게 천천히!",
    )
    val naviRemaining = listOf(
        "{멍이} 코를 킁킁. 달빛 섬 냄새가 나! {분}분이면 도착이래.",
        "길 안내 요정이 알려 줬어요. 달빛 섬까지 {거리} 남았대. 조금만 더 날아가자.",
    )

    /** 내비의 다음 안내를 아이 말로. */
    val naviTurnWords: Map<String, String> = mapOf(
        "좌회전" to "왼쪽으로 돌아가래",
        "우회전" to "오른쪽으로 돌아가래",
        "왼쪽 길" to "왼쪽 길로 가래",
        "오른쪽 길" to "오른쪽 길로 가래",
        "직진" to "쭉 곧장 가래",
        "유턴" to "빙글 돌아서 되돌아가래",
        "회전교차로" to "빙글빙글 도는 길로 가래",
        "터널" to "구름 터널 속으로 들어가래",
        "톨게이트" to "하늘 문을 지나가래",
        "고속도로 진입" to "커다란 하늘 길로 올라타래",
        "고속도로 출구" to "커다란 하늘 길에서 내려가래",
        "도착" to "거의 다 왔대",
    )

    /** 영어를 한 가지 배울 때마다 간식이 생겼다고 알려 주는 말. */
    val treatEarned = listOf(
        "딩동! {멍의} 간식이 하나 생겼어.",
        "간식 하나 더! {멍이} 좋아하겠다.",
        "간식 주머니가 점점 무거워져.",
    )

    /** 영어를 만났지만 목소리가 들리지 않았을 때: 반 조각. 둘이 모이면 하나. */
    const val TREAT_HALF = "간식 반 조각이 생겼어. 반 조각 더 모으면 하나가 돼!"
    const val TREAT_HALF_COMPLETED = "반 조각 두 개가 만나서, 간식 하나 완성!"

    /** 도착했을 때 모은 간식을 알려 주는 말(차가 아이 목소리를 들을 수 없을 때). */
    const val TREATS_AT_ARRIVAL = "오늘 모은 간식은 {개수}개! 차를 세우면 {멍한테} 줄 수 있어."

    /**
     * 도착했을 때 뒷자리에서 말로 간식을 주게 합니다. 아이 손이 차량 화면에 닿지 않으므로,
     * 아이가 "Here you are!"라고 말하면 차가 듣고 간식이 강아지에게 날아갑니다.
     */
    const val FEED_ASK = "오늘 모은 간식은 {개수}개! {멍한테} 간식을 줄 때는 영어로 이렇게 말해."
    const val FEED_SAY = "Here you are!"
    const val FEED_OK = "냠냠! 고마워, {이름아}! 정말 맛있다!"
    const val FEED_LATER = "괜찮아. 차를 세운 뒤에 간식 주기 버튼을 눌러도 돼."

    /** 간식을 먹었을 때. */
    val yum = listOf(
        "냠냠! 고마워, {이름아}!",
        "멍멍! 맛있다!",
        "냠냠냠. 한 입에 꿀꺽!",
    )

    /** 단계가 올라 집에 새 물건이 열렸을 때. */
    const val NEW_ITEM = "{멍}네 집에 새 물건이 생겼어! 뭐가 열렸는지 보러 갈까?"

    /** 모습이 바뀔 만큼 자랐을 때. */
    const val GROWN = "와, {멍이} 자랐어! {이름이} 영어를 열심히 배운 덕분이야."
    const val FULLY_GROWN = "{멍이} 다 자랐어! 이제 {이름의} 가장 든든한 친구야. 그래도 간식은 계속 좋아해."

    /** 자라면서 영어 레벨 최소값이 올랐을 때 덧붙이는 말. */
    const val GROWN_HARDER_ENGLISH = "그리고 {멍이} 자라서, 이제 조금 더 긴 영어도 배울 수 있어!"

    /** 이야기를 시작하자마자 미리 받아 둘 반응 대사(ElevenLabs 사용 시 첫 반응이 늦지 않도록). */
    /**
     * 부모의 비밀 버튼을 눌렀을 때. 한 묶음씩 돌아가며 말합니다.
     * 겁을 주거나("울면 떠날 거야") 울음을 참으라고 하지 않습니다. 마음을 먼저 받아 주고, 곁에 있다고 말해 주고,
     * 숨 고르기나 다음에 할 일로 마음을 돌려 줍니다.
     */
    val calm: List<List<String>> = listOf(
        listOf("{이름아}, 속상했구나. 괜찮아, {멍이} 옆에 있어.", "우리 같이 후 하고 숨 쉬어 볼까? 후."),
        listOf("어? {멍의} 귀가 쫑긋했어. {이름아}, 무슨 일이야?", "울어도 괜찮아. {멍이} 꼭 붙어 있을게."),
        listOf("{이름아}, {멍이} 여기 있어. 눈물이 나면 울어도 돼.", "다 울고 나면 {멍이랑} 같이 달빛 섬에 가자."),
    )

    val warmUp: List<String> = listOf(
        turnLeft[0], turnRight[0], stopped[0], goAgain[0], BELT_ASK, BELT_OK, treatEarned[0], TREAT_HALF, TREAT_HALF_COMPLETED,
    )
}

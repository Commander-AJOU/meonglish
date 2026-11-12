import { Video } from "@remotion/media";
import { AbsoluteFill, Img, interpolate, random, staticFile, useCurrentFrame } from "remotion";
import { Header, Phase, Pill } from "../parts/Bits";
import { DeviceFrame } from "../parts/DeviceFrame";
import { PuppyOnCloud } from "../parts/Puppy";
import { FEED_INTRO } from "../timeline";
import { C, FONT, art, pop, s } from "../theme";

/** 앱과 같은 성장 단계(Pet.kt): 모습이 바뀌는 단계와 이름. */
const STAGES = [
  { level: 1, name: "갓 태어난 강아지" },
  { level: 10, name: "아기 강아지" },
  { level: 30, name: "꼬마 강아지" },
  { level: 60, name: "소년 강아지" },
  { level: 100, name: "다 자란 강아지" },
];
/** 모습이 바뀌는 때(2~5번째 모습). */
/** 앞의 FEED_INTRO초는 "첫 화면(로비)에서 말로 간식 주기"(실제 앱 화면)이고, 그 뒤로 자라는 모습이 이어집니다. */
const INTRO = s(FEED_INTRO);
const GROW_AT = [1.1, 1.9, 2.7, 3.5].map((at) => INTRO + s(at));
const NAME_AT = INTRO + s(5.0);
/** 아이가 "Here you are!"라고 말하는 때(그래픽 말풍선). 녹화 화면에서 앱이 듣기 시작하는 때와 맞춥니다. */
const SAY_AT = s(3.3);
const NAMES = ["콩이", "보리", "초코"];
const HOME_AT = INTRO + s(7.7);
const CARDS_AT = INTRO + s(10.2);
/** 그래픽 강아지의 가운데 x. 왼쪽에는 실제 앱의 간식 주기 화면이 들어갑니다. */
const CX = 1250;
/**
 * clips/feed.mp4(로비에서 강아지가 "Here you are!"를 알려 주고 아이가 따라 말함)에서 첫 간식이 개수 표시에서
 * 날아가는 때(초). 그래픽 간식과 맞춥니다. 녹화의 2초부터 잘라 썼습니다(원래 녹화 9.1초).
 */
const CLIP_FEED = 7.1;

/** 5-1: 간식을 주면 자라고, 이름은 아이가 지어 줍니다. */
const Growth: React.FC = () => {
  const frame = useCurrentFrame();
  const grown = GROW_AT.filter((at) => frame >= at).length;
  const stage = grown + 1;
  const last = grown > 0 ? frame - GROW_AT[grown - 1] : 99;
  const bump = last < 10 ? Math.sin((last / 10) * Math.PI) : 0;
  const flash = last < 12 ? 1 - last / 12 : 0;
  const level =
    grown >= GROW_AT.length
      ? 100
      : Math.round(
          interpolate(frame, [grown === 0 ? GROW_AT[0] - 20 : GROW_AT[grown - 1], GROW_AT[grown]], [STAGES[grown].level, STAGES[grown + 1].level], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
        );
  const naming = frame >= NAME_AT;
  const nameIndex = Math.min(NAMES.length - 1, Math.floor((frame - NAME_AT) / s(0.8)));
  const nameK = naming ? pop(frame, NAME_AT + nameIndex * s(0.8)) : 0;
  return (
    <AbsoluteFill>
      {/* 실제 앱: 간식 주기 */}
      <div style={{ position: "absolute", left: 80, top: 330, fontFamily: FONT }}>
        <DeviceFrame width={600} ratio={1682 / 1210}>
          <Video
            src={staticFile("demo/clips/feed.mp4")}
            trimBefore={Math.round(CLIP_FEED * 30) - (GROW_AT[0] - 14)}
            muted
            style={{ width: "100%", height: "100%", objectFit: "cover" }}
          />
        </DeviceFrame>
        <div style={{ textAlign: "center", fontSize: 28, color: "rgba(255,255,255,0.65)", marginTop: 12 }}>실제 앱 화면 · 첫 화면에서 말로 간식 주기</div>
      </div>
      {/* 뒷자리 아이가 하는 말 */}
      {frame >= SAY_AT && frame < GROW_AT[0] + s(1.5) ? (
        <div style={{ position: "absolute", left: 150, top: 220, transform: `scale(${pop(frame, SAY_AT)}) rotate(-3deg)`, fontFamily: FONT }}>
          <Pill size={52} bg={C.yellow} color={C.ink}>
            아이: “Here you are!”
          </Pill>
        </div>
      ) : null}
      {/* 날아오는 간식 */}
      {GROW_AT.map((at, i) => {
        const t = interpolate(frame, [at - 14, at], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
        if (t <= 0 || t >= 1) return null;
        return (
          <Img
            key={i}
            src={art("treat")}
            style={{
              position: "absolute",
              width: 130,
              left: interpolate(t, [0, 1], [640, CX - 70]),
              top: interpolate(t, [0, 1], [600, 470]) - Math.sin(t * Math.PI) * 220,
              transform: `rotate(${t * 300}deg) scale(${1 - t * 0.4})`,
            }}
          />
        );
      })}
      {/* 자랄 때 번쩍 */}
      <div
        style={{
          position: "absolute",
          left: CX - 420,
          top: 470 - 420,
          width: 840,
          height: 840,
          borderRadius: "50%",
          background: `radial-gradient(circle, rgba(255,233,168,${flash * 0.85}) 0%, rgba(255,233,168,0) 62%)`,
          transform: `scale(${1.25 - flash * 0.5})`,
        }}
      />
      <div style={{ position: "absolute", left: CX - 262, top: 235, transform: `scale(${1 + bump * 0.16})`, transformOrigin: "50% 80%" }}>
        <PuppyOnCloud stage={stage} size={420} happy={stage > 1 && last < 20} />
      </div>

      {/* 단계 막대 */}
      <div style={{ position: "absolute", left: CX - 440, top: 868, width: 880, height: 10, borderRadius: 5, background: "rgba(255,255,255,0.22)" }}>
        <div style={{ width: `${level}%`, height: "100%", borderRadius: 5, background: C.yellow }} />
        {STAGES.map((st, i) => (
          <div
            key={st.level}
            style={{
              position: "absolute",
              left: `${st.level === 1 ? 0 : st.level}%`,
              top: -11,
              width: 32,
              height: 32,
              marginLeft: -16,
              borderRadius: 16,
              background: stage > i ? C.yellow : "#5B4FD0",
              border: "4px solid #FFFFFF",
              boxSizing: "border-box",
            }}
          />
        ))}
      </div>

      {/* 단계 이름 ↔ 이름표 */}
      <div style={{ position: "absolute", left: CX - 500, top: 742, width: 1000, textAlign: "center", fontFamily: FONT }}>
        {naming ? (
          <div style={{ transform: `scale(${nameK})`, display: "inline-block" }}>
            <Pill size={62} bg="#FFFFFF" color={C.ink}>
              <span style={{ opacity: 0.5, fontSize: 40 }}>이름 </span>
              <span style={{ color: C.pink }}>{NAMES[nameIndex]}</span>
            </Pill>
          </div>
        ) : (
          <div style={{ fontSize: 64, color: "#FFFFFF" }}>
            <span style={{ color: C.yellow }}>{level}단계</span> · {STAGES[stage - 1].name}
          </div>
        )}
      </div>
    </AbsoluteFill>
  );
};

/** 앱과 같은 물건 자리(House.kt). 방 그림 1024×740 기준의 비율, 열리는 단계 순. */
const ITEMS = [
  { id: "rug", x: 0.5, y: 0.76, w: 0.36, layer: 0 },
  { id: "bed", x: 0.5, y: 0.7, w: 0.28, layer: 1 },
  { id: "bowl", x: 0.69, y: 0.8, w: 0.13, layer: 3 },
  { id: "ball", x: 0.4, y: 0.86, w: 0.1, layer: 3 },
  { id: "lamp", x: 0.71, y: 0.58, w: 0.15, layer: 1 },
  { id: "frame", x: 0.35, y: 0.34, w: 0.11, layer: 0 },
  { id: "house", x: 0.29, y: 0.57, w: 0.21, layer: 1 },
  { id: "plant", x: 0.78, y: 0.86, w: 0.13, layer: 3 },
  { id: "toybox", x: 0.24, y: 0.86, w: 0.17, layer: 2 },
  { id: "trophy", x: 0.65, y: 0.35, w: 0.09, layer: 0 },
];
const ROOM_W = 900;
const ROOM_H = (ROOM_W * 740) / 1024;

const WORDS = [
  { en: "Left", ko: "왼쪽", met: 4 },
  { en: "Right", ko: "오른쪽", met: 3 },
  { en: "Rain", ko: "비", met: 4 },
  { en: "Stop", ko: "멈춰", met: 2 },
  { en: "Moon", ko: "달", met: 4 },
  { en: "Cloud", ko: "구름", met: 1 },
  { en: "Star", ko: "별", met: 3 },
  { en: "Puppy", ko: "강아지", met: 2 },
];

/** 5-2: 집 꾸미기와 배운 말 카드(꿈 구슬 도감). */
const Home: React.FC = () => {
  const frame = useCurrentFrame();
  const roomIn = pop(frame, HOME_AT, false);
  const order = [...ITEMS.keys()].sort((a, b) => ITEMS[a].layer - ITEMS[b].layer);
  return (
    <AbsoluteFill>
      <div style={{ position: "absolute", left: 100, top: 232, width: ROOM_W, textAlign: "center", opacity: roomIn }}>
        <Pill size={36}>집 꾸미기 · 자랄수록 새 물건이 열려요</Pill>
      </div>
      <div
        style={{
          position: "absolute",
          left: 100,
          top: 312,
          width: ROOM_W,
          height: ROOM_H,
          borderRadius: 44,
          overflow: "hidden",
          opacity: roomIn,
          transform: `translateY(${(1 - roomIn) * 40}px)`,
          boxShadow: "0 24px 60px rgba(8,6,40,0.5)",
        }}
      >
        <Img src={art("room")} style={{ width: ROOM_W, height: ROOM_H }} />
        {order.map((index) => {
          const item = ITEMS[index];
          const at = HOME_AT + 10 + index * 6;
          const k = frame >= at ? pop(frame, at) : 0;
          const size = item.w * ROOM_W;
          return (
            <Img
              key={item.id}
              src={art(`item_${item.id}`)}
              style={{
                position: "absolute",
                left: item.x * ROOM_W - size / 2,
                top: item.y * ROOM_H - size / 2,
                width: size,
                height: size,
                transform: `scale(${k})`,
                zIndex: item.layer > 2 ? 3 : 1,
              }}
            />
          );
        })}
        <Img
          src={art(frame % 84 >= 78 ? "puppy_3_blink" : "puppy_3")}
          style={{ position: "absolute", left: 0.5 * ROOM_W - 0.125 * ROOM_W, top: 0.63 * ROOM_H - 0.125 * ROOM_W, width: 0.25 * ROOM_W, zIndex: 2 }}
        />
      </div>

      {/* 배운 말 카드 */}
      <div style={{ position: "absolute", left: 1080, top: 232, width: 740, textAlign: "center", opacity: pop(frame, CARDS_AT - 6, false) }}>
        <Pill size={36}>꿈 구슬 도감 · 배운 단어가 카드로</Pill>
      </div>
      {WORDS.map((word, i) => {
        const at = CARDS_AT + i * 5;
        if (frame < at) return null;
        const k = pop(frame, at);
        const gold = word.met >= 4;
        const col = i % 2;
        const row = Math.floor(i / 2);
        const tilt = (random(`tilt${i}`) - 0.5) * 5;
        return (
          <div
            key={word.en}
            style={{
              position: "absolute",
              left: 1080 + col * 380,
              top: 318 + row * 150 - (1 - k) * 260,
              width: 360,
              height: 132,
              borderRadius: 30,
              background: gold ? "linear-gradient(160deg, #FFE9A8 0%, #FBBF24 100%)" : "#FFFFFF",
              opacity: Math.min(1, k * 1.5),
              transform: `rotate(${tilt * (2 - k)}deg)`,
              boxShadow: "0 12px 30px rgba(8,6,40,0.4)",
              fontFamily: FONT,
              display: "flex",
              alignItems: "center",
              padding: "0 30px",
              boxSizing: "border-box",
              justifyContent: "space-between",
            }}
          >
            <div>
              <div style={{ fontSize: 56, color: gold ? "#7C4A03" : C.blue, lineHeight: 1 }}>{word.en}</div>
              <div style={{ fontSize: 30, color: C.ink, opacity: 0.65, marginTop: 6 }}>{word.ko}</div>
            </div>
            <div style={{ display: "flex", gap: 7 }}>
              {[0, 1, 2, 3].map((d) => (
                <div
                  key={d}
                  style={{
                    width: 20,
                    height: 20,
                    borderRadius: 10,
                    background: d < word.met ? (gold ? "#FFFFFF" : C.yellow) : "rgba(30,27,75,0.14)",
                  }}
                />
              ))}
            </div>
          </div>
        );
      })}
    </AbsoluteFill>
  );
};

/** 장면 5: 간식 → 성장 → 이름 → 집 꾸미기 → 배운 말 카드. */
export const Scene5: React.FC<{ duration: number }> = ({ duration }) => (
  <AbsoluteFill>
    <Header at={4} until={GROW_AT[0] - 6} text="첫 화면에서, " hi="말로 간식을 줘요" />
    <Header at={GROW_AT[0] - 2} until={NAME_AT - 6} text="간식을 주면 " hi="쑥쑥 자라요" />
    <Header at={NAME_AT + 2} until={HOME_AT - 8} text="이름은 " hi="아이가 지어 줘요" />
    <Header at={HOME_AT + 2} text="자랄수록 늘어나는 " hi="집과 카드" />
    <Phase start={0} end={HOME_AT}>
      <Growth />
    </Phase>
    <Phase start={HOME_AT - 4} end={duration + 30}>
      <Home />
    </Phase>
  </AbsoluteFill>
);

import { Video } from "@remotion/media";
import { AbsoluteFill, Img, Sequence, interpolate, staticFile, useCurrentFrame } from "remotion";
import { Header, Moon, Phase, Pill } from "../parts/Bits";
import { DeviceFrame } from "../parts/DeviceFrame";
import { PuppyOnCloud } from "../parts/Puppy";
import { C, FONT, art, pop, s } from "../theme";
import { STORY } from "../timeline";

/** 장면 안의 구간: 매일 새로 → 상황 세 가지 → 오늘 있었던 일 → 남은 시간 → 내비 안내(시각은 timeline.ts의 STORY). */
const CHIPS_AT = s(STORY.chips);
const NOTE_AT = s(STORY.note);
const ASK_AT = s(STORY.ask);
const COMFORT_AT = s(STORY.comfort);
const FIT_AT = s(STORY.fit);
const NAVI_AT = s(STORY.navi);
const PAUSE_AT = NAVI_AT + s(2.4);
/** 차가 다 돈 때와, 그 뒤 강아지가 지난 일로 받아 주는 때. */
const TURNED_AT = NAVI_AT + s(5.0);
const REPLY_AT = NAVI_AT + s(6.6);

/**
 * 앱이 그날 실제로 쓴 이야기(기기에 저장된 files/stories/날짜.json에서 옮김). 제목의 '콩이'는 강아지 이름 자리에 들어간 것.
 */
const DAYS = [
  { date: "11월 8일", title: "콩이와 반딧불 숲", lines: ["반딧불이들이 길을 비췄어요.", "빛이 물결처럼 흔들렸어요."], color: C.blue },
  { date: "11월 9일", title: "달빛섬 가는 콩이", lines: ["눈 내리는 구름 마을이 보였어요.", "눈사람이 모자에서 별을 꺼냈어요."], color: C.pink },
  { date: "11월 10일", title: "달빛섬 가는 콩이", lines: ["하늘에 반짝이는 별똥별 조각이 떨어졌어요.", "조각들이 반짝이며 길을 알리네요."], color: "#0EA5A4" },
];

/** 글자가 써지듯 한 글자씩 나타납니다. */
export const Typed: React.FC<{ text: string; from: number; perChar?: number }> = ({ text, from, perChar = 1.3 }) => {
  const frame = useCurrentFrame();
  const shown = Math.max(0, Math.min(text.length, Math.floor((frame - from) / perChar)));
  const writing = frame >= from && shown < text.length;
  return (
    <span>
      {text.slice(0, shown)}
      {writing ? <span style={{ opacity: frame % 8 < 4 ? 1 : 0.2 }}>▍</span> : null}
      <span style={{ opacity: 0 }}>{text.slice(shown)}</span>
    </span>
  );
};

/** 6-1: 날마다 다른 이야기(실제로 생성된 세 편). */
const Daily: React.FC = () => {
  const frame = useCurrentFrame();
  return (
    <AbsoluteFill>
      {DAYS.map((d, i) => {
        const at = 10 + i * 16;
        if (frame < at) return null;
        const k = pop(frame, at);
        const turn = interpolate(frame, [at, at + 12], [70, 0], { extrapolateRight: "clamp" });
        const write = at + 16;
        return (
          <div key={d.date} style={{ position: "absolute", left: 70 + i * 600, top: 280, width: 580, height: 480, perspective: 1200 }}>
            <div
              style={{
                width: "100%",
                height: "100%",
                borderRadius: 44,
                background: "#FFFFFF",
                overflow: "hidden",
                transform: `scale(${k}) rotateY(${turn}deg) rotate(${(i - 1) * 1.4}deg)`,
                boxShadow: "0 24px 60px rgba(8,6,40,0.5)",
                fontFamily: FONT,
              }}
            >
              <div style={{ background: d.color, color: "#FFFFFF", fontSize: 44, padding: "20px 36px 14px", display: "flex", justifyContent: "space-between" }}>
                <span>{d.date}</span>
                <span style={{ opacity: 0.8, fontSize: 32, paddingTop: 8 }}>오늘의 이야기</span>
              </div>
              <div style={{ padding: "30px 36px 0" }}>
                <div style={{ fontSize: 56, color: C.ink, whiteSpace: "nowrap" }}>
                  <Typed text={`「${d.title}」`} from={write} />
                </div>
                <div style={{ fontSize: 41, color: C.ink, opacity: 0.78, lineHeight: 1.4, marginTop: 22, wordBreak: "keep-all" }}>
                  <Typed text={d.lines[0]} from={write + 16} />
                  <br />
                  <Typed text={d.lines[1]} from={write + 16 + d.lines[0].length * 1.3 + 4} />
                </div>
              </div>
            </div>
          </div>
        );
      })}
      <div style={{ position: "absolute", left: 0, top: 800, width: 1920, textAlign: "center", opacity: pop(frame, 60, false) }}>
        <Pill size={38}>앱이 그날 실제로 쓴 이야기 · 생성형 AI</Pill>
      </div>
    </AbsoluteFill>
  );
};

const SITUATIONS = [
  { label: "오늘 있었던 일", sub: "부모가 적어 둔 메모", color: C.pink },
  { label: "남은 시간", sub: "목적지까지 몇 분", color: C.yellow },
  { label: "내비 안내", sub: "방금 들린 길 안내", color: C.green },
];

/** 6-2: 이야기를 바꾸는 그날의 상황 세 가지. */
const Situations: React.FC = () => {
  const frame = useCurrentFrame() - CHIPS_AT;
  return (
    <AbsoluteFill style={{ fontFamily: FONT }}>
      {SITUATIONS.map((sit, i) => {
        const at = 8 + i * 9;
        const k = frame >= at ? pop(frame, at) : 0;
        return (
          <div
            key={sit.label}
            style={{
              position: "absolute",
              left: 150 + i * 560,
              top: 310,
              width: 500,
              height: 250,
              borderRadius: 48,
              background: "rgba(10,9,32,0.5)",
              border: `6px solid ${sit.color}`,
              boxSizing: "border-box",
              transform: `scale(${k})`,
              textAlign: "center",
              color: "#FFFFFF",
            }}
          >
            <div style={{ fontSize: 62, marginTop: 58, color: sit.color }}>{sit.label}</div>
            <div style={{ fontSize: 36, marginTop: 12, opacity: 0.8 }}>{sit.sub}</div>
          </div>
        );
      })}
      <svg width={80} height={110} viewBox="0 0 80 110" style={{ position: "absolute", left: 920, top: 600, opacity: pop(frame, 36, false) }}>
        <path d="M40 4 L40 84 M10 58 L40 100 L70 58" fill="none" stroke="#FFFFFF" strokeWidth={12} strokeLinecap="round" strokeLinejoin="round" />
      </svg>
      <div style={{ position: "absolute", left: 0, top: 740, width: 1920, textAlign: "center", transform: `scale(${frame >= 40 ? pop(frame, 40) : 0})` }}>
        <Pill size={56} bg="#FFFFFF" color={C.ink}>
          오늘의 이야기
        </Pill>
      </div>
    </AbsoluteFill>
  );
};

const MEMO = "유치원에서 블록 쌓다가 넘어져서 무릎을 다쳤어";

/** 6-3: 부모가 적어 둔 오늘 있었던 일을 강아지가 먼저 물어봅니다(오른쪽은 실제 앱 녹화). */
const Note: React.FC = () => {
  const frame = useCurrentFrame();
  const local = frame - NOTE_AT;
  const asked = frame >= ASK_AT;
  const comforted = frame >= COMFORT_AT;
  const deviceIn = pop(local, 4, false);
  const ring = interpolate(local, [14, 26], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  return (
    <AbsoluteFill style={{ fontFamily: FONT }}>
      {/* 부모가 적은 메모 */}
      <div style={{ position: "absolute", left: 80, top: 250, width: 800, transform: `scale(${pop(local, 6)})`, transformOrigin: "0 0" }}>
        <div style={{ fontSize: 32, color: "rgba(255,255,255,0.7)", marginBottom: 10 }}>① 부모가 설정에 적어 두면</div>
        <div style={{ background: "#FFFFFF", borderRadius: 36, padding: "26px 34px 28px", boxShadow: "0 16px 40px rgba(8,6,40,0.45)" }}>
          <div style={{ fontSize: 34, color: C.ink, opacity: 0.55 }}>오늘 있었던 일</div>
          <div style={{ fontSize: 46, color: C.ink, lineHeight: 1.3, marginTop: 8, wordBreak: "keep-all" }}>
            <Typed text={MEMO} from={NOTE_AT + 14} perChar={1.6} />
          </div>
        </div>
      </div>

      {/* 강아지가 이야기 속에서 */}
      {asked ? (
        <div style={{ position: "absolute", left: 80, top: 560, width: 800, transform: `scale(${pop(frame, ASK_AT)})`, transformOrigin: "0 0" }}>
          <div style={{ fontSize: 32, color: "rgba(255,255,255,0.7)", marginBottom: 10 }}>② 강아지가 이야기 속에서 먼저 물어요</div>
          <div
            style={{
              background: C.yellow,
              color: C.ink,
              fontSize: 48,
              lineHeight: 1.3,
              borderRadius: 36,
              padding: "24px 34px 20px",
              wordBreak: "keep-all",
              boxShadow: "0 8px 0 rgba(0,0,0,0.2)",
            }}
          >
            “오늘 넘어져서 무릎을 다쳤다며? <span style={{ color: "#BE185D" }}>어땠어?</span>”
          </div>
          {comforted ? (
            <div
              style={{
                marginTop: 18,
                background: "#FFFFFF",
                color: C.ink,
                fontSize: 44,
                borderRadius: 36,
                padding: "20px 34px 16px",
                transform: `scale(${pop(frame, COMFORT_AT)})`,
                transformOrigin: "0 0",
                wordBreak: "keep-all",
              }}
            >
              “아프면 내 품에 기대도 돼. 내가 꼭 지켜 줄게.”
            </div>
          ) : (
            <div style={{ marginTop: 22, opacity: frame >= ASK_AT + s(STORY.askLen) ? 1 : 0 }}>
              <Pill size={36} bg="rgba(10,9,32,0.55)">아이가 대답할 틈 · 5초 기다림</Pill>
            </div>
          )}
        </div>
      ) : null}

      {/* 실제 앱: 설정 화면 → 이야기 */}
      <div style={{ position: "absolute", left: 960 + (1 - deviceIn) * 1100, top: 240 }}>
        <DeviceFrame width={880} ratio={1682 / 1210}>
          {asked ? (
            <>
              <Sequence from={ASK_AT} durationInFrames={COMFORT_AT - ASK_AT} layout="none">
                <Video src={staticFile("demo/clips/note_story.mp4")} trimBefore={Math.round(STORY.clipAsk * 30)} muted style={{ width: "100%", height: "100%", objectFit: "cover" }} />
              </Sequence>
              <Sequence from={COMFORT_AT} layout="none">
                <Video src={staticFile("demo/clips/note_story.mp4")} trimBefore={Math.round(STORY.clipComfort * 30)} muted style={{ width: "100%", height: "100%", objectFit: "cover" }} />
              </Sequence>
            </>
          ) : (
            <div style={{ position: "relative", width: "100%", height: "100%" }}>
              <Img src={art("settings_full")} style={{ width: "100%", height: "100%" }} />
              {/* 설정 화면의 '오늘 있었던 일' 카드(앱 화면 1682×1210 기준 42,428 ~ 1642,758) */}
              <div
                style={{
                  position: "absolute",
                  left: `${(30 / 1682) * 100}%`,
                  top: `${(418 / 1210) * 100}%`,
                  width: `${(1622 / 1682) * 100}%`,
                  height: `${(350 / 1210) * 100}%`,
                  border: `7px solid ${C.pink}`,
                  borderRadius: 26,
                  boxSizing: "border-box",
                  opacity: ring,
                  transform: `scale(${1.08 - ring * 0.08})`,
                }}
              />
            </div>
          )}
        </DeviceFrame>
        <div style={{ textAlign: "center", fontSize: 28, color: "rgba(255,255,255,0.65)", marginTop: 12 }}>
          실제 앱 화면 · {asked ? "메모를 넣어 새로 쓴 오늘의 이야기" : "설정의 ‘오늘 있었던 일’"}
        </div>
      </div>
    </AbsoluteFill>
  );
};

const CarIcon: React.FC = () => (
  <svg width={150} height={70} viewBox="0 0 520 230">
    <path d="M118 112 L168 44 Q174 36 186 36 L344 36 Q358 36 366 46 L414 112 Z" fill="#F59E0B" />
    <rect x={10} y={104} width={500} height={80} rx={34} fill={C.yellow} />
    <path d="M150 104 L186 54 L250 54 L250 104 Z" fill="#BFE8FF" />
    <path d="M264 54 L338 54 L384 104 L264 104 Z" fill="#BFE8FF" />
    <circle cx={130} cy={184} r={40} fill="#1B1A3F" />
    <circle cx={400} cy={184} r={40} fill="#1B1A3F" />
  </svg>
);

/** 6-2: 남은 시간에 꼭 맞는 이야기 길이. */
const Fit: React.FC = () => {
  const frame = useCurrentFrame() - FIT_AT;
  const L = 240;
  const R = 1680;
  const grow = interpolate(frame, [14, 62], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  const minutes = Math.max(1, Math.round(grow * 12));
  const routeIn = pop(frame, 4, false);
  const chips = [
    { word: "Left", at: 92, x: 0.24 },
    { word: "Rain", at: 100, x: 0.5 },
    { word: "Moon", at: 108, x: 0.76 },
  ];
  return (
    <AbsoluteFill style={{ fontFamily: FONT }}>
      {/* 길 */}
      <div style={{ position: "absolute", left: L, top: 400, width: R - L, borderTop: "8px dashed rgba(255,255,255,0.45)", opacity: routeIn }} />
      <div style={{ position: "absolute", left: L - 150, top: 345, opacity: routeIn }}>
        <CarIcon />
      </div>
      <Moon x={R + 60} y={400} r={56} opacity={routeIn} />
      <div style={{ position: "absolute", left: 0, top: 286, width: 1920, textAlign: "center", opacity: routeIn }}>
        <Pill size={44} bg="rgba(10,9,32,0.5)">
          목적지까지 <span style={{ color: C.yellow }}>12분</span>
        </Pill>
      </div>

      {/* 이야기 막대 */}
      <div style={{ position: "absolute", left: L, top: 610, width: R - L, height: 96, borderRadius: 48, background: "rgba(255,255,255,0.16)" }} />
      <div
        style={{
          position: "absolute",
          left: L,
          top: 610,
          width: Math.max(96, (R - L) * grow),
          height: 96,
          borderRadius: 48,
          background: `linear-gradient(90deg, ${C.pink} 0%, ${C.yellow} 100%)`,
          opacity: frame >= 12 ? 1 : 0,
        }}
      />
      <div style={{ position: "absolute", left: L, top: 610, width: R - L, height: 96, lineHeight: "100px", textAlign: "center", fontSize: 46, color: "#FFFFFF", opacity: frame >= 12 ? 1 : 0, textShadow: "0 3px 0 rgba(23,19,95,0.4)" }}>
        오늘의 이야기 {minutes}분
      </div>
      {/* 도착과 엔딩이 만나는 선 */}
      <div
        style={{
          position: "absolute",
          left: R - 4,
          top: 420,
          height: 300,
          borderLeft: `8px dotted ${C.yellow}`,
          opacity: pop(frame, 64, false),
        }}
      />
      <div style={{ position: "absolute", left: R - 330, top: 740, width: 420, textAlign: "center", transform: `scale(${pop(frame, 66)})` }}>
        <Pill size={40} bg={C.yellow} color={C.ink}>
          도착할 때 엔딩
        </Pill>
      </div>
      {chips.map((chip) => {
        if (frame < chip.at) return null;
        const k = pop(frame, chip.at);
        return (
          <div key={chip.word} style={{ position: "absolute", left: L + (R - L) * chip.x - 90, top: 520 - (1 - k) * 120, width: 180, textAlign: "center", opacity: Math.min(1, k * 2) }}>
            <Pill size={44} bg="#FFFFFF" color={C.blue} style={{ boxShadow: "0 8px 20px rgba(8,6,40,0.4)" }}>
              {chip.word}
            </Pill>
          </div>
        );
      })}
    </AbsoluteFill>
  );
};

const Bars: React.FC<{ on: boolean; color: string }> = ({ on, color }) => {
  const frame = useCurrentFrame();
  return (
    <div style={{ display: "flex", gap: 8, alignItems: "center", height: 60 }}>
      {[0, 1, 2, 3, 4].map((i) => (
        <div key={i} style={{ width: 10, borderRadius: 5, background: color, height: on ? 16 + 40 * Math.abs(Math.sin(frame / 3 + i * 1.3)) : 10, opacity: on ? 1 : 0.4 }} />
      ))}
    </div>
  );
};

/** 6-3: 내비가 말하고 차가 도는 동안은 기다렸다가, 다 돈 뒤에 이야기 속 말로 받아 줍니다. */
const Navi: React.FC = () => {
  const frame = useCurrentFrame();
  const naviIn = pop(frame, NAVI_AT + 6);
  const naviTalking = frame >= NAVI_AT + 10 && frame < PAUSE_AT + 20;
  const paused = frame >= NAVI_AT + 10 && frame < REPLY_AT;
  const reply = frame >= REPLY_AT ? pop(frame, REPLY_AT) : 0;
  return (
    <AbsoluteFill style={{ fontFamily: FONT }}>
      {/* 내비 */}
      <div
        style={{
          position: "absolute",
          left: 130,
          top: 320,
          width: 640,
          height: 330,
          borderRadius: 44,
          background: "linear-gradient(160deg, #23223A 0%, #0B0B14 80%)",
          border: "4px solid rgba(255,255,255,0.18)",
          transform: `scale(${naviIn})`,
          boxSizing: "border-box",
          padding: "36px 44px",
          color: "#FFFFFF",
          boxShadow: "0 24px 60px rgba(8,6,40,0.5)",
        }}
      >
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <div style={{ fontSize: 34, opacity: 0.6 }}>내비게이션</div>
          <Bars on={naviTalking} color={C.green} />
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: 30, marginTop: 34 }}>
          <svg width={130} height={130} viewBox="0 0 130 130" style={{ transform: "scaleX(-1)" }}>
            <path d="M34 118 L34 62 Q34 44 52 44 L84 44 L84 20 L122 56 L84 92 L84 68 L58 68 L58 118 Z" fill={frame >= TURNED_AT ? "#9CA3AF" : C.green} />
          </svg>
          <div>
            <div style={{ fontSize: 76, lineHeight: 1 }}>{frame >= TURNED_AT ? "✓" : "300m"}</div>
            <div style={{ fontSize: 46, marginTop: 10 }}>{frame >= TURNED_AT ? "좌회전 완료" : "앞에서 좌회전"}</div>
          </div>
        </div>
      </div>

      {/* 강아지 */}
      <div style={{ position: "absolute", left: 1010, top: 420, opacity: pop(frame, NAVI_AT + 2, false) }}>
        <PuppyOnCloud stage={2} size={330} happy={frame >= REPLY_AT} />
      </div>
      {paused ? (
        <div style={{ position: "absolute", left: 1360, top: 470, transform: `scale(${pop(frame, NAVI_AT + 12)})`, transformOrigin: "0 50%" }}>
          <Pill size={44} bg="rgba(10,9,32,0.55)">
            <span style={{ letterSpacing: 4 }}>❙❙</span> {frame >= PAUSE_AT + 20 ? "도는 동안 조용히" : "잠깐 멈춤"}
          </Pill>
        </div>
      ) : null}
      {frame >= REPLY_AT ? (
        <div style={{ position: "absolute", left: 900, top: 226, width: 900, transform: `scale(${reply})`, transformOrigin: "40% 100%" }}>
          <div
            style={{
              background: "#FFFFFF",
              color: C.ink,
              fontSize: 54,
              lineHeight: 1.25,
              borderRadius: 44,
              padding: "30px 44px 24px",
              textAlign: "center",
              boxShadow: "0 10px 0 rgba(0,0,0,0.2)",
            }}
          >
            길 안내 요정이 알려 준 대로 구름이 <span style={{ color: C.pink }}>왼쪽</span>으로 돌았어요!
          </div>
          <svg width={60} height={50} viewBox="0 0 60 50" style={{ position: "absolute", left: 300, bottom: -44 }}>
            <path d="M6 0 L54 0 L20 48 Z" fill="#FFFFFF" />
          </svg>
        </div>
      ) : null}
    </AbsoluteFill>
  );
};

/** 장면 6: 매일 새로 쓰는 이야기가 그날의 상황(오늘 있었던 일, 남은 시간, 내비 안내)에 맞춰 달라집니다. */
export const Scene6: React.FC<{ duration: number }> = ({ duration }) => (
  <AbsoluteFill>
    <Header at={2} until={CHIPS_AT - 8} text="이야기는 " hi="매일 새로 씁니다" />
    <Header at={CHIPS_AT + 2} until={NOTE_AT - 8} text="그리고 " hi="그날의 상황에 맞춰" />
    <Header at={NOTE_AT + 2} until={FIT_AT - 8} text="오늘 있었던 일이 " hi="이야기 속으로" />
    <Header at={FIT_AT + 2} until={NAVI_AT - 8} text="남은 시간에 " hi="꼭 맞게" />
    <Header at={NAVI_AT + 2} text="다 돌고 나면, " hi="이야기가 받아 줘요" />
    <Phase start={0} end={CHIPS_AT}>
      <Daily />
    </Phase>
    <Phase start={CHIPS_AT - 4} end={NOTE_AT}>
      <Situations />
    </Phase>
    <Phase start={NOTE_AT - 4} end={FIT_AT}>
      <Note />
    </Phase>
    <Phase start={FIT_AT - 4} end={NAVI_AT}>
      <Fit />
    </Phase>
    <Phase start={NAVI_AT - 4} end={duration + 30}>
      <Navi />
    </Phase>
  </AbsoluteFill>
);

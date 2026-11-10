import { Video } from "@remotion/media";
import { AbsoluteFill, interpolate, staticFile, useCurrentFrame } from "remotion";
import { DeviceFrame } from "../parts/DeviceFrame";
import { Arrow, BeltIcon, BrakeIcon, WiperIcon } from "../parts/Icons";
import { C, FONT, WIDTH, pop, s } from "../theme";

/** 녹화한 앱 화면에서 강아지가 기울기 시작하는 때(초). 이때가 장면의 SIGNAL 시각과 겹치게 자릅니다. */
const CLIP_TILT = 19.65;
const SIGNAL = s(2.1);
const SIGNAL_OFF = s(7.15);
const HERO_OUT = s(8.4);
const BLINK = 20; // 프레임. 켜짐 10 + 꺼짐 10

const Label: React.FC<{ children: React.ReactNode; left: number; width: number; at: number }> = ({ children, left, width, at }) => {
  const frame = useCurrentFrame();
  const k = pop(frame, at, false);
  return (
    <div
      style={{
        position: "absolute",
        left,
        top: 92,
        width,
        textAlign: "center",
        fontFamily: FONT,
        fontSize: 36,
        color: "rgba(255,255,255,0.86)",
        opacity: k,
        transform: `translateY(${(1 - k) * 16}px)`,
      }}
    >
      {children}
    </div>
  );
};

/** 앞부분: 방향지시등(그림) → 실제 앱 화면. */
const Hero: React.FC = () => {
  const frame = useCurrentFrame();
  const enter = pop(frame, 2, false);
  const gauge = pop(frame, 8);
  const exit = interpolate(frame, [HERO_OUT, HERO_OUT + 12], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  const signalOn = frame >= SIGNAL && frame < SIGNAL_OFF;
  const lit = signalOn && (frame - SIGNAL) % BLINK < BLINK / 2;
  const pulse = signalOn ? ((frame - SIGNAL) % BLINK) / BLINK : 1;
  const badge = pop(frame, SIGNAL + 3);
  const ko = pop(frame, SIGNAL + 6);
  const en = pop(frame, SIGNAL + 30);
  const deviceWidth = 1020;
  return (
    <AbsoluteFill style={{ opacity: 1 - exit }}>
      <Label left={90} width={480} at={10}>
        ① 운전자가 방향지시등을 켜면
      </Label>
      <Label left={820} width={deviceWidth} at={16}>
        ② 앱이 바로 그 말을 가르칩니다 <span style={{ opacity: 0.6 }}>· 실제 앱 화면</span>
      </Label>

      {/* 계기판 */}
      <div style={{ position: "absolute", left: 140 - exit * 500, top: 200, width: 380, height: 380, transform: `scale(${gauge})` }}>
        <div
          style={{
            position: "absolute",
            inset: 0,
            borderRadius: "50%",
            background: "radial-gradient(circle at 40% 30%, #2B2880 0%, #121040 75%)",
            border: "6px solid rgba(255,255,255,0.28)",
            boxShadow: lit ? `0 0 70px ${C.green}66` : "0 20px 50px rgba(8,6,40,0.5)",
          }}
        />
        {signalOn ? (
          <div
            style={{
              position: "absolute",
              inset: -pulse * 46,
              borderRadius: "50%",
              border: `5px solid ${C.green}`,
              opacity: (1 - pulse) * 0.7,
            }}
          />
        ) : null}
        <div style={{ position: "absolute", left: 26, top: 125 }}>
          <Arrow direction="left" color={lit ? C.green : "#3F3C92"} glow={lit} />
        </div>
        <div style={{ position: "absolute", left: 204, top: 125 }}>
          <Arrow direction="right" color="#3F3C92" />
        </div>
      </div>

      {/* 신호가 앱으로 넘어가는 점선 */}
      <div style={{ position: "absolute", left: 548, top: 376, width: 250, display: "flex", justifyContent: "space-between", opacity: (1 - exit) * gauge }}>
        {Array.from({ length: 7 }, (_, i) => {
          const on = signalOn && ((frame - SIGNAL) * 1.4 - i + 70) % 10 < 4;
          return <div key={i} style={{ width: 18, height: 18, borderRadius: 9, background: on ? C.green : "rgba(255,255,255,0.25)" }} />;
        })}
      </div>
      <div
        style={{
          position: "absolute",
          left: 673,
          top: 280,
          transform: `translate(-50%, 0) scale(${badge}) rotate(-4deg)`,
          fontFamily: FONT,
          fontSize: 44,
          color: C.ink,
          background: C.yellow,
          padding: "8px 22px 6px",
          borderRadius: 26,
          whiteSpace: "nowrap",
          boxShadow: "0 6px 0 rgba(0,0,0,0.22)",
        }}
      >
        0.03초
      </div>

      {/* 앱이 하는 말 */}
      <div style={{ position: "absolute", left: 90 - exit * 500, top: 640, width: 480, textAlign: "center", fontFamily: FONT, lineHeight: 1.05 }}>
        <div style={{ fontSize: 70, color: "#FFFFFF", transform: `scale(${ko})`, opacity: Math.min(1, ko * 2) }}>“왼쪽!”</div>
        <div style={{ fontSize: 150, color: C.yellow, transform: `scale(${en}) rotate(-3deg)`, opacity: Math.min(1, en * 2), textShadow: "0 8px 0 rgba(23,19,95,0.55)" }}>
          Left!
        </div>
      </div>

      {/* 실제 앱 화면 */}
      <div style={{ position: "absolute", left: 820 + (1 - enter) * 1200 + exit * 1200, top: 160 }}>
        <DeviceFrame width={deviceWidth} ratio={1682 / 1210}>
          <Video
            src={staticFile("demo/clips/hero.mp4")}
            trimBefore={Math.round(CLIP_TILT * 30) - SIGNAL}
            muted
            style={{ width: "100%", height: "100%", objectFit: "cover" }}
          />
        </DeviceFrame>
      </div>
    </AbsoluteFill>
  );
};

type CardSpec = { cue: string; word: string; meaning: string; color: string; icon: (frame: number) => React.ReactNode };

const CARDS: CardSpec[] = [
  { cue: "와이퍼를 켜면", word: "Rain", meaning: "비", color: C.blue, icon: (f) => <WiperIcon swing={Math.sin(f / 4)} /> },
  { cue: "브레이크를 밟으면", word: "Stop", meaning: "멈춰", color: C.pink, icon: () => <BrakeIcon /> },
  { cue: "안전벨트를 매면", word: "Seatbelt", meaning: "안전벨트", color: "#D97706", icon: () => <BeltIcon /> },
];
const CARD_W = 440;
const CARD_H = 470;

const Card: React.FC<{ spec: CardSpec; at: number; cx: number }> = ({ spec, at, cx }) => {
  const frame = useCurrentFrame();
  if (frame < at) return null;
  const k = pop(frame, at);
  const flip = interpolate(frame, [at + 20, at + 32], [0, 180], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  const front = flip < 90;
  const word = pop(frame, at + 26);
  return (
    <div style={{ position: "absolute", left: cx - CARD_W / 2, top: 320, width: CARD_W, height: CARD_H, perspective: 1400 }}>
      <div
        style={{
          width: "100%",
          height: "100%",
          borderRadius: 48,
          transform: `scale(${k}) rotateY(${front ? flip : flip - 180}deg)`,
          background: front ? "linear-gradient(160deg, #332FA0 0%, #1D1A66 100%)" : "#FFFFFF",
          border: front ? "5px solid rgba(255,255,255,0.3)" : "5px solid #FFFFFF",
          boxShadow: "0 24px 60px rgba(8,6,40,0.5)",
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
          fontFamily: FONT,
          boxSizing: "border-box",
        }}
      >
        {front ? (
          <>
            {spec.icon(frame - at)}
            <div style={{ fontSize: 44, color: "#FFFFFF", marginTop: 34 }}>{spec.cue}</div>
          </>
        ) : (
          <>
            <div style={{ fontSize: 36, color: C.ink, opacity: 0.55, marginBottom: 26 }}>{spec.cue}</div>
            <div style={{ fontSize: spec.word.length > 6 ? 90 : 104, color: spec.color, transform: `scale(${word})` }}>{spec.word}</div>
            <div style={{ fontSize: 46, color: C.ink, opacity: 0.7, marginTop: 18 }}>{spec.meaning}</div>
          </>
        )}
      </div>
    </div>
  );
};

/** 뒷부분: 다른 차량 신호들도 영어가 됩니다. */
const Montage: React.FC<{ from: number }> = ({ from }) => {
  const frame = useCurrentFrame();
  if (frame < from) return null;
  const head = pop(frame, from, false);
  return (
    <AbsoluteFill>
      <div
        style={{
          position: "absolute",
          top: 150,
          width: WIDTH,
          textAlign: "center",
          fontFamily: FONT,
          fontSize: 68,
          color: "#FFFFFF",
          opacity: head,
          transform: `translateY(${(1 - head) * 24}px)`,
        }}
      >
        차가 움직일 때마다, <span style={{ color: C.yellow }}>그 순간의 영어</span>
      </div>
      {CARDS.map((spec, i) => (
        <Card key={spec.word} spec={spec} at={from + 6 + i * s(1.4)} cx={400 + i * 560} />
      ))}
    </AbsoluteFill>
  );
};

/** 장면 3(핵심): 차량 신호가 켜지는 바로 그때 영어를 가르칩니다. */
export const Scene3: React.FC = () => (
  <AbsoluteFill>
    <Hero />
    <Montage from={s(8.7)} />
  </AbsoluteFill>
);

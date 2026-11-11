import { Video } from "@remotion/media";
import { AbsoluteFill, interpolate, staticFile, useCurrentFrame } from "remotion";
import { Header, Pill } from "../parts/Bits";
import { DeviceFrame } from "../parts/DeviceFrame";
import { Arrow } from "../parts/Icons";
import { PuppyOnCloud } from "../parts/Puppy";
import { C, FONT, pop, s } from "../theme";
import { TURN } from "../timeline";

/** 녹화한 앱 화면(clips/turn.mp4)에서 깜빡이를 켠 때(초). 이때가 장면의 SIGNAL 시각과 겹치게 자릅니다. */
const CLIP_ON = 3.03;
const SIGNAL = s(TURN.on);
const SIGNAL_OFF = s(TURN.off);
const HERO_OUT = s(TURN.recallAt - 0.7);
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
        ① 깜빡이를 켜고 도는 동안
      </Label>
      {frame < SIGNAL_OFF + 6 ? (
        <Label left={820} width={deviceWidth} at={16}>
          ② 말없이 화면 속 구름만 기울어요 <span style={{ opacity: 0.6 }}>· 실제 앱 화면</span>
        </Label>
      ) : (
        <Label left={820} width={deviceWidth} at={SIGNAL_OFF + 6}>
          ③ 다 돌고 나면, 이야기로 한마디 <span style={{ opacity: 0.6 }}>· 실제 앱 화면</span>
        </Label>
      )}

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
        {frame < SIGNAL_OFF ? "조용히" : "다 돌았어요"}
      </div>

      {/* 운전자에게 하는 약속 */}
      <div style={{ position: "absolute", left: 90 - exit * 500, top: 650, width: 480, textAlign: "center", fontFamily: FONT, lineHeight: 1.2 }}>
        <div style={{ fontSize: 46, color: "#FFFFFF", transform: `scale(${ko})`, opacity: Math.min(1, ko * 2) }}>
          {frame < SIGNAL_OFF ? "운전에 집중할 때는" : "운전이 끝난 뒤에"}
        </div>
        <div style={{ fontSize: 64, color: C.yellow, transform: `scale(${frame < SIGNAL_OFF ? en : pop(frame, SIGNAL_OFF + 4)})`, opacity: Math.min(1, en * 2) }}>
          {frame < SIGNAL_OFF ? "말하지 않아요" : "이야기가 받아 줘요"}
        </div>
      </div>

      {/* 실제 앱 화면 */}
      <div style={{ position: "absolute", left: 820 + (1 - enter) * 1200 + exit * 1200, top: 160 }}>
        <DeviceFrame width={deviceWidth} ratio={1682 / 1210}>
          <Video
            src={staticFile("demo/clips/turn.mp4")}
            trimBefore={Math.round(CLIP_ON * 30) - SIGNAL}
            muted
            style={{ width: "100%", height: "100%", objectFit: "cover" }}
          />
        </DeviceFrame>
      </div>
    </AbsoluteFill>
  );
};

/** 뒷부분: 그 움직임의 영어는 다음 장 사이 수업에서, 아까 그 순간을 떠올리며 배웁니다. */
const Recall: React.FC = () => {
  const frame = useCurrentFrame();
  const at = s(TURN.recallAt);
  if (frame < at - 4) return null;
  const q = s(TURN.recall);
  const m = s(TURN.meaning);
  const e = s(TURN.english);
  const line = frame >= m ? "‘왼쪽’. 영어로는," : "아까 구름이 왼쪽으로 기울었지?";
  const bubble = frame >= q ? pop(frame, frame >= m ? m : q) : 0;
  return (
    <AbsoluteFill style={{ fontFamily: FONT }}>
      <Header at={at} text="영어는 다음 수업에서, " hi="아까 그 순간을 떠올리며" />
      <div style={{ position: "absolute", left: 300, top: 470, opacity: pop(frame, at, false), transform: `rotate(${frame >= q && frame < m ? -7 : 0}deg)`, transformOrigin: "50% 80%" }}>
        <PuppyOnCloud stage={2} size={420} happy={frame >= e} />
      </div>
      {frame >= q ? (
        <div style={{ position: "absolute", left: 760, top: 330, width: 900, transform: `scale(${bubble})`, transformOrigin: "0% 100%" }}>
          <div style={{ background: "#FFFFFF", color: C.ink, fontSize: 58, lineHeight: 1.25, borderRadius: 44, padding: "30px 44px 24px", textAlign: "center", boxShadow: "0 10px 0 rgba(0,0,0,0.2)" }}>
            {line}
          </div>
        </div>
      ) : null}
      {frame >= e ? (
        <div style={{ position: "absolute", left: 760, top: 560, width: 900, textAlign: "center", transform: `scale(${pop(frame, e)}) rotate(-3deg)` }}>
          <div style={{ fontSize: 170, color: C.yellow, textShadow: "0 8px 0 rgba(23,19,95,0.55)" }}>Left!</div>
        </div>
      ) : null}
      {frame >= q ? (
        <div style={{ position: "absolute", left: 1210, top: 830, transform: `scale(${pop(frame, q + 6)})` }}>
          <Pill size={36} bg="rgba(10,9,32,0.55)">실제 앱의 다음 수업 대사</Pill>
        </div>
      ) : null}
    </AbsoluteFill>
  );
};

/** 장면 3(핵심): 차가 움직이는 동안은 조용히, 다 움직인 뒤에 이야기로, 영어는 다음 수업에서. */
export const Scene3: React.FC = () => (
  <AbsoluteFill>
    <Hero />
    <Recall />
  </AbsoluteFill>
);

import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { Header, Pill } from "../parts/Bits";
import { C, FONT, pop, s } from "../theme";

/** 앱의 복습 일정(README '진도 규칙'): 서로 다른 날에 네 번 만나면 꿈 구슬, 그 뒤에도 2주 뒤에 다시 닦습니다. */
const STEPS = [
  { day: "1일째", what: "새로 배움" },
  { day: "다음 날", what: "다시 묻기" },
  { day: "그다음 날", what: "거꾸로 묻기" },
  { day: "이틀 뒤", what: "마지막 확인" },
];
const XS = [250, 610, 970, 1330, 1690];
const HOPS = [s(0.5), s(1.3), s(2.1), s(2.9), s(3.9)];
const SDK_AT = s(4.7);
const SDKS = ["Vehicle", "TextToSpeech", "SpeechToText", "NaviHelper"];

/** 장면 7: 잊기 전에 다시 묻는 복습 일정과, 쓰인 Pleos SDK. */
export const Scene7: React.FC = () => {
  const frame = useCurrentFrame();
  const reached = HOPS.filter((at) => frame >= at).length;
  const from = Math.max(0, reached - 1);
  const hop = reached === 0 ? 0 : interpolate(frame, [HOPS[from], HOPS[from] + 10], [0, 1], { extrapolateRight: "clamp" });
  const chipX = reached <= 1 ? XS[0] : XS[from - 1] + (XS[from] - XS[from - 1]) * hop;
  const chipY = 300 - (reached > 1 ? Math.sin(hop * Math.PI) * 70 : 0);
  const gold = reached >= 5 && hop >= 1;
  const line = reached <= 1 ? 0 : (chipX - XS[0]) / (XS[4] - XS[0]);
  return (
    <AbsoluteFill style={{ fontFamily: FONT }}>
      <Header at={2} text="배운 단어는 " hi="잊기 전에 다시" />
      <div style={{ position: "absolute", left: XS[0], top: 455, width: XS[4] - XS[0], height: 10, borderRadius: 5, background: "rgba(255,255,255,0.22)" }}>
        <div style={{ width: `${line * 100}%`, height: "100%", borderRadius: 5, background: C.yellow }} />
      </div>
      {STEPS.map((step, i) => {
        const k = pop(frame, 6 + i * 4);
        const on = reached > i;
        return (
          <div key={step.day} style={{ position: "absolute", left: XS[i] - 160, top: 410, width: 320, textAlign: "center", transform: `scale(${k})` }}>
            <div
              style={{
                width: 100,
                height: 100,
                borderRadius: 50,
                margin: "0 auto",
                background: on ? C.yellow : "#4A3FC0",
                border: "6px solid #FFFFFF",
                boxSizing: "border-box",
                color: on ? C.ink : "#FFFFFF",
                fontSize: 52,
                lineHeight: "94px",
              }}
            >
              {i + 1}
            </div>
            <div style={{ fontSize: 46, color: "#FFFFFF", marginTop: 22 }}>{step.day}</div>
            <div style={{ fontSize: 36, color: "#FFFFFF", opacity: 0.7, marginTop: 6 }}>{step.what}</div>
          </div>
        );
      })}
      {/* 꿈 구슬 */}
      <div style={{ position: "absolute", left: XS[4] - 160, top: 395, width: 320, textAlign: "center", transform: `scale(${pop(frame, 22)})` }}>
        <div
          style={{
            width: 130,
            height: 130,
            borderRadius: 65,
            margin: "0 auto",
            background: gold ? "radial-gradient(circle at 35% 30%, #FFFBEA 0%, #FBBF24 70%)" : "radial-gradient(circle at 35% 30%, rgba(255,255,255,0.6) 0%, rgba(255,255,255,0.12) 70%)",
            boxShadow: gold ? `0 0 ${50 + 20 * Math.sin(frame / 5)}px ${C.yellow}` : "none",
          }}
        />
        <div style={{ fontSize: 46, color: gold ? C.yellow : "#FFFFFF", marginTop: 7 }}>꿈 구슬</div>
        <div style={{ fontSize: 36, color: "#FFFFFF", opacity: 0.7, marginTop: 6 }}>2주 뒤 다시 닦기</div>
      </div>
      {/* 낱말 카드 */}
      {reached > 0 ? (
        <div style={{ position: "absolute", left: chipX - 110, top: chipY, width: 220, textAlign: "center", transform: `scale(${pop(frame, HOPS[0])})` }}>
          <Pill size={56} bg={gold ? C.yellow : "#FFFFFF"} color={gold ? "#7C4A03" : C.blue} style={{ boxShadow: "0 10px 24px rgba(8,6,40,0.45)" }}>
            Left
          </Pill>
        </div>
      ) : null}

      {/* SDK */}
      <div style={{ position: "absolute", left: 0, top: 730, width: 1920, textAlign: "center", opacity: pop(frame, SDK_AT, false), fontSize: 38, color: "rgba(255,255,255,0.8)" }}>
        Pleos Connect SDK로 차와 이어집니다
      </div>
      <div style={{ position: "absolute", left: 0, top: 800, width: 1920, display: "flex", justifyContent: "center", gap: 26 }}>
        {SDKS.map((name, i) => (
          <div key={name} style={{ transform: `scale(${frame >= SDK_AT + 4 + i * 4 ? pop(frame, SDK_AT + 4 + i * 4) : 0})` }}>
            <Pill size={46} bg="rgba(10,9,32,0.55)" style={{ border: "3px solid rgba(255,255,255,0.3)" }}>
              {name}
            </Pill>
          </div>
        ))}
      </div>
    </AbsoluteFill>
  );
};

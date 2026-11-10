import { Video } from "@remotion/media";
import { AbsoluteFill, Img, interpolate, staticFile, useCurrentFrame } from "remotion";
import { Header, Pill } from "../parts/Bits";
import { DeviceFrame } from "../parts/DeviceFrame";
import { C, FONT, art, pop, s } from "../theme";

/** 녹화한 앱 화면(clips/lesson.mp4) 속 시각(초): 질문, 듣기 시작, 통과, 영어, 간식. */
const CLIP = { ask: 2.37, listen: 4.87, pass: 6.03, treat: 8.9 };
/** 장면 안에서 질문이 나오는 때. 나머지는 녹화와 같은 간격으로 따라옵니다. */
const ASK = s(2.1);
const at = (clipSeconds: number) => ASK + Math.round((clipSeconds - CLIP.ask) * 30);
const LISTEN = at(CLIP.listen);
const PASS = at(CLIP.pass);
const TREAT = at(CLIP.treat);
const CHILD = LISTEN + 9;

const Step: React.FC<{ at: number; top: number; children: React.ReactNode }> = ({ at: from, top, children }) => {
  const frame = useCurrentFrame();
  if (frame < from) return null;
  const k = pop(frame, from);
  return <div style={{ position: "absolute", left: 1110, top, width: 720, transform: `scale(${k})`, transformOrigin: "0 50%" }}>{children}</div>;
};

/** 장면 4: 강아지가 묻고, 아이가 말하면 차가 듣고, 통과하면 간식이 생깁니다. */
export const Scene4: React.FC = () => {
  const frame = useCurrentFrame();
  const enter = pop(frame, 2, false);
  const listening = frame >= LISTEN && frame < PASS;
  const stamp = frame >= PASS ? pop(frame, PASS) : 0;
  return (
    <AbsoluteFill style={{ fontFamily: FONT }}>
      <Header at={4} text="따라 말하면, " hi="차가 들어요" />

      <div style={{ position: "absolute", left: 90 - (1 - enter) * 1100, top: 232 }}>
        <DeviceFrame width={940} ratio={1682 / 1210}>
          <Video
            src={staticFile("demo/clips/lesson.mp4")}
            trimBefore={Math.round(CLIP.ask * 30) - ASK}
            muted
            style={{ width: "100%", height: "100%", objectFit: "cover" }}
          />
        </DeviceFrame>
        <div style={{ textAlign: "center", fontSize: 28, color: "rgba(255,255,255,0.65)", marginTop: 12 }}>
          실제 앱 화면 · 에뮬레이터에 시험용 목소리를 들려준 결과
        </div>
      </div>

      {/* ① 강아지가 묻고 */}
      <Step at={ASK} top={250}>
        <div style={{ fontSize: 32, color: "rgba(255,255,255,0.7)", marginBottom: 10 }}>① 강아지가 물어요</div>
        <div style={{ background: "#FFFFFF", color: C.ink, fontSize: 50, borderRadius: 36, padding: "22px 34px 16px", display: "inline-block" }}>
          ‘구름’. 영어로 뭐였지?
        </div>
      </Step>

      {/* ② 아이가 말하면 차가 듣고 */}
      <Step at={LISTEN} top={440}>
        <div style={{ fontSize: 32, color: "rgba(255,255,255,0.7)", marginBottom: 10 }}>② 아이가 말하면 차가 들어요</div>
        <div style={{ display: "flex", alignItems: "center", gap: 22 }}>
          <div style={{ display: "flex", gap: 7, alignItems: "center", height: 84, padding: "0 26px", borderRadius: 42, background: "rgba(10,9,32,0.55)" }}>
            {[0, 1, 2, 3, 4, 5].map((i) => (
              <div
                key={i}
                style={{
                  width: 10,
                  borderRadius: 5,
                  background: listening ? C.pink : "rgba(255,255,255,0.4)",
                  height: listening && frame >= CHILD ? 16 + 44 * Math.abs(Math.sin(frame / 2.6 + i * 1.1)) : 12,
                }}
              />
            ))}
          </div>
          {frame >= CHILD ? (
            <div
              style={{
                background: C.pink,
                color: "#FFFFFF",
                fontSize: 64,
                borderRadius: 40,
                padding: "16px 40px 10px",
                transform: `scale(${pop(frame, CHILD)}) rotate(-3deg)`,
                boxShadow: "0 8px 0 rgba(0,0,0,0.22)",
              }}
            >
              “Cloud!”
            </div>
          ) : null}
        </div>
      </Step>

      {/* ③ 통과하면 간식 */}
      <Step at={PASS} top={650}>
        <div style={{ fontSize: 32, color: "rgba(255,255,255,0.7)", marginBottom: 10 }}>③ 통과하면 간식이 생겨요</div>
        <div style={{ display: "flex", alignItems: "center", gap: 26 }}>
          <div
            style={{
              fontSize: 64,
              color: C.green,
              border: `8px solid ${C.green}`,
              borderRadius: 28,
              padding: "8px 30px 2px",
              transform: `scale(${interpolate(stamp, [0, 1], [2.2, 1])}) rotate(-7deg)`,
              opacity: Math.min(1, stamp * 2),
            }}
          >
            통과!
          </div>
          {frame >= TREAT ? (
            <div style={{ display: "flex", alignItems: "center", gap: 6, transform: `scale(${pop(frame, TREAT)})` }}>
              <Img src={art("treat")} style={{ width: 150, transform: `rotate(${Math.sin(frame / 8) * 8}deg)` }} />
              <Pill size={54} bg={C.yellow} color={C.ink}>
                간식 +1
              </Pill>
            </div>
          ) : null}
        </div>
      </Step>
    </AbsoluteFill>
  );
};

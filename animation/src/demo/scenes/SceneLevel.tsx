import { Video } from "@remotion/media";
import { AbsoluteFill, Sequence, interpolate, staticFile, useCurrentFrame } from "remotion";
import { Header, Pill } from "../parts/Bits";
import { DeviceFrame } from "../parts/DeviceFrame";
import { C, FONT, pop, s } from "../theme";
import { LEVEL } from "../timeline";

const ASK = s(LEVEL.ask);
const LISTEN = s(LEVEL.listen);
const RIGHT = s(LEVEL.right);
const CHOOSE = s(LEVEL.choose);
const RESULT = s(LEVEL.result);
const DEVICE_W = 900;

/** 앱의 다섯 레벨(Levels.kt). */
const LEVELS = [
  { name: "첫 단어", sample: "Puppy · Apple · Red" },
  { name: "단어 넓히기", sample: "Big · Jump · Happy" },
  { name: "짧은 표현", sample: "Let's fly! · A red dog" },
  { name: "문장", sample: "I see a dog." },
  { name: "묻고 답하기", sample: "Where is the dog?" },
];

/** 녹화한 레벨 테스트에서 Claude가 실제로 고른 문제와 결과. */
const ASKED = [
  { ko: "고양이", ok: true },
  { ko: "바나나", ok: true },
  { ko: "크다", ok: true },
  { ko: "뛰어요", ok: true },
  { ko: "딸기", ok: true },
  { ko: "큰 트럭", ok: false },
  { ko: "이거", ok: false },
];

/**
 * 레벨 테스트: 강아지가 묻고 아이가 영어로 대답하면, AI(Claude)가 대답을 보고 다음 문제를 고르고 레벨을 정합니다.
 * 왼쪽은 실제 앱 녹화(Claude가 출제한 테스트), 오른쪽은 그 흐름을 풀어 보여 줍니다.
 */
export const SceneLevel: React.FC = () => {
  const frame = useCurrentFrame();
  const enter = pop(frame, 2, false);
  const result = frame >= RESULT;
  // 결과가 나오면 레벨 사다리가 1 → 2 → 3으로 올라가 3에서 멈춥니다.
  const climbed = result ? Math.min(3, 1 + Math.floor((frame - RESULT - 10) / 12)) : 0;
  return (
    <AbsoluteFill style={{ fontFamily: FONT }}>
      <Header at={4} until={RESULT - 8} text="먼저, " hi="레벨 테스트로 아이 수준을 알아봐요" />
      <Header at={RESULT + 2} text="다섯 레벨 가운데 " hi="딱 맞는 곳에서 시작해요" />

      <div style={{ position: "absolute", left: 90 - (1 - enter) * 1100, top: 250 }}>
        <DeviceFrame width={DEVICE_W} ratio={1140 / 820}>
          <Sequence durationInFrames={RESULT} layout="none">
            <Video
              src={staticFile("demo/clips/level_q.mp4")}
              trimBefore={Math.round((LEVEL.clipStart - 46.5) * 30)}
              muted
              style={{ width: "100%", height: "100%", objectFit: "cover" }}
            />
          </Sequence>
          <Sequence from={RESULT} layout="none">
            <Video
              src={staticFile("demo/clips/level_r.mp4")}
              trimBefore={Math.round((LEVEL.clipResult - 121) * 30)}
              muted
              style={{ width: "100%", height: "100%", objectFit: "cover" }}
            />
          </Sequence>
        </DeviceFrame>
        <div style={{ textAlign: "center", fontSize: 28, color: "rgba(255,255,255,0.65)", marginTop: 12 }}>
          실제 앱 화면 · AI(Claude)가 출제한 레벨 테스트
        </div>
      </div>

      {/* 뒷자리 아이의 대답 */}
      {frame >= LISTEN && frame < CHOOSE + 20 ? (
        <div style={{ position: "absolute", left: 640, top: 214, transform: `scale(${pop(frame, LISTEN)}) rotate(-3deg)` }}>
          <Pill size={52} bg={C.yellow} color={C.ink}>
            아이: “Banana!”
          </Pill>
        </div>
      ) : null}

      {/* 오른쪽: 묻고 → 대답하고 → AI가 다음 문제를 고름 */}
      {!result ? (
        <div style={{ position: "absolute", left: 1080, top: 262, width: 760 }}>
          <div style={{ transform: `scale(${frame >= ASK - 6 ? pop(frame, ASK - 6) : 0})`, transformOrigin: "0 50%" }}>
            <div style={{ fontSize: 32, color: "rgba(255,255,255,0.7)", marginBottom: 10 }}>① 강아지가 우리말로 물어요</div>
            <div style={{ background: "#FFFFFF", color: C.ink, fontSize: 46, borderRadius: 36, padding: "20px 32px 16px", boxShadow: "0 8px 0 rgba(0,0,0,0.2)" }}>
              “‘바나나’. 영어로 뭐라고 할까?”
            </div>
          </div>
          {frame >= RIGHT ? (
            <div style={{ marginTop: 34, transform: `scale(${pop(frame, RIGHT)})`, transformOrigin: "0 50%" }}>
              <div style={{ fontSize: 32, color: "rgba(255,255,255,0.7)", marginBottom: 10 }}>② 아이가 영어로 대답하면 차가 들어요</div>
              <Pill size={48} bg={C.green} color={C.ink}>
                ✓ 맞아!
              </Pill>
            </div>
          ) : null}
          {frame >= CHOOSE ? (
            <div style={{ marginTop: 34, opacity: pop(frame, CHOOSE, false) }}>
              <div style={{ fontSize: 32, color: "rgba(255,255,255,0.7)", marginBottom: 12 }}>③ AI가 대답을 보고 다음 문제를 골라요</div>
              <div style={{ display: "flex", flexWrap: "wrap", gap: 12 }}>
                {ASKED.map((item, i) => {
                  const at = CHOOSE + 6 + i * 7;
                  if (frame < at) return null;
                  return (
                    <div
                      key={item.ko}
                      style={{
                        transform: `scale(${pop(frame, at)})`,
                        fontSize: 38,
                        color: item.ok ? C.ink : "#FFFFFF",
                        background: item.ok ? "#FFFFFF" : "rgba(255,255,255,0.16)",
                        border: item.ok ? "none" : "3px dashed rgba(255,255,255,0.5)",
                        borderRadius: 40,
                        padding: "10px 24px 8px",
                      }}
                    >
                      {item.ko} <span style={{ color: item.ok ? "#16A34A" : C.pink }}>{item.ok ? "✓" : "?"}</span>
                    </div>
                  );
                })}
              </div>
            </div>
          ) : null}
        </div>
      ) : (
        <div style={{ position: "absolute", left: 1080, top: 250, width: 760 }}>
          {LEVELS.map((level, i) => {
            const at = RESULT + 4 + i * 4;
            const index = i + 1;
            const here = index === 3 && climbed >= 3;
            const passed = index < 3 && climbed >= index;
            const glow = here ? interpolate(Math.sin((frame - RESULT) / 6), [-1, 1], [0.5, 1]) : 0;
            return (
              <div
                key={level.name}
                style={{
                  transform: `scale(${frame >= at ? pop(frame, at) : 0})`,
                  transformOrigin: "0 50%",
                  display: "flex",
                  alignItems: "center",
                  gap: 22,
                  height: 112,
                  marginBottom: 14,
                  padding: "0 30px",
                  borderRadius: 36,
                  background: here ? C.yellow : "rgba(10,9,32,0.5)",
                  boxShadow: here ? `0 0 ${50 * glow}px rgba(251,191,36,0.9)` : "none",
                  color: here ? C.ink : "#FFFFFF",
                  opacity: index > 3 ? 0.6 : 1,
                }}
              >
                <div style={{ fontSize: 50, width: 56 }}>{index}</div>
                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: 42, lineHeight: 1.1 }}>{level.name}</div>
                  <div style={{ fontSize: 26, opacity: 0.7 }}>{level.sample}</div>
                </div>
                {passed ? <div style={{ fontSize: 34, color: C.green }}>✓ 알아요</div> : null}
                {here ? <div style={{ fontSize: 36 }}>여기서 시작!</div> : null}
              </div>
            );
          })}
        </div>
      )}
    </AbsoluteFill>
  );
};

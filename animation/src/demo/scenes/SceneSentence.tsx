import { Video } from "@remotion/media";
import { AbsoluteFill, Img, staticFile, useCurrentFrame } from "remotion";
import { Header, Pill } from "../parts/Bits";
import { DeviceFrame } from "../parts/DeviceFrame";
import { C, FONT, art, pop, s } from "../theme";
import { SENTENCE } from "../timeline";

const ASK = s(SENTENCE.ask);
const PASS = s(SENTENCE.pass);
const CARD2 = s(SENTENCE.card2);

const Card: React.FC<{
  at: number;
  top: number;
  english: React.ReactNode;
  korean: string;
  stage: number;
  tag: string;
  passed?: boolean;
}> = ({ at, top, english, korean, stage, tag, passed }) => {
  const frame = useCurrentFrame();
  if (frame < at) return null;
  return (
    <div
      style={{
        position: "absolute",
        left: 90,
        top,
        width: 830,
        height: 190,
        borderRadius: 40,
        background: "#FFFFFF",
        boxShadow: "0 16px 40px rgba(8,6,40,0.45)",
        transform: `scale(${pop(frame, at)})`,
        transformOrigin: "0 50%",
        display: "flex",
        alignItems: "center",
        padding: "0 34px",
        boxSizing: "border-box",
        gap: 26,
      }}
    >
      <Img src={art(`puppy_${stage}`)} style={{ width: 130, height: 130 }} />
      <div style={{ flex: 1 }}>
        <div style={{ fontSize: 30, color: C.ink, opacity: 0.55 }}>{tag}</div>
        <div style={{ fontSize: 72, color: C.blue, lineHeight: 1.1, whiteSpace: "nowrap" }}>{english}</div>
        <div style={{ fontSize: 34, color: C.ink, opacity: 0.7 }}>{korean}</div>
      </div>
      {passed ? (
        <div
          style={{
            fontSize: 40,
            color: C.green,
            border: `6px solid ${C.green}`,
            borderRadius: 20,
            padding: "4px 18px 0",
            transform: `scale(${pop(frame, PASS)}) rotate(-7deg)`,
          }}
        >
          통과!
        </div>
      ) : null}
    </div>
  );
};

/** 익힌 낱말(꿈 구슬)로 만드는 표현과 문장. 오른쪽은 실제 앱이 "A big dog"를 묻는 녹화입니다. */
export const SceneSentence: React.FC = () => {
  const frame = useCurrentFrame();
  const enter = pop(frame, 2, false);
  return (
    <AbsoluteFill style={{ fontFamily: FONT }}>
      <Header at={4} text="익힌 단어가 모이면, " hi="표현과 문장으로" />

      {/* 익힌 낱말 두 개 */}
      <div style={{ position: "absolute", left: 90, top: 244, width: 830, display: "flex", alignItems: "center", gap: 20 }}>
        <div style={{ fontSize: 32, color: "rgba(255,255,255,0.7)", marginRight: 6 }}>익힌 단어(꿈 구슬)</div>
        {["Dog", "Big"].map((word, i) => (
          <div key={word} style={{ transform: `scale(${frame >= 12 + i * 8 ? pop(frame, 12 + i * 8) : 0})` }}>
            <Pill size={50} bg="linear-gradient(160deg, #FFE9A8 0%, #FBBF24 100%)" color="#7C4A03" style={{ boxShadow: `0 0 26px ${C.yellow}88` }}>
              {word}
            </Pill>
          </div>
        ))}
      </div>

      <Card
        at={s(1.6)}
        top={340}
        stage={3}
        tag="꼬마 강아지(30단계)부터 · 짧은 표현"
        english={
          <>
            A <span style={{ color: C.pink }}>big</span> dog
          </>
        }
        korean="큰 개"
        passed={frame >= PASS}
      />
      <Card
        at={CARD2}
        top={560}
        stage={4}
        tag="소년 강아지(60단계)부터 · 문장"
        english={
          <>
            The dog is <span style={{ color: C.pink }}>big</span>.
          </>
        }
        korean="개가 커요."
      />
      <div style={{ position: "absolute", left: 90, top: 778, width: 830, textAlign: "center", opacity: frame >= CARD2 + 14 ? pop(frame, CARD2 + 14, false) : 0 }}>
        <Pill size={34}>익힌 단어가 늘수록 새 문장도 끝없이 늘어요</Pill>
      </div>

      {/* 실제 앱 */}
      <div style={{ position: "absolute", left: 990 + (1 - enter) * 1100, top: 240 }}>
        <DeviceFrame width={850} ratio={1682 / 1210}>
          <Video
            src={staticFile("demo/clips/sentence.mp4")}
            trimBefore={Math.max(0, Math.round(SENTENCE.clipAsk * 30) - ASK)}
            muted
            style={{ width: "100%", height: "100%", objectFit: "cover" }}
          />
        </DeviceFrame>
        <div style={{ textAlign: "center", fontSize: 28, color: "rgba(255,255,255,0.65)", marginTop: 12 }}>
          실제 앱 화면 · 익힌 Dog와 Big으로 만든 표현을 물어요
        </div>
      </div>
    </AbsoluteFill>
  );
};

import { AbsoluteFill, interpolate, spring, useCurrentFrame } from "remotion";
import { PopText } from "../parts/PopText";
import { PuppyOnCloud } from "../parts/Puppy";
import { C, FONT, FPS, HEIGHT, WIDTH, pop, s } from "../theme";

/** 장면 2: 밤하늘이 열리고 구름 강아지가 내려앉으며 제목이 나옵니다(하늘 배경은 Demo가 그립니다). */
export const Scene2: React.FC<{ duration: number }> = ({ duration }) => {
  const frame = useCurrentFrame();
  const drop = spring({ frame: frame - 12, fps: FPS, config: { damping: 11, stiffness: 120, mass: 0.9 } });
  const landed = frame - 21;
  const squash = landed >= 0 && landed < 12 ? Math.sin((landed / 12) * Math.PI) : 0;
  const bob = Math.sin(frame / 16) * 9;
  const sub = pop(frame, s(3.0), false);
  const out = interpolate(frame, [duration - 12, duration - 1], [1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  const moon = pop(frame, 20, false);
  return (
    <AbsoluteFill style={{ opacity: out, transform: `scale(${0.96 + 0.04 * out})` }}>
      <svg width={WIDTH} height={HEIGHT} style={{ position: "absolute" }}>
        <defs>
          <radialGradient id="s2Glow">
            <stop offset="0%" stopColor={C.moon} stopOpacity={0.5} />
            <stop offset="100%" stopColor={C.moon} stopOpacity={0} />
          </radialGradient>
          <radialGradient id="s2Moon" cx="38%" cy="34%" r="75%">
            <stop offset="0%" stopColor="#FFFBEA" />
            <stop offset="60%" stopColor={C.moon} />
            <stop offset="100%" stopColor="#F6CF78" />
          </radialGradient>
        </defs>
        <g transform={`translate(1660 ${190 - (1 - moon) * 60})`} opacity={moon}>
          <circle r={230 + 10 * Math.sin(frame / 20)} fill="url(#s2Glow)" />
          <circle r={96} fill="url(#s2Moon)" />
          <g opacity={0.14} fill="#B8862B">
            <ellipse cx={-30} cy={-26} rx={22} ry={16} />
            <ellipse cx={36} cy={22} rx={28} ry={19} />
            <ellipse cx={-4} cy={54} rx={12} ry={8} />
          </g>
        </g>
      </svg>

      <div style={{ position: "absolute", left: 150, top: interpolate(drop, [0, 1], [-640, 300]) + (frame > 40 ? bob : 0) }}>
        <PuppyOnCloud stage={2} size={460} happy={frame >= 21 && frame < 75} squash={squash} />
      </div>

      <div style={{ position: "absolute", left: 800, top: 380 }}>
        <PopText text="아직 멀었어?" at={s(1.6)} size={168} accent={C.yellow} />
      </div>
      <div
        style={{
          position: "absolute",
          left: 806,
          top: 610,
          opacity: sub,
          transform: `translateY(${(1 - sub) * 30}px)`,
          fontFamily: FONT,
          fontSize: 54,
          color: "#FFFFFF",
        }}
      >
        <span style={{ color: C.yellow }}>차가 가르치는 영어</span>, 배울수록 자라는 강아지
      </div>
    </AbsoluteFill>
  );
};

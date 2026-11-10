import { AbsoluteFill, Easing, interpolate, useCurrentFrame } from "remotion";
import { Moon } from "../parts/Bits";
import { PuppyOnCloud } from "../parts/Puppy";
import { C, FONT, pop, s } from "../theme";

const FLIP_AT = s(2.4);
const END_AT = s(4.8);

/** 장면 8: 달빛 섬에 도착. "아직 멀었어?"가 "벌써 다 왔어?"로 뒤집힙니다. */
export const Scene8: React.FC<{ duration: number }> = ({ duration }) => {
  const frame = useCurrentFrame();
  const fly = interpolate(frame, [0, s(2.4)], [0, 1], { extrapolateRight: "clamp", easing: Easing.bezier(0.2, 0.7, 0.2, 1) });
  const bob = Math.sin(frame / 14) * 10;
  const flip = interpolate(frame, [FLIP_AT, FLIP_AT + 14], [0, 180], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  const front = flip < 90;
  const end = pop(frame, END_AT, false);
  const dark = interpolate(frame, [duration - 18, duration - 2], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  return (
    <AbsoluteFill style={{ fontFamily: FONT }}>
      <Moon x={1530} y={330} r={interpolate(pop(frame, 2, false), [0, 1], [120, 190])} />
      <div style={{ position: "absolute", left: interpolate(fly, [0, 1], [-560, 1150]), top: interpolate(fly, [0, 1], [640, 430]) + bob }}>
        <PuppyOnCloud stage={5} size={400} happy={frame >= s(2.4) && frame < s(4.4)} />
      </div>

      <div style={{ position: "absolute", left: 120, top: 330, perspective: 1400 }}>
        <div
          style={{
            fontSize: 150,
            lineHeight: 1.1,
            whiteSpace: "nowrap",
            color: front ? "#FFFFFF" : C.yellow,
            transform: `scale(${pop(frame, 10)}) rotateX(${front ? flip : flip - 180}deg)`,
            transformOrigin: "0 50%",
            textShadow: "0 8px 0 rgba(23,19,95,0.55)",
          }}
        >
          {front ? "아직 멀었어?" : "벌써 다 왔어?"}
        </div>
      </div>

      <div style={{ position: "absolute", left: 126, top: 540, opacity: end, transform: `translateY(${(1 - end) * 24}px)`, color: "#FFFFFF" }}>
        <div style={{ fontSize: 58 }}>
          Pleos Connect 앱 <span style={{ color: C.yellow }}>「멍글리시」</span>
        </div>
        <div style={{ fontSize: 44, opacity: 0.85, marginTop: 16 }}>차가 가르치는 영어, 배울수록 자라는 강아지</div>
      </div>
      <AbsoluteFill style={{ background: "#0A0920", opacity: dark }} />
    </AbsoluteFill>
  );
};

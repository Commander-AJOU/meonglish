import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { C, FONT, HEIGHT, WIDTH, pop, s } from "../theme";

/** 옆에서 본 자동차. 뒷좌석 창에 아이가 보입니다. */
const Car: React.FC<{ wheel: number }> = ({ wheel }) => (
  <svg width={520} height={230} viewBox="0 0 520 230">
    <path d="M118 112 L168 44 Q174 36 186 36 L344 36 Q358 36 366 46 L414 112 Z" fill="#F59E0B" />
    <rect x={10} y={104} width={500} height={80} rx={34} fill={C.yellow} />
    <path d="M150 104 L186 54 L250 54 L250 104 Z" fill="#BFE8FF" />
    <path d="M264 54 L338 54 L384 104 L264 104 Z" fill="#BFE8FF" />
    {/* 뒷좌석 아이 */}
    <circle cx={214} cy={86} r={19} fill={C.ink} />
    <path d="M198 78 Q214 58 232 78" fill={C.ink} />
    {/* 운전자 */}
    <circle cx={318} cy={82} r={20} fill="#3B3880" />
    <rect x={486} y={122} width={22} height={20} rx={8} fill="#FFF7D6" />
    <rect x={12} y={124} width={14} height={18} rx={6} fill="#F87171" />
    {[130, 400].map((cx) => (
      <g key={cx} transform={`rotate(${wheel} ${cx} 184)`}>
        <circle cx={cx} cy={184} r={40} fill="#1B1A3F" />
        <circle cx={cx} cy={184} r={18} fill="#C7C9E8" />
        <rect x={cx - 3} y={148} width={6} height={72} fill="#1B1A3F" opacity={0.5} />
      </g>
    ))}
  </svg>
);

const Bubble: React.FC<{ at: number; cx: number; cy: number; size: number; shake?: boolean }> = ({ at, cx, cy, size, shake }) => {
  const frame = useCurrentFrame();
  const k = pop(frame, at);
  if (frame < at) return null;
  const wobble = shake ? Math.sin((frame - at) * 1.1) * 2.2 * Math.max(0, 1 - (frame - at) / 50) : 0;
  return (
    <div
      style={{
        position: "absolute",
        left: cx,
        top: cy,
        transform: `translate(-50%, -50%) scale(${k}) rotate(${wobble - 3}deg)`,
      }}
    >
      <div
        style={{
          fontFamily: FONT,
          fontSize: size,
          color: C.ink,
          background: "#FFFFFF",
          padding: `${size * 0.3}px ${size * 0.5}px ${size * 0.24}px`,
          borderRadius: size * 0.7,
          whiteSpace: "nowrap",
          boxShadow: `0 ${size * 0.08}px 0 rgba(0,0,0,0.25)`,
        }}
      >
        아직 멀었어<span style={{ color: C.pink }}>?</span>
      </div>
      <svg width={size * 0.6} height={size * 0.5} viewBox="0 0 60 50" style={{ position: "absolute", left: size * 0.5, bottom: -size * 0.36 }}>
        <path d="M6 0 L54 0 L4 48 Z" fill="#FFFFFF" />
      </svg>
    </div>
  );
};

/** 장면 1: 밤길을 달리는 차, 뒷자리에서 점점 커지는 "아직 멀었어?" */
export const Scene1: React.FC = () => {
  const frame = useCurrentFrame();
  const roadIn = interpolate(frame, [0, 24], [0, 1], { extrapolateRight: "clamp" });
  const carIn = pop(frame, 6, false);
  const bob = Math.sin(frame / 5) * 2.5;
  const pill = pop(frame, s(1.0));
  return (
    <AbsoluteFill style={{ background: `radial-gradient(ellipse at 50% 60%, #1A1850 0%, ${C.road} 70%)` }}>
      <svg width={WIDTH} height={HEIGHT} style={{ position: "absolute" }}>
        {/* 멀리 지나가는 가로등 */}
        {[0, 1, 2, 3].map((i) => {
          const x = ((((i * 640 - frame * 7) % 2560) + 2560) % 2560) - 320;
          return (
            <g key={i} opacity={0.5 * roadIn}>
              <rect x={x} y={560} width={8} height={270} fill="#2A2870" />
              <circle cx={x + 4} cy={556} r={16} fill="#FFE9A8" opacity={0.8} />
              <circle cx={x + 4} cy={556} r={54} fill="#FFE9A8" opacity={0.08} />
            </g>
          );
        })}
        <rect x={0} y={828} width={WIDTH * roadIn} height={112} fill="#15143A" />
        {Array.from({ length: 16 }, (_, i) => {
          const x = ((((i * 150 - frame * 14) % 2400) + 2400) % 2400) - 240;
          return x < WIDTH * roadIn ? <rect key={i} x={x} y={880} width={84} height={9} rx={4} fill="#FFFFFF" opacity={0.55} /> : null;
        })}
      </svg>

      <div style={{ position: "absolute", left: interpolate(carIn, [0, 1], [-560, 120]), top: 664 + bob, transform: "scale(1.3)", transformOrigin: "0 100%" }}>
        <Car wheel={frame * 16} />
      </div>

      <div
        style={{
          position: "absolute",
          left: 110,
          top: 84,
          transform: `scale(${pill})`,
          transformOrigin: "0 50%",
          fontFamily: FONT,
          fontSize: 38,
          color: "#FFFFFF",
          border: "3px solid rgba(255,255,255,0.35)",
          borderRadius: 40,
          padding: "12px 30px 10px",
        }}
      >
        목적지까지 <span style={{ color: C.yellow }}>42분</span>
      </div>

      <Bubble at={s(2.5)} cx={470} cy={505} size={52} />
      <Bubble at={s(3.7)} cx={1030} cy={575} size={84} />
      <Bubble at={s(4.7)} cx={1340} cy={255} size={136} shake />
    </AbsoluteFill>
  );
};

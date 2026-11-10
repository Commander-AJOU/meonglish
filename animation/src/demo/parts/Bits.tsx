import { interpolate, useCurrentFrame } from "remotion";
import { C, FONT, WIDTH, pop } from "../theme";

/** 장면 위쪽의 한 줄 제목. hi는 노란색으로 강조할 부분. */
export const Header: React.FC<{ at: number; until?: number; text: string; hi?: string }> = ({ at, until, text, hi }) => {
  const frame = useCurrentFrame();
  if (frame < at || (until !== undefined && frame >= until + 8)) return null;
  const k = pop(frame, at, false);
  const out = until === undefined ? 1 : interpolate(frame, [until, until + 8], [1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  return (
    <div
      style={{
        position: "absolute",
        top: 120,
        width: WIDTH,
        textAlign: "center",
        fontFamily: FONT,
        fontSize: 68,
        color: "#FFFFFF",
        opacity: k * out,
        transform: `translateY(${(1 - k) * 24}px)`,
      }}
    >
      {text}
      {hi ? <span style={{ color: C.yellow }}>{hi}</span> : null}
    </div>
  );
};

/** start~end 프레임 동안만 보이고, 양 끝에서 부드럽게 나타났다 사라지는 묶음. */
export const Phase: React.FC<{ start: number; end: number; children: React.ReactNode }> = ({ start, end, children }) => {
  const frame = useCurrentFrame();
  if (frame < start || frame >= end) return null;
  const opacity = interpolate(frame, [start, start + 8, end - 8, end], [0, 1, 1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  return <div style={{ position: "absolute", inset: 0, opacity }}>{children}</div>;
};

/** 밤하늘의 달(달빛 섬). */
export const Moon: React.FC<{ x: number; y: number; r: number; opacity?: number }> = ({ x, y, r, opacity = 1 }) => {
  const frame = useCurrentFrame();
  const size = r * 5;
  return (
    <svg width={size} height={size} viewBox={`${-size / 2} ${-size / 2} ${size} ${size}`} style={{ position: "absolute", left: x - size / 2, top: y - size / 2, opacity }}>
      <defs>
        <radialGradient id="bitMoonGlow">
          <stop offset="0%" stopColor={C.moon} stopOpacity={0.5} />
          <stop offset="100%" stopColor={C.moon} stopOpacity={0} />
        </radialGradient>
        <radialGradient id="bitMoon" cx="38%" cy="34%" r="75%">
          <stop offset="0%" stopColor="#FFFBEA" />
          <stop offset="60%" stopColor={C.moon} />
          <stop offset="100%" stopColor="#F6CF78" />
        </radialGradient>
      </defs>
      <circle r={r * (2.35 + 0.1 * Math.sin(frame / 20))} fill="url(#bitMoonGlow)" />
      <circle r={r} fill="url(#bitMoon)" />
      <g opacity={0.14} fill="#B8862B">
        <ellipse cx={-r * 0.31} cy={-r * 0.27} rx={r * 0.23} ry={r * 0.17} />
        <ellipse cx={r * 0.37} cy={r * 0.23} rx={r * 0.29} ry={r * 0.2} />
        <ellipse cx={-r * 0.04} cy={r * 0.56} rx={r * 0.13} ry={r * 0.09} />
      </g>
    </svg>
  );
};

/** 둥근 알약 모양 표시. */
export const Pill: React.FC<{ children: React.ReactNode; size?: number; bg?: string; color?: string; style?: React.CSSProperties }> = ({
  children,
  size = 40,
  bg = "rgba(255,255,255,0.14)",
  color = "#FFFFFF",
  style,
}) => (
  <div
    style={{
      display: "inline-block",
      fontFamily: FONT,
      fontSize: size,
      color,
      background: bg,
      padding: `${size * 0.3}px ${size * 0.75}px ${size * 0.24}px`,
      borderRadius: size * 1.2,
      whiteSpace: "nowrap",
      ...style,
    }}
  >
    {children}
  </div>
);

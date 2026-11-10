import { AbsoluteFill } from "remotion";
import { MOON_LOOP, useLoop, wave } from "./loop";
import { palette } from "./palette";

/** 달빛 섬: 숨 쉬듯 번지는 달빛과 그 둘레에 떠 있는 꿈 구슬(투명 배경). */
export const MoonIsland: React.FC = () => {
  return (
    <AbsoluteFill>
      <svg viewBox="0 0 600 600" width="100%" height="100%">
        <defs>
          <radialGradient id="moonGlow">
            <stop offset="0%" stopColor={palette.moon} stopOpacity={0.55} />
            <stop offset="45%" stopColor={palette.moon} stopOpacity={0.16} />
            <stop offset="100%" stopColor={palette.moon} stopOpacity={0} />
          </radialGradient>
          <radialGradient id="moonDisc" cx="38%" cy="34%" r="75%">
            <stop offset="0%" stopColor="#FFFBEA" />
            <stop offset="60%" stopColor={palette.moon} />
            <stop offset="100%" stopColor="#F6CF78" />
          </radialGradient>
          <radialGradient id="bead" cx="35%" cy="30%" r="75%">
            <stop offset="0%" stopColor="#FFFFFF" stopOpacity={0.95} />
            <stop offset="100%" stopColor="#FFFFFF" stopOpacity={0.15} />
          </radialGradient>
          <filter id="moonSoft" x="-50%" y="-50%" width="200%" height="200%">
            <feGaussianBlur stdDeviation={9} />
          </filter>
        </defs>

        <Glow />
        <circle cx={300} cy={300} r={132} fill="url(#moonDisc)" />
        {/* 달 표면의 옅은 무늬 */}
        <g opacity={0.14} fill="#B8862B">
          <ellipse cx={256} cy={262} rx={30} ry={22} />
          <ellipse cx={352} cy={330} rx={40} ry={28} />
          <ellipse cx={300} cy={372} rx={18} ry={12} />
          <ellipse cx={340} cy={246} rx={14} ry={10} />
        </g>
        <CloudBand />
        <Beads />
      </svg>
    </AbsoluteFill>
  );
};

const Glow: React.FC = () => {
  const t = useLoop(MOON_LOOP);
  const breathe = wave(t);

  return (
    <circle
      cx={300}
      cy={300}
      r={270 + breathe * 22}
      fill="url(#moonGlow)"
      opacity={0.85 + breathe * 0.15}
    />
  );
};

/** 달 아래를 스치는 구름 띠. */
const CloudBand: React.FC = () => {
  const t = useLoop(MOON_LOOP);
  const sway = wave(t, 1, 0.3) * 10;

  return (
    <g filter="url(#moonSoft)" opacity={0.85} transform={`translate(${sway} 0)`}>
      <ellipse cx={220} cy={408} rx={86} ry={26} fill="#FFFFFF" />
      <ellipse cx={320} cy={420} rx={110} ry={30} fill="#FFFFFF" />
      <ellipse cx={410} cy={404} rx={70} ry={22} fill="#FFFFFF" />
    </g>
  );
};

const BEADS = [
  { angle: -150, distance: 196, size: 20, color: "#BDE7FF" },
  { angle: -96, distance: 214, size: 14, color: "#FFD1E3" },
  { angle: -38, distance: 198, size: 24, color: "#D9C8FF" },
  { angle: 18, distance: 220, size: 15, color: "#C8F5D9" },
  { angle: 150, distance: 208, size: 18, color: "#FFE3B8" },
  { angle: 204, distance: 226, size: 12, color: "#BDE7FF" },
];

/** 꿈 구슬은 제자리에서 저마다 다른 박자로 떠오르내리며 반짝입니다. */
const Beads: React.FC = () => {
  const t = useLoop(MOON_LOOP);

  return (
    <g>
      {BEADS.map((bead, i) => {
        const radians = (bead.angle * Math.PI) / 180;
        const x = 300 + Math.cos(radians) * bead.distance;
        const y = 300 + Math.sin(radians) * bead.distance + wave(t, 1 + (i % 2), i / 6) * 9;
        const twinkle = 0.86 + wave(t, 2, i / 5) * 0.14;
        return (
          <g key={i} transform={`translate(${x} ${y})`} opacity={twinkle}>
            <circle r={bead.size * 2.1} fill={bead.color} opacity={0.22} filter="url(#moonSoft)" />
            <circle r={bead.size} fill={bead.color} />
            <circle r={bead.size} fill="url(#bead)" />
          </g>
        );
      })}
    </g>
  );
};

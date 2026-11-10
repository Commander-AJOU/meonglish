import { AbsoluteFill, random, useCurrentFrame } from "remotion";
import { C, HEIGHT, WIDTH } from "../theme";

const STARS = Array.from({ length: 90 }, (_, i) => ({
  x: random(`sx${i}`) * WIDTH,
  y: random(`sy${i}`) * HEIGHT * 0.78,
  r: 1.2 + random(`sr${i}`) * 2.6,
  phase: random(`sp${i}`) * Math.PI * 2,
  speed: 0.04 + random(`ss${i}`) * 0.06,
}));

/** 앱과 같은 밤하늘: 남색에서 보라로, 반짝이는 별. */
export const Sky: React.FC<{ moon?: { x: number; y: number; r: number } }> = ({ moon }) => {
  const frame = useCurrentFrame();
  return (
    <AbsoluteFill style={{ background: `linear-gradient(180deg, ${C.nightTop} 0%, #4A36C4 62%, ${C.nightBottom} 100%)` }}>
      <svg width={WIDTH} height={HEIGHT}>
        <defs>
          <radialGradient id="skyMoonGlow">
            <stop offset="0%" stopColor={C.moon} stopOpacity={0.5} />
            <stop offset="100%" stopColor={C.moon} stopOpacity={0} />
          </radialGradient>
          <radialGradient id="skyMoon" cx="38%" cy="34%" r="75%">
            <stop offset="0%" stopColor="#FFFBEA" />
            <stop offset="60%" stopColor={C.moon} />
            <stop offset="100%" stopColor="#F6CF78" />
          </radialGradient>
        </defs>
        {STARS.map((star, i) => (
          <circle
            key={i}
            cx={star.x}
            cy={star.y}
            r={star.r}
            fill="#FFFFFF"
            opacity={0.35 + 0.55 * (0.5 + 0.5 * Math.sin(frame * star.speed + star.phase))}
          />
        ))}
        {moon ? (
          <g>
            <circle cx={moon.x} cy={moon.y} r={moon.r * (2.3 + 0.12 * Math.sin(frame / 22))} fill="url(#skyMoonGlow)" />
            <circle cx={moon.x} cy={moon.y} r={moon.r} fill="url(#skyMoon)" />
            <g opacity={0.14} fill="#B8862B">
              <ellipse cx={moon.x - moon.r * 0.33} cy={moon.y - moon.r * 0.28} rx={moon.r * 0.22} ry={moon.r * 0.16} />
              <ellipse cx={moon.x + moon.r * 0.38} cy={moon.y + moon.r * 0.22} rx={moon.r * 0.3} ry={moon.r * 0.2} />
              <ellipse cx={moon.x} cy={moon.y + moon.r * 0.55} rx={moon.r * 0.13} ry={moon.r * 0.09} />
            </g>
          </g>
        ) : null}
      </svg>
    </AbsoluteFill>
  );
};

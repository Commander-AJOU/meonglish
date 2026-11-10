import { AbsoluteFill, random } from "remotion";
import { RAIN_LOOP, useLoop } from "./loop";

const WIDTH = 960;
const HEIGHT = 540;
const MARGIN = 120;
const TRAVEL = HEIGHT + MARGIN * 2;
const SLANT = 0.28;

const DROPS = new Array(46).fill(0).map((_, i) => ({
  x: random(`rain-x-${i}`) * (WIDTH + 240),
  start: random(`rain-y-${i}`),
  length: 44 + random(`rain-len-${i}`) * 44,
  // 한 반복 동안 화면을 정수 번 지나가야 이음새가 생기지 않습니다.
  laps: random(`rain-lap-${i}`) > 0.6 ? 2 : 1,
  opacity: 0.22 + random(`rain-o-${i}`) * 0.4,
  width: 1.6 + random(`rain-w-${i}`) * 1.6,
}));

/** 비스듬히 떨어지는 빗줄기(투명 배경). 장면 위에 겹쳐 씁니다. */
export const RainLayer: React.FC = () => {
  const t = useLoop(RAIN_LOOP);

  return (
    <AbsoluteFill>
      <svg viewBox={`0 0 ${WIDTH} ${HEIGHT}`} width="100%" height="100%">
        {DROPS.map((drop, i) => {
          const fall = ((drop.start + t * drop.laps) % 1) * TRAVEL - MARGIN;
          const x = drop.x - fall * SLANT;
          return (
            <line
              key={i}
              x1={x + drop.length * SLANT}
              y1={fall - drop.length}
              x2={x}
              y2={fall}
              stroke="#BFEBFF"
              strokeWidth={drop.width}
              strokeLinecap="round"
              opacity={drop.opacity}
            />
          );
        })}
      </svg>
    </AbsoluteFill>
  );
};

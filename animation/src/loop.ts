import { useCurrentFrame } from "remotion";

export const FPS = 24;

export const MOON_LOOP = 72;
export const RAIN_LOOP = 24;

/** 반복 안에서의 위치를 0 이상 1 미만으로 돌려줍니다. */
export const useLoop = (loopFrames: number): number => {
  const frame = useCurrentFrame();
  return (frame % loopFrames) / loopFrames;
};

/** 한 반복 동안 cycles번 오르내리는 -1~1 값. cycles가 정수면 이음새가 생기지 않습니다. */
export const wave = (t: number, cycles = 1, phase = 0): number =>
  Math.sin((t * cycles + phase) * Math.PI * 2);

/** 0~1을 반복하는 톱니 값. 떠오르거나 흘러가는 조각에 씁니다. */
export const drift = (t: number, offset: number, cycles = 1): number =>
  (((t * cycles + offset) % 1) + 1) % 1;

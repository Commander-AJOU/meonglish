import { loadFont } from "@remotion/fonts";
import { spring, staticFile } from "remotion";

export const FPS = 30;
export const WIDTH = 1920;
export const HEIGHT = 1080;

/** 초를 프레임으로. */
export const s = (seconds: number): number => Math.round(seconds * FPS);

/** 앱의 밤하늘 색(res/values/colors.xml)과 맞춘 색. */
export const C = {
  road: "#0A0920",
  nightTop: "#17135F",
  nightBottom: "#7C5CF0",
  moon: "#FFE9A8",
  yellow: "#FBBF24",
  pink: "#EC4899",
  blue: "#2563EB",
  green: "#34D399",
  ink: "#1E1B4B",
  white: "#FFFFFF",
};

export const FONT = "Jua";
loadFont({ family: FONT, url: staticFile("demo/fonts/jua.ttf") });

/** 통 하고 튀어나오는 0→1 값(살짝 넘쳤다가 돌아옴). */
export const pop = (frame: number, at: number, bouncy = true): number =>
  spring({
    frame: frame - at,
    fps: FPS,
    config: bouncy ? { damping: 10, stiffness: 170, mass: 0.7 } : { damping: 200 },
  });

export const art = (name: string): string => staticFile(`demo/art/${name}.png`);

import { Img, useCurrentFrame } from "remotion";
import { art } from "../theme";

/** 폭신한 구름(강아지가 타는 것). width 기준으로 그립니다. */
export const Cloud: React.FC<{ width: number }> = ({ width }) => (
  <svg width={width} height={width * 0.3} viewBox="0 0 400 120">
    <ellipse cx={200} cy={78} rx={196} ry={34} fill="#DCD6FF" opacity={0.9} />
    <ellipse cx={200} cy={70} rx={190} ry={32} fill="#FFFFFF" />
    <circle cx={96} cy={56} r={40} fill="#FFFFFF" />
    <circle cx={160} cy={44} r={46} fill="#FFFFFF" />
    <circle cx={236} cy={46} r={44} fill="#FFFFFF" />
    <circle cx={304} cy={58} r={38} fill="#FFFFFF" />
  </svg>
);

/**
 * 구름 탄 강아지. 숨 쉬고, 가끔 눈을 깜빡입니다. size는 강아지 그림의 한 변.
 * happy가 true면 웃는 얼굴.
 */
export const PuppyOnCloud: React.FC<{ stage?: number; size: number; happy?: boolean; squash?: number }> = ({
  stage = 2,
  size,
  happy = false,
  squash = 0,
}) => {
  const frame = useCurrentFrame();
  const breathe = 1 + 0.014 * Math.sin(frame / 11);
  const blinking = stage > 1 && frame % 84 >= 78 && frame % 84 < 83;
  const face = happy ? `puppy_${stage}_happy` : blinking ? `puppy_${stage}_blink` : `puppy_${stage}`;
  return (
    <div style={{ position: "relative", width: size * 1.25, height: size * 1.12 }}>
      <div style={{ position: "absolute", left: 0, top: size * 0.78 }}>
        <Cloud width={size * 1.25} />
      </div>
      <Img
        src={art(face)}
        style={{
          position: "absolute",
          left: size * 0.125,
          top: 0,
          width: size,
          height: size,
          transformOrigin: "50% 92%",
          transform: `scale(${1 + squash * 0.12}, ${breathe * (1 - squash * 0.14)})`,
        }}
      />
    </div>
  );
};

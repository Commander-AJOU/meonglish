import { interpolate, useCurrentFrame } from "remotion";
import { FONT, FPS, WIDTH } from "../theme";
import { NARRATION } from "../timeline";

/** 내레이션 자막(소리를 끄고 봐도 내용이 전해지게). */
export const Caption: React.FC = () => {
  const frame = useCurrentFrame();
  const t = frame / FPS;
  const cue = NARRATION.find((c) => t >= c.at - 0.1 && t <= c.at + c.seconds + 0.35);
  if (!cue?.text) return null;
  const opacity = interpolate(t, [cue.at - 0.1, cue.at + 0.15, cue.at + cue.seconds + 0.1, cue.at + cue.seconds + 0.35], [0, 1, 1, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });
  return (
    <div style={{ position: "absolute", left: 0, top: 968, width: WIDTH, display: "flex", justifyContent: "center", opacity }}>
      <div
        style={{
          fontFamily: FONT,
          fontSize: 40,
          color: "#FFFFFF",
          background: "rgba(10, 9, 32, 0.62)",
          padding: "12px 34px 10px",
          borderRadius: 40,
          letterSpacing: 0.5,
        }}
      >
        {cue.text}
      </div>
    </div>
  );
};

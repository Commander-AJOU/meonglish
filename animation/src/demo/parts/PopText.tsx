import { useCurrentFrame } from "remotion";
import { FONT, pop } from "../theme";

/** 글자가 하나씩 통통 튀어나오는 제목. */
export const PopText: React.FC<{
  text: string;
  at: number;
  size: number;
  color?: string;
  accent?: string;
  stagger?: number;
}> = ({ text, at, size, color = "#FFFFFF", accent, stagger = 3 }) => {
  const frame = useCurrentFrame();
  return (
    <div style={{ fontFamily: FONT, fontSize: size, lineHeight: 1.1, whiteSpace: "pre", display: "flex" }}>
      {[...text].map((ch, i) => {
        const k = pop(frame, at + i * stagger);
        return (
          <span
            key={i}
            style={{
              display: "inline-block",
              color: accent && (ch === "?" || ch === "!") ? accent : color,
              transform: `translateY(${(1 - k) * size * 0.5}px) scale(${k})`,
              opacity: Math.min(1, k * 2),
              textShadow: `0 ${size * 0.05}px 0 rgba(23, 19, 95, 0.55)`,
            }}
          >
            {ch}
          </span>
        );
      })}
    </div>
  );
};

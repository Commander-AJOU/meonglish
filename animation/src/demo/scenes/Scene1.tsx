import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { Header } from "../parts/Bits";
import { PuppyOnCloud } from "../parts/Puppy";
import { C, FONT, HEIGHT, WIDTH, pop, s } from "../theme";
import { INTRO } from "../timeline";

/** 옆에서 본 자동차. 뒷좌석 창에 아이가 보입니다. */
const Car: React.FC<{ wheel: number; glow?: string; glowK?: number }> = ({ wheel, glow = "#60A5FA", glowK = 0 }) => (
  <svg width={520} height={230} viewBox="0 0 520 230" style={{ overflow: "visible" }}>
    <path d="M118 112 L168 44 Q174 36 186 36 L344 36 Q358 36 366 46 L414 112 Z" fill="#F59E0B" />
    <rect x={10} y={104} width={500} height={80} rx={34} fill={C.yellow} />
    <path d="M150 104 L186 54 L250 54 L250 104 Z" fill="#BFE8FF" />
    <path d="M264 54 L338 54 L384 104 L264 104 Z" fill="#BFE8FF" />
    {/* 뒷좌석 창의 빛(태블릿 화면의 파란 빛 → 강아지의 따뜻한 빛) */}
    <path d="M150 104 L186 54 L250 54 L250 104 Z" fill={glow} opacity={0.55 * glowK} />
    <circle cx={226} cy={84} r={36} fill={glow} opacity={0.35 * glowK} />
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

const Bubble: React.FC<{ at: number; cx: number; cy: number; size: number; shake?: boolean; until: number }> = ({ at, cx, cy, size, shake, until }) => {
  const frame = useCurrentFrame();
  const k = pop(frame, at) * interpolate(frame, [until, until + 8], [1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  if (frame < at || frame > until + 8) return null;
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

/** 나타났다가(at) 사라지는(until) 부분. */
const Beat: React.FC<{ at: number; until: number; children: React.ReactNode }> = ({ at, until, children }) => {
  const frame = useCurrentFrame();
  if (frame < at || frame > until + 10) return null;
  const opacity = interpolate(frame, [at, at + 10, until, until + 10], [0, 1, 1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  return <div style={{ position: "absolute", inset: 0, opacity }}>{children}</div>;
};

/** 차 위의 사람을 가리키는 카드. (x, y)는 카드 가운데, (tx, ty)는 가리키는 머리. */
const SeatCard: React.FC<{ at: number; x: number; y: number; tx: number; ty: number; title: string; text: string; icon: React.ReactNode }> = ({
  at,
  x,
  y,
  tx,
  ty,
  title,
  text,
  icon,
}) => {
  const frame = useCurrentFrame();
  const k = pop(frame, at);
  const line = interpolate(frame, [at + 6, at + 18], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  return (
    <>
      <svg width={WIDTH} height={HEIGHT} style={{ position: "absolute", left: 0, top: 0 }}>
        <line x1={x} y1={y + 86} x2={x + (tx - x) * line} y2={y + 86 + (ty - 40 - y - 86) * line} stroke="rgba(255,255,255,0.55)" strokeWidth={4} strokeDasharray="10 12" />
      </svg>
      <div
        style={{
          position: "absolute",
          left: x - 230,
          top: y - 80,
          width: 460,
          height: 166,
          borderRadius: 40,
          background: "#FFFFFF",
          boxShadow: "0 10px 0 rgba(0,0,0,0.25)",
          transform: `scale(${k})`,
          display: "flex",
          alignItems: "center",
          gap: 22,
          padding: "0 30px",
          boxSizing: "border-box",
          fontFamily: FONT,
        }}
      >
        {icon}
        <div>
          <div style={{ fontSize: 30, color: C.ink, opacity: 0.55 }}>{title}</div>
          <div style={{ fontSize: 50, color: C.ink, lineHeight: 1.15 }}>{text}</div>
        </div>
      </div>
    </>
  );
};

const Wheel: React.FC = () => (
  <svg width={96} height={96} viewBox="-50 -50 100 100">
    <circle r={40} fill="none" stroke={C.ink} strokeWidth={10} />
    <circle r={9} fill={C.ink} />
    <path d="M-38 6 L-9 4 M38 6 L9 4 M0 9 L0 38" stroke={C.ink} strokeWidth={10} strokeLinecap="round" />
  </svg>
);

const Bored: React.FC = () => {
  const frame = useCurrentFrame();
  return (
    <svg width={96} height={96} viewBox="-50 -50 100 100">
      <circle r={42} fill={C.yellow} />
      <path d="M-24 -6 L-8 -6 M8 -6 L24 -6" stroke={C.ink} strokeWidth={7} strokeLinecap="round" />
      <path d="M-14 20 Q0 14 14 20" stroke={C.ink} strokeWidth={6} fill="none" strokeLinecap="round" />
      <text x={30} y={-26 - (frame % 40) / 4} fontSize={22} fill={C.ink} fontFamily={FONT} opacity={0.7}>
        z
      </text>
    </svg>
  );
};

/** 태블릿: 아이 손에 쥐여 주는 화면. */
const Tablet: React.FC<{ at: number }> = ({ at }) => {
  const frame = useCurrentFrame();
  const k = pop(frame, at);
  const progress = ((frame - at) / 150) % 1;
  return (
    <div style={{ position: "absolute", left: 1060, top: 250, transform: `scale(${k}) rotate(4deg)`, transformOrigin: "50% 50%" }}>
      <div
        style={{
          width: 640,
          height: 440,
          borderRadius: 46,
          background: "#14123A",
          padding: 26,
          boxSizing: "border-box",
          boxShadow: "0 0 120px rgba(96,165,250,0.55), 0 20px 0 rgba(0,0,0,0.3)",
        }}
      >
        <div
          style={{
            width: "100%",
            height: "100%",
            borderRadius: 24,
            background: "linear-gradient(160deg, #3B82F6 0%, #1E3A8A 100%)",
            position: "relative",
            overflow: "hidden",
          }}
        >
          <svg width={160} height={160} viewBox="-50 -50 100 100" style={{ position: "absolute", left: 214, top: 90 }}>
            <circle r={44} fill="rgba(255,255,255,0.22)" />
            <path d="M-14 -22 L24 0 L-14 22 Z" fill="#FFFFFF" />
          </svg>
          <div style={{ position: "absolute", left: 30, right: 30, bottom: 30, height: 10, borderRadius: 5, background: "rgba(255,255,255,0.25)" }}>
            <div style={{ width: `${progress * 100}%`, height: "100%", borderRadius: 5, background: "#FFFFFF" }} />
          </div>
        </div>
      </div>
    </div>
  );
};

/** 영유아 사교육 통계(교육부 「2024 유아 사교육비 시험조사」, 2025년 3월 발표, 국가 미승인 시험조사). */
const Stat: React.FC<{ at: number; top: number; big: string; unit: string; text: string; color: string }> = ({ at, top, big, unit, text, color }) => {
  const frame = useCurrentFrame();
  const k = pop(frame, at);
  return (
    <div
      style={{
        position: "absolute",
        left: 930,
        top,
        width: 880,
        height: 180,
        borderRadius: 44,
        background: "rgba(255,255,255,0.1)",
        border: "3px solid rgba(255,255,255,0.22)",
        transform: `scale(${k})`,
        transformOrigin: "0 50%",
        display: "flex",
        alignItems: "center",
        gap: 30,
        padding: "0 40px",
        boxSizing: "border-box",
        fontFamily: FONT,
      }}
    >
      <div style={{ fontSize: 100, color, lineHeight: 1, whiteSpace: "nowrap", minWidth: 330 }}>
        {big}
        <span style={{ fontSize: 50 }}>{unit}</span>
      </div>
      <div style={{ fontSize: 42, color: "#FFFFFF", lineHeight: 1.25, whiteSpace: "nowrap" }}>{text}</div>
    </div>
  );
};

/**
 * 장면 1: 공감을 이끄는 도입.
 * "아직 멀었어?" → 운전에 집중해야 하는 부모와 지루한 아이 → 결국 쥐여 주는 태블릿 → 영어 교육에 대한 바람 →
 * 화면 대신 옆에서 같이 배우는 친구. 끝나면 뒷좌석 창에서 밤하늘 세계가 동그랗게 열립니다(Demo).
 */
export const Scene1: React.FC = () => {
  const frame = useCurrentFrame();
  const roadIn = interpolate(frame, [0, 24], [0, 1], { extrapolateRight: "clamp" });
  const carIn = pop(frame, 6, false);
  const bob = Math.sin(frame / 5) * 2.5;
  // "목적지까지 42분"은 처음에만(위 제목과 겹치지 않게).
  const pill = pop(frame, s(1.0)) * interpolate(frame, [s(INTRO.driver) - 8, s(INTRO.driver)], [1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  const DRIVER = s(INTRO.driver);
  const TABLET = s(INTRO.tablet);
  const WORRY = s(INTRO.worry);
  const FRIEND = s(INTRO.friend);
  // 뒷좌석 창의 빛: 태블릿을 쥔 동안은 파랗게, 강아지가 오면 따뜻하게.
  const blue = interpolate(frame, [TABLET + 6, TABLET + 20, FRIEND, FRIEND + 12], [0, 1, 1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  const warm = interpolate(frame, [FRIEND + 6, FRIEND + 24], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  const puppyIn = pop(frame, FRIEND + 14);
  const float = Math.sin(frame / 12) * 8;
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
        <Car wheel={frame * 16} glow={warm > 0 ? "#FFE9A8" : "#60A5FA"} glowK={Math.max(blue, warm)} />
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

      <Bubble at={s(2.5)} cx={470} cy={505} size={52} until={DRIVER - 6} />
      <Bubble at={s(3.7)} cx={1030} cy={575} size={84} until={DRIVER - 6} />
      <Bubble at={s(4.7)} cx={1340} cy={255} size={136} shake until={DRIVER - 6} />

      {/* ② 운전석은 앞만, 뒷자리는 지루함 */}
      <Beat at={DRIVER} until={TABLET - 6}>
        <Header at={DRIVER + 2} text="부모는 운전에 집중해야 하고, " hi="아이는 지루해요" />
        <SeatCard at={DRIVER + 8} x={420} y={380} tx={398} ty={707} title="뒷자리" text="너무 심심해…" icon={<Bored />} />
        <SeatCard at={DRIVER + 22} x={1010} y={420} tx={533} ty={702} title="운전석" text="앞만 봐야 해요" icon={<Wheel />} />
      </Beat>

      {/* ③ 결국 쥐여 주는 태블릿 */}
      <Beat at={TABLET} until={WORRY - 6}>
        <Header at={TABLET + 2} text="그래서 결국, " hi="태블릿을 쥐여 줘요" />
        <Tablet at={TABLET + 8} />
      </Beat>

      {/* ④ 영어 교육에 대한 바람 */}
      <Beat at={WORRY} until={FRIEND - 6}>
        <Header at={WORRY + 2} text="영어 공부는 " hi="일찍부터 시키고 싶은데" />
        <Stat at={WORRY + 10} top={270} big="81.2" unit="%" text="5세 아이의 사교육 참여율" color={C.yellow} />
        <Stat at={WORRY + 22} top={480} big="154" unit="만 원" text="영어유치원 월평균 비용" color={C.pink} />
        <div style={{ position: "absolute", left: 934, top: 682, fontFamily: FONT, fontSize: 26, color: "rgba(255,255,255,0.55)", opacity: pop(frame, WORRY + 30, false) }}>
          교육부 「2024 유아 사교육비 시험조사」(2025.3, 국가 미승인 시험조사)
        </div>
      </Beat>

      {/* ⑤ 화면 대신, 옆에서 같이 배우는 친구 */}
      <Beat at={FRIEND} until={s(INTRO.end) + 30}>
        <Header at={FRIEND + 2} text="화면 대신, " hi="옆에서 같이 배우는 친구" />
        <div style={{ position: "absolute", left: 190, top: 330 + float, transform: `scale(${puppyIn})`, transformOrigin: "50% 100%" }}>
          <PuppyOnCloud stage={2} size={300} happy={frame - FRIEND > 20 && frame - FRIEND < 60} />
        </div>
        <div style={{ position: "absolute", left: 760, top: 330, display: "flex", flexDirection: "column", gap: 26, fontFamily: FONT }}>
          {["영어 동화를 들려주고", "같이 따라 말하고", "배운 만큼 함께 자라요"].map((t, i) => {
            const k = pop(frame, FRIEND + 40 + i * 14);
            return (
              <div
                key={t}
                style={{
                  transform: `scale(${k})`,
                  transformOrigin: "0 50%",
                  fontSize: 58,
                  color: C.ink,
                  background: i === 0 ? C.yellow : "#FFFFFF",
                  borderRadius: 60,
                  padding: "16px 44px 12px",
                  alignSelf: "flex-start",
                  boxShadow: "0 8px 0 rgba(0,0,0,0.22)",
                }}
              >
                {t}
              </div>
            );
          })}
        </div>
      </Beat>
    </AbsoluteFill>
  );
};

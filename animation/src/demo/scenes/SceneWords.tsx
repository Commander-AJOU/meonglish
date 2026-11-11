import { AbsoluteFill, Img, interpolate, random, useCurrentFrame } from "remotion";
import { Header, Phase, Pill } from "../parts/Bits";
import { C, FONT, art, pop, s } from "../theme";
import { WORDS } from "../timeline";

const PICK_AT = s(WORDS.pick);
const PACE_AT = s(WORDS.pace);

/** 앱 단어장(WordBank.kt)의 주제와 낱말 수, 보기 낱말. */
const TOPICS = [
  { name: "집과 장난감", count: 131, sample: "Sofa · Robot · Kite" },
  { name: "동사", count: 125, sample: "Jump · Sing · Hug" },
  { name: "형용사", count: 110, sample: "Brave · Fluffy · Shiny" },
  { name: "동물", count: 109, sample: "Otter · Dinosaur · Owl" },
  { name: "먹을 것", count: 101, sample: "Mango · Gimbap · Pie" },
  { name: "창밖에 보이는 것", count: 86, sample: "Bridge · Bakery · Tower" },
  { name: "자연", count: 80, sample: "Acorn · Puddle · Cave" },
  { name: "몸과 옷", count: 74, sample: "Knee · Umbrella · Boot" },
  { name: "때와 학교", count: 71, sample: "Monday · Picnic · Game" },
  { name: "사람", count: 57, sample: "Grandma · Pilot · Chef" },
  { name: "차와 길", count: 48, sample: "Wiper · Tire · Subway" },
  { name: "이야기 속 단어", count: 33, sample: "Puppy · Moon · Wish" },
  { name: "공룡과 상상", count: 27, sample: "Dragon · Fairy · Alien" },
  { name: "수", count: 26, sample: "One · Ten · Hundred" },
  { name: "색깔", count: 15, sample: "Red · Silver · Navy" },
];
const COLORS = [C.blue, C.pink, "#0EA5A4", "#D97706", "#7C3AED"];

/** 1: 배우는 말의 규모. */
const Scale: React.FC = () => {
  const frame = useCurrentFrame();
  const total = Math.round(interpolate(frame, [10, 60], [0, 1101], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }));
  return (
    <AbsoluteFill style={{ fontFamily: FONT }}>
      <div style={{ position: "absolute", left: 0, top: 232, width: 1920, display: "flex", justifyContent: "center", gap: 28 }}>
        {[
          { label: "단어", value: total.toLocaleString("en-US"), color: C.yellow, at: 8 },
          { label: "짧은 표현", value: "171", color: "#FFFFFF", at: 40 },
          { label: "문장", value: "147", color: "#FFFFFF", at: 48 },
          { label: "익힌 단어로 만드는 문장", value: "끝없이", color: C.green, at: 56 },
        ].map((item) => (
          <div
            key={item.label}
            style={{
              transform: `scale(${frame >= item.at ? pop(frame, item.at) : 0})`,
              background: "rgba(10,9,32,0.5)",
              borderRadius: 36,
              padding: "16px 36px 14px",
              textAlign: "center",
            }}
          >
            <div style={{ fontSize: 76, color: item.color, lineHeight: 1 }}>{item.value}</div>
            <div style={{ fontSize: 32, color: "#FFFFFF", opacity: 0.8, marginTop: 6 }}>{item.label}</div>
          </div>
        ))}
      </div>
      {TOPICS.map((topic, i) => {
        const at = 16 + i * 4;
        if (frame < at) return null;
        const col = i % 5;
        const row = Math.floor(i / 5);
        const tilt = (random(`wt${i}`) - 0.5) * 3;
        return (
          <div
            key={topic.name}
            style={{
              position: "absolute",
              left: 70 + col * 360,
              top: 428 + row * 172,
              width: 340,
              height: 152,
              borderRadius: 32,
              background: "#FFFFFF",
              transform: `scale(${pop(frame, at)}) rotate(${tilt}deg)`,
              boxShadow: "0 12px 30px rgba(8,6,40,0.4)",
              padding: "18px 26px 0",
              boxSizing: "border-box",
            }}
          >
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline" }}>
              <span style={{ fontSize: 34, color: C.ink }}>{topic.name}</span>
              <span style={{ fontSize: 40, color: COLORS[i % COLORS.length] }}>{topic.count}</span>
            </div>
            <div style={{ fontSize: 27, color: C.ink, opacity: 0.6, marginTop: 16, whiteSpace: "nowrap" }}>{topic.sample}</div>
          </div>
        );
      })}
    </AbsoluteFill>
  );
};

/** 앱이 실제로 고른 결과(에뮬레이터에서 확인한 값). */
const PICKS = [
  { when: "비가 오면", sign: "와이퍼 신호", word: "Umbrella", ko: "우산", color: C.blue },
  { when: "공원에 가는 길이면", sign: "내비 목적지", word: "Park", ko: "공원", color: "#0EA5A4" },
  { when: "오늘 이야기에 나오면", sign: "그날의 이야기 주제", word: "Wish", ko: "소원", color: C.pink },
  { when: "가을이면", sign: "달력과 시계", word: "Leaf", ko: "나뭇잎", color: "#D97706" },
];

/** 2: 지금 상황에 맞는 낱말을 먼저. */
const Picking: React.FC = () => {
  const frame = useCurrentFrame() - PICK_AT;
  return (
    <AbsoluteFill style={{ fontFamily: FONT }}>
      {PICKS.map((pick, i) => {
        const at = 8 + i * 12;
        if (frame < at) return null;
        const flip = pop(frame, at + 10);
        return (
          <div key={pick.word} style={{ position: "absolute", left: 80 + i * 450, top: 250, width: 420, transform: `scale(${pop(frame, at)})` }}>
            <div style={{ background: "rgba(10,9,32,0.5)", border: `5px solid ${pick.color}`, borderRadius: 36, padding: "22px 0 18px", textAlign: "center" }}>
              <div style={{ fontSize: 28, color: "#FFFFFF", opacity: 0.65 }}>{pick.sign}</div>
              <div style={{ fontSize: 44, color: "#FFFFFF", marginTop: 6 }}>{pick.when}</div>
            </div>
            <svg width={60} height={56} viewBox="0 0 60 56" style={{ display: "block", margin: "8px auto", opacity: flip }}>
              <path d="M30 2 L30 44 M12 28 L30 50 L48 28" fill="none" stroke="#FFFFFF" strokeWidth={9} strokeLinecap="round" strokeLinejoin="round" />
            </svg>
            <div
              style={{
                background: "#FFFFFF",
                borderRadius: 36,
                padding: "22px 0 18px",
                textAlign: "center",
                transform: `scale(${flip})`,
                boxShadow: "0 14px 34px rgba(8,6,40,0.45)",
              }}
            >
              <div style={{ fontSize: 80, color: pick.color, lineHeight: 1.05 }}>{pick.word}</div>
              <div style={{ fontSize: 36, color: C.ink, opacity: 0.65 }}>{pick.ko}</div>
            </div>
          </div>
        );
      })}
      {/* 실제 설정 화면 */}
      <div style={{ position: "absolute", left: 250, top: 676, width: 1420, opacity: pop(frame, 62, false), transform: `translateY(${(1 - pop(frame, 62, false)) * 30}px)` }}>
        <Img src={art("settings_english")} style={{ width: 1420, borderRadius: 26, boxShadow: "0 14px 34px rgba(8,6,40,0.45)" }} />
        <div style={{ textAlign: "center", fontSize: 28, color: "rgba(255,255,255,0.65)", marginTop: 10 }}>
          실제 앱의 설정 화면 · 비 오는 날 공원에 갈 때 고른 새 단어
        </div>
      </div>
    </AbsoluteFill>
  );
};

/** 앱의 설정 "배우는 양"과 같은 계산: 출발하자마자 하나, 그 뒤로 10분마다 고른 수만큼(EnglishCoach.expectedLessons). */
const PER_TEN = 3;
const TRIPS = [
  { label: "15분 등원길", minutes: 15, color: C.yellow },
  { label: "1시간 나들이", minutes: 60, color: C.green },
];

/** 3: 얼마나 배울지는 부모가 "10분마다 몇 개"로 고르고, 이동이 길수록 그만큼 더 배웁니다. */
const Pace: React.FC = () => {
  const frame = useCurrentFrame() - PACE_AT;
  return (
    <AbsoluteFill style={{ fontFamily: FONT }}>
      {/* 설정의 선택지: 1~5개 */}
      <div style={{ position: "absolute", left: 160, top: 250, width: 1600, opacity: pop(frame, 6, false) }}>
        <div style={{ fontSize: 34, color: "rgba(255,255,255,0.75)", marginBottom: 14 }}>설정 · 10분마다 영어 몇 개 (복습 포함)</div>
        <div style={{ display: "flex", gap: 18, alignItems: "center" }}>
          {[1, 2, 3, 4, 5].map((count) => {
            const at = 10 + count * 4;
            const picked = count === PER_TEN && frame >= 36;
            return (
              <div
                key={count}
                style={{
                  transform: `scale(${frame >= at ? pop(frame, at) : 0}) scale(${picked ? 1.12 : 1})`,
                  width: 150,
                  height: 96,
                  borderRadius: 48,
                  background: picked ? C.blue : "#FFFFFF",
                  color: picked ? "#FFFFFF" : C.ink,
                  fontSize: 46,
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  boxShadow: "0 8px 0 rgba(0,0,0,0.22)",
                }}
              >
                {count}개
              </div>
            );
          })}
          <div style={{ marginLeft: 22, fontSize: 30, color: C.pink, opacity: frame >= 46 ? pop(frame, 46, false) : 0 }}>
            5개는 아이가 지루해할 수 있어요
          </div>
        </div>
      </div>

      {/* 이동이 길수록 그만큼 더 */}
      {TRIPS.map((trip, i) => {
        const at = 60 + i * 26;
        if (frame < at) return null;
        const lessons = 1 + Math.floor((trip.minutes * PER_TEN) / 10);
        const shown = Math.round(interpolate(frame, [at + 6, at + 6 + lessons * 2], [0, lessons], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }));
        return (
          <div
            key={trip.label}
            style={{
              position: "absolute",
              left: 160,
              top: 470 + i * 200,
              width: 1600,
              height: 172,
              borderRadius: 40,
              background: "rgba(10,9,32,0.5)",
              transform: `scale(${pop(frame, at)})`,
              display: "flex",
              alignItems: "center",
              padding: "0 44px",
              boxSizing: "border-box",
              gap: 36,
            }}
          >
            <div style={{ width: 330, fontSize: 50, color: "#FFFFFF" }}>{trip.label}</div>
            <div style={{ flex: 1, display: "flex", gap: 9 }}>
              {Array.from({ length: shown }, (_, k) => (
                <div key={k} style={{ width: 30, height: 30, borderRadius: 15, background: trip.color, flexShrink: 0 }} />
              ))}
            </div>
            <div style={{ width: 300, textAlign: "right" }}>
              <Pill size={52} bg={trip.color} color={C.ink}>
                영어 {shown}개쯤
              </Pill>
            </div>
          </div>
        );
      })}
    </AbsoluteFill>
  );
};

/** 배우는 말의 규모와, 무엇을 얼마나 배울지 고르는 방법. */
export const SceneWords: React.FC<{ duration: number }> = ({ duration }) => (
  <AbsoluteFill>
    <Header at={2} until={PICK_AT - 8} text="배우는 단어와 문장은 " hi="1,400가지가 넘어요" />
    <Header at={PICK_AT + 2} until={PACE_AT - 8} text="무엇을 먼저 배울지는 " hi="지금 상황이 정해요" />
    <Header at={PACE_AT + 2} text="얼마나 배울지는 " hi="10분마다 몇 개로" />
    <Phase start={0} end={PICK_AT}>
      <Scale />
    </Phase>
    <Phase start={PICK_AT - 4} end={PACE_AT}>
      <Picking />
    </Phase>
    <Phase start={PACE_AT - 4} end={duration + 30}>
      <Pace />
    </Phase>
  </AbsoluteFill>
);

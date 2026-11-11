import { s } from "./theme";

/**
 * 이야기 장면(장면 6) 안의 시각(초). clip…은 녹화한 앱 화면(clips/note_story.mp4) 속 시각입니다.
 * 매일 새로 → 상황 세 가지 → 오늘 있었던 일(질문 → 기다림 → 위로) → 남은 시간 → 내비 안내.
 */
export const STORY = {
  chips: 7.8,
  note: 10.7,
  ask: 13.4,
  askLen: 4.56,
  comfort: 20.3,
  fit: 24.7,
  navi: 30.4,
  end: 41.4,
  clipAsk: 1.17,
  clipComfort: 10.93,
};

/**
 * 도입(장면 1) 안의 시각(초): "아직 멀었어?" → 운전에 집중해야 하는 부모와 지루한 아이(driver) → 결국 쥐여 주는 태블릿(tablet)
 * → 영어 교육에 대한 바람(worry) → 화면 대신 옆에서 같이 배우는 친구(friend) → 제목으로(end).
 */
export const INTRO = { driver: 6.6, tablet: 12.0, worry: 17.3, friend: 23.3, end: 31.2 };
/** 제목 장면(장면 2)이 시작하는 때. 동그란 전환이 도입 끝과 겹치도록 조금 일찍. */
const T2 = INTRO.end - 0.4;

/**
 * 깜빡이 장면(장면 3) 안의 시각(초): 켜짐 → 꺼짐 → 이야기 반응(녹화한 화면과 같은 간격) → 다음 수업에서 떠올리기.
 */
export const TURN = { on: 2.1, off: 8.17, react: 11.25, recallAt: 14.6, recall: 15.0, meaning: 17.5, english: 19.7, end: 21.6 };

/** 간식 장면(장면 5) 앞에 붙은 "말로 간식 주기" 부분의 길이(초). 그 뒤의 시각은 모두 이만큼 밀립니다. */
export const FEED_INTRO = 4.5;

/** 비밀 버튼 장면 안의 시각(초). clipCalm은 clips/secret.mp4에서 달래는 말이 시작되는 때. */
export const SECRET = { press: 3.5, calm1: 4.3, calm2: 9.2, end: 14.3, clipCalm: 7.97 };

/** 문장 장면 안의 시각(초). clipAsk는 clips/sentence.mp4에서 "‘큰 개’. 영어로 뭐였지?"가 나오는 때. */
export const SENTENCE = { ask: 3.5, pass: 8.03, english: 9.46, card2: 11.6, end: 14.5, clipAsk: 3.77 };

/** 낱말 장면 안의 시각(초): 규모 → 상황에 맞는 낱말 → 아이 속도에 맞춘 양. */
export const WORDS = { pick: 5.4, pace: 10.6, end: 17.3 };

/*
 * 장면 순서: 주요 기능을 차례로 소개하되, 앞 장면을 보고 궁금해질 즈음에 그 답이 되는 장면이 오게 합니다.
 * 문제 → 제목 → 매일 새로 쓰는 동화(6) → 동화 사이 영어 수업, 통과하면 간식(4) → 간식으로 자라는 강아지(5)
 * → 배운 말의 복습(7)과 문장으로의 확장 → 무엇을 얼마나 배우는지(낱말) → 차의 움직임과 함께, 운전은 방해하지 않게(3, 내비)
 * → 아이가 울 때(비밀 버튼) → 마무리(8).
 * 장면 6의 내비 부분(STORY.navi부터)은 떼어 깜빡이 장면 뒤에 둡니다.
 */
const T6 = T2 + 6.4;
const T4 = T6 + STORY.navi;
const T5 = T4 + 12;
const T7 = T5 + 13.6 + FEED_INTRO;
const TN = T7 + 7;
const TW = TN + SENTENCE.end;
const T3 = TW + WORDS.end;
const TNAV = T3 + TURN.end;
const TS = TNAV + (STORY.end - STORY.navi);
const T8 = TS + SECRET.end;

/** 장면이 시작하는 프레임. 장면 2는 동그란 전환이 겹치도록 조금 일찍 시작합니다. */
export const SCENE = {
  one: { from: 0, duration: s(INTRO.end) },
  two: { from: s(T2), duration: s(6.4) },
  six: { from: s(T6), duration: s(STORY.navi) },
  four: { from: s(T4), duration: s(12) },
  five: { from: s(T5), duration: s(13.6 + FEED_INTRO) },
  three: { from: s(T3), duration: s(TURN.end) },
  /** 장면 6의 내비 부분(STORY.navi부터 끝까지)을 깜빡이 장면 뒤에 따로 보여 줍니다. */
  navi: { from: s(TNAV), duration: s(STORY.end - STORY.navi) },
  words: { from: s(TW), duration: s(WORDS.end) },
  secret: { from: s(TS), duration: s(SECRET.end) },
  seven: { from: s(T7), duration: s(7) },
  sentence: { from: s(TN), duration: s(SENTENCE.end) },
  eight: { from: s(T8), duration: s(7.9) },
};

/** 영상 전체 길이(프레임). */
export const TOTAL = SCENE.eight.from + SCENE.eight.duration;

/** at: 영상 전체 기준 시작(초), seconds: 들려줄 길이, trim: 파일 앞을 잘라 낼 길이(초). */
export type Cue = { at: number; src: string; seconds: number; text?: string; rate?: number; volume?: number; trim?: number };

/** 내레이터(어른 목소리). text는 자막으로도 나옵니다. */
export const NARRATION: Cue[] = [
  { at: 0.5, src: "demo/vo/a_n1.mp3", seconds: 3.12, text: "차에 타면 꼭 나오는 말. “아직 멀었어?”" },
  { at: INTRO.driver + 0.2, src: "demo/vo/a_i1.mp3", seconds: 4.72, text: "운전하는 부모는 앞만 봐야 하고, 뒷자리의 아이는 금세 지루해집니다." },
  { at: INTRO.tablet + 0.3, src: "demo/vo/a_i2.mp3", seconds: 4.56, text: "그래서 결국 태블릿을 쥐여 주지만, 마음 한편은 편하지 않습니다." },
  { at: INTRO.worry + 0.3, src: "demo/vo/a_i3.mp3", seconds: 5.28, text: "영어 공부는 일찍부터 시키고 싶은데, 매일 차 안의 시간은 그냥 흘러갑니다." },
  { at: INTRO.friend + 0.3, src: "demo/vo/a_i4.mp3", seconds: 6.88, text: "화면만 쥐여 주는 대신, 옆자리에서 같이 공부하며 영어 동화를 들려주는 친구가 있다면 어떨까요?" },
  { at: T2 + 0.7, src: "demo/vo/a_n2b.mp3", seconds: 4.56, text: "이동하는 시간이, 강아지와 함께 영어를 배우는 동화 시간이 됩니다." },
  { at: T3 + 0.3, src: "demo/vo/a_r3a.mp3", seconds: 4.72, text: "운전자가 깜빡이를 켜고 도는 동안, 강아지는 조용히 기다립니다." },
  { at: T3 + 8.25, src: "demo/vo/a_r3b.mp3", seconds: 2.96, text: "다 돌고 나면, 이야기가 그 일을 받아 줍니다." },
  { at: T4 + 0.1, src: "demo/vo/a_n4d.mp3", seconds: 1.9, text: "이야기 사이엔, 영어 수업." },
  { at: T4 + 9.5, src: "demo/vo/a_n4b.mp3", seconds: 2.0, text: "통과하면 간식이 생깁니다." },
  // 낱말 장면
  { at: TW + 0.3, src: "demo/vo/a_v1.mp3", seconds: 4.56, text: "낱말만 천 개가 넘고, 표현과 문장도 삼백 개가 넘습니다." },
  { at: TW + WORDS.pick + 0.3, src: "demo/vo/a_v2.mp3", seconds: 4.32, text: "그중 무엇을 먼저 배울지는, 차가 지금 상황을 보고 고릅니다." },
  { at: TW + WORDS.pace + 0.3, src: "demo/vo/a_v3.mp3", seconds: 6.0, text: "잘 따라 하면 조금 더, 힘들어하면 조금 덜. 아이의 속도에 맞춥니다." },
  { at: T5 + 0.3, src: "demo/vo/a_r5a.mp3", seconds: 6.0, text: "도착해서 아이가 “Here you are!” 하고 말하면, 강아지가 간식을 받아먹고 자랍니다." },
  { at: T5 + FEED_INTRO + 5.2, src: "demo/vo/a_n5b.mp3", seconds: 1.76, text: "이름은 아이가 지어 주고요." },
  { at: T5 + FEED_INTRO + 7.9, src: "demo/vo/a_n5c.mp3", seconds: 4.24, text: "집에는 새 물건이 열리고, 배운 말은 카드로 모입니다." },
  // 장면 6: 이야기
  { at: T6 + 0.3, src: "demo/vo/a_m1.mp3", seconds: 3.1, text: "이야기는 매일, 생성형 AI가 새로 씁니다." },
  { at: T6 + 4.0, src: "demo/vo/a_m2.mp3", seconds: 3.36, text: "같은 길이어도, 어제와는 다른 모험이 됩니다." },
  { at: T6 + 7.9, src: "demo/vo/a_m3.mp3", seconds: 2.64, text: "그리고 그날의 상황에 맞춰 달라집니다." },
  { at: T6 + 10.9, src: "demo/vo/a_m4.mp3", seconds: 2.32, text: "부모가 오늘 있었던 일을 적어 두면," },
  { at: T6 + 18.2, src: "demo/vo/a_m5.mp3", seconds: 1.84, text: "강아지가 먼저 물어봐 줍니다." },
  { at: T6 + STORY.fit + 0.3, src: "demo/vo/a_n6b.mp3", seconds: 4.64, text: "목적지까지 남은 시간에 꼭 맞게, 오늘 배울 말을 담아서." },
  { at: TNAV + 0.3, src: "demo/vo/a_r6c.mp3", seconds: 6.16, text: "내비가 안내하고 차가 도는 동안은 기다렸다가, 다 돈 뒤에 이야기로 받아 줍니다." },
  // 비밀 버튼
  { at: TS + 0.3, src: "demo/vo/a_m6.mp3", seconds: 2.96, text: "아이가 울 때는, 어른만 아는 비밀 버튼." },
  { at: T7 + 0.3, src: "demo/vo/a_r7.mp3", seconds: 5.6, text: "어제 배운 말은 오늘 다시 묻고, 다 익힌 말도 잊지 않게 다시 닦아 줍니다." },
  { at: TN + 0.3, src: "demo/vo/a_s1.mp3", seconds: 3.12, text: "익힌 낱말이 모이면, 짧은 표현이 되고," },
  { at: TN + SENTENCE.card2, src: "demo/vo/a_s2.mp3", seconds: 2.56, text: "강아지가 더 자라면, 문장이 됩니다." },
  { at: T8 + 0.4, src: "demo/vo/a_n8.mp3", seconds: 4.16, text: "“아직 멀었어?”가 “벌써 다 왔어?”가 되는 시간." },
];

/** 앱이 실제로 내는 강아지 목소리(앱에 저장된 음성 그대로). 영어는 앱처럼 0.8배로 천천히. */
export const APP_VOICE: Cue[] = [
  // 장면 3: 깜빡이를 끈 뒤의 이야기 반응(첫 문장만), 다음 수업에서 떠올리기
  { at: T3 + TURN.react, src: "demo/app/react.mp3", seconds: 2.94 },
  { at: T3 + TURN.recall, src: "demo/app/recall.mp3", seconds: 2.32 },
  { at: T3 + TURN.meaning, src: "demo/app/leftq.mp3", seconds: 2.08 },
  { at: T3 + TURN.english, src: "demo/app/left_en.mp3", seconds: 0.8, rate: 0.8 },
  // 장면 4: 녹화한 화면과 같은 간격(질문은 장면 시작 2.1초 뒤)
  { at: T4 + 2.1, src: "demo/app/cloud_q.mp3", seconds: 2.3 },
  { at: T4 + 6.0, src: "demo/app/right_answer.mp3", seconds: 0.9 },
  { at: T4 + 7.5, src: "demo/app/cloud_en.mp3", seconds: 0.8, rate: 0.8 },
  { at: T4 + 8.63, src: "demo/app/treat.mp3", seconds: 0.78 }, // "딩동!"까지만
  // 장면 5: 아이가 "Here you are!"라고 말해 간식을 받은 강아지
  { at: T5 + 6.45, src: "demo/app/feed_ok.mp3", seconds: 3.12 },
  // 장면 6: 오늘 있었던 일을 묻고 위로하는 말, 내비 안내를 받아 주는 말("길 안내 요정이 오른쪽으로 돌아가래." 부분만)
  { at: T6 + STORY.ask, src: "demo/app/note_ask.mp3", seconds: STORY.askLen },
  { at: T6 + STORY.comfort, src: "demo/app/note_comfort.mp3", seconds: 3.92 },
  { at: TNAV + 6.6, src: "demo/app/navi_left.mp3", seconds: 4.06, trim: 2.0 },
  // 문장 장면: 녹화한 화면과 같은 간격
  { at: TN + SENTENCE.ask, src: "demo/app/bigdog_q.mp3", seconds: 2.56 },
  { at: TN + SENTENCE.pass, src: "demo/app/right_answer.mp3", seconds: 0.9 },
  { at: TN + SENTENCE.english, src: "demo/app/bigdog_en.mp3", seconds: 1.52, rate: 0.8 },
  // 비밀 버튼
  { at: TS + SECRET.calm1, src: "demo/app/calm_1.mp3", seconds: 4.64 },
  { at: TS + SECRET.calm2, src: "demo/app/calm_2.mp3", seconds: 4.56 },
];

const sfx = (at: number, name: string, volume: number, seconds = 0.5): Cue => ({ at, src: `demo/sfx/${name}.wav`, seconds, volume });

/** 효과음. */
export const SFX: Cue[] = [
  sfx(2.5, "pop", 0.35), sfx(3.7, "pop", 0.45), sfx(4.7, "pop", 0.6),
  // 도입의 각 부분과 제목 장면
  sfx(INTRO.driver - 0.1, "whoosh", 0.5), sfx(INTRO.driver + 0.3, "pop", 0.3), sfx(INTRO.driver + 0.75, "pop", 0.3),
  sfx(INTRO.tablet - 0.1, "whoosh", 0.5), sfx(INTRO.tablet + 0.3, "pop", 0.35),
  sfx(INTRO.worry - 0.1, "whoosh", 0.5), sfx(INTRO.worry + 0.35, "pop", 0.3), sfx(INTRO.worry + 0.75, "pop", 0.3),
  sfx(INTRO.friend - 0.1, "whoosh", 0.5), sfx(INTRO.friend + 0.5, "sparkle", 0.3, 1.3),
  ...[0, 1, 2].map((i) => sfx(INTRO.friend + (40 + i * 14) / 30, "pop", 0.3)),
  sfx(T2 - 0.05, "whoosh", 0.8), sfx(T2 + 0.75, "thud", 0.5), sfx(T2 + 1.7, "sparkle", 0.3, 1.3),
  sfx(T3 - 0.1, "whoosh", 0.7), sfx(T3 + TURN.react - 0.1, "pop", 0.3),
  sfx(T3 + TURN.recallAt - 0.2, "whoosh", 0.7), sfx(T3 + TURN.recall - 0.05, "pop", 0.35), sfx(T3 + TURN.english - 0.05, "sparkle", 0.3, 1.3),
  sfx(T4 - 0.1, "whoosh", 0.7), sfx(T4 + 4.9, "pop", 0.45), sfx(T4 + 5.76, "ding", 0.2, 1.4),
  // 낱말 장면
  sfx(TW - 0.1, "whoosh", 0.7), sfx(TW + 0.4, "sparkle", 0.25, 1.3), sfx(TW + WORDS.pick - 0.2, "whoosh", 0.5),
  sfx(TW + WORDS.pick + 0.6, "pop", 0.3), sfx(TW + WORDS.pick + 1.0, "pop", 0.3), sfx(TW + WORDS.pick + 1.4, "pop", 0.3), sfx(TW + WORDS.pick + 1.8, "pop", 0.3),
  sfx(TW + WORDS.pace - 0.2, "whoosh", 0.5),
  // 장면 5
  sfx(T5 - 0.1, "whoosh", 0.7), sfx(T5 + 3.3, "pop", 0.35),
  ...[1.1, 1.9, 2.7].map((at) => sfx(T5 + FEED_INTRO + at, "pop", 0.4)), sfx(T5 + FEED_INTRO + 3.5, "sparkle", 0.3, 1.3),
  sfx(T5 + FEED_INTRO + 5.0, "pop", 0.3), sfx(T5 + FEED_INTRO + 7.6, "whoosh", 0.6), sfx(T5 + FEED_INTRO + 10.2, "pop", 0.3),
  // 장면 6
  sfx(T6 - 0.1, "whoosh", 0.7), sfx(T6 + 0.4, "pop", 0.3), sfx(T6 + 0.9, "pop", 0.3), sfx(T6 + 1.45, "pop", 0.3),
  sfx(T6 + STORY.chips - 0.2, "whoosh", 0.5), sfx(T6 + STORY.chips + 1.6, "sparkle", 0.25, 1.3),
  sfx(T6 + STORY.note - 0.2, "whoosh", 0.5), sfx(T6 + STORY.ask - 0.1, "pop", 0.35),
  sfx(T6 + STORY.fit - 0.2, "whoosh", 0.5), sfx(TNAV - 0.1, "whoosh", 0.6), sfx(TNAV + 6.55, "pop", 0.4),
  // 비밀 버튼
  sfx(TS - 0.1, "whoosh", 0.7), sfx(TS + SECRET.calm1 - 0.1, "ding", 0.18, 1.4),
  // 장면 7, 8
  sfx(T7 - 0.1, "whoosh", 0.7), sfx(T7 + 3.9, "ding", 0.2, 1.4),
  sfx(TN - 0.1, "whoosh", 0.7), sfx(TN + 1.6, "sparkle", 0.25, 1.3), sfx(TN + SENTENCE.pass - 0.2, "ding", 0.2, 1.4), sfx(TN + SENTENCE.card2, "pop", 0.4),
  sfx(T8 - 0.1, "whoosh", 0.7), sfx(T8 + 2.4, "sparkle", 0.3, 1.3),
  // 방향지시등의 똑·딱(켜짐부터 꺼짐까지, 0.67초마다)
  ...Array.from({ length: Math.floor((TURN.off - TURN.on) * 1.5) }, (_, i) => [
    sfx(T3 + TURN.on + i * (2 / 3), "tick", 0.2, 0.1),
    sfx(T3 + TURN.on + i * (2 / 3) + 1 / 3, "tock", 0.16, 0.1),
  ]).flat(),
];

/** 누가 말하는 동안에는 음악을 낮춥니다. */
export const musicVolume = (frame: number): number => {
  const t = frame / 30;
  const open = 0.42;
  const ducked = 0.19;
  let level = open;
  for (const cue of [...NARRATION, ...APP_VOICE]) {
    const start = cue.at - 0.25;
    const end = cue.at + cue.seconds + 0.25;
    if (t <= start - 0.3 || t >= end + 0.5) continue;
    const k = t < start ? (t - (start - 0.3)) / 0.3 : t > end ? 1 - (t - end) / 0.5 : 1;
    level = Math.min(level, open + (ducked - open) * k);
  }
  const fadeOut = Math.min(1, Math.max(0, (TOTAL - frame) / 20));
  return level * fadeOut;
};

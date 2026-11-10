import { Audio } from "@remotion/media";
import { AbsoluteFill, Easing, Sequence, interpolate, staticFile, useCurrentFrame } from "remotion";
import { Caption } from "./parts/Caption";
import { Sky } from "./parts/Sky";
import { Scene1 } from "./scenes/Scene1";
import { Scene2 } from "./scenes/Scene2";
import { Scene3 } from "./scenes/Scene3";
import { Scene4 } from "./scenes/Scene4";
import { Scene5 } from "./scenes/Scene5";
import { Scene6 } from "./scenes/Scene6";
import { Scene7 } from "./scenes/Scene7";
import { Scene8 } from "./scenes/Scene8";
import { SceneSecret } from "./scenes/SceneSecret";
import { SceneSentence } from "./scenes/SceneSentence";
import { SceneWords } from "./scenes/SceneWords";
import { s } from "./theme";
import { APP_VOICE, Cue, NARRATION, SCENE, SFX, TOTAL, musicVolume } from "./timeline";

/** 장면이 바뀔 때 부드럽게 나타나고 사라지게 합니다. */
const Fade: React.FC<{ duration: number; fadeOut: boolean; children: React.ReactNode }> = ({ duration, fadeOut, children }) => {
  const frame = useCurrentFrame();
  const opacity = Math.min(
    interpolate(frame, [0, 6], [0, 1], { extrapolateRight: "clamp" }),
    fadeOut ? interpolate(frame, [duration - 8, duration - 1], [1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }) : 1,
  );
  return <AbsoluteFill style={{ opacity }}>{children}</AbsoluteFill>;
};

const LATER = [
  { scene: SCENE.three, node: <Scene3 />, fadeOut: true },
  { scene: SCENE.four, node: <Scene4 />, fadeOut: true },
  { scene: SCENE.words, node: <SceneWords duration={SCENE.words.duration} />, fadeOut: true },
  { scene: SCENE.five, node: <Scene5 duration={SCENE.five.duration} />, fadeOut: true },
  { scene: SCENE.six, node: <Scene6 duration={SCENE.six.duration} />, fadeOut: true },
  { scene: SCENE.secret, node: <SceneSecret />, fadeOut: true },
  { scene: SCENE.seven, node: <Scene7 />, fadeOut: true },
  { scene: SCENE.sentence, node: <SceneSentence />, fadeOut: true },
  { scene: SCENE.eight, node: <Scene8 duration={SCENE.eight.duration} />, fadeOut: false },
];

/** 밤하늘 세계(장면 2부터). 장면 1의 뒷좌석 창에서 동그랗게 열립니다. */
const NightWorld: React.FC = () => {
  const frame = useCurrentFrame();
  const radius = interpolate(frame, [0, 20], [0, 2300], { extrapolateRight: "clamp", easing: Easing.bezier(0.5, 0, 0.2, 1) });
  return (
    <AbsoluteFill style={{ clipPath: `circle(${radius}px at 398px 707px)` }}>
      <Sky />
      <Sequence durationInFrames={SCENE.two.duration} layout="none">
        <Scene2 duration={SCENE.two.duration} />
      </Sequence>
      {LATER.map(({ scene, node, fadeOut }) => (
        <Sequence key={scene.from} from={scene.from - SCENE.two.from} durationInFrames={scene.duration} layout="none">
          <Fade duration={scene.duration} fadeOut={fadeOut}>
            {node}
          </Fade>
        </Sequence>
      ))}
    </AbsoluteFill>
  );
};

const Sound: React.FC<{ cue: Cue; volume: number }> = ({ cue, volume }) => (
  <Sequence from={s(cue.at)} durationInFrames={s(cue.seconds / (cue.rate ?? 1)) + (cue.trim || cue.src.endsWith("treat.mp3") ? 0 : 20)} layout="none">
    <Audio src={staticFile(cue.src)} volume={cue.volume ?? volume} playbackRate={cue.rate ?? 1} trimBefore={cue.trim ? s(cue.trim) : undefined} />
  </Sequence>
);

/** 「아직 멀었어?」 시연 영상(장면 1~8). */
export const Demo: React.FC = () => (
  <AbsoluteFill style={{ background: "#0A0920" }}>
    <Sequence durationInFrames={SCENE.one.duration + 10} layout="none">
      <Scene1 />
    </Sequence>
    <Sequence from={SCENE.two.from} layout="none">
      <NightWorld />
    </Sequence>
    <Caption />

    <Audio src={staticFile("demo/music.wav")} volume={musicVolume} />
    {NARRATION.map((cue) => (
      <Sound key={cue.src} cue={cue} volume={0.88} />
    ))}
    {APP_VOICE.map((cue, i) => (
      <Sound key={i} cue={cue} volume={0.88} />
    ))}
    {SFX.map((cue, i) => (
      <Sound key={i} cue={cue} volume={0.5} />
    ))}
  </AbsoluteFill>
);

export const DEMO_FRAMES = TOTAL;

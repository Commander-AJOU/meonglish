import { Composition } from "remotion";
import { FPS, MOON_LOOP, RAIN_LOOP } from "./loop";
import { MoonIsland } from "./MoonIsland";
import { RainLayer } from "./RainLayer";
import { DEMO_FRAMES, Demo } from "./demo/Demo";

/**
 * 앱에 넣는 그림은 scripts/export.sh가 뽑습니다.
 * 강아지 그림은 여기서 만들지 않고 AI 이미지 생성으로 만들었습니다(docs/puppy-prompts.md).
 */
export const RemotionRoot: React.FC = () => {
  return (
    <>
      {/* 시연 영상(가로 16:9). scripts/make_music.py, scripts/narrate.sh로 소리를 먼저 만듭니다. */}
      <Composition id="Demo" component={Demo} durationInFrames={DEMO_FRAMES} fps={30} width={1920} height={1080} />
      <Composition
        id="MoonIsland"
        component={MoonIsland}
        durationInFrames={MOON_LOOP}
        fps={FPS}
        width={600}
        height={600}
      />
      <Composition
        id="RainLayer"
        component={RainLayer}
        durationInFrames={RAIN_LOOP}
        fps={FPS}
        width={960}
        height={540}
      />
    </>
  );
};

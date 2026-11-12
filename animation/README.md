# animation

앱에 들어가는 움직이는 그림과 시연 영상을 만드는 [Remotion](https://www.remotion.dev) 프로젝트입니다.

- `src/` — 달빛 섬과 비 장면(투명 배경의 움직이는 WebP로 뽑아 앱에 넣음)
- `src/demo/` — 시연 영상(`Demo` 합성). 장면 시각·자막·소리는 `src/demo/timeline.ts` 한 곳에서 맞춥니다.
- `scripts/` — 그림 뽑기(`export.sh`), 강아지 그림 배경 떼기(`keyout.sh`), 음악·효과음 합성(`make_music.py`),
  내레이션 만들기(`narrate.sh`), 앱이 만들어 둔 강아지 목소리 꺼내기(`pull_voice.py`)

```bash
npm install
npm run dev                                                    # 미리 보기
npx remotion render Demo ../docs/demo/ajik-meoreosseo-demo.mp4   # 시연 영상 만들기
```

만드는 순서와 설명은 저장소 첫 화면의 README("장면 그림", "시연 영상")에 있습니다.
Remotion은 개인과 3인 이하 조직에 무료입니다([라이선스](https://github.com/remotion-dev/remotion/blob/main/LICENSE.md)).

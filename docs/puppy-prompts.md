# 강아지 캐릭터 이미지 생성 프롬프트 (Gemini용)

앱에서 그대로 쓸 수 있게, 아래 규칙대로 뽑아 주세요.

## 진행 순서

1. **P0(3단계)** 를 먼저 만들어 마음에 드는 결과를 하나 고릅니다. 이게 기준 그림(마스터)입니다.
2. 나머지 프롬프트는 **마스터 이미지를 첨부한 채** 보냅니다. 그래야 같은 캐릭터로 나옵니다.
3. 결과는 PNG, 가능한 가장 큰 크기로 저장하고 아래 파일 이름을 붙여 이 대화에 올려 주세요.

## 통과 기준 (하나라도 어긋나면 다시 생성)

- 배경이 **완전히 균일한 초록(#00FF00)** 이어야 합니다. 그라데이션·바닥·그림자·빛번짐·체커보드 무늬가 있으면 배경을 못 뗍니다.
- 몸 전체가 잘리지 않고 가운데에 있어야 합니다.
- 캐릭터 한 마리만, 글자·워터마크·소품 없음.
- 얼굴 특징(오른쪽 눈 위 갈색 점, 늘어진 갈색 귀, 크림색 몸)이 모든 단계에서 같아야 합니다.

## 필수 5장

### P0 — 3단계 (Lv.30, 기준 그림) → `puppy_3.png`

```
A cute chibi puppy character for a children's mobile game, soft 3D clay-like render in Pixar toy style, smooth matte surfaces, no visible fur strands. Golden-cream colored puppy with floppy caramel-brown ears and one caramel-brown patch over its right eye, big round glossy dark-brown eyes with white highlights, tiny black button nose, small pink tongue sticking out, rosy cheeks. Sitting upright, facing the viewer, front view, tail curled up to the side. Wearing a simple red collar with a small round gold tag. Full body visible, centered, nothing cropped, occupying about 75% of the image height. Soft even studio lighting with a subtle cool blue rim light. Plain solid flat bright green background (#00FF00), no ground shadow, no floor, no gradient, no glow, no text, no watermark, no other objects. Square 1:1 image.
```

### P1 — 1단계 (Lv.1, 갓 태어난 강아지) → `puppy_1.png`  *(마스터 첨부)*

```
Using the attached image as the exact same character (same colors, same brown patch over the right eye, same style and lighting), draw its NEWBORN version: a tiny round baby puppy lying curled up on its belly, eyes gently closed and sleeping peacefully, ears still small and folded, soft round body, paws tucked in, no collar. Full body visible, centered, occupying about 70% of the image width. Plain solid flat bright green background (#00FF00), no ground shadow, no floor, no gradient, no text, nothing else. Square 1:1 image.
```

### P2 — 2단계 (Lv.10, 아기 강아지) → `puppy_2.png`  *(마스터 첨부)*

```
Using the attached image as the exact same character (same colors, same brown patch over the right eye, same style and lighting), draw its YOUNG PUPPY version: very big head compared to its small body, short stubby legs, sitting on its bottom with front paws on the ground, eyes open wide and curious, floppy ears, no collar. Full body visible, centered, occupying about 70% of the image height. Plain solid flat bright green background (#00FF00), no ground shadow, no floor, no gradient, no text, nothing else. Square 1:1 image.
```

### P4 — 4단계 (Lv.60, 소년 강아지) → `puppy_4.png`  *(마스터 첨부)*

```
Using the attached image as the exact same character (same colors, same brown patch over the right eye, same style and lighting), draw it OLDER: a slightly taller and leaner body, a bit longer snout, sitting up proudly, wearing a blue neckerchief bandana instead of the red collar. Full body visible, centered, occupying about 78% of the image height. Plain solid flat bright green background (#00FF00), no ground shadow, no floor, no gradient, no text, nothing else. Square 1:1 image.
```

### P5 — 5단계 (Lv.100, 다 자란 개) → `puppy_5.png`  *(마스터 첨부)*

```
Using the attached image as the exact same character (same colors, same brown patch over the right eye, same style and lighting), draw it FULLY GROWN: an adult dog sitting tall and proud, longer snout, calm confident smile, wearing a small red hero cape over its shoulders and a gold star medal on a ribbon around its neck. Full body visible, centered, occupying about 82% of the image height. Plain solid flat bright green background (#00FF00), no ground shadow, no floor, no gradient, no text, nothing else. Square 1:1 image.
```

## 있으면 좋은 것 (여유 될 때)

### 간식 먹고 기뻐하는 표정 → `happy_grid.png` (2~5단계를 2×2 한 장으로)  *(2~5단계 이미지 첨부)*

```
Edit the attached image: keep the exact same character, pose, outfit, framing and green background. Change only the face: eyes squeezed shut in joy like a happy ^ ^ expression, a big open happy smile, cheeks slightly puffed, tongue out. Nothing else changes.
```

### 눈 감은 장면 (눈 깜빡임용) → `blink_grid.png` (2~5단계를 2×2 한 장으로)  *(2~5단계 이미지 첨부)*

```
Edit the attached image: keep everything exactly the same (character, pose, outfit, framing, green background). Change only the eyes: both eyes gently closed as a soft curved line, relaxed expression. Nothing else changes.
```

### 간식 (뼈다귀 과자) → `treat.png`

```
A single small dog treat: a cream-colored bone-shaped biscuit with slightly toasted edges, cute children's game item, soft 3D clay-like render in Pixar toy style, matching a chibi puppy character. Centered, occupying about 70% of the image width, viewed from slightly above. Plain solid flat bright green background (#00FF00), no ground shadow, no floor, no gradient, no text, nothing else. Square 1:1 image.
```

## 앱에 넣기

그림을 한 폴더에 모아 `cd animation && bash scripts/keyout.sh <그림 폴더>`를 실행하면 초록 배경을 떼어
`app/src/main/res/drawable-nodpi/`에 넣습니다. 2×2 격자는 왼쪽 위부터 2, 3, 4, 5단계 순서입니다.

## 앱에서 하는 일

초록 배경을 떼어 투명 이미지로 만들고, 숨쉬기(살짝 커졌다 작아짐)·눈 깜빡임(blink 그림 교체)·간식 먹을 때 폴짝 뛰기·단계가 오를 때 빛나는 진화 연출을 앱이 붙입니다. 크기는 단계별로 앱에서 맞추니 그림 안에서 크기를 정확히 맞출 필요는 없습니다.

# 강아지 집 꾸미기 그림 프롬프트 (Gemini용)

## 앱에서 이렇게 씁니다

- 홈에 "콩이네 집" 버튼이 생기고, 들어가면 구름 위의 아늑한 방이 보입니다. 가운데에 강아지가 앉아 있습니다.
- 방 안 물건은 **단계가 오를 때마다 하나씩 열립니다**. 열린 물건은 아래 상자에 나타나고, 아이가 누르면 방의 정해진 자리에 놓입니다.
  놓인 물건을 다시 누르면 상자로 돌아갑니다(놓을지 말지, 무엇을 꺼낼지를 아이가 정하는 방식).
- 그래서 그림은 **방 배경 1장 + 물건 10장**이면 됩니다. 물건은 각각 한 장이고, 색깔 변형은 없습니다.

| 열리는 단계 | 물건 | 파일 이름 | 방에서 놓이는 자리 |
|---|---|---|---|
| 5 | 폭신한 강아지 방석 | `item_bed.png` | 가운데 뒤쪽(강아지가 그 위에 앉음) |
| 10 | 밥그릇 | `item_bowl.png` | 오른쪽 앞 |
| 20 | 빨간 공 | `item_ball.png` | 왼쪽 앞 |
| 30 | 동그란 러그 | `item_rug.png` | 바닥 가운데(가장 뒤 겹) |
| 40 | 별 모양 스탠드 램프 | `item_lamp.png` | 오른쪽 뒤 |
| 50 | 뼈다귀 그림 액자 | `item_frame.png` | 뒷벽 왼쪽 |
| 60 | 작은 개집 | `item_house.png` | 왼쪽 뒤 |
| 70 | 화분 | `item_plant.png` | 뒷벽 오른쪽 |
| 85 | 장난감 상자 | `item_toybox.png` | 왼쪽 가운데 |
| 100 | 황금 트로피 | `item_trophy.png` | 뒷벽 가운데 선반 |

## 진행 순서

1. 방 배경(B0)을 먼저 뽑습니다. 배경은 초록이 아니라 **그대로 쓰는 그림**이라 통과 기준이 다릅니다.
2. 물건은 모두 **강아지 기준 그림(`puppy_3`)을 첨부**하고 보냅니다. 같은 재질·조명으로 나오게 하기 위해서입니다.
3. 결과는 PNG로 저장해서 위 파일 이름을 붙여 이 대화에 올려 주세요.

## 통과 기준

- 물건: 배경이 완전히 균일한 초록(#00FF00), 바닥 그림자 없음, 물건 하나만, 글자 없음, 잘리지 않음. (강아지 때와 같음)
- 배경: 16:9 가로, **방 가운데와 앞쪽 바닥이 비어 있어야** 합니다(물건과 강아지가 거기 들어감). 캐릭터·물건·글자 없음.

## B0 — 방 배경 → `room.png`

```
A cozy little puppy room floating on a fluffy cloud in the night sky, for a children's mobile game, soft 3D clay-like render in Pixar toy style, matching a chibi puppy character. Wide 16:9 scene viewed straight from the front, slightly from above. Back wall is a rounded cream-and-lavender wall with one round window showing a deep navy night sky (#17135F to #7C5CF0 gradient) with tiny stars and a crescent moon. The floor is a soft white cloud. The center of the room and the front floor must be completely EMPTY (no furniture, no objects, no characters) so items can be placed there later. Only the walls, window and cloud floor. Warm soft lighting, no text, no watermark.
```

## 물건 (모두 강아지 기준 그림 첨부)

공통 문구가 길어 보여도 그대로 붙여 넣으면 됩니다. 물건 설명만 다릅니다.

### I1 — 방석 (5단계) → `item_bed.png`

```
Match the style, material and lighting of the attached puppy character (soft 3D clay-like render, Pixar toy style, smooth matte surfaces). A single object only: a round fluffy puppy cushion bed, soft pastel blue with a cream rim, viewed from the front slightly above. Centered, occupying about 70% of the image width. Plain solid flat bright green background (#00FF00), no ground shadow, no floor, no gradient, no text, nothing else. Square 1:1 image.
```

### I2 — 밥그릇 (10단계) → `item_bowl.png`

```
Match the style, material and lighting of the attached puppy character (soft 3D clay-like render, Pixar toy style, smooth matte surfaces). A single object only: a cute dog food bowl, pastel yellow with a small bone symbol on the side, filled with cream-colored kibble, viewed from the front slightly above. Centered, occupying about 60% of the image width. Plain solid flat bright green background (#00FF00), no ground shadow, no floor, no gradient, no text, nothing else. Square 1:1 image.
```

### I3 — 공 (20단계) → `item_ball.png`

```
Match the style, material and lighting of the attached puppy character (soft 3D clay-like render, Pixar toy style, smooth matte surfaces). A single object only: a bouncy red rubber ball with one white star on it, viewed from the front slightly above. Centered, occupying about 55% of the image width. Plain solid flat bright green background (#00FF00), no ground shadow, no floor, no gradient, no text, nothing else. Square 1:1 image.
```

### I4 — 러그 (30단계) → `item_rug.png`

```
Match the style, material and lighting of the attached puppy character (soft 3D clay-like render, Pixar toy style, smooth matte surfaces). A single object only: a round soft rug lying flat on the floor, pastel pink with a cream paw-print pattern, viewed from the front slightly above so it looks like a wide ellipse. Centered, occupying about 80% of the image width. Plain solid flat bright green background (#00FF00), no ground shadow, no floor, no gradient, no text, nothing else. Square 1:1 image.
```

### I5 — 별 램프 (40단계) → `item_lamp.png`

```
Match the style, material and lighting of the attached puppy character (soft 3D clay-like render, Pixar toy style, smooth matte surfaces). A single object only: a small cute floor lamp with a glowing yellow star-shaped lampshade on a short cream stand, softly lit, viewed from the front. Centered, occupying about 65% of the image height. Plain solid flat bright green background (#00FF00), no ground shadow, no floor, no gradient, no glow spilling onto the background, no text, nothing else. Square 1:1 image.
```

### I6 — 뼈다귀 액자 (50단계) → `item_frame.png`

```
Match the style, material and lighting of the attached puppy character (soft 3D clay-like render, Pixar toy style, smooth matte surfaces). A single object only: a small square picture frame, rounded wooden frame in warm caramel color, with a simple picture of a cream bone on a pastel blue background inside, viewed straight from the front. Centered, occupying about 60% of the image width. Plain solid flat bright green background (#00FF00), no ground shadow, no wall, no gradient, no text, nothing else. Square 1:1 image.
```

### I7 — 개집 (60단계) → `item_house.png`

```
Match the style, material and lighting of the attached puppy character (soft 3D clay-like render, Pixar toy style, smooth matte surfaces). A single object only: a small cute dog house, cream walls with a rounded red roof and a round dark doorway, a tiny bone sign above the door, viewed from the front slightly from the left. Centered, occupying about 75% of the image height. Plain solid flat bright green background (#00FF00), no ground shadow, no floor, no gradient, no text, nothing else. Square 1:1 image.
```

### I8 — 화분 (70단계) → `item_plant.png`

```
Match the style, material and lighting of the attached puppy character (soft 3D clay-like render, Pixar toy style, smooth matte surfaces). A single object only: a small potted plant, round pastel blue pot with a chubby green leafy plant, viewed from the front. Centered, occupying about 65% of the image height. Plain solid flat bright green background (#00FF00), no ground shadow, no floor, no gradient, no text, nothing else. Square 1:1 image. Make sure the plant's green is a dark bluish green, clearly different from the bright green background.
```

### I9 — 장난감 상자 (85단계) → `item_toybox.png`

```
Match the style, material and lighting of the attached puppy character (soft 3D clay-like render, Pixar toy style, smooth matte surfaces). A single object only: a small open toy box, pastel yellow with rounded corners and a blue lid leaning open, with a rope toy and a rubber duck peeking out, viewed from the front slightly above. Centered, occupying about 70% of the image width. Plain solid flat bright green background (#00FF00), no ground shadow, no floor, no gradient, no text, nothing else. Square 1:1 image.
```

### I10 — 트로피 (100단계) → `item_trophy.png`

```
Match the style, material and lighting of the attached puppy character (soft 3D clay-like render, Pixar toy style, smooth matte surfaces). A single object only: a shiny gold trophy cup with two little handles and a gold star on the front, on a small navy base, viewed from the front. Centered, occupying about 70% of the image height. Plain solid flat bright green background (#00FF00), no ground shadow, no floor, no gradient, no text, nothing else. Square 1:1 image.
```

## 그림이 오면 제가 하는 일

초록 배경을 떼어 넣고, 집 화면(방 배경 + 자리 10곳 + 아래 물건 상자)과 단계별로 열리는 규칙, 놓기/치우기, 새 물건이 열릴 때의 알림("새 물건이 열렸어! 집에 놓아 볼까?")을 붙입니다.

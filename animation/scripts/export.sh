#!/bin/bash
# Remotion 장면(달빛 섬, 비)을 투명 배경의 움직이는 WebP로 뽑아 앱의 리소스 폴더에 넣습니다.
#   사용법: animation 폴더에서  bash scripts/export.sh
#   필요한 것: Node, img2webp (brew install webp)
# 강아지 그림은 여기서 만들지 않습니다. scripts/keyout.sh를 보세요.
set -euo pipefail
cd "$(dirname "$0")/.."

RES="../app/src/main/res/drawable-nodpi"
mkdir -p "$RES"

# export_loop <장면ID> <앱 리소스 이름> <크기 배율> <n번째 프레임마다> <품질>
# 장면은 24fps로 만들었으므로, 프레임을 건너뛰면 그만큼 한 장을 오래 보여 줍니다.
export_loop() {
  local id="$1" name="$2" scale="$3" nth="$4" quality="$5"
  local frame_ms=$(( 1000 * nth / 24 ))
  rm -rf "out/$id"
  npx remotion render "$id" "out/$id" --sequence --image-format=png \
    --scale="$scale" --every-nth-frame="$nth" --log=error
  img2webp -loop 0 -d "$frame_ms" -lossy -q "$quality" -m 6 out/"$id"/*.png -o "$RES/$name.webp" 2> /dev/null
  echo "$name.webp  $(( $(stat -f%z "$RES/$name.webp") / 1024 )) KB  ($(ls out/"$id" | wc -l | tr -d ' ') frames, ${frame_ms}ms each)"
}

export_loop MoonIsland moon_island 0.8 2 76   # 느린 움직임: 12fps
export_loop RainLayer  rain_layer  1   1 80

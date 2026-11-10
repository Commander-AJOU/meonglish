#!/bin/bash
# AI 이미지 생성으로 만든 강아지 그림(초록 배경)에서 배경을 떼어 앱 리소스(투명 WebP)로 만듭니다.
#   사용법: bash scripts/keyout.sh <그림 폴더>
#   그림 폴더에는 docs/puppy-prompts.md의 이름대로 파일을 둡니다:
#     puppy_1..5.(png|webp|jpg)           단계별 평소 모습
#     blink_grid.*, happy_grid.*          2~5단계 눈 감은 표정 / 기뻐하는 표정이 2×2로 배열된 한 장(선택)
#     treat.*                             간식
#   필요한 것: ffmpeg, cwebp (brew install ffmpeg webp)
set -euo pipefail
cd "$(dirname "$0")/.."
SRC="${1:?그림 폴더를 적어 주세요}"
RES="../app/src/main/res/drawable-nodpi"
OUT="out/keyout"
mkdir -p "$OUT" "$RES"

# 생성된 그림의 초록은 정확히 #00FF00이 아니라 조금 어둡게 나오므로 실제 값에 가깝게 잡았습니다.
KEY="format=rgba,chromakey=0x15E51B:0.12:0.02"
# 가장자리의 초록 테두리를 2픽셀 깎고 부드럽게 합니다.
EDGE="split[c][m];[m]alphaextract,erosion,erosion,gblur=sigma=0.8[a];[c][a]alphamerge"
# 오른쪽 아래 구석의 작은 생성기 표시를 지웁니다(한 장짜리 그림만).
CORNER="format=gbrap,geq=r='r(X,Y)':g='g(X,Y)':b='b(X,Y)':a='if(gt(X,W-170)*gt(Y,H-150),0,alpha(X,Y))',format=rgba"

find_src() { ls "$SRC"/"$1".{png,webp,jpg,jpeg} 2> /dev/null | head -1; }

to_webp() { cwebp -quiet -q 90 -alpha_q 100 -m 6 "$1" -o "$RES/$2.webp"; echo "$2.webp"; }

for n in 1 2 3 4 5; do
  f=$(find_src "puppy_$n") || true
  [ -n "$f" ] || { echo "puppy_$n 없음, 건너뜀"; continue; }
  ffmpeg -v error -y -i "$f" -filter_complex "$KEY,$CORNER,$EDGE" "$OUT/puppy_$n.png"
  to_webp "$OUT/puppy_$n.png" "puppy_$n"
done

f=$(find_src treat) || true
if [ -n "$f" ]; then
  ffmpeg -v error -y -i "$f" -filter_complex "$KEY,$CORNER,$EDGE" "$OUT/treat.png"
  to_webp "$OUT/treat.png" treat
fi

# 2×2 격자: 왼쪽 위 2단계, 오른쪽 위 3단계, 왼쪽 아래 4단계, 오른쪽 아래 5단계
grid() {
  local f="$1" kind="$2"
  local w h
  w=$(ffprobe -v error -select_streams v:0 -show_entries stream=width -of csv=p=0 "$f")
  h=$(ffprobe -v error -select_streams v:0 -show_entries stream=height -of csv=p=0 "$f")
  local cw=$(( w / 2 )) ch=$(( h / 2 ))
  local i=0
  for pos in "0:0" "$cw:0" "0:$ch" "$cw:$ch"; do
    local stage=$(( i + 2 )); i=$(( i + 1 ))
    ffmpeg -v error -y -i "$f" -filter_complex "$KEY,crop=$cw:$ch:${pos%%:*}:${pos##*:},$EDGE" "$OUT/puppy_${stage}_$kind.png"
    to_webp "$OUT/puppy_${stage}_$kind.png" "puppy_${stage}_$kind"
  done
}
f=$(find_src blink_grid) || true; [ -n "$f" ] && grid "$f" blink
f=$(find_src happy_grid) || true; [ -n "$f" ] && grid "$f" happy
echo "완료: $RES"

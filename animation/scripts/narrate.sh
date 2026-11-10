#!/bin/bash
# 내레이션을 ElevenLabs로 만듭니다(강아지와 다른 어른 목소리). 이미 있는 파일은 다시 만들지 않습니다.
#   사용법: bash scripts/narrate.sh <목소리ID> <접두어> "<이름>=<문장>" ...
# 키는 ../secrets.properties 에서 읽고 화면에 내지 않습니다.
set -uo pipefail
cd "$(dirname "$0")/.."
KEY=$(grep '^ELEVENLABS_API_KEY=' ../secrets.properties | cut -d= -f2- | tr -d '\r\n ')
[ -n "$KEY" ] || { echo "키 없음"; exit 1; }
VOICE="$1"; PREFIX="$2"; shift 2
mkdir -p public/demo/vo
for pair in "$@"; do
  name="${pair%%=*}"; text="${pair#*=}"; out="public/demo/vo/${PREFIX}${name}.mp3"
  [ -s "$out" ] && { echo "있음  $out"; continue; }
  body=$(python3 -c 'import json,sys; print(json.dumps({"text": sys.argv[1], "model_id": "eleven_v3"}, ensure_ascii=False))' "$text")
  code=$(curl -s -o "$out" -w "%{http_code}" -X POST "https://api.elevenlabs.io/v1/text-to-speech/$VOICE?output_format=mp3_44100_128" \
    -H "xi-api-key: $KEY" -H "Content-Type: application/json" -d "$body")
  if [ "$code" = "200" ]; then echo "만듦  $out ($(ffprobe -v error -show_entries format=duration -of csv=p=0 "$out")s)"
  else echo "실패 $code $name: $(head -c 200 "$out")"; rm -f "$out"; fi
done

"""앱이 이미 만들어 둔 강아지 목소리(ElevenLabs 저장본)에서 필요한 문장을 찾아 public/demo/app 으로 복사합니다.
파일 이름은 SHA-1("모델|목소리ID|문장") 입니다(ElevenLabsVoice.fileFor)."""
import hashlib, shutil, sys, os
cache, out = sys.argv[1], sys.argv[2]
MODEL, VOICE = "eleven_v3", "cgSgspJ2msm6clMCkdW9"
LINES = {
    "left_ko": "왼쪽!", "left_en": "Left", "right_ko": "오른쪽!", "right_en": "Right",
    "right_answer": "맞아!", "treat": "딩동! 콩이의 간식이 하나 생겼어.",
    "repeat": "따라 해 볼까?", "pass": "잘했어! 통과!",
    "rain_en": "Rain", "stop_en": "Stop", "seatbelt_en": "Seatbelt",
    "cloud_q": "‘구름’. 영어로 뭐였지?", "cloud_en": "Cloud",
    "note_ask": "서준아, 오늘 블록하다가 넘어져서 무릎을 다쳤다며? 어땠어?",
    "note_comfort": "아프면 내 품에 기대도 돼. 내가 꼭 지켜 줄게.",
    "calm_1": "서준아, 속상했구나. 괜찮아, 콩이가 옆에 있어.",
    "calm_2": "우리 같이 후 하고 숨 쉬어 볼까? 후.",
    "bigdog_q": "‘큰 개’. 영어로 뭐였지?", "bigdog_en": "A big dog",
    "navi_right": "콩이가 귀를 쫑긋 세웠어요. 길 안내 요정이 오른쪽으로 돌아가래. 우리 달빛 섬은 그쪽인가 봐!",
}
for name, text in LINES.items():
    h = hashlib.sha1(f"{MODEL}|{VOICE}|{text}".encode()).hexdigest()
    src = os.path.join(cache, h + ".mp3")
    if os.path.exists(src) and os.path.getsize(src) > 0:
        shutil.copy(src, os.path.join(out, name + ".mp3")); print("ok  ", name)
    else:
        print("MISS", name, "|", text)

#!/usr/bin/env python3
"""시연 영상의 배경 음악(오르골풍)과 효과음을 합성합니다. 파이썬 표준 라이브러리만 씁니다.

    python3 scripts/make_music.py [길이(초)]

만드는 파일: public/demo/music.wav, public/demo/sfx/{pop,ding,whoosh,sparkle,tick,tock,thud}.wav
직접 만든 소리라 저작권 걱정이 없습니다. 다른 음원으로 바꾸려면 music.wav만 바꿔 끼우면 됩니다.
"""
import math
import random
import struct
import sys
import wave
from array import array
from pathlib import Path

RATE = 44100
OUT = Path(__file__).resolve().parent.parent / "public" / "demo"
TAU = math.pi * 2

# 한 마디 3.5초(4/4, 약 69bpm). 첫 두 마디(7초)는 장면 1에 맞춰 드문드문 울립니다.
BAR = 3.5
EIGHTH = BAR / 8


def midi(note: int) -> float:
    return 440.0 * 2 ** ((note - 69) / 12)


def add(buf: array, start: float, samples: list) -> None:
    i0 = int(start * RATE)
    end = min(len(buf), i0 + len(samples))
    for i in range(max(i0, 0), end):
        buf[i] += samples[i - i0]


def music_box(freq: float, amp: float, length: float = 2.2) -> list:
    """오르골 한 음: 기본음 + 살짝 어긋난 배음, 빠르게 사라짐."""
    n = int(length * RATE)
    w1, w2, w3 = TAU * freq / RATE, TAU * freq * 2.0 / RATE, TAU * freq * 5.4 / RATE
    d1, d2, d3 = 2.6 / RATE, 5.0 / RATE, 14.0 / RATE
    attack = int(0.004 * RATE)
    sin, exp = math.sin, math.exp
    return [
        amp * min(1.0, i / attack) * (sin(w1 * i) * exp(-d1 * i) + 0.32 * sin(w2 * i) * exp(-d2 * i) + 0.18 * sin(w3 * i) * exp(-d3 * i))
        for i in range(n)
    ]


def pad(freqs: list, amp: float, length: float) -> list:
    """부드러운 바탕 화음: 사인파 여러 개가 천천히 들어왔다 나감."""
    n = int(length * RATE)
    ws = [TAU * f / RATE for f in freqs]
    edge = int(0.9 * RATE)
    sin = math.sin
    out = []
    for i in range(n):
        env = min(1.0, i / edge, (n - i) / edge)
        out.append(amp * env * sum(sin(w * i) for w in ws) / len(ws))
    return out


# 화음 진행 C – Am – F – G. 낮은 음부터 아르페지오로 풉니다.
CHORDS = [
    [48, 55, 60, 64],  # C
    [45, 52, 57, 60],  # Am
    [41, 48, 53, 57],  # F
    [43, 50, 55, 59],  # G
]
ARP = [0, 1, 2, 3, 2, 3, 2, 1]

# 8마디 가락(5음계). (8분음표 위치, 음)
MELODY = [
    [(0, 76), (2, 79), (4, 81), (6, 79)],
    [(0, 76), (2, 74), (4, 72)],
    [(0, 69), (2, 72), (4, 74), (6, 72)],
    [(0, 74), (4, 67), (7, 74)],
    [(0, 76), (1, 79), (2, 84), (4, 81), (6, 79)],
    [(0, 81), (2, 79), (3, 76), (4, 74), (6, 72)],
    [(0, 74), (2, 72), (4, 69), (6, 72)],
    [(0, 74), (4, 72)],
]
INTRO = [[(0, 76), (4, 79)], [(0, 81), (3, 79), (6, 76)]]


def make_music(seconds: float) -> array:
    n = int(seconds * RATE)
    buf = array("d", bytes(8 * n))
    bars = int(math.ceil(seconds / BAR))
    cache: dict = {}

    def note(pitch: int, amp: float) -> list:
        key = (pitch, amp)
        if key not in cache:
            cache[key] = music_box(midi(pitch), amp)
        return cache[key]

    for bar in range(bars):
        t0 = bar * BAR
        chord = CHORDS[bar % 4]
        if bar < 2:
            for step, pitch in INTRO[bar]:
                add(buf, t0 + step * EIGHTH, note(pitch, 0.20))
            continue
        last = bar == bars - 1
        add(buf, t0, pad([midi(p) for p in chord[:3]], 0.085, BAR + 0.9))
        if last:
            # 마지막 마디: 으뜸화음을 한 번 펼치고 끝
            for k, pitch in enumerate([48, 55, 64, 72, 76, 84]):
                add(buf, t0 + k * 0.11, note(pitch, 0.20))
            break
        for step, idx in enumerate(ARP):
            add(buf, t0 + step * EIGHTH, note(chord[idx] + 12, 0.11 if step else 0.15))
        for step, pitch in MELODY[(bar - 2) % 8]:
            add(buf, t0 + step * EIGHTH, note(pitch, 0.24))
    # 끝 1.5초는 서서히 줄임
    fade = int(1.5 * RATE)
    for i in range(fade):
        buf[n - 1 - i] *= i / fade
    return buf


def sfx_pop() -> list:
    n = int(0.14 * RATE)
    out, phase = [], 0.0
    for i in range(n):
        t = i / n
        phase += TAU * (380 + 620 * t) / RATE
        out.append(0.7 * math.sin(phase) * math.exp(-5.5 * t) * min(1.0, i / 60))
    return out


def sfx_ding() -> list:
    a = music_box(midi(88), 0.55, 1.4)
    b = music_box(midi(93), 0.45, 1.3)
    out = a[:]
    off = int(0.11 * RATE)
    for i, v in enumerate(b):
        if i + off < len(out):
            out[i + off] += v
    return out


def sfx_sparkle() -> list:
    out = [0.0] * int(1.3 * RATE)
    for k, pitch in enumerate([84, 88, 91, 96, 100, 103]):
        tone = music_box(midi(pitch), 0.26, 0.8)
        off = int(k * 0.07 * RATE)
        for i, v in enumerate(tone):
            if i + off < len(out):
                out[i + off] += v
    return out


def sfx_whoosh() -> list:
    rnd = random.Random(7)
    n = int(0.5 * RATE)
    out, low, band = [], 0.0, 0.0
    for i in range(n):
        t = i / n
        env = math.sin(math.pi * t) ** 2
        cutoff = 0.02 + 0.22 * math.sin(math.pi * t)  # 필터가 열렸다 닫히며 '휙'
        low += cutoff * (rnd.uniform(-1, 1) - low)
        band += 0.5 * (low - band)
        out.append(0.9 * env * (low - 0.6 * band))
    return out


def sfx_click(freq: float, amp: float) -> list:
    """방향지시등의 '똑/딱'."""
    rnd = random.Random(int(freq))
    n = int(0.06 * RATE)
    return [
        amp * math.exp(-70 * i / RATE) * (math.sin(TAU * freq * i / RATE) + 0.35 * rnd.uniform(-1, 1) * math.exp(-400 * i / RATE))
        for i in range(n)
    ]


def sfx_thud() -> list:
    """폭신한 착지."""
    n = int(0.28 * RATE)
    out, phase = [], 0.0
    for i in range(n):
        t = i / n
        phase += TAU * (150 - 80 * t) / RATE
        out.append(0.8 * math.sin(phase) * math.exp(-7 * t) * min(1.0, i / 80))
    return out


def write(path: Path, samples, stereo_spread: float = 0.0) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    peak = max(1e-9, max(abs(v) for v in samples))
    gain = min(1.0, 0.89 / peak)  # 찢어지지 않게 최대치만 맞춤
    frames = bytearray()
    pack = struct.pack
    for v in samples:
        s = int(max(-1.0, min(1.0, v * gain)) * 32767)
        frames += pack("<hh", s, s)
    with wave.open(str(path), "wb") as f:
        f.setnchannels(2)
        f.setsampwidth(2)
        f.setframerate(RATE)
        f.writeframes(bytes(frames))
    print(f"{path.relative_to(OUT.parent.parent)}  {len(samples) / RATE:.2f}s  peak {peak * gain:.2f}")


def main() -> None:
    seconds = float(sys.argv[1]) if len(sys.argv) > 1 else 77.0
    write(OUT / "music.wav", make_music(seconds))
    write(OUT / "sfx" / "pop.wav", sfx_pop())
    write(OUT / "sfx" / "ding.wav", sfx_ding())
    write(OUT / "sfx" / "sparkle.wav", sfx_sparkle())
    write(OUT / "sfx" / "whoosh.wav", sfx_whoosh())
    write(OUT / "sfx" / "tick.wav", sfx_click(2100, 0.8))
    write(OUT / "sfx" / "tock.wav", sfx_click(1500, 0.8))
    write(OUT / "sfx" / "thud.wav", sfx_thud())


if __name__ == "__main__":
    main()

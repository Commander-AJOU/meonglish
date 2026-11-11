import { Video } from "@remotion/media";
import { AbsoluteFill, interpolate, staticFile, useCurrentFrame } from "remotion";
import { Header, Pill } from "../parts/Bits";
import { DeviceFrame } from "../parts/DeviceFrame";
import { C, FONT, pop, s } from "../theme";
import { SECRET } from "../timeline";

const PRESS = s(SECRET.press);
const CALM1 = s(SECRET.calm1);
const CALM2 = s(SECRET.calm2);
const DEVICE_W = 940;
/** 장 제목 표시의 가운데(앱 화면 1682×1210 기준). */
const TAG = { x: 120, y: 89 };

/** 부모의 비밀 버튼: 장 제목을 길게 누르면 강아지가 우는 아이를 달랩니다(왼쪽은 실제 앱 녹화). */
export const SceneSecret: React.FC = () => {
  const frame = useCurrentFrame();
  const enter = pop(frame, 2, false);
  const scale = (DEVICE_W - 36) / 1682;
  const hold = interpolate(frame, [PRESS, CALM1], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  const pressing = frame >= PRESS - 8 && frame < CALM1 + 12;
  return (
    <AbsoluteFill style={{ fontFamily: FONT }}>
      <Header at={4} text="아이가 울 때, " hi="어른만 아는 비밀 버튼" />

      <div style={{ position: "absolute", left: 90 - (1 - enter) * 1100, top: 232 }}>
        <DeviceFrame width={DEVICE_W} ratio={1682 / 1210}>
          <Video
            src={staticFile("demo/clips/secret.mp4")}
            trimBefore={Math.round(SECRET.clipCalm * 30) - CALM1}
            muted
            style={{ width: "100%", height: "100%", objectFit: "cover" }}
          />
        </DeviceFrame>
        {/* 길게 누르는 손가락 표시 */}
        {pressing ? (
          <svg
            width={200}
            height={200}
            viewBox="-100 -100 200 200"
            style={{ position: "absolute", left: 18 + TAG.x * scale - 100, top: 18 + TAG.y * scale - 100, opacity: pop(frame, PRESS - 8, false) }}
          >
            <circle r={62} fill="rgba(255,255,255,0.16)" stroke="rgba(255,255,255,0.5)" strokeWidth={6} />
            <circle
              r={62}
              fill="none"
              stroke={C.yellow}
              strokeWidth={12}
              strokeLinecap="round"
              strokeDasharray={`${hold * 390} 390`}
              transform="rotate(-90)"
            />
            <circle r={22 + 6 * Math.sin(frame / 3)} fill="#FFFFFF" opacity={0.85} />
          </svg>
        ) : null}
        <div style={{ textAlign: "center", fontSize: 28, color: "rgba(255,255,255,0.65)", marginTop: 12 }}>
          실제 앱 화면 · 아이 눈에는 평범한 장 제목
        </div>
      </div>

      <div style={{ position: "absolute", left: 1110, top: 250, width: 730 }}>
        <div style={{ transform: `scale(${frame >= PRESS - 14 ? pop(frame, PRESS - 14) : 0})`, transformOrigin: "0 50%" }}>
          <div style={{ fontSize: 32, color: "rgba(255,255,255,0.7)", marginBottom: 10 }}>① 왼쪽 위 장 제목을 길게 꾹 · 달리는 중엔 톡 한 번</div>
          <Pill size={50} bg="rgba(10,9,32,0.55)" style={{ border: `4px solid ${C.yellow}` }}>
            하늘 길로 출발 <span style={{ color: C.yellow, fontSize: 36 }}>← 꾹 누르기</span>
          </Pill>
        </div>

        {frame >= CALM1 ? (
          <div style={{ marginTop: 44, transform: `scale(${pop(frame, CALM1)})`, transformOrigin: "0 0" }}>
            <div style={{ fontSize: 32, color: "rgba(255,255,255,0.7)", marginBottom: 10 }}>② 강아지가 읽던 말을 멈추고 달래요</div>
            <div style={{ background: "#FFFFFF", color: C.ink, fontSize: 46, lineHeight: 1.3, borderRadius: 36, padding: "22px 32px 18px", wordBreak: "keep-all", boxShadow: "0 8px 0 rgba(0,0,0,0.2)" }}>
              “서준아, 속상했구나. 괜찮아, 콩이가 옆에 있어.”
            </div>
          </div>
        ) : null}
        {frame >= CALM2 ? (
          <div
            style={{
              marginTop: 18,
              background: C.yellow,
              color: C.ink,
              fontSize: 46,
              lineHeight: 1.3,
              borderRadius: 36,
              padding: "22px 32px 18px",
              wordBreak: "keep-all",
              transform: `scale(${pop(frame, CALM2)})`,
              transformOrigin: "0 0",
            }}
          >
            “우리 같이 후 하고 숨 쉬어 볼까? 후.”
          </div>
        ) : null}
      </div>
    </AbsoluteFill>
  );
};

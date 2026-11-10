/** 차량 화면처럼 보이는 틀. width는 틀 전체 너비, ratio는 안쪽 화면의 가로/세로 비. */
export const DeviceFrame: React.FC<{ width: number; ratio: number; children: React.ReactNode }> = ({ width, ratio, children }) => {
  const bezel = 18;
  const inner = width - bezel * 2;
  return (
    <div
      style={{
        width,
        padding: bezel,
        boxSizing: "border-box",
        background: "linear-gradient(160deg, #23223A 0%, #0B0B14 60%)",
        borderRadius: 40,
        boxShadow: "0 30px 80px rgba(8, 6, 40, 0.6), inset 0 2px 0 rgba(255,255,255,0.14)",
      }}
    >
      <div style={{ width: inner, height: inner / ratio, borderRadius: 24, overflow: "hidden", background: "#17135F" }}>{children}</div>
    </div>
  );
};

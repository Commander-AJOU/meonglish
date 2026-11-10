/** 차량 신호 아이콘들(직접 그린 선 그림). 모두 200×200. */
const stroke = { fill: "none", stroke: "#FFFFFF", strokeWidth: 12, strokeLinecap: "round" as const, strokeLinejoin: "round" as const };

/** 와이퍼: 앞유리 위를 함께 오가는 와이퍼 두 개와 빗방울. swing은 -1~1. */
export const WiperIcon: React.FC<{ swing: number }> = ({ swing }) => (
  <svg width={200} height={200} viewBox="0 0 200 200">
    <path d="M40 52 L160 52 Q176 52 180 68 L194 138 Q197 156 180 156 L20 156 Q3 156 6 138 L20 68 Q24 52 40 52 Z" {...stroke} strokeWidth={10} />
    {[62, 138].map((x) => (
      <g key={x} transform={`rotate(${-18 + swing * 34} ${x} 156)`}>
        <line x1={x} y1={152} x2={x} y2={84} {...stroke} strokeWidth={9} />
      </g>
    ))}
    {[
      [56, 28],
      [100, 16],
      [144, 28],
    ].map(([x, y], i) => (
      <path key={i} d={`M${x} ${y - 14} q-11 17 0 24 q11 -7 0 -24 Z`} fill="#7FD8FF" />
    ))}
  </svg>
);

/** 브레이크 경고등 모양. */
export const BrakeIcon: React.FC = () => (
  <svg width={200} height={200} viewBox="0 0 200 200">
    <circle cx={100} cy={100} r={52} {...stroke} />
    <line x1={100} y1={74} x2={100} y2={106} {...stroke} />
    <circle cx={100} cy={126} r={7} fill="#FFFFFF" />
    <path d="M38 48 Q10 100 38 152" {...stroke} />
    <path d="M162 48 Q190 100 162 152" {...stroke} />
  </svg>
);

/** 안전벨트를 맨 사람. */
export const BeltIcon: React.FC = () => (
  <svg width={200} height={200} viewBox="0 0 200 200">
    <circle cx={100} cy={48} r={26} {...stroke} />
    <path d="M52 176 L52 132 Q52 92 100 92 Q148 92 148 132 L148 176" {...stroke} />
    <line x1={62} y1={104} x2={146} y2={172} stroke="#FBBF24" strokeWidth={14} strokeLinecap="round" />
  </svg>
);

/** 계기판의 방향지시등 화살표. */
export const Arrow: React.FC<{ direction: "left" | "right"; color: string; glow?: boolean }> = ({ direction, color, glow }) => (
  <svg width={150} height={130} viewBox="0 0 150 130" style={{ transform: direction === "right" ? "scaleX(-1)" : undefined, filter: glow ? `drop-shadow(0 0 22px ${color})` : undefined }}>
    <path d="M8 65 L70 8 L70 42 L142 42 L142 88 L70 88 L70 122 Z" fill={color} />
  </svg>
);

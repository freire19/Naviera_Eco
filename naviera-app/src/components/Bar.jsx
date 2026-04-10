export default function Bar({ value, t, h = 4 }) {
  return <div style={{ width: "100%", height: h, borderRadius: h, background: t.border }}>
    <div className="progress-fill" style={{ width: `${Math.min(100, Math.max(0, value))}%`, height: "100%", borderRadius: h, background: t.pri }} />
  </div>;
}

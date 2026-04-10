import { API } from "../api.js";

export default function Av({ letters, size = 36, t, fotoUrl }) {
  const src = fotoUrl ? `${API}${fotoUrl}` : null;
  if (src) return <img src={src} alt="" style={{ width: size, height: size, borderRadius: "50%", objectFit: "cover", flexShrink: 0, border: `1px solid ${t.border}` }} />;
  return <div style={{ width: size, height: size, borderRadius: "50%", background: t.accent, display: "flex", alignItems: "center", justifyContent: "center", fontSize: size * 0.35, fontWeight: 700, color: t.pri, flexShrink: 0 }}>{letters}</div>;
}

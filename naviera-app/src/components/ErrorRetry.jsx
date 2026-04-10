import Cd from "./Card.jsx";
import { IconAlert, IconRefresh } from "../icons.jsx";

export default function ErrorRetry({ erro, onRetry, t }) {
  return <Cd t={t} style={{ padding: 20, textAlign: "center" }}>
    <IconAlert size={28} color={t.err} />
    <div style={{ fontSize: 13, color: t.txSoft, marginTop: 8 }}>{erro}</div>
    <button onClick={onRetry} className="btn-outline" style={{ marginTop: 12, padding: "8px 20px", border: `1px solid ${t.border}`, color: t.pri, borderRadius: 10, display: "inline-flex", alignItems: "center", gap: 6 }}>
      <IconRefresh size={14} color={t.pri} /> Tentar novamente
    </button>
  </Cd>;
}

import Cd from "./Card.jsx";
import { IconAlert, IconRefresh } from "../icons.jsx";
import { useTheme } from "../contexts/ThemeContext.jsx";

export default function ErrorRetry({ erro, onRetry }) {
  const { t } = useTheme();
  return <Cd style={{ padding: 20, textAlign: "center" }}>
    <div role="alert">
      <IconAlert size={28} color={t.err} aria-hidden="true" />
      <div style={{ fontSize: 13, color: t.txSoft, marginTop: 8 }}>{erro}</div>
    </div>
    <button onClick={onRetry} className="btn-outline" style={{ marginTop: 12, padding: "8px 20px", border: `1px solid ${t.border}`, color: t.pri, borderRadius: 10, display: "inline-flex", alignItems: "center", gap: 6 }}>
      <IconRefresh size={14} color={t.pri} /> Tentar novamente
    </button>
  </Cd>;
}

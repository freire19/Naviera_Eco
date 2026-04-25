import { useEffect } from "react";
import { useTheme } from "../contexts/ThemeContext.jsx";

export default function Toast({ message, type = "success", onClose }) {
  const { t } = useTheme();
  useEffect(() => { const timer = setTimeout(onClose, 3000); return () => clearTimeout(timer); }, [onClose]);
  const bg = type === "success" ? t.okBg : type === "error" ? t.errBg : t.infoBg;
  const c = type === "success" ? t.okTx : type === "error" ? t.errTx : t.infoTx;
  return <div role="status" aria-live="polite" className="toast-enter" style={{ position: "fixed", bottom: 80, left: 20, right: 20, maxWidth: 380, margin: "0 auto", zIndex: 60, padding: "12px 16px", borderRadius: 12, background: bg, color: c, fontSize: 13, fontWeight: 600, textAlign: "center" }}>{message}</div>;
}

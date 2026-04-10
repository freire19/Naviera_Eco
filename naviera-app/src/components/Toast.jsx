import { useEffect } from "react";

export default function Toast({ message, type = "success", t, onClose }) {
  useEffect(() => { const timer = setTimeout(onClose, 3000); return () => clearTimeout(timer); }, [onClose]);
  const bg = type === "success" ? t.okBg : type === "error" ? t.errBg : t.infoBg;
  const c = type === "success" ? t.okTx : type === "error" ? t.errTx : t.infoTx;
  return <div className="toast-enter" style={{ position: "fixed", bottom: 80, left: 20, right: 20, maxWidth: 380, margin: "0 auto", zIndex: 60, padding: "12px 16px", borderRadius: 12, background: bg, color: c, fontSize: 13, fontWeight: 600, textAlign: "center" }}>{message}</div>;
}

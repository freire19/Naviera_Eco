import { useState, useEffect } from "react";
import { useTheme } from "../contexts/ThemeContext.jsx";

/**
 * NotificationBanner — banner para solicitar permissao + toast de notificacao foreground.
 *
 * Props:
 *   permissao      — "default" | "granted" | "denied"
 *   notificacao    — { titulo, corpo } | null  (toast foreground)
 *   onSolicitar    — () => void  (pedir permissao)
 *   onLimpar       — () => void  (fechar toast)
 */
export default function NotificationBanner({ permissao, notificacao, onSolicitar, onLimpar }) {
  const { t } = useTheme();
  const [dismissed, setDismissed] = useState(() => {
    try { return sessionStorage.getItem("naviera_notif_dismissed") === "1"; }
    catch { return false; }
  });

  /* ── Toast auto-close ── */
  useEffect(() => {
    if (!notificacao) return;
    const timer = setTimeout(onLimpar, 5000);
    return () => clearTimeout(timer);
  }, [notificacao, onLimpar]);

  const dismiss = () => {
    setDismissed(true);
    try { sessionStorage.setItem("naviera_notif_dismissed", "1"); } catch {}
  };

  /* ═══ TOAST (foreground notification) ═══ */
  if (notificacao) {
    return (
      <div
        className="toast-enter"
        style={{
          position: "fixed", top: 16, left: 16, right: 16,
          maxWidth: 380, margin: "0 auto", zIndex: 9999,
          padding: "14px 16px", borderRadius: 14,
          background: t.infoBg, color: t.infoTx,
          boxShadow: "0 4px 20px rgba(0,0,0,0.15)",
          cursor: "pointer",
        }}
        onClick={onLimpar}
      >
        <div style={{ fontWeight: 700, fontSize: 14, marginBottom: 2 }}>
          {notificacao.titulo}
        </div>
        <div style={{ fontSize: 13, opacity: 0.85 }}>
          {notificacao.corpo}
        </div>
      </div>
    );
  }

  /* ═══ BANNER (pedir permissao) ═══ */
  if (permissao !== "default" || dismissed) return null;

  return (
    <div style={{
      position: "fixed", bottom: 80, left: 12, right: 12,
      maxWidth: 396, margin: "0 auto", zIndex: 50,
      padding: "14px 16px", borderRadius: 14,
      background: t.card, color: t.tx,
      border: `1px solid ${t.borderStrong}`,
      boxShadow: "0 4px 20px rgba(0,0,0,0.1)",
      display: "flex", alignItems: "center", gap: 12,
    }}>
      {/* icon */}
      <div style={{
        width: 36, height: 36, borderRadius: 10,
        background: t.priLight, display: "flex",
        alignItems: "center", justifyContent: "center", flexShrink: 0,
      }}>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke={t.pri} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
          <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
        </svg>
      </div>

      <div style={{ flex: 1 }}>
        <div style={{ fontWeight: 700, fontSize: 13 }}>Ativar notificacoes?</div>
        <div style={{ fontSize: 12, color: t.txMuted, marginTop: 2 }}>
          Receba avisos de embarques e encomendas.
        </div>
      </div>

      <div style={{ display: "flex", gap: 6, flexShrink: 0 }}>
        <button
          onClick={dismiss}
          style={{
            background: "none", border: "none", color: t.txMuted,
            fontSize: 12, cursor: "pointer", padding: "6px 8px",
          }}
        >
          Depois
        </button>
        <button
          onClick={onSolicitar}
          style={{
            background: t.priGrad, color: "#fff", border: "none",
            borderRadius: 8, fontSize: 12, fontWeight: 700,
            padding: "8px 14px", cursor: "pointer",
          }}
        >
          Ativar
        </button>
      </div>
    </div>
  );
}

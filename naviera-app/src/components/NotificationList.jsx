import { useState, useRef, useEffect } from "react";

/* ═══ ICON MAP BY TYPE ═══ */
const TYPE_META = {
  PASSAGEM_CRIADA:    { icon: "\uD83C\uDFAB", label: "Passagem" },
  ENCOMENDA_ENTREGUE: { icon: "\uD83D\uDCE6", label: "Encomenda" },
  VIAGEM_ATIVADA:     { icon: "\u26F5",       label: "Viagem" },
  FRETE_CRIADO:       { icon: "\uD83D\uDE9A", label: "Frete" },
  SYNC_COMPLETO:      { icon: "\u2705",       label: "Sync" },
};

function getMeta(type) {
  return TYPE_META[type] || { icon: "\uD83D\uDD14", label: "Aviso" };
}

function timeAgo(ts) {
  if (!ts) return "";
  const diff = Date.now() - new Date(ts).getTime();
  const sec = Math.floor(diff / 1000);
  if (sec < 60) return "agora";
  const min = Math.floor(sec / 60);
  if (min < 60) return `${min}min`;
  const hrs = Math.floor(min / 60);
  if (hrs < 24) return `${hrs}h`;
  return `${Math.floor(hrs / 24)}d`;
}

export default function NotificationList({ t, notifications, clearNotifications, unreadCount }) {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);

  /* close on outside click */
  useEffect(() => {
    if (!open) return;
    const handler = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, [open]);

  return (
    <div ref={ref} style={{ position: "relative" }}>
      {/* bell button */}
      <button
        onClick={() => setOpen((v) => !v)}
        style={{
          width: 32, height: 32, borderRadius: 8,
          border: `1px solid ${t.border}`, background: t.soft,
          cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center",
          position: "relative",
        }}
        aria-label="Notificacoes"
      >
        <svg width={14} height={14} viewBox="0 0 24 24" fill="none" stroke={t.txMuted} strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
          <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9" />
          <path d="M13.73 21a2 2 0 01-3.46 0" />
        </svg>
        {unreadCount > 0 && (
          <span style={{
            position: "absolute", top: -4, right: -4,
            background: t.err || "#DC2626", color: "#fff",
            fontSize: 9, fontWeight: 700, borderRadius: 99,
            minWidth: 16, height: 16, display: "flex", alignItems: "center", justifyContent: "center",
            padding: "0 4px", lineHeight: 1,
          }}>
            {unreadCount > 99 ? "99+" : unreadCount}
          </span>
        )}
      </button>

      {/* dropdown panel */}
      {open && (
        <div style={{
          position: "absolute", top: 40, right: 0, zIndex: 1000,
          width: 280, maxHeight: 360, overflowY: "auto",
          background: t.card, border: `1px solid ${t.border}`,
          borderRadius: 12, boxShadow: t.shadow || "0 4px 20px rgba(0,0,0,0.12)",
        }}>
          {/* header */}
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "10px 12px 6px", borderBottom: `1px solid ${t.border}` }}>
            <span style={{ fontSize: 13, fontWeight: 700, color: t.tx }}>Notificacoes</span>
            {notifications.length > 0 && (
              <button
                onClick={() => { clearNotifications(); setOpen(false); }}
                style={{ fontSize: 11, color: t.pri, background: "none", border: "none", cursor: "pointer", fontWeight: 600 }}
              >
                Marcar como lidas
              </button>
            )}
          </div>

          {/* list */}
          {notifications.length === 0 ? (
            <div style={{ padding: 20, textAlign: "center", color: t.txMuted, fontSize: 12 }}>
              Nenhuma notificacao
            </div>
          ) : (
            notifications.map((n, i) => {
              const meta = getMeta(n.type);
              return (
                <div key={`${n.timestamp}-${i}`} style={{
                  display: "flex", gap: 8, padding: "8px 12px",
                  borderBottom: i < notifications.length - 1 ? `1px solid ${t.border}` : "none",
                  alignItems: "flex-start",
                }}>
                  <span style={{ fontSize: 18, lineHeight: 1, flexShrink: 0, marginTop: 2 }}>{meta.icon}</span>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 12, fontWeight: 600, color: t.tx, lineHeight: 1.3 }}>{n.message}</div>
                    <div style={{ fontSize: 10, color: t.txMuted, marginTop: 2 }}>{meta.label} · {timeAgo(n.timestamp)}</div>
                  </div>
                </div>
              );
            })
          )}
        </div>
      )}
    </div>
  );
}

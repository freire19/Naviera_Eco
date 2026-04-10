export default function TabBar({ tabs, tab, setTab, t }) {
  return <div style={{ position: "fixed", bottom: 0, left: "50%", transform: "translateX(-50%)", width: "100%", maxWidth: 420, background: t.card, borderTop: `1px solid ${t.border}`, padding: "6px 8px 12px", zIndex: 40 }}>
    <div style={{ display: "flex", gap: 2 }}>
      {tabs.map(tb => <button key={tb.id} onClick={() => setTab(tb.id)} className="tab-item"
        style={{ background: tab === tb.id ? t.accent : "transparent", color: tab === tb.id ? t.pri : t.txMuted }}>
        <tb.Icon size={18} color={tab === tb.id ? t.pri : t.txMuted} />
        <span>{tb.label}</span>
        {tab === tb.id && <div style={{ width: 4, height: 4, borderRadius: 2, background: t.pri }} />}
      </button>)}
    </div>
  </div>;
}

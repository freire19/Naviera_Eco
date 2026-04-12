import { IconBack, IconUser, IconMoon, IconSun, IconLogout } from "../icons.jsx";
import Logo from "./Logo.jsx";
import NotificationList from "./NotificationList.jsx";

export default function Header({ t, mode, setMode, tab, navigateTab, goBack, profile, minhaFoto, doLogout, notifications = [], clearNotifications = () => {}, unreadCount = 0 }) {
  const isCPF = profile === "cpf";
  return <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "14px 18px 10px", borderBottom: `1px solid ${t.border}`, background: t.card }}>
    <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
      {tab !== "home" && <button onClick={goBack} style={{ background: "none", border: "none", color: t.txMuted, cursor: "pointer", padding: "0 4px 0 0", display: "flex", alignItems: "center" }}><IconBack size={18} color={t.txMuted} /></button>}
      <div style={{ width: 30, height: 30, borderRadius: 8, background: t.accent, display: "flex", alignItems: "center", justifyContent: "center" }}><Logo size={16} t={t} /></div>
      <span style={{ fontSize: 15, fontWeight: 700, letterSpacing: 2, color: t.tx }}>NAVIERA</span>
      <span style={{ fontSize: 9, padding: "2px 6px", borderRadius: 6, background: isCPF ? t.accent : t.amberBg, color: isCPF ? t.pri : t.amber, fontWeight: 700, marginLeft: 4, letterSpacing: 0.5 }}>{isCPF ? "CPF" : "CNPJ"}</span>
    </div>
    <div style={{ display: "flex", gap: 6, alignItems: "center" }}>
      <NotificationList t={t} notifications={notifications} clearNotifications={clearNotifications} unreadCount={unreadCount} />
      <button onClick={() => navigateTab("perfil")} style={{ width: 32, height: 32, borderRadius: "50%", border: `2px solid ${tab === "perfil" ? t.pri : t.border}`, background: tab === "perfil" ? t.accent : t.soft, cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center", padding: 0, overflow: "hidden" }}>
        {minhaFoto ? <img src={minhaFoto} alt="" style={{ width: "100%", height: "100%", objectFit: "cover" }} /> : <IconUser size={14} color={tab === "perfil" ? t.pri : t.txMuted} />}
      </button>
      <button onClick={() => setMode(m => m === "light" ? "dark" : "light")} style={{ width: 32, height: 32, borderRadius: 8, border: `1px solid ${t.border}`, background: t.soft, cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center" }}>
        {mode === "light" ? <IconMoon size={14} color={t.txMuted} /> : <IconSun size={14} color={t.txMuted} />}
      </button>
      <button onClick={doLogout} style={{ width: 32, height: 32, borderRadius: 8, border: `1px solid ${t.border}`, background: t.soft, cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center" }}>
        <IconLogout size={14} color={t.txMuted} />
      </button>
    </div>
  </div>;
}

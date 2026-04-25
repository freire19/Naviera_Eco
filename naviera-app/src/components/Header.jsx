import { IconBack, IconUser, IconMoon, IconSun, IconLogout } from "../icons.jsx";
import Logo from "./Logo.jsx";
import NotificationList from "./NotificationList.jsx";
import { useTheme } from "../contexts/ThemeContext.jsx";
import { useAuth } from "../contexts/AuthContext.jsx";

export default function Header({ tab, navigateTab, goBack, profile, minhaFoto, notifications = [], clearNotifications = () => {}, unreadCount = 0 }) {
  const { t, mode, setMode } = useTheme();
  const { logout } = useAuth();
  const isCPF = profile === "cpf";
  return <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "14px 18px 10px", borderBottom: `1px solid ${t.border}`, background: t.card }}>
    <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
      {tab !== "home" && <button onClick={goBack} aria-label="Voltar" style={{ background: "none", border: "none", color: t.txMuted, cursor: "pointer", padding: "0 4px 0 0", display: "flex", alignItems: "center" }}><IconBack size={18} color={t.txMuted} /></button>}
      <div style={{ width: 30, height: 30, borderRadius: 8, background: t.accent, display: "flex", alignItems: "center", justifyContent: "center" }}><Logo size={16} /></div>
      <span style={{ fontSize: 15, fontWeight: 700, letterSpacing: 2, color: t.tx }}>NAVIERA</span>
      <span style={{ fontSize: 9, padding: "2px 6px", borderRadius: 6, background: isCPF ? t.accent : t.amberBg, color: isCPF ? t.pri : t.amber, fontWeight: 700, marginLeft: 4, letterSpacing: 0.5 }}>{isCPF ? "CPF" : "CNPJ"}</span>
    </div>
    <div style={{ display: "flex", gap: 6, alignItems: "center" }}>
      <NotificationList notifications={notifications} clearNotifications={clearNotifications} unreadCount={unreadCount} />
      <button onClick={() => navigateTab("perfil")} aria-label="Meu perfil" aria-current={tab === "perfil" ? "page" : undefined} style={{ width: 32, height: 32, borderRadius: "50%", border: `2px solid ${tab === "perfil" ? t.pri : t.border}`, background: tab === "perfil" ? t.accent : t.soft, cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center", padding: 0, overflow: "hidden" }}>
        {minhaFoto ? <img src={minhaFoto} alt="" style={{ width: "100%", height: "100%", objectFit: "cover" }} /> : <IconUser size={14} color={tab === "perfil" ? t.pri : t.txMuted} />}
      </button>
      <button onClick={() => setMode(m => m === "light" ? "dark" : "light")} aria-label={mode === "light" ? "Ativar tema escuro" : "Ativar tema claro"} style={{ width: 32, height: 32, borderRadius: 8, border: `1px solid ${t.border}`, background: t.soft, cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center" }}>
        {mode === "light" ? <IconMoon size={14} color={t.txMuted} /> : <IconSun size={14} color={t.txMuted} />}
      </button>
      <button onClick={() => { if (window.confirm("Sair da conta?")) logout(); }} aria-label="Sair da conta" style={{ width: 32, height: 32, borderRadius: 8, border: `1px solid ${t.border}`, background: t.soft, cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center" }}>
        <IconLogout size={14} color={t.txMuted} />
      </button>
    </div>
  </div>;
}

import { useState, useEffect, useRef } from "react";
import "./App.css";
import {
  IconHome, IconHeart, IconShip, IconTicket, IconGrid, IconCart,
  IconUsers, IconWallet, IconStore, IconPackage
} from "./icons.jsx";
import { T } from "./theme.js";
import { API } from "./api.js";
import useWebSocket from "./hooks/useWebSocket.js";
import useNotifications from "./hooks/useNotifications.js";
import Header from "./components/Header.jsx";
import TabBar from "./components/TabBar.jsx";
import NotificationBanner from "./components/NotificationBanner.jsx";
import usePWA from "./hooks/usePWA.js";
import LoginScreen from "./screens/LoginScreen.jsx";
import HomeCPF from "./screens/HomeCPF.jsx";
import AmigosCPF from "./screens/AmigosCPF.jsx";
import MapaCPF from "./screens/MapaCPF.jsx";
import PassagensCPF from "./screens/PassagensCPF.jsx";
import HomeCNPJ from "./screens/HomeCNPJ.jsx";
import PedidosCNPJ from "./screens/PedidosCNPJ.jsx";
import LojasParceiras from "./screens/LojasParceiras.jsx";
import FinanceiroCNPJ from "./screens/FinanceiroCNPJ.jsx";
import LojaCNPJ from "./screens/LojaCNPJ.jsx";
import PerfilScreen from "./screens/PerfilScreen.jsx";
import EncomendaCPF from "./screens/EncomendaCPF.jsx";

/* ═══ TAB DEFINITIONS ═══ */
const TABS_CPF = [
  { id: "home", label: "In\u00edcio", Icon: IconHome },
  { id: "amigos", label: "Amigos", Icon: IconHeart },
  { id: "mapa", label: "Barcos", Icon: IconShip },
  { id: "passagens", label: "Passagens", Icon: IconTicket },
  { id: "encomendas", label: "Encomendas", Icon: IconPackage }
];
const TABS_CNPJ = [
  { id: "home", label: "Painel", Icon: IconGrid },
  { id: "pedidos", label: "Pedidos", Icon: IconCart },
  { id: "lojas", label: "Parceiros", Icon: IconUsers },
  { id: "financeiro", label: "Financ.", Icon: IconWallet },
  { id: "loja", label: "Loja", Icon: IconStore }
];

/* ═══ MAIN ═══ */
export default function Naviera() {
  const [profile, setProfile] = useState(() => { try { const u = JSON.parse(localStorage.getItem("naviera_usuario")); return u?.tipo === "CNPJ" ? "cnpj" : u ? "cpf" : null; } catch { return null; } });
  const [tab, setTab] = useState("home");
  const [tabHistory, setTabHistory] = useState([]);
  const navigateTab = (newTab) => { setTabHistory(h => [...h, tab]); setTab(newTab); };
  const goBack = () => { if (tabHistory.length > 0) { setTab(tabHistory[tabHistory.length - 1]); setTabHistory(h => h.slice(0, -1)); } };
  const { canInstall, promptInstall, dismiss: dismissInstall } = usePWA();
  const [mode, setMode] = useState("light");
  const [token, setToken] = useState(() => localStorage.getItem("naviera_token"));
  const [usuario, setUsuario] = useState(() => { try { return JSON.parse(localStorage.getItem("naviera_usuario")); } catch { return null; } });
  const [minhaFoto, setMinhaFoto] = useState(null);
  const t = T[mode];

  /* ═══ PUSH NOTIFICATIONS ═══ */
  const {
    suportado: notifSuportado, permissao, tokenFcm,
    notificacao, solicitarPermissao, obterTokenFcm,
    enviarNotificacaoLocal, limparNotificacao,
  } = useNotifications();
  const pushTokenEnviado = useRef(false);

  // Apos permissao concedida, tenta obter token FCM
  useEffect(() => {
    if (permissao === "granted") obterTokenFcm();
  }, [permissao, obterTokenFcm]);

  // Registra token FCM no backend
  useEffect(() => {
    if (!tokenFcm || !token || pushTokenEnviado.current) return;
    pushTokenEnviado.current = true;
    fetch(`${API}/push/registrar`, {
      method: "POST",
      headers: { "Authorization": `Bearer ${token}`, "Content-Type": "application/json" },
      body: JSON.stringify({ tokenFcm, plataforma: "web" }),
    }).catch(() => { pushTokenEnviado.current = false; });
  }, [tokenFcm, token]);

  const handleSolicitarPermissao = async () => {
    const result = await solicitarPermissao();
    if (result === "granted") obterTokenFcm();
  };

  /* ═══ WEBSOCKET ═══ */
  const { notifications, clearNotifications, unreadCount } = useWebSocket({
    token,
    empresaId: usuario?.id || null,
    apiUrl: API,
  });

  useEffect(() => {
    if (!token) { setMinhaFoto(null); return; }
    fetch(`${API}/perfil`, { headers: { "Authorization": `Bearer ${token}`, "Content-Type": "application/json" } })
      .then(r => r.ok ? r.json() : null)
      .then(d => { if (d?.fotoUrl) setMinhaFoto(`${API}${d.fotoUrl}`); })
      .catch(() => {});
  }, [token]);

  const handleLogin = (data) => {
    localStorage.setItem("naviera_token", data.token);
    localStorage.setItem("naviera_usuario", JSON.stringify({ nome: data.nome, tipo: data.tipo, id: data.id }));
    setToken(data.token);
    setUsuario({ nome: data.nome, tipo: data.tipo, id: data.id });
    setProfile(data.tipo === "CNPJ" ? "cnpj" : "cpf");
    setTab("home"); setTabHistory([]);
  };

  const doLogout = () => { localStorage.removeItem("naviera_token"); localStorage.removeItem("naviera_usuario"); setProfile(null); setToken(null); setUsuario(null); setTab("home"); setTabHistory([]); setMinhaFoto(null); };

  /* ═══ LOGIN SCREEN ═══ */
  if (!profile) return <LoginScreen t={t} mode={mode} setMode={setMode} onLogin={handleLogin} />;

  /* ═══ AUTHENTICATED SHELL ═══ */
  const isCPF = profile === "cpf";
  const tabs = isCPF ? TABS_CPF : TABS_CNPJ;
  const authHeaders = token ? { "Authorization": `Bearer ${token}`, "Content-Type": "application/json" } : {};
  const screen = () => {
    if (tab === "perfil") return <PerfilScreen t={t} token={token} authHeaders={authHeaders} usuario={usuario} onFotoChange={setMinhaFoto} />;
    if (isCPF) {
      if (tab === "home") return <HomeCPF t={t} onNav={navigateTab} authHeaders={authHeaders} usuario={usuario} />;
      if (tab === "amigos") return <AmigosCPF t={t} authHeaders={authHeaders} />;
      if (tab === "mapa") return <MapaCPF t={t} authHeaders={authHeaders} />;
      if (tab === "passagens") return <PassagensCPF t={t} authHeaders={authHeaders} />;
      if (tab === "encomendas") return <EncomendaCPF t={t} authHeaders={authHeaders} />;
    } else {
      if (tab === "home") return <HomeCNPJ t={t} onNav={navigateTab} authHeaders={authHeaders} usuario={usuario} />;
      if (tab === "pedidos") return <PedidosCNPJ t={t} authHeaders={authHeaders} />;
      if (tab === "lojas") return <LojasParceiras t={t} authHeaders={authHeaders} />;
      if (tab === "financeiro") return <FinanceiroCNPJ t={t} authHeaders={authHeaders} />;
      if (tab === "loja") return <LojaCNPJ t={t} authHeaders={authHeaders} />;
    }
  };

  return (
    <div style={{ minHeight: "100vh", background: t.bg, maxWidth: 420, margin: "0 auto", position: "relative", transition: "background 0.3s", color: t.tx }}>
      <Header t={t} mode={mode} setMode={setMode} tab={tab} navigateTab={navigateTab} goBack={goBack} profile={profile} minhaFoto={minhaFoto} doLogout={doLogout} notifications={notifications} clearNotifications={clearNotifications} unreadCount={unreadCount} />
      {canInstall && (
        <div style={{
          margin: "8px 18px 0", padding: "12px 16px", borderRadius: 12,
          background: t.priGrad, display: "flex", alignItems: "center",
          justifyContent: "space-between", gap: 12
        }}>
          <span style={{ color: "#fff", fontSize: 13, fontWeight: 600 }}>
            Instalar o app Naviera
          </span>
          <div style={{ display: "flex", gap: 8, flexShrink: 0 }}>
            <button onClick={dismissInstall} style={{
              background: "rgba(255,255,255,0.2)", color: "#fff", border: "none",
              borderRadius: 8, padding: "6px 12px", fontSize: 12, fontWeight: 600,
              cursor: "pointer", fontFamily: "inherit"
            }}>Agora nao</button>
            <button onClick={promptInstall} style={{
              background: "#fff", color: "#059669", border: "none",
              borderRadius: 8, padding: "6px 14px", fontSize: 12, fontWeight: 700,
              cursor: "pointer", fontFamily: "inherit"
            }}>Instalar</button>
          </div>
        </div>
      )}
      <div style={{ padding: "16px 18px 100px" }}>{screen()}</div>
      <TabBar tabs={tabs} tab={tab} setTab={(id) => { setTab(id); setTabHistory([]); }} t={t} />
      {notifSuportado && (
        <NotificationBanner
          t={t}
          permissao={permissao}
          notificacao={notificacao}
          onSolicitar={handleSolicitarPermissao}
          onLimpar={limparNotificacao}
        />
      )}
    </div>
  );
}

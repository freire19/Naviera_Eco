import { useState, useEffect, useRef, useMemo, useCallback, lazy, Suspense } from "react";
import "./App.css";
import {
  IconHome, IconHeart, IconShip, IconTicket, IconGrid, IconCart,
  IconUsers, IconWallet, IconStore, IconPackage
} from "./icons.jsx";
import { T } from "./theme.js";
import { API, authFetch, lerUsuarioValido } from "./api.js";
import { ThemeProvider } from "./contexts/ThemeContext.jsx";
import { AuthProvider } from "./contexts/AuthContext.jsx";
import useWebSocket from "./hooks/useWebSocket.js";
import useNotifications from "./hooks/useNotifications.js";
import Header from "./components/Header.jsx";
import TabBar from "./components/TabBar.jsx";
import NotificationBanner from "./components/NotificationBanner.jsx";
import Skeleton from "./components/Skeleton.jsx";
import usePWA from "./hooks/usePWA.js";
import LoginScreen from "./screens/LoginScreen.jsx";

// Code-split: cada screen carrega sob demanda + STOMP/SockJS sai do bundle pre-login.
const HomeCPF = lazy(() => import("./screens/HomeCPF.jsx"));
const AmigosCPF = lazy(() => import("./screens/AmigosCPF.jsx"));
const MapaCPF = lazy(() => import("./screens/MapaCPF.jsx"));
const PassagensCPF = lazy(() => import("./screens/PassagensCPF.jsx"));
const HomeCNPJ = lazy(() => import("./screens/HomeCNPJ.jsx"));
const PedidosCNPJ = lazy(() => import("./screens/PedidosCNPJ.jsx"));
const LojasParceiras = lazy(() => import("./screens/LojasParceiras.jsx"));
const FinanceiroCNPJ = lazy(() => import("./screens/FinanceiroCNPJ.jsx"));
const LojaCNPJ = lazy(() => import("./screens/LojaCNPJ.jsx"));
const PerfilScreen = lazy(() => import("./screens/PerfilScreen.jsx"));
const EncomendaCPF = lazy(() => import("./screens/EncomendaCPF.jsx"));

// #DS5-209: migra credenciais de localStorage (legado) para sessionStorage no import do modulo.
//   StrictMode roda useState initializers 2x — manter em escopo de modulo evita o duplo-efeito.
try {
  const legacyToken = localStorage.getItem("naviera_token");
  const legacyUser = localStorage.getItem("naviera_usuario");
  if (legacyToken && !sessionStorage.getItem("naviera_token")) {
    sessionStorage.setItem("naviera_token", legacyToken);
  }
  if (legacyUser && !sessionStorage.getItem("naviera_usuario")) {
    sessionStorage.setItem("naviera_usuario", legacyUser);
  }
  if (legacyToken || legacyUser) {
    localStorage.removeItem("naviera_token");
    localStorage.removeItem("naviera_usuario");
    localStorage.removeItem("naviera_app_token");
  }
} catch { /* sandbox sem storage — ignorar */ }

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
  // #DS5-209: token e usuario em sessionStorage (migracao do legado em localStorage roda no import do modulo acima).
  // #DR286: schema validado em lerUsuarioValido — schema mismatch -> logout silencioso.
  const [profile, setProfile] = useState(() => {
    const u = lerUsuarioValido();
    if (!u) {
      sessionStorage.removeItem("naviera_usuario");
      sessionStorage.removeItem("naviera_token");
      return null;
    }
    return u.tipo === "CNPJ" ? "cnpj" : "cpf";
  });
  const [tab, setTab] = useState("home");
  const [tabHistory, setTabHistory] = useState([]);
  const navigateTab = (newTab) => { setTabHistory(h => [...h, tab]); setTab(newTab); };
  const goBack = () => { if (tabHistory.length > 0) { setTab(tabHistory[tabHistory.length - 1]); setTabHistory(h => h.slice(0, -1)); } };
  const { canInstall, promptInstall, dismiss: dismissInstall } = usePWA();
  const [mode, setMode] = useState("light");
  const [token, setToken] = useState(() => sessionStorage.getItem("naviera_token"));
  const [usuario, setUsuario] = useState(() => lerUsuarioValido());
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
    authFetch(`${API}/push/registrar`, {
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
  // #DB156: usuario.id is the client's own ID, not empresa_id.
  // App mobile clients (CPF/CNPJ) don't receive tenant operational notifications,
  // so empresaId is null — WebSocket stays connected but won't subscribe to wrong topic.
  const { notifications, clearNotifications, unreadCount } = useWebSocket({
    token,
    empresaId: null,
    apiUrl: API,
  });

  useEffect(() => {
    if (!token) { setMinhaFoto(null); return; }
    authFetch(`${API}/perfil`, { headers: { "Authorization": `Bearer ${token}`, "Content-Type": "application/json" } })
      .then(r => r.ok ? r.json() : null)
      .then(d => { if (d?.fotoUrl) setMinhaFoto(`${API}${d.fotoUrl}`); })
      .catch(e => console.warn("[App] erro ao carregar foto do perfil:", e?.message));
  }, [token]);

  const handleLogin = useCallback((data) => {
    sessionStorage.setItem("naviera_token", data.token);
    sessionStorage.setItem("naviera_usuario", JSON.stringify({ nome: data.nome, tipo: data.tipo, id: data.id }));
    setToken(data.token);
    setUsuario({ nome: data.nome, tipo: data.tipo, id: data.id });
    setProfile(data.tipo === "CNPJ" ? "cnpj" : "cpf");
    setTab("home"); setTabHistory([]);
  }, []);

  const doLogout = useCallback(() => {
    sessionStorage.removeItem("naviera_token"); sessionStorage.removeItem("naviera_usuario");
    setProfile(null); setToken(null); setUsuario(null);
    setTab("home"); setTabHistory([]); setMinhaFoto(null);
    pushTokenEnviado.current = false;
  }, []);

  const isCPF = profile === "cpf";
  const tabs = isCPF ? TABS_CPF : TABS_CNPJ;
  // Estabilidade de referencia: consumidores listam authHeaders em deps de useEffect.
  const authHeaders = useMemo(
    () => token ? { "Authorization": `Bearer ${token}`, "Content-Type": "application/json" } : {},
    [token]
  );

  const themeValue = useMemo(() => ({ t, mode, setMode }), [t, mode]);
  const authValue = useMemo(
    () => ({ token, usuario, authHeaders, login: handleLogin, logout: doLogout }),
    [token, usuario, authHeaders, handleLogin, doLogout]
  );

  const screen = () => {
    if (tab === "perfil") return <PerfilScreen onFotoChange={setMinhaFoto} />;
    if (isCPF) {
      if (tab === "home") return <HomeCPF onNav={navigateTab} />;
      if (tab === "amigos") return <AmigosCPF />;
      if (tab === "mapa") return <MapaCPF />;
      if (tab === "passagens") return <PassagensCPF />;
      if (tab === "encomendas") return <EncomendaCPF />;
    } else {
      if (tab === "home") return <HomeCNPJ onNav={navigateTab} />;
      if (tab === "pedidos") return <PedidosCNPJ />;
      if (tab === "lojas") return <LojasParceiras />;
      if (tab === "financeiro") return <FinanceiroCNPJ />;
      if (tab === "loja") return <LojaCNPJ />;
    }
  };

  return (
    <ThemeProvider value={themeValue}>
      <AuthProvider value={authValue}>
        {!profile ? (
          <LoginScreen />
        ) : (
          <div style={{ minHeight: "100vh", background: t.bg, maxWidth: 420, margin: "0 auto", position: "relative", transition: "background 0.3s", color: t.tx }}>
            <Header tab={tab} navigateTab={navigateTab} goBack={goBack} profile={profile} minhaFoto={minhaFoto} notifications={notifications} clearNotifications={clearNotifications} unreadCount={unreadCount} />
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
            <main style={{ padding: "16px 18px 100px" }}>
              <Suspense fallback={<Skeleton height={80} count={3} />}>{screen()}</Suspense>
            </main>
            <TabBar tabs={tabs} tab={tab} setTab={(id) => { setTab(id); setTabHistory([]); }} />
            {notifSuportado && (
              <NotificationBanner
                permissao={permissao}
                notificacao={notificacao}
                onSolicitar={handleSolicitarPermissao}
                onLimpar={limparNotificacao}
              />
            )}
          </div>
        )}
      </AuthProvider>
    </ThemeProvider>
  );
}

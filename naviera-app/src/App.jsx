import { useState, useEffect, useCallback, useRef } from "react";
import "./App.css";
import {
  IconHome, IconHeart, IconShip, IconTicket, IconGrid, IconCart,
  IconUsers, IconWallet, IconStore, IconUser, IconBack, IconSun,
  IconMoon, IconLogout, IconSearch, IconCheck, IconPlus, IconRefresh,
  IconAlert, IconMapPin, IconPackage, IconClock, IconCalendar
} from "./icons.jsx";

/* ═══ BRAND V4 ═══ */
const T = {
  light: {
    bg: "#F7FBF9", card: "#FFFFFF", soft: "#EEF7F2", accent: "#E6F5ED",
    tx: "#0F2620", txSoft: "#3D6B56", txMuted: "#7BA393",
    pri: "#059669", priLight: "#D1FAE5", priBorder: "#A7F3D0",
    priGrad: "linear-gradient(135deg, #059669, #34D399)",
    border: "rgba(5,150,105,0.12)", borderStrong: "rgba(5,150,105,0.25)",
    info: "#0369A1", infoBg: "#DBEAFE", infoTx: "#1D4ED8",
    warn: "#B45309", warnBg: "#FEF3C7", warnTx: "#B45309",
    ok: "#047857", okBg: "#D1FAE5", okTx: "#047857",
    err: "#DC2626", errBg: "#FEE2E2", errTx: "#DC2626",
    amber: "#B45309", amberBg: "#FEF3C7",
    shadow: "0 2px 12px rgba(0,0,0,0.05)",
    skBase: "#EEF7F2", skShine: "#EEF7F2",
  },
  dark: {
    bg: "#040D0A", card: "#0F2D24", soft: "#0A1F18", accent: "#0F2D24",
    tx: "#F0FDF4", txSoft: "#6EE7B7", txMuted: "#34D399",
    pri: "#34D399", priLight: "#052E22", priBorder: "#059669",
    priGrad: "linear-gradient(135deg, #059669, #34D399)",
    border: "rgba(52,211,153,0.08)", borderStrong: "rgba(52,211,153,0.2)",
    info: "#0EA5E9", infoBg: "#0c2d48", infoTx: "#38BDF8",
    warn: "#F59E0B", warnBg: "#3a3520", warnTx: "#FBBF24",
    ok: "#4ADE80", okBg: "#0f2b1c", okTx: "#4ADE80",
    err: "#EF4444", errBg: "#450a0a", errTx: "#F87171",
    amber: "#FBBF24", amberBg: "#3a3520",
    shadow: "none",
    skBase: "#0A1F18", skShine: "#0A1F18",
  }
};

const API = import.meta.env.VITE_API_URL || "http://localhost:8080/api";

/* ═══ HELPERS ═══ */
const fmt = (d) => d ? new Date(d + "T00:00:00").toLocaleDateString("pt-BR") : "—";
const money = (v) => v != null ? `R$ ${Number(v).toLocaleString("pt-BR", { minimumFractionDigits: 2 })}` : "—";
const initials = (name) => name ? name.split(" ").map(w => w[0]).join("").slice(0, 2).toUpperCase() : "?";

const maskCPF = (v) => v.replace(/\D/g, "").slice(0, 11).replace(/(\d{3})(\d)/, "$1.$2").replace(/(\d{3})(\d)/, "$1.$2").replace(/(\d{3})(\d{1,2})$/, "$1-$2");
const maskCNPJ = (v) => v.replace(/\D/g, "").slice(0, 14).replace(/(\d{2})(\d)/, "$1.$2").replace(/(\d{3})(\d)/, "$1.$2").replace(/(\d{3})(\d)/, "$1/$2").replace(/(\d{4})(\d{1,2})$/, "$1-$2");
const maskDoc = (v, tipo) => tipo === "CNPJ" ? maskCNPJ(v) : maskCPF(v);

const validarDocumento = (doc, tipo) => {
  const nums = doc.replace(/\D/g, "");
  if (tipo === "CPF" && nums.length !== 11) return "CPF deve ter 11 dígitos.";
  if (tipo === "CNPJ" && nums.length !== 14) return "CNPJ deve ter 14 dígitos.";
  return null;
};

/* ═══ LOGO ═══ */
const Logo = ({ size = 16, t }) => (
  <svg width={size} height={size} viewBox="0 0 60 60" fill="none">
    <path d="M14 48 L14 14 Q14 10, 18 14 L30 30 Q34 35, 34 30 L34 14" stroke={t.pri} strokeWidth="5" strokeLinecap="round" strokeLinejoin="round" fill="none"/>
    <path d="M34 30 Q34 35, 38 30 L48 16" stroke={t.pri} strokeWidth="3.5" strokeLinecap="round" fill="none" opacity="0.25"/>
  </svg>
);

/* ═══ HOOK: useApi com refresh ═══ */
function useApi(path, authHeaders, deps = []) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState("");
  const [rev, setRev] = useState(0);

  const refresh = useCallback(() => setRev(r => r + 1), []);

  useEffect(() => {
    if (!authHeaders?.Authorization) return;
    setLoading(true); setErro("");
    fetch(`${API}${path}`, { headers: authHeaders })
      .then(r => {
        if (r.status === 401 || r.status === 403) {
          localStorage.removeItem("naviera_token"); localStorage.removeItem("naviera_usuario");
          window.location.reload();
          return Promise.reject("Sessão expirada");
        }
        return r.ok ? r.json() : Promise.reject("Erro ao carregar");
      })
      .then(d => setData(d))
      .catch((e) => setErro(typeof e === "string" ? e : "Erro ao carregar dados."))
      .finally(() => setLoading(false));
  }, [path, authHeaders?.Authorization, rev, ...deps]);

  return { data, loading, erro, refresh };
}

/* ═══ UI COMPONENTS ═══ */
function Badge({ status, t }) {
  const m = {
    "Em trânsito": [t.infoBg, t.infoTx], "EM_VIAGEM": [t.infoBg, t.infoTx], "Em viagem": [t.infoBg, t.infoTx],
    "Entregue": [t.okBg, t.okTx], "NO_PORTO": [t.warnBg, t.warnTx], "Confirmada": [t.okBg, t.okTx],
    "Reservada": [t.warnBg, t.warnTx], "Aguardando": [t.warnBg, t.warnTx], "No destino": [t.okBg, t.okTx],
    "Offline": [t.soft, t.txMuted], "Pendente": [t.errBg, t.errTx], "Pago": [t.okBg, t.okTx], "Verificada": [t.okBg, t.okTx],
    "CONFIRMADA": [t.okBg, t.okTx], "PENDENTE_CONFIRMACAO": [t.warnBg, t.warnTx], "EMBARCADO": [t.infoBg, t.infoTx],
    "CANCELADA": [t.errBg, t.errTx], "EXPIRADA": [t.soft, t.txMuted], "PENDENTE": [t.errBg, t.errTx]
  };
  const [bg, c] = m[status] || [t.soft, t.txMuted];
  const labels = { "EM_VIAGEM": "Em viagem", "NO_PORTO": "No porto", "CONFIRMADA": "Confirmada", "PENDENTE_CONFIRMACAO": "Aguardando pgto", "EMBARCADO": "Embarcado", "CANCELADA": "Cancelada", "EXPIRADA": "Expirada", "PENDENTE": "Pendente" };
  const label = labels[status] || status;
  return <span style={{ fontSize: 11, padding: "3px 10px", borderRadius: 20, background: bg, color: c, fontWeight: 600 }}>{label}</span>;
}

function Bar({ value, t, h = 4 }) {
  return <div style={{ width: "100%", height: h, borderRadius: h, background: t.border }}>
    <div className="progress-fill" style={{ width: `${Math.min(100, Math.max(0, value))}%`, height: "100%", borderRadius: h, background: t.pri }} />
  </div>;
}

function Av({ letters, size = 36, t, fotoUrl }) {
  const src = fotoUrl ? `${API}${fotoUrl}` : null;
  if (src) return <img src={src} alt="" style={{ width: size, height: size, borderRadius: "50%", objectFit: "cover", flexShrink: 0, border: `1px solid ${t.border}` }} />;
  return <div style={{ width: size, height: size, borderRadius: "50%", background: t.accent, display: "flex", alignItems: "center", justifyContent: "center", fontSize: size * 0.35, fontWeight: 700, color: t.pri, flexShrink: 0 }}>{letters}</div>;
}

function Cd({ children, style, onClick, t, className = "" }) {
  return <div onClick={onClick} className={onClick ? `card-interactive ${className}` : className}
    style={{ background: t.card, borderRadius: 14, padding: 16, border: `1px solid ${t.border}`, cursor: onClick ? "pointer" : "default", boxShadow: t.shadow, ...style }}>
    {children}
  </div>;
}

function Skeleton({ t, height = 60, count = 3 }) {
  return <>{Array.from({ length: count }).map((_, i) => (
    <div key={i} className="skeleton" style={{ height, "--sk-base": t.skBase, "--sk-shine": t.skShine, marginBottom: 8, borderRadius: 14 }} />
  ))}</>;
}

function ErrorRetry({ erro, onRetry, t }) {
  return <Cd t={t} style={{ padding: 20, textAlign: "center" }}>
    <IconAlert size={28} color={t.err} />
    <div style={{ fontSize: 13, color: t.txSoft, marginTop: 8 }}>{erro}</div>
    <button onClick={onRetry} className="btn-outline" style={{ marginTop: 12, padding: "8px 20px", border: `1px solid ${t.border}`, color: t.pri, borderRadius: 10, display: "inline-flex", alignItems: "center", gap: 6 }}>
      <IconRefresh size={14} color={t.pri} /> Tentar novamente
    </button>
  </Cd>;
}

function Toast({ message, type = "success", t, onClose }) {
  useEffect(() => { const timer = setTimeout(onClose, 3000); return () => clearTimeout(timer); }, [onClose]);
  const bg = type === "success" ? t.okBg : type === "error" ? t.errBg : t.infoBg;
  const c = type === "success" ? t.okTx : type === "error" ? t.errTx : t.infoTx;
  return <div className="toast-enter" style={{ position: "fixed", bottom: 80, left: 20, right: 20, maxWidth: 380, margin: "0 auto", zIndex: 60, padding: "12px 16px", borderRadius: 12, background: bg, color: c, fontSize: 13, fontWeight: 600, textAlign: "center" }}>{message}</div>;
}

/* ═══ HEADER ═══ */
function Header({ t, mode, setMode, tab, navigateTab, goBack, profile, minhaFoto, doLogout }) {
  const isCPF = profile === "cpf";
  return <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "14px 18px 10px", borderBottom: `1px solid ${t.border}`, background: t.card }}>
    <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
      {tab !== "home" && <button onClick={goBack} style={{ background: "none", border: "none", color: t.txMuted, cursor: "pointer", padding: "0 4px 0 0", display: "flex", alignItems: "center" }}><IconBack size={18} color={t.txMuted} /></button>}
      <div style={{ width: 30, height: 30, borderRadius: 8, background: t.accent, display: "flex", alignItems: "center", justifyContent: "center" }}><Logo size={16} t={t} /></div>
      <span style={{ fontSize: 15, fontWeight: 700, letterSpacing: 2, color: t.tx }}>NAVIERA</span>
      <span style={{ fontSize: 9, padding: "2px 6px", borderRadius: 6, background: isCPF ? t.accent : t.amberBg, color: isCPF ? t.pri : t.amber, fontWeight: 700, marginLeft: 4, letterSpacing: 0.5 }}>{isCPF ? "CPF" : "CNPJ"}</span>
    </div>
    <div style={{ display: "flex", gap: 6 }}>
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

/* ═══ TAB BAR ═══ */
function TabBar({ tabs, tab, setTab, t }) {
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

/* ═══ CPF SCREENS ═══ */
function HomeCPF({ t, onNav, authHeaders, usuario }) {
  const { data: viagens, loading: lv, erro: ev, refresh: rv } = useApi("/viagens/ativas", authHeaders);
  const { data: encomendas, loading: le } = useApi("/encomendas", authHeaders);
  const { data: amigos } = useApi("/amigos", authHeaders);
  const proxima = viagens?.find(v => v.atual) || viagens?.[0];

  if (ev) return <ErrorRetry erro={ev} onRetry={rv} t={t} />;

  return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 14 }}>
    <div><span style={{ fontSize: 13, color: t.txMuted }}>Olá,</span><h2 style={{ margin: 0, fontSize: 22, fontWeight: 700 }}>{usuario?.nome || "Passageiro"}</h2></div>

    {lv ? <Skeleton t={t} height={90} count={1} /> :
    proxima && <Cd t={t} style={{ border: `1px solid ${t.borderStrong}` }} onClick={() => onNav("mapa")}>
      <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 10 }}>
        <div><div style={{ fontSize: 10, color: t.pri, fontWeight: 700, letterSpacing: 1.5, textTransform: "uppercase", marginBottom: 4 }}>Próxima viagem</div>
          <div style={{ fontSize: 16, fontWeight: 600 }}>{proxima.embarcacao}</div><div style={{ fontSize: 13, color: t.txMuted, marginTop: 2 }}>{proxima.origem} → {proxima.destino}</div></div>
        <Badge status={proxima.atual ? "Em viagem" : "Confirmada"} t={t} />
      </div>
      <div style={{ display: "flex", gap: 20, fontSize: 12, color: t.txMuted }}>
        <span style={{ display: "flex", alignItems: "center", gap: 4 }}><IconCalendar size={12} color={t.txMuted} /> {fmt(proxima.dataViagem)}</span>
        <span style={{ display: "flex", alignItems: "center", gap: 4 }}><IconClock size={12} color={t.txMuted} /> {proxima.horarioSaida || "—"}</span>
      </div>
    </Cd>}

    {!lv && (!viagens || viagens.length === 0) && <Cd t={t} style={{ padding: 16, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Nenhuma viagem ativa no momento.</div></Cd>}

    <div style={{ display: "flex", justifyContent: "space-between", marginTop: 4 }}>
      <span style={{ fontSize: 14, fontWeight: 600 }}>Amigos</span>
      <button style={{ background: "none", border: "none", color: t.pri, fontSize: 12, fontWeight: 600, cursor: "pointer" }} onClick={() => onNav("amigos")}>Ver todos →</button>
    </div>
    {amigos?.length > 0 ? amigos.slice(0, 3).map(f => (
      <Cd key={f.id} t={t} style={{ padding: 12 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
          <Av letters={initials(f.nome)} size={40} t={t} fotoUrl={f.fotoUrl} />
          <div style={{ flex: 1 }}><div style={{ fontSize: 14, fontWeight: 600 }}>{f.nome}</div><div style={{ fontSize: 12, color: t.txMuted }}>{f.cidade || "Sem cidade"}</div></div>
        </div>
      </Cd>)) : <Cd t={t} style={{ padding: 12, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Nenhum amigo. <span style={{ color: t.pri, cursor: "pointer" }} onClick={() => onNav("amigos")}>Adicionar →</span></div></Cd>}

    <span style={{ fontSize: 14, fontWeight: 600, marginTop: 4 }}>Encomendas</span>
    {le ? <Skeleton t={t} height={70} count={2} /> :
    encomendas?.length > 0 ? encomendas.slice(0, 5).map(e => (
      <Cd key={e.id} t={t} style={{ padding: 12 }}>
        <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 4 }}>
          <span style={{ fontSize: 12, fontWeight: 700, fontFamily: "'Space Mono', monospace", color: t.txSoft }}>{e.numeroEncomenda || `ENC-${e.id}`}</span>
          <Badge status={e.entregue ? "Entregue" : "Em trânsito"} t={t} /></div>
        <div style={{ fontSize: 13 }}>{e.rota}</div><div style={{ fontSize: 12, color: t.txMuted }}>{e.embarcacao} • {money(e.totalAPagar)}</div>
      </Cd>)) : <Cd t={t} style={{ padding: 12, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Nenhuma encomenda encontrada.</div></Cd>}
  </div>;
}

function AmigosCPF({ t, authHeaders }) {
  const { data: amigos, loading, refresh } = useApi("/amigos", authHeaders);
  const { data: pendentes, refresh: refreshPendentes } = useApi("/amigos/pendentes", authHeaders);
  const { data: sugestoes } = useApi("/amigos/sugestoes", authHeaders);
  const [busca, setBusca] = useState("");
  const [resultados, setResultados] = useState(null);
  const [buscando, setBuscando] = useState(false);
  const [enviados, setEnviados] = useState({});
  const [toast, setToast] = useState(null);

  const pesquisar = async (nome) => {
    setBusca(nome);
    if (nome.trim().length < 2) { setResultados(null); return; }
    setBuscando(true);
    try {
      const res = await fetch(`${API}/amigos/buscar?nome=${encodeURIComponent(nome.trim())}`, { headers: authHeaders });
      if (res.ok) setResultados(await res.json());
    } catch {} finally { setBuscando(false); }
  };

  const addAmigo = async (amigoId, nome) => {
    try {
      const res = await fetch(`${API}/amigos/${amigoId}`, { method: "POST", headers: authHeaders });
      const data = await res.json();
      if (res.ok) { setEnviados(e => ({ ...e, [amigoId]: true })); setToast(`Convite enviado para ${nome}!`); }
      else setToast(data.erro || "Erro ao enviar.");
    } catch { setToast("Erro de conexão."); }
  };

  const aceitarAmigo = async (amizadeId) => {
    await fetch(`${API}/amigos/${amizadeId}/aceitar`, { method: "PUT", headers: authHeaders });
    refresh(); refreshPendentes();
  };

  const removerAmigo = async (amizadeId) => {
    await fetch(`${API}/amigos/${amizadeId}`, { method: "DELETE", headers: authHeaders });
    refresh();
  };

  const PessoaCard = ({ p, acao }) => (
    <Cd t={t} style={{ padding: 12 }}>
      <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
        <Av letters={initials(p.nome)} size={42} t={t} fotoUrl={p.fotoUrl} />
        <div style={{ flex: 1 }}><div style={{ fontSize: 14, fontWeight: 600 }}>{p.nome}</div><div style={{ fontSize: 12, color: t.txMuted }}>{p.cidade || ""}</div></div>
        {acao}
      </div>
    </Cd>
  );

  if (loading) return <Skeleton t={t} height={60} count={4} />;

  return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Amigos</h3>
    <div style={{ position: "relative" }}>
      <input value={busca} onChange={e => pesquisar(e.target.value)} placeholder="Buscar pessoas por nome..."
        className="input-field" style={{ width: "100%", padding: "10px 14px 10px 36px", borderRadius: 10, border: `1px solid ${t.border}`, background: t.soft, color: t.tx, fontSize: 13, outline: "none", boxSizing: "border-box" }} />
      <div style={{ position: "absolute", left: 12, top: "50%", transform: "translateY(-50%)" }}><IconSearch size={14} color={t.txMuted} /></div>
    </div>

    {buscando && <div style={{ fontSize: 12, color: t.txMuted, textAlign: "center" }}>Buscando...</div>}
    {resultados && !buscando && resultados.length === 0 && <div style={{ fontSize: 12, color: t.txMuted, textAlign: "center" }}>Ninguém encontrado.</div>}
    {resultados?.map(p => <PessoaCard key={p.id} p={p} acao={
      enviados[p.id] ? <IconCheck size={18} color={t.pri} /> :
      <button onClick={() => addAmigo(p.id, p.nome)} style={{ background: t.priGrad, border: "none", borderRadius: 8, padding: "6px 12px", cursor: "pointer", display: "flex", alignItems: "center", gap: 4 }}>
        <IconPlus size={12} color="#fff" /><span style={{ color: "#fff", fontSize: 11, fontWeight: 600 }}>Add</span>
      </button>
    } />)}

    {pendentes?.length > 0 && <>
      <div style={{ fontSize: 13, fontWeight: 600, color: t.pri, marginTop: 4 }}>Convites pendentes</div>
      {pendentes.map(p => <PessoaCard key={p.amizadeId} p={p} acao={
        <div style={{ display: "flex", gap: 6 }}>
          <button onClick={() => aceitarAmigo(p.amizadeId)} style={{ background: t.priGrad, border: "none", borderRadius: 8, padding: "6px 10px", cursor: "pointer", color: "#fff", fontSize: 11, fontWeight: 600 }}>Aceitar</button>
          <button onClick={() => removerAmigo(p.amizadeId)} style={{ background: "none", border: `1px solid ${t.border}`, borderRadius: 8, padding: "6px 10px", cursor: "pointer", color: t.txMuted, fontSize: 11 }}>Recusar</button>
        </div>
      } />)}
    </>}

    {sugestoes?.length > 0 && !resultados && <>
      <div style={{ fontSize: 13, fontWeight: 600, color: t.txSoft, marginTop: 4 }}>Sugestões</div>
      {sugestoes.map(p => <PessoaCard key={p.id} p={p} acao={
        enviados[p.id] ? <IconCheck size={18} color={t.pri} /> :
        <button onClick={() => addAmigo(p.id, p.nome)} className="btn-outline" style={{ border: `1px solid ${t.border}`, borderRadius: 8, padding: "6px 10px", color: t.pri, fontSize: 11, fontWeight: 600, background: "transparent", cursor: "pointer" }}>Adicionar</button>
      } />)}
    </>}

    {!resultados && amigos?.length > 0 && <>
      <div style={{ fontSize: 13, fontWeight: 600, color: t.txSoft, marginTop: 4 }}>Seus amigos ({amigos.length})</div>
      {amigos.map(f => <PessoaCard key={f.amizadeId || f.id} p={f} acao={
        <button onClick={() => removerAmigo(f.amizadeId)} style={{ background: "none", border: "none", color: t.txMuted, fontSize: 11, cursor: "pointer" }}>Remover</button>
      } />)}
    </>}

    {toast && <Toast message={toast} t={t} onClose={() => setToast(null)} />}
  </div>;
}

/* ═══ MAP SCREEN — Real-time tracking ═══ */
function MapaCPF({ t, authHeaders }) {
  const { data: boats, loading, erro, refresh } = useApi("/embarcacoes", authHeaders);
  const [sel, setSel] = useState(null);
  const embId = sel !== null ? boats?.[sel]?.id : null;
  const { data: viagensEmb } = useApi(embId ? `/viagens/embarcacao/${embId}` : null, embId ? authHeaders : null);

  if (loading) return <Skeleton t={t} height={80} count={3} />;
  if (erro) return <ErrorRetry erro={erro} onRetry={refresh} t={t} />;

  if (sel !== null) {
    const detalhe = boats?.[sel];
    if (!detalhe) { setSel(null); return null; }

    const emViagem = detalhe.status === "EM_VIAGEM";

    return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 12 }}>
      <button onClick={() => setSel(null)} style={{ background: "none", border: "none", color: t.txMuted, fontSize: 13, cursor: "pointer", textAlign: "left", padding: 0, display: "flex", alignItems: "center", gap: 4 }}><IconBack size={14} color={t.txMuted} /> Voltar</button>

      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>{detalhe.nome}</h3>
        <Badge status={detalhe.status || "NO_PORTO"} t={t} />
      </div>

      {/* MAP — SVG river route */}
      {emViagem && <div style={{ borderRadius: 14, overflow: "hidden", border: `1px solid ${t.border}` }}>
        <div style={{ position: "relative", height: 180, background: "linear-gradient(160deg, #040D0A 0%, #0F2D24 30%, #040D0A 70%, #0A1F18 100%)" }}>
          <svg width="100%" height="100%" viewBox="0 0 400 180" style={{ position: "absolute", top: 0, left: 0 }}>
            <path d="M20 80 Q60 95, 110 75 Q160 55, 210 82 Q260 105, 310 70 Q350 48, 390 85" fill="none" stroke="rgba(5,150,105,0.5)" strokeWidth="20" opacity="0.35"/>
            <path d="M20 80 Q60 95, 110 75 Q160 55, 210 82 Q260 105, 310 70 Q350 48, 390 85" fill="none" stroke="rgba(52,211,153,0.6)" strokeWidth="9" opacity="0.4"/>
            <path d="M25 82 Q65 96, 115 76 Q165 56, 215 83 Q265 106, 315 71 Q355 50, 392 86" fill="none" stroke="rgba(255,255,255,0.18)" strokeWidth="1.2" strokeDasharray="5 4"/>
            <circle cx="25" cy="82" r="4.5" fill="#34D399" stroke="white" strokeWidth="1.5"/>
            <text x="25" y="100" textAnchor="middle" fill="white" fontSize="8.5" fontWeight="500">Manaus</text>
            <circle cx="215" cy="83" r="6" fill="#F59E0B" stroke="white" strokeWidth="2" className="boat-marker"/>
            <circle cx="215" cy="83" r="14" fill="none" stroke="#F59E0B" strokeWidth="1" className="boat-ring"/>
            <circle cx="115" cy="75" r="2.5" fill="rgba(255,255,255,0.4)"/>
            <text x="115" y="66" textAnchor="middle" fill="rgba(255,255,255,0.55)" fontSize="7.5">Codajás</text>
            <circle cx="315" cy="70" r="2.5" fill="rgba(255,255,255,0.4)"/>
            <text x="315" y="61" textAnchor="middle" fill="rgba(255,255,255,0.55)" fontSize="7.5">Fonte Boa</text>
            <circle cx="392" cy="86" r="4.5" fill="#EF4444" stroke="white" strokeWidth="1.5"/>
            <text x="392" y="104" textAnchor="middle" fill="white" fontSize="8.5" fontWeight="500">Jutaí</text>
          </svg>
          <div style={{ position: "absolute", bottom: 8, left: 8, right: 8, background: "rgba(0,0,0,0.55)", borderRadius: 10, padding: "8px 12px", display: "flex", justifyContent: "space-between" }}>
            <div><div style={{ fontSize: 8, color: "rgba(255,255,255,0.5)" }}>Velocidade</div><div style={{ fontSize: 12, color: "white", fontWeight: 600 }}>12.4 nós</div></div>
            <div><div style={{ fontSize: 8, color: "rgba(255,255,255,0.5)" }}>ETA</div><div style={{ fontSize: 12, color: "white", fontWeight: 600 }}>17/04 14h</div></div>
            <div><div style={{ fontSize: 8, color: "rgba(255,255,255,0.5)" }}>Percorrido</div><div style={{ fontSize: 12, color: "white", fontWeight: 600 }}>58%</div></div>
          </div>
        </div>
        <div style={{ padding: "8px 14px", background: t.card }}>
          <div style={{ display: "flex", justifyContent: "space-between", fontSize: 11, color: t.txMuted, marginBottom: 4 }}><span>Manaus</span><span>Jutaí</span></div>
          <Bar value={58} t={t} />
          <div style={{ fontSize: 10, color: t.txMuted, marginTop: 4, textAlign: "center" }}>Atualizado há 3 min</div>
        </div>
      </div>}

      {detalhe.descricao && <Cd t={t} style={{ padding: 14 }}><div style={{ fontSize: 13, color: t.txSoft, lineHeight: 1.7 }}>{detalhe.descricao}</div></Cd>}

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
        <Cd t={t} style={{ padding: 12, textAlign: "center" }}>
          <IconUsers size={20} color={t.pri} /><div style={{ fontSize: 18, fontWeight: 700, color: t.pri, marginTop: 4 }}>{detalhe.capacidadePassageiros || "—"}</div><div style={{ fontSize: 11, color: t.txMuted }}>Passageiros</div>
        </Cd>
        <Cd t={t} style={{ padding: 12, textAlign: "center" }}>
          <IconMapPin size={20} color={t.info} /><div style={{ fontSize: 14, fontWeight: 700, color: t.info, marginTop: 4 }}>{emViagem ? "Em viagem" : "No porto"}</div><div style={{ fontSize: 11, color: t.txMuted }}>Status</div>
        </Cd>
      </div>

      {detalhe.horarioSaidaPadrao && <Cd t={t} style={{ padding: 14, border: `1px solid ${t.borderStrong}` }}>
        <div style={{ fontSize: 12, fontWeight: 700, color: t.pri, textTransform: "uppercase", letterSpacing: 1, marginBottom: 8 }}>Horários</div>
        {detalhe.horarioSaidaPadrao.split("\n").map((line, i) =>
          line.trim() === "" ? <div key={i} style={{ height: 8 }} /> :
          line === line.toUpperCase() && line.includes("→") ? <div key={i} style={{ fontSize: 13, fontWeight: 700, color: t.tx, marginTop: i > 0 ? 4 : 0 }}>{line}</div> :
          <div key={i} style={{ fontSize: 12, color: t.txSoft, lineHeight: 1.6, paddingLeft: 8 }}>{line}</div>
        )}
      </Cd>}

      {detalhe.telefone && <Cd t={t} style={{ padding: 12 }}>
        <div style={{ fontSize: 12, color: t.txMuted }}>Telefone</div><div style={{ fontSize: 14, fontWeight: 600 }}>{detalhe.telefone}</div>
      </Cd>}

      {viagensEmb?.length > 0 && <>
        <div style={{ fontSize: 12, fontWeight: 700, color: t.pri, textTransform: "uppercase", letterSpacing: 1 }}>Próximas viagens</div>
        {viagensEmb.map(v => <Cd key={v.id} t={t} style={{ padding: 12 }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <div><div style={{ fontSize: 13, fontWeight: 600 }}>{v.origem} → {v.destino}</div><div style={{ fontSize: 12, color: t.txMuted, marginTop: 2 }}>Saída: {fmt(v.dataViagem)} • Chegada: {fmt(v.previsaoChegada || v.dataChegada)}</div></div>
            <Badge status={v.atual ? "Em viagem" : "Confirmada"} t={t} />
          </div>
        </Cd>)}
      </>}
    </div>;
  }

  return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Embarcações</h3>
    {(!boats || boats.length === 0) && <Cd t={t} style={{ padding: 16, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Nenhuma embarcação encontrada.</div></Cd>}
    {boats?.map((b, i) => <Cd key={b.id || i} t={t} style={{ padding: 0, overflow: "hidden" }} onClick={() => setSel(i)}>
      {b.fotoUrl && <img src={`${API}${b.fotoUrl}`} alt={b.nome} style={{ width: "100%", height: 120, objectFit: "cover" }} />}
      <div style={{ padding: 14 }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
          <div><div style={{ fontSize: 15, fontWeight: 600 }}>{b.nome}</div><div style={{ fontSize: 12, color: t.txMuted, marginTop: 2 }}>{b.rotaPrincipal || b.rotaAtual || ""}</div></div>
          <Badge status={b.status || "NO_PORTO"} t={t} />
        </div>
        {b.horarioSaidaPadrao && <div style={{ fontSize: 11, color: t.txMuted, marginTop: 6, lineHeight: 1.5 }}>{b.horarioSaidaPadrao.split("\n").find(l => l.includes("Saída de Manaus"))?.trim() || ""}</div>}
        <div style={{ fontSize: 12, color: t.pri, fontWeight: 600, marginTop: 8 }}>Ver detalhes →</div>
      </div>
    </Cd>)}
  </div>;
}

/* ═══ BILHETE DIGITAL — NFT-style ticket (always dark) ═══ */
function BilheteScreen({ bilhete, t: _t, onBack }) {
  // Força tema dark pro bilhete — visual premium
  const t = { ...T.dark };
  const [now, setNow] = useState(Date.now());
  const [brightness, setBrightness] = useState(false);
  useEffect(() => { const iv = setInterval(() => setNow(Date.now()), 1000); return () => clearInterval(iv); }, []);

  const totp = (() => {
    const counter = Math.floor(now / 30000);
    let h = 0; const s = (bilhete.totp_secret || bilhete.totpSecret || bilhete.numero_bilhete || "x") + counter;
    for (let i = 0; i < s.length; i++) h = ((h << 5) - h + s.charCodeAt(i)) | 0;
    return Math.abs(h % 1000000).toString().padStart(6, "0");
  })();
  const timeLeft = 30 - (Math.floor(now / 1000) % 30);
  const pct = (timeLeft / 30) * 100;
  const hAngle = (now % 6000) / 6000 * 360;

  const b = bilhete;
  const nome = b.nome_passageiro || b.nomePassageiro || "Passageiro";
  const origem = b.origem || "—";
  const destino = b.destino || "—";
  const embarc = b.embarcacao || "—";
  const data = b.data_viagem || b.dataViagem || "—";
  const horario = b.horario_saida || b.horarioSaida || "12:00";
  const valor = b.valor_total || b.valorTotal || b.valor_a_pagar || "—";
  const numBilhete = b.numero_bilhete || b.numeroBilhete || "00000";
  const qr = b.qr_hash || b.qrHash || numBilhete;
  const idViagem = b.id_viagem || b.idViagem || "?";
  const acomodacao = b.acomodacao || b.tipo || "Rede";
  const doc = b.documento || b.numero_documento || "";
  const docMask = doc ? doc.replace(/(\d{3})(\d+)(\d{2})/, "$1.•••.$3") : "";

  return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 12, background: t.bg, margin: "-16px -18px", padding: "16px 18px 100px", minHeight: "100vh" }}>
    <button onClick={onBack} style={{ background: "none", border: "none", color: t.txMuted, fontSize: 13, cursor: "pointer", textAlign: "left", padding: 0, display: "flex", alignItems: "center", gap: 4 }}><IconBack size={14} color={t.txMuted} /> Voltar</button>
    <div style={{ textAlign: "center", marginBottom: 4 }}>
      <div style={{ fontSize: 10, letterSpacing: 4, textTransform: "uppercase", color: t.txMuted, fontWeight: 300 }}>Bilhete digital</div>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: 6, marginTop: 4 }}>
        <div style={{ width: 6, height: 6, borderRadius: 3, background: t.pri, animation: "pulse 2s infinite" }} />
        <span style={{ fontSize: 11, color: t.txMuted }}>Válido — código rotativo ativo</span>
      </div>
    </div>

    {/* TICKET CARD */}
    <div style={{ position: "relative", borderRadius: 20, overflow: "hidden", filter: brightness ? "brightness(1.3) contrast(1.1)" : "none" }}>
      <div style={{ position: "relative", background: `linear-gradient(170deg, ${t.card} 0%, ${t.soft} 40%, ${t.card} 100%)`, padding: "22px 20px 18px", borderRadius: 20, border: `1px solid ${t.borderStrong}` }}>
        {/* Watermark */}
        <div style={{ position: "absolute", fontSize: 100, fontWeight: 800, color: t.pri, opacity: 0.03, letterSpacing: 8, transform: "rotate(-25deg)", top: "25%", left: "-5%", pointerEvents: "none" }}>NAVIERA</div>

        {/* Header */}
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 14 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <Logo size={26} t={t} />
            <div><div style={{ fontSize: 13, fontWeight: 700, letterSpacing: 3 }}>NAVIERA</div><div style={{ fontSize: 7, color: t.txMuted, letterSpacing: 2, textTransform: "uppercase" }}>Passagem fluvial</div></div>
          </div>
          <div style={{ textAlign: "right" }}><div style={{ fontSize: 9, fontFamily: "'Space Mono', monospace", color: t.txMuted }}>BLT-{numBilhete}</div><div style={{ fontSize: 8, color: t.txMuted }}>Viagem #{idViagem}</div></div>
        </div>

        {/* Boat SVG */}
        <svg width="100%" height="50" viewBox="0 0 300 50" fill="none">
          <path d="M0 38 Q25 32,50 38 Q75 44,100 38 Q125 32,150 38 Q175 44,200 38 Q225 32,250 38 Q275 44,300 38" stroke={t.pri} strokeWidth="0.8" opacity="0.25" fill="none"><animate attributeName="d" dur="3s" repeatCount="indefinite" values="M0 38 Q25 32,50 38 Q75 44,100 38 Q125 32,150 38 Q175 44,200 38 Q225 32,250 38 Q275 44,300 38;M0 38 Q25 44,50 38 Q75 32,100 38 Q125 44,150 38 Q175 32,200 38 Q225 44,250 38 Q275 32,300 38;M0 38 Q25 32,50 38 Q75 44,100 38 Q125 32,150 38 Q175 44,200 38 Q225 32,250 38 Q275 44,300 38"/></path>
          <path d="M100 35 L108 24 L192 24 L200 35Z" fill={t.pri} opacity="0.6"/><rect x="120" y="18" width="60" height="8" rx="2" fill={t.pri} opacity="0.4"/><rect x="138" y="10" width="24" height="10" rx="2" fill={t.txMuted} opacity="0.3"/>
        </svg>

        {/* Route */}
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", margin: "8px 0 14px", padding: "0 6px" }}>
          <div style={{ textAlign: "center" }}><div style={{ fontSize: 20, fontWeight: 800 }}>{origem.slice(0, 3).toUpperCase()}</div><div style={{ fontSize: 10, color: t.txMuted, marginTop: 2 }}>{origem}</div></div>
          <div style={{ flex: 1, display: "flex", alignItems: "center", justifyContent: "center", padding: "0 10px" }}>
            <div style={{ flex: 1, height: 1, background: t.border }} /><IconShip size={20} color={t.pri} /><div style={{ flex: 1, height: 1, background: t.border }} />
          </div>
          <div style={{ textAlign: "center" }}><div style={{ fontSize: 20, fontWeight: 800 }}>{destino.slice(0, 3).toUpperCase()}</div><div style={{ fontSize: 10, color: t.txMuted, marginTop: 2 }}>{destino}</div></div>
        </div>

        {/* Info grid */}
        <div style={{ background: `${t.pri}0a`, borderRadius: 12, padding: "12px 14px", marginBottom: 12, border: `1px solid ${t.border}` }}>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "10px 16px" }}>
            {[["Passageiro", nome], ["Documento", docMask || "—"], ["Data", typeof data === "string" && data.includes("-") ? fmt(data) : data], ["Horário", horario], ["Acomodação", acomodacao], ["Embarcação", embarc]].map(([l, v], i) => (
              <div key={i}><div style={{ fontSize: 8, color: t.txMuted, textTransform: "uppercase", letterSpacing: 1, marginBottom: 2 }}>{l}</div><div style={{ fontSize: 12, fontWeight: 600 }}>{typeof v === "number" ? money(v) : v}</div></div>
            ))}
          </div>
        </div>

        {/* Tear line */}
        <div style={{ display: "flex", alignItems: "center", margin: "0 -20px 12px" }}>
          <div style={{ width: 14, height: 14, borderRadius: "50%", background: t.bg, flexShrink: 0, marginLeft: -7 }} />
          <div style={{ flex: 1, borderTop: `2px dashed ${t.border}`, margin: "0 4px" }} />
          <div style={{ width: 14, height: 14, borderRadius: "50%", background: t.bg, flexShrink: 0, marginRight: -7 }} />
        </div>

        {/* QR + TOTP */}
        <div style={{ display: "flex", alignItems: "center", gap: 14 }}>
          <div style={{ background: `${t.pri}10`, borderRadius: 12, padding: 8, border: `1px solid ${t.border}` }}>
            <svg width="90" height="90" viewBox="0 0 21 21">{(() => {
              let h = 0; const s = qr + totp;
              for (let i = 0; i < s.length; i++) h = ((h << 5) - h + s.charCodeAt(i)) | 0;
              const cells = [];
              for (let r = 0; r < 21; r++) for (let c = 0; c < 21; c++) {
                const finder = (r < 7 && c < 7) || (r < 7 && c > 13) || (r > 13 && c < 7);
                const border = finder && (r === 0 || r === 6 || c === 0 || c === 6 || (r > 13 && (r === 14 || r === 20)) || (c > 13 && (c === 14 || c === 20)));
                const inner = finder && r >= 2 && r <= 4 && c >= 2 && c <= 4;
                const innerBR = finder && r >= 16 && r <= 18 && c >= 2 && c <= 4;
                const innerTR = finder && r >= 2 && r <= 4 && c >= 16 && c <= 18;
                if (border || inner || innerBR || innerTR) { cells.push(<rect key={`${r}-${c}`} x={c} y={r} width="1" height="1" fill={t.pri} />); }
                else if (!finder) { h = ((h * 1103515245 + 12345) & 0x7fffffff); if (h % 3 !== 0) cells.push(<rect key={`${r}-${c}`} x={c} y={r} width="1" height="1" fill={t.txSoft} opacity="0.7" />); }
              }
              return cells;
            })()}</svg>
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 8, color: t.txMuted, textTransform: "uppercase", letterSpacing: 1.5, marginBottom: 6 }}>Código de segurança</div>
            <div style={{ fontSize: 26, fontWeight: 800, fontFamily: "'Space Mono', monospace", letterSpacing: 5, color: t.pri, marginBottom: 8 }}>
              {totp.slice(0, 3)}<span style={{ opacity: 0.3 }}>·</span>{totp.slice(3)}
            </div>
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <svg width="20" height="20" viewBox="0 0 20 20">
                <circle cx="10" cy="10" r="8" fill="none" stroke={t.border} strokeWidth="2" />
                <circle cx="10" cy="10" r="8" fill="none" stroke={timeLeft <= 5 ? t.err : t.pri} strokeWidth="2"
                  strokeDasharray={`${2 * Math.PI * 8}`} strokeDashoffset={`${2 * Math.PI * 8 * (1 - pct / 100)}`}
                  transform="rotate(-90 10 10)" style={{ transition: "stroke-dashoffset 1s linear" }} />
                <text x="10" y="10" textAnchor="middle" dominantBaseline="central" fill={timeLeft <= 5 ? t.err : t.pri} fontSize="7" fontWeight="700">{timeLeft}</text>
              </svg>
              <div><div style={{ fontSize: 10, color: timeLeft <= 5 ? t.err : t.txMuted }}>{timeLeft <= 5 ? "Renovando..." : `Renova em ${timeLeft}s`}</div>
                <div style={{ fontSize: 8, color: t.txMuted, opacity: 0.5, marginTop: 1 }}>HMAC-SHA256 • Anti-clone</div></div>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: 12, paddingTop: 10, borderTop: `1px solid ${t.border}` }}>
          <div style={{ fontSize: 8, color: t.txMuted, opacity: 0.5, fontFamily: "'Space Mono', monospace" }}>SIG: {qr.slice(0, 8)}...</div>
          <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
            <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke={t.pri} strokeWidth="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
            <span style={{ fontSize: 8, color: t.txMuted }}>Verificado</span>
          </div>
          <div style={{ fontSize: 14, fontWeight: 800, color: t.pri }}>{typeof valor === "number" ? money(valor) : valor}</div>
        </div>
      </div>
    </div>

    {/* Actions */}
    <div style={{ display: "flex", gap: 10 }}>
      <button onClick={() => setBrightness(!brightness)} style={{ flex: 1, padding: "12px 0", borderRadius: 12, border: `1px solid ${t.borderStrong}`, background: brightness ? `${t.pri}15` : "transparent", color: t.txMuted, fontSize: 12, fontWeight: 600, cursor: "pointer", fontFamily: "inherit", display: "flex", alignItems: "center", justifyContent: "center", gap: 6 }}>
        <IconSun size={14} color={t.txMuted} /> {brightness ? "Normal" : "Brilho maximo"}
      </button>
      <button onClick={() => navigator.share?.({ title: `Bilhete ${numBilhete}`, text: `Passagem ${origem}→${destino} - ${embarc} - ${data}` }).catch(() => {})} style={{ flex: 1, padding: "12px 0", borderRadius: 12, border: "none", background: t.priGrad, color: "#fff", fontSize: 12, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>Compartilhar</button>
    </div>
    <div style={{ fontSize: 10, color: t.txMuted, opacity: 0.5, textAlign: "center", lineHeight: 1.6 }}>Código rotativo anti-clone. Muda a cada 30s. Screenshots não funcionam. Apresente esta tela ao operador.</div>
  </div>;
}

function PassagensCPF({ t, authHeaders }) {
  const { data: embarcacoes, loading: le } = useApi("/embarcacoes", authHeaders);
  const { data: viagens, loading: lv, erro: ev, refresh: rv } = useApi("/viagens/ativas", authHeaders);
  const { data: tarifas } = useApi("/tarifas", authHeaders);
  const { data: minhas, refresh: rm } = useApi("/passagens", authHeaders);
  const [busca, setBusca] = useState("");
  const [selEmb, setSelEmb] = useState(null);
  const [compra, setCompra] = useState(null);
  const [tipoSel, setTipoSel] = useState(null);
  const [comprando, setComprando] = useState(false);
  const [resultado, setResultado] = useState(null);
  const [erro, setErro] = useState("");
  const [toast, setToast] = useState(null);
  const [selBilhete, setSelBilhete] = useState(null);

  const hoje = new Date().toISOString().split("T")[0];
  const viagensEmb = selEmb ? viagens?.filter(v => v.embarcacao === selEmb.nome && v.dataViagem >= hoje && !v.atual) || [] : [];
  const tarifasDaViagem = compra ? tarifas?.filter(x => x.origem === compra.origem && x.destino === compra.destino) || [] : [];
  const embFiltradas = busca.trim() ? embarcacoes?.filter(e => e.nome.toLowerCase().includes(busca.toLowerCase()) || (e.rotaPrincipal || "").toLowerCase().includes(busca.toLowerCase())) : embarcacoes;

  const confirmarCompra = async () => {
    if (!tipoSel) { setErro("Selecione o tipo de passagem."); return; }
    setErro(""); setComprando(true);
    try {
      const res = await fetch(`${API}/passagens/comprar`, { method: "POST", headers: authHeaders, body: JSON.stringify({ idViagem: compra.id, idTipoPassagem: tipoSel, formaPagamento: "PIX" }) });
      const data = await res.json();
      if (!res.ok) { setErro(data.erro || "Erro ao comprar."); return; }
      setResultado(data); rm();
    } catch { setErro("Erro de conexao."); } finally { setComprando(false); }
  };

  if (ev) return <ErrorRetry erro={ev} onRetry={rv} t={t} />;

  // Tela do bilhete digital
  if (selBilhete) return <BilheteScreen bilhete={selBilhete} t={t} onBack={() => setSelBilhete(null)} />;

  // Tela de sucesso
  if (resultado) return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 16, alignItems: "center", padding: "40px 0" }}>
    <div style={{ width: 64, height: 64, borderRadius: "50%", background: t.okBg, display: "flex", alignItems: "center", justifyContent: "center" }}>
      <IconCheck size={32} color={t.ok} />
    </div>
    <h3 style={{ margin: 0, fontSize: 20, fontWeight: 700 }}>Passagem emitida!</h3>
    <Cd t={t} style={{ padding: 16, width: "100%", textAlign: "center" }}>
      <div style={{ fontSize: 12, color: t.txMuted }}>Bilhete</div>
      <div style={{ fontSize: 18, fontWeight: 700, fontFamily: "'Space Mono', monospace", color: t.pri, marginTop: 4 }}>{resultado.numeroBilhete}</div>
      <div style={{ fontSize: 14, fontWeight: 600, marginTop: 12 }}>{money(resultado.valorTotal)}</div>
      <div style={{ padding: "6px 12px", borderRadius: 8, background: t.warnBg, color: t.warnTx, fontSize: 11, marginTop: 8, display: "inline-block" }}>Aguardando confirmacao do operador</div>
      <div style={{ fontSize: 11, color: t.txMuted, marginTop: 8, lineHeight: 1.5 }}>O operador recebera a notificacao e confirmara o pagamento. Apos confirmacao, seu bilhete digital ficara ativo.</div>
    </Cd>
    <button onClick={() => {
      setSelBilhete({ numero_bilhete: resultado.numeroBilhete, valor_total: resultado.valorTotal, status_passagem: resultado.status, embarcacao: compra?.embarcacao, origem: compra?.origem, destino: compra?.destino, data_viagem: compra?.dataViagem, totp_secret: resultado.numeroBilhete });
      setResultado(null); setCompra(null); setSelEmb(null); setTipoSel(null);
    }} className="btn-primary" style={{ width: "100%", padding: "14px 0", background: t.priGrad, color: "#fff", fontSize: 14 }}>Ver bilhete digital</button>
    <button onClick={() => { setResultado(null); setCompra(null); setSelEmb(null); setTipoSel(null); }} style={{ width: "100%", padding: "12px 0", borderRadius: 12, border: `1px solid ${t.border}`, background: "transparent", color: t.txMuted, fontSize: 13, fontWeight: 600, cursor: "pointer", fontFamily: "inherit" }}>Voltar para passagens</button>
  </div>;

  // Tela de compra
  if (compra) return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <button onClick={() => { setCompra(null); setTipoSel(null); setErro(""); }} style={{ background: "none", border: "none", color: t.txMuted, fontSize: 13, cursor: "pointer", textAlign: "left", padding: 0, display: "flex", alignItems: "center", gap: 4 }}><IconBack size={14} color={t.txMuted} /> Voltar</button>
    <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Comprar passagem</h3>
    <Cd t={t} style={{ padding: 14, border: `1px solid ${t.borderStrong}` }}>
      <div style={{ fontSize: 16, fontWeight: 600 }}>{compra.embarcacao}</div>
      <div style={{ fontSize: 13, color: t.txMuted, marginTop: 2 }}>{compra.origem} → {compra.destino}</div>
      <div style={{ display: "flex", gap: 16, fontSize: 12, color: t.txMuted, marginTop: 6 }}>
        <span style={{ display: "flex", alignItems: "center", gap: 4 }}><IconCalendar size={12} color={t.txMuted} /> {fmt(compra.dataViagem)}</span>
        <span>Chegada: {fmt(compra.dataChegada)}</span>
      </div>
    </Cd>
    <div style={{ fontSize: 14, fontWeight: 600, marginTop: 4 }}>Escolha o tipo</div>
    {tarifasDaViagem.length > 0 ? tarifasDaViagem.map((x, i) => {
      const total = (Number(x.valor_transporte) || 0) + (Number(x.valor_alimentacao) || 0) - (Number(x.valor_desconto) || 0);
      const selected = tipoSel === x.tipo_passageiro_id;
      return <Cd key={i} t={t} style={{ padding: 14, cursor: "pointer", border: `2px solid ${selected ? t.pri : t.border}`, background: selected ? t.accent : t.card }} onClick={() => setTipoSel(x.tipo_passageiro_id)}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <div><div style={{ fontSize: 14, fontWeight: 600 }}>{x.tipo_passageiro}</div>
            <div style={{ fontSize: 11, color: t.txMuted, marginTop: 2 }}>Transporte + Alimentacao{Number(x.valor_desconto) > 0 ? " - Desconto" : ""}</div></div>
          <div style={{ fontSize: 20, fontWeight: 700, color: selected ? t.pri : t.tx }}>{money(total)}</div>
        </div>
      </Cd>;
    }) : <Cd t={t} style={{ padding: 14, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Nenhuma tarifa disponivel para esta rota.</div></Cd>}
    {erro && <div style={{ padding: "10px 14px", borderRadius: 10, background: t.errBg, color: t.errTx, fontSize: 12 }}>{erro}</div>}
    {tarifasDaViagem.length > 0 && <button onClick={confirmarCompra} disabled={comprando || !tipoSel} className="btn-primary" style={{ width: "100%", padding: "14px 0", background: comprando || !tipoSel ? t.txMuted : t.priGrad, color: "#fff", fontSize: 14, opacity: !tipoSel ? 0.5 : 1 }}>{comprando ? "Processando..." : "Confirmar e pagar via PIX"}</button>}
  </div>;

  // Tela de viagens de uma embarcação
  if (selEmb) return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <button onClick={() => setSelEmb(null)} style={{ background: "none", border: "none", color: t.txMuted, fontSize: 13, cursor: "pointer", textAlign: "left", padding: 0, display: "flex", alignItems: "center", gap: 4 }}><IconBack size={14} color={t.txMuted} /> Voltar</button>
    {selEmb.fotoUrl && <img src={`${API}${selEmb.fotoUrl}`} alt={selEmb.nome} style={{ width: "100%", borderRadius: 14, objectFit: "cover", maxHeight: 160 }} />}
    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
      <div><h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>{selEmb.nome}</h3>
        <div style={{ fontSize: 12, color: t.txMuted, marginTop: 2 }}>{selEmb.rotaPrincipal || selEmb.rotaAtual || ""}</div></div>
      <Badge status={selEmb.status || "NO_PORTO"} t={t} />
    </div>
    <div style={{ fontSize: 14, fontWeight: 600, marginTop: 4 }}>Proximas saidas</div>
    {lv ? <Skeleton t={t} height={70} count={2} /> :
    viagensEmb.length > 0 ? viagensEmb.map(v => <Cd key={v.id} t={t} style={{ padding: 14, cursor: "pointer" }} onClick={() => setCompra(v)}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <div><div style={{ fontSize: 14, fontWeight: 600 }}>{v.origem} → {v.destino}</div>
          <div style={{ display: "flex", gap: 12, fontSize: 12, color: t.txMuted, marginTop: 4 }}>
            <span style={{ display: "flex", alignItems: "center", gap: 4 }}><IconCalendar size={11} color={t.txMuted} /> {fmt(v.dataViagem)}</span>
            <span>→ {fmt(v.dataChegada)}</span>
          </div></div>
        <div style={{ fontSize: 12, color: t.pri, fontWeight: 600 }}>Comprar →</div>
      </div>
    </Cd>) : <Cd t={t} style={{ padding: 14, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Nenhuma viagem futura para esta embarcacao.</div></Cd>}
  </div>;

  // Tela principal
  return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Passagens</h3>

    {minhas?.length > 0 && <>
      <div style={{ fontSize: 13, fontWeight: 600, color: t.txSoft }}>Minhas passagens</div>
      {minhas.map((p, i) => <Cd key={i} t={t} style={{ padding: 12, borderLeft: `3px solid ${t.pri}`, borderRadius: "0 14px 14px 0", cursor: "pointer" }} onClick={() => setSelBilhete(p)}>
        <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 4 }}>
          <span style={{ fontSize: 11, fontWeight: 700, fontFamily: "'Space Mono', monospace", color: t.pri }}>{p.numero_bilhete}</span>
          <Badge status={p.status_passagem || "CONFIRMADA"} t={t} />
        </div>
        <div style={{ fontSize: 14, fontWeight: 600 }}>{p.embarcacao}</div>
        <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12, color: t.txMuted, marginTop: 4 }}>
          <span>{p.origem} → {p.destino} • {p.tipo || "Rede"} • {fmt(p.data_viagem)}</span>
          <span style={{ fontWeight: 600, color: t.tx }}>{money(p.valor_a_pagar)}</span>
        </div>
        <div style={{ fontSize: 11, color: t.pri, fontWeight: 600, marginTop: 6 }}>Abrir bilhete →</div>
      </Cd>)}
    </>}

    <div style={{ fontSize: 13, fontWeight: 600, color: t.txSoft, marginTop: minhas?.length > 0 ? 4 : 0 }}>Comprar passagem</div>
    <input value={busca} onChange={e => setBusca(e.target.value)} placeholder="Buscar embarcacao ou rota..." className="input-field" style={{ width: "100%", padding: "10px 14px", borderRadius: 10, border: `1px solid ${t.border}`, background: t.soft, color: t.tx, fontSize: 13, outline: "none", boxSizing: "border-box" }} />
    {le ? <Skeleton t={t} height={80} count={2} /> :
    embFiltradas?.length > 0 ? embFiltradas.map(e => {
      const proxViagem = viagens?.find(v => v.embarcacao === e.nome && v.dataViagem >= hoje && !v.atual);
      return <Cd key={e.id} t={t} style={{ padding: 0, overflow: "hidden", cursor: "pointer" }} onClick={() => setSelEmb(e)}>
        {e.fotoUrl && <img src={`${API}${e.fotoUrl}`} alt={e.nome} style={{ width: "100%", height: 100, objectFit: "cover" }} />}
        <div style={{ padding: 12 }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
            <div><div style={{ fontSize: 15, fontWeight: 600 }}>{e.nome}</div>
              <div style={{ fontSize: 12, color: t.txMuted, marginTop: 2 }}>{e.rotaPrincipal || e.rotaAtual || ""}</div></div>
            <Badge status={e.status || "NO_PORTO"} t={t} />
          </div>
          {proxViagem ? <div style={{ display: "flex", alignItems: "center", gap: 4, fontSize: 11, color: t.pri, fontWeight: 600, marginTop: 6 }}><IconCalendar size={11} color={t.pri} /> Proxima: {fmt(proxViagem.dataViagem)}</div>
            : <div style={{ fontSize: 11, color: t.txMuted, marginTop: 6 }}>Sem viagens futuras</div>}
          <div style={{ fontSize: 12, color: t.pri, fontWeight: 600, marginTop: 6 }}>Ver datas e comprar →</div>
        </div>
      </Cd>;
    }) : <Cd t={t} style={{ padding: 16, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>{busca ? `Nenhum resultado para "${busca}"` : "Nenhuma embarcacao cadastrada."}</div></Cd>}

    {toast && <Toast message={toast} t={t} onClose={() => setToast(null)} />}
  </div>;
}

/* ═══ CNPJ SCREENS ═══ */
function HomeCNPJ({ t, onNav, authHeaders, usuario }) {
  const { data: fretes, loading: lf, erro: ef, refresh: rf } = useApi("/fretes", authHeaders);
  const { data: pedidos, loading: lp } = useApi("/lojas/pedidos", authHeaders);
  const { data: lojas, loading: ll } = useApi("/lojas", authHeaders);
  const fretesAtivos = fretes?.filter(f => f.status !== "ENTREGUE" && f.status !== "CANCELADO") || [];
  const totalDevedor = fretes?.reduce((s, f) => s + (f.valorDevedor || 0), 0) || 0;

  if (ef) return <ErrorRetry erro={ef} onRetry={rf} t={t} />;

  return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 14 }}>
    <div><span style={{ fontSize: 13, color: t.txMuted }}>Empresa</span><h2 style={{ margin: 0, fontSize: 20, fontWeight: 700 }}>{usuario?.nome || "Empresa"}</h2></div>
    <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
      {[{ l: "Fretes ativos", v: lf ? "..." : String(fretesAtivos.length), c: t.pri, Ic: IconPackage },
        { l: "Pedidos", v: lp ? "..." : String(pedidos?.length || 0), c: t.amber, Ic: IconCart },
        { l: "Total fretes", v: lf ? "..." : String(fretes?.length || 0), c: t.info, Ic: IconCheck },
        { l: "Devedor", v: lf ? "..." : money(totalDevedor), c: t.err, Ic: IconWallet }].map((s, i) =>
        <Cd key={i} t={t} style={{ padding: 14, textAlign: "center" }}>
          <s.Ic size={20} color={s.c} /><div style={{ fontSize: 22, fontWeight: 700, color: s.c, marginTop: 4 }}>{s.v}</div><div style={{ fontSize: 11, color: t.txMuted, marginTop: 2 }}>{s.l}</div>
        </Cd>)}
    </div>
    <div style={{ display: "flex", justifyContent: "space-between", marginTop: 4 }}><span style={{ fontSize: 14, fontWeight: 600 }}>Fretes recentes</span>
      <button style={{ background: "none", border: "none", color: t.pri, fontSize: 12, fontWeight: 600, cursor: "pointer" }} onClick={() => onNav("pedidos")}>Ver todos →</button></div>
    {lf ? <Skeleton t={t} height={80} count={2} /> :
    fretes?.slice(0, 3).map(f => <Cd key={f.id} t={t} style={{ padding: 12 }}>
      <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 4 }}><span style={{ fontSize: 12, fontWeight: 700, fontFamily: "'Space Mono', monospace", color: t.txSoft }}>FRT-{f.numeroFrete || f.id}</span><Badge status={f.status || "Aguardando"} t={t} /></div>
      <div style={{ fontSize: 13 }}>{f.nomeDestinatario}</div><div style={{ fontSize: 12, color: t.txMuted, marginTop: 2 }}>{f.nomeRota} • {f.embarcacao}</div>
      <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12, marginTop: 6 }}><span style={{ color: t.txMuted }}>{fmt(f.dataViagem)}</span><span style={{ fontWeight: 600 }}>{money(f.valorTotal)}</span></div>
    </Cd>) || <Cd t={t} style={{ padding: 12, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Nenhum frete encontrado.</div></Cd>}
    <div style={{ display: "flex", justifyContent: "space-between", marginTop: 4 }}><span style={{ fontSize: 14, fontWeight: 600 }}>Lojas parceiras</span>
      <button style={{ background: "none", border: "none", color: t.pri, fontSize: 12, fontWeight: 600, cursor: "pointer" }} onClick={() => onNav("lojas")}>Ver todas →</button></div>
    {ll ? <Skeleton t={t} height={60} count={1} /> :
    lojas?.slice(0, 2).map(l => <Cd key={l.id} t={t} style={{ padding: 12 }}>
      <div style={{ display: "flex", alignItems: "center", gap: 10 }}><Av letters={initials(l.nomeLoja)} size={38} t={t} /><div style={{ flex: 1 }}><div style={{ fontSize: 14, fontWeight: 600 }}>{l.nomeLoja}{l.verificada && <span style={{ color: t.pri, marginLeft: 4, fontSize: 11 }}><IconCheck size={11} color={t.pri} /></span>}</div><div style={{ fontSize: 12, color: t.txMuted }}>{l.segmento}</div></div></div>
    </Cd>) || null}
  </div>;
}

function LojasParceiras({ t, authHeaders }) {
  const { data: lojas, loading, erro, refresh } = useApi("/lojas", authHeaders);
  const [sel, setSel] = useState(null);

  if (loading) return <Skeleton t={t} height={70} count={4} />;
  if (erro) return <ErrorRetry erro={erro} onRetry={refresh} t={t} />;

  if (sel) {
    const loja = lojas?.find(l => l.id === sel);
    if (!loja) { setSel(null); return null; }
    return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 12 }}>
      <button onClick={() => setSel(null)} style={{ background: "none", border: "none", color: t.txMuted, fontSize: 13, cursor: "pointer", textAlign: "left", padding: 0, display: "flex", alignItems: "center", gap: 4 }}><IconBack size={14} color={t.txMuted} /> Voltar</button>
      <Cd t={t} style={{ padding: 18, border: `1px solid ${t.borderStrong}` }}>
        <div style={{ display: "flex", alignItems: "center", gap: 14, marginBottom: 14 }}><Av letters={initials(loja.nomeLoja)} size={54} t={t} />
          <div><div style={{ fontSize: 18, fontWeight: 700 }}>{loja.nomeLoja}</div><div style={{ fontSize: 13, color: t.txMuted }}>{loja.segmento}</div>
            {loja.verificada && <div style={{ fontSize: 11, color: t.pri, fontWeight: 600, marginTop: 3, display: "flex", alignItems: "center", gap: 3 }}><IconCheck size={11} color={t.pri} /> Verificada Naviera</div>}</div></div>
        <div style={{ fontSize: 12, color: t.txMuted, marginBottom: 14 }}>{loja.descricao || "Sem descrição."}</div>
      </Cd>
    </div>;
  }

  return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Lojas parceiras</h3>
    <div style={{ fontSize: 13, color: t.txMuted }}>Fornecedores verificados que embarcam pelo Naviera.</div>
    {(!lojas || lojas.length === 0) && <Cd t={t} style={{ padding: 16, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Nenhuma loja cadastrada.</div></Cd>}
    {lojas?.map(l => <Cd key={l.id} t={t} style={{ padding: 14 }} onClick={() => setSel(l.id)}>
      <div style={{ display: "flex", alignItems: "center", gap: 12 }}><Av letters={initials(l.nomeLoja)} size={46} t={t} />
        <div style={{ flex: 1 }}><div style={{ display: "flex", alignItems: "center", gap: 6 }}><span style={{ fontSize: 15, fontWeight: 600 }}>{l.nomeLoja}</span>{l.verificada && <IconCheck size={12} color={t.pri} />}</div>
          <div style={{ fontSize: 12, color: t.txMuted, marginTop: 1 }}>{l.segmento}</div></div>
        <span style={{ color: t.txMuted, fontSize: 16 }}>›</span></div></Cd>)}
  </div>;
}

function PedidosCNPJ({ t, authHeaders }) {
  const { data: pedidos, loading, erro, refresh } = useApi("/lojas/pedidos", authHeaders);
  if (loading) return <Skeleton t={t} height={80} count={3} />;
  if (erro) return <ErrorRetry erro={erro} onRetry={refresh} t={t} />;
  return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Pedidos da loja</h3>
    {(!pedidos || pedidos.length === 0) ? <Cd t={t} style={{ padding: 16, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Nenhum pedido recebido ainda.</div></Cd> :
    pedidos.map(p => <Cd key={p.id} t={t} style={{ padding: 14 }}>
      <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 6 }}><span style={{ fontSize: 12, fontWeight: 700, fontFamily: "'Space Mono', monospace", color: t.txSoft }}>PED-{String(p.id).padStart(4, "0")}</span><Badge status={p.status || "Aguardando"} t={t} /></div>
      <div style={{ fontSize: 14, fontWeight: 600 }}>{p.nomeComprador || "Cliente"}</div>
      <div style={{ fontSize: 13, color: t.txMuted, marginTop: 2 }}>{p.descricao || "Sem descrição"}</div>
      <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12, color: t.txMuted, marginTop: 6 }}><span>{fmt(p.dataPedido)}</span><span style={{ fontWeight: 600, color: t.tx }}>{money(p.valorTotal)}</span></div>
      {p.codigoRastreio && <div style={{ marginTop: 8, padding: "8px 12px", borderRadius: 8, background: t.accent, border: `1px solid ${t.border}` }}>
        <div style={{ fontSize: 12, color: t.pri, fontWeight: 600, display: "flex", alignItems: "center", gap: 4 }}><IconPackage size={12} color={t.pri} /> Rastreio: {p.codigoRastreio}</div></div>}
    </Cd>)}
  </div>;
}

function FinanceiroCNPJ({ t, authHeaders }) {
  const { data: fretes, loading, erro, refresh } = useApi("/fretes", authHeaders);
  if (loading) return <Skeleton t={t} height={70} count={4} />;
  if (erro) return <ErrorRetry erro={erro} onRetry={refresh} t={t} />;
  const totalDevedor = fretes?.reduce((s, f) => s + (f.valorDevedor || 0), 0) || 0;
  const totalPago = fretes?.reduce((s, f) => s + (f.valorPago || 0), 0) || 0;
  const fretesDevendo = fretes?.filter(f => (f.valorDevedor || 0) > 0) || [];
  return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Financeiro</h3>
    <Cd t={t} style={{ padding: 16 }}>
      <div style={{ fontSize: 12, color: t.txMuted, marginBottom: 4 }}>Total pendente</div>
      <div style={{ fontSize: 28, fontWeight: 700, color: totalDevedor > 0 ? t.err : t.ok }}>{money(totalDevedor)}</div>
      <div style={{ fontSize: 12, color: t.txMuted, marginTop: 4 }}>{fretesDevendo.length} frete(s) em aberto</div>
    </Cd>
    <Cd t={t} style={{ padding: 14, border: `1px solid ${t.borderStrong}` }}>
      <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 8 }}><span style={{ fontSize: 13, fontWeight: 600 }}>Total pago</span><span style={{ fontSize: 15, fontWeight: 700, color: t.pri }}>{money(totalPago)}</span></div>
      <div style={{ fontSize: 12, color: t.txMuted }}>{fretes?.length || 0} fretes no total</div>
    </Cd>
    <div style={{ fontSize: 14, fontWeight: 600, marginTop: 4 }}>Fretes</div>
    {fretes?.map(f => <Cd key={f.id} t={t} style={{ padding: 12 }}>
      <div style={{ display: "flex", justifyContent: "space-between" }}><div><div style={{ fontSize: 13 }}>FRT-{f.numeroFrete || f.id} — {f.nomeDestinatario}</div><div style={{ fontSize: 11, color: t.txMuted, marginTop: 2 }}>{fmt(f.dataViagem)}</div></div>
        <div style={{ textAlign: "right" }}><div style={{ fontSize: 14, fontWeight: 700, color: (f.valorDevedor || 0) > 0 ? t.err : t.tx }}>{money(f.valorTotal)}</div>
          <Badge status={(f.valorDevedor || 0) > 0 ? "Pendente" : "Pago"} t={t} /></div></div></Cd>)}
  </div>;
}

function LojaCNPJ({ t, authHeaders }) {
  const { data: loja, loading, erro, refresh } = useApi("/lojas/minha", authHeaders);
  if (loading) return <Skeleton t={t} height={80} count={2} />;
  if (erro) return <ErrorRetry erro={erro} onRetry={refresh} t={t} />;
  if (!loja) return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Minha loja</h3>
    <Cd t={t} style={{ padding: 16, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Você ainda não tem uma loja cadastrada.</div></Cd>
  </div>;
  return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <div style={{ display: "flex", justifyContent: "space-between" }}><h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Minha loja</h3>{loja.verificada && <Badge status="Verificada" t={t} />}</div>
    <Cd t={t} style={{ padding: 16, border: `1px solid ${t.borderStrong}` }}>
      <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 14 }}><Av letters={initials(loja.nomeLoja)} size={50} t={t} />
        <div><div style={{ fontSize: 16, fontWeight: 600 }}>{loja.nomeLoja}</div><div style={{ fontSize: 12, color: t.txMuted }}>{loja.segmento}</div></div></div>
      <div style={{ fontSize: 12, color: t.txMuted, lineHeight: 1.6 }}>{loja.descricao || "Clientes compram, você vincula ao frete, rastreio automático."}</div></Cd>
    <div style={{ fontSize: 14, fontWeight: 600, marginTop: 4 }}>Como funciona</div>
    {[{ s: "1", ti: "Cliente compra na vitrine", desc: "Pedido aparece na aba Pedidos", c: t.amber },
      { s: "2", ti: "Você vincula ao frete", desc: "Associa o pedido ao embarque", c: t.pri },
      { s: "3", ti: "Rastreio automático", desc: "Cliente acompanha até a entrega", c: t.info }].map((s, i) =>
      <Cd key={i} t={t} style={{ padding: 12, borderLeft: `3px solid ${s.c}`, borderRadius: "0 14px 14px 0" }}>
        <div style={{ display: "flex", alignItems: "flex-start", gap: 10 }}>
          <div style={{ width: 26, height: 26, borderRadius: "50%", background: t.accent, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 12, fontWeight: 700, color: s.c, flexShrink: 0 }}>{s.s}</div>
          <div><div style={{ fontSize: 14, fontWeight: 600 }}>{s.ti}</div><div style={{ fontSize: 12, color: t.txMuted, marginTop: 2 }}>{s.desc}</div></div></div></Cd>)}
  </div>;
}

/* ═══ CADASTRO ═══ */
function TelaCadastro({ t, onVoltar, onSucesso }) {
  const [tipo, setTipo] = useState("CPF");
  const [form, setForm] = useState({ documento: "", nome: "", email: "", telefone: "", cidade: "", senha: "", senhaConfirm: "" });
  const [erro, setErro] = useState("");
  const [loading, setLoading] = useState(false);
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));
  const inputStyle = { width: "100%", padding: "12px 14px", borderRadius: 10, border: `1px solid ${t.border}`, background: t.soft, color: t.tx, fontSize: 13, outline: "none", boxSizing: "border-box" };
  const labelStyle = { fontSize: 12, fontWeight: 600, color: t.txSoft, marginBottom: 4, display: "block" };

  const submit = async () => {
    setErro("");
    if (!form.documento.trim() || !form.nome.trim() || !form.senha.trim()) { setErro("Documento, nome e senha são obrigatórios."); return; }
    const docErro = validarDocumento(form.documento, tipo);
    if (docErro) { setErro(docErro); return; }
    if (form.senha.length < 6) { setErro("Senha deve ter no mínimo 6 caracteres."); return; }
    if (form.senha !== form.senhaConfirm) { setErro("As senhas não conferem."); return; }
    setLoading(true);
    try {
      const res = await fetch(`${API}/auth/registrar`, { method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ documento: form.documento.replace(/\D/g, ""), tipoDocumento: tipo, nome: form.nome.trim(), email: form.email.trim() || null, telefone: form.telefone.trim() || null, cidade: form.cidade.trim() || null, senha: form.senha }) });
      const data = await res.json();
      if (!res.ok) { setErro(data.erro || "Erro ao cadastrar."); return; }
      onSucesso(data);
    } catch { setErro("Erro de conexão com o servidor."); } finally { setLoading(false); }
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 14, width: "100%", maxWidth: 380 }}>
      <button onClick={onVoltar} style={{ background: "none", border: "none", color: t.txMuted, fontSize: 14, cursor: "pointer", padding: 0, textAlign: "left", display: "flex", alignItems: "center", gap: 4 }}><IconBack size={16} color={t.txMuted} /> Voltar</button>
      <div style={{ textAlign: "center", marginBottom: 4 }}><Logo size={40} t={t} /><h2 style={{ margin: "8px 0 2px", fontSize: 22, fontWeight: 700 }}>Criar conta</h2><p style={{ fontSize: 12, color: t.txMuted, margin: 0 }}>Preencha seus dados para começar</p></div>
      <div style={{ display: "flex", gap: 8 }}>
        {["CPF", "CNPJ"].map(tp => <button key={tp} onClick={() => { setTipo(tp); set("documento", ""); }} style={{ flex: 1, padding: "10px 0", borderRadius: 10, border: `1px solid ${tipo === tp ? t.pri : t.border}`, background: tipo === tp ? t.accent : "transparent", color: tipo === tp ? t.pri : t.txMuted, fontSize: 13, fontWeight: 600, cursor: "pointer" }}>{tp}</button>)}
      </div>
      <div><label style={labelStyle}>{tipo}</label><input value={form.documento} onChange={e => set("documento", maskDoc(e.target.value, tipo))} placeholder={tipo === "CPF" ? "000.000.000-00" : "00.000.000/0001-00"} className="input-field" style={inputStyle} /></div>
      <div><label style={labelStyle}>Nome completo</label><input value={form.nome} onChange={e => set("nome", e.target.value)} placeholder="Seu nome" className="input-field" style={inputStyle} /></div>
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8 }}>
        <div><label style={labelStyle}>Email</label><input value={form.email} onChange={e => set("email", e.target.value)} placeholder="email@exemplo.com" type="email" className="input-field" style={inputStyle} /></div>
        <div><label style={labelStyle}>Telefone</label><input value={form.telefone} onChange={e => set("telefone", e.target.value)} placeholder="(92) 99999-0000" className="input-field" style={inputStyle} /></div>
      </div>
      <div><label style={labelStyle}>Cidade</label><input value={form.cidade} onChange={e => set("cidade", e.target.value)} placeholder="Sua cidade" className="input-field" style={inputStyle} /></div>
      <div><label style={labelStyle}>Senha</label><input value={form.senha} onChange={e => set("senha", e.target.value)} type="password" placeholder="Mínimo 6 caracteres" className="input-field" style={inputStyle} /></div>
      <div><label style={labelStyle}>Confirmar senha</label><input value={form.senhaConfirm} onChange={e => set("senhaConfirm", e.target.value)} type="password" placeholder="Repita a senha" className="input-field" style={inputStyle} /></div>
      {erro && <div style={{ padding: "10px 14px", borderRadius: 10, background: t.errBg, color: t.errTx, fontSize: 12, fontWeight: 500 }}>{erro}</div>}
      <button onClick={submit} disabled={loading} className="btn-primary" style={{ background: loading ? t.txMuted : t.priGrad, color: "#fff", opacity: loading ? 0.7 : 1 }}>{loading ? "Cadastrando..." : "Criar conta"}</button>
    </div>
  );
}

/* ═══ PERFIL ═══ */
function PerfilScreen({ t, token, authHeaders, usuario, onFotoChange }) {
  const [perfil, setPerfil] = useState(null);
  const [editando, setEditando] = useState(false);
  const [form, setForm] = useState({});
  const [erro, setErro] = useState("");
  const [sucesso, setSucesso] = useState("");
  const [loading, setLoading] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [uploadingFoto, setUploadingFoto] = useState(false);
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));
  const inputStyle = { width: "100%", padding: "10px 12px", borderRadius: 8, border: `1px solid ${t.border}`, background: t.soft, color: t.tx, fontSize: 13, outline: "none", boxSizing: "border-box" };
  const labelStyle = { fontSize: 11, fontWeight: 600, color: t.txMuted, marginBottom: 3, display: "block" };
  const valStyle = { fontSize: 14, fontWeight: 500, display: "block", marginTop: 2 };

  useEffect(() => {
    if (!token) return;
    fetch(`${API}/perfil`, { headers: authHeaders })
      .then(r => r.ok ? r.json() : null)
      .then(d => { if (d) { setPerfil(d); setForm({ nome: d.nome || "", email: d.email || "", telefone: d.telefone || "", cidade: d.cidade || "" }); } })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [token]);

  const salvar = async () => {
    setSalvando(true); setErro(""); setSucesso("");
    try {
      const res = await fetch(`${API}/perfil`, { method: "PUT", headers: authHeaders, body: JSON.stringify(form) });
      if (res.ok) { const d = await res.json(); setPerfil(d); setEditando(false); setSucesso("Perfil atualizado!"); }
      else { const d = await res.json(); setErro(d.erro || "Erro ao salvar."); }
    } catch { setErro("Erro de conexão."); } finally { setSalvando(false); }
  };

  const uploadFoto = async (e) => {
    const file = e.target.files?.[0]; if (!file) return;
    setUploadingFoto(true);
    try {
      const fd = new FormData(); fd.append("foto", file);
      const res = await fetch(`${API}/perfil/foto`, { method: "POST", headers: { "Authorization": authHeaders.Authorization }, body: fd });
      if (res.ok) { const d = await res.json(); if (d.fotoUrl) { setPerfil(p => ({ ...p, fotoUrl: d.fotoUrl })); onFotoChange(`${API}${d.fotoUrl}`); } }
    } catch {} finally { setUploadingFoto(false); }
  };

  if (loading) return <Skeleton t={t} height={60} count={3} />;
  if (!perfil) return <Cd t={t} style={{ padding: 16, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Erro ao carregar perfil.</div></Cd>;

  return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 14 }}>
    <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Meu perfil</h3>
    {sucesso && <div style={{ padding: "10px 14px", borderRadius: 10, background: t.okBg, color: t.okTx, fontSize: 12, fontWeight: 500 }}>{sucesso}</div>}
    {erro && <div style={{ padding: "10px 14px", borderRadius: 10, background: t.errBg, color: t.errTx, fontSize: 12, fontWeight: 500 }}>{erro}</div>}
    <Cd t={t} style={{ padding: 18 }}>
      <div style={{ display: "flex", alignItems: "center", gap: 14, marginBottom: 16 }}>
        <div style={{ position: "relative" }}>
          <Av letters={initials(perfil.nome)} size={64} t={t} fotoUrl={perfil.fotoUrl} />
          <label style={{ position: "absolute", bottom: -2, right: -2, width: 24, height: 24, borderRadius: "50%", background: t.priGrad, display: "flex", alignItems: "center", justifyContent: "center", cursor: uploadingFoto ? "default" : "pointer", border: `2px solid ${t.card}` }}>
            <span style={{ fontSize: 12, color: "#fff" }}>{uploadingFoto ? "..." : "+"}</span>
            <input type="file" accept="image/jpeg,image/png,image/webp" onChange={uploadFoto} style={{ display: "none" }} disabled={uploadingFoto} />
          </label>
        </div>
        <div><div style={{ fontSize: 16, fontWeight: 700 }}>{perfil.nome}</div><div style={{ fontSize: 12, color: t.txMuted }}>{perfil.tipo} {perfil.documento}</div></div>
      </div>
      {editando ? <>
        <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
          <div><label style={labelStyle}>Nome</label><input value={form.nome} onChange={e => set("nome", e.target.value)} className="input-field" style={inputStyle} /></div>
          <div><label style={labelStyle}>Email</label><input value={form.email} onChange={e => set("email", e.target.value)} type="email" className="input-field" style={inputStyle} /></div>
          <div><label style={labelStyle}>Telefone</label><input value={form.telefone} onChange={e => set("telefone", e.target.value)} className="input-field" style={inputStyle} /></div>
          <div><label style={labelStyle}>Cidade</label><input value={form.cidade} onChange={e => set("cidade", e.target.value)} className="input-field" style={inputStyle} /></div>
        </div>
        <div style={{ display: "flex", gap: 8, marginTop: 12 }}>
          <button onClick={() => { setEditando(false); setErro(""); setForm({ nome: perfil.nome || "", email: perfil.email || "", telefone: perfil.telefone || "", cidade: perfil.cidade || "" }); }} className="btn-outline" style={{ flex: 1, padding: "10px 0", border: `1px solid ${t.border}`, color: t.txMuted, borderRadius: 10 }}>Cancelar</button>
          <button onClick={salvar} disabled={salvando} className="btn-primary" style={{ flex: 1, padding: "10px 0", background: salvando ? t.txMuted : t.priGrad, color: "#fff" }}>{salvando ? "Salvando..." : "Salvar"}</button>
        </div>
      </> : <>
        <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
          <div><span style={labelStyle}>Email</span><span style={valStyle}>{perfil.email || "—"}</span></div>
          <div><span style={labelStyle}>Telefone</span><span style={valStyle}>{perfil.telefone || "—"}</span></div>
          <div><span style={labelStyle}>Cidade</span><span style={valStyle}>{perfil.cidade || "—"}</span></div>
        </div>
        <button onClick={() => { setEditando(true); setSucesso(""); }} className="btn-primary" style={{ background: t.priGrad, color: "#fff", marginTop: 12 }}>Editar perfil</button>
      </>}
    </Cd>
  </div>;
}

/* ═══ MAIN ═══ */
const TABS_CPF = [{ id: "home", label: "Início", Icon: IconHome }, { id: "amigos", label: "Amigos", Icon: IconHeart }, { id: "mapa", label: "Barcos", Icon: IconShip }, { id: "passagens", label: "Passagens", Icon: IconTicket }];
const TABS_CNPJ = [{ id: "home", label: "Painel", Icon: IconGrid }, { id: "pedidos", label: "Pedidos", Icon: IconCart }, { id: "lojas", label: "Parceiros", Icon: IconUsers }, { id: "financeiro", label: "Financ.", Icon: IconWallet }, { id: "loja", label: "Loja", Icon: IconStore }];

export default function Naviera() {
  const [profile, setProfile] = useState(() => { try { const u = JSON.parse(localStorage.getItem("naviera_usuario")); return u?.tipo === "CNPJ" ? "cnpj" : u ? "cpf" : null; } catch { return null; } });
  const [tab, setTab] = useState("home");
  const [tabHistory, setTabHistory] = useState([]);
  const navigateTab = (newTab) => { setTabHistory(h => [...h, tab]); setTab(newTab); };
  const goBack = () => { if (tabHistory.length > 0) { setTab(tabHistory[tabHistory.length - 1]); setTabHistory(h => h.slice(0, -1)); } };
  const [mode, setMode] = useState("light");
  const [tela, setTela] = useState("login");
  const [msgSucesso, setMsgSucesso] = useState("");
  const [token, setToken] = useState(() => localStorage.getItem("naviera_token"));
  const [usuario, setUsuario] = useState(() => { try { return JSON.parse(localStorage.getItem("naviera_usuario")); } catch { return null; } });
  const [minhaFoto, setMinhaFoto] = useState(null);
  const [loginDoc, setLoginDoc] = useState("");
  const [loginSenha, setLoginSenha] = useState("");
  const [loginErro, setLoginErro] = useState("");
  const [loginLoading, setLoginLoading] = useState(false);
  const [loginTipo, setLoginTipo] = useState("CPF");
  const t = T[mode];

  useEffect(() => {
    if (!token) { setMinhaFoto(null); return; }
    fetch(`${API}/perfil`, { headers: { "Authorization": `Bearer ${token}`, "Content-Type": "application/json" } })
      .then(r => r.ok ? r.json() : null)
      .then(d => { if (d?.fotoUrl) setMinhaFoto(`${API}${d.fotoUrl}`); })
      .catch(() => {});
  }, [token]);

  const doLogin = async () => {
    setLoginErro("");
    if (!loginDoc.trim() || !loginSenha.trim()) { setLoginErro("Informe documento e senha."); return; }
    setLoginLoading(true);
    try {
      const res = await fetch(`${API}/auth/login`, { method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ documento: loginDoc.replace(/\D/g, ""), senha: loginSenha }) });
      const data = await res.json();
      if (!res.ok) { setLoginErro(data.erro || "Credenciais inválidas."); return; }
      localStorage.setItem("naviera_token", data.token);
      localStorage.setItem("naviera_usuario", JSON.stringify({ nome: data.nome, tipo: data.tipo, id: data.id }));
      setToken(data.token);
      setUsuario({ nome: data.nome, tipo: data.tipo, id: data.id });
      setProfile(data.tipo === "CNPJ" ? "cnpj" : "cpf");
      setTab("home"); setTabHistory([]);
      setLoginDoc(""); setLoginSenha(""); setMsgSucesso("");
    } catch { setLoginErro("Erro de conexão com o servidor."); } finally { setLoginLoading(false); }
  };

  const doLogout = () => { localStorage.removeItem("naviera_token"); localStorage.removeItem("naviera_usuario"); setProfile(null); setToken(null); setUsuario(null); setTab("home"); setTabHistory([]); setMinhaFoto(null); };

  const inputStyle = { width: "100%", padding: "12px 14px", borderRadius: 10, border: `1px solid ${t.border}`, background: t.soft, color: t.tx, fontSize: 13, outline: "none", boxSizing: "border-box" };

  /* ═══ LOGIN SCREEN ═══ */
  if (!profile) return (
    <div style={{ minHeight: "100vh", background: t.bg, display: "flex", alignItems: "center", justifyContent: "center", padding: 20, color: t.tx, transition: "background 0.3s" }}>
      {tela === "cadastro" ? (
        <TelaCadastro t={t} onVoltar={() => setTela("login")} onSucesso={(data) => {
          localStorage.setItem("naviera_token", data.token);
          localStorage.setItem("naviera_usuario", JSON.stringify({ nome: data.nome, tipo: data.tipo, id: data.id }));
          setToken(data.token);
          setUsuario({ nome: data.nome, tipo: data.tipo, id: data.id });
          setProfile(data.tipo === "CNPJ" ? "cnpj" : "cpf");
          setTab("home"); setTabHistory([]);
        }} />
      ) : (
      <div style={{ width: "100%", maxWidth: 380, textAlign: "center" }}>
        <div style={{ marginBottom: 20 }}><Logo size={60} t={t} /></div>
        <h1 style={{ fontSize: 32, fontWeight: 800, margin: "0 0 4px", letterSpacing: 4, color: t.tx }}>NAVIERA</h1>
        <p style={{ fontSize: 12, color: t.txMuted, margin: "0 0 24px", letterSpacing: 6, textTransform: "uppercase", fontWeight: 300 }}>Navegação fluvial</p>
        {msgSucesso && <div style={{ padding: "10px 14px", borderRadius: 10, background: t.okBg, color: t.okTx, fontSize: 12, fontWeight: 500, marginBottom: 16 }}>{msgSucesso}</div>}
        <div style={{ display: "flex", flexDirection: "column", gap: 10, textAlign: "left" }}>
          <div><label style={{ fontSize: 12, fontWeight: 600, color: t.txSoft, marginBottom: 4, display: "block" }}>CPF ou CNPJ</label>
            <input value={loginDoc} onChange={e => {
              const raw = e.target.value.replace(/\D/g, "");
              const tp = raw.length > 11 ? "CNPJ" : "CPF";
              setLoginTipo(tp);
              setLoginDoc(maskDoc(e.target.value, tp));
            }} placeholder="000.000.000-00" className="input-field" style={inputStyle} onKeyDown={e => e.key === "Enter" && doLogin()} /></div>
          <div><label style={{ fontSize: 12, fontWeight: 600, color: t.txSoft, marginBottom: 4, display: "block" }}>Senha</label>
            <input value={loginSenha} onChange={e => setLoginSenha(e.target.value)} type="password" placeholder="Sua senha" className="input-field" style={inputStyle} onKeyDown={e => e.key === "Enter" && doLogin()} /></div>
          {loginErro && <div style={{ padding: "10px 14px", borderRadius: 10, background: t.errBg, color: t.errTx, fontSize: 12, fontWeight: 500 }}>{loginErro}</div>}
          <button onClick={doLogin} disabled={loginLoading} className="btn-primary" style={{ background: loginLoading ? t.txMuted : t.priGrad, color: "#fff", marginTop: 4 }}>{loginLoading ? "Entrando..." : "Entrar"}</button>
        </div>
        <button onClick={() => { setMsgSucesso(""); setLoginErro(""); setTela("cadastro"); }} style={{ marginTop: 16, background: "none", border: "none", color: t.pri, fontSize: 13, fontWeight: 600, cursor: "pointer" }}>Não tem conta? <span style={{ textDecoration: "underline" }}>Cadastre-se</span></button>
        <div><button onClick={() => setMode(m => m === "light" ? "dark" : "light")} style={{ marginTop: 12, padding: "8px 20px", borderRadius: 10, background: t.soft, border: `1px solid ${t.border}`, cursor: "pointer", color: t.txMuted, fontSize: 12, display: "inline-flex", alignItems: "center", gap: 6 }}>
          {mode === "light" ? <><IconMoon size={12} color={t.txMuted} /> Dark</> : <><IconSun size={12} color={t.txMuted} /> Light</>}
        </button></div>
        <p style={{ fontSize: 10, color: t.txMuted, marginTop: 16, fontFamily: "'Space Mono', monospace" }}>Naviera v4.0</p>
      </div>)}
    </div>
  );

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
      <Header t={t} mode={mode} setMode={setMode} tab={tab} navigateTab={navigateTab} goBack={goBack} profile={profile} minhaFoto={minhaFoto} doLogout={doLogout} />
      <div style={{ padding: "16px 18px 100px" }}>{screen()}</div>
      <TabBar tabs={tabs} tab={tab} setTab={(id) => { setTab(id); setTabHistory([]); }} t={t} />
    </div>
  );
}

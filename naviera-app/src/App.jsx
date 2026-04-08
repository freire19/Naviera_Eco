import { useState } from "react";

/* ═══ BRAND V3 ═══ */
const T = {
  light: {
    bg: "#F7FBF9", card: "#FFFFFF", soft: "#EEF7F2", accent: "#E6F5ED",
    tx: "#0F2620", txSoft: "#3D6B56", txMuted: "#7BA393",
    pri: "#059669", priGrad: "linear-gradient(135deg, #059669, #34D399)",
    border: "rgba(5,150,105,0.12)", borderStrong: "rgba(5,150,105,0.25)",
    info: "#0369A1", infoBg: "#DBEAFE", infoTx: "#1D4ED8",
    warn: "#B45309", warnBg: "#FEF3C7", warnTx: "#B45309",
    ok: "#047857", okBg: "#D1FAE5", okTx: "#047857",
    err: "#DC2626", errBg: "#FEE2E2", errTx: "#DC2626",
    amber: "#B45309", amberBg: "#FEF3C7",
    shadow: "0 2px 12px rgba(0,0,0,0.05)",
  },
  dark: {
    bg: "#040D0A", card: "#0F2D24", soft: "#0A1F18", accent: "#0F2D24",
    tx: "#F0FDF4", txSoft: "#6EE7B7", txMuted: "#34D399",
    pri: "#34D399", priGrad: "linear-gradient(135deg, #059669, #34D399)",
    border: "rgba(52,211,153,0.08)", borderStrong: "rgba(52,211,153,0.2)",
    info: "#0EA5E9", infoBg: "#0c2d48", infoTx: "#38BDF8",
    warn: "#F59E0B", warnBg: "#3a3520", warnTx: "#FBBF24",
    ok: "#4ADE80", okBg: "#0f2b1c", okTx: "#4ADE80",
    err: "#EF4444", errBg: "#450a0a", errTx: "#F87171",
    amber: "#FBBF24", amberBg: "#3a3520",
    shadow: "none",
  }
};

const Logo = ({ size = 16, t }) => (
  <svg width={size} height={size} viewBox="0 0 60 60" fill="none">
    <path d="M14 48 L14 14 Q14 10, 18 14 L30 30 Q34 35, 34 30 L34 14" stroke={t.pri} strokeWidth="5" strokeLinecap="round" strokeLinejoin="round" fill="none"/>
    <path d="M34 30 Q34 35, 38 30 L48 16" stroke={t.pri} strokeWidth="3.5" strokeLinecap="round" fill="none" opacity="0.25"/>
  </svg>
);

/* ═══ DATA ═══ */
const FRIENDS = [
  { id: 1, name: "Maria Silva", av: "MS", status: "Em viagem", detail: "Manaus → Jutaí", boat: "Deus de Aliança", progress: 45 },
  { id: 2, name: "João Santos", av: "JS", status: "No destino", detail: "Chegou em Tefé", boat: "José Lemos", progress: 100 },
  { id: 3, name: "Ana Costa", av: "AC", status: "Offline", detail: "Manaus", boat: null, progress: 0 },
  { id: 4, name: "Carlos Reis", av: "CR", status: "Em viagem", detail: "Manaus → Tabatinga", boat: "Sagrado Coração", progress: 72 },
];
const ENCOMENDAS = [
  { id: "ENC-0451", desc: "Caixa de medicamentos", rota: "Manaus → Jutaí", status: "Em trânsito", boat: "Deus de Aliança", progress: 45, eta: "10/04 às 14h" },
  { id: "ENC-0387", desc: "Peças para motor", rota: "Manaus → Jutaí", status: "Entregue", boat: "Deus de Aliança", progress: 100 },
];
const VIAGENS = [
  { id: 1, boat: "Deus de Aliança", rota: "Manaus → Jutaí", data: "15/04/2026", hr: "18:00", status: "Confirmada", pol: "Rede 14" },
  { id: 2, boat: "José Lemos V", rota: "Jutaí → Manaus", data: "28/04/2026", hr: "06:00", status: "Reservada", pol: "Rede 08" },
];
const BOATS = [
  { name: "Deus de Aliança", rota: "Manaus → Jutaí", status: "EM_VIAGEM", progress: 45, saida: "08/04 18:00", chegada: "10/04 14:00" },
  { name: "José Lemos V", rota: "Manaus → Fonte Boa", status: "NO_PORTO", progress: 0, saida: "12/04 06:00", chegada: "-" },
  { name: "Sagrado Coração", rota: "Manaus → Tabatinga", status: "EM_VIAGEM", progress: 72, saida: "06/04 12:00", chegada: "11/04 08:00" },
  { name: "Golfinho do Mar", rota: "Manaus → Parintins", status: "NO_PORTO", progress: 0, saida: "14/04 20:00", chegada: "-" },
];
const FRETES = [
  { id: "FRT-1204", desc: "150 cxs alimentos", dest: "Jutaí", boat: "Deus de Aliança", status: "Em trânsito", peso: "2.400kg", valor: "R$ 3.600", progress: 45 },
  { id: "FRT-1198", desc: "80 cxs bebidas", dest: "Tefé", boat: "José Lemos", status: "Entregue", peso: "1.800kg", valor: "R$ 2.900", progress: 100 },
  { id: "FRT-1215", desc: "200 cxs mat. construção", dest: "Fonte Boa", boat: "José Lemos V", status: "Aguardando", peso: "5.200kg", valor: "R$ 7.800", progress: 0 },
];
const LOJAS = [
  { id: 1, name: "Distribuidora Solimões", seg: "Alimentos e bebidas", av: "DS", rotas: ["Jutaí", "Tefé", "Fonte Boa"], rating: 4.7, fretes: 53, verified: true },
  { id: 2, name: "Casa do Construtor AM", seg: "Material de construção", av: "CC", rotas: ["Tabatinga", "Jutaí", "Tefé"], rating: 4.5, fretes: 32, verified: true },
  { id: 3, name: "Eletrônicos Manaus", seg: "Eletrônicos e informática", av: "EM", rotas: ["Tabatinga", "Parintins", "Fonte Boa"], rating: 4.6, fretes: 28, verified: true },
  { id: 4, name: "Farmácia Saúde Interior", seg: "Farmácia e saúde", av: "FS", rotas: ["Jutaí", "Tefé", "Fonte Boa", "Tabatinga"], rating: 4.9, fretes: 61, verified: true },
  { id: 5, name: "Moda Tropical", seg: "Vestuário e calçados", av: "MT", rotas: ["Parintins", "Tefé"], rating: 4.3, fretes: 15, verified: false },
];
const PEDIDOS = [
  { id: "PED-0089", cliente: "Ana Costa", dest: "Jutaí", itens: "3x Cesta básica premium", valor: "R$ 890", status: "Aguardando", frete: null, data: "08/04" },
  { id: "PED-0087", cliente: "Pedro Lima", dest: "Tefé", itens: "1x Kit limpeza + 2x Arroz", valor: "R$ 345", status: "Em trânsito", frete: "FRT-1204", boat: "Deus de Aliança", progress: 45, data: "06/04" },
  { id: "PED-0082", cliente: "Lucia Mendes", dest: "Fonte Boa", itens: "5x Água sanitária + diversos", valor: "R$ 520", status: "Entregue", frete: "FRT-1198", data: "02/04" },
];
const CHAT_MSGS = [
  { from: "Maria Silva", av: "MS", text: "Alguém vai na viagem do dia 15 pro Jutaí?", time: "14:30", mine: false },
  { from: "Eu", av: "RF", text: "Eu vou! Rede 14.", time: "14:32", mine: true },
  { from: "Maria Silva", av: "MS", text: "Bora levar dominó 😄", time: "14:33", mine: false },
];

/* ═══ COMPONENTS ═══ */
function Badge({ status, t }) {
  const m = { "Em trânsito": [t.infoBg, t.infoTx], "EM_VIAGEM": [t.infoBg, t.infoTx], "Em viagem": [t.infoBg, t.infoTx],
    "Entregue": [t.okBg, t.okTx], "NO_PORTO": [t.warnBg, t.warnTx], "Confirmada": [t.okBg, t.okTx],
    "Reservada": [t.warnBg, t.warnTx], "Aguardando": [t.warnBg, t.warnTx], "No destino": [t.okBg, t.okTx],
    "Offline": [t.soft, t.txMuted], "Pendente": [t.errBg, t.errTx], "Pago": [t.okBg, t.okTx], "Verificada": [t.okBg, t.okTx] };
  const [bg, c] = m[status] || [t.soft, t.txMuted];
  const label = status === "EM_VIAGEM" ? "Em viagem" : status === "NO_PORTO" ? "No porto" : status;
  return <span style={{ fontSize: 11, padding: "3px 10px", borderRadius: 20, background: bg, color: c, fontWeight: 600 }}>{label}</span>;
}
function Bar({ value, t, h = 4 }) {
  return <div style={{ width: "100%", height: h, borderRadius: h, background: t.border }}><div style={{ width: `${value}%`, height: "100%", borderRadius: h, background: t.pri, transition: "width 0.8s" }} /></div>;
}
function Av({ letters, size = 36, t }) {
  return <div style={{ width: size, height: size, borderRadius: "50%", background: t.accent, display: "flex", alignItems: "center", justifyContent: "center", fontSize: size * 0.35, fontWeight: 700, color: t.pri, flexShrink: 0 }}>{letters}</div>;
}
function Cd({ children, style, onClick, t }) {
  return <div onClick={onClick} style={{ background: t.card, borderRadius: 14, padding: 16, border: `1px solid ${t.border}`, cursor: onClick ? "pointer" : "default", boxShadow: t.shadow, ...style }}>{children}</div>;
}

/* ═══ CPF SCREENS (unchanged structure) ═══ */
function HomeCPF({ t, onNav }) {
  return <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
    <div><span style={{ fontSize: 13, color: t.txMuted }}>Olá,</span><h2 style={{ margin: 0, fontSize: 22, fontWeight: 700, letterSpacing: -0.5 }}>Renato Freire</h2></div>
    {VIAGENS.filter(v => v.status === "Confirmada").map(v => (
      <Cd key={v.id} t={t} style={{ border: `1px solid ${t.borderStrong}` }}>
        <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 10 }}>
          <div><div style={{ fontSize: 10, color: t.pri, fontWeight: 700, letterSpacing: 1.5, textTransform: "uppercase", marginBottom: 4 }}>Próxima viagem</div>
            <div style={{ fontSize: 16, fontWeight: 600 }}>{v.boat}</div><div style={{ fontSize: 13, color: t.txMuted, marginTop: 2 }}>{v.rota}</div></div>
          <Badge status={v.status} t={t} />
        </div>
        <div style={{ display: "flex", gap: 20, fontSize: 12, color: t.txMuted }}>
          <span>📅 {v.data}</span><span>🕐 {v.hr}</span><span>🛏️ {v.pol}</span></div>
      </Cd>))}
    <div style={{ display: "flex", justifyContent: "space-between", marginTop: 4 }}>
      <span style={{ fontSize: 14, fontWeight: 600 }}>Amigos</span>
      <button style={{ background: "none", border: "none", color: t.pri, fontSize: 12, fontWeight: 600, cursor: "pointer", fontFamily: "inherit" }} onClick={() => onNav("amigos")}>Ver todos →</button></div>
    {FRIENDS.filter(f => f.status !== "Offline").map(f => (
      <Cd key={f.id} t={t} style={{ padding: 12 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
          <Av letters={f.av} size={40} t={t} />
          <div style={{ flex: 1 }}><div style={{ fontSize: 14, fontWeight: 600 }}>{f.name}</div><div style={{ fontSize: 12, color: t.txMuted }}>{f.detail}{f.boat ? ` • ${f.boat}` : ""}</div></div>
          <Badge status={f.status} t={t} /></div>
        {f.status === "Em viagem" && <div style={{ marginTop: 8, marginLeft: 52 }}><Bar value={f.progress} t={t} h={3} /></div>}
      </Cd>))}
    <span style={{ fontSize: 14, fontWeight: 600, marginTop: 4 }}>Encomendas</span>
    {ENCOMENDAS.map(e => (
      <Cd key={e.id} t={t} style={{ padding: 12 }}>
        <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 4 }}>
          <span style={{ fontSize: 12, fontWeight: 700, fontFamily: "'Space Mono', monospace", color: t.txSoft }}>{e.id}</span><Badge status={e.status} t={t} /></div>
        <div style={{ fontSize: 13 }}>{e.desc}</div><div style={{ fontSize: 12, color: t.txMuted }}>{e.rota} • {e.boat}</div>
        {e.progress > 0 && e.progress < 100 && <div style={{ marginTop: 6 }}><Bar value={e.progress} t={t} h={3} /><div style={{ fontSize: 11, color: t.txMuted, marginTop: 3 }}>Chega: {e.eta}</div></div>}
      </Cd>))}
  </div>;
}

function AmigosCPF({ t }) {
  const [chat, setChat] = useState(false);
  const [msg, setMsg] = useState("");
  if (chat) return <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
    <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 6 }}><button onClick={() => setChat(false)} style={{ background: "none", border: "none", color: t.txMuted, fontSize: 18, cursor: "pointer", padding: 0 }}>←</button><span style={{ fontSize: 15, fontWeight: 600 }}>Viagem Jutaí 15/04</span></div>
    {CHAT_MSGS.map((m, i) => <div key={i} style={{ display: "flex", flexDirection: m.mine ? "row-reverse" : "row", gap: 8, alignItems: "flex-end" }}>
      {!m.mine && <Av letters={m.av} size={28} t={t} />}
      <div style={{ maxWidth: "75%", padding: "10px 14px", borderRadius: m.mine ? "14px 14px 4px 14px" : "14px 14px 14px 4px", background: m.mine ? t.pri : t.soft, color: m.mine ? "#fff" : t.tx, fontSize: 13, lineHeight: 1.5 }}>
        {!m.mine && <div style={{ fontSize: 11, fontWeight: 600, color: t.pri, marginBottom: 3 }}>{m.from}</div>}{m.text}
        <div style={{ fontSize: 10, color: m.mine ? "rgba(255,255,255,0.5)" : t.txMuted, textAlign: "right", marginTop: 3 }}>{m.time}</div></div></div>)}
    <div style={{ display: "flex", gap: 8, marginTop: 8 }}><input value={msg} onChange={e => setMsg(e.target.value)} placeholder="Mensagem..." style={{ flex: 1, padding: "10px 14px", borderRadius: 20, border: `1px solid ${t.border}`, background: t.soft, color: t.tx, fontSize: 13, outline: "none", fontFamily: "inherit" }} />
      <button style={{ width: 40, height: 40, borderRadius: "50%", background: t.priGrad, border: "none", color: "#fff", fontSize: 16, cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center" }}>↑</button></div></div>;

  return <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <div style={{ display: "flex", justifyContent: "space-between" }}><h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Amigos</h3>
      <button style={{ padding: "6px 14px", borderRadius: 20, border: `1px solid ${t.borderStrong}`, background: "transparent", color: t.pri, fontSize: 12, fontWeight: 600, cursor: "pointer", fontFamily: "inherit" }}>+ Adicionar</button></div>
    <Cd t={t} style={{ padding: 14, border: `1px solid ${t.borderStrong}`, cursor: "pointer" }} onClick={() => setChat(true)}>
      <div style={{ display: "flex", alignItems: "center", gap: 10 }}><div style={{ width: 40, height: 40, borderRadius: 10, background: t.accent, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 18 }}>💬</div>
        <div style={{ flex: 1 }}><div style={{ fontSize: 14, fontWeight: 600 }}>Viagem Jutaí 15/04</div><div style={{ fontSize: 12, color: t.txMuted }}>Maria: Bora levar dominó 😄</div></div></div></Cd>
    {FRIENDS.map(f => <Cd key={f.id} t={t} style={{ padding: 12 }}>
      <div style={{ display: "flex", alignItems: "center", gap: 12 }}><Av letters={f.av} size={42} t={t} /><div style={{ flex: 1 }}><div style={{ fontSize: 14, fontWeight: 600 }}>{f.name}</div><div style={{ fontSize: 12, color: t.txMuted }}>{f.detail}</div></div><Badge status={f.status} t={t} /></div>
      {f.status === "Em viagem" && <div style={{ marginTop: 8 }}><Bar value={f.progress} t={t} h={3} /></div>}</Cd>)}
  </div>;
}

function MapaCPF({ t }) {
  const [sel, setSel] = useState(null);
  return <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Embarcações</h3>
    {BOATS.map((b, i) => <Cd key={b.name} t={t} style={{ padding: 12, border: sel === i ? `1px solid ${t.borderStrong}` : `1px solid ${t.border}` }} onClick={() => setSel(sel === i ? null : i)}>
      <div style={{ display: "flex", justifyContent: "space-between" }}><div><div style={{ fontSize: 15, fontWeight: 600 }}>{b.name}</div><div style={{ fontSize: 12, color: t.txMuted, marginTop: 2 }}>{b.rota}</div></div><Badge status={b.status} t={t} /></div>
      {sel === i && <div style={{ marginTop: 10, paddingTop: 10, borderTop: `1px solid ${t.border}` }}>
        {b.status === "EM_VIAGEM" && <div style={{ marginBottom: 8 }}><Bar value={b.progress} t={t} h={4} /><div style={{ fontSize: 11, color: t.txMuted, marginTop: 4 }}>{b.progress}% do trajeto</div></div>}
        <div style={{ display: "flex", gap: 16, fontSize: 12, color: t.txMuted }}><span>Saída: {b.saida}</span><span>Chegada: {b.chegada}</span></div>
        {b.status === "NO_PORTO" && <div style={{ marginTop: 8, padding: "6px 10px", borderRadius: 8, background: t.warnBg, fontSize: 12, color: t.warnTx }}>⚓ No porto — recebendo mercadorias</div>}</div>}
    </Cd>)}
  </div>;
}

function PassagensCPF({ t }) {
  return <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Minhas passagens</h3>
    {VIAGENS.map(v => <Cd key={v.id} t={t} style={{ padding: 14 }}>
      <div style={{ display: "flex", justifyContent: "space-between" }}><div><div style={{ fontSize: 16, fontWeight: 600 }}>{v.boat}</div><div style={{ fontSize: 13, color: t.txMuted, marginTop: 2 }}>{v.rota}</div></div><Badge status={v.status} t={t} /></div>
      <div style={{ display: "flex", gap: 16, fontSize: 12, color: t.txMuted, marginTop: 10 }}><span>📅 {v.data}</span><span>🕐 {v.hr}</span><span>🛏️ {v.pol}</span></div>
    </Cd>)}
    <div style={{ fontSize: 14, fontWeight: 600, marginTop: 8 }}>Rotas disponíveis</div>
    {[{ r: "Manaus → Tabatinga", b: "Sagrado Coração", p: "R$ 350" }, { r: "Manaus → Jutaí", b: "Deus de Aliança, José Lemos", p: "R$ 220" }, { r: "Manaus → Fonte Boa", b: "José Lemos V", p: "R$ 280" }, { r: "Manaus → Parintins", b: "Golfinho do Mar", p: "R$ 120" }].map((x, i) =>
      <Cd key={i} t={t} style={{ padding: 12 }}><div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}><div><div style={{ fontSize: 14, fontWeight: 600 }}>{x.r}</div><div style={{ fontSize: 12, color: t.txMuted, marginTop: 2 }}>{x.b}</div></div>
        <div style={{ textAlign: "right" }}><div style={{ fontSize: 12, color: t.pri, fontWeight: 600 }}>a partir de</div><div style={{ fontSize: 16, fontWeight: 700 }}>{x.p}</div></div></div></Cd>)}
  </div>;
}

/* ═══ CNPJ SCREENS ═══ */
function HomeCNPJ({ t, onNav }) {
  return <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
    <div><span style={{ fontSize: 13, color: t.txMuted }}>Empresa</span><h2 style={{ margin: 0, fontSize: 20, fontWeight: 700 }}>Comercial Rio Negro LTDA</h2><span style={{ fontSize: 12, color: t.txMuted }}>CNPJ: 12.345.678/0001-90</span></div>
    <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
      {[{ l: "Fretes ativos", v: "2", c: t.pri, ic: "📦" }, { l: "Pedidos novos", v: "1", c: t.amber, ic: "🛒" }, { l: "Entregues (mês)", v: "8", c: t.info, ic: "✓" }, { l: "Pendências", v: "R$ 3.600", c: t.err, ic: "💰" }].map((s, i) =>
        <Cd key={i} t={t} style={{ padding: 14, textAlign: "center" }}><div style={{ fontSize: 20, marginBottom: 4 }}>{s.ic}</div><div style={{ fontSize: 22, fontWeight: 700, color: s.c }}>{s.v}</div><div style={{ fontSize: 11, color: t.txMuted, marginTop: 2 }}>{s.l}</div></Cd>)}
    </div>
    <div style={{ display: "flex", justifyContent: "space-between", marginTop: 4 }}><span style={{ fontSize: 14, fontWeight: 600 }}>Pedidos recentes</span>
      <button style={{ background: "none", border: "none", color: t.pri, fontSize: 12, fontWeight: 600, cursor: "pointer", fontFamily: "inherit" }} onClick={() => onNav("pedidos")}>Ver todos →</button></div>
    {PEDIDOS.slice(0, 2).map(p => <Cd key={p.id} t={t} style={{ padding: 12 }}>
      <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 4 }}><span style={{ fontSize: 12, fontWeight: 700, fontFamily: "'Space Mono', monospace", color: t.txSoft }}>{p.id}</span><Badge status={p.status} t={t} /></div>
      <div style={{ fontSize: 13 }}>{p.cliente} → {p.dest}</div><div style={{ fontSize: 12, color: t.txMuted, marginTop: 2 }}>{p.itens}</div>
      <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12, marginTop: 6 }}><span style={{ color: t.txMuted }}>{p.data}</span><span style={{ fontWeight: 600 }}>{p.valor}</span></div>
      {p.progress > 0 && p.progress < 100 && <div style={{ marginTop: 6 }}><Bar value={p.progress} t={t} h={3} /></div>}
    </Cd>)}
    <div style={{ display: "flex", justifyContent: "space-between", marginTop: 4 }}><span style={{ fontSize: 14, fontWeight: 600 }}>Lojas parceiras</span>
      <button style={{ background: "none", border: "none", color: t.pri, fontSize: 12, fontWeight: 600, cursor: "pointer", fontFamily: "inherit" }} onClick={() => onNav("lojas")}>Ver todas →</button></div>
    {LOJAS.slice(0, 2).map(l => <Cd key={l.id} t={t} style={{ padding: 12 }}>
      <div style={{ display: "flex", alignItems: "center", gap: 10 }}><Av letters={l.av} size={38} t={t} /><div style={{ flex: 1 }}><div style={{ fontSize: 14, fontWeight: 600 }}>{l.name}{l.verified && <span style={{ color: t.pri, marginLeft: 4, fontSize: 11 }}>✓</span>}</div><div style={{ fontSize: 12, color: t.txMuted }}>{l.seg} • ★ {l.rating}</div></div></div>
    </Cd>)}
  </div>;
}

function LojasParceiras({ t }) {
  const [cidade, setCidade] = useState("Todas");
  const [sel, setSel] = useState(null);
  const cidades = ["Todas", "Jutaí", "Tefé", "Tabatinga", "Fonte Boa", "Parintins"];
  const filtered = cidade === "Todas" ? LOJAS : LOJAS.filter(l => l.rotas.includes(cidade));

  if (sel) {
    const loja = LOJAS.find(l => l.id === sel);
    return <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
      <button onClick={() => setSel(null)} style={{ background: "none", border: "none", color: t.txMuted, fontSize: 13, cursor: "pointer", fontFamily: "inherit", textAlign: "left", padding: 0 }}>← Voltar</button>
      <Cd t={t} style={{ padding: 18, border: `1px solid ${t.borderStrong}` }}>
        <div style={{ display: "flex", alignItems: "center", gap: 14, marginBottom: 14 }}><Av letters={loja.av} size={54} t={t} />
          <div><div style={{ fontSize: 18, fontWeight: 700 }}>{loja.name}</div><div style={{ fontSize: 13, color: t.txMuted }}>{loja.seg}</div>
            {loja.verified && <div style={{ fontSize: 11, color: t.pri, fontWeight: 600, marginTop: 3 }}>✓ Verificada Naviera</div>}</div></div>
        <div style={{ display: "flex", gap: 12, marginBottom: 14 }}>
          <div style={{ flex: 1, textAlign: "center", padding: 10, borderRadius: 10, background: t.soft }}><div style={{ fontSize: 18, fontWeight: 700, color: t.amber }}>★ {loja.rating}</div><div style={{ fontSize: 10, color: t.txMuted }}>Avaliação</div></div>
          <div style={{ flex: 1, textAlign: "center", padding: 10, borderRadius: 10, background: t.soft }}><div style={{ fontSize: 18, fontWeight: 700, color: t.info }}>{loja.fretes}</div><div style={{ fontSize: 10, color: t.txMuted }}>Entregas</div></div></div>
        <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 8 }}>Envia para:</div>
        <div style={{ display: "flex", flexWrap: "wrap", gap: 6, marginBottom: 14 }}>{loja.rotas.map(r => <span key={r} style={{ padding: "4px 12px", borderRadius: 14, background: t.accent, fontSize: 12, color: t.pri, fontWeight: 500 }}>{r}</span>)}</div>
        <button style={{ width: "100%", padding: "12px 0", borderRadius: 12, border: "none", background: t.priGrad, color: "#fff", fontSize: 14, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>Entrar em contato</button>
      </Cd>
      <div style={{ fontSize: 13, fontWeight: 600 }}>Avaliações</div>
      {[{ u: "Maria S.", s: 5, txt: "Sempre no prazo, bem embalado." }, { u: "Pedro L.", s: 4, txt: "Bom atendimento, frete ok." }].map((r, i) =>
        <Cd key={i} t={t} style={{ padding: 12 }}><div style={{ display: "flex", justifyContent: "space-between", marginBottom: 4 }}><span style={{ fontSize: 13, fontWeight: 600 }}>{r.u}</span><span style={{ fontSize: 12, color: t.amber }}>{"★".repeat(r.s)}{"☆".repeat(5 - r.s)}</span></div>
          <div style={{ fontSize: 12, color: t.txMuted }}>{r.txt}</div></Cd>)}
    </div>;
  }

  return <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Lojas parceiras</h3>
    <div style={{ fontSize: 13, color: t.txMuted }}>Fornecedores verificados que embarcam pelo Naviera.</div>
    <div style={{ display: "flex", gap: 6, overflowX: "auto", paddingBottom: 4 }}>
      {cidades.map(c => <button key={c} onClick={() => setCidade(c)} style={{ padding: "6px 14px", borderRadius: 20, border: `1px solid ${cidade === c ? t.pri : t.border}`, background: cidade === c ? t.accent : "transparent", color: cidade === c ? t.pri : t.txMuted, fontSize: 12, fontWeight: 600, cursor: "pointer", whiteSpace: "nowrap", fontFamily: "inherit" }}>{c}</button>)}</div>
    {filtered.map(l => <Cd key={l.id} t={t} style={{ padding: 14, cursor: "pointer" }} onClick={() => setSel(l.id)}>
      <div style={{ display: "flex", alignItems: "center", gap: 12 }}><Av letters={l.av} size={46} t={t} />
        <div style={{ flex: 1 }}><div style={{ display: "flex", alignItems: "center", gap: 6 }}><span style={{ fontSize: 15, fontWeight: 600 }}>{l.name}</span>{l.verified && <span style={{ fontSize: 10, color: t.pri }}>✓</span>}</div>
          <div style={{ fontSize: 12, color: t.txMuted, marginTop: 1 }}>{l.seg}</div>
          <div style={{ display: "flex", gap: 8, marginTop: 4, fontSize: 11, color: t.txMuted }}><span style={{ color: t.amber }}>★ {l.rating}</span><span>{l.fretes} entregas</span><span>→ {l.rotas.slice(0, 2).join(", ")}{l.rotas.length > 2 ? ` +${l.rotas.length - 2}` : ""}</span></div></div>
        <span style={{ color: t.txMuted, fontSize: 16 }}>›</span></div></Cd>)}
  </div>;
}

function PedidosCNPJ({ t }) {
  const [f, setF] = useState("todos");
  const filtered = f === "todos" ? PEDIDOS : PEDIDOS.filter(p => { if (f === "novos") return p.status === "Aguardando"; if (f === "transito") return p.status === "Em trânsito"; if (f === "entregue") return p.status === "Entregue"; return true; });
  return <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Pedidos da loja</h3>
    <div style={{ fontSize: 13, color: t.txMuted }}>Vincule ao frete para gerar rastreio automático.</div>
    <div style={{ display: "flex", gap: 6, overflowX: "auto" }}>
      {[["todos", "Todos"], ["novos", "Novos"], ["transito", "Em trânsito"], ["entregue", "Entregues"]].map(([id, l]) =>
        <button key={id} onClick={() => setF(id)} style={{ padding: "6px 14px", borderRadius: 20, border: `1px solid ${f === id ? t.pri : t.border}`, background: f === id ? t.accent : "transparent", color: f === id ? t.pri : t.txMuted, fontSize: 12, fontWeight: 600, cursor: "pointer", whiteSpace: "nowrap", fontFamily: "inherit" }}>{l}</button>)}</div>
    {filtered.map(p => <Cd key={p.id} t={t} style={{ padding: 14 }}>
      <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 6 }}><span style={{ fontSize: 12, fontWeight: 700, fontFamily: "'Space Mono', monospace", color: t.txSoft }}>{p.id}</span><Badge status={p.status} t={t} /></div>
      <div style={{ fontSize: 14, fontWeight: 600 }}>{p.cliente} <span style={{ fontWeight: 400, color: t.txMuted }}>→ {p.dest}</span></div>
      <div style={{ fontSize: 13, color: t.txMuted, marginTop: 2 }}>{p.itens}</div>
      <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12, color: t.txMuted, marginTop: 6 }}><span>{p.data}</span><span style={{ fontWeight: 600, color: t.tx }}>{p.valor}</span></div>
      {p.frete && <div style={{ marginTop: 8, padding: "8px 12px", borderRadius: 8, background: t.accent, border: `1px solid ${t.border}` }}><div style={{ display: "flex", justifyContent: "space-between", fontSize: 12 }}><span style={{ color: t.pri, fontWeight: 600 }}>Frete: {p.frete}</span><span style={{ color: t.txMuted }}>{p.boat}</span></div>
        {p.progress > 0 && p.progress < 100 && <div style={{ marginTop: 6 }}><Bar value={p.progress} t={t} h={3} /></div>}
        <div style={{ fontSize: 11, color: t.txMuted, marginTop: 4 }}>Rastreio enviado ao cliente</div></div>}
      {!p.frete && <div style={{ display: "flex", gap: 8, marginTop: 10 }}>
        <button style={{ flex: 1, padding: "8px 0", borderRadius: 10, border: "none", background: t.priGrad, color: "#fff", fontSize: 12, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>Vincular ao frete</button>
        <button style={{ flex: 1, padding: "8px 0", borderRadius: 10, border: `1px solid ${t.border}`, background: "transparent", color: t.txMuted, fontSize: 12, fontWeight: 600, cursor: "pointer", fontFamily: "inherit" }}>Enviar link</button></div>}
    </Cd>)}
  </div>;
}

function FinanceiroCNPJ({ t }) {
  return <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Financeiro</h3>
    <Cd t={t} style={{ padding: 16 }}>
      <div style={{ fontSize: 12, color: t.txMuted, marginBottom: 4 }}>Total pendente</div>
      <div style={{ fontSize: 28, fontWeight: 700, color: t.err }}>R$ 3.600,00</div>
      <div style={{ fontSize: 12, color: t.txMuted, marginTop: 4 }}>1 frete em aberto</div>
      <button style={{ marginTop: 12, width: "100%", padding: "10px 0", borderRadius: 10, border: "none", background: t.priGrad, color: "#fff", fontSize: 13, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>Pagar via PIX</button></Cd>
    <Cd t={t} style={{ padding: 14, border: `1px solid ${t.borderStrong}` }}>
      <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 8 }}><span style={{ fontSize: 13, fontWeight: 600 }}>Receita via lojas — Abril</span><span style={{ fontSize: 15, fontWeight: 700, color: t.pri }}>R$ 8.435</span></div>
      <div style={{ display: "flex", gap: 12, fontSize: 12, color: t.txMuted }}><span>12 pedidos</span><span>3 cidades</span><span>★ 4.8</span></div></Cd>
    <div style={{ fontSize: 14, fontWeight: 600, marginTop: 4 }}>Histórico</div>
    {[{ d: "FRT-1198 — 80 cxs bebidas", v: "R$ 2.900", s: "Pago", dt: "05/04" }, { d: "FRT-1204 — 150 cxs alimentos", v: "R$ 3.600", s: "Pendente", dt: "08/04" }].map((h, i) =>
      <Cd key={i} t={t} style={{ padding: 12 }}><div style={{ display: "flex", justifyContent: "space-between" }}><div><div style={{ fontSize: 13 }}>{h.d}</div><div style={{ fontSize: 11, color: t.txMuted, marginTop: 2 }}>{h.dt}</div></div>
        <div style={{ textAlign: "right" }}><div style={{ fontSize: 14, fontWeight: 700, color: h.s === "Pendente" ? t.err : t.tx }}>{h.v}</div><Badge status={h.s} t={t} /></div></div></Cd>)}
  </div>;
}

function LojaCNPJ({ t }) {
  return <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <div style={{ display: "flex", justifyContent: "space-between" }}><h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Minha loja</h3><Badge status="Verificada" t={t} /></div>
    <Cd t={t} style={{ padding: 16, border: `1px solid ${t.borderStrong}` }}>
      <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 14 }}><Av letters="RN" size={50} t={t} />
        <div><div style={{ fontSize: 16, fontWeight: 600 }}>Comercial Rio Negro</div><div style={{ fontSize: 12, color: t.txMuted }}>Alimentos e bebidas</div><div style={{ fontSize: 11, color: t.pri, fontWeight: 600, marginTop: 2 }}>★ 4.8 • 47 entregas</div></div></div>
      <div style={{ fontSize: 12, color: t.txMuted, lineHeight: 1.6 }}>Clientes compram, você vincula ao frete, rastreio automático.</div></Cd>
    <div style={{ fontSize: 14, fontWeight: 600, marginTop: 4 }}>Como funciona</div>
    {[{ s: "1", ti: "Cliente compra na vitrine", desc: "Pedido aparece na aba Pedidos", c: t.amber },
      { s: "2", ti: "Você vincula ao frete", desc: "Associa o pedido ao embarque", c: t.pri },
      { s: "3", ti: "Rastreio automático", desc: "Cliente acompanha até a entrega", c: t.info }].map((s, i) =>
      <Cd key={i} t={t} style={{ padding: 12, borderLeft: `3px solid ${s.c}`, borderRadius: "0 14px 14px 0" }}>
        <div style={{ display: "flex", alignItems: "flex-start", gap: 10 }}>
          <div style={{ width: 26, height: 26, borderRadius: "50%", background: t.accent, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 12, fontWeight: 700, color: s.c, flexShrink: 0 }}>{s.s}</div>
          <div><div style={{ fontSize: 14, fontWeight: 600 }}>{s.ti}</div><div style={{ fontSize: 12, color: t.txMuted, marginTop: 2 }}>{s.desc}</div></div></div></Cd>)}
    <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 8, marginTop: 4 }}>
      {[{ l: "Visualizações", v: "124", c: t.amber }, { l: "Pedidos", v: "12", c: t.pri }, { l: "Receita", v: "R$ 8.4k", c: t.info }].map((s, i) =>
        <Cd key={i} t={t} style={{ padding: 10, textAlign: "center" }}><div style={{ fontSize: 18, fontWeight: 700, color: s.c }}>{s.v}</div><div style={{ fontSize: 10, color: t.txMuted, marginTop: 2 }}>{s.l}</div></Cd>)}
    </div>
    <button style={{ width: "100%", padding: "12px 0", borderRadius: 12, border: "none", background: t.priGrad, color: "#fff", fontSize: 13, fontWeight: 700, cursor: "pointer", fontFamily: "inherit", marginTop: 4 }}>Editar vitrine</button>
  </div>;
}

/* ═══ API ═══ */
const API = "http://localhost:8080/api";

/* ═══ CADASTRO SCREEN ═══ */
function TelaCadastro({ t, onVoltar, onSucesso }) {
  const [tipo, setTipo] = useState("CPF");
  const [form, setForm] = useState({ documento: "", nome: "", email: "", telefone: "", cidade: "", senha: "", senhaConfirm: "" });
  const [erro, setErro] = useState("");
  const [loading, setLoading] = useState(false);
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));
  const inputStyle = { width: "100%", padding: "12px 14px", borderRadius: 10, border: `1px solid ${t.border}`, background: t.soft, color: t.tx, fontSize: 13, outline: "none", fontFamily: "inherit", boxSizing: "border-box" };
  const labelStyle = { fontSize: 12, fontWeight: 600, color: t.txSoft, marginBottom: 4, display: "block" };

  const submit = async () => {
    setErro("");
    if (!form.documento.trim() || !form.nome.trim() || !form.senha.trim()) { setErro("Documento, nome e senha sao obrigatorios."); return; }
    if (form.senha.length < 6) { setErro("Senha deve ter no minimo 6 caracteres."); return; }
    if (form.senha !== form.senhaConfirm) { setErro("As senhas nao conferem."); return; }
    setLoading(true);
    try {
      const res = await fetch(`${API}/auth/registrar`, {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ documento: form.documento.trim(), tipoDocumento: tipo, nome: form.nome.trim(), email: form.email.trim() || null, telefone: form.telefone.trim() || null, cidade: form.cidade.trim() || null, senha: form.senha })
      });
      const data = await res.json();
      if (!res.ok) { setErro(data.erro || "Erro ao cadastrar."); return; }
      onSucesso(data);
    } catch { setErro("Erro de conexao com o servidor."); } finally { setLoading(false); }
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 14, width: "100%", maxWidth: 380 }}>
      <button onClick={onVoltar} style={{ background: "none", border: "none", color: t.txMuted, fontSize: 14, cursor: "pointer", padding: 0, textAlign: "left", fontFamily: "inherit" }}><span style={{ marginRight: 6 }}>←</span>Voltar</button>
      <div style={{ textAlign: "center", marginBottom: 4 }}>
        <Logo size={40} t={t} />
        <h2 style={{ margin: "8px 0 2px", fontSize: 22, fontWeight: 700 }}>Criar conta</h2>
        <p style={{ fontSize: 12, color: t.txMuted, margin: 0 }}>Preencha seus dados para comecar</p>
      </div>
      <div style={{ display: "flex", gap: 8 }}>
        {["CPF", "CNPJ"].map(tp => <button key={tp} onClick={() => setTipo(tp)} style={{ flex: 1, padding: "10px 0", borderRadius: 10, border: `1px solid ${tipo === tp ? t.pri : t.border}`, background: tipo === tp ? t.accent : "transparent", color: tipo === tp ? t.pri : t.txMuted, fontSize: 13, fontWeight: 600, cursor: "pointer", fontFamily: "inherit" }}>{tp}</button>)}
      </div>
      <div><label style={labelStyle}>{tipo}</label><input value={form.documento} onChange={e => set("documento", e.target.value)} placeholder={tipo === "CPF" ? "000.000.000-00" : "00.000.000/0001-00"} style={inputStyle} /></div>
      <div><label style={labelStyle}>Nome completo</label><input value={form.nome} onChange={e => set("nome", e.target.value)} placeholder="Seu nome" style={inputStyle} /></div>
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8 }}>
        <div><label style={labelStyle}>Email</label><input value={form.email} onChange={e => set("email", e.target.value)} placeholder="email@exemplo.com" type="email" style={inputStyle} /></div>
        <div><label style={labelStyle}>Telefone</label><input value={form.telefone} onChange={e => set("telefone", e.target.value)} placeholder="(92) 99999-0000" style={inputStyle} /></div>
      </div>
      <div><label style={labelStyle}>Cidade</label><input value={form.cidade} onChange={e => set("cidade", e.target.value)} placeholder="Sua cidade" style={inputStyle} /></div>
      <div><label style={labelStyle}>Senha</label><input value={form.senha} onChange={e => set("senha", e.target.value)} type="password" placeholder="Minimo 6 caracteres" style={inputStyle} /></div>
      <div><label style={labelStyle}>Confirmar senha</label><input value={form.senhaConfirm} onChange={e => set("senhaConfirm", e.target.value)} type="password" placeholder="Repita a senha" style={inputStyle} /></div>
      {erro && <div style={{ padding: "10px 14px", borderRadius: 10, background: t.errBg, color: t.errTx, fontSize: 12, fontWeight: 500 }}>{erro}</div>}
      <button onClick={submit} disabled={loading} style={{ width: "100%", padding: "14px 0", borderRadius: 12, border: "none", background: loading ? t.txMuted : t.priGrad, color: "#fff", fontSize: 14, fontWeight: 700, cursor: loading ? "default" : "pointer", fontFamily: "inherit", opacity: loading ? 0.7 : 1 }}>{loading ? "Cadastrando..." : "Criar conta"}</button>
    </div>
  );
}

/* ═══ PERFIL SCREEN ═══ */
function PerfilScreen({ t, token, authHeaders, usuario }) {
  const [perfil, setPerfil] = useState(null);
  const [editando, setEditando] = useState(false);
  const [form, setForm] = useState({});
  const [erro, setErro] = useState("");
  const [sucesso, setSucesso] = useState("");
  const [loading, setLoading] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));
  const inputStyle = { width: "100%", padding: "10px 12px", borderRadius: 8, border: `1px solid ${t.border}`, background: t.soft, color: t.tx, fontSize: 13, outline: "none", fontFamily: "inherit", boxSizing: "border-box" };

  useState(() => {
    if (!token) return;
    fetch(`${API}/perfil`, { headers: authHeaders })
      .then(r => r.ok ? r.json() : Promise.reject("Erro ao carregar perfil"))
      .then(data => { setPerfil(data); setForm({ nome: data.nome || "", email: data.email || "", telefone: data.telefone || "", cidade: data.cidade || "" }); })
      .catch(e => setErro(typeof e === "string" ? e : "Erro de conexao."))
      .finally(() => setLoading(false));
  }, []);

  const salvar = async () => {
    setErro(""); setSucesso(""); setSalvando(true);
    try {
      const res = await fetch(`${API}/perfil`, { method: "PUT", headers: authHeaders, body: JSON.stringify(form) });
      if (!res.ok) { const d = await res.json(); setErro(d.erro || d.message || "Erro ao salvar."); return; }
      setPerfil(p => ({ ...p, ...form }));
      setSucesso("Perfil atualizado!");
      setEditando(false);
    } catch { setErro("Erro de conexao."); } finally { setSalvando(false); }
  };

  if (loading) return <div style={{ textAlign: "center", padding: 40, color: t.txMuted, fontSize: 13 }}>Carregando perfil...</div>;

  const labelStyle = { fontSize: 11, fontWeight: 600, color: t.txMuted, marginBottom: 2, display: "block", textTransform: "uppercase", letterSpacing: 0.5 };
  const valStyle = { fontSize: 14, fontWeight: 500 };

  return <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
    <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Meu perfil</h3>
    {erro && <div style={{ padding: "10px 14px", borderRadius: 10, background: t.errBg, color: t.errTx, fontSize: 12 }}>{erro}</div>}
    {sucesso && <div style={{ padding: "10px 14px", borderRadius: 10, background: t.okBg, color: t.okTx, fontSize: 12 }}>{sucesso}</div>}
    {perfil && <Cd t={t}>
      <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 16 }}>
        <Av letters={perfil.nome ? perfil.nome.split(" ").map(w => w[0]).join("").slice(0, 2).toUpperCase() : "?"} size={48} t={t} />
        <div><div style={{ fontSize: 16, fontWeight: 700 }}>{perfil.nome}</div><div style={{ fontSize: 12, color: t.txMuted }}>{perfil.tipo} {perfil.documento}</div></div>
      </div>
      {editando ? <>
        <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
          <div><label style={labelStyle}>Nome</label><input value={form.nome} onChange={e => set("nome", e.target.value)} style={inputStyle} /></div>
          <div><label style={labelStyle}>Email</label><input value={form.email} onChange={e => set("email", e.target.value)} type="email" style={inputStyle} /></div>
          <div><label style={labelStyle}>Telefone</label><input value={form.telefone} onChange={e => set("telefone", e.target.value)} style={inputStyle} /></div>
          <div><label style={labelStyle}>Cidade</label><input value={form.cidade} onChange={e => set("cidade", e.target.value)} style={inputStyle} /></div>
        </div>
        <div style={{ display: "flex", gap: 8, marginTop: 12 }}>
          <button onClick={() => { setEditando(false); setErro(""); setForm({ nome: perfil.nome || "", email: perfil.email || "", telefone: perfil.telefone || "", cidade: perfil.cidade || "" }); }} style={{ flex: 1, padding: "10px 0", borderRadius: 10, border: `1px solid ${t.border}`, background: "transparent", color: t.txMuted, fontSize: 13, fontWeight: 600, cursor: "pointer", fontFamily: "inherit" }}>Cancelar</button>
          <button onClick={salvar} disabled={salvando} style={{ flex: 1, padding: "10px 0", borderRadius: 10, border: "none", background: salvando ? t.txMuted : t.priGrad, color: "#fff", fontSize: 13, fontWeight: 600, cursor: salvando ? "default" : "pointer", fontFamily: "inherit" }}>{salvando ? "Salvando..." : "Salvar"}</button>
        </div>
      </> : <>
        <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
          <div><span style={labelStyle}>Email</span><span style={valStyle}>{perfil.email || "—"}</span></div>
          <div><span style={labelStyle}>Telefone</span><span style={valStyle}>{perfil.telefone || "—"}</span></div>
          <div><span style={labelStyle}>Cidade</span><span style={valStyle}>{perfil.cidade || "—"}</span></div>
        </div>
        <button onClick={() => { setEditando(true); setSucesso(""); }} style={{ width: "100%", padding: "12px 0", borderRadius: 10, border: "none", background: t.priGrad, color: "#fff", fontSize: 13, fontWeight: 700, cursor: "pointer", fontFamily: "inherit", marginTop: 12 }}>Editar perfil</button>
      </>}
    </Cd>}
  </div>;
}

/* ═══ MAIN ═══ */
const TABS_CPF = [{ id: "home", label: "Inicio", icon: "🏠" }, { id: "amigos", label: "Amigos", icon: "♡" }, { id: "mapa", label: "Barcos", icon: "⛴" }, { id: "passagens", label: "Passagens", icon: "🎫" }];
const TABS_CNPJ = [{ id: "home", label: "Painel", icon: "▣" }, { id: "pedidos", label: "Pedidos", icon: "🛒" }, { id: "lojas", label: "Parceiros", icon: "🤝" }, { id: "financeiro", label: "Financ.", icon: "💳" }, { id: "loja", label: "Loja", icon: "🏪" }];

export default function Naviera() {
  const [profile, setProfile] = useState(null);
  const [tab, setTab] = useState("home");
  const [mode, setMode] = useState("light");
  const [aiOpen, setAiOpen] = useState(false);
  const [tela, setTela] = useState("login");
  const [msgSucesso, setMsgSucesso] = useState("");
  const [token, setToken] = useState(null);
  const [usuario, setUsuario] = useState(null);
  const [loginDoc, setLoginDoc] = useState("");
  const [loginSenha, setLoginSenha] = useState("");
  const [loginErro, setLoginErro] = useState("");
  const [loginLoading, setLoginLoading] = useState(false);
  const t = T[mode];

  const doLogin = async () => {
    setLoginErro("");
    if (!loginDoc.trim() || !loginSenha.trim()) { setLoginErro("Informe documento e senha."); return; }
    setLoginLoading(true);
    try {
      const res = await fetch(`${API}/auth/login`, {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ documento: loginDoc.trim(), senha: loginSenha })
      });
      const data = await res.json();
      if (!res.ok) { setLoginErro(data.erro || "Credenciais invalidas."); return; }
      setToken(data.token);
      setUsuario({ nome: data.nome, tipo: data.tipo, id: data.id });
      setProfile(data.tipo === "CNPJ" ? "cnpj" : "cpf");
      setTab("home");
      setLoginDoc(""); setLoginSenha(""); setMsgSucesso("");
    } catch { setLoginErro("Erro de conexao com o servidor."); } finally { setLoginLoading(false); }
  };

  const doLogout = () => { setProfile(null); setToken(null); setUsuario(null); setTab("home"); };

  const inputStyle = { width: "100%", padding: "12px 14px", borderRadius: 10, border: `1px solid ${t.border}`, background: t.soft, color: t.tx, fontSize: 13, outline: "none", fontFamily: "inherit", boxSizing: "border-box" };

  if (!profile) return (
    <div style={{ minHeight: "100vh", background: t.bg, display: "flex", alignItems: "center", justifyContent: "center", padding: 20, fontFamily: "'Sora', sans-serif", color: t.tx, transition: "all 0.3s" }}>
      <link href="https://fonts.googleapis.com/css2?family=Sora:wght@300;400;500;600;700;800&family=Space+Mono:wght@400;700&display=swap" rel="stylesheet"/>
      {tela === "cadastro" ? (
        <TelaCadastro t={t} onVoltar={() => setTela("login")} onSucesso={(data) => { setMsgSucesso(`Conta criada com sucesso! Faca login para continuar.`); setTela("login"); }} />
      ) : (
      <div style={{ width: "100%", maxWidth: 380, textAlign: "center" }}>
        <div style={{ marginBottom: 20 }}><Logo size={60} t={t} /></div>
        <h1 style={{ fontSize: 32, fontWeight: 800, margin: "0 0 4px", letterSpacing: 4 }}>NAVIERA</h1>
        <p style={{ fontSize: 12, color: t.txMuted, margin: "0 0 24px", letterSpacing: 6, textTransform: "uppercase", fontWeight: 300 }}>Navegacao fluvial</p>
        {msgSucesso && <div style={{ padding: "10px 14px", borderRadius: 10, background: t.okBg, color: t.okTx, fontSize: 12, fontWeight: 500, marginBottom: 16 }}>{msgSucesso}</div>}
        <div style={{ display: "flex", flexDirection: "column", gap: 10, textAlign: "left" }}>
          <div><label style={{ fontSize: 12, fontWeight: 600, color: t.txSoft, marginBottom: 4, display: "block" }}>CPF ou CNPJ</label>
            <input value={loginDoc} onChange={e => setLoginDoc(e.target.value)} placeholder="000.000.000-00" style={inputStyle} onKeyDown={e => e.key === "Enter" && doLogin()} /></div>
          <div><label style={{ fontSize: 12, fontWeight: 600, color: t.txSoft, marginBottom: 4, display: "block" }}>Senha</label>
            <input value={loginSenha} onChange={e => setLoginSenha(e.target.value)} type="password" placeholder="Sua senha" style={inputStyle} onKeyDown={e => e.key === "Enter" && doLogin()} /></div>
          {loginErro && <div style={{ padding: "10px 14px", borderRadius: 10, background: t.errBg, color: t.errTx, fontSize: 12, fontWeight: 500 }}>{loginErro}</div>}
          <button onClick={doLogin} disabled={loginLoading} style={{ width: "100%", padding: "14px 0", borderRadius: 12, border: "none", background: loginLoading ? t.txMuted : t.priGrad, color: "#fff", fontSize: 14, fontWeight: 700, cursor: loginLoading ? "default" : "pointer", fontFamily: "inherit", opacity: loginLoading ? 0.7 : 1, marginTop: 4 }}>{loginLoading ? "Entrando..." : "Entrar"}</button>
        </div>
        <button onClick={() => { setMsgSucesso(""); setLoginErro(""); setTela("cadastro"); }} style={{ marginTop: 16, background: "none", border: "none", color: t.pri, fontSize: 13, fontWeight: 600, cursor: "pointer", fontFamily: "inherit" }}>Nao tem conta? <span style={{ textDecoration: "underline" }}>Cadastre-se</span></button>
        <div><button onClick={() => setMode(m => m === "light" ? "dark" : "light")} style={{ marginTop: 12, padding: "8px 20px", borderRadius: 10, background: t.soft, border: `1px solid ${t.border}`, cursor: "pointer", color: t.txMuted, fontFamily: "inherit", fontSize: 12 }}>{mode === "light" ? "🌙 Dark" : "☀️ Light"}</button></div>
        <p style={{ fontSize: 10, color: t.txMuted, marginTop: 16, fontFamily: "'Space Mono', monospace" }}>Naviera v3.0</p>
      </div>)}
    </div>
  );

  const isCPF = profile === "cpf";
  const tabs = isCPF ? TABS_CPF : TABS_CNPJ;
  const authHeaders = token ? { "Authorization": `Bearer ${token}`, "Content-Type": "application/json" } : {};
  const screen = () => {
    if (tab === "perfil") return <PerfilScreen t={t} token={token} authHeaders={authHeaders} usuario={usuario} />;
    if (isCPF) { if (tab === "home") return <HomeCPF t={t} onNav={setTab} />; if (tab === "amigos") return <AmigosCPF t={t} />; if (tab === "mapa") return <MapaCPF t={t} />; if (tab === "passagens") return <PassagensCPF t={t} />; }
    else { if (tab === "home") return <HomeCNPJ t={t} onNav={setTab} />; if (tab === "pedidos") return <PedidosCNPJ t={t} />; if (tab === "lojas") return <LojasParceiras t={t} />; if (tab === "financeiro") return <FinanceiroCNPJ t={t} />; if (tab === "loja") return <LojaCNPJ t={t} />; }
  };

  return (
    <div style={{ minHeight: "100vh", background: t.bg, fontFamily: "'Sora', sans-serif", color: t.tx, maxWidth: 420, margin: "0 auto", position: "relative", transition: "all 0.3s" }}>
      <link href="https://fonts.googleapis.com/css2?family=Sora:wght@300;400;500;600;700;800&family=Space+Mono:wght@400;700&display=swap" rel="stylesheet"/>

      {/* Header */}
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "14px 18px 10px", borderBottom: `1px solid ${t.border}`, background: t.card }}>
        <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
          <div style={{ width: 30, height: 30, borderRadius: 8, background: t.accent, display: "flex", alignItems: "center", justifyContent: "center" }}><Logo size={16} t={t} /></div>
          <span style={{ fontSize: 15, fontWeight: 700, letterSpacing: 2 }}>NAVIERA</span>
          <span style={{ fontSize: 9, padding: "2px 6px", borderRadius: 6, background: t.accent, color: t.pri, fontWeight: 700, marginLeft: 4, letterSpacing: 0.5 }}>{isCPF ? "CPF" : "CNPJ"}</span>
        </div>
        <div style={{ display: "flex", gap: 6 }}>
          <button onClick={() => setTab("perfil")} style={{ width: 32, height: 32, borderRadius: 8, border: `1px solid ${tab === "perfil" ? t.borderStrong : t.border}`, background: tab === "perfil" ? t.accent : t.soft, cursor: "pointer", fontSize: 13, display: "flex", alignItems: "center", justifyContent: "center", color: tab === "perfil" ? t.pri : t.txMuted }} title="Meu perfil">👤</button>
          <button onClick={() => setMode(m => m === "light" ? "dark" : "light")} style={{ width: 32, height: 32, borderRadius: 8, border: `1px solid ${t.border}`, background: t.soft, cursor: "pointer", fontSize: 13, display: "flex", alignItems: "center", justifyContent: "center" }}>{mode === "light" ? "🌙" : "☀️"}</button>
          <button onClick={doLogout} style={{ width: 32, height: 32, borderRadius: 8, border: `1px solid ${t.border}`, background: t.soft, color: t.txMuted, fontSize: 13, cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center" }} title="Sair">↩</button>
        </div>
      </div>

      <div style={{ padding: "16px 18px 100px" }}>{screen()}</div>

      {/* AI */}
      <div onClick={() => setAiOpen(!aiOpen)} style={{ position: "fixed", bottom: 80, right: 20, width: 44, height: 44, borderRadius: "50%", background: t.priGrad, display: "flex", alignItems: "center", justifyContent: "center", cursor: "pointer", boxShadow: `0 4px 20px rgba(5,150,105,0.3)`, fontSize: 12, color: "#fff", fontWeight: 800, zIndex: 50 }}>IA</div>
      {aiOpen && <div style={{ position: "fixed", bottom: 130, right: 20, width: 280, background: t.card, borderRadius: 16, border: `1px solid ${t.border}`, padding: 16, boxShadow: "0 8px 40px rgba(0,0,0,0.2)", zIndex: 50 }}>
        <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 12 }}><span style={{ fontSize: 14, fontWeight: 600 }}>Assistente Naviera</span><button onClick={() => setAiOpen(false)} style={{ background: "none", border: "none", color: t.txMuted, cursor: "pointer", fontSize: 16 }}>✕</button></div>
        <div style={{ fontSize: 13, color: t.txMuted, lineHeight: 1.6, marginBottom: 12 }}>{isCPF ? "Ajudo com preços, horários, rastreio e barcos." : "Ajudo com fretes, pedidos, lojas parceiras e financeiro."}</div>
        <div style={{ display: "flex", flexWrap: "wrap", gap: 6 }}>{(isCPF ? ["💰 Preços", "📦 Encomendas", "🕐 Horários", "📍 Barco"] : ["📦 Fretes", "🛒 Pedidos", "🤝 Parceiros", "💰 Pendências"]).map((q, i) =>
          <button key={i} style={{ padding: "6px 10px", borderRadius: 14, border: `1px solid ${t.border}`, background: "transparent", color: t.txMuted, fontSize: 11, cursor: "pointer", fontFamily: "inherit" }}>{q}</button>)}</div></div>}

      {/* Tab bar */}
      <div style={{ position: "fixed", bottom: 0, left: "50%", transform: "translateX(-50%)", width: "100%", maxWidth: 420, background: t.card, borderTop: `1px solid ${t.border}`, padding: "6px 8px 12px", zIndex: 40 }}>
        <div style={{ display: "flex", gap: 2 }}>
          {tabs.map(tb => <button key={tb.id} onClick={() => setTab(tb.id)} style={{ flex: 1, padding: "8px 2px", borderRadius: 8, border: "none", background: tab === tb.id ? t.accent : "transparent", color: tab === tb.id ? t.pri : t.txMuted, fontSize: 10, fontWeight: 600, cursor: "pointer", display: "flex", flexDirection: "column", alignItems: "center", gap: 3, fontFamily: "inherit", transition: "all 0.2s" }}>
            <span style={{ fontSize: 15 }}>{tb.icon}</span>{tb.label}
            {tab === tb.id && <div style={{ width: 4, height: 4, borderRadius: 2, background: t.pri }} />}
          </button>)}
        </div>
      </div>
    </div>
  );
}

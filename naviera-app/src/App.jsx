import { useState, useEffect } from "react";

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

/* ═══ HELPERS ═══ */
const fmt = (d) => d ? new Date(d + "T00:00:00").toLocaleDateString("pt-BR") : "—";
const money = (v) => v != null ? `R$ ${Number(v).toLocaleString("pt-BR", { minimumFractionDigits: 2 })}` : "—";
const initials = (name) => name ? name.split(" ").map(w => w[0]).join("").slice(0, 2).toUpperCase() : "?";
const validarDocumento = (doc, tipo) => {
  const nums = doc.replace(/\D/g, "");
  if (tipo === "CPF" && nums.length !== 11) return "CPF deve ter 11 digitos.";
  if (tipo === "CNPJ" && nums.length !== 14) return "CNPJ deve ter 14 digitos.";
  return null;
};

/* ═══ HOOK: fetch com auth ═══ */
function useApi(path, authHeaders, deps = []) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState("");
  useEffect(() => {
    if (!authHeaders?.Authorization) return;
    setLoading(true); setErro("");
    fetch(`${API}${path}`, { headers: authHeaders })
      .then(r => {
        if (r.status === 401 || r.status === 403) {
          localStorage.removeItem("naviera_token"); localStorage.removeItem("naviera_usuario");
          window.location.reload();
          return Promise.reject("Sessao expirada");
        }
        return r.ok ? r.json() : Promise.reject("Erro ao carregar");
      })
      .then(d => setData(d))
      .catch((e) => setErro(typeof e === "string" ? e : "Erro ao carregar dados."))
      .finally(() => setLoading(false));
  }, [path, authHeaders?.Authorization, ...deps]);
  return { data, loading, erro };
}

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
function Av({ letters, size = 36, t, fotoUrl }) {
  const src = fotoUrl ? `${API}${fotoUrl}` : null;
  if (src) return <img src={src} alt="" style={{ width: size, height: size, borderRadius: "50%", objectFit: "cover", flexShrink: 0, border: `1px solid ${t.border}` }} />;
  return <div style={{ width: size, height: size, borderRadius: "50%", background: t.accent, display: "flex", alignItems: "center", justifyContent: "center", fontSize: size * 0.35, fontWeight: 700, color: t.pri, flexShrink: 0 }}>{letters}</div>;
}
function Cd({ children, style, onClick, t }) {
  return <div onClick={onClick} style={{ background: t.card, borderRadius: 14, padding: 16, border: `1px solid ${t.border}`, cursor: onClick ? "pointer" : "default", boxShadow: t.shadow, ...style }}>{children}</div>;
}

/* ═══ CPF SCREENS ═══ */
function HomeCPF({ t, onNav, authHeaders, usuario }) {
  const { data: viagens, loading: lv } = useApi("/viagens/ativas", authHeaders);
  const { data: encomendas, loading: le } = useApi("/encomendas", authHeaders);
  const { data: amigos } = useApi("/amigos", authHeaders);
  const proxima = viagens?.find(v => v.atual) || viagens?.[0];
  return <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
    <div><span style={{ fontSize: 13, color: t.txMuted }}>Olá,</span><h2 style={{ margin: 0, fontSize: 22, fontWeight: 700, letterSpacing: -0.5 }}>{usuario?.nome || "Passageiro"}</h2></div>
    {lv ? <div style={{ fontSize: 13, color: t.txMuted, padding: 20, textAlign: "center" }}>Carregando viagens...</div> :
    proxima && <Cd t={t} style={{ border: `1px solid ${t.borderStrong}` }}>
      <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 10 }}>
        <div><div style={{ fontSize: 10, color: t.pri, fontWeight: 700, letterSpacing: 1.5, textTransform: "uppercase", marginBottom: 4 }}>Próxima viagem</div>
          <div style={{ fontSize: 16, fontWeight: 600 }}>{proxima.embarcacao}</div><div style={{ fontSize: 13, color: t.txMuted, marginTop: 2 }}>{proxima.origem} → {proxima.destino}</div></div>
        <Badge status={proxima.atual ? "Em viagem" : "Confirmada"} t={t} />
      </div>
      <div style={{ display: "flex", gap: 20, fontSize: 12, color: t.txMuted }}>
        <span>📅 {fmt(proxima.dataViagem)}</span><span>🕐 {proxima.horarioSaida || "—"}</span></div>
    </Cd>}
    {!lv && (!viagens || viagens.length === 0) && <Cd t={t} style={{ padding: 16, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Nenhuma viagem ativa no momento.</div></Cd>}
    <div style={{ display: "flex", justifyContent: "space-between", marginTop: 4 }}>
      <span style={{ fontSize: 14, fontWeight: 600 }}>Amigos</span>
      <button style={{ background: "none", border: "none", color: t.pri, fontSize: 12, fontWeight: 600, cursor: "pointer", fontFamily: "inherit" }} onClick={() => onNav("amigos")}>Ver todos →</button></div>
    {amigos?.length > 0 ? amigos.slice(0, 3).map(f => (
      <Cd key={f.id} t={t} style={{ padding: 12 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
          <Av letters={initials(f.nome)} size={40} t={t} fotoUrl={f.fotoUrl} />
          <div style={{ flex: 1 }}><div style={{ fontSize: 14, fontWeight: 600 }}>{f.nome}</div><div style={{ fontSize: 12, color: t.txMuted }}>{f.cidade || "Sem cidade"}</div></div>
        </div>
      </Cd>)) : <Cd t={t} style={{ padding: 12, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Nenhum amigo. <span style={{ color: t.pri, cursor: "pointer" }} onClick={() => onNav("amigos")}>Adicionar →</span></div></Cd>}
    <span style={{ fontSize: 14, fontWeight: 600, marginTop: 4 }}>Encomendas</span>
    {le ? <div style={{ fontSize: 13, color: t.txMuted, padding: 10, textAlign: "center" }}>Carregando...</div> :
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
  const { data: amigos, loading } = useApi("/amigos", authHeaders);
  const { data: pendentes } = useApi("/amigos/pendentes", authHeaders);
  const { data: sugestoes } = useApi("/amigos/sugestoes", authHeaders);
  const [busca, setBusca] = useState("");
  const [resultados, setResultados] = useState(null);
  const [buscando, setBuscando] = useState(false);
  const [enviados, setEnviados] = useState({});
  const [msg, setMsg] = useState("");
  const inputStyle = { width: "100%", padding: "10px 14px", borderRadius: 10, border: `1px solid ${t.border}`, background: t.soft, color: t.tx, fontSize: 13, outline: "none", fontFamily: "inherit", boxSizing: "border-box" };

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
      if (res.ok) { setEnviados(e => ({ ...e, [amigoId]: true })); setMsg(`Convite enviado para ${nome}!`); setTimeout(() => setMsg(""), 3000); }
      else setMsg(data.erro || "Erro ao enviar.");
    } catch { setMsg("Erro de conexao."); }
  };

  const aceitarAmigo = async (amizadeId) => {
    await fetch(`${API}/amigos/${amizadeId}/aceitar`, { method: "PUT", headers: authHeaders });
    window.location.reload();
  };

  const removerAmigo = async (amizadeId) => {
    await fetch(`${API}/amigos/${amizadeId}`, { method: "DELETE", headers: authHeaders });
    window.location.reload();
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

  if (loading) return <div style={{ fontSize: 13, color: t.txMuted, padding: 20, textAlign: "center" }}>Carregando...</div>;

  return <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Amigos</h3>

    <input value={busca} onChange={e => pesquisar(e.target.value)} placeholder="Buscar pessoas por nome..." style={inputStyle} />

    {msg && <div style={{ padding: "8px 12px", borderRadius: 8, background: msg.includes("Erro") ? t.errBg : t.okBg, color: msg.includes("Erro") ? t.errTx : t.okTx, fontSize: 12 }}>{msg}</div>}

    {busca.trim().length >= 2 && <>
      <div style={{ fontSize: 13, fontWeight: 600, color: t.txSoft }}>Resultados</div>
      {buscando ? <div style={{ fontSize: 12, color: t.txMuted, padding: 8, textAlign: "center" }}>Buscando...</div> :
      resultados?.length > 0 ? resultados.map(p =>
        <PessoaCard key={p.idAmigo} p={p} acao={enviados[p.idAmigo]
          ? <span style={{ fontSize: 11, color: t.ok, fontWeight: 600 }}>Enviado</span>
          : <button onClick={() => addAmigo(p.idAmigo, p.nome)} style={{ padding: "6px 14px", borderRadius: 20, border: "none", background: t.priGrad, color: "#fff", fontSize: 11, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>+ Adicionar</button>
        } />) :
      <div style={{ fontSize: 12, color: t.txMuted, padding: 8, textAlign: "center" }}>Nenhum resultado para "{busca}"</div>}
    </>}

    {!busca.trim() && <>
      {pendentes?.length > 0 && <>
        <div style={{ fontSize: 13, fontWeight: 600, color: t.amber }}>Convites pendentes ({pendentes.length})</div>
        {pendentes.map(p => <Cd key={p.id} t={t} style={{ padding: 12, border: `1px solid ${t.warnBg}` }}>
          <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
            <Av letters={initials(p.nome)} size={42} t={t} fotoUrl={p.fotoUrl} />
            <div style={{ flex: 1 }}><div style={{ fontSize: 14, fontWeight: 600 }}>{p.nome}</div><div style={{ fontSize: 12, color: t.txMuted }}>{p.cidade || ""}</div></div>
          </div>
          <div style={{ display: "flex", gap: 8, marginTop: 10 }}>
            <button onClick={() => aceitarAmigo(p.id)} style={{ flex: 1, padding: "8px 0", borderRadius: 10, border: "none", background: t.priGrad, color: "#fff", fontSize: 12, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>Aceitar</button>
            <button onClick={() => removerAmigo(p.id)} style={{ flex: 1, padding: "8px 0", borderRadius: 10, border: `1px solid ${t.border}`, background: "transparent", color: t.txMuted, fontSize: 12, fontWeight: 600, cursor: "pointer", fontFamily: "inherit" }}>Recusar</button>
          </div>
        </Cd>)}
      </>}

      {amigos?.length > 0 && <>
        <div style={{ fontSize: 13, fontWeight: 600 }}>Seus amigos ({amigos.length})</div>
        {amigos.map(f => <PessoaCard key={f.id} p={f} acao={
          <button onClick={() => removerAmigo(f.id)} style={{ background: "none", border: "none", color: t.txMuted, fontSize: 14, cursor: "pointer", padding: 4 }} title="Remover">✕</button>
        } />)}
      </>}

      {sugestoes?.length > 0 && <>
        <div style={{ fontSize: 13, fontWeight: 600, color: t.txSoft, marginTop: 4 }}>Talvez voce conheca</div>
        {sugestoes.map(p => <PessoaCard key={p.idAmigo} p={p} acao={enviados[p.idAmigo]
          ? <span style={{ fontSize: 11, color: t.ok, fontWeight: 600 }}>Enviado</span>
          : <button onClick={() => addAmigo(p.idAmigo, p.nome)} style={{ padding: "6px 14px", borderRadius: 20, border: "none", background: t.priGrad, color: "#fff", fontSize: 11, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>+ Adicionar</button>
        } />)}
      </>}

      {(!amigos || amigos.length === 0) && (!sugestoes || sugestoes.length === 0) &&
        <Cd t={t} style={{ padding: 16, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Busque pessoas pelo nome para adicionar.</div></Cd>}
    </>}
  </div>;
}

function MapaCPF({ t, authHeaders }) {
  const { data: boats, loading } = useApi("/embarcacoes", authHeaders);
  const [sel, setSel] = useState(null);
  const embId = sel !== null ? boats?.[sel]?.id : null;
  const { data: viagensEmb } = useApi(embId ? `/viagens/embarcacao/${embId}` : null, embId ? authHeaders : null);
  if (loading) return <div style={{ fontSize: 13, color: t.txMuted, padding: 20, textAlign: "center" }}>Carregando embarcacoes...</div>;

  const detalhe = sel !== null ? boats?.[sel] : null;
  if (detalhe) return <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <button onClick={() => setSel(null)} style={{ background: "none", border: "none", color: t.txMuted, fontSize: 13, cursor: "pointer", fontFamily: "inherit", textAlign: "left", padding: 0 }}>← Voltar</button>
    {detalhe.fotoUrl && <img src={`${API}${detalhe.fotoUrl}`} alt={detalhe.nome} style={{ width: "100%", borderRadius: 14, objectFit: "cover", maxHeight: 200 }} />}
    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
      <div><h3 style={{ margin: 0, fontSize: 20, fontWeight: 700 }}>{detalhe.nome}</h3>
        <div style={{ fontSize: 13, color: t.txMuted, marginTop: 4 }}>{detalhe.rotaPrincipal || detalhe.rotaAtual || ""}</div></div>
      <Badge status={detalhe.status || "NO_PORTO"} t={t} />
    </div>
    {detalhe.descricao && <Cd t={t} style={{ padding: 14 }}><div style={{ fontSize: 13, color: t.txSoft, lineHeight: 1.7 }}>{detalhe.descricao}</div></Cd>}
    <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
      <Cd t={t} style={{ padding: 12, textAlign: "center" }}><div style={{ fontSize: 20, marginBottom: 4 }}>👥</div><div style={{ fontSize: 18, fontWeight: 700, color: t.pri }}>{detalhe.capacidadePassageiros || "—"}</div><div style={{ fontSize: 11, color: t.txMuted }}>Passageiros</div></Cd>
      <Cd t={t} style={{ padding: 12, textAlign: "center" }}><div style={{ fontSize: 20, marginBottom: 4 }}>📦</div><div style={{ fontSize: 18, fontWeight: 700, color: t.info }}>{detalhe.status === "EM_VIAGEM" ? "Em viagem" : "No porto"}</div><div style={{ fontSize: 11, color: t.txMuted }}>Status</div></Cd>
    </div>
    {detalhe.horarioSaidaPadrao && <Cd t={t} style={{ padding: 14, border: `1px solid ${t.borderStrong}` }}>
      <div style={{ fontSize: 12, fontWeight: 700, color: t.pri, textTransform: "uppercase", letterSpacing: 1, marginBottom: 8 }}>Horarios</div>
      {detalhe.horarioSaidaPadrao.split("\n").map((line, i) =>
        line.trim() === "" ? <div key={i} style={{ height: 8 }} /> :
        line === line.toUpperCase() && line.includes("→") ? <div key={i} style={{ fontSize: 13, fontWeight: 700, color: t.tx, marginTop: i > 0 ? 4 : 0 }}>{line}</div> :
        <div key={i} style={{ fontSize: 12, color: t.txSoft, lineHeight: 1.6, paddingLeft: line.startsWith("Sa") || line.startsWith("Ch") || line.startsWith("Pa") ? 8 : 0 }}>{line}</div>
      )}
    </Cd>}
    {detalhe.telefone && <Cd t={t} style={{ padding: 12 }}>
      <div style={{ fontSize: 12, color: t.txMuted }}>Telefone</div><div style={{ fontSize: 14, fontWeight: 600 }}>{detalhe.telefone}</div>
    </Cd>}
    {viagensEmb?.length > 0 && <>
      <div style={{ fontSize: 12, fontWeight: 700, color: t.pri, textTransform: "uppercase", letterSpacing: 1 }}>Proximas viagens</div>
      {viagensEmb.map(v => <Cd key={v.id} t={t} style={{ padding: 12 }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <div><div style={{ fontSize: 13, fontWeight: 600 }}>{v.origem} → {v.destino}</div><div style={{ fontSize: 12, color: t.txMuted, marginTop: 2 }}>Saida: {fmt(v.dataViagem)} • Chegada: {fmt(v.previsaoChegada || v.dataChegada)}</div></div>
          <Badge status={v.atual ? "Em viagem" : "Confirmada"} t={t} />
        </div>
      </Cd>)}
    </>}
    {detalhe.linkExterno && <a href={detalhe.linkExterno} target="_blank" rel="noopener noreferrer" style={{ textDecoration: "none" }}>
      <Cd t={t} style={{ padding: 14, textAlign: "center", border: `1px solid ${t.borderStrong}`, cursor: "pointer" }}>
        <div style={{ fontSize: 13, fontWeight: 600, color: t.pri }}>Ver pagina da embarcacao →</div>
      </Cd>
    </a>}
  </div>;

  return <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Embarcacoes</h3>
    {(!boats || boats.length === 0) && <Cd t={t} style={{ padding: 16, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Nenhuma embarcacao encontrada.</div></Cd>}
    {boats?.map((b, i) => <Cd key={b.id || i} t={t} style={{ padding: 0, overflow: "hidden", cursor: "pointer" }} onClick={() => setSel(i)}>
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

function PassagensCPF({ t, authHeaders }) {
  const { data: viagens, loading: lv } = useApi("/viagens/ativas", authHeaders);
  const { data: tarifas, loading: lt } = useApi("/tarifas", authHeaders);
  return <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Viagens ativas</h3>
    {lv ? <div style={{ fontSize: 13, color: t.txMuted, padding: 10, textAlign: "center" }}>Carregando...</div> :
    viagens?.length > 0 ? viagens.map(v => <Cd key={v.id} t={t} style={{ padding: 14 }}>
      <div style={{ display: "flex", justifyContent: "space-between" }}><div><div style={{ fontSize: 16, fontWeight: 600 }}>{v.embarcacao}</div><div style={{ fontSize: 13, color: t.txMuted, marginTop: 2 }}>{v.origem} → {v.destino}</div></div><Badge status={v.atual ? "Em viagem" : "Confirmada"} t={t} /></div>
      <div style={{ display: "flex", gap: 16, fontSize: 12, color: t.txMuted, marginTop: 10 }}><span>📅 {fmt(v.dataViagem)}</span><span>🕐 {v.horarioSaida || "—"}</span></div>
    </Cd>) : <Cd t={t} style={{ padding: 16, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Nenhuma viagem ativa.</div></Cd>}
    <div style={{ fontSize: 14, fontWeight: 600, marginTop: 8 }}>Tarifas por rota</div>
    {lt ? <div style={{ fontSize: 13, color: t.txMuted, padding: 10, textAlign: "center" }}>Carregando...</div> :
    tarifas?.length > 0 ? tarifas.map((x, i) =>
      <Cd key={i} t={t} style={{ padding: 12 }}><div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}><div><div style={{ fontSize: 14, fontWeight: 600 }}>{x.origem} → {x.destino}</div><div style={{ fontSize: 12, color: t.txMuted, marginTop: 2 }}>{x.tipoPassageiro}</div></div>
        <div style={{ textAlign: "right" }}><div style={{ fontSize: 12, color: t.pri, fontWeight: 600 }}>transporte</div><div style={{ fontSize: 16, fontWeight: 700 }}>{money(x.valorTransporte)}</div></div></div></Cd>) :
      <Cd t={t} style={{ padding: 12, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Nenhuma tarifa cadastrada.</div></Cd>}
  </div>;
}

/* ═══ CNPJ SCREENS ═══ */
function HomeCNPJ({ t, onNav, authHeaders, usuario }) {
  const { data: fretes, loading: lf } = useApi("/fretes", authHeaders);
  const { data: pedidos, loading: lp } = useApi("/lojas/pedidos", authHeaders);
  const { data: lojas, loading: ll } = useApi("/lojas", authHeaders);
  const fretesAtivos = fretes?.filter(f => f.status !== "ENTREGUE" && f.status !== "CANCELADO") || [];
  const totalDevedor = fretes?.reduce((s, f) => s + (f.valorDevedor || 0), 0) || 0;
  return <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
    <div><span style={{ fontSize: 13, color: t.txMuted }}>Empresa</span><h2 style={{ margin: 0, fontSize: 20, fontWeight: 700 }}>{usuario?.nome || "Empresa"}</h2></div>
    <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
      {[{ l: "Fretes ativos", v: lf ? "..." : String(fretesAtivos.length), c: t.pri, ic: "📦" },
        { l: "Pedidos", v: lp ? "..." : String(pedidos?.length || 0), c: t.amber, ic: "🛒" },
        { l: "Total fretes", v: lf ? "..." : String(fretes?.length || 0), c: t.info, ic: "✓" },
        { l: "Devedor", v: lf ? "..." : money(totalDevedor), c: t.err, ic: "💰" }].map((s, i) =>
        <Cd key={i} t={t} style={{ padding: 14, textAlign: "center" }}><div style={{ fontSize: 20, marginBottom: 4 }}>{s.ic}</div><div style={{ fontSize: 22, fontWeight: 700, color: s.c }}>{s.v}</div><div style={{ fontSize: 11, color: t.txMuted, marginTop: 2 }}>{s.l}</div></Cd>)}
    </div>
    <div style={{ display: "flex", justifyContent: "space-between", marginTop: 4 }}><span style={{ fontSize: 14, fontWeight: 600 }}>Fretes recentes</span>
      <button style={{ background: "none", border: "none", color: t.pri, fontSize: 12, fontWeight: 600, cursor: "pointer", fontFamily: "inherit" }} onClick={() => onNav("pedidos")}>Ver todos →</button></div>
    {lf ? <div style={{ fontSize: 13, color: t.txMuted, padding: 10, textAlign: "center" }}>Carregando...</div> :
    fretes?.slice(0, 3).map(f => <Cd key={f.id} t={t} style={{ padding: 12 }}>
      <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 4 }}><span style={{ fontSize: 12, fontWeight: 700, fontFamily: "'Space Mono', monospace", color: t.txSoft }}>FRT-{f.numeroFrete || f.id}</span><Badge status={f.status || "Aguardando"} t={t} /></div>
      <div style={{ fontSize: 13 }}>{f.nomeDestinatario}</div><div style={{ fontSize: 12, color: t.txMuted, marginTop: 2 }}>{f.nomeRota} • {f.embarcacao}</div>
      <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12, marginTop: 6 }}><span style={{ color: t.txMuted }}>{fmt(f.dataViagem)}</span><span style={{ fontWeight: 600 }}>{money(f.valorTotal)}</span></div>
    </Cd>) || <Cd t={t} style={{ padding: 12, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Nenhum frete encontrado.</div></Cd>}
    <div style={{ display: "flex", justifyContent: "space-between", marginTop: 4 }}><span style={{ fontSize: 14, fontWeight: 600 }}>Lojas parceiras</span>
      <button style={{ background: "none", border: "none", color: t.pri, fontSize: 12, fontWeight: 600, cursor: "pointer", fontFamily: "inherit" }} onClick={() => onNav("lojas")}>Ver todas →</button></div>
    {ll ? <div style={{ fontSize: 13, color: t.txMuted, padding: 10, textAlign: "center" }}>Carregando...</div> :
    lojas?.slice(0, 2).map(l => <Cd key={l.id} t={t} style={{ padding: 12 }}>
      <div style={{ display: "flex", alignItems: "center", gap: 10 }}><Av letters={initials(l.nomeLoja)} size={38} t={t} /><div style={{ flex: 1 }}><div style={{ fontSize: 14, fontWeight: 600 }}>{l.nomeLoja}{l.verificada && <span style={{ color: t.pri, marginLeft: 4, fontSize: 11 }}>✓</span>}</div><div style={{ fontSize: 12, color: t.txMuted }}>{l.segmento}</div></div></div>
    </Cd>) || null}
  </div>;
}

function LojasParceiras({ t, authHeaders }) {
  const { data: lojas, loading } = useApi("/lojas", authHeaders);
  const [sel, setSel] = useState(null);

  if (loading) return <div style={{ fontSize: 13, color: t.txMuted, padding: 20, textAlign: "center" }}>Carregando lojas...</div>;

  if (sel) {
    const loja = lojas?.find(l => l.id === sel);
    if (!loja) { setSel(null); return null; }
    return <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
      <button onClick={() => setSel(null)} style={{ background: "none", border: "none", color: t.txMuted, fontSize: 13, cursor: "pointer", fontFamily: "inherit", textAlign: "left", padding: 0 }}>← Voltar</button>
      <Cd t={t} style={{ padding: 18, border: `1px solid ${t.borderStrong}` }}>
        <div style={{ display: "flex", alignItems: "center", gap: 14, marginBottom: 14 }}><Av letters={initials(loja.nomeLoja)} size={54} t={t} />
          <div><div style={{ fontSize: 18, fontWeight: 700 }}>{loja.nomeLoja}</div><div style={{ fontSize: 13, color: t.txMuted }}>{loja.segmento}</div>
            {loja.verificada && <div style={{ fontSize: 11, color: t.pri, fontWeight: 600, marginTop: 3 }}>Verificada Naviera</div>}</div></div>
        <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 8 }}>Descricao:</div>
        <div style={{ fontSize: 12, color: t.txMuted, marginBottom: 14 }}>{loja.descricao || "Sem descricao."}</div>
      </Cd>
    </div>;
  }

  return <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Lojas parceiras</h3>
    <div style={{ fontSize: 13, color: t.txMuted }}>Fornecedores verificados que embarcam pelo Naviera.</div>
    {(!lojas || lojas.length === 0) && <Cd t={t} style={{ padding: 16, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Nenhuma loja cadastrada.</div></Cd>}
    {lojas?.map(l => <Cd key={l.id} t={t} style={{ padding: 14, cursor: "pointer" }} onClick={() => setSel(l.id)}>
      <div style={{ display: "flex", alignItems: "center", gap: 12 }}><Av letters={initials(l.nomeLoja)} size={46} t={t} />
        <div style={{ flex: 1 }}><div style={{ display: "flex", alignItems: "center", gap: 6 }}><span style={{ fontSize: 15, fontWeight: 600 }}>{l.nomeLoja}</span>{l.verificada && <span style={{ fontSize: 10, color: t.pri }}>✓</span>}</div>
          <div style={{ fontSize: 12, color: t.txMuted, marginTop: 1 }}>{l.segmento}</div></div>
        <span style={{ color: t.txMuted, fontSize: 16 }}>›</span></div></Cd>)}
  </div>;
}

function PedidosCNPJ({ t, authHeaders }) {
  const { data: pedidos, loading } = useApi("/lojas/pedidos", authHeaders);
  if (loading) return <div style={{ fontSize: 13, color: t.txMuted, padding: 20, textAlign: "center" }}>Carregando pedidos...</div>;
  return <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Pedidos da loja</h3>
    {(!pedidos || pedidos.length === 0) ? <Cd t={t} style={{ padding: 16, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Nenhum pedido recebido ainda.</div></Cd> :
    pedidos.map(p => <Cd key={p.id} t={t} style={{ padding: 14 }}>
      <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 6 }}><span style={{ fontSize: 12, fontWeight: 700, fontFamily: "'Space Mono', monospace", color: t.txSoft }}>PED-{String(p.id).padStart(4, "0")}</span><Badge status={p.status || "Aguardando"} t={t} /></div>
      <div style={{ fontSize: 14, fontWeight: 600 }}>{p.nomeComprador || "Cliente"}</div>
      <div style={{ fontSize: 13, color: t.txMuted, marginTop: 2 }}>{p.descricao || "Sem descricao"}</div>
      <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12, color: t.txMuted, marginTop: 6 }}><span>{fmt(p.dataPedido)}</span><span style={{ fontWeight: 600, color: t.tx }}>{money(p.valorTotal)}</span></div>
      {p.codigoRastreio && <div style={{ marginTop: 8, padding: "8px 12px", borderRadius: 8, background: t.accent, border: `1px solid ${t.border}` }}>
        <div style={{ fontSize: 12, color: t.pri, fontWeight: 600 }}>Rastreio: {p.codigoRastreio}</div></div>}
    </Cd>)}
  </div>;
}

function FinanceiroCNPJ({ t, authHeaders }) {
  const { data: fretes, loading } = useApi("/fretes", authHeaders);
  const totalDevedor = fretes?.reduce((s, f) => s + (f.valorDevedor || 0), 0) || 0;
  const totalPago = fretes?.reduce((s, f) => s + (f.valorPago || 0), 0) || 0;
  const fretesDevendo = fretes?.filter(f => (f.valorDevedor || 0) > 0) || [];
  return <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Financeiro</h3>
    {loading ? <div style={{ fontSize: 13, color: t.txMuted, padding: 20, textAlign: "center" }}>Carregando...</div> : <>
    <Cd t={t} style={{ padding: 16 }}>
      <div style={{ fontSize: 12, color: t.txMuted, marginBottom: 4 }}>Total pendente</div>
      <div style={{ fontSize: 28, fontWeight: 700, color: totalDevedor > 0 ? t.err : t.ok }}>{money(totalDevedor)}</div>
      <div style={{ fontSize: 12, color: t.txMuted, marginTop: 4 }}>{fretesDevendo.length} frete(s) em aberto</div></Cd>
    <Cd t={t} style={{ padding: 14, border: `1px solid ${t.borderStrong}` }}>
      <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 8 }}><span style={{ fontSize: 13, fontWeight: 600 }}>Total pago</span><span style={{ fontSize: 15, fontWeight: 700, color: t.pri }}>{money(totalPago)}</span></div>
      <div style={{ fontSize: 12, color: t.txMuted }}>{fretes?.length || 0} fretes no total</div></Cd>
    <div style={{ fontSize: 14, fontWeight: 600, marginTop: 4 }}>Fretes</div>
    {fretes?.map(f => <Cd key={f.id} t={t} style={{ padding: 12 }}>
      <div style={{ display: "flex", justifyContent: "space-between" }}><div><div style={{ fontSize: 13 }}>FRT-{f.numeroFrete || f.id} — {f.nomeDestinatario}</div><div style={{ fontSize: 11, color: t.txMuted, marginTop: 2 }}>{fmt(f.dataViagem)}</div></div>
        <div style={{ textAlign: "right" }}><div style={{ fontSize: 14, fontWeight: 700, color: (f.valorDevedor || 0) > 0 ? t.err : t.tx }}>{money(f.valorTotal)}</div>
          <Badge status={(f.valorDevedor || 0) > 0 ? "Pendente" : "Pago"} t={t} /></div></div></Cd>)}
    </>}
  </div>;
}

function LojaCNPJ({ t, authHeaders }) {
  const { data: loja, loading } = useApi("/lojas/minha", authHeaders);
  if (loading) return <div style={{ fontSize: 13, color: t.txMuted, padding: 20, textAlign: "center" }}>Carregando...</div>;
  if (!loja) return <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Minha loja</h3>
    <Cd t={t} style={{ padding: 16, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Voce ainda nao tem uma loja cadastrada.</div></Cd>
  </div>;
  return <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <div style={{ display: "flex", justifyContent: "space-between" }}><h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Minha loja</h3>{loja.verificada && <Badge status="Verificada" t={t} />}</div>
    <Cd t={t} style={{ padding: 16, border: `1px solid ${t.borderStrong}` }}>
      <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 14 }}><Av letters={initials(loja.nomeLoja)} size={50} t={t} />
        <div><div style={{ fontSize: 16, fontWeight: 600 }}>{loja.nomeLoja}</div><div style={{ fontSize: 12, color: t.txMuted }}>{loja.segmento}</div></div></div>
      <div style={{ fontSize: 12, color: t.txMuted, lineHeight: 1.6 }}>{loja.descricao || "Clientes compram, voce vincula ao frete, rastreio automatico."}</div></Cd>
    <div style={{ fontSize: 14, fontWeight: 600, marginTop: 4 }}>Como funciona</div>
    {[{ s: "1", ti: "Cliente compra na vitrine", desc: "Pedido aparece na aba Pedidos", c: t.amber },
      { s: "2", ti: "Voce vincula ao frete", desc: "Associa o pedido ao embarque", c: t.pri },
      { s: "3", ti: "Rastreio automatico", desc: "Cliente acompanha ate a entrega", c: t.info }].map((s, i) =>
      <Cd key={i} t={t} style={{ padding: 12, borderLeft: `3px solid ${s.c}`, borderRadius: "0 14px 14px 0" }}>
        <div style={{ display: "flex", alignItems: "flex-start", gap: 10 }}>
          <div style={{ width: 26, height: 26, borderRadius: "50%", background: t.accent, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 12, fontWeight: 700, color: s.c, flexShrink: 0 }}>{s.s}</div>
          <div><div style={{ fontSize: 14, fontWeight: 600 }}>{s.ti}</div><div style={{ fontSize: 12, color: t.txMuted, marginTop: 2 }}>{s.desc}</div></div></div></Cd>)}
  </div>;
}

/* ═══ API ═══ */
const API = import.meta.env.VITE_API_URL || "http://localhost:8080/api";

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
    const docErro = validarDocumento(form.documento, tipo);
    if (docErro) { setErro(docErro); return; }
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
  const inputStyle = { width: "100%", padding: "10px 12px", borderRadius: 8, border: `1px solid ${t.border}`, background: t.soft, color: t.tx, fontSize: 13, outline: "none", fontFamily: "inherit", boxSizing: "border-box" };

  useState(() => {
    if (!token) return;
    fetch(`${API}/perfil`, { headers: authHeaders })
      .then(r => r.ok ? r.json() : Promise.reject("Erro ao carregar perfil"))
      .then(data => { setPerfil(data); setForm({ nome: data.nome || "", email: data.email || "", telefone: data.telefone || "", cidade: data.cidade || "" }); })
      .catch(e => setErro(typeof e === "string" ? e : "Erro de conexao."))
      .finally(() => setLoading(false));
  }, []);

  const uploadFoto = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setErro(""); setSucesso(""); setUploadingFoto(true);
    const fd = new FormData();
    fd.append("foto", file);
    try {
      const res = await fetch(`${API}/perfil/foto`, { method: "POST", headers: { "Authorization": authHeaders.Authorization }, body: fd });
      const data = await res.json();
      if (!res.ok) { setErro(data.erro || "Erro ao enviar foto."); return; }
      setPerfil(p => ({ ...p, fotoUrl: data.fotoUrl }));
      if (onFotoChange) onFotoChange(`${API}${data.fotoUrl}`);
      setSucesso("Foto atualizada!");
    } catch { setErro("Erro de conexao."); } finally { setUploadingFoto(false); }
  };

  const fotoSrc = perfil?.fotoUrl ? `${API}${perfil.fotoUrl}` : null;

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
      <div style={{ display: "flex", alignItems: "center", gap: 14, marginBottom: 16 }}>
        <div style={{ position: "relative" }}>
          {fotoSrc ? <img src={fotoSrc} alt="Foto" style={{ width: 64, height: 64, borderRadius: "50%", objectFit: "cover", border: `2px solid ${t.border}` }} />
            : <Av letters={initials(perfil.nome)} size={64} t={t} />}
          <label style={{ position: "absolute", bottom: -2, right: -2, width: 24, height: 24, borderRadius: "50%", background: t.priGrad, display: "flex", alignItems: "center", justifyContent: "center", cursor: uploadingFoto ? "default" : "pointer", border: `2px solid ${t.card}` }}>
            <span style={{ fontSize: 12, color: "#fff" }}>{uploadingFoto ? "..." : "+"}</span>
            <input type="file" accept="image/jpeg,image/png,image/webp" onChange={uploadFoto} style={{ display: "none" }} disabled={uploadingFoto} />
          </label>
        </div>
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
  const [profile, setProfile] = useState(() => { try { const u = JSON.parse(localStorage.getItem("naviera_usuario")); return u?.tipo === "CNPJ" ? "cnpj" : u ? "cpf" : null; } catch { return null; } });
  const [tab, setTab] = useState("home");
  const [tabHistory, setTabHistory] = useState([]);
  const navigateTab = (newTab) => {
    setTabHistory(h => [...h, tab]);
    setTab(newTab);
  };
  const goBack = () => {
    if (tabHistory.length > 0) {
      const prev = tabHistory[tabHistory.length - 1];
      setTabHistory(h => h.slice(0, -1));
      setTab(prev);
    }
  };
  const [mode, setMode] = useState("light");
  const [aiOpen, setAiOpen] = useState(false);
  const [tela, setTela] = useState("login");
  const [msgSucesso, setMsgSucesso] = useState("");
  const [token, setToken] = useState(() => localStorage.getItem("naviera_token"));
  const [usuario, setUsuario] = useState(() => { try { return JSON.parse(localStorage.getItem("naviera_usuario")); } catch { return null; } });
  const [minhaFoto, setMinhaFoto] = useState(null);
  const [loginDoc, setLoginDoc] = useState("");
  const [loginSenha, setLoginSenha] = useState("");
  const [loginErro, setLoginErro] = useState("");
  const [loginLoading, setLoginLoading] = useState(false);
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
      const res = await fetch(`${API}/auth/login`, {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ documento: loginDoc.trim(), senha: loginSenha })
      });
      const data = await res.json();
      if (!res.ok) { setLoginErro(data.erro || "Credenciais invalidas."); return; }
      localStorage.setItem("naviera_token", data.token);
      localStorage.setItem("naviera_usuario", JSON.stringify({ nome: data.nome, tipo: data.tipo, id: data.id }));
      setToken(data.token);
      setUsuario({ nome: data.nome, tipo: data.tipo, id: data.id });
      setProfile(data.tipo === "CNPJ" ? "cnpj" : "cpf");
      setTab("home"); setTabHistory([]);
      setLoginDoc(""); setLoginSenha(""); setMsgSucesso("");
    } catch { setLoginErro("Erro de conexao com o servidor."); } finally { setLoginLoading(false); }
  };

  const doLogout = () => { localStorage.removeItem("naviera_token"); localStorage.removeItem("naviera_usuario"); setProfile(null); setToken(null); setUsuario(null); setTab("home"); setTabHistory([]); };

  const inputStyle = { width: "100%", padding: "12px 14px", borderRadius: 10, border: `1px solid ${t.border}`, background: t.soft, color: t.tx, fontSize: 13, outline: "none", fontFamily: "inherit", boxSizing: "border-box" };

  if (!profile) return (
    <div style={{ minHeight: "100vh", background: t.bg, display: "flex", alignItems: "center", justifyContent: "center", padding: 20, fontFamily: "'Sora', sans-serif", color: t.tx, transition: "all 0.3s" }}>
      <link href="https://fonts.googleapis.com/css2?family=Sora:wght@300;400;500;600;700;800&family=Space+Mono:wght@400;700&display=swap" rel="stylesheet"/>
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
    if (tab === "perfil") return <PerfilScreen t={t} token={token} authHeaders={authHeaders} usuario={usuario} onFotoChange={setMinhaFoto} />;
    if (isCPF) { if (tab === "home") return <HomeCPF t={t} onNav={navigateTab} authHeaders={authHeaders} usuario={usuario} />; if (tab === "amigos") return <AmigosCPF t={t} authHeaders={authHeaders} />; if (tab === "mapa") return <MapaCPF t={t} authHeaders={authHeaders} />; if (tab === "passagens") return <PassagensCPF t={t} authHeaders={authHeaders} />; }
    else { if (tab === "home") return <HomeCNPJ t={t} onNav={navigateTab} authHeaders={authHeaders} usuario={usuario} />; if (tab === "pedidos") return <PedidosCNPJ t={t} authHeaders={authHeaders} />; if (tab === "lojas") return <LojasParceiras t={t} authHeaders={authHeaders} />; if (tab === "financeiro") return <FinanceiroCNPJ t={t} authHeaders={authHeaders} />; if (tab === "loja") return <LojaCNPJ t={t} authHeaders={authHeaders} />; }
  };

  return (
    <div style={{ minHeight: "100vh", background: t.bg, fontFamily: "'Sora', sans-serif", color: t.tx, maxWidth: 420, margin: "0 auto", position: "relative", transition: "all 0.3s" }}>
      <link href="https://fonts.googleapis.com/css2?family=Sora:wght@300;400;500;600;700;800&family=Space+Mono:wght@400;700&display=swap" rel="stylesheet"/>

      {/* Header */}
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "14px 18px 10px", borderBottom: `1px solid ${t.border}`, background: t.card }}>
        <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
          {tab !== "home" && <button onClick={goBack} style={{ background: "none", border: "none", color: t.txMuted, fontSize: 20, cursor: "pointer", padding: "0 4px 0 0", display: "flex", alignItems: "center", fontFamily: "inherit" }}>←</button>}
          <div style={{ width: 30, height: 30, borderRadius: 8, background: t.accent, display: "flex", alignItems: "center", justifyContent: "center" }}><Logo size={16} t={t} /></div>
          <span style={{ fontSize: 15, fontWeight: 700, letterSpacing: 2 }}>NAVIERA</span>
          <span style={{ fontSize: 9, padding: "2px 6px", borderRadius: 6, background: t.accent, color: t.pri, fontWeight: 700, marginLeft: 4, letterSpacing: 0.5 }}>{isCPF ? "CPF" : "CNPJ"}</span>
        </div>
        <div style={{ display: "flex", gap: 6 }}>
          <button onClick={() => navigateTab("perfil")} style={{ width: 32, height: 32, borderRadius: "50%", border: `2px solid ${tab === "perfil" ? t.pri : t.border}`, background: tab === "perfil" ? t.accent : t.soft, cursor: "pointer", fontSize: 13, display: "flex", alignItems: "center", justifyContent: "center", color: tab === "perfil" ? t.pri : t.txMuted, padding: 0, overflow: "hidden" }} title="Meu perfil">
            {minhaFoto ? <img src={minhaFoto} alt="" style={{ width: "100%", height: "100%", objectFit: "cover" }} /> : "👤"}
          </button>
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
          {tabs.map(tb => <button key={tb.id} onClick={() => { setTab(tb.id); setTabHistory([]); }} style={{ flex: 1, padding: "8px 2px", borderRadius: 8, border: "none", background: tab === tb.id ? t.accent : "transparent", color: tab === tb.id ? t.pri : t.txMuted, fontSize: 10, fontWeight: 600, cursor: "pointer", display: "flex", flexDirection: "column", alignItems: "center", gap: 3, fontFamily: "inherit", transition: "all 0.2s" }}>
            <span style={{ fontSize: 15 }}>{tb.icon}</span>{tb.label}
            {tab === tb.id && <div style={{ width: 4, height: 4, borderRadius: 2, background: t.pri }} />}
          </button>)}
        </div>
      </div>
    </div>
  );
}

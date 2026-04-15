import { useState } from "react";
import { API, useApi, authFetch } from "../api.js";
import { initials } from "../helpers.js";
import { IconSearch, IconCheck, IconPlus } from "../icons.jsx";
import Cd from "../components/Card.jsx";
import Av from "../components/Avatar.jsx";
import Skeleton from "../components/Skeleton.jsx";
import Toast from "../components/Toast.jsx";

export default function AmigosCPF({ t, authHeaders }) {
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
      const res = await authFetch(`${API}/amigos/buscar?nome=${encodeURIComponent(nome.trim())}`, { headers: authHeaders });
      if (res.ok) setResultados(await res.json());
    } catch {} finally { setBuscando(false); }
  };

  const addAmigo = async (amigoId, nome) => {
    try {
      const res = await authFetch(`${API}/amigos/${amigoId}`, { method: "POST", headers: authHeaders });
      const data = await res.json();
      if (res.ok) { setEnviados(e => ({ ...e, [amigoId]: true })); setToast(`Convite enviado para ${nome}!`); }
      else setToast(data.erro || "Erro ao enviar.");
    } catch { setToast("Erro de conex\u00e3o."); }
  };

  const aceitarAmigo = async (amizadeId) => {
    await authFetch(`${API}/amigos/${amizadeId}/aceitar`, { method: "PUT", headers: authHeaders });
    refresh(); refreshPendentes();
  };

  const removerAmigo = async (amizadeId) => {
    await authFetch(`${API}/amigos/${amizadeId}`, { method: "DELETE", headers: authHeaders });
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
    {resultados && !buscando && resultados.length === 0 && <div style={{ fontSize: 12, color: t.txMuted, textAlign: "center" }}>Ningu\u00e9m encontrado.</div>}
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
      <div style={{ fontSize: 13, fontWeight: 600, color: t.txSoft, marginTop: 4 }}>Sugest\u00f5es</div>
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

import { useState } from "react";
import { useApi } from "../api.js";
import { initials } from "../helpers.js";
import { IconBack, IconCheck } from "../icons.jsx";
import Cd from "../components/Card.jsx";
import Av from "../components/Avatar.jsx";
import Skeleton from "../components/Skeleton.jsx";
import ErrorRetry from "../components/ErrorRetry.jsx";

export default function LojasParceiras({ t, authHeaders }) {
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
        <div style={{ fontSize: 12, color: t.txMuted, marginBottom: 14 }}>{loja.descricao || "Sem descri\u00e7\u00e3o."}</div>
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
        <span style={{ color: t.txMuted, fontSize: 16 }}>\u203a</span></div></Cd>)}
  </div>;
}

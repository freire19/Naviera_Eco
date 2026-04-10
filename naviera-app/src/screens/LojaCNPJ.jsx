import { useApi } from "../api.js";
import { initials } from "../helpers.js";
import Cd from "../components/Card.jsx";
import Av from "../components/Avatar.jsx";
import Badge from "../components/Badge.jsx";
import Skeleton from "../components/Skeleton.jsx";
import ErrorRetry from "../components/ErrorRetry.jsx";

export default function LojaCNPJ({ t, authHeaders }) {
  const { data: loja, loading, erro, refresh } = useApi("/lojas/minha", authHeaders);
  if (loading) return <Skeleton t={t} height={80} count={2} />;
  if (erro) return <ErrorRetry erro={erro} onRetry={refresh} t={t} />;
  if (!loja) return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Minha loja</h3>
    <Cd t={t} style={{ padding: 16, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Voc\u00ea ainda n\u00e3o tem uma loja cadastrada.</div></Cd>
  </div>;
  return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <div style={{ display: "flex", justifyContent: "space-between" }}><h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Minha loja</h3>{loja.verificada && <Badge status="Verificada" t={t} />}</div>
    <Cd t={t} style={{ padding: 16, border: `1px solid ${t.borderStrong}` }}>
      <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 14 }}><Av letters={initials(loja.nomeLoja)} size={50} t={t} />
        <div><div style={{ fontSize: 16, fontWeight: 600 }}>{loja.nomeLoja}</div><div style={{ fontSize: 12, color: t.txMuted }}>{loja.segmento}</div></div></div>
      <div style={{ fontSize: 12, color: t.txMuted, lineHeight: 1.6 }}>{loja.descricao || "Clientes compram, voc\u00ea vincula ao frete, rastreio autom\u00e1tico."}</div></Cd>
    <div style={{ fontSize: 14, fontWeight: 600, marginTop: 4 }}>Como funciona</div>
    {[{ s: "1", ti: "Cliente compra na vitrine", desc: "Pedido aparece na aba Pedidos", c: t.amber },
      { s: "2", ti: "Voc\u00ea vincula ao frete", desc: "Associa o pedido ao embarque", c: t.pri },
      { s: "3", ti: "Rastreio autom\u00e1tico", desc: "Cliente acompanha at\u00e9 a entrega", c: t.info }].map((s, i) =>
      <Cd key={i} t={t} style={{ padding: 12, borderLeft: `3px solid ${s.c}`, borderRadius: "0 14px 14px 0" }}>
        <div style={{ display: "flex", alignItems: "flex-start", gap: 10 }}>
          <div style={{ width: 26, height: 26, borderRadius: "50%", background: t.accent, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 12, fontWeight: 700, color: s.c, flexShrink: 0 }}>{s.s}</div>
          <div><div style={{ fontSize: 14, fontWeight: 600 }}>{s.ti}</div><div style={{ fontSize: 12, color: t.txMuted, marginTop: 2 }}>{s.desc}</div></div></div></Cd>)}
  </div>;
}

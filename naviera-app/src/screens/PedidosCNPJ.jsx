import { useApi } from "../api.js";
import { fmt, money } from "../helpers.js";
import { IconPackage } from "../icons.jsx";
import Badge from "../components/Badge.jsx";
import Cd from "../components/Card.jsx";
import Skeleton from "../components/Skeleton.jsx";
import ErrorRetry from "../components/ErrorRetry.jsx";
import { useTheme } from "../contexts/ThemeContext.jsx";
import { useAuth } from "../contexts/AuthContext.jsx";

export default function PedidosCNPJ() {
  const { t } = useTheme();
  const { authHeaders } = useAuth();
  const { data: pedidos, loading, erro, refresh } = useApi("/lojas/pedidos", authHeaders);
  if (loading) return <Skeleton height={80} count={3} />;
  if (erro) return <ErrorRetry erro={erro} onRetry={refresh} />;
  return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Pedidos da loja</h3>
    {(!pedidos || pedidos.length === 0) ? <Cd style={{ padding: 16, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Nenhum pedido recebido ainda.</div></Cd> :
    pedidos.map(p => <Cd key={p.id} style={{ padding: 14 }}>
      <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 6 }}><span style={{ fontSize: 12, fontWeight: 700, fontFamily: "'Space Mono', monospace", color: t.txSoft }}>PED-{String(p.id).padStart(4, "0")}</span><Badge status={p.status || "Aguardando"} /></div>
      <div style={{ fontSize: 14, fontWeight: 600 }}>{p.nomeComprador || "Cliente"}</div>
      <div style={{ fontSize: 13, color: t.txMuted, marginTop: 2 }}>{p.descricao || "Sem descri\u00e7\u00e3o"}</div>
      <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12, color: t.txMuted, marginTop: 6 }}><span>{fmt(p.dataPedido)}</span><span style={{ fontWeight: 600, color: t.tx }}>{money(p.valorTotal)}</span></div>
      {p.codigoRastreio && <div style={{ marginTop: 8, padding: "8px 12px", borderRadius: 8, background: t.accent, border: `1px solid ${t.border}` }}>
        <div style={{ fontSize: 12, color: t.pri, fontWeight: 600, display: "flex", alignItems: "center", gap: 4 }}><IconPackage size={12} color={t.pri} /> Rastreio: {p.codigoRastreio}</div></div>}
    </Cd>)}
  </div>;
}

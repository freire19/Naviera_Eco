import { useApi } from "../api.js";
import { fmt, money } from "../helpers.js";
import Badge from "../components/Badge.jsx";
import Cd from "../components/Card.jsx";
import Skeleton from "../components/Skeleton.jsx";
import ErrorRetry from "../components/ErrorRetry.jsx";

export default function FinanceiroCNPJ({ t, authHeaders }) {
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
      <div style={{ display: "flex", justifyContent: "space-between" }}><div><div style={{ fontSize: 13 }}>FRT-{f.numeroFrete || f.id} \u2014 {f.nomeDestinatario}</div><div style={{ fontSize: 11, color: t.txMuted, marginTop: 2 }}>{fmt(f.dataViagem)}</div></div>
        <div style={{ textAlign: "right" }}><div style={{ fontSize: 14, fontWeight: 700, color: (f.valorDevedor || 0) > 0 ? t.err : t.tx }}>{money(f.valorTotal)}</div>
          <Badge status={(f.valorDevedor || 0) > 0 ? "Pendente" : "Pago"} t={t} /></div></div></Cd>)}
  </div>;
}

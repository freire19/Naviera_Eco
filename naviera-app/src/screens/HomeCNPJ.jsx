import { useApi } from "../api.js";
import { fmt, money, initials } from "../helpers.js";
import { IconPackage, IconCart, IconCheck, IconWallet } from "../icons.jsx";
import Badge from "../components/Badge.jsx";
import Cd from "../components/Card.jsx";
import Av from "../components/Avatar.jsx";
import Skeleton from "../components/Skeleton.jsx";
import ErrorRetry from "../components/ErrorRetry.jsx";
import { useTheme } from "../contexts/ThemeContext.jsx";
import { useAuth } from "../contexts/AuthContext.jsx";

export default function HomeCNPJ({ onNav }) {
  const { t } = useTheme();
  const { authHeaders, usuario } = useAuth();
  const { data: fretes, loading: lf, erro: ef, refresh: rf } = useApi("/fretes", authHeaders);
  const { data: pedidos, loading: lp } = useApi("/lojas/pedidos", authHeaders);
  const { data: lojas, loading: ll } = useApi("/lojas", authHeaders);
  const fretesAtivos = fretes?.filter(f => f.status !== "ENTREGUE" && f.status !== "CANCELADO") || [];
  const totalDevedor = fretes?.reduce((s, f) => s + (f.valorDevedor || 0), 0) || 0;

  if (ef) return <ErrorRetry erro={ef} onRetry={rf} />;

  return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 14 }}>
    <div><span style={{ fontSize: 13, color: t.txMuted }}>Empresa</span><h2 style={{ margin: 0, fontSize: 20, fontWeight: 700 }}>{usuario?.nome || "Empresa"}</h2></div>
    <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
      {[{ l: "Fretes ativos", v: lf ? "..." : String(fretesAtivos.length), c: t.pri, Ic: IconPackage },
        { l: "Pedidos", v: lp ? "..." : String(pedidos?.length || 0), c: t.amber, Ic: IconCart },
        { l: "Total fretes", v: lf ? "..." : String(fretes?.length || 0), c: t.info, Ic: IconCheck },
        { l: "Devedor", v: lf ? "..." : money(totalDevedor), c: t.err, Ic: IconWallet }].map((s, i) =>
        <Cd key={i} style={{ padding: 14, textAlign: "center" }}>
          <s.Ic size={20} color={s.c} /><div style={{ fontSize: 22, fontWeight: 700, color: s.c, marginTop: 4 }}>{s.v}</div><div style={{ fontSize: 11, color: t.txMuted, marginTop: 2 }}>{s.l}</div>
        </Cd>)}
    </div>
    <div style={{ display: "flex", justifyContent: "space-between", marginTop: 4 }}><span style={{ fontSize: 14, fontWeight: 600 }}>Fretes recentes</span>
      <button style={{ background: "none", border: "none", color: t.pri, fontSize: 12, fontWeight: 600, cursor: "pointer" }} onClick={() => onNav("pedidos")}>Ver todos \u2192</button></div>
    {lf ? <Skeleton height={80} count={2} /> :
    fretes?.length > 0 ? fretes.slice(0, 3).map(f => <Cd key={f.id} style={{ padding: 12 }}>
      <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 4 }}><span style={{ fontSize: 12, fontWeight: 700, fontFamily: "'Space Mono', monospace", color: t.txSoft }}>FRT-{f.numeroFrete || f.id}</span><Badge status={f.status || "Aguardando"} /></div>
      <div style={{ fontSize: 13 }}>{f.nomeDestinatario}</div><div style={{ fontSize: 12, color: t.txMuted, marginTop: 2 }}>{f.nomeRota} \u2022 {f.embarcacao}</div>
      <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12, marginTop: 6 }}><span style={{ color: t.txMuted }}>{fmt(f.dataViagem)}</span><span style={{ fontWeight: 600 }}>{money(f.valorTotal)}</span></div>
    </Cd>) : <Cd style={{ padding: 12, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Nenhum frete recente</div></Cd>}
    <div style={{ display: "flex", justifyContent: "space-between", marginTop: 4 }}><span style={{ fontSize: 14, fontWeight: 600 }}>Lojas parceiras</span>
      <button style={{ background: "none", border: "none", color: t.pri, fontSize: 12, fontWeight: 600, cursor: "pointer" }} onClick={() => onNav("lojas")}>Ver todas \u2192</button></div>
    {ll ? <Skeleton height={60} count={1} /> :
    lojas?.length > 0 ? lojas.slice(0, 2).map(l => <Cd key={l.id} style={{ padding: 12 }}>
      <div style={{ display: "flex", alignItems: "center", gap: 10 }}><Av letters={initials(l.nomeLoja)} size={38} /><div style={{ flex: 1 }}><div style={{ fontSize: 14, fontWeight: 600 }}>{l.nomeLoja}{l.verificada && <span style={{ color: t.pri, marginLeft: 4, fontSize: 11 }}><IconCheck size={11} color={t.pri} /></span>}</div><div style={{ fontSize: 12, color: t.txMuted }}>{l.segmento}</div></div></div>
    </Cd>) : <Cd style={{ padding: 12, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Nenhuma loja parceira</div></Cd>}
  </div>;
}

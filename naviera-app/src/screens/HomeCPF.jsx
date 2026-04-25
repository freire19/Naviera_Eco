import { useApi } from "../api.js";
import { fmt, money, initials } from "../helpers.js";
import { IconCalendar, IconClock } from "../icons.jsx";
import Badge from "../components/Badge.jsx";
import Cd from "../components/Card.jsx";
import Av from "../components/Avatar.jsx";
import Skeleton from "../components/Skeleton.jsx";
import ErrorRetry from "../components/ErrorRetry.jsx";
import { useTheme } from "../contexts/ThemeContext.jsx";
import { useAuth } from "../contexts/AuthContext.jsx";

export default function HomeCPF({ onNav }) {
  const { t } = useTheme();
  const { authHeaders, usuario } = useAuth();
  const { data: viagens, loading: lv, erro: ev, refresh: rv } = useApi("/viagens/ativas", authHeaders);
  const { data: encomendas, loading: le } = useApi("/encomendas", authHeaders);
  const { data: amigos } = useApi("/amigos", authHeaders);
  const proxima = viagens?.find(v => v.atual) || viagens?.[0];

  if (ev) return <ErrorRetry erro={ev} onRetry={rv} />;

  return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 14 }}>
    <div><span style={{ fontSize: 13, color: t.txMuted }}>Ol\u00e1,</span><h1 style={{ margin: 0, fontSize: 22, fontWeight: 700 }}>{usuario?.nome || "Passageiro"}</h1></div>

    {lv ? <Skeleton height={90} count={1} /> :
    proxima && <Cd style={{ border: `1px solid ${t.borderStrong}` }} onClick={() => onNav("mapa")}>
      <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 10 }}>
        <div><div style={{ fontSize: 10, color: t.pri, fontWeight: 700, letterSpacing: 1.5, textTransform: "uppercase", marginBottom: 4 }}>Pr\u00f3xima viagem</div>
          <div style={{ fontSize: 16, fontWeight: 600 }}>{proxima.embarcacao}</div><div style={{ fontSize: 13, color: t.txMuted, marginTop: 2 }}>{proxima.origem} \u2192 {proxima.destino}</div></div>
        <Badge status={proxima.atual ? "Em viagem" : "Confirmada"} />
      </div>
      <div style={{ display: "flex", gap: 20, fontSize: 12, color: t.txMuted }}>
        <span style={{ display: "flex", alignItems: "center", gap: 4 }}><IconCalendar size={12} color={t.txMuted} /> {fmt(proxima.dataViagem)}</span>
        <span style={{ display: "flex", alignItems: "center", gap: 4 }}><IconClock size={12} color={t.txMuted} /> {proxima.horarioSaida || "\u2014"}</span>
      </div>
    </Cd>}

    {!lv && (!viagens || viagens.length === 0) && <Cd style={{ padding: 16, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Nenhuma viagem ativa no momento.</div></Cd>}

    <div style={{ display: "flex", justifyContent: "space-between", marginTop: 4 }}>
      <span style={{ fontSize: 14, fontWeight: 600 }}>Amigos</span>
      <button style={{ background: "none", border: "none", color: t.pri, fontSize: 12, fontWeight: 600, cursor: "pointer" }} onClick={() => onNav("amigos")}>Ver todos \u2192</button>
    </div>
    {amigos?.length > 0 ? amigos.slice(0, 3).map(f => (
      <Cd key={f.id} style={{ padding: 12 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
          <Av letters={initials(f.nome)} size={40} fotoUrl={f.fotoUrl} />
          <div style={{ flex: 1 }}><div style={{ fontSize: 14, fontWeight: 600 }}>{f.nome}</div><div style={{ fontSize: 12, color: t.txMuted }}>{f.cidade || "Sem cidade"}</div></div>
        </div>
      </Cd>)) : <Cd style={{ padding: 12, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Nenhum amigo. <button onClick={() => onNav("amigos")} style={{ color: t.pri, background: "none", border: "none", cursor: "pointer", padding: 0, font: "inherit" }}>Adicionar \u2192</button></div></Cd>}

    <span style={{ fontSize: 14, fontWeight: 600, marginTop: 4 }}>Encomendas</span>
    {le ? <Skeleton height={70} count={2} /> :
    encomendas?.length > 0 ? encomendas.slice(0, 5).map(e => (
      <Cd key={e.id} style={{ padding: 12 }}>
        <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 4 }}>
          <span style={{ fontSize: 12, fontWeight: 700, fontFamily: "'Space Mono', monospace", color: t.txSoft }}>{e.numeroEncomenda || `ENC-${e.id}`}</span>
          <Badge status={e.entregue ? "Entregue" : "Em tr\u00e2nsito"} /></div>
        <div style={{ fontSize: 13 }}>{e.rota}</div><div style={{ fontSize: 12, color: t.txMuted }}>{e.embarcacao} \u2022 {money(e.totalAPagar)}</div>
      </Cd>)) : <Cd style={{ padding: 12, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Nenhuma encomenda encontrada.</div></Cd>}
  </div>;
}

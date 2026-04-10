import { useState } from "react";
import { API, useApi } from "../api.js";
import { fmt } from "../helpers.js";
import { IconBack, IconUsers, IconMapPin } from "../icons.jsx";
import Badge from "../components/Badge.jsx";
import Bar from "../components/Bar.jsx";
import Cd from "../components/Card.jsx";
import Skeleton from "../components/Skeleton.jsx";
import ErrorRetry from "../components/ErrorRetry.jsx";

export default function MapaCPF({ t, authHeaders }) {
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
            <text x="115" y="66" textAnchor="middle" fill="rgba(255,255,255,0.55)" fontSize="7.5">Codaj\u00e1s</text>
            <circle cx="315" cy="70" r="2.5" fill="rgba(255,255,255,0.4)"/>
            <text x="315" y="61" textAnchor="middle" fill="rgba(255,255,255,0.55)" fontSize="7.5">Fonte Boa</text>
            <circle cx="392" cy="86" r="4.5" fill="#EF4444" stroke="white" strokeWidth="1.5"/>
            <text x="392" y="104" textAnchor="middle" fill="white" fontSize="8.5" fontWeight="500">Juta\u00ed</text>
          </svg>
          <div style={{ position: "absolute", bottom: 8, left: 8, right: 8, background: "rgba(0,0,0,0.55)", borderRadius: 10, padding: "8px 12px", display: "flex", justifyContent: "space-between" }}>
            <div><div style={{ fontSize: 8, color: "rgba(255,255,255,0.5)" }}>Velocidade</div><div style={{ fontSize: 12, color: "white", fontWeight: 600 }}>12.4 n\u00f3s</div></div>
            <div><div style={{ fontSize: 8, color: "rgba(255,255,255,0.5)" }}>ETA</div><div style={{ fontSize: 12, color: "white", fontWeight: 600 }}>17/04 14h</div></div>
            <div><div style={{ fontSize: 8, color: "rgba(255,255,255,0.5)" }}>Percorrido</div><div style={{ fontSize: 12, color: "white", fontWeight: 600 }}>58%</div></div>
          </div>
        </div>
        <div style={{ padding: "8px 14px", background: t.card }}>
          <div style={{ display: "flex", justifyContent: "space-between", fontSize: 11, color: t.txMuted, marginBottom: 4 }}><span>Manaus</span><span>Juta\u00ed</span></div>
          <Bar value={58} t={t} />
          <div style={{ fontSize: 10, color: t.txMuted, marginTop: 4, textAlign: "center" }}>Atualizado h\u00e1 3 min</div>
        </div>
      </div>}

      {detalhe.descricao && <Cd t={t} style={{ padding: 14 }}><div style={{ fontSize: 13, color: t.txSoft, lineHeight: 1.7 }}>{detalhe.descricao}</div></Cd>}

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
        <Cd t={t} style={{ padding: 12, textAlign: "center" }}>
          <IconUsers size={20} color={t.pri} /><div style={{ fontSize: 18, fontWeight: 700, color: t.pri, marginTop: 4 }}>{detalhe.capacidadePassageiros || "\u2014"}</div><div style={{ fontSize: 11, color: t.txMuted }}>Passageiros</div>
        </Cd>
        <Cd t={t} style={{ padding: 12, textAlign: "center" }}>
          <IconMapPin size={20} color={t.info} /><div style={{ fontSize: 14, fontWeight: 700, color: t.info, marginTop: 4 }}>{emViagem ? "Em viagem" : "No porto"}</div><div style={{ fontSize: 11, color: t.txMuted }}>Status</div>
        </Cd>
      </div>

      {detalhe.horarioSaidaPadrao && <Cd t={t} style={{ padding: 14, border: `1px solid ${t.borderStrong}` }}>
        <div style={{ fontSize: 12, fontWeight: 700, color: t.pri, textTransform: "uppercase", letterSpacing: 1, marginBottom: 8 }}>Hor\u00e1rios</div>
        {detalhe.horarioSaidaPadrao.split("\n").map((line, i) =>
          line.trim() === "" ? <div key={i} style={{ height: 8 }} /> :
          line === line.toUpperCase() && line.includes("\u2192") ? <div key={i} style={{ fontSize: 13, fontWeight: 700, color: t.tx, marginTop: i > 0 ? 4 : 0 }}>{line}</div> :
          <div key={i} style={{ fontSize: 12, color: t.txSoft, lineHeight: 1.6, paddingLeft: 8 }}>{line}</div>
        )}
      </Cd>}

      {detalhe.telefone && <Cd t={t} style={{ padding: 12 }}>
        <div style={{ fontSize: 12, color: t.txMuted }}>Telefone</div><div style={{ fontSize: 14, fontWeight: 600 }}>{detalhe.telefone}</div>
      </Cd>}

      {viagensEmb?.length > 0 && <>
        <div style={{ fontSize: 12, fontWeight: 700, color: t.pri, textTransform: "uppercase", letterSpacing: 1 }}>Pr\u00f3ximas viagens</div>
        {viagensEmb.map(v => <Cd key={v.id} t={t} style={{ padding: 12 }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <div><div style={{ fontSize: 13, fontWeight: 600 }}>{v.origem} \u2192 {v.destino}</div><div style={{ fontSize: 12, color: t.txMuted, marginTop: 2 }}>Sa\u00edda: {fmt(v.dataViagem)} \u2022 Chegada: {fmt(v.previsaoChegada || v.dataChegada)}</div></div>
            <Badge status={v.atual ? "Em viagem" : "Confirmada"} t={t} />
          </div>
        </Cd>)}
      </>}
    </div>;
  }

  return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Embarca\u00e7\u00f5es</h3>
    {(!boats || boats.length === 0) && <Cd t={t} style={{ padding: 16, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Nenhuma embarca\u00e7\u00e3o encontrada.</div></Cd>}
    {boats?.map((b, i) => <Cd key={b.id || i} t={t} style={{ padding: 0, overflow: "hidden" }} onClick={() => setSel(i)}>
      {b.fotoUrl && <img src={`${API}${b.fotoUrl}`} alt={b.nome} style={{ width: "100%", height: 120, objectFit: "cover" }} />}
      <div style={{ padding: 14 }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
          <div><div style={{ fontSize: 15, fontWeight: 600 }}>{b.nome}</div><div style={{ fontSize: 12, color: t.txMuted, marginTop: 2 }}>{b.rotaPrincipal || b.rotaAtual || ""}</div></div>
          <Badge status={b.status || "NO_PORTO"} t={t} />
        </div>
        {b.horarioSaidaPadrao && <div style={{ fontSize: 11, color: t.txMuted, marginTop: 6, lineHeight: 1.5 }}>{b.horarioSaidaPadrao.split("\n").find(l => l.includes("Sa\u00edda de Manaus"))?.trim() || ""}</div>}
        <div style={{ fontSize: 12, color: t.pri, fontWeight: 600, marginTop: 8 }}>Ver detalhes \u2192</div>
      </div>
    </Cd>)}
  </div>;
}

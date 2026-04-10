import { useState } from "react";
import { useApi } from "../api.js";
import { money } from "../helpers.js";
import Badge from "../components/Badge.jsx";
import Cd from "../components/Card.jsx";
import Skeleton from "../components/Skeleton.jsx";
import ErrorRetry from "../components/ErrorRetry.jsx";

export default function EncomendaCPF({ t, authHeaders }) {
  const { data: encomendas, loading, erro, refresh } = useApi("/encomendas/rastreio", authHeaders);
  const [busca, setBusca] = useState("");

  const filtradas = encomendas?.filter(e => {
    if (!busca.trim()) return true;
    const q = busca.toLowerCase();
    return (e.numero_encomenda || "").toLowerCase().includes(q)
      || (e.remetente || "").toLowerCase().includes(q)
      || (e.destinatario || "").toLowerCase().includes(q);
  });

  if (erro) return <ErrorRetry erro={erro} onRetry={refresh} t={t} />;

  return (
    <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 12 }}>
      <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Encomendas</h3>

      <input
        value={busca}
        onChange={e => setBusca(e.target.value)}
        placeholder="Buscar por numero, remetente ou destinatario..."
        className="input-field"
        style={{
          width: "100%", padding: "10px 14px", borderRadius: 10,
          border: `1px solid ${t.border}`, background: t.soft,
          color: t.tx, fontSize: 13, outline: "none", boxSizing: "border-box"
        }}
      />

      {loading ? <Skeleton t={t} height={90} count={3} /> :
        filtradas?.length > 0 ? filtradas.map((e, i) => (
          <Cd key={e.id || i} t={t} style={{ padding: 14 }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 6 }}>
              <span style={{ fontSize: 13, fontWeight: 700, fontFamily: "'Space Mono', monospace", color: t.pri }}>
                {e.numero_encomenda}
              </span>
              <div style={{ display: "flex", gap: 6 }}>
                <Badge status={e.entregue ? "Entregue" : "Pendente"} t={t} />
                <Badge status={e.status_pagamento === "PAGO" ? "Pago" : "Pendente"} t={t} />
              </div>
            </div>
            <div style={{ display: "flex", justifyContent: "space-between", fontSize: 13 }}>
              <div>
                <div style={{ color: t.txSoft }}>
                  <span style={{ fontWeight: 600 }}>De:</span> {e.remetente || "\u2014"}
                </div>
                <div style={{ color: t.txSoft, marginTop: 2 }}>
                  <span style={{ fontWeight: 600 }}>Para:</span> {e.destinatario || "\u2014"}
                </div>
              </div>
              <div style={{ textAlign: "right", fontWeight: 600, color: t.tx, fontSize: 14, alignSelf: "center" }}>
                {money(e.total_a_pagar)}
              </div>
            </div>
          </Cd>
        )) : (
          <Cd t={t} style={{ padding: 16, textAlign: "center" }}>
            <div style={{ fontSize: 13, color: t.txMuted }}>
              {busca ? `Nenhum resultado para "${busca}"` : "Nenhuma encomenda encontrada."}
            </div>
          </Cd>
        )
      }
    </div>
  );
}

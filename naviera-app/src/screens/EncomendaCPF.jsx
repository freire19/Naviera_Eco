import { useState } from "react";
import { API, useApi } from "../api.js";
import { money, calcularDescontoApp } from "../helpers.js";
import Badge from "../components/Badge.jsx";
import Cd from "../components/Card.jsx";
import Skeleton from "../components/Skeleton.jsx";
import ErrorRetry from "../components/ErrorRetry.jsx";
import Toast from "../components/Toast.jsx";
import PagamentoSucesso from "../components/PagamentoSucesso.jsx";
import { useTheme } from "../contexts/ThemeContext.jsx";
import { useAuth } from "../contexts/AuthContext.jsx";
import usePagamento from "../hooks/usePagamento.js";

const FORMAS_PAGAMENTO = [
  { v: "PIX", t: "PIX", s: "10% de desconto" },
  { v: "CARTAO", t: "Cartao", s: "Sem desconto" },
  { v: "BARCO", t: "Pagar no barco", s: "Sem desconto, confirma no embarque" },
];

export default function EncomendaCPF() {
  const { t } = useTheme();
  const { authHeaders } = useAuth();
  const { data: encomendas, loading, erro, refresh } = useApi("/encomendas/rastreio", authHeaders);
  const [busca, setBusca] = useState("");
  const pag = usePagamento(item => `${API}/encomendas/${item.id_encomenda}/pagar`, authHeaders);

  const filtradas = encomendas?.filter(e => {
    if (!busca.trim()) return true;
    const q = busca.toLowerCase();
    return (e.numero_encomenda || "").toLowerCase().includes(q)
      || (e.remetente || "").toLowerCase().includes(q)
      || (e.destinatario || "").toLowerCase().includes(q);
  });

  const confirmarPagamento = () => pag.confirmar(
    { numero: pag.pagando?.numero_encomenda, destinatario: pag.pagando?.destinatario },
    refresh
  );

  if (erro) return <ErrorRetry erro={erro} onRetry={refresh} />;

  if (pag.resultado) return <PagamentoSucesso resultado={pag.resultado}
    toast={pag.toast} onCloseToast={() => pag.setToast(null)} onVoltar={pag.fecharResultado} />;

  if (pag.pagando) {
    const item = pag.pagando;
    const saldo = Math.max(0, (Number(item.total_a_pagar) || 0) - (Number(item.desconto) || 0) - (Number(item.valor_pago) || 0));
    const desconto10 = calcularDescontoApp(saldo, pag.formaPag);
    const aPagar = saldo - desconto10;
    return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 12 }}>
      <button onClick={pag.cancelar} style={{ background: "none", border: "none", color: t.txMuted, fontSize: 13, cursor: "pointer", textAlign: "left", padding: 0 }}>{"< Voltar"}</button>
      <h1 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Pagar encomenda</h1>
      <Cd style={{ padding: 14 }}>
        <div style={{ fontSize: 13, fontWeight: 700, fontFamily: "'Space Mono', monospace", color: t.pri }}>{item.numero_encomenda}</div>
        <div style={{ fontSize: 12, color: t.txMuted, marginTop: 4 }}>De: {item.remetente || "-"}</div>
        <div style={{ fontSize: 12, color: t.txMuted }}>Para: {item.destinatario || "-"}</div>
        {item.embarcacao && <div style={{ fontSize: 12, color: t.txMuted, marginTop: 2 }}>Embarcacao: {item.embarcacao}</div>}
      </Cd>

      <div style={{ fontSize: 14, fontWeight: 600, marginTop: 4 }}>Forma de pagamento</div>
      {FORMAS_PAGAMENTO.map(o => {
        const sel = pag.formaPag === o.v;
        return <Cd key={o.v} style={{ padding: 12, cursor: "pointer", border: `2px solid ${sel ? t.pri : t.border}`, background: sel ? t.accent : t.card }} onClick={() => pag.setFormaPag(o.v)}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <div><div style={{ fontSize: 14, fontWeight: 600 }}>{o.t}</div>
              <div style={{ fontSize: 11, color: t.txMuted, marginTop: 2 }}>{o.s}</div></div>
            {o.v === "PIX" && <div style={{ fontSize: 11, fontWeight: 700, color: t.ok, background: t.okBg, padding: "3px 8px", borderRadius: 8 }}>-10%</div>}
          </div>
        </Cd>;
      })}

      <Cd style={{ padding: 12, background: t.soft, border: `1px dashed ${t.border}` }}>
        <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12, color: t.txMuted }}><span>Saldo</span><span>{money(saldo)}</span></div>
        {desconto10 > 0 && <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12, color: t.ok, marginTop: 4 }}><span>Desconto PIX (10%)</span><span>- {money(desconto10)}</span></div>}
        <div style={{ display: "flex", justifyContent: "space-between", fontSize: 15, fontWeight: 700, marginTop: 8, paddingTop: 8, borderTop: `1px solid ${t.border}` }}><span>Total</span><span style={{ color: t.pri }}>{money(aPagar)}</span></div>
      </Cd>

      {pag.errPag && <div role="alert" style={{ padding: "10px 14px", borderRadius: 10, background: t.errBg, color: t.errTx, fontSize: 12 }}>{pag.errPag}</div>}
      <button onClick={confirmarPagamento} disabled={pag.enviando} className="btn-primary" style={{ width: "100%", padding: "14px 0", background: pag.enviando ? t.txMuted : t.priGrad, color: "#fff", fontSize: 14 }}>
        {pag.enviando ? "Processando..." : pag.formaPag === "BARCO" ? "Reservar para pagar no barco" : `Pagar via ${pag.formaPag === "PIX" ? "PIX" : "cartao"}`}
      </button>
    </div>;
  }

  return (
    <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 12 }}>
      <h1 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Encomendas</h1>

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

      {loading ? <Skeleton height={90} count={3} /> :
        filtradas?.length > 0 ? filtradas.map((e, i) => {
          const pago = e.status_pagamento === "PAGO";
          const aguardando = e.status_pagamento === "PENDENTE_CONFIRMACAO";
          const podeP = !pago && !aguardando && !e.entregue;
          return (
            <Cd key={e.id_encomenda || i} style={{ padding: 14 }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 6 }}>
                <span style={{ fontSize: 13, fontWeight: 700, fontFamily: "'Space Mono', monospace", color: t.pri }}>
                  {e.numero_encomenda}
                </span>
                <div style={{ display: "flex", gap: 6 }}>
                  <Badge status={e.entregue ? "Entregue" : "Pendente"} />
                  <Badge status={pago ? "Pago" : aguardando ? "Aguardando" : "Pendente"} />
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
              {podeP && (
                <button onClick={() => pag.setPagando(e)} className="btn-primary" style={{ marginTop: 10, width: "100%", padding: "10px 0", background: t.priGrad, color: "#fff", fontSize: 13, borderRadius: 10, border: "none", fontWeight: 600, cursor: "pointer", fontFamily: "inherit" }}>
                  Pagar encomenda
                </button>
              )}
              {aguardando && <div style={{ marginTop: 8, fontSize: 11, color: t.warnTx, background: t.warnBg, padding: "6px 10px", borderRadius: 8, textAlign: "center" }}>Aguardando confirmacao de pagamento</div>}
            </Cd>
          );
        }) : (
          <Cd style={{ padding: 16, textAlign: "center" }}>
            <div style={{ fontSize: 13, color: t.txMuted }}>
              {busca ? `Nenhum resultado para "${busca}"` : "Nenhuma encomenda encontrada."}
            </div>
          </Cd>
        )
      }

      {pag.toast && <Toast message={pag.toast} onClose={() => pag.setToast(null)} />}
    </div>
  );
}

import { useState } from "react";
import { API, useApi, authFetch } from "../api.js";
import { fmt, money } from "../helpers.js";
import Badge from "../components/Badge.jsx";
import Cd from "../components/Card.jsx";
import Skeleton from "../components/Skeleton.jsx";
import ErrorRetry from "../components/ErrorRetry.jsx";
import Toast from "../components/Toast.jsx";

export default function FinanceiroCNPJ({ t, authHeaders }) {
  const { data: fretes, loading, erro, refresh } = useApi("/fretes", authHeaders);
  const [pagando, setPagando] = useState(null);
  const [formaPag, setFormaPag] = useState("PIX");
  const [enviando, setEnviando] = useState(false);
  const [errPag, setErrPag] = useState("");
  const [toast, setToast] = useState(null);

  const confirmarPagamento = async () => {
    if (!pagando) return;
    setErrPag(""); setEnviando(true);
    try {
      const res = await authFetch(`${API}/fretes/${pagando.id}/pagar`, {
        method: "POST", headers: authHeaders,
        body: JSON.stringify({ formaPagamento: formaPag }),
      });
      const data = await res.json();
      if (!res.ok) { setErrPag(data.erro || data.message || "Erro ao pagar."); return; }
      const msgs = { PIX: "Pagamento enviado. Aguardando confirmacao.", CARTAO: "Pagamento enviado. Aguardando confirmacao.", BOLETO: "Boleto sera enviado. Aguardando confirmacao.", BARCO: "Reservado para pagar no embarque." };
      setToast(msgs[formaPag] || "Pagamento registrado");
      setPagando(null); setFormaPag("PIX"); refresh();
    } catch { setErrPag("Erro de conexao."); } finally { setEnviando(false); }
  };

  if (loading) return <Skeleton t={t} height={70} count={4} />;
  if (erro) return <ErrorRetry erro={erro} onRetry={refresh} t={t} />;

  // Modal de pagamento
  if (pagando) {
    const saldo = Math.max(0, (Number(pagando.valorNominal) || 0) - (Number(pagando.valorPago) || 0));
    const desconto10 = formaPag === "PIX" ? saldo * 0.10 : 0;
    const aPagar = saldo - desconto10;
    const opts = [
      { v: "PIX", t: "PIX", s: "10% de desconto" },
      { v: "CARTAO", t: "Cartao", s: "Sem desconto" },
      { v: "BOLETO", t: "Boleto", s: "Sem desconto, link via email" },
      { v: "BARCO", t: "Pagar no barco", s: "Sem desconto, no embarque" },
    ];
    return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 12 }}>
      <button onClick={() => { setPagando(null); setErrPag(""); setFormaPag("PIX"); }} style={{ background: "none", border: "none", color: t.txMuted, fontSize: 13, cursor: "pointer", textAlign: "left", padding: 0 }}>{"< Voltar"}</button>
      <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Pagar frete</h3>
      <Cd t={t} style={{ padding: 14 }}>
        <div style={{ fontSize: 13, fontWeight: 700, fontFamily: "'Space Mono', monospace", color: t.pri }}>FRT-{pagando.numeroFrete || pagando.id}</div>
        <div style={{ fontSize: 12, color: t.txMuted, marginTop: 4 }}>Para: {pagando.destinatario || "-"}</div>
        <div style={{ fontSize: 12, color: t.txMuted }}>Embarcacao: {pagando.embarcacao || "-"}</div>
        {pagando.dataViagem && <div style={{ fontSize: 12, color: t.txMuted, marginTop: 2 }}>Viagem: {fmt(pagando.dataViagem)}</div>}
      </Cd>

      <div style={{ fontSize: 14, fontWeight: 600, marginTop: 4 }}>Forma de pagamento</div>
      {opts.map(o => {
        const sel = formaPag === o.v;
        return <Cd key={o.v} t={t} style={{ padding: 12, cursor: "pointer", border: `2px solid ${sel ? t.pri : t.border}`, background: sel ? t.accent : t.card }} onClick={() => setFormaPag(o.v)}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <div><div style={{ fontSize: 14, fontWeight: 600 }}>{o.t}</div>
              <div style={{ fontSize: 11, color: t.txMuted, marginTop: 2 }}>{o.s}</div></div>
            {o.v === "PIX" && <div style={{ fontSize: 11, fontWeight: 700, color: t.ok, background: t.okBg, padding: "3px 8px", borderRadius: 8 }}>-10%</div>}
          </div>
        </Cd>;
      })}

      <Cd t={t} style={{ padding: 12, background: t.soft, border: `1px dashed ${t.border}` }}>
        <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12, color: t.txMuted }}><span>Saldo</span><span>{money(saldo)}</span></div>
        {desconto10 > 0 && <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12, color: t.ok, marginTop: 4 }}><span>Desconto PIX (10%)</span><span>- {money(desconto10)}</span></div>}
        <div style={{ display: "flex", justifyContent: "space-between", fontSize: 15, fontWeight: 700, marginTop: 8, paddingTop: 8, borderTop: `1px solid ${t.border}` }}><span>Total</span><span style={{ color: t.pri }}>{money(aPagar)}</span></div>
      </Cd>

      {errPag && <div style={{ padding: "10px 14px", borderRadius: 10, background: t.errBg, color: t.errTx, fontSize: 12 }}>{errPag}</div>}
      <button onClick={confirmarPagamento} disabled={enviando} className="btn-primary" style={{ width: "100%", padding: "14px 0", background: enviando ? t.txMuted : t.priGrad, color: "#fff", fontSize: 14 }}>
        {enviando ? "Processando..." : formaPag === "BARCO" ? "Reservar para pagar no barco" : formaPag === "BOLETO" ? "Gerar boleto" : `Pagar via ${formaPag === "PIX" ? "PIX" : "cartao"}`}
      </button>
    </div>;
  }

  // Agrupar fretes por embarcacao
  const grupos = (fretes || []).reduce((acc, f) => {
    const key = f.embarcacao || "Sem embarcacao";
    if (!acc[key]) acc[key] = { embarcacao: key, fretes: [], pendente: 0, pago: 0 };
    acc[key].fretes.push(f);
    acc[key].pendente += Number(f.valorDevedor) || 0;
    acc[key].pago += Number(f.valorPago) || 0;
    return acc;
  }, {});
  const listaGrupos = Object.values(grupos).sort((a, b) => b.pendente - a.pendente);

  const totalPendente = listaGrupos.reduce((s, g) => s + g.pendente, 0);
  const totalPago = listaGrupos.reduce((s, g) => s + g.pago, 0);

  return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Financeiro</h3>

    <Cd t={t} style={{ padding: 16 }}>
      <div style={{ fontSize: 12, color: t.txMuted, marginBottom: 4 }}>Total pendente</div>
      <div style={{ fontSize: 28, fontWeight: 700, color: totalPendente > 0 ? t.err : t.ok }}>{money(totalPendente)}</div>
      <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12, color: t.txMuted, marginTop: 6 }}>
        <span>{listaGrupos.length} embarcacao(oes)</span>
        <span>Pago: {money(totalPago)}</span>
      </div>
    </Cd>

    {listaGrupos.length === 0 && <Cd t={t} style={{ padding: 16, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Nenhum frete encontrado.</div></Cd>}

    {listaGrupos.map(g => (
      <div key={g.embarcacao} style={{ display: "flex", flexDirection: "column", gap: 8 }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: 8, padding: "6px 12px", background: t.soft, borderRadius: 8 }}>
          <span style={{ fontSize: 13, fontWeight: 700, color: t.tx }}>{g.embarcacao}</span>
          <span style={{ fontSize: 12, color: g.pendente > 0 ? t.err : t.ok, fontWeight: 600 }}>{money(g.pendente)} pendente</span>
        </div>
        {g.fretes.map(f => {
          const pago = f.statusPagamento === "PAGO" || (Number(f.valorDevedor) || 0) <= 0;
          const aguardando = f.statusPagamento === "PENDENTE_CONFIRMACAO";
          const podeP = !pago && !aguardando;
          return (
            <Cd key={f.id} t={t} style={{ padding: 12 }}>
              <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 4 }}>
                <span style={{ fontSize: 12, fontWeight: 700, fontFamily: "'Space Mono', monospace", color: t.txSoft }}>FRT-{f.numeroFrete || f.id}</span>
                <Badge status={pago ? "Pago" : aguardando ? "Aguardando" : "Pendente"} t={t} />
              </div>
              <div style={{ fontSize: 13 }}>{f.destinatario}</div>
              {f.dataViagem && <div style={{ fontSize: 11, color: t.txMuted, marginTop: 2 }}>{fmt(f.dataViagem)}</div>}
              <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12, marginTop: 6 }}>
                <span style={{ color: t.txMuted }}>Total {money(f.valorNominal)}</span>
                <span style={{ fontWeight: 700, color: !pago ? t.err : t.tx }}>{money(f.valorDevedor)}</span>
              </div>
              {podeP && (
                <button onClick={() => setPagando(f)} className="btn-primary" style={{ marginTop: 10, width: "100%", padding: "10px 0", background: t.priGrad, color: "#fff", fontSize: 13, borderRadius: 10, border: "none", fontWeight: 600, cursor: "pointer", fontFamily: "inherit" }}>
                  Pagar frete
                </button>
              )}
              {aguardando && <div style={{ marginTop: 8, fontSize: 11, color: t.warnTx, background: t.warnBg, padding: "6px 10px", borderRadius: 8, textAlign: "center" }}>Aguardando confirmacao {f.formaPagamentoApp ? `(${f.formaPagamentoApp})` : ""}</div>}
            </Cd>
          );
        })}
      </div>
    ))}

    {toast && <Toast message={toast} t={t} onClose={() => setToast(null)} />}
  </div>;
}

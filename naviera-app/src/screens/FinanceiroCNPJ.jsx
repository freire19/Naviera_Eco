import { API, useApi } from "../api.js";
import { fmt, money, calcularDescontoApp } from "../helpers.js";
import Badge from "../components/Badge.jsx";
import Cd from "../components/Card.jsx";
import Skeleton from "../components/Skeleton.jsx";
import ErrorRetry from "../components/ErrorRetry.jsx";
import Toast from "../components/Toast.jsx";
import PagamentoSucesso from "../components/PagamentoSucesso.jsx";
import { useTheme } from "../contexts/ThemeContext.jsx";
import { useAuth } from "../contexts/AuthContext.jsx";
import usePagamento from "../hooks/usePagamento.js";

// #DP082: opts em escopo de modulo — antes recriava o array a cada render do PagandoView,
//   forçando React a tratar Cd elements como nova prop reference.
const FORMAS_PAGAMENTO = [
  { v: "PIX", t: "PIX", s: "10% de desconto" },
  { v: "CARTAO", t: "Cartao", s: "Sem desconto" },
  { v: "BOLETO", t: "Boleto", s: "Sem desconto, link via email" },
  { v: "BARCO", t: "Pagar no barco", s: "Sem desconto, no embarque" },
];

export default function FinanceiroCNPJ() {
  const { t } = useTheme();
  const { authHeaders } = useAuth();
  const { data: fretes, loading, erro, refresh } = useApi("/fretes", authHeaders);
  const pag = usePagamento(item => `${API}/fretes/${item.id}/pagar`, authHeaders);

  const confirmarPagamento = () => pag.confirmar(
    { numero: `FRT-${pag.pagando?.numeroFrete || pag.pagando?.id}`, destinatario: pag.pagando?.destinatario, embarcacao: pag.pagando?.embarcacao },
    refresh
  );

  if (loading) return <Skeleton height={70} count={4} />;
  if (erro) return <ErrorRetry erro={erro} onRetry={refresh} />;

  if (pag.resultado) return <PagamentoSucesso resultado={pag.resultado}
    toast={pag.toast} onCloseToast={() => pag.setToast(null)} onVoltar={pag.fecharResultado} />;

  if (pag.pagando) {
    const item = pag.pagando;
    // #DB220: usar valorDevedor (ja subtrai desconto aplicado pela API) — evita cobrar a mais
    const saldo = item.valorDevedor != null
      ? Math.max(0, Number(item.valorDevedor))
      : Math.max(0, (Number(item.valorNominal) || 0) - (Number(item.valorPago) || 0));
    const desconto10 = calcularDescontoApp(saldo, pag.formaPag);
    const aPagar = saldo - desconto10;
    return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 12 }}>
      <button onClick={pag.cancelar} style={{ background: "none", border: "none", color: t.txMuted, fontSize: 13, cursor: "pointer", textAlign: "left", padding: 0 }}>{"< Voltar"}</button>
      <h1 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Pagar frete</h1>
      <Cd style={{ padding: 14 }}>
        <div style={{ fontSize: 13, fontWeight: 700, fontFamily: "'Space Mono', monospace", color: t.pri }}>FRT-{item.numeroFrete || item.id}</div>
        <div style={{ fontSize: 12, color: t.txMuted, marginTop: 4 }}>Para: {item.destinatario || "-"}</div>
        <div style={{ fontSize: 12, color: t.txMuted }}>Embarcacao: {item.embarcacao || "-"}</div>
        {item.dataViagem && <div style={{ fontSize: 12, color: t.txMuted, marginTop: 2 }}>Viagem: {fmt(item.dataViagem)}</div>}
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
        {pag.enviando ? "Processando..." : pag.formaPag === "BARCO" ? "Reservar para pagar no barco" : pag.formaPag === "BOLETO" ? "Gerar boleto" : `Pagar via ${pag.formaPag === "PIX" ? "PIX" : "cartao"}`}
      </button>
    </div>;
  }

  // #232: agrupar por empresa quando disponivel (CNPJ pode ter fretes em empresas diferentes
  // que compartilham nomes de embarcacao). Fallback para embarcacao se empresa_nome ausente.
  const grupos = (fretes || []).reduce((acc, f) => {
    const empresaNome = f.empresaNome || f.empresa_nome;
    const key = empresaNome || f.embarcacao || "Sem empresa";
    if (!acc[key]) acc[key] = { titulo: key, embarcacao: key, fretes: [], pendente: 0, pago: 0 };
    acc[key].fretes.push(f);
    acc[key].pendente += Number(f.valorDevedor) || 0;
    acc[key].pago += Number(f.valorPago) || 0;
    return acc;
  }, {});
  const listaGrupos = Object.values(grupos).sort((a, b) => b.pendente - a.pendente);

  const totalPendente = listaGrupos.reduce((s, g) => s + g.pendente, 0);
  const totalPago = listaGrupos.reduce((s, g) => s + g.pago, 0);

  return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <h1 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Financeiro</h1>

    <Cd style={{ padding: 16 }}>
      <div style={{ fontSize: 12, color: t.txMuted, marginBottom: 4 }}>Total pendente</div>
      <div style={{ fontSize: 28, fontWeight: 700, color: totalPendente > 0 ? t.err : t.ok }}>{money(totalPendente)}</div>
      <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12, color: t.txMuted, marginTop: 6 }}>
        <span>{listaGrupos.length} embarcacao(oes)</span>
        <span>Pago: {money(totalPago)}</span>
      </div>
    </Cd>

    {listaGrupos.length === 0 && <Cd style={{ padding: 16, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Nenhum frete encontrado.</div></Cd>}

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
            <Cd key={f.id} style={{ padding: 12 }}>
              <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 4 }}>
                <span style={{ fontSize: 12, fontWeight: 700, fontFamily: "'Space Mono', monospace", color: t.txSoft }}>FRT-{f.numeroFrete || f.id}</span>
                <Badge status={pago ? "Pago" : aguardando ? "Aguardando" : "Pendente"} />
              </div>
              <div style={{ fontSize: 13 }}>{f.destinatario}</div>
              {f.dataViagem && <div style={{ fontSize: 11, color: t.txMuted, marginTop: 2 }}>{fmt(f.dataViagem)}</div>}
              <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12, marginTop: 6 }}>
                <span style={{ color: t.txMuted }}>Total {money(f.valorNominal)}</span>
                <span style={{ fontWeight: 700, color: !pago ? t.err : t.tx }}>{money(f.valorDevedor)}</span>
              </div>
              {podeP && (
                <button onClick={() => pag.setPagando(f)} className="btn-primary" style={{ marginTop: 10, width: "100%", padding: "10px 0", background: t.priGrad, color: "#fff", fontSize: 13, borderRadius: 10, border: "none", fontWeight: 600, cursor: "pointer", fontFamily: "inherit" }}>
                  Pagar frete
                </button>
              )}
              {aguardando && <div style={{ marginTop: 8, fontSize: 11, color: t.warnTx, background: t.warnBg, padding: "6px 10px", borderRadius: 8, textAlign: "center" }}>Aguardando confirmacao {f.formaPagamentoApp ? `(${f.formaPagamentoApp})` : ""}</div>}
            </Cd>
          );
        })}
      </div>
    ))}

    {pag.toast && <Toast message={pag.toast} onClose={() => pag.setToast(null)} />}
  </div>;
}

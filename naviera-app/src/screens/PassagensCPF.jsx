import { useState } from "react";
import { API, useApi, authFetch } from "../api.js";
import { fmt, money, lerRespostaJson } from "../helpers.js";
import { IconBack, IconCheck, IconCalendar } from "../icons.jsx";
import Badge from "../components/Badge.jsx";
import Cd from "../components/Card.jsx";
import Skeleton from "../components/Skeleton.jsx";
import ErrorRetry from "../components/ErrorRetry.jsx";
import Toast from "../components/Toast.jsx";
import PagamentoArtefato from "../components/PagamentoArtefato.jsx";
import BilheteScreen from "./BilheteScreen.jsx";
import { useTheme } from "../contexts/ThemeContext.jsx";
import { useAuth } from "../contexts/AuthContext.jsx";

export default function PassagensCPF() {
  const { t } = useTheme();
  const { authHeaders } = useAuth();
  const { data: embarcacoes, loading: le } = useApi("/embarcacoes", authHeaders);
  const { data: viagens, loading: lv, erro: ev, refresh: rv } = useApi("/viagens/ativas", authHeaders);
  const { data: tarifas } = useApi("/tarifas", authHeaders);
  const { data: minhas, refresh: rm } = useApi("/passagens", authHeaders);
  const [busca, setBusca] = useState("");
  const [selEmb, setSelEmb] = useState(null);
  const [compra, setCompra] = useState(null);
  const [tipoSel, setTipoSel] = useState(null);
  const [formaPag, setFormaPag] = useState("PIX");
  const [comprando, setComprando] = useState(false);
  const [resultado, setResultado] = useState(null);
  const [erro, setErro] = useState("");
  const [toast, setToast] = useState(null);
  const [selBilhete, setSelBilhete] = useState(null);

  // #209 #220: usar data-local (sv-SE = YYYY-MM-DD) evita drift UTC no fim do dia BR.
  const hoje = new Date().toLocaleDateString('sv-SE');
  const viagensEmb = selEmb ? viagens?.filter(v => v.embarcacao === selEmb.nome && v.dataViagem >= hoje && !v.atual) || [] : [];
  const tarifasDaViagem = compra ? tarifas?.filter(x => x.origem === compra.origem && x.destino === compra.destino) || [] : [];
  const embFiltradas = busca.trim() ? embarcacoes?.filter(e => e.nome.toLowerCase().includes(busca.toLowerCase()) || (e.rotaPrincipal || "").toLowerCase().includes(busca.toLowerCase())) : embarcacoes;

  const confirmarCompra = async () => {
    if (!tipoSel) { setErro("Selecione o tipo de passagem."); return; }
    setErro(""); setComprando(true);
    try {
      // #231: fallback para id_viagem quando DTO usar snake_case
      const viagemIdCompra = compra.id ?? compra.id_viagem ?? compra.idViagem;
      const res = await authFetch(`${API}/passagens/comprar`, { method: "POST", headers: authHeaders, body: JSON.stringify({ idViagem: viagemIdCompra, idTipoPassagem: tipoSel, formaPagamento: formaPag }) });
      const { raw, data } = await lerRespostaJson(res);
      if (!res.ok) {
        setErro(data?.erro || (raw && raw.slice(0, 120).trim()) || `HTTP ${res.status}`);
        return;
      }
      setResultado(data); rm();
    } catch (e) {
      console.warn("[PassagensCPF] confirmarCompra falhou:", e?.message);
      setErro("Sem conexao com o servidor.");
    } finally { setComprando(false); }
  };

  if (ev) return <ErrorRetry erro={ev} onRetry={rv} />;

  // Tela do bilhete digital
  if (selBilhete) return <BilheteScreen bilhete={selBilhete} onBack={() => setSelBilhete(null)} />;

  // Tela de sucesso
  if (resultado) return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 16, alignItems: "center", padding: "40px 0" }}>
    <div style={{ width: 64, height: 64, borderRadius: "50%", background: t.okBg, display: "flex", alignItems: "center", justifyContent: "center" }}>
      <IconCheck size={32} color={t.ok} />
    </div>
    <h3 style={{ margin: 0, fontSize: 20, fontWeight: 700 }}>Passagem emitida!</h3>
    <Cd style={{ padding: 16, width: "100%", textAlign: "center" }}>
      <div style={{ fontSize: 12, color: t.txMuted }}>Bilhete</div>
      <div style={{ fontSize: 18, fontWeight: 700, fontFamily: "'Space Mono', monospace", color: t.pri, marginTop: 4 }}>{resultado.numeroBilhete}</div>
      {Number(resultado.descontoApp) > 0 && <div style={{ fontSize: 11, color: t.ok, marginTop: 8 }}>Desconto PIX: - {money(resultado.descontoApp)}</div>}
      <div style={{ fontSize: 14, fontWeight: 600, marginTop: 4 }}>{money(resultado.valorAPagar ?? resultado.valorTotal)}</div>
      <div style={{ padding: "6px 12px", borderRadius: 8, background: t.warnBg, color: t.warnTx, fontSize: 11, marginTop: 8, display: "inline-block" }}>{resultado.formaPagamento === "BARCO" ? "Pagamento no embarque" : "Aguardando confirmacao"}</div>
      <div style={{ fontSize: 11, color: t.txMuted, marginTop: 8, lineHeight: 1.5 }}>{resultado.mensagem || "Apos confirmacao, seu bilhete digital ficara ativo."}</div>
    </Cd>
    <PagamentoArtefato formaPagamento={resultado.formaPagamento}
      qrCodePayload={resultado.qrCodePayload} qrCodeImageUrl={resultado.qrCodeImageUrl}
      linhaDigitavel={resultado.linhaDigitavel} boletoUrl={resultado.boletoUrl}
      checkoutUrl={resultado.checkoutUrl} />
    <button onClick={() => {
      // #600: id_passagem obrigatorio — BilheteScreen chama /passagens/:id/totp sem ele.
      // totp_secret fica vazio aqui; cliente vai buscar o secret do servidor pelo id_passagem.
      setSelBilhete({ id_passagem: resultado.idPassagem, numero_bilhete: resultado.numeroBilhete, valor_total: resultado.valorTotal, status_passagem: resultado.status, embarcacao: compra?.embarcacao, origem: compra?.origem, destino: compra?.destino, data_viagem: compra?.dataViagem });
      setResultado(null); setCompra(null); setSelEmb(null); setTipoSel(null); setFormaPag("PIX");
    }} className="btn-primary" style={{ width: "100%", padding: "14px 0", background: t.priGrad, color: "#fff", fontSize: 14 }}>Ver bilhete digital</button>
    <button onClick={() => { setResultado(null); setCompra(null); setSelEmb(null); setTipoSel(null); setFormaPag("PIX"); }} style={{ width: "100%", padding: "12px 0", borderRadius: 12, border: `1px solid ${t.border}`, background: "transparent", color: t.txMuted, fontSize: 13, fontWeight: 600, cursor: "pointer", fontFamily: "inherit" }}>Voltar para passagens</button>
  </div>;

  // Tela de compra
  if (compra) return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <button onClick={() => { setCompra(null); setTipoSel(null); setErro(""); }} style={{ background: "none", border: "none", color: t.txMuted, fontSize: 13, cursor: "pointer", textAlign: "left", padding: 0, display: "flex", alignItems: "center", gap: 4 }}><IconBack size={14} color={t.txMuted} /> Voltar</button>
    <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Comprar passagem</h3>
    <Cd style={{ padding: 14, border: `1px solid ${t.borderStrong}` }}>
      <div style={{ fontSize: 16, fontWeight: 600 }}>{compra.embarcacao}</div>
      <div style={{ fontSize: 13, color: t.txMuted, marginTop: 2 }}>{compra.origem} \u2192 {compra.destino}</div>
      <div style={{ display: "flex", gap: 16, fontSize: 12, color: t.txMuted, marginTop: 6 }}>
        <span style={{ display: "flex", alignItems: "center", gap: 4 }}><IconCalendar size={12} color={t.txMuted} /> {fmt(compra.dataViagem)}</span>
        <span>Chegada: {fmt(compra.dataChegada)}</span>
      </div>
    </Cd>
    <div style={{ fontSize: 14, fontWeight: 600, marginTop: 4 }}>Escolha o tipo</div>
    {tarifasDaViagem.length > 0 ? tarifasDaViagem.map((x, i) => {
      const total = Math.max(0, (Number(x.valor_transporte) || 0) + (Number(x.valor_alimentacao) || 0) - (Number(x.valor_desconto) || 0));
      const selected = tipoSel === x.tipo_passageiro_id;
      return <Cd key={i} style={{ padding: 14, cursor: "pointer", border: `2px solid ${selected ? t.pri : t.border}`, background: selected ? t.accent : t.card }} onClick={() => setTipoSel(x.tipo_passageiro_id)}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <div><div style={{ fontSize: 14, fontWeight: 600 }}>{x.tipo_passageiro}</div>
            <div style={{ fontSize: 11, color: t.txMuted, marginTop: 2 }}>Transporte + Alimentacao{Number(x.valor_desconto) > 0 ? " - Desconto" : ""}</div></div>
          <div style={{ fontSize: 20, fontWeight: 700, color: selected ? t.pri : t.tx }}>{money(total)}</div>
        </div>
      </Cd>;
    }) : <Cd style={{ padding: 14, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Nenhuma tarifa disponivel para esta rota.</div></Cd>}
    {tipoSel && (() => {
      const tarifa = tarifasDaViagem.find(x => x.tipo_passageiro_id === tipoSel);
      const valorTotal = tarifa ? Math.max(0, (Number(tarifa.valor_transporte) || 0) + (Number(tarifa.valor_alimentacao) || 0) - (Number(tarifa.valor_desconto) || 0)) : 0;
      const desconto10 = formaPag === "PIX" ? valorTotal * 0.10 : 0;
      const aPagar = valorTotal - desconto10;
      const opts = [
        { v: "PIX", t: "PIX", s: "10% de desconto" },
        { v: "CARTAO", t: "Cartao", s: "Sem desconto" },
        { v: "BARCO", t: "Pagar no barco", s: "Sem desconto, confirma no embarque" },
      ];
      return <>
        <div style={{ fontSize: 14, fontWeight: 600, marginTop: 4 }}>Forma de pagamento</div>
        {opts.map(o => {
          const sel = formaPag === o.v;
          return <Cd key={o.v} style={{ padding: 12, cursor: "pointer", border: `2px solid ${sel ? t.pri : t.border}`, background: sel ? t.accent : t.card }} onClick={() => setFormaPag(o.v)}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <div><div style={{ fontSize: 14, fontWeight: 600 }}>{o.t}</div>
                <div style={{ fontSize: 11, color: t.txMuted, marginTop: 2 }}>{o.s}</div></div>
              {o.v === "PIX" && <div style={{ fontSize: 11, fontWeight: 700, color: t.ok, background: t.okBg, padding: "3px 8px", borderRadius: 8 }}>-10%</div>}
            </div>
          </Cd>;
        })}
        <Cd style={{ padding: 12, background: t.soft, border: `1px dashed ${t.border}` }}>
          <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12, color: t.txMuted }}><span>Subtotal</span><span>{money(valorTotal)}</span></div>
          {desconto10 > 0 && <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12, color: t.ok, marginTop: 4 }}><span>Desconto PIX (10%)</span><span>- {money(desconto10)}</span></div>}
          <div style={{ display: "flex", justifyContent: "space-between", fontSize: 15, fontWeight: 700, marginTop: 8, paddingTop: 8, borderTop: `1px solid ${t.border}` }}><span>Total</span><span style={{ color: t.pri }}>{money(aPagar)}</span></div>
        </Cd>
      </>;
    })()}
    {erro && <div role="alert" style={{ padding: "10px 14px", borderRadius: 10, background: t.errBg, color: t.errTx, fontSize: 12 }}>{erro}</div>}
    {tarifasDaViagem.length > 0 && <button onClick={confirmarCompra} disabled={comprando || !tipoSel} className="btn-primary" style={{ width: "100%", padding: "14px 0", background: comprando || !tipoSel ? t.txMuted : t.priGrad, color: "#fff", fontSize: 14, opacity: !tipoSel ? 0.5 : 1 }}>{comprando ? "Processando..." : formaPag === "BARCO" ? "Reservar para pagar no barco" : `Confirmar e pagar via ${formaPag === "PIX" ? "PIX" : "cartao"}`}</button>}
  </div>;

  // Tela de viagens de uma embarcacao
  if (selEmb) return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <button onClick={() => setSelEmb(null)} style={{ background: "none", border: "none", color: t.txMuted, fontSize: 13, cursor: "pointer", textAlign: "left", padding: 0, display: "flex", alignItems: "center", gap: 4 }}><IconBack size={14} color={t.txMuted} /> Voltar</button>
    {selEmb.fotoUrl && <img src={`${API}${selEmb.fotoUrl}`} alt={selEmb.nome} style={{ width: "100%", borderRadius: 14, objectFit: "cover", maxHeight: 160 }} />}
    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
      <div><h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>{selEmb.nome}</h3>
        <div style={{ fontSize: 12, color: t.txMuted, marginTop: 2 }}>{selEmb.rotaPrincipal || selEmb.rotaAtual || ""}</div></div>
      <Badge status={selEmb.status || "NO_PORTO"} />
    </div>
    <div style={{ fontSize: 14, fontWeight: 600, marginTop: 4 }}>Proximas saidas</div>
    {lv ? <Skeleton height={70} count={2} /> :
    viagensEmb.length > 0 ? viagensEmb.map(v => <Cd key={v.id} style={{ padding: 14, cursor: "pointer" }} onClick={() => setCompra(v)}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <div><div style={{ fontSize: 14, fontWeight: 600 }}>{v.origem} \u2192 {v.destino}</div>
          <div style={{ display: "flex", gap: 12, fontSize: 12, color: t.txMuted, marginTop: 4 }}>
            <span style={{ display: "flex", alignItems: "center", gap: 4 }}><IconCalendar size={11} color={t.txMuted} /> {fmt(v.dataViagem)}</span>
            <span>\u2192 {fmt(v.dataChegada)}</span>
          </div></div>
        <div style={{ fontSize: 12, color: t.pri, fontWeight: 600 }}>Comprar \u2192</div>
      </div>
    </Cd>) : <Cd style={{ padding: 14, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Nenhuma viagem futura para esta embarcacao.</div></Cd>}
  </div>;

  // Tela principal
  return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Passagens</h3>

    {minhas?.length > 0 && <>
      <div style={{ fontSize: 13, fontWeight: 600, color: t.txSoft }}>Minhas passagens</div>
      {minhas.map((p, i) => <Cd key={i} style={{ padding: 12, borderLeft: `3px solid ${t.pri}`, borderRadius: "0 14px 14px 0", cursor: "pointer" }} onClick={() => setSelBilhete(p)}>
        <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 4 }}>
          <span style={{ fontSize: 11, fontWeight: 700, fontFamily: "'Space Mono', monospace", color: t.pri }}>{p.numero_bilhete}</span>
          <Badge status={p.status_passagem || "CONFIRMADA"} />
        </div>
        <div style={{ fontSize: 14, fontWeight: 600 }}>{p.embarcacao}</div>
        <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12, color: t.txMuted, marginTop: 4 }}>
          <span>{p.origem} \u2192 {p.destino} \u2022 {p.tipo || "Rede"} \u2022 {fmt(p.data_viagem)}</span>
          <span style={{ fontWeight: 600, color: t.tx }}>{money(p.valor_a_pagar)}</span>
        </div>
        <div style={{ fontSize: 11, color: t.pri, fontWeight: 600, marginTop: 6 }}>Abrir bilhete \u2192</div>
      </Cd>)}
    </>}

    <div style={{ fontSize: 13, fontWeight: 600, color: t.txSoft, marginTop: minhas?.length > 0 ? 4 : 0 }}>Comprar passagem</div>
    <input value={busca} onChange={e => setBusca(e.target.value)} placeholder="Buscar embarcacao ou rota..." className="input-field" style={{ width: "100%", padding: "10px 14px", borderRadius: 10, border: `1px solid ${t.border}`, background: t.soft, color: t.tx, fontSize: 13, outline: "none", boxSizing: "border-box" }} />
    {le ? <Skeleton height={80} count={2} /> :
    embFiltradas?.length > 0 ? embFiltradas.map(e => {
      const proxViagem = viagens?.find(v => v.embarcacao === e.nome && v.dataViagem >= hoje && !v.atual);
      return <Cd key={e.id} style={{ padding: 0, overflow: "hidden", cursor: "pointer" }} onClick={() => setSelEmb(e)}>
        {e.fotoUrl && <img src={`${API}${e.fotoUrl}`} alt={e.nome} style={{ width: "100%", height: 100, objectFit: "cover" }} />}
        <div style={{ padding: 12 }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
            <div><div style={{ fontSize: 15, fontWeight: 600 }}>{e.nome}</div>
              <div style={{ fontSize: 12, color: t.txMuted, marginTop: 2 }}>{e.rotaPrincipal || e.rotaAtual || ""}</div></div>
            <Badge status={e.status || "NO_PORTO"} />
          </div>
          {proxViagem ? <div style={{ display: "flex", alignItems: "center", gap: 4, fontSize: 11, color: t.pri, fontWeight: 600, marginTop: 6 }}><IconCalendar size={11} color={t.pri} /> Proxima: {fmt(proxViagem.dataViagem)}</div>
            : <div style={{ fontSize: 11, color: t.txMuted, marginTop: 6 }}>Sem viagens futuras</div>}
          <div style={{ fontSize: 12, color: t.pri, fontWeight: 600, marginTop: 6 }}>Ver datas e comprar \u2192</div>
        </div>
      </Cd>;
    }) : <Cd style={{ padding: 16, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>{busca ? `Nenhum resultado para "${busca}"` : "Nenhuma embarcacao cadastrada."}</div></Cd>}

    {toast && <Toast message={toast} onClose={() => setToast(null)} />}
  </div>;
}

import { money } from "../helpers.js";
import Cd from "./Card.jsx";
import PagamentoArtefato from "./PagamentoArtefato.jsx";
import Toast from "./Toast.jsx";
import { useTheme } from "../contexts/ThemeContext.jsx";

/**
 * Tela de sucesso compartilhada entre Encomendas (CPF) e Fretes (CNPJ).
 * resultado deve trazer: numero, destinatario?, embarcacao?, valorAPagar, descontoApp,
 * formaPagamento, qrCodePayload, qrCodeImageUrl, linhaDigitavel, boletoUrl, checkoutUrl.
 */
export default function PagamentoSucesso({ resultado, toast, onCloseToast, onVoltar }) {
  const { t } = useTheme();
  return (
    <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 12 }}>
      <button onClick={onVoltar} style={{ background: "none", border: "none", color: t.txMuted, fontSize: 13, cursor: "pointer", textAlign: "left", padding: 0 }}>{"< Voltar"}</button>
      <h1 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Pagamento gerado</h1>
      <Cd style={{ padding: 14 }}>
        <div style={{ fontSize: 13, fontWeight: 700, fontFamily: "'Space Mono', monospace", color: t.pri }}>{resultado.numero}</div>
        {resultado.destinatario && <div style={{ fontSize: 12, color: t.txMuted, marginTop: 4 }}>Para: {resultado.destinatario}</div>}
        {resultado.embarcacao && <div style={{ fontSize: 12, color: t.txMuted }}>Embarcacao: {resultado.embarcacao}</div>}
        <div style={{ fontSize: 16, fontWeight: 700, marginTop: 8 }}>{money(resultado.valorAPagar)}</div>
        {Number(resultado.descontoApp) > 0 && <div style={{ fontSize: 11, color: t.ok, marginTop: 2 }}>Desconto PIX: -{money(resultado.descontoApp)}</div>}
      </Cd>
      <PagamentoArtefato formaPagamento={resultado.formaPagamento}
        qrCodePayload={resultado.qrCodePayload} qrCodeImageUrl={resultado.qrCodeImageUrl}
        linhaDigitavel={resultado.linhaDigitavel} boletoUrl={resultado.boletoUrl}
        checkoutUrl={resultado.checkoutUrl} />
      {toast && <Toast message={toast} onClose={onCloseToast} />}
    </div>
  );
}

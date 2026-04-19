import { useState } from "react";
import Cd from "./Card.jsx";

/**
 * Mostra o artefato de pagamento retornado pelo PSP (Asaas):
 *   - PIX   : QR Code (imagem base64) + copia-e-cola
 *   - BOLETO: linha digitavel + botao pra abrir PDF + copiar codigo
 *   - CARTAO: botao abre checkoutUrl (pagina Asaas)
 *
 * Props:
 *   formaPagamento: "PIX" | "CARTAO" | "BOLETO"
 *   qrCodePayload / qrCodeImageUrl : strings (PIX)
 *   linhaDigitavel / boletoUrl     : strings (BOLETO)
 *   checkoutUrl                    : string (CARTAO)
 *   t : theme
 */
export default function PagamentoArtefato({ formaPagamento, qrCodePayload, qrCodeImageUrl, linhaDigitavel, boletoUrl, checkoutUrl, t }) {
  const [copiado, setCopiado] = useState("");
  const copiar = (txt, label) => {
    if (!txt) return;
    navigator.clipboard?.writeText(txt).then(() => {
      setCopiado(label);
      setTimeout(() => setCopiado(""), 2000);
    });
  };

  if (formaPagamento === "PIX") {
    if (!qrCodePayload && !qrCodeImageUrl) return null;
    const imgSrc = qrCodeImageUrl?.startsWith("data:") ? qrCodeImageUrl
                 : qrCodeImageUrl ? `data:image/png;base64,${qrCodeImageUrl}` : null;
    return (
      <Cd t={t} style={{ padding: 16, textAlign: "center", border: `2px solid ${t.pri}` }}>
        <div style={{ fontSize: 13, fontWeight: 600, color: t.pri, marginBottom: 10 }}>QR Code PIX</div>
        {imgSrc && <img src={imgSrc} alt="QR PIX" style={{ width: 200, height: 200, objectFit: "contain", background: "#fff", padding: 6, borderRadius: 8 }} />}
        {qrCodePayload && (
          <>
            <div style={{ fontSize: 11, color: t.txMuted, marginTop: 10 }}>Ou copie o codigo PIX:</div>
            <div style={{ padding: "8px 10px", background: t.soft, borderRadius: 8, marginTop: 4, fontSize: 10, fontFamily: "monospace", color: t.txSoft, wordBreak: "break-all", textAlign: "left" }}>
              {qrCodePayload}
            </div>
            <button onClick={() => copiar(qrCodePayload, "pix")} className="btn-primary" style={{ marginTop: 10, padding: "10px 18px", background: t.priGrad, color: "#fff", fontSize: 13, borderRadius: 10, border: "none", fontWeight: 600, cursor: "pointer", fontFamily: "inherit" }}>
              {copiado === "pix" ? "Copiado!" : "Copiar codigo PIX"}
            </button>
          </>
        )}
      </Cd>
    );
  }

  if (formaPagamento === "BOLETO") {
    if (!linhaDigitavel && !boletoUrl) return null;
    return (
      <Cd t={t} style={{ padding: 16, border: `2px solid ${t.pri}` }}>
        <div style={{ fontSize: 13, fontWeight: 600, color: t.pri, marginBottom: 10 }}>Boleto</div>
        {linhaDigitavel && (
          <>
            <div style={{ fontSize: 11, color: t.txMuted }}>Linha digitavel:</div>
            <div style={{ padding: "8px 10px", background: t.soft, borderRadius: 8, marginTop: 4, fontSize: 11, fontFamily: "monospace", color: t.txSoft, wordBreak: "break-all" }}>
              {linhaDigitavel}
            </div>
            <button onClick={() => copiar(linhaDigitavel, "boleto")} style={{ marginTop: 10, padding: "10px 18px", background: "transparent", color: t.pri, fontSize: 13, borderRadius: 10, border: `1px solid ${t.pri}`, fontWeight: 600, cursor: "pointer", fontFamily: "inherit", width: "100%" }}>
              {copiado === "boleto" ? "Copiado!" : "Copiar linha digitavel"}
            </button>
          </>
        )}
        {boletoUrl && (
          <a href={boletoUrl} target="_blank" rel="noopener noreferrer" className="btn-primary" style={{ display: "block", marginTop: 10, padding: "12px 18px", background: t.priGrad, color: "#fff", fontSize: 13, borderRadius: 10, border: "none", fontWeight: 600, cursor: "pointer", textAlign: "center", textDecoration: "none" }}>
            Abrir boleto (PDF)
          </a>
        )}
      </Cd>
    );
  }

  if (formaPagamento === "CARTAO") {
    if (!checkoutUrl) return null;
    return (
      <Cd t={t} style={{ padding: 16, textAlign: "center", border: `2px solid ${t.pri}` }}>
        <div style={{ fontSize: 13, fontWeight: 600, color: t.pri, marginBottom: 10 }}>Pagar com cartao</div>
        <div style={{ fontSize: 12, color: t.txMuted, marginBottom: 12 }}>Voce sera redirecionado para o checkout seguro</div>
        <a href={checkoutUrl} target="_blank" rel="noopener noreferrer" className="btn-primary" style={{ display: "block", padding: "12px 18px", background: t.priGrad, color: "#fff", fontSize: 14, borderRadius: 10, border: "none", fontWeight: 600, textDecoration: "none" }}>
          Abrir checkout
        </a>
      </Cd>
    );
  }

  return null;
}

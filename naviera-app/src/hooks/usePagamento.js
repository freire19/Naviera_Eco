import { useState } from "react";
import { authFetch } from "../api.js";
import { lerRespostaJson } from "../helpers.js";

/**
 * usePagamento — fluxo POST /pagar compartilhado entre Encomendas (CPF) e Fretes (CNPJ).
 *
 * @param {(item) => string} resolveUrl — recebe o item e retorna a URL absoluta do POST.
 * @param {object} authHeaders
 */
export default function usePagamento(resolveUrl, authHeaders) {
  const [pagando, setPagando] = useState(null);
  const [formaPag, setFormaPag] = useState("PIX");
  const [enviando, setEnviando] = useState(false);
  const [errPag, setErrPag] = useState("");
  const [resultado, setResultado] = useState(null);
  const [toast, setToast] = useState(null);

  const cancelar = () => { setPagando(null); setErrPag(""); setFormaPag("PIX"); };
  const fecharResultado = () => { setResultado(null); setFormaPag("PIX"); };

  // resultadoExtra deixa o caller anexar campos display-only (numero, destinatario, embarcacao).
  const confirmar = async (resultadoExtra = {}, onSuccess) => {
    if (!pagando || enviando) return;
    setErrPag(""); setEnviando(true);
    try {
      const res = await authFetch(resolveUrl(pagando), {
        method: "POST", headers: authHeaders,
        body: JSON.stringify({ formaPagamento: formaPag }),
      });
      const { raw, data } = await lerRespostaJson(res);
      if (!res.ok) {
        setErrPag(data?.erro || data?.message || (raw && raw.slice(0, 120).trim()) || `HTTP ${res.status}`);
        return;
      }
      if (formaPag === "BARCO") {
        setToast("Reservado para pagar no embarque");
        cancelar();
      } else {
        setResultado({ ...(data || {}), ...resultadoExtra });
        setPagando(null);
      }
      onSuccess?.();
    } catch (e) {
      console.warn("[usePagamento] confirmar falhou:", e?.message);
      setErrPag("Sem conexao com o servidor.");
    } finally { setEnviando(false); }
  };

  return {
    pagando, setPagando,
    formaPag, setFormaPag,
    enviando, errPag,
    resultado, fecharResultado,
    toast, setToast,
    cancelar, confirmar,
  };
}

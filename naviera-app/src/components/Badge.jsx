import { useTheme } from "../contexts/ThemeContext.jsx";

export default function Badge({ status }) {
  const { t } = useTheme();
  const m = {
    "Em tr\u00e2nsito": [t.infoBg, t.infoTx], "EM_VIAGEM": [t.infoBg, t.infoTx], "Em viagem": [t.infoBg, t.infoTx],
    "Entregue": [t.okBg, t.okTx], "NO_PORTO": [t.warnBg, t.warnTx], "Confirmada": [t.okBg, t.okTx],
    "Reservada": [t.warnBg, t.warnTx], "Aguardando": [t.warnBg, t.warnTx], "No destino": [t.okBg, t.okTx],
    "Offline": [t.soft, t.txMuted], "Pendente": [t.errBg, t.errTx], "Pago": [t.okBg, t.okTx], "Verificada": [t.okBg, t.okTx],
    "CONFIRMADA": [t.okBg, t.okTx], "PENDENTE_CONFIRMACAO": [t.warnBg, t.warnTx], "EMBARCADO": [t.infoBg, t.infoTx],
    "CANCELADA": [t.errBg, t.errTx], "EXPIRADA": [t.soft, t.txMuted], "PENDENTE": [t.errBg, t.errTx]
  };
  const [bg, c] = m[status] || [t.soft, t.txMuted];
  const labels = { "EM_VIAGEM": "Em viagem", "NO_PORTO": "No porto", "CONFIRMADA": "Confirmada", "PENDENTE_CONFIRMACAO": "Aguardando pgto", "EMBARCADO": "Embarcado", "CANCELADA": "Cancelada", "EXPIRADA": "Expirada", "PENDENTE": "Pendente" };
  const label = labels[status] || status;
  return <span style={{ fontSize: 11, padding: "3px 10px", borderRadius: 20, background: bg, color: c, fontWeight: 600 }}>{label}</span>;
}

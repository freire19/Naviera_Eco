const STATUS_MAP = {
  pendente: { label: 'Pendente', key: 'warn' },
  revisado_operador: { label: 'Revisado', key: 'info' },
  aprovado: { label: 'Aprovado', key: 'ok' },
  rejeitado: { label: 'Rejeitado', key: 'err' }
}

export default function Badge({ status, t }) {
  const cfg = STATUS_MAP[status] || { label: status, key: 'info' }
  const bg = t[cfg.key + 'Bg'] || t.infoBg
  const tx = t[cfg.key + 'Tx'] || t.infoTx

  return (
    <span className="badge" style={{ background: bg, color: tx }}>
      {cfg.label}
    </span>
  )
}

export function ConfidenceBadge({ value, t }) {
  let bg, tx
  if (value >= 80) { bg = t.okBg; tx = t.okTx }
  else if (value >= 50) { bg = t.warnBg; tx = t.warnTx }
  else { bg = t.errBg; tx = t.errTx }

  return (
    <span className="badge" style={{ background: bg, color: tx }}>
      {value}%
    </span>
  )
}

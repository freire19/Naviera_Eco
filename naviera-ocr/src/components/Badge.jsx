import { OCR_STATUS, STATUS_LABELS } from '../constants.js'

const STATUS_THEME = {
  [OCR_STATUS.PENDENTE]: 'warn',
  [OCR_STATUS.REVISADO]: 'info',
  [OCR_STATUS.APROVADO]: 'ok',
  [OCR_STATUS.REJEITADO]: 'err'
}

export default function Badge({ status, t }) {
  const themeKey = STATUS_THEME[status] || 'info'
  const label = STATUS_LABELS[status] || status

  return (
    <span className="badge" style={{
      background: t[themeKey + 'Bg'] || t.infoBg,
      color: t[themeKey + 'Tx'] || t.infoTx
    }}>
      {label}
    </span>
  )
}

const CONFIDENCE_LEVELS = [
  { min: 80, label: 'Alta', key: 'ok' },
  { min: 50, label: 'Media', key: 'warn' },
  { min: 0, label: 'Baixa', key: 'err' }
]

export function ConfidenceBadge({ value, t }) {
  const level = CONFIDENCE_LEVELS.find(l => value >= l.min) || CONFIDENCE_LEVELS[2]

  return (
    <span className="badge" style={{
      background: t[level.key + 'Bg'],
      color: t[level.key + 'Tx']
    }} title={`Confianca ${level.label}: ${value}% de certeza do OCR`}>
      {value}% {level.label}
    </span>
  )
}

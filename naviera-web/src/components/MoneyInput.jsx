import { useState } from 'react'

// Input monetario que mostra "R$ 0,00" quando nao esta em foco
// e aceita digitacao natural (vírgula ou ponto) durante edicao.
// Emite onChange(numero) — o parent sempre recebe Number.

function formatBRL(n) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(Number(n) || 0)
}

function parseDigits(str) {
  // Aceita "4,5", "4.5", "4500", "R$ 4,50" etc.
  // Remove tudo exceto digitos, virgula e ponto; troca vírgula por ponto
  const cleaned = String(str || '').replace(/[^\d.,-]/g, '').replace(',', '.')
  const n = parseFloat(cleaned)
  return isNaN(n) ? 0 : n
}

const baseInputStyle = {
  width: '100%',
  boxSizing: 'border-box',
  padding: '7px 10px',
  fontSize: '0.85rem',
  background: 'var(--bg-soft)',
  border: '1px solid var(--border)',
  borderRadius: 4,
  color: 'var(--text)',
  fontFamily: 'Space Mono, monospace',
  textAlign: 'right'
}

export default function MoneyInput({ value, onChange, style = {}, readOnly, disabled, ...rest }) {
  const [editing, setEditing] = useState(false)
  const [tempStr, setTempStr] = useState('')

  function handleFocus(e) {
    if (readOnly || disabled) return
    setEditing(true)
    // Ao focar, mostra o valor em formato digitavel (com virgula brasileira)
    const n = Number(value) || 0
    setTempStr(n === 0 ? '' : n.toFixed(2).replace('.', ','))
    setTimeout(() => e.target.select?.(), 0)
  }

  function handleChange(e) {
    setTempStr(e.target.value)
    onChange?.(parseDigits(e.target.value))
  }

  function handleBlur() {
    setEditing(false)
  }

  return (
    <input
      type="text"
      inputMode="decimal"
      value={editing ? tempStr : formatBRL(value)}
      onFocus={handleFocus}
      onChange={handleChange}
      onBlur={handleBlur}
      readOnly={readOnly}
      disabled={disabled}
      style={{ ...baseInputStyle, ...style }}
      {...rest}
    />
  )
}

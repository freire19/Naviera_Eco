import { useEffect } from 'react'

export default function Toast({ message, type = 'info', t, onClose }) {
  useEffect(() => {
    const timer = setTimeout(onClose, 3000)
    return () => clearTimeout(timer)
  }, [onClose])

  const colors = {
    success: { bg: t.okBg, tx: t.okTx, border: t.ok },
    error: { bg: t.errBg, tx: t.errTx, border: t.err },
    info: { bg: t.infoBg, tx: t.infoTx, border: t.info },
    warn: { bg: t.warnBg, tx: t.warnTx, border: t.warn }
  }
  const c = colors[type] || colors.info

  return (
    <div className="toast" style={{
      background: c.bg, color: c.tx, border: `1px solid ${c.border}`,
      borderRadius: 12, padding: '10px 20px', fontSize: '0.9rem', fontWeight: 500
    }}>
      {message}
    </div>
  )
}

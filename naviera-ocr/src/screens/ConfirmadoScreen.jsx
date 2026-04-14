import { IconCheck } from '../icons.jsx'

export default function ConfirmadoScreen({ t, lancamentoId, onNovo, onHistorico }) {
  return (
    <div className="screen-enter" style={{
      padding: 24, display: 'flex', flexDirection: 'column', alignItems: 'center',
      justifyContent: 'center', minHeight: '60vh', gap: 20, textAlign: 'center'
    }}>
      <div style={{
        width: 72, height: 72, borderRadius: '50%', background: t.okBg,
        display: 'flex', alignItems: 'center', justifyContent: 'center'
      }}>
        <IconCheck size={36} color={t.ok} />
      </div>

      <div>
        <h2 style={{ color: t.tx, fontSize: '1.2rem', fontWeight: 700, marginBottom: 6 }}>
          Lancamento Enviado
        </h2>
        <p style={{ color: t.txMuted, fontSize: '0.9rem' }}>
          OCR #{lancamentoId} esta aguardando revisao do conferente.
        </p>
      </div>

      <div style={{ display: 'flex', gap: 12, width: '100%', maxWidth: 320 }}>
        <button
          className="btn"
          onClick={onNovo}
          style={{ flex: 1, background: t.priGrad, color: '#fff' }}
        >
          Nova Captura
        </button>
        <button
          className="btn"
          onClick={onHistorico}
          style={{ flex: 1, background: t.soft, color: t.tx, border: `1px solid ${t.border}` }}
        >
          Historico
        </button>
      </div>
    </div>
  )
}

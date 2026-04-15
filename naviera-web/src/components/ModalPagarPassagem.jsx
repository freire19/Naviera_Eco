import { useState } from 'react'
import { api } from '../api.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}

export default function ModalPagarPassagem({ passagem, onClose, onSuccess, showToast }) {
  const restante = (passagem.valor_total || 0) - (passagem.valor_pago || 0)
  const [valorPagamento, setValorPagamento] = useState(restante > 0 ? restante.toFixed(2) : '')
  const [pagando, setPagando] = useState(false)

  async function handlePagar(e) {
    e.preventDefault()
    const valor = Number(valorPagamento)
    if (!valor || valor <= 0) {
      showToast('Informe um valor valido', 'error')
      return
    }

    setPagando(true)
    try {
      await api.post(`/passagens/${passagem.id_passagem}/pagar`, { valor_pago: valor })
      showToast('Pagamento registrado com sucesso')
      onSuccess()
    } catch (err) {
      showToast(err.message || 'Erro ao registrar pagamento', 'error')
    } finally {
      setPagando(false)
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 400 }}>
        <h3>Registrar Pagamento</h3>
        <p style={{ color: 'var(--text-muted)', marginBottom: 12 }}>
          Passageiro: <strong>{passagem.nome_passageiro || '\u2014'}</strong>
          <br />
          Restante: <strong>{formatMoney(restante)}</strong>
        </p>
        <form onSubmit={handlePagar}>
          <div className="form-group">
            <label>Valor do Pagamento *</label>
            <input
              type="number"
              step="0.01"
              min="0.01"
              value={valorPagamento}
              onChange={e => setValorPagamento(e.target.value)}
              placeholder="0.00"
              autoFocus
              required
            />
          </div>
          <div className="modal-actions">
            <button type="button" className="btn-secondary" onClick={onClose} disabled={pagando}>
              Cancelar
            </button>
            <button type="submit" className="btn-primary" disabled={pagando}>
              {pagando ? 'Processando...' : 'Confirmar Pagamento'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

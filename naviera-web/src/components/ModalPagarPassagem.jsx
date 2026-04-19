import { useState, useEffect } from 'react'
import { api } from '../api.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}

export default function ModalPagarPassagem({ passagem, caixas = [], onClose, onSuccess, showToast }) {
  const restante = Math.round(((passagem.valor_a_pagar || passagem.valor_total || 0) - (passagem.valor_pago || 0)) * 100) / 100
  const [dinheiro, setDinheiro] = useState('')
  const [pix, setPix] = useState('')
  const [cartao, setCartao] = useState('')
  const [idCaixa, setIdCaixa] = useState('')
  const [pagando, setPagando] = useState(false)

  // Esc fecha o modal (mantém passagem salva como PENDENTE)
  useEffect(() => {
    const onEsc = (e) => { if (e.key === 'Escape') onClose?.() }
    window.addEventListener('keydown', onEsc)
    return () => window.removeEventListener('keydown', onEsc)
  }, [onClose])

  const vDinheiro = parseFloat(dinheiro) || 0
  const vPix = parseFloat(pix) || 0
  const vCartao = parseFloat(cartao) || 0
  const totalRecebido = Math.round((vDinheiro + vPix + vCartao) * 100) / 100
  const troco = totalRecebido > restante ? Math.round((totalRecebido - restante) * 100) / 100 : 0
  const devedor = totalRecebido < restante ? Math.round((restante - totalRecebido) * 100) / 100 : 0

  async function handlePagar(e) {
    e.preventDefault()
    if (totalRecebido <= 0) {
      showToast('Informe pelo menos um valor de pagamento', 'error')
      return
    }

    const valorEfetivo = Math.min(totalRecebido, restante)

    setPagando(true)
    try {
      await api.post(`/passagens/${passagem.id_passagem}/pagar`, {
        valor_pago: valorEfetivo,
        valor_pagamento_dinheiro: vDinheiro,
        valor_pagamento_pix: vPix,
        valor_pagamento_cartao: vCartao,
        id_caixa: idCaixa || null
      })
      showToast(troco > 0 ? `Pagamento registrado. Troco: ${formatMoney(troco)}` : 'Pagamento registrado com sucesso')
      onSuccess()
    } catch (err) {
      showToast(err.message || 'Erro ao registrar pagamento', 'error')
    } finally {
      setPagando(false)
    }
  }

  const readonlyStyle = { background: 'var(--bg-hover)', cursor: 'default' }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 480 }}>
        <h3>Finalizar Pagamento (Misto)</h3>
        <p style={{ color: 'var(--text-muted)', marginBottom: 4 }}>
          Passageiro: <strong>{passagem.nome_passageiro || '\u2014'}</strong>
        </p>
        <p style={{ color: 'var(--text-muted)', marginBottom: 12 }}>
          Bilhete: <strong>{passagem.numero_bilhete}</strong>
          {' \u2014 '}
          Restante: <strong style={{ color: 'var(--danger, #e74c3c)' }}>{formatMoney(restante)}</strong>
        </p>
        <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: 12, fontStyle: 'italic' }}>
          A passagem ja foi criada e esta como PENDENTE. Use <strong>Pagar Depois</strong> se o cliente vai pagar no destino.
        </p>

        <form onSubmit={handlePagar}>
          <div className="form-grid">
            <div className="form-group">
              <label>Valor a Pagar</label>
              <input value={formatMoney(restante)} readOnly style={readonlyStyle} tabIndex={-1} />
            </div>
            <div className="form-group">
              <label>Caixa</label>
              <select value={idCaixa} onChange={e => setIdCaixa(e.target.value)}>
                <option value="">Selecione...</option>
                {caixas.map(c => (
                  <option key={c.id_caixa} value={c.id_caixa}>{c.nome_caixa}</option>
                ))}
              </select>
            </div>
          </div>

          <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px', borderBottom: '1px solid var(--border)', paddingBottom: 4, marginBottom: 10, marginTop: 8 }}>
            Formas de Pagamento
          </div>

          <div className="form-grid">
            <div className="form-group">
              <label>Dinheiro</label>
              <input
                type="number" step="0.01" min="0"
                value={dinheiro} onChange={e => setDinheiro(e.target.value)}
                placeholder="0.00" autoFocus
              />
            </div>
            <div className="form-group">
              <label>PIX</label>
              <input
                type="number" step="0.01" min="0"
                value={pix} onChange={e => setPix(e.target.value)}
                placeholder="0.00"
              />
            </div>
            <div className="form-group">
              <label>Cartao</label>
              <input
                type="number" step="0.01" min="0"
                value={cartao} onChange={e => setCartao(e.target.value)}
                placeholder="0.00"
              />
            </div>
          </div>

          <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px', borderBottom: '1px solid var(--border)', paddingBottom: 4, marginBottom: 10, marginTop: 8 }}>
            Resumo
          </div>

          <div className="form-grid">
            <div className="form-group">
              <label>Total Recebido</label>
              <input value={formatMoney(totalRecebido)} readOnly style={readonlyStyle} tabIndex={-1} />
            </div>
            <div className="form-group">
              <label>Troco</label>
              <input
                value={formatMoney(troco)}
                readOnly
                style={{ ...readonlyStyle, color: troco > 0 ? 'var(--success, #27ae60)' : undefined, fontWeight: troco > 0 ? 700 : 400 }}
                tabIndex={-1}
              />
            </div>
            <div className="form-group">
              <label>Devedor</label>
              <input
                value={formatMoney(devedor)}
                readOnly
                style={{ ...readonlyStyle, color: devedor > 0 ? 'var(--danger, #e74c3c)' : undefined, fontWeight: devedor > 0 ? 700 : 400 }}
                tabIndex={-1}
              />
            </div>
          </div>

          <div className="modal-actions">
            <button type="button" className="btn-secondary" onClick={onClose} disabled={pagando}
              title="Fecha este modal mantendo a passagem salva como PENDENTE (Esc)">
              Pagar Depois
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

import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}

const FORM_INITIAL = {
  remetente_nome_temp: '',
  destinatario_nome_temp: '',
  rota_temp: '',
  conferente_temp: '',
  valor_total_itens: '',
  desconto: '',
  valor_pago: '',
  tipo_pagamento: 'DINHEIRO',
  nome_caixa: '',
  observacoes: '',
}

export default function Fretes({ viagemAtiva }) {
  const [fretes, setFretes] = useState([])
  const [loading, setLoading] = useState(false)

  // Modal de criacao
  const [showCreate, setShowCreate] = useState(false)
  const [form, setForm] = useState(FORM_INITIAL)
  const [saving, setSaving] = useState(false)

  // Modal de pagamento
  const [showPay, setShowPay] = useState(null) // frete selecionado
  const [valorPago, setValorPago] = useState('')
  const [paying, setPaying] = useState(false)

  // Toast
  const [toast, setToast] = useState(null)

  const showToast = useCallback((message, type = 'success') => {
    setToast({ message, type })
    setTimeout(() => setToast(null), 3500)
  }, [])

  const fetchFretes = useCallback(() => {
    if (!viagemAtiva) return
    setLoading(true)
    api.get(`/op/fretes?viagem_id=${viagemAtiva.id_viagem}`)
      .then(setFretes)
      .catch(() => showToast('Erro ao carregar fretes', 'error'))
      .finally(() => setLoading(false))
  }, [viagemAtiva, showToast])

  useEffect(() => {
    fetchFretes()
  }, [fetchFretes])

  // --- Criar frete ---
  function handleFormChange(e) {
    const { name, value } = e.target
    setForm(prev => ({ ...prev, [name]: value }))
  }

  function openCreateModal() {
    setForm(FORM_INITIAL)
    setShowCreate(true)
  }

  async function handleCreate(e) {
    e.preventDefault()
    setSaving(true)
    try {
      await api.post('/op/fretes', {
        id_viagem: viagemAtiva.id_viagem,
        remetente_nome_temp: form.remetente_nome_temp,
        destinatario_nome_temp: form.destinatario_nome_temp,
        rota_temp: form.rota_temp,
        conferente_temp: form.conferente_temp,
        observacoes: form.observacoes,
        valor_total_itens: parseFloat(form.valor_total_itens) || 0,
        desconto: parseFloat(form.desconto) || 0,
        valor_pago: parseFloat(form.valor_pago) || 0,
        tipo_pagamento: form.tipo_pagamento,
        nome_caixa: form.nome_caixa,
      })
      showToast('Frete criado com sucesso')
      setShowCreate(false)
      fetchFretes()
    } catch {
      showToast('Erro ao criar frete', 'error')
    } finally {
      setSaving(false)
    }
  }

  // --- Pagar frete ---
  function openPayModal(frete) {
    setShowPay(frete)
    setValorPago('')
  }

  async function handlePay(e) {
    e.preventDefault()
    if (!showPay) return
    setPaying(true)
    try {
      await api.post(`/op/fretes/${showPay.id_frete}/pagar`, {
        valor_pago: parseFloat(valorPago) || 0,
      })
      showToast('Pagamento registrado com sucesso')
      setShowPay(null)
      fetchFretes()
    } catch {
      showToast('Erro ao registrar pagamento', 'error')
    } finally {
      setPaying(false)
    }
  }

  // --- Excluir frete ---
  async function handleDelete(frete) {
    if (!window.confirm(`Excluir frete #${frete.numero_frete || frete.id_frete}? Esta acao nao pode ser desfeita.`)) return
    try {
      await api.delete(`/op/fretes/${frete.id_frete}`)
      showToast('Frete excluido com sucesso')
      fetchFretes()
    } catch {
      showToast('Erro ao excluir frete', 'error')
    }
  }

  // --- Placeholder sem viagem ---
  if (!viagemAtiva) {
    return (
      <div className="placeholder-page">
        <div className="ph-icon">{'\uD83D\uDE9A'}</div>
        <h2>Fretes</h2>
        <p>Selecione uma viagem para ver os fretes.</p>
      </div>
    )
  }

  return (
    <div>
      {/* Toast */}
      {toast && (
        <div className={`toast ${toast.type}`}>{toast.message}</div>
      )}

      <div className="card">
        <div className="card-header">
          <h3>Fretes — {viagemAtiva.descricao || `Viagem #${viagemAtiva.id_viagem}`}</h3>
          <div className="toolbar">
            <span className="badge info">{fretes.length} registros</span>
            <button className="btn-primary" onClick={openCreateModal}>+ Novo Frete</button>
          </div>
        </div>

        {loading ? (
          <p style={{ color: 'var(--text-muted)', padding: 20 }}>Carregando...</p>
        ) : (
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>N. Frete</th>
                  <th>Remetente</th>
                  <th>Destinatario</th>
                  <th>Rota</th>
                  <th>Valor</th>
                  <th>Pago</th>
                  <th>Status</th>
                  <th>Acoes</th>
                </tr>
              </thead>
              <tbody>
                {fretes.map(f => (
                  <tr key={f.id_frete}>
                    <td>{f.numero_frete}</td>
                    <td>{f.nome_remetente || '\u2014'}</td>
                    <td>{f.nome_destinatario || '\u2014'}</td>
                    <td>{f.nome_rota || '\u2014'}</td>
                    <td className="money">{formatMoney(f.valor_nominal)}</td>
                    <td className="money">{formatMoney(f.valor_pago)}</td>
                    <td>
                      <span className={`badge ${f.status === 'PAGO' ? 'success' : f.status === 'CANCELADO' ? 'danger' : 'warning'}`}>
                        {f.status || 'Pendente'}
                      </span>
                    </td>
                    <td>
                      {f.status !== 'PAGO' && f.status !== 'CANCELADO' && (
                        <button className="btn-sm primary" onClick={() => openPayModal(f)}>Pagar</button>
                      )}
                      {' '}
                      <button className="btn-sm danger" onClick={() => handleDelete(f)}>Excluir</button>
                    </td>
                  </tr>
                ))}
                {fretes.length === 0 && (
                  <tr>
                    <td colSpan="8" style={{ textAlign: 'center', padding: 30, color: 'var(--text-muted)' }}>
                      Nenhum frete nesta viagem
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal Criar Frete */}
      {showCreate && (
        <div className="modal-overlay" onClick={() => setShowCreate(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3>Novo Frete</h3>
            <form onSubmit={handleCreate}>
              <div className="form-grid">
                <div className="form-group">
                  <label>Remetente</label>
                  <input name="remetente_nome_temp" value={form.remetente_nome_temp} onChange={handleFormChange} required />
                </div>
                <div className="form-group">
                  <label>Destinatario</label>
                  <input name="destinatario_nome_temp" value={form.destinatario_nome_temp} onChange={handleFormChange} required />
                </div>
                <div className="form-group">
                  <label>Rota</label>
                  <input name="rota_temp" value={form.rota_temp} onChange={handleFormChange} />
                </div>
                <div className="form-group">
                  <label>Conferente</label>
                  <input name="conferente_temp" value={form.conferente_temp} onChange={handleFormChange} />
                </div>
                <div className="form-group">
                  <label>Valor Total Itens (R$)</label>
                  <input name="valor_total_itens" type="number" step="0.01" min="0" value={form.valor_total_itens} onChange={handleFormChange} required />
                </div>
                <div className="form-group">
                  <label>Desconto (R$)</label>
                  <input name="desconto" type="number" step="0.01" min="0" value={form.desconto} onChange={handleFormChange} />
                </div>
                <div className="form-group">
                  <label>Valor Pago (R$)</label>
                  <input name="valor_pago" type="number" step="0.01" min="0" value={form.valor_pago} onChange={handleFormChange} />
                </div>
                <div className="form-group">
                  <label>Tipo Pagamento</label>
                  <select name="tipo_pagamento" value={form.tipo_pagamento} onChange={handleFormChange}>
                    <option value="DINHEIRO">Dinheiro</option>
                    <option value="PIX">PIX</option>
                    <option value="CARTAO">Cartao</option>
                  </select>
                </div>
                <div className="form-group">
                  <label>Nome do Caixa</label>
                  <input name="nome_caixa" value={form.nome_caixa} onChange={handleFormChange} />
                </div>
                <div className="form-group full-width">
                  <label>Observacoes</label>
                  <textarea name="observacoes" value={form.observacoes} onChange={handleFormChange} rows={3} />
                </div>
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => setShowCreate(false)}>Cancelar</button>
                <button type="submit" className="btn-primary" disabled={saving}>
                  {saving ? 'Salvando...' : 'Criar Frete'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Pagar Frete */}
      {showPay && (
        <div className="modal-overlay" onClick={() => setShowPay(null)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3>Registrar Pagamento — Frete #{showPay.numero_frete || showPay.id_frete}</h3>
            <p style={{ color: 'var(--text-muted)', margin: '8px 0 16px' }}>
              Valor pendente: {formatMoney((showPay.valor_nominal || 0) - (showPay.valor_pago || 0))}
            </p>
            <form onSubmit={handlePay}>
              <div className="form-group">
                <label>Valor Pago (R$)</label>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  value={valorPago}
                  onChange={e => setValorPago(e.target.value)}
                  required
                  autoFocus
                />
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => setShowPay(null)}>Cancelar</button>
                <button type="submit" className="btn-primary" disabled={paying}>
                  {paying ? 'Processando...' : 'Confirmar Pagamento'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

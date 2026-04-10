import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}

const EMPTY_FORM = {
  remetente: '',
  destinatario: '',
  rota: '',
  total_volumes: 1,
  total_a_pagar: 0,
  valor_pago: 0,
  desconto: 0,
  forma_pagamento: 'DINHEIRO',
  observacoes: '',
}

function Toast({ toast, onClose }) {
  useEffect(() => {
    if (!toast) return
    const t = setTimeout(onClose, 3500)
    return () => clearTimeout(t)
  }, [toast, onClose])

  if (!toast) return null
  return (
    <div className={`toast ${toast.type}`} onClick={onClose}>
      {toast.msg}
    </div>
  )
}

export default function Encomendas({ viagemAtiva }) {
  const [encomendas, setEncomendas] = useState([])
  const [loading, setLoading] = useState(false)
  const [toast, setToast] = useState(null)

  // modals
  const [showCreate, setShowCreate] = useState(false)
  const [showPagar, setShowPagar] = useState(null)   // encomenda obj
  const [showEntregar, setShowEntregar] = useState(null) // encomenda obj
  const [showConfirmDelete, setShowConfirmDelete] = useState(null)

  // forms
  const [form, setForm] = useState(EMPTY_FORM)
  const [pagarValor, setPagarValor] = useState('')
  const [entregarForm, setEntregarForm] = useState({ nome_recebedor: '', doc_recebedor: '' })
  const [submitting, setSubmitting] = useState(false)

  const notify = useCallback((msg, type = 'success') => setToast({ msg, type }), [])

  const fetchEncomendas = useCallback(() => {
    if (!viagemAtiva) return
    setLoading(true)
    api.get(`/op/encomendas?viagem_id=${viagemAtiva.id_viagem}`)
      .then(setEncomendas)
      .catch(() => notify('Erro ao carregar encomendas', 'error'))
      .finally(() => setLoading(false))
  }, [viagemAtiva, notify])

  useEffect(() => {
    fetchEncomendas()
  }, [fetchEncomendas])

  // ---- Criar encomenda ----
  function openCreate() {
    setForm(EMPTY_FORM)
    setShowCreate(true)
  }

  async function handleCreate(e) {
    e.preventDefault()
    if (!form.remetente.trim() || !form.destinatario.trim()) {
      notify('Preencha remetente e destinatario', 'error')
      return
    }
    setSubmitting(true)
    try {
      await api.post('/op/encomendas', {
        id_viagem: viagemAtiva.id_viagem,
        remetente: form.remetente.trim(),
        destinatario: form.destinatario.trim(),
        rota: form.rota.trim(),
        total_volumes: Number(form.total_volumes) || 1,
        total_a_pagar: Number(form.total_a_pagar) || 0,
        valor_pago: Number(form.valor_pago) || 0,
        desconto: Number(form.desconto) || 0,
        forma_pagamento: form.forma_pagamento,
        observacoes: form.observacoes.trim(),
        itens: [],
      })
      notify('Encomenda criada com sucesso')
      setShowCreate(false)
      fetchEncomendas()
    } catch (err) {
      notify(err.message || 'Erro ao criar encomenda', 'error')
    } finally {
      setSubmitting(false)
    }
  }

  // ---- Pagar ----
  function openPagar(enc) {
    setShowPagar(enc)
    setPagarValor('')
  }

  async function handlePagar(e) {
    e.preventDefault()
    const valor = Number(pagarValor)
    if (!valor || valor <= 0) {
      notify('Informe um valor valido', 'error')
      return
    }
    setSubmitting(true)
    try {
      await api.post(`/op/encomendas/${showPagar.id_encomenda}/pagar`, { valor_pago: valor })
      notify('Pagamento registrado com sucesso')
      setShowPagar(null)
      fetchEncomendas()
    } catch (err) {
      notify(err.message || 'Erro ao registrar pagamento', 'error')
    } finally {
      setSubmitting(false)
    }
  }

  // ---- Entregar ----
  function openEntregar(enc) {
    setShowEntregar(enc)
    setEntregarForm({ nome_recebedor: '', doc_recebedor: '' })
  }

  async function handleEntregar(e) {
    e.preventDefault()
    if (!entregarForm.nome_recebedor.trim()) {
      notify('Informe o nome do recebedor', 'error')
      return
    }
    setSubmitting(true)
    try {
      await api.put(`/op/encomendas/${showEntregar.id_encomenda}/entregar`, {
        nome_recebedor: entregarForm.nome_recebedor.trim(),
        doc_recebedor: entregarForm.doc_recebedor.trim(),
      })
      notify('Encomenda marcada como entregue')
      setShowEntregar(null)
      fetchEncomendas()
    } catch (err) {
      notify(err.message || 'Erro ao entregar encomenda', 'error')
    } finally {
      setSubmitting(false)
    }
  }

  // ---- Excluir ----
  async function handleDelete() {
    setSubmitting(true)
    try {
      await api.delete(`/op/encomendas/${showConfirmDelete.id_encomenda}`)
      notify('Encomenda excluida')
      setShowConfirmDelete(null)
      fetchEncomendas()
    } catch (err) {
      notify(err.message || 'Erro ao excluir encomenda', 'error')
    } finally {
      setSubmitting(false)
    }
  }

  // ---- Placeholder (sem viagem) ----
  if (!viagemAtiva) {
    return (
      <div className="placeholder-page">
        <div className="ph-icon">{'\uD83D\uDCE6'}</div>
        <h2>Encomendas</h2>
        <p>Selecione uma viagem para ver as encomendas.</p>
      </div>
    )
  }

  return (
    <div>
      <Toast toast={toast} onClose={() => setToast(null)} />

      <div className="card">
        <div className="card-header">
          <h3>Encomendas — {viagemAtiva.descricao || `Viagem #${viagemAtiva.id_viagem}`}</h3>
          <div className="toolbar">
            <span className="badge info">{encomendas.length} registros</span>
            <button className="btn-primary" onClick={openCreate}>+ Nova Encomenda</button>
          </div>
        </div>

        {loading ? (
          <p style={{ color: 'var(--text-muted)', padding: 20 }}>Carregando...</p>
        ) : (
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>N.</th>
                  <th>Remetente</th>
                  <th>Destinatario</th>
                  <th>Volumes</th>
                  <th>Total</th>
                  <th>Pago</th>
                  <th>Pagamento</th>
                  <th>Entrega</th>
                  <th>Acoes</th>
                </tr>
              </thead>
              <tbody>
                {encomendas.map(e => (
                  <tr key={e.id_encomenda}>
                    <td>{e.numero_encomenda}</td>
                    <td>{e.remetente || '\u2014'}</td>
                    <td>{e.destinatario || '\u2014'}</td>
                    <td>{e.total_volumes}</td>
                    <td className="money">{formatMoney(e.total_a_pagar)}</td>
                    <td className="money">{formatMoney(e.valor_pago)}</td>
                    <td>
                      <span className={`badge ${e.status_pagamento === 'PAGO' ? 'success' : 'warning'}`}>
                        {e.status_pagamento || 'Pendente'}
                      </span>
                    </td>
                    <td>
                      <span className={`badge ${e.entregue ? 'success' : 'warning'}`}>
                        {e.entregue ? 'Entregue' : 'Pendente'}
                      </span>
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: 6 }}>
                        {!e.entregue && e.status_pagamento !== 'PAGO' && (
                          <button className="btn-sm primary" onClick={() => openPagar(e)}>Pagar</button>
                        )}
                        {!e.entregue && (
                          <button className="btn-sm primary" onClick={() => openEntregar(e)}>Entregar</button>
                        )}
                        <button className="btn-sm danger" onClick={() => setShowConfirmDelete(e)}>Excluir</button>
                      </div>
                    </td>
                  </tr>
                ))}
                {encomendas.length === 0 && (
                  <tr>
                    <td colSpan="9" style={{ textAlign: 'center', padding: 30, color: 'var(--text-muted)' }}>
                      Nenhuma encomenda nesta viagem
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal — Nova Encomenda */}
      {showCreate && (
        <div className="modal-overlay" onClick={() => setShowCreate(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3>Nova Encomenda</h3>
            <form onSubmit={handleCreate}>
              <div className="form-grid">
                <div className="form-group">
                  <label>Remetente *</label>
                  <input
                    type="text"
                    value={form.remetente}
                    onChange={e => setForm({ ...form, remetente: e.target.value })}
                    required
                  />
                </div>
                <div className="form-group">
                  <label>Destinatario *</label>
                  <input
                    type="text"
                    value={form.destinatario}
                    onChange={e => setForm({ ...form, destinatario: e.target.value })}
                    required
                  />
                </div>
                <div className="form-group">
                  <label>Rota</label>
                  <input
                    type="text"
                    value={form.rota}
                    onChange={e => setForm({ ...form, rota: e.target.value })}
                  />
                </div>
                <div className="form-group">
                  <label>Total Volumes</label>
                  <input
                    type="number"
                    min="1"
                    value={form.total_volumes}
                    onChange={e => setForm({ ...form, total_volumes: e.target.value })}
                  />
                </div>
                <div className="form-group">
                  <label>Total a Pagar (R$)</label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    value={form.total_a_pagar}
                    onChange={e => setForm({ ...form, total_a_pagar: e.target.value })}
                  />
                </div>
                <div className="form-group">
                  <label>Valor Pago (R$)</label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    value={form.valor_pago}
                    onChange={e => setForm({ ...form, valor_pago: e.target.value })}
                  />
                </div>
                <div className="form-group">
                  <label>Desconto (R$)</label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    value={form.desconto}
                    onChange={e => setForm({ ...form, desconto: e.target.value })}
                  />
                </div>
                <div className="form-group">
                  <label>Forma de Pagamento</label>
                  <select
                    value={form.forma_pagamento}
                    onChange={e => setForm({ ...form, forma_pagamento: e.target.value })}
                  >
                    <option value="DINHEIRO">Dinheiro</option>
                    <option value="PIX">PIX</option>
                    <option value="CARTAO">Cartao</option>
                  </select>
                </div>
              </div>
              <div className="form-group full-width">
                <label>Observacoes</label>
                <textarea
                  rows={3}
                  value={form.observacoes}
                  onChange={e => setForm({ ...form, observacoes: e.target.value })}
                />
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => setShowCreate(false)}>
                  Cancelar
                </button>
                <button type="submit" className="btn-primary" disabled={submitting}>
                  {submitting ? 'Salvando...' : 'Salvar'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal — Pagar */}
      {showPagar && (
        <div className="modal-overlay" onClick={() => setShowPagar(null)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3>Registrar Pagamento</h3>
            <p>
              Encomenda #{showPagar.numero_encomenda} — {showPagar.remetente}
            </p>
            <p>
              Total: {formatMoney(showPagar.total_a_pagar)} | Ja pago: {formatMoney(showPagar.valor_pago)}
            </p>
            <form onSubmit={handlePagar}>
              <div className="form-group">
                <label>Valor a pagar (R$)</label>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  value={pagarValor}
                  onChange={e => setPagarValor(e.target.value)}
                  autoFocus
                  required
                />
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => setShowPagar(null)}>
                  Cancelar
                </button>
                <button type="submit" className="btn-primary" disabled={submitting}>
                  {submitting ? 'Registrando...' : 'Confirmar Pagamento'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal — Entregar */}
      {showEntregar && (
        <div className="modal-overlay" onClick={() => setShowEntregar(null)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3>Registrar Entrega</h3>
            <p>
              Encomenda #{showEntregar.numero_encomenda} — {showEntregar.destinatario}
            </p>
            <form onSubmit={handleEntregar}>
              <div className="form-group">
                <label>Nome do Recebedor *</label>
                <input
                  type="text"
                  value={entregarForm.nome_recebedor}
                  onChange={e => setEntregarForm({ ...entregarForm, nome_recebedor: e.target.value })}
                  autoFocus
                  required
                />
              </div>
              <div className="form-group">
                <label>Documento do Recebedor</label>
                <input
                  type="text"
                  value={entregarForm.doc_recebedor}
                  onChange={e => setEntregarForm({ ...entregarForm, doc_recebedor: e.target.value })}
                />
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => setShowEntregar(null)}>
                  Cancelar
                </button>
                <button type="submit" className="btn-primary" disabled={submitting}>
                  {submitting ? 'Registrando...' : 'Confirmar Entrega'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal — Confirmar Exclusao */}
      {showConfirmDelete && (
        <div className="modal-overlay" onClick={() => setShowConfirmDelete(null)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3>Confirmar Exclusao</h3>
            <p>
              Deseja excluir a encomenda #{showConfirmDelete.numero_encomenda} de{' '}
              <strong>{showConfirmDelete.remetente}</strong> para{' '}
              <strong>{showConfirmDelete.destinatario}</strong>?
            </p>
            <p style={{ color: 'var(--danger)' }}>Esta acao nao pode ser desfeita.</p>
            <div className="modal-actions">
              <button className="btn-secondary" onClick={() => setShowConfirmDelete(null)}>
                Cancelar
              </button>
              <button className="btn-primary" onClick={handleDelete} disabled={submitting}
                style={{ background: 'var(--danger)' }}>
                {submitting ? 'Excluindo...' : 'Excluir'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}

const FORM_INICIAL = {
  id_passageiro: '',
  id_viagem: '',
  assento: '',
  valor_total: '',
  valor_pago: '',
  observacoes: '',
  id_rota: '',
  id_tipo_passagem: '',
  id_acomodacao: '',
  id_caixa: '',
  valor_pagamento_dinheiro: '',
  valor_pagamento_pix: '',
  valor_pagamento_cartao: '',
  nome_passageiro: '',
  numero_doc: ''
}

export default function Passagens({ viagemAtiva }) {
  const [passagens, setPassagens] = useState([])
  const [loading, setLoading] = useState(false)

  // Modal criar
  const [modalCriar, setModalCriar] = useState(false)
  const [form, setForm] = useState(FORM_INICIAL)
  const [salvando, setSalvando] = useState(false)

  // Modal pagar
  const [modalPagar, setModalPagar] = useState(null)
  const [valorPagamento, setValorPagamento] = useState('')
  const [pagando, setPagando] = useState(false)

  // Toast
  const [toast, setToast] = useState(null)

  // Tipos passageiro (dropdown)
  const [tiposPassageiro, setTiposPassageiro] = useState([])

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  const carregarPassagens = useCallback(() => {
    if (!viagemAtiva) return
    setLoading(true)
    api.get(`/op/passagens?viagem_id=${viagemAtiva.id_viagem}`)
      .then(setPassagens)
      .catch(() => showToast('Erro ao carregar passagens', 'error'))
      .finally(() => setLoading(false))
  }, [viagemAtiva])

  useEffect(() => {
    carregarPassagens()
  }, [carregarPassagens])

  useEffect(() => {
    api.get('/op/cadastros/tipos-passageiro')
      .then(setTiposPassageiro)
      .catch(() => {})
  }, [])

  // --- Criar passagem ---

  function abrirModalCriar() {
    setForm({ ...FORM_INICIAL, id_viagem: viagemAtiva.id_viagem })
    setModalCriar(true)
  }

  function fecharModalCriar() {
    setModalCriar(false)
    setForm(FORM_INICIAL)
  }

  function handleFormChange(e) {
    const { name, value } = e.target
    setForm(prev => ({ ...prev, [name]: value }))
  }

  async function handleCriar(e) {
    e.preventDefault()
    if (!form.nome_passageiro.trim()) {
      showToast('Informe o nome do passageiro', 'error')
      return
    }
    if (!form.valor_total || Number(form.valor_total) <= 0) {
      showToast('Informe o valor total', 'error')
      return
    }

    setSalvando(true)
    try {
      await api.post('/op/passagens', {
        ...form,
        id_viagem: viagemAtiva.id_viagem,
        valor_total: Number(form.valor_total) || 0,
        valor_pago: Number(form.valor_pago) || 0,
        valor_pagamento_dinheiro: Number(form.valor_pagamento_dinheiro) || 0,
        valor_pagamento_pix: Number(form.valor_pagamento_pix) || 0,
        valor_pagamento_cartao: Number(form.valor_pagamento_cartao) || 0,
        id_tipo_passagem: form.id_tipo_passagem || null,
        id_rota: form.id_rota || null,
        id_acomodacao: form.id_acomodacao || null,
        id_caixa: form.id_caixa || null,
        id_passageiro: form.id_passageiro || null
      })
      showToast('Passagem criada com sucesso')
      fecharModalCriar()
      carregarPassagens()
    } catch (err) {
      showToast(err.message || 'Erro ao criar passagem', 'error')
    } finally {
      setSalvando(false)
    }
  }

  // --- Pagar ---

  function abrirModalPagar(passagem) {
    const restante = (passagem.valor_total || 0) - (passagem.valor_pago || 0)
    setModalPagar(passagem)
    setValorPagamento(restante > 0 ? restante.toFixed(2) : '')
  }

  function fecharModalPagar() {
    setModalPagar(null)
    setValorPagamento('')
  }

  async function handlePagar(e) {
    e.preventDefault()
    const valor = Number(valorPagamento)
    if (!valor || valor <= 0) {
      showToast('Informe um valor valido', 'error')
      return
    }

    setPagando(true)
    try {
      await api.post(`/op/passagens/${modalPagar.id_passagem}/pagar`, { valor_pago: valor })
      showToast('Pagamento registrado com sucesso')
      fecharModalPagar()
      carregarPassagens()
    } catch (err) {
      showToast(err.message || 'Erro ao registrar pagamento', 'error')
    } finally {
      setPagando(false)
    }
  }

  // --- Excluir ---

  async function handleExcluir(passagem) {
    if (!window.confirm(`Excluir passagem ${passagem.num_bilhete || passagem.id_passagem}?`)) return

    try {
      await api.delete(`/op/passagens/${passagem.id_passagem}`)
      showToast('Passagem excluida com sucesso')
      carregarPassagens()
    } catch (err) {
      showToast(err.message || 'Erro ao excluir passagem', 'error')
    }
  }

  // --- Placeholder (sem viagem) ---

  if (!viagemAtiva) {
    return (
      <div className="placeholder-page">
        <div className="ph-icon">{'\uD83C\uDFAB'}</div>
        <h2>Passagens</h2>
        <p>Selecione uma viagem para ver as passagens.</p>
      </div>
    )
  }

  // --- Render ---

  return (
    <div>
      {/* Toast */}
      {toast && (
        <div className={`toast ${toast.type}`}>{toast.msg}</div>
      )}

      <div className="card">
        <div className="card-header">
          <div className="toolbar">
            <h3>Passagens — {viagemAtiva.descricao || `Viagem #${viagemAtiva.id_viagem}`}</h3>
            <button className="btn-primary" onClick={abrirModalCriar}>
              + Nova Passagem
            </button>
          </div>
          <span className="badge info">{passagens.length} registros</span>
        </div>

        {loading ? (
          <p style={{ color: 'var(--text-muted)', padding: 20 }}>Carregando...</p>
        ) : (
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Bilhete</th>
                  <th>Passageiro</th>
                  <th>Documento</th>
                  <th>Assento</th>
                  <th>Valor Total</th>
                  <th>Pago</th>
                  <th>Status</th>
                  <th>Acoes</th>
                </tr>
              </thead>
              <tbody>
                {passagens.map(p => {
                  const restante = (p.valor_total || 0) - (p.valor_pago || 0)
                  return (
                    <tr key={p.id_passagem}>
                      <td>{p.num_bilhete}</td>
                      <td>{p.nome_passageiro || '\u2014'}</td>
                      <td>{p.numero_doc || '\u2014'}</td>
                      <td>{p.assento || '\u2014'}</td>
                      <td className="money">{formatMoney(p.valor_total)}</td>
                      <td className="money">{formatMoney(p.valor_pago)}</td>
                      <td>
                        <span className={`badge ${p.devedor ? 'danger' : 'success'}`}>
                          {p.devedor ? 'Devedor' : 'Pago'}
                        </span>
                      </td>
                      <td>
                        {restante > 0 && (
                          <button
                            className="btn-sm primary"
                            onClick={() => abrirModalPagar(p)}
                            style={{ marginRight: 6 }}
                          >
                            Pagar
                          </button>
                        )}
                        <button
                          className="btn-sm danger"
                          onClick={() => handleExcluir(p)}
                        >
                          Excluir
                        </button>
                      </td>
                    </tr>
                  )
                })}
                {passagens.length === 0 && (
                  <tr>
                    <td colSpan="8" style={{ textAlign: 'center', padding: 30, color: 'var(--text-muted)' }}>
                      Nenhuma passagem nesta viagem
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal Criar Passagem */}
      {modalCriar && (
        <div className="modal-overlay" onClick={fecharModalCriar}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3>Nova Passagem</h3>
            <form onSubmit={handleCriar}>
              <div className="form-grid">
                <div className="form-group">
                  <label>Nome do Passageiro *</label>
                  <input
                    name="nome_passageiro"
                    value={form.nome_passageiro}
                    onChange={handleFormChange}
                    placeholder="Nome completo"
                    required
                  />
                </div>
                <div className="form-group">
                  <label>Documento</label>
                  <input
                    name="numero_doc"
                    value={form.numero_doc}
                    onChange={handleFormChange}
                    placeholder="CPF / RG"
                  />
                </div>
                <div className="form-group">
                  <label>Assento</label>
                  <input
                    name="assento"
                    value={form.assento}
                    onChange={handleFormChange}
                    placeholder="Ex: A12"
                  />
                </div>
                <div className="form-group">
                  <label>Tipo Passageiro</label>
                  <select
                    name="id_tipo_passagem"
                    value={form.id_tipo_passagem}
                    onChange={handleFormChange}
                  >
                    <option value="">Selecione...</option>
                    {tiposPassageiro.map(t => (
                      <option key={t.id_tipo_passageiro} value={t.id_tipo_passageiro}>
                        {t.descricao}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label>Valor Total *</label>
                  <input
                    name="valor_total"
                    type="number"
                    step="0.01"
                    min="0"
                    value={form.valor_total}
                    onChange={handleFormChange}
                    placeholder="0.00"
                    required
                  />
                </div>
                <div className="form-group">
                  <label>Valor Pago</label>
                  <input
                    name="valor_pago"
                    type="number"
                    step="0.01"
                    min="0"
                    value={form.valor_pago}
                    onChange={handleFormChange}
                    placeholder="0.00"
                  />
                </div>
                <div className="form-group">
                  <label>Pgto Dinheiro</label>
                  <input
                    name="valor_pagamento_dinheiro"
                    type="number"
                    step="0.01"
                    min="0"
                    value={form.valor_pagamento_dinheiro}
                    onChange={handleFormChange}
                    placeholder="0.00"
                  />
                </div>
                <div className="form-group">
                  <label>Pgto PIX</label>
                  <input
                    name="valor_pagamento_pix"
                    type="number"
                    step="0.01"
                    min="0"
                    value={form.valor_pagamento_pix}
                    onChange={handleFormChange}
                    placeholder="0.00"
                  />
                </div>
                <div className="form-group">
                  <label>Pgto Cartao</label>
                  <input
                    name="valor_pagamento_cartao"
                    type="number"
                    step="0.01"
                    min="0"
                    value={form.valor_pagamento_cartao}
                    onChange={handleFormChange}
                    placeholder="0.00"
                  />
                </div>
                <div className="form-group full-width">
                  <label>Observacoes</label>
                  <textarea
                    name="observacoes"
                    value={form.observacoes}
                    onChange={handleFormChange}
                    rows={3}
                    placeholder="Observacoes opcionais..."
                  />
                </div>
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={fecharModalCriar} disabled={salvando}>
                  Cancelar
                </button>
                <button type="submit" className="btn-primary" disabled={salvando}>
                  {salvando ? 'Salvando...' : 'Salvar'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Pagar */}
      {modalPagar && (
        <div className="modal-overlay" onClick={fecharModalPagar}>
          <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 400 }}>
            <h3>Registrar Pagamento</h3>
            <p style={{ color: 'var(--text-muted)', marginBottom: 12 }}>
              Passageiro: <strong>{modalPagar.nome_passageiro || '\u2014'}</strong>
              <br />
              Restante: <strong>{formatMoney((modalPagar.valor_total || 0) - (modalPagar.valor_pago || 0))}</strong>
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
                <button type="button" className="btn-secondary" onClick={fecharModalPagar} disabled={pagando}>
                  Cancelar
                </button>
                <button type="submit" className="btn-primary" disabled={pagando}>
                  {pagando ? 'Processando...' : 'Confirmar Pagamento'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

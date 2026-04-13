import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}

function formatDate(dateStr) {
  if (!dateStr) return '--'
  const d = new Date(dateStr)
  return d.toLocaleDateString('pt-BR')
}

const STATUS_OPTIONS = ['TODOS', 'PENDENTE', 'PAGO']

export default function Boletos({ viagemAtiva }) {
  const [boletos, setBoletos] = useState([])
  const [loading, setLoading] = useState(false)
  const [filtroStatus, setFiltroStatus] = useState('PENDENTE')
  const [toast, setToast] = useState(null)

  // Modal novo boleto
  const [showNovo, setShowNovo] = useState(false)
  const [salvando, setSalvando] = useState(false)
  const [formNovo, setFormNovo] = useState({
    descricao: '', valor_total: '', data_vencimento: '',
    id_categoria: '', observacoes: ''
  })

  // Modal parcelas
  const [showParcelas, setShowParcelas] = useState(false)
  const [formParcelas, setFormParcelas] = useState({
    descricao_base: '', valor_total: '', parcelas: 2,
    data_primeira_vencimento: '', intervalo_dias: 30, id_categoria: ''
  })

  // Modal baixa
  const [showBaixa, setShowBaixa] = useState(null)
  const [formaPagBaixa, setFormaPagBaixa] = useState('BOLETO')

  const mostrarToast = useCallback((msg, tipo = 'success') => {
    setToast({ msg, tipo })
    setTimeout(() => setToast(null), 3500)
  }, [])

  const carregarBoletos = useCallback(() => {
    setLoading(true)
    let url = '/financeiro/boletos?'
    if (viagemAtiva) url += `viagem_id=${viagemAtiva.id_viagem}&`
    if (filtroStatus !== 'TODOS') url += `status=${filtroStatus}`
    api.get(url)
      .then(data => setBoletos(Array.isArray(data) ? data : []))
      .catch(() => { setBoletos([]); mostrarToast('Erro ao carregar boletos', 'error') })
      .finally(() => setLoading(false))
  }, [viagemAtiva, filtroStatus, mostrarToast])

  useEffect(() => { carregarBoletos() }, [carregarBoletos])

  // Summary
  const totalPendente = boletos.filter(b => b.status === 'PENDENTE').reduce((s, b) => s + parseFloat(b.valor_total || 0), 0)
  const totalPago = boletos.filter(b => b.status === 'PAGO').reduce((s, b) => s + parseFloat(b.valor_total || 0), 0)

  // Handlers novo boleto
  function handleChangeNovo(e) {
    const { name, value } = e.target
    setFormNovo(prev => ({ ...prev, [name]: value }))
  }

  async function handleSalvarNovo(e) {
    e.preventDefault()
    if (!formNovo.descricao.trim()) return mostrarToast('Preencha a descricao.', 'error')
    if (!formNovo.valor_total || Number(formNovo.valor_total) <= 0) return mostrarToast('Informe um valor valido.', 'error')
    if (!formNovo.data_vencimento) return mostrarToast('Informe a data de vencimento.', 'error')

    setSalvando(true)
    try {
      await api.post('/financeiro/boleto', {
        descricao: formNovo.descricao.trim(),
        valor_total: parseFloat(formNovo.valor_total),
        data_vencimento: formNovo.data_vencimento,
        id_categoria: formNovo.id_categoria || null,
        id_viagem: viagemAtiva?.id_viagem || null,
        observacoes: formNovo.observacoes.trim() || null
      })
      mostrarToast('Boleto criado com sucesso!')
      setShowNovo(false)
      setFormNovo({ descricao: '', valor_total: '', data_vencimento: '', id_categoria: '', observacoes: '' })
      carregarBoletos()
    } catch (err) {
      mostrarToast(err?.message || 'Erro ao criar boleto.', 'error')
    } finally {
      setSalvando(false)
    }
  }

  // Handlers parcelas
  function handleChangeParcelas(e) {
    const { name, value } = e.target
    setFormParcelas(prev => ({ ...prev, [name]: value }))
  }

  async function handleSalvarParcelas(e) {
    e.preventDefault()
    if (!formParcelas.descricao_base.trim()) return mostrarToast('Preencha a descricao base.', 'error')
    if (!formParcelas.valor_total || Number(formParcelas.valor_total) <= 0) return mostrarToast('Informe o valor total.', 'error')
    if (!formParcelas.parcelas || Number(formParcelas.parcelas) < 1) return mostrarToast('Informe o numero de parcelas.', 'error')
    if (!formParcelas.data_primeira_vencimento) return mostrarToast('Informe a data do primeiro vencimento.', 'error')

    setSalvando(true)
    try {
      await api.post('/financeiro/boleto/batch', {
        descricao_base: formParcelas.descricao_base.trim(),
        valor_total: parseFloat(formParcelas.valor_total),
        parcelas: parseInt(formParcelas.parcelas),
        data_primeira_vencimento: formParcelas.data_primeira_vencimento,
        intervalo_dias: parseInt(formParcelas.intervalo_dias) || 30,
        id_categoria: formParcelas.id_categoria || null,
        id_viagem: viagemAtiva?.id_viagem || null
      })
      mostrarToast(`${formParcelas.parcelas} boletos criados com sucesso!`)
      setShowParcelas(false)
      setFormParcelas({ descricao_base: '', valor_total: '', parcelas: 2, data_primeira_vencimento: '', intervalo_dias: 30, id_categoria: '' })
      carregarBoletos()
    } catch (err) {
      mostrarToast(err?.message || 'Erro ao gerar parcelas.', 'error')
    } finally {
      setSalvando(false)
    }
  }

  // Handler baixa
  async function handleConfirmarBaixa() {
    if (!showBaixa) return
    setSalvando(true)
    try {
      await api.put(`/financeiro/boleto/${showBaixa.id}/baixa`, {
        forma_pagamento: formaPagBaixa
      })
      mostrarToast('Baixa realizada com sucesso!')
      setShowBaixa(null)
      carregarBoletos()
    } catch (err) {
      mostrarToast(err?.message || 'Erro ao dar baixa.', 'error')
    } finally {
      setSalvando(false)
    }
  }

  return (
    <div>
      {toast && <div className={`toast ${toast.tipo}`}>{toast.msg}</div>}

      {/* Summary Cards */}
      <div className="dash-grid">
        <div className="stat-card warning">
          <span className="stat-label">Total Pendente</span>
          <span className="stat-value money">{formatMoney(totalPendente)}</span>
        </div>
        <div className="stat-card success">
          <span className="stat-label">Total Pago</span>
          <span className="stat-value money positive">{formatMoney(totalPago)}</span>
        </div>
        <div className="stat-card info">
          <span className="stat-label">Qtd. Boletos</span>
          <span className="stat-value">{boletos.length}</span>
        </div>
      </div>

      {/* Filters & Actions */}
      <div className="card" style={{ marginTop: '1.5rem' }}>
        <div className="card-header">
          <h3>Boletos</h3>
          <div className="toolbar" style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
            <select value={filtroStatus} onChange={e => setFiltroStatus(e.target.value)} style={{ padding: '0.4rem 0.8rem' }}>
              {STATUS_OPTIONS.map(s => <option key={s} value={s}>{s}</option>)}
            </select>
            <button className="btn-primary" onClick={() => setShowNovo(true)}>+ Novo Boleto</button>
            <button className="btn-secondary" onClick={() => setShowParcelas(true)}>Gerar Parcelas</button>
          </div>
        </div>

        <div className="table-container">
          {loading ? (
            <p style={{ padding: '1rem', color: 'var(--text-muted)' }}>Carregando...</p>
          ) : boletos.length === 0 ? (
            <p style={{ padding: '1rem', color: 'var(--text-muted)' }}>Nenhum boleto encontrado.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Vencimento</th>
                  <th>Descricao</th>
                  <th>Parcela</th>
                  <th>Valor</th>
                  <th>Status</th>
                  <th>Acoes</th>
                </tr>
              </thead>
              <tbody>
                {boletos.map((b, idx) => (
                  <tr key={b.id || idx}>
                    <td>{formatDate(b.data_vencimento)}</td>
                    <td>{b.descricao}</td>
                    <td>{b.numero_parcela && b.total_parcelas ? `${b.numero_parcela}/${b.total_parcelas}` : '--'}</td>
                    <td className="money">{formatMoney(b.valor_total)}</td>
                    <td>
                      <span className={`badge ${b.status === 'PAGO' ? 'success' : 'warning'}`}>
                        {b.status}
                      </span>
                    </td>
                    <td>
                      {b.status === 'PENDENTE' && (
                        <button className="btn-sm success" onClick={() => { setShowBaixa(b); setFormaPagBaixa('BOLETO') }}>
                          Dar Baixa
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* Modal Novo Boleto */}
      {showNovo && (
        <div className="modal-overlay" onClick={() => setShowNovo(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3>Novo Boleto</h3>
            <form onSubmit={handleSalvarNovo}>
              <div className="form-grid">
                <div className="form-group full-width">
                  <label>Descricao *</label>
                  <input type="text" name="descricao" value={formNovo.descricao} onChange={handleChangeNovo} placeholder="Ex: Aluguel, Fornecedor..." autoFocus />
                </div>
                <div className="form-group">
                  <label>Valor (R$) *</label>
                  <input type="number" name="valor_total" value={formNovo.valor_total} onChange={handleChangeNovo} placeholder="0.00" min="0.01" step="0.01" />
                </div>
                <div className="form-group">
                  <label>Data Vencimento *</label>
                  <input type="date" name="data_vencimento" value={formNovo.data_vencimento} onChange={handleChangeNovo} />
                </div>
                <div className="form-group full-width">
                  <label>Observacoes</label>
                  <textarea name="observacoes" value={formNovo.observacoes} onChange={handleChangeNovo} rows={3} placeholder="Observacoes adicionais..." />
                </div>
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => setShowNovo(false)} disabled={salvando}>Cancelar</button>
                <button type="submit" className="btn-primary" disabled={salvando}>{salvando ? 'Salvando...' : 'Salvar'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Gerar Parcelas */}
      {showParcelas && (
        <div className="modal-overlay" onClick={() => setShowParcelas(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3>Gerar Parcelas (Boletos)</h3>
            <form onSubmit={handleSalvarParcelas}>
              <div className="form-grid">
                <div className="form-group full-width">
                  <label>Descricao Base *</label>
                  <input type="text" name="descricao_base" value={formParcelas.descricao_base} onChange={handleChangeParcelas} placeholder="Ex: Financiamento Motor" autoFocus />
                </div>
                <div className="form-group">
                  <label>Valor Total (R$) *</label>
                  <input type="number" name="valor_total" value={formParcelas.valor_total} onChange={handleChangeParcelas} placeholder="0.00" min="0.01" step="0.01" />
                </div>
                <div className="form-group">
                  <label>Numero de Parcelas *</label>
                  <input type="number" name="parcelas" value={formParcelas.parcelas} onChange={handleChangeParcelas} min="1" max="120" />
                </div>
                <div className="form-group">
                  <label>Data 1o Vencimento *</label>
                  <input type="date" name="data_primeira_vencimento" value={formParcelas.data_primeira_vencimento} onChange={handleChangeParcelas} />
                </div>
                <div className="form-group">
                  <label>Intervalo (dias)</label>
                  <input type="number" name="intervalo_dias" value={formParcelas.intervalo_dias} onChange={handleChangeParcelas} min="1" max="365" />
                </div>
              </div>
              {formParcelas.valor_total && formParcelas.parcelas > 0 && (
                <p style={{ color: 'var(--text-muted)', marginTop: '0.5rem' }}>
                  Valor por parcela: {formatMoney(parseFloat(formParcelas.valor_total) / parseInt(formParcelas.parcelas))}
                </p>
              )}
              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => setShowParcelas(false)} disabled={salvando}>Cancelar</button>
                <button type="submit" className="btn-primary" disabled={salvando}>{salvando ? 'Gerando...' : 'Gerar Parcelas'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Dar Baixa */}
      {showBaixa && (
        <div className="modal-overlay" onClick={() => setShowBaixa(null)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3>Dar Baixa no Boleto</h3>
            <p>
              Confirmar pagamento de <strong>{showBaixa.descricao}</strong> no valor de <strong>{formatMoney(showBaixa.valor_total)}</strong>?
            </p>
            <div className="form-group" style={{ marginTop: '1rem' }}>
              <label>Forma de Pagamento</label>
              <select value={formaPagBaixa} onChange={e => setFormaPagBaixa(e.target.value)}>
                <option value="BOLETO">Boleto</option>
                <option value="PIX">PIX</option>
                <option value="TRANSFERENCIA">Transferencia</option>
                <option value="DINHEIRO">Dinheiro</option>
                <option value="CARTAO">Cartao</option>
              </select>
            </div>
            <div className="modal-actions">
              <button type="button" className="btn-secondary" onClick={() => setShowBaixa(null)} disabled={salvando}>Cancelar</button>
              <button type="button" className="btn-primary" onClick={handleConfirmarBaixa} disabled={salvando}>
                {salvando ? 'Processando...' : 'Confirmar Baixa'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

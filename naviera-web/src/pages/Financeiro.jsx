import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}

function formatDate(dateStr) {
  if (!dateStr) return '—'
  const d = new Date(dateStr)
  return d.toLocaleDateString('pt-BR')
}

export default function Financeiro({ viagemAtiva }) {
  const [balanco, setBalanco] = useState(null)
  const [saidas, setSaidas] = useState([])
  const [loading, setLoading] = useState(false)
  const [loadingSaidas, setLoadingSaidas] = useState(false)
  const [modalAberto, setModalAberto] = useState(false)
  const [salvando, setSalvando] = useState(false)
  const [toast, setToast] = useState(null)
  const [modalExcluir, setModalExcluir] = useState(null)
  const [motivoExclusao, setMotivoExclusao] = useState('')

  const [form, setForm] = useState({
    descricao: '',
    valor: '',
    data: '',
    tipo: '',
    observacoes: ''
  })

  const mostrarToast = useCallback((msg, tipo = 'success') => {
    setToast({ msg, tipo })
    setTimeout(() => setToast(null), 3500)
  }, [])

  const carregarBalanco = useCallback(() => {
    if (!viagemAtiva) return
    api.get(`/op/financeiro/balanco?viagem_id=${viagemAtiva.id_viagem}`)
      .then(setBalanco)
      .catch(() => {})
  }, [viagemAtiva])

  const carregarSaidas = useCallback(() => {
    if (!viagemAtiva) return
    setLoadingSaidas(true)
    api.get(`/op/financeiro/saidas?viagem_id=${viagemAtiva.id_viagem}`)
      .then(data => setSaidas(Array.isArray(data) ? data : []))
      .catch(() => setSaidas([]))
      .finally(() => setLoadingSaidas(false))
  }, [viagemAtiva])

  useEffect(() => {
    if (!viagemAtiva) return
    setLoading(true)
    Promise.all([
      api.get(`/op/financeiro/balanco?viagem_id=${viagemAtiva.id_viagem}`).then(setBalanco).catch(() => {}),
      api.get(`/op/financeiro/saidas?viagem_id=${viagemAtiva.id_viagem}`).then(data => setSaidas(Array.isArray(data) ? data : [])).catch(() => setSaidas([]))
    ]).finally(() => setLoading(false))
  }, [viagemAtiva])

  function abrirModal() {
    setForm({ descricao: '', valor: '', data: '', tipo: '', observacoes: '' })
    setModalAberto(true)
  }

  function fecharModal() {
    setModalAberto(false)
  }

  function handleChange(e) {
    const { name, value } = e.target
    setForm(prev => ({ ...prev, [name]: value }))
  }

  async function handleSalvar(e) {
    e.preventDefault()

    if (!form.descricao.trim()) {
      mostrarToast('Preencha a descricao.', 'error')
      return
    }
    if (!form.valor || Number(form.valor) <= 0) {
      mostrarToast('Informe um valor valido.', 'error')
      return
    }
    if (!form.data) {
      mostrarToast('Informe a data.', 'error')
      return
    }

    setSalvando(true)
    try {
      await api.post('/op/financeiro/saida', {
        id_viagem: viagemAtiva.id_viagem,
        descricao: form.descricao.trim(),
        valor: parseFloat(form.valor),
        data: form.data,
        id_categoria: null,
        tipo: form.tipo.trim() || null,
        observacoes: form.observacoes.trim() || null
      })
      mostrarToast('Saida criada com sucesso!')
      fecharModal()
      carregarSaidas()
      carregarBalanco()
    } catch (err) {
      mostrarToast(err?.message || 'Erro ao criar saida.', 'error')
    } finally {
      setSalvando(false)
    }
  }

  function abrirExcluir(saida) {
    setModalExcluir(saida)
    setMotivoExclusao('')
  }

  function fecharExcluir() {
    setModalExcluir(null)
    setMotivoExclusao('')
  }

  async function confirmarExclusao() {
    if (!motivoExclusao.trim()) {
      mostrarToast('Informe o motivo da exclusao.', 'error')
      return
    }

    setSalvando(true)
    try {
      await api.delete(`/op/financeiro/saida/${modalExcluir.id_despesa || modalExcluir.id}`, {
        motivo: motivoExclusao.trim()
      })
      mostrarToast('Saida excluida com sucesso!')
      fecharExcluir()
      carregarSaidas()
      carregarBalanco()
    } catch (err) {
      mostrarToast(err?.message || 'Erro ao excluir saida.', 'error')
    } finally {
      setSalvando(false)
    }
  }

  if (!viagemAtiva) {
    return (
      <div className="placeholder-page">
        <div className="ph-icon">{'\uD83D\uDCB0'}</div>
        <h2>Financeiro</h2>
        <p>Selecione uma viagem para ver o financeiro.</p>
      </div>
    )
  }

  return (
    <div>
      {loading && <p style={{ color: 'var(--text-muted)' }}>Carregando...</p>}

      {/* Toast */}
      {toast && (
        <div className={`toast ${toast.tipo}`}>
          {toast.msg}
        </div>
      )}

      {/* Balance Cards */}
      {balanco && (
        <>
          <div className="dash-grid">
            <div className="stat-card primary">
              <span className="stat-label">Passagens</span>
              <span className="stat-value money">{formatMoney(balanco.receitas.passagens)}</span>
            </div>
            <div className="stat-card info">
              <span className="stat-label">Encomendas</span>
              <span className="stat-value money">{formatMoney(balanco.receitas.encomendas)}</span>
            </div>
            <div className="stat-card warning">
              <span className="stat-label">Fretes</span>
              <span className="stat-value money">{formatMoney(balanco.receitas.fretes)}</span>
            </div>
          </div>

          <div className="dash-grid">
            <div className="stat-card success">
              <span className="stat-label">Total Receitas</span>
              <span className="stat-value money positive">{formatMoney(balanco.totalReceitas)}</span>
            </div>
            <div className="stat-card" style={{ borderLeft: '3px solid var(--danger)' }}>
              <span className="stat-label">Total Despesas</span>
              <span className="stat-value money negative">{formatMoney(balanco.totalDespesas)}</span>
            </div>
            <div className="stat-card" style={{ borderLeft: `3px solid ${balanco.saldo >= 0 ? 'var(--success)' : 'var(--danger)'}` }}>
              <span className="stat-label">Saldo</span>
              <span className={`stat-value money ${balanco.saldo >= 0 ? 'positive' : 'negative'}`}>
                {formatMoney(balanco.saldo)}
              </span>
            </div>
          </div>
        </>
      )}

      {/* Saidas Table */}
      <div className="card" style={{ marginTop: '1.5rem' }}>
        <div className="card-header">
          <h3>Despesas (Saidas)</h3>
          <div className="toolbar">
            <button className="btn-primary" onClick={abrirModal}>
              + Nova Saida
            </button>
          </div>
        </div>

        <div className="table-container">
          {loadingSaidas ? (
            <p style={{ padding: '1rem', color: 'var(--text-muted)' }}>Carregando saidas...</p>
          ) : saidas.length === 0 ? (
            <p style={{ padding: '1rem', color: 'var(--text-muted)' }}>Nenhuma saida registrada nesta viagem.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Data</th>
                  <th>Descricao</th>
                  <th>Tipo</th>
                  <th>Valor</th>
                  <th>Acoes</th>
                </tr>
              </thead>
              <tbody>
                {saidas.map((s, idx) => (
                  <tr key={s.id_despesa || s.id || idx}>
                    <td>{formatDate(s.data)}</td>
                    <td>{s.descricao}</td>
                    <td>
                      {s.tipo ? <span className="badge">{s.tipo}</span> : '—'}
                    </td>
                    <td className="money">{formatMoney(s.valor)}</td>
                    <td>
                      <button
                        className="btn-sm danger"
                        onClick={() => abrirExcluir(s)}
                      >
                        Excluir
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* Modal Nova Saida */}
      {modalAberto && (
        <div className="modal-overlay" onClick={fecharModal}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3>Nova Saida</h3>
            <form onSubmit={handleSalvar}>
              <div className="form-grid">
                <div className="form-group full-width">
                  <label>Descricao *</label>
                  <input
                    type="text"
                    name="descricao"
                    value={form.descricao}
                    onChange={handleChange}
                    placeholder="Ex: Combustivel, Manutencao..."
                    autoFocus
                  />
                </div>
                <div className="form-group">
                  <label>Valor (R$) *</label>
                  <input
                    type="number"
                    name="valor"
                    value={form.valor}
                    onChange={handleChange}
                    placeholder="0.00"
                    min="0.01"
                    step="0.01"
                  />
                </div>
                <div className="form-group">
                  <label>Data *</label>
                  <input
                    type="date"
                    name="data"
                    value={form.data}
                    onChange={handleChange}
                  />
                </div>
                <div className="form-group">
                  <label>Tipo</label>
                  <input
                    type="text"
                    name="tipo"
                    value={form.tipo}
                    onChange={handleChange}
                    placeholder="Ex: Combustivel, Alimentacao..."
                  />
                </div>
                <div className="form-group full-width">
                  <label>Observacoes</label>
                  <textarea
                    name="observacoes"
                    value={form.observacoes}
                    onChange={handleChange}
                    rows={3}
                    placeholder="Observacoes adicionais..."
                  />
                </div>
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={fecharModal} disabled={salvando}>
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

      {/* Modal Confirmar Exclusao */}
      {modalExcluir && (
        <div className="modal-overlay" onClick={fecharExcluir}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3>Excluir Saida</h3>
            <p>
              Tem certeza que deseja excluir a despesa <strong>{modalExcluir.descricao}</strong> de{' '}
              <strong>{formatMoney(modalExcluir.valor)}</strong>?
            </p>
            <div className="form-group" style={{ marginTop: '1rem' }}>
              <label>Motivo da exclusao *</label>
              <input
                type="text"
                value={motivoExclusao}
                onChange={e => setMotivoExclusao(e.target.value)}
                placeholder="Informe o motivo..."
                autoFocus
              />
            </div>
            <div className="modal-actions">
              <button type="button" className="btn-secondary" onClick={fecharExcluir} disabled={salvando}>
                Cancelar
              </button>
              <button type="button" className="btn-primary" onClick={confirmarExclusao} disabled={salvando}>
                {salvando ? 'Excluindo...' : 'Confirmar Exclusao'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

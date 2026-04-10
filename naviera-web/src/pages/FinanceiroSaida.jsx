import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}

function formatDate(dateStr) {
  if (!dateStr) return '\u2014'
  const d = new Date(dateStr)
  return d.toLocaleDateString('pt-BR')
}

export default function FinanceiroSaida({ viagemAtiva }) {
  const [saidas, setSaidas] = useState([])
  const [loading, setLoading] = useState(false)
  const [toast, setToast] = useState(null)

  // Modal criar
  const [modalCriar, setModalCriar] = useState(false)
  const [form, setForm] = useState({ descricao: '', valor: '', data: '', tipo: '', observacoes: '' })
  const [salvando, setSalvando] = useState(false)

  // Modal excluir
  const [modalExcluir, setModalExcluir] = useState(null)
  const [motivoExclusao, setMotivoExclusao] = useState('')

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  const carregar = useCallback(() => {
    if (!viagemAtiva) return
    setLoading(true)
    api.get(`/financeiro/saidas?viagem_id=${viagemAtiva.id_viagem}`)
      .then(data => setSaidas(Array.isArray(data) ? data : []))
      .catch(() => showToast('Erro ao carregar saidas', 'error'))
      .finally(() => setLoading(false))
  }, [viagemAtiva])

  useEffect(() => {
    carregar()
  }, [carregar])

  // --- Criar ---
  function abrirCriar() {
    setForm({ descricao: '', valor: '', data: '', tipo: '', observacoes: '' })
    setModalCriar(true)
  }

  function handleChange(e) {
    const { name, value } = e.target
    setForm(prev => ({ ...prev, [name]: value }))
  }

  async function handleCriar(e) {
    e.preventDefault()
    if (!form.descricao.trim()) {
      showToast('Preencha a descricao.', 'error')
      return
    }
    if (!form.valor || Number(form.valor) <= 0) {
      showToast('Informe um valor valido.', 'error')
      return
    }
    if (!form.data) {
      showToast('Informe a data.', 'error')
      return
    }

    setSalvando(true)
    try {
      await api.post('/financeiro/saida', {
        id_viagem: viagemAtiva.id_viagem,
        descricao: form.descricao.trim(),
        valor: parseFloat(form.valor),
        data: form.data,
        id_categoria: null,
        tipo: form.tipo.trim() || null,
        observacoes: form.observacoes.trim() || null
      })
      showToast('Saida criada com sucesso!')
      setModalCriar(false)
      carregar()
    } catch (err) {
      showToast(err?.message || 'Erro ao criar saida.', 'error')
    } finally {
      setSalvando(false)
    }
  }

  // --- Excluir ---
  function abrirExcluir(saida) {
    setModalExcluir(saida)
    setMotivoExclusao('')
  }

  async function confirmarExclusao() {
    if (!motivoExclusao.trim()) {
      showToast('Informe o motivo da exclusao.', 'error')
      return
    }
    setSalvando(true)
    try {
      await api.delete(`/financeiro/saida/${modalExcluir.id_despesa || modalExcluir.id}`, {
        motivo: motivoExclusao.trim()
      })
      showToast('Saida excluida com sucesso!')
      setModalExcluir(null)
      carregar()
    } catch (err) {
      showToast(err?.message || 'Erro ao excluir saida.', 'error')
    } finally {
      setSalvando(false)
    }
  }

  if (!viagemAtiva) {
    return (
      <div className="placeholder-page">
        <div className="ph-icon">{'\uD83D\uDCB8'}</div>
        <h2>Saidas Financeiras</h2>
        <p>Selecione uma viagem para gerenciar as saidas.</p>
      </div>
    )
  }

  return (
    <div>
      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}

      <div className="card">
        <div className="card-header">
          <h3>Despesas (Saidas) — {viagemAtiva.descricao || `Viagem #${viagemAtiva.id_viagem}`}</h3>
          <div className="toolbar">
            <span className="badge info">{saidas.length} registros</span>
            <button className="btn-primary" onClick={abrirCriar}>+ Nova Saida</button>
          </div>
        </div>

        {loading ? (
          <p style={{ color: 'var(--text-muted)', padding: 20 }}>Carregando...</p>
        ) : (
          <div className="table-container">
            {saidas.length === 0 ? (
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
                      <td>{s.tipo ? <span className="badge">{s.tipo}</span> : '\u2014'}</td>
                      <td className="money">{formatMoney(s.valor)}</td>
                      <td>
                        <button className="btn-sm danger" onClick={() => abrirExcluir(s)}>Excluir</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}
      </div>

      {/* Modal Nova Saida */}
      {modalCriar && (
        <div className="modal-overlay" onClick={() => setModalCriar(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3>Nova Saida</h3>
            <form onSubmit={handleCriar}>
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
                  <input type="date" name="data" value={form.data} onChange={handleChange} />
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
                <button type="button" className="btn-secondary" onClick={() => setModalCriar(false)} disabled={salvando}>
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
        <div className="modal-overlay" onClick={() => setModalExcluir(null)}>
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
              <button type="button" className="btn-secondary" onClick={() => setModalExcluir(null)} disabled={salvando}>
                Cancelar
              </button>
              <button type="button" className="btn-primary" onClick={confirmarExclusao} disabled={salvando}
                style={{ background: 'var(--danger)' }}>
                {salvando ? 'Excluindo...' : 'Confirmar Exclusao'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

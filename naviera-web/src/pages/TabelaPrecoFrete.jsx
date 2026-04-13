import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}

export default function TabelaPrecoFrete() {
  const [itens, setItens] = useState([])
  const [loading, setLoading] = useState(false)
  const [toast, setToast] = useState(null)
  const [salvando, setSalvando] = useState(false)

  // Modal create/edit
  const [showModal, setShowModal] = useState(false)
  const [editando, setEditando] = useState(null) // null = novo, object = editando
  const [form, setForm] = useState({ nome_item: '', preco_padrao: '' })

  const mostrarToast = useCallback((msg, tipo = 'success') => {
    setToast({ msg, tipo })
    setTimeout(() => setToast(null), 3500)
  }, [])

  const carregarItens = useCallback(() => {
    setLoading(true)
    api.get('/cadastros/itens-frete')
      .then(data => setItens(Array.isArray(data) ? data : []))
      .catch(() => { setItens([]); mostrarToast('Erro ao carregar itens', 'error') })
      .finally(() => setLoading(false))
  }, [mostrarToast])

  useEffect(() => { carregarItens() }, [carregarItens])

  function abrirNovo() {
    setEditando(null)
    setForm({ nome_item: '', preco_padrao: '' })
    setShowModal(true)
  }

  function abrirEditar(item) {
    setEditando(item)
    setForm({ nome_item: item.nome_item || '', preco_padrao: item.preco_padrao || '' })
    setShowModal(true)
  }

  function fecharModal() {
    setShowModal(false)
    setEditando(null)
  }

  function handleChange(e) {
    const { name, value } = e.target
    setForm(prev => ({ ...prev, [name]: value }))
  }

  async function handleSalvar(e) {
    e.preventDefault()
    if (!form.nome_item.trim()) return mostrarToast('Preencha o nome do item.', 'error')

    setSalvando(true)
    try {
      const payload = {
        nome_item: form.nome_item.trim(),
        preco_padrao: parseFloat(form.preco_padrao) || 0
      }
      if (editando) {
        await api.put(`/cadastros/itens-frete/${editando.id}`, payload)
        mostrarToast('Item atualizado com sucesso!')
      } else {
        await api.post('/cadastros/itens-frete', payload)
        mostrarToast('Item criado com sucesso!')
      }
      fecharModal()
      carregarItens()
    } catch (err) {
      mostrarToast(err?.message || 'Erro ao salvar item.', 'error')
    } finally {
      setSalvando(false)
    }
  }

  async function handleDesativar(item) {
    if (!confirm(`Desativar o item "${item.nome_item}"?`)) return
    setSalvando(true)
    try {
      await api.delete(`/cadastros/itens-frete/${item.id}`)
      mostrarToast('Item desativado com sucesso!')
      carregarItens()
    } catch (err) {
      mostrarToast(err?.message || 'Erro ao desativar item.', 'error')
    } finally {
      setSalvando(false)
    }
  }

  return (
    <div>
      {toast && <div className={`toast ${toast.tipo}`}>{toast.msg}</div>}

      <div className="card">
        <div className="card-header">
          <h3>Tabela de Precos - Frete</h3>
          <div className="toolbar">
            <button className="btn-primary" onClick={abrirNovo}>+ Novo Item</button>
          </div>
        </div>

        <div className="table-container">
          {loading ? (
            <p style={{ padding: '1rem', color: 'var(--text-muted)' }}>Carregando...</p>
          ) : itens.length === 0 ? (
            <p style={{ padding: '1rem', color: 'var(--text-muted)' }}>Nenhum item cadastrado.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Nome do Item</th>
                  <th>Preco Padrao</th>
                  <th>Acoes</th>
                </tr>
              </thead>
              <tbody>
                {itens.map((item, idx) => (
                  <tr key={item.id || idx}>
                    <td>{item.nome_item}</td>
                    <td className="money">{formatMoney(item.preco_padrao)}</td>
                    <td style={{ display: 'flex', gap: '0.4rem' }}>
                      <button className="btn-sm" onClick={() => abrirEditar(item)}>Editar</button>
                      <button className="btn-sm danger" onClick={() => handleDesativar(item)}>Desativar</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* Modal Create/Edit */}
      {showModal && (
        <div className="modal-overlay" onClick={fecharModal}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3>{editando ? 'Editar Item Frete' : 'Novo Item Frete'}</h3>
            <form onSubmit={handleSalvar}>
              <div className="form-grid">
                <div className="form-group full-width">
                  <label>Nome do Item *</label>
                  <input type="text" name="nome_item" value={form.nome_item} onChange={handleChange} placeholder="Ex: Carga geral, Veiculo..." autoFocus />
                </div>
                <div className="form-group">
                  <label>Preco Padrao (R$)</label>
                  <input type="number" name="preco_padrao" value={form.preco_padrao} onChange={handleChange} placeholder="0.00" min="0" step="0.01" />
                </div>
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={fecharModal} disabled={salvando}>Cancelar</button>
                <button type="submit" className="btn-primary" disabled={salvando}>{salvando ? 'Salvando...' : 'Salvar'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

const FORM_INICIAL = { origem: '', destino: '' }

export default function CadastroRota({ viagemAtiva, onNavigate }) {
  const [rotas, setRotas] = useState([])
  const [loading, setLoading] = useState(false)
  const [modalAberto, setModalAberto] = useState(false)
  const [editando, setEditando] = useState(null)
  const [form, setForm] = useState(FORM_INICIAL)
  const [salvando, setSalvando] = useState(false)
  const [toast, setToast] = useState(null)

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  const carregar = useCallback(() => {
    setLoading(true)
    api.get('/rotas')
      .then(setRotas)
      .catch(() => showToast('Erro ao carregar rotas', 'error'))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { carregar() }, [carregar])

  function abrirCriar() {
    setEditando(null)
    setForm(FORM_INICIAL)
    setModalAberto(true)
  }

  function abrirEditar(item) {
    setEditando(item)
    setForm({ origem: item.origem || '', destino: item.destino || '' })
    setModalAberto(true)
  }

  function fecharModal() {
    setModalAberto(false)
    setEditando(null)
    setForm(FORM_INICIAL)
  }

  function handleChange(e) {
    const { name, value } = e.target
    setForm(prev => ({ ...prev, [name]: value }))
  }

  async function handleSalvar(e) {
    e.preventDefault()
    if (!form.origem.trim() || !form.destino.trim()) {
      showToast('Preencha origem e destino', 'error')
      return
    }
    setSalvando(true)
    try {
      if (editando) {
        await api.put(`/cadastros/rotas/${editando.id_rota}`, form)
        showToast('Rota atualizada com sucesso')
      } else {
        await api.post('/cadastros/rotas', form)
        showToast('Rota criada com sucesso')
      }
      fecharModal()
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao salvar rota', 'error')
    } finally {
      setSalvando(false)
    }
  }

  async function handleExcluir(item) {
    if (!window.confirm(`Excluir rota "${item.origem} - ${item.destino}"?`)) return
    try {
      await api.delete(`/cadastros/rotas/${item.id_rota}`)
      showToast('Rota excluida com sucesso')
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao excluir rota', 'error')
    }
  }

  return (
    <div className="card">
      <div className="card-header">
        <h2>Cadastro de Rotas</h2>
        <div className="toolbar">
          <button className="btn-primary" onClick={abrirCriar}>+ Nova Rota</button>
        </div>
      </div>

      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Origem</th>
              <th>Destino</th>
              <th>Acoes</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="4">Carregando...</td></tr>
            ) : rotas.length === 0 ? (
              <tr><td colSpan="4">Nenhuma rota cadastrada</td></tr>
            ) : rotas.map(r => (
              <tr key={r.id_rota}>
                <td>{r.id_rota}</td>
                <td>{r.origem || '-'}</td>
                <td>{r.destino || '-'}</td>
                <td>
                  <button className="btn-sm primary" onClick={() => abrirEditar(r)} style={{ marginRight: 4 }}>Editar</button>
                  <button className="btn-sm danger" onClick={() => handleExcluir(r)}>Excluir</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {modalAberto && (
        <div className="modal-overlay" onClick={fecharModal}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3>{editando ? 'Editar Rota' : 'Nova Rota'}</h3>
            <form onSubmit={handleSalvar}>
              <div className="form-grid">
                <div className="form-group">
                  <label>Origem *</label>
                  <input type="text" name="origem" value={form.origem} onChange={handleChange} required />
                </div>
                <div className="form-group">
                  <label>Destino *</label>
                  <input type="text" name="destino" value={form.destino} onChange={handleChange} required />
                </div>
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={fecharModal}>Cancelar</button>
                <button type="submit" className="btn-primary" disabled={salvando}>
                  {salvando ? 'Salvando...' : 'Salvar'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}
    </div>
  )
}

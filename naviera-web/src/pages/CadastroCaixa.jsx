import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

const FORM_INICIAL = { nome: '' }

export default function CadastroCaixa({ viagemAtiva, onNavigate }) {
  const [caixas, setCaixas] = useState([])
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
    api.get('/cadastros/caixas')
      .then(setCaixas)
      .catch(() => showToast('Erro ao carregar caixas', 'error'))
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
    setForm({ nome: item.nome || '' })
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
    if (!form.nome.trim()) { showToast('Informe o nome', 'error'); return }
    setSalvando(true)
    try {
      if (editando) {
        await api.put(`/cadastros/caixas/${editando.id_caixa}`, form)
        showToast('Caixa atualizado com sucesso')
      } else {
        await api.post('/cadastros/caixas', form)
        showToast('Caixa criado com sucesso')
      }
      fecharModal()
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao salvar caixa', 'error')
    } finally {
      setSalvando(false)
    }
  }

  return (
    <div className="card">
      <div className="card-header">
        <h2>Cadastro de Caixas</h2>
        <div className="toolbar">
          <button className="btn-primary" onClick={abrirCriar}>+ Novo Caixa</button>
        </div>
      </div>

      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>Nome</th>
              <th>Acoes</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="2">Carregando...</td></tr>
            ) : caixas.length === 0 ? (
              <tr><td colSpan="2">Nenhum caixa cadastrado</td></tr>
            ) : caixas.map(c => (
              <tr key={c.id_caixa}>
                <td>{c.nome || '-'}</td>
                <td>
                  <button className="btn-sm primary" onClick={() => abrirEditar(c)}>Editar</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {modalAberto && (
        <div className="modal-overlay" onClick={fecharModal}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3>{editando ? 'Editar Caixa' : 'Novo Caixa'}</h3>
            <form onSubmit={handleSalvar}>
              <div className="form-grid">
                <div className="form-group full-width">
                  <label>Nome</label>
                  <input type="text" name="nome" value={form.nome} onChange={handleChange} />
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

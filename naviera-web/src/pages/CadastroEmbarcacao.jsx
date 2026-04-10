import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

const FORM_INICIAL = {
  nome: '',
  registro_capitania: '',
  capacidade_passageiros: '',
  observacoes: ''
}

export default function CadastroEmbarcacao({ viagemAtiva, onNavigate }) {
  const [embarcacoes, setEmbarcacoes] = useState([])
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
    api.get('/op/embarcacoes')
      .then(setEmbarcacoes)
      .catch(() => showToast('Erro ao carregar embarcacoes', 'error'))
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
    setForm({
      nome: item.nome || '',
      registro_capitania: item.registro_capitania || '',
      capacidade_passageiros: item.capacidade_passageiros || '',
      observacoes: item.observacoes || ''
    })
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
    if (!form.nome.trim()) { showToast('Informe o nome da embarcacao', 'error'); return }
    setSalvando(true)
    try {
      const payload = { ...form, capacidade_passageiros: Number(form.capacidade_passageiros) || 0 }
      if (editando) {
        await api.put(`/op/cadastros/embarcacoes/${editando.id_embarcacao}`, payload)
        showToast('Embarcacao atualizada com sucesso')
      } else {
        await api.post('/op/cadastros/embarcacoes', payload)
        showToast('Embarcacao criada com sucesso')
      }
      fecharModal()
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao salvar embarcacao', 'error')
    } finally {
      setSalvando(false)
    }
  }

  return (
    <div className="card">
      <div className="card-header">
        <h2>Cadastro de Embarcacoes</h2>
        <div className="toolbar">
          <button className="btn-primary" onClick={abrirCriar}>+ Nova Embarcacao</button>
        </div>
      </div>

      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>Nome</th>
              <th>Registro Capitania</th>
              <th>Capacidade</th>
              <th>Observacoes</th>
              <th>Acoes</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="5">Carregando...</td></tr>
            ) : embarcacoes.length === 0 ? (
              <tr><td colSpan="5">Nenhuma embarcacao cadastrada</td></tr>
            ) : embarcacoes.map(e => (
              <tr key={e.id_embarcacao}>
                <td>{e.nome || '-'}</td>
                <td>{e.registro_capitania || '-'}</td>
                <td>{e.capacidade_passageiros || '-'}</td>
                <td>{e.observacoes || '-'}</td>
                <td>
                  <button className="btn-sm primary" onClick={() => abrirEditar(e)}>Editar</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {modalAberto && (
        <div className="modal-overlay" onClick={fecharModal}>
          <div className="modal" onClick={ev => ev.stopPropagation()}>
            <h3>{editando ? 'Editar Embarcacao' : 'Nova Embarcacao'}</h3>
            <form onSubmit={handleSalvar}>
              <div className="form-grid">
                <div className="form-group">
                  <label>Nome</label>
                  <input type="text" name="nome" value={form.nome} onChange={handleChange} />
                </div>
                <div className="form-group">
                  <label>Registro Capitania</label>
                  <input type="text" name="registro_capitania" value={form.registro_capitania} onChange={handleChange} />
                </div>
                <div className="form-group">
                  <label>Capacidade Passageiros</label>
                  <input type="number" name="capacidade_passageiros" value={form.capacidade_passageiros} onChange={handleChange} />
                </div>
                <div className="form-group full-width">
                  <label>Observacoes</label>
                  <textarea name="observacoes" value={form.observacoes} onChange={handleChange} rows={3} />
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

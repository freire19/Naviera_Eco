import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

const FORM_INICIAL = {
  id_embarcacao: '',
  id_rota: '',
  data_viagem: '',
  data_chegada: '',
  descricao: ''
}

export default function CadastroViagem({ viagemAtiva, onNavigate }) {
  const [viagens, setViagens] = useState([])
  const [loading, setLoading] = useState(false)
  const [modalAberto, setModalAberto] = useState(false)
  const [editando, setEditando] = useState(null)
  const [form, setForm] = useState(FORM_INICIAL)
  const [salvando, setSalvando] = useState(false)
  const [toast, setToast] = useState(null)

  const [embarcacoes, setEmbarcacoes] = useState([])
  const [rotas, setRotas] = useState([])

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  const carregar = useCallback(() => {
    setLoading(true)
    api.get('/op/viagens')
      .then(setViagens)
      .catch(() => showToast('Erro ao carregar viagens', 'error'))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { carregar() }, [carregar])

  useEffect(() => {
    api.get('/op/cadastros/embarcacoes').then(setEmbarcacoes).catch(() => {})
    api.get('/op/cadastros/rotas').then(setRotas).catch(() => {})
  }, [])

  function abrirCriar() {
    setEditando(null)
    setForm(FORM_INICIAL)
    setModalAberto(true)
  }

  function abrirEditar(item) {
    setEditando(item)
    setForm({
      id_embarcacao: item.id_embarcacao || '',
      id_rota: item.id_rota || '',
      data_viagem: item.data_viagem || '',
      data_chegada: item.data_chegada || '',
      descricao: item.descricao || ''
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
    setSalvando(true)
    try {
      if (editando) {
        await api.put(`/op/viagens/${editando.id_viagem}`, form)
        showToast('Viagem atualizada com sucesso')
      } else {
        await api.post('/op/viagens', form)
        showToast('Viagem criada com sucesso')
      }
      fecharModal()
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao salvar viagem', 'error')
    } finally {
      setSalvando(false)
    }
  }

  async function handleExcluir(item) {
    if (!window.confirm(`Excluir viagem "${item.descricao || item.id_viagem}"?`)) return
    try {
      await api.delete(`/op/viagens/${item.id_viagem}`)
      showToast('Viagem excluida com sucesso')
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao excluir viagem', 'error')
    }
  }

  async function handleToggleAtiva(item) {
    try {
      await api.put(`/op/viagens/${item.id_viagem}/ativar`, { ativa: !item.ativa })
      showToast(item.ativa ? 'Viagem desativada' : 'Viagem ativada')
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao alterar status', 'error')
    }
  }

  return (
    <div className="card">
      <div className="card-header">
        <h2>Cadastro de Viagens</h2>
        <div className="toolbar">
          <button className="btn-primary" onClick={abrirCriar}>+ Nova Viagem</button>
        </div>
      </div>

      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>Data Viagem</th>
              <th>Data Chegada</th>
              <th>Descricao</th>
              <th>Embarcacao</th>
              <th>Rota</th>
              <th>Status</th>
              <th>Acoes</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="7">Carregando...</td></tr>
            ) : viagens.length === 0 ? (
              <tr><td colSpan="7">Nenhuma viagem cadastrada</td></tr>
            ) : viagens.map(v => (
              <tr key={v.id_viagem}>
                <td>{v.data_viagem || '-'}</td>
                <td>{v.data_chegada || '-'}</td>
                <td>{v.descricao || '-'}</td>
                <td>{v.embarcacao || v.nome_embarcacao || '-'}</td>
                <td>{v.rota || v.nome_rota || '-'}</td>
                <td>
                  <span className={`badge ${v.ativa ? 'success' : ''}`}>
                    {v.ativa ? 'Ativa' : 'Inativa'}
                  </span>
                </td>
                <td>
                  <button className="btn-sm primary" onClick={() => handleToggleAtiva(v)}>
                    {v.ativa ? 'Desativar' : 'Ativar'}
                  </button>
                  <button className="btn-sm primary" onClick={() => abrirEditar(v)}>Editar</button>
                  <button className="btn-sm danger" onClick={() => handleExcluir(v)}>Excluir</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {modalAberto && (
        <div className="modal-overlay" onClick={fecharModal}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3>{editando ? 'Editar Viagem' : 'Nova Viagem'}</h3>
            <form onSubmit={handleSalvar}>
              <div className="form-grid">
                <div className="form-group">
                  <label>Embarcacao</label>
                  <select name="id_embarcacao" value={form.id_embarcacao} onChange={handleChange}>
                    <option value="">Selecione...</option>
                    {embarcacoes.map(e => (
                      <option key={e.id_embarcacao} value={e.id_embarcacao}>{e.nome}</option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label>Rota</label>
                  <select name="id_rota" value={form.id_rota} onChange={handleChange}>
                    <option value="">Selecione...</option>
                    {rotas.map(r => (
                      <option key={r.id_rota} value={r.id_rota}>{r.origem} - {r.destino}</option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label>Data Viagem</label>
                  <input type="date" name="data_viagem" value={form.data_viagem} onChange={handleChange} />
                </div>
                <div className="form-group">
                  <label>Data Chegada</label>
                  <input type="date" name="data_chegada" value={form.data_chegada} onChange={handleChange} />
                </div>
                <div className="form-group full-width">
                  <label>Descricao</label>
                  <input type="text" name="descricao" value={form.descricao} onChange={handleChange} />
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

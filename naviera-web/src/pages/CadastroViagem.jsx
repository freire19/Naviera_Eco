import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

const FORM_INICIAL = {
  id_embarcacao: '',
  id_rota: '',
  data_viagem: '',
  data_chegada: '',
  id_horario_saida: '',
  descricao: ''
}

function formatDate(val) {
  if (!val) return '-'
  // Se ja vem formatado DD/MM/YYYY do backend
  if (val.includes('/')) return val
  const d = new Date(val + 'T00:00:00')
  return d.toLocaleDateString('pt-BR')
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
  const [horariosSaida, setHorariosSaida] = useState([])

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  const carregar = useCallback(() => {
    setLoading(true)
    api.get('/viagens')
      .then(setViagens)
      .catch(() => showToast('Erro ao carregar viagens', 'error'))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { carregar() }, [carregar])

  useEffect(() => {
    api.get('/embarcacoes').then(setEmbarcacoes).catch(() => {})
    api.get('/rotas').then(setRotas).catch(() => {})
    api.get('/cadastros/horarios-saida').then(setHorariosSaida).catch(() => {})
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
      data_viagem: item.data_viagem_raw || item.data_viagem || '',
      data_chegada: item.data_chegada_raw || item.data_chegada || '',
      id_horario_saida: item.id_horario_saida || '',
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
    if (!form.id_embarcacao || !form.id_rota || !form.data_viagem || !form.data_chegada) {
      showToast('Preencha todos os campos obrigatorios', 'error')
      return
    }
    setSalvando(true)
    try {
      if (editando) {
        await api.put(`/viagens/${editando.id_viagem}`, form)
        showToast('Viagem atualizada com sucesso')
      } else {
        await api.post('/viagens', form)
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
    if (!window.confirm(`Excluir viagem "${item.descricao || item.id_viagem}"? Todos os lancamentos serao removidos.`)) return
    try {
      await api.delete(`/viagens/${item.id_viagem}`)
      showToast('Viagem excluida com sucesso')
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao excluir viagem', 'error')
    }
  }

  async function handleToggleAtiva(item) {
    try {
      await api.put(`/viagens/${item.id_viagem}/ativar`, { ativa: !item.ativa })
      showToast(item.ativa ? 'Viagem desativada' : 'Viagem ativada para lancamentos')
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao alterar status', 'error')
    }
  }

  return (
    <div className="card">
      <div className="card-header">
        <h2>Cadastro de Viagem</h2>
        <div className="toolbar">
          <button className="btn-primary" onClick={abrirCriar}>+ Nova Viagem</button>
        </div>
      </div>

      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Dt. Viagem</th>
              <th>Horario</th>
              <th>Dt. Chegada</th>
              <th>Descricao</th>
              <th>Embarcacao</th>
              <th>Rota</th>
              <th>Ativa</th>
              <th>Acoes</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="9">Carregando...</td></tr>
            ) : viagens.length === 0 ? (
              <tr><td colSpan="9">Nenhuma viagem cadastrada</td></tr>
            ) : viagens.map(v => (
              <tr key={v.id_viagem}>
                <td>{v.id_viagem}</td>
                <td>{formatDate(v.data_viagem)}</td>
                <td>{v.horario || '-'}</td>
                <td>{formatDate(v.data_chegada)}</td>
                <td>{v.descricao || '-'}</td>
                <td>{v.nome_embarcacao || '-'}</td>
                <td>{v.nome_rota || '-'}</td>
                <td>
                  <span className={`badge ${v.ativa ? 'success' : ''}`}>
                    {v.ativa ? 'Sim' : 'Nao'}
                  </span>
                </td>
                <td>
                  <button className="btn-sm primary" onClick={() => handleToggleAtiva(v)} style={{ marginRight: 4 }}>
                    {v.ativa ? 'Desativar' : 'Ativar'}
                  </button>
                  <button className="btn-sm primary" onClick={() => abrirEditar(v)} style={{ marginRight: 4 }}>Editar</button>
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
                  <label>Embarcacao *</label>
                  <select name="id_embarcacao" value={form.id_embarcacao} onChange={handleChange} required>
                    <option value="">Selecione a Embarcacao</option>
                    {embarcacoes.map(e => (
                      <option key={e.id_embarcacao} value={e.id_embarcacao}>{e.nome}</option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label>Rota *</label>
                  <select name="id_rota" value={form.id_rota} onChange={handleChange} required>
                    <option value="">Selecione a Rota</option>
                    {rotas.map(r => (
                      <option key={r.id_rota} value={r.id_rota}>{r.origem} - {r.destino}</option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label>Data Viagem *</label>
                  <input type="date" name="data_viagem" value={form.data_viagem} onChange={handleChange} required />
                </div>
                <div className="form-group">
                  <label>Data Chegada *</label>
                  <input type="date" name="data_chegada" value={form.data_chegada} onChange={handleChange} required />
                </div>
                <div className="form-group">
                  <label>Horario Saida</label>
                  <select name="id_horario_saida" value={form.id_horario_saida} onChange={handleChange}>
                    <option value="">Selecione o Horario</option>
                    {horariosSaida.map(h => (
                      <option key={h.id_horario_saida} value={h.id_horario_saida}>{h.descricao_horario_saida}</option>
                    ))}
                  </select>
                </div>
                <div className="form-group full-width">
                  <label>Descricao</label>
                  <input type="text" name="descricao" value={form.descricao} onChange={handleChange} placeholder="Ex: Viagem semanal, observacoes" />
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

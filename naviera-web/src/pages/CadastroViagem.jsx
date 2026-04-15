import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

const FORM_INICIAL = {
  id_embarcacao: '',
  id_rota: '',
  data_viagem: '',
  data_chegada: '',
  id_horario_saida: '',
  descricao: '',
  ativa: false
}

function formatDate(val) {
  if (!val) return '-'
  if (val.includes('/')) return val
  const d = new Date(val + 'T00:00:00')
  return d.toLocaleDateString('pt-BR')
}

export default function CadastroViagem() {
  const [viagens, setViagens] = useState([])
  const [loading, setLoading] = useState(false)
  const [selecionado, setSelecionado] = useState(null)
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

  function handleSelectRow(item) {
    setSelecionado(item)
    setForm({
      id_embarcacao: item.id_embarcacao || '',
      id_rota: item.id_rota || '',
      data_viagem: item.data_viagem_raw || item.data_viagem || '',
      data_chegada: item.data_chegada_raw || item.data_chegada || '',
      id_horario_saida: item.id_horario_saida || '',
      descricao: item.descricao || '',
      ativa: !!item.ativa
    })
  }

  function handleNovo() {
    setSelecionado(null)
    setForm(FORM_INICIAL)
  }

  function handleChange(e) {
    const { name, value, type, checked } = e.target
    setForm(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }))
  }

  async function handleSalvar() {
    if (!form.id_embarcacao || !form.id_rota || !form.data_viagem || !form.data_chegada) {
      showToast('Preencha todos os campos obrigatorios', 'error')
      return
    }
    setSalvando(true)
    try {
      const payload = { ...form }
      if (selecionado) {
        await api.put(`/viagens/${selecionado.id_viagem}`, payload)
        showToast('Viagem atualizada com sucesso')
      } else {
        await api.post('/viagens', payload)
        showToast('Viagem criada com sucesso')
      }
      handleNovo()
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao salvar viagem', 'error')
    } finally {
      setSalvando(false)
    }
  }

  async function handleExcluir() {
    if (!selecionado) { showToast('Selecione uma viagem na tabela', 'error'); return }
    if (!window.confirm('Excluir viagem? Todos os lancamentos serao removidos.')) return
    try {
      await api.delete(`/viagens/${selecionado.id_viagem}`)
      showToast('Viagem excluida com sucesso')
      handleNovo()
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao excluir viagem', 'error')
    }
  }

  return (
    <div className="card">
      <h2 style={{ marginBottom: 16 }}>Cadastro de Viagem</h2>

      <div className="cadastro-form-4col">
        <label>ID Viagem:</label>
        <input type="text" value={selecionado?.id_viagem || 'Automatico'} readOnly />

        <label>Embarcacao:</label>
        <select name="id_embarcacao" value={form.id_embarcacao} onChange={handleChange}>
          <option value="">Selecione a Embarcacao</option>
          {embarcacoes.map(e => (
            <option key={e.id_embarcacao} value={e.id_embarcacao}>{e.nome}</option>
          ))}
        </select>

        <label>Data Viagem:</label>
        <input type="date" name="data_viagem" value={form.data_viagem} onChange={handleChange} />

        <label>Data Chegada:</label>
        <input type="date" name="data_chegada" value={form.data_chegada} onChange={handleChange} />

        <label>Rota:</label>
        <select name="id_rota" value={form.id_rota} onChange={handleChange}>
          <option value="">Selecione a Rota</option>
          {rotas.map(r => (
            <option key={r.id_rota} value={r.id_rota}>{r.origem} - {r.destino}</option>
          ))}
        </select>

        <label>Descricao:</label>
        <input type="text" name="descricao" value={form.descricao} onChange={handleChange} placeholder="Ex: Viagem semanal, observacoes" />

        <label>Horario Saida:</label>
        <select name="id_horario_saida" value={form.id_horario_saida} onChange={handleChange}>
          <option value="">Selecione o Horario</option>
          {horariosSaida.map(h => (
            <option key={h.id_horario_saida} value={h.id_horario_saida}>{h.descricao_horario_saida}</option>
          ))}
        </select>

        <div />

        <div className="checkbox-row">
          <input type="checkbox" id="ativa" name="ativa" checked={form.ativa} onChange={handleChange} />
          <label htmlFor="ativa" style={{ textAlign: 'left' }}>Viagem Ativa para Lancamentos</label>
        </div>
      </div>

      <div className="cadastro-buttons">
        <button onClick={handleNovo}>Nova Viagem</button>
        <button onClick={handleSalvar} disabled={salvando}>
          {salvando ? 'Salvando...' : 'Salvar'}
        </button>
        <button onClick={handleExcluir}>Excluir</button>
      </div>

      <div className="table-container" style={{ marginTop: 8 }}>
        <table>
          <thead>
            <tr>
              <th style={{ width: 60 }}>ID</th>
              <th>Dt. Viagem</th>
              <th>Horario</th>
              <th>Dt. Chegada</th>
              <th>Descricao</th>
              <th>Embarcacao</th>
              <th>Rota</th>
              <th style={{ width: 70 }}>Ativa</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="8">Carregando...</td></tr>
            ) : viagens.length === 0 ? (
              <tr><td colSpan="8">Nenhuma viagem cadastrada</td></tr>
            ) : viagens.map(v => (
              <tr key={v.id_viagem}
                  className={`clickable ${selecionado?.id_viagem === v.id_viagem ? 'selected' : ''}`}
                  onClick={() => handleSelectRow(v)}>
                <td>{v.id_viagem}</td>
                <td>{formatDate(v.data_viagem)}</td>
                <td>{v.horario || '-'}</td>
                <td>{formatDate(v.data_chegada)}</td>
                <td>{v.descricao || '-'}</td>
                <td>{v.nome_embarcacao || '-'}</td>
                <td>{v.nome_rota || '-'}</td>
                <td>{v.ativa ? 'Sim' : 'Nao'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}
    </div>
  )
}

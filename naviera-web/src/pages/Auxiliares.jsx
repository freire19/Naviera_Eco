import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

const TABS = [
  { key: 'sexo', label: 'Sexo', endpoint: '/cadastros/sexos', idField: 'id_sexo', nomeField: 'nome_sexo' },
  { key: 'tipo_doc', label: 'Tipo Doc', endpoint: '/cadastros/tipos-documento', idField: 'id_tipo_doc', nomeField: 'nome_tipo_doc' },
  { key: 'nacionalidade', label: 'Nacionalidade', endpoint: '/cadastros/nacionalidades', idField: 'id_nacionalidade', nomeField: 'nome_nacionalidade' },
  { key: 'passagem_aux', label: 'Passagem Aux', endpoint: '/cadastros/tipos-passagem-aux', idField: 'id_tipo_passagem', nomeField: 'nome_tipo_passagem' },
  { key: 'agente_aux', label: 'Agente Aux', endpoint: '/cadastros/agentes', idField: 'id_agente', nomeField: 'nome_agente' },
  { key: 'horario_saida', label: 'Horario Saida', endpoint: '/cadastros/horarios-saida', idField: 'id_horario_saida', nomeField: 'descricao_horario_saida' },
  { key: 'acomodacao', label: 'Acomodacao', endpoint: '/cadastros/acomodacoes', idField: 'id_acomodacao', nomeField: 'nome_acomodacao' }
]

export default function Auxiliares() {
  const [tabAtiva, setTabAtiva] = useState('sexo')
  const [itens, setItens] = useState([])
  const [loading, setLoading] = useState(false)
  const [selecionado, setSelecionado] = useState(null)
  const [nome, setNome] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [toast, setToast] = useState(null)

  const tab = TABS.find(t => t.key === tabAtiva)

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  const carregar = useCallback(() => {
    setLoading(true)
    api.get(tab.endpoint)
      .then(data => setItens(Array.isArray(data) ? data : []))
      .catch(() => showToast('Erro ao carregar dados', 'error'))
      .finally(() => setLoading(false))
  }, [tab.endpoint])

  useEffect(() => { carregar() }, [carregar])

  function handleTabChange(key) {
    setTabAtiva(key)
    setSelecionado(null)
    setNome('')
  }

  function handleSelectRow(item) {
    setSelecionado(item)
    setNome(item[tab.nomeField] || '')
  }

  function handleNovo() {
    setSelecionado(null)
    setNome('')
  }

  async function handleSalvar() {
    if (!nome.trim()) { showToast('Informe o nome', 'error'); return }
    setSalvando(true)
    try {
      const payload = { nome: nome.trim() }
      if (selecionado) {
        await api.put(`${tab.endpoint}/${selecionado[tab.idField]}`, payload)
        showToast('Atualizado com sucesso')
      } else {
        await api.post(tab.endpoint, payload)
        showToast('Criado com sucesso')
      }
      handleNovo()
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao salvar', 'error')
    } finally {
      setSalvando(false)
    }
  }

  async function handleExcluir() {
    if (!selecionado) { showToast('Selecione um item na tabela', 'error'); return }
    if (!window.confirm(`Excluir "${selecionado[tab.nomeField]}"?`)) return
    try {
      await api.delete(`${tab.endpoint}/${selecionado[tab.idField]}`)
      showToast('Excluido com sucesso')
      handleNovo()
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao excluir', 'error')
    }
  }

  return (
    <div className="card">
      <h2 style={{ marginBottom: 16 }}>Tabelas Auxiliares</h2>

      <div className="tab-nav">
        {TABS.map(t => (
          <button key={t.key}
                  className={tabAtiva === t.key ? 'active' : ''}
                  onClick={() => handleTabChange(t.key)}>
            {t.label}
          </button>
        ))}
      </div>

      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>Nome</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td>Carregando...</td></tr>
            ) : itens.length === 0 ? (
              <tr><td>Nenhum item cadastrado</td></tr>
            ) : itens.map(item => (
              <tr key={item[tab.idField]}
                  className={`clickable ${selecionado?.[tab.idField] === item[tab.idField] ? 'selected' : ''}`}
                  onClick={() => handleSelectRow(item)}>
                <td>{item[tab.nomeField] || '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="cadastro-inline-form" style={{ maxWidth: 600 }}>
        <label>Nome:</label>
        <input type="text" value={nome} onChange={e => setNome(e.target.value)} />
      </div>

      <div className="cadastro-buttons">
        <button onClick={handleNovo}>Novo</button>
        <button onClick={handleSalvar} disabled={salvando}>
          {salvando ? 'Salvando...' : 'Salvar'}
        </button>
        <button onClick={handleExcluir}>Excluir</button>
      </div>

      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}
    </div>
  )
}

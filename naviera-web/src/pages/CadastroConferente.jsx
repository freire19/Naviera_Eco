import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

export default function CadastroConferente() {
  const [conferentes, setConferentes] = useState([])
  const [loading, setLoading] = useState(false)
  const [selecionado, setSelecionado] = useState(null)
  const [nome, setNome] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [toast, setToast] = useState(null)

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  const carregar = useCallback(() => {
    setLoading(true)
    api.get('/cadastros/conferentes')
      .then(setConferentes)
      .catch(() => showToast('Erro ao carregar conferentes', 'error'))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { carregar() }, [carregar])

  function handleSelectRow(item) {
    setSelecionado(item)
    setNome(item.nome_conferente || item.nome || '')
  }

  function handleNovo() {
    setSelecionado(null)
    setNome('')
  }

  async function handleSalvar() {
    if (!nome.trim()) { showToast('Informe o nome', 'error'); return }
    setSalvando(true)
    try {
      if (selecionado) {
        await api.put(`/cadastros/conferentes/${selecionado.id_conferente}`, { nome: nome.trim() })
        showToast('Conferente atualizado com sucesso')
      } else {
        await api.post('/cadastros/conferentes', { nome: nome.trim() })
        showToast('Conferente criado com sucesso')
      }
      handleNovo()
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao salvar conferente', 'error')
    } finally {
      setSalvando(false)
    }
  }

  async function handleExcluir() {
    if (!selecionado) { showToast('Selecione um conferente na tabela', 'error'); return }
    if (!window.confirm(`Excluir conferente "${selecionado.nome_conferente || selecionado.nome}"?`)) return
    try {
      await api.delete(`/cadastros/conferentes/${selecionado.id_conferente}`)
      showToast('Conferente excluido com sucesso')
      handleNovo()
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao excluir conferente', 'error')
    }
  }

  return (
    <div className="card">
      <h2 style={{ marginBottom: 16 }}>Cadastro de Conferente</h2>

      <div className="cadastro-inline-form">
        <label>ID:</label>
        <input type="text" value={selecionado?.id_conferente || 'Automatico'} readOnly />

        <label>Nome:</label>
        <input type="text" value={nome} onChange={e => setNome(e.target.value)}
               placeholder="Nome completo do conferente" />
      </div>

      <div className="cadastro-buttons">
        <button onClick={handleNovo}>Novo</button>
        <button onClick={handleSalvar} disabled={salvando}>
          {salvando ? 'Salvando...' : 'Salvar'}
        </button>
        <button onClick={handleExcluir}>Excluir</button>
      </div>

      <div className="table-container" style={{ marginTop: 8 }}>
        <table>
          <thead>
            <tr>
              <th style={{ width: 75 }}>ID</th>
              <th>Nome do Conferente</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="2">Carregando...</td></tr>
            ) : conferentes.length === 0 ? (
              <tr><td colSpan="2">Nenhum conferente cadastrado</td></tr>
            ) : conferentes.map(c => (
              <tr key={c.id_conferente}
                  className={`clickable ${selecionado?.id_conferente === c.id_conferente ? 'selected' : ''}`}
                  onClick={() => handleSelectRow(c)}>
                <td>{c.id_conferente}</td>
                <td>{c.nome_conferente || c.nome || '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}
    </div>
  )
}

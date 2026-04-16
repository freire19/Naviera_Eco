import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

export default function CadastroContatoFrete() {
  const [contatos, setContatos] = useState([])
  const [loading, setLoading] = useState(false)
  const [selecionado, setSelecionado] = useState(null)
  const [nome, setNome] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [toast, setToast] = useState(null)

  function showToast(msg, type = 'success') {
    setToast({ msg, type }); setTimeout(() => setToast(null), 3500)
  }

  const carregar = useCallback(() => {
    setLoading(true)
    api.get('/fretes/contatos')
      .then(setContatos)
      .catch(() => showToast('Erro ao carregar', 'error'))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { carregar() }, [carregar])

  function handleSelectRow(item) {
    setSelecionado(item)
    setNome(item.nome_razao_social || '')
  }

  function handleNovo() {
    setSelecionado(null)
    setNome('')
  }

  async function handleSalvar() {
    if (!nome.trim()) { showToast('Informe o nome', 'error'); return }
    setSalvando(true)
    try {
      await api.post('/fretes/contatos', { nome: nome.trim().toUpperCase() })
      showToast('Contato salvo')
      handleNovo()
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro', 'error')
    } finally {
      setSalvando(false)
    }
  }

  return (
    <div className="card">
      <h2 style={{ marginBottom: 16 }}>Cadastro de Contatos (Frete)</h2>

      <div className="cadastro-inline-form">
        <label>ID:</label>
        <input type="text" value={selecionado?.id || 'Automatico'} readOnly />
        <label>Nome / Razao Social:</label>
        <input type="text" value={nome} onChange={e => setNome(e.target.value)} placeholder="Nome do contato..." />
      </div>

      <div className="cadastro-buttons">
        <button onClick={handleNovo}>Novo</button>
        <button onClick={handleSalvar} disabled={salvando}>{salvando ? 'Salvando...' : 'Salvar'}</button>
      </div>

      <div className="table-container" style={{ marginTop: 8 }}>
        <table>
          <thead><tr><th style={{ width: 60 }}>ID</th><th>Nome / Razao Social</th></tr></thead>
          <tbody>
            {loading ? <tr><td colSpan="2">Carregando...</td></tr>
            : contatos.length === 0 ? <tr><td colSpan="2" style={{ textAlign: 'center', padding: 20, color: 'var(--text-muted)' }}>Nenhum contato cadastrado</td></tr>
            : contatos.map(c => (
              <tr key={c.id} className={`clickable ${selecionado?.id === c.id ? 'selected' : ''}`} onClick={() => handleSelectRow(c)}>
                <td>{c.id}</td>
                <td>{c.nome_razao_social}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}
    </div>
  )
}

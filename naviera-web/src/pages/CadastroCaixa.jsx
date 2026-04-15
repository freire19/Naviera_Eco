import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

export default function CadastroCaixa() {
  const [caixas, setCaixas] = useState([])
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
    api.get('/cadastros/caixas')
      .then(setCaixas)
      .catch(() => showToast('Erro ao carregar caixas', 'error'))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { carregar() }, [carregar])

  function handleSelectRow(item) {
    setSelecionado(item)
    setNome(item.nome_caixa || item.nome || '')
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
        await api.put(`/cadastros/caixas/${selecionado.id_caixa}`, { nome: nome.trim() })
        showToast('Caixa atualizado com sucesso')
      } else {
        await api.post('/cadastros/caixas', { nome: nome.trim() })
        showToast('Caixa criado com sucesso')
      }
      handleNovo()
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao salvar caixa', 'error')
    } finally {
      setSalvando(false)
    }
  }

  async function handleExcluir() {
    if (!selecionado) { showToast('Selecione um caixa na tabela', 'error'); return }
    if (!window.confirm(`Excluir caixa "${selecionado.nome_caixa || selecionado.nome}"?`)) return
    try {
      await api.delete(`/cadastros/caixas/${selecionado.id_caixa}`)
      showToast('Caixa excluido com sucesso')
      handleNovo()
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao excluir caixa', 'error')
    }
  }

  return (
    <div className="card">
      <h2 style={{ marginBottom: 16 }}>Cadastro de Tipos de Caixa</h2>

      <div className="cadastro-inline-form">
        <label>ID:</label>
        <input type="text" value={selecionado?.id_caixa || 'Automatico'} readOnly />

        <label>Nome:</label>
        <input type="text" value={nome} onChange={e => setNome(e.target.value)}
               placeholder="Nome do Tipo de Caixa (Ex: Caixa Principal)" />
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
              <th>Nome do Caixa</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="2">Carregando...</td></tr>
            ) : caixas.length === 0 ? (
              <tr><td colSpan="2">Nenhum caixa cadastrado</td></tr>
            ) : caixas.map(c => (
              <tr key={c.id_caixa}
                  className={`clickable ${selecionado?.id_caixa === c.id_caixa ? 'selected' : ''}`}
                  onClick={() => handleSelectRow(c)}>
                <td>{c.id_caixa}</td>
                <td>{c.nome_caixa || c.nome || '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}
    </div>
  )
}

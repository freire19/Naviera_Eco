import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

const FORM_INICIAL = { origem: '', destino: '' }

export default function CadastroRota() {
  const [rotas, setRotas] = useState([])
  const [loading, setLoading] = useState(false)
  const [selecionado, setSelecionado] = useState(null)
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

  function handleSelectRow(item) {
    setSelecionado(item)
    setForm({ origem: item.origem || '', destino: item.destino || '' })
  }

  function handleNovo() {
    setSelecionado(null)
    setForm(FORM_INICIAL)
  }

  function handleChange(e) {
    setForm(prev => ({ ...prev, [e.target.name]: e.target.value }))
  }

  async function handleSalvar() {
    if (!form.origem.trim() || !form.destino.trim()) {
      showToast('Preencha origem e destino', 'error')
      return
    }
    setSalvando(true)
    try {
      if (selecionado) {
        await api.put(`/cadastros/rotas/${selecionado.id_rota}`, form)
        showToast('Rota atualizada com sucesso')
      } else {
        await api.post('/cadastros/rotas', form)
        showToast('Rota criada com sucesso')
      }
      handleNovo()
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao salvar rota', 'error')
    } finally {
      setSalvando(false)
    }
  }

  async function handleExcluir() {
    if (!selecionado) { showToast('Selecione uma rota na tabela', 'error'); return }
    if (!window.confirm(`Excluir rota "${selecionado.origem} - ${selecionado.destino}"?`)) return
    try {
      await api.delete(`/cadastros/rotas/${selecionado.id_rota}`)
      showToast('Rota excluida com sucesso')
      handleNovo()
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao excluir rota', 'error')
    }
  }

  return (
    <div className="card">
      <h2 style={{ marginBottom: 16 }}>Cadastro de Rotas</h2>

      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th style={{ width: 70 }}>ID</th>
              <th>Origem</th>
              <th>Destino</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="3">Carregando...</td></tr>
            ) : rotas.length === 0 ? (
              <tr><td colSpan="3">Nenhuma rota cadastrada</td></tr>
            ) : rotas.map(r => (
              <tr key={r.id_rota}
                  className={`clickable ${selecionado?.id_rota === r.id_rota ? 'selected' : ''}`}
                  onClick={() => handleSelectRow(r)}>
                <td>{r.id_rota}</td>
                <td>{r.origem || '-'}</td>
                <td>{r.destino || '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="cadastro-inline-form">
        <label>ID:</label>
        <input type="text" value={selecionado?.id_rota || ''} readOnly />

        <label>Origem:</label>
        <input type="text" name="origem" value={form.origem} onChange={handleChange} />

        <label>Destino:</label>
        <input type="text" name="destino" value={form.destino} onChange={handleChange} />
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

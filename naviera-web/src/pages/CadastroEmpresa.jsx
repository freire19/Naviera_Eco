import { useState, useEffect } from 'react'
import { api } from '../api.js'

export default function CadastroEmpresa({ viagemAtiva, onNavigate }) {
  const [form, setForm] = useState({})
  const [loading, setLoading] = useState(false)
  const [salvando, setSalvando] = useState(false)
  const [toast, setToast] = useState(null)

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  useEffect(() => {
    setLoading(true)
    api.get('/cadastros/empresa')
      .then(data => {
        if (Array.isArray(data) && data.length > 0) {
          setForm(data[0])
        } else if (data && !Array.isArray(data)) {
          setForm(data)
        }
      })
      .catch(() => showToast('Erro ao carregar dados da empresa', 'error'))
      .finally(() => setLoading(false))
  }, [])

  function handleChange(e) {
    const { name, value } = e.target
    setForm(prev => ({ ...prev, [name]: value }))
  }

  async function handleSalvar(e) {
    e.preventDefault()
    setSalvando(true)
    try {
      await api.put('/cadastros/empresa', form)
      showToast('Dados da empresa atualizados com sucesso')
    } catch (err) {
      showToast(err.message || 'Erro ao salvar dados da empresa', 'error')
    } finally {
      setSalvando(false)
    }
  }

  if (loading) {
    return <div className="card"><div className="card-header"><h2>Dados da Empresa</h2></div><p style={{ padding: '1rem' }}>Carregando...</p></div>
  }

  return (
    <div className="card">
      <div className="card-header">
        <h2>Dados da Empresa</h2>
      </div>

      <form onSubmit={handleSalvar} style={{ padding: '1.5rem' }}>
        <div className="form-grid">
          <div className="form-group">
            <label>Nome da Empresa</label>
            <input type="text" name="nome" value={form.nome || ''} onChange={handleChange} />
          </div>
          <div className="form-group">
            <label>CNPJ</label>
            <input type="text" name="cnpj" value={form.cnpj || ''} onChange={handleChange} />
          </div>
          <div className="form-group">
            <label>Telefone</label>
            <input type="text" name="telefone" value={form.telefone || ''} onChange={handleChange} />
          </div>
          <div className="form-group">
            <label>Email</label>
            <input type="email" name="email" value={form.email || ''} onChange={handleChange} />
          </div>
          <div className="form-group full-width">
            <label>Endereco</label>
            <input type="text" name="endereco" value={form.endereco || ''} onChange={handleChange} />
          </div>
          <div className="form-group full-width">
            <label>Observacoes</label>
            <textarea name="observacoes" value={form.observacoes || ''} onChange={handleChange} rows={3} />
          </div>
        </div>
        <div className="modal-actions">
          <button type="submit" className="btn-primary" disabled={salvando}>
            {salvando ? 'Salvando...' : 'Salvar'}
          </button>
        </div>
      </form>

      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}
    </div>
  )
}

import { useState, useEffect } from 'react'
import { api } from '../api.js'

const CAMPOS = [
  { key: 'companhia', label: 'Nome da Empresa' },
  { key: 'cnpj', label: 'CNPJ' },
  { key: 'ie', label: 'IE' },
  { key: 'proprietario', label: 'Proprietario' },
  { key: 'gerente', label: 'Gerente' },
  { key: 'comandante', label: 'Comandante' },
  { key: 'nome_embarcacao', label: 'Embarcacao' },
  { key: 'origem_padrao', label: 'Origem Padrao' },
  { key: 'linha_rio_padrao', label: 'Linha/Rio' },
  { key: 'telefone', label: 'Telefone' },
  { key: 'endereco', label: 'Endereco' },
  { key: 'cep', label: 'CEP' },
  { key: 'frase_relatorio', label: 'Frase Relatorio', type: 'textarea' },
  { key: 'recomendacoes_bilhete', label: 'Recomendacoes Bilhete', type: 'textarea' },
]

export default function CadastroEmpresa() {
  const [empresa, setEmpresa] = useState(null)
  const [loading, setLoading] = useState(false)
  const [modalAberto, setModalAberto] = useState(false)
  const [form, setForm] = useState({})
  const [salvando, setSalvando] = useState(false)
  const [toast, setToast] = useState(null)

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  function carregar() {
    setLoading(true)
    api.get('/cadastros/empresa')
      .then(data => {
        const emp = Array.isArray(data) ? data[0] || null : data || null
        setEmpresa(emp)
      })
      .catch(() => showToast('Erro ao carregar dados da empresa', 'error'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { carregar() }, [])

  function abrirEditar() {
    setForm(empresa ? { ...empresa } : {})
    setModalAberto(true)
  }

  function fecharModal() {
    setModalAberto(false)
    setForm({})
  }

  function handleChange(e) {
    const { name, value } = e.target
    setForm(prev => ({ ...prev, [name]: value }))
  }

  async function handleSalvar(e) {
    e.preventDefault()
    setSalvando(true)
    try {
      await api.put('/cadastros/empresa', form)
      showToast('Dados da empresa atualizados')
      fecharModal()
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao salvar', 'error')
    } finally {
      setSalvando(false)
    }
  }

  const camposVisiveis = ['companhia', 'cnpj', 'proprietario', 'telefone', 'endereco', 'gerente', 'comandante', 'nome_embarcacao']

  return (
    <div className="card">
      <div className="card-header">
        <h2>Dados da Empresa</h2>
        <div className="toolbar">
          <button className="btn-primary" onClick={abrirEditar}>Editar</button>
        </div>
      </div>

      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>Empresa</th>
              <th>CNPJ</th>
              <th>Proprietario</th>
              <th>Telefone</th>
              <th>Endereco</th>
              <th>Gerente</th>
              <th>Comandante</th>
              <th>Embarcacao</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="8">Carregando...</td></tr>
            ) : !empresa || !empresa.companhia ? (
              <tr><td colSpan="8">Nenhum dado cadastrado. Clique em "Editar" para configurar.</td></tr>
            ) : (
              <tr>
                {camposVisiveis.map(k => (
                  <td key={k}>{empresa[k] || '-'}</td>
                ))}
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {modalAberto && (
        <div className="modal-overlay" onClick={fecharModal}>
          <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 700 }}>
            <h3>Editar Dados da Empresa</h3>
            <form onSubmit={handleSalvar}>
              <div className="form-grid">
                {CAMPOS.map(c => (
                  <div className={`form-group ${c.type === 'textarea' ? 'full-width' : ''}`} key={c.key}>
                    <label>{c.label}</label>
                    {c.type === 'textarea' ? (
                      <textarea name={c.key} value={form[c.key] || ''} onChange={handleChange} rows={2} />
                    ) : (
                      <input type="text" name={c.key} value={form[c.key] || ''} onChange={handleChange} />
                    )}
                  </div>
                ))}
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

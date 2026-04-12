import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

const FORM_INICIAL = {
  nome: '',
  slug: '',
  cor_primaria: '#1a73e8',
  logo_url: ''
}

export default function AdminEmpresas({ viagemAtiva, onNavigate }) {
  const [empresas, setEmpresas] = useState([])
  const [loading, setLoading] = useState(false)
  const [modalAberto, setModalAberto] = useState(false)
  const [editando, setEditando] = useState(null)
  const [form, setForm] = useState(FORM_INICIAL)
  const [salvando, setSalvando] = useState(false)
  const [toast, setToast] = useState(null)

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  const carregar = useCallback(() => {
    setLoading(true)
    api.get('/admin/empresas')
      .then(setEmpresas)
      .catch(() => showToast('Erro ao carregar empresas', 'error'))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { carregar() }, [carregar])

  function abrirCriar() {
    setEditando(null)
    setForm(FORM_INICIAL)
    setModalAberto(true)
  }

  function abrirEditar(item) {
    setEditando(item)
    setForm({
      nome: item.nome || '',
      slug: item.slug || '',
      cor_primaria: item.cor_primaria || '#1a73e8',
      logo_url: item.logo_url || ''
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
    if (!form.nome.trim()) { showToast('Informe o nome', 'error'); return }
    if (!form.slug.trim()) { showToast('Informe o slug', 'error'); return }

    setSalvando(true)
    try {
      if (editando) {
        await api.put(`/admin/empresas/${editando.id}`, form)
        showToast('Empresa atualizada com sucesso')
      } else {
        await api.post('/admin/empresas', form)
        showToast('Empresa criada com sucesso')
      }
      fecharModal()
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao salvar empresa', 'error')
    } finally {
      setSalvando(false)
    }
  }

  async function toggleAtivo(empresa) {
    try {
      await api.put(`/admin/empresas/${empresa.id}/ativar`)
      showToast(`Empresa ${empresa.ativo ? 'desativada' : 'ativada'} com sucesso`)
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao alterar status', 'error')
    }
  }

  return (
    <div className="card">
      <div className="card-header">
        <h2>Gestao de Empresas</h2>
        <div className="toolbar">
          <button className="btn-primary" onClick={abrirCriar}>+ Nova Empresa</button>
        </div>
      </div>

      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>Nome</th>
              <th>Slug</th>
              <th>Cor</th>
              <th>Status</th>
              <th>Usuarios</th>
              <th>Passagens</th>
              <th>Encomendas</th>
              <th>Fretes</th>
              <th>Acoes</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="9">Carregando...</td></tr>
            ) : empresas.length === 0 ? (
              <tr><td colSpan="9">Nenhuma empresa cadastrada</td></tr>
            ) : empresas.map(e => (
              <tr key={e.id}>
                <td>{e.nome}</td>
                <td><code>{e.slug}</code></td>
                <td>
                  <span
                    style={{
                      display: 'inline-block',
                      width: 20,
                      height: 20,
                      borderRadius: 4,
                      backgroundColor: e.cor_primaria || '#ccc',
                      verticalAlign: 'middle',
                      border: '1px solid var(--border)'
                    }}
                  />
                </td>
                <td>
                  <span className={`badge ${e.ativo ? 'success' : 'error'}`}>
                    {e.ativo ? 'Ativa' : 'Inativa'}
                  </span>
                </td>
                <td>{e.total_usuarios || 0}</td>
                <td>{e.total_passagens || 0}</td>
                <td>{e.total_encomendas || 0}</td>
                <td>{e.total_fretes || 0}</td>
                <td>
                  <button className="btn-sm primary" onClick={() => abrirEditar(e)}>Editar</button>
                  {' '}
                  <button
                    className={`btn-sm ${e.ativo ? 'danger' : 'success'}`}
                    onClick={() => toggleAtivo(e)}
                  >
                    {e.ativo ? 'Desativar' : 'Ativar'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {modalAberto && (
        <div className="modal-overlay" onClick={fecharModal}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3>{editando ? 'Editar Empresa' : 'Nova Empresa'}</h3>
            <form onSubmit={handleSalvar}>
              <div className="form-grid">
                <div className="form-group">
                  <label>Nome</label>
                  <input type="text" name="nome" value={form.nome} onChange={handleChange} />
                </div>
                <div className="form-group">
                  <label>Slug (subdominio)</label>
                  <input type="text" name="slug" value={form.slug} onChange={handleChange} placeholder="ex: saofrancisco" />
                </div>
                <div className="form-group">
                  <label>Cor Primaria</label>
                  <input type="color" name="cor_primaria" value={form.cor_primaria} onChange={handleChange} />
                </div>
                <div className="form-group">
                  <label>Logo URL</label>
                  <input type="text" name="logo_url" value={form.logo_url} onChange={handleChange} placeholder="https://..." />
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

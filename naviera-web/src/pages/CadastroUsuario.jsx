import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

const FORM_INICIAL = {
  nome: '',
  email: '',
  senha: '',
  funcao: '',
  permissao: ''
}

const FUNCOES = ['OPERADOR', 'CONFERENTE', 'CAIXA', 'ADMIN']

export default function CadastroUsuario({ viagemAtiva, onNavigate }) {
  const [usuarios, setUsuarios] = useState([])
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
    api.get('/op/cadastros/usuarios')
      .then(setUsuarios)
      .catch(() => showToast('Erro ao carregar usuarios', 'error'))
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
      email: item.email || '',
      senha: '',
      funcao: item.funcao || '',
      permissao: item.permissao || ''
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
    if (!form.email.trim()) { showToast('Informe o email', 'error'); return }
    if (!editando && !form.senha.trim()) { showToast('Informe a senha', 'error'); return }

    setSalvando(true)
    try {
      const payload = { ...form }
      if (editando && !payload.senha) delete payload.senha

      if (editando) {
        await api.put(`/op/cadastros/usuarios/${editando.id_usuario}`, payload)
        showToast('Usuario atualizado com sucesso')
      } else {
        await api.post('/op/cadastros/usuarios', payload)
        showToast('Usuario criado com sucesso')
      }
      fecharModal()
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao salvar usuario', 'error')
    } finally {
      setSalvando(false)
    }
  }

  return (
    <div className="card">
      <div className="card-header">
        <h2>Cadastro de Usuarios</h2>
        <div className="toolbar">
          <button className="btn-primary" onClick={abrirCriar}>+ Novo Usuario</button>
        </div>
      </div>

      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>Nome</th>
              <th>Email</th>
              <th>Funcao</th>
              <th>Permissao</th>
              <th>Acoes</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="5">Carregando...</td></tr>
            ) : usuarios.length === 0 ? (
              <tr><td colSpan="5">Nenhum usuario cadastrado</td></tr>
            ) : usuarios.map(u => (
              <tr key={u.id_usuario}>
                <td>{u.nome || '-'}</td>
                <td>{u.email || '-'}</td>
                <td>{u.funcao || '-'}</td>
                <td>{u.permissao || '-'}</td>
                <td>
                  <button className="btn-sm primary" onClick={() => abrirEditar(u)}>Editar</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {modalAberto && (
        <div className="modal-overlay" onClick={fecharModal}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3>{editando ? 'Editar Usuario' : 'Novo Usuario'}</h3>
            <form onSubmit={handleSalvar}>
              <div className="form-grid">
                <div className="form-group">
                  <label>Nome</label>
                  <input type="text" name="nome" value={form.nome} onChange={handleChange} />
                </div>
                <div className="form-group">
                  <label>Email</label>
                  <input type="email" name="email" value={form.email} onChange={handleChange} />
                </div>
                <div className="form-group">
                  <label>Senha {editando && '(deixe vazio para manter)'}</label>
                  <input type="password" name="senha" value={form.senha} onChange={handleChange} />
                </div>
                <div className="form-group">
                  <label>Funcao</label>
                  <select name="funcao" value={form.funcao} onChange={handleChange}>
                    <option value="">Selecione...</option>
                    {FUNCOES.map(f => (
                      <option key={f} value={f}>{f}</option>
                    ))}
                  </select>
                </div>
                <div className="form-group full-width">
                  <label>Permissao</label>
                  <input type="text" name="permissao" value={form.permissao} onChange={handleChange} />
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

import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

const FORM_INICIAL = {
  nome_cliente: '',
  telefone: '',
  endereco: ''
}

export default function CadastroClienteEncomenda({ viagemAtiva, onNavigate }) {
  const [clientes, setClientes] = useState([])
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
    api.get('/cadastros/clientes-encomenda')
      .then(setClientes)
      .catch(() => showToast('Erro ao carregar clientes', 'error'))
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
      nome_cliente: item.nome_cliente || '',
      telefone: item.telefone || '',
      endereco: item.endereco || ''
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
    if (!form.nome_cliente.trim()) { showToast('Informe o nome do cliente', 'error'); return }
    setSalvando(true)
    try {
      if (editando) {
        await api.put(`/cadastros/clientes-encomenda/${editando.id_cliente_encomenda}`, form)
        showToast('Cliente atualizado com sucesso')
      } else {
        await api.post('/cadastros/clientes-encomenda', form)
        showToast('Cliente criado com sucesso')
      }
      fecharModal()
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao salvar cliente', 'error')
    } finally {
      setSalvando(false)
    }
  }

  return (
    <div className="card">
      <div className="card-header">
        <h2>Cadastro de Clientes de Encomenda</h2>
        <div className="toolbar">
          <button className="btn-primary" onClick={abrirCriar}>+ Novo Cliente</button>
        </div>
      </div>

      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>Nome</th>
              <th>Telefone</th>
              <th>Endereco</th>
              <th>Acoes</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="4">Carregando...</td></tr>
            ) : clientes.length === 0 ? (
              <tr><td colSpan="4">Nenhum cliente cadastrado</td></tr>
            ) : clientes.map(c => (
              <tr key={c.id_cliente_encomenda}>
                <td>{c.nome_cliente || '-'}</td>
                <td>{c.telefone || '-'}</td>
                <td>{c.endereco || '-'}</td>
                <td>
                  <button className="btn-sm primary" onClick={() => abrirEditar(c)}>Editar</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {modalAberto && (
        <div className="modal-overlay" onClick={fecharModal}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3>{editando ? 'Editar Cliente' : 'Novo Cliente'}</h3>
            <form onSubmit={handleSalvar}>
              <div className="form-grid">
                <div className="form-group">
                  <label>Nome do Cliente</label>
                  <input type="text" name="nome_cliente" value={form.nome_cliente} onChange={handleChange} />
                </div>
                <div className="form-group">
                  <label>Telefone</label>
                  <input type="text" name="telefone" value={form.telefone} onChange={handleChange} />
                </div>
                <div className="form-group full-width">
                  <label>Endereco</label>
                  <input type="text" name="endereco" value={form.endereco} onChange={handleChange} />
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

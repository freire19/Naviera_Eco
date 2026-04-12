import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

const FORM_INICIAL = {
  nome: '', cpf: '', rg: '', ctps: '', telefone: '', endereco: '',
  cargo: '', salario: '', data_admissao: '', data_nascimento: '',
  is_clt: false, recebe_decimo_terceiro: false
}

export default function GestaoFuncionarios({ viagemAtiva, onNavigate }) {
  const [funcionarios, setFuncionarios] = useState([])
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
    api.get('/cadastros/funcionarios')
      .then(setFuncionarios)
      .catch(() => showToast('Erro ao carregar funcionarios', 'error'))
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
      nome: item.nome || '', cpf: item.cpf || '', rg: item.rg || '',
      ctps: item.ctps || '', telefone: item.telefone || '', endereco: item.endereco || '',
      cargo: item.cargo || '', salario: item.salario || '',
      data_admissao: item.data_admissao ? item.data_admissao.slice(0, 10) : '',
      data_nascimento: item.data_nascimento ? item.data_nascimento.slice(0, 10) : '',
      is_clt: item.is_clt || false, recebe_decimo_terceiro: item.recebe_decimo_terceiro || false
    })
    setModalAberto(true)
  }

  function fecharModal() {
    setModalAberto(false)
    setEditando(null)
    setForm(FORM_INICIAL)
  }

  function handleChange(e) {
    const { name, value, type, checked } = e.target
    setForm(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }))
  }

  async function handleSalvar(e) {
    e.preventDefault()
    if (!form.nome.trim()) { showToast('Informe o nome', 'error'); return }
    setSalvando(true)
    try {
      if (editando) {
        await api.put(`/cadastros/funcionarios/${editando.id}`, form)
        showToast('Funcionario atualizado')
      } else {
        await api.post('/cadastros/funcionarios', form)
        showToast('Funcionario cadastrado')
      }
      fecharModal()
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao salvar', 'error')
    } finally {
      setSalvando(false)
    }
  }

  async function handleDesativar(item) {
    if (!confirm(`Desativar ${item.nome}?`)) return
    try {
      await api.delete(`/cadastros/funcionarios/${item.id}`)
      showToast('Funcionario desativado')
      carregar()
    } catch (err) {
      showToast('Erro ao desativar', 'error')
    }
  }

  const fmtMoney = v => v != null ? `R$ ${Number(v).toLocaleString('pt-BR', { minimumFractionDigits: 2 })}` : '-'
  const fmtDate = d => d ? new Date(d + 'T00:00:00').toLocaleDateString('pt-BR') : '-'

  return (
    <div className="card">
      <div className="card-header">
        <h2>Gestao de Funcionarios</h2>
        <div className="toolbar">
          <button className="btn-primary" onClick={abrirCriar}>+ Novo Funcionario</button>
        </div>
      </div>

      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>Nome</th>
              <th>Cargo</th>
              <th>CPF</th>
              <th>Telefone</th>
              <th>Salario</th>
              <th>Admissao</th>
              <th>CLT</th>
              <th>Acoes</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="8">Carregando...</td></tr>
            ) : funcionarios.length === 0 ? (
              <tr><td colSpan="8">Nenhum funcionario ativo</td></tr>
            ) : funcionarios.map(f => (
              <tr key={f.id}>
                <td>{f.nome}</td>
                <td>{f.cargo || '-'}</td>
                <td>{f.cpf || '-'}</td>
                <td>{f.telefone || '-'}</td>
                <td>{fmtMoney(f.salario)}</td>
                <td>{fmtDate(f.data_admissao)}</td>
                <td>{f.is_clt ? 'Sim' : 'Nao'}</td>
                <td>
                  <button className="btn-sm primary" onClick={() => abrirEditar(f)}>Editar</button>{' '}
                  <button className="btn-sm danger" onClick={() => handleDesativar(f)}>Desativar</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {modalAberto && (
        <div className="modal-overlay" onClick={fecharModal}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3>{editando ? 'Editar Funcionario' : 'Novo Funcionario'}</h3>
            <form onSubmit={handleSalvar}>
              <div className="form-grid">
                <div className="form-group">
                  <label>Nome *</label>
                  <input type="text" name="nome" value={form.nome} onChange={handleChange} />
                </div>
                <div className="form-group">
                  <label>Cargo</label>
                  <input type="text" name="cargo" value={form.cargo} onChange={handleChange} />
                </div>
                <div className="form-group">
                  <label>CPF</label>
                  <input type="text" name="cpf" value={form.cpf} onChange={handleChange} />
                </div>
                <div className="form-group">
                  <label>RG</label>
                  <input type="text" name="rg" value={form.rg} onChange={handleChange} />
                </div>
                <div className="form-group">
                  <label>CTPS</label>
                  <input type="text" name="ctps" value={form.ctps} onChange={handleChange} />
                </div>
                <div className="form-group">
                  <label>Telefone</label>
                  <input type="text" name="telefone" value={form.telefone} onChange={handleChange} />
                </div>
                <div className="form-group full-width">
                  <label>Endereco</label>
                  <input type="text" name="endereco" value={form.endereco} onChange={handleChange} />
                </div>
                <div className="form-group">
                  <label>Salario</label>
                  <input type="number" name="salario" step="0.01" value={form.salario} onChange={handleChange} />
                </div>
                <div className="form-group">
                  <label>Data Admissao</label>
                  <input type="date" name="data_admissao" value={form.data_admissao} onChange={handleChange} />
                </div>
                <div className="form-group">
                  <label>Data Nascimento</label>
                  <input type="date" name="data_nascimento" value={form.data_nascimento} onChange={handleChange} />
                </div>
                <div className="form-group">
                  <label style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <input type="checkbox" name="is_clt" checked={form.is_clt} onChange={handleChange} /> CLT
                  </label>
                </div>
                <div className="form-group">
                  <label style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <input type="checkbox" name="recebe_decimo_terceiro" checked={form.recebe_decimo_terceiro} onChange={handleChange} /> 13o Salario
                  </label>
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

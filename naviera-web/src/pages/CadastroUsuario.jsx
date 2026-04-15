import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

const FORM_INICIAL = {
  nome: '',
  email: '',
  senha: '',
  confirmar_senha: '',
  funcao: '',
  permissao: '',
  ativo: true
}

const FUNCOES = ['Administrador', 'Gerente', 'Operador de Caixa', 'Conferente', 'Atendente', 'Outro']
const PERMISSOES = ['TOTAL', 'ADMINISTRATIVO', 'OPERACIONAL_COMPLETO', 'OPERACIONAL_RESTRITO', 'FINANCEIRO', 'CONSULTA_APENAS']

export default function CadastroUsuario() {
  const [usuarios, setUsuarios] = useState([])
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
    api.get('/cadastros/usuarios')
      .then(setUsuarios)
      .catch(() => showToast('Erro ao carregar usuarios', 'error'))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { carregar() }, [carregar])

  function handleSelectRow(item) {
    setSelecionado(item)
    setForm({
      nome: item.nome || '',
      email: item.email || '',
      senha: '',
      confirmar_senha: '',
      funcao: item.funcao || '',
      permissao: item.permissao || '',
      ativo: item.excluido === false || item.excluido === null || !item.excluido
    })
  }

  function handleNovo() {
    setSelecionado(null)
    setForm(FORM_INICIAL)
  }

  function handleChange(e) {
    const { name, value, type, checked } = e.target
    setForm(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }))
  }

  async function handleSalvar() {
    if (!form.nome.trim()) { showToast('Informe o nome completo', 'error'); return }
    if (!selecionado && !form.senha) { showToast('Informe a senha', 'error'); return }
    if (form.senha && form.senha.length < 6) { showToast('Senha deve ter no minimo 6 caracteres', 'error'); return }
    if (form.senha && form.senha !== form.confirmar_senha) { showToast('Senhas nao conferem', 'error'); return }

    setSalvando(true)
    try {
      const payload = {
        nome: form.nome.trim(),
        email: form.email.trim() || null,
        funcao: form.funcao || 'OPERADOR',
        permissao: form.permissao || null
      }
      if (form.senha) payload.senha = form.senha

      if (selecionado) {
        await api.put(`/cadastros/usuarios/${selecionado.id || selecionado.id_usuario}`, payload)
        showToast('Usuario atualizado com sucesso')
      } else {
        if (!form.senha) { showToast('Informe a senha para novo usuario', 'error'); setSalvando(false); return }
        await api.post('/cadastros/usuarios', payload)
        showToast('Usuario criado com sucesso')
      }
      handleNovo()
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao salvar usuario', 'error')
    } finally {
      setSalvando(false)
    }
  }

  async function handleExcluir() {
    if (!selecionado) { showToast('Selecione um usuario na tabela', 'error'); return }
    if (!window.confirm(`Excluir usuario "${selecionado.nome}"?`)) return
    showToast('Funcao de exclusao nao disponivel via web', 'error')
  }

  return (
    <div className="card">
      <h2 style={{ marginBottom: 16 }}>Cadastro de Usuario</h2>

      <div className="cadastro-form-4col">
        <label>ID:</label>
        <input type="text" value={selecionado?.id || selecionado?.id_usuario || 'Automatico'} readOnly />
        <div /><div />

        <label>Nome Completo:</label>
        <input type="text" name="nome" value={form.nome} onChange={handleChange}
               placeholder="Nome completo do usuario" style={{ gridColumn: '2 / -1' }} />

        <label>Login de Usuario:</label>
        <input type="text" value={form.nome.toUpperCase()} readOnly
               title="Login gerado a partir do nome" />

        <label>Email:</label>
        <input type="email" name="email" value={form.email} onChange={handleChange}
               placeholder="email@dominio.com (opcional)" />

        <label>Senha:</label>
        <input type="password" name="senha" value={form.senha} onChange={handleChange}
               placeholder={selecionado ? '(deixe vazio para manter)' : 'Minimo 6 caracteres'} />

        <label>Confirmar Senha:</label>
        <input type="password" name="confirmar_senha" value={form.confirmar_senha} onChange={handleChange}
               placeholder="Repita a senha" />

        <label>Funcao:</label>
        <select name="funcao" value={form.funcao} onChange={handleChange}>
          <option value="">Selecione a funcao</option>
          {FUNCOES.map(f => <option key={f} value={f}>{f}</option>)}
        </select>

        <label>Permissao:</label>
        <select name="permissao" value={form.permissao} onChange={handleChange}>
          <option value="">Selecione o nivel de permissao</option>
          {PERMISSOES.map(p => <option key={p} value={p}>{p}</option>)}
        </select>

        <div className="checkbox-row">
          <input type="checkbox" id="usuario_ativo" name="ativo" checked={form.ativo} onChange={handleChange} />
          <label htmlFor="usuario_ativo" style={{ textAlign: 'left' }}>Usuario Ativo</label>
        </div>
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
              <th style={{ width: 60 }}>ID</th>
              <th>Nome Completo</th>
              <th>Login</th>
              <th>Funcao</th>
              <th style={{ width: 75 }}>Ativo</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="5">Carregando...</td></tr>
            ) : usuarios.length === 0 ? (
              <tr><td colSpan="5">Nenhum usuario cadastrado</td></tr>
            ) : usuarios.map(u => (
              <tr key={u.id || u.id_usuario}
                  className={`clickable ${(selecionado?.id || selecionado?.id_usuario) === (u.id || u.id_usuario) ? 'selected' : ''}`}
                  onClick={() => handleSelectRow(u)}>
                <td>{u.id || u.id_usuario}</td>
                <td>{u.nome || '-'}</td>
                <td>{(u.nome || '').toUpperCase()}</td>
                <td>{u.funcao || '-'}</td>
                <td>{u.excluido ? 'Nao' : 'Sim'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}
    </div>
  )
}

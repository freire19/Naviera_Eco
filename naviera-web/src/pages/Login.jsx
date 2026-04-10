import { useState } from 'react'
import { api } from '../api.js'

export default function Login({ onLogin, theme, toggleTheme }) {
  const [login, setLogin] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setErro('')
    setLoading(true)

    try {
      const data = await api.post('/auth/login', { login, senha })
      onLogin(data)
    } catch (err) {
      setErro(err.message || 'Erro ao fazer login')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="logo-icon">
          <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
            <rect width="48" height="48" rx="12" fill="url(#g)" />
            <path d="M14 28c2-4 6-8 10-8s8 4 10 8" stroke="#fff" strokeWidth="2.5" strokeLinecap="round" />
            <path d="M12 32h24M16 36h16" stroke="#fff" strokeWidth="2" strokeLinecap="round" opacity=".6" />
            <defs><linearGradient id="g" x1="0" y1="0" x2="48" y2="48"><stop stopColor="#059669"/><stop offset="1" stopColor="#34D399"/></linearGradient></defs>
          </svg>
        </div>
        <h1>Naviera</h1>
        <p className="subtitle">Sistema de Gestao — Console Web</p>

        {erro && <div className="error-msg">{erro}</div>}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Login</label>
            <input
              type="text"
              value={login}
              onChange={e => setLogin(e.target.value)}
              placeholder="Seu login"
              autoFocus
              required
            />
          </div>
          <div className="form-group">
            <label>Senha</label>
            <input
              type="password"
              value={senha}
              onChange={e => setSenha(e.target.value)}
              placeholder="Sua senha"
              required
            />
          </div>
          <button className="btn-primary" type="submit" disabled={loading}>
            {loading ? 'Entrando...' : 'Entrar'}
          </button>
        </form>

        <div style={{ textAlign: 'center', marginTop: 16 }}>
          <span className="theme-toggle" onClick={toggleTheme} title="Alternar tema">
            {theme === 'light' ? '\u263E' : '\u2600'}
          </span>
        </div>
      </div>
    </div>
  )
}

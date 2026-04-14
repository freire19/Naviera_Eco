import { useState } from 'react'
import { API } from '../api.js'
import { IconCamera } from '../icons.jsx'

export default function LoginScreen({ t, onLogin }) {
  const [login, setLogin] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    if (!login.trim() || !senha) return
    setLoading(true)
    setErro('')
    try {
      const res = await fetch(`${API}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ login: login.trim(), senha })
      })
      const data = await res.json()
      if (!res.ok) {
        setErro(data.error || 'Credenciais invalidas')
        return
      }
      onLogin(data)
    } catch {
      setErro('Erro de conexao. Verifique sua internet.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{
      minHeight: '100vh', background: t.bg, display: 'flex', flexDirection: 'column',
      alignItems: 'center', justifyContent: 'center', padding: 24
    }}>
      <div style={{ textAlign: 'center', marginBottom: 32 }}>
        <div style={{
          width: 72, height: 72, borderRadius: 20, background: t.priGrad,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          margin: '0 auto 16px'
        }}>
          <IconCamera size={36} color="#fff" />
        </div>
        <h1 style={{ color: t.tx, fontSize: '1.5rem', fontWeight: 700 }}>Naviera OCR</h1>
        <p style={{ color: t.txMuted, fontSize: '0.9rem', marginTop: 4 }}>
          Lancamento de fretes por foto
        </p>
      </div>

      <form onSubmit={submit} style={{
        width: '100%', maxWidth: 360, display: 'flex', flexDirection: 'column', gap: 12
      }}>
        <input
          className="input"
          placeholder="Login ou email"
          value={login}
          onChange={(e) => setLogin(e.target.value)}
          autoComplete="username"
          style={{ background: t.card, color: t.tx, borderColor: t.border }}
        />
        <input
          className="input"
          type="password"
          placeholder="Senha"
          value={senha}
          onChange={(e) => setSenha(e.target.value)}
          autoComplete="current-password"
          style={{ background: t.card, color: t.tx, borderColor: t.border }}
        />

        {erro && (
          <div style={{
            background: t.errBg, color: t.errTx, borderRadius: 8,
            padding: '8px 12px', fontSize: '0.85rem'
          }}>
            {erro}
          </div>
        )}

        <button
          type="submit"
          disabled={loading}
          className="btn btn-block"
          style={{ background: t.priGrad, color: '#fff', marginTop: 4 }}
        >
          {loading ? 'Entrando...' : 'Entrar'}
        </button>
      </form>
    </div>
  )
}

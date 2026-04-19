import { useState, useEffect, useRef } from 'react'

/**
 * Modal generico para confirmar acao sensivel (excluir passagem/encomenda/frete,
 * estornar pagamento etc.) com login + senha + motivo.
 *
 * Props:
 *   titulo, mensagem
 *   onConfirm(payload)  — recebe { login_autorizador, senha_autorizador, motivo }
 *   onClose()
 *   loading             — desabilita botoes
 *   erro                — mensagem em vermelho (ex: "senha invalida")
 */
export default function ModalSenhaAdmin({ titulo, mensagem, onConfirm, onClose, loading, erro }) {
  const [login, setLogin] = useState('')
  const [senha, setSenha] = useState('')
  const [motivo, setMotivo] = useState('')
  const loginRef = useRef(null)

  useEffect(() => {
    setTimeout(() => loginRef.current?.focus(), 50)
    const onKey = (e) => { if (e.key === 'Escape') onClose?.() }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  function submit(e) {
    e.preventDefault()
    if (!login.trim() || !senha.trim() || !motivo.trim()) return
    onConfirm({ login_autorizador: login.trim(), senha_autorizador: senha, motivo: motivo.trim() })
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 420 }}>
        <h3>{titulo || 'Confirmar com senha'}</h3>
        {mensagem && <p style={{ color: 'var(--text-muted)', marginBottom: 10, fontSize: '0.88rem' }}>{mensagem}</p>}

        <form onSubmit={submit}>
          <div style={{ marginBottom: 10 }}>
            <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 600, marginBottom: 3 }}>Autorizador (login ou email):</label>
            <input ref={loginRef} value={login} onChange={e => setLogin(e.target.value)}
              style={{ width: '100%', padding: '8px 10px', border: '1px solid var(--border)', borderRadius: 4, background: 'var(--bg-soft)', color: 'var(--text)' }}
              autoComplete="off" />
          </div>
          <div style={{ marginBottom: 10 }}>
            <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 600, marginBottom: 3 }}>Senha:</label>
            <input type="password" value={senha} onChange={e => setSenha(e.target.value)}
              style={{ width: '100%', padding: '8px 10px', border: '1px solid var(--border)', borderRadius: 4, background: 'var(--bg-soft)', color: 'var(--text)' }}
              autoComplete="off" />
          </div>
          <div style={{ marginBottom: 14 }}>
            <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 600, marginBottom: 3 }}>Motivo:</label>
            <input value={motivo} onChange={e => setMotivo(e.target.value)}
              placeholder="Descreva brevemente a razao..."
              style={{ width: '100%', padding: '8px 10px', border: '1px solid var(--border)', borderRadius: 4, background: 'var(--bg-soft)', color: 'var(--text)' }} />
          </div>

          {erro && <div style={{ color: 'var(--danger)', fontSize: '0.82rem', marginBottom: 10, fontWeight: 600 }}>{erro}</div>}

          <div className="modal-actions">
            <button type="button" className="btn-secondary" onClick={onClose} disabled={loading}>Cancelar (Esc)</button>
            <button type="submit" className="btn-primary" disabled={loading || !login.trim() || !senha.trim() || !motivo.trim()}
              style={{ background: 'var(--danger)' }}>
              {loading ? 'Processando...' : 'Confirmar'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

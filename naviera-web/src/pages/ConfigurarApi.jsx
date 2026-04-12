import { useState, useEffect } from 'react'
import { api } from '../api.js'

export default function ConfigurarApi({ viagemAtiva }) {
  const [health, setHealth] = useState(null)
  const [loading, setLoading] = useState(false)
  const [toast, setToast] = useState(null)

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  async function verificarConexao() {
    setLoading(true)
    try {
      const res = await api.get('/health')
      setHealth({ ok: true, timestamp: res.timestamp })
      showToast('API conectada')
    } catch (err) {
      setHealth({ ok: false, error: err.message })
      showToast('API indisponivel', 'error')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { verificarConexao() }, [])

  return (
    <div className="card">
      <div className="card-header">
        <h2>Configuracao da API</h2>
        <div className="toolbar">
          <button className="btn-primary" onClick={verificarConexao} disabled={loading}>
            {loading ? 'Verificando...' : 'Testar Conexao'}
          </button>
        </div>
      </div>

      <div style={{ padding: '20px' }}>
        <div style={{ display: 'grid', gap: '16px', maxWidth: '500px' }}>
          <div>
            <label style={{ fontWeight: 600, display: 'block', marginBottom: '4px' }}>Endpoint BFF</label>
            <code style={{ padding: '8px 12px', background: 'var(--bg-secondary)', borderRadius: '6px', display: 'block' }}>
              {window.location.origin}/api
            </code>
          </div>

          <div>
            <label style={{ fontWeight: 600, display: 'block', marginBottom: '4px' }}>Status</label>
            <div style={{
              padding: '12px 16px',
              background: health?.ok ? 'var(--success-bg, #e8f5e9)' : 'var(--error-bg, #fce4ec)',
              borderRadius: '6px',
              color: health?.ok ? 'var(--success, #2e7d32)' : 'var(--error, #c62828)'
            }}>
              {health === null ? 'Verificando...' :
                health.ok ? `Conectado (${health.timestamp})` :
                `Erro: ${health.error}`}
            </div>
          </div>

          <div>
            <label style={{ fontWeight: 600, display: 'block', marginBottom: '4px' }}>Versao</label>
            <span>Naviera Web BFF v1.0</span>
          </div>
        </div>
      </div>

      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}
    </div>
  )
}

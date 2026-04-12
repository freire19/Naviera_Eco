import { useState, useEffect } from 'react'
import { api } from '../api.js'

export default function AdminMetricas({ viagemAtiva, onNavigate }) {
  const [metricas, setMetricas] = useState(null)
  const [loading, setLoading] = useState(false)
  const [toast, setToast] = useState(null)

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  useEffect(() => {
    setLoading(true)
    api.get('/admin/metricas')
      .then(setMetricas)
      .catch(() => showToast('Erro ao carregar metricas', 'error'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) {
    return (
      <div className="card">
        <div className="card-header"><h2>Metricas da Plataforma</h2></div>
        <p style={{ padding: '1rem' }}>Carregando...</p>
      </div>
    )
  }

  if (!metricas) {
    return (
      <div className="card">
        <div className="card-header"><h2>Metricas da Plataforma</h2></div>
        <p style={{ padding: '1rem', color: 'var(--text-muted)' }}>Nenhuma metrica disponivel.</p>
      </div>
    )
  }

  const { totais, por_empresa } = metricas

  return (
    <div>
      <div className="dash-grid">
        <div className="stat-card primary">
          <span className="stat-label">Empresas</span>
          <span className="stat-value">{totais.empresas}</span>
          <span className="stat-sub">cadastradas</span>
        </div>
        <div className="stat-card info">
          <span className="stat-label">Usuarios</span>
          <span className="stat-value">{totais.usuarios}</span>
          <span className="stat-sub">ativos na plataforma</span>
        </div>
        <div className="stat-card success">
          <span className="stat-label">Passagens</span>
          <span className="stat-value">{totais.passagens}</span>
          <span className="stat-sub">emitidas</span>
        </div>
        <div className="stat-card warning">
          <span className="stat-label">Encomendas</span>
          <span className="stat-value">{totais.encomendas}</span>
          <span className="stat-sub">registradas</span>
        </div>
        <div className="stat-card primary">
          <span className="stat-label">Fretes</span>
          <span className="stat-value">{totais.fretes}</span>
          <span className="stat-sub">lancados</span>
        </div>
      </div>

      <div className="card" style={{ marginTop: 24 }}>
        <div className="card-header">
          <h2>Metricas por Empresa</h2>
        </div>
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Empresa</th>
                <th>Slug</th>
                <th>Status</th>
                <th>Usuarios</th>
                <th>Passagens</th>
                <th>Encomendas</th>
                <th>Fretes</th>
              </tr>
            </thead>
            <tbody>
              {por_empresa.length === 0 ? (
                <tr><td colSpan="7">Nenhuma empresa cadastrada</td></tr>
              ) : por_empresa.map(e => (
                <tr key={e.id}>
                  <td>{e.nome}</td>
                  <td><code>{e.slug}</code></td>
                  <td>
                    <span className={`badge ${e.ativo ? 'success' : 'error'}`}>
                      {e.ativo ? 'Ativa' : 'Inativa'}
                    </span>
                  </td>
                  <td>{e.total_usuarios}</td>
                  <td>{e.total_passagens}</td>
                  <td>{e.total_encomendas}</td>
                  <td>{e.total_fretes}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}
    </div>
  )
}

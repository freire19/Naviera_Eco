import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'
import { PieChart } from '../components/Charts.jsx'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}

export default function RelatorioFretes({ viagemAtiva }) {
  const [resumo, setResumo] = useState(null)
  const [fretes, setFretes] = useState([])
  const [loading, setLoading] = useState(false)
  const [toast, setToast] = useState(null)

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  const carregar = useCallback(() => {
    if (!viagemAtiva) return
    setLoading(true)
    Promise.all([
      api.get(`/fretes/resumo?viagem_id=${viagemAtiva.id_viagem}`),
      api.get(`/fretes?viagem_id=${viagemAtiva.id_viagem}`)
    ])
      .then(([r, f]) => {
        setResumo(r)
        setFretes(f)
      })
      .catch(() => showToast('Erro ao carregar relatorio de fretes', 'error'))
      .finally(() => setLoading(false))
  }, [viagemAtiva])

  useEffect(() => {
    carregar()
  }, [carregar])

  if (!viagemAtiva) {
    return (
      <div className="placeholder-page">
        <div className="ph-icon">{'\uD83D\uDCCA'}</div>
        <h2>Relatorio de Fretes</h2>
        <p>Selecione uma viagem para ver o relatorio.</p>
      </div>
    )
  }

  // PieChart data: pago vs devedor
  const pagos = fretes.filter(f => f.status === 'PAGO').length
  const pendentes = fretes.filter(f => f.status !== 'PAGO').length
  const pieData = [
    { label: 'Pago', value: pagos, color: '#4ADE80' },
    { label: 'Devedor', value: pendentes, color: '#EF4444' }
  ]

  return (
    <div>
      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}

      {loading ? (
        <p style={{ color: 'var(--text-muted)', padding: 20 }}>Carregando...</p>
      ) : (
        <>
          {resumo && (
            <div className="dash-grid">
              <div className="stat-card primary">
                <span className="stat-label">Total Fretes</span>
                <span className="stat-value">{resumo.total_fretes ?? resumo.total ?? 0}</span>
              </div>
              <div className="stat-card success">
                <span className="stat-label">Valor Total</span>
                <span className="stat-value money">{formatMoney(resumo.valor_total)}</span>
              </div>
              <div className="stat-card info">
                <span className="stat-label">Valor Pago</span>
                <span className="stat-value money">{formatMoney(resumo.valor_pago)}</span>
              </div>
            </div>
          )}

          {/* Chart */}
          {fretes.length > 0 && (
            <div className="dash-grid" style={{ marginTop: '1.5rem' }}>
              <div className="card" style={{ padding: '1rem', textAlign: 'center' }}>
                <h4 style={{ marginBottom: '0.75rem', color: 'var(--text-primary)' }}>Status Pagamento</h4>
                <PieChart data={pieData} size={180} />
              </div>
            </div>
          )}

          <div className="card" style={{ marginTop: '1.5rem' }}>
            <div className="card-header">
              <h3>Fretes — {viagemAtiva.descricao || `Viagem #${viagemAtiva.id_viagem}`}</h3>
              <span className="badge info">{fretes.length} registros</span>
            </div>

            <div className="table-container">
              <table>
                <thead>
                  <tr>
                    <th>Numero</th>
                    <th>Remetente</th>
                    <th>Destinatario</th>
                    <th>Rota</th>
                    <th>Valor Nominal</th>
                    <th>Valor Pago</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {fretes.map(f => (
                    <tr key={f.id_frete}>
                      <td>{f.numero_frete}</td>
                      <td>{f.remetente_nome_temp || '\u2014'}</td>
                      <td>{f.destinatario_nome_temp || '\u2014'}</td>
                      <td>{f.rota_temp || '\u2014'}</td>
                      <td className="money">{formatMoney(f.valor_nominal)}</td>
                      <td className="money">{formatMoney(f.valor_pago)}</td>
                      <td>
                        <span className={`badge ${f.status === 'PAGO' ? 'success' : 'warning'}`}>
                          {f.status || 'Pendente'}
                        </span>
                      </td>
                    </tr>
                  ))}
                  {fretes.length === 0 && (
                    <tr>
                      <td colSpan="7" style={{ textAlign: 'center', padding: 30, color: 'var(--text-muted)' }}>
                        Nenhum frete nesta viagem
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </div>
  )
}

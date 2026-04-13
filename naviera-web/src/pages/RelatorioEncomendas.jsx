import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'
import { PieChart } from '../components/Charts.jsx'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}

export default function RelatorioEncomendas({ viagemAtiva }) {
  const [resumo, setResumo] = useState(null)
  const [encomendas, setEncomendas] = useState([])
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
      api.get(`/encomendas/resumo?viagem_id=${viagemAtiva.id_viagem}`),
      api.get(`/encomendas?viagem_id=${viagemAtiva.id_viagem}`)
    ])
      .then(([r, e]) => {
        setResumo(r)
        setEncomendas(e)
      })
      .catch(() => showToast('Erro ao carregar relatorio de encomendas', 'error'))
      .finally(() => setLoading(false))
  }, [viagemAtiva])

  useEffect(() => {
    carregar()
  }, [carregar])

  if (!viagemAtiva) {
    return (
      <div className="placeholder-page">
        <div className="ph-icon">{'\uD83D\uDCCA'}</div>
        <h2>Relatorio de Encomendas</h2>
        <p>Selecione uma viagem para ver o relatorio.</p>
      </div>
    )
  }

  // PieChart data: entregue vs pendente
  const entregues = encomendas.filter(e => e.entregue).length
  const pendentes = encomendas.filter(e => !e.entregue).length
  const pieData = [
    { label: 'Entregue', value: entregues, color: '#4ADE80' },
    { label: 'Pendente', value: pendentes, color: '#F59E0B' }
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
                <span className="stat-label">Total Encomendas</span>
                <span className="stat-value">{resumo.total_encomendas ?? resumo.total ?? 0}</span>
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
          {encomendas.length > 0 && (
            <div className="dash-grid" style={{ marginTop: '1.5rem' }}>
              <div className="card" style={{ padding: '1rem', textAlign: 'center' }}>
                <h4 style={{ marginBottom: '0.75rem', color: 'var(--text-primary)' }}>Entrega</h4>
                <PieChart data={pieData} size={180} />
              </div>
            </div>
          )}

          <div className="card" style={{ marginTop: '1.5rem' }}>
            <div className="card-header">
              <h3>Encomendas — {viagemAtiva.descricao || `Viagem #${viagemAtiva.id_viagem}`}</h3>
              <span className="badge info">{encomendas.length} registros</span>
            </div>

            <div className="table-container">
              <table>
                <thead>
                  <tr>
                    <th>Numero</th>
                    <th>Remetente</th>
                    <th>Destinatario</th>
                    <th>Volumes</th>
                    <th>Total a Pagar</th>
                    <th>Pagamento</th>
                    <th>Entrega</th>
                  </tr>
                </thead>
                <tbody>
                  {encomendas.map(e => (
                    <tr key={e.id_encomenda}>
                      <td>{e.numero_encomenda}</td>
                      <td>{e.remetente || '\u2014'}</td>
                      <td>{e.destinatario || '\u2014'}</td>
                      <td>{e.total_volumes}</td>
                      <td className="money">{formatMoney(e.total_a_pagar)}</td>
                      <td>
                        <span className={`badge ${e.status_pagamento === 'PAGO' ? 'success' : 'warning'}`}>
                          {e.status_pagamento || 'Pendente'}
                        </span>
                      </td>
                      <td>
                        <span className={`badge ${e.entregue ? 'success' : 'warning'}`}>
                          {e.entregue ? 'Entregue' : 'Pendente'}
                        </span>
                      </td>
                    </tr>
                  ))}
                  {encomendas.length === 0 && (
                    <tr>
                      <td colSpan="7" style={{ textAlign: 'center', padding: 30, color: 'var(--text-muted)' }}>
                        Nenhuma encomenda nesta viagem
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

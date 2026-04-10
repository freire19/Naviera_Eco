import { useState, useEffect } from 'react'
import { api } from '../api.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}

export default function Encomendas({ viagemAtiva }) {
  const [encomendas, setEncomendas] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!viagemAtiva) return
    setLoading(true)
    api.get(`/encomendas?viagem_id=${viagemAtiva.id_viagem}`)
      .then(setEncomendas)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [viagemAtiva])

  if (!viagemAtiva) {
    return (
      <div className="placeholder-page">
        <div className="ph-icon">\uD83D\uDCE6</div>
        <h2>Encomendas</h2>
        <p>Selecione uma viagem para ver as encomendas.</p>
      </div>
    )
  }

  return (
    <div>
      <div className="card">
        <div className="card-header">
          <h3>Encomendas — {viagemAtiva.descricao || `Viagem #${viagemAtiva.id_viagem}`}</h3>
          <span className="badge info">{encomendas.length} registros</span>
        </div>
        {loading ? (
          <p style={{ color: 'var(--text-muted)', padding: 20 }}>Carregando...</p>
        ) : (
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>N.</th>
                  <th>Remetente</th>
                  <th>Destinatario</th>
                  <th>Volumes</th>
                  <th>Total</th>
                  <th>Pago</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {encomendas.map(e => (
                  <tr key={e.id_encomenda}>
                    <td>{e.numero_encomenda}</td>
                    <td>{e.remetente || '—'}</td>
                    <td>{e.destinatario || '—'}</td>
                    <td>{e.total_volumes}</td>
                    <td className="money">{formatMoney(e.total_a_pagar)}</td>
                    <td className="money">{formatMoney(e.valor_pago)}</td>
                    <td>
                      <span className={`badge ${e.status_pagamento === 'PAGO' ? 'success' : 'warning'}`}>
                        {e.status_pagamento || 'Pendente'}
                      </span>
                    </td>
                  </tr>
                ))}
                {encomendas.length === 0 && (
                  <tr><td colSpan="7" style={{ textAlign: 'center', padding: 30, color: 'var(--text-muted)' }}>Nenhuma encomenda nesta viagem</td></tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}

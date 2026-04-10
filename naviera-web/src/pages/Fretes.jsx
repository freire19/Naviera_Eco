import { useState, useEffect } from 'react'
import { api } from '../api.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}

export default function Fretes({ viagemAtiva }) {
  const [fretes, setFretes] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!viagemAtiva) return
    setLoading(true)
    api.get(`/op/fretes?viagem_id=${viagemAtiva.id_viagem}`)
      .then(setFretes)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [viagemAtiva])

  if (!viagemAtiva) {
    return (
      <div className="placeholder-page">
        <div className="ph-icon">\uD83D\uDE9A</div>
        <h2>Fretes</h2>
        <p>Selecione uma viagem para ver os fretes.</p>
      </div>
    )
  }

  return (
    <div>
      <div className="card">
        <div className="card-header">
          <h3>Fretes — {viagemAtiva.descricao || `Viagem #${viagemAtiva.id_viagem}`}</h3>
          <span className="badge info">{fretes.length} registros</span>
        </div>
        {loading ? (
          <p style={{ color: 'var(--text-muted)', padding: 20 }}>Carregando...</p>
        ) : (
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>N. Frete</th>
                  <th>Remetente</th>
                  <th>Destinatario</th>
                  <th>Rota</th>
                  <th>Valor</th>
                  <th>Pago</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {fretes.map(f => (
                  <tr key={f.id_frete}>
                    <td>{f.numero_frete}</td>
                    <td>{f.nome_remetente || '—'}</td>
                    <td>{f.nome_destinatario || '—'}</td>
                    <td>{f.nome_rota || '—'}</td>
                    <td className="money">{formatMoney(f.valor_nominal)}</td>
                    <td className="money">{formatMoney(f.valor_pago)}</td>
                    <td>
                      <span className={`badge ${f.status === 'PAGO' ? 'success' : f.status === 'CANCELADO' ? 'danger' : 'warning'}`}>
                        {f.status || 'Pendente'}
                      </span>
                    </td>
                  </tr>
                ))}
                {fretes.length === 0 && (
                  <tr><td colSpan="7" style={{ textAlign: 'center', padding: 30, color: 'var(--text-muted)' }}>Nenhum frete nesta viagem</td></tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}

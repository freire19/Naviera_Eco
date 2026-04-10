import { useState, useEffect } from 'react'
import { api } from '../api.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}

export default function Passagens({ viagemAtiva }) {
  const [passagens, setPassagens] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!viagemAtiva) return
    setLoading(true)
    api.get(`/passagens?viagem_id=${viagemAtiva.id_viagem}`)
      .then(setPassagens)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [viagemAtiva])

  if (!viagemAtiva) {
    return (
      <div className="placeholder-page">
        <div className="ph-icon">\uD83C\uDFAB</div>
        <h2>Passagens</h2>
        <p>Selecione uma viagem para ver as passagens.</p>
      </div>
    )
  }

  return (
    <div>
      <div className="card">
        <div className="card-header">
          <h3>Passagens — {viagemAtiva.descricao || `Viagem #${viagemAtiva.id_viagem}`}</h3>
          <span className="badge info">{passagens.length} registros</span>
        </div>
        {loading ? (
          <p style={{ color: 'var(--text-muted)', padding: 20 }}>Carregando...</p>
        ) : (
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Bilhete</th>
                  <th>Passageiro</th>
                  <th>Documento</th>
                  <th>Valor Total</th>
                  <th>Pago</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {passagens.map(p => (
                  <tr key={p.id_passagem}>
                    <td>{p.num_bilhete}</td>
                    <td>{p.nome_passageiro || '—'}</td>
                    <td>{p.numero_doc || '—'}</td>
                    <td className="money">{formatMoney(p.valor_total)}</td>
                    <td className="money">{formatMoney(p.valor_pago)}</td>
                    <td>
                      <span className={`badge ${p.devedor ? 'danger' : 'success'}`}>
                        {p.devedor ? 'Devedor' : 'Pago'}
                      </span>
                    </td>
                  </tr>
                ))}
                {passagens.length === 0 && (
                  <tr><td colSpan="6" style={{ textAlign: 'center', padding: 30, color: 'var(--text-muted)' }}>Nenhuma passagem nesta viagem</td></tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}

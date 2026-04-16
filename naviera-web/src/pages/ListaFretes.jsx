import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}

export default function ListaFretes({ viagemAtiva }) {
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
    api.get(`/fretes?viagem_id=${viagemAtiva.id_viagem}`)
      .then(setFretes)
      .catch(() => showToast('Erro ao carregar fretes', 'error'))
      .finally(() => setLoading(false))
  }, [viagemAtiva])

  useEffect(() => {
    carregar()
  }, [carregar])

  if (!viagemAtiva) {
    return (
      <div className="placeholder-page">
        <div className="ph-icon">{'\uD83D\uDE9A'}</div>
        <h2>Lista de Fretes</h2>
        <p>Selecione uma viagem para ver os fretes.</p>
      </div>
    )
  }

  return (
    <div>
      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}

      <div className="card">
        <div className="card-header">
          <h3>Lista de Fretes — {viagemAtiva.descricao || `Viagem #${viagemAtiva.id_viagem}`}</h3>
          <span className="badge info">{fretes.length} fretes</span>
        </div>

        {loading ? (
          <p style={{ color: 'var(--text-muted)', padding: 20 }}>Carregando...</p>
        ) : (
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
                {fretes.map((f, idx) => {
                  const devedor = Math.max(0, (parseFloat(f.valor_total_itens || f.valor_frete_calculado) || 0) - (parseFloat(f.valor_pago) || 0))
                  const status = f.status_frete || (devedor <= 0.01 ? 'PAGO' : 'PENDENTE')
                  return (
                  <tr key={f.id_frete} style={{ background: idx % 2 === 0 ? 'transparent' : 'rgba(255,255,255,0.03)' }}>
                    <td style={{ fontWeight: 700, color: 'var(--primary)' }}>{f.numero_frete}</td>
                    <td>{(f.remetente || f.remetente_nome_temp || '—').toUpperCase()}</td>
                    <td>{(f.destinatario || f.destinatario_nome_temp || '—').toUpperCase()}</td>
                    <td>{f.rota || f.rota_temp || '—'}</td>
                    <td className="money">{formatMoney(f.valor_total_itens || f.valor_frete_calculado)}</td>
                    <td className="money">{formatMoney(f.valor_pago)}</td>
                    <td style={{ fontWeight: 700, color: status === 'PAGO' ? '#059669' : '#DC2626' }}>
                      {status}
                    </td>
                  </tr>
                  )
                })}
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
        )}
      </div>
    </div>
  )
}

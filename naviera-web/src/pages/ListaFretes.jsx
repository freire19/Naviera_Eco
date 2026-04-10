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
        )}
      </div>
    </div>
  )
}

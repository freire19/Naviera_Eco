import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}

export default function ListaEncomendas({ viagemAtiva }) {
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
    api.get(`/op/encomendas?viagem_id=${viagemAtiva.id_viagem}`)
      .then(setEncomendas)
      .catch(() => showToast('Erro ao carregar encomendas', 'error'))
      .finally(() => setLoading(false))
  }, [viagemAtiva])

  useEffect(() => {
    carregar()
  }, [carregar])

  if (!viagemAtiva) {
    return (
      <div className="placeholder-page">
        <div className="ph-icon">{'\uD83D\uDCE6'}</div>
        <h2>Lista de Encomendas</h2>
        <p>Selecione uma viagem para ver as encomendas.</p>
      </div>
    )
  }

  return (
    <div>
      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}

      <div className="card">
        <div className="card-header">
          <h3>Lista de Encomendas — {viagemAtiva.descricao || `Viagem #${viagemAtiva.id_viagem}`}</h3>
          <span className="badge info">{encomendas.length} encomendas</span>
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
        )}
      </div>
    </div>
  )
}

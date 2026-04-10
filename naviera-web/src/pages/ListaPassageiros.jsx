import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}

export default function ListaPassageiros({ viagemAtiva }) {
  const [passagens, setPassagens] = useState([])
  const [loading, setLoading] = useState(false)
  const [toast, setToast] = useState(null)

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  const carregar = useCallback(() => {
    if (!viagemAtiva) return
    setLoading(true)
    api.get(`/op/passagens?viagem_id=${viagemAtiva.id_viagem}`)
      .then(setPassagens)
      .catch(() => showToast('Erro ao carregar passageiros', 'error'))
      .finally(() => setLoading(false))
  }, [viagemAtiva])

  useEffect(() => {
    carregar()
  }, [carregar])

  if (!viagemAtiva) {
    return (
      <div className="placeholder-page">
        <div className="ph-icon">{'\uD83D\uDC65'}</div>
        <h2>Lista de Passageiros</h2>
        <p>Selecione uma viagem para ver os passageiros.</p>
      </div>
    )
  }

  return (
    <div>
      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}

      <div className="card">
        <div className="card-header">
          <h3>Lista de Passageiros — {viagemAtiva.descricao || `Viagem #${viagemAtiva.id_viagem}`}</h3>
          <span className="badge info">{passagens.length} passageiros</span>
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
                  <th>Assento</th>
                  <th>Valor Total</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {passagens.map(p => (
                  <tr key={p.id_passagem}>
                    <td>{p.num_bilhete}</td>
                    <td>{p.nome_passageiro || '\u2014'}</td>
                    <td>{p.numero_doc || '\u2014'}</td>
                    <td>{p.assento || '\u2014'}</td>
                    <td className="money">{formatMoney(p.valor_total)}</td>
                    <td>
                      <span className={`badge ${p.devedor ? 'danger' : 'success'}`}>
                        {p.devedor ? 'Devedor' : 'Pago'}
                      </span>
                    </td>
                  </tr>
                ))}
                {passagens.length === 0 && (
                  <tr>
                    <td colSpan="6" style={{ textAlign: 'center', padding: 30, color: 'var(--text-muted)' }}>
                      Nenhum passageiro nesta viagem
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

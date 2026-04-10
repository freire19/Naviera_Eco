import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}

export default function RelatorioPassagens({ viagemAtiva }) {
  const [resumo, setResumo] = useState(null)
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
    Promise.all([
      api.get(`/passagens/resumo?viagem_id=${viagemAtiva.id_viagem}`),
      api.get(`/passagens?viagem_id=${viagemAtiva.id_viagem}`)
    ])
      .then(([r, p]) => {
        setResumo(r)
        setPassagens(p)
      })
      .catch(() => showToast('Erro ao carregar relatorio de passagens', 'error'))
      .finally(() => setLoading(false))
  }, [viagemAtiva])

  useEffect(() => {
    carregar()
  }, [carregar])

  if (!viagemAtiva) {
    return (
      <div className="placeholder-page">
        <div className="ph-icon">{'\uD83D\uDCCA'}</div>
        <h2>Relatorio de Passagens</h2>
        <p>Selecione uma viagem para ver o relatorio.</p>
      </div>
    )
  }

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
                <span className="stat-label">Total Passagens</span>
                <span className="stat-value">{resumo.total_passagens ?? resumo.total ?? 0}</span>
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

          <div className="card" style={{ marginTop: '1.5rem' }}>
            <div className="card-header">
              <h3>Passagens — {viagemAtiva.descricao || `Viagem #${viagemAtiva.id_viagem}`}</h3>
              <span className="badge info">{passagens.length} registros</span>
            </div>

            <div className="table-container">
              <table>
                <thead>
                  <tr>
                    <th>Bilhete</th>
                    <th>Passageiro</th>
                    <th>Documento</th>
                    <th>Assento</th>
                    <th>Valor Total</th>
                    <th>Valor Pago</th>
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
                      <td className="money">{formatMoney(p.valor_pago)}</td>
                      <td>
                        <span className={`badge ${p.devedor ? 'danger' : 'success'}`}>
                          {p.devedor ? 'Devedor' : 'Pago'}
                        </span>
                      </td>
                    </tr>
                  ))}
                  {passagens.length === 0 && (
                    <tr>
                      <td colSpan="7" style={{ textAlign: 'center', padding: 30, color: 'var(--text-muted)' }}>
                        Nenhuma passagem nesta viagem
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

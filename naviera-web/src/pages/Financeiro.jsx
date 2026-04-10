import { useState, useEffect } from 'react'
import { api } from '../api.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}

export default function Financeiro({ viagemAtiva }) {
  const [balanco, setBalanco] = useState(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!viagemAtiva) return
    setLoading(true)
    api.get(`/op/financeiro/balanco?viagem_id=${viagemAtiva.id_viagem}`)
      .then(setBalanco)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [viagemAtiva])

  if (!viagemAtiva) {
    return (
      <div className="placeholder-page">
        <div className="ph-icon">\uD83D\uDCB0</div>
        <h2>Financeiro</h2>
        <p>Selecione uma viagem para ver o financeiro.</p>
      </div>
    )
  }

  return (
    <div>
      {loading && <p style={{ color: 'var(--text-muted)' }}>Carregando...</p>}

      {balanco && (
        <>
          <div className="dash-grid">
            <div className="stat-card primary">
              <span className="stat-label">Passagens</span>
              <span className="stat-value money">{formatMoney(balanco.receitas.passagens)}</span>
            </div>
            <div className="stat-card info">
              <span className="stat-label">Encomendas</span>
              <span className="stat-value money">{formatMoney(balanco.receitas.encomendas)}</span>
            </div>
            <div className="stat-card warning">
              <span className="stat-label">Fretes</span>
              <span className="stat-value money">{formatMoney(balanco.receitas.fretes)}</span>
            </div>
          </div>

          <div className="dash-grid">
            <div className="stat-card success">
              <span className="stat-label">Total Receitas</span>
              <span className="stat-value money positive">{formatMoney(balanco.totalReceitas)}</span>
            </div>
            <div className="stat-card" style={{ borderLeft: '3px solid var(--danger)' }}>
              <span className="stat-label">Total Despesas</span>
              <span className="stat-value money negative">{formatMoney(balanco.totalDespesas)}</span>
            </div>
            <div className="stat-card" style={{ borderLeft: `3px solid ${balanco.saldo >= 0 ? 'var(--success)' : 'var(--danger)'}` }}>
              <span className="stat-label">Saldo</span>
              <span className={`stat-value money ${balanco.saldo >= 0 ? 'positive' : 'negative'}`}>
                {formatMoney(balanco.saldo)}
              </span>
            </div>
          </div>
        </>
      )}
    </div>
  )
}

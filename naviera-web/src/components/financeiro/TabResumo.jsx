import { BarChart } from '../Charts.jsx'
import { formatMoney } from './formatters.js'

export default function TabResumo({ balanco }) {
  if (!balanco) return null

  const barData = [
    { label: 'Receitas', value: balanco.totalReceitas || 0, color: '#4ADE80' },
    { label: 'Despesas', value: balanco.totalDespesas || 0, color: '#EF4444' }
  ]

  return (
    <>
      {/* Revenue breakdown */}
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

      {/* Totals */}
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

      {/* BarChart */}
      {(balanco.totalReceitas > 0 || balanco.totalDespesas > 0) && (
        <div className="dash-grid" style={{ marginTop: '1rem' }}>
          <div className="card" style={{ padding: '1rem', textAlign: 'center' }}>
            <h4 style={{ marginBottom: '0.75rem', color: 'var(--text-primary)' }}>Receitas vs Despesas</h4>
            <BarChart data={barData} width={300} height={200} />
          </div>
        </div>
      )}
    </>
  )
}

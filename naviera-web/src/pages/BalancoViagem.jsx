import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'
import { PieChart } from '../components/Charts.jsx'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}

export default function BalancoViagem({ viagemAtiva }) {
  const [balanco, setBalanco] = useState(null)
  const [loading, setLoading] = useState(false)
  const [toast, setToast] = useState(null)

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  const carregar = useCallback(() => {
    if (!viagemAtiva) return
    setLoading(true)
    api.get(`/financeiro/balanco?viagem_id=${viagemAtiva.id_viagem}`)
      .then(setBalanco)
      .catch(() => showToast('Erro ao carregar balanco', 'error'))
      .finally(() => setLoading(false))
  }, [viagemAtiva])

  useEffect(() => {
    carregar()
  }, [carregar])

  if (!viagemAtiva) {
    return (
      <div className="placeholder-page">
        <div className="ph-icon">{'\uD83D\uDCB0'}</div>
        <h2>Balanco da Viagem</h2>
        <p>Selecione uma viagem para ver o balanco.</p>
      </div>
    )
  }

  // PieChart: receitas por tipo
  const pieData = balanco ? [
    { label: 'Passagens', value: balanco.receitas?.passagens || 0, color: '#059669' },
    { label: 'Encomendas', value: balanco.receitas?.encomendas || 0, color: '#0EA5E9' },
    { label: 'Fretes', value: balanco.receitas?.fretes || 0, color: '#F59E0B' }
  ] : []

  return (
    <div>
      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}

      <div className="card">
        <div className="card-header">
          <h3>Balanco — {viagemAtiva.descricao || `Viagem #${viagemAtiva.id_viagem}`}</h3>
        </div>
      </div>

      {loading ? (
        <p style={{ color: 'var(--text-muted)', padding: 20 }}>Carregando...</p>
      ) : balanco ? (
        <>
          {/* Receitas por tipo — lancado */}
          <div className="dash-grid" style={{ marginTop: '1.5rem' }}>
            <div className="stat-card primary">
              <span className="stat-label">PASSAGENS</span>
              <span className="stat-value money">{formatMoney(balanco.receitas?.passagens)}</span>
              <span className="stat-sub">Recebido: {formatMoney(balanco.recebido?.passagens)}</span>
            </div>
            <div className="stat-card info">
              <span className="stat-label">ENCOMENDAS</span>
              <span className="stat-value money">{formatMoney(balanco.receitas?.encomendas)}</span>
              <span className="stat-sub">Recebido: {formatMoney(balanco.recebido?.encomendas)}</span>
            </div>
            <div className="stat-card warning">
              <span className="stat-label">FRETES</span>
              <span className="stat-value money">{formatMoney(balanco.receitas?.fretes)}</span>
              <span className="stat-sub">Recebido: {formatMoney(balanco.recebido?.fretes)}</span>
            </div>
          </div>

          {/* PieChart receitas */}
          {(balanco.totalReceitas || 0) > 0 && (
            <div className="dash-grid" style={{ marginTop: '1.5rem' }}>
              <div className="card" style={{ padding: '1rem', textAlign: 'center' }}>
                <h4 style={{ marginBottom: '0.75rem', color: 'var(--text-primary)' }}>Composicao Receitas</h4>
                <PieChart data={pieData} size={200} />
              </div>
            </div>
          )}

          {/* Totais */}
          <div className="dash-grid" style={{ marginTop: '1rem' }}>
            <div className="stat-card success">
              <span className="stat-label">Total Receitas</span>
              <span className="stat-value money">{formatMoney(balanco.totalReceitas)}</span>
            </div>
            <div className="stat-card" style={{ borderLeft: '3px solid var(--danger)' }}>
              <span className="stat-label">Total Despesas</span>
              <span className="stat-value money">{formatMoney(balanco.totalDespesas)}</span>
            </div>
            <div className="stat-card" style={{ borderLeft: `3px solid ${(balanco.saldo ?? 0) >= 0 ? 'var(--success)' : 'var(--danger)'}` }}>
              <span className="stat-label">Saldo Final</span>
              <span className={`stat-value money ${(balanco.saldo ?? 0) >= 0 ? 'positive' : 'negative'}`}>
                {formatMoney(balanco.saldo)}
              </span>
              <span className="stat-sub">{(balanco.saldo ?? 0) >= 0 ? 'Positivo' : 'Negativo'}</span>
            </div>
          </div>
        </>
      ) : (
        <p style={{ color: 'var(--text-muted)', padding: 20 }}>Nenhum dado de balanco disponivel.</p>
      )}
    </div>
  )
}

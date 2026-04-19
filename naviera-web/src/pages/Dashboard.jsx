import { useState, useEffect } from 'react'
import { api } from '../api.js'
import CalendarioEventos from '../components/CalendarioEventos.jsx'

const OCR_URL = window.location.hostname === 'localhost'
  ? `http://${window.location.hostname}:5175`
  : `https://ocr.${window.location.hostname.replace(/^[^.]+\./, '')}`

const QUICK_ACTIONS = [
  { key: 'vender-passagem', icon: '\uD83C\uDFAB', label: 'Vender Passagem' },
  { key: 'listar-passageiros', icon: '\uD83D\uDCCB', label: 'Lista Passageiros' },
  { key: 'nova-encomenda', icon: '\uD83D\uDCE6', label: 'Nova Encomenda' },
  { key: 'listar-encomendas', icon: '\uD83D\uDCCB', label: 'Lista Encomendas' },
  { key: 'lancar-frete', icon: '\uD83D\uDE9A', label: 'Lancar Frete' },
  { key: 'listar-fretes', icon: '\uD83D\uDCCB', label: 'Lista Fretes' },
  { key: '_ocr-frete', icon: '\uD83D\uDCF7', label: 'Frete por Foto', external: true, query: 'tipo=frete' },
  { key: '_ocr-encomenda', icon: '\uD83D\uDCF7', label: 'Encomenda por Foto', external: true, query: 'tipo=encomenda' },
  { key: 'cadastro-viagem', icon: '\u26F4', label: 'Cadastrar Viagem' },
  { key: 'balanco-viagem', icon: '\uD83D\uDCC8', label: 'Balanco Viagem' }
]

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}

export default function Dashboard({ viagemAtiva, onNavigate }) {
  const [resumo, setResumo] = useState(null)

  useEffect(() => {
    if (!viagemAtiva) return
    api.get(`/dashboard/resumo?viagem_id=${viagemAtiva.id_viagem}`)
      .then(setResumo)
      .catch(() => {})
  }, [viagemAtiva])

  return (
    <div>
      {!viagemAtiva && (
        <div className="card" style={{ marginBottom: 24, textAlign: 'center', padding: 40 }}>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
            Selecione uma viagem no topo para ver o painel.
          </p>
        </div>
      )}

      {viagemAtiva && (
        <>
          <div style={{ marginBottom: 20 }}>
            <h2 style={{ fontSize: '1.15rem', fontWeight: 700 }}>
              {viagemAtiva.descricao || `Viagem #${viagemAtiva.id_viagem}`}
            </h2>
            <p style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>
              {viagemAtiva.origem} → {viagemAtiva.destino} | {viagemAtiva.nome_embarcacao}
            </p>
          </div>

          {resumo && (
            <div className="dash-grid">
              <div className="stat-card primary">
                <span className="stat-label">Passageiros</span>
                <span className="stat-value">{resumo.passagens.total}</span>
                <span className="stat-sub">{formatMoney(resumo.passagens.valor)}</span>
              </div>
              <div className="stat-card info">
                <span className="stat-label">Encomendas</span>
                <span className="stat-value">{resumo.encomendas.total}</span>
                <span className="stat-sub">{formatMoney(resumo.encomendas.valor)}</span>
              </div>
              <div className="stat-card warning">
                <span className="stat-label">Fretes</span>
                <span className="stat-value">{resumo.fretes.total}</span>
                <span className="stat-sub">{formatMoney(resumo.fretes.valor)}</span>
              </div>
              <div className="stat-card success">
                <span className="stat-label">Total Geral</span>
                <span className="stat-value">
                  {formatMoney(resumo.passagens.valor + resumo.encomendas.valor + resumo.fretes.valor)}
                </span>
                <span className="stat-sub">receita bruta</span>
              </div>
            </div>
          )}
        </>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: 'minmax(280px, 1fr) minmax(420px, 1.4fr)', gap: 16, alignItems: 'flex-start' }}>
        <div className="card">
          <div className="card-header">
            <h3>Acesso Rapido</h3>
          </div>
          <div className="quick-actions">
            {QUICK_ACTIONS.map(a => a.external ? (
              <a key={a.key} className="quick-action" href={a.query ? `${OCR_URL}/?${a.query}` : OCR_URL} target="_blank" rel="noopener noreferrer"
                style={{ textDecoration: 'none', color: 'inherit' }}>
                <span className="qa-icon">{a.icon}</span>
                {a.label} <span style={{ fontSize: '0.7rem', opacity: 0.5 }}>&#8599;</span>
              </a>
            ) : (
              <button key={a.key} className="quick-action" onClick={() => onNavigate(a.key)}>
                <span className="qa-icon">{a.icon}</span>
                {a.label}
              </button>
            ))}
          </div>
        </div>

        <CalendarioEventos />
      </div>
    </div>
  )
}

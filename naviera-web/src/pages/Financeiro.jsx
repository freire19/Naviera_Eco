import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'
import { PieChart } from '../components/Charts.jsx'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}
function fmtDate(d) {
  if (!d) return ''
  if (typeof d === 'string' && /^\d{2}\/\d{2}\/\d{4}$/.test(d)) return d
  const dt = new Date(d)
  return isNaN(dt) ? String(d) : dt.toLocaleDateString('pt-BR')
}

export default function Financeiro({ viagemAtiva, onNavigate }) {
  const [viagens, setViagens] = useState([])
  const [viagemId, setViagemId] = useState('')
  const [categoria, setCategoria] = useState('Todas')
  const [formaPagto, setFormaPagto] = useState('Todas')
  const [caixa, setCaixa] = useState('Todos')
  const [caixas, setCaixas] = useState([])
  const [dashboard, setDashboard] = useState(null)
  const [loading, setLoading] = useState(false)
  const [toast, setToast] = useState(null)

  function showToast(msg, type = 'success') {
    setToast({ msg, type }); setTimeout(() => setToast(null), 3500)
  }

  // Carregar viagens e caixas
  useEffect(() => {
    api.get('/viagens').then(setViagens).catch(() => {})
    api.get('/financeiro/caixas').then(setCaixas).catch(() => {})
  }, [])

  useEffect(() => {
    if (viagemAtiva && !viagemId) setViagemId(String(viagemAtiva.id_viagem))
  }, [viagemAtiva])

  // Carregar dashboard
  const carregar = useCallback(() => {
    setLoading(true)
    const params = new URLSearchParams()
    if (viagemId) params.append('viagem_id', viagemId)
    if (categoria !== 'Todas') params.append('categoria', categoria)
    if (formaPagto !== 'Todas') params.append('forma_pagto', formaPagto)
    if (caixa !== 'Todos') params.append('caixa', caixa)

    api.get(`/financeiro/dashboard?${params}`)
      .then(setDashboard)
      .catch(() => showToast('Erro ao carregar dados', 'error'))
      .finally(() => setLoading(false))
  }, [viagemId, categoria, formaPagto, caixa])

  useEffect(() => { carregar() }, [carregar])

  // PieChart — receita por categoria
  const pieData = dashboard ? [
    { label: 'Passagens', value: dashboard.categorias?.passagens || 0, color: '#059669' },
    { label: 'Encomendas', value: dashboard.categorias?.encomendas || 0, color: '#0EA5E9' },
    { label: 'Fretes', value: dashboard.categorias?.fretes || 0, color: '#F59E0B' }
  ].filter(d => d.value > 0) : []

  // BarChart simples — formas de pagamento
  const formas = dashboard?.formasPagamento || {}
  const maxForma = Math.max(formas.dinheiro || 0, formas.pix || 0, formas.cartao || 0, 1)

  const viagemSel = viagens.find(v => String(v.id_viagem) === viagemId)

  // Styles
  const S = {
    filterBar: { display: 'flex', alignItems: 'center', gap: 12, padding: '10px 14px', background: '#047857', borderRadius: 6, marginBottom: 16, flexWrap: 'wrap' },
    filterLabel: { fontSize: '0.72rem', fontWeight: 700, color: '#fff' },
    filterSelect: { padding: '6px 10px', fontSize: '0.8rem', background: '#065f46', color: '#fff', border: '1px solid #047857', borderRadius: 4, fontWeight: 600, minWidth: 140 },
    card: { flex: 1, padding: '16px 20px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg-card)' },
    cardLabel: { fontSize: '0.75rem', fontWeight: 700, letterSpacing: '0.05em' },
    cardValue: { fontSize: '1.8rem', fontWeight: 700, fontFamily: 'Space Mono, monospace', margin: '4px 0' },
    cardSub: { fontSize: '0.72rem', color: 'var(--text-muted)' },
    sidebar: { width: 220, flexShrink: 0 },
    sideBtn: { width: '100%', padding: '12px 16px', border: '1px solid var(--border)', borderRadius: 8, background: 'var(--bg-card)', cursor: 'pointer', fontSize: '0.85rem', fontWeight: 600, color: 'var(--text)', marginBottom: 8, textAlign: 'center', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 },
  }

  if (!viagemAtiva && !viagemId) {
    return <div className="placeholder-page"><div className="ph-icon">💰</div><h2>Lancar Entrada Financeira</h2><p>Selecione uma viagem.</p></div>
  }

  return (
    <div>
      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}

      {/* HEADER */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
        <div>
          <h3 style={{ margin: 0, fontSize: '0.95rem' }}>Visao Geral ({categoria})</h3>
          <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>Data de Hoje: {new Date().toLocaleDateString('pt-BR')}</span>
        </div>
        <button style={{ padding: '8px 20px', background: '#047857', color: '#fff', border: 'none', borderRadius: 4, fontWeight: 700, cursor: 'pointer', fontSize: '0.82rem' }}
          onClick={carregar}>ATUALIZAR DADOS</button>
      </div>

      {/* FILTROS */}
      <div style={S.filterBar}>
        <span style={{ ...S.filterLabel, marginRight: 4 }}>FILTRAR POR:</span>

        <div>
          <div style={S.filterLabel}>Viagem / Data</div>
          <select style={S.filterSelect} value={viagemId} onChange={e => setViagemId(e.target.value)}>
            <option value="">Todas as Viagens</option>
            {viagens.map(v => (
              <option key={v.id_viagem} value={v.id_viagem}>
                {v.id_viagem} ({fmtDate(v.data_viagem)}{v.data_chegada ? ' - ' + fmtDate(v.data_chegada) : ''})
              </option>
            ))}
          </select>
        </div>

        <div>
          <div style={S.filterLabel}>Categoria</div>
          <select style={S.filterSelect} value={categoria} onChange={e => setCategoria(e.target.value)}>
            <option>Todas</option>
            <option>PASSAGEM</option>
            <option>ENCOMENDA</option>
            <option>FRETE</option>
          </select>
        </div>

        <div>
          <div style={S.filterLabel}>Forma Pagto</div>
          <select style={S.filterSelect} value={formaPagto} onChange={e => setFormaPagto(e.target.value)}>
            <option>Todas</option>
            <option>DINHEIRO</option>
            <option>PIX</option>
            <option>CARTAO</option>
            <option>BOLETO</option>
          </select>
        </div>

        <div>
          <div style={S.filterLabel}>Caixa/Usuario</div>
          <select style={S.filterSelect} value={caixa} onChange={e => setCaixa(e.target.value)}>
            <option>Todos</option>
            {caixas.map(c => <option key={c} value={c}>{c}</option>)}
          </select>
        </div>
      </div>

      <div style={{ display: 'flex', gap: 16 }}>
        {/* MAIN CONTENT */}
        <div style={{ flex: 1, minWidth: 0 }}>

          {/* 3 CARDS */}
          <div style={{ display: 'flex', gap: 12, marginBottom: 16 }}>
            <div style={{ ...S.card, borderLeft: '4px solid #9ca3af' }}>
              <div style={{ ...S.cardLabel, color: '#9ca3af' }}>FATURAMENTO TOTAL</div>
              <div style={S.cardValue}>{formatMoney(dashboard?.totalGeral)}</div>
              <div style={S.cardSub}>Soma Geral</div>
            </div>
            <div style={{ ...S.card, borderLeft: '4px solid #059669' }}>
              <div style={{ ...S.cardLabel, color: '#059669' }}>TOTAL RECEBIDO (CAIXA)</div>
              <div style={{ ...S.cardValue, color: '#059669' }}>{formatMoney(dashboard?.totalRecebido)}</div>
              <div style={S.cardSub}>Confirmados</div>
            </div>
            <div style={{ ...S.card, borderLeft: '4px solid #DC2626' }}>
              <div style={{ ...S.cardLabel, color: '#DC2626' }}>A RECEBER (PENDENTE)</div>
              <div style={{ ...S.cardValue, color: '#DC2626' }}>{formatMoney(dashboard?.pendente)}</div>
              <div style={S.cardSub}>Falta receber</div>
            </div>
          </div>

          {/* 2 GRAFICOS */}
          <div style={{ display: 'flex', gap: 12 }}>
            {/* Pie — Receita por Categoria */}
            <div className="card" style={{ flex: 1, padding: 16, textAlign: 'center' }}>
              <h4 style={{ margin: '0 0 12px', fontSize: '0.9rem' }}>Receita por Categoria</h4>
              {pieData.length > 0 ? (
                <PieChart data={pieData} size={180} />
              ) : (
                <div style={{ padding: 40, color: 'var(--text-muted)' }}>Sem dados</div>
              )}
            </div>

            {/* Bar — Formas de Pagamento */}
            <div className="card" style={{ flex: 1, padding: 16 }}>
              <h4 style={{ margin: '0 0 12px', fontSize: '0.9rem', textAlign: 'center' }}>Formas de Pagamento (Recebido)</h4>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10, padding: '10px 0' }}>
                {[
                  { label: 'Dinheiro', value: formas.dinheiro || 0, color: '#059669' },
                  { label: 'Pix', value: formas.pix || 0, color: '#0EA5E9' },
                  { label: 'Cartao', value: formas.cartao || 0, color: '#F59E0B' },
                ].map(f => (
                  <div key={f.label}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.78rem', marginBottom: 3 }}>
                      <span style={{ fontWeight: 600 }}>{f.label}</span>
                      <span style={{ fontFamily: 'Space Mono, monospace' }}>{formatMoney(f.value)}</span>
                    </div>
                    <div style={{ background: 'var(--bg-soft)', borderRadius: 4, height: 20, overflow: 'hidden' }}>
                      <div style={{ width: `${Math.max((f.value / maxForma) * 100, 0)}%`, height: '100%', background: f.color, borderRadius: 4, transition: 'width 0.3s' }} />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>

        </div>

        {/* SIDEBAR DIREITA */}
        <div style={S.sidebar}>
          <div className="card" style={{ padding: 14 }}>
            <h4 style={{ margin: '0 0 4px', fontSize: '0.85rem', textAlign: 'center' }}>Acesso Rapido</h4>
            <p style={{ textAlign: 'center', fontSize: '0.72rem', color: 'var(--text-muted)', margin: '0 0 12px' }}>Ver lista detalhada:</p>

            <button style={S.sideBtn} onClick={() => onNavigate && onNavigate('financeiro-encomendas')}>
              📦 ENCOMENDAS
            </button>
            <button style={S.sideBtn} onClick={() => onNavigate && onNavigate('financeiro-passagens')}>
              🎫 PASSAGENS
            </button>
            <button style={S.sideBtn} onClick={() => onNavigate && onNavigate('financeiro-fretes')}>
              🚚 FRETES
            </button>
            <button style={S.sideBtn} onClick={() => onNavigate && onNavigate('balanco-viagem')}>
              📋 LISTA GERAL
            </button>
            <button style={{ ...S.sideBtn, background: '#047857', color: '#fff', border: 'none' }}
              onClick={() => onNavigate && onNavigate('dashboard')}>
              🚪 SAIR
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

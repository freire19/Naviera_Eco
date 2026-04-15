import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}
function formatDate(val) {
  if (!val) return '—'
  const s = String(val)
  if (s.includes('/')) return s
  try {
    const d = new Date(s.includes('T') ? s : s + 'T00:00:00')
    if (isNaN(d.getTime())) return '—'
    return d.toLocaleDateString('pt-BR')
  } catch { return '—' }
}

export default function RelatorioPassagens({ viagemAtiva, onNavigate }) {
  const [passagens, setPassagens] = useState([])
  const [loading, setLoading] = useState(false)
  const [toast, setToast] = useState(null)

  // Dados auxiliares para filtros
  const [viagens, setViagens] = useState([])
  const [rotas, setRotas] = useState([])
  const [agentes, setAgentes] = useState([])
  const [tiposPassagem, setTiposPassagem] = useState([])
  const [caixas, setCaixas] = useState([])

  // Filtros
  const [filtroStatus, setFiltroStatus] = useState('')
  const [filtroPeriodoDe, setFiltroPeriodoDe] = useState('')
  const [filtroPeriodoAte, setFiltroPeriodoAte] = useState('')
  const [filtroViagem, setFiltroViagem] = useState(viagemAtiva?.id_viagem || '')
  const [filtroRota, setFiltroRota] = useState('')
  const [filtroAgente, setFiltroAgente] = useState('')
  const [filtroTipo, setFiltroTipo] = useState('')
  const [filtroFormaPgto, setFiltroFormaPgto] = useState('')
  const [filtroCaixa, setFiltroCaixa] = useState('')

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  // Carregar auxiliares
  useEffect(() => {
    Promise.allSettled([
      api.get('/viagens').then(setViagens),
      api.get('/rotas').then(setRotas),
      api.get('/cadastros/agentes').then(setAgentes),
      api.get('/cadastros/tipos-passageiro').then(setTiposPassagem),
      api.get('/cadastros/caixas').then(setCaixas)
    ]).catch(() => {})
  }, [])

  // Carregar passagens
  const carregar = useCallback(() => {
    const vid = filtroViagem || viagemAtiva?.id_viagem
    if (!vid) return
    setLoading(true)
    api.get(`/passagens?viagem_id=${vid}`)
      .then(setPassagens)
      .catch(() => showToast('Erro ao carregar', 'error'))
      .finally(() => setLoading(false))
  }, [filtroViagem, viagemAtiva])

  useEffect(() => { carregar() }, [carregar])

  // Aplicar filtros locais
  const filtradas = passagens.filter(p => {
    if (filtroStatus && p.status_passagem !== filtroStatus) return false
    if (filtroRota && String(p.id_rota) !== filtroRota) return false
    if (filtroAgente && String(p.id_agente) !== filtroAgente) return false
    if (filtroTipo && String(p.id_tipo_passagem) !== filtroTipo) return false
    if (filtroCaixa && String(p.id_caixa) !== filtroCaixa) return false
    if (filtroFormaPgto) {
      const forma = buildFormaPgto(p)
      if (!forma.toLowerCase().includes(filtroFormaPgto.toLowerCase())) return false
    }
    return true
  })

  function buildFormaPgto(p) {
    const parts = []
    if (parseFloat(p.valor_pagamento_dinheiro) > 0) parts.push('Dinheiro')
    if (parseFloat(p.valor_pagamento_pix) > 0) parts.push('PIX')
    if (parseFloat(p.valor_pagamento_cartao) > 0) parts.push('Cartao')
    return parts.length ? parts.join(', ') : '—'
  }

  // Totais
  const totalVendido = filtradas.reduce((s, p) => s + (parseFloat(p.valor_total) || 0), 0)
  const totalRecebido = filtradas.reduce((s, p) => s + (parseFloat(p.valor_pago) || 0), 0)
  const totalAReceber = totalVendido - totalRecebido

  const I = { padding: '7px 10px', fontSize: '0.82rem', background: 'var(--bg-soft)', border: '1px solid var(--border)', borderRadius: 4, color: 'var(--text)', fontFamily: 'Sora, sans-serif', width: '100%', boxSizing: 'border-box' }
  const L = { fontSize: '0.72rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: 3, display: 'block' }

  if (!viagemAtiva) {
    return (
      <div className="placeholder-page">
        <div className="ph-icon">📊</div>
        <h2>Relatorio de Passagens</h2>
        <p>Selecione uma viagem para ver o relatorio.</p>
      </div>
    )
  }

  return (
    <div style={{ display: 'flex', gap: 16 }}>
      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}

      {/* CONTEUDO PRINCIPAL (esquerda) */}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div className="card" style={{ marginBottom: 16 }}>
          <h2 style={{ textAlign: 'center', marginBottom: 16 }}>Relatorio Financeiro de Passagens</h2>

          {/* Totais */}
          <div style={{ display: 'flex', justifyContent: 'center', gap: 32, marginBottom: 16 }}>
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600 }}>Total Vendido</div>
              <div style={{ fontSize: '1.2rem', fontWeight: 700, fontFamily: 'Space Mono, monospace' }}>{formatMoney(totalVendido)}</div>
            </div>
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600 }}>Total Recebido</div>
              <div style={{ fontSize: '1.2rem', fontWeight: 700, fontFamily: 'Space Mono, monospace', color: 'var(--success)' }}>{formatMoney(totalRecebido)}</div>
            </div>
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600 }}>Total a Receber</div>
              <div style={{ fontSize: '1.2rem', fontWeight: 700, fontFamily: 'Space Mono, monospace', color: totalAReceber > 0 ? 'var(--danger)' : undefined }}>{formatMoney(totalAReceber)}</div>
            </div>
          </div>
        </div>

        {/* Tabela */}
        <div className="card">
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Bilhete</th>
                  <th>Data Viagem</th>
                  <th>Rota</th>
                  <th>Tipo Passagem</th>
                  <th>Agente</th>
                  <th>Valor Total</th>
                  <th>Valor Pago</th>
                  <th>Devedor</th>
                  <th>Forma Pag.</th>
                  <th>Caixa</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr><td colSpan="10">Carregando...</td></tr>
                ) : filtradas.length === 0 ? (
                  <tr><td colSpan="10" style={{ textAlign: 'center', padding: 40, color: 'var(--text-muted)' }}>
                    Nenhum dado encontrado para os filtros selecionados.
                  </td></tr>
                ) : filtradas.map(p => (
                  <tr key={p.id_passagem}>
                    <td>{p.numero_bilhete || '—'}</td>
                    <td>{formatDate(p.data_emissao)}</td>
                    <td>{p.origem && p.destino ? `${p.origem} - ${p.destino}` : p.nome_rota || '—'}</td>
                    <td>{p.nome_tipo_passagem || '—'}</td>
                    <td>{p.nome_agente || '—'}</td>
                    <td className="money">{formatMoney(p.valor_total)}</td>
                    <td className="money">{formatMoney(p.valor_pago)}</td>
                    <td className="money" style={{ color: parseFloat(p.valor_devedor) > 0 ? 'var(--danger)' : undefined }}>
                      {formatMoney(p.valor_devedor)}
                    </td>
                    <td>{buildFormaPgto(p)}</td>
                    <td>{p.nome_caixa || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* PAINEL FILTROS (direita) */}
      <div style={{ width: 260, flexShrink: 0 }}>
        <div className="card" style={{ position: 'sticky', top: 60 }}>
          <h3 style={{ marginBottom: 12, fontSize: '0.95rem' }}>Filtros do Relatorio</h3>

          <div style={{ marginBottom: 10 }}>
            <label style={L}>Status Pagamento:</label>
            <select style={I} value={filtroStatus} onChange={e => setFiltroStatus(e.target.value)}>
              <option value="">Todos</option>
              <option value="PAGO">PAGO</option>
              <option value="PENDENTE">PENDENTE</option>
              <option value="PARCIAL">PARCIAL</option>
            </select>
          </div>

          <div style={{ marginBottom: 10 }}>
            <label style={L}>Periodo De:</label>
            <input type="date" style={I} value={filtroPeriodoDe} onChange={e => setFiltroPeriodoDe(e.target.value)} />
          </div>
          <div style={{ marginBottom: 10 }}>
            <label style={L}>Ate:</label>
            <input type="date" style={I} value={filtroPeriodoAte} onChange={e => setFiltroPeriodoAte(e.target.value)} />
          </div>

          <div style={{ marginBottom: 10 }}>
            <label style={L}>Viagem:</label>
            <select style={I} value={filtroViagem} onChange={e => setFiltroViagem(e.target.value)}>
              {viagens.map(v => (
                <option key={v.id_viagem} value={v.id_viagem}>
                  {v.id_viagem} - {v.data_viagem} - {v.nome_rota || ''}
                </option>
              ))}
            </select>
          </div>

          <div style={{ marginBottom: 10 }}>
            <label style={L}>Rota:</label>
            <select style={I} value={filtroRota} onChange={e => setFiltroRota(e.target.value)}>
              <option value="">Todas</option>
              {rotas.map(r => <option key={r.id_rota} value={r.id_rota}>{r.origem} - {r.destino}</option>)}
            </select>
          </div>

          <div style={{ marginBottom: 10 }}>
            <label style={L}>Agente:</label>
            <select style={I} value={filtroAgente} onChange={e => setFiltroAgente(e.target.value)}>
              <option value="">Todos</option>
              {agentes.map(a => <option key={a.id_agente} value={a.id_agente}>{a.nome_agente}</option>)}
            </select>
          </div>

          <div style={{ marginBottom: 10 }}>
            <label style={L}>Tipo Passagem:</label>
            <select style={I} value={filtroTipo} onChange={e => setFiltroTipo(e.target.value)}>
              <option value="">Todos</option>
              {tiposPassagem.map(t => <option key={t.id || t.id_tipo_passagem} value={t.id || t.id_tipo_passagem}>{t.nome || t.nome_tipo_passagem}</option>)}
            </select>
          </div>

          <div style={{ marginBottom: 10 }}>
            <label style={L}>Forma de Pagamento:</label>
            <select style={I} value={filtroFormaPgto} onChange={e => setFiltroFormaPgto(e.target.value)}>
              <option value="">Todos</option>
              <option value="Dinheiro">Dinheiro</option>
              <option value="PIX">PIX</option>
              <option value="Cartao">Cartao</option>
            </select>
          </div>

          <div style={{ marginBottom: 16 }}>
            <label style={L}>Caixa:</label>
            <select style={I} value={filtroCaixa} onChange={e => setFiltroCaixa(e.target.value)}>
              <option value="">Todos</option>
              {caixas.map(c => <option key={c.id_caixa} value={c.id_caixa}>{c.nome_caixa || c.nome}</option>)}
            </select>
          </div>

          <button className="btn-primary" style={{ marginBottom: 8 }}
                  onClick={() => window.print()}>
            Imprimir Relatorio
          </button>
          {onNavigate && (
            <button className="btn-secondary" style={{ width: '100%' }}
                    onClick={() => onNavigate('vender-passagem')}>
              SAIR (ESC)
            </button>
          )}
        </div>
      </div>
    </div>
  )
}

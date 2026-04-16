import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}
function formatDate(val) {
  if (!val) return '—'
  const s = String(val)
  if (/^\d{2}\/\d{2}\/\d{4}$/.test(s)) return s
  try { const p = (s.includes('T') ? s.substring(0,10) : s).split('-'); return p.length === 3 ? `${p[2]}/${p[1]}/${p[0]}` : s } catch { return s }
}

export default function ListaFretes({ viagemAtiva, onNavigate, onClose }) {
  const [fretes, setFretes] = useState([])
  const [loading, setLoading] = useState(false)
  const [toast, setToast] = useState(null)

  // Filtros
  const [viagens, setViagens] = useState([])
  const [filtroViagem, setFiltroViagem] = useState(viagemAtiva?.id_viagem || '')
  const [filtroStatus, setFiltroStatus] = useState('')
  const [filtroItem, setFiltroItem] = useState('')
  const [filtroNumero, setFiltroNumero] = useState('')
  const [filtroRemetente, setFiltroRemetente] = useState('')
  const [filtroDestinatario, setFiltroDestinatario] = useState('')
  const [filtrosVisiveis, setFiltrosVisiveis] = useState(true)

  function showToast(msg, type = 'success') {
    setToast({ msg, type }); setTimeout(() => setToast(null), 3500)
  }

  useEffect(() => { api.get('/viagens').then(setViagens).catch(() => {}) }, [])

  const carregar = useCallback(() => {
    if (!filtroViagem) return
    setLoading(true)
    api.get(`/fretes?viagem_id=${filtroViagem}`)
      .then(setFretes)
      .catch(() => showToast('Erro ao carregar', 'error'))
      .finally(() => setLoading(false))
  }, [filtroViagem])

  useEffect(() => { carregar() }, [carregar])

  // Filtros locais
  const filtrados = fretes.filter(f => {
    if (filtroStatus === 'PAGO' && f.status_frete !== 'PAGO') return false
    if (filtroStatus === 'PENDENTE' && f.status_frete === 'PAGO') return false
    if (filtroStatus === 'CANCELADO' && f.status_frete !== 'CANCELADO') return false
    if (filtroNumero && !String(f.numero_frete).includes(filtroNumero)) return false
    if (filtroRemetente && !(f.remetente || '').toLowerCase().includes(filtroRemetente.toLowerCase())) return false
    if (filtroDestinatario && !(f.destinatario || '').toLowerCase().includes(filtroDestinatario.toLowerCase())) return false
    return true
  })

  const totalLancado = filtrados.reduce((s, f) => s + (parseFloat(f.valor_total_itens || f.valor_frete_calculado) || 0), 0)
  const totalRecebido = filtrados.reduce((s, f) => s + (parseFloat(f.valor_pago) || 0), 0)
  const totalAReceber = totalLancado - totalRecebido
  const totalVolumes = filtrados.reduce((s, f) => s + (parseInt(f.total_volumes) || 0), 0)

  const I = { padding: '7px 10px', fontSize: '0.82rem', background: 'var(--bg-soft)', border: '1px solid var(--border)', borderRadius: 4, color: 'var(--text)', fontFamily: 'Sora, sans-serif', width: '100%', boxSizing: 'border-box' }
  const L = { fontSize: '0.72rem', fontWeight: 700, color: 'var(--text)', marginBottom: 3, display: 'block' }

  if (!viagemAtiva) {
    return <div className="placeholder-page"><div className="ph-icon">🚚</div><h2>Lista de Fretes</h2><p>Selecione uma viagem.</p></div>
  }

  return (
    <div style={{ display: 'flex', gap: 0 }}>
      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}

      {/* CONTEUDO PRINCIPAL */}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div className="card" style={{ marginBottom: 0 }}>
          <h2 style={{ textAlign: 'center', marginBottom: 12 }}>Gerenciamento de Fretes da Viagem</h2>

          {/* TABELA */}
          <div className="table-container">
            <table>
              <thead><tr>
                <th style={{ width: 60 }}>N° Frete</th>
                <th>Remetente</th>
                <th>Destinatario</th>
                <th>Rota/Viagem</th>
                <th>Data Viagem</th>
                <th>Emissao</th>
                <th>Vlr. Frete</th>
                <th>A Receber</th>
                <th>Recebido</th>
                <th>Conferente</th>
              </tr></thead>
              <tbody>
                {loading ? <tr><td colSpan="10">Carregando...</td></tr>
                : filtrados.length === 0 ? <tr><td colSpan="10" style={{ textAlign: 'center', padding: 30, color: 'var(--text-muted)' }}>Nenhum frete nesta viagem</td></tr>
                : filtrados.map((f, idx) => {
                  const valor = parseFloat(f.valor_total_itens || f.valor_frete_calculado) || 0
                  const pago = parseFloat(f.valor_pago) || 0
                  const devedor = Math.max(0, valor - pago)
                  return (
                    <tr key={f.id_frete} style={{ background: idx % 2 === 0 ? 'transparent' : 'rgba(255,255,255,0.03)' }}>
                      <td style={{ fontWeight: 700, color: 'var(--primary)', fontSize: '0.85rem' }}>{f.numero_frete}</td>
                      <td>{(f.remetente || '—').toUpperCase()}</td>
                      <td>{(f.destinatario || '—').toUpperCase()}</td>
                      <td>{f.rota || '—'}</td>
                      <td>{formatDate(f.data_emissao)}</td>
                      <td>{formatDate(f.data_emissao)}</td>
                      <td className="money" style={{ fontWeight: 700, color: 'var(--primary)' }}>{formatMoney(valor)}</td>
                      <td className="money" style={{ fontWeight: 700, color: devedor > 0 ? '#DC2626' : undefined }}>{formatMoney(devedor)}</td>
                      <td className="money" style={{ color: '#059669' }}>{formatMoney(pago)}</td>
                      <td>{f.conferente || '—'}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>

          {/* TOTAIS */}
          <div style={{ display: 'flex', justifyContent: 'center', gap: 24, padding: '12px 0', borderTop: '1px solid var(--border)', fontSize: '0.85rem', flexWrap: 'wrap' }}>
            <span>Total Lancado: <strong style={{ color: 'var(--primary)' }}>{formatMoney(totalLancado)}</strong></span>
            <span>|</span>
            <span>Total Recebido: <strong style={{ color: 'var(--primary)' }}>{formatMoney(totalRecebido)}</strong></span>
            <span>|</span>
            <span>Total a Receber: <strong style={{ color: '#DC2626' }}>{formatMoney(totalAReceber)}</strong></span>
            <span>|</span>
            <span>Lancamentos: <strong style={{ color: 'var(--primary)' }}>{filtrados.length}</strong></span>
            <span>|</span>
            <span>Volumes: <strong style={{ color: '#F59E0B' }}>{totalVolumes}</strong></span>
          </div>

          {/* BOTOES */}
          <div style={{ display: 'flex', gap: 8, justifyContent: 'center', padding: '8px 0', flexWrap: 'wrap' }}>
            <button className="btn-sm primary" onClick={() => {}}>Fretes a Receber</button>
            <button className="btn-sm primary" onClick={() => {}}>Nota de Frete</button>
            <button className="btn-sm primary" onClick={() => {}}>Imprimir Lista</button>
            <button className="btn-sm primary" onClick={carregar}>Atualizar Lista</button>
            <button className="btn-sm danger" onClick={() => onClose ? onClose() : onNavigate && onNavigate('lancar-frete')}>Fechar</button>
          </div>
        </div>
      </div>

      {/* TOGGLE FILTROS */}
      <div onClick={() => setFiltrosVisiveis(prev => !prev)}
        style={{ writingMode: 'vertical-rl', cursor: 'pointer', padding: '12px 4px', background: 'var(--bg-card)', border: '1px solid var(--border)', borderRadius: '4px 0 0 4px', fontSize: '0.72rem', fontWeight: 700, color: 'var(--primary)', userSelect: 'none' }}>
        {filtrosVisiveis ? '▶' : '◀'} FILTROS
      </div>

      {/* FILTROS LATERAIS */}
      {filtrosVisiveis && (
      <div style={{ width: 260, flexShrink: 0 }}>
        <div className="card" style={{ position: 'sticky', top: 60 }}>

          <div style={{ marginBottom: 10 }}>
            <label style={L}>Status Pagamento:</label>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 4, fontSize: '0.82rem' }}>
              <label><input type="radio" name="stf" checked={filtroStatus === ''} onChange={() => setFiltroStatus('')} /> Todos</label>
              <label style={{ color: '#059669' }}><input type="radio" name="stf" checked={filtroStatus === 'PAGO'} onChange={() => setFiltroStatus('PAGO')} /> Quitados (Pago)</label>
              <label style={{ color: '#DC2626' }}><input type="radio" name="stf" checked={filtroStatus === 'PENDENTE'} onChange={() => setFiltroStatus('PENDENTE')} /> A Receber (Pendente)</label>
              <label style={{ color: 'var(--text-muted)' }}><input type="radio" name="stf" checked={filtroStatus === 'CANCELADO'} onChange={() => setFiltroStatus('CANCELADO')} /> Cancelado</label>
            </div>
          </div>

          <div style={{ marginBottom: 10 }}>
            <label style={L}>Filtrar por Viagem:</label>
            <select style={I} value={filtroViagem} onChange={e => setFiltroViagem(e.target.value)}>
              {viagens.map(v => <option key={v.id_viagem} value={v.id_viagem}>{v.data_viagem} - {v.nome_rota || ''}</option>)}
            </select>
          </div>

          <div style={{ marginBottom: 10 }}>
            <label style={L}>Filtrar N° Frete:</label>
            <input style={I} placeholder="Digite o N° Frete" value={filtroNumero} onChange={e => setFiltroNumero(e.target.value)} />
          </div>

          <div style={{ marginBottom: 10 }}>
            <label style={L}>Remetente:</label>
            <input style={I} placeholder="Todos" value={filtroRemetente} onChange={e => setFiltroRemetente(e.target.value)} />
          </div>

          <div style={{ marginBottom: 10 }}>
            <label style={L}>Destinatario:</label>
            <input style={I} placeholder="Todos" value={filtroDestinatario} onChange={e => setFiltroDestinatario(e.target.value)} />
          </div>

          <div style={{ marginBottom: 10 }}>
            <label style={L}>Buscar por Item:</label>
            <input style={I} placeholder="Digite a descricao..." value={filtroItem} onChange={e => setFiltroItem(e.target.value)} />
          </div>

        </div>
      </div>
      )}
    </div>
  )
}

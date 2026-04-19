import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'
import { printNotaFrete, escapeHtml } from '../utils/print.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}
function formatDate(val) {
  if (!val) return '\u2014'
  const s = String(val)
  if (/^\d{2}\/\d{2}\/\d{4}$/.test(s)) return s
  try { const p = (s.includes('T') ? s.substring(0,10) : s).split('-'); return p.length === 3 ? `${p[2]}/${p[1]}/${p[0]}` : s } catch { return s }
}

function printContent(html) {
  const win = window.open('', '_blank', 'width=820,height=600')
  if (!win) { alert('Popup bloqueado.'); return }
  win.document.write(html)
  win.document.close()
}

export default function ListaFretes({ viagemAtiva, onNavigate, onClose }) {
  const [fretes, setFretes] = useState([])
  const [loading, setLoading] = useState(false)
  const [toast, setToast] = useState(null)
  const [selecionado, setSelecionado] = useState(null)

  // Filtros
  const [filtrosVisiveis, setFiltrosVisiveis] = useState(true)
  const [filtroViagem, setFiltroViagem] = useState(viagemAtiva?.id_viagem || '')
  const [filtroNumero, setFiltroNumero] = useState('')
  const [filtroItem, setFiltroItem] = useState('')
  const [filtroRemetente, setFiltroRemetente] = useState('')
  const [filtroDestinatario, setFiltroDestinatario] = useState('')
  const [filtroDataIni, setFiltroDataIni] = useState('')
  const [filtroDataFim, setFiltroDataFim] = useState('')
  const [contatosAberto, setContatosAberto] = useState(true)
  const [periodoAberto, setPeriodoAberto] = useState(true)
  const [viagens, setViagens] = useState([])

  // Listas para dropdowns
  const [remetentes, setRemetentes] = useState([])
  const [destinatarios, setDestinatarios] = useState([])

  // Itens carregados para busca por item
  const [itensMap, setItensMap] = useState({}) // { id_frete: [itens] }

  function showToast(msg, type = 'success') {
    setToast({ msg, type }); setTimeout(() => setToast(null), 3500)
  }

  useEffect(() => { api.get('/viagens').then(setViagens).catch(() => {}) }, [])

  // Sincroniza com viagem ativa global quando o usuario troca no topo
  useEffect(() => {
    if (viagemAtiva?.id_viagem) setFiltroViagem(String(viagemAtiva.id_viagem))
  }, [viagemAtiva])

  const carregar = useCallback(() => {
    if (!filtroViagem) return
    setLoading(true)
    api.get(`/fretes?viagem_id=${filtroViagem}`)
      .then(data => {
        const list = Array.isArray(data) ? data : []
        setFretes(list)
        // Extrair remetentes e destinatarios unicos
        setRemetentes([...new Set(list.map(f => f.remetente || f.remetente_nome_temp).filter(Boolean))].sort())
        setDestinatarios([...new Set(list.map(f => f.destinatario || f.destinatario_nome_temp).filter(Boolean))].sort())
      })
      .catch(() => showToast('Erro ao carregar', 'error'))
      .finally(() => setLoading(false))
  }, [filtroViagem])

  useEffect(() => { carregar() }, [carregar])

  // Carregar itens de todos os fretes para busca por item
  useEffect(() => {
    if (!filtroItem || fretes.length === 0) return
    // Carregar itens apenas quando usuario digitar busca
    const idsParaCarregar = fretes.filter(f => !itensMap[f.id_frete]).map(f => f.id_frete)
    if (idsParaCarregar.length === 0) return
    Promise.all(idsParaCarregar.map(id =>
      api.get(`/fretes/${id}/itens`).then(itens => ({ id, itens })).catch(() => ({ id, itens: [] }))
    )).then(results => {
      setItensMap(prev => {
        const next = { ...prev }
        results.forEach(r => { next[r.id] = r.itens })
        return next
      })
    })
  }, [filtroItem, fretes])

  // Filtros locais
  const filtrados = fretes.filter(f => {
    if (filtroNumero && !String(f.numero_frete).includes(filtroNumero)) return false
    if (filtroRemetente && (f.remetente || f.remetente_nome_temp || '') !== filtroRemetente) return false
    if (filtroDestinatario && (f.destinatario || f.destinatario_nome_temp || '') !== filtroDestinatario) return false
    // Filtro por item
    if (filtroItem) {
      const itens = itensMap[f.id_frete] || []
      const match = itens.some(i => (i.nome_item_ou_id_produto || '').toLowerCase().includes(filtroItem.toLowerCase()))
      if (!match) return false
    }
    // Filtro por periodo
    if (filtroDataIni || filtroDataFim) {
      const dataStr = f.data_emissao || ''
      let dataFrete
      if (/^\d{2}\/\d{2}\/\d{4}$/.test(dataStr)) {
        const [d, m, y] = dataStr.split('/')
        dataFrete = new Date(y, m - 1, d)
      } else {
        dataFrete = new Date(dataStr)
      }
      if (isNaN(dataFrete)) return true
      if (filtroDataIni && dataFrete < new Date(filtroDataIni)) return false
      if (filtroDataFim && dataFrete > new Date(filtroDataFim + 'T23:59:59')) return false
    }
    return true
  })

  const totalLancado = filtrados.reduce((s, f) => s + (parseFloat(f.valor_total_itens || f.valor_frete_calculado) || 0), 0)
  const totalRecebido = filtrados.reduce((s, f) => s + (parseFloat(f.valor_pago) || 0), 0)
  const totalAReceber = Math.max(0, totalLancado - totalRecebido)
  const totalVolumes = filtrados.reduce((s, f) => s + (parseInt(f.total_volumes) || 0), 0)

  // Filtrar apenas fretes a receber (devedor > 0)
  function filtrarAReceber() {
    setFiltroRemetente('')
    setFiltroDestinatario('')
    setFiltroNumero('')
    setFiltroItem('')
    // Marca para mostrar apenas devedores — usaremos um flag
    setFretes(prev => prev.map(f => ({ ...f, _filtroReceber: true })))
  }

  // Imprimir lista
  function imprimirLista() {
    const css = `body{font-family:Arial,sans-serif;font-size:11px;margin:10px}table{width:100%;border-collapse:collapse}th{background:#047857;color:#fff;padding:5px 6px;text-align:left;font-size:10px}td{padding:4px 6px;border-bottom:1px solid #ddd;font-size:10px}tr:nth-child(even){background:#f0fdf4}.money{text-align:right;font-family:'Courier New',monospace}h2{text-align:center;color:#047857;font-size:16px;margin:4px 0}.totais{display:flex;justify-content:center;gap:16px;font-size:11px;margin-top:8px;padding:8px;border-top:2px solid #047857}@media print{body{margin:0}}`
    const rows = filtrados.map(f => {
      const valor = parseFloat(f.valor_total_itens || f.valor_frete_calculado) || 0
      const pago = parseFloat(f.valor_pago) || 0
      const dev = Math.max(0, valor - pago)
      // #DS5-205: escape obrigatorio em dados do banco — XSS via OCR/input livre
      return `<tr><td>${escapeHtml(f.numero_frete)}</td><td>${escapeHtml(f.remetente || f.remetente_nome_temp || '')}</td><td>${escapeHtml(f.destinatario || f.destinatario_nome_temp || '')}</td><td>${escapeHtml(f.rota || '')}</td><td>${escapeHtml(formatDate(f.data_emissao))}</td><td class="money">${formatMoney(valor)}</td><td class="money">${formatMoney(dev)}</td><td class="money">${formatMoney(pago)}</td><td>${escapeHtml(f.conferente || '')}</td></tr>`
    }).join('')
    const html = `<!DOCTYPE html><html><head><title>Lista de Fretes</title><style>${css}</style></head><body>
      <h2>Gerenciamento de Fretes da Viagem</h2>
      <table><thead><tr><th>N°</th><th>Remetente</th><th>Destinatario</th><th>Rota</th><th>Emissao</th><th>Vlr. Frete</th><th>A Receber</th><th>Recebido</th><th>Conferente</th></tr></thead><tbody>${rows}</tbody></table>
      <div class="totais">
        <span>Total Lancado: <strong>${formatMoney(totalLancado)}</strong></span> |
        <span>Total Recebido: <strong>${formatMoney(totalRecebido)}</strong></span> |
        <span>Total a Receber: <strong>${formatMoney(totalAReceber)}</strong></span> |
        <span>Lancamentos: <strong>${filtrados.length}</strong></span> |
        <span>Volumes: <strong>${totalVolumes}</strong></span>
      </div>
      <div style="text-align:center;font-size:9px;margin-top:10px;color:#999">${new Date().toLocaleString('pt-BR')}</div>
      <script>window.onload=()=>window.print()</script></body></html>`
    printContent(html)
  }

  // Nota de frete
  async function imprimirNota() {
    if (!selecionado) { showToast('Selecione um frete na lista', 'error'); return }
    try {
      const itens = await api.get(`/fretes/${selecionado.id_frete}/itens`)
      const freteComItens = { ...selecionado, itens: (itens || []).map(i => ({ descricao: i.nome_item_ou_id_produto, quantidade: i.quantidade, valor_unitario: i.preco_unitario, valor: i.subtotal_item })) }
      printNotaFrete(freteComItens, viagemAtiva)
    } catch {
      printNotaFrete(selecionado, viagemAtiva)
    }
  }

  // Styles
  const I = { padding: '7px 10px', fontSize: '0.82rem', background: 'var(--bg-soft)', border: '1px solid var(--border)', borderRadius: 4, color: 'var(--text)', fontFamily: 'Sora, sans-serif', width: '100%', boxSizing: 'border-box' }
  const L = { fontSize: '0.72rem', fontWeight: 700, color: 'var(--text)', marginBottom: 3, display: 'block', marginTop: 10 }
  const C = { padding: '4px 6px', fontSize: '0.75rem', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: 0 }
  const selI = { ...I, background: 'var(--primary)', color: '#fff', border: '1px solid var(--primary)', fontWeight: 600 }
  const toggleStyle = { cursor: 'pointer', padding: '4px 8px', background: 'var(--bg-soft)', border: '1px solid var(--border)', borderRadius: 4, fontSize: '0.75rem', fontWeight: 700, color: 'var(--primary)', display: 'flex', alignItems: 'center', gap: 4, marginTop: 12 }

  if (!viagemAtiva) {
    return <div className="placeholder-page"><div className="ph-icon">🚚</div><h2>Lista de Fretes</h2><p>Selecione uma viagem.</p></div>
  }

  return (
    <div style={{ display: 'flex', gap: 0 }}>
      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}

      {/* CONTEUDO PRINCIPAL */}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div className="card" style={{ marginBottom: 0, padding: 0 }}>
          <h2 style={{ textAlign: 'center', padding: '12px 0 8px', margin: 0, fontSize: '1rem' }}>Gerenciamento de Fretes da Viagem</h2>

          {/* TABELA ZEBRADA */}
          <div style={{ overflow: 'auto', maxHeight: 'calc(100vh - 240px)' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', tableLayout: 'fixed' }}>
              <colgroup>
                <col style={{ width: '5%' }} />
                <col style={{ width: '13%' }} />
                <col style={{ width: '16%' }} />
                <col style={{ width: '12%' }} />
                <col style={{ width: '8%' }} />
                <col style={{ width: '8%' }} />
                <col style={{ width: '9%' }} />
                <col style={{ width: '9%' }} />
                <col style={{ width: '9%' }} />
                <col style={{ width: '9%' }} />
              </colgroup>
              <thead><tr style={{ background: '#047857', color: '#fff', position: 'sticky', top: 0, zIndex: 2 }}>
                <th style={{ padding: '6px 6px', fontSize: '0.72rem', fontWeight: 700, textAlign: 'left' }}>N° Frete</th>
                <th style={{ padding: '6px 6px', fontSize: '0.72rem', fontWeight: 700, textAlign: 'left' }}>Remetente</th>
                <th style={{ padding: '6px 6px', fontSize: '0.72rem', fontWeight: 700, textAlign: 'left' }}>Destinatario</th>
                <th style={{ padding: '6px 6px', fontSize: '0.72rem', fontWeight: 700, textAlign: 'left' }}>Rota/Viagem</th>
                <th style={{ padding: '6px 6px', fontSize: '0.72rem', fontWeight: 700, textAlign: 'left' }}>Data Viagem</th>
                <th style={{ padding: '6px 6px', fontSize: '0.72rem', fontWeight: 700, textAlign: 'left' }}>Emissao</th>
                <th style={{ padding: '6px 6px', fontSize: '0.72rem', fontWeight: 700, textAlign: 'right' }}>Vlr. Frete</th>
                <th style={{ padding: '6px 6px', fontSize: '0.72rem', fontWeight: 700, textAlign: 'right' }}>A Receber</th>
                <th style={{ padding: '6px 6px', fontSize: '0.72rem', fontWeight: 700, textAlign: 'right' }}>Recebido</th>
                <th style={{ padding: '6px 6px', fontSize: '0.72rem', fontWeight: 700, textAlign: 'left' }}>Conferente</th>
              </tr></thead>
              <tbody>
                {loading ? <tr><td colSpan="10" style={{ padding: 30, textAlign: 'center' }}>Carregando...</td></tr>
                : filtrados.length === 0 ? <tr><td colSpan="10" style={{ textAlign: 'center', padding: 40, color: 'var(--text-muted)' }}>Nenhum frete nesta viagem</td></tr>
                : filtrados.map((f, idx) => {
                  const valor = parseFloat(f.valor_total_itens || f.valor_frete_calculado) || 0
                  const pago = parseFloat(f.valor_pago) || 0
                  const devedor = Math.max(0, valor - pago)
                  const isSel = selecionado?.id_frete === f.id_frete
                  const zebraColor = idx % 2 === 0 ? 'rgba(4,120,87,0.06)' : 'transparent'
                  const bgColor = isSel ? 'rgba(4,120,87,0.25)' : zebraColor
                  return (
                    <tr key={f.id_frete}
                        style={{ background: bgColor, cursor: 'pointer', transition: 'background 0.15s' }}
                        onClick={() => setSelecionado(f)}
                        onDoubleClick={() => {
                          sessionStorage.setItem('frete_editar', JSON.stringify(f))
                          onNavigate && onNavigate('lancar-frete')
                        }}
                        onMouseEnter={e => { if (!isSel) e.currentTarget.style.background = 'rgba(4,120,87,0.15)' }}
                        onMouseLeave={e => { if (!isSel) e.currentTarget.style.background = zebraColor }}
                        title="Clique para selecionar, duplo-clique para editar">
                      <td style={C}><span style={{ fontWeight: 700, color: '#047857' }}>{f.numero_frete}</span></td>
                      <td style={C} title={f.remetente || f.remetente_nome_temp || ''}>{(f.remetente || f.remetente_nome_temp || '\u2014').toUpperCase()}</td>
                      <td style={C} title={f.destinatario || f.destinatario_nome_temp || ''}>{(f.destinatario || f.destinatario_nome_temp || '\u2014').toUpperCase()}</td>
                      <td style={C}>{f.rota || f.rota_temp || '\u2014'}</td>
                      <td style={C}>{formatDate(f.data_emissao)}</td>
                      <td style={C}>{formatDate(f.data_emissao)}</td>
                      <td style={{ ...C, textAlign: 'right', fontFamily: 'Space Mono, monospace', fontWeight: 700, color: '#047857' }}>{formatMoney(valor)}</td>
                      <td style={{ ...C, textAlign: 'right', fontFamily: 'Space Mono, monospace', fontWeight: 700, color: devedor > 0.01 ? '#DC2626' : '#047857' }}>{formatMoney(devedor)}</td>
                      <td style={{ ...C, textAlign: 'right', fontFamily: 'Space Mono, monospace', color: '#059669' }}>{formatMoney(pago)}</td>
                      <td style={C}>{f.conferente || f.conferente_temp || '\u2014'}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>

          {/* TOTAIS */}
          <div style={{ display: 'flex', justifyContent: 'center', gap: 20, padding: '10px 14px', borderTop: '2px solid #047857', fontSize: '0.82rem', flexWrap: 'wrap', background: 'var(--bg-soft)' }}>
            <span>Total Lancado: <strong style={{ color: '#047857' }}>{formatMoney(totalLancado)}</strong></span>
            <span style={{ color: 'var(--border)' }}>|</span>
            <span>Total Recebido: <strong style={{ color: '#047857' }}>{formatMoney(totalRecebido)}</strong></span>
            <span style={{ color: 'var(--border)' }}>|</span>
            <span>Total a Receber: <strong style={{ color: '#DC2626' }}>{formatMoney(totalAReceber)}</strong></span>
            <span style={{ color: 'var(--border)' }}>|</span>
            <span>Lancamentos: <strong style={{ color: '#047857' }}>{filtrados.length}</strong></span>
            <span style={{ color: 'var(--border)' }}>|</span>
            <span>Volumes: <strong style={{ color: '#F59E0B' }}>{totalVolumes}</strong></span>
          </div>

          {/* BOTOES */}
          <div style={{ display: 'flex', gap: 10, justifyContent: 'center', padding: '10px 14px', flexWrap: 'wrap' }}>
            <button style={{ padding: '8px 20px', background: '#047857', color: '#fff', border: 'none', borderRadius: 4, fontWeight: 700, cursor: 'pointer', fontSize: '0.82rem' }}
              onClick={() => {
                setFiltroRemetente(''); setFiltroDestinatario(''); setFiltroNumero(''); setFiltroItem('')
                setFretes(prev => prev.filter(f => Math.max(0, (parseFloat(f.valor_total_itens) || 0) - (parseFloat(f.valor_pago) || 0)) > 0.01))
              }}>Fretes a Receber</button>
            <button style={{ padding: '8px 20px', background: '#047857', color: '#fff', border: 'none', borderRadius: 4, fontWeight: 700, cursor: 'pointer', fontSize: '0.82rem' }}
              onClick={imprimirNota}>Nota de Frete</button>
            <button style={{ padding: '8px 20px', background: '#047857', color: '#fff', border: 'none', borderRadius: 4, fontWeight: 700, cursor: 'pointer', fontSize: '0.82rem' }}
              onClick={imprimirLista}>Imprimir Lista</button>
            <button style={{ padding: '8px 20px', background: '#047857', color: '#fff', border: 'none', borderRadius: 4, fontWeight: 700, cursor: 'pointer', fontSize: '0.82rem' }}
              onClick={carregar}>Atualizar Lista</button>
            <button style={{ padding: '8px 20px', background: '#fff', color: '#DC2626', border: '1px solid #DC2626', borderRadius: 4, fontWeight: 700, cursor: 'pointer', fontSize: '0.82rem' }}
              onClick={() => onClose ? onClose() : onNavigate && onNavigate('lancar-frete')}>Fechar</button>
          </div>
        </div>
      </div>

      {/* TOGGLE FILTROS */}
      <div onClick={() => setFiltrosVisiveis(prev => !prev)}
        style={{ writingMode: 'vertical-rl', cursor: 'pointer', padding: '12px 5px', background: 'var(--bg-card)', border: '1px solid var(--border)', borderRadius: '0 6px 6px 0', fontSize: '0.72rem', fontWeight: 700, color: '#047857', userSelect: 'none', display: 'flex', alignItems: 'center', gap: 4 }}>
        {filtrosVisiveis ? '\u25B6' : '\u25C0'} FILTROS
      </div>

      {/* FILTROS LATERAIS */}
      {filtrosVisiveis && (
      <div style={{ width: 260, flexShrink: 0 }}>
        <div className="card" style={{ position: 'sticky', top: 60, padding: 14, overflowY: 'auto', maxHeight: 'calc(100vh - 80px)' }}>

          {/* Filtro de Viagem */}
          <label style={{ ...L, marginTop: 0 }}>Viagem:</label>
          <select style={selI} value={filtroViagem} onChange={e => setFiltroViagem(e.target.value)}>
            <option value="">Selecione...</option>
            {viagens.map(v => (
              <option key={v.id_viagem} value={v.id_viagem}>
                {v.id_viagem} - {v.data_viagem}{v.ativa ? ' (ATIVA)' : ''}
                {v.nome_rota ? ` (${v.nome_rota})` : ''}
              </option>
            ))}
          </select>

          {/* Filtrar por Periodo */}
          <div style={toggleStyle} onClick={() => setPeriodoAberto(p => !p)}>
            <span>{periodoAberto ? '\u25BC' : '\u25B6'}</span> Filtrar por Periodo
          </div>
          {periodoAberto && (
            <div style={{ marginTop: 6 }}>
              <label style={{ ...L, marginTop: 4 }}>Data Inicio:</label>
              <input type="date" style={I} value={filtroDataIni} onChange={e => setFiltroDataIni(e.target.value)} />
              <label style={L}>Data Fim:</label>
              <input type="date" style={I} value={filtroDataFim} onChange={e => setFiltroDataFim(e.target.value)} />
            </div>
          )}

          {/* Buscar por Item */}
          <label style={L}>Buscar por Item:</label>
          <input style={I} placeholder="Digite a descricao..." value={filtroItem} onChange={e => setFiltroItem(e.target.value)} />

          {/* Filtrar N° Frete */}
          <label style={L}>Filtrar N° Frete:</label>
          <input style={I} placeholder="Digite o N° Frete" value={filtroNumero} onChange={e => setFiltroNumero(e.target.value)} />

          {/* Filtrar por Contatos */}
          <div style={toggleStyle} onClick={() => setContatosAberto(p => !p)}>
            <span>{contatosAberto ? '\u25BC' : '\u25B6'}</span> Filtrar por Contatos
          </div>
          {contatosAberto && (
            <div style={{ marginTop: 6 }}>
              <label style={{ ...L, marginTop: 4 }}>Remetente:</label>
              <select style={selI} value={filtroRemetente} onChange={e => setFiltroRemetente(e.target.value)}>
                <option value="">Todos</option>
                {remetentes.map(r => <option key={r} value={r}>{r}</option>)}
              </select>

              <label style={L}>Destinatario:</label>
              <select style={selI} value={filtroDestinatario} onChange={e => setFiltroDestinatario(e.target.value)}>
                <option value="">Todos</option>
                {destinatarios.map(d => <option key={d} value={d}>{d}</option>)}
              </select>
            </div>
          )}

          {/* Limpar filtros */}
          <button style={{ ...I, marginTop: 14, background: 'transparent', border: '1px solid var(--border)', cursor: 'pointer', textAlign: 'center', fontWeight: 600, color: 'var(--text-muted)' }}
            onClick={() => { setFiltroNumero(''); setFiltroItem(''); setFiltroRemetente(''); setFiltroDestinatario(''); setFiltroDataIni(''); setFiltroDataFim(''); carregar() }}>
            Limpar Filtros
          </button>

        </div>
      </div>
      )}
    </div>
  )
}

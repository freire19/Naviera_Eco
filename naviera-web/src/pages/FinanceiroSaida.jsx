import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'
import { printContent } from '../utils/print.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}
function fmtDate(d) {
  if (!d) return '\u2014'
  if (typeof d === 'string' && /^\d{2}\/\d{2}\/\d{4}$/.test(d)) return d
  const s = String(d).includes('T') ? d.substring(0, 10) : d
  try { const p = s.split('-'); return p.length === 3 ? `${p[2]}/${p[1]}/${p[0]}` : s } catch { return s }
}
function todayISO() { return new Date().toISOString().split('T')[0] }

const FORMAS = ['DINHEIRO', 'PIX', 'CARTAO', 'TRANSFERENCIA', 'BOLETO']
const FORMA_COLORS = { DINHEIRO: '#059669', PIX: '#059669', CARTAO: '#B45309', TRANSFERENCIA: '#7BA393', BOLETO: '#DC2626' }

export default function FinanceiroSaida({ viagemAtiva, onNavigate, onClose }) {
  const [viagens, setViagens] = useState([])
  const [categorias, setCategorias] = useState([])
  const [saidas, setSaidas] = useState([])
  const [loading, setLoading] = useState(false)
  const [toast, setToast] = useState(null)
  const [selecionado, setSelecionado] = useState(null)

  // Filtros
  const [filtroViagem, setFiltroViagem] = useState(viagemAtiva?.id_viagem || '')
  const [filtroCategoria, setFiltroCategoria] = useState('Todas')
  const [filtroForma, setFiltroForma] = useState('Todas')
  const [filtroData, setFiltroData] = useState('')

  // Form lancamento rapido
  const [dataGasto, setDataGasto] = useState(todayISO())
  const [dataPagamento, setDataPagamento] = useState(todayISO())
  const [descricao, setDescricao] = useState('')
  const [categoriaSel, setCategoriaSel] = useState('')
  const [valor, setValor] = useState('')
  const [formaPagto, setFormaPagto] = useState('DINHEIRO')
  const [salvando, setSalvando] = useState(false)

  // Modal nova categoria
  const [modalCategoria, setModalCategoria] = useState(false)
  const [novaCategoria, setNovaCategoria] = useState('')

  // Modal excluir
  const [modalExcluir, setModalExcluir] = useState(null)
  const [motivoExclusao, setMotivoExclusao] = useState('')

  function showToast(msg, type = 'success') { setToast({ msg, type }); setTimeout(() => setToast(null), 3500) }

  useEffect(() => {
    api.get('/viagens').then(setViagens).catch(() => {})
    api.get('/financeiro/categorias').then(data => setCategorias(Array.isArray(data) ? data : [])).catch(() => {})
  }, [])

  useEffect(() => { if (viagemAtiva && !filtroViagem) setFiltroViagem(String(viagemAtiva.id_viagem)) }, [viagemAtiva])

  const carregar = useCallback(() => {
    setLoading(true)
    const params = new URLSearchParams()
    if (filtroViagem) params.append('viagem_id', filtroViagem)
    if (filtroCategoria !== 'Todas') params.append('categoria', filtroCategoria)
    if (filtroForma !== 'Todas') params.append('forma_pagto', filtroForma)
    if (filtroData) params.append('data_especifica', filtroData)
    api.get(`/financeiro/saidas?${params}`)
      .then(data => setSaidas(Array.isArray(data) ? data : []))
      .catch(() => showToast('Erro ao carregar', 'error'))
      .finally(() => setLoading(false))
  }, [filtroViagem, filtroCategoria, filtroForma, filtroData])

  useEffect(() => { carregar() }, [carregar])

  const totalGasto = saidas.reduce((s, r) => s + (parseFloat(r.valor_total) || 0), 0)

  // Salvar despesa
  async function salvar() {
    if (!descricao.trim()) { showToast('Informe a descricao', 'error'); return }
    if (!valor || parseFloat(valor) <= 0) { showToast('Informe o valor', 'error'); return }
    if (!filtroViagem) { showToast('Selecione uma viagem', 'error'); return }
    setSalvando(true)
    try {
      let idCategoria = null
      if (categoriaSel) {
        const cat = categorias.find(c => c.nome === categoriaSel)
        if (cat) idCategoria = cat.id
        else {
          const nova = await api.post('/financeiro/categorias', { nome: categoriaSel })
          idCategoria = nova.id
          setCategorias(prev => [...prev, nova].sort((a, b) => (a.nome || '').localeCompare(b.nome || '')))
        }
      }
      const status = formaPagto === 'BOLETO' ? 'PENDENTE' : 'PAGO'
      await api.post('/financeiro/saida', {
        id_viagem: filtroViagem,
        descricao: descricao.trim().toUpperCase(),
        valor_total: parseFloat(valor),
        valor_pago: status === 'PAGO' ? parseFloat(valor) : 0,
        data_vencimento: dataGasto,
        data_pagamento: status === 'PAGO' ? dataPagamento : null,
        id_categoria: idCategoria,
        forma_pagamento: formaPagto,
        status
      })
      showToast('Despesa salva!')
      setDescricao(''); setValor(''); setCategoriaSel('')
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao salvar', 'error')
    } finally { setSalvando(false) }
  }

  // Excluir (soft delete)
  async function excluir() {
    if (!modalExcluir) return
    if (!motivoExclusao.trim()) { showToast('Informe o motivo', 'error'); return }
    try {
      await api.delete(`/financeiro/saida/${modalExcluir.id}`, { motivo: motivoExclusao.trim() })
      showToast('Despesa excluida')
      setModalExcluir(null); setMotivoExclusao('')
      setSelecionado(null)
      carregar()
    } catch (err) { showToast(err.message || 'Erro ao excluir', 'error') }
  }

  // Nova categoria
  async function criarCategoria() {
    if (!novaCategoria.trim()) return
    try {
      const result = await api.post('/financeiro/categorias', { nome: novaCategoria.trim() })
      setCategorias(prev => [...prev, result].sort((a, b) => (a.nome || '').localeCompare(b.nome || '')))
      setCategoriaSel(result.nome)
      setModalCategoria(false); setNovaCategoria('')
      showToast('Categoria criada')
    } catch (err) { showToast(err.message || 'Erro', 'error') }
  }

  // Imprimir
  function imprimir() {
    const css = `body{font-family:Arial;font-size:11px;margin:10px}table{width:100%;border-collapse:collapse}th{background:#047857;color:#fff;padding:5px 8px;font-size:10px;text-align:left}td{padding:4px 8px;border-bottom:1px solid #ddd;font-size:10px}tr:nth-child(even){background:#f0fdf4}.money{text-align:right;font-family:'Courier New',monospace}h2{text-align:center;color:#047857}@media print{body{margin:0}}`
    const rows = saidas.map(s => `<tr><td>${fmtDate(s.data_vencimento)}</td><td>${s.descricao || ''}</td><td>${s.categoria_nome || ''}</td><td>${s.forma_pagamento || ''}</td><td class="money">${formatMoney(s.valor_total)}</td><td>${s.status || ''}</td></tr>`).join('')
    const html = `<!DOCTYPE html><html><head><title>Relatorio Saidas</title><style>${css}</style></head><body>
      <h2>CONTROLE DE DESPESAS E SAIDAS</h2>
      <table><thead><tr><th>Data</th><th>Descricao</th><th>Categoria</th><th>Forma Pgto</th><th>Valor</th><th>Status</th></tr></thead><tbody>${rows}</tbody></table>
      <div style="text-align:right;margin-top:8px;font-weight:700;font-size:13px">TOTAL: ${formatMoney(totalGasto)}</div>
      <script>window.onload=()=>window.print()</script></body></html>`
    printContent(html)
  }

  // Limpar filtros
  function limparFiltros() { setFiltroCategoria('Todas'); setFiltroForma('Todas'); setFiltroData('') }

  const I = { padding: '7px 10px', fontSize: '0.82rem', background: 'var(--bg-soft)', border: '1px solid var(--border)', borderRadius: 4, color: 'var(--text)', fontFamily: 'Sora, sans-serif', width: '100%', boxSizing: 'border-box' }
  const L = { fontSize: '0.72rem', fontWeight: 700, color: 'var(--text)', display: 'block', marginBottom: 3, marginTop: 10 }
  const selGreen = { ...I, background: '#047857', color: '#fff', border: '1px solid #047857', fontWeight: 600 }

  return (
    <div>
      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}

      {/* HEADER */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 12 }}>
        <h2 style={{ margin: 0, fontSize: '1.1rem' }}>CONTROLE DE DESPESAS E SAIDAS</h2>
        <div style={{ textAlign: 'right', padding: '8px 16px', border: '1px solid var(--border)', borderRadius: 6, background: 'var(--bg-card)' }}>
          <div style={{ fontSize: '0.72rem', fontWeight: 700, color: '#757575' }}>TOTAL GASTO (FILTRADO)</div>
          <div style={{ fontSize: '1.6rem', fontWeight: 700, color: '#DC2626', fontFamily: 'Space Mono, monospace' }}>{formatMoney(totalGasto)}</div>
        </div>
      </div>

      {/* FILTROS */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '8px 14px', background: '#047857', borderRadius: 6, marginBottom: 12, flexWrap: 'wrap' }}>
        <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#fff' }}>FILTRAR:</span>
        <div>
          <div style={{ fontSize: '0.68rem', fontWeight: 700, color: '#fff' }}>Viagem / Data</div>
          <select style={{ ...selGreen, minWidth: 200 }} value={filtroViagem} onChange={e => setFiltroViagem(e.target.value)}>
            <option value="">TODAS AS VIAGENS</option>
            {viagens.map(v => <option key={v.id_viagem} value={v.id_viagem}>{v.id_viagem} ({fmtDate(v.data_viagem)}){v.ativa ? ' (ATUAL)' : ''}</option>)}
          </select>
        </div>
        <div>
          <div style={{ fontSize: '0.68rem', fontWeight: 700, color: '#fff' }}>Categoria</div>
          <select style={{ ...selGreen, minWidth: 120 }} value={filtroCategoria} onChange={e => setFiltroCategoria(e.target.value)}>
            <option>Todas</option>
            {categorias.map(c => <option key={c.id} value={c.nome}>{c.nome}</option>)}
          </select>
        </div>
        <div>
          <div style={{ fontSize: '0.68rem', fontWeight: 700, color: '#fff' }}>Forma Pagto</div>
          <select style={{ ...selGreen, minWidth: 100 }} value={filtroForma} onChange={e => setFiltroForma(e.target.value)}>
            <option>Todas</option>
            {FORMAS.map(f => <option key={f}>{f}</option>)}
          </select>
        </div>
        <div>
          <div style={{ fontSize: '0.68rem', fontWeight: 700, color: '#fff' }}>Data Especifica</div>
          <input type="date" style={{ ...selGreen, minWidth: 120 }} value={filtroData} onChange={e => setFiltroData(e.target.value)} />
        </div>
        <button style={{ padding: '6px 16px', background: '#fff', color: '#047857', border: 'none', borderRadius: 4, fontWeight: 700, cursor: 'pointer' }} onClick={carregar}>BUSCAR</button>
        <button style={{ padding: '6px 16px', background: 'rgba(255,255,255,0.2)', color: '#fff', border: '1px solid rgba(255,255,255,0.3)', borderRadius: 4, fontWeight: 700, cursor: 'pointer' }} onClick={limparFiltros}>LIMPAR</button>
      </div>

      <div style={{ display: 'flex', gap: 12 }}>
        {/* SIDEBAR ESQUERDA — Lancamento Rapido */}
        <div style={{ width: 280, flexShrink: 0 }}>
          <div className="card" style={{ padding: 14, overflowY: 'auto', maxHeight: 'calc(100vh - 220px)' }}>
            <h4 style={{ margin: '0 0 2px', fontSize: '0.9rem' }}>Lancamento Rapido</h4>
            <p style={{ margin: '0 0 8px', fontSize: '0.72rem', color: 'var(--text-muted)' }}>Despesa do dia-a-dia</p>
            <hr style={{ border: 'none', borderTop: '1px solid var(--border)', margin: '6px 0' }} />

            <label style={L}>Data do Gasto</label>
            <input type="date" style={I} value={dataGasto} onChange={e => setDataGasto(e.target.value)} />

            <label style={L}>Data do Pagamento</label>
            <input type="date" style={I} value={dataPagamento} onChange={e => setDataPagamento(e.target.value)} />

            <label style={L}>Descricao / Fornecedor</label>
            <input style={I} placeholder="Ex: Padaria Central" value={descricao} onChange={e => setDescricao(e.target.value)} />

            <label style={L}>Categoria</label>
            <div style={{ display: 'flex', gap: 6 }}>
              <select style={{ ...I, flex: 1 }} value={categoriaSel} onChange={e => setCategoriaSel(e.target.value)}>
                <option value="">Selecione...</option>
                {categorias.map(c => <option key={c.id} value={c.nome}>{c.nome}</option>)}
              </select>
              <button style={{ padding: '6px 12px', background: '#047857', color: '#fff', border: 'none', borderRadius: 4, fontWeight: 700, cursor: 'pointer', fontSize: '1rem' }}
                onClick={() => setModalCategoria(true)}>+</button>
            </div>

            <label style={L}>Valor (R$)</label>
            <input style={{ ...I, background: '#FEF3C7', fontWeight: 700 }} type="number" step="0.01" placeholder="0,00" value={valor} onChange={e => setValor(e.target.value)} />

            <label style={L}>Forma de Pagamento</label>
            <select style={selGreen} value={formaPagto} onChange={e => setFormaPagto(e.target.value)}>
              {FORMAS.map(f => <option key={f}>{f}</option>)}
            </select>

            <button style={{ width: '100%', padding: '10px', marginTop: 14, background: '#059669', color: '#fff', border: 'none', borderRadius: 4, fontWeight: 700, cursor: 'pointer', fontSize: '0.9rem' }}
              onClick={salvar} disabled={salvando}>{salvando ? 'Salvando...' : 'SALVAR DESPESA'}</button>

            <hr style={{ border: 'none', borderTop: '1px solid var(--border)', margin: '14px 0 8px' }} />
            <p style={{ fontSize: '0.72rem', color: '#757575', margin: '0 0 4px' }}>Lancamento Complexo:</p>
            <button style={{ width: '100%', padding: '8px', background: '#F59E0B', color: '#fff', border: 'none', borderRadius: 4, fontWeight: 700, cursor: 'pointer', fontSize: '0.82rem' }}
              onClick={() => onNavigate && onNavigate('boletos')}>BOLETOS / A PRAZO</button>
          </div>
        </div>

        {/* TABELA PRINCIPAL */}
        <div style={{ flex: 1, minWidth: 0 }}>
          <div className="card" style={{ padding: 0 }}>
            <div style={{ overflow: 'auto', maxHeight: 'calc(100vh - 280px)' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', tableLayout: 'fixed' }}>
                <colgroup>
                  <col style={{ width: '10%' }} />
                  <col style={{ width: '30%' }} />
                  <col style={{ width: '15%' }} />
                  <col style={{ width: '13%' }} />
                  <col style={{ width: '15%' }} />
                  <col style={{ width: '10%' }} />
                </colgroup>
                <thead><tr style={{ background: '#047857', color: '#fff', position: 'sticky', top: 0, zIndex: 2 }}>
                  <th style={{ padding: '6px 10px', fontSize: '0.72rem', textAlign: 'left' }}>Data</th>
                  <th style={{ padding: '6px 10px', fontSize: '0.72rem', textAlign: 'left' }}>Descricao</th>
                  <th style={{ padding: '6px 10px', fontSize: '0.72rem', textAlign: 'left' }}>Categoria</th>
                  <th style={{ padding: '6px 10px', fontSize: '0.72rem', textAlign: 'left' }}>Forma Pgto</th>
                  <th style={{ padding: '6px 10px', fontSize: '0.72rem', textAlign: 'right' }}>Valor</th>
                  <th style={{ padding: '6px 10px', fontSize: '0.72rem', textAlign: 'center' }}>Status</th>
                </tr></thead>
                <tbody>
                  {loading ? <tr><td colSpan={6} style={{ padding: 30, textAlign: 'center' }}>Carregando...</td></tr>
                  : saidas.length === 0 ? <tr><td colSpan={6} style={{ padding: 40, textAlign: 'center', color: 'var(--text-muted)' }}>Nao ha conteudo na tabela</td></tr>
                  : saidas.map((s, idx) => {
                    const isSel = selecionado?.id === s.id
                    const zebraColor = idx % 2 === 0 ? 'rgba(4,120,87,0.06)' : 'transparent'
                    const bgColor = isSel ? 'rgba(4,120,87,0.25)' : zebraColor
                    const formaColor = FORMA_COLORS[s.forma_pagamento] || 'inherit'
                    const C = { padding: '5px 10px', fontSize: '0.8rem', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: 0 }
                    return (
                      <tr key={s.id} style={{ background: bgColor, cursor: 'pointer' }}
                          onClick={() => setSelecionado(s)}
                          onMouseEnter={e => { if (!isSel) e.currentTarget.style.background = 'rgba(4,120,87,0.15)' }}
                          onMouseLeave={e => { if (!isSel) e.currentTarget.style.background = zebraColor }}>
                        <td style={C}>{fmtDate(s.data_vencimento)}</td>
                        <td style={C} title={s.descricao || ''}>{s.descricao || '\u2014'}</td>
                        <td style={C}>{s.categoria_nome || '\u2014'}</td>
                        <td style={{ ...C, color: formaColor, fontWeight: 600 }}>{s.forma_pagamento || '\u2014'}</td>
                        <td style={{ ...C, textAlign: 'right', fontFamily: 'Space Mono, monospace', fontWeight: 700 }}>{formatMoney(s.valor_total)}</td>
                        <td style={{ ...C, textAlign: 'center' }}>
                          <span className={`badge ${s.status === 'PAGO' ? 'success' : s.status === 'VENCIDO' ? 'danger' : 'warning'}`} style={{ fontSize: '0.68rem' }}>
                            {s.status || 'PENDENTE'}
                          </span>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>

            {/* BOTOES RODAPE */}
            <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', padding: '10px 14px', borderTop: '1px solid var(--border)', flexWrap: 'wrap' }}>
              <button style={{ padding: '8px 20px', background: '#047857', color: '#fff', border: 'none', borderRadius: 4, fontWeight: 700, cursor: 'pointer', fontSize: '0.82rem' }} onClick={imprimir}>IMPRIMIR RELATORIO</button>
              <button style={{ padding: '8px 20px', background: '#DC2626', color: '#fff', border: 'none', borderRadius: 4, fontWeight: 700, cursor: 'pointer', fontSize: '0.82rem', opacity: selecionado ? 1 : 0.5 }}
                onClick={() => { if (selecionado) { setModalExcluir(selecionado); setMotivoExclusao('') } }} disabled={!selecionado}>EXCLUIR SELECIONADO</button>
              <button style={{ padding: '8px 20px', background: '#7BA393', color: '#fff', border: 'none', borderRadius: 4, fontWeight: 700, cursor: 'pointer', fontSize: '0.82rem' }}
                onClick={() => onClose ? onClose() : onNavigate && onNavigate('dashboard')}>SAIR</button>
            </div>
          </div>
        </div>
      </div>

      {/* MODAL NOVA CATEGORIA */}
      {modalCategoria && (
        <div className="modal-overlay" onClick={() => setModalCategoria(false)}>
          <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 350 }}>
            <h3>Nova Categoria</h3>
            <input style={I} placeholder="Nome da categoria" value={novaCategoria} onChange={e => setNovaCategoria(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && criarCategoria()} autoFocus />
            <div style={{ display: 'flex', gap: 8, marginTop: 12, justifyContent: 'flex-end' }}>
              <button className="btn-sm" onClick={() => setModalCategoria(false)}>Cancelar</button>
              <button className="btn-sm primary" onClick={criarCategoria}>Salvar</button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL EXCLUIR */}
      {modalExcluir && (
        <div className="modal-overlay" onClick={() => setModalExcluir(null)}>
          <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 420 }}>
            <h3>Excluir Despesa</h3>
            <p style={{ marginBottom: 8 }}>Excluir: <strong>{modalExcluir.descricao}</strong> — {formatMoney(modalExcluir.valor_total)}</p>
            <label style={L}>Motivo da exclusao: *</label>
            <textarea style={{ ...I, minHeight: 60 }} placeholder="Informe o motivo..." value={motivoExclusao} onChange={e => setMotivoExclusao(e.target.value)} />
            <div style={{ display: 'flex', gap: 8, marginTop: 12, justifyContent: 'flex-end' }}>
              <button className="btn-sm" onClick={() => setModalExcluir(null)}>Cancelar</button>
              <button style={{ padding: '6px 16px', background: '#DC2626', color: '#fff', border: 'none', borderRadius: 4, fontWeight: 700, cursor: 'pointer' }} onClick={excluir}>Confirmar Exclusao</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

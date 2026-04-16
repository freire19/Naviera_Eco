import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'
import { useAuth } from '../App.jsx'

// Similaridade entre duas strings (0 a 1) — baseada em bigramas
function similarity(a, b) {
  if (!a || !b) return 0
  a = a.toLowerCase().trim()
  b = b.toLowerCase().trim()
  if (a === b) return 1
  if (a.length < 2 || b.length < 2) return 0
  const bigrams = (s) => { const set = new Map(); for (let i = 0; i < s.length - 1; i++) { const bg = s.substring(i, i + 2); set.set(bg, (set.get(bg) || 0) + 1) } return set }
  const bg1 = bigrams(a), bg2 = bigrams(b)
  let matches = 0
  for (const [bg, count] of bg1) matches += Math.min(count, bg2.get(bg) || 0)
  return (2 * matches) / (a.length - 1 + b.length - 1)
}

// Busca o melhor match no catalogo (retorna null se nao achar similar)
function findCatalogoMatch(nomeItem, catalogo) {
  if (!nomeItem || !catalogo.length) return null
  let best = null, bestScore = 0
  const nome = nomeItem.toLowerCase().trim()
  for (const cat of catalogo) {
    const catNome = (cat.nome_item || '').toLowerCase().trim()
    if (catNome === nome) return null // exato = ja cadastrado, sem sugestao
    const score = similarity(nome, catNome)
    if (score > bestScore) { bestScore = score; best = cat }
  }
  return bestScore >= 0.45 ? { ...best, score: bestScore } : null
}

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}

function fmtDateTime(d) {
  return d ? new Date(d).toLocaleString('pt-BR') : '\u2014'
}

function timeAgo(d) {
  if (!d) return ''
  const diff = Date.now() - new Date(d).getTime()
  const min = Math.floor(diff / 60000)
  if (min < 1) return 'agora'
  if (min < 60) return `${min}min`
  const h = Math.floor(min / 60)
  if (h < 24) return `${h}h`
  return `${Math.floor(h / 24)}d`
}

const STATUS_LABELS = {
  pendente: 'Pendente',
  revisado_operador: 'Revisado',
  aprovado: 'Aprovado',
  rejeitado: 'Rejeitado'
}

const STATUS_CLASSES = {
  pendente: 'warning',
  revisado_operador: 'info',
  aprovado: 'success',
  rejeitado: 'danger'
}

export default function ReviewOCR({ viagemAtiva, onNavigate }) {
  const { usuario } = useAuth()
  const isAdmin = ['administrador', 'admin', 'gerente'].includes((usuario?.funcao || '').toLowerCase())
  const [lancamentos, setLancamentos] = useState([])
  const [loading, setLoading] = useState(false)
  const [filtro, setFiltro] = useState('revisado_operador')
  const [toast, setToast] = useState(null)
  const [expandido, setExpandido] = useState(null)
  const [editados, setEditados] = useState({}) // { lancamento_id: { dados editados } }
  const [motivoRejeicao, setMotivoRejeicao] = useState('')
  const [rotas, setRotas] = useState([])
  const [conferentes, setConferentes] = useState([])
  const [actionLoading, setActionLoading] = useState(null)
  const [docFotoUrl, setDocFotoUrl] = useState(null)
  const [itensCatalogo, setItensCatalogo] = useState([])

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  const carregar = useCallback(() => {
    setLoading(true)
    const params = filtro ? `?status=${filtro}` : ''
    api.get(`/ocr/lancamentos${params}`)
      .then(res => setLancamentos(Array.isArray(res) ? res : (res.data || [])))
      .catch(() => showToast('Erro ao carregar lancamentos OCR', 'error'))
      .finally(() => setLoading(false))
  }, [filtro])

  useEffect(() => { carregar() }, [carregar])

  // Carregar rotas, conferentes e catalogo de itens
  useEffect(() => {
    api.get('/rotas').then(setRotas).catch(() => {})
    api.get('/cadastros/conferentes').then(setConferentes).catch(() => {})
    api.get('/cadastros/itens-frete').then(data => setItensCatalogo(Array.isArray(data) ? data : [])).catch(() => {})
  }, [])

  // ESC para fechar tela de detalhes
  useEffect(() => {
    if (!expandido) return
    const handler = (e) => {
      if (e.key === 'Escape') {
        setExpandido(null)
        setEditados(prev => { const n = { ...prev }; delete n[expandido]; return n })
      }
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [expandido])

  // Iniciar edicao de um lancamento
  function iniciarEdicao(lanc) {
    const dados = typeof lanc.dados_extraidos === 'string' ? JSON.parse(lanc.dados_extraidos) : lanc.dados_extraidos
    setEditados(prev => ({ ...prev, [lanc.id]: JSON.parse(JSON.stringify(dados)) }))
  }

  // Atualizar campo editado
  function editarCampo(lancId, campo, valor) {
    setEditados(prev => ({ ...prev, [lancId]: { ...prev[lancId], [campo]: valor } }))
  }

  // Atualizar item editado
  function editarItem(lancId, idx, campo, valor) {
    setEditados(prev => {
      const dados = { ...prev[lancId] }
      dados.itens = [...(dados.itens || [])]
      dados.itens[idx] = { ...dados.itens[idx], [campo]: campo === 'quantidade' ? parseInt(valor) || 0 : campo === 'preco_unitario' ? parseFloat(valor) || 0 : valor }
      if (campo === 'quantidade' || campo === 'preco_unitario') {
        dados.itens[idx].subtotal = (dados.itens[idx].quantidade || 0) * (dados.itens[idx].preco_unitario || 0)
      }
      return { ...prev, [lancId]: dados }
    })
  }

  // Adicionar item
  function adicionarItem(lancId) {
    setEditados(prev => {
      const dados = { ...prev[lancId] }
      dados.itens = [...(dados.itens || []), { nome_item: '', quantidade: 1, preco_unitario: 0, subtotal: 0 }]
      return { ...prev, [lancId]: dados }
    })
  }

  // Remover item
  function removerItem(lancId, idx) {
    setEditados(prev => {
      const dados = { ...prev[lancId] }
      dados.itens = (dados.itens || []).filter((_, i) => i !== idx)
      return { ...prev, [lancId]: dados }
    })
  }

  // Salvar edicoes no backend antes de aprovar
  async function salvarEdicoes(lancId) {
    const dadosEditados = editados[lancId]
    if (!dadosEditados) return
    try {
      await api.put(`/ocr/lancamentos/${lancId}`, { dados_extraidos: dadosEditados })
      showToast('Edicoes salvas')
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao salvar edicoes', 'error')
    }
  }

  const aprovar = async (id, autoCadastrar = false) => {
    // Se tem edicoes pendentes, salvar antes
    if (editados[id]) {
      try {
        await api.put(`/ocr/lancamentos/${id}`, { dados_extraidos: editados[id] })
      } catch {}
    }
    if (!autoCadastrar && !confirm('Aprovar este lancamento? Sera criado automaticamente.')) return
    setActionLoading(id)
    try {
      const result = await api.put(`/ocr/lancamentos/${id}/aprovar`, { auto_cadastrar: autoCadastrar })
      if (result.encomenda) {
        showToast(`Encomenda #${result.encomenda.numero_encomenda || result.encomenda.id_encomenda} criada!`)
      } else {
        showToast(`Frete #${result.frete?.numero_frete || result.frete?.id_frete} criado!`)
      }
      carregar()
      setExpandido(null)
    } catch (err) {
      if (err.status === 409 && err.clientes_faltantes) {
        const nomes = err.clientes_faltantes.map(c =>
          `${c.campo === 'remetente' ? 'Remetente' : 'Destinatario'}: "${c.nome}"`
        ).join('\n')
        if (confirm(`Clientes nao cadastrados:\n${nomes}\n\nDeseja cadastrar automaticamente e aprovar?`)) {
          aprovar(id, true)
          return
        }
      } else {
        showToast(err.message || 'Erro ao aprovar', 'error')
      }
    } finally {
      if (!autoCadastrar) setActionLoading(null)
    }
  }

  const rejeitar = async (id) => {
    if (!motivoRejeicao.trim()) {
      showToast('Informe o motivo da rejeicao', 'error')
      return
    }
    setActionLoading(id)
    try {
      await api.put(`/ocr/lancamentos/${id}/rejeitar`, { motivo: motivoRejeicao.trim() })
      showToast('Lancamento rejeitado')
      setMotivoRejeicao('')
      carregar()
      setExpandido(null)
    } catch (err) {
      showToast(err.message || 'Erro ao rejeitar', 'error')
    } finally {
      setActionLoading(null)
    }
  }

  const reanalisar = async (id) => {
    setActionLoading(id)
    try {
      const result = await api.post(`/ocr/lancamentos/${id}/ia-review`, {})
      showToast(`Reanalisado: ${result.dados_extraidos?.itens?.length || 0} itens encontrados`)
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao reanalisar', 'error')
    } finally {
      setActionLoading(null)
    }
  }

  const excluir = async (id) => {
    if (!confirm('Excluir este lancamento? Esta acao nao pode ser desfeita.')) return
    setActionLoading(id)
    try {
      await api.delete(`/ocr/lancamentos/${id}`)
      showToast('Lancamento excluido')
      carregar()
      setExpandido(null)
    } catch (err) {
      showToast(err.message || 'Erro ao excluir', 'error')
    } finally {
      setActionLoading(null)
    }
  }

  const verDocFoto = async (id) => {
    try {
      const token = localStorage.getItem('naviera_token')
      const res = await fetch(`/api/ocr/lancamentos/${id}/doc-foto`, {
        headers: { Authorization: `Bearer ${token}` }
      })
      if (!res.ok) throw new Error('Foto nao disponivel')
      const blob = await res.blob()
      const url = URL.createObjectURL(blob)
      setDocFotoUrl(url)
    } catch (err) {
      showToast(err.message || 'Erro ao carregar foto do documento', 'error')
    }
  }

  const fecharDocFoto = () => {
    if (docFotoUrl) URL.revokeObjectURL(docFotoUrl)
    setDocFotoUrl(null)
  }

  const filtros = ['', 'pendente', 'revisado_operador', 'aprovado', 'rejeitado']

  return (
    <div>
      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}

      <div className="card">
        <div className="card-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            {onNavigate && (
              <button className="btn btn-sm btn-outline" onClick={() => onNavigate('dashboard')}>
                ← Voltar
              </button>
            )}
            <h3 style={{ margin: 0 }}>Conferir Lancamentos OCR</h3>
          </div>
          <button className="btn btn-sm" onClick={carregar} disabled={loading}>
            {loading ? 'Carregando...' : 'Atualizar'}
          </button>
        </div>

        {/* Filtros */}
        <div style={{ display: 'flex', gap: 6, padding: '12px 0', flexWrap: 'wrap' }}>
          {filtros.map(f => (
            <button
              key={f}
              className={`btn btn-sm ${filtro === f ? 'btn-primary' : 'btn-outline'}`}
              onClick={() => setFiltro(f)}
            >
              {f ? STATUS_LABELS[f] : 'Todos'}
            </button>
          ))}
        </div>

        {/* Tabela */}
        {loading ? (
          <div className="loading-placeholder">Carregando...</div>
        ) : lancamentos.length === 0 ? (
          <div className="empty-state">
            <span style={{ fontSize: '2rem' }}>📷</span>
            <p>Nenhum lancamento {filtro ? STATUS_LABELS[filtro]?.toLowerCase() : ''} encontrado</p>
          </div>
        ) : (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>#</th>
                  <th>Status</th>
                  <th>Remetente</th>
                  <th>Doc</th>
                  <th>Destinatario</th>
                  <th>Rota</th>
                  <th>Conferente</th>
                  <th>Itens</th>
                  <th>Valor</th>
                  <th>Confianca</th>
                  <th>Operador</th>
                  <th>Data</th>
                  <th>Acoes</th>
                </tr>
              </thead>
              <tbody>
                {lancamentos.map(l => {
                  const dados = l.dados_revisados || l.dados_extraidos || {}
                  const qtdItens = dados.itens?.length || 0
                  const isExpanded = expandido === l.id
                  const canAction = ['pendente', 'revisado_operador'].includes(l.status)

                  return (
                    <tr key={l.id} className={isExpanded ? 'row-expanded' : ''}>
                      <td>{l.id}</td>
                      <td>
                        <span className={`badge ${STATUS_CLASSES[l.status] || 'info'}`}>
                          {STATUS_LABELS[l.status] || l.status}
                        </span>
                        {l.tipo === 'encomenda' && (
                          <span className="badge info" style={{ marginLeft: 4 }}>Encomenda</span>
                        )}
                      </td>
                      <td>{dados.remetente || '\u2014'}</td>
                      <td>
                        {dados.doc_remetente ? (
                          <span className="badge success" title={`CPF: ${dados.doc_remetente.cpf || '—'} | RG: ${dados.doc_remetente.rg || '—'}`}>
                            ✓
                          </span>
                        ) : (
                          <span style={{ color: '#999' }}>—</span>
                        )}
                      </td>
                      <td>{dados.destinatario || '\u2014'}</td>
                      <td>{dados.rota || '\u2014'}</td>
                      <td>{dados.conferente || '\u2014'}</td>
                      <td>{qtdItens}</td>
                      <td>{formatMoney(dados.valor_total)}</td>
                      <td>
                        <span className={`badge ${l.ocr_confianca >= 80 ? 'success' : l.ocr_confianca >= 50 ? 'warning' : 'danger'}`}>
                          {l.ocr_confianca || 0}%
                        </span>
                      </td>
                      <td>{l.nome_usuario_criou || '\u2014'}</td>
                      <td title={fmtDateTime(l.criado_em)}>{timeAgo(l.criado_em)}</td>
                      <td>
                        <div style={{ display: 'flex', gap: 4 }}>
                          <button
                            className="btn btn-sm btn-outline"
                            onClick={() => setExpandido(isExpanded ? null : l.id)}
                          >
                            {isExpanded ? 'Fechar' : 'Detalhes'}
                          </button>
                          {canAction && (
                            <>
                              <button
                                className="btn btn-sm btn-success"
                                onClick={() => aprovar(l.id)}
                                disabled={actionLoading === l.id}
                              >
                                Aprovar
                              </button>
                              <button
                                className="btn btn-sm btn-outline"
                                onClick={() => reanalisar(l.id)}
                                disabled={actionLoading === l.id}
                                title="Re-analisar com Gemini IA"
                              >
                                {actionLoading === l.id ? '...' : 'IA'}
                              </button>
                              <button
                                className="btn btn-sm btn-danger"
                                onClick={() => excluir(l.id)}
                                disabled={actionLoading === l.id}
                              >
                                Excluir
                              </button>
                            </>
                          )}
                        </div>

                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* TELA COMPLETA — Detalhes do Lancamento OCR */}
      {expandido && (() => {
        const l = lancamentos.find(x => x.id === expandido)
        if (!l) return null
        const dados = l.dados_revisados || l.dados_extraidos || {}
        const canAction = ['pendente', 'revisado_operador'].includes(l.status)
        const ed = editados[l.id]
        const dadosAtual = ed || dados
        const isEditando = !!ed
        const totalItens = (dadosAtual.itens || []).reduce((s, it) => s + (it.subtotal || (it.quantidade || 0) * (it.preco_unitario || 0)), 0)
        const totalVolumes = (dadosAtual.itens || []).reduce((s, it) => s + (parseInt(it.quantidade) || 0), 0)

        const I = { padding: '7px 10px', fontSize: '0.82rem', background: 'var(--bg-soft)', border: '1px solid var(--border)', borderRadius: 4, color: 'var(--text)', fontFamily: 'Sora, sans-serif', width: '100%', boxSizing: 'border-box' }
        const L = { fontSize: '0.75rem', fontWeight: 700, color: 'var(--text)', marginBottom: 3, display: 'block' }
        const RO = { ...I, opacity: 0.6, cursor: 'default' }

        return (
          <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'var(--bg)', zIndex: 1000, overflow: 'auto', padding: 20 }}>
            <div style={{ maxWidth: 960, margin: '0 auto' }}>
              <div className="card" style={{ padding: 16 }}>

                {/* HEADER */}
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                    <h2 style={{ fontSize: '1.05rem', margin: 0 }}>Detalhes do Lancamento OCR</h2>
                    <span className={`badge ${STATUS_CLASSES[l.status] || 'info'}`}>{STATUS_LABELS[l.status] || l.status}</span>
                    {l.tipo === 'encomenda' && <span className="badge info">Encomenda</span>}
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={L}>ID:</span>
                    <input style={{ ...RO, width: 60, textAlign: 'center', fontWeight: 700, fontSize: '1rem' }} value={`#${l.id}`} readOnly />
                    <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>Confianca: </span>
                    <span className={`badge ${l.ocr_confianca >= 80 ? 'success' : l.ocr_confianca >= 50 ? 'warning' : 'danger'}`}>{l.ocr_confianca || 0}%</span>
                  </div>
                </div>

                {/* ROW 1: Remetente + Destinatario */}
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 8 }}>
                  <div>
                    <label style={L}>Remetente:</label>
                    {isEditando ? (
                      <input style={I} value={dadosAtual.remetente || ''} onChange={e => editarCampo(l.id, 'remetente', e.target.value)} placeholder="Nome do remetente" />
                    ) : (
                      <input style={RO} value={dadosAtual.remetente || '\u2014'} readOnly />
                    )}
                  </div>
                  <div>
                    <label style={L}>Destinatario:</label>
                    {isEditando ? (
                      <input style={I} value={dadosAtual.destinatario || ''} onChange={e => editarCampo(l.id, 'destinatario', e.target.value)} placeholder="Nome do destinatario" />
                    ) : (
                      <input style={RO} value={dadosAtual.destinatario || '\u2014'} readOnly />
                    )}
                  </div>
                </div>

                {/* ROW 2: Rota, Conferente, Data, Operador */}
                <div style={{ display: 'grid', gridTemplateColumns: '2fr 1.5fr 1fr 1.5fr', gap: 8, marginBottom: 8 }}>
                  <div>
                    <label style={L}>Rota:</label>
                    {isEditando ? (
                      <select style={I} value={dadosAtual.rota || ''} onChange={e => editarCampo(l.id, 'rota', e.target.value)}>
                        <option value="">Selecione a rota</option>
                        {rotas.map(r => <option key={r.id_rota} value={`${r.origem} - ${r.destino}`}>{r.origem} - {r.destino}</option>)}
                      </select>
                    ) : (
                      <input style={RO} value={dadosAtual.rota || '\u2014'} readOnly />
                    )}
                  </div>
                  <div>
                    <label style={L}>Conferente:</label>
                    {isEditando ? (
                      <select style={I} value={dadosAtual.conferente || ''} onChange={e => editarCampo(l.id, 'conferente', e.target.value)}>
                        <option value="">Selecione o conferente</option>
                        {conferentes.map(c => <option key={c.id_conferente} value={c.nome_conferente || c.nome}>{c.nome_conferente || c.nome}</option>)}
                      </select>
                    ) : (
                      <input style={RO} value={dadosAtual.conferente || '\u2014'} readOnly />
                    )}
                  </div>
                  <div>
                    <label style={L}>Data:</label>
                    <input style={RO} value={fmtDateTime(l.criado_em).split(',')[0] || '\u2014'} readOnly />
                  </div>
                  <div>
                    <label style={L}>Operador:</label>
                    <input style={RO} value={l.nome_usuario_criou || '\u2014'} readOnly />
                  </div>
                </div>

                {/* ROW 3: Nota Fiscal + Observacoes */}
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 8 }}>
                  {/* Documento do remetente */}
                  <div style={{ padding: 10, border: '1px solid var(--border)', borderRadius: 6 }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 }}>
                      <label style={{ ...L, marginBottom: 0 }}>Documento do Remetente</label>
                      {isAdmin && dados.doc_remetente?.foto_doc_path && (
                        <button className="btn btn-sm" onClick={() => verDocFoto(l.id)} style={{ fontSize: '0.78rem' }}>Ver Doc</button>
                      )}
                    </div>
                    {dados.doc_remetente ? (
                      <div style={{ display: 'flex', gap: 16, fontSize: '0.85rem' }}>
                        {dados.doc_remetente.cpf && <span><strong>CPF:</strong> {dados.doc_remetente.cpf}</span>}
                        {dados.doc_remetente.rg && <span><strong>RG:</strong> {dados.doc_remetente.rg}</span>}
                        {dados.doc_remetente.tipo_doc && <span className="badge info">{dados.doc_remetente.tipo_doc}</span>}
                      </div>
                    ) : (
                      <span style={{ color: 'var(--text-muted)', fontSize: '0.82rem' }}>Nenhum documento registrado</span>
                    )}
                  </div>
                  {/* Observacoes */}
                  <div>
                    <label style={L}>Observacoes:</label>
                    {isEditando ? (
                      <textarea style={{ ...I, minHeight: 60, resize: 'vertical' }} value={dadosAtual.observacoes || ''} onChange={e => editarCampo(l.id, 'observacoes', e.target.value)} placeholder="Observacoes do lancamento..." />
                    ) : (
                      <textarea style={{ ...RO, minHeight: 60, resize: 'none' }} value={dadosAtual.observacoes || ''} readOnly />
                    )}
                  </div>
                </div>

                {/* Frete/Encomenda criado */}
                {l.id_frete && (
                  <div style={{ marginBottom: 8, padding: '8px 12px', background: 'rgba(5,150,105,0.08)', border: '1px solid var(--primary)', borderRadius: 6 }}>
                    <span className="badge success" style={{ fontSize: '0.85rem' }}>Frete #{l.id_frete} criado com sucesso</span>
                  </div>
                )}
                {l.id_encomenda && (
                  <div style={{ marginBottom: 8, padding: '8px 12px', background: 'rgba(5,150,105,0.08)', border: '1px solid var(--primary)', borderRadius: 6 }}>
                    <span className="badge success" style={{ fontSize: '0.85rem' }}>Encomenda #{l.id_encomenda} criada com sucesso</span>
                  </div>
                )}

                {/* Motivo rejeicao */}
                {l.motivo_rejeicao && (
                  <div style={{ marginBottom: 8, padding: '8px 12px', background: 'rgba(220,53,69,0.08)', border: '1px solid #dc3545', borderRadius: 6 }}>
                    <strong style={{ color: '#dc3545', fontSize: '0.85rem' }}>Motivo rejeicao:</strong>
                    <span style={{ marginLeft: 8, fontSize: '0.85rem' }}>{l.motivo_rejeicao}</span>
                  </div>
                )}

                {/* TABELA DE ITENS */}
                <div style={{ marginBottom: 0 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
                    <label style={{ ...L, marginBottom: 0, fontSize: '0.85rem' }}>Itens do Lancamento</label>
                    {isEditando && (
                      <button onClick={() => adicionarItem(l.id)} style={{ padding: '4px 12px', background: '#F59E0B', color: '#fff', border: 'none', borderRadius: 4, fontWeight: 700, cursor: 'pointer', fontSize: '0.78rem' }}>+ ADICIONAR ITEM</button>
                    )}
                  </div>
                  <div className="table-container" style={{ marginBottom: 0 }}>
                    <table>
                      <thead><tr>
                        <th style={{ width: 60 }}>Qtd</th>
                        <th>Descricao do Item</th>
                        <th style={{ width: 120 }}>Preco Unit.</th>
                        <th style={{ width: 120 }}>Subtotal</th>
                        <th style={{ width: 80 }}>Obs</th>
                        {isEditando && <th style={{ width: 40 }}></th>}
                      </tr></thead>
                      <tbody>
                        {(!dadosAtual.itens || dadosAtual.itens.length === 0) ? (
                          <tr><td colSpan={isEditando ? 6 : 5} style={{ textAlign: 'center', padding: 30, color: 'var(--text-muted)' }}>Nenhum item extraido</td></tr>
                        ) : dadosAtual.itens.map((item, idx) => {
                          const match = itensCatalogo.length ? findCatalogoMatch(item.nome_item, itensCatalogo) : null
                          const exactMatch = itensCatalogo.some(c => (c.nome_item || '').toLowerCase().trim() === (item.nome_item || '').toLowerCase().trim())
                          return (
                          <tr key={idx}>
                            <td style={{ textAlign: 'center' }}>
                              {isEditando ? (
                                <input type="number" min="1" value={item.quantidade} style={{ width: 45, textAlign: 'center', background: 'transparent', border: '1px solid transparent', color: 'inherit', fontSize: 'inherit' }}
                                  onFocus={e => e.target.style.borderColor = 'var(--primary)'} onBlur={e => e.target.style.borderColor = 'transparent'}
                                  onChange={e => editarItem(l.id, idx, 'quantidade', e.target.value)} />
                              ) : item.quantidade}
                            </td>
                            <td>
                              {isEditando ? (
                                <div>
                                  <input value={item.nome_item || ''} style={{ width: '100%', background: 'transparent', border: '1px solid transparent', color: 'inherit', fontSize: 'inherit' }}
                                    onFocus={e => e.target.style.borderColor = 'var(--primary)'} onBlur={e => e.target.style.borderColor = 'transparent'}
                                    onChange={e => editarItem(l.id, idx, 'nome_item', e.target.value)} />
                                  {match && (
                                    <div style={{ marginTop: 2 }}>
                                      <span
                                        onClick={() => { editarItem(l.id, idx, 'nome_item', match.nome_item); editarItem(l.id, idx, 'preco_unitario', match.preco_unitario_padrao || match.preco_padrao || item.preco_unitario) }}
                                        style={{ fontSize: '0.68rem', color: '#d97706', cursor: 'pointer', background: 'rgba(217,119,6,0.1)', padding: '1px 6px', borderRadius: 3, border: '1px solid rgba(217,119,6,0.3)' }}
                                        title={`Similaridade: ${Math.round(match.score * 100)}% — Clique para usar este nome`}
                                      >
                                        Similar: <strong>{match.nome_item}</strong> ({formatMoney(match.preco_unitario_padrao || match.preco_padrao)}) — usar?
                                      </span>
                                    </div>
                                  )}
                                </div>
                              ) : (
                                <div>
                                  {item.nome_item || '\u2014'}
                                  {match && (
                                    <div style={{ marginTop: 1 }}>
                                      <span style={{ fontSize: '0.65rem', color: '#d97706' }} title={`Similaridade: ${Math.round(match.score * 100)}%`}>
                                        Similar a: {match.nome_item}
                                      </span>
                                    </div>
                                  )}
                                </div>
                              )}
                            </td>
                            <td className="money">
                              {isEditando ? (
                                <input type="number" step="0.01" value={item.preco_unitario} style={{ width: 90, textAlign: 'right', background: 'transparent', border: '1px solid transparent', color: 'inherit', fontFamily: 'Space Mono, monospace' }}
                                  onFocus={e => e.target.style.borderColor = 'var(--primary)'} onBlur={e => e.target.style.borderColor = 'transparent'}
                                  onChange={e => editarItem(l.id, idx, 'preco_unitario', e.target.value)} />
                              ) : formatMoney(item.preco_unitario)}
                            </td>
                            <td className="money" style={{ fontWeight: 700 }}>{formatMoney(item.subtotal || (item.quantidade || 0) * (item.preco_unitario || 0))}</td>
                            <td>
                              {exactMatch && <span className="badge success" style={{ fontSize: '0.68rem' }}>OK</span>}
                              {!exactMatch && !match && item.nome_item && <span className="badge info" style={{ fontSize: '0.68rem' }}>NOVO</span>}
                              {item.preco_diferente && <span className="badge warning" title={`Padrao: ${formatMoney(item.preco_padrao)}`} style={{ fontSize: '0.68rem' }}>Difere</span>}
                            </td>
                            {isEditando && (
                              <td><button className="btn-sm danger" onClick={() => removerItem(l.id, idx)} style={{ padding: '2px 6px' }}>x</button></td>
                            )}
                          </tr>
                          )
                        })}
                      </tbody>
                    </table>
                  </div>
                </div>

                {/* TOTAIS */}
                {(dadosAtual.itens || []).length > 0 && (
                  <div style={{ borderTop: '2px solid var(--primary)', padding: '10px 0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div><label style={L}>Volumes:</label><span style={{ fontWeight: 700, fontSize: '1rem' }}>{totalVolumes}</span></div>
                    <div style={{ fontWeight: 700, fontSize: '1.2rem', color: 'var(--primary)', fontFamily: 'Space Mono, monospace' }}>TOTAL GERAL: {formatMoney(totalItens)}</div>
                  </div>
                )}

                {/* REJEITAR COM MOTIVO */}
                {canAction && (
                  <div style={{ marginTop: 8, padding: 12, background: 'var(--bg-soft)', borderRadius: 8, border: '1px solid var(--border)' }}>
                    <label style={{ ...L, marginBottom: 6 }}>Rejeitar com motivo:</label>
                    <div style={{ display: 'flex', gap: 8 }}>
                      <input type="text" placeholder="Motivo da rejeicao..." value={motivoRejeicao} onChange={e => setMotivoRejeicao(e.target.value)}
                        style={{ ...I, flex: 1 }} />
                      <button className="btn btn-sm btn-danger" onClick={() => rejeitar(l.id)} disabled={actionLoading === l.id}>Rejeitar</button>
                    </div>
                  </div>
                )}

                {/* Revisor info */}
                {l.nome_usuario_revisou && (
                  <p style={{ marginTop: 8, fontSize: '0.82rem', color: 'var(--text-muted)' }}>
                    Revisado por <strong>{l.nome_usuario_revisou}</strong> em {fmtDateTime(l.data_revisao)}
                  </p>
                )}

                {/* BOTOES */}
                <div style={{ display: 'flex', justifyContent: 'space-between', padding: '12px 0', borderTop: '1px solid var(--border)', marginTop: 8, flexWrap: 'wrap', gap: 6 }}>
                  <div style={{ display: 'flex', gap: 6 }}>
                    {canAction && !isEditando && (
                      <button className="btn-sm primary" onClick={() => iniciarEdicao(l)}>Editar Dados</button>
                    )}
                    {isEditando && (
                      <>
                        <button className="btn-sm primary" onClick={() => salvarEdicoes(l.id)} style={{ fontWeight: 700 }}>SALVAR EDICOES</button>
                        <button className="btn-sm" onClick={() => setEditados(prev => { const n = { ...prev }; delete n[l.id]; return n })}>Cancelar Edicao</button>
                      </>
                    )}
                    {canAction && (
                      <>
                        <button className="btn-sm primary" style={{ background: '#059669', color: '#fff', fontWeight: 700 }} onClick={() => aprovar(l.id)} disabled={actionLoading === l.id}>
                          {actionLoading === l.id ? 'Processando...' : 'APROVAR'}
                        </button>
                        <button className="btn-sm" onClick={() => reanalisar(l.id)} disabled={actionLoading === l.id} title="Re-analisar com Gemini IA">
                          {actionLoading === l.id ? '...' : 'Re-analisar IA'}
                        </button>
                        <button className="btn-sm danger" onClick={() => excluir(l.id)} disabled={actionLoading === l.id}>Excluir</button>
                      </>
                    )}
                  </div>
                  <div>
                    <button className="btn-sm" onClick={() => { setExpandido(null); setEditados(prev => { const n = { ...prev }; delete n[expandido]; return n }) }}>FECHAR (Esc)</button>
                  </div>
                </div>

              </div>
            </div>
          </div>
        )
      })()}

      {/* Modal foto documento — admin only */}
      {docFotoUrl && (
        <div onClick={fecharDocFoto} style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0,0,0,0.85)', display: 'flex', alignItems: 'center',
          justifyContent: 'center', zIndex: 9999, cursor: 'pointer', padding: 20
        }}>
          <div style={{ position: 'relative', maxWidth: '90vw', maxHeight: '90vh' }}>
            <img src={docFotoUrl} alt="Documento do remetente" style={{
              maxWidth: '100%', maxHeight: '85vh', borderRadius: 8, boxShadow: '0 4px 20px rgba(0,0,0,0.5)'
            }} />
            <div style={{
              position: 'absolute', top: -36, right: 0, color: '#fff',
              fontSize: '0.85rem', opacity: 0.7
            }}>
              Clique para fechar
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

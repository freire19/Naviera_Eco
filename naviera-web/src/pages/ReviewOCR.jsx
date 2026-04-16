import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'
import { useAuth } from '../App.jsx'

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
  const [actionLoading, setActionLoading] = useState(null)
  const [docFotoUrl, setDocFotoUrl] = useState(null)

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

                        {/* Detalhes expandidos */}
                        {isExpanded && (() => {
                          const ed = editados[l.id]
                          const dadosAtual = ed || dados
                          const isEditando = !!ed
                          const inputS = { padding: '4px 6px', fontSize: '0.78rem', background: 'var(--bg-soft)', border: '1px solid var(--border)', borderRadius: 3, color: 'var(--text)', width: '100%', boxSizing: 'border-box' }
                          return (
                          <div style={{ marginTop: 12, textAlign: 'left' }}>

                            {/* Botao editar */}
                            {!isEditando && l.status !== 'aprovado' && (
                              <button className="btn-sm primary" onClick={() => iniciarEdicao(l)} style={{ marginBottom: 8 }}>Editar dados</button>
                            )}
                            {isEditando && (
                              <button className="btn-sm primary" onClick={() => salvarEdicoes(l.id)} style={{ marginBottom: 8, marginRight: 6 }}>Salvar edicoes</button>
                            )}

                            {/* Campos editaveis: Remetente, Destinatario, Rota, Conferente */}
                            {isEditando && (
                              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 6, marginBottom: 10 }}>
                                <div><label style={{ fontSize: '0.7rem', fontWeight: 700 }}>Remetente:</label><input style={inputS} value={dadosAtual.remetente || ''} onChange={e => editarCampo(l.id, 'remetente', e.target.value)} /></div>
                                <div><label style={{ fontSize: '0.7rem', fontWeight: 700 }}>Destinatario:</label><input style={inputS} value={dadosAtual.destinatario || ''} onChange={e => editarCampo(l.id, 'destinatario', e.target.value)} /></div>
                                <div><label style={{ fontSize: '0.7rem', fontWeight: 700 }}>Rota:</label><input style={inputS} value={dadosAtual.rota || ''} onChange={e => editarCampo(l.id, 'rota', e.target.value)} /></div>
                                <div><label style={{ fontSize: '0.7rem', fontWeight: 700 }}>Conferente:</label><input style={inputS} value={dadosAtual.conferente || ''} onChange={e => editarCampo(l.id, 'conferente', e.target.value)} /></div>
                              </div>
                            )}

                            {/* Itens */}
                            <h4 style={{ marginBottom: 8 }}>Itens extraidos:</h4>
                            {dadosAtual.itens && dadosAtual.itens.length > 0 ? (
                              <table className="table-inner">
                                <thead>
                                  <tr>
                                    <th>Item</th>
                                    <th style={{ width: 60 }}>Qtd</th>
                                    <th style={{ width: 100 }}>Preco Unit.</th>
                                    <th style={{ width: 100 }}>Subtotal</th>
                                    <th>Obs</th>
                                  </tr>
                                </thead>
                                <tbody>
                                  {dadosAtual.itens.map((item, idx) => (
                                    <tr key={idx}>
                                      <td>{isEditando ? <input style={inputS} value={item.nome_item || ''} onChange={e => editarItem(l.id, idx, 'nome_item', e.target.value)} /> : item.nome_item}</td>
                                      <td>{isEditando ? <input type="number" style={{ ...inputS, width: 50, textAlign: 'center' }} value={item.quantidade} onChange={e => editarItem(l.id, idx, 'quantidade', e.target.value)} /> : item.quantidade}</td>
                                      <td>{isEditando ? <input type="number" step="0.01" style={{ ...inputS, width: 80, textAlign: 'right' }} value={item.preco_unitario} onChange={e => editarItem(l.id, idx, 'preco_unitario', e.target.value)} /> : formatMoney(item.preco_unitario)}</td>
                                      <td>{formatMoney(item.subtotal || (item.quantidade || 0) * (item.preco_unitario || 0))}</td>
                                      <td>
                                        {item.preco_diferente && (
                                          <span className="badge warning" title={`Padrao: ${formatMoney(item.preco_padrao)}`}>
                                            Preco difere
                                          </span>
                                        )}
                                        {item.item_novo && (
                                          <span className="badge info">Novo</span>
                                        )}
                                      </td>
                                    </tr>
                                  ))}
                                </tbody>
                              </table>
                            ) : (
                              <p style={{ color: '#888' }}>Nenhum item extraido</p>
                            )}

                            {/* Remetente / Destinatario */}
                            <div style={{ marginTop: 8, display: 'flex', gap: 12, flexWrap: 'wrap' }}>
                              {dados.remetente && (
                                <p><strong>Remetente:</strong> {dados.remetente}</p>
                              )}
                              {dados.destinatario && (
                                <p><strong>Destinatario:</strong> {dados.destinatario}</p>
                              )}
                            </div>

                            {/* Documento do remetente */}
                            {dados.doc_remetente && (
                              <div style={{
                                marginTop: 8, padding: '8px 12px', borderRadius: 6,
                                background: '#d4edda', border: '1px solid #c3e6cb'
                              }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                  <strong style={{ fontSize: '0.85rem' }}>Documento arquivado:</strong>
                                  {isAdmin && dados.doc_remetente.foto_doc_path && (
                                    <button
                                      className="btn btn-sm"
                                      onClick={() => verDocFoto(l.id)}
                                      style={{ fontSize: '0.8rem' }}
                                    >
                                      Ver Doc
                                    </button>
                                  )}
                                </div>
                                <div style={{ display: 'flex', gap: 16, marginTop: 4, fontSize: '0.9rem' }}>
                                  {dados.doc_remetente.cpf && (
                                    <span><strong>CPF:</strong> {dados.doc_remetente.cpf}</span>
                                  )}
                                  {dados.doc_remetente.rg && (
                                    <span><strong>RG:</strong> {dados.doc_remetente.rg}</span>
                                  )}
                                  {dados.doc_remetente.tipo_doc && (
                                    <span className="badge info">{dados.doc_remetente.tipo_doc}</span>
                                  )}
                                </div>
                              </div>
                            )}

                            {/* Rota e obs */}
                            {dados.rota && <p style={{ marginTop: 8 }}><strong>Rota:</strong> {dados.rota}</p>}
                            {dados.observacoes && <p><strong>Obs:</strong> {dados.observacoes}</p>}

                            {/* Frete criado */}
                            {l.id_frete && (
                              <p style={{ marginTop: 8 }}>
                                <span className="badge success">Frete #{l.id_frete} criado</span>
                              </p>
                            )}

                            {/* Motivo rejeicao */}
                            {l.motivo_rejeicao && (
                              <p style={{ marginTop: 8, color: '#dc3545' }}>
                                <strong>Motivo rejeicao:</strong> {l.motivo_rejeicao}
                              </p>
                            )}

                            {/* Acoes detalhadas */}
                            {canAction && (
                              <div style={{ marginTop: 12, padding: 12, background: '#f8f9fa', borderRadius: 8 }}>
                                <label style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>
                                  Rejeitar com motivo:
                                </label>
                                <div style={{ display: 'flex', gap: 8 }}>
                                  <input
                                    type="text"
                                    placeholder="Motivo da rejeicao..."
                                    value={motivoRejeicao}
                                    onChange={(e) => setMotivoRejeicao(e.target.value)}
                                    style={{ flex: 1, padding: 8, borderRadius: 6, border: '1px solid #ddd' }}
                                  />
                                  <button
                                    className="btn btn-sm btn-danger"
                                    onClick={() => rejeitar(l.id)}
                                    disabled={actionLoading === l.id}
                                  >
                                    Rejeitar
                                  </button>
                                </div>
                              </div>
                            )}

                            {/* Revisor */}
                            {l.nome_usuario_revisou && (
                              <p style={{ marginTop: 8, fontSize: '0.85rem', color: '#666' }}>
                                Revisado por {l.nome_usuario_revisou} em {fmtDateTime(l.data_revisao)}
                              </p>
                            )}
                          </div>
                        )})()}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

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

import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

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

export default function ReviewOCR({ viagemAtiva }) {
  const [lancamentos, setLancamentos] = useState([])
  const [loading, setLoading] = useState(false)
  const [filtro, setFiltro] = useState('revisado_operador')
  const [toast, setToast] = useState(null)
  const [expandido, setExpandido] = useState(null)
  const [motivoRejeicao, setMotivoRejeicao] = useState('')
  const [actionLoading, setActionLoading] = useState(null)

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  const carregar = useCallback(() => {
    setLoading(true)
    const params = filtro ? `?status=${filtro}` : ''
    api.get(`/ocr/lancamentos${params}`)
      .then(setLancamentos)
      .catch(() => showToast('Erro ao carregar lancamentos OCR', 'error'))
      .finally(() => setLoading(false))
  }, [filtro])

  useEffect(() => { carregar() }, [carregar])

  const aprovar = async (id, autoCadastrar = false) => {
    if (!autoCadastrar && !confirm('Aprovar este lancamento? Um frete sera criado automaticamente.')) return
    setActionLoading(id)
    try {
      const result = await api.put(`/ocr/lancamentos/${id}/aprovar`, { auto_cadastrar: autoCadastrar })
      showToast(`Frete #${result.frete?.numero_frete || result.frete?.id_frete} criado com sucesso!`)
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

  const filtros = ['', 'pendente', 'revisado_operador', 'aprovado', 'rejeitado']

  return (
    <div>
      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}

      <div className="card">
        <div className="card-header">
          <h3>Conferir Lancamentos OCR</h3>
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
                      </td>
                      <td>{dados.remetente || '\u2014'}</td>
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
                                {actionLoading === l.id ? '...' : 'Aprovar'}
                              </button>
                            </>
                          )}
                        </div>

                        {/* Detalhes expandidos */}
                        {isExpanded && (
                          <div style={{ marginTop: 12, textAlign: 'left' }}>
                            {/* Itens */}
                            <h4 style={{ marginBottom: 8 }}>Itens extraidos:</h4>
                            {dados.itens && dados.itens.length > 0 ? (
                              <table className="table-inner">
                                <thead>
                                  <tr>
                                    <th>Item</th>
                                    <th>Qtd</th>
                                    <th>Preco Unit.</th>
                                    <th>Subtotal</th>
                                    <th>Obs</th>
                                  </tr>
                                </thead>
                                <tbody>
                                  {dados.itens.map((item, idx) => (
                                    <tr key={idx}>
                                      <td>{item.nome_item}</td>
                                      <td>{item.quantidade}</td>
                                      <td>{formatMoney(item.preco_unitario)}</td>
                                      <td>{formatMoney(item.subtotal || item.quantidade * item.preco_unitario)}</td>
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
                        )}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}

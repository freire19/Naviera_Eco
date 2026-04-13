import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'
import { BarChart } from '../components/Charts.jsx'
import { exportCSV } from '../utils/export.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}

function formatDate(dateStr) {
  if (!dateStr) return '\u2014'
  const d = new Date(dateStr)
  return d.toLocaleDateString('pt-BR')
}

const TABS = ['Resumo', 'Passagens', 'Encomendas', 'Fretes', 'Saidas']

export default function Financeiro({ viagemAtiva }) {
  const [balanco, setBalanco] = useState(null)
  const [saidas, setSaidas] = useState([])
  const [loading, setLoading] = useState(false)
  const [loadingSaidas, setLoadingSaidas] = useState(false)
  const [modalAberto, setModalAberto] = useState(false)
  const [salvando, setSalvando] = useState(false)
  const [toast, setToast] = useState(null)
  const [modalExcluir, setModalExcluir] = useState(null)
  const [motivoExclusao, setMotivoExclusao] = useState('')

  // Tabs
  const [tabAtiva, setTabAtiva] = useState('Resumo')

  // Filters
  const [dataInicio, setDataInicio] = useState('')
  const [dataFim, setDataFim] = useState('')
  const [filtroTipo, setFiltroTipo] = useState('')

  // Detail data
  const [detPassagens, setDetPassagens] = useState([])
  const [detEncomendas, setDetEncomendas] = useState([])
  const [detFretes, setDetFretes] = useState([])
  const [loadingDetail, setLoadingDetail] = useState(false)

  const [form, setForm] = useState({
    descricao: '',
    valor: '',
    data: '',
    tipo: '',
    observacoes: ''
  })

  const mostrarToast = useCallback((msg, tipo = 'success') => {
    setToast({ msg, tipo })
    setTimeout(() => setToast(null), 3500)
  }, [])

  const carregarBalanco = useCallback(() => {
    if (!viagemAtiva) return
    api.get(`/financeiro/balanco?viagem_id=${viagemAtiva.id_viagem}`)
      .then(setBalanco)
      .catch(() => {})
  }, [viagemAtiva])

  const carregarSaidas = useCallback(() => {
    if (!viagemAtiva) return
    setLoadingSaidas(true)
    api.get(`/financeiro/saidas?viagem_id=${viagemAtiva.id_viagem}`)
      .then(data => setSaidas(Array.isArray(data) ? data : []))
      .catch(() => setSaidas([]))
      .finally(() => setLoadingSaidas(false))
  }, [viagemAtiva])

  const carregarDetalhes = useCallback(() => {
    if (!viagemAtiva) return
    setLoadingDetail(true)
    const params = `viagem_id=${viagemAtiva.id_viagem}${dataInicio ? `&data_inicio=${dataInicio}` : ''}${dataFim ? `&data_fim=${dataFim}` : ''}`
    Promise.all([
      api.get(`/financeiro/passagens?${params}`).catch(() => []),
      api.get(`/financeiro/encomendas?${params}`).catch(() => []),
      api.get(`/financeiro/fretes?${params}`).catch(() => [])
    ]).then(([p, e, f]) => {
      setDetPassagens(Array.isArray(p) ? p : [])
      setDetEncomendas(Array.isArray(e) ? e : [])
      setDetFretes(Array.isArray(f) ? f : [])
    }).finally(() => setLoadingDetail(false))
  }, [viagemAtiva, dataInicio, dataFim])

  useEffect(() => {
    if (!viagemAtiva) return
    setLoading(true)
    Promise.all([
      api.get(`/financeiro/balanco?viagem_id=${viagemAtiva.id_viagem}`).then(setBalanco).catch(() => {}),
      api.get(`/financeiro/saidas?viagem_id=${viagemAtiva.id_viagem}`).then(data => setSaidas(Array.isArray(data) ? data : [])).catch(() => setSaidas([]))
    ]).finally(() => setLoading(false))
  }, [viagemAtiva])

  // Load details when switching to detail tabs or when filters change
  useEffect(() => {
    if (['Passagens', 'Encomendas', 'Fretes'].includes(tabAtiva)) {
      carregarDetalhes()
    }
  }, [tabAtiva, carregarDetalhes])

  function abrirModal() {
    setForm({ descricao: '', valor: '', data: '', tipo: '', observacoes: '' })
    setModalAberto(true)
  }

  function fecharModal() {
    setModalAberto(false)
  }

  function handleChange(e) {
    const { name, value } = e.target
    setForm(prev => ({ ...prev, [name]: value }))
  }

  async function handleSalvar(e) {
    e.preventDefault()

    if (!form.descricao.trim()) {
      mostrarToast('Preencha a descricao.', 'error')
      return
    }
    if (!form.valor || Number(form.valor) <= 0) {
      mostrarToast('Informe um valor valido.', 'error')
      return
    }
    if (!form.data) {
      mostrarToast('Informe a data.', 'error')
      return
    }

    setSalvando(true)
    try {
      await api.post('/financeiro/saida', {
        id_viagem: viagemAtiva.id_viagem,
        descricao: form.descricao.trim(),
        valor_total: parseFloat(form.valor),
        data_vencimento: form.data,
        id_categoria: null,
        tipo: form.tipo.trim() || null,
        observacoes: form.observacoes.trim() || null
      })
      mostrarToast('Saida criada com sucesso!')
      fecharModal()
      carregarSaidas()
      carregarBalanco()
    } catch (err) {
      mostrarToast(err?.message || 'Erro ao criar saida.', 'error')
    } finally {
      setSalvando(false)
    }
  }

  function abrirExcluir(saida) {
    setModalExcluir(saida)
    setMotivoExclusao('')
  }

  function fecharExcluir() {
    setModalExcluir(null)
    setMotivoExclusao('')
  }

  async function confirmarExclusao() {
    if (!motivoExclusao.trim()) {
      mostrarToast('Informe o motivo da exclusao.', 'error')
      return
    }

    setSalvando(true)
    try {
      await api.delete(`/financeiro/saida/${modalExcluir.id_despesa || modalExcluir.id}`, {
        motivo: motivoExclusao.trim()
      })
      mostrarToast('Saida excluida com sucesso!')
      fecharExcluir()
      carregarSaidas()
      carregarBalanco()
    } catch (err) {
      mostrarToast(err?.message || 'Erro ao excluir saida.', 'error')
    } finally {
      setSalvando(false)
    }
  }

  // Export functions
  function exportarPassagens() {
    exportCSV(detPassagens, [
      { key: 'numero_bilhete', label: 'Bilhete' },
      { key: 'nome_passageiro', label: 'Passageiro' },
      { key: 'valor_total', label: 'Valor Total' },
      { key: 'valor_pago', label: 'Valor Pago' },
      { key: 'valor_devedor', label: 'Valor Devedor' },
      { key: 'status_passagem', label: 'Status' },
      { key: 'data_emissao', label: 'Data Emissao' }
    ], `passagens_viagem_${viagemAtiva.id_viagem}`)
  }

  function exportarEncomendas() {
    exportCSV(detEncomendas, [
      { key: 'numero_encomenda', label: 'Numero' },
      { key: 'remetente', label: 'Remetente' },
      { key: 'destinatario', label: 'Destinatario' },
      { key: 'total_a_pagar', label: 'Total a Pagar' },
      { key: 'valor_pago', label: 'Valor Pago' },
      { key: 'status_pagamento', label: 'Pagamento' },
      { key: 'entregue', label: 'Entregue' },
      { key: 'data_emissao', label: 'Data Emissao' }
    ], `encomendas_viagem_${viagemAtiva.id_viagem}`)
  }

  function exportarFretes() {
    exportCSV(detFretes, [
      { key: 'numero_frete', label: 'Numero' },
      { key: 'remetente_nome_temp', label: 'Remetente' },
      { key: 'destinatario_nome_temp', label: 'Destinatario' },
      { key: 'valor_nominal', label: 'Valor Nominal' },
      { key: 'valor_pago', label: 'Valor Pago' },
      { key: 'status', label: 'Status' },
      { key: 'data_emissao', label: 'Data Emissao' }
    ], `fretes_viagem_${viagemAtiva.id_viagem}`)
  }

  function exportarSaidas() {
    const filtered = filtroTipo ? saidas.filter(s => (s.tipo || '').toLowerCase().includes(filtroTipo.toLowerCase())) : saidas
    exportCSV(filtered, [
      { key: 'descricao', label: 'Descricao' },
      { key: 'valor_total', label: 'Valor' },
      { key: 'tipo', label: 'Tipo' },
      { key: 'data_vencimento', label: 'Data' },
      { key: 'observacoes', label: 'Observacoes' }
    ], `saidas_viagem_${viagemAtiva.id_viagem}`)
  }

  if (!viagemAtiva) {
    return (
      <div className="placeholder-page">
        <div className="ph-icon">{'\uD83D\uDCB0'}</div>
        <h2>Financeiro</h2>
        <p>Selecione uma viagem para ver o financeiro.</p>
      </div>
    )
  }

  // Filtered saidas for the table
  const saidasFiltradas = filtroTipo
    ? saidas.filter(s => (s.tipo || '').toLowerCase().includes(filtroTipo.toLowerCase()))
    : saidas

  // Bar chart data
  const barData = balanco ? [
    { label: 'Receitas', value: balanco.totalReceitas || 0, color: '#4ADE80' },
    { label: 'Despesas', value: balanco.totalDespesas || 0, color: '#EF4444' }
  ] : []

  return (
    <div>
      {loading && <p style={{ color: 'var(--text-muted)' }}>Carregando...</p>}

      {/* Toast */}
      {toast && (
        <div className={`toast ${toast.tipo}`}>
          {toast.msg}
        </div>
      )}

      {/* Tabs */}
      <div style={{ display: 'flex', gap: 4, marginBottom: '1rem', flexWrap: 'wrap' }}>
        {TABS.map(tab => (
          <button
            key={tab}
            onClick={() => setTabAtiva(tab)}
            className={tabAtiva === tab ? 'btn-primary' : 'btn-secondary'}
            style={{ padding: '6px 16px', fontSize: 13 }}
          >
            {tab}
          </button>
        ))}
      </div>

      {/* ===== TAB: Resumo ===== */}
      {tabAtiva === 'Resumo' && (
        <>
          {/* Balance Cards */}
          {balanco && (
            <>
              <div className="dash-grid">
                <div className="stat-card primary">
                  <span className="stat-label">Passagens</span>
                  <span className="stat-value money">{formatMoney(balanco.receitas.passagens)}</span>
                </div>
                <div className="stat-card info">
                  <span className="stat-label">Encomendas</span>
                  <span className="stat-value money">{formatMoney(balanco.receitas.encomendas)}</span>
                </div>
                <div className="stat-card warning">
                  <span className="stat-label">Fretes</span>
                  <span className="stat-value money">{formatMoney(balanco.receitas.fretes)}</span>
                </div>
              </div>

              <div className="dash-grid">
                <div className="stat-card success">
                  <span className="stat-label">Total Receitas</span>
                  <span className="stat-value money positive">{formatMoney(balanco.totalReceitas)}</span>
                </div>
                <div className="stat-card" style={{ borderLeft: '3px solid var(--danger)' }}>
                  <span className="stat-label">Total Despesas</span>
                  <span className="stat-value money negative">{formatMoney(balanco.totalDespesas)}</span>
                </div>
                <div className="stat-card" style={{ borderLeft: `3px solid ${balanco.saldo >= 0 ? 'var(--success)' : 'var(--danger)'}` }}>
                  <span className="stat-label">Saldo</span>
                  <span className={`stat-value money ${balanco.saldo >= 0 ? 'positive' : 'negative'}`}>
                    {formatMoney(balanco.saldo)}
                  </span>
                </div>
              </div>

              {/* BarChart receitas vs despesas */}
              {(balanco.totalReceitas > 0 || balanco.totalDespesas > 0) && (
                <div className="dash-grid" style={{ marginTop: '1rem' }}>
                  <div className="card" style={{ padding: '1rem', textAlign: 'center' }}>
                    <h4 style={{ marginBottom: '0.75rem', color: 'var(--text-primary)' }}>Receitas vs Despesas</h4>
                    <BarChart data={barData} width={300} height={200} />
                  </div>
                </div>
              )}
            </>
          )}
        </>
      )}

      {/* ===== TAB: Passagens ===== */}
      {tabAtiva === 'Passagens' && (
        <div className="card">
          <div className="card-header">
            <h3>Passagens Detalhado</h3>
            <div className="toolbar">
              <input type="date" value={dataInicio} onChange={e => setDataInicio(e.target.value)} title="Data inicio" style={{ fontSize: 12 }} />
              <input type="date" value={dataFim} onChange={e => setDataFim(e.target.value)} title="Data fim" style={{ fontSize: 12 }} />
              {detPassagens.length > 0 && (
                <button className="btn-sm primary" onClick={exportarPassagens}>Exportar CSV</button>
              )}
            </div>
          </div>
          {loadingDetail ? (
            <p style={{ padding: '1rem', color: 'var(--text-muted)' }}>Carregando...</p>
          ) : (
            <>
              {detPassagens.length > 0 && (
                <div style={{ padding: '0.75rem 1rem', borderBottom: '1px solid var(--border)', display: 'flex', gap: 16, fontSize: 13 }}>
                  <span>Total: <strong>{detPassagens.length}</strong></span>
                  <span>Valor Total: <strong className="money">{formatMoney(detPassagens.reduce((s, p) => s + (parseFloat(p.valor_total) || 0), 0))}</strong></span>
                  <span>Pago: <strong className="money positive">{formatMoney(detPassagens.reduce((s, p) => s + (parseFloat(p.valor_pago) || 0), 0))}</strong></span>
                </div>
              )}
              <div className="table-container">
                <table>
                  <thead>
                    <tr>
                      <th>Bilhete</th>
                      <th>Passageiro</th>
                      <th>Valor Total</th>
                      <th>Valor Pago</th>
                      <th>Devedor</th>
                      <th>Status</th>
                      <th>Data</th>
                    </tr>
                  </thead>
                  <tbody>
                    {detPassagens.map(p => (
                      <tr key={p.id_passagem}>
                        <td>{p.numero_bilhete}</td>
                        <td>{p.nome_passageiro || '\u2014'}</td>
                        <td className="money">{formatMoney(p.valor_total)}</td>
                        <td className="money">{formatMoney(p.valor_pago)}</td>
                        <td className="money">{formatMoney(p.valor_devedor)}</td>
                        <td><span className={`badge ${p.status_passagem === 'PAGO' ? 'success' : 'warning'}`}>{p.status_passagem || 'Pendente'}</span></td>
                        <td>{formatDate(p.data_emissao)}</td>
                      </tr>
                    ))}
                    {detPassagens.length === 0 && (
                      <tr><td colSpan="7" style={{ textAlign: 'center', padding: 30, color: 'var(--text-muted)' }}>Nenhuma passagem encontrada</td></tr>
                    )}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </div>
      )}

      {/* ===== TAB: Encomendas ===== */}
      {tabAtiva === 'Encomendas' && (
        <div className="card">
          <div className="card-header">
            <h3>Encomendas Detalhado</h3>
            <div className="toolbar">
              <input type="date" value={dataInicio} onChange={e => setDataInicio(e.target.value)} title="Data inicio" style={{ fontSize: 12 }} />
              <input type="date" value={dataFim} onChange={e => setDataFim(e.target.value)} title="Data fim" style={{ fontSize: 12 }} />
              {detEncomendas.length > 0 && (
                <button className="btn-sm primary" onClick={exportarEncomendas}>Exportar CSV</button>
              )}
            </div>
          </div>
          {loadingDetail ? (
            <p style={{ padding: '1rem', color: 'var(--text-muted)' }}>Carregando...</p>
          ) : (
            <>
              {detEncomendas.length > 0 && (
                <div style={{ padding: '0.75rem 1rem', borderBottom: '1px solid var(--border)', display: 'flex', gap: 16, fontSize: 13 }}>
                  <span>Total: <strong>{detEncomendas.length}</strong></span>
                  <span>A Pagar: <strong className="money">{formatMoney(detEncomendas.reduce((s, e) => s + (parseFloat(e.total_a_pagar) || 0), 0))}</strong></span>
                  <span>Pago: <strong className="money positive">{formatMoney(detEncomendas.reduce((s, e) => s + (parseFloat(e.valor_pago) || 0), 0))}</strong></span>
                </div>
              )}
              <div className="table-container">
                <table>
                  <thead>
                    <tr>
                      <th>Numero</th>
                      <th>Remetente</th>
                      <th>Destinatario</th>
                      <th>Total a Pagar</th>
                      <th>Pago</th>
                      <th>Pagamento</th>
                      <th>Entrega</th>
                      <th>Data</th>
                    </tr>
                  </thead>
                  <tbody>
                    {detEncomendas.map(e => (
                      <tr key={e.id_encomenda}>
                        <td>{e.numero_encomenda}</td>
                        <td>{e.remetente || '\u2014'}</td>
                        <td>{e.destinatario || '\u2014'}</td>
                        <td className="money">{formatMoney(e.total_a_pagar)}</td>
                        <td className="money">{formatMoney(e.valor_pago)}</td>
                        <td><span className={`badge ${e.status_pagamento === 'PAGO' ? 'success' : 'warning'}`}>{e.status_pagamento || 'Pendente'}</span></td>
                        <td><span className={`badge ${e.entregue ? 'success' : 'warning'}`}>{e.entregue ? 'Sim' : 'Nao'}</span></td>
                        <td>{formatDate(e.data_emissao)}</td>
                      </tr>
                    ))}
                    {detEncomendas.length === 0 && (
                      <tr><td colSpan="8" style={{ textAlign: 'center', padding: 30, color: 'var(--text-muted)' }}>Nenhuma encomenda encontrada</td></tr>
                    )}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </div>
      )}

      {/* ===== TAB: Fretes ===== */}
      {tabAtiva === 'Fretes' && (
        <div className="card">
          <div className="card-header">
            <h3>Fretes Detalhado</h3>
            <div className="toolbar">
              <input type="date" value={dataInicio} onChange={e => setDataInicio(e.target.value)} title="Data inicio" style={{ fontSize: 12 }} />
              <input type="date" value={dataFim} onChange={e => setDataFim(e.target.value)} title="Data fim" style={{ fontSize: 12 }} />
              {detFretes.length > 0 && (
                <button className="btn-sm primary" onClick={exportarFretes}>Exportar CSV</button>
              )}
            </div>
          </div>
          {loadingDetail ? (
            <p style={{ padding: '1rem', color: 'var(--text-muted)' }}>Carregando...</p>
          ) : (
            <>
              {detFretes.length > 0 && (
                <div style={{ padding: '0.75rem 1rem', borderBottom: '1px solid var(--border)', display: 'flex', gap: 16, fontSize: 13 }}>
                  <span>Total: <strong>{detFretes.length}</strong></span>
                  <span>Valor Nominal: <strong className="money">{formatMoney(detFretes.reduce((s, f) => s + (parseFloat(f.valor_nominal) || 0), 0))}</strong></span>
                  <span>Pago: <strong className="money positive">{formatMoney(detFretes.reduce((s, f) => s + (parseFloat(f.valor_pago) || 0), 0))}</strong></span>
                </div>
              )}
              <div className="table-container">
                <table>
                  <thead>
                    <tr>
                      <th>Numero</th>
                      <th>Remetente</th>
                      <th>Destinatario</th>
                      <th>Valor Nominal</th>
                      <th>Valor Pago</th>
                      <th>Status</th>
                      <th>Data</th>
                    </tr>
                  </thead>
                  <tbody>
                    {detFretes.map(f => (
                      <tr key={f.id_frete}>
                        <td>{f.numero_frete}</td>
                        <td>{f.remetente_nome_temp || '\u2014'}</td>
                        <td>{f.destinatario_nome_temp || '\u2014'}</td>
                        <td className="money">{formatMoney(f.valor_nominal)}</td>
                        <td className="money">{formatMoney(f.valor_pago)}</td>
                        <td><span className={`badge ${f.status === 'PAGO' ? 'success' : 'warning'}`}>{f.status || 'Pendente'}</span></td>
                        <td>{formatDate(f.data_emissao)}</td>
                      </tr>
                    ))}
                    {detFretes.length === 0 && (
                      <tr><td colSpan="7" style={{ textAlign: 'center', padding: 30, color: 'var(--text-muted)' }}>Nenhum frete encontrado</td></tr>
                    )}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </div>
      )}

      {/* ===== TAB: Saidas ===== */}
      {tabAtiva === 'Saidas' && (
        <div className="card">
          <div className="card-header">
            <h3>Despesas (Saidas)</h3>
            <div className="toolbar">
              <input
                type="text"
                value={filtroTipo}
                onChange={e => setFiltroTipo(e.target.value)}
                placeholder="Filtrar por tipo..."
                style={{ fontSize: 12, width: 150 }}
              />
              <button className="btn-primary" onClick={abrirModal}>
                + Nova Saida
              </button>
              {saidasFiltradas.length > 0 && (
                <button className="btn-sm primary" onClick={exportarSaidas}>Exportar CSV</button>
              )}
            </div>
          </div>

          {saidasFiltradas.length > 0 && (
            <div style={{ padding: '0.75rem 1rem', borderBottom: '1px solid var(--border)', display: 'flex', gap: 16, fontSize: 13 }}>
              <span>Total: <strong>{saidasFiltradas.length}</strong></span>
              <span>Valor: <strong className="money negative">{formatMoney(saidasFiltradas.reduce((s, d) => s + (parseFloat(d.valor_total) || 0), 0))}</strong></span>
            </div>
          )}

          <div className="table-container">
            {loadingSaidas ? (
              <p style={{ padding: '1rem', color: 'var(--text-muted)' }}>Carregando saidas...</p>
            ) : saidasFiltradas.length === 0 ? (
              <p style={{ padding: '1rem', color: 'var(--text-muted)' }}>Nenhuma saida registrada{filtroTipo ? ' com este filtro' : ' nesta viagem'}.</p>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>Data</th>
                    <th>Descricao</th>
                    <th>Tipo</th>
                    <th>Valor</th>
                    <th>Acoes</th>
                  </tr>
                </thead>
                <tbody>
                  {saidasFiltradas.map((s, idx) => (
                    <tr key={s.id_despesa || s.id || idx}>
                      <td>{formatDate(s.data_vencimento)}</td>
                      <td>{s.descricao}</td>
                      <td>
                        {s.tipo ? <span className="badge">{s.tipo}</span> : '\u2014'}
                      </td>
                      <td className="money">{formatMoney(s.valor_total)}</td>
                      <td>
                        <button
                          className="btn-sm danger"
                          onClick={() => abrirExcluir(s)}
                        >
                          Excluir
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      )}

      {/* Modal Nova Saida */}
      {modalAberto && (
        <div className="modal-overlay" onClick={fecharModal}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3>Nova Saida</h3>
            <form onSubmit={handleSalvar}>
              <div className="form-grid">
                <div className="form-group full-width">
                  <label>Descricao *</label>
                  <input
                    type="text"
                    name="descricao"
                    value={form.descricao}
                    onChange={handleChange}
                    placeholder="Ex: Combustivel, Manutencao..."
                    autoFocus
                  />
                </div>
                <div className="form-group">
                  <label>Valor (R$) *</label>
                  <input
                    type="number"
                    name="valor"
                    value={form.valor}
                    onChange={handleChange}
                    placeholder="0.00"
                    min="0.01"
                    step="0.01"
                  />
                </div>
                <div className="form-group">
                  <label>Data *</label>
                  <input
                    type="date"
                    name="data"
                    value={form.data}
                    onChange={handleChange}
                  />
                </div>
                <div className="form-group">
                  <label>Tipo</label>
                  <input
                    type="text"
                    name="tipo"
                    value={form.tipo}
                    onChange={handleChange}
                    placeholder="Ex: Combustivel, Alimentacao..."
                  />
                </div>
                <div className="form-group full-width">
                  <label>Observacoes</label>
                  <textarea
                    name="observacoes"
                    value={form.observacoes}
                    onChange={handleChange}
                    rows={3}
                    placeholder="Observacoes adicionais..."
                  />
                </div>
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={fecharModal} disabled={salvando}>
                  Cancelar
                </button>
                <button type="submit" className="btn-primary" disabled={salvando}>
                  {salvando ? 'Salvando...' : 'Salvar'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Confirmar Exclusao */}
      {modalExcluir && (
        <div className="modal-overlay" onClick={fecharExcluir}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3>Excluir Saida</h3>
            <p>
              Tem certeza que deseja excluir a despesa <strong>{modalExcluir.descricao}</strong> de{' '}
              <strong>{formatMoney(modalExcluir.valor_total)}</strong>?
            </p>
            <div className="form-group" style={{ marginTop: '1rem' }}>
              <label>Motivo da exclusao *</label>
              <input
                type="text"
                value={motivoExclusao}
                onChange={e => setMotivoExclusao(e.target.value)}
                placeholder="Informe o motivo..."
                autoFocus
              />
            </div>
            <div className="modal-actions">
              <button type="button" className="btn-secondary" onClick={fecharExcluir} disabled={salvando}>
                Cancelar
              </button>
              <button type="button" className="btn-primary" onClick={confirmarExclusao} disabled={salvando}>
                {salvando ? 'Excluindo...' : 'Confirmar Exclusao'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

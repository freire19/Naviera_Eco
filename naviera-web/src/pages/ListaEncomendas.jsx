import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}

export default function ListaEncomendas({ viagemAtiva, onNavigate }) {
  const [encomendas, setEncomendas] = useState([])
  const [loading, setLoading] = useState(false)
  const [filtrosVisiveis, setFiltrosVisiveis] = useState(true)
  const [toast, setToast] = useState(null)

  const [viagens, setViagens] = useState([])
  const [rotas, setRotas] = useState([])
  const [filtroViagem, setFiltroViagem] = useState(viagemAtiva?.id_viagem || '')
  const [filtroRota, setFiltroRota] = useState('')
  const [filtroPagamento, setFiltroPagamento] = useState('')
  const [filtroEntrega, setFiltroEntrega] = useState('')
  const [filtroCliente, setFiltroCliente] = useState('')
  const [filtroNumero, setFiltroNumero] = useState('')
  const [filtroItem, setFiltroItem] = useState('')

  function showToast(msg, type = 'success') {
    setToast({ msg, type }); setTimeout(() => setToast(null), 3500)
  }

  useEffect(() => {
    Promise.allSettled([
      api.get('/viagens').then(setViagens),
      api.get('/rotas').then(setRotas)
    ]).catch(() => {})
  }, [])

  // Cache de itens por encomenda para filtro "Contem Produto"
  const [itensCache, setItensCache] = useState({})

  const carregar = useCallback(() => {
    if (!filtroViagem) return
    setLoading(true)
    api.get(`/encomendas?viagem_id=${filtroViagem}`)
      .then(async (data) => {
        setEncomendas(data)
        // Carregar itens de todas encomendas para filtro de produto
        const cache = {}
        await Promise.allSettled(data.map(async (e) => {
          try {
            const itens = await api.get(`/encomendas/${e.id_encomenda}/itens`)
            cache[e.id_encomenda] = itens
          } catch { cache[e.id_encomenda] = [] }
        }))
        setItensCache(cache)
      })
      .catch(() => showToast('Erro ao carregar', 'error'))
      .finally(() => setLoading(false))
  }, [filtroViagem])

  useEffect(() => { carregar() }, [carregar])

  const filtradas = encomendas.filter(e => {
    if (filtroRota && e.rota && !e.rota.toLowerCase().includes(filtroRota.toLowerCase())) return false
    if (filtroPagamento === 'PAGO' && e.status_pagamento !== 'PAGO') return false
    if (filtroPagamento === 'ABERTO' && e.status_pagamento === 'PAGO') return false
    if (filtroEntrega === 'SIM' && !e.entregue) return false
    if (filtroEntrega === 'NAO' && e.entregue) return false
    if (filtroCliente) {
      const q = filtroCliente.toLowerCase()
      if (!(e.remetente || '').toLowerCase().includes(q) && !(e.destinatario || '').toLowerCase().includes(q)) return false
    }
    if (filtroNumero && !String(e.numero_encomenda).includes(filtroNumero)) return false
    if (filtroItem) {
      const q = filtroItem.toLowerCase()
      const eItens = itensCache[e.id_encomenda] || []
      const temItem = eItens.some(it => (it.descricao || '').toLowerCase().includes(q))
      if (!temItem) return false
    }
    return true
  })

  const totalLancado = filtradas.reduce((s, e) => s + (parseFloat(e.total_a_pagar) || 0), 0)
  const totalRecebido = filtradas.reduce((s, e) => s + (parseFloat(e.valor_pago) || 0), 0)
  const totalAReceber = totalLancado - totalRecebido

  const I = { padding: '7px 10px', fontSize: '0.82rem', background: 'var(--bg-soft)', border: '1px solid var(--border)', borderRadius: 4, color: 'var(--text)', fontFamily: 'Sora, sans-serif', width: '100%', boxSizing: 'border-box' }
  const L = { fontSize: '0.72rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: 3, display: 'block' }

  if (!viagemAtiva) {
    return <div className="placeholder-page"><div className="ph-icon">📦</div><h2>Lista de Encomendas</h2><p>Selecione uma viagem.</p></div>
  }

  return (
    <div style={{ display: 'flex', gap: 16 }}>
      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}

      <div style={{ flex: 1, minWidth: 0 }}>
        <div className="card" style={{ marginBottom: 12 }}>
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th style={{ width: 50 }}>N°</th>
                  <th>Remetente</th>
                  <th>Destinatario</th>
                  <th>Rota</th>
                  <th>Valor Total</th>
                  <th>Pago</th>
                  <th>A Receber</th>
                  <th>Doc. Recebedor</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr><td colSpan="9">Carregando...</td></tr>
                ) : filtradas.length === 0 ? (
                  <tr><td colSpan="9" style={{ textAlign: 'center', padding: 30, color: 'var(--text-muted)' }}>Nenhuma encomenda encontrada</td></tr>
                ) : filtradas.map((e, idx) => {
                  const devedor = Math.max(0, (parseFloat(e.total_a_pagar) || 0) - (parseFloat(e.desconto) || 0) - (parseFloat(e.valor_pago) || 0))
                  const isPago = devedor <= 0.01
                  const isEntregue = e.entregue
                  const statusPg = isPago ? 'PAGO' : 'FALTA'
                  const statusEnt = isEntregue ? 'ENTREGUE' : 'PENDENTE'
                  const statusText = `${statusPg} / ${statusEnt}`
                  const statusColor = isPago && isEntregue ? '#059669' : isPago ? '#059669' : '#DC2626'
                  return (
                    <tr key={e.id_encomenda} className="clickable"
                        style={{ background: idx % 2 === 0 ? 'transparent' : 'rgba(255,255,255,0.03)' }}
                        onDoubleClick={() => {
                          if (onNavigate) {
                            sessionStorage.setItem('encomenda_editar', JSON.stringify(e))
                            onNavigate('nova-encomenda')
                          }
                        }}
                        title="Duplo-clique para abrir/editar">
                      <td style={{ fontWeight: 600 }}>{e.numero_encomenda}</td>
                      <td>{(e.remetente || '—').toUpperCase()}</td>
                      <td>{(e.destinatario || '—').toUpperCase()}</td>
                      <td>{e.rota || '—'}</td>
                      <td className="money">{formatMoney(e.total_a_pagar)}</td>
                      <td className="money">{formatMoney(e.valor_pago)}</td>
                      <td className="money" style={{ fontWeight: devedor > 0 ? 700 : 400, color: devedor > 0 ? '#DC2626' : undefined }}>{formatMoney(devedor)}</td>
                      <td>{e.doc_recebedor || '—'}</td>
                      <td style={{ fontWeight: 700, fontSize: '0.75rem', color: statusColor }}>{statusText}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </div>

        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-around', padding: '8px 0' }}>
            <div style={{ textAlign: 'center' }}><div style={{ fontSize: '0.72rem', fontWeight: 600, color: 'var(--text-muted)' }}>Qtd. Encomendas</div><div style={{ fontSize: '1.2rem', fontWeight: 700 }}>{filtradas.length}</div></div>
            <div style={{ textAlign: 'center' }}><div style={{ fontSize: '0.72rem', fontWeight: 600, color: 'var(--text-muted)' }}>Total Lancado</div><div style={{ fontSize: '1.2rem', fontWeight: 700, color: 'var(--primary)', fontFamily: 'Space Mono, monospace' }}>{formatMoney(totalLancado)}</div></div>
            <div style={{ textAlign: 'center' }}><div style={{ fontSize: '0.72rem', fontWeight: 600, color: 'var(--text-muted)' }}>Total Recebido</div><div style={{ fontSize: '1.2rem', fontWeight: 700, color: 'var(--primary)', fontFamily: 'Space Mono, monospace' }}>{formatMoney(totalRecebido)}</div></div>
            <div style={{ textAlign: 'center' }}><div style={{ fontSize: '0.72rem', fontWeight: 600, color: 'var(--text-muted)' }}>A Receber</div><div style={{ fontSize: '1.3rem', fontWeight: 700, color: 'var(--danger)', fontFamily: 'Space Mono, monospace' }}>{formatMoney(totalAReceber)}</div></div>
          </div>
        </div>
      </div>

      {/* BOTAO TOGGLE FILTROS */}
      <div onClick={() => setFiltrosVisiveis(prev => !prev)}
        style={{
          writingMode: 'vertical-rl', textOrientation: 'mixed',
          cursor: 'pointer', padding: '12px 4px', background: 'var(--bg-card)',
          border: '1px solid var(--border)', borderRadius: '4px 0 0 4px',
          fontSize: '0.72rem', fontWeight: 700, color: 'var(--primary)',
          display: 'flex', alignItems: 'center', gap: 4, flexShrink: 0,
          userSelect: 'none', letterSpacing: '0.05em'
        }}
        title={filtrosVisiveis ? 'Ocultar filtros' : 'Mostrar filtros'}>
        {filtrosVisiveis ? '▶' : '◀'} FILTROS
      </div>

      {/* PAINEL FILTROS */}
      {filtrosVisiveis && (
      <div style={{ width: 280, flexShrink: 0 }}>
        <div className="card" style={{ position: 'sticky', top: 60 }}>
          <h3 style={{ marginBottom: 12, fontSize: '0.95rem', textTransform: 'uppercase' }}>Filtros de Busca</h3>

          <div style={{ marginBottom: 8 }}><label style={L}>Viagem:</label>
            <select style={I} value={filtroViagem} onChange={e => setFiltroViagem(e.target.value)}>
              {viagens.map(v => <option key={v.id_viagem} value={v.id_viagem}>{v.id_viagem} - {v.data_viagem} - {v.nome_rota || ''}</option>)}
            </select></div>

          <div style={{ marginBottom: 8 }}><label style={L}>Rota:</label>
            <select style={I} value={filtroRota} onChange={e => setFiltroRota(e.target.value)}>
              <option value="">Todas as Rotas</option>
              {rotas.map(r => <option key={r.id_rota} value={`${r.origem} - ${r.destino}`}>{r.origem} - {r.destino}</option>)}
            </select></div>

          <div style={{ marginBottom: 8 }}><label style={L}>Status Pagamento:</label>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 3, fontSize: '0.82rem' }}>
              <label><input type="radio" name="pgto" checked={filtroPagamento === ''} onChange={() => setFiltroPagamento('')} /> Todos</label>
              <label style={{ color: 'var(--success)' }}><input type="radio" name="pgto" checked={filtroPagamento === 'PAGO'} onChange={() => setFiltroPagamento('PAGO')} /> Quitados</label>
              <label style={{ color: 'var(--danger)' }}><input type="radio" name="pgto" checked={filtroPagamento === 'ABERTO'} onChange={() => setFiltroPagamento('ABERTO')} /> Abertos</label>
            </div></div>

          <div style={{ marginBottom: 8 }}><label style={L}>Status Entrega:</label>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 3, fontSize: '0.82rem' }}>
              <label><input type="radio" name="ent" checked={filtroEntrega === ''} onChange={() => setFiltroEntrega('')} /> Todos</label>
              <label style={{ color: 'var(--success)' }}><input type="radio" name="ent" checked={filtroEntrega === 'SIM'} onChange={() => setFiltroEntrega('SIM')} /> Entregues</label>
              <label style={{ color: 'var(--danger)' }}><input type="radio" name="ent" checked={filtroEntrega === 'NAO'} onChange={() => setFiltroEntrega('NAO')} /> Pendentes</label>
            </div></div>

          <div style={{ marginBottom: 8 }}><label style={L}>Cliente (Nome):</label>
            <input style={I} placeholder="Digite parte do nome..." value={filtroCliente} onChange={e => setFiltroCliente(e.target.value)} /></div>

          <div style={{ marginBottom: 8 }}><label style={L}>N° Encomenda:</label>
            <input style={I} placeholder="Ex: 5" value={filtroNumero} onChange={e => setFiltroNumero(e.target.value)} /></div>

          <div style={{ marginBottom: 12 }}><label style={L}>Contem o Produto (Item):</label>
            <input style={I} placeholder="Ex: Cimento, Motor..." value={filtroItem} onChange={e => setFiltroItem(e.target.value)} /></div>

          <button className="btn-primary" style={{ marginBottom: 8 }} onClick={() => {}}>IMPRIMIR LISTA</button>
          <button className="btn-secondary" style={{ width: '100%', marginBottom: 8 }} onClick={() => { setFiltroPagamento(''); setFiltroEntrega(''); setFiltroCliente(''); setFiltroNumero(''); setFiltroRota(''); setFiltroItem('') }}>Limpar Filtros</button>
          {onNavigate && <button className="btn-secondary" style={{ width: '100%' }} onClick={() => onNavigate('nova-encomenda')}>FECHAR</button>}
        </div>
      </div>
      )}
    </div>
  )
}

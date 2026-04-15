import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'
import { printReciboEncomenda } from '../utils/print.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}
function formatDate(val) {
  if (!val) return '—'
  const s = String(val)
  if (/^\d{2}\/\d{2}\/\d{4}$/.test(s)) return s
  try {
    const iso = s.includes('T') ? s.substring(0, 10) : s
    const p = iso.split('-')
    if (p.length === 3) return `${p[2]}/${p[1]}/${p[0]}`
    return s
  } catch { return s }
}

const ITEM_VAZIO = { quantidade: 1, descricao: '', valor_unitario: '', valor_total: '', local_armazenamento: '' }

export default function Encomendas({ viagemAtiva, onNavigate }) {
  const [encomendas, setEncomendas] = useState([])
  const [loading, setLoading] = useState(false)
  const [selecionada, setSelecionada] = useState(null)
  const [editando, setEditando] = useState(false)
  const [salvando, setSalvando] = useState(false)
  const [toast, setToast] = useState(null)

  // Form
  const [remetente, setRemetente] = useState('')
  const [destinatario, setDestinatario] = useState('')
  const [idRota, setIdRota] = useState('')
  const [observacoes, setObservacoes] = useState('')
  const [statusEntrega, setStatusEntrega] = useState('')
  const [nomeRecebedor, setNomeRecebedor] = useState('Pendente de Entrega')
  const [docRecebedor, setDocRecebedor] = useState('')
  const [numEncomenda, setNumEncomenda] = useState('1')

  // Itens
  const [itens, setItens] = useState([])
  const [novoItem, setNovoItem] = useState({ ...ITEM_VAZIO })

  // Auxiliares
  const [rotas, setRotas] = useState([])
  const [clientes, setClientes] = useState([])
  const [itensPadrao, setItensPadrao] = useState([])
  const [caixas, setCaixas] = useState([])

  // Pagamento modal
  const [modalPagar, setModalPagar] = useState(null)
  const [pgDinheiro, setPgDinheiro] = useState('')
  const [pgPix, setPgPix] = useState('')
  const [pgCartao, setPgCartao] = useState('')
  const [pgCaixa, setPgCaixa] = useState('')

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  const carregar = useCallback(() => {
    if (!viagemAtiva) return
    setLoading(true)
    api.get(`/encomendas?viagem_id=${viagemAtiva.id_viagem}`)
      .then(setEncomendas)
      .catch(() => showToast('Erro ao carregar encomendas', 'error'))
      .finally(() => setLoading(false))
  }, [viagemAtiva])

  useEffect(() => { carregar() }, [carregar])

  useEffect(() => {
    Promise.allSettled([
      api.get('/rotas').then(setRotas),
      api.get('/cadastros/clientes-encomenda').then(setClientes),
      api.get('/cadastros/itens-encomenda').then(setItensPadrao),
      api.get('/cadastros/caixas').then(setCaixas)
    ]).catch(() => {})
  }, [])

  // Calculos
  const totalItens = itens.reduce((s, i) => s + (parseFloat(i.valor_total) || 0), 0)
  const totalVolumes = itens.reduce((s, i) => s + (parseInt(i.quantidade) || 0), 0)

  function limparForm() {
    setRemetente('')
    setDestinatario('')
    setIdRota('')
    setObservacoes('')
    setStatusEntrega('')
    setNomeRecebedor('Pendente de Entrega')
    setDocRecebedor('')
    setItens([])
    setNovoItem({ ...ITEM_VAZIO })
    setSelecionada(null)
    setEditando(false)
  }

  function handleNovo() {
    limparForm()
    setEditando(true)
    setNumEncomenda('')
  }

  function handleSelectRow(enc) {
    setSelecionada(enc)
    setEditando(false)
    setRemetente(enc.remetente || '')
    setDestinatario(enc.destinatario || '')
    setIdRota(enc.id_rota || '')
    setObservacoes(enc.observacoes || '')
    setStatusEntrega(enc.entregue ? 'ENTREGUE' : 'PENDENTE')
    setNomeRecebedor(enc.nome_recebedor || 'Pendente de Entrega')
    setDocRecebedor(enc.doc_recebedor || '')
    setNumEncomenda(enc.numero_encomenda || '')
    // Carregar itens
    api.get(`/encomendas/${enc.id_encomenda}/itens`)
      .then(setItens).catch(() => setItens([]))
  }

  // Item management
  function handleNovoItemChange(field, value) {
    const updated = { ...novoItem, [field]: value }
    if (field === 'quantidade' || field === 'valor_unitario') {
      const q = parseFloat(updated.quantidade) || 0
      const v = parseFloat(updated.valor_unitario) || 0
      updated.valor_total = (q * v).toFixed(2)
    }
    setNovoItem(updated)
  }

  async function handleAdicionarItem() {
    if (!novoItem.descricao.trim()) { showToast('Informe a descricao do item', 'error'); return }

    // Verificar se item existe no catalogo
    const descLower = novoItem.descricao.trim().toLowerCase()
    const itemExiste = itensPadrao.some(ip => (ip.nome_item || '').toLowerCase() === descLower)
    if (!itemExiste && novoItem.descricao.trim()) {
      const salvar = window.confirm(`Item "${novoItem.descricao.trim()}" nao encontrado no catalogo.\n\nDeseja salvar para futuras encomendas?`)
      if (salvar) {
        try {
          await api.post('/cadastros/itens-encomenda', {
            nome_item: novoItem.descricao.trim(),
            preco_padrao: parseFloat(novoItem.valor_unitario) || 0
          })
          // Recarregar lista de itens padrao
          const novosItens = await api.get('/cadastros/itens-encomenda')
          setItensPadrao(novosItens)
          showToast(`Item "${novoItem.descricao.trim()}" salvo no catalogo`)
        } catch { /* silencioso se falhar */ }
      }
    }

    setItens(prev => [...prev, { ...novoItem, valor_total: parseFloat(novoItem.valor_total) || 0, valor_unitario: parseFloat(novoItem.valor_unitario) || 0, quantidade: parseInt(novoItem.quantidade) || 1 }])
    setNovoItem({ ...ITEM_VAZIO })
  }

  // Verificar e salvar cliente novo (remetente ou destinatario)
  async function verificarSalvarCliente(nome) {
    if (!nome || !nome.trim()) return
    const nomeLower = nome.trim().toLowerCase()
    const existe = clientes.some(c => (c.nome_cliente || '').toLowerCase() === nomeLower)
    if (!existe) {
      const salvar = window.confirm(`Cliente "${nome.trim()}" nao encontrado no cadastro.\n\nDeseja salvar para futuras encomendas?`)
      if (salvar) {
        try {
          await api.post('/cadastros/clientes-encomenda', { nome_cliente: nome.trim() })
          const novosClientes = await api.get('/cadastros/clientes-encomenda')
          setClientes(novosClientes)
          showToast(`Cliente "${nome.trim()}" salvo no cadastro`)
        } catch { /* silencioso */ }
      }
    }
  }

  function handleRemoverItem(idx) {
    setItens(prev => prev.filter((_, i) => i !== idx))
  }

  // Selecionar item padrao
  function handleSelectItemPadrao(e) {
    const id = e.target.value
    if (!id) return
    const item = itensPadrao.find(i => String(i.id) === id)
    if (item) {
      setNovoItem(prev => ({
        ...prev,
        descricao: item.nome_item,
        valor_unitario: item.preco_padrao || '',
        valor_total: ((parseInt(prev.quantidade) || 1) * (parseFloat(item.preco_padrao) || 0)).toFixed(2)
      }))
    }
  }

  // FINALIZAR
  async function handleFinalizar() {
    if (!destinatario.trim()) { showToast('Informe o destinatario', 'error'); return }

    // Abrir modal pagamento
    setModalPagar({ totalAPagar: totalItens })
    setPgDinheiro('')
    setPgPix('')
    setPgCartao('')
    setPgCaixa('')
  }

  async function handleConfirmarPagamento() {
    const vDin = parseFloat(pgDinheiro) || 0
    const vPix = parseFloat(pgPix) || 0
    const vCart = parseFloat(pgCartao) || 0
    const totalPago = vDin + vPix + vCart

    setSalvando(true)
    try {
      const rota = rotas.find(r => String(r.id_rota) === idRota)
      const rotaNome = rota ? `${rota.origem} - ${rota.destino}` : ''

      const payload = {
        id_viagem: viagemAtiva.id_viagem,
        remetente: remetente.trim(),
        destinatario: destinatario.trim(),
        rota: rotaNome,
        observacoes: observacoes.trim(),
        total_volumes: totalVolumes,
        total_a_pagar: totalItens,
        valor_pago: totalPago,
        desconto: 0,
        forma_pagamento: [vDin > 0 && 'DINHEIRO', vPix > 0 && 'PIX', vCart > 0 && 'CARTAO'].filter(Boolean).join(', ') || 'DINHEIRO',
        id_caixa: pgCaixa || null,
        itens: itens.map(i => ({
          quantidade: i.quantidade,
          descricao: i.descricao,
          valor_unitario: i.valor_unitario,
          valor_total: i.valor_total,
          local_armazenamento: i.local_armazenamento || null
        }))
      }

      if (selecionada) {
        await api.put(`/encomendas/${selecionada.id_encomenda}`, payload)
        showToast('Encomenda atualizada')
      } else {
        await api.post('/encomendas', payload)
        showToast('Encomenda criada com sucesso')
      }

      setModalPagar(null)
      limparForm()
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao salvar', 'error')
    } finally {
      setSalvando(false)
    }
  }

  // Excluir
  async function handleExcluir() {
    if (!selecionada) { showToast('Selecione uma encomenda', 'error'); return }
    if (!window.confirm(`Excluir encomenda #${selecionada.numero_encomenda}?`)) return
    try {
      await api.delete(`/encomendas/${selecionada.id_encomenda}`)
      showToast('Encomenda excluida')
      limparForm()
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao excluir', 'error')
    }
  }

  // Entregar
  async function handleEntregar() {
    if (!selecionada) { showToast('Selecione uma encomenda', 'error'); return }
    const nome = prompt('Nome do recebedor:')
    if (!nome) return
    const doc = prompt('Documento do recebedor (opcional):') || ''
    try {
      await api.put(`/encomendas/${selecionada.id_encomenda}/entregar`, { nome_recebedor: nome, doc_recebedor: doc })
      showToast('Encomenda marcada como entregue')
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro', 'error')
    }
  }

  if (!viagemAtiva) {
    return (
      <div className="placeholder-page">
        <div className="ph-icon">📦</div>
        <h2>Encomendas</h2>
        <p>Selecione uma viagem para comecar.</p>
      </div>
    )
  }

  const I = { padding: '7px 10px', fontSize: '0.82rem', background: 'var(--bg-soft)', border: '1px solid var(--border)', borderRadius: 4, color: 'var(--text)', fontFamily: 'Sora, sans-serif', width: '100%', boxSizing: 'border-box' }
  const L = { fontSize: '0.72rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: 3, display: 'block' }
  const RO = { ...I, opacity: 0.6, cursor: 'default' }

  const vData = formatDate(viagemAtiva.data_viagem)
  const vCheg = formatDate(viagemAtiva.data_chegada)

  // Pagamento calculos
  const pgTotal = (parseFloat(pgDinheiro) || 0) + (parseFloat(pgPix) || 0) + (parseFloat(pgCartao) || 0)
  const pgRestante = (modalPagar?.totalAPagar || 0) - pgTotal

  return (
    <div className="card" style={{ padding: 12 }}>
      {/* HEADER */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12, gap: 12 }}>
        <div style={{ display: 'flex', gap: 8 }}>
          <button className="btn-primary" onClick={handleNovo} style={{ width: 'auto', padding: '8px 20px' }}>Nova (F2)</button>
          <button className="btn-primary" onClick={() => onNavigate && onNavigate('listar-encomendas')} style={{ width: 'auto', padding: '8px 20px', background: 'var(--primary-light)' }}>Ver Lista</button>
        </div>
        <h2 style={{ fontSize: '1.05rem', margin: 0, flex: 1, textAlign: 'center' }}>REGISTRAR NOVA ENCOMENDA</h2>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <span style={L}>N° Encomenda:</span>
          <input style={{ ...RO, width: 60, textAlign: 'center', fontWeight: 700, fontSize: '1rem' }} value={numEncomenda || '—'} readOnly />
        </div>
      </div>

      {/* 2 PAINEIS */}
      <div style={{ display: 'flex', gap: 16, marginBottom: 12 }}>

        {/* PAINEL ESQUERDO — Remetente/Destinatario/Viagem */}
        <div style={{ width: 420, flexShrink: 0 }}>
          <div style={{ marginBottom: 8 }}>
            <label style={L}>Remetente:</label>
            <select style={I} value={remetente} onChange={e => setRemetente(e.target.value)}>
              <option value=""></option>
              {clientes.map(c => <option key={c.id_cliente} value={c.nome_cliente}>{c.nome_cliente}</option>)}
            </select>
            <input style={{ ...I, marginTop: 4 }} placeholder="Ou digite o nome..." value={remetente} onChange={e => setRemetente(e.target.value)} onBlur={() => verificarSalvarCliente(remetente)} />
          </div>
          <div style={{ marginBottom: 8 }}>
            <label style={L}>Destinatario:</label>
            <select style={I} value={destinatario} onChange={e => setDestinatario(e.target.value)}>
              <option value=""></option>
              {clientes.map(c => <option key={c.id_cliente} value={c.nome_cliente}>{c.nome_cliente}</option>)}
            </select>
            <input style={{ ...I, marginTop: 4 }} placeholder="Ou digite o nome..." value={destinatario} onChange={e => setDestinatario(e.target.value)} onBlur={() => verificarSalvarCliente(destinatario)} />
          </div>
          <div style={{ marginBottom: 8 }}>
            <label style={L}>Rota:</label>
            <select style={I} value={idRota} onChange={e => setIdRota(e.target.value)}>
              <option value=""></option>
              {rotas.map(r => <option key={r.id_rota} value={r.id_rota}>{r.origem} - {r.destino}</option>)}
            </select>
          </div>
          <div style={{ marginBottom: 8 }}>
            <label style={L}>Viagem Atual (Automatico):</label>
            <input style={{ ...RO, fontWeight: 600 }} value={`${viagemAtiva.id_viagem} - ${vData} ate ${vCheg}`} readOnly />
          </div>
          <div style={{ marginBottom: 8 }}>
            <label style={L}>Observacoes:</label>
            <input style={I} placeholder="Ex: Caixa Fragil" value={observacoes} onChange={e => setObservacoes(e.target.value)} />
          </div>

          <hr style={{ border: 'none', borderTop: '1px solid var(--border)', margin: '12px 0' }} />

          <div style={{ padding: 10, border: '1px solid var(--border)', borderRadius: 6 }}>
            <div style={{ marginBottom: 6 }}>
              <label style={{ ...L, fontWeight: 700 }}>Status da Entrega:</label>
              <input style={RO} value={selecionada ? (selecionada.entregue ? 'ENTREGUE' : 'PENDENTE') : ''} readOnly />
            </div>
            <div style={{ marginBottom: 6 }}>
              <label style={L}>Nome do Recebedor:</label>
              <input style={RO} value={nomeRecebedor} readOnly />
            </div>
            <div>
              <label style={L}>RG / CPF / Documento:</label>
              <input style={RO} value={docRecebedor} readOnly />
            </div>
          </div>
        </div>

        {/* PAINEL DIREITO — Itens */}
        <div style={{ flex: 1, minWidth: 0 }}>
          {/* Entrada de item */}
          <div style={{ display: 'grid', gridTemplateColumns: '60px 1fr 100px 100px auto', gap: 8, padding: 10, background: 'rgba(5,150,105,0.05)', borderRadius: 6, border: '1px solid var(--primary)', marginBottom: 8, alignItems: 'end' }}>
            <div>
              <label style={{ ...L, fontSize: '0.65rem' }}>Qtd</label>
              <input style={{ ...I, textAlign: 'center' }} type="number" min="1" value={novoItem.quantidade} onChange={e => handleNovoItemChange('quantidade', e.target.value)} />
            </div>
            <div>
              <label style={{ ...L, fontSize: '0.65rem' }}>Descricao do Item (Enter busca)</label>
              <div style={{ display: 'flex', gap: 4 }}>
                <input style={{ ...I, flex: 1 }} value={novoItem.descricao} onChange={e => handleNovoItemChange('descricao', e.target.value)} placeholder="Digite ou selecione..." />
                <select style={{ ...I, width: 'auto', maxWidth: 40 }} onChange={handleSelectItemPadrao} value="">
                  <option value="">▼</option>
                  {itensPadrao.map(ip => <option key={ip.id} value={ip.id}>{ip.nome_item}</option>)}
                </select>
              </div>
            </div>
            <div>
              <label style={{ ...L, fontSize: '0.65rem' }}>V. Unit.</label>
              <input style={{ ...I, textAlign: 'right' }} type="number" step="0.01" min="0" value={novoItem.valor_unitario} onChange={e => handleNovoItemChange('valor_unitario', e.target.value)} />
            </div>
            <div>
              <label style={{ ...L, fontSize: '0.65rem' }}>Total</label>
              <input style={{ ...RO, textAlign: 'right' }} value={novoItem.valor_total || '0.00'} readOnly />
            </div>
            <button onClick={handleAdicionarItem} style={{ padding: '8px 16px', background: '#F59E0B', color: '#fff', border: 'none', borderRadius: 4, fontWeight: 700, cursor: 'pointer', fontSize: '0.82rem', whiteSpace: 'nowrap' }}>Adicionar</button>
          </div>

          {/* Tabela de itens */}
          <div className="table-container" style={{ maxHeight: 300, overflowY: 'auto' }}>
            <table>
              <thead>
                <tr>
                  <th style={{ width: 60 }}>Quant.</th>
                  <th>Descricao</th>
                  <th style={{ width: 90 }}>V. Unit.</th>
                  <th style={{ width: 90 }}>V. Total</th>
                  <th style={{ width: 100 }}>Local</th>
                  <th style={{ width: 40 }}></th>
                </tr>
              </thead>
              <tbody>
                {itens.length === 0 ? (
                  <tr><td colSpan="6" style={{ textAlign: 'center', padding: 30, color: 'var(--text-muted)' }}>Nao ha conteudo na tabela</td></tr>
                ) : itens.map((item, idx) => (
                  <tr key={idx}>
                    <td style={{ textAlign: 'center' }}>{item.quantidade}</td>
                    <td>{item.descricao}</td>
                    <td className="money">{formatMoney(item.valor_unitario)}</td>
                    <td className="money">{formatMoney(item.valor_total)}</td>
                    <td>{item.local_armazenamento || '—'}</td>
                    <td><button className="btn-sm danger" onClick={() => handleRemoverItem(idx)} style={{ padding: '2px 6px' }}>×</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* TOTAIS + BOTOES */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '10px 0', borderTop: '1px solid var(--border)' }}>
        <div style={{ display: 'flex', gap: 16, alignItems: 'center' }}>
          <span style={{ fontSize: '0.85rem' }}>Total Volumes: <strong>{totalVolumes}</strong></span>
          <span style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--primary)', fontFamily: 'Space Mono, monospace' }}>
            TOTAL A PAGAR: {formatMoney(totalItens)}
          </span>
        </div>
        <div style={{ display: 'flex', gap: 6 }}>
          <button className="btn-sm primary" onClick={handleFinalizar} disabled={!editando || salvando} style={{ padding: '8px 16px', fontWeight: 700 }}>F3 - FINALIZAR</button>
          <button className="btn-sm primary" onClick={() => { if (selecionada) { setEditando(true) } }}>Editar</button>
          <button className="btn-sm danger" onClick={handleExcluir}>Excluir</button>
          <button className="btn-sm primary" onClick={() => { if (selecionada) printReciboEncomenda(selecionada, viagemAtiva) }}>Imprimir</button>
          <button className="btn-sm primary" onClick={handleEntregar} style={{ background: '#0369A1' }}>Entregar</button>
          <button className="btn-sm" onClick={() => limparForm()}>Sair (Esc)</button>
        </div>
      </div>

      {/* Lista de encomendas abaixo */}
      <div className="table-container" style={{ marginTop: 8 }}>
        <table>
          <thead>
            <tr>
              <th style={{ width: 50 }}>N°</th>
              <th>Remetente</th>
              <th>Destinatario</th>
              <th>Rota</th>
              <th>Total</th>
              <th>Pago</th>
              <th>Status</th>
              <th>Entrega</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="8">Carregando...</td></tr>
            ) : encomendas.length === 0 ? (
              <tr><td colSpan="8" style={{ textAlign: 'center', padding: 20, color: 'var(--text-muted)' }}>Nenhuma encomenda nesta viagem</td></tr>
            ) : encomendas.map(e => (
              <tr key={e.id_encomenda}
                  className={`clickable ${selecionada?.id_encomenda === e.id_encomenda ? 'selected' : ''}`}
                  onClick={() => handleSelectRow(e)}>
                <td>{e.numero_encomenda}</td>
                <td>{e.remetente || '—'}</td>
                <td>{e.destinatario || '—'}</td>
                <td>{e.rota || '—'}</td>
                <td className="money">{formatMoney(e.total_a_pagar)}</td>
                <td className="money">{formatMoney(e.valor_pago)}</td>
                <td><span className={`badge ${e.status_pagamento === 'PAGO' ? 'success' : 'warning'}`}>{e.status_pagamento || 'PENDENTE'}</span></td>
                <td><span className={`badge ${e.entregue ? 'success' : 'warning'}`}>{e.entregue ? 'Entregue' : 'Pendente'}</span></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}

      {/* MODAL PAGAMENTO */}
      {modalPagar && (
        <div className="modal-overlay" onClick={() => setModalPagar(null)}>
          <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 480 }}>
            <h3>Registrar Pagamento</h3>
            <p style={{ color: 'var(--text-muted)', marginBottom: 8 }}>
              Total a Pagar: <strong style={{ color: 'var(--primary)', fontSize: '1.1rem' }}>{formatMoney(modalPagar.totalAPagar)}</strong>
            </p>

            <div className="form-grid">
              <div className="form-group">
                <label>Dinheiro (R$)</label>
                <input type="number" step="0.01" min="0" value={pgDinheiro} onChange={e => setPgDinheiro(e.target.value)} autoFocus />
              </div>
              <div className="form-group">
                <label>PIX (R$)</label>
                <input type="number" step="0.01" min="0" value={pgPix} onChange={e => setPgPix(e.target.value)} />
              </div>
              <div className="form-group">
                <label>Cartao (R$)</label>
                <input type="number" step="0.01" min="0" value={pgCartao} onChange={e => setPgCartao(e.target.value)} />
              </div>
              <div className="form-group">
                <label>Caixa</label>
                <select value={pgCaixa} onChange={e => setPgCaixa(e.target.value)}>
                  <option value="">Selecione...</option>
                  {caixas.map(c => <option key={c.id_caixa} value={c.id_caixa}>{c.nome_caixa}</option>)}
                </select>
              </div>
              <div className="form-group">
                <label>Total Recebido</label>
                <input value={formatMoney(pgTotal)} readOnly style={{ opacity: 0.6 }} />
              </div>
              <div className="form-group">
                <label>Restante</label>
                <input value={formatMoney(Math.max(0, pgRestante))} readOnly style={{ opacity: 0.6, color: pgRestante > 0 ? 'var(--danger)' : undefined }} />
              </div>
            </div>

            <div className="modal-actions">
              <button className="btn-secondary" onClick={() => setModalPagar(null)}>Cancelar</button>
              <button className="btn-primary" onClick={handleConfirmarPagamento} disabled={salvando}>
                {salvando ? 'Salvando...' : 'Confirmar'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

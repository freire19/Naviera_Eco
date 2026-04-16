import { useState, useEffect, useCallback, useRef } from 'react'
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

  // Dropdown itens customizado
  const [showItemList, setShowItemList] = useState(false)
  const itemDropdownRef = useRef(null)

  // Fechar dropdown ao clicar fora
  useEffect(() => {
    if (!showItemList) return
    const handler = (e) => {
      if (itemDropdownRef.current && !itemDropdownRef.current.contains(e.target)) {
        setShowItemList(false)
      }
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [showItemList])

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

  // Verificar se veio encomenda para editar (duplo-clique da lista)
  useEffect(() => {
    const raw = sessionStorage.getItem('encomenda_editar')
    if (raw) {
      sessionStorage.removeItem('encomenda_editar')
      try {
        const enc = JSON.parse(raw)
        handleSelectRow(enc)
        setEditando(true)
      } catch {}
    }
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

  async function handleNovo() {
    limparForm()
    setEditando(true)
    // Buscar proximo numero automatico
    try {
      const res = await api.get('/encomendas/proximo-numero')
      setNumEncomenda(res.numero || '1')
    } catch {
      setNumEncomenda('—')
    }
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

  // Monta payload da encomenda
  function buildPayload(totalPago = 0, formaPgto = '', idCaixa = null) {
    const rota = rotas.find(r => String(r.id_rota) === idRota)
    const rotaNome = rota ? `${rota.origem} - ${rota.destino}` : ''
    return {
      id_viagem: viagemAtiva.id_viagem,
      remetente: remetente.trim(),
      destinatario: destinatario.trim(),
      rota: rotaNome,
      observacoes: observacoes.trim(),
      total_volumes: totalVolumes,
      total_a_pagar: totalItens || 0,
      valor_pago: totalPago,
      desconto: 0,
      forma_pagamento: formaPgto || null,
      id_caixa: idCaixa || null,
      itens: itens.map(i => ({
        quantidade: i.quantidade,
        descricao: i.descricao,
        valor_unitario: i.valor_unitario,
        valor_total: i.valor_total,
        local_armazenamento: i.local_armazenamento || null
      }))
    }
  }

  // FINALIZAR — pergunta se quer registrar pagamento agora ou salvar como pendente
  async function handleFinalizar() {
    if (!destinatario.trim()) { showToast('Informe o destinatario', 'error'); return }

    if (totalItens > 0) {
      const pagarAgora = window.confirm(`Total: ${formatMoney(totalItens)}\n\nDeseja registrar o pagamento agora?\n\n[OK] = Registrar pagamento\n[Cancelar] = Salvar como PENDENTE`)
      if (pagarAgora) {
        setModalPagar({ totalAPagar: totalItens })
        setPgDinheiro('')
        setPgPix('')
        setPgCartao('')
        setPgCaixa('')
        return
      }
    }

    // Salvar direto como pendente (sem pagamento)
    await salvarEncomenda(0, '', null)
  }

  async function salvarEncomenda(totalPago, formaPgto, idCaixa) {
    setSalvando(true)
    try {
      const payload = buildPayload(totalPago, formaPgto, idCaixa)
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

  // Confirmar pagamento do modal
  async function handleConfirmarPagamento() {
    const vDin = parseFloat(pgDinheiro) || 0
    const vPix = parseFloat(pgPix) || 0
    const vCart = parseFloat(pgCartao) || 0
    const totalPago = vDin + vPix + vCart
    const formaPgto = [vDin > 0 && 'DINHEIRO', vPix > 0 && 'PIX', vCart > 0 && 'CARTAO'].filter(Boolean).join(', ') || ''
    setModalPagar(null)
    await salvarEncomenda(totalPago, formaPgto, pgCaixa || null)
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

  // Atalho ESC para limpar/sair
  useEffect(() => {
    const handler = (e) => {
      if (e.key === 'Escape') {
        if (modalPagar) { setModalPagar(null); return }
        limparForm()
      }
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [modalPagar])

  const I = { padding: '7px 10px', fontSize: '0.82rem', background: 'var(--bg-soft)', border: '1px solid var(--border)', borderRadius: 4, color: 'var(--text)', fontFamily: 'Sora, sans-serif', width: '100%', boxSizing: 'border-box' }
  const L = { fontSize: '0.72rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: 3, display: 'block' }
  const RO = { ...I, opacity: 0.6, cursor: 'default' }

  const vData = formatDate(viagemAtiva.data_viagem)
  const vCheg = formatDate(viagemAtiva.data_chegada)

  // Info de pagamento da encomenda selecionada
  const selValor = parseFloat(selecionada?.total_a_pagar) || 0
  const selPago = parseFloat(selecionada?.valor_pago) || 0
  const selDevedor = Math.max(0, selValor - (parseFloat(selecionada?.desconto) || 0) - selPago)
  const selStatus = selecionada ? (selDevedor <= 0.01 ? 'PAGO' : 'FALTA PAGAR') : ''
  const selEntrega = selecionada ? (selecionada.entregue ? 'ENTREGUE' : 'PENDENTE') : ''

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
          <div style={{ display: 'grid', gridTemplateColumns: '60px 1fr 90px 90px auto', gap: 10, padding: 10, background: 'rgba(5,150,105,0.05)', borderRadius: 6, border: '1px solid var(--primary)', marginBottom: 8, alignItems: 'end' }}>
            <div>
              <label style={{ ...L, fontSize: '0.65rem' }}>Qtd</label>
              <input style={{ ...I, textAlign: 'center' }} type="number" min="1" value={novoItem.quantidade} onChange={e => handleNovoItemChange('quantidade', e.target.value)} />
            </div>
            <div style={{ position: 'relative' }} ref={itemDropdownRef}>
              <label style={{ ...L, fontSize: '0.65rem' }}>Descricao do Item (Enter busca)</label>
              <input style={I} value={novoItem.descricao}
                onChange={e => {
                  handleNovoItemChange('descricao', e.target.value)
                  setShowItemList(true)
                }}
                onFocus={() => setShowItemList(true)}
                placeholder="Digite ou selecione um item..." />
              {showItemList && (() => {
                const q = (novoItem.descricao || '').toLowerCase()
                const filtered = itensPadrao.filter(ip => !q || (ip.nome_item || '').toLowerCase().includes(q))
                if (filtered.length === 0) return null
                return (
                  <div style={{
                    position: 'absolute', top: '100%', left: 0, right: 0, zIndex: 50,
                    background: 'var(--bg-card)', border: '1px solid var(--border)', borderRadius: 4,
                    maxHeight: 220, overflowY: 'auto', boxShadow: 'var(--shadow-lg)'
                  }}>
                    {filtered.map((ip, idx) => {
                      const preco = Number(ip.preco_padrao || ip.preco_unitario_padrao || 0)
                      return (
                        <div key={ip.id || ip.id_item_encomenda || idx}
                          onMouseDown={() => {
                            setNovoItem(prev => ({
                              ...prev,
                              descricao: ip.nome_item,
                              valor_unitario: preco,
                              valor_total: ((parseInt(prev.quantidade) || 1) * preco).toFixed(2)
                            }))
                            setShowItemList(false)
                          }}
                          style={{
                            display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                            padding: '8px 12px', cursor: 'pointer', fontSize: '0.85rem',
                            background: idx % 2 === 0 ? 'transparent' : 'var(--bg-soft)',
                            borderBottom: '1px solid var(--border)'
                          }}
                          onMouseEnter={e => { e.currentTarget.style.background = 'var(--primary)'; e.currentTarget.style.color = '#fff' }}
                          onMouseLeave={e => { e.currentTarget.style.background = idx % 2 === 0 ? 'transparent' : 'var(--bg-soft)'; e.currentTarget.style.color = '' }}
                        >
                          <span style={{ fontWeight: 600 }}>{ip.nome_item}</span>
                          <span style={{ fontFamily: 'Space Mono, monospace', fontSize: '0.82rem' }}>R$ {preco.toFixed(2)}</span>
                        </div>
                      )
                    })}
                  </div>
                )
              })()}
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
                    <td style={{ textAlign: 'center' }}>
                      <input type="number" min="1" value={item.quantidade} style={{ width: 40, textAlign: 'center', background: 'transparent', border: '1px solid transparent', color: 'inherit', fontSize: 'inherit', fontFamily: 'inherit', padding: '2px' }}
                        onFocus={e => e.target.style.borderColor = 'var(--primary)'}
                        onBlur={e => e.target.style.borderColor = 'transparent'}
                        onChange={e => { const v = parseInt(e.target.value) || 1; setItens(prev => prev.map((it, i) => i === idx ? { ...it, quantidade: v, valor_total: (v * (parseFloat(it.valor_unitario) || 0)).toFixed(2) } : it)) }} />
                    </td>
                    <td>
                      <input value={item.descricao} style={{ width: '100%', background: 'transparent', border: '1px solid transparent', color: 'inherit', fontSize: 'inherit', fontFamily: 'inherit', padding: '2px' }}
                        onFocus={e => e.target.style.borderColor = 'var(--primary)'}
                        onBlur={e => e.target.style.borderColor = 'transparent'}
                        onChange={e => setItens(prev => prev.map((it, i) => i === idx ? { ...it, descricao: e.target.value } : it))} />
                    </td>
                    <td>
                      <input type="number" step="0.01" value={item.valor_unitario} style={{ width: 70, textAlign: 'right', background: 'transparent', border: '1px solid transparent', color: 'inherit', fontSize: 'inherit', fontFamily: 'Space Mono, monospace', padding: '2px' }}
                        onFocus={e => e.target.style.borderColor = 'var(--primary)'}
                        onBlur={e => e.target.style.borderColor = 'transparent'}
                        onChange={e => { const v = parseFloat(e.target.value) || 0; setItens(prev => prev.map((it, i) => i === idx ? { ...it, valor_unitario: v, valor_total: ((parseInt(it.quantidade) || 1) * v).toFixed(2) } : it)) }} />
                    </td>
                    <td className="money">{formatMoney(item.valor_total)}</td>
                    <td>
                      <input value={item.local_armazenamento || ''} style={{ width: '100%', background: 'transparent', border: '1px solid transparent', color: 'inherit', fontSize: 'inherit', fontFamily: 'inherit', padding: '2px' }}
                        onFocus={e => e.target.style.borderColor = 'var(--primary)'}
                        onBlur={e => e.target.style.borderColor = 'transparent'}
                        onChange={e => setItens(prev => prev.map((it, i) => i === idx ? { ...it, local_armazenamento: e.target.value } : it))} />
                    </td>
                    <td><button className="btn-sm danger" onClick={() => handleRemoverItem(idx)} style={{ padding: '2px 6px' }}>×</button></td>
                  </tr>
                ))}
              </tbody>
              {itens.length > 0 && (
              <tfoot>
                <tr style={{ borderTop: '2px solid var(--primary)' }}>
                  <td style={{ fontWeight: 700, color: 'var(--primary)' }}>{totalVolumes}</td>
                  <td style={{ fontWeight: 700, color: 'var(--primary)' }}>TOTAL VOLUMES: {totalVolumes}</td>
                  <td></td>
                  <td style={{ fontWeight: 700, fontSize: '1rem', color: 'var(--primary)', fontFamily: 'Space Mono, monospace' }}>{formatMoney(totalItens)}</td>
                  <td></td>
                  <td></td>
                </tr>
              </tfoot>
              )}
            </table>
          </div>
        </div>
      </div>

      {/* BANNER STATUS ao editar encomenda existente */}
      {selecionada && (
        <div style={{
          display: 'flex', gap: 16, alignItems: 'center', padding: '8px 12px', marginBottom: 8,
          borderRadius: 6, border: `2px solid ${selDevedor <= 0.01 ? '#059669' : '#DC2626'}`,
          background: selDevedor <= 0.01 ? 'rgba(5,150,105,0.08)' : 'rgba(220,38,38,0.08)'
        }}>
          <span style={{ fontWeight: 700, fontSize: '0.9rem' }}>Encomenda #{selecionada.numero_encomenda}</span>
          <span>Total: <strong>{formatMoney(selValor)}</strong></span>
          <span>Pago: <strong style={{ color: '#059669' }}>{formatMoney(selPago)}</strong></span>
          {selDevedor > 0 && <span>Falta: <strong style={{ color: '#DC2626' }}>{formatMoney(selDevedor)}</strong></span>}
          <span style={{ fontWeight: 700, fontSize: '0.85rem', padding: '2px 10px', borderRadius: 4, color: '#fff', background: selDevedor <= 0.01 ? '#059669' : '#DC2626' }}>
            {selStatus}
          </span>
          <span style={{ fontWeight: 700, fontSize: '0.85rem', padding: '2px 10px', borderRadius: 4, color: '#fff', background: selecionada.entregue ? '#059669' : '#F59E0B' }}>
            {selEntrega}
          </span>
        </div>
      )}

      {/* BOTOES */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', padding: '10px 0', borderTop: '1px solid var(--border)', gap: 6 }}>
        <div style={{ display: 'flex', gap: 6 }}>
          <button className="btn-sm primary" onClick={handleFinalizar} disabled={!editando || salvando} style={{ padding: '8px 16px', fontWeight: 700 }}>F3 - FINALIZAR</button>
          <button className="btn-sm primary" onClick={() => { if (selecionada) { setEditando(true) } }}>Editar</button>
          <button className="btn-sm danger" onClick={handleExcluir}>Excluir</button>
          <button className="btn-sm primary" onClick={() => { if (selecionada) printReciboEncomenda(selecionada, viagemAtiva) }}>Imprimir</button>
          <button className="btn-sm primary" onClick={handleEntregar} style={{ background: '#0369A1' }}>Entregar</button>
          <button className="btn-sm" onClick={() => limparForm()}>Sair (Esc)</button>
        </div>
      </div>

      {/* Lista removida — acesse via "Ver Lista" no menu lateral */}
      <div style={{ display: 'none' }}>
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

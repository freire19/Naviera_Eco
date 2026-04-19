import { useState, useEffect, useCallback, useRef } from 'react'
import { api } from '../api.js'
import { printNotaFrete, printEtiquetaFrete } from '../utils/print.js'
import Autocomplete from '../components/Autocomplete.jsx'
import MoneyInput from '../components/MoneyInput.jsx'

function filtrarContatos(contatos, termo) {
  const t = (termo || '').trim().toLowerCase()
  if (!t) return []
  return contatos.filter(c => (c.nome_razao_social || '').toLowerCase().includes(t)).slice(0, 10)
}

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}
function formatDate(val) {
  if (!val) return '—'
  const s = String(val)
  if (/^\d{2}\/\d{2}\/\d{4}$/.test(s)) return s
  try { const p = (s.includes('T') ? s.substring(0,10) : s).split('-'); return p.length === 3 ? `${p[2]}/${p[1]}/${p[0]}` : s } catch { return s }
}

const ITEM_VAZIO = { quantidade: 1, descricao: '', valor_unitario: '', subtotal: '' }

export default function Fretes({ viagemAtiva, onNavigate, onClose }) {
  const [selecionado, setSelecionado] = useState(null)
  const [editando, setEditando] = useState(false)
  const [salvando, setSalvando] = useState(false)
  const [toast, setToast] = useState(null)
  const [numFrete, setNumFrete] = useState('')

  // Form
  const [remetente, setRemetente] = useState('')
  const [destinatario, setDestinatario] = useState('')
  const [idRota, setIdRota] = useState('')
  const [localTransporte, setLocalTransporte] = useState('')
  const [conferente, setConferente] = useState('')
  const [cidadeCobranca, setCidadeCobranca] = useState('')
  const [notaFiscal, setNotaFiscal] = useState(false)
  const [numNota, setNumNota] = useState('')
  const [valorNota, setValorNota] = useState('')
  const [pesoNota, setPesoNota] = useState('')
  const [observacoes, setObservacoes] = useState('')
  const [precoTipo, setPrecoTipo] = useState('normal')

  // Itens
  const [itens, setItens] = useState([])
  const [novoItem, setNovoItem] = useState({ ...ITEM_VAZIO })
  const [showItemList, setShowItemList] = useState(false)
  const itemDropdownRef = useRef(null)

  // Auxiliares
  const [rotas, setRotas] = useState([])
  const [conferentes, setConferentes] = useState([])
  const [itensPadrao, setItensPadrao] = useState([])
  const [caixas, setCaixas] = useState([])
  const [contatos, setContatos] = useState([])

  // Modal novo contato (cliente frete)
  const [modalNovoContato, setModalNovoContato] = useState(null)
  const [ncRazaoSocial, setNcRazaoSocial] = useState('')
  const [ncCpfCnpj, setNcCpfCnpj] = useState('')
  const [ncEndereco, setNcEndereco] = useState('')
  const [ncInscricao, setNcInscricao] = useState('')
  const [ncEmail, setNcEmail] = useState('')
  const [ncTelefone, setNcTelefone] = useState('')

  // Modal salvar item novo
  const [modalNovoItem, setModalNovoItem] = useState(null)
  const [novoItemPrecoNormal, setNovoItemPrecoNormal] = useState('')
  const [novoItemPrecoDesc, setNovoItemPrecoDesc] = useState('')

  // Pagamento modal
  const [modalPagar, setModalPagar] = useState(null)
  const [pgDesconto, setPgDesconto] = useState('')
  const [pgValorPago, setPgValorPago] = useState('')
  const [pgTipo, setPgTipo] = useState('Dinheiro')
  const [pgCaixa, setPgCaixa] = useState('')

  function showToast(msg, type = 'success') {
    setToast({ msg, type }); setTimeout(() => setToast(null), 3500)
  }

  useEffect(() => {
    Promise.allSettled([
      api.get('/rotas').then(setRotas),
      api.get('/cadastros/conferentes').then(setConferentes),
      api.get('/cadastros/itens-frete').then(setItensPadrao),
      api.get('/cadastros/caixas').then(setCaixas),
      api.get('/fretes/contatos').then(setContatos)
    ]).catch(() => {})
    // Verificar se veio frete para editar (duplo-clique da lista)
    const raw = sessionStorage.getItem('frete_editar')
    if (raw) {
      sessionStorage.removeItem('frete_editar')
      try {
        const f = JSON.parse(raw)
        setSelecionado(f)
        setEditando(true)
        setNumFrete(f.numero_frete || '')
        setRemetente(f.remetente || f.remetente_nome_temp || '')
        setDestinatario(f.destinatario || f.destinatario_nome_temp || '')
        setObservacoes(f.observacoes || '')
        setConferente(f.conferente || f.conferente_temp || '')
        setLocalTransporte(f.local_transporte || '')
        setCidadeCobranca(f.cidade_cobranca || '')
        // Nota fiscal
        if (f.num_notafiscal) {
          setNotaFiscal(true)
          setNumNota(f.num_notafiscal || '')
          setValorNota(f.valor_notafiscal || '')
          setPesoNota(f.peso_notafiscal || '')
        }
        // Carregar itens
        api.get(`/fretes/${f.id_frete}/itens`).then(data => {
          if (Array.isArray(data)) {
            setItens(data.map(i => ({
              quantidade: i.quantidade || 1,
              descricao: i.nome_item_ou_id_produto || '',
              valor_unitario: (parseFloat(i.preco_unitario) || 0).toFixed(2),
              subtotal: (parseFloat(i.subtotal_item) || 0).toFixed(2)
            })))
          }
        }).catch(() => {})
      } catch {}
    } else {
      // Ja inicia pronto para lancar (sem precisar clicar Novo)
      setEditando(true)
      api.get('/fretes/proximo-numero').then(r => setNumFrete(r.numero || '1')).catch(() => setNumFrete('—'))
    }
  }, [])

  // Resolver idRota quando rotas carregam e ha frete selecionado com rota_temp
  useEffect(() => {
    if (!rotas.length || !selecionado) return
    const rotaTexto = (selecionado.rota_temp || selecionado.rota || '').toLowerCase().trim()
    if (!rotaTexto || idRota) return
    const match = rotas.find(r => `${r.origem} - ${r.destino}`.toLowerCase() === rotaTexto)
    if (match) setIdRota(String(match.id_rota))
  }, [rotas, selecionado])

  // Fechar dropdowns ao clicar fora (remetente/destinatario agora usam Autocomplete com fechamento proprio)
  useEffect(() => {
    const handler = (e) => {
      if (showItemList && itemDropdownRef.current && !itemDropdownRef.current.contains(e.target)) setShowItemList(false)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [showItemList])

  // ESC
  useEffect(() => {
    const handler = (e) => { if (e.key === 'Escape') { if (modalPagar) setModalPagar(null); else limparForm() } }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [modalPagar])

  // Calculos
  const totalItens = itens.reduce((s, i) => s + (parseFloat(i.subtotal) || 0), 0)
  const totalVolumes = itens.reduce((s, i) => s + (parseInt(i.quantidade) || 0), 0)

  function limparForm() {
    setRemetente(''); setDestinatario(''); setIdRota(''); setLocalTransporte('')
    setConferente(''); setCidadeCobranca(''); setNotaFiscal(false); setNumNota('')
    setValorNota(''); setPesoNota(''); setObservacoes(''); setPrecoTipo('normal')
    setItens([]); setNovoItem({ ...ITEM_VAZIO }); setSelecionado(null); setEditando(false)
  }

  async function handleNovo() {
    limparForm()
    setEditando(true)
    try { const res = await api.get('/fretes/proximo-numero'); setNumFrete(res.numero || '1') } catch { setNumFrete('—') }
  }

  function handleNovoItemChange(field, value) {
    const updated = { ...novoItem, [field]: value }
    if (field === 'quantidade' || field === 'valor_unitario') {
      updated.subtotal = ((parseFloat(updated.quantidade) || 0) * (parseFloat(updated.valor_unitario) || 0)).toFixed(2)
    }
    setNovoItem(updated)
  }

  // Verificar e salvar contato novo (tabela contatos, separada de clientes encomenda)
  function verificarSalvarContato(nome) {
    if (!nome || !nome.trim()) return
    const nomeLower = nome.trim().toLowerCase()
    const existe = contatos.some(c => (c.nome_razao_social || '').toLowerCase() === nomeLower)
    if (!existe) {
      setModalNovoContato(nome.trim().toUpperCase())
      setNcRazaoSocial(''); setNcCpfCnpj(''); setNcEndereco(''); setNcInscricao(''); setNcEmail(''); setNcTelefone('')
    }
  }

  async function salvarNovoContato() {
    if (!modalNovoContato) return
    try {
      await api.post('/fretes/contatos', {
        nome: modalNovoContato,
        razao_social: ncRazaoSocial || null,
        cpf_cnpj: ncCpfCnpj || null,
        endereco: ncEndereco || null,
        inscricao_estadual: ncInscricao || null,
        email: ncEmail || null,
        telefone: ncTelefone || null
      })
      const novos = await api.get('/fretes/contatos')
      setContatos(novos)
      showToast(`Cliente "${modalNovoContato}" cadastrado`)
      setModalNovoContato(null)
    } catch (err) {
      showToast(err.message || 'Erro ao salvar', 'error')
    }
  }

  async function handleAdicionarItem() {
    if (!novoItem.descricao.trim()) { showToast('Informe o item', 'error'); return }

    // Verificar se item existe no catalogo
    const descLower = novoItem.descricao.trim().toLowerCase()
    const itemExiste = itensPadrao.some(ip => (ip.nome_item || '').toLowerCase() === descLower)

    // Adicionar item na lista
    setItens(prev => [...prev, { ...novoItem, subtotal: parseFloat(novoItem.subtotal) || 0, valor_unitario: parseFloat(novoItem.valor_unitario) || 0, quantidade: parseInt(novoItem.quantidade) || 1 }])

    // Se nao existe, abrir modal para salvar com precos
    if (!itemExiste) {
      setModalNovoItem({ nome: novoItem.descricao.trim().toUpperCase() })
      setNovoItemPrecoNormal(String(novoItem.valor_unitario || ''))
      setNovoItemPrecoDesc('')
    }

    setNovoItem({ ...ITEM_VAZIO })
  }

  async function handleSalvarNovoItem() {
    if (!modalNovoItem) return
    try {
      await api.post('/cadastros/itens-frete', {
        nome_item: modalNovoItem.nome,
        preco_padrao: parseFloat(novoItemPrecoNormal) || 0,
        preco_desconto: parseFloat(novoItemPrecoDesc) || 0
      })
      const novos = await api.get('/cadastros/itens-frete')
      setItensPadrao(novos)
      showToast(`Item "${modalNovoItem.nome}" salvo no catalogo`)
    } catch {}
    setModalNovoItem(null)
  }

  // Quando trocar Normal/Desc, atualizar precos de todos os itens ja adicionados
  function atualizarPrecosItens(tipo) {
    setItens(prev => prev.map(item => {
      const catalogo = itensPadrao.find(ip => (ip.nome_item || '').toLowerCase() === (item.descricao || '').toLowerCase())
      if (!catalogo) return item
      const novoPreco = tipo === 'normal'
        ? Number(catalogo.preco_padrao || catalogo.preco_unitario_padrao || 0)
        : Number(catalogo.preco_unitario_desconto || catalogo.preco_padrao || catalogo.preco_unitario_padrao || 0)
      return { ...item, valor_unitario: novoPreco, subtotal: ((parseInt(item.quantidade) || 1) * novoPreco).toFixed(2) }
    }))
  }

  function handleTrocarPrecoTipo(tipo) {
    setPrecoTipo(tipo)
    atualizarPrecosItens(tipo)
  }

  function handleSelectItemPadrao(ip) {
    const preco = precoTipo === 'normal' ? (ip.preco_padrao || ip.preco_unitario_padrao || 0) : (ip.preco_unitario_desconto || ip.preco_padrao || 0)
    setNovoItem(prev => ({ ...prev, descricao: ip.nome_item, valor_unitario: preco, subtotal: ((parseInt(prev.quantidade) || 1) * parseFloat(preco)).toFixed(2) }))
    setShowItemList(false)
  }

  // SALVAR — grava direto como PENDENTE (pagamento sera feito depois no Financeiro)
  async function handleSalvar() {
    if (!destinatario.trim()) { showToast('Informe o destinatario', 'error'); return }
    setSalvando(true)
    try {
      const rota = rotas.find(r => String(r.id_rota) === idRota)
      const rotaNome = rota ? `${rota.origem} - ${rota.destino}` : (selecionado?.rota_temp || selecionado?.rota || '')
      const payload = {
        id_viagem: viagemAtiva.id_viagem,
        remetente_nome_temp: remetente.trim().toUpperCase(),
        destinatario_nome_temp: destinatario.trim().toUpperCase(),
        rota_temp: rotaNome,
        conferente_temp: conferente,
        observacoes: observacoes.trim(),
        local_transporte: localTransporte,
        cidade_cobranca: cidadeCobranca,
        num_notafiscal: notaFiscal ? numNota : null,
        valor_notafiscal: notaFiscal ? parseFloat(valorNota) || 0 : 0,
        peso_notafiscal: notaFiscal ? parseFloat(pesoNota) || 0 : 0,
        valor_total_itens: totalItens || 0,
        desconto: 0,
        valor_pago: 0,
        troco: 0,
        tipo_pagamento: null,
        nome_caixa: null,
        status_frete: 'PENDENTE',
        itens: itens.map(i => ({ nome_item: i.descricao, quantidade: i.quantidade, preco_unitario: i.valor_unitario, subtotal_item: i.subtotal }))
      }
      if (selecionado && selecionado.id_frete) {
        await api.put(`/fretes/${selecionado.id_frete}`, payload)
        showToast('Frete atualizado')
      } else {
        await api.post('/fretes', payload)
        showToast('Frete salvo como PENDENTE')
      }
      // Preparar automaticamente para proximo frete
      limparForm()
      setEditando(true)
      try { const res = await api.get('/fretes/proximo-numero'); setNumFrete(res.numero || '') } catch { setNumFrete('—') }
    } catch (err) {
      showToast(err.message || 'Erro ao salvar', 'error')
    } finally {
      setSalvando(false)
    }
  }

  // Pagamento (chamado separadamente, nao no SALVAR)
  async function handleConfirmarPagamento() {
    const vDesc = parseFloat(pgDesconto) || 0
    const vPago = parseFloat(pgValorPago) || 0
    setSalvando(true)
    try {
      const rota = rotas.find(r => String(r.id_rota) === idRota)
      const rotaNome = rota ? `${rota.origem} - ${rota.destino}` : (selecionado?.rota_temp || selecionado?.rota || '')
      const payload = {
        id_viagem: viagemAtiva.id_viagem,
        remetente_nome_temp: remetente.trim().toUpperCase(),
        destinatario_nome_temp: destinatario.trim().toUpperCase(),
        rota_temp: rotaNome,
        conferente_temp: conferente,
        observacoes: observacoes.trim(),
        local_transporte: localTransporte,
        cidade_cobranca: cidadeCobranca,
        num_notafiscal: notaFiscal ? numNota : null,
        valor_notafiscal: notaFiscal ? parseFloat(valorNota) || 0 : 0,
        peso_notafiscal: notaFiscal ? parseFloat(pesoNota) || 0 : 0,
        valor_total_itens: totalItens,
        desconto: vDesc,
        valor_pago: vPago,
        troco: Math.max(0, vPago - (totalItens - vDesc)),
        tipo_pagamento: pgTipo,
        nome_caixa: caixas.find(c => String(c.id_caixa) === pgCaixa)?.nome_caixa || '',
        status_frete: vPago >= (totalItens - vDesc) ? 'PAGO' : vPago > 0 ? 'PARCIAL' : 'PENDENTE',
        itens: itens.map(i => ({ nome_item: i.descricao, quantidade: i.quantidade, preco_unitario: i.valor_unitario, subtotal_item: i.subtotal }))
      }
      await api.post('/fretes', payload)
      showToast('Frete salvo com sucesso')
      setModalPagar(null)
      limparForm()
    } catch (err) {
      showToast(err.message || 'Erro ao salvar', 'error')
    } finally {
      setSalvando(false)
    }
  }

  if (!viagemAtiva) {
    return <div className="placeholder-page"><div className="ph-icon">🚚</div><h2>Lancamento de Frete</h2><p>Selecione uma viagem.</p></div>
  }

  const I = { padding: '7px 10px', fontSize: '0.82rem', background: 'var(--bg-soft)', border: '1px solid var(--border)', borderRadius: 4, color: 'var(--text)', fontFamily: 'Sora, sans-serif', width: '100%', boxSizing: 'border-box' }
  const L = { fontSize: '0.75rem', fontWeight: 700, color: 'var(--text)', marginBottom: 3, display: 'block' }
  const RO = { ...I, opacity: 0.6, cursor: 'default' }
  const vData = formatDate(viagemAtiva.data_viagem)

  // Pagamento calculos
  const pgDesc = parseFloat(pgDesconto) || 0
  const pgPago = parseFloat(pgValorPago) || 0
  const pgAPagar = Math.max(0, (modalPagar?.total || 0) - pgDesc)
  const pgDevedor = Math.max(0, pgAPagar - pgPago)
  const pgTroco = Math.max(0, pgPago - pgAPagar)

  return (
    <div className="card" style={{ padding: 12 }}>
      {/* HEADER */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
        <h2 style={{ fontSize: '1.05rem', margin: 0 }}>Lancamento de Frete</h2>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <span style={L}>N° Frete:</span>
          <input style={{ ...RO, width: 80, textAlign: 'center', fontWeight: 700, fontSize: '1rem' }} value={numFrete || '—'} readOnly />
        </div>
      </div>

      {/* ROW 1: Remetente + Destinatario */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 8 }}>
        <div>
          <label style={L}>Remetente:</label>
          <Autocomplete
            value={remetente}
            onChange={v => setRemetente(v)}
            onSelect={c => setRemetente(c.nome_razao_social)}
            onBlur={v => verificarSalvarContato(v)}
            suggestions={filtrarContatos(contatos, remetente)}
            allItems={contatos}
            placeholder="Digite o nome do remetente..."
            emptyMessage="Nenhum contato encontrado. Sera cadastrado como novo."
            renderItem={c => (
              <>
                <div style={{ fontWeight: 600 }}>{c.nome_razao_social}</div>
                {(c.cpf_cnpj || c.telefone) && (
                  <div style={{ fontSize: '0.68rem', opacity: 0.7 }}>
                    {c.cpf_cnpj || ''}{c.cpf_cnpj && c.telefone ? ' | ' : ''}{c.telefone || ''}
                  </div>
                )}
              </>
            )}
          />
        </div>
        <div>
          <label style={L}>Destinatario:</label>
          <Autocomplete
            value={destinatario}
            onChange={v => setDestinatario(v)}
            onSelect={c => setDestinatario(c.nome_razao_social)}
            onBlur={v => verificarSalvarContato(v)}
            suggestions={filtrarContatos(contatos, destinatario)}
            allItems={contatos}
            placeholder="Digite o nome do destinatario..."
            emptyMessage="Nenhum contato encontrado. Sera cadastrado como novo."
            renderItem={c => (
              <>
                <div style={{ fontWeight: 600 }}>{c.nome_razao_social}</div>
                {(c.cpf_cnpj || c.telefone) && (
                  <div style={{ fontSize: '0.68rem', opacity: 0.7 }}>
                    {c.cpf_cnpj || ''}{c.cpf_cnpj && c.telefone ? ' | ' : ''}{c.telefone || ''}
                  </div>
                )}
              </>
            )}
          />
        </div>
      </div>

      {/* ROW 2: Rota, Data, Emissao, Local, Conferente, Cidade */}
      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1fr 1.5fr 1.5fr 1.5fr', gap: 8, marginBottom: 8 }}>
        <div>
          <label style={L}>Rota:</label>
          <select style={I} value={idRota} onChange={e => setIdRota(e.target.value)}>
            <option value=""></option>
            {rotas.map(r => <option key={r.id_rota} value={r.id_rota}>{r.origem} - {r.destino}</option>)}
          </select>
        </div>
        <div><label style={L}>Data Viagem:</label><input style={RO} value={vData} readOnly /></div>
        <div><label style={L}>Emissao:</label><input style={RO} value={new Date().toLocaleDateString('pt-BR')} readOnly /></div>
        <div><label style={L}>Local Transp.:</label><input style={I} value={localTransporte} onChange={e => setLocalTransporte(e.target.value)} placeholder="Ex: Porao" /></div>
        <div>
          <label style={L}>Conferente:</label>
          <select style={I} value={conferente} onChange={e => setConferente(e.target.value)}>
            <option value=""></option>
            {conferentes.map(c => <option key={c.id_conferente} value={c.nome_conferente || c.nome}>{c.nome_conferente || c.nome}</option>)}
          </select>
        </div>
        <div><label style={L}>Cidade Cobranca:</label><input style={I} value={cidadeCobranca} onChange={e => setCidadeCobranca(e.target.value)} /></div>
      </div>

      {/* ROW 3: Nota Fiscal + Observacoes */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 8 }}>
        <div style={{ padding: 10, border: '1px solid var(--border)', borderRadius: 6 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
            <label style={{ ...L, marginBottom: 0 }}>Nota Fiscal?</label>
            <label style={{ fontSize: '0.82rem' }}><input type="radio" checked={notaFiscal} onChange={() => setNotaFiscal(true)} /> Sim</label>
            <label style={{ fontSize: '0.82rem' }}><input type="radio" checked={!notaFiscal} onChange={() => setNotaFiscal(false)} /> Nao</label>
          </div>
          {notaFiscal && (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 8 }}>
              <div><label style={{ ...L, fontSize: '0.68rem' }}>N° Nota:</label><input style={I} value={numNota} onChange={e => setNumNota(e.target.value)} /></div>
              <div><label style={{ ...L, fontSize: '0.68rem' }}>Valor (R$):</label><input style={I} type="number" step="0.01" value={valorNota} onChange={e => setValorNota(e.target.value)} /></div>
              <div><label style={{ ...L, fontSize: '0.68rem' }}>Peso (kg):</label><input style={I} type="number" step="0.01" value={pesoNota} onChange={e => setPesoNota(e.target.value)} /></div>
            </div>
          )}
        </div>
        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 3 }}>
            <label style={{ ...L, marginBottom: 0 }}>Observacoes:</label>
            <div style={{ display: 'flex', gap: 8, alignItems: 'center', fontSize: '0.78rem' }}>
              <label style={L}>Preco:</label>
              <label><input type="radio" checked={precoTipo === 'normal'} onChange={() => handleTrocarPrecoTipo('normal')} /> Normal</label>
              <label><input type="radio" checked={precoTipo === 'desconto'} onChange={() => handleTrocarPrecoTipo('desconto')} /> Desc.</label>
            </div>
          </div>
          <textarea style={{ ...I, minHeight: 60, resize: 'vertical' }} value={observacoes} onChange={e => setObservacoes(e.target.value)} />
        </div>
      </div>

      {/* ITEM ENTRY */}
      <div style={{ display: 'grid', gridTemplateColumns: '60px 1fr 100px 100px auto', gap: 8, padding: 10, background: 'rgba(5,150,105,0.05)', border: '1px solid var(--primary)', borderRadius: 6, marginBottom: 8, alignItems: 'end' }}>
        <div><label style={{ ...L, fontSize: '0.65rem' }}>Qtd.</label><input style={{ ...I, textAlign: 'center' }} type="number" min="1" value={novoItem.quantidade} onChange={e => handleNovoItemChange('quantidade', e.target.value)} /></div>
        <div style={{ position: 'relative' }} ref={itemDropdownRef}>
          <label style={{ ...L, fontSize: '0.65rem' }}>Item / Produto</label>
          <input style={I} value={novoItem.descricao} onChange={e => { handleNovoItemChange('descricao', e.target.value); setShowItemList(true) }} onFocus={() => setShowItemList(true)} placeholder="Digite para buscar..." />
          {showItemList && (() => {
            const q = (novoItem.descricao || '').toLowerCase()
            const filtered = itensPadrao.filter(ip => !q || (ip.nome_item || '').toLowerCase().includes(q))
            if (!filtered.length) return null
            return <div style={{ position: 'absolute', top: '100%', left: 0, right: 0, zIndex: 50, background: 'var(--bg-card)', border: '1px solid var(--border)', borderRadius: 4, maxHeight: 200, overflowY: 'auto', boxShadow: 'var(--shadow-lg)' }}>
              {filtered.map((ip, idx) => {
                const preco = Number(precoTipo === 'normal' ? (ip.preco_padrao || ip.preco_unitario_padrao || 0) : (ip.preco_unitario_desconto || ip.preco_padrao || 0))
                return <div key={ip.id || idx} onMouseDown={() => handleSelectItemPadrao(ip)} style={{ display: 'flex', justifyContent: 'space-between', padding: '7px 10px', cursor: 'pointer', fontSize: '0.82rem', background: idx % 2 === 0 ? 'transparent' : 'var(--bg-soft)', borderBottom: '1px solid var(--border)' }}
                  onMouseEnter={e => { e.currentTarget.style.background = 'var(--primary)'; e.currentTarget.style.color = '#fff' }}
                  onMouseLeave={e => { e.currentTarget.style.background = idx % 2 === 0 ? 'transparent' : 'var(--bg-soft)'; e.currentTarget.style.color = '' }}>
                  <span style={{ fontWeight: 600 }}>{ip.nome_item}</span>
                  <span style={{ fontFamily: 'Space Mono, monospace', fontSize: '0.78rem' }}>R$ {preco.toFixed(2)}</span>
                </div>
              })}
            </div>
          })()}
        </div>
        <div><label style={{ ...L, fontSize: '0.65rem' }}>Preco Unit.</label><MoneyInput value={novoItem.valor_unitario} onChange={v => handleNovoItemChange('valor_unitario', v)} /></div>
        <div><label style={{ ...L, fontSize: '0.65rem' }}>Subtotal</label><MoneyInput value={novoItem.subtotal} readOnly /></div>
        <button onClick={handleAdicionarItem} style={{ padding: '8px 16px', background: '#F59E0B', color: '#fff', border: 'none', borderRadius: 4, fontWeight: 700, cursor: 'pointer', whiteSpace: 'nowrap' }}>ADICIONAR</button>
      </div>

      {/* TABELA ITENS */}
      <div className="table-container" style={{ marginBottom: 0, flex: 1 }}>
        <table>
          <thead><tr>
            <th style={{ width: 60 }}>Qtd</th>
            <th>Descricao do Item</th>
            <th style={{ width: 110 }}>Preco Unit.</th>
            <th style={{ width: 110 }}>Subtotal</th>
            <th style={{ width: 40 }}></th>
          </tr></thead>
          <tbody>
            {itens.length === 0 ? (
              <tr><td colSpan="5" style={{ textAlign: 'center', padding: 30, color: 'var(--text-muted)' }}>Nao ha conteudo na tabela</td></tr>
            ) : itens.map((item, idx) => (
              <tr key={idx}>
                <td style={{ textAlign: 'center' }}>
                  <input type="number" min="1" value={item.quantidade} style={{ width: 40, textAlign: 'center', background: 'transparent', border: '1px solid transparent', color: 'inherit', fontSize: 'inherit' }}
                    onFocus={e => e.target.style.borderColor = 'var(--primary)'} onBlur={e => e.target.style.borderColor = 'transparent'}
                    onChange={e => { const v = parseInt(e.target.value) || 1; setItens(prev => prev.map((it, i) => i === idx ? { ...it, quantidade: v, subtotal: (v * (parseFloat(it.valor_unitario) || 0)).toFixed(2) } : it)) }} />
                </td>
                <td><input value={item.descricao} style={{ width: '100%', background: 'transparent', border: '1px solid transparent', color: 'inherit', fontSize: 'inherit' }}
                  onFocus={e => e.target.style.borderColor = 'var(--primary)'} onBlur={e => e.target.style.borderColor = 'transparent'}
                  onChange={e => setItens(prev => prev.map((it, i) => i === idx ? { ...it, descricao: e.target.value } : it))} /></td>
                <td className="money">
                  <MoneyInput value={item.valor_unitario}
                    style={{ width: 100, background: 'transparent', border: '1px solid transparent', color: 'inherit' }}
                    onChange={v => setItens(prev => prev.map((it, i) => i === idx ? { ...it, valor_unitario: v, subtotal: ((parseInt(it.quantidade) || 1) * v).toFixed(2) } : it))} />
                </td>
                <td className="money" style={{ fontWeight: 700 }}>{formatMoney(item.subtotal)}</td>
                <td><button className="btn-sm danger" onClick={() => setItens(prev => prev.filter((_, i) => i !== idx))} style={{ padding: '2px 6px' }}>×</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* TOTAIS no final */}
      {itens.length > 0 && (
        <div style={{ borderTop: '2px solid var(--primary)', padding: '10px 0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div><label style={L}>Volumes:</label><span style={{ fontWeight: 700, fontSize: '1rem' }}>{totalVolumes}</span></div>
          <div style={{ fontWeight: 700, fontSize: '1.2rem', color: 'var(--primary)', fontFamily: 'Space Mono, monospace' }}>TOTAL GERAL: {formatMoney(totalItens)}</div>
        </div>
      )}

      {/* BOTOES */}
      <div style={{ display: 'flex', justifyContent: 'space-between', padding: '10px 0', borderTop: '1px solid var(--border)', flexWrap: 'wrap', gap: 6 }}>
        <div style={{ display: 'flex', gap: 6 }}>
          <button className="btn-sm primary" onClick={handleNovo}>Novo (F1)</button>
          <button className="btn-sm primary" onClick={() => {}}>Alterar (F2)</button>
          <button className="btn-sm primary" onClick={handleSalvar} disabled={!editando || salvando} style={{ fontWeight: 700 }}>SALVAR (F3)</button>
          <button className="btn-sm danger" onClick={() => {}}>Excluir (F4)</button>
        </div>
        <div style={{ display: 'flex', gap: 6 }}>
          <button className="btn-sm primary" onClick={() => onNavigate && onNavigate('listar-fretes')}>Lista de Fretes (F5)</button>
          <button className="btn-sm primary" onClick={() => { if (selecionado) printNotaFrete(selecionado, viagemAtiva) }}>Imprimir (F6)</button>
          <button className="btn-sm primary" onClick={() => {
            if (!selecionado) return
            // OK = rolo (80mm, uma embaixo da outra) / Cancelar = A4 (varias por folha)
            const rolo = window.confirm('Imprimir em impressora de rolo (80mm)?\n\n[OK] Rolo termico 80mm — uma etiqueta embaixo da outra\n[Cancelar] Folha A4 — varias etiquetas por pagina')
            printEtiquetaFrete(selecionado, rolo ? 'rolo' : 'a4')
          }}>Etiqueta</button>
          <button className="btn-sm" onClick={() => onClose ? onClose() : limparForm()}>SAIR (Esc)</button>
        </div>
      </div>

      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}

      {/* MODAL CADASTRAR NOVO CONTATO/CLIENTE */}
      {modalNovoContato && (
        <div className="modal-overlay" onClick={() => setModalNovoContato(null)}>
          <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 600 }}>
            <h3>Cadastrar Novo Cliente</h3>
            <p style={{ color: 'var(--text-muted)', marginBottom: 12 }}>
              Cliente <strong>"{modalNovoContato}"</strong> nao esta cadastrado. Preencha os dados (apenas nome e obrigatorio):
            </p>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '6px 12px' }}>
              <div style={{ gridColumn: '1 / -1' }}>
                <label style={L}>Nome: *</label>
                <input style={I} value={modalNovoContato} readOnly />
              </div>
              <div style={{ gridColumn: '1 / -1' }}>
                <label style={L}>Razao Social:</label>
                <input style={I} value={ncRazaoSocial} onChange={e => setNcRazaoSocial(e.target.value)} placeholder="Razao social completa" />
              </div>
              <div>
                <label style={L}>CPF ou CNPJ:</label>
                <input style={I} value={ncCpfCnpj} onChange={e => setNcCpfCnpj(e.target.value)} placeholder="000.000.000-00" />
              </div>
              <div>
                <label style={L}>Inscricao Estadual:</label>
                <input style={I} value={ncInscricao} onChange={e => setNcInscricao(e.target.value)} />
              </div>
              <div style={{ gridColumn: '1 / -1' }}>
                <label style={L}>Endereco:</label>
                <input style={I} value={ncEndereco} onChange={e => setNcEndereco(e.target.value)} placeholder="Rua, numero, bairro, cidade" />
              </div>
              <div>
                <label style={L}>Email:</label>
                <input style={I} value={ncEmail} onChange={e => setNcEmail(e.target.value)} placeholder="email@exemplo.com" />
              </div>
              <div>
                <label style={L}>Telefone:</label>
                <input style={I} value={ncTelefone} onChange={e => setNcTelefone(e.target.value)} placeholder="(00) 00000-0000" />
              </div>
            </div>
            <div style={{ display: 'flex', gap: 8, marginTop: 14, justifyContent: 'flex-end' }}>
              <button className="btn-sm" onClick={() => setModalNovoContato(null)}>Cancelar</button>
              <button className="btn-sm" onClick={() => { api.post('/fretes/contatos', { nome: modalNovoContato }).then(() => api.get('/fretes/contatos').then(setContatos)); setModalNovoContato(null); showToast('Salvo apenas com nome') }} style={{ background: 'var(--bg-soft)' }}>Salvar so Nome</button>
              <button className="btn-sm primary" style={{ fontWeight: 700 }} onClick={salvarNovoContato}>Salvar Completo</button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL CADASTRAR NOVO ITEM */}
      {modalNovoItem && (
        <div className="modal-overlay" onClick={() => setModalNovoItem(null)}>
          <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 700 }}>
            <h3>Cadastrar Novo Item</h3>
            <p style={{ color: 'var(--text-muted)', marginBottom: 12 }}>
              Item <strong>"{modalNovoItem.nome}"</strong> nao esta cadastrado.
            </p>

            <div style={{ borderTop: '2px solid var(--primary)', padding: '12px 0' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                <span style={{ fontWeight: 700 }}>Adicionar / Editar Item</span>
                <span style={{ color: 'var(--primary)', fontSize: '0.82rem', fontStyle: 'italic' }}>Modo: Insercao de Novo Item</span>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr auto auto', gap: 12, alignItems: 'end' }}>
                <div>
                  <label style={{ fontSize: '0.78rem', fontWeight: 700, display: 'block', marginBottom: 3 }}>Descricao do Item:</label>
                  <input style={{ padding: '10px 14px', fontSize: '0.9rem', background: 'var(--bg-soft)', border: '1px solid var(--border)', borderRadius: 6, color: 'var(--text)', width: '100%', boxSizing: 'border-box' }}
                    value={modalNovoItem.nome} onChange={e => setModalNovoItem({ ...modalNovoItem, nome: e.target.value.toUpperCase() })} />
                </div>
                <div>
                  <label style={{ fontSize: '0.78rem', fontWeight: 700, display: 'block', marginBottom: 3 }}>Preco Normal (R$):</label>
                  <input type="number" step="0.01" min="0" style={{ padding: '10px 14px', fontSize: '0.9rem', background: 'var(--bg-soft)', border: '1px solid var(--primary)', borderRadius: 6, color: 'var(--text)', width: 130, textAlign: 'right', fontFamily: 'Space Mono, monospace' }}
                    value={novoItemPrecoNormal} onChange={e => setNovoItemPrecoNormal(e.target.value)} autoFocus />
                </div>
                <div>
                  <label style={{ fontSize: '0.78rem', fontWeight: 700, display: 'block', marginBottom: 3 }}>Preco c/ Desc. (R$):</label>
                  <input type="number" step="0.01" min="0" style={{ padding: '10px 14px', fontSize: '0.9rem', background: 'var(--bg-soft)', border: '1px solid var(--border)', borderRadius: 6, color: 'var(--text)', width: 130, textAlign: 'right', fontFamily: 'Space Mono, monospace' }}
                    value={novoItemPrecoDesc} onChange={e => setNovoItemPrecoDesc(e.target.value)} />
                </div>
              </div>
            </div>

            <div className="modal-actions" style={{ marginTop: 12 }}>
              <button className="btn-secondary" onClick={() => setModalNovoItem(null)}>Cancelar</button>
              <button className="btn-primary" style={{ width: 'auto', padding: '10px 24px' }} onClick={handleSalvarNovoItem}>SALVAR</button>
            </div>

            {/* Mini tabela dos itens ja cadastrados */}
            {itensPadrao.length > 0 && (
              <div style={{ marginTop: 16, borderTop: '1px solid var(--border)', paddingTop: 12 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                  <span style={{ fontWeight: 700, fontSize: '0.82rem' }}>Itens Cadastrados no Sistema:</span>
                </div>
                <div style={{ maxHeight: 200, overflowY: 'auto' }}>
                  <table style={{ width: '100%' }}>
                    <thead><tr>
                      <th>Descricao / Item</th>
                      <th style={{ width: 110, textAlign: 'right' }}>Preco Normal</th>
                      <th style={{ width: 110, textAlign: 'right' }}>Preco Desconto</th>
                    </tr></thead>
                    <tbody>
                      {itensPadrao.map((ip, idx) => (
                        <tr key={ip.id || idx}>
                          <td>{ip.nome_item}</td>
                          <td className="money" style={{ textAlign: 'right' }}>{formatMoney(ip.preco_padrao || ip.preco_unitario_padrao)}</td>
                          <td className="money" style={{ textAlign: 'right' }}>{formatMoney(ip.preco_unitario_desconto)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* MODAL PAGAMENTO */}
      {modalPagar && (
        <div className="modal-overlay" onClick={() => setModalPagar(null)}>
          <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 420 }}>
            <h3>Pagamento do Frete</h3>
            <div className="form-grid">
              <div className="form-group"><label>Total Frete</label><input value={formatMoney(modalPagar.total)} readOnly style={{ opacity: 0.6 }} /></div>
              <div className="form-group"><label>Desconto</label><input type="number" step="0.01" min="0" value={pgDesconto} onChange={e => setPgDesconto(e.target.value)} /></div>
              <div className="form-group"><label>A Pagar</label><input value={formatMoney(pgAPagar)} readOnly style={{ opacity: 0.6 }} /></div>
              <div className="form-group"><label>Valor Pago</label><input type="number" step="0.01" min="0" value={pgValorPago} onChange={e => setPgValorPago(e.target.value)} autoFocus /></div>
              <div className="form-group"><label>Devedor</label><input value={formatMoney(pgDevedor)} readOnly style={{ opacity: 0.6, color: pgDevedor > 0 ? 'var(--danger)' : undefined }} /></div>
              <div className="form-group"><label>Troco</label><input value={formatMoney(pgTroco)} readOnly style={{ opacity: 0.6 }} /></div>
              <div className="form-group"><label>Tipo Pagamento</label>
                <select value={pgTipo} onChange={e => setPgTipo(e.target.value)}>
                  <option>Dinheiro</option><option>Cartao</option><option>PIX</option>
                </select></div>
              <div className="form-group"><label>Caixa</label>
                <select value={pgCaixa} onChange={e => setPgCaixa(e.target.value)}>
                  <option value="">Selecione...</option>
                  {caixas.map(c => <option key={c.id_caixa} value={c.id_caixa}>{c.nome_caixa}</option>)}
                </select></div>
            </div>
            <div className="modal-actions">
              <button className="btn-secondary" onClick={() => setModalPagar(null)}>Cancelar</button>
              <button className="btn-primary" onClick={handleConfirmarPagamento} disabled={salvando}>{salvando ? 'Salvando...' : 'OK'}</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

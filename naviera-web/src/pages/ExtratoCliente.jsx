import { useState, useEffect, useCallback, useMemo } from 'react'
import { api } from '../api.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}
function fmtDate(d) {
  if (!d) return '—'
  const s = String(d)
  if (/^\d{4}-\d{2}-\d{2}/.test(s)) {
    const [y, m, da] = s.substring(0, 10).split('-')
    return `${da}/${m}/${y}`
  }
  if (/^\d{2}\/\d{2}\/\d{4}$/.test(s)) return s
  const dt = new Date(s)
  return isNaN(dt) ? s : dt.toLocaleDateString('pt-BR')
}
function hojeISO() {
  return new Date().toISOString().slice(0, 10)
}

const FORMAS_PAGAMENTO = ['DINHEIRO', 'PIX', 'CARTAO', 'TRANSFERENCIA', 'CHEQUE', 'BOLETO']

/**
 * Extrato de Cliente consolidado (frete + encomenda + passagem).
 * Filtra por cliente, viagem, tipo e status; permite dar baixa em aberto.
 */
export default function ExtratoCliente({ viagens: viagensProp = [], viagemAtiva }) {
  const [viagens, setViagens] = useState(viagensProp)
  const [clientes, setClientes] = useState([])
  const [cliente, setCliente] = useState('')
  const [viagemId, setViagemId] = useState('')
  const [tipoFrete, setTipoFrete] = useState(true)
  const [tipoEncomenda, setTipoEncomenda] = useState(true)
  const [tipoPassagem, setTipoPassagem] = useState(true)
  const [status, setStatus] = useState('todos')

  const [itens, setItens] = useState([])
  const [loading, setLoading] = useState(false)
  const [toast, setToast] = useState(null)

  const [modalPagar, setModalPagar] = useState(null) // {modo:'item'|'lote', item|itens, ...}
  const [valorPagar, setValorPagar] = useState('')
  const [formaPag, setFormaPag] = useState('DINHEIRO')
  const [dataPag, setDataPag] = useState(hojeISO())
  const [obsPag, setObsPag] = useState('')
  const [salvando, setSalvando] = useState(false)

  const [selecionados, setSelecionados] = useState(new Set())
  const keyOf = (it) => `${it.tipo}:${it.id_original}`

  function showToast(msg, type = 'success') { setToast({ msg, type }); setTimeout(() => setToast(null), 3500) }

  useEffect(() => {
    api.get('/extrato-cliente/clientes').then(setClientes).catch(() => {})
    api.get('/viagens').then(v => setViagens(Array.isArray(v) ? v : [])).catch(() => {})
  }, [])

  const buscar = useCallback(async () => {
    if (!cliente) { showToast('Selecione um cliente', 'error'); return }
    const tipos = []
    if (tipoFrete) tipos.push('frete')
    if (tipoEncomenda) tipos.push('encomenda')
    if (tipoPassagem) tipos.push('passagem')
    if (tipos.length === 0) { showToast('Marque pelo menos um tipo', 'error'); return }

    setLoading(true)
    try {
      const params = new URLSearchParams({ cliente, tipos: tipos.join(','), status })
      if (viagemId) params.append('viagem_id', viagemId)
      const data = await api.get(`/extrato-cliente/buscar?${params}`)
      setItens(Array.isArray(data) ? data : [])
    } catch (err) {
      showToast(err?.message || 'Erro ao buscar extrato', 'error')
    } finally {
      setLoading(false)
    }
  }, [cliente, viagemId, tipoFrete, tipoEncomenda, tipoPassagem, status])

  const totais = useMemo(() => {
    let total = 0, pago = 0, saldo = 0
    for (const it of itens) {
      total += Number(it.valor_total) || 0
      pago += Number(it.valor_pago) || 0
      saldo += Number(it.saldo_devedor) || 0
    }
    return { total, pago, saldo, count: itens.length }
  }, [itens])

  function abrirModalItem(it) {
    setModalPagar({ modo: 'item', item: it })
    setValorPagar(String(it.saldo_devedor).replace('.', ','))
    setFormaPag('DINHEIRO')
    setDataPag(hojeISO())
    setObsPag('')
  }

  function abrirModalLote(itensAlvo, label) {
    if (itensAlvo.length === 0) { showToast('Nada em aberto', 'error'); return }
    const total = itensAlvo.reduce((s, it) => s + Number(it.saldo_devedor), 0)
    setModalPagar({ modo: 'lote', itens: itensAlvo, total, label })
    setValorPagar(String(total.toFixed(2)).replace('.', ','))
    setFormaPag('DINHEIRO')
    setDataPag(hojeISO())
    setObsPag('')
  }

  async function confirmarBaixa() {
    if (!modalPagar) return
    if (modalPagar.modo === 'item') {
      const it = modalPagar.item
      const valor = parseFloat(String(valorPagar).replace(',', '.'))
      if (!(valor > 0)) { showToast('Informe valor > 0', 'error'); return }
      if (valor > Number(it.saldo_devedor) + 0.01) { showToast('Valor maior que o saldo devedor', 'error'); return }
      setSalvando(true)
      try {
        await api.post('/extrato-cliente/baixa', {
          tipo: it.tipo, id_original: it.id_original, valor, forma_pagamento: formaPag,
        })
        const comprovante = {
          tipo: 'individual',
          cliente,
          data_pagamento: dataPag,
          forma_pagamento: formaPag,
          observacao: obsPag,
          itens: [{ ...it, valor_baixa: valor }],
          total: valor,
        }
        showToast('Baixa registrada')
        setModalPagar(null)
        imprimirComprovante(comprovante)
        buscar()
      } catch (err) {
        showToast(err?.message || 'Erro ao dar baixa', 'error')
      } finally {
        setSalvando(false)
      }
    } else {
      // lote — quita cada item pelo saldo devedor com a mesma forma de pagamento
      setSalvando(true)
      try {
        const r = await api.post('/extrato-cliente/quitar-tudo', {
          itens: modalPagar.itens.map(it => ({
            tipo: it.tipo, id_original: it.id_original, valor: Number(it.saldo_devedor),
          })),
          forma_pagamento: formaPag,
        })
        const comprovante = {
          tipo: 'lote',
          cliente,
          data_pagamento: dataPag,
          forma_pagamento: formaPag,
          observacao: obsPag,
          itens: modalPagar.itens.map(it => ({ ...it, valor_baixa: Number(it.saldo_devedor) })),
          total: modalPagar.total,
        }
        showToast(`${r.sucesso} lançamento(s) quitado(s)`)
        setModalPagar(null)
        setSelecionados(new Set())
        imprimirComprovante(comprovante)
        buscar()
      } catch (err) {
        showToast(err?.message || 'Erro ao quitar', 'error')
      } finally {
        setSalvando(false)
      }
    }
  }

  function quitarTudo() {
    const devedores = itens.filter(it => Number(it.saldo_devedor) > 0.01)
    abrirModalLote(devedores, 'Quitar Tudo')
  }
  function quitarSelecionados() {
    const alvo = itens.filter(it => selecionados.has(keyOf(it)) && Number(it.saldo_devedor) > 0.01)
    abrirModalLote(alvo, 'Dar Baixa nos Selecionados')
  }

  function toggleSelecionado(it) {
    const k = keyOf(it)
    const novo = new Set(selecionados)
    if (novo.has(k)) novo.delete(k); else novo.add(k)
    setSelecionados(novo)
  }
  function toggleTodos() {
    const devedores = itens.filter(it => Number(it.saldo_devedor) > 0.01)
    if (selecionados.size >= devedores.length && devedores.every(it => selecionados.has(keyOf(it)))) {
      setSelecionados(new Set())
    } else {
      setSelecionados(new Set(devedores.map(keyOf)))
    }
  }

  useEffect(() => { setSelecionados(new Set()) }, [itens])

  const totalSelecionado = useMemo(() => {
    return itens.filter(it => selecionados.has(keyOf(it))).reduce((s, it) => s + Number(it.saldo_devedor || 0), 0)
  }, [itens, selecionados])

  const devedoresCount = itens.filter(it => Number(it.saldo_devedor) > 0.01).length
  const todosDevedoresSelecionados = devedoresCount > 0 && selecionados.size === devedoresCount
    && itens.filter(it => Number(it.saldo_devedor) > 0.01).every(it => selecionados.has(keyOf(it)))

  function imprimirExtrato() {
    if (itens.length === 0) { showToast('Nada para imprimir', 'error'); return }
    const filtrosTxt = []
    if (viagemId === 'agenda') filtrosTxt.push('Agenda (a partir de hoje)')
    else if (viagemId) {
      const v = viagens.find(x => String(x.id_viagem) === String(viagemId))
      if (v) filtrosTxt.push(`Viagem #${v.id_viagem} - ${fmtDate(v.data_viagem)}`)
    } else filtrosTxt.push('Todas as viagens')
    if (status !== 'todos') filtrosTxt.push(`Status: ${status === 'devedores' ? 'A pagar' : 'Pagos'}`)
    const tipos = []
    if (tipoFrete) tipos.push('Frete'); if (tipoEncomenda) tipos.push('Encomenda'); if (tipoPassagem) tipos.push('Passagem')
    filtrosTxt.push(`Tipos: ${tipos.join(', ')}`)

    const linhas = itens.map(it => `
      <tr>
        <td>${it.tipo_label}</td>
        <td>${it.numero || '—'}</td>
        <td>${fmtDate(it.data_viagem)}</td>
        <td>${it.rota || '—'}</td>
        <td>${it.descricao || ''}</td>
        <td class="money">${formatMoney(it.valor_total)}</td>
        <td class="money">${formatMoney(it.valor_pago)}</td>
        <td class="money ${Number(it.saldo_devedor) > 0.01 ? 'devedor' : ''}">${formatMoney(it.saldo_devedor)}</td>
        <td>${it.status}</td>
      </tr>`).join('')

    const html = `<!DOCTYPE html><html><head><meta charset="utf-8"><title>Extrato ${cliente}</title><style>
      @page { size: A4; margin: 12mm; }
      * { box-sizing: border-box; }
      body { font-family: Arial, sans-serif; color: #1a1a1a; font-size: 11px; margin: 0; }
      h1 { color: #1d6f4a; font-size: 18px; margin: 0 0 4px; }
      .head { border-bottom: 2px solid #1d6f4a; padding-bottom: 8px; margin-bottom: 12px; }
      .meta { font-size: 10.5px; color: #444; }
      .meta strong { color: #1a1a1a; }
      table { width: 100%; border-collapse: collapse; margin-top: 8px; }
      th { background: #1d6f4a; color: #fff; padding: 6px 5px; text-align: left; font-size: 10.5px; border: 1px solid #1d6f4a; }
      td { padding: 5px; border: 1px solid #d0d7d3; font-size: 10.5px; }
      td.money { text-align: right; font-variant-numeric: tabular-nums; }
      td.devedor { color: #b00020; font-weight: 700; }
      tr:nth-child(even) td { background: #f4faf6; }
      .totais { margin-top: 14px; display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
      .tot { border: 1px solid #1d6f4a; border-left: 4px solid #1d6f4a; padding: 8px 10px; }
      .tot .l { font-size: 9.5px; color: #555; text-transform: uppercase; letter-spacing: .5px; }
      .tot .v { font-size: 13px; font-weight: 700; color: #1d6f4a; margin-top: 2px; }
      .tot.devedor .v { color: #b00020; }
      .footer { margin-top: 18px; font-size: 9.5px; color: #666; text-align: center; }
    </style></head><body>
      <div class="head">
        <h1>Extrato de Cliente</h1>
        <div class="meta">
          <div><strong>Cliente:</strong> ${cliente}</div>
          <div><strong>Filtros:</strong> ${filtrosTxt.join(' · ')}</div>
          <div><strong>Emitido em:</strong> ${new Date().toLocaleString('pt-BR')}</div>
        </div>
      </div>
      <table>
        <thead>
          <tr><th>Tipo</th><th>Número</th><th>Data Viagem</th><th>Rota</th><th>Descrição</th>
              <th>Total</th><th>Pago</th><th>A Pagar</th><th>Status</th></tr>
        </thead>
        <tbody>${linhas}</tbody>
      </table>
      <div class="totais">
        <div class="tot"><div class="l">Total Lançado</div><div class="v">${formatMoney(totais.total)}</div></div>
        <div class="tot"><div class="l">Total Pago</div><div class="v">${formatMoney(totais.pago)}</div></div>
        <div class="tot ${totais.saldo > 0.01 ? 'devedor' : ''}"><div class="l">A Receber</div><div class="v">${formatMoney(totais.saldo)}</div></div>
        <div class="tot"><div class="l">Lançamentos</div><div class="v">${totais.count}</div></div>
      </div>
      <div class="footer">Naviera Eco · Extrato gerado pelo sistema</div>
      <script>window.onload = () => { window.print(); setTimeout(() => window.close(), 500) }</script>
    </body></html>`

    const w = window.open('', '_blank', 'width=1000,height=700')
    if (!w) { showToast('Habilite popups para imprimir', 'error'); return }
    w.document.write(html); w.document.close()
  }

  function imprimirComprovante(c) {
    const linhas = c.itens.map(it => `
      <tr>
        <td>${it.tipo_label}</td>
        <td>${it.numero || '—'}</td>
        <td>${fmtDate(it.data_viagem)}</td>
        <td>${it.descricao || ''}</td>
        <td class="money">${formatMoney(it.valor_baixa)}</td>
      </tr>`).join('')

    const html = `<!DOCTYPE html><html><head><meta charset="utf-8"><title>Comprovante de Pagamento</title><style>
      @page { size: A4; margin: 14mm; }
      body { font-family: Arial, sans-serif; color: #1a1a1a; font-size: 12px; margin: 0; }
      .box { max-width: 720px; margin: 0 auto; border: 2px solid #1d6f4a; border-radius: 6px; padding: 18px 22px; }
      h1 { color: #1d6f4a; font-size: 20px; margin: 0 0 4px; }
      .sub { color: #666; font-size: 10.5px; margin-bottom: 14px; border-bottom: 1px solid #d0d7d3; padding-bottom: 8px; }
      .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px 18px; margin-bottom: 14px; }
      .grid .l { font-size: 9.5px; color: #555; text-transform: uppercase; letter-spacing: .5px; }
      .grid .v { font-weight: 700; color: #1a1a1a; font-size: 12.5px; }
      table { width: 100%; border-collapse: collapse; margin-top: 6px; }
      th { background: #1d6f4a; color: #fff; padding: 6px; text-align: left; font-size: 11px; }
      td { padding: 6px; border-bottom: 1px solid #d0d7d3; font-size: 11px; }
      td.money { text-align: right; font-variant-numeric: tabular-nums; }
      .total { margin-top: 14px; padding: 10px 12px; background: #f4faf6; border-left: 4px solid #1d6f4a; display: flex; justify-content: space-between; align-items: center; }
      .total .l { font-size: 11px; color: #555; text-transform: uppercase; letter-spacing: .5px; }
      .total .v { font-size: 18px; font-weight: 800; color: #1d6f4a; }
      .obs { margin-top: 12px; padding: 8px 12px; background: #fafafa; border: 1px dashed #c0c8c4; border-radius: 4px; font-size: 11px; }
      .obs .l { font-size: 9.5px; color: #555; text-transform: uppercase; letter-spacing: .5px; margin-bottom: 2px; }
      .ass { margin-top: 38px; display: grid; grid-template-columns: 1fr 1fr; gap: 30px; }
      .ass .lin { border-top: 1px solid #1a1a1a; padding-top: 4px; text-align: center; font-size: 10px; color: #444; }
      .footer { margin-top: 18px; font-size: 9.5px; color: #666; text-align: center; }
    </style></head><body>
      <div class="box">
        <h1>Comprovante de Pagamento</h1>
        <div class="sub">Naviera Eco · Emitido em ${new Date().toLocaleString('pt-BR')}</div>
        <div class="grid">
          <div><div class="l">Cliente</div><div class="v">${c.cliente}</div></div>
          <div><div class="l">Data do Pagamento</div><div class="v">${fmtDate(c.data_pagamento)}</div></div>
          <div><div class="l">Forma de Pagamento</div><div class="v">${c.forma_pagamento}</div></div>
          <div><div class="l">Lançamentos Quitados</div><div class="v">${c.itens.length}</div></div>
        </div>
        <table>
          <thead><tr><th>Tipo</th><th>Número</th><th>Data Viagem</th><th>Descrição</th><th>Valor Pago</th></tr></thead>
          <tbody>${linhas}</tbody>
        </table>
        <div class="total">
          <div class="l">Total Pago</div>
          <div class="v">${formatMoney(c.total)}</div>
        </div>
        ${c.observacao ? `<div class="obs"><div class="l">Observação</div>${c.observacao.replace(/</g, '&lt;')}</div>` : ''}
        <div class="ass">
          <div class="lin">Recebido por</div>
          <div class="lin">Cliente</div>
        </div>
        <div class="footer">Este comprovante foi gerado automaticamente pelo sistema.</div>
      </div>
      <script>window.onload = () => { window.print(); setTimeout(() => window.close(), 500) }</script>
    </body></html>`

    const w = window.open('', '_blank', 'width=900,height=700')
    if (!w) { showToast('Habilite popups para imprimir comprovante', 'error'); return }
    w.document.write(html); w.document.close()
  }

  const S = {
    label: { fontSize: '0.75rem', fontWeight: 700, color: 'var(--text)', display: 'block', marginBottom: 3 },
    input: { width: '100%', padding: '7px 10px', fontSize: '0.85rem', background: 'var(--bg-soft)', border: '1px solid var(--border)', borderRadius: 6 },
    btn: { padding: '8px 14px', fontSize: '0.82rem', fontWeight: 600, borderRadius: 6, border: '1px solid var(--border)', cursor: 'pointer' },
  }

  return (
    <div className="page">
      <div className="card" style={{ marginBottom: 16 }}>
        <div className="card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <h3>Extrato de Cliente</h3>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Frete · Encomenda · Passagem consolidados</span>
          </div>
          <button style={{ ...S.btn, background: 'var(--bg-soft)', color: 'var(--text)' }}
            onClick={imprimirExtrato} disabled={itens.length === 0}>
            🖨 Imprimir Extrato
          </button>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '2fr 1.5fr auto', gap: 10, marginBottom: 10 }}>
          <div>
            <label style={S.label}>Cliente</label>
            <input style={S.input} list="clientes-list" value={cliente} onChange={e => setCliente(e.target.value)}
              placeholder="Selecione ou digite o nome do cliente..." />
            <datalist id="clientes-list">
              {clientes.map(c => <option key={c} value={c} />)}
            </datalist>
          </div>
          <div>
            <label style={S.label}>Viagem</label>
            <select style={S.input} value={viagemId} onChange={e => setViagemId(e.target.value)}>
              <option value="">Todas as viagens</option>
              <option value="agenda">📅 Agenda (viagens a partir de hoje)</option>
              <optgroup label="— Viagem específica —">
                {viagens.map(v => (
                  <option key={v.id_viagem} value={v.id_viagem}>
                    {v.id_viagem} - {fmtDate(v.data_viagem)} {v.nome_rota ? `(${v.nome_rota})` : ''}
                  </option>
                ))}
              </optgroup>
            </select>
          </div>
          <div style={{ display: 'flex', alignItems: 'flex-end' }}>
            <button style={{ ...S.btn, background: 'var(--primary)', color: '#fff', borderColor: 'var(--primary)' }}
              disabled={loading} onClick={buscar}>
              {loading ? 'Buscando...' : '🔍 Buscar'}
            </button>
          </div>
        </div>

        <div style={{ display: 'flex', gap: 20, alignItems: 'center', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', fontSize: '0.82rem' }}>
            <strong style={{ color: 'var(--text-muted)' }}>Tipo:</strong>
            <label><input type="checkbox" checked={tipoFrete} onChange={e => setTipoFrete(e.target.checked)} /> Frete</label>
            <label><input type="checkbox" checked={tipoEncomenda} onChange={e => setTipoEncomenda(e.target.checked)} /> Encomenda</label>
            <label><input type="checkbox" checked={tipoPassagem} onChange={e => setTipoPassagem(e.target.checked)} /> Passagem</label>
          </div>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', fontSize: '0.82rem' }}>
            <strong style={{ color: 'var(--text-muted)' }}>Status:</strong>
            <label><input type="radio" name="st" checked={status === 'todos'} onChange={() => setStatus('todos')} /> Todos</label>
            <label><input type="radio" name="st" checked={status === 'devedores'} onChange={() => setStatus('devedores')} /> A pagar</label>
            <label><input type="radio" name="st" checked={status === 'pagos'} onChange={() => setStatus('pagos')} /> Pagos</label>
          </div>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 10, marginBottom: 16 }}>
        <div className="stat-card primary">
          <span className="stat-label">Total Lançado</span>
          <span className="stat-value">{formatMoney(totais.total)}</span>
        </div>
        <div className="stat-card success">
          <span className="stat-label">Total Pago</span>
          <span className="stat-value">{formatMoney(totais.pago)}</span>
        </div>
        <div className="stat-card warning">
          <span className="stat-label">A Receber</span>
          <span className="stat-value" style={{ color: totais.saldo > 0.01 ? 'var(--danger)' : 'var(--success)' }}>
            {formatMoney(totais.saldo)}
          </span>
        </div>
        <div className="stat-card info">
          <span className="stat-label">Lançamentos</span>
          <span className="stat-value">{totais.count}</span>
        </div>
      </div>

      <div className="card">
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th style={{ width: 32, textAlign: 'center' }}>
                  <input type="checkbox" checked={todosDevedoresSelecionados} onChange={toggleTodos}
                    disabled={devedoresCount === 0}
                    title={todosDevedoresSelecionados ? 'Desmarcar todos' : 'Marcar todos pendentes'} />
                </th>
                <th>Tipo</th>
                <th>Número</th>
                <th>Data Viagem</th>
                <th>Rota</th>
                <th>Remetente/Origem</th>
                <th>Descrição</th>
                <th style={{ textAlign: 'right' }}>Total</th>
                <th style={{ textAlign: 'right' }}>Pago</th>
                <th style={{ textAlign: 'right' }}>A Pagar</th>
                <th>Status</th>
                <th>Ação</th>
              </tr>
            </thead>
            <tbody>
              {itens.length === 0 ? (
                <tr><td colSpan="12" style={{ textAlign: 'center', color: 'var(--text-muted)', padding: 30 }}>
                  {loading ? 'Carregando...' : 'Selecione um cliente e clique em Buscar'}
                </td></tr>
              ) : itens.map((it, idx) => {
                const devedor = Number(it.saldo_devedor) > 0.01
                const k = keyOf(it)
                const checked = selecionados.has(k)
                return (
                  <tr key={k + '-' + idx} style={checked ? { background: 'var(--bg-accent)' } : undefined}>
                    <td style={{ textAlign: 'center' }}>
                      {devedor && (
                        <input type="checkbox" checked={checked} onChange={() => toggleSelecionado(it)} />
                      )}
                    </td>
                    <td><span style={{ fontWeight: 700, color: 'var(--text-soft)' }}>{it.tipo_label}</span></td>
                    <td>{it.numero || '—'}</td>
                    <td>{fmtDate(it.data_viagem)}</td>
                    <td>{it.rota || '—'}</td>
                    <td>{it.remetente_ou_origem || '—'}</td>
                    <td>{it.descricao}</td>
                    <td className="money">{formatMoney(it.valor_total)}</td>
                    <td className="money">{formatMoney(it.valor_pago)}</td>
                    <td className="money" style={{ color: devedor ? 'var(--danger)' : 'var(--success)', fontWeight: 700 }}>
                      {formatMoney(it.saldo_devedor)}
                    </td>
                    <td>
                      <span className={`badge ${devedor ? 'danger' : 'success'}`}>{it.status}</span>
                    </td>
                    <td>
                      {devedor && (
                        <button className="btn-sm primary" onClick={() => abrirModalItem(it)}>
                          Baixa
                        </button>
                      )}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>

        {itens.length > 0 && (
          <div style={{ marginTop: 16, display: 'flex', gap: 10, justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap' }}>
            {selecionados.size > 0 ? (
              <div style={{ fontSize: '0.82rem', color: 'var(--text-soft)' }}>
                <strong>{selecionados.size}</strong> selecionado(s) · Total: <strong style={{ color: 'var(--danger)' }}>{formatMoney(totalSelecionado)}</strong>
              </div>
            ) : <div />}
            <div style={{ display: 'flex', gap: 10 }}>
              {selecionados.size > 0 && (
                <button style={{ ...S.btn, background: 'var(--primary)', color: '#fff', borderColor: 'var(--primary)' }}
                  onClick={quitarSelecionados} disabled={loading}>
                  ✓ Dar Baixa nos Selecionados ({selecionados.size})
                </button>
              )}
              {totais.saldo > 0.01 && (
                <button style={{ ...S.btn, background: 'var(--warning)', color: '#fff', borderColor: 'var(--warning)' }}
                  onClick={quitarTudo} disabled={loading}>
                  💰 Quitar Tudo em Aberto ({formatMoney(totais.saldo)})
                </button>
              )}
            </div>
          </div>
        )}
      </div>

      {modalPagar && (
        <div className="modal-overlay" onClick={() => !salvando && setModalPagar(null)}>
          <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 520 }}>
            <h2>{modalPagar.modo === 'lote' ? `Baixa em Lote — ${modalPagar.label}` : 'Dar Baixa'}</h2>

            {modalPagar.modo === 'item' ? (
              <div style={{ fontSize: '0.88rem', marginBottom: 12 }}>
                <div><strong>{modalPagar.item.tipo_label} #{modalPagar.item.numero}</strong></div>
                <div style={{ color: 'var(--text-muted)', fontSize: '0.78rem' }}>{modalPagar.item.descricao}</div>
              </div>
            ) : (
              <div style={{ fontSize: '0.85rem', marginBottom: 12, color: 'var(--text-soft)' }}>
                <strong>{modalPagar.itens.length}</strong> lançamento(s) · Total: <strong style={{ color: 'var(--danger)' }}>{formatMoney(modalPagar.total)}</strong>
              </div>
            )}

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginBottom: 12 }}>
              {modalPagar.modo === 'item' ? (
                <>
                  <div>
                    <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>TOTAL</div>
                    <div style={{ fontWeight: 700 }}>{formatMoney(modalPagar.item.valor_total)}</div>
                  </div>
                  <div>
                    <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>SALDO DEVEDOR</div>
                    <div style={{ fontWeight: 700, color: 'var(--danger)' }}>{formatMoney(modalPagar.item.saldo_devedor)}</div>
                  </div>
                </>
              ) : null}
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: modalPagar.modo === 'item' ? '1fr 1fr' : '1fr 1fr', gap: 10, marginBottom: 10 }}>
              {modalPagar.modo === 'item' && (
                <div>
                  <label style={S.label}>Valor a pagar</label>
                  <input style={S.input} autoFocus value={valorPagar} onChange={e => setValorPagar(e.target.value)} placeholder="0,00" />
                </div>
              )}
              <div>
                <label style={S.label}>Forma de Pagamento</label>
                <select style={S.input} value={formaPag} onChange={e => setFormaPag(e.target.value)}>
                  {FORMAS_PAGAMENTO.map(f => <option key={f} value={f}>{f}</option>)}
                </select>
              </div>
              <div>
                <label style={S.label}>Data do Pagamento</label>
                <input style={S.input} type="date" value={dataPag} onChange={e => setDataPag(e.target.value)} />
              </div>
            </div>

            <div style={{ marginBottom: 10 }}>
              <label style={S.label}>Observação</label>
              <textarea style={{ ...S.input, minHeight: 60, resize: 'vertical', fontFamily: 'inherit' }}
                value={obsPag} onChange={e => setObsPag(e.target.value)}
                placeholder="(opcional) — número do PIX, banco, recibo, etc."
              />
            </div>

            <div className="modal-actions">
              <button className="btn-secondary" onClick={() => setModalPagar(null)} disabled={salvando}>Cancelar</button>
              <button className="btn-sm primary" onClick={confirmarBaixa} disabled={salvando}>
                {salvando ? 'Salvando...' : 'Confirmar e Imprimir Comprovante'}
              </button>
            </div>
          </div>
        </div>
      )}

      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}
    </div>
  )
}

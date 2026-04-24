import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'
import { printContent } from '../utils/print.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}
function fmtDate(d) {
  if (!d) return '\u2014'
  // Se ja vem formatado DD/MM/YYYY, retorna direto
  if (typeof d === 'string' && /^\d{2}\/\d{2}\/\d{4}$/.test(d)) return d
  const dt = new Date(d)
  return isNaN(dt) ? String(d) : dt.toLocaleDateString('pt-BR')
}

export default function RelatorioFretes({ viagemAtiva }) {
  const [viagens, setViagens] = useState([])
  const [viagemId, setViagemId] = useState('')
  const [rotas, setRotas] = useState([])
  const [rotaSel, setRotaSel] = useState('')
  const [clientes, setClientes] = useState([])
  const [clienteSel, setClienteSel] = useState('')
  const [devedores, setDevedores] = useState([])
  const [devedorSel, setDevedorSel] = useState('')

  const [itensRelatorio, setItensRelatorio] = useState([])
  const [financeiro, setFinanceiro] = useState([])
  const [loading, setLoading] = useState(false)
  const [toast, setToast] = useState(null)

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  // Carregar viagens e rotas
  useEffect(() => {
    api.get('/viagens').then(setViagens).catch(() => {})
    api.get('/rotas').then(setRotas).catch(() => {})
  }, [])

  // Auto-selecionar viagem ativa
  useEffect(() => {
    if (viagemAtiva && !viagemId) setViagemId(String(viagemAtiva.id_viagem))
  }, [viagemAtiva])

  // Carregar clientes e devedores quando muda viagem
  useEffect(() => {
    if (!viagemId) { setClientes([]); setDevedores([]); return }
    api.get(`/fretes?viagem_id=${viagemId}`).then(fretes => {
      const nomes = [...new Set(fretes.map(f => f.destinatario || f.destinatario_nome_temp).filter(Boolean))].sort()
      setClientes(nomes)
      const devs = [...new Set(fretes.filter(f => (f.valor_devedor || 0) > 0.01).map(f => f.destinatario || f.destinatario_nome_temp).filter(Boolean))].sort()
      setDevedores(devs)
    }).catch(() => {})
    setClienteSel('')
    setDevedorSel('')
  }, [viagemId])

  // Quando seleciona devedor, auto-preencher cliente
  useEffect(() => {
    if (devedorSel) setClienteSel(devedorSel)
  }, [devedorSel])

  // Carregar dados do relatorio
  const carregar = useCallback(() => {
    if (!viagemId) return
    setLoading(true)
    const params = new URLSearchParams({ viagem_id: viagemId })
    if (clienteSel) params.append('cliente', clienteSel)
    if (rotaSel) params.append('rota', rotaSel)

    Promise.all([
      api.get(`/fretes/relatorio/itens?${params}`),
      api.get(`/fretes/relatorio/financeiro?${params}`)
    ]).then(([itens, fin]) => {
      setItensRelatorio(Array.isArray(itens) ? itens : [])
      setFinanceiro(Array.isArray(fin) ? fin : [])
    }).catch(() => showToast('Erro ao carregar relatorio', 'error'))
      .finally(() => setLoading(false))
  }, [viagemId, clienteSel, rotaSel])

  useEffect(() => { carregar() }, [carregar])

  // Totais
  const totalItens = itensRelatorio.reduce((s, i) => s + (parseFloat(i.total_item) || 0), 0)
  const totalFinanceiro = financeiro.reduce((s, f) => s + (parseFloat(f.valor_total_itens) || 0), 0)
  const totalPago = financeiro.reduce((s, f) => s + (parseFloat(f.valor_pago) || 0), 0)
  const totalDevedor = Math.max(0, totalFinanceiro - totalPago)

  // Viagem selecionada
  const viagemSel = viagens.find(v => String(v.id_viagem) === viagemId)

  // ====== IMPRESSAO ======
  const baseStyle = `
    body { font-family: Arial, sans-serif; margin: 0; padding: 10px; color: #0F2620; }
    table { width: 100%; border-collapse: collapse; margin: 0; }
    /* Cabecalho da tabela: sem fundo pintado, bordas verdes + texto escuro bold */
    th {
      background: #fff;
      color: #047857;
      padding: 8px 10px;
      text-align: left;
      font-size: 11px;
      font-weight: 700;
      letter-spacing: 0.03em;
      border-top: 2px solid #047857;
      border-bottom: 2px solid #047857;
    }
    td { padding: 6px 10px; border-bottom: 1px solid #E5E7EB; font-size: 11px; }
    .money { text-align: right; font-family: 'Courier New', monospace; }
    .header { text-align: center; margin-bottom: 14px; padding-bottom: 10px; border-bottom: 2px solid #047857; }
    .header h2 { margin: 0; color: #047857; font-size: 17px; letter-spacing: 0.02em; }
    .header p { margin: 3px 0 0; font-size: 11px; color: #3D6B56; }
    .bold { font-weight: 700; }
    .green { color: #047857; font-weight: 700; }
    .red { color: #DC2626; font-weight: 700; }
    .signature { margin-top: 30px; border-top: 1px solid #333; width: 200px; text-align: center; font-size: 10px; padding-top: 4px; }
    /* Cabecalho de cada frete: sem fundo pintado — barra lateral + borda verde */
    .frete-header {
      background: #fff;
      color: #047857;
      padding: 8px 12px;
      margin-top: 14px;
      border-left: 4px solid #047857;
      border-top: 1px solid #047857;
      border-right: 1px solid #047857;
      border-radius: 0 4px 0 0;
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-weight: 700;
    }
    .frete-footer {
      background: transparent;
      padding: 8px 12px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 11px;
      margin-bottom: 6px;
      border-left: 4px solid #047857;
      border-right: 1px solid #047857;
      border-bottom: 1px solid #047857;
    }
    /* Resumo financeiro: borda verde em vez de fundo cinza */
    .summary {
      margin-top: 14px;
      padding: 10px 12px;
      background: transparent;
      border: 1px solid #047857;
      border-left: 4px solid #047857;
      border-radius: 0 4px 4px 0;
    }
    .summary div { display: flex; justify-content: space-between; padding: 3px 0; font-size: 12px; }
    /* Impressao: forca que o navegador imprima as cores (border verde, texto verde) */
    @media print {
      body { margin: 0; }
      * { -webkit-print-color-adjust: exact !important; print-color-adjust: exact !important; }
    }
  `

  function printRelatorio() {
    if (!clienteSel) { showToast('Selecione um cliente', 'error'); return }
    const fretesCliente = financeiro
    const itensCliente = itensRelatorio
    const volumes = itensCliente.reduce((s, i) => s + (parseInt(i.quantidade) || 0), 0)
    const html = `<!DOCTYPE html><html><head><title>Recibo de Frete</title><style>${baseStyle}
      body { width: 270px; font-family: 'Courier New', monospace; font-size: 10px; }
      th, td { font-size: 9px; padding: 3px 4px; }
      th { background: #333; }
    </style></head><body>
      <div class="header"><h2>RECIBO DE FRETE</h2></div>
      <div><strong>Dest:</strong> ${clienteSel}</div>
      <div><strong>Viagem:</strong> ${fmtDate(viagemSel?.data_viagem)}</div>
      <div><strong>Rota:</strong> ${rotaSel || 'Todas'}</div>
      <hr/>
      <table><thead><tr><th>QTD</th><th>DESCRICAO</th></tr></thead>
      <tbody>${itensCliente.map(i => `<tr><td>${i.quantidade}</td><td>${i.item}</td></tr>`).join('')}</tbody></table>
      <div class="bold" style="margin-top:8px">VOLUMES: ${volumes}</div>
      <hr/><div class="signature">Assinatura</div>
      <div style="text-align:center;font-size:8px;margin-top:8px">${new Date().toLocaleString('pt-BR')}</div>
      <script>window.onload=()=>window.print()</script>
    </body></html>`
    printContent(html, 'Recibo de Frete')
  }

  function printCobranca() {
    if (!clienteSel) { showToast('Selecione um cliente', 'error'); return }
    const volumes = itensRelatorio.reduce((s, i) => s + (parseInt(i.quantidade) || 0), 0)
    const status = totalDevedor <= 0.01 ? 'QUITADO' : 'PENDENTE'
    const html = `<!DOCTYPE html><html><head><title>Cobranca Frete</title><style>${baseStyle}
      body { width: 270px; font-family: 'Courier New', monospace; font-size: 10px; }
      th, td { font-size: 9px; padding: 3px 4px; }
      th { background: #333; }
    </style></head><body>
      <div class="header"><h2>COBRANCA DE FRETE</h2></div>
      <div><strong>Dest:</strong> ${clienteSel}</div>
      <div><strong>Viagem:</strong> ${fmtDate(viagemSel?.data_viagem)}</div>
      <div><strong>Rota:</strong> ${rotaSel || 'Todas'}</div>
      <hr/>
      <table><thead><tr><th>QTD</th><th>DESC</th><th>V.UN</th><th>TOTAL</th></tr></thead>
      <tbody>${itensRelatorio.map(i => `<tr><td>${i.quantidade}</td><td>${i.item}</td><td class="money">${formatMoney(i.preco_unitario)}</td><td class="money">${formatMoney(i.total_item)}</td></tr>`).join('')}</tbody></table>
      <div class="bold">VOLUMES: ${volumes}</div>
      <hr/>
      <div class="summary">
        <div><span>TOTAL:</span><span class="bold">${formatMoney(totalFinanceiro)}</span></div>
        <div><span>PAGO:</span><span class="bold green">${formatMoney(totalPago)}</span></div>
        <div><span>STATUS:</span><span class="bold ${status === 'QUITADO' ? 'green' : 'red'}">${status}</span></div>
      </div>
      <hr/><div class="signature">Assinatura</div>
      <div style="text-align:center;font-size:8px;margin-top:8px">${new Date().toLocaleString('pt-BR')}</div>
      <script>window.onload=()=>window.print()</script>
    </body></html>`
    printContent(html, 'Cobranca de Frete')
  }

  function printGeralA4(filtroTipo = 'tudo') {
    // Filtrar financeiro conforme tipo
    let fretesGeral = [...financeiro]
    if (filtroTipo === 'pendentes') fretesGeral = fretesGeral.filter(f => Math.max(0, (parseFloat(f.valor_total_itens) || 0) - (parseFloat(f.valor_pago) || 0)) > 0.01)
    if (filtroTipo === 'falta_pagar') fretesGeral = fretesGeral.filter(f => Math.max(0, (parseFloat(f.valor_total_itens) || 0) - (parseFloat(f.valor_pago) || 0)) > 0.01)

    const totGeral = fretesGeral.reduce((s, f) => s + (parseFloat(f.valor_total_itens) || 0), 0)
    const totPago = fretesGeral.reduce((s, f) => s + (parseFloat(f.valor_pago) || 0), 0)
    const totDevedor = Math.max(0, totGeral - totPago)

    // Agrupar itens por frete
    const itensPorFrete = {}
    itensRelatorio.forEach(i => {
      if (!itensPorFrete[i.numero_frete]) itensPorFrete[i.numero_frete] = []
      itensPorFrete[i.numero_frete].push(i)
    })

    let fretesHtml = ''
    for (const fr of fretesGeral) {
      const devedor = Math.max(0, (parseFloat(fr.valor_total_itens) || 0) - (parseFloat(fr.valor_pago) || 0))
      const statusBadge = devedor <= 0.01
        ? '<span style="border:1.5px solid #059669;color:#059669;padding:2px 10px;border-radius:4px;font-size:10px;font-weight:700;letter-spacing:0.03em">PAGO</span>'
        : '<span style="border:1.5px solid #DC2626;color:#DC2626;padding:2px 10px;border-radius:4px;font-size:10px;font-weight:700;letter-spacing:0.03em">FALTA PAGAR</span>'
      const itens = itensPorFrete[fr.numero_frete] || []

      fretesHtml += `
        <div class="frete-header">
          <div><strong style="font-size:14px">Frete #${fr.numero_frete}</strong> &nbsp; <span>${fr.remetente || ''}</span> &rarr; <span>${fr.destinatario || ''}</span></div>
          ${statusBadge}
        </div>
        <table style="margin:0;border-left:4px solid #047857;border-right:1px solid #047857">
          <thead><tr><th style="width:8%">QTD</th><th style="width:52%">DESCRICAO</th><th style="width:20%">VL UNIT</th><th style="width:20%">VL TOTAL</th></tr></thead>
          <tbody>${itens.map(i => `<tr><td>${i.quantidade}</td><td>${i.item}</td><td class="money">${formatMoney(i.preco_unitario)}</td><td class="money">${formatMoney(i.total_item)}</td></tr>`).join('')}
          ${itens.length === 0 ? '<tr><td colspan="4" style="text-align:center;color:#999">Sem itens</td></tr>' : ''}
          </tbody></table>
        <div class="frete-footer">
          <div style="border-top:1px solid #333;width:150px;text-align:center;font-size:9px;padding-top:3px">Assinatura</div>
          <div style="text-align:right">
            <span>Total: <strong>${formatMoney(fr.valor_total_itens)}</strong></span>
            ${(parseFloat(fr.valor_pago) || 0) > 0.01 ? ` &nbsp; <span class="green">Pago: <strong>${formatMoney(fr.valor_pago)}</strong></span>` : ''}
            ${devedor > 0.01 ? ` &nbsp; <span class="red">A Pagar: <strong>${formatMoney(devedor)}</strong></span>` : ''}
          </div>
        </div>`
    }

    const titulo = 'RELATORIO GERAL DE FRETES'
    const html = `<!DOCTYPE html><html><head><title>${titulo}</title><style>${baseStyle}
      @page { size: A4 portrait; margin: 15mm; }
      body { font-size: 11px; }
    </style></head><body>
      <div class="header">
        <h2 style="font-size:18px">${titulo}</h2>
        <p>Viagem: ${viagemSel?.descricao || viagemId} | Rota: ${rotaSel || 'Todas'} | ${clienteSel ? 'Cliente: ' + clienteSel : 'Todos os clientes'}</p>
        <p>Filtro: ${filtroTipo === 'tudo' ? 'Todos' : filtroTipo === 'pendentes' ? 'So Pendentes' : 'Falta Pagar'}</p>
      </div>
      ${fretesHtml}
      <div style="margin-top:20px;padding:12px;border:2px solid #047857;border-radius:6px">
        <h3 style="margin:0 0 8px;color:#047857">RESUMO FINANCEIRO GERAL</h3>
        <div style="display:flex;justify-content:space-between;font-size:13px">
          <div><strong>TOTAL LANCADO:</strong> <span style="color:#047857;font-weight:700">${formatMoney(totGeral)}</span></div>
          <div><strong>TOTAL RECEBIDO:</strong> <span class="green bold">${formatMoney(totPago)}</span></div>
          <div><strong>TOTAL A RECEBER:</strong> <span class="${totDevedor > 0.01 ? 'red' : ''} bold">${formatMoney(totDevedor)}</span></div>
        </div>
      </div>
      <div style="text-align:center;font-size:9px;margin-top:12px;color:#999">${new Date().toLocaleString('pt-BR')}</div>
      <script>window.onload=()=>window.print()</script>
    </body></html>`
    printContent(html, titulo)
  }

  function printResumido() {
    if (!clienteSel) { showToast('Selecione um cliente', 'error'); return }
    // Agrupar itens por remetente
    const porRemetente = {}
    itensRelatorio.forEach(i => {
      const rem = i.remetente || 'SEM REMETENTE'
      if (!porRemetente[rem]) porRemetente[rem] = []
      porRemetente[rem].push(i)
    })
    const volumes = itensRelatorio.reduce((s, i) => s + (parseInt(i.quantidade) || 0), 0)
    const status = totalDevedor <= 0.01 ? 'QUITADO' : 'PENDENTE'

    let body = ''
    for (const [rem, itens] of Object.entries(porRemetente)) {
      const sub = itens.reduce((s, i) => s + (parseFloat(i.total_item) || 0), 0)
      const vol = itens.reduce((s, i) => s + (parseInt(i.quantidade) || 0), 0)
      body += `<div style="padding:4px 8px;font-weight:700;margin-top:8px;color:#047857;border-bottom:1.5px solid #047857">${rem}</div>`
      body += itens.map(i => `<div style="padding:2px 8px;font-size:9px">&nbsp; ${i.quantidade}x ${i.item} <span style="float:right">${formatMoney(i.total_item)}</span></div>`).join('')
      body += `<div style="padding:2px 8px;font-size:9px;font-weight:700">&nbsp; Subtotal: ${formatMoney(sub)} (${vol} vol)</div><hr style="margin:4px 0"/>`
    }

    const html = `<!DOCTYPE html><html><head><title>Resumido</title><style>${baseStyle}
      body { width: 270px; font-family: 'Courier New', monospace; font-size: 10px; }
    </style></head><body>
      <div class="header"><h2 style="font-size:13px">RESUMIDO DE FRETE</h2></div>
      <div><strong>Dest:</strong> ${clienteSel}</div>
      <div><strong>Viagem:</strong> ${fmtDate(viagemSel?.data_viagem)}</div>
      <hr/>${body}
      <div class="bold" style="margin-top:6px">VOLUMES: ${volumes}</div>
      <div class="summary">
        <div><span>TOTAL:</span><span class="bold">${formatMoney(totalFinanceiro)}</span></div>
        <div><span>PAGO:</span><span class="bold green">${formatMoney(totalPago)}</span></div>
        <div><span>STATUS:</span><span class="bold ${status === 'QUITADO' ? 'green' : 'red'}">${status}</span></div>
      </div>
      <hr/><div class="signature">Assinatura</div>
      <div style="text-align:center;font-size:8px;margin-top:8px">${new Date().toLocaleString('pt-BR')}</div>
      <script>window.onload=()=>window.print()</script>
    </body></html>`
    printContent(html, 'Resumido de Frete')
  }

  function printConfereViagem() {
    // Agrupar por DESTINATARIO, dentro por nota (numero_frete).
    // Assim o conferente recebe tudo junto do mesmo cliente,
    // mas ainda confere nota por nota.
    const porDestinatario = {}
    itensRelatorio.forEach(i => {
      const dest = i.destinatario || 'SEM DESTINATARIO'
      if (!porDestinatario[dest]) porDestinatario[dest] = {}
      const nota = String(i.numero_frete)
      if (!porDestinatario[dest][nota]) {
        porDestinatario[dest][nota] = {
          remetente: i.remetente || '-',
          local: i.local_transporte || '-',
          itens: []
        }
      }
      porDestinatario[dest][nota].itens.push(i)
    })

    let html = ''
    for (const dest of Object.keys(porDestinatario).sort()) {
      const fretesDoDest = porDestinatario[dest]
      const totalVolumesDest = Object.values(fretesDoDest)
        .reduce((s, f) => s + f.itens.reduce((a, i) => a + (parseInt(i.quantidade) || 0), 0), 0)
      const numNotas = Object.keys(fretesDoDest).length

      html += `
        <div class="dest-header">
          <span class="dest-name">${dest}</span>
          <span class="dest-meta">${numNotas} nota${numNotas > 1 ? 's' : ''} · ${totalVolumesDest} volume${totalVolumesDest > 1 ? 's' : ''}</span>
        </div>`

      for (const numero of Object.keys(fretesDoDest).sort((a, b) => Number(a) - Number(b))) {
        const dados = fretesDoDest[numero]
        const vol = dados.itens.reduce((s, i) => s + (parseInt(i.quantidade) || 0), 0)
        html += `
          <div class="nota-header">
            <strong>Nota #${numero}</strong>
            <span style="margin-left:12px">REM: <span class="green">${dados.remetente}</span></span>
          </div>
          <table style="margin:0;border-left:4px solid #047857;border-right:1px solid #047857">
            <thead><tr>
              <th style="width:10%">QTD</th>
              <th style="width:65%">DESCRICAO</th>
              <th style="width:25%">LOCAL</th>
            </tr></thead>
            <tbody>${dados.itens.map(i => `<tr>
              <td style="text-align:center">${i.quantidade}</td>
              <td>${i.item}</td>
              <td>${dados.local}</td>
            </tr>`).join('')}
            </tbody>
          </table>
          <div class="nota-footer">
            <div style="font-size:10px;color:#3D6B56">Volumes desta nota: <strong>${vol}</strong></div>
          </div>`
      }
    }

    const volumesTotal = itensRelatorio.reduce((s, i) => s + (parseInt(i.quantidade) || 0), 0)
    const titulo = 'CONFERE DE VIAGEM'
    const full = `<!DOCTYPE html><html><head><title>${titulo}</title><style>${baseStyle}
      @page { size: A4 portrait; margin: 15mm; }
      body { font-size: 11px; }
      /* Cabecalho do destinatario: faixa superior verde destacada */
      .dest-header {
        margin-top: 18px;
        padding: 10px 14px 8px;
        border-left: 5px solid #047857;
        border-top: 2px solid #047857;
        border-bottom: 2px solid #047857;
        display: flex;
        justify-content: space-between;
        align-items: baseline;
      }
      .dest-header .dest-name {
        color: #047857;
        font-size: 14px;
        font-weight: 800;
        letter-spacing: 0.02em;
        text-transform: uppercase;
      }
      .dest-header .dest-meta {
        color: #3D6B56;
        font-size: 10px;
        font-weight: 600;
      }
      /* Cabecalho de cada nota dentro do destinatario */
      .nota-header {
        background: #fff;
        color: #0F2620;
        padding: 7px 12px;
        margin-top: 10px;
        border-left: 4px solid #047857;
        border-top: 1px solid #047857;
        border-right: 1px solid #047857;
        font-size: 12px;
      }
      .nota-footer {
        padding: 8px 12px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-size: 11px;
        margin-bottom: 4px;
        border-left: 4px solid #047857;
        border-right: 1px solid #047857;
        border-bottom: 1px solid #047857;
      }
    </style></head><body>
      <div class="header">
        <h2 style="font-size:18px">${titulo}</h2>
        <p>Viagem: ${viagemSel?.descricao || viagemId} | Rota: ${rotaSel || 'Todas'} | ${clienteSel ? 'Cliente: ' + clienteSel : 'Todos os clientes'}</p>
      </div>
      ${html || '<p style="text-align:center;color:#999">Sem itens para conferir</p>'}
      <div style="margin-top:24px;padding:14px 16px;border:2px solid #047857;border-radius:4px">
        <div style="display:flex;justify-content:space-between;align-items:center;padding-bottom:6px;border-bottom:1px dashed #047857">
          <strong style="font-size:13px;color:#047857;text-transform:uppercase;letter-spacing:0.04em">Total geral da viagem</strong>
          <strong style="font-size:16px;color:#047857">${volumesTotal} volumes</strong>
        </div>
        <div style="margin-top:40px;display:flex;justify-content:center">
          <div style="text-align:center;width:80%">
            <div style="border-bottom:1px solid #0F2620;height:1px;margin-bottom:4px"></div>
            <div style="font-size:11px;color:#3D6B56;letter-spacing:0.04em;text-transform:uppercase;font-weight:700">Conferido por</div>
            <div style="font-size:9px;color:#7BA393;margin-top:2px">Assinatura e data</div>
          </div>
        </div>
      </div>
      <div style="text-align:center;font-size:9px;margin-top:10px;color:#999">${new Date().toLocaleString('pt-BR')}</div>
      <script>window.onload=()=>window.print()</script>
    </body></html>`
    printContent(full, titulo)
  }

  function printExtrato() {
    if (!clienteSel) { showToast('Selecione um cliente', 'error'); return }
    const status = totalDevedor <= 0.01 ? 'QUITADO' : 'PENDENTE'
    const html = `<!DOCTYPE html><html><head><title>Extrato</title><style>${baseStyle}
      body { width: 270px; font-family: 'Courier New', monospace; font-size: 10px; }
      th, td { font-size: 9px; padding: 3px 4px; }
      th { background: #333; }
    </style></head><body>
      <div class="header"><h2 style="font-size:13px">EXTRATO DE FRETES</h2></div>
      <div><strong>CLIENTE:</strong> ${clienteSel}</div>
      <div><strong>VIAGEM:</strong> ${fmtDate(viagemSel?.data_viagem)}</div>
      <hr/>
      <table><thead><tr><th>FRETE</th><th>TOTAL</th><th>PAGO</th><th>SALDO</th></tr></thead>
      <tbody>${financeiro.map(f => {
        const dev = Math.max(0, (parseFloat(f.valor_total_itens) || 0) - (parseFloat(f.valor_pago) || 0))
        return `<tr><td>${f.numero_frete}</td><td class="money">${formatMoney(f.valor_total_itens)}</td><td class="money">${formatMoney(f.valor_pago)}</td><td class="money">${formatMoney(dev)}</td></tr>`
      }).join('')}</tbody></table>
      <div class="summary">
        <div><span>TOTAL:</span><span class="bold">${formatMoney(totalFinanceiro)}</span></div>
        <div><span>PAGO:</span><span class="bold green">${formatMoney(totalPago)}</span></div>
        <div><span>STATUS:</span><span class="bold ${status === 'QUITADO' ? 'green' : 'red'}">${status}</span></div>
      </div>
      <hr/><div class="signature">Assinatura</div>
      <div style="text-align:center;font-size:8px;margin-top:8px">${new Date().toLocaleString('pt-BR')}</div>
      <script>window.onload=()=>window.print()</script>
    </body></html>`
    printContent(html, 'Extrato de Fretes')
  }

  // ====== DIALOG GERAL A4 ======
  const [showGeralDialog, setShowGeralDialog] = useState(false)

  // ====== STYLES ======
  const S = {
    container: { display: 'flex', gap: 16, minHeight: 'calc(100vh - 120px)' },
    sidebar: { width: 240, flexShrink: 0, background: 'var(--bg-card)', borderRadius: 8, padding: 14, border: '1px solid var(--border)' },
    main: { flex: 1, minWidth: 0 },
    label: { fontSize: '0.75rem', fontWeight: 700, color: 'var(--text)', display: 'block', marginBottom: 3, marginTop: 10 },
    select: { width: '100%', padding: '7px 8px', fontSize: '0.8rem', background: 'var(--primary)', color: '#fff', border: 'none', borderRadius: 4, fontWeight: 600, cursor: 'pointer' },
    selectAlt: { width: '100%', padding: '7px 8px', fontSize: '0.8rem', background: 'var(--bg-soft)', color: 'var(--text)', border: '1px solid var(--border)', borderRadius: 4 },
    btn: { width: '100%', padding: '8px 12px', border: 'none', borderRadius: 4, fontWeight: 700, cursor: 'pointer', fontSize: '0.8rem', marginTop: 6, display: 'flex', alignItems: 'center', gap: 6 },
  }

  if (!viagemAtiva && !viagemId) {
    return <div className="placeholder-page"><div className="ph-icon">📊</div><h2>Relatorio de Fretes</h2><p>Selecione uma viagem.</p></div>
  }

  return (
    <div>
      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}

      <h2 style={{ textAlign: 'center', color: 'var(--primary)', margin: '0 0 16px', fontSize: '1.2rem', fontWeight: 700 }}>
        CENTRAL DE RELATORIOS DE FRETES
      </h2>

      <div style={S.container}>
        {/* SIDEBAR FILTROS */}
        <div style={S.sidebar}>
          <h4 style={{ margin: '0 0 4px', fontSize: '0.85rem' }}>FILTROS</h4>

          <label style={S.label}>Viagem:</label>
          <select style={S.select} value={viagemId} onChange={e => setViagemId(e.target.value)}>
            <option value="">Selecione</option>
            {viagens.map(v => (
              <option key={v.id_viagem} value={v.id_viagem}>
                {v.id_viagem} - {fmtDate(v.data_viagem)} ({v.nome_rota || v.descricao || ''}){v.ativa ? ' (ATIVA)' : ''}
              </option>
            ))}
          </select>

          <label style={S.label}>Rota:</label>
          <select style={S.select} value={rotaSel} onChange={e => setRotaSel(e.target.value)}>
            <option value="">Todas as Rotas</option>
            {rotas.map(r => (
              <option key={r.id_rota} value={`${r.origem} - ${r.destino}`}>{r.origem} - {r.destino}</option>
            ))}
          </select>

          <label style={S.label}>Cliente (Destinatario):</label>
          <select style={S.select} value={clienteSel} onChange={e => setClienteSel(e.target.value)}>
            <option value="">Todos os Clientes</option>
            {clientes.map(c => <option key={c} value={c}>{c}</option>)}
          </select>

          <label style={S.label}>Devedores da Viagem:</label>
          <select style={S.select} value={devedorSel} onChange={e => setDevedorSel(e.target.value)}>
            <option value="">Todos</option>
            {devedores.map(d => <option key={d} value={d}>{d}</option>)}
          </select>

          <h4 style={{ margin: '18px 0 4px', fontSize: '0.85rem' }}>OPCOES DE IMPRESSAO</h4>

          <button style={{ ...S.btn, background: '#047857', color: '#fff' }} onClick={printRelatorio}>
            Relatorio
          </button>
          <button style={{ ...S.btn, background: '#047857', color: '#fff' }} onClick={printCobranca}>
            Cobranca
          </button>
          <button style={{ ...S.btn, background: '#047857', color: '#fff' }} onClick={() => setShowGeralDialog(true)}>
            Geral (A4)
          </button>
          <button style={{ ...S.btn, background: '#047857', color: '#fff' }} onClick={printResumido}>
            Resumido
          </button>
          <button style={{ ...S.btn, background: '#047857', color: '#fff' }} onClick={printConfereViagem}>
            Confere Viagem
          </button>
          <button style={{ ...S.btn, background: '#047857', color: '#fff' }} onClick={printExtrato}>
            Extrato
          </button>
        </div>

        {/* MAIN CONTENT */}
        <div style={S.main}>
          {loading ? (
            <div style={{ padding: 40, textAlign: 'center', color: 'var(--text-muted)' }}>Carregando...</div>
          ) : (
            <>
              {/* TABELA FRETES DO CLIENTE */}
              <div className="card" style={{ marginBottom: 12, padding: 0 }}>
                <h3 style={{ padding: '10px 14px', margin: 0, fontSize: '0.9rem', borderBottom: '1px solid var(--border)' }}>
                  FRETES {clienteSel ? `DO CLIENTE: ${clienteSel}` : 'DA VIAGEM'}
                </h3>
                <div className="table-container" style={{ maxHeight: 350, overflow: 'auto' }}>
                  <table>
                    <thead><tr>
                      <th style={{ width: 80 }}>Cod. Frete</th>
                      <th style={{ width: 90 }}>Viagem</th>
                      <th>Remetente</th>
                      <th>Item</th>
                      <th style={{ width: 60 }}>Quant.</th>
                      <th style={{ width: 90 }}>Preco</th>
                      <th style={{ width: 100 }}>Total</th>
                    </tr></thead>
                    <tbody>
                      {itensRelatorio.length === 0 ? (
                        <tr><td colSpan={7} style={{ textAlign: 'center', padding: 30, color: 'var(--text-muted)' }}>Nenhum registro</td></tr>
                      ) : itensRelatorio.map((i, idx) => (
                        <tr key={idx}>
                          <td>{i.numero_frete}</td>
                          <td>{fmtDate(i.data_viagem)}</td>
                          <td>{i.remetente || '\u2014'}</td>
                          <td>{i.item || '\u2014'}</td>
                          <td style={{ textAlign: 'center' }}>{i.quantidade}</td>
                          <td className="money">{formatMoney(i.preco_unitario)}</td>
                          <td className="money" style={{ fontWeight: 700 }}>{formatMoney(i.total_item)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                {itensRelatorio.length > 0 && (
                  <div style={{ display: 'flex', justifyContent: 'flex-end', padding: '8px 14px', borderTop: '2px solid var(--primary)' }}>
                    <span style={{ fontSize: '0.85rem' }}>Total Fretes por item: </span>
                    <span style={{ fontWeight: 700, fontSize: '1rem', color: 'var(--primary)', fontFamily: 'Space Mono, monospace', marginLeft: 8 }}>{formatMoney(totalItens)}</span>
                  </div>
                )}
              </div>

              {/* SITUACAO FINANCEIRA */}
              <div className="card" style={{ padding: 0 }}>
                <h3 style={{ padding: '10px 14px', margin: 0, fontSize: '0.9rem', borderBottom: '1px solid var(--border)' }}>
                  SITUACAO FINANCEIRA:
                </h3>
                <div className="table-container">
                  <table>
                    <thead><tr>
                      <th style={{ width: 120 }}>Total</th>
                      <th style={{ width: 120 }}>Baixado</th>
                      <th style={{ width: 120 }}>Devedor</th>
                      <th style={{ width: 100 }}>N° Frete</th>
                    </tr></thead>
                    <tbody>
                      {financeiro.length === 0 ? (
                        <tr><td colSpan={4} style={{ textAlign: 'center', padding: 20, color: 'var(--text-muted)' }}>Nenhum registro</td></tr>
                      ) : financeiro.map((f, idx) => {
                        const dev = Math.max(0, (parseFloat(f.valor_total_itens) || 0) - (parseFloat(f.valor_pago) || 0))
                        return (
                          <tr key={idx}>
                            <td className="money">{formatMoney(f.valor_total_itens)}</td>
                            <td className="money">{formatMoney(f.valor_pago)}</td>
                            <td className="money" style={{ color: dev > 0.01 ? '#DC2626' : 'inherit', fontWeight: dev > 0.01 ? 700 : 400 }}>{formatMoney(dev)}</td>
                            <td>{f.numero_frete}</td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                </div>
                {financeiro.length > 0 && (
                  <div style={{ display: 'flex', justifyContent: 'space-between', padding: '10px 14px', borderTop: '2px solid var(--primary)', alignItems: 'center' }}>
                    <div style={{ fontSize: '0.82rem' }}>
                      <span>Total: <strong>{formatMoney(totalFinanceiro)}</strong></span>
                      <span style={{ marginLeft: 16, color: '#059669' }}>Pago: <strong>{formatMoney(totalPago)}</strong></span>
                    </div>
                    <div style={{ fontWeight: 700, fontSize: '1.1rem', color: totalDevedor > 0.01 ? '#DC2626' : '#059669', fontFamily: 'Space Mono, monospace' }}>
                      Em Aberto: {formatMoney(totalDevedor)}
                    </div>
                  </div>
                )}
              </div>
            </>
          )}
        </div>
      </div>

      {/* DIALOG GERAL A4 */}
      {showGeralDialog && (
        <div className="modal-overlay" onClick={() => setShowGeralDialog(false)}>
          <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 350 }}>
            <h3>Relatorio Geral A4</h3>
            <p style={{ color: 'var(--text-muted)', marginBottom: 12 }}>Selecione o filtro:</p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              <button className="btn-sm primary" style={{ padding: 10 }} onClick={() => { setShowGeralDialog(false); printGeralA4('tudo') }}>Tudo</button>
              <button className="btn-sm primary" style={{ padding: 10 }} onClick={() => { setShowGeralDialog(false); printGeralA4('pendentes') }}>So Pendentes</button>
              <button className="btn-sm primary" style={{ padding: 10 }} onClick={() => { setShowGeralDialog(false); printGeralA4('falta_pagar') }}>Falta Pagar</button>
              <button className="btn-sm" style={{ padding: 10 }} onClick={() => setShowGeralDialog(false)}>Cancelar</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

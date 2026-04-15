/**
 * Naviera Web — Print Utilities
 * Generates printable HTML for bilhetes, recibos, notas, etiquetas, and listas.
 */

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}

function formatDate(dateStr) {
  if (!dateStr) return '\u2014'
  try {
    return new Date(dateStr).toLocaleDateString('pt-BR', {
      day: '2-digit', month: '2-digit', year: 'numeric'
    })
  } catch {
    return dateStr
  }
}

function formatDateTime(dateStr) {
  if (!dateStr) return '\u2014'
  try {
    return new Date(dateStr).toLocaleString('pt-BR', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    })
  } catch {
    return dateStr
  }
}

/** Common base styles used in all print documents */
const BASE_STYLES = `
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body {
    font-family: 'Segoe UI', Arial, sans-serif;
    color: #111;
    line-height: 1.4;
  }
  .header {
    text-align: center;
    border-bottom: 2px solid #059669;
    padding-bottom: 10px;
    margin-bottom: 14px;
  }
  .header h1 {
    font-size: 18px;
    font-weight: 700;
    color: #059669;
    letter-spacing: 1px;
  }
  .header .subtitle {
    font-size: 11px;
    color: #666;
    margin-top: 2px;
  }
  .header .doc-title {
    font-size: 13px;
    font-weight: 600;
    color: #333;
    margin-top: 6px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }
  .info-row {
    display: flex;
    justify-content: space-between;
    margin-bottom: 4px;
    font-size: 12px;
  }
  .info-row .label {
    font-weight: 600;
    color: #444;
    min-width: 120px;
  }
  .info-row .value {
    text-align: right;
    flex: 1;
  }
  .section {
    margin-bottom: 12px;
  }
  .section-title {
    font-size: 12px;
    font-weight: 700;
    color: #059669;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    border-bottom: 1px solid #ddd;
    padding-bottom: 3px;
    margin-bottom: 6px;
  }
  .divider {
    border: none;
    border-top: 1px dashed #ccc;
    margin: 10px 0;
  }
  .footer {
    text-align: center;
    font-size: 10px;
    color: #888;
    margin-top: 16px;
    padding-top: 8px;
    border-top: 1px solid #ddd;
  }
  table {
    width: 100%;
    border-collapse: collapse;
    font-size: 11px;
  }
  table th {
    background: #f5f5f5;
    border: 1px solid #ccc;
    padding: 5px 8px;
    text-align: left;
    font-size: 10px;
    font-weight: 600;
    text-transform: uppercase;
    color: #555;
  }
  table td {
    border: 1px solid #ddd;
    padding: 4px 8px;
  }
  .qr-placeholder {
    text-align: center;
    margin: 10px 0;
    padding: 12px;
    border: 1px dashed #ccc;
    font-size: 10px;
    color: #999;
  }
  .total-row {
    font-weight: 700;
    font-size: 14px;
    text-align: right;
    margin-top: 8px;
  }
`

/** Thermal-specific styles (80mm width) */
const THERMAL_STYLES = `
  body { width: 72mm; max-width: 72mm; font-size: 11px; }
  .header h1 { font-size: 14px; }
  .header .subtitle { font-size: 9px; }
  .header .doc-title { font-size: 11px; }
  .info-row { font-size: 11px; }
  .info-row .label { min-width: 80px; }
  table { font-size: 10px; }
  table th { font-size: 9px; padding: 3px 4px; }
  table td { padding: 3px 4px; }
  .footer { font-size: 9px; }
  @page { size: 80mm auto; margin: 2mm; }
`

/** A4-specific styles */
const A4_STYLES = `
  body { width: 100%; max-width: 210mm; margin: 0 auto; padding: 15mm; }
  @page { size: A4; margin: 15mm; }
`

function buildHeader(docTitle) {
  return `
    <div class="header">
      <h1>NAVIERA</h1>
      <div class="subtitle">Sistema de Gestao Fluvial</div>
      <div class="doc-title">${docTitle}</div>
    </div>
  `
}

function buildFooter() {
  const now = new Date().toLocaleString('pt-BR')
  return `
    <div class="footer">
      Documento gerado pelo sistema Naviera em ${now}<br>
      Este documento nao tem valor fiscal sem autenticacao.
    </div>
  `
}

function buildPage(content, title, thermal = false) {
  return `<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <title>${title}</title>
  <style>
    ${BASE_STYLES}
    ${thermal ? THERMAL_STYLES : A4_STYLES}
  </style>
</head>
<body>
  ${content}
  <script>window.onload = function() { window.print(); }</script>
</body>
</html>`
}

/**
 * Opens a new window with formatted HTML and triggers print dialog.
 */
export function printContent(htmlContent, title = 'Naviera - Impressao') {
  const win = window.open('', '_blank', 'width=820,height=600')
  if (!win) {
    alert('Popup bloqueado. Permita popups para imprimir.')
    return
  }
  win.document.write(htmlContent)
  win.document.close()
}

/**
 * Prints a bilhete (passagem) — formato desktop (2 paineis: esquerdo 65% + direito 35%).
 * Carrega dados da empresa automaticamente.
 */
export async function printBilhete(passagem, viagem, empresaData) {
  // Carregar dados da empresa se nao fornecidos
  let emp = empresaData || {}
  if (!emp.companhia) {
    try {
      const token = localStorage.getItem('token')
      const res = await fetch('/api/cadastros/empresa', { headers: { Authorization: `Bearer ${token}` } })
      if (res.ok) emp = await res.json()
    } catch {}
  }

  const origem = viagem?.origem || ''
  const destino = viagem?.destino || ''
  const rota = (origem && destino) ? `${origem} - ${destino}` : (viagem?.nome_rota || '\u2014')
  const dataViagem = viagem?.data_viagem || '\u2014'
  const dataChegada = viagem?.data_chegada || '\u2014'
  const numBilhete = passagem.numero_bilhete || passagem.num_bilhete || passagem.id_passagem || '\u2014'
  const vTotal = parseFloat(passagem.valor_total) || 0
  const vPago = parseFloat(passagem.valor_pago) || 0
  const vDevedor = parseFloat(passagem.valor_devedor) || Math.max(0, vTotal - vPago)
  const troco = parseFloat(passagem.troco) || 0
  const situacao = vDevedor <= 0.01 ? 'A VISTA' : 'PENDENTE'
  const statusColor = vDevedor <= 0.01 ? '#059669' : '#DC2626'

  const html = `<!DOCTYPE html>
<html lang="pt-BR"><head><meta charset="UTF-8"><title>Bilhete ${numBilhete}</title>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body { font-family: Arial, sans-serif; color: #111; }
  @page { size: 80mm auto; margin: 2mm; }
  @media print { body { width: 76mm; } }
  .ticket { border: 2px solid #000; width: 100%; display: flex; }
  .left { width: 65%; padding: 8px 10px; border-right: 2px solid #000; }
  .right { width: 35%; display: flex; flex-direction: column; align-items: center; }

  /* Header empresa */
  .emp-header { text-align: center; border-bottom: 2.5px solid #000; padding-bottom: 6px; margin-bottom: 6px; }
  .emp-nome { font-size: 16px; font-weight: 700; }
  .emp-prop { font-size: 10px; }
  .emp-info { font-size: 9px; color: #444; }
  .emp-frase { font-size: 8px; font-style: italic; color: #666; margin-top: 2px; }

  /* Secoes */
  .sec-title { font-size: 9px; font-weight: 700; text-transform: uppercase; color: #059669; border-bottom: 1px solid #ccc; padding-bottom: 2px; margin: 6px 0 4px; }
  .row { display: flex; font-size: 9px; line-height: 1.5; }
  .row .lbl { font-weight: 600; min-width: 42px; color: #333; }
  .row .val { flex: 1; }

  /* Pagamento 2 colunas */
  .pag-grid { display: flex; gap: 8px; margin-top: 4px; }
  .pag-col { flex: 1; }
  .pag-total { font-size: 14px; font-weight: 700; text-align: right; }

  /* Painel direito */
  .bilhete-box { background: #eee; border-bottom: 2px solid #000; width: 100%; text-align: center; padding: 10px 4px; flex: 1; display: flex; flex-direction: column; justify-content: center; }
  .bilhete-label { font-size: 16px; font-weight: 700; }
  .bilhete-num { font-size: 22px; font-weight: 700; margin: 4px 0; }
  .bilhete-status { font-size: 14px; font-weight: 700; }
  .avisos-box { padding: 6px 4px; font-size: 7px; color: #555; text-align: left; flex: 1; }
  .avisos-title { font-size: 8px; font-weight: 700; color: #059669; margin-bottom: 3px; text-transform: uppercase; }

  .footer { font-size: 7px; color: #888; text-align: left; margin-top: 4px; }
</style></head><body>
<div class="ticket">
  <!-- PAINEL ESQUERDO -->
  <div class="left">
    <div class="emp-header">
      <div class="emp-nome">${emp.companhia || emp.nome_embarcacao || 'NAVIERA'}</div>
      <div class="emp-prop">${emp.proprietario || ''}</div>
      <div class="emp-info">${[emp.cnpj, emp.telefone].filter(Boolean).join(' | ')}</div>
      ${emp.frase_relatorio ? `<div class="emp-frase">${emp.frase_relatorio}</div>` : ''}
    </div>

    <div class="sec-title">VIAGEM</div>
    <div class="row"><span class="lbl">De:</span><span class="val">${origem || '\u2014'}</span></div>
    <div class="row"><span class="lbl">Para:</span><span class="val">${destino || '\u2014'}</span></div>
    <div class="row"><span class="lbl">Data:</span><span class="val">${dataViagem} | Prev: ${dataChegada}</span></div>
    <div class="row"><span class="lbl">Acom.:</span><span class="val">${passagem.nome_acomodacao || '\u2014'} | Agente: ${passagem.nome_agente || '\u2014'}</span></div>

    <div class="sec-title">PASSAGEIRO</div>
    <div class="row"><span class="lbl">Nome:</span><span class="val">${passagem.nome_passageiro || '\u2014'}</span></div>
    <div class="row"><span class="lbl">Doc:</span><span class="val">${passagem.numero_doc || '\u2014'} | Nac: ${passagem.nome_nacionalidade || '\u2014'}</span></div>
    <div class="row"><span class="lbl">DN:</span><span class="val">${formatDate(passagem.data_nascimento)} | Sx: ${passagem.nome_sexo || '\u2014'}</span></div>

    <div class="sec-title">PAGAMENTO</div>
    <div class="pag-grid">
      <div class="pag-col">
        <div class="row"><span class="lbl">Alim.:</span><span class="val">${formatMoney(passagem.valor_alimentacao)}</span></div>
        <div class="row"><span class="lbl">Transp.:</span><span class="val">${formatMoney(passagem.valor_transporte)}</span></div>
        <div class="row"><span class="lbl">Carga:</span><span class="val">${formatMoney(passagem.valor_cargas)}</span></div>
      </div>
      <div class="pag-col" style="text-align:right;">
        <div class="pag-total">TOTAL: ${formatMoney(vTotal)}</div>
        ${passagem.valor_desconto_geral > 0 ? `<div class="row" style="justify-content:flex-end;"><span>Desc.: ${formatMoney(passagem.valor_desconto_geral)}</span></div>` : ''}
        <div class="row" style="justify-content:flex-end;"><span>Pago: ${formatMoney(vPago)}</span></div>
        ${troco > 0 ? `<div class="row" style="justify-content:flex-end;"><span>Troco: ${formatMoney(troco)}</span></div>` : ''}
        ${vDevedor > 0.01 ? `<div class="row" style="justify-content:flex-end; color:#DC2626;"><span>Falta: ${formatMoney(vDevedor)}</span></div>` : ''}
        <div class="row" style="justify-content:flex-end; font-weight:600;"><span>Situacao: ${situacao}</span></div>
      </div>
    </div>

    <div class="footer">Emissao: ${new Date().toLocaleString('pt-BR')}</div>
  </div>

  <!-- PAINEL DIREITO -->
  <div class="right">
    <div class="bilhete-box">
      <div class="bilhete-label">BILHETE</div>
      <div class="bilhete-num">${numBilhete}</div>
      <div class="bilhete-status" style="color:${statusColor};">${situacao === 'A VISTA' ? 'PAGO' : 'PENDENTE'}</div>
    </div>
    <div class="avisos-box">
      <div class="avisos-title">AVISOS</div>
      ${(emp.recomendacoes_bilhete || '- Chegar com 01 hora de antecedencia.\n- Documento original obrigatorio.\n- Menores so com responsavel.').replace(/\\n/g, '\n').split('\n').map(l => `<div>${l.trim()}</div>`).join('')}
    </div>
  </div>
</div>
<script>window.onload = function() { window.print(); }</script>
</body></html>`

  printContent(html, `Bilhete ${numBilhete}`)
}

function buildFormaPagamento(p) {
  const parts = []
  if (p.valor_pagamento_dinheiro > 0) parts.push('Dinheiro')
  if (p.valor_pagamento_pix > 0) parts.push('PIX')
  if (p.valor_pagamento_cartao > 0) parts.push('Cartao')
  return parts.length ? parts.join(', ') : '\u2014'
}

/**
 * Prints a recibo de encomenda — thermal 80mm format.
 */
export function printReciboEncomenda(encomenda, viagem) {
  const viagemDesc = viagem?.descricao || `Viagem #${encomenda.id_viagem || '\u2014'}`

  const content = `
    ${buildHeader('Recibo de Encomenda')}

    <div class="section">
      <div class="info-row">
        <span class="label">N. Encomenda:</span>
        <span class="value"><strong>${encomenda.numero_encomenda || encomenda.id_encomenda || '\u2014'}</strong></span>
      </div>
      <div class="info-row">
        <span class="label">Viagem:</span>
        <span class="value">${viagemDesc}</span>
      </div>
    </div>

    <hr class="divider">

    <div class="section">
      <div class="section-title">Remetente / Destinatario</div>
      <div class="info-row">
        <span class="label">Remetente:</span>
        <span class="value">${encomenda.remetente || '\u2014'}</span>
      </div>
      <div class="info-row">
        <span class="label">Destinatario:</span>
        <span class="value">${encomenda.destinatario || '\u2014'}</span>
      </div>
      <div class="info-row">
        <span class="label">Rota:</span>
        <span class="value">${encomenda.rota || '\u2014'}</span>
      </div>
    </div>

    <hr class="divider">

    <div class="section">
      <div class="section-title">Detalhes</div>
      <div class="info-row">
        <span class="label">Volumes:</span>
        <span class="value">${encomenda.total_volumes || 0}</span>
      </div>
      ${encomenda.itens && encomenda.itens.length > 0 ? `
        <table style="margin-top: 6px;">
          <thead><tr><th>Item</th><th>Qtd</th><th>Valor</th></tr></thead>
          <tbody>
            ${encomenda.itens.map(i => `
              <tr>
                <td>${i.descricao || i.nome || '\u2014'}</td>
                <td>${i.quantidade || 1}</td>
                <td>${formatMoney(i.valor)}</td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      ` : ''}
    </div>

    <hr class="divider">

    <div class="section">
      <div class="section-title">Pagamento</div>
      <div class="info-row">
        <span class="label">Total:</span>
        <span class="value"><strong>${formatMoney(encomenda.total_a_pagar)}</strong></span>
      </div>
      <div class="info-row">
        <span class="label">Desconto:</span>
        <span class="value">${formatMoney(encomenda.desconto)}</span>
      </div>
      <div class="info-row">
        <span class="label">Pago:</span>
        <span class="value">${formatMoney(encomenda.valor_pago)}</span>
      </div>
      <div class="info-row">
        <span class="label">Forma Pgto:</span>
        <span class="value">${encomenda.forma_pagamento || '\u2014'}</span>
      </div>
      <div class="info-row">
        <span class="label">Status:</span>
        <span class="value">${encomenda.status_pagamento || 'Pendente'}</span>
      </div>
    </div>

    ${buildFooter()}
  `

  const html = buildPage(content, `Recibo Encomenda ${encomenda.numero_encomenda || ''}`, true)
  printContent(html)
}

/**
 * Prints a nota de frete — thermal 80mm format.
 */
export function printNotaFrete(frete, viagem) {
  const viagemDesc = viagem?.descricao || `Viagem #${frete.id_viagem || '\u2014'}`

  const content = `
    ${buildHeader('Nota de Frete')}

    <div class="section">
      <div class="info-row">
        <span class="label">N. Frete:</span>
        <span class="value"><strong>${frete.numero_frete || frete.id_frete || '\u2014'}</strong></span>
      </div>
      <div class="info-row">
        <span class="label">Viagem:</span>
        <span class="value">${viagemDesc}</span>
      </div>
      <div class="info-row">
        <span class="label">Rota:</span>
        <span class="value">${frete.nome_rota || frete.rota_temp || '\u2014'}</span>
      </div>
    </div>

    <hr class="divider">

    <div class="section">
      <div class="section-title">Remetente / Destinatario</div>
      <div class="info-row">
        <span class="label">Remetente:</span>
        <span class="value">${frete.nome_remetente || frete.remetente_nome_temp || '\u2014'}</span>
      </div>
      <div class="info-row">
        <span class="label">Destinatario:</span>
        <span class="value">${frete.nome_destinatario || frete.destinatario_nome_temp || '\u2014'}</span>
      </div>
    </div>

    <hr class="divider">

    ${frete.itens && frete.itens.length > 0 ? `
      <div class="section">
        <div class="section-title">Itens</div>
        <table>
          <thead><tr><th>Descricao</th><th>Qtd</th><th>Valor</th></tr></thead>
          <tbody>
            ${frete.itens.map(i => `
              <tr>
                <td>${i.descricao || i.nome || '\u2014'}</td>
                <td>${i.quantidade || 1}</td>
                <td>${formatMoney(i.valor || i.valor_unitario)}</td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>
      <hr class="divider">
    ` : ''}

    <div class="section">
      <div class="section-title">Pagamento</div>
      <div class="info-row">
        <span class="label">Valor Total:</span>
        <span class="value"><strong>${formatMoney(frete.valor_nominal || frete.valor_total_itens)}</strong></span>
      </div>
      <div class="info-row">
        <span class="label">Desconto:</span>
        <span class="value">${formatMoney(frete.desconto)}</span>
      </div>
      <div class="info-row">
        <span class="label">Valor Pago:</span>
        <span class="value">${formatMoney(frete.valor_pago)}</span>
      </div>
      <div class="info-row">
        <span class="label">Tipo Pgto:</span>
        <span class="value">${frete.tipo_pagamento || '\u2014'}</span>
      </div>
      <div class="info-row">
        <span class="label">Status:</span>
        <span class="value">${frete.status || 'Pendente'}</span>
      </div>
    </div>

    <div class="qr-placeholder">
      [ QR Code / Codigo de Barras ]<br>
      Frete ${frete.numero_frete || frete.id_frete || ''}
    </div>

    ${buildFooter()}
  `

  const html = buildPage(content, `Nota Frete ${frete.numero_frete || ''}`, true)
  printContent(html)
}

/**
 * Prints an etiqueta de frete — small label format.
 */
export function printEtiquetaFrete(frete) {
  const content = `
    <div style="text-align:center; padding: 4px;">
      <div style="font-size: 10px; font-weight: 700; color: #059669; margin-bottom: 4px;">NAVIERA</div>

      <div style="font-size: 14px; font-weight: 700; margin: 6px 0;">
        ${frete.nome_destinatario || frete.destinatario_nome_temp || '\u2014'}
      </div>

      <div style="font-size: 11px; margin-bottom: 4px;">
        ${frete.nome_rota || frete.rota_temp || '\u2014'}
      </div>

      <hr class="divider">

      <div style="font-size: 12px; font-weight: 600; margin: 4px 0;">
        Frete: ${frete.numero_frete || frete.id_frete || '\u2014'}
      </div>

      <div style="font-size: 11px;">
        Volumes: <strong>${frete.total_volumes || '\u2014'}</strong>
      </div>

      <div style="font-size: 11px; margin-top: 2px;">
        Remetente: ${frete.nome_remetente || frete.remetente_nome_temp || '\u2014'}
      </div>

      <div class="qr-placeholder" style="margin-top: 6px; padding: 8px;">
        [ Codigo ]<br>${frete.numero_frete || frete.id_frete || ''}
      </div>
    </div>
  `

  const html = `<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <title>Etiqueta Frete ${frete.numero_frete || ''}</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body {
      font-family: 'Segoe UI', Arial, sans-serif;
      width: 72mm;
      max-width: 72mm;
      color: #111;
    }
    .divider {
      border: none;
      border-top: 1px dashed #ccc;
      margin: 4px 0;
    }
    .qr-placeholder {
      border: 1px dashed #ccc;
      font-size: 9px;
      color: #999;
    }
    @page { size: 80mm 60mm; margin: 2mm; }
  </style>
</head>
<body>
  ${content}
  <script>window.onload = function() { window.print(); }</script>
</body>
</html>`

  printContent(html)
}

/** Helper: carrega dados da empresa para impressao */
async function loadEmpresa() {
  try {
    const token = localStorage.getItem('token')
    const res = await fetch('/api/cadastros/empresa', { headers: { Authorization: `Bearer ${token}` } })
    if (res.ok) return await res.json()
  } catch {}
  return {}
}

function calcIdadePrint(dataNasc) {
  if (!dataNasc) return ''
  try {
    const s = String(dataNasc)
    const nasc = new Date(s.includes('T') ? s : s + 'T00:00:00')
    if (isNaN(nasc.getTime())) return ''
    const hoje = new Date()
    let idade = hoje.getFullYear() - nasc.getFullYear()
    const m = hoje.getMonth() - nasc.getMonth()
    if (m < 0 || (m === 0 && hoje.getDate() < nasc.getDate())) idade--
    return idade >= 0 ? idade : ''
  } catch { return '' }
}

/** Cabecalho padrao para relatorios A4 (empresa + titulo) */
function buildEmpresaHeader(emp, titulo) {
  return `
    <div style="text-align:center; margin-bottom:12px;">
      <div style="font-size:18px; font-weight:700;">${emp.nome_embarcacao || emp.companhia || 'NAVIERA'}</div>
      <div style="font-size:10px; color:#555;">${emp.endereco || ''}</div>
      <div style="font-size:10px; color:#555;">CNPJ: ${emp.cnpj || '—'}</div>
      <div style="font-size:10px; color:#555;">Tel: ${emp.telefone || '—'}</div>
      <div style="font-size:15px; font-weight:700; margin-top:10px; border-top:2px solid #059669; border-bottom:2px solid #059669; padding:6px 0;">${titulo}</div>
    </div>`
}

/**
 * Prints a lista de passageiros — A4 estilo desktop.
 * Colunas: ORD, Nome do Passageiro, Doc/RG, Nasc., Origem, Destino
 */
export async function printListaPassageiros(passagens, viagem) {
  const emp = await loadEmpresa()
  const vEmb = emp.nome_embarcacao || viagem?.nome_embarcacao || '—'
  const vData = viagem?.data_viagem || '—'
  const vCheg = viagem?.data_chegada || '—'
  const vHora = viagem?.horario || '—'

  const rows = passagens.map((p, i) => `
    <tr>
      <td style="text-align:center; width:35px;">${i + 1}</td>
      <td style="text-transform:uppercase;">${p.nome_passageiro || '—'}</td>
      <td>${p.numero_doc || '—'}</td>
      <td>${formatDate(p.data_nascimento)}</td>
      <td>${p.origem || '—'}</td>
      <td>${p.destino || '—'}</td>
    </tr>
  `).join('')

  const html = `<!DOCTYPE html>
<html lang="pt-BR"><head><meta charset="UTF-8"><title>Lista de Passageiros</title>
<style>
  * { margin:0; padding:0; box-sizing:border-box; }
  body { font-family: Arial, sans-serif; color:#111; padding:15mm; }
  @page { size: A4; margin: 12mm; }
  table { width:100%; border-collapse:collapse; font-size:11px; margin-top:8px; }
  th { background:#059669; color:#fff; padding:6px 8px; text-align:left; font-size:10px; text-transform:uppercase; font-weight:600; }
  td { padding:5px 8px; border-bottom:1px solid #ccc; }
  tr:nth-child(even) td { background:#f5f7fa; }
  .footer { position:fixed; bottom:10mm; left:15mm; right:15mm; display:flex; justify-content:space-between; font-size:9px; color:#888; border-top:1px solid #ccc; padding-top:4px; }
</style></head><body>
  ${buildEmpresaHeader(emp, 'LISTA DE PASSAGEIROS')}
  <div style="font-size:10px; margin-bottom:10px;">
    Embarcacao: <strong>${vEmb}</strong> | Saida: <strong>${vData}</strong> | Prev. Chegada: <strong>${vCheg}</strong> | Hora: <strong>${vHora}</strong>
  </div>
  <table>
    <thead><tr>
      <th style="width:35px;">ORD</th>
      <th>NOME DO PASSAGEIRO</th>
      <th>DOC/RG</th>
      <th>NASC.</th>
      <th>ORIGEM</th>
      <th>DESTINO</th>
    </tr></thead>
    <tbody>${rows}</tbody>
  </table>
  <div class="footer">
    <span>Impresso por: Jessyca</span>
    <span>Data: ${new Date().toLocaleString('pt-BR')}</span>
    <span>Pagina 1</span>
  </div>
  <script>window.onload = function() { window.print(); }</script>
</body></html>`

  printContent(html, 'Lista de Passageiros')
}

/**
 * Prints relatorio financeiro de passagens — A4 com filtros aplicados.
 */
export async function printRelatorioPassagens(passagens, viagem, filtrosTexto) {
  const emp = await loadEmpresa()

  const totalVendido = passagens.reduce((s, p) => s + (parseFloat(p.valor_total) || 0), 0)
  const totalRecebido = passagens.reduce((s, p) => s + (parseFloat(p.valor_pago) || 0), 0)
  const totalAReceber = totalVendido - totalRecebido

  function formaPgto(p) {
    const parts = []
    if (parseFloat(p.valor_pagamento_dinheiro) > 0) parts.push('Dinheiro')
    if (parseFloat(p.valor_pagamento_pix) > 0) parts.push('PIX')
    if (parseFloat(p.valor_pagamento_cartao) > 0) parts.push('Cartao')
    return parts.join(', ') || '—'
  }

  const rows = passagens.map(p => `
    <tr>
      <td>${p.numero_bilhete || '—'}</td>
      <td>${formatDate(p.data_emissao)}</td>
      <td>${p.nome_passageiro || '—'}</td>
      <td>${p.origem && p.destino ? p.origem + ' - ' + p.destino : '—'}</td>
      <td>${p.nome_tipo_passagem || '—'}</td>
      <td>${p.nome_agente || '—'}</td>
      <td style="text-align:right;">${formatMoney(p.valor_total)}</td>
      <td style="text-align:right;">${formatMoney(p.valor_pago)}</td>
      <td style="text-align:right; color:${parseFloat(p.valor_devedor) > 0 ? '#c00' : '#000'};">${formatMoney(p.valor_devedor)}</td>
      <td>${formaPgto(p)}</td>
      <td style="text-align:center;"><span style="padding:2px 6px; border-radius:3px; font-size:9px; font-weight:600; background:${p.status_passagem === 'PAGO' ? '#d4edda' : '#f8d7da'}; color:${p.status_passagem === 'PAGO' ? '#155724' : '#721c24'};">${p.status_passagem || 'PENDENTE'}</span></td>
    </tr>
  `).join('')

  const filtrosHtml = filtrosTexto ? `<div style="font-size:10px; margin-bottom:8px; color:#555;">Filtros: ${filtrosTexto}</div>` : ''

  const html = `<!DOCTYPE html>
<html lang="pt-BR"><head><meta charset="UTF-8"><title>Relatorio Passagens</title>
<style>
  * { margin:0; padding:0; box-sizing:border-box; }
  body { font-family: Arial, sans-serif; color:#111; padding:12mm; }
  @page { size: A4 landscape; margin: 10mm; }
  table { width:100%; border-collapse:collapse; font-size:10px; margin-top:8px; }
  th { background:#059669; color:#fff; padding:5px 6px; text-align:left; font-size:9px; text-transform:uppercase; font-weight:600; }
  td { padding:4px 6px; border-bottom:1px solid #ddd; }
  tr:nth-child(even) td { background:#f5f7fa; }
  .totais { display:flex; justify-content:center; gap:40px; margin:12px 0; }
  .totais div { text-align:center; }
  .totais .label { font-size:10px; color:#666; }
  .totais .valor { font-size:16px; font-weight:700; }
  .footer { margin-top:16px; display:flex; justify-content:space-between; font-size:9px; color:#888; border-top:1px solid #ccc; padding-top:4px; }
</style></head><body>
  ${buildEmpresaHeader(emp, 'RELATORIO FINANCEIRO DE PASSAGENS')}
  ${filtrosHtml}
  <div class="totais">
    <div><div class="label">Total Vendido</div><div class="valor">${formatMoney(totalVendido)}</div></div>
    <div><div class="label">Total Recebido</div><div class="valor" style="color:#059669;">${formatMoney(totalRecebido)}</div></div>
    <div><div class="label">Total a Receber</div><div class="valor" style="color:${totalAReceber > 0 ? '#c00' : '#000'};">${formatMoney(totalAReceber)}</div></div>
  </div>
  <table>
    <thead><tr>
      <th>Bilhete</th><th>Data</th><th>Passageiro</th><th>Rota</th><th>Tipo</th><th>Agente</th>
      <th style="text-align:right;">Total</th><th style="text-align:right;">Pago</th><th style="text-align:right;">Devedor</th>
      <th>Forma Pag.</th><th style="text-align:center;">Status</th>
    </tr></thead>
    <tbody>${rows}</tbody>
    <tfoot><tr style="font-weight:700; background:#e8ecf1;">
      <td colspan="6" style="text-align:right; border-top:2px solid #059669; padding:6px;">TOTAIS (${passagens.length} passagens):</td>
      <td style="text-align:right; border-top:2px solid #059669;">${formatMoney(totalVendido)}</td>
      <td style="text-align:right; border-top:2px solid #059669;">${formatMoney(totalRecebido)}</td>
      <td style="text-align:right; border-top:2px solid #059669; color:#c00;">${formatMoney(totalAReceber)}</td>
      <td colspan="2" style="border-top:2px solid #059669;"></td>
    </tr></tfoot>
  </table>
  <div class="footer">
    <span>Viagem: ${viagem?.id_viagem || '—'} - ${viagem?.data_viagem || ''} (${viagem?.nome_rota || viagem?.origem && viagem?.destino ? viagem.origem + ' - ' + viagem.destino : ''})</span>
    <span>Data: ${new Date().toLocaleString('pt-BR')}</span>
  </div>
  <script>window.onload = function() { window.print(); }</script>
</body></html>`

  printContent(html, 'Relatorio Passagens')
}

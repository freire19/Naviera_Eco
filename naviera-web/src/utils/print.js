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
 * Prints a bilhete (passagem) — thermal 80mm format.
 */
export function printBilhete(passagem, viagem) {
  const viagemDesc = viagem?.descricao || `Viagem #${passagem.id_viagem || '\u2014'}`
  const origem = viagem?.origem || viagem?.nome_origem || ''
  const destino = viagem?.destino || viagem?.nome_destino || ''
  const rota = (origem && destino) ? `${origem} \u2192 ${destino}` : (viagem?.nome_rota || '\u2014')

  const content = `
    ${buildHeader('Bilhete de Passagem')}

    <div class="section">
      <div class="info-row">
        <span class="label">N. Bilhete:</span>
        <span class="value"><strong>${passagem.num_bilhete || passagem.id_passagem || '\u2014'}</strong></span>
      </div>
      <div class="info-row">
        <span class="label">Viagem:</span>
        <span class="value">${viagemDesc}</span>
      </div>
      <div class="info-row">
        <span class="label">Rota:</span>
        <span class="value">${rota}</span>
      </div>
      <div class="info-row">
        <span class="label">Data Viagem:</span>
        <span class="value">${formatDateTime(viagem?.data_saida || viagem?.data_viagem)}</span>
      </div>
    </div>

    <hr class="divider">

    <div class="section">
      <div class="section-title">Passageiro</div>
      <div class="info-row">
        <span class="label">Nome:</span>
        <span class="value">${passagem.nome_passageiro || '\u2014'}</span>
      </div>
      <div class="info-row">
        <span class="label">Documento:</span>
        <span class="value">${passagem.numero_doc || '\u2014'}</span>
      </div>
      <div class="info-row">
        <span class="label">Assento:</span>
        <span class="value">${passagem.assento || '\u2014'}</span>
      </div>
    </div>

    <hr class="divider">

    <div class="section">
      <div class="section-title">Pagamento</div>
      <div class="info-row">
        <span class="label">Valor Total:</span>
        <span class="value"><strong>${formatMoney(passagem.valor_total)}</strong></span>
      </div>
      <div class="info-row">
        <span class="label">Valor Pago:</span>
        <span class="value">${formatMoney(passagem.valor_pago)}</span>
      </div>
      <div class="info-row">
        <span class="label">Forma Pgto:</span>
        <span class="value">${passagem.forma_pagamento || buildFormaPagamento(passagem)}</span>
      </div>
    </div>

    <div class="qr-placeholder">
      [ QR Code / Codigo de Barras ]<br>
      ${passagem.num_bilhete || passagem.id_passagem || ''}
    </div>

    ${buildFooter()}
  `

  const html = buildPage(content, `Bilhete ${passagem.num_bilhete || ''}`, true)
  printContent(html)
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

/**
 * Prints a lista de passageiros — A4 table format.
 */
export function printListaPassageiros(passagens, viagem) {
  const viagemDesc = viagem?.descricao || `Viagem #${viagem?.id_viagem || '\u2014'}`
  const dataViagem = formatDateTime(viagem?.data_saida || viagem?.data_viagem)

  const rows = passagens.map((p, i) => `
    <tr>
      <td style="text-align:center">${i + 1}</td>
      <td>${p.num_bilhete || '\u2014'}</td>
      <td>${p.nome_passageiro || '\u2014'}</td>
      <td>${p.numero_doc || '\u2014'}</td>
      <td style="text-align:center">${p.assento || '\u2014'}</td>
      <td style="text-align:right">${formatMoney(p.valor_total)}</td>
      <td style="text-align:center">${p.devedor ? 'Devedor' : 'Pago'}</td>
    </tr>
  `).join('')

  const totalValor = passagens.reduce((s, p) => s + (p.valor_total || 0), 0)
  const totalPago = passagens.reduce((s, p) => s + (p.valor_pago || 0), 0)

  const content = `
    ${buildHeader('Lista de Passageiros')}

    <div class="section">
      <div class="info-row">
        <span class="label">Viagem:</span>
        <span class="value">${viagemDesc}</span>
      </div>
      <div class="info-row">
        <span class="label">Data Saida:</span>
        <span class="value">${dataViagem}</span>
      </div>
      <div class="info-row">
        <span class="label">Total Passageiros:</span>
        <span class="value"><strong>${passagens.length}</strong></span>
      </div>
    </div>

    <hr class="divider">

    <table>
      <thead>
        <tr>
          <th style="text-align:center">#</th>
          <th>Bilhete</th>
          <th>Passageiro</th>
          <th>Documento</th>
          <th style="text-align:center">Assento</th>
          <th style="text-align:right">Valor</th>
          <th style="text-align:center">Status</th>
        </tr>
      </thead>
      <tbody>
        ${rows}
      </tbody>
      <tfoot>
        <tr style="font-weight:700; background:#f5f5f5;">
          <td colspan="5" style="text-align:right; border:1px solid #ccc; padding:5px 8px;">TOTAIS:</td>
          <td style="text-align:right; border:1px solid #ccc; padding:5px 8px;">${formatMoney(totalValor)}</td>
          <td style="text-align:center; border:1px solid #ccc; padding:5px 8px;">Pago: ${formatMoney(totalPago)}</td>
        </tr>
      </tfoot>
    </table>

    ${buildFooter()}
  `

  const html = buildPage(content, `Lista Passageiros - ${viagemDesc}`, false)
  printContent(html)
}

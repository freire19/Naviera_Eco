// Geracao dos 3 templates de impressao do recibo avulso.
// Replica o comportamento do desktop (GerarReciboAvulsoController).

import { valorPorExtenso } from './valorExtenso.js'

const BRAND = '#059669'

function formatarValor(v) {
  const n = Number(v) || 0
  return n.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

function formatarData(iso) {
  if (!iso) return ''
  const d = iso.length >= 10 ? new Date(iso.substring(0, 10) + 'T12:00:00') : new Date()
  return d.toLocaleDateString('pt-BR')
}

function escapeHtml(s) {
  if (s === null || s === undefined) return ''
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

// ==============================================================
//  Template 1: A4 preenchido (uma via)
// ==============================================================
function blocoA4(recibo, empresa, preenchido) {
  const numero = preenchido && recibo.id_recibo ? recibo.id_recibo : '____'
  const nome = preenchido ? escapeHtml((recibo.nome_pagador || '').toUpperCase()) : ''
  const extenso = preenchido ? escapeHtml(valorPorExtenso(recibo.valor).toUpperCase()) : ''
  const ref = preenchido ? escapeHtml((recibo.referente_a || '').toUpperCase()) : ''
  const valorTxt = preenchido ? formatarValor(recibo.valor) : ''
  const dataTxt = preenchido ? formatarData(recibo.data_emissao) : ''
  const cidade = escapeHtml((empresa.cidade || '').toUpperCase())

  return `
  <div class="recibo-a4">
    <div class="header">
      <div class="left">
        ${empresa.path_logo ? `<img src="${empresa.path_logo}" alt="logo" class="logo"/>` : ''}
        <div class="empresa">
          <div class="nome">${escapeHtml(empresa.nome || '')}</div>
          <div class="end">${escapeHtml(empresa.endereco || '')}</div>
          <div class="end">CNPJ: ${escapeHtml(empresa.cnpj || '')}${empresa.telefone ? ' | Tel: ' + escapeHtml(empresa.telefone) : ''}</div>
        </div>
      </div>
      <div class="right">
        <div class="recibo-num">RECIBO Nº <b>${numero}</b></div>
        <div class="valor-box">
          <div class="label">VALOR R$</div>
          <div class="valor">${valorTxt}</div>
        </div>
      </div>
    </div>

    <div class="body">
      <div class="linha"><b>Recebi(emos) de:</b> <span class="campo">${nome}</span></div>
      <div class="linha"><b>A importancia de:</b> <span class="campo">${extenso}</span></div>
      <div class="linha"><b>Referente a:</b> <span class="campo">${ref}</span></div>
      <div class="linha-blank"></div>
    </div>

    <div class="footer">
      <div class="data-assin">
        <div class="local-data"><b>${[cidade, preenchido ? dataTxt : ''].filter(Boolean).join(', ')}</b></div>
        <div class="sub">Cidade e Data</div>
      </div>
      <div class="assin">
        <div class="linha-assin"></div>
        <div class="sub">Assinatura</div>
      </div>
    </div>
  </div>`
}

function cssCommon() {
  return `
    body { margin: 0; padding: 0; font-family: Arial, sans-serif; color: #111; }
    .recibo-a4 {
      border: 2px solid ${BRAND};
      padding: 18px 22px;
      margin: 0 auto;
      width: 680px;
      box-sizing: border-box;
      background: white;
    }
    .recibo-a4 .header { display: flex; justify-content: space-between; align-items: flex-start; }
    .recibo-a4 .left { display: flex; gap: 10px; align-items: center; }
    .recibo-a4 .logo { width: 50px; height: auto; }
    .recibo-a4 .empresa .nome { color: ${BRAND}; font-weight: bold; font-size: 16px; }
    .recibo-a4 .empresa .end { font-size: 11px; color: #333; }
    .recibo-a4 .right { text-align: right; }
    .recibo-a4 .recibo-num { font-size: 14px; margin-bottom: 6px; }
    .recibo-a4 .valor-box {
      border: 1.5px solid ${BRAND};
      padding: 6px 14px;
      min-width: 170px;
      text-align: center;
    }
    .recibo-a4 .valor-box .label { font-size: 10px; color: #555; }
    .recibo-a4 .valor-box .valor { font-weight: bold; font-size: 15px; color: #111; min-height: 18px; }
    .recibo-a4 .body { margin-top: 22px; }
    .recibo-a4 .body .linha { margin-bottom: 18px; font-size: 12px; border-bottom: 1px solid #111; padding-bottom: 2px; }
    .recibo-a4 .body .linha-blank { border-bottom: 1px solid #111; margin-bottom: 18px; height: 16px; }
    .recibo-a4 .body .campo { margin-left: 6px; }
    .recibo-a4 .footer {
      display: flex; justify-content: space-between; align-items: flex-end; margin-top: 28px;
    }
    .recibo-a4 .local-data { border-bottom: 1px solid #111; padding-bottom: 2px; min-width: 260px; font-size: 12px; }
    .recibo-a4 .linha-assin { border-bottom: 1px solid #111; min-width: 260px; height: 22px; }
    .recibo-a4 .sub { font-size: 10px; color: #555; text-align: center; }
    .corte {
      border: none; border-top: 2px dashed #888; margin: 22px 0;
      position: relative; text-align: center;
    }
    .corte::before {
      content: '✂ Corte Aqui'; background: white; padding: 0 10px;
      position: relative; top: -11px; color: #888; font-size: 11px;
    }
    @media print {
      body { margin: 0; }
      .recibo-a4 { page-break-inside: avoid; }
    }
    @page { size: A4; margin: 12mm; }
  `
}

function cssTermica() {
  return `
    body { margin: 0; padding: 0; font-family: Arial, sans-serif; color: #000; width: 80mm; }
    .termica { width: 72mm; margin: 0 auto; padding: 4mm 0 24mm 0; text-align: center; }
    .termica .logo { width: 45px; height: auto; margin-bottom: 4px; }
    .termica .emp { font-weight: bold; font-size: 10pt; }
    .termica .small { font-size: 8pt; }
    .termica .sep { margin: 4px 0; }
    .termica .titulo { font-weight: bold; font-size: 11pt; margin: 6px 0; }
    .termica .bloco { text-align: left; margin: 6px 2mm; font-size: 9pt; }
    .termica .bloco b { display: block; font-size: 8pt; }
    .termica .valor { font-weight: bold; font-size: 12pt; }
    .termica .extenso { font-size: 8pt; }
    .termica .footer { margin-top: 10mm; text-align: center; font-size: 8pt; }
    .termica .footer .cidade-data { margin-bottom: 28px; }
    .termica .linha-assin { border-top: 1px solid #000; width: 55mm; margin: 18px auto 2px; }
    @media print { body { margin: 0; } }
    @page { size: 80mm auto; margin: 2mm; }
  `
}

function blocoTermica(recibo, empresa) {
  const num = recibo.id_recibo || '____'
  const cidade = (empresa.cidade || '').toUpperCase()
  return `
  <div class="termica">
    ${empresa.path_logo ? `<img src="${empresa.path_logo}" alt="logo" class="logo"/>` : ''}
    <div class="emp">${escapeHtml((empresa.nome || '').toUpperCase())}</div>
    <div class="small">CNPJ: ${escapeHtml(empresa.cnpj || '')}</div>
    <div class="small">${escapeHtml(empresa.endereco || '')}</div>
    ${empresa.telefone ? `<div class="small">Tel: ${escapeHtml(empresa.telefone)}</div>` : ''}
    <div class="sep">--------------------------------</div>
    <div class="titulo">RECIBO Nº ${num}</div>
    <div class="bloco">
      <b>PAGADOR:</b>
      <div>${escapeHtml((recibo.nome_pagador || '').toUpperCase())}</div>
    </div>
    <div class="sep">- - - - - - - - - - - - - - -</div>
    <div class="bloco">
      <div class="valor">VALOR: ${formatarValor(recibo.valor)}</div>
      <div class="extenso">(${escapeHtml(valorPorExtenso(recibo.valor).toUpperCase())})</div>
    </div>
    <div class="sep">- - - - - - - - - - - - - - -</div>
    <div class="bloco">
      <b>REFERENTE:</b>
      <div>${escapeHtml((recibo.referente_a || '').toUpperCase())}</div>
    </div>
    <div class="footer">
      <div class="cidade-data"><b>${[escapeHtml(cidade), recibo.data_emissao ? formatarData(recibo.data_emissao) : ''].filter(Boolean).join(', ')}</b></div>
      <div class="linha-assin"></div>
      <div>Assinatura</div>
      <div style="margin-top:10px;">Impresso em: ${new Date().toLocaleString('pt-BR')}</div>
    </div>
  </div>`
}

// ==============================================================
//  API publica
// ==============================================================

/**
 * Imprime um recibo.
 * @param {'A4_PREENCHIDO'|'A4_BRANCO'|'TERMICA'} tipo
 * @param {object} recibo  { id_recibo, nome_pagador, referente_a, valor, data_emissao }
 * @param {object} empresa { nome, cnpj, endereco, telefone, path_logo }
 */
export function imprimirRecibo(tipo, recibo, empresa) {
  let html = ''
  if (tipo === 'TERMICA') {
    html = `<!doctype html><html><head><meta charset="utf-8"><title>Recibo</title>
            <style>${cssTermica()}</style></head><body>${blocoTermica(recibo, empresa)}
            <script>window.onload=()=>{setTimeout(()=>{window.print();},150);};</script>
            </body></html>`
  } else if (tipo === 'A4_BRANCO') {
    const blank = { id_recibo: null, nome_pagador: '', referente_a: '', valor: 0, data_emissao: null }
    html = `<!doctype html><html><head><meta charset="utf-8"><title>Recibo</title>
            <style>${cssCommon()}</style></head><body>
            ${blocoA4(blank, empresa, false)}
            <div class="corte"></div>
            ${blocoA4(blank, empresa, false)}
            <script>window.onload=()=>{setTimeout(()=>{window.print();},150);};</script>
            </body></html>`
  } else {
    // A4_PREENCHIDO (padrao)
    html = `<!doctype html><html><head><meta charset="utf-8"><title>Recibo</title>
            <style>${cssCommon()}</style></head><body>
            ${blocoA4(recibo, empresa, true)}
            <script>window.onload=()=>{setTimeout(()=>{window.print();},150);};</script>
            </body></html>`
  }

  const win = window.open('', '_blank', 'width=900,height=1000')
  if (!win) {
    alert('O navegador bloqueou a janela de impressao. Permita popups para este site.')
    return false
  }
  win.document.open()
  win.document.write(html)
  win.document.close()
  return true
}

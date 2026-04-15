import { useState, useEffect } from 'react'
import { api } from '../api.js'
import { printContent } from '../utils/print.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}

export default function RelatorioEncomendas({ viagemAtiva, onNavigate }) {
  const [viagens, setViagens] = useState([])
  const [rotas, setRotas] = useState([])
  const [filtroViagem, setFiltroViagem] = useState(viagemAtiva?.id_viagem || '')
  const [filtroRota, setFiltroRota] = useState('')
  const [formato, setFormato] = useState('completo')

  useEffect(() => {
    Promise.allSettled([
      api.get('/viagens').then(setViagens),
      api.get('/rotas').then(setRotas)
    ]).catch(() => {})
  }, [])

  const I = { padding: '7px 10px', fontSize: '0.82rem', background: 'var(--bg-soft)', border: '1px solid var(--border)', borderRadius: 4, color: 'var(--text)', fontFamily: 'Sora, sans-serif', width: '100%', boxSizing: 'border-box' }
  const L = { fontSize: '0.72rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: 3, display: 'block' }

  async function handleGerar() {
    if (!filtroViagem) { alert('Selecione uma viagem'); return }
    try {
      const encomendas = await api.get(`/encomendas?viagem_id=${filtroViagem}`)
      const filtradas = filtroRota ? encomendas.filter(e => (e.rota || '').includes(filtroRota)) : encomendas

      let emp = {}
      try {
        const token = localStorage.getItem('token')
        const res = await fetch('/api/cadastros/empresa', { headers: { Authorization: `Bearer ${token}` } })
        if (res.ok) emp = await res.json()
      } catch {}

      const viagem = viagens.find(v => String(v.id_viagem) === String(filtroViagem))
      const header = `<div style="text-align:center; margin-bottom:12px;">
        <div style="font-size:18px; font-weight:700;">${emp.nome_embarcacao || emp.companhia || 'NAVIERA'}</div>
        <div style="font-size:10px; color:#555;">CNPJ: ${emp.cnpj || '—'} | Tel: ${emp.telefone || ''}</div>
      </div>`

      let body = ''

      if (formato === 'completo') {
        const totalGeral = filtradas.reduce((s, e) => s + (parseFloat(e.total_a_pagar) || 0), 0)
        const totalPago = filtradas.reduce((s, e) => s + (parseFloat(e.valor_pago) || 0), 0)
        body = `${header}
        <div style="font-size:14px; font-weight:700; text-align:center; border-top:2px solid #059669; border-bottom:2px solid #059669; padding:6px; margin-bottom:12px;">CONFERENCIA GERAL DE ENCOMENDAS</div>
        <div style="font-size:10px; margin-bottom:8px;">Viagem: ${viagem?.id_viagem || ''} - ${viagem?.data_viagem || ''} | Rota: ${filtroRota || 'Todas'}</div>
        ${filtradas.map(e => `
          <div style="border:1px solid #ccc; padding:8px; margin-bottom:8px; font-size:10px;">
            <div style="font-weight:700;">Encomenda #${e.numero_encomenda} ${e.rota ? '— ' + e.rota : ''}</div>
            <div>Remetente: ${e.remetente || '—'}</div>
            <div>Destinatario: ${e.destinatario || '—'}</div>
            <div>Total: ${formatMoney(e.total_a_pagar)} | Pago: ${formatMoney(e.valor_pago)} | Status: ${e.status_pagamento || 'PENDENTE'}</div>
          </div>
        `).join('')}
        <div style="border-top:2px solid #059669; padding-top:8px; font-size:11px; font-weight:700;">
          TOTAIS: ${filtradas.length} encomendas | Total: ${formatMoney(totalGeral)} | Recebido: ${formatMoney(totalPago)} | A Receber: ${formatMoney(totalGeral - totalPago)}
        </div>`
      } else if (formato === 'simples') {
        body = `${header}
        <div style="font-size:14px; font-weight:700; text-align:center; border-top:2px solid #059669; border-bottom:2px solid #059669; padding:6px; margin-bottom:12px;">LISTA RAPIDA DE ENTREGA</div>
        ${filtradas.map(e => `<div style="font-size:12px; padding:4px 0; border-bottom:1px solid #eee;"><strong>N° ${e.numero_encomenda}</strong> — ${e.destinatario || '—'}</div>`).join('')}
        <div style="margin-top:8px; font-size:10px; font-weight:700;">Total: ${filtradas.length} encomendas</div>`
      } else if (formato === 'tabular') {
        const totalGeral = filtradas.reduce((s, e) => s + (parseFloat(e.total_a_pagar) || 0), 0)
        body = `${header}
        <div style="font-size:14px; font-weight:700; text-align:center; border-top:2px solid #059669; border-bottom:2px solid #059669; padding:6px; margin-bottom:12px;">LISTA DE ENCOMENDAS</div>
        <table style="width:100%; border-collapse:collapse; font-size:10px;">
          <thead><tr style="background:#059669; color:#fff;">
            <th style="padding:4px 6px; text-align:left;">N°</th><th style="padding:4px 6px; text-align:left;">Remetente</th>
            <th style="padding:4px 6px; text-align:left;">Destinatario</th><th style="padding:4px 6px; text-align:right;">Valor</th>
            <th style="padding:4px 6px; text-align:center;">Status</th>
          </tr></thead>
          <tbody>
            ${filtradas.map(e => `<tr><td style="padding:3px 6px; border-bottom:1px solid #ddd;">${e.numero_encomenda}</td>
              <td style="padding:3px 6px; border-bottom:1px solid #ddd;">${e.remetente || '—'}</td>
              <td style="padding:3px 6px; border-bottom:1px solid #ddd;">${e.destinatario || '—'}</td>
              <td style="padding:3px 6px; border-bottom:1px solid #ddd; text-align:right;">${formatMoney(e.total_a_pagar)}</td>
              <td style="padding:3px 6px; border-bottom:1px solid #ddd; text-align:center;">${e.status_pagamento || 'PENDENTE'}</td></tr>`).join('')}
          </tbody>
          <tfoot><tr style="font-weight:700; background:#e8f5e9;"><td colspan="3" style="padding:4px 6px; text-align:right;">TOTAIS (${filtradas.length}):</td><td style="padding:4px 6px; text-align:right;">${formatMoney(totalGeral)}</td><td></td></tr></tfoot>
        </table>`
      }

      const html = `<!DOCTYPE html><html lang="pt-BR"><head><meta charset="UTF-8"><title>Relatorio Encomendas</title>
      <style>* { margin:0; padding:0; box-sizing:border-box; } body { font-family:Arial,sans-serif; padding:12mm; color:#111; } @page { size:A4; margin:10mm; }</style>
      </head><body>${body}
      <div style="margin-top:16px; font-size:9px; color:#888; border-top:1px solid #ccc; padding-top:4px; display:flex; justify-content:space-between;">
        <span>Viagem: ${viagem?.id_viagem || ''} - ${viagem?.data_viagem || ''}</span>
        <span>Data: ${new Date().toLocaleString('pt-BR')}</span>
      </div>
      <script>window.onload = function() { window.print(); }</script></body></html>`

      printContent(html, 'Relatorio Encomendas')
    } catch (err) {
      alert('Erro ao gerar relatorio: ' + (err.message || err))
    }
  }

  return (
    <div className="card" style={{ maxWidth: 700, margin: '0 auto' }}>
      <h2 style={{ textAlign: 'center', marginBottom: 20 }}>CENTRAL DE RELATORIOS</h2>

      <div style={{ padding: 16, border: '1px solid var(--border)', borderRadius: 8, marginBottom: 16 }}>
        <h3 style={{ marginBottom: 12, fontSize: '0.9rem' }}>1. FILTROS:</h3>

        <div style={{ marginBottom: 10 }}>
          <label style={L}>Selecione a Viagem (Obrigatorio):</label>
          <select style={I} value={filtroViagem} onChange={e => setFiltroViagem(e.target.value)}>
            <option value="">Escolha a viagem...</option>
            {viagens.map(v => <option key={v.id_viagem} value={v.id_viagem}>{v.id_viagem} - {v.data_viagem} - {v.nome_rota || ''}</option>)}
          </select>
        </div>

        <div>
          <label style={L}>Selecione a Rota (Opcional):</label>
          <select style={I} value={filtroRota} onChange={e => setFiltroRota(e.target.value)}>
            <option value="">Todas as Rotas</option>
            {rotas.map(r => <option key={r.id_rota} value={`${r.origem} - ${r.destino}`}>{r.origem} - {r.destino}</option>)}
          </select>
        </div>
      </div>

      <div style={{ padding: 16, border: '1px solid var(--border)', borderRadius: 8, marginBottom: 16 }}>
        <h3 style={{ marginBottom: 12, fontSize: '0.9rem' }}>2. MODELO DE IMPRESSAO:</h3>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <label style={{ cursor: 'pointer' }}>
            <input type="radio" name="fmt" checked={formato === 'completo'} onChange={() => setFormato('completo')} />
            <strong style={{ marginLeft: 6 }}>1. CONFERENCIA GERAL (Relatorio Detalhado)</strong>
            <div style={{ marginLeft: 24, fontSize: '0.78rem', color: 'var(--text-muted)' }}>Exibe remetente, destinatario, todos os itens, valores e resumo.</div>
          </label>

          <label style={{ cursor: 'pointer' }}>
            <input type="radio" name="fmt" checked={formato === 'simples'} onChange={() => setFormato('simples')} />
            <strong style={{ marginLeft: 6 }}>2. LISTA RAPIDA DE ENTREGA (Simplificado)</strong>
            <div style={{ marginLeft: 24, fontSize: '0.78rem', color: 'var(--text-muted)' }}>Exibe APENAS o Numero da Encomenda e o Nome do Destinatario.</div>
          </label>

          <label style={{ cursor: 'pointer' }}>
            <input type="radio" name="fmt" checked={formato === 'tabular'} onChange={() => setFormato('tabular')} />
            <strong style={{ marginLeft: 6 }}>3. LISTA DE ENCOMENDAS (Tabela da Viagem)</strong>
            <div style={{ marginLeft: 24, fontSize: '0.78rem', color: 'var(--text-muted)' }}>Gera uma tabela com linhas e colunas (N°, Remetente, Destinatario, Valor, Status).</div>
          </label>
        </div>
      </div>

      <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
        {onNavigate && <button className="btn-secondary" style={{ padding: '12px 28px' }} onClick={() => onNavigate('nova-encomenda')}>SAIR</button>}
        <button className="btn-primary" style={{ width: 'auto', padding: '12px 32px', fontSize: '0.95rem' }} onClick={handleGerar}>GERAR RELATORIO</button>
      </div>
    </div>
  )
}

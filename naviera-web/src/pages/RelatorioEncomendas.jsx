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

  // Modal: "O que deseja imprimir?"
  const [showFiltroModal, setShowFiltroModal] = useState(false)
  const [gerando, setGerando] = useState(false)

  useEffect(() => {
    Promise.allSettled([
      api.get('/viagens').then(setViagens),
      api.get('/rotas').then(setRotas)
    ]).catch(() => {})
  }, [])

  const I = { padding: '7px 10px', fontSize: '0.82rem', background: 'var(--bg-soft)', border: '1px solid var(--border)', borderRadius: 4, color: 'var(--text)', fontFamily: 'Sora, sans-serif', width: '100%', boxSizing: 'border-box' }
  const L = { fontSize: '0.72rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: 3, display: 'block' }

  function handleClickGerar() {
    if (!filtroViagem) { alert('Selecione uma viagem'); return }
    setShowFiltroModal(true)
  }

  async function gerarRelatorio(filtroImpressao) {
    setShowFiltroModal(false)
    setGerando(true)
    try {
      const encomendas = await api.get(`/encomendas?viagem_id=${filtroViagem}`)
      let filtradas = filtroRota ? encomendas.filter(e => (e.rota || '').includes(filtroRota)) : encomendas

      // Aplicar filtro de impressao
      if (filtroImpressao === 'pendentes') {
        filtradas = filtradas.filter(e => !e.entregue)
      } else if (filtroImpressao === 'falta_pagar') {
        filtradas = filtradas.filter(e => {
          const devedor = (parseFloat(e.total_a_pagar) || 0) - (parseFloat(e.desconto) || 0) - (parseFloat(e.valor_pago) || 0)
          return devedor > 0.01
        })
      }

      let emp = {}
      try {
        const token = localStorage.getItem('token')
        const res = await fetch('/api/cadastros/empresa', { headers: { Authorization: `Bearer ${token}` } })
        if (res.ok) emp = await res.json()
      } catch {}

      const viagem = viagens.find(v => String(v.id_viagem) === String(filtroViagem))
      const now = new Date()
      const dataHora = `${now.toLocaleDateString('pt-BR')} ${now.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}`
      const filtroTexto = filtroImpressao === 'pendentes' ? 'So Pendentes' : filtroImpressao === 'falta_pagar' ? 'Falta Pagar' : 'Tudo'

      const totalLancado = filtradas.reduce((s, e) => s + (parseFloat(e.total_a_pagar) || 0), 0)
      const totalDesconto = filtradas.reduce((s, e) => s + (parseFloat(e.desconto) || 0), 0)
      const totalRecebido = filtradas.reduce((s, e) => s + (parseFloat(e.valor_pago) || 0), 0)
      const totalAReceber = totalLancado - totalDesconto - totalRecebido

      const header = `<div style="text-align:center; margin-bottom:6px;">
        <div style="font-size:16px; font-weight:700;">${emp.nome_embarcacao ? 'F/B ' + emp.nome_embarcacao : emp.companhia || 'NAVIERA'}</div>
        <div style="font-size:9px; color:#555;">CNPJ: ${emp.cnpj || '—'}</div>
        <div style="font-size:9px; color:#555;">${emp.endereco || ''}</div>
        <div style="font-size:13px; font-weight:700; margin-top:6px; text-decoration:underline;">RELATORIO DE ENCOMENDAS</div>
      </div>
      <div style="font-size:10px; margin-bottom:6px;">
        <div><strong>DATA DA VIAGEM: ${viagem?.id_viagem || ''} - ${viagem?.data_viagem || ''}</strong></div>
        <div>IMPRESSO EM: ${dataHora}  |  FILTROS: ${filtroTexto}</div>
        <div><strong>PAGINA: 1/1</strong></div>
      </div>
      <hr style="border:none; border-top:2px solid #059669; margin-bottom:8px;">`

      let body = ''

      if (formato === 'completo') {
        // CONFERENCIA GERAL — detalhado com itens de cada encomenda
        let encHtml = ''
        for (const e of filtradas) {
          // Carregar itens de cada encomenda
          let itensData = []
          try {
            const token = localStorage.getItem('token')
            const res = await fetch(`/api/encomendas/${e.id_encomenda}/itens`, { headers: { Authorization: `Bearer ${token}` } })
            if (res.ok) itensData = await res.json()
          } catch {}

          const devedor = Math.max(0, (parseFloat(e.total_a_pagar) || 0) - (parseFloat(e.desconto) || 0) - (parseFloat(e.valor_pago) || 0))

          encHtml += `
          <div style="border:1px solid #999; margin-bottom:10px; page-break-inside:avoid;">
            <div style="background:#059669; color:#fff; padding:4px 8px; font-size:11px; font-weight:700;">
              ENCOMENDA N° ${e.numero_encomenda} — ${e.rota || ''}
            </div>
            <div style="padding:6px 8px; font-size:10px;">
              <div><strong>Remetente:</strong> ${(e.remetente || '—').toUpperCase()}</div>
              <div><strong>Destinatario:</strong> ${(e.destinatario || '—').toUpperCase()}</div>
              ${e.observacoes ? `<div><strong>Obs:</strong> ${e.observacoes}</div>` : ''}

              ${itensData.length > 0 ? `
              <table style="width:100%; border-collapse:collapse; margin:6px 0; font-size:10px;">
                <thead><tr style="background:#f0f0f0; border-bottom:1px solid #999;">
                  <th style="padding:3px 6px; text-align:left; width:40px;">Qtd</th>
                  <th style="padding:3px 6px; text-align:left;">Descricao</th>
                  <th style="padding:3px 6px; text-align:right; width:80px;">V. Unit.</th>
                  <th style="padding:3px 6px; text-align:right; width:80px;">V. Total</th>
                </tr></thead>
                <tbody>
                  ${itensData.map((it, i) => `<tr style="background:${i % 2 === 0 ? '#fff' : '#f9f9f9'}; border-bottom:1px solid #ddd;">
                    <td style="padding:2px 6px;">${it.quantidade || 1}</td>
                    <td style="padding:2px 6px;">${(it.descricao || '').toUpperCase()}</td>
                    <td style="padding:2px 6px; text-align:right;">${formatMoney(it.valor_unitario)}</td>
                    <td style="padding:2px 6px; text-align:right;">${formatMoney(it.valor_total)}</td>
                  </tr>`).join('')}
                </tbody>
              </table>` : '<div style="color:#888; margin:4px 0;">Sem itens registrados</div>'}

              <div style="display:flex; justify-content:space-between; margin-top:4px; padding-top:4px; border-top:1px solid #ccc;">
                <span>Volumes: <strong>${e.total_volumes || 0}</strong></span>
                <span>Total: <strong>${formatMoney(e.total_a_pagar)}</strong></span>
                <span>Pago: <strong>${formatMoney(e.valor_pago)}</strong></span>
                <span style="color:${devedor > 0 ? '#DC2626' : '#059669'}; font-weight:700;">
                  ${devedor > 0 ? 'Falta: ' + formatMoney(devedor) : 'QUITADO'}
                </span>
                <span>Entrega: <strong>${e.entregue ? 'SIM' : 'PENDENTE'}</strong></span>
              </div>
            </div>
          </div>`
        }

        body = `${header}${encHtml}
        <div style="margin-top:10px; padding:8px; border:2px solid #059669; font-size:11px;">
          <div><strong>TOTAL LANCADO: ${formatMoney(totalLancado)}</strong></div>
          <div><strong>TOTAL DESCONTO: ${formatMoney(totalDesconto)}</strong></div>
          <div><strong>TOTAL RECEBIDO: ${formatMoney(totalRecebido)}</strong></div>
          <div style="color:#DC2626; font-weight:700; font-size:12px;">TOTAL A RECEBER: ${formatMoney(totalAReceber)}</div>
        </div>`

      } else if (formato === 'simples') {
        body = `${header}
        ${filtradas.map(e => `<div style="font-size:12px; padding:4px 0; border-bottom:1px solid #eee;"><strong>N° ${e.numero_encomenda}</strong> — ${(e.destinatario || '—').toUpperCase()}</div>`).join('')}
        <div style="margin-top:8px; font-size:10px; font-weight:700;">Total: ${filtradas.length} encomendas</div>`

      } else if (formato === 'tabular') {
        let rows = ''
        filtradas.forEach((e, idx) => {
          const devedor = Math.max(0, (parseFloat(e.total_a_pagar) || 0) - (parseFloat(e.desconto) || 0) - (parseFloat(e.valor_pago) || 0))
          const statusPg = devedor <= 0.01 ? 'PAGO' : 'FALTA'
          const statusEnt = e.entregue ? 'ENTR.' : 'PEND.'
          rows += `<tr style="background:${idx % 2 === 0 ? '#fff' : '#f5f5f5'};">
            <td style="padding:3px 6px; border-bottom:1px solid #ddd;">${e.numero_encomenda}</td>
            <td style="padding:3px 6px; border-bottom:1px solid #ddd;">${(e.remetente || '').toUpperCase()}</td>
            <td style="padding:3px 6px; border-bottom:1px solid #ddd;">${(e.destinatario || '').toUpperCase()}</td>
            <td style="padding:3px 6px; border-bottom:1px solid #ddd;">${e.rota || '—'}</td>
            <td style="padding:3px 6px; border-bottom:1px solid #ddd; text-align:right;">${formatMoney(e.total_a_pagar)}</td>
            <td style="padding:3px 6px; border-bottom:1px solid #ddd; text-align:center; color:${devedor > 0 ? '#DC2626' : '#059669'}; font-weight:600;">${statusPg} / ${statusEnt}</td>
            <td style="padding:3px 6px; border-bottom:1px solid #ddd;">${e.doc_recebedor || ''}</td>
          </tr>`
        })
        body = `${header}
        <table style="width:100%; border-collapse:collapse; font-size:10px;">
          <thead><tr style="background:#059669; color:#fff;">
            <th style="padding:4px 6px;">N°</th><th style="padding:4px 6px;">REMETENTE</th>
            <th style="padding:4px 6px;">DESTINATARIO</th><th style="padding:4px 6px;">ROTA</th>
            <th style="padding:4px 6px; text-align:right;">VALOR</th><th style="padding:4px 6px; text-align:center;">STATUS</th>
            <th style="padding:4px 6px;">RECEBEDOR</th>
          </tr></thead><tbody>${rows}</tbody>
        </table>
        <div style="margin-top:10px; padding:8px; border:2px solid #059669; font-size:11px;">
          <div><strong>TOTAL LANCADO: ${formatMoney(totalLancado)}</strong></div>
          <div><strong>TOTAL RECEBIDO: ${formatMoney(totalRecebido)}</strong></div>
          <div style="color:#DC2626; font-weight:700;">TOTAL A RECEBER: ${formatMoney(totalAReceber)}</div>
        </div>`

      } else if (formato === 'precos') {
        // Tabela de precos do sistema
        let itensPreco = []
        try { itensPreco = await api.get('/cadastros/itens-encomenda') } catch {}
        body = `${header}
        <div style="font-size:12px; font-weight:700; text-align:center; margin-bottom:8px;">TABELA DE PRECOS DO SISTEMA</div>
        <table style="width:100%; border-collapse:collapse; font-size:11px;">
          <thead><tr style="background:#059669; color:#fff;">
            <th style="padding:4px 8px; text-align:left;">DESCRICAO</th>
            <th style="padding:4px 8px; text-align:left; width:80px;">UNIDADE</th>
            <th style="padding:4px 8px; text-align:right; width:100px;">PRECO (R$)</th>
          </tr></thead><tbody>
          ${itensPreco.map((ip, i) => `<tr style="background:${i % 2 === 0 ? '#fff' : '#f5f5f5'}; border-bottom:1px solid #ddd;">
            <td style="padding:3px 8px;">${(ip.nome_item || '').toUpperCase()}</td>
            <td style="padding:3px 8px;">${ip.unidade_medida || 'UN'}</td>
            <td style="padding:3px 8px; text-align:right;">${formatMoney(ip.preco_padrao || ip.preco_unitario_padrao)}</td>
          </tr>`).join('')}
          </tbody></table>
        <div style="margin-top:8px; font-size:10px;">Total: ${itensPreco.length} itens cadastrados</div>`
      }

      const html = `<!DOCTYPE html><html lang="pt-BR"><head><meta charset="UTF-8"><title>Relatorio Encomendas</title>
      <style>* { margin:0; padding:0; box-sizing:border-box; } body { font-family:Arial,sans-serif; padding:10mm; color:#111; } @page { size:A4; margin:8mm; }</style>
      </head><body>${body}
      <div style="margin-top:12px; font-size:9px; color:#888; border-top:1px solid #ccc; padding-top:4px; display:flex; justify-content:space-between;">
        <span>Viagem: ${viagem?.id_viagem || ''} - ${viagem?.data_viagem || ''}</span>
        <span>Data: ${dataHora}</span>
      </div>
      <script>window.onload = function() { window.print(); }</script></body></html>`

      printContent(html, 'Relatorio Encomendas')
    } catch (err) {
      alert('Erro: ' + (err.message || err))
    } finally {
      setGerando(false)
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
            <option value="">TODAS - AS ROTAS</option>
            {rotas.map(r => <option key={r.id_rota} value={`${r.origem} - ${r.destino}`}>{r.origem} - {r.destino}</option>)}
          </select>
        </div>
      </div>

      <div style={{ padding: 16, border: '1px solid var(--border)', borderRadius: 8, marginBottom: 16 }}>
        <h3 style={{ marginBottom: 12, fontSize: '0.9rem' }}>2. MODELO DE IMPRESSAO:</h3>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          {[
            { key: 'completo', title: '1. CONFERENCIA GERAL (Relatorio Detalhado)', desc: '(Padrao) Exibe remetente, destinatario, todos os itens, valores e resumo.' },
            { key: 'simples', title: '2. LISTA RAPIDA DE ENTREGA (Simplificado)', desc: 'Exibe APENAS o Numero da Encomenda e o Nome do Destinatario.' },
            { key: 'tabular', title: '3. LISTA DE ENCOMENDAS (Tabela da Viagem)', desc: 'Gera uma tabela com linhas e colunas (N°, Remetente, Destinatario, Valor, Status).' },
            { key: 'precos', title: '4. TABELA DE PRECOS GERAL (Sistema)', desc: 'Imprime a tabela de precos cadastrada no sistema (nao depende da viagem).' }
          ].map(opt => (
            <label key={opt.key} style={{ cursor: 'pointer' }}>
              <input type="radio" name="fmt" checked={formato === opt.key} onChange={() => setFormato(opt.key)} />
              <strong style={{ marginLeft: 6 }}>{opt.title}</strong>
              <div style={{ marginLeft: 24, fontSize: '0.78rem', color: 'var(--text-muted)' }}>{opt.desc}</div>
              {opt.key !== 'precos' && <hr style={{ border: 'none', borderTop: '1px solid var(--border)', margin: '8px 0 0' }} />}
            </label>
          ))}
        </div>
      </div>

      <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
        {onNavigate && <button className="btn-secondary" style={{ padding: '12px 28px' }} onClick={() => onNavigate('nova-encomenda')}>SAIR</button>}
        <button className="btn-primary" style={{ width: 'auto', padding: '12px 32px', fontSize: '0.95rem' }} onClick={handleClickGerar} disabled={gerando}>
          {gerando ? 'Gerando...' : 'GERAR RELATORIO'}
        </button>
      </div>

      {/* Modal — O que deseja imprimir? */}
      {showFiltroModal && (
        <div className="modal-overlay" onClick={() => setShowFiltroModal(false)}>
          <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 420, textAlign: 'center' }}>
            <h3 style={{ marginBottom: 16 }}>O que deseja imprimir?</h3>
            <div style={{ display: 'flex', gap: 10, justifyContent: 'center', flexWrap: 'wrap' }}>
              <button className="btn-primary" style={{ width: 'auto', padding: '10px 20px' }} onClick={() => gerarRelatorio('tudo')}>Tudo</button>
              <button className="btn-secondary" style={{ padding: '10px 20px' }} onClick={() => gerarRelatorio('pendentes')}>So Pendentes</button>
              <button className="btn-secondary" style={{ padding: '10px 20px' }} onClick={() => gerarRelatorio('falta_pagar')}>Falta Pagar</button>
              <button className="btn-secondary" style={{ padding: '10px 20px' }} onClick={() => setShowFiltroModal(false)}>Cancelar</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

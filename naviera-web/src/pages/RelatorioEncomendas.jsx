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

  // Modal orientacao
  const [showOrientacao, setShowOrientacao] = useState(false)
  const [htmlParaImprimir, setHtmlParaImprimir] = useState('')

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
      const now = new Date()
      const dataHora = `${now.toLocaleDateString('pt-BR')} ${now.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}`

      const totalLancado = filtradas.reduce((s, e) => s + (parseFloat(e.total_a_pagar) || 0), 0)
      const totalDesconto = filtradas.reduce((s, e) => s + (parseFloat(e.desconto) || 0), 0)
      const totalRecebido = filtradas.reduce((s, e) => s + (parseFloat(e.valor_pago) || 0), 0)
      const totalAReceber = totalLancado - totalDesconto - totalRecebido

      const header = `<div style="text-align:center; margin-bottom:8px;">
        <div style="font-size:16px; font-weight:700;">${emp.nome_embarcacao ? 'F/B ' + emp.nome_embarcacao : emp.companhia || 'NAVIERA'}</div>
        <div style="font-size:9px; color:#555;">CNPJ: ${emp.cnpj || '—'}</div>
        <div style="font-size:9px; color:#555;">${emp.endereco || ''}</div>
        <div style="font-size:13px; font-weight:700; margin-top:6px; text-decoration:underline;">RELATORIO DE ENCOMENDAS</div>
      </div>`

      const meta = `<div style="font-size:10px; margin-bottom:8px;">
        <div><strong>DATA DA VIAGEM: ${viagem?.id_viagem || ''}</strong></div>
        <div>IMPRESSO EM: ${dataHora}  |  FILTROS: ${filtroRota || 'Nenhum'}</div>
        <div><strong>PAGINA: 1/1</strong></div>
      </div>
      <hr style="border:none; border-top:2px solid #059669; margin-bottom:8px;">`

      let rows = ''
      filtradas.forEach(e => {
        const devedor = Math.max(0, (parseFloat(e.total_a_pagar) || 0) - (parseFloat(e.desconto) || 0) - (parseFloat(e.valor_pago) || 0))
        const statusPg = devedor <= 0.01 ? 'PAGO' : 'FALTA'
        const statusEnt = e.entregue ? 'ENTR.' : 'PEND.'
        const statusFull = `${statusPg} / ${statusEnt}`
        const statusColor = statusPg === 'PAGO' ? '#059669' : '#DC2626'
        rows += `<tr>
          <td style="padding:3px 6px; border-bottom:1px solid #ddd;">${e.numero_encomenda || ''}</td>
          <td style="padding:3px 6px; border-bottom:1px solid #ddd;">${(e.remetente || '').toUpperCase()}</td>
          <td style="padding:3px 6px; border-bottom:1px solid #ddd;">${(e.destinatario || '').toUpperCase()}</td>
          <td style="padding:3px 6px; border-bottom:1px solid #ddd;">${e.rota || '—'}</td>
          <td style="padding:3px 6px; border-bottom:1px solid #ddd; text-align:right;">${formatMoney(e.total_a_pagar)}</td>
          <td style="padding:3px 6px; border-bottom:1px solid #ddd; text-align:center; color:${statusColor}; font-weight:600;">${statusFull}</td>
          <td style="padding:3px 6px; border-bottom:1px solid #ddd;">${e.entregue && e.doc_recebedor ? 'OK' : ''}</td>
        </tr>`
      })

      const body = `${header}${meta}
      <table style="width:100%; border-collapse:collapse; font-size:10px;">
        <thead><tr style="background:#059669; color:#fff;">
          <th style="padding:4px 6px; text-align:left;">N°</th>
          <th style="padding:4px 6px; text-align:left;">REMETENTE</th>
          <th style="padding:4px 6px; text-align:left;">DESTINATARIO</th>
          <th style="padding:4px 6px; text-align:left;">ROTA</th>
          <th style="padding:4px 6px; text-align:right;">VALOR</th>
          <th style="padding:4px 6px; text-align:center;">STATUS</th>
          <th style="padding:4px 6px; text-align:center;">RECEBEDOR</th>
        </tr></thead>
        <tbody>${rows}</tbody>
      </table>
      <div style="margin-top:10px; padding:8px; border:1px solid #ccc; font-size:10px;">
        <div><strong>TOTAL LANCADO: &nbsp; ${formatMoney(totalLancado)}</strong></div>
        <div><strong>TOTAL DESCONTO: &nbsp; ${formatMoney(totalDesconto)}</strong></div>
        <div><strong>TOTAL RECEBIDO: &nbsp; ${formatMoney(totalRecebido)}</strong></div>
        <div style="color:#DC2626; font-weight:700;">TOTAL A RECEBER: ${formatMoney(totalAReceber)}</div>
      </div>`

      // Salvar HTML e abrir dialogo de orientacao
      setHtmlParaImprimir(body)
      setShowOrientacao(true)

    } catch (err) {
      alert('Erro ao gerar relatorio: ' + (err.message || err))
    }
  }

  function imprimirComOrientacao(orientacao) {
    const pageSize = orientacao === 'paisagem' ? 'A4 landscape' : 'A4 portrait'
    const html = `<!DOCTYPE html><html lang="pt-BR"><head><meta charset="UTF-8"><title>Relatorio Encomendas</title>
    <style>
      * { margin:0; padding:0; box-sizing:border-box; }
      body { font-family:Arial,sans-serif; padding:10mm; color:#111; }
      @page { size: ${pageSize}; margin: 8mm; }
    </style></head><body>${htmlParaImprimir}
    <script>window.onload = function() { window.print(); }</script></body></html>`
    printContent(html, 'Relatorio Encomendas')
    setShowOrientacao(false)
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

      {/* Modal orientacao */}
      {showOrientacao && (
        <div className="modal-overlay" onClick={() => setShowOrientacao(false)}>
          <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 400, textAlign: 'center' }}>
            <h3>Configuracao de Impressao</h3>
            <p style={{ margin: '16px 0', color: 'var(--text-soft)' }}>Escolha a orientacao do papel:</p>
            <div style={{ display: 'flex', gap: 12, justifyContent: 'center' }}>
              <button className="btn-primary" style={{ width: 'auto', padding: '10px 20px' }} onClick={() => imprimirComOrientacao('paisagem')}>
                Paisagem (Deitado)
              </button>
              <button className="btn-secondary" style={{ padding: '10px 20px' }} onClick={() => imprimirComOrientacao('retrato')}>
                Retrato (Em Pe)
              </button>
              <button className="btn-secondary" style={{ padding: '10px 20px' }} onClick={() => setShowOrientacao(false)}>
                Cancelar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

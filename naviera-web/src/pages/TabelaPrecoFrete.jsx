import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'
import { printContent } from '../utils/print.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}

export default function TabelaPrecoFrete() {
  const [itens, setItens] = useState([])
  const [loading, setLoading] = useState(false)
  const [selecionado, setSelecionado] = useState(null)
  const [nome, setNome] = useState('')
  const [precoNormal, setPrecoNormal] = useState('')
  const [precoDesconto, setPrecoDesconto] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [toast, setToast] = useState(null)

  function showToast(msg, type = 'success') {
    setToast({ msg, type }); setTimeout(() => setToast(null), 3500)
  }

  const carregar = useCallback(() => {
    setLoading(true)
    api.get('/cadastros/itens-frete')
      .then(data => setItens(Array.isArray(data) ? data : []))
      .catch(() => showToast('Erro ao carregar', 'error'))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { carregar() }, [carregar])

  function handleSelect(item) {
    setSelecionado(item)
    setNome(item.nome_item || '')
    setPrecoNormal(item.preco_padrao || item.preco_unitario_padrao || '')
    setPrecoDesconto(item.preco_unitario_desconto || '')
  }

  function handleNovo() {
    setSelecionado(null); setNome(''); setPrecoNormal(''); setPrecoDesconto('')
  }

  async function handleSalvar() {
    if (!nome.trim()) { showToast('Informe a descricao', 'error'); return }
    setSalvando(true)
    try {
      if (selecionado) {
        await api.put(`/cadastros/itens-frete/${selecionado.id || selecionado.id_item_frete}`, {
          nome_item: nome.trim().toUpperCase(), preco_padrao: parseFloat(precoNormal) || 0, preco_desconto: parseFloat(precoDesconto) || 0
        })
        showToast('Item atualizado')
      } else {
        await api.post('/cadastros/itens-frete', {
          nome_item: nome.trim().toUpperCase(), preco_padrao: parseFloat(precoNormal) || 0, preco_desconto: parseFloat(precoDesconto) || 0
        })
        showToast('Item salvo')
      }
      handleNovo(); carregar()
    } catch (err) { showToast(err.message || 'Erro', 'error') }
    finally { setSalvando(false) }
  }

  async function handleExcluir() {
    if (!selecionado) return
    if (!window.confirm(`Excluir "${selecionado.nome_item}"?`)) return
    try {
      await api.delete(`/cadastros/itens-frete/${selecionado.id || selecionado.id_item_frete}`)
      showToast('Item excluido'); handleNovo(); carregar()
    } catch (err) { showToast(err.message || 'Erro', 'error') }
  }

  async function handleImprimir() {
    let emp = {}
    try { const token = localStorage.getItem('token'); const res = await fetch('/api/cadastros/empresa', { headers: { Authorization: `Bearer ${token}` } }); if (res.ok) emp = await res.json() } catch {}
    const rows = itens.map((item, idx) => `<tr style="background:${idx % 2 === 0 ? '#fff' : '#f5f7fa'};">
      <td style="padding:5px 8px; border-bottom:1px solid #ddd;">${(item.nome_item || '').toUpperCase()}</td>
      <td style="padding:5px 8px; border-bottom:1px solid #ddd; text-align:right;">${formatMoney(item.preco_padrao || item.preco_unitario_padrao)}</td>
      <td style="padding:5px 8px; border-bottom:1px solid #ddd; text-align:right;">${formatMoney(item.preco_unitario_desconto)}</td>
    </tr>`).join('')
    const html = `<!DOCTYPE html><html><head><meta charset="UTF-8"><title>Tabela Precos Frete</title>
    <style>*{margin:0;padding:0;box-sizing:border-box}body{font-family:Arial,sans-serif;padding:15mm;color:#111}@page{size:A4;margin:12mm}table{width:100%;border-collapse:collapse}th{background:#059669;color:#fff;padding:6px 8px;text-align:left;font-size:11px}</style></head><body>
    <div style="text-align:center;margin-bottom:12px;"><div style="font-size:16px;font-weight:700;">${emp.nome_embarcacao ? 'F/B ' + emp.nome_embarcacao : 'NAVIERA'}</div><div style="font-size:9px;color:#555;">CNPJ: ${emp.cnpj || '—'} | ${emp.endereco || ''}</div><h2 style="font-size:14px;margin-top:10px;">Tabela de Precos de Frete</h2><hr style="border:none;border-top:2px solid #059669;margin:8px 0;"></div>
    <table><thead><tr><th>Descricao / Item</th><th style="text-align:right;width:120px;">Preco Normal</th><th style="text-align:right;width:120px;">Preco Desconto</th></tr></thead><tbody>${rows}</tbody></table>
    <div style="margin-top:12px;font-size:10px;color:#888;">Total: ${itens.length} itens | ${new Date().toLocaleString('pt-BR')}</div>
    <script>window.onload=function(){window.print()}</script></body></html>`
    printContent(html, 'Tabela Precos Frete')
  }

  const I = { padding: '10px 14px', fontSize: '0.9rem', background: 'var(--bg-soft)', border: '1px solid var(--border)', borderRadius: 6, color: 'var(--text)', fontFamily: 'Sora, sans-serif', width: '100%', boxSizing: 'border-box' }
  const L = { fontSize: '0.78rem', fontWeight: 700, color: 'var(--text)', display: 'block', marginBottom: 3 }

  return (
    <div className="card">
      <h2 style={{ marginBottom: 16 }}>Gerenciamento de Fretes</h2>

      {/* FORM — Adicionar / Editar */}
      <div style={{ borderTop: '2px solid var(--primary)', padding: '12px 0', marginBottom: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
          <span style={{ fontWeight: 700 }}>Adicionar / Editar Item</span>
          <span style={{ color: 'var(--primary)', fontSize: '0.82rem', fontStyle: 'italic' }}>
            {selecionado ? 'Modo: Edicao' : 'Modo: Insercao de Novo Item'}
          </span>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr auto auto', gap: 12, alignItems: 'end' }}>
          <div>
            <label style={L}>Descricao do Item:</label>
            <input style={I} value={nome} onChange={e => setNome(e.target.value)} />
          </div>
          <div>
            <label style={L}>Preco Normal (R$):</label>
            <input type="number" step="0.01" min="0" style={{ ...I, width: 140, textAlign: 'right', fontFamily: 'Space Mono, monospace', border: '1px solid var(--primary)' }}
              value={precoNormal} onChange={e => setPrecoNormal(e.target.value)} />
          </div>
          <div>
            <label style={L}>Preco c/ Desc. (R$):</label>
            <input type="number" step="0.01" min="0" style={{ ...I, width: 140, textAlign: 'right', fontFamily: 'Space Mono, monospace' }}
              value={precoDesconto} onChange={e => setPrecoDesconto(e.target.value)} />
          </div>
        </div>

        <div style={{ display: 'flex', gap: 8, marginTop: 12, justifyContent: 'center' }}>
          <button className="btn-secondary" style={{ padding: '10px 24px' }} onClick={handleNovo}>Novo (+)</button>
          <button className="btn-primary" style={{ width: 'auto', padding: '10px 24px' }} onClick={handleSalvar} disabled={salvando}>{salvando ? 'Salvando...' : 'SALVAR'}</button>
          <button className="btn-sm danger" style={{ padding: '10px 24px' }} onClick={handleExcluir}>Excluir</button>
        </div>
      </div>

      {/* TABELA */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
        <span style={{ fontWeight: 700 }}>Itens Cadastrados no Sistema:</span>
        <button className="btn-sm primary" onClick={handleImprimir}>Imprimir Tabela</button>
      </div>

      <div className="table-container">
        <table>
          <thead><tr>
            <th>Descricao / Item</th>
            <th style={{ width: 130, textAlign: 'right' }}>Preco Normal</th>
            <th style={{ width: 130, textAlign: 'right' }}>Preco Desconto</th>
          </tr></thead>
          <tbody>
            {loading ? <tr><td colSpan="3">Carregando...</td></tr>
            : itens.length === 0 ? <tr><td colSpan="3" style={{ textAlign: 'center', padding: 20, color: 'var(--text-muted)' }}>Nenhum item</td></tr>
            : itens.map((item, idx) => (
              <tr key={item.id || item.id_item_frete || idx}
                  className={`clickable ${(selecionado?.id || selecionado?.id_item_frete) === (item.id || item.id_item_frete) ? 'selected' : ''}`}
                  onClick={() => handleSelect(item)}>
                <td>{item.nome_item}</td>
                <td className="money" style={{ textAlign: 'right' }}>{formatMoney(item.preco_padrao || item.preco_unitario_padrao)}</td>
                <td className="money" style={{ textAlign: 'right' }}>{formatMoney(item.preco_unitario_desconto)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}
    </div>
  )
}

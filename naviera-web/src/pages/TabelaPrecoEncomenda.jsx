import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'
import { printContent } from '../utils/print.js'

function formatMoney(val) {
  return 'R$ ' + Number(val || 0).toFixed(2).replace('.', ',')
}

export default function TabelaPrecoEncomenda() {
  const [itens, setItens] = useState([])
  const [loading, setLoading] = useState(false)
  const [selecionado, setSelecionado] = useState(null)
  const [busca, setBusca] = useState('')
  const [toast, setToast] = useState(null)

  // Form para adicionar/editar
  const [formDescricao, setFormDescricao] = useState('')
  const [formUnidade, setFormUnidade] = useState('UN')
  const [formPreco, setFormPreco] = useState('')
  const [salvando, setSalvando] = useState(false)

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  const carregar = useCallback(() => {
    setLoading(true)
    api.get('/cadastros/itens-encomenda')
      .then(data => setItens(Array.isArray(data) ? data : []))
      .catch(() => showToast('Erro ao carregar itens', 'error'))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { carregar() }, [carregar])

  function handleSelectRow(item) {
    setSelecionado(item)
    setFormDescricao(item.nome_item || '')
    setFormUnidade(item.unidade_medida || 'UN')
    setFormPreco(item.preco_padrao || item.preco_unitario_padrao || '')
  }

  function handleNovo() {
    setSelecionado(null)
    setFormDescricao('')
    setFormUnidade('UN')
    setFormPreco('')
  }

  async function handleSalvar() {
    if (!formDescricao.trim()) { showToast('Informe a descricao', 'error'); return }
    setSalvando(true)
    try {
      const payload = { nome_item: formDescricao.trim().toUpperCase(), preco_padrao: parseFloat(formPreco) || 0 }
      if (selecionado) {
        await api.put(`/cadastros/itens-encomenda/${selecionado.id || selecionado.id_item_encomenda}`, payload)
        showToast('Item atualizado')
      } else {
        await api.post('/cadastros/itens-encomenda', payload)
        showToast('Item adicionado')
      }
      handleNovo()
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao salvar', 'error')
    } finally {
      setSalvando(false)
    }
  }

  async function handleExcluir() {
    if (!selecionado) { showToast('Selecione um item', 'error'); return }
    if (!window.confirm(`Excluir "${selecionado.nome_item}"?`)) return
    try {
      await api.delete(`/cadastros/itens-encomenda/${selecionado.id || selecionado.id_item_encomenda}`)
      showToast('Item excluido')
      handleNovo()
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao excluir', 'error')
    }
  }

  async function handleImprimir() {
    let emp = {}
    try {
      const token = localStorage.getItem('token')
      const res = await fetch('/api/cadastros/empresa', { headers: { Authorization: `Bearer ${token}` } })
      if (res.ok) emp = await res.json()
    } catch {}

    const rows = filtrados.map((item, idx) => `
      <tr style="background:${idx % 2 === 0 ? '#fff' : '#f5f7fa'}; border-bottom:1px solid #ddd;">
        <td style="padding:4px 8px;">${(item.nome_item || '').toUpperCase()}</td>
        <td style="padding:4px 8px; text-align:center;">${item.unidade_medida || 'UN'}</td>
        <td style="padding:4px 8px; text-align:right;">${formatMoney(item.preco_padrao || item.preco_unitario_padrao)}</td>
      </tr>
    `).join('')

    const html = `<!DOCTYPE html><html lang="pt-BR"><head><meta charset="UTF-8"><title>Tabela de Precos</title>
    <style>
      * { margin:0; padding:0; box-sizing:border-box; }
      body { font-family:Arial,sans-serif; padding:15mm; color:#111; }
      @page { size:A4; margin:12mm; }
      table { width:100%; border-collapse:collapse; }
      th { background:#059669; color:#fff; padding:6px 8px; text-align:left; font-size:11px; }
      td { font-size:11px; }
    </style></head><body>
      <div style="text-align:center; margin-bottom:12px;">
        <div style="font-size:16px; font-weight:700;">${emp.nome_embarcacao ? 'F/B ' + emp.nome_embarcacao : emp.companhia || 'NAVIERA'}</div>
        <div style="font-size:9px; color:#555;">CNPJ: ${emp.cnpj || '—'}</div>
        <div style="font-size:9px; color:#555;">${emp.endereco || ''}</div>
        <h2 style="font-size:14px; margin-top:10px;">Tabela de Precos de Encomenda</h2>
        <hr style="border:none; border-top:2px solid #059669; margin:8px 0;">
      </div>
      <table>
        <thead><tr>
          <th>Descricao</th>
          <th style="text-align:center; width:60px;">Un.</th>
          <th style="text-align:right; width:100px;">Preco Padrao (R$)</th>
        </tr></thead>
        <tbody>${rows}</tbody>
      </table>
      <div style="margin-top:12px; font-size:10px; color:#888;">
        Total: ${filtrados.length} itens | Impresso em: ${new Date().toLocaleString('pt-BR')}
      </div>
      <script>window.onload = function() { window.print(); }</script>
    </body></html>`

    printContent(html, 'Tabela de Precos')
  }

  // Filtrar pela busca
  const filtrados = itens.filter(i =>
    !busca.trim() || (i.nome_item || '').toLowerCase().includes(busca.toLowerCase())
  )

  const I = { padding: '7px 10px', fontSize: '0.82rem', background: 'var(--bg-soft)', border: '1px solid var(--border)', borderRadius: 4, color: 'var(--text)', fontFamily: 'Sora, sans-serif', width: '100%', boxSizing: 'border-box' }

  return (
    <div className="card">
      <h2 style={{ marginBottom: 12 }}>Tabela de Precos de Encomenda</h2>

      {/* Barra de busca */}
      <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 12 }}>
        <span style={{ fontWeight: 600, fontSize: '0.82rem', whiteSpace: 'nowrap' }}>Pesquisar:</span>
        <input style={{ ...I, flex: 1 }} placeholder="Digite parte da descricao" value={busca} onChange={e => setBusca(e.target.value)} />
        <div style={{ display: 'flex', gap: 6 }}>
          <button className="btn-sm" onClick={handleNovo}>Adicionar</button>
          <button className="btn-sm" onClick={() => { if (selecionado) handleSelectRow(selecionado) }}>Editar</button>
          <button className="btn-sm danger" onClick={handleExcluir}>Excluir</button>
          <button className="btn-sm primary" onClick={handleImprimir}>Imprimir Tabela</button>
        </div>
      </div>

      {/* Tabela */}
      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>Descricao</th>
              <th style={{ width: 100 }}>Unidade</th>
              <th style={{ width: 150 }}>Preco Padrao (R$)</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="3">Carregando...</td></tr>
            ) : filtrados.length === 0 ? (
              <tr><td colSpan="3" style={{ textAlign: 'center', padding: 20, color: 'var(--text-muted)' }}>Nenhum item encontrado</td></tr>
            ) : filtrados.map((item, idx) => (
              <tr key={item.id || item.id_item_encomenda || idx}
                  className={`clickable ${(selecionado?.id || selecionado?.id_item_encomenda) === (item.id || item.id_item_encomenda) ? 'selected' : ''}`}
                  onClick={() => handleSelectRow(item)}>
                <td style={{ textTransform: 'uppercase' }}>{item.nome_item}</td>
                <td>{item.unidade_medida || 'UN'}</td>
                <td className="money">{formatMoney(item.preco_padrao || item.preco_unitario_padrao)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Form inline para adicionar/editar (aparece abaixo) */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 100px 120px auto', gap: 8, marginTop: 12, alignItems: 'end' }}>
        <div>
          <label style={{ fontSize: '0.7rem', fontWeight: 600, color: 'var(--text-muted)', display: 'block', marginBottom: 2 }}>Descricao:</label>
          <input style={I} value={formDescricao} onChange={e => setFormDescricao(e.target.value)} placeholder="Nome do item..." />
        </div>
        <div>
          <label style={{ fontSize: '0.7rem', fontWeight: 600, color: 'var(--text-muted)', display: 'block', marginBottom: 2 }}>Unidade:</label>
          <select style={I} value={formUnidade} onChange={e => setFormUnidade(e.target.value)}>
            <option value="UN">UN</option>
            <option value="FD">FD</option>
            <option value="CX">CX</option>
            <option value="PC">PC</option>
            <option value="KG">KG</option>
          </select>
        </div>
        <div>
          <label style={{ fontSize: '0.7rem', fontWeight: 600, color: 'var(--text-muted)', display: 'block', marginBottom: 2 }}>Preco (R$):</label>
          <input style={{ ...I, textAlign: 'right', fontFamily: 'Space Mono, monospace' }} type="number" step="0.01" min="0" value={formPreco} onChange={e => setFormPreco(e.target.value)} />
        </div>
        <button className="btn-primary" style={{ width: 'auto', padding: '8px 16px' }} onClick={handleSalvar} disabled={salvando}>
          {salvando ? 'Salvando...' : selecionado ? 'Atualizar' : 'Salvar'}
        </button>
      </div>

      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}
    </div>
  )
}

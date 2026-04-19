import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'
import MoneyInput from '../components/MoneyInput.jsx'

function formatMoney(val) {
  return 'R$ ' + Number(val || 0).toFixed(2).replace('.', ',')
}

const TABS = [
  { key: 'frete', label: 'Itens de Frete', endpoint: '/cadastros/itens-frete', idField: 'id' },
  { key: 'encomenda', label: 'Itens de Encomenda', endpoint: '/cadastros/itens-encomenda', idField: 'id' }
]

export default function CadastroItem() {
  const [tabAtiva, setTabAtiva] = useState('frete')
  const [itens, setItens] = useState([])
  const [loading, setLoading] = useState(false)
  const [selecionado, setSelecionado] = useState(null)
  const [form, setForm] = useState({ nome_item: '', preco_normal: '', preco_desconto: '', ativo: true })
  const [salvando, setSalvando] = useState(false)
  const [toast, setToast] = useState(null)

  const tab = TABS.find(t => t.key === tabAtiva)

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  const carregar = useCallback(() => {
    setLoading(true)
    api.get(tab.endpoint)
      .then(data => setItens(Array.isArray(data) ? data : []))
      .catch(() => showToast('Erro ao carregar itens', 'error'))
      .finally(() => setLoading(false))
  }, [tab.endpoint])

  useEffect(() => { carregar() }, [carregar])

  function handleSelectRow(item) {
    setSelecionado(item)
    setForm({
      nome_item: item.nome_item || '',
      preco_normal: item.preco_padrao ?? item.preco_unitario_padrao ?? '',
      preco_desconto: item.preco_unitario_desconto ?? item.preco_desconto ?? '',
      ativo: item.ativo !== false
    })
  }

  function handleNovo() {
    setSelecionado(null)
    setForm({ nome_item: '', preco_normal: '', preco_desconto: '', ativo: true })
  }

  function handleTabChange(key) {
    setTabAtiva(key)
    setSelecionado(null)
    setForm({ nome_item: '', preco_normal: '', preco_desconto: '', ativo: true })
  }

  function handleChange(e) {
    const { name, value, type, checked } = e.target
    setForm(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }))
  }

  async function handleSalvar() {
    if (!form.nome_item.trim()) { showToast('Informe o nome do item', 'error'); return }
    setSalvando(true)
    try {
      const payload = {
        nome_item: form.nome_item.trim(),
        preco_padrao: parseFloat(form.preco_normal) || 0
      }
      if (selecionado) {
        await api.put(`${tab.endpoint}/${selecionado[tab.idField]}`, payload)
        showToast('Item atualizado com sucesso')
      } else {
        await api.post(tab.endpoint, payload)
        showToast('Item criado com sucesso')
      }
      handleNovo()
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao salvar item', 'error')
    } finally {
      setSalvando(false)
    }
  }

  async function handleExcluir() {
    if (!selecionado) { showToast('Selecione um item na tabela', 'error'); return }
    if (!window.confirm(`Desativar item "${selecionado.nome_item}"?`)) return
    try {
      await api.delete(`${tab.endpoint}/${selecionado[tab.idField]}`)
      showToast('Item desativado com sucesso')
      handleNovo()
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao desativar item', 'error')
    }
  }

  return (
    <div className="card">
      <h2 style={{ marginBottom: 16 }}>Itens</h2>

      <div className="tab-nav">
        {TABS.map(t => (
          <button key={t.key}
                  className={tabAtiva === t.key ? 'active' : ''}
                  onClick={() => handleTabChange(t.key)}>
            {t.label}
          </button>
        ))}
      </div>

      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>Nome do Item</th>
              <th>Valor Normal (R$)</th>
              <th>Valor com Desconto (R$)</th>
              <th style={{ width: 60 }}>Ativo</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="4">Carregando...</td></tr>
            ) : itens.length === 0 ? (
              <tr><td colSpan="4">Nenhum item cadastrado</td></tr>
            ) : itens.map((item, idx) => (
              <tr key={item[tab.idField] || idx}
                  className={`clickable ${selecionado?.[tab.idField] === item[tab.idField] ? 'selected' : ''}`}
                  onClick={() => handleSelectRow(item)}>
                <td>{item.nome_item}</td>
                <td className="money">{formatMoney(item.preco_padrao ?? item.preco_unitario_padrao)}</td>
                <td className="money">{formatMoney(item.preco_unitario_desconto ?? item.preco_desconto ?? 0)}</td>
                <td>
                  <input type="checkbox" checked={item.ativo !== false} readOnly style={{ pointerEvents: 'none' }} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="cadastro-inline-form" style={{ maxWidth: 500 }}>
        <label>Nome do Item:</label>
        <input type="text" name="nome_item" value={form.nome_item} onChange={handleChange} />

        <label>Valor Normal:</label>
        <MoneyInput value={form.preco_normal} onChange={v => setForm(prev => ({ ...prev, preco_normal: v }))} />

        <label>Valor c/ Desconto:</label>
        <MoneyInput value={form.preco_desconto} onChange={v => setForm(prev => ({ ...prev, preco_desconto: v }))} />

        <label>Ativo:</label>
        <div>
          <input type="checkbox" name="ativo" checked={form.ativo} onChange={handleChange} style={{ width: 18, height: 18, accentColor: 'var(--primary)' }} />
        </div>
      </div>

      <div className="cadastro-buttons">
        <button onClick={handleNovo}>Novo</button>
        <button onClick={() => selecionado && handleSelectRow(selecionado)}>Editar</button>
        <button onClick={handleExcluir}>Excluir</button>
        <button onClick={handleSalvar} disabled={salvando}>
          {salvando ? 'Salvando...' : 'Salvar'}
        </button>
      </div>

      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}
    </div>
  )
}

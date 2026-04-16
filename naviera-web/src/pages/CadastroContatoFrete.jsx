import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

export default function CadastroContatoFrete() {
  const [contatos, setContatos] = useState([])
  const [loading, setLoading] = useState(false)
  const [selecionado, setSelecionado] = useState(null)
  const [nome, setNome] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [toast, setToast] = useState(null)

  function showToast(msg, type = 'success') {
    setToast({ msg, type }); setTimeout(() => setToast(null), 3500)
  }

  const carregar = useCallback(() => {
    setLoading(true)
    api.get('/fretes/contatos')
      .then(setContatos)
      .catch(() => showToast('Erro ao carregar', 'error'))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { carregar() }, [carregar])

  function handleSelect(item) {
    setSelecionado(item)
    setNome(item.nome_razao_social || '')
  }

  function handleNovo() { setSelecionado(null); setNome('') }

  async function handleSalvar() {
    if (!nome.trim()) { showToast('Informe o nome', 'error'); return }
    setSalvando(true)
    try {
      await api.post('/fretes/contatos', { nome: nome.trim().toUpperCase() })
      showToast('Contato salvo')
      handleNovo(); carregar()
    } catch (err) { showToast(err.message || 'Erro', 'error') }
    finally { setSalvando(false) }
  }

  const I = { padding: '10px 14px', fontSize: '0.9rem', background: 'var(--bg-soft)', border: '1px solid var(--border)', borderRadius: 6, color: 'var(--text)', fontFamily: 'Sora, sans-serif', width: '100%', boxSizing: 'border-box' }

  return (
    <div className="card">
      <h2 style={{ textAlign: 'center', marginBottom: 16 }}>Cadastro de Clientes (Frete)</h2>

      <div style={{ display: 'flex', gap: 24 }}>
        {/* LISTA ESQUERDA */}
        <div style={{ flex: 1, maxHeight: 500, overflowY: 'auto', border: '1px solid var(--border)', borderRadius: 6 }}>
          {loading ? <div style={{ padding: 20, color: 'var(--text-muted)' }}>Carregando...</div> :
          contatos.map((c, idx) => (
            <div key={c.id}
              onClick={() => handleSelect(c)}
              style={{
                padding: '10px 14px', cursor: 'pointer', fontSize: '0.85rem', fontWeight: 600,
                textTransform: 'uppercase', letterSpacing: '0.02em',
                background: selecionado?.id === c.id ? 'var(--primary)' : idx % 2 === 0 ? 'transparent' : 'var(--bg-soft)',
                color: selecionado?.id === c.id ? '#fff' : 'var(--text)',
                borderBottom: '1px solid var(--border)'
              }}>
              {c.nome_razao_social}
            </div>
          ))}
          {contatos.length === 0 && <div style={{ padding: 20, color: 'var(--text-muted)', textAlign: 'center' }}>Nenhum contato</div>}
        </div>

        {/* FORM DIREITA */}
        <div style={{ width: 300, flexShrink: 0 }}>
          <label style={{ fontSize: '0.82rem', fontWeight: 700, color: 'var(--text)', display: 'block', marginBottom: 6 }}>Nome do Cliente:</label>
          <input style={I} value={nome} onChange={e => setNome(e.target.value)} placeholder="" />

          <div style={{ display: 'flex', gap: 8, marginTop: 16 }}>
            <button className="btn-secondary" style={{ flex: 1, padding: '10px' }} onClick={handleNovo}>Novo</button>
            <button className="btn-primary" style={{ flex: 1, padding: '10px' }} onClick={handleSalvar} disabled={salvando}>{salvando ? 'Salvando...' : 'Salvar'}</button>
          </div>
        </div>
      </div>

      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}
    </div>
  )
}

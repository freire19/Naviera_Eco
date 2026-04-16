import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

export default function CadastroPassageiro() {
  const [passageiros, setPassageiros] = useState([])
  const [loading, setLoading] = useState(false)
  const [selecionado, setSelecionado] = useState(null)
  const [nome, setNome] = useState('')
  const [toast, setToast] = useState(null)

  function showToast(msg, type = 'success') {
    setToast({ msg, type }); setTimeout(() => setToast(null), 3500)
  }

  const carregar = useCallback(() => {
    setLoading(true)
    api.get('/passagens/busca-passageiro?q=')
      .then(data => setPassageiros(Array.isArray(data) ? data : []))
      .catch(() => {
        // Endpoint retorna vazio se q < 2 chars, tentar com 'a'
        api.get('/passagens/busca-passageiro?q=a').then(data => setPassageiros(Array.isArray(data) ? data : [])).catch(() => {})
      })
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { carregar() }, [carregar])

  function handleSelect(item) {
    setSelecionado(item)
    setNome(item.nome_passageiro || '')
  }

  function handleNovo() { setSelecionado(null); setNome('') }

  // Busca ao digitar
  useEffect(() => {
    if (nome.length < 2) return
    const timer = setTimeout(() => {
      api.get(`/passagens/busca-passageiro?q=${encodeURIComponent(nome)}`)
        .then(data => setPassageiros(Array.isArray(data) ? data : []))
        .catch(() => {})
    }, 300)
    return () => clearTimeout(timer)
  }, [nome])

  const I = { padding: '10px 14px', fontSize: '0.9rem', background: 'var(--bg-soft)', border: '1px solid var(--border)', borderRadius: 6, color: 'var(--text)', fontFamily: 'Sora, sans-serif', width: '100%', boxSizing: 'border-box' }

  return (
    <div className="card">
      <h2 style={{ textAlign: 'center', marginBottom: 16 }}>Cadastro de Passageiros</h2>

      <div style={{ display: 'flex', gap: 24 }}>
        {/* LISTA ESQUERDA */}
        <div style={{ flex: 1, maxHeight: 500, overflowY: 'auto', border: '1px solid var(--border)', borderRadius: 6 }}>
          {loading ? <div style={{ padding: 20, color: 'var(--text-muted)' }}>Carregando...</div> :
          passageiros.map((p, idx) => (
            <div key={p.id_passageiro}
              onClick={() => handleSelect(p)}
              style={{
                padding: '10px 14px', cursor: 'pointer', fontSize: '0.85rem', fontWeight: 600,
                textTransform: 'uppercase', letterSpacing: '0.02em',
                background: selecionado?.id_passageiro === p.id_passageiro ? 'var(--primary)' : idx % 2 === 0 ? 'transparent' : 'var(--bg-soft)',
                color: selecionado?.id_passageiro === p.id_passageiro ? '#fff' : 'var(--text)',
                borderBottom: '1px solid var(--border)'
              }}>
              {p.nome_passageiro}
              {p.numero_documento && <span style={{ float: 'right', opacity: 0.6, fontSize: '0.78rem' }}>{p.numero_documento}</span>}
            </div>
          ))}
          {passageiros.length === 0 && <div style={{ padding: 20, color: 'var(--text-muted)', textAlign: 'center' }}>Nenhum passageiro. Digite para buscar.</div>}
        </div>

        {/* FORM DIREITA */}
        <div style={{ width: 300, flexShrink: 0 }}>
          <label style={{ fontSize: '0.82rem', fontWeight: 700, color: 'var(--text)', display: 'block', marginBottom: 6 }}>Nome do Passageiro:</label>
          <input style={I} value={nome} onChange={e => setNome(e.target.value)} placeholder="Digite para buscar..." />

          <div style={{ display: 'flex', gap: 8, marginTop: 16 }}>
            <button className="btn-secondary" style={{ flex: 1, padding: '10px' }} onClick={handleNovo}>Novo</button>
          </div>

          {selecionado && (
            <div style={{ marginTop: 16, fontSize: '0.82rem', color: 'var(--text-soft)' }}>
              <div><strong>Doc:</strong> {selecionado.numero_documento || '—'}</div>
              <div><strong>Nasc:</strong> {selecionado.data_nascimento || '—'}</div>
            </div>
          )}
        </div>
      </div>

      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}
    </div>
  )
}

import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

function formatDate(val) {
  if (!val) return '—'
  const s = String(val)
  if (/^\d{2}\/\d{2}\/\d{4}$/.test(s)) return s
  try { const p = (s.includes('T') ? s.substring(0,10) : s).split('-'); return p.length === 3 ? `${p[2]}/${p[1]}/${p[0]}` : s } catch { return s }
}

export default function CadastroPassageiro() {
  const [passageiros, setPassageiros] = useState([])
  const [loading, setLoading] = useState(false)
  const [selecionado, setSelecionado] = useState(null)
  const [busca, setBusca] = useState('')
  const [toast, setToast] = useState(null)

  function showToast(msg, type = 'success') {
    setToast({ msg, type }); setTimeout(() => setToast(null), 3500)
  }

  const carregar = useCallback(() => {
    setLoading(true)
    api.get('/passagens/passageiros')
      .then(data => setPassageiros(Array.isArray(data) ? data : []))
      .catch(() => showToast('Erro ao carregar', 'error'))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { carregar() }, [carregar])

  function handleSelect(p) { setSelecionado(p) }
  function handleNovo() { setSelecionado(null); setBusca('') }

  // Filtro local
  const filtrados = passageiros.filter(p => {
    if (!busca.trim()) return true
    const q = busca.toLowerCase()
    return (p.nome_passageiro || '').toLowerCase().includes(q) || (p.numero_documento || '').includes(q)
  })

  const I = { padding: '8px 12px', fontSize: '0.85rem', background: 'var(--bg-soft)', border: '1px solid var(--border)', borderRadius: 6, color: 'var(--text)', fontFamily: 'Sora, sans-serif', width: '100%', boxSizing: 'border-box' }
  const L = { fontSize: '0.75rem', fontWeight: 700, color: 'var(--text)', marginBottom: 3, display: 'block' }

  return (
    <div className="card">
      <h2 style={{ textAlign: 'center', marginBottom: 16 }}>Cadastro de Passageiros</h2>

      <div style={{ display: 'flex', gap: 24 }}>
        {/* LISTA ESQUERDA */}
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ marginBottom: 8 }}>
            <input style={I} placeholder="Buscar por nome ou documento..." value={busca} onChange={e => setBusca(e.target.value)} />
          </div>
          <div style={{ maxHeight: 450, overflowY: 'auto', border: '1px solid var(--border)', borderRadius: 6 }}>
            {loading ? <div style={{ padding: 20, color: 'var(--text-muted)' }}>Carregando...</div> :
            filtrados.length === 0 ? <div style={{ padding: 20, color: 'var(--text-muted)', textAlign: 'center' }}>Nenhum passageiro encontrado</div> :
            filtrados.map((p, idx) => (
              <div key={p.id_passageiro}
                onClick={() => handleSelect(p)}
                style={{
                  padding: '8px 14px', cursor: 'pointer', fontSize: '0.82rem', fontWeight: 600,
                  textTransform: 'uppercase', display: 'flex', justifyContent: 'space-between',
                  background: selecionado?.id_passageiro === p.id_passageiro ? 'var(--primary)' : idx % 2 === 0 ? 'transparent' : 'var(--bg-soft)',
                  color: selecionado?.id_passageiro === p.id_passageiro ? '#fff' : 'var(--text)',
                  borderBottom: '1px solid var(--border)'
                }}>
                <span>{p.nome_passageiro}</span>
                <span style={{ opacity: 0.6, fontSize: '0.75rem', fontWeight: 400 }}>{p.numero_documento || ''}</span>
              </div>
            ))}
          </div>
        </div>

        {/* DETALHES DIREITA */}
        <div style={{ width: 320, flexShrink: 0 }}>
          {selecionado ? (
            <div>
              <div style={{ marginBottom: 10 }}>
                <label style={L}>Nome Completo:</label>
                <input style={I} value={selecionado.nome_passageiro || ''} readOnly />
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, marginBottom: 10 }}>
                <div>
                  <label style={L}>Documento:</label>
                  <input style={I} value={selecionado.numero_documento || '—'} readOnly />
                </div>
                <div>
                  <label style={L}>Tipo Doc:</label>
                  <input style={I} value={selecionado.nome_tipo_doc || '—'} readOnly />
                </div>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, marginBottom: 10 }}>
                <div>
                  <label style={L}>Data Nascimento:</label>
                  <input style={I} value={formatDate(selecionado.data_nascimento)} readOnly />
                </div>
                <div>
                  <label style={L}>Sexo:</label>
                  <input style={I} value={selecionado.nome_sexo || '—'} readOnly />
                </div>
              </div>
              <div style={{ marginBottom: 10 }}>
                <label style={L}>Nacionalidade:</label>
                <input style={I} value={selecionado.nome_nacionalidade || '—'} readOnly />
              </div>

              <div style={{ display: 'flex', gap: 8, marginTop: 16 }}>
                <button className="btn-secondary" style={{ flex: 1, padding: '10px' }} onClick={handleNovo}>Limpar</button>
              </div>
            </div>
          ) : (
            <div style={{ color: 'var(--text-muted)', textAlign: 'center', padding: 40 }}>
              Selecione um passageiro na lista para ver os detalhes.
            </div>
          )}
        </div>
      </div>

      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}
    </div>
  )
}

import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

export default function CadastroContatoFrete() {
  const [contatos, setContatos] = useState([])
  const [loading, setLoading] = useState(false)
  const [selecionado, setSelecionado] = useState(null)
  const [salvando, setSalvando] = useState(false)
  const [toast, setToast] = useState(null)
  const [busca, setBusca] = useState('')

  // Form fields
  const [nome, setNome] = useState('')
  const [razaoSocial, setRazaoSocial] = useState('')
  const [cpfCnpj, setCpfCnpj] = useState('')
  const [endereco, setEndereco] = useState('')
  const [inscricaoEstadual, setInscricaoEstadual] = useState('')
  const [email, setEmail] = useState('')
  const [telefone, setTelefone] = useState('')

  function showToast(msg, type = 'success') {
    setToast({ msg, type }); setTimeout(() => setToast(null), 3500)
  }

  const carregar = useCallback(() => {
    setLoading(true)
    api.get('/fretes/contatos')
      .then(data => setContatos(Array.isArray(data) ? data : []))
      .catch(() => showToast('Erro ao carregar', 'error'))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { carregar() }, [carregar])

  function limparForm() {
    setSelecionado(null)
    setNome(''); setRazaoSocial(''); setCpfCnpj(''); setEndereco('')
    setInscricaoEstadual(''); setEmail(''); setTelefone('')
  }

  function handleSelect(item) {
    setSelecionado(item)
    setNome(item.nome_razao_social || item.nome_cliente || '')
    setRazaoSocial(item.razao_social || '')
    setCpfCnpj(item.cpf_cnpj || '')
    setEndereco(item.endereco || '')
    setInscricaoEstadual(item.inscricao_estadual || '')
    setEmail(item.email || '')
    setTelefone(item.telefone || '')
  }

  async function handleSalvar() {
    if (!nome.trim()) { showToast('Informe o nome do cliente', 'error'); return }
    setSalvando(true)
    try {
      const payload = {
        nome: nome.trim(),
        razao_social: razaoSocial.trim() || null,
        cpf_cnpj: cpfCnpj.trim() || null,
        endereco: endereco.trim() || null,
        inscricao_estadual: inscricaoEstadual.trim() || null,
        email: email.trim() || null,
        telefone: telefone.trim() || null
      }
      if (selecionado?.id) {
        await api.put(`/fretes/contatos/${selecionado.id}`, payload)
        showToast('Cliente atualizado')
      } else {
        await api.post('/fretes/contatos', payload)
        showToast('Cliente cadastrado')
      }
      limparForm()
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao salvar', 'error')
    } finally {
      setSalvando(false)
    }
  }

  async function handleExcluir() {
    if (!selecionado?.id) { showToast('Selecione um cliente para excluir', 'error'); return }
    if (!confirm(`Excluir cliente "${selecionado.nome_razao_social}"? Esta acao nao pode ser desfeita.`)) return
    try {
      await api.delete(`/fretes/contatos/${selecionado.id}`)
      showToast('Cliente excluido')
      limparForm()
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao excluir', 'error')
    }
  }

  // Filtrar lista por busca
  const filtrados = busca
    ? contatos.filter(c => (c.nome_razao_social || '').toLowerCase().includes(busca.toLowerCase()) || (c.cpf_cnpj || '').includes(busca))
    : contatos

  const I = { padding: '8px 10px', fontSize: '0.82rem', background: 'var(--bg-soft)', border: '1px solid var(--border)', borderRadius: 4, color: 'var(--text)', fontFamily: 'Sora, sans-serif', width: '100%', boxSizing: 'border-box' }
  const L = { fontSize: '0.72rem', fontWeight: 700, color: 'var(--text)', marginBottom: 2, display: 'block', marginTop: 8 }

  return (
    <div className="card" style={{ padding: 16 }}>
      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}

      <h2 style={{ textAlign: 'center', marginBottom: 14, fontSize: '1.05rem' }}>Cadastro de Clientes (Frete)</h2>

      <div style={{ display: 'flex', gap: 16 }}>
        {/* LISTA ESQUERDA */}
        <div style={{ width: 320, flexShrink: 0 }}>
          <input style={{ ...I, marginBottom: 8 }} placeholder="Buscar cliente..." value={busca} onChange={e => setBusca(e.target.value)} />
          <div style={{ maxHeight: 500, overflowY: 'auto', border: '1px solid var(--border)', borderRadius: 6 }}>
            {loading ? <div style={{ padding: 20, color: 'var(--text-muted)' }}>Carregando...</div> :
            filtrados.length === 0 ? <div style={{ padding: 20, color: 'var(--text-muted)', textAlign: 'center' }}>Nenhum cliente</div> :
            filtrados.map((c, idx) => (
              <div key={c.id || idx}
                onClick={() => handleSelect(c)}
                style={{
                  padding: '8px 12px', cursor: 'pointer', fontSize: '0.8rem',
                  background: selecionado?.id === c.id ? 'var(--primary)' : idx % 2 === 0 ? 'transparent' : 'var(--bg-soft)',
                  color: selecionado?.id === c.id ? '#fff' : 'var(--text)',
                  borderBottom: '1px solid var(--border)', transition: 'background 0.15s'
                }}
                onMouseEnter={e => { if (selecionado?.id !== c.id) e.currentTarget.style.background = 'rgba(4,120,87,0.15)' }}
                onMouseLeave={e => { if (selecionado?.id !== c.id) e.currentTarget.style.background = idx % 2 === 0 ? 'transparent' : 'var(--bg-soft)' }}>
                <div style={{ fontWeight: 700 }}>{c.nome_razao_social}</div>
                {(c.cpf_cnpj || c.telefone) && (
                  <div style={{ fontSize: '0.7rem', opacity: 0.7, marginTop: 2 }}>
                    {c.cpf_cnpj && <span>CPF/CNPJ: {c.cpf_cnpj}</span>}
                    {c.cpf_cnpj && c.telefone && <span> | </span>}
                    {c.telefone && <span>Tel: {c.telefone}</span>}
                  </div>
                )}
              </div>
            ))}
          </div>
          <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: 4 }}>{filtrados.length} clientes</div>
        </div>

        {/* FORMULARIO DIREITA */}
        <div style={{ flex: 1 }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 14px' }}>
            <div style={{ gridColumn: '1 / -1' }}>
              <label style={L}>Nome do Cliente: *</label>
              <input style={I} value={nome} onChange={e => setNome(e.target.value)} placeholder="Nome ou fantasia" />
            </div>

            <div style={{ gridColumn: '1 / -1' }}>
              <label style={L}>Razao Social:</label>
              <input style={I} value={razaoSocial} onChange={e => setRazaoSocial(e.target.value)} placeholder="Razao social completa" />
            </div>

            <div>
              <label style={L}>CPF ou CNPJ:</label>
              <input style={I} value={cpfCnpj} onChange={e => setCpfCnpj(e.target.value)} placeholder="000.000.000-00" />
            </div>

            <div>
              <label style={L}>Inscricao Estadual:</label>
              <input style={I} value={inscricaoEstadual} onChange={e => setInscricaoEstadual(e.target.value)} placeholder="Inscricao estadual" />
            </div>

            <div style={{ gridColumn: '1 / -1' }}>
              <label style={L}>Endereco:</label>
              <input style={I} value={endereco} onChange={e => setEndereco(e.target.value)} placeholder="Rua, numero, bairro, cidade - UF" />
            </div>

            <div>
              <label style={L}>Email:</label>
              <input style={I} type="email" value={email} onChange={e => setEmail(e.target.value)} placeholder="email@exemplo.com" />
            </div>

            <div>
              <label style={L}>Telefone:</label>
              <input style={I} value={telefone} onChange={e => setTelefone(e.target.value)} placeholder="(00) 00000-0000" />
            </div>
          </div>

          <div style={{ display: 'flex', gap: 8, marginTop: 16, justifyContent: 'space-between' }}>
            <div style={{ display: 'flex', gap: 8 }}>
              <button style={{ padding: '8px 20px', background: '#047857', color: '#fff', border: 'none', borderRadius: 4, fontWeight: 700, cursor: 'pointer', fontSize: '0.82rem' }} onClick={limparForm}>Novo</button>
              <button style={{ padding: '8px 20px', background: '#047857', color: '#fff', border: 'none', borderRadius: 4, fontWeight: 700, cursor: 'pointer', fontSize: '0.82rem', opacity: selecionado ? 1 : 0.5 }} onClick={handleSalvar} disabled={!selecionado || salvando}>Editar</button>
              <button style={{ padding: '8px 20px', background: '#DC2626', color: '#fff', border: 'none', borderRadius: 4, fontWeight: 700, cursor: 'pointer', fontSize: '0.82rem', opacity: selecionado ? 1 : 0.5 }} onClick={handleExcluir} disabled={!selecionado}>Excluir</button>
            </div>
            <button style={{ padding: '8px 28px', background: '#059669', color: '#fff', border: 'none', borderRadius: 4, fontWeight: 700, cursor: 'pointer', fontSize: '0.85rem' }} onClick={handleSalvar} disabled={salvando}>
              {salvando ? 'Salvando...' : 'Salvar'}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

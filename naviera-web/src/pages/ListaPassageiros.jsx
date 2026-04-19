import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'
import { printListaPassageiros } from '../utils/print.js'

function formatDate(val) {
  if (!val) return '—'
  const s = String(val)
  if (s.includes('/')) return s
  try {
    const d = new Date(s.includes('T') ? s : s + 'T00:00:00')
    if (isNaN(d.getTime())) return '—'
    return d.toLocaleDateString('pt-BR')
  } catch { return '—' }
}

function calcIdade(dataNasc) {
  if (!dataNasc) return ''
  try {
    const nasc = new Date(String(dataNasc).includes('T') ? dataNasc : dataNasc + 'T00:00:00')
    if (isNaN(nasc.getTime())) return ''
    const hoje = new Date()
    let idade = hoje.getFullYear() - nasc.getFullYear()
    const m = hoje.getMonth() - nasc.getMonth()
    if (m < 0 || (m === 0 && hoje.getDate() < nasc.getDate())) idade--
    return idade >= 0 ? idade : ''
  } catch { return '' }
}

export default function ListaPassageiros({ viagemAtiva, onNavigate, onClose }) {
  const [passagens, setPassagens] = useState([])
  const [loading, setLoading] = useState(false)
  const [viagens, setViagens] = useState([])
  const [viagemSel, setViagemSel] = useState(null)

  // Carrega lista de viagens + inicializa com ativa
  useEffect(() => { api.get('/viagens').then(setViagens).catch(() => {}) }, [])
  useEffect(() => { if (viagemAtiva) setViagemSel(viagemAtiva) }, [viagemAtiva])

  const carregar = useCallback(() => {
    if (!viagemSel) return
    setLoading(true)
    api.get(`/passagens?viagem_id=${viagemSel.id_viagem}`)
      .then(setPassagens)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [viagemSel])

  useEffect(() => { carregar() }, [carregar])

  function handleViagemChange(e) {
    const id = e.target.value
    const v = viagens.find(v => String(v.id_viagem) === id)
    setViagemSel(v || null)
  }

  if (!viagemAtiva && !viagemSel) {
    return (
      <div className="placeholder-page">
        <div className="ph-icon">👥</div>
        <h2>Lista de Passageiros</h2>
        <p>Selecione uma viagem para ver os passageiros.</p>
      </div>
    )
  }

  const v = viagemSel || viagemAtiva
  const vData = formatDate(v.data_viagem)
  const vCheg = formatDate(v.data_chegada)
  const vHora = v.horario || '—'
  const vEmb = v.nome_embarcacao || '—'

  return (
    <div className="card">
      <h2 style={{ textAlign: 'center', marginBottom: 8 }}>Lista de Passageiros por Viagem</h2>

      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12, flexWrap: 'wrap' }}>
        <strong style={{ fontSize: '0.85rem' }}>Filtrar Viagem:</strong>
        <select value={viagemSel?.id_viagem || ''} onChange={handleViagemChange}
          style={{ padding: '6px 10px', borderRadius: 4, border: '1px solid var(--border)', background: 'var(--bg-soft)', color: 'var(--text)', fontSize: '0.85rem', minWidth: 280 }}>
          <option value="">Selecione...</option>
          {viagens.map(vi => (
            <option key={vi.id_viagem} value={vi.id_viagem}>
              {vi.id_viagem} - {vi.data_viagem}{vi.ativa ? ' (ATIVA)' : ''}
              {vi.nome_rota ? ` (${vi.nome_rota})` : ''}
            </option>
          ))}
        </select>
      </div>

      <p style={{ marginBottom: 16, fontSize: '0.85rem' }}>
        <strong>Viagem:</strong>{' '}
        Embarcacao: {vEmb} | Saida: {vData} | Prev. Chegada: {vCheg} | Hora: {vHora}
      </p>

      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th style={{ width: 50 }}>ORD</th>
              <th>Nome Completo</th>
              <th>Dt. Nascimento</th>
              <th style={{ width: 60 }}>Idade</th>
              <th>RG</th>
              <th>Origem</th>
              <th>Destino</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="7">Carregando...</td></tr>
            ) : passagens.length === 0 ? (
              <tr><td colSpan="7" style={{ textAlign: 'center', padding: 30, color: 'var(--text-muted)' }}>Nenhum passageiro nesta viagem</td></tr>
            ) : passagens.map((p, i) => (
              <tr key={p.id_passagem}>
                <td>{i + 1}</td>
                <td>{(p.nome_passageiro || '—').toUpperCase()}</td>
                <td>{formatDate(p.data_nascimento)}</td>
                <td>{calcIdade(p.data_nascimento)}</td>
                <td>{p.numero_doc || '—'}</td>
                <td>{p.origem || '—'}</td>
                <td>{p.destino || '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 16 }}>
        <button className="btn-primary" style={{ width: 'auto', padding: '10px 24px' }}
                onClick={() => printListaPassageiros(passagens, viagemAtiva)}>
          Imprimir Lista
        </button>
        <button className="btn-secondary" style={{ padding: '10px 24px' }}
                onClick={() => onClose ? onClose() : onNavigate && onNavigate('vender-passagem')}>
          Sair
        </button>
      </div>
    </div>
  )
}

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

export default function ListaPassageiros({ viagemAtiva, onNavigate }) {
  const [passagens, setPassagens] = useState([])
  const [loading, setLoading] = useState(false)

  const carregar = useCallback(() => {
    if (!viagemAtiva) return
    setLoading(true)
    api.get(`/passagens?viagem_id=${viagemAtiva.id_viagem}`)
      .then(setPassagens)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [viagemAtiva])

  useEffect(() => { carregar() }, [carregar])

  if (!viagemAtiva) {
    return (
      <div className="placeholder-page">
        <div className="ph-icon">👥</div>
        <h2>Lista de Passageiros</h2>
        <p>Selecione uma viagem para ver os passageiros.</p>
      </div>
    )
  }

  const vData = formatDate(viagemAtiva.data_viagem)
  const vCheg = formatDate(viagemAtiva.data_chegada)
  const vHora = viagemAtiva.horario || '—'
  const vEmb = viagemAtiva.nome_embarcacao || '—'
  const vRota = viagemAtiva.nome_rota || [viagemAtiva.origem, viagemAtiva.destino].filter(Boolean).join(' - ')

  return (
    <div className="card">
      <h2 style={{ textAlign: 'center', marginBottom: 8 }}>Lista de Passageiros por Viagem</h2>

      <p style={{ marginBottom: 16, fontSize: '0.85rem' }}>
        <strong>Viagem Ativa:</strong>{' '}
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
        {onNavigate && (
          <button className="btn-secondary" style={{ padding: '10px 24px' }}
                  onClick={() => onNavigate('vender-passagem')}>
            Sair
          </button>
        )}
      </div>
    </div>
  )
}

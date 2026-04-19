import { useState, useEffect } from 'react'
import { api } from '../api.js'

const FREQS = [
  { value: 1, label: 'Diaria (todo dia)' },
  { value: 2, label: 'Alternada (2 em 2 dias)' },
  { value: 7, label: 'Semanal (7 dias)' },
  { value: 14, label: 'Quinzenal (14 dias)' },
  { value: 30, label: 'Mensal (30 dias)' }
]

function todayISO() { return new Date().toISOString().substring(0, 10) }
function addMesesISO(n) {
  const d = new Date()
  d.setMonth(d.getMonth() + n)
  return d.toISOString().substring(0, 10)
}

export default function GerarEscala({ onClose }) {
  const [embarcacoes, setEmbarcacoes] = useState([])
  const [rotas, setRotas] = useState([])
  const [horarios, setHorarios] = useState([])

  const [idEmbarcacao, setIdEmbarcacao] = useState('')
  const [idRota, setIdRota] = useState('')
  const [idHorario, setIdHorario] = useState('')
  const [dataInicio, setDataInicio] = useState(todayISO())
  const [dataFim, setDataFim] = useState(addMesesISO(6))
  const [frequencia, setFrequencia] = useState(14)
  const [diasDuracao, setDiasDuracao] = useState(3)
  const [gerando, setGerando] = useState(false)
  const [toast, setToast] = useState(null)

  useEffect(() => {
    api.get('/embarcacoes').then(setEmbarcacoes).catch(() => {})
    api.get('/rotas').then(setRotas).catch(() => {})
    api.get('/cadastros/horarios-saida').then(setHorarios).catch(() => {})
  }, [])

  function showToast(msg, type = 'success') {
    setToast({ msg, type }); setTimeout(() => setToast(null), 3500)
  }

  // Preview de quantidade de viagens
  const previewTotal = (() => {
    if (!dataInicio || !dataFim || !frequencia) return 0
    const ini = new Date(dataInicio + 'T12:00:00Z')
    const fim = new Date(dataFim + 'T12:00:00Z')
    if (fim < ini) return 0
    const diff = Math.floor((fim - ini) / (24 * 3600 * 1000))
    return Math.floor(diff / parseInt(frequencia)) + 1
  })()

  async function gerar() {
    if (!idEmbarcacao || !idRota || !dataInicio || !dataFim || !frequencia) {
      showToast('Preencha todos os campos obrigatorios', 'error'); return
    }
    if (previewTotal > 500) {
      showToast('Mais de 500 viagens. Reduza o periodo ou aumente a frequencia.', 'error'); return
    }
    if (!window.confirm(`Sera(ao) criada(s) ${previewTotal} viagem(ns). Confirmar?`)) return

    setGerando(true)
    try {
      const res = await api.post('/viagens/escala', {
        id_embarcacao: parseInt(idEmbarcacao),
        id_rota: parseInt(idRota),
        data_inicio: dataInicio,
        data_fim: dataFim,
        frequencia_dias: parseInt(frequencia),
        id_horario_saida: idHorario ? parseInt(idHorario) : null,
        dias_duracao: parseInt(diasDuracao)
      })
      showToast(`${res.total} viagem(ns) criada(s) com sucesso!`)
      window.dispatchEvent(new CustomEvent('viagem-ativa-changed')) // recarrega dropdown global
      if (onClose) setTimeout(onClose, 1500)
    } catch (err) {
      showToast(err.error || err.message || 'Erro ao gerar escala', 'error')
    } finally {
      setGerando(false)
    }
  }

  const L = { fontSize: '0.82rem', fontWeight: 700, color: 'var(--text)', display: 'block', marginBottom: 4 }
  const I = { width: '100%', padding: '8px 10px', border: '1px solid var(--border)', borderRadius: 4, fontSize: '0.88rem', boxSizing: 'border-box', background: 'var(--bg-soft)', color: 'var(--text)' }

  return (
    <div className="card" style={{ padding: 18, maxWidth: 600, margin: '0 auto' }}>
      <h2 style={{ marginTop: 0, marginBottom: 6, fontSize: '1.1rem' }}>Agendar Saidas Recorrentes</h2>
      <p style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginBottom: 18 }}>
        Cria varias viagens de uma vez conforme a frequencia escolhida (aparecem no calendario da tela inicial).
      </p>

      <div style={{ display: 'grid', gap: 14 }}>
        <div>
          <label style={L}>Embarcacao:</label>
          <select style={I} value={idEmbarcacao} onChange={e => setIdEmbarcacao(e.target.value)}>
            <option value="">Selecione...</option>
            {embarcacoes.map(e => <option key={e.id_embarcacao} value={e.id_embarcacao}>{e.nome}</option>)}
          </select>
        </div>
        <div>
          <label style={L}>Rota:</label>
          <select style={I} value={idRota} onChange={e => setIdRota(e.target.value)}>
            <option value="">Selecione...</option>
            {rotas.map(r => <option key={r.id_rota} value={r.id_rota}>{r.origem} - {r.destino}</option>)}
          </select>
        </div>
        <div>
          <label style={L}>Horario de Saida (opcional):</label>
          <select style={I} value={idHorario} onChange={e => setIdHorario(e.target.value)}>
            <option value="">Nenhum</option>
            {horarios.map(h => <option key={h.id_horario_saida} value={h.id_horario_saida}>{h.descricao_horario_saida}</option>)}
          </select>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
          <div>
            <label style={L}>Data Inicio:</label>
            <input type="date" style={I} value={dataInicio} onChange={e => setDataInicio(e.target.value)} />
          </div>
          <div>
            <label style={L}>Marcar ate:</label>
            <input type="date" style={I} value={dataFim} onChange={e => setDataFim(e.target.value)} />
          </div>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: '1.5fr 1fr', gap: 12 }}>
          <div>
            <label style={L}>Frequencia:</label>
            <select style={I} value={frequencia} onChange={e => setFrequencia(e.target.value)}>
              {FREQS.map(f => <option key={f.value} value={f.value}>{f.label}</option>)}
            </select>
          </div>
          <div>
            <label style={L}>Duracao (dias):</label>
            <input type="number" min="1" style={I} value={diasDuracao} onChange={e => setDiasDuracao(e.target.value)}
              title="Dias entre data de saida e data de chegada (default: 3)" />
          </div>
        </div>

        <div style={{ padding: 10, background: 'var(--bg-soft)', borderRadius: 4, border: '1px solid var(--border)', fontSize: '0.88rem' }}>
          <strong>Preview:</strong> serao criadas <strong style={{ color: 'var(--primary)', fontSize: '1rem' }}>{previewTotal}</strong> viagem(ns)
          {previewTotal > 500 && <div style={{ color: 'var(--danger)', fontSize: '0.78rem', marginTop: 4 }}>&#9888; Limite de 500. Reduza o periodo ou aumente a frequencia.</div>}
        </div>
      </div>

      <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', marginTop: 18 }}>
        {onClose && <button className="btn-secondary" onClick={onClose} disabled={gerando}>Cancelar (Esc)</button>}
        <button className="btn-primary" style={{ padding: '10px 24px', width: 'auto' }} onClick={gerar}
          disabled={gerando || previewTotal === 0 || previewTotal > 500}>
          {gerando ? 'Gerando...' : 'OK - Gerar Escala'}
        </button>
      </div>

      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}
    </div>
  )
}

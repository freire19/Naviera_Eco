import { useState, useEffect, useMemo } from 'react'
import { api } from '../api.js'
import { mapaFeriados } from '../utils/feriados.js'

const DIAS_SEMANA = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sab']
const MESES = ['Janeiro','Fevereiro','Marco','Abril','Maio','Junho','Julho','Agosto','Setembro','Outubro','Novembro','Dezembro']

function pad(n) { return String(n).padStart(2, '0') }
function toDateStr(y, m, d) { return `${y}-${pad(m + 1)}-${pad(d)}` }

// Aceita "YYYY-MM-DD", "DD/MM/YYYY" ou ISO; retorna YYYY-MM-DD ou null
function normalizarData(v) {
  if (!v) return null
  const s = String(v)
  if (/^\d{4}-\d{2}-\d{2}/.test(s)) return s.substring(0, 10)
  const brMatch = s.match(/^(\d{2})\/(\d{2})\/(\d{4})/)
  if (brMatch) return `${brMatch[3]}-${brMatch[2]}-${brMatch[1]}`
  return null
}

/**
 * Calendario grande pro Dashboard. Marca:
 *   - Hoje (fundo verde)
 *   - Viagens (dot vermelho) — data_viagem
 *   - Tarefas/anotacoes (dot roxo) — /agenda
 *   - Feriados nacionais (dot amarelo + nome)
 *   - Boletos / contas a pagar (icone no canto)
 */
export default function CalendarioEventos() {
  const hoje = new Date()
  const [mes, setMes] = useState(hoje.getMonth())
  const [ano, setAno] = useState(hoje.getFullYear())
  const [diaSelecionado, setDiaSelecionado] = useState(hoje.getDate())

  const [viagens, setViagens] = useState([])
  const [tarefas, setTarefas] = useState([])
  const [boletos, setBoletos] = useState([])

  useEffect(() => {
    api.get('/viagens').then(setViagens).catch(() => {})
  }, [])

  useEffect(() => {
    api.get(`/agenda?mes=${mes + 1}&ano=${ano}`).then(d => setTarefas(Array.isArray(d) ? d : [])).catch(() => {})
    api.get('/financeiro/boletos').then(d => setBoletos(Array.isArray(d) ? d : [])).catch(() => {})
  }, [mes, ano])

  const feriados = useMemo(() => mapaFeriados([ano - 1, ano, ano + 1]), [ano])

  // Indices por dia-ISO
  const { viagensPorDia, tarefasPorDia, boletosPorDia } = useMemo(() => {
    const v = {}, t = {}, b = {}
    viagens.forEach(it => {
      const d = normalizarData(it.data_viagem)
      if (d) (v[d] = v[d] || []).push(it)
    })
    tarefas.forEach(it => {
      const d = normalizarData(it.data_evento)
      if (d) (t[d] = t[d] || []).push(it)
    })
    boletos.forEach(it => {
      const d = normalizarData(it.data_vencimento)
      if (d) (b[d] = b[d] || []).push(it)
    })
    return { viagensPorDia: v, tarefasPorDia: t, boletosPorDia: b }
  }, [viagens, tarefas, boletos])

  const primeiroDia = new Date(ano, mes, 1).getDay()
  const diasNoMes = new Date(ano, mes + 1, 0).getDate()
  const hojeStr = toDateStr(hoje.getFullYear(), hoje.getMonth(), hoje.getDate())

  function navMes(delta) {
    let novoMes = mes + delta
    let novoAno = ano
    if (novoMes < 0) { novoMes = 11; novoAno-- }
    if (novoMes > 11) { novoMes = 0; novoAno++ }
    setMes(novoMes)
    setAno(novoAno)
    setDiaSelecionado(1)
  }

  const dataSelecionada = toDateStr(ano, mes, diaSelecionado)
  const eventosDoDia = [
    ...(viagensPorDia[dataSelecionada] || []).map(v => ({ tipo: 'viagem', titulo: `Viagem #${v.id_viagem}`, sub: v.descricao || v.nome_rota || '' })),
    ...(tarefasPorDia[dataSelecionada] || []).map(t => ({ tipo: 'tarefa', titulo: t.descricao, sub: t.concluida ? 'Concluida' : '' })),
    ...(boletosPorDia[dataSelecionada] || []).map(b => ({ tipo: 'boleto', titulo: b.descricao || 'Boleto', sub: `R$ ${Number(b.valor_total || 0).toFixed(2).replace('.', ',')}` })),
    ...(feriados[dataSelecionada] ? [{ tipo: 'feriado', titulo: feriados[dataSelecionada], sub: 'Feriado nacional' }] : [])
  ]

  const btnMes = {
    padding: '6px 14px', background: 'var(--primary)', color: '#fff', border: 'none',
    borderRadius: 4, fontWeight: 700, cursor: 'pointer', fontSize: '0.82rem'
  }

  return (
    <div className="card" style={{ padding: 12 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
        <button style={btnMes} onClick={() => navMes(-1)}>&lt; Anterior</button>
        <h3 style={{ margin: 0, fontSize: '1.05rem' }}>{MESES[mes]} {ano}</h3>
        <button style={btnMes} onClick={() => navMes(1)}>Proximo &gt;</button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 2, marginBottom: 4 }}>
        {DIAS_SEMANA.map(d => (
          <div key={d} style={{ fontSize: 11, fontWeight: 700, color: 'var(--text-muted)', padding: '6px 0', textAlign: 'center' }}>{d}</div>
        ))}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 2 }}>
        {Array.from({ length: primeiroDia }).map((_, i) => <div key={`e${i}`} />)}
        {Array.from({ length: diasNoMes }).map((_, i) => {
          const dia = i + 1
          const dateStr = toDateStr(ano, mes, dia)
          const isHoje = dateStr === hojeStr
          const isSel = dia === diaSelecionado
          const feriadoNome = feriados[dateStr]
          const temViagem = (viagensPorDia[dateStr] || []).length > 0
          const temTarefa = (tarefasPorDia[dateStr] || []).length > 0
          const temBoleto = (boletosPorDia[dateStr] || []).length > 0

          return (
            <button key={dia}
              onClick={() => setDiaSelecionado(dia)}
              style={{
                minHeight: 62, padding: '4px 3px', position: 'relative',
                background: isHoje ? 'var(--primary)' : (isSel ? 'var(--bg-accent)' : 'var(--bg-card)'),
                color: isHoje ? '#fff' : 'var(--text)',
                border: isSel && !isHoje ? '2px solid var(--primary)' : '1px solid var(--border)',
                borderRadius: 4, cursor: 'pointer', fontSize: 13, fontWeight: isHoje || isSel ? 700 : 500,
                display: 'flex', flexDirection: 'column', alignItems: 'flex-start', justifyContent: 'flex-start',
                gap: 2, textAlign: 'left'
              }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', width: '100%', alignItems: 'center' }}>
                <span>{dia}</span>
                {temBoleto && <span style={{ fontSize: 10 }} title="Conta a pagar">&#128221;</span>}
              </div>
              {feriadoNome && (
                <div style={{ fontSize: 9, color: isHoje ? '#fff' : '#B45309', lineHeight: 1.1 }}>&#9733; {feriadoNome}</div>
              )}
              <div style={{ display: 'flex', gap: 3, marginTop: 'auto' }}>
                {temViagem && <span style={{ width: 7, height: 7, borderRadius: '50%', background: '#DC2626' }} title="Viagem" />}
                {temTarefa && <span style={{ width: 7, height: 7, borderRadius: '50%', background: '#7C3AED' }} title="Anotacao" />}
                {feriadoNome && <span style={{ width: 7, height: 7, borderRadius: '50%', background: '#F59E0B' }} title="Feriado" />}
              </div>
            </button>
          )
        })}
      </div>

      <div style={{ display: 'flex', gap: 16, fontSize: 11, marginTop: 10, flexWrap: 'wrap', color: 'var(--text-muted)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}><span style={{ width: 8, height: 8, borderRadius: '50%', background: '#DC2626' }} /> Viagem</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}><span style={{ width: 8, height: 8, borderRadius: '50%', background: '#7C3AED' }} /> Anotacao</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}><span style={{ width: 8, height: 8, borderRadius: '50%', background: '#F59E0B' }} /> Feriado</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}><span style={{ width: 8, height: 8, borderRadius: '50%', background: 'var(--primary)' }} /> Hoje</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>&#128221; Contas a Pagar</div>
      </div>

      {eventosDoDia.length > 0 && (
        <div style={{ marginTop: 12, padding: 10, background: 'var(--bg-soft)', borderRadius: 6, border: '1px solid var(--border)' }}>
          <div style={{ fontSize: '0.82rem', fontWeight: 700, marginBottom: 6, color: 'var(--text)' }}>
            Eventos em {dataSelecionada.split('-').reverse().join('/')}
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            {eventosDoDia.map((ev, i) => (
              <div key={i} style={{ display: 'flex', gap: 8, fontSize: '0.78rem' }}>
                <span style={{
                  width: 6, height: 6, borderRadius: '50%', marginTop: 5, flexShrink: 0,
                  background: ev.tipo === 'viagem' ? '#DC2626' : ev.tipo === 'tarefa' ? '#7C3AED' : ev.tipo === 'boleto' ? '#0EA5E9' : '#F59E0B'
                }} />
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontWeight: 600, color: 'var(--text)' }}>{ev.titulo}</div>
                  {ev.sub && <div style={{ color: 'var(--text-muted)', fontSize: '0.72rem' }}>{ev.sub}</div>}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

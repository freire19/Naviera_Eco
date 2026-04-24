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

  const COR_VIAGEM = '#DC2626'
  const COR_TAREFA = '#7C3AED'
  const COR_FERIADO = '#F59E0B'
  const COR_BOLETO = '#0EA5E9'

  return (
    <div className="calendario-card">
      {/* Header */}
      <div style={{
        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
        marginBottom: 8, paddingBottom: 8, borderBottom: '1px solid var(--border)'
      }}>
        <button className="calendario-btn-nav" onClick={() => navMes(-1)}>&larr; Anterior</button>
        <h3 style={{ margin: 0, fontSize: '1rem', fontWeight: 700, color: 'var(--text)', letterSpacing: '-0.02em' }}>
          {MESES[mes]} <span style={{ color: 'var(--text-muted)', fontWeight: 500, marginLeft: 4 }}>{ano}</span>
        </h3>
        <button className="calendario-btn-nav" onClick={() => navMes(1)}>Proximo &rarr;</button>
      </div>

      {/* Dias da semana */}
      <div className="calendario-weekdays">
        {DIAS_SEMANA.map((d, i) => (
          <div key={d} className={`calendario-weekday${(i === 0 || i === 6) ? ' weekend' : ''}`}>{d}</div>
        ))}
      </div>

      {/* Grade de dias */}
      <div className="calendario-grid">
        {Array.from({ length: primeiroDia }).map((_, i) => (
          <div key={`e${i}`} className="calendario-placeholder-cell" />
        ))}
        {Array.from({ length: diasNoMes }).map((_, i) => {
          const dia = i + 1
          const dateStr = toDateStr(ano, mes, dia)
          const isHoje = dateStr === hojeStr
          const isSel = dia === diaSelecionado
          const diaSemana = (primeiroDia + i) % 7
          const isFimSemana = diaSemana === 0 || diaSemana === 6
          const feriadoNome = feriados[dateStr]
          const viagensDia = viagensPorDia[dateStr] || []
          const tarefasDia = tarefasPorDia[dateStr] || []
          const boletosDia = boletosPorDia[dateStr] || []
          const temViagem = viagensDia.length > 0
          const temTarefa = tarefasDia.length > 0
          const temBoleto = boletosDia.length > 0
          const temEvento = temViagem || temTarefa || temBoleto || !!feriadoNome
          const rotaResumo = temViagem
            ? (viagensDia[0].nome_rota ||
               (viagensDia[0].origem && viagensDia[0].destino ? `${viagensDia[0].origem} - ${viagensDia[0].destino}` : viagensDia[0].descricao || 'Viagem'))
            : null

          const cellClasses = [
            'calendario-cell',
            isFimSemana ? 'weekend' : '',
            isHoje ? 'is-hoje' : '',
            (isSel && !isHoje) ? 'is-selecionado' : ''
          ].filter(Boolean).join(' ')

          return (
            <button key={dia} className={cellClasses} onClick={() => setDiaSelecionado(dia)}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span className="dia-num">{dia}</span>
              </div>

              {feriadoNome && (
                <div className="evento-linha" style={{ color: isHoje ? undefined : COR_FERIADO }} title={feriadoNome}>
                  ★ {feriadoNome}
                </div>
              )}
              {rotaResumo && (
                <div className="evento-linha" style={{ color: isHoje ? undefined : COR_VIAGEM, fontWeight: 700 }}
                  title={viagensDia.map(v => v.nome_rota || v.descricao).join(', ')}>
                  ⚓ {rotaResumo}{viagensDia.length > 1 ? ` +${viagensDia.length - 1}` : ''}
                </div>
              )}
              {temTarefa && tarefasDia[0]?.descricao && (
                <div className="evento-linha" style={{ color: isHoje ? undefined : COR_TAREFA }}
                  title={tarefasDia.map(t => t.descricao).join(', ')}>
                  ✎ {tarefasDia[0].descricao}{tarefasDia.length > 1 ? ` +${tarefasDia.length - 1}` : ''}
                </div>
              )}
              {temBoleto && (() => {
                const b = boletosDia[0]
                const desc = b.descricao || 'Boleto'
                const valor = Number(b.valor_total || 0)
                const label = valor > 0 ? `${desc} R$ ${valor.toFixed(2).replace('.', ',')}` : desc
                const titulo = boletosDia.map(x => `${x.descricao || 'Boleto'} R$ ${Number(x.valor_total || 0).toFixed(2).replace('.', ',')}`).join(' | ')
                return (
                  <div className="evento-linha" style={{ color: isHoje ? undefined : COR_BOLETO, fontWeight: 700 }} title={titulo}>
                    $ {label}{boletosDia.length > 1 ? ` +${boletosDia.length - 1}` : ''}
                  </div>
                )
              })()}

              {temEvento && (
                <div style={{ display: 'flex', gap: 2, marginTop: 'auto', paddingTop: 1 }}>
                  {temViagem && <span style={{ width: 5, height: 5, borderRadius: '50%', background: isHoje ? '#fff' : COR_VIAGEM }} title="Viagem" />}
                  {temTarefa && <span style={{ width: 5, height: 5, borderRadius: '50%', background: isHoje ? '#fff' : COR_TAREFA }} title="Anotacao" />}
                  {feriadoNome && <span style={{ width: 5, height: 5, borderRadius: '50%', background: isHoje ? '#fff' : COR_FERIADO }} title="Feriado" />}
                  {temBoleto && <span style={{ width: 5, height: 5, borderRadius: '50%', background: isHoje ? '#fff' : COR_BOLETO }} title="Conta a pagar" />}
                </div>
              )}
            </button>
          )
        })}
      </div>

      {/* Legenda */}
      <div style={{
        display: 'flex', gap: 14, fontSize: 10, marginTop: 8, paddingTop: 8,
        borderTop: '1px solid var(--border)',
        flexWrap: 'wrap', color: 'var(--text-muted)', fontWeight: 500
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}><span style={{ width: 8, height: 8, borderRadius: '50%', background: COR_VIAGEM }} /> Viagem</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}><span style={{ width: 8, height: 8, borderRadius: '50%', background: COR_TAREFA }} /> Anotacao</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}><span style={{ width: 8, height: 8, borderRadius: '50%', background: COR_FERIADO }} /> Feriado</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}><span style={{ width: 8, height: 8, borderRadius: '50%', background: COR_BOLETO }} /> Conta a Pagar</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}><span style={{ width: 8, height: 8, borderRadius: '50%', background: 'var(--primary)' }} /> Hoje</div>
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

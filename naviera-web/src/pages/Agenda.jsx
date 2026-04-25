import { useState, useEffect, useCallback, useMemo } from 'react'
import { api } from '../api.js'

const DIAS_SEMANA = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sab']
const MESES = ['Janeiro','Fevereiro','Marco','Abril','Maio','Junho','Julho','Agosto','Setembro','Outubro','Novembro','Dezembro']

function pad(n) { return String(n).padStart(2, '0') }

function toDateStr(y, m, d) {
  return `${y}-${pad(m + 1)}-${pad(d)}`
}

export default function Agenda() {
  const hoje = new Date()
  const [mes, setMes] = useState(hoje.getMonth())
  const [ano, setAno] = useState(hoje.getFullYear())
  const [diaSelecionado, setDiaSelecionado] = useState(hoje.getDate())
  const [tarefas, setTarefas] = useState([])
  const [viagens, setViagens] = useState([])
  const [loading, setLoading] = useState(false)
  const [novaDescricao, setNovaDescricao] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [toast, setToast] = useState(null)

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  const carregarTarefas = useCallback(() => {
    setLoading(true)
    api.get(`/agenda?mes=${mes + 1}&ano=${ano}`)
      .then(data => setTarefas(Array.isArray(data) ? data : []))
      .catch(() => setTarefas([]))
      .finally(() => setLoading(false))
  }, [mes, ano])

  useEffect(() => {
    carregarTarefas()
  }, [carregarTarefas])

  useEffect(() => {
    api.get('/viagens')
      .then(setViagens)
      .catch(() => {})
  }, [])

  // Calendar grid
  const primeiroDia = new Date(ano, mes, 1).getDay()
  const diasNoMes = new Date(ano, mes + 1, 0).getDate()

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

  // Tasks for selected day
  const tarefasDoDia = tarefas.filter(t => {
    const d = t.data_evento?.split('T')[0]
    return d === dataSelecionada
  })

  // #DP079: contagem memoizada — re-build do objeto inteiro a cada render era desperdicio.
  const contagem = useMemo(() => {
    const c = {}
    tarefas.forEach(t => {
      const d = t.data_evento?.split('T')[0]
      c[d] = (c[d] || 0) + 1
    })
    return c
  }, [tarefas])

  // Viagens neste mes
  const viagensMes = viagens.filter(v => {
    if (!v.data_viagem) return false
    const d = new Date(v.data_viagem + 'T00:00:00')
    return d.getMonth() === mes && d.getFullYear() === ano
  })
  const viagensDia = viagens.filter(v => {
    return v.data_viagem?.split('T')[0] === dataSelecionada
  })

  async function adicionarTarefa(e) {
    e.preventDefault()
    if (!novaDescricao.trim()) return
    setSalvando(true)
    try {
      await api.post('/agenda', {
        data_evento: dataSelecionada,
        descricao: novaDescricao.trim()
      })
      setNovaDescricao('')
      carregarTarefas()
      showToast('Tarefa adicionada')
    } catch (err) {
      showToast(err.message || 'Erro ao adicionar tarefa', 'error')
    } finally {
      setSalvando(false)
    }
  }

  async function toggleConcluida(tarefa) {
    try {
      await api.put(`/agenda/${tarefa.id}`, { concluida: !tarefa.concluida })
      carregarTarefas()
    } catch (err) {
      showToast('Erro ao atualizar tarefa', 'error')
    }
  }

  async function excluirTarefa(tarefa) {
    if (!window.confirm('Excluir esta tarefa?')) return
    try {
      await api.delete(`/agenda/${tarefa.id}`)
      carregarTarefas()
      showToast('Tarefa excluida')
    } catch (err) {
      showToast('Erro ao excluir tarefa', 'error')
    }
  }

  const hojeStr = toDateStr(hoje.getFullYear(), hoje.getMonth(), hoje.getDate())

  return (
    <div>
      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem' }}>
        {/* Calendar */}
        <div className="card" style={{ padding: '1rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <button className="btn-secondary" onClick={() => navMes(-1)} style={{ padding: '4px 12px' }}>&#9664;</button>
            <h3 style={{ margin: 0 }}>{MESES[mes]} {ano}</h3>
            <button className="btn-secondary" onClick={() => navMes(1)} style={{ padding: '4px 12px' }}>&#9654;</button>
          </div>

          {/* Week headers */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 2, textAlign: 'center', marginBottom: 4 }}>
            {DIAS_SEMANA.map(d => (
              <div key={d} style={{ fontSize: 11, fontWeight: 600, color: 'var(--text-muted)', padding: '4px 0' }}>{d}</div>
            ))}
          </div>

          {/* Days */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 2 }}>
            {Array.from({ length: primeiroDia }).map((_, i) => (
              <div key={`e${i}`} />
            ))}
            {Array.from({ length: diasNoMes }).map((_, i) => {
              const dia = i + 1
              const dateStr = toDateStr(ano, mes, dia)
              const isHoje = dateStr === hojeStr
              const isSelecionado = dia === diaSelecionado
              const temTarefas = contagem[dateStr] > 0
              const temViagem = viagens.some(v => v.data_viagem?.split('T')[0] === dateStr)

              return (
                <button
                  key={dia}
                  onClick={() => setDiaSelecionado(dia)}
                  style={{
                    padding: '6px 2px',
                    border: isSelecionado ? '2px solid var(--primary)' : '1px solid transparent',
                    borderRadius: 6,
                    background: isHoje ? 'var(--primary)' : isSelecionado ? 'var(--bg-hover)' : 'transparent',
                    color: isHoje ? '#fff' : 'var(--text-primary)',
                    cursor: 'pointer',
                    fontSize: 13,
                    fontWeight: isHoje ? 700 : 400,
                    position: 'relative',
                    minHeight: 36,
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: 2
                  }}
                >
                  {dia}
                  <div style={{ display: 'flex', gap: 2 }}>
                    {temTarefas && (
                      <span style={{ width: 5, height: 5, borderRadius: '50%', background: '#0EA5E9' }} />
                    )}
                    {temViagem && (
                      <span style={{ width: 5, height: 5, borderRadius: '50%', background: '#F59E0B' }} />
                    )}
                  </div>
                </button>
              )
            })}
          </div>

          {/* Legend */}
          <div style={{ marginTop: '0.75rem', display: 'flex', gap: 16, fontSize: 11, color: 'var(--text-muted)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
              <span style={{ width: 8, height: 8, borderRadius: '50%', background: '#0EA5E9' }} /> Tarefas
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
              <span style={{ width: 8, height: 8, borderRadius: '50%', background: '#F59E0B' }} /> Viagens
            </div>
          </div>
        </div>

        {/* Day detail */}
        <div className="card" style={{ padding: '1rem' }}>
          <h3 style={{ marginBottom: '1rem' }}>
            {pad(diaSelecionado)}/{pad(mes + 1)}/{ano}
          </h3>

          {/* Viagens do dia */}
          {viagensDia.length > 0 && (
            <div style={{ marginBottom: '1rem' }}>
              <h4 style={{ color: 'var(--warning)', fontSize: 13, marginBottom: 6 }}>Viagens</h4>
              {viagensDia.map(v => (
                <div key={v.id_viagem} style={{ padding: '6px 10px', background: 'rgba(245, 158, 11, 0.1)', borderRadius: 6, marginBottom: 4, fontSize: 13 }}>
                  {v.descricao} {v.ativa && <span className="badge success" style={{ fontSize: 10 }}>Ativa</span>}
                </div>
              ))}
            </div>
          )}

          {/* Tarefas do dia */}
          <h4 style={{ color: 'var(--text-muted)', fontSize: 13, marginBottom: 6 }}>Tarefas</h4>

          {loading ? (
            <p style={{ color: 'var(--text-muted)', fontSize: 13 }}>Carregando...</p>
          ) : (
            <>
              {tarefasDoDia.length === 0 && (
                <p style={{ color: 'var(--text-muted)', fontSize: 13, marginBottom: '1rem' }}>Nenhuma tarefa neste dia.</p>
              )}
              <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginBottom: '1rem' }}>
                {tarefasDoDia.map(t => (
                  <div
                    key={t.id}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 8,
                      padding: '8px 10px',
                      background: 'var(--bg-hover)',
                      borderRadius: 6,
                      fontSize: 13
                    }}
                  >
                    <input
                      type="checkbox"
                      checked={!!t.concluida}
                      onChange={() => toggleConcluida(t)}
                      style={{ cursor: 'pointer', width: 16, height: 16, accentColor: 'var(--primary)' }}
                    />
                    <span style={{
                      flex: 1,
                      textDecoration: t.concluida ? 'line-through' : 'none',
                      opacity: t.concluida ? 0.6 : 1
                    }}>
                      {t.descricao}
                    </span>
                    <button
                      onClick={() => excluirTarefa(t)}
                      style={{
                        background: 'none',
                        border: 'none',
                        color: 'var(--danger)',
                        cursor: 'pointer',
                        fontSize: 16,
                        padding: '2px 6px'
                      }}
                      title="Excluir"
                    >
                      &#10005;
                    </button>
                  </div>
                ))}
              </div>

              {/* Add task form */}
              <form onSubmit={adicionarTarefa} style={{ display: 'flex', gap: 8 }}>
                <input
                  type="text"
                  value={novaDescricao}
                  onChange={e => setNovaDescricao(e.target.value)}
                  placeholder="Nova tarefa..."
                  style={{ flex: 1 }}
                  disabled={salvando}
                />
                <button type="submit" className="btn-primary" disabled={salvando || !novaDescricao.trim()}>
                  {salvando ? '...' : '+'}
                </button>
              </form>
            </>
          )}
        </div>
      </div>

      {/* Viagens do mes (table below) */}
      {viagensMes.length > 0 && (
        <div className="card" style={{ marginTop: '1.5rem' }}>
          <div className="card-header">
            <h3>Viagens em {MESES[mes]}</h3>
            <span className="badge info">{viagensMes.length}</span>
          </div>
          <div className="table-container">
            <table>
              <thead>
                <tr><th>Descricao</th><th>Saida</th><th>Chegada</th><th>Status</th></tr>
              </thead>
              <tbody>
                {viagensMes.map(v => (
                  <tr key={v.id_viagem}>
                    <td>{v.descricao}</td>
                    <td>{v.data_viagem ? new Date(v.data_viagem + 'T00:00:00').toLocaleDateString('pt-BR') : '-'}</td>
                    <td>{v.data_chegada ? new Date(v.data_chegada + 'T00:00:00').toLocaleDateString('pt-BR') : '-'}</td>
                    <td>
                      {v.ativa ? <span className="badge success">Ativa</span> : <span className="badge">Agendada</span>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}

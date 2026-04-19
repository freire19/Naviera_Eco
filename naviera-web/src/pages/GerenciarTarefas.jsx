import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

function fmtData(d) {
  if (!d) return '-'
  const s = String(d).substring(0, 10)
  const p = s.split('-')
  return p.length === 3 ? `${p[2]}/${p[1]}/${p[0]}` : s
}

export default function GerenciarTarefas({ onClose }) {
  const [tarefas, setTarefas] = useState([])
  const [loading, setLoading] = useState(false)
  const [ocultarConcluidas, setOcultarConcluidas] = useState(false)
  const [toast, setToast] = useState(null)

  function showToast(msg, type = 'success') {
    setToast({ msg, type }); setTimeout(() => setToast(null), 2500)
  }

  const carregar = useCallback(() => {
    setLoading(true)
    api.get('/agenda')
      .then(d => setTarefas(Array.isArray(d) ? d : []))
      .catch(() => showToast('Erro ao carregar tarefas', 'error'))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { carregar() }, [carregar])

  async function toggleConcluida(t) {
    try {
      await api.put(`/agenda/${t.id}`, { concluida: !t.concluida })
      setTarefas(prev => prev.map(x => x.id === t.id ? { ...x, concluida: !t.concluida } : x))
    } catch { showToast('Erro ao atualizar', 'error') }
  }

  async function excluir(t) {
    if (!window.confirm('Excluir esta tarefa?')) return
    try {
      await api.delete(`/agenda/${t.id}`)
      setTarefas(prev => prev.filter(x => x.id !== t.id))
      showToast('Excluida')
    } catch { showToast('Erro ao excluir', 'error') }
  }

  const visiveis = ocultarConcluidas ? tarefas.filter(t => !t.concluida) : tarefas

  return (
    <div className="card" style={{ padding: 14 }}>
      <h2 style={{ marginTop: 0, marginBottom: 12, fontSize: '1.1rem' }}>Gerenciador de Tarefas e Agenda</h2>

      <div style={{ display: 'flex', alignItems: 'center', gap: 14, marginBottom: 12 }}>
        <label style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.85rem', cursor: 'pointer' }}>
          <input type="checkbox" checked={ocultarConcluidas} onChange={e => setOcultarConcluidas(e.target.checked)} />
          Ocultar Concluidas
        </label>
        <button className="btn-primary" style={{ padding: '8px 16px', width: 'auto' }} onClick={carregar}>
          &#x21BB; Atualizar Lista
        </button>
        <div style={{ flex: 1 }} />
        <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>{visiveis.length} tarefa(s)</span>
      </div>

      <div className="table-container" style={{ maxHeight: 'calc(100vh - 260px)', overflow: 'auto' }}>
        <table>
          <thead>
            <tr>
              <th style={{ width: 110 }}>Data</th>
              <th>Descricao da Tarefa</th>
              <th style={{ width: 110, textAlign: 'center' }}>Concluida?</th>
              <th style={{ width: 80, textAlign: 'center' }}>Acoes</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="4" style={{ textAlign: 'center', padding: 30 }}>Carregando...</td></tr>
            ) : visiveis.length === 0 ? (
              <tr><td colSpan="4" style={{ textAlign: 'center', padding: 40, color: 'var(--text-muted)', fontWeight: 600 }}>
                Nao ha conteudo na tabela
              </td></tr>
            ) : visiveis.map(t => (
              <tr key={t.id} style={{ opacity: t.concluida ? 0.55 : 1 }}>
                <td>{fmtData(t.data_evento)}</td>
                <td style={{ textDecoration: t.concluida ? 'line-through' : 'none' }}>{t.descricao}</td>
                <td style={{ textAlign: 'center' }}>
                  <input type="checkbox" checked={!!t.concluida} onChange={() => toggleConcluida(t)} />
                </td>
                <td style={{ textAlign: 'center' }}>
                  <button className="btn-sm danger" onClick={() => excluir(t)} style={{ padding: '4px 10px' }}>Excluir</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {onClose && (
        <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 12 }}>
          <button className="btn-secondary" onClick={onClose}>Sair (Esc)</button>
        </div>
      )}

      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}
    </div>
  )
}

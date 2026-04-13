import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}

function formatDateTime(dateStr) {
  if (!dateStr) return '—'
  const d = new Date(dateStr)
  return d.toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}

const TIPO_BADGE = {
  passagem: 'info',
  encomenda: 'warning',
  frete: 'success'
}

const TIPO_LABEL = {
  passagem: 'Passagem',
  encomenda: 'Encomenda',
  frete: 'Frete'
}

export default function HistoricoEstornos({ viagemAtiva }) {
  const [estornos, setEstornos] = useState([])
  const [loading, setLoading] = useState(false)
  const [toast, setToast] = useState(null)

  // Filtros
  const [filtroTipo, setFiltroTipo] = useState('')
  const [filtroDataInicio, setFiltroDataInicio] = useState('')
  const [filtroDataFim, setFiltroDataFim] = useState('')
  const [filtroAutorizador, setFiltroAutorizador] = useState('')

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  const carregarEstornos = useCallback(() => {
    setLoading(true)
    const params = new URLSearchParams()
    if (filtroTipo) params.append('tipo', filtroTipo)
    if (filtroDataInicio) params.append('data_inicio', filtroDataInicio)
    if (filtroDataFim) params.append('data_fim', filtroDataFim)
    if (filtroAutorizador.trim()) params.append('autorizador', filtroAutorizador.trim())
    const qs = params.toString()
    api.get(`/estornos/historico${qs ? '?' + qs : ''}`)
      .then(data => setEstornos(Array.isArray(data) ? data : []))
      .catch(() => showToast('Erro ao carregar historico', 'error'))
      .finally(() => setLoading(false))
  }, [filtroTipo, filtroDataInicio, filtroDataFim, filtroAutorizador])

  useEffect(() => { carregarEstornos() }, [carregarEstornos])

  function limparFiltros() {
    setFiltroTipo('')
    setFiltroDataInicio('')
    setFiltroDataFim('')
    setFiltroAutorizador('')
  }

  const totalEstornado = estornos.reduce((sum, e) => sum + (parseFloat(e.valor_estornado) || 0), 0)

  return (
    <div className="page-content">
      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}

      <div className="page-header">
        <h2>Historico de Estornos</h2>
        <span className="badge info">{estornos.length} registros — Total: {formatMoney(totalEstornado)}</span>
      </div>

      {/* Filtros */}
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12, marginBottom: 16, alignItems: 'flex-end' }}>
        <div className="form-group" style={{ margin: 0, minWidth: 140 }}>
          <label style={{ fontSize: '0.8rem', marginBottom: 4 }}>Tipo</label>
          <select value={filtroTipo} onChange={e => setFiltroTipo(e.target.value)}>
            <option value="">Todos</option>
            <option value="passagem">Passagem</option>
            <option value="encomenda">Encomenda</option>
            <option value="frete">Frete</option>
          </select>
        </div>
        <div className="form-group" style={{ margin: 0, minWidth: 140 }}>
          <label style={{ fontSize: '0.8rem', marginBottom: 4 }}>Data Inicio</label>
          <input type="date" value={filtroDataInicio} onChange={e => setFiltroDataInicio(e.target.value)} />
        </div>
        <div className="form-group" style={{ margin: 0, minWidth: 140 }}>
          <label style={{ fontSize: '0.8rem', marginBottom: 4 }}>Data Fim</label>
          <input type="date" value={filtroDataFim} onChange={e => setFiltroDataFim(e.target.value)} />
        </div>
        <div className="form-group" style={{ margin: 0, minWidth: 160 }}>
          <label style={{ fontSize: '0.8rem', marginBottom: 4 }}>Autorizador</label>
          <input
            type="text"
            value={filtroAutorizador}
            onChange={e => setFiltroAutorizador(e.target.value)}
            placeholder="Nome do autorizador"
          />
        </div>
        <button className="btn-secondary" onClick={limparFiltros} style={{ height: 38 }}>
          Limpar
        </button>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: 40, color: 'var(--text-muted)' }}>Carregando...</div>
      ) : estornos.length === 0 ? (
        <div style={{ textAlign: 'center', padding: 40, color: 'var(--text-muted)' }}>
          Nenhum estorno encontrado
        </div>
      ) : (
        <div className="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>Data/Hora</th>
                <th>Tipo</th>
                <th>Numero</th>
                <th>Valor Estornado</th>
                <th>Motivo</th>
                <th>Forma Devolucao</th>
                <th>Autorizador</th>
              </tr>
            </thead>
            <tbody>
              {estornos.map((e, idx) => (
                <tr key={e.id_log || idx}>
                  <td>{formatDateTime(e.data_hora)}</td>
                  <td>
                    <span className={`badge ${TIPO_BADGE[e.tipo] || ''}`}>
                      {TIPO_LABEL[e.tipo] || e.tipo}
                    </span>
                  </td>
                  <td>#{e.numero || '—'}</td>
                  <td style={{ fontWeight: 600, color: 'var(--danger)' }}>{formatMoney(e.valor_estornado)}</td>
                  <td style={{ maxWidth: 250, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {e.motivo || '—'}
                  </td>
                  <td>{e.forma_devolucao || '—'}</td>
                  <td>{e.nome_autorizador || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

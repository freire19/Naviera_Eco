import { formatMoney, formatDate } from './formatters.js'

const SUMMARY_STYLE = { padding: '0.75rem 1rem', borderBottom: '1px solid var(--border)', display: 'flex', gap: 16, fontSize: 13 }
const EMPTY_STYLE = { textAlign: 'center', padding: 30, color: 'var(--text-muted)' }
const DATE_STYLE = { fontSize: 12 }

/**
 * Column definition: { key, label, format?, className?, badge? }
 *   - format: 'money' | 'date' | function(value, row) => ReactNode
 *   - badge: function(row) => { className, label } for badge rendering
 */

function renderCell(row, col) {
  const val = row[col.key]

  if (col.badge) {
    const b = col.badge(row)
    return <span className={`badge ${b.className}`}>{b.label}</span>
  }

  if (col.format === 'money') {
    return <span className="money">{formatMoney(val)}</span>
  }

  if (col.format === 'date') {
    return formatDate(val)
  }

  if (typeof col.format === 'function') {
    return col.format(val, row)
  }

  return val || '\u2014'
}

export default function TabDetalhe({
  titulo,
  dados,
  colunas,
  idKey,
  loading,
  dataInicio,
  dataFim,
  onDataInicioChange,
  onDataFimChange,
  onExportar,
  sumarios
}) {
  return (
    <div className="card">
      <div className="card-header">
        <h3>{titulo}</h3>
        <div className="toolbar">
          <input type="date" value={dataInicio} onChange={e => onDataInicioChange(e.target.value)} title="Data inicio" style={DATE_STYLE} />
          <input type="date" value={dataFim} onChange={e => onDataFimChange(e.target.value)} title="Data fim" style={DATE_STYLE} />
          {dados.length > 0 && (
            <button className="btn-sm primary" onClick={onExportar}>Exportar CSV</button>
          )}
        </div>
      </div>
      {loading ? (
        <p style={{ padding: '1rem', color: 'var(--text-muted)' }}>Carregando...</p>
      ) : (
        <>
          {dados.length > 0 && sumarios && (
            <div style={SUMMARY_STYLE}>
              {sumarios.map((s, i) => (
                <span key={i}>
                  {s.label}: <strong className={s.className || ''}>{s.render ? s.render() : s.value}</strong>
                </span>
              ))}
            </div>
          )}
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  {colunas.map(col => (
                    <th key={col.key}>{col.label}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {dados.map(row => (
                  <tr key={row[idKey]}>
                    {colunas.map(col => (
                      <td key={col.key} className={col.format === 'money' ? 'money' : ''}>
                        {renderCell(row, col)}
                      </td>
                    ))}
                  </tr>
                ))}
                {dados.length === 0 && (
                  <tr><td colSpan={colunas.length} style={EMPTY_STYLE}>Nenhum registro encontrado</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  )
}

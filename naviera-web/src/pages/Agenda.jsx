import { useState, useEffect } from 'react'
import { api } from '../api.js'

export default function Agenda({ viagemAtiva }) {
  const [viagens, setViagens] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    setLoading(true)
    api.get('/viagens')
      .then(setViagens)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  const fmtDate = d => d ? new Date(d + 'T00:00:00').toLocaleDateString('pt-BR') : '-'

  const proximas = viagens.filter(v => !v.ativa && new Date(v.data_viagem) >= new Date(new Date().toDateString()))
  const anteriores = viagens.filter(v => !v.ativa && new Date(v.data_viagem) < new Date(new Date().toDateString()))
  const ativas = viagens.filter(v => v.ativa)

  return (
    <div className="card">
      <div className="card-header">
        <h2>Agenda de Viagens</h2>
      </div>

      {loading ? <p style={{ padding: '20px' }}>Carregando...</p> : (
        <>
          {ativas.length > 0 && (
            <>
              <h3 style={{ padding: '16px 20px 8px', color: 'var(--success)' }}>Viagem Ativa</h3>
              <div className="table-container">
                <table>
                  <thead>
                    <tr><th>Descricao</th><th>Saida</th><th>Chegada</th></tr>
                  </thead>
                  <tbody>
                    {ativas.map(v => (
                      <tr key={v.id_viagem}>
                        <td><strong>{v.descricao}</strong></td>
                        <td>{fmtDate(v.data_viagem)}</td>
                        <td>{fmtDate(v.data_chegada)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}

          <h3 style={{ padding: '16px 20px 8px' }}>Proximas Viagens ({proximas.length})</h3>
          <div className="table-container">
            <table>
              <thead>
                <tr><th>Descricao</th><th>Saida</th><th>Chegada</th></tr>
              </thead>
              <tbody>
                {proximas.length === 0 ? (
                  <tr><td colSpan="3">Nenhuma viagem agendada</td></tr>
                ) : proximas.map(v => (
                  <tr key={v.id_viagem}>
                    <td>{v.descricao}</td>
                    <td>{fmtDate(v.data_viagem)}</td>
                    <td>{fmtDate(v.data_chegada)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {anteriores.length > 0 && (
            <>
              <h3 style={{ padding: '16px 20px 8px', color: 'var(--text-muted)' }}>Viagens Anteriores ({anteriores.length})</h3>
              <div className="table-container">
                <table>
                  <thead>
                    <tr><th>Descricao</th><th>Saida</th><th>Chegada</th></tr>
                  </thead>
                  <tbody>
                    {anteriores.slice(0, 10).map(v => (
                      <tr key={v.id_viagem} style={{ opacity: 0.7 }}>
                        <td>{v.descricao}</td>
                        <td>{fmtDate(v.data_viagem)}</td>
                        <td>{fmtDate(v.data_chegada)}</td>
                      </tr>
                    ))}
                    {anteriores.length > 10 && (
                      <tr><td colSpan="3" style={{ textAlign: 'center', color: 'var(--text-muted)' }}>
                        ...e mais {anteriores.length - 10} viagens
                      </td></tr>
                    )}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </>
      )}
    </div>
  )
}

import { useState, useEffect } from 'react'
import { apiGet } from '../api.js'
import { money, fmtDateTime, timeAgo } from '../helpers.js'
import Badge from '../components/Badge.jsx'
import Card from '../components/Card.jsx'
import { IconRefresh } from '../icons.jsx'

export default function HistoricoScreen({ t, showToast }) {
  const [lancamentos, setLancamentos] = useState([])
  const [loading, setLoading] = useState(true)
  const [filtro, setFiltro] = useState('')

  const carregar = async () => {
    setLoading(true)
    try {
      const url = filtro ? `/ocr/lancamentos?status=${filtro}` : '/ocr/lancamentos'
      const data = await apiGet(url)
      setLancamentos(data || [])
    } catch {
      showToast('Erro ao carregar historico', 'error')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { carregar() }, [filtro])

  const filtros = [
    { key: '', label: 'Todos' },
    { key: 'pendente', label: 'Pendentes' },
    { key: 'revisado_operador', label: 'Revisados' },
    { key: 'aprovado', label: 'Aprovados' },
    { key: 'rejeitado', label: 'Rejeitados' }
  ]

  return (
    <div className="screen-enter" style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 12, paddingBottom: 100 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2 style={{ color: t.tx, fontSize: '1.1rem', fontWeight: 600 }}>Historico</h2>
        <button onClick={carregar} style={{
          background: 'none', border: 'none', cursor: 'pointer', padding: 4
        }}>
          <IconRefresh size={18} color={t.txMuted} />
        </button>
      </div>

      {/* Filtros */}
      <div style={{ display: 'flex', gap: 6, overflowX: 'auto', paddingBottom: 4 }}>
        {filtros.map(f => (
          <button
            key={f.key}
            onClick={() => setFiltro(f.key)}
            className="btn"
            style={{
              padding: '6px 14px', fontSize: '0.8rem', whiteSpace: 'nowrap',
              background: filtro === f.key ? t.pri : t.soft,
              color: filtro === f.key ? '#fff' : t.txSoft,
              border: `1px solid ${filtro === f.key ? t.pri : t.border}`
            }}
          >
            {f.label}
          </button>
        ))}
      </div>

      {/* Lista */}
      {loading ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {[1, 2, 3].map(i => (
            <div key={i} className="skeleton" style={{ height: 80, borderRadius: 12 }} />
          ))}
        </div>
      ) : lancamentos.length === 0 ? (
        <div style={{
          textAlign: 'center', padding: 40, color: t.txMuted, fontSize: '0.9rem'
        }}>
          Nenhum lancamento encontrado
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {lancamentos.map(l => {
            const dados = l.dados_revisados || l.dados_extraidos || {}
            const qtdItens = dados.itens?.length || 0
            return (
              <Card key={l.id} t={t} style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span style={{ color: t.tx, fontWeight: 600, fontSize: '0.95rem' }}>
                    OCR #{l.id}
                  </span>
                  <Badge status={l.status} t={t} />
                </div>

                <div style={{ display: 'flex', justifyContent: 'space-between', color: t.txSoft, fontSize: '0.85rem' }}>
                  <span>{qtdItens} {qtdItens === 1 ? 'item' : 'itens'}</span>
                  <span>{money(dados.valor_total)}</span>
                </div>

                {dados.remetente && (
                  <div style={{ fontSize: '0.8rem', color: t.txMuted }}>
                    {dados.remetente} → {dados.destinatario || '?'}
                  </div>
                )}

                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', color: t.txMuted }}>
                  <span>{l.nome_usuario_criou || 'Operador'}</span>
                  <span>{timeAgo(l.criado_em)}</span>
                </div>

                {l.status === 'rejeitado' && l.motivo_rejeicao && (
                  <div style={{
                    background: t.errBg, color: t.errTx, borderRadius: 6,
                    padding: '4px 8px', fontSize: '0.8rem'
                  }}>
                    Motivo: {l.motivo_rejeicao}
                  </div>
                )}

                {l.id_frete && (
                  <div style={{
                    background: t.okBg, color: t.okTx, borderRadius: 6,
                    padding: '4px 8px', fontSize: '0.8rem'
                  }}>
                    Frete #{l.id_frete} criado
                  </div>
                )}
              </Card>
            )
          })}
        </div>
      )}
    </div>
  )
}

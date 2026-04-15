import { useState } from 'react'
import { apiPut } from '../api.js'
import { money } from '../helpers.js'
import PhotoPreview from '../components/PhotoPreview.jsx'
import ItemList from '../components/ItemList.jsx'
import Card from '../components/Card.jsx'
import { IconCheck } from '../icons.jsx'

export default function RevisaoLoteScreen({ t, lancamentos, onConfirm, showToast }) {
  const [encomendas, setEncomendas] = useState(() =>
    lancamentos.map(l => ({
      id: l.lancamento.id,
      remetente: l.dados_extraidos.remetente || '',
      destinatario: l.dados_extraidos.destinatario || '',
      rota: l.dados_extraidos.rota || '',
      itens: l.dados_extraidos.itens || [],
      observacoes: l.dados_extraidos.observacoes || ''
    }))
  )
  const [expanded, setExpanded] = useState(() => lancamentos.map(() => true))
  const [loading, setLoading] = useState(false)
  const [progress, setProgress] = useState(null)

  const updateEnc = (idx, field, value) => {
    setEncomendas(prev => prev.map((e, i) => i === idx ? { ...e, [field]: value } : e))
  }

  const updateItem = (encIdx, itemIdx, updated) => {
    setEncomendas(prev => prev.map((e, i) => {
      if (i !== encIdx) return e
      const itens = [...e.itens]
      itens[itemIdx] = updated
      return { ...e, itens }
    }))
  }

  const removeItem = (encIdx, itemIdx) => {
    setEncomendas(prev => prev.map((e, i) => {
      if (i !== encIdx) return e
      return { ...e, itens: e.itens.filter((_, j) => j !== itemIdx) }
    }))
  }

  const addItem = (encIdx) => {
    setEncomendas(prev => prev.map((e, i) => {
      if (i !== encIdx) return e
      return { ...e, itens: [...e.itens, { nome_item: '', quantidade: 1, preco_unitario: 0 }] }
    }))
  }

  const toggle = (idx) => setExpanded(prev => prev.map((v, i) => i === idx ? !v : v))

  const totalGeral = encomendas.reduce((sum, e) =>
    sum + e.itens.reduce((s, i) => s + ((i.quantidade || 0) * (i.preco_unitario || 0)), 0), 0
  )

  const confirmar = async () => {
    const vazias = encomendas.filter(e => e.itens.length === 0)
    if (vazias.length > 0) {
      showToast(`${vazias.length} encomenda(s) sem itens. Adicione pelo menos 1 item em cada.`, 'error')
      return
    }
    setLoading(true)
    setProgress({ current: 0, total: encomendas.length })
    try {
      const ids = []
      for (let i = 0; i < encomendas.length; i++) {
        const enc = encomendas[i]
        const dados_revisados = {
          remetente: enc.remetente,
          destinatario: enc.destinatario,
          rota: enc.rota,
          itens: enc.itens.map(it => ({
            nome_item: it.nome_item,
            quantidade: it.quantidade,
            preco_unitario: it.preco_unitario,
            subtotal: (it.quantidade || 0) * (it.preco_unitario || 0)
          })),
          valor_total: enc.itens.reduce((s, it) => s + ((it.quantidade || 0) * (it.preco_unitario || 0)), 0),
          observacoes: enc.observacoes
        }
        await apiPut(`/ocr/lancamentos/${enc.id}/revisar`, { dados_revisados })
        ids.push(enc.id)
        setProgress({ current: i + 1, total: encomendas.length })
      }
      onConfirm(ids)
    } catch (err) {
      showToast(err.message || 'Erro ao confirmar lote', 'error')
    } finally {
      setLoading(false)
      setProgress(null)
    }
  }

  return (
    <div className="screen-enter" style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 16, paddingBottom: 100 }}>
      <h2 style={{ color: t.tx, fontSize: '1.1rem', fontWeight: 600 }}>
        Revisar Lote — {encomendas.length} encomendas
      </h2>

      <PhotoPreview lancamentoId={lancamentos[0]?.lancamento.id} t={t} />

      {/* Cards de encomendas */}
      {encomendas.map((enc, idx) => {
        const subtotal = enc.itens.reduce((s, i) => s + ((i.quantidade || 0) * (i.preco_unitario || 0)), 0)
        return (
          <Card key={enc.id} t={t} style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {/* Header colapsavel */}
            <button
              onClick={() => toggle(idx)}
              style={{
                display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                background: 'none', border: 'none', cursor: 'pointer', padding: 0, width: '100%'
              }}
            >
              <span style={{ color: t.tx, fontWeight: 700, fontSize: '0.95rem' }}>
                {expanded[idx] ? '▼' : '▶'} Encomenda {idx + 1}
              </span>
              <span style={{ color: t.txSoft, fontSize: '0.85rem' }}>
                {enc.itens.length} {enc.itens.length === 1 ? 'item' : 'itens'} — {money(subtotal)}
              </span>
            </button>

            {/* Resumo quando colapsado */}
            {!expanded[idx] && (enc.remetente || enc.destinatario) && (
              <div style={{ fontSize: '0.8rem', color: t.txMuted, paddingLeft: 20 }}>
                {enc.remetente || '?'} → {enc.destinatario || '?'}
              </div>
            )}

            {/* Conteudo expandido */}
            {expanded[idx] && (
              <>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                  <div>
                    <label style={{ color: t.txSoft, fontSize: '0.8rem', fontWeight: 500 }}>Remetente</label>
                    <input className="input" value={enc.remetente} onChange={e => updateEnc(idx, 'remetente', e.target.value)}
                      placeholder="Quem envia" style={{ background: t.card, color: t.tx, borderColor: t.border, marginTop: 4 }} />
                  </div>
                  <div>
                    <label style={{ color: t.txSoft, fontSize: '0.8rem', fontWeight: 500 }}>Destinatario</label>
                    <input className="input" value={enc.destinatario} onChange={e => updateEnc(idx, 'destinatario', e.target.value)}
                      placeholder="Quem recebe" style={{ background: t.card, color: t.tx, borderColor: t.border, marginTop: 4 }} />
                  </div>
                  <div>
                    <label style={{ color: t.txSoft, fontSize: '0.8rem', fontWeight: 500 }}>Rota</label>
                    <input className="input" value={enc.rota} onChange={e => updateEnc(idx, 'rota', e.target.value)}
                      placeholder="Ex: Manaus - Parintins" style={{ background: t.card, color: t.tx, borderColor: t.border, marginTop: 4 }} />
                  </div>
                </div>

                <ItemList
                  itens={enc.itens} t={t}
                  onUpdate={(itemIdx, updated) => updateItem(idx, itemIdx, updated)}
                  onRemove={(itemIdx) => removeItem(idx, itemIdx)}
                  onAdd={() => addItem(idx)}
                />
              </>
            )}
          </Card>
        )
      })}

      {/* Total geral */}
      <Card t={t} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{ color: t.txSoft, fontWeight: 500 }}>Total ({encomendas.length} encomendas)</span>
        <span style={{ color: t.tx, fontWeight: 700, fontSize: '1.2rem' }}>{money(totalGeral)}</span>
      </Card>

      {/* Progresso */}
      {progress && (
        <div style={{ fontSize: '0.85rem', color: t.txSoft, textAlign: 'center' }}>
          Enviando {progress.current} de {progress.total}...
        </div>
      )}

      {/* Confirmar todas */}
      <button
        className="btn btn-block"
        onClick={confirmar}
        disabled={loading}
        style={{ background: t.priGrad, color: '#fff', padding: 16, fontSize: '1rem' }}
      >
        <IconCheck size={20} color="#fff" />
        {loading ? 'Confirmando...' : `Confirmar ${encomendas.length} Encomendas`}
      </button>
    </div>
  )
}

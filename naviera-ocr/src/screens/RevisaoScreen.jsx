import { useState } from 'react'
import { apiPut, fotoUrl } from '../api.js'
import { money } from '../helpers.js'
import { ConfidenceBadge } from '../components/Badge.jsx'
import Card from '../components/Card.jsx'
import ItemRow from '../components/ItemRow.jsx'
import { IconPlus, IconCheck } from '../icons.jsx'

export default function RevisaoScreen({ t, lancamento, dados, onConfirm, showToast }) {
  const [remetente, setRemetente] = useState(dados.remetente || '')
  const [destinatario, setDestinatario] = useState(dados.destinatario || '')
  const [rota, setRota] = useState(dados.rota || '')
  const [itens, setItens] = useState(dados.itens || [])
  const [loading, setLoading] = useState(false)

  const valorTotal = itens.reduce((sum, i) => sum + ((i.quantidade || 0) * (i.preco_unitario || 0)), 0)

  const updateItem = (idx, updated) => {
    const copy = [...itens]
    copy[idx] = updated
    setItens(copy)
  }

  const removeItem = (idx) => {
    setItens(itens.filter((_, i) => i !== idx))
  }

  const addItem = () => {
    setItens([...itens, { nome_item: '', quantidade: 1, preco_unitario: 0, subtotal: 0 }])
  }

  const confirmar = async () => {
    if (itens.length === 0) {
      showToast('Adicione pelo menos 1 item', 'error')
      return
    }
    setLoading(true)
    try {
      const dadosRevisados = {
        remetente, destinatario, rota,
        itens: itens.map(i => ({
          nome_item: i.nome_item,
          quantidade: i.quantidade,
          preco_unitario: i.preco_unitario,
          subtotal: (i.quantidade || 0) * (i.preco_unitario || 0)
        })),
        valor_total: valorTotal,
        observacoes: dados.observacoes || ''
      }
      await apiPut(`/ocr/lancamentos/${lancamento.id}/revisar`, { dados_revisados: dadosRevisados })
      onConfirm(lancamento.id)
    } catch (err) {
      showToast(err.message || 'Erro ao confirmar', 'error')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="screen-enter" style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 16, paddingBottom: 100 }}>
      <h2 style={{ color: t.tx, fontSize: '1.1rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: 8 }}>
        Revisar Lancamento
        {lancamento.ocr_confianca != null && (
          <ConfidenceBadge value={lancamento.ocr_confianca} t={t} />
        )}
      </h2>

      {/* Thumbnail da foto */}
      <Card t={t} style={{ padding: 8 }}>
        <img
          src={fotoUrl(lancamento.id)}
          alt="Foto original"
          className="img-preview"
          style={{ maxHeight: 180 }}
        />
      </Card>

      {/* Remetente / Destinatario / Rota */}
      <Card t={t} style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <div>
          <label style={{ color: t.txSoft, fontSize: '0.8rem', fontWeight: 500 }}>Remetente</label>
          <input
            className="input"
            value={remetente}
            onChange={(e) => setRemetente(e.target.value)}
            placeholder="Nome do remetente"
            style={{ background: t.card, color: t.tx, borderColor: t.border, marginTop: 4 }}
          />
        </div>
        <div>
          <label style={{ color: t.txSoft, fontSize: '0.8rem', fontWeight: 500 }}>Destinatario</label>
          <input
            className="input"
            value={destinatario}
            onChange={(e) => setDestinatario(e.target.value)}
            placeholder="Nome do destinatario"
            style={{ background: t.card, color: t.tx, borderColor: t.border, marginTop: 4 }}
          />
        </div>
        <div>
          <label style={{ color: t.txSoft, fontSize: '0.8rem', fontWeight: 500 }}>Rota</label>
          <input
            className="input"
            value={rota}
            onChange={(e) => setRota(e.target.value)}
            placeholder="Ex: Manaus - Parintins"
            style={{ background: t.card, color: t.tx, borderColor: t.border, marginTop: 4 }}
          />
        </div>
      </Card>

      {/* Lista de itens */}
      <div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
          <h3 style={{ color: t.tx, fontSize: '0.95rem', fontWeight: 600 }}>
            Itens ({itens.length})
          </h3>
          <button onClick={addItem} className="btn" style={{
            background: t.soft, color: t.pri, border: `1px solid ${t.border}`,
            padding: '6px 12px', fontSize: '0.8rem'
          }}>
            <IconPlus size={14} color={t.pri} /> Adicionar
          </button>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {itens.map((item, idx) => (
            <ItemRow
              key={idx}
              item={item}
              index={idx}
              t={t}
              editable
              onChange={updateItem}
              onRemove={removeItem}
            />
          ))}
        </div>

        {itens.length === 0 && (
          <div style={{
            textAlign: 'center', padding: 24, color: t.txMuted, fontSize: '0.9rem',
            background: t.soft, borderRadius: 10
          }}>
            Nenhum item extraido. Adicione manualmente.
          </div>
        )}
      </div>

      {/* Total */}
      <Card t={t} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{ color: t.txSoft, fontWeight: 500 }}>Total</span>
        <span style={{ color: t.tx, fontWeight: 700, fontSize: '1.2rem' }}>
          {money(valorTotal)}
        </span>
      </Card>

      {/* Botao confirmar */}
      <button
        className="btn btn-block"
        onClick={confirmar}
        disabled={loading}
        style={{ background: t.priGrad, color: '#fff', padding: 16, fontSize: '1rem' }}
      >
        <IconCheck size={20} color="#fff" />
        {loading ? 'Confirmando...' : 'Confirmar e Enviar'}
      </button>
    </div>
  )
}

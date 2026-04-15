import { useState } from 'react'
import { apiPut, apiPost } from '../api.js'
import { money } from '../helpers.js'
import { ConfidenceBadge } from '../components/Badge.jsx'
import Card from '../components/Card.jsx'
import PhotoPreview from '../components/PhotoPreview.jsx'
import ItemList from '../components/ItemList.jsx'
import { IconCheck, IconRefresh } from '../icons.jsx'

export default function RevisaoScreen({ t, lancamento, dados, onConfirm, showToast }) {
  const [remetente, setRemetente] = useState(dados.remetente || '')
  const [destinatario, setDestinatario] = useState(dados.destinatario || '')
  const [rota, setRota] = useState(dados.rota || '')
  const [itens, setItens] = useState(dados.itens || [])
  const [loading, setLoading] = useState(false)
  const [iaLoading, setIaLoading] = useState(false)

  const valorTotal = itens.reduce((sum, i) => sum + ((i.quantidade || 0) * (i.preco_unitario || 0)), 0)

  const updateItem = (idx, updated) => {
    const copy = [...itens]
    copy[idx] = updated
    setItens(copy)
  }

  const removeItem = (idx) => setItens(itens.filter((_, i) => i !== idx))

  const addItem = () => setItens([...itens, { nome_item: '', quantidade: 1, preco_unitario: 0, subtotal: 0 }])

  const revisarComIA = async () => {
    setIaLoading(true)
    showToast('Analisando com IA... aguarde', 'info')
    try {
      const result = await apiPost(`/ocr/lancamentos/${lancamento.id}/ia-review`, {})
      if (result?.dados_extraidos) {
        const d = result.dados_extraidos
        if (itens.length > 0 && d.itens && d.itens.length > 0) {
          if (!window.confirm(`A IA retornou ${d.itens.length} itens. Deseja substituir seus ${itens.length} itens editados?`)) return
        }
        setRemetente(d.remetente || remetente)
        setDestinatario(d.destinatario || destinatario)
        setRota(d.rota || rota)
        setItens(d.itens || [])
        showToast(`IA identificou ${d.itens?.length || 0} itens`, 'success')
      }
    } catch (err) {
      console.error('[IA Review]', err)
      showToast(err.message || 'Erro na revisao por IA', 'error')
    } finally {
      setIaLoading(false)
    }
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

      <PhotoPreview lancamentoId={lancamento.id} t={t} />

      {/* Botao Revisar com IA */}
      <button
        className="btn btn-block"
        onClick={revisarComIA}
        disabled={iaLoading || loading}
        style={{
          background: iaLoading ? t.soft : 'linear-gradient(135deg, #6366F1, #8B5CF6)',
          color: iaLoading ? t.txMuted : '#fff',
          padding: 14, fontSize: '0.95rem',
          border: iaLoading ? `1px solid ${t.border}` : 'none'
        }}
      >
        {iaLoading ? (
          <span className="pulse">Analisando com IA...</span>
        ) : (
          <><IconRefresh size={18} color="#fff" /> Revisar com IA</>
        )}
      </button>

      {/* Remetente / Destinatario / Rota */}
      <Card t={t} style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {[
          { label: 'Remetente', value: remetente, set: setRemetente, placeholder: 'Nome do remetente' },
          { label: 'Destinatario', value: destinatario, set: setDestinatario, placeholder: 'Nome do destinatario' },
          { label: 'Rota', value: rota, set: setRota, placeholder: 'Ex: Manaus - Parintins' }
        ].map(f => (
          <div key={f.label}>
            <label style={{ color: t.txSoft, fontSize: '0.8rem', fontWeight: 500 }}>{f.label}</label>
            <input
              className="input"
              value={f.value}
              onChange={(e) => f.set(e.target.value)}
              placeholder={f.placeholder}
              style={{ background: t.card, color: t.tx, borderColor: t.border, marginTop: 4 }}
            />
          </div>
        ))}
      </Card>

      <ItemList itens={itens} t={t} onUpdate={updateItem} onRemove={removeItem} onAdd={addItem} />

      {/* Total */}
      <Card t={t} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{ color: t.txSoft, fontWeight: 500 }}>Total</span>
        <span style={{ color: t.tx, fontWeight: 700, fontSize: '1.2rem' }}>{money(valorTotal)}</span>
      </Card>

      {/* Botao confirmar */}
      <button
        className="btn btn-block"
        onClick={confirmar}
        disabled={loading || iaLoading}
        style={{ background: t.priGrad, color: '#fff', padding: 16, fontSize: '1rem' }}
      >
        <IconCheck size={20} color="#fff" />
        {loading ? 'Confirmando...' : 'Confirmar e Enviar'}
      </button>
    </div>
  )
}

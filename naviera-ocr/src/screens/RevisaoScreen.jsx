import { useState, useRef } from 'react'
import { apiPut, apiPost, uploadFotoAdicional } from '../api.js'
import { money } from '../helpers.js'
import { ConfidenceBadge } from '../components/Badge.jsx'
import Card from '../components/Card.jsx'
import PhotoPreview from '../components/PhotoPreview.jsx'
import ItemList from '../components/ItemList.jsx'
import { IconCheck, IconRefresh, IconCamera, IconPlus } from '../icons.jsx'

// Compress image (same logic as CameraCapture)
function compressImage(file, maxSize = 2048) {
  return new Promise((resolve) => {
    const img = new Image()
    const url = URL.createObjectURL(file)
    img.onload = () => {
      URL.revokeObjectURL(url)
      let { width, height } = img
      if (width <= maxSize && height <= maxSize) { resolve(file); return }
      const ratio = Math.min(maxSize / width, maxSize / height)
      const canvas = document.createElement('canvas')
      canvas.width = width * ratio
      canvas.height = height * ratio
      canvas.getContext('2d').drawImage(img, 0, 0, canvas.width, canvas.height)
      canvas.toBlob(blob => resolve(blob || file), 'image/jpeg', 0.85)
    }
    img.onerror = () => { URL.revokeObjectURL(url); resolve(file) }
    img.src = url
  })
}

export default function RevisaoScreen({ t, lancamento, dados, onConfirm, showToast }) {
  const [numeroNota, setNumeroNota] = useState(dados.numero_nota || '')
  const [remetente, setRemetente] = useState(dados.remetente || '')
  const [destinatario, setDestinatario] = useState(dados.destinatario || '')
  const [rota, setRota] = useState(dados.rota || '')
  const [itens, setItens] = useState(dados.itens || [])
  const [loading, setLoading] = useState(false)
  const [iaLoading, setIaLoading] = useState(false)
  const [fotoLoading, setFotoLoading] = useState(false)
  const [fotosAdicionais, setFotosAdicionais] = useState(0)
  const [docRemetente, setDocRemetente] = useState(dados.doc_remetente || null) // {tipo_doc, numero_doc, foto_doc_path}
  const fotoCameraRef = useRef(null)
  const fotoGaleriaRef = useRef(null)

  const isEncomenda = lancamento.tipo === 'encomenda' || lancamento.tipo === 'lote'
  const valorTotal = itens.reduce((sum, i) => sum + ((i.quantidade || 0) * (i.preco_unitario || 0)), 0)

  const updateItem = (idx, updated) => {
    const copy = [...itens]
    copy[idx] = updated
    setItens(copy)
  }

  const removeItem = (idx) => setItens(itens.filter((_, i) => i !== idx))

  const addItem = () => setItens([...itens, { nome_item: '', quantidade: 1, preco_unitario: 0, subtotal: 0 }])

  // Adicionar foto extra (encomenda multi-volume)
  const handleFotoAdicional = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    e.target.value = ''

    setFotoLoading(true)
    showToast('Analisando foto...', 'info')
    try {
      const compressed = await compressImage(file)
      const fotoFile = new File([compressed], file.name || 'foto.jpg', { type: 'image/jpeg' })
      const result = await uploadFotoAdicional(lancamento.id, fotoFile)

      if (result?.tipo === 'documento') {
        if (result.nome) setRemetente(result.nome)
        setDocRemetente({ tipo_doc: result.tipo_doc, cpf: result.cpf || '', rg: result.rg || '', foto_doc_path: result.foto_doc_path })
        showToast(`${result.tipo_doc || 'Documento'} identificado: ${result.nome || 'sem nome'}`, 'success')
      } else if (result?.itens?.length > 0) {
        // Item fisico
        setItens(prev => [...prev, ...result.itens])
        setFotosAdicionais(prev => prev + 1)
        showToast(`${result.itens.length} item(ns) adicionado(s)`, 'success')
      } else {
        showToast('Nenhum item ou documento identificado na foto', 'warn')
      }
    } catch (err) {
      showToast(err.message || 'Erro ao processar foto', 'error')
    } finally {
      setFotoLoading(false)
    }
  }

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
        if (d.numero_nota) setNumeroNota(d.numero_nota)
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
        numero_nota: numeroNota || '',
        remetente, destinatario, rota,
        itens: itens.map(i => ({
          nome_item: i.nome_item,
          quantidade: i.quantidade,
          preco_unitario: i.preco_unitario,
          subtotal: (i.quantidade || 0) * (i.preco_unitario || 0)
        })),
        valor_total: valorTotal,
        observacoes: dados.observacoes || '',
        doc_remetente: docRemetente || null
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
        disabled={iaLoading || loading || fotoLoading}
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

      {/* Numero da Nota + Modo Marcador */}
      {!isEncomenda && (
        <Card t={t} style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={{ flex: '0 0 auto' }}>
            <label style={{ color: t.txSoft, fontSize: '0.8rem', fontWeight: 500 }}>N. Nota</label>
            <input className="input" value={numeroNota} onChange={e => setNumeroNota(e.target.value)}
              placeholder="826"
              inputMode="numeric"
              style={{
                background: t.card, color: t.tx, borderColor: t.border, marginTop: 4,
                fontSize: '1.3rem', fontWeight: 700, textAlign: 'center',
                width: 100, letterSpacing: 2
              }} />
          </div>
          <div style={{ flex: 1 }}>
            {dados.modo_marcador && (
              <span style={{
                display: 'inline-block', padding: '4px 10px', borderRadius: 6,
                background: t.warnBg || 'rgba(245,158,11,0.15)',
                color: t.warnTx || '#B45309',
                fontSize: '0.78rem', fontWeight: 600
              }}>
                MARCADOR — itens simplificados
              </span>
            )}
          </div>
        </Card>
      )}

      {/* Remetente / Destinatario / Rota */}
      <Card t={t} style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <div>
          <label style={{ color: t.txSoft, fontSize: '0.8rem', fontWeight: 500 }}>Remetente</label>
          <input className="input" value={remetente} onChange={e => setRemetente(e.target.value)}
            placeholder="Nome do remetente" style={{ background: t.card, color: t.tx, borderColor: t.border, marginTop: 4 }} />
        </div>

        {/* Documento do remetente */}
        {isEncomenda && docRemetente && (
          <div style={{
            background: t.okBg, borderRadius: 8, padding: '8px 12px',
            display: 'flex', flexDirection: 'column', gap: 6
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontSize: '0.8rem', fontWeight: 600, color: t.okTx }}>
                {docRemetente.tipo_doc || 'Documento'} arquivado
              </span>
              <button onClick={() => setDocRemetente(null)} style={{
                background: 'none', border: 'none', cursor: 'pointer', fontSize: '0.75rem', color: t.txMuted
              }}>remover</button>
            </div>
            <div>
              <label style={{ color: t.okTx, fontSize: '0.75rem', fontWeight: 500 }}>CPF</label>
              <input className="input" value={docRemetente.cpf || ''} onChange={e => setDocRemetente(prev => ({ ...prev, cpf: e.target.value }))}
                placeholder="000.000.000-00" style={{ background: t.card, color: t.tx, borderColor: t.border, fontSize: '0.85rem', marginTop: 2 }} />
            </div>
            <div>
              <label style={{ color: t.okTx, fontSize: '0.75rem', fontWeight: 500 }}>RG</label>
              <input className="input" value={docRemetente.rg || ''} onChange={e => setDocRemetente(prev => ({ ...prev, rg: e.target.value }))}
                placeholder="0000000" style={{ background: t.card, color: t.tx, borderColor: t.border, fontSize: '0.85rem', marginTop: 2 }} />
            </div>
          </div>
        )}

        <div>
          <label style={{ color: t.txSoft, fontSize: '0.8rem', fontWeight: 500 }}>Destinatario</label>
          <input className="input" value={destinatario} onChange={e => setDestinatario(e.target.value)}
            placeholder="Nome do destinatario" style={{ background: t.card, color: t.tx, borderColor: t.border, marginTop: 4 }} />
        </div>
        <div>
          <label style={{ color: t.txSoft, fontSize: '0.8rem', fontWeight: 500 }}>Rota</label>
          <input className="input" value={rota} onChange={e => setRota(e.target.value)}
            placeholder="Ex: Manaus - Parintins" style={{ background: t.card, color: t.tx, borderColor: t.border, marginTop: 4 }} />
        </div>
      </Card>

      <ItemList itens={itens} t={t} onUpdate={updateItem} onRemove={removeItem} onAdd={addItem} />

      {/* Adicionar Foto — so para encomenda */}
      {isEncomenda && (
        fotoLoading ? (
          <div className="btn btn-block" style={{
            background: t.soft, color: t.txMuted, padding: 14, fontSize: '0.95rem',
            border: `1px solid ${t.border}`, textAlign: 'center'
          }}>
            <span className="pulse">Identificando item...</span>
          </div>
        ) : (
          <div style={{ display: 'flex', gap: 8 }}>
            <button
              className="btn"
              onClick={() => fotoCameraRef.current?.click()}
              disabled={loading}
              style={{
                flex: 1, background: t.card, color: t.pri, padding: 12, fontSize: '0.9rem',
                border: `1px solid ${t.border}`, gap: 6
              }}
            >
              <IconCamera size={18} color={t.pri} /> Foto
            </button>
            <button
              className="btn"
              onClick={() => fotoGaleriaRef.current?.click()}
              disabled={loading}
              style={{
                flex: 1, background: t.card, color: t.pri, padding: 12, fontSize: '0.9rem',
                border: `1px solid ${t.border}`, gap: 6
              }}
            >
              <IconPlus size={18} color={t.pri} /> Galeria
            </button>
            {fotosAdicionais > 0 && (
              <span style={{
                alignSelf: 'center', fontSize: '0.8rem', color: t.txMuted, whiteSpace: 'nowrap'
              }}>+{fotosAdicionais}</span>
            )}
          </div>
        )
      )}
      <input ref={fotoCameraRef} type="file" accept="image/*" capture="environment" onChange={handleFotoAdicional} style={{ display: 'none' }} />
      <input ref={fotoGaleriaRef} type="file" accept="image/*" onChange={handleFotoAdicional} style={{ display: 'none' }} />

      {/* Total */}
      <Card t={t} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{ color: t.txSoft, fontWeight: 500 }}>
          Total{fotosAdicionais > 0 ? ` (${1 + fotosAdicionais} fotos)` : ''}
        </span>
        <span style={{ color: t.tx, fontWeight: 700, fontSize: '1.2rem' }}>{money(valorTotal)}</span>
      </Card>

      {/* Botao confirmar */}
      <button
        className="btn btn-block"
        onClick={confirmar}
        disabled={loading || iaLoading || fotoLoading}
        style={{ background: t.priGrad, color: '#fff', padding: 16, fontSize: '1rem' }}
      >
        <IconCheck size={20} color="#fff" />
        {loading ? 'Confirmando...' : 'Confirmar e Enviar'}
      </button>
    </div>
  )
}

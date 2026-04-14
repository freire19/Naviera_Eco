import { useState, useEffect } from 'react'
import { apiGet, uploadFoto } from '../api.js'
import CameraCapture from '../components/CameraCapture.jsx'
import Card from '../components/Card.jsx'

export default function CapturaScreen({ t, onResult, isOnline, onOfflineAdd, showToast }) {
  const [viagens, setViagens] = useState([])
  const [viagemId, setViagemId] = useState('')
  const [fotoBlob, setFotoBlob] = useState(null)
  const [fotoName, setFotoName] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    apiGet('/viagens').then(v => {
      if (v) {
        setViagens(v)
        // Selecionar viagem ativa por padrao
        const ativa = v.find(vi => vi.ativa || vi.status === 'ativa')
        if (ativa) setViagemId(String(ativa.id_viagem))
      }
    }).catch(() => {})
  }, [])

  const handleCapture = (blob, name) => {
    setFotoBlob(blob)
    setFotoName(name || '')
  }

  const enviar = async () => {
    if (!fotoBlob) {
      showToast('Nenhuma foto selecionada', 'error')
      return
    }

    // Se offline, salvar na fila
    if (!isOnline) {
      await onOfflineAdd(fotoBlob, viagemId || null)
      showToast('Foto salva na fila offline. Sera enviada quando houver internet.', 'warn')
      setFotoBlob(null)
      setFotoName('')
      return
    }

    setLoading(true)
    showToast('Enviando foto e processando OCR...', 'info')
    try {
      // fotoBlob pode ser File ou Blob — garantir que e File
      let file = fotoBlob
      if (!(fotoBlob instanceof File)) {
        file = new File([fotoBlob], fotoName || 'foto.jpg', { type: fotoBlob.type || 'image/jpeg' })
      }
      const result = await uploadFoto(file, viagemId || null)
      if (result) {
        showToast('Foto processada! Revise os itens.', 'success')
        onResult(result)
      } else {
        showToast('Resposta vazia do servidor', 'error')
      }
    } catch (err) {
      console.error('[OCR] Erro no upload:', err)
      showToast(err.message || 'Erro ao enviar foto', 'error')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="screen-enter" style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 16 }}>
      <h2 style={{ color: t.tx, fontSize: '1.1rem', fontWeight: 600 }}>Nova Captura</h2>

      {/* Seletor de viagem */}
      <Card t={t}>
        <label style={{ color: t.txSoft, fontSize: '0.85rem', fontWeight: 500, marginBottom: 6, display: 'block' }}>
          Viagem
        </label>
        <select
          className="select"
          value={viagemId}
          onChange={(e) => setViagemId(e.target.value)}
          style={{ background: t.card, color: t.tx, borderColor: t.border }}
        >
          <option value="">Selecione a viagem...</option>
          {viagens.map(v => (
            <option key={v.id_viagem} value={v.id_viagem}>
              Viagem {v.numero_viagem || v.id_viagem} {v.ativa ? '(Ativa)' : ''}
            </option>
          ))}
        </select>
      </Card>

      {/* Camera */}
      <Card t={t}>
        <label style={{ color: t.txSoft, fontSize: '0.85rem', fontWeight: 500, marginBottom: 10, display: 'block' }}>
          Foto da nota / cupom / caderno
        </label>
        <CameraCapture t={t} onCapture={handleCapture} disabled={loading} />
      </Card>

      {/* Botao enviar */}
      {fotoBlob && (
        <button
          className="btn btn-block"
          onClick={enviar}
          disabled={loading}
          style={{ background: t.priGrad, color: '#fff', padding: 16, fontSize: '1rem' }}
        >
          {loading ? (
            <span className="pulse">Processando OCR...</span>
          ) : isOnline ? (
            'Enviar para analise'
          ) : (
            'Salvar na fila offline'
          )}
        </button>
      )}

      {/* Dica */}
      <p style={{ color: t.txMuted, fontSize: '0.8rem', textAlign: 'center' }}>
        Tire uma foto legivel da nota fiscal, cupom ou caderno. O sistema vai extrair os itens automaticamente.
      </p>
    </div>
  )
}

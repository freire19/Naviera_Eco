import { useState, useEffect, useRef } from 'react'
import { apiGet, uploadFoto } from '../api.js'
import CameraCapture from '../components/CameraCapture.jsx'
import Card from '../components/Card.jsx'
import { IconCamera, IconPlus, IconX } from '../icons.jsx'

const LOADING_MSG = {
  frete: 'Processando OCR...',
  encomenda: 'Identificando item com IA...',
  lote: 'Extraindo encomendas do protocolo...'
}

const CAMERA_LABEL = {
  frete: 'Foto da nota / cupom / caderno',
  encomenda: 'Foto do item / pacote / mercadoria',
  lote: 'Foto da pagina do protocolo (varias encomendas)'
}

const DICA = {
  frete: 'Tire uma foto legivel da nota fiscal, cupom ou caderno. Se a nota tem varias paginas, adicione todas antes de enviar.',
  encomenda: 'Tire uma foto do item ou pacote. A IA vai identificar o que e e criar a encomenda.',
  lote: 'Tire uma foto da pagina do protocolo de encomendas. A IA vai separar cada encomenda automaticamente.'
}

// Comprime imagem (mesma logica do CameraCapture)
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

export default function CapturaScreen({ t, onResult, showToast }) {
  const [viagens, setViagens] = useState([])
  const [viagemId, setViagemId] = useState('')
  const [tipo, setTipo] = useState('frete')
  const [lote, setLote] = useState(false)
  const [fotoBlob, setFotoBlob] = useState(null)
  const [fotoName, setFotoName] = useState('')
  const [loading, setLoading] = useState(false)

  // Multi-foto para fretes
  const [fotosExtras, setFotosExtras] = useState([]) // [{ blob, name, preview }]
  const extraCameraRef = useRef(null)
  const extraGaleriaRef = useRef(null)

  useEffect(() => {
    apiGet('/viagens').then(v => {
      if (v) {
        setViagens(v)
        const ativa = v.find(vi => vi.ativa || vi.status === 'ativa')
        if (ativa) setViagemId(String(ativa.id_viagem))
      }
    }).catch((err) => {
      if (showToast) showToast('Erro ao carregar viagens: ' + (err.message || 'sem conexao'))
    })
  }, [])

  const handleCapture = (blob, name) => {
    setFotoBlob(blob)
    setFotoName(name || '')
    // Limpar fotos extras ao trocar a foto principal
    fotosExtras.forEach(f => URL.revokeObjectURL(f.preview))
    setFotosExtras([])
  }

  // Adicionar pagina extra (frete multi-pagina)
  const handleFotoExtra = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    e.target.value = ''

    const compressed = await compressImage(file)
    const preview = URL.createObjectURL(compressed)
    setFotosExtras(prev => [...prev, { blob: compressed, name: file.name, preview }])
    showToast(`Pagina ${fotosExtras.length + 2} adicionada`, 'success')
  }

  const removerFotoExtra = (idx) => {
    setFotosExtras(prev => {
      const copy = [...prev]
      URL.revokeObjectURL(copy[idx].preview)
      copy.splice(idx, 1)
      return copy
    })
  }

  const enviar = async () => {
    if (!fotoBlob) {
      showToast('Nenhuma foto selecionada', 'error')
      return
    }

    setLoading(true)
    const totalFotos = 1 + fotosExtras.length
    showToast(totalFotos > 1 ? `Processando ${totalFotos} paginas...` : LOADING_MSG[tipo], 'info')
    try {
      // Montar array de arquivos
      const mainFile = fotoBlob instanceof File
        ? fotoBlob
        : new File([fotoBlob], fotoName || 'foto.jpg', { type: fotoBlob.type || 'image/jpeg' })

      let files
      if (tipo === 'frete' && fotosExtras.length > 0) {
        const extraFiles = fotosExtras.map((f, i) =>
          new File([f.blob], f.name || `pagina${i + 2}.jpg`, { type: 'image/jpeg' })
        )
        files = [mainFile, ...extraFiles]
      } else {
        files = mainFile // single file
      }

      const result = await uploadFoto(files, viagemId || null, tipo)
      if (result) {
        const msg = result.tipo === 'lote'
          ? `${result.lancamentos.length} encomendas extraidas! Revise.`
          : totalFotos > 1
            ? `${totalFotos} paginas processadas! Revise os itens.`
            : 'Foto processada! Revise os itens.'
        showToast(msg, 'success')
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

  const tabStyle = (active) => ({
    flex: 1, padding: '10px 0', textAlign: 'center', fontWeight: 600, fontSize: '0.85rem',
    cursor: 'pointer', borderRadius: 8, border: 'none', transition: 'all 0.2s',
    background: active ? t.pri : t.soft,
    color: active ? '#fff' : t.txMuted
  })

  return (
    <div className="screen-enter" style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 16 }}>
      <h2 style={{ color: t.tx, fontSize: '1.1rem', fontWeight: 600 }}>Nova Captura</h2>

      {/* Toggle Frete / Encomenda */}
      <div style={{ display: 'flex', gap: 6, background: t.soft, borderRadius: 10, padding: 4 }}>
        <button style={tabStyle(tipo === 'frete')} onClick={() => { setTipo('frete'); setLote(false) }}>Frete</button>
        <button style={tabStyle(tipo === 'encomenda' || tipo === 'lote')} onClick={() => setTipo(lote ? 'lote' : 'encomenda')}>Encomenda</button>
      </div>

      {/* Toggle Lote — dentro de encomenda */}
      {(tipo === 'encomenda' || tipo === 'lote') && (
        <div
          onClick={() => { setLote(!lote); setTipo(!lote ? 'lote' : 'encomenda') }}
          style={{
            display: 'flex', alignItems: 'center', gap: 10, cursor: 'pointer',
            padding: '8px 12px', background: t.soft, borderRadius: 8
          }}
        >
          <div style={{
            width: 40, height: 22, borderRadius: 11, padding: 2,
            background: lote ? t.pri : t.border, transition: 'background 0.2s'
          }}>
            <div style={{
              width: 18, height: 18, borderRadius: '50%', background: '#fff',
              transition: 'transform 0.2s',
              transform: lote ? 'translateX(18px)' : 'translateX(0)'
            }} />
          </div>
          <span style={{ color: t.tx, fontSize: '0.85rem', fontWeight: 500 }}>
            Lote (varias encomendas na mesma foto)
          </span>
        </div>
      )}

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
          {CAMERA_LABEL[tipo]}
        </label>
        <CameraCapture t={t} onCapture={handleCapture} disabled={loading} />
      </Card>

      {/* Adicionar mais paginas — so para frete, apos primeira foto */}
      {tipo === 'frete' && fotoBlob && !loading && (
        <Card t={t}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: fotosExtras.length > 0 ? 10 : 0 }}>
            <label style={{ color: t.txSoft, fontSize: '0.85rem', fontWeight: 500 }}>
              Paginas adicionais {fotosExtras.length > 0 && `(${fotosExtras.length})`}
            </label>
            <div style={{ display: 'flex', gap: 6 }}>
              <button
                className="btn"
                onClick={() => extraCameraRef.current?.click()}
                style={{
                  background: t.card, color: t.pri, padding: '6px 12px', fontSize: '0.8rem',
                  border: `1px solid ${t.border}`, gap: 4
                }}
              >
                <IconCamera size={14} color={t.pri} /> Foto
              </button>
              <button
                className="btn"
                onClick={() => extraGaleriaRef.current?.click()}
                style={{
                  background: t.card, color: t.pri, padding: '6px 12px', fontSize: '0.8rem',
                  border: `1px solid ${t.border}`, gap: 4
                }}
              >
                <IconPlus size={14} color={t.pri} /> Galeria
              </button>
            </div>
          </div>

          {/* Thumbnails das paginas extras */}
          {fotosExtras.length > 0 && (
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              {fotosExtras.map((f, idx) => (
                <div key={idx} style={{ position: 'relative', width: 72, height: 72 }}>
                  <img src={f.preview} alt={`Pagina ${idx + 2}`} style={{
                    width: 72, height: 72, objectFit: 'cover', borderRadius: 8,
                    border: `1px solid ${t.border}`
                  }} />
                  <button onClick={() => removerFotoExtra(idx)} style={{
                    position: 'absolute', top: -6, right: -6, background: 'rgba(220,53,69,0.9)',
                    border: 'none', borderRadius: '50%', width: 20, height: 20,
                    display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer'
                  }}>
                    <IconX size={12} color="#fff" />
                  </button>
                  <span style={{
                    position: 'absolute', bottom: 2, left: 0, right: 0, textAlign: 'center',
                    fontSize: '0.65rem', color: '#fff', textShadow: '0 1px 2px rgba(0,0,0,0.8)'
                  }}>p.{idx + 2}</span>
                </div>
              ))}
            </div>
          )}

          <input ref={extraCameraRef} type="file" accept="image/*" capture="environment" onChange={handleFotoExtra} style={{ display: 'none' }} />
          <input ref={extraGaleriaRef} type="file" accept="image/*" onChange={handleFotoExtra} style={{ display: 'none' }} />
        </Card>
      )}

      {/* Botao enviar */}
      {fotoBlob && (
        <button
          className="btn btn-block"
          onClick={enviar}
          disabled={loading}
          style={{ background: t.priGrad, color: '#fff', padding: 16, fontSize: '1rem' }}
        >
          {loading ? (
            <span className="pulse">{fotosExtras.length > 0 ? `Processando ${1 + fotosExtras.length} paginas...` : LOADING_MSG[tipo]}</span>
          ) : (
            fotosExtras.length > 0
              ? `Enviar ${1 + fotosExtras.length} paginas para analise`
              : 'Enviar para analise'
          )}
        </button>
      )}

      {/* Dica */}
      <p style={{ color: t.txMuted, fontSize: '0.8rem', textAlign: 'center' }}>
        {DICA[tipo]}
      </p>
    </div>
  )
}

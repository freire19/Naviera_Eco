import { useState, useEffect } from 'react'
import { apiGet, uploadFoto } from '../api.js'
import CameraCapture from '../components/CameraCapture.jsx'
import Card from '../components/Card.jsx'

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
  frete: 'Tire uma foto legivel da nota fiscal, cupom ou caderno. O sistema vai extrair os itens automaticamente.',
  encomenda: 'Tire uma foto do item ou pacote. A IA vai identificar o que e e criar a encomenda.',
  lote: 'Tire uma foto da pagina do protocolo de encomendas. A IA vai separar cada encomenda automaticamente.'
}

export default function CapturaScreen({ t, onResult, showToast }) {
  const [viagens, setViagens] = useState([])
  const [viagemId, setViagemId] = useState('')
  const [tipo, setTipo] = useState('frete')
  const [lote, setLote] = useState(false)
  const [fotoBlob, setFotoBlob] = useState(null)
  const [fotoName, setFotoName] = useState('')
  const [loading, setLoading] = useState(false)

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
  }

  const enviar = async () => {
    if (!fotoBlob) {
      showToast('Nenhuma foto selecionada', 'error')
      return
    }

    setLoading(true)
    showToast(LOADING_MSG[tipo], 'info')
    try {
      let file = fotoBlob
      if (!(fotoBlob instanceof File)) {
        file = new File([fotoBlob], fotoName || 'foto.jpg', { type: fotoBlob.type || 'image/jpeg' })
      }
      const result = await uploadFoto(file, viagemId || null, tipo)
      if (result) {
        const msg = result.tipo === 'lote'
          ? `${result.lancamentos.length} encomendas extraidas! Revise.`
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

      {/* Botao enviar */}
      {fotoBlob && (
        <button
          className="btn btn-block"
          onClick={enviar}
          disabled={loading}
          style={{ background: t.priGrad, color: '#fff', padding: 16, fontSize: '1rem' }}
        >
          {loading ? (
            <span className="pulse">{LOADING_MSG[tipo]}</span>
          ) : (
            'Enviar para analise'
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

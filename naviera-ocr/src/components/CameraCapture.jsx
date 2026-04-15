import { useRef, useState } from 'react'
import { IconCamera, IconImage, IconX } from '../icons.jsx'

// Comprime imagem para max 2048px de lado maior
function compressImage(file, maxSize = 2048) {
  return new Promise((resolve, reject) => {
    const img = new Image()
    const url = URL.createObjectURL(file)
    img.onload = () => {
      URL.revokeObjectURL(url)
      let { width, height } = img

      if (width <= maxSize && height <= maxSize) {
        resolve(file)
        return
      }
      const ratio = Math.min(maxSize / width, maxSize / height)
      width *= ratio
      height *= ratio
      const canvas = document.createElement('canvas')
      canvas.width = width
      canvas.height = height
      const ctx = canvas.getContext('2d')
      ctx.drawImage(img, 0, 0, width, height)
      canvas.toBlob(blob => resolve(blob || file), 'image/jpeg', 0.85)
    }
    img.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('Imagem invalida'))
    }
    img.src = url
  })
}

export default function CameraCapture({ t, onCapture, disabled }) {
  const cameraRef = useRef(null)
  const galleryRef = useRef(null)
  const [preview, setPreview] = useState(null)

  const handleFile = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    const compressed = await compressImage(file)
    const previewUrl = URL.createObjectURL(compressed)
    setPreview(previewUrl)
    onCapture(compressed, file.name)
  }

  const clear = () => {
    if (preview) URL.revokeObjectURL(preview)
    setPreview(null)
    onCapture(null, null)
    if (cameraRef.current) cameraRef.current.value = ''
    if (galleryRef.current) galleryRef.current.value = ''
  }

  return (
    <div>
      {preview ? (
        <div style={{ position: 'relative' }}>
          <img src={preview} alt="Preview" className="img-preview" style={{ maxHeight: 300 }} />
          <button onClick={clear} style={{
            position: 'absolute', top: 8, right: 8, background: 'rgba(0,0,0,0.6)',
            border: 'none', borderRadius: '50%', width: 32, height: 32,
            display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer'
          }}>
            <IconX size={18} color="#fff" />
          </button>
        </div>
      ) : (
        <div style={{
          display: 'flex', gap: 12, justifyContent: 'center'
        }}>
          {/* Camera */}
          <button
            onClick={() => cameraRef.current?.click()}
            disabled={disabled}
            className="btn"
            style={{
              background: t.priGrad, color: '#fff', flex: 1, padding: '20px 16px',
              flexDirection: 'column', gap: 8
            }}
          >
            <IconCamera size={32} color="#fff" />
            <span>Tirar Foto</span>
          </button>
          <input ref={cameraRef} type="file" accept="image/*" capture="environment" onChange={handleFile} style={{ display: 'none' }} />

          {/* Gallery */}
          <button
            onClick={() => galleryRef.current?.click()}
            disabled={disabled}
            className="btn"
            style={{
              background: t.soft, color: t.tx, flex: 1, padding: '20px 16px',
              border: `1px solid ${t.border}`, flexDirection: 'column', gap: 8
            }}
          >
            <IconImage size={32} color={t.txSoft} />
            <span>Galeria</span>
          </button>
          <input ref={galleryRef} type="file" accept="image/*" onChange={handleFile} style={{ display: 'none' }} />
        </div>
      )}
    </div>
  )
}

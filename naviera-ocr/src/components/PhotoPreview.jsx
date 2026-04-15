import { useState, useEffect } from 'react'
import { fetchFoto } from '../api.js'
import Card from './Card.jsx'

export default function PhotoPreview({ lancamentoId, t }) {
  const [blobUrl, setBlobUrl] = useState(null)

  useEffect(() => {
    let revoked = false
    fetchFoto(lancamentoId).then(url => {
      if (!revoked) setBlobUrl(url)
    })
    return () => {
      revoked = true
      if (blobUrl) URL.revokeObjectURL(blobUrl)
    }
  }, [lancamentoId])

  return (
    <Card t={t} style={{ padding: 8 }}>
      <img
        src={blobUrl || ''}
        alt="Foto original"
        className="img-preview"
        style={{ maxHeight: 180 }}
      />
    </Card>
  )
}

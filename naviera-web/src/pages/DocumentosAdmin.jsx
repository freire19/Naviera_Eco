import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

function fmtDate(d) {
  return d ? new Date(d).toLocaleDateString('pt-BR') : '—'
}

const CATEGORIAS = [
  { key: '', label: 'Todos', icon: '📁' },
  { key: 'encomenda', label: 'Encomendas', icon: '📦' },
  { key: 'lote', label: 'Lotes', icon: '📋' },
  { key: 'frete', label: 'Fretes', icon: '🚚' }
]

export default function DocumentosAdmin() {
  const [docs, setDocs] = useState([])
  const [loading, setLoading] = useState(true)
  const [categoria, setCategoria] = useState('')
  const [toast, setToast] = useState(null)
  const [fotoUrl, setFotoUrl] = useState(null)
  const [fotoInfo, setFotoInfo] = useState(null)

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  const carregar = useCallback(async () => {
    setLoading(true)
    try {
      const params = categoria ? `?categoria=${categoria}` : ''
      const result = await api.get(`/ocr/documentos${params}`)
      setDocs(Array.isArray(result) ? result : [])
    } catch (err) {
      if (err.status === 403) {
        showToast('Acesso restrito a administradores', 'error')
      } else {
        showToast('Erro ao carregar documentos', 'error')
      }
    } finally {
      setLoading(false)
    }
  }, [categoria])

  useEffect(() => { carregar() }, [carregar])

  const verFoto = async (doc) => {
    try {
      const token = localStorage.getItem('naviera_token')
      const res = await fetch(`/api/ocr/lancamentos/${doc.id}/doc-foto`, {
        headers: { Authorization: `Bearer ${token}` }
      })
      if (!res.ok) throw new Error('Foto nao disponivel')
      const blob = await res.blob()
      setFotoUrl(URL.createObjectURL(blob))
      setFotoInfo(doc)
    } catch (err) {
      showToast(err.message, 'error')
    }
  }

  const fecharFoto = () => {
    if (fotoUrl) URL.revokeObjectURL(fotoUrl)
    setFotoUrl(null)
    setFotoInfo(null)
  }

  const tipoLabel = (tipo) => {
    const map = { encomenda: 'Encomenda', lote: 'Lote', frete: 'Frete' }
    return map[tipo] || tipo
  }

  return (
    <div>
      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}

      <div className="card">
        <div className="card-header">
          <h3 style={{ margin: 0 }}>Documentos Arquivados</h3>
          <button className="btn btn-sm" onClick={carregar} disabled={loading}>
            {loading ? 'Carregando...' : 'Atualizar'}
          </button>
        </div>

        <p style={{ color: '#888', fontSize: '0.85rem', margin: '0 0 12px' }}>
          Fotos de documentos de identidade (RG, CNH, CPF) anexados nas encomendas. Acesso restrito ao administrador.
        </p>

        {/* Categorias */}
        <div style={{ display: 'flex', gap: 6, marginBottom: 16, flexWrap: 'wrap' }}>
          {CATEGORIAS.map(c => (
            <button
              key={c.key}
              className={`btn btn-sm ${categoria === c.key ? 'btn-primary' : 'btn-outline'}`}
              onClick={() => setCategoria(c.key)}
            >
              {c.icon} {c.label}
            </button>
          ))}
        </div>

        {/* Contagem */}
        {!loading && (
          <div style={{ fontSize: '0.85rem', color: '#666', marginBottom: 12 }}>
            {docs.length} documento(s) encontrado(s)
          </div>
        )}

        {/* Grid de documentos */}
        {loading ? (
          <div className="loading-placeholder">Carregando...</div>
        ) : docs.length === 0 ? (
          <div className="empty-state">
            <span style={{ fontSize: '2rem' }}>🔒</span>
            <p>Nenhum documento arquivado{categoria ? ` em ${tipoLabel(categoria)}` : ''}</p>
          </div>
        ) : (
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
            gap: 12
          }}>
            {docs.map(doc => (
              <div key={doc.id} style={{
                border: '1px solid var(--border)',
                borderRadius: 8,
                padding: 14,
                background: 'var(--card-bg)',
                display: 'flex', flexDirection: 'column', gap: 8
              }}>
                {/* Header */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span style={{ fontWeight: 600 }}>
                    {tipoLabel(doc.tipo)} #{doc.id_encomenda || doc.id_frete || doc.id}
                  </span>
                  <span className={`badge ${doc.tipo === 'encomenda' || doc.tipo === 'lote' ? 'info' : 'warning'}`}>
                    {tipoLabel(doc.tipo)}
                  </span>
                </div>

                {/* Remetente */}
                <div style={{ fontSize: '0.9rem' }}>
                  <strong>Remetente:</strong> {doc.remetente || '—'}
                </div>
                <div style={{ fontSize: '0.9rem' }}>
                  <strong>Destinatario:</strong> {doc.destinatario || '—'}
                </div>

                {/* Documento */}
                <div style={{
                  background: '#d4edda', borderRadius: 6, padding: '8px 10px',
                  fontSize: '0.85rem'
                }}>
                  <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
                    {doc.doc_remetente.tipo_doc && (
                      <span><strong>Tipo:</strong> {doc.doc_remetente.tipo_doc}</span>
                    )}
                    {doc.doc_remetente.cpf && (
                      <span><strong>CPF:</strong> {doc.doc_remetente.cpf}</span>
                    )}
                    {doc.doc_remetente.rg && (
                      <span><strong>RG:</strong> {doc.doc_remetente.rg}</span>
                    )}
                  </div>
                </div>

                {/* Footer */}
                <div style={{
                  display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                  fontSize: '0.8rem', color: '#888'
                }}>
                  <span>{doc.operador || '—'} · {fmtDate(doc.criado_em)}</span>
                  {doc.doc_remetente.foto_doc_path && (
                    <button className="btn btn-sm" onClick={() => verFoto(doc)}>
                      Ver Documento
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Modal foto — fullscreen */}
      {fotoUrl && (
        <div onClick={fecharFoto} style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0,0,0,0.9)', display: 'flex', flexDirection: 'column',
          alignItems: 'center', justifyContent: 'center', zIndex: 9999,
          cursor: 'pointer', padding: 20
        }}>
          {fotoInfo && (
            <div style={{
              color: '#fff', marginBottom: 12, textAlign: 'center',
              fontSize: '0.9rem', opacity: 0.8
            }}>
              {fotoInfo.remetente} — {fotoInfo.doc_remetente?.tipo_doc} {fotoInfo.doc_remetente?.cpf || fotoInfo.doc_remetente?.rg || ''}
            </div>
          )}
          <img src={fotoUrl} alt="Documento" style={{
            maxWidth: '90vw', maxHeight: '80vh', borderRadius: 8,
            boxShadow: '0 4px 20px rgba(0,0,0,0.5)'
          }} />
          <div style={{ color: '#fff', marginTop: 12, fontSize: '0.8rem', opacity: 0.5 }}>
            Clique para fechar
          </div>
        </div>
      )}
    </div>
  )
}

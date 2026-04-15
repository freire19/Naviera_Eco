import { useState, useEffect, useCallback, useRef } from 'react'
import { api } from '../api.js'

function fmtDate(d) {
  return d ? new Date(d).toLocaleDateString('pt-BR') : '—'
}

const CATEGORIAS = [
  { key: '', label: 'Todos', icon: '📁' },
  { key: 'passageiro', label: 'Passageiros', icon: '🎫' },
  { key: 'encomenda', label: 'Encomendas', icon: '📦' },
  { key: 'frete', label: 'Fretes', icon: '🚚' },
  { key: 'empresa', label: 'Empresas', icon: '🏢' }
]

export default function DocumentosAdmin() {
  const [docs, setDocs] = useState([])
  const [ocrDocs, setOcrDocs] = useState([])
  const [loading, setLoading] = useState(true)
  const [categoria, setCategoria] = useState('')
  const [toast, setToast] = useState(null)
  const [fotoUrl, setFotoUrl] = useState(null)
  const [fotoInfo, setFotoInfo] = useState(null)
  const [uploading, setUploading] = useState(false)
  const [showUpload, setShowUpload] = useState(false)
  const [uploadForm, setUploadForm] = useState({ categoria: 'passageiro', referencia_nome: '' })
  const fileRef = useRef(null)

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  const carregar = useCallback(async () => {
    setLoading(true)
    try {
      const params = categoria ? `?categoria=${categoria}` : ''
      // Buscar de ambas as fontes
      const [docsResult, ocrResult] = await Promise.all([
        api.get(`/documentos${params}`).catch(() => []),
        // OCR docs so carrega para encomenda/frete/lote ou todos
        (!categoria || ['encomenda', 'frete', 'lote'].includes(categoria))
          ? api.get(`/ocr/documentos${params}`).catch(() => [])
          : Promise.resolve([])
      ])
      setDocs(Array.isArray(docsResult) ? docsResult : [])
      setOcrDocs(Array.isArray(ocrResult) ? ocrResult : [])
    } catch {
      showToast('Erro ao carregar documentos', 'error')
    } finally {
      setLoading(false)
    }
  }, [categoria])

  useEffect(() => { carregar() }, [carregar])

  // Unifica docs de ambas as fontes
  const todosOsDocs = [
    ...docs.map(d => ({
      id: d.id,
      source: 'docs',
      categoria: d.categoria,
      remetente: d.nome_pessoa || d.referencia_nome || '',
      destinatario: '',
      cpf: d.cpf || '',
      rg: d.rg || '',
      tipo_doc: d.tipo_doc || '',
      tem_foto: !!d.foto_path,
      operador: d.nome_usuario_criou,
      criado_em: d.criado_em,
      ref_id: d.referencia_id,
      ref_nome: d.referencia_nome
    })),
    ...ocrDocs.map(d => ({
      id: d.id,
      source: 'ocr',
      categoria: d.tipo === 'lote' ? 'encomenda' : d.tipo,
      remetente: d.remetente || '',
      destinatario: d.destinatario || '',
      cpf: d.doc_remetente?.cpf || '',
      rg: d.doc_remetente?.rg || '',
      tipo_doc: d.doc_remetente?.tipo_doc || '',
      tem_foto: !!d.doc_remetente?.foto_doc_path,
      operador: d.operador,
      criado_em: d.criado_em,
      ref_id: d.id_encomenda || d.id_frete,
      ref_nome: ''
    }))
  ].sort((a, b) => new Date(b.criado_em) - new Date(a.criado_em))

  const verFoto = async (doc) => {
    try {
      const token = localStorage.getItem('naviera_token')
      const url = doc.source === 'ocr'
        ? `/api/ocr/lancamentos/${doc.id}/doc-foto`
        : `/api/documentos/${doc.id}/foto`
      const res = await fetch(url, { headers: { Authorization: `Bearer ${token}` } })
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

  const handleUpload = async () => {
    const file = fileRef.current?.files?.[0]
    if (!file) { showToast('Selecione uma foto', 'error'); return }
    if (!uploadForm.referencia_nome.trim()) { showToast('Informe o nome', 'error'); return }

    setUploading(true)
    try {
      const form = new FormData()
      form.append('foto', file)
      form.append('categoria', uploadForm.categoria)
      form.append('referencia_nome', uploadForm.referencia_nome.trim())

      const token = localStorage.getItem('naviera_token')
      const res = await fetch('/api/documentos/upload', {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        body: form
      })
      if (!res.ok) {
        const err = await res.json().catch(() => ({}))
        throw new Error(err.error || 'Erro no upload')
      }
      const result = await res.json()
      showToast(`Documento arquivado — ${result.tipo_doc || 'ID'}: ${result.nome_pessoa || uploadForm.referencia_nome}`)
      setShowUpload(false)
      setUploadForm({ categoria: 'passageiro', referencia_nome: '' })
      fileRef.current.value = ''
      carregar()
    } catch (err) {
      showToast(err.message, 'error')
    } finally {
      setUploading(false)
    }
  }

  const excluir = async (doc) => {
    if (doc.source === 'ocr') { showToast('Documentos OCR nao podem ser excluidos aqui', 'error'); return }
    if (!confirm('Excluir este documento?')) return
    try {
      await api.delete(`/documentos/${doc.id}`)
      showToast('Documento excluido')
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro', 'error')
    }
  }

  const catLabel = (cat) => CATEGORIAS.find(c => c.key === cat)?.label || cat

  return (
    <div>
      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}

      <div className="card">
        <div className="card-header">
          <h3 style={{ margin: 0 }}>Documentos Arquivados</h3>
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn btn-sm btn-primary" onClick={() => setShowUpload(!showUpload)}>
              {showUpload ? 'Cancelar' : '+ Novo Documento'}
            </button>
            <button className="btn btn-sm" onClick={carregar} disabled={loading}>Atualizar</button>
          </div>
        </div>

        {/* Upload form */}
        {showUpload && (
          <div style={{
            padding: 16, margin: '12px 0', borderRadius: 8,
            border: '1px solid var(--border)', background: 'var(--card-bg)'
          }}>
            <h4 style={{ margin: '0 0 12px' }}>Arquivar Documento</h4>
            <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', alignItems: 'flex-end' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', marginBottom: 4 }}>Categoria</label>
                <select
                  value={uploadForm.categoria}
                  onChange={e => setUploadForm(f => ({ ...f, categoria: e.target.value }))}
                  style={{ padding: 8, borderRadius: 6, border: '1px solid #ddd' }}
                >
                  <option value="passageiro">Passageiro</option>
                  <option value="encomenda">Encomenda</option>
                  <option value="frete">Frete</option>
                  <option value="empresa">Empresa</option>
                </select>
              </div>
              <div style={{ flex: 1, minWidth: 200 }}>
                <label style={{ display: 'block', fontSize: '0.85rem', marginBottom: 4 }}>Nome da pessoa</label>
                <input
                  type="text"
                  placeholder="Nome completo..."
                  value={uploadForm.referencia_nome}
                  onChange={e => setUploadForm(f => ({ ...f, referencia_nome: e.target.value }))}
                  style={{ width: '100%', padding: 8, borderRadius: 6, border: '1px solid #ddd' }}
                />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', marginBottom: 4 }}>Foto do documento</label>
                <input ref={fileRef} type="file" accept="image/*" style={{ fontSize: '0.85rem' }} />
              </div>
              <button
                className="btn btn-sm btn-primary"
                onClick={handleUpload}
                disabled={uploading}
              >
                {uploading ? 'Processando...' : 'Arquivar'}
              </button>
            </div>
            <p style={{ fontSize: '0.8rem', color: '#888', margin: '8px 0 0' }}>
              A IA vai extrair automaticamente nome, CPF e RG da foto do documento.
            </p>
          </div>
        )}

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

        {!loading && (
          <div style={{ fontSize: '0.85rem', color: '#666', marginBottom: 12 }}>
            {todosOsDocs.length} documento(s)
          </div>
        )}

        {/* Grid */}
        {loading ? (
          <div className="loading-placeholder">Carregando...</div>
        ) : todosOsDocs.length === 0 ? (
          <div className="empty-state">
            <span style={{ fontSize: '2rem' }}>🔒</span>
            <p>Nenhum documento{categoria ? ` em ${catLabel(categoria)}` : ''}</p>
          </div>
        ) : (
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
            gap: 12
          }}>
            {todosOsDocs.map(doc => (
              <div key={`${doc.source}-${doc.id}`} style={{
                border: '1px solid var(--border)',
                borderRadius: 8, padding: 14,
                background: 'var(--card-bg)',
                display: 'flex', flexDirection: 'column', gap: 8
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span style={{ fontWeight: 600 }}>{doc.remetente || '—'}</span>
                  <span className={`badge ${doc.categoria === 'passageiro' ? 'info' : doc.categoria === 'encomenda' ? 'success' : 'warning'}`}>
                    {catLabel(doc.categoria)}
                  </span>
                </div>

                {doc.destinatario && (
                  <div style={{ fontSize: '0.85rem', color: '#888' }}>
                    → {doc.destinatario}
                  </div>
                )}

                <div style={{
                  background: '#d4edda', borderRadius: 6, padding: '6px 10px',
                  fontSize: '0.85rem', display: 'flex', gap: 12, flexWrap: 'wrap'
                }}>
                  {doc.tipo_doc && <span><strong>Tipo:</strong> {doc.tipo_doc}</span>}
                  {doc.cpf && <span><strong>CPF:</strong> {doc.cpf}</span>}
                  {doc.rg && <span><strong>RG:</strong> {doc.rg}</span>}
                  {!doc.tipo_doc && !doc.cpf && !doc.rg && <span style={{ color: '#888' }}>Sem dados extraidos</span>}
                </div>

                <div style={{
                  display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                  fontSize: '0.8rem', color: '#888'
                }}>
                  <span>{doc.operador || '—'} · {fmtDate(doc.criado_em)}</span>
                  <div style={{ display: 'flex', gap: 6 }}>
                    {doc.tem_foto && (
                      <button className="btn btn-sm" onClick={() => verFoto(doc)}>Ver Doc</button>
                    )}
                    {doc.source === 'docs' && (
                      <button className="btn btn-sm btn-danger" onClick={() => excluir(doc)} style={{ padding: '2px 8px' }}>×</button>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Modal foto */}
      {fotoUrl && (
        <div onClick={fecharFoto} style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0,0,0,0.9)', display: 'flex', flexDirection: 'column',
          alignItems: 'center', justifyContent: 'center', zIndex: 9999,
          cursor: 'pointer', padding: 20
        }}>
          {fotoInfo && (
            <div style={{ color: '#fff', marginBottom: 12, textAlign: 'center', fontSize: '0.9rem', opacity: 0.8 }}>
              {fotoInfo.remetente} — {fotoInfo.tipo_doc} {fotoInfo.cpf || fotoInfo.rg || ''}
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

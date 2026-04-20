export const API = import.meta.env.VITE_API_URL || '/api'

const TOKEN_KEY = 'naviera_ocr_token'
const USER_KEY = 'naviera_ocr_usuario'

function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  window.location.reload()
}

/* ═══ Core request function (unified pattern — mirrors naviera-web/naviera-app) ═══ */
async function request(path, options = {}) {
  const token = getToken()
  const headers = { 'Content-Type': 'application/json', ...options.headers }
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(`${API}${path}`, { ...options, headers })

  if (res.status === 401 || res.status === 403) {
    clearSession()
    return null
  }

  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    const err = new Error(body.error || `Erro ${res.status}`)
    err.status = res.status
    Object.assign(err, body)
    throw err
  }

  return res.json()
}

/* ═══ api object (unified pattern — same interface as naviera-web/naviera-app) ═══ */
export const api = {
  get: (path) => request(path),
  post: (path, data) => request(path, { method: 'POST', body: JSON.stringify(data) }),
  put: (path, data) => request(path, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (path) => request(path, { method: 'DELETE' })
}

/* ═══ Backward-compatible aliases (match existing import names across codebase) ═══ */
export const apiGet = api.get
export const apiPost = api.post
export const apiPut = api.put

/* ═══ Domain-specific: OCR photo upload (single ou multiplas fotos) ═══ */
export async function uploadFoto(fileOrFiles, viagemId, tipo, clientUuid, extra = {}) {
  const form = new FormData()
  // Suporte a multiplas fotos: Array<File> ou File unico
  const files = Array.isArray(fileOrFiles) ? fileOrFiles : [fileOrFiles]
  if (files.length === 1) {
    form.append('foto', files[0])
  } else {
    for (const f of files) form.append('fotos', f)
  }
  if (viagemId) form.append('viagem_id', viagemId)
  if (tipo) form.append('tipo', tipo)
  if (extra.num_notafiscal) form.append('num_notafiscal', extra.num_notafiscal)
  // Idempotencia: UUID gerado no cliente para evitar duplicatas em retry/refresh
  form.append('client_uuid', clientUuid || crypto.randomUUID())

  const token = getToken()
  const res = await fetch(`${API}/ocr/upload`, {
    method: 'POST',
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    body: form
  })
  if (res.status === 401 || res.status === 403) { clearSession(); return null }
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.error || `Erro ${res.status}`)
  }
  return res.json()
}

/* ═══ Domain-specific: upload additional photo to existing lancamento ═══ */
export async function uploadFotoAdicional(lancamentoId, file) {
  const form = new FormData()
  form.append('foto', file)

  const token = getToken()
  const res = await fetch(`${API}/ocr/lancamentos/${lancamentoId}/adicionar-foto`, {
    method: 'POST',
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    body: form
  })
  if (res.status === 401 || res.status === 403) { clearSession(); return null }
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.error || `Erro ${res.status}`)
  }
  return res.json()
}

/* ═══ Domain-specific: fetch photo via Authorization header ═══ */
// #DB151: fetch foto via Authorization header instead of exposing JWT in URL
export async function fetchFoto(lancamentoId) {
  const token = getToken()
  const res = await fetch(`${API}/ocr/lancamentos/${lancamentoId}/foto`, {
    headers: { 'Authorization': `Bearer ${token}` }
  })
  if (!res.ok) return null
  const blob = await res.blob()
  return URL.createObjectURL(blob)
}

const BASE = import.meta.env.VITE_API_URL || '/api'

const TOKEN_KEY = 'naviera_token'
const USER_KEY = 'naviera_usuario'

function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  window.location.reload()
}

/* ═══ Core request function (unified pattern — mirrors naviera-app/naviera-ocr) ═══ */
async function request(path, options = {}) {
  const token = getToken()
  const headers = { 'Content-Type': 'application/json', ...options.headers }
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(`${BASE}${path}`, { ...options, headers })

  if (res.status === 401) {
    clearSession()
    return
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

/* ═══ api object (unified pattern — same interface as naviera-app/naviera-ocr) ═══ */
export const api = {
  get: (path) => request(path),
  post: (path, data) => request(path, { method: 'POST', body: JSON.stringify(data) }),
  put: (path, data) => request(path, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (path, data) => request(path, { method: 'DELETE', ...(data ? { body: JSON.stringify(data) } : {}) })
}

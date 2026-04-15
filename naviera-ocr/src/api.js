export const API = import.meta.env.VITE_API_URL || '/api'

function authHeaders() {
  const token = localStorage.getItem('naviera_ocr_token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

function handle401(res) {
  if (res.status === 401 || res.status === 403) {
    localStorage.removeItem('naviera_ocr_token')
    localStorage.removeItem('naviera_ocr_usuario')
    window.location.reload()
    return true
  }
  return false
}

export async function apiGet(path) {
  const res = await fetch(`${API}${path}`, { headers: authHeaders() })
  if (handle401(res)) return null
  if (!res.ok) throw new Error(`Erro ${res.status}`)
  return res.json()
}

export async function apiPut(path, data) {
  const res = await fetch(`${API}${path}`, {
    method: 'PUT',
    headers: { ...authHeaders(), 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  })
  if (handle401(res)) return null
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.error || `Erro ${res.status}`)
  }
  return res.json()
}

export async function apiPost(path, data) {
  const res = await fetch(`${API}${path}`, {
    method: 'POST',
    headers: { ...authHeaders(), 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  })
  if (handle401(res)) return null
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.error || `Erro ${res.status}`)
  }
  return res.json()
}

export async function uploadFoto(file, viagemId, tipo) {
  const form = new FormData()
  form.append('foto', file)
  if (viagemId) form.append('viagem_id', viagemId)
  if (tipo) form.append('tipo', tipo)

  const res = await fetch(`${API}/ocr/upload`, {
    method: 'POST',
    headers: authHeaders(),
    body: form
  })
  if (handle401(res)) return null
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.error || `Erro ${res.status}`)
  }
  return res.json()
}

// #DB151: fetch foto via Authorization header instead of exposing JWT in URL
export async function fetchFoto(lancamentoId) {
  const token = localStorage.getItem('naviera_ocr_token')
  const res = await fetch(`${API}/ocr/lancamentos/${lancamentoId}/foto`, {
    headers: { 'Authorization': `Bearer ${token}` }
  })
  if (!res.ok) return null
  const blob = await res.blob()
  return URL.createObjectURL(blob)
}

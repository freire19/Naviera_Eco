import { useState, useEffect, useCallback } from "react";

export const API = import.meta.env.VITE_API_URL || "http://localhost:8081/api";

const TOKEN_KEY = 'naviera_token'
const USER_KEY = 'naviera_usuario'

function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem('naviera_app_token') // legacy key cleanup
  window.location.reload()
}

/* ═══ Core request function (unified pattern — mirrors naviera-web/naviera-ocr) ═══ */
async function request(path, options = {}) {
  const token = getToken()
  const headers = { 'Content-Type': 'application/json', ...options.headers }
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(`${API}${path}`, { ...options, headers })

  if (res.status === 401 || res.status === 403) {
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

/* ═══ api object (unified pattern — same interface as naviera-web/naviera-ocr) ═══ */
export const api = {
  get: (path) => request(path),
  post: (path, data) => request(path, { method: 'POST', body: JSON.stringify(data) }),
  put: (path, data) => request(path, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (path) => request(path, { method: 'DELETE' })
}

/* ═══ authFetch: backward-compatible wrapper (delegates to unified 401 handling) ═══ */
export function authFetch(url, options = {}) {
  return fetch(url, options).then(res => {
    if (res.status === 401 || res.status === 403) {
      clearSession()
    }
    return res
  })
}

/* ═══ HOOK: useApi com refresh ═══ */
export function useApi(path, authHeaders, deps = []) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState("");
  const [rev, setRev] = useState(0);

  const refresh = useCallback(() => setRev(r => r + 1), []);

  useEffect(() => {
    if (!authHeaders?.Authorization) return;
    const controller = new AbortController();
    const signal = AbortSignal.any
      ? AbortSignal.any([controller.signal, AbortSignal.timeout(15000)])
      : controller.signal;
    setLoading(true); setErro("");
    fetch(`${API}${path}`, { headers: authHeaders, signal })
      .then(r => {
        if (r.status === 401 || r.status === 403) {
          clearSession()
          return Promise.reject("Sessao expirada");
        }
        return r.ok ? r.json() : Promise.reject("Erro ao carregar");
      })
      .then(d => setData(d))
      .catch((e) => {
        if (e.name === 'AbortError') return;
        setErro(typeof e === "string" ? e : "Erro ao carregar dados.");
      })
      .finally(() => setLoading(false));
    return () => controller.abort();
  }, [path, authHeaders?.Authorization, rev, ...deps]);

  return { data, loading, erro, refresh };
}

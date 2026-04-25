import { useState, useEffect, useCallback } from "react";

export const API = import.meta.env.VITE_API_URL || "http://localhost:8081/api";

const TOKEN_KEY = 'naviera_token'
const USER_KEY = 'naviera_usuario'

// #DS5-209: token e dados de usuario migrados de localStorage para sessionStorage —
//   sessionStorage e isolado por aba e some no fechamento, reduzindo janela de roubo via XSS.
//   Trade-off aceito: usuario refaz login se fechar a aba/navegador.
function getToken() {
  return sessionStorage.getItem(TOKEN_KEY)
}

function clearSession() {
  sessionStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(USER_KEY)
  // Cleanup de chaves legadas (versoes anteriores guardavam em localStorage).
  try {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    localStorage.removeItem('naviera_app_token')
  } catch { /* sandbox sem localStorage — ignorar */ }
  window.location.reload()
}

/* ═══ Core request function (unified pattern — mirrors naviera-web/naviera-ocr) ═══ */
async function request(path, options = {}) {
  const token = getToken()
  const headers = { 'Content-Type': 'application/json', ...options.headers }
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(`${API}${path}`, { ...options, headers })

  // #DS5-225: 403 = autorizacao recusada (ACL pontual), nao expirada — NAO derruba sessao.
  //   Apenas 401 (token invalido/expirado) deve forcar logout.
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
    // #DS5-225: so 401 derruba sessao; 403 e ACL especifica.
    if (res.status === 401) {
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
    // #DR280: guarda contra setState apos unmount — fetch pode resolver depois do componente sair.
    let active = true;
    const controller = new AbortController();
    const signal = AbortSignal.any
      ? AbortSignal.any([controller.signal, AbortSignal.timeout(15000)])
      : controller.signal;
    setLoading(true); setErro("");
    fetch(`${API}${path}`, { headers: authHeaders, signal })
      .then(r => {
        // #DS5-225: 403 nao expira sessao — usuario pode ter perdido permissao especifica.
        if (r.status === 401) {
          clearSession()
          return Promise.reject("Sessao expirada");
        }
        return r.ok ? r.json() : Promise.reject("Erro ao carregar");
      })
      .then(d => { if (active) setData(d); })
      .catch((e) => {
        if (e.name === 'AbortError' || !active) return;
        setErro(typeof e === "string" ? e : "Erro ao carregar dados.");
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; controller.abort(); };
  }, [path, authHeaders?.Authorization, rev, ...deps]);

  return { data, loading, erro, refresh };
}

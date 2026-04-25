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

// #DR286: schema mismatch apos refactor de campos virava bug silencioso. Logout limpo
//   e melhor que estado indefinido — chamadores sao responsaveis por redirecionar pra login.
export function lerUsuarioValido() {
  try {
    const u = JSON.parse(sessionStorage.getItem(USER_KEY))
    if (!u || typeof u !== 'object') return null
    if (u.tipo !== 'CPF' && u.tipo !== 'CNPJ') return null
    if (typeof u.nome !== 'string') return null
    return u
  } catch { return null }
}

/* ═══ authFetch: cliente HTTP unico (com handling de 401) ═══ */
// #DS5-225: so 401 derruba sessao; 403 e ACL especifica.
export function authFetch(url, options = {}) {
  return fetch(url, options).then(res => {
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
    authFetch(`${API}${path}`, { headers: authHeaders, signal })
      .then(r => r.ok ? r.json() : Promise.reject("Erro ao carregar"))
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

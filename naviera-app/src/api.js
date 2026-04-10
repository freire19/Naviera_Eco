import { useState, useEffect, useCallback } from "react";

export const API = import.meta.env.VITE_API_URL || "http://localhost:8081/api";

/* ═══ HOOK: useApi com refresh ═══ */
export function useApi(path, authHeaders, deps = []) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState("");
  const [rev, setRev] = useState(0);

  const refresh = useCallback(() => setRev(r => r + 1), []);

  useEffect(() => {
    if (!authHeaders?.Authorization) return;
    setLoading(true); setErro("");
    fetch(`${API}${path}`, { headers: authHeaders })
      .then(r => {
        if (r.status === 401 || r.status === 403) {
          localStorage.removeItem("naviera_token"); localStorage.removeItem("naviera_usuario");
          window.location.reload();
          return Promise.reject("Sess\u00e3o expirada");
        }
        return r.ok ? r.json() : Promise.reject("Erro ao carregar");
      })
      .then(d => setData(d))
      .catch((e) => setErro(typeof e === "string" ? e : "Erro ao carregar dados."))
      .finally(() => setLoading(false));
  }, [path, authHeaders?.Authorization, rev, ...deps]);

  return { data, loading, erro, refresh };
}

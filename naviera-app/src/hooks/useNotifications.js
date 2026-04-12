import { useState, useEffect, useCallback, useRef } from "react";

/**
 * useNotifications — push notifications com FCM (quando configurado) ou fallback browser.
 *
 * Firebase e inicializado SOMENTE se existir config em:
 *   - window.__FIREBASE_CONFIG__   (runtime injection)
 *   - import.meta.env.VITE_FIREBASE_*  (build-time)
 *
 * Sem config -> usa apenas Notification API nativa (graceful degradation).
 */

/* ── helpers para detectar config ── */
function getFirebaseConfig() {
  // runtime override
  if (window.__FIREBASE_CONFIG__) return window.__FIREBASE_CONFIG__;

  const apiKey = import.meta.env.VITE_FIREBASE_API_KEY;
  if (!apiKey) return null;

  return {
    apiKey,
    authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || "",
    projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID || "",
    messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID || "",
    appId: import.meta.env.VITE_FIREBASE_APP_ID || "",
  };
}

/* ── lazy-load Firebase SDK (so nao pesa se nao usa) ── */
let _firebaseApp = null;
let _messaging = null;

async function initFirebase(config) {
  if (_messaging) return _messaging;
  try {
    const { initializeApp, getApps } = await import("firebase/app");
    const { getMessaging, getToken, onMessage } = await import("firebase/messaging");

    _firebaseApp = getApps().length === 0 ? initializeApp(config) : getApps()[0];
    _messaging = getMessaging(_firebaseApp);
    return _messaging;
  } catch (e) {
    console.warn("[Notificacoes] Erro ao inicializar Firebase:", e);
    return null;
  }
}

/* ══════════════════════════════════════ */
export default function useNotifications() {
  const suportado = "Notification" in window;
  const [permissao, setPermissao] = useState(suportado ? Notification.permission : "denied");
  const [tokenFcm, setTokenFcm] = useState(null);
  const [firebaseAtivo, setFirebaseAtivo] = useState(false);
  const [notificacao, setNotificacao] = useState(null); // { titulo, corpo } — foreground toast
  const unsubRef = useRef(null);

  /* ── permissao ── */
  const solicitarPermissao = useCallback(async () => {
    if (!suportado) return "denied";
    const result = await Notification.requestPermission();
    setPermissao(result);
    return result;
  }, [suportado]);

  /* ── registrar SW e obter token FCM ── */
  const obterTokenFcm = useCallback(async () => {
    const config = getFirebaseConfig();
    if (!config) return null;

    try {
      const messaging = await initFirebase(config);
      if (!messaging) return null;

      // Registra o service worker dedicado ao FCM
      const swReg = await navigator.serviceWorker.register("/firebase-messaging-sw.js");

      const { getToken } = await import("firebase/messaging");
      const vapidKey = import.meta.env.VITE_FIREBASE_VAPID_KEY || config.vapidKey || undefined;
      const token = await getToken(messaging, {
        serviceWorkerRegistration: swReg,
        ...(vapidKey ? { vapidKey } : {}),
      });

      if (token) {
        setTokenFcm(token);
        setFirebaseAtivo(true);
      }
      return token;
    } catch (e) {
      console.warn("[Notificacoes] Erro ao obter token FCM:", e);
      return null;
    }
  }, []);

  /* ── escutar mensagens em foreground ── */
  useEffect(() => {
    if (!firebaseAtivo || !_messaging) return;

    let cancelled = false;
    (async () => {
      const { onMessage } = await import("firebase/messaging");
      if (cancelled) return;
      unsubRef.current = onMessage(_messaging, (payload) => {
        const n = payload.notification || {};
        setNotificacao({ titulo: n.title || "Naviera", corpo: n.body || "" });
      });
    })();

    return () => {
      cancelled = true;
      unsubRef.current?.();
    };
  }, [firebaseAtivo]);

  /* ── notificacao local (fallback / uso geral) ── */
  const enviarNotificacaoLocal = useCallback((titulo, corpo) => {
    if (!suportado || permissao !== "granted") return;

    // Se app em foreground, tambem dispara toast interno
    setNotificacao({ titulo, corpo });

    try {
      new Notification(titulo, {
        body: corpo,
        icon: "/icons/icon-192.png",
        badge: "/icons/icon-192.png",
      });
    } catch {
      // Alguns browsers mobile bloqueiam new Notification() — usa SW
      navigator.serviceWorker?.ready?.then((reg) => {
        reg.showNotification(titulo, { body: corpo, icon: "/icons/icon-192.png" });
      });
    }
  }, [suportado, permissao]);

  /* ── limpar toast ── */
  const limparNotificacao = useCallback(() => setNotificacao(null), []);

  return {
    suportado,
    permissao,
    tokenFcm,
    firebaseAtivo,
    notificacao,
    solicitarPermissao,
    obterTokenFcm,
    enviarNotificacaoLocal,
    limparNotificacao,
  };
}

/* ═══ Firebase Cloud Messaging — Service Worker (background) ═══ */

/* eslint-disable no-restricted-globals */

/*
 * Este SW lida APENAS com push notifications em background.
 * O sw.js principal cuida de cache / offline.
 *
 * Firebase config e injetada via query-string ou mensagem do client.
 * Se nenhum config for fornecido, o SW simplesmente nao faz nada.
 */

importScripts("https://www.gstatic.com/firebasejs/10.12.0/firebase-app-compat.js");
importScripts("https://www.gstatic.com/firebasejs/10.12.0/firebase-messaging-compat.js");

/* ── Config recebida do client ── */
let firebaseInitialized = false;

self.addEventListener("message", (event) => {
  if (event.data && event.data.type === "FIREBASE_CONFIG") {
    if (!firebaseInitialized && event.data.config) {
      firebase.initializeApp(event.data.config);
      firebaseInitialized = true;
      setupMessaging();
    }
  }
});

/* ── Tenta inicializar via env vars embutidas no build (fallback) ── */
/* Note: SWs nao tem import.meta.env, entao usamos o mecanismo de mensagem acima.
   Porem, se o app injetar __FIREBASE_CONFIG__ globalmente antes do registro, podemos
   captura-lo via fetch event self.registration. Na pratica o client envia via postMessage. */

function setupMessaging() {
  try {
    const messaging = firebase.messaging();

    messaging.onBackgroundMessage((payload) => {
      const n = payload.notification || {};
      const title = n.title || "Naviera";
      const options = {
        body: n.body || "",
        icon: "/icons/icon-192.png",
        badge: "/icons/icon-192.png",
        data: payload.data || {},
        tag: "naviera-push",
        renotify: true,
      };
      self.registration.showNotification(title, options);
    });
  } catch (e) {
    console.warn("[firebase-messaging-sw] Erro ao configurar messaging:", e);
  }
}

/* ── Click handler — abre o app ── */
self.addEventListener("notificationclick", (event) => {
  event.notification.close();

  const urlToOpen = event.notification.data?.url || "/";

  event.waitUntil(
    self.clients.matchAll({ type: "window", includeUncontrolled: true }).then((clientList) => {
      // Se ja tem uma aba aberta, foca nela
      for (const client of clientList) {
        if (client.url.includes(self.location.origin) && "focus" in client) {
          return client.focus();
        }
      }
      // Senao, abre nova aba
      return self.clients.openWindow(urlToOpen);
    })
  );
});

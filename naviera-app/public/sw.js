/* ═══ Naviera Service Worker ═══ */
/*
 * NOTA: Push notifications (Firebase Cloud Messaging) sao tratadas
 * por um SW separado: /firebase-messaging-sw.js
 * Este SW cuida apenas de cache e offline.
 */
const CACHE_VERSION = 'naviera-v1';
const STATIC_CACHE = `${CACHE_VERSION}-static`;
const API_CACHE = `${CACHE_VERSION}-api`;

const STATIC_EXTENSIONS = ['.js', '.css', '.png', '.jpg', '.jpeg', '.svg', '.webp', '.woff', '.woff2', '.ttf'];

const OFFLINE_PAGE = '/offline.html';

/* ── Install ── */
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(STATIC_CACHE).then((cache) =>
      cache.addAll([
        '/',
        '/index.html',
        '/manifest.json',
        '/offline.html',
      ])
    )
  );
  self.skipWaiting();
});

/* ── Activate — clean old caches ── */
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(
        keys
          .filter((key) => key !== STATIC_CACHE && key !== API_CACHE)
          .map((key) => caches.delete(key))
      )
    )
  );
  self.clients.claim();
});

/* ── Fetch ── */
self.addEventListener('fetch', (event) => {
  const { request } = event;
  const url = new URL(request.url);

  // Skip non-GET requests
  if (request.method !== 'GET') return;

  // Skip chrome-extension and other non-http(s) schemes
  if (!url.protocol.startsWith('http')) return;

  // API calls → network-first
  if (url.pathname.startsWith('/api') || url.hostname !== self.location.hostname) {
    event.respondWith(networkFirst(request));
    return;
  }

  // Static assets → cache-first
  if (STATIC_EXTENSIONS.some((ext) => url.pathname.endsWith(ext))) {
    event.respondWith(cacheFirst(request));
    return;
  }

  // Navigation requests → network-first with offline fallback
  if (request.mode === 'navigate') {
    event.respondWith(
      fetch(request)
        .then((response) => {
          const clone = response.clone();
          caches.open(STATIC_CACHE).then((cache) => cache.put(request, clone));
          return response;
        })
        .catch(() => caches.match(OFFLINE_PAGE) || caches.match('/index.html'))
    );
    return;
  }

  // Default → network with cache fallback
  event.respondWith(networkFirst(request));
});

/* ── Strategies ── */
function cacheFirst(request) {
  return caches.match(request).then((cached) => {
    if (cached) return cached;
    return fetch(request).then((response) => {
      if (response.ok) {
        const clone = response.clone();
        caches.open(STATIC_CACHE).then((cache) => cache.put(request, clone));
      }
      return response;
    });
  });
}

function networkFirst(request) {
  return fetch(request)
    .then((response) => {
      if (response.ok) {
        const clone = response.clone();
        caches.open(API_CACHE).then((cache) => cache.put(request, clone));
      }
      return response;
    })
    .catch(() => caches.match(request));
}

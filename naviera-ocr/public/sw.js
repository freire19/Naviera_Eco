const CACHE_VERSION = 2
const STATIC_CACHE = `naviera-ocr-static-v${CACHE_VERSION}`
const API_CACHE = `naviera-ocr-api-v${CACHE_VERSION}`

const STATIC_URLS = ['/', '/index.html', '/manifest.json']

self.addEventListener('install', (e) => {
  e.waitUntil(
    caches.open(STATIC_CACHE).then(c => c.addAll(STATIC_URLS)).then(() => self.skipWaiting())
  )
})

self.addEventListener('activate', (e) => {
  e.waitUntil(
    caches.keys().then(keys =>
      Promise.all(keys.filter(k => k !== STATIC_CACHE && k !== API_CACHE).map(k => caches.delete(k)))
    ).then(() => self.clients.claim())
  )
})

self.addEventListener('fetch', (e) => {
  const { request } = e
  if (request.method !== 'GET') return
  if (!request.url.startsWith('http')) return

  if (request.url.includes('/api/')) {
    // Network-first para API
    e.respondWith(
      fetch(request).then(res => {
        const clone = res.clone()
        caches.open(API_CACHE).then(c => c.put(request, clone))
        return res
      }).catch(() => caches.match(request))
    )
  } else {
    // Network-first para tudo (garante atualizacoes), fallback cache para offline
    e.respondWith(
      fetch(request).then(res => {
        const clone = res.clone()
        caches.open(STATIC_CACHE).then(c => c.put(request, clone))
        return res
      }).catch(() => caches.match(request).then(cached => cached || caches.match('/index.html')))
    )
  }
})

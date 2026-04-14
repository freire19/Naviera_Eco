const STATIC_CACHE = 'naviera-ocr-static-v1'
const API_CACHE = 'naviera-ocr-api-v1'

const STATIC_URLS = ['/', '/index.html', '/offline.html', '/manifest.json']

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
    // Cache-first para assets estaticos
    e.respondWith(
      caches.match(request).then(cached => cached || fetch(request).then(res => {
        const clone = res.clone()
        caches.open(STATIC_CACHE).then(c => c.put(request, clone))
        return res
      })).catch(() => caches.match('/offline.html'))
    )
  }
})

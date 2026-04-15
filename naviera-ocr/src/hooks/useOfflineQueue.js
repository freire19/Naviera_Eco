import { useState, useEffect, useRef, useCallback } from 'react'
import { getQueue, markSynced, clearSynced, queueCount, addToQueue as dbAdd } from '../db.js'
import { uploadFoto } from '../api.js'

// Backoff: 30s, 1min, 2min, 5min, 5min (cap)
const RETRY_DELAYS = [30000, 60000, 120000, 300000]

export default function useOfflineQueue(onToast) {
  const [isOnline, setIsOnline] = useState(navigator.onLine)
  const [count, setCount] = useState(0)
  const [syncing, setSyncing] = useState(false)
  const syncingRef = useRef(false)
  const retryRef = useRef(null)
  const retryAttemptRef = useRef(0)

  const refreshCount = useCallback(async () => {
    try { setCount(await queueCount()) } catch {}
  }, [])

  const clearRetry = useCallback(() => {
    if (retryRef.current) {
      clearTimeout(retryRef.current)
      retryRef.current = null
    }
  }, [])

  const scheduleRetry = useCallback((hadFailures) => {
    clearRetry()
    if (!hadFailures) {
      retryAttemptRef.current = 0
      return
    }
    const idx = Math.min(retryAttemptRef.current, RETRY_DELAYS.length - 1)
    const delay = RETRY_DELAYS[idx]
    retryAttemptRef.current++
    retryRef.current = setTimeout(() => {
      retryRef.current = null
      syncQueue()
    }, delay)
  }, [])

  const syncQueue = useCallback(async () => {
    if (syncingRef.current || !navigator.onLine) return
    syncingRef.current = true
    setSyncing(true)
    let hadFailures = false
    try {
      const items = await getQueue()
      if (items.length === 0) {
        retryAttemptRef.current = 0
        return
      }
      let synced = 0
      for (const item of items) {
        try {
          const file = new File([item.blob], `offline-${item.id}.jpg`, { type: 'image/jpeg' })
          await uploadFoto(file, item.viagem_id, item.tipo, item.client_uuid)
          await markSynced(item.id)
          synced++
        } catch {
          hadFailures = true
        }
      }
      await clearSynced()
      await refreshCount()
      if (synced > 0 && onToast) {
        onToast(`${synced} foto(s) enviada(s) da fila offline`, 'success')
      }
    } finally {
      syncingRef.current = false
      setSyncing(false)
      scheduleRetry(hadFailures)
    }
  }, [onToast, refreshCount, scheduleRetry])

  useEffect(() => {
    refreshCount()
    const goOnline = () => {
      setIsOnline(true)
      retryAttemptRef.current = 0
      syncQueue()
    }
    const goOffline = () => {
      setIsOnline(false)
      clearRetry()
    }
    window.addEventListener('online', goOnline)
    window.addEventListener('offline', goOffline)

    // Tentar sync no mount caso tenha itens pendentes e esteja online
    if (navigator.onLine) syncQueue()

    return () => {
      window.removeEventListener('online', goOnline)
      window.removeEventListener('offline', goOffline)
      clearRetry()
    }
  }, [])

  const addOffline = useCallback(async (blob, viagemId, empresaId, tipo) => {
    await dbAdd(blob, viagemId, empresaId, tipo)
    await refreshCount()
  }, [refreshCount])

  return { isOnline, queueCount: count, syncing, addOffline, syncQueue }
}

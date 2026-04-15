import { useState, useEffect, useRef, useCallback } from 'react'
import { getQueue, markSynced, clearSynced, queueCount, addToQueue as dbAdd } from '../db.js'
import { uploadFoto } from '../api.js'

export default function useOfflineQueue(onToast) {
  const [isOnline, setIsOnline] = useState(navigator.onLine)
  const [count, setCount] = useState(0)
  const [syncing, setSyncing] = useState(false)
  const syncingRef = useRef(false)

  const refreshCount = useCallback(async () => {
    try { setCount(await queueCount()) } catch {}
  }, [])

  useEffect(() => {
    refreshCount()
    const goOnline = () => { setIsOnline(true); syncQueue() }
    const goOffline = () => setIsOnline(false)
    window.addEventListener('online', goOnline)
    window.addEventListener('offline', goOffline)
    return () => {
      window.removeEventListener('online', goOnline)
      window.removeEventListener('offline', goOffline)
    }
  }, [])

  const syncQueue = useCallback(async () => {
    if (syncingRef.current || !navigator.onLine) return
    syncingRef.current = true
    setSyncing(true)
    try {
      const items = await getQueue()
      let synced = 0
      for (const item of items) {
        try {
          const file = new File([item.blob], `offline-${item.id}.jpg`, { type: 'image/jpeg' })
          await uploadFoto(file, item.viagem_id)
          await markSynced(item.id)
          synced++
        } catch {
          // Falhou — mantem na fila para proxima tentativa
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
    }
  }, [onToast, refreshCount])

  const addOffline = useCallback(async (blob, viagemId, empresaId) => {
    await dbAdd(blob, viagemId, empresaId)
    await refreshCount()
  }, [refreshCount])

  return { isOnline, queueCount: count, syncing, addOffline, syncQueue }
}

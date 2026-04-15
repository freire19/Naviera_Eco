import { IconLogout, IconSun, IconMoon, IconWifi, IconWifiOff } from '../icons.jsx'

export default function Header({ t, usuario, isOnline, queueCount, syncing, syncProgress, onLogout, mode, onToggleMode }) {
  return (
    <header style={{
      background: t.card, borderBottom: `1px solid ${t.border}`,
      padding: '12px 16px', display: 'flex', flexDirection: 'column', gap: 0,
      position: 'sticky', top: 0, zIndex: 100
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flex: 1 }}>
          <span style={{ fontSize: '1.2rem' }}>📷</span>
          <span style={{ fontWeight: 700, color: t.tx, fontSize: '1rem' }}>Naviera OCR</span>
        </div>

        {/* Status conexao */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
          {isOnline
            ? <IconWifi size={16} color={t.ok} />
            : <IconWifiOff size={16} color={t.err} />
          }
          {queueCount > 0 && !syncing && (
            <span style={{
              background: t.amberBg, color: t.amber, fontSize: '0.7rem',
              fontWeight: 700, borderRadius: 10, padding: '1px 6px'
            }}>
              {queueCount}
            </span>
          )}
        </div>

        {/* Theme toggle */}
        <button onClick={onToggleMode} style={{
          background: 'none', border: 'none', cursor: 'pointer', padding: 4
        }}>
          {mode === 'dark' ? <IconSun size={18} color={t.txMuted} /> : <IconMoon size={18} color={t.txMuted} />}
        </button>

        {/* Logout */}
        {usuario && (
          <button onClick={onLogout} style={{
            background: 'none', border: 'none', cursor: 'pointer', padding: 4
          }}>
            <IconLogout size={18} color={t.txMuted} />
          </button>
        )}
      </div>

      {/* Barra de progresso do sync */}
      {syncing && syncProgress && (
        <div style={{ marginTop: 8 }}>
          <div style={{
            display: 'flex', justifyContent: 'space-between', alignItems: 'center',
            fontSize: '0.75rem', color: t.txSoft, marginBottom: 4
          }}>
            <span>Enviando fotos offline...</span>
            <span>{syncProgress.current}/{syncProgress.total}{syncProgress.failed > 0 ? ` (${syncProgress.failed} falhou)` : ''}</span>
          </div>
          <div style={{
            height: 4, background: t.soft, borderRadius: 2, overflow: 'hidden'
          }}>
            <div style={{
              height: '100%', borderRadius: 2, transition: 'width 0.3s ease',
              width: `${Math.round((syncProgress.current / syncProgress.total) * 100)}%`,
              background: syncProgress.failed > 0 ? t.amber : t.pri
            }} />
          </div>
        </div>
      )}
    </header>
  )
}

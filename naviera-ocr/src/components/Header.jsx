import { IconLogout, IconSun, IconMoon } from '../icons.jsx'

export default function Header({ t, usuario, onLogout, mode, onToggleMode }) {
  return (
    <header style={{
      background: t.card, borderBottom: `1px solid ${t.border}`,
      padding: '12px 16px', display: 'flex', alignItems: 'center', gap: 12,
      position: 'sticky', top: 0, zIndex: 100
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, flex: 1 }}>
        <span style={{ fontSize: '1.2rem' }}>📷</span>
        <span style={{ fontWeight: 700, color: t.tx, fontSize: '1rem' }}>Naviera OCR</span>
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
    </header>
  )
}

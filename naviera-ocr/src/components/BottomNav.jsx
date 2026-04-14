import { IconCamera, IconClock, IconEdit } from '../icons.jsx'

const TABS = [
  { key: 'captura', icon: IconCamera, label: 'Capturar' },
  { key: 'historico', icon: IconClock, label: 'Historico' },
]

export default function BottomNav({ tab, onTab, t, queueCount }) {
  return (
    <nav style={{
      position: 'fixed', bottom: 0, left: 0, right: 0,
      background: t.card, borderTop: `1px solid ${t.border}`,
      display: 'flex', justifyContent: 'center', zIndex: 100,
      paddingBottom: 'env(safe-area-inset-bottom, 0px)'
    }}>
      <div style={{ display: 'flex', maxWidth: 420, width: '100%' }}>
        {TABS.map(({ key, icon: Icon, label }) => {
          const active = tab === key
          return (
            <button key={key} onClick={() => onTab(key)} style={{
              flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center',
              gap: 2, padding: '10px 0', background: 'none', border: 'none',
              cursor: 'pointer', position: 'relative'
            }}>
              <Icon size={22} color={active ? t.pri : t.txMuted} />
              <span style={{
                fontSize: '0.7rem', fontWeight: active ? 600 : 400,
                color: active ? t.pri : t.txMuted
              }}>
                {label}
              </span>
              {active && (
                <div style={{
                  position: 'absolute', bottom: 4, width: 4, height: 4,
                  borderRadius: '50%', background: t.pri
                }} />
              )}
            </button>
          )
        })}
      </div>
    </nav>
  )
}

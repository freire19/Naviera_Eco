export default function Card({ children, style, onClick, t }) {
  return (
    <div
      onClick={onClick}
      className={onClick ? 'card-interactive' : ''}
      style={{
        background: t.card, borderRadius: 14, padding: 16,
        border: `1px solid ${t.border}`, boxShadow: t.shadow,
        cursor: onClick ? 'pointer' : 'default',
        ...style
      }}
    >
      {children}
    </div>
  )
}

export default function Cd({ children, style, onClick, t, className = "" }) {
  return <div onClick={onClick} className={onClick ? `card-interactive ${className}` : className}
    style={{ background: t.card, borderRadius: 14, padding: 16, border: `1px solid ${t.border}`, cursor: onClick ? "pointer" : "default", boxShadow: t.shadow, ...style }}>
    {children}
  </div>;
}

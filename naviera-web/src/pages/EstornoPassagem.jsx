export default function EstornoPassagem({ viagemAtiva, onNavigate }) {
  return (
    <div className="placeholder-page">
      <div className="ph-icon">{'\u21A9\uFE0F'}</div>
      <h2>Estorno de Passagem</h2>
      <p style={{ marginBottom: '1.5rem', color: 'var(--text-muted)', maxWidth: 480, textAlign: 'center' }}>
        Para estornar uma passagem, acesse a tela de Passagens e utilize o botao de exclusao.
      </p>
      <button className="btn-primary" onClick={() => onNavigate('vender-passagem')}>
        Ir para Passagens
      </button>
    </div>
  )
}

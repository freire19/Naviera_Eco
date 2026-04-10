export default function TopBar({ label, viagens, viagemAtiva, onViagemChange }) {
  function handleChange(e) {
    const id = parseInt(e.target.value)
    const v = viagens.find(v => v.id_viagem === id)
    if (v) onViagemChange(v)
  }

  return (
    <div className="topbar">
      <div className="breadcrumb">
        Naviera / <strong>{label}</strong>
      </div>

      <div className="viagem-selector">
        <span>Viagem:</span>
        <select
          value={viagemAtiva?.id_viagem || ''}
          onChange={handleChange}
        >
          <option value="">Selecione...</option>
          {viagens.map(v => (
            <option key={v.id_viagem} value={v.id_viagem}>
              {v.descricao || `#${v.id_viagem}`} — {v.origem} → {v.destino}
              {v.ativa ? ' (ATIVA)' : ''}
            </option>
          ))}
        </select>
      </div>
    </div>
  )
}

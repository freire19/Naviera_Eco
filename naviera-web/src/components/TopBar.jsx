function formatViagem(v) {
  const data = v.data_viagem_fmt || v.data_viagem || ''
  const rota = v.nome_rota || (v.origem && v.destino ? `${v.origem} - ${v.destino}` : '')
  const desc = v.descricao ? ` ${v.descricao}` : ''
  const ativa = v.ativa ? ' (ATIVA)' : ''
  return `${v.id_viagem} - ${data}${desc} (${rota})${ativa}`
}

export default function TopBar({ label, viagens, viagemAtiva, onViagemChange }) {
  function handleChange(e) {
    // BIGINT do Postgres vem como string no node-pg — compara via String() para evitar mismatch
    const id = e.target.value
    const v = viagens.find(v => String(v.id_viagem) === id)
    if (v) onViagemChange(v)
    else onViagemChange(null)
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
              {formatViagem(v)}
            </option>
          ))}
        </select>
      </div>
    </div>
  )
}

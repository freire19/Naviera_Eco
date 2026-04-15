import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

const FORM_INICIAL = {
  id_rota: '',
  id_tipo_passagem: '',
  valor_transporte: '',
  valor_alimentacao: '',
  valor_cargas: '',
  valor_desconto: ''
}

export default function CadastroTarifa() {
  const [tarifas, setTarifas] = useState([])
  const [loading, setLoading] = useState(false)
  const [selecionado, setSelecionado] = useState(null)
  const [form, setForm] = useState(FORM_INICIAL)
  const [salvando, setSalvando] = useState(false)
  const [toast, setToast] = useState(null)
  const [rotas, setRotas] = useState([])
  const [tiposPassagem, setTiposPassagem] = useState([])

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  const carregar = useCallback(() => {
    setLoading(true)
    api.get('/cadastros/tarifas')
      .then(setTarifas)
      .catch(() => showToast('Erro ao carregar tarifas', 'error'))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { carregar() }, [carregar])

  useEffect(() => {
    api.get('/rotas').then(setRotas).catch(() => {})
    api.get('/cadastros/tipos-passageiro').then(setTiposPassagem).catch(() => {})
  }, [])

  function handleSelectRow(item) {
    setSelecionado(item)
    setForm({
      id_rota: item.id_rota || '',
      id_tipo_passagem: item.id_tipo_passagem || '',
      valor_transporte: item.valor_transporte ?? '',
      valor_alimentacao: item.valor_alimentacao ?? '',
      valor_cargas: item.valor_cargas ?? '',
      valor_desconto: item.valor_desconto ?? ''
    })
  }

  function handleNovo() {
    setSelecionado(null)
    setForm(FORM_INICIAL)
  }

  function handleChange(e) {
    setForm(prev => ({ ...prev, [e.target.name]: e.target.value }))
  }

  async function handleSalvar() {
    if (!form.id_rota || !form.id_tipo_passagem) {
      showToast('Selecione rota e tipo passageiro', 'error')
      return
    }
    setSalvando(true)
    try {
      const payload = {
        id_rota: form.id_rota,
        id_tipo_passagem: form.id_tipo_passagem,
        valor_transporte: Number(form.valor_transporte) || 0,
        valor_alimentacao: Number(form.valor_alimentacao) || 0,
        valor_cargas: Number(form.valor_cargas) || 0,
        valor_desconto: Number(form.valor_desconto) || 0
      }
      if (selecionado) {
        await api.put(`/cadastros/tarifas/${selecionado.id_tarifa}`, payload)
        showToast('Tarifa atualizada com sucesso')
      } else {
        await api.post('/cadastros/tarifas', payload)
        showToast('Tarifa criada com sucesso')
      }
      handleNovo()
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao salvar tarifa', 'error')
    } finally {
      setSalvando(false)
    }
  }

  async function handleExcluir() {
    if (!selecionado) { showToast('Selecione uma tarifa na tabela', 'error'); return }
    if (!window.confirm('Excluir tarifa selecionada?')) return
    try {
      await api.delete(`/cadastros/tarifas/${selecionado.id_tarifa}`)
      showToast('Tarifa excluida com sucesso')
      handleNovo()
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao excluir tarifa', 'error')
    }
  }

  function fmt(val) {
    return Number(val || 0).toFixed(2)
  }

  return (
    <div className="card">
      <h2 style={{ marginBottom: 16 }}>Cadastro de Tarifa</h2>

      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th style={{ width: 50 }}>ID</th>
              <th>Rota</th>
              <th>Tipo Passageiro</th>
              <th>Transp.</th>
              <th>Cargas</th>
              <th>Aliment</th>
              <th>Desconto</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="7">Carregando...</td></tr>
            ) : tarifas.length === 0 ? (
              <tr><td colSpan="7">Nenhuma tarifa cadastrada</td></tr>
            ) : tarifas.map(t => (
              <tr key={t.id_tarifa}
                  className={`clickable ${selecionado?.id_tarifa === t.id_tarifa ? 'selected' : ''}`}
                  onClick={() => handleSelectRow(t)}>
                <td>{t.id_tarifa}</td>
                <td>{t.origem && t.destino ? `${t.origem} - ${t.destino}` : t.rota || '-'}</td>
                <td>{t.nome_tipo_passageiro || '-'}</td>
                <td className="money">{fmt(t.valor_transporte)}</td>
                <td className="money">{fmt(t.valor_cargas)}</td>
                <td className="money">{fmt(t.valor_alimentacao)}</td>
                <td className="money">{fmt(t.valor_desconto)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="cadastro-inline-form">
        <label>ID:</label>
        <input type="text" value={selecionado?.id_tarifa || ''} readOnly />

        <label>Rota:</label>
        <select name="id_rota" value={form.id_rota} onChange={handleChange}>
          <option value=""></option>
          {rotas.map(r => (
            <option key={r.id_rota} value={r.id_rota}>{r.origem} - {r.destino}</option>
          ))}
        </select>

        <label>Tipo Passageiro:</label>
        <select name="id_tipo_passagem" value={form.id_tipo_passagem} onChange={handleChange}>
          <option value=""></option>
          {tiposPassagem.map(tp => (
            <option key={tp.id || tp.id_tipo_passagem} value={tp.id || tp.id_tipo_passagem}>{tp.nome || tp.nome_tipo_passagem}</option>
          ))}
        </select>

        <label>Transporte:</label>
        <input type="number" step="0.01" min="0" name="valor_transporte" value={form.valor_transporte} onChange={handleChange} placeholder="0.00" />

        <label>Cargas:</label>
        <input type="number" step="0.01" min="0" name="valor_cargas" value={form.valor_cargas} onChange={handleChange} placeholder="0.00" />

        <label>Alimentacao:</label>
        <input type="number" step="0.01" min="0" name="valor_alimentacao" value={form.valor_alimentacao} onChange={handleChange} placeholder="0.00" />

        <label>Desconto:</label>
        <input type="number" step="0.01" min="0" name="valor_desconto" value={form.valor_desconto} onChange={handleChange} placeholder="0.00" />
      </div>

      <div className="cadastro-buttons">
        <button onClick={handleNovo}>Novo</button>
        <button onClick={handleSalvar} disabled={salvando}>
          {salvando ? 'Salvando...' : 'Salvar'}
        </button>
        <button onClick={handleExcluir}>Excluir</button>
      </div>

      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}
    </div>
  )
}

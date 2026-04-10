import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}

const FORM_INICIAL = {
  id_rota: '',
  id_tipo_passageiro: '',
  valor_transporte: '',
  valor_alimentacao: '',
  valor_cargas: '',
  valor_desconto: ''
}

export default function CadastroTarifa({ viagemAtiva, onNavigate }) {
  const [tarifas, setTarifas] = useState([])
  const [loading, setLoading] = useState(false)
  const [modalAberto, setModalAberto] = useState(false)
  const [editando, setEditando] = useState(null)
  const [form, setForm] = useState(FORM_INICIAL)
  const [salvando, setSalvando] = useState(false)
  const [toast, setToast] = useState(null)

  const [rotas, setRotas] = useState([])
  const [tiposPassageiro, setTiposPassageiro] = useState([])

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
    api.get('/cadastros/tipos-passageiro').then(setTiposPassageiro).catch(() => {})
  }, [])

  function abrirCriar() {
    setEditando(null)
    setForm(FORM_INICIAL)
    setModalAberto(true)
  }

  function abrirEditar(item) {
    setEditando(item)
    setForm({
      id_rota: item.id_rota || '',
      id_tipo_passageiro: item.id_tipo_passageiro || '',
      valor_transporte: item.valor_transporte || '',
      valor_alimentacao: item.valor_alimentacao || '',
      valor_cargas: item.valor_cargas || '',
      valor_desconto: item.valor_desconto || ''
    })
    setModalAberto(true)
  }

  function fecharModal() {
    setModalAberto(false)
    setEditando(null)
    setForm(FORM_INICIAL)
  }

  function handleChange(e) {
    const { name, value } = e.target
    setForm(prev => ({ ...prev, [name]: value }))
  }

  async function handleSalvar(e) {
    e.preventDefault()
    setSalvando(true)
    try {
      const payload = {
        ...form,
        valor_transporte: Number(form.valor_transporte) || 0,
        valor_alimentacao: Number(form.valor_alimentacao) || 0,
        valor_cargas: Number(form.valor_cargas) || 0,
        valor_desconto: Number(form.valor_desconto) || 0
      }
      if (editando) {
        await api.put(`/cadastros/tarifas/${editando.id_tarifa}`, payload)
        showToast('Tarifa atualizada com sucesso')
      } else {
        await api.post('/cadastros/tarifas', payload)
        showToast('Tarifa criada com sucesso')
      }
      fecharModal()
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao salvar tarifa', 'error')
    } finally {
      setSalvando(false)
    }
  }

  return (
    <div className="card">
      <div className="card-header">
        <h2>Cadastro de Tarifas</h2>
        <div className="toolbar">
          <button className="btn-primary" onClick={abrirCriar}>+ Nova Tarifa</button>
        </div>
      </div>

      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>Rota</th>
              <th>Tipo Passageiro</th>
              <th>Transporte</th>
              <th>Alimentacao</th>
              <th>Cargas</th>
              <th>Desconto</th>
              <th>Acoes</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="7">Carregando...</td></tr>
            ) : tarifas.length === 0 ? (
              <tr><td colSpan="7">Nenhuma tarifa cadastrada</td></tr>
            ) : tarifas.map(t => (
              <tr key={t.id_tarifa}>
                <td>{t.rota || t.nome_rota || '-'}</td>
                <td>{t.tipo_passageiro || t.nome_tipo_passageiro || '-'}</td>
                <td className="money">{formatMoney(t.valor_transporte)}</td>
                <td className="money">{formatMoney(t.valor_alimentacao)}</td>
                <td className="money">{formatMoney(t.valor_cargas)}</td>
                <td className="money">{formatMoney(t.valor_desconto)}</td>
                <td>
                  <button className="btn-sm primary" onClick={() => abrirEditar(t)}>Editar</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {modalAberto && (
        <div className="modal-overlay" onClick={fecharModal}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3>{editando ? 'Editar Tarifa' : 'Nova Tarifa'}</h3>
            <form onSubmit={handleSalvar}>
              <div className="form-grid">
                <div className="form-group">
                  <label>Rota</label>
                  <select name="id_rota" value={form.id_rota} onChange={handleChange}>
                    <option value="">Selecione...</option>
                    {rotas.map(r => (
                      <option key={r.id_rota} value={r.id_rota}>{r.origem} - {r.destino}</option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label>Tipo Passageiro</label>
                  <select name="id_tipo_passageiro" value={form.id_tipo_passageiro} onChange={handleChange}>
                    <option value="">Selecione...</option>
                    {tiposPassageiro.map(tp => (
                      <option key={tp.id_tipo_passageiro} value={tp.id_tipo_passageiro}>{tp.descricao || tp.nome}</option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label>Valor Transporte</label>
                  <input type="number" step="0.01" name="valor_transporte" value={form.valor_transporte} onChange={handleChange} />
                </div>
                <div className="form-group">
                  <label>Valor Alimentacao</label>
                  <input type="number" step="0.01" name="valor_alimentacao" value={form.valor_alimentacao} onChange={handleChange} />
                </div>
                <div className="form-group">
                  <label>Valor Cargas</label>
                  <input type="number" step="0.01" name="valor_cargas" value={form.valor_cargas} onChange={handleChange} />
                </div>
                <div className="form-group">
                  <label>Valor Desconto</label>
                  <input type="number" step="0.01" name="valor_desconto" value={form.valor_desconto} onChange={handleChange} />
                </div>
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={fecharModal}>Cancelar</button>
                <button type="submit" className="btn-primary" disabled={salvando}>
                  {salvando ? 'Salvando...' : 'Salvar'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}
    </div>
  )
}

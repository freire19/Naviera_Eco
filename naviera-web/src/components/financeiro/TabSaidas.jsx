import { useState } from 'react'
import { api } from '../../api.js'
import { formatMoney, formatDate } from './formatters.js'

const SUMMARY_STYLE = { padding: '0.75rem 1rem', borderBottom: '1px solid var(--border)', display: 'flex', gap: 16, fontSize: 13 }

function ModalNovaSaida({ viagemAtiva, onSalvo, onFechar, mostrarToast }) {
  const [salvando, setSalvando] = useState(false)
  const [form, setForm] = useState({
    descricao: '',
    valor: '',
    data: '',
    tipo: '',
    observacoes: ''
  })

  function handleChange(e) {
    const { name, value } = e.target
    setForm(prev => ({ ...prev, [name]: value }))
  }

  async function handleSalvar(e) {
    e.preventDefault()

    if (!form.descricao.trim()) {
      mostrarToast('Preencha a descricao.', 'error')
      return
    }
    if (!form.valor || Number(form.valor) <= 0) {
      mostrarToast('Informe um valor valido.', 'error')
      return
    }
    if (!form.data) {
      mostrarToast('Informe a data.', 'error')
      return
    }

    setSalvando(true)
    try {
      await api.post('/financeiro/saida', {
        id_viagem: viagemAtiva.id_viagem,
        descricao: form.descricao.trim(),
        valor_total: parseFloat(form.valor),
        data_vencimento: form.data,
        id_categoria: null,
        tipo: form.tipo.trim() || null,
        observacoes: form.observacoes.trim() || null
      })
      mostrarToast('Saida criada com sucesso!')
      onSalvo()
    } catch (err) {
      mostrarToast(err?.message || 'Erro ao criar saida.', 'error')
    } finally {
      setSalvando(false)
    }
  }

  return (
    <div className="modal-overlay" onClick={onFechar}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <h3>Nova Saida</h3>
        <form onSubmit={handleSalvar}>
          <div className="form-grid">
            <div className="form-group full-width">
              <label>Descricao *</label>
              <input
                type="text"
                name="descricao"
                value={form.descricao}
                onChange={handleChange}
                placeholder="Ex: Combustivel, Manutencao..."
                autoFocus
              />
            </div>
            <div className="form-group">
              <label>Valor (R$) *</label>
              <input
                type="number"
                name="valor"
                value={form.valor}
                onChange={handleChange}
                placeholder="0.00"
                min="0.01"
                step="0.01"
              />
            </div>
            <div className="form-group">
              <label>Data *</label>
              <input
                type="date"
                name="data"
                value={form.data}
                onChange={handleChange}
              />
            </div>
            <div className="form-group">
              <label>Tipo</label>
              <input
                type="text"
                name="tipo"
                value={form.tipo}
                onChange={handleChange}
                placeholder="Ex: Combustivel, Alimentacao..."
              />
            </div>
            <div className="form-group full-width">
              <label>Observacoes</label>
              <textarea
                name="observacoes"
                value={form.observacoes}
                onChange={handleChange}
                rows={3}
                placeholder="Observacoes adicionais..."
              />
            </div>
          </div>
          <div className="modal-actions">
            <button type="button" className="btn-secondary" onClick={onFechar} disabled={salvando}>
              Cancelar
            </button>
            <button type="submit" className="btn-primary" disabled={salvando}>
              {salvando ? 'Salvando...' : 'Salvar'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

function ModalExcluirSaida({ saida, onExcluido, onFechar, mostrarToast }) {
  const [salvando, setSalvando] = useState(false)
  const [motivoExclusao, setMotivoExclusao] = useState('')

  async function confirmarExclusao() {
    if (!motivoExclusao.trim()) {
      mostrarToast('Informe o motivo da exclusao.', 'error')
      return
    }

    setSalvando(true)
    try {
      await api.delete(`/financeiro/saida/${saida.id_despesa || saida.id}`, {
        motivo: motivoExclusao.trim()
      })
      mostrarToast('Saida excluida com sucesso!')
      onExcluido()
    } catch (err) {
      mostrarToast(err?.message || 'Erro ao excluir saida.', 'error')
    } finally {
      setSalvando(false)
    }
  }

  return (
    <div className="modal-overlay" onClick={onFechar}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <h3>Excluir Saida</h3>
        <p>
          Tem certeza que deseja excluir a despesa <strong>{saida.descricao}</strong> de{' '}
          <strong>{formatMoney(saida.valor_total)}</strong>?
        </p>
        <div className="form-group" style={{ marginTop: '1rem' }}>
          <label>Motivo da exclusao *</label>
          <input
            type="text"
            value={motivoExclusao}
            onChange={e => setMotivoExclusao(e.target.value)}
            placeholder="Informe o motivo..."
            autoFocus
          />
        </div>
        <div className="modal-actions">
          <button type="button" className="btn-secondary" onClick={onFechar} disabled={salvando}>
            Cancelar
          </button>
          <button type="button" className="btn-primary" onClick={confirmarExclusao} disabled={salvando}>
            {salvando ? 'Excluindo...' : 'Confirmar Exclusao'}
          </button>
        </div>
      </div>
    </div>
  )
}

export default function TabSaidas({
  saidas,
  loadingSaidas,
  filtroTipo,
  onFiltroTipoChange,
  onExportar,
  viagemAtiva,
  mostrarToast,
  onDadosAlterados
}) {
  const [modalAberto, setModalAberto] = useState(false)
  const [modalExcluir, setModalExcluir] = useState(null)

  const saidasFiltradas = filtroTipo
    ? saidas.filter(s => (s.tipo || '').toLowerCase().includes(filtroTipo.toLowerCase()))
    : saidas

  return (
    <>
      <div className="card">
        <div className="card-header">
          <h3>Despesas (Saidas)</h3>
          <div className="toolbar">
            <input
              type="text"
              value={filtroTipo}
              onChange={e => onFiltroTipoChange(e.target.value)}
              placeholder="Filtrar por tipo..."
              style={{ fontSize: 12, width: 150 }}
            />
            <button className="btn-primary" onClick={() => setModalAberto(true)}>
              + Nova Saida
            </button>
            {saidasFiltradas.length > 0 && (
              <button className="btn-sm primary" onClick={() => onExportar(saidasFiltradas)}>Exportar CSV</button>
            )}
          </div>
        </div>

        {saidasFiltradas.length > 0 && (
          <div style={SUMMARY_STYLE}>
            <span>Total: <strong>{saidasFiltradas.length}</strong></span>
            <span>Valor: <strong className="money negative">{formatMoney(saidasFiltradas.reduce((s, d) => s + (parseFloat(d.valor_total) || 0), 0))}</strong></span>
          </div>
        )}

        <div className="table-container">
          {loadingSaidas ? (
            <p style={{ padding: '1rem', color: 'var(--text-muted)' }}>Carregando saidas...</p>
          ) : saidasFiltradas.length === 0 ? (
            <p style={{ padding: '1rem', color: 'var(--text-muted)' }}>Nenhuma saida registrada{filtroTipo ? ' com este filtro' : ' nesta viagem'}.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Data</th>
                  <th>Descricao</th>
                  <th>Tipo</th>
                  <th>Valor</th>
                  <th>Acoes</th>
                </tr>
              </thead>
              <tbody>
                {saidasFiltradas.map((s, idx) => (
                  <tr key={s.id_despesa || s.id || idx}>
                    <td>{formatDate(s.data_vencimento)}</td>
                    <td>{s.descricao}</td>
                    <td>
                      {s.tipo ? <span className="badge">{s.tipo}</span> : '\u2014'}
                    </td>
                    <td className="money">{formatMoney(s.valor_total)}</td>
                    <td>
                      <button
                        className="btn-sm danger"
                        onClick={() => setModalExcluir(s)}
                      >
                        Excluir
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {modalAberto && (
        <ModalNovaSaida
          viagemAtiva={viagemAtiva}
          mostrarToast={mostrarToast}
          onFechar={() => setModalAberto(false)}
          onSalvo={() => {
            setModalAberto(false)
            onDadosAlterados()
          }}
        />
      )}

      {modalExcluir && (
        <ModalExcluirSaida
          saida={modalExcluir}
          mostrarToast={mostrarToast}
          onFechar={() => setModalExcluir(null)}
          onExcluido={() => {
            setModalExcluir(null)
            onDadosAlterados()
          }}
        />
      )}
    </>
  )
}

import { useState, useEffect, useCallback, useMemo } from 'react'
import { api } from '../api.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}
function fmtDate(d) {
  if (!d) return '—'
  const s = String(d)
  if (/^\d{4}-\d{2}-\d{2}/.test(s)) {
    const [y, m, da] = s.substring(0, 10).split('-')
    return `${da}/${m}/${y}`
  }
  if (/^\d{2}\/\d{2}\/\d{4}$/.test(s)) return s
  const dt = new Date(s)
  return isNaN(dt) ? s : dt.toLocaleDateString('pt-BR')
}

/**
 * Extrato de Cliente consolidado (frete + encomenda + passagem).
 * Filtra por cliente, viagem, tipo e status; permite dar baixa em aberto.
 */
export default function ExtratoCliente({ viagens = [], viagemAtiva }) {
  const [clientes, setClientes] = useState([])
  const [cliente, setCliente] = useState('')
  const [viagemId, setViagemId] = useState('')
  const [tipoFrete, setTipoFrete] = useState(true)
  const [tipoEncomenda, setTipoEncomenda] = useState(true)
  const [tipoPassagem, setTipoPassagem] = useState(true)
  const [status, setStatus] = useState('todos') // todos | devedores | pagos

  const [itens, setItens] = useState([])
  const [loading, setLoading] = useState(false)
  const [toast, setToast] = useState(null)

  const [modalPagar, setModalPagar] = useState(null) // item selecionado
  const [valorPagar, setValorPagar] = useState('')
  const [salvando, setSalvando] = useState(false)

  function showToast(msg, type = 'success') { setToast({ msg, type }); setTimeout(() => setToast(null), 3500) }

  // Carrega lista de clientes (autocomplete)
  useEffect(() => {
    api.get('/extrato-cliente/clientes').then(setClientes).catch(() => {})
  }, [])

  const buscar = useCallback(async () => {
    if (!cliente) { showToast('Selecione um cliente', 'error'); return }
    const tipos = []
    if (tipoFrete) tipos.push('frete')
    if (tipoEncomenda) tipos.push('encomenda')
    if (tipoPassagem) tipos.push('passagem')
    if (tipos.length === 0) { showToast('Marque pelo menos um tipo', 'error'); return }

    setLoading(true)
    try {
      const params = new URLSearchParams({
        cliente,
        tipos: tipos.join(','),
        status,
      })
      if (viagemId) params.append('viagem_id', viagemId)
      const data = await api.get(`/extrato-cliente/buscar?${params}`)
      setItens(Array.isArray(data) ? data : [])
    } catch (err) {
      showToast(err?.message || 'Erro ao buscar extrato', 'error')
    } finally {
      setLoading(false)
    }
  }, [cliente, viagemId, tipoFrete, tipoEncomenda, tipoPassagem, status])

  // Totalizadores
  const totais = useMemo(() => {
    let total = 0, pago = 0, saldo = 0
    for (const it of itens) {
      total += Number(it.valor_total) || 0
      pago += Number(it.valor_pago) || 0
      saldo += Number(it.saldo_devedor) || 0
    }
    return { total, pago, saldo, count: itens.length }
  }, [itens])

  async function confirmarBaixa() {
    if (!modalPagar) return
    const valor = parseFloat(String(valorPagar).replace(',', '.'))
    if (!(valor > 0)) { showToast('Informe valor > 0', 'error'); return }
    if (valor > Number(modalPagar.saldo_devedor) + 0.01) {
      showToast('Valor maior que o saldo devedor', 'error'); return
    }
    setSalvando(true)
    try {
      await api.post('/extrato-cliente/baixa', {
        tipo: modalPagar.tipo,
        id_original: modalPagar.id_original,
        valor,
      })
      showToast('Baixa registrada')
      setModalPagar(null); setValorPagar('')
      buscar()
    } catch (err) {
      showToast(err?.message || 'Erro ao dar baixa', 'error')
    } finally {
      setSalvando(false)
    }
  }

  async function quitarTudo() {
    const devedores = itens.filter(it => Number(it.saldo_devedor) > 0.01)
    if (devedores.length === 0) { showToast('Nada em aberto', 'error'); return }
    const total = devedores.reduce((s, it) => s + Number(it.saldo_devedor), 0)
    if (!window.confirm(`Confirma quitar ${devedores.length} lançamento(s) totalizando ${formatMoney(total)}?`)) return
    setLoading(true)
    try {
      const r = await api.post('/extrato-cliente/quitar-tudo', {
        itens: devedores.map(it => ({
          tipo: it.tipo, id_original: it.id_original, valor: Number(it.saldo_devedor),
        })),
      })
      showToast(`${r.sucesso} lançamento(s) quitado(s)`)
      buscar()
    } catch (err) {
      showToast(err?.message || 'Erro ao quitar', 'error')
    } finally {
      setLoading(false)
    }
  }

  const S = {
    label: { fontSize: '0.75rem', fontWeight: 700, color: 'var(--text)', display: 'block', marginBottom: 3 },
    input: { width: '100%', padding: '7px 10px', fontSize: '0.85rem', background: 'var(--bg-soft)', border: '1px solid var(--border)', borderRadius: 6 },
    btn: { padding: '8px 14px', fontSize: '0.82rem', fontWeight: 600, borderRadius: 6, border: '1px solid var(--border)', cursor: 'pointer' },
  }

  return (
    <div className="page">
      <div className="card" style={{ marginBottom: 16 }}>
        <div className="card-header"><h3>Extrato de Cliente</h3>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Frete · Encomenda · Passagem consolidados</span>
        </div>

        {/* Filtros */}
        <div style={{ display: 'grid', gridTemplateColumns: '2fr 1.5fr auto', gap: 10, marginBottom: 10 }}>
          <div>
            <label style={S.label}>Cliente</label>
            <input style={S.input} list="clientes-list" value={cliente} onChange={e => setCliente(e.target.value)}
              placeholder="Selecione ou digite o nome do cliente..." />
            <datalist id="clientes-list">
              {clientes.map(c => <option key={c} value={c} />)}
            </datalist>
          </div>
          <div>
            <label style={S.label}>Viagem</label>
            <select style={S.input} value={viagemId} onChange={e => setViagemId(e.target.value)}>
              <option value="">Todas as viagens</option>
              {viagens.map(v => (
                <option key={v.id_viagem} value={v.id_viagem}>
                  {v.id_viagem} - {fmtDate(v.data_viagem)} {v.nome_rota ? `(${v.nome_rota})` : ''}
                </option>
              ))}
            </select>
          </div>
          <div style={{ display: 'flex', alignItems: 'flex-end' }}>
            <button style={{ ...S.btn, background: 'var(--primary)', color: '#fff', borderColor: 'var(--primary)' }}
              disabled={loading} onClick={buscar}>
              {loading ? 'Buscando...' : '🔍 Buscar'}
            </button>
          </div>
        </div>

        <div style={{ display: 'flex', gap: 20, alignItems: 'center', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', fontSize: '0.82rem' }}>
            <strong style={{ color: 'var(--text-muted)' }}>Tipo:</strong>
            <label><input type="checkbox" checked={tipoFrete} onChange={e => setTipoFrete(e.target.checked)} /> Frete</label>
            <label><input type="checkbox" checked={tipoEncomenda} onChange={e => setTipoEncomenda(e.target.checked)} /> Encomenda</label>
            <label><input type="checkbox" checked={tipoPassagem} onChange={e => setTipoPassagem(e.target.checked)} /> Passagem</label>
          </div>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', fontSize: '0.82rem' }}>
            <strong style={{ color: 'var(--text-muted)' }}>Status:</strong>
            <label><input type="radio" name="st" checked={status === 'todos'} onChange={() => setStatus('todos')} /> Todos</label>
            <label><input type="radio" name="st" checked={status === 'devedores'} onChange={() => setStatus('devedores')} /> A pagar</label>
            <label><input type="radio" name="st" checked={status === 'pagos'} onChange={() => setStatus('pagos')} /> Pagos</label>
          </div>
        </div>
      </div>

      {/* Totalizadores */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 10, marginBottom: 16 }}>
        <div className="stat-card primary">
          <span className="stat-label">Total Lançado</span>
          <span className="stat-value">{formatMoney(totais.total)}</span>
        </div>
        <div className="stat-card success">
          <span className="stat-label">Total Pago</span>
          <span className="stat-value">{formatMoney(totais.pago)}</span>
        </div>
        <div className="stat-card warning">
          <span className="stat-label">A Receber</span>
          <span className="stat-value" style={{ color: totais.saldo > 0.01 ? 'var(--danger)' : 'var(--success)' }}>
            {formatMoney(totais.saldo)}
          </span>
        </div>
        <div className="stat-card info">
          <span className="stat-label">Lançamentos</span>
          <span className="stat-value">{totais.count}</span>
        </div>
      </div>

      {/* Tabela */}
      <div className="card">
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Tipo</th>
                <th>Número</th>
                <th>Data Viagem</th>
                <th>Rota</th>
                <th>Remetente/Origem</th>
                <th>Descrição</th>
                <th style={{ textAlign: 'right' }}>Total</th>
                <th style={{ textAlign: 'right' }}>Pago</th>
                <th style={{ textAlign: 'right' }}>A Pagar</th>
                <th>Status</th>
                <th>Ação</th>
              </tr>
            </thead>
            <tbody>
              {itens.length === 0 ? (
                <tr><td colSpan="11" style={{ textAlign: 'center', color: 'var(--text-muted)', padding: 30 }}>
                  {loading ? 'Carregando...' : 'Selecione um cliente e clique em Buscar'}
                </td></tr>
              ) : itens.map((it, idx) => {
                const devedor = Number(it.saldo_devedor) > 0.01
                return (
                  <tr key={`${it.tipo}-${it.id_original}-${idx}`}>
                    <td><span style={{ fontWeight: 700, color: 'var(--text-soft)' }}>{it.tipo_label}</span></td>
                    <td>{it.numero || '—'}</td>
                    <td>{fmtDate(it.data_viagem)}</td>
                    <td>{it.rota || '—'}</td>
                    <td>{it.remetente_ou_origem || '—'}</td>
                    <td>{it.descricao}</td>
                    <td className="money">{formatMoney(it.valor_total)}</td>
                    <td className="money">{formatMoney(it.valor_pago)}</td>
                    <td className="money" style={{ color: devedor ? 'var(--danger)' : 'var(--success)', fontWeight: 700 }}>
                      {formatMoney(it.saldo_devedor)}
                    </td>
                    <td>
                      <span className={`badge ${devedor ? 'danger' : 'success'}`}>{it.status}</span>
                    </td>
                    <td>
                      {devedor && (
                        <button className="btn-sm primary" onClick={() => { setModalPagar(it); setValorPagar(String(it.saldo_devedor).replace('.', ',')) }}>
                          Dar Baixa
                        </button>
                      )}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>

        {itens.length > 0 && totais.saldo > 0.01 && (
          <div style={{ marginTop: 16, textAlign: 'right' }}>
            <button style={{ ...S.btn, background: 'var(--warning)', color: '#fff', borderColor: 'var(--warning)' }}
              onClick={quitarTudo} disabled={loading}>
              💰 Quitar Tudo em Aberto ({formatMoney(totais.saldo)})
            </button>
          </div>
        )}
      </div>

      {/* Modal dar baixa */}
      {modalPagar && (
        <div className="modal-overlay" onClick={() => setModalPagar(null)}>
          <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 420 }}>
            <h2>Dar Baixa</h2>
            <div style={{ fontSize: '0.88rem', marginBottom: 12 }}>
              <div><strong>{modalPagar.tipo_label} #{modalPagar.numero}</strong></div>
              <div style={{ color: 'var(--text-muted)', fontSize: '0.78rem' }}>{modalPagar.descricao}</div>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginBottom: 14 }}>
              <div>
                <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>TOTAL</div>
                <div style={{ fontWeight: 700 }}>{formatMoney(modalPagar.valor_total)}</div>
              </div>
              <div>
                <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>SALDO DEVEDOR</div>
                <div style={{ fontWeight: 700, color: 'var(--danger)' }}>{formatMoney(modalPagar.saldo_devedor)}</div>
              </div>
            </div>
            <label style={S.label}>Valor a pagar</label>
            <input style={S.input} autoFocus value={valorPagar} onChange={e => setValorPagar(e.target.value)} placeholder="0,00" />
            <div className="modal-actions">
              <button className="btn-secondary" onClick={() => setModalPagar(null)}>Cancelar</button>
              <button className="btn-sm primary" onClick={confirmarBaixa} disabled={salvando}>
                {salvando ? 'Salvando...' : 'Confirmar Baixa'}
              </button>
            </div>
          </div>
        </div>
      )}

      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}
    </div>
  )
}

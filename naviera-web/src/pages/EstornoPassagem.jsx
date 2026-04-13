import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}

const TIPO_LABELS = { passagem: 'Passagem', encomenda: 'Encomenda', frete: 'Frete' }

export default function EstornoPassagem({ viagemAtiva, onNavigate }) {
  const [tipo, setTipo] = useState('passagem')
  const [itens, setItens] = useState([])
  const [loading, setLoading] = useState(false)
  const [selecionado, setSelecionado] = useState(null)
  const [modalAberto, setModalAberto] = useState(false)
  const [etapa, setEtapa] = useState('form') // form | confirmar
  const [processando, setProcessando] = useState(false)
  const [toast, setToast] = useState(null)

  const [form, setForm] = useState({
    valor: '',
    motivo: '',
    forma_devolucao: 'DINHEIRO',
    login_autorizador: '',
    senha_autorizador: ''
  })

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  const carregarItens = useCallback(() => {
    if (!viagemAtiva) return
    setLoading(true)
    setSelecionado(null)
    const endpoint = tipo === 'passagem'
      ? `/passagens?viagem_id=${viagemAtiva.id_viagem}`
      : tipo === 'encomenda'
        ? `/encomendas?viagem_id=${viagemAtiva.id_viagem}`
        : `/fretes?viagem_id=${viagemAtiva.id_viagem}`
    api.get(endpoint)
      .then(data => setItens(Array.isArray(data) ? data : []))
      .catch(() => showToast('Erro ao carregar itens', 'error'))
      .finally(() => setLoading(false))
  }, [viagemAtiva, tipo])

  useEffect(() => { carregarItens() }, [carregarItens])

  function getItemId(item) {
    if (tipo === 'passagem') return item.id_passagem
    if (tipo === 'encomenda') return item.id_encomenda
    return item.id_frete
  }

  function getItemNumero(item) {
    if (tipo === 'passagem') return item.numero_bilhete
    if (tipo === 'encomenda') return item.numero_encomenda
    return item.numero_frete
  }

  function getItemLabel(item) {
    if (tipo === 'passagem') return `#${item.numero_bilhete} - ${item.nome_passageiro || 'Sem nome'}`
    if (tipo === 'encomenda') return `#${item.numero_encomenda} - ${item.remetente || ''} > ${item.destinatario || ''}`
    return `#${item.numero_frete} - ${item.remetente_nome_temp || ''} > ${item.destinatario_nome_temp || ''}`
  }

  function getValorPago(item) {
    return parseFloat(item.valor_pago) || 0
  }

  function getValorTotal(item) {
    if (tipo === 'passagem') return parseFloat(item.valor_total || item.valor_a_pagar) || 0
    if (tipo === 'encomenda') return parseFloat(item.total_a_pagar) || 0
    return parseFloat(item.valor_frete_calculado) || 0
  }

  function getValorDevedor(item) {
    if (tipo === 'passagem') return parseFloat(item.valor_devedor) || 0
    if (tipo === 'encomenda') return getValorTotal(item) - getValorPago(item)
    return parseFloat(item.valor_devedor) || 0
  }

  function abrirModal(item) {
    setSelecionado(item)
    setForm({
      valor: '',
      motivo: '',
      forma_devolucao: 'DINHEIRO',
      login_autorizador: '',
      senha_autorizador: ''
    })
    setEtapa('form')
    setModalAberto(true)
  }

  function fecharModal() {
    setModalAberto(false)
    setSelecionado(null)
    setEtapa('form')
  }

  function handleChange(e) {
    const { name, value } = e.target
    setForm(prev => ({ ...prev, [name]: value }))
  }

  function handleAvancar(e) {
    e.preventDefault()
    const valorEstorno = parseFloat(form.valor)
    if (!valorEstorno || valorEstorno <= 0) return showToast('Informe um valor valido', 'error')
    if (valorEstorno > getValorPago(selecionado) + 0.01) return showToast('Valor excede o valor pago', 'error')
    if (!form.motivo.trim()) return showToast('Informe o motivo', 'error')
    if (!form.login_autorizador.trim()) return showToast('Informe o login do autorizador', 'error')
    if (!form.senha_autorizador) return showToast('Informe a senha do autorizador', 'error')
    setEtapa('confirmar')
  }

  async function handleConfirmar() {
    setProcessando(true)
    try {
      const endpoint = `/estornos/${tipo}/${getItemId(selecionado)}`
      await api.post(endpoint, {
        valor: parseFloat(form.valor),
        motivo: form.motivo,
        forma_devolucao: form.forma_devolucao,
        login_autorizador: form.login_autorizador,
        senha_autorizador: form.senha_autorizador
      })
      showToast('Estorno realizado com sucesso')
      fecharModal()
      carregarItens()
    } catch (err) {
      showToast(err.message || 'Erro ao processar estorno', 'error')
      setEtapa('form')
    } finally {
      setProcessando(false)
    }
  }

  // Filtrar itens com valor pago > 0 (so faz sentido estornar se pagou algo)
  const itensComPagamento = itens.filter(i => getValorPago(i) > 0)

  if (!viagemAtiva) {
    return (
      <div className="placeholder-page">
        <h2>Estorno</h2>
        <p style={{ color: 'var(--text-muted)' }}>Selecione uma viagem para realizar estornos.</p>
      </div>
    )
  }

  return (
    <div className="page-content">
      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}

      <div className="page-header">
        <h2>Estorno de Pagamento</h2>
        <p style={{ color: 'var(--text-muted)', margin: 0 }}>
          Viagem #{viagemAtiva.id_viagem} — Selecione o tipo e o item para estornar
        </p>
      </div>

      {/* Seletor de tipo */}
      <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
        {['passagem', 'encomenda', 'frete'].map(t => (
          <button
            key={t}
            className={tipo === t ? 'btn-primary' : 'btn-secondary'}
            onClick={() => setTipo(t)}
          >
            {TIPO_LABELS[t]}s
          </button>
        ))}
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: 40, color: 'var(--text-muted)' }}>Carregando...</div>
      ) : itensComPagamento.length === 0 ? (
        <div style={{ textAlign: 'center', padding: 40, color: 'var(--text-muted)' }}>
          Nenhum(a) {TIPO_LABELS[tipo].toLowerCase()} com pagamento nesta viagem
        </div>
      ) : (
        <div className="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>Numero</th>
                <th>{tipo === 'passagem' ? 'Passageiro' : 'Remetente / Destinatario'}</th>
                <th>Valor Total</th>
                <th>Valor Pago</th>
                <th>Valor Devedor</th>
                <th>Status</th>
                <th>Acao</th>
              </tr>
            </thead>
            <tbody>
              {itensComPagamento.map(item => {
                const id = getItemId(item)
                const status = item.status_passagem || item.status_pagamento || item.status_frete || '—'
                return (
                  <tr key={id}>
                    <td>#{getItemNumero(item)}</td>
                    <td>
                      {tipo === 'passagem'
                        ? (item.nome_passageiro || '—')
                        : tipo === 'encomenda'
                          ? `${item.remetente || '—'} > ${item.destinatario || '—'}`
                          : `${item.remetente_nome_temp || '—'} > ${item.destinatario_nome_temp || '—'}`
                      }
                    </td>
                    <td>{formatMoney(getValorTotal(item))}</td>
                    <td>{formatMoney(getValorPago(item))}</td>
                    <td>{formatMoney(getValorDevedor(item))}</td>
                    <td><span className="badge">{status}</span></td>
                    <td>
                      <button className="btn-danger btn-sm" onClick={() => abrirModal(item)}>
                        Estornar
                      </button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Modal de Estorno */}
      {modalAberto && selecionado && (
        <div className="modal-overlay" onClick={fecharModal}>
          <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 500 }}>
            <h3>Estorno de {TIPO_LABELS[tipo]}</h3>

            {/* Info do item */}
            <div style={{ background: 'var(--bg-secondary)', padding: 12, borderRadius: 8, marginBottom: 16 }}>
              <p style={{ margin: '0 0 4px', fontWeight: 600 }}>{getItemLabel(selecionado)}</p>
              <div style={{ display: 'flex', gap: 16, fontSize: '0.9rem', color: 'var(--text-muted)' }}>
                <span>Total: {formatMoney(getValorTotal(selecionado))}</span>
                <span>Pago: {formatMoney(getValorPago(selecionado))}</span>
                <span>Devedor: {formatMoney(getValorDevedor(selecionado))}</span>
              </div>
            </div>

            {etapa === 'form' ? (
              <form onSubmit={handleAvancar}>
                <div className="form-grid">
                  <div className="form-group">
                    <label>Valor do Estorno *</label>
                    <input
                      name="valor"
                      type="number"
                      step="0.01"
                      min="0.01"
                      max={getValorPago(selecionado)}
                      value={form.valor}
                      onChange={handleChange}
                      placeholder="0.00"
                      autoFocus
                      required
                    />
                    <small style={{ color: 'var(--text-muted)' }}>
                      Maximo: {formatMoney(getValorPago(selecionado))}
                    </small>
                  </div>
                  <div className="form-group">
                    <label>Forma de Devolucao *</label>
                    <select name="forma_devolucao" value={form.forma_devolucao} onChange={handleChange}>
                      <option value="DINHEIRO">Dinheiro</option>
                      <option value="PIX">PIX</option>
                      <option value="CARTAO">Cartao</option>
                    </select>
                  </div>
                  <div className="form-group full-width">
                    <label>Motivo do Estorno *</label>
                    <textarea
                      name="motivo"
                      value={form.motivo}
                      onChange={handleChange}
                      rows={3}
                      placeholder="Descreva o motivo do estorno..."
                      required
                    />
                  </div>
                  <div className="form-group">
                    <label>Login do Autorizador *</label>
                    <input
                      name="login_autorizador"
                      value={form.login_autorizador}
                      onChange={handleChange}
                      placeholder="Nome ou email"
                      required
                    />
                  </div>
                  <div className="form-group">
                    <label>Senha do Autorizador *</label>
                    <input
                      name="senha_autorizador"
                      type="password"
                      value={form.senha_autorizador}
                      onChange={handleChange}
                      placeholder="Senha"
                      required
                    />
                  </div>
                </div>
                <div className="modal-actions">
                  <button type="button" className="btn-secondary" onClick={fecharModal}>
                    Cancelar
                  </button>
                  <button type="submit" className="btn-danger">
                    Revisar Estorno
                  </button>
                </div>
              </form>
            ) : (
              <div>
                <div style={{ background: 'var(--bg-secondary)', padding: 16, borderRadius: 8, marginBottom: 16 }}>
                  <h4 style={{ margin: '0 0 12px' }}>Confirmar Estorno</h4>
                  <table style={{ width: '100%', fontSize: '0.9rem' }}>
                    <tbody>
                      <tr><td style={{ padding: '4px 0', color: 'var(--text-muted)' }}>Valor:</td><td style={{ fontWeight: 600 }}>{formatMoney(form.valor)}</td></tr>
                      <tr><td style={{ padding: '4px 0', color: 'var(--text-muted)' }}>Forma:</td><td>{form.forma_devolucao}</td></tr>
                      <tr><td style={{ padding: '4px 0', color: 'var(--text-muted)' }}>Motivo:</td><td>{form.motivo}</td></tr>
                      <tr><td style={{ padding: '4px 0', color: 'var(--text-muted)' }}>Autorizador:</td><td>{form.login_autorizador}</td></tr>
                    </tbody>
                  </table>
                </div>
                <p style={{ color: 'var(--danger)', fontWeight: 500, fontSize: '0.9rem', marginBottom: 16 }}>
                  Esta acao nao pode ser desfeita. O valor sera devolvido e o pagamento sera ajustado.
                </p>
                <div className="modal-actions">
                  <button className="btn-secondary" onClick={() => setEtapa('form')} disabled={processando}>
                    Voltar
                  </button>
                  <button className="btn-danger" onClick={handleConfirmar} disabled={processando}>
                    {processando ? 'Processando...' : 'Confirmar Estorno'}
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

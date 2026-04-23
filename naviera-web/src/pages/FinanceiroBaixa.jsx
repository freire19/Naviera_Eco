import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}

/**
 * Tela de gestao financeira — dar baixa em pagamentos pendentes.
 * Props: tipo = 'passagens' | 'encomendas' | 'fretes'
 */
export default function FinanceiroBaixa({ viagemAtiva, onNavigate, onClose, tipo = 'passagens' }) {
  const [registros, setRegistros] = useState([])
  const [loading, setLoading] = useState(false)
  const [toast, setToast] = useState(null)
  const [modalPagar, setModalPagar] = useState(null)
  const [valorPagar, setValorPagar] = useState('')
  const [formaPagto, setFormaPagto] = useState('DINHEIRO')
  const [salvando, setSalvando] = useState(false)
  const [filtroStatus, setFiltroStatus] = useState('pendente')

  // Estorno
  const [modalEstorno, setModalEstorno] = useState(null)
  const [loginAdmin, setLoginAdmin] = useState('')
  const [senhaAdmin, setSenhaAdmin] = useState('')
  const [motivoEstorno, setMotivoEstorno] = useState('')
  const [estornando, setEstornando] = useState(false)
  const [erroSenha, setErroSenha] = useState('')

  function showToast(msg, type = 'success') { setToast({ msg, type }); setTimeout(() => setToast(null), 3500) }

  const config = {
    passagens: {
      titulo: 'Financeiro de Passagens',
      endpoint: '/financeiro/passagens',
      endpointPagar: '/passagens',
      idKey: 'id_passagem',
      numKey: 'numero_bilhete',
      nomeKey: 'nome_passageiro',
      totalKey: 'valor_total',
      pagoKey: 'valor_pago',
      devedorKey: 'valor_devedor',
      statusKey: 'status_passagem',
    },
    encomendas: {
      titulo: 'Financeiro de Encomendas',
      endpoint: '/financeiro/encomendas',
      endpointPagar: '/encomendas',
      idKey: 'id_encomenda',
      numKey: 'numero_encomenda',
      nomeKey: 'remetente',
      nome2Key: 'destinatario',
      totalKey: 'total_a_pagar',
      pagoKey: 'valor_pago',
      devedorCalc: true,
      statusKey: 'status_pagamento',
    },
    fretes: {
      titulo: 'Financeiro de Fretes',
      endpoint: '/financeiro/fretes',
      endpointPagar: '/fretes',
      idKey: 'id_frete',
      numKey: 'numero_frete',
      nomeKey: 'remetente',
      nome2Key: 'destinatario',
      totalKey: 'valor_total_itens',
      pagoKey: 'valor_pago',
      devedorCalc: true,
      statusKey: 'status_frete',
    }
  }

  const cfg = config[tipo] || config.passagens

  const carregar = useCallback(() => {
    if (!viagemAtiva) return
    setLoading(true)
    api.get(`${cfg.endpoint}?viagem_id=${viagemAtiva.id_viagem}`)
      .then(data => setRegistros(Array.isArray(data) ? data : []))
      .catch(() => showToast('Erro ao carregar', 'error'))
      .finally(() => setLoading(false))
  }, [viagemAtiva, cfg.endpoint])

  useEffect(() => { carregar() }, [carregar])

  // Filtrar
  const filtrados = registros.filter(r => {
    const total = parseFloat(r[cfg.totalKey]) || 0
    const pago = parseFloat(r[cfg.pagoKey]) || 0
    const devedor = cfg.devedorCalc ? Math.max(0, total - pago) : (parseFloat(r[cfg.devedorKey]) || Math.max(0, total - pago))
    if (filtroStatus === 'pendente' && devedor <= 0.01) return false
    if (filtroStatus === 'pago' && devedor > 0.01) return false
    return true
  })

  // Totais
  const totalGeral = filtrados.reduce((s, r) => s + (parseFloat(r[cfg.totalKey]) || 0), 0)
  const totalPago = filtrados.reduce((s, r) => s + (parseFloat(r[cfg.pagoKey]) || 0), 0)
  const totalDevedor = Math.max(0, totalGeral - totalPago)

  // Dar baixa
  async function darBaixa() {
    if (!modalPagar) return
    const valor = parseFloat(valorPagar)
    if (!valor || valor <= 0) { showToast('Informe o valor', 'error'); return }
    setSalvando(true)
    try {
      const payload = { valor_pago: valor }
      if (tipo === 'passagens') {
        if (formaPagto === 'DINHEIRO') payload.valor_pagamento_dinheiro = valor
        else if (formaPagto === 'PIX') payload.valor_pagamento_pix = valor
        else payload.valor_pagamento_cartao = valor
      }
      if (tipo === 'encomendas') {
        payload.forma_pagamento = formaPagto
      }
      await api.post(`${cfg.endpointPagar}/${modalPagar[cfg.idKey]}/pagar`, payload)
      showToast('Pagamento registrado!')
      setModalPagar(null)
      setValorPagar('')
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao registrar pagamento', 'error')
    } finally {
      setSalvando(false)
    }
  }

  // Estornar pagamento via /api/estornos/{tipo}/:id (estorno integral — valor total pago)
  async function estornar() {
    if (!modalEstorno) return
    if (!motivoEstorno.trim()) { setErroSenha('Informe o motivo do estorno'); return }
    if (!loginAdmin.trim()) { setErroSenha('Informe o login do autorizador'); return }
    if (!senhaAdmin.trim()) { setErroSenha('Informe a senha do autorizador'); return }
    setEstornando(true); setErroSenha('')
    try {
      const tipoMap = { passagens: 'passagem', encomendas: 'encomenda', fretes: 'frete' }
      const valorTotal = parseFloat(modalEstorno[cfg.pagoKey]) || 0
      if (valorTotal <= 0) { setErroSenha('Nao ha valor pago para estornar'); setEstornando(false); return }
      await api.post(`/estornos/${tipoMap[tipo]}/${modalEstorno[cfg.idKey]}`, {
        valor: valorTotal,
        motivo: motivoEstorno.trim(),
        login_autorizador: loginAdmin.trim(),
        senha_autorizador: senhaAdmin,
      })
      showToast(`Estorno de ${formatMoney(valorTotal)} realizado!`)
      setModalEstorno(null); setLoginAdmin(''); setSenhaAdmin(''); setMotivoEstorno('')
      carregar()
    } catch (err) {
      setErroSenha(err.message || 'Erro ao estornar')
    } finally { setEstornando(false) }
  }

  const I = { padding: '8px 10px', fontSize: '0.82rem', background: 'var(--bg-soft)', border: '1px solid var(--border)', borderRadius: 4, color: 'var(--text)', width: '100%', boxSizing: 'border-box' }
  const L = { fontSize: '0.72rem', fontWeight: 700, color: 'var(--text)', display: 'block', marginBottom: 3, marginTop: 8 }

  if (!viagemAtiva) {
    return <div className="placeholder-page"><div className="ph-icon">💰</div><h2>{cfg.titulo}</h2><p>Selecione uma viagem.</p></div>
  }

  return (
    <div>
      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}

      <div className="card" style={{ padding: 0 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 16px', borderBottom: '1px solid var(--border)' }}>
          <h2 style={{ margin: 0, fontSize: '1rem' }}>{cfg.titulo}</h2>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <select style={{ ...I, width: 'auto' }} value={filtroStatus} onChange={e => setFiltroStatus(e.target.value)}>
              <option value="">Todos</option>
              <option value="pendente">Pendentes</option>
              <option value="pago">Pagos</option>
            </select>
            <button style={{ padding: '8px 16px', background: '#047857', color: '#fff', border: 'none', borderRadius: 4, fontWeight: 700, cursor: 'pointer', fontSize: '0.8rem' }}
              onClick={carregar}>Atualizar</button>
            <button style={{ padding: '8px 16px', background: 'var(--bg-soft)', color: 'var(--text)', border: '1px solid var(--border)', borderRadius: 4, cursor: 'pointer', fontSize: '0.8rem' }}
              onClick={() => onClose ? onClose() : onNavigate && onNavigate('financeiro-entrada')}>Voltar</button>
          </div>
        </div>

        {/* TABELA */}
        <div style={{ overflow: 'auto', maxHeight: 'calc(100vh - 280px)' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead><tr style={{ background: '#047857', color: '#fff', position: 'sticky', top: 0 }}>
              <th style={{ padding: '6px 10px', fontSize: '0.72rem', textAlign: 'left' }}>N°</th>
              <th style={{ padding: '6px 10px', fontSize: '0.72rem', textAlign: 'left' }}>{tipo === 'passagens' ? 'Passageiro' : 'Remetente'}</th>
              {cfg.nome2Key && <th style={{ padding: '6px 10px', fontSize: '0.72rem', textAlign: 'left' }}>Destinatario</th>}
              <th style={{ padding: '6px 10px', fontSize: '0.72rem', textAlign: 'right' }}>Total</th>
              <th style={{ padding: '6px 10px', fontSize: '0.72rem', textAlign: 'right' }}>Pago</th>
              <th style={{ padding: '6px 10px', fontSize: '0.72rem', textAlign: 'right' }}>Devedor</th>
              <th style={{ padding: '6px 10px', fontSize: '0.72rem', textAlign: 'center' }}>Status</th>
              <th style={{ padding: '6px 10px', fontSize: '0.72rem', textAlign: 'center' }}>Acao</th>
            </tr></thead>
            <tbody>
              {loading ? <tr><td colSpan={cfg.nome2Key ? 8 : 7} style={{ padding: 30, textAlign: 'center' }}>Carregando...</td></tr>
              : filtrados.length === 0 ? <tr><td colSpan={cfg.nome2Key ? 8 : 7} style={{ padding: 30, textAlign: 'center', color: 'var(--text-muted)' }}>Nenhum registro {filtroStatus === 'pendente' ? 'pendente' : ''}</td></tr>
              : filtrados.map((r, idx) => {
                const total = parseFloat(r[cfg.totalKey]) || 0
                const pago = parseFloat(r[cfg.pagoKey]) || 0
                const devedor = cfg.devedorCalc ? Math.max(0, total - pago) : (parseFloat(r[cfg.devedorKey]) || Math.max(0, total - pago))
                const isPago = devedor <= 0.01
                return (
                  <tr key={r[cfg.idKey]} style={{ background: idx % 2 === 0 ? 'rgba(4,120,87,0.06)' : 'transparent' }}>
                    <td style={{ padding: '5px 10px', fontWeight: 700, color: '#047857' }}>{r[cfg.numKey]}</td>
                    <td style={{ padding: '5px 10px', fontSize: '0.8rem' }}>{r[cfg.nomeKey] || '\u2014'}</td>
                    {cfg.nome2Key && <td style={{ padding: '5px 10px', fontSize: '0.8rem' }}>{r[cfg.nome2Key] || '\u2014'}</td>}
                    <td style={{ padding: '5px 10px', textAlign: 'right', fontFamily: 'Space Mono, monospace', fontSize: '0.8rem' }}>{formatMoney(total)}</td>
                    <td style={{ padding: '5px 10px', textAlign: 'right', fontFamily: 'Space Mono, monospace', fontSize: '0.8rem', color: '#059669' }}>{formatMoney(pago)}</td>
                    <td style={{ padding: '5px 10px', textAlign: 'right', fontFamily: 'Space Mono, monospace', fontSize: '0.8rem', fontWeight: 700, color: devedor > 0.01 ? '#DC2626' : '#059669' }}>{formatMoney(devedor)}</td>
                    <td style={{ padding: '5px 10px', textAlign: 'center' }}>
                      <span className={`badge ${isPago ? 'success' : 'warning'}`}>{isPago ? 'PAGO' : 'PENDENTE'}</span>
                    </td>
                    <td style={{ padding: '5px 10px', textAlign: 'center' }}>
                      <div style={{ display: 'flex', gap: 4, justifyContent: 'center' }}>
                        {!isPago && (
                          <button style={{ padding: '3px 10px', background: '#059669', color: '#fff', border: 'none', borderRadius: 3, fontWeight: 700, cursor: 'pointer', fontSize: '0.68rem' }}
                            onClick={() => { setModalPagar(r); setValorPagar(String(devedor.toFixed(2))); setFormaPagto('DINHEIRO') }}>
                            Dar Baixa
                          </button>
                        )}
                        {isPago && (
                          <button style={{ padding: '3px 10px', background: '#DC2626', color: '#fff', border: 'none', borderRadius: 3, fontWeight: 700, cursor: 'pointer', fontSize: '0.68rem' }}
                            onClick={() => { setModalEstorno(r); setLoginAdmin(''); setSenhaAdmin(''); setMotivoEstorno(''); setErroSenha('') }}>
                            Estornar
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>

        {/* TOTAIS */}
        <div style={{ display: 'flex', justifyContent: 'center', gap: 20, padding: '10px 14px', borderTop: '2px solid #047857', fontSize: '0.82rem', background: 'var(--bg-soft)', flexWrap: 'wrap' }}>
          <span>Total: <strong style={{ color: '#047857' }}>{formatMoney(totalGeral)}</strong></span>
          <span>|</span>
          <span>Recebido: <strong style={{ color: '#059669' }}>{formatMoney(totalPago)}</strong></span>
          <span>|</span>
          <span>A Receber: <strong style={{ color: '#DC2626' }}>{formatMoney(totalDevedor)}</strong></span>
          <span>|</span>
          <span>Registros: <strong>{filtrados.length}</strong></span>
        </div>
      </div>

      {/* MODAL DAR BAIXA */}
      {modalPagar && (
        <div className="modal-overlay" onClick={() => setModalPagar(null)}>
          <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 420 }}>
            <h3>Dar Baixa — #{modalPagar[cfg.numKey]}</h3>
            <p style={{ color: 'var(--text-muted)', marginBottom: 8 }}>
              {modalPagar[cfg.nomeKey]}{cfg.nome2Key && modalPagar[cfg.nome2Key] ? ` → ${modalPagar[cfg.nome2Key]}` : ''}
            </p>
            <div style={{ display: 'flex', gap: 16, marginBottom: 8, fontSize: '0.85rem' }}>
              <span>Total: <strong>{formatMoney(modalPagar[cfg.totalKey])}</strong></span>
              <span>Pago: <strong style={{ color: '#059669' }}>{formatMoney(modalPagar[cfg.pagoKey])}</strong></span>
            </div>

            <label style={L}>Valor a Pagar (R$):</label>
            <input style={I} type="number" step="0.01" value={valorPagar} onChange={e => setValorPagar(e.target.value)} />

            <label style={L}>Forma de Pagamento:</label>
            <select style={I} value={formaPagto} onChange={e => setFormaPagto(e.target.value)}>
              <option>DINHEIRO</option>
              <option>PIX</option>
              <option>CARTAO</option>
            </select>

            <div style={{ display: 'flex', gap: 8, marginTop: 16, justifyContent: 'flex-end' }}>
              <button className="btn-sm" onClick={() => setModalPagar(null)}>Cancelar</button>
              <button style={{ padding: '8px 24px', background: '#059669', color: '#fff', border: 'none', borderRadius: 4, fontWeight: 700, cursor: 'pointer' }}
                onClick={darBaixa} disabled={salvando}>{salvando ? 'Processando...' : 'Confirmar Pagamento'}</button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL ESTORNO */}
      {modalEstorno && (
        <div className="modal-overlay" onClick={() => setModalEstorno(null)}>
          <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 450 }}>
            <h3 style={{ color: '#DC2626' }}>Estornar Pagamento</h3>
            <p style={{ color: 'var(--text-muted)', marginBottom: 8 }}>
              #{modalEstorno[cfg.numKey]} — {modalEstorno[cfg.nomeKey]}
              {cfg.nome2Key && modalEstorno[cfg.nome2Key] ? ` → ${modalEstorno[cfg.nome2Key]}` : ''}
            </p>
            <div style={{ padding: '8px 12px', background: 'rgba(220,53,69,0.08)', border: '1px solid #DC2626', borderRadius: 6, marginBottom: 12, fontSize: '0.85rem' }}>
              <strong>Valor pago que sera estornado: {formatMoney(modalEstorno[cfg.pagoKey])}</strong>
            </div>

            <label style={L}>Motivo do estorno: *</label>
            <textarea style={{ ...I, minHeight: 60 }} placeholder="Descreva o motivo do estorno..." value={motivoEstorno} onChange={e => setMotivoEstorno(e.target.value)} />

            <label style={L}>Login do Autorizador: *</label>
            <input style={I} type="text" autoComplete="off" placeholder="Login/email do usuario autorizador" value={loginAdmin} onChange={e => { setLoginAdmin(e.target.value); setErroSenha('') }} />

            <label style={L}>Senha do Autorizador: *</label>
            <input style={I} type="password" autoComplete="new-password" placeholder="Digite a senha" value={senhaAdmin} onChange={e => { setSenhaAdmin(e.target.value); setErroSenha('') }} />

            {erroSenha && <p style={{ color: '#DC2626', fontSize: '0.78rem', margin: '6px 0 0' }}>{erroSenha}</p>}

            <div style={{ display: 'flex', gap: 8, marginTop: 14, justifyContent: 'flex-end' }}>
              <button className="btn-sm" onClick={() => setModalEstorno(null)}>Cancelar</button>
              <button style={{ padding: '8px 20px', background: '#DC2626', color: '#fff', border: 'none', borderRadius: 4, fontWeight: 700, cursor: 'pointer' }}
                onClick={estornar} disabled={estornando}>{estornando ? 'Processando...' : 'Confirmar Estorno'}</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

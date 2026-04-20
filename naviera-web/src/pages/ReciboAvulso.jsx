import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { imprimirRecibo } from '../utils/reciboPrint.js'

function todayISO() { return new Date().toISOString().substring(0, 10) }

function formatarValorBR(v) {
  const n = Number(v) || 0
  return n.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

function formatarDataBR(iso) {
  if (!iso) return ''
  const d = new Date(iso.substring(0, 10) + 'T12:00:00')
  return d.toLocaleDateString('pt-BR')
}

function parseValor(str) {
  if (!str) return 0
  const normalizado = String(str).replace(/\./g, '').replace(',', '.')
  const n = parseFloat(normalizado)
  return Number.isFinite(n) ? n : 0
}

export default function ReciboAvulso({ viagemAtiva }) {
  const [nome, setNome] = useState('')
  const [valorStr, setValorStr] = useState('')
  const [dataEmissao, setDataEmissao] = useState(todayISO())
  const [referente, setReferente] = useState('')

  const [empresa, setEmpresa] = useState({})
  const [viagens, setViagens] = useState([])
  const [historico, setHistorico] = useState([])
  const [filtroViagem, setFiltroViagem] = useState(viagemAtiva?.id_viagem || '')
  const [toast, setToast] = useState(null)
  const [salvando, setSalvando] = useState(false)

  useEffect(() => {
    api.get('/cadastros/empresa').then(setEmpresa).catch(() => {})
    api.get('/viagens').then(r => setViagens(Array.isArray(r) ? r : [])).catch(() => {})
  }, [])

  useEffect(() => {
    const id = filtroViagem || viagemAtiva?.id_viagem
    if (!id) { setHistorico([]); return }
    api.get(`/recibos?id_viagem=${id}`).then(r => setHistorico(Array.isArray(r) ? r : [])).catch(() => setHistorico([]))
  }, [filtroViagem, viagemAtiva])

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  function empresaParaPrint() {
    return {
      nome: empresa.nome_embarcacao || empresa.companhia || 'Empresa',
      cnpj: empresa.cnpj || '',
      endereco: empresa.endereco || '',
      telefone: empresa.telefone || '',
      path_logo: empresa.path_logo || ''
    }
  }

  function validar() {
    if (!nome.trim()) { showToast('Informe o nome do pagador.', 'error'); return false }
    if (!valorStr.trim()) { showToast('Informe o valor.', 'error'); return false }
    const v = parseValor(valorStr)
    if (v <= 0) { showToast('Valor deve ser maior que zero.', 'error'); return false }
    if (!viagemAtiva || !viagemAtiva.id_viagem) {
      showToast('Nenhuma viagem ativa encontrada. Ative uma viagem no topo.', 'error'); return false
    }
    return true
  }

  function limparForm() {
    setNome(''); setValorStr(''); setReferente(''); setDataEmissao(todayISO())
  }

  async function salvarEImprimir(tipoImpressao) {
    if (!validar()) return
    setSalvando(true)
    try {
      const payload = {
        id_viagem: viagemAtiva.id_viagem,
        nome_pagador: nome.trim(),
        referente_a: referente.trim() || null,
        valor: parseValor(valorStr),
        data_emissao: dataEmissao,
        tipo_recibo: tipoImpressao
      }
      const novo = await api.post('/recibos', payload)
      showToast(`Recibo #${novo.id_recibo} salvo!`)
      imprimirRecibo(tipoImpressao, novo, empresaParaPrint())
      limparForm()
      // Recarregar historico
      api.get(`/recibos?id_viagem=${viagemAtiva.id_viagem}`)
        .then(r => setHistorico(Array.isArray(r) ? r : [])).catch(() => {})
    } catch (err) {
      showToast(err.error || err.message || 'Erro ao salvar recibo', 'error')
    } finally {
      setSalvando(false)
    }
  }

  function imprimirBranco() {
    imprimirRecibo('A4_BRANCO', { nome_pagador: '', valor: 0 }, empresaParaPrint())
  }

  async function reimprimir(recibo) {
    imprimirRecibo(recibo.tipo_recibo === 'TERMICA' ? 'TERMICA' : 'A4_PREENCHIDO', recibo, empresaParaPrint())
  }

  async function excluirRecibo(id) {
    if (!window.confirm(`Excluir recibo #${id}?`)) return
    try {
      await api.delete(`/recibos/${id}`)
      showToast('Recibo excluido.')
      setHistorico(historico.filter(r => r.id_recibo !== id))
    } catch (err) {
      showToast(err.error || 'Erro ao excluir', 'error')
    }
  }

  const L = { fontSize: '0.82rem', fontWeight: 700, color: 'var(--text)', display: 'block', marginBottom: 4 }
  const I = { width: '100%', padding: '8px 10px', border: '1px solid var(--border)', borderRadius: 4, fontSize: '0.88rem', boxSizing: 'border-box', background: 'var(--bg-soft)', color: 'var(--text)' }

  return (
    <div style={{ padding: '10px 4px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
        <h2 style={{ margin: 0 }}>EMISSAO DE RECIBOS AVULSOS</h2>
      </div>

      <div className="card" style={{ padding: 16, marginBottom: 18 }}>
        <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1fr', gap: 12, marginBottom: 12 }}>
          <div>
            <label style={L}>Nome do Pagador:</label>
            <input style={I} value={nome} onChange={e => setNome(e.target.value)} placeholder="Quem esta pagando..." />
          </div>
          <div>
            <label style={L}>Valor (R$):</label>
            <input style={I} value={valorStr} onChange={e => setValorStr(e.target.value)} placeholder="0,00" inputMode="decimal" />
          </div>
          <div>
            <label style={L}>Data do Recibo:</label>
            <input type="date" style={I} value={dataEmissao} onChange={e => setDataEmissao(e.target.value)} />
          </div>
        </div>

        <div>
          <label style={L}>Referente a (Descricao):</label>
          <textarea style={{ ...I, minHeight: 60, resize: 'vertical' }} value={referente} onChange={e => setReferente(e.target.value)} placeholder="Descricao do servico, produto ou motivo do recibo..." />
        </div>

        <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', marginTop: 14, alignItems: 'center' }}>
          <span style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginRight: 'auto' }}>Opcoes de Impressao:</span>
          <button className="btn-secondary" onClick={imprimirBranco} disabled={salvando}>A4 EM BRANCO</button>
          <button className="btn-primary" onClick={() => salvarEImprimir('A4_PREENCHIDO')} disabled={salvando}>
            {salvando ? 'Salvando...' : 'SALVAR E IMPRIMIR (A4)'}
          </button>
          <button className="btn-primary" style={{ background: '#047857' }} onClick={() => salvarEImprimir('TERMICA')} disabled={salvando}>
            SALVAR E IMPRIMIR (TERMICA)
          </button>
        </div>
      </div>

      <div className="card" style={{ padding: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
          <h3 style={{ margin: 0 }}>Historico de Recibos</h3>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <label style={{ fontSize: '0.82rem', fontWeight: 700 }}>Filtrar por Viagem:</label>
            <select style={{ ...I, width: 'auto', minWidth: 220 }} value={filtroViagem} onChange={e => setFiltroViagem(e.target.value)}>
              <option value="">Viagem ativa atual</option>
              {viagens.map(v => (
                <option key={v.id_viagem} value={v.id_viagem}>
                  {v.id_viagem} - {formatarDataBR(v.data_viagem)}{v.data_chegada ? ' ate ' + formatarDataBR(v.data_chegada) : ''}
                </option>
              ))}
            </select>
            {filtroViagem && (
              <button className="btn-secondary" onClick={() => setFiltroViagem('')}>Limpar Filtro</button>
            )}
          </div>
        </div>

        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.88rem' }}>
          <thead>
            <tr style={{ background: BRAND_GREEN }}>
              <th style={TH}>Data</th>
              <th style={TH}>Pagador</th>
              <th style={TH}>Referente a</th>
              <th style={{ ...TH, textAlign: 'right' }}>Valor (R$)</th>
              <th style={{ ...TH, textAlign: 'center', width: 120 }}>Acoes</th>
            </tr>
          </thead>
          <tbody>
            {historico.length === 0 && (
              <tr><td colSpan="5" style={{ padding: 28, textAlign: 'center', color: 'var(--text-muted)' }}>Nao ha conteudo na tabela</td></tr>
            )}
            {historico.map(r => (
              <tr key={r.id_recibo} style={{ borderBottom: '1px solid var(--border)' }}>
                <td style={TD}>{formatarDataBR(r.data_emissao)}</td>
                <td style={TD}>{r.nome_pagador}</td>
                <td style={TD}>{r.referente_a || '—'}</td>
                <td style={{ ...TD, textAlign: 'right' }}>{formatarValorBR(r.valor)}</td>
                <td style={{ ...TD, textAlign: 'center' }}>
                  <button className="btn-secondary" style={{ padding: '4px 10px', fontSize: '0.78rem' }} onClick={() => reimprimir(r)} title="Reimprimir">🖨</button>
                  <button className="btn-secondary" style={{ padding: '4px 10px', fontSize: '0.78rem', marginLeft: 4, color: 'var(--danger)' }} onClick={() => excluirRecibo(r.id_recibo)} title="Excluir">×</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}
    </div>
  )
}

const BRAND_GREEN = '#059669'
const TH = { textAlign: 'left', padding: '10px 12px', color: 'white', fontWeight: 700, fontSize: '0.82rem' }
const TD = { padding: '10px 12px', color: 'var(--text)' }

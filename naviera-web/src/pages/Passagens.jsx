import { useState, useEffect, useCallback, useRef } from 'react'
import { api } from '../api.js'
import { printBilhete, printListaPassageiros } from '../utils/print.js'
import Autocomplete from '../components/Autocomplete.jsx'
import ModalPagarPassagem from '../components/ModalPagarPassagem.jsx'

function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}
function formatDate(val) {
  if (!val) return '—'
  const s = String(val)
  if (s.includes('/')) return s
  if (s === 'Invalid Date' || s === 'undefined' || s === 'null') return '—'
  try {
    const d = new Date(s.includes('T') ? s : s + 'T00:00:00')
    if (isNaN(d.getTime())) return '—'
    return d.toLocaleDateString('pt-BR')
  } catch { return '—' }
}
function calcIdade(dataNasc) {
  if (!dataNasc) return ''
  const nasc = new Date(dataNasc + 'T00:00:00')
  const hoje = new Date()
  let idade = hoje.getFullYear() - nasc.getFullYear()
  const m = hoje.getMonth() - nasc.getMonth()
  if (m < 0 || (m === 0 && hoje.getDate() < nasc.getDate())) idade--
  return idade >= 0 ? idade : ''
}

const FORM_INICIAL = {
  id_passageiro: '', nome_passageiro: '', numero_doc: '',
  id_tipo_doc: '', id_nacionalidade: '', data_nascimento: '', id_sexo: '',
  id_rota: '', id_acomodacao: '', id_tipo_passagem: '',
  id_agente: '', numero_requisicao: '', assento: '',
  valor_alimentacao: '', valor_transporte: '', valor_cargas: '',
  valor_desconto_tarifa: '', valor_desconto_geral: '', observacoes: ''
}

export default function Passagens({ viagemAtiva }) {
  const [passagens, setPassagens] = useState([])
  const [loading, setLoading] = useState(false)
  const [selecionada, setSelecionada] = useState(null)
  const [editando, setEditando] = useState(false)
  const [form, setForm] = useState(FORM_INICIAL)
  const [salvando, setSalvando] = useState(false)
  const [toast, setToast] = useState(null)
  const [modalPagar, setModalPagar] = useState(null)
  const [passagemRecemCriada, setPassagemRecemCriada] = useState(null)

  // Dados auxiliares
  const [rotas, setRotas] = useState([])
  const [acomodacoes, setAcomodacoes] = useState([])
  const [tiposPassagem, setTiposPassagem] = useState([])
  const [agentes, setAgentes] = useState([])
  const [nacionalidades, setNacionalidades] = useState([])
  const [tiposDocumento, setTiposDocumento] = useState([])
  const [sexos, setSexos] = useState([])
  const [caixas, setCaixas] = useState([])

  // Busca passageiro
  const [sugestoes, setSugestoes] = useState([])
  const [buscando, setBuscando] = useState(false)
  const debounceRef = useRef(null)

  // Busca na tabela
  const [filtro, setFiltro] = useState('')
  const [modoBusca, setModoBusca] = useState('nome')

  // Tarifas auto
  const [tarifaLoading, setTarifaLoading] = useState(false)

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  // Calculos
  const alimentacao = parseFloat(form.valor_alimentacao) || 0
  const transporte = parseFloat(form.valor_transporte) || 0
  const cargas = parseFloat(form.valor_cargas) || 0
  const descontoTarifa = parseFloat(form.valor_desconto_tarifa) || 0
  const descontoGeral = parseFloat(form.valor_desconto_geral) || 0
  const subtotal = Math.round((alimentacao + transporte + cargas - descontoTarifa) * 100) / 100
  const valorAPagar = Math.round((subtotal - descontoGeral) * 100) / 100
  const idade = calcIdade(form.data_nascimento)

  // Carregar passagens
  const carregarPassagens = useCallback(() => {
    if (!viagemAtiva) return
    setLoading(true)
    api.get(`/passagens?viagem_id=${viagemAtiva.id_viagem}`)
      .then(setPassagens)
      .catch(() => showToast('Erro ao carregar passagens', 'error'))
      .finally(() => setLoading(false))
  }, [viagemAtiva])

  useEffect(() => { carregarPassagens() }, [carregarPassagens])

  useEffect(() => {
    Promise.allSettled([
      api.get('/rotas').then(setRotas),
      api.get('/cadastros/acomodacoes').then(setAcomodacoes),
      api.get('/cadastros/tipos-passageiro').then(setTiposPassagem),
      api.get('/cadastros/agentes').then(setAgentes),
      api.get('/cadastros/nacionalidades').then(setNacionalidades),
      api.get('/cadastros/tipos-documento').then(setTiposDocumento),
      api.get('/cadastros/sexos').then(setSexos),
      api.get('/cadastros/caixas').then(setCaixas)
    ]).catch(() => {})
  }, [])

  // Buscar tarifa automaticamente
  useEffect(() => {
    if (!form.id_rota || !form.id_tipo_passagem) return
    setTarifaLoading(true)
    api.get(`/cadastros/tarifas/busca?id_rota=${form.id_rota}&id_tipo_passagem=${form.id_tipo_passagem}`)
      .then(tarifa => {
        if (tarifa) {
          setForm(prev => ({
            ...prev,
            valor_transporte: tarifa.valor_transporte ?? '',
            valor_alimentacao: tarifa.valor_alimentacao ?? '',
            valor_cargas: tarifa.valor_cargas ?? '',
            valor_desconto_tarifa: tarifa.valor_desconto ?? ''
          }))
        }
      })
      .catch(() => {})
      .finally(() => setTarifaLoading(false))
  }, [form.id_rota, form.id_tipo_passagem])

  function handleChange(e) {
    setForm(prev => ({ ...prev, [e.target.name]: e.target.value }))
  }

  // Busca passageiro autocomplete
  const handleNomeChange = useCallback((value) => {
    setForm(prev => ({ ...prev, nome_passageiro: value, id_passageiro: '' }))
    if (debounceRef.current) clearTimeout(debounceRef.current)
    if (value.trim().length < 2) { setSugestoes([]); return }
    debounceRef.current = setTimeout(() => {
      setBuscando(true)
      api.get(`/passagens/busca-passageiro?q=${encodeURIComponent(value.trim())}`)
        .then(data => setSugestoes(Array.isArray(data) ? data : []))
        .catch(() => setSugestoes([]))
        .finally(() => setBuscando(false))
    }, 300)
  }, [])

  function selecionarPassageiro(p) {
    setForm(prev => ({
      ...prev,
      nome_passageiro: p.nome_passageiro,
      numero_doc: p.numero_documento || '',
      id_passageiro: p.id_passageiro,
      data_nascimento: p.data_nascimento ? p.data_nascimento.substring(0, 10) : '',
      id_tipo_doc: p.id_tipo_doc || '',
      id_sexo: p.id_sexo || '',
      id_nacionalidade: p.id_nacionalidade || ''
    }))
    setSugestoes([])
  }

  // NOVO
  function handleNovo() {
    setSelecionada(null)
    setEditando(true)
    setForm(FORM_INICIAL)
  }

  // Selecionar da tabela
  function handleSelectRow(p) {
    setSelecionada(p)
    setEditando(false)
  }

  // EDITAR
  function handleEditar() {
    if (!selecionada) { showToast('Selecione uma passagem', 'error'); return }
    setEditando(true)
    setForm({
      id_passageiro: selecionada.id_passageiro || '',
      nome_passageiro: selecionada.nome_passageiro || '',
      numero_doc: selecionada.numero_doc || '',
      id_tipo_doc: selecionada.id_tipo_doc || '',
      id_nacionalidade: selecionada.id_nacionalidade || selecionada.pas_id_nacionalidade || '',
      data_nascimento: selecionada.data_nascimento ? selecionada.data_nascimento.substring(0, 10) : '',
      id_sexo: selecionada.id_sexo || '',
      id_rota: selecionada.id_rota || '',
      id_acomodacao: selecionada.id_acomodacao || '',
      id_tipo_passagem: selecionada.id_tipo_passagem || '',
      id_agente: selecionada.id_agente || '',
      numero_requisicao: selecionada.numero_requisicao || '',
      assento: selecionada.assento || '',
      valor_alimentacao: selecionada.valor_alimentacao ?? '',
      valor_transporte: selecionada.valor_transporte ?? '',
      valor_cargas: selecionada.valor_cargas ?? '',
      valor_desconto_tarifa: selecionada.valor_desconto_tarifa ?? '',
      valor_desconto_geral: selecionada.valor_desconto_geral ?? '',
      observacoes: selecionada.observacoes || ''
    })
  }

  // CANCELAR
  function handleCancelar() {
    setEditando(false)
    setForm(FORM_INICIAL)
  }

  // FINALIZAR (salvar + abrir pagamento)
  async function handleFinalizar() {
    if (!form.nome_passageiro.trim()) { showToast('Informe o nome do passageiro', 'error'); return }
    if (valorAPagar < 0) { showToast('Valor a pagar nao pode ser negativo', 'error'); return }

    setSalvando(true)
    try {
      const payload = {
        id_viagem: viagemAtiva.id_viagem,
        id_passageiro: form.id_passageiro || null,
        nome_passageiro: form.nome_passageiro.trim(),
        documento: form.numero_doc || null,
        data_nascimento: form.data_nascimento || null,
        id_tipo_doc: form.id_tipo_doc || null,
        id_sexo: form.id_sexo || null,
        id_nacionalidade: form.id_nacionalidade || null,
        assento: form.assento || null,
        id_rota: form.id_rota || null,
        id_tipo_passagem: form.id_tipo_passagem || null,
        id_acomodacao: form.id_acomodacao || null,
        id_agente: form.id_agente || null,
        numero_requisicao: form.numero_requisicao || null,
        id_horario_saida: viagemAtiva.id_horario_saida || null,
        valor_alimentacao: alimentacao,
        valor_transporte: transporte,
        valor_cargas: cargas,
        valor_desconto_tarifa: descontoTarifa,
        valor_desconto_geral: descontoGeral,
        valor_total: valorAPagar > 0 ? valorAPagar : subtotal,
        valor_pago: 0,
        observacoes: form.observacoes || null
      }

      let resultado
      if (selecionada && selecionada.id_passagem) {
        resultado = await api.put(`/passagens/${selecionada.id_passagem}`, payload)
        showToast('Passagem atualizada')
      } else {
        resultado = await api.post('/passagens', payload)
        showToast('Passagem criada')
      }

      setEditando(false)
      setForm(FORM_INICIAL)
      await carregarPassagens()

      // Abrir pagamento automaticamente se valor > 0
      if (valorAPagar > 0) {
        const passagemParaPagar = resultado || { ...payload, id_passagem: resultado?.id_passagem, valor_a_pagar: valorAPagar, valor_pago: 0, numero_bilhete: resultado?.numero_bilhete }
        // Recarregar e pegar a passagem mais recente
        const atualizada = await api.get(`/passagens?viagem_id=${viagemAtiva.id_viagem}`)
        const mais_recente = Array.isArray(atualizada) ? atualizada[0] : null
        if (mais_recente) {
          setModalPagar(mais_recente)
        }
      }
    } catch (err) {
      showToast(err.message || 'Erro ao salvar passagem', 'error')
    } finally {
      setSalvando(false)
    }
  }

  // EXCLUIR
  async function handleExcluir() {
    if (!selecionada) { showToast('Selecione uma passagem', 'error'); return }
    if (!window.confirm(`Excluir passagem ${selecionada.numero_bilhete}?`)) return
    try {
      await api.delete(`/passagens/${selecionada.id_passagem}`)
      showToast('Passagem excluida')
      setSelecionada(null)
      carregarPassagens()
    } catch (err) {
      showToast(err.message || 'Erro ao excluir', 'error')
    }
  }

  // Filtro da tabela
  const passagensFiltradas = passagens.filter(p => {
    if (!filtro.trim()) return true
    const q = filtro.toLowerCase()
    if (modoBusca === 'numero') return String(p.numero_bilhete).includes(q)
    if (modoBusca === 'doc') return (p.numero_doc || '').toLowerCase().includes(q)
    return (p.nome_passageiro || '').toLowerCase().includes(q)
  })

  if (!viagemAtiva) {
    return (
      <div className="placeholder-page">
        <div className="ph-icon">🎫</div>
        <h2>Emissao de Passagens</h2>
        <p>Selecione uma viagem no topo para comecar.</p>
      </div>
    )
  }

  const I = { padding: '7px 10px', fontSize: '0.82rem', background: 'var(--bg-soft)', border: '1px solid var(--border)', borderRadius: 4, color: 'var(--text)', fontFamily: 'Sora, sans-serif', width: '100%', boxSizing: 'border-box' }
  const L = { fontSize: '0.68rem', fontWeight: 600, textTransform: 'uppercase', color: 'var(--text-muted)', marginBottom: 2, letterSpacing: '0.03em', display: 'block' }
  const RO = { ...I, opacity: 0.6, cursor: 'default' }
  const MONO = { ...I, textAlign: 'right', fontFamily: 'Space Mono, monospace', fontSize: '0.82rem' }

  // Datas da viagem
  const vData = formatDate(viagemAtiva.data_viagem)
  const vCheg = formatDate(viagemAtiva.data_chegada)
  const vHora = viagemAtiva.horario || '—'
  const vRota = viagemAtiva.nome_rota || [viagemAtiva.origem, viagemAtiva.destino].filter(Boolean).join(' - ')
  const vDescViagem = `${viagemAtiva.id_viagem} - ${vData} (${vRota}) - Prev: ${vCheg}`

  return (
    <div className="card" style={{ padding: 12 }}>
      {/* HEADER */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12, gap: 12 }}>
        <button className="btn-primary" onClick={handleNovo} style={{ width: 'auto', padding: '8px 20px', whiteSpace: 'nowrap' }}>
          NOVO (F2)
        </button>
        <h2 style={{ fontSize: '1.1rem', margin: 0, flex: 1, textAlign: 'center' }}>EMISSAO DE PASSAGENS</h2>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <span style={labelStyle}>N° Bilhete:</span>
          <input style={{ ...roStyle, width: 80, textAlign: 'center' }} value={selecionada?.numero_bilhete || '—'} readOnly />
        </div>
      </div>

      {/* FORM — Passageiro (grid 6 colunas) */}
      <div style={{ display: 'grid', gridTemplateColumns: '3fr 1fr 1fr 1fr 1.2fr 0.7fr', gap: '6px 10px', marginBottom: 8, padding: 10, border: '1px solid var(--border)', borderRadius: 6 }}>
        <div>
          <label style={L}>Passageiro (Nome Completo) *</label>
          <Autocomplete
            value={form.nome_passageiro}
            onChange={handleNomeChange}
            onSelect={selecionarPassageiro}
            suggestions={sugestoes}
            loading={buscando}
            placeholder="Digite o nome do passageiro..."
            emptyMessage="Nenhum encontrado. Sera cadastrado como novo."
            renderItem={(s) => <><strong>{s.nome_passageiro}</strong>{s.numero_documento && <span style={{ color: 'var(--text-muted)', marginLeft: 6, fontSize: 11 }}>{s.numero_documento}</span>}</>}
          />
        </div>
        <div>
          <label style={L}>N° Documento</label>
          <input style={I} name="numero_doc" value={form.numero_doc} onChange={handleChange} />
        </div>
        <div>
          <label style={L}>Tipo Doc</label>
          <select style={I} name="id_tipo_doc" value={form.id_tipo_doc} onChange={handleChange}>
            <option value=""></option>
            {tiposDocumento.map(t => <option key={t.id_tipo_doc} value={t.id_tipo_doc}>{t.nome_tipo_doc}</option>)}
          </select>
        </div>
        <div>
          <label style={L}>Nacionalidade</label>
          <select style={I} name="id_nacionalidade" value={form.id_nacionalidade} onChange={handleChange}>
            <option value=""></option>
            {nacionalidades.map(n => <option key={n.id_nacionalidade} value={n.id_nacionalidade}>{n.nome_nacionalidade}</option>)}
          </select>
        </div>
        <div>
          <label style={L}>Nascimento / Idade</label>
          <div style={{ display: 'flex', gap: 4 }}>
            <input type="date" style={{ ...I, flex: 1 }} name="data_nascimento" value={form.data_nascimento} onChange={handleChange} />
            <input style={{ ...RO, width: 38, textAlign: 'center', fontSize: '0.75rem', padding: '7px 2px' }} value={idade} readOnly tabIndex={-1} />
          </div>
        </div>
        <div>
          <label style={L}>Sexo</label>
          <select style={I} name="id_sexo" value={form.id_sexo} onChange={handleChange}>
            <option value=""></option>
            {sexos.map(s => <option key={s.id_sexo} value={s.id_sexo}>{s.nome_sexo}</option>)}
          </select>
        </div>
      </div>

      {/* FORM — Viagem (grid 7 colunas) */}
      <div style={{ display: 'grid', gridTemplateColumns: '2.5fr 1.3fr 1.2fr 1fr 1fr 1fr 0.6fr', gap: '6px 10px', marginBottom: 8, padding: 10, border: '1px solid var(--border)', borderRadius: 6 }}>
        <div>
          <label style={L}>Viagem Selecionada</label>
          <input style={{ ...RO, fontWeight: 600, fontSize: '0.78rem' }} value={vDescViagem} readOnly tabIndex={-1} />
        </div>
        <div>
          <label style={L}>Data / Hora</label>
          <div style={{ display: 'flex', gap: 4 }}>
            <input style={{ ...RO, flex: 1 }} value={vData} readOnly tabIndex={-1} />
            <input style={{ ...RO, width: 55, textAlign: 'center' }} value={vHora} readOnly tabIndex={-1} />
          </div>
        </div>
        <div>
          <label style={L}>Rota</label>
          <select style={I} name="id_rota" value={form.id_rota} onChange={handleChange}>
            <option value=""></option>
            {rotas.map(r => <option key={r.id_rota} value={r.id_rota}>{r.origem} - {r.destino}</option>)}
          </select>
        </div>
        <div>
          <label style={L}>Acomodacao</label>
          <select style={I} name="id_acomodacao" value={form.id_acomodacao} onChange={handleChange}>
            <option value=""></option>
            {acomodacoes.map(a => <option key={a.id_acomodacao} value={a.id_acomodacao}>{a.nome_acomodacao}</option>)}
          </select>
        </div>
        <div>
          <label style={L}>Tipo Pass.</label>
          <select style={I} name="id_tipo_passagem" value={form.id_tipo_passagem} onChange={handleChange}>
            <option value=""></option>
            {tiposPassagem.map(t => <option key={t.id || t.id_tipo_passagem} value={t.id || t.id_tipo_passagem}>{t.nome || t.nome_tipo_passagem}</option>)}
          </select>
        </div>
        <div>
          <label style={L}>Agente</label>
          <select style={I} name="id_agente" value={form.id_agente} onChange={handleChange}>
            <option value=""></option>
            {agentes.map(a => <option key={a.id_agente} value={a.id_agente}>{a.nome_agente}</option>)}
          </select>
        </div>
        <div>
          <label style={L}>Req.</label>
          <input style={I} name="numero_requisicao" value={form.numero_requisicao} onChange={handleChange} />
        </div>
      </div>

      {/* TARIFAS + FINALIZAR (grid fixo) */}
      <div style={{ display: 'grid', gridTemplateColumns: 'auto 1fr auto', gap: '0 16px', marginBottom: 12, padding: 10, border: '1px solid var(--border)', borderRadius: 6, alignItems: 'end' }}>
        <div style={{ display: 'flex', gap: 8, alignItems: 'flex-end', border: '1px solid var(--border)', borderRadius: 4, padding: '6px 10px' }}>
          <span style={{ ...L, whiteSpace: 'nowrap', paddingBottom: 8, marginBottom: 0 }}>TARIFAS:</span>
          {[
            { label: 'Alim.', name: 'valor_alimentacao' },
            { label: 'Transp.', name: 'valor_transporte' },
            { label: 'Cargas', name: 'valor_cargas' },
            { label: 'Desc.', name: 'valor_desconto_tarifa' }
          ].map(f => (
            <div key={f.name} style={{ width: 80 }}>
              <label style={{ ...L, fontSize: '0.62rem' }}>{f.label}</label>
              <input style={MONO} type="number" step="0.01" min="0" name={f.name} value={form[f.name]} onChange={handleChange} />
            </div>
          ))}
        </div>

        <div style={{ display: 'flex', gap: 10, alignItems: 'flex-end', justifyContent: 'flex-end' }}>
          <div style={{ width: 100 }}>
            <label style={L}>Subtotal</label>
            <input style={{ ...RO, textAlign: 'right', fontFamily: 'Space Mono, monospace' }} value={subtotal.toFixed(2)} readOnly tabIndex={-1} />
          </div>
          <div style={{ width: 100 }}>
            <label style={{ ...L, color: 'var(--danger)' }}>Desconto</label>
            <input style={{ ...MONO, color: 'var(--danger)' }} type="number" step="0.01" min="0" name="valor_desconto_geral" value={form.valor_desconto_geral} onChange={handleChange} />
          </div>
          <div style={{ width: 110 }}>
            <label style={{ ...L, color: 'var(--primary)', fontWeight: 700 }}>A PAGAR</label>
            <input style={{ ...RO, textAlign: 'right', fontFamily: 'Space Mono, monospace', fontWeight: 700, fontSize: '1.05rem', color: 'var(--primary)' }} value={valorAPagar.toFixed(2)} readOnly tabIndex={-1} />
          </div>
        </div>

        <button
          className="btn-primary"
          onClick={handleFinalizar}
          disabled={salvando || !editando}
          style={{ width: 'auto', padding: '12px 28px', fontSize: '0.9rem', whiteSpace: 'nowrap', height: 'fit-content' }}
        >
          {salvando ? 'Salvando...' : 'FINALIZAR (F1)'}
        </button>
      </div>

      {/* BUSCA */}
      <div style={{ display: 'flex', gap: 8, marginBottom: 8, alignItems: 'center', padding: '6px 0' }}>
        <span style={{ ...labelStyle, whiteSpace: 'nowrap' }}>Pesquisar:</span>
        <select style={{ ...inputStyle, width: 100 }} value={modoBusca} onChange={e => setModoBusca(e.target.value)}>
          <option value="numero">Numero...</option>
          <option value="nome">Passageiro</option>
          <option value="doc">Documento</option>
        </select>
        <input style={{ ...inputStyle, flex: 1 }} placeholder="Digite para buscar..." value={filtro} onChange={e => setFiltro(e.target.value)} />
        <button className="btn-primary" style={{ width: 'auto', padding: '6px 16px' }} onClick={() => {}}>BUSCAR</button>
      </div>

      {/* TABELA */}
      <div className="table-container" style={{ marginBottom: 8 }}>
        <table>
          <thead>
            <tr>
              <th style={{ width: 60 }}>Bilhete</th>
              <th>Passageiro</th>
              <th>Nascimento</th>
              <th>Documento</th>
              <th>Nacionalidade</th>
              <th>Origem</th>
              <th>Destino</th>
              <th>Total</th>
              <th>Desc.</th>
              <th>Pago</th>
              <th>A Pagar</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="12">Carregando...</td></tr>
            ) : passagensFiltradas.length === 0 ? (
              <tr><td colSpan="12" style={{ textAlign: 'center', padding: 20, color: 'var(--text-muted)' }}>Nenhuma passagem nesta viagem</td></tr>
            ) : passagensFiltradas.map(p => (
              <tr key={p.id_passagem}
                  className={`clickable ${selecionada?.id_passagem === p.id_passagem ? 'selected' : ''}`}
                  onClick={() => handleSelectRow(p)}>
                <td>{p.numero_bilhete}</td>
                <td>{p.nome_passageiro || '—'}</td>
                <td>{formatDate(p.data_nascimento)}</td>
                <td>{p.numero_doc || '—'}</td>
                <td>{p.nome_nacionalidade || '—'}</td>
                <td>{p.origem || '—'}</td>
                <td>{p.destino || '—'}</td>
                <td className="money">{formatMoney(p.valor_total)}</td>
                <td className="money">{formatMoney(p.valor_desconto_geral)}</td>
                <td className="money">{formatMoney(p.valor_pago)}</td>
                <td className="money">{formatMoney(p.valor_a_pagar || p.valor_total)}</td>
                <td>
                  <span className={`badge ${p.status_passagem === 'PAGO' ? 'success' : p.status_passagem === 'PARCIAL' ? 'warning' : 'danger'}`}>
                    {p.status_passagem || 'PENDENTE'}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* TOOLBAR INFERIOR */}
      <div style={{ display: 'flex', gap: 6, alignItems: 'center', padding: '8px 0', borderTop: '1px solid var(--border)', flexWrap: 'wrap' }}>
        <button className="btn-sm primary" onClick={handleEditar}>EDITAR (F3)</button>
        <button className="btn-sm danger" onClick={handleExcluir}>EXCLUIR (F4)</button>
        <button className="btn-sm" onClick={handleCancelar}>CANCELAR (F5)</button>
        <button className="btn-sm primary" onClick={() => { if (selecionada) printBilhete(selecionada, viagemAtiva) }}>BILHETE (F6)</button>
        <button className="btn-sm primary" onClick={() => printListaPassageiros(passagens, viagemAtiva)} style={{ fontWeight: 600 }}>LISTA DE PASSAGEIROS (F7)</button>
        <button className="btn-sm primary" onClick={() => showToast('Funcao disponivel em breve')}>RELATORIO (F8)</button>
        <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 8 }}>
          <span style={{ fontWeight: 600, color: 'var(--text-soft)' }}>Total:</span>
          <span style={{ fontWeight: 700, fontSize: '1.1rem', color: 'var(--text)' }}>{passagens.length}</span>
        </div>
      </div>

      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}

      {/* MODAL PAGAMENTO */}
      {modalPagar && (
        <ModalPagarPassagem
          passagem={modalPagar}
          caixas={caixas}
          onClose={() => setModalPagar(null)}
          onSuccess={() => { setModalPagar(null); carregarPassagens() }}
          showToast={showToast}
        />
      )}
    </div>
  )
}

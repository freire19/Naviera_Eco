import { useState, useRef, useCallback, useEffect } from 'react'
import { api } from '../api.js'
import Autocomplete from './Autocomplete.jsx'

const FORM_INICIAL = {
  // Passageiro
  id_passageiro: '',
  nome_passageiro: '',
  numero_doc: '',
  id_tipo_doc: '',
  id_nacionalidade: '',
  data_nascimento: '',
  id_sexo: '',
  // Viagem / Rota
  id_rota: '',
  id_acomodacao: '',
  id_tipo_passagem: '',
  id_agente: '',
  numero_requisicao: '',
  assento: '',
  // Tarifa (auto-preenchido)
  valor_alimentacao: '',
  valor_transporte: '',
  valor_cargas: '',
  valor_desconto_tarifa: '',
  valor_desconto_geral: '',
  // Observacoes
  observacoes: ''
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

export default function ModalCriarPassagem({
  viagemAtiva, rotas, acomodacoes, tiposPassagemAux, agentes,
  nacionalidades, tiposDocumento, sexos, caixas,
  onClose, onSuccess, showToast
}) {
  const [form, setForm] = useState(FORM_INICIAL)
  const [salvando, setSalvando] = useState(false)
  const [sugestoes, setSugestoes] = useState([])
  const [buscando, setBuscando] = useState(false)
  const [tarifaCarregando, setTarifaCarregando] = useState(false)
  const debounceRef = useRef(null)

  // Calculos automaticos
  const alimentacao = parseFloat(form.valor_alimentacao) || 0
  const transporte = parseFloat(form.valor_transporte) || 0
  const cargas = parseFloat(form.valor_cargas) || 0
  const descontoTarifa = parseFloat(form.valor_desconto_tarifa) || 0
  const descontoGeral = parseFloat(form.valor_desconto_geral) || 0
  const valorTotal = Math.round((alimentacao + transporte + cargas - descontoTarifa) * 100) / 100
  const valorAPagar = Math.round((valorTotal - descontoGeral) * 100) / 100
  const idade = calcIdade(form.data_nascimento)

  function handleFormChange(e) {
    const { name, value } = e.target
    setForm(prev => ({ ...prev, [name]: value }))
  }

  // Buscar tarifa quando rota ou tipo passagem mudar
  useEffect(() => {
    if (!form.id_rota || !form.id_tipo_passagem) return
    setTarifaCarregando(true)
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
      .finally(() => setTarifaCarregando(false))
  }, [form.id_rota, form.id_tipo_passagem])

  // Busca passageiro por nome
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

  async function handleCriar(e) {
    e.preventDefault()
    if (!form.nome_passageiro.trim()) {
      showToast('Informe o nome do passageiro', 'error')
      return
    }
    if (valorAPagar < 0) {
      showToast('Valor a pagar nao pode ser negativo', 'error')
      return
    }

    setSalvando(true)
    try {
      await api.post('/passagens', {
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
        valor_total: valorAPagar > 0 ? valorAPagar : valorTotal,
        valor_pago: 0,
        observacoes: form.observacoes || null
      })
      showToast('Passagem criada com sucesso')
      onSuccess()
    } catch (err) {
      showToast(err.message || 'Erro ao criar passagem', 'error')
    } finally {
      setSalvando(false)
    }
  }

  const sectionStyle = { marginBottom: 16 }
  const sectionTitleStyle = {
    fontSize: 13, fontWeight: 600, color: 'var(--text-muted)',
    textTransform: 'uppercase', letterSpacing: '0.5px',
    borderBottom: '1px solid var(--border)', paddingBottom: 4, marginBottom: 10
  }
  const readonlyStyle = { background: 'var(--bg-hover)', cursor: 'default' }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 720, maxHeight: '90vh', overflowY: 'auto' }}>
        <h3>Nova Passagem</h3>
        <form onSubmit={handleCriar}>

          {/* === PASSAGEIRO === */}
          <div style={sectionStyle}>
            <div style={sectionTitleStyle}>Dados do Passageiro</div>
            <div className="form-grid">
              <div className="form-group">
                <label>Nome do Passageiro *</label>
                <Autocomplete
                  value={form.nome_passageiro}
                  onChange={handleNomeChange}
                  onSelect={selecionarPassageiro}
                  suggestions={sugestoes}
                  loading={buscando}
                  placeholder="Digite para buscar..."
                  emptyMessage="Nenhum passageiro encontrado. Um novo sera criado."
                  renderItem={(s) => (
                    <>
                      <strong>{s.nome_passageiro}</strong>
                      {s.numero_documento && (
                        <span style={{ color: 'var(--text-muted)', marginLeft: 8, fontSize: 12 }}>
                          Doc: {s.numero_documento}
                        </span>
                      )}
                    </>
                  )}
                />
              </div>
              <div className="form-group">
                <label>Tipo Documento</label>
                <select name="id_tipo_doc" value={form.id_tipo_doc} onChange={handleFormChange}>
                  <option value="">Selecione...</option>
                  {tiposDocumento.map(t => (
                    <option key={t.id_tipo_doc} value={t.id_tipo_doc}>{t.nome_tipo_doc}</option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label>Documento</label>
                <input name="numero_doc" value={form.numero_doc} onChange={handleFormChange} placeholder="CPF / RG / Passaporte" />
              </div>
              <div className="form-group">
                <label>Nacionalidade</label>
                <select name="id_nacionalidade" value={form.id_nacionalidade} onChange={handleFormChange}>
                  <option value="">Selecione...</option>
                  {nacionalidades.map(n => (
                    <option key={n.id_nacionalidade} value={n.id_nacionalidade}>{n.nome_nacionalidade}</option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label>Data Nascimento</label>
                <input type="date" name="data_nascimento" value={form.data_nascimento} onChange={handleFormChange} />
              </div>
              <div className="form-group">
                <label>Idade</label>
                <input value={idade !== '' ? `${idade} anos` : ''} readOnly style={readonlyStyle} tabIndex={-1} />
              </div>
              <div className="form-group">
                <label>Sexo</label>
                <select name="id_sexo" value={form.id_sexo} onChange={handleFormChange}>
                  <option value="">Selecione...</option>
                  {sexos.map(s => (
                    <option key={s.id_sexo} value={s.id_sexo}>{s.nome_sexo}</option>
                  ))}
                </select>
              </div>
            </div>
          </div>

          {/* === VIAGEM / ROTA === */}
          <div style={sectionStyle}>
            <div style={sectionTitleStyle}>Dados da Viagem</div>
            <div className="form-grid">
              <div className="form-group">
                <label>Rota</label>
                <select name="id_rota" value={form.id_rota} onChange={handleFormChange}>
                  <option value="">Selecione...</option>
                  {rotas.map(r => (
                    <option key={r.id_rota} value={r.id_rota}>{r.origem} → {r.destino}</option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label>Acomodacao</label>
                <select name="id_acomodacao" value={form.id_acomodacao} onChange={handleFormChange}>
                  <option value="">Selecione...</option>
                  {acomodacoes.map(a => (
                    <option key={a.id_acomodacao} value={a.id_acomodacao}>{a.nome_acomodacao}</option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label>Tipo Passagem</label>
                <select name="id_tipo_passagem" value={form.id_tipo_passagem} onChange={handleFormChange}>
                  <option value="">Selecione...</option>
                  {tiposPassagemAux.map(t => (
                    <option key={t.id || t.id_tipo_passagem} value={t.id || t.id_tipo_passagem}>{t.nome || t.nome_tipo_passagem}</option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label>Agente</label>
                <select name="id_agente" value={form.id_agente} onChange={handleFormChange}>
                  <option value="">Selecione...</option>
                  {agentes.map(a => (
                    <option key={a.id_agente} value={a.id_agente}>{a.nome_agente}</option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label>Requisicao</label>
                <input name="numero_requisicao" value={form.numero_requisicao} onChange={handleFormChange} placeholder="Numero da requisicao" />
              </div>
              <div className="form-group">
                <label>Assento</label>
                <input name="assento" value={form.assento} onChange={handleFormChange} placeholder="Ex: A12" />
              </div>
            </div>
          </div>

          {/* === TARIFA / VALORES === */}
          <div style={sectionStyle}>
            <div style={sectionTitleStyle}>
              Valores {tarifaCarregando && <span style={{ fontSize: 11, fontWeight: 400, color: 'var(--primary)' }}> (buscando tarifa...)</span>}
            </div>
            <div className="form-grid">
              <div className="form-group">
                <label>Alimentacao</label>
                <input name="valor_alimentacao" type="number" step="0.01" min="0" value={form.valor_alimentacao} onChange={handleFormChange} placeholder="0.00" />
              </div>
              <div className="form-group">
                <label>Transporte</label>
                <input name="valor_transporte" type="number" step="0.01" min="0" value={form.valor_transporte} onChange={handleFormChange} placeholder="0.00" />
              </div>
              <div className="form-group">
                <label>Cargas</label>
                <input name="valor_cargas" type="number" step="0.01" min="0" value={form.valor_cargas} onChange={handleFormChange} placeholder="0.00" />
              </div>
              <div className="form-group">
                <label>Desconto Tarifa</label>
                <input name="valor_desconto_tarifa" type="number" step="0.01" min="0" value={form.valor_desconto_tarifa} onChange={handleFormChange} placeholder="0.00" />
              </div>
              <div className="form-group">
                <label>Valor Total</label>
                <input value={valorTotal.toFixed(2)} readOnly style={readonlyStyle} tabIndex={-1} />
              </div>
              <div className="form-group">
                <label>Desconto Geral</label>
                <input name="valor_desconto_geral" type="number" step="0.01" min="0" value={form.valor_desconto_geral} onChange={handleFormChange} placeholder="0.00" />
              </div>
              <div className="form-group">
                <label>Valor a Pagar</label>
                <input
                  value={valorAPagar.toFixed(2)}
                  readOnly
                  style={{ ...readonlyStyle, fontWeight: 700, fontSize: 16, color: 'var(--primary)' }}
                  tabIndex={-1}
                />
              </div>
            </div>
          </div>

          {/* === OBSERVACOES === */}
          <div style={sectionStyle}>
            <div className="form-group full-width">
              <label>Observacoes</label>
              <textarea name="observacoes" value={form.observacoes} onChange={handleFormChange} rows={2} placeholder="Observacoes opcionais..." />
            </div>
          </div>

          <div className="modal-actions">
            <button type="button" className="btn-secondary" onClick={onClose} disabled={salvando}>
              Cancelar
            </button>
            <button type="submit" className="btn-primary" disabled={salvando}>
              {salvando ? 'Salvando...' : 'Salvar Passagem'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

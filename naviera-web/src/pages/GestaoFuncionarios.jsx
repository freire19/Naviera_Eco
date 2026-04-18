import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

const FORM_INICIAL = {
  nome: '', cpf: '', rg: '', ctps: '', telefone: '', endereco: '',
  cargo: '', salario: '', data_admissao: '', data_nascimento: '',
  data_inicio_calculo: '', valor_inss: '', descontar_inss: false,
  is_clt: false, recebe_decimo_terceiro: false
}

const fmtMoney = v => v != null ? new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(v) : 'R$ 0,00'
const fmtDate = d => {
  if (!d) return '-'
  const s = String(d).includes('T') ? d.substring(0, 10) : String(d)
  try { const p = s.split('-'); return p.length === 3 ? `${p[2]}/${p[1]}/${p[0]}` : s } catch { return s }
}

const MESES_NOME = ['Janeiro','Fevereiro','Marco','Abril','Maio','Junho','Julho','Agosto','Setembro','Outubro','Novembro','Dezembro']

function gerarListaMeses() {
  const lista = []
  const hoje = new Date()
  let d = new Date(hoje.getFullYear(), hoje.getMonth() + 1, 1)
  for (let i = 0; i < 15; i++) {
    lista.push({ mes: d.getMonth() + 1, ano: d.getFullYear(), label: `${MESES_NOME[d.getMonth()].toUpperCase()}/${d.getFullYear()}` })
    d = new Date(d.getFullYear(), d.getMonth() - 1, 1)
  }
  return lista
}

export default function GestaoFuncionarios({ viagemAtiva, onNavigate }) {
  const [funcionarios, setFuncionarios] = useState([])
  const [loading, setLoading] = useState(false)
  const [selecionado, setSelecionado] = useState(null)
  const [form, setForm] = useState(FORM_INICIAL)
  const [salvando, setSalvando] = useState(false)
  const [toast, setToast] = useState(null)
  const [abaAtiva, setAbaAtiva] = useState('dados')
  const [mostrarInativos, setMostrarInativos] = useState(false)
  const [empresaNome, setEmpresaNome] = useState('')

  // Financeiro
  const [financeiro, setFinanceiro] = useState(null)
  const [historico, setHistorico] = useState([])
  const [mesesLista] = useState(gerarListaMeses)
  const [mesSel, setMesSel] = useState(() => { const h = new Date(); return `${h.getMonth() + 1}-${h.getFullYear()}` })

  // Lancamento pagamento
  const [descPgto, setDescPgto] = useState('')
  const [valorPgto, setValorPgto] = useState('')

  // Modal falta
  const [modalFalta, setModalFalta] = useState(false)
  const [dataFalta, setDataFalta] = useState(() => new Date().toISOString().split('T')[0])

  // Modal fechar mes
  const [modalFechar, setModalFechar] = useState(false)

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  const carregar = useCallback(() => {
    setLoading(true)
    api.get(`/cadastros/funcionarios?incluir_inativos=${mostrarInativos}`)
      .then(data => setFuncionarios(data))
      .catch(() => showToast('Erro ao carregar funcionarios', 'error'))
      .finally(() => setLoading(false))
  }, [mostrarInativos])

  useEffect(() => { carregar() }, [carregar])

  useEffect(() => {
    api.get('/cadastros/empresa').then(e => setEmpresaNome(e?.nome || e?.razao_social || '')).catch(() => {})
  }, [])

  const carregarFinanceiro = useCallback((id) => {
    if (!id) return
    api.get(`/cadastros/funcionarios/${id}/financeiro`).then(setFinanceiro).catch(() => setFinanceiro(null))
  }, [])

  const carregarHistorico = useCallback((id, mesAno) => {
    if (!id) return
    const [m, a] = (mesAno || mesSel).split('-')
    api.get(`/cadastros/funcionarios/${id}/historico?mes=${m}&ano=${a}`).then(setHistorico).catch(() => setHistorico([]))
  }, [mesSel])

  function selecionarFuncionario(f) {
    setSelecionado(f)
    setForm({
      nome: f.nome || '', cpf: f.cpf || '', rg: f.rg || '',
      ctps: f.ctps || '', telefone: f.telefone || '', endereco: f.endereco || '',
      cargo: f.cargo || '', salario: f.salario || '',
      data_admissao: f.data_admissao ? String(f.data_admissao).slice(0, 10) : '',
      data_nascimento: f.data_nascimento ? String(f.data_nascimento).slice(0, 10) : '',
      data_inicio_calculo: f.data_inicio_calculo ? String(f.data_inicio_calculo).slice(0, 10) : '',
      valor_inss: f.valor_inss || '', descontar_inss: f.descontar_inss || false,
      is_clt: f.is_clt || false, recebe_decimo_terceiro: f.recebe_decimo_terceiro || false
    })
    carregarFinanceiro(f.id)
    carregarHistorico(f.id)
  }

  function novoFuncionario() {
    setSelecionado(null)
    setForm({ ...FORM_INICIAL, data_admissao: new Date().toISOString().split('T')[0] })
    setFinanceiro(null)
    setHistorico([])
    setAbaAtiva('dados')
  }

  function handleChange(e) {
    const { name, value, type, checked } = e.target
    setForm(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }))
  }

  async function handleSalvar(e) {
    e.preventDefault()
    if (!form.nome.trim() || !form.cargo.trim() || !form.salario || !form.data_admissao) {
      showToast('Preencha: Nome, Cargo, Salario e Data de Admissao.', 'error'); return
    }
    setSalvando(true)
    try {
      if (selecionado) {
        const updated = await api.put(`/cadastros/funcionarios/${selecionado.id}`, form)
        showToast('Salvo com sucesso!')
        carregar()
        selecionarFuncionario(updated)
      } else {
        const created = await api.post('/cadastros/funcionarios', form)
        showToast('Salvo com sucesso!')
        carregar()
        selecionarFuncionario(created)
      }
    } catch (err) {
      showToast(err.message || 'Erro ao salvar', 'error')
    } finally { setSalvando(false) }
  }

  async function handleDemitir() {
    if (!selecionado) return
    if (!confirm(`Deseja realmente demitir ${selecionado.nome}?`)) return
    try {
      await api.post(`/cadastros/funcionarios/${selecionado.id}/demitir`)
      showToast('Funcionario demitido com sucesso.')
      setSelecionado(null); setForm(FORM_INICIAL); carregar()
    } catch { showToast('Erro ao demitir', 'error') }
  }

  async function handleLancarPagamento() {
    if (!selecionado) return
    if (!descPgto.trim()) { showToast('Digite uma descricao.', 'error'); return }
    if (!valorPgto || parseFloat(valorPgto) <= 0) { showToast('Valor invalido.', 'error'); return }
    try {
      const desc = `PAGTO ${selecionado.nome.toUpperCase()} - ${descPgto.toUpperCase()}`
      await api.post(`/cadastros/funcionarios/${selecionado.id}/pagamento`, { descricao: desc, valor: parseFloat(valorPgto) })
      showToast('Pagamento lancado!')
      setDescPgto(''); setValorPgto('')
      carregarFinanceiro(selecionado.id); carregarHistorico(selecionado.id)
    } catch (err) { showToast(err.message || 'Erro', 'error') }
  }

  async function handleRegistrarFalta() {
    if (!selecionado) return
    try {
      const res = await api.post(`/cadastros/funcionarios/${selecionado.id}/falta`, { data_falta: dataFalta })
      showToast(`Falta registrada no prontuario! Valor: ${fmtMoney(res.valor)}`)
      setModalFalta(false)
      carregarFinanceiro(selecionado.id); carregarHistorico(selecionado.id)
    } catch (err) { showToast(err.error || err.message || 'Erro', 'error') }
  }

  async function handleFecharMes() {
    if (!selecionado) return
    try {
      const res = await api.post(`/cadastros/funcionarios/${selecionado.id}/fechar-mes`)
      let msg = `Ciclo Fechado! Novo ciclo iniciado em: ${fmtDate(res.nova_data_inicio)}`
      showToast(msg)
      setModalFechar(false); carregar()
      api.get(`/cadastros/funcionarios?incluir_inativos=${mostrarInativos}`).then(data => {
        const updated = data.find(f => f.id === selecionado.id)
        if (updated) selecionarFuncionario(updated)
      })
    } catch (err) { showToast(err.message || 'Erro ao fechar mes', 'error') }
  }

  function handleMesChange(e) {
    setMesSel(e.target.value)
    if (selecionado) carregarHistorico(selecionado.id, e.target.value)
  }

  function imprimirHolerite() {
    if (!selecionado) { showToast('Selecione um funcionario.', 'error'); return }
    const mesLabel = mesesLista.find(m => `${m.mes}-${m.ano}` === mesSel)?.label || ''
    const salarioBase = parseFloat(selecionado.salario) || 0

    const linhas = historico.map(h => {
      let desc = h.descricao || ''
      desc = desc.replace(selecionado.nome, '').replace((selecionado.nome || '').toUpperCase(), '')
        .replace(' - - ', ' - ').trim()
      if (desc.startsWith('- ')) desc = desc.substring(2)
      if (desc.length > 32) desc = desc.substring(0, 32) + '...'
      return { data: fmtDate(h.data), desc, valor: h.valor }
    })

    const totalVencimentos = salarioBase
    const totalDescontos = historico.reduce((s, h) => s + (parseFloat(h.valor) || 0), 0)
    const liquido = totalVencimentos - totalDescontos

    const linhasHtml = linhas.map(l => `
      <div class="row">
        <span class="c-data">${l.data}</span>
        <span class="c-desc">${l.desc}</span>
        <span class="c-ref"></span>
        <span class="c-venc"></span>
        <span class="c-descv">${fmtMoney(l.valor)}</span>
      </div>`).join('')

    const viaHtml = (titulo) => `
      <div class="holerite">
        <div class="header">
          <div class="top-row">
            <span class="empresa">${empresaNome || ''}</span>
            <span class="via">${titulo}</span>
          </div>
          <div class="titulo">RECIBO DE PAGAMENTO DE SALARIO</div>
        </div>
        <div class="dados">
          <div><b>FUNCIONARIO:</b> ${selecionado.nome || ''}</div>
          <div><b>COMPETENCIA:</b> <b>${mesLabel}</b></div>
          <div><b>CARGO:</b> ${selecionado.cargo || ''}</div>
          <div></div>
        </div>
        <div class="tabela-header row">
          <span class="c-data">DATA</span>
          <span class="c-desc">DESCRICAO</span>
          <span class="c-ref">REF.</span>
          <span class="c-venc">VENC.</span>
          <span class="c-descv">DESC.</span>
        </div>
        <div class="row">
          <span class="c-data"></span>
          <span class="c-desc">SALARIO BASE MENSAL</span>
          <span class="c-ref">30d</span>
          <span class="c-venc">${fmtMoney(salarioBase)}</span>
          <span class="c-descv"></span>
        </div>
        ${linhasHtml}
        <div class="totais">
          <div class="bloco">
            <div class="lbl">TOT. VENCIMENTOS</div>
            <div class="val">${fmtMoney(totalVencimentos)}</div>
          </div>
          <div class="bloco">
            <div class="lbl">TOT. DESCONTOS</div>
            <div class="val vermelho">${fmtMoney(totalDescontos)}</div>
          </div>
          <div class="bloco liquido">
            <div class="lbl">LIQUIDO A RECEBER</div>
            <div class="val-liq">${fmtMoney(liquido)}</div>
          </div>
        </div>
        <div class="assinatura">
          <div class="linha-ass">__________________________________________</div>
          <div class="lbl-ass">ASSINATURA DO FUNCIONARIO</div>
        </div>
      </div>`

    const html = `<!DOCTYPE html>
<html><head><meta charset="utf-8"><title>Holerite - ${selecionado.nome}</title>
<style>
  * { box-sizing: border-box; }
  body { font-family: Arial, sans-serif; margin: 0; padding: 10px; background: #fff; color: #000; }
  .holerite { width: 500px; margin: 0 auto; border: 1px solid #ccc; background: #fff; }
  .header { background: #059669; color: #fff; padding: 5px 8px; }
  .top-row { display: flex; justify-content: space-between; align-items: center; }
  .empresa { font-weight: bold; font-size: 11px; }
  .via { font-size: 9px; }
  .titulo { font-weight: bold; font-size: 12px; margin-top: 2px; }
  .dados { display: grid; grid-template-columns: 1fr 1fr; gap: 4px 15px; padding: 6px 8px; border-bottom: 1px solid #eee; font-size: 10px; }
  .tabela-header { background: #E6F5ED; border-bottom: 1px solid #A7F3D0; font-weight: bold; }
  .row { display: flex; padding: 2px 5px; font-size: 10px; }
  .c-data { width: 60px; }
  .c-desc { width: 240px; }
  .c-ref { width: 30px; text-align: center; }
  .c-venc { width: 75px; text-align: right; }
  .c-descv { width: 75px; text-align: right; }
  .totais { display: flex; background: #f5f5f5; padding: 5px 8px; border-top: 1px solid #e0e0e0; gap: 20px; }
  .bloco { display: flex; flex-direction: column; font-size: 10px; }
  .bloco .lbl { font-weight: bold; }
  .bloco .val { font-size: 11px; }
  .bloco.liquido { margin-left: auto; border: 1px solid #059669; padding: 5px; text-align: right; min-width: 130px; }
  .bloco.liquido .lbl { color: #059669; font-weight: bold; font-size: 9px; }
  .bloco.liquido .val-liq { font-weight: bold; font-size: 14px; }
  .vermelho { color: #DC2626; }
  .assinatura { text-align: center; padding: 15px 0 5px; }
  .lbl-ass { font-weight: bold; font-size: 9px; }
  .linha-corte { display: flex; align-items: center; justify-content: center; gap: 10px; padding: 10px 0; font-size: 10px; color: gray; }
  .linha-corte .pontos { flex: 1; border-top: 1px dashed #999; max-width: 480px; }
  @media print {
    body { padding: 0; }
    @page { size: auto; margin: 10mm; }
  }
</style>
</head>
<body>
  ${viaHtml('VIA DO EMPREGADOR')}
  <div class="linha-corte"><span>&#9986; CORTE</span><span class="pontos"></span></div>
  ${viaHtml('VIA DO FUNCIONARIO')}
  <script>window.onload = function() { window.print(); }<\/script>
</body></html>`

    const w = window.open('', '_blank', 'width=700,height=800')
    if (!w) { showToast('Permita popups para imprimir.', 'error'); return }
    w.document.write(html)
    w.document.close()
  }

  // ---- INLINE STYLES ----
  const L = { fontSize: '0.82rem', fontWeight: 700, color: '#424242', display: 'block', marginBottom: 2 }
  const I = { width: '100%', padding: '9px 10px', border: '1px solid #c8c8c8', borderRadius: 4, fontSize: '0.88rem', boxSizing: 'border-box', background: '#fff' }
  const Ival = { ...I, background: '#FEF3C7', fontWeight: 700 }

  return (
    <div style={{ display: 'flex', gap: 0, height: 'calc(100vh - 100px)', minHeight: 500 }}>
      {/* ========= SIDEBAR ESQUERDA ========= */}
      <div style={{ width: 300, minWidth: 260, borderRight: '1px solid #e0e0e0', padding: 15, display: 'flex', flexDirection: 'column', gap: 10 }}>
        <button onClick={novoFuncionario} style={{
          width: '100%', padding: '14px', background: '#059669', color: '#fff', border: 'none',
          borderRadius: 6, fontWeight: 700, fontSize: '1rem', cursor: 'pointer'
        }}>+ NOVO FUNCIONARIO</button>

        <div style={{ fontSize: '0.88rem', color: '#757575', marginTop: 8 }}>Selecione um funcionario:</div>

        <div style={{ flex: 1, overflow: 'auto', border: '1px solid #e0e0e0', borderRadius: 4 }}>
          {loading ? <div style={{ padding: 12, color: '#999' }}>Carregando...</div> :
            funcionarios.length === 0 ? <div style={{ padding: 12, color: '#999' }}>Nenhum funcionario</div> :
            funcionarios.map(f => (
              <div key={f.id} onClick={() => selecionarFuncionario(f)} style={{
                padding: '10px 12px', cursor: 'pointer', borderBottom: '1px solid #f0f0f0',
                background: selecionado?.id === f.id ? '#E6F5ED' : 'transparent',
                opacity: f.ativo ? 1 : 0.5, fontSize: '0.88rem'
              }}>
                <div style={{ fontWeight: 600 }}>{f.nome}</div>
                <div style={{ fontSize: '0.72rem', color: '#888' }}>{f.cargo || ''}{!f.ativo ? ' (INATIVO)' : ''}</div>
              </div>
            ))
          }
        </div>

        <label style={{ fontSize: '0.82rem', display: 'flex', alignItems: 'center', gap: 6, marginBottom: 5 }}>
          <input type="checkbox" checked={mostrarInativos} onChange={e => setMostrarInativos(e.target.checked)} />
          Exibir Demitidos / Inativos
        </label>
      </div>

      {/* ========= AREA PRINCIPAL ========= */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
        {/* TABS */}
        <div style={{ display: 'flex', borderBottom: '1px solid #e0e0e0' }}>
          <button onClick={() => setAbaAtiva('dados')} style={{
            padding: '14px 28px', border: 'none', borderBottom: abaAtiva === 'dados' ? '3px solid #059669' : '3px solid transparent',
            background: abaAtiva === 'dados' ? '#fff' : '#f5f5f5', color: abaAtiva === 'dados' ? '#1a1a1a' : '#666',
            fontWeight: 700, fontSize: '0.88rem', cursor: 'pointer'
          }}>DADOS PESSOAIS E CONTRATO</button>
          <button onClick={() => setAbaAtiva('financeiro')} style={{
            padding: '14px 28px', border: 'none', borderBottom: abaAtiva === 'financeiro' ? '3px solid #059669' : '3px solid transparent',
            background: abaAtiva === 'financeiro' ? '#fff' : '#f5f5f5', color: abaAtiva === 'financeiro' ? '#1a1a1a' : '#666',
            fontWeight: 700, fontSize: '0.88rem', cursor: 'pointer'
          }}>FINANCEIRO & PAGAMENTOS</button>
        </div>

        {/* ====== ABA 1: DADOS PESSOAIS E CONTRATO ====== */}
        {abaAtiva === 'dados' && (
          <div style={{ flex: 1, overflow: 'auto', padding: '30px 40px' }}>
            <form onSubmit={handleSalvar}>
              {/* Dados Pessoais */}
              <div style={{ marginBottom: 25 }}>
                <div style={{ fontSize: '1.2rem', fontWeight: 700, marginBottom: 15, color: '#333' }}>Dados Pessoais</div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '25px 25px' }}>
                  <div>
                    <label style={L}>Nome Completo *</label>
                    <input style={I} name="nome" value={form.nome} onChange={handleChange} />
                  </div>
                  <div>
                    <label style={L}>CPF</label>
                    <input style={I} name="cpf" value={form.cpf} onChange={handleChange} />
                  </div>
                  <div>
                    <label style={L}>RG</label>
                    <input style={I} name="rg" value={form.rg} onChange={handleChange} />
                  </div>
                  <div>
                    <label style={L}>Data Nascimento</label>
                    <input style={I} type="date" name="data_nascimento" value={form.data_nascimento} onChange={handleChange} />
                  </div>
                  <div>
                    <label style={L}>CTPS</label>
                    <input style={I} name="ctps" value={form.ctps} onChange={handleChange} />
                  </div>
                  <div>
                    <label style={L}>Telefone / WhatsApp</label>
                    <input style={I} name="telefone" value={form.telefone} onChange={handleChange} />
                  </div>
                  <div style={{ gridColumn: '1 / -1' }}>
                    <label style={L}>Endereco Completo</label>
                    <input style={I} name="endereco" value={form.endereco} onChange={handleChange} />
                  </div>
                </div>
              </div>

              <hr style={{ border: 'none', borderTop: '1px solid #e0e0e0', margin: '20px 0' }} />

              {/* Dados do Contrato */}
              <div style={{ marginBottom: 20 }}>
                <div style={{ fontSize: '1.2rem', fontWeight: 700, marginBottom: 15, color: '#333' }}>Dados do Contrato (CLT / INSS)</div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '25px 25px' }}>
                  <div>
                    <label style={L}>Cargo *</label>
                    <input style={I} name="cargo" value={form.cargo} onChange={handleChange} />
                  </div>
                  <div>
                    <label style={L}>Salario Base (R$) *</label>
                    <input style={Ival} type="number" step="0.01" name="salario" value={form.salario} onChange={handleChange} />
                  </div>
                  <div>
                    <label style={L}>Data de Admissao *</label>
                    <input style={I} type="date" name="data_admissao" value={form.data_admissao} onChange={handleChange} />
                  </div>
                  <div>
                    <label style={L}>Valor INSS/Folha (R$)</label>
                    <input style={I} type="number" step="0.01" name="valor_inss" placeholder="0,00" value={form.valor_inss} onChange={handleChange} />
                  </div>
                  <div>
                    <label style={{ ...L, color: '#757575' }}>Data de Reinicio (Opcional)</label>
                    <input style={I} type="date" name="data_inicio_calculo" placeholder="Data de corte" value={form.data_inicio_calculo} onChange={handleChange} />
                  </div>
                  <div />
                </div>

                {/* Checkboxes row */}
                <div style={{
                  display: 'flex', alignItems: 'center', gap: 30, padding: '12px 16px', marginTop: 20,
                  border: '1px solid #e0e0e0', borderRadius: 5
                }}>
                  <label style={{ fontSize: '0.85rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: 6 }}>
                    <input type="checkbox" name="descontar_inss" checked={form.descontar_inss} onChange={handleChange} /> Descontar INSS Auto?
                  </label>
                  <label style={{ fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: 6 }}>
                    <input type="checkbox" name="is_clt" checked={form.is_clt} onChange={handleChange} /> Carteira Assinada (CLT)
                  </label>
                  <label style={{ fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: 6 }}>
                    <input type="checkbox" name="recebe_decimo_terceiro" checked={form.recebe_decimo_terceiro} onChange={handleChange} /> Recebe 13o Salario
                  </label>
                </div>

                {/* Status */}
                {selecionado && (
                  <div style={{
                    fontSize: '1.1rem', fontWeight: 700, marginTop: 16,
                    color: selecionado.ativo ? '#059669' : '#DC2626'
                  }}>
                    {selecionado.ativo ? 'ATIVO' : 'DEMITIDO / INATIVO'}
                  </div>
                )}
              </div>

              {/* Botoes */}
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 12, marginTop: 10 }}>
                {selecionado && selecionado.ativo && (
                  <button type="button" onClick={handleDemitir} style={{
                    padding: '12px 24px', background: '#DC2626', color: '#fff', border: 'none',
                    borderRadius: 4, fontWeight: 700, fontSize: '0.95rem', cursor: 'pointer'
                  }}>Demissao</button>
                )}
                <button type="submit" disabled={salvando} style={{
                  padding: '12px 30px', background: '#059669', color: '#fff', border: 'none',
                  borderRadius: 4, fontWeight: 700, fontSize: '1rem', cursor: 'pointer', minWidth: 180
                }}>{salvando ? 'Salvando...' : 'SALVAR DADOS'}</button>
              </div>
            </form>
          </div>
        )}

        {/* ====== ABA 2: FINANCEIRO & PAGAMENTOS ====== */}
        {abaAtiva === 'financeiro' && (
          <div style={{ flex: 1, overflow: 'auto', padding: '20px 20px' }}>
            {!selecionado ? (
              <div style={{ textAlign: 'center', padding: 60, color: '#999', fontSize: '1rem' }}>
                Selecione um funcionario na lista para ver o financeiro.
              </div>
            ) : (
              <>
                {/* Cards resumo — 4 em linha */}
                <div style={{ display: 'flex', gap: 15, marginBottom: 16 }}>
                  <div style={{ flex: 1, textAlign: 'center', padding: '14px 10px', border: '1px solid #eee', borderRadius: 5, background: '#fff' }}>
                    <div style={{ fontSize: '0.78rem', fontWeight: 700, color: '#757575' }}>Base de Calculo</div>
                    <div style={{ fontSize: '1.25rem', fontWeight: 700 }}>{financeiro?.dias_trabalhados ?? 0} dias</div>
                  </div>
                  <div style={{ flex: 1, textAlign: 'center', padding: '14px 10px', border: '1px solid #eee', borderRadius: 5, background: '#fff' }}>
                    <div style={{ fontSize: '0.78rem', fontWeight: 700, color: '#757575' }}>Salario Acumulado</div>
                    <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#059669' }}>{fmtMoney(financeiro?.acumulado)}</div>
                  </div>
                  <div style={{ flex: 1, textAlign: 'center', padding: '14px 10px', border: '1px solid #eee', borderRadius: 5, background: '#fff' }}>
                    <div style={{ fontSize: '0.78rem', fontWeight: 700, color: '#757575' }}>Total Pago</div>
                    <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#c62828' }}>{fmtMoney(financeiro?.pago)}</div>
                  </div>
                  <div style={{ flex: 1.2, textAlign: 'center', padding: '14px 10px', border: '1px solid #eee', borderRadius: 5, background: '#fff' }}>
                    <div style={{ fontSize: '0.85rem', fontWeight: 700, color: '#757575' }}>Saldo a Receber</div>
                    <div style={{
                      fontSize: '1.7rem', fontWeight: 700,
                      color: (financeiro?.saldo ?? 0) >= 0 ? '#059669' : '#DC2626'
                    }}>{fmtMoney(financeiro?.saldo)}</div>
                  </div>
                </div>

                {/* Linha info referencia */}
                <div style={{ display: 'flex', alignItems: 'center', gap: 15, marginBottom: 16, fontSize: '0.85rem' }}>
                  <span style={{ color: '#757575' }}>Mes Referencia (Calculo):</span>
                  <span style={{ fontWeight: 700, fontSize: '1rem' }}>
                    {financeiro?.data_inicio ? fmtDate(financeiro.data_inicio) : 'Mes Atual'}
                    {financeiro?.ciclo_atual ? ' (CICLO ATUAL)' : financeiro?.data_inicio ? ' (EM ABERTO)' : ''}
                  </span>
                  <div style={{ flex: 1 }} />
                  <span style={{ fontWeight: 700, color: '#555' }}>Valor Diaria (30d): {fmtMoney(financeiro?.valor_diaria)}</span>
                  <div style={{ flex: 1 }} />
                  <span style={{ fontWeight: 700, color: '#555' }}>13o Acumulado: {fmtMoney(financeiro?.provisao_13)}</span>
                </div>

                {/* Titulo lancamento */}
                <div style={{ fontSize: '1rem', fontWeight: 700, color: '#333', marginBottom: 10 }}>
                  Lancar Novo Pagamento / Vale / Acoes
                </div>

                {/* Linha unica: Descricao | Valor | LANCAR | REGISTRAR FALTA | FECHAR CICLO */}
                <div style={{ display: 'flex', gap: 10, alignItems: 'center', marginBottom: 16 }}>
                  <input style={{ ...I, flex: 2 }} placeholder="Descricao (Ex: Vale Supermercado)" value={descPgto} onChange={e => setDescPgto(e.target.value)} />
                  <input style={{ ...Ival, width: 130, flex: 'none' }} type="number" step="0.01" placeholder="R$ Valor" value={valorPgto} onChange={e => setValorPgto(e.target.value)} />
                  <button type="button" onClick={handleLancarPagamento} style={{
                    padding: '10px 18px', background: '#059669', color: '#fff', border: 'none',
                    borderRadius: 4, fontWeight: 700, fontSize: '0.85rem', cursor: 'pointer', whiteSpace: 'nowrap',
                    boxShadow: '0 2px 4px rgba(0,0,0,0.2)'
                  }}>LANCAR (+)</button>
                  <div style={{ width: 1, height: 30, background: '#ddd' }} />
                  <button type="button" onClick={() => setModalFalta(true)} style={{
                    padding: '10px 18px', background: '#DC2626', color: '#fff', border: 'none',
                    borderRadius: 4, fontWeight: 700, fontSize: '0.85rem', cursor: 'pointer', whiteSpace: 'nowrap'
                  }}>REGISTRAR FALTA</button>
                  <div style={{ width: 1, height: 30, background: '#ddd' }} />
                  <button type="button" onClick={() => setModalFechar(true)} style={{
                    padding: '10px 18px', background: '#059669', color: '#fff', border: 'none',
                    borderRadius: 4, fontWeight: 700, fontSize: '0.85rem', cursor: 'pointer', whiteSpace: 'nowrap'
                  }}>FECHAR CICLO / MES</button>
                </div>

                <hr style={{ border: 'none', borderTop: '1px solid #e0e0e0', margin: '10px 0 14px' }} />

                {/* Historico header */}
                <div style={{ display: 'flex', alignItems: 'center', gap: 15, marginBottom: 10 }}>
                  <span style={{ fontSize: '1.05rem', fontWeight: 700, color: '#333' }}>Historico Detalhado</span>
                  <span style={{ fontSize: '0.88rem', color: '#666' }}>| Selecionar Competencia:</span>
                  <select value={mesSel} onChange={handleMesChange} style={{
                    padding: '7px 10px', fontWeight: 700, fontSize: '0.85rem', borderRadius: 4,
                    border: '1px solid #c8c8c8', background: '#047857', color: '#fff', cursor: 'pointer'
                  }}>
                    {mesesLista.map(m => (
                      <option key={`${m.mes}-${m.ano}`} value={`${m.mes}-${m.ano}`}>{m.label}</option>
                    ))}
                  </select>
                  <div style={{ flex: 1 }} />
                  <button type="button" onClick={imprimirHolerite} style={{
                    padding: '8px 16px', background: '#fff', color: '#333', border: '1px solid #c8c8c8',
                    borderRadius: 4, fontWeight: 700, fontSize: '0.82rem', cursor: 'pointer',
                    display: 'flex', alignItems: 'center', gap: 6
                  }}>&#128424; IMPRIMIR HOLERITE</button>
                </div>

                {/* Tabela historico */}
                <div style={{ border: '1px solid #e0e0e0', borderRadius: 4, overflow: 'hidden' }}>
                  <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.88rem' }}>
                    <thead>
                      <tr style={{ background: '#047857', color: '#fff' }}>
                        <th style={{ padding: '8px 12px', textAlign: 'left', width: '15%' }}>Data</th>
                        <th style={{ padding: '8px 12px', textAlign: 'left' }}>Descricao do Lancamento</th>
                        <th style={{ padding: '8px 12px', textAlign: 'right', width: '18%' }}>Valor (R$)</th>
                      </tr>
                    </thead>
                    <tbody>
                      {historico.length === 0 ? (
                        <tr><td colSpan="3" style={{ padding: 40, textAlign: 'center', color: '#999', fontWeight: 700 }}>
                          Nao ha conteudo na tabela
                        </td></tr>
                      ) : historico.map((h, i) => {
                        const isDesc = h.tipo === 'DESCONTO'
                        let desc = h.descricao || ''
                        if (selecionado) {
                          desc = desc.replace(selecionado.nome, '').replace((selecionado.nome || '').toUpperCase(), '')
                            .replace(' - - ', ' - ').trim()
                          if (desc.startsWith('- ')) desc = desc.substring(2)
                        }
                        return (
                          <tr key={i} style={{ borderBottom: '1px solid #f0f0f0' }}>
                            <td style={{ padding: '6px 12px' }}>{fmtDate(h.data)}</td>
                            <td style={{ padding: '6px 12px' }}>{desc}</td>
                            <td style={{ padding: '6px 12px', textAlign: 'right', fontWeight: 700, color: isDesc ? '#DC2626' : '#059669' }}>
                              {isDesc ? '(-) ' : ''}{fmtMoney(h.valor)}
                            </td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                </div>

                {/* Botao demissao (baixo da tabela, so aparece com funcionario selecionado ativo) */}
                {selecionado && selecionado.ativo && (
                  <div style={{ marginTop: 14 }}>
                    <button type="button" onClick={handleDemitir} style={{
                      padding: '10px 20px', background: '#DC2626', color: '#fff', border: 'none',
                      borderRadius: 4, fontWeight: 700, cursor: 'pointer', fontSize: '0.85rem'
                    }}>Demissao</button>
                  </div>
                )}
              </>
            )}
          </div>
        )}
      </div>

      {/* ==== MODAL FALTA ==== */}
      {modalFalta && (
        <div className="modal-overlay" onClick={() => setModalFalta(false)}>
          <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 380 }}>
            <h3>Registrar Falta</h3>
            <p style={{ fontSize: '0.85rem', color: '#666' }}>Lancar Falta no RH</p>
            <div style={{ marginBottom: 12 }}>
              <label style={L}>Data da Falta:</label>
              <input style={I} type="date" value={dataFalta} onChange={e => setDataFalta(e.target.value)} />
            </div>
            <div className="modal-actions">
              <button type="button" className="btn-secondary" onClick={() => setModalFalta(false)}>Cancelar</button>
              <button type="button" className="btn-primary" onClick={handleRegistrarFalta}>OK</button>
            </div>
          </div>
        </div>
      )}

      {/* ==== MODAL FECHAR MES ==== */}
      {modalFechar && (
        <div className="modal-overlay" onClick={() => setModalFechar(false)}>
          <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 480 }}>
            <h3>Fechar Ciclo</h3>
            <p style={{ fontSize: '0.88rem', fontWeight: 600, marginBottom: 10 }}>
              Encerrar competencia e iniciar novo mes?
            </p>
            <div style={{ background: '#f9f9f9', padding: 14, borderRadius: 6, fontSize: '0.88rem', marginBottom: 14 }}>
              <div>Resumo do Fechamento:</div>
              <div style={{ marginTop: 6 }}>Acumulado Bruto: <strong>{fmtMoney(financeiro?.acumulado)}</strong></div>
              {financeiro?.desconto_inss_auto > 0 && !financeiro?.inss_ja_lancado && (
                <div>INSS/Encargos (Sera registrado): <strong>{fmtMoney(financeiro.desconto_inss_auto)}</strong></div>
              )}
              <div>Total Descontos (Faltas/Outros): <strong>{fmtMoney(financeiro?.descontos_rh)}</strong></div>
              <div>Ja Pago em Dinheiro: <strong>{fmtMoney(financeiro?.pago)}</strong></div>
              <div style={{ marginTop: 8, fontSize: '1rem', fontWeight: 700, color: (financeiro?.saldo ?? 0) >= 0 ? '#059669' : '#DC2626' }}>
                Saldo Liquido Restante: {fmtMoney(financeiro?.saldo)}
              </div>
              <div style={{ marginTop: 8, fontSize: '0.78rem', color: '#888' }}>
                Ao confirmar, o INSS sera gravado no historico (RH) e o saldo sera pago.
              </div>
            </div>
            <div className="modal-actions">
              <button type="button" className="btn-secondary" onClick={() => setModalFechar(false)}>Cancelar</button>
              <button type="button" className="btn-primary" onClick={handleFecharMes}>OK</button>
            </div>
          </div>
        </div>
      )}

      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}
    </div>
  )
}

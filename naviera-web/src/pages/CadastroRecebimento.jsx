import { useState, useEffect } from 'react'
import { api } from '../api.js'

const EMPTY = {
  chave_pix: '', tipo_chave_pix: '', titular_conta: '', cpf_cnpj_recebedor: '',
  banco: '', agencia: '', conta_numero: '', conta_tipo: '',
  psp_provider: '', psp_subconta_id: ''
}

const EMPTY_ONBOARDING = {
  razaoSocial: '', cnpj: '', email: '', telefone: '', mobilePhone: '',
  responsavelNome: '', responsavelCpf: '', birthDate: '',
  companyType: 'LIMITED', incomeValue: '',
  endereco: '', addressNumber: '', complemento: '', bairro: '',
  cep: '', cidade: '', estado: ''
}

export default function CadastroRecebimento() {
  const [form, setForm] = useState(EMPTY)
  const [loading, setLoading] = useState(false)
  const [salvando, setSalvando] = useState(false)
  const [toast, setToast] = useState(null)

  const [pspStatus, setPspStatus] = useState(null)
  const [showOnboarding, setShowOnboarding] = useState(false)
  const [onbForm, setOnbForm] = useState(EMPTY_ONBOARDING)
  const [onbEnviando, setOnbEnviando] = useState(false)

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  function carregar() {
    setLoading(true)
    api.get('/cadastros/recebimento')
      .then(data => setForm({ ...EMPTY, ...Object.fromEntries(Object.entries(data || {}).map(([k, v]) => [k, v ?? ''])) }))
      .catch(() => showToast('Erro ao carregar dados de recebimento', 'error'))
      .finally(() => setLoading(false))
  }

  function carregarPspStatus() {
    api.get('/cadastros/recebimento/psp/status')
      .then(setPspStatus)
      .catch(() => setPspStatus({ status: 'SEM_SUBCONTA' }))
  }

  useEffect(() => { carregar(); carregarPspStatus() }, [])

  async function enviarOnboarding() {
    setOnbEnviando(true)
    try {
      const payload = { ...onbForm, incomeValue: onbForm.incomeValue ? Number(onbForm.incomeValue) : null }
      const resp = await api.post('/cadastros/recebimento/onboarding', payload)
      showToast(resp.mensagem || 'Subconta criada')
      setShowOnboarding(false)
      setOnbForm(EMPTY_ONBOARDING)
      carregar(); carregarPspStatus()
    } catch (err) {
      showToast(err.message || 'Erro no onboarding', 'error')
    } finally {
      setOnbEnviando(false)
    }
  }

  function handleChange(e) {
    setForm(prev => ({ ...prev, [e.target.name]: e.target.value }))
  }

  async function handleSalvar() {
    setSalvando(true)
    try {
      await api.put('/cadastros/recebimento', form)
      showToast('Dados de recebimento atualizados')
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao salvar', 'error')
    } finally {
      setSalvando(false)
    }
  }

  if (loading) return <div className="card"><p>Carregando...</p></div>

  return (
    <div className="card">
      <h2 style={{ textAlign: 'center', marginBottom: 8 }}>Dados de Recebimento</h2>
      <p style={{ textAlign: 'center', fontSize: '0.85rem', color: 'var(--text-soft)', marginTop: 0, marginBottom: 20 }}>
        Conta que recebera os pagamentos de passagens e encomendas pagas pelo app.
      </p>

      <h3 style={{ fontSize: '1rem', marginTop: 0, marginBottom: 12, color: 'var(--primary)' }}>PIX</h3>
      <div className="cadastro-form-4col">
        <label>Chave PIX:</label>
        <input type="text" name="chave_pix" value={form.chave_pix} onChange={handleChange}
               placeholder="CPF, CNPJ, email, telefone ou chave aleatoria" />

        <label>Tipo de Chave:</label>
        <select name="tipo_chave_pix" value={form.tipo_chave_pix} onChange={handleChange}>
          <option value="">-- Selecione --</option>
          <option value="CPF">CPF</option>
          <option value="CNPJ">CNPJ</option>
          <option value="EMAIL">Email</option>
          <option value="TELEFONE">Telefone</option>
          <option value="ALEATORIA">Aleatoria</option>
        </select>

        <label>Titular da Conta:</label>
        <input type="text" name="titular_conta" value={form.titular_conta} onChange={handleChange}
               placeholder="Nome como aparece na conta" />

        <label>CPF/CNPJ Recebedor:</label>
        <input type="text" name="cpf_cnpj_recebedor" value={form.cpf_cnpj_recebedor} onChange={handleChange}
               placeholder="Documento do titular" />
      </div>

      <h3 style={{ fontSize: '1rem', marginTop: 24, marginBottom: 12, color: 'var(--primary)' }}>Conta Bancaria (opcional)</h3>
      <div className="cadastro-form-4col">
        <label>Banco:</label>
        <input type="text" name="banco" value={form.banco} onChange={handleChange}
               placeholder="Nome ou codigo do banco" />

        <label>Agencia:</label>
        <input type="text" name="agencia" value={form.agencia} onChange={handleChange} />

        <label>Conta:</label>
        <input type="text" name="conta_numero" value={form.conta_numero} onChange={handleChange}
               placeholder="Numero da conta com digito" />

        <label>Tipo:</label>
        <select name="conta_tipo" value={form.conta_tipo} onChange={handleChange}>
          <option value="">-- Selecione --</option>
          <option value="CORRENTE">Corrente</option>
          <option value="POUPANCA">Poupanca</option>
        </select>
      </div>

      <h3 style={{ fontSize: '1rem', marginTop: 24, marginBottom: 12, color: 'var(--primary)' }}>Subconta no Gateway (Asaas)</h3>
      <p style={{ fontSize: '0.8rem', color: 'var(--text-soft)', marginTop: -4, marginBottom: 12 }}>
        A subconta recebe os pagamentos do app (PIX, Cartao, Boleto) com split automatico da taxa Naviera.
      </p>

      <div style={{
        padding: '14px 16px', borderRadius: 10, border: `1px solid var(--border)`,
        background: 'var(--bg-soft)', marginBottom: 12
      }}>
        {pspStatus?.status === 'ATIVA' ? (
          <div>
            <div style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--success)' }}>
              ✓ Subconta ativa ({pspStatus.provider})
            </div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-soft)', marginTop: 4, fontFamily: 'monospace' }}>
              ID: {pspStatus.subcontaId}
            </div>
          </div>
        ) : (
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10 }}>
            <div>
              <div style={{ fontSize: '0.85rem', fontWeight: 600 }}>Sem subconta cadastrada</div>
              <div style={{ fontSize: '0.78rem', color: 'var(--text-soft)', marginTop: 2 }}>
                Cadastre para habilitar pagamentos online no app (PIX, Cartao, Boleto).
              </div>
            </div>
            <button type="button" onClick={() => setShowOnboarding(true)} style={{
              padding: '8px 16px', fontSize: '0.85rem', borderRadius: 8,
              background: 'var(--primary)', color: '#fff', border: 'none', cursor: 'pointer', fontFamily: 'inherit'
            }}>
              Cadastrar subconta Asaas
            </button>
          </div>
        )}
      </div>

      <div className="cadastro-buttons" style={{ justifyContent: 'center', gap: 16, marginTop: 24 }}>
        <button onClick={handleSalvar} disabled={salvando}>
          {salvando ? 'Salvando...' : 'SALVAR DADOS'}
        </button>
      </div>

      {showOnboarding && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100
        }} onClick={() => setShowOnboarding(false)}>
          <div style={{
            background: 'var(--bg)', padding: 24, borderRadius: 12,
            maxWidth: 720, width: '90%', maxHeight: '90vh', overflowY: 'auto'
          }} onClick={e => e.stopPropagation()}>
            <h3 style={{ margin: 0, marginBottom: 8 }}>Cadastro da subconta Asaas</h3>
            <p style={{ fontSize: '0.82rem', color: 'var(--text-soft)', marginTop: 0, marginBottom: 16 }}>
              Dados da empresa e do responsavel pra abrir a subconta marketplace.
            </p>
            <div className="cadastro-form-4col">
              <label>Razao social:</label>
              <input type="text" value={onbForm.razaoSocial}
                     onChange={e => setOnbForm({ ...onbForm, razaoSocial: e.target.value })} />
              <label>CNPJ:</label>
              <input type="text" value={onbForm.cnpj}
                     onChange={e => setOnbForm({ ...onbForm, cnpj: e.target.value })}
                     placeholder="Apenas numeros ou com pontuacao" />
              <label>Email:</label>
              <input type="email" value={onbForm.email}
                     onChange={e => setOnbForm({ ...onbForm, email: e.target.value })} />
              <label>Tipo juridico:</label>
              <select value={onbForm.companyType}
                      onChange={e => setOnbForm({ ...onbForm, companyType: e.target.value })}>
                <option value="LIMITED">LTDA</option>
                <option value="INDIVIDUAL">Empresario Individual</option>
                <option value="MEI">MEI</option>
                <option value="ASSOCIATION">Associacao</option>
              </select>
              <label>Telefone fixo:</label>
              <input type="text" value={onbForm.telefone}
                     onChange={e => setOnbForm({ ...onbForm, telefone: e.target.value })} />
              <label>Celular:</label>
              <input type="text" value={onbForm.mobilePhone}
                     onChange={e => setOnbForm({ ...onbForm, mobilePhone: e.target.value })} />
              <label>Responsavel:</label>
              <input type="text" value={onbForm.responsavelNome}
                     onChange={e => setOnbForm({ ...onbForm, responsavelNome: e.target.value })} />
              <label>CPF do responsavel:</label>
              <input type="text" value={onbForm.responsavelCpf}
                     onChange={e => setOnbForm({ ...onbForm, responsavelCpf: e.target.value })} />
              <label>Data nascimento:</label>
              <input type="date" value={onbForm.birthDate}
                     onChange={e => setOnbForm({ ...onbForm, birthDate: e.target.value })} />
              <label>Faturamento mensal (R$):</label>
              <input type="number" step="0.01" value={onbForm.incomeValue}
                     onChange={e => setOnbForm({ ...onbForm, incomeValue: e.target.value })}
                     placeholder="Estimativa do mes" />
              <label>CEP:</label>
              <input type="text" value={onbForm.cep}
                     onChange={e => setOnbForm({ ...onbForm, cep: e.target.value })} />
              <label>Endereco:</label>
              <input type="text" value={onbForm.endereco}
                     onChange={e => setOnbForm({ ...onbForm, endereco: e.target.value })}
                     placeholder="Rua / Av" />
              <label>Numero:</label>
              <input type="text" value={onbForm.addressNumber}
                     onChange={e => setOnbForm({ ...onbForm, addressNumber: e.target.value })} />
              <label>Complemento:</label>
              <input type="text" value={onbForm.complemento}
                     onChange={e => setOnbForm({ ...onbForm, complemento: e.target.value })} />
              <label>Bairro:</label>
              <input type="text" value={onbForm.bairro}
                     onChange={e => setOnbForm({ ...onbForm, bairro: e.target.value })} />
              <label>Cidade:</label>
              <input type="text" value={onbForm.cidade}
                     onChange={e => setOnbForm({ ...onbForm, cidade: e.target.value })} />
              <label>Estado:</label>
              <input type="text" value={onbForm.estado} maxLength={2}
                     onChange={e => setOnbForm({ ...onbForm, estado: e.target.value.toUpperCase() })}
                     placeholder="UF" />
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 12, marginTop: 20 }}>
              <button type="button" onClick={() => setShowOnboarding(false)} style={{
                padding: '10px 20px', borderRadius: 8, border: '1px solid var(--border)',
                background: 'transparent', cursor: 'pointer', fontFamily: 'inherit'
              }}>Cancelar</button>
              <button type="button" onClick={enviarOnboarding} disabled={onbEnviando} style={{
                padding: '10px 20px', borderRadius: 8, border: 'none',
                background: 'var(--primary)', color: '#fff', cursor: 'pointer', fontFamily: 'inherit',
                opacity: onbEnviando ? 0.6 : 1
              }}>{onbEnviando ? 'Enviando...' : 'Criar subconta'}</button>
            </div>
          </div>
        </div>
      )}

      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}
    </div>
  )
}

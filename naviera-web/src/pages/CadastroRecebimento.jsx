import { useState, useEffect } from 'react'
import { api } from '../api.js'

const EMPTY = {
  chave_pix: '', tipo_chave_pix: '', titular_conta: '', cpf_cnpj_recebedor: '',
  banco: '', agencia: '', conta_numero: '', conta_tipo: '',
  psp_provider: '', psp_subconta_id: ''
}

export default function CadastroRecebimento() {
  const [form, setForm] = useState(EMPTY)
  const [loading, setLoading] = useState(false)
  const [salvando, setSalvando] = useState(false)
  const [toast, setToast] = useState(null)

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

  useEffect(() => { carregar() }, [])

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

      <h3 style={{ fontSize: '1rem', marginTop: 24, marginBottom: 12, color: 'var(--primary)' }}>Gateway (PSP)</h3>
      <p style={{ fontSize: '0.8rem', color: 'var(--text-soft)', marginTop: -4, marginBottom: 12 }}>
        Preencha quando a integracao com Mercado Pago / Asaas / Stripe estiver ativa.
      </p>
      <div className="cadastro-form-4col">
        <label>Provedor:</label>
        <select name="psp_provider" value={form.psp_provider} onChange={handleChange}>
          <option value="">-- Nenhum --</option>
          <option value="ASAAS">Asaas</option>
          <option value="MERCADOPAGO">Mercado Pago</option>
          <option value="STRIPE">Stripe</option>
        </select>

        <label>ID da Subconta:</label>
        <input type="text" name="psp_subconta_id" value={form.psp_subconta_id} onChange={handleChange}
               placeholder="ID fornecido pelo PSP" />
      </div>

      <div className="cadastro-buttons" style={{ justifyContent: 'center', gap: 16, marginTop: 24 }}>
        <button onClick={handleSalvar} disabled={salvando}>
          {salvando ? 'Salvando...' : 'SALVAR DADOS'}
        </button>
      </div>

      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}
    </div>
  )
}

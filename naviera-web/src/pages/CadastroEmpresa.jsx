import { useState, useEffect } from 'react'
import { api } from '../api.js'

export default function CadastroEmpresa() {
  const [form, setForm] = useState({
    companhia: '', nome_embarcacao: '', comandante: '', proprietario: '',
    origem_padrao: '', gerente: '', linha_rio_padrao: '', cnpj: '', ie: '',
    endereco: '', cep: '', telefone: '', frase_relatorio: '', recomendacoes_bilhete: ''
  })
  const [loading, setLoading] = useState(false)
  const [salvando, setSalvando] = useState(false)
  const [toast, setToast] = useState(null)

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  function carregar() {
    setLoading(true)
    api.get('/cadastros/empresa')
      .then(data => {
        const emp = Array.isArray(data) ? data[0] || {} : data || {}
        setForm(prev => {
          const f = { ...prev }
          Object.keys(f).forEach(k => { f[k] = emp[k] || '' })
          return f
        })
      })
      .catch(() => showToast('Erro ao carregar dados da empresa', 'error'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { carregar() }, [])

  function handleChange(e) {
    setForm(prev => ({ ...prev, [e.target.name]: e.target.value }))
  }

  function handleLimpar() {
    setForm({
      companhia: '', nome_embarcacao: '', comandante: '', proprietario: '',
      origem_padrao: '', gerente: '', linha_rio_padrao: '', cnpj: '', ie: '',
      endereco: '', cep: '', telefone: '', frase_relatorio: '', recomendacoes_bilhete: ''
    })
  }

  async function handleSalvar() {
    setSalvando(true)
    try {
      await api.put('/cadastros/empresa', form)
      showToast('Dados da empresa atualizados')
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
      <h2 style={{ textAlign: 'center', marginBottom: 20 }}>Configuracoes da Empresa & Bilhete</h2>

      <div className="cadastro-form-4col">
        <label>Companhia:</label>
        <input type="text" name="companhia" value={form.companhia} onChange={handleChange} />

        <label>Embarcacao:</label>
        <input type="text" name="nome_embarcacao" value={form.nome_embarcacao} onChange={handleChange} />

        <label>Comandante:</label>
        <input type="text" name="comandante" value={form.comandante} onChange={handleChange} />

        <label>Proprietario:</label>
        <input type="text" name="proprietario" value={form.proprietario} onChange={handleChange} />

        <label>Origem:</label>
        <input type="text" name="origem_padrao" value={form.origem_padrao} onChange={handleChange} />

        <label>Gerente:</label>
        <input type="text" name="gerente" value={form.gerente} onChange={handleChange} />

        <label>Linha/Rio:</label>
        <input type="text" name="linha_rio_padrao" value={form.linha_rio_padrao} onChange={handleChange} />

        <label>CNPJ:</label>
        <input type="text" name="cnpj" value={form.cnpj} onChange={handleChange} />

        <label>IE:</label>
        <input type="text" name="ie" value={form.ie} onChange={handleChange} />

        <label>Endereco:</label>
        <input type="text" name="endereco" value={form.endereco} onChange={handleChange} />

        <label>CEP:</label>
        <input type="text" name="cep" value={form.cep} onChange={handleChange} />

        <label>Telefone:</label>
        <input type="text" name="telefone" value={form.telefone} onChange={handleChange} />

        <div className="full-row">
          <label>Frase Rodape:</label>
          <input type="text" name="frase_relatorio" value={form.frase_relatorio} onChange={handleChange} />
        </div>

        <div className="full-row" style={{ alignItems: 'start' }}>
          <label style={{ paddingTop: 8 }}>Regras/Avisos do Bilhete:</label>
          <textarea name="recomendacoes_bilhete" value={form.recomendacoes_bilhete} onChange={handleChange} rows={4} />
        </div>
      </div>

      <div className="cadastro-buttons" style={{ justifyContent: 'center', gap: 16 }}>
        <button onClick={handleLimpar}>Limpar</button>
        <button onClick={handleSalvar} disabled={salvando}>
          {salvando ? 'Salvando...' : 'SALVAR DADOS'}
        </button>
      </div>

      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}
    </div>
  )
}

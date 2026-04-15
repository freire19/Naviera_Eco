import { useState, useRef, useCallback } from 'react'
import { api } from '../api.js'
import Autocomplete from './Autocomplete.jsx'

const FORM_INICIAL = {
  id_passageiro: '',
  id_viagem: '',
  assento: '',
  valor_total: '',
  valor_pago: '',
  observacoes: '',
  id_rota: '',
  id_tipo_passagem: '',
  id_acomodacao: '',
  id_caixa: '',
  valor_pagamento_dinheiro: '',
  valor_pagamento_pix: '',
  valor_pagamento_cartao: '',
  nome_passageiro: '',
  numero_doc: ''
}

export default function ModalCriarPassagem({ viagemAtiva, tiposPassageiro, onClose, onSuccess, showToast }) {
  const [form, setForm] = useState({ ...FORM_INICIAL, id_viagem: viagemAtiva.id_viagem })
  const [salvando, setSalvando] = useState(false)
  const [sugestoes, setSugestoes] = useState([])
  const [buscando, setBuscando] = useState(false)
  const debounceRef = useRef(null)

  function handleFormChange(e) {
    const { name, value } = e.target
    setForm(prev => ({ ...prev, [name]: value }))
  }

  const handleNomeChange = useCallback((value) => {
    setForm(prev => ({ ...prev, nome_passageiro: value, id_passageiro: '' }))

    if (debounceRef.current) clearTimeout(debounceRef.current)

    if (value.trim().length < 2) {
      setSugestoes([])
      return
    }

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
      id_passageiro: p.id_passageiro
    }))
    setSugestoes([])
  }

  async function handleCriar(e) {
    e.preventDefault()
    if (!form.nome_passageiro.trim()) {
      showToast('Informe o nome do passageiro', 'error')
      return
    }
    if (!form.valor_total || Number(form.valor_total) <= 0) {
      showToast('Informe o valor total', 'error')
      return
    }

    setSalvando(true)
    try {
      await api.post('/passagens', {
        ...form,
        id_viagem: viagemAtiva.id_viagem,
        valor_total: Number(form.valor_total) || 0,
        valor_pago: Number(form.valor_pago) || 0,
        valor_pagamento_dinheiro: Number(form.valor_pagamento_dinheiro) || 0,
        valor_pagamento_pix: Number(form.valor_pagamento_pix) || 0,
        valor_pagamento_cartao: Number(form.valor_pagamento_cartao) || 0,
        id_tipo_passagem: form.id_tipo_passagem || null,
        id_rota: form.id_rota || null,
        id_acomodacao: form.id_acomodacao || null,
        id_caixa: form.id_caixa || null,
        id_passageiro: form.id_passageiro || null
      })
      showToast('Passagem criada com sucesso')
      onSuccess()
    } catch (err) {
      showToast(err.message || 'Erro ao criar passagem', 'error')
    } finally {
      setSalvando(false)
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <h3>Nova Passagem</h3>
        <form onSubmit={handleCriar}>
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
              <label>Documento</label>
              <input
                name="numero_doc"
                value={form.numero_doc}
                onChange={handleFormChange}
                placeholder="CPF / RG"
              />
            </div>
            <div className="form-group">
              <label>Assento</label>
              <input
                name="assento"
                value={form.assento}
                onChange={handleFormChange}
                placeholder="Ex: A12"
              />
            </div>
            <div className="form-group">
              <label>Tipo Passageiro</label>
              <select
                name="id_tipo_passagem"
                value={form.id_tipo_passagem}
                onChange={handleFormChange}
              >
                <option value="">Selecione...</option>
                {tiposPassageiro.map(t => (
                  <option key={t.id_tipo_passageiro} value={t.id_tipo_passageiro}>
                    {t.descricao}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label>Valor Total *</label>
              <input
                name="valor_total"
                type="number"
                step="0.01"
                min="0"
                value={form.valor_total}
                onChange={handleFormChange}
                placeholder="0.00"
                required
              />
            </div>
            <div className="form-group">
              <label>Valor Pago</label>
              <input
                name="valor_pago"
                type="number"
                step="0.01"
                min="0"
                value={form.valor_pago}
                onChange={handleFormChange}
                placeholder="0.00"
              />
            </div>
            <div className="form-group">
              <label>Pgto Dinheiro</label>
              <input
                name="valor_pagamento_dinheiro"
                type="number"
                step="0.01"
                min="0"
                value={form.valor_pagamento_dinheiro}
                onChange={handleFormChange}
                placeholder="0.00"
              />
            </div>
            <div className="form-group">
              <label>Pgto PIX</label>
              <input
                name="valor_pagamento_pix"
                type="number"
                step="0.01"
                min="0"
                value={form.valor_pagamento_pix}
                onChange={handleFormChange}
                placeholder="0.00"
              />
            </div>
            <div className="form-group">
              <label>Pgto Cartao</label>
              <input
                name="valor_pagamento_cartao"
                type="number"
                step="0.01"
                min="0"
                value={form.valor_pagamento_cartao}
                onChange={handleFormChange}
                placeholder="0.00"
              />
            </div>
            <div className="form-group full-width">
              <label>Observacoes</label>
              <textarea
                name="observacoes"
                value={form.observacoes}
                onChange={handleFormChange}
                rows={3}
                placeholder="Observacoes opcionais..."
              />
            </div>
          </div>
          <div className="modal-actions">
            <button type="button" className="btn-secondary" onClick={onClose} disabled={salvando}>
              Cancelar
            </button>
            <button type="submit" className="btn-primary" disabled={salvando}>
              {salvando ? 'Salvando...' : 'Salvar'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

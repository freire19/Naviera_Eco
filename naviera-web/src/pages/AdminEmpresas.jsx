import { useState, useEffect, useCallback } from 'react'
import { api } from '../api.js'

const FORM_INICIAL = {
  nome: '',
  slug: '',
  cor_primaria: '#1a73e8',
  logo_url: '',
  operador_nome: '',
  operador_email: ''
}

const ONB_INICIAL = {
  razaoSocial: '', cnpj: '', email: '', telefone: '', mobilePhone: '',
  responsavelNome: '', responsavelCpf: '', birthDate: '',
  companyType: 'LIMITED', incomeValue: '',
  endereco: '', addressNumber: '', complemento: '', bairro: '',
  cep: '', cidade: '', estado: ''
}

export default function AdminEmpresas({ viagemAtiva, onNavigate }) {
  const [empresas, setEmpresas] = useState([])
  const [loading, setLoading] = useState(false)
  const [modalAberto, setModalAberto] = useState(false)
  const [editando, setEditando] = useState(null)
  const [form, setForm] = useState(FORM_INICIAL)
  const [salvando, setSalvando] = useState(false)
  const [toast, setToast] = useState(null)
  const [credenciais, setCredenciais] = useState(null)

  // PSP
  const [pspStatus, setPspStatus] = useState({})  // { [empresaId]: { status, subcontaId } }
  const [onbEmpresa, setOnbEmpresa] = useState(null) // empresa escolhida pra onboarding
  const [onbForm, setOnbForm] = useState(ONB_INICIAL)
  const [onbEnviando, setOnbEnviando] = useState(false)

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  const carregar = useCallback(() => {
    setLoading(true)
    api.get('/admin/empresas')
      .then(rows => {
        setEmpresas(rows)
        // Buscar status PSP de cada empresa em paralelo
        Promise.all(
          rows.map(e =>
            api.get(`/admin/empresas/${e.id}/psp/status`)
              .then(st => [e.id, st])
              .catch(() => [e.id, { status: 'SEM_SUBCONTA' }])
          )
        ).then(results => {
          const map = {}
          results.forEach(([id, st]) => { map[id] = st })
          setPspStatus(map)
        })
      })
      .catch(() => showToast('Erro ao carregar empresas', 'error'))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { carregar() }, [carregar])

  function abrirCriar() {
    setEditando(null)
    setForm(FORM_INICIAL)
    setModalAberto(true)
  }

  function abrirEditar(item) {
    setEditando(item)
    setForm({
      nome: item.nome || '',
      slug: item.slug || '',
      cor_primaria: item.cor_primaria || '#1a73e8',
      logo_url: item.logo_url || ''
    })
    setModalAberto(true)
  }

  function abrirOnboarding(empresa) {
    setOnbEmpresa(empresa)
    setOnbForm({
      ...ONB_INICIAL,
      razaoSocial: empresa.nome || '',
      cnpj: empresa.cnpj || ''
    })
  }

  async function enviarOnboarding() {
    if (!onbEmpresa) return
    setOnbEnviando(true)
    try {
      const payload = { ...onbForm, incomeValue: onbForm.incomeValue ? Number(onbForm.incomeValue) : null }
      const resp = await api.post(`/admin/empresas/${onbEmpresa.id}/psp/onboarding`, payload)
      showToast(resp.mensagem || 'Subconta criada')
      setOnbEmpresa(null)
      setOnbForm(ONB_INICIAL)
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro no onboarding', 'error')
    } finally {
      setOnbEnviando(false)
    }
  }

  function fecharModal() {
    setModalAberto(false)
    setEditando(null)
    setForm(FORM_INICIAL)
  }

  function handleChange(e) {
    const { name, value } = e.target
    setForm(prev => ({ ...prev, [name]: value }))
  }

  async function handleSalvar(e) {
    e.preventDefault()
    if (!form.nome.trim()) { showToast('Informe o nome', 'error'); return }
    if (!form.slug.trim()) { showToast('Informe o slug', 'error'); return }
    if (!editando && !form.operador_nome.trim()) { showToast('Informe o nome do operador', 'error'); return }
    if (!editando && !form.operador_email.trim()) { showToast('Informe o email do operador', 'error'); return }

    setSalvando(true)
    try {
      if (editando) {
        await api.put(`/admin/empresas/${editando.id}`, form)
        showToast('Empresa atualizada com sucesso')
        fecharModal()
      } else {
        const result = await api.post('/admin/empresas', form)
        setCredenciais({
          empresa: result.nome,
          slug: result.slug,
          nome: result.operador.nome,
          email: result.operador.email,
          senha: result.operador.senha_temporaria
        })
        fecharModal()
      }
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao salvar empresa', 'error')
    } finally {
      setSalvando(false)
    }
  }

  async function toggleAtivo(empresa) {
    try {
      await api.put(`/admin/empresas/${empresa.id}/ativar`)
      showToast(`Empresa ${empresa.ativo ? 'desativada' : 'ativada'} com sucesso`)
      carregar()
    } catch (err) {
      showToast(err.message || 'Erro ao alterar status', 'error')
    }
  }

  return (
    <div className="card">
      <div className="card-header">
        <h2>Gestao de Empresas</h2>
        <div className="toolbar">
          <button className="btn-primary" onClick={abrirCriar}>+ Nova Empresa</button>
        </div>
      </div>

      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>Nome</th>
              <th>Slug</th>
              <th>Cor</th>
              <th>Status</th>
              <th>Asaas</th>
              <th>Usuarios</th>
              <th>Passagens</th>
              <th>Encomendas</th>
              <th>Fretes</th>
              <th>Acoes</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="10">Carregando...</td></tr>
            ) : empresas.length === 0 ? (
              <tr><td colSpan="10">Nenhuma empresa cadastrada</td></tr>
            ) : empresas.map(e => {
              const psp = pspStatus[e.id]
              const pspAtiva = psp?.status === 'ATIVA'
              return (
                <tr key={e.id}>
                  <td>{e.nome}</td>
                  <td><code>{e.slug}</code></td>
                  <td>
                    <span
                      style={{
                        display: 'inline-block',
                        width: 20,
                        height: 20,
                        borderRadius: 4,
                        backgroundColor: e.cor_primaria || '#ccc',
                        verticalAlign: 'middle',
                        border: '1px solid var(--border)'
                      }}
                    />
                  </td>
                  <td>
                    <span className={`badge ${e.ativo ? 'success' : 'error'}`}>
                      {e.ativo ? 'Ativa' : 'Inativa'}
                    </span>
                  </td>
                  <td>
                    {pspAtiva ? (
                      <span className="badge success" title={psp.subcontaId} style={{ fontFamily: 'monospace', fontSize: 11 }}>
                        ✓ {psp.subcontaId ? psp.subcontaId.slice(0, 8) + '...' : 'Ativa'}
                      </span>
                    ) : (
                      <button className="btn-sm primary" onClick={() => abrirOnboarding(e)}>
                        Cadastrar
                      </button>
                    )}
                  </td>
                  <td>{e.total_usuarios || 0}</td>
                  <td>{e.total_passagens || 0}</td>
                  <td>{e.total_encomendas || 0}</td>
                  <td>{e.total_fretes || 0}</td>
                  <td>
                    <button className="btn-sm primary" onClick={() => abrirEditar(e)}>Editar</button>
                    {' '}
                    <button
                      className={`btn-sm ${e.ativo ? 'danger' : 'success'}`}
                      onClick={() => toggleAtivo(e)}
                    >
                      {e.ativo ? 'Desativar' : 'Ativar'}
                    </button>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      {modalAberto && (
        <div className="modal-overlay" onClick={fecharModal}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3>{editando ? 'Editar Empresa' : 'Nova Empresa'}</h3>
            <form onSubmit={handleSalvar}>
              <div className="form-grid">
                <div className="form-group">
                  <label>Nome</label>
                  <input type="text" name="nome" value={form.nome} onChange={handleChange} />
                </div>
                <div className="form-group">
                  <label>Slug (subdominio)</label>
                  <input type="text" name="slug" value={form.slug} onChange={handleChange} placeholder="ex: saofrancisco" />
                </div>
                <div className="form-group">
                  <label>Cor Primaria</label>
                  <input type="color" name="cor_primaria" value={form.cor_primaria} onChange={handleChange} />
                </div>
                <div className="form-group">
                  <label>Logo URL</label>
                  <input type="text" name="logo_url" value={form.logo_url} onChange={handleChange} placeholder="https://..." />
                </div>
              </div>
              {!editando && (
                <>
                  <h4 style={{ margin: '16px 0 8px', borderTop: '1px solid var(--border)', paddingTop: 16 }}>Operador Responsavel</h4>
                  <div className="form-grid">
                    <div className="form-group">
                      <label>Nome do Operador</label>
                      <input type="text" name="operador_nome" value={form.operador_nome} onChange={handleChange} placeholder="Nome completo" />
                    </div>
                    <div className="form-group">
                      <label>Email do Operador</label>
                      <input type="email" name="operador_email" value={form.operador_email} onChange={handleChange} placeholder="operador@email.com" />
                    </div>
                  </div>
                </>
              )}
              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={fecharModal}>Cancelar</button>
                <button type="submit" className="btn-primary" disabled={salvando}>
                  {salvando ? 'Salvando...' : 'Salvar'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {onbEmpresa && (
        <div className="modal-overlay" onClick={() => setOnbEmpresa(null)}>
          <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 720 }}>
            <h3>Cadastrar subconta Asaas — {onbEmpresa.nome}</h3>
            <p style={{ fontSize: 13, color: 'var(--text-soft)', marginTop: 4, marginBottom: 16 }}>
              Dados da empresa e responsavel pra abrir a subconta marketplace.
            </p>
            <div className="form-grid">
              <div className="form-group"><label>Razao social</label>
                <input type="text" value={onbForm.razaoSocial} onChange={e => setOnbForm({ ...onbForm, razaoSocial: e.target.value })} /></div>
              <div className="form-group"><label>CNPJ</label>
                <input type="text" value={onbForm.cnpj} onChange={e => setOnbForm({ ...onbForm, cnpj: e.target.value })} placeholder="Apenas numeros ou com pontuacao" /></div>
              <div className="form-group"><label>Email</label>
                <input type="email" value={onbForm.email} onChange={e => setOnbForm({ ...onbForm, email: e.target.value })} /></div>
              <div className="form-group"><label>Tipo juridico</label>
                <select value={onbForm.companyType} onChange={e => setOnbForm({ ...onbForm, companyType: e.target.value })}>
                  <option value="LIMITED">LTDA</option>
                  <option value="INDIVIDUAL">Empresario Individual</option>
                  <option value="MEI">MEI</option>
                  <option value="ASSOCIATION">Associacao</option>
                </select></div>
              <div className="form-group"><label>Telefone fixo</label>
                <input type="text" value={onbForm.telefone} onChange={e => setOnbForm({ ...onbForm, telefone: e.target.value })} /></div>
              <div className="form-group"><label>Celular</label>
                <input type="text" value={onbForm.mobilePhone} onChange={e => setOnbForm({ ...onbForm, mobilePhone: e.target.value })} /></div>
              <div className="form-group"><label>Responsavel</label>
                <input type="text" value={onbForm.responsavelNome} onChange={e => setOnbForm({ ...onbForm, responsavelNome: e.target.value })} /></div>
              <div className="form-group"><label>CPF do responsavel</label>
                <input type="text" value={onbForm.responsavelCpf} onChange={e => setOnbForm({ ...onbForm, responsavelCpf: e.target.value })} /></div>
              <div className="form-group"><label>Data nascimento</label>
                <input type="date" value={onbForm.birthDate} onChange={e => setOnbForm({ ...onbForm, birthDate: e.target.value })} /></div>
              <div className="form-group"><label>Faturamento mensal (R$)</label>
                <input type="number" step="0.01" value={onbForm.incomeValue} onChange={e => setOnbForm({ ...onbForm, incomeValue: e.target.value })} placeholder="Estimativa" /></div>
              <div className="form-group"><label>CEP</label>
                <input type="text" value={onbForm.cep} onChange={e => setOnbForm({ ...onbForm, cep: e.target.value })} /></div>
              <div className="form-group"><label>Endereco</label>
                <input type="text" value={onbForm.endereco} onChange={e => setOnbForm({ ...onbForm, endereco: e.target.value })} placeholder="Rua / Av" /></div>
              <div className="form-group"><label>Numero</label>
                <input type="text" value={onbForm.addressNumber} onChange={e => setOnbForm({ ...onbForm, addressNumber: e.target.value })} /></div>
              <div className="form-group"><label>Complemento</label>
                <input type="text" value={onbForm.complemento} onChange={e => setOnbForm({ ...onbForm, complemento: e.target.value })} /></div>
              <div className="form-group"><label>Bairro</label>
                <input type="text" value={onbForm.bairro} onChange={e => setOnbForm({ ...onbForm, bairro: e.target.value })} /></div>
              <div className="form-group"><label>Cidade</label>
                <input type="text" value={onbForm.cidade} onChange={e => setOnbForm({ ...onbForm, cidade: e.target.value })} /></div>
              <div className="form-group"><label>Estado</label>
                <input type="text" value={onbForm.estado} maxLength={2} onChange={e => setOnbForm({ ...onbForm, estado: e.target.value.toUpperCase() })} placeholder="UF" /></div>
            </div>
            <div className="modal-actions">
              <button type="button" className="btn-secondary" onClick={() => setOnbEmpresa(null)}>Cancelar</button>
              <button type="button" className="btn-primary" onClick={enviarOnboarding} disabled={onbEnviando}>
                {onbEnviando ? 'Enviando...' : 'Criar subconta'}
              </button>
            </div>
          </div>
        </div>
      )}

      {credenciais && (
        <div className="modal-overlay" onClick={() => setCredenciais(null)}>
          <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 480 }}>
            <h3 style={{ color: 'var(--success)' }}>Empresa Criada com Sucesso!</h3>
            <p style={{ margin: '12px 0 4px', fontSize: 14, color: 'var(--text-muted)' }}>
              Envie estas credenciais para o operador. A senha e temporaria.
            </p>
            <div style={{ background: 'var(--bg-secondary)', borderRadius: 8, padding: 16, margin: '12px 0', fontFamily: 'monospace', fontSize: 14, lineHeight: 2 }}>
              <div><strong>Empresa:</strong> {credenciais.empresa}</div>
              <div><strong>Acesso:</strong> {credenciais.slug}.naviera.com.br</div>
              <div style={{ borderTop: '1px solid var(--border)', margin: '8px 0' }} />
              <div><strong>Login:</strong> {credenciais.email}</div>
              <div><strong>Senha:</strong> <span style={{ color: 'var(--danger)', fontWeight: 'bold', fontSize: 16 }}>{credenciais.senha}</span></div>
            </div>
            <p style={{ fontSize: 12, color: 'var(--text-muted)' }}>
              O operador deve trocar a senha apos o primeiro login.
            </p>
            <div className="modal-actions">
              <button className="btn-secondary" onClick={() => {
                navigator.clipboard.writeText(`Empresa: ${credenciais.empresa}\nAcesso: ${credenciais.slug}.naviera.com.br\nLogin: ${credenciais.email}\nSenha: ${credenciais.senha}`)
                showToast('Credenciais copiadas!')
              }}>Copiar</button>
              <button className="btn-primary" onClick={() => setCredenciais(null)}>Fechar</button>
            </div>
          </div>
        </div>
      )}

      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}
    </div>
  )
}

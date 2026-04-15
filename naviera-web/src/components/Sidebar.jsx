import { useAuth } from '../App.jsx'

const NAV = [
  {
    title: 'Principal',
    items: [
      { key: 'inicio', icon: '\u2302', label: 'Inicio' }
    ]
  },
  {
    title: 'Passagens',
    items: [
      { key: 'vender-passagem', icon: '\uD83C\uDFAB', label: 'Vender Passagem' },
      { key: 'listar-passageiros', icon: '\uD83D\uDCCB', label: 'Lista Passageiros' },
      { key: 'relatorio-passagens', icon: '\uD83D\uDCCA', label: 'Relatorio' }
    ]
  },
  {
    title: 'Encomendas',
    items: [
      { key: 'nova-encomenda', icon: '\uD83D\uDCE6', label: 'Nova Encomenda' },
      { key: 'listar-encomendas', icon: '\uD83D\uDCCB', label: 'Listar Encomendas' },
      { key: 'relatorio-encomendas', icon: '\uD83D\uDCCA', label: 'Relatorio' },
      { key: 'tabela-preco-encomenda', icon: '\uD83C\uDFF7', label: 'Tabela de Precos' },
      { key: 'cadastro-produto', icon: '\uD83D\uDC64', label: 'Clientes' }
    ]
  },
  {
    title: 'Fretes',
    items: [
      { key: 'lancar-frete', icon: '\uD83D\uDE9A', label: 'Lancar Frete' },
      { key: 'listar-fretes', icon: '\uD83D\uDCCB', label: 'Lista Fretes' },
      { key: 'relatorio-fretes', icon: '\uD83D\uDCCA', label: 'Relatorio' },
      { key: 'tabela-preco-frete', icon: '\uD83C\uDFF7', label: 'Tabela Precos' },
      { key: 'review-ocr', icon: '\uD83D\uDCF7', label: 'Conferir OCR' }
    ]
  },
  {
    title: 'OCR por Foto',
    items: [
      { key: '_ocr-app', icon: '\uD83D\uDCF1', label: 'Lancar por Foto', external: true },
      { key: 'review-ocr-section', icon: '\uD83D\uDD0D', label: 'Conferir Lancamentos', alias: 'review-ocr' }
    ]
  },
  {
    title: 'Financeiro',
    items: [
      { key: 'financeiro-entrada', icon: '\uD83D\uDCB0', label: 'Lancar Entrada' },
      { key: 'financeiro-saida', icon: '\uD83D\uDCB8', label: 'Lancar Saida' },
      { key: 'balanco-viagem', icon: '\uD83D\uDCC8', label: 'Balanco Viagem' },
      { key: 'boletos', icon: '\uD83D\uDCDD', label: 'Boletos' }
    ]
  },
  {
    title: 'Cadastros',
    items: [
      { key: 'cadastro-viagem', icon: '\u26F4', label: 'Viagens' },
      { key: 'cadastro-usuario', icon: '\uD83D\uDC64', label: 'Usuarios' },
      { key: 'cadastro-rota', icon: '\uD83D\uDDFA', label: 'Rotas' },
      { key: 'cadastro-tarifa', icon: '\uD83C\uDFF7', label: 'Tarifas' },
      { key: 'cadastro-empresa', icon: '\uD83C\uDFE2', label: 'Empresa' },
      { key: 'cadastro-embarcacao', icon: '\uD83D\uDEA2', label: 'Embarcacoes' },
      { key: 'cadastro-conferente', icon: '\uD83D\uDCCB', label: 'Conferentes' },
      { key: 'cadastro-caixa', icon: '\uD83D\uDCB3', label: 'Caixas' },
      { key: 'cadastro-item', icon: '\uD83D\uDCE6', label: 'Itens' },
      { key: 'cadastro-produto', icon: '\uD83D\uDC64', label: 'Clientes Encomenda' },
      { key: 'auxiliares', icon: '\uD83D\uDDD2', label: 'Auxiliares' },
      { key: 'gestao-funcionarios', icon: '\uD83D\uDC65', label: 'Funcionarios' }
    ]
  },
  {
    title: 'Sistema',
    items: [
      { key: 'agenda', icon: '\uD83D\uDCC5', label: 'Agenda' },
      { key: 'recibo-avulso', icon: '\uD83E\uDDFE', label: 'Recibos' },
      { key: 'historico-estornos', icon: '\u21A9', label: 'Estornos' },
      { key: 'configurar-api', icon: '\u2699', label: 'Config. API' }
    ]
  }
]

const NAV_DOCS = {
  title: 'Documentos',
  items: [
    { key: 'admin-documentos', icon: '\uD83D\uDD12', label: 'Docs Arquivados' }
  ]
}

const NAV_ADMIN = {
  title: 'Admin Naviera',
  items: [
    { key: 'admin-empresas', icon: '\uD83C\uDFE2', label: 'Empresas' },
    { key: 'admin-metricas', icon: '\uD83D\uDCCA', label: 'Metricas' }
  ]
}

export default function Sidebar({ currentPage, onNavigate, pages }) {
  const { usuario, logout, theme, toggleTheme } = useAuth()

  const iniciais = (usuario?.nome || 'U')
    .split(' ')
    .slice(0, 2)
    .map(w => w[0])
    .join('')
    .toUpperCase()

  const isAdminSubdomain = window.location.hostname.startsWith('admin.')
  const funcaoLower = (usuario?.funcao || '').toLowerCase()
  const isAdminGlobal = (funcaoLower === 'administrador' || funcaoLower === 'admin') && isAdminSubdomain
  const isAdminEmpresa = funcaoLower === 'administrador' || funcaoLower === 'admin' || funcaoLower === 'gerente'

  const sections = [...NAV]
  if (isAdminEmpresa) sections.push(NAV_DOCS)
  if (isAdminGlobal) sections.push(NAV_ADMIN)

  return (
    <aside className="sidebar">
      <div className="sidebar-header">
        <h2>Naviera</h2>
        <div className="tagline">Console Web</div>
      </div>

      <nav className="sidebar-nav">
        {sections.map(section => (
          <div className="nav-section" key={section.title}>
            <div className="nav-section-title">{section.title}</div>
            {section.items.map(item => {
              if (item.external) {
                // Botao que abre naviera-ocr em nova aba
                const ocrUrl = window.location.hostname === 'localhost'
                  ? `http://${window.location.hostname}:5175`
                  : `https://ocr.${window.location.hostname.replace(/^[^.]+\./, '')}`
                return (
                  <a
                    key={item.key}
                    className="nav-item nav-item-external"
                    href={ocrUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    style={{ textDecoration: 'none', color: 'inherit', display: 'flex', alignItems: 'center', gap: 8 }}
                  >
                    <span className="icon">{item.icon}</span>
                    {item.label}
                    <span style={{ marginLeft: 'auto', fontSize: '0.7rem', opacity: 0.5 }}>&#8599;</span>
                  </a>
                )
              }
              const navKey = item.alias || item.key
              return (
                <div
                  key={item.key}
                  className={`nav-item ${currentPage === navKey ? 'active' : ''}`}
                  onClick={() => onNavigate(navKey)}
                >
                  <span className="icon">{item.icon}</span>
                  {item.label}
                </div>
              )
            })}
          </div>
        ))}
      </nav>

      <div className="sidebar-footer">
        <div className="user-info">
          <div className="user-avatar">{iniciais}</div>
          <div>
            <div className="user-name">{usuario?.nome}</div>
            <div className="user-role">{usuario?.funcao}</div>
          </div>
        </div>
        <div className="sidebar-footer-actions">
          <button className="btn-icon" onClick={toggleTheme}>
            {theme === 'light' ? '\u263E Dark' : '\u2600 Light'}
          </button>
          <button className="btn-icon" onClick={logout}>Sair</button>
        </div>
      </div>
    </aside>
  )
}

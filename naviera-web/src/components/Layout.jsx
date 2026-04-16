import { useState, useEffect, lazy, Suspense } from 'react'
import { useAuth } from '../App.jsx'
import Sidebar from './Sidebar.jsx'
import TopBar from './TopBar.jsx'
import { api } from '../api.js'

// DP062: React.lazy code splitting — cada pagina e um chunk separado
const Dashboard = lazy(() => import('../pages/Dashboard.jsx'))
const Passagens = lazy(() => import('../pages/Passagens.jsx'))
const Encomendas = lazy(() => import('../pages/Encomendas.jsx'))
const Fretes = lazy(() => import('../pages/Fretes.jsx'))
const Financeiro = lazy(() => import('../pages/Financeiro.jsx'))
const ListaPassageiros = lazy(() => import('../pages/ListaPassageiros.jsx'))
const ListaEncomendas = lazy(() => import('../pages/ListaEncomendas.jsx'))
const ListaFretes = lazy(() => import('../pages/ListaFretes.jsx'))
const RelatorioPassagens = lazy(() => import('../pages/RelatorioPassagens.jsx'))
const RelatorioEncomendas = lazy(() => import('../pages/RelatorioEncomendas.jsx'))
const RelatorioFretes = lazy(() => import('../pages/RelatorioFretes.jsx'))
const FinanceiroSaida = lazy(() => import('../pages/FinanceiroSaida.jsx'))
const BalancoViagem = lazy(() => import('../pages/BalancoViagem.jsx'))
const Boletos = lazy(() => import('../pages/Boletos.jsx'))
const TabelaPrecoFrete = lazy(() => import('../pages/TabelaPrecoFrete.jsx'))
const TabelaPrecoEncomenda = lazy(() => import('../pages/TabelaPrecoEncomenda.jsx'))
const CadastroItem = lazy(() => import('../pages/CadastroItem.jsx'))
const Auxiliares = lazy(() => import('../pages/Auxiliares.jsx'))
const CadastroViagem = lazy(() => import('../pages/CadastroViagem.jsx'))
const CadastroUsuario = lazy(() => import('../pages/CadastroUsuario.jsx'))
const CadastroRota = lazy(() => import('../pages/CadastroRota.jsx'))
const CadastroTarifa = lazy(() => import('../pages/CadastroTarifa.jsx'))
const CadastroEmpresa = lazy(() => import('../pages/CadastroEmpresa.jsx'))
const CadastroEmbarcacao = lazy(() => import('../pages/CadastroEmbarcacao.jsx'))
const CadastroConferente = lazy(() => import('../pages/CadastroConferente.jsx'))
const CadastroCaixa = lazy(() => import('../pages/CadastroCaixa.jsx'))
const CadastroClienteEncomenda = lazy(() => import('../pages/CadastroClienteEncomenda.jsx'))
const AdminEmpresas = lazy(() => import('../pages/AdminEmpresas.jsx'))
const AdminMetricas = lazy(() => import('../pages/AdminMetricas.jsx'))
const DocumentosAdmin = lazy(() => import('../pages/DocumentosAdmin.jsx'))
const ReviewOCR = lazy(() => import('../pages/ReviewOCR.jsx'))
const EstornoPassagem = lazy(() => import('../pages/EstornoPassagem.jsx'))
const HistoricoEstornos = lazy(() => import('../pages/HistoricoEstornos.jsx'))
const ReciboAvulso = lazy(() => import('../pages/ReciboAvulso.jsx'))
const GestaoFuncionarios = lazy(() => import('../pages/GestaoFuncionarios.jsx'))
const Agenda = lazy(() => import('../pages/Agenda.jsx'))
const ConfigurarApi = lazy(() => import('../pages/ConfigurarApi.jsx'))

const PAGES = {
  inicio: { component: Dashboard, label: 'Inicio', section: 'principal' },
  'vender-passagem': { component: Passagens, label: 'Vender Passagem', section: 'passagens' },
  'listar-passageiros': { component: ListaPassageiros, label: 'Lista de Passageiros', section: 'passagens' },
  'relatorio-passagens': { component: RelatorioPassagens, label: 'Relatorio Passagens', section: 'passagens' },
  'nova-encomenda': { component: Encomendas, label: 'Nova Encomenda', section: 'encomendas' },
  'listar-encomendas': { component: ListaEncomendas, label: 'Lista Encomendas', section: 'encomendas' },
  'relatorio-encomendas': { component: RelatorioEncomendas, label: 'Relatorio Encomendas', section: 'encomendas' },
  'tabela-preco-encomenda': { component: TabelaPrecoEncomenda, label: 'Tabela de Precos', section: 'encomendas' },
  'lancar-frete': { component: Fretes, label: 'Lancar Frete', section: 'fretes' },
  'listar-fretes': { component: ListaFretes, label: 'Lista Fretes', section: 'fretes' },
  'relatorio-fretes': { component: RelatorioFretes, label: 'Relatorio Fretes', section: 'fretes' },
  'tabela-preco-frete': { component: TabelaPrecoFrete, label: 'Tabela Precos', section: 'fretes' },
  'review-ocr': { component: ReviewOCR, label: 'Conferir OCR', section: 'fretes' },
  'financeiro-entrada': { component: Financeiro, label: 'Lancar Entrada', section: 'financeiro' },
  'financeiro-saida': { component: FinanceiroSaida, label: 'Lancar Saida', section: 'financeiro' },
  'balanco-viagem': { component: BalancoViagem, label: 'Balanco Viagem', section: 'financeiro' },
  'boletos': { component: Boletos, label: 'Boletos', section: 'financeiro' },
  'cadastro-viagem': { component: CadastroViagem, label: 'Cadastrar Viagem', section: 'cadastros' },
  'cadastro-usuario': { component: CadastroUsuario, label: 'Usuarios', section: 'cadastros' },
  'cadastro-rota': { component: CadastroRota, label: 'Rotas', section: 'cadastros' },
  'cadastro-tarifa': { component: CadastroTarifa, label: 'Tarifas', section: 'cadastros' },
  'cadastro-empresa': { component: CadastroEmpresa, label: 'Empresa', section: 'cadastros' },
  'cadastro-embarcacao': { component: CadastroEmbarcacao, label: 'Embarcacoes', section: 'cadastros' },
  'cadastro-conferente': { component: CadastroConferente, label: 'Conferentes', section: 'cadastros' },
  'cadastro-caixa': { component: CadastroCaixa, label: 'Caixas', section: 'cadastros' },
  'cadastro-produto': { component: CadastroClienteEncomenda, label: 'Clientes Encomenda', section: 'cadastros' },
  'cadastro-item': { component: CadastroItem, label: 'Itens (Frete/Encomenda)', section: 'cadastros' },
  'auxiliares': { component: Auxiliares, label: 'Tabelas Auxiliares', section: 'cadastros' },
  'gestao-funcionarios': { component: GestaoFuncionarios, label: 'Gestao Funcionarios', section: 'cadastros' },
  'estorno-passagem': { component: EstornoPassagem, label: 'Estorno Passagem', section: 'pagamentos' },
  'historico-estornos': { component: HistoricoEstornos, label: 'Historico Estornos', section: 'pagamentos' },
  'recibo-avulso': { component: ReciboAvulso, label: 'Recibo Avulso', section: 'recibos' },
  agenda: { component: Agenda, label: 'Agenda', section: 'sistema' },
  'configurar-api': { component: ConfigurarApi, label: 'Configurar API', section: 'sistema' },
  'admin-empresas': { component: AdminEmpresas, label: 'Gestao de Empresas', section: 'admin' },
  'admin-metricas': { component: AdminMetricas, label: 'Metricas da Plataforma', section: 'admin' },
  'admin-documentos': { component: DocumentosAdmin, label: 'Documentos Arquivados', section: 'admin' }
}

export default function Layout() {
  // Sistema de abas multiplas — cada aba permanece viva
  const [tabs, setTabs] = useState([{ id: 'inicio', page: 'inicio' }])
  const [activeTab, setActiveTab] = useState('inicio')
  const [viagens, setViagens] = useState([])
  const [viagemAtiva, setViagemAtiva] = useState(null)
  const [sidebarVisible, setSidebarVisible] = useState(true)

  useEffect(() => {
    api.get('/viagens').then(setViagens).catch(() => {})
    api.get('/viagens/ativa').then(v => { if (v) setViagemAtiva(v) }).catch(() => {})
  }, [])

  // Navegar para pagina — abre nova aba ou foca existente
  function navigateTo(page) {
    const existing = tabs.find(t => t.page === page)
    if (existing) {
      setActiveTab(existing.id)
    } else {
      const newTab = { id: `${page}-${Date.now()}`, page }
      setTabs(prev => [...prev, newTab])
      setActiveTab(newTab.id)
    }
  }

  // Fechar aba (nunca fecha Inicio)
  function closeTab(tabId) {
    const tab = tabs.find(t => t.id === tabId)
    if (tab?.page === 'inicio') return
    const newTabs = tabs.filter(t => t.id !== tabId)
    if (activeTab === tabId) {
      const idx = tabs.findIndex(t => t.id === tabId)
      const nextTab = newTabs[Math.min(idx, newTabs.length - 1)]
      setActiveTab(nextTab?.id || 'inicio')
    }
    setTabs(newTabs)
  }

  // Fechar aba ativa (para botoes Sair/ESC das paginas)
  function closeActiveTab() {
    closeTab(activeTab)
  }

  // ESC global: fecha aba ativa (se nao for Inicio e nao tiver modal aberto)
  useEffect(() => {
    const handler = (e) => {
      if (e.key === 'Escape') {
        // Se tem modal aberto, deixa o modal tratar
        if (document.querySelector('.modal-overlay')) return
        // Se nao e Inicio, fecha a aba
        const tab = tabs.find(t => t.id === activeTab)
        if (tab && tab.page !== 'inicio') {
          closeTab(activeTab)
        }
      }
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [activeTab, tabs])

  const activeTabObj = tabs.find(t => t.id === activeTab) || tabs[0]
  const activePageConfig = PAGES[activeTabObj?.page] || PAGES.inicio

  return (
    <div className="app-layout">
      {sidebarVisible && <Sidebar currentPage={activeTabObj?.page} onNavigate={navigateTo} pages={PAGES} />}
      {/* Botao toggle sidebar */}
      <div onClick={() => setSidebarVisible(prev => !prev)}
        style={{
          position: 'fixed', left: sidebarVisible ? 240 : 0, top: '50%', transform: 'translateY(-50%)',
          zIndex: 101, cursor: 'pointer', background: 'var(--sidebar-bg)', border: '1px solid var(--border)',
          borderLeft: 'none', borderRadius: '0 6px 6px 0', padding: '8px 4px',
          color: 'var(--primary)', fontSize: '0.8rem', transition: 'left 0.2s'
        }}
        title={sidebarVisible ? 'Ocultar menu' : 'Mostrar menu'}>
        {sidebarVisible ? '◀' : '▶'}
      </div>
      <div className="main-content" style={sidebarVisible ? {} : { marginLeft: 0 }}>
        <TopBar
          label={activePageConfig.label}
          viagens={viagens}
          viagemAtiva={viagemAtiva}
          onViagemChange={setViagemAtiva}
        />

        {/* BARRA DE ABAS */}
        {tabs.length > 1 && (
          <div style={{
            display: 'flex', gap: 0, background: 'var(--bg-card)', borderBottom: '1px solid var(--border)',
            overflowX: 'auto', padding: '0 8px', minHeight: 32
          }}>
            {tabs.map(tab => {
              const cfg = PAGES[tab.page] || PAGES.inicio
              const isActive = tab.id === activeTab
              const isInicio = tab.page === 'inicio'
              return (
                <div key={tab.id}
                  onClick={() => setActiveTab(tab.id)}
                  style={{
                    display: 'flex', alignItems: 'center', gap: 6,
                    padding: '6px 14px', cursor: 'pointer', whiteSpace: 'nowrap',
                    fontSize: '0.78rem', fontWeight: isActive ? 600 : 400,
                    color: isActive ? 'var(--primary)' : 'var(--text-muted)',
                    background: isActive ? 'var(--bg-soft)' : 'transparent',
                    borderBottom: isActive ? '2px solid var(--primary)' : '2px solid transparent',
                    transition: 'all 0.15s'
                  }}>
                  {isInicio ? '⌂ ' : ''}{cfg.label}
                  {!isInicio && (
                    <span onClick={e => { e.stopPropagation(); closeTab(tab.id) }}
                      style={{ marginLeft: 4, fontSize: '0.9rem', opacity: 0.5, cursor: 'pointer', lineHeight: 1 }}
                      title="Fechar aba">×</span>
                  )}
                </div>
              )
            })}
          </div>
        )}

        {/* Mobile nav */}
        <div className="mobile-nav">
          <button className={activeTabObj?.page === 'inicio' ? 'active' : ''} onClick={() => navigateTo('inicio')}>&#8962; Inicio</button>
          <button className={activeTabObj?.page === 'vender-passagem' ? 'active' : ''} onClick={() => navigateTo('vender-passagem')}>&#127915; Passagens</button>
          <button className={activeTabObj?.page === 'nova-encomenda' ? 'active' : ''} onClick={() => navigateTo('nova-encomenda')}>&#128230; Encomendas</button>
          <button className={activeTabObj?.page === 'lancar-frete' ? 'active' : ''} onClick={() => navigateTo('lancar-frete')}>&#128666; Fretes</button>
          <button className={activeTabObj?.page === 'financeiro-entrada' ? 'active' : ''} onClick={() => navigateTo('financeiro-entrada')}>&#128176; Financeiro</button>
        </div>

        {/* CONTEUDO DAS ABAS — todas permanecem montadas, so a ativa e visivel */}
        <div style={{ flex: 1, position: 'relative' }}>
          {tabs.map(tab => {
            const cfg = PAGES[tab.page] || PAGES.inicio
            const PageComponent = cfg.component
            const isVisible = tab.id === activeTab
            return (
              <div key={tab.id} className="page" style={{ display: isVisible ? 'block' : 'none' }}>
                <Suspense fallback={<div style={{padding:'2rem',textAlign:'center',opacity:0.5}}>Carregando...</div>}>
                  <PageComponent
                    viagemAtiva={viagemAtiva}
                    onNavigate={navigateTo}
                    onClose={closeActiveTab}
                  />
                </Suspense>
              </div>
            )
          })}
        </div>

      </div>
    </div>
  )
}

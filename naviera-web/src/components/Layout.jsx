import { useState, useEffect } from 'react'
import { useAuth } from '../App.jsx'
import Sidebar from './Sidebar.jsx'
import TopBar from './TopBar.jsx'
import { api } from '../api.js'

// Paginas principais
import Dashboard from '../pages/Dashboard.jsx'
import Passagens from '../pages/Passagens.jsx'
import Encomendas from '../pages/Encomendas.jsx'
import Fretes from '../pages/Fretes.jsx'
import Financeiro from '../pages/Financeiro.jsx'

// Listas
import ListaPassageiros from '../pages/ListaPassageiros.jsx'
import ListaEncomendas from '../pages/ListaEncomendas.jsx'
import ListaFretes from '../pages/ListaFretes.jsx'

// Relatorios
import RelatorioPassagens from '../pages/RelatorioPassagens.jsx'
import RelatorioEncomendas from '../pages/RelatorioEncomendas.jsx'
import RelatorioFretes from '../pages/RelatorioFretes.jsx'

// Financeiro
import FinanceiroSaida from '../pages/FinanceiroSaida.jsx'
import BalancoViagem from '../pages/BalancoViagem.jsx'
import Boletos from '../pages/Boletos.jsx'

// Tabela Precos
import TabelaPrecoFrete from '../pages/TabelaPrecoFrete.jsx'
import CadastroItem from '../pages/CadastroItem.jsx'
import TabelaPrecoEncomenda from '../pages/TabelaPrecoEncomenda.jsx'
import Auxiliares from '../pages/Auxiliares.jsx'

// Cadastros
import CadastroViagem from '../pages/CadastroViagem.jsx'
import CadastroUsuario from '../pages/CadastroUsuario.jsx'
import CadastroRota from '../pages/CadastroRota.jsx'
import CadastroTarifa from '../pages/CadastroTarifa.jsx'
import CadastroEmpresa from '../pages/CadastroEmpresa.jsx'
import CadastroEmbarcacao from '../pages/CadastroEmbarcacao.jsx'
import CadastroConferente from '../pages/CadastroConferente.jsx'
import CadastroCaixa from '../pages/CadastroCaixa.jsx'
import CadastroClienteEncomenda from '../pages/CadastroClienteEncomenda.jsx'

// Admin
import AdminEmpresas from '../pages/AdminEmpresas.jsx'
import AdminMetricas from '../pages/AdminMetricas.jsx'
import DocumentosAdmin from '../pages/DocumentosAdmin.jsx'

// OCR
import ReviewOCR from '../pages/ReviewOCR.jsx'

// Outros
import EstornoPassagem from '../pages/EstornoPassagem.jsx'
import HistoricoEstornos from '../pages/HistoricoEstornos.jsx'
import ReciboAvulso from '../pages/ReciboAvulso.jsx'
import GestaoFuncionarios from '../pages/GestaoFuncionarios.jsx'
import Agenda from '../pages/Agenda.jsx'
import ConfigurarApi from '../pages/ConfigurarApi.jsx'

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

  const activeTabObj = tabs.find(t => t.id === activeTab) || tabs[0]
  const activePageConfig = PAGES[activeTabObj?.page] || PAGES.inicio

  return (
    <div className="app-layout">
      <Sidebar currentPage={activeTabObj?.page} onNavigate={navigateTo} pages={PAGES} />
      <div className="main-content">
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
                <PageComponent
                  viagemAtiva={viagemAtiva}
                  onNavigate={navigateTo}
                />
              </div>
            )
          })}
        </div>

      </div>
    </div>
  )
}

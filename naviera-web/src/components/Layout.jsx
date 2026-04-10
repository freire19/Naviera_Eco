import { useState, useEffect } from 'react'
import { useAuth } from '../App.jsx'
import Sidebar from './Sidebar.jsx'
import TopBar from './TopBar.jsx'
import Dashboard from '../pages/Dashboard.jsx'
import Passagens from '../pages/Passagens.jsx'
import Encomendas from '../pages/Encomendas.jsx'
import Fretes from '../pages/Fretes.jsx'
import Financeiro from '../pages/Financeiro.jsx'
import Placeholder from '../pages/Placeholder.jsx'
import { api } from '../api.js'

const PAGES = {
  inicio: { component: Dashboard, label: 'Inicio', section: 'principal' },
  'vender-passagem': { component: Passagens, label: 'Vender Passagem', section: 'passagens' },
  'listar-passageiros': { component: Placeholder, label: 'Lista de Passageiros', section: 'passagens' },
  'relatorio-passagens': { component: Placeholder, label: 'Relatorio Passagens', section: 'passagens' },
  'nova-encomenda': { component: Encomendas, label: 'Nova Encomenda', section: 'encomendas' },
  'listar-encomendas': { component: Placeholder, label: 'Lista Encomendas', section: 'encomendas' },
  'relatorio-encomendas': { component: Placeholder, label: 'Relatorio Encomendas', section: 'encomendas' },
  'lancar-frete': { component: Fretes, label: 'Lancar Frete', section: 'fretes' },
  'listar-fretes': { component: Placeholder, label: 'Lista Fretes', section: 'fretes' },
  'relatorio-fretes': { component: Placeholder, label: 'Relatorio Fretes', section: 'fretes' },
  'financeiro-entrada': { component: Financeiro, label: 'Lancar Entrada', section: 'financeiro' },
  'financeiro-saida': { component: Placeholder, label: 'Lancar Saida', section: 'financeiro' },
  'balanco-viagem': { component: Placeholder, label: 'Balanco Viagem', section: 'financeiro' },
  'cadastro-viagem': { component: Placeholder, label: 'Cadastrar Viagem', section: 'cadastros' },
  'cadastro-usuario': { component: Placeholder, label: 'Usuarios', section: 'cadastros' },
  'cadastro-rota': { component: Placeholder, label: 'Rotas', section: 'cadastros' },
  'cadastro-tarifa': { component: Placeholder, label: 'Tarifas', section: 'cadastros' },
  'cadastro-empresa': { component: Placeholder, label: 'Empresa', section: 'cadastros' },
  'cadastro-embarcacao': { component: Placeholder, label: 'Embarcacoes', section: 'cadastros' },
  'cadastro-conferente': { component: Placeholder, label: 'Conferentes', section: 'cadastros' },
  'cadastro-caixa': { component: Placeholder, label: 'Caixas', section: 'cadastros' },
  'cadastro-produto': { component: Placeholder, label: 'Produtos/Servicos', section: 'cadastros' },
  'tabelas-auxiliares': { component: Placeholder, label: 'Tabelas Auxiliares', section: 'cadastros' },
  'gestao-funcionarios': { component: Placeholder, label: 'Gestao Funcionarios', section: 'cadastros' },
  'estorno-passagem': { component: Placeholder, label: 'Estorno Passagem', section: 'pagamentos' },
  'historico-estornos': { component: Placeholder, label: 'Historico Estornos', section: 'pagamentos' },
  'recibo-avulso': { component: Placeholder, label: 'Recibo Avulso', section: 'recibos' },
  agenda: { component: Placeholder, label: 'Agenda', section: 'sistema' },
  'configurar-api': { component: Placeholder, label: 'Configurar API', section: 'sistema' }
}

export default function Layout() {
  const [currentPage, setCurrentPage] = useState('inicio')
  const [viagens, setViagens] = useState([])
  const [viagemAtiva, setViagemAtiva] = useState(null)

  useEffect(() => {
    api.get('/op/viagens').then(setViagens).catch(() => {})
    api.get('/op/viagens/ativa').then(v => { if (v) setViagemAtiva(v) }).catch(() => {})
  }, [])

  const pageConfig = PAGES[currentPage] || PAGES.inicio
  const PageComponent = pageConfig.component

  return (
    <div className="app-layout">
      <Sidebar currentPage={currentPage} onNavigate={setCurrentPage} pages={PAGES} />
      <div className="main-content">
        <TopBar
          label={pageConfig.label}
          viagens={viagens}
          viagemAtiva={viagemAtiva}
          onViagemChange={setViagemAtiva}
        />
        <div className="page">
          <PageComponent
            key={currentPage}
            viagemAtiva={viagemAtiva}
            onNavigate={setCurrentPage}
          />
        </div>
      </div>
    </div>
  )
}

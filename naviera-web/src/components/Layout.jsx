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
  'lancar-frete': { component: Fretes, label: 'Lancar Frete', section: 'fretes' },
  'listar-fretes': { component: ListaFretes, label: 'Lista Fretes', section: 'fretes' },
  'relatorio-fretes': { component: RelatorioFretes, label: 'Relatorio Fretes', section: 'fretes' },
  'tabela-preco-frete': { component: TabelaPrecoFrete, label: 'Tabela Precos', section: 'fretes' },
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
  'gestao-funcionarios': { component: GestaoFuncionarios, label: 'Gestao Funcionarios', section: 'cadastros' },
  'estorno-passagem': { component: EstornoPassagem, label: 'Estorno Passagem', section: 'pagamentos' },
  'historico-estornos': { component: HistoricoEstornos, label: 'Historico Estornos', section: 'pagamentos' },
  'recibo-avulso': { component: ReciboAvulso, label: 'Recibo Avulso', section: 'recibos' },
  agenda: { component: Agenda, label: 'Agenda', section: 'sistema' },
  'configurar-api': { component: ConfigurarApi, label: 'Configurar API', section: 'sistema' },
  'admin-empresas': { component: AdminEmpresas, label: 'Gestao de Empresas', section: 'admin' },
  'admin-metricas': { component: AdminMetricas, label: 'Metricas da Plataforma', section: 'admin' }
}

export default function Layout() {
  const [currentPage, setCurrentPage] = useState('inicio')
  const [viagens, setViagens] = useState([])
  const [viagemAtiva, setViagemAtiva] = useState(null)

  useEffect(() => {
    api.get('/viagens').then(setViagens).catch(() => {})
    api.get('/viagens/ativa').then(v => { if (v) setViagemAtiva(v) }).catch(() => {})
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

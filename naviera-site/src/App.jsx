import { useState, useEffect, useRef, useCallback } from "react";

// DS4-042 fix: URL da API via env var (antes hardcoded)
const API_URL = import.meta.env.VITE_API_URL || "https://api.naviera.com.br/api";

const NAV_ITEMS = [
  { id: "empresas", label: "Para Empresas" },
  { id: "passageiros", label: "Para Passageiros" },
  { id: "funcionalidades", label: "Funcionalidades" },
  { id: "precos", label: "Preços" },
  { id: "contato", label: "Contato" },
];

const Logo = ({ light }) => (
  <svg width="120" height="40" viewBox="0 0 600 200" fill="none">
    <path d="M40 160 L40 52 Q40 42, 48 52 L88 108 Q96 120, 96 108 L96 52" stroke={light ? "#34D399" : "#059669"} strokeWidth="11" strokeLinecap="round" strokeLinejoin="round" fill="none"/>
    <path d="M96 108 Q96 120, 104 108 L136 62" stroke={light ? "#34D399" : "#059669"} strokeWidth="7" strokeLinecap="round" strokeLinejoin="round" fill="none" opacity="0.25"/>
    <circle cx="54" cy="178" r="3.5" fill={light ? "#34D399" : "#059669"} opacity="0.2"/>
    <circle cx="72" cy="178" r="3.5" fill={light ? "#34D399" : "#059669"} opacity="0.45"/>
    <circle cx="90" cy="178" r="3.5" fill={light ? "#34D399" : "#059669"} opacity="0.7"/>
    <circle cx="108" cy="178" r="3.5" fill={light ? "#34D399" : "#059669"}/>
    <text x="175" y="118" fontFamily="'Sora', sans-serif" fontSize="64" fontWeight="700" fill={light ? "#F0FDF4" : "#0F2620"} letterSpacing="8">NAVIERA</text>
  </svg>
);

const Arrow = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
);

const Check = () => (
  <span style={{width:18,height:18,borderRadius:'50%',background:'#ECFDF5',display:'inline-flex',alignItems:'center',justifyContent:'center',flexShrink:0}}>
    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#059669" strokeWidth="2.5"><polyline points="20 6 9 17 4 12"/></svg>
  </span>
);

const FeatureIcon = ({ children }) => (
  <div style={{width:52,height:52,borderRadius:14,background:'var(--icon-bg)',display:'flex',alignItems:'center',justifyContent:'center',marginBottom:20}}>
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--icon-stroke)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">{children}</svg>
  </div>
);

// ────────────────────────────────────────────
// PAGES
// ────────────────────────────────────────────

function HomePage({ go }) {
  return <>
    {/* HERO */}
    <section className="hero">
      <div className="hero-inner">
        <div className="badge badge-green fade-up" style={{marginBottom:24}}>Navegação Fluvial Inteligente</div>
        <h1 className="fade-up d1">O <em>ERP</em> que o<br/>barco precisava.</h1>
        <p className="hero-desc fade-up d2">Gestão completa de passageiros, fretes, encomendas e financeiro — no barco, no escritório e no celular do passageiro. Funciona até sem internet.</p>
        <div className="hero-actions fade-up d3">
          <button className="btn btn-pri" onClick={() => go('empresas')}>Sou operador de embarcação <Arrow/></button>
          <button className="btn btn-ghost" onClick={() => go('passageiros')}>Sou passageiro</button>
        </div>
        <div className="stats-bar fade-up d4">
          <Stat n="4" suffix=" plataformas" label="Desktop · Web · API · App"/>
          <Stat n="96" suffix=" telas" label="Interfaces funcionais"/>
          <Stat n="52" suffix=" tabelas" label="Banco de dados completo"/>
          <Stat n="100" suffix="%" label="Funciona offline"/>
        </div>
      </div>
    </section>

    {/* FEATURES */}
    <section className="sec">
      <div className="container">
        <SectionHeader badge="O Problema" badgeType="dark" title={<>43 municípios do Amazonas<br/>só têm acesso por rio.</>} desc="Mais de 2 milhões de pessoas dependem do transporte fluvial. Passagens vendidas sem padrão, fretes informais, zero controle financeiro. O Naviera resolve tudo isso."/>
        <div className="grid3">
          <Card title="Manifesto Digital" desc="Lista completa de passageiros com dados, acomodação e bilhete impresso. Fim do controle manual em caderno." icon={<><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></>}/>
          <Card title="Frete e Encomendas" desc="Cadastro, rastreio e controle financeiro de cargas com impressão de recibos e relatórios detalhados." icon={<path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z"/>}/>
          <Card title="Financeiro Completo" desc="Entradas, saídas, balanço por viagem, boletos, provisões de 13º, férias e auditoria de estornos." icon={<><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6"/></>}/>
          <Card title="GPS em Tempo Real" desc="Passageiros veem onde o barco está no mapa. Atualização a cada 30 segundos." icon={<><circle cx="12" cy="10" r="3"/><path d="M12 21.7C17.3 17 20 13 20 10a8 8 0 10-16 0c0 3 2.7 7 8 11.7z"/></>}/>
          <Card title="Sync Offline" desc="O Desktop funciona 100% sem internet. Quando a conexão volta, sincroniza automaticamente." icon={<><path d="M21.5 2v6h-6M2.5 22v-6h6"/><path d="M2.5 15.5A10 10 0 0121.5 8M21.5 8.5A10 10 0 012.5 16"/></>}/>
          <Card title="App para Passageiro" desc="Compra de passagem, bilhete digital com QR Code, rastreio de encomendas e GPS — tudo no celular." icon={<><rect x="5" y="2" width="14" height="20" rx="2"/><line x1="12" y1="18" x2="12.01" y2="18"/></>}/>
        </div>
      </div>
    </section>

    {/* ARCH */}
    <section className="sec sec-dark">
      <div className="container">
        <SectionHeader badge="Arquitetura" badgeType="green" title={<>4 plataformas.<br/>1 banco de dados.</>} desc="Todas as camadas conversam entre si. O Desktop opera offline e sincroniza quando há internet." inverted/>
        <div className="grid4">
          <ArchCard emoji="🖥️" title="Desktop" tech="JavaFX · Offline-first" desc="Console operacional a bordo. 51 telas, impressão térmica." color="rgba(52,211,153,.1)"/>
          <ArchCard emoji="🌐" title="Web" tech="React · Express BFF" desc="Espelho do Desktop para o escritório. 31 páginas CRUD." color="rgba(14,165,233,.1)"/>
          <ArchCard emoji="⚡" title="API" tech="Spring Boot · REST" desc="Backend multi-tenant com JWT, WebSocket e GPS." color="rgba(168,85,247,.1)"/>
          <ArchCard emoji="📱" title="App" tech="React PWA · Mobile" desc="Bilhete digital, mapa GPS, rastreio de encomendas." color="rgba(245,158,11,.1)"/>
        </div>
      </div>
    </section>

    {/* DUAL */}
    <section className="sec">
      <div className="container">
        <SectionHeader badge="Para Quem" badgeType="dark" title={<>Uma plataforma,<br/>duas jornadas.</>}/>
        <div className="grid2">
          <div className="dual-card dual-b2b">
            <div className="badge badge-green">Operadores · B2B</div>
            <h3>Controle total da sua embarcação</h3>
            <p>Substitua cadernos e planilhas por um sistema profissional que funciona até no meio do rio.</p>
            <ul>{["Desktop offline para uso a bordo","Web para gestão no escritório","Manifesto digital de passageiros","Controle financeiro por viagem","Impressão térmica de recibos","Relatórios e auditoria"].map(t=><li key={t}>{t}</li>)}</ul>
            <button className="btn btn-pri btn-sm" onClick={() => go('empresas')}>Conhecer o sistema <Arrow/></button>
          </div>
          <div className="dual-card dual-b2c">
            <div className="badge badge-dark">Passageiros · B2C</div>
            <h3>Viaje com informação na mão</h3>
            <p>Saiba onde seu barco está, compre passagem sem fila e rastreie suas encomendas — tudo pelo celular.</p>
            <ul>{["Compra de passagem pelo app","Bilhete digital com QR Code","GPS do barco em tempo real","Rastreio de encomendas","Histórico de viagens","Notificações e alertas"].map(t=><li key={t}>{t}</li>)}</ul>
            <button className="btn btn-pri btn-sm" onClick={() => go('passageiros')}>Baixar o app <Arrow/></button>
          </div>
        </div>
      </div>
    </section>

    {/* DIFFERENTIATORS */}
    <section className="sec sec-muted">
      <div className="container">
        <SectionHeader badge="Por Que Naviera" badgeType="dark" title={<>O que ninguém<br/>mais oferece.</>} desc="Concorrentes vendem passagem. Nós gerenciamos a operação inteira da embarcação."/>
        <div className="grid2">
          <Card title="🔌 Funciona Offline" desc="Sem internet no meio do rio? Sem problema. O Desktop opera 100% offline e sincroniza quando a conexão volta."/>
          <Card title="🏢 Multi-Tenant SaaS" desc="Cada empresa tem seu espaço isolado. Subdomínio próprio, dados separados, gestão independente."/>
          <Card title="📦 Mais que Passagens" desc="Fretes, encomendas com rastreio, balanço financeiro por viagem. O ERP completo da embarcação."/>
          <Card title="📍 GPS em Tempo Real" desc="Passageiros veem o barco no mapa do rio Amazonas. Atualização a cada 30 segundos."/>
        </div>
      </div>
    </section>

    <CTA title="Pronto para modernizar sua operação?" desc="Cadastre sua empresa e comece em minutos. 30 dias sem compromisso." btn="Cadastrar minha empresa" btnAction={() => go('cadastro')} btn2="Falar com a gente" btn2Action={() => go('contato')}/>
  </>;
}

function EmpresasPage({ go }) {
  return <>
    <section className="hero hero-short">
      <div className="hero-inner">
        <div className="badge badge-green fade-up" style={{marginBottom:24}}>Para Operadores</div>
        <h1 className="fade-up d1" style={{fontSize:'clamp(32px, 5vw, 60px)'}}>Seu barco merece<br/>um <em>sistema de verdade</em>.</h1>
        <p className="hero-desc fade-up d2">Chega de caderno, planilha e controle no WhatsApp. O Naviera é o ERP completo — e funciona até sem internet.</p>
        <div className="hero-actions fade-up d3">
          <button className="btn btn-pri" onClick={() => go('cadastro')}>Cadastrar minha empresa</button>
          <button className="btn btn-ghost" onClick={() => go('precos')}>Ver preços</button>
        </div>
      </div>
    </section>
    <section className="sec">
      <div className="container">
        <SectionHeader badge="Como Funciona" badgeType="dark" title={<>Três ferramentas.<br/>Uma operação integrada.</>} desc="Desktop no barco, Web no escritório, App no bolso do passageiro. Tudo conectado."/>
        <div className="grid3">
          <Card title="🖥️ Desktop no Barco" desc="Vende passagens, cadastra fretes e encomendas, imprime recibos na térmica. Funciona 100% offline." accent="#059669"/>
          <Card title="🌐 Web no Escritório" desc="Relatórios, financeiro, cadastros e viagens ativas. Mesmo sistema, mesmos dados — sem instalar nada." accent="#0EA5E9"/>
          <Card title="📱 App no Bolso" desc="Passageiros compram passagem, recebem bilhete digital e rastreiam o barco. Lojas despacham encomendas." accent="#F59E0B"/>
        </div>
      </div>
    </section>
    <section className="sec sec-dark">
      <div className="container">
        <SectionHeader badge="Desktop" badgeType="green" title={<>51 telas para gerenciar<br/>tudo a bordo.</>} inverted/>
        <div className="grid3">
          {[{t:"Venda de Passagens",d:"Cadastro completo com documento, acomodação, rota, pagamento e impressão do bilhete."},{t:"Fretes e Encomendas",d:"Itens, volumes, peso, valores. Pagamento parcial, quitação e recibos."},{t:"Financeiro",d:"Entradas, saídas, balanço por viagem, boletos, provisão de 13º e férias."},{t:"Estornos e Auditoria",d:"Histórico completo de estornos com motivo e log de auditoria."},{t:"Impressão Térmica",d:"Bilhetes, recibos de frete e encomenda direto na térmica 80mm."},{t:"Sync Inteligente",d:"Sincronização bidirecional automática quando o barco pega internet."}].map(f => <Card key={f.t} title={f.t} desc={f.d} dark/>)}
        </div>
      </div>
    </section>
    <section className="sec">
      <div className="container">
        <SectionHeader badge="Começar" badgeType="dark" title={<>Em 3 passos você<br/>já está operando.</>}/>
        <div className="grid3">
          {[{n:"01",t:"Baixe e instale",d:"Download gratuito para Windows ou Linux. Instalação em 2 minutos."},{n:"02",t:"Configure sua empresa",d:"Cadastre embarcação, rotas, tarifas e funcionários."},{n:"03",t:"Comece a operar",d:"Venda passagens, cadastre fretes e acompanhe o financeiro."}].map(s => (
            <div key={s.n} className="card" style={{textAlign:'center'}}>
              <div style={{fontSize:48,fontWeight:800,color:'#059669',marginBottom:16,fontFamily:"'Space Mono', monospace"}}>{s.n}</div>
              <h3>{s.t}</h3><p>{s.d}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
    <CTA title="Pronto para deixar o caderno de lado?" desc="Cadastre sua empresa e comece a gerenciar sua embarcação." btn="Cadastrar minha empresa" btnAction={() => go('cadastro')}/>
  </>;
}

function PassageirosPage({ go }) {
  return <>
    <section className="hero hero-short">
      <div className="hero-inner">
        <div className="badge badge-green fade-up" style={{marginBottom:24}}>Para Passageiros</div>
        <h1 className="fade-up d1" style={{fontSize:'clamp(32px, 5vw, 60px)'}}>Saiba onde seu barco<br/>está. <em>Sempre.</em></h1>
        <p className="hero-desc fade-up d2">Compre passagem sem fila, receba bilhete digital no celular, rastreie encomendas e veja o barco no mapa em tempo real.</p>
        <div className="hero-actions fade-up d3">
          <button className="btn btn-pri" onClick={() => go('download')}>Baixar o App — Grátis</button>
        </div>
      </div>
    </section>
    <section className="sec">
      <div className="container">
        <SectionHeader badge="O Que Você Pode Fazer" badgeType="dark" title={<>Tudo pelo celular.<br/>Sem fila, sem papel.</>}/>
        <div className="grid3">
          {[{t:"Compra de Passagem",d:"Escolha rota, data e acomodação. Pague pelo app e receba o bilhete digital."},{t:"Bilhete Digital",d:"Seu bilhete fica no celular com QR Code seguro (TOTP). Apresente no embarque."},{t:"GPS em Tempo Real",d:"Veja no mapa exatamente onde o barco está. Atualização a cada 30 segundos."},{t:"Rastreio de Encomendas",d:"Despache encomendas e acompanhe o status em tempo real."},{t:"Lista de Amigos",d:"Adicione amigos que viajam com você. Compartilhe viagens e combine encontros."},{t:"Notificações",d:"Alertas sobre horários, mudanças de rota, chegada e status de encomendas."}].map(f => <Card key={f.t} title={f.t} desc={f.d}/>)}
        </div>
      </div>
    </section>
    <section className="sec sec-muted">
      <div className="container">
        <SectionHeader badge="Lojas Parceiras" badgeType="dark" title={<>Tem um comércio?<br/>Despache pelo app.</>} desc="Lojas (CNPJ) têm painel dedicado para gerenciar despachos, entregas e financeiro."/>
        <div className="grid2">
          <Card title="📦 Gestão de Pedidos" desc="Crie pedidos, vincule com fretes, acompanhe cada despacho por viagem e data."/>
          <Card title="💰 Extrato Financeiro" desc="Gastos com fretes, recebimentos, histórico de pagamentos e notas fiscais."/>
          <Card title="🤝 Rede de Parceiros" desc="Conecte-se com outras lojas. Negocie fretes compartilhados e reduza custos."/>
          <Card title="⭐ Avaliações" desc="Avalie o serviço de cada embarcação. Ajude outros comerciantes a escolher."/>
        </div>
      </div>
    </section>
    <CTA title="Baixe o app e viaje com informação." desc="Gratuito para passageiros e lojas. Cadastre-se com CPF ou CNPJ." btn="Baixar Grátis" btnAction={() => go('download')}/>
  </>;
}

function FuncionalidadesPage({ go }) {
  return <>
    <section className="hero hero-short">
      <div className="hero-inner">
        <div className="badge badge-green fade-up" style={{marginBottom:24}}>Funcionalidades</div>
        <h1 className="fade-up d1" style={{fontSize:'clamp(32px, 5vw, 56px)'}}>Tudo que você precisa.<br/><em>Nada que não precisa.</em></h1>
        <p className="hero-desc fade-up d2">96 telas, 190 endpoints, 52 tabelas. Cada feature resolve um problema real.</p>
      </div>
    </section>
    <section className="sec">
      <div className="container">
        <SectionHeader badge="Operacional" badgeType="dark" title="Gestão da embarcação" desc="Tudo que acontece a bordo — de passageiros a cargas — organizado e registrado."/>
        <div className="grid3">
          {[{t:"🎫 Passagens",d:"Venda com dados completos, múltiplas rotas, acomodações, descontos. Bilhete impresso ou digital."},{t:"📦 Encomendas",d:"Remetente, destinatário, itens, volumes, peso e valores. Pagamento parcial e quitação."},{t:"🚛 Fretes",d:"Nota fiscal, peso, local de transporte, cidade de cobrança. Itens com preço unitário."},{t:"🚢 Viagens",d:"Controle de viagens ativas/encerradas. Embarcação, rota, horário, tripulação."},{t:"👥 Tripulação",d:"Funcionários, conferentes e caixas. Permissões por função e auditoria."},{t:"🖨️ Impressão",d:"Bilhetes e recibos na térmica 80mm. JasperReports + PDF."}].map(f => <Card key={f.t} title={f.t} desc={f.d}/>)}
        </div>
      </div>
    </section>
    <section className="sec sec-muted">
      <div className="container">
        <SectionHeader badge="Financeiro" badgeType="dark" title="Controle total do dinheiro" desc="Saiba exatamente quanto entrou, quanto saiu e quanto sobrou — por viagem."/>
        <div className="grid2">
          <Card title="📊 Balanço por Viagem" desc="Entradas vs. saídas. Resultado líquido por viagem."/>
          <Card title="🧾 Boletos e Parcelas" desc="Geração de boletos, controle de vencimento e inadimplência."/>
          <Card title="↩️ Estornos" desc="Estorno com motivo registrado. Histórico completo com log de auditoria."/>
          <Card title="📋 Relatórios" desc="Relatório geral por período, rota, cliente. Exportação PDF."/>
        </div>
      </div>
    </section>
    <section className="sec sec-dark">
      <div className="container">
        <SectionHeader badge="Tecnologia" badgeType="green" title="Infraestrutura robusta" desc="Construído para funcionar sem internet, com baixa banda, em dispositivos simples." inverted/>
        <div className="grid3">
          {[{t:"🔌 Offline-First",d:"Desktop com PostgreSQL local. Sync automático quando a conexão volta."},{t:"🔄 Sync Bidirecional",d:"Dados sobem e descem. Last-write-wins com resolução de conflitos."},{t:"🏢 Multi-Tenant",d:"Dados isolados por empresa. Subdomínio próprio, JWT com empresa_id."},{t:"📍 GPS Tracking",d:"Tripulação envia posição via app. Passageiros veem no mapa SVG."},{t:"🔔 Push & WebSocket",d:"Firebase FCM + WebSocket STOMP/SockJS. Alertas em tempo real."},{t:"🐳 Docker Deploy",d:"Docker Compose + Nginx wildcard SSL. Deploy em minutos."}].map(f => <Card key={f.t} title={f.t} desc={f.d} dark/>)}
        </div>
      </div>
    </section>
    <CTA title="Quer ver na prática?" desc="Baixe o Desktop e explore o sistema." btn="Baixar o Desktop" btnAction={() => go('download')} btn2="Falar conosco" btn2Action={() => go('contato')}/>
  </>;
}

function PrecosPage({ go }) {
  return <>
    <section className="hero hero-short">
      <div className="hero-inner" style={{textAlign:'center'}}>
        <div className="badge badge-green fade-up" style={{marginBottom:24}}>Preços</div>
        <h1 className="fade-up d1" style={{fontSize:'clamp(32px, 5vw, 56px)'}}>Simples. <em>Transparente.</em></h1>
        <p className="hero-desc fade-up d2" style={{margin:'0 auto'}}>App gratuito para passageiros. Para operadores, um preço justo por embarcação.</p>
      </div>
    </section>
    <section className="sec" style={{paddingTop:0}}>
      <div className="container">
        <div className="pricing-grid">
          <PricingCard title="Passageiro" price="Grátis" priceDesc="Para sempre. Sem limites." features={["Compra de passagens","Bilhete digital QR Code","GPS em tempo real","Rastreio de encomendas","Notificações push","Lista de amigos"]} btnLabel="Baixar o App" btnAction={() => go('download')} ghost/>
          <PricingCard title="Operador" price="R$ 299" priceSuffix="/mês" priceDesc="Por embarcação. Tudo incluso." features={["Desktop offline completo","Web para escritório","Passagens, fretes e encomendas","Financeiro completo","GPS tracking","Sync offline automático","Impressão térmica","Relatórios e auditoria","Suporte prioritário"]} btnLabel="Começar agora" btnAction={() => go('cadastro')} featured/>
          <PricingCard title="Frota" price="Sob consulta" priceDesc="Para 3+ embarcações." features={["Tudo do plano Operador","Painel admin centralizado","Métricas consolidadas","Desconto por volume","Onboarding dedicado","Subdomínio personalizado"]} btnLabel="Falar com vendas" btnAction={() => go('contato')} ghost/>
        </div>
      </div>
    </section>
    <section className="sec sec-muted">
      <div className="container">
        <SectionHeader title="Perguntas frequentes"/>
        <div style={{maxWidth:680,margin:'0 auto'}}>
          {[{q:"Tem contrato de fidelidade?",a:"Não. Plano mensal sem fidelidade. Cancele quando quiser, sem multa."},{q:"Posso testar antes de pagar?",a:"Sim. Os primeiros 30 dias são gratuitos. Só começa a pagar quando decidir continuar."},{q:"O que está incluso no preço?",a:"Tudo. Desktop, Web, API, App, sync, GPS, impressão, relatórios, suporte."},{q:"Cobram taxa por passagem vendida?",a:"Não. Valor fixo por embarcação, independente do volume de vendas."},{q:"Funciona sem internet?",a:"Sim. Desktop opera 100% offline. App funciona offline via PWA com service worker."}].map(f => <div key={f.q} className="card" style={{marginBottom:16}}><h3>{f.q}</h3><p>{f.a}</p></div>)}
        </div>
      </div>
    </section>
    <CTA title="30 dias grátis. Sem cartão." desc="Cadastre sua empresa e teste o Naviera sem compromisso." btn="Começar gratuitamente" btnAction={() => go('cadastro')}/>
  </>;
}

function DownloadPage({ go }) {
  const [os, setOs] = useState('unknown');
  useEffect(() => {
    const ua = navigator.userAgent.toLowerCase();
    if (ua.includes('win')) setOs('windows');
    else if (ua.includes('linux')) setOs('linux');
    else if (ua.includes('mac')) setOs('mac');
    else if (ua.includes('android')) setOs('android');
    else if (ua.includes('iphone') || ua.includes('ipad')) setOs('ios');
  }, []);

  const isDesktop = os === 'windows' || os === 'linux' || os === 'mac' || os === 'unknown';
  const isMobile = os === 'android' || os === 'ios';

  return <>
    <section className="hero hero-short">
      <div className="hero-inner" style={{textAlign:'center'}}>
        <div className="badge badge-green fade-up" style={{marginBottom:24}}>Download</div>
        <h1 className="fade-up d1" style={{fontSize:'clamp(32px, 5vw, 56px)'}}>Baixe. Instale. <em>Use.</em></h1>
        <p className="hero-desc fade-up d2" style={{margin:'0 auto'}}>
          {isDesktop ? 'Detectamos que você usa ' + (os === 'windows' ? 'Windows' : os === 'linux' ? 'Linux' : 'Desktop') + '.' : 'Baixe o app no seu celular.'}
        </p>

        {/* Botão principal — detecta SO */}
        <div className="fade-up d3" style={{marginTop:32}}>
          {os === 'windows' && <a className="btn btn-pri" href="/downloads/naviera-desktop.msi" download style={{fontSize:18,padding:'20px 40px'}}>⬇ Baixar para Windows (.msi)</a>}
          {os === 'linux' && <a className="btn btn-pri" href="/downloads/naviera-desktop.deb" download style={{fontSize:18,padding:'20px 40px'}}>⬇ Baixar para Linux (.deb)</a>}
          {os === 'android' && <a className="btn btn-pri" href="https://app.naviera.com.br" target="_blank" rel="noopener noreferrer" style={{fontSize:18,padding:'20px 40px'}}>⬇ Instalar App (PWA)</a>}
          {os === 'ios' && <a className="btn btn-pri" href="https://app.naviera.com.br" target="_blank" rel="noopener noreferrer" style={{fontSize:18,padding:'20px 40px'}}>⬇ Instalar App (PWA)</a>}
          {(os === 'unknown' || os === 'mac') && <a className="btn btn-pri" href="/downloads/naviera-desktop.msi" download style={{fontSize:18,padding:'20px 40px'}}>⬇ Baixar para Windows (.msi)</a>}
        </div>
        {isDesktop && <p className="fade-up d4" style={{marginTop:12,fontSize:13,color:'rgba(255,255,255,.35)'}}>
          Também disponível para {os === 'windows' ? <a onClick={(e) => {e.preventDefault(); window.location.href='/downloads/naviera-desktop.deb'}} style={{color:'var(--pri-light)',cursor:'pointer'}}>Linux (.deb)</a> : <a onClick={(e) => {e.preventDefault(); window.location.href='/downloads/naviera-desktop.msi'}} style={{color:'var(--pri-light)',cursor:'pointer'}}>Windows (.msi)</a>}
        </p>}
      </div>
    </section>

    <section className="sec sec-dark" style={{paddingTop:0}}>
      <div className="container">
        <div className="grid2">
          <div className="dl-card dl-dark">
            <div className="badge badge-green" style={{marginBottom:20}}>Para Operadores</div>
            <h3 style={{color:'#F0FDF4',fontSize:24,marginBottom:8}}>Naviera Desktop</h3>
            <p style={{color:'rgba(255,255,255,.5)',marginBottom:24}}>Software completo para gestão a bordo. Instale e use em minutos.</p>
            <div className="dl-buttons">
              <DlBtn title="Windows (.msi)" sub="Windows 10+ · 64-bit" dark href="/downloads/naviera-desktop.msi" primary={os === 'windows'}/>
              <DlBtn title="Linux (.deb)" sub="Ubuntu 22.04+ / Debian 12+" dark href="/downloads/naviera-desktop.deb" primary={os === 'linux'}/>
            </div>
            <div className="dl-reqs">
              <h4>Requisitos Mínimos</h4>
              <ul><li>4 GB RAM / 500 MB disco</li><li>PostgreSQL 14+ (pode instalar junto)</li><li>Impressora térmica 80mm (opcional)</li></ul>
            </div>
          </div>
          <div className="dl-card dl-light">
            <div className="badge badge-dark" style={{marginBottom:20}}>Para Passageiros</div>
            <h3 style={{color:'#0F2620',fontSize:24,marginBottom:8}}>Naviera App</h3>
            <p style={{color:'#3D6B56',marginBottom:24}}>Compre passagens, veja o GPS e rastreie encomendas.</p>
            <div className="dl-buttons">
              <DlBtn title="PWA (Web App)" sub="Funciona em qualquer navegador" href="https://app.naviera.com.br" primary={isMobile}/>
              <DlBtn title="App Store" sub="Em breve — iPhone e iPad" href="#"/>
              <DlBtn title="Google Play" sub="Em breve — Android 8.0+" href="#"/>
            </div>
            <div className="dl-reqs dl-reqs-light">
              <h4>Instalação Rápida</h4>
              <ul><li>Acesse app.naviera.com.br no celular</li><li>Clique em "Instalar" no banner</li><li>Pronto — funciona como app nativo</li><li>Cadastro com CPF ou CNPJ</li></ul>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section className="sec">
      <div className="container" style={{maxWidth:680,textAlign:'center'}}>
        <div className="badge badge-dark">Acesso Web</div>
        <h2 style={{fontSize:'clamp(24px,3.5vw,36px)',fontWeight:700,margin:'16px 0',lineHeight:1.15}}>Não precisa instalar nada?<br/>Use pelo navegador.</h2>
        <p style={{color:'#3D6B56',marginBottom:32}}>Operadores também podem acessar pelo navegador no subdomínio da empresa.</p>
        <div className="card" style={{textAlign:'left'}}>
          <h3 style={{fontFamily:"'Space Mono', monospace",fontSize:14,color:'#059669',marginBottom:12}}>suaempresa.naviera.com.br</h3>
          <p>Acesse o painel web com login e senha. Mesmos dados do Desktop, qualquer navegador moderno.</p>
        </div>
      </div>
    </section>
    <CTA title="Dúvidas sobre a instalação?" desc="Fale com a gente pelo WhatsApp." btn="Falar com suporte" btnAction={() => go('contato')}/>
  </>;
}

function ContatoPage() {
  return <>
    <section className="hero hero-short">
      <div className="hero-inner">
        <div className="badge badge-green fade-up" style={{marginBottom:24}}>Contato</div>
        <h1 className="fade-up d1" style={{fontSize:'clamp(32px, 5vw, 56px)'}}>Fale com <em>a gente</em>.</h1>
        <p className="hero-desc fade-up d2">Dúvidas, sugestões, suporte técnico ou interesse comercial.</p>
      </div>
    </section>
    <section className="sec" style={{paddingTop:0}}>
      <div className="container">
        <div className="grid2" style={{alignItems:'start'}}>
          <div>
            <div className="badge badge-dark" style={{marginBottom:24}}>Nossos Canais</div>
            {[{icon:"💬",title:"WhatsApp",value:"(92) 00000-0000"},{icon:"✉️",title:"Email",value:"suporte@naviera.com.br"},{icon:"📍",title:"Localização",value:"Manaus, AM — Brasil"},{icon:"🕐",title:"Atendimento",value:"Segunda a sexta, 8h às 18h"}].map(c => (
              <div key={c.title} style={{display:'flex',alignItems:'center',gap:16,marginBottom:24}}>
                <div style={{width:48,height:48,borderRadius:14,background:'#ECFDF5',display:'flex',alignItems:'center',justifyContent:'center',fontSize:22,flexShrink:0}}>{c.icon}</div>
                <div>
                  <div style={{fontSize:11,fontWeight:700,letterSpacing:'1px',textTransform:'uppercase',color:'#7BA393',fontFamily:"'Space Mono', monospace"}}>{c.title}</div>
                  <div style={{fontSize:15,fontWeight:600,color:'#0F2620'}}>{c.value}</div>
                </div>
              </div>
            ))}
          </div>
          <div>
            <div style={{background:'linear-gradient(160deg, #040D0A, #0F2D24)',borderRadius:32,padding:'40px 32px',color:'#F0FDF4'}}>
              <h3 style={{fontSize:22,fontWeight:700,marginBottom:8}}>Prefere falar direto?</h3>
              <p style={{color:'rgba(255,255,255,.5)',marginBottom:24,fontSize:15,lineHeight:1.7}}>O jeito mais rápido é pelo WhatsApp. Respondemos em minutos durante o horário comercial.</p>
              <button className="btn btn-pri" style={{width:'100%',justifyContent:'center',background:'#25D366',marginBottom:20}}>💬 Chamar no WhatsApp</button>
              <div style={{borderTop:'1px solid rgba(255,255,255,.06)',paddingTop:20}}>
                <h4 style={{fontSize:14,fontWeight:600,marginBottom:12}}>Assuntos comuns:</h4>
                <div style={{display:'flex',flexWrap:'wrap',gap:8}}>
                  {["Dúvidas sobre o sistema","Suporte técnico","Interesse comercial","Parceria / Investimento","Imprensa"].map(s => (
                    <span key={s} style={{fontSize:12,padding:'6px 14px',borderRadius:100,background:'rgba(255,255,255,.05)',border:'1px solid rgba(255,255,255,.08)',color:'rgba(255,255,255,.5)'}}>{s}</span>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </>;
}

function CadastroPage({ go }) {
  const [form, setForm] = useState({ nome_empresa:'', cnpj:'', nome_embarcacao:'', telefone:'', email:'', nome_operador:'', senha:'', confirmar:'' });
  const [erro, setErro] = useState('');
  const [salvando, setSalvando] = useState(false);
  const [resultado, setResultado] = useState(null);

  function handle(e) { setForm(f => ({...f, [e.target.name]: e.target.value})); setErro(''); }

  function formatCnpj(v) {
    const d = v.replace(/\D/g,'').slice(0,14);
    if (d.length<=2) return d;
    if (d.length<=5) return d.slice(0,2)+'.'+d.slice(2);
    if (d.length<=8) return d.slice(0,2)+'.'+d.slice(2,5)+'.'+d.slice(5);
    if (d.length<=12) return d.slice(0,2)+'.'+d.slice(2,5)+'.'+d.slice(5,8)+'/'+d.slice(8);
    return d.slice(0,2)+'.'+d.slice(2,5)+'.'+d.slice(5,8)+'/'+d.slice(8,12)+'-'+d.slice(12);
  }

  function formatTel(v) {
    const d = v.replace(/\D/g,'').slice(0,11);
    if (d.length<=2) return d.length?'('+d:d;
    if (d.length<=7) return '('+d.slice(0,2)+') '+d.slice(2);
    return '('+d.slice(0,2)+') '+d.slice(2,7)+'-'+d.slice(7);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    if (!form.nome_empresa.trim()) { setErro('Informe o nome da empresa'); return; }
    if (!form.email.trim() || !form.email.includes('@')) { setErro('Informe um email valido'); return; }
    if (!form.nome_operador.trim()) { setErro('Informe o nome do responsavel'); return; }
    if (form.senha.length < 6) { setErro('A senha deve ter no minimo 6 caracteres'); return; }
    if (form.senha !== form.confirmar) { setErro('As senhas nao conferem'); return; }

    setSalvando(true); setErro('');
    try {
      const resp = await fetch(`${API_URL}/public/registrar-empresa`, {
        method:'POST', headers:{'Content-Type':'application/json'},
        body: JSON.stringify({
          nome_empresa: form.nome_empresa.trim(),
          cnpj: form.cnpj.replace(/\D/g,'') || null,
          nome_embarcacao: form.nome_embarcacao.trim() || null,
          telefone: form.telefone.trim() || null,
          email: form.email.trim().toLowerCase(),
          nome_operador: form.nome_operador.trim(),
          senha: form.senha
        })
      });
      const data = await resp.json();
      if (!resp.ok) throw new Error(data.error || data.message || 'Erro ao cadastrar');
      setResultado(data);
    } catch(err) {
      setErro(err.message || 'Erro de conexao. Verifique sua internet.');
    } finally { setSalvando(false); }
  }

  // Tela de sucesso com codigo
  if (resultado) {
    return <>
      <section className="hero hero-short">
        <div className="hero-inner" style={{textAlign:'center'}}>
          <div className="badge badge-green fade-up" style={{marginBottom:24}}>Empresa Cadastrada!</div>
          <h1 className="fade-up d1" style={{fontSize:'clamp(32px, 5vw, 56px)'}}>Seu codigo de <em>ativacao</em></h1>
        </div>
      </section>
      <section className="sec" style={{paddingTop:0}}>
        <div className="container" style={{maxWidth:560,textAlign:'center'}}>
          <div style={{background:'linear-gradient(160deg, #040D0A, #0F2D24)',borderRadius:24,padding:'40px 32px',color:'#F0FDF4',marginBottom:32}}>
            <p style={{color:'rgba(255,255,255,.5)',fontSize:14,marginBottom:8}}>Anote ou copie este codigo:</p>
            <div style={{fontSize:48,fontWeight:800,color:'#34D399',letterSpacing:8,fontFamily:"'Space Mono', monospace",margin:'16px 0'}}>{resultado.codigo_ativacao}</div>
            <p style={{color:'rgba(255,255,255,.5)',fontSize:13,marginBottom:20}}>Voce vai usar este codigo na primeira vez que abrir o Naviera Desktop.</p>
            <button className="btn btn-pri btn-sm" style={{background:'rgba(255,255,255,.1)',border:'1px solid rgba(255,255,255,.15)'}} onClick={() => {
              navigator.clipboard.writeText(resultado.codigo_ativacao);
            }}>Copiar codigo</button>
          </div>

          <div className="card" style={{textAlign:'left',marginBottom:24}}>
            <h3 style={{marginBottom:12}}>Proximos passos:</h3>
            <div style={{display:'flex',flexDirection:'column',gap:12}}>
              {[
                {n:'1',t:'Baixe o instalador',d:'Clique no botao abaixo para baixar o Naviera Desktop.'},
                {n:'2',t:'Instale no computador',d:'Execute o instalador normalmente (.deb no Linux, .msi no Windows).'},
                {n:'3',t:'Digite o codigo',d:'Na primeira abertura, digite o codigo ' + resultado.codigo_ativacao + ' e pronto!'}
              ].map(s => (
                <div key={s.n} style={{display:'flex',gap:12,alignItems:'flex-start'}}>
                  <div style={{width:28,height:28,borderRadius:'50%',background:'#ECFDF5',display:'flex',alignItems:'center',justifyContent:'center',flexShrink:0,fontWeight:700,color:'#059669',fontSize:13}}>{s.n}</div>
                  <div><div style={{fontWeight:700,fontSize:14}}>{s.t}</div><div style={{fontSize:13,color:'#3D6B56'}}>{s.d}</div></div>
                </div>
              ))}
            </div>
          </div>

          <div style={{display:'flex',gap:16,justifyContent:'center',flexWrap:'wrap'}}>
            <button className="btn btn-pri" onClick={() => go('download')}>Baixar o Desktop</button>
            <button className="btn btn-ghost-dark" onClick={() => go('contato')}>Falar com suporte</button>
          </div>

          <div style={{marginTop:32,padding:16,borderRadius:12,background:'#ECFDF5'}}>
            <p style={{fontSize:13,color:'#3D6B56',margin:0}}>
              <strong>Seus dados:</strong> Empresa: {resultado.nome} | Slug: {resultado.slug}.naviera.com.br | Login: {resultado.email}
            </p>
          </div>
        </div>
      </section>
    </>;
  }

  // Formulario
  const inputStyle = {width:'100%',padding:'12px 16px',borderRadius:10,border:'1.5px solid rgba(5,150,105,.2)',fontSize:15,fontFamily:"'Sora', sans-serif",outline:'none',transition:'border .2s',background:'white'};

  return <>
    <section className="hero hero-short">
      <div className="hero-inner" style={{textAlign:'center'}}>
        <div className="badge badge-green fade-up" style={{marginBottom:24}}>Cadastro</div>
        <h1 className="fade-up d1" style={{fontSize:'clamp(32px, 5vw, 56px)'}}>Cadastre sua <em>empresa</em></h1>
        <p className="hero-desc fade-up d2" style={{margin:'0 auto'}}>Preencha os dados abaixo e receba seu codigo de ativacao para instalar o Naviera Desktop.</p>
      </div>
    </section>
    <section className="sec" style={{paddingTop:0}}>
      <div className="container" style={{maxWidth:600}}>
        <div className="card" style={{padding:'36px 32px'}}>
          <div onSubmit={handleSubmit} style={{display:'flex',flexDirection:'column',gap:20}}>

            <div>
              <label style={{fontSize:13,fontWeight:600,color:'#0F2620',display:'block',marginBottom:6}}>Nome da empresa *</label>
              <input name="nome_empresa" value={form.nome_empresa} onChange={handle} placeholder="Ex: Navegacoes Sao Francisco" style={inputStyle} />
            </div>

            <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:16}}>
              <div>
                <label style={{fontSize:13,fontWeight:600,color:'#0F2620',display:'block',marginBottom:6}}>CNPJ</label>
                <input name="cnpj" value={form.cnpj} onChange={e => { setForm(f=>({...f,cnpj:formatCnpj(e.target.value)})); setErro(''); }} placeholder="00.000.000/0000-00" style={inputStyle} />
              </div>
              <div>
                <label style={{fontSize:13,fontWeight:600,color:'#0F2620',display:'block',marginBottom:6}}>Nome da embarcacao</label>
                <input name="nome_embarcacao" value={form.nome_embarcacao} onChange={handle} placeholder="Ex: MV Sao Francisco II" style={inputStyle} />
              </div>
            </div>

            <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:16}}>
              <div>
                <label style={{fontSize:13,fontWeight:600,color:'#0F2620',display:'block',marginBottom:6}}>Telefone / WhatsApp</label>
                <input name="telefone" value={form.telefone} onChange={e => { setForm(f=>({...f,telefone:formatTel(e.target.value)})); setErro(''); }} placeholder="(92) 99999-9999" style={inputStyle} />
              </div>
              <div>
                <label style={{fontSize:13,fontWeight:600,color:'#0F2620',display:'block',marginBottom:6}}>Email *</label>
                <input name="email" type="email" value={form.email} onChange={handle} placeholder="operador@email.com" style={inputStyle} />
              </div>
            </div>

            <div style={{borderTop:'1.5px solid rgba(5,150,105,.1)',paddingTop:20}}>
              <p style={{fontSize:13,color:'#7BA393',marginBottom:16}}>Dados do responsavel (primeiro usuario do sistema)</p>
            </div>

            <div>
              <label style={{fontSize:13,fontWeight:600,color:'#0F2620',display:'block',marginBottom:6}}>Nome do responsavel *</label>
              <input name="nome_operador" value={form.nome_operador} onChange={handle} placeholder="Nome completo" style={inputStyle} />
            </div>

            <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:16}}>
              <div>
                <label style={{fontSize:13,fontWeight:600,color:'#0F2620',display:'block',marginBottom:6}}>Criar senha *</label>
                <input name="senha" type="password" value={form.senha} onChange={handle} placeholder="Minimo 6 caracteres" style={inputStyle} />
              </div>
              <div>
                <label style={{fontSize:13,fontWeight:600,color:'#0F2620',display:'block',marginBottom:6}}>Confirmar senha *</label>
                <input name="confirmar" type="password" value={form.confirmar} onChange={handle} placeholder="Repita a senha" style={inputStyle} />
              </div>
            </div>

            {erro && <div style={{padding:'10px 16px',borderRadius:8,background:'#FEF2F2',border:'1px solid #FECACA',color:'#991B1B',fontSize:13}}>{erro}</div>}

            <button type="button" className="btn btn-pri" style={{width:'100%',justifyContent:'center',marginTop:8}} disabled={salvando} onClick={handleSubmit}>
              {salvando ? 'Cadastrando...' : 'Cadastrar e receber codigo de ativacao'}
            </button>

            <p style={{fontSize:12,color:'#7BA393',textAlign:'center'}}>Ao cadastrar, voce concorda com os termos de uso e politica de privacidade.</p>
          </div>
        </div>
      </div>
    </section>
  </>;
}

// ────────────────────────────────────────────
// SHARED COMPONENTS
// ────────────────────────────────────────────

function Stat({ n, suffix, label }) {
  const [val, setVal] = useState(0);
  useEffect(() => {
    const t = parseInt(n);
    const dur = 1800;
    const start = performance.now();
    function tick(now) {
      const p = Math.min((now - start) / dur, 1);
      setVal(Math.floor(t * (1 - Math.pow(1 - p, 3))));
      if (p < 1) requestAnimationFrame(tick);
    }
    const timer = setTimeout(() => requestAnimationFrame(tick), 400);
    return () => clearTimeout(timer);
  }, [n]);
  return <div className="stat"><div className="stat-num">{val}{suffix}</div><div className="stat-label">{label}</div></div>;
}

function SectionHeader({ badge, badgeType, title, desc, inverted }) {
  return (
    <div className="sec-header">
      {badge && <div className={`badge badge-${badgeType}`}>{badge}</div>}
      <h2 className="sec-title" style={inverted ? {color:'#F0FDF4'} : {}}>{title}</h2>
      {desc && <p className="sec-desc">{desc}</p>}
    </div>
  );
}

function Card({ title, desc, icon, dark, accent }) {
  return (
    <div className={`card ${dark ? 'card-dark' : ''}`} style={accent ? {borderLeft:`3px solid ${accent}`} : {}}>
      {icon && <FeatureIcon>{icon}</FeatureIcon>}
      <h3>{title}</h3>
      <p>{desc}</p>
    </div>
  );
}

function ArchCard({ emoji, title, tech, desc, color }) {
  return (
    <div className="arch-card">
      <div className="arch-icon" style={{background: color}}>{emoji}</div>
      <h4>{title}</h4>
      <div className="arch-tech">{tech}</div>
      <p>{desc}</p>
    </div>
  );
}

function PricingCard({ title, price, priceSuffix, priceDesc, features, btnLabel, btnAction, featured, ghost }) {
  return (
    <div className={`pricing-card ${featured ? 'pricing-featured' : ''}`}>
      {featured && <div className="pricing-popular">POPULAR</div>}
      <h3>{title}</h3>
      <div className="pricing-price">{price}{priceSuffix && <small>{priceSuffix}</small>}</div>
      <div className="pricing-desc">{priceDesc}</div>
      <ul className="pricing-features">{features.map(f => <li key={f}><Check/>{f}</li>)}</ul>
      <button className={`btn ${ghost ? 'btn-ghost-dark' : 'btn-pri'} btn-sm`} style={{width:'100%',justifyContent:'center'}} onClick={btnAction}>{btnLabel}</button>
    </div>
  );
}

function DlBtn({ title, sub, dark, href, primary }) {
  return (
    <a className={`dl-btn ${dark ? 'dl-btn-dark' : ''} ${primary ? 'dl-btn-primary' : ''}`} href={href || '#'} download={href && !href.startsWith('http') && href !== '#' ? true : undefined} target={href && href.startsWith('http') ? '_blank' : undefined} rel={href && href.startsWith('http') ? 'noopener noreferrer' : undefined}>
      <div><strong>{title}</strong><span>{sub}</span></div>
    </a>
  );
}

function CTA({ title, desc, btn, btnAction, btn2, btn2Action }) {
  return (
    <section className="sec" style={{padding:'60px 24px'}}>
      <div className="container">
        <div className="cta-box">
          <h2>{title}</h2>
          <p>{desc}</p>
          <div style={{display:'flex',gap:16,justifyContent:'center',flexWrap:'wrap'}}>
            <button className="btn btn-pri" onClick={btnAction}>{btn}</button>
            {btn2 && <button className="btn btn-ghost" onClick={btn2Action}>{btn2}</button>}
          </div>
        </div>
      </div>
    </section>
  );
}

function Footer({ go }) {
  return (
    <footer className="footer">
      <div className="ft-grid">
        <div><div style={{marginBottom:12}}><Logo light/></div><p className="ft-desc">Plataforma SaaS de gestão completa do transporte fluvial. De Manaus ao interior do Amazonas.</p></div>
        <div><div className="ft-h">Plataforma</div>{["empresas","passageiros","funcionalidades","precos"].map(p => <a key={p} className="ft-link" onClick={() => go(p)}>{NAV_ITEMS.find(n=>n.id===p)?.label || p}</a>)}</div>
        <div><div className="ft-h">Recursos</div><a className="ft-link" onClick={() => go('download')}>Download Desktop</a><a className="ft-link" onClick={() => go('download')}>Baixar o App</a></div>
        <div><div className="ft-h">Empresa</div><a className="ft-link" onClick={() => go('contato')}>Contato</a><a className="ft-link">Termos de Uso</a><a className="ft-link">Privacidade</a></div>
      </div>
      <div className="ft-bottom"><span>© 2026 Naviera Eco · Manaus, AM</span><span>Navegação fluvial inteligente ⚓</span></div>
    </footer>
  );
}

// ────────────────────────────────────────────
// APP
// ────────────────────────────────────────────

export default function App() {
  const [page, setPage] = useState("home");
  const [scrolled, setScrolled] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const mainRef = useRef(null);

  const go = useCallback((p) => {
    setPage(p);
    setMenuOpen(false);
    if (mainRef.current) mainRef.current.scrollTop = 0;
  }, []);

  const handleScroll = useCallback((e) => {
    setScrolled(e.target.scrollTop > 40);
  }, []);

  const pages = { home: HomePage, empresas: EmpresasPage, passageiros: PassageirosPage, funcionalidades: FuncionalidadesPage, precos: PrecosPage, download: DownloadPage, contato: ContatoPage, cadastro: CadastroPage };
  const PageComponent = pages[page] || HomePage;

  return (
    <div className="site-root" ref={mainRef} onScroll={handleScroll}>
      <link href="https://fonts.googleapis.com/css2?family=Sora:wght@300;400;500;600;700;800&family=Space+Mono:wght@400;700&display=swap" rel="stylesheet"/>
      <style>{styles}</style>

      {/* NAV */}
      <header className={`navbar ${scrolled ? 'scrolled' : ''}`}>
        <div className="nav-inner">
          <a className="nav-brand" onClick={() => go('home')} style={{cursor:'pointer'}}><Logo/></a>
          <nav className="nav-links">
            {NAV_ITEMS.map(n => <a key={n.id} className={`nav-link ${page === n.id ? 'active' : ''}`} onClick={() => go(n.id)}>{n.label}</a>)}
            <button className="btn btn-pri btn-sm nav-cta" onClick={() => go('download')}>Download</button>
          </nav>
          <button className="hamburger" onClick={() => setMenuOpen(!menuOpen)}><span/><span/><span/></button>
        </div>
      </header>

      {menuOpen && <div className="drawer">
        <button className="drawer-close" onClick={() => setMenuOpen(false)}>✕</button>
        <a onClick={() => go('home')}>Início</a>
        {NAV_ITEMS.map(n => <a key={n.id} onClick={() => go(n.id)}>{n.label}</a>)}
        <button className="btn btn-pri" onClick={() => go('download')}>Download</button>
      </div>}

      <PageComponent go={go}/>
      <Footer go={go}/>
    </div>
  );
}

// ────────────────────────────────────────────
// STYLES
// ────────────────────────────────────────────

const styles = `
  :root {
    --pri: #059669; --pri-light: #34D399; --pri-dark: #047857; --pri-900: #064E3B;
    --pri-50: #ECFDF5; --pri-100: #D1FAE5; --pri-200: #A7F3D0;
    --dark: #040D0A; --dark-card: #0F2D24; --dark-border: rgba(52,211,153,.08);
    --light: #F7FBF9; --light-card: #FFFFFF; --light-soft: #EEF7F2;
    --light-border: rgba(5,150,105,.12);
    --text: #0F2620; --text-soft: #3D6B56; --text-muted: #7BA393;
    --text-inv: #F0FDF4;
    --sans: 'Sora', system-ui, sans-serif; --mono: 'Space Mono', monospace;
    --max: 1140px; --radius-lg: 24px; --radius-xl: 32px;
    --icon-bg: var(--pri-50); --icon-stroke: var(--pri);
  }
  .site-root { height: 100vh; overflow-y: auto; overflow-x: hidden; font-family: var(--sans); background: var(--light); color: var(--text); -webkit-font-smoothing: antialiased; line-height: 1.6; scroll-behavior: smooth; }
  .site-root * { margin: 0; padding: 0; box-sizing: border-box; }
  a, button { font-family: var(--sans); }
  ::selection { background: rgba(5,150,105,.15); }

  .container { max-width: var(--max); margin: 0 auto; padding: 0 24px; }
  .badge { display: inline-block; font-size: 11px; font-weight: 700; letter-spacing: .2em; text-transform: uppercase; font-family: var(--mono); padding: 6px 14px; border-radius: 100px; }
  .badge-green { color: var(--pri-light); background: rgba(52,211,153,.1); border: 1px solid rgba(52,211,153,.15); }
  .badge-dark { color: var(--pri); background: var(--pri-50); border: 1px solid var(--pri-100); }

  .btn { display: inline-flex; align-items: center; gap: 8px; border: none; border-radius: 14px; font-weight: 700; cursor: pointer; font-size: 15px; transition: all .25s ease; text-decoration: none; }
  .btn-pri { background: var(--pri); color: #fff; padding: 16px 32px; box-shadow: 0 4px 20px rgba(5,150,105,.3), inset 0 1px 0 rgba(255,255,255,.15); }
  .btn-pri:hover { background: var(--pri-dark); transform: translateY(-2px); box-shadow: 0 8px 32px rgba(5,150,105,.4); }
  .btn-ghost { background: transparent; padding: 16px 32px; border: 2px solid rgba(255,255,255,.15); color: rgba(255,255,255,.7); }
  .btn-ghost:hover { border-color: rgba(255,255,255,.3); background: rgba(255,255,255,.04); }
  .btn-ghost-dark { background: transparent; padding: 16px 32px; border: 2px solid rgba(5,150,105,.3); color: var(--pri); }
  .btn-ghost-dark:hover { border-color: var(--pri); background: var(--pri-50); }
  .btn-sm { padding: 12px 24px; font-size: 13px; border-radius: 12px; }

  .navbar { position: sticky; top: 0; z-index: 100; padding: 18px 24px; display: flex; justify-content: center; transition: all .3s ease; background: transparent; }
  .nav-inner { max-width: var(--max); width: 100%; display: flex; align-items: center; justify-content: space-between; }
  .navbar.scrolled { padding: 10px 24px; background: rgba(247,251,249,.92); backdrop-filter: blur(20px) saturate(1.4); border-bottom: 1px solid var(--light-border); }
  .nav-brand { display: flex; align-items: center; cursor: pointer; }
  .nav-brand svg { height: 34px; width: auto; }
  .nav-links { display: flex; align-items: center; gap: 4px; }
  .nav-link { padding: 8px 14px; border-radius: 10px; font-size: 13px; font-weight: 500; color: var(--text-soft); transition: all .2s; cursor: pointer; background: none; border: none; text-decoration: none; }
  .nav-link:hover { color: var(--pri); background: var(--pri-50); }
  .nav-link.active { color: var(--pri); font-weight: 600; }
  .nav-cta { margin-left: 8px; }
  .hamburger { display: none; background: none; border: none; cursor: pointer; padding: 8px; flex-direction: column; gap: 5px; }
  .hamburger span { display: block; width: 22px; height: 2px; background: var(--text); border-radius: 2px; }

  .drawer { position: fixed; inset: 0; z-index: 200; background: rgba(247,251,249,.98); backdrop-filter: blur(20px); display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 24px; }
  .drawer a { text-decoration: none; color: var(--text); font-size: 20px; font-weight: 600; padding: 8px 24px; border-radius: 12px; cursor: pointer; transition: all .2s; }
  .drawer a:hover { color: var(--pri); background: var(--pri-50); }
  .drawer-close { position: absolute; top: 20px; right: 24px; background: none; border: none; font-size: 24px; cursor: pointer; color: var(--text-soft); }

  .sec { padding: 100px 24px; }
  .sec-dark { background: var(--dark); color: var(--text-inv); --icon-bg: rgba(52,211,153,.1); --icon-stroke: var(--pri-light); }
  .sec-muted { background: var(--light-soft); }
  .sec-header { text-align: center; margin-bottom: 64px; }
  .sec-header .badge { margin-bottom: 16px; }
  .sec-title { font-size: clamp(28px, 4.5vw, 44px); font-weight: 700; line-height: 1.15; margin-bottom: 16px; letter-spacing: -0.5px; }
  .sec-desc { font-size: 17px; line-height: 1.7; max-width: 560px; margin: 0 auto; color: var(--text-soft); }
  .sec-dark .sec-desc { color: rgba(255,255,255,.6); }

  .hero { position: relative; min-height: 100vh; display: flex; align-items: center; background: var(--dark); overflow: hidden; padding: 80px 24px; }
  .hero::before { content: ''; position: absolute; top: -200px; right: -200px; width: 800px; height: 800px; border-radius: 50%; background: radial-gradient(circle, rgba(52,211,153,.08) 0%, transparent 70%); pointer-events: none; }
  .hero-short { min-height: auto; padding: 100px 24px 60px; }
  .hero-inner { max-width: var(--max); margin: 0 auto; width: 100%; position: relative; z-index: 2; }
  .hero h1 { font-size: clamp(36px, 6vw, 72px); font-weight: 800; line-height: 1.05; color: var(--text-inv); margin-bottom: 24px; letter-spacing: -1px; }
  .hero h1 em { font-style: normal; color: var(--pri-light); }
  .hero-desc { font-size: clamp(16px, 2vw, 20px); color: rgba(255,255,255,.55); max-width: 520px; margin-bottom: 40px; line-height: 1.7; }
  .hero-actions { display: flex; gap: 16px; flex-wrap: wrap; margin-bottom: 48px; }

  .stats-bar { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1px; background: var(--dark-border); border-radius: var(--radius-lg); overflow: hidden; border: 1px solid var(--dark-border); }
  .stat { background: var(--dark-card); padding: 32px 24px; text-align: center; }
  .stat-num { font-size: clamp(28px, 4vw, 40px); font-weight: 800; color: var(--pri-light); margin-bottom: 4px; }
  .stat-label { font-size: 12px; color: rgba(255,255,255,.4); letter-spacing: .1em; text-transform: uppercase; font-weight: 500; }

  .grid2 { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }
  .grid3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
  .grid4 { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }

  .card { background: var(--light-card); border: 1px solid var(--light-border); border-radius: var(--radius-lg); padding: 36px 28px; transition: all .3s ease; }
  .card:hover { transform: translateY(-4px); box-shadow: 0 12px 40px rgba(5,150,105,.08); border-color: var(--pri-200); }
  .card h3 { font-size: 18px; font-weight: 700; margin-bottom: 8px; }
  .card p { font-size: 14px; color: var(--text-soft); line-height: 1.7; }
  .card-dark { background: var(--dark-card); border-color: var(--dark-border); }
  .card-dark:hover { border-color: rgba(52,211,153,.2); box-shadow: 0 12px 40px rgba(0,0,0,.3); }
  .card-dark h3 { color: var(--text-inv); }
  .card-dark p { color: rgba(255,255,255,.5); }

  .arch-card { border-radius: var(--radius-lg); padding: 32px 24px; text-align: center; border: 1px solid var(--dark-border); background: var(--dark-card); transition: all .3s; }
  .arch-card:hover { border-color: rgba(52,211,153,.2); }
  .arch-icon { width: 56px; height: 56px; border-radius: 16px; margin: 0 auto 16px; display: flex; align-items: center; justify-content: center; font-size: 24px; }
  .arch-card h4 { font-size: 16px; font-weight: 700; margin-bottom: 4px; color: var(--text-inv); }
  .arch-tech { font-size: 11px; font-family: var(--mono); color: var(--pri-light); margin-bottom: 12px; }
  .arch-card p { font-size: 13px; color: rgba(255,255,255,.45); line-height: 1.6; }

  .dual-card { border-radius: var(--radius-xl); padding: 48px 40px; }
  .dual-card .badge { margin-bottom: 16px; }
  .dual-card h3 { font-size: clamp(22px, 3vw, 30px); font-weight: 700; margin-bottom: 12px; line-height: 1.2; }
  .dual-card p { font-size: 15px; line-height: 1.7; margin-bottom: 28px; opacity: .7; }
  .dual-card ul { list-style: none; margin-bottom: 32px; }
  .dual-card li { padding: 8px 0; font-size: 14px; font-weight: 500; display: flex; align-items: center; gap: 10px; }
  .dual-card li::before { content: ''; width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; }
  .dual-b2b { background: linear-gradient(160deg, var(--dark), var(--dark-card)); color: var(--text-inv); border: 1px solid var(--dark-border); }
  .dual-b2b li::before { background: var(--pri-light); }
  .dual-b2c { background: linear-gradient(160deg, var(--pri-50), var(--light-card)); color: var(--text); border: 1px solid var(--pri-100); }
  .dual-b2c li::before { background: var(--pri); }

  .pricing-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 24px; }
  .pricing-card { background: var(--light-card); border: 1px solid var(--light-border); border-radius: var(--radius-xl); padding: 40px 32px; text-align: center; transition: all .3s; position: relative; }
  .pricing-featured { border-color: var(--pri); box-shadow: 0 0 0 1px var(--pri), 0 16px 48px rgba(5,150,105,.12); transform: scale(1.03); }
  .pricing-popular { position: absolute; top: -12px; left: 50%; transform: translateX(-50%); background: var(--pri); color: #fff; font-size: 10px; font-weight: 700; letter-spacing: .15em; padding: 5px 16px; border-radius: 100px; }
  .pricing-card h3 { font-size: 20px; font-weight: 700; margin-bottom: 8px; }
  .pricing-price { font-size: 42px; font-weight: 800; color: var(--pri); margin: 20px 0; }
  .pricing-price small { font-size: 14px; font-weight: 500; color: var(--text-muted); }
  .pricing-desc { font-size: 13px; color: var(--text-muted); margin-bottom: 28px; }
  .pricing-features { list-style: none; text-align: left; margin-bottom: 32px; }
  .pricing-features li { padding: 10px 0; font-size: 14px; color: var(--text-soft); display: flex; align-items: center; gap: 10px; border-bottom: 1px solid rgba(5,150,105,.06); }
  .pricing-features li:last-child { border: none; }

  .dl-card { border-radius: var(--radius-xl); padding: 48px 40px; }
  .dl-dark { background: var(--dark-card); border: 1px solid var(--dark-border); }
  .dl-light { background: linear-gradient(160deg, var(--pri-50), var(--light-card)); border: 1px solid var(--pri-100); }
  .dl-buttons { display: flex; flex-direction: column; gap: 12px; margin-bottom: 24px; }
  .dl-btn { display: flex; align-items: center; gap: 14px; padding: 16px 24px; border-radius: 14px; border: 1px solid rgba(255,255,255,.1); cursor: pointer; transition: all .25s; }
  .dl-btn:hover { background: rgba(255,255,255,.05); border-color: rgba(255,255,255,.2); }
  .dl-btn strong { display: block; font-size: 15px; color: #fff; }
  .dl-btn span { display: block; font-size: 12px; color: rgba(255,255,255,.5); }
  .dl-btn:not(.dl-btn-dark) { border-color: var(--pri-200); background: white; }
  .dl-btn:not(.dl-btn-dark) strong { color: var(--text); }
  .dl-btn:not(.dl-btn-dark) span { color: var(--text-muted); }
  .dl-btn:not(.dl-btn-dark):hover { border-color: var(--pri); }
  .dl-btn-primary { border-color: var(--pri) !important; box-shadow: 0 0 0 1px var(--pri), 0 4px 16px rgba(5,150,105,.2); transform: scale(1.02); }
  .dl-btn-primary strong::after { content: ' ← recomendado'; font-size: 10px; font-weight: 500; color: var(--pri-light); margin-left: 8px; text-transform: uppercase; letter-spacing: .1em; }
  .dl-reqs { margin-top: 0; padding: 20px; border-radius: 12px; background: rgba(255,255,255,.03); }
  .dl-reqs h4 { font-size: 11px; text-transform: uppercase; letter-spacing: .15em; color: var(--pri-light); margin-bottom: 10px; font-family: var(--mono); }
  .dl-reqs li { font-size: 13px; color: rgba(255,255,255,.4); padding: 3px 0; list-style: none; }
  .dl-reqs li::before { content: '→ '; color: var(--pri-light); }
  .dl-reqs-light { background: rgba(5,150,105,.05); }
  .dl-reqs-light h4 { color: var(--pri); }
  .dl-reqs-light li { color: var(--text-soft); }
  .dl-reqs-light li::before { color: var(--pri); }

  .cta-box { border-radius: var(--radius-xl); padding: 64px 48px; text-align: center; background: linear-gradient(135deg, var(--dark), var(--dark-card)); color: var(--text-inv); position: relative; overflow: hidden; }
  .cta-box::before { content: ''; position: absolute; top: -100px; right: -100px; width: 400px; height: 400px; border-radius: 50%; background: radial-gradient(circle, rgba(52,211,153,.1) 0%, transparent 70%); }
  .cta-box h2 { font-size: clamp(24px, 4vw, 36px); font-weight: 700; margin-bottom: 12px; position: relative; }
  .cta-box p { color: rgba(255,255,255,.5); margin-bottom: 32px; max-width: 440px; margin-left: auto; margin-right: auto; font-size: 16px; position: relative; }
  .cta-box .btn { position: relative; }

  .footer { padding: 64px 24px 32px; background: var(--dark); color: var(--text-inv); }
  .ft-grid { max-width: var(--max); margin: 0 auto; display: grid; grid-template-columns: 2fr 1fr 1fr 1fr; gap: 48px; margin-bottom: 48px; }
  .ft-desc { font-size: 13px; color: rgba(255,255,255,.4); line-height: 1.7; max-width: 260px; }
  .ft-h { font-size: 11px; font-weight: 700; letter-spacing: .15em; text-transform: uppercase; margin-bottom: 16px; color: rgba(255,255,255,.3); }
  .ft-link { display: block; font-size: 14px; color: rgba(255,255,255,.55); padding: 5px 0; cursor: pointer; transition: all .2s; text-decoration: none; background: none; border: none; text-align: left; }
  .ft-link:hover { color: var(--pri-light); }
  .ft-bottom { max-width: var(--max); margin: 0 auto; padding-top: 24px; border-top: 1px solid rgba(255,255,255,.06); display: flex; justify-content: space-between; font-size: 12px; color: rgba(255,255,255,.25); }

  @keyframes fadeUp { from { opacity: 0; transform: translateY(24px); } to { opacity: 1; transform: translateY(0); } }
  .fade-up { animation: fadeUp .6s ease both; }
  .d1 { animation-delay: .1s; } .d2 { animation-delay: .2s; } .d3 { animation-delay: .3s; } .d4 { animation-delay: .4s; }

  @media (max-width: 900px) {
    .nav-links { display: none; }
    .hamburger { display: flex; }
    .grid2, .grid3 { grid-template-columns: 1fr; }
    .grid4 { grid-template-columns: 1fr 1fr; }
    .pricing-grid { grid-template-columns: 1fr; }
    .pricing-featured { transform: none; }
    .ft-grid { grid-template-columns: 1fr 1fr; gap: 32px; }
    .stats-bar { grid-template-columns: 1fr 1fr; }
    .hero { padding: 60px 24px; }
    .hero-short { padding: 60px 24px 40px; }
    .sec { padding: 64px 24px; }
    .hero-actions { flex-direction: column; }
    .hero-actions .btn { width: 100%; justify-content: center; }
    .dual-card { padding: 32px 24px; }
    .dl-card { padding: 32px 24px; }
    .cta-box { padding: 40px 24px; }
  }
  @media (max-width: 600px) {
    .grid4 { grid-template-columns: 1fr; }
    .ft-grid { grid-template-columns: 1fr; }
  }
`;

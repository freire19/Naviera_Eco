# Naviera Eco — Analise de Valor de Mercado e Custo de Desenvolvimento

**Data:** 12 de abril de 2026
**Versao:** 1.0
**Status do projeto:** MVP 77% funcional

---

## Sumario Executivo

O Naviera Eco e uma plataforma SaaS multi-tenant de gestao de transporte fluvial, desenvolvida especificamente para a regiao amazonica. Atende passageiros, freteiros, lojas parceiras e operadores de embarcacoes com 4 plataformas integradas (Desktop, Web, API e App Mobile).

O projeto ocupa um nicho sem concorrente direto: **ERP operacional para embarcacoes fluviais**. Os competidores existentes focam exclusivamente em venda de passagens (marketplace B2C), enquanto o Naviera resolve o lado operacional completo — manifesto, carga, frete, financeiro, GPS, sincronizacao offline e impressao.

**Valuation estimado: R$ 1,5M a R$ 3M** (cenario realista, pre-receita).

---

## 1. Inventario Tecnico — O Que Existe Hoje

### Numeros do projeto

| Metrica | Quantidade |
|---------|-----------|
| Linhas de codigo | ~61.000 |
| Arquivos fonte | ~500 |
| Tabelas no banco de dados | 52 |
| Endpoints de API (Spring Boot + BFF) | ~190 |
| Telas de interface (total) | 96 |
| Migrations SQL | 21 |
| Camadas completas | 4 |

### Camadas da plataforma

| Camada | Tecnologia | Funcao | Status |
|--------|-----------|--------|--------|
| Desktop | JavaFX 23 + Java 17 | Console operacional offline (barco) | Funcional |
| Web | React + Vite + Express BFF | Espelho do Desktop online (escritorio) | Funcional — 31 paginas, CRUD completo |
| API | Spring Boot 3.3 + PostgreSQL | Backend REST multi-tenant | Funcional — 120 endpoints |
| App Mobile | React PWA | App para clientes finais (CPF/CNPJ) | Funcional — 14 telas, 2 perfis |

### Detalhamento por camada

**Desktop JavaFX (37.167 LOC)**
- 51 telas FXML com controllers
- 28 DAOs tenant-aware
- 33 classes de modelo
- Relatorios JasperReports + impressao termica
- Funciona 100% offline
- Build nativo (.deb Linux, .msi/.exe Windows) via jpackage

**API Spring Boot (5.251 LOC)**
- 28 controllers, 30 services
- 120 endpoints REST
- Auth JWT + BCrypt + Spring Security
- Multi-tenancy com TenantUtils (empresa_id no JWT)
- WebSocket STOMP/SockJS para notificacoes real-time
- Firebase Cloud Messaging (push notifications)
- GPS tracking (POST posicao, GET embarcacoes)
- Sync bidirecional Desktop <-> Cloud
- Sistema de versao com auto-update

**Web React + Express BFF (8.137 LOC)**
- 31 paginas funcionais (CRUD completo)
- 70 endpoints no BFF Express
- Multi-tenant por subdominio (slug.naviera.com.br)
- Painel admin: gestao de empresas + metricas da plataforma
- Responsivo (3 breakpoints), light/dark theme
- Rate limiting, query timeout, logging estruturado

**App Mobile PWA (2.363 LOC)**
- 14 telas, 2 perfis (CPF pessoa fisica / CNPJ loja parceira)
- Design system Naviera V4 (light/dark)
- Mapa GPS com barcos em tempo real (SVG rio Amazonas)
- Bilhete digital com TOTP
- Rastreio de encomendas
- Sistema de amigos
- Push notifications Firebase
- PWA com service worker (funciona offline)
- WebSocket para notificacoes real-time

### Features implementadas

- Autenticacao JWT + BCrypt (2 fluxos: operador e cliente)
- Multi-tenancy completo (subdominio, JWT, filtro em todas as queries)
- GPS tracking em tempo real (tripulacao envia, passageiro visualiza)
- WebSocket STOMP/SockJS (notificacoes tenant-aware)
- PWA com service worker e cache-first
- Push notifications Firebase FCM
- Sync offline bidirecional (Desktop <-> Cloud, last-write-wins)
- Bilhete digital com TOTP (QR code)
- Marketplace de lojas parceiras (pedidos, avaliacoes, vinculo com frete)
- Sistema de amigos (convite, busca, sugestoes)
- Relatorios PDF (JasperReports + OpenPDF)
- Impressao termica configuravel
- Painel admin da plataforma
- Sistema de versao com auto-update no Desktop
- Docker + Nginx wildcard SSL para deploy

### Infraestrutura

| Componente | Detalhe |
|-----------|---------|
| Docker Compose | 3 servicos (PostgreSQL 16, API, App) |
| Nginx | Wildcard SSL para *.naviera.com.br |
| Subdominios | {slug}.naviera.com.br (empresas), app., admin., api. |
| Build nativo | jpackage para .deb (Linux) e .msi/.exe (Windows) |
| Deploy scripts | build.sh (Linux) + build.bat (Windows) |

### Banco de dados — 52 tabelas

**Operacionais (14):** passageiros, viagens, tarifas, passagens, encomendas, encomenda_itens, fretes, frete_itens, rotas, embarcacoes, caixas, conferentes, contatos, tipo_passageiro

**Financeiro/RH (7):** financeiro_saidas, eventos_rh, recibos_avulsos, historico_recibo_quitacao_passageiro, agenda_anotacoes, auditoria_financeiro, categorias_despesa

**Multi-tenant (3):** empresas, versao_sistema, funcionarios

**App/Mobile (5):** clientes_app, dispositivos_push, feedbacks, pagamentos_app, usuarios

**GPS e bilhetes (2):** embarcacao_gps, bilhetes_digitais

**Lojas parceiras (4):** lojas_parceiras, pedidos_loja, avaliacoes_loja, amigos_app

**Sync (2):** sync_controle, sync_log

**Auditoria (3):** log_estornos_fretes, log_estornos_encomendas, log_estornos_passagens

**Auxiliares compartilhadas (8):** aux_tipos_documento, aux_sexo, aux_nacionalidades, aux_tipos_passagem, aux_agentes, aux_horarios_saida, aux_acomodacoes, aux_formas_pagamento

**Configuracao (4):** configuracao_empresa, itens_frete_padrao, itens_encomenda_padrao, cad_clientes_encomenda

---

## 2. Custo de Desenvolvimento — Quanto Custaria Construir

### Estimativa de horas por camada

| Camada | Horas estimadas | Justificativa |
|--------|----------------|---------------|
| Desktop JavaFX (37K LOC, 51 telas, 28 DAOs, relatorios, impressao) | 1.800–2.200h | Camada mais complexa, offline-first, logica de negocio completa |
| API Spring Boot (120 endpoints, auth, sync, GPS, WebSocket, FCM) | 800–1.000h | Backend robusto com multi-tenancy, integracao Firebase, STOMP |
| Web React + BFF (31 paginas, 70 endpoints, CRUD, admin) | 600–800h | Frontend operacional completo com Express BFF |
| App Mobile PWA (14 telas, 2 perfis, GPS map, bilhete, amigos) | 400–500h | App cliente com design system, PWA, push |
| Banco de dados (52 tabelas, 21 migrations, modelo multi-tenant) | 200–300h | Modelagem, migrations, indices, constraints |
| Infra (Docker, Nginx wildcard SSL, deploy, build nativo) | 150–200h | DevOps, containerizacao, CI/CD |
| **TOTAL** | **3.950–5.000h** | |

### Custo em reais por cenario de contratacao

| Cenario | Rate por hora | Custo total |
|---------|--------------|-------------|
| Dev freelancer mid (Brasil) | R$ 80–120/h | R$ 316.000–600.000 |
| Equipe PJ (2-3 devs) | R$ 120–180/h | R$ 474.000–900.000 |
| Software house brasileira | R$ 150–250/h | R$ 592.000–1.250.000 |
| Agencia internacional | R$ 300–500/h | R$ 1.185.000–2.500.000 |

### Custo realista de reproducao

> **R$ 500.000 a R$ 900.000**
>
> Equivalente a uma equipe de 2-3 desenvolvedores trabalhando por 12 a 18 meses.

Isso nao inclui:
- Pesquisa de mercado e entendimento do dominio fluvial
- Iteracoes de UX com usuarios reais
- Custo de oportunidade (tempo de mercado perdido)
- Conhecimento de regras de negocio embarcado no codigo

---

## 3. Analise de Mercado — Transporte Fluvial no Amazonas

### O cenario

O estado do Amazonas possui 62 municipios. Aproximadamente **43 deles (70%) sao acessiveis apenas por rio** — nao existem estradas. O transporte fluvial nao e uma opcao, e a **unica forma de locomocao** para mais de 2 milhoes de pessoas.

Atualmente, tudo funciona de forma arcaica:
- Passagens vendidas no cais, sem padronizacao de preco
- Fretes negociados informalmente, sem registro
- Encomendas sem rastreio
- Nenhum controle financeiro sistematizado
- Nenhum manifesto digital de passageiros
- Concorrencia predatoria que prejudica os proprios barqueiros

### Numeros do mercado

| Dado | Valor | Fonte |
|------|-------|-------|
| Populacao do Amazonas | 4,28 milhoes (2024) | IBGE |
| Municipios dependentes de rio | ~43 de 62 (70%) | InfoAmazonia |
| Passageiros intermunicipais formais (2024) | 589.121 (so Manaus) | ARSEPAM/Agencia Amazonas |
| Passageiros regiao amazonica total | 9,8 milhoes/ano | ANTAQ/UFPA |
| Embarcacoes registradas (regiao Norte) | ~2.000+ | ANTAQ |
| Empresas de navegacao (Amazonia = 74% do Brasil) | Maioria do pais | ANTAQ |

### Precos praticados

| Tipo de rota | Rede (preco) | Cabine (preco) |
|-------------|-------------|----------------|
| Curta (area metropolitana Manaus) | R$ 5–25 | — |
| Media (Manaus–Tefe, ~500 km) | R$ 120–200 | R$ 300–500 |
| Longa (Manaus–Tabatinga, ~1.100 km) | R$ 250–350 | R$ 600–900 |

**Frete fluvial e ate 15x mais barato que frete rodoviario** por tonelada-km.

### Tamanho estimado do mercado

| Segmento | Estimativa anual |
|----------|-----------------|
| Passagens (Amazonas) | R$ 100–300M |
| Fretes e encomendas | R$ 50–200M |
| **Total estimado** | **R$ 150–500M/ano** |

Esses numeros cobrem apenas o Amazonas. A regiao amazonica completa (Para, Amapa, Rondonia) multiplica por 3-4x.

### Investimento governamental

O governo federal esta investindo pesado em hidrovias:

| Iniciativa | Valor/Detalhe |
|-----------|---------------|
| PAC Hidrovias | R$ 4,1 bilhoes destinados |
| Investimento em 2025 | R$ 500M+ (recorde historico) |
| Decreto 11.979/2024 | Criou Secretaria Nacional de Hidrovias e Navegacao |
| Primeira concessao hidroviaria | Rio Paraguai (dez/2024), Tocantins e Tapajos em modelagem |
| Programa BR dos Rios | Modernizacao do transporte fluvial como alternativa rodoviaria |

Esse contexto e extremamente favoravel para solucoes tecnologicas no setor.

---

## 4. Analise Competitiva

### Concorrentes identificados

| Empresa | Modelo | O que faz | Faturamento |
|---------|--------|-----------|-------------|
| **Navegam** (Manaus, 2019) | Marketplace B2C | Venda de passagens online (tipo Decolar dos barcos) | R$ 18,7M em 2024; 300K+ tickets; projetando R$ 40M em 2025 |
| **Ibarco** | Marketplace B2C | Venda de passagens (Pix, parcelamento) | Escopo nao divulgado |
| **Zarpar** | SaaS B2B | Gestao para operadores | Early stage |
| **App Uba** (UFPA) | App B2C | Horarios e reservas | Academico/startup |
| **Pelas Aguas** | Agregador | Agregador de agencias | Early stage |

### Diferencial do Naviera Eco

**Nenhum concorrente faz o que o Naviera faz.**

Todos focam em **vender passagem** (B2C marketplace). Ninguem oferece:

- ERP operacional completo para a embarcacao
- Gestao de fretes e encomendas com rastreio
- Controle financeiro (entradas, saidas, balanco por viagem)
- Manifesto digital de passageiros
- Sincronizacao offline (barco sem internet)
- Impressao termica de recibos e bilhetes
- GPS tracking em tempo real
- Multi-tenancy (multiplas empresas na mesma plataforma)
- Desktop para uso a bordo + Web para escritorio + App para cliente

O Naviera e o **ERP do barco** — uma categoria que nao existe no mercado hoje.

### Vantagem competitiva

| Fator | Naviera Eco | Concorrentes |
|-------|------------|-------------|
| Gestao operacional (manifesto, carga, financeiro) | Sim | Nao |
| Funciona offline no barco | Sim | Nao |
| Multi-plataforma (Desktop + Web + App) | Sim | Nao (so app/web) |
| Multi-tenant SaaS | Sim | Parcial |
| GPS tracking | Sim | Nao |
| Venda de passagens B2C | Sim (via app) | Sim (foco unico) |
| Frete e encomendas | Sim | Nao |
| Impressao termica | Sim | Nao |

---

## 5. Valuation — Quanto Vale Hoje

### Metodo 1: Custo de reproducao (piso)

Quanto custaria para alguem construir o que ja existe do zero:

> **R$ 500.000–900.000**

Esse e o valor minimo. Considera apenas codigo, sem contexto de mercado, conhecimento de dominio ou posicionamento.

### Metodo 2: Comparavel de mercado

A Navegam (concorrente mais proximo, so marketplace de passagens) faturou R$ 18,7M em 2024 com 300K tickets. Com multiplo tipico de startups early-stage (3-5x receita), seu valuation estimado esta na faixa de R$ 56-93M.

O Naviera nao fatura ainda, mas:
- Tem escopo muito maior (ERP operacional completo, nao so marketplace)
- Tem 4 plataformas funcionais (Desktop + Web + API + App)
- Ataca o lado B2B que ninguem atende (operador da embarcacao)
- Tem potencial de lock-in alto (quem usa como ERP nao troca facil)
- Tem barreira tecnica de entrada (sync offline, multi-tenant, multi-plataforma)

### Metodo 3: Decomposicao de valor

| Componente | Valor estimado | Justificativa |
|-----------|---------------|---------------|
| Codigo e tecnologia (4 plataformas, 61K LOC) | R$ 500K–900K | Custo de reproducao tecnica |
| Conhecimento de dominio (regras de negocio fluvial embarcadas) | R$ 200K–400K | Meses de pesquisa, entrevistas, iteracao |
| Posicionamento de mercado (first-mover no ERP fluvial) | R$ 300K–1M | Nicho sem concorrente direto |
| Potencial de receita (SaaS B2B + marketplace B2C) | R$ 500K–2M | Modelo SaaS com lock-in alto e TAM grande |
| Barreira tecnica (sync offline, multi-tenant, 4 plataformas) | R$ 200K–500K | Complexidade que leva 12+ meses para replicar |

### Resumo do valuation

| Cenario | Valor estimado | Condicao |
|---------|---------------|----------|
| **Conservador** | **R$ 700K–1,3M** | So codigo + conhecimento, sem tracao |
| **Realista** | **R$ 1,5M–3M** | Codigo + posicionamento + potencial, pre-receita |
| **Otimista** | **R$ 3M–8M** | Com primeiros clientes pagantes e tracao comprovada |
| **Com tracao forte** | **R$ 10M+** | 50+ embarcacoes, milhares de usuarios ativos |

---

## 6. Estrategia de Monetizacao e Escala

### O playbook: capturar demanda primeiro, converter oferta depois

A estrategia e a mesma que funcionou com iFood, Uber, 99 e similares: **primeiro viciar o usuario final, depois a empresa precisa estar dentro.**

### Fases de execucao

**Fase 1 — Capturar o passageiro (demanda)**
- App gratuito para passageiros e lojas
- Compra de passagem pelo app, rastreio de encomenda, GPS do barco
- Marketing: "saiba onde seu barco esta", "compre passagem sem fila"
- Meta: 5.000 usuarios ativos em 6 meses

**Fase 2 — Criar dependencia (lock-in)**
- Passageiros so querem comprar pelo app
- Lojas so querem despachar pelo app
- Barcos que nao estao no app perdem clientes
- Padronizacao de precos: passagens fixas, fretes tabelados

**Fase 3 — Converter o operador (oferta)**
- Oferecer o ERP (Desktop/Web) como SaaS para barqueiros
- Precificacao: R$ 200–500/mes por embarcacao
- Valor: "seus passageiros ja estao aqui, venha gerenciar tudo num so lugar"
- Meta: 50 embarcacoes pagantes no primeiro ano

**Fase 4 — Marketplace com taxa**
- Taxa sobre passagens vendidas pelo app: 5–10%
- Taxa sobre fretes e encomendas intermediados: 3–7%
- Receita recorrente e escalavel

**Fase 5 — Dados e ecossistema**
- Precificacao inteligente baseada em dados (demanda, rota, epoca)
- Credito para operadores (antecipacao de receita)
- Seguros de viagem
- Parcerias com governo (manifesto digital obrigatorio)

### Projecao de receita (TAM — Mercado Total Enderecavel)

| Fonte de receita | Calculo | Receita anual estimada |
|-----------------|---------|----------------------|
| SaaS operadores | ~2.000 embarcacoes x R$ 300/mes | R$ 7,2M |
| Taxa sobre passagens | 589K+ passagens x R$ 10 taxa media | R$ 5,9M |
| Taxa sobre fretes/encomendas | Estimativa | R$ 3–5M |
| **TAM Amazonas** | | **R$ 15–20M/ano** |
| **TAM Regiao Norte** (PA, AP, RO, AM) | x3-4 | **R$ 50–80M/ano** |

---

## 7. Riscos e Mitigacoes

| Risco | Probabilidade | Impacto | Mitigacao |
|-------|-------------|---------|-----------|
| Baixa adocao inicial por passageiros | Media | Alto | Marketing local agressivo, parcerias com portos |
| Resistencia dos barqueiros a tecnologia | Alta | Medio | Desktop simples, treinamento presencial, versao offline |
| Concorrente (Navegam) expandir para ERP | Baixa | Alto | Velocidade de execucao, lock-in pelo Desktop offline |
| Regulacao desfavoravel | Baixa | Medio | Alinhar com ARSEPAM/ANTAQ desde o inicio |
| Conectividade precaria na regiao | Alta | Medio | Sync offline ja implementado, PWA com cache |
| Dificuldade de escalar suporte | Media | Medio | Self-service no app, onboarding automatizado |

---

## 8. Conclusao

O Naviera Eco esta numa posicao privilegiada:

1. **Mercado gigante** — R$ 150-500M/ano so no Amazonas, bilhoes na regiao Norte
2. **Pouca digitalizacao** — penetracao abaixo de 10%, tudo ainda e arcaico
3. **Nenhum concorrente no nicho operacional** — todos vendem passagem, ninguem faz ERP
4. **Timing perfeito** — governo investindo R$ 4,1 bilhoes em hidrovias
5. **Tecnologia 77% pronta** — 4 plataformas funcionais, 61K linhas de codigo
6. **Barreira tecnica alta** — sync offline + multi-tenant + 4 plataformas levam 12-18 meses para replicar

O que falta nao e tecnologia — e **go-to-market**: colocar o app na mao dos primeiros 1.000 passageiros e fechar os primeiros 5 barcos como pilotos. Com tracao comprovada, o valuation salta de R$ 1,5M para R$ 5M+ rapidamente.

---

*Documento gerado em 12 de abril de 2026*
*Dados de mercado: IBGE, ANTAQ, ARSEPAM, InfoAmazonia, Agencia Gov*

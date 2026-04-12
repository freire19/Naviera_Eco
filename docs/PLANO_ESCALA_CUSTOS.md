# Naviera Eco — Plano de Escala Agressiva e Custos

**Data:** 12 de abril de 2026
**Objetivo:** Dominar o mercado de transporte fluvial digital no Amazonas antes que concorrentes reajam
**Estrategia:** Blitzscaling regional — escala rapida com investimento concentrado

---

## Resumo Executivo

Para colocar o Naviera Eco em funcionamento, escalar com forca e travar o mercado antes da concorrencia, o investimento total estimado e:

| Horizonte | Investimento total | Resultado esperado |
|-----------|-------------------|-------------------|
| **Primeiros 6 meses** (lancamento + tracao) | **R$ 120K–180K** | App no ar, 5.000+ usuarios, 10-20 barcos piloto |
| **Meses 7-12** (escala agressiva) | **R$ 200K–350K** | 20.000+ usuarios, 50-100 barcos, primeiras receitas |
| **Ano 2** (dominio + expansao) | **R$ 400K–700K** | 50.000+ usuarios, 200+ barcos, receita recorrente |
| **TOTAL 24 meses** | **R$ 720K–1,23M** | Mercado travado, monopolio operacional |

---

## Fase 1: Pre-Lancamento e Infraestrutura (Meses 1-2)

### O que precisa estar pronto antes de gastar com ads

Antes de jogar dinheiro em marketing, o produto precisa estar redondo. Gastar em ads com app bugado e queimar dinheiro.

### Custos de infraestrutura

| Item | Custo mensal | Custo 2 meses | Detalhe |
|------|-------------|--------------|---------|
| VPS principal (API + Web + BFF) | R$ 200–400 | R$ 400–800 | 4 vCPU, 8GB RAM, 100GB SSD |
| VPS banco PostgreSQL | R$ 150–300 | R$ 300–600 | Dedicado ou managed (DigitalOcean/Hetzner) |
| Dominio naviera.com.br | R$ 40/ano | R$ 40 | Registro.br |
| SSL wildcard (Let's Encrypt) | Gratuito | R$ 0 | Certbot automatico |
| Firebase (FCM push) | Gratuito ate escala | R$ 0 | Plano Spark ate 10K devices |
| Conta Google Play | R$ 130 (unica vez) | R$ 130 | Para publicar o app Android |
| Conta Apple Developer | R$ 500/ano | R$ 500 | Para publicar no iOS |
| Email profissional (Google Workspace) | R$ 30/mes | R$ 60 | contato@naviera.com.br |
| Ferramentas (Hotjar, Analytics, Sentry) | R$ 0–200/mes | R$ 0–400 | Planos free inicialmente |
| **Subtotal infra** | | **R$ 1.430–2.530** | |

### Custos de finalizacao do produto

| Item | Custo estimado | Detalhe |
|------|---------------|---------|
| Completar os 23% restantes do MVP | R$ 0 (voce dev) | Fechar fluxos criticos do app |
| Testes com 5-10 usuarios reais | R$ 0–500 | Passagens de ida/volta para testar em rota real |
| Publicacao na Play Store + PWA | R$ 0 (voce dev) | Build, screenshots, descricao |
| Landing page naviera.com.br | R$ 0 (voce dev) | Pagina institucional simples |
| **Subtotal produto** | **R$ 0–500** | |

### Custo total Fase 1: R$ 1.430–3.030

---

## Fase 2: Lancamento + Primeiros Usuarios (Meses 3-4)

### Estrategia: Marketing de guerrilha + ads focados

No Amazonas, marketing digital funciona, mas **presenca fisica no porto e rei**. O passageiro decide comprar passagem no cais, nao no sofa.

### Marketing digital — Ads

| Canal | Orcamento mensal | CPI estimado* | Installs/mes | Estrategia |
|-------|-----------------|--------------|-------------|-----------|
| Facebook/Instagram Ads | R$ 3.000–5.000 | R$ 2–5 | 600–2.500 | Videos curtos: "saiba onde seu barco esta", "compre passagem sem fila" |
| Google Ads (Search) | R$ 1.500–3.000 | R$ 3–8 | 200–1.000 | Keywords: "passagem barco Manaus", "barco Tefe", "frete fluvial" |
| TikTok Ads | R$ 1.000–2.000 | R$ 1,50–4 | 250–1.300 | Conteudo viral: vida no rio, bastidores de viagem |
| **Total ads/mes** | **R$ 5.500–10.000** | | **1.050–4.800** | |

*CPI no Norte e 30-50% mais barato que SP/RJ — menos concorrencia por atencao.

### Marketing de guerrilha (presencial)

| Acao | Custo | Impacto |
|------|-------|---------|
| Banner + faixa nos 3 portos de Manaus | R$ 800–1.500 | Visibilidade direta no ponto de decisao |
| Panfletos/adesivos nos portos e barcos | R$ 300–600 | QR code direto pro app |
| Promotor nos portos (2 pessoas, fins de semana) | R$ 2.000–3.000/mes | Ajuda a instalar, cadastra na hora |
| Camisetas/bones Naviera para tripulacao parceira | R$ 500–1.000 | Branding ambulante |
| Parceria com radios locais (cidades do interior) | R$ 500–1.500/mes | Radio e o meio #1 no interior do AM |
| WiFi gratis no porto com splash screen Naviera | R$ 300–500/mes | Captive portal: instala app pra liberar WiFi |
| **Total guerrilha/mes** | **R$ 4.400–8.100** | |

### Custos operacionais mensais

| Item | Custo/mes | Detalhe |
|------|----------|---------|
| Infraestrutura (servidores, dominio, email) | R$ 500–800 | Escala com usuarios |
| Suporte ao usuario (1 pessoa meio periodo) | R$ 1.500–2.500 | WhatsApp + telefone |
| Onboarding de barcos (visita + treinamento) | R$ 1.000–2.000 | Transporte + diaria para ir ao barco |
| Conteudo para redes sociais (designer freelancer) | R$ 800–1.500 | Posts, stories, reels |
| **Total operacional/mes** | **R$ 3.800–6.800** | |

### Custo total Fase 2 (2 meses): R$ 27.400–49.800

### Metas da Fase 2

- 2.000–5.000 downloads do app
- 500–1.500 usuarios cadastrados
- 10-20 embarcacoes piloto usando o Desktop/Web (gratuito nessa fase)
- Primeiras passagens vendidas pelo app

---

## Fase 3: Escala Agressiva — Travar o Mercado (Meses 5-8)

### Estrategia: Aumentar ads 3x, expandir para rotas do interior, fechar exclusividade com barcos

Aqui e onde voce pisa no acelerador. O objetivo e criar dependencia antes que qualquer concorrente consiga reagir.

### Marketing digital — Ads escalados

| Canal | Orcamento mensal | Installs/mes estimados |
|-------|-----------------|----------------------|
| Facebook/Instagram Ads | R$ 8.000–15.000 | 1.600–7.500 |
| Google Ads (Search + Display) | R$ 4.000–8.000 | 500–2.700 |
| TikTok Ads | R$ 3.000–5.000 | 750–3.300 |
| YouTube Ads (pre-roll curto) | R$ 2.000–4.000 | 400–1.500 |
| **Total ads/mes** | **R$ 17.000–32.000** | **3.250–15.000** |

### Marketing guerrilha escalado

| Acao | Custo/mes | Detalhe |
|------|----------|---------|
| Promotores em 5+ portos (Manaus, Parintins, Tefe, Itacoatiara, Tabatinga) | R$ 5.000–8.000 | 1 pessoa por porto nos dias de saida |
| Parceria com 10+ radios do interior | R$ 2.000–5.000 | Spot de 30s, 3x ao dia |
| Patrocinio de eventos locais (Festival de Parintins, festas de municipio) | R$ 2.000–5.000 | Branding Naviera em eventos de massa |
| Programa de indicacao (passageiro indica, ganha desconto) | R$ 1.000–3.000 | R$ 5-10 de credito por indicacao |
| Adesivos em todos os barcos parceiros | R$ 500–1.000 | "Este barco esta no Naviera" |
| **Total guerrilha/mes** | **R$ 10.500–22.000** | |

### Equipe (contratacoes necessarias)

| Funcao | Formato | Custo/mes |
|--------|---------|----------|
| Suporte ao usuario (1 pessoa tempo integral) | CLT ou PJ | R$ 2.500–4.000 |
| Comercial/vendas para barcos (1 pessoa) | PJ + comissao | R$ 3.000–5.000 |
| Social media + conteudo (freelancer ou PJ) | PJ | R$ 1.500–3.000 |
| **Total equipe/mes** | | **R$ 7.000–12.000** |

### Custos operacionais escalados

| Item | Custo/mes |
|------|----------|
| Infraestrutura (servidores escalados) | R$ 800–1.500 |
| Ferramentas (analytics, monitoring, CRM) | R$ 300–800 |
| Viagens para onboarding de barcos no interior | R$ 2.000–4.000 |
| Juridico/contabil (MEI ou LTDA + contratos) | R$ 500–1.500 |
| **Total operacional/mes** | **R$ 3.600–7.800** |

### Custo total Fase 3 (4 meses): R$ 152.400–295.200

### Metas da Fase 3

- 10.000–25.000 downloads acumulados
- 3.000–8.000 usuarios ativos mensais
- 50-100 embarcacoes usando a plataforma
- Cobertura das 10 rotas mais movimentadas do AM
- Primeiras receitas: taxa sobre passagens + assinatura SaaS piloto

---

## Fase 4: Dominacao e Monetizacao (Meses 9-12)

### Estrategia: Ligar a monetizacao, expandir para Para, consolidar monopolio operacional

### Marketing — manutencao + expansao

| Canal | Orcamento mensal | Objetivo |
|-------|-----------------|---------|
| Facebook/Instagram Ads | R$ 10.000–20.000 | Retencao + novos mercados (Belem, Santarem) |
| Google Ads | R$ 5.000–10.000 | Defender keywords, capturar intent |
| TikTok + YouTube | R$ 3.000–7.000 | Brand awareness regional |
| Marketing guerrilha (portos, radio, eventos) | R$ 8.000–15.000 | Expansao para Para e Amapa |
| Programa de indicacao | R$ 2.000–5.000 | Crescimento organico |
| **Total marketing/mes** | **R$ 28.000–57.000** | |

### Equipe expandida

| Funcao | Custo/mes |
|--------|----------|
| Suporte (2 pessoas) | R$ 5.000–8.000 |
| Comercial (2 pessoas — AM + PA) | R$ 6.000–10.000 |
| Social media/marketing | R$ 2.000–4.000 |
| Dev manutencao (voce + 1 junior PJ) | R$ 3.000–5.000 |
| **Total equipe/mes** | **R$ 16.000–27.000** |

### Operacional

| Item | Custo/mes |
|------|----------|
| Infraestrutura escalada (multi-region) | R$ 1.500–3.000 |
| Ferramentas e servicos | R$ 500–1.500 |
| Viagens comerciais | R$ 3.000–6.000 |
| Juridico/contabil | R$ 1.000–2.000 |
| Escritorio/coworking (opcional) | R$ 800–2.000 |
| **Total operacional/mes** | **R$ 6.800–14.500** |

### Custo total Fase 4 (4 meses): R$ 203.200–394.000

### Metas da Fase 4

- 30.000–60.000 downloads acumulados
- 10.000–25.000 usuarios ativos mensais
- 100-200+ embarcacoes na plataforma
- Receita mensal: R$ 20.000–80.000 (SaaS + taxas)
- Presenca no Amazonas + Para + Amapa
- Break-even operacional no horizonte

---

## Consolidado — Investimento Total por Fase

| Fase | Periodo | Investimento | Acumulado |
|------|---------|-------------|-----------|
| 1. Pre-lancamento | Meses 1-2 | R$ 1.430–3.030 | R$ 1.430–3.030 |
| 2. Lancamento | Meses 3-4 | R$ 27.400–49.800 | R$ 28.830–52.830 |
| 3. Escala agressiva | Meses 5-8 | R$ 152.400–295.200 | R$ 181.230–348.030 |
| 4. Dominacao | Meses 9-12 | R$ 203.200–394.000 | R$ 384.430–742.030 |
| **TOTAL ANO 1** | **12 meses** | **R$ 384K–742K** | |

### Ano 2 — Expansao regional

| Item | Custo anual estimado |
|------|---------------------|
| Marketing (ads + guerrilha) | R$ 200K–400K |
| Equipe (5-8 pessoas) | R$ 150K–250K |
| Infra + operacional | R$ 50K–80K |
| **Total Ano 2** | **R$ 400K–730K** |

### Investimento total em 24 meses

> **R$ 780K–1,47M**
>
> Cenario medio realista: **~R$ 1M a R$ 1,2M** para dominar o mercado em 2 anos.

---

## Distribuicao do Investimento (Onde Vai o Dinheiro)

### Ano 1 — Cenario medio (R$ 550K)

| Categoria | % do total | Valor |
|-----------|-----------|-------|
| Marketing digital (ads) | 40% | R$ 220K |
| Marketing presencial (guerrilha) | 18% | R$ 99K |
| Equipe (suporte, comercial, conteudo) | 25% | R$ 137K |
| Infraestrutura (servidores, ferramentas) | 7% | R$ 38K |
| Operacional (viagens, juridico, escritorio) | 10% | R$ 55K |

### Funil de conversao esperado

```
Impressoes de ads:     ~5.000.000 (ao longo de 12 meses)
     |
     v  CTR 2%
Cliques:               ~100.000
     |
     v  Conversao 30%
Installs:              ~30.000
     |
     v  Cadastro 40%
Usuarios cadastrados:  ~12.000
     |
     v  Primeira compra 20%
Usuarios pagantes:     ~2.400
     |
     v  Retencao 50%
Usuarios ativos:       ~1.200 comprando recorrente
```

### CAC (Custo de Aquisicao de Cliente)

| Tipo | CAC estimado | LTV estimado | LTV/CAC |
|------|-------------|-------------|---------|
| Passageiro (B2C) | R$ 15–40 | R$ 100–300/ano (taxas) | 2,5–7,5x |
| Embarcacao (B2B) | R$ 500–2.000 | R$ 3.600–6.000/ano (SaaS) | 1,8–12x |

LTV/CAC acima de 3x e considerado saudavel. O modelo se paga.

---

## Cenarios de Investimento

### Cenario Enxuto (Bootstrap agressivo) — R$ 384K em 12 meses

- Voce faz tudo que e tech (dev, deploy, analytics)
- 1-2 contratacoes PJ (suporte + comercial)
- Ads focados so em Facebook/Instagram (canal mais barato)
- Guerrilha concentrada em Manaus (3 portos)
- Meta: 10.000 usuarios, 50 barcos

### Cenario Medio (Recomendado) — R$ 550K em 12 meses

- Voce faz tech + estrategia
- 3-4 contratacoes PJ
- Ads em Facebook + Google + TikTok
- Guerrilha em Manaus + 5 cidades do interior
- Meta: 25.000 usuarios, 100 barcos

### Cenario Agressivo (Blitzscaling) — R$ 742K em 12 meses

- Voce faz tech, contrata gerente de operacoes
- 5-6 contratacoes
- Ads em todos os canais + influenciadores locais
- Guerrilha em todo o Amazonas + inicio no Para
- Meta: 50.000 usuarios, 200 barcos, receita cobrindo 50%+ do custo

---

## Projecao de Receita vs Investimento

### Com 100 embarcacoes + 10.000 usuarios ativos (mes 12)

| Fonte de receita | Calculo | Receita mensal |
|-----------------|---------|---------------|
| SaaS embarcacoes (100 x R$ 300/mes) | 100 barcos | R$ 30.000 |
| Taxa passagens (2.000 x R$ 8 taxa) | 2.000 passagens/mes | R$ 16.000 |
| Taxa fretes/encomendas | Estimativa | R$ 5.000–10.000 |
| **Total receita mensal** | | **R$ 51.000–56.000** |
| **Receita anualizada** | | **R$ 612.000–672.000** |

### Break-even

Com custo operacional mensal de R$ 40-60K na fase de escala:

> **Break-even estimado: mes 14-18** (entre o final do ano 1 e metade do ano 2)

Apos o break-even, cada barco novo e cada usuario novo e lucro marginal.

---

## Cronograma Visual

```
MES  1  2  3  4  5  6  7  8  9  10  11  12
     |-----|-----|-----|-----------|-----------|
     PREP   LANC   ESCALA AGRESSIVA  DOMINACAO
     
     [Produto]  [Ads leves]  [Ads pesados]  [Monetizacao]
     [Infra  ]  [Guerrilha]  [Equipe cresc] [Expansao PA]
     [Testes ]  [10 barcos]  [50-100 barcos] [200 barcos]
     
GASTO/MES:
     R$1-2K   R$14-25K    R$38-74K       R$51-98K
     
USUARIOS:
     50       2.000-5.000  10.000-25.000  30.000-60.000
```

---

## Tacticas Anti-Concorrencia

O objetivo nao e so crescer — e **impedir que outros cresçam**. Tacticas especificas:

### 1. Lock-in por exclusividade
- Contrato de 12 meses com barcos parceiros (desconto de 50% no SaaS no primeiro ano em troca de exclusividade)
- Custo: R$ 0 (troca desconto por exclusividade)

### 2. Lock-in por dependencia operacional
- Quanto mais dados o barqueiro coloca no sistema (passageiros, fretes, financeiro), mais caro e sair
- O Desktop offline e insubstituivel — nenhum concorrente tem isso

### 3. Dominar as keywords
- Comprar TODAS as keywords de transporte fluvial no Google (volume baixo, CPC barato)
- "passagem barco Manaus", "frete fluvial", "barco Tefe", etc.
- Custo: R$ 1.000–3.000/mes — barato para trancar o canal

### 4. Dominar os portos fisicamente
- Ser a unica marca visivel nos portos de Manaus
- Banners, promotores, WiFi gratis, adesivos nos barcos
- Custo: R$ 3.000–5.000/mes

### 5. Rede de efeito (network effect)
- Quanto mais passageiros no app → mais barcos precisam estar → mais passageiros veem valor
- Apos massa critica (~5.000 usuarios, ~50 barcos), o crescimento se auto-alimenta

### 6. Dados como barreira
- Historico de precos, rotas, sazonalidade, demanda
- Apos 12 meses operando, voce tem dados que ninguem mais tem
- Isso vira produto: precificacao dinamica, relatorios para governo, credito para barqueiros

---

## O Que NAO Fazer (Erros Comuns)

1. **Nao gastar em ads antes do app estar redondo** — cada usuario que instala e encontra bug e um usuario perdido para sempre
2. **Nao tentar escalar nacionalmente cedo demais** — domine o Amazonas primeiro, depois expanda
3. **Nao cobrar dos barcos no inicio** — primeiro 6 meses gratuito, criar dependencia, depois monetizar
4. **Nao ignorar o marketing presencial** — no interior do AM, radio e porto valem mais que Instagram
5. **Nao subestimar suporte** — usuario de primeira viagem digital precisa de mao na roda (WhatsApp)
6. **Nao competir com Navegam em passagem** — seu diferencial e o ERP, nao o marketplace

---

## Conclusao

| Pergunta | Resposta |
|----------|---------|
| Quanto custa escalar com forca? | **R$ 550K–750K no primeiro ano** |
| Quanto custa dominar em 2 anos? | **R$ 1M–1,2M total** |
| Onde vai a maior parte do dinheiro? | **58% em marketing (digital + guerrilha)** |
| Quando comeca a dar retorno? | **Mes 14-18 (break-even operacional)** |
| O que trava concorrente? | **Exclusividade com barcos + lock-in do ERP + dominio dos portos** |
| Qual o risco principal? | **Gastar em ads antes do produto estar pronto** |

O investimento parece alto, mas no contexto do mercado (R$ 150-500M/ano so no AM), gastar R$ 1M para travar o mercado e **barato**. A Navegam faturou R$ 18,7M em 2024 — com o posicionamento certo, o retorno vem.

---

*Documento gerado em 12 de abril de 2026*
*Dados de custos: Meta Business, Google Ads Benchmarks, RD Station, GSMA LatAm 2024*
*Ajustes regionais aplicados para Norte/Amazonas (30-50% mais barato que eixo SP-RJ)*

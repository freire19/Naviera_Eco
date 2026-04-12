# Naviera Eco — Justificativa Detalhada de Cada Custo

**Data:** 12 de abril de 2026
**Contexto:** Detalhamento centavo por centavo de onde vai cada real, por que e necessario, e o que acontece se nao gastar.

---

## Cenario medio usado como base: R$ 550K no Ano 1

Esse e o valor que vou dissecar abaixo. Cada real tem destino, motivo e consequencia.

---

## 1. INFRAESTRUTURA — R$ 14.400/ano (2,6% do total)

Isso e o custo de manter o sistema no ar. Sem isso, nao existe produto.

### Servidores — R$ 6.000–9.600/ano

| Item | Custo/mes | Por que precisa | O que acontece sem |
|------|----------|----------------|-------------------|
| VPS para API Spring Boot + BFF Express | R$ 250–400 | E o cerebro. Toda requisicao do app, do web e do desktop passa aqui. Precisa de CPU e RAM suficiente para aguentar centenas de usuarios simultaneos. | Sem servidor = sem app, sem web, sem nada. Tudo para. |
| VPS para PostgreSQL | R$ 200–400 | Banco de dados com 52 tabelas, dados de todas as empresas, passageiros, fretes, financeiro. Precisa de disco rapido (SSD) e backup automatico. | Sem banco = perda total de dados. Um disco que falha sem backup mata o negocio. |

**Por que nao usar so 1 servidor?** Pode comecar com 1, mas separar banco do app e basico de seguranca e performance. Se o app trava, o banco continua. Se precisa escalar, escala so o que precisa.

**Por que Hetzner/DigitalOcean e nao AWS?** Preco. Uma VPS de 4 vCPU/8GB na Hetzner custa R$ 200/mes. Na AWS, o mesmo custa R$ 600+. No inicio, cada real conta.

### Dominio e email — R$ 400/ano

| Item | Custo/ano | Por que precisa |
|------|----------|----------------|
| naviera.com.br (Registro.br) | R$ 40 | Sem dominio nao tem site, nao tem subdominio para cada empresa, nao tem credibilidade. O wildcard *.naviera.com.br e o que permite saofrancisco.naviera.com.br, barco.naviera.com.br, etc. |
| Google Workspace (1 usuario) | R$ 360 | Email contato@naviera.com.br. Ninguem confia em empresa que manda email de @gmail.com. Tambem da Google Drive para docs internos. |

### Contas de loja — R$ 630 (unico)

| Item | Custo | Por que precisa |
|------|-------|----------------|
| Google Play Developer | R$ 130 (unica vez) | Para publicar o app Android. 80%+ dos usuarios no Amazonas usam Android. Sem isso, nao tem app na loja. |
| Apple Developer | R$ 500/ano | Para publicar no iOS. Opcional no inicio (minoria do publico), mas necessario para credibilidade. Pode adiar 6 meses se quiser economizar. |

### Ferramentas — R$ 2.400/ano

| Item | Custo/mes | Por que precisa | Alternativa gratis? |
|------|----------|----------------|-------------------|
| Sentry (monitoramento de erros) | R$ 0 (plano free) | Quando o app crasha no celular do usuario, voce precisa saber. Sem isso, so descobre quando alguem reclama no WhatsApp. | Sim, plano free cobre ate 5K erros/mes. |
| Google Analytics / Mixpanel | R$ 0 (plano free) | Saber quantos usuarios abriram o app, quais telas usam, onde desistem. Sem dados, voce gasta em ads no escuro. | Sim, GA e gratuito. |
| Hotjar (mapas de calor) | R$ 0–100 | Ver onde o usuario clica, onde trava, onde desiste. Util nos primeiros meses para ajustar UX. | Plano free com limite de sessoes. |

**Resumo infra: R$ 14.400/ano e o MINIMO para existir.** E o preco de 2 meses de aluguel em Manaus. Se isso parece caro, o negocio nao e viavel — mas e viavel porque esse custo e irrelevante perto da receita que gera.

---

## 2. MARKETING DIGITAL (ADS) — R$ 220K/ano (40% do total)

Esse e o maior gasto e o mais importante de justificar. Cada real de ads precisa voltar em usuario.

### Como funciona o dinheiro de ads

Voce nao paga "para aparecer". Voce paga por resultado mensuravel:

```
R$ 1 gasto em ad
  → Facebook mostra pra ~40-100 pessoas (CPM R$ 10-25)
  → ~2 pessoas clicam (CTR 2%)
  → ~1 pessoa instala o app (conversao 30-50%)
  → Custo por install: R$ 2-5 no Norte do Brasil
```

No Amazonas, ads sao **30-50% mais baratos** que em SP/RJ porque tem menos anunciantes competindo pela atencao. Isso e uma vantagem enorme.

### Facebook/Instagram Ads — R$ 120K/ano (R$ 10K/mes medio)

| Detalhe | Valor |
|---------|-------|
| Orcamento mensal medio | R$ 10.000 |
| CPI (custo por install) no Norte | R$ 2–5 |
| Installs por mes | 2.000–5.000 |
| Installs no ano | 24.000–60.000 |

**Por que Facebook/Instagram?**
- 83% dos brasileiros usam Instagram ou Facebook (DataReportal 2025)
- No interior do AM, Facebook e a internet para muita gente
- Permite segmentar por cidade (Manaus, Parintins, Tefe, Tabatinga)
- Formato video curto (Reels) e o que mais converte para app

**Que tipo de anuncio?**
- Video de 15s: "Voce sabe onde seu barco esta agora? Com o Naviera, voce rastreia em tempo real"
- Carrossel: "Compre passagem sem fila, rastreie sua encomenda, veja o GPS do barco"
- Stories: depoimento de passageiro real usando o app
- Retargeting: quem visitou o site mas nao instalou — lembrar de baixar

**Por que R$ 10K/mes e nao R$ 2K?**
Com R$ 2K voce consegue 400-1.000 installs/mes. Parece ok, mas:
- 1.000 installs → 300 cadastros → 60 compram passagem
- 60 usuarios ativos nao convence nenhum barqueiro a entrar na plataforma
- O concorrente (Navegam) ja tem 300K+ tickets vendidos
- Voce precisa de massa critica rapido: **5.000+ installs/mes** para criar efeito de rede

**O que acontece se nao gastar?**
Crescimento organico em nicho regional e lento demais. Levaria 2-3 anos para chegar onde ads levam em 6 meses. Nesse tempo, Navegam ou outro player fecha o mercado.

### Google Ads — R$ 60K/ano (R$ 5K/mes medio)

| Detalhe | Valor |
|---------|-------|
| Orcamento mensal medio | R$ 5.000 |
| CPC medio (keywords fluviais no Norte) | R$ 0,80–2,50 |
| Cliques por mes | 2.000–6.250 |
| Installs por mes (conversao 25%) | 500–1.500 |

**Por que Google Ads?**
Captura **intencao**. Quem pesquisa "passagem barco Manaus Tefe" QUER comprar passagem agora. E o usuario mais valioso — ja esta decidido, so precisa do caminho.

**Keywords que voce compra:**

| Keyword | CPC estimado | Volume mensal | Por que |
|---------|-------------|--------------|---------|
| "passagem barco manaus" | R$ 1,50–2,50 | 1.000–3.000 | Intent de compra direto |
| "barco manaus tefe" | R$ 0,80–1,50 | 500–1.000 | Rota especifica — usuario decidido |
| "barco manaus tabatinga" | R$ 0,80–1,50 | 300–800 | Rota longa, ticket alto |
| "frete fluvial manaus" | R$ 1,00–2,00 | 200–500 | B2B — lojas querendo despachar |
| "rastrear encomenda barco" | R$ 0,50–1,00 | 100–300 | Feature unica do Naviera |
| "naviera" (marca propria) | R$ 0,20–0,50 | Cresce com awareness | Defesa de marca |

**Por que gastar em Google se ja gasta em Facebook?**
Sao publicos diferentes:
- Facebook = voce mostra o app para quem NAO estava procurando (awareness)
- Google = voce aparece para quem JA esta procurando (intent)
- Os dois juntos formam o funil completo: Facebook cria desejo → Google captura quando o desejo vira acao

**O que acontece se nao gastar?**
Quem pesquisar "passagem barco" encontra Navegam ou Ibarco. Voce perde o usuario mais valioso — o que ja quer comprar.

### TikTok Ads — R$ 30K/ano (R$ 2.500/mes medio)

| Detalhe | Valor |
|---------|-------|
| Orcamento mensal medio | R$ 2.500 |
| CPI no Norte | R$ 1,50–4 |
| Installs por mes | 625–1.650 |

**Por que TikTok?**
- CPI mais barato de todos os canais
- Publico jovem (18-35) — exatamente quem viaja de barco e usa celular
- Conteudo viral: videos de viagem no rio, bastidores, humor amazonense
- Uma vez que um video viraliza organicamente, o custo por install despenca

**Que tipo de conteudo?**
- "POV: voce comprando passagem de barco pelo celular pela primeira vez"
- "Como e viajar 3 dias de Manaus a Tabatinga" (informativo + branding)
- "Meu frete chegou e eu rastreei pelo app" (prova social)
- Parcerias com criadores locais (micro-influenciadores do AM)

### YouTube Ads — R$ 10K/ano (R$ 800/mes medio, so a partir do mes 5)

**Por que?** Pre-roll de 6 segundos antes de videos e barato e fixa a marca. "Naviera — seu barco na palma da mao". Funciona como reforco dos outros canais.

### Resumo: por que 40% do orcamento vai em ads

| Motivo | Explicacao |
|--------|-----------|
| Velocidade | Ads geram 30.000+ installs em 12 meses. Organico geraria 3.000. |
| Mensurabilidade | Cada real e rastreavel: gasto → clique → install → cadastro → compra. Se nao esta convertendo, ajusta na hora. |
| Defesa competitiva | Se voce nao compra as keywords "passagem barco", o Navegam compra. |
| Efeito de rede | Precisa de massa critica rapida (5.000+ usuarios) para convencer barcos a entrar. Ads e o unico caminho rapido. |
| Custo regional favoravel | No Norte, CPI e R$ 2-5. No Sudeste seria R$ 8-15. O dinheiro rende 3x mais aqui. |

---

## 3. MARKETING PRESENCIAL (GUERRILHA) — R$ 99K/ano (18% do total)

No Amazonas, o digital sozinho nao basta. O passageiro decide no cais, nao no sofa.

### Promotores nos portos — R$ 48K/ano

| Detalhe | Valor |
|---------|-------|
| Custo por promotor (meio periodo, fins de semana e dias de saida) | R$ 1.500–2.000/mes |
| Quantidade | 2 em Manaus (meses 3-4), depois 4-5 (meses 5-12) |
| Custo anual | R$ 36.000–48.000 |

**O que o promotor faz?**
- Fica no porto com colete Naviera e celular demonstrativo
- Aborda passageiro: "voce ja sabe o horario do seu barco? Baixa aqui que mostra tudo"
- Ajuda a instalar o app no celular da pessoa (muitos nao sabem baixar app)
- Cadastra o passageiro na hora
- Distribui panfleto com QR code

**Por que precisa de gente fisica?**
- 40% do publico do interior tem dificuldade com tecnologia
- Confianca: pessoa explicando vale mais que anuncio
- O porto e o UNICO lugar onde 100% do seu publico-alvo esta presente
- Taxa de conversao de promotor no porto: 30-50% (vs 2% de ad online)

**Por que nao so panfleto?**
Panfleto sem pessoa explicando vai pro lixo. Promotor + panfleto = install na hora.

### Material grafico — R$ 8.400/ano

| Item | Custo | Quantidade | Por que |
|------|-------|-----------|---------|
| Banners para portos (lona 2x1m) | R$ 150–300 cada | 6-10 banners | Visibilidade no ponto de decisao. Passageiro ve enquanto espera embarcar. |
| Panfletos com QR code | R$ 0,15 cada | 20.000/ano | QR code direto pra Play Store. Barato e funcional. |
| Adesivos para barcos | R$ 5–15 cada | 200/ano | "Este barco esta no Naviera" — prova social permanente que viaja junto com o barco. |
| Camisetas/bones Naviera | R$ 25–40 cada | 100/ano | Para tripulacao parceira usar. Branding ambulante gratis depois do investimento inicial. |

**Por que material fisico em 2026?**
Porque o publico e ribeirinho. Internet e intermitente. O banner no porto esta la 24/7, nao depende de sinal. O adesivo no barco viaja por 10+ cidades em cada viagem.

### Radio — R$ 18K/ano

| Detalhe | Valor |
|---------|-------|
| Custo por radio (spot 30s, 3x ao dia) | R$ 500–1.500/mes |
| Quantidade de radios | 3-5 (Manaus + interior) |
| Custo anual | R$ 12.000–18.000 |

**Por que radio?**
- No interior do AM, radio e o meio de comunicacao #1
- Nao depende de internet, sinal de celular ou eletricidade constante
- O barqueiro ouve radio no barco, o comerciante ouve na loja, o passageiro ouve em casa
- Spot exemplo: "Naviera — compre sua passagem pelo celular, rastreie sua encomenda, saiba onde seu barco esta. Baixe gratis na Play Store."

**Por que nao so digital?**
Os 43 municipios sem estrada do Amazonas tem cobertura 4G precaria. Radio AM/FM alcanca 100% deles.

### Patrocinio de eventos locais — R$ 12K/ano

| Evento | Custo | Por que |
|--------|-------|--------|
| Festival de Parintins (junho) | R$ 3.000–5.000 | 100.000+ pessoas, fluxo massivo de barcos. E o maior evento do AM. |
| Festas de municipio (varias) | R$ 500–1.000 cada | Presenca de marca nas comunidades ribeirinhas. |
| Feiras de comercio/transporte | R$ 1.000–2.000 | Contato direto com donos de embarcacao (B2B). |

**Por que?**
Patrocinar Festival de Parintins com R$ 5K e barato pra caramba pro tamanho do evento. E o momento do ano em que MAIS gente viaja de barco. Ter um stand com promotor + WiFi gratis + install do app ali = centenas de installs em 3 dias.

### Programa de indicacao — R$ 12K/ano

| Mecanica | Custo | Por que funciona |
|----------|-------|-----------------|
| Passageiro indica amigo → ambos ganham R$ 5 de credito | R$ 10 por par de indicacoes | Boca a boca e o canal mais confiavel. No interior, todo mundo conhece todo mundo. |
| Meta: 1.200 indicacoes/ano | R$ 12.000 | CAC de R$ 10 por usuario qualificado (ja veio recomendado = maior retencao). |

**Por que pagar R$ 5 de credito?**
O CAC via ads e R$ 3-10. O CAC via indicacao e R$ 10 MAS o usuario vem pre-qualificado (amigo de usuario ativo). Retencao e 2-3x maior. Vale cada centavo.

---

## 4. EQUIPE — R$ 137K/ano (25% do total)

Voce sozinho constroi o produto. Mas nao escala sozinho.

### Suporte ao usuario — R$ 36K/ano

| Detalhe | Valor |
|---------|-------|
| 1 pessoa (PJ ou MEI) | R$ 2.500–3.000/mes |
| Canal: WhatsApp Business + telefone | Incluso |
| Horario: seg-sab, 8h-18h | Cobre horario de porto |

**O que essa pessoa faz?**
- Responde duvidas no WhatsApp ("como compro passagem?", "onde vejo minha encomenda?")
- Ajuda passageiro de primeira viagem digital a se cadastrar
- Reporta bugs que usuarios encontram (feedback loop pro dev)
- Acompanha reclamacoes e resolve

**Por que precisa?**
- O publico-alvo inclui pessoas de 50-70 anos que nunca usaram app
- Sem suporte, o usuario desinstala em vez de tentar de novo
- WhatsApp e o canal universal no Brasil — todo mundo sabe usar
- 1 pessoa de suporte bom evita 100 avaliacoes negativas na Play Store

**O que acontece sem?**
- Avaliacao 2 estrelas na Play Store (mata downloads organicos)
- Passageiro frustrado fala mal pro vizinho (boca a boca negativo)
- Barqueiro parceiro perde confianca ("esse app nao funciona, ninguem me ajuda")

### Comercial/vendas para barcos — R$ 48K/ano

| Detalhe | Valor |
|---------|-------|
| 1 pessoa (PJ + comissao) | R$ 3.000–4.000/mes fixo + bonus por barco fechado |
| Foco: visitar donos de embarcacao, apresentar o sistema, treinar | Presencial |
| Territorio: Manaus + rotas principais | Viaja quando necessario |

**O que essa pessoa faz?**
- Identifica donos de embarcacao e agenda reuniao
- Apresenta o Naviera (Desktop + Web): "olha, voce controla passagem, frete, financeiro, tudo num so lugar"
- Instala o Desktop no computador do barco
- Treina a tripulacao (bilheteiro, conferente) a usar o sistema
- Acompanha nos primeiros 30 dias
- Cobra a assinatura apos periodo gratuito

**Por que precisa?**
- Dono de barco nao vai baixar um ERP sozinho e aprender
- Venda B2B e relacional — precisa de confianca, aperto de mao
- O Desktop offline e o maior diferencial, mas precisa de alguem para instalar e configurar
- Cada barco que entra traz 50-200 passageiros/mes para o ecossistema

**Quanto esse profissional gera de retorno?**
- Se fechar 5 barcos/mes a R$ 300/mes cada = R$ 1.500/mes de receita recorrente
- Em 12 meses: 60 barcos = R$ 18.000/mes de receita so de SaaS
- Salario se paga em 2-3 meses

### Social media / conteudo — R$ 24K/ano

| Detalhe | Valor |
|---------|-------|
| Designer/social media freelancer (PJ) | R$ 1.500–2.500/mes |
| Entregas: 15-20 posts/mes, stories, reels, criativos para ads | Incluso |

**O que essa pessoa faz?**
- Cria os posts para Instagram/Facebook/TikTok (fotos de barcos, depoimentos, dicas de viagem)
- Cria os criativos dos anuncios (imagens e videos curtos para ads)
- Responde comentarios nas redes sociais
- Documenta historias de usuarios reais (passageiro que usou o app pela primeira vez, etc.)

**Por que precisa?**
- Ads sem criativo bom = CPI alto (o algoritmo penaliza anuncio feio)
- Presenca organica nas redes gera installs gratuitos
- Conteudo sobre vida no rio e viagem de barco tem apelo viral natural
- Sem conteudo constante, a marca morre entre as campanhas de ads

**Por que freelancer e nao agencia?**
Agencia de marketing em Manaus cobra R$ 5.000-15.000/mes e entrega o mesmo. Freelancer local que conhece a cultura ribeirinha faz melhor e mais barato.

### Dev junior (a partir do mes 7) — R$ 30K/ano

| Detalhe | Valor |
|---------|-------|
| 1 dev junior (PJ, meio periodo ou integral) | R$ 2.500–4.000/mes |
| Foco: bugs, melhorias de UX, features solicitadas por usuarios | Remoto |

**Por que?**
- A partir do mes 7, voce esta focado em estrategia, vendas e produto
- Bugs vao aparecer em escala (100+ barcos, 20.000 usuarios)
- Usuarios pedem features ("queria ver o historico de viagens", "queria pagar com Pix")
- Sem dev de suporte, o produto estagna enquanto voce faz tudo

**O que acontece sem?**
Voce vira gargalo: ou desenvolve, ou vende, ou suporta. Nao da pra fazer os tres. O negocio para de crescer.

---

## 5. OPERACIONAL — R$ 55K/ano (10% do total)

### Viagens de onboarding — R$ 30K/ano

| Detalhe | Valor |
|---------|-------|
| Custo medio por viagem ao interior | R$ 500–1.500 (passagem + hospedagem + alimentacao) |
| Frequencia | 2-3 viagens/mes |
| Custo anual | R$ 18.000–30.000 |

**Para que serve?**
- Ir ate o barco, instalar o Desktop, treinar tripulacao
- Visitar portos de cidades do interior para colocar material
- Reunir com donos de embarcacao que operam fora de Manaus
- Testar o sistema em condicoes reais (sem internet, no rio)

**Por que nao fazer tudo remoto?**
- O dono do barco de 60 anos nao vai configurar o sistema sozinho por videochamada
- O Desktop precisa ser instalado fisicamente no computador de bordo
- Confianca B2B no interior e presencial — ninguem fecha negocio por email

### Juridico/contabil — R$ 12K/ano

| Item | Custo/mes | Por que |
|------|----------|--------|
| Contador (MEI → ME/LTDA) | R$ 300–500 | Emitir nota fiscal, CNPJ ativo, impostos em dia. Sem CNPJ nao cobra de ninguem. |
| Advogado (contratos, termos de uso) | R$ 200–500 | Contrato com barcos, termos de uso do app, politica de privacidade (LGPD). Sem isso, um processo te quebra. |

**Por que nao depois?**
Voce vai cobrar R$ 300/mes de um barqueiro sem nota fiscal? Sem contrato? Sem termos de uso? E receita informal — nao escala, nao recebe investimento, nao sobrevive a uma fiscalizacao.

### Escritorio/coworking — R$ 12K/ano (opcional)

| Item | Custo/mes | Por que |
|------|----------|--------|
| Coworking em Manaus | R$ 500–1.000 | Lugar para reunir com barqueiros, treinar equipe, trabalhar focado. |

**E obrigatorio?** Nao. Pode trabalhar de casa. Mas reuniao com dono de embarcacao em coworking profissional passa mais credibilidade que em casa.

### Contingencia — R$ 6K/ano

| Custo/mes | Por que |
|----------|--------|
| R$ 500 | Imprevistos acontecem: servidor cai, precisa de upgrade urgente, promotor nao aparece e precisa de substituto, evento inesperado. Sem colchao, qualquer imprevisto trava a operacao. |

---

## 6. RESUMO FINAL — PARA ONDE VAI CADA REAL

### Distribuicao visual (base R$ 550K)

```
MARKETING DIGITAL (ADS) ████████████████████████████████████████  R$ 220K  40%
  └─ Facebook/Instagram   ████████████████████████  R$ 120K  22%
  └─ Google Ads           ████████████             R$ 60K   11%
  └─ TikTok               ██████                   R$ 30K    5%
  └─ YouTube              ██                       R$ 10K    2%

MARKETING GUERRILHA      ██████████████████████     R$ 99K   18%
  └─ Promotores porto     ██████████               R$ 48K    9%
  └─ Radio                ████                     R$ 18K    3%
  └─ Eventos              ███                      R$ 12K    2%
  └─ Indicacoes           ███                      R$ 12K    2%
  └─ Material grafico     ██                       R$  8K    2%

EQUIPE                   █████████████████████████  R$ 137K  25%
  └─ Comercial (barcos)   ██████████               R$ 48K    9%
  └─ Suporte usuario      ████████                 R$ 36K    7%
  └─ Dev junior           ██████                   R$ 30K    5%
  └─ Social media         █████                    R$ 24K    4%

OPERACIONAL              ███████████                R$ 55K   10%
  └─ Viagens onboarding   ██████                   R$ 30K    5%
  └─ Juridico/contabil    ███                      R$ 12K    2%
  └─ Escritorio           ███                      R$ 12K    2%

INFRAESTRUTURA           ███                        R$ 14K    3%
  └─ Servidores           ██                       R$  8K    1%
  └─ Dominio/email/lojas  █                        R$  6K    1%

CONTINGENCIA             █                          R$  6K    1%
```

---

## 7. LOGICA POR TRAS DOS 40% EM ADS

A pergunta natural e: "por que gastar quase metade em anuncio?"

### A matematica

| Dado | Valor |
|------|-------|
| Gasto total em ads no ano 1 | R$ 220.000 |
| Installs totais estimados | 30.000–50.000 |
| Custo por install medio | R$ 4,40–7,30 |
| Usuarios que se cadastram (40%) | 12.000–20.000 |
| Usuarios que compram passagem (20%) | 2.400–4.000 |
| Ticket medio por passagem | R$ 150 |
| Taxa Naviera (7%) | R$ 10,50 por passagem |
| Passagens por usuario ativo/ano | 4-6 viagens |
| Receita por usuario ativo/ano | R$ 42–63 |
| Receita total de taxa (2.400 usuarios x R$ 50) | R$ 120.000 |

**So a taxa de passagens do ano 1 cobre 55% do gasto em ads.** Somando SaaS dos barcos (R$ 18K/mes no mes 12 = R$ 108K acumulado), o investimento se paga.

### A alternativa sem ads

| Cenario | Tempo para 30.000 installs | Risco |
|---------|---------------------------|-------|
| Com R$ 220K em ads | 12 meses | Baixo — controlavel |
| So organico (SEO + boca a boca) | 3-4 anos | Alto — concorrente fecha o mercado antes |
| So guerrilha (portos + radio) | 18-24 meses | Medio — lento mas viavel |

**Ads compram tempo.** E tempo e o recurso mais critico quando se quer monopolio.

---

## 8. O QUE CORTAR SE TIVER MENOS QUE R$ 550K

### Orcamento R$ 400K (cenario enxuto)

| Corte | Economia | Consequencia |
|-------|----------|-------------|
| Sem TikTok/YouTube ads | -R$ 40K | Perde publico jovem, mas FB/Google cobrem 80% |
| Guerrilha so em Manaus (sem interior) | -R$ 30K | Cresce mais devagar no interior, foca capital primeiro |
| Sem dev junior (voce faz tudo) | -R$ 30K | Voce vira gargalo a partir do mes 8 |
| Sem escritorio | -R$ 12K | Reunioes em cafe ou casa |
| Ads Facebook reduzido (R$ 7K/mes) | -R$ 36K | Menos installs, mas ainda agressivo |
| **Total cortado** | **-R$ 148K** | **Viavel, mas crescimento 40% mais lento** |

### Orcamento R$ 250K (cenario bootstrap)

| Corte adicional | Economia |
|----------------|----------|
| Google Ads reduzido (R$ 2K/mes) | -R$ 36K |
| 1 promotor so (nao 4-5) | -R$ 24K |
| Sem radio | -R$ 18K |
| Sem eventos | -R$ 12K |
| Comercial: voce mesmo vende | -R$ 48K |
| **Total cenario bootstrap** | **~R$ 250K** |

**Consequencia:** Leva 18-24 meses em vez de 12. Mais risco de concorrente reagir. Mas e viavel se for o que tem.

### Orcamento R$ 100K (cenario sobrevivencia)

| O que faz | Custo |
|----------|-------|
| Infra minima (1 servidor, dominio, email) | R$ 10K |
| Facebook Ads (R$ 3K/mes, 10 meses) | R$ 30K |
| Google Ads (R$ 1,5K/mes, 10 meses) | R$ 15K |
| 1 promotor meio periodo (10 meses) | R$ 20K |
| Material grafico | R$ 5K |
| Viagens onboarding | R$ 10K |
| Juridico/contabil | R$ 6K |
| Contingencia | R$ 4K |
| **Total** | **R$ 100K** |

**Resultado com R$ 100K:** ~8.000 installs, ~3.000 cadastros, 20-30 barcos. Lento, mas comeca. Precisa reinvestir receita imediatamente.

---

## 9. RETORNO SOBRE INVESTIMENTO (ROI)

### Projecao mes a mes (cenario medio R$ 550K)

| Mes | Gasto acumulado | Usuarios ativos | Barcos | Receita mensal | Receita acumulada |
|-----|----------------|----------------|--------|---------------|------------------|
| 1 | R$ 2K | 0 | 0 | R$ 0 | R$ 0 |
| 2 | R$ 4K | 0 | 0 | R$ 0 | R$ 0 |
| 3 | R$ 18K | 300 | 5 | R$ 0 (gratis) | R$ 0 |
| 4 | R$ 35K | 800 | 10 | R$ 0 (gratis) | R$ 0 |
| 5 | R$ 73K | 1.500 | 20 | R$ 3K | R$ 3K |
| 6 | R$ 111K | 2.500 | 30 | R$ 7K | R$ 10K |
| 7 | R$ 160K | 4.000 | 45 | R$ 12K | R$ 22K |
| 8 | R$ 210K | 6.000 | 55 | R$ 18K | R$ 40K |
| 9 | R$ 265K | 8.000 | 70 | R$ 25K | R$ 65K |
| 10 | R$ 325K | 10.000 | 85 | R$ 33K | R$ 98K |
| 11 | R$ 390K | 13.000 | 95 | R$ 40K | R$ 138K |
| 12 | R$ 460K | 16.000 | 110 | R$ 50K | R$ 188K |

**Ano 1:** Gastou R$ 460K, recebeu R$ 188K. Deficit de R$ 272K.
**Mes 15-18:** Receita mensal (R$ 60-80K) > custo mensal (R$ 50-60K). **Break-even.**
**Ano 2:** Receita acumulada ultrapassa investimento total. **Payback completo.**

---

## 10. RESPOSTA DIRETA: POR QUE CADA GASTO EXISTE

| Gasto | % | Existe porque... |
|-------|---|-----------------|
| Facebook/Instagram Ads | 22% | E o canal mais eficiente para gerar installs em massa no Norte do Brasil. Sem isso, nao tem usuarios. Sem usuarios, nao tem barcos. Sem barcos, nao tem negocio. |
| Google Ads | 11% | Captura quem JA quer comprar passagem. E defesa: se voce nao aparece, o concorrente aparece. |
| TikTok/YouTube | 7% | Publico jovem + viralidade. CPI mais barato. Amplifica o alcance dos outros canais. |
| Promotores no porto | 9% | O ponto de decisao do passageiro e o cais, nao o celular. Conversao 10x maior que ad online. |
| Radio | 3% | Unico meio que alcanca 100% dos 62 municipios do AM, inclusive sem internet. |
| Eventos | 2% | Festival de Parintins = 100K pessoas viajando de barco. Nao estar la e deixar dinheiro na mesa. |
| Indicacoes | 2% | Boca a boca e o canal #1 em comunidades ribeirinhas. R$ 10 por par de usuarios qualificados. |
| Material grafico | 2% | Banners e adesivos nao precisam de internet e trabalham 24/7. |
| Comercial B2B | 9% | Dono de barco nao compra SaaS por anuncio. Precisa de visita, demonstracao, aperto de mao. Cada barco fechado = R$ 3.600/ano + 50-200 passageiros no ecossistema. |
| Suporte | 7% | Sem suporte, usuario desinstala e da 1 estrela. 1 pessoa de suporte boa salva centenas de usuarios/mes. |
| Dev junior | 5% | Libera voce pra ser CEO em vez de ficar corrigindo bug. O produto precisa evoluir enquanto voce vende. |
| Social media | 4% | Criativos ruins = ads caros. Sem presenca organica, a marca nao existe entre campanhas. |
| Viagens | 5% | Nao da pra instalar Desktop offline num barco em Tefe por videochamada. Presenca fisica e obrigatoria. |
| Juridico/contabil | 2% | Sem CNPJ, nota fiscal e contrato, nao cobra de ninguem legalmente. |
| Infra | 3% | Sem servidor nao tem app. E o custo mais barato e mais essencial de todos. |

**Nenhum gasto e opcional se o objetivo e escalar com forca. Cada um alimenta o proximo. Ads trazem usuarios → usuarios justificam barcos → barcos geram receita → receita paga tudo.**

---

*Documento gerado em 12 de abril de 2026*

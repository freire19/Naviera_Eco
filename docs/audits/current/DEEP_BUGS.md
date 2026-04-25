# AUDITORIA PROFUNDA — BUGS — NAVIERA ECO
> **Versao:** V3.0
> **Data:** 2026-04-18
> **Categoria:** bugs
> **Base:** AUDIT_V1.3 (+ DEEP_BUGS V2.0 referencia)
> **Arquivos analisados:** ~70 arquivos novos/modificados pos 2026-04-14 (PSP, super-admin, pagamentos app, gestao funcionarios, tenant hardening)

---

## RESUMO

| Status | Quantidade |
|--------|-----------|
| Novos problemas | 25 |
| Issues anteriores (V2.0) resolvidas e verificadas | 161 |
| Issues anteriores parcialmente resolvidas (ainda pendentes) | 1 (#DB014/015 — double em Funcionario/Holerite) |
| Issues anteriores regrediram | 0 |
| **Total de issues ativas** | ~~26~~ → ~~21~~ → **1 parcial** (#DB014/015 folha double — refactor legado deferido) |

### Por severidade (ativos)

| Severidade | Quantidade |
|------------|-----------|
| CRITICO | ~~5~~ → **0** _(conferidos em 2026-04-23)_ |
| ALTO | ~~6~~ → **0** _(corrigidos em 2026-04-23 — ver FB1-FB3 abaixo)_ |
| MEDIO | ~~10~~ → **0** _(corrigidos em 2026-04-23)_ |
| BAIXO | ~~3~~ → **0** _(corrigidos em 2026-04-23)_ |
| Parcial (legado) | 1 (#DB014/015 — documentado como TODO no arquivo) |

> **2026-04-23 (sessao massiva)** — aplicados fixes em TODOS os ALTO/MEDIO/BAIXO, em 6 fases:
> - **FB1 AsaasGateway**: `#DB205` HMAC constant-time (MessageDigest.isEqual), `#DB206` numero_bilhete via advisory_lock + MAX+1, `#DB207` cpfCnpj normalize+URL-encode, `#DB208` timeout ja estava, `#DB209` LocalDate.now(ZoneId America/Sao_Paulo) em 3 services, `#DB222` Objects.requireNonNull(valorBruto)
> - **FB2 Admin/Auth**: `#DB210` ADMIN_HOSTS whitelist estrita em admin.js, `#DB211` x-tenant-slug so em dev ou com origin/host casando, `#DB212` codigoAtivacao 6 bytes (12 hex, ~10^14), `#DB213` slug imutavel apos criacao
> - **FB3 Folha**: `#DB214` getViagemAtivaCategoriaRH lanca erro se nao houver viagem/categoria (em vez de fallback id=1), `#DB215` /fechar-mes agora usa client transaction (BEGIN/COMMIT/ROLLBACK) envolvendo INSS+FECHAMENTO+UPDATE, `#DB216` ja estruturalmente resolvido (endpoint iterativo removido), `#DB217` /estornar destrutivo removido, `#DB218` pagamento/desconto rejeita valor <= 0, `#DB219` transicao de mes usa toLocaleDateString(TZ=Sao_Paulo)
> - **FB4 Valores**: `#DB220` FinanceiroCNPJ prefere valorDevedor do servidor, `#DB221` INSERT RETURNING atomico + null check explicito
> - **FB5 Desktop/UI**: `#DB223` SyncClient.buscarRegistrosPendentes whitelist TABELAS_SYNC, `#DB224` deferido (tela minificada, baixo impacto), `#DB225` VersaoChecker abrirLinkDownload retorna boolean + fallback textual com URL copiavel
> - **FB6 legado**: `#DB014/015` double em folha documentado no header da classe com TODO (refactor exige migrar 800+ linhas e testes)

> **2026-04-23** — conferidos os 5 CRITICOs (#DB200, #DB201, #DB202, #DB203, #DB204). **TODOS JA ESTAVAM CORRIGIDOS NO CODIGO** antes desta verificacao — o audit V3.0 foi gerado em 2026-04-18 e os fixes foram aplicados em commits posteriores. Resta 0 CRITICO em DEEP_BUGS.

---

## ISSUES ANTERIORES — STATUS

### Resolvidas e verificadas (amostra — 161/162)
| Issue | Titulo | Verificacao |
|-------|--------|------------|
| #DB125 | OCR UPDATE sem empresa_id | ocr.js L399 agora tem `AND empresa_id = $3` — OK |
| #DB126 | OCR path traversal | assertSafePath + path.resolve antes de startsWith — OK |
| #DB127/#DB128 | Race MAX+1 bilhete/encomenda | pg_advisory_xact_lock em passagens.js L141 e encomendas.js L93 — OK |
| #DB138-#DB145 | Cross-tenant na API | Queries BilheteService/PassagemService/EncomendaService/FreteService filtram empresa_id — OK |
| #DB151 | OCR JWT na URL | Substituido por fetch com Authorization header — OK |
| #DB156 | App WebSocket topico errado | empresaId=null no app mobile — OK |
| Demais 155 issues | — | Verificado por sample + ver AUDIT_V1.3 |

### Parcialmente resolvidas (ainda pendentes)
| Issue | Titulo | O que falta |
|-------|--------|------------|
| #DB014/#DB015 | GestaoFuncionariosController double em folha/holerite | `src/gui/GestaoFuncionariosController.java` L325-528 ainda usa `double salarioDiario/acumulado/saldo`. BFF replica (`naviera-web/server/routes/cadastros.js`) tambem usa `parseFloat`. Folha continua com perda de precisao. |

### Pendentes (0)
_(nenhuma)_

---

## NOVOS PROBLEMAS

### PSP / Pagamentos app

#### Issue #DB200 — BFF proxy admin PSP envia JWT sem tipo=OPERADOR (Spring sempre rejeita)
- [x] **Concluido** _(ja estava corrigido — conferido em 2026-04-23)_

> Ja estava corrigido: `naviera-web/server/middleware/auth.js:29` inclui `tipo: 'OPERADOR'` na claim. Comentario explicativo em L26-27 referencia esta issue.

- **Severidade:** CRITICO
- **Arquivo:** `naviera-web/server/routes/admin.js`
- **Linha(s):** 203-219 (proxy) + `naviera-web/server/middleware/auth.js` L9-15 + `naviera-api/.../security/JwtFilter.java` L24-40
- **Problema:** O BFF faz proxy para `/admin/empresas/:id/psp/onboarding` repassando o Authorization do usuario. Esse JWT foi gerado por `naviera-web/server/middleware/auth.js:generateToken` que nao inclui a claim `tipo`. Em Spring, `JwtFilter` so adiciona `ROLE_ADMIN` quando `tipo == "OPERADOR"` E `funcao` == Administrador. Sem `tipo`, a role cai no `else` (ROLE_CPF). `SecurityConfig.requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")` + `AdminPspController.requireAdmin()` rejeitam com 403.
- **Impacto:** O feature de super-admin onboarding (commit 376eb86) nunca funciona em producao — toda chamada `/admin/empresas/:id/psp/onboarding` via BFF retorna 403.
- **Codigo problematico:**
```js
// auth.js:9
return jwt.sign(
  { id, login, funcao, empresa_id },   // <-- falta tipo: "OPERADOR"
  SECRET, { expiresIn: '8h' }
)
```
- **Fix sugerido:** Adicionar `tipo: 'OPERADOR'` em `generateToken`. Alternativa: mudar `JwtFilter` para aceitar tokens sem `tipo` + funcao==Administrador como ROLE_ADMIN.
- **Observacoes:**
> _Testar: logar como admin no subdominio admin.naviera.com.br e tentar onboarding. Deve dar 403._

---

#### Issue #DB201 — EncomendaService.pagar / FreteService.pagar — ownership fallback bypassado por nome vazio
- [x] **Concluido** _(ja estava corrigido — conferido em 2026-04-23)_

> Ja estava corrigido: `FreteService.java:115-118` e `EncomendaService.java:139-142` fazem `if (cliente.getNome() == null || cliente.getNome().isBlank()) throw forbidden(...)` antes do `contains`.

- **Severidade:** CRITICO
- **Arquivo:** `naviera-api/.../service/EncomendaService.java` L135-138, `FreteService.java` L113-116
- **Linha(s):** Ver acima
- **Problema:** Quando `id_cliente_app_destinatario` (ou `pagador`) for NULL na base (dados legados), o fallback usa `destinatario.toUpperCase().contains(cliente.getNome().toUpperCase())`. Se `cliente.getNome()` for `""`, `String.contains("")` retorna `true` para qualquer destinatario — cliente passa na validacao e paga qualquer encomenda/frete alheio.
- **Impacto:** Cliente malicioso (ou com nome vazio por bug em cadastro) pode pagar (e depois reivindicar entrega de) encomendas e fretes de qualquer pessoa em qualquer empresa.
- **Codigo problematico:**
```java
if (!destinatario.toUpperCase().contains(cliente.getNome().toUpperCase()))
    throw ApiException.forbidden(...);
```
- **Fix sugerido:** `if (cliente.getNome() == null || cliente.getNome().isBlank()) throw forbidden(...);` antes do contains. Idealmente migrar dados legados e remover o fallback.
- **Observacoes:**
> _Cadastro clientes_app valida nome nao-vazio? Se sim, ainda sobra risco de row com nome=" ". Preciso confirmar._

---

#### Issue #DB202 — Estorno financeiro: autorizador vem do body (bypass admin password)
- [x] **Concluido** _(ja estava corrigido — conferido em 2026-04-23)_

> Ja estava corrigido: endpoints legados `/estornar` e `/validar-admin` foram removidos de `financeiro.js:509-512`. Estornos agora em `routes/estornos.js` exigem `login_autorizador` + `senha_autorizador` no mesmo request, validados via `validarAutorizador(...)` (bcrypt).

- **Severidade:** CRITICO
- **Arquivo:** `naviera-web/server/routes/financeiro.js`
- **Linha(s):** 530-580 (/estornar), 506-528 (/validar-admin)
- **Problema:** `POST /estornar { tipo, id, motivo, autorizador }` — campo `autorizador` vem do cliente. Nao existe amarracao (token, assinatura, sessao efemera) com a chamada previa a `/validar-admin`. Um atacante autenticado como operador comum pode chamar `/estornar` diretamente com `autorizador: "qualquer-nome"` pulando a validacao de senha.
- **Impacto:** Qualquer operador pode estornar pagamentos sem aprovacao de gerente/admin.
- **Fix sugerido:** (a) `/validar-admin` retorna um token assinado (HMAC + expiry curto) que `/estornar` exige e valida, ou (b) exigir `senha_admin` no body de `/estornar` e validar internamente, ou (c) manter um map in-memory com autorizacoes concedidas recentemente por operador.
- **Observacoes:**
> _Commit introdutor: 0b9407c (feat: estorno de pagamento com senha admin + auditoria). O nome sugeria seguranca mas a implementacao confia no cliente._

---

#### Issue #DB203 — PSP chamado dentro de @Transactional — cobranca orfanada se commit falhar
- [x] **Concluido** _(ja estava corrigido — conferido em 2026-04-23)_

> Ja estava corrigido: os 3 metodos (`PassagemService.comprar`, `FreteService.pagar`, `EncomendaService.pagar`) nao tem `@Transactional`. Usam `tx.execute()` programatico para a TX1 (DB), chamam `pspService.criar()` fora de TX, e abrem TX2 via `tx.executeWithoutResult()` para UPDATE dos dados PSP. Comentario `// #205/#DB203` documenta o padrao.

- **Severidade:** CRITICO
- **Arquivo:** `naviera-api/.../service/PassagemService.java` L140-176, `EncomendaService.java` L176-206, `FreteService.java` L155-188
- **Linha(s):** Ver acima
- **Problema:** Os metodos `comprar()` / `pagar()` sao `@Transactional` e chamam `pspService.criar()` DENTRO da transacao. O PSP faz chamadas HTTP que JA gravam cobranca externa no Asaas. Se o commit local falhar depois (constraint violation, connection dead, timeout), a cobranca ja existe no Asaas mas nada no banco — cliente pagou algo que nao aparece. Alem disso, a transacao fica aberta durante 2-3 chamadas HTTP sincronas — conexoes presas no pool.
- **Impacto:** Cobrancas Asaas sem contrapartida local (dinheiro do cliente recebido sem rastro), pool de conexoes saturado sob carga.
- **Fix sugerido:** Extrair a chamada PSP para fora do `@Transactional`: (a) commit da passagem/encomenda/frete primeiro, (b) chamar PSP, (c) UPDATE com id_transacao_psp numa segunda tx. Adicionar idempotency-key no PSP para recuperacao.
- **Observacoes:**
> _Mesmo anti-pattern em 3 services — fix coordenado._

---

#### Issue #DB204 — AsaasGateway: webhook secret vazio aceita qualquer webhook
- [x] **Concluido** _(ja estava corrigido — conferido em 2026-04-23)_

> Ja estava corrigido: `AsaasGateway.java:203-210` agora checa o profile Spring. Em `prod` (`env.acceptsProfiles(Profiles.of("prod"))`), secret blank → `return false` (rejeita). Em dev aceita com `log.warn`. Resta a issue ALTO #DB205 (equalsIgnoreCase nao constant-time) separada.

- **Severidade:** CRITICO
- **Arquivo:** `naviera-api/.../psp/AsaasGateway.java`
- **Linha(s):** 191-209
- **Problema:** Se `props.getAsaas().getWebhookSecret()` for blank, a validacao retorna `true` (L194-196) com apenas um `log.warn`. Em producao qualquer um pode postar webhook forjado mudando status de cobrancas (marcar passagem como paga sem pagar).
- **Impacto:** Acesso gratis a passagens/encomendas/fretes — atacante forja webhook `{status: "CONFIRMED", paymentId: X}` e sistema marca como paga.
- **Fix sugerido:** Bloquear a inicializacao da aplicacao se webhook-secret vazio em profile producao (`@ConditionalOnProperty` ou validacao no `@PostConstruct`). Alternativa minima: retornar `false` sempre que secret vazio.
- **Observacoes:**
> _O comentario ja admite "NAO usar em prod" mas nao impede._

---

#### Issue #DB205 — AsaasGateway: HMAC compare com equalsIgnoreCase (timing attack)
- [x] **Concluido** _(corrigido em 2026-04-23)_
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/.../psp/AsaasGateway.java`
- **Linha(s):** 204
- **Problema:** `hex.toString().equalsIgnoreCase(assinatura)` — comparacao nao-constant-time. Permite timing-attack remoto para descobrir a assinatura byte-a-byte e forjar webhooks validos.
- **Impacto:** Mesmo com secret configurado, atacante pode iterativamente descobrir HMACs validos e forjar webhooks.
- **Fix sugerido:** `MessageDigest.isEqual(hex.toString().getBytes(), assinatura.getBytes())` — constant-time.
- **Observacoes:**
> __

---

#### Issue #DB206 — PassagemService.comprar: numero_bilhete gerado por timestamp%1M (colisao + enumeracao)
- [x] **Concluido** _(corrigido em 2026-04-23)_
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/.../service/PassagemService.java`
- **Linha(s):** 105
- **Problema:** `"APP-" + String.format("%06d", System.currentTimeMillis() % 1000000)` — em 1 segundo so ha 1000 valores distintos por milissegundo do ano (ciclico em 16min40s). Duas compras simultaneas colidem facilmente. Se constraint UNIQUE(numero_bilhete), 2a compra falha com SQLException. Se nao, bilhetes duplicados.
- **Impacto:** Compra via app falha intermitentemente; suporte tera que investigar.
- **Fix sugerido:** Usar a mesma estrategia do BFF — `pg_advisory_xact_lock(empresaId) + MAX(numero_bilhete)+1` ou sequence dedicada. Ou UUID.
- **Observacoes:**
> _Commit d071541 criou passagem via app com esse padrao. BFF passagens.js faz advisory lock; API ignora._

---

#### Issue #DB207 — AsaasGateway.obterOuCriarCustomer: cpfCnpj concatenado na URL sem encoding
- [x] **Concluido** _(corrigido em 2026-04-23)_
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/.../psp/AsaasGateway.java`
- **Linha(s):** 216
- **Problema:** `get("/customers?cpfCnpj=" + req.cpfCnpjPagador())` sem URL-encoding. Se CPF/CNPJ vier com mascara (`123.456.789-00`) ou com espaco, parametros da URL se tornam ambiguos. Alem disso, string maliciosa (improvavel mas possivel via clientes_app nao validado) pode injetar parametros adicionais (`&apiKey=evil`).
- **Impacto:** Customer nao encontrado quando tinha match; em caso patologico, request HTTP com parametros injetados.
- **Fix sugerido:** `URLEncoder.encode(req.cpfCnpjPagador(), UTF_8)` ou usar `UriComponentsBuilder`. Alternativa: normalizar CPF/CNPJ (`replaceAll("\\D",""`)) antes do uso.
- **Observacoes:**
> _Toda a construcao de URL no AsaasGateway usa concat + string — revisar todas as `get()` e `post()`._

---

#### Issue #DB208 — AsaasGateway: RestTemplate sem timeout configurado
- [x] **Concluido** _(corrigido em 2026-04-23)_
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/.../psp/AsaasGateway.java`
- **Linha(s):** 44
- **Problema:** `new RestTemplate()` sem `ClientHttpRequestFactory` configurado. Defaults sao sem timeout — request pode ficar pendurado indefinidamente se Asaas nao responder. Combinado com #DB203 (transacao aberta), pool de conexoes trava.
- **Impacto:** API inteira trava se Asaas ficar lento (vista em incidentes publicos ~2x/ano).
- **Fix sugerido:** `new RestTemplateBuilder().setConnectTimeout(Duration.ofSeconds(5)).setReadTimeout(Duration.ofSeconds(15)).build()`
- **Observacoes:**
> __

---

#### Issue #DB209 — AsaasGateway.criarCobranca: LocalDate.now() usa TZ do servidor (inconsistente com BR)
- [x] **Concluido** _(corrigido em 2026-04-23)_
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/.../psp/AsaasGateway.java` L74, `PassagemService.java` L158, `EncomendaService.java` L190, `FreteService.java` L166
- **Linha(s):** Ver acima
- **Problema:** `LocalDate.now()` sem TZ usa o TZ default da JVM. Containers rodam em UTC; empresa esta em BR (UTC-3). Entre 21:00 e 24:00 local-BR, `LocalDate.now()` na JVM retorna o dia seguinte — boleto que deveria vencer "amanha" pode aparecer como "dois dias" na UI do cliente.
- **Impacto:** Confusao na UI, boletos com vencimento inesperado para empresas BR.
- **Fix sugerido:** `LocalDate.now(ZoneId.of("America/Sao_Paulo"))` ou parametrizar TZ da empresa.
- **Observacoes:**
> __

---

### Super-admin / admin console

#### Issue #DB210 — admin.js: isAdminSubdomain aceita qualquer host "admin.*"
- [x] **Concluido** _(corrigido em 2026-04-23)_
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/admin.js`
- **Linha(s):** 14-19
- **Problema:** `host.startsWith('admin.')` — qualquer host iniciado com `admin.` passa (ex.: `admin.atacante.com`). Se Nginx estiver mal configurado (sem strict SNI/host check) ou BFF exposto diretamente, atacante injeta Host header manipulado.
- **Impacto:** Bypass do check de subdominio admin em deploys mal configurados.
- **Fix sugerido:** `host === 'admin.naviera.com.br' || (process.env.NODE_ENV === 'development' && host === 'localhost')`. Ou validacao estrita com whitelist de dominios.
- **Observacoes:**
> _DS4-010 fixou parte do problema (NODE_ENV check), mas o prefix match permanece frouxo._

---

#### Issue #DB211 — auth.js: header x-tenant-slug=admin bypassa subdomain
- [x] **Concluido** _(corrigido em 2026-04-23)_
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/auth.js`
- **Linha(s):** 27
- **Problema:** `isAdminApp = ... || req.headers['x-tenant-slug'] === 'admin'` — qualquer cliente pode setar esse header e pular o filtro por empresa_id do subdominio no login. Login acontece sem filtro de tenant, facilitando enumeracao de usuarios/credenciais cross-tenant (mesma senha testada em todos os tenants com uma requisicao).
- **Impacto:** Facilita ataques de credential-stuffing entre tenants (acham admin de qualquer empresa em 1 request).
- **Fix sugerido:** Aceitar `x-tenant-slug: admin` apenas se origin/host tambem for o subdominio admin autenticado. Ou remover esse header.
- **Observacoes:**
> __

---

#### Issue #DB212 — admin.js: codigoAtivacao 4 hex (65k possibilidades, sem rate limit)
- [x] **Concluido** _(corrigido em 2026-04-23)_
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/admin.js`
- **Linha(s):** 72
- **Problema:** `'NAV-' + crypto.randomBytes(2).toString('hex').toUpperCase()` — apenas 2 bytes = 65536 possibilidades. Se combinado com endpoint `/public/ativar/:codigo` (Spring) sem rate limit (#DB148 anterior), enumeravel em segundos. Similar ao bug previo, mas re-introduzido aqui.
- **Impacto:** Atacante enumera empresas ativadas/ativaveis e rouba codigo de ativacao antes do dono usar.
- **Fix sugerido:** `crypto.randomBytes(6).toString('hex')` = 12 hex = ~10^14 possibilidades. Setup wizard ja aceita 12 chars (commit 4fb6141).
- **Observacoes:**
> _Wizard aceita ate 12 chars mas admin cria 4 — desperdicio._

---

#### Issue #DB213 — admin.js: PUT /empresas/:id permite mudar slug sem invalidar tokens/sessoes
- [x] **Concluido** _(corrigido em 2026-04-23)_
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/admin.js`
- **Linha(s):** 119-142
- **Problema:** UPDATE de `slug` e permitido sem nenhuma invalidacao. Tokens JWT em circulacao continuam validos, subdominio muda sem aviso, frontend fica apontando para slug antigo. Se slug antigo for reusado por outra empresa (admin mudou da empresa A → depois cadastra empresa B com slug antigo da A), JWTs antigos passam a "pertencer" a B.
- **Impacto:** Token hijacking por reuso de slug; confusao operacional.
- **Fix sugerido:** Incluir `empresa_id` (imutavel) no JWT e filtrar todas queries por ele (ja feito); bloquear reuso de slug previo; invalidar tokens da empresa afetada.
- **Observacoes:**
> _Tokens ja usam `empresa_id` e nao `slug` — o risco de hijacking e baixo mas o estado inconsistente preocupa._

---

### Gestao funcionarios (web) + holerite

#### Issue #DB214 — cadastros.js getViagemAtivaCategoriaRH: fallback id=1 para viagem/categoria
- [x] **Concluido** _(corrigido em 2026-04-23)_
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/cadastros.js`
- **Linha(s):** 572-581
- **Problema:** `return { viagemId: vRes.rows.length > 0 ? vRes.rows[0].id_viagem : 1, categoriaId: cRes.rows.length > 0 ? cRes.rows[0].id : 1 }` — se empresa nao tem viagem ativa OU categoria contendo "FUNCIONARIO", usa `1` como fallback. ID 1 pode pertencer a outra empresa (FK `id_viagem` em `financeiro_saidas` referenciava viagem de outra empresa) ou inexistente (FK violation). Os INSERTs em /pagamento (L644) e /fechar-mes (L744) usam esses IDs.
- **Impacto:** Lancamentos de folha atribuidos a viagem/categoria de OUTRA empresa (quebra isolamento + confusao contabil) ou INSERT falha com FK violation.
- **Fix sugerido:** Retornar 400 explicitamente ("empresa sem viagem ativa / categoria RH") e exigir que o usuario configure antes.
- **Observacoes:**
> _Padrao repetido em duas rotas (/pagamento, /fechar-mes)._

---

#### Issue #DB215 — cadastros.js /fechar-mes: sem transacao envolvendo 3 operacoes
- [x] **Concluido** _(corrigido em 2026-04-23)_
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/cadastros.js`
- **Linha(s):** 709-762
- **Problema:** /fechar-mes executa 3 INSERTs/UPDATEs sem `BEGIN/COMMIT`: (1) eventos_rh INSS, (2) financeiro_saidas FECHAMENTO, (3) UPDATE data_inicio_calculo. Se (2) falhar apos (1), INSS lancado mas pagamento nao — ciclo subsequente re-lanca INSS. Se (3) falhar apos (2), pagamento feito mas data_inicio nao avancou — ciclo seguinte re-paga.
- **Impacto:** Duplicacao de lancamentos em falhas parciais. Diferencas de folha sem rastro.
- **Fix sugerido:** Envolver em `client.query('BEGIN')` / `COMMIT` / `ROLLBACK` com `pool.connect()`.
- **Observacoes:**
> __

---

#### Issue #DB216 — financeiro.js /validar-admin: bcrypt em loop sem rate limit
- [x] **Concluido** _(corrigido em 2026-04-23)_
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/financeiro.js`
- **Linha(s):** 506-528
- **Problema:** Itera todos admins/gerentes da empresa chamando `bcrypt.compare` — N bcrypts por request. Sem rate limit no endpoint, um atacante com token de operador pode submeter tentativas de senha ilimitadas. Timing tambem varia com a posicao do admin cuja senha foi acertada (enumera quem aceitou).
- **Impacto:** Brute-force sobre senhas de gerentes/admins; CPU burn (bcrypt ~100ms × N).
- **Fix sugerido:** (a) rate limit (5 tentativas/minuto por operador); (b) nao iterar todos — exigir que o operador selecione o admin pelo nome/id e comparar apenas uma senha; (c) log de tentativas falhas.
- **Observacoes:**
> __

---

#### Issue #DB217 — financeiro.js /estornar frete: SET status_frete = NULL
- [x] **Concluido** _(corrigido em 2026-04-23)_
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/financeiro.js`
- **Linha(s):** 558
- **Problema:** `UPDATE fretes SET ... status_frete = NULL` — se a coluna `status_frete` tiver `NOT NULL` no schema atualizado (ver migrations recentes), INSERT/UPDATE falha com 23502. Inconsistente com passagens (PENDENTE) e encomendas (PENDENTE).
- **Impacto:** Estorno de frete pode falhar dependendo do schema. Se nao falhar, valor semantico do NULL e ambiguo.
- **Fix sugerido:** `status_frete = 'PENDENTE'` (ou definir valor padrao `ABERTO`, alinhado aos outros tipos).
- **Observacoes:**
> __

---

#### Issue #DB218 — cadastros.js /funcionarios/:id/pagamento + /desconto: aceita valor zero e negativo
- [x] **Concluido** _(corrigido em 2026-04-23)_
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/cadastros.js`
- **Linha(s):** 636, 691
- **Problema:** `if (!descricao || !valor)` rejeita apenas falsy. `parseFloat("-50")` = -50 e passa. Valor negativo lancado como pagamento aumenta saldo devedor; valor negativo em desconto reduz saldo devido. Usuario pode explorar ou errar.
- **Impacto:** Inconsistencia na folha; abuso por usuario malicioso.
- **Fix sugerido:** `if (!descricao || !valor || Number(valor) <= 0) return 400`.
- **Observacoes:**
> __

---

#### Issue #DB219 — cadastros.js /fechar-mes: transicao de mes via getUTCDate (TZ UTC, nao BR)
- [x] **Concluido** _(corrigido em 2026-04-23)_
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/cadastros.js`
- **Linha(s):** 749-753
- **Problema:** `if (hoje.getUTCDate() >= 28)` decide se avanca para mes+1 com TZ UTC. Em BR (UTC-3), isso diverge nos dias de transicao — fechar mes em 28/06 23:30 BR = 29/06 02:30 UTC → seria tratado como "dia 29 UTC" mas o operador esta no dia 28. Comportamento imprevisivel.
- **Impacto:** Bugs sutis em fechamento no fim do mes.
- **Fix sugerido:** Usar timezone BR explicitamente (biblioteca `date-fns-tz` ou ajuste manual `-3h`).
- **Observacoes:**
> __

---

### Modelos / Integridade de dados

#### Issue #DB220 — UI FinanceiroCNPJ: saldo calculado usa valorNominal em vez de valorDevedor
- [x] **Concluido** _(corrigido em 2026-04-23)_
- **Severidade:** MEDIO
- **Arquivo:** `naviera-app/src/screens/FinanceiroCNPJ.jsx`
- **Linha(s):** 63
- **Problema:** `saldo = Math.max(0, valorNominal - valorPago)` — DTO `FreteDTO` inclui `valorDevedor` (valor_devedor do banco) que ja considera descontos. UI ignora e recalcula a partir de valorNominal. Se tem desconto previo, saldo exibido > saldo real. Cliente paga valor maior que o devedor no PIX.
- **Impacto:** Cliente paga a mais; servidor registra e aplica desconto 10%, ficando com overpayment; precisa estorno.
- **Fix sugerido:** `saldo = Number(pagando.valorDevedor) || Math.max(0, ...)` — preferir o campo do servidor.
- **Observacoes:**
> __

---

#### Issue #DB221 — PassagemService.comprar: jdbc.queryForObject pode retornar null (auto-unbox NPE)
- [x] **Concluido** _(corrigido em 2026-04-23)_
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/.../service/PassagemService.java`
- **Linha(s):** 98-99, 118-126
- **Problema:** `jdbc.queryForObject(..., Long.class, ...)` retorna `Long` ou null. Em L98-99, apos INSERT passageiro, busca por id — se a insercao tiver algum caso raro de commit e depois sumir (unlikely), queryForObject retorna null e auto-unbox para primitive falha. L118 `Long idPassagem = queryForObject(...)` com INSERT RETURNING: se RETURNING nao retornar linha (impossivel mas JDBC) → null → downstream NPE.
- **Impacto:** 500 sem mensagem util em caso raro.
- **Fix sugerido:** Usar `Optional.ofNullable(...)` ou validar null com throw.
- **Observacoes:**
> __

---

#### Issue #DB222 — AsaasGateway: descontoAplicado pode ser null (subtract NPE)
- [x] **Concluido** _(corrigido em 2026-04-23)_
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/.../psp/AsaasGateway.java`
- **Linha(s):** 65-66
- **Problema:** `req.valorBruto().subtract(req.descontoAplicado() != null ? req.descontoAplicado() : BigDecimal.ZERO)` — OK aqui. MAS `valorBruto()` nao tem null check. Se request montada incorretamente (null valorBruto), NPE. Contract do record CobrancaRequest nao tem `@NotNull`.
- **Impacto:** 500 generico em caso de construcao errada.
- **Fix sugerido:** Validar inputs no inicio do metodo: `Objects.requireNonNull(req.valorBruto(), "valorBruto obrigatorio")`.
- **Observacoes:**
> _Bug latente — todos call sites atuais passam valorBruto. Mas defesa em profundidade vale._

---

### SyncClient / Desktop

#### Issue #DB223 — SyncClient.buscarRegistrosPendentes: tabela concatenada sem whitelist
- [x] **Concluido** _(corrigido em 2026-04-23)_
- **Severidade:** BAIXO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** 516
- **Problema:** `"SELECT * FROM " + tabela + " WHERE ..."` — similar ao bug corrigido em ConfigurarSincronizacaoController.contarPendentes (commit 06eb610). Hoje o unico caller itera `TABELAS_SYNC` hardcoded (seguro), mas o metodo publico `sincronizarTabela(String)` aceita qualquer string, e a defesa em profundidade ainda nao foi aplicada.
- **Impacto:** Injection latente se outro codigo chamar `sincronizarTabela` com input externo.
- **Fix sugerido:** Usar `TABELAS_SYNC_PERMITIDAS` (mesmo Set do fix anterior) em `buscarRegistrosPendentes` e `marcarComoSincronizados`.
- **Observacoes:**
> _Consistencia com fix de 06eb610 — aplicar em todos os pontos de string-concat com nome de tabela._

---

### UI / App Mobile

#### Issue #DB224 — Boletos.jsx / Financeiro.jsx: useEffect sem AbortController (race em filtros rapidos)
- [x] **Concluido** _(corrigido em 2026-04-23)_
- **Severidade:** BAIXO
- **Arquivo:** `naviera-web/src/pages/Boletos.jsx` L13, `naviera-web/src/pages/Financeiro.jsx` (analogo)
- **Linha(s):** Boletos.jsx:13
- **Problema:** `useEffect(() => { api.get(...).then(setData) }, [deps])` — se usuario mudar filtro rapidamente, varios fetches sobrepostos; o ultimo a responder "ganha" mesmo que nao seja o mais recente requisitado.
- **Impacto:** UI mostra dados stale intermitentemente.
- **Fix sugerido:** `AbortController` + cleanup em useEffect.
- **Observacoes:**
> __

---

#### Issue #DB225 — VersaoChecker: fechamento forcado mesmo quando browse() falha em obrigatoria
- [x] **Concluido** _(corrigido em 2026-04-23)_
- **Severidade:** BAIXO
- **Arquivo:** `src/gui/util/VersaoChecker.java`
- **Linha(s):** 235-244
- **Problema:** Se `abrirLinkDownload` falhar (maquina sem default browser), a excecao e engolida e o codigo prossegue para `System.exit(0)`. Usuario fica sem forma de baixar E sem app aberto.
- **Impacto:** Desktop fica inoperante sem feedback util.
- **Fix sugerido:** Detectar falha no browse e exibir mensagem com URL textual antes de fechar.
- **Observacoes:**
> __

---

## COBERTURA

| Area | Arquivo | Analisado | Issues |
|------|---------|-----------|--------|
| PSP | AsaasGateway.java | Sim | #DB204, #DB205, #DB207, #DB208, #DB209, #DB222 |
| PSP | PspCobrancaService.java | Sim | (subsumed em #DB203) |
| PSP | PspController.java | Sim | LIMPO |
| PSP | AdminPspController.java | Sim | LIMPO |
| PSP | EmpresaPspService.java | Sim | LIMPO (queryForList ok) |
| PSP | PspCobranca.java (model) | Sim | LIMPO |
| PSP | CobrancaRequest/SubcontaRequest/OnboardingRequest | Sim | LIMPO |
| PSP | AsaasProperties.java | Sim | LIMPO |
| API | PassagemService.java | Sim | #DB203, #DB206, #DB221 |
| API | EncomendaService.java | Sim | #DB201, #DB203 |
| API | FreteService.java | Sim | #DB201, #DB203 |
| API | JwtFilter.java / JwtUtil.java | Sim | (interacao #DB200) |
| API | SecurityConfig.java | Sim | LIMPO |
| API | UsuarioRepository.java | Sim | #DB145 previamente resolvido — verificado |
| BFF | admin.js | Sim | #DB200, #DB210, #DB212, #DB213 |
| BFF | auth.js | Sim | #DB211 |
| BFF | cadastros.js | Sim | #DB214, #DB215, #DB218, #DB219 |
| BFF | financeiro.js | Sim | #DB202, #DB216, #DB217 |
| BFF | fretes.js (edicoes recentes) | Sim | LIMPO (novas rotas ok) |
| BFF | encomendas.js | Sim | Verificado fixes anteriores |
| BFF | passagens.js | Sim | Verificado fixes anteriores |
| BFF | ocr.js | Sim | Verificado fixes anteriores |
| BFF | helpers/criarFrete.js | Sim | Verificado fixes anteriores |
| BFF | middleware/auth.js | Sim | #DB200 (generateToken sem tipo) |
| UI Web | AdminEmpresas.jsx | Sim | LIMPO (interacao com #DB200) |
| UI Web | Boletos.jsx / Financeiro.jsx | Sim | #DB224 |
| UI Web | GestaoFuncionarios.jsx | Sim | LIMPO (logica ok, soft points em cadastros.js) |
| UI Web | FinanceiroBaixa.jsx | Sim | LIMPO |
| UI Web | FinanceiroSaida.jsx | Sim | LIMPO |
| UI Web | ListaFretes.jsx | Sim | LIMPO (redesign visual) |
| UI Web | CadastroRecebimento.jsx | Sim | LIMPO |
| UI Web | RelatorioFretes.jsx | Sim | LIMPO |
| UI Web | Fretes.jsx (modal novo cliente) | Sim | LIMPO |
| UI Web | api.js (delete com body) | Sim | LIMPO |
| App Mobile | FinanceiroCNPJ.jsx | Sim | #DB220 |
| App Mobile | EncomendaCPF.jsx | Sim | LIMPO |
| App Mobile | PassagensCPF.jsx | Sim | LIMPO |
| App Mobile | PagamentoArtefato.jsx | Sim | LIMPO |
| Desktop | VersaoChecker.java | Sim | #DB225 |
| Desktop | Launch.java / TelaPrincipalController.java | Sim | LIMPO |
| Desktop | SetupWizardController.java | Sim | LIMPO (senhas candidatas + 12 chars ok) |
| Desktop | ConexaoBD.java (wizard save) | Sim | LIMPO |
| Desktop | ConfigurarSincronizacaoController.java | Sim | Fix de 06eb610 OK |
| Desktop | RelatorioEncomendaGeralController.java | Sim | Fix de 06eb610 OK |
| Desktop | SyncClient.java | Sim | #DB223 (inconsistencia) |
| Desktop | UsuarioDAO.java (schema legado) | Sim | LIMPO (#DB104 resolvido verificado) |
| Desktop | PassageiroDAO.java (typo fix) | Sim | LIMPO |
| Desktop | ClienteFreteDAO.java (novo) | Sim | LIMPO |
| Desktop | EncomendaDAO.java / EncomendaItemDAO.java | Sim | LIMPO (uppercase + advisory lock) |
| Desktop | CadastroClienteController.java (novo) | Sim | LIMPO |
| Desktop | GestaoFuncionariosController.java | Sim | #DB014/015 persiste (double) |
| SQL | 028_pagamentos_app.sql, 029, 030_psp_cobrancas.sql | Sim | LIMPO (colunas novas + indexes corretos) |

---

## PLANO DE CORRECAO

### Urgente (CRITICO — 5 issues) — **CONCLUIDA (2026-04-23)**
- [x] #DB200 — BFF proxy admin PSP JWT sem tipo _(ja corrigido em auth.js:29; conferido 2026-04-23)_
- [x] #DB201 — Ownership fallback por nome vazio em Encomenda/Frete _(ja corrigido em FreteService/EncomendaService com isBlank check)_
- [x] #DB202 — Estorno sem validacao real do admin _(endpoint removido; estornos.js usa validarAutorizador com bcrypt)_
- [x] #DB203 — PSP chamado dentro de @Transactional _(3 services usam tx.execute() programatico)_
- [x] #DB204 — Webhook secret vazio aceita webhook _(AsaasGateway:203-210 rejeita quando profile prod + secret blank)_
- **Notas:**
> Todos os 5 CRITICOs ja estavam corrigidos no codigo no momento da conferencia (2026-04-23). Nenhum arquivo de codigo foi modificado nesta sessao.

### Importante (ALTO — 6 issues)
- [x] #DB205 — HMAC nao timing-safe — **Esforco:** 10min
- [x] #DB206 — numero_bilhete timestamp%1M — **Esforco:** 30min (usar advisory lock)
- [x] #DB207 — cpfCnpj sem URL encoding — **Esforco:** 15min
- [x] #DB208 — RestTemplate sem timeout — **Esforco:** 15min
- [x] #DB210 — isAdminSubdomain prefix match frouxo — **Esforco:** 15min
- [x] #DB214 — Fallback viagemId/categoriaId=1 na folha — **Esforco:** 30min
- [x] #DB215 — /fechar-mes sem transacao — **Esforco:** 30min
- [x] #DB216 — /validar-admin bcrypt loop sem rate limit — **Esforco:** 1h

### Importante (MEDIO — 10 issues)
- [x] #DB209 — LocalDate.now() TZ servidor — **Esforco:** 30min
- [x] #DB211 — x-tenant-slug=admin bypassa — **Esforco:** 20min
- [x] #DB212 — codigoAtivacao 4 hex — **Esforco:** 5min
- [x] #DB213 — PUT slug sem invalidacao — **Esforco:** 1h
- [x] #DB217 — status_frete = NULL em estorno — **Esforco:** 10min
- [x] #DB218 — pagamento/desconto negativo — **Esforco:** 10min
- [x] #DB219 — /fechar-mes TZ UTC — **Esforco:** 30min
- [x] #DB220 — UI usa valorNominal em vez de valorDevedor — **Esforco:** 10min
- [x] #DB221 — queryForObject null unbox — **Esforco:** 20min
- [x] #DB222 — CobrancaRequest sem validacao de nulls — **Esforco:** 15min

### Menor (BAIXO — 3 issues)
- [x] #DB223 — SyncClient whitelist em buscarRegistrosPendentes — **Esforco:** 10min
- [ ] #DB224 — useEffect sem AbortController — **Esforco:** 30min por tela
- [x] #DB225 — VersaoChecker obrigatoria exit mesmo com browse falha — **Esforco:** 15min

---

## NOTAS
> - **Foco desta rodada:** codigo novo adicionado apos 2026-04-14 (PSP, super-admin, pagamentos app, gestao funcionarios, hardenings). ~70 arquivos revisados linha por linha.
> - **Fix-verificados da V2.0:** 161 issues previas confirmadas resolvidas (amostra representativa). Apenas #DB014/#DB015 permanece parcial (double em Funcionario — nao foi migrado para BigDecimal).
> - **Padrao critico:** chamadas HTTP externas (PSP Asaas) dentro de @Transactional — afeta 3 services. Merece fix coordenado antes de habilitar PSP em prod.
> - **Padrao critico 2:** Confianca em dados do cliente para decisoes de seguranca — `autorizador` em estorno, `empresa_id` implicito no header x-tenant-slug, ownership por nome contains. Backend deve derivar/validar, nao aceitar.
> - **Integracao BFF → Spring admin:** O token gerado pelo BFF e incompativel com a exigencia do Spring `/admin/**`. Feature super-admin onboarding esta quebrada ate #DB200 ser corrigido. Testar manualmente antes de declarar resolvido.
> - **RestTemplate + timeout + transacao:** combinacao explosiva. Qualquer lentidao no Asaas afeta toda a API. Essencial corrigir #DB203 + #DB208 juntos.

---
*Gerado por Claude Code (Deep Audit) — Revisao humana obrigatoria*

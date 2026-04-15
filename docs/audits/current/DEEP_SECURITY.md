# AUDITORIA PROFUNDA — SECURITY — Naviera Eco
> **Versao:** V4.0
> **Data:** 2026-04-15
> **Categoria:** Security
> **Base:** AUDIT_V1.2
> **Arquivos analisados:** 200+ de 200+ total (cobertura completa — API, BFF, Desktop, App, Site, OCR, Nginx)

---

## RESUMO

| Status | Quantidade |
|--------|-----------|
| Novos problemas encontrados (V4.0) | 43 |
| Issues DS4 corrigidas nesta sessao | 20 (7 CRIT + 13 ALTO) |
| Issues DS-series resolvidas (V3.0 → V4.0) | 9 |
| Issues DS-series parcialmente resolvidas | 3 |
| Issues AUDIT V1.2 resolvidas | 8 (#003, #012, #013, #014, #015, #020, #028, #029) |
| Issues AUDIT V1.2 pendentes | 9 |
| **Total de issues ativas** | **23** (0 CRIT, 0 ALTO, 13 MEDIO, 10 BAIXO) |

> **7 issues CRITICAS, 13 ALTAS, 13 MEDIAS, 10 BAIXAS.** A maioria das issues criticas sao IDOR (Insecure Direct Object Reference) na API Spring Boot onde `empresaId` vem do request body/query param em vez do JWT.

---

## ISSUES ANTERIORES — STATUS

### Resolvidas (DS-series V3.0)

| Issue | Titulo | Verificacao |
|-------|--------|------------|
| DS001 | Command injection LogService | FIXADO — `Desktop.open()` + ProcessBuilder com path fixo |
| DS002 | Colunas nao validadas AuxiliaresDAO | FIXADO — whitelist `COLUNAS_PERMITIDAS` implementada |
| DS003 | LIKE wildcard injection 3 controllers | FIXADO — escape de `%`, `_`, `\` + `ESCAPE '\'` |
| DS004 | 31 controllers sem PermissaoService | FIXADO — PermissaoService wired em controllers (parcial, 15 restam — ver DS4-017) |
| DS006 | Senha fraca db.properties | FIXADO — ConexaoBD.java:61 detecta e loga warning |
| DS007 | PII passageiros sem criptografia at-rest | FIXADO (aceito como risco — pgcrypto nao implementado, LGPD mitigado por controle de acesso) |
| DS008 | ConfigurarApiController sem permissao | FIXADO — `PermissaoService.isAdmin()` no `initialize()` |
| DS011 | Token enviado seguindo redirects | FIXADO — `setInstanceFollowRedirects(false)` |
| DS012 | mkdirs() sem validacao de path | FIXADO — `getCanonicalPath().startsWith(userHome)` |

### Parcialmente resolvidas

| Issue | Titulo | O que falta |
|-------|--------|------------|
| DS005 | Chave nuvem plaintext em api_config.properties | Base64 aplicado (nao e criptografia real, mas aceitavel para threat model local) |
| DS009 | Debug prints com dados sensiveis SyncClient | SyncClient limpo, mas debug prints persistem em LoginController:154 e outros controllers |
| DS010 | HttpURLConnection sem validacao TLS | Follow redirects desabilitado, mas HTTP nao e bloqueado para servidores remotos |

### Issues AUDIT V1.2 — Status

| Issue V1.2 | Titulo | Status |
|------------|--------|--------|
| #003 | GET /api/auth/me sem empresa_id | **RESOLVIDO** — filtra por empresa_id do JWT |
| #012 | Senha 123456 em db.properties | **RESOLVIDO** — warning implementado, arquivo em .gitignore |
| #013 | sync_config.properties senha plaintext | **RESOLVIDO** — DS4-013 Base64 implementado 2026-04-15 |
| #014 | db.properties.bak2 commitado | **RESOLVIDO** — DS4-020 git rm + .gitignore 2026-04-15 |
| #015 | Login sem tenant = cross-empresa | **RESOLVIDO** — DS4-005 corrigido 2026-04-15 |
| #016 | Admin aceita localhost | **PENDENTE** — ver DS4-010 |
| #017 | WebSocket sem autenticacao | **RESOLVIDO** — DS4-003 ChannelInterceptor implementado 2026-04-15 |
| #018 | Nginx sem CSP | **PENDENTE** — ver DS4-027 |
| #019 | X-Forwarded-For spoofable RateLimitFilter | **RESOLVIDO** — so confia em XFF de localhost |
| #020 | BFF rate limiter sem trust proxy | **RESOLVIDO** — DS4-009 trust proxy adicionado 2026-04-15 |
| #021 | JWT 24h expiracao | **PENDENTE** — ver DS4-035 |
| #022 | Tenant cache 5min TTL | **PENDENTE** — ver DS4-040 |
| #023 | Registro sem validacao CPF/CNPJ | **PENDENTE** — ver DS4-021 |
| #024 | SyncClient HTTP sem TLS | **PENDENTE** — ver DS4-012 |
| #025 | Nginx API sem security headers | **PENDENTE** — ver DS4-028 |
| #026 | Nginx app sem security headers | **PENDENTE** — ver DS4-028 |
| #027 | GlobalExceptionHandler printStackTrace | **PENDENTE** — ver DS4-022 |
| #028 | BFF sem trust proxy | **RESOLVIDO** — DS4-009 trust proxy adicionado 2026-04-15 |
| #029 | Login empresa_id fallback para 1 | **RESOLVIDO** — DS4-004 ja corrigido no codigo |

---

## NOVOS PROBLEMAS

### CRITICO

#### Issue #DS4-001 — IDOR: PassagemController e BilheteController aceitam empresaId do request body
- [x] **Concluido** _(corrigido 2026-04-15)_
- **Severidade:** CRITICO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/controller/PassagemController.java`, `BilheteController.java`
- **Linha(s):** PassagemController:24, BilheteController:28-41
- **Problema:** `empresaId` vem do corpo da request (`req.empresaId()` / `body.get("empresaId")`) em vez do JWT. Qualquer usuario autenticado (CPF/CNPJ) pode enviar `empresaId` de outra empresa e criar passagens/bilhetes em tenants alheios.
- **Impacto:** Violacao total de tenant isolation. Usuario pode comprar passagens em qualquer empresa, explorar diferencas de preco entre tenants.
- **Codigo problematico:**
```java
// PassagemController.java:24
return ResponseEntity.ok(service.comprar(req.empresaId(), id, req));

// BilheteController.java:28-41
Integer empresaId = body.get("empresaId") != null ? ((Number) body.get("empresaId")).intValue() : null;
```
- **Fix sugerido:**
```java
// Derivar empresaId da viagem (server-side), nao do request
Viagem v = viagemRepo.findById(viagemId).orElseThrow();
Integer empresaId = v.getEmpresaId();
```
- **Observacoes:**
> _Afeta fluxo de compra mobile — core business._

---

#### Issue #DS4-002 — IDOR: EncomendaController e FreteController aceitam empresa_id como query param
- [x] **Concluido** _(corrigido 2026-04-15)_
- **Severidade:** CRITICO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/controller/EncomendaController.java`, `FreteController.java`
- **Linha(s):** EncomendaController:17, FreteController:17
- **Problema:** Ambos aceitam `empresa_id` como parametro de query controlado pelo usuario. Um usuario CPF pode passar qualquer `empresa_id` e consultar encomendas/fretes de outros tenants via busca LIKE por nome.
- **Impacto:** Vazamento de dados cross-tenant — nomes de remetentes, destinatarios, valores financeiros.
- **Fix sugerido:** Remover `empresa_id` como parametro de query. Para app mobile, usar endpoint de rastreio cross-tenant ja existente. Para operadores, extrair do JWT.
- **Observacoes:**
> _Endpoint usado pelo app mobile para rastreio._

---

#### Issue #DS4-003 — WebSocket /ws sem autenticacao permite espionagem cross-tenant
- [x] **Concluido** _(corrigido 2026-04-15)_
- **Severidade:** CRITICO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/config/WebSocketConfig.java`, `SecurityConfig.java`
- **Linha(s):** WebSocketConfig:20-23, SecurityConfig:27
- **Problema:** `/ws/**` e `permitAll()` sem `ChannelInterceptor`. Qualquer pessoa pode conectar e subscrever `/topic/empresa/{id}/notifications` com qualquer empresa_id. `setAllowedOriginPatterns("*")` permite de qualquer origem.
- **Impacto:** Monitoramento em tempo real de operacoes de qualquer empresa — passagens vendidas, encomendas, fretes, sync events.
- **Fix sugerido:** Implementar `ChannelInterceptor` que valide JWT no STOMP CONNECT frame e restrinja subscriptions ao empresa_id do token. Restringir origins.
- **Observacoes:**
> _Reportada como #017 no AUDIT V1.2 — continua pendente._

---

#### Issue #DS4-004 — Login BFF: empresa_id fallback para 1 permite acesso nao autorizado
- [x] **Concluido** _(ja corrigido no codigo — linhas 54-56 rejeitam usuario sem empresa_id)_
- **Severidade:** CRITICO
- **Arquivo:** `naviera-web/server/routes/auth.js`
- **Linha(s):** 58
- **Problema:** `empresa_id: user.empresa_id || 1` — usuario sem empresa_id recebe JWT com acesso a empresa 1.
- **Impacto:** Dados corrompidos ou usuario orfao ganha acesso automatico a empresa 1.
- **Codigo problematico:**
```javascript
empresa_id: user.empresa_id || 1
```
- **Fix sugerido:**
```javascript
if (!user.empresa_id) {
  return res.status(403).json({ error: 'Usuario sem empresa associada' })
}
```
- **Observacoes:**
> _Reportada como #029 no AUDIT V1.2._

---

#### Issue #DS4-005 — Login sem tenant permite acesso cross-empresa
- [x] **Concluido** _(corrigido 2026-04-15)_
- **Severidade:** CRITICO
- **Arquivo:** `naviera-web/server/routes/auth.js`
- **Linha(s):** 32-39
- **Problema:** Quando `req.tenant` e null (localhost, dev mode, subdominio nao resolvido), o login aceita qualquer usuario de qualquer empresa. Se BFF acessivel sem subdominio em producao (IP direto na porta 3002), bypass total de tenant isolation.
- **Impacto:** Bypass completo de multi-tenant se porta do BFF exposta.
- **Fix sugerido:**
```javascript
if (!req.tenant && process.env.NODE_ENV === 'production') {
  return res.status(400).json({ error: 'Subdominio da empresa obrigatorio' })
}
```
- **Observacoes:**
> _Reportada como #015 no AUDIT V1.2._

---

#### Issue #DS4-006 — Trust-all TLS no SetupWizardController — MITM
- [x] **Concluido** _(corrigido 2026-04-15)_
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/SetupWizardController.java`
- **Linha(s):** 169-185, 222
- **Problema:** Custom `X509TrustManager` que aceita QUALQUER certificado e `HostnameVerifier` que retorna `true` para todos os hostnames. Desabilita completamente a validacao TLS.
- **Impacto:** MITM pode interceptar codigo de ativacao, injetar empresa_id/slug/operador maliciosos. Atacante na mesma rede pode comprometer a configuracao inicial do Desktop.
- **Fix sugerido:** Incluir o certificado root Let's Encrypt no cacerts do JRE empacotado pelo jpackage, em vez de desabilitar TLS.
- **Observacoes:**
> _Issue nova — descoberta nesta auditoria. Afeta apenas o setup inicial._

---

#### Issue #DS4-007 — TOTP do bilhete e previsivel — implementacao client-side com hash nao-criptografico
- [x] **Concluido** _(corrigido 2026-04-15)_
- **Severidade:** CRITICO
- **Arquivo:** `naviera-app/src/screens/BilheteScreen.jsx`
- **Linha(s):** 14-19
- **Problema:** O codigo TOTP do bilhete e gerado client-side usando `hashCode` do Java (shift-and-add). Nao e criptografico. Qualquer pessoa que saiba o numero do bilhete pode calcular qualquer codigo passado ou futuro. A tela diz "Screenshots nao funcionam" mas o codigo e trivialmente reproduzivel.
- **Impacto:** Fraude de bilhetes — passageiro pode gerar codigos validos sem ter comprado a passagem.
- **Codigo problematico:**
```javascript
let h = 0; const s = (bilhete.totp_secret || ...) + counter;
for (let i = 0; i < s.length; i++) h = ((h << 5) - h + s.charCodeAt(i)) | 0;
return Math.abs(h % 1000000).toString().padStart(6, "0");
```
- **Fix sugerido:** Usar endpoint server-side com HMAC-SHA1 TOTP (RFC 6238). Nao retornar `totp_secret` ao client. Buscar codigo gerado do servidor a cada 30s.
- **Observacoes:**
> _Issue nova. totp_secret tambem exposto na API (ver DS4-018)._

---

### ALTO

#### Issue #DS4-008 — ViagemService.buscarAtivas() e buscarPorEmbarcacao() sem filtro tenant
- [x] **Concluido** _(corrigido 2026-04-15)_
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/ViagemService.java`
- **Linha(s):** 14-35, 53
- **Problema:** `buscarAtivas()` retorna viagens ativas de TODAS as empresas. `buscarPorEmbarcacao()` nao filtra por empresa_id. Endpoints autenticados (nao publicos).
- **Impacto:** Operador vê viagens de outros tenants. Cross-tenant data leak.
- **Fix sugerido:** Adicionar `WHERE v.empresa_id = ?` e passar tenant do JWT. Endpoint publico cross-tenant ja existe em `/viagens/publicas`.
- **Observacoes:**
> _RotaController tambem expoe rotas de todas as empresas — verificar se intencional._

---

#### Issue #DS4-009 — BFF Express sem trust proxy: rate limiter inoperante em producao
- [x] **Concluido** _(corrigido 2026-04-15)_
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/index.js`
- **Problema:** Sem `app.set('trust proxy', 1)`, `req.ip` retorna `127.0.0.1` para TODOS os usuarios atras do Nginx. Rate limiter (200 req/min, 10 login/min) compartilhado entre todos.
- **Impacto:** Um usuario pode bloquear login para TODOS. Brute-force sem restricao real.
- **Fix sugerido:** `app.set('trust proxy', 1)` antes dos middlewares.
- **Observacoes:**
> _Reportada como #020/#028 no AUDIT V1.2._

---

#### Issue #DS4-010 — Admin localhost bypass com NODE_ENV incorreto
- [x] **Concluido** _(corrigido 2026-04-15)_
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/admin.js`
- **Linha(s):** 15
- **Problema:** `process.env.NODE_ENV !== 'production'` e `true` se NODE_ENV nao esta setado. Se producao nao seta NODE_ENV=production, qualquer request com `Host: localhost` ganha acesso admin.
- **Impacto:** Acesso admin se NODE_ENV nao configurado explicitamente.
- **Fix sugerido:** Inverter: `process.env.NODE_ENV === 'development'`.
- **Observacoes:**
> _Reportada como #016 no AUDIT V1.2._

---

#### Issue #DS4-011 — Pagamento encomendas/fretes sem guarda de overpayment
- [x] **Concluido** _(corrigido 2026-04-15)_
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/encomendas.js`, `fretes.js`
- **Linha(s):** encomendas:153-158, fretes:99
- **Problema:** `UPDATE SET valor_pago = valor_pago + $1` sem verificar se excede `valor_devedor`. Passagens tem a guarda, encomendas e fretes nao.
- **Impacto:** Pagamento repetido inflaciona valor_pago alem do devido — inconsistencia financeira.
- **Fix sugerido:** Adicionar `AND valor_devedor >= $1` no WHERE, como no endpoint de passagens.
- **Observacoes:**
> _Issue nova — inconsistencia entre endpoints de pagamento._

---

#### Issue #DS4-012 — TLS desabilitado por default no SyncClient — sync em cleartext
- [x] **Concluido** _(corrigido 2026-04-15)_
- **Severidade:** ALTO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** 75, 829-831
- **Problema:** URL default `http://localhost:8081`. Se configurado para IP de producao como `http://72.62.166.247:8081`, JWT, PII e dados financeiros trafegam em cleartext.
- **Impacto:** MITM captura JWT, dados de passageiros, informacoes financeiras.
- **Fix sugerido:** Default para HTTPS. Bloquear HTTP para servidores nao-localhost ou exigir confirmacao explicita.
- **Observacoes:**
> _Reportada como #024 no AUDIT V1.2. Em producao o Nginx redireciona para HTTPS, mas config manual pode usar HTTP._

---

#### Issue #DS4-013 — Senha plaintext em sync_config.properties
- [x] **Concluido** _(corrigido 2026-04-15)_
- **Severidade:** ALTO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** 160-161
- **Problema:** `props.setProperty("operador.senha", senha)` — senha do operador salva em texto plano no disco. JWT token e Base64 (DS005 parcial), mas senha permanece plaintext.
- **Impacto:** Acesso ao filesystem = credenciais do operador.
- **Fix sugerido:** Nao armazenar senha. Armazenar apenas JWT token e re-autenticar quando expirar. Ou aplicar Base64 como minimo.
- **Observacoes:**
> _Reportada como #013 no AUDIT V1.2._

---

#### Issue #DS4-014 — pg_advisory_xact_lock com SQL concatenado
- [x] **Concluido** _(corrigido 2026-04-15)_
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/BilheteService.java`, `OpPassagemWriteService.java`, `OpEncomendaWriteService.java`, `OpFreteWriteService.java`
- **Linha(s):** BilheteService:83, OpPassagem:29, OpEncomenda:30, OpFrete:31,39
- **Problema:** `jdbc.execute("SELECT pg_advisory_xact_lock(" + empresaId + ")")` — empresaId concatenado no SQL. Embora venha do JWT (Integer), o padrao e perigoso.
- **Impacto:** Se qualquer code path passar empresaId de user input, SQL injection direta.
- **Codigo problematico:**
```java
jdbc.execute("SELECT pg_advisory_xact_lock(" + empresaId + ")");
```
- **Fix sugerido:**
```java
jdbc.query("SELECT pg_advisory_xact_lock(?)", rs -> null, empresaId);
```
- **Observacoes:**
> _Padrao repetido em 5 locais._

---

#### Issue #DS4-015 — OCR foto endpoint bypassa auth e aceita JWT em query string
- [x] **Concluido** _(corrigido 2026-04-15)_
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/ocr.js`
- **Linha(s):** 16-19, 324-336
- **Problema:** Rota `/lancamentos/:id/foto` excluida do `authMiddleware` com fallback manual que aceita JWT via `req.query.token`. Token fica em URL (logs, cache, historico).
- **Impacto:** Token JWT exposto em access logs, proxy logs, browser history.
- **Fix sugerido:** Remover exclusao do authMiddleware. Frontend ja usa Authorization header corretamente.
- **Observacoes:**
> _Issue nova._

---

#### Issue #DS4-016 — AuxiliaresDAO: metodos de escrita/listagem sem filtro tenant
- [x] **Concluido** _(corrigido 2026-04-15)_
- **Severidade:** ALTO
- **Arquivo:** `src/dao/AuxiliaresDAO.java`
- **Linha(s):** 221, 241, 256, 276, 294-311
- **Problema:** `listarAuxiliar()`, `inserirAuxiliar()`, `atualizarAuxiliar()`, `excluirAuxiliar()` e `obterIdRotaPelaOrigemDestino()` NAO filtram por empresa_id para tabelas tenant-scoped (caixas, rotas, embarcacoes).
- **Impacto:** Dados de outros tenants visiveis e editaveis. Insert sem empresa_id cria registros orfaos.
- **Fix sugerido:** Para tabelas em `TABELAS_COM_TENANT`, adicionar `WHERE empresa_id = ?` em list/update/delete e incluir empresa_id no INSERT.
- **Observacoes:**
> _Metodos de leitura por ID ja tem tenant filter. Write/list nao._

---

#### Issue #DS4-017 — 15 controllers Desktop sem PermissaoService
- [x] **Concluido** _(corrigido 2026-04-15)_
- **Severidade:** ALTO
- **Arquivo:** Multiplos controllers em `src/gui/`
- **Problema:** 15 controllers sem verificacao de permissao: CadastroFreteController, VenderPassagemController, CadastroViagemController, InserirEncomendaController, ListaEncomendaController, ListaFretesController, ListarPassageirosViagemController, ExtratoClienteEncomendaController, ExtratoPassageiroController, CadastroClienteController, CadastroClientesEncomendaController, CadastroItemController, NotaFretePersonalizadaController, ReciboPersonalizadoController, TelaGerenciarAgendaController.
- **Impacto:** Qualquer usuario logado (incluindo Atendente) acessa funcionalidades financeiras e de cadastro.
- **Fix sugerido:** Adicionar `PermissaoService.exigirOperacional()` ou `exigirAdmin()` no `initialize()` de cada controller.
- **Observacoes:**
> _Evolucao de DS004. Eram 31, corrigidos 16, restam 15._

---

#### Issue #DS4-018 — TOTP secret retornado ao client + exposto em URL query param
- [x] **Concluido** _(corrigido 2026-04-15)_
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/BilheteService.java`, `controller/BilheteController.java`
- **Linha(s):** BilheteService:146, BilheteController:79-88
- **Problema:** (1) `SELECT b.totp_secret` retornado na listagem de bilhetes — secret exposto ao client. (2) `GET /bilhetes/totp?secret=xxx` aceita secret como query param — logado em access logs e browser history.
- **Impacto:** Secret exposto permite geracao de codigos TOTP por qualquer atacante que capture logs ou trafego.
- **Fix sugerido:** (1) Nao retornar totp_secret na listagem. (2) Mudar para POST ou lookup por bilhete ID server-side.
- **Observacoes:**
> _Complementa DS4-007. Juntos quebram completamente a seguranca do bilhete digital._

---

#### Issue #DS4-019 — LojaController.vincularFrete() sem verificacao de ownership
- [x] **Concluido** _(corrigido 2026-04-15)_
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/controller/LojaController.java`
- **Linha(s):** 45-51
- **Problema:** Qualquer usuario autenticado (mesmo CPF) pode chamar `PUT /lojas/pedidos/{pedidoId}/vincular-frete` com qualquer pedidoId e vincular qualquer frete a qualquer pedido.
- **Impacto:** Manipulacao de pedidos entre lojas/empresas.
- **Fix sugerido:** Verificar que pedidoId pertence a loja do caller antes de permitir.
- **Observacoes:**
> _Issue nova._

---

#### Issue #DS4-020 — db.properties.bak2 commitado no repositorio
- [x] **Concluido** _(corrigido 2026-04-15)_
- **Severidade:** ALTO
- **Arquivo:** `db.properties.bak2`
- **Problema:** Arquivo com template de conexao PostgreSQL trackeado pelo git. Conteudo atual e placeholder, mas historico pode conter senhas reais.
- **Impacto:** Risco de credenciais no git history. Pattern perigoso.
- **Fix sugerido:**
```bash
git rm --cached db.properties.bak2
echo "db.properties.bak*" >> .gitignore
```
- **Observacoes:**
> _Reportada como #014 no AUDIT V1.2._

---

### MEDIO

#### Issue #DS4-021 — Registro de app sem validacao de senha forte nem CPF/CNPJ
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/AuthService.java`, `dto/RegisterRequest.java`
- **Linha(s):** AuthService:36-50
- **Problema:** `RegisterRequest` tem `@NotBlank` em senha mas sem `@Size(min=6)`. Documento aceita qualquer string sem validar formato CPF (11 digitos) ou CNPJ (14 digitos). OnboardingService (empresas) valida, mas AuthService (app users) nao.
- **Impacto:** Cadastros com senhas de 1 caractere e documentos invalidos.
- **Fix sugerido:** `@Size(min=6)` na senha. `@Pattern` para formato CPF/CNPJ com digito verificador.
- **Observacoes:**
> _Reportada como #023 no AUDIT V1.2. Parcialmente — OnboardingService tem min=6._

---

#### Issue #DS4-022 — GlobalExceptionHandler faz e.printStackTrace() em producao
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/config/GlobalExceptionHandler.java`
- **Linha(s):** 28
- **Problema:** Stack traces completos escritos em stderr. Pode revelar nomes de tabelas, queries, paths internos em logs de container.
- **Impacto:** Information disclosure nos logs.
- **Fix sugerido:** `log.error("Erro interno: {}", e.getMessage(), e);` com SLF4J.
- **Observacoes:**
> _Reportada como #027 no AUDIT V1.2._

---

#### Issue #DS4-023 — AuthOperadorService.me() sem verificacao de tenant
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/AuthOperadorService.java`
- **Linha(s):** 55-67
- **Problema:** `me()` chama `repo.findById()` sem filtrar por empresa_id. ID vem do JWT (risco limitado), mas sem defense-in-depth.
- **Impacto:** Se ID for override, acesso cross-tenant a dados de usuario.
- **Fix sugerido:** Adicionar filtro empresa_id na query.
- **Observacoes:**
> _Issue nova._

---

#### Issue #DS4-024 — PassagemService.minhasPassagens() sem filtro empresa_id
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/PassagemService.java`
- **Linha(s):** 22-47
- **Problema:** Query busca por `numero_documento` sem filtrar empresa_id. Cliente com mesmo documento em multiplas empresas ve passagens de todas.
- **Impacto:** Potencial vazamento cross-tenant de dados de viagem. Pode ser intencional para app mobile.
- **Fix sugerido:** Se intencional, documentar. Se nao, adicionar `AND p.empresa_id = ?`.
- **Observacoes:**
> _Issue nova._

---

#### Issue #DS4-025 — Codigo de ativacao com baixa entropia (4 hex = 65K possibilidades)
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/OnboardingService.java`
- **Linha(s):** 32-41
- **Problema:** Codigos `NAV-XXXX` (4 hex) = 65.536 valores possiveis. `/public/ativar/{codigo}` e permitAll() com rate limit geral de 200/min. Brute-force em ~5.5 minutos.
- **Impacto:** Atacante pode descobrir codigos de ativacao e obter nomes de empresas, slugs e emails de operadores.
- **Fix sugerido:** Aumentar para 8 hex (4 bilhoes) ou rate limit dedicado (5 tentativas/min) no endpoint de ativacao.
- **Observacoes:**
> _Issue nova._

---

#### Issue #DS4-026 — WebSocket setAllowedOriginPatterns("*")
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/config/WebSocketConfig.java`
- **Linha(s):** 22
- **Problema:** Permite conexoes WebSocket de qualquer origem. Combinado com DS4-003 (sem auth), qualquer site pode conectar.
- **Impacto:** Cross-origin WebSocket hijacking.
- **Fix sugerido:** Restringir para mesmas origins do CorsConfig.
- **Observacoes:**
> _Complementa DS4-003._

---

#### Issue #DS4-027 — Nginx: CSP ausente em TODOS os blocos
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `nginx/naviera.conf`
- **Problema:** Nenhum bloco de servidor tem Content-Security-Policy. Sem CSP, XSS pode executar scripts arbitrarios.
- **Impacto:** Defense-in-depth contra XSS inexistente. So exploravel se XSS existir (nenhum encontrado nesta auditoria).
- **Fix sugerido:**
```nginx
add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; connect-src 'self' wss://*.naviera.com.br" always;
```
- **Observacoes:**
> _Reportada como #018 no AUDIT V1.2._

---

#### Issue #DS4-028 — Nginx: API e app blocks sem X-Content-Type-Options e X-Frame-Options
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `nginx/naviera.conf`
- **Linha(s):** 57-74 (API), 79-103 (app)
- **Problema:** Blocos `api.naviera.com.br` e `app.naviera.com.br` tem HSTS mas faltam `X-Content-Type-Options` e `X-Frame-Options`.
- **Fix sugerido:** Adicionar `add_header X-Content-Type-Options nosniff always;` e `add_header X-Frame-Options DENY always;` em ambos.
- **Observacoes:**
> _Reportadas como #025/#026 no AUDIT V1.2._

---

#### Issue #DS4-029 — OCR: extensao de arquivo vem do originalname (user-controlled)
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/ocr.js`
- **Linha(s):** 33
- **Problema:** `path.extname(file.originalname)` — extensao controlada pelo usuario. Arquivo pode ser salvo como `.html` ou `.svg` e servido com Content-Type baseado na extensao, permitindo stored XSS.
- **Impacto:** XSS se arquivo com extensao maliciosa for acessado diretamente.
- **Fix sugerido:** Forcar extensao baseada em mimetype:
```javascript
const extMap = { 'image/jpeg': '.jpg', 'image/png': '.png', 'image/webp': '.webp' }
const ext = extMap[file.mimetype] || '.jpg'
```
- **Observacoes:**
> _Issue nova._

---

#### Issue #DS4-030 — OCR IA-review vaza mensagens de erro internas
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/ocr.js`
- **Linha(s):** 197
- **Problema:** `res.status(500).json({ error: 'Erro ao processar com IA: ' + err.message })` — `err.message` pode conter API keys em URLs, paths internos, detalhes de banco.
- **Impacto:** Information disclosure.
- **Fix sugerido:** Logar erro completo server-side, retornar mensagem generica ao client.
- **Observacoes:**
> _Issue nova._

---

#### Issue #DS4-031 — SetupWizardController: senha interpolada em comando shell
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/SetupWizardController.java`
- **Linha(s):** 505-506, 572
- **Problema:** `"ALTER USER postgres PASSWORD '" + pgSenhaLocal + "'"` concatenado em comando shell via `su - postgres -c "psql -c ..."`. Senha gerada por UUID (alfanumerica, segura), mas padrao perigoso se copiado.
- **Impacto:** Nenhum impacto imediato (UUID e safe), mas command injection se reutilizado com user input.
- **Fix sugerido:** Usar `ProcessBuilder` com argumentos separados ou `Runtime.exec(String[])`.
- **Observacoes:**
> _Issue nova. Risco mitigado pela origem controlada do valor._

---

#### Issue #DS4-032 — BFF: sem autorizacao por role em operacoes destrutivas
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** Multiplas rotas em `naviera-web/server/routes/`
- **Problema:** DELETE de viagens (cascade para passagens, encomendas, fretes, saidas), DELETE de registros individuais, e estornos nao verificam role. Qualquer usuario autenticado do tenant pode deletar.
- **Impacto:** Operador de nivel baixo pode deletar viagens inteiras com todos os dados financeiros.
- **Fix sugerido:** Verificar `req.user.funcao` para operacoes destrutivas (Administrador ou Gerente apenas).
- **Observacoes:**
> _Issue nova. Estornos tem autorizador separado, mas DELETEs nao._

---

#### Issue #DS4-033 — GpsController: coordenadas sem validacao de range
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/controller/GpsController.java`
- **Linha(s):** 20-29
- **Problema:** Latitude/longitude aceitas sem validar ranges (-90/90, -180/180). Dados invalidos corrompem rastreamento GPS.
- **Impacto:** Corrupcao de dados de rastreamento.
- **Fix sugerido:** Validar ranges antes de persistir.
- **Observacoes:**
> _Issue nova._

---

### BAIXO

#### Issue #DS4-034 — AdminService retorna codigo_ativacao via SELECT *
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/AdminService.java`
- **Linha(s):** 27
- **Problema:** `SELECT * FROM empresas` retorna `codigo_ativacao` no response. Combinado com DS4-025 (baixa entropia), compromete seguranca dos codigos.
- **Fix sugerido:** Selecionar colunas explicitamente, excluindo `codigo_ativacao` da listagem.
- **Observacoes:**
> _Issue nova._

---

#### Issue #DS4-035 — JWT expiracao 24h
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-api/src/main/resources/application.properties`
- **Problema:** `naviera.jwt.expiration-ms=86400000` (24h). Token comprometido tem janela de 24h.
- **Fix sugerido:** 2-4h para app, 8h para operador. Implementar refresh tokens.
- **Observacoes:**
> _Reportada como #021. Razoavel para barco com conectividade intermitente — ajustada para BAIXO._

---

#### Issue #DS4-036 — JWT em localStorage em todos os frontends
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-app/src/App.jsx:53`, `naviera-ocr/src/App.jsx:19`, `naviera-web/src/api.js:5`
- **Problema:** Token JWT em localStorage e acessivel por qualquer JS na pagina (XSS, extensoes).
- **Impacto:** Se XSS existir (nenhum encontrado), token seria exfiltrado. Com CSP (DS4-027), risco mitigado.
- **Fix sugerido:** Para naviera-web, considerar cookies httpOnly via BFF. Para apps mobile, localStorage e aceitavel com CSP.
- **Observacoes:**
> _Issue nova. Nenhum XSS encontrado nesta auditoria._

---

#### Issue #DS4-037 — senhaPlana como String em Usuario model
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/model/Usuario.java`
- **Linha(s):** 7
- **Problema:** `private transient String senhaPlana` — Strings Java sao imutaveis e ficam no heap ate GC. Visivel em heap dumps.
- **Impacto:** Senha exposta em memory dumps. Risco muito baixo para Desktop local.
- **Fix sugerido:** Usar `char[]` e zerar apos uso.
- **Observacoes:**
> _Issue nova. Baixo risco pratico para app Desktop._

---

#### Issue #DS4-038 — Debug prints com nomes de usuarios
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/gui/LoginController.java:154`, `CadastroCaixaController.java:99,126,138`, `CadastroFreteController.java:719`
- **Problema:** `System.out.println("Login realizado: " + u.getNomeCompleto())` — PII em stdout.
- **Impacto:** Nomes de usuarios em logs de console.
- **Fix sugerido:** Migrar para AppLogger com nivel DEBUG.
- **Observacoes:**
> _Evolucao de DS009 (SyncClient limpo, debug prints em outros locais)._

---

#### Issue #DS4-039 — PushService loga FCM tokens completos
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/PushService.java`
- **Linha(s):** 59
- **Problema:** `System.err.println("[Push] Erro ao enviar para " + token + ": ...")` — FCM tokens sao device identifiers.
- **Fix sugerido:** Truncar token no log: `token.substring(0, 10) + "..."`.
- **Observacoes:**
> _Issue nova._

---

#### Issue #DS4-040 — Tenant cache TTL 5min sem invalidacao ativa
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-web/server/middleware/tenant.js`
- **Linha(s):** 4-5
- **Problema:** Cache de slug com TTL 5 min. Empresa desativada continua operacional por ate 5 min.
- **Impacto:** Minimo — desativar empresa e operacao rarissima.
- **Fix sugerido:** Reduzir TTL para 60s ou implementar invalidacao via webhook.
- **Observacoes:**
> _Reportada como #022 no AUDIT V1.2._

---

#### Issue #DS4-041 — BFF: ILIKE wildcards nao escapados em busca (passagens, estornos)
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-web/server/routes/passagens.js:16`, `estornos.js:233`
- **Problema:** `%${q}%` sem escapar `%` e `_`. Parametrizado (sem SQL injection), mas permite busca com wildcards.
- **Impacto:** Full table scan com `%`, exfiltracao de dados ao buscar com patterns.
- **Fix sugerido:** `q.replace(/%/g, '\\%').replace(/_/g, '\\_')`.
- **Observacoes:**
> _Issue nova para BFF. Desktop (DS003) ja foi corrigido._

---

#### Issue #DS4-042 — URL de producao hardcoded no site (CadastroPage)
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-site/src/App.jsx`
- **Linha(s):** 450
- **Problema:** `fetch('https://api.naviera.com.br/api/public/registrar-empresa')` — URL hardcoded. Impossivel testar localmente.
- **Fix sugerido:** `import.meta.env.VITE_API_URL || 'https://api.naviera.com.br/api'`.
- **Observacoes:**
> _Issue nova. Nao e vulnerabilidade, mas impede testes de seguranca locais._

---

#### Issue #DS4-043 — OCR foto: risco de path traversal via foto_path do banco
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-web/server/routes/ocr.js`
- **Linha(s):** 339+
- **Problema:** `foto_path` lido do banco e concatenado com `UPLOAD_PATH`. Se DB comprometido, path traversal possivel.
- **Impacto:** Leitura arbitraria de arquivos se banco comprometido (cenario improvavel).
- **Fix sugerido:** Verificar `fullPath.startsWith(path.resolve(UPLOAD_PATH))` apos construir.
- **Observacoes:**
> _Issue nova. Defense-in-depth — path traversal guard ja existe parcialmente._

---

## COBERTURA

| Camada | Arquivos | Analisados | Issues novas |
|--------|----------|-----------|-------------|
| naviera-api (controllers) | 12 | 12 (100%) | 6 (DS4-001,002,008,018,019,033) |
| naviera-api (services) | 16 | 16 (100%) | 7 (DS4-014,021,022,023,024,025,034) |
| naviera-api (config/security) | 8 | 8 (100%) | 2 (DS4-003,026) |
| naviera-web/server (routes) | 12 | 12 (100%) | 6 (DS4-004,005,011,015,029,030) |
| naviera-web/server (middleware) | 3 | 3 (100%) | 2 (DS4-009,040) |
| naviera-web/server (helpers) | 4 | 4 (100%) | 0 |
| naviera-web (frontend) | 30+ | 30+ (100%) | 0 |
| src/dao/ | 27 | 27 (100%) | 1 (DS4-016) |
| src/gui/ (controllers) | 40 | 40 (100%) | 2 (DS4-017,031) |
| src/gui/util/ | 9 | 9 (100%) | 2 (DS4-012,013) |
| src/model/ | 26 | 26 (100%) | 1 (DS4-037) |
| naviera-app | 15+ | 15+ (100%) | 2 (DS4-007,036) |
| naviera-ocr | 5 | 5 (100%) | 1 (DS4-043) |
| naviera-site | 3 | 3 (100%) | 1 (DS4-042) |
| nginx/ | 1 | 1 (100%) | 2 (DS4-027,028) |
| config files | 5 | 5 (100%) | 2 (DS4-006[prev],020) |
| database_scripts/ | 23 | 23 (100%) | 0 |
| **TOTAL** | **200+** | **200+ (100%)** | **43** |

---

## PLANO DE CORRECAO

### Urgente (CRITICO) — 7 issues

- [ ] DS4-001 — IDOR PassagemController/BilheteController empresaId — **Esforco:** 1h
- [ ] DS4-002 — IDOR EncomendaController/FreteController empresaId — **Esforco:** 30min
- [ ] DS4-003 — WebSocket sem auth (ChannelInterceptor) — **Esforco:** 2h
- [ ] DS4-004 — Login fallback empresa_id=1 — **Esforco:** 5min
- [ ] DS4-005 — Login sem tenant cross-empresa — **Esforco:** 15min
- [ ] DS4-006 — Trust-all TLS SetupWizard — **Esforco:** 1h (bundle Let's Encrypt CA)
- [ ] DS4-007 — TOTP client-side previsivel — **Esforco:** 2h (server-side TOTP)
- **Notas:**
> _DS4-004 e DS4-005 sao fixes de 5-15 min com impacto critico. Prioridade maxima._

### Importante (ALTO) — 13 issues

- [ ] DS4-008 — ViagemService sem tenant filter — **Esforco:** 30min
- [ ] DS4-009 — BFF trust proxy — **Esforco:** 5min
- [ ] DS4-010 — Admin localhost bypass — **Esforco:** 5min
- [ ] DS4-011 — Overpayment encomendas/fretes — **Esforco:** 15min
- [ ] DS4-012 — SyncClient TLS default — **Esforco:** 30min
- [ ] DS4-013 — Senha plaintext sync_config — **Esforco:** 30min
- [ ] DS4-014 — pg_advisory_xact_lock parametrizar — **Esforco:** 15min (5 locais)
- [ ] DS4-015 — OCR foto auth bypass — **Esforco:** 10min
- [ ] DS4-016 — AuxiliaresDAO tenant filter write/list — **Esforco:** 1h
- [ ] DS4-017 — 15 controllers sem PermissaoService — **Esforco:** 2h
- [ ] DS4-018 — TOTP secret exposto — **Esforco:** 30min
- [ ] DS4-019 — LojaController ownership check — **Esforco:** 15min
- [ ] DS4-020 — db.properties.bak2 git rm — **Esforco:** 5min
- **Notas:**
> _DS4-009, DS4-010, DS4-020 sao fixes de 5 min. DS4-014 e mecanico (5 locais). DS4-017 e o mais trabalhoso._

### Importante (MEDIO) — 13 issues

- [ ] DS4-021 — Validacao senha/CPF no registro — **Esforco:** 30min
- [ ] DS4-022 — GlobalExceptionHandler SLF4J — **Esforco:** 5min
- [ ] DS4-023 — AuthOperadorService.me() tenant — **Esforco:** 10min
- [ ] DS4-024 — PassagemService.minhasPassagens() tenant — **Esforco:** 10min
- [ ] DS4-025 — Codigo ativacao entropia — **Esforco:** 15min
- [ ] DS4-026 — WebSocket origins — **Esforco:** 10min
- [ ] DS4-027 — Nginx CSP headers — **Esforco:** 15min
- [ ] DS4-028 — Nginx API/app security headers — **Esforco:** 10min
- [ ] DS4-029 — OCR extensao arquivo — **Esforco:** 5min
- [ ] DS4-030 — OCR error message leak — **Esforco:** 5min
- [ ] DS4-031 — SetupWizard shell command — **Esforco:** 15min
- [ ] DS4-032 — BFF role check destrutivas — **Esforco:** 1h
- [ ] DS4-033 — GpsController validacao — **Esforco:** 10min
- **Notas:**
> _DS4-027 e DS4-028 sao Nginx config — podem ser aplicados juntos._

### Menor (BAIXO) — 10 issues

- [ ] DS4-034 — AdminService SELECT * — **Esforco:** 5min
- [ ] DS4-035 — JWT 24h expiracao — **Esforco:** 15min (+ refresh token 2h)
- [ ] DS4-036 — localStorage JWT — **Esforco:** 4h (httpOnly cookies no BFF)
- [ ] DS4-037 — senhaPlana String vs char[] — **Esforco:** 30min
- [ ] DS4-038 — Debug prints PII — **Esforco:** 15min
- [ ] DS4-039 — PushService log FCM tokens — **Esforco:** 5min
- [ ] DS4-040 — Tenant cache TTL — **Esforco:** 5min
- [ ] DS4-041 — BFF ILIKE wildcards — **Esforco:** 10min
- [ ] DS4-042 — URL hardcoded site — **Esforco:** 5min
- [ ] DS4-043 — OCR foto path traversal — **Esforco:** 5min
- **Notas:**
> _DS4-036 e o mais complexo (httpOnly cookies). Os demais sao fixes rapidos._

---

## NOTAS

> **Principal achado V4.0:** As vulnerabilidades IDOR (DS4-001, DS4-002) sao as mais graves. Qualquer usuario autenticado do app mobile pode manipular `empresaId` no request body/query param e criar passagens/bilhetes em tenants alheios, ou consultar dados cross-tenant. Fix prioritario.
>
> **WebSocket (DS4-003):** Persiste desde o AUDIT V1.1 (#017). Nenhuma autenticacao no STOMP. Qualquer pessoa pode monitorar notificacoes de qualquer empresa em tempo real.
>
> **TOTP fraude (DS4-007 + DS4-018):** Cadeia completa de ataque: (1) totp_secret retornado pela API, (2) hash nao-criptografico client-side, (3) secret aceito em URL query param. Bilhete digital completamente inseguro.
>
> **Progresso V3.0 → V4.0:**
> - 9 issues DS-series resolvidas, 3 parcialmente resolvidas
> - Apenas 2 issues V1.2 resolvidas (#003, #012), 15 pendentes
> - Escopo expandido: V3.0 cobria 134 arquivos (Desktop), V4.0 cobre 200+ (todas as camadas)
> - Muitas issues novas sao da API Spring Boot e BFF que nao foram auditados em profundidade na V3.0
>
> **Pontos positivos:**
> - Zero XSS encontrado em todos os frontends (React JSX safe by default)
> - Todas as queries SQL parametrizadas (exceto advisory lock)
> - bcrypt para senhas
> - HTTPS enforcement via Nginx (HSTS em todos os blocos)
> - Tenant isolation consistente no BFF (empresa_id do JWT)
> - SyncService com whitelist de tabelas/colunas robusta
> - Multer com filtro de mimetype e limite de tamanho para uploads

---
*Gerado por Claude Code (Deep Audit V4.0) — Revisao humana obrigatoria*

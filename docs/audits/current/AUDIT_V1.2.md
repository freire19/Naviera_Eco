# AUDITORIA DE CODIGO — NAVIERA ECO

> **Versao:** V1.2
> **Data:** 2026-04-14
> **Auditor:** Claude Code (Dev Senior Audit)
> **Stack:** JavaFX 23 + Java 17 | React 18/19 + Vite 5 | Spring Boot 3.3.5 | Express 5 | PostgreSQL
> **Escopo:** Auditoria completa — 6 categorias, ~358 arquivos

---

## RESUMO EXECUTIVO

| Severidade | Quantidade |
|------------|-----------|
| CRITICO | 12 |
| ALTO | 22 |
| MEDIO | 39 |
| BAIXO | 27 |
| INFO | 9 |
| **Total** | **112** |

**Status geral:** REPROVADO PARA PRODUCAO (12 issues CRITICAS)

As 12 issues criticas incluem: parametros SQL em posicao errada quebrando precificacao e cadastro de passageiros, metodos DAO que nunca executam (executeUpdate faltando), filtros de tenant completamente ausentes em tabelas de negocio sensiveis, e SQL sintaticamente invalido com duplo WHERE.

---

## PROBLEMAS ENCONTRADOS

### 2.1 — Bugs Criticos e Runtime

#### Issue #001 — DriverManager.setLoginTimeout e global/static e causa race condition
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/ConexaoBD.java`
- **Linha(s):** 123
- **Problema:** `DriverManager.setLoginTimeout(5)` e um metodo estatico que afeta TODAS as threads/conexoes. Se duas threads chamam `getConnection()` simultaneamente, uma pode alterar o timeout enquanto outra esta no meio de `DriverManager.getConnection()`.
- **Impacto:** Em cenarios de concorrencia alta (sync + UI), o timeout de login pode mudar inesperadamente para outra thread.
- **Codigo problematico:**
```java
// Linha 123 — dentro do loop que pode rodar em threads paralelas
DriverManager.setLoginTimeout(5);
Connection real = DriverManager.getConnection(URL, USUARIO, SENHA);
```
- **Fix sugerido:**
```java
// Mover para o bloco static {} (executado uma unica vez)
static {
    // ... (apos carregar props)
    DriverManager.setLoginTimeout(5);
}
```

---

#### Issue #002 — PooledConnection.close() nao e thread-safe
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/dao/ConexaoBD.java`
- **Linha(s):** 190-195
- **Problema:** O campo `closed` nao e volatile e o metodo `close()` nao e synchronized. Se duas threads chamarem `close()` no mesmo PooledConnection, a conexao pode ser devolvida ao pool duas vezes.
- **Impacto:** Devolucao dupla ao pool resultaria em duas threads usando a mesma conexao real simultaneamente, causando corrupcao de transacao.
- **Codigo problematico:**
```java
private boolean closed = false; // nao volatile

@Override
public void close() throws SQLException {
    if (!closed) {   // check
        closed = true;  // set — nao atomico
        ConexaoBD.devolver(real);
    }
}
```
- **Fix sugerido:**
```java
private volatile boolean closed = false;

@Override
public synchronized void close() throws SQLException {
    if (!closed) {
        closed = true;
        ConexaoBD.devolver(real);
    }
}
```

---

#### Issue #003 — GET /api/auth/me nao filtra por empresa_id
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/auth.js`
- **Linha(s):** 80-93
- **Problema:** O endpoint `/me` busca o usuario apenas pelo `id` do JWT, sem filtrar por `empresa_id`. O response inclui `u.empresa_id` do resultado SQL mas a query nao seleciona `empresa_id` — sera `undefined`.
- **Impacto:** 1) `empresa_id` retorna `undefined` no response. 2) Usuario de empresa desativada manteria acesso.
- **Codigo problematico:**
```javascript
const result = await pool.query(
  'SELECT id, nome, email, funcao, permissao FROM usuarios WHERE id = $1',
  [req.user.id]
)
// ...
res.json({ ..., empresa_id: u.empresa_id }) // u.empresa_id e undefined
```
- **Fix sugerido:**
```javascript
const result = await pool.query(
  'SELECT id, nome, email, funcao, permissao, empresa_id FROM usuarios WHERE id = $1 AND empresa_id = $2',
  [req.user.id, req.user.empresa_id]
)
```

---

#### Issue #004 — Boleto batch nao usa transacao — parcelas podem ficar parcialmente criadas
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/financeiro.js`
- **Linha(s):** 263-307
- **Problema:** O endpoint `POST /api/financeiro/boleto/batch` executa N inserts (um por parcela) + N inserts na agenda, todos usando `pool.query` direto (sem `BEGIN/COMMIT`). Se uma query falhar na parcela 5 de 10, as 4 primeiras ja estarao salvas no banco.
- **Impacto:** Dados financeiros inconsistentes — parcelas parcialmente criadas sem como identificar/reverter.
- **Codigo problematico:**
```javascript
// Nenhum BEGIN/COMMIT — cada query e independente
for (let i = 0; i < parcelas; i++) {
    const result = await pool.query(`INSERT INTO financeiro_saidas ...`, [...])
    await pool.query('INSERT INTO agenda_anotacoes ...', [...])
}
```
- **Fix sugerido:**
```javascript
const client = await pool.connect()
try {
    await client.query('BEGIN')
    for (let i = 0; i < parcelas; i++) {
        const result = await client.query(`INSERT INTO financeiro_saidas ...`, [...])
        await client.query('INSERT INTO agenda_anotacoes ...', [...])
        boletos.push(result.rows[0])
    }
    await client.query('COMMIT')
    res.status(201).json(boletos)
} catch (err) {
    await client.query('ROLLBACK')
    // ...
} finally {
    client.release()
}
```
- **Observacoes:**
> _Review: Alem de usar transacao, calcular resto da divisao de parcelas e adicionar a ultima parcela para garantir soma = valor_total._

---

#### Issue #005 — Race condition no numero_bilhete / numero_encomenda / numero_frete
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/passagens.js`, `encomendas.js`, `fretes.js`
- **Linha(s):** passagens.js:115-119, encomendas.js:61-65, fretes.js:63-73
- **Problema:** A geracao de numero sequencial usa `SELECT MAX(...) + 1` sem lock explicito. Se dois operadores criam passagens simultaneamente na mesma viagem, ambos podem ler o mesmo MAX e gerar o mesmo numero_bilhete.
- **Impacto:** Numeros de bilhete/encomenda/frete duplicados.
- **Codigo problematico:**
```javascript
const seqResult = await client.query(
  'SELECT COALESCE(MAX(numero_bilhete::bigint), 0) + 1 AS next_num FROM passagens WHERE empresa_id = $1',
  [empresaId]
)
```
- **Fix sugerido:**
```javascript
// Usar advisory lock antes do SELECT MAX, ou usar sequences PostgreSQL
await client.query('SELECT pg_advisory_xact_lock($1)', [empresaId])
const seqResult = await client.query(
  'SELECT COALESCE(MAX(numero_bilhete::bigint), 0) + 1 AS next_num FROM passagens WHERE empresa_id = $1',
  [empresaId]
)
// Ou melhor: usar uma sequencia PostgreSQL por empresa
```
- **Observacoes:**
> _Review: FOR UPDATE nao funciona com MAX() — PostgreSQL ignora lock em aggregate queries. Usar pg_advisory_xact_lock(empresa_id) antes do SELECT MAX, ou criar sequencias por empresa._

---

#### Issue #006 — fretes.js: gera id_frete manual com MAX+1 em tabela com serial/identity
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/fretes.js`
- **Linha(s):** 63-67
- **Problema:** O INSERT inclui `id_frete` manualmente calculado como `MAX(id_frete) + 1`. Se a tabela fretes usa uma sequence/serial para `id_frete`, este valor pode colidir com o valor da sequence em inserts vindos de outras fontes (Desktop sync, API direta).
- **Impacto:** Conflito de PK causando falha no INSERT, ou pior, sobrescrita de dados se nao houver UNIQUE constraint.
- **Codigo problematico:**
```javascript
const idResult = await client.query(
  'SELECT COALESCE(MAX(id_frete), 0) + 1 AS next_id FROM fretes WHERE empresa_id = $1',
  [empresaId]
)
// INSERT INTO fretes (id_frete, ...) VALUES ($1, ...)
```
- **Fix sugerido:**
```javascript
// Manter MAX+1 protegido com pg_advisory_xact_lock(empresaId), ou verificar se tabela usa SERIAL
await client.query('SELECT pg_advisory_xact_lock($1)', [empresaId])
const idResult = await client.query(
  'SELECT COALESCE(MAX(id_frete), 0) + 1 AS next_id FROM fretes WHERE empresa_id = $1',
  [empresaId]
)
```
- **Observacoes:**
> _Review: Desktop tambem gera IDs manualmente para sync. Manter MAX+1 protegido com advisory lock em vez de remover._

---

#### Issue #007 — DELETE encomenda_itens nao filtra por empresa_id (cross-tenant delete possivel)
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/encomendas.js`
- **Linha(s):** 170
- **Problema:** `DELETE FROM encomenda_itens WHERE id_encomenda = $1` usa o `req.params.id` diretamente sem verificar que o id_encomenda pertence ao empresa_id do usuario.
- **Impacto:** Um usuario autenticado pode deletar dados de outra empresa via IDOR. Mitigado: atacante precisa adivinhar id_encomenda SERIAL de outra empresa; so deleta itens (encomenda pai tem filtro empresa_id).
- **Codigo problematico:**
```javascript
await client.query('DELETE FROM encomenda_itens WHERE id_encomenda = $1', [req.params.id])
// So depois verifica empresa_id no DELETE FROM encomendas
```
- **Fix sugerido:**
```javascript
// Usar subquery para garantir tenant isolation
await client.query(
  'DELETE FROM encomenda_itens WHERE id_encomenda IN (SELECT id_encomenda FROM encomendas WHERE id_encomenda = $1 AND empresa_id = $2)',
  [req.params.id, empresaId]
)
```
- **Observacoes:**
> _Review: Severidade ajustada de ALTO para MEDIO — IDOR real mas mitigado: atacante precisa adivinhar id SERIAL de outra empresa._

---

#### Issue #008 — DELETE frete_itens nao filtra por empresa_id (cross-tenant delete possivel)
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/fretes.js`
- **Linha(s):** 168
- **Problema:** Identico ao #007 mas para frete_itens. `DELETE FROM frete_itens WHERE id_frete = $1` usa `req.params.id` sem verificar que o frete pertence ao tenant.
- **Impacto:** Um usuario autenticado pode deletar dados de outra empresa via IDOR. Mitigado pelo mesmo raciocinio do #007.
- **Codigo problematico:**
```javascript
await client.query('DELETE FROM frete_itens WHERE id_frete = $1', [req.params.id])
```
- **Fix sugerido:**
```javascript
await client.query(
  'DELETE FROM frete_itens WHERE id_frete IN (SELECT id_frete FROM fretes WHERE id_frete = $1 AND empresa_id = $2)',
  [req.params.id, empresaId]
)
```
- **Observacoes:**
> _Review: Severidade ajustada de ALTO para MEDIO — mesmo raciocinio do #007._

---

#### Issue #009 — Boleto single tambem nao usa transacao
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/financeiro.js`
- **Linha(s):** 227-260
- **Problema:** `POST /api/financeiro/boleto` faz dois inserts (financeiro_saidas + agenda_anotacoes) sem transacao. Se o insert na agenda falhar, o boleto ja estara criado sem a correspondente anotacao na agenda.
- **Impacto:** Inconsistencia entre boleto e agenda.
- **Codigo problematico:**
```javascript
const result = await pool.query(`INSERT INTO financeiro_saidas ...`, [...])
// Se este falhar, o de cima ja foi salvo
await pool.query('INSERT INTO agenda_anotacoes ...', [...])
```
- **Fix sugerido:** Usar transacao com `pool.connect()` + `BEGIN/COMMIT/ROLLBACK`.

---

#### Issue #010 — TelaPrincipalController usa string concatenation em SQL com DAOUtils.empresaId()
- [ ] **Concluido**
- **Severidade:** INFO
- **Arquivo:** `src/gui/TelaPrincipalController.java`
- **Linha(s):** 535, 538
- **Problema:** Usa concatenacao de string com `empresaId()` em SQL inline ao inves de PreparedStatement com parametro. `empresaId()` retorna um `int` (nao vem de input externo).
- **Impacto:** Nao e SQL injection (valor e int interno), mas cria mau precedente se o padrao for copiado.
- **Codigo problematico:**
```java
carregarDadosComboSimples(cmbBarcos, "SELECT nome FROM embarcacoes WHERE empresa_id = " + dao.DAOUtils.empresaId() + " ORDER BY nome");
```
- **Fix sugerido:** Refatorar `carregarDadosComboSimples` para aceitar parametros SQL, ou usar PreparedStatement.
- **Observacoes:**
> _Review: Severidade ajustada de BAIXO para INFO — empresaId() retorna int de ThreadLocal, nunca vem de input externo. Style issue, nao bug._

---

#### Issue #011 — WebSocket aceita qualquer origem (setAllowedOriginPatterns("*"))
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/config/WebSocketConfig.java`
- **Linha(s):** 21
- **Problema:** O WebSocket STOMP aceita conexoes de QUALQUER origem (`setAllowedOriginPatterns("*")`), enquanto o CORS do REST e corretamente restrito. Permite que qualquer site malicioso conecte ao WebSocket.
- **Impacto:** Vazamento de informacao via WebSocket cross-origin.
- **Codigo problematico:**
```java
registry.addEndpoint("/ws")
        .setAllowedOriginPatterns("*")
        .withSockJS();
```
- **Fix sugerido:**
```java
registry.addEndpoint("/ws")
        .setAllowedOriginPatterns("https://*.naviera.com.br", "http://localhost:*")
        .withSockJS();
```

---

### 2.2 — Seguranca

#### Issue #012 — Senha do banco "123456" em db.properties (arquivo local nao commitado, mas ATIVO)
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `db.properties`
- **Linha(s):** 4
- **Problema:** O arquivo `db.properties` contem a senha `123456` para o banco PostgreSQL local. O codigo em `ConexaoBD.java` detecta e avisa sobre senhas fracas, mas apenas loga um warning. O arquivo esta no `.gitignore`.
- **Impacto:** Qualquer pessoa com acesso a maquina pode acessar o banco local.
- **Codigo problematico:**
```properties
db.senha=123456
```
- **Fix sugerido:**
```properties
db.senha=GERE_COM_openssl_rand_base64_24
```
- **Observacoes:**
> _Review: Severidade ajustada de CRITICO para BAIXO — banco LOCAL de dev em maquina pessoal, arquivo em .gitignore, codigo ja loga warning._

---

#### Issue #013 — sync_config.properties contem senha em texto plano e JWT token
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `sync_config.properties`
- **Linha(s):** 3, 5, 8
- **Problema:** O arquivo contem JWT token em base64, login do operador admin, e senha do operador em TEXTO PLANO (`admin123`). Em .gitignore mas ativo no disco.
- **Impacto:** Comprometimento total do tenant se alguem tiver acesso ao filesystem. Se mesma senha usada em producao, seria CRITICO.
- **Codigo problematico:**
```properties
operador.login=Admin Naviera
operador.senha=admin123
api.token=ZXlKaGJHY2lP...  # JWT decodificavel
```
- **Fix sugerido:**
1. NUNCA armazenar senha em texto plano — usar keyring do OS ou criptografia com chave derivada.
2. Rotacionar imediatamente a senha `admin123` em producao.
3. O token JWT deve ter expiracao curta e nao precisa ser persistido — re-autenticar a cada startup.
- **Observacoes:**
> _Review: Severidade ajustada de CRITICO para ALTO — arquivo local em .gitignore, mas credenciais admin do sistema._

---

#### Issue #014 — db.properties.bak2 commitado no repositorio git
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `db.properties.bak2`
- **Linha(s):** 1-5
- **Problema:** O arquivo `db.properties.bak2` esta COMMITADO no repositorio. A senha nele e `SUA_SENHA_AQUI` (placeholder), mas o historico do git pode conter versoes anteriores com senhas reais.
- **Impacto:** Arquivo desnecessario no repo. Remover por higiene.
- **Codigo problematico:**
```properties
db.senha=SUA_SENHA_AQUI
```
- **Fix sugerido:**
```bash
git rm db.properties.bak2
echo "db.properties.bak*" >> .gitignore
```
- **Observacoes:**
> _Review: Severidade ajustada de MEDIO para BAIXO — conteudo real e placeholder. Sem credencial real exposta._

---

#### Issue #015 — Login sem tenant permite acesso cross-empresa (dev mode)
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/middleware/tenant.js`
- **Linha(s):** 29-31
- **Problema:** Quando `req.tenant` e null (localhost, sem subdominio), o login aceita QUALQUER usuario de QUALQUER empresa. Se o BFF for exposto em producao sem subdominio (IP direto), qualquer operador pode logar em qualquer empresa.
- **Impacto:** Bypass completo do isolamento multi-tenant se o BFF for acessivel sem o header X-Tenant-Slug.
- **Codigo problematico:**
```javascript
if (!slug || slug === 'localhost' || slug === 'api' || slug === 'app' || slug === 'admin') {
    req.tenant = null // sem tenant especifico — login aceita qualquer empresa
    return next()
}
```
- **Fix sugerido:**
```javascript
if (!slug || slug === 'localhost' || slug === 'api' || slug === 'app' || slug === 'admin') {
    if (process.env.NODE_ENV === 'production') {
        return res.status(400).json({ error: 'Subdominio da empresa obrigatorio' })
    }
    req.tenant = null
    return next()
}
```
- **Observacoes:**
> _Review: Fix aplicado em tenant.js (middleware), nao em auth.js como originalmente referenciado._

---

#### Issue #016 — Admin check permite localhost como admin subdomain
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/admin.js`
- **Linha(s):** 14-15
- **Problema:** A funcao `adminOnly` aceita `hostname === 'localhost'` como equivalente a `admin.naviera.com.br`. Se a porta 3002 do BFF for exposta, um operador Administrador de qualquer empresa pode acessar o painel admin.
- **Impacto:** Em producao, se a porta 3002 for exposta, permite acesso admin por fora do Nginx.
- **Codigo problematico:**
```javascript
const isAdminSubdomain = host.startsWith('admin.') || host === 'localhost'
```
- **Fix sugerido:**
```javascript
const isAdminSubdomain = host.startsWith('admin.') || (process.env.NODE_ENV !== 'production' && host === 'localhost')
```

---

#### Issue #017 — WebSocket sem autenticacao permite subscription a notificacoes de qualquer empresa
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/config/WebSocketConfig.java`
- **Linha(s):** 20-23
- **Problema:** O endpoint WebSocket `/ws` esta em `permitAll()`. Nao ha autenticacao no handshake. Qualquer pessoa pode subscrever a `/topic/empresa/{id}/notifications` com qualquer empresa_id.
- **Impacto:** Vazamento de informacao cross-tenant — atacante pode monitorar operacoes de qualquer empresa em tempo real.
- **Codigo problematico:**
```java
// SecurityConfig.java:27
.requestMatchers("/ws/**").permitAll()

// WebSocketConfig.java:21
registry.addEndpoint("/ws")
        .setAllowedOriginPatterns("*")
```
- **Fix sugerido:** Implementar `ChannelInterceptor` para validar JWT no STOMP CONNECT frame e filtrar subscriptions por empresa_id do token.

---

#### Issue #018 — Nginx nao configura CSP (Content-Security-Policy) no wildcard
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `nginx/naviera.conf`
- **Linha(s):** 137-172
- **Problema:** O bloco wildcard `*.naviera.com.br` nao inclui header `Content-Security-Policy`. Sem CSP, XSS pode executar scripts arbitrarios.
- **Impacto:** Se um XSS for encontrado, o atacante pode exfiltrar tokens JWT e dados financeiros. CSP e defense-in-depth, so exploravel com XSS existente.
- **Codigo problematico:**
```nginx
add_header X-Content-Type-Options nosniff always;
add_header X-Frame-Options DENY always;
# Falta: add_header Content-Security-Policy "..." always;
```
- **Fix sugerido:**
```nginx
add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self' wss://*.naviera.com.br" always;
```
- **Observacoes:**
> _Review: Severidade ajustada de MEDIO para BAIXO — boa pratica, nao urgente._

---

#### Issue #019 — API Spring Boot: X-Forwarded-For spoofable para rate limiting
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/config/RateLimitFilter.java`
- **Linha(s):** 83-88
- **Problema:** `getClientIp()` confia no header `X-Forwarded-For` sem validacao. Um atacante pode enviar `X-Forwarded-For: 1.2.3.4` e bypassar o rate limiting por IP.
- **Impacto:** Rate limiting ineficaz — atacante pode fazer brute-force no login sem ser limitado.
- **Codigo problematico:**
```java
private String getClientIp(HttpServletRequest req) {
    String xff = req.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isBlank()) {
        return xff.split(",")[0].trim();
    }
    return req.getRemoteAddr();
}
```
- **Fix sugerido:**
```java
// Usar server.forward-headers-strategy=NATIVE no Spring Boot para respeitar RFC de forma robusta
// Ou confiar no XFF apenas se vier do reverse proxy local:
private String getClientIp(HttpServletRequest req) {
    String remoteAddr = req.getRemoteAddr();
    if ("127.0.0.1".equals(remoteAddr) || "::1".equals(remoteAddr)) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
    }
    return remoteAddr;
}
```
- **Observacoes:**
> _Review: Fix corrigido — usar forward-headers-strategy=NATIVE ou validar IP de origem._

---

#### Issue #020 — BFF Express: rate limiter tambem nao valida IP source para X-Forwarded-For
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/middleware/rateLimit.js`
- **Linha(s):** 17
- **Problema:** O Express nao tem `trust proxy` configurado. Sem isso, `req.ip` retorna o IP do Nginx (127.0.0.1) em producao, resultando em TODOS os clientes compartilhando o mesmo bucket de rate limit.
- **Impacto:** Em producao, TODOS os usuarios compartilham o limite de 200 req/min e 10 logins/min.
- **Codigo problematico:**
```javascript
// index.js — NAO tem app.set('trust proxy', ...)
const key = req.ip || req.connection.remoteAddress
// Em producao: req.ip = '127.0.0.1' para TODOS
```
- **Fix sugerido:**
```javascript
// Em index.js, adicionar ANTES dos middlewares:
app.set('trust proxy', 'loopback')
```

---

#### Issue #021 — JWT da API tem expiracao de 24h — excessivo para operador
- [ ] **Concluido**
- **Severidade:** INFO
- **Arquivo:** `naviera-api/src/main/resources/application.properties`
- **Linha(s):** 31
- **Problema:** `naviera.jwt.expiration-ms=86400000` (24h). Mesmo secret e expiracao sao usados para operadores (console web) e app mobile.
- **Impacto:** Token de operador valido por 24h aumenta janela de ataque se comprometido.
- **Codigo problematico:**
```properties
naviera.jwt.expiration-ms=86400000
```
- **Fix sugerido:** Usar expiracao diferenciada: 8h para operadores, 24h para app mobile. Ou implementar refresh tokens.
- **Observacoes:**
> _Review: Severidade ajustada de BAIXO para INFO — 24h e razoavel para app mobile e Desktop com conectividade intermitente em barcos._

---

#### Issue #022 — Tenant cache no BFF nao e invalidado quando empresa e desativada
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-web/server/middleware/tenant.js`
- **Linha(s):** 4-5, 36-39
- **Problema:** O cache de slug para empresa tem TTL de 5 minutos. Se um admin desativar uma empresa, o BFF continuara servindo requests por ate 5 minutos.
- **Impacto:** Empresa desativada continua operacional por ate 5 minutos apos desativacao. Operacao rarissima.
- **Codigo problematico:**
```javascript
const CACHE_TTL = 5 * 60 * 1000 // 5 minutos
```
- **Fix sugerido:** Reduzir TTL para 30-60 segundos, ou implementar invalidacao via webhook.
- **Observacoes:**
> _Review: Severidade ajustada de MEDIO para BAIXO — desativar empresa e operacao rarissima. 5 min de cache aceitavel._

---

#### Issue #023 — API registrar() nao valida formato/tamanho do documento (CPF/CNPJ)
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/AuthService.java`
- **Linha(s):** 36-51
- **Problema:** O endpoint de registro aceita qualquer string como `documento` e `tipoDocumento` sem validar formato (11 digitos CPF, 14 CNPJ), nem verificar digitos verificadores.
- **Impacto:** Dados invalidos no banco. Possibilidade de cadastro com documentos absurdos.
- **Codigo problematico:**
```java
c.setDocumento(req.documento()); // sem validacao de formato
c.setTipoDocumento(req.tipoDocumento() != null ? req.tipoDocumento() : "CPF");
```
- **Fix sugerido:** Validar CPF/CNPJ com digito verificador antes do registro. Restringir `tipoDocumento` a enum {"CPF", "CNPJ"}.

---

#### Issue #024 — SyncClient conecta via HTTP sem TLS a servidor remoto (apenas warning)
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** 806-808
- **Problema:** O SyncClient detecta quando esta usando HTTP para um servidor remoto e apenas loga um warning, mas permite a conexao. Credenciais JWT e dados de negocio sao enviados em texto plano.
- **Impacto:** Man-in-the-middle pode capturar JWT, dados de passageiros, informacoes financeiras. Em producao, serverUrl aponta para api.naviera.com.br com SSL via Nginx, risco real apenas se alguem configurar http:// manualmente.
- **Codigo problematico:**
```java
if (serverUrl != null && serverUrl.startsWith("http://")
    && !serverUrl.contains("localhost") && !serverUrl.contains("127.0.0.1")) {
    AppLogger.warn(TAG, "AVISO SEGURANCA: Sync usando HTTP sem TLS...");
    // NAO bloqueia
}
```
- **Fix sugerido:** Em producao, bloquear conexoes HTTP para servidores remotos ou exigir confirmacao explicita.
- **Observacoes:**
> _Review: Severidade ajustada de MEDIO (mantido) — em producao, serverUrl aponta para HTTPS via Nginx._

---

#### Issue #025 — Nginx API block falta security headers
- [ ] **Concluido**
- **Severidade:** INFO
- **Arquivo:** `nginx/naviera.conf`
- **Linha(s):** 57-74
- **Problema:** O bloco `api.naviera.com.br` tem HSTS mas falta `X-Content-Type-Options` e `X-Frame-Options`.
- **Impacto:** Baixo — API nao serve HTML. Defense-in-depth recomenda headers em todas as respostas.
- **Codigo problematico:**
```nginx
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
# Falta: X-Content-Type-Options, X-Frame-Options
```
- **Fix sugerido:**
```nginx
add_header X-Content-Type-Options nosniff always;
add_header X-Frame-Options DENY always;
```
- **Observacoes:**
> _Review: Severidade ajustada de BAIXO para INFO — API nao serve HTML, headers irrelevantes para API pura._

---

#### Issue #026 — Nginx app.naviera.com.br falta X-Content-Type-Options e X-Frame-Options
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `nginx/naviera.conf`
- **Linha(s):** 79-103
- **Problema:** O bloco `app.naviera.com.br` (app mobile PWA) tem HSTS mas falta headers de seguranca.
- **Impacto:** XSS via MIME-type confusion, ou clickjacking. App usa JWT em header (nao cookies), reduz impacto.
- **Codigo problematico:**
```nginx
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
# Falta: X-Content-Type-Options, X-Frame-Options
```
- **Fix sugerido:**
```nginx
add_header X-Content-Type-Options nosniff always;
add_header X-Frame-Options DENY always;
```
- **Observacoes:**
> _Review: Severidade ajustada de MEDIO para BAIXO — app usa JWT em header, reduz impacto de clickjacking._

---

#### Issue #027 — GlobalExceptionHandler faz e.printStackTrace() em producao
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/config/GlobalExceptionHandler.java`
- **Linha(s):** 28
- **Problema:** O handler generico de excecoes faz `e.printStackTrace()` que escreve stack traces completos no stdout/stderr.
- **Impacto:** Information disclosure nos logs. Pode revelar detalhes internos.
- **Codigo problematico:**
```java
@ExceptionHandler(Exception.class)
public ResponseEntity<?> handleGeneric(Exception e) {
    e.printStackTrace();
    return ResponseEntity.internalServerError().body(Map.of("erro", "Erro interno do servidor"));
}
```
- **Fix sugerido:**
```java
@ExceptionHandler(Exception.class)
public ResponseEntity<?> handleGeneric(Exception e) {
    log.error("Erro nao tratado: {}", e.getMessage(), e);
    return ResponseEntity.internalServerError().body(Map.of("erro", "Erro interno do servidor"));
}
```

---

#### Issue #028 — BFF Express sem trust proxy: rate limiter inoperante em producao
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/index.js`
- **Problema:** Sem `app.set('trust proxy', 1)`, `req.ip` retorna `127.0.0.1` para TODOS os usuarios atras do Nginx. Rate limiter de login (10 tentativas/min) e compartilhado entre todos, permitindo DoS trivial.
- **Impacto:** Um unico usuario pode bloquear login para TODOS os outros. Logs mostram 127.0.0.1 para todos.
- **Fix sugerido:**
```javascript
// Em index.js, ANTES dos middlewares:
app.set('trust proxy', 1)
```
- **Observacoes:**
> _Issue nova encontrada na contra-verificacao (SEC-NEW-001)._

---

#### Issue #029 — Login BFF: empresa_id fallback silencioso para 1
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/auth.js`
- **Linha(s):** 56
- **Problema:** `empresa_id: user.empresa_id || 1` — usuario sem empresa_id (dados corrompidos) recebe acesso automatico a empresa 1.
- **Impacto:** Acesso nao autorizado a dados da empresa 1 em caso de dados corrompidos.
- **Fix sugerido:**
```javascript
if (!user.empresa_id) {
  return res.status(403).json({ error: 'Usuario sem empresa associada' })
}
// JWT com empresa_id: user.empresa_id (sem fallback)
```
- **Observacoes:**
> _Issue nova encontrada na contra-verificacao (SEC-NEW-002)._

---

### 2.3 — Logica de Negocio

#### Issue #030 — TarifaDAO.buscarTarifaPorRotaETipo: parametros em posicao errada (empresa_id ignorado)
- [x] **Concluido** _(corrigido 2026-04-14)_
- **Severidade:** CRITICO
- **Arquivo:** `src/dao/TarifaDAO.java`
- **Linha(s):** 17-22
- **Problema:** O SQL contem `WHERE empresa_id = ? AND id_rota = ? AND id_tipo_passagem = ?` (3 parametros), mas o bind seta `stmt.setLong(1, idRota)` e `stmt.setInt(2, idTipoPassagem)` — apenas 2 parametros. O empresa_id recebe o valor de idRota. A query nunca retorna a tarifa correta.
- **Impacto:** Tarifas nunca sao encontradas, ou retorna tarifa de outra empresa. Precificacao completamente quebrada.
- **Codigo problematico:**
```java
String sql = "SELECT ... FROM tarifas WHERE empresa_id = ? AND id_rota = ? AND id_tipo_passagem = ?";
stmt.setLong(1, idRota);       // ERRADO: deveria ser empresaId
stmt.setInt(2, idTipoPassagem); // ERRADO: deveria ser idRota
// FALTA: stmt.setInt(3, ...) para idTipoPassagem
```
- **Fix sugerido:**
```java
stmt.setInt(1, DAOUtils.empresaId());
stmt.setLong(2, idRota);
stmt.setInt(3, idTipoPassagem);
```

---

#### Issue #031 — PassageiroDAO.inserir: SQL tem 6 placeholders mas 7 parametros
- [x] **Concluido** _(corrigido 2026-04-14)_
- **Severidade:** CRITICO
- **Arquivo:** `src/dao/PassageiroDAO.java`
- **Linha(s):** 58-71
- **Problema:** O SQL INSERT lista 7 colunas mas o VALUES tem apenas 6 placeholders. O `stmt.setInt(7, empresaId())` vai lancar `PSQLException: The column index is out of range`.
- **Impacto:** Toda insercao de passageiro no Desktop falha com excecao. Funcionalidade core completamente quebrada.
- **Codigo problematico:**
```java
String sql = "INSERT INTO passageiros (nome_passageiro, numero_documento, id_tipo_doc, data_nascimento, id_sexo, id_nacionalidade, empresa_id) VALUES (?, ?, ?, ?, ?, ?)";
// 7 setXxx() calls mas apenas 6 placeholders
stmt.setInt(7, empresaId()); // BOOM: index out of range
```
- **Fix sugerido:**
```java
String sql = "INSERT INTO passageiros (nome_passageiro, numero_documento, id_tipo_doc, data_nascimento, id_sexo, id_nacionalidade, empresa_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
```

---

#### Issue #032 — PassageiroDAO.listarTodos: PreparedStatement executado sem setar parametro
- [x] **Concluido** _(corrigido 2026-04-14)_
- **Severidade:** CRITICO
- **Arquivo:** `src/dao/PassageiroDAO.java`
- **Linha(s):** 32-43
- **Problema:** O SQL usa `WHERE empresa_id = ?` mas `stmt.executeQuery()` esta no try-with-resources, executado ANTES de `stmt.setInt(1, empresaId())`.
- **Impacto:** Lista de passageiros sempre vazia ou erro de execucao.
- **Codigo problematico:**
```java
try (Connection conn = ConexaoBD.getConnection();
     PreparedStatement stmt = conn.prepareStatement(sql);
     ResultSet rs = stmt.executeQuery()) {  // Executa ANTES de setar parametro!
    while (rs.next()) { ... }
}
```
- **Fix sugerido:**
```java
try (Connection conn = ConexaoBD.getConnection();
     PreparedStatement stmt = conn.prepareStatement(sql)) {
    stmt.setInt(1, empresaId());
    try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) { ... }
    }
}
```

---

#### Issue #033 — PassageiroDAO.listarTodosNomesPassageiros: mesmo problema — PreparedStatement executado sem setar parametro
- [x] **Concluido** _(corrigido 2026-04-14)_
- **Severidade:** CRITICO
- **Arquivo:** `src/dao/PassageiroDAO.java`
- **Linha(s):** 113-125
- **Problema:** Identico ao #032. SQL usa `empresa_id = ?` mas `stmt.executeQuery()` esta no try-with-resources, executado antes de setar o parametro.
- **Impacto:** ComboBox de nomes de passageiros vazio, impedindo venda de passagens.
- **Codigo problematico:**
```java
try (Connection conn = ConexaoBD.getConnection();
     PreparedStatement stmt = conn.prepareStatement(sql);
     ResultSet rs = stmt.executeQuery()) { // ANTES de setInt!
```
- **Fix sugerido:** Mesmo padrao do #032.

---

#### Issue #034 — PassageiroDAO.buscarPorNome: parametro na posicao errada (tenant isolation violation)
- [x] **Concluido** _(corrigido 2026-04-14)_
- **Severidade:** CRITICO
- **Arquivo:** `src/dao/PassageiroDAO.java`
- **Linha(s):** 128-141
- **Problema:** SQL e `WHERE empresa_id = ? AND nome_passageiro ILIKE ?`. O bind seta `stmt.setString(1, nome)` — o nome vai na posicao do empresa_id. O empresa_id nunca e setado.
- **Impacto:** Violacao de tenant isolation — passageiro de outra empresa pode ser retornado.
- **Codigo problematico:**
```java
stmt.setString(1, nome);  // ERRADO: posicao 1 e empresa_id
// FALTA: stmt.setInt(1, empresaId()) e stmt.setString(2, nome)
```
- **Fix sugerido:**
```java
stmt.setInt(1, empresaId());
stmt.setString(2, nome);
```

---

#### Issue #035 — TipoPassageiroDAO.inserir: SQL com 6 colunas mas 5 placeholders, empresa_id nunca inserido
- [x] **Concluido** _(corrigido 2026-04-14)_
- **Severidade:** CRITICO
- **Arquivo:** `src/dao/TipoPassageiroDAO.java`
- **Linha(s):** 13-31
- **Problema:** O SQL INSERT lista `empresa_id` nas colunas mas o VALUES tem apenas 5 placeholders em vez de 6. O `empresa_id` nunca e inserido.
- **Impacto:** Tipos de passageiro criados sem empresa_id — visiveis para todas as empresas ou rejeitados por constraint NOT NULL.
- **Codigo problematico:**
```java
String sql = "INSERT INTO tipo_passageiro (nome, idade_min, idade_max, deficiente, gratuito, empresa_id) "
           + "VALUES (?,?,?,?,?)";
// 6 colunas, 5 placeholders, setInt(6, empresaId()) nunca chamado
```
- **Fix sugerido:**
```java
String sql = "INSERT INTO tipo_passageiro (nome, idade_min, idade_max, deficiente, gratuito, empresa_id) "
           + "VALUES (?,?,?,?,?,?)";
// ...
ps.setInt(6, DAOUtils.empresaId());
```
- **Observacoes:**
> _Review: Fix completo: (1) Mudar VALUES para (?,?,?,?,?,?) E (2) Adicionar ps.setInt(6, DAOUtils.empresaId()); antes de executeUpdate._

---

#### Issue #036 — TipoPassageiroDAO.listarTodos: sem filtro empresa_id (tenant data leak)
- [x] **Concluido** _(corrigido 2026-04-14)_
- **Severidade:** CRITICO
- **Arquivo:** `src/dao/TipoPassageiroDAO.java`
- **Linha(s):** 33-57
- **Problema:** A query `SELECT ... FROM tipo_passageiro ORDER BY id` nao filtra por empresa_id. Retorna tipos de passageiro de TODAS as empresas.
- **Impacto:** Empresas veem tipos de passageiro de outras empresas. Dados cruzados entre tenants.
- **Codigo problematico:**
```java
String sql = "SELECT id, nome, idade_min, idade_max, deficiente, gratuito "
           + "FROM tipo_passageiro "
           + "ORDER BY id";
// Sem WHERE empresa_id = ?
```
- **Fix sugerido:**
```java
String sql = "SELECT id, nome, idade_min, idade_max, deficiente, gratuito "
           + "FROM tipo_passageiro WHERE empresa_id = ? ORDER BY id";
// + stmt.setInt(1, DAOUtils.empresaId());
```

---

#### Issue #037 — TarifaDAO.listarTodos: sem filtro empresa_id (tenant data leak)
- [x] **Concluido** _(corrigido 2026-04-14)_
- **Severidade:** CRITICO
- **Arquivo:** `src/dao/TarifaDAO.java`
- **Linha(s):** 95-126
- **Problema:** A query nao filtra por `empresa_id`. Retorna tarifas de todas as empresas.
- **Impacto:** Empresa ve precos de tarifas de todas as outras empresas. Vazamento de dados comerciais sensiveis.
- **Codigo problematico:**
```java
String sql = "SELECT t.id_tarifa, t.id_rota, ... FROM tarifas t " +
             "JOIN rotas r ON t.id_rota = r.id " +
             "JOIN aux_tipos_passagem atp ON t.id_tipo_passagem = atp.id_tipo_passagem " +
             "ORDER BY r.origem, atp.nome_tipo_passagem";
// Nenhum WHERE empresa_id = ?
```
- **Fix sugerido:**
```java
String sql = "... FROM tarifas t " +
             "JOIN rotas r ON t.id_rota = r.id " +
             "JOIN aux_tipos_passagem atp ON t.id_tipo_passagem = atp.id_tipo_passagem " +
             "WHERE t.empresa_id = ? ORDER BY r.origem, atp.nome_tipo_passagem";
// + stmt.setInt(1, DAOUtils.empresaId());
```

---

#### Issue #038 — DespesaDAO.buscarDespesas: sem filtro empresa_id
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/dao/DespesaDAO.java`
- **Linha(s):** 26-92
- **Problema:** A query monta condicoes dinamicas mas NUNCA adiciona `empresa_id = ?`. O filtro de tenant esta completamente ausente.
- **Impacto:** Despesas de todas as empresas sao exibidas para qualquer operador.
- **Codigo problematico:**
```java
StringBuilder sql = new StringBuilder(
    "SELECT ... FROM financeiro_saidas s " +
    "LEFT JOIN categorias_despesa c ON s.id_categoria = c.id " +
    "WHERE 1=1 "
);
// Nenhum: sql.append(" AND s.empresa_id = ?"); params.add(DAOUtils.empresaId());
```
- **Fix sugerido:**
```java
sql.append(" AND s.empresa_id = ?");
params.add(DAOUtils.empresaId());
```
- **Observacoes:**
> _Review: Severidade ajustada de CRITICO para ALTO — metodo recebe idViagem como filtro obrigatorio, limitando escopo. Ainda precisa corrigir._

---

#### Issue #039 — ReciboAvulsoDAO.listarPorViagem: parametro na posicao errada
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/dao/ReciboAvulsoDAO.java`
- **Linha(s):** 39-57
- **Problema:** SQL e `WHERE empresa_id = ? AND id_viagem = ?` (2 parametros). O bind seta `stmt.setInt(1, idViagem)` — o idViagem vai na posicao do empresa_id. O empresa_id nunca e setado na posicao 2.
- **Impacto:** Recibos avulsos nao sao listados ou listam dados de outro tenant.
- **Codigo problematico:**
```java
String sql = "SELECT * FROM recibos_avulsos WHERE empresa_id = ? AND id_viagem = ? ORDER BY id_recibo DESC";
stmt.setInt(1, idViagem);  // ERRADO: deveria ser DAOUtils.empresaId()
// FALTA: stmt.setInt(2, idViagem);
```
- **Fix sugerido:**
```java
stmt.setInt(1, DAOUtils.empresaId());
stmt.setInt(2, idViagem);
```

---

#### Issue #040 — AgendaDAO.adicionarAnotacao: executeUpdate nunca chamado
- [x] **Concluido** _(corrigido 2026-04-14)_
- **Severidade:** CRITICO
- **Arquivo:** `src/dao/AgendaDAO.java`
- **Linha(s):** 53-63
- **Problema:** O PreparedStatement e preparado e os parametros sao setados, mas `stmt.executeUpdate()` nunca e chamado. A anotacao nunca e salva no banco.
- **Impacto:** Todas as anotacoes de agenda criadas pelo usuario sao silenciosamente descartadas. Dados perdidos sem aviso.
- **Codigo problematico:**
```java
try (Connection conn = ConexaoBD.getConnection();
     PreparedStatement stmt = conn.prepareStatement(sql)) {
    stmt.setDate(1, Date.valueOf(data));
    stmt.setString(2, texto);
    stmt.setInt(3, DAOUtils.empresaId());
    // FALTA: stmt.executeUpdate();
}
```
- **Fix sugerido:**
```java
    stmt.setInt(3, DAOUtils.empresaId());
    stmt.executeUpdate();
}
```

---

#### Issue #041 — AgendaDAO.buscarBoletosPendentesNoMes: sem filtro empresa_id
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/dao/AgendaDAO.java`
- **Linha(s):** 115-145
- **Problema:** A query de boletos pendentes nao filtra por empresa_id. Boletos pendentes de todas as empresas aparecem no calendario.
- **Impacto:** Operador ve boletos de outras empresas no calendario.
- **Codigo problematico:**
```java
String sql = "SELECT data_vencimento, descricao, valor_total FROM financeiro_saidas " +
             "WHERE forma_pagamento = 'BOLETO' AND status = 'PENDENTE' " +
             "AND data_vencimento >= ? AND data_vencimento < ?";
// Sem AND empresa_id = ?
```
- **Fix sugerido:**
```java
String sql = "SELECT ... FROM financeiro_saidas " +
             "WHERE forma_pagamento = 'BOLETO' AND status = 'PENDENTE' " +
             "AND empresa_id = ? AND data_vencimento >= ? AND data_vencimento < ?";
// + stmt.setInt(1, DAOUtils.empresaId());
```

---

#### Issue #042 — DespesaDAO.excluirBoleto: audit + delete sem transacao
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/dao/DespesaDAO.java`
- **Linha(s):** 191-213
- **Problema:** Executa INSERT na auditoria_financeiro e depois DELETE no financeiro_saidas, ambos auto-commit. Se o DELETE falhar, o registro de auditoria ja foi persistido indicando que o boleto foi excluido quando nao foi.
- **Impacto:** Inconsistencia entre auditoria e dados reais.
- **Codigo problematico:**
```java
try (Connection con = ConexaoBD.getConnection()) {
    // INSERT auditoria (auto-commit = true)
    try (PreparedStatement audit = con.prepareStatement(...)) { audit.executeUpdate(); }
    // DELETE financeiro_saidas — se falhar, auditoria ja foi commitada
    try (PreparedStatement s = con.prepareStatement(...)) { return s.executeUpdate() > 0; }
}
```
- **Fix sugerido:**
```java
try (Connection con = ConexaoBD.getConnection()) {
    con.setAutoCommit(false);
    try {
        // INSERT auditoria
        // DELETE financeiro_saidas
        con.commit();
    } catch (SQLException ex) {
        con.rollback();
        throw ex;
    }
}
```

---

#### Issue #043 — ItemEncomendaPadraoDAO.listarTodos: SQL sintaticamente invalido (duplo WHERE)
- [x] **Concluido** _(corrigido 2026-04-14)_
- **Severidade:** CRITICO
- **Arquivo:** `src/dao/ItemEncomendaPadraoDAO.java`
- **Linha(s):** 28-56
- **Problema:** Quando `apenasAtivos` e true, o SQL resultante e: `SELECT * FROM itens_encomenda_padrao WHERE empresa_id = ? WHERE ativo = true ORDER BY nome_item`. Dois `WHERE` na mesma query = erro de sintaxe SQL.
- **Impacto:** Listagem de itens de encomenda padrao com filtro de ativos sempre falha com erro de SQL.
- **Codigo problematico:**
```java
String sql = "SELECT * FROM itens_encomenda_padrao WHERE empresa_id = ? ";
if (apenasAtivos) {
    sql += " WHERE ativo = true ";  // ERRO: deveria ser AND
}
```
- **Fix sugerido:**
```java
if (apenasAtivos) {
    sql += " AND ativo = true ";
}
```

---

#### Issue #044 — FuncionarioDAO: double para valores financeiros (salario, pagamentos)
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/FuncionarioDAO.java`
- **Linha(s):** 100-118, 172-220, 285, 314
- **Problema:** Metodos financeiros usam `double` para valores monetarios. Calculos com double causam erros de arredondamento.
- **Impacto:** Erros de centavos em calculos de folha de pagamento.
- **Codigo problematico:**
```java
stmt.setDouble(8, f.getSalario());
stmt.setDouble(2, valor);
stmt.setDouble(14, f.getValorInss());
```
- **Fix sugerido:** Migrar Funcionario.salario, valorInss, e metodos financeiros para BigDecimal.

---

#### Issue #045 — FuncionarioDAO.buscarIdCategoriaFuncionarios: tabela 'categorias' sem filtro empresa_id
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/dao/FuncionarioDAO.java`
- **Linha(s):** 357-367
- **Problema:** A query `SELECT id FROM categorias WHERE UPPER(nome) LIKE ...` nao filtra por empresa_id. Pode retornar categoria de outra empresa.
- **Impacto:** Debito de funcionario pode ser lancado na categoria errada (de outra empresa).
- **Codigo problematico:**
```java
"SELECT id FROM categorias WHERE UPPER(nome) LIKE '%FUNCIONARIO%' OR ..."
// Sem AND empresa_id = ?
```
- **Fix sugerido:**
```java
"SELECT id FROM categorias_despesa WHERE (UPPER(nome) LIKE '%FUNCIONARIO%' OR ...) AND empresa_id = ? LIMIT 1"
```
- **Observacoes:**
> _Review: Fix corrigido — mudar para PreparedStatement com WHERE ... AND empresa_id = ? + stmt.setInt(1, DAOUtils.empresaId())._

---

#### Issue #046 — PassagemDAO.obterProximoBilhete: fallback sem filtro empresa_id
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/dao/PassagemDAO.java`
- **Linha(s):** 34-39
- **Problema:** O fallback `SELECT COALESCE(MAX(CAST(numero_bilhete AS INTEGER)), 0) + 1 FROM passagens` nao filtra por empresa_id. Se a sequence falhar, o proximo bilhete e calculado com base em TODAS as passagens.
- **Impacto:** Numeros de bilhete duplicados ou com gaps grandes em cenario multi-tenant quando a sequence nao existe.
- **Codigo problematico:**
```java
String fallback = "SELECT COALESCE(MAX(CAST(numero_bilhete AS INTEGER)), 0) + 1 FROM passagens";
```
- **Fix sugerido:**
```java
String fallback = "SELECT COALESCE(MAX(CAST(numero_bilhete AS INTEGER)), 0) + 1 FROM passagens WHERE empresa_id = ?";
// + stmt.setInt(1, DAOUtils.empresaId());
```
- **Observacoes:**
> _Review: Severidade ajustada de MEDIO para BAIXO — fallback so executa se sequence nao existe (cenario migration nao rodada)._

---

#### Issue #047 — EncomendaDAO.obterProximoNumero: fallback sem filtro empresa_id
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/dao/EncomendaDAO.java`
- **Linha(s):** 230-238
- **Problema:** O fallback query filtra por `id_viagem` e `rota` mas NAO por empresa_id.
- **Impacto:** Numeros de encomenda duplicados em cenario onde a sequence falha.
- **Codigo problematico:**
```java
String fallback = "SELECT ... FROM encomendas WHERE id_viagem = ? AND rota = ? AND ...";
// Sem AND empresa_id = ?
```
- **Fix sugerido:** Adicionar `AND empresa_id = ?` e setar o parametro. Usar nextval dentro de try/catch com fallback `MAX+1 WHERE empresa_id = $1` na mesma transacao. Ou garantir migration 005 no banco central.
- **Observacoes:**
> _Review: Severidade ajustada de MEDIO para BAIXO — mesmo raciocinio do #046._

---

#### Issue #048 — Web BFF passagens.js: numero_bilhete usa MAX+1 em vez de sequence (race condition)
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/passagens.js`
- **Linha(s):** 115-119
- **Problema:** O Desktop usa `SELECT nextval('seq_numero_bilhete')` (atomico), mas o Web BFF usa `SELECT COALESCE(MAX(numero_bilhete::bigint), 0) + 1`. Em operacoes concorrentes, dois bilhetes podem receber o mesmo numero.
- **Impacto:** Bilhetes duplicados quando dois operadores criam passagens simultaneamente no web.
- **Codigo problematico:**
```javascript
const seqResult = await client.query(
  'SELECT COALESCE(MAX(numero_bilhete::bigint), 0) + 1 AS next_num FROM passagens WHERE empresa_id = $1',
  [empresaId]
)
```
- **Fix sugerido:**
```javascript
const seqResult = await client.query("SELECT nextval('seq_numero_bilhete') AS next_num")
```

---

#### Issue #049 — Web BFF encomendas.js: numero_encomenda usa MAX+1 em vez de sequence (race condition)
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/encomendas.js`
- **Linha(s):** 61-64
- **Problema:** Identico ao #048 mas para encomendas. Usa `MAX(numero_encomenda)` em vez de sequence.
- **Impacto:** Numeros de encomenda duplicados em uso concorrente.
- **Codigo problematico:**
```javascript
const seqResult = await client.query(
  'SELECT COALESCE(MAX(numero_encomenda), 0) + 1 AS next_num FROM encomendas WHERE empresa_id = $1',
  [empresaId]
)
```
- **Fix sugerido:** Usar `SELECT nextval('seq_numero_encomenda')`.

---

#### Issue #050 — Web BFF financeiro.js boleto/batch: sem transacao (parcelas parciais)
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/financeiro.js`
- **Linha(s):** 262-307
- **Problema:** O endpoint `POST /api/financeiro/boleto/batch` insere N parcelas de boleto + N entradas de agenda em loop com `pool.query()` individual sem transacao.
- **Impacto:** Boletos parcelados com parcelas faltando no banco. Inconsistencia financeira.
- **Codigo problematico:**
```javascript
for (let i = 0; i < parcelas; i++) {
  const result = await pool.query(`INSERT INTO financeiro_saidas ...`, [...])
  await pool.query('INSERT INTO agenda_anotacoes ...', [...])
}
```
- **Fix sugerido:**
```javascript
const client = await pool.connect()
await client.query('BEGIN')
try {
  for (let i = 0; i < parcelas; i++) { ... }
  await client.query('COMMIT')
} catch (err) {
  await client.query('ROLLBACK')
  throw err
} finally {
  client.release()
}
```

---

#### Issue #051 — DespesaDAO.buscarBoletos: rs.getDouble para valor financeiro
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/DespesaDAO.java`
- **Linha(s):** 379
- **Problema:** `rs.getDouble("valor_total")` usa double para valor financeiro, enquanto os demais metodos do mesmo DAO usam `rs.getBigDecimal()`. Inconsistencia.
- **Impacto:** Valores de boletos podem ter erros de centavos.
- **Codigo problematico:**
```java
row.put("valor_total", rs.getDouble("valor_total"));
```
- **Fix sugerido:**
```java
row.put("valor_total", rs.getBigDecimal("valor_total"));
```

---

#### Issue #052 — UsuarioDAO.buscarPorUsuarioESenha e buscarPorLogin: sem filtro empresa_id
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/dao/UsuarioDAO.java`
- **Linha(s):** 148-162, 165-183
- **Problema:** `buscarPorLogin` e `buscarPorUsuarioESenha` fazem `WHERE nome = ?` sem filtrar por empresa_id. Um operador de empresa A pode fazer login com credenciais de empresa B se o nome coincide.
- **Impacto:** Violacao de autenticacao cross-tenant.
- **Codigo problematico:**
```java
String sql = "SELECT ... FROM usuarios WHERE nome = ?";  // SEM empresa_id
```
- **Fix sugerido:**
```java
String sql = "SELECT ... FROM usuarios WHERE nome = ? AND empresa_id = ? AND excluido IS NOT TRUE";
// + ps.setInt(2, empresaId());
```

---

#### Issue #053 — ViagemDAO: cache estatico sem isolamento de tenant
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/dao/ViagemDAO.java`
- **Linha(s):** 155-156
- **Problema:** `cacheViagemAtiva` e um campo `static` unico. No Desktop (single-tenant) funciona, mas se reutilizado em multi-thread, uma thread de empresa A pode obter a viagem de empresa B.
- **Impacto:** Mitigado — Desktop SEMPRE opera single-tenant. DAO nao usado pela API.
- **Codigo problematico:**
```java
private static Viagem cacheViagemAtiva = null;
```
- **Fix sugerido:**
```java
private static final ConcurrentHashMap<Integer, Viagem> cacheViagemAtiva = new ConcurrentHashMap<>();
```
- **Observacoes:**
> _Review: Severidade ajustada de MEDIO para BAIXO — Desktop SEMPRE opera single-tenant. Cache seguro no contexto._

---

#### Issue #054 — Web BFF financeiro.js: valor de balanco calculado com parseFloat (precisao)
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-web/server/routes/financeiro.js`
- **Linha(s):** 63-68
- **Problema:** O balanco financeiro soma receitas usando `parseFloat()`. Para valores grandes, `parseFloat` pode perder precisao.
- **Impacto:** Diferenca de centavos entre Desktop e Web em balancos com valores altos. Impacto pratico minimo para transporte fluvial regional.
- **Codigo problematico:**
```javascript
receitas.passagens + receitas.encomendas + receitas.fretes
```
- **Fix sugerido:** Usar biblioteca de precisao como `decimal.js` ou calcular somas no PostgreSQL diretamente.
- **Observacoes:**
> _Review: Severidade ajustada de MEDIO para BAIXO — parseFloat perde precisao apenas acima de 2^53._

---

#### Issue #055 — PassagemDAO.temDataChegada: campo de instancia compartilhado entre chamadas (thread-unsafe)
- [ ] **Concluido**
- **Severidade:** INFO
- **Arquivo:** `src/dao/PassagemDAO.java`
- **Linha(s):** 183
- **Problema:** `temDataChegada` e campo de instancia setado em `listarTodos()` e usado em `mapResultSetToPassagem()`.
- **Impacto:** Minimo — Desktop roda single-thread (JavaFX Application Thread). Cada controller cria propria instancia de DAO.
- **Codigo problematico:**
```java
private boolean temDataChegada = false;
```
- **Fix sugerido:** Passar como parametro para `mapResultSetToPassagem`.
- **Observacoes:**
> _Review: Severidade ajustada de BAIXO para INFO — sem concorrencia real no Desktop._

---

#### Issue #056 — ItemEncomendaPadraoDAO.listarTodos: PreparedStatement executado sem setar parametro
- [x] **Concluido** _(corrigido 2026-04-14)_
- **Severidade:** CRITICO
- **Arquivo:** `src/dao/ItemEncomendaPadraoDAO.java`
- **Linha(s):** 36-38
- **Problema:** SQL usa `WHERE empresa_id = ?` mas `stmt.executeQuery()` e `ResultSet` estao no try-with-resources junto com o PreparedStatement. Query executa ANTES de `stmt.setInt(1, empresaId())`. Bug ADICIONAL ao duplo WHERE do #043.
- **Impacto:** Metodo sempre falha com PSQLException.
- **Fix sugerido:** Separar PreparedStatement do try-with-resources e setar parametro antes de executeQuery.
- **Observacoes:**
> _Issue nova encontrada na contra-verificacao (NOVO-L01)._

---

#### Issue #057 — ReciboAvulsoDAO.listarPorViagem: falta segundo parametro no bind
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/dao/ReciboAvulsoDAO.java`
- **Linha(s):** 42-46
- **Problema:** SQL tem `WHERE empresa_id = ? AND id_viagem = ?` (2 placeholders) mas so 1 parametro e setado: `stmt.setInt(1, idViagem)`. Segundo parametro nunca setado.
- **Impacto:** PSQLException em toda chamada.
- **Fix sugerido:**
```java
stmt.setInt(1, DAOUtils.empresaId());
stmt.setInt(2, idViagem);
```
- **Observacoes:**
> _Issue nova encontrada na contra-verificacao (NOVO-L02)._

---

#### Issue #058 — TipoPassageiroDAO.inserir: empresa_id nunca inserido
- [x] **Concluido** _(corrigido 2026-04-14)_
- **Severidade:** CRITICO
- **Arquivo:** `src/dao/TipoPassageiroDAO.java`
- **Linha(s):** 13-25
- **Problema:** Alem do placeholder faltante (#035), o metodo NUNCA chama `ps.setInt(6, DAOUtils.empresaId())`. Registros inseridos terao empresa_id NULL.
- **Impacto:** Quebra multi-tenant — tipos de passageiro sem empresa_id.
- **Fix sugerido:**
```java
ps.setInt(6, DAOUtils.empresaId());
```
- **Observacoes:**
> _Issue nova encontrada na contra-verificacao (NOVO-L03). Relacionada ao #035._

---

#### Issue #059 — EncomendaDAO.excluir: deleta itens sem verificar tenant antes do commit
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/EncomendaDAO.java`
- **Linha(s):** 176-198
- **Problema:** Deleta encomenda_itens sem verificar empresa_id da encomenda pai. Commit acontece na linha 190 ANTES de verificar rows > 0. Se id nao pertence ao tenant, itens de outra empresa sao deletados e commitados.
- **Impacto:** Possivel exclusao de itens de encomenda cross-tenant.
- **Fix sugerido:** Mover check de rows ANTES do commit; se 0, fazer ROLLBACK.
- **Observacoes:**
> _Issue nova encontrada na contra-verificacao (NOVO-L04)._

---

### 2.4 — Resiliencia e Error Handling

#### Issue #060 — SyncClient.garantirAutenticacao: token JWT nunca validado (expiracao ignorada)
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** 256-261
- **Problema:** `garantirAutenticacao()` apenas verifica se `jwtToken` e non-null e non-empty. Nunca verifica se o token esta expirado. Um token de 8h atras sera considerado valido, e todas as requests de sync falharao com 401 repetidamente.
- **Impacto:** Sync automatica falha silenciosamente por horas apos a expiracao do token.
- **Codigo problematico:**
```java
private boolean garantirAutenticacao() {
    if (jwtToken != null && !jwtToken.isEmpty()) {
        return true;  // Token pode estar expirado!
    }
    return autenticar();
}
```
- **Fix sugerido:**
```java
private boolean garantirAutenticacao() {
    if (jwtToken != null && !jwtToken.isEmpty()) {
        // Salvar timestamp de obtencao e comparar com 7.5h (margem sobre 8h validade)
        if (System.currentTimeMillis() - tokenObtidoEm > 7.5 * 3600 * 1000) {
            jwtToken = "";
            return autenticar();
        }
        return true;
    }
    return autenticar();
}
```
- **Observacoes:**
> _Review: Fix corrigido — Desktop nao tem parser JWT nas dependencias. Mais simples: salvar timestamp de obtencao do token e comparar com 7.5h._

---

#### Issue #061 — SyncClient.sincronizarTudo: re-autenticacao apos 401 nunca tentada
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** 342-387
- **Problema:** Se `sincronizarTabela()` falha com 401, o SyncClient nao tenta re-autenticar. Apenas registra o erro e continua para a proxima tabela (que tambem falhara). Todas as 11 tabelas falham em cascata.
- **Impacto:** Uma sync inteira falha quando o token expira. O usuario ve 11 erros em vez de uma re-autenticacao automatica.
- **Codigo problematico:**
```java
for (String tabela : TABELAS_SYNC) {
    try {
        SyncResult resultado = sincronizarTabela(tabela).get(60, TimeUnit.SECONDS);
    } catch (Exception e) {
        resultadoGeral.erros.add(tabela + ": " + e.getMessage());
    }
}
```
- **Fix sugerido:** No catch, verificar se erro contem "401", chamar autenticar(), retry tabela atual. Nao reiniciar todo o loop.
- **Observacoes:**
> _Review: Fix corrigido — token pode expirar DURANTE sync. Retry apenas a tabela atual apos re-autenticacao._

---

#### Issue #062 — SyncClient.aplicarRegistroRecebido: cada registro abre e fecha conexao (N+1)
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** 557-577
- **Problema:** Para cada registro recebido, `aplicarRegistroRecebido` abre uma nova conexao. Para 500 registros, sao 500 conexoes abertas e fechadas sequencialmente.
- **Impacto:** Sync lenta, pressao no connection pool.
- **Codigo problematico:**
```java
private void aplicarRegistroRecebido(String tabela, Map<String, Object> registro) {
    try (java.sql.Connection conn = dao.ConexaoBD.getConnection()) {
        // Um registro por conexao
    }
}
```
- **Fix sugerido:** Abrir uma unica conexao no `sincronizarTabela()` e passar como parametro.
- **Observacoes:**
> _Review: Severidade ajustada de MEDIO para BAIXO — pool gerencia conexoes; obter/devolver e barato._

---

#### Issue #063 — SyncClient.sincronizarTabela: CompletableFuture dentro de CompletableFuture (thread starvation)
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** 342-387, 393-441
- **Problema:** `sincronizarTudo()` roda em `CompletableFuture.supplyAsync()` e dentro chama `sincronizarTabela()` que tambem usa `CompletableFuture.supplyAsync()`. O `.get(60, TimeUnit.SECONDS)` bloqueia a thread do pool comum.
- **Impacto:** Em maquinas com poucos cores, a sync pode travar por deadlock no ForkJoinPool.
- **Codigo problematico:**
```java
for (String tabela : TABELAS_SYNC) {
    SyncResult resultado = sincronizarTabela(tabela).get(60, TimeUnit.SECONDS);
    // Bloqueia thread do ForkJoinPool esperando outra task no mesmo pool
}
```
- **Fix sugerido:** Executar `sincronizarTabela` sincronamente dentro de `sincronizarTudo`, ja que o loop e sequencial.

---

#### Issue #064 — SyncClient: credenciais de operador salvas em texto plano no disco
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** 156-161
- **Problema:** `operador.senha` e salva em `sync_config.properties` como texto plano. Qualquer pessoa com acesso ao disco le as credenciais.
- **Impacto:** Credenciais do operador expostas.
- **Codigo problematico:**
```java
props.setProperty("operador.login", login != null ? login : "");
props.setProperty("operador.senha", senha != null ? senha : "");
```
- **Fix sugerido:** Nao salvar a senha no disco. Armazenar apenas o JWT (que expira). Se expirar, pedir senha ao usuario novamente. Ou encriptar com chave derivada.

---

#### Issue #065 — Desktop DAOs: erros de SQL logados apenas como warn, sem stacktrace
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/*.java` (todos os DAOs)
- **Linha(s):** Multiplos
- **Problema:** Todos os DAOs capturam `SQLException` e logam apenas `e.getMessage()`. O stacktrace completo e perdido.
- **Impacto:** Debugging em producao extremamente dificil.
- **Codigo problematico:**
```java
catch (SQLException e) {
    AppLogger.warn("ViagemDAO", "Erro SQL em ViagemDAO: " + e.getMessage());
}
```
- **Fix sugerido:**
```java
catch (SQLException e) {
    AppLogger.error("ViagemDAO", "Erro SQL em ViagemDAO: " + e.getMessage(), e);
}
```

---

#### Issue #066 — EstornoPagamentoController: PreparedStatement leak em carregarFormasPagamento
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/EstornoPagamentoController.java`
- **Linha(s):** 76-80
- **Problema:** O `PreparedStatement` criado por `con.prepareStatement("SELECT ...")` nao e fechado. O ResultSet esta no try-with-resources, mas o PreparedStatement intermediario nao.
- **Impacto:** Vazamento de Statement handles. Em uso intenso, pode atingir limites de cursores.
- **Codigo problematico:**
```java
try (Connection con = ConexaoBD.getConnection();
     ResultSet rs = con.prepareStatement("SELECT ...").executeQuery()) {
    // PreparedStatement intermediario nunca e fechado
```
- **Fix sugerido:**
```java
try (Connection con = ConexaoBD.getConnection();
     PreparedStatement stmt = con.prepareStatement("SELECT ...");
     ResultSet rs = stmt.executeQuery()) {
```

---

#### Issue #067 — SyncClient.enviarComRetry: retry nao diferencia erros recuperaveis de permanentes
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** ~404-406
- **Problema:** O metodo `enviarComRetry` faz ate 3 retentativas para qualquer erro HTTP. Erros como 400, 403, 422 sao irrecuperaveis e nao deveriam ser retentados.
- **Impacto:** Delays desnecessarios (3x 2s = 6s) em erros que nunca serao resolvidos por retry.
- **Fix sugerido:** Verificar HTTP status code. So retentar para >= 500 ou IOException/SocketTimeoutException.

---

#### Issue #068 — Web BFF db.js: DB_PASSWORD sem fallback — falha silenciosa
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-web/server/db.js`
- **Linha(s):** 9
- **Problema:** `password: process.env.DB_PASSWORD` sem fallback. Se a variavel nao estiver definida, `password` sera `undefined`.
- **Impacto:** Deploy em producao pode falhar com erro generico de autenticacao. Em producao, .env gerenciado por PM2/systemd.
- **Codigo problematico:**
```javascript
password: process.env.DB_PASSWORD,
```
- **Fix sugerido:**
```javascript
password: process.env.DB_PASSWORD || (() => { console.error('FATAL: DB_PASSWORD nao configurado'); process.exit(1); })(),
```
- **Observacoes:**
> _Review: Severidade ajustada de MEDIO para BAIXO — em producao, .env gerenciado por PM2. Dev local pode usar trust auth._

---

#### Issue #069 — SyncClient: ResultSet nao fechado em buscarRegistrosPendentes
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** 459-496
- **Problema:** O `ResultSet rs = stmt.executeQuery()` nao esta em try-with-resources. Se uma excecao ocorrer durante o processamento, o ResultSet nao sera fechado.
- **Impacto:** Vazamento de cursores do PostgreSQL em caso de erro.
- **Codigo problematico:**
```java
java.sql.ResultSet rs = stmt.executeQuery();  // Nao fechado explicitamente
ResultSetMetaData meta = rs.getMetaData();
while (rs.next()) { ... }
```
- **Fix sugerido:**
```java
try (java.sql.ResultSet rs = stmt.executeQuery()) {
    ResultSetMetaData meta = rs.getMetaData();
    while (rs.next()) { ... }
}
```

---

#### Issue #070 — SyncClient: scheduler pode agendar multiplas tasks se iniciarSyncAutomatica chamado N vezes
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** 297-315
- **Problema:** `iniciarSyncAutomatica()` nao verifica se ja existe uma task agendada. Se chamado duas vezes, duas tasks de sync rodam em paralelo.
- **Impacto:** Sync duplicada, carga dobrada no servidor, possivel corrupcao de dados.
- **Codigo problematico:**
```java
public void iniciarSyncAutomatica() {
    if (!autoSyncEnabled) return;
    if (scheduler.isShutdown()) scheduler = criarScheduler();
    // Nenhuma verificacao se ja tem task agendada
    scheduler.scheduleAtFixedRate(() -> { ... }, 1, syncIntervalMinutes, TimeUnit.MINUTES);
}
```
- **Fix sugerido:** Manter referencia ao `ScheduledFuture<?>` e cancelar antes de agendar nova task.

---

#### Issue #071 — Web BFF estornos.js: parseFloat para calculos financeiros de estorno
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/estornos.js`
- **Linha(s):** 53-62
- **Problema:** Calculos de estorno usam `parseFloat` e aritmetica JavaScript. Erros de IEEE 754 podem causar `valor_devedor = 0.009999999...` em vez de `0`.
- **Impacto:** Passagem pode ficar como 'PENDENTE' quando deveria ser 'PAGO' apos estorno total.
- **Codigo problematico:**
```javascript
const novoValorPago = Math.max(0, valorPago - parseFloat(valor))
const novoValorDevedor = parseFloat(passagem.valor_a_pagar || passagem.valor_total || 0) - novoValorPago
```
- **Fix sugerido:** Fazer a aritmetica no SQL do PostgreSQL (que usa NUMERIC com precisao exata) ou usar biblioteca de precisao.

---

#### Issue #072 — BalancoViagemDAO: saidas SQL erro silencioso nao propagado
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/dao/BalancoViagemDAO.java`
- **Linha(s):** 118-131
- **Problema:** Se a query de saidas falhar com SQLException, `dados.marcarIncompleto()` NAO e chamado (diferente de passagens/encomendas/fretes). O balanco retorna com saidas = 0, parecendo correto.
- **Impacto:** Balanco com total de saidas = 0 quando houve erro de query, mostrando lucro inflado.
- **Codigo problematico:**
```java
// Passagens/Encomendas/Fretes fazem:
} catch (SQLException e) {
    dados.marcarIncompleto("Passagens", e.getMessage());
}
// Saidas NAO fazem — erro vai para o catch geral
```
- **Fix sugerido:**
```java
} catch (SQLException e) {
    AppLogger.warn("BalancoViagemDAO", "Erro SQL Saidas: " + e.getMessage());
    dados.marcarIncompleto("Saidas", e.getMessage());
}
```

---

#### Issue #073 — ConexaoBD: DriverManager.setLoginTimeout e global (race condition)
- [ ] **Concluido**
- **Severidade:** INFO
- **Arquivo:** `src/dao/ConexaoBD.java`
- **Linha(s):** 123
- **Problema:** `DriverManager.setLoginTimeout(5)` chamado em cada `getConnection()`. Metodo estatico e global — redundante e desnecessariamente chamado a cada conexao.
- **Impacto:** Baixo — valor e sempre 5 e chamado dentro de synchronized. Nenhuma race condition real.
- **Codigo problematico:**
```java
DriverManager.setLoginTimeout(5);
```
- **Fix sugerido:** Mover para o bloco `static {}` initializer.
- **Observacoes:**
> _Review: Severidade ajustada de BAIXO para INFO — valor e sempre 5, chamado dentro de synchronized._

---

#### Issue #074 — Web BFF boleto single (POST /api/financeiro/boleto) insere boleto + agenda sem transacao
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/financeiro.js`
- **Linha(s):** 227-260
- **Problema:** Insere o boleto no `financeiro_saidas` e depois na `agenda_anotacoes` em duas queries separadas sem transacao.
- **Impacto:** Boleto criado sem entrada na agenda.
- **Codigo problematico:**
```javascript
const result = await pool.query(`INSERT INTO financeiro_saidas ...`)
await pool.query('INSERT INTO agenda_anotacoes ...') // Se falhar, boleto ja existe
```
- **Fix sugerido:** Usar client com BEGIN/COMMIT.

---

#### Issue #075 — Web BFF encomendas.js DELETE: itens deletados sem verificar empresa_id
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/encomendas.js`
- **Linha(s):** 165-185
- **Problema:** DELETE de encomenda_itens feito pelo `id_encomenda` sem verificar que a encomenda pertence ao tenant. Itens deletados ANTES do DELETE da encomenda (que filtra por empresa_id).
- **Impacto:** Possivel exclusao de itens de encomenda de outra empresa.
- **Codigo problematico:**
```javascript
await client.query('DELETE FROM encomenda_itens WHERE id_encomenda = $1', [req.params.id])
const result = await client.query(
  'DELETE FROM encomendas WHERE id_encomenda = $1 AND empresa_id = $2 ...', ...)
```
- **Fix sugerido:**
```javascript
await client.query(
  'DELETE FROM encomenda_itens WHERE id_encomenda IN (SELECT id_encomenda FROM encomendas WHERE id_encomenda = $1 AND empresa_id = $2)',
  [req.params.id, empresaId]
)
```
- **Observacoes:**
> _Review: Fix corrigido — mover check ANTES do commit; se 0, fazer ROLLBACK._

---

#### Issue #076 — SyncClient: senha enviada em JSON sem HTTPS enforcement
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** 222-224
- **Problema:** Credenciais enviadas em JSON via HTTP POST. O `serverUrl` pode ser `http://`. Nao ha verificacao de que a URL usa HTTPS.
- **Impacto:** Credenciais interceptaveis por man-in-the-middle. Em producao, serverUrl aponta para api.naviera.com.br com SSL via Nginx.
- **Codigo problematico:**
```java
String jsonBody = "{\"login\":\"" + escapeJson(login)
    + "\",\"senha\":\"" + escapeJson(senha) + "\"}";
// serverUrl pode ser http://
```
- **Fix sugerido:** Validar que `serverUrl` comeca com `https://` ou alertar o usuario.
- **Observacoes:**
> _Review: Severidade ajustada de ALTO para MEDIO — risco real apenas se alguem configurar http:// manualmente._

---

#### Issue #077 — EncomendaDAO.excluir: commit executa mesmo quando encomenda nao pertence ao tenant
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/EncomendaDAO.java`
- **Linha(s):** 176-198
- **Problema:** `conn.commit()` na linha 190 executa SEMPRE, mesmo quando DELETE da encomenda retorna 0 rows. Itens de encomenda de outro tenant ja foram deletados e commitados.
- **Impacto:** Dados de outro tenant podem ser excluidos irreversivelmente.
- **Fix sugerido:** Mover check de rows ANTES do commit; se 0, fazer ROLLBACK.
- **Observacoes:**
> _Issue nova encontrada na contra-verificacao (NOVO-R01). Relacionada ao #059._

---

#### Issue #078 — Web BFF passagens.js: race condition em numero_bilhete
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/passagens.js`
- **Linha(s):** 115-119
- **Problema:** SELECT MAX nao protegido por lock. Duas requests concorrentes geram bilhetes duplicados. Desktop usa nextval() (sem problema), web usa MAX+1 (com problema).
- **Impacto:** Bilhetes duplicados em operacao simultanea.
- **Fix sugerido:**
```javascript
const seqResult = await client.query("SELECT nextval('seq_numero_bilhete') AS next_num")
```
- **Observacoes:**
> _Issue nova encontrada na contra-verificacao (NOVO-R02)._

---

### 2.5 — Performance

#### Issue #079 — N+1 queries no mapResultSetToPassagem (Desktop PassagemDAO)
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/dao/PassagemDAO.java`
- **Linha(s):** 238-242
- **Problema:** Dentro do `mapResultSetToPassagem()`, que e chamado para CADA linha, sao feitas 5 chamadas a `buscarNomeAuxiliarPorId()`. No cold-start, para 500 passagens, sao 2500 queries. Pre-carregamento mitiga mas `listarExtratoPorPassageiro` NAO faz o pre-carregamento.
- **Impacto:** Cold-start lento de 5-15 segundos ao abrir telas de passagens pela primeira vez.
- **Codigo problematico:**
```java
p.setAcomodacao(auxiliaresDAO.buscarNomeAuxiliarPorId("aux_acomodacoes", "nome_acomodacao", "id_acomodacao", p.getIdAcomodacao()));
p.setTipoPassagemAux(auxiliaresDAO.buscarNomeAuxiliarPorId("aux_tipos_passagem", "nome_tipo_passagem", "id_tipo_passagem", p.getIdTipoPassagem()));
p.setAgenteAux(auxiliaresDAO.buscarNomeAuxiliarPorId("aux_agentes", "nome_agente", "id_agente", p.getIdAgente()));
p.setFormaPagamento(auxiliaresDAO.buscarNomeAuxiliarPorId("aux_formas_pagamento", "nome_forma_pagamento", "id_forma_pagamento", p.getIdFormaPagamento()));
p.setCaixa(auxiliaresDAO.buscarNomeAuxiliarPorId("caixas", "nome_caixa", "id_caixa", p.getIdCaixa()));
```
- **Fix sugerido:**
```java
// Em listarExtratoPorPassageiro E filtrarRelatorio, adicionar pre-carregamento:
public List<Passagem> listarExtratoPorPassageiro(String nomePassageiro, String status) {
    List<Passagem> lista = new ArrayList<>();
    try { auxiliaresDAO.preCarregarCachesPassagem(); } catch (SQLException e) { /* cache opcional */ }
    // ... resto do metodo
```
- **Observacoes:**
> _Review: Fix incompleto no original — filtrarRelatorio (linha 278) TAMBEM nao chama preCarregarCachesPassagem(). Adicionar em ambos._

---

#### Issue #080 — BFF: queries sem LIMIT retornam datasets ilimitados
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/encomendas.js`
- **Linha(s):** 10-26
- **Problema:** O endpoint `GET /api/encomendas` nao aplica LIMIT. Se uma empresa tiver 50.000 encomendas, o BFF carrega tudo em memoria. Mesmo problema em `fretes.js`, `financeiro.js` saidas, e `admin.js`.
- **Impacto:** Uso excessivo de memoria; timeout; possivel OOM kill.
- **Codigo problematico:**
```javascript
let sql = 'SELECT * FROM encomendas WHERE empresa_id = $1'
sql += ' ORDER BY id_encomenda DESC'
// Sem LIMIT
```
- **Fix sugerido:**
```javascript
const page = parseInt(req.query.page) || 1
const limit = Math.min(parseInt(req.query.limit) || 100, 500)
const offset = (page - 1) * limit
sql += ` LIMIT ${limit} OFFSET ${offset}`
```

---

#### Issue #081 — BFF: SELECT * em 15+ endpoints
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/cadastros.js`, `encomendas.js`, `fretes.js`, `financeiro.js`
- **Linha(s):** Multiplas
- **Problema:** 17 endpoints usam `SELECT *` em vez de especificar colunas necessarias. Tabelas com 25+ colunas enviam dados desnecessarios ao frontend.
- **Impacto:** Payloads JSON 2-3x maiores que necessario; expoe colunas internas ao frontend.
- **Codigo problematico:**
```javascript
let sql = 'SELECT * FROM encomendas WHERE empresa_id = $1'
```
- **Fix sugerido:**
```javascript
let sql = `SELECT id_encomenda, numero_encomenda, remetente, destinatario,
           total_volumes, total_a_pagar, valor_pago, status_pagamento, entregue,
           data_lancamento FROM encomendas WHERE empresa_id = $1`
```

---

#### Issue #082 — Admin: N+1 subqueries correlacionadas para metricas
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/admin.js`
- **Linha(s):** 32-48, 178-224
- **Problema:** O endpoint `GET /api/admin/empresas` executa 4 subqueries correlacionadas para CADA empresa. Com 50 empresas, gera 200 queries.
- **Impacto:** Painel admin fica lento com crescimento do numero de empresas.
- **Codigo problematico:**
```javascript
const result = await pool.query(`
  SELECT e.*,
    (SELECT COUNT(*) FROM usuarios u WHERE u.empresa_id = e.id ...) AS total_usuarios,
    (SELECT COUNT(*) FROM passagens p WHERE p.empresa_id = e.id) AS total_passagens,
    (SELECT COUNT(*) FROM encomendas en WHERE en.empresa_id = e.id) AS total_encomendas,
    (SELECT COUNT(*) FROM fretes f WHERE f.empresa_id = e.id) AS total_fretes
  FROM empresas e
`)
```
- **Fix sugerido:**
```javascript
const result = await pool.query(`
  SELECT e.*, COALESCE(u.cnt, 0) AS total_usuarios, COALESCE(p.cnt, 0) AS total_passagens,
    COALESCE(en.cnt, 0) AS total_encomendas, COALESCE(f.cnt, 0) AS total_fretes
  FROM empresas e
  LEFT JOIN (SELECT empresa_id, COUNT(*) AS cnt FROM usuarios WHERE excluido = FALSE OR excluido IS NULL GROUP BY empresa_id) u ON u.empresa_id = e.id
  LEFT JOIN (SELECT empresa_id, COUNT(*) AS cnt FROM passagens GROUP BY empresa_id) p ON p.empresa_id = e.id
  LEFT JOIN (SELECT empresa_id, COUNT(*) AS cnt FROM encomendas GROUP BY empresa_id) en ON en.empresa_id = e.id
  LEFT JOIN (SELECT empresa_id, COUNT(*) AS cnt FROM fretes GROUP BY empresa_id) f ON f.empresa_id = e.id
  ORDER BY e.nome
`)
```

---

#### Issue #083 — BFF: boleto batch insere parcelas sequencialmente sem transaction
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/financeiro.js`
- **Linha(s):** 263-307
- **Problema:** O endpoint insere parcelas em loop sequencial com `await pool.query()` sem transacao. Divergencia com Desktop que usa batch atomico.
- **Impacto:** Dados inconsistentes (parcelas parciais) em caso de erro.
- **Codigo problematico:**
```javascript
for (let i = 0; i < parcelas; i++) {
  const result = await pool.query(`INSERT INTO financeiro_saidas ...`, [...])
  boletos.push(result.rows[0])
  await pool.query('INSERT INTO agenda_anotacoes ...', [...])
}
```
- **Fix sugerido:**
```javascript
const client = await pool.connect()
try {
  await client.query('BEGIN')
  for (let i = 0; i < parcelas; i++) {
    const result = await client.query(`INSERT INTO financeiro_saidas ...`, [...])
    boletos.push(result.rows[0])
    await client.query('INSERT INTO agenda_anotacoes ...', [...])
  }
  await client.query('COMMIT')
} catch (err) {
  await client.query('ROLLBACK')
  throw err
} finally {
  client.release()
}
```

---

#### Issue #084 — BFF: race condition no numero_bilhete via MAX+1
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/passagens.js`
- **Linha(s):** 115-119
- **Problema:** O numero do bilhete e gerado com `MAX(numero_bilhete::bigint) + 1`. Desktop usa `nextval('seq_numero_bilhete')` (sequence), mas o BFF nao usa a mesma sequence.
- **Impacto:** Numeros de bilhete duplicados em operacao simultanea.
- **Codigo problematico:**
```javascript
const seqResult = await client.query(
  'SELECT COALESCE(MAX(numero_bilhete::bigint), 0) + 1 AS next_num FROM passagens WHERE empresa_id = $1',
  [empresaId]
)
```
- **Fix sugerido:**
```javascript
const seqResult = await client.query("SELECT nextval('seq_numero_bilhete') AS next_num")
```

---

#### Issue #085 — BFF: race condition no id_frete via MAX+1
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/fretes.js`
- **Linha(s):** 63-73
- **Problema:** Tanto `id_frete` quanto `numero_frete` sao gerados via `MAX(...) + 1` dentro da transacao, mas a query nao usa `FOR UPDATE` nem sequence.
- **Impacto:** INSERT falha com unique constraint violation; frete perdido.
- **Codigo problematico:**
```javascript
const idResult = await client.query(
  'SELECT COALESCE(MAX(id_frete), 0) + 1 AS next_id FROM fretes WHERE empresa_id = $1',
  [empresaId]
)
const seqResult = await client.query(
  'SELECT COALESCE(MAX(numero_frete), 0) + 1 AS next_num FROM fretes WHERE empresa_id = $1',
  [empresaId]
)
```
- **Fix sugerido:**
```javascript
// Usar sequence real: seq_numero_frete (confirmado em 010_criar_sequence_frete.sql)
const seqResult = await client.query("SELECT nextval('seq_numero_frete') AS next_id")
```
- **Observacoes:**
> _Review: Fix corrigido — sequence real e seq_numero_frete (nao seq_frete)._

---

#### Issue #086 — Desktop: ViagemDAO.listarTodasViagensResumido sem LIMIT
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/dao/ViagemDAO.java`
- **Linha(s):** 272-297
- **Problema:** Carrega TODAS as viagens de uma empresa sem LIMIT. 5 anos com 2 viagens/dia = ~3650 viagens com 3 JOINs.
- **Impacto:** ~100KB total. Lentidao minima com indice composto.
- **Codigo problematico:**
```java
"WHERE v.empresa_id = ? " +
"ORDER BY v.data_viagem DESC, ahs.descricao_horario_saida DESC";
// Sem LIMIT
```
- **Fix sugerido:**
```java
"WHERE v.empresa_id = ? " +
"ORDER BY v.data_viagem DESC, ahs.descricao_horario_saida DESC LIMIT 200";
```
- **Observacoes:**
> _Review: Severidade ajustada de MEDIO para BAIXO — ~3650 viagens com JOINs leves e indice composto nao causa problema real._

---

#### Issue #087 — React Web: sem React.lazy nem code splitting
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/src/App.jsx`, `naviera-web/src/components/Layout.jsx`
- **Linha(s):** N/A
- **Problema:** As 33 paginas do frontend web sao importadas estaticamente. Nao ha `React.lazy()` nem `Suspense`. O bundle inteiro e carregado no login.
- **Impacto:** Tempo de carregamento inicial alto; desperdicio de banda em conexoes lentas.
- **Codigo problematico:**
```jsx
import Passagens from './pages/Passagens.jsx'
import Encomendas from './pages/Encomendas.jsx'
// ... 31 mais imports
```
- **Fix sugerido:**
```jsx
const Passagens = React.lazy(() => import('./pages/Passagens.jsx'))
const AdminEmpresas = React.lazy(() => import('./pages/AdminEmpresas.jsx'))
// Envolver em <Suspense fallback={<Loading />}>
```

---

#### Issue #088 — Desktop: filtrarRelatorio faz pos-filtragem em Java
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/dao/PassagemDAO.java`
- **Linha(s):** 323-342
- **Problema:** Carrega TODAS as passagens e depois aplica 5 filtros em Java via `removeIf()`. Esses filtros poderiam ser SQL JOINs.
- **Impacto:** Transferencia de dados desnecessaria. Volume ja reduzido pelos filtros SQL. Filtros pos-Java sao para campos resolvidos pelo AuxiliaresDAO (com cache).
- **Codigo problematico:**
```java
if (agente != null && !agente.trim().isEmpty()) {
    passagens.removeIf(p -> p.getAgenteAux() == null || !p.getAgenteAux().equalsIgnoreCase(agente));
}
```
- **Fix sugerido:**
```java
if (agente != null && !agente.trim().isEmpty()) {
    conditions.add("ag.nome_agente ILIKE ?");
    params.add(agente);
}
```
- **Observacoes:**
> _Review: Severidade ajustada de MEDIO para BAIXO — filtros sao para campos com cache, volume ja reduzido._

---

#### Issue #089 — Desktop: cache da viagem ativa nao e thread-safe
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/ViagemDAO.java`
- **Linha(s):** 155-156, 158-182
- **Problema:** `cacheViagemAtiva` e um campo static sem sincronizacao. Thread de sync e JavaFX Application Thread podem ler/escrever simultaneamente.
- **Impacto:** Em cenarios raros, viagem ativa cache pode retornar dados parcialmente inicializados.
- **Codigo problematico:**
```java
private static Viagem cacheViagemAtiva = null;
public static void invalidarCacheViagem() { cacheViagemAtiva = null; }
```
- **Fix sugerido:**
```java
private static volatile Viagem cacheViagemAtiva = null;
public static void invalidarCacheViagem() { cacheViagemAtiva = null; }
```

---

#### Issue #090 — PassagemDAO.temDataChegada e campo de instancia compartilhado
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/PassagemDAO.java`
- **Linha(s):** 183, 228, 255-256
- **Problema:** O campo `temDataChegada` e setado antes do loop de resultados. Se duas threads usarem a mesma instancia de PassagemDAO, uma pode sobrescrever o valor.
- **Impacto:** Dados de data_chegada incorretamente omitidos em cenarios concorrentes.
- **Codigo problematico:**
```java
private boolean temDataChegada = false;

temDataChegada = detectarTemDataChegada(rs);
while (rs.next()) passagens.add(mapResultSetToPassagem(rs));
```
- **Fix sugerido:**
```java
private Passagem mapResultSetToPassagem(ResultSet rs, boolean temDataChegada) throws SQLException {
```

---

#### Issue #091 — Site institucional: 757 linhas sem code splitting
- [ ] **Concluido**
- **Severidade:** INFO
- **Arquivo:** `naviera-site/src/App.jsx`
- **Linha(s):** 1-757
- **Problema:** Site inteiro em um unico arquivo de 757 linhas. Sem code splitting.
- **Impacto:** FCP ~200-500ms maior que necessario. Bundle de ~30-50KB gzipped. Vite ja faz tree-shaking. Impacto < 50ms.
- **Codigo problematico:**
```jsx
function HomePage({ go }) { /* ... */ }
function EmpresasPage({ go }) { /* ... */ }
// ... mais 4 paginas + 10 componentes auxiliares
```
- **Fix sugerido:** Extrair cada pagina para arquivo separado e usar React.lazy(). Prioridade baixa.
- **Observacoes:**
> _Review: Severidade ajustada de BAIXO para INFO — bundle minusculo, sem rota dinamica._

---

#### Issue #092 — filtrarRelatorio tambem nao pre-carrega caches
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/PassagemDAO.java`
- **Linha(s):** 278-344
- **Problema:** Issue #079 so menciona `listarExtratoPorPassageiro`, mas `filtrarRelatorio` tambem chama `mapResultSetToPassagem` sem `preCarregarCachesPassagem()`. Relatorios com muitos registros amplificam N+1.
- **Impacto:** Relatorios lentos na primeira execucao.
- **Fix sugerido:**
```java
public List<Passagem> filtrarRelatorio(...) {
    try { auxiliaresDAO.preCarregarCachesPassagem(); } catch (SQLException e) { /* cache opcional */ }
    // ... resto do metodo
}
```
- **Observacoes:**
> _Issue nova encontrada na contra-verificacao (PERF-NEW-001)._

---

#### Issue #093 — DespesaDAO.buscarBoletos sem LIMIT
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/dao/DespesaDAO.java`
- **Linha(s):** 343-388
- **Problema:** Retorna todos os boletos sem LIMIT. Boletos acumulam com o tempo.
- **Impacto:** Lentidao crescente ao longo dos anos de operacao.
- **Fix sugerido:** Adicionar LIMIT ou paginacao.
- **Observacoes:**
> _Issue nova encontrada na contra-verificacao (PERF-NEW-002)._

---

### 2.6 — Manutenibilidade

#### Issue #094 — CadastroFreteController: 2253 linhas, arquivo gigante
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/CadastroFreteController.java`
- **Linha(s):** 1-2253
- **Problema:** Controller monolitico com 2253 linhas que mistura: logica de UI, logica de negocios, SQL inline, impressao, OCR/voz.
- **Impacto:** Extremamente dificil de manter; alto risco de regressao.
- **Codigo problematico:**
```java
import net.sourceforge.tess4j.ITesseract;
import org.vosk.Model;
// ... + 70 outros imports
public class CadastroFreteController implements Initializable {
    // 2253 linhas
}
```
- **Fix sugerido:**
```
Extrair em 4-5 classes:
1. FreteFormController — UI e binding
2. FreteCalculoService — logica de calculo
3. FreteAutocompleteHandler — ContextMenu + sugestoes
4. FreteImpressaoService — geracao de recibo
5. FreteOCRService — OCR e voz
```

---

#### Issue #095 — VenderPassagemController: 1822 linhas, mesmo padrao
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/VenderPassagemController.java`
- **Linha(s):** 1-1822
- **Problema:** Segundo maior controller com mesma mistura de responsabilidades. Comentarios com encoding corrompido.
- **Impacto:** Dificil de manter e testar.
- **Codigo problematico:**
```java
// <<< MÃ‰TODOS DE AÃ‡ÃƒO >>>
// <<< OUTROS MÃ‰TODOS AUXILIARES >>>
```
- **Fix sugerido:**
```
1. Corrigir encoding dos comentarios
2. Extrair logica de calculo de tarifa para TarifaCalculoService
3. Extrair impressao de bilhete para BilheteImpressaoService
4. Alvo: controller < 500 linhas
```

---

#### Issue #096 — RelatorioFretesController: 1769 linhas
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/RelatorioFretesController.java`
- **Linha(s):** 1-1769
- **Problema:** Terceiro arquivo com 1700+ linhas. Controller de relatorios com logica de layout inline, queries SQL, calculo de totais.
- **Impacto:** Manutencao custosa; duplicacao provavel de logica de impressao.
- **Fix sugerido:** Extrair template de relatorio para classe separada; compartilhar servico de impressao.

---

#### Issue #097 — InserirEncomendaController: 1717 linhas
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/InserirEncomendaController.java`
- **Linha(s):** 1-1717
- **Problema:** Quarto controller com 1700+ linhas. Padrao sistemico de controllers monoliticos.
- **Impacto:** Padrao sistemico que torna todo o Desktop dificil de manter.
- **Codigo problematico:**
```
Resumo dos controllers acima de 500 linhas:
CadastroFreteController    2253
VenderPassagemController   1822
RelatorioFretesController  1769
InserirEncomendaController 1717
TelaPrincipalController    1464
ListaEncomendaController    979
SetupWizardController       936
...
```
- **Fix sugerido:** Priorizar os 4 maiores para refatoracao. Criar camada de Services Java.

---

#### Issue #098 — Financeiro.jsx: 692 linhas, componente monolitico React
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/src/pages/Financeiro.jsx`
- **Linha(s):** 1-692
- **Problema:** Componente React com 692 linhas com 5 tabs, 2 modals, 4 funcoes de export CSV, e toda a logica de state/effects.
- **Impacto:** Dificil de manter; re-renders desnecessarios.
- **Codigo problematico:**
```jsx
// 14 useState, 4 useCallback, 2 useEffect, 5 tabs, 2 modals
export default function Financeiro({ viagemAtiva }) {
  const [balanco, setBalanco] = useState(null)
  // ... 13 mais useState
```
- **Fix sugerido:**
```jsx
import FinanceiroResumo from './financeiro/FinanceiroResumo'
import FinanceiroPassagens from './financeiro/FinanceiroPassagens'
import FinanceiroSaidas from './financeiro/FinanceiroSaidas'
```

---

#### Issue #099 — Passagens.jsx: 581 linhas, inline styles excessivos
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/src/pages/Passagens.jsx`
- **Linha(s):** 1-581
- **Problema:** Componente de 581 linhas com estilos inline extensivos. Autocomplete construido manualmente com divs e event handlers.
- **Impacto:** Duplicacao de logica de autocomplete; estilos inline dificultam temas.
- **Codigo problematico:**
```jsx
<div style={{
  position: 'absolute', top: '100%', left: 0, right: 0,
  background: 'var(--bg-card)', border: '1px solid var(--border)',
  borderRadius: 6, maxHeight: 200, overflowY: 'auto',
  zIndex: 100, boxShadow: '0 4px 12px rgba(0,0,0,0.3)'
}}>
```
- **Fix sugerido:**
```jsx
<Autocomplete
  value={form.nome_passageiro}
  onSearch={handleNomeChange}
  onSelect={selecionarPassageiro}
  suggestions={sugestoes}
  loading={buscando}
/>
```

---

#### Issue #100 — Web frontend: sem Error Boundaries
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/src/App.jsx`
- **Linha(s):** 1-46
- **Problema:** Nenhum componente React usa Error Boundaries. Uma excecao em qualquer componente derruba a app inteira, mostrando tela branca.
- **Impacto:** Em producao, qualquer erro de JavaScript derruba toda a interface.
- **Codigo problematico:**
```jsx
export default function App() {
  return (
    <AuthContext.Provider value={{ usuario, logout, theme, toggleTheme }}>
      <Layout />
    </AuthContext.Provider>
  )
}
```
- **Fix sugerido:**
```jsx
import { ErrorBoundary } from 'react-error-boundary'

<ErrorBoundary FallbackComponent={ErrorFallback}>
  <Layout />
</ErrorBoundary>
```

---

#### Issue #101 — Duplicacao Desktop vs Web: logica de calculo de status
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/dao/PassagemDAO.java` vs `naviera-web/server/routes/passagens.js`
- **Linha(s):** PassagemDAO 376-404 vs passagens.js 121-124
- **Problema:** Logica de determinar status (PAGO/PENDENTE/PARCIAL) e implementada independentemente no Desktop e no BFF. Thresholds sao iguais (0.01) atualmente, mas duplicacao em 3+ locais torna qualquer mudanca futura fragil.
- **Impacto:** Fragilidade alta — qualquer mudanca futura exige achar todos os locais. Risco de divergencia.
- **Codigo problematico:**
```javascript
// BFF (passagens.js:124)
const status = vDevedor <= 0.01 ? 'PAGO' : 'PENDENTE'
```
```java
// Desktop (PassagemDAO.java:307-308)
if (statusPagamento.equals("Falta Pagar")) conditions.add("p.valor_devedor > 0.01");
```
- **Fix sugerido:** Centralizar constante de tolerancia. Idealmente, mover logica de status para o banco (trigger ou computed column).
- **Observacoes:**
> _Review: Severidade ajustada de MEDIO para ALTO — thresholds SAO iguais, mas duplicacao em 3+ locais e fragilidade alta._

---

#### Issue #102 — Duplicacao Desktop vs Web: geracao de numero sequencial
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/PassagemDAO.java` vs `naviera-web/server/routes/passagens.js`
- **Linha(s):** PassagemDAO 24-41 vs passagens.js 115-119
- **Problema:** Desktop usa `nextval('seq_numero_bilhete')` (thread-safe). BFF usa `MAX(numero_bilhete::bigint) + 1` (race condition). Numeros podem colidir se ambos operam simultaneamente.
- **Impacto:** Numeros duplicados quando Desktop e Web criam passagens ao mesmo tempo.
- **Codigo problematico:**
```java
// Desktop: seguro
String sql = "SELECT nextval('seq_numero_bilhete')";
```
```javascript
// Web: inseguro
'SELECT COALESCE(MAX(numero_bilhete::bigint), 0) + 1 AS next_num FROM passagens WHERE empresa_id = $1'
```
- **Fix sugerido:**
```javascript
const seqResult = await client.query("SELECT nextval('seq_numero_bilhete') AS next_num")
```
- **Observacoes:**
> _Review: Fix corrigido — sequence real e seq_numero_bilhete (nao seq_bilhete)._

---

#### Issue #103 — BFF cadastros.js: 519 linhas com 30+ endpoints repetitivos
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/cadastros.js`
- **Linha(s):** 1-519
- **Problema:** 30+ endpoints CRUD seguem exatamente o mesmo padrao. Nao ha abstraccao ou factory.
- **Impacto:** Adicionar novo cadastro exige copiar/colar ~60 linhas; bugs nao sao corrigidos nos outros.
- **Codigo problematico:**
```javascript
router.post('/conferentes', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { nome } = req.body
    if (!nome) return res.status(400).json({ error: 'nome obrigatorio' })
    const result = await pool.query(
      'INSERT INTO conferentes (nome_conferente, empresa_id) VALUES ($1, $2) RETURNING *',
      [nome, empresaId]
    )
    res.status(201).json(result.rows[0])
  } catch (err) { ... }
})
// ... identico para caixas, clientes-encomenda, etc.
```
- **Fix sugerido:**
```javascript
function simpleCrud(tableName, pkColumn, nameColumn) {
  const r = Router()
  r.get('/', async (req, res) => { /* ... */ })
  r.post('/', async (req, res) => { /* ... */ })
  r.put('/:id', async (req, res) => { /* ... */ })
  return r
}
router.use('/conferentes', simpleCrud('conferentes', 'id_conferente', 'nome_conferente'))
```

---

#### Issue #104 — PassagemDAO: metodo @deprecated ainda em uso
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/PassagemDAO.java`
- **Linha(s):** 406-442
- **Problema:** O metodo `quitarDividaTotalPassageiro(String nomePassageiro)` esta @deprecated mas ainda e chamado por `ExtratoPassageiroController.java:429`. Quitacao por nome com risco de homonimos esta ativa em producao.
- **Impacto:** Risco de homonimos — dois passageiros com mesmo nome tem dividas quitadas juntos.
- **Codigo problematico:**
```java
/** @deprecated Use quitarDividaTotalPassageiroPorId(long) para evitar homonimos. */
public boolean quitarDividaTotalPassageiro(String nomePassageiro) {
```
- **Fix sugerido:** Verificar todos os callers; migrar para metodo por ID; remover metodo deprecated.
- **Observacoes:**
> _Review: Severidade ajustada de BAIXO para MEDIO — metodo deprecated chamado em ExtratoPassageiroController.java:429. Quitacao por nome com risco de homonimos esta ativa._

---

#### Issue #105 — Desktop: encoding corrompido em multiplos arquivos
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/gui/CadastroFreteController.java`, `src/gui/VenderPassagemController.java`
- **Linha(s):** CadastroFreteController:78, VenderPassagemController:594+
- **Problema:** Comentarios em portugues com caracteres acentuados corrompidos (UTF-8 interpretado como Latin-1).
- **Impacto:** Legibilidade prejudicada; nao afeta execucao.
- **Codigo problematico:**
```java
* VERSÃƒO CORRIGIDA: MÃ©todo PUBLIC e EdiÃ§Ã£o Direta.
// <<< MÃ‰TODOS DE AÃ‡ÃƒO >>>
```
- **Fix sugerido:** Converter arquivos para UTF-8 correto. Configurar Eclipse para salvar em UTF-8.

---

#### Issue #106 — DespesaDAO: retorna Map<String, Object> em vez de DTO
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/DespesaDAO.java`
- **Linha(s):** 26-92, 343-388
- **Problema:** Metodos retornam `List<Map<String, Object>>` em vez de objetos tipados. Perde type-safety.
- **Impacto:** Bugs silenciosos quando colunas mudam; nao aparece em IDE autocomplete.
- **Codigo problematico:**
```java
public List<Map<String, Object>> buscarDespesas(...) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("id", rs.getInt("id"));
    row.put("descricao", rs.getString("descricao"));
}
```
- **Fix sugerido:**
```java
public record DespesaResumo(int id, LocalDate dataVencimento, String descricao,
    String categoriaNome, String formaPagamento, BigDecimal valorTotal,
    String status, boolean excluido) {}

public List<DespesaResumo> buscarDespesas(...) { /* ... */ }
```

---

#### Issue #107 — Site institucional: CSS inline em template strings
- [ ] **Concluido**
- **Severidade:** INFO
- **Arquivo:** `naviera-site/src/App.jsx`
- **Linha(s):** Multiplas
- **Problema:** CSS inline via `style={{}}` em todos os componentes. Impossivel usar media queries ou pseudo-selectors em inline styles.
- **Impacto:** Dificil de manter; impossivel pseudo-selectors (:hover, :focus). Site funcional, deploy estatico.
- **Codigo problematico:**
```jsx
<div style={{width:52,height:52,borderRadius:14,background:'var(--icon-bg)',
  display:'flex',alignItems:'center',justifyContent:'center',marginBottom:20}}>
```
- **Fix sugerido:** Extrair para App.css com classes semanticas. Prioridade baixa.
- **Observacoes:**
> _Review: Severidade ajustada de BAIXO para INFO — site de 7 paginas com visitacao minima. CSS inline razoavel para prototipos._

---

#### Issue #108 — Convencao de retorno de erro inconsistente nos DAOs
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/dao/PassagemDAO.java`, `src/dao/EncomendaDAO.java`
- **Linha(s):** PassagemDAO:17-19
- **Problema:** Alguns DAOs retornam -1 para erro, outros null, outros false, outros lancam excecao. Nao ha padrao consistente.
- **Impacto:** Callers devem saber a convencao de cada DAO; risco de nao tratar erro.
- **Codigo problematico:**
```java
// PassagemDAO: retorna 1 (fallback)
public int obterProximoBilhete() { return 1; }
// EncomendaDAO: retorna null
public Encomenda inserir(Encomenda encomenda) { return null; }
```
- **Fix sugerido:** Adotar convencao uniforme: Optional<T> para buscas, boolean para operacoes, RuntimeException para erros.

---

#### Issue #109 — TelaPrincipalController: 1464 linhas, ponto de entrada do Desktop
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/TelaPrincipalController.java`
- **Linha(s):** 1-1464
- **Problema:** Controller da tela principal com 1464 linhas. Ponto de entrada onde novas features tendem a ser adicionadas.
- **Impacto:** Acumulacao continua de codigo.
- **Fix sugerido:** Extrair em modulos: MenuController, StatusBarController, ViagemAtivaController.

---

#### Issue #110 — Duplicacao de formatMoney em 20 arquivos (nao 2)
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** 20 arquivos em `naviera-web/src/pages/` + `print.js`
- **Problema:** Funcao `formatMoney` identica duplicada em 20 arquivos. Ja existe `utils/export.js` — deveria estar em `utils/format.js`.
- **Impacto:** Se formato de moeda mudar, precisa alterar em 20 arquivos. Muito pior que os 2 alegados no scan original.
- **Codigo problematico:**
```jsx
// Em 20 arquivos:
function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}
```
- **Fix sugerido:**
```javascript
// Em utils/format.js (compartilhado):
export function formatMoney(val) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0)
}
```
- **Observacoes:**
> _Review: Severidade ajustada de BAIXO para MEDIO — duplicado em 20 arquivos, nao 2. Issue MAINT-NEW-001 confirma escopo real._

---

#### Issue #111 — TabelasAuxiliaresController: 683 linhas de CRUD repetitivo
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/TabelasAuxiliaresController.java`
- **Linha(s):** 1-683
- **Problema:** CRUD identico para 7 tipos de tabelas auxiliares. Cada bloco tem ~75 linhas com o mesmo padrao.
- **Impacto:** Adicionar nova tabela auxiliar exige copiar ~75 linhas.
- **Codigo problematico:**
```java
// --- METODOS SEXO ---          (linha 151)
// --- METODOS TIPO DOC ---      (linha 227)
// --- METODOS NACIONALIDADE --- (linha 303)
// ... 7 blocos identicos de ~75 linhas cada
```
- **Fix sugerido:**
```java
private void configurarAbaCRUD(Tab tab, String tabela, String colunaNome, String colunaId) {
    // Implementacao generica unica
}
```

---

#### Issue #112 — criarFrete.js helper propaga bug MAX+1
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-web/server/helpers/criarFrete.js`
- **Linha(s):** 20-29
- **Problema:** Logica MAX+1 extraida para helper, mas bug de race condition se propagou. Fix deve ser no helper, nao na route.
- **Impacto:** Race condition herdada do codigo original.
- **Fix sugerido:** Aplicar advisory lock ou usar sequence no helper.
- **Observacoes:**
> _Issue nova encontrada na contra-verificacao (MAINT-NEW-002)._

---

## CONTRA-VERIFICACAO

### Falsos Positivos Descartados (4)

| Cat | Issue | Titulo | Motivo |
|-----|-------|--------|--------|
| 1 | #009 | DELETE viagens cascade | Operacao idempotente — DELETEs em transacao nao afetam nada se viagem nao existe |
| 2 | #006 | CSRF desabilitado | Pratica padrao para API REST stateless com JWT em header |
| 2 | #017 | JwtFilter nao seta empresa_id para CPF/CNPJ | Intencional — endpoints mobile sao cross-tenant |
| 5 | #006 | Indices sem empresa_id | JA RESOLVIDO no script 013_multi_tenant.sql |

### Severidades Ajustadas (29)

29 issues tiveram severidade ajustada. As mudancas mais significativas:
- 2 CRITICOs rebaixados para BAIXO/ALTO (senhas locais de dev)
- 3 MEDIOs rebaixados para BAIXO (caches Desktop single-tenant)
- 5 BAIXOs rebaixados para INFO (praticas aceitaveis no contexto)
- 2 BAIXOs elevados para MEDIO (escopo real maior que alegado)
- 1 MEDIO elevado para ALTO (duplicacao de logica de status)

### Fixes Corrigidos (16)

16 fixes tiveram erros ou imprecisoes corrigidas. Os mais importantes:
- FOR UPDATE nao funciona com MAX() — corrigido para pg_advisory_xact_lock
- Sequences referenciadas com nomes errados — corrigidos para nomes reais
- Fix em arquivo errado (auth.js vs tenant.js) — corrigido
- Desktop nao tem parser JWT — fix simplificado para comparacao de timestamp

### Duplicado Removido (1)

Cat 5 #002 (DespesaDAO.buscarDespesas sem empresa_id) era identico ao Cat 3 #009. Removido da categoria Performance.

### Novos Problemas Encontrados (14)

14 issues adicionais encontradas durante a contra-verificacao, incluindo 2 CRITICOs e 3 ALTOs.

### Pontos Cegos Declarados

1. **naviera-api (Spring Boot)** — cobertura parcial (107 arquivos Java, nem todos lidos linha por linha)
2. **database_scripts/** — scripts 001-005, 007-012, 014-015 nao verificados
3. **naviera-app/** — cobertura superficial (validacao de input nao testada)
4. **Testes (src/tests/, naviera-api/src/test/)** — cobertura de testes nao auditada
5. **FXML views** — nao auditados
6. **JasperReports (relatorios/)** — nao verificados
7. **Docker/CI/CD** — nenhuma configuracao auditada

---

## PLANO DE CORRECAO

### Sprint 1 — Criticos (fazer AGORA)

| # | Issue | Descricao | Esforco |
|---|-------|-----------|---------|
| 1 | #030 | TarifaDAO.buscarTarifaPorRotaETipo parametros errados | 15min |
| 2 | #031 | PassageiroDAO.inserir falta placeholder | 5min |
| 3 | #032, #033 | PassageiroDAO.listarTodos/NomesPassageiros executeQuery sem setInt | 15min |
| 4 | #034 | PassageiroDAO.buscarPorNome parametro na posicao errada | 10min |
| 5 | #035, #058 | TipoPassageiroDAO.inserir falta placeholder + empresa_id | 10min |
| 6 | #036 | TipoPassageiroDAO.listarTodos sem empresa_id | 10min |
| 7 | #037 | TarifaDAO.listarTodos sem empresa_id | 10min |
| 8 | #040 | AgendaDAO.adicionarAnotacao sem executeUpdate | 5min |
| 9 | #043, #056 | ItemEncomendaPadraoDAO duplo WHERE + executeQuery sem setInt | 15min |

### Sprint 2 — Altos (esta semana)

| # | Issue | Descricao | Esforco |
|---|-------|-----------|---------|
| 1 | #004, #050, #083 | Boleto batch sem transacao (BFF) | 30min |
| 2 | #005, #048, #049, #084, #085 | Race condition MAX+1 (BFF) — usar sequences | 1h |
| 3 | #006 | fretes.js id_frete manual | 30min |
| 4 | #013 | sync_config.properties senha plaintext | 1h |
| 5 | #015 | Login sem tenant em producao | 15min |
| 6 | #016 | Admin aceita localhost em producao | 10min |
| 7 | #017 | WebSocket sem autenticacao | 2h |
| 8 | #028 | BFF sem trust proxy | 5min |
| 9 | #038 | DespesaDAO.buscarDespesas sem empresa_id | 15min |
| 10 | #039, #057 | ReciboAvulsoDAO parametros errados | 15min |
| 11 | #041 | AgendaDAO boletos sem empresa_id | 10min |
| 12 | #042 | DespesaDAO.excluirBoleto sem transacao | 20min |
| 13 | #045 | FuncionarioDAO categoria sem empresa_id | 15min |
| 14 | #052 | UsuarioDAO login sem empresa_id | 15min |
| 15 | #060, #061 | SyncClient token expiracao + re-auth | 1h |
| 16 | #064 | SyncClient credenciais plaintext | 1h |
| 17 | #078 | BFF passagens.js race condition | 15min |
| 18 | #079, #092 | PassagemDAO N+1 queries | 30min |
| 19 | #080 | BFF queries sem LIMIT | 1h |
| 20 | #094-#097, #101 | Controllers monoliticos + duplicacao status | Backlog |

### Sprint 3 — Medios (este mes)

Issues #001, #003, #007, #008, #009, #011, #019, #020, #023, #024, #029, #044, #051, #059, #063, #065, #066, #067, #069, #070, #071, #074, #075, #076, #077, #081, #082, #087, #089, #090, #098, #099, #100, #102, #103, #104, #106, #109, #110, #111

### Backlog — Baixos e INFO

Issues #002, #010, #012, #014, #018, #021, #022, #025, #026, #027, #046, #047, #053, #054, #055, #062, #068, #072, #073, #086, #088, #091, #093, #105, #107, #108, #112

---

## HISTORICO DE AUDITORIAS

| Versao | Local | Data | Issues | Observacoes |
|--------|-------|------|--------|-------------|
| V1.0 | archive | 2026-04-12 | ~80 | Primeiro scan completo |
| V1.1 | archive | 2026-04-13 | ~95 | Deep dives em 6 categorias |
| V1.2 | current | 2026-04-14 | 112 | Scan + contra-verificacao + 14 novas issues |

---

*Gerado por Claude Code — Revisao humana obrigatoria*

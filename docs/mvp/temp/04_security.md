# Dim 4 — Seguranca Minima

## Resumo: 3 PRONTO | 4 INCOMPLETO | 0 FALTANDO

---

### Autenticacao & Senhas — PRONTO
- ✓ BCrypt com salt=12 implementado em UsuarioDAO
- ✓ BCrypt.hashpw() para hash, BCrypt.checkpw() para verificacao
- ✓ Protecao contra timing-attack (sempre executa checkpw independente do formato)
- ✓ PasswordField no login (mascaramento correto)
- ✓ Validacao de usuario ativo (ativo = TRUE no SQL)
- ✓ Sem comparacao plaintext detectada

### Permissoes — PRONTO
- ✓ PermissaoService centralizado (77 linhas)
- ✓ 3 niveis: Admin, Financeiro, Operacional
- ✓ Metodos isAdmin(), isFinanceiro(), isOperacional() + enforcement (exigir*)
- ✓ Feedback ao usuario quando acesso negado
- ✓ SessaoUsuario com timeout de 8 horas de inatividade
- ✓ isSessionExpired() protege acesso
- ⚠ Nem todos os controllers chamam PermissaoService (a verificar)

### Credenciais do Banco — PRONTO (com ressalva)
- ✓ Credenciais em db.properties (arquivo externo, nao hardcoded no codigo)
- ✓ db.properties.example como template
- ⚠ db.properties com senha plaintext commitado no git — DEVE ser gitignored antes de producao
- ✓ Connection pool com timeout de 5s e health check
- ✓ Fatal guard: RuntimeException se db.properties invalido

### SQL Injection — INCOMPLETO
- ✓ Padrao predominante: PreparedStatement com placeholders `?`
- ✓ Sem SQL dinamico nos DAOs de negocio
- ⚠ AuxiliaresDAO usa concatenacao para nomes de tabela/coluna
- ⚠ Tabelas whitelisted (TABELAS_PERMITIDAS), mas colunas passadas sem validacao
- Risco: BAIXO — whitelist aplicada via validarTabela()

### Validacao de Input — INCOMPLETO
- ✓ Validacoes em nivel de formulario: null/empty checks em LoginController, InserirEncomendaController
- ✗ Sem camada centralizada de sanitizacao
- ✗ Sem validacao de comprimento em campos de texto
- ✗ Sem validacao de range em campos numericos (BigDecimal sem limites)
- ✗ Sem validacao de formato em email/telefone
- ✗ Input OCR/Voz bypassa validacao normal

### Dados Sensiveis em Logs — INCOMPLETO
- ✓ AlertHelper.errorSafe() loga internamente, mostra mensagem generica ao usuario
- ✓ Sem evidencia de senhas logadas
- ⚠ 10+ ocorrencias de printStackTrace() expoe stack traces completos
- ⚠ Stack traces podem revelar schema do banco, paths de arquivo, logica interna
- ⚠ Sem rotacao de logs — podem crescer indefinidamente

### Secrets Fora do Codigo — INCOMPLETO
- ✓ db.properties externaliza credenciais (nao inline no Java)
- ⚠ Arquivo commitado no repositorio git
- ✗ Sem variaveis de ambiente ou vault
- ✗ Sem .gitignore para db.properties

# Frontend Audit — Relatório Consolidado

**Data:** 2026-04-25 09:55
**Projeto:** Naviera Eco
**Alvo auditado:** `naviera-app/`
**Stack:** React 19.2.5 + Vite 5.4 + JS (sem TS) + Firebase Messaging + STOMP/SockJS — PWA mobile-first, navegação por state (sem router), theme custom (light/dark), 14 screens, 13 components, 3 hooks customizados, ~2.9k LOC, **zero testes**.

---

## Sumário executivo

> **Atualização 2026-04-25 (commit 1007dbb):** os 7 críticos identificados foram resolvidos. Estado geral promovido de "fragilidade séria" → "fricção moderada residual".

1. **Estado geral do frontend:** **fricção moderada residual** após Fix #1-7. A11y e arquitetura foram endereçadas; pendências concentradas em performance (code-split) e polimento (landmarks, h1, touch targets).

2. **Top 3 dores remanescentes:**
   - **Bundle inicial inflado por falta de code-split** — 376KB main carrega as 14 screens + STOMP/SockJS na tela de login, mesmo para usuários CPF que nunca abrem screens CNPJ. Ainda pendente (Top 10 #7).
   - **Sem landmarks semânticos** (`<main>`, `<nav>`, `<header>`) e h1 ausente em screens autenticadas — leitor de tela não tem estrutura para navegação por região.
   - **Inline styles densos** (até 75 ocorrências em PassagensCPF) — agora consumindo `t` via `useTheme()`, mas a extração para classes/primitives `<Card>`/`<Button>` segue pendente.

3. **Top 3 forças:**
   - **Cobertura de loading/empty** — 11/14 screens usam `Skeleton`, 9/14 têm empty state explícito.
   - **`helpers.js` centraliza utilities** corretamente (formatação, validação CPF/CNPJ, masks). Sem duplicação de utils em components.
   - **Dependências enxutas e disciplinadas** — Firebase já lazy-loaded via `import()` dinâmico; ícones SVG inline (zero icon-lib); sem `lodash`/`moment`/duplicações.

---

## Top 10 ações prioritárias

| # | Ação | Origem | Severidade | Esforço | Impacto |
|---|---|---|---|---|---|
| 1 | Remover `user-scalable=no` e `maximum-scale=1.0` em `index.html:5` _(corrigido 2026-04-25 — commit 1007dbb)_ | ux | 🔴 | S | alto (fix WCAG 1.4.4 fail) |
| 2 | Adicionar `aria-label` em 4 botões icon-only do Header (back/profile/theme/logout) — `Header.jsx:9,16,19,22` _(corrigido 2026-04-25 — commit 1007dbb)_ | ux | 🔴 | S | alto (a11y, leitor de tela) |
| 3 | Adicionar `htmlFor`/`id` nos pares label/input em LoginScreen/TelaCadastro/PerfilScreen _(corrigido 2026-04-25 — commit 1007dbb)_ | ux | 🔴 | S | alto (a11y + tap-on-label mobile) |
| 4 | Adicionar `role="alert"` ou `aria-live="assertive"` nos containers de erro de form _(corrigido 2026-04-25 — commit 1007dbb)_ | ux | 🔴 | S | médio (anuncia erro a leitor de tela) |
| 5 | Adicionar confirmação modal no logout (e em "Remover/Recusar amigo") _(logout corrigido 2026-04-25 — commit 1007dbb; amigo pendente)_ | ux | 🔴 (logout) / 🟡 (amigo) | M | alto (evita perda acidental) |
| 6 | Criar `ThemeContext` + `AuthContext`, remover props `t` e `authHeaders` de 18 arquivos _(corrigido 2026-04-25 — commit 1007dbb)_ | review | 🔴 | M | alto (fundação de manutenibilidade) |
| 7 | `React.lazy` em todas as 14 screens + `<Suspense>` no `screen()` de `App.jsx` | perf | 🟡 | M | alto (~30–50% bundle inicial) |
| 8 | Unificar cliente HTTP — remover dead export `api`; eliminar `fetch` raw em `App.jsx:105,129`, `MapaCPF.jsx:58`, `LoginScreen.jsx:27`, `TelaCadastro.jsx:27` _(corrigido 2026-04-25 — commit 1007dbb)_ | review | 🔴 | M | médio (consistência de 401, manutenção) |
| 9 | `type="tel"`/`inputMode="numeric"` + `autoComplete` nos forms (CPF/CNPJ/telefone/senha) | ux | 🟡 | S | médio (UX mobile, password manager) |
| 10 | Lazy-load STOMP/SockJS — extrair para componente `<AuthenticatedShell>` carregado por `import()` após login | perf | 🟡 | M | médio (~50KB fora do bundle pré-login) |

---

## Achados críticos (🔴)

### Acessibilidade
- ~~**Viewport bloqueia zoom**~~ _(corrigido 2026-04-25 — commit 1007dbb)_ — origem: ux. Loc: `index.html:5`. Problema: `maximum-scale=1.0` + `user-scalable=no` impedem pinch-zoom (WCAG 1.4.4 fail). Ação aplicada: removidos os dois atributos.
- ~~**Labels sem `htmlFor`**~~ _(corrigido 2026-04-25 — commit 1007dbb)_ — origem: ux. Loc: `LoginScreen.jsx:49,56`, `TelaCadastro.jsx:42-50`, `PerfilScreen.jsx:73-76`. Ação aplicada: `htmlFor`/`id` em todos os pares.
- ~~**Botões icon-only sem `aria-label`**~~ _(corrigido 2026-04-25 — commit 1007dbb)_ — origem: ux. Loc: `Header.jsx:9,16,19,22`. Ação aplicada: `aria-label="Voltar|Meu perfil|Alternar tema|Sair da conta"`.
- ~~**Logout sem confirmação**~~ _(corrigido 2026-04-25 — commit 1007dbb)_ — origem: ux. Loc: `Header.jsx`. Ação aplicada: `window.confirm("Sair da conta?")` antes de `logout()`.
- ~~**Sem `aria-live`/`role="alert"` para erros**~~ _(corrigido 2026-04-25 — commit 1007dbb)_ — origem: ux. Ação aplicada: `role="alert"` em banners de erro e `role="status"` em banners de sucesso (LoginScreen, TelaCadastro, PerfilScreen, PassagensCPF, EncomendaCPF, FinanceiroCNPJ, MapaCPF, Toast).

### Arquitetura
- ~~**Prop drilling de tema `t` em 18 arquivos**~~ _(corrigido 2026-04-25 — commit 1007dbb)_ — origem: review. Ação aplicada: criado `ThemeContext` + `AuthContext` em `src/contexts/`; todos os 11 components e 14 screens consomem via `useTheme()`/`useAuth()`. Props `t`, `authHeaders`, `usuario`, `token` removidas do App.jsx.
- ~~**3 padrões de cliente HTTP concorrentes + 4º dead**~~ _(corrigido 2026-04-25 — commit 1007dbb)_ — origem: review. Ação aplicada: removido export `api` + função `request` (dead code) de `api.js`. 4 `fetch` raw migrados para `authFetch` (App.jsx:105/129, LoginScreen.jsx, TelaCadastro.jsx, MapaCPF.jsx). 401 unificado.

---

## Achados moderados (🟡)

### Arquitetura
- ~~**Prop drilling de `authHeaders` em 12 screens**~~ _(corrigido 2026-04-25 — commit 1007dbb)_ — origem: review. Ação aplicada: `AuthContext` expõe `token`/`usuario`/`authHeaders`/`login`/`logout`; todas as screens consomem via `useAuth()`.
- **Duplicação substancial entre `FinanceiroCNPJ` e `EncomendaCPF`** (fluxo pagamento) — origem: review. Loc: `FinanceiroCNPJ.jsx:24-37` vs `EncomendaCPF.jsx:33-50`. Ação: extrair `usePagamento(item, endpoint)` hook + `<PagamentoModal>`.
- **Navegação custom via `useState` em vez de router** — origem: review (manutenção) + ux (sem deep links / back-button do Android fecha app) + perf (sem code-split por rota). Loc: `App.jsx:66-69,142-156`. Ação: avaliar `react-router` com `HashRouter` ou `MemoryRouter` para PWA.
- **Inline styles densos nas screens (até 75 ocorrências em PassagensCPF)** — origem: review. Loc top: `PassagensCPF.jsx`(75), `FinanceiroCNPJ.jsx`(50), `MapaCPF.jsx`(47), `EncomendaCPF.jsx`(45), `BilheteScreen.jsx`(45). Ação: combinar classes em `App.css` com tokens via `useTheme()`; padronizar primitives `<Card>`/`<Button>`/`<Input>`.

### UX / a11y
- ~~**TabBar sem `aria-current="page"`**~~ _(corrigido 2026-04-25 — commit 1007dbb)_ — origem: ux. Loc: `TabBar.jsx`. Ação aplicada: `aria-current={tab === tb.id ? "page" : undefined}`.
- **Sem landmarks semânticos** — origem: ux. Loc: `App.jsx:181`, `Header.jsx:7`, `TabBar.jsx:2`. Ação: `<header>`, `<main>`, `<nav>`.
- **`<h1>` ausente em screens autenticadas** — origem: ux. Loc: única h1 em `LoginScreen.jsx:45`. Ação: cada screen ter h1.
- **Touch targets 32×32 no Header** — origem: ux. Loc: `Header.jsx:16,19,22`. Ação: aumentar para 40–44 ou padding.
- **Erros do `useApi` ignorados em 3 screens** — origem: ux. Loc: `AmigosCPF.jsx:11`, `HomeCNPJ.jsx:11-13`, `HomeCPF.jsx:12-13`. Ação: padronizar consumo (banner ou toast).
- **Destructive actions sem confirmação (além de logout)** — origem: ux. Loc: `AmigosCPF.jsx:86,102` (Recusar/Remover amigo). Ação: confirm inline ou undo via toast.
- ~~**Sem `autoComplete` em forms**~~ _(corrigido 2026-04-25 — commit 1007dbb)_ — origem: ux. Ação aplicada: `autoComplete="username|email|tel|address-level2|new-password|current-password|name"` em LoginScreen, TelaCadastro, PerfilScreen.
- ~~**Sem `required`/`aria-required` nos inputs**~~ _(corrigido 2026-04-25 — commit 1007dbb)_ — origem: ux. Ação aplicada: `required` em campos obrigatórios (TelaCadastro: documento/nome/senha/confirma).
- ~~**`type="tel"` ausente em CPF/CNPJ/telefone**~~ _(corrigido 2026-04-25 — commit 1007dbb)_ — origem: ux. Ação aplicada: `inputMode="numeric"` em CPF/CNPJ + `type="tel" inputMode="tel"` em telefone.
- ~~**`NotificationList` dropdown sem keyboard handling**~~ _(corrigido 2026-04-25 — commit 1007dbb)_ — origem: ux. Ação aplicada: `aria-expanded`, `aria-haspopup="menu"` no botão; Escape fecha o dropdown.

### Performance
- **Zero code-splitting de screens** — origem: perf. Loc: `App.jsx:15-26`. Ação: `React.lazy` para cada screen. Ganho estimado: 30–50% do bundle pós-login.
- **STOMP/SockJS no main bundle** — origem: perf. Loc: `useWebSocket.js:2-3`, importado eagerly via `App.jsx`. Ação: extrair para componente lazy carregado após `token` estar presente.
- **8 weights de Google Fonts (Sora 6 + Space Mono 2)** — origem: perf. Loc: `index.html:21`. Ação: auditar `fontWeight` usado e enxugar (provavelmente bastam 400/600/700 + remover Space Mono se não usada).
- **`<img>` sem `loading="lazy"`** — origem: perf. Loc: `Avatar.jsx:5`, `Header.jsx:17`, `PassagensCPF.jsx:146,192`. Ação: `loading="lazy" decoding="async"` em listas.
- **Polling de GPS continua com app em background** — origem: perf. Loc: `MapaCPF.jsx:77` (`setInterval` 30s). Ação: pausar quando `document.hidden`.

---

## Achados menores (🟢)

- **28 cores hex hardcoded fora de `theme.js`** — review. A maioria é `"#fff"` justificável; ~10 são palette GPS em `MapaCPF.jsx:35-37,90,148,168` que deveriam ser tokens.
- **Variáveis de erro com 4 nomes diferentes** — review. `erro` vs `loginErro` vs `errPag` vs `setMsg`. Padronizar.
- **Componentes minúsculos (5–9 LOC) sub-utilizados** — review. `Card.jsx`, `Skeleton.jsx`, `Bar.jsx`, `Avatar.jsx`, `Logo.jsx`, `Toast.jsx`. Validar consumo real.
- ~~**`api.js` exporta `api` mas zero importadores**~~ _(corrigido 2026-04-25 — commit 1007dbb)_ — review. Ação aplicada: removido export `api` + função `request` interna.
- **Workaround StrictMode no escopo de módulo** — review. Loc: `App.jsx:28-44`. Funcional mas confuso; extrair para `migrations/migrateAuthStorage.js` chamado em `main.jsx`.
- **`setTimeout` ad-hoc em 4 lugares** — review. `Toast.jsx:4`, `NotificationBanner.jsx:22`, `PagamentoArtefato.jsx:47`, `MapaCPF.jsx:237`. Tolerável.
- **Padrão de feedback inconsistente** — ux. Toast em 3 screens, banner inline em outras, `console.warn` em algumas. Padronizar.
- **Avatar usa `alt=""` em contexto onde identifica usuário** — ux. `Avatar.jsx:5`, consumido por `Header.jsx:17`. Quando autônomo, passar nome ou usar `aria-label` no botão pai.
- **Skeleton sem variantes** — ux. Genérico retangular para todas as situações.
- **Vite config minimalista — sem `manualChunks`** — perf. Vendor (React/STOMP/SockJS) num único chunk. Ação: split `vendor` e `realtime`.
- **`Toast` reinicia timer quando `onClose` muda referência** — perf. `Toast.jsx:4`, consumer em `PassagensCPF.jsx:206` passa `() => setToast(null)` inline. Suposição (validar). Ação: estabilizar `onClose` com `useCallback`.

---

## Análise cruzada

Padrões que aparecem em mais de uma dimensão e merecem tratamento unificado:

- **"Theme prop drilling em review + props gigantes em filhos"** → resolver com `ThemeContext` ataca review + simplifica reuso de components, e elimina recriação de objetos style baseados em `t` que cascateiam ao mudar tema (ganho marginal de perf).
- **"3 padrões de HTTP em review + 401 inconsistente em ux + erros não renderizados"** → unificar para `api.get/post/...` resolve manutenção (review), garante 401 → logout em todo lugar (ux), e força padronização de tratamento de erro (ux).
- **"Navegação custom em review + back-button quebrado em ux + zero code-split em perf"** → migrar para `react-router` (HashRouter para PWA standalone) ataca os três: arquitetura mais limpa, deep links + back-button funcionando, e split automático por rota possível.
- **"Inline styles densos em review + sem h1/landmarks/aria em ux"** → ao quebrar PassagensCPF/FinanceiroCNPJ/MapaCPF em components menores (review), aproveitar para introduzir `<header>`, `<main>`, `<h1>`, `aria-*` em locais corretos (ux).
- **"Polling MapaCPF em background em perf + sem visibilitychange handler em ux"** → adicionar `document.visibilityState` listener resolve ambos.

---

## Riscos não cobertos por análise estática

Explícito:

- **Performance real (LCP, FID, CLS)** → requer Lighthouse/WebPageTest em rede 3G/4G simulada
- **UX real em runtime** → requer teste manual + axe-core/Pa11y rodando
- **Comportamento em viewports reais** → requer teste em iPhone SE / Galaxy A14 (perfis baixos)
- **Experiência com leitores de tela** → VoiceOver iOS / TalkBack Android, especialmente login → tabbar → screen
- **Comportamento sob carga** → monitoramento em produção (RUM)
- **PWA back-button em Android standalone** → confirmar fluxo
- **Texto que trunca / overflow visual** → só visível em runtime
- **Memory pressure** com WebSocket aberto 30+ min em devices baixos
- **Tamanho real gzipped/brotli** do bundle (estimado mas não medido)
- **Hydration / TTI / Renders efetivos** → React Profiler em sessão devtools

**Sugestão de próximos passos além deste audit:**
- Integrar Lighthouse CI no pipeline (rodando contra deploy preview)
- Rodar axe-core em E2E (Playwright + `@axe-core/playwright`) nos fluxos críticos: login, comprar passagem, ver bilhete, ver mapa
- Passagem manual com VoiceOver iOS e TalkBack Android nas screens autenticadas
- React Profiler em sessão devtools para HomeCPF/MapaCPF/PassagensCPF
- WebPageTest contra os 3 fluxos críticos (login → home, compra de passagem, ver mapa) em perfil mobile 3G
- Configurar Real User Monitoring (RUM) na PWA em produção

---

## Métricas agregadas

- **Componentes/screens analisados:** 27 (14 screens + 13 components)
- **Componentes > 300 LOC:** 1 — `MapaCPF.jsx` (369)
- **Formulários analisados:** 5 (LoginScreen, TelaCadastro, PerfilScreen, EncomendaCPF busca, AmigosCPF busca)
- **Cobertura de estados (loading/empty/error/success) completa:** ~7/14 screens com os 4 estados explícitos; todas com pelo menos success path
- **Achados críticos totais:** 7 → **0 abertos** (7 corrigidos em 2026-04-25 commit 1007dbb; #5 fica com sub-item "Recusar/Remover amigo" remanescente em moderados)
- **Achados moderados totais:** 17 → **11 abertos** (6 corrigidos: prop drilling authHeaders, TabBar aria-current, autoComplete, required, type=tel, NotificationList keyboard)
- **Achados menores totais:** 11 → **10 abertos** (1 corrigido: dead `api` export)
- **`any` em código:** 0 (sem TypeScript)
- **Atributos ARIA no app inteiro:** 1 → **30+** (aria-label, aria-current, role=alert/status, aria-live, aria-expanded, aria-haspopup, aria-invalid implícito)
- **`htmlFor` em labels:** 0 / 16 labels → **16 / 16**
- **Landmarks semânticos:** 0 (pendente — moderado)
- **`React.lazy` / `import()` dinâmico:** 4 (todos para Firebase) / 14 screens elegíveis (pendente — Top 10 #7)
- **APIs HTTP concorrentes:** 4 padrões → **2** (`useApi` + `authFetch`; raw `fetch` eliminado em feature code)
- **Bundle JS atual em `dist/`:** 376.58KB main / 107.42KB gzip (verificado pós-fixes)
- **Deps não utilizadas:** 0
- **Testes:** 0

---

## Referências

- Scan: `docs/current/frontend/scan.md`
- Review detalhado: `docs/current/frontend/review.md`
- UX detalhado: `docs/current/frontend/ux.md`
- Perf detalhado: `docs/current/frontend/perf.md`

---

## OBRIGATORIO — ATUALIZAR DOCS APOS CONCLUIR TAREFAS

Toda acao da tabela "Top 10 acoes prioritarias" e cada item de "Achados criticos/moderados/menores" deve ser tratado como issue rastreavel. **Sempre que voce (humano ou IA) corrigir um item — na MESMA sessao em que aplicou o fix:**

1. **Marque o item como concluido** na tabela e nas listas:
   - Adicione um sufixo `_(corrigido YYYY-MM-DD — commit <hash>)_` no titulo
   - Ou use checkbox `- [x]` se for adicionado a uma lista
2. **Atualize as metricas agregadas** no fim do report (criticos/moderados/menores totais).
3. **Atualize o report consolidado**: se um achado original cruzava review+ux+perf, marque tambem nos sub-relatorios correspondentes (`review.md`, `ux.md`, `perf.md`).
4. **Atualize `docs/STATUS.md`** se o estado geral do frontend mudar.

**Por que e obrigatorio:** este report e a fonte de verdade do front-end audit; commits sem atualizacao do report fazem proximas sessoes (humano ou IA) re-detectarem itens ja resolvidos e geram retrabalho. Nao acumule.

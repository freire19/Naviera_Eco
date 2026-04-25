# Frontend Audit — Review (Arquitetura e Organização)

**Data:** 2026-04-25 09:35
**Alvo:** `naviera-app/`

## Resumo executivo

Frontend funcional e relativamente compacto (~2.9k LOC, 14 screens, 13 components, 3 hooks). **A dor arquitetural #1 é prop drilling severo de tema (`t`) e `authHeaders` em todas as 14 screens** — não há `Context` em parte alguma do app. **A dor #2 é a coexistência de 3 padrões concorrentes de chamada HTTP** (`useApi`, `authFetch`, `fetch` raw) com tratamento de erro divergente entre eles, mais um quarto padrão exposto (`api.get/post/...`) que está exportado mas nunca é importado. Pontos fortes: `helpers.js` centraliza utils corretamente; `theme.js` é uma fonte única de tokens (mas é entregue por prop); `api.js` mostra evidência de migração planejada (comentário "unified pattern same as naviera-web/naviera-ocr") que ficou pela metade.

## Achados por severidade

### 🔴 Crítico

- **Prop drilling de `t` (theme tokens) em todas as 14 screens e 5 components**
  - Localização: `src/App.jsx:142-156` (handoff), incidência: 18 arquivos com `t={t}` (15× em App.jsx, 20× em PassagensCPF, 13× em EncomendaCPF, 13× em FinanceiroCNPJ, 12× em HomeCPF, 10× em HomeCNPJ, …)
  - Descrição: `t` (objeto com ~30 design tokens) é passado como prop para toda screen e re-passado pra cada filho que estiliza algo. Zero `useContext`/`createContext` no projeto inteiro.
  - Impacto: cada nova screen/component obriga a passar `t` manualmente; mudar a forma do tema requer edição em 18 arquivos; testar componente isoladamente exige mockar `t`.
  - Sugestão: criar `ThemeContext` (provider em `App.jsx`) e `useTheme()` em `theme.js`; remover prop `t` em cascata.

- **3 padrões concorrentes de cliente HTTP, com 4º padrão dead-code**
  - Localização: `src/api.js:54-70` (api object não usado), `useApi` em 18 chamadas, `authFetch` em 11, `fetch` raw em 5 (ex: `App.jsx:94,118`, `MapaCPF.jsx:58`, `LoginScreen.jsx:27`, `TelaCadastro.jsx:27`)
  - Descrição: `api.js` exporta `api.get/post/put/delete` (abstração unificada com tratamento 401), mas nenhum arquivo importa `api`. Só `useApi`, `authFetch`, e `API` (URL constant) são consumidos. Resultado: tratamento de 401 está implementado 3 vezes (`request()` em api.js linha 37, `useApi` linha 93, `authFetch` linha 65) e ausente no `fetch` raw em `MapaCPF.jsx:58`.
  - Impacto: 401 em chamada via `MapaCPF` não derruba sessão; comportamento depende do caminho HTTP escolhido pelo dev. Migração para o pattern unificado nunca aconteceu — comentário `"unified pattern — same interface as naviera-web/naviera-ocr"` (api.js:53) sinaliza intenção sem follow-through.
  - Sugestão: completar migração para `api.get/post/...` ou remover. Remover `authFetch` e `fetch` raw após substituição.

### 🟡 Moderado

- **Prop drilling de `authHeaders` em 12 screens**
  - Localização: `App.jsx:141` cria `authHeaders` e passa para 11 screens (ex: `App.jsx:143-155`)
  - Descrição: `authHeaders` é derivável a partir de `token` em qualquer ponto da árvore. Hoje é construído uma vez em `App.jsx` e prop-drilled.
  - Impacto: igual a `t` — qualquer componente novo que precisa de auth herda a obrigação de receber e re-passar.
  - Sugestão: junto do `ThemeContext`, criar `AuthContext` expondo `token`, `usuario`, `headers`.

- **Duplicação substancial entre `FinanceiroCNPJ` e `EncomendaCPF` (fluxo de pagamento)**
  - Localização: `src/screens/FinanceiroCNPJ.jsx:24-37` vs `src/screens/EncomendaCPF.jsx:33-50`
  - Descrição: estado `pagando`, `enviando`, `metodo`, `errPag`, função `confirmarPagamento`, JSX de seleção de método e botão de confirmação são quase idênticos entre as duas screens. Diferenças marginais: parsing de erro mais robusto em EncomendaCPF, `console.warn` adicional. Comparação `diff` mostra ~5 linhas reais de diferença num bloco de ~30.
  - Impacto: bugs corrigidos em uma podem não chegar à outra (já há divergência: parsing de erro melhor em EncomendaCPF que em FinanceiroCNPJ).
  - Sugestão: extrair `usePagamento(item, endpoint)` hook + `<PagamentoModal>` reusable. `PagamentoArtefato.jsx` já existe como UI compartilhada — falta o lado lógico.

- **Navegação custom via `useState` em vez de `react-router`**
  - Localização: `src/App.jsx:66-69` (`tab`, `tabHistory`, `navigateTab`, `goBack`), `App.jsx:142-156` (switch manual)
  - Descrição: roteamento é um `useState` com pilha manual. Sem URL representando rota.
  - Impacto: links profundos impossíveis (não dá pra mandar URL de "minhas passagens" pra alguém); refresh do browser sempre volta pra `home`; back-button do Android/PWA fecha o app em vez de voltar tab; analytics não consegue distinguir páginas.
  - Sugestão: como app é mobile-first/PWA, considerar `react-router` 6+ com `HashRouter` (compatível com PWA standalone) ou `MemoryRouter` se URL não for desejada e manter pilha controlada. A decisão aqui é não-trivial — alinhar com produto.

- **`useApi` hook tem comportamento inconsistente com `authFetch` para 403**
  - Localização: `src/api.js:65` (authFetch) e `api.js:93` (useApi) — ambos só tratam 401. Mas a função `request()` (api.js:37) também só trata 401 com comentário `#DS5-225`. Consistente entre si — bom. Mas `MapaCPF.jsx:58` faz `fetch` direto e não trata nenhum status de erro.
  - Descrição: comportamento de 401 está padronizado em `useApi`/`authFetch`/`request`, mas `fetch` raw em `MapaCPF` e `App.jsx:94,118` ignora.
  - Impacto: token expirado em sessão olhando o mapa não derruba sessão.
  - Sugestão: ver achado crítico de unificação do client HTTP — esse cobre.

- **Inline styles densos em screens (até 75 ocorrências em PassagensCPF)**
  - Localização: top: `PassagensCPF.jsx` (75), `FinanceiroCNPJ.jsx` (50), `MapaCPF.jsx` (47), `EncomendaCPF.jsx` (45), `BilheteScreen.jsx` (45)
  - Descrição: `style={{...}}` com 5–15 propriedades repetido inline. `App.css` é o único stylesheet (não foi inspecionado em profundidade aqui, mas é único). Não há CSS Modules, nem styled-components, nem Tailwind.
  - Impacto: difícil de manter consistência visual; mudanças em padrões (ex: border-radius padrão dos cards) exigem grep+edit em N arquivos. Bundle inflado por strings de estilo.
  - Sugestão: combinar uso de classes em `App.css` com tokens vindos de `useTheme()`. Padronizar primitives — `<Card>`, `<Button>`, `<Input>` — atualmente `Card.jsx` tem 6 LOC (provavelmente subutilizado).

### 🟢 Menor

- **Cores hex hardcoded fora de `theme.js` (28 ocorrências)**
  - Localização: `src/screens/MapaCPF.jsx:35-37,90,148,168,359-360` (palette de status GPS), `LoginScreen.jsx:64`, `EncomendaCPF.jsx:113,165`, `FinanceiroCNPJ.jsx:104,163`, `BilheteScreen.jsx:173`, `AmigosCPF.jsx:77,85`, `TelaCadastro.jsx:52`, `NotificationBanner.jsx:101`, `NotificationList.jsx:60`, `PagamentoArtefato.jsx:60`
  - Descrição: mais comum é `color: "#fff"` para texto sobre `t.priGrad` (gradiente verde). Esses são aceitáveis — `t.tx` daria contraste errado sobre gradiente. Mas MapaCPF tem palette GPS própria (`#22C55E`, `#F59E0B`, `#9CA3AF`) que deveria viver em `theme.js` como tokens dedicados.
  - Sugestão: adicionar em `theme.js`: `gpsOk`, `gpsStale`, `gpsOff` e `onPrimary` para o branco-sobre-gradiente.

- **Vars de erro com 4 nomes diferentes para mesma intenção**
  - Localização: `erro` (PassagensCPF, MapaCPF, PerfilScreen, useApi), `loginErro` (LoginScreen), `errPag` (FinanceiroCNPJ, EncomendaCPF), `setMsg`/`msgSucesso` (LoginScreen)
  - Descrição: cada screen escolhe seu nome. Não viola convenção, mas leitor cruzando screens pensa que são coisas diferentes.
  - Sugestão: adotar `erro` (já é maioria) e prefixar quando há mais de um (`erroPag`, `erroLogin`).

- **`Card.jsx`, `Skeleton.jsx`, `Bar.jsx`, `Avatar.jsx` com 5–9 LOC sugerem under-utilização**
  - Localização: `src/components/Card.jsx` (6), `Skeleton.jsx` (5), `Bar.jsx` (5), `Avatar.jsx` (7), `Logo.jsx` (9), `Toast.jsx` (8)
  - Descrição: componentes muito pequenos (1–2 elementos) podem indicar wrappers triviais que não estão sendo usados onde poderiam (vide os 75 inline styles em PassagensCPF — provavelmente tem cards lá que poderiam ser `<Card>`).
  - Sugestão: validar consumo de cada um (`grep -c "import Card" src/`) e considerar engrossar API ou inlinar se quase nunca usados.

- **`api.js` exporta `api` (linha 54) mas zero importadores**
  - Localização: `src/api.js:53-59`
  - Descrição: dead export.
  - Sugestão: remover ou completar migração (ver crítico).

- **Workaround StrictMode para sessionStorage no escopo do módulo**
  - Localização: `src/App.jsx:28-44`
  - Descrição: bloco try fora de qualquer função/componente. Justificativa documentada (`#DS5-209`): React 19 StrictMode rerruna useState initializers; manter no escopo de módulo evita duplo-efeito.
  - Impacto: confunde leitor que não conhece o issue. Funciona, mas é frágil.
  - Sugestão: extrair pra `src/migrations/migrateAuthStorage.js` chamado uma única vez em `main.jsx` antes do `createRoot`. Mesmo efeito, código mais óbvio.

- **`PagamentoArtefato.jsx` tem `setTimeout` ad-hoc sem hook de timer reutilizável**
  - Localização: `src/components/PagamentoArtefato.jsx:47`, `Toast.jsx:4`, `NotificationBanner.jsx:22`, `MapaCPF.jsx:237`
  - Descrição: 4 lugares fazem `setTimeout` para auto-dismiss/refresh.
  - Sugestão: hook `useTimeout(fn, ms)` ou aceitar como duplicação tolerável (cada uso é único).

## Padrões de código gerado por IA detectados

- **Comentários óbvios:** AUSENTES. Comentários no código têm IDs de rastreamento (`#DS5-209`, `#DR280`, `#DR282`, `#DS5-225`, `#018`, `#DB156`) e descrevem constraints reais (race condition, sandbox sem storage, parsing de gateway timeout). Sinal positivo — não é geração automática descontextualizada.
- **Hooks reimplementados:** não detectado. Hooks customizados são únicos (`useNotifications`, `usePWA`, `useWebSocket`, `useApi`).
- **Utils duplicadas:** baixa duplicação. `helpers.js` centraliza `fmt`, `money`, `initials`, `maskCPF`, `maskCNPJ`, `validarDocumento`. Único caso fora: `MapaCPF.jsx:323` faz `toLocaleString` direto numa data — mas é format diferente (datetime, não date), aceitável.
- **Tipos repetidos:** N/A (sem TypeScript).
- **Naming drift em fns de fetch:** baixo — só 4 funções dedicadas (`fetchGps`, `getMeta`, `getToken`, `fetchTotp`), domínios distintos.
- **Error handling inconsistente:** **detectado** — feedbacks variados (toast em AmigosCPF, banner inline em PassagensCPF/PerfilScreen, console.warn em PerfilScreen:27 e App.jsx:121). Sem padrão.
- **API calls espalhados em componentes:** **detectado em parte** — `App.jsx:94,118`, `MapaCPF.jsx:58`, `LoginScreen.jsx:27`, `TelaCadastro.jsx:27` chamam `fetch` direto. Já coberto no crítico.
- **Exports inconsistentes:** consistente — 100% `export default` em screens/components.

## Métricas

- **Componentes/screens > 300 LOC:** 1 — `MapaCPF.jsx` (369 LOC)
- **`any` encontrados:** 0 (sem TypeScript)
- **Duplicações detectadas:**
  - 1 grande (FinanceiroCNPJ ↔ EncomendaCPF — fluxo pagamento)
  - 4 pequenas (`setTimeout` ad-hoc)
  - 0 utilitárias
- **Prop drilling ≥ 3 níveis:** 2 padrões massivos (`t`, `authHeaders`) — não 3 níveis isolados, mas todos os caminhos
- **Deps não utilizadas:** 0 — todas as 5 deps de produção e 2 de dev têm consumidores
- **Dead exports:** 1 (`api` em `api.js:54`)
- **APIs HTTP concorrentes:** 4 padrões (`api.get/post/...` dead, `useApi`, `authFetch`, `fetch` raw)
- **Inline `style={{...}}` no top-5 de screens:** 75/50/47/45/45 ocorrências
- **Cores hex fora de theme:** 28 (a maioria `"#fff"` justificado; ~10 deveriam virar tokens — palette GPS em `MapaCPF`)

## O que não foi auditado aqui

- Cobertura de estados de UI → ver `audit-3-front-ux`
- Performance → ver `audit-4-front-perf`
- Comportamento em runtime → fora do escopo de análise estática

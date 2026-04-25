# Frontend Audit — Performance (Estática)

**Data:** 2026-04-25 09:55
**Alvo:** `naviera-app/`

> ⚠️ **Análise 100% estática.** Esta auditoria NÃO mede performance real (LCP/FID/CLS). Detecta padrões que tipicamente causam performance ruim. Medição real exige Lighthouse / WebPageTest / profiling em produção.

## Resumo executivo

App tem fundamentos bons: ícones SVG inline (zero icon-lib), Firebase lazy-loaded via `import()` dinâmico, dependências enxutas (apenas React + Firebase + STOMP/SockJS no runtime). **Principais oportunidades de redução de bundle inicial: (1) zero code-splitting de screens — todas as 14 importadas eagerly em `App.jsx` mesmo quando o usuário só consome 1 dos 2 fluxos (CPF ou CNPJ); (2) `stompjs` + `sockjs-client` (~80KB combinados) carregados no main bundle quando poderiam ser lazy após login; (3) Google Fonts puxa 8 weights (Sora 6 + Space Mono 2) — provavelmente excessivo.** Build atual em `dist/` produz main JS de **365KB** (não-gzipped) — não-trivial para mobile primário.

## Dependências pesadas

| Lib | Contexto | Tamanho gzip estimado | Notas |
|---|---|---|---|
| `firebase` (12.12.0) | Apenas Messaging, importada dinâmica | ~50KB gzip (chunks `index.esm-*.js` em dist/) | ✅ **Lazy via `import()` em `useNotifications.js:37,38,85,109`** — chunk separa do main bundle |
| `@stomp/stompjs` (7.3.0) | WebSocket backend notifications | ~25KB gzip estimado | ❌ Top-level import em `useWebSocket.js:2` — vai pro main bundle |
| `sockjs-client` (1.6.1) | Fallback websocket transport | ~30KB gzip estimado | ❌ Top-level em `useWebSocket.js:3` (`/dist/sockjs` específico). Bundle inflado mesmo se browser suportar WebSocket nativo |
| `react` + `react-dom` (19.2.5) | Core | ~45KB gzip | OK — versão atual minimizada |

Não foram detectadas duplicações (`moment` + `date-fns`, `lodash` + `ramda`, etc.). Dep set é mínimo.

## Imports problemáticos

- `src/hooks/useWebSocket.js:2-3` — `import { Client } from "@stomp/stompjs"; import SockJS from "sockjs-client/dist/sockjs";`
  - Top-level. `useWebSocket` é invocado em `App.jsx:118`, ou seja, depois do login.
  - **Mas o módulo é importado no top-level de App.jsx** (transitivamente via `App.jsx:9`), então o código de WebSocket entra no bundle inicial **mesmo na tela de login**.
  - Sugestão: extrair WS para um componente lazy (`<AuthenticatedShell>`) que só é importado quando há `token`. Ou usar `import()` dinâmico no hook quando `token` estiver presente.

- `src/App.jsx:15-26` — 12 imports de screens em top-level
  - `LoginScreen`, `HomeCPF`, `AmigosCPF`, `MapaCPF`, `PassagensCPF`, `EncomendaCPF`, `HomeCNPJ`, `PedidosCNPJ`, `LojasParceiras`, `FinanceiroCNPJ`, `LojaCNPJ`, `PerfilScreen`.
  - Zero `React.lazy`, zero `import()` dinâmico.
  - Usuário CPF baixa as 5 screens CNPJ (e vice-versa). Antes mesmo de logar, baixa todas as 14.
  - Sugestão: `const HomeCPF = lazy(() => import("./screens/HomeCPF.jsx"))` para cada screen, com `<Suspense fallback={<Skeleton ... />}>` em torno do `screen()` em `App.jsx:184`. Ganho estimado: 30–50% do bundle pós-login.

## Imagens

- **Imagens em `public/`:** apenas ícones de PWA — `icon-192.png` (3.1KB), `icon-512.png` (8.7KB), `icon-512-maskable.png` (4KB), 2 SVGs source (~500B cada). **Zero arquivo > 200KB.** ✅
- **`<img>` no código:** 5 ocorrências
  - `src/components/Avatar.jsx:5` — sem `loading=` nem `width`/`height` literais (usa `style={{ width: size, height: size }}`)
  - `src/components/Header.jsx:17` — sem `loading=`
  - `src/components/PagamentoArtefato.jsx:60` — QR PIX (above-the-fold no contexto modal — `loading="eager"` aceitável)
  - `src/screens/PassagensCPF.jsx:146,192` — fotos de embarcação, sem `loading=`
- **Sem `loading="lazy"`** em 4/5 imagens. PassagensCPF lista embarcações com foto — listas longas perdem benefício do lazy load.
- **`width`/`height` literais ausentes** — todos passam via `style`. Browser ainda calcula CLS via style atribuído antes de pintar, então o impacto provável é baixo, mas não zero.
- Sugestão: `loading="lazy" decoding="async"` nas imagens de lista (PassagensCPF). Width/height numéricos onde o tamanho é fixo (Avatar, Header).

## Lazy loading

- **Code-split automático de rotas:** ❌ não aplicável — sem `react-router`, navegação por `useState` (`App.jsx:66-69`); sem framework com auto-split (não é Next/Remix).
- **Componentes pesados candidatos a lazy:**
  - 14 screens (ver "Imports problemáticos" acima)
  - `useWebSocket` (STOMP/SockJS)
  - `BilheteScreen` carrega TOTP rotativo + interval — pesado, deveria entrar só quando usuário clica num bilhete
- **`import()` dinâmicos existentes:** 4 — todos para `firebase/app` e `firebase/messaging` (`useNotifications.js:37,38,85,109`). Padrão já está no projeto, falta replicar.

## Configs de build

`vite.config.js` é mínimo:

```js
export default defineConfig({
  plugins: [react()],
  server: { port: 5173, open: true }
})
```

**Ausências notáveis (oportunidades não realizadas):**

- `build.rollupOptions.output.manualChunks` — sem split manual, vendor (React/STOMP/SockJS) fica todo num único chunk
- `build.target` — sem definir, Vite usa default `modules` (ESM2015). Para mobile-first em mercados com Android antigo, considerar `es2018`/`es2020` explícito
- `build.minify` — default `esbuild` (rápido, OK)
- `build.sourcemap` — não definido (default `false`). ✅ não vaza source em prod
- Sem plugin de compressão (gzip/brotli pré-compactado) — Nginx provavelmente faz on-the-fly
- Sem `VitePWA` — PWA é configurada à mão (`public/sw.js`, `manifest.json`). Trade-off aceito; manter manual evita lock-in mas perde features (precaching automatizado)

**Sugestões:**

- `manualChunks: { vendor: ['react', 'react-dom'], realtime: ['@stomp/stompjs', 'sockjs-client'] }` para que mudanças no código do app não invalidem cache de vendor
- Avaliar `vite-plugin-pwa` para precaching de rotas e estratégias de cache de API explícitas

Sem `tsconfig.json` (projeto JS).

## Renders desnecessários (suspeitas)

Análise estática conservadora — só apontar padrões claramente subótimos:

- **`App.jsx:160` — objeto `style` inline gigante recriado em toda render do componente raiz**
  - `<div style={{ minHeight: "100vh", background: t.bg, maxWidth: 420, ... }}>`
  - `t` muda quando tema é trocado — então `bg`, `tx` mudam de fato. Mas o objeto inteiro é recriado mesmo quando só o `tab` muda.
  - Suposição (validar com React Profiler): impacto baixo porque `t` é referência estável; a recriação do objeto força reconciliação do `<div>` mas não afeta filhos.

- **Padrão repetido em screens: `<Cd t={t} style={{ ... }}>`** — `<Cd>` é provavelmente o componente `Card` (6 LOC). Cada item de lista cria um novo objeto `style`. Não são memoizados. Em listas curtas (<50 itens) é tolerável.

- **`Toast.jsx:4` — `useEffect(() => { setTimeout(...) }, [onClose])`**
  - Se `onClose` for recriado a cada render do parent (que é o caso — passado como `() => setToast(null)` inline), o timer reinicia a cada render do parent.
  - Em `PassagensCPF.jsx:206`, `onClose={() => setToast(null)}` — recriado todo render.
  - Suposição: timer pode reiniciar várias vezes no ciclo de vida do toast antes de fechar. Validar comportamento real.
  - Sugestão: estabilizar `onClose` com `useCallback` no consumer, ou Toast usar ref.

- **`App.jsx:142-156` — função `screen()` redeclarada toda render** + grande switch
  - Não é otimizável de forma trivial (depende de `tab`, `t`, `authHeaders`, etc.). Aceitável.

- **`useApi` (`api.js:73-109`) abre AbortController a cada `rev`/`deps` mudança** — comportamento esperado. ✅

- **40 `onClick={() => ...}` inline em screens** — sem `React.memo` em filhos, então não causam re-render extra. Não-issue.

## Fontes

- **Carregamento:** `<link rel="stylesheet">` para Google Fonts em `index.html:21`, com `preconnect` para `fonts.googleapis.com` e `fonts.gstatic.com` (linhas 19-20). ✅
- **`display=swap`** presente. ✅ Evita FOIT (flash of invisible text).
- **Famílias e weights:**
  - **Sora**: 6 weights (300, 400, 500, 600, 700, 800)
  - **Space Mono**: 2 weights (400, 700)
  - **Total: 8 weights**
- **Estimativa:** ~12–18KB por weight (subset latin) → 96–144KB de fontes. Considerável para mobile 3G/4G.
- **Uso real:** sample de inline styles encontra `fontWeight: 600`, `700`, `800`. Não vi `300` nem `500` em uso ativo.
- Sugestão: auditar `grep -rE "fontWeight: [0-9]+" src/` e enxugar para os weights efetivamente usados (provavelmente 400, 600, 700). Se Space Mono não é usada (procurar `fontFamily: 'Space Mono'` — não vi nas amostras), remover por completo.

## CSS

- `App.css` tem 106 linhas — minimalista. Inline styles são o veículo principal (já apontado em review).
- **Sem Tailwind** → não há "purge". Volume de CSS em runtime é exatamente o que está em `App.css` + strings inline no JS.
- **`!important`** — não auditado em profundidade aqui (CSS é pequeno).
- **`outline: none`** global ausente. ✅

## Polling / Network

- `MapaCPF.jsx:10,77` — `setInterval(fetchGps, 30_000)` (a cada 30s)
  - Polling enquanto a tela está aberta. Sem `document.visibilityState` check — continua polling com app em background.
  - Sugestão: pausar quando `document.hidden`. Em mobile economiza bateria + dados.
- `BilheteScreen.jsx:32-33` — interval para timer de TOTP. Esperado para a feature.
- `useWebSocket` tem reconexão com backoff exponencial (max 30s). ✅
- `useApi` cria AbortController em cada effect — bom para cancelar fetch ao desmontar. ✅

## Service Worker / PWA

- `public/sw.js` (3KB no `dist/`), `firebase-messaging-sw.js` (2.5KB) — pequenos.
- Não auditei estratégia de cache em profundidade. Vale revisar se há cache stale-while-revalidate de API ou só de assets estáticos.

## Bundle real (build em `dist/`)

Existe build prévio em `dist/`. Tamanhos não-gzipped:

| Arquivo | Tamanho |
|---|---|
| `index-CqudNYgS.js` (main) | **365 KB** |
| `index.esm-Cd8r4XaR.js` (firebase chunk) | 44 KB |
| `index.esm-X7qdZUZE.js` (firebase chunk) | 40 KB |
| `index.esm-D3rEkRqD.js` (firebase chunk) | 1.4 KB |
| `index-cmak-0w6.css` | 2.1 KB |
| Total JS | **490 KB** (não-gzip) |

Gzip costuma reduzir JS para ~30%, então estimativa: **~150KB gzip total**, **~110KB gzip do main**. **NB:** isso é leitura de artefato — não é medição instrumentada.

## Requer validação em runtime

Tudo que só dá para saber rodando:

- **Tamanho real gzipped/brotli** (estimativa acima é aproximação)
- **LCP/FID/CLS reais** em conexões 3G/4G simuladas
- **Hydration / TTI** — quanto tempo do download do JS até o app responsivo
- **Comportamento sob StrictMode em produção** (StrictMode só roda em dev, mas comportamento dependente de doubleinvoke pode esconder bugs)
- **Renders efetivos em screens quentes** (HomeCPF, MapaCPF, PassagensCPF) — exige React Profiler
- **Cache hits do SW** — verificar se assets estão sendo servidos do cache em segunda visita
- **Throttling em rede 3G real** — Lighthouse simula, mas teste em dispositivo real difere
- **Memory pressure em dispositivos baixos** (Android 1–2GB RAM) com app rodando 30+ min com WebSocket aberto

**Sugestão:** integrar Lighthouse CI no pipeline (rodando contra deploy preview), WebPageTest para os 3 fluxos críticos (login, comprar passagem, ver bilhete), e React Profiler em sessão de devtools para HomeCPF e MapaCPF.

## O que não foi auditado aqui

- Arquitetura → ver `audit-2-front-review`
- UX estrutural → ver `audit-3-front-ux`
- Performance de backend/API (latência de `/viagens/ativas`, `/gps/embarcacoes`) → fora do escopo

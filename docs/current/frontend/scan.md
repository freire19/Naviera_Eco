---
**Data:** 2026-04-25 09:30
**Projeto:** Naviera Eco
**Alvo selecionado:** naviera-app
---

# Frontend Audit — Scan

## Stack detectada

- **Framework:** React 19.2.5 (PWA, SPA)
- **Build tool:** Vite 5.4.21
- **Linguagem:** JavaScript (sem TypeScript, sem `tsconfig.json`)
- **Styling:** vanilla CSS (`src/App.css` único) + design tokens JS em `src/theme.js` (objeto `T` com `light`/`dark`) — inline styles consumindo os tokens
- **State:** nenhuma lib dedicada — `useState`/`useEffect` puros; navegação por state (`activeTab` em `App.jsx`), sem `react-router`
- **UI lib:** nenhuma — componentes próprios em `src/components/` (Avatar, Badge, Bar, Card, ErrorRetry, Header, Logo, NotificationBanner, NotificationList, PagamentoArtefato, Skeleton, TabBar, Toast)
- **Data fetching:** `fetch` nativo via wrapper em `src/api.js` (não usa axios — CLAUDE.md está desatualizado)
- **Realtime:** `@stomp/stompjs` 7.3.0 + `sockjs-client` 1.6.1 (hook `useWebSocket`)
- **Push/PWA:** Firebase 12.12 (Messaging) + Service Worker (`public/sw.js`, `firebase-messaging-sw.js`), `manifest.json`, fallback `offline.html`
- **Testing:** ausente — zero arquivos `*.test.*` ou `*.spec.*`, nenhuma lib de teste em `package.json`

## Estrutura do projeto

- **Tipo:** multi-app (não-monorepo) — coexiste com `naviera-web` (React 18 + BFF) e Desktop JavaFX no mesmo repositório raiz
- **Apps detectados:** 3 (Desktop fora de escopo deste audit)
- Lista completa:
  - `naviera-app/` — React 19 + Vite — **mobile (destino final)** ← alvo
  - `naviera-web/` — React 18 + Vite + Express BFF — frontend dev/admin
  - `src/` (raiz) — JavaFX 23 — desktop, fora de escopo

**App selecionado para audit:** `naviera-app/`

## Inventário do alvo

- **LOC:** ~2.877 linhas (`.js`/`.jsx` em `src/`)
- **Arquivos `.js`/`.jsx`:** 37
- **Screens:** 14 (`src/screens/`) — fluxos CPF (Home, Amigos, Mapa, Passagens, Bilhete, Encomenda, Perfil) e CNPJ (Home, Pedidos, Loja, Financeiro, LojasParceiras), + Login + Cadastro
- **Componentes reutilizáveis:** 13 (`src/components/`)
- **Hooks customizados:** 3 (`useNotifications`, `usePWA`, `useWebSocket`)
- **Rotas:** N/A — navegação por `useState` em `App.jsx` (sem `react-router`)
- **Testes:** **ausente** — sem cobertura

## Configuração de build relevante

`vite.config.js` minimalista — apenas `@vitejs/plugin-react` + `server.port: 5173, open: true`. Sem aliases, sem code-splitting customizado, sem PWA plugin (PWA é manual via `public/sw.js`), sem proxy. Build padrão Vite.

## Observações da detecção

- **Sem TypeScript** apesar do tamanho do app (~2.9k LOC, 14 screens) — manutenção depende de disciplina manual.
- **Sem testes nem lib de teste** — ausência de rede de segurança para refactors.
- **Workaround StrictMode no top-level** de `App.jsx` (linhas 28–46): migração `localStorage` → `sessionStorage` rodada no escopo do módulo para evitar duplo-efeito do React 19 StrictMode. Comentado como `#DS5-209`.
- **Comentários referenciando IDs internos** (`#DS5-209`, `#DR280`) sugerem catálogo de issues/decisões fora do código — verificar se está documentado.
- **`react-dom` 19.2.5** é versão muito recente (Q1 2026); compatibilidade de libs externas pode ser ponto de atenção.
- **CLAUDE.md desatualizado** menciona axios; o app usa `fetch` nativo.
- **Dois perfis de usuário no mesmo binário** (CPF/CNPJ) — bundle carrega tudo, sem code-splitting por perfil.

## Próximos passos

Rode em seguida:
- `audit-2-front-review` — arquitetura e organização
- `audit-3-front-ux` — estados, formulários, a11y estrutural
- `audit-4-front-perf` — performance estática
- `audit-front-deep` — tudo acima + relatório consolidado

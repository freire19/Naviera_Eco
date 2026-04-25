# Frontend Audit — UX / A11y / Formulários

**Data:** 2026-04-25 09:45
**Alvo:** `naviera-app/`

> ⚠️ **Análise 100% estática.** Não mede UX real — detecta padrões que violam UX/a11y por construção. Validação em runtime (leitor de tela, contraste em todos os temas, testes com usuários reais) segue como próximo passo.

## Resumo executivo

Cobertura de estados (loading/empty) é a parte mais saudável: ~11 das 14 screens usam `Skeleton` e ~9 declaram empty state explícito. **Acessibilidade é o ponto fraco crítico**: app inteiro tem 1 (uma) ocorrência de atributo ARIA, zero `htmlFor` em labels, zero landmarks semânticos, viewport bloqueia pinch-zoom (WCAG fail), e botões icon-only do Header (logout/theme/profile/back) não têm `aria-label`. Formulários têm labels visuais, type=email, e proteção contra double-submit, mas faltam `autoComplete`, `required`, `type="tel"` em CPF/CNPJ/telefone, e qualquer feedback de erro ligado por `aria-describedby`/`aria-invalid`. Padrão de feedback é inconsistente (3 screens com Toast, outras com banner inline, algumas só com `console.warn`).

## Achados por severidade

### 🔴 Crítico

- **Viewport bloqueia zoom do usuário (WCAG 1.4.4 fail)**
  - Localização: `index.html:5` — `<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />`
  - Descrição: `maximum-scale=1.0` + `user-scalable=no` impedem o usuário de aumentar o texto via pinch-zoom.
  - Impacto: usuários com baixa visão não conseguem ampliar conteúdo. Falha WCAG nível AA.
  - Sugestão: remover `maximum-scale` e `user-scalable`. Manter apenas `width=device-width, initial-scale=1`. Se há motivo específico para travar zoom (algum viewport bug), documentar e revisitar.

- **Labels não associados aos inputs (zero `htmlFor`)**
  - Localização: 16 `<label>` em `LoginScreen.jsx:49,56`, `TelaCadastro.jsx:42-50`, `PerfilScreen.jsx:73-76`. `grep -r htmlFor src/` retorna zero.
  - Descrição: labels são `<label style="...">Nome</label>` puramente visuais, sem `htmlFor` nem aninhamento `<label><input/></label>`.
  - Impacto: leitores de tela não conectam o label ao input — usuário cego ouve "campo de texto" sem saber o que é. Tap no label tampouco move foco para o input (UX mobile).
  - Sugestão: adicionar `htmlFor`/`id` em todos os pares label/input, ou aninhar `<label>{texto}<input/></label>`.

- **Botões icon-only sem `aria-label` (Header + 1 acionável destrutivo)**
  - Localização: `src/components/Header.jsx:9` (Back), `:16` (Profile/Avatar), `:19-21` (Theme toggle), `:22-24` (Logout). Todos sem `aria-label`.
  - Descrição: 4 botões com apenas ícone SVG. Ausentes de texto acessível.
  - Impacto: leitor de tela anuncia "botão" sem dizer o quê. Logout é destrutivo — o usuário cego não sabe que está prestes a sair.
  - Sugestão: `aria-label="Voltar"`, `aria-label="Meu perfil"`, `aria-label={mode === 'light' ? 'Ativar tema escuro' : 'Ativar tema claro'}`, `aria-label="Sair"` (e ver moderado abaixo sobre confirmação).

- **Logout sem confirmação**
  - Localização: `src/components/Header.jsx:22` chama `doLogout` (`App.jsx:155`) diretamente.
  - Descrição: tap único em ícone discreto encerra a sessão (limpa sessionStorage, zera state). Sem `window.confirm`, sem modal.
  - Impacto: tap acidental no canto superior direito (zona de polegar em mobile) derruba sessão e força login + 2FA. Pior em PWA standalone — sem botão "voltar do navegador" pra reverter.
  - Sugestão: confirmação dupla (modal "Sair da conta?" com Cancelar/Sair) OU long-press em vez de tap.

- **Sem `aria-live`/`role="alert"` para erros de fetch e form**
  - Localização: erros são divs comuns (`{erro && <div>...</div>}`) — ex: `LoginScreen.jsx:62`, `PassagensCPF.jsx:46`, `MapaCPF.jsx:286`, `TelaCadastro.jsx:51`.
  - Descrição: zero `aria-live`, `role="alert"`, `role="status"` no app. Um leitor de tela não anuncia que apareceu mensagem de erro após submit.
  - Impacto: usuário cego clica "Entrar", não houve feedback audível, fica sem saber por que nada aconteceu.
  - Sugestão: adicionar `role="alert"` ou `aria-live="assertive"` no container de erro de cada form. Para feedback positivo (toast/snackbar), `aria-live="polite"`.

### 🟡 Moderado

- **TabBar ativo sem `aria-current="page"`**
  - Localização: `src/components/TabBar.jsx:4-9`
  - Descrição: o item ativo é distinguido só por `background: t.accent` + um ponto verde (linha 8). Sem indicação programática.
  - Impacto: leitor de tela percorrendo a barra inferior não diz "atual: home" — só "Início, botão" para todas as 5 abas.
  - Sugestão: `aria-current={tab === tb.id ? "page" : undefined}` no `<button>`.

- **Sem landmarks semânticos no app**
  - Localização: `App.jsx:181` envolve tudo num `<div>`; `Header.jsx:7` é `<div>`; `TabBar.jsx:2` é `<div>`. Nenhum `<main>`, `<header>`, `<nav>`, `<footer>` no projeto.
  - Descrição: leitor de tela não consegue saltar para "main content" via shortcut.
  - Impacto: navegação por landmarks (recurso comum em leitores) não funciona; usuários precisam ler header e tabbar a cada nova screen.
  - Sugestão: trocar wrappers por `<header>`, `<main>` (em volta de `screen()`), `<nav>` (em volta de TabBar). Ou usar `role="main"` etc. se há dificuldade estrutural.

- **`<h1>` ausente em screens autenticadas (heading h2/h3 vira o topo)**
  - Localização: única `<h1>` em `LoginScreen.jsx:45` ("NAVIERA"). Screens autenticadas usam `<h2>` ou `<h3>` como topo (ex: `HomeCPF.jsx:19` — `<h2>`, `AmigosCPF.jsx:65` — `<h3>`, `MapaCPF.jsx:255` — `<h3>`).
  - Descrição: hierarquia de headings inicia em h2/h3 sem h1 ancestral.
  - Impacto: leitor de tela navegando por headings (`h` shortcut) percorre estrutura inconsistente.
  - Sugestão: cada screen deve ter um `<h1>` (pode ser visualmente igual ao h2 atual). Manter `<h1>NAVIERA</h1>` apenas em login/home, ou promover o título da screen a h1.

- **Touch targets de 32×32px no Header**
  - Localização: `Header.jsx:16,19,22` — `width: 32, height: 32` para Profile, Theme, Logout.
  - Descrição: WCAG 2.5.5 (AAA) pede 44×44 mínimo; Apple HIG 44pt; Material Design 48dp. 32 está abaixo.
  - Impacto: tap miss frequente em mobile (especialmente para usuários com motricidade reduzida).
  - Sugestão: aumentar para 40–44 ou adicionar `padding` para área tocável maior que o visual. `min-height: 44px` no CSS para todos os botões.

- **Erros do `useApi` ignorados em 3 screens**
  - Localização: `AmigosCPF.jsx:11` (loading e refresh extraídos, `erro` não), `HomeCNPJ.jsx:11-13` (3 chamadas, nenhuma renderiza erro), `HomeCPF.jsx:11-13` (parcial — só `ev` é renderizado).
  - Descrição: `useApi` expõe `erro`, mas a screen consome só `data`/`loading`. Em caso de falha, usuário vê tela vazia ou skeleton infinito.
  - Impacto: usuário não sabe que houve erro de rede; tenta esperar.
  - Sugestão: padronizar consumo de `useApi` — sempre renderizar `erro` (banner ou toast). Pode virar um `<ErrorState>` reusable (já existe `ErrorRetry.jsx` — verificar uso).

- **Destructive actions sem confirmação (além de logout)**
  - Localização: `AmigosCPF.jsx:86` (Recusar amizade), `:102` (Remover amigo).
  - Descrição: tap único remove vínculo. Sem confirmação.
  - Impacto: tap acidental remove amigo; usuário precisa pedir nova solicitação.
  - Sugestão: confirmação inline ("Tem certeza?" → "Sim, remover") ou undo via toast por 5s.

- **Sem `autoComplete` em forms de auth (login, cadastro, perfil)**
  - Localização: `LoginScreen.jsx:55,58`, `TelaCadastro.jsx:42-50`, `PerfilScreen.jsx:73-76`. `grep -r autoComplete src/` retorna zero.
  - Descrição: nenhum input declara `autoComplete="username|email|tel|name|new-password|current-password"`.
  - Impacto: gerenciador de senhas (1Password, Apple Keychain, Google) não preenche; iOS strong-password offering não funciona; UX de cadastro mobile ruim.
  - Sugestão: login → `autoComplete="username"` no doc, `autoComplete="current-password"` na senha. Cadastro → `autoComplete="new-password"` na senha; `email`, `tel`, `name`, `address-level2` (cidade).

- **Sem `required`/`aria-required` nos forms**
  - Localização: todos os inputs em `LoginScreen`, `TelaCadastro`, `PerfilScreen`.
  - Descrição: validação só por JS (`if (!loginDoc) ...`).
  - Impacto: usuário sem JS habilitado (improvável, mas existe modo restritivo) e leitores de tela não recebem hint de obrigatoriedade.
  - Sugestão: `required` nos campos obrigatórios + asterisco visual no label.

- **`type="tel"` ausente em campos numéricos (CPF, CNPJ, telefone)**
  - Localização: `LoginScreen.jsx:55` (doc), `TelaCadastro.jsx:42` (doc), `:46` (telefone), `PerfilScreen.jsx:75` (telefone).
  - Descrição: usam `type="text"` (ou nenhum, default text). Em mobile, abre teclado alfabético em vez de numérico.
  - Impacto: usuário precisa trocar de teclado manualmente — fricção em ~100% dos cadastros.
  - Sugestão: `type="tel"` ou `inputMode="numeric"` em CPF/CNPJ/telefone. Manter `type="email"` no email (já presente).

- **`NotificationList` (dropdown) sem keyboard handling**
  - Localização: `src/components/NotificationList.jsx:29-83`
  - Descrição: dropdown abre com clique no botão (line 44), mas sem `aria-expanded`, `aria-haspopup`, sem tratamento de Escape para fechar, sem focus trap, sem retornar foco ao trigger ao fechar.
  - Impacto: usuário de teclado fica preso quando abre o painel; leitor de tela não anuncia estado expandido/colapsado.
  - Sugestão: `aria-expanded={open}`, `aria-haspopup="true"`, listener de `keydown Escape`, focus management básico.

### 🟢 Menor

- **Padrão de feedback de ação inconsistente**
  - Localização: `Toast` usado em `PassagensCPF.jsx`, `EncomendaCPF.jsx`, `AmigosCPF.jsx`. Banner inline em `LoginScreen`, `TelaCadastro`, `PerfilScreen`, `MapaCPF`. `console.warn` em `App.jsx:121`, `PerfilScreen.jsx:27`.
  - Descrição: 3 padrões de feedback coexistem. Falhas de fetch em alguns lugares só vão pra console.
  - Sugestão: padronizar — Toast para ação concluída, banner inline para erro de form, nada vai só pra console quando afeta UX.

- **Botões da PWA install prompt sem `aria-label`**
  - Localização: `App.jsx:172` ("Agora não") e `:177` ("Instalar")
  - Descrição: botões com texto, mas o gradiente verde no `priGrad` mais o branco "#fff" pode ter contraste limítrofe — verificar.
  - Sugestão: validação de contraste em runtime; texto já é claro ("Agora não", "Instalar") então `aria-label` é redundante aqui. Não-issue de a11y, mas sinalizo para o item de contraste.

- **`Avatar.jsx` usa `alt=""` mesmo quando o avatar identifica usuário**
  - Localização: `src/components/Avatar.jsx:5`
  - Descrição: `alt=""` é correto para imagem decorativa que tem texto adjacente. Em `Header.jsx:17` o avatar é o conteúdo principal do botão de perfil — sem texto adjacente.
  - Sugestão: passar `alt={nome}` no Avatar quando ele for autônomo, ou `aria-label` no botão pai.

- **Skeleton genérico — sem variantes**
  - Localização: `src/components/Skeleton.jsx` (5 LOC)
  - Descrição: skeleton retangular único; chamadores variam por `height` e `count`. Não há variantes (lista, avatar, card-com-imagem) — todas as screens recebem o mesmo placeholder.
  - Impacto: skeleton não combina com a estrutura real (ex: `MapaCPF` espera lista de barcos com avatar — skeleton retangular mostra layout diferente do real).
  - Sugestão: ampliar `Skeleton` com `variant="card" | "list" | "avatar"` ou aceitar duplicação tolerável.

## Cobertura de estados

| Screen | Loading | Empty | Error (mostrado) | Success |
|--------|---------|-------|------------------|---------|
| AmigosCPF | ✅ Skeleton | ❌ (lista crua) | ❌ (erro do useApi não renderizado) | ✅ + Toast |
| BilheteScreen | ⚠️ (sem skeleton — depende de TOTP local) | N/A | ❌ | ✅ |
| EncomendaCPF | ✅ Skeleton | ✅ "Nenhuma encomenda…" | ✅ banner | ✅ Toast/banner |
| FinanceiroCNPJ | ✅ Skeleton | ✅ "Nenhum frete…" | ✅ banner | ✅ |
| HomeCNPJ | ✅ Skeleton (3 seções) | ✅ "Nenhum frete recente" + "Nenhuma loja parceira" | ❌ | ✅ |
| HomeCPF | ✅ Skeleton | ✅ "Nenhuma viagem ativa" + "Nenhum amigo" + "Nenhuma encomenda" | ⚠️ parcial (`ev` renderizado, `le` não) | ✅ |
| LoginScreen | ✅ texto "Entrando..." | N/A | ✅ banner | ✅ |
| LojaCNPJ | ✅ Skeleton | ⚠️ trata `loja` ausente como branch único | ✅ | ✅ |
| LojasParceiras | ✅ Skeleton | ✅ "Nenhuma loja cadastrada" | ✅ | ✅ |
| MapaCPF | ✅ Skeleton | ✅ "Nenhuma embarcação com GPS" | ✅ | ✅ |
| PassagensCPF | ✅ Skeleton | ✅ múltiplos empty | ✅ banner + Toast | ✅ "Passagem emitida!" |
| PedidosCNPJ | ✅ Skeleton | ✅ "Nenhum pedido recebido" | ✅ | ✅ |
| PerfilScreen | ✅ Skeleton | N/A (form) | ✅ banner | ✅ "Sucesso" |
| TelaCadastro | ✅ texto "Cadastrando..." | N/A | ✅ banner | ✅ |

**Totais:** ~11/14 com loading skeleton, ~9/14 com empty explícito, ~10/14 com erro renderizado, todas com success path.

## Formulários

| Form | Campos | Labels (visual) | `htmlFor` | aria-invalid / -describedby | Submit disabled durante envio | autoComplete | type apropriado | Erro visível |
|------|--------|----------------|-----------|------------------------------|------------------------------|--------------|-----------------|--------------|
| LoginScreen | doc + senha | ✅ | ❌ | ❌ | ✅ `loginLoading` | ❌ | senha=password ✅, doc=text ❌ (deveria ser tel) | ✅ banner |
| TelaCadastro | 7 campos | ✅ | ❌ | ❌ | ✅ `loading` | ❌ | email/senha ✅; doc/telefone text ❌ | ✅ banner |
| PerfilScreen | 4 campos | ✅ | ❌ | ❌ | ✅ `salvando` | ❌ | email ✅; telefone text ❌ | ✅ banner |
| EncomendaCPF (busca) | 1 input | ❌ (placeholder-only) | ❌ | ❌ | N/A | ❌ | text ✅ | N/A |
| AmigosCPF (busca) | 1 input | ❌ (placeholder-only) | ❌ | ❌ | N/A | ❌ | text ✅ | N/A |

## Acessibilidade estrutural

- **Páginas sem `<h1>` único:** 13/14 — só `LoginScreen` tem h1; demais começam em h2/h3
- **`<img>` sem `alt`:** 0 — todos têm `alt` (vazio ou descritivo)
- **`<div onClick>` / `<span onClick>` sem `role`/`tabIndex`/keyboard handler:** 2 — `Card.jsx:2` (interativo via prop), `HomeCPF.jsx:46` (`<span onClick="...">Adicionar →</span>`)
- **`outline: none` global sem replacement:** ❌ não detectado — `.input-field:focus` tem `box-shadow` (App.css)
- **`lang` no `<html>`:** ✅ `pt-BR` estático (app monolíngue)
- **Skip link:** ❌ ausente
- **Landmarks (`<main>`, `<nav>`, `<header>`):** ❌ zero
- **ARIA total no app:** 1 (`aria-label="Notificacoes"` em NotificationList.jsx:51)
- **`aria-current` em TabBar:** ❌
- **`aria-live` para feedback assíncrono:** ❌
- **Focus visível:** ✅ em inputs (`box-shadow`); ⚠️ não verificado em `<button>` (sem `:focus-visible` custom em App.css amostrado)

## Feedback de ação

- **Sistema de toast:** componente próprio `Toast.jsx` (8 LOC, auto-close 3s)
- **Cobertura do toast:** 3/14 screens (PassagensCPF, EncomendaCPF, AmigosCPF)
- **Destructive actions sem confirmação:**
  - Logout (Header)
  - Recusar amizade (`AmigosCPF.jsx:86`)
  - Remover amigo (`AmigosCPF.jsx:102`)
- **Mutations sem confirmação visual:** `App.jsx:94` (registrar push token — silencioso é OK aqui), `App.jsx:118` (carregar foto — silencioso é OK)
- **Optimistic update / revert:** nenhuma mutation usa optimistic — todas esperam resposta. Aceitável para o domínio (pagamento).

## Mobile / viewports

- **Viewport meta presente:** ✅ — mas com `user-scalable=no` (ver crítico)
- **Menu mobile adaptado:** ✅ — TabBar fixa inferior + Header + max-width 420px (mobile-first)
- **Touch targets suspeitos:** Header buttons 32×32 (back, profile, theme, logout)
- **Tabelas com scroll horizontal:** N/A — não há tabelas
- **PWA standalone friendly:** ✅ manifest, sw, install prompt; ⚠️ navegação por state (não URL) é problema em standalone — back-button do Android fecha o app

## i18n

**N/A — app monolíngue (pt-BR estático).** Strings em português hardcoded em todos os componentes; sem lib de i18n; uso correto de `Intl` via `helpers.js` (`fmt`, `money` usam `toLocaleString("pt-BR")`).

## Requer validação em runtime

Não dá para saber por análise estática:

- **Contraste real em ambos os temas** (claro e escuro) — especialmente texto branco sobre `priGrad` (gradiente verde), `t.txMuted` em cards
- **Ordem de tabulação efetiva** — nenhum `tabindex` positivo (bom), mas ordem natural pode estar quebrada por flexbox
- **Experiência com leitor de tela** (NVDA/VoiceOver/TalkBack) — especialmente fluxo login → tabbar → screen
- **Comportamento em viewports reais** — max-width 420px funciona em telas estreitas, mas iPhone Pro Max (430+ CSS px) deve funcionar; tablets ficam com app centralizado e laterais brancas
- **PWA back-button** em Android standalone — confirmar se fecha app ou volta tab
- **Touch target real** considerando padding tocável — visual 32×32 pode ter área tocável maior se tiver padding
- **Texto que trunca** sem `text-overflow: ellipsis` — visualmente em runtime

**Sugestão:** rodar axe-core em E2E (Playwright + @axe-core/playwright), passagem manual com VoiceOver iOS / TalkBack Android, e teste em iPhone SE / Galaxy A14 (perfis baixos).

## O que não foi auditado aqui

- Arquitetura de código → ver `audit-2-front-review`
- Performance → ver `audit-4-front-perf`
- Conteúdo / copy editorial → fora do escopo técnico

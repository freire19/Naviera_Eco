# Plano: Sistema de Agentes AI para Naviera (inspirado OpenSquad)

## Objetivo

Implementar um sistema multi-agente dentro do projeto Naviera para criacao, analise e revisao de documentos com rigor profissional. Cada agente tem persona, especialidade e regras. O fluxo passa por quality gates antes de produzir output final.

---

## 1. Arquitetura Geral

```
naviera-agents/
├── _core/                           # Motor do sistema
│   ├── runner.md                    # Orquestrador de pipeline
│   ├── best-practices/              # Guias por disciplina
│   │   ├── analise-tecnica.md
│   │   ├── redacao-comercial.md
│   │   ├── revisao.md
│   │   ├── documentacao-tecnica.md
│   │   ├── relatorio-operacional.md
│   │   └── proposta-comercial.md
│   └── _memory/
│       ├── company.md               # Contexto Naviera (setor, tom, publico)
│       └── preferences.md           # Preferencias do usuario
│
├── squads/                          # Squads disponiveis
│   ├── doc-creator/                 # Criacao de documentos
│   ├── doc-analyzer/                # Analise de documentos existentes
│   ├── proposal-builder/            # Propostas comerciais B2B
│   ├── report-generator/            # Relatorios operacionais
│   └── contract-reviewer/           # Revisao de contratos
│
└── skills/                          # Skills reutilizaveis entre squads
    ├── pesquisa-web.md
    ├── formatacao-abnt.md
    ├── calculo-financeiro.md
    └── extracao-dados.md
```

---

## 2. Anatomia de um Squad

Cada squad segue a mesma estrutura:

```
squads/{nome-do-squad}/
├── squad.yaml                       # Config: nome, descricao, agentes, dominio
├── squad-party.csv                  # Roster: agente, papel, modo execucao
├── agents/
│   ├── pesquisador.agent.md         # Persona + regras do pesquisador
│   ├── escritor.agent.md            # Persona + regras do escritor
│   ├── revisor.agent.md             # Persona + regras do revisor
│   └── editor.agent.md             # Persona + regras do editor
├── pipeline/
│   ├── pipeline.yaml                # Steps em ordem + dependencias
│   ├── steps/
│   │   ├── step-01-briefing.md      # Checkpoint: usuario define escopo
│   │   ├── step-02-pesquisa.md      # Pesquisador coleta dados
│   │   ├── step-03-outline.md       # Escritor cria estrutura
│   │   ├── step-04-rascunho.md      # Escritor escreve versao completa
│   │   ├── step-05-revisao.md       # Revisor audita com criterios
│   │   ├── step-06-edicao.md        # Editor polish final
│   │   └── step-07-aprovacao.md     # Checkpoint: usuario aprova
│   └── data/
│       ├── criterios-qualidade.md   # Checklist de qualidade
│       ├── exemplos-bons.md         # Exemplos de referencia
│       └── anti-patterns.md         # O que evitar
├── output/                          # Outputs de cada execucao
│   └── {YYYY-MM-DD-HHmmss}/
│       ├── pesquisa.md
│       ├── outline.md
│       ├── rascunho-v1.md
│       ├── revisao.md
│       ├── final.md
│       └── state.json
└── _memory/
    ├── memories.md                  # Preferencias aprendidas
    └── runs.md                      # Historico de execucoes
```

---

## 3. Definicao de Agentes

### 3.1 Estrutura de um agente (.agent.md)

```yaml
---
id: squads/{squad}/agents/{nome}
name: "Nome Humano"
title: "Titulo do Papel"
icon: "emoji"
execution: inline | subagent
skills: [lista de skills]
---

# Nome do Agente

## Persona
### Papel — o que faz
### Identidade — quem e, background
### Estilo de comunicacao — tom, nivel de detalhe

## Principios (5+)
## Vocabulario — sempre usar / nunca usar
## Anti-Patterns — nunca fazer / sempre fazer
## Criterios de Qualidade — checklist de sucesso
## Integracao — le de onde, escreve onde
```

### 3.2 Agentes planejados

#### Squad: doc-creator (Criacao de Documentos)

| Agente | Papel | Modo | Descricao |
|--------|-------|------|-----------|
| **Davi Dados** | Pesquisador | subagent | Coleta dados, referencias, contexto. Busca web, le documentos existentes, compila evidencias. |
| **Clara Conteudo** | Escritora | subagent | Produz rascunho completo baseado no outline aprovado. Escreve com tom e estrutura definidos. |
| **Ricardo Revisao** | Revisor | inline | Auditoria rigorosa: verifica dados, tom, estrutura, coerencia, completude. Rejeita ou aprova com score. |
| **Elena Edicao** | Editora | inline | Polish final: gramatica, fluidez, formatacao, consistencia terminologica. Ultimo gate antes da aprovacao. |

#### Squad: doc-analyzer (Analise de Documentos)

| Agente | Papel | Modo | Descricao |
|--------|-------|------|-----------|
| **Ana Analise** | Analista | subagent | Le documento, extrai estrutura, identifica pontos fortes/fracos, classifica por categoria. |
| **Pedro Parecer** | Parecerista | inline | Emite parecer tecnico: conformidade, riscos, recomendacoes, score de qualidade. |
| **Sara Sintese** | Sintetizadora | inline | Resume findings em formato executivo: 1 pagina, bullet points, decisao recomendada. |

#### Squad: proposal-builder (Propostas Comerciais)

| Agente | Papel | Modo | Descricao |
|--------|-------|------|-----------|
| **Marcos Mercado** | Pesquisador | subagent | Pesquisa mercado, concorrentes, precos, diferenciais. |
| **Paula Proposta** | Redatora | subagent | Escreve proposta completa: problema, solucao, escopo, preco, timeline, diferenciais. |
| **Vitor Validacao** | Revisor | inline | Valida: precos coerentes, escopo claro, sem promessas impossíveis, tom profissional. |

#### Squad: report-generator (Relatorios Operacionais)

| Agente | Papel | Modo | Descricao |
|--------|-------|------|-----------|
| **Bruno Busca** | Coletor | subagent | Extrai dados do banco, logs, metricas. Compila em dataset estruturado. |
| **Laura Laudo** | Redatora | subagent | Transforma dados em relatorio narrativo com graficos, comparativos, tendencias. |
| **Oscar Operacional** | Revisor | inline | Valida numeros, detecta inconsistencias, verifica se conclusoes batem com dados. |

---

## 4. Pipeline — Fluxo de Execucao

### 4.1 Tipos de step

| Tipo | Quem executa | Descricao |
|------|-------------|-----------|
| **checkpoint** | Usuario | Coleta input ou aprovacao. Nao executa IA. |
| **subagent** | Agente (background) | Executa em contexto separado, retorna output. |
| **inline** | Agente (conversa) | Executa na conversa, usuario ve raciocinio. |

### 4.2 Pipeline do doc-creator (exemplo completo)

```
┌─────────────────────────────────────────────────────┐
│ Step 1: BRIEFING (checkpoint)                       │
│ Usuario define: tipo documento, objetivo, publico,  │
│ tom, extensao, referencias                          │
│ Output: briefing.md                                 │
└──────────────────────┬──────────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────────┐
│ Step 2: PESQUISA (Davi Dados, subagent)             │
│ Coleta dados, referencias, exemplos                 │
│ Input: briefing.md                                  │
│ Output: pesquisa.md                                 │
│ Veto: menos de 3 fontes = rejeitar                  │
└──────────────────────┬──────────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────────┐
│ Step 3: APROVAR PESQUISA (checkpoint)               │
│ Usuario revisa dados coletados                      │
│ Pode: aprovar / pedir mais pesquisa / ajustar foco  │
└──────────────────────┬──────────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────────┐
│ Step 4: OUTLINE (Clara Conteudo, inline)             │
│ Cria estrutura do documento: secoes, topicos, ordem │
│ Input: briefing.md + pesquisa.md                    │
│ Output: outline.md                                  │
└──────────────────────┬──────────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────────┐
│ Step 5: APROVAR OUTLINE (checkpoint)                │
│ Usuario valida estrutura antes da redacao           │
└──────────────────────┬──────────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────────┐
│ Step 6: RASCUNHO (Clara Conteudo, subagent)         │
│ Escreve documento completo                          │
│ Input: briefing.md + pesquisa.md + outline.md       │
│ Output: rascunho-v1.md                              │
│ Veto: menos de 500 palavras = rejeitar              │
└──────────────────────┬──────────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────────┐
│ Step 7: REVISAO (Ricardo Revisao, inline)            │
│ Auditoria completa com scoring                      │
│ Input: rascunho-v1.md + briefing.md                 │
│ Output: revisao.md (score + feedback)               │
│ Criterios: dados corretos, tom adequado, estrutura  │
│   coerente, sem repeticoes, referencias validas     │
│ Veto: score < 6/10 em qualquer criterio = REJEITAR  │
│ Se rejeitado: volta ao Step 6 (max 2 tentativas)    │
└──────────────────────┬──────────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────────┐
│ Step 8: EDICAO (Elena Edicao, inline)                │
│ Polish final: gramatica, fluidez, formatacao        │
│ Input: rascunho-v1.md (ou v2 se houve rejeicao)     │
│ Output: final.md                                    │
└──────────────────────┬──────────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────────┐
│ Step 9: APROVACAO FINAL (checkpoint)                │
│ Usuario le versao final, aprova ou pede ajustes     │
│ Status: Aprovado / Rejeitado / Publicado            │
└─────────────────────────────────────────────────────┘
```

### 4.3 Mecanismos de qualidade

**Veto Conditions (rejeicao automatica):**
- Cada step pode definir condicoes que auto-rejeitam o output
- Se rejeitado, volta ao step anterior para correcao (max 2 tentativas)
- Na 3a falha, para e pede intervencao humana

**Scoring do Revisor (exemplo):**

| Criterio | Peso | Nota (1-10) | Veto se < |
|----------|------|-------------|-----------|
| Dados corretos e verificaveis | 25% | — | 6 |
| Tom adequado ao publico | 20% | — | 6 |
| Estrutura logica e coerente | 20% | — | 5 |
| Completude (cobre o briefing) | 15% | — | 6 |
| Clareza e fluidez | 10% | — | 5 |
| Formatacao e consistencia | 10% | — | 4 |

**Resultado:** APROVADO (media >= 7) / REVISAO (media 5-7) / REJEITADO (qualquer criterio < veto)

---

## 5. Memoria e Aprendizado

### 5.1 memories.md (por squad)

Armazena preferencias aprendidas entre execucoes:

```markdown
## Estilo de Escrita
- Usuario prefere paragrafos curtos (max 4 linhas)
- Evitar jargao tecnico quando publico e leigo
- Bullet points para listas, nunca paragrafos enumerados

## Formatacao
- Sempre incluir sumario executivo no inicio
- Tabelas para comparativos, nunca texto corrido
- Negrito em termos-chave, nunca sublinhado

## Proibicoes
- Nunca usar "outrossim", "destarte", "nao obstante"
- Nunca iniciar com "No cenario atual..."
- Nunca usar mais de 2 niveis de heading

## Tom
- Profissional mas acessivel
- Dados antes de opiniao
- Conclusoes diretas, sem rodeios
```

### 5.2 runs.md (historico)

| Data | Run ID | Tipo | Tema | Resultado |
|------|--------|------|------|-----------|
| 2026-04-15 | run-001 | Proposta | Plano escala Naviera | Aprovado |
| 2026-04-14 | run-002 | Relatorio | Metricas abril | Aprovado |

---

## 6. Integracao com Naviera

### 6.1 Onde vive no projeto

```
Naviera_Eco/
├── naviera-agents/          # <<< NOVO — sistema de agentes
│   ├── _core/
│   ├── squads/
│   └── skills/
├── naviera-web/             # Existente
├── naviera-ocr/             # Existente
├── naviera-app/             # Existente
└── ...
```

### 6.2 Como invocar

Via Claude Code skill (`/squad`):

```
/squad create doc-creator     # Wizard para criar novo squad
/squad run doc-creator        # Executar pipeline
/squad edit doc-creator       # Editar agentes/pipeline
/squad list                   # Listar squads disponiveis
```

### 6.3 Dados do Naviera como input

Os agentes podem acessar dados operacionais do Naviera como contexto:

- **Banco PostgreSQL** — metricas, viagens, financeiro (para relatorios)
- **docs/** — documentos existentes (para analise)
- **CLAUDE.md** — contexto do projeto (para documentacao tecnica)
- **Git history** — changelogs, decisoes (para release notes)

---

## 7. Squads Prioritarios (ordem de implementacao)

| # | Squad | Uso imediato | Complexidade |
|---|-------|-------------|-------------|
| 1 | **doc-creator** | Documentacao, specs, planos | Media — 4 agentes, 9 steps |
| 2 | **report-generator** | Relatorios operacionais | Media — 3 agentes, 7 steps |
| 3 | **proposal-builder** | Propostas B2B para empresas | Media — 3 agentes, 8 steps |
| 4 | **doc-analyzer** | Analise de contratos, specs | Baixa — 3 agentes, 5 steps |
| 5 | **contract-reviewer** | Revisao juridica basica | Alta — precisa knowledge legal |

---

## 8. Diferencias vs OpenSquad original

| Aspecto | OpenSquad (Meta ADS) | Naviera Agents |
|---------|---------------------|----------------|
| Dominio | Ads + conteudo social | Documentos + relatorios + propostas |
| Investigacao | Sherlock (perfis sociais) | Extracao de dados do banco Naviera |
| Output | Copy de ads, scripts de Reels | Documentos PDF-ready, relatorios |
| Revisao | 1 revisor generico | Revisor especializado + Editor (2 gates) |
| Dados | Web scraping | PostgreSQL + arquivos internos |
| Deploy | Publica em redes sociais | Salva em docs/ ou exporta PDF |
| Automacao | Playwright (browser) | Queries SQL + file system |

---

## 9. Proximos Passos

1. Criar estrutura de pastas `naviera-agents/`
2. Escrever `_core/runner.md` (orquestrador)
3. Escrever `_core/_memory/company.md` (contexto Naviera)
4. Implementar squad `doc-creator` completo (4 agentes + 9 steps)
5. Criar skill `/squad` no Claude Code
6. Testar com caso real: gerar spec tecnica de uma feature do Naviera
7. Iterar sobre feedback → atualizar memories.md
8. Implementar squad 2 (report-generator)

---

*Plano criado: 2026-04-15*
*Baseado na arquitetura OpenSquad do projeto Meta_ADS*

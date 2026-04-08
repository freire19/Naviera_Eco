# AUDITORIA PROFUNDA — PERFORMANCE — SistemaEmbarcacaoProjeto_Novo
> **Versao:** V1.0
> **Data:** 2026-04-07
> **Categoria:** Performance
> **Base:** AUDIT_V1.0
> **Arquivos analisados:** 131 de 131 total (cobertura completa)

---

## RESUMO

| Status | Quantidade |
|--------|-----------|
| Novos problemas | 25 |
| Issues anteriores resolvidas | 0 |
| Issues anteriores parcialmente resolvidas | 0 |
| Issues anteriores pendentes | 6 |
| **Total de issues ativas** | **31** |

---

## ISSUES ANTERIORES — STATUS

### Pendentes

| Issue | Titulo | Observacao |
|-------|--------|-----------|
| #043 | N+1 PassageiroDAO (3/row) | Confirmado: linhas 171-186 |
| #044 | N+1 PassagemDAO (5/row) | Confirmado: linhas 207-211 |
| #045 | Sem connection pooling | Confirmado: DriverManager.getConnection() direto |
| #046 | println em cada conexao | Confirmado: DatabaseConnection:18 |
| #047 | ORDER BY 1 fragil | Confirmado: ReciboAvulsoDAO:31,47 |
| #048 | JSON parser custom (250+ linhas) | Confirmado: SyncClient:449-704 |

---

## NOVOS PROBLEMAS

### N+1 e Multiplas Conexoes por Operacao

#### Issue #DP001 — PassagemDAO.inserir(): 5 conexoes por insert de passagem
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/dao/PassagemDAO.java`
- **Linha(s):** 46-49, 62
- **Problema:** Cada insert resolve 4 nomes auxiliares via `obterIdAuxiliar()`, cada um abrindo conexao propria. Total: 5 conexoes por venda de passagem.
- **Impacto:** Cada venda de bilhete faz 5 TCP handshakes + 5 auth PostgreSQL.
- **Fix sugerido:** Passar IDs em vez de nomes, ou cachear tabelas auxiliares em HashMap.
- **Observacoes:**
> _Sem connection pool (#045), impacto e 5x pior._

---

#### Issue #DP002 — PassagemDAO.atualizar(): 6 conexoes por update
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/dao/PassagemDAO.java`
- **Linha(s):** 98-101, 114
- **Problema:** Mesmo padrao do inserir, com 5 lookups auxiliares + 1 conexao principal.
- **Impacto:** Editar passagem e tao caro quanto criar.
- **Fix sugerido:** Mesmo que DP001.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DP003 — PassageiroDAO.inserir/atualizar: 4 conexoes por operacao
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/PassageiroDAO.java`
- **Linha(s):** 61-64 (inserir), 92-95 (atualizar)
- **Problema:** 3 lookups auxiliares + 1 principal = 4 conexoes por CRUD de passageiro.
- **Impacto:** Cadastro de passageiro mais lento que necessario.
- **Fix sugerido:** Cache de tabelas auxiliares ou passar IDs.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DP004 — N+1 em ListaEncomendaController: query de itens POR encomenda no filtro
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/ListaEncomendaController.java`
- **Linha(s):** 494-497
- **Problema:** Filtro por item chama `encomendaItemDAO.listarPorIdEncomenda()` para CADA encomenda na lista. 200 encomendas = 200 queries. Acionado a cada tecla digitada no campo de busca.
- **Impacto:** UI congela por segundos ao digitar filtro. Cada keystroke dispara N queries.
- **Codigo problematico:**
```java
List<EncomendaItem> itens = encomendaItemDAO.listarPorIdEncomenda(e.getId());
matchItem = itens.stream().anyMatch(i -> i.getDescricao().toUpperCase().contains(itemBusca));
```
- **Fix sugerido:** Pre-carregar itens em `Map<Long, List<EncomendaItem>>` ao carregar viagem. Filtrar contra mapa em memoria.
- **Observacoes:**
> _Text listener em txtFiltroItem (linha 148) dispara aplicarFiltros() a cada caracter._

---

#### Issue #DP005 — N+1 no calendario: 30+ queries por mes
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/TelaPrincipalController.java`
- **Linha(s):** 332
- **Problema:** `construirCalendario()` chama `agendaDAO.buscarAnotacoesPorData(dataAtual)` para CADA dia do mes (28-31 queries). Chamado em init, apos cada modal fechar, ao trocar mes.
- **Impacto:** 30+ queries a cada interacao com tela principal.
- **Codigo problematico:**
```java
for (int dia = 1; dia <= diasNoMes; dia++) {
    List<String> notas = agendaDAO.buscarAnotacoesPorData(dataAtual);
```
- **Fix sugerido:** `buscarAnotacoesPorMes(mes, ano)` retornando `Map<LocalDate, List<String>>`. 1 query em vez de 30.
- **Observacoes:**
> _Combinado com queries de viagens e boletos nas linhas 255-256, total e 90+ queries por build._

---

### Queries Sem Indice e Full Table Scans

#### Issue #DP006 — CAST() em ORDER BY/MAX previne uso de indice
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** PassagemDAO:21, EncomendaDAO:48,163
- **Linha(s):** Ver acima
- **Problema:** `CAST(numero_bilhete AS INTEGER)` e `CAST(numero_encomenda AS INTEGER)` forcam full scan.
- **Impacto:** Cada venda faz full scan em passagens. Cada listagem de encomendas faz full scan.
- **Fix sugerido:** Mudar colunas para INTEGER ou criar expression index.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DP007 — EXTRACT(MONTH/YEAR) previne uso de indice
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** ViagemDAO:29, AgendaDAO:82-83
- **Linha(s):** Ver acima
- **Problema:** `EXTRACT(MONTH FROM data_viagem)` e `EXTRACT(YEAR FROM data_viagem)` forcam scan completo.
- **Impacto:** Calendario e listagem de viagens por mes fazem full scan.
- **Fix sugerido:** Substituir por range: `WHERE data_viagem >= ? AND data_viagem < ?`.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DP008 — Indices criticos ausentes (16 colunas identificadas)
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** database_scripts/ (migracao necessaria)
- **Linha(s):** N/A
- **Problema:** Scripts de migracao so criam indices em uuid e sincronizado. Colunas usadas em WHERE/JOIN/ORDER BY nao tem indice.
- **Impacto:** Toda query filtrada faz sequential scan. Degrada linearmente com volume.

| Tabela | Coluna(s) | Usado em |
|--------|-----------|----------|
| passagens | id_viagem | PassagemDAO, BalancoViagemDAO |
| passagens | id_passageiro | PassagemDAO joins |
| passagens | data_emissao | PassagemDAO ORDER BY |
| passagens | status_passagem | Filtros multiplos |
| encomendas | id_viagem | EncomendaDAO, BalancoViagemDAO |
| fretes | id_viagem | FreteDAO, BalancoViagemDAO |
| fretes | data_emissao | FreteDAO filtro por data |
| viagens | data_viagem | ViagemDAO calendario |
| viagens | is_atual | ViagemDAO.buscarViagemAtiva |
| passageiros | nome_passageiro | ILIKE searches |
| passageiros | numero_documento | PassageiroDAO.buscarPorDoc |
| frete_itens | id_frete | FreteDAO subquery |
| financeiro_saidas | data_vencimento | AgendaDAO, FinanceiroSaidaController |
| financeiro_saidas | id_viagem | FinanceiroEntradaController |
| financeiro_saidas | forma_pagamento | AgendaDAO filtro |

- **Fix sugerido:** Script de migracao com todos os indices acima.
- **Observacoes:**
> _Para nome_passageiro ILIKE, usar indice trigram: `CREATE INDEX ... USING gin (nome_passageiro gin_trgm_ops)`._

---

#### Issue #DP009 — ILIKE com wildcard leading em FreteDAO.buscarFretes
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/FreteDAO.java`
- **Linha(s):** 43-44
- **Problema:** `fi.nome_item_ou_id_produto ILIKE '%termo%'` dentro de EXISTS subquery. Leading `%` impede qualquer indice.
- **Impacto:** Para cada frete, scan completo de frete_itens.
- **Fix sugerido:** Indice trigram GIN ou remover leading `%`.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DP010 — Subquery correlacionada por frete em FreteDAO
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/FreteDAO.java`
- **Linha(s):** 29
- **Problema:** `(SELECT COALESCE(SUM(fi.quantidade), 0) FROM frete_itens fi WHERE fi.id_frete = f.id_frete)` executa por linha.
- **Impacto:** N subqueries para N fretes.
- **Fix sugerido:** LEFT JOIN com subquery agrupada.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

### Queries Sem Limite (Unbounded)

#### Issue #DP011 — listarTodos() sem LIMIT em 4 DAOs criticos
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** PassagemDAO:217, EncomendaDAO:62, ViagemDAO:252, AgendaDAO:134
- **Linha(s):** Ver acima
- **Problema:** Queries carregam TODOS os registros historicos sem LIMIT. Combinado com N+1 em PassagemDAO, e catastrofico.
- **Impacto:** 1000 passagens * 5 conexoes/row = 5001 conexoes. Memoria cresce indefinidamente.
- **Fix sugerido:** Adicionar LIMIT ou filtro por viagem ativa/periodo.
- **Observacoes:**
> _PassagemDAO.listarTodos() + N+1 (#044) = pior cenario de performance do projeto._

---

### Dados Redundantes e Cache Ausente

#### Issue #DP012 — configuracao_empresa consultada 6+ vezes por sessao
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** VenderPassagemController:377, BalancoViagemController:430, RelatorioFretesController:381+948+1063+1432, RelatorioUtil:147
- **Linha(s):** Ver acima
- **Problema:** Tabela de 1 registro consultada independentemente por cada controller. RelatorioFretesController consulta 4 vezes (uma por metodo de impressao).
- **Impacto:** 6+ conexoes desnecessarias por sessao para dado statico.
- **Fix sugerido:** Singleton `EmpresaConfigCache` com carga unica.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DP013 — viagem ativa consultada redundantemente em cada tela
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** TelaPrincipalController:567,588,779, CadastroFreteController:435, InserirEncomendaController:206
- **Linha(s):** Ver acima
- **Problema:** `buscarViagemAtiva()` chamada em atualizarDashboard (apos cada modal fechar), carregarViagensNoCombo, abrirTelaComViagem, e por cada tela filha.
- **Impacto:** 3-5 queries redundantes por ciclo de uso.
- **Fix sugerido:** Cache em TelaPrincipalController, passar para filhos.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DP014 — Contatos carregados 2x com mesma query em CadastroFreteController
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/gui/CadastroFreteController.java`
- **Linha(s):** 615-616, 1059-1073
- **Problema:** `carregarContatosParaComboBoxes` chamada 2x com mesma SQL. Remetentes e Clientes sao a mesma query.
- **Impacto:** 1 conexao extra desnecessaria.
- **Fix sugerido:** Carregar uma vez e copiar para ambas as listas.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

### UI Thread e Impressao

#### Issue #DP015 — Pixel-by-pixel image copy em VenderPassagemController
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/VenderPassagemController.java`
- **Linha(s):** 1792-1796
- **Problema:** Loop `for x { for y { bufferedImage.setRGB() } }` para converter JavaFX Image. 600x230 = 138.000 iteracoes no FX thread.
- **Impacto:** UI congela por ~500ms durante impressao de bilhete.
- **Codigo problematico:**
```java
for (int x = 0; x < imgWidth; x++) {
    for (int y = 0; y < imgHeight; y++) {
        bufferedImage.setRGB(x, y, fxImage.getPixelReader().getArgb(x, y));
    }
}
```
- **Fix sugerido:** `SwingFXUtils.fromFXImage(fxImage, null)` — chamada unica nativa.
- **Observacoes:**
> _Fix de 1 linha que elimina 138.000 iteracoes._

---

#### Issue #DP016 — Impressao sincrona bloqueia UI thread
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/VenderPassagemController.java`
- **Linha(s):** 1834
- **Problema:** `job.print()` (AWT PrinterJob) e chamada sincrona no FX thread. Espera spooler completar.
- **Impacto:** UI congela durante impressao (1-5 segundos com impressora termica).
- **Fix sugerido:** Mover conversao + job.print() para background thread apos snapshot.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DP017 — 5 conexoes por impressao em RelatorioFretesController
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/RelatorioFretesController.java`
- **Linha(s):** 381, 430, 441, 496, 551
- **Problema:** Cada metodo de impressao abre 5 conexoes separadas (empresa, numero, remetente, itens, valores).
- **Impacto:** Impressao de nota de frete demora mais por excesso de roundtrips.
- **Fix sugerido:** Uma unica conexao para buscar todos os dados antes de montar layout.
- **Observacoes:**
> _Padrao repete em imprimirResumidoPorRemetente e imprimirExtratoCliente._

---

#### Issue #DP018 — Logo carregado do disco a cada impressao
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/gui/RelatorioFretesController.java`
- **Linha(s):** 398-403, 1079-1085, 1448-1455
- **Problema:** `new Image("file:" + caminhoFoto)` criado a cada impressao termica.
- **Impacto:** I/O de disco desnecessario.
- **Fix sugerido:** Cache da Image em campo static.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

### Threads e Recursos

#### Issue #DP019 — Raw Thread sem daemon flag (8+ locais)
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** VenderPassagemController:261,482, InserirEncomendaController:275,294, CadastroFreteController:1517,1542, TelaPrincipalController:930,1207
- **Linha(s):** Ver acima
- **Problema:** `new Thread(() -> {...}).start()` sem setDaemon(true). Se app fechar durante thread, JVM nao termina.
- **Impacto:** Processo Java fica pendurado apos fechar aplicacao.
- **Fix sugerido:** `setDaemon(true)` ou usar `javafx.concurrent.Task`.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DP020 — PreparedStatements nao fechados em BalancoViagemController
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/BalancoViagemController.java`
- **Linha(s):** 203-205
- **Problema:** `st1` e `st2` criados sem try-with-resources. Nunca fechados.
- **Impacto:** Leak de statements acumula com trocas de viagem.
- **Fix sugerido:** Wrap em try-with-resources.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DP021 — DateTimeFormatter criado repetidamente em toString()
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** ReciboQuitacaoPassageiro:45, Viagem:78,83,87-89
- **Linha(s):** Ver acima
- **Problema:** `DateTimeFormatter.ofPattern()` dentro de toString()/getDataViagemStr(). Instanciado a cada chamada (caro para parsing de pattern).
- **Impacto:** Overhead em TableViews com muitas linhas.
- **Fix sugerido:** Extrair para `private static final DateTimeFormatter`.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DP022 — NumberFormat criado dentro de loop no calendario
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/gui/TelaPrincipalController.java`
- **Linha(s):** 313
- **Problema:** `NumberFormat.getCurrencyInstance(new Locale("pt","BR"))` dentro de for(dia).
- **Impacto:** 30 instanciacoes por build de calendario.
- **Fix sugerido:** Mover para fora do loop.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

### Dependencias

#### Issue #DP023 — JARs duplicados e potencialmente nao utilizados (~35MB desperdicados)
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `lib/`
- **Linha(s):** N/A
- **Problema:** Duplicatas: commons-beanutils (1.9.2 + 1.9.4), commons-logging (1.1.1 + 1.2). Conflito: log4j + log4j-over-slf4j. Possivelmente nao usados: vosk (23MB), tess4j+lept4j (12MB), jboss-vfs. JUnit em producao.

| JAR | Tamanho | Status |
|-----|---------|--------|
| vosk-0.3.38.jar | 23MB | Usado em 2 controllers apenas |
| tess4j-3.4.8 + lept4j-1.6.4 | 12MB | OCR — usado em 2 controllers |
| commons-beanutils-1.9.2 | 229KB | Duplicado (manter 1.9.4) |
| commons-logging-1.1.1 | 60KB | Duplicado (manter 1.2) |
| log4j-1.2.17 | 478KB | Conflita com log4j-over-slf4j |
| junit-4.12 + hamcrest | 390KB | Nao deve estar em producao |
| jboss-vfs-3.2.12 | 558KB | Incomum para desktop |

- **Fix sugerido:** Remover duplicatas, mover JUnit para classpath de teste, considerar loading dinamico para vosk/tess4j.
- **Observacoes:**
> _Total lib/: ~78MB. ~35MB possivelmente eliminavel._

---

#### Issue #DP024 — .classpath com 13 JARs faltando e paths Windows
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `.classpath`
- **Linha(s):** 12-19
- **Problema:** Classpath referencia `C:/javafx-sdk-23.0.2/` (Windows). 13 JARs do lib/ nao estao no classpath (mortos).
- **Impacto:** Build quebrado em qualquer maquina que nao seja o PC original.
- **Fix sugerido:** Migrar para Maven/Gradle.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DP025 — SQL dinamico impede cache de PreparedStatement
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/dao/AuxiliaresDAO.java`
- **Linha(s):** 29, 89
- **Problema:** Concatenacao de tabela/coluna gera SQL unico a cada chamada. PostgreSQL nao reutiliza plano.
- **Impacto:** Overhead de parsing multiplicado pelo N+1.
- **Fix sugerido:** Usar metodos dedicados por tabela (ja existem — deprecar generico).
- **Observacoes:**
> _(sem observacoes adicionais)_

---

## COBERTURA

| Diretorio | Arquivos | Issues novas |
|-----------|----------|-------------|
| src/dao/ | 28 | 8 |
| src/database/ | 2 | 0 |
| src/gui/ | 55 | 12 |
| src/gui/util/ | 5 | 0 |
| src/model/ | 26 | 1 |
| database_scripts/ | 7 | 1 |
| lib/ + .classpath | 2 | 2 |
| **TOTAL** | **131** | **25** |

---

## PLANO DE CORRECAO

### Urgente (CRITICO)

- [ ] #DP004 — N+1 em filtro de encomendas (ListaEncomendaController) — **Esforco:** 1h
- [ ] #DP005 — 30+ queries por calendario (TelaPrincipalController) — **Esforco:** 1h
- **Notas:**
> _DP004 dispara a cada keystroke. DP005 dispara em cada interacao com tela principal._

### Importante (ALTO)

- [ ] #DP001 — 5 conexoes por insert de passagem — **Esforco:** 2h (cache de auxiliares)
- [ ] #DP002 — 6 conexoes por update de passagem — **Esforco:** junto com DP001
- [ ] #DP006 — CAST previne indice (bilhete/encomenda) — **Esforco:** 1h (migration + alter table)
- [ ] #DP007 — EXTRACT previne indice (viagem/agenda) — **Esforco:** 30min (rewrite queries)
- [ ] #DP008 — 16 indices criticos ausentes — **Esforco:** 1h (script de migracao)
- [ ] #DP011 — listarTodos sem LIMIT (4 DAOs) — **Esforco:** 30min
- **Notas:**
> _DP008 e o fix de maior impacto com menor esforco. DP001+DP002 resolvem tambem ao cachear._

### Importante (MEDIO)

- [ ] #DP003 — 4 conexoes por CRUD passageiro — **Esforco:** junto com DP001
- [ ] #DP009 — ILIKE leading wildcard em FreteDAO — **Esforco:** 30min
- [ ] #DP010 — Subquery correlacionada em FreteDAO — **Esforco:** 15min (LEFT JOIN)
- [ ] #DP012 — Empresa config sem cache (6+x) — **Esforco:** 30min (singleton)
- [ ] #DP013 — Viagem ativa sem cache — **Esforco:** 30min
- [ ] #DP015 — Pixel-by-pixel image copy — **Esforco:** 5min (1 linha)
- [ ] #DP016 — Impressao sincrona — **Esforco:** 30min
- [ ] #DP017 — 5 conexoes por impressao — **Esforco:** 1h
- [ ] #DP019 — Threads sem daemon flag — **Esforco:** 15min
- [ ] #DP020 — Statements nao fechados — **Esforco:** 10min
- [ ] #DP023 — JARs duplicados/nao usados — **Esforco:** 1h
- **Notas:**
> _DP015 e fix de 1 linha com impacto imediato (elimina 138k iteracoes)._

### Menor (BAIXO)

- [ ] #DP014 — Contatos carregados 2x — **Esforco:** 5min
- [ ] #DP018 — Logo sem cache — **Esforco:** 10min
- [ ] #DP021 — DateTimeFormatter repetido — **Esforco:** 5min
- [ ] #DP022 — NumberFormat no loop — **Esforco:** 2min
- [ ] #DP024 — Classpath broken — **Esforco:** junto com migracao Maven
- [ ] #DP025 — SQL dinamico sem cache — **Esforco:** junto com DP001
- **Notas:**
> _(sem observacoes adicionais)_

---

## MAPA DE CONEXOES POR OPERACAO

| Operacao | Conexoes | Impacto |
|----------|----------|---------|
| Listar 100 passagens | 501 (1 + 5*100) | CATASTROFICO |
| Listar 100 passageiros | 301 (1 + 3*100) | SEVERO |
| Vender 1 passagem | 5 | ALTO |
| Editar 1 passagem | 6 | ALTO |
| Build calendario (1 mes) | 30+ | ALTO |
| Filtrar encomendas (por item) | 1 + N | ALTO (por keystroke) |
| Imprimir nota frete | 5 | MEDIO |
| Atualizar dashboard | 4 | MEDIO |
| Cadastrar passageiro | 4 | MEDIO |

---

## NOTAS

> **Fix #1 de maior impacto: Cache de tabelas auxiliares.** Cachear as ~10 tabelas auxiliares em HashMaps (total < 500 registros) eliminaria ~80% das conexoes extras do projeto. Um unico `AuxiliaresCache.getInstance()` carregado no startup resolveria DP001, DP002, DP003, e reduziria drasticamente #043 e #044.
>
> **Fix #2 de maior impacto: Indices.** DP008 (16 indices) pode ser feito em 1 hora e acelera toda query filtrada do sistema.
>
> **Fix #3 de maior impacto: Connection pool.** HikariCP com pool de 10 conexoes eliminaria o overhead de TCP+auth por operacao. Sem pool, cada uma das 501 conexoes de "listar passagens" e um TCP handshake completo.
>
> **Comparacao com scan:** Scan encontrou 6 issues de performance. Deep audit encontrou 25 adicionais. Total ativo: **31**.

---
*Gerado por Claude Code (Deep Audit) — Revisao humana obrigatoria*

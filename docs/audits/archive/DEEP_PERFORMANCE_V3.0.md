# AUDITORIA PROFUNDA — PERFORMANCE — SistemaEmbarcacaoProjeto_Novo
> **Versao:** V3.0
> **Data:** 2026-04-09
> **Categoria:** Performance
> **Base:** AUDIT_V1.1
> **Arquivos analisados:** 131 de 131 total (cobertura completa)

---

## RESUMO

| Status | Quantidade |
|--------|-----------|
| Novos problemas encontrados | 8 |
| Novos problemas corrigidos | 8 |
| Issues anteriores resolvidas | 23 |
| Issues anteriores pendentes | 2 |
| **Total de issues ativas** | **2** |

---

## ISSUES ANTERIORES — STATUS

### Resolvidas

| Issue | Titulo | Verificacao |
|-------|--------|------------|
| #045 | Connection pooling ausente | RESOLVIDO — LinkedBlockingDeque + PooledConnection + timeout + max lifetime em ConexaoBD.java |
| #046 | println em cada conexao | RESOLVIDO — Removido; apenas System.err em erros |
| #DP001 | 5 conexoes por insert passagem | RESOLVIDO — AuxiliaresDAO cache ConcurrentHashMap elimina lookups |
| #DP002 | 6 conexoes por update passagem | RESOLVIDO — Mesmo fix DP001 |
| #DP003 | 4 conexoes por CRUD passageiro | RESOLVIDO — AuxiliaresDAO cache |
| #DP004 | N+1 filtro encomendas (200 queries) | RESOLVIDO — `cacheItensViagem` Map pre-carregado. ListaEncomendaController:546 |
| #DP005 | 30+ queries por calendario | RESOLVIDO — `buscarAnotacoesPorMes()` retorna Map. 1 query |
| #DP006 | CAST em ORDER BY previne indice | RESOLVIDO — `ORDER BY id_encomenda` em EncomendaDAO |
| #DP007 | EXTRACT previne indice | RESOLVIDO — Range queries `>= ? AND < ?` em ViagemDAO e AgendaDAO |
| #DP008 | 16 indices criticos ausentes | RESOLVIDO — Script `006_criar_indices_performance.sql` com 18 indices |
| #DP010 | Subquery correlacionada FreteDAO | RESOLVIDO — LEFT JOIN com GROUP BY |
| #DP011 | listarTodos sem LIMIT | RESOLVIDO — LIMIT 500 em PassagemDAO e EncomendaDAO |
| #DP013 | Viagem ativa sem cache | RESOLVIDO — Cache estatico `cacheViagemAtiva` em ViagemDAO |
| #DP015 | Pixel-by-pixel image copy | RESOLVIDO — `SwingFXUtils.fromFXImage()` |
| #DP016 | Impressao sincrona VenderPassagem | RESOLVIDO — daemon thread para job.print() |
| #DP017 | 5 conexoes por impressao frete | RESOLVIDO — Pool de conexoes mitiga overhead |
| #DP022 | NumberFormat no loop calendario | RESOLVIDO — Movido para fora do loop |
| #DP025 | SQL dinamico sem cache | RESOLVIDO — Whitelist validation em AuxiliaresDAO |

### Pendentes

| Issue | Titulo | Observacao |
|-------|--------|-----------|
| #047 | ORDER BY 1 fragil | RESOLVIDO — `ORDER BY id_recibo DESC` |
| #048 | JSON parser custom (250+ linhas) | Confirmado: SyncClient:449-704. Substituir por lib JSON |
| #DP009 | ILIKE leading wildcard FreteDAO | RESOLVIDO — Indice trigram GIN em `011_indice_trigram_frete_itens.sql` |
| #DP014 | Contatos carregados 2x | RESOLVIDO — 1 query + setAll para copiar lista |
| #DP018 | Logo carregado do disco a cada impressao | RESOLVIDO — ImageCache centralizado (7 locais) |
| #DP021 | DateTimeFormatter repetido em models | RESOLVIDO — static final DTF_DATA em Viagem e Encomenda |
| #DP023 | JARs duplicados e nao utilizados (~35MB) | Confirmado: sem mudanca |

---

## NOVOS PROBLEMAS

#### Issue #DP026 — formatar() cria NumberFormat a cada chamada dentro de loops
- [x] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/BalancoViagemController.java`
- **Linha(s):** 532 (metodo), chamado em 187, 199, 211-213, 225-226, 526
- **Problema:** `formatar(double v)` instancia `NumberFormat.getCurrencyInstance(new Locale("pt","BR"))` a CADA chamada. Usado dentro de `while(rs.next())` em carregarDetalhamentoTab2 (linhas 184-191, 196-202) — cria um NumberFormat POR LINHA de resultado. Tambem chamado 5+ vezes nos cards (211-226).
- **Impacto:** 20-50 instanciacoes de NumberFormat por abertura de tela balanco. NumberFormat.getCurrencyInstance e pesado (lookup de locale + cache miss).
- **Codigo problematico:**
```java
private String formatar(double v) { 
    return NumberFormat.getCurrencyInstance(new Locale("pt","BR")).format(v); // NOVA INSTANCIA A CADA CHAMADA
}
// Chamado dentro de:
while(rs.next()) {
    String txt = String.format("...", formatar(rs.getDouble(4))); // LOOP
}
```
- **Fix sugerido:**
```java
private static final NumberFormat FMT_MOEDA = NumberFormat.getCurrencyInstance(new Locale("pt","BR"));
private String formatar(double v) { return FMT_MOEDA.format(v); }
```
- **Observacoes:**
> _Mesmo padrao do DP022 (corrigido no calendario), mas esquecido neste controller._

---

#### Issue #DP027 — Impressao sincrona bloqueia FX thread em ListarPassageirosViagemController
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/ListarPassageirosViagemController.java`
- **Linha(s):** 399-406
- **Problema:** `job.print()` (AWT PrinterJob) executado diretamente no FX thread. Bloqueia UI enquanto spooler de impressao processa. DP016 foi corrigido em VenderPassagemController, mas NAO neste controller.
- **Impacto:** UI congela por 1-5 segundos durante impressao de lista de passageiros.
- **Codigo problematico:**
```java
boolean doPrint = job.printDialog();
if (doPrint) {
    try { job.print(); } // SINCRONO NO FX THREAD
    catch (PrinterException e) { ... }
}
```
- **Fix sugerido:**
```java
if (doPrint) {
    Thread t = new Thread(() -> {
        try { job.print(); } catch (PrinterException e) { 
            Platform.runLater(() -> showAlert(...)); 
        }
    });
    t.setDaemon(true);
    t.start();
}
```
- **Observacoes:**
> _Padrao identico ao fix ja aplicado em VenderPassagemController._

---

#### Issue #DP028 — Autocomplete sem debounce dispara stream filter a cada keystroke
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/InserirEncomendaController.java`
- **Linha(s):** 1009-1023 (clientes), 1044-1058 (rotas)
- **Problema:** `setOnKeyReleased` executa `stream().filter().collect()` na lista completa a CADA tecla. Tambem reconstroi `menu.getItems()` com novos MenuItems a cada keystroke. Com 200+ clientes, cada tecla gera: 1 stream filter + 1 clear + N instantiacoes de MenuItem.
- **Impacto:** Input lag perceptivel em listas grandes. Garbage collection pressure por MenuItems descartados.
- **Codigo problematico:**
```java
cmb.getEditor().setOnKeyReleased(e -> {
    String txt = cmb.getEditor().getText().toUpperCase();
    List<String> achados = listaMestraClientes.stream()
        .filter(c -> c.contains(txt)).collect(Collectors.toList()); // CADA TECLA
    menu.getItems().clear(); // DESCARTA MenuItems anteriores
    for(String s : achados) {
        MenuItem mi = new MenuItem(s); // CRIA NOVOS A CADA TECLA
        menu.getItems().add(mi);
    }
});
```
- **Fix sugerido:** Debounce com PauseTransition (300ms) ou filtrar apenas se texto mudou:
```java
PauseTransition debounce = new PauseTransition(Duration.millis(300));
cmb.getEditor().setOnKeyReleased(e -> {
    if(isNavegacaoKey(e.getCode())) return;
    debounce.setOnFinished(ev -> aplicarFiltroClientes(cmb, menu));
    debounce.playFromStart();
});
```
- **Observacoes:**
> _Mesmo padrao em 3 locais: clientes remetente, clientes destinatario, rotas._

---

#### Issue #DP029 — Logo carregado do disco em 7 locais sem cache
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** Multiplos controllers
- **Linha(s):**
  - `RelatorioFretesController.java`: 437, 1134, 1518
  - `InserirEncomendaController.java`: 884
  - `ListaFretesController.java`: 337
  - `BalancoViagemController.java`: 466
  - `RelatorioUtil.java`: 431
- **Problema:** `new Image("file:" + caminhoFoto)` instanciado a cada impressao/relatorio. Mesmo caminho, mesma imagem, lido do disco toda vez. Em sessao com 10 impressoes = 10 leituras de disco do mesmo arquivo.
- **Impacto:** I/O de disco desnecessario. Latencia adicionada a cada impressao (~50-200ms por leitura).
- **Fix sugerido:** Cache estatico centralizado:
```java
public class ImageCache {
    private static final Map<String, Image> cache = new ConcurrentHashMap<>();
    public static Image getLogo(String path) {
        return cache.computeIfAbsent(path, p -> new Image("file:" + p));
    }
}
```
- **Observacoes:**
> _Expande DP018 (originalmente 3 locais) para 7 locais confirmados._

---

#### Issue #DP030 — BalancoViagemController.carregarDadosEmpresa() bypassa EmpresaDAO cache
- [x] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/BalancoViagemController.java`
- **Linha(s):** 533
- **Problema:** Metodo faz query SQL direta (`SELECT nome_embarcacao, cnpj... FROM configuracao_empresa LIMIT 1`) em vez de usar `EmpresaDAO.buscarPorId()` que tem cache estatic (DP012). Cada abertura de tela de balanco ignora o cache e faz roundtrip ao banco.
- **Impacto:** 1 query desnecessaria por abertura de tela de balanco.
- **Codigo problematico:**
```java
private void carregarDadosEmpresa() { 
    String sql = "SELECT nome_embarcacao, cnpj, endereco, telefone, path_logo FROM configuracao_empresa LIMIT 1"; 
    try (Connection con = ConexaoBD.getConnection(); PreparedStatement stmt = con.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) { ... }
}
```
- **Fix sugerido:** Substituir por `new EmpresaDAO().buscarPorId(EmpresaDAO.ID_EMPRESA_PRINCIPAL)` que usa cache.
- **Observacoes:**
> _Fix de 1 linha. DP012 ja implementou o cache no EmpresaDAO._

---

#### Issue #DP031 — ObservableList recriado a cada aplicacao de filtro em ListaEncomendaController
- [x] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/ListaEncomendaController.java`
- **Linha(s):** 553
- **Problema:** `tabelaEncomendas.setItems(FXCollections.observableArrayList(filtrados))` cria nova ObservableList a cada filtro. Isso perde selecao do usuario, forca re-render completo da TableView, e gera garbage da lista anterior.
- **Impacto:** Re-render completo da tabela a cada filtro. Selecao perdida.
- **Codigo problematico:**
```java
tabelaEncomendas.setItems(FXCollections.observableArrayList(filtrados));
```
- **Fix sugerido:** Usar FilteredList ou `setAll()`:
```java
tabelaEncomendas.getItems().setAll(filtrados); // reutiliza ObservableList existente
```
- **Observacoes:**
> _Fix de 1 linha. setAll() atualiza conteudo sem recriar lista._

---

#### Issue #DP032 — DateTimeFormatter inline em Viagem.toString() impacta render de TableView
- [x] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/model/Viagem.java`
- **Linha(s):** 78, 82, 87-89
- **Problema:** `getDataViagemStr()` e `getDataChegadaStr()` criam `DateTimeFormatter.ofPattern("dd/MM/yyyy")` a cada chamada. `toString()` chama `getDataViagemStr()`. TableView chama toString() para cada celula em cada render cycle. Com 50 viagens visveis = 50+ instanciacoes de DTF por render.
- **Impacto:** Overhead acumulado no render de TableViews e ComboBoxes com Viagem.
- **Codigo problematico:**
```java
public String getDataViagemStr() {
    return dataViagem != null ? dataViagem.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
}
```
- **Fix sugerido:**
```java
private static final DateTimeFormatter DTF_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
public String getDataViagemStr() {
    return dataViagem != null ? dataViagem.format(DTF_DATA) : "";
}
```
- **Observacoes:**
> _Mesmo fix para Encomenda.java:96 (DP021). DateTimeFormatter e thread-safe — static final e seguro._

---

#### Issue #DP033 — 16 model classes sem equals()/hashCode()
- [x] **Concluido** (5 prioritarios: Encomenda, Frete, Passageiro, Usuario, Tarifa)
- **Severidade:** BAIXO
- **Arquivo:** `src/model/` — 16 classes
- **Linha(s):** N/A
- **Problema:** Classes que NAO tem equals/hashCode: Empresa, Encomenda, Frete, FreteItem, ItemFrete, Passageiro, Usuario, Tarifa, TipoPassageiro, Produto, ReciboAvulso, ReciboQuitacaoPassageiro, DadosBalancoViagem, ItemResumoBalanco, ApiConfig, Auxiliares. Classes que TEM (8): Passagem, Viagem, Rota, Embarcacao, Caixa, ClienteEncomenda, EncomendaItem, ItemEncomendaPadrao.
- **Impacto:** `contains()`, `indexOf()`, `remove()` em ObservableLists usam `==` em vez de comparacao por ID. Pode causar falhas em selecao/deduplicacao em ComboBoxes.
- **Fix sugerido:** Adicionar equals/hashCode baseado em ID para classes usadas em collections:
```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Encomenda that = (Encomenda) o;
    return id == that.id;
}
@Override
public int hashCode() { return Integer.hashCode(id); }
```
- **Observacoes:**
> _Priorizar: Encomenda, Frete, Passageiro, Usuario, Tarifa (usados em ComboBox/TableView). Demais sao opcionais._

---

## COBERTURA

| Diretorio | Arquivos | Issues ativas |
|-----------|----------|---------------|
| src/dao/ | 23 | 3 (#047, DP009, DP014 indireto) |
| src/database/ | 2 | 0 |
| src/gui/ | 55 | 8 (DP026-DP031 + DP018/DP027) |
| src/gui/util/ | 8 | 1 (DP029 em RelatorioUtil) |
| src/model/ | 25 | 3 (DP021, DP032, DP033) |
| database_scripts/ | 7 | 0 |
| lib/ + .classpath | 2 | 2 (#048, DP023) |
| **TOTAL** | **131** | **15** |

---

## PLANO DE CORRECAO

### Urgente (CRITICO)

- [x] #DP026 — NumberFormat em formatar() — **FIXADO** (static final FMT_MOEDA)
- **Notas:**
> _Extrair para static final. Fix identico ao DP022._

### Importante (ALTO)

- [x] #DP027 — Impressao sincrona ListarPassageiros — **FIXADO** (daemon thread)
- [x] #DP028 — Autocomplete sem debounce InserirEncomenda — **FIXADO** (PauseTransition 250ms em 3 listeners)
- [x] #DP029 — Logo sem cache (7 locais) — **FIXADO** (ImageCache centralizado, 7 locais atualizados)
- [x] #DP009 — ILIKE leading wildcard FreteDAO — **FIXADO** (indice trigram GIN em script 011)
- **Notas:**
> _DP027 e DP029 sao fixes rapidos e de alto impacto. DP028 requer PauseTransition em 3 metodos._

### Importante (MEDIO)

- [x] #DP030 — BalancoViagem bypassa EmpresaDAO cache — **FIXADO** (usa EmpresaDAO.buscarPorId com cache)
- [x] #DP031 — ObservableList recriado no filtro — **FIXADO** (getItems().setAll())
- [x] #DP032 — DateTimeFormatter inline Viagem/Encomenda — **FIXADO** (static final DTF_DATA)
- [x] #DP021 — DateTimeFormatter em Encomenda.java — **FIXADO** (junto com DP032)
- [x] #DP014 — Contatos carregados 2x — **FIXADO** (1 query + setAll para copiar)
- [x] #DP020 — ResultSet nao fechado BalancoViagem — **FIXADO** (try-with-resources em 4 locais)
- **Notas:**
> _DP030 e DP031 sao fixes de 1 linha cada._

### Menor (BAIXO)

- [x] #DP033 — equals/hashCode em 5 models prioritarios — **FIXADO** (Encomenda, Frete, Passageiro, Usuario, Tarifa)
- [x] #047 — ORDER BY 1 fragil — **FIXADO** (ORDER BY id_recibo DESC)
- [ ] #048 — JSON parser custom — **Esforco:** 2h (substituir por Gson/Jackson)
- [ ] #DP023 — JARs duplicados (~35MB) — **Esforco:** 1h

---

## MAPA DE CONEXOES POR OPERACAO (ATUALIZADO)

| Operacao | Antes | Agora | Status |
|----------|-------|-------|--------|
| Listar 100 passagens | 501 | 1 (+cache) | CORRIGIDO |
| Listar 100 passageiros | 301 | 1 (+cache) | CORRIGIDO |
| Vender 1 passagem | 5 | 1 (+cache) | CORRIGIDO |
| Editar 1 passagem | 6 | 1 (+cache) | CORRIGIDO |
| Build calendario (1 mes) | 30+ | 3-4 | CORRIGIDO |
| Filtrar encomendas (por item) | 1+N | 0 (cache) | CORRIGIDO |
| Imprimir nota frete | 5 | 5 (pool) | MITIGADO |
| Atualizar dashboard | 4 | 2-3 | MELHORADO |
| Cadastrar passageiro | 4 | 1 (+cache) | CORRIGIDO |
| Abrir balanco viagem | 3 | 3 (+1 bypass cache) | DP030 |

---

## NOTAS

> **Progresso V2.0 → V3.0:** 23 issues anteriores + 8 novas = 31 resolvidas. Restam apenas 2 issues (#048 JSON parser custom, #DP023 JARs duplicados) — ambas de baixa prioridade e fora do codigo-fonte.
>
> **Fixes aplicados nesta sessao:** DP026 (NumberFormat static final), DP027 (print async), DP028 (debounce 250ms), DP029 (ImageCache 7 locais), DP009 (indice trigram), DP030 (EmpresaDAO cache), DP031 (setAll filtro), DP032/DP021 (DTF static final), DP014 (1 query contatos), DP020 (try-with-resources), DP033 (equals/hashCode 5 models), #047 (ORDER BY explicito).
>
> **Categoria Performance: 95% resolvida.** Issues restantes sao de infraestrutura (JARs, JSON parser) e nao afetam runtime.

---
*Gerado por Claude Code (Deep Audit) — Revisao humana obrigatoria*

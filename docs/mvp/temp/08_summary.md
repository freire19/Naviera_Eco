# Resumo de Gaps — MVP Check v2.0

**Data:** 2026-04-08
**Projeto:** Sistema Embarcacao (Desktop JavaFX)

---

## Contagem Geral

| Status | Quantidade |
|--------|-----------|
| PRONTO | 12 |
| INCOMPLETO | 18 |
| FALTANDO | 8 |
| POS-MVP | — |

**Total de itens avaliados: 38**
**Prontidao MVP: ~32%**

---

## Por Dimensao

| Dimensao | PRONTO | INCOMPLETO | FALTANDO |
|----------|--------|-----------|----------|
| 1. Features Core (10) | 1 | 8 | 1 |
| 2. Fluxos Criticos (4) | 1 | 3 | 0 |
| 3. Infraestrutura (11) | 2 | 4 | 5 |
| 4. Seguranca (7) | 3 | 4 | 0 |
| 5. Estabilidade (5) | 2 | 3 | 0 |
| 6. UX Minima (5) | 3 | 0 | 2 |
| 7. Dependencias (4) | 1 | 3 | 0 |

---

## Bloqueadores de Lancamento

### CRITICOS (impedem uso)
1. **CadastroCaixa nao funcional** — DAOs comentados, dados hardcoded, sem persistencia. Bloqueia fluxo de caixa/pagamentos.
2. **Overbooking de assentos possivel** — Sem deteccao de conflito em VenderPassagem. Dois agentes podem vender mesmo assento.
3. **Sem seed de dados iniciais** — Setup novo requer inserts manuais no banco. Operador nao sabe o que criar.

### ALTOS (riscos operacionais serios)
4. **Balanco Viagem com dados incompletos** — Fontes de despesa faltando, pagamentos pendentes invisiveis. Decisoes baseadas em totais incorretos.
5. **Estorno nao reverte status upstream** — Passagem continua EMITIDA apos estorno. Estorno duplicado possivel.
6. **Error handling inconsistente** — VenderPassagemController (2000+ linhas) com apenas 2-3 try-catch. Crash possivel em edge cases.
7. **Graceful shutdown ausente** — System.exit(0) sem chamar ConexaoBD.shutdown(). Transacoes podem ser orfanadas.

### MEDIOS (devem ser resolvidos antes de producao)
8. **db.properties commitado no git** — Credenciais em plaintext no repositorio.
9. **Sem statement timeout** — Queries lentas podem travar app indefinidamente.
10. **Sem indicadores de loading** — App parece travado durante operacoes longas.
11. **Sem mascaras de input** — MascarasFX nao encontrado. Formatos invalidos causam erros de parse.
12. **Paths absolutos Windows no .classpath** — Quebra em Linux/Mac.

---

## Estimativa de Esforco (bloqueadores)

| Bloqueador | Esforco |
|-----------|---------|
| CadastroCaixa: restaurar integracao DAO | 2-3h |
| Deteccao de conflito de assentos | 3-4h |
| Script seed.sql com dados iniciais | 2-3h |
| Completar fontes do Balanco Viagem | 4-6h |
| Estorno reverter status + prevenir duplicata | 3-4h |
| Error handling em VenderPassagem/InserirEncomenda | 4-6h |
| Shutdown hook + ConexaoBD.shutdown() | 1-2h |
| Gitignore db.properties + docs | 1h |
| Statement timeout em DAOs | 1-2h |
| Indicadores de loading nos controllers | 3-4h |

**Total estimado para bloqueadores: ~25-35h de desenvolvimento**

---

## Pontos Fortes (o que ja funciona bem)

- BCrypt com protecao timing-attack
- Connection pooling com health check e timeout
- Try-with-resources consistente (zero connection leaks)
- PermissaoService centralizado com 3 niveis
- AlertHelper thread-safe com mensagens amigaveis
- 74 telas FXML com design profissional
- Session timeout de 8 horas
- 9 scripts de migracao SQL organizados
- Queries parametrizadas (sem SQL injection nos DAOs de negocio)
- Transacoes atomicas (EncomendaDAO.inserirComItens)

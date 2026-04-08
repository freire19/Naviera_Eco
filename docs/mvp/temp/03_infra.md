# Dim 3 — Infraestrutura

## Resumo: 2 PRONTO | 4 INCOMPLETO | 5 FALTANDO

---

### A. Deploy & Containerizacao
| Item | Status | Detalhe |
|------|--------|---------|
| Dockerfile | FALTANDO | Sem container image; desktop-only |
| docker-compose.yml | FALTANDO | Sem orquestracao DB + app |
| CI/CD | FALTANDO | Sem GitHub Actions ou pipeline |
| .env.example | INCOMPLETO | Apenas db.properties.example; paths hardcoded em .classpath |
| Build system | INCOMPLETO | Eclipse .project + .classpath (sem Maven/Gradle) |

### B. Database & Migrations
| Item | Status | Detalhe |
|------|--------|---------|
| Scripts de migracao | PRONTO | 9 scripts SQL numerados (001-009) em database_scripts/ |
| Versionamento de schema | INCOMPLETO | Sem tabela de versao; incerto quais scripts aplicados |
| Dados iniciais | FALTANDO | Sem seed data (viagens, embarcacoes); admin cria manualmente |
| Connection pooling | PRONTO | LinkedBlockingDeque com health check e timeout de 5s |
| Suporte a transacoes | PRONTO | setAutoCommit(false) + rollback implementado |

### C. Configuracao & Ambiente
| Item | Status | Detalhe |
|------|--------|---------|
| db.properties | INCOMPLETO | Existe com .example; mas credenciais em plaintext no git |
| Gestao de secrets | INCOMPLETO | Senha do DB em arquivo plaintext (db.properties) |
| Logging | INCOMPLETO | LogService existe; mas muitos printStackTrace() ainda presentes |
| Config de API | INCOMPLETO | Endpoints hardcoded em controllers |
| .classpath | INCOMPLETO | Paths absolutos Windows (C:/javafx-sdk-23.0.2/) |

### D. Documentacao
| Item | Status | Detalhe |
|------|--------|---------|
| README.md | FALTANDO | Sem README do projeto desktop (apenas naviera-api/README.md) |
| CLAUDE.md | PRONTO | Excelente contexto para devs |
| Guia de setup | FALTANDO | Sem passo-a-passo para rodar do zero |
| Diagrama ER | FALTANDO | Schema apenas nos scripts SQL |
| Relatorios de auditoria | PRONTO | docs/audits/ com analise detalhada |

### E. Monitoramento & Logging
| Item | Status | Detalhe |
|------|--------|---------|
| Logs de aplicacao | INCOMPLETO | LogService existe; nao integrado em todos os controllers |
| Logging estruturado | FALTANDO | Sem formato JSON; text concatenado |
| Rotacao de logs | FALTANDO | Logs sem rotacao |
| Metricas de erro | FALTANDO | Sem tracking de operacoes falhas |
| Trilha de auditoria | INCOMPLETO | log_estornos e log_passagens existem; cobertura incompleta |
| Health check | FALTANDO | Sem sondas (desktop app, menos critico) |

### F. Testes
| Item | Status | Detalhe |
|------|--------|---------|
| Testes unitarios | INCOMPLETO | src/tests/ com ~3 classes: TesteApp, TesteConexao, TesteConexaoPostgreSQL |
| Testes de integracao | FALTANDO | Sem testes DAO + DB |
| Testes de UI | FALTANDO | Sem TestFX ou similar |
| Dados de teste | FALTANDO | Sem fixtures |
| Cobertura | <5% | Suite minima |

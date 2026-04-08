# Dim 7 — Dependencias

## Resumo: 1 PRONTO | 3 INCOMPLETO | 0 FALTANDO

---

### JARs & Versionamento — PRONTO
- ✓ 45 JARs no diretorio lib/
- ✓ Versoes recentes e compativeis:
  - postgresql-42.7.5.jar (driver DB)
  - jbcrypt-0.4.jar (seguranca)
  - logback-core-1.2.3.jar + logback-classic-1.2.3.jar + slf4j-api-1.7.25.jar (logging)
  - jasperreports-6.21.3.jar + openpdf-1.3.32.jar + itext-2.1.7.jar (PDF/impressao)
  - vosk-0.3.38.jar + tess4j-3.4.8.jar (voz/OCR)
  - jackson-databind-2.15.3.jar + jackson-core-2.15.3.jar (JSON)
  - jfreechart-1.0.19.jar (graficos)
- ⚠ Possivel duplicacao: iText-2.1.7 e OpenPDF-1.3.32 (potencial conflito)
- ⚠ Sem gerenciamento automatico (Maven/Gradle) — dependencias transitivas manuais
- ⚠ 45 JARs e substancial — podem existir dependencias nao usadas

### .classpath & Paths — INCOMPLETO
- ✓ Todas as dependencias listadas no .classpath
- ✗ Paths absolutos Windows: C:/javafx-sdk-23.0.2/lib/javafx-*.jar
- ✗ Quebra em Linux/Mac sem modificacao
- ✗ Sem uso de variaveis de ambiente ou paths relativos para JavaFX SDK

### APIs Externas — INCOMPLETO
- ✓ SyncClient.java para sincronizacao com API remota
- ✓ sync_config.properties com URL servidor e token
- ✓ Sync agendado a cada 5 minutos (configuravel)
- ✓ CompletableFuture para operacoes async
- ⚠ URL padrao: http://localhost:8080 (sem HTTPS)
- ⚠ Token de autenticacao vazio na config padrao
- ⚠ Sem timeout nas chamadas HTTP
- ⚠ Sem modo offline-first
- ⚠ Sem fallback se API indisponivel

### PostgreSQL Down — INCOMPLETO
- ✓ Timeout de conexao: 5 segundos
- ✓ Mensagens genericas de erro ao usuario
- ✓ App continua rodando (nao crasha)
- ✗ Sem logica de retry com backoff exponencial
- ✗ Sem distincao entre erro transiente vs permanente para o usuario
- ✗ Sem mecanismo de health check
- ✗ Sem degradacao graciosa (operacoes read-only poderiam funcionar)
- ✗ Operacoes longas (impressao, exportacao) falham completamente sem retry

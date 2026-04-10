---
type: brand-audit
phase: deep
date: 2026-04-09
issues_confirmadas: 139
falsos_positivos: 9
novas_issues: 12
---

# Brand Deep Audit — Fase 2

## Resumo executivo
- Issues do scan: 156
- Confirmadas: 139
- Falsos positivos: 9 (descartados)
- Contextuais (aceitaveis): 8
- Novas issues encontradas: 12
- **Total a corrigir: 151**

---

## Issues confirmadas (prontas para correcao)

### PRIORIDADE 1 — FXML: Azul legado PROIBIDO (47 ocorrencias)

---

### VenderPassagem.fxml — 10 issues
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 18 | `-fx-background-color: #dce6ec` | `#F7FBF9` | BAIXO |
| 26 | `-fx-background-color: #0d47a1` | `#059669` | BAIXO |
| 143 | `textFill="#1976d2"` | `#059669` | BAIXO |
| 191 | `textFill="#1976d2"` | `#3D6B56` | BAIXO |
| 226 | `textFill="#e65100"` | `#B45309` | BAIXO |
| 287 | `-fx-text-fill: #d32f2f; -fx-border-color: #ef9a9a` | `#DC2626; #FEE2E2` | BAIXO |
| 321 | `-fx-background-color: #0d47a1` | `#059669` | BAIXO |
| 354 | `-fx-background-color: #1e88e5` | `#059669` | BAIXO |
| 367 | `-fx-background-color: #0d47a1` | `#059669` | BAIXO |
| 370 | `-fx-background-color: #0d47a1` | `#059669` | BAIXO |

### InserirEncomenda.fxml — 11 issues
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 22 | `-fx-background-color: #0056b3` | `#059669` | BAIXO |
| 23 | `-fx-background-color: #6c757d` | `#7BA393` | BAIXO |
| 24 | `textFill="#0056b3"` | `#059669` | BAIXO |
| 134 | `-fx-background-color: #0056b3` | `#059669` | BAIXO |
| 135 | `-fx-background-color: #0056b3` | `#059669` | BAIXO |
| 227 | `textFill="#0056b3"` | `#059669` | BAIXO |
| 245 | `-fx-background-color: #28a745` | `#047857` | BAIXO |
| 247 | `-fx-background-color: #007bff` | `#059669` | BAIXO |
| 249 | `-fx-background-color: #dc3545` | `#DC2626` | BAIXO |
| 253 | `-fx-background-color: #17a2b8` | `#0369A1` | BAIXO |
| 257 | `-fx-background-color: #343a40` | `#0F2620` | BAIXO |

### Login.fxml — 3 issues
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 22 | `-fx-background-color: #2b2b2b; -fx-border-color: #404040` | `#0F2D24; #059669` | MEDIO |
| 61 | `-fx-background-color: #cfd8dc; -fx-text-fill: #455a64` | `#E6F5ED; #3D6B56` | BAIXO |
| 66 | `-fx-background-color: #0d47a1` | `#059669` | BAIXO |

### FinanceiroEncomendas.fxml — 7 issues
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 32 | `-fx-border-color: #1565c0` | `#059669` | BAIXO |
| 46 | `-fx-background-color: #0d47a1` | `#059669` | BAIXO |
| 86 | `-fx-background-color: #2e7d32` | `#047857` | BAIXO |
| 90 | `-fx-background-color: #0d47a1` | `#059669` | BAIXO |
| 94 | `-fx-background-color: #ff9800` | `#F59E0B` | BAIXO |
| 98 | `-fx-background-color: #c62828` | `#DC2626` | BAIXO |
| 103 | `-fx-background-color: #546e7a` | `#7BA393` | BAIXO |

### FinanceiroFretes.fxml — 5 issues
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 32 | `-fx-border-color: #1565c0` | `#059669` | BAIXO |
| 46 | `-fx-background-color: #0d47a1` | `#059669` | BAIXO |
| 84 | `-fx-background-color: #2e7d32` | `#047857` | BAIXO |
| 91 | `-fx-background-color: #0d47a1` | `#059669` | BAIXO |
| 103 | `-fx-background-color: #546e7a` | `#7BA393` | BAIXO |

### FinanceiroEntrada.fxml — 2 issues
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 30 | `textFill="#1a237e"` | `#0F2620` | BAIXO |
| 43 | `-fx-background-color: #1a237e` | `#059669` | BAIXO |

### FinanceiroPassagens.fxml — 1 issue
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 89 | `-fx-background-color: #0d47a1` | `#059669` | BAIXO |

### FinanceiroSaida.fxml — 5 issues
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 119 | `-fx-background-color: #4caf50` | `#047857` | BAIXO |
| 141 | `-fx-background-color: #ef6c00` | `#B45309` | BAIXO |
| 149 | `-fx-background-color: #1976d2` | `#059669` | BAIXO |
| 175 | `-fx-background-color: #616161` | `#7BA393` | BAIXO |
| 180 | `-fx-background-color: #0d47a1` | `#059669` | BAIXO |

### BalancoViagem.fxml — 2 issues
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 31 | `-fx-background-color: #0d47a1` | `#059669` | BAIXO |
| 127 | `textFill="#f57f17"` | `#B45309` | BAIXO |

### ExtratoClienteEncomenda.fxml — 2 issues
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 38 | `-fx-background-color: #0d47a1` | `#059669` | BAIXO |
| 61 | `-fx-background-color: #0d47a1` | `#059669` | BAIXO |

### ExtratoPassageiro.fxml — 3 issues
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 37 | `-fx-background-color: #0d47a1` | `#059669` | BAIXO |
| 69 | `-fx-background-color: #0d47a1` | `#059669` | BAIXO |
| 100 | `-fx-background-color: #66bb6a` | `#047857` | BAIXO |

### GerarReciboAvulso.fxml — 1 issue
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 71 | `-fx-background-color: #0d47a1` | `#059669` | BAIXO |

### HistoricoEstornos.fxml — 1 issue
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 32 | `-fx-background-color: #0d47a1` | `#059669` | BAIXO |

### HistoricoEstornosFretes.fxml — 1 issue
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 32 | `-fx-background-color: #0d47a1` | `#059669` | BAIXO |

### HistoricoEstornosPassagens.fxml — 1 issue
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 31 | `-fx-background-color: #0d47a1` | `#059669` | BAIXO |

### ListaEncomenda.fxml — 3 issues
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 53 | `-fx-text-fill: #d84315` | `#DC2626` | BAIXO |
| 74 | `-fx-background-color: #0056b3` | `#059669` | BAIXO |
| 125 | `-fx-text-fill: #0078d7` | `#059669` | BAIXO |

### ListaFretes.fxml — 2 issues
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 64 | `-fx-text-fill:#0078D7` | `#059669` | BAIXO |
| 82 | `-fx-text-fill:#0078D7` | `#059669` | BAIXO |

### TabelaPrecoFrete.fxml — 2 issues
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 64 | `-fx-text-fill: #0d47a1` | `#059669` | BAIXO |
| 111 | `-fx-text-fill: #0d47a1` | `#059669` | BAIXO |

### TelaPrincipal.fxml — 2 issues
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 331 | `fill="#e3f2fd" stroke="#2196f3"` | `fill="#E6F5ED" stroke="#059669"` | BAIXO |

### RelatorioFretes.fxml — 5 issues
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 53 | `-fx-background-color: #0d47a1` | `#059669` | BAIXO |
| 55 | `-fx-background-color: #0d47a1` | `#059669` | BAIXO |
| 63 | `-fx-background-color: #7b1fa2` | `#7BA393` | BAIXO |
| 67 | `-fx-background-color: #d32f2f` | `#DC2626` | BAIXO |
| 98 | `textFill="#00ff00"` | `#4ADE80` | BAIXO |

### RegistrarPagamentoEncomenda.fxml — 3 issues
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 21 | `-fx-background-color: #1a3c7d` | `#059669` | BAIXO |
| 38 | `textFill="#0d47a1"` | `#059669` | BAIXO |
| 43 | `textFill="#0d47a1"` | `#059669` | BAIXO |

### GestaoFuncionarios.fxml — 1 issue
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 245 | `textFill="#0277bd"` | `#059669` | BAIXO |

### ConfigurarApi.fxml — 6 issues
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 19 | `-fx-text-fill: #2c3e50` | `#0F2620` | BAIXO |
| 20 | `-fx-text-fill: #7f8c8d` | `#7BA393` | BAIXO |
| 23 | `-fx-text-fill: #e67e22` | `#B45309` | BAIXO |
| 39 | `-fx-background-color: #3498db` | `#0369A1` | BAIXO |
| 53 | `-fx-background-color: #9b59b6` | `#7BA393` | BAIXO |
| 188 | `-fx-background-color: #e74c3c` | `#DC2626` | BAIXO |

### CadastroFrete.fxml — 1 issue
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 165 | `-fx-background-color: #00796b` | `#047857` | BAIXO |

---

### PRIORIDADE 2 — CSS: Cores off-palette e legado (38 issues)

---

### resources/css/dark.css — 6 issues
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 106 | `rgba(13, 86, 223, 0.6)` dropshadow | `rgba(5,150,105,0.4)` | BAIXO |
| 479 | `rgba(77,184,255,0.5)` dropshadow | `rgba(52,211,153,0.3)` | BAIXO |
| 364 | `#b71c1c` border | `#DC2626` | BAIXO |
| 366 | `#ef5350` text | `#EF4444` | BAIXO |
| 386 | `#ffcc80` border/bg | `#FBBF24` | BAIXO |
| 869 | `#3d2626` bg | `#450a0a` | BAIXO |

### resources/css/main.css — 30 issues
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 49, 446 | `#0d9668` | `#059669` | BAIXO |
| 65 | `#cc0000` | `#DC2626` | BAIXO |
| 69 | `#ff0000` | `#EF4444` | BAIXO |
| 83, 221, 851 | `#065f46` | `#047857` | BAIXO |
| 95, 638, 644, 659 | `#263238` | `#0F2620` | BAIXO |
| 126, 246, 504, 755, 818, 843, 872 | `#cfd8dc` | `#A7F3D0` | MEDIO |
| 161, 175, 252, 560, 580, 639 | `#B0BEC5` | `#7BA393` | MEDIO |
| 169, 308 | `#546e7a` | `#3D6B56` | BAIXO |
| 202, 210 | `#455A64` | `#3D6B56` | BAIXO |
| 301 | `#064e3b` | `#052E22` | BAIXO |
| 307 | `#78909c` | `#7BA393` | BAIXO |
| 673 | `#e0f2f1` | `#E6F5ED` | BAIXO |
| 675 | `#b2dfdb` | `#A7F3D0` | BAIXO |
| 681, 798 | `#fff3e0` | `#FEF3C7` | BAIXO |
| 683 | `#ffe0b2` | `#FBBF24` | BAIXO |
| 745 | `#ffebee` | `#FEE2E2` | BAIXO |
| 799 | `#ffb74d` | `#F59E0B` | BAIXO |
| 264 | `#fff9c4` | `#FEF3C7` | BAIXO |
| 265 | `#fbc02d` | `#FBBF24` | BAIXO |
| 276 | `#f9a825` | `#F59E0B` | BAIXO |
| 886 | `#fce4ec` | `#FEE2E2` | BAIXO |
| 887 | `#f48fb1` | `#EF4444` | BAIXO |

### src/gui/ConfigurarSincronizacao.css — 2 issues
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 7, 11, 16 | `#f5f6fa` | `#F7FBF9` | BAIXO |
| 125 | `#9C27B0` (purple) | `#059669` | BAIXO |

---

### PRIORIDADE 3 — Java Controllers (34 issues)

---

### GerarReciboAvulsoController.java — 12 issues (constante propaga)
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 102 | `COR_AZUL_SISTEMA = "#1a3c7d"` | Renomear para `COR_PRIMARIA = "#059669"` | MEDIO |
| 254 | usa COR_AZUL_SISTEMA em btnPrint | Automatico via constante | BAIXO |
| 291 | usa COR_AZUL_SISTEMA em btnA4 | Automatico via constante | BAIXO |
| 295 | `#78909c` | `#7BA393` | BAIXO |
| 515 | usa COR_AZUL_SISTEMA em container | Automatico via constante | BAIXO |
| 523 | usa COR_AZUL_SISTEMA em lblEmp | Automatico via constante | BAIXO |
| 531 | usa COR_AZUL_SISTEMA em lblN | Automatico via constante | BAIXO |
| 535 | usa COR_AZUL_SISTEMA em valorBox | Automatico via constante | BAIXO |
| 545 | usa COR_AZUL_SISTEMA em div stroke | Automatico via constante | BAIXO |
| 580 | usa COR_AZUL_SISTEMA em lbl | Automatico via constante | BAIXO |
| 583 | `Color.web("#B0BEC5")` | `Color.web("#7BA393")` | BAIXO |
| 299 | `#37474f` | `#3D6B56` | BAIXO |

### TelaPrincipalController.java — 8 issues
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 298 | `"#0d56df"` (dark mode borda) | `"#34D399"` | MEDIO |
| 299 | `"#121a33"` (dark mode hover) | `"#0A1F18"` | MEDIO |
| 301 | `"#b71c1c"` (feriado dark) | `"#DC2626"` | BAIXO |
| 302 | `"#0d56df"` (dark mode hoje) | `"#34D399"` | MEDIO |
| 357 | `Color.ORANGE` | `Color.web("#F59E0B")` | BAIXO |
| 363 | `"#ffcdd2"` / `"#f57f17"` | `"#FEE2E2"` / `"#B45309"` | BAIXO |
| 387 | `"#1a3c7d"` (dark badge bg) | `"#0F2D24"` | MEDIO |
| 400 | `"#b71c1c"` / `"#ffebee"` | `"#DC2626"` / `"#FEE2E2"` | BAIXO |

### GestaoFuncionariosController.java — 2 issues
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 654 | `-fx-border-color: #bbdefb` | `#A7F3D0` | BAIXO |
| 690 | `Color.RED` | `Color.web("#DC2626")` | BAIXO |

### RegistrarPagamentoEncomendaController.java — 1 issue
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 148 | `#1a3c7d` | `#059669` | BAIXO |

### TabelaPrecosEncomendaController.java — 2 issues
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 438 | `#1F3A56` (text fill) | `#0F2620` | BAIXO |
| 539 | `#1F3A56` (bg) | `#059669` | BAIXO |

### BalancoViagemController.java — 5 issues
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 156 | `#EF6C00` | `#B45309` | BAIXO |
| 372 | `Color.GREEN` | `Color.web("#059669")` | BAIXO |
| 374 | `Color.BLUE` | `Color.web("#0369A1")` | BAIXO |
| 375 | `Color.ORANGE` | `Color.web("#F59E0B")` | BAIXO |
| 449-452 | Color.GREEN/RED/BLUE borders | `#059669` / `#DC2626` / `#0369A1` | MEDIO |

### ListaFretesController.java — 4 issues
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 151 | `#e8f5e9` | `#D1FAE5` | BAIXO |
| 161 | `#cfd8dc` | `#A7F3D0` | BAIXO |
| 391 | `Color.BLUE` | `Color.web("#0369A1")` | BAIXO |
| 392-393 | `Color.GREEN` / `Color.RED` | `Color.web("#059669")` / `Color.web("#DC2626")` | BAIXO |

### InserirEncomendaController.java — 6 issues
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 416 | `#607d8b` | `#7BA393` | BAIXO |
| 418 | `#ff8f00` | `#F59E0B` | BAIXO |
| 421 | `#fbc02d` | `#FBBF24` | BAIXO |
| 422 | `#c49000` | `#B45309` | BAIXO |
| 423 | `#fbc02d` | `#FBBF24` | BAIXO |
| 1613 | `#ff9800` | `#F59E0B` | BAIXO |

### FinanceiroSaidaController.java — 1 issue
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 710 | `#7C3AED` (purple) | `#7BA393` | BAIXO |

### RelatorioPassagensController.java — 1 issue
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 384 | Bootstrap colors array | `#047857, #F59E0B, #DC2626, #0369A1, #7BA393` | MEDIO |

### ListaEncomendaController.java — 2 issues
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 310 | `#003d82` (border) | `#059669` | BAIXO |
| 392 | `#d84315` | `#DC2626` | BAIXO |

### ConfigurarSincronizacaoController.java — 3 issues
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 81 | `#FF9800` | `#F59E0B` | BAIXO |
| 90, 106, 191 | `#F44336` | `#EF4444` | BAIXO |
| 102, 172 | `#4CAF50` | `#059669` | BAIXO |

### VenderPassagemController.java — 2 issues
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 1855 | `Color.RED` | `Color.web("#DC2626")` | BAIXO |
| 1856 | `Color.GREEN` | `Color.web("#059669")` | BAIXO |

### RelatorioFretesController.java — 3 issues
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 900 | `Color.LIGHTGREEN` / `Color.ORANGE` | `Color.web("#4ADE80")` / `Color.web("#F59E0B")` | BAIXO |
| 968, 1069 | `Color.RED` | `Color.web("#DC2626")` | BAIXO |

### RelatorioEncomendaGeralController.java — 3 issues
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 432 | `Color.RED` / `Color.GREEN` | `Color.web("#DC2626")` / `Color.web("#059669")` | BAIXO |
| 643 | `Color.LIGHTGREEN` / `Color.ORANGE` | `Color.web("#4ADE80")` / `Color.web("#F59E0B")` | BAIXO |
| 699, 722 | `Color.RED` | `Color.web("#DC2626")` | BAIXO |

### util/RelatorioUtil.java — 1 issue
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 501 | `Color.LIGHTGREEN` / `Color.ORANGE` | `Color.web("#4ADE80")` / `Color.web("#F59E0B")` | BAIXO |

### util/StatusPagamentoView.java — 1 issue
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 10 | `#ef6c00` | `#B45309` | BAIXO |

### TelaGerenciarAgendaController.java — 1 issue
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 80 | `#ffcdd2` | `#FEE2E2` | BAIXO |

---

### PRIORIDADE 4 — App React (5 issues)

---

### naviera-app/src/App.jsx — 5 issues
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 23 | `skShine: "#dff0e6"` | `"#EEF7F2"` | BAIXO |
| 36 | `skShine: "#14332a"` | `"#0A1F18"` | BAIXO |
| 365 | gradient 160deg 4 cores | `linear-gradient(160deg, #040D0A 0%, #0F2D24 30%, #040D0A 70%, #0A1F18 100%)` | MEDIO |
| 367 | `#1a6b5a` (rio stroke) | `rgba(5,150,105,0.5)` | BAIXO |
| 368 | `#2a9d7e` (rio stroke) | `rgba(52,211,153,0.6)` | BAIXO |

---

## Novas issues [NOVO]

Issues que o scan nao pegou:

### GerarReciboAvulsoController.java [NOVO]
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 254, 291, 515, 523, 531, 535, 545, 580 | `COR_AZUL_SISTEMA` (9 usos alem da def) | Corrigidos ao mudar a constante L102 | BAIXO |
| 299 | `#37474f` | `#3D6B56` | BAIXO |

### TelaPrincipalController.java [NOVO]
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 299 | `#121a33` (dark hover bg) | `#0A1F18` | MEDIO |
| 400 | `#b71c1c` / `#ffebee` (vencido badge) | `#DC2626` / `#FEE2E2` | BAIXO |
| 413-420 | 8 Material palette cores (dark mode) | Brand dark equivalents | MEDIO |

### ListaEncomenda.fxml [NOVO]
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 52 | `-fx-text-fill: #2e7d32` (radio "Entregues") | `#047857` | BAIXO |

### Login.fxml [NOVO]
| Linha | Atual | Correcao | Risco |
|-------|-------|----------|-------|
| 22 | `#2b2b2b` / `#404040` (header) | `#0F2D24` / `#059669` | MEDIO |
| 61 | `#cfd8dc` / `#455a64` (btn sair) | `#E6F5ED` / `#3D6B56` | BAIXO |

---

## Falsos positivos descartados

| Arquivo | Linha | Cor | Motivo |
|---------|-------|-----|--------|
| ConfigurarApi.fxml | 162 | `-fx-font-family: monospace` | Nao e cor, apenas fonte — monospace em contexto tecnico |
| ConfigurarApi.fxml | 73 | `#27ae60` | Classificado como MEDIA no scan, confirmado — mantido como issue |
| relatorio.css | 2 | `#f0f2f5` | Neutro cinza muito proximo de #F7FBF9 — aceitavel |
| main.css (impressao) | varios | `#000000`, `#ffffff` | Cores neutras de impressao |
| Java (impressao) | varios | `Courier New`, `Arial` | Fontes de impressao termica — contexto justifica |
| App.jsx SVG | 365-368 | Gradient + rios | Decorativo — cores green-family, nao azul legado |
| main.css | 322-345 | `#ddd`, `#ccc`, `#333` etc | Neutros cinza genericos |
| dark.css | 386 | `#ffcc80` | Reclassificado: realmente e off-palette, MANTIDO como issue |
| FinanceiroSaida.fxml | 175 | `#616161` | Cinza neutro puro, mas substitui por brand para consistencia |

---

## Plano de correcao

Ordem sugerida (do mais seguro ao mais arriscado):

### Fase A — Replace direto, risco BAIXO (estimativa: ~120 correcoes)
1. **FXML files** — search-and-replace de cores proibidas por brand equivalents
2. **CSS main.css** — replace de Material Design colors por palette
3. **CSS dark.css** — replace das 2 rgba legado + 4 off-palette
4. **CSS ConfigurarSincronizacao.css** — 2 fixes
5. **Java controllers** — replace `Color.RED/GREEN/BLUE/ORANGE` por `Color.web("#hex")`
6. **Java `COR_AZUL_SISTEMA`** — renomear constante + mudar valor (corrige 10 locais de uma vez)

### Fase B — Verificacao visual necessaria, risco MEDIO (estimativa: ~20 correcoes)
1. **main.css** — `#cfd8dc` → `#A7F3D0` e `#B0BEC5` → `#7BA393` (mudar gray-blue para green pode alterar visual)
2. **TelaPrincipalController.java** — dark mode calendar colors
3. **Login.fxml** — header styling
4. **BalancoViagemController.java** — border styling em cards
5. **App.jsx** — SVG map gradient (decorativo)

### Fase C — Requer teste funcional (estimativa: ~11 correcoes)
1. **RelatorioPassagensController.java** — array de cores para graficos/relatorios
2. **TelaPrincipalController.java lines 413-420** — dark mode calendar Material palette (8 cores)

# Dim 1 — Funcionalidades Core

## Resumo: 1 PRONTO | 8 INCOMPLETO | 1 FALTANDO

---

### 1. Vender Passagem — INCOMPLETO
- ✓ CRUD completo em PassagemDAO com RETURN_GENERATED_KEYS
- ✓ Validacao: nome, data nascimento, acomodacao, rota, agente, valor > 0
- ✓ Numero bilhete via sequence SQL com fallback MAX+1
- ✓ Finalizacao pagamento (dinheiro/PIX/cartao)
- ✓ Background thread loading previne UI freeze
- ⚠ Sem validacao de idade vs tipo_passagem (desconto crianca/estudante)
- ⚠ Sem deteccao de conflito de assento — overbooking possivel
- ⚠ Falha de impressao silenciosa (bilhete salvo mas nao impresso)
- ⚠ Sem prevencao de passageiros duplicados na mesma viagem

### 2. Inserir Encomenda — INCOMPLETO
- ✓ Formulario FXML com remetente/destinatario, tabela de itens
- ✓ Insert atomico via EncomendaDAO.inserirComItens() com rollback
- ✓ Botoes de audio/OCR para reconhecimento de itens (Tess4J + Vosk)
- ⚠ Sem checagem de itens duplicados na tabela
- ⚠ Validacao de destinatario aceita qualquer string
- ⚠ Funcoes audio/OCR presentes mas sem error handling (dark code)
- ⚠ Parse de valores fragil (replace manual de separador de milhar)

### 3. Cadastro Frete — INCOMPLETO
- ✓ FXML com remetente, rota, conferente, tabela dinamica de itens
- ✓ Queries parametrizadas (sem SQL injection)
- ✓ Botoes de foto e XML para documentos fiscais
- ⚠ Logica de precificacao de itens nao visivel
- ⚠ Variavel estatica staticNumeroFreteParaAbrir nao e thread-safe
- ⚠ Campo remetente_nome_temp indica codigo legado/migrado parcialmente
- ⚠ Sem validacao de arquivo (tamanho, formato) em upload

### 4. Cadastro Viagem — PRONTO
- ✓ CRUD completo (Novo/Editar/Deletar/Salvar)
- ✓ Validacoes: data >= hoje, embarcacao & rota obrigatorios
- ✓ Cache de viagem ativa evita queries redundantes
- ✓ Background loading previne UI freeze
- ⚠ Soft delete nao implementado (viagem deletada pode orfanar passagens)
- ⚠ Exclusividade de is_atual nao garantida no banco

### 5. Cadastro Caixa — FALTANDO
- ✓ Form UI existe com tabela e botoes
- ✗ DAOs completamente comentados
- ✗ Dados hardcoded de teste em vez de query ao banco
- ✗ Botao Salvar imprime debug mas nao chama DAO
- ✗ **Feature nao funcional — bloqueador critico**

### 6. Balanco Viagem — INCOMPLETO
- ✓ DAO busca 4 fontes de receita: passagens, encomendas, fretes
- ✓ Categorias de despesa com agregacao SUM
- ✓ Tabs de resumo e detalhamento com color coding
- ✓ Graficos e impressao presentes
- ⚠ Dados incompletos: aviso exibido mas permite continuar
- ⚠ Fontes de despesa faltando (salarios, manutencao, etc.)
- ⚠ Pagamentos pendentes nao visiveis
- ⚠ Info da empresa hardcoded no controller

### 7. Financeiro (5 telas) — INCOMPLETO
- ✓ Controllers para Entradas, Saidas, Passagens, Encomendas, Fretes
- ✓ Filtros por viagem, categoria, forma_pagamento, caixa
- ✓ Permission check: PermissaoService.isFinanceiro()
- ✓ Graficos (pie/bar) para visualizacao
- ⚠ Sem filtro de periodo/data (apenas "Todas as Viagens")
- ⚠ Reconciliacao impossivel (sem comparar saldo caixa vs banco)
- ⚠ Valores negativos aceitos sem validacao
- ⚠ Sem trilha de auditoria para edicoes financeiras

### 8. Login / Autenticacao — INCOMPLETO
- ✓ ComboBox carrega usuarios ativos
- ✓ Verificacao de senha via BCrypt
- ✓ Sessao armazenada em SessaoUsuario
- ✓ Timeout de sessao de 8 horas implementado
- ⚠ Sem bloqueio por tentativas falhas (brute-force possivel)
- ⚠ Sem troca de senha pelo usuario
- ⚠ Sem botao de logout explicito na tela principal

### 9. Estornos — INCOMPLETO
- ✓ Modal com valor, motivo, forma de devolucao
- ✓ Autenticacao de Gerente via BCrypt
- ✓ Escrita em log_estornos com trilha de auditoria
- ✓ Validacao: valor <= valor_pago + tolerancia
- ⚠ Nao reverte status upstream (passagem continua EMITIDA)
- ⚠ Sem prevencao de estorno duplicado na mesma transacao
- ⚠ Sem geracao de comprovante de estorno

### 10. Cadastros de Entidades — INCOMPLETO
| Entidade | CRUD | Validacao | Gaps |
|----------|------|-----------|------|
| Passageiro | ✓ | ~50% | Sem check de CPF duplicado |
| Cliente | ✓ | ~60% | Sem constraint de unicidade |
| Empresa | ✓ | ~40% | Sem validacao CNPJ |
| Conferente | ✓ | ~30% | Sem gate de permissao |
| Usuario | ✓ | ~70% | BCrypt OK, sem complexidade de senha |
| Rota | ✓ | ~70% | Origem/destino obrigatorios |
| Tarifa | ✓ | ~50% | Taxas recomendadas sem enforcement |

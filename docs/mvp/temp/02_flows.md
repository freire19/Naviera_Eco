# Dim 2 — Fluxos Criticos

## Resumo: 1 PRONTO | 3 INCOMPLETO

---

### A. Primeiro Uso / Onboarding — INCOMPLETO
- ✗ Sem script SQL para criar admin inicial (insert manual na tabela usuarios)
- ✗ Sem requisito de complexidade de senha
- ✗ Sem guia de boas-vindas ou dicas de uso
- ✗ Dashboard pode falhar se tabelas vazias (sem empty state gracioso)
- ✗ Sem seeder de dados mestres (viagens, embarcacoes, rotas)
- ✗ Usuario deve criar dados manualmente antes de vender passagens
- **Risco:** Operador novo pode nao saber que precisa pre-criar dados mestres

### B. Fluxo Principal E2E — PRONTO (com ressalvas)
**Sequencia happy path funciona:**
1. ✓ Cadastro Viagem → criar viagem (data, embarcacao, rota)
2. ✓ Vender Passagem → vender bilhete para viagem
3. ✓ Inserir Encomenda → registrar encomenda
4. ✓ Cadastro Frete → registrar frete
5. ✓ Registrar Pagamento → marcar pagamentos recebidos
6. ✓ Balanco Viagem → ver resumo financeiro
7. ✓ Estorno → reverter pagamento se necessario

**Edge cases que quebram o fluxo:**
- ⚠ Sem viagem padrao selecionada → vende bilhete para viagem null
- ⚠ Edicao concorrente → cache nao invalidado em update
- ⚠ Crash do app entre salvar passagem e pagamento → race condition
- ⚠ Sem acao "Fechar Viagem" para impedir novas vendas retroativas

### C. Tratamento de Erros do Usuario — INCOMPLETO
| Cenario | Tratamento |
|---------|------------|
| Campo obrigatorio vazio | ✓ Alert + foco no campo |
| Preco negativo | ✗ Sem validacao; aceita via parse |
| Passageiro duplicado | ✗ Permite; sem check de unicidade |
| Overbooking de assentos | ✗ Sem deteccao de conflito |
| Viagem errada selecionada | ✗ Bilhete aparece no historico errado |
| Conexao DB perdida | ✗ SQLException silenciosa; UI mostra "Erro ao Carregar" |
| Impressora offline | ✗ Falha silenciosa; bilhete salvo mas nao impresso |
| Data invalida | ⚠ DatePicker previne; TextField (nascimento) nao |
| Limite estorno excedido | ✓ Valida <= valor_pago + tolerancia |

**Cobertura estimada: ~50% dos erros de usuario**

### D. Saida / Encerramento — INCOMPLETO
- ✗ System.exit(0) sem prompt de confirmacao
- ✗ Alteracoes nao salvas perdidas silenciosamente
- ✗ Sem procedimento de fechamento de viagem
- ✗ Sem logout; apenas encerramento do app
- ✗ Sessao nao limpa no encerramento
- ✗ Pool de conexoes nao drenado (ConexaoBD.shutdown() existe mas nao e chamado)

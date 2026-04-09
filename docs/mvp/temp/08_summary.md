# Resumo de Gaps — MVP App Naviera

| Status | Quantidade |
|--------|-----------|
| PRONTO | 39 |
| INCOMPLETO | 20 |
| FALTANDO | 27 |
| POS-MVP | 1 |

## Bloqueador #1 — Telas usando dados MOCK (critico)
**13 das 19 funcionalidades core nao consomem a API.**
Todas as telas apos login mostram dados hardcoded. O usuario ve informacoes falsas. A API existe e funciona, mas o frontend nao a chama.

Telas afetadas:
- HomeCPF (viagens, amigos, encomendas)
- PassagensCPF (viagens, rotas, tarifas)
- MapaCPF (embarcacoes)
- AmigosCPF (amigos, chat)
- HomeCNPJ (fretes, pedidos, lojas)
- PedidosCNPJ (pedidos)
- LojasParceiras (lojas)
- FinanceiroCNPJ (financeiro)
- LojaCNPJ (minha loja)

## Bloqueador #2 — Nomes hardcoded
- HomeCPF mostra "Renato Freire" em vez do usuario logado
- HomeCNPJ mostra "Comercial Rio Negro LTDA" em vez da empresa logada

## Bloqueador #3 — Token nao persiste
- Refresh da pagina faz logout (token so em useState, nao em localStorage)

## Bloqueador #4 — API de Amigos/Chat inexistente
- Tabela amigos_app existe no banco
- Nenhum endpoint na API para amigos ou chat
- Pode ser movido para POS-MVP se necessario

## Bloqueador #5 — API Financeiro inexistente
- Nenhum endpoint financeiro na API
- Pode ser movido para POS-MVP se necessario

## Estimativa de esforco para resolver bloqueadores

| Tarefa | Esforco |
|--------|---------|
| Conectar telas CPF a API (viagens, encomendas, embarcacoes) | 4-6h |
| Conectar telas CNPJ a API (fretes, lojas, pedidos) | 4-6h |
| Persistir token em localStorage | 30min |
| Usar nome do usuario logado no home | 30min |
| Criar API de amigos (CRUD basico) | 3-4h |
| Criar API financeiro (basico) | 3-4h |
| **Total bloqueadores** | **~15-21h** |

Sem amigos/chat e financeiro (POS-MVP): **~9-13h**

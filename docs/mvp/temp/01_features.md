# Dim 1 — Funcionalidades Core

## Resumo
O app tem UI completa para todas as features, mas **apenas 3 telas consomem a API real** (Login, Cadastro, Perfil). As demais usam dados hardcoded (constantes FRIENDS, ENCOMENDAS, VIAGENS, BOATS, FRETES, LOJAS, PEDIDOS, CHAT_MSGS no App.jsx linhas 39-80).

## Autenticacao (Login + Cadastro)
- **PRONTO** — Login com CPF/CNPJ + senha via API /auth/login
- **PRONTO** — Cadastro com validacao, CPF/CNPJ, via API /auth/registrar
- **PRONTO** — JWT token armazenado em state, enviado como Bearer header
- **INCOMPLETO** — Token nao persiste (refresh da pagina faz logout)

## Perfil
- **PRONTO** — Visualizacao e edicao via API /perfil (GET/PUT)

## Viagens / Passagens (CPF)
- **FALTANDO** — Tela PassagensCPF usa dados hardcoded (const VIAGENS)
- **FALTANDO** — Nao chama API /viagens/ativas nem /tarifas
- API endpoint existe e funciona

## Encomendas (CPF)
- **FALTANDO** — Tela HomeCPF exibe encomendas de const ENCOMENDAS
- **FALTANDO** — Nao chama API /encomendas
- API endpoint existe (busca por nome — fragil)

## Embarcacoes / Barcos (CPF)
- **FALTANDO** — Tela MapaCPF usa const BOATS
- **FALTANDO** — Nao chama API /embarcacoes
- API endpoint existe e funciona

## Amigos + Chat (CPF)
- **FALTANDO** — Tela AmigosCPF usa const FRIENDS e CHAT_MSGS
- **FALTANDO** — Nenhum endpoint de amigos/chat na API
- Tabela amigos_app existe no banco mas sem API

## Fretes (CNPJ)
- **FALTANDO** — Tela HomeCNPJ usa const FRETES
- **FALTANDO** — Nao chama API /fretes
- API endpoint existe (busca por nome — fragil)

## Lojas Parceiras
- **FALTANDO** — Tela LojasParceiras usa const LOJAS
- **FALTANDO** — Nao chama API /lojas
- API endpoints existem (GET /lojas, GET /lojas/minha)

## Pedidos (CNPJ)
- **FALTANDO** — Tela PedidosCNPJ usa const PEDIDOS
- **FALTANDO** — Nao chama API /lojas/pedidos
- API endpoint existe

## Financeiro (CNPJ)
- **FALTANDO** — Tela FinanceiroCNPJ e 100% hardcoded
- **FALTANDO** — Nenhum endpoint financeiro na API

## Assistente IA
- **POS-MVP** — Botao existe mas so mostra sugestoes estaticas, sem backend

## Contagem
| Status | Qtd |
|--------|-----|
| PRONTO | 4 |
| INCOMPLETO | 1 |
| FALTANDO | 13 |
| POS-MVP | 1 |

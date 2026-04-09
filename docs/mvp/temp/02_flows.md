# Dim 2 — Fluxos Criticos

## Onboarding / Primeiro uso
- **PRONTO** — Tela de login clara com opcao "Cadastre-se"
- **PRONTO** — Cadastro com CPF/CNPJ funcional via API
- **INCOMPLETO** — Apos cadastro, redireciona para login (nao faz auto-login)
- **INCOMPLETO** — Nome do usuario no home (HomeCPF) esta hardcoded "Renato Freire" (linha 105), nao usa usuario.nome

## Fluxo principal CPF (ponta a ponta)
1. Login — **PRONTO**
2. Ver proxima viagem — **FALTANDO** (dados mock)
3. Ver encomendas — **FALTANDO** (dados mock)
4. Rastrear embarcacao — **FALTANDO** (dados mock)
5. Ver amigos em viagem — **FALTANDO** (dados mock + sem API)
6. Ver passagens/rotas — **FALTANDO** (dados mock)

## Fluxo principal CNPJ (ponta a ponta)
1. Login — **PRONTO**
2. Ver painel com fretes ativos — **FALTANDO** (dados mock)
3. Ver pedidos da loja — **FALTANDO** (dados mock)
4. Vincular pedido ao frete — **FALTANDO** (botao existe, sem acao)
5. Ver financeiro — **FALTANDO** (dados mock)
6. Gerenciar loja — **FALTANDO** (dados mock)

## Tratamento de erros do usuario
- **PRONTO** — Login: mensagem de erro clara
- **PRONTO** — Cadastro: validacao de campos, senhas, feedback
- **PRONTO** — Perfil: erro de conexao, erro ao salvar

## Fluxo de saida
- **PRONTO** — Botao de logout no header, limpa state
- **FALTANDO** — Token nao persiste, nao tem "lembrar de mim"

## Contagem
| Status | Qtd |
|--------|-----|
| PRONTO | 5 |
| INCOMPLETO | 2 |
| FALTANDO | 9 |
| POS-MVP | 0 |

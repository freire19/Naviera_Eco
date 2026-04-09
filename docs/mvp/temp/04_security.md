# Dim 4 — Seguranca Minima

## Autenticacao
- **PRONTO** — JWT com expiracao (24h), BCrypt para senhas
- **PRONTO** — Spring Security filtra endpoints autenticados
- **INCOMPLETO** — Token armazenado em useState (perde no refresh), nao usa localStorage/httpOnly cookie

## Secrets fora do codigo
- **PRONTO** — JWT_SECRET, DB_PASSWORD via env vars
- **PRONTO** — .env.example sem valores reais

## Inputs validados
- **PRONTO** — Cadastro valida campos obrigatorios e tamanho senha no frontend
- **INCOMPLETO** — API nao valida formato CPF/CNPJ (aceita qualquer string)
- **INCOMPLETO** — API nao valida formato email

## HTTPS
- **FALTANDO** — Sem configuracao TLS/SSL (nem no nginx.conf nem no Spring)
- Aceitavel em dev, bloqueador para producao

## Rate limiting
- **FALTANDO** — Sem rate limiting em nenhum endpoint
- Login pode sofrer brute force

## Dados sensiveis
- **PRONTO** — senha_hash no banco (BCrypt), senha nunca retornada na resposta
- **PRONTO** — JWT nao inclui dados sensiveis (so id, documento, tipo)

## CORS
- **PRONTO** — Configurado com origens especificas (nao e wildcard)
- **INCOMPLETO** — Porta hardcoded no config, facil de esquecer ao mudar

## Contagem
| Status | Qtd |
|--------|-----|
| PRONTO | 6 |
| INCOMPLETO | 3 |
| FALTANDO | 2 |
| POS-MVP | 0 |

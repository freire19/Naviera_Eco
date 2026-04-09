# Dim 5 — Estabilidade

## Error handling nos fluxos core
- **PRONTO** — Login/Cadastro/Perfil: try/catch com mensagem ao usuario
- **PRONTO** — GlobalExceptionHandler captura exceptions genericas
- **INCOMPLETO** — GlobalExceptionHandler engole a exception real (so mostra "Erro interno do servidor")

## Reconexao com banco
- **PRONTO** — HikariCP com pool configurado (max 10, min idle 2, timeout 5s)
- **PRONTO** — connection-timeout e max-lifetime configurados

## Graceful shutdown
- **INCOMPLETO** — Spring Boot faz shutdown padrao mas sem config explicita
- **INCOMPLETO** — App React nao tem cleanup de estado

## Timeouts
- **PRONTO** — HikariCP connection-timeout: 5000ms
- **INCOMPLETO** — Sem timeout nos fetch() do frontend (podem ficar pendurados)

## Recuperacao de erros transientes
- **FALTANDO** — Sem retry em chamadas de API no frontend
- **FALTANDO** — Sem retry em queries de banco na API

## Contagem
| Status | Qtd |
|--------|-----|
| PRONTO | 4 |
| INCOMPLETO | 4 |
| FALTANDO | 2 |
| POS-MVP | 0 |

# Dim 7 — Dependencias

## Dependencias pinadas
- **PRONTO** — package.json com versoes fixas (react 19.0.0, vite 6.0.5)
- **PRONTO** — pom.xml com Spring Boot 3.3.5, versoes gerenciadas
- **INCOMPLETO** — Sem package-lock.json commitado (pode gerar builds diferentes)

## APIs externas
- **PRONTO** — Nenhuma API externa (tudo e interno: API propria + banco proprio)

## Servicos de terceiros
- **PRONTO** — Google Fonts carregada via CDN (Sora, Space Mono)
- **INCOMPLETO** — Se CDN cair, fontes voltam ao fallback do sistema (nao e critico)

## Fallback se externo cair
- **PRONTO** — fontFamily: "'Sora', sans-serif" (tem fallback)
- **PRONTO** — App funciona offline parcial (dados mock sempre aparecem — ironicamente)

## Contagem
| Status | Qtd |
|--------|-----|
| PRONTO | 5 |
| INCOMPLETO | 2 |
| FALTANDO | 0 |
| POS-MVP | 0 |

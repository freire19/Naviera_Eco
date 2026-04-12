# Naviera Web

Console operacional web para gestao de transporte fluvial. Espelho do Desktop (JavaFX) para uso no escritorio com internet.

## Arquitetura

```
Browser (React, porta 5174) --proxy /api--> Express BFF (porta 3002) --SQL--> PostgreSQL
```

## Setup

```bash
# Instalar dependencias
npm install

# Copiar e configurar variaveis de ambiente
cp .env.example .env

# Iniciar frontend (dev)
npm run dev

# Iniciar BFF Express (em outro terminal)
npm run server
```

## Funcionalidades

- 30 paginas (todas funcionais)
- CRUD completo: passagens, encomendas, fretes, viagens, financeiro
- 9 telas de cadastro: usuarios, conferentes, caixas, tarifas, rotas, embarcacoes, clientes, empresa, funcionarios
- Listas, relatorios, balanco, estornos, recibos
- Agenda de viagens
- Auth JWT com multi-tenant (empresa_id)
- Tema light/dark
- Rate limiting (login: 10 req/min, geral: 200 req/min)
- Logging estruturado com rotacao diaria (30 dias)

## Variaveis de ambiente

Ver `.env.example` para todas as variaveis disponiveis.

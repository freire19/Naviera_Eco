# Naviera API — Backend Spring Boot

Backend REST que conecta o app Naviera ao **mesmo banco PostgreSQL** do sistema desktop JavaFX.

## Arquitetura

```
┌────────────────┐     ┌──────────────┐     ┌──────────────────┐
│  JavaFX Desktop│     │  Naviera API │     │  App Mobile/Web  │
│  (operadores)  │────▶│ Spring Boot  │◀────│  (clientes)      │
└───────┬────────┘     └──────┬───────┘     └──────────────────┘
        │                     │
        └─────────┬───────────┘
            ┌─────▼─────┐
            │ PostgreSQL │  ← MESMO banco, sem sincronização
            │ sistema_   │
            │ embarcacao  │
            └────────────┘
```

**Não há duplicação de dados.** O desktop grava encomendas, fretes e viagens. A API lê essas mesmas tabelas. Tabelas novas (`clientes_app`, `lojas_parceiras`, `pedidos_loja`) vivem no mesmo banco sem interferir.

## Pré-requisitos

- Java 17+
- Maven 3.8+
- PostgreSQL rodando com o banco `sistema_embarcacao`
- Migrations 001-008 já executadas (sistema desktop)

## Setup

### 1. Executar migration 009 (lojas parceiras)

```bash
psql -U postgres -d sistema_embarcacao -f database/009_tabelas_lojas_parceiras.sql
```

### 2. Configurar variáveis de ambiente

```bash
export DB_USER=postgres
export DB_PASSWORD=sua_senha
export JWT_SECRET=chave-secreta-forte-aqui
```

### 3. Rodar

```bash
cd naviera-api
mvn spring-boot:run
```

A API estará em `http://localhost:8080/api`

## Endpoints

### Auth (público)
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/auth/login` | Login com CPF/CNPJ + senha |
| POST | `/api/auth/registrar` | Cadastro novo cliente |

**Body login:**
```json
{ "documento": "123.456.789-00", "senha": "minhasenha" }
```

**Resposta:**
```json
{ "token": "eyJ...", "tipo": "CPF", "nome": "Maria Souza", "id": 1 }
```

### Perfil CPF/CNPJ (autenticado)
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/perfil` | Dados do perfil logado |
| PUT | `/api/perfil` | Atualizar nome, email, etc |

### Encomendas — Perfil CPF
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/encomendas` | Encomendas do cliente logado (por nome) |

### Fretes — Perfil CNPJ
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/fretes` | Fretes do remetente logado |

### Viagens e Embarcações (autenticado)
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/viagens/ativas` | Viagens ativas |
| GET | `/api/viagens/embarcacao/{id}` | Viagens de uma embarcação |
| GET | `/api/embarcacoes` | Embarcações com status (NO_PORTO/EM_VIAGEM) |

### Tarifas e Rotas
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/tarifas` | Preços por rota e tipo |
| GET | `/api/rotas` | Todas as rotas |

### Lojas Parceiras — Perfil CNPJ
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/lojas` | Lista lojas ativas |
| GET | `/api/lojas?cidade=Jutaí` | Filtra por cidade destino |
| GET | `/api/lojas/minha` | Minha loja (CNPJ logado) |
| GET | `/api/lojas/pedidos` | Pedidos recebidos |
| GET | `/api/lojas/minhas-compras` | Compras feitas (CPF) |
| PUT | `/api/lojas/pedidos/{id}/vincular-frete` | Vincula pedido a frete |

**Body vincular frete:**
```json
{ "idFrete": 1204, "codigoRastreio": "FRT-1204" }
```

## Fluxo: Loja Parceira → Rastreio

1. **Cliente CPF** compra na vitrine da loja (cria `pedido_loja`)
2. **Comerciante CNPJ** vê o pedido em `/api/lojas/pedidos`
3. Comerciante embarca a mercadoria no sistema desktop (cria frete normal)
4. Comerciante chama `PUT /api/lojas/pedidos/{id}/vincular-frete` com o id_frete
5. **Cliente CPF** vê o rastreio atualizado em `/api/lojas/minhas-compras`
6. Quando o frete muda de status no desktop, o app reflete automaticamente

## Autenticação

Todas as rotas (exceto `/auth/**`) exigem header:
```
Authorization: Bearer <token_jwt>
```

O token contém: `id` (cliente), `tipo` (CPF/CNPJ), `sub` (documento).

## Tabelas novas (migration 009)

- `lojas_parceiras` — vitrine do comerciante CNPJ
- `pedidos_loja` — pedidos de CPF para lojas CNPJ
- `avaliacoes_loja` — avaliações de clientes
- `amigos_app` — rede de amigos entre CPFs

Tabelas existentes lidas (read-only pela API):
`encomendas`, `fretes`, `viagens`, `embarcacoes`, `rotas`, `tarifas`, `passageiros`, `passagens`

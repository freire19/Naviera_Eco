# Arquitetura Multi-Tenant — Naviera Eco

> Documento tecnico detalhado do modelo de operacao offline-first com sincronizacao em nuvem.
> Atualizado em: 2026-04-14

---

## 1. Visao Geral

A Naviera Eco opera no modelo **device-bound tenant** — o mesmo principio usado pelo Windows + Microsoft Account. Cada instalacao Desktop pertence a uma unica empresa e funciona de forma autonoma, sem depender de internet. Quando ha conectividade, os dados sao sincronizados bidirecionalmente com o banco central na nuvem.

```
                           NUVEM (VPS 72.62.166.247)
                    ┌─────────────────────────────────────┐
                    │    Banco Central: naviera_eco        │
                    │    (PostgreSQL Docker, porta 5435)   │
                    │                                      │
                    │  ┌────────┐ ┌────────┐ ┌────────┐   │
                    │  │ Emp 1  │ │ Emp 2  │ │ Emp N  │   │
                    │  │Padrao  │ │Alianca │ │ ...    │   │
                    │  └────────┘ └────────┘ └────────┘   │
                    │                                      │
                    │  API Spring Boot (porta 8081)        │
                    │  Web BFF Express (porta 3002)        │
                    │  App Mobile (Nginx)                  │
                    │  Site (Nginx)                        │
                    └──────┬──────────┬───────────┬───────┘
                           │          │           │
                      sync │     sync │      sync │
                           │          │           │
              ┌────────────┴┐  ┌──────┴─────┐  ┌─┴───────────┐
              │  BARCO 1    │  │  BARCO 2   │  │  BARCO N    │
              │  empresa=1  │  │  empresa=2 │  │  empresa=N  │
              │  DB local   │  │  DB local  │  │  DB local   │
              │  offline ok │  │  offline ok│  │  offline ok │
              └─────────────┘  └────────────┘  └─────────────┘
```

**Principio fundamental:** o Desktop NUNCA depende de internet para funcionar. O operador lanca passagens, fretes e encomendas normalmente mesmo sem conexao. A internet e apenas o canal de sincronizacao — nao e pre-requisito.

---

## 2. Ciclo de Vida de uma Empresa

### 2.1. Cadastro na Plataforma

Uma nova empresa e cadastrada no **banco central** da VPS. Isso pode acontecer via:

- Painel admin Naviera (`admin.naviera.com.br`)
- Diretamente no banco central por um administrador

Ao cadastrar, a empresa recebe:

| Campo | Exemplo | Descricao |
|-------|---------|-----------|
| `id` | 2 | Identificador unico (auto-incremento) |
| `nome` | Deus de Alianca | Nome da empresa |
| `slug` | deusdealianca | Identificador URL-safe para subdominio |
| `ativo` | true | Se a empresa esta ativa na plataforma |

```sql
-- Exemplo: empresa cadastrada no banco central
INSERT INTO empresas (nome, slug, ativo) VALUES ('Deus de Alianca', 'deusdealianca', true);
-- Retorna id = 2
```

### 2.2. Criacao do Operador Admin

Junto com a empresa, e criado pelo menos um usuario operador com permissao `ADMIN`:

```sql
INSERT INTO usuarios (nome, email, senha, funcao, permissao, empresa_id)
VALUES ('Admin Alianca', 'admin@deusdealianca.com', '$2a$10$...hash...', 'Administrador', 'ADMIN', 2);
```

Este operador e o ponto de partida — ele podera criar outros usuarios dentro da mesma empresa.

### 2.3. Instalacao do Desktop

A empresa recebe o instalador do Desktop (`naviera-desktop.deb` para Linux ou `.msi` para Windows). Durante a configuracao inicial:

1. O arquivo `db.properties` e configurado com o `empresa.id` correspondente
2. O banco local PostgreSQL e inicializado com o schema completo
3. O SyncClient faz o primeiro download dos dados da empresa a partir do banco central

```properties
# db.properties — instalacao da Deus de Alianca
db.url=jdbc:postgresql://localhost:5437/naviera_eco
db.usuario=postgres
db.senha=SenhaLocal123
db.pool.tamanho=5

# VINCULO: esta maquina pertence a empresa 2
empresa.id=2

app.versao=1.0.0
```

**Este `empresa.id` e o vinculo permanente entre a maquina e a empresa na nuvem.** Equivale ao "login da conta Microsoft" no Windows — a partir daqui, tudo que acontece nesse PC esta ligado a empresa 2.

### 2.4. Estado Apos Setup

```
BANCO CENTRAL (VPS)                    BANCO LOCAL (Barco)
┌──────────────────────┐               ┌──────────────────────┐
│ empresas:            │               │ empresa_id = 2       │
│   id=1 Padrao        │               │ (fixo no db.properties)
│   id=2 Alianca  ◄────┼── vinculo ──► │                      │
│   id=3 Jose Lemos    │               │ Apenas dados da      │
│                      │               │ empresa 2 existem    │
│ usuarios:            │               │ no banco local       │
│   id=1 admin emp=1   │               │                      │
│   id=2 admin emp=2   │               └──────────────────────┘
└──────────────────────┘
```

---

## 3. Operacao Offline (Dia a Dia)

### 3.1. O que acontece no barco

O operador liga o computador, abre o Desktop, faz login (validado contra o banco LOCAL) e opera normalmente:

```
Operador abre Desktop
       │
       ▼
  Login (banco local)
       │
       ▼
  TenantContext.setEmpresaId(2)  ← empresa_id lido do db.properties
       │
       ▼
  Tela Principal
  ├── Vender Passagem    → INSERT passagens ... empresa_id = 2
  ├── Cadastrar Frete    → INSERT fretes ... empresa_id = 2
  ├── Nova Encomenda     → INSERT encomendas ... empresa_id = 2
  ├── Registrar Despesa  → INSERT despesas ... empresa_id = 2
  └── Fechar Caixa       → UPDATE caixas ... empresa_id = 2
```

**Toda operacao grava no banco local com `empresa_id` da instalacao.**

### 3.2. Isolamento por empresa_id

Mesmo que o banco local so tenha dados de uma empresa, o filtro por `empresa_id` e aplicado em TODAS as queries. Isso garante:

- **Integridade** — se por erro houver dados de outra empresa, eles nao aparecem
- **Consistencia** — o mesmo codigo roda no Desktop (1 empresa) e na API (N empresas) sem alteracao
- **Seguranca** — camada extra de protecao contra vazamento de dados

```java
// TenantContext.java — ThreadLocal com empresa_id + fallback para default
public final class TenantContext {

    private static final ThreadLocal<Integer> currentTenant = new ThreadLocal<>();
    private static volatile int defaultEmpresaId = 1;  // compatibilidade

    // Desktop: chamado uma vez no startup (lido de db.properties)
    public static void setDefaultEmpresaId(int empresaId) { defaultEmpresaId = empresaId; }

    // API REST: chamado a cada request (extraido do JWT)
    public static void setEmpresaId(int empresaId) { currentTenant.set(empresaId); }

    // Retorna empresa_id da thread, ou o default se nao definido
    public static int getEmpresaId() {
        Integer id = currentTenant.get();
        return (id != null) ? id : defaultEmpresaId;
    }

    // Limpar no finally de cada request HTTP (evitar vazamento entre threads)
    public static void clear() { currentTenant.remove(); }
}

// Toda query inclui o filtro:
String sql = "SELECT * FROM passagens WHERE empresa_id = ? AND viagem_id = ?";
stmt.setInt(1, DAOUtils.empresaId());  // chama TenantContext.getEmpresaId()
```

### 3.3. Marcacao para sincronizacao

Toda tabela de negocio possui as colunas de controle:

| Coluna | Tipo | Funcao |
|--------|------|--------|
| `uuid` | UUID | Identificador global unico (nao depende de auto-increment) |
| `ultima_atualizacao` | timestamp | Momento da ultima modificacao |
| `sincronizado` | boolean | `false` = pendente de sync, `true` = ja sincronizado |
| `excluido` | boolean | Soft delete (nunca apaga fisicamente, para sync funcionar) |

Quando o operador cria ou edita um registro:
1. `sincronizado` = `false` automaticamente (trigger no banco)
2. `ultima_atualizacao` = `NOW()` automaticamente (trigger)
3. `uuid` = gerado na criacao (nunca muda)

```sql
-- Funcao de trigger aplicada nas tabelas de negocio
CREATE FUNCTION atualizar_ultima_atualizacao()
RETURNS TRIGGER AS $$
BEGIN
    NEW.ultima_atualizacao = CURRENT_TIMESTAMP;
    NEW.sincronizado = false;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Exemplo: trigger na tabela passagens
CREATE TRIGGER trg_passagens_update
    BEFORE UPDATE ON passagens
    FOR EACH ROW EXECUTE FUNCTION atualizar_ultima_atualizacao();
```

Cada tabela de negocio tem seu trigger (`trg_{tabela}_update`) apontando para esta mesma funcao.

---

## 4. Sincronizacao (Online)

### 4.1. Quando acontece

O SyncClient roda em background no Desktop e detecta automaticamente quando ha internet:

```
Desktop inicia
     │
     ▼
SyncClient.start()
     │
     ├── Verifica conectividade com API
     │      │
     │      ├── SEM internet → aguarda, retry com backoff
     │      │
     │      └── COM internet → inicia ciclo de sync
     │              │
     │              ▼
     │         Autentica via JWT (empresa_id no token)
     │              │
     │              ▼
     │         Upload: local → nuvem (registros nao sincronizados)
     │              │
     │              ▼
     │         Download: nuvem → local (registros mais recentes)
     │              │
     │              ▼
     │         Marca sincronizado = true nos registros enviados
     │              │
     │              ▼
     │         Aguarda intervalo → proximo ciclo
     │
     └── Internet cai no meio? → para, aguarda, retoma quando voltar
```

### 4.2. Endpoint unico de sync

O SyncClient usa um **unico endpoint** para upload e download simultaneamente:

```
POST /api/sync
Authorization: Bearer <JWT com empresa_id=2>
```

A autenticacao e feita via `POST /api/auth/operador/login` (email + senha do operador), que retorna o JWT com `empresa_id` embutido.

### 4.3. Fluxo de Upload (Local → Nuvem)

```sql
-- 1. Desktop busca registros pendentes de cada tabela
SELECT * FROM passagens WHERE sincronizado = false AND empresa_id = 2;

-- 2. Envia para API via POST /api/sync
-- Body inclui tabela + registros nao sincronizados

-- 3. API insere no banco central com ON CONFLICT (uuid)
INSERT INTO passagens (...) VALUES (...)
ON CONFLICT (uuid) DO UPDATE SET ...
WHERE passagens.ultima_atualizacao < EXCLUDED.ultima_atualizacao;
-- last-write-wins: so atualiza se o registro enviado for mais recente

-- 4. Desktop marca como sincronizado
UPDATE passagens SET sincronizado = true WHERE uuid IN (...);
```

### 4.4. Fluxo de Download (Nuvem → Local)

```sql
-- 1. Na mesma chamada POST /api/sync, API retorna registros mais recentes
-- API filtra: empresa_id = 2 AND ultima_atualizacao > ultimo_sync

-- 2. Desktop insere/atualiza localmente com ON CONFLICT (uuid)
INSERT INTO passagens (...) VALUES (...)
ON CONFLICT (uuid) DO UPDATE SET ...
WHERE passagens.ultima_atualizacao < EXCLUDED.ultima_atualizacao;
```

### 4.5. Resolucao de Conflitos: Last-Write-Wins

O sistema usa a estrategia **last-write-wins** (ultima escrita vence):

```
Cenario: operador A edita passagem no barco, operador B edita a mesma no escritorio (web)

Barco (offline):  UPDATE passagem SET poltrona=12  →  ultima_atualizacao = 10:30
Web (online):     UPDATE passagem SET poltrona=15  →  ultima_atualizacao = 10:45

Quando barco sincroniza:
  Upload: passagem com poltrona=12, timestamp=10:30
  Banco central: passagem com poltrona=15, timestamp=10:45
  Resultado: 10:30 < 10:45 → banco central MANTEM poltrona=15

  Download: passagem com poltrona=15, timestamp=10:45
  Banco local: passagem com poltrona=12, timestamp=10:30
  Resultado: 10:30 < 10:45 → banco local ATUALIZA para poltrona=15
```

### 4.6. Tabelas sincronizadas

O SyncClient sincroniza **11 tabelas** de negocio, em ordem de dependencia (tabelas referenciadas primeiro):

| # | Tabela | Direcao | Conteudo |
|---|--------|---------|----------|
| 1 | `embarcacoes` | bidirecional | Cadastro de barcos |
| 2 | `rotas` | bidirecional | Rotas de viagem |
| 3 | `tarifas` | bidirecional | Tabela de precos |
| 4 | `conferentes` | bidirecional | Conferentes de carga |
| 5 | `caixas` | bidirecional | Abertura/fechamento de caixa |
| 6 | `passageiros` | bidirecional | Cadastro de passageiros |
| 7 | `viagens` | bidirecional | Viagens (ida/volta) |
| 8 | `passagens` | bidirecional | Bilhetes vendidos |
| 9 | `encomendas` | bidirecional | Encomendas registradas |
| 10 | `fretes` | bidirecional | Fretes lancados |
| 11 | `financeiro_saidas` | bidirecional | Despesas/saidas operacionais |

**A ordem importa:** embarcacoes e rotas sincronizam antes de viagens (que referencia ambas), passageiros antes de passagens, etc.

**Tabelas com empresa_id mas NAO sincronizadas** (operadas apenas via web/API):
- `funcionarios`, `usuarios`, `cad_clientes_encomenda`
- `categorias_despesa`, `configuracao_empresa`, `itens_frete_padrao`, `itens_encomenda_padrao`
- `tipo_passageiro`, `recibos_avulsos`, `historico_recibo_quitacao_passageiro`
- `agenda_anotacoes`, `auditoria_financeiro`, `ocr_lancamentos`

**Tabelas sem empresa_id** (dados globais/compartilhados):
- `aux_*` (auxiliares: acomodacoes, nacionalidades, formas de pagamento, etc.)
- `empresas` (cadastro de empresas — gerenciado apenas na nuvem)
- `clientes_app` (clientes do app mobile — pertencem a plataforma, nao a empresa)

---

## 5. Acesso Web (Escritorio)

### 5.1. Multi-tenant por subdominio

O escritorio da empresa acessa o sistema web pelo subdominio exclusivo:

```
deusdealianca.naviera.com.br  →  Console web da empresa 2
joselemos.naviera.com.br      →  Console web da empresa 3
padrao.naviera.com.br         →  Console web da empresa 1
```

### 5.2. Fluxo de autenticacao web

```
Operador acessa deusdealianca.naviera.com.br
          │
          ▼
    Nginx recebe request
    Extrai subdominio: "deusdealianca"
    Adiciona header: X-Tenant-Slug: deusdealianca
          │
          ▼
    Express BFF (middleware tenant.js)
    Consulta: SELECT id FROM empresas WHERE slug = 'deusdealianca'
    Resolve: empresa_id = 2
          │
          ▼
    Tela de Login
    Operador digita email + senha
          │
          ▼
    POST /api/auth/login
    Valida: usuario com email X, senha Y, empresa_id = 2
          │
          ▼
    JWT gerado com { usuario_id: 2, empresa_id: 2, funcao: 'Administrador' }
          │
          ▼
    Todas as queries filtram por empresa_id = 2
    O operador so ve dados da propria empresa
```

### 5.3. Diferenca Web vs Desktop

| Aspecto | Desktop | Web |
|---------|---------|-----|
| Banco | Local (maquina) | Central (VPS) |
| Funciona offline | Sim | Nao |
| empresa_id vem de | `db.properties` | JWT (login por subdominio) |
| Sync necessario | Sim | Nao (acessa banco central direto) |
| Operacoes de escrita | Todas | CRUD completo (~50 endpoints) |
| Uso tipico | Barco (em viagem) | Escritorio (administracao) |

---

## 6. App Mobile (Clientes)

### 6.1. Quem usa

O app e para **clientes finais**, nao para operadores:

- **CPF** (pessoa fisica) — passageiros que compram bilhetes
- **CNPJ** (loja parceira) — lojas que despacham encomendas/fretes

### 6.2. Fluxo de acesso

```
Cliente abre app.naviera.com.br
          │
          ▼
    Cadastro/Login por CPF ou CNPJ
          │
          ▼
    API Spring Boot valida credenciais
    JWT gerado (sem empresa_id fixo — cliente pode interagir com varias empresas)
          │
          ▼
    Telas disponiveis:
    ├── CPF: Home, Passagens, Mapa GPS, Amigos, Rastreio Encomenda
    └── CNPJ: Painel, Pedidos, Parceiros, Financeiro, Loja
```

**Diferenca importante:** clientes do app NAO estao vinculados a uma unica empresa. Um passageiro pode comprar bilhetes de qualquer empresa operadora. O vinculo empresa-cliente acontece no momento da transacao (passagem, frete, encomenda).

### 6.3. Tabela clientes_app

```sql
-- Clientes do app vivem APENAS no banco central (VPS)
-- Nao sao sincronizados para o banco local do barco
CREATE TABLE clientes_app (
    id BIGSERIAL PRIMARY KEY,
    documento VARCHAR(20) NOT NULL UNIQUE,  -- CPF (11 digitos) ou CNPJ (14 digitos)
    tipo_documento VARCHAR(4) NOT NULL DEFAULT 'CPF',  -- 'CPF' ou 'CNPJ'
    nome VARCHAR(200) NOT NULL,
    email VARCHAR(200),
    telefone VARCHAR(20),
    cidade VARCHAR(100),
    senha_hash VARCHAR(255) NOT NULL,
    foto_url VARCHAR(500),
    ativo BOOLEAN DEFAULT true,
    data_cadastro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ultimo_acesso TIMESTAMP,
    cnpj_matriz VARCHAR(20),         -- para filiais CNPJ
    responsavel_nome VARCHAR(200),   -- responsavel da loja CNPJ
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    ultima_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sincronizado BOOLEAN DEFAULT false,
    excluido BOOLEAN DEFAULT false
    -- SEM empresa_id — cliente e global (pode interagir com qualquer empresa)
);
```

---

## 7. Diagrama Completo — Quem Acessa O Que

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         BANCO CENTRAL (VPS)                                │
│                    PostgreSQL Docker, porta 5435                            │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                    DADOS POR EMPRESA                                │    │
│  │                                                                     │    │
│  │  empresa_id=1        empresa_id=2          empresa_id=3            │    │
│  │  ┌──────────────┐   ┌──────────────┐      ┌──────────────┐        │    │
│  │  │ Padrao       │   │ Deus Alianca │      │ Jose Lemos   │        │    │
│  │  │ passagens    │   │ passagens    │      │ passagens    │        │    │
│  │  │ fretes       │   │ fretes       │      │ fretes       │        │    │
│  │  │ encomendas   │   │ encomendas   │      │ encomendas   │        │    │
│  │  │ viagens      │   │ viagens      │      │ viagens      │        │    │
│  │  │ ...          │   │ ...          │      │ ...          │        │    │
│  │  └──────────────┘   └──────────────┘      └──────────────┘        │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│  ┌──────────────────────┐  ┌──────────────────────┐                        │
│  │ DADOS GLOBAIS        │  │ CLIENTES APP         │                        │
│  │ aux_* (auxiliares)   │  │ clientes_app (CPF)   │                        │
│  │ empresas (cadastro)  │  │ clientes_app (CNPJ)  │                        │
│  └──────────────────────┘  │ (sem empresa_id)     │                        │
│                             └──────────────────────┘                        │
└──────────────────┬──────────────┬──────────────┬───────────────────────────┘
                   │              │              │
        ┌──────────┘     ┌────────┘       ┌──────┘
        │                │                │
   ┌────┴─────┐    ┌─────┴────┐    ┌──────┴──────┐
   │ Web BFF  │    │ API Rest │    │  SyncClient │
   │ Express  │    │ Spring   │    │  (Desktop)  │
   │ porta    │    │ Boot     │    │             │
   │ 3002     │    │ porta    │    │ JWT auth    │
   │          │    │ 8081     │    │ upload/     │
   │ Acessado │    │          │    │ download    │
   │ por:     │    │ Acessado │    │             │
   │ Operador │    │ por:     │    │ Conecta     │
   │ no       │    │ App      │    │ quando ha   │
   │ escritor.│    │ Mobile   │    │ internet    │
   └──────────┘    └──────────┘    └──────┬──────┘
                                          │
                                   ┌──────┴──────┐
                                   │ BANCO LOCAL │
                                   │ (no barco)  │
                                   │             │
                                   │ empresa_id  │
                                   │ fixo = N    │
                                   │             │
                                   │ Apenas dados│
                                   │ da empresa N│
                                   └─────────────┘
```

---

## 8. Analogia com Microsoft Windows

Para facilitar o entendimento, a comparacao direta:

| Conceito Microsoft | Equivalente Naviera |
|-------------------|---------------------|
| Microsoft Account (email) | Empresa cadastrada no banco central (`empresas.id`) |
| Windows Product Key | `empresa.id` no `db.properties` |
| PC local com Windows | Desktop JavaFX com banco local PostgreSQL |
| Arquivos locais (C:\Users\) | Dados no banco local (passagens, fretes, etc.) |
| OneDrive (sync automatico) | SyncClient (sync bidirecional automatico) |
| OneDrive na nuvem | Banco central na VPS |
| Office 365 Web | naviera-web (`{slug}.naviera.com.br`) |
| App mobile (Outlook, Teams) | naviera-app (`app.naviera.com.br`) |
| Azure AD (multi-tenant) | Tabela `empresas` + `empresa_id` em todas as tabelas |
| Login no Windows | Login no Desktop (valida contra banco local) |
| Login no Office Web | Login no Web (valida contra banco central, filtra por subdominio) |

**A experiencia e identica:**
- Voce liga o PC → ele funciona com ou sem internet (Desktop offline)
- Quando tem internet, seus arquivos sobem pro OneDrive automaticamente (SyncClient)
- Voce pode acessar seus arquivos pelo navegador de qualquer lugar (naviera-web)
- Cada conta Microsoft e isolada — voce nunca ve dados de outro usuario (empresa_id)

---

## 9. Seguranca e Isolamento

### 9.1. Camadas de protecao

```
Camada 1: Rede
├── Subdominio resolve empresa (slug → empresa_id)
├── JWT carrega empresa_id assinado
└── Nginx roteia para o BFF/API correto

Camada 2: Aplicacao
├── TenantContext (ThreadLocal) define empresa_id da thread
├── DAOUtils.empresaId() retorna o tenant atual
├── TODOS os DAOs filtram por empresa_id
└── Controllers com SQL inline filtram por empresa_id

Camada 3: Banco
├── Coluna empresa_id em TODAS as tabelas de negocio (NOT NULL)
├── Foreign key empresas(id) em todas as tabelas
├── Indices compostos (empresa_id, ...) para performance
└── Soft delete (excluido=true) — nunca DELETE fisico
```

### 9.2. O que impede vazamento entre empresas

| Ponto de controle | Mecanismo |
|-------------------|-----------|
| Desktop | `empresa.id` fixo no db.properties + TenantContext |
| API REST | JWT com empresa_id + TenantUtils extrai e filtra |
| Web BFF | Subdominio → slug → empresa_id + JWT |
| Banco | `WHERE empresa_id = ?` em TODA query |
| Sync | JWT autentica + API filtra por empresa_id do token |

### 9.3. Tabelas sem empresa_id (dados compartilhados)

Estas tabelas sao globais e compartilhadas entre todas as empresas:

- `aux_acomodacoes` — tipos de acomodacao (rede, beliche, poltrona)
- `aux_agentes` — agentes de viagem
- `aux_formas_pagamento` — dinheiro, pix, cartao, etc.
- `aux_nacionalidades` — lista de nacionalidades
- `aux_sexo` — masculino, feminino
- `aux_tipos_documento` — RG, CPF, passaporte
- `aux_tipos_passagem` — ida, ida/volta
- `clientes_app` — clientes do app mobile (pertencem a plataforma, nao a empresa)

---

## 10. Subsistemas Complementares

### 10.1. OCR — Lancamento de Fretes por Foto

O naviera-ocr e uma PWA standalone (`ocr.naviera.com.br`) que permite operadores lancarem fretes fotografando notas fiscais, cupons ou cadernos.

```
Operador tira foto (camera/galeria)
          │
          ▼
    Upload para BFF Express
    POST /api/ocr/upload
          │
          ▼
    Google Cloud Vision (DOCUMENT_TEXT_DETECTION)
    Extrai texto bruto da imagem
          │
          ▼
    Parser regex extrai itens (nome, qtd, preco frete)
    Precos vem da tabela itens_frete_padrao da empresa
          │
          ▼
    Operador revisa itens na tela
    (opcional: "Revisar com IA" → Google Gemini reprocessa)
          │
          ▼
    Operador confirma → ocr_lancamentos.status = 'pendente'
          │
          ▼
    Conferente aprova no naviera-web (ReviewOCR)
    → Frete criado automaticamente na tabela fretes
```

**Offline:** Fotos ficam em fila no IndexedDB do navegador e sao enviadas quando a internet volta.

**Tenant-aware:** Cada foto e salva em `uploads/ocr/{empresa_id}/` e o lancamento OCR carrega o `empresa_id` do operador.

### 10.2. WebSocket — Notificacoes em Tempo Real

O sistema usa **STOMP sobre SockJS** para notificacoes push entre servidor e clientes.

```
API Spring Boot (/ws endpoint)
          │
          ▼
    NotificationService (tenant-aware)
    Publica em: /topic/empresa/{empresa_id}/notifications
          │
          ├── naviera-web: recebe via useWebSocket hook
          └── naviera-app: recebe via useWebSocket hook
```

Cada empresa recebe notificacoes apenas no seu topico. Quando o SyncClient sincroniza dados, o SyncService dispara notificacao para o topico da empresa, avisando a web/app que ha dados novos.

### 10.3. Auto-Update do Desktop

O Desktop verifica automaticamente se ha versao mais recente no startup:

```
Desktop inicia
     │
     ▼
VersaoChecker.java
     │
     ├── GET /api/versao/atual (API Spring Boot)
     │
     ├── Compara com app.versao do db.properties
     │
     ├── Se versao nova disponivel:
     │   └── Dialog com changelog + link para download
     │
     └── Se versao atual: continua normalmente
```

A tabela `versao_sistema` no banco central armazena historico de versoes e changelogs.

### 10.4. PWA e Offline nos Apps Web

Tanto o naviera-ocr quanto o naviera-app sao **Progressive Web Apps** instaláveis:

| App | Service Worker | Offline | Cache |
|-----|---------------|---------|-------|
| naviera-ocr | Sim | Fila de fotos no IndexedDB | Cache-first para assets |
| naviera-app | Sim | Telas basicas offline | Cache-first + offline fallback |

Ambos tem `manifest.json` para instalacao como app nativo (Add to Home Screen).

### 10.5. Push Notifications (Firebase FCM)

O naviera-app usa Firebase Cloud Messaging para notificacoes push:

```
Evento no servidor (nova passagem, encomenda, etc.)
          │
          ▼
    API envia push via Firebase FCM
    (tabela dispositivos_push vincula cliente → token FCM)
          │
          ▼
    Navegador/app do cliente recebe notificacao
    (graceful degradation: se FCM falha, usa polling)
```

O hook `useNotifications` gerencia permissoes, tokens e exibicao. O `NotificationBanner` aparece in-app para notificacoes recebidas enquanto o app esta aberto.

---

## 11. Cenarios Reais de Operacao

### 11.1. Dia normal no barco (offline → online)

```
06:00  Operador liga PC no barco, abre Desktop
       TenantContext = empresa 2 (Deus de Alianca)
       Internet: SEM SINAL (rio Amazonas)

06:15  Vende 5 passagens → banco local, sincronizado=false
07:00  Registra 3 encomendas → banco local, sincronizado=false
08:30  Lanca 2 fretes → banco local, sincronizado=false

10:00  Barco se aproxima de cidade → internet volta
       SyncClient detecta conectividade
       Autentica JWT com empresa_id=2
       Upload: 5 passagens + 3 encomendas + 2 fretes → banco central
       Download: tarifas atualizadas pelo escritorio → banco local
       Marca tudo como sincronizado=true

10:05  Internet cai novamente
       Operador continua trabalhando normalmente
       Novos registros marcados sincronizado=false

14:00  Internet volta → novo ciclo de sync automatico
```

### 11.2. Escritorio acompanha em tempo real (web)

```
08:00  Gerente abre deusdealianca.naviera.com.br
       Faz login → JWT com empresa_id=2
       Dashboard mostra dados da ultima sincronizacao

10:05  Sync do barco chega → dados aparecem no dashboard
       Gerente ve as 5 passagens, 3 encomendas, 2 fretes do dia
       Pode gerar relatorios, ver financeiro, conferir balanco

10:30  Gerente atualiza tabela de tarifas via web
       → Gravado no banco central

14:05  Proximo sync do barco → Desktop baixa tarifas atualizadas
```

### 11.3. Passageiro usa o app

```
Cliente abre app.naviera.com.br
Login com CPF 007.608.912-65
       → JWT sem empresa_id fixo (cliente e global)

Consulta viagens disponiveis
       → API retorna viagens de TODAS as empresas ativas
       → Passageiro ve barcos da Alianca E do Jose Lemos

Compra passagem no barco da Deus de Alianca
       → passagem.empresa_id = 2 (vinculada a empresa do barco)

Proximo sync do barco da Alianca
       → Desktop baixa a passagem vendida pelo app
```

---

## 12. Resumo do Estado Atual

### Banco Central (VPS) — 2026-04-14

| Empresas | Operadores | Clientes App |
|----------|-----------|--------------|
| 3 cadastradas | 2 operadores | 1 cliente CPF |
| Padrao (id=1) | Admin Naviera (emp=1) | Jonatas Freire |
| Deus de Alianca (id=2) | Admin Alianca (emp=2) | |
| Jose Lemos (id=3) | — (sem operador) | |

### Banco Local (dev) — 2026-04-14

| Empresa vinculada | Operadores | Clientes App |
|-------------------|-----------|--------------|
| Padrao (id=1) | 1 (Freire) | 6 (testes) |

---

## 13. Proximos Passos

- [ ] Executar migration 013 no banco de producao (VPS)
- [ ] Testar sync completo com empresa_id=2 (Deus de Alianca)
- [ ] Criar operador para Jose Lemos (empresa_id=3)
- [ ] Refinar Setup Wizard no Desktop: fluxo de primeiro boot com configuracao de empresa_id
- [ ] Painel admin: gerar instalador pre-vinculado a empresa

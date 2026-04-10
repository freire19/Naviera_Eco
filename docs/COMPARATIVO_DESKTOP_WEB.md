# Comparativo Desktop vs Web — Analise de Discrepancias
> Data: 2026-04-10
> Fonte: DAOs Java, Express BFF routes, schema PostgreSQL real

---

## DISCREPANCIAS ENCONTRADAS

### CRITICAS (queries quebram)

| # | Tabela | Coluna | Desktop (DAO) | Web (BFF) | Schema Real | Status |
|---|--------|--------|--------------|-----------|-------------|--------|
| 1 | usuarios | PK | `id_usuario` | `id` | `id` | **Desktop usa nome diferente** — DAO faz SELECT id_usuario mas PK real e `id` |
| 2 | usuarios | nome | `nome_completo` | `nome` | `nome` | **Desktop usa nome diferente** — DAO faz `nome_completo` mas coluna real e `nome` |
| 3 | usuarios | login | `login_usuario` | (nao usa) | (nao existe) | **Desktop referencia coluna inexistente** — `login_usuario` nao existe no schema |
| 4 | usuarios | senha | `senha_hash` | `senha` | `senha` | **Desktop usa nome diferente** — DAO faz `senha_hash` mas coluna real e `senha` |
| 5 | usuarios | permissoes | `permissoes` | `permissao` | `permissao` | **Desktop usa nome diferente** — DAO faz `permissoes` (plural) |
| 6 | usuarios | ativo | `ativo` | `excluido` | `excluido` | **Desktop usa coluna inexistente** — `ativo` nao existe, schema tem `excluido` |
| 7 | conferentes | nome | `nome_conferente` | `nome` | `nome_conferente` | **Web usa nome errado** — BFF faz `nome` mas coluna real e `nome_conferente` |
| 8 | caixas | nome | `nome_caixa` | `nome` | `nome_caixa` | **Web usa nome errado** — BFF faz `nome` mas coluna real e `nome_caixa` |
| 9 | cad_clientes | campos | `nome_cliente` | `nome_cliente, telefone, endereco` | `nome_cliente` (so) | **Web insere colunas inexistentes** — `telefone` e `endereco` nao existem na tabela |
| 10 | rotas | PK | `id` | `id_rota` (alias) | `id` | Web faz `SELECT id AS id_rota` — OK como alias, mas PUT usa `id_rota = $3` que **nao existe** |
| 11 | tarifas | FK tipo | `id_tipo_passagem` | POST usa `id_tipo_passageiro` | `id_tipo_passagem` | **Web POST usa nome errado** — insere `id_tipo_passageiro` mas coluna e `id_tipo_passagem` |
| 12 | frete_itens | colunas | `nome_item_ou_id_produto, quantidade, valor_unitario, valor_total, observacao` | mesmos | `nome_item_ou_id_produto, quantidade, preco_unitario, subtotal_item` | **Ambos usam nomes errados** — `valor_unitario`/`valor_total` nao existem, real e `preco_unitario`/`subtotal_item`. E `observacao` nao existe |
| 13 | viagens | ativa vs is_atual | Desktop usa `is_atual` para viagem ativa | Web usa `ativa` | Ambas existem | **Semantica diferente** — Desktop ativa com `is_atual=TRUE`, Web com `ativa=TRUE` |
| 14 | passagens | numero_bilhete | tipo `varchar` | Web faz `MAX(numero_bilhete::bigint)` | `character varying` | **Cast necessario** — OK no Web (ja corrigido), Desktop usa sequence |

### ALTAS (funcionalidade incompleta)

| # | Area | Desktop | Web | Impacto |
|---|------|---------|-----|---------|
| 15 | Viagens DELETE | Deleta cascata: encomenda_itens, passagens, encomendas, fretes, recibos, saidas | Deleta so viagens (sem cascata) | **Web pode falhar** com FK constraint se viagem tem filhos |
| 16 | Passagens INSERT | 29 colunas (completo) | 19 colunas (parcial) | Web nao insere: id_agente, numero_requisicao, valor_alimentacao, valor_transporte, valor_cargas, valor_desconto_tarifa, valor_desconto_geral, troco, id_horario_saida |
| 17 | Passagens UPDATE | 25 colunas atualizaveis | 4 colunas (assento, observacoes, id_acomodacao, id_rota) | Web muito limitado — nao permite editar valores financeiros |
| 18 | Encomendas INSERT | 18 colunas + local_pagamento + id_caixa | 15 colunas (sem local_pagamento na Web) | Web nao insere `local_pagamento` e `id_caixa` |
| 19 | Encomendas itens | Insere `local_armazenamento` | Nao insere `local_armazenamento` | Campo perdido no Web |
| 20 | Fretes INSERT | Desktop gera `id_frete` via MAX+1 | Web usa SERIAL (auto) | Comportamento diferente — Desktop controla ID, Web delega ao banco |
| 21 | Fretes campos extra | Desktop tem `data_saida_viagem, local_transporte, cidade_cobranca, num_notafiscal, valor_notafiscal, peso_notafiscal, troco, status_frete` | Web nao insere nenhum desses | Web frete muito simplificado |
| 22 | Financeiro saidas | Desktop: INSERT com status, forma_pagamento, boletos, parcelas | Web: INSERT simplificado (sem status, parcelas) | Web nao suporta boletos nem parcelas |
| 23 | Financeiro categorias | Desktop busca/cria categorias (`categorias_despesa`) | Web nao gerencia categorias | Sem suporte a categorias no Web |
| 24 | Tarifas JOIN | Desktop: `JOIN aux_tipos_passagem atp ON t.id_tipo_passagem = atp.id_tipo_passagem` | Web: `JOIN tipo_passageiro tp ON t.id_tipo_passagem = tp.id` | **Tabelas diferentes** — Desktop usa `aux_tipos_passagem`, Web usa `tipo_passageiro` |
| 25 | Embarcacoes INSERT | Desktop: ON CONFLICT (empresa_id, nome) DO NOTHING | Web: INSERT simples (pode duplicar) | Web nao previne duplicatas |

### MEDIAS (inconsistencias menores)

| # | Area | Diferenca |
|---|------|-----------|
| 26 | Viagens SELECT | Desktop retorna `id_horario_saida` + `descricao_horario_saida` (JOIN aux_horarios_saida). Web nao faz esse JOIN |
| 27 | Passagens SELECT | Desktop faz JOIN com 7 tabelas (passageiros, viagens, rotas, embarcacoes, aux_horarios_saida, aux_nacionalidades). Web faz JOIN so com passageiros |
| 28 | Encomendas UPDATE | Desktop atualiza 14 campos. Web atualiza 5 campos (remetente, destinatario, observacoes, rota, total_volumes) |
| 29 | Conferentes INSERT | Desktop gera ID via `nextval('seq_conferente')`. Web delega ao SERIAL |
| 30 | Rotas INSERT | Desktop gera ID via `nextval('seq_rota')` e insere com ID. Web delega ao SERIAL |
| 31 | Auth login | Desktop busca por `login_usuario`. Web busca por `nome` ou `email`. Coluna `login_usuario` nao existe |
| 32 | Viagens ativa | Desktop usa campo `is_atual`. Web usa campo `ativa`. Ambos existem no schema mas tem semantica diferente |

---

## RESUMO POR TABELA

| Tabela | Desktop OK? | Web OK? | Discrepancias |
|--------|------------|---------|---------------|
| viagens | SIM (JOIN r.id correto) | SIM (corrigido) | #13 is_atual vs ativa, #15 cascata, #26 horario |
| passagens | SIM | PARCIAL | #14 cast, #16 campos faltando, #17 update limitado |
| passageiros | SIM | SIM | — |
| encomendas | SIM | PARCIAL | #18 campos faltando, #19 itens, #28 update limitado |
| encomenda_itens | SIM | PARCIAL | #19 local_armazenamento |
| fretes | SIM | PARCIAL | #20 ID, #21 campos faltando |
| frete_itens | **ERRADO** | **ERRADO** | #12 nomes de colunas errados em ambos |
| financeiro_saidas | SIM | PARCIAL | #22 boletos/parcelas, #23 categorias |
| rotas | SIM | **ERRADO** | #10 PUT usa id_rota (inexiste) |
| embarcacoes | SIM | PARCIAL | #25 sem ON CONFLICT |
| conferentes | SIM | **ERRADO** | #7 nome vs nome_conferente |
| caixas | SIM | **ERRADO** | #8 nome vs nome_caixa |
| usuarios | **ERRADO** | PARCIAL | #1-6 multiplas colunas com nomes errados |
| tarifas | SIM | PARCIAL | #11 id_tipo_passageiro, #24 tabela diferente |
| cad_clientes_encomenda | SIM | **ERRADO** | #9 colunas inexistentes |
| tipo_passageiro | SIM | SIM | — |
| configuracao_empresa | SIM | SIM | — |

---

## PRIORIDADE DE CORRECAO

### P1 — Quebram agora (erro 500)
- #7 conferentes.nome → nome_conferente
- #8 caixas.nome → nome_caixa
- #9 clientes telefone/endereco nao existem
- #10 rotas PUT id_rota → id
- #11 tarifas id_tipo_passageiro → id_tipo_passagem
- #12 frete_itens colunas erradas

### P2 — Funcionalidade incompleta
- #15 viagens DELETE sem cascata
- #16-#21 campos faltando em passagens/encomendas/fretes
- #22-#23 financeiro sem boletos/categorias
- #13 is_atual vs ativa (semantica)

### P3 — Desktop precisa corrigir
- #1-#6 usuarios DAO com nomes de colunas errados (pode estar funcionando via aliases ou schema antigo)
- #31 login_usuario nao existe

---
*Gerado por Claude Code — analise baseada em schema real do banco*

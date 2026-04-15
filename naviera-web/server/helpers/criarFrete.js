/**
 * Cria um frete com itens no banco de dados.
 * Logica extraida de routes/fretes.js POST para reutilizacao
 * pelo endpoint de aprovacao OCR.
 *
 * @param {PoolClient} client - Conexao do pool (com BEGIN ja chamado)
 * @param {number} empresaId - ID da empresa (tenant)
 * @param {object} payload - Dados do frete
 * @returns {object} Frete criado (row do INSERT RETURNING *)
 */
export async function criarFreteComItens(client, empresaId, payload) {
  const {
    id_viagem, remetente_nome_temp, destinatario_nome_temp, rota_temp, conferente_temp,
    observacoes, valor_total_itens, desconto, valor_pago, tipo_pagamento, nome_caixa, itens,
    data_saida_viagem, local_transporte, cidade_cobranca, num_notafiscal,
    valor_notafiscal, peso_notafiscal, troco, status_frete
  } = payload

  // Gerar proximo ID e numero de frete via sequences (atomico, sem race condition)
  // Usa SAVEPOINT para que falha na sequence nao aborte a transacao inteira
  let nextIdFrete, numFrete
  try {
    await client.query('SAVEPOINT seq_id')
    const idRes = await client.query("SELECT nextval('fretes_id_frete_seq') AS next_id")
    nextIdFrete = idRes.rows[0].next_id
    await client.query('RELEASE SAVEPOINT seq_id')
  } catch (e) {
    await client.query('ROLLBACK TO SAVEPOINT seq_id')
    await client.query('SELECT pg_advisory_xact_lock($1)', [empresaId])
    const idResult = await client.query(
      'SELECT COALESCE(MAX(id_frete), 0) + 1 AS next_id FROM fretes WHERE empresa_id = $1',
      [empresaId]
    )
    nextIdFrete = idResult.rows[0].next_id
  }
  try {
    await client.query('SAVEPOINT seq_num')
    const seqRes = await client.query("SELECT nextval('seq_numero_frete') AS next_num")
    numFrete = seqRes.rows[0].next_num
    await client.query('RELEASE SAVEPOINT seq_num')
  } catch (e) {
    await client.query('ROLLBACK TO SAVEPOINT seq_num')
    await client.query('SELECT pg_advisory_xact_lock($1)', [empresaId])
    const seqResult = await client.query(
      'SELECT COALESCE(MAX(numero_frete), 0) + 1 AS next_num FROM fretes WHERE empresa_id = $1',
      [empresaId]
    )
    numFrete = seqResult.rows[0].next_num
  }

  // Calcular valores
  const vItens = parseFloat(valor_total_itens) || 0
  const vDesconto = parseFloat(desconto) || 0
  const vCalculado = vItens - vDesconto
  const vPago = parseFloat(valor_pago) || 0
  const vDevedor = Math.max(0, vCalculado - vPago)

  // Inserir frete
  const result = await client.query(`
    INSERT INTO fretes (id_frete, numero_frete, data_emissao, id_viagem, remetente_nome_temp, destinatario_nome_temp,
      rota_temp, conferente_temp, observacoes, valor_total_itens, desconto,
      valor_frete_calculado, valor_pago, valor_devedor, tipo_pagamento, nome_caixa,
      data_saida_viagem, local_transporte, cidade_cobranca, num_notafiscal,
      valor_notafiscal, peso_notafiscal, troco, status_frete, empresa_id)
    VALUES ($1,$2,CURRENT_DATE,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17,$18,$19,$20,$21,$22,$23,$24)
    RETURNING *
  `, [
    nextIdFrete, numFrete, id_viagem, remetente_nome_temp || null, destinatario_nome_temp || null,
    rota_temp || null, conferente_temp || null, observacoes || null,
    vItens, vDesconto, vCalculado, vPago, vDevedor,
    tipo_pagamento || null, nome_caixa || null,
    data_saida_viagem || null, local_transporte || null, cidade_cobranca || null, num_notafiscal || null,
    parseFloat(valor_notafiscal) || 0, parseFloat(peso_notafiscal) || 0, parseFloat(troco) || 0,
    status_frete || null, empresaId
  ])

  const freteId = result.rows[0].id_frete

  // DP054: batch insert em vez de loop individual
  if (itens && Array.isArray(itens) && itens.length > 0) {
    const values = []
    const params = []
    itens.forEach((item, i) => {
      const off = i * 5
      values.push(`($${off+1}, $${off+2}, $${off+3}, $${off+4}, $${off+5})`)
      params.push(
        freteId,
        item.nome_item || item.nome_item_ou_id_produto || null,
        item.quantidade || 1,
        item.preco_unitario || item.valor_unitario || 0,
        item.subtotal_item || item.subtotal || item.valor_total || 0
      )
    })
    await client.query(
      `INSERT INTO frete_itens (id_frete, nome_item_ou_id_produto, quantidade, preco_unitario, subtotal_item) VALUES ${values.join(', ')}`,
      params
    )
  }

  return result.rows[0]
}

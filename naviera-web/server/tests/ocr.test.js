import { describe, it } from 'node:test'
import assert from 'node:assert/strict'
import { parseOcrText } from '../helpers/parseOcrText.js'
import { repairTruncatedJSON } from '../helpers/geminiParser.js'
import { fetchWithRetry } from '../helpers/fetchWithRetry.js'
import { OCR_STATUS, EDITAVEIS } from '../helpers/ocrStatus.js'

// ============================================================================
// ocrStatus — constantes
// ============================================================================
describe('ocrStatus', () => {
  it('deve ter 4 status definidos', () => {
    assert.equal(OCR_STATUS.PENDENTE, 'pendente')
    assert.equal(OCR_STATUS.REVISADO, 'revisado_operador')
    assert.equal(OCR_STATUS.APROVADO, 'aprovado')
    assert.equal(OCR_STATUS.REJEITADO, 'rejeitado')
  })

  it('EDITAVEIS deve conter apenas pendente e revisado', () => {
    assert.deepEqual(EDITAVEIS, ['pendente', 'revisado_operador'])
  })
})

// ============================================================================
// parseOcrText — parser regex
// ============================================================================
describe('parseOcrText', () => {
  it('deve retornar estrutura vazia para texto vazio', () => {
    const result = parseOcrText('', [])
    assert.equal(result.remetente, '')
    assert.equal(result.destinatario, '')
    assert.deepEqual(result.itens, [])
    assert.equal(result.valor_total, 0)
  })

  it('deve detectar NFC-e e extrair itens', () => {
    const nfce = `MERCADO BOM PRECO
CNPJ: 12.345.678/0001-90
NOTA FISCAL
001 0075 PAO FRANCES KG
0,440 KG X 11.99
5,28
002 0088 ARROZ TIPO 1
1 UN X 22.90
22,90`
    const result = parseOcrText(nfce, [])
    assert.equal(result.remetente, 'MERCADO BOM PRECO')
    assert.ok(result.itens.length >= 2, `Esperado >= 2 itens, recebeu ${result.itens.length}`)
    assert.ok(result.itens.some(i => /pao/i.test(i.nome_item)))
    assert.ok(result.itens.some(i => /arroz/i.test(i.nome_item)))
  })

  it('deve usar preco da tabela de frete quando houver match', () => {
    const nfce = `LOJA XYZ
CNPJ: 00.000.000/0001-00
CUPOM FISCAL
001 0001 MARGARINA
1 UN X 5.00
5,00`
    const tabelaFrete = [
      { nome_item: 'Margarina', preco_unitario_padrao: 3.50, preco_unitario_desconto: 3.00 }
    ]
    const result = parseOcrText(nfce, tabelaFrete)
    const margarina = result.itens.find(i => /margarina/i.test(i.nome_item))
    assert.ok(margarina, 'Margarina nao encontrada nos itens')
    assert.equal(margarina.preco_unitario, 3.50)
  })

  it('deve extrair remetente e destinatario de nota generica', () => {
    const nota = `Remetente: Joao Silva
Destinatario: Maria Costa
Rota: Manaus - Parintins
2 cx Margarina
3 saco Farinha`
    const result = parseOcrText(nota, [])
    assert.equal(result.remetente, 'Joao Silva')
    assert.equal(result.destinatario, 'Maria Costa')
    assert.equal(result.rota, 'Manaus - Parintins')
  })

  it('deve ignorar linhas de lixo (totais, forma de pagamento)', () => {
    const nfce = `MERCADO
CNPJ: 11.111.111/0001-11
NOTA FISCAL
001 0001 REFRIGERANTE 2L
1 UN X 8.00
8,00
QTD TOTAL DE ITENS: 1
VALOR TOTAL: 8,00
FORMA DE PAGAMENTO
DINHEIRO`
    const result = parseOcrText(nfce, [])
    const nomes = result.itens.map(i => i.nome_item.toLowerCase())
    assert.ok(!nomes.some(n => /total|forma|dinheiro|qtd/i.test(n)), 'Lixo nao deveria estar nos itens')
  })
})

// ============================================================================
// repairTruncatedJSON — reparar JSON cortado pelo Gemini
// ============================================================================
describe('repairTruncatedJSON', () => {
  it('deve reparar JSON com array de itens cortado no meio', () => {
    const truncated = '{"remetente":"Joao","itens":[{"nome_item":"Arroz","quantidade":1},{"nome_item":"Fe'
    const result = repairTruncatedJSON(truncated)
    assert.ok(result, 'Deveria reparar o JSON')
    assert.equal(result.remetente, 'Joao')
    assert.equal(result.itens.length, 1)
    assert.equal(result.itens[0].nome_item, 'Arroz')
  })

  it('deve reparar JSON com brackets faltando no final', () => {
    const truncated = '{"itens":[{"nome_item":"X","quantidade":2}],"valor_total":10'
    const result = repairTruncatedJSON(truncated)
    assert.ok(result)
    assert.equal(result.valor_total, 10)
  })

  it('deve retornar null para JSON irrecuperavel', () => {
    const result = repairTruncatedJSON('isto nao e json')
    assert.equal(result, null)
  })

  it('deve funcionar com JSON completo e valido', () => {
    const valid = '{"remetente":"A","itens":[]}'
    const result = repairTruncatedJSON(valid)
    assert.ok(result)
    assert.equal(result.remetente, 'A')
  })
})

// ============================================================================
// fetchWithRetry — retry com backoff
// ============================================================================
describe('fetchWithRetry', () => {
  it('deve retornar response em sucesso na primeira tentativa', async () => {
    const mockUrl = 'https://httpbin.org/status/200'
    // Usar uma URL real simples ou mock
    // Testar a logica sem rede: simular com AbortSignal
    const res = await fetchWithRetry('data:text/plain,ok', {}, { retries: 0 })
    assert.equal(res.ok, true)
  })

  it('deve retentar em erro 500 e esgotar retries', async () => {
    let attempts = 0
    const originalFetch = globalThis.fetch
    globalThis.fetch = async () => {
      attempts++
      return { ok: false, status: 500 }
    }
    try {
      await fetchWithRetry('http://fake', {}, { retries: 1, baseDelay: 10 })
      assert.fail('Deveria ter lancado erro')
    } catch (err) {
      assert.equal(err.status, 500)
      assert.equal(attempts, 2) // 1 original + 1 retry
    } finally {
      globalThis.fetch = originalFetch
    }
  })

  it('NAO deve retentar em erro 4xx', async () => {
    let attempts = 0
    const originalFetch = globalThis.fetch
    globalThis.fetch = async () => {
      attempts++
      return { ok: false, status: 400 }
    }
    try {
      const res = await fetchWithRetry('http://fake', {}, { retries: 2, baseDelay: 10 })
      assert.equal(res.status, 400)
      assert.equal(attempts, 1) // sem retry
    } finally {
      globalThis.fetch = originalFetch
    }
  })

  it('deve retentar em erro de rede (fetch throws)', async () => {
    let attempts = 0
    const originalFetch = globalThis.fetch
    globalThis.fetch = async () => {
      attempts++
      throw new Error('network error')
    }
    try {
      await fetchWithRetry('http://fake', {}, { retries: 1, baseDelay: 10 })
      assert.fail('Deveria ter lancado erro')
    } catch (err) {
      assert.equal(err.message, 'network error')
      assert.equal(attempts, 2)
    } finally {
      globalThis.fetch = originalFetch
    }
  })
})

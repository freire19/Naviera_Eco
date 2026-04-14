/**
 * Parseia texto bruto do OCR e extrai itens para lancamento de frete.
 *
 * IMPORTANTE: O preco do frete NAO e o preco do produto na nota.
 * O preco vem da tabela itens_frete_padrao (preco de transporte por volume).
 * Se o item nao esta na tabela, o preco fica 0 e o operador preenche.
 *
 * Formatos suportados:
 * - NFC-e brasileira (cupom fiscal eletronico)
 * - Nota fiscal manual (caderno escrito a mao)
 * - Lista simples (item quantidade preco)
 *
 * @param {string} rawText - Texto bruto do Google Cloud Vision
 * @param {Array} itensPadrao - Itens de itens_frete_padrao da empresa
 * @returns {{ remetente, destinatario, rota, itens[], valor_total, observacoes }}
 */
export function parseOcrText(rawText, itensPadrao = []) {
  const lines = rawText.split('\n').map(l => l.trim()).filter(Boolean)

  // Detectar formato
  const isNFCe = lines.some(l =>
    /nota fiscal/i.test(l) || /cnpj/i.test(l) || /cupom fiscal/i.test(l) ||
    /documento auxiliar/i.test(l) || /qtd\.?\s*total/i.test(l)
  )

  let remetente = ''
  let destinatario = ''
  let rota = ''
  let itens = []

  if (isNFCe) {
    // Extrair nome do estabelecimento como remetente
    remetente = extractEstabelecimento(lines)
    itens = parseNFCe(lines, itensPadrao)
  } else {
    // Formato manual/generico
    const result = parseGenerico(lines, itensPadrao)
    remetente = result.remetente
    destinatario = result.destinatario
    rota = result.rota
    itens = result.itens
  }

  const valor_total = itens.reduce((sum, i) => sum + (i.subtotal || 0), 0)

  return {
    remetente,
    destinatario,
    rota,
    itens,
    valor_total: Math.round(valor_total * 100) / 100,
    observacoes: isNFCe ? 'Extraido de NFC-e' : ''
  }
}

/**
 * Parser para NFC-e (Nota Fiscal de Consumidor Eletronica).
 * Formato tipico:
 *   001 0075 PAO FRANCES KG
 *   0,440 KG X 11.99
 *   5,28
 *
 * Extrai: nome do produto e quantidade.
 * Preco vem da tabela de frete, NAO da nota.
 */
function parseNFCe(lines, itensPadrao) {
  const itens = []
  let i = 0

  while (i < lines.length) {
    const line = lines[i]

    // Detectar linha de item: comeca com codigo (001, 002, etc.) ou codigo de barras
    const itemMatch = line.match(/^(\d{3,})\s+(\d{4,})?\s*(.+)/)
    if (itemMatch) {
      const descricao = cleanDescricao(itemMatch[3] || '')

      // Ignorar se descricao e muito curta ou e lixo
      if (descricao.length < 3) { i++; continue }

      // Buscar quantidade nas proximas linhas
      let quantidade = 1
      let j = i + 1
      while (j < lines.length && j <= i + 3) {
        const qtyMatch = lines[j].match(/^(\d+(?:[,.]\d+)?)\s*(UN|KG|LT|ML|G|PCT|CX|FD|SC|DZ)\s*X/i)
        if (qtyMatch) {
          const qtyRaw = qtyMatch[1].replace(',', '.')
          const qty = parseFloat(qtyRaw)
          // Para KG com decimais (0,440 KG), arredondar para 1 unidade de frete
          if (qtyMatch[2].toUpperCase() === 'KG' && qty < 1) {
            quantidade = 1
          } else {
            quantidade = Math.ceil(qty) // Arredondar para cima (frete por unidade inteira)
          }
          break
        }
        j++
      }

      // Buscar na tabela de precos de frete
      const match = findBestMatch(descricao, itensPadrao)
      const precoFrete = match ? match.preco_unitario_padrao : 0

      itens.push({
        nome_item: capitalizeWords(descricao),
        quantidade,
        preco_unitario: precoFrete,
        subtotal: Math.round(quantidade * precoFrete * 100) / 100,
        confianca: match ? 85 : 60,
        item_padrao_nome: match?.nome_item || null,
        preco_padrao: match?.preco_unitario_padrao || null,
        preco_desconto: match?.preco_unitario_desconto || null,
        item_novo: !match,
        preco_diferente: false
      })
    }

    i++
  }

  // Filtrar itens duplicados (mesmo nome) e linhas que nao sao produtos
  return deduplicateItens(itens)
}

/**
 * Parser generico para notas manuscritas ou listas simples.
 * Formato: "2 cx margarina R$5,00" ou "Saco cimento 10 x 15,00"
 */
function parseGenerico(lines, itensPadrao) {
  let remetente = ''
  let destinatario = ''
  let rota = ''
  const itens = []

  for (const line of lines) {
    const lower = line.toLowerCase()

    if (!remetente && matchLabel(lower, ['remetente', 'rem:', 'de:', 'origem'])) {
      remetente = extractAfterLabel(line); continue
    }
    if (!destinatario && matchLabel(lower, ['destinatario', 'dest:', 'para:', 'destino'])) {
      destinatario = extractAfterLabel(line); continue
    }
    if (!rota && matchLabel(lower, ['rota', 'trajeto', 'trecho'])) {
      rota = extractAfterLabel(line); continue
    }

    const item = parseItemLineGenerico(line)
    if (item) {
      const match = findBestMatch(item.nome_item, itensPadrao)
      if (match) {
        // Usar preco da tabela de frete
        item.preco_unitario = match.preco_unitario_padrao
        item.subtotal = item.quantidade * match.preco_unitario_padrao
        item.item_padrao_nome = match.nome_item
        item.preco_padrao = match.preco_unitario_padrao
        item.preco_desconto = match.preco_unitario_desconto
        item.preco_diferente = false
      } else {
        item.item_novo = true
      }
      itens.push(item)
    }
  }

  return { remetente, destinatario, rota, itens }
}

/**
 * Extrai item de linha generica (caderno manuscrito).
 */
function parseItemLineGenerico(line) {
  const normalized = line.replace(/r\$/gi, '').replace(/(\d),(\d{2})(?!\d)/g, '$1.$2').trim()

  // Pattern: QTD [x] NOME [PRECO]
  const p1 = normalized.match(/^(\d+)\s*[xX]?\s+(.+?)(?:\s+([\d.]+))?\s*$/)
  if (p1 && p1[2].length > 2) {
    return buildItem(p1[2].trim(), parseInt(p1[1]), parseFloat(p1[3]) || 0)
  }

  // Pattern: NOME QTD [x] PRECO
  const p2 = normalized.match(/^(.+?)\s+(\d+)\s*[xX]\s*([\d.]+)\s*$/)
  if (p2 && p2[1].length > 2) {
    return buildItem(p2[1].trim(), parseInt(p2[2]), parseFloat(p2[3]) || 0)
  }

  // Pattern: NOME - QTD
  const p3 = normalized.match(/^(.+?)\s*[-–—]\s*(\d+)\s*$/)
  if (p3 && p3[1].length > 2) {
    return buildItem(p3[1].trim(), parseInt(p3[2]), 0)
  }

  return null
}

function buildItem(nome_item, quantidade, preco_unitario) {
  if (!nome_item || isNaN(quantidade)) return null
  if (quantidade <= 0) return null
  return {
    nome_item: capitalizeWords(nome_item),
    quantidade,
    preco_unitario: Math.round((preco_unitario || 0) * 100) / 100,
    subtotal: Math.round(quantidade * (preco_unitario || 0) * 100) / 100,
    confianca: 75
  }
}

/**
 * Limpa a descricao extraida de uma NFC-e.
 * Remove codigos de barras, unidades soltas, e lixo.
 */
function cleanDescricao(desc) {
  return desc
    .replace(/\d{8,}/g, '')              // remove codigos de barras longos
    .replace(/\b(KG|UN|LT|ML|G|PCT)\b\s*$/i, '') // remove unidade no final
    .replace(/\b\d+[gG]\b\s*$/, '')      // remove peso no final (200g, 500G)
    .replace(/\b\d+[mM][lL]\b\s*$/, '')  // remove ml no final
    .replace(/\s+/g, ' ')
    .trim()
}

/**
 * Extrai nome do estabelecimento da NFC-e (primeira linha nao-vazia
 * antes do CNPJ).
 */
function extractEstabelecimento(lines) {
  for (let i = 0; i < Math.min(5, lines.length); i++) {
    const l = lines[i]
    if (/cnpj/i.test(l)) break
    if (l.length > 3 && !/^\d+$/.test(l) && !/fone/i.test(l)) {
      return l
    }
  }
  return ''
}

/**
 * Remove itens duplicados (mesmo nome normalizado).
 * Soma quantidades de duplicatas.
 */
function deduplicateItens(itens) {
  const map = new Map()
  for (const item of itens) {
    const key = normalize(item.nome_item)
    if (key.length < 3) continue

    // Ignorar linhas que parecem lixo (numeros puros, codigos)
    if (/^\d[\d\s]*$/.test(item.nome_item)) continue
    if (/^qtd|^valor|^total|^forma|^troco|^dinheiro|^consulte/i.test(item.nome_item)) continue

    if (map.has(key)) {
      const existing = map.get(key)
      existing.quantidade += item.quantidade
      existing.subtotal = Math.round(existing.quantidade * existing.preco_unitario * 100) / 100
    } else {
      map.set(key, { ...item })
    }
  }
  return Array.from(map.values())
}

/**
 * Fuzzy match contra tabela de precos de frete.
 */
function findBestMatch(nome, itensPadrao) {
  if (!itensPadrao.length || !nome) return null

  const nomeNorm = normalize(nome)
  let bestMatch = null
  let bestScore = Infinity

  for (const item of itensPadrao) {
    const padNorm = normalize(item.nome_item)
    if (nomeNorm === padNorm) return item
    if (nomeNorm.includes(padNorm) || padNorm.includes(nomeNorm)) return item

    const dist = levenshtein(nomeNorm, padNorm)
    const threshold = Math.max(3, Math.floor(padNorm.length * 0.35))
    if (dist < bestScore && dist <= threshold) {
      bestScore = dist
      bestMatch = item
    }
  }

  return bestMatch
}

function normalize(str) {
  return str.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9\s]/g, '').replace(/\s+/g, ' ').trim()
}

function levenshtein(a, b) {
  const m = a.length, n = b.length
  const dp = Array.from({ length: m + 1 }, () => Array(n + 1).fill(0))
  for (let i = 0; i <= m; i++) dp[i][0] = i
  for (let j = 0; j <= n; j++) dp[0][j] = j
  for (let i = 1; i <= m; i++) {
    for (let j = 1; j <= n; j++) {
      dp[i][j] = a[i - 1] === b[j - 1]
        ? dp[i - 1][j - 1]
        : 1 + Math.min(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
    }
  }
  return dp[m][n]
}

function matchLabel(lower, labels) {
  return labels.some(l => lower.startsWith(l))
}

function extractAfterLabel(line) {
  return line.replace(/^[^:]+[:\-–—]\s*/, '').trim() || line.replace(/^\S+\s+/, '').trim()
}

function capitalizeWords(str) {
  return str.toLowerCase().replace(/(?:^|\s)\S/g, c => c.toUpperCase())
}

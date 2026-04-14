/**
 * Parseia texto bruto do OCR e extrai itens de frete estruturados.
 * Faz fuzzy match contra a tabela de precos padrao da empresa.
 *
 * @param {string} rawText - Texto bruto do Google Cloud Vision
 * @param {Array} itensPadrao - Itens de itens_frete_padrao da empresa
 *   Cada item: { nome_item, preco_unitario_padrao, preco_unitario_desconto }
 * @returns {{ remetente, destinatario, rota, itens[], valor_total, observacoes }}
 */
export function parseOcrText(rawText, itensPadrao = []) {
  const lines = rawText.split('\n').map(l => l.trim()).filter(Boolean)

  let remetente = ''
  let destinatario = ''
  let rota = ''
  const itens = []
  const unmatchedLines = []

  for (const line of lines) {
    const lower = line.toLowerCase()

    // Tentar extrair remetente
    if (!remetente && matchLabel(lower, ['remetente', 'rem', 'de:', 'origem'])) {
      remetente = extractAfterLabel(line)
      continue
    }

    // Tentar extrair destinatario
    if (!destinatario && matchLabel(lower, ['destinatario', 'dest', 'para:', 'destino'])) {
      destinatario = extractAfterLabel(line)
      continue
    }

    // Tentar extrair rota
    if (!rota && matchLabel(lower, ['rota', 'trajeto', 'trecho'])) {
      rota = extractAfterLabel(line)
      continue
    }

    // Tentar extrair item de frete
    const item = parseItemLine(line)
    if (item) {
      // Fuzzy match contra itens padrao
      const match = findBestMatch(item.nome_item, itensPadrao)
      if (match) {
        item.item_padrao_nome = match.nome_item
        item.preco_padrao = match.preco_unitario_padrao
        item.preco_desconto = match.preco_unitario_desconto
        item.preco_diferente = Math.abs(item.preco_unitario - match.preco_unitario_padrao) > match.preco_unitario_padrao * 0.10
      } else {
        item.item_novo = true
      }
      itens.push(item)
    } else {
      unmatchedLines.push(line)
    }
  }

  const valor_total = itens.reduce((sum, i) => sum + (i.subtotal || 0), 0)

  return {
    remetente,
    destinatario,
    rota,
    itens,
    valor_total: Math.round(valor_total * 100) / 100,
    observacoes: unmatchedLines.length > 0 ? unmatchedLines.join(' | ') : ''
  }
}

/**
 * Tenta extrair um item de frete de uma linha de texto.
 * Padroes reconhecidos:
 *   "2 cx margarina 5,00"
 *   "2x cx margarina R$ 5,00"
 *   "cx margarina 2 x 5,00"
 *   "cx margarina - 2 - R$5,00"
 *   "1 Saco de cimento R$15,00"
 */
function parseItemLine(line) {
  // Normalizar: remover R$, trocar virgula decimal por ponto
  const normalized = line
    .replace(/r\$/gi, '')
    .replace(/(\d),(\d{2})(?!\d)/g, '$1.$2')  // 5,00 → 5.00
    .trim()

  // Pattern 1: QTD [x] NOME PRECO
  // Ex: "2 cx margarina 5.00" ou "2x cx margarina 5.00"
  const p1 = normalized.match(/^(\d+)\s*[xX]?\s+(.+?)\s+([\d.]+)\s*$/)
  if (p1) {
    return buildItem(p1[2].trim(), parseInt(p1[1]), parseFloat(p1[3]))
  }

  // Pattern 2: NOME QTD [x] PRECO
  // Ex: "cx margarina 2 x 5.00"
  const p2 = normalized.match(/^(.+?)\s+(\d+)\s*[xX]?\s*([\d.]+)\s*$/)
  if (p2 && p2[1].length > 1) {
    return buildItem(p2[1].trim(), parseInt(p2[2]), parseFloat(p2[3]))
  }

  // Pattern 3: QTD NOME - PRECO (com separadores)
  // Ex: "2 cx margarina - 5.00"
  const p3 = normalized.match(/^(\d+)\s+(.+?)\s*[-–—]\s*([\d.]+)\s*$/)
  if (p3) {
    return buildItem(p3[2].trim(), parseInt(p3[1]), parseFloat(p3[3]))
  }

  // Pattern 4: NOME PRECO (sem quantidade, assume 1)
  // Ex: "cx margarina 5.00"
  const p4 = normalized.match(/^(.+?)\s+([\d.]+)\s*$/)
  if (p4 && p4[1].length > 2 && !p4[1].match(/^\d/)) {
    return buildItem(p4[1].trim(), 1, parseFloat(p4[2]))
  }

  return null
}

function buildItem(nome_item, quantidade, preco_unitario) {
  if (!nome_item || isNaN(quantidade) || isNaN(preco_unitario)) return null
  if (quantidade <= 0 || preco_unitario <= 0) return null
  return {
    nome_item: capitalizeFirst(nome_item),
    quantidade,
    preco_unitario: Math.round(preco_unitario * 100) / 100,
    subtotal: Math.round(quantidade * preco_unitario * 100) / 100,
    confianca: 75  // default, ajustado pelo caller se tiver dados do Vision
  }
}

/**
 * Fuzzy match: encontra o item padrao mais proximo pelo nome.
 * Usa distancia de Levenshtein com threshold proporcional ao tamanho.
 */
function findBestMatch(nome, itensPadrao) {
  if (!itensPadrao.length || !nome) return null

  const nomeNorm = normalize(nome)
  let bestMatch = null
  let bestScore = Infinity

  for (const item of itensPadrao) {
    const padNorm = normalize(item.nome_item)

    // Match exato (normalizado)
    if (nomeNorm === padNorm) return item

    // Substring match
    if (nomeNorm.includes(padNorm) || padNorm.includes(nomeNorm)) return item

    // Levenshtein
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
  return str
    .toLowerCase()
    .normalize('NFD').replace(/[\u0300-\u036f]/g, '')  // remove acentos
    .replace(/[^a-z0-9\s]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
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
  // Remove label e separadores
  return line.replace(/^[^:]+[:\-–—]\s*/, '').trim() || line.replace(/^\S+\s+/, '').trim()
}

function capitalizeFirst(str) {
  return str.charAt(0).toUpperCase() + str.slice(1)
}

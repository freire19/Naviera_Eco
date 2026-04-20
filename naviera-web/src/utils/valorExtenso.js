// Conversao de valor numerico em texto por extenso (pt-BR).
// Replica util.ValorExtensoUtil do desktop.

const UNIDADES = ['', 'um', 'dois', 'tres', 'quatro', 'cinco', 'seis', 'sete', 'oito', 'nove',
  'dez', 'onze', 'doze', 'treze', 'quatorze', 'quinze', 'dezesseis', 'dezessete', 'dezoito', 'dezenove']
const DEZENAS = ['', '', 'vinte', 'trinta', 'quarenta', 'cinquenta', 'sessenta', 'setenta', 'oitenta', 'noventa']
const CENTENAS = ['', 'cento', 'duzentos', 'trezentos', 'quatrocentos', 'quinhentos', 'seiscentos', 'setecentos', 'oitocentos', 'novecentos']

function ateMil(n) {
  if (n === 0) return ''
  if (n === 100) return 'cem'
  const c = Math.floor(n / 100)
  const resto = n % 100
  const partes = []
  if (c > 0) partes.push(CENTENAS[c])
  if (resto < 20) {
    if (resto > 0) partes.push(UNIDADES[resto])
  } else {
    const d = Math.floor(resto / 10)
    const u = resto % 10
    partes.push(DEZENAS[d] + (u > 0 ? ' e ' + UNIDADES[u] : ''))
  }
  return partes.join(' e ')
}

export function valorPorExtenso(valor) {
  if (valor === null || valor === undefined || isNaN(valor)) return ''
  const v = Math.abs(Number(valor))
  const inteiros = Math.floor(v)
  const centavos = Math.round((v - inteiros) * 100)

  const partes = []
  if (inteiros === 0) {
    partes.push('zero reais')
  } else {
    const milhoes = Math.floor(inteiros / 1_000_000)
    const milhares = Math.floor((inteiros % 1_000_000) / 1000)
    const resto = inteiros % 1000

    const texto = []
    if (milhoes > 0) {
      texto.push(milhoes === 1 ? 'um milhao' : `${ateMil(milhoes)} milhoes`)
    }
    if (milhares > 0) {
      if (milhares === 1) texto.push('mil')
      else texto.push(`${ateMil(milhares)} mil`)
    }
    if (resto > 0) {
      if (texto.length > 0 && (resto < 100 || resto % 100 === 0)) texto.push('e')
      else if (texto.length > 0) texto.push('')
      texto.push(ateMil(resto))
    }
    const unidade = inteiros === 1 ? 'real' : 'reais'
    partes.push(`${texto.join(' ').replace(/\s+/g, ' ').trim()} ${unidade}`)
  }

  if (centavos > 0) {
    const textoCent = ateMil(centavos)
    const palavra = centavos === 1 ? 'centavo' : 'centavos'
    partes.push(`e ${textoCent} ${palavra}`)
  }

  return partes.join(' ').replace(/\s+/g, ' ').trim()
}

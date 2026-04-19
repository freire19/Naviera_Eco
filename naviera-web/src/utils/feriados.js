// Feriados nacionais brasileiros (fixos + moveis calculados)
// Formato de retorno: [{ data: 'YYYY-MM-DD', nome: 'Tiradentes' }, ...]

function calcularPascoa(ano) {
  // Algoritmo de Gauss/Meeus
  const a = ano % 19
  const b = Math.floor(ano / 100)
  const c = ano % 100
  const d = Math.floor(b / 4)
  const e = b % 4
  const f = Math.floor((b + 8) / 25)
  const g = Math.floor((b - f + 1) / 3)
  const h = (19 * a + b - d - g + 15) % 30
  const i = Math.floor(c / 4)
  const k = c % 4
  const l = (32 + 2 * e + 2 * i - h - k) % 7
  const m = Math.floor((a + 11 * h + 22 * l) / 451)
  const mes = Math.floor((h + l - 7 * m + 114) / 31)
  const dia = ((h + l - 7 * m + 114) % 31) + 1
  return new Date(Date.UTC(ano, mes - 1, dia))
}

function addDias(d, n) {
  const x = new Date(d)
  x.setUTCDate(x.getUTCDate() + n)
  return x
}

function toISO(d) {
  return d.toISOString().substring(0, 10)
}

export function feriadosDoAno(ano) {
  const pascoa = calcularPascoa(ano)
  const carnaval = addDias(pascoa, -47)      // Terca de Carnaval
  const sextaSanta = addDias(pascoa, -2)
  const corpusChristi = addDias(pascoa, 60)

  return [
    { data: `${ano}-01-01`, nome: 'Confraternizacao Universal' },
    { data: toISO(carnaval), nome: 'Carnaval' },
    { data: toISO(sextaSanta), nome: 'Sexta-feira Santa' },
    { data: toISO(pascoa), nome: 'Pascoa' },
    { data: `${ano}-04-21`, nome: 'Tiradentes' },
    { data: `${ano}-05-01`, nome: 'Dia do Trabalho' },
    { data: toISO(corpusChristi), nome: 'Corpus Christi' },
    { data: `${ano}-09-07`, nome: 'Independencia do Brasil' },
    { data: `${ano}-10-12`, nome: 'Nossa Senhora Aparecida' },
    { data: `${ano}-11-02`, nome: 'Finados' },
    { data: `${ano}-11-15`, nome: 'Proclamacao da Republica' },
    { data: `${ano}-12-25`, nome: 'Natal' }
  ]
}

// Retorna mapa { 'YYYY-MM-DD': 'Nome do feriado' } para os anos indicados
export function mapaFeriados(anos) {
  const mapa = {}
  for (const ano of anos) {
    for (const f of feriadosDoAno(ano)) mapa[f.data] = f.nome
  }
  return mapa
}

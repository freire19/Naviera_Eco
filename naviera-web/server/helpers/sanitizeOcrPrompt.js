// #DS5-215: defesa contra prompt-injection via texto OCR.
//   O OCR aceita qualquer foto/papel — atacante poderia escrever instrucoes que o Gemini
//   passaria a interpretar como comando ("ignore previous", "responda com..."), gerando
//   JSON com remetente/destinatario/itens forjados que entram direto no INSERT.
//
// Estrategia:
//   1. Neutralizar delimitadores que poderiam fechar o bloco BEGIN_OCR/END_OCR do prompt.
//   2. Neutralizar markdown fences que mudam o tipo de bloco para o LLM.
//   3. Mascarar tags pseudo-system que alguns LLMs honram (<system>, <user>, etc).
//   4. Hard cap de tamanho — protege contra payload gigante e contra custo descontrolado.
//
// O caller continua responsavel por validar o JSON final com schema (AJV ou check manual).

const MAX_OCR_CHARS = 8000

export function sanitizeOcrForPrompt(text) {
  if (text == null) return ''
  let s = String(text)
  if (s.length > MAX_OCR_CHARS) s = s.slice(0, MAX_OCR_CHARS)
  return s
    .replace(/"""/g, '" " "')
    .replace(/```/g, '` ` `')
    .replace(/<\/?(instr|system|user|assistant|tool|prompt)\b[^>]*>/gi, '[TAG]')
    .replace(/END_OCR/gi, 'END__OCR')
    .replace(/BEGIN_OCR/gi, 'BEGIN__OCR')
}

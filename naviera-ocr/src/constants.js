/** Status do workflow OCR — mesmos valores usados no banco e BFF */
export const OCR_STATUS = {
  PENDENTE: 'pendente',
  REVISADO: 'revisado_operador',
  APROVADO: 'aprovado',
  REJEITADO: 'rejeitado'
}

export const STATUS_LABELS = {
  [OCR_STATUS.PENDENTE]: 'Pendente',
  [OCR_STATUS.REVISADO]: 'Revisado',
  [OCR_STATUS.APROVADO]: 'Aprovado',
  [OCR_STATUS.REJEITADO]: 'Rejeitado'
}

export const FILTROS_STATUS = [
  { key: '', label: 'Todos' },
  { key: OCR_STATUS.PENDENTE, label: 'Pendentes' },
  { key: OCR_STATUS.REVISADO, label: 'Revisados' },
  { key: OCR_STATUS.APROVADO, label: 'Aprovados' },
  { key: OCR_STATUS.REJEITADO, label: 'Rejeitados' }
]

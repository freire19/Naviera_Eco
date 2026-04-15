/**
 * Constantes de status do workflow OCR.
 * Usado em rotas, queries e frontend.
 */
export const OCR_STATUS = {
  PENDENTE: 'pendente',
  REVISADO: 'revisado_operador',
  APROVADO: 'aprovado',
  REJEITADO: 'rejeitado'
}

/** Status que permitem edicao pelo operador */
export const EDITAVEIS = [OCR_STATUS.PENDENTE, OCR_STATUS.REVISADO]

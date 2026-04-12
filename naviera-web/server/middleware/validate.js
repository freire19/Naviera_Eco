/**
 * Middleware de validacao de request body.
 *
 * Uso:
 *   validate({ id_viagem: 'required', valor_total: 'number', nome: 'string' })
 *
 * Tipos: required (nao-nulo/vazio), string, number, boolean, array, integer
 */
export function validate(schema) {
  return (req, res, next) => {
    const errors = []
    const body = req.body || {}

    for (const [field, rule] of Object.entries(schema)) {
      const value = body[field]
      const rules = rule.split('|')
      const isRequired = rules.includes('required')

      if (value === undefined || value === null || value === '') {
        if (isRequired) errors.push(`${field} e obrigatorio`)
        continue
      }

      for (const r of rules) {
        if (r === 'required') continue

        if (r === 'string' && typeof value !== 'string') {
          errors.push(`${field} deve ser texto`)
        } else if (r === 'number' && (typeof value !== 'number' || isNaN(value))) {
          const parsed = Number(value)
          if (isNaN(parsed)) errors.push(`${field} deve ser numero`)
        } else if (r === 'integer') {
          if (!Number.isInteger(Number(value))) errors.push(`${field} deve ser inteiro`)
        } else if (r === 'boolean' && typeof value !== 'boolean') {
          errors.push(`${field} deve ser booleano`)
        } else if (r === 'array' && !Array.isArray(value)) {
          errors.push(`${field} deve ser lista`)
        } else if (r.startsWith('min:')) {
          const min = Number(r.split(':')[1])
          if (typeof value === 'string' && value.length < min) errors.push(`${field} deve ter no minimo ${min} caracteres`)
          if (typeof value === 'number' && value < min) errors.push(`${field} deve ser no minimo ${min}`)
        } else if (r.startsWith('max:')) {
          const max = Number(r.split(':')[1])
          if (typeof value === 'string' && value.length > max) errors.push(`${field} deve ter no maximo ${max} caracteres`)
          if (typeof value === 'number' && value > max) errors.push(`${field} deve ser no maximo ${max}`)
        }
      }
    }

    if (errors.length > 0) {
      return res.status(400).json({ error: errors.join('; ') })
    }

    next()
  }
}

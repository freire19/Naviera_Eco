import bcrypt from 'bcryptjs'
import pool from '../db.js'

/**
 * Valida login + senha contra o cadastro de usuarios da empresa.
 * Retorna { id, nome } do autorizador, ou null se invalido.
 *
 * Usado em estornos e exclusoes de passagem/encomenda/frete —
 * qualquer usuario ativo pode autorizar (nao apenas admin), mas
 * o registro fica no log com nome/id para auditoria.
 */
export async function validarAutorizador(login, senha, empresaId) {
  if (!login || !senha || !empresaId) return null
  const result = await pool.query(
    `SELECT id, nome, senha FROM usuarios
     WHERE (LOWER(nome) = LOWER($1) OR LOWER(email) = LOWER($1))
       AND (excluido = FALSE OR excluido IS NULL)
       AND empresa_id = $2`,
    [login, empresaId]
  )
  if (result.rows.length === 0) return null
  const user = result.rows[0]
  const valida = await bcrypt.compare(senha, user.senha)
  if (!valida) return null
  return { id: user.id, nome: user.nome }
}

import { Router } from 'express'
import pool from '../db.js'

/**
 * Creates CRUD route handlers for a tenant-scoped table (has empresa_id).
 *
 * @param {object} opts
 * @param {string} opts.table        - Table name (e.g. 'conferentes')
 * @param {string} opts.idColumn     - Primary key column (e.g. 'id_conferente')
 * @param {string} opts.nameColumn   - Column used for the name field (e.g. 'nome_conferente')
 * @param {string} opts.nameField    - Body field name sent by client (default: 'nome')
 * @param {string} opts.orderBy      - ORDER BY column (default: nameColumn)
 * @param {string} [opts.listSelect] - Custom SELECT for GET list (default: '*')
 * @param {string} [opts.listWhere]  - Extra WHERE clause for GET list (e.g. 'ativo = TRUE')
 * @returns {{ list, create, update, remove }} — Express handlers
 */
export function tenantCrud({ table, idColumn, nameColumn, nameField = 'nome', orderBy, listSelect, listWhere }) {
  const order = orderBy || nameColumn

  function list(req, res, next) {
    const empresaId = req.user.empresa_id
    const select = listSelect || '*'
    const extraWhere = listWhere ? ` AND ${listWhere}` : ''
    pool.query(
      `SELECT ${select} FROM ${table} WHERE empresa_id = $1${extraWhere} ORDER BY ${order}`,
      [empresaId]
    )
      .then(result => res.json(result.rows))
      .catch(next)
  }

  function create(req, res, next) {
    const empresaId = req.user.empresa_id
    const name = req.body[nameField]
    if (!name) return res.status(400).json({ error: `${nameField} obrigatorio` })
    pool.query(
      `INSERT INTO ${table} (${nameColumn}, empresa_id) VALUES ($1, $2) RETURNING *`,
      [name, empresaId]
    )
      .then(result => res.status(201).json(result.rows[0]))
      .catch(next)
  }

  function update(req, res, next) {
    const empresaId = req.user.empresa_id
    const name = req.body[nameField]
    pool.query(
      `UPDATE ${table} SET ${nameColumn} = $1 WHERE ${idColumn} = $2 AND empresa_id = $3 RETURNING *`,
      [name, req.params.id, empresaId]
    )
      .then(result => {
        if (result.rows.length === 0) return res.status(404).json({ error: 'Nao encontrado' })
        res.json(result.rows[0])
      })
      .catch(next)
  }

  function remove(req, res, next) {
    const empresaId = req.user.empresa_id
    pool.query(
      `DELETE FROM ${table} WHERE ${idColumn} = $1 AND empresa_id = $2 RETURNING ${idColumn}`,
      [req.params.id, empresaId]
    )
      .then(result => {
        if (result.rows.length === 0) return res.status(404).json({ error: 'Nao encontrado' })
        res.json({ ok: true })
      })
      .catch(next)
  }

  return { list, create, update, remove }
}

/**
 * Creates CRUD route handlers for a shared auxiliary table (NO empresa_id).
 *
 * @param {object} opts
 * @param {string} opts.table      - Table name (e.g. 'aux_sexo')
 * @param {string} opts.idColumn   - Primary key column (e.g. 'id_sexo')
 * @param {string} opts.nameColumn - Column used for the name field (e.g. 'nome_sexo')
 * @param {string} [opts.selectCols] - Columns to SELECT for list (default: idColumn, nameColumn)
 * @param {string} [opts.orderBy]  - ORDER BY column (default: nameColumn)
 * @returns {{ list, create, update, remove }} — Express handlers
 */
export function auxCrud({ table, idColumn, nameColumn, selectCols, orderBy }) {
  const order = orderBy || nameColumn
  const cols = selectCols || `${idColumn}, ${nameColumn}`

  function list(_req, res, next) {
    pool.query(`SELECT ${cols} FROM ${table} ORDER BY ${order}`)
      .then(result => res.json(result.rows))
      .catch(next)
  }

  function create(req, res, next) {
    const { nome } = req.body
    if (!nome) return res.status(400).json({ error: 'nome obrigatorio' })
    pool.query(
      `INSERT INTO ${table} (${nameColumn}) VALUES ($1) RETURNING *`,
      [nome]
    )
      .then(result => res.status(201).json(result.rows[0]))
      .catch(next)
  }

  function update(req, res, next) {
    const { nome } = req.body
    pool.query(
      `UPDATE ${table} SET ${nameColumn} = $1 WHERE ${idColumn} = $2 RETURNING *`,
      [nome, req.params.id]
    )
      .then(result => {
        if (result.rows.length === 0) return res.status(404).json({ error: 'Nao encontrado' })
        res.json(result.rows[0])
      })
      .catch(next)
  }

  function remove(req, res, next) {
    pool.query(
      `DELETE FROM ${table} WHERE ${idColumn} = $1 RETURNING ${idColumn}`,
      [req.params.id]
    )
      .then(result => {
        if (result.rows.length === 0) return res.status(404).json({ error: 'Nao encontrado' })
        res.json({ ok: true })
      })
      .catch(next)
  }

  return { list, create, update, remove }
}

/**
 * Registers GET/POST/PUT/DELETE on a router for a given path + handlers.
 */
export function registerCrud(router, path, handlers) {
  router.get(path, handlers.list)
  if (handlers.create) router.post(path, handlers.create)
  if (handlers.update) router.put(`${path}/:id`, handlers.update)
  if (handlers.remove) router.delete(`${path}/:id`, handlers.remove)
}

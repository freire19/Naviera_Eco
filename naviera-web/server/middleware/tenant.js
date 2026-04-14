import pool from '../db.js'

// Cache de slug → empresa para evitar query a cada request
const cache = new Map()
const CACHE_TTL = 5 * 60 * 1000 // 5 minutos

/**
 * Middleware que resolve o tenant (empresa) pelo slug do subdominio.
 * O slug vem do header X-Tenant-Slug (setado pelo Nginx)
 * ou e extraido do Host header diretamente.
 * 
 * Resultado: req.tenant = { id, nome, slug, logo_url, cor_primaria }
 * Usado na rota de login para validar usuario da empresa correta.
 */
export async function tenantMiddleware(req, res, next) {
  // 1. Tentar header X-Tenant-Slug (setado pelo Nginx em producao)
  let slug = req.headers['x-tenant-slug']

  // 2. Fallback: extrair do Host header (dev sem Nginx)
  if (!slug) {
    const host = req.headers.host || ''
    const match = host.match(/^([a-z0-9-]+)\.naviera\.com\.br/i)
    if (match) {
      slug = match[1].toLowerCase()
    }
  }

  // 3. Se nao tem slug (localhost em dev), usar empresa padrao
  if (!slug || slug === 'localhost' || slug === 'api' || slug === 'app' || slug === 'admin' || slug === 'ocr') {
    req.tenant = null // sem tenant especifico — login aceita qualquer empresa
    return next()
  }

  // 4. Buscar empresa pelo slug (com cache)
  const cacheKey = slug.toLowerCase()
  const cached = cache.get(cacheKey)
  if (cached && Date.now() - cached.ts < CACHE_TTL) {
    req.tenant = cached.data
    return next()
  }

  try {
    const result = await pool.query(
      'SELECT id, nome, slug, logo_url, cor_primaria FROM empresas WHERE slug = $1 AND ativo = TRUE',
      [cacheKey]
    )
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Empresa nao encontrada' })
    }
    const empresa = result.rows[0]
    cache.set(cacheKey, { data: empresa, ts: Date.now() })
    req.tenant = empresa
    next()
  } catch (err) {
    console.error('[Tenant] Erro ao resolver slug:', err.message)
    res.status(500).json({ error: 'Erro ao identificar empresa' })
  }
}

/**
 * Endpoint para o frontend buscar info da empresa pelo slug (sem auth)
 * GET /api/tenant/info?slug=saofrancisco
 * ou GET /api/tenant/info (usa X-Tenant-Slug header)
 */
export function tenantInfoRoute(req, res) {
  if (req.tenant) {
    res.json(req.tenant)
  } else {
    res.json(null)
  }
}

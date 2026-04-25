import pool from '../db.js'

// Cache de slug → empresa para evitar query a cada request
const cache = new Map()
// DS4-040 fix: reduzido de 5min para 60s (empresa desativada fica acessivel por menos tempo)
const CACHE_TTL = 60 * 1000 // 60 segundos

// #650: X-Tenant-Slug SO e confiavel quando vem do Nginx local (loopback) ou de um proxy explicitamente
// declarado em TRUSTED_PROXY_IPS. Sem validar, qualquer cliente mandava o header e forjava tenant
// (bypass multi-tenant). A conexao TCP chega em req.socket.remoteAddress — checamos este, pois req.ip
// respeita trust proxy e pode refletir valores setados pelo proprio atacante via X-Forwarded-For.
const TRUSTED_PROXY_IPS = new Set(
  (process.env.TRUSTED_PROXY_IPS || '').split(',').map(s => s.trim()).filter(Boolean)
)
function isFromTrustedProxy(req) {
  const ra = req.socket?.remoteAddress || ''
  if (!ra) return false
  if (ra === '127.0.0.1' || ra === '::1' || ra.startsWith('::ffff:127.')) return true
  if (TRUSTED_PROXY_IPS.has(ra)) return true
  // Suporta "::ffff:10.0.0.1" => "10.0.0.1"
  const stripped = ra.startsWith('::ffff:') ? ra.slice(7) : ra
  return TRUSTED_PROXY_IPS.has(stripped)
}

/**
 * Middleware que resolve o tenant (empresa) pelo slug do subdominio.
 * O slug vem do header X-Tenant-Slug (setado pelo Nginx)
 * ou e extraido do Host header diretamente.
 *
 * Resultado: req.tenant = { id, nome, slug, logo_url, cor_primaria }
 * Usado na rota de login para validar usuario da empresa correta.
 */
export async function tenantMiddleware(req, res, next) {
  // #650: descartar X-Tenant-Slug se a conexao nao vem de proxy confiavel — evita spoof do tenant.
  if (req.headers['x-tenant-slug'] && !isFromTrustedProxy(req)) {
    delete req.headers['x-tenant-slug']
  }

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

/**
 * #024: invalidacao explicita do cache tenant.
 * Admin chama apos toggle de ativo/alteracao de nome/cor/logo — evita janela de 60s
 * com dados stale (ex: empresa desativada ainda aceitando logins).
 * Sem slug limpa tudo (usar quando mutar em lote).
 */
export function invalidateTenantCache(slug) {
  if (!slug) {
    cache.clear()
    return
  }
  cache.delete(slug.toLowerCase())
}

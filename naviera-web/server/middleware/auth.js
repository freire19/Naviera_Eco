import jwt from 'jsonwebtoken'
import pool from '../db.js'
import { createBoundedMap } from '../utils/boundedCache.js'

// #234: cache de senha_atualizada_em (TTL 60s) — invalidado em /trocar-senha.
// #DS5-220 (debito relacionado): bounded em 5000 ids para evitar growth ilimitado.
const senhaEpochCache = createBoundedMap(5000)
const SENHA_CACHE_TTL_MS = 60_000

export function invalidateSenhaCache(userId) {
  if (userId != null) senhaEpochCache.delete(Number(userId))
}

const SECRET = process.env.JWT_SECRET
if (!SECRET) {
  console.error('[Auth] ERRO FATAL: JWT_SECRET nao configurado. Defina a variavel de ambiente JWT_SECRET.')
  process.exit(1)
}

// #DS5-405: rejeita secrets fracos no boot (comprimento + padroes previsiveis)
const PADROES_FRACOS = ['dev', 'local', 'naviera', 'secret', 'changeme', 'default', 'test', '123', 'password']
if (Buffer.byteLength(SECRET, 'utf8') < 32) {
  console.error('[Auth] ERRO FATAL: JWT_SECRET muito curto (< 32 bytes). Use: openssl rand -base64 48')
  process.exit(1)
}
{
  const lower = SECRET.toLowerCase()
  for (const p of PADROES_FRACOS) {
    if (lower.includes(p)) {
      console.error(`[Auth] ERRO FATAL: JWT_SECRET contem padrao previsivel ('${p}'). Gere um secret aleatorio real.`)
      process.exit(1)
    }
  }
}

export function generateToken(user) {
  // #DB200: claim `tipo` obrigatoria — Spring JwtFilter so concede ROLE_OPERADOR/ROLE_ADMIN
  // quando tipo === 'OPERADOR'; sem ela, proxy para /admin/** e /op/** cai em ROLE_CPF e retorna 403.
  // #100/#114: super_admin (bool) emite ROLE_SUPERADMIN no Spring e libera /api/admin no BFF —
  //   flag so vale quando usuarios.super_admin = TRUE no DB.
  return jwt.sign(
    {
      id: user.id,
      login: user.login_usuario || user.nome || user.login,
      tipo: 'OPERADOR',
      funcao: user.funcao,
      empresa_id: user.empresa_id,
      super_admin: user.super_admin === true
    },
    SECRET,
    { expiresIn: '8h', algorithm: 'HS256' }
  )
}

export async function authMiddleware(req, res, next) {
  const header = req.headers.authorization
  if (!header || !header.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Token nao fornecido' })
  }

  try {
    const token = header.slice(7)
    const decoded = jwt.verify(token, SECRET, { algorithms: ['HS256'] })

    // #234: invalida JWTs emitidos antes da ultima troca de senha.
    //   Cache com TTL 60s evita SELECT por request (hot path em dezenas de rotas).
    if (decoded.tipo === 'OPERADOR' && decoded.iat && decoded.id) {
      const senhaEpoch = await getSenhaEpoch(decoded.id, decoded.empresa_id)
      if (senhaEpoch != null && decoded.iat < senhaEpoch) {
        return res.status(401).json({ error: 'Sessao expirada apos troca de senha. Refaca login.' })
      }
    }

    req.user = decoded
    next()
  } catch {
    return res.status(401).json({ error: 'Token invalido ou expirado' })
  }
}

async function getSenhaEpoch(userId, empresaId) {
  const key = Number(userId)
  const hit = senhaEpochCache.get(key)
  if (hit && hit.expiresAt > Date.now()) return hit.epoch
  try {
    const r = await pool.query(
      'SELECT senha_atualizada_em FROM usuarios WHERE id = $1 AND empresa_id = $2',
      [userId, empresaId]
    )
    const epoch = r.rows.length > 0 && r.rows[0].senha_atualizada_em
      ? Math.floor(new Date(r.rows[0].senha_atualizada_em).getTime() / 1000)
      : null
    senhaEpochCache.set(key, { epoch, expiresAt: Date.now() + SENHA_CACHE_TTL_MS })
    return epoch
  } catch (err) {
    // Falha transitoria no DB: loga mas NAO bloqueia auth (token JWT ja foi validado criptograficamente)
    console.warn('[Auth] Falha ao ler senha_atualizada_em:', err.message)
    return null
  }
}

import jwt from 'jsonwebtoken'

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
  return jwt.sign(
    { id: user.id, login: user.login_usuario || user.nome || user.login, tipo: 'OPERADOR', funcao: user.funcao, empresa_id: user.empresa_id },
    SECRET,
    { expiresIn: '8h' }
  )
}

export function authMiddleware(req, res, next) {
  const header = req.headers.authorization
  if (!header || !header.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Token nao fornecido' })
  }

  try {
    const token = header.slice(7)
    const decoded = jwt.verify(token, SECRET)
    req.user = decoded
    next()
  } catch {
    return res.status(401).json({ error: 'Token invalido ou expirado' })
  }
}

import jwt from 'jsonwebtoken'

const SECRET = process.env.JWT_SECRET
if (!SECRET) {
  console.error('[Auth] ERRO FATAL: JWT_SECRET nao configurado. Defina a variavel de ambiente JWT_SECRET.')
  process.exit(1)
}

export function generateToken(user) {
  return jwt.sign(
    { id: user.id, login: user.login_usuario, funcao: user.funcao, empresa_id: user.empresa_id },
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

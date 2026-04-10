import jwt from 'jsonwebtoken'

const SECRET = process.env.JWT_SECRET || 'naviera-web-secret'

export function generateToken(user) {
  return jwt.sign(
    { id: user.id, login: user.login_usuario, funcao: user.funcao },
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

import pg from 'pg'
const { Pool } = pg

// #DR271: keepAlive evita firewall/LB matar conexoes ociosas sem o pool saber
//   (proxima query receberia ECONNRESET). allowExitOnIdle:false impede graceful shutdown
//   sair antes de pool.end().
const pool = new Pool({
  host: process.env.DB_HOST || 'localhost',
  port: parseInt(process.env.DB_PORT || '5437'),
  database: process.env.DB_NAME || 'naviera_eco',
  user: process.env.DB_USER || 'postgres',
  password: process.env.DB_PASSWORD,
  max: 10,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 5000,
  statement_timeout: 30000,
  query_timeout: 30000,
  keepAlive: true,
  keepAliveInitialDelayMillis: 10000,
  allowExitOnIdle: false
})

pool.on('error', (err) => {
  console.error(`${new Date().toISOString().slice(0,23)} ERROR [DB] Pool error: ${err.message}`)
})

export default pool

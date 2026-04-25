import { useState, useEffect, createContext, useContext } from 'react'
import Login from './pages/Login.jsx'
import Layout from './components/Layout.jsx'
import ErrorBoundary from './components/ErrorBoundary.jsx'
import './styles/global.css'

export const AuthContext = createContext(null)
export const useAuth = () => useContext(AuthContext)

export default function App() {
  // #DP080: try/catch protege contra localStorage com JSON corrompido (que crashava o App
  //   antes de qualquer ErrorBoundary entrar) e contra sandbox sem localStorage.
  const [usuario, setUsuario] = useState(() => {
    try {
      const saved = localStorage.getItem('naviera_usuario')
      return saved ? JSON.parse(saved) : null
    } catch {
      try { localStorage.removeItem('naviera_usuario') } catch { /* sandbox */ }
      return null
    }
  })

  const [theme, setTheme] = useState(() => localStorage.getItem('naviera_theme') || 'light')

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
    localStorage.setItem('naviera_theme', theme)
  }, [theme])

  function login(data) {
    localStorage.setItem('naviera_token', data.token)
    localStorage.setItem('naviera_usuario', JSON.stringify(data.usuario))
    setUsuario(data.usuario)
  }

  function logout() {
    localStorage.removeItem('naviera_token')
    localStorage.removeItem('naviera_usuario')
    setUsuario(null)
  }

  function toggleTheme() {
    setTheme(t => t === 'light' ? 'dark' : 'light')
  }

  if (!usuario) return <Login onLogin={login} theme={theme} toggleTheme={toggleTheme} />

  return (
    <AuthContext.Provider value={{ usuario, logout, theme, toggleTheme }}>
      <ErrorBoundary>
        <Layout />
      </ErrorBoundary>
    </AuthContext.Provider>
  )
}

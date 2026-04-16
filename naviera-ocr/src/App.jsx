import { useState, useCallback } from 'react'
import T from './theme.js'
import './App.css'

import Header from './components/Header.jsx'
import BottomNav from './components/BottomNav.jsx'
import Toast from './components/Toast.jsx'

import LoginScreen from './screens/LoginScreen.jsx'
import CapturaScreen from './screens/CapturaScreen.jsx'
import RevisaoScreen from './screens/RevisaoScreen.jsx'
import ConfirmadoScreen from './screens/ConfirmadoScreen.jsx'
import HistoricoScreen from './screens/HistoricoScreen.jsx'
import RevisaoLoteScreen from './screens/RevisaoLoteScreen.jsx'



export default function App() {
  // Auth
  const [token, setToken] = useState(() => localStorage.getItem('naviera_ocr_token'))
  const [usuario, setUsuario] = useState(() => {
    try { return JSON.parse(localStorage.getItem('naviera_ocr_usuario')) } catch { return null }
  })

  // Theme
  const [mode, setMode] = useState(() => localStorage.getItem('naviera_ocr_mode') || 'light')
  const t = T[mode]

  // Navigation
  const [tab, setTab] = useState('captura')
  const [screen, setScreen] = useState('captura') // captura | revisao | confirmado | historico
  const [lancamentoAtual, setLancamentoAtual] = useState(null)
  const [dadosAtual, setDadosAtual] = useState(null)
  const [loteData, setLoteData] = useState(null)

  // Toast
  const [toast, setToast] = useState(null)
  const showToast = useCallback((message, type = 'info') => {
    setToast({ message, type, key: Date.now() })
  }, [])



  // Login
  const handleLogin = (data) => {
    localStorage.setItem('naviera_ocr_token', data.token)
    localStorage.setItem('naviera_ocr_usuario', JSON.stringify(data.usuario))
    setToken(data.token)
    setUsuario(data.usuario)
  }

  // Logout
  const handleLogout = () => {
    localStorage.removeItem('naviera_ocr_token')
    localStorage.removeItem('naviera_ocr_usuario')
    setToken(null)
    setUsuario(null)
  }

  // Theme toggle
  const toggleMode = () => {
    const next = mode === 'light' ? 'dark' : 'light'
    setMode(next)
    localStorage.setItem('naviera_ocr_mode', next)
  }

  // OCR result from upload
  const handleOcrResult = (result) => {
    if (result.tipo === 'lote') {
      setLoteData(result.lancamentos)
      setScreen('revisao-lote')
    } else {
      setLancamentoAtual(result.lancamento)
      setDadosAtual(result.dados_extraidos)
      setScreen('revisao')
    }
  }

  // After operator confirms review
  const handleConfirm = (lancamentoId) => {
    setLancamentoAtual({ ...lancamentoAtual, id: lancamentoId })
    setScreen('confirmado')
    showToast('Lancamento enviado para revisao do conferente', 'success')
  }

  // After operator confirms lote
  const handleLoteConfirm = (ids) => {
    setLoteData(null)
    setLancamentoAtual({ id: ids[0] })
    setScreen('confirmado')
    showToast(`${ids.length} encomendas enviadas para revisao do conferente`, 'success')
  }

  // Navigation
  const goCaptura = () => { setScreen('captura'); setTab('captura'); setLancamentoAtual(null); setDadosAtual(null); setLoteData(null) }
  const goHistorico = () => { setScreen('historico'); setTab('historico') }
  const handleTab = (key) => {
    setTab(key)
    setScreen(key)
    if (key === 'captura') { setLancamentoAtual(null); setDadosAtual(null) }
  }

  // Not logged in
  if (!token) {
    return <LoginScreen t={t} onLogin={handleLogin} />
  }

  // Render screen
  let content
  switch (screen) {
    case 'revisao':
      content = lancamentoAtual && dadosAtual ? (
        <RevisaoScreen
          t={t}
          lancamento={lancamentoAtual}
          dados={dadosAtual}
          onConfirm={handleConfirm}
          showToast={showToast}
        />
      ) : null
      break
    case 'revisao-lote':
      content = loteData ? (
        <RevisaoLoteScreen
          t={t}
          lancamentos={loteData}
          onConfirm={handleLoteConfirm}
          showToast={showToast}
        />
      ) : null
      break
    case 'confirmado':
      content = (
        <ConfirmadoScreen
          t={t}
          lancamentoId={lancamentoAtual?.id}
          onNovo={goCaptura}
          onHistorico={goHistorico}
        />
      )
      break
    case 'historico':
      content = <HistoricoScreen t={t} showToast={showToast} />
      break
    default:
      content = (
        <CapturaScreen
          t={t}
          onResult={handleOcrResult}
          showToast={showToast}
        />
      )
  }

  return (
    <div style={{ background: t.bg, minHeight: '100vh', color: t.tx }}>
      <Header
        t={t}
        usuario={usuario}
        onLogout={handleLogout}
        mode={mode}
        onToggleMode={toggleMode}
      />

      <main style={{ paddingBottom: 70 }}>
        {content}
      </main>

      <BottomNav tab={tab} onTab={handleTab} t={t} />

      {toast && (
        <Toast
          key={toast.key}
          message={toast.message}
          type={toast.type}
          t={t}
          onClose={() => setToast(null)}
        />
      )}
    </div>
  )
}

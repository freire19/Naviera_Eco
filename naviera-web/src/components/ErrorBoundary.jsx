import { Component } from 'react'

export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error }
  }

  componentDidCatch(error, errorInfo) {
    console.error('[ErrorBoundary] Erro capturado:', error, errorInfo)
  }

  handleReload = () => {
    window.location.reload()
  }

  render() {
    if (this.state.hasError) {
      return (
        <div style={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: '100vh',
          padding: '2rem',
          backgroundColor: 'var(--bg-card)',
          color: 'var(--text)',
          textAlign: 'center',
          fontFamily: 'inherit'
        }}>
          <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>:(</div>
          <h1 style={{ fontSize: '1.5rem', marginBottom: '0.5rem' }}>
            Algo deu errado
          </h1>
          <p style={{ color: 'var(--text-muted)', marginBottom: '1.5rem', maxWidth: '400px' }}>
            Ocorreu um erro inesperado. Tente recarregar a pagina.
          </p>
          {this.state.error && (
            <pre style={{
              color: 'var(--text-muted)',
              fontSize: '0.75rem',
              marginBottom: '1.5rem',
              maxWidth: '500px',
              overflow: 'auto',
              padding: '0.75rem',
              borderRadius: '6px',
              backgroundColor: 'rgba(0,0,0,0.05)',
              whiteSpace: 'pre-wrap',
              wordBreak: 'break-word'
            }}>
              {this.state.error.message}
            </pre>
          )}
          <button
            onClick={this.handleReload}
            style={{
              padding: '0.625rem 1.5rem',
              backgroundColor: 'var(--primary)',
              color: '#fff',
              border: 'none',
              borderRadius: '6px',
              cursor: 'pointer',
              fontSize: '0.95rem',
              fontWeight: 600
            }}
          >
            Recarregar
          </button>
        </div>
      )
    }

    return this.props.children
  }
}

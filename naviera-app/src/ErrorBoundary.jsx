import { Component } from 'react';

export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, info) {
    console.error('[ErrorBoundary]', error, info);
    // #DR285: melhor esforco — envia para BFF para diagnostico de "tela branca" em prod.
    //   keepalive permite o request sobreviver ao unload da pagina; .catch silencia
    //   se o endpoint nao existir (nao queremos error boundary causar erro adicional).
    try {
      const apiBase = (typeof window !== 'undefined' && window.location?.origin) || '';
      fetch(`${apiBase}/api/client-errors`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        keepalive: true,
        body: JSON.stringify({
          message: error?.message?.slice(0, 500) || 'unknown',
          stack: error?.stack?.slice(0, 2000) || null,
          componentStack: info?.componentStack?.slice(0, 2000) || null,
          user_agent: navigator?.userAgent?.slice(0, 200) || null,
          rota: window?.location?.pathname || null
        })
      }).catch(() => {});
    } catch { /* never let logging crash the boundary */ }
  }

  render() {
    if (this.state.hasError) {
      return (
        <div style={{ padding: 40, textAlign: 'center', fontFamily: 'sans-serif' }}>
          <h2>Algo deu errado</h2>
          <p>Tente recarregar a pagina.</p>
          <button onClick={() => window.location.reload()} style={{ padding: '10px 20px', fontSize: 16, cursor: 'pointer' }}>
            Recarregar
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}

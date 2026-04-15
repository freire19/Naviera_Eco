import React from 'react'
import ReactDOM from 'react-dom/client'
import Naviera from './App.jsx'
import ErrorBoundary from './ErrorBoundary'

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <ErrorBoundary>
      <Naviera />
    </ErrorBoundary>
  </React.StrictMode>
)

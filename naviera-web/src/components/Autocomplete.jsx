import { useState, useEffect, useRef } from 'react'

const dropdownStyle = {
  position: 'absolute',
  top: '100%',
  left: 0,
  right: 0,
  background: 'var(--bg-card)',
  border: '1px solid var(--border)',
  borderRadius: 6,
  maxHeight: 200,
  overflowY: 'auto',
  zIndex: 100,
  boxShadow: '0 4px 12px rgba(0,0,0,0.3)'
}

const itemStyle = {
  padding: '8px 12px',
  cursor: 'pointer',
  borderBottom: '1px solid var(--border)',
  fontSize: 13
}

const loadingStyle = {
  position: 'absolute',
  right: 8,
  top: 34,
  fontSize: 11,
  color: 'var(--text-muted)'
}

const emptyStyle = {
  ...dropdownStyle,
  maxHeight: 'none',
  padding: '8px 12px',
  fontSize: 12,
  color: 'var(--text-muted)'
}

export default function Autocomplete({
  value,
  onChange,
  onSelect,
  suggestions,
  loading,
  placeholder = 'Digite para buscar...',
  emptyMessage = 'Nenhum resultado encontrado.',
  renderItem,
  minChars = 2
}) {
  const [showDropdown, setShowDropdown] = useState(false)
  const containerRef = useRef(null)

  useEffect(() => {
    function handleClick(e) {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setShowDropdown(false)
      }
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [])

  function handleChange(e) {
    onChange(e.target.value)
    if (e.target.value.trim().length >= minChars) {
      setShowDropdown(true)
    } else {
      setShowDropdown(false)
    }
  }

  function handleFocus() {
    if (suggestions.length > 0) setShowDropdown(true)
  }

  function handleSelect(item) {
    onSelect(item)
    setShowDropdown(false)
  }

  const showSuggestions = showDropdown && suggestions.length > 0
  const showEmpty = showDropdown && suggestions.length === 0 && value.trim().length >= minChars && !loading

  return (
    <div ref={containerRef} style={{ position: 'relative' }}>
      <input
        value={value}
        onChange={handleChange}
        onFocus={handleFocus}
        placeholder={placeholder}
        required
        autoComplete="off"
      />
      {loading && <div style={loadingStyle}>Buscando...</div>}
      {showSuggestions && (
        <div style={dropdownStyle}>
          {suggestions.map((item, idx) => (
            <div
              key={item.id || idx}
              onClick={() => handleSelect(item)}
              style={itemStyle}
              onMouseEnter={e => e.target.style.background = 'var(--bg-hover)'}
              onMouseLeave={e => e.target.style.background = 'transparent'}
            >
              {renderItem ? renderItem(item) : <strong>{item.label}</strong>}
            </div>
          ))}
        </div>
      )}
      {showEmpty && (
        <div style={emptyStyle}>{emptyMessage}</div>
      )}
    </div>
  )
}

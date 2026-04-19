import { useState, useEffect, useRef } from 'react'

const inputStyle = {
  width: '100%',
  boxSizing: 'border-box',
  padding: '7px 10px',
  fontSize: '0.85rem',
  background: 'var(--bg-soft)',
  border: '1px solid var(--border)',
  borderRadius: 4,
  color: 'var(--text)'
}

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
  boxShadow: '0 4px 12px rgba(0,0,0,0.3)',
  whiteSpace: 'normal'
}

const itemStyle = {
  padding: '8px 12px',
  cursor: 'pointer',
  borderBottom: '1px solid var(--border)',
  fontSize: 13,
  whiteSpace: 'normal',
  wordBreak: 'break-word'
}

const itemHighlightedStyle = {
  ...itemStyle,
  background: 'var(--bg-accent)'
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
  const [highlightedIndex, setHighlightedIndex] = useState(-1)
  const containerRef = useRef(null)
  const itemRefs = useRef([])

  useEffect(() => {
    function handleClick(e) {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setShowDropdown(false)
      }
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [])

  // Reseta highlight quando sugestoes mudam
  useEffect(() => {
    setHighlightedIndex(-1)
    itemRefs.current = itemRefs.current.slice(0, suggestions.length)
  }, [suggestions])

  // Rola item destacado para dentro do viewport do dropdown
  useEffect(() => {
    if (highlightedIndex >= 0 && itemRefs.current[highlightedIndex]) {
      itemRefs.current[highlightedIndex].scrollIntoView({ block: 'nearest' })
    }
  }, [highlightedIndex])

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
    setHighlightedIndex(-1)
  }

  function handleKeyDown(e) {
    // Esc fecha dropdown mesmo sem itens
    if (e.key === 'Escape') {
      setShowDropdown(false)
      setHighlightedIndex(-1)
      return
    }
    if (!showDropdown || suggestions.length === 0) return

    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setHighlightedIndex(i => (i < suggestions.length - 1 ? i + 1 : 0))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setHighlightedIndex(i => (i > 0 ? i - 1 : suggestions.length - 1))
    } else if (e.key === 'Enter') {
      if (highlightedIndex >= 0 && suggestions[highlightedIndex]) {
        e.preventDefault() // evita submit do form
        handleSelect(suggestions[highlightedIndex])
      }
    } else if (e.key === 'Tab') {
      // Tab seleciona item destacado (se houver) e segue para o proximo campo
      if (highlightedIndex >= 0 && suggestions[highlightedIndex]) {
        handleSelect(suggestions[highlightedIndex])
      }
    }
  }

  const showSuggestions = showDropdown && suggestions.length > 0
  const showEmpty = showDropdown && suggestions.length === 0 && value.trim().length >= minChars && !loading

  return (
    <div ref={containerRef} style={{ position: 'relative', width: '100%' }}>
      <input
        value={value}
        onChange={handleChange}
        onFocus={handleFocus}
        onKeyDown={handleKeyDown}
        placeholder={placeholder}
        required
        autoComplete="off"
        style={inputStyle}
      />
      {loading && <div style={loadingStyle}>Buscando...</div>}
      {showSuggestions && (
        <div style={dropdownStyle}>
          {suggestions.map((item, idx) => (
            <div
              key={item.id || idx}
              ref={el => { itemRefs.current[idx] = el }}
              onClick={() => handleSelect(item)}
              onMouseEnter={() => setHighlightedIndex(idx)}
              style={idx === highlightedIndex ? itemHighlightedStyle : itemStyle}
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

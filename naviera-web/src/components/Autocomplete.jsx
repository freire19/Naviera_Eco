import { useState, useEffect, useRef } from 'react'

const inputStyle = {
  width: '100%',
  boxSizing: 'border-box',
  padding: '7px 28px 7px 10px', // espaço à direita pra seta
  fontSize: '0.85rem',
  background: 'var(--bg-soft)',
  border: '1px solid var(--border)',
  borderRadius: 4,
  color: 'var(--text)'
}

const arrowBtnStyle = {
  position: 'absolute',
  right: 4,
  top: '50%',
  transform: 'translateY(-50%)',
  background: 'transparent',
  border: 'none',
  cursor: 'pointer',
  color: 'var(--text-muted)',
  fontSize: 10,
  padding: '4px 6px',
  lineHeight: 1,
  userSelect: 'none'
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
  right: 24,
  top: '50%',
  transform: 'translateY(-50%)',
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
  onBlur,
  suggestions,
  allItems,
  loading,
  placeholder = 'Digite para buscar...',
  emptyMessage = 'Nenhum resultado encontrado.',
  renderItem,
  minChars = 2
}) {
  const [showDropdown, setShowDropdown] = useState(false)
  const [browsing, setBrowsing] = useState(false) // true = listando allItems (sem filtro)
  const [highlightedIndex, setHighlightedIndex] = useState(-1)
  const containerRef = useRef(null)
  const inputRef = useRef(null)
  const itemRefs = useRef([])

  // Lista exibida: allItems (browse) ou suggestions filtradas
  const displayed = browsing && Array.isArray(allItems) ? allItems : suggestions
  const hasArrow = Array.isArray(allItems) && allItems.length > 0

  useEffect(() => {
    function handleClick(e) {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setShowDropdown(false)
        setBrowsing(false)
      }
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [])

  useEffect(() => {
    setHighlightedIndex(-1)
    itemRefs.current = itemRefs.current.slice(0, displayed.length)
  }, [displayed])

  useEffect(() => {
    if (highlightedIndex >= 0 && itemRefs.current[highlightedIndex]) {
      itemRefs.current[highlightedIndex].scrollIntoView({ block: 'nearest' })
    }
  }, [highlightedIndex])

  function handleChange(e) {
    onChange(e.target.value)
    setBrowsing(false) // digitou, volta a filtrar suggestions
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
    setBrowsing(false)
    setHighlightedIndex(-1)
  }

  function toggleBrowse() {
    if (showDropdown && browsing) {
      setShowDropdown(false)
      setBrowsing(false)
    } else {
      setBrowsing(true)
      setShowDropdown(true)
      setHighlightedIndex(-1)
      inputRef.current?.focus()
    }
  }

  function handleKeyDown(e) {
    if (e.key === 'Escape') {
      setShowDropdown(false)
      setBrowsing(false)
      setHighlightedIndex(-1)
      return
    }
    // ArrowDown em input vazio OU Alt+ArrowDown abre lista completa
    if (e.key === 'ArrowDown' && hasArrow && !showDropdown && (value.length === 0 || e.altKey)) {
      e.preventDefault()
      setBrowsing(true)
      setShowDropdown(true)
      setHighlightedIndex(0)
      return
    }
    if (!showDropdown || displayed.length === 0) return

    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setHighlightedIndex(i => (i < displayed.length - 1 ? i + 1 : 0))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setHighlightedIndex(i => (i > 0 ? i - 1 : displayed.length - 1))
    } else if (e.key === 'Enter') {
      if (highlightedIndex >= 0 && displayed[highlightedIndex]) {
        e.preventDefault()
        handleSelect(displayed[highlightedIndex])
      }
    } else if (e.key === 'Tab') {
      if (highlightedIndex >= 0 && displayed[highlightedIndex]) {
        handleSelect(displayed[highlightedIndex])
      }
    }
  }

  const showSuggestions = showDropdown && displayed.length > 0
  const showEmpty = showDropdown && !browsing && displayed.length === 0 && value.trim().length >= minChars && !loading

  return (
    <div ref={containerRef} style={{ position: 'relative', width: '100%' }}>
      <input
        ref={inputRef}
        value={value}
        onChange={handleChange}
        onFocus={handleFocus}
        onKeyDown={handleKeyDown}
        onBlur={onBlur ? () => setTimeout(() => onBlur(value), 200) : undefined}
        placeholder={placeholder}
        required
        autoComplete="off"
        style={inputStyle}
      />
      {loading && <div style={loadingStyle}>...</div>}
      {hasArrow && (
        <button
          type="button"
          tabIndex={-1}
          onMouseDown={e => e.preventDefault()} // evita blur ao clicar
          onClick={toggleBrowse}
          style={arrowBtnStyle}
          title="Ver todos (↓ quando campo vazio)"
        >▼</button>
      )}
      {showSuggestions && (
        <div style={dropdownStyle}>
          {displayed.map((item, idx) => (
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

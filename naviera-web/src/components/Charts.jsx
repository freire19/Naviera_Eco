/**
 * Pure SVG chart components — no external dependencies.
 * Uses CSS custom properties for theme integration.
 */

export function PieChart({ data = [], size = 200 }) {
  const total = data.reduce((sum, d) => sum + (d.value || 0), 0)
  if (total === 0) return null

  const cx = size / 2
  const cy = size / 2
  const r = size / 2 - 10

  let cumulativeAngle = -Math.PI / 2 // start at top

  const slices = data.filter(d => d.value > 0).map((d) => {
    const angle = (d.value / total) * 2 * Math.PI
    const startX = cx + r * Math.cos(cumulativeAngle)
    const startY = cy + r * Math.sin(cumulativeAngle)
    const endX = cx + r * Math.cos(cumulativeAngle + angle)
    const endY = cy + r * Math.sin(cumulativeAngle + angle)
    const largeArc = angle > Math.PI ? 1 : 0

    // Label position (midpoint of arc)
    const midAngle = cumulativeAngle + angle / 2
    const labelR = r * 0.65
    const labelX = cx + labelR * Math.cos(midAngle)
    const labelY = cy + labelR * Math.sin(midAngle)
    const pct = ((d.value / total) * 100).toFixed(1)

    const path = `M ${cx} ${cy} L ${startX} ${startY} A ${r} ${r} 0 ${largeArc} 1 ${endX} ${endY} Z`

    cumulativeAngle += angle

    return { ...d, path, labelX, labelY, pct }
  })

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8 }}>
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
        {slices.map((s, i) => (
          <g key={i}>
            <path d={s.path} fill={s.color} stroke="var(--bg-card)" strokeWidth="2" />
            {parseFloat(s.pct) >= 5 && (
              <text
                x={s.labelX}
                y={s.labelY}
                textAnchor="middle"
                dominantBaseline="central"
                fill="#fff"
                fontSize={11}
                fontWeight="600"
              >
                {s.pct}%
              </text>
            )}
          </g>
        ))}
      </svg>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px 16px', justifyContent: 'center' }}>
        {data.filter(d => d.value > 0).map((d, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12 }}>
            <span style={{ width: 12, height: 12, borderRadius: 2, background: d.color, display: 'inline-block', flexShrink: 0 }} />
            <span style={{ color: 'var(--text-primary)' }}>{d.label}</span>
          </div>
        ))}
      </div>
    </div>
  )
}

export function BarChart({ data = [], width = 400, height = 220 }) {
  const maxValue = Math.max(...data.map(d => d.value || 0), 1)
  if (data.length === 0) return null

  const padding = { top: 16, right: 16, bottom: 40, left: 60 }
  const chartW = width - padding.left - padding.right
  const chartH = height - padding.top - padding.bottom
  const barGap = 8
  const barWidth = Math.min(60, (chartW - barGap * (data.length - 1)) / data.length)
  const totalBarsWidth = data.length * barWidth + (data.length - 1) * barGap
  const offsetX = (chartW - totalBarsWidth) / 2

  // Y-axis ticks (4 lines)
  const ticks = [0, 0.25, 0.5, 0.75, 1].map(f => ({
    value: maxValue * f,
    y: padding.top + chartH * (1 - f)
  }))

  function fmtVal(v) {
    if (v >= 1000000) return (v / 1000000).toFixed(1) + 'M'
    if (v >= 1000) return (v / 1000).toFixed(1) + 'K'
    return v.toFixed(0)
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
      <svg width={width} height={height} viewBox={`0 0 ${width} ${height}`}>
        {/* Grid lines + Y labels */}
        {ticks.map((t, i) => (
          <g key={i}>
            <line
              x1={padding.left}
              y1={t.y}
              x2={width - padding.right}
              y2={t.y}
              stroke="var(--border)"
              strokeDasharray={i === 0 ? 'none' : '4 2'}
              strokeWidth={0.5}
            />
            <text
              x={padding.left - 8}
              y={t.y + 4}
              textAnchor="end"
              fill="var(--text-muted)"
              fontSize={10}
            >
              {fmtVal(t.value)}
            </text>
          </g>
        ))}

        {/* Bars */}
        {data.map((d, i) => {
          const barH = (d.value / maxValue) * chartH
          const x = padding.left + offsetX + i * (barWidth + barGap)
          const y = padding.top + chartH - barH
          return (
            <g key={i}>
              <rect
                x={x}
                y={y}
                width={barWidth}
                height={barH}
                fill={d.color}
                rx={3}
              />
              {/* Value on top */}
              <text
                x={x + barWidth / 2}
                y={y - 4}
                textAnchor="middle"
                fill="var(--text-primary)"
                fontSize={10}
                fontWeight="600"
              >
                {fmtVal(d.value)}
              </text>
              {/* Label below */}
              <text
                x={x + barWidth / 2}
                y={padding.top + chartH + 16}
                textAnchor="middle"
                fill="var(--text-muted)"
                fontSize={10}
              >
                {d.label.length > 10 ? d.label.slice(0, 9) + '..' : d.label}
              </text>
            </g>
          )
        })}
      </svg>
    </div>
  )
}

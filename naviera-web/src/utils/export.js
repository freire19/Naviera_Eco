/**
 * Exports an array of objects as a CSV file download.
 *
 * @param {Object[]} data - Array of row objects
 * @param {{ key: string, label: string }[]} columns - Column definitions
 * @param {string} filename - Download filename (without .csv)
 */
export function exportCSV(data, columns, filename) {
  if (!data || data.length === 0) return

  const separator = ';'
  const header = columns.map(c => `"${c.label}"`).join(separator)
  const rows = data.map(row =>
    columns.map(c => {
      let val = row[c.key]
      if (val === null || val === undefined) val = ''
      val = String(val).replace(/"/g, '""')
      return `"${val}"`
    }).join(separator)
  )

  const bom = '\uFEFF' // UTF-8 BOM for Excel compatibility
  const csv = bom + [header, ...rows].join('\r\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)

  const link = document.createElement('a')
  link.href = url
  link.download = `${filename}.csv`
  link.style.display = 'none'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

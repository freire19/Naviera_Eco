import { money } from '../helpers.js'
import { IconTrash, IconAlert } from '../icons.jsx'

export default function ItemRow({ item, index, t, editable, onChange, onRemove }) {
  const subtotal = (item.quantidade || 0) * (item.preco_unitario || 0)

  const update = (field, value) => {
    if (!onChange) return
    const updated = { ...item, [field]: value }
    updated.subtotal = (updated.quantidade || 0) * (updated.preco_unitario || 0)
    onChange(index, updated)
  }

  return (
    <div style={{
      display: 'flex', flexDirection: 'column', gap: 6,
      padding: 12, background: t.soft, borderRadius: 10,
      border: `1px solid ${t.border}`
    }}>
      {/* Nome do item */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <span style={{ color: t.txMuted, fontSize: '0.8rem', fontWeight: 600, minWidth: 20 }}>
          {index + 1}.
        </span>
        {editable ? (
          <input
            className="input"
            value={item.nome_item || ''}
            onChange={(e) => update('nome_item', e.target.value)}
            placeholder="Nome do item"
            style={{ background: t.card, color: t.tx, borderColor: t.border, flex: 1 }}
          />
        ) : (
          <span style={{ color: t.tx, fontWeight: 500, flex: 1 }}>{item.nome_item}</span>
        )}
        {editable && onRemove && (
          <button onClick={() => onRemove(index)} style={{
            background: 'none', border: 'none', cursor: 'pointer', padding: 4
          }}>
            <IconTrash size={16} color={t.err} />
          </button>
        )}
      </div>

      {/* Quantidade x Preco */}
      <div style={{ display: 'flex', gap: 8, alignItems: 'center', paddingLeft: 28 }}>
        {editable ? (
          <>
            <input
              className="input"
              type="number"
              min="1"
              value={item.quantidade || ''}
              onChange={(e) => update('quantidade', parseInt(e.target.value) || 0)}
              style={{ width: 60, background: t.card, color: t.tx, borderColor: t.border, textAlign: 'center' }}
            />
            <span style={{ color: t.txMuted }}>x</span>
            <input
              className="input"
              type="number"
              step="0.01"
              min="0"
              value={item.preco_unitario || ''}
              onChange={(e) => update('preco_unitario', parseFloat(e.target.value) || 0)}
              style={{ width: 100, background: t.card, color: t.tx, borderColor: t.border }}
            />
          </>
        ) : (
          <>
            <span style={{ color: t.txSoft }}>{item.quantidade}x</span>
            <span style={{ color: t.txSoft }}>{money(item.preco_unitario)}</span>
          </>
        )}
        <span style={{ color: t.tx, fontWeight: 600, marginLeft: 'auto' }}>
          {money(subtotal)}
        </span>
      </div>

      {/* Alerta de preco diferente */}
      {item.preco_diferente && (
        <div style={{
          display: 'flex', alignItems: 'center', gap: 6, paddingLeft: 28,
          background: t.warnBg, borderRadius: 6, padding: '4px 8px 4px 28px'
        }}>
          <IconAlert size={14} color={t.warn} />
          <span style={{ fontSize: '0.75rem', color: t.warnTx }}>
            Preco padrao: {money(item.preco_padrao)}
          </span>
        </div>
      )}

      {/* Item novo (nao cadastrado) */}
      {item.item_novo && (
        <div style={{
          display: 'flex', alignItems: 'center', gap: 6,
          paddingLeft: 28, fontSize: '0.75rem', color: t.txMuted
        }}>
          Item nao cadastrado na tabela de precos
        </div>
      )}

      {/* Confianca */}
      {item.confianca != null && (
        <div style={{ paddingLeft: 28 }}>
          <span style={{
            fontSize: '0.7rem', fontWeight: 600, borderRadius: 10, padding: '1px 8px',
            background: item.confianca >= 80 ? t.okBg : item.confianca >= 50 ? t.warnBg : t.errBg,
            color: item.confianca >= 80 ? t.okTx : item.confianca >= 50 ? t.warnTx : t.errTx
          }}>
            {item.confianca}% confianca
          </span>
        </div>
      )}
    </div>
  )
}

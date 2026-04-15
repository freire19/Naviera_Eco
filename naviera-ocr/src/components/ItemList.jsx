import ItemRow from './ItemRow.jsx'
import { IconPlus } from '../icons.jsx'

export default function ItemList({ itens, t, onUpdate, onRemove, onAdd }) {
  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
        <h3 style={{ color: t.tx, fontSize: '0.95rem', fontWeight: 600 }}>
          Itens ({itens.length})
        </h3>
        <button onClick={onAdd} className="btn" style={{
          background: t.soft, color: t.pri, border: `1px solid ${t.border}`,
          padding: '6px 12px', fontSize: '0.8rem'
        }}>
          <IconPlus size={14} color={t.pri} /> Adicionar
        </button>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        {itens.map((item, idx) => (
          <ItemRow
            key={idx}
            item={item}
            index={idx}
            t={t}
            editable
            onChange={onUpdate}
            onRemove={onRemove}
          />
        ))}
      </div>

      {itens.length === 0 && (
        <div style={{
          textAlign: 'center', padding: 24, color: t.txMuted, fontSize: '0.9rem',
          background: t.soft, borderRadius: 10
        }}>
          Nenhum item extraido. Use "Revisar com IA" ou adicione manualmente.
        </div>
      )}
    </div>
  )
}

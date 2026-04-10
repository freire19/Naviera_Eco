export default function Skeleton({ t, height = 60, count = 3 }) {
  return <>{Array.from({ length: count }).map((_, i) => (
    <div key={i} className="skeleton" style={{ height, "--sk-base": t.skBase, "--sk-shine": t.skShine, marginBottom: 8, borderRadius: 14 }} />
  ))}</>;
}

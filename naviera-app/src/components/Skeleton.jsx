import { useTheme } from "../contexts/ThemeContext.jsx";

export default function Skeleton({ height = 60, count = 3 }) {
  const { t } = useTheme();
  return <>{Array.from({ length: count }).map((_, i) => (
    <div key={i} className="skeleton" style={{ height, "--sk-base": t.skBase, "--sk-shine": t.skShine, marginBottom: 8, borderRadius: 14 }} />
  ))}</>;
}

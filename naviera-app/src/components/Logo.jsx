/* ═══ LOGO ═══ */
import { useTheme } from "../contexts/ThemeContext.jsx";

export default function Logo({ size = 16 }) {
  const { t } = useTheme();
  return (
    <svg width={size} height={size} viewBox="0 0 60 60" fill="none">
      <path d="M14 48 L14 14 Q14 10, 18 14 L30 30 Q34 35, 34 30 L34 14" stroke={t.pri} strokeWidth="5" strokeLinecap="round" strokeLinejoin="round" fill="none"/>
      <path d="M34 30 Q34 35, 38 30 L48 16" stroke={t.pri} strokeWidth="3.5" strokeLinecap="round" fill="none" opacity="0.25"/>
    </svg>
  );
}

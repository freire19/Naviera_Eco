import { useState } from "react";

const LogoV3 = ({ size = 200, variant = "full", theme = "dark" }) => {
  const c = {
    dark:  { pri: "#34D399", sec: "#059669", tx: "#F0FDF4", txSub: "#6EE7B7", bg: "#061210" },
    light: { pri: "#059669", sec: "#047857", tx: "#052E22", txSub: "#059669", bg: "#F0FDF4" },
    mono:  { pri: "#D1D5DB", sec: "#9CA3AF", tx: "#E5E7EB", txSub: "#9CA3AF", bg: "#1F2937" },
  }[theme];

  if (variant === "icon") {
    return (
      <svg width={size} height={size} viewBox="0 0 120 120" fill="none">
        <rect x="6" y="6" width="108" height="108" rx="28" fill={c.bg} stroke={c.pri} strokeWidth="1.5"/>
        <path d="M32 86 L32 34 Q32 30, 36 34 L60 64 Q64 69, 64 64 L64 34" stroke={c.pri} strokeWidth="7" strokeLinecap="round" strokeLinejoin="round" fill="none"/>
        <path d="M64 64 Q64 69, 68 64 L88 38" stroke={c.pri} strokeWidth="5" strokeLinecap="round" strokeLinejoin="round" fill="none" opacity="0.35"/>
        <circle cx="40" cy="96" r="3" fill={c.pri} opacity="0.3"/>
        <circle cx="56" cy="96" r="3" fill={c.pri} opacity="0.55"/>
        <circle cx="72" cy="96" r="3" fill={c.pri}/>
      </svg>
    );
  }

  if (variant === "symbol") {
    return (
      <svg width={size} height={size} viewBox="0 0 200 200" fill="none">
        <path d="M48 160 L48 52 Q48 42, 56 52 L100 112 Q108 124, 108 112 L108 52" stroke={c.pri} strokeWidth="12" strokeLinecap="round" strokeLinejoin="round" fill="none"/>
        <path d="M108 112 Q108 124, 116 112 L152 60" stroke={c.pri} strokeWidth="8" strokeLinecap="round" strokeLinejoin="round" fill="none" opacity="0.25"/>
        <circle cx="62" cy="180" r="4.5" fill={c.pri} opacity="0.2"/>
        <circle cx="84" cy="180" r="4.5" fill={c.pri} opacity="0.45"/>
        <circle cx="106" cy="180" r="4.5" fill={c.pri} opacity="0.7"/>
        <circle cx="128" cy="180" r="4.5" fill={c.pri}/>
        <line x1="100" y1="18" x2="100" y2="30" stroke={c.pri} strokeWidth="2" strokeLinecap="round" opacity="0.3"/>
        <polygon points="100,14 96,22 104,22" fill={c.pri} opacity="0.3"/>
      </svg>
    );
  }

  if (variant === "stacked") {
    return (
      <svg width={size} height={size * 1.3} viewBox="0 0 300 390" fill="none">
        <path d="M108 210 L108 82 Q108 68, 118 82 L150 128 Q160 144, 160 128 L160 82" stroke={c.pri} strokeWidth="14" strokeLinecap="round" strokeLinejoin="round" fill="none"/>
        <path d="M160 128 Q160 144, 170 128 L198 84" stroke={c.pri} strokeWidth="9" strokeLinecap="round" strokeLinejoin="round" fill="none" opacity="0.25"/>
        <circle cx="118" cy="232" r="4" fill={c.pri} opacity="0.2"/>
        <circle cx="136" cy="232" r="4" fill={c.pri} opacity="0.45"/>
        <circle cx="154" cy="232" r="4" fill={c.pri} opacity="0.7"/>
        <circle cx="172" cy="232" r="4" fill={c.pri}/>
        <text x="150" y="290" textAnchor="middle" fontFamily="'Sora', sans-serif" fontSize="52" fontWeight="700" fill={c.tx} letterSpacing="10">NAVIERA</text>
        <text x="150" y="322" textAnchor="middle" fontFamily="'Sora', sans-serif" fontSize="12" fontWeight="400" fill={c.txSub} letterSpacing="8" opacity="0.7">NAVEGAÇÃO FLUVIAL</text>
      </svg>
    );
  }

  return (
    <svg width={size * 3} height={size} viewBox="0 0 600 200" fill="none">
      <path d="M40 160 L40 52 Q40 42, 48 52 L88 108 Q96 120, 96 108 L96 52" stroke={c.pri} strokeWidth="11" strokeLinecap="round" strokeLinejoin="round" fill="none"/>
      <path d="M96 108 Q96 120, 104 108 L136 62" stroke={c.pri} strokeWidth="7" strokeLinecap="round" strokeLinejoin="round" fill="none" opacity="0.25"/>
      <circle cx="54" cy="178" r="3.5" fill={c.pri} opacity="0.2"/>
      <circle cx="72" cy="178" r="3.5" fill={c.pri} opacity="0.45"/>
      <circle cx="90" cy="178" r="3.5" fill={c.pri} opacity="0.7"/>
      <circle cx="108" cy="178" r="3.5" fill={c.pri}/>
      <text x="175" y="118" fontFamily="'Sora', sans-serif" fontSize="64" fontWeight="700" fill={c.tx} letterSpacing="8">NAVIERA</text>
      <line x1="177" y1="134" x2="560" y2="134" stroke={c.pri} strokeWidth="0.8" opacity="0.2"/>
      <text x="177" y="156" fontFamily="'Sora', sans-serif" fontSize="13" fontWeight="400" fill={c.txSub} letterSpacing="10" opacity="0.6">NAVEGAÇÃO FLUVIAL</text>
    </svg>
  );
};

export default function App() {
  const [mode, setMode] = useState("light");

  const t = mode === "light" ? {
    bg: "#F7FBF9", bgCard: "#FFFFFF", bgSoft: "#EEF7F2", bgAccent: "#E6F5ED",
    tx: "#0F2620", txSoft: "#3D6B56", txMuted: "#7BA393",
    pri: "#059669", priLight: "#D1FAE5", priBorder: "#A7F3D0",
    border: "rgba(5,150,105,0.12)", borderStrong: "rgba(5,150,105,0.25)",
    info: "#0369A1", warning: "#B45309", danger: "#DC2626",
  } : {
    bg: "#040D0A", bgCard: "#0F2D24", bgSoft: "#0A1F18", bgAccent: "#0F2D24",
    tx: "#F0FDF4", txSoft: "#6EE7B7", txMuted: "#34D399",
    pri: "#34D399", priLight: "#052E22", priBorder: "#059669",
    border: "rgba(52,211,153,0.08)", borderStrong: "rgba(52,211,153,0.2)",
    info: "#0EA5E9", warning: "#F59E0B", danger: "#EF4444",
  };
  const th = mode === "light" ? "light" : "dark";

  return (
    <div style={{ minHeight: "100vh", background: t.bg, color: t.tx, fontFamily: "'Sora', sans-serif", transition: "all 0.3s" }}>
      <link href="https://fonts.googleapis.com/css2?family=Sora:wght@300;400;500;600;700;800&family=Space+Mono:wght@400;700&display=swap" rel="stylesheet"/>
      <style>{`@keyframes pulse{0%,100%{transform:scale(1);opacity:.2}50%{transform:scale(1.3);opacity:1}} @keyframes fadeIn{from{opacity:0;transform:translateY(10px)}to{opacity:1;transform:translateY(0)}} .fi{animation:fadeIn .4s ease both}`}</style>

      {/* Toggle */}
      <div style={{ position: "fixed", top: 16, right: 16, zIndex: 100 }}>
        <button onClick={() => setMode(m => m === "light" ? "dark" : "light")} style={{ padding: "10px 20px", borderRadius: 12, background: t.bgCard, border: `1px solid ${t.border}`, cursor: "pointer", color: t.tx, fontFamily: "'Sora', sans-serif", fontSize: 13, fontWeight: 600, display: "flex", alignItems: "center", gap: 8, boxShadow: mode === "light" ? "0 2px 12px rgba(0,0,0,0.06)" : "none", transition: "all 0.3s" }}>
          {mode === "light" ? "🌙" : "☀️"} {mode === "light" ? "Dark" : "Light"}
        </button>
      </div>

      {/* Hero */}
      <div style={{ padding: "56px 40px 40px", textAlign: "center", borderBottom: `1px solid ${t.border}` }}>
        <div style={{ fontSize: 10, letterSpacing: "0.4em", textTransform: "uppercase", color: t.pri, marginBottom: 24, fontWeight: 600 }}>Opção 03 — Modo {mode === "light" ? "Claro" : "Escuro"}</div>
        <LogoV3 size={110} variant="full" theme={th}/>
        <div style={{ marginTop: 20, fontSize: 14, color: t.txMuted, maxWidth: 400, margin: "20px auto 0", lineHeight: 1.8, fontWeight: 300 }}>
          Alternando entre os modos para validar a identidade em ambos os contextos.
        </div>
      </div>

      <div style={{ maxWidth: 860, margin: "0 auto", padding: "0 24px 60px" }}>

        {/* ═══ ALL VARIATIONS ═══ */}
        <section style={{ marginTop: 48 }}>
          <h2 style={{ fontSize: 20, fontWeight: 600, marginBottom: 24 }}>Todas as variações</h2>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 14 }}>
            {/* Horizontal */}
            <div style={{ background: t.bgCard, borderRadius: 16, padding: 36, border: `1px solid ${t.border}`, textAlign: "center", boxShadow: mode === "light" ? "0 1px 8px rgba(0,0,0,0.04)" : "none" }}>
              <LogoV3 size={85} variant="full" theme={th}/>
              <div style={{ marginTop: 14, fontSize: 11, color: t.txMuted, letterSpacing: "0.08em" }}>Horizontal</div>
            </div>
            {/* Stacked */}
            <div style={{ background: t.bgCard, borderRadius: 16, padding: 28, border: `1px solid ${t.border}`, textAlign: "center", display: "flex", flexDirection: "column", alignItems: "center", boxShadow: mode === "light" ? "0 1px 8px rgba(0,0,0,0.04)" : "none" }}>
              <LogoV3 size={90} variant="stacked" theme={th}/>
              <div style={{ marginTop: 8, fontSize: 11, color: t.txMuted, letterSpacing: "0.08em" }}>Empilhado</div>
            </div>
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 14, marginTop: 14 }}>
            {/* Symbol */}
            <div style={{ background: t.bgCard, borderRadius: 16, padding: 28, border: `1px solid ${t.border}`, textAlign: "center", display: "flex", flexDirection: "column", alignItems: "center", boxShadow: mode === "light" ? "0 1px 8px rgba(0,0,0,0.04)" : "none" }}>
              <LogoV3 size={110} variant="symbol" theme={th}/>
              <div style={{ marginTop: 8, fontSize: 11, color: t.txMuted }}>Símbolo</div>
            </div>
            {/* Icon */}
            <div style={{ background: t.bgCard, borderRadius: 16, padding: 28, border: `1px solid ${t.border}`, textAlign: "center", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", boxShadow: mode === "light" ? "0 1px 8px rgba(0,0,0,0.04)" : "none" }}>
              <LogoV3 size={80} variant="icon" theme={th}/>
              <div style={{ marginTop: 12, fontSize: 11, color: t.txMuted }}>Ícone do App</div>
            </div>
            {/* Inverted preview */}
            <div style={{ background: mode === "light" ? "#061210" : "#F0FDF4", borderRadius: 16, padding: 28, border: `1px solid ${t.border}`, textAlign: "center", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center" }}>
              <LogoV3 size={80} variant="icon" theme={mode === "light" ? "dark" : "light"}/>
              <div style={{ marginTop: 12, fontSize: 11, color: mode === "light" ? "#6EE7B7" : "#047857" }}>Ícone invertido</div>
            </div>
          </div>
        </section>

        {/* ═══ MOCK: APP SCREENS ═══ */}
        <section style={{ marginTop: 48 }}>
          <h2 style={{ fontSize: 20, fontWeight: 600, marginBottom: 24 }}>Aplicação no app</h2>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16 }}>
            {/* Login mock */}
            <div style={{ background: t.bgCard, borderRadius: 20, border: `1px solid ${t.border}`, overflow: "hidden", boxShadow: mode === "light" ? "0 4px 24px rgba(0,0,0,0.06)" : "none" }}>
              <div style={{ padding: "32px 24px", textAlign: "center" }}>
                <LogoV3 size={56} variant="stacked" theme={th}/>
                <div style={{ marginTop: 20 }}>
                  <input placeholder="CPF ou CNPJ" style={{ width: "100%", padding: "12px 16px", borderRadius: 12, border: `1px solid ${t.border}`, background: t.bgSoft, color: t.tx, fontSize: 13, outline: "none", boxSizing: "border-box", fontFamily: "inherit", marginBottom: 10 }}/>
                  <input type="password" placeholder="Senha" style={{ width: "100%", padding: "12px 16px", borderRadius: 12, border: `1px solid ${t.border}`, background: t.bgSoft, color: t.tx, fontSize: 13, outline: "none", boxSizing: "border-box", fontFamily: "inherit", marginBottom: 14 }}/>
                  <button style={{ width: "100%", padding: "13px", borderRadius: 12, background: mode === "light" ? "linear-gradient(135deg, #059669, #34D399)" : "linear-gradient(135deg, #059669, #34D399)", color: "#fff", fontWeight: 700, border: "none", fontSize: 14, fontFamily: "inherit" }}>Entrar</button>
                </div>
              </div>
              <div style={{ textAlign: "center", padding: "12px", borderTop: `1px solid ${t.border}`, fontSize: 11, color: t.txMuted }}>Tela de login</div>
            </div>

            {/* Header + card mock */}
            <div style={{ background: t.bgCard, borderRadius: 20, border: `1px solid ${t.border}`, overflow: "hidden", boxShadow: mode === "light" ? "0 4px 24px rgba(0,0,0,0.06)" : "none" }}>
              {/* Header */}
              <div style={{ padding: "14px 20px", display: "flex", justifyContent: "space-between", alignItems: "center", borderBottom: `1px solid ${t.border}` }}>
                <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                  <div style={{ width: 30, height: 30, borderRadius: 8, background: mode === "light" ? "#E6F5ED" : "#0F2D24", display: "flex", alignItems: "center", justifyContent: "center" }}>
                    <svg width={16} height={16} viewBox="0 0 60 60" fill="none">
                      <path d="M14 48 L14 14 Q14 10, 18 14 L30 30 Q34 35, 34 30 L34 14" stroke={t.pri} strokeWidth="5" strokeLinecap="round" strokeLinejoin="round" fill="none"/>
                      <path d="M34 30 Q34 35, 38 30 L48 16" stroke={t.pri} strokeWidth="3.5" strokeLinecap="round" fill="none" opacity="0.25"/>
                    </svg>
                  </div>
                  <span style={{ fontSize: 15, fontWeight: 700 }}>Naviera</span>
                </div>
                <div style={{ width: 30, height: 30, borderRadius: 8, background: t.bgSoft, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 13 }}>👤</div>
              </div>

              {/* Content */}
              <div style={{ padding: "16px 20px" }}>
                <div style={{ fontSize: 13, color: t.txMuted }}>Olá,</div>
                <div style={{ fontSize: 18, fontWeight: 700, marginBottom: 16 }}>Maria Souza 👋</div>

                {/* Encomenda card */}
                <div style={{ background: t.bgSoft, borderRadius: 14, padding: 14, border: `1px solid ${t.border}`, marginBottom: 10 }}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 6 }}>
                    <span style={{ fontSize: 12, fontWeight: 600, fontFamily: "'Space Mono', monospace", color: t.txSoft }}>ENC-2026-0451</span>
                    <span style={{ fontSize: 10, padding: "2px 10px", borderRadius: 20, background: mode === "light" ? "#DBEAFE" : "#0c2d48", color: mode === "light" ? "#1D4ED8" : "#38BDF8", fontWeight: 600 }}>🚢 Em Trânsito</span>
                  </div>
                  <div style={{ fontSize: 13, fontWeight: 600 }}>F/B Deus de Aliança</div>
                  <div style={{ fontSize: 11, color: t.txMuted }}>Manaus → Jutaí • Chegada: 10/04</div>
                </div>

                {/* Another card */}
                <div style={{ background: t.bgSoft, borderRadius: 14, padding: 14, border: `1px solid ${t.border}` }}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 6 }}>
                    <span style={{ fontSize: 12, fontWeight: 600, fontFamily: "'Space Mono', monospace", color: t.txSoft }}>ENC-2026-0310</span>
                    <span style={{ fontSize: 10, padding: "2px 10px", borderRadius: 20, background: mode === "light" ? "#D1FAE5" : "#0f2b1c", color: mode === "light" ? "#047857" : "#4ade80", fontWeight: 600 }}>✅ Entregue</span>
                  </div>
                  <div style={{ fontSize: 13, fontWeight: 600 }}>F/B José Lemos</div>
                  <div style={{ fontSize: 11, color: t.txMuted }}>Manaus → Jutaí • Entregue 09/04</div>
                </div>
              </div>

              {/* Nav */}
              <div style={{ display: "flex", justifyContent: "space-around", padding: "10px 0", borderTop: `1px solid ${t.border}`, marginTop: 8 }}>
                {[["🏠", "Início", true], ["📦", "Cargas", false], ["💳", "Financeiro", false], ["⛴", "Barcos", false], ["👤", "Perfil", false]].map(([ic, lb, act], i) => (
                  <div key={i} style={{ textAlign: "center", color: act ? t.pri : t.txMuted, fontSize: 9, fontWeight: 600 }}>
                    <div style={{ fontSize: 16 }}>{ic}</div>
                    {lb}
                    {act && <div style={{ width: 4, height: 4, borderRadius: 2, background: t.pri, margin: "2px auto 0" }}/>}
                  </div>
                ))}
              </div>

              <div style={{ textAlign: "center", padding: "8px", borderTop: `1px solid ${t.border}`, fontSize: 11, color: t.txMuted }}>Tela principal</div>
            </div>
          </div>
        </section>

        {/* ═══ UI ELEMENTS ═══ */}
        <section style={{ marginTop: 48 }}>
          <h2 style={{ fontSize: 20, fontWeight: 600, marginBottom: 24 }}>Elementos de UI</h2>

          <div style={{ background: t.bgCard, borderRadius: 16, padding: 32, border: `1px solid ${t.border}`, boxShadow: mode === "light" ? "0 2px 12px rgba(0,0,0,0.04)" : "none" }}>
            {/* Buttons */}
            <div style={{ fontSize: 11, color: t.txMuted, letterSpacing: "0.1em", textTransform: "uppercase", marginBottom: 14, fontWeight: 600 }}>Botões</div>
            <div style={{ display: "flex", gap: 12, flexWrap: "wrap", marginBottom: 28 }}>
              <button style={{ padding: "12px 28px", borderRadius: 12, background: `linear-gradient(135deg, #059669, #34D399)`, color: "#fff", fontWeight: 700, border: "none", fontSize: 13, fontFamily: "inherit" }}>Primário</button>
              <button style={{ padding: "12px 28px", borderRadius: 12, background: "transparent", color: t.pri, fontWeight: 700, border: `2px solid ${t.pri}`, fontSize: 13, fontFamily: "inherit" }}>Secundário</button>
              <button style={{ padding: "12px 28px", borderRadius: 12, background: t.bgSoft, color: t.txSoft, fontWeight: 600, border: `1px solid ${t.border}`, fontSize: 13, fontFamily: "inherit" }}>Terciário</button>
              <button style={{ padding: "12px 28px", borderRadius: 12, background: mode === "light" ? "#FEE2E2" : "#450a0a", color: t.danger, fontWeight: 700, border: "none", fontSize: 13, fontFamily: "inherit" }}>Perigo</button>
            </div>

            {/* Badges */}
            <div style={{ fontSize: 11, color: t.txMuted, letterSpacing: "0.1em", textTransform: "uppercase", marginBottom: 14, fontWeight: 600 }}>Status badges</div>
            <div style={{ display: "flex", gap: 10, flexWrap: "wrap", marginBottom: 28 }}>
              {[
                ["🚢 Em Trânsito", mode === "light" ? "#DBEAFE" : "#0c2d48", mode === "light" ? "#1D4ED8" : "#38BDF8"],
                ["⏳ Aguardando", mode === "light" ? "#FEF3C7" : "#3a3520", mode === "light" ? "#B45309" : "#FBBF24"],
                ["✅ Entregue", mode === "light" ? "#D1FAE5" : "#0f2b1c", mode === "light" ? "#047857" : "#4ADE80"],
                ["⚠️ Vencido", mode === "light" ? "#FEE2E2" : "#450a0a", mode === "light" ? "#DC2626" : "#F87171"],
                ["⚡ No Porto", mode === "light" ? "#D1FAE5" : "#0f2b1c", mode === "light" ? "#047857" : "#4ADE80"],
              ].map(([label, bg, color], i) => (
                <span key={i} style={{ fontSize: 11, padding: "5px 14px", borderRadius: 20, background: bg, color, fontWeight: 600 }}>{label}</span>
              ))}
            </div>

            {/* Input */}
            <div style={{ fontSize: 11, color: t.txMuted, letterSpacing: "0.1em", textTransform: "uppercase", marginBottom: 14, fontWeight: 600 }}>Inputs</div>
            <div style={{ display: "flex", gap: 12, marginBottom: 28 }}>
              <input placeholder="Buscar embarcação..." style={{ flex: 1, padding: "12px 16px", borderRadius: 12, border: `1px solid ${t.border}`, background: t.bgSoft, color: t.tx, fontSize: 13, outline: "none", fontFamily: "inherit" }}/>
              <button style={{ padding: "12px 20px", borderRadius: 12, background: `linear-gradient(135deg, #059669, #34D399)`, color: "#fff", fontWeight: 700, border: "none", fontSize: 13, fontFamily: "inherit" }}>🔍</button>
            </div>

            {/* Loading dots */}
            <div style={{ fontSize: 11, color: t.txMuted, letterSpacing: "0.1em", textTransform: "uppercase", marginBottom: 14, fontWeight: 600 }}>Loading</div>
            <div style={{ display: "flex", gap: 10, alignItems: "center" }}>
              {[0, 1, 2, 3].map(i => (
                <div key={i} style={{ width: 10, height: 10, borderRadius: 5, background: t.pri, animation: `pulse 1.2s ${i * 0.2}s ease-in-out infinite alternate` }}/>
              ))}
              <span style={{ marginLeft: 12, fontSize: 13, color: t.txMuted }}>Carregando...</span>
            </div>
            <style>{`@keyframes pulse{from{transform:scale(1);opacity:.15}to{transform:scale(1.4);opacity:1}}`}</style>
          </div>
        </section>

        {/* ═══ COLORS IN CONTEXT ═══ */}
        <section style={{ marginTop: 48 }}>
          <h2 style={{ fontSize: 20, fontWeight: 600, marginBottom: 24 }}>Paleta em contexto</h2>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: 10 }}>
            {[
              { hex: mode === "light" ? "#059669" : "#34D399", name: "Primária", on: mode === "light" ? "#F7FBF9" : "#040D0A" },
              { hex: mode === "light" ? "#F7FBF9" : "#040D0A", name: "Background", on: null },
              { hex: mode === "light" ? "#FFFFFF" : "#0F2D24", name: "Superfície", on: null },
              { hex: mode === "light" ? "#0F2620" : "#F0FDF4", name: "Texto", on: mode === "light" ? "#FFFFFF" : "#0F2D24" },
              { hex: mode === "light" ? "#3D6B56" : "#6EE7B7", name: "Texto suave", on: mode === "light" ? "#FFFFFF" : "#0F2D24" },
              { hex: mode === "light" ? "#EEF7F2" : "#0A1F18", name: "Card fundo", on: null },
              { hex: "#0EA5E9", name: "Info", on: null },
              { hex: "#F59E0B", name: "Alerta", on: null },
              { hex: "#EF4444", name: "Erro", on: null },
            ].map((co, i) => (
              <div key={i} style={{ borderRadius: 12, overflow: "hidden", border: `1px solid ${t.border}`, background: t.bgCard }}>
                <div style={{ height: 48, background: co.hex, border: co.hex === t.bg ? `1px inset ${t.border}` : "none" }}/>
                <div style={{ padding: "8px 12px", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                  <span style={{ fontSize: 12, fontWeight: 600 }}>{co.name}</span>
                  <span style={{ fontSize: 10, fontFamily: "'Space Mono', monospace", color: t.txMuted }}>{co.hex}</span>
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* Footer */}
        <div style={{ marginTop: 56, textAlign: "center", paddingTop: 28, borderTop: `1px solid ${t.border}` }}>
          <LogoV3 size={45} variant="full" theme={th}/>
          <div style={{ marginTop: 12, fontSize: 10, fontFamily: "'Space Mono', monospace", color: t.txMuted }}>Naviera v3.0 — {mode === "light" ? "Light" : "Dark"} Mode</div>
        </div>
      </div>
    </div>
  );
}

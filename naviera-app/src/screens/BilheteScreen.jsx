import { useState, useEffect, useCallback } from "react";
import { T } from "../theme.js";
import { fmt, money } from "../helpers.js";
import { IconBack, IconShip, IconSun } from "../icons.jsx";
import Logo from "../components/Logo.jsx";
import { API, authFetch } from "../api.js";
import { useAuth } from "../contexts/AuthContext.jsx";

// Bilhete sempre em dark — visual premium independente do tema do app.
const t = T.dark;

export default function BilheteScreen({ bilhete, onBack }) {
  const { authHeaders } = useAuth();
  const [now, setNow] = useState(Date.now());
  const [brightness, setBrightness] = useState(false);
  const [totp, setTotp] = useState("------");
  const [timeLeft, setTimeLeft] = useState(30);

  // DS4-007 fix: buscar TOTP do servidor (HMAC-SHA256), nunca gerar client-side
  // #600: id pode vir como `id`, `id_passagem` ou `idPassagem` (DTOs variam entre /passagens/minhas e /passagens/comprar)
  const bilheteId = bilhete?.id || bilhete?.id_passagem || bilhete?.idPassagem;
  const fetchTotp = useCallback(async () => {
    if (!bilheteId || !authHeaders?.Authorization) return;
    try {
      const res = await authFetch(`${API}/bilhetes/${bilheteId}/totp`, { headers: authHeaders });
      if (res.ok) {
        const data = await res.json();
        setTotp(data.code || "------");
        setTimeLeft(data.timeLeft || 30);
      }
    } catch { /* silencioso — mostra ultimo codigo valido */ }
  }, [bilheteId, authHeaders]);

  useEffect(() => { fetchTotp(); }, [fetchTotp]);
  useEffect(() => {
    const iv = setInterval(() => {
      setNow(Date.now());
      setTimeLeft(prev => {
        if (prev <= 1) { fetchTotp(); return 30; }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(iv);
  }, [fetchTotp]);
  const pct = (timeLeft / 30) * 100;

  // #037: guard defensivo apos hooks — se bilhete ausente, render nada em vez de TypeError em b.field
  if (!bilhete) return null;

  const b = bilhete;
  const nome = b.nome_passageiro || b.nomePassageiro || "Passageiro";
  const origem = b.origem || "\u2014";
  const destino = b.destino || "\u2014";
  const embarc = b.embarcacao || "\u2014";
  const data = b.data_viagem || b.dataViagem || "\u2014";
  const horario = b.horario_saida || b.horarioSaida || "12:00";
  const valor = b.valor_total || b.valorTotal || b.valor_a_pagar || "\u2014";
  const numBilhete = b.numero_bilhete || b.numeroBilhete || "00000";
  const qr = b.qr_hash || b.qrHash || numBilhete;
  const idViagem = b.id_viagem || b.idViagem || "?";
  const acomodacao = b.acomodacao || b.tipo || "Rede";
  const doc = b.documento || b.numero_documento || "";
  const docMask = doc ? doc.replace(/(\d{3})(\d+)(\d{2})/, "$1.\u2022\u2022\u2022.$3") : "";

  return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 12, background: t.bg, margin: "-16px -18px", padding: "16px 18px 100px", minHeight: "100vh" }}>
    <button onClick={onBack} style={{ background: "none", border: "none", color: t.txMuted, fontSize: 13, cursor: "pointer", textAlign: "left", padding: 0, display: "flex", alignItems: "center", gap: 4 }}><IconBack size={14} color={t.txMuted} /> Voltar</button>
    <div style={{ textAlign: "center", marginBottom: 4 }}>
      <div style={{ fontSize: 10, letterSpacing: 4, textTransform: "uppercase", color: t.txMuted, fontWeight: 300 }}>Bilhete digital</div>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: 6, marginTop: 4 }}>
        <div style={{ width: 6, height: 6, borderRadius: 3, background: t.pri, animation: "pulse 2s infinite" }} />
        <span style={{ fontSize: 11, color: t.txMuted }}>V\u00e1lido \u2014 c\u00f3digo rotativo ativo</span>
      </div>
    </div>

    {/* TICKET CARD */}
    <div style={{ position: "relative", borderRadius: 20, overflow: "hidden", filter: brightness ? "brightness(1.3) contrast(1.1)" : "none" }}>
      <div style={{ position: "relative", background: `linear-gradient(170deg, ${t.card} 0%, ${t.soft} 40%, ${t.card} 100%)`, padding: "22px 20px 18px", borderRadius: 20, border: `1px solid ${t.borderStrong}` }}>
        {/* Watermark */}
        <div style={{ position: "absolute", fontSize: 100, fontWeight: 800, color: t.pri, opacity: 0.03, letterSpacing: 8, transform: "rotate(-25deg)", top: "25%", left: "-5%", pointerEvents: "none" }}>NAVIERA</div>

        {/* Header */}
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 14 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <Logo size={26} />
            <div><div style={{ fontSize: 13, fontWeight: 700, letterSpacing: 3 }}>NAVIERA</div><div style={{ fontSize: 7, color: t.txMuted, letterSpacing: 2, textTransform: "uppercase" }}>Passagem fluvial</div></div>
          </div>
          <div style={{ textAlign: "right" }}><div style={{ fontSize: 9, fontFamily: "'Space Mono', monospace", color: t.txMuted }}>BLT-{numBilhete}</div><div style={{ fontSize: 8, color: t.txMuted }}>Viagem #{idViagem}</div></div>
        </div>

        {/* Boat SVG */}
        <svg width="100%" height="50" viewBox="0 0 300 50" fill="none">
          <path d="M0 38 Q25 32,50 38 Q75 44,100 38 Q125 32,150 38 Q175 44,200 38 Q225 32,250 38 Q275 44,300 38" stroke={t.pri} strokeWidth="0.8" opacity="0.25" fill="none"><animate attributeName="d" dur="3s" repeatCount="indefinite" values="M0 38 Q25 32,50 38 Q75 44,100 38 Q125 32,150 38 Q175 44,200 38 Q225 32,250 38 Q275 44,300 38;M0 38 Q25 44,50 38 Q75 32,100 38 Q125 44,150 38 Q175 32,200 38 Q225 44,250 38 Q275 32,300 38;M0 38 Q25 32,50 38 Q75 44,100 38 Q125 32,150 38 Q175 44,200 38 Q225 32,250 38 Q275 44,300 38"/></path>
          <path d="M100 35 L108 24 L192 24 L200 35Z" fill={t.pri} opacity="0.6"/><rect x="120" y="18" width="60" height="8" rx="2" fill={t.pri} opacity="0.4"/><rect x="138" y="10" width="24" height="10" rx="2" fill={t.txMuted} opacity="0.3"/>
        </svg>

        {/* Route */}
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", margin: "8px 0 14px", padding: "0 6px" }}>
          <div style={{ textAlign: "center" }}><div style={{ fontSize: 20, fontWeight: 800 }}>{origem.slice(0, 3).toUpperCase()}</div><div style={{ fontSize: 10, color: t.txMuted, marginTop: 2 }}>{origem}</div></div>
          <div style={{ flex: 1, display: "flex", alignItems: "center", justifyContent: "center", padding: "0 10px" }}>
            <div style={{ flex: 1, height: 1, background: t.border }} /><IconShip size={20} color={t.pri} /><div style={{ flex: 1, height: 1, background: t.border }} />
          </div>
          <div style={{ textAlign: "center" }}><div style={{ fontSize: 20, fontWeight: 800 }}>{destino.slice(0, 3).toUpperCase()}</div><div style={{ fontSize: 10, color: t.txMuted, marginTop: 2 }}>{destino}</div></div>
        </div>

        {/* Info grid */}
        <div style={{ background: `${t.pri}0a`, borderRadius: 12, padding: "12px 14px", marginBottom: 12, border: `1px solid ${t.border}` }}>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "10px 16px" }}>
            {[["Passageiro", nome], ["Documento", docMask || "\u2014"], ["Data", typeof data === "string" && data.includes("-") ? fmt(data) : data], ["Hor\u00e1rio", horario], ["Acomoda\u00e7\u00e3o", acomodacao], ["Embarca\u00e7\u00e3o", embarc]].map(([l, v], i) => (
              <div key={i}><div style={{ fontSize: 8, color: t.txMuted, textTransform: "uppercase", letterSpacing: 1, marginBottom: 2 }}>{l}</div><div style={{ fontSize: 12, fontWeight: 600 }}>{typeof v === "number" ? money(v) : v}</div></div>
            ))}
          </div>
        </div>

        {/* Tear line */}
        <div style={{ display: "flex", alignItems: "center", margin: "0 -20px 12px" }}>
          <div style={{ width: 14, height: 14, borderRadius: "50%", background: t.bg, flexShrink: 0, marginLeft: -7 }} />
          <div style={{ flex: 1, borderTop: `2px dashed ${t.border}`, margin: "0 4px" }} />
          <div style={{ width: 14, height: 14, borderRadius: "50%", background: t.bg, flexShrink: 0, marginRight: -7 }} />
        </div>

        {/* QR + TOTP */}
        <div style={{ display: "flex", alignItems: "center", gap: 14 }}>
          <div style={{ background: `${t.pri}10`, borderRadius: 12, padding: 8, border: `1px solid ${t.border}` }}>
            <svg width="90" height="90" viewBox="0 0 21 21">{(() => {
              let h = 0; const s = qr + totp;
              for (let i = 0; i < s.length; i++) h = ((h << 5) - h + s.charCodeAt(i)) | 0;
              const cells = [];
              for (let r = 0; r < 21; r++) for (let c = 0; c < 21; c++) {
                const finder = (r < 7 && c < 7) || (r < 7 && c > 13) || (r > 13 && c < 7);
                const border = finder && (r === 0 || r === 6 || c === 0 || c === 6 || (r > 13 && (r === 14 || r === 20)) || (c > 13 && (c === 14 || c === 20)));
                const inner = finder && r >= 2 && r <= 4 && c >= 2 && c <= 4;
                const innerBR = finder && r >= 16 && r <= 18 && c >= 2 && c <= 4;
                const innerTR = finder && r >= 2 && r <= 4 && c >= 16 && c <= 18;
                if (border || inner || innerBR || innerTR) { cells.push(<rect key={`${r}-${c}`} x={c} y={r} width="1" height="1" fill={t.pri} />); }
                else if (!finder) { h = ((h * 1103515245 + 12345) & 0x7fffffff); if (h % 3 !== 0) cells.push(<rect key={`${r}-${c}`} x={c} y={r} width="1" height="1" fill={t.txSoft} opacity="0.7" />); }
              }
              return cells;
            })()}</svg>
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 8, color: t.txMuted, textTransform: "uppercase", letterSpacing: 1.5, marginBottom: 6 }}>C\u00f3digo de seguran\u00e7a</div>
            <div style={{ fontSize: 26, fontWeight: 800, fontFamily: "'Space Mono', monospace", letterSpacing: 5, color: t.pri, marginBottom: 8 }}>
              {totp.slice(0, 3)}<span style={{ opacity: 0.3 }}>\u00b7</span>{totp.slice(3)}
            </div>
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <svg width="20" height="20" viewBox="0 0 20 20">
                <circle cx="10" cy="10" r="8" fill="none" stroke={t.border} strokeWidth="2" />
                <circle cx="10" cy="10" r="8" fill="none" stroke={timeLeft <= 5 ? t.err : t.pri} strokeWidth="2"
                  strokeDasharray={`${2 * Math.PI * 8}`} strokeDashoffset={`${2 * Math.PI * 8 * (1 - pct / 100)}`}
                  transform="rotate(-90 10 10)" style={{ transition: "stroke-dashoffset 1s linear" }} />
                <text x="10" y="10" textAnchor="middle" dominantBaseline="central" fill={timeLeft <= 5 ? t.err : t.pri} fontSize="7" fontWeight="700">{timeLeft}</text>
              </svg>
              <div><div style={{ fontSize: 10, color: timeLeft <= 5 ? t.err : t.txMuted }}>{timeLeft <= 5 ? "Renovando..." : `Renova em ${timeLeft}s`}</div>
                <div style={{ fontSize: 8, color: t.txMuted, opacity: 0.5, marginTop: 1 }}>Codigo de verificacao</div></div>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: 12, paddingTop: 10, borderTop: `1px solid ${t.border}` }}>
          <div style={{ fontSize: 8, color: t.txMuted, opacity: 0.5, fontFamily: "'Space Mono', monospace" }}>SIG: {qr.slice(0, 8)}...</div>
          <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
            <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke={t.pri} strokeWidth="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
            <span style={{ fontSize: 8, color: t.txMuted }}>Verificado</span>
          </div>
          <div style={{ fontSize: 14, fontWeight: 800, color: t.pri }}>{typeof valor === "number" ? money(valor) : valor}</div>
        </div>
      </div>
    </div>

    {/* Actions */}
    <div style={{ display: "flex", gap: 10 }}>
      <button onClick={() => setBrightness(!brightness)} style={{ flex: 1, padding: "12px 0", borderRadius: 12, border: `1px solid ${t.borderStrong}`, background: brightness ? `${t.pri}15` : "transparent", color: t.txMuted, fontSize: 12, fontWeight: 600, cursor: "pointer", fontFamily: "inherit", display: "flex", alignItems: "center", justifyContent: "center", gap: 6 }}>
        <IconSun size={14} color={t.txMuted} /> {brightness ? "Normal" : "Brilho maximo"}
      </button>
      <button onClick={() => navigator.share?.({ title: `Bilhete ${numBilhete}`, text: `Passagem ${origem}\u2192${destino} - ${embarc} - ${data}` }).catch(() => {})} style={{ flex: 1, padding: "12px 0", borderRadius: 12, border: "none", background: t.priGrad, color: "#fff", fontSize: 12, fontWeight: 700, cursor: "pointer", fontFamily: "inherit" }}>Compartilhar</button>
    </div>
    <div style={{ fontSize: 10, color: t.txMuted, opacity: 0.5, textAlign: "center", lineHeight: 1.6 }}>C\u00f3digo rotativo anti-clone. Muda a cada 30s. Screenshots n\u00e3o funcionam. Apresente esta tela ao operador.</div>
  </div>;
}

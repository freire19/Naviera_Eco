import { useState, useEffect, useCallback, useRef } from "react";
import { API } from "../api.js";
import { IconRefresh, IconShip, IconMapPin, IconClock } from "../icons.jsx";
import Badge from "../components/Badge.jsx";
import Cd from "../components/Card.jsx";
import Skeleton from "../components/Skeleton.jsx";
import ErrorRetry from "../components/ErrorRetry.jsx";

/* ═══ CONFIG ═══ */
const REFRESH_MS = 30_000;
const BOUNDS = { latMin: -5, latMax: 0, lonMin: -70, lonMax: -50 };

/* ═══ HELPERS ═══ */
function latLonToPercent(lat, lon) {
  const x = ((lon - BOUNDS.lonMin) / (BOUNDS.lonMax - BOUNDS.lonMin)) * 100;
  const y = ((BOUNDS.latMax - lat) / (BOUNDS.latMax - BOUNDS.latMin)) * 100;
  return { x: Math.max(0, Math.min(100, x)), y: Math.max(0, Math.min(100, y)) };
}

function tempoRelativo(iso) {
  if (!iso) return "sem dados";
  const diff = Date.now() - new Date(iso).getTime();
  const min = Math.floor(diff / 60000);
  if (min < 1) return "agora";
  if (min < 60) return `ha ${min} min`;
  const hrs = Math.floor(min / 60);
  if (hrs < 24) return `ha ${hrs}h`;
  const dias = Math.floor(hrs / 24);
  return `ha ${dias}d`;
}

function statusCor(iso) {
  if (!iso) return "gray";
  const min = (Date.now() - new Date(iso).getTime()) / 60000;
  if (min < 5) return "#22C55E";
  if (min < 30) return "#F59E0B";
  return "#9CA3AF";
}

/* ═══ HOOK: fetch GPS positions ═══ */
function useGps(authHeaders) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState("");
  const timerRef = useRef(null);
  const abortRef = useRef(null);

  const fetchGps = useCallback(async (silent = false) => {
    // Cancel previous in-flight fetch
    if (abortRef.current) abortRef.current.abort();
    const controller = new AbortController();
    abortRef.current = controller;

    if (!silent) setLoading(true);
    setErro("");
    try {
      const headers = authHeaders?.Authorization ? { ...authHeaders } : {};
      const r = await fetch(`${API}/gps/embarcacoes`, { headers, signal: controller.signal });
      if (!r.ok) throw new Error("Erro ao carregar posicoes GPS");
      const d = await r.json();
      setData(d);
    } catch (e) {
      if (e.name === 'AbortError') return;
      setErro(e.message || "Erro ao carregar dados GPS.");
    } finally {
      setLoading(false);
    }
  }, [authHeaders?.Authorization]);

  useEffect(() => {
    fetchGps();
    timerRef.current = setInterval(() => fetchGps(true), REFRESH_MS);
    return () => {
      clearInterval(timerRef.current);
      if (abortRef.current) abortRef.current.abort();
    };
  }, [fetchGps]);

  const refresh = useCallback(() => fetchGps(false), [fetchGps]);
  return { data, loading, erro, refresh };
}

/* ═══ SVG RIVER MAP ═══ */
function RiverMap({ boats, t, onSelectBoat }) {
  return (
    <div style={{
      position: "relative", width: "100%", paddingBottom: "56%",
      borderRadius: 14, overflow: "hidden",
      border: `1px solid ${t.border}`,
      background: "linear-gradient(160deg, #040D0A 0%, #0F2D24 30%, #040D0A 70%, #0A1F18 100%)"
    }}>
      <svg width="100%" height="100%" viewBox="0 0 1000 560" preserveAspectRatio="none"
        style={{ position: "absolute", top: 0, left: 0 }}>
        {/* Amazon river main channel */}
        <path d="M0 320 Q80 290, 180 310 Q280 340, 380 300 Q450 270, 520 280 Q600 295, 680 270 Q760 245, 840 260 Q920 280, 1000 250"
          fill="none" stroke="rgba(5,150,105,0.3)" strokeWidth="50" />
        <path d="M0 320 Q80 290, 180 310 Q280 340, 380 300 Q450 270, 520 280 Q600 295, 680 270 Q760 245, 840 260 Q920 280, 1000 250"
          fill="none" stroke="rgba(52,211,153,0.4)" strokeWidth="18" />
        <path d="M0 322 Q80 292, 180 312 Q280 342, 380 302 Q450 272, 520 282 Q600 297, 680 272 Q760 247, 840 262 Q920 282, 1000 252"
          fill="none" stroke="rgba(255,255,255,0.12)" strokeWidth="1.5" strokeDasharray="6 5" />
        {/* Solimoes tributary */}
        <path d="M0 280 Q100 260, 200 275 Q300 295, 380 300"
          fill="none" stroke="rgba(5,150,105,0.15)" strokeWidth="25" />
        {/* Negro tributary */}
        <path d="M420 100 Q440 180, 450 240 Q460 280, 520 280"
          fill="none" stroke="rgba(5,150,105,0.15)" strokeWidth="20" />
        {/* Reference cities */}
        <circle cx={latLonToPercent(-3.119, -60.021).x * 10} cy={latLonToPercent(-3.119, -60.021).y * 5.6} r="5" fill="rgba(255,255,255,0.5)" />
        <text x={latLonToPercent(-3.119, -60.021).x * 10} y={latLonToPercent(-3.119, -60.021).y * 5.6 - 10}
          textAnchor="middle" fill="rgba(255,255,255,0.6)" fontSize="12" fontWeight="500">Manaus</text>
        <circle cx={latLonToPercent(-3.367, -64.72).x * 10} cy={latLonToPercent(-3.367, -64.72).y * 5.6} r="4" fill="rgba(255,255,255,0.35)" />
        <text x={latLonToPercent(-3.367, -64.72).x * 10} y={latLonToPercent(-3.367, -64.72).y * 5.6 - 9}
          textAnchor="middle" fill="rgba(255,255,255,0.45)" fontSize="10">Tefe</text>
        <circle cx={latLonToPercent(-2.516, -66.063).x * 10} cy={latLonToPercent(-2.516, -66.063).y * 5.6} r="4" fill="rgba(255,255,255,0.35)" />
        <text x={latLonToPercent(-2.516, -66.063).x * 10} y={latLonToPercent(-2.516, -66.063).y * 5.6 - 9}
          textAnchor="middle" fill="rgba(255,255,255,0.45)" fontSize="10">Fonte Boa</text>
        <circle cx={latLonToPercent(-2.631, -56.737).x * 10} cy={latLonToPercent(-2.631, -56.737).y * 5.6} r="4" fill="rgba(255,255,255,0.35)" />
        <text x={latLonToPercent(-2.631, -56.737).x * 10} y={latLonToPercent(-2.631, -56.737).y * 5.6 - 9}
          textAnchor="middle" fill="rgba(255,255,255,0.45)" fontSize="10">Parintins</text>
        {/* Grid lines */}
        {[-65, -60, -55].map(lon => {
          const x = ((lon - BOUNDS.lonMin) / (BOUNDS.lonMax - BOUNDS.lonMin)) * 1000;
          return <line key={lon} x1={x} y1="0" x2={x} y2="560" stroke="rgba(255,255,255,0.04)" strokeWidth="1" />;
        })}
        {[-4, -3, -2, -1].map(lat => {
          const y = ((BOUNDS.latMax - lat) / (BOUNDS.latMax - BOUNDS.latMin)) * 560;
          return <line key={lat} x1="0" y1={y} x2="1000" y2={y} stroke="rgba(255,255,255,0.04)" strokeWidth="1" />;
        })}
      </svg>

      {/* Boat markers as absolutely positioned divs */}
      {boats?.map((b) => {
        const { x, y } = latLonToPercent(b.latitude, b.longitude);
        const cor = statusCor(b.ultima_atualizacao);
        return (
          <div key={b.id_embarcacao}
            onClick={() => onSelectBoat?.(b)}
            style={{
              position: "absolute", left: `${x}%`, top: `${y}%`,
              transform: "translate(-50%, -50%)", cursor: "pointer",
              display: "flex", flexDirection: "column", alignItems: "center", gap: 2,
              zIndex: 10
            }}>
            <div style={{
              width: 14, height: 14, borderRadius: "50%",
              background: cor, border: "2px solid white",
              boxShadow: `0 0 8px ${cor}`,
              animation: cor === "#22C55E" ? "pulse-gps 2s infinite" : undefined
            }} />
            <div style={{
              fontSize: 9, color: "white", fontWeight: 600,
              background: "rgba(0,0,0,0.65)", borderRadius: 4,
              padding: "1px 5px", whiteSpace: "nowrap", maxWidth: 90,
              overflow: "hidden", textOverflow: "ellipsis"
            }}>
              {b.nome}
            </div>
          </div>
        );
      })}

      {/* Legend */}
      <div style={{
        position: "absolute", bottom: 8, left: 8,
        background: "rgba(0,0,0,0.6)", borderRadius: 8,
        padding: "5px 10px", display: "flex", gap: 12, alignItems: "center"
      }}>
        {[["#22C55E", "< 5 min"], ["#F59E0B", "< 30 min"], ["#9CA3AF", "> 30 min"]].map(([c, label]) => (
          <div key={c} style={{ display: "flex", alignItems: "center", gap: 4 }}>
            <div style={{ width: 8, height: 8, borderRadius: "50%", background: c }} />
            <span style={{ fontSize: 9, color: "rgba(255,255,255,0.7)" }}>{label}</span>
          </div>
        ))}
      </div>

      {/* Boat count */}
      <div style={{
        position: "absolute", top: 8, right: 8,
        background: "rgba(0,0,0,0.6)", borderRadius: 8,
        padding: "4px 10px", fontSize: 10, color: "rgba(255,255,255,0.7)"
      }}>
        {boats?.length || 0} embarcacoes
      </div>
    </div>
  );
}

/* ═══ BOAT LIST CARD ═══ */
function BoatCard({ boat, t, onClick }) {
  const cor = statusCor(boat.ultima_atualizacao);
  const tempo = tempoRelativo(boat.ultima_atualizacao);
  return (
    <Cd t={t} onClick={onClick} style={{ padding: 14 }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
        <div style={{ display: "flex", alignItems: "center", gap: 10, flex: 1 }}>
          <div style={{
            width: 36, height: 36, borderRadius: "50%",
            background: t.soft, display: "flex", alignItems: "center", justifyContent: "center",
            border: `2px solid ${cor}`, flexShrink: 0
          }}>
            <IconShip size={18} color={cor} />
          </div>
          <div style={{ minWidth: 0, flex: 1 }}>
            <div style={{ fontSize: 14, fontWeight: 600, color: t.tx }}>{boat.nome}</div>
            <div style={{ display: "flex", alignItems: "center", gap: 4, marginTop: 3 }}>
              <IconMapPin size={11} color={t.txMuted} />
              <span style={{ fontSize: 11, color: t.txMuted }}>
                {boat.latitude?.toFixed(4)}, {boat.longitude?.toFixed(4)}
              </span>
            </div>
          </div>
        </div>
        <div style={{ textAlign: "right", flexShrink: 0 }}>
          <div style={{
            display: "inline-flex", alignItems: "center", gap: 4,
            fontSize: 11, fontWeight: 600, color: cor,
            background: `${cor}18`, padding: "2px 8px", borderRadius: 12
          }}>
            <div style={{ width: 6, height: 6, borderRadius: "50%", background: cor }} />
            {tempo}
          </div>
        </div>
      </div>
    </Cd>
  );
}

/* ═══ MAIN SCREEN ═══ */
export default function MapaCPF({ t, authHeaders }) {
  const { data: boats, loading, erro, refresh } = useGps(authHeaders);
  const [refreshing, setRefreshing] = useState(false);
  const [selectedBoat, setSelectedBoat] = useState(null);

  const handleRefresh = async () => {
    setRefreshing(true);
    await refresh();
    setTimeout(() => setRefreshing(false), 500);
  };

  if (loading && !boats) return <Skeleton t={t} height={80} count={4} />;
  if (erro && !boats) return <ErrorRetry erro={erro} onRetry={refresh} t={t} />;

  const isEmpty = !boats || boats.length === 0;

  return (
    <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 12 }}>
      {/* Header */}
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <div>
          <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700, color: t.tx }}>Mapa GPS</h3>
          <div style={{ fontSize: 11, color: t.txMuted, marginTop: 2 }}>
            Posicao em tempo real das embarcacoes
          </div>
        </div>
        <button onClick={handleRefresh} style={{
          background: t.soft, border: `1px solid ${t.border}`, borderRadius: 10,
          padding: "8px 14px", cursor: "pointer", display: "flex", alignItems: "center",
          gap: 6, color: t.pri, fontSize: 12, fontWeight: 600
        }}>
          <IconRefresh size={14} color={t.pri} style={refreshing ? { animation: "spin 0.6s linear infinite" } : undefined} />
          Atualizar
        </button>
      </div>

      {/* Error banner (non-blocking, when we have stale data) */}
      {erro && boats && (
        <div style={{
          padding: "8px 14px", borderRadius: 10,
          background: t.warnBg, color: t.warnTx,
          fontSize: 12, display: "flex", alignItems: "center", gap: 6
        }}>
          <IconClock size={14} color={t.warnTx} />
          Falha ao atualizar. Mostrando dados anteriores.
        </div>
      )}

      {/* Empty state */}
      {isEmpty && (
        <Cd t={t} style={{ padding: 32, textAlign: "center" }}>
          <IconShip size={40} color={t.txMuted} />
          <div style={{ fontSize: 15, fontWeight: 600, color: t.tx, marginTop: 12 }}>
            Nenhuma embarcacao com GPS
          </div>
          <div style={{ fontSize: 12, color: t.txMuted, marginTop: 4 }}>
            Os dados aparecerao aqui quando as embarcacoes reportarem sua posicao.
          </div>
        </Cd>
      )}

      {/* Map */}
      {!isEmpty && (
        <RiverMap boats={boats} t={t} onSelectBoat={setSelectedBoat} />
      )}

      {/* Selected boat detail */}
      {selectedBoat && (
        <Cd t={t} style={{ padding: 14, border: `2px solid ${statusCor(selectedBoat.ultima_atualizacao)}` }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 8 }}>
            <div style={{ fontSize: 15, fontWeight: 700, color: t.tx }}>{selectedBoat.nome}</div>
            <button onClick={() => setSelectedBoat(null)} style={{
              background: "none", border: "none", fontSize: 18, cursor: "pointer",
              color: t.txMuted, padding: "0 4px", lineHeight: 1
            }}>x</button>
          </div>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
            <div>
              <div style={{ fontSize: 10, color: t.txMuted, textTransform: "uppercase", letterSpacing: 0.5 }}>Latitude</div>
              <div style={{ fontSize: 13, fontWeight: 600, color: t.tx }}>{selectedBoat.latitude?.toFixed(6)}</div>
            </div>
            <div>
              <div style={{ fontSize: 10, color: t.txMuted, textTransform: "uppercase", letterSpacing: 0.5 }}>Longitude</div>
              <div style={{ fontSize: 13, fontWeight: 600, color: t.tx }}>{selectedBoat.longitude?.toFixed(6)}</div>
            </div>
            <div style={{ gridColumn: "span 2" }}>
              <div style={{ fontSize: 10, color: t.txMuted, textTransform: "uppercase", letterSpacing: 0.5 }}>Ultima atualizacao</div>
              <div style={{ display: "flex", alignItems: "center", gap: 6, marginTop: 2 }}>
                <div style={{ width: 8, height: 8, borderRadius: "50%", background: statusCor(selectedBoat.ultima_atualizacao) }} />
                <span style={{ fontSize: 13, fontWeight: 600, color: t.tx }}>
                  {tempoRelativo(selectedBoat.ultima_atualizacao)}
                </span>
                {selectedBoat.ultima_atualizacao && (
                  <span style={{ fontSize: 11, color: t.txMuted }}>
                    ({new Date(selectedBoat.ultima_atualizacao).toLocaleString("pt-BR")})
                  </span>
                )}
              </div>
            </div>
          </div>
        </Cd>
      )}

      {/* Boat list */}
      {!isEmpty && (
        <>
          <div style={{
            fontSize: 12, fontWeight: 700, color: t.pri,
            textTransform: "uppercase", letterSpacing: 1, marginTop: 4
          }}>
            Embarcacoes ({boats.length})
          </div>
          {boats.map((b) => (
            <BoatCard key={b.id_embarcacao} boat={b} t={t} onClick={() => setSelectedBoat(b)} />
          ))}
        </>
      )}

      {/* Auto-refresh indicator */}
      {!isEmpty && (
        <div style={{
          textAlign: "center", fontSize: 10, color: t.txMuted, padding: "4px 0"
        }}>
          Atualizacao automatica a cada 30s
        </div>
      )}

      {/* CSS animations */}
      <style>{`
        @keyframes pulse-gps {
          0%, 100% { box-shadow: 0 0 4px #22C55E; }
          50% { box-shadow: 0 0 14px #22C55E, 0 0 24px rgba(34,197,94,0.3); }
        }
        @keyframes spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }
      `}</style>
    </div>
  );
}

import { useState, useEffect } from "react";
import { API, authFetch } from "../api.js";
import { initials } from "../helpers.js";
import Cd from "../components/Card.jsx";
import Av from "../components/Avatar.jsx";
import Skeleton from "../components/Skeleton.jsx";
import { useTheme } from "../contexts/ThemeContext.jsx";
import { useAuth } from "../contexts/AuthContext.jsx";

export default function PerfilScreen({ onFotoChange }) {
  const { t } = useTheme();
  const { token, authHeaders } = useAuth();
  const [perfil, setPerfil] = useState(null);
  const [editando, setEditando] = useState(false);
  const [form, setForm] = useState({});
  const [erro, setErro] = useState("");
  const [sucesso, setSucesso] = useState("");
  const [loading, setLoading] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [uploadingFoto, setUploadingFoto] = useState(false);
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));
  const inputStyle = { width: "100%", padding: "10px 12px", borderRadius: 8, border: `1px solid ${t.border}`, background: t.soft, color: t.tx, fontSize: 13, outline: "none", boxSizing: "border-box" };
  const labelStyle = { fontSize: 11, fontWeight: 600, color: t.txMuted, marginBottom: 3, display: "block" };
  const valStyle = { fontSize: 14, fontWeight: 500, display: "block", marginTop: 2 };

  useEffect(() => {
    if (!token) return;
    authFetch(`${API}/perfil`, { headers: authHeaders })
      .then(r => r.ok ? r.json() : null)
      .then(d => { if (d) { setPerfil(d); setForm({ nome: d.nome || "", email: d.email || "", telefone: d.telefone || "", cidade: d.cidade || "" }); } })
      .catch(e => console.warn("[PerfilScreen] erro ao carregar perfil:", e?.message))
      .finally(() => setLoading(false));
  }, [token]);

  const salvar = async () => {
    setSalvando(true); setErro(""); setSucesso("");
    try {
      const res = await authFetch(`${API}/perfil`, { method: "PUT", headers: authHeaders, body: JSON.stringify(form) });
      if (res.ok) { const d = await res.json(); setPerfil(d); setEditando(false); setSucesso("Perfil atualizado!"); }
      else { const d = await res.json(); setErro(d.erro || "Erro ao salvar."); }
    } catch { setErro("Erro de conex\u00e3o."); } finally { setSalvando(false); }
  };

  const uploadFoto = async (e) => {
    const file = e.target.files?.[0]; if (!file) return;
    setUploadingFoto(true);
    try {
      const fd = new FormData(); fd.append("foto", file);
      const res = await authFetch(`${API}/perfil/foto`, { method: "POST", headers: { "Authorization": authHeaders.Authorization }, body: fd });
      if (res.ok) { const d = await res.json(); if (d.fotoUrl) { setPerfil(p => ({ ...p, fotoUrl: d.fotoUrl })); onFotoChange(`${API}${d.fotoUrl}`); } }
      else { console.warn("[PerfilScreen] upload de foto retornou HTTP", res.status); }
    } catch (e) {
      console.warn("[PerfilScreen] erro no upload de foto:", e?.message);
    } finally { setUploadingFoto(false); }
  };

  if (loading) return <Skeleton height={60} count={3} />;
  if (!perfil) return <Cd style={{ padding: 16, textAlign: "center" }}><div style={{ fontSize: 13, color: t.txMuted }}>Erro ao carregar perfil.</div></Cd>;

  return <div className="screen-enter" style={{ display: "flex", flexDirection: "column", gap: 14 }}>
    <h1 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Meu perfil</h1>
    {sucesso && <div role="status" style={{ padding: "10px 14px", borderRadius: 10, background: t.okBg, color: t.okTx, fontSize: 12, fontWeight: 500 }}>{sucesso}</div>}
    {erro && <div role="alert" style={{ padding: "10px 14px", borderRadius: 10, background: t.errBg, color: t.errTx, fontSize: 12, fontWeight: 500 }}>{erro}</div>}
    <Cd style={{ padding: 18 }}>
      <div style={{ display: "flex", alignItems: "center", gap: 14, marginBottom: 16 }}>
        <div style={{ position: "relative" }}>
          <Av letters={initials(perfil.nome)} size={64} fotoUrl={perfil.fotoUrl} />
          <label style={{ position: "absolute", bottom: -2, right: -2, width: 24, height: 24, borderRadius: "50%", background: t.priGrad, display: "flex", alignItems: "center", justifyContent: "center", cursor: uploadingFoto ? "default" : "pointer", border: `2px solid ${t.card}` }}>
            <span style={{ fontSize: 12, color: "#fff" }}>{uploadingFoto ? "..." : "+"}</span>
            <input type="file" accept="image/jpeg,image/png,image/webp" onChange={uploadFoto} style={{ display: "none" }} disabled={uploadingFoto} />
          </label>
        </div>
        <div><div style={{ fontSize: 16, fontWeight: 700 }}>{perfil.nome}</div><div style={{ fontSize: 12, color: t.txMuted }}>{perfil.tipo} {perfil.documento}</div></div>
      </div>
      {editando ? <>
        <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
          <div><label htmlFor="perfil-nome" style={labelStyle}>Nome</label><input id="perfil-nome" name="nome" autoComplete="name" value={form.nome} onChange={e => set("nome", e.target.value)} className="input-field" style={inputStyle} /></div>
          <div><label htmlFor="perfil-email" style={labelStyle}>Email</label><input id="perfil-email" name="email" autoComplete="email" value={form.email} onChange={e => set("email", e.target.value)} type="email" className="input-field" style={inputStyle} /></div>
          <div><label htmlFor="perfil-telefone" style={labelStyle}>Telefone</label><input id="perfil-telefone" name="telefone" autoComplete="tel" type="tel" inputMode="tel" value={form.telefone} onChange={e => set("telefone", e.target.value)} className="input-field" style={inputStyle} /></div>
          <div><label htmlFor="perfil-cidade" style={labelStyle}>Cidade</label><input id="perfil-cidade" name="cidade" autoComplete="address-level2" value={form.cidade} onChange={e => set("cidade", e.target.value)} className="input-field" style={inputStyle} /></div>
        </div>
        <div style={{ display: "flex", gap: 8, marginTop: 12 }}>
          <button onClick={() => { setEditando(false); setErro(""); setForm({ nome: perfil.nome || "", email: perfil.email || "", telefone: perfil.telefone || "", cidade: perfil.cidade || "" }); }} className="btn-outline" style={{ flex: 1, padding: "10px 0", border: `1px solid ${t.border}`, color: t.txMuted, borderRadius: 10 }}>Cancelar</button>
          <button onClick={salvar} disabled={salvando} className="btn-primary" style={{ flex: 1, padding: "10px 0", background: salvando ? t.txMuted : t.priGrad, color: "#fff" }}>{salvando ? "Salvando..." : "Salvar"}</button>
        </div>
      </> : <>
        <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
          <div><span style={labelStyle}>Email</span><span style={valStyle}>{perfil.email || "\u2014"}</span></div>
          <div><span style={labelStyle}>Telefone</span><span style={valStyle}>{perfil.telefone || "\u2014"}</span></div>
          <div><span style={labelStyle}>Cidade</span><span style={valStyle}>{perfil.cidade || "\u2014"}</span></div>
        </div>
        <button onClick={() => { setEditando(true); setSucesso(""); }} className="btn-primary" style={{ background: t.priGrad, color: "#fff", marginTop: 12 }}>Editar perfil</button>
      </>}
    </Cd>
  </div>;
}

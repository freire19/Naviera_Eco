import { useState } from "react";
import { API } from "../api.js";
import { maskDoc, validarDocumento } from "../helpers.js";
import { IconBack } from "../icons.jsx";
import Logo from "../components/Logo.jsx";

export default function TelaCadastro({ t, onVoltar, onSucesso }) {
  const [tipo, setTipo] = useState("CPF");
  const [form, setForm] = useState({ documento: "", nome: "", email: "", telefone: "", cidade: "", senha: "", senhaConfirm: "" });
  const [erro, setErro] = useState("");
  const [loading, setLoading] = useState(false);
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));
  const inputStyle = { width: "100%", padding: "12px 14px", borderRadius: 10, border: `1px solid ${t.border}`, background: t.soft, color: t.tx, fontSize: 13, outline: "none", boxSizing: "border-box" };
  const labelStyle = { fontSize: 12, fontWeight: 600, color: t.txSoft, marginBottom: 4, display: "block" };

  const submit = async () => {
    // #DR281: bloquear double-submit (Enter repetido / clique duplo).
    if (loading) return;
    setErro("");
    if (!form.documento.trim() || !form.nome.trim() || !form.senha.trim()) { setErro("Documento, nome e senha s\u00e3o obrigat\u00f3rios."); return; }
    const docErro = validarDocumento(form.documento, tipo);
    if (docErro) { setErro(docErro); return; }
    if (form.senha.length < 6) { setErro("Senha deve ter no m\u00ednimo 6 caracteres."); return; }
    if (form.senha !== form.senhaConfirm) { setErro("As senhas n\u00e3o conferem."); return; }
    setLoading(true);
    try {
      const res = await fetch(`${API}/auth/registrar`, { method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ documento: form.documento.replace(/\D/g, ""), tipoDocumento: tipo, nome: form.nome.trim(), email: form.email.trim() || null, telefone: form.telefone.trim() || null, cidade: form.cidade.trim() || null, senha: form.senha }) });
      const data = await res.json();
      if (!res.ok) { setErro(data.erro || "Erro ao cadastrar."); return; }
      onSucesso(data);
    } catch { setErro("Erro de conex\u00e3o com o servidor."); } finally { setLoading(false); }
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 14, width: "100%", maxWidth: 380 }}>
      <button onClick={onVoltar} style={{ background: "none", border: "none", color: t.txMuted, fontSize: 14, cursor: "pointer", padding: 0, textAlign: "left", display: "flex", alignItems: "center", gap: 4 }}><IconBack size={16} color={t.txMuted} /> Voltar</button>
      <div style={{ textAlign: "center", marginBottom: 4 }}><Logo size={40} t={t} /><h2 style={{ margin: "8px 0 2px", fontSize: 22, fontWeight: 700 }}>Criar conta</h2><p style={{ fontSize: 12, color: t.txMuted, margin: 0 }}>Preencha seus dados para come\u00e7ar</p></div>
      <div style={{ display: "flex", gap: 8 }}>
        {["CPF", "CNPJ"].map(tp => <button key={tp} onClick={() => { setTipo(tp); set("documento", ""); }} style={{ flex: 1, padding: "10px 0", borderRadius: 10, border: `1px solid ${tipo === tp ? t.pri : t.border}`, background: tipo === tp ? t.accent : "transparent", color: tipo === tp ? t.pri : t.txMuted, fontSize: 13, fontWeight: 600, cursor: "pointer" }}>{tp}</button>)}
      </div>
      <div><label style={labelStyle}>{tipo}</label><input value={form.documento} onChange={e => set("documento", maskDoc(e.target.value, tipo))} placeholder={tipo === "CPF" ? "000.000.000-00" : "00.000.000/0001-00"} className="input-field" style={inputStyle} /></div>
      <div><label style={labelStyle}>Nome completo</label><input value={form.nome} onChange={e => set("nome", e.target.value)} placeholder="Seu nome" className="input-field" style={inputStyle} /></div>
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8 }}>
        <div><label style={labelStyle}>Email</label><input value={form.email} onChange={e => set("email", e.target.value)} placeholder="email@exemplo.com" type="email" className="input-field" style={inputStyle} /></div>
        <div><label style={labelStyle}>Telefone</label><input value={form.telefone} onChange={e => set("telefone", e.target.value)} placeholder="(92) 99999-0000" className="input-field" style={inputStyle} /></div>
      </div>
      <div><label style={labelStyle}>Cidade</label><input value={form.cidade} onChange={e => set("cidade", e.target.value)} placeholder="Sua cidade" className="input-field" style={inputStyle} /></div>
      <div><label style={labelStyle}>Senha</label><input value={form.senha} onChange={e => set("senha", e.target.value)} type="password" placeholder="M\u00ednimo 6 caracteres" className="input-field" style={inputStyle} /></div>
      <div><label style={labelStyle}>Confirmar senha</label><input value={form.senhaConfirm} onChange={e => set("senhaConfirm", e.target.value)} type="password" placeholder="Repita a senha" className="input-field" style={inputStyle} /></div>
      {erro && <div style={{ padding: "10px 14px", borderRadius: 10, background: t.errBg, color: t.errTx, fontSize: 12, fontWeight: 500 }}>{erro}</div>}
      <button onClick={submit} disabled={loading} className="btn-primary" style={{ background: loading ? t.txMuted : t.priGrad, color: "#fff", opacity: loading ? 0.7 : 1 }}>{loading ? "Cadastrando..." : "Criar conta"}</button>
    </div>
  );
}

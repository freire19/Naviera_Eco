import { useState } from "react";
import { API, authFetch } from "../api.js";
import { maskDoc, validarDocumento } from "../helpers.js";
import { IconBack } from "../icons.jsx";
import Logo from "../components/Logo.jsx";
import { useTheme } from "../contexts/ThemeContext.jsx";

export default function TelaCadastro({ onVoltar, onSucesso }) {
  const { t } = useTheme();
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
      const res = await authFetch(`${API}/auth/registrar`, { method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ documento: form.documento.replace(/\D/g, ""), tipoDocumento: tipo, nome: form.nome.trim(), email: form.email.trim() || null, telefone: form.telefone.trim() || null, cidade: form.cidade.trim() || null, senha: form.senha }) });
      const data = await res.json();
      if (!res.ok) { setErro(data.erro || "Erro ao cadastrar."); return; }
      onSucesso(data);
    } catch { setErro("Erro de conex\u00e3o com o servidor."); } finally { setLoading(false); }
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 14, width: "100%", maxWidth: 380 }}>
      <button onClick={onVoltar} style={{ background: "none", border: "none", color: t.txMuted, fontSize: 14, cursor: "pointer", padding: 0, textAlign: "left", display: "flex", alignItems: "center", gap: 4 }}><IconBack size={16} color={t.txMuted} /> Voltar</button>
      <div style={{ textAlign: "center", marginBottom: 4 }}><Logo size={40} /><h2 style={{ margin: "8px 0 2px", fontSize: 22, fontWeight: 700 }}>Criar conta</h2><p style={{ fontSize: 12, color: t.txMuted, margin: 0 }}>Preencha seus dados para come\u00e7ar</p></div>
      <div style={{ display: "flex", gap: 8 }}>
        {["CPF", "CNPJ"].map(tp => <button key={tp} onClick={() => { setTipo(tp); set("documento", ""); }} style={{ flex: 1, padding: "10px 0", borderRadius: 10, border: `1px solid ${tipo === tp ? t.pri : t.border}`, background: tipo === tp ? t.accent : "transparent", color: tipo === tp ? t.pri : t.txMuted, fontSize: 13, fontWeight: 600, cursor: "pointer" }}>{tp}</button>)}
      </div>
      <div><label htmlFor="cad-documento" style={labelStyle}>{tipo}</label><input id="cad-documento" name="documento" autoComplete="username" inputMode="numeric" required value={form.documento} onChange={e => set("documento", maskDoc(e.target.value, tipo))} placeholder={tipo === "CPF" ? "000.000.000-00" : "00.000.000/0001-00"} className="input-field" style={inputStyle} /></div>
      <div><label htmlFor="cad-nome" style={labelStyle}>Nome completo</label><input id="cad-nome" name="nome" autoComplete="name" required value={form.nome} onChange={e => set("nome", e.target.value)} placeholder="Seu nome" className="input-field" style={inputStyle} /></div>
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8 }}>
        <div><label htmlFor="cad-email" style={labelStyle}>Email</label><input id="cad-email" name="email" autoComplete="email" value={form.email} onChange={e => set("email", e.target.value)} placeholder="email@exemplo.com" type="email" className="input-field" style={inputStyle} /></div>
        <div><label htmlFor="cad-telefone" style={labelStyle}>Telefone</label><input id="cad-telefone" name="telefone" autoComplete="tel" type="tel" inputMode="tel" value={form.telefone} onChange={e => set("telefone", e.target.value)} placeholder="(92) 99999-0000" className="input-field" style={inputStyle} /></div>
      </div>
      <div><label htmlFor="cad-cidade" style={labelStyle}>Cidade</label><input id="cad-cidade" name="cidade" autoComplete="address-level2" value={form.cidade} onChange={e => set("cidade", e.target.value)} placeholder="Sua cidade" className="input-field" style={inputStyle} /></div>
      <div><label htmlFor="cad-senha" style={labelStyle}>Senha</label><input id="cad-senha" name="senha" autoComplete="new-password" required value={form.senha} onChange={e => set("senha", e.target.value)} type="password" placeholder="M\u00ednimo 6 caracteres" className="input-field" style={inputStyle} /></div>
      <div><label htmlFor="cad-senha-confirm" style={labelStyle}>Confirmar senha</label><input id="cad-senha-confirm" name="senhaConfirm" autoComplete="new-password" required value={form.senhaConfirm} onChange={e => set("senhaConfirm", e.target.value)} type="password" placeholder="Repita a senha" className="input-field" style={inputStyle} /></div>
      {erro && <div role="alert" style={{ padding: "10px 14px", borderRadius: 10, background: t.errBg, color: t.errTx, fontSize: 12, fontWeight: 500 }}>{erro}</div>}
      <button onClick={submit} disabled={loading} className="btn-primary" style={{ background: loading ? t.txMuted : t.priGrad, color: "#fff", opacity: loading ? 0.7 : 1 }}>{loading ? "Cadastrando..." : "Criar conta"}</button>
    </div>
  );
}

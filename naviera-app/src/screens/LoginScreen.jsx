import { useState } from "react";
import { API } from "../api.js";
import { maskDoc } from "../helpers.js";
import { IconMoon, IconSun } from "../icons.jsx";
import Logo from "../components/Logo.jsx";
import TelaCadastro from "./TelaCadastro.jsx";

export default function LoginScreen({ t, mode, setMode, onLogin }) {
  const [tela, setTela] = useState("login");
  const [msgSucesso, setMsgSucesso] = useState("");
  const [loginDoc, setLoginDoc] = useState("");
  const [loginSenha, setLoginSenha] = useState("");
  const [loginErro, setLoginErro] = useState("");
  const [loginLoading, setLoginLoading] = useState(false);
  const [loginTipo, setLoginTipo] = useState("CPF");

  const inputStyle = { width: "100%", padding: "12px 14px", borderRadius: 10, border: `1px solid ${t.border}`, background: t.soft, color: t.tx, fontSize: 13, outline: "none", boxSizing: "border-box" };

  const doLogin = async () => {
    setLoginErro("");
    if (!loginDoc.trim() || !loginSenha.trim()) { setLoginErro("Informe documento e senha."); return; }
    setLoginLoading(true);
    try {
      const res = await fetch(`${API}/auth/login`, { method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ documento: loginDoc.replace(/\D/g, ""), senha: loginSenha }) });
      const data = await res.json();
      if (!res.ok) { setLoginErro(data.erro || "Credenciais inv\u00e1lidas."); return; }
      onLogin(data);
      setLoginDoc(""); setLoginSenha(""); setMsgSucesso("");
    } catch { setLoginErro("Erro de conex\u00e3o com o servidor."); } finally { setLoginLoading(false); }
  };

  return (
    <div style={{ minHeight: "100vh", background: t.bg, display: "flex", alignItems: "center", justifyContent: "center", padding: 20, color: t.tx, transition: "background 0.3s" }}>
      {tela === "cadastro" ? (
        <TelaCadastro t={t} onVoltar={() => setTela("login")} onSucesso={(data) => {
          onLogin(data);
        }} />
      ) : (
      <div style={{ width: "100%", maxWidth: 380, textAlign: "center" }}>
        <div style={{ marginBottom: 20 }}><Logo size={60} t={t} /></div>
        <h1 style={{ fontSize: 32, fontWeight: 800, margin: "0 0 4px", letterSpacing: 4, color: t.tx }}>NAVIERA</h1>
        <p style={{ fontSize: 12, color: t.txMuted, margin: "0 0 24px", letterSpacing: 6, textTransform: "uppercase", fontWeight: 300 }}>Navega\u00e7\u00e3o fluvial</p>
        {msgSucesso && <div style={{ padding: "10px 14px", borderRadius: 10, background: t.okBg, color: t.okTx, fontSize: 12, fontWeight: 500, marginBottom: 16 }}>{msgSucesso}</div>}
        <div style={{ display: "flex", flexDirection: "column", gap: 10, textAlign: "left" }}>
          <div><label style={{ fontSize: 12, fontWeight: 600, color: t.txSoft, marginBottom: 4, display: "block" }}>CPF ou CNPJ</label>
            <input value={loginDoc} onChange={e => {
              const raw = e.target.value.replace(/\D/g, "");
              const tp = raw.length > 11 ? "CNPJ" : "CPF";
              setLoginTipo(tp);
              setLoginDoc(maskDoc(e.target.value, tp));
            }} placeholder="000.000.000-00" className="input-field" style={inputStyle} onKeyDown={e => e.key === "Enter" && doLogin()} /></div>
          <div><label style={{ fontSize: 12, fontWeight: 600, color: t.txSoft, marginBottom: 4, display: "block" }}>Senha</label>
            <input value={loginSenha} onChange={e => setLoginSenha(e.target.value)} type="password" placeholder="Sua senha" className="input-field" style={inputStyle} onKeyDown={e => e.key === "Enter" && doLogin()} /></div>
          {loginErro && <div style={{ padding: "10px 14px", borderRadius: 10, background: t.errBg, color: t.errTx, fontSize: 12, fontWeight: 500 }}>{loginErro}</div>}
          <button onClick={doLogin} disabled={loginLoading} className="btn-primary" style={{ background: loginLoading ? t.txMuted : t.priGrad, color: "#fff", marginTop: 4 }}>{loginLoading ? "Entrando..." : "Entrar"}</button>
        </div>
        <button onClick={() => { setMsgSucesso(""); setLoginErro(""); setTela("cadastro"); }} style={{ marginTop: 16, background: "none", border: "none", color: t.pri, fontSize: 13, fontWeight: 600, cursor: "pointer" }}>N\u00e3o tem conta? <span style={{ textDecoration: "underline" }}>Cadastre-se</span></button>
        <div><button onClick={() => setMode(m => m === "light" ? "dark" : "light")} style={{ marginTop: 12, padding: "8px 20px", borderRadius: 10, background: t.soft, border: `1px solid ${t.border}`, cursor: "pointer", color: t.txMuted, fontSize: 12, display: "inline-flex", alignItems: "center", gap: 6 }}>
          {mode === "light" ? <><IconMoon size={12} color={t.txMuted} /> Dark</> : <><IconSun size={12} color={t.txMuted} /> Light</>}
        </button></div>
        <p style={{ fontSize: 10, color: t.txMuted, marginTop: 16, fontFamily: "'Space Mono', monospace" }}>Naviera v4.0</p>
      </div>)}
    </div>
  );
}

/* ═══ HELPERS ═══ */
export const fmt = (d) => d ? new Date(d + "T00:00:00").toLocaleDateString("pt-BR") : "\u2014";
export const money = (v) => v != null ? `R$ ${Number(v).toLocaleString("pt-BR", { minimumFractionDigits: 2 })}` : "\u2014";
export const initials = (name) => name ? name.split(" ").map(w => w[0]).join("").slice(0, 2).toUpperCase() : "?";

export const maskCPF = (v) => v.replace(/\D/g, "").slice(0, 11).replace(/(\d{3})(\d)/, "$1.$2").replace(/(\d{3})(\d)/, "$1.$2").replace(/(\d{3})(\d{1,2})$/, "$1-$2");
export const maskCNPJ = (v) => v.replace(/\D/g, "").slice(0, 14).replace(/(\d{2})(\d)/, "$1.$2").replace(/(\d{3})(\d)/, "$1.$2").replace(/(\d{3})(\d)/, "$1/$2").replace(/(\d{4})(\d{1,2})$/, "$1-$2");
export const maskDoc = (v, tipo) => tipo === "CNPJ" ? maskCNPJ(v) : maskCPF(v);

export const validarDocumento = (doc, tipo) => {
  const nums = doc.replace(/\D/g, "");
  if (tipo === "CPF" && nums.length !== 11) return "CPF deve ter 11 d\u00edgitos.";
  if (tipo === "CNPJ" && nums.length !== 14) return "CNPJ deve ter 14 d\u00edgitos.";
  return null;
};

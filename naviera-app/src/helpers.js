/* === HELPERS === */
export const fmt = (d) => d ? new Date(d + "T00:00:00").toLocaleDateString("pt-BR") : "\u2014";
export const money = (v) => v != null ? `R$ ${Number(v).toLocaleString("pt-BR", { minimumFractionDigits: 2 })}` : "\u2014";
export const initials = (name) => name ? name.split(" ").map(w => w[0]).join("").slice(0, 2).toUpperCase() : "?";

export const maskCPF = (v) => v.replace(/\D/g, "").slice(0, 11).replace(/(\d{3})(\d)/, "$1.$2").replace(/(\d{3})(\d)/, "$1.$2").replace(/(\d{3})(\d{1,2})$/, "$1-$2");
export const maskCNPJ = (v) => v.replace(/\D/g, "").slice(0, 14).replace(/(\d{2})(\d)/, "$1.$2").replace(/(\d{3})(\d)/, "$1.$2").replace(/(\d{3})(\d)/, "$1/$2").replace(/(\d{4})(\d{1,2})$/, "$1-$2");
export const maskDoc = (v, tipo) => tipo === "CNPJ" ? maskCNPJ(v) : maskCPF(v);

export const validarDocumento = (doc, tipo) => {
  const nums = doc.replace(/\D/g, "");
  if (tipo === "CPF") return validarCPF(nums);
  if (tipo === "CNPJ") return validarCNPJ(nums);
  return "Tipo de documento invalido.";
};

function validarCPF(nums) {
  if (nums.length !== 11) return "CPF deve ter 11 d\u00edgitos.";
  if (/^(\d)\1{10}$/.test(nums)) return "CPF invalido.";

  let soma = 0;
  for (let i = 0; i < 9; i++) soma += parseInt(nums[i]) * (10 - i);
  let resto = (soma * 10) % 11;
  if (resto === 10) resto = 0;
  if (resto !== parseInt(nums[9])) return "CPF invalido.";

  soma = 0;
  for (let i = 0; i < 10; i++) soma += parseInt(nums[i]) * (11 - i);
  resto = (soma * 10) % 11;
  if (resto === 10) resto = 0;
  if (resto !== parseInt(nums[10])) return "CPF invalido.";

  return null;
}

function validarCNPJ(nums) {
  if (nums.length !== 14) return "CNPJ deve ter 14 d\u00edgitos.";
  if (/^(\d)\1{13}$/.test(nums)) return "CNPJ invalido.";

  const pesos1 = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
  const pesos2 = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];

  let soma = 0;
  for (let i = 0; i < 12; i++) soma += parseInt(nums[i]) * pesos1[i];
  let resto = soma % 11;
  const dig1 = resto < 2 ? 0 : 11 - resto;
  if (dig1 !== parseInt(nums[12])) return "CNPJ invalido.";

  soma = 0;
  for (let i = 0; i < 13; i++) soma += parseInt(nums[i]) * pesos2[i];
  resto = soma % 11;
  const dig2 = resto < 2 ? 0 : 11 - resto;
  if (dig2 !== parseInt(nums[13])) return "CNPJ invalido.";

  return null;
}

export const fmt = (d) => d ? new Date(d + "T00:00:00").toLocaleDateString("pt-BR") : "\u2014";
export const fmtDateTime = (d) => d ? new Date(d).toLocaleString("pt-BR") : "\u2014";
export const money = (v) => v != null ? `R$ ${Number(v).toLocaleString("pt-BR", { minimumFractionDigits: 2 })}` : "\u2014";
export const timeAgo = (d) => {
  if (!d) return "";
  const diff = Date.now() - new Date(d).getTime();
  const min = Math.floor(diff / 60000);
  if (min < 1) return "agora";
  if (min < 60) return `${min}min`;
  const h = Math.floor(min / 60);
  if (h < 24) return `${h}h`;
  const days = Math.floor(h / 24);
  return `${days}d`;
};

# Naviera App

App para clientes finais (CPF pessoa fisica, CNPJ loja parceira) do sistema Naviera. Desenvolvido em React web para iteracao rapida da UI — destino final e mobile.

## Setup

```bash
# Instalar dependencias
npm install

# Copiar e configurar variaveis de ambiente
cp .env.example .env

# Iniciar dev server
npm run dev
```

## Requer

- API Spring Boot rodando em `VITE_API_URL` (default: `http://localhost:8081/api`)

## Telas

**Perfil CPF (pessoa fisica):** Home, Amigos, Mapa, Passagens, Bilhete, Perfil

**Perfil CNPJ (loja parceira):** Painel, Pedidos, Parceiros, Financeiro, Loja

## Stack

- React 19 + Vite
- Design system Naviera V4 (light/dark)
- Auth JWT via localStorage
- Validacao CPF/CNPJ com digitos verificadores

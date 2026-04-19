# ── Stage 1: Build ──
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# ── Stage 2: Serve ──
# #DS5-402: imagem nginx oficial ja cria o usuario 'nginx' (uid 101) — executa como non-root
# via entrypoint que aplica chown e depois dropa para nginx user nos workers.
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx-http.conf nginx-https.conf /etc/nginx/templates/
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh \
 && chown -R nginx:nginx /usr/share/nginx/html /var/cache/nginx /var/log/nginx /etc/nginx \
 && touch /var/run/nginx.pid \
 && chown nginx:nginx /var/run/nginx.pid
# nginx:alpine nao tem /etc/nginx/conf.d pertencendo a nginx por default; garantido acima.
EXPOSE 80 443
ENTRYPOINT ["/docker-entrypoint.sh"]

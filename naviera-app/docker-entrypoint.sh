#!/bin/sh
# Auto-seleciona config HTTP ou HTTPS baseado na presenca de certificados

CERT_FILE="/etc/nginx/certs/fullchain.pem"
KEY_FILE="/etc/nginx/certs/privkey.pem"

if [ -f "$CERT_FILE" ] && [ -f "$KEY_FILE" ]; then
    echo "[naviera] SSL certs encontrados — ativando HTTPS"
    cp /etc/nginx/templates/nginx-https.conf /etc/nginx/conf.d/default.conf
else
    echo "[naviera] SSL certs nao encontrados — servindo em HTTP"
    cp /etc/nginx/templates/nginx-http.conf /etc/nginx/conf.d/default.conf
fi

exec nginx -g 'daemon off;'

# Образ фронтенду для сервера: збираємо Angular і віддаємо готові файли через Caddy
# (Caddy також проксує /api і /uploads на бекенд і сам отримує HTTPS-сертифікат — див. deploy/Caddyfile)

# Крок 1: збірка
FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npx ng build

# Крок 2: лише статичні файли + вебсервер
FROM caddy:2-alpine
COPY --from=build /app/dist/frontend/browser /srv

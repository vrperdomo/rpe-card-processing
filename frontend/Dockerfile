# syntax=docker/dockerfile:1
#
# Frontend: build com Node e runtime com Nginx sem privilégios (usuário não-root, porta 8080).
# O contexto de build é frontend/ (ver docker-compose.yml).

FROM node:22-alpine AS build
WORKDIR /app

# Só os manifestos primeiro: a camada do "npm ci" fica em cache enquanto as dependências não mudam.
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY . .
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.29-alpine AS runtime
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY rpe-proxy.conf /etc/nginx/rpe-proxy.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=5s --retries=5 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/healthz || exit 1

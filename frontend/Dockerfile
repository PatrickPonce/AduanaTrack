# --- Etapa 1: build de Vite ---
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# --- Etapa 2: nginx sin privilegios sirve dist/ y hace de proxy /api → backend ---
# La imagen unprivileged escucha en 8080 (un usuario sin root no puede usar el puerto 80).
FROM nginxinc/nginx-unprivileged:alpine
# La plantilla se procesa al arrancar: BACKEND_URL (sin barra final) es el backend al que se reenvía /api.
ENV BACKEND_URL=http://backend:8080
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
USER 101
EXPOSE 8080

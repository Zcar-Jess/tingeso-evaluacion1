# ---- Stage 1: build the React app ----
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
# The Keycloak URL is only known when the container starts, so the build uses a placeholder that
# docker-entrypoint.sh replaces with the KEYCLOAK_URL environment variable.
ENV VITE_API_URL="" \
    VITE_AUTH_MODE=keycloak \
    VITE_KEYCLOAK_URL=__KEYCLOAK_URL__ \
    VITE_KEYCLOAK_REALM=siga-ipt \
    VITE_KEYCLOAK_CLIENT_ID=siga-frontend
RUN npm run build

# ---- Stage 2: Nginx serving the static files ----
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
COPY docker-entrypoint.sh /docker-entrypoint.d/40-keycloak-url.sh
RUN chmod +x /docker-entrypoint.d/40-keycloak-url.sh
ENV KEYCLOAK_URL=http://localhost:9090
EXPOSE 80
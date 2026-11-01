#!/bin/sh
# Runs when the container starts (the official nginx image executes /docker-entrypoint.d/*.sh).
# Puts the real Keycloak URL in the compiled files.
set -e
: "${KEYCLOAK_URL:?KEYCLOAK_URL must be set}"
find /usr/share/nginx/html -type f \( -name '*.js' -o -name '*.html' \) \
  -exec sed -i "s|__KEYCLOAK_URL__|${KEYCLOAK_URL}|g" {} +
echo "Frontend configured with KEYCLOAK_URL=${KEYCLOAK_URL}"
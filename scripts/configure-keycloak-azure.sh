#!/usr/bin/env bash
# Run ON Azure VM (SSH). Uses localhost:8180 for Admin API (HTTP), FE URL stays public IP.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

export KEYCLOAK_URL="${KEYCLOAK_URL:-http://127.0.0.1:8180}"
export FE_PUBLIC_URL="${FE_PUBLIC_URL:-http://${PUBLIC_HOST:-localhost}:${FRONTEND_PORT:-4200}}"

echo "KEYCLOAK_URL=$KEYCLOAK_URL (admin — localhost on VM)"
echo "FE_PUBLIC_URL=$FE_PUBLIC_URL"

if ! curl -fsS -o /dev/null -m 5 "$KEYCLOAK_URL/realms/master"; then
  echo "Keycloak not reachable at $KEYCLOAK_URL — is sam-identity running?"
  exit 1
fi

bash "$ROOT/identity-service/keycloak/configure-sam-shop.sh"

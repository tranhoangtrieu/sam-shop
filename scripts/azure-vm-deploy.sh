#!/usr/bin/env bash
# Deploy Sam Shop on Azure VM (Ubuntu) with Docker Compose
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker not found. Install: https://docs.docker.com/engine/install/ubuntu/"
  exit 1
fi

if [ ! -f .env ]; then
  if [ -f .env.azure.example ]; then
    cp .env.azure.example .env
    echo "Created .env from .env.azure.example — edit PUBLIC_HOST and passwords, then re-run."
    exit 1
  fi
  echo "Missing .env — set PUBLIC_HOST to your VM public IP or DNS."
  exit 1
fi

# shellcheck disable=SC1091
set -a && source .env && set +a

if [ -z "${PUBLIC_HOST:-}" ] || [ "$PUBLIC_HOST" = "20.x.x.x" ] || [ "$PUBLIC_HOST" = "CHANGE_ME" ]; then
  echo "Set PUBLIC_HOST in .env to your VM public IP or DNS (e.g. 20.12.34.56)."
  exit 1
fi

echo "Deploying with PUBLIC_HOST=$PUBLIC_HOST"
docker compose up --build -d

echo ""
echo "Open: http://${PUBLIC_HOST}:${FRONTEND_PORT:-4200}"
echo "Keycloak: http://${PUBLIC_HOST}:${KEYCLOAK_PORT:-8180}"
echo ""
echo "Configure realm (from your PC or VM):"
echo "  KEYCLOAK_URL=http://${PUBLIC_HOST}:${KEYCLOAK_PORT:-8180} \\"
echo "  FE_PUBLIC_URL=http://${PUBLIC_HOST}:${FRONTEND_PORT:-4200} \\"
echo "  powershell -File identity-service/keycloak/configure-sam-shop.ps1"

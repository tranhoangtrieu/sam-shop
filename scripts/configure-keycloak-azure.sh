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

if ! command -v pwsh >/dev/null 2>&1; then
  echo "Installing PowerShell..."
  sudo apt-get update -qq
  sudo apt-get install -y wget apt-transport-https software-properties-common
  wget -q https://packages.microsoft.com/config/ubuntu/22.04/packages-microsoft-prod.deb -O /tmp/packages-microsoft-prod.deb
  sudo dpkg -i /tmp/packages-microsoft-prod.deb
  sudo apt-get update -qq
  sudo apt-get install -y powershell
fi

pwsh -ExecutionPolicy Bypass -File "$ROOT/identity-service/keycloak/configure-sam-shop.ps1"

#!/usr/bin/env bash
# Nuclear option: delete and recreate test users (customer1, staff1, admin1)
set -euo pipefail

KEYCLOAK_URL="${KEYCLOAK_URL:-http://127.0.0.1:8180}"
KEYCLOAK_URL="${KEYCLOAK_URL%/}"
ADMIN_PASSWORD="${KC_ADMIN_PASSWORD:-admin}"
REALM="${KEYCLOAK_REALM:-sam-shop}"

docker exec sam-identity /opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 --realm master --user admin --password "$ADMIN_PASSWORD" >/dev/null

for u in customer1 staff1 admin1; do
  docker exec sam-identity /opt/keycloak/bin/kcadm.sh delete users -r "$REALM" -q "username=$u" 2>/dev/null || true
done

export KEYCLOAK_URL FE_PUBLIC_URL="${FE_PUBLIC_URL:-http://20.24.185.233:4200}"
export KC_ADMIN_PASSWORD="$ADMIN_PASSWORD"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
bash "$SCRIPT_DIR/configure-sam-shop.sh"

#!/usr/bin/env bash
# Fix "Account is not fully set up" — clear required actions and reset passwords
set -euo pipefail

KEYCLOAK_URL="${KEYCLOAK_URL:-http://127.0.0.1:8180}"
KEYCLOAK_URL="${KEYCLOAK_URL%/}"
ADMIN_USER="${KC_ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${KC_ADMIN_PASSWORD:-admin}"
REALM="${KEYCLOAK_REALM:-sam-shop}"

TOKEN=$(curl -fsS -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=admin-cli&username=${ADMIN_USER}&password=${ADMIN_PASSWORD}&grant_type=password" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

AUTH=(-H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json")

fix_user() {
  local username=$1 password=$2
  local users user_id
  users=$(curl -fsS "${AUTH[@]}" "$KEYCLOAK_URL/admin/realms/$REALM/users?username=${username}&exact=true")
  user_id=$(echo "$users" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d[0]['id'] if d else '')")
  if [ -z "$user_id" ]; then
    echo "User $username not found"
    return
  fi
  curl -fsS -X PUT "${AUTH[@]}" \
    -d '{"enabled":true,"emailVerified":true,"requiredActions":[]}' \
    "$KEYCLOAK_URL/admin/realms/$REALM/users/$user_id" >/dev/null
  curl -fsS -X PUT "${AUTH[@]}" \
    -d "{\"type\":\"password\",\"value\":\"$password\",\"temporary\":false}" \
    "$KEYCLOAK_URL/admin/realms/$REALM/users/$user_id/reset-password" >/dev/null
  echo "Fixed user $username"
}

fix_user customer1 123456
fix_user staff1 123456
fix_user admin1 123456

echo ""
echo "Test token:"
curl -sS -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=sam-shop-ui&username=customer1&password=123456&grant_type=password" | python3 -m json.tool | head -5

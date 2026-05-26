#!/usr/bin/env bash
# Fix "Account is not fully set up" — disable realm required actions + reset users
set -euo pipefail

KEYCLOAK_URL="${KEYCLOAK_URL:-http://127.0.0.1:8180}"
KEYCLOAK_URL="${KEYCLOAK_URL%/}"
ADMIN_USER="${KC_ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${KC_ADMIN_PASSWORD:-admin}"
REALM="${KEYCLOAK_REALM:-sam-shop}"

echo "Keycloak: $KEYCLOAK_URL"

# Prefer kcadm inside container (most reliable on Azure VM)
if docker ps --format '{{.Names}}' 2>/dev/null | grep -qx 'sam-identity'; then
  echo "Using kcadm in container sam-identity..."
  docker exec sam-identity /opt/keycloak/bin/kcadm.sh config credentials \
    --server http://localhost:8080 --realm master \
    --user "$ADMIN_USER" --password "$ADMIN_PASSWORD" >/dev/null 2>&1 || true

  for action in VERIFY_EMAIL UPDATE_PASSWORD UPDATE_PROFILE CONFIGURE_TOTP terms_and_conditions; do
    docker exec sam-identity /opt/keycloak/bin/kcadm.sh update "authentication/required-actions/${action}" \
      -r "$REALM" -s enabled=false -s defaultAction=false 2>/dev/null || true
  done

  fix_kcadm() {
    local username=$1 password=$2
    docker exec sam-identity /opt/keycloak/bin/kcadm.sh update users \
      -r "$REALM" -q "username=$username" \
      -s enabled=true -s emailVerified=true -s 'requiredActions=[]'
    docker exec sam-identity /opt/keycloak/bin/kcadm.sh set-password \
      -r "$REALM" --username "$username" --new-password "$password"
    echo "Fixed (kcadm) $username"
  }

  fix_kcadm customer1 123456
  fix_kcadm staff1 123456
  fix_kcadm admin1 123456
else
  echo "Container sam-identity not found — using Admin REST API..."
  TOKEN=$(curl -fsS -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "client_id=admin-cli&username=${ADMIN_USER}&password=${ADMIN_PASSWORD}&grant_type=password" \
    | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
  AUTH=(-H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json")

  for action in VERIFY_EMAIL UPDATE_PASSWORD UPDATE_PROFILE CONFIGURE_TOTP terms_and_conditions; do
    curl -fsS -X PUT "${AUTH[@]}" \
      -d '{"alias":"'"$action"'","enabled":false,"defaultAction":false}' \
      "$KEYCLOAK_URL/admin/realms/$REALM/authentication/required-actions/$action" 2>/dev/null || true
  done

  fix_api() {
    local username=$1 password=$2
    local users user_id body
    users=$(curl -fsS "${AUTH[@]}" "$KEYCLOAK_URL/admin/realms/$REALM/users?username=${username}&exact=true")
    user_id=$(echo "$users" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d[0]['id'] if d else '')")
    [ -n "$user_id" ] || { echo "User $username not found"; return; }

    body=$(curl -fsS "${AUTH[@]}" "$KEYCLOAK_URL/admin/realms/$REALM/users/$user_id" | python3 -c "
import sys, json
u = json.load(sys.stdin)
allowed = ('username','email','firstName','lastName','enabled','emailVerified','attributes','requiredActions')
out = {k: u[k] for k in allowed if k in u}
out['enabled'] = True
out['emailVerified'] = True
out['requiredActions'] = []
print(json.dumps(out))
")
    curl -fsS -X PUT "${AUTH[@]}" -d "$body" \
      "$KEYCLOAK_URL/admin/realms/$REALM/users/$user_id" >/dev/null
    curl -fsS -X PUT "${AUTH[@]}" \
      -d "{\"type\":\"password\",\"value\":\"$password\",\"temporary\":false}" \
      "$KEYCLOAK_URL/admin/realms/$REALM/users/$user_id/reset-password" >/dev/null
    curl -fsS -X PUT "${AUTH[@]}" -d '{"requiredActions":[]}' \
      "$KEYCLOAK_URL/admin/realms/$REALM/users/$user_id" >/dev/null
    echo "Fixed (api) $username"
  }

  fix_api customer1 123456
  fix_api staff1 123456
  fix_api admin1 123456
fi

echo ""
echo "Test token:"
RESP=$(curl -sS -w "\n%{http_code}" -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=sam-shop-ui&username=customer1&password=123456&grant_type=password")
CODE=$(echo "$RESP" | tail -n1)
BODY=$(echo "$RESP" | sed '$d')
echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
if [ "$CODE" = "200" ]; then
  echo "OK — login should work at http://20.24.185.233:4200"
else
  echo "HTTP $CODE — still failing. Try delete/recreate users (see docs)."
fi

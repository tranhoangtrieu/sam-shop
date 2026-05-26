#!/usr/bin/env bash
# Configure Keycloak realm sam-shop (bash — for Azure VM without PowerShell)
# Usage on VM:
#   export KEYCLOAK_URL=http://127.0.0.1:8180
#   export FE_PUBLIC_URL=http://20.24.185.233:4200
#   export KC_ADMIN_PASSWORD=admin
#   bash identity-service/keycloak/configure-sam-shop.sh

set -euo pipefail

KEYCLOAK_URL="${KEYCLOAK_URL:-http://127.0.0.1:8180}"
KEYCLOAK_URL="${KEYCLOAK_URL%/}"
ADMIN_USER="${KC_ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${KC_ADMIN_PASSWORD:-admin}"
REALM="${KEYCLOAK_REALM:-sam-shop}"
FE_URL="${FE_PUBLIC_URL:-http://localhost:4200}"
FE_URL="${FE_URL%/}"

echo "Keycloak: $KEYCLOAK_URL"
echo "Frontend URL: $FE_URL"

echo "Waiting for Keycloak..."
for i in $(seq 1 60); do
  if curl -fsS -o /dev/null "$KEYCLOAK_URL/realms/master" 2>/dev/null; then
    break
  fi
  sleep 3
done
curl -fsS -o /dev/null "$KEYCLOAK_URL/realms/master" || {
  echo "Keycloak not ready at $KEYCLOAK_URL"
  exit 1
}

TOKEN=$(curl -fsS -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=admin-cli&username=${ADMIN_USER}&password=${ADMIN_PASSWORD}&grant_type=password" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

AUTH=(-H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json")

kc() {
  local method=$1 path=$2
  shift 2
  local data="${1:-}"
  if [ -n "$data" ]; then
    curl -fsS -X "$method" "${AUTH[@]}" -d "$data" "$KEYCLOAK_URL/admin$path"
  else
    curl -fsS -X "$method" "${AUTH[@]}" "$KEYCLOAK_URL/admin$path"
  fi
}

# 1. Realm
if curl -fsS -o /dev/null "${AUTH[@]}" "$KEYCLOAK_URL/admin/realms/$REALM" 2>/dev/null; then
  echo "Realm $REALM exists — updating"
  kc PUT "/realms/$REALM" '{"enabled":true,"registrationAllowed":false,"loginWithEmailAllowed":true,"resetPasswordAllowed":true,"verifyEmail":false,"sslRequired":"none"}' >/dev/null
else
  echo "Creating realm $REALM"
  kc POST "/realms" "{\"realm\":\"$REALM\",\"enabled\":true,\"displayName\":\"Sam Shop\",\"registrationAllowed\":false,\"loginWithEmailAllowed\":true,\"duplicateEmailsAllowed\":false,\"resetPasswordAllowed\":true,\"verifyEmail\":false,\"editUsernameAllowed\":false,\"sslRequired\":\"none\"}" >/dev/null
fi

# 2. Roles
for role in USER EMPLOYEE ADMIN; do
  if curl -fsS -o /dev/null "${AUTH[@]}" "$KEYCLOAK_URL/admin/realms/$REALM/roles/$role" 2>/dev/null; then
    echo "Role $role exists"
  else
    kc POST "/realms/$REALM/roles" "{\"name\":\"$role\"}" >/dev/null
    echo "Created role $role"
  fi
done

# 3. Client
CLIENT_JSON=$(kc GET "/realms/$REALM/clients?clientId=sam-shop-ui")
CLIENT_ID=$(echo "$CLIENT_JSON" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d[0]['id'] if d else '')")

if [ -z "$CLIENT_ID" ]; then
  kc POST "/realms/$REALM/clients" "{\"clientId\":\"sam-shop-ui\",\"name\":\"Sam Shop UI\",\"enabled\":true,\"publicClient\":true,\"directAccessGrantsEnabled\":true,\"standardFlowEnabled\":true,\"rootUrl\":\"$FE_URL\",\"baseUrl\":\"$FE_URL\",\"redirectUris\":[\"$FE_URL/*\"],\"webOrigins\":[\"$FE_URL\",\"+\"],\"protocol\":\"openid-connect\"}" >/dev/null
  CLIENT_JSON=$(kc GET "/realms/$REALM/clients?clientId=sam-shop-ui")
  CLIENT_ID=$(echo "$CLIENT_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin)[0]['id'])")
  echo "Created client sam-shop-ui"
else
  kc PUT "/realms/$REALM/clients/$CLIENT_ID" "{\"clientId\":\"sam-shop-ui\",\"enabled\":true,\"publicClient\":true,\"directAccessGrantsEnabled\":true,\"standardFlowEnabled\":true,\"rootUrl\":\"$FE_URL\",\"redirectUris\":[\"$FE_URL/*\"],\"webOrigins\":[\"$FE_URL\",\"+\"]}" >/dev/null
  echo "Updated client sam-shop-ui"
fi

# 4. userId mapper
MAPPERS=$(kc GET "/realms/$REALM/clients/$CLIENT_ID/protocol-mappers/models")
if ! echo "$MAPPERS" | python3 -c "import sys,json; print(any(m.get('name')=='userId-mapper' for m in json.load(sys.stdin)))" | grep -q True; then
  kc POST "/realms/$REALM/clients/$CLIENT_ID/protocol-mappers/models" '{"name":"userId-mapper","protocol":"openid-connect","protocolMapper":"oidc-usermodel-attribute-mapper","config":{"user.attribute":"userId","claim.name":"userId","jsonType.label":"long","id.token.claim":"true","access.token.claim":"true","userinfo.token.claim":"true"}}' >/dev/null
  echo "Created userId protocol mapper"
fi

# 5. Users: username|email|userId|role|password
clear_required_actions() {
  local user_id=$1
  kc PUT "/realms/$REALM/users/$user_id" '{"requiredActions":[]}' >/dev/null
}

setup_user() {
  local username=$1 email=$2 uid=$3 role=$4 password=$5
  local users user_id role_json
  users=$(kc GET "/realms/$REALM/users?username=${username}&exact=true")
  user_id=$(echo "$users" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d[0]['id'] if d else '')")
  if [ -z "$user_id" ]; then
    kc POST "/realms/$REALM/users" "{\"username\":\"$username\",\"email\":\"$email\",\"enabled\":true,\"emailVerified\":true,\"requiredActions\":[],\"attributes\":{\"userId\":[\"$uid\"]}}" >/dev/null
    users=$(kc GET "/realms/$REALM/users?username=${username}&exact=true")
    user_id=$(echo "$users" | python3 -c "import sys,json; print(json.load(sys.stdin)[0]['id'])")
    echo "Created user $username"
  else
    kc PUT "/realms/$REALM/users/$user_id" "{\"email\":\"$email\",\"enabled\":true,\"emailVerified\":true,\"requiredActions\":[],\"attributes\":{\"userId\":[\"$uid\"]}}" >/dev/null
    echo "Updated user $username"
  fi
  kc PUT "/realms/$REALM/users/$user_id/reset-password" "{\"type\":\"password\",\"value\":\"$password\",\"temporary\":false}" >/dev/null
  clear_required_actions "$user_id"
  role_json=$(kc GET "/realms/$REALM/roles/$role")
  kc POST "/realms/$REALM/users/$user_id/role-mappings/realm" "[$role_json]" >/dev/null
  echo "  Assigned role $role"
}

setup_user customer1 customer1@samshop.local 1 USER 123456
setup_user staff1 staff1@samshop.local 2 EMPLOYEE 123456
setup_user admin1 admin1@samshop.local 3 ADMIN 123456

echo ""
echo "=== Keycloak sam-shop configured ==="
echo "App: $FE_URL"
echo "Login: customer1 / 123456"

echo "Verifying login token..."
TOKEN_RESP=$(curl -sS -w "\n%{http_code}" -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=sam-shop-ui&username=customer1&password=123456&grant_type=password")
HTTP_CODE=$(echo "$TOKEN_RESP" | tail -n1)
BODY=$(echo "$TOKEN_RESP" | sed '$d')
if [ "$HTTP_CODE" != "200" ]; then
  echo "WARNING: token test failed (HTTP $HTTP_CODE): $BODY"
  echo "Try: bash identity-service/keycloak/fix-keycloak-users.sh"
  exit 0
fi
echo "Sample JWT payload:"
echo "$BODY" | python3 -c "
import sys, json, base64
t = json.load(sys.stdin)['access_token']
p = t.split('.')[1]
p += '=' * ((4 - len(p) % 4) % 4)
print(base64.urlsafe_b64decode(p).decode())
"

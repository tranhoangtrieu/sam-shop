#!/usr/bin/env bash
# Run ON the Azure VM: bash scripts/azure-vm-check.sh
set -euo pipefail

echo "=== 1. Local HTTP (inside VM) ==="
for port in 4200 8180 8088; do
  if curl -fsS -o /dev/null -m 5 "http://127.0.0.1:${port}/" 2>/dev/null || \
     curl -fsS -o /dev/null -m 5 "http://127.0.0.1:${port}/actuator/health" 2>/dev/null; then
    echo "  OK  port $port responds locally"
  else
    code=$(curl -s -o /dev/null -w "%{http_code}" -m 5 "http://127.0.0.1:${port}/" 2>/dev/null || echo "fail")
    echo "  ??  port $port local check: $code (gateway may need /actuator/health)"
  fi
done

echo ""
echo "=== 2. Docker ==="
docker compose ps 2>/dev/null || docker-compose ps

echo ""
echo "=== 3. Listening ports ==="
ss -tlnp 2>/dev/null | grep -E ':4200|:8180|:8088' || netstat -tlnp 2>/dev/null | grep -E '4200|8180|8088' || true

echo ""
echo "=== 4. PUBLIC_HOST in .env ==="
grep -E '^PUBLIC_HOST=' .env 2>/dev/null || echo "  (no .env or PUBLIC_HOST missing)"

echo ""
echo "=== 5. UFW (if enabled) ==="
if command -v ufw >/dev/null 2>&1; then
  sudo ufw status || true
else
  echo "  ufw not installed"
fi

echo ""
echo "If local OK but browser from Internet fails → open Azure NSG ports 4200 and 8180."
echo "App URL: http://$(curl -fsS ifconfig.me 2>/dev/null || echo PUBLIC_IP):4200"

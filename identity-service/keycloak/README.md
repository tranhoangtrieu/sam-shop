# Keycloak — realm `sam-shop`

**Giải thích chi tiết script setup:** [CONFIGURE_SAM_SHOP.md](./CONFIGURE_SAM_SHOP.md) (công việc từng bước, lý do thiết kế, trade-off).

## Đã cấu hình

| Mục | Giá trị |
|-----|---------|
| Realm | `sam-shop` |
| Client | `sam-shop-ui` (public, Direct Access Grants) |
| Issuer | `http://localhost:8180/realms/sam-shop` |
| Roles | `USER`, `EMPLOYEE`, `ADMIN` |
| Claim JWT | `userId` (long), `realm_access.roles` |

## Tài khoản dev

### Đăng ký / tạo user không qua Admin Console

- **Khách hàng:** form `/register` → `POST /api/auth/register` (role `USER` tự động).
- **Admin:** `/admin/users` → `POST /api/admin/users` (chọn `USER` / `EMPLOYEE` / `ADMIN`).
- Service: `user-service` gọi Keycloak Admin API (credentials `KC_ADMIN_*` trong docker-compose).

| Username | Password | Role | userId |
|----------|----------|------|--------|
| `customer1` | `123456` | USER | 1 |
| `staff1` | `123456` | EMPLOYEE | 2 |
| `admin1` | `123456` | ADMIN | 3 |

## Admin Console

- URL: http://localhost:8180/admin/master/console/#/sam-shop
- User admin Keycloak: `admin` / `admin` (từ `.env` root)

## Chạy lại cấu hình

```powershell
# 1. Khởi động Keycloak
docker compose up -d postgres-keycloak identity-service

# 2. Script tự động (realm, client, mapper, roles)
powershell -ExecutionPolicy Bypass -File identity-service/keycloak/configure-sam-shop.ps1

# 3. Hoàn tất user/role (nếu script lỗi JSON — dùng kcadm trong container)
docker exec sam-identity /opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080 --realm master --user admin --password admin
# ... xem lịch sử commit / hướng dẫn trong README gốc
```

## Kiểm tra token

```http
POST http://localhost:8180/realms/sam-shop/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=password&client_id=sam-shop-ui&username=customer1&password=123456
```

JWT phải có `"userId": 1` và `"roles": ["USER", ...]`.

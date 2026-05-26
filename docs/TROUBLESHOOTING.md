# Sam Shop — Ghi chú xử lý lỗi (Troubleshooting)

## 1. Đăng nhập Keycloak: "Invalid user credentials"

| Nguyên nhân | Cách xử lý |
|-------------|------------|
| Sai username (`admin` thay vì `admin1`) | Dùng `customer1`, `staff1`, `admin1` |
| Khoảng trắng thừa trong username | Frontend đã trim; nhập lại chính xác |
| Required actions chưa xóa | Chạy lại script Keycloak trong `identity-service/keycloak/` |
| Mở URL token bằng trình duyệt (GET) | Token endpoint chỉ nhận **POST** — không mở URL trực tiếp |

## 2. API trả 401 dù đã gửi Bearer token (trong Docker)

**Nguyên nhân:** Service trong container không lấy được JWK từ `http://localhost:8180` (localhost trong container ≠ Keycloak).

**Cách xử lý:** `docker-compose.yml` đã cấu hình:

```yaml
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: http://localhost:8180/realms/sam-shop
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI: http://identity-service:8080/realms/sam-shop/protocol/openid-connect/certs
```

- `issuer-uri`: khớp claim `iss` trong JWT (client lấy token từ host).
- `jwk-set-uri`: tải public key từ Keycloak **trong Docker network**.

## 3. API trả 404 khi có JWT (cart / order / payment)

**Nguyên nhân chính:** Image Docker cũ chỉ chứa class `*Application.class`, **không có Controller** (build/cache lỗi thời điểm đầu).

**Triệu chứng:**
- Không token → 401
- Có token hợp lệ → 404 `{"error":"Not Found","path":"/api/cart/add"}`

**Cách xử lý:**

```powershell
docker compose build cart-service order-service payment-service --no-cache
docker compose up -d cart-service order-service payment-service
```

Kiểm tra JAR có controller:

```powershell
docker exec sam-cart unzip -l /app/app.jar | findstr CartController
```

## 4. GET `/api/products` trả 401 hoặc 500

| Lỗi | Nguyên nhân | Sửa |
|-----|-------------|-----|
| 401 | Pattern `/api/products/**` không khớp `/api/products` | Thêm `/api/products` vào `permitAll` (gateway + product) |
| 500 `lower(bytea)` | Query search với `search=null` trên PostgreSQL | Tách query trong `ProductServiceImpl` |

## 5. Tạo đơn hàng 500 — clear cart Feign sai URL

**Lỗi:** `DELETE /api/cart/internal/1` thay vì `/api/cart/internal/1/clear`

**Sửa:** `order-service/.../CartClient.java` → `@DeleteMapping("/api/cart/internal/{userId}/clear")`

## 6. Payment 403 — userId không khớp

**Nguyên nhân:** `payment-service` dùng `jwt.getSubject()` (UUID Keycloak) thay vì claim `userId` (1, 2, 3).

**Sửa:** `SecurityUtils.getCurrentUserId()` đọc claim `userId` giống cart/order.

## 7. Đơn ONLINE vẫn `paymentStatus=UNPAID` sau callback

**Nguyên nhân:** Payment cập nhật PAID nhưng không gọi order-service.

**Sửa:** `payment-service` gọi `PUT /api/orders/internal/{id}/payment-status` sau COD confirm và Online callback thành công.

## 8. Eureka không đăng ký (log `localhost:8761 Connection refused`)

Trong container, Eureka client phải trỏ `discovery-service:8761`. Biến môi trường trong compose:

`EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://discovery-service:8761/eureka/`

`application.yaml` dùng placeholder `${EUREKA_CLIENT_SERVICEURL_DEFAULTZONE:...}`.

## 9. Frontend Docker — API 404

**Đã sửa:** `sam-shop-frontend/nginx.conf` proxy `/api/` → `http://gateway-service:8088/api/`.  
Production build dùng `apiUrl: ''` (relative) — request `/api/...` đi qua nginx tới Gateway.

Rebuild frontend: `docker compose build frontend && docker compose up -d frontend`

## Chạy lại test E2E

```powershell
docker compose up -d
powershell -ExecutionPolicy Bypass -File scripts/run-e2e-tests.ps1
```

Kết quả: `docs/TEST_RESULTS.md`

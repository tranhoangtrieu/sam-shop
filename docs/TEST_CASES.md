# Sam Shop — Test Cases (E2E API)

Tài liệu test case cho kiểm thử chức năng qua **API Gateway** (`http://localhost:8088`) và **Keycloak** (`http://localhost:8180`).

## Tiền điều kiện

| # | Điều kiện |
|---|-----------|
| P-01 | `docker compose up -d` — toàn bộ stack đang chạy |
| P-02 | Keycloak realm `sam-shop`, client `sam-shop-ui` đã cấu hình |
| P-03 | User dev: `customer1` / `staff1` / `admin1`, password `123456` |
| P-04 | Product service đã seed ≥ 1 sản phẩm |

## Tài khoản test

| Username | Password | Role | userId (JWT) |
|----------|----------|------|--------------|
| customer1 | 123456 | USER | 1 |
| staff1 | 123456 | EMPLOYEE | 2 |
| admin1 | 123456 | ADMIN | 3 |

## Chạy test tự động

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-e2e-tests.ps1
```

Kết quả ghi vào `docs/TEST_RESULTS.md`.

---

## 1. Authentication (Keycloak)

| ID | Mô tả | Steps | Expected |
|----|-------|-------|----------|
| AUTH-01 | Đăng nhập USER | POST token, username=customer1 | 200, có `access_token` |
| AUTH-02 | Đăng nhập EMPLOYEE | POST token, username=staff1 | 200, có `access_token` |
| AUTH-03 | Đăng nhập ADMIN | POST token, username=admin1 | 200, có `access_token` |
| AUTH-04 | Sai mật khẩu | POST token, password sai | 400/401, không có token |

---

## 2. Product Service

| ID | Mô tả | Method | Path | Auth | Expected |
|----|-------|--------|------|------|----------|
| PRD-01 | Danh sách SP | GET | `/api/products` | Không | 200, `success=true`, có data |
| PRD-02 | Chi tiết SP | GET | `/api/products/{id}` | Không | 200, đúng id |
| PRD-03 | Tìm kiếm | GET | `/api/products?search=Tai` | Không | 200 |
| PRD-04 | Tạo SP | POST | `/api/products` | EMPLOYEE | 200, SP mới |
| PRD-05 | Cập nhật SP | PUT | `/api/products/{id}` | EMPLOYEE | 200, giá/name đổi |
| PRD-06 | Danh mục | GET | `/api/products/categories` | Không | 200, có categories |
| PRD-07 | Tạo danh mục | POST | `/api/products/categories` | EMPLOYEE | 200 |
| PRD-08 | Điều chỉnh tồn | PUT | `/api/products/{id}/stock` | Không | 200, quantity thay đổi |
| PRD-09 | Xóa SP | DELETE | `/api/products/{id}` | ADMIN | 200 |

---

## 3. Cart Service

| ID | Mô tả | Method | Path | Auth | Expected |
|----|-------|--------|------|------|----------|
| CRT-01 | Thêm giỏ | POST | `/api/cart/add` | USER | 200, có items |
| CRT-02 | Xem giỏ | GET | `/api/cart/1` | USER (userId=1) | 200, userId=1 |
| CRT-03 | Cập nhật SL | PUT | `/api/cart/update` | USER | 200, quantity đổi |
| CRT-04 | Thêm giỏ (flow Online) | POST | `/api/cart/add` | USER | 200 |
| CRT-05 | Xóa dòng giỏ | DELETE | `/api/cart/remove/{itemId}` | USER | 200 |

---

## 4. Order Service — COD

| ID | Mô tả | Method | Path | Auth | Expected |
|----|-------|--------|------|------|----------|
| ORD-01 | Tạo đơn COD | POST | `/api/orders` | USER | status=PENDING, payment=UNPAID |
| ORD-02 | Đơn của tôi | GET | `/api/orders` | USER | Có đơn vừa tạo |
| ORD-03 | Chi tiết đơn | GET | `/api/orders/{id}` | USER | 200 |
| ORD-04 | Xác nhận đơn | PUT | `/api/orders/{id}/confirm` | EMPLOYEE | status=CONFIRMED |
| ORD-05 | Giao hàng | PUT | `/api/orders/{id}/status` | EMPLOYEE | status=SHIPPING |
| ORD-06 | Hoàn tất | PUT | `/api/orders/{id}/status` | EMPLOYEE | status=COMPLETED |
| ORD-07 | Tất cả đơn | GET | `/api/orders/all` | EMPLOYEE | 200, danh sách |
| ORD-08 | Doanh thu | GET | `/api/orders/revenue` | ADMIN | 200, có totalRevenue |

Body tạo đơn:
```json
{ "shippingAddress": "123 Test St", "paymentMethod": "COD" }
```

---

## 5. Payment Service

| ID | Mô tả | Method | Path | Auth | Expected |
|----|-------|--------|------|------|----------|
| PAY-01 | Payment theo order | GET | `/api/payments/order/{orderId}` | USER | paymentMethod=COD |
| PAY-02 | Xác nhận COD | PUT | `/api/payments/{id}/confirm-cod` | EMPLOYEE | paymentStatus=PAID |
| PAY-03 | Khởi tạo Online | POST | `/api/payments/online/initiate` | USER | có paymentUrl |
| PAY-04 | Callback Online | POST | `/api/payments/online/callback` | USER | paymentStatus=PAID |

---

## 6. Order Service — ONLINE

| ID | Mô tả | Expected |
|----|-------|----------|
| ORD-09 | Tạo đơn ONLINE | 200, có orderId |
| ORD-10 | Sau callback | order.paymentStatus=PAID |

Body tạo đơn:
```json
{ "shippingAddress": "456 Online Ave", "paymentMethod": "ONLINE" }
```

---

## 7. Security / Negative

| ID | Mô tả | Expected |
|----|-------|----------|
| SEC-01 | USER gọi revenue | 403 Forbidden |
| SEC-02 | Không token thêm giỏ | 401 Unauthorized |

---

## Luồng nghiệp vụ end-to-end

### Luồng A — COD
```
Login USER → Add cart → Create order COD → Staff confirm → SHIPPING → COMPLETED → Staff confirm COD PAID → Admin xem revenue
```

### Luồng B — ONLINE
```
Login USER → Add cart → Create order ONLINE → Initiate → Callback success → Verify order PAID
```

---

## Import Postman

Collection: `postman/Sam-Shop-API.postman_collection.json`

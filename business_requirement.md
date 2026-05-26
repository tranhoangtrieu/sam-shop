# Sam Shop — Tài liệu yêu cầu nghiệp vụ (Business Requirements)

## 1. Tổng quan dự án

**Sam Shop** là hệ thống thương mại điện tử (e-commerce) xây dựng theo kiến trúc **microservices**, phục vụ hai nhóm người dùng chính:

- **Khách hàng (USER):** xem sản phẩm, mua hàng, thanh toán, theo dõi đơn hàng.
- **Nhân viên vận hành (EMPLOYEE):** quản lý sản phẩm, xử lý đơn hàng.
- **Quản trị viên (ADMIN):** toàn quyền hệ thống, quản lý người dùng, xem báo cáo doanh thu.

Hệ thống gồm **frontend Angular**, **API Gateway**, các **microservice nghiệp vụ**, **Eureka Discovery**, và **Keycloak** làm dịch vụ xác thực/ủy quyền tập trung.

---

## 2. Mục tiêu kinh doanh

| Mục tiêu | Mô tả |
|----------|--------|
| Bán hàng trực tuyến | Cho phép khách đặt mua sản phẩm qua web, thanh toán và nhận hàng. |
| Quản lý kho & danh mục | Nhân viên/admin cập nhật sản phẩm, tồn kho, danh mục. |
| Quản lý đơn hàng | Theo dõi vòng đời đơn từ tạo → xác nhận → giao hàng → hoàn tất / hủy. |
| Bảo mật & phân quyền | Đăng nhập OAuth2/OIDC (Keycloak), phân quyền theo vai trò. |
| Mở rộng | Kiến trúc tách service để scale và bảo trì độc lập từng domain. |

---

## 3. Phạm vi (Scope)

### 3.1 Trong phạm vi (In scope)

- Đăng nhập / đăng xuất qua Keycloak (realm `sam-shop`).
- CRUD sản phẩm và danh mục (admin/employee).
- Giỏ hàng theo từng người dùng.
- Tạo đơn hàng từ giỏ, cập nhật trạng thái đơn.
- Thanh toán đơn hàng theo **2 phương thức**: **COD** (thanh toán khi nhận hàng) và **Online** (thanh toán trực tuyến).
- Tra cứu đơn hàng (khách xem đơn của mình; nhân viên/admin xem tất cả).
- Báo cáo doanh thu cơ bản (admin).
- API Gateway làm điểm vào duy nhất cho client.
- Service discovery (Eureka).
- Triển khai Docker: **mỗi service một container**, **mỗi service có DB một container PostgreSQL riêng** (trừ gateway/discovery).

### 3.2 Ngoài phạm vi (Out of scope — giai đoạn hiện tại)

- Tích hợp cổng thanh toán thật (VNPay, MoMo, Stripe) — Phase 1 chỉ **mô phỏng** luồng Online; COD xử lý nội bộ.
- Giao hàng / logistics thực tế (đối tác vận chuyển).
- Đánh giá sản phẩm, khuyến mãi, mã giảm giá phức tạp.
- Đa ngôn ngữ / đa tiền tệ.
- Ứng dụng mobile native.

---

## 4. Đối tượng sử dụng (Actors)

| Vai trò | Mã | Mô tả |
|---------|-----|--------|
| Khách hàng | `USER` | Mua sắm, quản lý giỏ hàng và đơn hàng của bản thân. |
| Nhân viên | `EMPLOYEE` | Quản lý sản phẩm, xử lý đơn (xác nhận, cập nhật trạng thái giao hàng). |
| Quản trị viên | `ADMIN` | Toàn quyền: sản phẩm, đơn hàng, người dùng, dashboard doanh thu. |

**Xác thực:** Keycloak realm `sam-shop`, client `sam-shop-ui`. JWT chứa `realm_access.roles` với một trong các role trên; claim tùy chỉnh `userId` (số) dùng để map với dữ liệu nghiệp vụ.

---

## 5. Kiến trúc hệ thống (tham chiếu kỹ thuật)

### 5.1 Nguyên tắc triển khai (bắt buộc)

| Nguyên tắc | Mô tả |
|------------|--------|
| **1 service = 1 container** | Mỗi microservice chạy trong **một Docker container riêng**, build từ `Dockerfile` riêng, lifecycle độc lập (start/stop/scale). |
| **1 service = 1 database** | Mỗi service sở hữu **một PostgreSQL database riêng**; **không** dùng chung database, **không** JOIN cross-DB. |
| Giao tiếp giữa service | Chỉ qua **HTTP/REST** (Gateway hoặc Feign); đồng bộ dữ liệu bằng API và ID tham chiếu (`productId`, `orderId`, `userId`). |
| Identity tách biệt | `identity-service` (Keycloak) có **container + database riêng**, không trộn với dữ liệu nghiệp vụ. |

### 5.2 Sơ đồ triển khai Docker (mục tiêu)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Docker network: sam-shop-net                            │
├─────────────────────────────────────────────────────────────────────────────┤
│  [sam-shop-frontend]     (container hoặc ng serve local)                     │
│         │                                                                      │
│         ├──► [gateway-service]          :8088                                  │
│         │         │                                                            │
│         │         ├──► [discovery-service]   :8761  (Eureka, không cần DB)   │
│         │         ├──► [product-service]     :8081 ──► [postgres-product]    │
│         │         ├──► [cart-service]        :8082 ──► [postgres-cart]        │
│         │         ├──► [order-service]       :8083 ──► [postgres-order]       │
│         │         └──► [payment-service]     :8084 ──► [postgres-payment]     │
│         │                                                                      │
│         └──► [identity-service]  Keycloak :8180 ──► [postgres-keycloak]       │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.3 Bảng container & database (chuẩn dự án)

| # | Application container | Image / build | Port app (host) | Database container | Database name | Port DB (host) |
|---|------------------------|---------------|-----------------|--------------------|-----------------|----------------|
| 1 | `sam-discovery` | `discovery-service/Dockerfile` | `8761` | — | — | — |
| 2 | `sam-gateway` | `gateway-service/Dockerfile` | `8088` | — | — | — |
| 3 | `sam-identity` | `identity-service/Dockerfile` | `8180` | `sam-postgres-keycloak` | `keycloak` | `5433` |
| 4 | `sam-product` | `product-service/Dockerfile` | `8081` | `sam-postgres-product` | `product_db` | `5434` |
| 5 | `sam-cart` | `cart-service/Dockerfile` | `8082` | `sam-postgres-cart` | `cart_db` | `5435` |
| 6 | `sam-order` | `order-service/Dockerfile` | `8083` | `sam-postgres-order` | `order_db` | `5436` |
| 7 | `sam-payment` | `payment-service/Dockerfile` | `8084` | `sam-postgres-payment` | `payment_db` | `5437` |
| 8 | `sam-frontend` | `sam-shop-frontend/Dockerfile` (tùy chọn) | `4200` | — | — | — |

**Tổng cộng:** 7 container ứng dụng + **5 container PostgreSQL** (mỗi DB phục vụ đúng một service có persistence).

> `discovery-service` và `gateway-service` **không** có database riêng.  
> Các service còn lại **bắt buộc** có đúng một Postgres container đi kèm.

### 5.4 Quy ước kết nối database (trong Docker)

Mỗi Spring service kết nối DB qua **hostname = tên database container** (Docker DNS), ví dụ:

| Service | Biến môi trường (ví dụ) | JDBC URL (trong Docker network) |
|---------|-------------------------|----------------------------------|
| product-service | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://sam-postgres-product:5432/product_db` |
| cart-service | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://sam-postgres-cart:5432/cart_db` |
| order-service | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://sam-postgres-order:5432/order_db` |
| payment-service | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://sam-postgres-payment:5432/payment_db` |
| identity-service | `KC_DB_URL` | `jdbc:postgresql://sam-postgres-keycloak:5432/keycloak` |

**Credential mặc định (dev):** user `postgres`, password `123456` — dùng chung user trên mọi DB container; **database name khác nhau**.

**Volume:** mỗi `sam-postgres-*` gắn **named volume riêng** (`product_data`, `cart_data`, …) để dữ liệu không bị trộn khi recreate container app.

### 5.5 Trách nhiệm từng service

| Service | Container | Database | Trách nhiệm nghiệp vụ |
|---------|-----------|----------|------------------------|
| `identity-service` | `sam-identity` | `keycloak` | Keycloak — đăng nhập, token, realm roles |
| `discovery-service` | `sam-discovery` | — | Đăng ký & tìm địa chỉ microservice (Eureka) |
| `gateway-service` | `sam-gateway` | — | Routing, CORS, validate JWT, phân quyền endpoint |
| `product-service` | `sam-product` | `product_db` | Sản phẩm, danh mục, tồn kho |
| `cart-service` | `sam-cart` | `cart_db` | Giỏ hàng theo `userId` |
| `order-service` | `sam-order` | `order_db` | Đơn hàng, trạng thái, doanh thu |
| `payment-service` | `sam-payment` | `payment_db` | Ghi nhận thanh toán theo đơn |
| `sam-shop-frontend` | `sam-frontend` | — | Giao diện người dùng |

### 5.6 Dữ liệu sở hữu theo service (data ownership)

| Database | Bảng / dữ liệu chính (gợi ý) | Không lưu tại DB khác |
|----------|------------------------------|------------------------|
| `product_db` | `products`, `categories` | Chi tiết sản phẩm đầy đủ |
| `cart_db` | `carts`, `cart_items` | Chỉ `productId` + snapshot `productName`, `price` |
| `order_db` | `orders`, `order_items` | Snapshot đơn; `paymentStatus` có thể đồng bộ từ payment |
| `payment_db` | `payments` | `orderId`, `userId`, `amount`, trạng thái thanh toán |
| `keycloak` | users, realms, clients (Keycloak) | Thông tin đăng nhập, roles |

### 5.7 Docker Compose (cấu trúc file gợi ý)

```
sam-shop-microservices/
├── docker-compose.yml          # orchestration toàn hệ thống
├── .env                        # port, password chung
├── identity-service/
│   ├── Dockerfile
│   └── docker-compose.yml      # có thể chạy độc lập (dev)
├── product-service/Dockerfile
├── cart-service/Dockerfile
├── order-service/Dockerfile
├── payment-service/Dockerfile
├── gateway-service/Dockerfile
├── discovery-service/Dockerfile
└── sam-shop-frontend/Dockerfile
```

**Thứ tự khởi động (`depends_on`):**

1. Tất cả `sam-postgres-*` (healthcheck `pg_isready`)
2. `sam-discovery` → `sam-identity` (Keycloak)
3. `sam-product`, `sam-cart`, `sam-order`, `sam-payment` (sau DB + Eureka)
4. `sam-gateway` (sau các service đã register Eureka)
5. `sam-frontend` (sau gateway)

---

## 6. Yêu cầu chức năng theo module

### 6.1 Xác thực & người dùng (Identity / Auth)

| ID | Yêu cầu | Chi tiết |
|----|---------|----------|
| AUTH-01 | Đăng nhập | User nhập username/password; hệ thống trả JWT từ Keycloak (grant type password cho dev). |
| AUTH-02 | Đăng xuất | Xóa token phía client, chuyển về trang login. |
| AUTH-03 | Phân quyền | Mọi API nghiệp vụ (trừ public) yêu cầu Bearer token hợp lệ. |
| AUTH-04 | Đăng ký | Giai đoạn 1: tạo user qua Keycloak Admin hoặc bật self-registration trên realm; không bắt buộc API register riêng. |
| AUTH-05 | Map userId | JWT phải có `userId` (number) đồng bộ với bản ghi nghiệp vụ trong các service. |

### 6.2 Sản phẩm & danh mục (Product Service)

| ID | Yêu cầu | Actor | Ghi chú |
|----|---------|-------|---------|
| PRD-01 | Xem danh sách sản phẩm | Tất cả (public hoặc authenticated) | Hỗ trợ tìm theo `search`, lọc `categoryId`. |
| PRD-02 | Xem chi tiết sản phẩm | Tất cả | Hiển thị tên, giá, mô tả, ảnh, tồn kho, trạng thái, danh mục. |
| PRD-03 | Tạo sản phẩm | ADMIN, EMPLOYEE | |
| PRD-04 | Cập nhật sản phẩm | ADMIN, EMPLOYEE | |
| PRD-05 | Xóa sản phẩm | ADMIN, EMPLOYEE | Soft-delete hoặc hard-delete tùy triển khai. |
| PRD-06 | Quản lý danh mục | ADMIN, EMPLOYEE | API `/api/products/categories`. |

**Thuộc tính sản phẩm:** `id`, `name`, `price`, `description`, `image`, `quantity` (tồn kho), `status`, `categoryId`, `categoryName`, `createdAt`.

**Trạng thái sản phẩm (gợi ý):** `ACTIVE`, `INACTIVE`, `OUT_OF_STOCK`.

### 6.3 Giỏ hàng (Cart Service)

| ID | Yêu cầu | Actor |
|----|---------|-------|
| CRT-01 | Thêm sản phẩm vào giỏ | USER |
| CRT-02 | Xem giỏ của mình | USER |
| CRT-03 | Cập nhật số lượng item | USER |
| CRT-04 | Xóa item khỏi giỏ | USER |

**Quy tắc:**

- Mỗi `userId` có tối đa một giỏ active.
- `subtotal` = `price × quantity` cho từng dòng; `totalPrice` = tổng các subtotal.
- Khi thêm vào giỏ: kiểm tra tồn kho sản phẩm (`quantity` từ product-service).
- Giá lưu tại thời điểm thêm giỏ (snapshot price) để tránh thay đổi giá ảnh hưởng đơn đã tạo.

### 6.4 Đơn hàng (Order Service)

| ID | Yêu cầu | Actor |
|----|---------|-------|
| ORD-01 | Tạo đơn từ giỏ | USER |
| ORD-02 | Xem đơn của mình | USER |
| ORD-03 | Xem chi tiết một đơn | USER (own), EMPLOYEE, ADMIN (all) |
| ORD-04 | Danh sách tất cả đơn | EMPLOYEE, ADMIN |
| ORD-05 | Xác nhận đơn | EMPLOYEE, ADMIN |
| ORD-06 | Cập nhật trạng thái đơn | EMPLOYEE, ADMIN |
| ORD-07 | Báo cáo doanh thu | ADMIN |

**Dữ liệu đơn hàng:**

- Header: `id`, `userId`, `totalPrice`, `status`, `paymentStatus`, `shippingAddress`, `createdAt`.
- Chi tiết: `items[]` — `productId`, `productName`, `quantity`, `price`.

**Trạng thái đơn (`OrderStatus`):**

| Trạng thái | Ý nghĩa |
|------------|---------|
| `PENDING` | Vừa tạo, chờ xác nhận / thanh toán |
| `CONFIRMED` | Đã xác nhận, chuẩn bị giao |
| `SHIPPING` | Đang giao hàng |
| `COMPLETED` | Hoàn tất |
| `CANCELLED` | Đã hủy |

**Luồng chuyển trạng thái hợp lệ:**

```
PENDING → CONFIRMED → SHIPPING → COMPLETED
PENDING → CANCELLED
CONFIRMED → CANCELLED (nếu chưa giao)
```

**Quy tắc tạo đơn (ORD-01):**

1. Lấy giỏ hiện tại của user; giỏ không được rỗng.
2. Kiểm tra tồn kho từng sản phẩm.
3. Tạo đơn + order items; trừ tồn kho (hoặc reserve — thống nhất một cách trong toàn hệ thống).
4. Gọi payment-service tạo bản ghi thanh toán theo `paymentMethod` (trạng thái ban đầu `UNPAID`).
5. Xóa / làm rỗng giỏ sau khi tạo đơn thành công.
6. `shippingAddress` **bắt buộc**; `paymentMethod` **bắt buộc** — một trong: `COD` hoặc `ONLINE`.

### 6.5 Thanh toán (Payment Service)

Hệ thống hỗ trợ **đúng 2 hình thức thanh toán** khi checkout:

| Phương thức | Mã | Mô tả |
|-------------|-----|--------|
| Thanh toán khi nhận hàng | `COD` | Khách trả tiền mặt/chuyển khoản trực tiếp cho shipper khi nhận hàng. |
| Thanh toán trực tuyến | `ONLINE` | Khách thanh toán qua cổng (Phase 1: mock; Phase 3: VNPay/MoMo/…). |

#### Yêu cầu chức năng

| ID | Yêu cầu | Actor |
|----|---------|-------|
| PAY-01 | Tạo bản ghi thanh toán khi có đơn mới (theo `paymentMethod`) | Hệ thống (internal) |
| PAY-02 | Tra cứu thanh toán theo `orderId` | USER (own order), EMPLOYEE, ADMIN |
| PAY-03 | **COD:** xác nhận đã thu tiền khi giao hàng thành công | EMPLOYEE, ADMIN |
| PAY-04 | **ONLINE:** khởi tạo / xử lý thanh toán trực tuyến (mock gateway Phase 1) | USER, Hệ thống |
| PAY-05 | **ONLINE:** cập nhật `PAID` / `FAILED` theo kết quả cổng thanh toán | Hệ thống (callback/webhook) |
| PAY-06 | Cho phép **thử lại** thanh toán Online nếu `FAILED` và đơn chưa `CANCELLED` | USER |

#### Phương thức thanh toán (`PaymentMethod`)

| Giá trị | Ý nghĩa |
|--------|---------|
| `COD` | Cash on Delivery — thanh toán khi nhận hàng |
| `ONLINE` | Thanh toán qua cổng trực tuyến |

#### Trạng thái thanh toán (`PaymentStatus`)

| Trạng thái | Ý nghĩa | Thường dùng với |
|------------|---------|-----------------|
| `UNPAID` | Chưa thanh toán | COD (đến khi giao) · ONLINE (trước khi pay) |
| `PAID` | Đã thanh toán thành công | COD (sau khi shipper thu tiền) · ONLINE (cổng báo success) |
| `FAILED` | Thanh toán thất bại | Chủ yếu **ONLINE** |
| `REFUNDED` | Đã hoàn tiền | Cả hai (khi hủy/hoàn đơn) |

#### Thuộc tính bản ghi `Payment`

`id`, `orderId`, `userId`, `amount`, `paymentMethod` (`COD` \| `ONLINE`), `paymentStatus`, `transactionCode` (bắt buộc với ONLINE khi `PAID`), `paidAt`, `createdAt`.

#### Luồng theo phương thức

**A. Thanh toán COD**

```
Checkout chọn COD
  → Tạo Order (PENDING) + Payment (COD, UNPAID)
  → EMPLOYEE xác nhận đơn (CONFIRMED) — không yêu cầu PAID trước
  → SHIPPING → COMPLETED
  → EMPLOYEE xác nhận đã thu tiền → Payment PAID, Order.paymentStatus = PAID
```

**B. Thanh toán Online**

```
Checkout chọn ONLINE
  → Tạo Order (PENDING) + Payment (ONLINE, UNPAID)
  → Chuyển sang màn / bước thanh toán (mock URL hoặc API pay)
  → Thành công: Payment PAID (+ transactionCode), Order.paymentStatus = PAID
              → Cho phép EMPLOYEE xác nhận / giao hàng
  → Thất bại: Payment FAILED — khách có thể thử lại (PAY-06)
  → Đơn ONLINE chưa PAID: không chuyển sang COMPLETED
```

#### Quy tắc nghiệp vụ thanh toán

| Mã | Quy tắc |
|----|---------|
| PAY-R01 | `amount` = `totalPrice` của đơn tương ứng. |
| PAY-R02 | Mỗi đơn có **tối đa một** bản ghi payment active (hoặc một chuỗi payment nếu retry ONLINE). |
| PAY-R03 | **COD:** chỉ chuyển `PAID` khi đơn đã `COMPLETED` (hoặc `SHIPPING` — thống nhất khi implement). |
| PAY-R04 | **ONLINE:** phải `PAID` trước khi đơn được coi là sẵn sàng giao (`CONFIRMED` trở đi). |
| PAY-R05 | **ONLINE:** `transactionCode` bắt buộc khi `paymentStatus = PAID`. |
| PAY-R06 | Doanh thu chỉ tính đơn `paymentStatus = PAID` (xem BR-05). |
| PAY-R07 | Phase 1: cổng Online **mock** (API nội bộ giả lập success/fail); Phase 3: tích hợp VNPay/MoMo. |

### 6.6 API Gateway

| ID | Yêu cầu |
|----|---------|
| GW-01 | Route `/api/products/**` → product-service |
| GW-02 | Route `/api/cart/**` → cart-service |
| GW-03 | Route `/api/orders/**` → order-service |
| GW-04 | Route `/api/payments/**` → payment-service |
| GW-05 | Validate JWT Keycloak cho các route protected |
| GW-06 | Trả response chuẩn `ApiResponse<T>`: `{ success, message, data }` |
| GW-07 | CORS cho frontend dev (`localhost:4200`) |

### 6.7 Frontend (Sam Shop UI)

| Khu vực | Chức năng |
|---------|-----------|
| Công khai | Danh sách & chi tiết sản phẩm |
| USER | Giỏ hàng, checkout (**chọn COD hoặc Online**), thanh toán, đơn hàng của tôi |
| EMPLOYEE + ADMIN | Admin: quản lý sản phẩm, đơn hàng |
| ADMIN | Dashboard doanh thu, quản lý user (Keycloak) |

---

## 7. Hợp đồng API (tham chiếu — đã có trên frontend)

Base URL: `http://localhost:8088` (Gateway).

| Method | Path | Mô tả |
|--------|------|--------|
| GET | `/api/products` | Danh sách sản phẩm |
| GET | `/api/products/{id}` | Chi tiết |
| POST | `/api/products` | Tạo |
| PUT | `/api/products/{id}` | Cập nhật |
| DELETE | `/api/products/{id}` | Xóa |
| GET | `/api/products/categories` | Danh mục |
| POST | `/api/cart/add` | Thêm vào giỏ |
| GET | `/api/cart/{userId}` | Lấy giỏ |
| PUT | `/api/cart/update` | Cập nhật số lượng |
| DELETE | `/api/cart/remove/{itemId}` | Xóa dòng giỏ |
| POST | `/api/orders` | Tạo đơn (`paymentMethod`: `COD` \| `ONLINE`) |
| GET | `/api/orders` | Đơn của tôi |
| GET | `/api/orders/{id}` | Chi tiết đơn |
| GET | `/api/orders/all` | Tất cả đơn (staff) |
| PUT | `/api/orders/{id}/confirm` | Xác nhận |
| PUT | `/api/orders/{id}/status` | Cập nhật trạng thái |
| GET | `/api/orders/revenue` | Doanh thu |
| GET | `/api/payments/order/{orderId}` | Thanh toán theo đơn |
| POST | `/api/payments/online/initiate` | Khởi tạo thanh toán Online (mock/trả payment URL) |
| POST | `/api/payments/online/callback` | Callback kết quả Online (mock/webhook) |
| PUT | `/api/payments/{id}/confirm-cod` | EMPLOYEE xác nhận đã thu tiền COD |
| POST | `/api/payments/online/retry` | Thử lại thanh toán Online sau `FAILED` |

**Response chuẩn:**

```json
{
  "success": true,
  "message": "Success",
  "data": { }
}
```

---

## 8. Quy tắc nghiệp vụ tổng hợp (Business Rules)

| Mã | Quy tắc |
|----|---------|
| BR-01 | Không cho đặt số lượng vượt tồn kho. |
| BR-02 | Chỉ USER mới thao tác giỏ/đặt hàng của chính mình (`userId` từ JWT). |
| BR-03 | EMPLOYEE không truy cập quản lý user và dashboard doanh thu (chỉ ADMIN). |
| BR-04 | Đơn `CANCELLED` không chuyển sang trạng thái khác; hoàn tồn kho nếu đã trừ khi tạo đơn. |
| BR-05 | Doanh thu (`Revenue`) chỉ tính đơn `paymentStatus = PAID` (và có thể `status = COMPLETED` tùy chính sách). |
| BR-06 | Giá tiền dùng đơn vị VND, số thập phân tối đa 2 chữ số. |
| BR-07 | Mọi thay đổi trạng thái đơn phải ghi log (khuyến nghị kỹ thuật: audit table hoặc application log). |
| BR-08 | **Cấm** hai microservice dùng chung một database hoặc schema; vi phạm = không đạt kiến trúc dự án. |
| BR-09 | Mỗi microservice **phải** deploy bằng container riêng; không gom nhiều service vào một container. |
| BR-10 | Truy vấn dữ liệu service khác **chỉ** qua API (Feign/REST), không query trực tiếp DB neighbor. |
| BR-11 | Checkout **bắt buộc** chọn `paymentMethod`: `COD` hoặc `ONLINE`; không chấp nhận giá trị khác. |
| BR-12 | Đơn **ONLINE** chưa `PAID` không được chuyển sang `SHIPPING` / `COMPLETED`. |
| BR-13 | Đơn **COD** được phép giao hàng khi `paymentStatus = UNPAID`; chuyển `PAID` sau khi thu tiền. |

---

## 9. Luồng nghiệp vụ chính

### 9.1 Luồng mua hàng — Thanh toán COD

```
1. USER đăng nhập → thêm sản phẩm vào giỏ
2. Checkout: nhập địa chỉ + chọn COD
3. Tạo đơn (PENDING) + Payment (COD, UNPAID)
4. EMPLOYEE xác nhận → CONFIRMED → SHIPPING
5. Giao hàng thành công → COMPLETED
6. EMPLOYEE xác nhận đã thu tiền → Payment PAID
7. Khách xem đơn tại /orders
```

### 9.2 Luồng mua hàng — Thanh toán Online

```
1. USER đăng nhập → thêm sản phẩm vào giỏ
2. Checkout: nhập địa chỉ + chọn ONLINE
3. Tạo đơn (PENDING) + Payment (ONLINE, UNPAID)
4. Gọi API thanh toán Online (mock Phase 1)
   → Thành công: PAID + transactionCode → EMPLOYEE xác nhận → giao hàng → COMPLETED
   → Thất bại: FAILED → khách retry hoặc hủy đơn
5. Khách xem đơn và trạng thái thanh toán tại /orders
```

### 9.3 Luồng quản trị sản phẩm

```
EMPLOYEE/ADMIN đăng nhập
  → /admin/products
  → CRUD sản phẩm & danh mục
  → Cập nhật quantity / status
```

---

## 10. Yêu cầu phi chức năng (Non-functional)

| Hạng mục | Yêu cầu |
|----------|---------|
| Hiệu năng | API đọc danh sách sản phẩm < 500ms (môi trường dev); hỗ trợ phân trang khi > 100 SP. |
| Bảo mật | HTTPS ở production; JWT expiry; không lưu password ở microservice. |
| Khả dụng | Mỗi service/container có thể restart độc lập; lỗi một service không làm sập toàn bộ. |
| Dữ liệu | **Database-per-service (bắt buộc)**; migration Flyway/Liquibase **trong từng service**. |
| Triển khai | **Docker Compose** điều phối toàn bộ; mỗi service một image, mỗi DB một Postgres container + volume riêng. |
| Quan sát | Spring Actuator `/actuator/health` trên mỗi container app; healthcheck Postgres `pg_isready`. |
| Mạng | Tất cả container trong network `sam-shop-net`; DB **không** expose ra internet (chỉ host port dev nếu cần pgAdmin). |

---

## 11. Cấu hình môi trường tham chiếu (Docker dev)

### 11.1 Application (từ host / browser)

| Thành phần | URL / Port |
|------------|------------|
| Frontend | `http://localhost:4200` |
| API Gateway | `http://localhost:8088` |
| Keycloak | `http://localhost:8180`, realm `sam-shop` |
| Eureka Dashboard | `http://localhost:8761` |
| Product API (trực tiếp, debug) | `http://localhost:8081` |
| Cart API (debug) | `http://localhost:8082` |
| Order API (debug) | `http://localhost:8083` |
| Payment API (debug) | `http://localhost:8084` |

### 11.2 PostgreSQL (pgAdmin — mỗi DB một connection)

| Connection name | Host | Port | Database | User | Password |
|-----------------|------|------|----------|------|----------|
| Sam - Keycloak | `localhost` | `5433` | `keycloak` | `postgres` | `123456` |
| Sam - Product | `localhost` | `5434` | `product_db` | `postgres` | `123456` |
| Sam - Cart | `localhost` | `5435` | `cart_db` | `postgres` | `123456` |
| Sam - Order | `localhost` | `5436` | `order_db` | `postgres` | `123456` |
| Sam - Payment | `localhost` | `5437` | `payment_db` | `postgres` | `123456` |

> Trong Docker network, app dùng hostname container (`sam-postgres-product`, …) port **5432** nội bộ.

---

## 12. Tiêu chí chấp nhận (Acceptance Criteria) — MVP

### 12.1 Nghiệp vụ

- [ ] User đăng nhập Keycloak và gọi API có JWT thành công.
- [ ] Xem danh sách/chi tiết sản phẩm không cần đăng nhập (hoặc có — thống nhất khi implement).
- [ ] USER thêm/sửa/xóa giỏ hàng và tạo đơn với địa chỉ giao hàng.
- [ ] EMPLOYEE/ADMIN quản lý sản phẩm qua `/admin/products`.
- [ ] EMPLOYEE/ADMIN xem tất cả đơn, xác nhận và đổi trạng thái.
- [ ] ADMIN xem dashboard doanh thu (`totalRevenue`, `paidOrderCount`, `pendingOrderCount`).
- [ ] Tra cứu payment theo `orderId`.
- [ ] Checkout chọn **COD** hoặc **ONLINE**; luồng trạng thái đúng từng loại.
- [ ] COD: EMPLOYEE xác nhận thu tiền → `PAID`.
- [ ] ONLINE (mock): thanh toán thành công / thất bại / thử lại.
- [ ] Gateway route đúng 4 nhóm API và từ chối request không có quyền.

### 12.2 Kiến trúc Docker & Database

- [ ] Mỗi service (`discovery`, `gateway`, `identity`, `product`, `cart`, `order`, `payment`) chạy trong **container riêng**.
- [ ] `product`, `cart`, `order`, `payment`, `identity` mỗi cái có **Postgres container + database riêng**.
- [ ] `docker compose up` khởi động toàn stack; `docker compose down -v` xóa sạch volume theo từng DB.
- [ ] pgAdmin kết nối được 5 database qua 5 port host khác nhau (mục 11.2).
- [ ] Dừng `sam-product` không làm mất dữ liệu `cart_db` / `order_db` (tách volume).
- [ ] Không có cấu hình JDBC trỏ hai service vào cùng một `database name`.

---

## 13. Lộ trình gợi ý (Roadmap)

| Giai đoạn | Nội dung |
|-----------|----------|
| **Phase 1 — MVP** | Docker Compose full stack: 7 app containers + 5 Postgres containers; database-per-service; frontend end-to-end. |
| **Phase 2** | Feign giữa services, saga/trừ tồn kho nhất quán, email thông báo đơn. |
| **Phase 3** | Cổng thanh toán thật, báo cáo nâng cao, CI/CD, Kubernetes. |

---

## 14. Cấu trúc dự án (Project Structure)

Quy ước chung:

- **Backend:** Java 21, Spring Boot 4, package `com.example.<tên-service>` (snake_case theo artifact).
- **Mỗi microservice** tuân theo **layered architecture**: `controller` → `service` → `repository` → `entity`, kèm `dto`, `mapper`, `config`, `exception`.
- **Frontend:** Angular 19, feature-based folders dưới `src/app/`.

### 14.1 Cấu trúc monorepo (root)

```
sam-shop-microservices/
├── business_requirement.md
├── docker-compose.yml
├── .env / .env.example
├── discovery-service/
├── gateway-service/
├── identity-service/
├── product-service/
├── cart-service/
├── order-service/
├── payment-service/
└── sam-shop-frontend/
```

### 14.2 Cấu trúc chuẩn một Spring microservice

Áp dụng cho `product-service`, `cart-service`, `order-service`, `payment-service` (điều chỉnh tên class theo domain).

```
<service-name>/
├── Dockerfile
├── .dockerignore
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/example/<service_name>/
    │   │   │
    │   │   ├── <ServiceName>Application.java
    │   │   │
    │   │   ├── config/
    │   │   │   ├── SecurityConfig.java
    │   │   │   ├── OpenFeignConfig.java          # cart, order, payment
    │   │   │   └── CorsConfig.java
    │   │   │
    │   │   ├── controller/
    │   │   │   └── ...Controller.java
    │   │   │
    │   │   ├── service/
    │   │   │   ├── ...Service.java
    │   │   │   └── impl/
    │   │   │       └── ...ServiceImpl.java
    │   │   │
    │   │   ├── repository/
    │   │   │   └── ...Repository.java
    │   │   │
    │   │   ├── entity/
    │   │   │   └── ...java
    │   │   │
    │   │   ├── dto/
    │   │   │   ├── request/
    │   │   │   │   └── ...Request.java
    │   │   │   └── response/
    │   │   │       ├── ...Response.java
    │   │   │       └── ApiResponse.java
    │   │   │
    │   │   ├── mapper/
    │   │   │   └── ...Mapper.java
    │   │   │
    │   │   ├── exception/
    │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   ├── AppException.java
    │   │   │   └── ErrorCode.java
    │   │   │
    │   │   ├── enums/
    │   │   │   └── ...java
    │   │   │
    │   │   ├── client/                           # OpenFeign gọi service khác
    │   │   │   └── ...Client.java
    │   │   │
    │   │   └── common/
    │   │       ├── PageResponse.java
    │   │       └── Constants.java
    │   │
    │   └── resources/
    │       ├── application.yaml
    │       └── db/migration/                     # Flyway (khuyến nghị)
    │           └── V1__init.sql
    │
    └── test/java/com/example/<service_name>/
        └── <ServiceName>ApplicationTests.java
```

### 14.3 Product Service (mẫu chi tiết)

Database: `product_db` · Container: `sam-product` · Port: `8081`

```
product-service/
└── src/main/java/com/example/product_service/
    │
    ├── ProductServiceApplication.java
    │
    ├── config/
    │   ├── SecurityConfig.java
    │   ├── OpenFeignConfig.java
    │   └── CorsConfig.java
    │
    ├── controller/
    │   ├── ProductController.java
    │   └── CategoryController.java
    │
    ├── service/
    │   ├── ProductService.java
    │   ├── CategoryService.java
    │   └── impl/
    │       ├── ProductServiceImpl.java
    │       └── CategoryServiceImpl.java
    │
    ├── repository/
    │   ├── ProductRepository.java
    │   └── CategoryRepository.java
    │
    ├── entity/
    │   ├── Product.java
    │   └── Category.java
    │
    ├── dto/
    │   ├── request/
    │   │   ├── ProductCreateRequest.java
    │   │   ├── ProductUpdateRequest.java
    │   │   └── CategoryRequest.java
    │   └── response/
    │       ├── ProductResponse.java
    │       ├── CategoryResponse.java
    │       └── ApiResponse.java
    │
    ├── mapper/
    │   ├── ProductMapper.java
    │   └── CategoryMapper.java
    │
    ├── exception/
    │   ├── GlobalExceptionHandler.java
    │   ├── AppException.java
    │   └── ErrorCode.java
    │
    ├── enums/
    │   └── ProductStatus.java
    │
    ├── client/                                   # tùy chọn — gọi service khác nếu tách inventory
    │   └── InventoryClient.java
    │
    └── common/
        ├── PageResponse.java
        └── Constants.java
```

### 14.4 Cart Service

Database: `cart_db` · Container: `sam-cart` · Port: `8082`

```
cart-service/
└── src/main/java/com/example/cart_service/
    ├── CartServiceApplication.java
    ├── config/
    │   ├── SecurityConfig.java
    │   ├── OpenFeignConfig.java
    │   └── CorsConfig.java
    ├── controller/
    │   └── CartController.java
    ├── service/
    │   ├── CartService.java
    │   └── impl/
    │       └── CartServiceImpl.java
    ├── repository/
    │   ├── CartRepository.java
    │   └── CartItemRepository.java
    ├── entity/
    │   ├── Cart.java
    │   └── CartItem.java
    ├── dto/
    │   ├── request/
    │   │   ├── AddToCartRequest.java
    │   │   └── UpdateCartItemRequest.java
    │   └── response/
    │       ├── CartResponse.java
    │       ├── CartItemResponse.java
    │       └── ApiResponse.java
    ├── mapper/
    │   └── CartMapper.java
    ├── exception/
    │   ├── GlobalExceptionHandler.java
    │   ├── AppException.java
    │   └── ErrorCode.java
    ├── client/
    │   └── ProductClient.java                    # Feign → product-service (kiểm tra tồn kho, giá)
    └── common/
        ├── PageResponse.java
        └── Constants.java
```

### 14.5 Order Service

Database: `order_db` · Container: `sam-order` · Port: `8083`

```
order-service/
└── src/main/java/com/example/order_service/
    ├── OrderServiceApplication.java
    ├── config/
    │   ├── SecurityConfig.java
    │   ├── OpenFeignConfig.java
    │   └── CorsConfig.java
    ├── controller/
    │   └── OrderController.java
    ├── service/
    │   ├── OrderService.java
    │   └── impl/
    │       └── OrderServiceImpl.java
    ├── repository/
    │   ├── OrderRepository.java
    │   └── OrderItemRepository.java
    ├── entity/
    │   ├── Order.java
    │   └── OrderItem.java
    ├── dto/
    │   ├── request/
    │   │   ├── CreateOrderRequest.java
    │   │   └── UpdateOrderStatusRequest.java
    │   └── response/
    │       ├── OrderResponse.java
    │       ├── OrderItemResponse.java
    │       ├── RevenueResponse.java
    │       └── ApiResponse.java
    ├── mapper/
    │   └── OrderMapper.java
    ├── exception/
    │   ├── GlobalExceptionHandler.java
    │   ├── AppException.java
    │   └── ErrorCode.java
    ├── enums/
    │   ├── OrderStatus.java
    │   └── PaymentStatus.java
    ├── client/
    │   ├── CartClient.java
    │   ├── ProductClient.java
    │   └── PaymentClient.java
    └── common/
        ├── PageResponse.java
        └── Constants.java
```

### 14.6 Payment Service

Database: `payment_db` · Container: `sam-payment` · Port: `8084`

```
payment-service/
└── src/main/java/com/example/payment_service/
    ├── PaymentServiceApplication.java
    ├── config/
    │   ├── SecurityConfig.java
    │   ├── OpenFeignConfig.java
    │   └── CorsConfig.java
    ├── controller/
    │   └── PaymentController.java
    ├── service/
    │   ├── PaymentService.java
    │   └── impl/
    │       └── PaymentServiceImpl.java
    ├── repository/
    │   └── PaymentRepository.java
    ├── entity/
    │   └── Payment.java
    ├── dto/
    │   ├── request/
    │   │   └── CreatePaymentRequest.java
    │   └── response/
    │       ├── PaymentResponse.java
    │       └── ApiResponse.java
    ├── mapper/
    │   └── PaymentMapper.java
    ├── exception/
    │   ├── GlobalExceptionHandler.java
    │   ├── AppException.java
    │   └── ErrorCode.java
    ├── enums/
    │   ├── PaymentMethod.java                      # COD, ONLINE
    │   └── PaymentStatus.java
    ├── client/
    │   └── OrderClient.java
    └── common/
        ├── PageResponse.java
        └── Constants.java
```

### 14.7 Discovery Service

Không có database · Container: `sam-discovery` · Port: `8761`

```
discovery-service/
└── src/main/java/com/example/discovery_service/
    ├── DiscoveryServiceApplication.java          # @EnableEurekaServer
    └── config/
        └── EurekaServerConfig.java               # tùy chọn
```

### 14.8 Gateway Service

Không có database · Container: `sam-gateway` · Port: `8088`

```
gateway-service/
└── src/main/java/com/example/gateway_service/
    ├── GatewayServiceApplication.java
    ├── config/
    │   ├── SecurityConfig.java                   # OAuth2 Resource Server
    │   ├── GatewayRouteConfig.java               # routes tới microservices
    │   └── CorsConfig.java
    ├── filter/
    │   └── JwtAuthenticationFilter.java          # tùy chọn
    └── exception/
        └── GlobalExceptionHandler.java
```

### 14.9 Identity Service (Keycloak)

Database: `keycloak` · Container: `sam-identity` · Port: `8180`

```
identity-service/
├── Dockerfile
├── docker-compose.yml                            # chạy độc lập (dev)
├── conf/
│   ├── keycloak.conf
│   └── cache-ispn.xml
├── providers/                                    # custom SPI (nếu có)
├── themes/                                       # custom theme (nếu có)
├── bin/
└── lib/                                          # Keycloak distribution
```

> Keycloak **không** theo cấu trúc Spring; quản lý user/role qua Admin Console hoặc realm export JSON.

### 14.10 Frontend (Angular)

Container: `sam-frontend` · Port: `4200`

```
sam-shop-frontend/
├── Dockerfile
├── nginx.conf
├── angular.json
├── package.json
└── src/
    ├── environments/
    │   ├── environment.ts
    │   └── environment.prod.ts
    ├── app/
    │   ├── app.component.ts
    │   ├── app.config.ts
    │   ├── app.routes.ts
    │   │
    │   ├── core/
    │   │   ├── guards/
    │   │   │   ├── auth.guard.ts
    │   │   │   └── role.guard.ts
    │   │   ├── interceptors/
    │   │   │   └── auth.interceptor.ts
    │   │   ├── services/
    │   │   │   ├── api.service.ts
    │   │   │   ├── auth.service.ts
    │   │   │   └── token.service.ts
    │   │   ├── models/
    │   │   │   ├── user.model.ts
    │   │   │   ├── product.model.ts
    │   │   │   ├── cart.model.ts
    │   │   │   └── order.model.ts
    │   │   └── utils/
    │   │       └── jwt.util.ts
    │   │
    │   ├── features/
    │   │   ├── auth/
    │   │   │   ├── login/
    │   │   │   └── register/
    │   │   ├── products/
    │   │   ├── cart/
    │   │   ├── orders/
    │   │   ├── payment/
    │   │   └── admin/
    │   │       ├── dashboard/
    │   │       ├── product-management/
    │   │       ├── order-management/
    │   │       └── user-management/
    │   │
    │   ├── layouts/
    │   │   ├── client-layout/
    │   │   └── admin-layout/
    │   │
    │   └── shared/
    │       ├── components/
    │       └── pipes/
    │
    └── styles.scss
```

### 14.11 Ma trận package ↔ layer (tham chiếu nhanh)

| Layer | Trách nhiệm | Ví dụ |
|-------|-------------|--------|
| `controller` | REST API, validate input, HTTP status | `ProductController` |
| `service` | Business logic, transaction | `ProductServiceImpl` |
| `repository` | JPA / DB access | `ProductRepository` |
| `entity` | Bảng DB mapping | `Product` |
| `dto` | Request/Response, tách API khỏi entity | `ProductCreateRequest` |
| `mapper` | Entity ↔ DTO | `ProductMapper` |
| `client` | Feign gọi service khác | `ProductClient` |
| `config` | Security, Feign, CORS | `SecurityConfig` |
| `exception` | Xử lý lỗi tập trung | `GlobalExceptionHandler` |
| `common` | Utils dùng chung trong service | `ApiResponse` |

---

## 15. Thuật ngữ

| Thuật ngữ | Định nghĩa |
|-----------|------------|
| Đơn hàng (Order) | Giao dịch mua gồm danh sách sản phẩm, tổng tiền, địa chỉ giao. |
| Giỏ hàng (Cart) | Tập sản phẩm tạm trước khi checkout. |
| Realm | Miền quản lý user/client trên Keycloak. |
| MVP | Sản phẩm tối thiểu khả dụng. |
| COD | Cash on Delivery — thanh toán khi nhận hàng (`paymentMethod = COD`). |
| ONLINE | Thanh toán trực tuyến qua cổng (`paymentMethod = ONLINE`). |
| Container | Một instance Docker chạy một process/service độc lập. |
| Database-per-service | Mỗi microservice có PostgreSQL database riêng, không chia sẻ. |

---

*Tài liệu này đồng bộ với codebase hiện tại (`sam-shop-frontend`, các `*-service`, `identity-service`) và là cơ sở để triển khai backend, cấu hình Keycloak realm, và kiểm thử chấp nhận.*

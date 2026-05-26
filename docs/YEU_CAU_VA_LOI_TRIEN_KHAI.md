# Yêu cầu đã thêm & lỗi khi triển khai

Tài liệu tổng hợp các tính năng bổ sung trong phiên phát triển gần đây, kèm lỗi thường gặp và cách xử lý. Chi tiết kỹ thuật từng mảng xem thêm:

- [CHAT_SERVICE.md](./CHAT_SERVICE.md)
- [TROUBLESHOOTING.md](./TROUBLESHOOTING.md)
- [identity-service/keycloak/README.md](../identity-service/keycloak/README.md)

---

## 1. Ảnh sản phẩm (lưu file, không base64)

### Yêu cầu

- Upload ảnh sản phẩm từ admin; lưu file trong thư mục `uploads/` của `product-service`.
- Database chỉ lưu **đường dẫn URL** (ví dụ `/api/products/uploads/product-1-xxx.jpg`), không lưu base64 trong `varchar(255)`.
- Frontend hiển thị ảnh qua URL (gateway proxy).

### Lỗi đã gặp

| Triệu chứng | Nguyên nhân | Cách xử lý |
|-------------|-------------|------------|
| Lưu ảnh lỗi / truncate DB | Base64 quá dài cho `varchar(255)` | Chuyển sang lưu file + path trong DB |
| FE không hiển thị ảnh | URL tương đối hoặc thiếu proxy | `product-image.util.ts` + nginx/gateway proxy `/api/products/**` |
| 401 khi xem catalog | Interceptor gửi token hết hạn lên GET public | `auth.interceptor.ts`: bỏ token cho `GET /api/products` |

### File liên quan

- `product-service` — `ProductImageStorageService`, `GET /api/products/uploads/{filename}`
- `sam-shop-frontend` — upload multipart admin, `product-image.util.ts`

---

## 2. Chat real-time (USER ↔ Staff/Admin)

### Yêu cầu

- Microservice **`chat-service`** (port **8085**), DB PostgreSQL `chat_db` (port host **5438**).
- REST qua Gateway `:8088` — `/api/chat/**`.
- WebSocket real-time; phân quyền Keycloak (`USER`, `EMPLOYEE`, `ADMIN`).
- FE: khách `/chat`, nhân viên/admin `/admin/chat`.
- Đăng ký Eureka `chat-service`; route Gateway `lb://chat-service`.

### Lỗi đã gặp

| Triệu chứng | Nguyên nhân | Cách xử lý |
|-------------|-------------|------------|
| `WebSocket connection failed` tới `ws://localhost:4200/api/chat/ws?access_token=...` | Spring Cloud Gateway **WebMVC không upgrade WebSocket**; URL JWT trong query quá dài | Nginx proxy **trực tiếp** `chat-service:8085`; auth JWT qua header **`Sec-WebSocket-Protocol`** |
| WS 401 / không handshake | Gateway chặn hoặc không forward upgrade | Bypass Gateway cho WS trong `sam-shop-frontend/nginx.conf` |
| Admin gửi tin → *"Mất kết nối chat"* | `assertCanAccess` dùng `SecurityUtils.isStaff()` trên thread WS — **SecurityContext rỗng** → FORBIDDEN → đóng socket | Kiểm tra quyền theo `AuthUser.role()` từ session WS; bọc lỗi trong handler, không đóng connection |
| Customer gửi tin, admin thấy; customer **không thấy** tin mình | `JOIN`/`SEND` gửi **trước** `WebSocket.OPEN` → session chưa vào registry | FE: hàng đợi message + `join()` trong `onopen`; BE: auto `joinConversation` khi `SEND` |
| REST chat 401 trong Docker | JWK lấy từ `localhost:8180` trong container | `JWK_SET_URI` → `http://identity-service:8080/.../certs` (xem `docker-compose.yml`) |

### File liên quan

- `chat-service/` — entity, REST, `ChatWebSocketHandler`, `JwtHandshakeInterceptor`
- `gateway-service` — route `/api/chat/**`
- `sam-shop-frontend/nginx.conf` — `location /api/chat/ws`
- `sam-shop-frontend` — `chat.service.ts`, `user-chat`, `staff-chat`

### Kiểm tra nhanh

```powershell
docker compose up -d chat-service frontend gateway-service
# customer1 / staff1 — mật khẩu 123456
# http://localhost:4200/chat  và  /admin/chat
```

---

## 3. Đăng ký tài khoản & phân quyền (không qua Keycloak Admin Console)

### Yêu cầu

- Khách **tự đăng ký** trên `/register` — role **`USER`**, tự gán `userId` trong Keycloak.
- **Admin** tạo tài khoản + chọn role (`USER` / `EMPLOYEE` / `ADMIN`) tại `/admin/users`.
- Microservice **`user-service`** (port **8086**) gọi **Keycloak Admin API** (credentials admin trong Docker, không lộ ra browser).
- API công khai: `POST /api/auth/register`; API admin: `GET/POST /api/admin/users` (cần JWT role `ADMIN`).
- Sau đăng ký, FE tự đăng nhập (Keycloak password grant).

### Lỗi đã gặp

| Triệu chứng | Nguyên nhân | Cách xử lý |
|-------------|-------------|------------|
| Thông báo *"Đăng ký qua Keycloak Admin..."* | `AuthService.register()` chỉ là stub | Gọi `POST /api/auth/register` qua Gateway |
| `POST /api/auth/register` → **401** | Gateway yêu cầu JWT cho mọi `/api/**` | `permitAll` cho `POST /api/auth/register` (gateway + user-service) |
| `POST /api/auth/register` → **500** `Type definition error: JsonNode` | `RestClient` không deserialize `JsonNode` | Dùng DTO (`OAuthTokenResponse`, `KeycloakUserRepresentation`, …) |
| `409 Conflict` khi đăng ký lại cùng username | Username/email đã tồn trên Keycloak | Thông báo rõ; dùng username khác |
| Admin không tạo được user | Thiếu route Gateway hoặc không có role ADMIN | Route `user-service`; đăng nhập `admin1` |
| JWT thiếu `userId` sau đăng ký mới | User mới chưa có attribute `userId` / mapper | `user-service` set attribute `userId`; realm cần protocol mapper `userId` (script `configure-sam-shop.ps1`) |

### File liên quan

- `user-service/` — `KeycloakAdminService`, `AuthController`, `AdminUserController`
- `gateway-service` — route `/api/auth/**`, `/api/admin/users/**`
- `docker-compose.yml` — service `sam-user`, env `KEYCLOAK_ADMIN_URL`, `KC_ADMIN_*`
- `sam-shop-frontend` — `auth.service.ts`, `user-management`, `user-admin.service.ts`

### Bảo mật

- Chỉ **`/api/auth/register`** được phép tạo role `USER`.
- Tạo `EMPLOYEE` / `ADMIN` chỉ qua API admin (JWT `ROLE_ADMIN`).
- Không gọi Keycloak Admin API từ trình duyệt.

### Kiểm tra nhanh

```powershell
docker compose build user-service gateway-service frontend
docker compose up -d user-service gateway-service frontend

# Đăng ký: http://localhost:4200/register
# Quản lý user: http://localhost:4200/admin/users (admin1 / 123456)
```

---

## 4. Lỗi hạ tầng chung (ảnh hưởng nhiều yêu cầu)

Các mục dưới đây xuất hiện khi triển khai Docker/Eureka/JWT — không gắn riêng một feature nhưng hay chặn test chat, đăng ký, API.

| Triệu chứng | Nguyên nhân | Cách xử lý |
|-------------|-------------|------------|
| FE gọi API trả **HTML** thay JSON | Nginx không proxy `/api/` | `nginx.conf`: `location /api/` → `gateway-service:8088` |
| API **401** dù có Bearer token (trong Docker) | Service tải JWK từ `localhost:8180` trong container | `JWK_SET_URI` trỏ `identity-service:8080` |
| Eureka `Connection refused` | Client trỏ `localhost:8761` trong container | `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://discovery-service:8761/eureka/` |
| GET `/api/products` **401** | Token hết hạn + interceptor vẫn gửi | Bỏ token cho GET public products; xóa token expired |

Chi tiết: [TROUBLESHOOTING.md](./TROUBLESHOOTING.md)

---

## 5. Bản đồ service & port (tham khảo)

| Service | Port (host) | Ghi chú |
|---------|-------------|---------|
| frontend (nginx) | 4200 | Proxy `/api/` → gateway; WS chat → chat-service |
| gateway-service | 8088 | REST; không proxy WebSocket |
| identity-service (Keycloak) | 8180 | Realm `sam-shop`, client `sam-shop-ui` |
| discovery-service (Eureka) | 8761 | Service discovery |
| product-service | 8081 | Upload ảnh `/api/products/uploads/**` |
| chat-service | 8085 | REST + WebSocket `/api/chat/ws` |
| user-service | 8086 | Đăng ký / quản lý user Keycloak |
| postgres-chat | 5438 | DB `chat_db` |

---

## 6. Tài khoản test Keycloak

| Username | Password | Role | userId (claim) |
|----------|----------|------|----------------|
| customer1 | 123456 | USER | 1 |
| staff1 | 123456 | EMPLOYEE | 2 |
| admin1 | 123456 | ADMIN | 3 |

Tài khoản mới đăng ký qua app nhận `userId` tăng dần (max attribute hiện có + 1).

---

## 7. Lệnh rebuild sau khi sửa code

```powershell
# Chat
docker compose build chat-service frontend
docker compose up -d chat-service frontend

# User / đăng ký
docker compose build user-service gateway-service frontend
docker compose up -d user-service gateway-service frontend

# Toàn stack
docker compose up -d
```

---

*Tài liệu cập nhật theo các yêu cầu: ảnh sản phẩm, chat-service, user-service (đăng ký & phân quyền), và các sửa lỗi FE/nginx/JWT/WebSocket đi kèm.*

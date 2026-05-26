# Deploy Sam Shop lên Azure VM (Docker Compose)

Hướng dẫn triển khai **toàn bộ stack** (microservices + Keycloak + PostgreSQL + frontend) trên **một Azure Virtual Machine** bằng `docker compose` — phù hợp demo, nội bộ, hoặc giai đoạn đầu production.

---

## 1. Kiến trúc trên VM

```text
Internet
   │
   ▼
Azure VM (Public IP / DNS)
   ├── :4200  frontend (nginx) ──► /api/* ──► gateway-service:8088
   │                              └── /api/chat/ws ──► chat-service:8085
   ├── :8180  Keycloak (identity-service)  ← browser gọi login/token
   └── Docker network nội bộ: Eureka, 6 DB Postgres, các microservice
```

| Port công khai | Service | Ghi chú |
|----------------|---------|---------|
| **4200** | Frontend | URL chính cho người dùng |
| **8180** | Keycloak | FE đăng nhập trực tiếp (`environment.prod` build với URL này) |

Các port khác (8081–8088, 8761, 5433–5438) **không cần** mở ra Internet nếu chỉ truy cập qua frontend.

---

## 2. Tạo Azure VM

### Portal (tóm tắt)

1. **Create a resource** → **Virtual machine**.
2. **Image:** Ubuntu 22.04 LTS (khuyến nghị).
3. **Size:** tối thiểu `Standard_B2s` (2 vCPU, 4 GB RAM); stack đầy đủ nên dùng **B4ms** trở lên nếu chạy ổn định.
4. **Authentication:** SSH public key.
5. **Networking:** tạo NSG, gán **Public IP**.
6. Sau khi tạo, ghi lại **Public IP** — dùng làm `PUBLIC_HOST` (ví dụ VM hiện tại: `20.24.185.233`).

### NSG — Inbound rules (bắt buộc — nếu thiếu sẽ timeout từ trình duyệt)

**Triệu chứng:** `docker compose ps` mọi thứ `Up` nhưng mở http://IP:4200 **không load / timeout**.

**Cách mở (Azure Portal):**

1. VM `shop-sam` → **Networking** (hoặc **Settings → Networking**).
2. Chọn **Network security group** (tên NSG gắn với NIC).
3. **Settings → Inbound security rules** → **Add**.
4. Thêm **2 rule** (hoặc sửa rule có sẵn):

| Name | Priority | Source | Destination | Service | Port | Action |
|------|----------|--------|-------------|---------|------|--------|
| Allow-HTTP-4200 | 1010 | Any | Any | Custom | **4200** | Allow |
| Allow-Keycloak-8180 | 1020 | Any | Any | Custom | **8180** | Allow |

5. **Save** — đợi vài giây, thử lại trình duyệt (dùng **http** không phải https).

| Priority | Port | Mục đích |
|----------|------|----------|
| 1000 | 22 | SSH |
| 1010 | 4200 | Frontend |
| 1020 | 8180 | Keycloak (login API từ browser) |

### SSH vào VM

```bash
ssh azureuser@20.12.34.56
```

---

## 3. Cài Docker trên VM (Ubuntu)

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl git
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
# Đăng xuất SSH và vào lại để group docker có hiệu lực
```

Kiểm tra:

```bash
docker compose version
```

---

## 4. Đưa code lên VM

**Cách 1 — Git (khuyến nghị)**

```bash
git clone https://github.com/<your-org>/sam-shop-microservices.git
cd sam-shop-microservices
```

**Cách 2 — Copy từ máy local**

```powershell
# Trên máy Windows
scp -r d:\sam-shop-microservices azureuser@20.12.34.56:~/sam-shop-microservices
```

---

## 5. File `.env` trên VM

```bash
cp .env.azure.example .env
nano .env
```

Sửa tối thiểu:

```env
PUBLIC_HOST=20.12.34.56
POSTGRES_PASSWORD=<mật_khẩu_mạnh>
KC_ADMIN_PASSWORD=<mật_khẩu_admin_keycloak>
```

| Biến | Ý nghĩa |
|------|---------|
| `PUBLIC_HOST` | IP hoặc DNS công khai VM (**không** có `http://`) |
| `KEYCLOAK_PORT` | Mặc định `8180` |
| `FRONTEND_PORT` | Mặc định `4200` |

Repo đã cấu hình:

- `issuer-uri` JWT = `http://${PUBLIC_HOST}:8180/realms/sam-shop`
- Keycloak `KC_HOSTNAME` = `PUBLIC_HOST`
- Frontend build nhúng Keycloak URL = `http://${PUBLIC_HOST}:8180`

---

## 6. Build và chạy stack

```bash
chmod +x scripts/azure-vm-deploy.sh
./scripts/azure-vm-deploy.sh
```

Hoặc thủ công:

```bash
docker compose up --build -d
docker compose ps
```

Lần đầu build có thể mất **15–30 phút** (nhiều image Java + npm).

---

## 7. Cấu hình Keycloak realm (bắt buộc lần đầu)

Gọi Admin API qua `http://<IP-công-khai>:8180` từ Windows có thể lỗi **`HTTPS required`**. Dùng **Cách A** hoặc **Cách B**.

### Cách A — Trên VM qua localhost (khuyến nghị)

```bash
cd ~/sam-shop-microservices
export KC_ADMIN_PASSWORD='<mật_khẩu_trong_.env>'
export FE_PUBLIC_URL='http://20.24.185.233:4200'

chmod +x scripts/configure-keycloak-azure.sh
./scripts/configure-keycloak-azure.sh
```

### Cách B — SSH tunnel từ Windows

PowerShell **1** (giữ mở): `ssh -L 8180:127.0.0.1:8180 azureuser@20.24.185.233`

PowerShell **2**:

```powershell
$env:KEYCLOAK_URL = "http://localhost:8180"
$env:FE_PUBLIC_URL = "http://20.24.185.233:4200"
$env:KC_ADMIN_PASSWORD = "<mật_khẩu_trong_.env>"
powershell -ExecutionPolicy Bypass -File identity-service\keycloak\configure-sam-shop.ps1
```

Script tạo realm `sam-shop`, client `sam-shop-ui`, mapper `userId`, user mẫu `customer1` / `staff1` / `admin1` (mật khẩu `123456`).

Chi tiết: [identity-service/keycloak/CONFIGURE_SAM_SHOP.md](../identity-service/keycloak/CONFIGURE_SAM_SHOP.md)

---

## 8. Kiểm tra sau deploy

| Bước | URL / hành động |
|------|------------------|
| Frontend | http://20.12.34.56:4200 |
| Đăng nhập | `customer1` / `123456` |
| Admin | `admin1` / `123456` → quản lý user, sản phẩm |
| Chat | `/chat`, `/admin/chat` |
| Health gateway | `curl http://localhost:8088/actuator/health` (trên VM) |

Nếu **401** trên API sau khi login:

- Kiểm tra `PUBLIC_HOST` trong `.env` khớp IP/DNS bạn mở trên browser.
- Rebuild: `docker compose up --build -d frontend gateway-service` (đổi `PUBLIC_HOST` phải rebuild frontend + restart services dùng JWT).

---

## 9. Cập nhật phiên bản mới

```bash
cd ~/sam-shop-microservices
git pull
docker compose up --build -d
```

---

## 10. Firewall trên VM (tuỳ chọn)

```bash
sudo ufw allow 22/tcp
sudo ufw allow 4200/tcp
sudo ufw allow 8180/tcp
sudo ufw enable
```

NSG Azure vẫn phải mở cùng các port.

---

## 11. DNS tùy chọn (không bắt buộc)

1. Tạo **A record** `shop.example.com` → IP VM.
2. Đặt `PUBLIC_HOST=shop.example.com` trong `.env`.
3. Chạy lại `configure-sam-shop.ps1` với `FE_PUBLIC_URL=http://shop.example.com:4200` và `KEYCLOAK_URL=http://shop.example.com:8180`.
4. Rebuild: `docker compose up --build -d`.

---

## 12. HTTPS (production — khuyến nghị sau)

Stack hiện dùng **HTTP** cho đơn giản. Production nên:

- Gắn **Application Gateway** hoặc **Nginx + Let's Encrypt** trước VM.
- Đổi Keycloak sang HTTPS (`KC_HOSTNAME=https://auth.example.com`, `KC_PROXY=edge`).
- Cập nhật `issuer-uri`, `KEYCLOAK_PUBLIC_URL`, `FE_PUBLIC_URL` sang `https://`.

---

## 13. Xử lý lỗi thường gặp

| Triệu chứng | Nguyên nhân | Cách xử lý |
|-------------|-------------|------------|
| Không mở được :4200 | NSG/ufw chưa mở | Mở port 4200 trên NSG + VM |
| Login lỗi CORS / network | Keycloak URL sai trong FE build | Kiểm tra `PUBLIC_HOST`, rebuild `frontend` |
| API 401 có token | `issuer-uri` ≠ claim `iss` trong JWT | `PUBLIC_HOST` phải khớp host Keycloak (IP:8180) |
| Chat mất kết nối | WS qua nginx | Dùng cùng host :4200, không gọi WS trực tiếp :8085 từ browser |
| Hết RAM | Quá nhiều container | Tăng size VM hoặc tắt service không dùng khi demo |
| `configure-sam-shop` lỗi | Keycloak chưa sẵn sàng | Đợi 1–2 phút sau `docker compose up`, thử lại |

Xem thêm: [TROUBLESHOOTING.md](./TROUBLESHOOTING.md), [YEU_CAU_VA_LOI_TRIEN_KHAI.md](./YEU_CAU_VA_LOI_TRIEN_KHAI.md)

---

## 14. Checklist nhanh

- [ ] VM Ubuntu + Docker + Docker Compose
- [ ] NSG: 22, 4200, 8180
- [ ] `.env` với `PUBLIC_HOST` = IP/DNS thật
- [ ] `docker compose up --build -d`
- [ ] Chạy `configure-sam-shop.ps1` với `KEYCLOAK_URL` + `FE_PUBLIC_URL`
- [ ] Mở http://&lt;PUBLIC_HOST&gt;:4200 và đăng nhập thử

---

*Tài liệu đi kèm thay đổi repo: `PUBLIC_HOST` trong `docker-compose.yml`, build arg frontend `KEYCLOAK_PUBLIC_URL`, script `scripts/azure-vm-deploy.sh`.*

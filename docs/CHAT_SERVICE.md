# Chat Service

## Chức năng

- **USER** chat với **EMPLOYEE / ADMIN** (hỗ trợ khách hàng)
- Lưu lịch sử tin nhắn PostgreSQL (`chat_db`)
- **WebSocket** real-time qua Gateway
- Phân quyền **Keycloak JWT** (roles: USER, EMPLOYEE, ADMIN)

## Kiến trúc

```
FE ──HTTP/WS──► Gateway :8088 ──► chat-service :8085
                                    └── postgres-chat :5438
```

## API REST (`/api/chat`)

| Method | Path | Role | Mô tả |
|--------|------|------|--------|
| GET | `/conversations/me` | USER | Lấy/tạo hội thoại của mình |
| GET | `/conversations` | EMPLOYEE, ADMIN | Inbox tất cả hội thoại |
| GET | `/conversations/{id}/messages` | USER (own), Staff | Lịch sử tin nhắn |
| POST | `/conversations/{id}/messages` | USER, Staff | Gửi tin (REST fallback) |

## WebSocket

- **Không đi qua Gateway** (Spring Cloud Gateway MVC không proxy WebSocket upgrade).
- Docker FE (`:4200`): `ws://localhost:4200/api/chat/ws?access_token=<JWT>` → nginx → `chat-service:8085`
- Dev `ng serve`: `ws://localhost:8085/api/chat/ws?access_token=<JWT>` (`environment.chatWsUrl`)
- Client gửi JSON:
  - `{"type":"JOIN","conversationId":1}`
  - `{"type":"SEND","conversationId":1,"content":"Xin chào"}`
- Server push: `{"type":"MESSAGE","payload":{...}}`

## Chạy Docker

```bash
docker compose up -d chat-service gateway-service frontend
```

## Tài khoản test

| User | Role | Màn hình |
|------|------|----------|
| customer1 | USER | `/chat` |
| staff1 / admin1 | EMPLOYEE / ADMIN | `/admin/chat` |

# FE Integration Doc - Chat Session Flow & APIs

Tai lieu nay mo ta day du luong va API de FE implement module chat theo kieu ChatGPT/Gemini tren backend hien tai.

## 1) Tong quan

- Base path: `/chat-sessions`
- Auth: Bearer token (JWT)
- Response wrapper: `ApiResponse<T>`

Mau chung:

```json
{
  "code": 1000,
  "message": "optional",
  "result": {}
}
```

## 2) Data model FE can store

### ChatSessionResponse

```json
{
  "id": "uuid",
  "userId": "uuid",
  "title": "New Chat",
  "status": "ACTIVE",
  "model": "gemini-2.5-flash",
  "totalMessages": 0,
  "totalWords": 0,
  "lastMessageAt": "2026-03-25T10:00:00Z",
  "createdAt": "2026-03-25T10:00:00Z",
  "updatedAt": "2026-03-25T10:00:00Z"
}
```

### ChatMessageResponse

```json
{
  "id": "uuid",
  "sessionId": "uuid",
  "userId": "uuid",
  "role": "USER",
  "content": "Noi dung tin nhan",
  "wordCount": 10,
  "model": "gemini-2.5-flash",
  "latencyMs": 1200,
  "sequenceNo": 2,
  "createdAt": "2026-03-25T10:00:10Z"
}
```

### ChatExchangeResponse (sau khi gui prompt)

```json
{
  "sessionId": "uuid",
  "userMessage": { "...ChatMessageResponse" },
  "assistantMessage": { "...ChatMessageResponse" },
  "memory": {
    "wordLimit": 1000,
    "currentWords": 734,
    "messageCount": 8,
    "trimmed": false
  }
}
```

## 3) Luong FE de implement

### Luong A: Mo trang chat

1. Goi `GET /chat-sessions?page=0&size=20&sortBy=lastMessageAt&direction=DESC`
2. Render danh sach session ben trai.
3. Neu co session dau tien: goi `GET /chat-sessions/{sessionId}/messages?page=0&size=50&sortBy=createdAt&direction=ASC`.

### Luong B: Tao session moi

1. FE bam `New chat` -> goi `POST /chat-sessions`.
2. Add session moi vao danh sach.
3. Chuyen UI sang session vua tao.

### Luong C: Gui prompt

1. FE goi `POST /chat-sessions/{sessionId}/messages` voi prompt.
2. Tu response `ChatExchangeResponse`:
   - append `userMessage`
   - append `assistantMessage`
3. Update session metadata UI (`lastMessageAt`, `totalMessages`).

### Luong D: Xem lai session cu

1. User click session.
2. FE goi `GET /chat-sessions/{sessionId}` de lay metadata.
3. FE goi `GET /chat-sessions/{sessionId}/messages` de lay history.

### Luong E: Rename / Archive / Delete

1. Rename: `PATCH /chat-sessions/{sessionId}`
2. Archive: `PATCH /chat-sessions/{sessionId}/archive`
3. Delete: `DELETE /chat-sessions/{sessionId}`

## 4) Chi tiet API

## 4.1 Tao session

- Method: `POST`
- URL: `/chat-sessions`

Request body:

```json
{
  "title": "On thi dao ham",
  "model": "gemini-2.5-flash"
}
```

Rules:
- `title`: optional, max 200. Neu rong backend set `New Chat`.
- `model`: optional, max 100. Neu rong backend set `gemini-2.5-flash`.

Response result: `ChatSessionResponse`.

## 4.2 Danh sach session

- Method: `GET`
- URL: `/chat-sessions`

Query params:
- `status`: optional (`ACTIVE`, `ARCHIVED`)
- `keyword`: optional (search theo title)
- `page`: default `0`
- `size`: default `20`
- `sortBy`: default `lastMessageAt`
- `direction`: default `DESC`

Response result: Spring Page

```json
{
  "content": [{ "...ChatSessionResponse" }],
  "number": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

## 4.3 Lay metadata 1 session

- Method: `GET`
- URL: `/chat-sessions/{sessionId}`

Path:
- `sessionId`: UUID

Response result: `ChatSessionResponse`.

## 4.4 Lay history message

- Method: `GET`
- URL: `/chat-sessions/{sessionId}/messages`

Query params:
- `page`: default `0`
- `size`: default `50`
- `sortBy`: default `createdAt`
- `direction`: default `ASC`

Response result: Page of `ChatMessageResponse`.

## 4.5 Gui prompt trong session

- Method: `POST`
- URL: `/chat-sessions/{sessionId}/messages`

Request body:

```json
{
  "prompt": "Giai thich dao ham la gi?",
  "temperature": 0.4,
  "maxOutputTokens": 800
}
```

Rules:
- `prompt`: required, not blank, max 20000 chars.
- Session `ARCHIVED` se bi chan.

Response result: `ChatExchangeResponse`.

## 4.6 Rename session

- Method: `PATCH`
- URL: `/chat-sessions/{sessionId}`

Request body:

```json
{
  "title": "On dao ham co ban"
}
```

Rules:
- `title`: required, max 200.

Response result: `ChatSessionResponse`.

## 4.7 Archive session

- Method: `PATCH`
- URL: `/chat-sessions/{sessionId}/archive`

Request body: none.

Response result: `ChatSessionResponse` voi `status = ARCHIVED`.

## 4.8 Delete session

- Method: `DELETE`
- URL: `/chat-sessions/{sessionId}`

Request body: none.

Response:

```json
{
  "code": 1000,
  "message": "Session deleted"
}
```

## 4.9 Memory info (debug panel)

- Method: `GET`
- URL: `/chat-sessions/{sessionId}/memory`

Response result:

```json
{
  "wordLimit": 1000,
  "currentWords": 700,
  "messageCount": 8,
  "trimmed": false
}
```

## 5) Error handling FE

Error response format:

```json
{
  "code": 1136,
  "message": "Chat session is archived"
}
```

Chat-related error codes:
- `1134` CHAT_SESSION_NOT_FOUND
- `1135` CHAT_SESSION_ACCESS_DENIED
- `1136` CHAT_SESSION_ARCHIVED
- `1137` CHAT_PROMPT_EMPTY
- `1138` CHAT_MESSAGE_NOT_FOUND
- `1139` CHAT_AI_CALL_FAILED

## 6) FE implementation notes

- Optimistic UI:
  - Co the hien ngay USER bubble tam thoi, sau do reconcile theo `userMessage` tu response.
- Paging history:
  - Mac dinh load trang moi nhat theo `ASC` + page tang dan, hoac doi `DESC` de infinite scroll nguoc.
- Session ordering:
  - Sau moi lan chat thanh cong, move session len dau list theo `lastMessageAt`.
- Archive UX:
  - Disable input box khi session `ARCHIVED`.
- Word-limit memory:
  - Gioi han 1000 words la in-memory context tren backend, FE khong can trim history local.

## 7) Curl mau de test nhanh

Create session:

```bash
curl -X POST "http://localhost:8080/chat-sessions" \\
  -H "Authorization: Bearer <TOKEN>" \\
  -H "Content-Type: application/json" \\
  -d '{"title":"On thi dao ham","model":"gemini-2.5-flash"}'
```

Send message:

```bash
curl -X POST "http://localhost:8080/chat-sessions/<SESSION_ID>/messages" \\
  -H "Authorization: Bearer <TOKEN>" \\
  -H "Content-Type: application/json" \\
  -d '{"prompt":"Giai thich dao ham la gi?"}'
```

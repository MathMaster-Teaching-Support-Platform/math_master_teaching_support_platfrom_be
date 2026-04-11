# API Contract — Admin Transactions (Quản lý Giao dịch)

**Ngày tạo:** 2026-04-11  
**Người tạo:** BE Team  
**Phản hồi cho:** FE spec request (Admin Transactions)  
**Trạng thái:** Chờ FE confirm

---

## Tổng quan

- ✅ Đã có sẵn endpoint admin transactions trước đó: `GET /admin/transactions`
- ⚠️ Đã cập nhật endpoint list để khớp FE spec: thêm `status`, `search`, và bổ sung field `userEmail`, `orderCode`
- 🔨 Mới implement trong lần này:
  - `GET /admin/transactions/stats`
  - `GET /admin/transactions/{transactionId}`
  - `GET /admin/transactions/export` (CSV)
- ⚠️ Project hiện tại **không có prefix `/api`**. Endpoint thật là `/admin/...`

---

## 1. Danh sách giao dịch (Transaction List)

**Trạng thái:** ⚠️ Đã có nhưng cần sửa

### Endpoint

```http
GET /admin/transactions
```

### Auth

- Header: `Authorization: Bearer <JWT>`
- Role được phép: `ADMIN`

### Request

**Query params:**

| Param   | Type   | Bắt buộc | Mặc định | Mô tả |
|---------|--------|----------|----------|------|
| page    | int    | ❌ | 0  | Trang hiện tại (0-based) |
| size    | int    | ❌ | 10 | Số dòng mỗi trang |
| sortBy  | string | ❌ | `createdAt` | Field sort |
| order   | string | ❌ | `DESC` | `ASC` / `DESC` |
| status  | string | ❌ | (all) | `completed` \| `pending` \| `failed` |
| search  | string | ❌ | null | Search theo `userName`, `userEmail`, `orderCode`, `planName` |

**Mapping status FE -> BE:**

- `completed` -> `SUCCESS`
- `pending` -> `PENDING`, `PROCESSING`
- `failed` -> `FAILED`, `CANCELLED`

### Response — 200 OK

```json
{
  "code": 1000,
  "result": {
    "items": [
      {
        "id": "d5d8f8f4-c3e7-4d06-a5ff-c0f4d242b5cc",
        "userId": "c3151d2d-f7da-4f1a-bcc4-4f8846f8f1f3",
        "userName": "Nguyen Van A",
        "userEmail": "vana@example.com",
        "planId": null,
        "planName": "Nạp ví qua PayOS",
        "amount": 99000,
        "status": "completed",
        "paymentMethod": "payos",
        "orderCode": 1029384756,
        "createdAt": "2026-04-11T08:23:11Z"
      }
    ],
    "totalItems": 150,
    "totalPages": 15,
    "currentPage": 0
  }
}
```

### Response — Lỗi phổ biến

```json
// 401 Unauthorized
{ "code": 1006, "message": "Unauthenticated" }

// 403 Forbidden
{ "code": 1007, "message": "Access denied" }

// 500 Internal Server Error (ví dụ sortBy không hợp lệ)
{ "code": 9999, "message": "Uncategorized error" }
```

### Validation BE áp dụng

- `status` nếu khác `completed|pending|failed` -> BE fallback về `all` (không reject 400)
- `page`, `size` là số nguyên; nếu FE gửi sai kiểu sẽ bị Spring bind error
- `createdAt` trả về kiểu `Instant` (ISO-8601 UTC)
- `amount` trả về `BigDecimal` từ DB, FE nên render như integer VND khi hiển thị

### Ghi chú

- Phân trang/filter/search xử lý server-side
- Chưa có cache cho endpoint này
- Chưa có realtime/webhook push cho admin transaction list
- `paymentMethod` hiện tại luôn là `payos`

---

## 2. Thống kê tổng quan (Transaction Stats)

**Trạng thái:** 🔨 Mới implement

### Endpoint

```http
GET /admin/transactions/stats
```

### Auth

- Header: `Authorization: Bearer <JWT>`
- Role được phép: `ADMIN`

### Request

- Không có query params

### Response — 200 OK

```json
{
  "code": 1000,
  "result": {
    "total": 150,
    "completed": 110,
    "pending": 25,
    "failed": 15,
    "totalRevenue": 45670000
  }
}
```

### Response — Lỗi phổ biến

```json
// 401 Unauthorized
{ "code": 1006, "message": "Unauthenticated" }

// 403 Forbidden
{ "code": 1007, "message": "Access denied" }
```

### Validation BE áp dụng

- `totalRevenue` chỉ tính transaction status = `SUCCESS`
- `pending` gồm `PENDING` + `PROCESSING`
- `failed` gồm `FAILED` + `CANCELLED`

### Ghi chú

- Có thể gọi endpoint này song song với list endpoint
- Chưa gộp stats vào `/admin/transactions` để giữ payload list nhẹ và tách concern

---

## 3. Chi tiết giao dịch (Transaction Detail)

**Trạng thái:** 🔨 Mới implement

### Endpoint

```http
GET /admin/transactions/{transactionId}
```

### Auth

- Header: `Authorization: Bearer <JWT>`
- Role được phép: `ADMIN`

### Request

**Path params:**

| Param | Type | Bắt buộc | Mô tả |
|-------|------|----------|------|
| transactionId | UUID | ✅ | ID transaction |

### Response — 200 OK

```json
{
  "code": 1000,
  "result": {
    "id": "d5d8f8f4-c3e7-4d06-a5ff-c0f4d242b5cc",
    "userId": "c3151d2d-f7da-4f1a-bcc4-4f8846f8f1f3",
    "userName": "Nguyen Van A",
    "userEmail": "vana@example.com",
    "planId": null,
    "planName": "Nạp ví qua PayOS",
    "amount": 99000,
    "status": "completed",
    "paymentMethod": "payos",
    "orderCode": 1029384756,
    "createdAt": "2026-04-11T08:23:11Z"
  }
}
```

### Response — Lỗi phổ biến

```json
// 404 Not Found
{ "code": 1030, "message": "Transaction not found" }

// 400 Bad Request (UUID sai format)
{ "code": 1008, "message": "Invalid request" }
```

### Validation BE áp dụng

- `transactionId` phải là UUID hợp lệ
- Nếu không tồn tại transaction -> `TRANSACTION_NOT_FOUND` (code 1030)

### Ghi chú

- FE có thể tiếp tục dùng data từ list cho modal đơn giản
- Endpoint detail nên dùng khi modal cần refresh độc lập hoặc mở deep-link

---

## 4. Xuất CSV (Export CSV)

**Trạng thái:** 🔨 Mới implement

### Endpoint

```http
GET /admin/transactions/export
```

### Auth

- Header: `Authorization: Bearer <JWT>`
- Role được phép: `ADMIN`

### Request

**Query params:**

| Param  | Type   | Bắt buộc | Mô tả |
|--------|--------|----------|------|
| status | string | ❌ | `completed` \| `pending` \| `failed` |
| search | string | ❌ | Cùng logic search với list |

### Response — 200 OK

- `Content-Type: text/csv; charset=UTF-8`
- `Content-Disposition: attachment; filename="transactions.csv"`

CSV columns:

```csv
id,userId,userName,userEmail,planName,amount,status,paymentMethod,orderCode,createdAt
```

### Response — Lỗi phổ biến

```json
// 401 Unauthorized
{ "code": 1006, "message": "Unauthenticated" }

// 403 Forbidden
{ "code": 1007, "message": "Access denied" }
```

### Validation BE áp dụng

- `status` mapping giống list endpoint
- `search` optional

### Ghi chú

- Export xử lý server-side
- FE chỉ cần trigger download Blob từ response binary

---

## 5. "Làm mới" Button

**Trạng thái:** ✅ Đã có (không cần endpoint mới)

### FE action đề xuất

- Re-fetch:
  - `GET /admin/transactions`
  - `GET /admin/transactions/stats`

---

## Danh sách thay đổi so với đề xuất của FE

| Feature | Thay đổi | Lý do |
|--------|----------|------|
| Tất cả endpoint | Dùng `/admin/...` thay vì `/api/admin/...` | Project không cấu hình prefix `/api` |
| List response | Wrapper hiện tại dùng `code` + `result` thay vì `success` + `data` | Chuẩn response thống nhất toàn hệ thống hiện tại |
| Transaction list item | Có thêm `planId` (null), bổ sung `userEmail`, `orderCode` | Tương thích DTO cũ + đáp ứng FE hiển thị |
| Status filter | `pending` map sang 2 trạng thái DB (`PENDING`, `PROCESSING`) | Khớp domain enum hiện tại |
| Stats endpoint | Tách riêng endpoint `/admin/transactions/stats` | Dễ cache/tối ưu và rõ responsibility |

---

## Checklist trước khi FE integrate

- [ ] BE đã deploy lên môi trường staging
- [ ] Swagger/Postman collection đã cập nhật
- [ ] FE cập nhật base URL từ `/api/admin/*` -> `/admin/*`
- [ ] FE mapping response từ `result` thay vì `data`
- [ ] FE test đủ 3 trạng thái filter: `completed`, `pending`, `failed`

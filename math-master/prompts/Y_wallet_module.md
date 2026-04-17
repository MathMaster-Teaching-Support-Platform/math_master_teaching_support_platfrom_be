# API Contract — Wallet Module (StudentWallet)

**Ngày tạo:** 2026-04-17  
**Người tạo:** BE Team  
**Phản hồi cho:** X.md — Wallet Module (StudentWallet)  
**Trạng thái:** Đã implement, sẵn sàng để FE integrate

---

## 1. Tổng số tiền đã nạp — `GET /wallet/my-wallet`

**Trạng thái:** 🔨 Đã cập nhật (bổ sung 3 field mới vào response hiện có)

### Endpoint

```
GET /wallet/my-wallet
```

### Auth

- Header: `Authorization: Bearer <JWT>`
- Role được phép: bất kỳ user đã đăng nhập

### Request

Không có body / query param.

### Response — 200 OK

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "walletId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "userId": "7b1e2c09-4321-4f0a-b882-9d1234567890",
    "balance": 31000,
    "totalDeposited": 50000,
    "totalSpent": 19000,
    "transactionCount": 5,
    "status": "ACTIVE",
    "createdAt": "2026-01-10T08:00:00Z",
    "updatedAt": "2026-04-17T12:30:00Z"
  }
}
```

### Định nghĩa các field mới

| Field              | Kiểu     | Mô tả                                                                                 |
| ------------------ | -------- | ------------------------------------------------------------------------------------- |
| `totalDeposited`   | `number` | Tổng tất cả giao dịch `type=DEPOSIT` và `status=SUCCESS` — không phụ thuộc phân trang |
| `totalSpent`       | `number` | Tổng tất cả giao dịch `type=PAYMENT` và `status=SUCCESS`                              |
| `transactionCount` | `number` | Tổng số giao dịch mọi status, mọi type                                                |

### Bất biến đảm bảo bởi BE

```
totalDeposited - totalSpent >= balance  (có thể sai nếu có REFUND — hiện chưa dùng)
```

### Response — Lỗi phổ biến

```json
// 401 Unauthorized — token hết hạn / không hợp lệ
{ "code": 1006, "message": "Unauthenticated" }

// 404 — ví không tồn tại (hiếm — getOrCreateWallet tự tạo)
{ "code": 1009, "message": "Wallet not found" }
```

### Ghi chú

- `GET /wallet/my-wallet` tự động **tạo ví** nếu user chưa có (`getOrCreateWallet`), không bao giờ trả 404 trừ lỗi DB.
- Không tạo endpoint `/wallet/summary` riêng — bổ sung vào response hiện có để FE không phải gọi 2 endpoint.

---

## 2. Transaction Status — Enum chính xác & Auto-cancel sau 15 phút

**Trạng thái:** 🔨 Đã implement auto-cancel scheduler + thêm `expiresAt` vào response

### 2.1 Enum chính xác của `TransactionStatus`

```
PENDING     — Chờ thanh toán (link đang hoạt động)
PROCESSING  — PayOS đang xử lý (trung gian, hiếm gặp)
SUCCESS     — Thanh toán thành công ✅
FAILED      — Thanh toán thất bại (lỗi từ PayOS)
CANCELLED   — Đã hủy (user hủy hoặc hết hạn 15 phút)
```

> ⚠️ **BREAKING đối với FE hiện tại:** Trạng thái thành công là `SUCCESS`, **KHÔNG PHẢI** `COMPLETED`. FE cần cập nhật `normalizeStatus`:

```ts
// FE phải dùng mapping này — KHÔNG dùng string-match nữa
type TransactionStatus =
  | "PENDING"
  | "PROCESSING"
  | "SUCCESS"
  | "FAILED"
  | "CANCELLED";

const STATUS_MAP: Record<
  TransactionStatus,
  "pending" | "completed" | "failed"
> = {
  PENDING: "pending",
  PROCESSING: "pending",
  SUCCESS: "completed",
  FAILED: "failed",
  CANCELLED: "failed", // hoặc tách 'cancelled' nếu FE muốn hiện "Đã hủy"
};
```

### 2.2 Auto-cancel PENDING sau 15 phút

- **BE đã implement** scheduler `PendingTransactionCancelScheduler` chạy **mỗi 60 giây**.
- Khi tạo giao dịch deposit, BE set `expiresAt = createdAt + 15 phút`.
- Scheduler tìm tất cả PENDING có `expiresAt < now` và đổi sang `CANCELLED`.
- FE **không cần** tự track time — chỉ cần poll `GET /payment/order/{orderCode}/status` (xem mục 5).

### 2.3 `CANCELLED` vs `FAILED`

| Status      | Nguyên nhân                                                  |
| ----------- | ------------------------------------------------------------ |
| `CANCELLED` | User chủ động hủy trên PayOS **hoặc** hết 15 phút (timeout)  |
| `FAILED`    | Lỗi thanh toán từ cổng PayOS (sai tài khoản, hết tiền, v.v.) |

Nếu FE muốn hiển thị "Hết hạn" riêng — cần kiểm tra `expiresAt` trong response:

```ts
// Gợi ý logic FE:
if (
  tx.status === "CANCELLED" &&
  tx.expiresAt &&
  new Date(tx.expiresAt) < new Date(tx.updatedAt)
) {
  label = "Hết hạn";
} else if (tx.status === "CANCELLED") {
  label = "Đã hủy";
}
```

---

## 3. Transaction List — Phân trang

**Trạng thái:** ✅ Đã có — trả `Page` object (Spring Data)

### Endpoint

```
GET /wallet/transactions?page=0&size=10
GET /wallet/transactions/status/{status}?page=0&size=10
```

### Auth

- Header: `Authorization: Bearer <JWT>`

### Query params

| Param  | Type    | Mặc định | Mô tả             |
| ------ | ------- | -------- | ----------------- |
| `page` | integer | `0`      | 0-indexed         |
| `size` | integer | `10`     | Số item mỗi trang |

### Response — 200 OK

```json
{
  "code": 1000,
  "result": {
    "content": [
      {
        "transactionId": "uuid-...",
        "walletId": "uuid-...",
        "orderCode": 1713340012345,
        "amount": 50000,
        "type": "DEPOSIT",
        "status": "SUCCESS",
        "description": "Nạp tiền vào ví",
        "paymentLinkId": "payos-link-id",
        "referenceCode": "FT2026041700001",
        "transactionDate": "2026-04-17T10:00:00Z",
        "expiresAt": null,
        "createdAt": "2026-04-17T09:45:00Z",
        "updatedAt": "2026-04-17T10:00:05Z"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "sort": { "sorted": true, "direction": "DESC", "property": "createdAt" }
    },
    "totalElements": 23,
    "totalPages": 3,
    "last": false,
    "first": true,
    "numberOfElements": 10,
    "size": 10,
    "number": 0
  }
}
```

> **Lưu ý serialization:** Spring Data `Page<T>` được Jackson serialize thành object có `content[]` + `totalElements` + `totalPages`. FE **không cần** handle 2 case (array vs object) nữa — **luôn luôn** là Page object.

### Xử lý phía FE (đơn giản hóa)

```ts
// Xóa đoạn code handle 2 case cũ, chỉ cần:
const list = result.content;
const totalElements = result.totalElements;
const totalPages = result.totalPages;
```

### Path param cho filter theo status

```
GET /wallet/transactions/status/PENDING
GET /wallet/transactions/status/SUCCESS
GET /wallet/transactions/status/CANCELLED
```

Status phải khớp **chính xác** với enum `TransactionStatus` (case-sensitive UPPER).

---

## 4. WalletTransaction — Shape đầy đủ

**Trạng thái:** 🔨 Đã cập nhật (thêm `expiresAt`)

### TypeScript Interface chính xác để FE dùng

```ts
interface WalletTransaction {
  transactionId: string; // UUID
  walletId: string; // UUID
  orderCode: number; // long — mã đơn hàng PayOS, dùng để hiển thị cho user
  amount: number; // luôn dương, đơn vị VND
  type: TransactionType; // enum
  status: TransactionStatus; // enum
  description?: string;
  paymentLinkId?: string; // ID link PayOS (nội bộ)
  referenceCode?: string; // Mã tham chiếu từ PayOS sau khi SUCCESS
  transactionDate?: string; // ISO 8601 — thời điểm PayOS confirm
  expiresAt?: string; // ISO 8601 — chỉ có khi PENDING, sau 15 phút sẽ bị CANCELLED
  createdAt: string; // ISO 8601
  updatedAt: string; // ISO 8601
}

type TransactionType = "DEPOSIT" | "WITHDRAWAL" | "PAYMENT" | "REFUND";
type TransactionStatus =
  | "PENDING"
  | "PROCESSING"
  | "SUCCESS"
  | "FAILED"
  | "CANCELLED";
```

### Giải thích các field theo yêu cầu FE

| Field FE hỏi    | Trạng thái BE      | Ghi chú                                                     |
| --------------- | ------------------ | ----------------------------------------------------------- |
| `type`          | ✅ Có              | Enum `DEPOSIT \| WITHDRAWAL \| PAYMENT \| REFUND`           |
| `orderCode`     | ✅ Có              | `number` (long), luôn có với DEPOSIT                        |
| `expiresAt`     | 🔨 Mới thêm        | Có với PENDING DEPOSIT, `null` với SUCCESS/FAILED/CANCELLED |
| `cancelledAt`   | ❌ Không implement | Dùng `updatedAt` khi `status=CANCELLED` thay thế            |
| `paymentMethod` | ⚠️ Không có        | PayOS không trả field này về BE — xem ghi chú bên dưới      |

> `paymentMethod`: PayOS không cung cấp thông tin phương thức thanh toán (ATM/QR/Ví) trong webhook. Field này sẽ luôn `undefined`. Nếu FE cần hiển thị, có thể để mặc định "PayOS".

---

## 5. Deposit — Polling trạng thái đơn

**Trạng thái:** 🔨 Mới implement

### Endpoint poll trạng thái

```
GET /payment/order/{orderCode}/status
```

### Auth

- Header: `Authorization: Bearer <JWT>`
- Chỉ user sở hữu giao dịch đó mới được xem (403 nếu sai user)

### Path param

| Param       | Type   | Mô tả                                       |
| ----------- | ------ | ------------------------------------------- |
| `orderCode` | `long` | Lấy từ response của `POST /payment/deposit` |

### Response — 200 OK

```json
{
  "code": 1000,
  "result": {
    "transactionId": "uuid-...",
    "walletId": "uuid-...",
    "orderCode": 1713340012345,
    "amount": 50000,
    "type": "DEPOSIT",
    "status": "PENDING",
    "expiresAt": "2026-04-17T10:00:00Z",
    "createdAt": "2026-04-17T09:45:00Z",
    "updatedAt": "2026-04-17T09:45:00Z"
  }
}
```

### Response — Lỗi

```json
// 403 Forbidden — không phải giao dịch của user này
{ "code": 1007, "message": "You do not have permission" }

// 404 Not Found — orderCode không tồn tại
{ "code": 1052, "message": "Transaction not found" }
```

### Khuyến nghị FE: logic polling

```ts
const pollOrderStatus = async (
  orderCode: number,
  intervalMs = 5000,
  maxRetries = 36,
) => {
  // 36 lần × 5s = 3 phút polling window
  for (let i = 0; i < maxRetries; i++) {
    const res = await api.get(`/payment/order/${orderCode}/status`);
    const { status } = res.data.result;

    if (status === "SUCCESS") {
      // reload wallet balance + transaction list
      break;
    }
    if (status === "FAILED" || status === "CANCELLED") {
      // hiển thị lỗi
      break;
    }
    // PENDING/PROCESSING → tiếp tục poll
    await sleep(intervalMs);
  }
};
```

### Luồng hoàn chỉnh Deposit

```
POST /payment/deposit
  → nhận { checkoutUrl, orderCode, paymentLinkId }
  → mở tab mới checkoutUrl
  → bắt đầu poll GET /payment/order/{orderCode}/status mỗi 5s
  → khi status=SUCCESS → reload GET /wallet/my-wallet (balance + stats mới)
  → khi status=CANCELLED → hiển thị "Giao dịch hết hạn / đã hủy"
```

> **Webhook:** BE đã có `POST /payment/webhook` nhận callback từ PayOS, tự động đổi status + cộng tiền vào ví. FE không cần xử lý webhook — chỉ cần poll để biết kết quả.

---

## Danh sách thay đổi so với đề xuất FE

| Feature             | Thay đổi                                                              | Lý do                                                                          |
| ------------------- | --------------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| Wallet summary      | Bổ sung vào `GET /wallet/my-wallet` thay vì tạo `/wallet/summary` mới | Giảm số lượng API call, FE vẫn dùng 1 endpoint                                 |
| `TransactionStatus` | Thành công là `SUCCESS`, không phải `COMPLETED`                       | Đây là enum BE thực tế, FE cần update type                                     |
| `expiresAt`         | Thêm field vào `TransactionResponse`                                  | Mới implement cùng lần này                                                     |
| `cancelledAt`       | Không implement                                                       | Dùng `updatedAt` khi `status=CANCELLED` — tránh thêm column DB không cần thiết |
| `paymentMethod`     | Không có                                                              | PayOS không expose thông tin này trong webhook                                 |
| Auto-cancel         | BE scheduler 60s, không cần FE tự đếm                                 | Đơn giản hóa phía FE                                                           |

---

## Checklist trước khi FE integrate

- [x] Migration `V4__Add_Expires_At_To_Transactions.sql` đã tạo
- [x] `PendingTransactionCancelScheduler` đã deploy — auto-cancel mỗi 60s
- [x] `GET /wallet/my-wallet` trả thêm `totalDeposited`, `totalSpent`, `transactionCount`
- [x] `GET /payment/order/{orderCode}/status` đã có
- [x] `TransactionResponse` đã có `expiresAt`
- [ ] BE cần deploy lên staging trước khi FE test
- [ ] Seed data test: tạo 1 user có ví với ≥5 giao dịch SUCCESS

# API Contract — AdminDashboard

**Ngày tạo:** 2026-04-11  
**Người tạo:** BE Team  
**Phản hồi cho:** X.md (FE spec — AdminDashboard)  
**Trạng thái:** Chờ FE confirm

---

## ⚠️ Lưu ý quan trọng trước khi tích hợp

### Base URL

```
http://localhost:8080
```

Không có prefix `/api`. Mọi endpoint đều bắt đầu ngay từ root.

### Response format thực tế của BE

BE **không** trả `"success": true`. Cấu trúc chuẩn:

```json
{
  "code": 1000,
  "result": { ... }
}
```

| Field     | Ý nghĩa                                          |
| --------- | ------------------------------------------------ |
| `code`    | `1000` = thành công; giá trị khác = lỗi          |
| `message` | Có khi lỗi hoặc thao tác đặc biệt                |
| `result`  | Payload thực, tương đương `result` trong spec FE |

FE cần đọc `response.result` thay vì `response.data`.  
FE cần kiểm tra `response.code === 1000` thay vì `response.success === true`.

---

## 1. Admin Identity (Header / Layout)

**Trạng thái:** ✅ Đã có (endpoint khác tên)

### Endpoint

```
GET /users/my-info
```

### Auth

- Header: `Authorization: Bearer <JWT>`
- Role được phép: Tất cả authenticated user (không cần role cụ thể)

### Request

Không có body hay query params.

### Response — 200 OK

```json
{
  "code": 1000,
  "result": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "userName": "admin_system",
    "fullName": "Admin System",
    "email": "admin@mathmaster.vn",
    "phoneNumber": null,
    "gender": null,
    "avatar": "https://storage.example.com/avatars/admin.png",
    "dob": null,
    "code": null,
    "status": "ACTIVE",
    "banReason": null,
    "banDate": null,
    "roles": ["ADMIN"],
    "createdDate": "2024-01-01T00:00:00Z",
    "createdBy": null,
    "updatedDate": "2024-01-01T00:00:00Z",
    "updatedBy": null
  }
}
```

### Response — Lỗi phổ biến

```json
// 401 Unauthorized
{ "code": 1006, "message": "Unauthenticated" }
```

### Validation BE áp dụng

- Endpoint lấy thông tin từ JWT subject (userId) trong SecurityContext
- `roles` là `Set<String>` (ví dụ: `["ADMIN"]`)

### Ghi chú — Thay đổi so với FE spec

| Field spec FE   | Field thực tế BE | Ghi chú                                        |
| --------------- | ---------------- | ---------------------------------------------- |
| `name`          | `fullName`       | FE cần đọc `result.fullName`                   |
| `role` (string) | `roles` (array)  | BE trả array, FE đọc `result.roles[0]`         |
| `avatar`        | `avatar`         | URL đầy đủ nếu upload MinIO; null nếu chưa set |

---

## 2. Notification Count (Header Badge)

**Trạng thái:** ⚠️ Đã có nhưng cần sửa (key thay đổi) — **đã fix trong commit này**

### Endpoint

```
GET /v1/notifications/unread-count
```

### Auth

- Header: `Authorization: Bearer <JWT>`
- Role được phép: Tất cả authenticated user

### Response — 200 OK

> ⚠️ **BREAKING thay đổi key**: Trước đây trả `"count"`, đã đổi thành `"unreadCount"` để khớp FE spec.

```json
{
  "unreadCount": 8
}
```

> ⚠️ Endpoint này **không** wrap trong `ApiResponse`. Trả thẳng JSON object.  
> FE đọc `response.unreadCount` (không có `code` hay `result`).

### Response — Lỗi phổ biến

```json
// 401 Unauthorized
{ "status": 401, "error": "Unauthorized" }
```

### Ghi chú

- Không gộp vào `/users/my-info` — giữ tách riêng để tiện polling/cache
- Không có cache, trả real-time từ DB

---

## 3. Stats Grid — Tổng quan hệ thống

**Trạng thái:** 🔨 Mới implement

### Endpoint

```
GET /admin/dashboard/stats
```

### Auth

- Header: `Authorization: Bearer <JWT>`
- Role được phép: `ADMIN` only

### Request

**Query params:**
| Param | Type | Bắt buộc | Mô tả |
|---------|--------|----------|------------------------------------|
| `month` | string | ❌ | Format `YYYY-MM`, mặc định: tháng hiện tại |

### Response — 200 OK

```json
{
  "code": 1000,
  "result": {
    "totalUsers": 1930,
    "totalUsersGrowthPercent": 12.0,
    "monthlyRevenue": 45200000,
    "monthlyRevenueGrowthPercent": 8.0,
    "activeEnrollments": 680,
    "activeEnrollmentsGrowthPercent": 15.0,
    "totalTransactions": 1245,
    "totalTransactionsGrowthPercent": 5.0,
    "month": "2026-04"
  }
}
```

### Thay đổi so với FE spec

| Field FE spec                      | Field BE thực tế                 | Lý do                                                               |
| ---------------------------------- | -------------------------------- | ------------------------------------------------------------------- |
| `activeSubscriptions`              | `activeEnrollments`              | Hệ thống không có "gói đăng ký" — tương đương là active enrollments |
| `activeSubscriptionsGrowthPercent` | `activeEnrollmentsGrowthPercent` | Đổi tên theo field trên                                             |

> **[FE note]** `activeEnrollments` = số lượng enrollments đang ở trạng thái `ACTIVE`. Không phải "subscription plan". FE cần cập nhật label nếu muốn hiển thị chính xác.

### Ghi chú

- `monthlyRevenue` tính từ tổng các transaction `SUCCESS` trong tháng, đơn vị: VNĐ tuyệt đối
- `totalUsersGrowthPercent` so sánh số user mới đăng ký trong tháng hiện tại vs tháng trước
- Âm là giảm, dương là tăng

---

## 4. Recent Users — Người dùng mới

**Trạng thái:** 🔨 Mới implement (endpoint mới, dùng data từ `/users/page` hiện có)

### Endpoint

```
GET /users/admin/recent
```

### Auth

- Header: `Authorization: Bearer <JWT>`
- Role được phép: `ADMIN` only

### Request

**Query params:**
| Param | Type | Bắt buộc | Mô tả |
|---------|--------|----------|----------------------|
| `page` | number | ❌ | Mặc định: 0 (0-indexed) |
| `size` | number | ❌ | Mặc định: 10 |

> **[FE note]** Phân trang dùng 0-indexed (`page=0` = trang đầu tiên).

### Response — 200 OK

```json
{
  "code": 1000,
  "result": {
    "content": [
      {
        "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "userName": "nguyen_van_a",
        "fullName": "Nguyễn Văn A",
        "email": "nguyenvana@example.com",
        "phoneNumber": null,
        "gender": "MALE",
        "avatar": null,
        "dob": null,
        "code": null,
        "status": "ACTIVE",
        "banReason": null,
        "banDate": null,
        "roles": ["STUDENT"],
        "createdDate": "2026-04-10T07:23:00Z",
        "createdBy": null,
        "updatedDate": "2026-04-10T07:23:00Z",
        "updatedBy": null
      }
    ],
    "totalElements": 1930,
    "totalPages": 193,
    "size": 10,
    "number": 0,
    "first": true,
    "last": false
  }
}
```

### Thay đổi so với FE spec

| Field FE spec     | Field BE thực tế   | Ghi chú                                                                          |
| ----------------- | ------------------ | -------------------------------------------------------------------------------- |
| `name`            | `fullName`         | FE đọc `fullName`                                                                |
| `role` (string)   | `roles` (array)    | FE đọc `roles[0]`                                                                |
| `status`          | `status` (enum)    | Giá trị: `ACTIVE`, `INACTIVE`, `BANNED`, `DELETED` (không phải lowercase)        |
| `joinedDate`      | `createdDate`      | FE đọc `createdDate`                                                             |
| `lastLogin`       | **Không có**       | Hệ thống chưa track last login time. Field này không tồn tại                     |
| `school`          | **Không có**       | Không có trong User entity                                                       |
| Cấu trúc `result` | Spring Page object | FE cần đọc `result.content` để lấy array users, `result.totalElements` cho total |

> **[FE note]** `status` trả UPPER_CASE. `"active"` → `"ACTIVE"`, `"inactive"` → `"INACTIVE"`.  
> `lastLogin` không có trong response, FE hiển thị `createdDate` hoặc bỏ cột này.

---

## 5. Teacher Profile Review — Đếm hồ sơ chờ duyệt

**Trạng thái:** ✅ Đã có — đúng spec

### Endpoint

```
GET /teacher-profiles/pending/count
```

### Auth

- Header: `Authorization: Bearer <JWT>`
- Role được phép: `ADMIN` only

### Response — 200 OK

```json
{
  "code": 1000,
  "result": 12
}
```

### Ghi chú

- `result` là một số nguyên (không phải object)
- FE hiện dùng `response.result` — đúng

---

## 6. Recent Transactions — Giao dịch gần đây

**Trạng thái:** 🔨 Mới implement

### Endpoint

```
GET /admin/transactions
```

### Auth

- Header: `Authorization: Bearer <JWT>`
- Role được phép: `ADMIN` only

### Request

**Query params:**
| Param | Type | Bắt buộc | Mô tả |
|----------|--------|----------|-----------------------------------------------------|
| `page` | number | ❌ | Mặc định: 0 (0-indexed) |
| `size` | number | ❌ | Mặc định: 5 |
| `sortBy` | string | ❌ | Mặc định: `createdAt` |
| `order` | string | ❌ | `ASC` hoặc `DESC`, mặc định: `DESC` |

### Response — 200 OK

```json
{
  "code": 1000,
  "result": {
    "content": [
      {
        "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "userId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
        "userName": "Nguyễn Văn A",
        "planId": null,
        "planName": "Nạp tiền vào ví",
        "amount": 199000,
        "status": "completed",
        "paymentMethod": "payos",
        "createdAt": "2026-04-10T07:23:00Z"
      }
    ],
    "totalElements": 1245,
    "totalPages": 249,
    "size": 5,
    "number": 0,
    "first": true,
    "last": false
  }
}
```

### Thay đổi so với FE spec — ⚠️ Architectural mismatch

| Field FE spec         | Field BE thực tế       | Ghi chú                                                                                  |
| --------------------- | ---------------------- | ---------------------------------------------------------------------------------------- |
| `planId`              | `null`                 | Hệ thống không có "subscription plan". Sẽ null cho đến khi feature này được build        |
| `planName`            | `description`          | Mapped từ `Transaction.description` (ví dụ: "Nạp tiền vào ví")                           |
| `paymentMethod`       | `"payos"`              | Chỉ có 1 gateway: PayOS. Không có wallet/momo/bank_transfer trong model                  |
| `status`              | mapped string          | `SUCCESS`→`"completed"`, `PENDING/PROCESSING`→`"pending"`, `FAILED/CANCELLED`→`"failed"` |
| `result.transactions` | `result.content`       | Spring Page — FE đọc `result.content`                                                    |
| `result.total`        | `result.totalElements` | FE đọc `result.totalElements`                                                            |
| `result.page`         | `result.number`        | FE đọc `result.number` (0-indexed)                                                       |

### Ghi chú

- **Không có endpoint `GET /admin/transactions/:id`** hiện tại. Nút 👁️ "Chi tiết" chưa thể kết nối. BE cần thêm endpoint này nếu FE cần.
- Transaction entity là wallet top-up qua PayOS — không có gói subscription/plan concept trong hệ thống.

---

## 7. Revenue Chart — Doanh thu theo tháng

**Trạng thái:** 🔨 Mới implement

### Endpoint

```
GET /admin/dashboard/revenue-by-month
```

### Auth

- Header: `Authorization: Bearer <JWT>`
- Role được phép: `ADMIN` only

### Request

**Query params:**
| Param | Type | Bắt buộc | Mô tả |
|--------|--------|----------|------------------------------------|
| `year` | number | ❌ | Mặc định: năm hiện tại |

### Response — 200 OK

```json
{
  "code": 1000,
  "result": {
    "year": 2026,
    "monthly": [
      { "month": 1, "revenue": 32000000 },
      { "month": 2, "revenue": 45000000 },
      { "month": 3, "revenue": 38000000 },
      { "month": 4, "revenue": 52000000 },
      { "month": 5, "revenue": 0 },
      { "month": 6, "revenue": 0 },
      { "month": 7, "revenue": 0 },
      { "month": 8, "revenue": 0 },
      { "month": 9, "revenue": 0 },
      { "month": 10, "revenue": 0 },
      { "month": 11, "revenue": 0 },
      { "month": 12, "revenue": 0 }
    ]
  }
}
```

### Ghi chú

- **Luôn trả đủ 12 phần tử** (tháng chưa có data trả `revenue: 0`)
- Đơn vị: VNĐ tuyệt đối (tổng các transaction `SUCCESS` trong tháng)
- FE tự tính `height%` theo max trong mảng

---

## 8. Quick Stats — Thống kê nhanh

**Trạng thái:** 🔨 Mới implement (một số metric là ước lượng)

### Endpoint

```
GET /admin/dashboard/quick-stats
```

### Auth

- Header: `Authorization: Bearer <JWT>`
- Role được phép: `ADMIN` only

### Response — 200 OK

```json
{
  "code": 1000,
  "result": {
    "conversionRate": 35.0,
    "activeUsers": 1456,
    "documentsCreated": 8234,
    "satisfactionRate": -1.0
  }
}
```

### Thay đổi và giải thích

| Field              | Định nghĩa BE                                         | Ghi chú                                                                   |
| ------------------ | ----------------------------------------------------- | ------------------------------------------------------------------------- |
| `conversionRate`   | `(tổng transactions / tổng users) * 100` (%)          | Xấp xỉ. Sẽ chính xác hơn khi có plan subscription                         |
| `activeUsers`      | Số user có status = `ACTIVE`                          | Không track last login                                                    |
| `documentsCreated` | `COUNT(lesson_plans) + COUNT(mindmaps)` (non-deleted) | Bao gồm lesson plans và mindmaps của giáo viên                            |
| `satisfactionRate` | **Luôn trả `-1.0`**                                   | Hệ thống chưa có tính năng đánh giá/rating. FE hiển thị "N/A" khi nhận -1 |

> **[FE note]** Khi `satisfactionRate === -1`, FE nên hiển thị "Chưa có dữ liệu" thay vì thanh progress.

---

## 9. System Status — Trạng thái hệ thống

**Trạng thái:** 🔨 Mới implement

### Endpoint

```
GET /admin/system/status
```

### Auth

- Header: `Authorization: Bearer <JWT>`
- Role được phép: `ADMIN` only

### Response — 200 OK

```json
{
  "code": 1000,
  "result": {
    "services": [
      {
        "name": "Web Server",
        "status": "active",
        "description": "Đang hoạt động bình thường",
        "usagePercent": null
      },
      {
        "name": "Database",
        "status": "active",
        "description": "Kết nối bình thường",
        "usagePercent": null
      },
      {
        "name": "AI Service",
        "status": "active",
        "description": "Kết nối Gemini API bình thường",
        "usagePercent": null
      },
      {
        "name": "Storage",
        "status": "active",
        "description": "MinIO storage đang hoạt động",
        "usagePercent": null
      }
    ]
  }
}
```

### Validation

- `status` trả đúng 3 giá trị: `"active"`, `"warning"`, `"error"` — khớp CSS class FE

### Ghi chú — Giới hạn hiện tại

| Service    | Cách kiểm tra                      | Giới hạn                                                                       |
| ---------- | ---------------------------------- | ------------------------------------------------------------------------------ |
| Web Server | Nếu endpoint respond = active      | Luôn active nếu request đến được                                               |
| Database   | Thực hiện `SELECT COUNT(*)` lên DB | `error` nếu throw exception                                                    |
| AI Service | Hardcode "active"                  | Không có health-check Gemini API key live                                      |
| Storage    | Hardcode "active"                  | MinIO không expose API quota trong cấu hình hiện tại; `usagePercent` luôn null |

---

## Danh sách thay đổi so với đề xuất FE

| Feature             | Thay đổi                                                         | Lý do                                                |
| ------------------- | ---------------------------------------------------------------- | ---------------------------------------------------- |
| Admin Identity      | URL `/api/auth/me` → `/users/my-info`                            | Endpoint đã tồn tại với URL khác                     |
| Admin Identity      | `name` → `fullName`, `role` → `roles[]`                          | UserResponse schema sẵn có                           |
| Notification Count  | Key `unreadCount` (đã fix, trước là `count`)                     | Đổi để khớp FE spec                                  |
| Notification Count  | Không wrap ApiResponse                                           | ResponseEntity<Map> — FE đọc trực tiếp               |
| Stats Grid          | `activeSubscriptions` → `activeEnrollments`                      | Không có subscription plan trong hệ thống            |
| Recent Users        | URL `/api/admin/users/recent` → `/users/admin/recent`            | Đặt dưới `/users` controller cho nhất quán           |
| Recent Users        | Phân trang 0-indexed, response là Spring Page object             | `result.content` thay vì `result.users`              |
| Recent Users        | `lastLogin`, `school` không có                                   | Không có field này trong User entity                 |
| Recent Users        | `status` UPPER_CASE                                              | Enum Java                                            |
| Recent Transactions | `planId` luôn null; `paymentMethod` luôn `"payos"`               | Không có subscription plan, chỉ có 1 payment gateway |
| Recent Transactions | `result.content`, `result.totalElements`, `result.number`        | Spring Page object                                   |
| Revenue Chart       | Đơn vị VNĐ tuyệt đối (không phải %)                              | FE tự tính height% theo max                          |
| Quick Stats         | `satisfactionRate: -1.0` khi chưa có dữ liệu                     | Chưa có feature đánh giá/rating                      |
| System Status       | `usagePercent` luôn null (Storage)                               | MinIO không expose quota API trong config hiện tại   |
| Global              | Response format `{ code, result }` thay vì `{ success, result }` | Cấu trúc ApiResponse chuẩn của BE                    |

---

## Checklist trước khi FE integrate

- [x] Tất cả endpoints đã implement và compile thành công
- [ ] BE deploy lên môi trường staging
- [ ] Test với Postman/curl
- [ ] FE cập nhật `response.result` thay vì `response.data`
- [ ] FE cập nhật check `response.code === 1000` thay vì `response.success`
- [ ] FE xử lý `roles[]` (array) thay vì `role` (string)
- [ ] FE xử lý `status` UPPER_CASE (`ACTIVE`/`INACTIVE`/`BANNED`)
- [ ] FE xử lý Spring Page structure (`result.content`, `result.totalElements`, `result.number`)
- [ ] FE xử lý `satisfactionRate === -1` → hiển thị "N/A"
- [ ] Seed data test sẵn sàng

---

## Tóm tắt endpoints admin dashboard

| #   | Feature                | Method | URL                                 | Status        |
| --- | ---------------------- | ------ | ----------------------------------- | ------------- |
| 1   | Admin Identity         | GET    | `/users/my-info`                    | ✅ Đã có      |
| 2   | Notification Count     | GET    | `/v1/notifications/unread-count`    | ⚠️ Đã fix key |
| 3   | Dashboard Stats        | GET    | `/admin/dashboard/stats`            | 🔨 Mới        |
| 4   | Recent Users           | GET    | `/users/admin/recent`               | 🔨 Mới        |
| 5   | Pending Profiles Count | GET    | `/teacher-profiles/pending/count`   | ✅ Đã có      |
| 6   | Admin Transactions     | GET    | `/admin/transactions`               | 🔨 Mới        |
| 7   | Revenue By Month       | GET    | `/admin/dashboard/revenue-by-month` | 🔨 Mới        |
| 8   | Quick Stats            | GET    | `/admin/dashboard/quick-stats`      | 🔨 Mới        |
| 9   | System Status          | GET    | `/admin/system/status`              | 🔨 Mới        |

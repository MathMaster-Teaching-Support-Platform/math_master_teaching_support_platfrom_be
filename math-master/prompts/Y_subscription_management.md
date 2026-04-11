# API Contract — SubscriptionManagement (Quản Lý Gói Đăng Ký)

**Ngày tạo:** 2026-04-11  
**Người tạo:** BE Team  
**Phản hồi cho:** X.md — FE spec request (SubscriptionManagement)  
**Trạng thái:** Chờ FE confirm

> **Base URL:** `http://<host>/admin/subscription-plans`  
> ⚠️ Không có prefix `/api/` — tất cả admin endpoint đều bắt đầu từ `/admin/...`  
> **Wrapper response:** Tất cả response đều được wrap bởi `ApiResponse<T>`:
> ```json
> { "code": 1000, "message": null, "result": <data> }
> ```
> - `code: 1000` = thành công
> - Không có field `success` hay `data` — FE phải đọc từ `result`

---

## 1. Danh sách gói đăng ký (Subscription Plans List)

**Trạng thái:** 🔨 Mới implement

### Endpoint

```
GET /admin/subscription-plans
```

### Auth
- Header: `Authorization: Bearer <JWT>`
- Role: `ADMIN`

### Request
Không có params — trả toàn bộ danh sách (không bị xóa mềm).

### Response — 200 OK

```json
{
  "code": 1000,
  "result": [
    {
      "id": "018f1a2b-0000-7000-8000-000000000001",
      "name": "Miễn phí",
      "slug": "mien-phi",
      "price": 0,
      "currency": "VND",
      "billingCycle": "FOREVER",
      "description": "Phù hợp để trải nghiệm",
      "featured": false,
      "isPublic": true,
      "status": "ACTIVE",
      "features": [
        "Tạo tối đa 10 bài giảng/tháng",
        "Lưu trữ 100MB",
        "Quản lý 1 lớp học",
        "AI trợ lý cơ bản",
        "Hỗ trợ email"
      ],
      "stats": {
        "activeUsers": 1250,
        "revenueThisMonth": 0,
        "growthPercent": 0.0
      },
      "createdAt": "2025-01-01T00:00:00Z",
      "updatedAt": "2026-04-01T00:00:00Z"
    },
    {
      "id": "018f1a2b-0000-7000-8000-000000000002",
      "name": "Giáo viên",
      "slug": "giao-vien",
      "price": 199000,
      "currency": "VND",
      "billingCycle": "MONTH",
      "description": "Dành cho giáo viên cá nhân",
      "featured": true,
      "isPublic": true,
      "status": "ACTIVE",
      "features": [
        "Tạo không giới hạn bài giảng",
        "Lưu trữ 10GB",
        "Quản lý không giới hạn lớp học",
        "AI trợ lý nâng cao",
        "Hỗ trợ ưu tiên"
      ],
      "stats": {
        "activeUsers": 450,
        "revenueThisMonth": 89550000,
        "growthPercent": 12.3
      },
      "createdAt": "2025-01-01T00:00:00Z",
      "updatedAt": "2026-04-01T00:00:00Z"
    },
    {
      "id": "018f1a2b-0000-7000-8000-000000000003",
      "name": "Trường học",
      "slug": "truong-hoc",
      "price": null,
      "currency": "VND",
      "billingCycle": "CUSTOM",
      "description": "Giải pháp cho tổ chức",
      "featured": false,
      "isPublic": true,
      "status": "ACTIVE",
      "features": [
        "Tất cả tính năng gói Giáo viên",
        "Không giới hạn tài khoản",
        "Lưu trữ không giới hạn"
      ],
      "stats": {
        "activeUsers": 30,
        "revenueThisMonth": 150000000,
        "growthPercent": 8.0
      },
      "createdAt": "2025-01-01T00:00:00Z",
      "updatedAt": "2026-04-01T00:00:00Z"
    }
  ]
}
```

### Response — Lỗi phổ biến

```json
// 401
{ "code": 1006, "message": "Unauthenticated" }

// 403
{ "code": 1007, "message": "You do not have permission" }
```

### Validation BE áp dụng

- Không có filter params — trả tất cả plan chưa bị soft-delete, sắp xếp theo `createdAt ASC`
- `stats` được tính real-time từ bảng `user_subscriptions`

### Ghi chú kỹ thuật

- `price: null` = gói doanh nghiệp "Liên hệ" (không có field `contactSales` riêng — FE check `price === null`)
- `billingCycle` là **enum string**: `FOREVER` | `MONTH` | `THREE_MONTHS` | `SIX_MONTHS` | `YEAR` | `CUSTOM`
- `featured: true` → render badge "⭐ Phổ biến nhất"
- `stats.growthPercent` = so sánh revenue tháng hiện tại vs tháng trước (có thể âm)
- Không có phân trang — cố định danh sách tất cả plans

---

## 2. Tạo gói đăng ký mới (Create Plan)

**Trạng thái:** 🔨 Mới implement

### Endpoint

```
POST /admin/subscription-plans
```

### Auth
- Header: `Authorization: Bearer <JWT>`
- Role: `ADMIN`

### Request Body

```json
{
  "name": "Giáo viên Pro",
  "description": "Dành cho giáo viên nâng cao",
  "price": 299000,
  "billingCycle": "MONTH",
  "features": [
    "Tạo không giới hạn bài giảng",
    "Lưu trữ 50GB"
  ],
  "featured": false,
  "isPublic": true
}
```

**Request fields:**

| Field          | Type                      | Bắt buộc | Mô tả                                     |
|----------------|---------------------------|----------|-------------------------------------------|
| `name`         | string                    | ✅        | Tên gói, không rỗng                       |
| `description`  | string                    | ❌        | Mô tả                                     |
| `price`        | number (>= 0) hoặc `null` | ❌        | `null` = enterprise/liên hệ; `0` = miễn phí |
| `billingCycle` | enum string               | ✅        | `FOREVER` \| `MONTH` \| `THREE_MONTHS` \| `SIX_MONTHS` \| `YEAR` \| `CUSTOM` |
| `features`     | string[]                  | ✅        | Ít nhất 1 phần tử, mỗi phần tử không rỗng |
| `featured`     | boolean                   | ❌        | Default `false`                           |
| `isPublic`     | boolean                   | ❌        | Default `true`                            |

### Response — 201 Created

```json
{
  "code": 1000,
  "result": {
    "id": "018f1a2b-xxxx-7000-8000-xxxxxxxxxxxx",
    "name": "Giáo viên Pro",
    "slug": "giao-vien-pro",
    "price": 299000,
    "currency": "VND",
    "billingCycle": "MONTH",
    "description": "Dành cho giáo viên nâng cao",
    "featured": false,
    "isPublic": true,
    "status": "ACTIVE",
    "features": ["Tạo không giới hạn bài giảng", "Lưu trữ 50GB"],
    "stats": { "activeUsers": 0, "revenueThisMonth": 0, "growthPercent": 0.0 },
    "createdAt": "2026-04-11T10:00:00Z",
    "updatedAt": "2026-04-11T10:00:00Z"
  }
}
```

### Response — Lỗi phổ biến

```json
// 400 — tên trùng slug
{ "code": 1155, "message": "A plan with this slug already exists" }

// 400 — validation
{ "code": 1001, "message": "features: At least one feature is required" }
```

### Validation BE áp dụng

- `name`: bắt buộc, không rỗng
- `billingCycle`: bắt buộc, phải là một trong enum values
- `price`: nếu cung cấp thì >= 0; `null` hợp lệ
- `features`: bắt buộc, ít nhất 1 phần tử không rỗng
- `slug` tự động sinh từ `name` bằng cách: chuẩn hóa Unicode → bỏ dấu → lowercase → spaces thành `-` → xóa ký tự đặc biệt
- Nếu slug trùng → 400 với code `1155`

### Ghi chú

- [FE đã hỏi] **BE tự sinh `slug`** — FE không cần gửi
- Sau khi tạo thành công → FE đóng modal và refetch `GET /admin/subscription-plans`

---

## 3. Cập nhật gói đăng ký (Update Plan)

**Trạng thái:** 🔨 Mới implement

### Endpoint

```
PUT /admin/subscription-plans/:planId
```

### Auth
- Header: `Authorization: Bearer <JWT>`
- Role: `ADMIN`

### Path Params

| Param    | Type         | Bắt buộc | Mô tả            |
|----------|--------------|----------|------------------|
| `planId` | UUID (v7)    | ✅        | ID của gói đăng ký |

### Request Body (partial update — chỉ gửi field cần thay đổi)

```json
{
  "name": "Giáo viên",
  "description": "Mô tả mới",
  "price": 249000,
  "billingCycle": "MONTH",
  "features": ["Feature A", "Feature B"],
  "featured": true,
  "isPublic": true,
  "status": "ACTIVE"
}
```

| Field          | Type                                  | Bắt buộc | Mô tả                             |
|----------------|---------------------------------------|----------|-----------------------------------|
| `name`         | string (not blank)                    | ✅        | Nếu gửi → slug được tái sinh      |
| `description`  | string                                | ❌        |                                   |
| `price`        | number (>= 0) hoặc `null`             | ❌        |                                   |
| `billingCycle` | enum string                           | ❌        |                                   |
| `features`     | string[]                              | ❌        | Nếu gửi phải có ít nhất 1 phần tử |
| `featured`     | boolean                               | ❌        |                                   |
| `isPublic`     | boolean                               | ❌        |                                   |
| `status`       | `"ACTIVE"` \| `"INACTIVE"`            | ❌        | Để deactivate plan                |

### Response — 200 OK

Trả về plan object đã cập nhật, shape giống `GET /admin/subscription-plans` item.

### Response — Lỗi phổ biến

```json
// 404
{ "code": 1154, "message": "Subscription plan not found" }

// 400 — slug mới xung đột với plan khác
{ "code": 1155, "message": "A plan with this slug already exists" }
```

### Ghi chú kỹ thuật

- [FE đã hỏi] **Thay đổi giá khi có user đang subscribe**: BE **cho phép** — giá mới chỉ áp dụng cho subscription mới. User đang có subscription giữ nguyên `amount` cũ trong record của họ.
- Để vô hiệu hóa plan mà không xóa: gửi `{ "status": "INACTIVE" }`

---

## 4. Xóa gói đăng ký (Delete Plan)

**Trạng thái:** 🔨 Mới implement

### Endpoint

```
DELETE /admin/subscription-plans/:planId
```

### Auth
- Header: `Authorization: Bearer <JWT>`
- Role: `ADMIN`

### Path Params

| Param    | Type      | Bắt buộc | Mô tả              |
|----------|-----------|----------|--------------------|
| `planId` | UUID (v7) | ✅        | ID của gói đăng ký |

### Response — 200 OK

```json
{
  "code": 1000,
  "message": "Plan deleted successfully"
}
```

### Response — Lỗi phổ biến

```json
// 404
{ "code": 1154, "message": "Subscription plan not found" }

// 400 — vẫn còn active subscribers
{ "code": 1156, "message": "Cannot delete a plan that has active subscribers. Deactivate the plan instead." }
```

### Ghi chú kỹ thuật

- [FE đã hỏi] **Không cho phép xóa** khi plan còn active subscribers → trả 400 với `code: 1156`
- Giải pháp: FE cần PUT `{ "status": "INACTIVE" }` trước → sau khi toàn bộ subscription hết hạn → mới DELETE được
- Xóa là **soft-delete** (`deletedAt` được set) — dữ liệu vẫn tồn tại trong DB
- FE nên hiển thị confirm dialog trước khi gọi DELETE

---

## 5. Thống kê doanh thu tổng (Revenue Stats)

**Trạng thái:** 🔨 Mới implement

### Endpoint

```
GET /admin/subscription-plans/stats
```

### Auth
- Header: `Authorization: Bearer <JWT>`
- Role: `ADMIN`

### Query Params

| Param   | Type   | Bắt buộc | Mô tả                              |
|---------|--------|----------|------------------------------------|
| `month` | string | ❌        | Format `YYYY-MM`, mặc định tháng hiện tại |

> ⚠️ Không có param `compareWith` — BE luôn so sánh với tháng liền trước

### Response — 200 OK

```json
{
  "code": 1000,
  "result": {
    "totalRevenue": 239550000,
    "totalRevenueTrend": 12.5,
    "totalPaidUsers": 480,
    "totalPaidUsersTrend": 8.3,
    "avgRevenuePerUser": 498854,
    "avgRevenuePerUserTrend": 4.2,
    "conversionRate": 23.5,
    "conversionRateTrend": 1.2,
    "period": "2026-04"
  }
}
```

### Validation BE áp dụng

- `month`: format `YYYY-MM` (YearMonth). Sai format → 500 (parse exception)
- [FE nên validate] `month` trước khi gửi để tránh lỗi

### Ghi chú kỹ thuật

- [FE đã hỏi] **Period mặc định** = tháng hiện tại (UTC), không phải rolling 30 ngày
- `totalRevenue` = tổng amount của tất cả UserSubscription có status=ACTIVE được tạo trong tháng
- `totalPaidUsers` = count UserSubscription có `amount > 0` và `status=ACTIVE` trong tháng
- `conversionRate` = (tổng active paid users của mọi thời gian) / (tổng users) × 100
- `trend` = % thay đổi so với tháng trước; âm nếu giảm
- Khác với `AdminQuickStatsResponse.conversionRate` (dùng cho widget dashboard khác): endpoint này tính chính xác hơn từ `user_subscriptions`

---

## 6. Danh sách đăng ký gần đây (Recent Subscriptions Table)

**Trạng thái:** 🔨 Mới implement

### Endpoint

```
GET /admin/subscription-plans/subscriptions
```

> ⚠️ **Thay đổi so với FE đề xuất**: FE đề xuất `/api/admin/subscriptions`, BE dùng `/admin/subscription-plans/subscriptions` (cùng controller, dễ quản lý)

### Auth
- Header: `Authorization: Bearer <JWT>`
- Role: `ADMIN`

### Query Params

| Param    | Type   | Bắt buộc | Mô tả                                               |
|----------|--------|----------|-----------------------------------------------------|
| `page`   | number | ❌        | 0-indexed, mặc định `0` ⚠️ (khác với FE đề xuất là 1-indexed) |
| `size`   | number | ❌        | Mặc định `10`, tối đa không giới hạn phía BE        |
| `status` | string | ❌        | `"ACTIVE"` \| `"EXPIRED"` \| `"CANCELLED"` \| `"all"` (mặc định: không lọc) |
| `planId` | UUID   | ❌        | Lọc theo plan                                       |
| `sortBy` | string | ❌        | Mặc định `"createdAt"`                              |
| `order`  | string | ❌        | `"ASC"` \| `"DESC"`, mặc định `"DESC"`              |

### Response — 200 OK

```json
{
  "code": 1000,
  "result": {
    "content": [
      {
        "id": "018f1a2b-xxxx-7000-8000-xxxxxxxxxxxx",
        "user": {
          "id": "018f0001-xxxx-7000-8000-xxxxxxxxxxxx",
          "name": "Nguyễn Văn A",
          "email": "nguyenvana@example.com",
          "avatarInitial": "N"
        },
        "plan": {
          "id": "018f1a2b-0000-7000-8000-000000000002",
          "name": "Giáo viên",
          "slug": "giao-vien"
        },
        "startDate": "2026-03-05T00:00:00Z",
        "endDate": "2026-04-05T00:00:00Z",
        "amount": 199000,
        "currency": "VND",
        "status": "ACTIVE",
        "paymentMethod": "payos",
        "createdAt": "2026-03-05T10:00:00Z"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10
    },
    "totalElements": 150,
    "totalPages": 15,
    "first": true,
    "last": false
  }
}
```

> ⚠️ **Pagination shape khác FE đề xuất**: Spring Data `Page<T>` trả về `content` (không phải `items`), `totalElements` (không phải `total`), `pageable.pageNumber` (0-indexed). FE cần map lại.

### Response — Lỗi phổ biến

```json
// 400 — status không hợp lệ
{ "code": 9999, "message": "No enum constant com.fptu.math_master.enums.UserSubscriptionStatus.INVALID" }
```

### Validation BE áp dụng

- `status` case-insensitive (`"active"`, `"ACTIVE"`, `"Active"` đều hợp lệ)
- `status = "all"` hoặc không truyền → không lọc theo status
- `planId` phải là UUID hợp lệ nếu truyền

### Ghi chú kỹ thuật

- [FE đã hỏi] **Action "🔄 Gia hạn"**: Chưa có endpoint. Cần implement `POST /admin/subscription-plans/subscriptions/:id/renew` — sẽ tạo UserSubscription mới cho user với cùng plan và set `startDate = endDate cũ`. **Chưa có trong sprint này.**
- [FE đã hỏi] **Action "👁️ Chi tiết"**: Không cần API riêng — dữ liệu đã đủ trong từng row. FE hiển thị modal sử dụng data row đã fetch.
- User info được batch-load (không N+1) từ `users` table

---

## Danh sách thay đổi so với đề xuất của FE

| Feature                        | Thay đổi                                                                 | Lý do                                                                      |
|--------------------------------|--------------------------------------------------------------------------|----------------------------------------------------------------------------|
| Tất cả endpoints               | Không có prefix `/api/` — dùng `/admin/...` trực tiếp                    | Project không cấu hình `context-path = /api`                               |
| Response wrapper               | `{ "code": 1000, "result": ... }` thay vì `{ "success": true, "data": ... }` | Codebase dùng `ApiResponse<T>` chuẩn                                   |
| Recent Subscriptions endpoint  | `/admin/subscription-plans/subscriptions` thay vì `/api/admin/subscriptions` | Cùng controller, dễ quản lý authorization                               |
| Pagination (subscriptions)     | 0-indexed (`page=0` = trang 1), response field `content`/`totalElements` | Spring Data Page convention                                                |
| `billingCycle` values          | Enum: `FOREVER`, `MONTH`, `THREE_MONTHS`, `SIX_MONTHS`, `YEAR`, `CUSTOM` | FE dùng `"3months"`, `"6months"` — BE dùng enum names                   |
| `price: null` cho enterprise   | Dùng `price: null`, không có field `contactSales: boolean`               | Đủ để FE phân biệt: `price===null` → "Liên hệ"                           |
| `stats` trong plan response    | Tính từ `user_subscriptions`, không phải `transactions`                  | Transactions không có `planId` trong model hiện tại                       |
| `compareWith` param (stats)    | Không có — BE tự so sánh với tháng trước liền kề                         | Đơn giản hóa, đủ dùng cho dashboard                                       |
| Slug                           | Auto-sinh từ `name`, FE không cần gửi                                    | Tránh nhập thủ công, tránh lỗi                                             |

---

## Checklist trước khi FE integrate

- [x] Tất cả endpoints đã implement và compile thành công
- [ ] Chạy migration DB để tạo bảng `subscription_plans` và `user_subscriptions`
- [ ] Seed 3 plan mặc định (`mien-phi`, `giao-vien`, `truong-hoc`) vào DB staging
- [ ] Swagger/OpenAPI đã auto-generate tại `/swagger-ui.html`
- [ ] Test với Postman collection (chưa có — FE tự test theo spec này)
- [ ] Endpoint `POST /admin/subscription-plans/subscriptions/:id/renew` chưa có — **ghi nhận backlog**

---

## UNKNOWN đã giải đáp

| Câu hỏi FE                                            | Trả lời BE                                                                                   |
|-------------------------------------------------------|----------------------------------------------------------------------------------------------|
| `price: null` vs `contactSales: boolean`?             | Dùng `price: null` — đơn giản và đủ                                                         |
| BE tự sinh `slug` hay FE gửi?                         | BE tự sinh từ `name`                                                                         |
| Thay đổi giá khi có user active?                      | Cho phép — giá mới áp dụng cho subscription mới; user cũ giữ nguyên amount đã ghi trong record |
| Xóa plan có user active?                              | Không cho phép (400) — phải deactivate trước bằng `PUT status=INACTIVE`                     |
| Endpoint gia hạn `POST .../renew`?                    | Chưa có trong sprint này — ghi vào backlog                                                   |
| API chi tiết subscription hay dùng row data?          | Dùng row data — không cần API riêng                                                          |
| Period stats mặc định là tháng hiện tại hay rolling?  | Tháng hiện tại (UTC calendar month)                                                          |

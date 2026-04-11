# API Contract — Admin / Quản Lý Người Dùng

**Ngày cập nhật:** 2026-04-11 (v2)  
**Người tạo:** BE Team  
**Phản hồi cho:** X.md v2 (FE spec — UserManagement, bao gồm bug fixes + section 10)  
**Trạng thái:** Đã implement, chờ FE confirm

> **Base URL:** `http://<host>:8080`  
> Không có `/api` prefix — server không cấu hình `context-path`.  
> Mọi endpoint admin user nằm dưới `/admin/users`.

---

## Changelog v2 (so với Y.md v1)

| #   | Thay đổi                                             | Lý do                                                            |
| --- | ---------------------------------------------------- | ---------------------------------------------------------------- |
| 1   | `stats.students` đã đếm theo **STUDENT_ONLY**        | Bug fix — giáo viên có role STUDENT không được tính vào học sinh |
| 2   | Thêm `role=STUDENT_ONLY` vào filter                  | Bug fix — FE loại bỏ client-side filter                          |
| 3   | Thêm `stats.admins` vào response                     | Tab Admin FE cần con số thực                                     |
| 4   | Thêm `sortBy` + `sortOrder` params                   | Section 10.4                                                     |
| 5   | Thêm `createdFrom` + `createdTo` params              | Section 10.5                                                     |
| 6   | Filter `status=BANNED` và `status=DELETED` đã hỗ trợ | Section 10.3                                                     |

---

## 1. Danh sách người dùng + Thống kê tổng quan

**Trạng thái:** ✅ Updated — bug fixes + params mới

### Endpoint

```
GET /admin/users
```

### Auth

- Header: `Authorization: Bearer <JWT>`
- Role được phép: `ADMIN`

### Request — Query params

| Param       | Type    | Default | Mô tả                                                                           |
| ----------- | ------- | ------- | ------------------------------------------------------------------------------- |
| page        | integer | `0`     | Zero-based page index                                                           |
| pageSize    | integer | `20`    | Số item mỗi trang                                                               |
| role        | string  | `all`   | `TEACHER` \| `STUDENT_ONLY` \| `ADMIN` \| `all`                                 |
| search      | string  | —       | Tìm trên `fullName`, `email`, `userName` (case-insensitive)                     |
| status      | string  | `all`   | `ACTIVE` \| `INACTIVE` \| `BANNED` \| `DELETED` \| `all`                        |
| sortBy      | string  | —       | `fullName` \| `email` \| `createdDate` \| `lastLogin` \| `status` \| `userName` |
| sortOrder   | string  | `desc`  | `asc` \| `desc`                                                                 |
| createdFrom | string  | —       | ISO 8601 instant, VD: `2025-01-01T00:00:00Z`                                    |
| createdTo   | string  | —       | ISO 8601 instant, VD: `2025-12-31T23:59:59Z`                                    |

> ⚠️ `STUDENT` (không có `_ONLY`) không còn được sử dụng — FE nên dùng `STUDENT_ONLY` để lấy học sinh thuần túy.

#### Các giá trị `role` và ý nghĩa

| Giá trị `role` | Hành vi BE                                                        |
| -------------- | ----------------------------------------------------------------- |
| `TEACHER`      | User có role `TEACHER` (kể cả user vừa là TEACHER vừa là STUDENT) |
| `STUDENT_ONLY` | User có role `STUDENT` **và không có** role `TEACHER`             |
| `ADMIN`        | User có role `ADMIN`                                              |
| `all`          | Không filter role (mặc định)                                      |

### Response — 200 OK

```json
{
  "code": 1000,
  "result": {
    "users": [
      {
        "id": "018eac12-0000-7000-8000-000000000001",
        "userName": "teacher01",
        "fullName": "Nguyễn Văn A",
        "email": "teacher01@mathmaster.vn",
        "avatar": "https://storage.mathmaster.vn/avatars/teacher01.jpg",
        "status": "ACTIVE",
        "lastLogin": "2026-04-10T08:30:00Z",
        "roles": ["TEACHER", "STUDENT"],
        "createdDate": "2025-09-01T00:00:00Z",
        "phoneNumber": null,
        "gender": null,
        "dob": null,
        "code": null,
        "banReason": null,
        "banDate": null,
        "updatedDate": "2026-04-10T08:30:00Z"
      }
    ],
    "stats": {
      "total": 40,
      "admins": 3,
      "teachers": 16,
      "students": 12,
      "active": 35
    },
    "pagination": {
      "page": 0,
      "pageSize": 20,
      "totalItems": 40,
      "totalPages": 2
    }
  }
}
```

> **Về `stats`:**
>
> - Luôn là **global stats** — không bị ảnh hưởng bởi filter hiện tại
> - `students` = số user có role `STUDENT` **mà không có** role `TEACHER` (STUDENT_ONLY)
> - `teachers` = số user có role `TEACHER` (kể cả user vừa TEACHER vừa STUDENT)
> - `admins` = số user có role `ADMIN`
> - Users với status `DELETED` bị loại khỏi `total` và tất cả stats

### Ví dụ filter học sinh thuần túy

```
GET /admin/users?role=STUDENT_ONLY
→ trả về 12 user có [STUDENT] không có [TEACHER]   ✅
→ KHÔNG trả về 16 giáo viên có [TEACHER, STUDENT]  ✅

GET /admin/users?role=TEACHER
→ trả về 16 user có role TEACHER                   ✅
```

### Ví dụ sort và filter ngày

```
GET /admin/users?sortBy=lastLogin&sortOrder=desc
GET /admin/users?createdFrom=2025-01-01T00:00:00Z&createdTo=2025-12-31T23:59:59Z
GET /admin/users?sortBy=fullName&sortOrder=asc&role=STUDENT_ONLY
```

### Lỗi phổ biến

```json
{ "code": 1006, "message": "Unauthenticated" }
{ "code": 1007, "message": "You do not have permission" }
```

---

## 2. Tạo người dùng mới

**Trạng thái:** ⚠️ Endpoint hiện có — `POST /users` — field names khác FE đề xuất

### Endpoint

```
POST /users
```

### Auth

- Header: `Authorization: Bearer <JWT>`
- Role được phép: `ADMIN`

### Request Body

```json
{
  "userName": "student_new",
  "fullName": "Trần Thị B",
  "email": "tranthib@mathmaster.vn",
  "password": "StrongP@ss123",
  "roles": ["STUDENT"],
  "status": "ACTIVE"
}
```

### Mapping FE → BE

| Field FE                | Field BE        | Ghi chú              |
| ----------------------- | --------------- | -------------------- |
| `name`                  | `fullName`      | ⚠️ Đổi tên           |
| `role` (string)         | `roles` (array) | ⚠️ `["TEACHER"]`     |
| `sendWelcomeEmail`      | —               | ❌ Không hỗ trợ      |
| `requirePasswordChange` | —               | ❌ Không hỗ trợ      |
| —                       | `userName`      | ⚠️ **Bắt buộc thêm** |

### Validation BE

| Field      | Ràng buộc                                                             |
| ---------- | --------------------------------------------------------------------- |
| `userName` | required, 3–50 ký tự, unique                                          |
| `password` | required, 8–128 ký tự, phải có chữ hoa + thường + số + ký tự đặc biệt |
| `fullName` | required, 2–50 ký tự                                                  |
| `email`    | required, format hợp lệ, max 50 ký tự, unique                         |
| `roles`    | tối đa 10 roles; mỗi role phải tồn tại                                |

### Lỗi phổ biến

```json
{ "code": 1013, "message": "Email already exists" }
{ "code": 1002, "message": "User existed" }
{ "code": 1009, "message": "Role not existed" }
```

---

## 3. Xem chi tiết người dùng

**Trạng thái:** ✅ Có sẵn

### Endpoint

```
GET /admin/users/{userId}
```

### Response — 200 OK

```json
{
  "code": 1000,
  "result": {
    "id": "018eac12-0000-7000-8000-000000000001",
    "userName": "teacher01",
    "fullName": "Nguyễn Văn A",
    "email": "teacher01@mathmaster.vn",
    "phoneNumber": "+84912345678",
    "gender": "MALE",
    "avatar": "https://...",
    "dob": "1990-05-15",
    "code": "GV001",
    "status": "ACTIVE",
    "lastLogin": "2026-04-10T08:30:00Z",
    "banReason": null,
    "banDate": null,
    "roles": ["TEACHER", "STUDENT"],
    "createdDate": "2025-09-01T00:00:00Z",
    "updatedDate": "2026-04-10T08:30:00Z"
  }
}
```

---

## 4. Cập nhật trạng thái người dùng

**Trạng thái:** ✅ Có sẵn

### Endpoint (khuyến nghị)

```
PATCH /admin/users/{userId}/status?status=ACTIVE
PATCH /admin/users/{userId}/status?status=INACTIVE
```

### Hoặc endpoints cũ

```
PUT /users/{userId}/enable   → set ACTIVE
PUT /users/{userId}/disable  → set INACTIVE
```

### Lỗi phổ biến

```json
{ "code": 1018, "message": "User is already disabled" }
{ "code": 1019, "message": "User is already enabled" }
{ "code": 1001, "message": "status must be ACTIVE or INACTIVE" }
```

---

## 5. Đặt lại mật khẩu

**Trạng thái:** ✅ Đã implement

### Endpoint

```
POST /admin/users/{userId}/reset-password
```

### Response — 200 OK

```json
{
  "code": 1000,
  "message": "Password reset successfully. Temporary password sent to user's email."
}
```

> BE tự sinh mật khẩu tạm 12 ký tự → gửi email async cho user. Mật khẩu tạm **không trả về API**.

---

## 6. Gửi email cho người dùng

**Trạng thái:** ✅ Đã implement

### Endpoint

```
POST /admin/users/{userId}/send-email
```

### Request Body

```json
{
  "subject": "Thông báo từ MathMaster",
  "body": "Xin chào,\n\nĐây là thông báo quan trọng..."
}
```

| Field   | Ràng buộc                             |
| ------- | ------------------------------------- |
| subject | required, max 255 ký tự               |
| body    | required, max 10000 ký tự, plain text |

### Response — 200 OK

```json
{ "code": 1000, "message": "Email sent successfully." }
```

---

## 7. Xóa tài khoản

**Trạng thái:** ✅ Có sẵn

### Endpoint

```
DELETE /users/{userId}
```

> **Soft delete** — set `status = DELETED`. User bị ẩn khỏi `total` và tất cả stats.

---

## 8. Xuất Excel danh sách người dùng

**Trạng thái:** ✅ Đã implement

### Endpoint

```
GET /admin/users/export
```

### Query params

| Param  | Mô tả                                           |
| ------ | ----------------------------------------------- |
| role   | `TEACHER` \| `STUDENT_ONLY` \| `ADMIN` \| `all` |
| search | fullName/email/userName                         |
| status | `ACTIVE` \| `INACTIVE` \| `BANNED` \| `all`     |

**Cột Excel:** `ID | Username | Full Name | Email | Role(s) | Status | Join Date | Last Login`

### Cách FE trigger download

```typescript
const res = await fetch("/admin/users/export?role=all&status=all", {
  headers: { Authorization: `Bearer ${token}` },
});
const blob = await res.blob();
const url = URL.createObjectURL(blob);
const a = document.createElement("a");
a.href = url;
a.download = "users.xlsx";
a.click();
URL.revokeObjectOffset(url);
```

---

## 9. Chỉnh sửa thông tin người dùng

**Trạng thái:** ✅ Có sẵn

### Endpoint

```
PUT /users/{userId}
```

### Request Body

```json
{
  "fullName": "Nguyễn Văn B (updated)",
  "email": "newemail@mathmaster.vn",
  "status": "ACTIVE",
  "roles": ["TEACHER"],
  "avatar": "https://...",
  "phoneNumber": null,
  "gender": "MALE",
  "dob": "1990-05-15",
  "code": "GV002"
}
```

> `email` là `@NotBlank` — luôn phải gửi dù không đổi.

---

## Tóm tắt divergence với FE spec

| Điểm khác                                   | BE thực tế                                           | Lý do                                |
| ------------------------------------------- | ---------------------------------------------------- | ------------------------------------ |
| Base URL                                    | Không có `/api` prefix                               | Server không cấu hình `context-path` |
| `page`                                      | Zero-based (`0`, `1`, `2`...)                        | Spring Data convention               |
| `name` → `fullName`                         | `fullName`                                           | Khớp entity/DB                       |
| `role` (string) → `roles` (array)           | `roles: ["TEACHER"]`                                 | Multi-role support                   |
| `userName`                                  | **Bắt buộc** khi tạo user                            | Field login, unique trong DB         |
| `sendWelcomeEmail`, `requirePasswordChange` | Không hỗ trợ                                         | Không trong roadmap                  |
| Status                                      | 4 giá trị: `ACTIVE`, `INACTIVE`, `BANNED`, `DELETED` | Không chỉ 2                          |
| `stats.students`                            | STUDENT_ONLY (không đếm giáo viên)                   | Bug fix v2                           |

---

## Checklist FE integrate (v2)

- [x] DB migration `last_login` đã chạy
- [ ] FE đổi `role=STUDENT` → `role=STUDENT_ONLY` cho tab Học sinh
- [ ] FE lấy `stats.students` để hiển thị số lượng tab (đã đúng sau fix)
- [ ] FE lấy `stats.admins` để hiển thị tab Admin
- [ ] FE implement sort: gửi `sortBy=fullName&sortOrder=asc`
- [ ] FE implement date range: gửi `createdFrom=...&createdTo=...` (ISO 8601 UTC)
- [ ] FE update form tạo user: thêm `userName`, đổi `name`→`fullName`, `role`→`roles` array
- [ ] FE bỏ `sendWelcomeEmail`, `requirePasswordChange` khỏi create form
- [ ] Swagger: `http://<host>:8080/swagger-ui/index.html`

**Ngày tạo:** 2026-04-11  
**Người tạo:** BE Team  
**Phản hồi cho:** X.md (FE spec request — UserManagement)  
**Trạng thái:** Chờ FE confirm

> **Base URL:** `http://<host>:8080`  
> Không có `/api` prefix — server không cấu hình `context-path`.  
> Mọi endpoint admin user nằm dưới `/admin/users`.

---

## 1. Danh sách người dùng + Thống kê tổng quan

**Trạng thái:** 🔨 Mới implement — endpoint mới tại `AdminUserController`

### Endpoint

```
GET /admin/users
```

### Auth

- Header: `Authorization: Bearer <JWT>`
- Role được phép: `ADMIN`

### Request

**Query params:**

| Param    | Type    | Bắt buộc | Mặc định | Mô tả                                                            |
| -------- | ------- | -------- | -------- | ---------------------------------------------------------------- |
| page     | integer | ❌       | `0`      | Zero-based page index                                            |
| pageSize | integer | ❌       | `20`     | Số item mỗi trang                                                |
| role     | string  | ❌       | `all`    | `TEACHER` \| `STUDENT` \| `ADMIN` \| `all` (case-insensitive)    |
| search   | string  | ❌       | —        | Tìm kiếm trên `fullName`, `email`, `userName` (case-insensitive) |
| status   | string  | ❌       | `all`    | `ACTIVE` \| `INACTIVE` \| `BANNED` \| `all`                      |

> ⚠️ **Lưu ý khác với FE đề xuất:**
>
> - `page` bắt đầu từ `0` (zero-based), không phải `1`
> - `pageSize` thay vì `limit` (nhất quán với các endpoint khác)
> - `role` dùng chữ hoa (`TEACHER`, `STUDENT`, `ADMIN`) — FE đề xuất chữ thường

### Response — 200 OK

```json
{
  "code": 1000,
  "result": {
    "users": [
      {
        "id": "018eac12-0000-7000-8000-000000000001",
        "userName": "teacher01",
        "fullName": "Nguyễn Văn A",
        "email": "teacher01@mathmaster.vn",
        "avatar": "https://storage.mathmaster.vn/avatars/teacher01.jpg",
        "status": "ACTIVE",
        "lastLogin": "2026-04-10T08:30:00Z",
        "roles": ["TEACHER"],
        "createdDate": "2025-09-01T00:00:00Z",
        "phoneNumber": null,
        "gender": null,
        "dob": null,
        "code": null,
        "banReason": null,
        "banDate": null,
        "updatedDate": "2026-04-10T08:30:00Z"
      }
    ],
    "stats": {
      "total": 1240,
      "teachers": 320,
      "students": 900,
      "active": 1100
    },
    "pagination": {
      "page": 0,
      "pageSize": 20,
      "totalItems": 1240,
      "totalPages": 62
    }
  }
}
```

> **Ghi chú về `stats`:** Luôn là global stats (tổng toàn hệ thống), không bị ảnh hưởng bởi filter. Users có status `DELETED` bị loại khỏi stats.

### Response — Lỗi phổ biến

```json
// 401 Unauthorized
{ "code": 1006, "message": "Unauthenticated" }

// 403 Forbidden
{ "code": 1007, "message": "You do not have permission" }
```

### Validation BE áp dụng

- `role`/`status` không hợp lệ → bị bỏ qua (treat as `all`)
- Users với status `DELETED` luôn bị loại khỏi kết quả

### Ghi chú

- `lastLogin` được ghi lại mỗi khi user đăng nhập — yêu cầu DB migration (xem Checklist).
- `avatar` là URL đầy đủ — FE cần fallback nếu `null`.
- `roles` là mảng string: `["TEACHER"]`, `["STUDENT"]`, `["ADMIN"]`.

---

## 2. Tạo người dùng mới

**Trạng thái:** ⚠️ Đã có nhưng cần sửa — `POST /users` tồn tại nhưng field name khác và thiếu `sendWelcomeEmail`/`requirePasswordChange`

### Endpoint

```
POST /users
```

### Auth

- Header: `Authorization: Bearer <JWT>`
- Role được phép: `ADMIN`

### Request Body

```json
{
  "userName": "student_new",
  "fullName": "Trần Thị B",
  "email": "tranthib@mathmaster.vn",
  "password": "StrongP@ss123",
  "roles": ["STUDENT"],
  "status": "ACTIVE"
}
```

**Mapping với form FE:**

| Field FE                | Field BE        | Ghi chú                                 |
| ----------------------- | --------------- | --------------------------------------- |
| `name`                  | `fullName`      | ⚠️ Khác tên — FE cần đổi                |
| `email`                 | `email`         | ✅                                      |
| `role` (string)         | `roles` (array) | ⚠️ BE nhận mảng, VD: `["TEACHER"]`      |
| `status`                | `status`        | ✅                                      |
| `password`              | `password`      | ✅                                      |
| `sendWelcomeEmail`      | —               | ❌ Không hỗ trợ                         |
| `requirePasswordChange` | —               | ❌ Không hỗ trợ                         |
| —                       | `userName`      | ⚠️ **Bắt buộc thêm** — FE cần field này |

### Response — 200 OK

```json
{
  "code": 1000,
  "result": {
    "id": "018eac12-0000-7000-8000-000000000042",
    "userName": "student_new",
    "fullName": "Trần Thị B",
    "email": "tranthib@mathmaster.vn",
    "status": "ACTIVE",
    "roles": ["STUDENT"],
    "createdDate": "2026-04-11T07:00:00Z",
    "avatar": null,
    "lastLogin": null
  }
}
```

### Response — Lỗi phổ biến

```json
// 400 — Email trùng
{ "code": 1013, "message": "Email already exists" }

// 400 — Username trùng
{ "code": 1002, "message": "User existed" }

// 400 — Role không tồn tại
{ "code": 1009, "message": "Role not existed" }
```

### Validation BE áp dụng

| Field      | Ràng buộc                                                                                 |
| ---------- | ----------------------------------------------------------------------------------------- |
| `userName` | required, 3–50 ký tự                                                                      |
| `password` | required, 8–128 ký tự, phải có chữ hoa + chữ thường + số + ký tự đặc biệt (`!@#$%^&*...`) |
| `fullName` | required, 2–50 ký tự                                                                      |
| `email`    | required, format hợp lệ, tối đa 50 ký tự                                                  |
| `roles`    | tối đa 10 role; mỗi tên role phải tồn tại (`TEACHER`, `STUDENT`, `ADMIN`)                 |

---

## 3. Xem chi tiết người dùng

**Trạng thái:** ✅ Đã có (nay có thêm `lastLogin` sau migration)

### Endpoint

```
GET /admin/users/{userId}
```

hoặc

```
GET /users/{userId}
```

### Auth

- Header: `Authorization: Bearer <JWT>`
- Role được phép: `ADMIN` (cả hai); `TEACHER`, `STUDENT` chỉ dùng `/users/{userId}`

### Response — 200 OK

```json
{
  "code": 1000,
  "result": {
    "id": "018eac12-0000-7000-8000-000000000001",
    "userName": "teacher01",
    "fullName": "Nguyễn Văn A",
    "email": "teacher01@mathmaster.vn",
    "phoneNumber": "+84912345678",
    "gender": "MALE",
    "avatar": "https://...",
    "dob": "1990-05-15",
    "code": "GV001",
    "status": "ACTIVE",
    "lastLogin": "2026-04-10T08:30:00Z",
    "banReason": null,
    "banDate": null,
    "roles": ["TEACHER"],
    "createdDate": "2025-09-01T00:00:00Z",
    "updatedDate": "2026-04-10T08:30:00Z"
  }
}
```

### Response — Lỗi phổ biến

```json
// 404
{ "code": 1005, "message": "User not existed" }
```

---

## 4. Cập nhật trạng thái người dùng (Kích hoạt / Tạm ngưng)

**Trạng thái:** ⚠️ Đã có — hai PUT endpoints riêng + thêm PATCH mới hợp nhất

### Endpoint (mới, khuyến nghị)

```
PATCH /admin/users/{userId}/status?status=ACTIVE
PATCH /admin/users/{userId}/status?status=INACTIVE
```

### Hoặc dùng endpoints cũ

```
PUT /users/{userId}/enable       → set ACTIVE
PUT /users/{userId}/disable      → set INACTIVE
```

### Auth

- Header: `Authorization: Bearer <JWT>`
- Role được phép: `ADMIN`

### Response — 200 OK

```json
{
  "code": 1000,
  "result": {
    "id": "018eac12-0000-7000-8000-000000000001",
    "status": "INACTIVE",
    "userName": "teacher01",
    "fullName": "Nguyễn Văn A",
    "email": "teacher01@mathmaster.vn"
  }
}
```

### Response — Lỗi phổ biến

```json
// 400 — Đã ở trạng thái đó
{ "code": 1018, "message": "User is already disabled" }
{ "code": 1019, "message": "User is already enabled" }

// 400 — status value không hợp lệ
{ "code": 1001, "message": "status must be ACTIVE or INACTIVE" }

// 404
{ "code": 1005, "message": "User not existed" }
```

---

## 5. Đặt lại mật khẩu

**Trạng thái:** 🔨 Mới implement

### Endpoint

```
POST /admin/users/{userId}/reset-password
```

### Auth

- Header: `Authorization: Bearer <JWT>`
- Role được phép: `ADMIN`

### Request Body

Không có body.

### Response — 200 OK

```json
{
  "code": 1000,
  "message": "Password reset successfully. Temporary password sent to user's email."
}
```

### Response — Lỗi phổ biến

```json
// 404
{ "code": 1005, "message": "User not existed" }
```

### Ghi chú

- BE **tự sinh mật khẩu tạm 12 ký tự** → gửi email async cho user
- Mật khẩu tạm **không trả về response** vì lý do bảo mật
- FE chỉ cần hiển thị: "Mật khẩu mới đã được gửi đến email của người dùng"

---

## 6. Gửi email cho người dùng

**Trạng thái:** 🔨 Mới implement

### Endpoint

```
POST /admin/users/{userId}/send-email
```

### Auth

- Header: `Authorization: Bearer <JWT>`
- Role được phép: `ADMIN`

### Request Body

```json
{
  "subject": "Thông báo từ MathMaster",
  "body": "Xin chào,\n\nĐây là thông báo quan trọng..."
}
```

| Field   | Type   | Bắt buộc | Ràng buộc                   |
| ------- | ------ | -------- | --------------------------- |
| subject | string | ✅       | max 255 ký tự               |
| body    | string | ✅       | max 10000 ký tự, plain text |

### Response — 200 OK

```json
{
  "code": 1000,
  "message": "Email sent successfully."
}
```

### Ghi chú

- Gửi email **tùy ý** — không phải template cố định
- Email gửi **async** — API trả 200 ngay cả khi mail server chậm; nếu lỗi chỉ log, không throw

---

## 7. Xóa tài khoản

**Trạng thái:** ✅ Đã có

### Endpoint

```
DELETE /users/{userId}
```

### Auth

- Header: `Authorization: Bearer <JWT>`
- Role được phép: `ADMIN`

### Response — 200 OK

```json
{
  "code": 1000,
  "message": "User deleted successfully"
}
```

### Ghi chú

- **Soft delete** — set `status = DELETED`, dữ liệu giữ lại trong DB
- ⚠️ Chưa có guard chống xóa admin cuối cùng — FE nên hiển thị confirm dialog

---

## 8. Xuất Excel danh sách người dùng

**Trạng thái:** 🔨 Mới implement

### Endpoint

```
GET /admin/users/export
```

### Auth

- Header: `Authorization: Bearer <JWT>`
- Role được phép: `ADMIN`

### Query params

| Param  | Type   | Bắt buộc | Mô tả                                      |
| ------ | ------ | -------- | ------------------------------------------ |
| role   | string | ❌       | `TEACHER` \| `STUDENT` \| `ADMIN` \| `all` |
| search | string | ❌       | Tìm kiếm fullName/email/userName           |
| status | string | ❌       | `ACTIVE` \| `INACTIVE` \| `all`            |

### Response

- **Content-Type:** `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- **Content-Disposition:** `attachment; filename="users.xlsx"`

**Cột Excel:** `ID | Username | Full Name | Email | Role(s) | Status | Join Date | Last Login`

### Cách FE trigger download

```typescript
const res = await fetch("/admin/users/export?role=all&status=all", {
  headers: { Authorization: `Bearer ${token}` },
});
const blob = await res.blob();
const url = URL.createObjectURL(blob);
const a = document.createElement("a");
a.href = url;
a.download = "users.xlsx";
a.click();
URL.revokeObjectURL(url);
```

---

## 9. Chỉnh sửa thông tin người dùng

**Trạng thái:** ✅ Đã có

### Endpoint

```
PUT /users/{userId}
```

### Auth

- Header: `Authorization: Bearer <JWT>`
- Role được phép: `ADMIN`

### Request Body

```json
{
  "fullName": "Nguyễn Văn B (updated)",
  "email": "newemail@mathmaster.vn",
  "status": "ACTIVE",
  "roles": ["TEACHER"],
  "avatar": "https://...",
  "phoneNumber": null,
  "gender": "MALE",
  "dob": "1990-05-15",
  "code": "GV002"
}
```

**Mapping với FE đề xuất:**

| Field FE        | Field BE        | Ghi chú                               |
| --------------- | --------------- | ------------------------------------- |
| `name`          | `fullName`      | ⚠️ Khác tên                           |
| `role` (string) | `roles` (array) | ⚠️ BE nhận mảng                       |
| `email`         | `email`         | ✅ — nhưng `@NotBlank`, luôn phải gửi |
| `status`        | `status`        | ✅                                    |
| `avatar`        | `avatar`        | ✅ URL, max 2048 chars                |

### Response — 200 OK

Trả về `UserResponse` đầy đủ (giống Feature 3).

### Response — Lỗi phổ biến

```json
{ "code": 1013, "message": "Email already exists" }
{ "code": 1005, "message": "User not existed" }
```

---

## Tóm tắt thay đổi so với đề xuất FE

| Feature                                     | Thay đổi                                                      | Lý do                                |
| ------------------------------------------- | ------------------------------------------------------------- | ------------------------------------ |
| Base URL tất cả                             | Không có `/api` prefix                                        | Server không cấu hình `context-path` |
| Phân trang                                  | `page` zero-based (0, 1, 2...)                                | Spring Data convention               |
| Tạo/Sửa — tên field                         | `fullName` thay vì `name`                                     | Khớp entity/DB                       |
| Tạo/Sửa — role                              | `roles: ["TEACHER"]` (array, UPPER) thay vì `role: "teacher"` | Multi-role support                   |
| Tạo user                                    | `userName` là **bắt buộc** thêm                               | Field login, unique trong DB         |
| `sendWelcomeEmail`, `requirePasswordChange` | Không hỗ trợ                                                  | Chưa implement; để roadmap           |
| Toggle status                               | PATCH mới + hai PUT cũ                                        | Tương thích ngược                    |
| Reset password                              | BE tự sinh password tạm → email                               | Không expose qua API                 |
| Status                                      | 4 trạng thái: `ACTIVE`, `INACTIVE`, `BANNED`, `DELETED`       | Không chỉ 2                          |
| `lastLogin`                                 | Cần DB migration trước khi dùng                               | Column chưa có trong schema cũ       |

---

## Checklist trước khi FE integrate

- [ ] **[BẮT BUỘC] Chạy DB migration:**
  ```sql
  ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login TIMESTAMPTZ NULL;
  ```
- [ ] BE đã deploy lên staging
- [ ] Swagger: `http://<host>:8080/swagger-ui/index.html`
- [ ] FE cập nhật form "Tạo người dùng": thêm `userName` (required), đổi `name` → `fullName`, đổi `role` string → `roles` array
- [ ] FE xử lý `page` zero-based
- [ ] FE handle `BANNED` status (khác với `INACTIVE`)
- [ ] FE bỏ `sendWelcomeEmail` và `requirePasswordChange` khỏi create form

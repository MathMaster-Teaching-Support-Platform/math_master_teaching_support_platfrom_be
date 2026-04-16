# API Contract — Forgot Password / Reset Password

**Ngày tạo:** 2026-04-16  
**Người tạo:** BE Team  
**Phản hồi cho:** Bug report — `POST /auth/forgot-password` trả về 401  
**Trạng thái:** ✅ Đã implement — sẵn sàng integrate

---

## 1. Forgot Password — Gửi email đặt lại mật khẩu

**Trạng thái:** 🔨 Mới implement

### Endpoint

```
POST /api/auth/forgot-password
```

### Auth

- **Không cần** `Authorization` header — endpoint public, không yêu cầu JWT
- Đã được thêm vào `permitAll()` trong Spring Security config

### Request

**Headers:**
| Header | Giá trị | Bắt buộc |
|----------------|--------------------|----------|
| Content-Type | application/json | ✅ |

**Request body:**

```json
{
  "email": "user@example.com"
}
```

**TypeScript interface:**

```ts
interface ForgotPasswordRequest {
  email: string; // valid email format
}
```

### Response — 200 OK (mọi trường hợp)

```json
{
  "code": 1000,
  "result": null
}
```

> ⚠️ **Anti user-enumeration:** Server luôn trả về `200 OK` dù email tồn tại hay không. FE **không** nên dựa vào response để suy luận email có trong hệ thống hay không. Hiển thị thông báo chung: _"Nếu email tồn tại trong hệ thống, bạn sẽ nhận được hướng dẫn đặt lại mật khẩu."_

### Response — Lỗi phổ biến

```json
// 400 Bad Request — email không đúng định dạng
{
  "code": 400,
  "message": "Email must be valid"
}
```

### Validation BE áp dụng

| Field | Bắt buộc | Validation                   |
| ----- | -------- | ---------------------------- |
| email | ✅       | `@NotBlank`, `@Email` format |

### Ghi chú kỹ thuật

- Nếu email tồn tại trong hệ thống → BE gửi email chứa link:  
  `{FRONTEND_URL}/reset-password?token=<JWT>`
- Token trong link có hiệu lực **15 phút**
- Gửi email bất đồng bộ (`@Async`) — không ảnh hưởng response time

---

## 2. Reset Password — Đặt lại mật khẩu bằng token

**Trạng thái:** 🔨 Mới implement

### Endpoint

```
POST /api/auth/reset-password
```

### Auth

- **Không cần** `Authorization` header — endpoint public, không yêu cầu JWT
- Đã được thêm vào `permitAll()` trong Spring Security config

### Request

**Headers:**
| Header | Giá trị | Bắt buộc |
|----------------|--------------------|----------|
| Content-Type | application/json | ✅ |

**Request body:**

```json
{
  "token": "<JWT từ link email>",
  "newPassword": "NewPass@123"
}
```

**TypeScript interface:**

```ts
interface ResetPasswordRequest {
  token: string;
  newPassword: string; // min 8, max 128 ký tự, phức tạp (xem validation bên dưới)
}
```

### Response — 200 OK

```json
{
  "code": 1000,
  "result": null
}
```

### Response — Lỗi phổ biến

```json
// 401 Unauthorized — token hết hạn, sai, hoặc không phải purpose=password-reset
{
  "code": 1006,
  "message": "Unauthenticated"
}

// 400 Bad Request — mật khẩu không đủ độ phức tạp
{
  "code": 400,
  "message": "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
}

// 400 Bad Request — mật khẩu quá ngắn
{
  "code": 400,
  "message": "Password must be between 8 and 128 characters"
}
```

### Validation BE áp dụng

| Field       | Bắt buộc | Validation                                                                |
| ----------- | -------- | ------------------------------------------------------------------------- |
| token       | ✅       | `@NotBlank` — JWT hợp lệ, chưa hết hạn, có claim `purpose=password-reset` |
| newPassword | ✅       | `@NotBlank`, 8–128 ký tự, phải có chữ hoa, chữ thường, số, ký tự đặc biệt |

**Regex mật khẩu:**

```
^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]).+$
```

### Ghi chú kỹ thuật

- Token là JWT ký bởi BE, hiệu lực **15 phút** kể từ khi gửi email
- Sau khi đặt lại mật khẩu thành công, token **không bị invalidate** chủ động (hết hạn tự nhiên sau 15 phút). FE nên chuyển hướng về trang login ngay sau khi thành công
- Token chỉ có thể dùng để reset password — có claim `purpose=password-reset`, không thể dùng như access token

---

## Flow tổng quan cho FE

```
1. FE: POST /api/auth/forgot-password  { email }
   → Hiển thị: "Nếu email tồn tại, bạn sẽ nhận được email hướng dẫn."

2. User: Mở email → click link
   → Browser mở: /reset-password?token=<JWT>

3. FE: Lấy ?token từ URL params
   → Hiển thị form nhập mật khẩu mới

4. FE: POST /api/auth/reset-password  { token, newPassword }
   → Thành công → redirect về /login
   → Thất bại (401) → "Link đặt lại mật khẩu đã hết hạn hoặc không hợp lệ. Vui lòng thử lại."
```

---

## Danh sách thay đổi so với bug report của FE

| Feature         | Thay đổi so với yêu cầu                         | Lý do                                                                |
| --------------- | ----------------------------------------------- | -------------------------------------------------------------------- |
| forgot-password | Implement mới hoàn toàn (endpoint chưa tồn tại) | BE chưa có, không phải chỉ thiếu whitelist                           |
| reset-password  | Implement mới hoàn toàn                         | BE chưa có                                                           |
| Response shape  | `{ "code": 1000, "result": null }` — đúng spec  | Khớp với ApiResponse wrapper chung của project                       |
| Token lifetime  | 15 phút                                         | Ngắn hơn so với email-confirmation (24h) vì đây là thao tác nhạy cảm |

---

## Checklist trước khi FE integrate

- [x] Cả 2 endpoint đã được whitelist trong Spring Security (`permitAll()`)
- [x] Email template đã có (`password-reset.html`)
- [x] Validation khớp chính xác với constraint trong DTO
- [ ] Deploy lên môi trường staging
- [ ] Test end-to-end với email thật

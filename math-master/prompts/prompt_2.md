# PROMPT 2 — Backend Developer → Đọc `X.md`, implement & sinh file `Y.md`

## Mục tiêu

Bạn là một **Backend Developer**. Bạn vừa nhận được file `X.md` từ FE team mô tả các API cần thiết. Nhiệm vụ của bạn:

1. **Đọc kỹ `X.md`** — hiểu từng feature, endpoint được đề xuất, request/response shape, và validation FE đang dùng.
2. **Kiểm tra codebase BE hiện tại**:
   - API đã tồn tại? → Cung cấp URL chính xác, response thực tế.
   - API chưa có hoặc thiếu field? → Implement / cập nhật, rồi mô tả kết quả.
   - API không khả thi như FE đề xuất? → Giải thích lý do và đề xuất phương án thay thế.
3. **Viết file `Y.md`** gửi lại FE với thông tin đầy đủ, chính xác để FE có thể tích hợp ngay.

---

## Đầu vào (Input)

- File `X.md` từ FE (đính kèm toàn bộ nội dung)
- Codebase backend hiện tại (routes, controllers, models, middleware)

---

## Yêu cầu xử lý theo từng feature trong `X.md`

Với mỗi mục trong `X.md`, BE phải ghi rõ:

1. **Trạng thái API**:
   - `✅ Đã có` — endpoint tồn tại, đúng spec
   - `⚠️ Đã có nhưng cần sửa` — thiếu field, sai method, cần update
   - `🔨 Mới implement` — chưa có, đã tạo mới trong PR/commit này
   - `❌ Không khả thi` — giải thích lý do + phương án thay thế

2. **Endpoint chính thức** (URL thực tế, không phải gợi ý):
   - Base URL / prefix
   - Full path với params

3. **Auth & Permission**:
   - Có cần JWT Bearer không?
   - Role nào được phép gọi? (student, teacher, admin...)

4. **Request chính thức**:
   - Headers bắt buộc
   - Path params, query params, request body (JSON schema hoặc TypeScript interface)

5. **Response chính thức**:
   - HTTP status codes (200, 201, 400, 401, 403, 404, 500...)
   - Response body mẫu cho trường hợp **thành công**
   - Response body mẫu cho trường hợp **lỗi phổ biến**

6. **Validation BE đang áp dụng** (để FE match chính xác):
   - Các trường bắt buộc, optional
   - Kiểu dữ liệu, min/max, format (UUID, ISO date, enum values...)
   - Error message mẫu

7. **Ghi chú kỹ thuật**:
   - Có phân trang không? (page, limit, totalCount)
   - Có rate limit không?
   - Dữ liệu có cache không? TTL bao lâu?
   - Có webhook / realtime update không?

---

## Format output — file `Y.md`

```markdown
# API Contract — [Tên dự án / Module]

**Ngày tạo:** YYYY-MM-DD  
**Người tạo:** BE Team  
**Phản hồi cho:** X.md (FE spec request)  
**Trạng thái:** Chờ FE confirm

---

## 1. [Tên Feature — khớp với X.md]

**Trạng thái:** ✅ Đã có

### Endpoint
```

GET /api/v1/students/:studentId/grades

````

### Auth
- Header: `Authorization: Bearer <JWT>`
- Role được phép: `student` (chỉ xem data của chính mình), `admin`

### Request

**Path params:**
| Param       | Type   | Bắt buộc | Mô tả              |
|-------------|--------|----------|--------------------|
| studentId   | string (UUID) | ✅ | ID của học sinh |

**Query params:**
| Param    | Type   | Bắt buộc | Mô tả                    |
|----------|--------|----------|--------------------------|
| semester | string | ❌ | Lọc theo kỳ học, mặc định kỳ hiện tại |
| page     | number | ❌ | Mặc định: 1              |
| limit    | number | ❌ | Mặc định: 20, tối đa: 100 |

### Response — 200 OK
```json
{
  "success": true,
  "data": {
    "studentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "grades": [
      {
        "subjectId": "sub_001",
        "subjectName": "Toán Đại Số",
        "score": 8.5,
        "maxScore": 10,
        "semester": "2024-1",
        "updatedAt": "2024-03-15T08:00:00Z"
      }
    ],
    "pagination": {
      "page": 1,
      "limit": 20,
      "totalCount": 12,
      "totalPages": 1
    }
  }
}
````

### Response — Lỗi phổ biến

```json
// 401 Unauthorized
{ "success": false, "error": "TOKEN_INVALID", "message": "Token không hợp lệ hoặc đã hết hạn" }

// 403 Forbidden
{ "success": false, "error": "ACCESS_DENIED", "message": "Bạn không có quyền xem dữ liệu này" }

// 404 Not Found
{ "success": false, "error": "STUDENT_NOT_FOUND", "message": "Không tìm thấy học sinh" }
```

### Validation BE áp dụng

- `studentId` phải là UUID hợp lệ → lỗi 400 nếu sai format
- `score` luôn trong khoảng [0, maxScore] — đảm bảo ở tầng DB
- `semester` format: `YYYY-[1|2]` ví dụ `2024-1`

### Ghi chú

- Data được cache 5 phút tại Redis key: `grades:student:{studentId}:semester:{semester}`
- Khi giáo viên cập nhật điểm, cache sẽ bị invalidate tự động

---

## 2. [Feature tiếp theo]

...

---

## Danh sách thay đổi so với đề xuất của FE

| Feature        | Thay đổi                              | Lý do                                  |
| -------------- | ------------------------------------- | -------------------------------------- |
| Student Grades | Thêm field `updatedAt` trong response | FE cần để hiển thị "cập nhật lần cuối" |
| ...            | ...                                   | ...                                    |

---

## Checklist trước khi FE integrate

- [ ] BE đã deploy lên môi trường staging
- [ ] Swagger/Postman collection đã cập nhật tại: [link]
- [ ] Seed data test đã sẵn sàng (studentId mẫu: `abc-123-...`)
- [ ] Mọi endpoint đã test với Postman/curl thành công

```

---

## Lưu ý khi viết `Y.md`
- **Phải khớp tuyệt đối** với thực tế BE — không ghi spec rồi implement khác
- Nếu thay đổi gì so với đề xuất FE, **ghi rõ vào bảng thay đổi**
- Cung cấp **ít nhất 1 ví dụ response thực tế** (copy từ Postman/Swagger)
- Nếu có breaking change so với version cũ (nếu có), đánh dấu `⚠️ BREAKING`
- Ghi rõ **error code dạng string** (`TOKEN_INVALID`, không chỉ `401`) để FE handle chính xác
```

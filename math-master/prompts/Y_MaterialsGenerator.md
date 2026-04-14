# API Contract — MaterialsGenerator (Tạo Tài Liệu với AI)

**Ngày tạo:** 2026-04-15  
**Người tạo:** BE Team  
**Phản hồi cho:** X_MaterialsGenerator.md (FE spec request)  
**Trạng thái:** Chờ FE confirm

> **Base URL:** `http://localhost:8080` (dev) — không có prefix `/api`  
> **Auth:** Tất cả endpoint (trừ ghi chú riêng) đều yêu cầu `Authorization: Bearer <JWT>`

---

## 1. Danh sách tài liệu AI đã tạo gần đây

**Trạng thái:** ❌ Không khả thi như đề xuất

### Lý do

Endpoint `/api/materials` (unified, trả về mọi loại tài liệu) **chưa tồn tại** và **chưa được implement**. Lý do kỹ thuật:

- Phiếu bài tập (`worksheet`) và Hình vẽ toán học (`math-drawing`) chưa có entity, service hay controller.
- Slide và Mindmap lưu ở hai bảng khác nhau với schema khác nhau — không có bảng chung.

### Phương án thay thế (BE đề xuất)

FE gọi **2 endpoint riêng** rồi merge client-side, hoặc hiển thị theo tab loại:

| Loại tài liệu        | Endpoint BE thực tế            |
| -------------------- | ------------------------------ |
| Slide (PPTX đã xuất) | `GET /lesson-slides/generated` |
| Mindmap              | `GET /mindmaps/my-mindmaps`    |
| Hình vẽ toán học     | ❌ Chưa implement (xem mục 4)  |
| Phiếu bài tập        | ❌ Chưa implement (xem mục 5)  |

---

## 2. Slide Bài Giảng — Đã có, khác spec FE

**Trạng thái:** ⚠️ Đã có nhưng cần FE điều chỉnh để match response thực tế

### 2a. Lấy danh sách slide đã xuất (generated files)

#### Endpoint

```
GET /lesson-slides/generated
```

#### Auth

- Header: `Authorization: Bearer <JWT>`
- Role: `TEACHER`

#### Request — Query params

| Param    | Type | Bắt buộc | Mô tả                   |
| -------- | ---- | -------- | ----------------------- |
| lessonId | UUID | ❌       | Lọc theo bài học cụ thể |

> ⚠️ **BREAKING so với FE spec:** Không hỗ trợ `search`, `page`, `limit`, `type` — trả về toàn bộ danh sách của teacher hiện tại.

#### Response — 200 OK

```json
{
  "code": 1000,
  "result": [
    {
      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "lessonId": "a1b2c3d4-0000-0000-0000-000000000001",
      "templateId": "t1t1t1t1-0000-0000-0000-000000000001",
      "fileName": "Slide_MenhDe_LogicToan.pptx",
      "contentType": "application/vnd.openxmlformats-officedocument.presentationml.presentation",
      "fileSizeBytes": 2621440,
      "isPublic": true,
      "publishedAt": "2026-01-21T03:00:00Z",
      "createdAt": "2026-01-20T10:00:00Z",
      "updatedAt": "2026-01-21T03:00:00Z"
    }
  ]
}
```

#### ⚠️ Các field FE mock nhưng BE không trả về

| Field FE cần             | Trạng thái BE                                      | Hướng xử lý                                                                 |
| ------------------------ | -------------------------------------------------- | --------------------------------------------------------------------------- |
| `status`                 | ❌ Không có field này — slide luôn là file đã xong | FE có thể map `isPublic=true` → `done`, `isPublic=false` → `done (private)` |
| `downloadUrl`            | ❌ Không có trong response                         | FE tự construct: `GET /lesson-slides/generated/{id}/download`               |
| `previewUrl`             | ❌ Không có                                        | Không hỗ trợ preview URL cho slide                                          |
| `tags`                   | ❌ Không có                                        | N/A                                                                         |
| `downloads` (count)      | ❌ Không có                                        | N/A                                                                         |
| `size` (string "2.5 MB") | Có `fileSizeBytes` (number)                        | FE tự format: `(fileSizeBytes / 1048576).toFixed(1) + ' MB'`                |
| `type`                   | Suy luận từ `contentType`                          | `application/vnd.openxmlformats...` = `pptx`                                |

#### 2b. Tải file slide đã xuất

```
GET /lesson-slides/generated/{generatedFileId}/download
```

- Response: binary file (`Content-Type: application/vnd.openxmlformats...`)
- Header: `Content-Disposition: attachment; filename*=UTF-8''...`

#### 2c. Các endpoint slide khác (đã có)

| Endpoint                                        | Mục đích               |
| ----------------------------------------------- | ---------------------- |
| `GET /lesson-slides/templates`                  | Lấy danh sách template |
| `POST /lesson-slides/generate-content`          | AI sinh nội dung slide |
| `POST /lesson-slides/generate-pptx`             | Xuất PPTX              |
| `POST /lesson-slides/generate-pptx-from-json`   | Xuất PPTX từ JSON      |
| `PATCH /lesson-slides/generated/{id}/publish`   | Publish file slide     |
| `PATCH /lesson-slides/generated/{id}/unpublish` | Unpublish file slide   |

### Ghi chú

- Route FE điều hướng đến trang tạo: `/teacher/ai-slide-generator` ✅ đúng như FE đả biết

---

## 3. Sơ Đồ Tư Duy (Mindmap) — Đã có, có 1 điểm khác spec

**Trạng thái:** ⚠️ Đã có nhưng endpoint `unpublish` không tồn tại

### 3a. Lấy danh sách mindmap của giáo viên

#### Endpoint

```
GET /mindmaps/my-mindmaps
```

#### Auth

- Header: `Authorization: Bearer <JWT>`
- Role: `TEACHER` hoặc `ADMIN`

#### Request — Query params

| Param     | Type          | Bắt buộc | Default     | Mô tả                                         |
| --------- | ------------- | -------- | ----------- | --------------------------------------------- |
| lessonId  | string (UUID) | ❌       | —           | Lọc theo bài học                              |
| page      | number        | ❌       | 0           | **0-indexed** (khác FE nếu FE dùng 1-indexed) |
| size      | number        | ❌       | 10          | Số items mỗi trang                            |
| sortBy    | string        | ❌       | `createdAt` | Field sắp xếp                                 |
| direction | string        | ❌       | `DESC`      | `ASC` hoặc `DESC`                             |

#### Response — 200 OK

```json
{
  "code": 1000,
  "result": {
    "content": [
      {
        "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "teacherId": "teacher-uuid",
        "teacherName": "Phạm Đăng Khôi",
        "lessonId": "lesson-uuid-or-null",
        "lessonTitle": "Mệnh đề và logic",
        "title": "Mindmap: Mệnh đề và logic",
        "description": "Sơ đồ tư duy về các khái niệm cơ bản",
        "aiGenerated": true,
        "generationPrompt": "Vẽ mindmap về mệnh đề logic lớp 10",
        "status": "PUBLISHED",
        "nodeCount": 15,
        "createdAt": "2026-01-20T10:00:00Z",
        "updatedAt": "2026-01-21T03:00:00Z"
      }
    ],
    "pageable": { "pageNumber": 0, "pageSize": 10 },
    "totalElements": 42,
    "totalPages": 5,
    "last": false,
    "first": true
  }
}
```

#### ⚠️ Các field FE mock nhưng BE không trả về

| Field FE cần                             | Trạng thái BE                                                             | Hướng xử lý                                                                                |
| ---------------------------------------- | ------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------ |
| `status` string `done/processing/failed` | BE trả `status` là `MindmapStatus` enum: `DRAFT`, `PUBLISHED`, `ARCHIVED` | FE map: `PUBLISHED` → `done`, `DRAFT` → `processing` (xấp xỉ), `ARCHIVED` → không hiển thị |
| `downloadUrl` / `previewUrl`             | ❌ Không có — Mindmap là JSON render web, không phải file                 | FE điều hướng đến `/teacher/mindmaps/{id}` để xem                                          |
| `tags`                                   | ❌ Không có                                                               | N/A                                                                                        |
| `downloads` (count)                      | ❌ Không có                                                               | N/A                                                                                        |
| `format`                                 | Mindmap không có format file                                              | N/A                                                                                        |

### 3b. Các endpoint mindmap khác (đã có)

| Endpoint                       | Mục đích              |
| ------------------------------ | --------------------- |
| `POST /mindmaps/generate`      | AI sinh mindmap       |
| `GET /mindmaps/{id}`           | Chi tiết 1 mindmap    |
| `PATCH /mindmaps/{id}/publish` | Xuất bản              |
| `PATCH /mindmaps/{id}/archive` | Gỡ xuất bản (archive) |

> ⚠️ **BREAKING — quan trọng:** FE spec dùng `POST /mindmaps/:id/unpublish` — **endpoint này không tồn tại**.  
> BE dùng `PATCH /mindmaps/{id}/archive` (HTTP method và path đều khác).  
> FE cần đổi lại.

---

## 4. Hình Vẽ Toán Học

**Trạng thái:** ❌ Chưa có — cần implement mới

Hiện tại **không có** entity, service, controller, hay bảng DB nào liên quan đến `math-drawing` trong codebase. BE cần thảo luận và confirm trước khi implement:

### Câu hỏi cần confirm

| #   | Câu hỏi                                                                                    | Trả lời BE                                      |
| --- | ------------------------------------------------------------------------------------------ | ----------------------------------------------- |
| 1   | Dùng thư viện/service nào để render hình? (GeoGebra API, Python matplotlib, JFreeChart...) | **Chưa quyết định** — cần họp kỹ thuật          |
| 2   | Output format: PNG hay SVG?                                                                | **Chưa quyết định**                             |
| 3   | Sync hay async? (render nhanh < 5s → sync; chậm hơn → job queue)                           | **Chưa quyết định**                             |
| 4   | Lưu file output ở đâu? (MinIO, S3, local storage như slide?)                               | MinIO (cùng infra với slide) — **chưa confirm** |

### Endpoint đề xuất (chờ confirm trước khi implement)

#### Sinh hình vẽ

```
POST /math-drawings/generate
```

**Request body (đề xuất):**

```json
{
  "lessonId": "UUID (optional)",
  "prompt": "string (bắt buộc — mô tả hình vẽ)",
  "type": "function-graph | geometry | number-line | other",
  "title": "string (bắt buộc)"
}
```

**Validation BE sẽ áp dụng:**

- `prompt`: `@NotBlank`, max 1000 ký tự
- `title`: `@NotBlank`, max 255 ký tự
- `type`: `@NotNull`, phải thuộc enum `function-graph | geometry | number-line | other`
- `lessonId`: UUID hợp lệ nếu cung cấp

**Response (đề xuất — chờ confirm format output):**

```json
{
  "code": 1000,
  "result": {
    "id": "UUID",
    "title": "string",
    "imageUrl": "string (URL PNG từ MinIO)",
    "type": "function-graph",
    "createdAt": "ISO 8601",
    "status": "done | processing | failed"
  }
}
```

#### Lấy danh sách hình vẽ

```
GET /math-drawings/my-drawings
```

**Query params:**

| Param     | Type               | Default     |
| --------- | ------------------ | ----------- |
| page      | number (0-indexed) | 0           |
| size      | number             | 10          |
| sortBy    | string             | `createdAt` |
| direction | string             | `DESC`      |

> ⚠️ BE sẽ dùng `page` 0-indexed (Spring Pageable), không phải 1-indexed như FE spec gợi ý.

---

## 5. Phiếu Bài Tập

**Trạng thái:** ❌ Chưa có — cần implement mới

Hiện tại **không có** entity, service, controller nào cho worksheet. Cần confirm approach trước khi implement:

### Câu hỏi cần confirm

| #   | Câu hỏi                                                                  | Trả lời BE                                      |
| --- | ------------------------------------------------------------------------ | ----------------------------------------------- |
| 1   | AI generate câu hỏi mới hay lấy từ `question_bank` hiện có?              | **Chưa quyết định**                             |
| 2   | Render PDF: dùng thư viện nào? (iText, Apache PDFBox, Jasper Reports...) | **Chưa quyết định**                             |
| 3   | Process là sync hay async (vì PDF generation thường chậm)?               | **Khuyến nghị async** — job queue + polling     |
| 4   | Lưu PDF ở đâu?                                                           | MinIO (cùng infra với slide) — **chưa confirm** |

### Endpoint đề xuất (chờ confirm)

#### Tạo phiếu bài tập

```
POST /worksheets/generate
```

**Request body (đề xuất):**

```json
{
  "lessonId": "UUID (optional)",
  "title": "string (bắt buộc)",
  "prompt": "string (bắt buộc)",
  "numberOfQuestions": "number (optional, default: 10, range: [1, 50])",
  "difficulty": "easy | medium | hard | mixed (optional, default: mixed)",
  "includeAnswerKey": "boolean (optional, default: true)"
}
```

**Validation BE sẽ áp dụng:**

- `title`: `@NotBlank`, max 255 ký tự
- `prompt`: `@NotBlank`, max 2000 ký tự
- `numberOfQuestions`: `@Min(1)`, `@Max(50)`
- `difficulty`: phải thuộc enum nếu cung cấp

**Response khi async (job queued):**

```json
{
  "code": 1000,
  "message": "Worksheet generation started",
  "result": {
    "id": "UUID",
    "title": "string",
    "status": "processing",
    "pdfUrl": null,
    "previewUrl": null,
    "createdAt": "ISO 8601",
    "numberOfQuestions": 10
  }
}
```

**Response polling — lấy trạng thái:**

```
GET /worksheets/{id}
```

```json
{
  "code": 1000,
  "result": {
    "id": "UUID",
    "title": "string",
    "status": "done",
    "pdfUrl": "https://minio.../worksheets/uuid.pdf",
    "previewUrl": null,
    "createdAt": "ISO 8601",
    "numberOfQuestions": 10
  }
}
```

#### Lấy danh sách phiếu bài tập

```
GET /worksheets/my-worksheets
```

**Query params:** (tương tự mindmap/math-drawing)

| Param     | Type               | Default     |
| --------- | ------------------ | ----------- |
| page      | number (0-indexed) | 0           |
| size      | number             | 10          |
| sortBy    | string             | `createdAt` |
| direction | string             | `DESC`      |

---

## 6. Lịch sử tạo tài liệu

**Trạng thái:** ⚠️ Không có unified endpoint — dùng endpoint riêng theo loại

**Xác nhận:** BE **không có** và **không có kế hoạch tạo** endpoint unified `GET /api/materials`. Lịch sử mỗi loại cần lấy từ endpoint riêng:

| Loại    | Endpoint                                    | Phân trang                    |
| ------- | ------------------------------------------- | ----------------------------- |
| Slide   | `GET /lesson-slides/generated?lessonId=...` | ❌ Không (trả full list)      |
| Mindmap | `GET /mindmaps/my-mindmaps`                 | ✅ `page`, `size` (0-indexed) |
| Hình vẽ | `GET /math-drawings/my-drawings`            | ✅ (khi implement xong)       |
| Phiếu   | `GET /worksheets/my-worksheets`             | ✅ (khi implement xong)       |

**Gợi ý FE:** Trang "Lịch sử tạo" mở modal/tab với 4 section tương ứng, hoặc chỉ show slide + mindmap (2 loại đã có) cho đến khi math-drawing và worksheet được implement.

---

## 7. Thông tin giáo viên (User Context)

**Trạng thái:** ✅ Đã có

### 7a. Lấy thông tin user hiện tại

#### Endpoint

```
GET /users/my-info
```

#### Auth

- Header: `Authorization: Bearer <JWT>`
- Role: Tất cả authenticated users

#### Response — 200 OK

```json
{
  "code": 1000,
  "result": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "userName": "phamkhoi",
    "fullName": "Phạm Đăng Khôi",
    "email": "khoi@example.com",
    "phoneNumber": "0912345678",
    "gender": "MALE",
    "avatar": "https://storage.example.com/avatars/khoi.jpg",
    "dob": "1990-05-15",
    "code": "TCH001",
    "status": "ACTIVE",
    "lastLogin": "2026-04-14T08:00:00Z",
    "roles": ["TEACHER"],
    "createdDate": "2025-01-01T00:00:00Z",
    "createdBy": "admin",
    "updatedDate": "2026-04-01T00:00:00Z",
    "updatedBy": "phamkhoi"
  }
}
```

> **Ghi chú FE:** `avatar` là URL đầy đủ (nếu đã upload), không phải chữ viết tắt. FE cần handle `null` và fallback sang chữ viết tắt từ `fullName`.  
> `role` nằm trong array `roles` — FE lấy `roles[0]` hoặc check `roles.includes('TEACHER')`.

---

### 7b. Số thông báo chưa đọc

#### Endpoint

```
GET /v1/notifications/unread-count
```

#### Auth

- Header: `Authorization: Bearer <JWT>`
- Role: Tất cả authenticated users

#### Response — 200 OK

```json
{
  "unreadCount": 5
}
```

> ⚠️ **Lưu ý:** Endpoint này trả về **plain JSON** (không wrap trong `ApiResponse`), khác với các endpoint khác.  
> FE đọc trực tiếp `response.data.unreadCount`.

#### Notification endpoints khác

| Endpoint                            | Mục đích                                   |
| ----------------------------------- | ------------------------------------------ |
| `GET /v1/notifications`             | Danh sách notification (paginated)         |
| `PATCH /v1/notifications/{id}/read` | Đánh dấu 1 notification đã đọc             |
| `PATCH /v1/notifications/read-all`  | Đánh dấu tất cả đã đọc                     |
| `GET /v1/notifications/token`       | Lấy Centrifugo connection token (realtime) |

---

## Danh sách thay đổi so với đề xuất của FE

| Feature                        | Thay đổi                                                            | Lý do                                                       |
| ------------------------------ | ------------------------------------------------------------------- | ----------------------------------------------------------- |
| Mục 1 — Unified materials list | `GET /api/materials` không tồn tại → dùng 2 endpoint riêng          | Không có bảng/entity chung cho tất cả loại tài liệu         |
| Mục 2 — Slide list             | Không hỗ trợ `search`, `page`, `limit`, `type` query params         | `GET /lesson-slides/generated` trả về full list của teacher |
| Mục 2 — Slide response         | Không có `status`, `tags`, `downloads`, `downloadUrl`, `previewUrl` | Không có trong model — FE tự xử lý theo bảng mapping ở trên |
| Mục 2 — Slide download         | URL download không có trong response                                | FE construct: `/lesson-slides/generated/{id}/download`      |
| Mục 3 — Mindmap unpublish      | `POST /mindmaps/:id/unpublish` không tồn tại                        | BE dùng `PATCH /mindmaps/{id}/archive` (method + path khác) |
| Mục 3 — Mindmap status         | `done/processing/failed` → `PUBLISHED/DRAFT/ARCHIVED`               | BE dùng `MindmapStatus` enum, không phải string job status  |
| Mục 3 — Mindmap pagination     | `page` là 0-indexed (Spring)                                        | FE spec gợi ý `page: 1` nhưng BE dùng `page: 0`             |
| Mục 4 — Math drawing           | Chưa implement                                                      | Cần xác định tech stack trước                               |
| Mục 5 — Worksheet              | Chưa implement                                                      | Cần xác định tech stack trước                               |
| Mục 7 — User info              | `GET /users/my-info` (không phải `/me` hay `/profile`)              | Endpoint đã có với path này                                 |
| Mục 7 — Notification count     | Response không wrap `ApiResponse`                                   | `{ "unreadCount": 5 }` — plain JSON                         |

---

## Checklist trước khi FE integrate

- [x] `GET /lesson-slides/generated` — có thể test ngay
- [x] `GET /mindmaps/my-mindmaps` — có thể test ngay
- [x] `GET /users/my-info` — có thể test ngay
- [x] `GET /v1/notifications/unread-count` — có thể test ngay
- [ ] Math drawing — chờ confirm tech stack và implement
- [ ] Worksheet — chờ confirm approach và implement
- [ ] Swagger cập nhật: `http://localhost:8080/swagger-ui.html`
- [ ] Thống nhất với FE về việc FE xử lý mapping `MindmapStatus` → badge màu

---

## Câu hỏi cần họp để quyết định

1. **Math Drawing:** Dùng thư viện render nào? Output PNG hay SVG? Sync hay async?
2. **Worksheet:** AI generate hay lấy từ question bank? PDF library nào? Sync hay async?
3. **Slide list:** FE có cần `search` và phân trang server-side không? Nếu có, BE cần update `GET /lesson-slides/generated` để hỗ trợ.
4. **Unified history:** FE có muốn BE tạo thêm `GET /teacher/materials/history` hợp nhất không? Chỉ khả thi sau khi worksheet và math-drawing được implement.

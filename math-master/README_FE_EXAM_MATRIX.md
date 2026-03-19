# API Tài Liệu — Ma Trận Đề Thi (Exam Matrix)

> **Base URL**: `http://localhost:8080`  
> **Auth**: Bearer JWT token trong header `Authorization: Bearer <token>`  
> **Quyền**: Tất cả API này yêu cầu role `TEACHER` hoặc `ADMIN`

---

## Tổng Quan Luồng

Có **2 luồng** để tạo ma trận:

### Luồng 1 — Build một lần (Khuyến nghị)

Tạo ma trận hoàn chỉnh trong **1 request duy nhất** bằng `POST /exam-matrices/build`.

```
POST /exam-matrices/build  →  GET /exam-matrices/{id}/table  →  POST /exam-matrices/{id}/validate  →  POST /exam-matrices/{id}/approve
```

### Luồng 2 — Tạo thủ công từng bước

Tạo rỗng → thêm từng row → validate → approve.

```
POST /exam-matrices  →  POST /exam-matrices/{id}/rows  →  ...  →  POST /exam-matrices/{id}/validate  →  POST /exam-matrices/{id}/approve
```

---

## Trạng Thái Ma Trận (`status`)

```
DRAFT  →  APPROVED  →  LOCKED
  ↑           │
  └───────────┘ (reset về DRAFT để chỉnh lại)
```

| Status     | Ý nghĩa                                          | Có thể chỉnh sửa? |
| ---------- | ------------------------------------------------ | ----------------- |
| `DRAFT`    | Mới tạo, đang chỉnh sửa                          | ✅ Có             |
| `APPROVED` | Giáo viên đã duyệt, sẵn sàng dùng cho assessment | ❌ Không          |
| `LOCKED`   | Hệ thống khoá khi assessment được publish        | ❌ Không          |

---

## Mức Độ Nhận Thức (`cognitiveLevel`)

| Enum value     | Viết tắt | Ý nghĩa      |
| -------------- | -------- | ------------ |
| `NHAN_BIET`    | NB       | Nhận biết    |
| `THONG_HIEU`   | TH       | Thông hiểu   |
| `VAN_DUNG`     | VD       | Vận dụng     |
| `VAN_DUNG_CAO` | VDC      | Vận dụng cao |

---

## PHẦN 1 — CRUD Ma Trận

### 1.1 Tạo ma trận mới (rỗng)

```
POST /exam-matrices
```

**Request body:**

```json
{
  "name": "Ma trận đề Toán 12 HK1",
  "description": "Ma trận đề thi cuối kỳ 1 Toán 12",
  "isReusable": true,
  "totalQuestionsTarget": 50,
  "totalPointsTarget": 10.0
}
```

| Field                  | Bắt buộc | Mô tả                                        |
| ---------------------- | -------- | -------------------------------------------- |
| `name`                 | ✅       | Tên ma trận, tối đa 255 ký tự                |
| `description`          | ❌       | Mô tả                                        |
| `isReusable`           | ❌       | Ma trận có thể tái sử dụng cho đề khác không |
| `totalQuestionsTarget` | ❌       | Tổng số câu mục tiêu (dùng để validate)      |
| `totalPointsTarget`    | ❌       | Tổng điểm mục tiêu (dùng để validate)        |

**Response:** `ExamMatrixResponse` (xem mục 7.1)

---

### 1.2 Cập nhật ma trận

```
PUT /exam-matrices/{matrixId}
```

Body giống `POST /exam-matrices`. Chỉ cập nhật được khi `status = DRAFT`.

**Response:** `ExamMatrixResponse`

---

### 1.3 Lấy chi tiết ma trận

```
GET /exam-matrices/{matrixId}
```

**Response:** `ExamMatrixResponse`

---

### 1.4 Lấy ma trận theo assessment

```
GET /exam-matrices/assessment/{assessmentId}
```

Lấy ma trận đang gắn với một assessment cụ thể.

**Response:** `ExamMatrixResponse`

---

### 1.5 Lấy danh sách ma trận của tôi

```
GET /exam-matrices/my
```

Trả về tất cả ma trận do giáo viên đang đăng nhập tạo.

**Response:** `List<ExamMatrixResponse>`

---

### 1.6 Xoá ma trận

```
DELETE /exam-matrices/{matrixId}
```

Soft delete. **Không xoá được** nếu `status = LOCKED`.

---

## PHẦN 2 — Build Ma Trận Hoàn Chỉnh (Luồng 1)

### 2.1 Build ma trận trong 1 request

```
POST /exam-matrices/build
```

Tạo ma trận **đầy đủ** gồm nhiều dạng bài trong một lần gọi. Đây là API chính cho FE.

**Request body:**

```json
{
  "name": "Ma trận đề Toán 12 HK1",
  "description": "Đề thi cuối kỳ 1",
  "curriculumId": "uuid-curriculum",
  "gradeLevel": 12,
  "isReusable": true,
  "totalQuestionsTarget": 50,
  "totalPointsTarget": 10.0,
  "rows": [
    {
      "chapterId": "uuid-chapter-1",
      "lessonId": "uuid-lesson-1",
      "templateId": "uuid-template-1",
      "questionTypeName": "Đơn điệu của hàm số",
      "referenceQuestions": "3,30",
      "orderIndex": 1,
      "cells": [
        {
          "cognitiveLevel": "NHAN_BIET",
          "questionCount": 2,
          "pointsPerQuestion": 0.2
        },
        {
          "cognitiveLevel": "THONG_HIEU",
          "questionCount": 1,
          "pointsPerQuestion": 0.2
        }
      ]
    },
    {
      "chapterId": "uuid-chapter-1",
      "questionTypeName": "Cực trị của hàm số",
      "referenceQuestions": "4,5,39,46",
      "orderIndex": 2,
      "cells": [
        {
          "cognitiveLevel": "NHAN_BIET",
          "questionCount": 1,
          "pointsPerQuestion": 0.2
        },
        {
          "cognitiveLevel": "THONG_HIEU",
          "questionCount": 1,
          "pointsPerQuestion": 0.2
        },
        {
          "cognitiveLevel": "VAN_DUNG",
          "questionCount": 1,
          "pointsPerQuestion": 0.25
        },
        {
          "cognitiveLevel": "VAN_DUNG_CAO",
          "questionCount": 1,
          "pointsPerQuestion": 0.5
        }
      ]
    }
  ]
}
```

| Field          | Bắt buộc | Mô tả                                                |
| -------------- | -------- | ---------------------------------------------------- |
| `name`         | ✅       | Tên ma trận                                          |
| `curriculumId` | ❌       | UUID chương trình học (auto-resolve grade + subject) |
| `gradeLevel`   | ❌       | Lớp (1–12), tự động lấy từ curriculum nếu bỏ trống   |
| `rows`         | ✅       | Danh sách các dạng bài (tối thiểu 1)                 |

**Mỗi `row`:**

| Field                | Bắt buộc | Mô tả                                                         |
| -------------------- | -------- | ------------------------------------------------------------- |
| `chapterId`          | ✅       | UUID chương bài thuộc về                                      |
| `lessonId`           | ❌       | UUID bài cụ thể trong chương                                  |
| `templateId`         | ❌       | UUID template câu hỏi (nếu có thì `questionTypeName` tự điền) |
| `questionTypeName`   | ❌\*     | Tên dạng bài. **Bắt buộc** nếu không có `templateId`          |
| `referenceQuestions` | ❌       | Số thứ tự câu tham chiếu, ví dụ: `"3,30"` hoặc `"4,5,39,46"`  |
| `orderIndex`         | ❌       | Thứ tự hiển thị trong chương                                  |
| `cells`              | ✅       | Danh sách ô (mức độ nhận thức), tối thiểu 1                   |

**Mỗi `cell`:**

| Field               | Bắt buộc | Mô tả                                                    |
| ------------------- | -------- | -------------------------------------------------------- |
| `cognitiveLevel`    | ✅       | `NHAN_BIET` / `THONG_HIEU` / `VAN_DUNG` / `VAN_DUNG_CAO` |
| `questionCount`     | ✅       | Số câu hỏi (≥ 1)                                         |
| `pointsPerQuestion` | ✅       | Điểm mỗi câu (> 0)                                       |

**Response:** `ExamMatrixTableResponse` (xem mục 7.3)

---

## PHẦN 3 — Xem & Chỉnh Sửa Bảng Ma Trận

### 3.1 Lấy bảng ma trận (Table view)

```
GET /exam-matrices/{matrixId}/table
```

Trả về ma trận dạng **bảng phân cấp**: Chương → Dạng bài → Cột NB/TH/VD/VDC, kèm tổng hàng và tổng cột — đúng theo format bảng ma trận THPT Việt Nam.

**Response:** `ExamMatrixTableResponse` (xem mục 7.3)

---

### 3.2 Thêm một dạng bài (row) vào ma trận

```
POST /exam-matrices/{matrixId}/rows
```

Thêm thêm 1 row vào ma trận đang `DRAFT`. Body giống một phần tử trong mảng `rows` của `BuildExamMatrixRequest`.

**Request body:**

```json
{
  "chapterId": "uuid-chapter",
  "questionTypeName": "Giới hạn của hàm số",
  "referenceQuestions": "10,11",
  "orderIndex": 3,
  "cells": [
    {
      "cognitiveLevel": "NHAN_BIET",
      "questionCount": 2,
      "pointsPerQuestion": 0.2
    }
  ]
}
```

**Response:** `ExamMatrixTableResponse` (toàn bộ bảng sau khi thêm)

---

### 3.3 Xoá một dạng bài (row)

```
DELETE /exam-matrices/{matrixId}/rows/{rowId}
```

Xoá row và tất cả các cells của nó. Ma trận phải đang ở trạng thái `DRAFT`.

**Response:** `ExamMatrixTableResponse` (toàn bộ bảng sau khi xoá)

---

## PHẦN 4 — Template Mappings (Luồng 2 — nâng cao)

> Template Mappings là cách gắn template câu hỏi vào ma trận theo mức độ nhận thức. Phục vụ luồng sinh câu hỏi tự động sau đó.

### 4.1 Thêm một template mapping

```
POST /exam-matrices/{matrixId}/template-mappings
```

**Request body:**

```json
{
  "templateId": "uuid-template",
  "cognitiveLevel": "NHAN_BIET",
  "questionCount": 5,
  "pointsPerQuestion": 0.2
}
```

**Response:** `TemplateMappingResponse` (xem mục 7.4)

---

### 4.2 Thêm nhiều template mappings cùng lúc (batch)

```
POST /exam-matrices/{matrixId}/templates
```

**Request body:**

```json
{
  "mappings": [
    {
      "templateId": "uuid-t1",
      "cognitiveLevel": "NHAN_BIET",
      "questionCount": 5,
      "pointsPerQuestion": 0.2
    },
    {
      "templateId": "uuid-t2",
      "cognitiveLevel": "THONG_HIEU",
      "questionCount": 3,
      "pointsPerQuestion": 0.2
    },
    {
      "templateId": "uuid-t3",
      "cognitiveLevel": "VAN_DUNG",
      "questionCount": 2,
      "pointsPerQuestion": 0.25
    }
  ]
}
```

**Response:**

```json
{
  "code": 200,
  "result": {
    "addedMappings": [
      /* List<TemplateMappingResponse> */
    ]
  }
}
```

---

### 4.3 Lấy danh sách template mappings

```
GET /exam-matrices/{matrixId}/template-mappings
```

**Response:** `List<TemplateMappingResponse>`

---

### 4.4 Xoá một template mapping

```
DELETE /exam-matrices/{matrixId}/template-mappings/{mappingId}
```

Chỉ thực hiện được khi ma trận đang `DRAFT`.

---

## PHẦN 5 — Validate & Lifecycle

### 5.1 Validate ma trận

```
GET /exam-matrices/{matrixId}/validate
```

Kiểm tra ma trận trước khi approve: coverage mức độ nhận thức, tổng câu/điểm có đúng target không.

**Response:** `MatrixValidationReport`

```json
{
  "code": 200,
  "message": "Matrix validation passed. Ready to approve.",
  "result": {
    "canApprove": true,
    "errors": [],
    "warnings": ["Total points 9.5 does not match target 10.0"],
    "totalTemplateMappings": 8,
    "totalQuestions": 50,
    "totalPoints": 9.5,
    "totalQuestionsTarget": 50,
    "totalPointsTarget": 10.0,
    "cognitiveLevelCoverage": {
      "NB": 20,
      "TH": 15,
      "VD": 10,
      "VDC": 5
    },
    "questionsMatchTarget": true,
    "pointsMatchTarget": false,
    "allCognitiveLevelsCovered": true
  }
}
```

> Nếu `canApprove = true` → cho phép gọi approve.  
> Nếu `canApprove = false` → hiển thị `errors` để giáo viên sửa.

---

### 5.2 Approve ma trận

```
POST /exam-matrices/{matrixId}/approve
```

Chuyển status từ `DRAFT` → `APPROVED`. Gọi validate nội bộ trước, nếu có errors thì trả về lỗi.

**Response:** `ExamMatrixResponse` với `status = "APPROVED"`

---

### 5.3 Reset về DRAFT

```
POST /exam-matrices/{matrixId}/reset
```

Chuyển `APPROVED` → `DRAFT` để chỉnh sửa lại.

**Response:** `ExamMatrixResponse` với `status = "DRAFT"`

---

### 5.4 Khoá ma trận _(Internal — hệ thống tự gọi)_

```
POST /exam-matrices/{matrixId}/lock
```

Hệ thống tự gọi khi assessment được publish. FE không cần gọi trực tiếp.

---

## PHẦN 6 — Sinh Câu Hỏi Tự Động _(nâng cao)_

### 6.1 Tìm templates phù hợp cho ma trận

```
GET /exam-matrices/{matrixId}/matching-templates
```

| Query param  | Bắt buộc | Mô tả                                     |
| ------------ | -------- | ----------------------------------------- |
| `q`          | ❌       | Tìm kiếm theo tên template                |
| `page`       | ❌       | Trang (default: 0)                        |
| `size`       | ❌       | Số item mỗi trang (default: 20)           |
| `onlyMine`   | ❌       | Chỉ lấy template của tôi (default: false) |
| `publicOnly` | ❌       | Chỉ lấy template public (default: false)  |

**Response:** `MatchingTemplatesResponse` — danh sách templates xếp hạng theo độ phù hợp.

---

### 6.2 Xem trước câu hỏi sinh từ template _(không lưu DB)_

```
POST /exam-matrices/{matrixId}/template-mappings/{mappingId}/generate-preview
```

Sinh câu hỏi **trong bộ nhớ** để giáo viên xem trước. Không ghi gì vào DB.

**Request body:**

```json
{
  "templateId": "uuid-template",
  "count": 5,
  "difficulty": "MEDIUM",
  "seed": 42
}
```

| Field        | Bắt buộc | Mô tả                                              |
| ------------ | -------- | -------------------------------------------------- |
| `templateId` | ✅       | Template dùng để sinh câu hỏi                      |
| `count`      | ✅       | Số câu cần sinh (1–50)                             |
| `difficulty` | ❌       | `EASY` / `MEDIUM` / `HARD` — override mặc định     |
| `seed`       | ❌       | Seed để sinh giống nhau cho lần sau (reproducible) |

**Response:** `PreviewCandidatesResponse`

```json
{
  "result": {
    "templateId": "...",
    "templateName": "...",
    "requestedCount": 5,
    "generatedCount": 5,
    "candidates": [
      {
        "index": 1,
        "questionText": "Cho hàm số y = x³ - 3x. Tìm cực trị.",
        "options": {
          "A": "Cực đại tại x=1, cực tiểu tại x=-1",
          "B": "Cực đại tại x=-1, cực tiểu tại x=1",
          "C": "Không có cực trị",
          "D": "Cực tiểu tại x=0"
        },
        "correctAnswer": "B",
        "explanation": "...",
        "difficulty": "MEDIUM",
        "cognitiveLevel": "VAN_DUNG"
      }
    ],
    "warnings": []
  }
}
```

---

### 6.3 Lưu câu hỏi đã chọn vào DB

```
POST /exam-matrices/{matrixId}/template-mappings/{mappingId}/finalize
```

Giáo viên chọn câu hỏi nào từ preview để lưu vào DB. Atomic transaction.

**Request body:**

```json
{
  "templateId": "uuid-template",
  "pointsPerQuestion": 0.2,
  "replaceExisting": true,
  "questionBankId": "uuid-bank",
  "questions": [
    {
      "questionText": "Cho hàm số y = x³ - 3x. Tìm cực trị.",
      "questionType": "MULTIPLE_CHOICE",
      "options": {
        "A": "Cực đại tại x=1, cực tiểu tại x=-1",
        "B": "Cực đại tại x=-1, cực tiểu tại x=1",
        "C": "Không có cực trị",
        "D": "Cực tiểu tại x=0"
      },
      "correctAnswer": "B",
      "difficulty": "MEDIUM",
      "cognitiveLevel": "VAN_DUNG",
      "explanation": "...",
      "tags": ["hàm số", "cực trị"]
    }
  ]
}
```

| Field               | Bắt buộc | Mô tả                                                 |
| ------------------- | -------- | ----------------------------------------------------- |
| `templateId`        | ✅       | Template đã dùng để sinh (traceability)               |
| `pointsPerQuestion` | ✅       | Điểm mỗi câu                                          |
| `replaceExisting`   | ✅       | `true`: xoá câu cũ rồi thêm mới; `false`: append thêm |
| `questionBankId`    | ❌       | Gắn câu hỏi vào question bank                         |
| `questions`         | ✅       | Danh sách câu hỏi cần lưu (lấy từ preview)            |

**Response:** `FinalizePreviewResponse`

```json
{
  "result": {
    "templateMappingId": "...",
    "matrixId": "...",
    "templateId": "...",
    "requestedCount": 5,
    "savedCount": 5,
    "questionIds": ["uuid1", "uuid2", "..."],
    "currentMappingQuestionCount": 5,
    "mappingTargetCount": 5,
    "warnings": []
  }
}
```

---

## PHẦN 7 — Export PDF

### 7.1 Tải ma trận dạng PDF

```
GET /exam-matrices/{matrixId}/export-pdf
```

Trả về file PDF A4 ngang — đúng layout bảng ma trận THPT Việt Nam.

**Response:** `application/pdf` — file stream, browser tự download.

> FE dùng: `window.open('/exam-matrices/{id}/export-pdf')` hoặc tạo thẻ `<a href="...">`.

---

## PHẦN 8 — Cấu Trúc Response

### 8.1 Wrapper chung

Tất cả API đều trả về:

```json
{
  "code": 200,
  "message": "...",
  "result": { ... }
}
```

Lỗi:

```json
{
  "code": 400,
  "message": "Mô tả lỗi"
}
```

### 8.2 `ExamMatrixResponse`

```json
{
  "id": "uuid",
  "teacherId": "uuid",
  "teacherName": "Nguyễn Văn A",
  "name": "Ma trận đề Toán 12 HK1",
  "description": "...",
  "isReusable": true,
  "totalQuestionsTarget": 50,
  "totalPointsTarget": 10.0,
  "status": "DRAFT",
  "templateMappingCount": 8,
  "templateMappings": [
    /* List<TemplateMappingResponse> */
  ],
  "createdAt": "2026-03-17T10:00:00Z",
  "updatedAt": "2026-03-17T10:00:00Z"
}
```

### 8.3 `ExamMatrixTableResponse` — Bảng ma trận đầy đủ

```json
{
  "id": "uuid",
  "name": "Ma trận đề Toán 12 HK1",
  "teacherName": "Nguyễn Văn A",
  "gradeLevel": 12,
  "curriculumName": "THPT 2018",
  "subjectName": "Toán",
  "status": "DRAFT",
  "chapters": [
    {
      "chapterId": "uuid",
      "chapterTitle": "Chương 1: Hàm số và đồ thị",
      "chapterOrderIndex": 1,
      "rows": [
        {
          "rowId": "uuid",
          "questionTypeName": "Đơn điệu của hàm số",
          "referenceQuestions": "3,30",
          "orderIndex": 1,
          "cells": [
            {
              "mappingId": "uuid",
              "cognitiveLevel": "NHAN_BIET",
              "cognitiveLevelLabel": "NB",
              "questionCount": 2,
              "pointsPerQuestion": 0.2,
              "totalPoints": 0.4
            }
          ],
          "countByCognitive": { "NB": 2, "TH": 1 },
          "rowTotalQuestions": 3,
          "rowTotalPoints": 0.6
        }
      ],
      "totalByCognitive": { "NB": 5, "TH": 3, "VD": 2 },
      "chapterTotalQuestions": 10,
      "chapterTotalPoints": 2.0
    }
  ],
  "grandTotalByCognitive": { "NB": 20, "TH": 15, "VD": 10, "VDC": 5 },
  "grandTotalQuestions": 50,
  "grandTotalPoints": 10.0,
  "totalQuestionsTarget": 50,
  "totalPointsTarget": 10.0
}
```

### 8.4 `TemplateMappingResponse`

```json
{
  "id": "uuid",
  "examMatrixId": "uuid",
  "templateId": "uuid",
  "templateName": "Template cực trị hàm số",
  "cognitiveLevel": "VAN_DUNG",
  "questionCount": 3,
  "pointsPerQuestion": 0.25,
  "totalPoints": 0.75,
  "createdAt": "...",
  "updatedAt": "..."
}
```

---

## PHẦN 9 — Tóm Tắt Nhanh (Quick Reference)

| Method     | Endpoint                                                             | Mô tả                                    |
| ---------- | -------------------------------------------------------------------- | ---------------------------------------- |
| `POST`     | `/exam-matrices`                                                     | Tạo ma trận rỗng                         |
| `PUT`      | `/exam-matrices/{id}`                                                | Cập nhật ma trận                         |
| `GET`      | `/exam-matrices/{id}`                                                | Chi tiết ma trận                         |
| `GET`      | `/exam-matrices/my`                                                  | Danh sách ma trận của tôi                |
| `GET`      | `/exam-matrices/assessment/{assessmentId}`                           | Ma trận theo assessment                  |
| `DELETE`   | `/exam-matrices/{id}`                                                | Xoá ma trận                              |
| **`POST`** | **`/exam-matrices/build`**                                           | **Build ma trận hoàn chỉnh (1 request)** |
| `GET`      | `/exam-matrices/{id}/table`                                          | Lấy bảng ma trận dạng bảng               |
| `POST`     | `/exam-matrices/{id}/rows`                                           | Thêm một dạng bài                        |
| `DELETE`   | `/exam-matrices/{id}/rows/{rowId}`                                   | Xoá một dạng bài                         |
| `GET`      | `/exam-matrices/{id}/validate`                                       | Kiểm tra ma trận                         |
| `POST`     | `/exam-matrices/{id}/approve`                                        | Duyệt ma trận                            |
| `POST`     | `/exam-matrices/{id}/reset`                                          | Reset về DRAFT                           |
| `POST`     | `/exam-matrices/{id}/template-mappings`                              | Thêm template mapping                    |
| `POST`     | `/exam-matrices/{id}/templates`                                      | Batch thêm template mappings             |
| `GET`      | `/exam-matrices/{id}/template-mappings`                              | Lấy danh sách mappings                   |
| `DELETE`   | `/exam-matrices/{id}/template-mappings/{mappingId}`                  | Xoá mapping                              |
| `GET`      | `/exam-matrices/{id}/matching-templates`                             | Tìm template phù hợp                     |
| `POST`     | `/exam-matrices/{id}/template-mappings/{mappingId}/generate-preview` | Xem trước câu hỏi                        |
| `POST`     | `/exam-matrices/{id}/template-mappings/{mappingId}/finalize`         | Lưu câu hỏi vào DB                       |
| `GET`      | `/exam-matrices/{id}/export-pdf`                                     | Tải PDF                                  |

---

_Last updated: March 17, 2026_

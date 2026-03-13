# Assignment / Exam Matrix Design — README

> **File:** `README_ASSIGNMENT_MATRIX.md`  
> **Project:** Math Master Teaching Support Platform (Backend)

---

## 1. Tổng quan (Overview)

Ma trận đề thi (Exam Matrix) là công cụ cho phép giáo viên thiết kế cấu trúc đề kiểm tra theo:

- **Lớp** (School grade: 10, 11, 12)
- **Môn** (Math subject: Đại Số, Hình Học, Giải Tích, Tổ Hợp – Xác Suất…)
- **Chương** (Chapter)
- **Dạng bài** (Question type / topic)
- **Mức độ nhận thức** (Cognitive level: NB / TH / VD / VDC)

Kết quả là một bảng ma trận tương tự như mẫu đề minh họa THPT Quốc Gia:

```
Lớp | Chương              | Dạng bài            | Trích dẫn | NB | TH | VD | VDC | Tổng bài | Tổng chương
----+---------------------+---------------------+-----------+----+----+----+-----+----------+------------
 12 | Đạo hàm và ứng dụng | Đơn điệu của HS     | 3,30      |  1 |  1 |    |     |     2    |
    |                     | Cực trị của HS      | 4,5,39,46 |  1 |  1 |  1 |  1  |     4    |    10
    |                     | ...                 |           |    |    |    |     |          |
----+---------------------+---------------------+-----------+----+----+----+-----+----------+------------
    | TỔNG                |                     |           | 20 | 15 | 10 |  5  |    50    |
```

---

## 2. Entity Hierarchy

```
 GradeSubject
 ┌──────────────────────────┐
 │ grade_level (10/11/12)   │   N                            N
 │ subject_id  ─────────────┼───────── Subject (môn học: Đại Số, Hình Học…)
 └──────────────────────────┘               │ 1
                                            │
                                            │ has many (1:N)
                                            ▼
                                    Curriculum (chương trình)
                                    e.g. "Toán 12 – Đại Số"       │ 1
                                            │
                                            │ has many (1:N)
                                            ▼
                                       Chapter (chương)            │ 1
                                            │
                                            │ has many (1:N)
                                            ▼
                                         Lesson (bài học)          │ 1
                                            │
                                            │ has many (1:N)
                                            ▼
                                    QuestionTemplate (dạng bài)
```

> **Ghi chú cardinality:**
>
> - 1 `Subject` có nhiều `Curriculum` (mỗi lớp/nhu cầu khác nhau)
> - 1 `Curriculum` có nhiều `Chapter`
> - 1 `Chapter` có nhiều `Lesson`
> - 1 `Lesson` có nhiều `QuestionTemplate`

```
                          Teacher
                             │
                             ▼
                        ExamMatrix ──── Curriculum (context, optional)
                             │
                    ┌────────┴────────┐
                    ▼                 ▼
              ExamMatrixRow     ExamMatrixTemplateMapping
              (dạng bài row)    (cell: template × cognitiveLevel × count)
                    │                 │
                    └────── matrixRowId ────┘
```

---

## 3. Entities mới / thay đổi

### 3.1 `Subject` _(NEW)_

Thay thế `CurriculumCategory` enum cho các trường hợp cần thông tin đầy đủ về môn học.

| Cột           | Kiểu         | Mô tả                                             |
| ------------- | ------------ | ------------------------------------------------- |
| `id`          | UUID         | Primary key                                       |
| `name`        | VARCHAR(255) | Tên môn, ví dụ "Đại Số", "Hình Học", "Giải Tích"  |
| `code`        | VARCHAR(50)  | Mã định danh duy nhất, ví dụ `DAI_SO`, `HINH_HOC` |
| `description` | TEXT         | Mô tả môn                                         |
| `grade_min`   | INT          | Lớp thấp nhất môn này xuất hiện (nullable)        |
| `grade_max`   | INT          | Lớp cao nhất môn này xuất hiện (nullable)         |
| `is_active`   | BOOLEAN      | Soft-delete flag                                  |
| `created_at`  | TIMESTAMP    |                                                   |
| `updated_at`  | TIMESTAMP    |                                                   |

**Quan hệ:** `Subject` 1 → N `Curriculum`, `Subject` N–N `SchoolGrade` via `GradeSubject`.

---

### 3.2 `GradeSubject` _(NEW)_

Bảng junction cho quan hệ N-N giữa **lớp học** và **môn học**.

| Cột           | Kiểu      | Mô tả                   |
| ------------- | --------- | ----------------------- |
| `id`          | UUID      | Primary key             |
| `grade_level` | INT       | Lớp học (10, 11, 12)    |
| `subject_id`  | UUID FK   | Liên kết đến `subjects` |
| `is_active`   | BOOLEAN   | Soft-delete flag        |
| `created_at`  | TIMESTAMP |                         |

**Ý nghĩa:** Ví dụ, grade=12 được liên kết với Subject `DAI_SO`, `HINH_HOC`, `GIAI_TICH` — cho phép UI hiển thị "Lớp 12 học các môn: Đại Số, Hình Học, Giải Tích".

---

### 3.3 `Curriculum` _(UPDATED)_

Thêm trường `subject_id` (nullable) để liên kết với `Subject` entity mới.

```
Curriculum.category (CurriculumCategory enum) — vẫn giữ để backward compat
Curriculum.subjectId (UUID FK → subjects)     — preferred going forward
```

---

### 3.4 `ExamMatrix` _(UPDATED)_

| Trường thêm mới | Kiểu    | Mô tả                                   |
| --------------- | ------- | --------------------------------------- |
| `curriculum_id` | UUID FK | Chương trình ma trận dựa vào (optional) |
| `grade_level`   | INT     | Cache lớp mục tiêu (ví dụ 12)           |

---

### 3.5 `ExamMatrixRow` _(NEW)_

Mỗi **hàng** trong bảng ma trận đề thi — tương ứng với một **dạng bài**.

| Cột                   | Kiểu         | Mô tả                                        |
| --------------------- | ------------ | -------------------------------------------- |
| `id`                  | UUID         | Primary key                                  |
| `exam_matrix_id`      | UUID FK      | Ma trận chứa hàng này                        |
| `chapter_id`          | UUID FK      | Chương (chongson)                            |
| `lesson_id`           | UUID FK      | Bài học (optional, thu hẹp xuống bài cụ thể) |
| `template_id`         | UUID FK      | QuestionTemplate (dạng bài, optional)        |
| `question_type_name`  | VARCHAR(500) | Tên hiển thị "Đơn điệu của HS"               |
| `reference_questions` | VARCHAR(255) | Số đề tham chiếu "3,30" (cột Trích dẫn)      |
| `order_index`         | INT          | Thứ tự trong nhóm chương                     |
| `created_at`          | TIMESTAMP    |                                              |
| `updated_at`          | TIMESTAMP    |                                              |

---

### 3.6 `ExamMatrixTemplateMapping` _(UPDATED)_

Thêm trường `matrix_row_id` (UUID FK → `exam_matrix_rows`) để liên kết cell với row.

---

### 3.7 `CognitiveLevel` enum _(UPDATED)_

Thêm 4 mức độ chuẩn THPT Việt Nam:

| Enum value     | Label | Ý nghĩa      |
| -------------- | ----- | ------------ |
| `NHAN_BIET`    | NB    | Nhận Biết    |
| `THONG_HIEU`   | TH    | Thông Hiểu   |
| `VAN_DUNG`     | VD    | Vận Dụng     |
| `VAN_DUNG_CAO` | VDC   | Vận Dụng Cao |

> Các giá trị Bloom cũ (`REMEMBER`, `UNDERSTAND`, `APPLY`, `ANALYZE`, `EVALUATE`, `CREATE`) vẫn được giữ để backward-compat.

---

## 4. API Reference

### 4.1 Subject (Môn học) — `/subjects`

| Method   | Endpoint                               | Auth   | Mô tả                            |
| -------- | -------------------------------------- | ------ | -------------------------------- |
| `POST`   | `/subjects`                            | ADMIN  | Tạo môn mới                      |
| `GET`    | `/subjects`                            | Public | Danh sách tất cả môn đang active |
| `GET`    | `/subjects/{subjectId}`                | Public | Chi tiết một môn                 |
| `GET`    | `/subjects/grade/{gradeLevel}`         | Public | Các môn thuộc lớp (10/11/12)     |
| `POST`   | `/subjects/{subjectId}/grades`         | ADMIN  | Liên kết môn với lớp (N-N)       |
| `DELETE` | `/subjects/{subjectId}/grades/{grade}` | ADMIN  | Bỏ liên kết môn-lớp              |
| `DELETE` | `/subjects/{subjectId}`                | ADMIN  | Deactivate môn (soft delete)     |

**Tạo môn mới:**

```json
POST /subjects
{
  "name": "Đại Số",
  "code": "DAI_SO",
  "description": "Môn Đại Số – Toán THPT",
  "gradeMin": 10,
  "gradeMax": 12
}
```

**Liên kết môn với lớp:**

```json
POST /subjects/{subjectId}/grades
{ "gradeLevel": 12 }
```

---

### 4.2 Structured Matrix Builder — `/exam-matrices/build`

#### `POST /exam-matrices/build`

Tạo ma trận đề thi hoàn chỉnh trong **một request duy nhất**.

**Request Body:**

```json
{
  "name": "Ma Trận Đề Minh Họa THPT 2024",
  "description": "Ma trận đề thi thử THPT Quốc Gia môn Toán lớp 12",
  "curriculumId": "uuid-of-curriculum",
  "gradeLevel": 12,
  "isReusable": true,
  "totalQuestionsTarget": 50,
  "totalPointsTarget": 10.0,
  "rows": [
    {
      "chapterId": "uuid-chuong-dao-ham",
      "templateId": "uuid-template-don-dieu",
      "questionTypeName": "Đơn điệu của HS",
      "referenceQuestions": "3,30",
      "orderIndex": 1,
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
        }
      ]
    },
    {
      "chapterId": "uuid-chuong-dao-ham",
      "templateId": "uuid-template-cuc-tri",
      "questionTypeName": "Cực trị của HS",
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
          "pointsPerQuestion": 0.2
        },
        {
          "cognitiveLevel": "VAN_DUNG_CAO",
          "questionCount": 1,
          "pointsPerQuestion": 0.2
        }
      ]
    }
  ]
}
```

**Response:** `ExamMatrixTableResponse` — xem mục 4.4.

---

#### `GET /exam-matrices/{matrixId}/table`

Lấy ma trận theo dạng bảng phân cấp (Chương → Dạng bài → Ô NB/TH/VD/VDC).

---

#### `POST /exam-matrices/{matrixId}/rows`

Thêm một dòng (dạng bài) vào ma trận đang ở trạng thái DRAFT.

```json
{
  "chapterId": "uuid-chapter",
  "templateId": "uuid-template",
  "questionTypeName": "PT Mũ – Logarit",
  "referenceQuestions": "12,13,47",
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
      "cognitiveLevel": "VAN_DUNG_CAO",
      "questionCount": 1,
      "pointsPerQuestion": 0.2
    }
  ]
}
```

---

#### `DELETE /exam-matrices/{matrixId}/rows/{rowId}`

Xoá một dòng và tất cả ô của nó khỏi ma trận DRAFT.

---

### 4.3 ExamMatrixTableResponse (Structure)

```
ExamMatrixTableResponse
├── id, name, description
├── teacherId, teacherName
├── gradeLevel           ← Lớp (12, 11…)
├── curriculumId, curriculumName
├── subjectId, subjectName  ← Môn (Đại Số, Hình Học…)
├── isReusable, status
├── chapters: List<MatrixChapterGroupResponse>
│   ├── chapterId, chapterTitle, chapterOrderIndex
│   ├── totalByCognitive: { NB: 4, TH: 3, VD: 2, VDC: 1 }
│   ├── chapterTotalQuestions: 10
│   ├── chapterTotalPoints: 2.0
│   └── rows: List<MatrixRowResponse>
│       ├── rowId, chapterId, lessonId, templateId
│       ├── questionTypeName: "Đơn điệu của HS"
│       ├── referenceQuestions: "3,30"
│       ├── countByCognitive: { NB: 1, TH: 1 }
│       ├── rowTotalQuestions: 2
│       ├── rowTotalPoints: 0.4
│       └── cells: List<MatrixCellResponse>
│           ├── mappingId, cognitiveLevel, cognitiveLevelLabel
│           ├── questionCount, pointsPerQuestion, totalPoints
├── grandTotalByCognitive: { NB: 20, TH: 15, VD: 10, VDC: 5 }
├── grandTotalQuestions: 50
└── grandTotalPoints: 10.0
```

---

## 5. Quy trình thiết kế ma trận (Workflow)

```
[1] Admin tạo Subject(s) và liên kết với lớp
         POST /subjects  +  POST /subjects/{id}/grades

[2] Admin/Teacher tạo Curriculum cho từng lớp-môn
         POST /curricula  (với subjectId)

[3] Admin/Teacher tạo Chapter và Lesson trong Curriculum
         POST /curricula/{id}/chapters
         POST /chapters/{id}/lessons

[4] Teacher tạo QuestionTemplate (dạng bài) trong Lesson
         POST /question-templates

[5] Teacher thiết kế ma trận đề thi
    OPTION A — Structured Builder (recommended):
         POST /exam-matrices/build
         → Tạo toàn bộ matrix + rows + cells trong 1 request

    OPTION B — Incremental:
         POST /exam-matrices          (tạo matrix trống)
         POST /exam-matrices/{id}/rows (thêm từng dạng bài)
         hoặc
         POST /exam-matrices/{id}/template-mappings

[6] Teacher xem bảng ma trận
         GET /exam-matrices/{id}/table

[7] Validate & Approve
         GET /exam-matrices/{id}/validate
         POST /exam-matrices/{id}/approve

[8] Gắn matrix vào Assessment (đề thi)
         POST /assessments  (với examMatrixId)

[9] Generate câu hỏi cho từng ô
         POST /exam-matrices/{id}/mappings/{mappingId}/preview
         POST /exam-matrices/{id}/mappings/{mappingId}/finalize
```

---

## 6. Ví dụ đầy đủ — Ma trận như hình

Dựa vào hình ảnh đề minh họa THPT (50 câu, 10 điểm):

| Lớp | Chương              | Dạng bài              | NB  | TH  | VD  | VDC | Tổng |
| --- | ------------------- | --------------------- | --- | --- | --- | --- | ---- |
| 12  | Đạo Hàm và Ứng Dụng | Đơn điệu của HS       | 1   | 1   |     |     | 2    |
| 12  |                     | Cực trị của HS        | 1   | 1   | 1   | 1   | 4    |
| 12  |                     | Min, Max của hàm số   |     |     | 1   |     | 1    |
| 12  |                     | Đường Tiệm Cận        | 1   |     |     |     | 1    |
| 12  |                     | Khảo sát và vẽ đồ thị | 1   | 1   |     |     | 2    |
| 12  | Hàm Số Mũ – Logarit | Lũy thừa – Mũ – Log   | 1   | 1   |     |     | 2    |
| ... | ...                 | ...                   | ..  | ..  | ..  | ..  | ...  |
| 11  | Tổ Hợp – Xác Suất   | Hoán vị – Chỉnh hợp   | 1   |     |     |     | 1    |
| 11  |                     | Cấp số cộng/nhân      | 1   |     |     |     | 1    |
| 11  |                     | Xác suất              |     | 1   |     |     | 1    |
|     | **Tổng**            |                       | 20  | 15  | 10  | 5   | 50   |

---

## 7. Database Migration Notes

Khi thêm các entity mới, bạn cần thêm vào migration SQL (Flyway/Liquibase):

```sql
-- subjects table
CREATE TABLE subjects (
  id           UUID         NOT NULL,
  name         NVARCHAR(255) NOT NULL,
  code         VARCHAR(50)  NOT NULL,
  description  NTEXT,
  grade_min    INT,
  grade_max    INT,
  is_active    BIT          NOT NULL DEFAULT 1,
  created_at   DATETIME2,
  updated_at   DATETIME2,
  CONSTRAINT pk_subjects PRIMARY KEY (id),
  CONSTRAINT uq_subjects_code UNIQUE (code)
);

-- grade_subjects (N-N)
CREATE TABLE grade_subjects (
  id           UUID      NOT NULL,
  grade_level  INT       NOT NULL,
  subject_id   UUID      NOT NULL,
  is_active    BIT       NOT NULL DEFAULT 1,
  created_at   DATETIME2,
  CONSTRAINT pk_grade_subjects PRIMARY KEY (id),
  CONSTRAINT uq_grade_subjects UNIQUE (grade_level, subject_id),
  CONSTRAINT fk_gs_subject FOREIGN KEY (subject_id) REFERENCES subjects(id)
);

-- Alter curricula: add subject_id
ALTER TABLE curricula ADD subject_id UUID NULL;
ALTER TABLE curricula ADD CONSTRAINT fk_curricula_subject
  FOREIGN KEY (subject_id) REFERENCES subjects(id);
CREATE INDEX idx_curricula_subject ON curricula(subject_id);

-- Alter exam_matrices: add curriculum_id, grade_level
ALTER TABLE exam_matrices ADD curriculum_id UUID NULL;
ALTER TABLE exam_matrices ADD grade_level    INT  NULL;
ALTER TABLE exam_matrices ADD CONSTRAINT fk_em_curriculum
  FOREIGN KEY (curriculum_id) REFERENCES curricula(id);
CREATE INDEX idx_exam_matrices_curriculum ON exam_matrices(curriculum_id);
CREATE INDEX idx_exam_matrices_grade      ON exam_matrices(grade_level);

-- exam_matrix_rows
CREATE TABLE exam_matrix_rows (
  id                   UUID          NOT NULL,
  exam_matrix_id       UUID          NOT NULL,
  chapter_id           UUID,
  lesson_id            UUID,
  template_id          UUID,
  question_type_name   NVARCHAR(500),
  reference_questions  VARCHAR(255),
  order_index          INT,
  created_at           DATETIME2,
  updated_at           DATETIME2,
  CONSTRAINT pk_exam_matrix_rows PRIMARY KEY (id),
  CONSTRAINT fk_emr_matrix  FOREIGN KEY (exam_matrix_id) REFERENCES exam_matrices(id),
  CONSTRAINT fk_emr_chapter FOREIGN KEY (chapter_id)     REFERENCES chapters(id),
  CONSTRAINT fk_emr_lesson  FOREIGN KEY (lesson_id)      REFERENCES lessons(id)
);
CREATE INDEX idx_emr_matrix  ON exam_matrix_rows(exam_matrix_id);
CREATE INDEX idx_emr_chapter ON exam_matrix_rows(chapter_id);
CREATE INDEX idx_emr_order   ON exam_matrix_rows(order_index);

-- Alter exam_matrix_template_mappings: add matrix_row_id
ALTER TABLE exam_matrix_template_mappings ADD matrix_row_id UUID NULL;
ALTER TABLE exam_matrix_template_mappings ADD CONSTRAINT fk_emtm_row
  FOREIGN KEY (matrix_row_id) REFERENCES exam_matrix_rows(id);
CREATE INDEX idx_exam_matrix_template_row ON exam_matrix_template_mappings(matrix_row_id);
```

---

## 8. Seeding dữ liệu mẫu

Ví dụ seed data cho môn học phổ biến THPT:

```sql
INSERT INTO subjects (id, name, code, grade_min, grade_max, is_active)
VALUES
  (gen_random_uuid(), N'Đại Số',              'DAI_SO',          10, 12, 1),
  (gen_random_uuid(), N'Hình Học',             'HINH_HOC',        10, 12, 1),
  (gen_random_uuid(), N'Giải Tích',            'GIAI_TICH',       11, 12, 1),
  (gen_random_uuid(), N'Tổ Hợp - Xác Suất',   'TO_HOP_XAC_SUAT', 11, 12, 1);
```

---

## 9. Tóm tắt thay đổi

| File                                           | Loại thay đổi | Mô tả                                         |
| ---------------------------------------------- | ------------- | --------------------------------------------- |
| `entity/Subject.java`                          | **NEW**       | Môn học entity                                |
| `entity/GradeSubject.java`                     | **NEW**       | N-N Grade ↔ Subject junction                  |
| `entity/ExamMatrixRow.java`                    | **NEW**       | Dạng-bài row trong ma trận                    |
| `entity/Curriculum.java`                       | **UPDATED**   | Thêm `subjectId` FK                           |
| `entity/ExamMatrix.java`                       | **UPDATED**   | Thêm `curriculumId`, `gradeLevel`             |
| `entity/ExamMatrixTemplateMapping.java`        | **UPDATED**   | Thêm `matrixRowId` FK                         |
| `enums/CognitiveLevel.java`                    | **UPDATED**   | Thêm NB/TH/VD/VDC (THPT standard)             |
| `exception/ErrorCode.java`                     | **UPDATED**   | Thêm error codes cho Subject, row             |
| `repository/SubjectRepository.java`            | **NEW**       | JPA repo cho Subject                          |
| `repository/GradeSubjectRepository.java`       | **NEW**       | JPA repo cho GradeSubject                     |
| `repository/ExamMatrixRowRepository.java`      | **NEW**       | JPA repo cho ExamMatrixRow                    |
| `dto/request/CreateSubjectRequest.java`        | **NEW**       | Request tạo Subject                           |
| `dto/request/LinkGradeSubjectRequest.java`     | **NEW**       | Request liên kết Grade-Subject                |
| `dto/request/BuildExamMatrixRequest.java`      | **NEW**       | Structured matrix builder request             |
| `dto/request/MatrixRowRequest.java`            | **NEW**       | Một hàng trong ma trận                        |
| `dto/request/MatrixCellRequest.java`           | **NEW**       | Một ô trong hàng                              |
| `dto/response/SubjectResponse.java`            | **NEW**       | Response cho Subject                          |
| `dto/response/MatrixCellResponse.java`         | **NEW**       | Response cho ô ma trận                        |
| `dto/response/MatrixRowResponse.java`          | **NEW**       | Response cho hàng ma trận                     |
| `dto/response/MatrixChapterGroupResponse.java` | **NEW**       | Response nhóm theo chương                     |
| `dto/response/ExamMatrixTableResponse.java`    | **NEW**       | Full hierarchical table response              |
| `service/SubjectService.java`                  | **NEW**       | Interface service Subject                     |
| `service/impl/SubjectServiceImpl.java`         | **NEW**       | Implementation SubjectService                 |
| `service/ExamMatrixService.java`               | **UPDATED**   | Thêm `buildMatrix`, `getMatrixTable`, row ops |
| `service/impl/ExamMatrixServiceImpl.java`      | **UPDATED**   | Implement new matrix methods                  |
| `controller/SubjectController.java`            | **NEW**       | REST endpoints cho Subject                    |
| `controller/ExamMatrixController.java`         | **UPDATED**   | Thêm `/build`, `/table`, `/rows` endpoints    |

---

## 10. Ví dụ đầu cuối — Tạo ma trận hoàn chỉnh

> **Kịch bản:** Giáo viên muốn tạo ma trận đề kiểm tra **Toán lớp 12 – Đại Số**, gồm **10 câu trắc nghiệm, 2 điểm** (mỗi câu 0.2đ), lấy từ 2 chương: _Đạo Hàm_ (7 câu) và _Hàm Số Mũ-Logarit_ (3 câu).

---

### Bước 0 — Chuẩn bị: lấy các UUID cần thiết

Trước khi gọi `/build`, bạn cần có UUID của:

| Thứ gì                   | Lấy từ đâu                                     |
| ------------------------ | ---------------------------------------------- |
| `curriculumId`           | `GET /curricula` → chọn "Toán 12 – Đại Số"     |
| `chapterId` (Đạo Hàm)    | `GET /curricula/{id}/chapters`                 |
| `chapterId` (Hàm Mũ-Log) | tương tự                                       |
| `templateId`             | `GET /question-templates` (nếu đã có template) |

> Nếu chưa có Curriculum, tạo trước bằng `POST /curricula`.  
> `templateId` là **optional** — có thể bỏ qua và chỉ dùng `questionTypeName`.

---

### Bước 1 — (Một lần) Tạo Subject + liên kết lớp

> Bước này chỉ cần làm **một lần** cho toàn hệ thống (do ADMIN thực hiện).

**1a. Tạo môn Đại Số**

```http
POST /subjects
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "name": "Đại Số",
  "code": "DAI_SO",
  "description": "Đại Số THPT – lớp 10, 11, 12",
  "gradeMin": 10,
  "gradeMax": 12
}
```

Response:

```json
{
  "code": 1000,
  "result": {
    "id": "01950000-0000-7000-8000-000000000001",
    "name": "Đại Số",
    "code": "DAI_SO",
    "gradeMin": 10,
    "gradeMax": 12,
    "isActive": true
  }
}
```

**1b. Liên kết Đại Số với lớp 12**

```http
POST /subjects/01950000-0000-7000-8000-000000000001/grades
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "gradeLevel": 12
}
```

---

### Bước 2 — Tạo ma trận hoàn chỉnh trong 1 request

> Đây là bước chính. Giáo viên cần `curriculumId` và các `chapterId` đã tồn tại trong DB.

```http
POST /exam-matrices/build
Authorization: Bearer <teacher-token>
Content-Type: application/json

{
  "name": "Ma Trận Kiểm Tra 15 Phút – Đại Số 12",
  "description": "Kiểm tra chương Đạo Hàm và Hàm Mũ-Logarit, 10 câu trắc nghiệm",
  "curriculumId": "01950000-0000-7000-8000-aaaaaaaaaaaa",
  "gradeLevel": 12,
  "isReusable": false,
  "totalQuestionsTarget": 10,
  "totalPointsTarget": 2.0,
  "rows": [

    {
      "chapterId": "01950000-0000-7000-8000-bbbbbbbbbb01",
      "questionTypeName": "Đơn điệu của hàm số",
      "referenceQuestions": "3,30",
      "orderIndex": 1,
      "cells": [
        { "cognitiveLevel": "NHAN_BIET",  "questionCount": 1, "pointsPerQuestion": 0.2 },
        { "cognitiveLevel": "THONG_HIEU", "questionCount": 1, "pointsPerQuestion": 0.2 }
      ]
    },

    {
      "chapterId": "01950000-0000-7000-8000-bbbbbbbbbb01",
      "questionTypeName": "Cực trị của hàm số",
      "referenceQuestions": "4,5,39,46",
      "orderIndex": 2,
      "cells": [
        { "cognitiveLevel": "NHAN_BIET",    "questionCount": 1, "pointsPerQuestion": 0.2 },
        { "cognitiveLevel": "THONG_HIEU",   "questionCount": 1, "pointsPerQuestion": 0.2 },
        { "cognitiveLevel": "VAN_DUNG",     "questionCount": 1, "pointsPerQuestion": 0.2 },
        { "cognitiveLevel": "VAN_DUNG_CAO", "questionCount": 1, "pointsPerQuestion": 0.2 }
      ]
    },

    {
      "chapterId": "01950000-0000-7000-8000-bbbbbbbbbb01",
      "questionTypeName": "Giá trị lớn nhất / nhỏ nhất của hàm số",
      "referenceQuestions": "12",
      "orderIndex": 3,
      "cells": [
        { "cognitiveLevel": "VAN_DUNG", "questionCount": 1, "pointsPerQuestion": 0.2 }
      ]
    },

    {
      "chapterId": "01950000-0000-7000-8000-cccccccccc02",
      "questionTypeName": "Lũy thừa – Mũ – Logarit (cơ bản)",
      "referenceQuestions": "21,22",
      "orderIndex": 1,
      "cells": [
        { "cognitiveLevel": "NHAN_BIET",  "questionCount": 1, "pointsPerQuestion": 0.2 },
        { "cognitiveLevel": "THONG_HIEU", "questionCount": 1, "pointsPerQuestion": 0.2 }
      ]
    },

    {
      "chapterId": "01950000-0000-7000-8000-cccccccccc02",
      "questionTypeName": "Phương trình Mũ – Logarit",
      "referenceQuestions": "47",
      "orderIndex": 2,
      "cells": [
        { "cognitiveLevel": "VAN_DUNG", "questionCount": 1, "pointsPerQuestion": 0.2 }
      ]
    }

  ]
}
```

---

### Bước 3 — Xem lại bảng ma trận

```http
GET /exam-matrices/{matrixId}/table
Authorization: Bearer <teacher-token>
```

Response thu gọn:

```json
{
  "code": 1000,
  "result": {
    "id": "01950000-0000-7000-8000-eeeeeeeeeeee",
    "name": "Ma Trận Kiểm Tra 15 Phút – Đại Số 12",
    "gradeLevel": 12,
    "chapters": [
      {
        "chapterId": "01950000-0000-7000-8000-bbbbbbbbbb01",
        "chapterTitle": "Đạo Hàm và Ứng Dụng",
        "chapterOrderIndex": 1,
        "rows": [
          {
            "questionTypeName": "Đơn điệu của hàm số",
            "referenceQuestions": "3,30",
            "countByCognitive": { "NB": 1, "TH": 1 },
            "rowTotalQuestions": 2,
            "rowTotalPoints": 0.4
          },
          {
            "questionTypeName": "Cực trị của hàm số",
            "referenceQuestions": "4,5,39,46",
            "countByCognitive": { "NB": 1, "TH": 1, "VD": 1, "VDC": 1 },
            "rowTotalQuestions": 4,
            "rowTotalPoints": 0.8
          },
          {
            "questionTypeName": "Giá trị lớn nhất / nhỏ nhất của hàm số",
            "referenceQuestions": "12",
            "countByCognitive": { "VD": 1 },
            "rowTotalQuestions": 1,
            "rowTotalPoints": 0.2
          }
        ],
        "totalByCognitive": { "NB": 2, "TH": 2, "VD": 2, "VDC": 1 },
        "chapterTotalQuestions": 7,
        "chapterTotalPoints": 1.4
      },
      {
        "chapterId": "01950000-0000-7000-8000-cccccccccc02",
        "chapterTitle": "Hàm Số Mũ và Logarit",
        "rows": [
          {
            "questionTypeName": "Lũy thừa – Mũ – Logarit (cơ bản)",
            "referenceQuestions": "21,22",
            "countByCognitive": { "NB": 1, "TH": 1 },
            "rowTotalQuestions": 2,
            "rowTotalPoints": 0.4
          },
          {
            "questionTypeName": "Phương trình Mũ – Logarit",
            "referenceQuestions": "47",
            "countByCognitive": { "VD": 1 },
            "rowTotalQuestions": 1,
            "rowTotalPoints": 0.2
          }
        ],
        "totalByCognitive": { "NB": 1, "TH": 1, "VD": 1 },
        "chapterTotalQuestions": 3,
        "chapterTotalPoints": 0.6
      }
    ],
    "grandTotalByCognitive": { "NB": 3, "TH": 3, "VD": 3, "VDC": 1 },
    "grandTotalQuestions": 10,
    "grandTotalPoints": 2.0
  }
}
```

---

### Kết quả dạng bảng (tương ứng hình ảnh)

| Chương               | Dạng bài                      | Trích dẫn | NB    | TH    | VD    | VDC   | Tổng   |
| -------------------- | ----------------------------- | --------- | ----- | ----- | ----- | ----- | ------ |
| Đạo Hàm và Ứng Dụng  | Đơn điệu của hàm số           | 3, 30     | 1     | 1     |       |       | 2      |
|                      | Cực trị của hàm số            | 4,5,39,46 | 1     | 1     | 1     | 1     | 4      |
|                      | GT lớn nhất / nhỏ nhất HS     | 12        |       |       | 1     |       | 1      |
|                      | **Tổng chương**               |           | **2** | **2** | **2** | **1** | **7**  |
| Hàm Số Mũ và Logarit | Lũy thừa – Mũ – Logarit (căn) | 21, 22    | 1     | 1     |       |       | 2      |
|                      | Phương trình Mũ – Logarit     | 47        |       |       | 1     |       | 1      |
|                      | **Tổng chương**               |           | **1** | **1** | **1** | **0** | **3**  |
| **TỔNG**             |                               |           | **3** | **3** | **3** | **1** | **10** |

---

### Bước 4 (tuỳ chọn) — Thêm / xoá dòng sau khi tạo

**Thêm một dạng bài mới:**

```http
POST /exam-matrices/{matrixId}/rows
Authorization: Bearer <teacher-token>
Content-Type: application/json

{
  "chapterId": "01950000-0000-7000-8000-bbbbbbbbbb01",
  "questionTypeName": "Tiệm cận của đồ thị",
  "referenceQuestions": "8",
  "orderIndex": 4,
  "cells": [
    { "cognitiveLevel": "NHAN_BIET", "questionCount": 1, "pointsPerQuestion": 0.2 }
  ]
}
```

**Xoá một dòng:**

```http
DELETE /exam-matrices/{matrixId}/rows/{rowId}
Authorization: Bearer <teacher-token>
```

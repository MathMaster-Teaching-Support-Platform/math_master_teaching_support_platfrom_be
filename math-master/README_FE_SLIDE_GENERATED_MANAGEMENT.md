# FE Integration Guide: Generated Slide Management

Muc tieu:

- Quan ly danh sach slide da generate
- Tai lai file PPTX da generate
- Public hoac unpublic file de Student tai hoc

Tai lieu nay map theo code backend hien tai.

## 1) Tong quan API

Luu y duong dan:

- Controller dang dinh nghia duong dan dang /lesson-slides...
- Trong moi truong cua du an, FE dang goi qua prefix /api
- Vi vay FE nen goi theo /api/lesson-slides...

Nhom API Teacher:

- POST /api/lesson-slides/generate-pptx
- POST /api/lesson-slides/generate-pptx-from-json
- GET /api/lesson-slides/generated
- GET /api/lesson-slides/generated?lessonId={lessonId}
- GET /api/lesson-slides/generated/{generatedFileId}/download
- PATCH /api/lesson-slides/generated/{generatedFileId}/publish
- PATCH /api/lesson-slides/generated/{generatedFileId}/unpublish

Nhom API Public cho Student:

- GET /api/lesson-slides/public/lessons/{lessonId}
- GET /api/lesson-slides/public/lessons/{lessonId}/generated
- GET /api/lesson-slides/public/generated/{generatedFileId}/download

## 2) Hanh vi backend quan trong cho FE

- Moi lan goi generate-pptx hoac generate-pptx-from-json:
  - Backend tra ve file pptx ngay lap tuc
  - Dong thoi backend tu dong luu metadata file da generate de list va tai lai sau
- File moi generate mac dinh isPublic = false
- Teacher co the publish tung generated file
- Student chi thay va tai duoc file generated da public
- Public file con phu thuoc lesson status:
  - Lesson phai o PUBLISHED

## 3) Kieu du lieu FE can dung

Response item cho danh sach generated slide:

- id: UUID
- lessonId: UUID
- templateId: UUID or null
- fileName: string
- contentType: string
- fileSizeBytes: number
- isPublic: boolean
- publishedAt: ISO datetime or null
- createdAt: ISO datetime
- updatedAt: ISO datetime

ApiResponse wrapper:

- message: string (co the null)
- result: data thuc te

## 4) API chi tiet cho FE

### 4.1 Generate va luu lich su

POST /api/lesson-slides/generate-pptx
POST /api/lesson-slides/generate-pptx-from-json

Auth:

- Bearer token role TEACHER

Response:

- Binary file pptx
- Header co Content-Disposition de dat ten file

FE xu ly:

1. Goi API
2. Nhan blob
3. Tao link tai file ngay
4. Sau khi tai xong, reload danh sach GET /generated de thay record moi

### 4.2 Lay danh sach file da generate

GET /api/lesson-slides/generated
GET /api/lesson-slides/generated?lessonId={lessonId}

Auth:

- Bearer token role TEACHER

Response:

- ApiResponse<List<LessonSlideGeneratedFileResponse>>
- Danh sach sap xep moi nhat truoc

### 4.3 Tai lai file da generate (Teacher)

GET /api/lesson-slides/generated/{generatedFileId}/download

Auth:

- Bearer token role TEACHER
- Backend check owner hoac ADMIN

Response:

- Binary pptx

### 4.4 Public file generated cho student

PATCH /api/lesson-slides/generated/{generatedFileId}/publish

Auth:

- Bearer token role TEACHER
- Backend check owner hoac ADMIN

Response:

- ApiResponse<LessonSlideGeneratedFileResponse>
- isPublic se la true

### 4.5 Unpublic file generated

PATCH /api/lesson-slides/generated/{generatedFileId}/unpublish

Auth:

- Bearer token role TEACHER

Response:

- ApiResponse<LessonSlideGeneratedFileResponse>
- isPublic se la false

### 4.6 Student list file generated public theo lesson

GET /api/lesson-slides/public/lessons/{lessonId}/generated

Auth:

- Public endpoint

Response:

- ApiResponse<List<LessonSlideGeneratedFileResponse>>

Dieu kien:

- Lesson phai la PUBLISHED
- Generated file phai isPublic = true

### 4.7 Student tai file generated public

GET /api/lesson-slides/public/generated/{generatedFileId}/download

Auth:

- Public endpoint

Response:

- Binary pptx

Dieu kien:

- File phai isPublic = true
- Lesson cua file phai la PUBLISHED

## 5) Goi y man hinh FE

Teacher - Slide Library tab:

- Filter theo lesson
- Bang danh sach cot:
  - File name
  - Created at
  - Size
  - Public status
  - Action
- Action:
  - Download
  - Publish neu dang private
  - Unpublish neu dang public

Student - Lesson public slide tab:

- Load GET /public/lessons/{lessonId}/generated
- Hien danh sach file
- Nut Download

## 6) Goi y xu ly download blob (frontend)

Huong dan chung:

1. fetch voi responseType blob
2. doc Content-Disposition de lay ten file
3. tao object URL
4. tao the a va trigger download
5. revoke object URL

Neu khong doc duoc ten file tu header thi fallback ten:

- generated-slide.pptx

## 7) Error code FE can map

Moi duoc them:

- 1166 GENERATED_SLIDE_NOT_FOUND
- 1167 GENERATED_SLIDE_ACCESS_DENIED
- 1168 GENERATED_SLIDE_NOT_PUBLIC

Lien quan co san:

- 1089 LESSON_NOT_FOUND
- 1039 NOT_A_TEACHER
- 1007 UNAUTHORIZED

## 8) File backend da cap nhat

- Entity moi:
  - src/main/java/com/fptu/math_master/entity/LessonSlideGeneratedFile.java
- Repository moi:
  - src/main/java/com/fptu/math_master/repository/LessonSlideGeneratedFileRepository.java
- DTO moi:
  - src/main/java/com/fptu/math_master/dto/response/LessonSlideGeneratedFileResponse.java
- Service interface cap nhat:
  - src/main/java/com/fptu/math_master/service/LessonSlideService.java
- Service impl cap nhat:
  - src/main/java/com/fptu/math_master/service/impl/LessonSlideServiceImpl.java
- Controller teacher cap nhat:
  - src/main/java/com/fptu/math_master/controller/LessonSlideController.java
- Controller public cap nhat:
  - src/main/java/com/fptu/math_master/controller/LessonSlidePublicController.java
- Error code cap nhat:
  - src/main/java/com/fptu/math_master/exception/ErrorCode.java
- SQL migration moi:
  - scripts/sql/V3\_\_create_lesson_slide_generated_files.sql

## 9) Luu y deploy

Do spring.jpa.hibernate.ddl-auto dang la none, can chay SQL migration V3 truoc khi dung API moi.

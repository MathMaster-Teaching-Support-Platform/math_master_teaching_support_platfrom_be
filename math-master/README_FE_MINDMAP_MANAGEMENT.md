# FE Integration Guide: Mindmap Management

Muc tieu:

- Quan ly mindmap cho teacher (tao, sua, publish, export)
- Student xem mindmap da publish
- Dong bo dung luong API hien tai cua BE

Tai lieu nay map theo backend hien tai.

## 1) Tong quan API

Luu y duong dan:

- Controller dang khai bao /mindmaps...
- FE goi qua prefix /api
- Vi vay FE can goi /api/mindmaps...

Nhom API Teacher:

- POST /api/mindmaps
- POST /api/mindmaps/generate
- GET /api/mindmaps/{id}
- PUT /api/mindmaps/{id}
- DELETE /api/mindmaps/{id}
- PATCH /api/mindmaps/{id}/publish
- PATCH /api/mindmaps/{id}/archive
- GET /api/mindmaps/my-mindmaps
- GET /api/mindmaps/lesson/{lessonId}
- GET /api/mindmaps/{id}/export?format=pdf|png
- POST /api/mindmaps/nodes
- PUT /api/mindmaps/nodes/{nodeId}
- DELETE /api/mindmaps/nodes/{nodeId}
- GET /api/mindmaps/{mindmapId}/nodes

Nhom API Student/Public (mindmap da publish):

- GET /api/mindmaps/public
- GET /api/mindmaps/public/{id}
- GET /api/mindmaps/public/{mindmapId}/nodes
- GET /api/mindmaps/public/{id}/export?format=pdf|png

## 2) Luu y quyen truy cap (quan trong)

- Hien tai /api/mindmaps/public/\*\* KHONG phai anonymous.
- Endpoint nay bat buoc user da dang nhap va co role STUDENT/TEACHER/ADMIN.
- Neu FE goi khi chua login se bi 401/1007.

Tom tat role:

- STUDENT: chi xem (public list/detail/nodes/export, lesson listing)
- TEACHER: xem + thao tac CRUD + node + publish/archive
- ADMIN: co quyen nhu teacher trong module nay

## 3) Kieu du lieu FE can dung

MindmapResponse (item list):

- id: UUID
- teacherId: UUID
- lessonId: UUID or null
- title: string
- description: string
- aiGenerated: boolean
- generationPrompt: string or null
- status: DRAFT | PUBLISHED | ARCHIVED
- createdAt: ISO datetime
- updatedAt: ISO datetime

MindmapDetailResponse:

- mindmap: MindmapResponse
- nodes: MindmapNodeResponse[]

MindmapNodeResponse:

- id: UUID
- mindmapId: UUID
- parentId: UUID or null
- content: string
- color: string or null
- icon: string or null
- displayOrder: number
- children: MindmapNodeResponse[]

ApiResponse wrapper:

- code: number (co the null)
- message: string (co the null)
- result: du lieu chinh

## 4) API chi tiet cho FE

### 4.1 List public mindmaps (cho student)

GET /api/mindmaps/public

Query params:

- lessonId: UUID (optional)
- name: string (optional)
- page: number (default 0)
- size: number (default 10)
- sortBy: string (default createdAt)
- direction: ASC|DESC (default DESC)

Auth:

- Bat buoc Bearer token role STUDENT/TEACHER/ADMIN

Response:

- ApiResponse<Page<MindmapResponse>>

### 4.2 Public detail + nodes

GET /api/mindmaps/public/{id}
GET /api/mindmaps/public/{mindmapId}/nodes

Auth:

- Bat buoc Bearer token role STUDENT/TEACHER/ADMIN

Dieu kien:

- Mindmap phai co status = PUBLISHED

### 4.3 Export mindmap

Teacher export:

- GET /api/mindmaps/{id}/export?format=pdf
- GET /api/mindmaps/{id}/export?format=png

Student/public export:

- GET /api/mindmaps/public/{id}/export?format=pdf
- GET /api/mindmaps/public/{id}/export?format=png

Response:

- Binary file voi Content-Disposition attachment

### 4.4 Teacher CRUD

Tao:

- POST /api/mindmaps

AI generate:

- POST /api/mindmaps/generate

Sua:

- PUT /api/mindmaps/{id}

Xoa:

- DELETE /api/mindmaps/{id}

Publish:

- PATCH /api/mindmaps/{id}/publish

Archive:

- PATCH /api/mindmaps/{id}/archive

### 4.5 Node CRUD (teacher)

- POST /api/mindmaps/nodes
- PUT /api/mindmaps/nodes/{nodeId}
- DELETE /api/mindmaps/nodes/{nodeId}
- GET /api/mindmaps/{mindmapId}/nodes

## 5) FE flow goi y

Teacher screen:

1. Load my mindmaps: GET /api/mindmaps/my-mindmaps
2. Open editor: GET /api/mindmaps/{id}
3. Save metadata: PUT /api/mindmaps/{id}
4. Save nodes: POST/PUT/DELETE node API
5. Publish: PATCH /api/mindmaps/{id}/publish

Student screen:

1. Login
2. Load list: GET /api/mindmaps/public
3. Open detail: GET /api/mindmaps/public/{id}
4. Load nodes: GET /api/mindmaps/public/{id}/nodes
5. Download: GET /api/mindmaps/public/{id}/export?format=pdf

## 6) Mau request

Tao mindmap:

```json
{
  "title": "Ham so bac nhat",
  "description": "Tong hop kien thuc co ban",
  "lessonId": "<lesson-uuid>"
}
```

Tao node:

```json
{
  "mindmapId": "<mindmap-uuid>",
  "parentId": null,
  "content": "Ham so y = ax + b",
  "color": "#2563eb",
  "icon": "book",
  "displayOrder": 1
}
```

Generate AI:

```json
{
  "title": "He thuc luong trong tam giac vuong",
  "description": "Sinh so do nhanh",
  "lessonId": "<lesson-uuid>",
  "prompt": "Tap trung vao cong thuc va bai tap van dung",
  "levels": 3
}
```

## 7) Error/edge cases FE can map

- 1007: You do not have permission
- MINDMAP_NOT_FOUND
- MINDMAP_ACCESS_DENIED

Tinh huong hay gap:

- Chua login goi /mindmaps/public/\* => 401/1007
- Mindmap chua publish goi public detail => access denied/not found theo mapping BE

## 8) File backend lien quan

- src/main/java/com/fptu/math_master/controller/MindmapController.java
- src/main/java/com/fptu/math_master/service/MindmapService.java
- src/main/java/com/fptu/math_master/service/impl/MindmapServiceImpl.java
- src/main/java/com/fptu/math_master/configuration/SecurityConfig.java

## 9) Ghi chu cho FE

- Neu FE can cho guest (khong login) xem mindmap public, can doi security rule cho /api/mindmaps/public/\*\* sang permitAll.
- Hien tai policy cua BE la "public theo noi dung", nhung van "private theo account" (phai login).

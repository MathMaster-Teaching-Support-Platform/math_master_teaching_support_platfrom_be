# Hướng dẫn Agent — Đọc & Cập nhật PROJECT_STRUCTURE.md

## Mục đích

File [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) chứa bản đồ toàn bộ source code của project **Math Master**.  
Agent **BẮT BUỘC** phải đọc file này trước khi bắt đầu bất kỳ task nào, và cập nhật lại khi có thay đổi cấu trúc.

---

## Khi nào phải ĐỌC file này?

- **Mỗi đầu conversation / task mới** — đọc `PROJECT_STRUCTURE.md` để nắm cấu trúc trước khi tìm file.
- **Khi cần tìm file** — xem bảng Domain Modules để biết controller/service/entity liên quan.
- **Khi cần hiểu naming convention** — xem mục "Quy ước đặt tên".

---

## Khi nào phải CẬP NHẬT file này?

Cập nhật **ngay sau khi hoàn tất** bất kỳ thao tác nào sau đây:

| Thao tác                      | Cần cập nhật                                                  |
| ----------------------------- | ------------------------------------------------------------- |
| **Thêm file Java mới**        | Thêm tên file vào section tương ứng (controller, entity, …)   |
| **Xoá file Java**             | Xoá tên file khỏi section tương ứng                           |
| **Rename file**               | Đổi tên trong section tương ứng                               |
| **Thêm package mới**          | Thêm package vào cây "Package Path" + tạo section mới nếu cần |
| **Thêm dependency (pom.xml)** | Cập nhật bảng "Tech Stack"                                    |
| **Thêm migration SQL**        | Cập nhật mục "Resources → db/migration/"                      |
| **Thêm email template**       | Cập nhật mục "Resources → templates/email/"                   |
| **Thêm/sửa config file gốc**  | Cập nhật mục "Root Files" (docker-compose, nginx, …)          |
| **Thêm script**               | Cập nhật mục "Scripts"                                        |
| **Thêm domain module mới**    | Thêm row vào bảng "Domain Modules"                            |

### KHÔNG cần cập nhật khi:

- Chỉ sửa nội dung bên trong file (logic, thêm method, fix bug…)
- Thay đổi config values trong application.yaml
- Thêm/sửa test data

---

## Cách cập nhật

### Bước 1 — Đọc file hiện tại

```
read_file PROJECT_STRUCTURE.md
```

### Bước 2 — Xác định section cần sửa

Mỗi section trong file tương ứng 1 package/folder:

| Package                     | Section trong file                          |
| --------------------------- | ------------------------------------------- |
| `controller/`               | **controller/ — REST Controllers**          |
| `entity/`                   | **entity/ — JPA Entities**                  |
| `repository/`               | **repository/ — Spring Data JPA Repos**     |
| `service/`                  | **service/ — Business Logic (interfaces)**  |
| `service/impl/`             | **service/impl/ — Service Implementations** |
| `service/async/`            | **service/async/ — Async Job Processing**   |
| `dto/request/`              | **dto/request/ — Inbound DTOs**             |
| `dto/response/`             | **dto/response/ — Outbound DTOs**           |
| `enums/`                    | **enums/ — Enum Definitions**               |
| `configuration/`            | **configuration/ — Spring Configs**         |
| `configuration/properties/` | **configuration/properties/**               |
| `component/`                | **component/ — Spring Components**          |
| `constant/`                 | **constant/ — Constants**                   |
| `exception/`                | **exception/ — Error Handling**             |
| `util/`                     | **util/ — Utilities**                       |
| `src/main/resources/`       | **Resources**                               |
| `src/test/`                 | **Test**                                    |

### Bước 3 — Thêm/xoá/rename tên file

- Giữ **alphabetical order** trong mỗi section.
- Nếu file có ghi chú đặc biệt (ví dụ `BaseEntity.java — Abstract base`), giữ format comment `— mô tả`.
- Nếu thêm module nghiệp vụ mới, thêm row vào bảng **Domain Modules**.

### Bước 4 — Cập nhật ngày

Sửa dòng `**Cập nhật lần cuối:**` ở đầu file thành ngày hiện tại.

### Bước 5 — Verify

Sau khi edit, kiểm tra nhanh:

- [ ] File mới đã xuất hiện đúng section?
- [ ] Thứ tự alphabetical đúng?
- [ ] Bảng Domain Modules phản ánh đúng?
- [ ] Ngày cập nhật đã đổi?

---

## Ví dụ cụ thể

### Ví dụ 1 — Thêm feature MathDrawing

Giả sử tạo các file mới:

```
entity/MathDrawing.java
repository/MathDrawingRepository.java
service/MathDrawingService.java
service/impl/MathDrawingServiceImpl.java
controller/MathDrawingController.java
dto/request/GenerateMathDrawingRequest.java
dto/response/MathDrawingResponse.java
enums/MathDrawingStatus.java
```

→ Cần cập nhật **8 section** trong PROJECT_STRUCTURE.md + thêm row Domain Modules:

```markdown
| Math Drawing | MathDrawingController | MathDrawing | MathDrawingService |
```

### Ví dụ 2 — Thêm migration SQL mới

Giả sử tạo `V4__create_math_drawing_table.sql` trong `db/migration/`:

→ Cập nhật section **Resources**:

```markdown
db/migration/
V1**Add_OCR_Fields_To_Teacher_Profile.sql
V3**Add_Token_Fields_To_Subscription.sql
V4\_\_create_math_drawing_table.sql ← THÊM
```

### Ví dụ 3 — Rename file

Nếu rename `GeminiController.java` → `AiController.java`:

→ Tìm và thay tên trong section **controller/** + bảng **Domain Modules**.

---

## Quy tắc cho Agent

1. **ĐỌC** `PROJECT_STRUCTURE.md` ở đầu mỗi task.
2. **CẬP NHẬT** ngay sau khi tạo/xoá/rename file — **KHÔNG** để sang bước sau.
3. **KHÔNG** chỉnh sửa nội dung file nếu không có thay đổi cấu trúc thực tế.
4. Khi tạo feature mới với nhiều file, **gom tất cả update** vào 1 lần edit duy nhất cho hiệu quả.
5. Nếu không chắc file nào đã thay đổi, chạy `list_dir` trên package liên quan rồi so sánh với file.

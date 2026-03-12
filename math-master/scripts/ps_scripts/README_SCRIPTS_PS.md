# 📚 Math Master PowerShell Scripts - Hướng Dẫn Chi Tiết

**Phiên bản**: 1.0
**Dành cho**: Windows Developers sử dụng PowerShell
**Bổ sung**: Ubuntu Scripts tương đương cho Linux/macOS/WSL2

---

## 📋 Mục Lục

1. [Cài Đặt Nhanh](#cài-đặt-nhanh)
2. [Cấu Trúc Script](#cấu-trúc-script)
3. [Hướng Dẫn Sử Dụng](#hướng-dẫn-sử-dụng)
4. [Các Tác Vụ Phổ Biến](#các-tác-vụ-phổ-biến)
5. [Cấu Hình Chi Tiết](#cấu-hình-chi-tiết)
6. [Khắc Phục Sự Cố](#khắc-phục-sự-cố)

---

## ⚡ Cài Đặt Nhanh

### Bước 1: Mở PowerShell

Nhấp chuột phải vào PowerShell → **Run as Administrator**

### Bước 2: Cho phép chạy scripts

```powershell
# Chỉ cần chạy lần đầu tiên
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### Bước 3: Cài đặt Manager

```powershell
# Chuyển tến thư mục ps_scripts
cd scripts\ps_scripts

# Chạy installer
.\install.ps1
```

### Bước 4: Xác Minh Cài Đặt

```powershell
# Reload PowerShell profile
. $PROFILE

# Hoặc mở terminal PowerShell mới

# Chạy manager
khoipd_terminal_ps
```

---

## 📁 Cấu Trúc Script

```
scripts/ps_scripts/
├── manager.ps1              # Script quản lý chính
├── khoipd_terminal_ps.ps1   # Wrapper script (được tạo bởi installer)
├── install.ps1              # Script cài đặt
├── apply-format.ps1         # Code formatting script (wrapper)
├── deploy-on-local.ps1      # Local deployment script (wrapper)
└── README_SCRIPTS_PS.md     # Tài liệu này
```

### Mô Tả File

| File                    | Mục Đích                | Chạy Qua                                  |
| ----------------------- | ----------------------- | ----------------------------------------- |
| **manager.ps1**         | Logic chính của manager | `khoipd_terminal_ps` hoặc `.\manager.ps1` |
| **install.ps1**         | Cài đặt alias và setup  | Chạy 1 lần tiên                           |
| **apply-format.ps1**    | Wrapper cho formatting  | `.\apply-format.ps1`                      |
| **deploy-on-local.ps1** | Wrapper cho deployment  | `.\deploy-on-local.ps1`                   |

---

## 🎯 Hướng Dẫn Sử Dụng

### Cách 1: Sử dụng Alias (sau khi cài đặt)

```powershell
khoipd_terminal_ps
```

### Cách 2: Chạy trực tiếp

```powershell
cd scripts\ps_scripts
.\manager.ps1
```

### Cách 3: Chạy Task cụ thể

```powershell
# Format code
khoipd_terminal_ps -Task ApplyFormat

# Local deployment
khoipd_terminal_ps -Task DeployLocal

# Show status
khoipd_terminal_ps -Task Status

# Help
khoipd_terminal_ps -Help
```

### Giao Diện Menu Chính

Khi chạy `khoipd_terminal_ps`, bạn sẽ thấy menu chính:

```
╔════════════════════════════════════════════════════════╗
║  Math Master Project Manager                          ║
╚════════════════════════════════════════════════════════╝

Available Tasks:
  1. Format Code          (Apply code formatting)
  2. Local Deployment     (Build and deploy with Docker)
  3. Project Status       (Show environment info)
  4. Help                 (Show help information)
  5. Exit                 (Close manager)

Select option: _
```

---

## 📋 Các Tác Vụ Phổ Biến

### 🔧 Tác Vụ 1: Format Code

**Mục đích**: Áp dụng code formatting sử dụng Spotless (Google Java Format)

**Các bước**:

```powershell
1. Chạy: khoipd_terminal_ps
2. Chọn: 1. Format Code
```

**Hoặc chạy trực tiếp**:

```powershell
khoipd_terminal_ps -Task ApplyFormat

# Hoặc
.\apply-format.ps1
```

**Kết quả**:

- ✓ Kiểm tra formatting issues
- ✓ Tự động fix formatting
- ✓ Xóa import không dùng
- ✓ Chuẩn hóa khoảng trắng

**Thời gian**: ~30-60 giây (lần đầu) / ~10-30 giây (lần tiếp theo)

### 🐳 Tác Vụ 2: Local Deployment

**Mục đích**: Build và khởi động tất cả services sử dụng Docker

**Các bước**:

```powershell
1. Chạy: khoipd_terminal_ps
2. Chọn: 2. Local Deployment
```

**Hoặc chạy trực tiếp**:

```powershell
khoipd_terminal_ps -Task DeployLocal

# Hoặc
.\deploy-on-local.ps1
```

**Nó sẽ**:

- ✓ Kiểm tra Docker
- ✓ Kiểm tra .env file
- ✓ Dừng containers cũ (nếu có)
- ✓ Build Docker images
- ✓ Khởi động tất cả services

**Services được khởi động**:

```
✓ PostgreSQL (Database)
✓ Redis (Cache & Real-time)
✓ Centrifugo (WebSocket server)
✓ Math Master App (Backend API)
```

**Thời gian**: 2-5 phút (lần đầu) / 30-60 giây (lần tiếp theo)

**Sau khi hoàn tất, truy cập**:

```
Database:   localhost:5432
Redis:      localhost:6379
Centrifugo: http://localhost:8000
Backend:    http://localhost:8080
Swagger:    http://localhost:8080/swagger-ui.html
```

### 📊 Tác Vụ 3: Project Status

**Mục đích**: Xem trạng thái dự án và services

**Các bước**:

```powershell
khoipd_terminal_ps -Task Status
```

**Hiển thị**:

- Phiên bản Java, Maven, Docker
- Danh sách containers đang chạy
- Status của từng service

### ❓ Tác Vụ 4: View Help

**Các bước**:

```powershell
khoipd_terminal_ps -Help
```

**Hoặc chọn option 4 từ menu**

---

## ⚙️ Cấu Hình Chi Tiết

### Environment File: `.env`

File này chứa cấu hình cho Docker Compose:

```bash
# Bắt buộc phải có - copy từ .env.production.example
cp .env.production.example .env
```

**Các biến quan trọng**:

```
DATABASE_URL=jdbc:postgresql://postgres:5432/mathmaster
REDIS_HOST=redis
REDIS_PORT=6379
CENTRIFUGO_PORT=8000
APP_PORT=8080
```

### Java & Maven Configuration

Khi chạy tasks, scripts sẽ tự động:

- ✓ Detect Java 21
- ✓ Detect Maven 3.6+
- ✓ Sử dụng mvnw.cmd (Maven wrapper)

**Yêu cầu tối thiểu**:

- Java 21+
- Maven 3.6+
- Docker Desktop (cho deployment tasks)

---

## 🚨 Khắc Phục Sự Cố

### ❌ Lỗi: "Could not find PowerShell Script"

**Nguyên nhân**: Script path không đúng

**Giải pháp**:

```powershell
# Chắc chắn bạn đang trong đúng thư mục
cd scripts\ps_scripts

# Kiểm tra file tồn tại
Get-ChildItem *.ps1

# Chạy lại
.\manager.ps1
```

### ❌ Lỗi: "ExecutionPolicy"

**Nguyên nhân**: PowerShell policy không cho phép chạy scripts

**Giải pháp**:

```powershell
# Sửa policy (chỉ lần đầu)
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# Xác nhận: Y
```

### ❌ Lỗi: "Docker daemon is not running"

**Nguyên nhân**: Docker Desktop không chạy

**Giải pháp**:

```powershell
# Mở Docker Desktop
# Windows Start Menu → Docker Desktop

# Chờ khoảng 30 giây đến khi Docker hoàn toàn khởi động

# Kiểm tra lại
docker ps

# Chạy lại deployment
khoipd_terminal_ps -Task DeployLocal
```

### ❌ Lỗi: ".env file not found"

**Nguyên nhân**: Chưa tạo .env file

**Giải pháp**:

```powershell
# Từ thư mục project root
Copy-Item .env.production.example .env

# Chỉnh sửa .env nếu cần (dùng Notepad++ hoặc VS Code)
notepad .env

# Chạy deployment lại
khoipd_terminal_ps -Task DeployLocal
```

### ❌ Lỗi: "mvnw.cmd not found"

**Nguyên nhân**: Script chạy từ sai thư mục

**Giải pháp**:

```powershell
# cd tới project root (nơi có mvnw.cmd)
cd d:\final_thesis\math_master_teaching_support_platfrom_be\math-master

# Kiểm tra
Get-ChildItem mvnw*

# Chạy lại formatting
khoipd_terminal_ps -Task ApplyFormat
```

### ❌ Lỗi: "Formatting failed" / "Build failed"

**Nguyên nhân**: Có error trong code hoặc dependency

**Giải pháp**:

```powershell
# Xem chi tiết error từ output
# Scroll up để đọc thông điệp lỗi

# Nếu là dependency issue:
mvn clean install

# Nếu là code issue:
# Sửa code theo error message rồi chạy lại
khoipd_terminal_ps -Task ApplyFormat
```

---

## 📱 So Sánh: PowerShell vs Bash Scripts

| Task             | PowerShell (Windows)                   | Bash (Linux/macOS/WSL2)                                   |
| ---------------- | -------------------------------------- | --------------------------------------------------------- |
| **Entry Point**  | `khoipd_terminal_ps`                   | `khoipd_terminal`                                         |
| **Formatting**   | `khoipd_terminal_ps -Task ApplyFormat` | `khoipd_terminal → 1. Build Operations → 2. Build Docker` |
| **Deployment**   | `khoipd_terminal_ps -Task DeployLocal` | `khoipd_terminal → 2. Docker Compose → 1. Start`          |
| **Health Check** | `khoipd_terminal_ps -Task Status`      | `khoipd_terminal → 4. Health Checks`                      |

---

## 🎓 Tips & Best Practices

### ✅ Do's

- ✓ Chạy installer lần đầu (`.\install.ps1`)
- ✓ Format code trước khi commit
- ✓ Kiểm tra Docker đang chạy trước local deployment
- ✓ Xem logs nếu deployment fail
- ✓ Sử dụng `-Task` parameter cho CI/CD automation

### ❌ Don'ts

- ✗ Không sửa manager.ps1 khi đang chạy
- ✗ Không chạy as Administrator nếu không cần thiết
- ✗ Không commit .env file (chứa sensitive data)
- ✗ Không bỏ qua .editorconfig - dùng để chuẩn hóa format
- ✗ Không kill Docker process - dùng `docker compose down`

### 🎯 Performance Tips

```powershell
# Format nhanh hơn - chỉ check, không fix
mvn spotless:check

# Deploy nhanh hơn - reuse images (bỏ --no-cache)
docker compose build
docker compose up -d

# Debug mode
khoipd_terminal_ps -Task ApplyFormat -Verbose
```

---

## 📞 Support & Contributing

### Báo Cáo Vấn Đề

Khi báo cáo issue, cung cấp:

```powershell
# 1. Output của command
khoipd_terminal_ps -Task ApplyFormat 2>&1 | Out-File error.txt

# 2. Environment info
java -version
mvn -version
docker --version
docker-compose --version

# 3. Tệp đính kèm:
# - error.txt (full output)
# - .env (nếu có permission)
# - pom.xml
```

### Liên Hệ

- **GitHub Issues**: Math Master Repository
- **Email**: (DevOps Team)
- **Slack**: #math-master-dev

---

## 📚 Tham Khảo

### Liên Kết Hữu Ích

- [PowerShell Docs](https://docs.microsoft.com/en-us/powershell/)
- [Docker Docs](https://docs.docker.com/)
- [Maven Docs](https://maven.apache.org/)
- [Google Java Format](https://github.com/google/google-java-format)
- [Spotless Plugin](https://github.com/diffplug/spotless)

### Liên Kết Bên Trong

- [Ubuntu Scripts Documentation](./README_SCRIPTS.md)
- [Project README](../README.md)

---

**Phiên bản**: 1.0  
**Cập nhật**: March 13, 2025  
**Tác giả**: DevOps Team

Chúc bạn phát triển vui vẻ! 🚀

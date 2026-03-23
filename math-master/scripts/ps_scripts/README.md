# Math Master PowerShell Scripts (Windows)

Công cụ quản lý dự án **Math Master Backend** để sử dụng trên Windows với PowerShell.

**Phiên bản**: 2.0  
**Yêu cầu**: PowerShell 5.1+, Windows 10+  
**Dành cho**: Developers Windows

---

## 🚀 Khởi Động Nhanh

### 1️⃣ Cài Đặt

```powershell
# Mở PowerShell as Administrator
cd scripts\ps_scripts
.\install.ps1
```

**Lưu ý**: Scripts sẽ tạo alias `khoipd_terminal_ps` và thêm vào PowerShell profile. Bạn có thể chạy từ bất kỳ thư mục nào sau khi cài đặt.

### 2️⃣ Sau Khi Cài Đặt

```powershell
# Reload PowerShell profile hoặc mở terminal mới
. $PROFILE

# Chạy từ bất kỳ đâu (interactive menu)
khoipd_terminal_ps
```

### 3️⃣ Chọn Task

```
  Math Master Manager
  -----------------------------------
  [1]  Format Code (Spotless)
  [2]  Sort Annotations
  [3]  Maven Clean Build
  [4]  Clean Build + Start Project
  -----------------------------------
  [5]  Start Redis
  [6]  Stop Redis
  [7]  Restart Redis
  -----------------------------------
  [8]  Start All Services
  [9]  Stop All Services
  [10] Restart All Services
  -----------------------------------
  [11] Deploy Local (build + up)
  [12] Deploy Full  (down+build+up)
  [13] Recreate Docker Containers
  -----------------------------------
  [14] Tail All Logs
  [15] Tail Redis Logs
  [16] Tail App Logs
  -----------------------------------
  [17] Project Status
  [18] Delete Log Files
  [19] Help
  [20] Clear Screen
  [0]  Exit
```

**Tính năng**:

- ✅ Tự động quay lại menu sau mỗi task
- ✅ Giao diện sạch, không nhiễu output
- ✅ Tự động navigate về project root từ bất kỳ thư mục nào
- ✅ Hỗ trợ cả interactive menu và `-Task` flag cho CI/scripting

---

## 📋 Các Công Cụ Sẵn Có

### Code Formatting

```powershell
khoipd_terminal_ps -Task ApplyFormat

# Hoặc chạy trực tiếp
.\apply-format.ps1
```

**Công dụng**: Check & apply Spotless formatter (Google Java Format, import order, trailing spaces)

---

### Sort Annotations

```powershell
khoipd_terminal_ps -Task SortAnnotations
```

**Công dụng**: Chuẩn hóa thứ tự Lombok annotations trong thư mục `entity/`

---

### Maven Clean Build

```powershell
khoipd_terminal_ps -Task CleanBuild
```

**Công dụng**: Chạy `mvnw clean compile` locally (không cần Docker)

---

### Clean Build + Start Project

```powershell
khoipd_terminal_ps -Task CleanBuildStart
```

**Công dụng**: Combo đầy đủ để khởi động project:

1. Khởi động tất cả Docker services (`docker compose up -d`)
2. Chạy `mvnw clean compile` để build sạch
3. Khởi động Spring Boot app (`mvnw spring-boot:run`)

> **Lưu ý**: App sẽ chạy ngay trong terminal (Ctrl+C để dừng)

---

### Redis Management

```powershell
khoipd_terminal_ps -Task StartRedis
khoipd_terminal_ps -Task StopRedis
khoipd_terminal_ps -Task RestartRedis
```

**Công dụng**: Khởi động / dừng / khởi động lại Redis container riêng lẻ

---

### Service Management

```powershell
khoipd_terminal_ps -Task StartAll
khoipd_terminal_ps -Task StopAll
khoipd_terminal_ps -Task RestartAll
```

**Công dụng**: Quản lý toàn bộ docker-compose services (Redis, Centrifugo, MinIO, ...)

---

### Deployment

```powershell
# Build images (no-cache) rồi khởi động
khoipd_terminal_ps -Task DeployLocal

# Full cycle: down + prune + build no-cache + up
khoipd_terminal_ps -Task DeployFull

# Force-recreate containers không rebuild image
khoipd_terminal_ps -Task RecreateDocker
khoipd_terminal_ps -Task RecreateDocker -Service redis   # chỉ một service

# Hoặc dùng wrapper
.\deploy-on-local.ps1
```

---

### Logs

```powershell
khoipd_terminal_ps -Task Logs          # Tất cả services
khoipd_terminal_ps -Task LogsRedis     # Redis container
khoipd_terminal_ps -Task LogsApp       # App container
```

---

### Project Status

```powershell
khoipd_terminal_ps -Task Status
```

**Công dụng**: Hiển thị trạng thái containers và kích thước Docker images

---

### Delete Logs

```powershell
khoipd_terminal_ps -Task DeleteLogs
```

**Công dụng**: Xóa tất cả log files (`*.log`, `*.txt`) từ thư mục `logs/`

---

## 📁 Cấu Trúc Thư Mục

```
scripts/ps_scripts/
├── manager.ps1              # Main manager logic + interactive menu
├── install.ps1              # Installation script (tạo alias)
├── apply-format.ps1         # Code formatting wrapper
├── sort-annotations.ps1     # Annotation sorter cho entity files
├── deploy-on-local.ps1      # Local deployment wrapper
├── README.md                # File này
└── README_SCRIPTS_PS.md     # Tài liệu chi tiết (Tiếng Việt)
```

---

## ⚙️ Yêu Cầu Hệ Thống

### Bắt Buộc

- ✅ **PowerShell 5.1+** (built-in từ Windows 10)
- ✅ **Java 21+**
- ✅ **Maven 3.6+**

### Tuỳ Chọn (cho deployment tasks)

- 🐳 **Docker Desktop** (for local deployment)

---

## 🔧 Configuration

### PowerShell Execution Policy

Chạy lần đầu:

```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
# Nhấp Y để xác nhận
```

### Environment Variables

Copy `.env.production.example` → `.env`:

```powershell
Copy-Item .env.production.example .env
```

---

## 📚 Tài Liệu

- **Hướng dẫn đầy đủ**: [README_SCRIPTS_PS.md](README_SCRIPTS_PS.md) (Tiếng Việt)
- **Troubleshooting**: Xem mục "Khắc Phục Sự Cố"

---

## 🆘 Troubleshooting

### Common Issues

| Issue                   | Solution                                                               |
| ----------------------- | ---------------------------------------------------------------------- |
| `ExecutionPolicy` error | `Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser` |
| Alias not found         | `. $PROFILE` hoặc mở terminal PowerShell mới                           |
| Docker not running      | Mở Docker Desktop, chờ khởi động xong                                  |
| `.env not found`        | `Copy-Item .env.production.example .env`                               |

Xem [README_SCRIPTS_PS.md](README_SCRIPTS_PS.md#-khắc-phục-sự-cố) cho giải pháp chi tiết.

---

## 📞 Support

- 📖 **Documentation**: [README_SCRIPTS_PS.md](README_SCRIPTS_PS.md)
- 🆘 **Help**: `khoipd_terminal_ps -Help`
- 📧 **Contact**: DevOps Team

---

## 🔄 Relationship với Ubuntu Scripts

PowerShell scripts này là counterpart Windows của [ubuntu_scripts](../ubuntu_scripts/README.md).

**Khác biệt chính**:

- PowerShell: Full menu (Format, Annotations, Build, Redis, Deploy, Logs, Status)
- Bash: Full menu with many options + utilities + database management

**Giống nhau**:

- Same underlying logic
- Same Docker Compose setup
- Same formatting standard (Spotless / Google Java Format)

---

**Version**: 2.0  
**Last Updated**: March 23, 2026

Happy Coding! 🚀

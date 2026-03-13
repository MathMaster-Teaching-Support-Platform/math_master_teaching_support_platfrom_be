# Math Master PowerShell Scripts (Windows)

Công cụ quản lý dự án **Math Master Backend** để sử dụng trên Windows với PowerShell.

**Phiên bản**: 1.0  
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

# Chạy từ bất kỳ đâu (output sẽ tự động cleaned)
khoipd_terminal_ps
```

### 3️⃣ Chọn Task

```
1. Format Code           - Apply Google Java Format
2. Local Deployment      - Build & deploy with Docker
3. Delete Logs           - Remove log files
4. Project Status        - Check Docker containers
5. Help                  - Show available tasks
6. Clear Screen          - Clear terminal output
7. Exit                  - Close manager
```

**Tính năng**:

- ✅ Tự động quay lại menu sau mỗi task
- ✅ Không hiển thị True/False (giao diện sạch)
- ✅ Tự động caches Maven metadata (lần thứ 2 nhanh hơn)
- ✅ Chạy từ bất kỳ thư mục nào (auto-navigate to project root)

---

## 📋 Các Công Cụ Sẵn Có

### Code Formatting

```powershell
khoipd_terminal_ps -Task ApplyFormat

# Hoặc chạy trực tiếp
.\apply-format.ps1
```

**Công dụng**: Áp dụng Google Java Format, fixing import, trailing spaces

---

### Local Deployment

```powershell
khoipd_terminal_ps -Task DeployLocal

# Hoặc chạy trực tiếp
.\deploy-on-local.ps1
```

**Công dụng**: Build Docker images, khởi động services (PostgreSQL, Redis, Centrifugo, Backend)

---

### Delete Logs

```powershell
khoipd_terminal_ps -Task DeleteLogs
```

**Công dụng**: Xóa tất cả log files (_.log, _.txt) từ thư mục logs

---

### Project Status

```powershell
khoipd_terminal_ps -Task Status
```

**Công dụng**: Kiểm tra Java, Maven, Docker, container status

---

## 📁 Cấu Trúc Thư Mục

```
scripts/ps_scripts/
├── manager.ps1              # Main manager logic
├── install.ps1              # Installation script
├── apply-format.ps1         # Code formatting (wrapper)
├── deploy-on-local.ps1      # Local deployment (wrapper)
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
- **So sánh Bash vs PowerShell**: Xem mục "So Sánh" trong documentation
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

- PowerShell: Focused tasks (Format, Deploy, Status)
- Bash: Full menu with many options + utilities + database management

**Giống nhau**:

- Same underlying logic
- Same Docker Compose setup
- Same formatting standard (Google Java Format)

---

**Version**: 1.0  
**Last Updated**: March 13, 2025

Happy Coding! 🚀

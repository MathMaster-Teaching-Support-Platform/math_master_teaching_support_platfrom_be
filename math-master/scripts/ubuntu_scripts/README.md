# Math Master Project Manager

Công cụ quản lý dự án **Math Master** - nền tảng hỗ trợ giảng dạy toán học (Spring Boot 3.5.0 + Java 21). Cung cấp các tiện ích toàn diện để build Maven, quản lý Docker Compose, kiểm tra sức khỏe services, và quản lý database.

## 📁 Cấu Trúc Thư Mục

```
ubuntu_scripts/
├── fr-manager.sh              # Script quản lý chính
├── fr                         # Wrapper script
├── install.sh                 # Script cài đặt
├── README.md                  # File này
├── config/
│   └── config.sh              # Cấu hình Math Master
├── lib/
│   ├── colors.sh              # Màu sắc terminal output
│   ├── ui.sh                  # Giao diện người dùng
│   ├── git_utils.sh           # Git utilities
│   ├── status_checker.sh      # Kiểm tra status repo
│   ├── builder.sh             # Maven build operations
│   ├── logger.sh              # Logging utilities
│   ├── environment-setup.sh   # Setup & verify environment
│   ├── health-checks.sh       # Service health checks
│   └── database-migrations.sh # Database management
├── logs/
│   └── (tệp logs tự động tạo)
```

## 🚀 Cài Đặt Nhanh

### Bước 1: Cấp quyền execute

```bash
cd scripts/ubuntu_scripts
chmod +x fr-manager.sh install.sh
chmod +x lib/*.sh
chmod +x config/*.sh
```

### Bước 2: Chạy installer

```bash
./install.sh
```

### Bước 3: Sử dụng

```bash
# Từ bất kỳ thư mục nào (sau khi cài installer)
fr

# Hoặc chạy trực tiếp
./fr-manager.sh
```

## 📋 Tính Năng Chính

### 1️⃣ Build Operations

- **Build Project (Maven)**: Compile, test, package với Maven
- **Build Docker Image**: Tạo Docker image từ Dockerfile
- **Compile Only**: Chỉ compile (skip test & package)
- **Run Tests**: Chạy test suite
- **Clean Build**: Xóa build artifacts

### 2️⃣ Docker Compose Management

Quản lý 4 services chính:
- **PostgreSQL**: Main database
- **Redis**: Caching & real-time features
- **Centrifugo**: WebSocket server
- **Math Master App**: Spring Boot application

Các tác vụ:
- Start/Stop/Restart services
- View logs real-time
- Check container status
- View configuration

### 3️⃣ Database Management

- **Initialize**: Khởi tạo database
- **Wait for Ready**: Đợi DB sẵn sàng
- **Show Info**: Database information
- **Backup**: Backup PostgreSQL
- **Restore**: Restore từ backup file
- **Reset**: Xóa toàn bộ data (⚠️ dangerous)
- **Migration Status**: Hibernate migration info

**Note:** Math Master sử dụng Hibernate `ddl-auto: update` để tự động manage schema.

### 4️⃣ Health Checks

- Check tất cả services
- Check app health endpoint
- Check Redis connectivity
- Check database state
- Check Centrifugo status
- Wait for all services ready
- Service status overview

### 5️⃣ Environment Setup

- **Full Setup**: Recommended lần đầu tiên
- **Quick Verify**: Kiểm tra nhanh
- Check Java 21 installation
- Check Maven 3.6+ installation
- Check Docker installation
- Setup .env file từ template

### 6️⃣ Repository Management

- Check git status & log
- View application logs
- Browse log files

## 💻 Quick Start Guide

### Lần Đầu Tiên (5 phút)

```bash
cd scripts/ubuntu_scripts

# 1. Cấp quyền
chmod +x fr-manager.sh install.sh lib/*.sh config/*.sh

# 2. Chạy installer
./install.sh

# 3. Setup environment
./fr-manager.sh
# → Chọn: 5. Environment Setup → 1. Full Environment Setup

# 4. Start services
./fr-manager.sh
# → Chọn: 2. Docker Compose → 1. Start All Services

# 5. Wait for ready
./fr-manager.sh
# → Chọn: 4. Health Checks → 6. Wait for Services Ready

# 6. Build project
./fr-manager.sh
# → Chọn: 1. Build Operations → 1. Build Project
```

Sau đó, app chạy tại `http://localhost:8080`

### Development Workflow

**Bắt Đầu Làm Việc:**
```bash
fr                           # Start manager
# → 2. Docker Compose → 1. Start All Services
# → 4. Health Checks → 6. Wait for Services Ready
# → 1. Build Operations → 1. Build Project (optional)
```

**Trong Quá Trình Phát Triển:**
```bash
# Edit code in IDE
# App sẽ auto-reload via Spring DevTools
# Hoặc:
mvn clean package -DskipTests && java -jar target/math-master*.jar
```

**Trước Khi Commit:**
```bash
fr
# → 4. Health Checks → 1. Check All Services (verify all OK)
# → 3. Database Management → 4. Backup Database (backup for safety)
```

**Dừng Phát Triển:**
```bash
fr
# → 2. Docker Compose → 2. Stop All Services
```

## 🔧 Cấu Hình

### config/config.sh - Các Biến Chính

```bash
# ===== PROJECT =====
PROJECT_ROOT           # Tự động detect
PROJECT_NAME="math-master"
ENVIRONMENT="local"    # local, staging, production

# ===== JAVA/MAVEN =====
JAVA_VERSION="21"
MAVEN_EXECUTABLE="mvn"
BUILD_PROFILES="dev"
MAVEN_SKIP_TESTS=false

# ===== DOCKER =====
DOCKER_COMPOSE_FILE    # docker-compose.yml path
DOCKER_IMAGE_TAG="latest"

# ===== DATABASE =====
DATABASE_TYPE="postgresql"
DATABASE_PORT=5432

# ===== REDIS =====
REDIS_HOST="localhost"
REDIS_PORT="6379"
REDIS_PASSWORD="redis_password"

# ===== APPLICATION =====
APP_PORT="8080"
APP_HEALTH_ENDPOINT="/actuator/health"

# ===== CENTRIFUGO =====
CENTRIFUGO_PORT="8000"
CENTRIFUGO_CONFIG     # centrifugo-config.json path
```

### .env File

Create file `.env` trong project root (copy từ template nếu có):

```bash
# Redis Configuration
REDIS_PASSWORD=redis_password_prod_change_me

# JWT Configuration
JWT_SIGNERKEY=your_jwt_signer_key_here
JWT_VALID_DURATION=3600
JWT_REFRESHABLE_DURATION=36000

# PayOS Configuration (Payment Gateway)
PAYOS_CLIENT_ID=...
PAYOS_API_KEY=...
PAYOS_CHECKSUM_KEY=...
PAYOS_RETURN_URL=...
PAYOS_CANCEL_URL=...

# Admin Account
ADMIN_USERNAME=admin
ADMIN_PASSWORD=change_me_in_production
ADMIN_GMAIL=admin@mathmaster.com
ADMIN_FULLNAME=System Administrator

# Student Account
STUDENT_USERNAME=student
STUDENT_PASSWORD=change_me_in_production
STUDENT_GMAIL=student@mathmaster.com
STUDENT_FULLNAME=Student User

# Teacher Account  
TEACHER_USERNAME=teacher
TEACHER_PASSWORD=change_me_in_production
TEACHER_GMAIL=teacher@mathmaster.com
TEACHER_FULLNAME=Teacher User

# Centrifugo
CENTRIFUGO_WS_URL=ws://localhost:8000/connection/websocket
```

## 📊 Services Architecture

```
┌──────────────────────────────────────────────┐
│    Math Master Application (Spring Boot)    │
│         Java 21 + Spring Boot 3.5.0         │
│   http://localhost:8080                     │
│   Swagger: /swagger-ui.html                 │
│   Actuator: /actuator/health                │
└────────┬──────────────┬──────────────┬──────┘
         │              │              │
    ┌────▼────┐    ┌────▼─────┐   ┌──▼───────┐
    │  Redis  │    │Centrifugo │   │PostgreSQL│
    │:6379    │    │  :8000    │   │ :5432    │
    └─────────┘    └───────────┘   └──────────┘
   (Cache/RT)   (WebSocket/RT)   (Database)
```

**Key Technologies:**
- Spring Boot 3.5.0 + Java 21
- Spring Security + JWT Authentication
- PostgreSQL (Hibernate ORM)
- Redis (Caching + RT features)
- Centrifugo (WebSocket)
- Swagger/OpenAPI (Documentation)
- Maven (Build tool)
- Docker Compose (Local development)

## 🚨 Common Issues & Solutions

### "Java not found"
```bash
# Check installation
java -version

# If missing:
# macOS: brew install java@21
# Ubuntu: sudo apt install openjdk-21-jdk
# Windows: Download from oracle.com or use choco install openjdk21
```

### "Maven command not found"
```bash
# Check installation
mvn -version

# If missing:
# macOS: brew install maven
# Ubuntu: sudo apt install maven
# Download from: https://maven.apache.org/download.cgi
```

### "Docker not running"
```bash
# Start Docker Desktop application (macOS, Windows)
# Or on Linux:
sudo systemctl start docker
```

### "Port 8080 already in use"
```bash
# Kill process using port 8080
lsof -i :8080  # macOS/Linux - find PID
kill -9 <PID>

# Or change port in application.yaml:
# server.port: 8081
```

### "Cannot connect to database"
```bash
# Check container status
docker-compose ps

# View database logs
docker-compose logs postgres-math-master

# Or using manager:
fr → Database Management → Show Database Info
```

### "Services not ready after long wait"
```bash
# Check logs
docker-compose logs

# Kill and restart
docker-compose down
docker-compose up -d

# Or using manager:
fr → Docker Compose → Stop All Services
fr → Docker Compose → Start All Services
```

## 📚 API Documentation

Once application is running:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **Health Check**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/metrics (if enabled)

## 📝 Logs & Debugging

### View Application Logs

```bash
# Real-time logs
docker-compose logs -f app

# Or using manager:
fr → Docker Compose → 4. View Logs

# Or manually:
less logs/math-master-YYYYMMDD.log
```

### Enable Debug Logging

Edit `src/main/resources/application.yaml`:
```yaml
logging:
  level:
    root: INFO
    com.fptu.math_master: DEBUG
```

### Database Logs

```bash
docker-compose logs -f postgres-math-master
```

## 🤝 Development Tips

1. **Use Manager Scripts**: Không cần nhớ lệnh dài, chỉ cần chạy `fr`
2. **Enable Logging**: Tất cả operations được log, có thể review sau
3. **Backup Before Major Changes**: `fr → Database → Backup Database`
4. **Check Health Regularly**: `fr → Health Checks → Check All Services`
5. **Use Git Branches**: Luôn tạo branch trước khi làm thay đổi
6. **Follow Maven Conventions**: `src/main/java`, `src/main/resources`, `src/test/java`
7. **Keep Dependencies Updated**: Review `pom.xml` regularly

## 📄 Files Modified/Added

### Modified (from old FR Manager):
- `config/config.sh` - Adapted for Math Master
- `lib/builder.sh` - Maven-specific builds
- `fr-manager.sh` - Simplified for single repo
- `README.md` - Complete rewrite
- `install.sh` - Updated paths

### New Files:
- `lib/environment-setup.sh` - Environment verification
- `lib/health-checks.sh` - Service health monitoring
- `lib/database-migrations.sh` - Database management

### Unchanged (still compatible):
- `lib/colors.sh` - Terminal colors
- `lib/ui.sh` - UI utilities
- `lib/git_utils.sh` - Git operations
- `lib/status_checker.sh` - Status checking
- `lib/logger.sh` - Logging

## 🔄 Update Guide

If you update the scripts from git:

```bash
cd scripts/ubuntu_scripts
git pull

# Update permissions
chmod +x fr-manager.sh install.sh lib/*.sh config/*.sh

# Reinstall (optional)
./install.sh

# Verify
fr → 5. Environment Setup → 2. Quick Verification
```

## 📞 Support & Reporting Issues

1. **Check Logs**: `fr → 8. View Logs`
2. **Verify Setup**: `fr → 5. Environment Setup → 2. Quick Verification`
3. **Check Health**: `fr → 4. Health Checks → 7. Show Service Status`
4. **Manual Debugging**:
   ```bash
   docker-compose ps                    # Check containers
   docker-compose logs                  # Check logs
   curl http://localhost:8080/actuator/health  # Check app health
   ```

## 📊 Version Info

- **Manager Version**: 2.1 (Math Master Edition)
- **Target Project**: Math Master Backend
- **Java Version**: 21
- **Spring Boot Version**: 3.5.0
- **Maven Version**: 3.6+
- **Docker**: Latest stable
- **Docker Compose**: 1.29+

## 📄 License

Part of Math Master Teaching Support Platform

---

**Last Updated**: 2025-01-16  
**Maintainer**: Math Master DevOps Team

## 📁 Cấu Trúc Thư Mục

```
ubuntu_scripts/
├── fr-manager.sh              # Script chính
├── fr                         # Wrapper script (tạo tự động)
├── install.sh                 # Script cài đặt
├── README.md                  # File này
├── config/
│   └── config.sh              # File cấu hình (Math Master)
├── lib/
│   ├── colors.sh              # Định nghĩa màu sắc
│   ├── ui.sh                  # Giao diện người dùng
│   ├── git_utils.sh           # Thao tác Git
│   ├── status_checker.sh      # Kiểm tra trạng thái repo
│   ├── builder.sh             # Maven build operations
│   ├── logger.sh              # Logging utilities
│   ├── environment-setup.sh   # Kiểm tra & cấu hình môi trường
│   ├── health-checks.sh       # Kiểm tra sức khỏe services
│   └── database-migrations.sh # Quản lý database
└── logs/
    └── (log files)
```

## 🚀 Cài Đặt

### Bước 1: Cấp quyền execute cho scripts

```bash
cd scripts/ubuntu_scripts
chmod +x fr-manager.sh install.sh
chmod +x lib/*.sh
chmod +x config/*.sh
```

### Bước 2: Chạy script cài đặt

```bash
./install.sh
```

### Bước 3: Sử dụng script manager

```bash
./fr-manager.sh
# Hoặc nếu đã cài đặt:
fr
```

### Bước 4: Cấu hình

Mở file `config/config.sh` và cập nhật các thông tin:

```bash
# Đường dẫn tới thư mục chứa tất cả repos
PROJECT_ROOT="${HOME}/fwr"

# Nhánh mặc định
DEFAULT_BRANCH="release/v3.1"

# Đường dẫn formatter plugin (nếu dùng)
FORMATTER_PLUGIN_PATH="..."
```

## 💻 Sử Dụng

Sau khi cài đặt, chỉ cần gõ:

```bash
fr
```

## 📋 Các Tính Năng

### 1. Switch Environment & Build

Chuyển đổi tất cả repos liên quan đến một task (ML/AP/BT) sang cùng một nhánh và build.

**Task Types:**
- **ML** (Module Library): repos 2, 3, 4
- **AP** (API): repos 2, 3, 4, 5  
- **BT** (Batch): repos 2, 3, 4, 6

**Versions:**
- `3.1` → `release/v3.1`
- `3.0` → `develop/v3.0`
- `2.8.65` → `fix/v2.8.65` (với fallback `fix/v2.8.60` nếu không tìm thấy)

**Flow:**
1. Kiểm tra trạng thái tất cả repos
2. Hiển thị các conflicts/uncommitted changes
3. Xin xác nhận từ người dùng
4. Checkout tất cả repos sang nhánh mong muốn
5. Build theo thứ tự: ILC modules → WES modules → API/Batch

### 2. Build Single Module

Build một module riêng lẻ mà không cần checkout hay build toàn bộ.

### 3. Build VRF Framework

Build VRF framework (chỉ cần build 1 lần).

Versions:
- `v2.0.0`
- `v1.4.25`

### 4. Apply Formatter

Format code sử dụng Prettier và prettier-plugin-java.

**Options:**
- Format một module cụ thể
- Format tất cả modules
- Check formatting (không thay đổi file)
- Format với backup (tạo stash trước)

### 5. Check Repository Status

Kiểm tra trạng thái của các repositories:
- Current branch
- Uncommitted changes
- Unpushed commits
- Sync status với remote

### 6. Cleanup Local Branches

Xóa tất cả local branches của một repo (trừ default branch).

**Options:**
- Cleanup một repo cụ thể
- Cleanup tất cả repos
- Cleanup merged branches only
- Cleanup stashes
- Cleanup untracked files

### 7. View Logs

Xem logs của các operations đã thực hiện:
- Conflicts và cách xử lý
- Stashes được tạo
- Build logs
- Checkout history

### 8. Configuration

Cấu hình các settings của FR Manager.

## 🔧 Cấu Hình Chi Tiết

### config/config.sh

```bash
# ===== PATHS =====
PROJECT_ROOT="${HOME}/fwr"
REPO_VRF="${PROJECT_ROOT}/fr-vrf-backend"
REPO_FLYWAY="${PROJECT_ROOT}/fr-wes-flyway"
REPO_ILC_MODULES="${PROJECT_ROOT}/fr-ilc-backend-modules"
REPO_WES_MODULES="${PROJECT_ROOT}/fr-wes-backend-modules"
REPO_WES_API="${PROJECT_ROOT}/fr-wes-onln-backend"
REPO_WES_BATCH="${PROJECT_ROOT}/fr-wes-batch"

# ===== BRANCHES =====
DEFAULT_BRANCH="release/v3.1"

# ===== BUILD SCRIPTS =====
BUILD_SCRIPT="./scripts/build.sh"
DEPLOY_SCRIPT="./scripts/deploy-on-local.sh"

# ===== LOGGING =====
LOG_DIR="${SCRIPT_DIR}/../logs"
ENABLE_LOGGING=true

# ===== CONFLICT HANDLING =====
DEFAULT_CONFLICT_ACTION="force"  # force, stash, abort
AUTO_STASH_ON_CONFLICT=true
STASH_PREFIX="fr-manager-auto"

# ===== BRANCH FALLBACK =====
ENABLE_BRANCH_FALLBACK=true  # fix/v2.8.65 → fix/v2.8.60
```

## 📝 Xử Lý Conflicts

Khi có conflicts (uncommitted changes, local commits khác remote), FR Manager sẽ:

1. **Hiển thị trạng thái chi tiết** của tất cả repos
2. **Đợi xác nhận** từ người dùng
3. **Đưa ra các lựa chọn:**
   - `force`: Discard changes và force checkout
   - `stash`: Tạo stash tự động (với tên theo timestamp)
   - `abort`: Hủy operation

### Auto Stash

Nếu chọn stash, FR Manager sẽ:
- Tạo stash với tên: `fr-manager-auto-<branch>-<timestamp>`
- Log vào file để theo dõi
- Tiếp tục với checkout

## 📊 Logging

Tất cả operations quan trọng được log vào `logs/`:

- `fr-manager-YYYYMMDD.log`: Log chính
- `conflicts-YYYYMMDD.log`: Conflicts và cách xử lý
- `stashes-YYYYMMDD.log`: Stashes được tạo

**View logs:**
```bash
fr  # Chọn option 7
```

## 🎨 Code Formatting

### Yêu Cầu

```bash
npm install -g prettier prettier-plugin-java
```

### Sử Dụng

FR Manager sẽ format tất cả file `.java` và `.json` trong các thư mục `src/`.

## 🔄 Workflow Điển Hình

### Scenario 1: Chuyển sang làm task mới

```bash
fr
# Chọn: 1. Switch Environment & Build
# Task type: AP
# Version: 2.8.65

# FR Manager sẽ:
# 1. Show status của repos 2,3,4,5
# 2. Xin confirm
# 3. Checkout tất cả sang fix/v2.8.65 (hoặc fix/v2.8.60)
# 4. Build: ILC → WES → API
```

### Scenario 2: Build lại một module

```bash
fr
# Chọn: 2. Build Single Module
# Chọn module cần build
```

### Scenario 3: Format code trước khi commit

```bash
fr
# Chọn: 4. Apply Formatter
# Chọn: 1. Format specific module
# Chọn module
```

### Scenario 4: Dọn dẹp branches cũ

```bash
fr
# Chọn: 6. Cleanup Local Branches
# Chọn repo hoặc All
```

## 🛠️ Troubleshooting

### Lỗi: "Branch not found"

- Kiểm tra xem branch có tồn tại trên remote không
- FR Manager sẽ tự động thử fallback branch (cho fix/v2.x.y)

### Lỗi: "Build failed"

- Check logs trong `logs/`
- Build manual để xem error chi tiết
- Đảm bảo các dependencies đã được build

### Lỗi: "Formatter not found"

```bash
npm install -g prettier prettier-plugin-java
```

Sau đó update `FORMATTER_PLUGIN_PATH` trong `config/config.sh`

## 📌 Tips

1. **Luôn check status** trước khi switch: Option 5
2. **Enable logging** để track các thay đổi
3. **Sử dụng stash** thay vì force nếu có uncommitted changes quan trọng
4. **Cleanup branches** định kỳ để giữ workspace sạch sẽ
5. **Format code** trước khi commit

## 🔐 Safety Features

- ✅ Confirmation trước khi force/delete
- ✅ Auto stash với timestamp
- ✅ Comprehensive logging
- ✅ Status check trước mọi operation
- ✅ Rollback capability (via stash)
- ✅ Branch fallback mechanism

## 📞 Support

Nếu gặp vấn đề:
1. Check logs: `fr` → Option 7
2. Review config: `config/config.sh`
3. Run với verbose mode (coming soon)

## 🚧 Future Enhancements

- [ ] Parallel builds
- [ ] Custom build profiles
- [ ] Git hooks integration
- [ ] Interactive branch selection
- [ ] Auto dependency resolution
- [ ] Configuration wizard
- [ ] Rollback specific operations

---

**Version:** 2.0  
**Last Updated:** 2025-01-16
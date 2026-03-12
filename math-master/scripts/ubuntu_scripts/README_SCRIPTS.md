# 📖 Hướng Dẫn Sử Dụng Math Master Project Manager Scripts

> Công cụ quản lý dự án Math Master backend (Spring Boot 3.5.0 + Java 21) với Docker Compose, Maven build, database management, và service health checks.

## 📑 Mục Lục

1. [🚀 Cài Đặt Nhanh](#cài-đặt-nhanh)
2. [📋 Cấu Trúc Script](#cấu-trúc-script)
3. [💻 Hướng Dẫn Sử Dụng](#hướng-dẫn-sử-dụng)
4. [🎯 Các Tác Vụ Phổ Biến](#các-tác-vụ-phổ-biến)
5. [🔧 Cấu Hình Chi Tiết](#cấu-hình-chi-tiết)
6. [🚨 Khắc Phục Sự Cố](#khắc-phục-sự-cố)
7. [🎓 Ví Dụ Nâng Cao](#ví-dụ-nâng-cao)
8. [📚 Tham Khảo](#tham-khảo)

---

## 🚀 Cài Đặt Nhanh

### Yêu Cầu Hệ Thống

- **OS**: Linux, macOS hoặc Windows (WSL2)
- **Java**: 21 trở lên
- **Maven**: 3.6+
- **Docker**: 20.10+
- **Docker Compose**: 1.29+

### Bước 1: Cấp Quyền Cho Scripts

```bash
cd scripts/ubuntu_scripts

# Cấp quyền execute
chmod +x fr-manager.sh install.sh
chmod +x lib/*.sh
chmod +x config/*.sh
```

### Bước 2: Chạy Installer

```bash
./install.sh
```

Installer sẽ:
- ✓ Tạo wrapper script `khoipd_terminal`
- ✓ Thêm alias vào shell RC file (`.bashrc`, `.zshrc`)
- ✓ Kiểm tra prerequisites (Java, Maven, Docker)
- ✓ Hiển thị hướng dẫn tiếp theo

### Bước 3: Reload Shell Hoặc Mở Terminal Mới

```bash
source ~/.bashrc   # Hoặc ~/.zshrc nếu dùng zsh
```

### Bước 4: Xác Minh Cài Đặt

```bash
khoipd_terminal

# Hoặc chạy trực tiếp
./fr-manager.sh
```

---

## 📋 Cấu Trúc Script

### Tệp Tin Chính

```
scripts/ubuntu_scripts/
├── fr-manager.sh              # Script quản lý chính
├── khoipd_terminal            # Wrapper script (được tạo bởi installer)
├── install.sh                 # Script cài đặt
├── README.md                  # Tài liệu dự án
├── README_SCRIPTS.md          # File này - Hướng dẫn chi tiết
│
├── config/                    # Thư mục cấu hình
│   └── config.sh              # Các biến cấu hình (chỉnh sửa tại đây)
│
├── lib/                       # Thư mục các thư viện script
│   ├── colors.sh              # Định nghĩa màu sắc cho output
│   ├── ui.sh                  # Các hàm giao diện người dùng
│   ├── git_utils.sh           # Các tiện ích Git
│   ├── status_checker.sh      # Kiểm tra trạng thái repository
│   ├── builder.sh             # Các hàm build Maven
│   ├── logger.sh              # Logging utilities
│   ├── environment-setup.sh   # Setup & verify môi trường
│   ├── health-checks.sh       # Kiểm tra sức khỏe services
│   └── database-migrations.sh # Quản lý database
│
└── logs/                      # Thư mục logs (tự động tạo)
    └── *.log                  # Log files từ các operations
```

### Mô Tả Các File Lib

| File | Chức Năng |
|------|----------|
| **colors.sh** | Định nghĩa màu ANSI cho terminal output (RED, GREEN, BLUE, etc.) |
| **ui.sh** | Hàm UI: title(), opt(), ask(), warn(), err(), success() |
| **git_utils.sh** | Git operations: check_git_status(), git_log_recent() |
| **status_checker.sh** | Kiểm tra git status, uncommitted changes, unpushed commits |
| **builder.sh** | Maven build: build_project(), build_docker(), run_tests() |
| **logger.sh** | Log functions: log_info(), log_error(), log_success() |
| **environment-setup.sh** | Environment checks & setup: check_java(), check_docker() |
| **health-checks.sh** | Service health: check_app_health(), check_redis_health() |
| **database-migrations.sh** | Database ops: backup_database(), restore_database() |

---

## 💻 Hướng Dẫn Sử Dụng

### Cách Chạy Manager

```bash
# Cách 1: Sử dụng alias (sau khi cài đặt)
khoipd_terminal

# Cách 2: Chạy trực tiếp
cd scripts/ubuntu_scripts
./fr-manager.sh

# Cách 3: Chạy từ folder ngoài (không cần cd)
cd /path/to/project
/path/to/scripts/ubuntu_scripts/khoipd_terminal
```

### Giao Diện Menu Chính

Khi chạy `khoipd_terminal`, bạn sẽ thấy menu chính:

```
╔═══════════════════════════════════════════════════════════╗
║                                                           ║
║              MATH MASTER PROJECT MANAGER                  ║
║                                                           ║
╚═══════════════════════════════════════════════════════════╝

1. Build Operations          # Các tác vụ build Maven
2. Docker Compose           # Quản lý Docker services
3. Database Management      # Quản lý database
4. Health Checks            # Kiểm tra sức khỏe services
5. Environment Setup        # Setup & verify môi trường
6. Repository Status        # Kiểm tra git status
7. Print Configuration      # Hiển thị cấu hình hiện tại
8. View Logs               # Xem log files
9. Exit                     # Thoát

Select option (1-9): 
```

---

## 🎯 Các Tác Vụ Phổ Biến

### ⚙️ Tác Vụ 1: Setup Môi Trường Lần Đầu

**Mục đích**: Kiểm tra & cấu hình môi trường development

**Các bước**:

```
1. Chạy: khoipd_terminal
2. Chọn: 5. Environment Setup
3. Chọn: 1. Full Environment Setup
```

Điều này sẽ:
- ✓ Kiểm tra Java 21 installation
- ✓ Kiểm tra Maven 3.6+ installation  
- ✓ Kiểm tra Docker & Docker Compose installation
- ✓ Tạo `.env` file từ template (nếu không có)
- ✓ Setup Git hooks (nếu cần)

**Output mong đợi**:
```
Java: OK (Java 21)
Maven: OK (Maven 3.9.9)
Docker: OK (Docker 20.10.21)
Docker Compose: OK (Docker Compose 2.0.0)
✓ All critical components are ready!
```

### 🐳 Tác Vụ 2: Khởi Động Services (Docker Compose)

**Mục đích**: Khởi động tất cả services (PostgreSQL, Redis, Centrifugo, App)

**Các bước**:

```
1. Chạy: khoipd_terminal
2. Chọn: 2. Docker Compose
3. Chọn: 1. Start All Services
```

**Nó sẽ khởi động**:
- 🐘 PostgreSQL (port 5432)
- 🔴 Redis (port 6379)
- 📡 Centrifugo (port 8000)
- 🚀 Math Master App (port 8080)

**Xác minh**:
```bash
# Kiểm tra các container
docker ps | grep math-master

# Hoặc dùng manager
khoipd_terminal → 2. Docker Compose → 5. Check Service Status
```

### ✔️ Tác Vụ 3: Chờ Services Sẵn Sàng

**Mục đích**: Đợi tất cả services healthy & ready

**Các bước**:

```
1. Chạy: khoipd_terminal
2. Chọn: 4. Health Checks
3. Chọn: 6. Wait for Services Ready
```

**Nó sẽ kiểm tra**:
- ✓ App health endpoint (http://localhost:8080/actuator/health)
- ✓ Redis connectivity
- ✓ Database state
- ✓ Centrifugo availability

**Timeout**: Mặc định 120 giây

### 🏗️ Tác Vụ 4: Build Project

**Mục đích**: Compile, test, và package ứng dụng

**Các bước**:

```
1. Chạy: khoipd_terminal
2. Chọn: 1. Build Operations
3. Chọn: 1. Build Project
```

**Lệnh thực tế**:
```bash
cd ${PROJECT_ROOT}
mvn clean package -P dev -DskipTests=false
```

**Tùy chọn**:
- Skip tests: `MAVEN_SKIP_TESTS=true` trong config.sh
- Custom profile: `BUILD_PROFILES="prod"` trong config.sh

### 🏃 Tác Vụ 5: Build Nhanh (Skip Tests)

**Mục đích**: Build nhanh mà không chạy tests

**Các bước**:

```
1. Chạy: khoipd_terminal
2. Chọn: 1. Build Operations
3. Chọn: 3. Compile Only
```

**Tốc độ**: ~30-60 giây (vs 3-5 phút với tests)

### 🧪 Tác Vụ 6: Chạy Tests

**Mục đích**: Chạy test suite

**Các bước**:

```
1. Chạy: khoipd_terminal
2. Chọn: 1. Build Operations
3. Chọn: 4. Run Tests
```

### 🐳 Tác Vụ 7: Build Docker Image

**Mục đích**: Tạo Docker image cho deployment

**Các bước**:

```
1. Chạy: khoipd_terminal
2. Chọn: 1. Build Operations
3. Chọn: 2. Build Docker Image
```

**Image được tạo**:
```
math-master:latest
```

**Kiểm tra**:
```bash
docker images | grep math-master
```

### 💾 Tác Vụ 8: Backup Database

**Mục đích**: Backup PostgreSQL trước khi làm thay đổi lớn

**Các bước**:

```
1. Chạy: khoipd_terminal
2. Chọn: 3. Database Management
3. Chọn: 4. Backup Database
```

**Backup file sẽ được lưu tại**:
```
${PROJECT_ROOT}/backups/math-master-backup-20250313_143022.sql
```

### 🔄 Tác Vụ 9: Restore Database

**Mục đích**: Restore database từ backup file

**Các bước**:

```
1. Chạy: khoipd_terminal
2. Chọn: 3. Database Management
3. Chọn: 5. Restore Database
4. Chọn backup file từ danh sách
5. Confirm restore (type 'yes')
```

⚠️ **Cảnh báo**: Thao tác này sẽ ghi đè toàn bộ data hiện tại

### 🔍 Tác Vụ 10: Kiểm Tra Sức Khỏe Services

**Mục đích**: Kiểm tra tất cả services đang hoạt động bình thường

**Các bước**:

```
1. Chạy: khoipd_terminal
2. Chọn: 4. Health Checks
3. Chọn: 1. Check All Services
```

**Sẽ kiểm tra**:
- ✓ Application health
- ✓ Redis connectivity  
- ✓ Database state
- ✓ Centrifugo availability

**Output**:
```
✓ Application is healthy (HTTP 200)
✓ Redis is healthy (PONG)
✓ PostgreSQL is accepting connections
✓ Centrifugo is healthy
✓ All services are healthy!
```

### 📊 Tác Vụ 11: Xem Log Services

**Mục đích**: Xem realtime logs từ containers

**Các bước**:

```
1. Chạy: khoipd_terminal
2. Chọn: 2. Docker Compose
3. Chọn: 4. View Logs
```

**Thoát**: Nhấn `Ctrl+C`

### 🛑 Tác Vụ 12: Dừng Services

**Mục đích**: Dừng tất cả services

**Các bước**:

```
1. Chạy: khoipd_terminal
2. Chọn: 2. Docker Compose
3. Chọn: 2. Stop All Services
```

---

## 🔧 Cấu Hình Chi Tiết

### File Cấu Hình Chính: `config/config.sh`

#### Project Configuration

```bash
# Project root directory (tự động detect)
PROJECT_ROOT=$(cd "$(dirname "${SCRIPT_DIR}")/../../" && pwd)

# Project name
PROJECT_NAME="math-master"

# Environment type: local, staging, production
ENVIRONMENT="local"
```

#### Java & Maven Configuration

```bash
# Java version (tương ứng với pom.xml)
JAVA_VERSION="21"

# Maven executable (mặc định: mvn)
MAVEN_EXECUTABLE="mvn"

# Maven profiles
BUILD_PROFILES="dev"  # Có thể: dev, prod, staging

# Skip tests khi build (true/false)
MAVEN_SKIP_TESTS=false

# Build timeout (seconds)
MAVEN_BUILD_TIMEOUT=600
```

#### Docker Configuration

```bash
# Docker Compose file location
DOCKER_COMPOSE_FILE="${PROJECT_ROOT}/docker-compose.yml"

# Docker image tag
DOCKER_IMAGE_TAG="latest"

# Environment file (.env)
ENV_FILE="${PROJECT_ROOT}/.env"
ENV_EXAMPLE_FILE="${PROJECT_ROOT}/.env.example"
```

#### Service Configuration

```bash
# Database
DATABASE_TYPE="postgresql"
DATABASE_PORT=5432

# Redis
REDIS_HOST="localhost"
REDIS_PORT=6379
REDIS_PASSWORD="redis_password"

# Application
APP_PORT=8080
APP_HEALTH_ENDPOINT="/actuator/health"

# Centrifugo
CENTRIFUGO_PORT=8000
CENTRIFUGO_CONFIG="${PROJECT_ROOT}/centrifugo-config.json"
```

#### Logging Configuration

```bash
# Log directory
LOG_DIR="${SCRIPT_DIR}/logs"

# Enable/disable logging
ENABLE_LOGGING=true
```

### Thay Đổi Cấu Hình

Để thay đổi cấu hình, chỉnh sửa file `config/config.sh`:

```bash
# Mở file để chỉnh sửa
nano config/config.sh

# Hoặc dùng editor khác
vim config/config.sh
code config/config.sh
```

**Ví dụ**: Thay đổi Maven profile

```bash
# Trước
BUILD_PROFILES="dev"

# Sau (để build cho production)
BUILD_PROFILES="prod"
```

---

## 🚨 Khắc Phục Sự Cố

### ❌ Lỗi: "Java not found"

**Nguyên nhân**: Java không được cài đặt hoặc không trong PATH

**Giải pháp**:

```bash
# Kiểm tra Java
java -version

# Không có? Cài đặt Java 21
# macOS
brew install java@21

# Ubuntu/Debian
sudo apt install openjdk-21-jdk

# Thêm vào PATH (nếu cần)
export PATH=/usr/lib/jvm/java-21-openjdk/bin:$PATH
```

### ❌ Lỗi: "Maven command not found"

**Nguyên nhân**: Maven không được cài đặt

**Giải pháp**:

```bash
# Kiểm tra Maven
mvn -version

# Cài đặt Maven
# macOS
brew install maven

# Ubuntu/Debian
sudo apt install maven

# Hoặc download từ: https://maven.apache.org/download.cgi
```

### ❌ Lỗi: "Docker is not running"

**Nguyên nhân**: Docker daemon không chạy

**Giải pháp**:

```bash
# Start Docker Desktop (macOS, Windows)
# Hoặc trên Linux
sudo systemctl start docker

# Verify
docker ps
```

### ❌ Lỗi: "Port 8080 already in use"

**Nguyên nhân**: Một application khác đang sử dụng port 8080

**Giải pháp**:

```bash
# Tìm process sử dụng port
lsof -i :8080  # macOS/Linux
netstat -ano | findstr :8080  # Windows

# Kill process
kill -9 <PID>

# Hoặc thay đổi port trong application.yaml
# server.port: 8081
```

### ❌ Lỗi: "Cannot connect to database"

**Nguyên nhân**: PostgreSQL container không chạy hoặc connection failed

**Giải pháp**:

```bash
# Kiểm tra container status
docker ps | grep postgres

# Xem logs
docker logs math-master-postgres

# Restart container
docker restart math-master-postgres

# Hoặc dùng manager
khoipd_terminal → 3. Database Management → Show Database Info
```

### ❌ Lỗi: "Health checks fail"

**Nguyên nhân**: Một hoặc nhiều services không healthy

**Giải pháp**:

```bash
# Kiểm tra từng service
khoipd_terminal → 4. Health Checks → 2. Check Application Health
khoipd_terminal → 4. Health Checks → 3. Check Redis Health
khoipd_terminal → 4. Health Checks → 4. Check Database Health

# Hoặc kiểm tra logs
khoipd_terminal → 2. Docker Compose → 4. View Logs

# Hoặc restart services
khoipd_terminal → 2. Docker Compose → 2. Stop All Services
khoipd_terminal → 2. Docker Compose → 1. Start All Services
```

### ❌ Lỗi: "mvn: command not found" (WSL2)

**Nguyên nhân**: Maven không được thêm vào PATH trong WSL2

**Giải pháp**:

```bash
# Thêm vào ~/.bashrc
echo 'export PATH=$PATH:/usr/bin/mvn' >> ~/.bashrc
source ~/.bashrc

# Hoặc cài đặt lại Maven
sudo apt remove maven
sudo apt install maven
```

### ❌ Lỗi: Build timeout

**Nguyên nhân**: Build vượt quá timeout limit (mặc định 600 giây)

**Giải pháp**:

```bash
# Tăng timeout trong config.sh
MAVEN_BUILD_TIMEOUT=1200  # 20 phút

# Hoặc skip tests (nhanh hơn)
MAVEN_SKIP_TESTS=true

# Build lại
khoipd_terminal → 1. Build Operations → 1. Build Project
```

---

## 🎓 Ví Dụ Nâng Cao

### 1️⃣ Development Workflow Hoàn Chỉnh

```bash
# Bước 1: Cài đặt & setup environment
cd scripts/ubuntu_scripts
./install.sh
khoipd_terminal → 5. Environment Setup → 1. Full Environment Setup

# Bước 2: Khởi động services
khoipd_terminal → 2. Docker Compose → 1. Start All Services
khoipd_terminal → 4. Health Checks → 6. Wait for Services Ready

# Bước 3: Build project
khoipd_terminal → 1. Build Operations → 1. Build Project

# Bước 4: Chạy ứng dụng
java -jar target/math-master-0.0.1-SNAPSHOT.jar

# Hoặc chạy từ IDE (Spring DevTools sẽ auto-reload)
# IDE sẽ tự động restart app khi bạn thay đổi code

# Bước 5: Dừng khi xong
khoipd_terminal → 2. Docker Compose → 2. Stop All Services
```

### 2️⃣ CI/CD Pipeline

```bash
#!/bin/bash
# Build script for CI/CD

cd scripts/ubuntu_scripts

# Setup
export MAVEN_SKIP_TESTS=false
export BUILD_PROFILES="prod"

# Full deployment
./fr-manager.sh <<EOF
1
1
9
EOF

# Check exit code
if [ $? -eq 0 ]; then
  echo "✓ Build successful"
  exit 0
else
  echo "✗ Build failed"
  exit 1
fi
```

### 3️⃣ Database Backup & Restore Pipeline

```bash
#!/bin/bash
# Backup trước khi deploy

# Start services
docker-compose up -d

# Wait for DB
sleep 10

# Backup
khoipd_terminal → 3. Database Management → 4. Backup Database

# Get backup file
BACKUP_FILE=$(ls -t backups/*.sql | head -1)

# Upload to storage
aws s3 cp "$BACKUP_FILE" s3://my-backup-bucket/math-master/

# Deploy
docker-compose pull
docker-compose up -d

# Check health
khoipd_terminal → 4. Health Checks → 1. Check All Services
```

### 4️⃣ Custom Build Profiles

Để tạo custom build profile:

**File**: `pom.xml`
```xml
<profiles>
  <profile>
    <id>dev</id>
    <properties>
      <logging.level>DEBUG</logging.level>
    </properties>
  </profile>
  
  <profile>
    <id>prod</id>
    <properties>
      <logging.level>INFO</logging.level>
    </properties>
  </profile>
</profiles>
```

**File**: `config/config.sh`
```bash
# Build env
BUILD_PROFILES="prod"
MAVEN_SKIP_TESTS=true
```

### 5️⃣ Logging & Debugging

**View application logs**:
```bash
# Real-time logs
docker-compose logs -f app

# Last 100 lines
docker-compose logs --tail=100 app

# Hoặc dùng manager
khoipd_terminal → 2. Docker Compose → 4. View Logs
```

**Enable debug logging**:

Edit `src/main/resources/application.yaml`:
```yaml
logging:
  level:
    root: INFO
    com.fptu.math_master: DEBUG
    org.springframework: DEBUG
```

**View script logs**:
```bash
# View manager operation logs
less logs/math-master-20250313.log

# Hoặc dùng manager
khoipd_terminal → 8. View Logs
```

---

## 📚 Tham Khảo

### Tài Liệu Chính Thức

- **Spring Boot**: https://spring.io/projects/spring-boot
- **Maven**: https://maven.apache.org/
- **Docker**: https://docs.docker.com/
- **PostgreSQL**: https://www.postgresql.org/docs/
- **Redis**: https://redis.io/documentation
- **Centrifugo**: https://centrifugo.dev/

### API Documentation

Khi app chạy:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **Health Check**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/metrics

### Available Endpoints

```bash
# Health check
curl http://localhost:8080/actuator/health

# Swagger UI
open http://localhost:8080/swagger-ui.html

# Metrics
curl http://localhost:8080/actuator/metrics

# Database
psql -h localhost -U postgres -d neondb

# Redis
redis-cli -h localhost -p 6379

# Centrifugo
curl http://localhost:8000/health
```

### Các Lệnh Hữu Ích

```bash
# List running containers
docker-compose ps

# View container logs
docker-compose logs <service_name>

# Access PostgreSQL
docker exec -it math-master-postgres psql -U postgres

# Clear Docker
docker-compose down -v  # Remove volumes too

# Maven skip tests
mvn clean package -DskipTests

# Maven without Docker
mvn clean install -P dev
```

### Environment Variables

**File**: `.env`

```bash
# Redis
REDIS_PASSWORD=redis_password_prod_change_me

# JWT
JWT_SIGNERKEY=your_jwt_signer_key_here

# PayOS (Payment Gateway)
PAYOS_CLIENT_ID=...
PAYOS_API_KEY=...

# Admin Account
ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin_password

# Student Account
STUDENT_USERNAME=student
STUDENT_PASSWORD=student_password

# Teacher Account
TEACHER_USERNAME=teacher
TEACHER_PASSWORD=teacher_password
```

---

## 💡 Tips & Best Practices

### ✅ Do's

- ✓ Chạy `khoipd_terminal → 5. Environment Setup → 1. Full Environment Setup` lần đầu
- ✓ Backup database trước khi làm thay đổi lớn
- ✓ Kiểm tra health checks thường xuyên
- ✓ Xem logs khi có lỗi
- ✓ Sử dụng `Compile Only` để test nhanh
- ✓ Keep scripts updated from git

### ❌ Don'ts

- ✗ Không edit scripts khi chúng đang chạy
- ✗ Không sử dụng `sudo` cho Docker (dùng user group)
- ✗ Không reset database khi không chắc chắn
- ✗ Không commit `.env` file (chứa sensitive data)
- ✗ Không bỏ qua environment setup

### 🎯 Performance Tips

```bash
# Nhanh hơn: Skip tests
MAVEN_SKIP_TESTS=true

# Nhanh hơn: Compile only
khoipd_terminal → 1. Build Operations → 3. Compile Only

# Nhanh hơn: Parallel build
mvn clean package -T 1C -DskipTests

# Nhanh hơn: Offline mode
mvn -o clean package
```

---

## 📞 Support & Contributing

### Báo Cáo Vấn Đề

```bash
# Step 1: Collect logs
khoipd_terminal → 8. View Logs

# Step 2: Check health
khoipd_terminal → 4. Health Checks → 7. Show Service Status

# Step 3: Save environment info
java -version
mvn -version
docker --version
docker-compose --version

# Step 4: Report with logs attached
# Create issue on GitHub/GitLab with:
# - Error message
# - Logs directory
# - Environment info
```

### Improvement Suggestions

Scripts được duy trì tại: `scripts/ubuntu_scripts/`

Có thể contribute bằng:
- Bug reports
- Feature requests
- Documentation improvements
- Performance optimizations

---

## 📝 Changelog

### Version 2.1 (Math Master Edition - 2025-01-16)

- ✨ New: Environment Setup menu with full checks
- ✨ New: Database Management menu with backup/restore
- ✨ New: Health Checks menu for all services
- 🔧 Improved: Auto-detect PROJECT_ROOT
- 🔧 Improved: Maven build logging
- 📝 Updated: Comprehensive documentation

### Version 2.0 (Adapted from FR Manager - 2025-01-10)

- 🔄 Adapted multi-repo manager to single-repo
- ✓ Added Docker Compose support
- ✓ Added database management
- ✓ Added service health checks

---

## 📄 License

Part of **Math Master Teaching Support Platform**

**Maintained by**: Math Master DevOps Team  
**Last Updated**: 2025-01-16  
**Version**: 2.1

---

## 🎉 Bắt Đầu Ngay

```bash
# Quick start
cd scripts/ubuntu_scripts
chmod +x *.sh lib/*.sh config/*.sh
./install.sh

# First setup
khoipd_terminal → 5. Environment Setup → 1. Full Environment Setup

# Start services
khoipd_terminal → 2. Docker Compose → 1. Start All Services

# Happy coding! 🚀
```

---

**Có câu hỏi?** Xem [README.md](./README.md) để biết thêm chi tiết.

**Tìm lỗi?** Kiểm tra [🚨 Khắc Phục Sự Cố](#khắc-phục-sự-cố) section.

**Cần giúp?** Xem logs: `khoipd_terminal → 8. View Logs`

Chúc bạn phát triển vui vẻ! 🎈

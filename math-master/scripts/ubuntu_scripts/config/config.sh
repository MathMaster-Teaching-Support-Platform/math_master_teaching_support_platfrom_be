#!/bin/bash
# config.sh - Configuration file for Math Master Project
# All configurable variables are defined here

# ================= PROJECT PATHS =================
# Get the project root (go up 3 levels from scripts/ubuntu_scripts)
PROJECT_ROOT="$(cd "$(dirname "${SCRIPT_DIR}")/../../" && pwd)"
PROJECT_NAME="math-master"

# Main scripts directory
MAIN_SCRIPTS_DIR="${PROJECT_ROOT}/scripts"

# ================= JAVA BUILD CONFIGURATION =================
JAVA_VERSION="21"
MAVEN_HOME="${MAVEN_HOME:-}"
MAVEN_EXECUTABLE="mvn"
BUILD_PROFILES="dev"  # Can be: dev, prod, staging

# Maven build options
MAVEN_SKIP_TESTS=false
MAVEN_BUILD_TIMEOUT=600  # In seconds
MAVEN_CLEAN_BUILD=false

# ================= DOCKER CONFIGURATION =================
DOCKER_COMPOSE_FILE="${PROJECT_ROOT}/docker-compose.yml"
DOCKER_IMAGE_BUILD=true
DOCKER_REGISTRY=""  # e.g., docker.io/myusername
DOCKER_IMAGE_TAG="latest"

# Environment for docker-compose
ENV_FILE="${PROJECT_ROOT}/.env"
ENV_EXAMPLE_FILE="${PROJECT_ROOT}/.env.example"

# ================= DATABASE CONFIGURATION =================
DATABASE_TYPE="postgresql"
DATABASE_SERVICE_NAME="postgres-math-master"
DATABASE_PORT="${DB_PORT:-5432}"
DATABASE_MIGRATION_TIMEOUT=120

# ================= REDIS CONFIGURATION =================
REDIS_SERVICE_NAME="math-master-redis"
REDIS_HOST="${SPRING_DATA_REDIS_HOST:-localhost}"
REDIS_PORT="${SPRING_DATA_REDIS_PORT:-6379}"
REDIS_PASSWORD="${REDIS_PASSWORD:-redis_password}"

# ================= APPLICATION CONFIGURATION =================
APP_NAME="math-master-app"
APP_PORT="${SERVER_PORT:-8080}"
APP_HEALTH_ENDPOINT="/actuator/health"
APP_STARTUP_TIMEOUT=60

# ================= CENTRIFUGO CONFIGURATION =================
CENTRIFUGO_SERVICE_NAME="math-master-centrifugo"
CENTRIFUGO_HOST="localhost"
CENTRIFUGO_PORT="${CENTRIFUGO_PORT:-8000}"
CENTRIFUGO_CONFIG="${PROJECT_ROOT}/centrifugo-config.json"

# ================= LOGGING CONFIGURATION =================
LOG_DIR="${SCRIPT_DIR}/logs"
LOG_FILE="${LOG_DIR}/math-master-$(date +%Y%m%d).log"
ENABLE_LOGGING=true

# ================= GIT CONFIGURATION =================
DEFAULT_BRANCH="main"
GIT_FETCH_TIMEOUT=30
GIT_PULL_TIMEOUT=60
AUTO_PULL_ON_START=false

# ================= UI CONFIGURATION =================
SHOW_DETAILED_STATUS=true
USE_COLOR_OUTPUT=true

# ================= DEVELOPMENT ENVIRONMENT =================
# Environment type: local, staging, production
ENVIRONMENT="${ENVIRONMENT:-local}"

# Code formatting tools
ENABLE_CODE_FORMATTING=true
FORMATTING_TOOL="prettier"  # Can be: prettier, spotless

# ================= DIRECTORY STRUCTURE =================
CONFIG_DIR="${SCRIPT_DIR}/../config"
LOGS_DIR="${SCRIPT_DIR}/logs"
TEMP_DIR="/tmp/math-master"
# ================= FUNCTION: Reset Proxy =================
reset_proxy() {
  unset http_proxy 2>/dev/null || true
  unset https_proxy 2>/dev/null || true
  unset HTTP_PROXY 2>/dev/null || true
  unset HTTPS_PROXY 2>/dev/null || true
  unset NO_PROXY 2>/dev/null || true
  unset no_proxy 2>/dev/null || true
}

# ================= FUNCTION: Print Configuration =================
print_config() {
  echo -e "${CYAN}Current Configuration - Math Master Project:${NC}"
  echo -e "${GRAY}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo -e "  Project Root:         ${GREEN}${PROJECT_ROOT}${NC}"
  echo -e "  Project Name:         ${GREEN}${PROJECT_NAME}${NC}"
  echo -e "  Environment:          ${GREEN}${ENVIRONMENT}${NC}"
  echo -e "  Java Version:         ${GREEN}${JAVA_VERSION}${NC}"
  echo -e "  App Port:             ${GREEN}${APP_PORT}${NC}"
  echo -e "  Redis Host:           ${GREEN}${REDIS_HOST}:${REDIS_PORT}${NC}"
  echo -e "  Centrifugo Port:      ${GREEN}${CENTRIFUGO_PORT}${NC}"
  echo -e "  Docker Compose File:  ${GREEN}${DOCKER_COMPOSE_FILE}${NC}"
  echo -e "  Logging Enabled:      ${GREEN}${ENABLE_LOGGING}${NC}"
  echo -e "  Log Directory:        ${GREEN}${LOG_DIR}${NC}"
  echo -e "${GRAY}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

# ================= VALIDATION =================
validate_config() {
  local errors=0
  
  if [[ ! -d "$PROJECT_ROOT" ]]; then
    echo -e "${RED}ERROR: Project root directory does not exist: ${PROJECT_ROOT}${NC}"
    ((errors++))
  fi
  
  if [[ ! -f "${PROJECT_ROOT}/pom.xml" ]]; then
    echo -e "${YELLOW}WARNING: Maven pom.xml not found in project root${NC}"
    ((errors++))
  fi
  
  if [[ ! -f "${DOCKER_COMPOSE_FILE}" ]]; then
    echo -e "${YELLOW}WARNING: docker-compose.yml not found at ${DOCKER_COMPOSE_FILE}${NC}"
    ((errors++))
  fi
  
  return $errors
}

# ================= FUNCTION: Create necessary directories =================
ensure_directories() {
  mkdir -p "${LOG_DIR}"
  mkdir -p "${TEMP_DIR}"
  mkdir -p "${LOGS_DIR}"
}
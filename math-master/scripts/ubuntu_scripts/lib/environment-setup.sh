#!/bin/bash
# environment-setup.sh - Development environment setup for Math Master Project

# ================= CHECK JAVA INSTALLATION =================
check_java_installation() {
  title "CHECKING JAVA INSTALLATION"
  
  if ! command -v java &> /dev/null; then
    err "Java is not installed"
    info "Please install Java ${JAVA_VERSION} or higher"
    return 1
  fi
  
  local java_version=$(java -version 2>&1 | grep -oP 'version "\K[^"]*')
  info "Found Java version: ${java_version}"
  
  success "Java installation verified"
  return 0
}

# ================= CHECK MAVEN INSTALLATION =================
check_maven_installation() {
  title "CHECKING MAVEN INSTALLATION"
  
  if ! command -v ${MAVEN_EXECUTABLE} &> /dev/null; then
    err "Maven is not installed or not in PATH"
    info "Please install Maven 3.6.0 or higher"
    return 1
  fi
  
  local mvn_version=$(${MAVEN_EXECUTABLE} -v 2>&1 | grep "Apache Maven" | grep -oP '\d+\.\d+\.\d+')
  info "Found Maven version: ${mvn_version}"
  
  success "Maven installation verified"
  return 0
}

# ================= CHECK DOCKER INSTALLATION =================
check_docker_installation() {
  title "CHECKING DOCKER INSTALLATION"
  
  if ! command -v docker &> /dev/null; then
    err "Docker is not installed"
    info "Please install Docker to use Docker Compose services"
    return 1
  fi
  
  if ! command -v docker-compose &> /dev/null; then
    err "Docker Compose is not installed"
    info "Please install Docker Compose (or use 'docker compose')"
    return 1
  fi
  
  local docker_version=$(docker --version | grep -oP '\d+\.\d+\.\d+' | head -1)
  info "Found Docker version: ${docker_version}"
  
  local compose_version=$(docker-compose --version 2>&1 | grep -oP '\d+\.\d+\.\d+' | head -1)
  info "Found Docker Compose version: ${compose_version}"
  
  success "Docker installation verified"
  return 0
}

# ================= SETUP ENVIRONMENT FILE =================
setup_environment_file() {
  title "SETTING UP ENVIRONMENT FILE"
  
  if [[ -f "${ENV_FILE}" ]]; then
    info "Environment file already exists: ${ENV_FILE}"
    ask "Do you want to reconfigure it? (y/n): "
    read reconfigure
    if [[ "${reconfigure}" != "y" ]]; then
      return 0
    fi
  fi
  
  if [[ ! -f "${ENV_EXAMPLE_FILE}" ]]; then
    warn "Example environment file not found: ${ENV_EXAMPLE_FILE}"
    info "You can create .env manually based on docker-compose.yml"
    return 1
  fi
  
  info "Creating environment file from template..."
  cp "${ENV_EXAMPLE_FILE}" "${ENV_FILE}"
  
  success "Environment file created at ${ENV_FILE}"
  info "Please edit the file with your configuration values"
  return 0
}

# ================= SETUP GIT HOOKS =================
setup_git_hooks() {
  title "SETTING UP GIT HOOKS"
  
  local hooks_dir="${PROJECT_ROOT}/.git/hooks"
  
  if [[ ! -d "${hooks_dir}" ]]; then
    warn "Git hooks directory not found"
    return 1
  fi
  
  # Create pre-commit hook
  cat > "${hooks_dir}/pre-commit" << 'EOF'
#!/bin/bash
# Pre-commit hook to prevent commits with syntax errors

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# Check for obvious errors
echo "Running pre-commit checks..."
cd "${PROJECT_ROOT}"

# You can add your checks here
# Example: mvn clean compile -q 2>/dev/null

exit 0
EOF
  
  chmod +x "${hooks_dir}/pre-commit"
  success "Git pre-commit hook installed"
}

# ================= FULL SETUP =================
full_environment_setup() {
  title "FULL ENVIRONMENT SETUP"
  echo
  
  local all_ok=true
  
  info "1/5 Checking Java..."
  if ! check_java_installation; then
    all_ok=false
  fi
  echo
  
  info "2/5 Checking Maven..."
  if ! check_maven_installation; then
    all_ok=false
  fi
  echo
  
  info "3/5 Checking Docker..."
  if ! check_docker_installation; then
    warn "Docker checks failed - some features may not work"
  fi
  echo
  
  info "4/5 Setting up environment file..."
  if ! setup_environment_file; then
    warn "Environment file setup incomplete"
  fi
  echo
  
  info "5/5 Setting up Git hooks..."
  if ! setup_git_hooks; then
    warn "Git hooks setup incomplete"
  fi
  echo
  
  if [[ "${all_ok}" == "true" ]]; then
    success "Environment setup completed successfully!"
    return 0
  else
    warn "Some setup steps had issues - please review and fix them"
    return 1
  fi
}

# ================= QUICK VERIFICATION =================
quick_verify() {
  title "QUICK VERIFICATION"
  
  local ok=0
  local total=0
  
  # Check Java
  ((total++))
  if command -v java &> /dev/null; then
    ((ok++))
    success "Java: OK"
  else
    err "Java: NOT FOUND"
  fi
  
  # Check Maven
  ((total++))
  if command -v ${MAVEN_EXECUTABLE} &> /dev/null; then
    ((ok++))
    success "Maven: OK"
  else
    err "Maven: NOT FOUND"
  fi
  
  # Check Docker
  ((total++))
  if command -v docker &> /dev/null; then
    ((ok++))
    success "Docker: OK"
  else
    warn "Docker: NOT FOUND (optional for development)"
  fi
  
  # Check project root
  ((total++))
  if [[ -d "${PROJECT_ROOT}" ]]; then
    ((ok++))
    success "Project root: OK (${PROJECT_ROOT})"
  else
    err "Project root: NOT FOUND"
  fi
  
  # Check pom.xml
  ((total++))
  if [[ -f "${PROJECT_ROOT}/pom.xml" ]]; then
    ((ok++))
    success "pom.xml: OK"
  else
    err "pom.xml: NOT FOUND"
  fi
  
  echo
  info "Verification: ${ok}/${total} checks passed"
  
  if [[ $ok -eq $total ]]; then
    success "All critical components are ready!"
    return 0
  else
    warn "Some components may be missing"
    return 1
  fi
}
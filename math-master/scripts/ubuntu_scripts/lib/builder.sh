#!/bin/bash
# builder.sh - Build operations for Math Master Project (Maven-based)

# ================= BUILD FULL PROJECT =================
build_project() {
  local profile="${1:-${BUILD_PROFILES}}"
  local clean="${2:-false}"
  
  title "BUILD MATH MASTER PROJECT"
  
  if [[ ! -f "${PROJECT_ROOT}/pom.xml" ]]; then
    err "pom.xml not found in project root: ${PROJECT_ROOT}"
    return 1
  fi
  
  cd "${PROJECT_ROOT}" || return 1
  
  # Build command
  local cmd="${MAVEN_EXECUTABLE} clean package"
  
  if [[ "${MAVEN_SKIP_TESTS}" == "true" ]]; then
    cmd="${cmd} -DskipTests"
  fi
  
  cmd="${cmd} -P ${profile}"
  
  info "Building Math Master with profile: ${profile}..."
  info "Command: ${cmd}"
  log_build_start "${PROJECT_NAME}"
  
  local start_time=$(date +%s)
  
  # Execute build with timeout
  timeout ${MAVEN_BUILD_TIMEOUT} bash -c "${cmd}"
  local result=$?
  
  local end_time=$(date +%s)
  local duration=$((end_time - start_time))
  
  if [[ $result -eq 0 ]]; then
    success "Build successful (${duration}s)"
    log_build_success "${PROJECT_NAME}" "$duration"
    cd - >/dev/null
    return 0
  else
    if [[ $result -eq 124 ]]; then
      err "Build timed out after ${MAVEN_BUILD_TIMEOUT} seconds"
    else
      err "Build failed"
    fi
    log_build_failure "${PROJECT_NAME}" "Build process failed or timed out"
    cd - >/dev/null
    return 1
  fi
}

# ================= BUILD WITH DOCKER =================
build_docker() {
  local tag="${1:-${DOCKER_IMAGE_TAG}}"
  
  title "BUILD DOCKER IMAGE"
  
  if [[ ! -f "${PROJECT_ROOT}/Dockerfile" ]]; then
    err "Dockerfile not found in project root"
    return 1
  fi
  
  cd "${PROJECT_ROOT}" || return 1
  
  local image_name="${PROJECT_NAME}:${tag}"
  
  info "Building Docker image: ${image_name}..."
  log_build_start "docker-${image_name}"
  
  local start_time=$(date +%s)
  
  docker build -t "${image_name}" . -f Dockerfile
  local result=$?
  
  local end_time=$(date +%s)
  local duration=$((end_time - start_time))
  
  if [[ $result -eq 0 ]]; then
    success "Docker image built successfully (${duration}s)"
    log_build_success "docker-${image_name}" "$duration"
    cd - >/dev/null
    return 0
  else
    err "Docker build failed"
    log_build_failure "docker-${image_name}" "Docker build process failed"
    cd - >/dev/null
    return 1
  fi
}

# ================= COMPILE ONLY (NO TESTS, NO PACKAGE) =================
compile_only() {
  local profile="${1:-${BUILD_PROFILES}}"
  
  title "COMPILE PROJECT (No Tests/Package)"
  
  cd "${PROJECT_ROOT}" || return 1
  
  local cmd="${MAVEN_EXECUTABLE} compile -P ${profile}"
  
  if [[ "${MAVEN_SKIP_TESTS}" != "true" ]]; then
    cmd="${cmd} -DskipTests"
  fi
  
  info "Compiling source code..."
  
  timeout ${MAVEN_BUILD_TIMEOUT} bash -c "${cmd}"
  local result=$?
  
  if [[ $result -eq 0 ]]; then
    success "Compilation successful"
    cd - >/dev/null
    return 0
  else
    err "Compilation failed"
    cd - >/dev/null
    return 1
  fi
}

# ================= RUN TESTS =================
run_tests() {
  local profile="${1:-${BUILD_PROFILES}}"
  
  title "RUN TESTS"
  
  cd "${PROJECT_ROOT}" || return 1
  
  local cmd="${MAVEN_EXECUTABLE} test -P ${profile}"
  
  info "Running tests..."
  
  timeout $((MAVEN_BUILD_TIMEOUT * 2)) bash -c "${cmd}"
  local result=$?
  
  if [[ $result -eq 0 ]]; then
    success "All tests passed"
    cd - >/dev/null
    return 0
  else
    err "Tests failed"
    cd - >/dev/null
    return 1
  fi
}

# ================= CLEAN BUILD ARTIFACTS =================
clean_build_artifacts() {
  title "CLEAN BUILD ARTIFACTS"
  
  cd "${PROJECT_ROOT}" || return 1
  
  info "Cleaning build artifacts..."
  
  ${MAVEN_EXECUTABLE} clean
  local result=$?
  
  cd - >/dev/null
  
  if [[ $result -eq 0 ]]; then
    success "Clean successful"
    return 0
  else
    err "Clean failed"
    return 1
  fi
}

# ================= BUILD LOG FUNCTIONS =================
log_build_start() {
  local component="$1"
  if [[ "${ENABLE_LOGGING}" == "true" ]]; then
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] BUILD START - ${component}" >> "${LOG_FILE}"
  fi
}

log_build_success() {
  local component="$1"
  local duration="${2:-unknown}"
  if [[ "${ENABLE_LOGGING}" == "true" ]]; then
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] BUILD SUCCESS - ${component} (${duration}s)" >> "${LOG_FILE}"
  fi
}

log_build_failure() {
  local component="$1"
  local reason="${2:-unknown}"
  if [[ "${ENABLE_LOGGING}" == "true" ]]; then
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] BUILD FAILED - ${component}: ${reason}" >> "${LOG_FILE}"
  fi
}
  local module_name="$2"
  
  if [[ ! -d "$module_dir" ]]; then
    err "Module directory not found: $module_dir"
    return 1
  fi
  
  cd "$module_dir" || return 1
  
  info "Building $module_name..."
  log_build_start "$module_name"
  
  local start_time=$(date +%s)
  local result=0
  
  # Determine build/deploy script
  local script_to_run=""
  
  if [[ "$module_name" == *"onln-backend"* ]] || [[ "$module_name" == *"batch"* ]]; then
    # API and Batch use deploy script
    if [[ -f "${DEPLOY_SCRIPT}" ]]; then
      script_to_run="${DEPLOY_SCRIPT}"
    else
      err "Deploy script not found: ${DEPLOY_SCRIPT}"
      cd - >/dev/null
      return 1
    fi
  else
    # Other modules use build script
    if [[ -f "${BUILD_SCRIPT}" ]]; then
      script_to_run="${BUILD_SCRIPT}"
    else
      err "Build script not found: ${BUILD_SCRIPT}"
      cd - >/dev/null
      return 1
    fi
  fi
  
  # Execute script
  ${script_to_run}
  result=$?
  
  local end_time=$(date +%s)
  local duration=$((end_time - start_time))
  
  if [[ $result -eq 0 ]]; then
    success "Build successful for $module_name (${duration}s)"
    log_build_success "$module_name" "$duration"
  else
    err "Build failed for $module_name"
    log_build_failure "$module_name" "Script returned error code $result"
    cd - >/dev/null
    return 1
  fi
  
  cd - >/dev/null
  return 0
}

# ================= BUILD TASK =================
build_task() {
  local task_type="$1"
  shift
  local repos=("$@")
  
  declare -a BUILD_SUMMARY=()
  
  title "BUILD TASK: $task_type"
  
  # Build modules in order
  for repo in "${repos[@]}"; do
    local module_name=$(basename "$repo")
    
    # Skip flyway (no build needed)
    if [[ "$module_name" == "fr-wes-flyway" ]]; then
      info "Skipping $module_name (no build needed)"
      BUILD_SUMMARY+=("$module_name|N/A|skip|No build needed")
      continue
    fi
    
    echo
    print_box "Building $module_name"
    
    if build_single_module "$repo" "$module_name"; then
      BUILD_SUMMARY+=("$module_name|N/A|success|")
    else
      BUILD_SUMMARY+=("$module_name|N/A|fail|Build failed")
      
      # Ask if user wants to continue
      echo
      if ! confirm "Build failed. Continue with remaining modules?"; then
        warn "Build process aborted"
        break
      fi
    fi
  done
  
  # Print build summary
  print_summary "BUILD SUMMARY" "${BUILD_SUMMARY[@]}"
}

# ================= BUILD MODULES (ILC + WES) =================
build_modules() {
  local repos=(
    "${REPO_ILC_MODULES}"
    "${REPO_WES_MODULES}"
  )
  
  declare -a BUILD_SUMMARY=()
  
  title "BUILD MODULES (ILC + WES)"
  
  for repo in "${repos[@]}"; do
    local module_name=$(basename "$repo")
    
    echo
    print_box "Building $module_name"
    
    if build_single_module "$repo" "$module_name"; then
      BUILD_SUMMARY+=("$module_name|N/A|success|")
    else
      BUILD_SUMMARY+=("$module_name|N/A|fail|Build failed")
      
      if ! confirm "Continue with next module?"; then
        break
      fi
    fi
  done
  
  print_summary "BUILD SUMMARY" "${BUILD_SUMMARY[@]}"
}

# ================= BUILD AND DEPLOY API =================
build_and_deploy_api() {
  # First build modules
  build_modules
  
  # Then deploy API
  echo
  title "DEPLOY API"
  
  local module_name="fr-wes-onln-backend"
  print_box "Deploying $module_name"
  
  if build_single_module "${REPO_WES_API}" "$module_name"; then
    success "API deployment successful"
    return 0
  else
    err "API deployment failed"
    return 1
  fi
}

# ================= BUILD AND DEPLOY BATCH =================
build_and_deploy_batch() {
  # First build modules
  build_modules
  
  # Then deploy Batch
  echo
  title "DEPLOY BATCH"
  
  local module_name="fr-wes-batch"
  print_box "Deploying $module_name"
  
  if build_single_module "${REPO_WES_BATCH}" "$module_name"; then
    success "Batch deployment successful"
    return 0
  else
    err "Batch deployment failed"
    return 1
  fi
}

# ================= CLEAN BUILD =================
clean_build() {
  local module_dir="$1"
  local module_name="$2"
  
  if [[ ! -d "$module_dir" ]]; then
    err "Module directory not found: $module_dir"
    return 1
  fi
  
  cd "$module_dir" || return 1
  
  info "Cleaning $module_name..."
  
  # Maven clean
  if [[ -f "pom.xml" ]]; then
    mvn clean &>/dev/null
    if [[ $? -eq 0 ]]; then
      success "Clean successful for $module_name"
    else
      warn "Clean had issues for $module_name"
    fi
  else
    warn "No pom.xml found, skipping clean"
  fi
  
  cd - >/dev/null
  return 0
}

# ================= BUILD WITH PROGRESS =================
build_with_progress() {
  local module_dir="$1"
  local module_name="$2"
  
  echo
  info "Building $module_name..."
  
  cd "$module_dir" || return 1
  
  # Run build in background
  local script_to_run="${BUILD_SCRIPT}"
  if [[ "$module_name" == *"onln-backend"* ]] || [[ "$module_name" == *"batch"* ]]; then
    script_to_run="${DEPLOY_SCRIPT}"
  fi
  
  ${script_to_run} > /tmp/build_output_$$.log 2>&1 &
  local build_pid=$!
  
  # Show spinner while building
  spinner $build_pid
  
  # Wait for completion
  wait $build_pid
  local result=$?
  
  cd - >/dev/null
  
  if [[ $result -eq 0 ]]; then
    success "Build successful"
    rm -f /tmp/build_output_$$.log
    return 0
  else
    err "Build failed. Output:"
    cat /tmp/build_output_$$.log
    rm -f /tmp/build_output_$$.log
    return 1
  fi
}
#!/bin/bash
# math-master-manager.sh - Main entry point for Math Master Project Management
# Description: Simplified single-repo project management tool for Math Master


# ================= SCRIPT DIRECTORY =================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ================= SOURCE ALL MODULES =================
source "${SCRIPT_DIR}/config/config.sh"
source "${SCRIPT_DIR}/lib/colors.sh"
source "${SCRIPT_DIR}/lib/ui.sh"
source "${SCRIPT_DIR}/lib/git_utils.sh"
source "${SCRIPT_DIR}/lib/status_checker.sh"
source "${SCRIPT_DIR}/lib/builder.sh"
source "${SCRIPT_DIR}/lib/logger.sh"
source "${SCRIPT_DIR}/lib/environment-setup.sh"
source "${SCRIPT_DIR}/lib/health-checks.sh"
source "${SCRIPT_DIR}/lib/database-migrations.sh"

set -euo pipefail

# ================= INITIALIZATION =================
init_manager() {
  ensure_directories
  log_info "Math Master Manager started"
}

# ================= BUILD MENU =================
build_menu() {
  while true; do
    clear
    title "BUILD OPTIONS"
    echo
    opt "1. Build Project (Maven)"
    opt "2. Build Docker Image"
    opt "3. Compile Only (No Tests)"
    opt "4. Run Tests"
    opt "5. Clean Build Artifacts"
    opt "6. Back to Main Menu"
    echo
    ask "Select option (1-6): "
    read BUILD_CHOICE
    
    case "$BUILD_CHOICE" in
      1) 
        build_project
        ask "Press enter to continue..."
        read
        ;;
      2)
        build_docker
        ask "Press enter to continue..."
        read
        ;;
      3)
        compile_only
        ask "Press enter to continue..."
        read
        ;;
      4)
        run_tests
        ask "Press enter to continue..."
        read
        ;;
      5)
        clean_build_artifacts
        ask "Press enter to continue..."
        read
        ;;
      6)
        break
        ;;
      *)
        warn "Invalid option"
        sleep 1
        ;;
    esac
  done
}

# ================= DOCKER COMPOSE MENU =================
docker_menu() {
  while true; do
    clear
    title "DOCKER COMPOSE OPTIONS"
    echo
    opt "1. Start All Services"
    opt "2. Stop All Services"
    opt "3. Restart Services"
    opt "4. View Logs"
    opt "5. Check Service Status"
    opt "6. View Docker Compose Config"
    opt "7. Back to Main Menu"
    echo
    ask "Select option (1-7): "
    read DOCKER_CHOICE
    
    case "$DOCKER_CHOICE" in
      1)
        docker_compose_up
        ask "Press enter to continue..."
        read
        ;;
      2)
        docker_compose_down
        ask "Press enter to continue..."
        read
        ;;
      3)
        docker_compose_restart
        ask "Press enter to continue..."
        read
        ;;
      4)
        docker_compose_logs
        ask "Press enter to continue..."
        read
        ;;
      5)
        docker_compose_status
        ask "Press enter to continue..."
        read
        ;;
      6)
        docker_compose_config
        ask "Press enter to continue..."
        read
        ;;
      7)
        break
        ;;
      *)
        warn "Invalid option"
        sleep 1
        ;;
    esac
  done
}

# ================= DATABASE MENU =================
database_menu() {
  while true; do
    clear
    title "DATABASE OPTIONS"
    echo
    opt "1. Initialize Database"
    opt "2. Wait for Database"
    opt "3. Show Database Info"
    opt "4. Backup Database"
    opt "5. Restore Database"
    opt "6. Reset Database (CAUTION)"
    opt "7. Show Migration Status"
    opt "8. Back to Main Menu"
    echo
    ask "Select option (1-8): "
    read DB_CHOICE
    
    case "$DB_CHOICE" in
      1)
        init_database
        ask "Press enter to continue..."
        read
        ;;
      2)
        wait_for_database
        ask "Press enter to continue..."
        read
        ;;
      3)
        show_database_info
        ask "Press enter to continue..."
        read
        ;;
      4)
        backup_database
        ask "Press enter to continue..."
        read
        ;;
      5)
        restore_database
        ask "Press enter to continue..."
        read
        ;;
      6)
        warn "This is a destructive operation!"
        reset_database
        ask "Press enter to continue..."
        read
        ;;
      7)
        show_migration_status
        ask "Press enter to continue..."
        read
        ;;
      8)
        break
        ;;
      *)
        warn "Invalid option"
        sleep 1
        ;;
    esac
  done
}

# ================= ENVIRONMENT SETUP MENU =================
environment_menu() {
  while true; do
    clear
    title "ENVIRONMENT SETUP OPTIONS"
    echo
    opt "1. Full Environment Setup"
    opt "2. Quick Verification"
    opt "3. Check Java Installation"
    opt "4. Check Maven Installation"
    opt "5. Check Docker Installation"
    opt "6. Setup Environment File"
    opt "7. Back to Main Menu"
    echo
    ask "Select option (1-7): "
    read ENV_CHOICE
    
    case "$ENV_CHOICE" in
      1)
        full_environment_setup
        ask "Press enter to continue..."
        read
        ;;
      2)
        quick_verify
        ask "Press enter to continue..."
        read
        ;;
      3)
        check_java_installation
        ask "Press enter to continue..."
        read
        ;;
      4)
        check_maven_installation
        ask "Press enter to continue..."
        read
        ;;
      5)
        check_docker_installation
        ask "Press enter to continue..."
        read
        ;;
      6)
        setup_environment_file
        ask "Press enter to continue..."
        read
        ;;
      7)
        break
        ;;
      *)
        warn "Invalid option"
        sleep 1
        ;;
    esac
  done
}

# ================= HEALTH CHECK MENU =================
health_check_menu() {
  while true; do
    clear
    title "HEALTH CHECK OPTIONS"
    echo
    opt "1. Check All Services"
    opt "2. Check Application Health"
    opt "3. Check Redis Health"
    opt "4. Check Database Health"
    opt "5. Check Centrifugo Health"
    opt "6. Wait for Services Ready"
    opt "7. Show Service Status"
    opt "8. Back to Main Menu"
    echo
    ask "Select option (1-8): "
    read HEALTH_CHOICE
    
    case "$HEALTH_CHOICE" in
      1)
        check_all_services
        ask "Press enter to continue..."
        read
        ;;
      2)
        check_app_health
        ask "Press enter to continue..."
        read
        ;;
      3)
        check_redis_health
        ask "Press enter to continue..."
        read
        ;;
      4)
        check_database_health
        ask "Press enter to continue..."
        read
        ;;
      5)
        check_centrifugo_health
        ask "Press enter to continue..."
        read
        ;;
      6)
        wait_for_services
        ask "Press enter to continue..."
        read
        ;;
      7)
        show_service_status
        ask "Press enter to continue..."
        read
        ;;
      8)
        break
        ;;
      *)
        warn "Invalid option"
        sleep 1
        ;;
    esac
  done
}

# ================= DOCKER COMPOSE OPERATIONS =================
docker_compose_up() {
  title "START DOCKER SERVICES"
  
  if [[ ! -f "${DOCKER_COMPOSE_FILE}" ]]; then
    err "docker-compose.yml not found at ${DOCKER_COMPOSE_FILE}"
    return 1
  fi
  
  cd "${PROJECT_ROOT}" || return 1
  
  if [[ -f "${ENV_FILE}" ]]; then
    info "Using environment file: ${ENV_FILE}"
    export $(cat "${ENV_FILE}" | grep -v '^#' | xargs)
  else
    warn "Environment file not found at ${ENV_FILE}"
  fi
  
  info "Starting services with docker-compose up..."
  docker-compose -f "${DOCKER_COMPOSE_FILE}" up -d
  local result=$?
  
  cd - >/dev/null
  
  if [[ $result -eq 0 ]]; then
    success "Services started successfully"
    docker_compose_status
    return 0
  else
    err "Failed to start services"
    return 1
  fi
}

docker_compose_down() {
  title "STOP DOCKER SERVICES"
  
  cd "${PROJECT_ROOT}" || return 1
  
  info "Stopping services..."
  docker-compose -f "${DOCKER_COMPOSE_FILE}" down
  local result=$?
  
  cd - >/dev/null
  
  if [[ $result -eq 0 ]]; then
    success "Services stopped"
    return 0
  else
    err "Failed to stop services"
    return 1
  fi
}

docker_compose_restart() {
  title "RESTART DOCKER SERVICES"
  
  cd "${PROJECT_ROOT}" || return 1
  
  info "Restarting services..."
  docker-compose -f "${DOCKER_COMPOSE_FILE}" restart
  local result=$?
  
  cd - >/dev/null
  
  if [[ $result -eq 0 ]]; then
    success "Services restarted"
    return 0
  else
    err "Failed to restart services"
    return 1
  fi
}

docker_compose_logs() {
  title "DOCKER COMPOSE LOGS (Press Ctrl+C to exit)"
  
  cd "${PROJECT_ROOT}" || return 1
  
  docker-compose -f "${DOCKER_COMPOSE_FILE}" logs -f
  
  cd - >/dev/null
}

docker_compose_status() {
  title "DOCKER SERVICES STATUS"
  
  cd "${PROJECT_ROOT}" || return 1
  
  docker-compose -f "${DOCKER_COMPOSE_FILE}" ps
  
  cd - >/dev/null
}

docker_compose_config() {
  title "DOCKER COMPOSE CONFIGURATION"
  
  cd "${PROJECT_ROOT}" || return 1
  
  docker-compose -f "${DOCKER_COMPOSE_FILE}" config
  
  cd - >/dev/null
}

# ================= MAIN MENU =================
main_menu() {
  reset_proxy
  init_manager
  
  while true; do
    clear
    print_banner
    title "MATH MASTER PROJECT MANAGER"
    echo
    opt "1. Build Operations"
    opt "2. Docker Compose"
    opt "3. Database Management"
    opt "4. Health Checks"
    opt "5. Environment Setup"
    opt "6. Repository Status"
    opt "7. Print Configuration"
    opt "8. View Logs"
    opt "9. Exit"
    echo
    ask "Select option (1-9): "
    read MAIN_CHOICE
    
    case "$MAIN_CHOICE" in
      1) build_menu ;;
      2) docker_menu ;;
      3) database_menu ;;
      4) health_check_menu ;;
      5) environment_menu ;;
      6) check_status_menu ;;
      7) print_config ;;
      8) view_logs_menu ;;
      9)
        success "Goodbye!"
        exit 0
        ;;
      *)
        warn "Invalid option"
        sleep 1
        ;;
    esac
  done
}

# ================= STATUS CHECK MENU =================
check_status_menu() {
  title "REPOSITORY STATUS"
  
  cd "${PROJECT_ROOT}" || return 1
  
  git status
  
  echo
  info "Git log (last 5 commits):"
  git log --oneline -5
  
  cd - >/dev/null
  
  ask "Press enter to continue..."
  read
}

# ================= LOGS MENU =================
view_logs_menu() {
  title "VIEW LOGS"
  
  if [[ ! -d "${LOG_DIR}" ]]; then
    warn "Log directory not found: ${LOG_DIR}"
    ask "Press enter to continue..."
    read
    return
  fi
  
  local log_count=$(find "${LOG_DIR}" -type f -name "*.log" 2>/dev/null | wc -l)
  
  if [[ $log_count -eq 0 ]]; then
    warn "No log files found"
  else
    info "Available log files:"
    echo
    select log_file in $(ls -rt "${LOG_DIR}"/*.log 2>/dev/null | tail -10) "Back"; do
      if [[ "$log_file" == "Back" ]]; then
        break
      elif [[ -n "$log_file" ]]; then
        less "$log_file"
        break
      fi
    done
  fi
}

# ================= ENTRY POINT =================
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  main_menu
fi
      4) apply_formatter_menu ;;
      5) check_status_menu ;;
      6) cleanup_branches_menu ;;
      7) view_logs_menu ;;
      8) configuration_menu ;;
      9) 
        echo
        success "Goodbye!"
        exit 0
        ;;
      *)
        warn "Invalid choice. Please try again."
        sleep 2
        ;;
    esac
  done
}

# ================= SWITCH ENVIRONMENT =================
switch_environment_menu() {
  title "SWITCH ENVIRONMENT & BUILD"
  echo
  opt "Task Types:"
  opt "1. ML - Module Library"
  opt "2. AP - API"
  opt "3. BT - Batch"
  echo
  ask "Select task type (1-3): "
  read TASK_TYPE_NUM

  case "$TASK_TYPE_NUM" in
    1) TASK_TYPE="ML" ;;
    2) TASK_TYPE="AP" ;;
    3) TASK_TYPE="BT" ;;
    *)
      err "Invalid task type!"
      press_any_key
      return
      ;;
  esac

  echo
  ask "Enter version (e.g. 3.1, 3.0, 2.8.65): "
  read VERSION
  
  # Parse version and determine branch
  local BRANCH=$(parse_version_to_branch "$VERSION")
  if [[ -z "$BRANCH" ]]; then
    err "Invalid version format!"
    press_any_key
    return
  fi
  
  local FALLBACK_BRANCH=$(get_fallback_branch "$BRANCH")
  
  # Determine which repos to process
  local REPOS=()
  case "$TASK_TYPE" in
    ML)
      REPOS=("${REPO_FLYWAY}" "${REPO_ILC_MODULES}" "${REPO_WES_MODULES}")
      ;;
    AP)
      REPOS=("${REPO_FLYWAY}" "${REPO_ILC_MODULES}" "${REPO_WES_MODULES}" "${REPO_WES_API}")
      ;;
    BT)
      REPOS=("${REPO_FLYWAY}" "${REPO_ILC_MODULES}" "${REPO_WES_MODULES}" "${REPO_WES_BATCH}")
      ;;
  esac
  
  # Check status before switching
  echo
  title "REPOSITORY STATUS CHECK"
  check_repos_status "${REPOS[@]}"
  
  echo
  warn "Ready to switch all repositories to branch: ${BRANCH}"
  ask "Continue? (Y/n): "
  read CONFIRM
  
  if [[ -n "$CONFIRM" && ! "$CONFIRM" =~ ^[Yy]$ ]]; then
    warn "Operation cancelled"
    press_any_key
    return
  fi
  
  # Perform checkout
  echo
  title "SWITCHING BRANCHES"
  declare -a CHECKOUT_SUMMARY=()
  
  for repo in "${REPOS[@]}"; do
    checkout_and_pull_branch "$repo" "$BRANCH" "$FALLBACK_BRANCH" CHECKOUT_SUMMARY
  done
  
  # Print checkout summary
  print_summary "CHECKOUT SUMMARY" "${CHECKOUT_SUMMARY[@]}"
  
  # Ask to build
  echo
  ask "Proceed with build? (Y/n): "
  read BUILD_CONFIRM
  
  if [[ -z "$BUILD_CONFIRM" || "$BUILD_CONFIRM" =~ ^[Yy]$ ]]; then
    build_task "$TASK_TYPE" "${REPOS[@]}"
  fi
  
  press_any_key
}

# ================= BUILD SINGLE MODULE =================
build_single_module_menu() {
  title "BUILD SINGLE MODULE"
  echo
  opt "1. fr-ilc-backend-modules"
  opt "2. fr-wes-backend-modules"
  opt "3. fr-wes-onln-backend (API)"
  opt "4. fr-wes-batch"
  echo
  ask "Select module (1-4): "
  read MODULE_CHOICE
  
  local MODULE_DIR=""
  local MODULE_NAME=""
  
  case "$MODULE_CHOICE" in
    1)
      MODULE_DIR="${REPO_ILC_MODULES}"
      MODULE_NAME="fr-ilc-backend-modules"
      ;;
    2)
      MODULE_DIR="${REPO_WES_MODULES}"
      MODULE_NAME="fr-wes-backend-modules"
      ;;
    3)
      MODULE_DIR="${REPO_WES_API}"
      MODULE_NAME="fr-wes-onln-backend"
      ;;
    4)
      MODULE_DIR="${REPO_WES_BATCH}"
      MODULE_NAME="fr-wes-batch"
      ;;
    *)
      err "Invalid choice"
      press_any_key
      return
      ;;
  esac
  
  echo
  print_box "Building $MODULE_NAME"
  
  if build_single_module "$MODULE_DIR" "$MODULE_NAME"; then
    success "Build successful!"
  else
    err "Build failed!"
  fi
  
  press_any_key
}

# ================= BUILD VRF =================
build_vrf_menu() {
  title "BUILD VRF FRAMEWORK"
  echo
  opt "1. v2.0.0"
  opt "2. v1.4.25"
  echo
  ask "Select version (1-2): "
  read VRF_CHOICE
  
  case "$VRF_CHOICE" in
    1) VRF_VERSION="v2.0.0" ;;
    2) VRF_VERSION="v1.4.25" ;;
    *)
      err "Invalid choice"
      press_any_key
      return
      ;;
  esac
  
  if build_vrf "$VRF_VERSION"; then
    success "VRF build successful!"
  else
    err "VRF build failed!"
  fi
  
  press_any_key
}

# ================= APPLY FORMATTER =================
apply_formatter_menu() {
  title "APPLY FORMATTER"
  echo
  opt "  1. fr-ilc-backend-modules"
  opt "  2. fr-wes-backend-modules"
  opt "  3. fr-wes-onln-backend"
  opt "  4. fr-wes-batch"
  echo
  ask "Select option (1-4): "
  read FORMAT_CHOICE
  
  case "$FORMAT_CHOICE" in
    1) format_module "${REPO_ILC_MODULES}" ;;
    2) format_module "${REPO_WES_MODULES}" ;;
    3) format_module "${REPO_WES_API}" ;;
    4) format_module "${REPO_WES_BATCH}" ;;
    *) err "Invalid choice" ;;
  esac
  
  press_any_key
}

# ================= CHECK STATUS =================
check_status_menu() {
  title "REPOSITORY STATUS"
  echo
  ask "Check all repos? (Y/n): "
  read CHECK_ALL
  
  local REPOS_TO_CHECK=()
  
  if [[ -z "$CHECK_ALL" || "$CHECK_ALL" =~ ^[Yy]$ ]]; then
    REPOS_TO_CHECK=(
      "${REPO_FLYWAY}"
      "${REPO_ILC_MODULES}"
      "${REPO_WES_MODULES}"
      "${REPO_WES_API}"
      "${REPO_WES_BATCH}"
    )
  else
    # Select specific repos
    echo
    hint "Select repos to check (space-separated, e.g. 1 2 3):"
    opt "  1. fr-wes-flyway"
    opt "  2. fr-ilc-backend-modules"
    opt "  3. fr-wes-backend-modules"
    opt "  4. fr-wes-onln-backend"
    opt "  5. fr-wes-batch"
    echo
    ask "Enter numbers: "
    read -a REPO_NUMS
    
    for num in "${REPO_NUMS[@]}"; do
      case "$num" in
        1) REPOS_TO_CHECK+=("${REPO_FLYWAY}") ;;
        2) REPOS_TO_CHECK+=("${REPO_ILC_MODULES}") ;;
        3) REPOS_TO_CHECK+=("${REPO_WES_MODULES}") ;;
        4) REPOS_TO_CHECK+=("${REPO_WES_API}") ;;
        5) REPOS_TO_CHECK+=("${REPO_WES_BATCH}") ;;
      esac
    done
  fi
  
  echo
  check_repos_status "${REPOS_TO_CHECK[@]}"
  
  press_any_key
}

# ================= CLEANUP BRANCHES =================
cleanup_branches_menu() {
  title "CLEANUP LOCAL BRANCHES"
  echo
  warn "This will delete all local branches except the default branch"
  warn "Default branch: ${DEFAULT_BRANCH}"
  echo
  
  opt "Select repository:"
  opt "  1. fr-wes-flyway"
  opt "  2. fr-ilc-backend-modules"
  opt "  3. fr-wes-backend-modules"
  opt "  4. fr-wes-onln-backend"
  opt "  5. fr-wes-batch"
  opt "  6. All repositories"
  echo
  ask "Select option (1-6): "
  read CLEANUP_CHOICE
  
  local REPOS_TO_CLEAN=()
  
  case "$CLEANUP_CHOICE" in
    1) REPOS_TO_CLEAN=("${REPO_FLYWAY}") ;;
    2) REPOS_TO_CLEAN=("${REPO_ILC_MODULES}") ;;
    3) REPOS_TO_CLEAN=("${REPO_WES_MODULES}") ;;
    4) REPOS_TO_CLEAN=("${REPO_WES_API}") ;;
    5) REPOS_TO_CLEAN=("${REPO_WES_BATCH}") ;;
    6)
      REPOS_TO_CLEAN=(
        "${REPO_FLYWAY}"
        "${REPO_ILC_MODULES}"
        "${REPO_WES_MODULES}"
        "${REPO_WES_API}"
        "${REPO_WES_BATCH}"
      )
      ;;
    *)
      err "Invalid choice"
      press_any_key
      return
      ;;
  esac
  
  echo
  err "WARNING: This action cannot be undone!"
  ask "Are you sure? (yes/no): "
  read FINAL_CONFIRM
  
  if [[ "$FINAL_CONFIRM" == "yes" ]]; then
    for repo in "${REPOS_TO_CLEAN[@]}"; do
      cleanup_local_branches "$repo"
    done
    success "Cleanup completed!"
  else
    warn "Operation cancelled"
  fi
  
  press_any_key
}

# ================= VIEW LOGS =================
view_logs_menu() {
  title "VIEW LOGS"
  echo
  
  if [[ ! -d "${LOG_DIR}" ]] || [[ -z "$(ls -A ${LOG_DIR} 2>/dev/null)" ]]; then
    warn "No logs found"
    press_any_key
    return
  fi
  
  opt "Available log files:"
  echo
  
  local LOG_FILES=("${LOG_DIR}"/*)
  local idx=1
  
  for log_file in "${LOG_FILES[@]}"; do
    echo -e "  ${GREEN}${idx}.${NC} $(basename "$log_file")"
    ((idx++))
  done
  
  echo
  ask "Select log file to view (1-$((idx-1))): "
  read LOG_CHOICE
  
  if [[ "$LOG_CHOICE" -ge 1 && "$LOG_CHOICE" -lt "$idx" ]]; then
    local SELECTED_LOG="${LOG_FILES[$((LOG_CHOICE-1))]}"
    echo
    title "LOG CONTENT: $(basename "$SELECTED_LOG")"
    echo
    cat "$SELECTED_LOG" | less -R
  else
    err "Invalid choice"
  fi
  
  press_any_key
}

# ================= CONFIGURATION =================
configuration_menu() {
  title "CONFIGURATION"
  echo
  opt "1. View current configuration"
  opt "2. Edit project path"
  opt "3. Edit default branch"
  opt "4. Reset to defaults"
  echo
  ask "Select option (1-4): "
  read CONFIG_CHOICE
  
  case "$CONFIG_CHOICE" in
    1)
      echo
      print_config
      ;;
    2)
      echo
      ask "Enter new project path: "
      read NEW_PATH
      if [[ -d "$NEW_PATH" ]]; then
        sed -i "s|^PROJECT_ROOT=.*|PROJECT_ROOT=\"$NEW_PATH\"|" "${SCRIPT_DIR}/config/config.sh"
        success "Project path updated. Please restart the script."
      else
        err "Path does not exist!"
      fi
      ;;
    3)
      echo
      ask "Enter new default branch: "
      read NEW_BRANCH
      sed -i "s|^DEFAULT_BRANCH=.*|DEFAULT_BRANCH=\"$NEW_BRANCH\"|" "${SCRIPT_DIR}/config/config.sh"
      success "Default branch updated. Please restart the script."
      ;;
    4)
      warn "Reset configuration to defaults? (yes/no): "
      read RESET_CONFIRM
      if [[ "$RESET_CONFIRM" == "yes" ]]; then
        # Restore default config
        success "Configuration reset (restart required)"
      fi
      ;;
    *)
      err "Invalid choice"
      ;;
  esac
  
  press_any_key
}

# ================= ENTRY POINT =================
main_menu
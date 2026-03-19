#!/bin/bash
# logger.sh - Logging utilities

# ================= LOG LEVELS =================
LOG_LEVEL_DEBUG=0
LOG_LEVEL_INFO=1
LOG_LEVEL_WARN=2
LOG_LEVEL_ERROR=3

CURRENT_LOG_LEVEL=${LOG_LEVEL_INFO}

# ================= LOG FUNCTIONS =================
log_message() {
  local level="$1"
  local message="$2"
  local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
  
  if [[ "${ENABLE_LOGGING}" != "true" ]]; then
    return
  fi
  
  # Ensure log directory exists
  mkdir -p "${LOG_DIR}"
  
  # Write to log file
  echo "[${timestamp}] [${level}] ${message}" >> "${LOG_FILE}"
}

log_debug() {
  if [[ ${CURRENT_LOG_LEVEL} -le ${LOG_LEVEL_DEBUG} ]]; then
    log_message "DEBUG" "$1"
  fi
}

log_info() {
  if [[ ${CURRENT_LOG_LEVEL} -le ${LOG_LEVEL_INFO} ]]; then
    log_message "INFO" "$1"
  fi
}

log_warn() {
  if [[ ${CURRENT_LOG_LEVEL} -le ${LOG_LEVEL_WARN} ]]; then
    log_message "WARN" "$1"
  fi
}

log_error() {
  if [[ ${CURRENT_LOG_LEVEL} -le ${LOG_LEVEL_ERROR} ]]; then
    log_message "ERROR" "$1"
  fi
}

# ================= CONFLICT LOGGING =================
log_conflict() {
  local repo="$1"
  local branch="$2"
  local action="$3"
  local details="$4"
  
  local conflict_log="${LOG_DIR}/conflicts-$(date +%Y%m%d).log"
  local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
  
  cat >> "${conflict_log}" << EOF
[${timestamp}] CONFLICT DETECTED
  Repository: ${repo}
  Branch: ${branch}
  Action Taken: ${action}
  Details: ${details}
────────────────────────────────────────
EOF
  
  log_warn "Conflict in ${repo} on ${branch} - ${action}"
}

# ================= STASH LOGGING =================
log_stash() {
  local repo="$1"
  local stash_name="$2"
  local branch="$3"
  
  local stash_log="${LOG_DIR}/stashes-$(date +%Y%m%d).log"
  local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
  
  cat >> "${stash_log}" << EOF
[${timestamp}] STASH CREATED
  Repository: ${repo}
  Stash Name: ${stash_name}
  Branch: ${branch}
────────────────────────────────────────
EOF
  
  log_info "Stash created in ${repo}: ${stash_name}"
}

# ================= BUILD LOGGING =================
log_build_start() {
  local module="$1"
  log_info "Build started for ${module}"
}

log_build_success() {
  local module="$1"
  local duration="$2"
  log_info "Build successful for ${module} (${duration}s)"
}

log_build_failure() {
  local module="$1"
  local error="$2"
  log_error "Build failed for ${module}: ${error}"
}

# ================= CHECKOUT LOGGING =================
log_checkout() {
  local repo="$1"
  local from_branch="$2"
  local to_branch="$3"
  log_info "Checkout ${repo}: ${from_branch} -> ${to_branch}"
}

# ================= SESSION LOGGING =================
log_session_start() {
  log_info "════════════════════════════════════════"
  log_info "FR Manager session started"
  log_info "User: ${USER}"
  log_info "Project Root: ${PROJECT_ROOT}"
  log_info "════════════════════════════════════════"
}

log_session_end() {
  log_info "════════════════════════════════════════"
  log_info "FR Manager session ended"
  log_info "════════════════════════════════════════"
}

# ================= LOG ROTATION =================
rotate_logs() {
  local max_days=30
  
  if [[ ! -d "${LOG_DIR}" ]]; then
    return
  fi
  
  # Delete logs older than max_days
  find "${LOG_DIR}" -name "*.log" -type f -mtime +${max_days} -delete
  
  log_info "Log rotation completed (kept last ${max_days} days)"
}

# ================= VIEW LOGS =================
show_recent_logs() {
  local lines="${1:-50}"
  
  if [[ ! -f "${LOG_FILE}" ]]; then
    warn "No log file found"
    return 1
  fi
  
  echo
  title "RECENT LOGS (Last ${lines} lines)"
  echo
  
  tail -n "${lines}" "${LOG_FILE}" | while IFS= read -r line; do
    # Color code log levels
    if [[ "$line" =~ \[ERROR\] ]]; then
      echo -e "${RED}${line}${NC}"
    elif [[ "$line" =~ \[WARN\] ]]; then
      echo -e "${YELLOW}${line}${NC}"
    elif [[ "$line" =~ \[INFO\] ]]; then
      echo -e "${CYAN}${line}${NC}"
    elif [[ "$line" =~ \[DEBUG\] ]]; then
      echo -e "${GRAY}${line}${NC}"
    else
      echo "$line"
    fi
  done
}

# ================= LOG STATS =================
show_log_stats() {
  if [[ ! -f "${LOG_FILE}" ]]; then
    warn "No log file found"
    return 1
  fi
  
  echo
  title "LOG STATISTICS"
  echo
  
  local total_lines=$(wc -l < "${LOG_FILE}")
  local errors=$(grep -c "\[ERROR\]" "${LOG_FILE}" || echo 0)
  local warnings=$(grep -c "\[WARN\]" "${LOG_FILE}" || echo 0)
  local info=$(grep -c "\[INFO\]" "${LOG_FILE}" || echo 0)
  
  echo -e "  Total Entries:  ${GREEN}${total_lines}${NC}"
  echo -e "  Errors:         ${RED}${errors}${NC}"
  echo -e "  Warnings:       ${YELLOW}${warnings}${NC}"
  echo -e "  Info:           ${CYAN}${info}${NC}"
  echo
}
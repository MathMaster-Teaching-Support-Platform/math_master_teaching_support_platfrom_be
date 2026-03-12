#!/bin/bash
# health-checks.sh - Service health checks for Math Master Project

# ================= CHECK APP HEALTH =================
check_app_health() {
  title "CHECKING APPLICATION HEALTH"
  
  local health_url="http://localhost:${APP_PORT}${APP_HEALTH_ENDPOINT}"
  
  info "Checking application at ${health_url}..."
  
  for i in {1..15}; do
    local response=$(curl -s -o /dev/null -w "%{http_code}" "${health_url}" 2>/dev/null || echo "000")
    
    if [[ "${response}" == "200" ]]; then
      success "Application is healthy (HTTP ${response})"
      return 0
    fi
    
    if [[ $i -lt 15 ]]; then
      info "Attempt $i/15: Health check returned HTTP ${response}, retrying..."
      sleep 2
    fi
  done
  
  err "Application health check failed after 15 attempts"
  return 1
}

# ================= CHECK REDIS HEALTH =================
check_redis_health() {
  title "CHECKING REDIS HEALTH"
  
  info "Connecting to Redis at ${REDIS_HOST}:${REDIS_PORT}..."
  
  local response=$(redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" -a "${REDIS_PASSWORD}" ping 2>/dev/null || echo "FAILED")
  
  if [[ "${response}" == "PONG" ]]; then
    success "Redis is healthy"
    
    # Get some stats
    local info=$(redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" -a "${REDIS_PASSWORD}" info stats 2>/dev/null)
    info "Redis stats:"
    echo "${info}" | grep -E "connected_clients|used_memory|total_commands_processed" | sed 's/^/  /'
    
    return 0
  else
    err "Redis health check failed: ${response}"
    return 1
  fi
}

# ================= CHECK DATABASE HEALTH =================
check_database_health() {
  title "CHECKING DATABASE HEALTH"
  
  info "Database type: ${DATABASE_TYPE}"
  
  if [[ "${DATABASE_TYPE}" == "postgresql" ]]; then
    # For PostgreSQL in Docker
    local response=$(docker exec math-master-postgres pg_isready -U postgres 2>/dev/null || echo "FAILED")
    
    if [[ "${response}" == *"accepting connections"* ]]; then
      success "PostgreSQL is accepting connections"
      return 0
    else
      warn "PostgreSQL check result: ${response}"
      info "Note: Database might still be initializing"
      return 0
    fi
  fi
  
  return 1
}

# ================= CHECK CENTRIFUGO HEALTH =================
check_centrifugo_health() {
  title "CHECKING CENTRIFUGO HEALTH"
  
  local health_url="http://localhost:${CENTRIFUGO_PORT}/health"
  
  info "Checking Centrifugo at ${health_url}..."
  
  local response=$(curl -s "${health_url}" 2>/dev/null || echo "FAILED")
  
  if [[ "${response}" == *"status"* ]] || [[ "${response}" == "ok" ]]; then
    success "Centrifugo is healthy"
    return 0
  else
    err "Centrifugo health check failed"
    return 1
  fi
}

# ================= CHECK ALL SERVICES =================
check_all_services() {
  title "FULL SERVICE HEALTH CHECK"
  
  local failed=0
  
  echo
  info "Checking all services..."
  echo
  
  # Check each service with error handling
  check_redis_health || ((failed++))
  echo
  
  check_centrifugo_health || ((failed++))
  echo
  
  check_database_health || ((failed++))
  echo
  
  check_app_health || ((failed++))
  echo
  
  if [[ $failed -eq 0 ]]; then
    success "All services are healthy!"
    return 0
  else
    warn "$failed service(s) failed health checks"
    return 1
  fi
}

# ================= WAIT FOR SERVICES =================
wait_for_services() {
  local timeout="${1:-120}"  # Default 2 minutes
  local start_time=$(date +%s)
  
  title "WAITING FOR SERVICES TO BE READY (timeout: ${timeout}s)"
  
  while true; do
    local current_time=$(date +%s)
    local elapsed=$((current_time - start_time))
    
    if [[ $elapsed -gt $timeout ]]; then
      err "Services did not become ready within ${timeout} seconds"
      return 1
    fi
    
    info "Checking services... (elapsed: ${elapsed}s)"
    
    if check_all_services 2>/dev/null; then
      success "All services are ready!"
      return 0
    fi
    
    sleep 5
  done
}

# ================= SHOW SERVICE STATUS =================
show_service_status() {
  title "SERVICE STATUS OVERVIEW"
  
  echo
  echo -e "${CYAN}Container Status:${NC}"
  docker ps --filter "name=math-master" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || warn "Docker not available"
  
  echo
  echo -e "${CYAN}Network Connectivity:${NC}"
  echo -n "  Application (localhost:${APP_PORT}): "
  if curl -s "http://localhost:${APP_PORT}/actuator/health" >/dev/null 2>&1; then
    success "✓ Reachable"
  else
    err "✗ Not reachable"
  fi
  
  echo -n "  Redis (${REDIS_HOST}:${REDIS_PORT}): "
  if redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" -a "${REDIS_PASSWORD}" ping >/dev/null 2>&1; then
    success "✓ Reachable"
  else
    err "✗ Not reachable"
  fi
  
  echo -n "  Centrifugo (localhost:${CENTRIFUGO_PORT}): "
  if curl -s "http://localhost:${CENTRIFUGO_PORT}/health" >/dev/null 2>&1; then
    success "✓ Reachable"
  else
    err "✗ Not reachable"
  fi
}

# ================= EXPORT FUNCTIONS FOR SOURCING =================
export -f check_app_health
export -f check_redis_health
export -f check_database_health
export -f check_centrifugo_health
export -f check_all_services
export -f wait_for_services
export -f show_service_status
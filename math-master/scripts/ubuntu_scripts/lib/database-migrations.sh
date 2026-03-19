#!/bin/bash
# database-migrations.sh - Database migration and initialization for Math Master Project

# ================= INIT DATABASE =================
init_database() {
  title "INITIALIZING DATABASE"
  
  if [[ "${DATABASE_TYPE}" != "postgresql" ]]; then
    warn "Database type '${DATABASE_TYPE}' not supported for automated initialization"
    return 1
  fi
  
  info "Initializing PostgreSQL database..."
  info "Note: Application uses Hibernate with ddl-auto: update"
  info "Database will be initialized when the application starts"
  
  return 0
}

# ================= WAIT FOR DATABASE =================
wait_for_database() {
  local timeout="${1:-120}"
  local start_time=$(date +%s)
  
  title "WAITING FOR DATABASE TO BE READY (timeout: ${timeout}s)"
  
  while true; do
    local current_time=$(date +%s)
    local elapsed=$((current_time - start_time))
    
    if [[ $elapsed -gt $timeout ]]; then
      err "Database did not become ready within ${timeout} seconds"
      return 1
    fi
    
    info "Checking database connection... (elapsed: ${elapsed}s)"
    
    if check_database_health 2>/dev/null; then
      success "Database is ready!"
      return 0
    fi
    
    sleep 5
  done
}

# ================= SHOW DATABASE INFO =================
show_database_info() {
  title "DATABASE INFORMATION"
  
  info "Database Type: ${DATABASE_TYPE}"
  info "Service Name: ${DATABASE_SERVICE_NAME}"
  info "Port: ${DATABASE_PORT}"
  
  # Check if running in Docker
  local db_container=$(docker ps --filter "name=${DATABASE_SERVICE_NAME}" --format "{{.Names}}" 2>/dev/null)
  
  if [[ -n "${db_container}" ]]; then
    success "Database container is running: ${db_container}"
    
    # Show basic stats
    echo
    info "Container Details:"
    docker inspect "${db_container}" 2>/dev/null | grep -E '"Status"|"State"' | head -5 | sed 's/^/  /'
  else
    info "Database container is not running"
  fi
}

# ================= RESET DATABASE =================
reset_database() {
  title "RESET DATABASE"
  
  warn "This will delete all data in the database!"
  ask "Are you sure? Type 'yes' to confirm: "
  read confirmation
  
  if [[ "${confirmation}" != "yes" ]]; then
    info "Database reset cancelled"
    return 0
  fi
  
  if [[ "${DATABASE_TYPE}" == "postgresql" ]]; then
    local db_container=$(docker ps -a --filter "name=math-master-postgres" --format "{{.Names}}" 2>/dev/null)
    
    if [[ -n "${db_container}" ]]; then
      info "Removing database container..."
      docker rm -f "${db_container}" 2>/dev/null || true
      
      info "Removing database volume..."
      docker volume rm math-master-postgres-data 2>/dev/null || true
      
      success "Database reset. New database will be created on next startup."
      return 0
    else
      err "Database container not found"
      return 1
    fi
  else
    err "Reset not supported for database type: ${DATABASE_TYPE}"
    return 1
  fi
}

# ================= DATABASE BACKUP =================
backup_database() {
  title "DATABASE BACKUP"
  
  local backup_dir="${PROJECT_ROOT}/backups"
  local timestamp=$(date +%Y%m%d_%H%M%S)
  local backup_file="${backup_dir}/math-master-backup-${timestamp}.sql"
  
  mkdir -p "${backup_dir}"
  
  if [[ "${DATABASE_TYPE}" == "postgresql" ]]; then
    local db_container=$(docker ps --filter "name=math-master-postgres" --format "{{.Names}}" 2>/dev/null)
    
    if [[ -z "${db_container}" ]]; then
      err "Database container not running"
      return 1
    fi
    
    info "Backing up PostgreSQL database to ${backup_file}..."
    
    docker exec "${db_container}" pg_dump -U postgres > "${backup_file}" 2>/dev/null
    
    if [[ $? -eq 0 ]]; then
      local size=$(du -h "${backup_file}" | cut -f1)
      success "Database backup created successfully (${size}): ${backup_file}"
      return 0
    else
      err "Database backup failed"
      rm -f "${backup_file}"
      return 1
    fi
  else
    err "Backup not supported for database type: ${DATABASE_TYPE}"
    return 1
  fi
}

# ================= DATABASE RESTORE =================
restore_database() {
  title "DATABASE RESTORE"
  
  local backup_dir="${PROJECT_ROOT}/backups"
  
  if [[ ! -d "${backup_dir}" ]]; then
    err "Backup directory not found: ${backup_dir}"
    return 1
  fi
  
  local backups=($(ls -t "${backup_dir}"/*.sql 2>/dev/null || echo ""))
  
  if [[ ${#backups[@]} -eq 0 ]]; then
    err "No backup files found"
    return 1
  fi
  
  info "Available backups:"
  select backup_file in "${backups[@]}"; do
    if [[ -n "${backup_file}" ]]; then
      break
    fi
  done
  
  warn "This will overwrite current database data!"
  ask "Type 'yes' to confirm restore from: $(basename "${backup_file}"): "
  read confirmation
  
  if [[ "${confirmation}" != "yes" ]]; then
    info "Restore cancelled"
    return 0
  fi
  
  if [[ "${DATABASE_TYPE}" == "postgresql" ]]; then
    local db_container=$(docker ps --filter "name=math-master-postgres" --format "{{.Names}}" 2>/dev/null)
    
    if [[ -z "${db_container}" ]]; then
      err "Database container not running"
      return 1
    fi
    
    info "Restoring database from ${backup_file}..."
    
    cat "${backup_file}" | docker exec -i "${db_container}" psql -U postgres
    
    if [[ $? -eq 0 ]]; then
      success "Database restored successfully"
      return 0
    else
      err "Database restore failed"
      return 1
    fi
  else
    err "Restore not supported for database type: ${DATABASE_TYPE}"
    return 1
  fi
}

# ================= SHOW MIGRATION STATUS =================
show_migration_status() {
  title "MIGRATION STATUS"
  
  info "Application Configuration:"
  echo "  DDL Auto:     update (Hibernate manages schema automatically)"
  echo "  Database:     ${DATABASE_TYPE}"
  echo
  
  info "To apply migrations:"
  echo "  1. Update entity classes in src/main/java/.../entity/"
  echo "  2. Restart the application"
  echo "  3. Hibernate will automatically update the schema"
  echo
  
  info "For custom migrations:"
  echo "  - Consider using Flyway or Liquibase"
  echo "  - Add migration scripts to src/main/resources/db/migration/"
}

# ================= EXPORT FUNCTIONS FOR SOURCING =================
export -f init_database
export -f wait_for_database
export -f show_database_info
export -f reset_database
export -f backup_database
export -f restore_database
export -f show_migration_status
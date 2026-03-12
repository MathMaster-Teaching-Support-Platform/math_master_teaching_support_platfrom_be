#!/bin/bash
# cleanup.sh - Repository cleanup utilities

# ================= CLEANUP LOCAL BRANCHES =================
cleanup_local_branches() {
  local repo="$1"
  local module_name=$(basename "$repo")
  
  if [[ ! -d "$repo" ]]; then
    err "Repository not found: $repo"
    return 1
  fi
  
  title "CLEANUP BRANCHES: $module_name"
  
  cd "$repo" || return 1
  
  # Get current branch
  local current_branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null)
  
  # Checkout default branch first
  info "Checking out default branch: ${DEFAULT_BRANCH}"
  git checkout "${DEFAULT_BRANCH}" &>/dev/null
  
  if [[ $? -ne 0 ]]; then
    warn "Default branch not found, trying to fetch..."
    git fetch origin "${DEFAULT_BRANCH}:${DEFAULT_BRANCH}" &>/dev/null
    
    if [[ $? -ne 0 ]]; then
      err "Cannot checkout default branch: ${DEFAULT_BRANCH}"
      cd - >/dev/null
      return 1
    fi
    
    git checkout "${DEFAULT_BRANCH}" &>/dev/null
  fi
  
  # Get list of local branches (excluding default and current)
  local branches_to_delete=()
  
  while IFS= read -r branch; do
    # Skip default branch and detached HEAD
    if [[ "$branch" != "${DEFAULT_BRANCH}" ]] && [[ "$branch" != "HEAD" ]]; then
      branches_to_delete+=("$branch")
    fi
  done < <(git branch --format='%(refname:short)')
  
  if [[ ${#branches_to_delete[@]} -eq 0 ]]; then
    success "No branches to delete"
    echo
    ask "Delete stashes for $module_name? (Y/n): "
    read DELETE_STASH
    if [[ -z "$DELETE_STASH" || "$DELETE_STASH" =~ ^[Yy]$ ]]; then
      cleanup_stashes "$repo"
    fi
    cd - >/dev/null
    return 0
  fi
  
  # Show branches to be deleted
  echo
  warn "The following branches will be deleted:"
  for branch in "${branches_to_delete[@]}"; do
    echo -e "  ${RED}●${NC} $branch"
  done
  
  echo
  info "Total branches to delete: ${#branches_to_delete[@]}"
  
  # Confirm deletion
  if ! confirm "Delete these branches?"; then
    info "Cleanup cancelled"
    cd - >/dev/null
    return 0
  fi
  
  # Delete branches
  local deleted=0
  local failed=0
  
  # Get current branch again (safety)
  local current_branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null)
  for branch in "${branches_to_delete[@]}"; do
    # Skip default branch, current branch, and HEAD
    if [[ "$branch" == "$current_branch" ]] || [[ "$branch" == "$DEFAULT_BRANCH" ]] || [[ "$branch" == "HEAD" ]]; then
      continue
    fi
    git branch -D "$branch"
    if [[ $? -eq 0 ]]; then
      success "Deleted: $branch"
      deleted=$((deleted+1))
      log_info "Deleted branch: $branch from $module_name"
    else
      err "Failed to delete: $branch"
      failed=$((failed+1))
    fi
  done
  
  cd - >/dev/null

  echo
  ask "Delete stashes for $module_name? (Y/n): "
  read DELETE_STASH
  if [[ -z "$DELETE_STASH" || "$DELETE_STASH" =~ ^[Yy]$ ]]; then
    cleanup_stashes "$repo"
  fi

  echo
  success "Cleanup complete for $module_name"
  info "Deleted: $deleted branches"

  if [[ $failed -gt 0 ]]; then
    warn "Failed to delete: $failed branches"
  fi

  return 0
}

# ================= CLEANUP STASHES =================
cleanup_stashes() {
  local repo="$1"
  local module_name=$(basename "$repo")
  
  if [[ ! -d "$repo" ]]; then
    err "Repository not found: $repo"
    return 1
  fi
  
  title "CLEANUP STASHES: $module_name"
  
  cd "$repo" || return 1
  
  # Get stash list
  local stash_count=$(git stash list | wc -l)
  
  if [[ $stash_count -eq 0 ]]; then
    success "No stashes to clean"
    cd - >/dev/null
    return 0
  fi
  
  echo
  info "Found $stash_count stashes:"
  git stash list
  
  echo
  opt "1. Delete all stashes"
  opt "2. Delete auto-generated stashes only"
  opt "3. Keep all stashes"
  echo
  ask "Select option (1-3): "
  read STASH_CHOICE
  
  case "$STASH_CHOICE" in
    1)
      if confirm "Delete ALL stashes?"; then
        git stash clear
        success "All stashes deleted"
        log_info "Cleared all stashes in $module_name"
      fi
      ;;
    2)
      # Delete only auto-generated stashes
      local deleted=0
      while IFS= read -r stash_ref; do
        local stash_msg=$(git stash list | grep "$stash_ref" | cut -d':' -f3-)
        if [[ "$stash_msg" =~ ${STASH_PREFIX} ]]; then
          git stash drop "$stash_ref" &>/dev/null
          if [[ $? -eq 0 ]]; then
            success "Deleted: $stash_ref"
            ((deleted++))
          fi
        fi
      done < <(git stash list | cut -d':' -f1)
      
      info "Deleted $deleted auto-generated stashes"
      ;;
    3)
      info "Keeping all stashes"
      ;;
    *)
      err "Invalid choice"
      ;;
  esac
  
  cd - >/dev/null
  return 0
}

# ================= CLEANUP UNTRACKED FILES =================
cleanup_untracked() {
  local repo="$1"
  local module_name=$(basename "$repo")
  
  if [[ ! -d "$repo" ]]; then
    err "Repository not found: $repo"
    return 1
  fi
  
  title "CLEANUP UNTRACKED FILES: $module_name"
  
  cd "$repo" || return 1
  
  # Show untracked files
  local untracked=$(git ls-files --others --exclude-standard)
  
  if [[ -z "$untracked" ]]; then
    success "No untracked files"
    cd - >/dev/null
    return 0
  fi
  
  echo
  warn "Untracked files found:"
  echo "$untracked" | while read -r file; do
    echo -e "  ${YELLOW}●${NC} $file"
  done
  
  echo
  err "WARNING: This will permanently delete these files!"
  
  if confirm "Delete all untracked files?"; then
    git clean -fd
    success "Untracked files deleted"
    log_warn "Deleted untracked files in $module_name"
  else
    info "Cleanup cancelled"
  fi
  
  cd - >/dev/null
  return 0
}

# ================= COMPREHENSIVE CLEANUP =================
comprehensive_cleanup() {
  local repo="$1"
  local module_name=$(basename "$repo")
  
  title "COMPREHENSIVE CLEANUP: $module_name"
  
  echo
  warn "This will perform:"
  echo -e "  ${YELLOW}●${NC} Delete local branches (except ${DEFAULT_BRANCH})"
  echo -e "  ${YELLOW}●${NC} Clean auto-generated stashes"
  echo -e "  ${YELLOW}●${NC} Remove untracked files"
  echo
  
  if ! confirm "Proceed with comprehensive cleanup?"; then
    info "Cleanup cancelled"
    return 0
  fi
  
  # Cleanup branches
  cleanup_local_branches "$repo"
  
  echo
  press_any_key
  
  # Cleanup stashes
  cleanup_stashes "$repo"
  
  echo
  press_any_key
  
  # Cleanup untracked
  cleanup_untracked "$repo"
  
  success "Comprehensive cleanup complete for $module_name"
}

# ================= CLEANUP ALL REPOS =================
cleanup_all_repos() {
  title "CLEANUP ALL REPOSITORIES"
  
  local repos=(
    "${REPO_FLYWAY}"
    "${REPO_ILC_MODULES}"
    "${REPO_WES_MODULES}"
    "${REPO_WES_API}"
    "${REPO_WES_BATCH}"
  )
  
  err "WARNING: This will cleanup all repositories!"
  
  if ! confirm "Continue?"; then
    info "Operation cancelled"
    return 0
  fi
  
  for repo in "${repos[@]}"; do
    echo
    cleanup_local_branches "$repo"
    press_any_key
  done
  
  success "All repositories cleaned up"
}
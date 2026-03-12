#!/bin/bash
# status_checker.sh - Repository status checking utilities

# ================= CHECK SINGLE REPO STATUS =================
check_single_repo_status() {
  local repo="$1"
  local module_name=$(basename "$repo")
  
  if [[ ! -d "$repo" ]]; then
    printf "%-30s ${RED}%-15s${NC} ${GRAY}%-10s${NC} ${GRAY}%s${NC}\n" \
      "$module_name" "NOT FOUND" "-" "-"
    return 1
  fi
  
  cd "$repo" || return 1
  
  # Get current branch
  local current_branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null)
  if [[ -z "$current_branch" ]]; then
    printf "%-30s ${RED}%-15s${NC} ${GRAY}%-10s${NC} ${GRAY}%s${NC}\n" \
      "$module_name" "ERROR" "-" "Not a git repo"
    cd - >/dev/null
    return 1
  fi
  
  # Check for uncommitted changes
  local status_icon=""
  if git diff-index --quiet HEAD -- 2>/dev/null; then
    status_icon="${GREEN}✓${NC}"
  else
    status_icon="${YELLOW}●${NC}"
  fi
  
  # Check for unpushed commits
  git fetch origin &>/dev/null
  local unpushed=$(git log origin/"${current_branch}"..HEAD --oneline 2>/dev/null | wc -l)
  local unpushed_info=""
  if [[ $unpushed -gt 0 ]]; then
    unpushed_info="${YELLOW}${unpushed} unpushed${NC}"
  else
    unpushed_info="${GREEN}synced${NC}"
  fi
  
  # Check if behind remote
  local behind=$(git log HEAD..origin/"${current_branch}" --oneline 2>/dev/null | wc -l)
  if [[ $behind -gt 0 ]]; then
    unpushed_info="${unpushed_info} ${RED}(${behind} behind)${NC}"
  fi
  
  # Status text
  local status_text=""
  if git diff-index --quiet HEAD -- 2>/dev/null; then
    status_text="${GREEN}clean${NC}"
  else
    local changed_files=$(git diff --name-only | wc -l)
    status_text="${YELLOW}${changed_files} changed${NC}"
  fi
  
  printf "%-30s ${CYAN}%-20s${NC} %-20b %-30b\n" \
    "$module_name" "$current_branch" "$status_text" "$unpushed_info"
  
  cd - >/dev/null
  return 0
}

# ================= CHECK MULTIPLE REPOS =================
check_repos_status() {
  local repos=("$@")
  
  echo
  print_table_header "Repository" "Branch" "Status" "Sync Status"
  
  for repo in "${repos[@]}"; do
    check_single_repo_status "$repo"
  done
  
  separator "─"
  echo
}

# ================= DETAILED STATUS =================
show_detailed_status() {
  local repo="$1"
  local module_name=$(basename "$repo")
  
  if [[ ! -d "$repo" ]]; then
    err "Repository not found: $module_name"
    return 1
  fi
  
  title "DETAILED STATUS: $module_name"
  echo
  
  cd "$repo" || return 1
  
  # Current branch
  local current_branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null)
  echo -e "  ${CYAN}Current Branch:${NC} $current_branch"
  
  # Last commit
  local last_commit=$(git log -1 --pretty=format:"%h - %s (%cr)" 2>/dev/null)
  echo -e "  ${CYAN}Last Commit:${NC} $last_commit"
  
  # Modified files
  echo
  echo -e "  ${CYAN}Modified Files:${NC}"
  local modified_files=$(git diff --name-only)
  if [[ -z "$modified_files" ]]; then
    echo -e "    ${GREEN}None${NC}"
  else
    echo "$modified_files" | while read -r file; do
      echo -e "    ${YELLOW}●${NC} $file"
    done
  fi
  
  # Untracked files
  echo
  echo -e "  ${CYAN}Untracked Files:${NC}"
  local untracked_files=$(git ls-files --others --exclude-standard)
  if [[ -z "$untracked_files" ]]; then
    echo -e "    ${GREEN}None${NC}"
  else
    echo "$untracked_files" | while read -r file; do
      echo -e "    ${GRAY}○${NC} $file"
    done
  fi
  
  # Stashes
  echo
  echo -e "  ${CYAN}Stashes:${NC}"
  local stash_count=$(git stash list | wc -l)
  if [[ $stash_count -eq 0 ]]; then
    echo -e "    ${GREEN}None${NC}"
  else
    git stash list | head -5 | while read -r stash; do
      echo -e "    ${MAGENTA}●${NC} $stash"
    done
    if [[ $stash_count -gt 5 ]]; then
      echo -e "    ${GRAY}... and $((stash_count - 5)) more${NC}"
    fi
  fi
  
  # Branches
  echo
  echo -e "  ${CYAN}Local Branches:${NC}"
  git branch --format='%(refname:short)' | while read -r branch; do
    if [[ "$branch" == "$current_branch" ]]; then
      echo -e "    ${GREEN}● $branch${NC} (current)"
    else
      echo -e "    ${GRAY}○ $branch${NC}"
    fi
  done
  
  cd - >/dev/null
  echo
}

# ================= CHECK ALL REPOS SUMMARY =================
check_all_repos_summary() {
  title "ALL REPOSITORIES STATUS SUMMARY"
  echo
  
  local all_repos=(
    "${REPO_VRF}"
    "${REPO_FLYWAY}"
    "${REPO_ILC_MODULES}"
    "${REPO_WES_MODULES}"
    "${REPO_WES_API}"
    "${REPO_WES_BATCH}"
  )
  
  local total=0
  local clean=0
  local dirty=0
  local not_found=0
  
  for repo in "${all_repos[@]}"; do
    ((total++))
    
    if [[ ! -d "$repo" ]]; then
      ((not_found++))
      continue
    fi
    
    cd "$repo" || continue
    
    if git diff-index --quiet HEAD -- 2>/dev/null; then
      ((clean++))
    else
      ((dirty++))
    fi
    
    cd - >/dev/null
  done
  
  echo -e "  Total Repositories:  ${CYAN}${total}${NC}"
  echo -e "  Clean:               ${GREEN}${clean}${NC}"
  echo -e "  Modified:            ${YELLOW}${dirty}${NC}"
  echo -e "  Not Found:           ${RED}${not_found}${NC}"
  echo
  
  if [[ $dirty -gt 0 ]]; then
    warn "Some repositories have uncommitted changes"
  fi
  
  if [[ $not_found -gt 0 ]]; then
    err "Some repositories are not found"
  fi
}

# ================= PRE-CHECKOUT VALIDATION =================
validate_before_checkout() {
  local repos=("$@")
  local issues=()
  
  for repo in "${repos[@]}"; do
    local module_name=$(basename "$repo")
    
    if [[ ! -d "$repo" ]]; then
      issues+=("${module_name}: Repository not found")
      continue
    fi
    
    if has_uncommitted_changes "$repo"; then
      issues+=("${module_name}: Has uncommitted changes")
    fi
    
    local current_branch=$(get_current_branch "$repo")
    if has_unpushed_commits "$repo" "$current_branch"; then
      issues+=("${module_name}: Has unpushed commits on ${current_branch}")
    fi
  done
  
  if [[ ${#issues[@]} -gt 0 ]]; then
    warn "Issues detected before checkout:"
    echo
    for issue in "${issues[@]}"; do
      echo -e "  ${YELLOW}●${NC} $issue"
    done
    echo
    return 1
  fi
  
  return 0
}
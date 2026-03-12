#!/bin/bash
# git_utils.sh - Git operations and utilities

# ================= VERSION PARSING =================
parse_version_to_branch() {
  local version="$1"
  
  if [[ "$version" =~ ^3\.1$ ]]; then
    echo "release/v3.1"
  elif [[ "$version" =~ ^3\.0$ ]]; then
    echo "develop/v3.0"
  elif [[ "$version" =~ ^2\.([0-9]+)\.([0-9]+)$ ]]; then
    echo "fix/v${version}"
  else
    echo ""
  fi
}

# ================= FALLBACK BRANCH =================
get_fallback_branch() {
  local branch="$1"
  
  if [[ "$branch" =~ ^fix/v2\.([0-9]+)\.([0-9]+)$ ]]; then
    local major="${BASH_REMATCH[1]}"
    local minor="${BASH_REMATCH[2]}"
    local minor_base="${minor%?}0"
    echo "fix/v2.${major}.${minor_base}"
  else
    echo ""
  fi
}

# ================= CHECK IF BRANCH EXISTS =================
branch_exists() {
  local repo="$1"
  local branch="$2"
  
  cd "$repo" || return 1
  git fetch origin &>/dev/null
  git show-ref --verify --quiet "refs/heads/${branch}" || \
  git show-ref --verify --quiet "refs/remotes/origin/${branch}"
  local result=$?
  cd - >/dev/null
  return $result
}

# ================= GET CURRENT BRANCH =================
get_current_branch() {
  local repo="$1"
  
  cd "$repo" || return 1
  local branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null)
  cd - >/dev/null
  echo "$branch"
}

# ================= CHECK FOR UNCOMMITTED CHANGES =================
has_uncommitted_changes() {
  local repo="$1"
  
  cd "$repo" || return 1
  git diff-index --quiet HEAD --
  local result=$?
  cd - >/dev/null
  
  # Return 0 if there ARE changes (inverse of git's return)
  [[ $result -ne 0 ]]
}

# ================= CHECK FOR UNPUSHED COMMITS =================
has_unpushed_commits() {
  local repo="$1"
  local branch="$2"
  
  cd "$repo" || return 1
  git fetch origin &>/dev/null
  
  local local_commit=$(git rev-parse HEAD 2>/dev/null)
  local remote_commit=$(git rev-parse origin/"${branch}" 2>/dev/null)
  
  cd - >/dev/null
  
  [[ "$local_commit" != "$remote_commit" ]]
}

# ================= CREATE AUTO STASH =================
create_auto_stash() {
  local repo="$1"
  local branch="$2"
  
  local stash_name="${STASH_PREFIX}-${branch}-$(date +%Y%m%d-%H%M%S)"
  
  cd "$repo" || return 1
  git stash push -m "$stash_name" &>/dev/null
  local result=$?
  cd - >/dev/null
  
  if [[ $result -eq 0 ]]; then
    log_stash "$(basename "$repo")" "$stash_name" "$branch"
    return 0
  else
    return 1
  fi
}

# ================= CHECKOUT AND PULL =================
checkout_and_pull_branch() {
  local repo="$1"
  local branch="$2"
  local fallback_branch="$3"
  local summary_var="$4"
  
  local module_name=$(basename "$repo")
  local current_branch=$(get_current_branch "$repo")
  
  # Check if directory exists
  if [[ ! -d "$repo" ]]; then
    err "Directory $module_name not found"
    eval "$summary_var+=(\"$module_name|$branch|fail|Directory not found\")"
    return 1
  fi
  
  cd "$repo" || return 1
  
  # Fetch latest
  git fetch origin &>/dev/null
  
  # Check if branch exists
  local target_branch="$branch"
  if ! git show-ref --verify --quiet "refs/remotes/origin/${branch}"; then
    if [[ -n "$fallback_branch" ]] && [[ "${ENABLE_BRANCH_FALLBACK}" == "true" ]]; then
      warn "Branch ${branch} not found in ${module_name}"
      
      if git show-ref --verify --quiet "refs/remotes/origin/${fallback_branch}"; then
        info "Fallback branch ${fallback_branch} found"
        
        if confirm "Use fallback branch ${fallback_branch}?"; then
          target_branch="$fallback_branch"
        else
          eval "$summary_var+=(\"$module_name|$branch|fail|User declined fallback\")"
          cd - >/dev/null
          return 1
        fi
      else
        err "Fallback branch ${fallback_branch} also not found"
        eval "$summary_var+=(\"$module_name|$branch|fail|Branch not found\")"
        cd - >/dev/null
        return 1
      fi
    else
      err "Branch ${branch} not found in ${module_name}"
      eval "$summary_var+=(\"$module_name|$branch|fail|Branch not found\")"
      cd - >/dev/null
      return 1
    fi
  fi
  
  # Handle uncommitted changes
  if has_uncommitted_changes "$repo"; then
    warn "Uncommitted changes detected in ${module_name}"
    echo "Conflict action? [stash/force/abort] (default: ${DEFAULT_CONFLICT_ACTION}): "
    read user_action
    action="${user_action:-$DEFAULT_CONFLICT_ACTION}"

    case "$action" in
      stash)
        if confirm "Stash changes?"; then
          if create_auto_stash "$repo" "$current_branch"; then
            success "Changes stashed"
            log_conflict "$module_name" "$current_branch" "stash" "Auto-stashed before checkout"
          else
            err "Failed to stash changes"
            eval "$summary_var+=(\"$module_name|$branch|fail|Stash failed\")"
            cd - >/dev/null
            return 1
          fi
        else
          eval "$summary_var+=(\"$module_name|$branch|fail|User declined stash\")"
          cd - >/dev/null
          return 1
        fi
        ;;
      force)
        if confirm "Force checkout (discard changes)?"; then
          git checkout -f "$target_branch" &>/dev/null
          log_conflict "$module_name" "$current_branch" "force" "Forced checkout, changes discarded"
        else
          eval "$summary_var+=(\"$module_name|$branch|fail|User declined force\")"
          cd - >/dev/null
          return 1
        fi
        ;;
      abort)
        err "Cannot checkout with uncommitted changes"
        eval "$summary_var+=(\"$module_name|$branch|fail|Uncommitted changes\")"
        cd - >/dev/null
        return 1
        ;;
    esac
  else
    # Normal checkout
    git checkout "$target_branch" &>/dev/null
    if [[ $? -ne 0 ]]; then
      err "Checkout failed for ${module_name}"
      eval "$summary_var+=(\"$module_name|$target_branch|fail|Checkout failed\")"
      cd - >/dev/null
      return 1
    fi
  fi
  
  # Pull latest changes
  git pull origin "$target_branch" &>/dev/null
  if [[ $? -ne 0 ]]; then
    warn "Pull failed for ${module_name}, trying reset"
    
    if confirm "Reset to origin/${target_branch}?"; then
      git reset --hard origin/"$target_branch" &>/dev/null
      log_conflict "$module_name" "$target_branch" "reset" "Reset to remote"
    else
      eval "$summary_var+=(\"$module_name|$target_branch|fail|Pull failed\")"
      cd - >/dev/null
      return 1
    fi
  fi
  
  log_checkout "$module_name" "$current_branch" "$target_branch"
  success "Checked out ${module_name} to ${target_branch}"
  
  cd - >/dev/null
  return 0
}

# ================= GET REPO STATUS =================
get_repo_status() {
  local repo="$1"
  local module_name=$(basename "$repo")
  
  if [[ ! -d "$repo" ]]; then
    echo "not_found"
    return
  fi
  
  cd "$repo" || return
  
  local current_branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null)
  local has_changes=$(git diff-index --quiet HEAD -- && echo "clean" || echo "dirty")
  local unpushed=$(git log origin/"${current_branch}"..HEAD --oneline 2>/dev/null | wc -l)
  
  cd - >/dev/null
  
  echo "${current_branch}|${has_changes}|${unpushed}"
}

# ================= LIST LOCAL BRANCHES =================
list_local_branches() {
  local repo="$1"
  
  cd "$repo" || return 1
  git branch --format='%(refname:short)'
  cd - >/dev/null
}

# ================= DELETE LOCAL BRANCH =================
delete_local_branch() {
  local repo="$1"
  local branch="$2"
  
  cd "$repo" || return 1
  git branch -D "$branch" &>/dev/null
  local result=$?
  cd - >/dev/null
  
  return $result
}
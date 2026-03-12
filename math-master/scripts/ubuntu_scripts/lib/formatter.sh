#!/bin/bash
# formatter.sh - Code formatting utilities
set -eu

PRETTIER_3="prettier --plugin=$(mise where npm:prettier-plugin-java)/lib/node_modules/prettier-plugin-java/dist/index.js --no-config --no-editorconfig --log-level error --cache --cache-strategy metadata --write"
PRETTIER_2='npx
  --quiet
  -p prettier@2.8.0
  -p prettier-plugin-java@2.0.0
  prettier
  --no-config
  --no-editorconfig
  --loglevel error
  --cache
  --cache-strategy metadata
  --write'

# ================= GET CURRENT VERSION FROM BRANCH =================
get_current_version() {
  local branch
  branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "")
  if [[ -z "$branch" ]]; then
    warn "Could not determine git branch. Defaulting version to 3.0."
    echo "3.0"
    return
  fi
  # Extract version part after first / and before second / (if any)
  local version
  version=$(echo "$branch" | sed -E 's|^[^/]*/([^/]+).*|\1|')
  # Normalize: if version starts with v, keep it, else add v
  if [[ ! "$version" =~ ^v ]]; then
    version="v$version"
  fi
  echo "$version"
}

# ================= SELECT PRETTIER BASED ON VERSION =================
select_prettier_by_version() {
  local version="$1"
  local major
  # Extract major version number (e.g., v2.8.60 -> 2, v3.0 -> 3)
  major=$(echo "$version" | sed -E 's/^v([0-9]+).*/\1/')
  if [[ "$major" == "2" ]]; then
    echo "$PRETTIER_2"
  else
    echo "$PRETTIER_3"
  fi
}

# ================= FORMAT SINGLE MODULE =================
format_module() {  
  local module_dir="$1"
  local module_name
  module_name="$(basename "$module_dir")"

  if [[ ! -d "$module_dir" ]]; then
    err "Module directory not found: $module_dir"
    return 1
  fi

  title "FORMAT MODULE: $module_name"

  cd "$module_dir" || return 1

  local -a src_dirs=()
  if [[ "$module_dir" == "$REPO_ILC_MODULES" ]]; then
    mapfile -t src_dirs < <(find . -type d -name "src" 2>/dev/null)
  elif [[ "$module_dir" == "$REPO_WES_MODULES" ]]; then
    mapfile -t src_dirs < <(find . -type d -path "*/wes-shared-logic/src" 2>/dev/null)
  elif [[ "$module_dir" == "$REPO_WES_API" || "$module_dir" == "$REPO_WES_BATCH" ]]; then
    mapfile -t src_dirs < <(find . -type d -path "*/fr-wes-api/src" 2>/dev/null)
  else
    warn "Unknown repo type for: $module_dir. Defaulting to -name 'src'."
    mapfile -t src_dirs < <(find . -type d -name "src" 2>/dev/null)
  fi

  if [[ "${#src_dirs[@]}" -eq 0 ]]; then
    warn "No src directories found in $module_name"
    cd - >/dev/null
    return 1
  fi

  info "Found ${#src_dirs[@]} src directories"

  local version
  version=$(get_current_version)
  local PRETTIER
  PRETTIER=$(select_prettier_by_version "$version")

  for src_dir in "${src_dirs[@]}"; do
    if [[ -d "$src_dir" ]]; then
      info "Formatting: $src_dir (using $version)"
      parent_dir=$(dirname "$src_dir")
      pushd "$parent_dir" >/dev/null
      command -v mise >/dev/null && eval "$(mise env)"
      eval $PRETTIER src
      popd >/dev/null
    fi
  done
  
  cd - >/dev/null
  
  echo
  success "Formatting complete for $module_name"
  
  return 0
}
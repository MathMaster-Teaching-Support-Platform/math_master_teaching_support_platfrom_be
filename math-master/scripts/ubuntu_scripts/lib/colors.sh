#!/bin/bash
# colors.sh - Color definitions for terminal output

# ================= COLOR CODES =================
if [[ "${USE_COLOR_OUTPUT}" == "true" ]]; then
  # Regular colors
  BLACK='\033[0;30m'
  RED='\033[0;31m'
  GREEN='\033[0;32m'
  YELLOW='\033[1;33m'
  BLUE='\033[1;34m'
  MAGENTA='\033[0;35m'
  CYAN='\033[0;36m'
  WHITE='\033[0;37m'
  
  # Bold colors
  BOLD_BLACK='\033[1;30m'
  BOLD_RED='\033[1;31m'
  BOLD_GREEN='\033[1;32m'
  BOLD_YELLOW='\033[1;33m'
  BOLD_BLUE='\033[1;34m'
  BOLD_MAGENTA='\033[1;35m'
  BOLD_CYAN='\033[1;36m'
  BOLD_WHITE='\033[1;37m'
  
  # Background colors
  BG_BLACK='\033[40m'
  BG_RED='\033[41m'
  BG_GREEN='\033[42m'
  BG_YELLOW='\033[43m'
  BG_BLUE='\033[44m'
  BG_MAGENTA='\033[45m'
  BG_CYAN='\033[46m'
  BG_WHITE='\033[47m'
  
  # Special formatting
  GRAY='\033[0;90m'
  BOLD='\033[1m'
  DIM='\033[2m'
  ITALIC='\033[3m'
  UNDERLINE='\033[4m'
  BLINK='\033[5m'
  REVERSE='\033[7m'
  HIDDEN='\033[8m'
  
  # Reset
  NC='\033[0m' # No Color
else
  # No colors if disabled
  BLACK=''
  RED=''
  GREEN=''
  YELLOW=''
  BLUE=''
  MAGENTA=''
  CYAN=''
  WHITE=''
  BOLD_BLACK=''
  BOLD_RED=''
  BOLD_GREEN=''
  BOLD_YELLOW=''
  BOLD_BLUE=''
  BOLD_MAGENTA=''
  BOLD_CYAN=''
  BOLD_WHITE=''
  BG_BLACK=''
  BG_RED=''
  BG_GREEN=''
  BG_YELLOW=''
  BG_BLUE=''
  BG_MAGENTA=''
  BG_CYAN=''
  BG_WHITE=''
  GRAY=''
  BOLD=''
  DIM=''
  ITALIC=''
  UNDERLINE=''
  BLINK=''
  REVERSE=''
  HIDDEN=''
  NC=''
fi

# ================= SEMANTIC COLORS =================
COLOR_SUCCESS="${GREEN}"
COLOR_ERROR="${RED}"
COLOR_WARNING="${YELLOW}"
COLOR_INFO="${CYAN}"
COLOR_DEBUG="${GRAY}"
COLOR_TITLE="${BLUE}"
COLOR_OPTION="${GREEN}"
COLOR_PROMPT="${CYAN}"
COLOR_HINT="${GRAY}"

# ================= ICONS =================
ICON_SUCCESS="✓"
ICON_ERROR="✖"
ICON_WARNING="⚠"
ICON_INFO="ℹ"
ICON_ARROW="▶"
ICON_CHECK="●"
ICON_CROSS="✗"
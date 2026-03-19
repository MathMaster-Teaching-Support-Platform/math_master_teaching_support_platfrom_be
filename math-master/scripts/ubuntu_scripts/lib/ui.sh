#!/bin/bash
# ui.sh - UI utilities and display functions

# ================= BANNER =================
print_banner() {
  echo -e "${BLUE}"
  cat << "EOF"
╔═══════════════════════════════════════════════════════════╗
║                                                           ║
║   ███████╗██████╗     ███╗   ███╗ █████╗ ███╗   ██╗       ║
║   ██╔════╝██╔══██╗    ████╗ ████║██╔══██╗████╗  ██║       ║
║   █████╗  ██████╔╝    ██╔████╔██║███████║██╔██╗ ██║       ║
║   ██╔══╝  ██╔══██╗    ██║╚██╔╝██║██╔══██║██║╚██╗██║       ║
║   ██║     ██║  ██║    ██║ ╚═╝ ██║██║  ██║██║ ╚████║       ║
║   ╚═╝     ╚═╝  ╚═╝    ╚═╝     ╚═╝╚═╝  ╚═╝╚═╝  ╚═══╝       ║
║                                                           ║
║           Framework Repository Manager v2.0               ║
║                                                           ║
╚═══════════════════════════════════════════════════════════╝
EOF
  echo -e "${NC}"
}

# ================= BOX =================
print_box() {
  local msg="$1"
  local color="${2:-$BLUE}"
  local len=${#msg}
  local border_len=$((len + 4))
  local border=$(printf "═%.0s" $(seq 1 $border_len))
  
  echo -e "${color}╔${border}╗${NC}"
  echo -e "${color}║  ${msg}  ║${NC}"
  echo -e "${color}╚${border}╝${NC}"
}

# ================= TITLE =================
title() {
  local text="$1"
  echo
  echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo -e "${BLUE}${ICON_ARROW}${ICON_ARROW} ${text} ${NC}"
  echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

# ================= MESSAGE FUNCTIONS =================
opt() {
  echo -e "  ${COLOR_OPTION}${1}${NC}"
}

ask() {
  echo -ne "${COLOR_PROMPT}${1}${NC}"
}

warn() {
  echo -e "${COLOR_WARNING}${ICON_WARNING} ${1}${NC}"
}

err() {
  echo -e "${COLOR_ERROR}${ICON_ERROR} ${1}${NC}"
}

success() {
  echo -e "${COLOR_SUCCESS}${ICON_SUCCESS} ${1}${NC}"
}

info() {
  echo -e "${COLOR_INFO}${ICON_INFO} ${1}${NC}"
}

hint() {
  echo -e "${COLOR_HINT}${ICON_INFO} ${1}${NC}"
}

debug() {
  if [[ "${DEBUG_MODE:-false}" == "true" ]]; then
    echo -e "${COLOR_DEBUG}[DEBUG] ${1}${NC}"
  fi
}

# ================= SEPARATOR =================
separator() {
  local char="${1:--}"
  local width="${2:-60}"
  printf "${GRAY}%${width}s${NC}\n" | tr ' ' "$char"
}

# ================= PROGRESS BAR =================
progress_bar() {
  local current=$1
  local total=$2
  local width=50
  local percentage=$((current * 100 / total))
  local filled=$((current * width / total))
  local empty=$((width - filled))
  
  printf "\r${CYAN}["
  printf "%${filled}s" | tr ' ' '█'
  printf "%${empty}s" | tr ' ' '░'
  printf "] ${percentage}%%${NC}"
  
  if [[ $current -eq $total ]]; then
    echo
  fi
}

# ================= SPINNER =================
spinner() {
  local pid=$1
  local delay=0.1
  local spinstr='|/-\'
  
  while ps -p $pid > /dev/null 2>&1; do
    local temp=${spinstr#?}
    printf " [%c]  " "$spinstr"
    local spinstr=$temp${spinstr%"$temp"}
    sleep $delay
    printf "\b\b\b\b\b\b"
  done
  printf "    \b\b\b\b"
}

# ================= TABLE =================
print_table_header() {
  local cols=("$@")
  local widths=(30 20 10 20)
  
  separator "─"
  printf "${BOLD}"
  for i in "${!cols[@]}"; do
    printf "%-${widths[$i]}s " "${cols[$i]}"
  done
  printf "${NC}\n"
  separator "─"
}

print_table_row() {
  local cols=("$@")
  local widths=(30 20 10 20)
  
  for i in "${!cols[@]}"; do
    printf "%-${widths[$i]}s " "${cols[$i]}"
  done
  printf "\n"
}

# ================= SUMMARY =================
print_summary() {
  local summary_title="$1"
  shift
  local entries=("$@")
  
  echo
  title "$summary_title"
  echo
  
  if [[ ${#entries[@]} -eq 0 ]]; then
    hint "No items to display"
    return
  fi
  
  print_table_header "Module" "Branch" "Status" "Note"
  
  for entry in "${entries[@]}"; do
    IFS='|' read -r mod br st note <<< "$entry"
    
    # Color status
    local status_colored=""
    case "$st" in
      success)
        status_colored="${COLOR_SUCCESS}${st}${NC}"
        ;;
      fail)
        status_colored="${COLOR_ERROR}${st}${NC}"
        ;;
      skip)
        status_colored="${COLOR_WARNING}${st}${NC}"
        ;;
      *)
        status_colored="$st"
        ;;
    esac
    
    printf "%-30s %-20s " "$mod" "$br"
    echo -ne "$status_colored"
    printf "%$((10 - ${#st}))s %-20s\n" "" "$note"
  done
  
  separator "─"
}

# ================= CONFIRMATION =================
confirm() {
  local prompt="$1"
  local default="${2:-Y}"
  
  if [[ "$default" == "Y" ]]; then
    ask "$prompt (Y/n): "
  else
    ask "$prompt (y/N): "
  fi
  
  read response
  
  if [[ "$default" == "Y" ]]; then
    [[ -z "$response" || "$response" =~ ^[Yy]$ ]]
  else
    [[ "$response" =~ ^[Yy]$ ]]
  fi
}

# ================= INPUT =================
read_input() {
  local prompt="$1"
  local default="$2"
  local var_name="$3"
  
  if [[ -n "$default" ]]; then
    ask "$prompt [$default]: "
  else
    ask "$prompt: "
  fi
  
  read input_value
  
  if [[ -z "$input_value" && -n "$default" ]]; then
    input_value="$default"
  fi
  
  eval "$var_name='$input_value'"
}

# ================= PRESS ANY KEY =================
press_any_key() {
  echo
  echo -ne "${GRAY}Press any key to continue...${NC}"
  read -n 1 -s
  echo
}

# ================= CLEAR SCREEN =================
clear_screen() {
  clear
  print_banner
}

# ================= STATUS INDICATOR =================
status_indicator() {
  local status="$1"
  
  case "$status" in
    running)
      echo -e "${YELLOW}●${NC}"
      ;;
    success)
      echo -e "${GREEN}●${NC}"
      ;;
    failed)
      echo -e "${RED}●${NC}"
      ;;
    waiting)
      echo -e "${GRAY}●${NC}"
      ;;
    *)
      echo -e "${GRAY}○${NC}"
      ;;
  esac
}
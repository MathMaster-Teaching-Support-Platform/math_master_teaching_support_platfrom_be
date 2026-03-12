#!/bin/bash
# install.sh - Installation script for Math Master Project Manager
# This script sets up the Math Master manager and creates the 'khoipd_terminal' command

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[1;34m'
NC='\033[0m'

echo -e "${BLUE}"
cat << "EOF"
╔═══════════════════════════════════════════════════════════╗
║                                                           ║
║       MATH MASTER PROJECT MANAGER INSTALLATION            ║
║                                                           ║
║     Spring Boot 3.5.0 + Java 21 Development Helper       ║
║                                                           ║
╚═══════════════════════════════════════════════════════════╝
EOF
echo -e "${NC}"

# Get script directory
INSTALL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo -e "${BLUE}Installation directory: ${INSTALL_DIR}${NC}"
echo

# Check if directories exist
if [[ ! -d "${INSTALL_DIR}/lib" ]] || [[ ! -d "${INSTALL_DIR}/config" ]]; then
  echo -e "${RED}✖ Error: Required directories not found!${NC}"
  echo -e "${YELLOW}Please ensure the following structure exists:${NC}"
  echo "  - lib/"
  echo "  - config/"
  exit 1
fi

# Make main script executable
chmod +x "${INSTALL_DIR}/fr-manager.sh"
echo -e "${GREEN}✓${NC} Made fr-manager.sh executable"

# Make all lib scripts executable
for script in "${INSTALL_DIR}"/lib/*.sh; do
  if [[ -f "$script" ]]; then
    chmod +x "$script"
  fi
done
echo -e "${GREEN}✓${NC} Made all lib scripts executable"

# Make config script executable
chmod +x "${INSTALL_DIR}/config/config.sh"
echo -e "${GREEN}✓${NC} Made config.sh executable"

# Create wrapper script
WRAPPER_SCRIPT="${INSTALL_DIR}/khoipd_terminal"

cat > "${WRAPPER_SCRIPT}" << 'WRAPPER_EOF'
#!/bin/bash
# Math Master Project Manager wrapper script

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
"${SCRIPT_DIR}/fr-manager.sh" "$@"
WRAPPER_EOF

chmod +x "${WRAPPER_SCRIPT}"
echo -e "${GREEN}✓${NC} Created wrapper script: khoipd_terminal"

# Detect shell
SHELL_NAME=$(basename "$SHELL")
RC_FILE=""

case "$SHELL_NAME" in
  bash)
    RC_FILE="${HOME}/.bashrc"
    ;;
  zsh)
    RC_FILE="${HOME}/.zshrc"
    ;;
  *)
    echo -e "${YELLOW}⚠ Warning: Unknown shell ($SHELL_NAME)${NC}"
    echo -e "${YELLOW}  Please manually add the alias to your shell RC file${NC}"
    ;;
esac

# Add alias to shell RC file
if [[ -n "$RC_FILE" ]]; then
  echo
  echo -e "${BLUE}Adding 'khoipd_terminal' command to ${RC_FILE}...${NC}"
  
  # Check if alias already exists
  if grep -q "alias khoipd_terminal=" "$RC_FILE" 2>/dev/null; then
    echo -e "${YELLOW}  Alias 'khoipd_terminal' already exists in ${RC_FILE}${NC}"
    echo -e "${YELLOW}  Removing old alias...${NC}"
    
    # Remove old alias
    sed -i '/alias khoipd_terminal=/d' "$RC_FILE"
  fi
  
  # Add new alias
  echo "" >> "$RC_FILE"
  echo "# Math Master Project Manager - Auto-generated on $(date)" >> "$RC_FILE"
  echo "alias khoipd_terminal='${INSTALL_DIR}/khoipd_terminal'" >> "$RC_FILE"
  
  echo -e "${GREEN}✓${NC} Added 'khoipd_terminal' alias to ${RC_FILE}"
  
  # Also add to current session
  alias khoipd_terminal="${INSTALL_DIR}/khoipd_terminal"
  
  echo
  echo -e "${GREEN}═════════════════════════════════════════════════════════════${NC}"
  echo -e "${GREEN}Installation Complete! 🎉${NC}"
  echo -e "${GREEN}═════════════════════════════════════════════════════════════${NC}"
  echo
  echo -e "${BLUE}To use Math Master Manager:${NC}"
  echo -e "  1. Reload your shell:"
  echo -e "     ${YELLOW}source ${RC_FILE}${NC}"
  echo -e "  2. Or open a new terminal"
  echo -e "  3. Then run:"
  echo -e "     ${GREEN}khoipd_terminal${NC}"
  echo
  echo -e "${BLUE}Or use immediately in this session:${NC}"
  echo -e "  ${GREEN}${INSTALL_DIR}/khoipd_terminal${NC}"
  
else
  echo
  echo -e "${GREEN}═════════════════════════════════════════════════════════════${NC}"
  echo -e "${GREEN}Installation Complete! 🎉${NC}"
  echo -e "${GREEN}═════════════════════════════════════════════════════════════${NC}"
  echo
  echo -e "${YELLOW}Manual setup required:${NC}"
  echo -e "Add this line to your shell RC file (${HOME}/.bashrc, ~/.zshrc, etc):"
  echo
  echo -e "  ${GREEN}alias khoipd_terminal='${INSTALL_DIR}/khoipd_terminal'${NC}"
  echo
  echo -e "Then reload your shell or open a new terminal"
fi

echo
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}CONFIGURATION${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo
echo -e "Configuration file: ${YELLOW}${INSTALL_DIR}/config/config.sh${NC}"
echo
echo -e "Most settings are auto-detected, but review these if needed:"
echo -e "  - ${GREEN}PROJECT_ROOT${NC} - Project directory (auto-detected)"
echo -e "  - ${GREEN}ENVIRONMENT${NC} - Environment type (local, staging, prod)"
echo -e "  - ${GREEN}JAVA_VERSION${NC} - Java version (currently 21)"
echo -e "  - ${GREEN}MAVEN_SKIP_TESTS${NC} - Skip tests during build"
echo -e "  - ${GREEN}REDIS_PASSWORD${NC} - Redis password"
echo -e "  - ${GREEN}APP_PORT${NC} - App port (default 8080)"
echo
echo -e "${BLUE}Next Steps:${NC}"
echo

# Check prerequisites
echo -e "${BLUE}Checking prerequisites:${NC}"

if ! command -v java &> /dev/null; then
  echo -e "${YELLOW}⚠ Java not found${NC} - Install Java 21 or higher"
else
  JAVA_VER=$(java -version 2>&1 | grep -oP 'version "\K[^"]*')
  echo -e "${GREEN}✓ Java found${NC} ($(echo ${JAVA_VER} | cut -d. -f1))"
fi

if ! command -v mvn &> /dev/null; then
  echo -e "${YELLOW}⚠ Maven not found${NC} - Install Maven 3.6+ or higher"
else
  MVN_VER=$(mvn -v 2>&1 | grep "Apache Maven" | grep -oP '\d+\.\d+' | head -1)
  echo -e "${GREEN}✓ Maven found${NC} (${MVN_VER})"
fi

if ! command -v docker &> /dev/null; then
  echo -e "${YELLOW}⚠ Docker not found${NC} - Install Docker for containerization"
else
  DOCKER_VER=$(docker --version | grep -oP '\d+\.\d+' | head -1)
  echo -e "${GREEN}✓ Docker found${NC} (${DOCKER_VER})"
fi

if ! command -v docker-compose &> /dev/null; then
  echo -e "${YELLOW}⚠ Docker Compose not found${NC} - Install Docker Compose v1.29+"
else
  COMPOSE_VER=$(docker-compose --version 2>&1 | grep -oP '\d+\.\d+' | head -1)
  echo -e "${GREEN}✓ Docker Compose found${NC} (${COMPOSE_VER})"
fi

echo
echo -e "${BLUE}Getting Started:${NC}"
echo
echo -e "  1. Setup environment (first time only):"
echo -e "     ${GREEN}khoipd_terminal${NC}"
echo -e "     → Select: 5. Environment Setup"
echo
echo -e "  2. Start services:"
echo -e "     ${GREEN}khoipd_terminal${NC}"
echo -e "     → Select: 2. Docker Compose → 1. Start All Services"
echo
echo -e "  3. Check services:"
echo -e "     ${GREEN}khoipd_terminal${NC}"
echo -e "     → Select: 1. Build Operations → 1. Build Project"
echo
echo -e "  4. Access application:"
echo -e "     http://localhost:8080"
echo
echo -e "  5. For full documentation:"
echo -e "     less ${INSTALL_DIR}/README.md"
echo
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo
echo -e "${GREEN}Happy local developing! 🚀${NC}"
echo
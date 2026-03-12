# ========================================
# Math Master Project Manager - Windows Installation Script
# This script sets up the Math Master PowerShell manager and creates the 'khoipd_terminal_ps' command
# ========================================

param(
    [switch]$AllUsers = $false
)

$InstallDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ManagerScript = Join-Path $InstallDir "manager.ps1"

Write-Host ""
Write-Host "╔═══════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║                                                           ║" -ForegroundColor Cyan
Write-Host "║     MATH MASTER PROJECT MANAGER INSTALLATION (Windows)    ║" -ForegroundColor Cyan
Write-Host "║                                                           ║" -ForegroundColor Cyan
Write-Host "║     Spring Boot 3.5.0 + Java 21 Development Helper       ║" -ForegroundColor Cyan
Write-Host "╚═══════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

Write-Host "Installation directory: $InstallDir" -ForegroundColor Cyan
Write-Host ""

# Check if manager.ps1 exists
if (-Not (Test-Path $ManagerScript)) {
    Write-Host "✖ Error: manager.ps1 not found!" -ForegroundColor Red
    Write-Host "Please ensure manager.ps1 is in the same directory as this script" -ForegroundColor Yellow
    exit 1
}

Write-Host "✓ Found manager.ps1" -ForegroundColor Green
Write-Host ""

# Determine profile path
$ProfileType = if ($AllUsers) { "AllUsersCurrentHost" } else { "CurrentUserCurrentHost" }
$ProfilePath = $PROFILE[$ProfileType]

if (-Not $ProfilePath) {
    $ProfilePath = $PROFILE.CurrentUserCurrentHost
}

$ProfileDir = Split-Path -Parent $ProfilePath

# Create profile directory if it doesn't exist
if (-Not (Test-Path $ProfileDir)) {
    New-Item -Path $ProfileDir -ItemType Directory -Force | Out-Null
    Write-Host "✓ Created PowerShell profile directory" -ForegroundColor Green
}

# Create or update profile
Write-Host "Setting up PowerShell profile..." -ForegroundColor Cyan

# Check if profile exists
if (-Not (Test-Path $ProfilePath)) {
    New-Item -Path $ProfilePath -ItemType File -Force | Out-Null
    Write-Host "✓ Created profile: $ProfilePath" -ForegroundColor Green
}
else {
    Write-Host "✓ Found existing profile: $ProfilePath" -ForegroundColor Green
}

# Check if alias already exists
$AliasLine = "Set-Alias -Name khoipd_terminal_ps -Value '$ManagerScript' -Option AllScope -Scope Global -Force"

if (Select-String -Path $ProfilePath -Pattern "khoipd_terminal_ps" -ErrorAction SilentlyContinue) {
    Write-Host "  Alias 'khoipd_terminal_ps' already exists in profile" -ForegroundColor Yellow
    Write-Host "  Removing old alias..." -ForegroundColor Yellow
    
    # Remove old lines
    $Content = Get-Content $ProfilePath
    $Content = $Content | Where-Object { $_ -notmatch "khoipd_terminal_ps" }
    $Content | Set-Content $ProfilePath
}

# Add new alias
Add-Content -Path $ProfilePath -Value "`n# Math Master Project Manager - Auto-generated on $(Get-Date)"
Add-Content -Path $ProfilePath -Value $AliasLine

Write-Host "✓ Added 'khoipd_terminal_ps' alias to profile" -ForegroundColor Green
Write-Host ""

Write-Host "═════════════════════════════════════════════════════════════" -ForegroundColor Green
Write-Host "Installation Complete! 🎉" -ForegroundColor Green
Write-Host "═════════════════════════════════════════════════════════════" -ForegroundColor Green
Write-Host ""

Write-Host "To use Math Master Manager:" -ForegroundColor Cyan
Write-Host "  1. Reload your PowerShell profile:" -ForegroundColor White
Write-Host "     . `$PROFILE" -ForegroundColor Yellow
Write-Host "  2. Or open a new PowerShell window" -ForegroundColor White
Write-Host "  3. Then run:" -ForegroundColor White
Write-Host "     khoipd_terminal_ps" -ForegroundColor Green
Write-Host ""

Write-Host "Or use immediately:" -ForegroundColor Cyan
Write-Host "  & '$ManagerScript'" -ForegroundColor Green
Write-Host ""

Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Blue
Write-Host "CONFIGURATION" -ForegroundColor Blue
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Blue
Write-Host ""

Write-Host "Most settings are auto-detected. Key options:" -ForegroundColor Cyan
Write-Host "  - PROJECT_ROOT  : Project directory (auto-detected)" -ForegroundColor White
Write-Host "  - JAVA_VERSION  : Java version (21 recommended)" -ForegroundColor White
Write-Host "  - MAVEN_SKIP_TESTS : Skip tests during build" -ForegroundColor White
Write-Host "  - APP_PORT      : Application port (default 8080)" -ForegroundColor White
Write-Host ""

# Check prerequisites
Write-Host "Checking prerequisites:" -ForegroundColor Cyan
Write-Host ""

if (Get-Command java -ErrorAction SilentlyContinue) {
    $JavaVersion = (java -version 2>&1 | Select-String "version" | ForEach-Object { $_.Line } | Select-Object -First 1).Split(' ')[2].Trim('"')
    Write-Host "✓ Java found ($JavaVersion)" -ForegroundColor Green
}
else {
    Write-Host "✗ Java not found - Install Java 21 or higher" -ForegroundColor Yellow
}

if (Get-Command mvn -ErrorAction SilentlyContinue) {
    $MavenVersion = (mvn -v 2>&1 | Select-String "Apache Maven" | ForEach-Object { $_.Line }).Split(' ')[2]
    Write-Host "✓ Maven found ($MavenVersion)" -ForegroundColor Green
}
else {
    Write-Host "✗ Maven not found - Install Maven 3.6+ or higher" -ForegroundColor Yellow
}

if (Get-Command docker -ErrorAction SilentlyContinue) {
    $DockerVersion = (docker --version) -replace "Docker version ", "" -replace ",.*", ""
    Write-Host "✓ Docker found ($DockerVersion)" -ForegroundColor Green
}
else {
    Write-Host "✗ Docker not found - Install Docker Desktop" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Getting Started:" -ForegroundColor Cyan
Write-Host ""
Write-Host "  1. Format code:" -ForegroundColor White
Write-Host "     khoipd_terminal_ps -Task ApplyFormat" -ForegroundColor Green
Write-Host ""
Write-Host "  2. Local deployment:" -ForegroundColor White
Write-Host "     khoipd_terminal_ps -Task DeployLocal" -ForegroundColor Green
Write-Host ""
Write-Host "  3. View help:" -ForegroundColor White
Write-Host "     khoipd_terminal_ps -Help" -ForegroundColor Green
Write-Host ""
Write-Host "Happy coding! 🚀" -ForegroundColor Cyan
Write-Host ""

# ========================================
# Math Master Project Manager - Windows Installation
# Sets up khoipd_terminal_ps PowerShell alias
# ========================================

param(
    [switch]$AllUsers = $false
)

$InstallDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ManagerScript = Join-Path $InstallDir "manager.ps1"

Write-Host ""
Write-Host "Math Master PowerShell Manager Installation" -ForegroundColor Cyan
Write-Host ""

if (-Not (Test-Path $ManagerScript)) {
    Write-Host "Error: manager.ps1 not found!" -ForegroundColor Red
    exit 1
}

Write-Host "Found manager.ps1" -ForegroundColor Green
Write-Host ""

$ProfilePath = $PROFILE.CurrentUserCurrentHost
$ProfileDir = Split-Path -Parent $ProfilePath

if (-Not (Test-Path $ProfileDir)) {
    New-Item -Path $ProfileDir -ItemType Directory -Force | Out-Null
}

Write-Host "Setting up PowerShell profile..." -ForegroundColor Cyan

if (-Not (Test-Path $ProfilePath)) {
    New-Item -Path $ProfilePath -ItemType File -Force | Out-Null
}

$AliasLine = "Set-Alias -Name khoipd_terminal_ps -Value '$ManagerScript' -Option AllScope -Scope Global -Force"

if (Select-String -Path $ProfilePath -Pattern "khoipd_terminal_ps" -ErrorAction SilentlyContinue) {
    $Content = Get-Content $ProfilePath
    $Content = $Content | Where-Object { $_ -notmatch "khoipd_terminal_ps" }
    $Content | Set-Content $ProfilePath
}

Add-Content -Path $ProfilePath -Value ""
Add-Content -Path $ProfilePath -Value "# Math Master Project Manager"
Add-Content -Path $ProfilePath -Value $AliasLine

Write-Host "Added alias to profile" -ForegroundColor Green
Write-Host ""

Write-Host "============================================================" -ForegroundColor Green
Write-Host "Installation Complete!" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host ""

Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "  1. Reload your PowerShell profile: . `$PROFILE" -ForegroundColor White
Write-Host "  2. Or open a new PowerShell window" -ForegroundColor White
Write-Host "  3. Then run: khoipd_terminal_ps" -ForegroundColor Green
Write-Host ""

Write-Host "Checking prerequisites:" -ForegroundColor Cyan
Write-Host ""

$javaFound = $null -ne (Get-Command java -ErrorAction SilentlyContinue)
$mavenFound = $null -ne (Get-Command mvn -ErrorAction SilentlyContinue)
$dockerFound = $null -ne (Get-Command docker -ErrorAction SilentlyContinue)

if ($javaFound) {
    Write-Host "OK: Java found" -ForegroundColor Green
} else {
    Write-Host "WARNING: Java not found" -ForegroundColor Yellow
}

if ($mavenFound) {
    Write-Host "OK: Maven found" -ForegroundColor Green
} else {
    Write-Host "WARNING: Maven not found" -ForegroundColor Yellow
}

if ($dockerFound) {
    Write-Host "OK: Docker found" -ForegroundColor Green
} else {
    Write-Host "WARNING: Docker not found" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Getting Started:" -ForegroundColor Green
Write-Host ""
Write-Host "  khoipd_terminal_ps              # Interactive menu" -ForegroundColor White
Write-Host "  khoipd_terminal_ps -Task ApplyFormat" -ForegroundColor White
Write-Host "  khoipd_terminal_ps -Task DeployLocal" -ForegroundColor White
Write-Host ""
Write-Host "Happy coding!" -ForegroundColor Green
Write-Host ""

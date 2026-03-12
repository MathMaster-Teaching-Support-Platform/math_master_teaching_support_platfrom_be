#!/usr/bin/env pwsh
# ========================================
# Local Deployment Script - Math Master BE (Wrapper)
# This script automatically builds Docker containers for local environment
# ========================================

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ManagerScript = Join-Path $ScriptDir "manager.ps1"

if (Test-Path $ManagerScript) {
    & $ManagerScript -Task DeployLocal
}
else {
    Write-Host "Error: manager.ps1 not found!" -ForegroundColor Red
    exit 1
}

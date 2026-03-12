#!/usr/bin/env pwsh
# ========================================
# Code Formatting Script - Math Master BE (Wrapper)
# This script applies code formatting to all Java files
# according to .editorconfig and Google Java Format style
# ========================================

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ManagerScript = Join-Path $ScriptDir "manager.ps1"

if (Test-Path $ManagerScript) {
    & $ManagerScript -Task ApplyFormat
}
else {
    Write-Host "Error: manager.ps1 not found!" -ForegroundColor Red
    exit 1
}

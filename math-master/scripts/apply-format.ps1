#!/usr/bin/env pwsh
# ========================================
# Code Formatting Script - Math Master BE
# ========================================
# This script applies code formatting to all Java files
# according to .editorconfig and Google Java Format style
# ========================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Math Master - Code Formatting Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if Maven wrapper exists
if (-Not (Test-Path "mvnw.cmd")) {
  Write-Host "[ERROR] mvnw.cmd not found!" -ForegroundColor Red
  Write-Host "Please run this script from the project root directory" -ForegroundColor Yellow
  exit 1
}

Write-Host "[1/2] Checking for code formatting issues..." -ForegroundColor Yellow
Write-Host "Running Spotless check..." -ForegroundColor Cyan
.\mvnw.cmd spotless:check

if ($LASTEXITCODE -eq 0) {
  Write-Host ""
  Write-Host "[OK] All files are already properly formatted!" -ForegroundColor Green
  Write-Host "No changes needed." -ForegroundColor Cyan
  Write-Host ""
  exit 0
}

Write-Host ""
Write-Host "[WARNING] Found formatting issues. Applying fixes..." -ForegroundColor Yellow
Write-Host ""

Write-Host "[2/2] Applying code formatting..." -ForegroundColor Yellow
Write-Host "Formatting all Java files in src/main/java and src/test/java..." -ForegroundColor Cyan
.\mvnw.cmd spotless:apply

if ($LASTEXITCODE -ne 0) {
  Write-Host ""
  Write-Host "[ERROR] Code formatting failed!" -ForegroundColor Red
  Write-Host "Please check the error messages above" -ForegroundColor Yellow
  exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  [SUCCESS] Formatting Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "All Java files have been formatted according to:" -ForegroundColor Cyan
Write-Host "  - Google Java Format style" -ForegroundColor White
Write-Host "  - .editorconfig settings (2 spaces indent)" -ForegroundColor White
Write-Host "  - Removed unused imports" -ForegroundColor White
Write-Host "  - Trimmed trailing whitespace" -ForegroundColor White
Write-Host "  - Added newline at end of files" -ForegroundColor White
Write-Host ""
Write-Host "Check changes with: git status" -ForegroundColor Yellow
Write-Host "Review changes with: git diff" -ForegroundColor Yellow
Write-Host ""

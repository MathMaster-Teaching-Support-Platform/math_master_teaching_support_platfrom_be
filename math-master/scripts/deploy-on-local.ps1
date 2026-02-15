#!/usr/bin/env pwsh
# ========================================
# Local Deployment Script - Math Master BE
# ========================================
# This script automatically formats code according to .editorconfig
# and builds Docker containers for local environment
# ========================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Math Master - Local Deployment Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if Docker is running
Write-Host "[1/4] Checking Docker..." -ForegroundColor Yellow
try {
  docker --version | Out-Null
  if ($LASTEXITCODE -ne 0) {
    throw "Docker not found"
  }
  Write-Host "[OK] Docker is installed" -ForegroundColor Green
  
  docker ps | Out-Null
  if ($LASTEXITCODE -ne 0) {
    throw "Docker daemon not running"
  }
  Write-Host "[OK] Docker daemon is running" -ForegroundColor Green
}
catch {
  Write-Host "[ERROR] Docker is not running or not installed!" -ForegroundColor Red
  Write-Host "Please install Docker Desktop and make sure it's running" -ForegroundColor Red
  exit 1
}

Write-Host ""

# Check .env file
Write-Host "[2/4] Checking environment configuration..." -ForegroundColor Yellow
if (-Not (Test-Path ".env")) {
  Write-Host "[ERROR] .env file not found!" -ForegroundColor Red
  Write-Host "Please create .env file from .env.production.example" -ForegroundColor Yellow
  Write-Host "Run: Copy-Item .env.production.example .env" -ForegroundColor Yellow
  exit 1
}
Write-Host "[OK] .env file found" -ForegroundColor Green
Write-Host ""

# Format code according to .editorconfig
Write-Host "[3/4] Formatting code with Maven Spotless..." -ForegroundColor Yellow
if (Test-Path "mvnw.cmd") {
  Write-Host "Applying code formatting to all Java files..." -ForegroundColor Cyan
  
  # Apply Spotless formatting to all Java source files
  .\mvnw.cmd spotless:apply
  
  if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Code formatting failed" -ForegroundColor Red
    Write-Host "Please check the error messages above" -ForegroundColor Yellow
    exit 1
  }
  
  Write-Host "[OK] Code formatting completed successfully" -ForegroundColor Green
  Write-Host "All Java files have been formatted according to .editorconfig" -ForegroundColor Cyan
}
else {
  Write-Host "[WARNING] mvnw.cmd not found, skipping code formatting" -ForegroundColor Yellow
}
Write-Host ""

# Stop old containers and rebuild
Write-Host "[4/4] Building and starting Docker containers..." -ForegroundColor Yellow
Write-Host "This may take a few minutes on first run..." -ForegroundColor Cyan
Write-Host ""

# Stop and remove old containers (if any)
Write-Host "Stopping existing containers..." -ForegroundColor Cyan
docker compose down

if ($LASTEXITCODE -ne 0) {
  Write-Host "[WARNING] Failed to stop containers (they may not exist)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Building Docker images..." -ForegroundColor Cyan
docker compose build --no-cache

if ($LASTEXITCODE -ne 0) {
  Write-Host "[ERROR] Docker build failed!" -ForegroundColor Red
  exit 1
}

Write-Host ""
Write-Host "Starting containers..." -ForegroundColor Cyan
docker compose up -d

if ($LASTEXITCODE -ne 0) {
  Write-Host "[ERROR] Failed to start containers!" -ForegroundColor Red
  exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  [SUCCESS] Deployment Successful!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Services are starting up..." -ForegroundColor Cyan
Write-Host "  - Redis:       http://localhost:6379" -ForegroundColor White
Write-Host "  - Centrifugo:  http://localhost:8000" -ForegroundColor White
Write-Host "  - Backend API: http://localhost:8080" -ForegroundColor White
Write-Host "  - Swagger UI:  http://localhost:8080/swagger-ui.html" -ForegroundColor White
Write-Host ""
Write-Host "View logs with:    docker compose logs -f" -ForegroundColor Yellow
Write-Host "Stop services:     docker compose down" -ForegroundColor Yellow
Write-Host "Restart services:  docker compose restart" -ForegroundColor Yellow
Write-Host ""
Write-Host "Check container status:" -ForegroundColor Cyan
docker compose ps

# Simple Math Master Manager for Windows PowerShell

param(
    [ValidateSet('ApplyFormat', 'DeployLocal', 'DeleteLogs', 'Help', 'Status')]
    [string]$Task,
    [switch]$Help
)

function Show-Help {
    Write-Host ""
    Write-Host "Math Master PowerShell Manager - Help" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "USAGE:" -ForegroundColor Cyan
    Write-Host "  khoipd_terminal_ps -Task TaskName" -ForegroundColor White
    Write-Host ""
    Write-Host "TASKS:" -ForegroundColor Cyan
    Write-Host "  ApplyFormat     - Apply code formatting (Spotless)" -ForegroundColor White
    Write-Host "  DeployLocal     - Build and deploy with Docker" -ForegroundColor White
    Write-Host "  DeleteLogs      - Delete log files" -ForegroundColor White
    Write-Host "  Status          - Show project status" -ForegroundColor White
    Write-Host "  Help            - Show this help" -ForegroundColor White
    Write-Host ""
    Write-Host "EXAMPLES:" -ForegroundColor Cyan
    Write-Host "  khoipd_terminal_ps -Task ApplyFormat" -ForegroundColor Green
    Write-Host "  khoipd_terminal_ps -Task DeployLocal" -ForegroundColor Green
    Write-Host ""
}

function Invoke-ApplyFormat {
    Write-Host ""
    Write-Host "Code Formatting - Math Master" -ForegroundColor Cyan
    Write-Host ""
    
    $projectRoot = (Get-Location).Path
    if ($projectRoot -like "*\ps_scripts" -or $projectRoot -like "*\ubuntu_scripts") {
        $projectRoot = Split-Path (Split-Path $projectRoot)
    }
    
    if (-Not (Test-Path (Join-Path $projectRoot "mvnw.cmd"))) {
        Write-Host "ERROR: mvnw.cmd not found!" -ForegroundColor Red
        Write-Host "Project root: $projectRoot" -ForegroundColor Yellow
        return
    }
    
    Push-Location $projectRoot
    
    Write-Host "Checking formatting issues..." -ForegroundColor Yellow
    &.\mvnw.cmd -DskipTests spotless:check 2>&1 | Where-Object { $_ -notmatch "Downloading from central" -and $_ -notmatch "Downloaded from central" }
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "OK: All files are properly formatted!" -ForegroundColor Green
        Write-Host ""
        Pop-Location
        return
    }
    
    Write-Host ""
    Write-Host "Applying formatting fixes..." -ForegroundColor Yellow
    Write-Host ""
    
    &.\mvnw.cmd -DskipTests spotless:apply 2>&1 | Where-Object { $_ -notmatch "Downloading from central" -and $_ -notmatch "Downloaded from central" }
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "ERROR: Formatting failed!" -ForegroundColor Red
        Pop-Location
        return
    }
    
    Write-Host ""
    Write-Host "Formatting Complete!" -ForegroundColor Green
    Write-Host ""
    Pop-Location
    return
}

function Invoke-DeployLocal {
    Write-Host ""
    Write-Host "Local Deployment - Math Master" -ForegroundColor Cyan
    Write-Host ""
    
    $projectRoot = (Get-Location).Path
    if ($projectRoot -like "*\ps_scripts" -or $projectRoot -like "*\ubuntu_scripts") {
        $projectRoot = Split-Path (Split-Path $projectRoot)
    }
    Push-Location $projectRoot
    
    Write-Host "Checking Docker..." -ForegroundColor Yellow
    try {
        docker --version 2>&1 | Out-Null
        docker ps 2>&1 | Out-Null
        Write-Host "OK: Docker is running" -ForegroundColor Green
    }
    catch {
        Write-Host "ERROR: Docker not running!" -ForegroundColor Red
        Pop-Location
        return
    }
    
    Write-Host ""
    if (-Not (Test-Path ".env")) {
        Write-Host "ERROR: .env file not found!" -ForegroundColor Red
        Pop-Location
        return
    }
    Write-Host "OK: .env file found" -ForegroundColor Green
    
    Write-Host ""
    Write-Host "Building and starting services..." -ForegroundColor Yellow
    Write-Host ""
    
    docker compose down 2>&1 | Out-Null
    docker compose build --no-cache
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "ERROR: Docker build failed!" -ForegroundColor Red
        Pop-Location
        return
    }
    
    docker compose up -d
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "ERROR: Failed to start services!" -ForegroundColor Red
        Pop-Location
        return
    }
    
    Write-Host ""
    Write-Host "Deployment Successful!" -ForegroundColor Green
    Write-Host ""
    docker compose ps
    Write-Host ""
    Pop-Location
    return
}

function Invoke-DeleteLogs {
    Write-Host ""
    Write-Host "Delete Log Files - Math Master" -ForegroundColor Cyan
    Write-Host ""
    
    $projectRoot = (Get-Location).Path
    if ($projectRoot -like "*\ps_scripts" -or $projectRoot -like "*\ubuntu_scripts") {
        $projectRoot = Split-Path (Split-Path $projectRoot)
    }
    
    $logDir = Join-Path $projectRoot "logs"
    
    if (Test-Path $logDir) {
        $logFiles = Get-ChildItem -Path $logDir -Filter "*.log", "*.txt" -ErrorAction SilentlyContinue
        
        if ($logFiles.Count -gt 0) {
            Write-Host "Found $($logFiles.Count) log files" -ForegroundColor Yellow
            Remove-Item -Path $logDir -Filter "*.log", "*.txt" -Force -ErrorAction SilentlyContinue
            Write-Host "Log files deleted successfully!" -ForegroundColor Green
        } else {
            Write-Host "No log files found" -ForegroundColor Yellow
        }
    } else {
        Write-Host "Logs directory not found at: $logDir" -ForegroundColor Yellow
    }
    
    Write-Host ""
    return
}

function Invoke-Status {
    Write-Host ""
    Write-Host "Project Status" -ForegroundColor Cyan
    Write-Host ""
    
    $projectRoot = (Get-Location).Path
    if ($projectRoot -like "*\ps_scripts" -or $projectRoot -like "*\ubuntu_scripts") {
        $projectRoot = Split-Path (Split-Path $projectRoot)
    }
    Push-Location $projectRoot
    
    docker compose ps 2>&1 | Where-Object { $_ -notmatch "Downloading from central" -and $_ -notmatch "Downloaded from central" }
    
    Pop-Location
    Write-Host ""
    return
}

function Show-MainMenu {
    Write-Host ""
    Write-Host "Math Master Manager" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "1 - Format Code" -ForegroundColor White
    Write-Host "2 - Local Deployment" -ForegroundColor White
    Write-Host "3 - Delete Logs" -ForegroundColor White
    Write-Host "4 - Project Status" -ForegroundColor White
    Write-Host "5 - Help" -ForegroundColor White
    Write-Host "6 - Clear Screen" -ForegroundColor White
    Write-Host "7 - Exit" -ForegroundColor White
    Write-Host ""
}

if ($Help -or $Task -eq 'Help') {
    Show-Help
    exit 0
}

if ($Task) {
    switch ($Task) {
        'ApplyFormat' {
            Invoke-ApplyFormat | Out-Null
            exit 0
        }
        'DeployLocal' {
            Invoke-DeployLocal | Out-Null
            exit 0
        }
        'DeleteLogs' {
            Invoke-DeleteLogs | Out-Null
            exit 0
        }
        'Status' {
            Invoke-Status | Out-Null
            exit 0
        }
    }
}

while ($true) {
    Show-MainMenu
    $choice = Read-Host "Enter number"
    
    switch ($choice) {
        '1' { 
            Invoke-ApplyFormat
        }
        '2' { 
            Invoke-DeployLocal
        }
        '3' { 
            Invoke-DeleteLogs
        }
        '4' { 
            Invoke-Status
        }
        '5' { 
            Show-Help
        }
        '6' {
            Clear-Host
        }
        '7' {
            Write-Host ""
            Write-Host "Goodbye!" -ForegroundColor Cyan
            exit 0
        }
    }
}

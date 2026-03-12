# ========================================
# Math Master Project Manager - PowerShell Version
# Main menu-driven manager script for Windows developers
# ========================================

param(
    [ValidateSet('ApplyFormat', 'DeployLocal', 'Help', 'Status')]
    [string]$Task,
    [switch]$Help
)

# Colors for Windows PowerShell
$Colors = @{
    Red     = [System.ConsoleColor]::Red
    Green   = [System.ConsoleColor]::Green
    Yellow  = [System.ConsoleColor]::Yellow
    Cyan    = [System.ConsoleColor]::Cyan
    Blue    = [System.ConsoleColor]::Blue
    White   = [System.ConsoleColor]::White
}

function Write-Header {
    param([string]$Text)
    Write-Host ""
    Write-Host "╔════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
    Write-Host "║  $Text" -ForegroundColor Cyan
    Write-Host "╚════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
    Write-Host ""
}

function Write-Step {
    param([string]$Text, [int]$Step, [int]$Total)
    Write-Host "[$Step/$Total] $Text" -ForegroundColor Yellow
}

function Write-Success {
    param([string]$Text)
    Write-Host "✓ $Text" -ForegroundColor Green
}

function Write-Error-Custom {
    param([string]$Text)
    Write-Host "✗ $Text" -ForegroundColor Red
}

function Write-Info {
    param([string]$Text)
    Write-Host "ℹ $Text" -ForegroundColor Cyan
}

function Show-Help {
    Write-Header "Math Master PowerShell Manager - Help"
    
    Write-Host "USAGE:" -ForegroundColor Cyan
    Write-Host "  khoipd_terminal_ps [-Task <TaskName>] [-Help]" -ForegroundColor White
    Write-Host ""
    
    Write-Host "TASKS:" -ForegroundColor Cyan
    Write-Host "  ApplyFormat     Apply code formatting using Spotless" -ForegroundColor White
    Write-Host "  DeployLocal     Build and deploy using Docker Compose" -ForegroundColor White
    Write-Host "  Status          Show project status and services" -ForegroundColor White
    Write-Host "  Help            Show this help message" -ForegroundColor White
    Write-Host ""
    
    Write-Host "EXAMPLES:" -ForegroundColor Cyan
    Write-Host "  khoipd_terminal_ps -Task ApplyFormat" -ForegroundColor Green
    Write-Host "  khoipd_terminal_ps -Task DeployLocal" -ForegroundColor Green
    Write-Host "  khoipd_terminal_ps -Help" -ForegroundColor Green
    Write-Host ""
}

function Invoke-ApplyFormat {
    Write-Header "Code Formatting - Math Master BE"
    
    # Check if Maven wrapper exists
    if (-Not (Test-Path "mvnw.cmd")) {
        Write-Error-Custom "mvnw.cmd not found!"
        Write-Info "Please run this script from the project root directory"
        return $false
    }
    
    Write-Step "Checking for code formatting issues..." 1 2
    Write-Info "Running Spotless check..."
    & .\mvnw.cmd spotless:check
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Success "All files are already properly formatted!"
        Write-Info "No changes needed."
        Write-Host ""
        return $true
    }
    
    Write-Host ""
    Write-Info "Found formatting issues. Applying fixes..."
    Write-Host ""
    
    Write-Step "Applying code formatting..." 2 2
    Write-Info "Formatting all Java files in src/main/java and src/test/java..."
    & .\mvnw.cmd spotless:apply
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Error-Custom "Code formatting failed!"
        Write-Info "Please check the error messages above"
        return $false
    }
    
    Write-Host ""
    Write-Host "═════════════════════════════════════════════════════════" -ForegroundColor Green
    Write-Host "✓ Formatting Complete!" -ForegroundColor Green
    Write-Host "═════════════════════════════════════════════════════════" -ForegroundColor Green
    Write-Host ""
    
    Write-Host "All Java files have been formatted according to:" -ForegroundColor Cyan
    Write-Host "  • Google Java Format style" -ForegroundColor White
    Write-Host "  • .editorconfig settings (2 spaces indent)" -ForegroundColor White
    Write-Host "  • Removed unused imports" -ForegroundColor White
    Write-Host "  • Trimmed trailing whitespace" -ForegroundColor White
    Write-Host "  • Added newline at end of files" -ForegroundColor White
    Write-Host ""
    
    Write-Host "Next steps:" -ForegroundColor Yellow
    Write-Host "  git status          # Check changes" -ForegroundColor Green
    Write-Host "  git diff            # Review changes" -ForegroundColor Green
    Write-Host "  git add .           # Stage changes" -ForegroundColor Green
    Write-Host "  git commit -m '...' # Commit changes" -ForegroundColor Green
    Write-Host ""
    
    return $true
}

function Invoke-DeployLocal {
    Write-Header "Local Deployment - Math Master BE"
    
    Write-Step "Checking Docker..." 1 3
    try {
        docker --version | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw "Docker not found"
        }
        Write-Success "Docker is installed"
        
        docker ps | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw "Docker daemon not running"
        }
        Write-Success "Docker daemon is running"
    }
    catch {
        Write-Host ""
        Write-Error-Custom "Docker is not running or not installed!"
        Write-Info "Please install Docker Desktop and make sure it's running"
        return $false
    }
    
    Write-Host ""
    
    # Check .env file
    Write-Step "Checking environment configuration..." 2 3
    if (-Not (Test-Path ".env")) {
        Write-Error-Custom ".env file not found!"
        Write-Info "Create .env file from .env.production.example"
        Write-Host "  Run: Copy-Item .env.production.example .env" -ForegroundColor Yellow
        return $false
    }
    Write-Success ".env file found"
    Write-Host ""
    
    # Stop old containers and rebuild
    Write-Step "Building and starting Docker containers..." 3 3
    Write-Info "This may take a few minutes on first run..."
    Write-Host ""
    
    # Stop and remove old containers (if any)
    Write-Info "Stopping existing containers..."
    docker compose down 2>&1 | Out-Null
    
    if ($LASTEXITCODE -ne 0) {
        Write-Info "Previous containers stopped (or didn't exist)"
    }
    else {
        Write-Success "Existing containers stopped"
    }
    
    Write-Host ""
    Write-Info "Building Docker images..."
    docker compose build --no-cache
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Error-Custom "Docker build failed!"
        return $false
    }
    
    Write-Success "Docker images built successfully"
    Write-Host ""
    
    Write-Info "Starting containers..."
    docker compose up -d
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Error-Custom "Failed to start containers!"
        return $false
    }
    
    Write-Success "Containers started successfully"
    Write-Host ""
    
    Write-Host "═════════════════════════════════════════════════════════" -ForegroundColor Green
    Write-Host "✓ Deployment Successful!" -ForegroundColor Green
    Write-Host "═════════════════════════════════════════════════════════" -ForegroundColor Green
    Write-Host ""
    
    Write-Host "Services are starting up..." -ForegroundColor Cyan
    Write-Host "  • Redis:       http://localhost:6379" -ForegroundColor White
    Write-Host "  • Centrifugo:  http://localhost:8000" -ForegroundColor White
    Write-Host "  • Backend API: http://localhost:8080" -ForegroundColor White
    Write-Host "  • Swagger UI:  http://localhost:8080/swagger-ui.html" -ForegroundColor White
    Write-Host ""
    
    Write-Host "Useful commands:" -ForegroundColor Yellow
    Write-Host "  docker compose logs -f           # View live logs" -ForegroundColor Green
    Write-Host "  docker compose ps                # Check status" -ForegroundColor Green
    Write-Host "  docker compose down              # Stop services" -ForegroundColor Green
    Write-Host "  docker compose restart           # Restart services" -ForegroundColor Green
    Write-Host ""
    
    Write-Host "Container status:" -ForegroundColor Cyan
    docker compose ps
    Write-Host ""
    
    return $true
}

function Invoke-Status {
    Write-Header "Project Status"
    
    Write-Host "Environment Information:" -ForegroundColor Cyan
    Write-Host ""
    
    # Java version
    if (Get-Command java -ErrorAction SilentlyContinue) {
        $JavaVersion = (java -version 2>&1 | Select-String "version" | ForEach-Object { $_.Line }).Split(' ')[2].Trim('"')
        Write-Success "Java: $JavaVersion"
    }
    else {
        Write-Error-Custom "Java: Not found"
    }
    
    # Maven version
    if (Get-Command mvn -ErrorAction SilentlyContinue) {
        $MavenVersion = (mvn -v 2>&1 | Select-String "Apache Maven" | ForEach-Object { $_.Line }).Split(' ')[2]
        Write-Success "Maven: $MavenVersion"
    }
    else {
        Write-Error-Custom "Maven: Not found"
    }
    
    # Docker version
    if (Get-Command docker -ErrorAction SilentlyContinue) {
        $DockerVersion = (docker --version) -replace "Docker version ", "" -replace ",.*", ""
        Write-Success "Docker: $DockerVersion"
    }
    else {
        Write-Error-Custom "Docker: Not found"
    }
    
    # Docker Compose version
    if (Get-Command docker-compose -ErrorAction SilentlyContinue) {
        $ComposeVersion = (docker-compose --version) -replace "docker-compose version ", "" -replace ",.*", ""
        Write-Success "Docker Compose: $ComposeVersion"
    }
    else {
        Write-Error-Custom "Docker Compose: Not found"
    }
    
    Write-Host ""
    Write-Host "Active Containers:" -ForegroundColor Cyan
    docker compose ps 2>&1
    
    Write-Host ""
}

function Show-MainMenu {
    Write-Header "Math Master Project Manager"
    
    Write-Host "Available Tasks:" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  1. Format Code          (Apply code formatting)" -ForegroundColor White
    Write-Host "  2. Local Deployment     (Build and deploy with Docker)" -ForegroundColor White
    Write-Host "  3. Project Status       (Show environment info)" -ForegroundColor White
    Write-Host "  4. Help                 (Show help information)" -ForegroundColor White
    Write-Host "  5. Exit                 (Close manager)" -ForegroundColor White
    Write-Host ""
}

# Main execution
if ($Help -or $Task -eq 'Help') {
    Show-Help
    exit 0
}

if ($Task) {
    switch ($Task) {
        'ApplyFormat' {
            $Success = Invoke-ApplyFormat
            exit $(if ($Success) { 0 } else { 1 })
        }
        'DeployLocal' {
            $Success = Invoke-DeployLocal
            exit $(if ($Success) { 0 } else { 1 })
        }
        'Status' {
            Invoke-Status
            exit 0
        }
        default {
            Show-Help
            exit 1
        }
    }
}

# Interactive mode
while ($true) {
    Show-MainMenu
    
    $choice = Read-Host "Select option"
    
    switch ($choice) {
        '1' {
            $Success = Invoke-ApplyFormat
            if (-Not $Success) {
                Read-Host "Press Enter to continue"
            }
        }
        '2' {
            $Success = Invoke-DeployLocal
            if (-Not $Success) {
                Read-Host "Press Enter to continue"
            }
        }
        '3' {
            Invoke-Status
            Read-Host "Press Enter to continue"
        }
        '4' {
            Show-Help
            Read-Host "Press Enter to continue"
        }
        '5' {
            Write-Host ""
            Write-Host "Goodbye! 👋" -ForegroundColor Cyan
            exit 0
        }
        default {
            Write-Error-Custom "Invalid option. Please try again."
            Read-Host "Press Enter to continue"
        }
    }
    
    Clear-Host
}

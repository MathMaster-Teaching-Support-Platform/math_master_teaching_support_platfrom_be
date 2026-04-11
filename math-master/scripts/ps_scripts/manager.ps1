# Math Master Manager - Windows PowerShell

param(
    [ValidateSet(
        'ApplyFormat', 'SortAnnotations', 'CleanBuild', 'CleanBuildStart',
        'StartRedis', 'StopRedis', 'RestartRedis',
        'StartAll', 'StopAll', 'RestartAll',
        'DeployLocal', 'DeployFull', 'RecreateDocker',
        'Logs', 'LogsRedis', 'LogsApp',
        'Status', 'DeleteLogs', 'Help'
    )]
    [string]$Task,
    [switch]$Help,
    [string]$Service = ""
)

# â”€â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

function Get-ProjectRoot {
    $root = (Get-Location).Path
    if ($root -like "*\ps_scripts" -or $root -like "*\scripts*") {
        $root = Split-Path (Split-Path $root)
    }
    return $root
}

function Assert-Docker {
    try {
        docker info 2>&1 | Out-Null
        if ($LASTEXITCODE -ne 0) { throw }
    } catch {
        Write-Host "ERROR: Docker is not running. Start Docker Desktop first." -ForegroundColor Red
        return $false
    }
    return $true
}

function Assert-EnvFile($root) {
    if (-Not (Test-Path (Join-Path $root ".env"))) {
        Write-Host "WARN: .env file not found at project root - using docker-compose defaults." -ForegroundColor Yellow
    }
}

function Write-Header($title) {
    Write-Host ""
    Write-Host ("=" * 50) -ForegroundColor DarkCyan
    Write-Host "  $title" -ForegroundColor Cyan
    Write-Host ("=" * 50) -ForegroundColor DarkCyan
    Write-Host ""
}

function Write-Step($msg) { Write-Host ">> $msg" -ForegroundColor Yellow }
function Write-OK($msg)   { Write-Host "OK  $msg" -ForegroundColor Green  }
function Write-Err($msg)  { Write-Host "ERR $msg" -ForegroundColor Red    }

function Stop-AppPort {
    param([int]$Port = 8080)
    $procs = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue |
             Where-Object { $_.State -eq 'Listen' -or $_.State -eq 'Established' }
    if (-not $procs) { Write-OK "Port $Port is already free."; return }
    $pids = $procs | Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($pid in $pids) {
        $proc = Get-Process -Id $pid -ErrorAction SilentlyContinue
        $name = if ($proc) { $proc.Name } else { "PID $pid" }
        Write-Step "Killing process on port ${Port}: $name (PID $pid)"
        Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
    }
    for ($i = 0; $i -lt 10; $i++) {
        Start-Sleep -Seconds 1
        $still = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue |
                 Where-Object { $_.State -eq 'Listen' -or $_.State -eq 'Established' }
        if (-not $still) { Write-OK "Port $Port is now free."; return }
    }
    Write-Err "Port $Port still in use after 10s. Continuing anyway..."
}

# Tasks

function Invoke-ApplyFormat {
    Write-Header "Code Formatting (Spotless)"
    $root = Get-ProjectRoot
    if (-Not (Test-Path (Join-Path $root "mvnw.cmd"))) { Write-Err "mvnw.cmd not found at: $root"; return }
    Push-Location $root
    Write-Step "Checking formatting..."
    &.\mvnw.cmd -DskipTests spotless:check 2>&1 | Where-Object { $_ -notmatch "Downloading|Downloaded" }
    if ($LASTEXITCODE -eq 0) { Write-OK "All files already formatted."; Pop-Location; return }
    Write-Step "Applying fixes..."
    &.\mvnw.cmd -DskipTests spotless:apply 2>&1 | Where-Object { $_ -notmatch "Downloading|Downloaded" }
    if ($LASTEXITCODE -eq 0) { Write-OK "Formatting applied successfully." } else { Write-Err "Formatting failed." }
    Pop-Location
}

function Invoke-SortAnnotations {
    Write-Header "Sort Entity Annotations"
    $root   = Get-ProjectRoot
    $script = Join-Path $PSScriptRoot "sort-annotations.ps1"
    if (-Not (Test-Path $script)) { Write-Err "sort-annotations.ps1 not found."; return }
    $entityDir = Join-Path $root "src\main\java\com\fptu\math_master\entity"
    & $script -EntityDir $entityDir
    Write-OK "Done."
}

function Invoke-CleanBuild {
    Write-Header "Clean Maven Build"
    $root = Get-ProjectRoot
    if (-Not (Test-Path (Join-Path $root "mvnw.cmd"))) { Write-Err "mvnw.cmd not found."; return }
    Push-Location $root
    Write-Step "Running: mvnw clean compile..."
    &.\mvnw.cmd clean compile -q 2>&1 | Where-Object { $_ -notmatch "Downloading|Downloaded" }
    if ($LASTEXITCODE -eq 0) { Write-OK "Build successful." } else { Write-Err "Build failed." }
    Pop-Location
}

function Invoke-CleanBuildStart {
    Write-Header "Clean Build + Start Project"
    $root = Get-ProjectRoot
    if (-Not (Test-Path (Join-Path $root "mvnw.cmd"))) { Write-Err "mvnw.cmd not found."; return }

    # Step 1: Remove the app Docker container so it cannot occupy port 8080
    if (Assert-Docker) {
        Assert-EnvFile $root
        Push-Location $root
        Write-Step "Removing app container to free port 8080..."
        # rm -f -s: force-stop then remove the container entirely (prevents auto-restart)
        docker compose rm -f -s app 2>&1 | Out-Null
        Write-Step "Starting infrastructure services (redis, centrifugo, minio)..."
        docker compose up -d redis centrifugo minio nginx-minio minio-init
        if ($LASTEXITCODE -ne 0) { Write-Err "Failed to start infrastructure services."; Pop-Location; return }
        Write-OK "Infrastructure services started."
        Pop-Location
    }

    # Step 2: Clean build
    Push-Location $root
    Write-Step "Running Maven clean compile..."
    &.\mvnw.cmd clean compile -q 2>&1 | Where-Object { $_ -notmatch "Downloading|Downloaded" }
    if ($LASTEXITCODE -ne 0) { Write-Err "Build failed. Fix errors before starting."; Pop-Location; return }
    Write-OK "Build successful."

    # Step 3: Kill anything still on port 8080 right before starting
    Stop-AppPort -Port 8080

    # Step 4: Start Spring Boot
    Write-Step "Starting Spring Boot application... (Ctrl+C to stop)"
    Write-Host ""
    &.\mvnw.cmd spring-boot:run
    Pop-Location
}

function Invoke-StartRedis {
    Write-Header "Start Redis"
    if (-not (Assert-Docker)) { return }
    $root = Get-ProjectRoot; Assert-EnvFile $root; Push-Location $root
    Write-Step "Starting Redis container..."
    docker compose up -d redis
    if ($LASTEXITCODE -eq 0) { Write-OK "Redis started." } else { Write-Err "Failed to start Redis." }
    Pop-Location
}

function Invoke-StopRedis {
    Write-Header "Stop Redis"
    if (-not (Assert-Docker)) { return }
    $root = Get-ProjectRoot; Push-Location $root
    Write-Step "Stopping Redis container..."
    docker compose stop redis
    if ($LASTEXITCODE -eq 0) { Write-OK "Redis stopped." } else { Write-Err "Failed to stop Redis." }
    Pop-Location
}

function Invoke-RestartRedis {
    Write-Header "Restart Redis"
    if (-not (Assert-Docker)) { return }
    $root = Get-ProjectRoot; Push-Location $root
    Write-Step "Restarting Redis container..."
    docker compose restart redis
    if ($LASTEXITCODE -eq 0) { Write-OK "Redis restarted." } else { Write-Err "Failed to restart Redis." }
    Pop-Location
}

function Invoke-StartAll {
    Write-Header "Start All Services"
    if (-not (Assert-Docker)) { return }
    $root = Get-ProjectRoot; Assert-EnvFile $root; Push-Location $root
    Write-Step "Starting all containers..."
    docker compose up -d --no-recreate
    if ($LASTEXITCODE -eq 0) { Write-OK "All services started."; Write-Host ""; docker compose ps }
    else { Write-Err "Some services failed to start." }
    Pop-Location
}

function Invoke-StopAll {
    Write-Header "Stop All Services"
    if (-not (Assert-Docker)) { return }
    $root = Get-ProjectRoot; Push-Location $root
    Write-Step "Stopping all containers..."
    docker compose down
    if ($LASTEXITCODE -eq 0) { Write-OK "All services stopped." } else { Write-Err "Failed to stop services." }
    Pop-Location
}

function Invoke-RestartAll {
    Write-Header "Restart All Services"
    if (-not (Assert-Docker)) { return }
    $root = Get-ProjectRoot; Push-Location $root
    Write-Step "Restarting all containers..."
    docker compose restart
    if ($LASTEXITCODE -eq 0) { Write-OK "All services restarted."; Write-Host ""; docker compose ps }
    else { Write-Err "Restart failed." }
    Pop-Location
}

function Invoke-DeployLocal {
    Write-Header "Local Deploy (build + up)"
    if (-not (Assert-Docker)) { return }
    $root = Get-ProjectRoot; Assert-EnvFile $root; Push-Location $root
    Write-Step "Building Docker images (no-cache)..."
    docker compose build --no-cache
    if ($LASTEXITCODE -ne 0) { Write-Err "Docker build failed."; Pop-Location; return }
    Write-Step "Starting services..."
    docker compose up -d
    if ($LASTEXITCODE -eq 0) { Write-OK "Deployment complete."; Write-Host ""; docker compose ps }
    else { Write-Err "Failed to start services." }
    Pop-Location
}

function Invoke-DeployFull {
    Write-Header "Full Deploy (down + clean build + up)"
    if (-not (Assert-Docker)) { return }
    $root = Get-ProjectRoot; Assert-EnvFile $root; Push-Location $root

    Write-Step "Stopping existing containers..."
    docker compose down --remove-orphans

    Write-Step "Pruning unused images..."
    docker image prune -f | Out-Null

    Write-Step "Building Docker images (no-cache)..."
    docker compose build --no-cache
    if ($LASTEXITCODE -ne 0) { Write-Err "Docker build failed."; Pop-Location; return }

    Write-Step "Starting all services..."
    docker compose up -d
    if ($LASTEXITCODE -eq 0) {
        Write-OK "Full deployment complete."
        Write-Host ""
        docker compose ps
    } else { Write-Err "Failed to start services." }
    Pop-Location
}

function Invoke-RecreateDocker {
    Write-Header "Recreate Docker Containers (force-recreate)"
    if (-not (Assert-Docker)) { return }
    $root = Get-ProjectRoot; Assert-EnvFile $root; Push-Location $root

    $target = if ($Service) { $Service } else { "" }

    if ($target) {
        Write-Step "Force-recreating container: $target"
        docker compose up -d --force-recreate $target
    } else {
        Write-Step "Force-recreating all containers (no rebuild)..."
        docker compose up -d --force-recreate
    }

    if ($LASTEXITCODE -eq 0) {
        Write-OK "Containers recreated."
        Write-Host ""
        docker compose ps
    } else { Write-Err "Recreate failed." }
    Pop-Location
}

function Invoke-Logs($service, $title) {
    Write-Header $title
    if (-not (Assert-Docker)) { return }
    $root = Get-ProjectRoot; Push-Location $root
    if ($service) {
        Write-Step "Tailing logs for: $service (Ctrl+C to stop)"
        docker compose logs -f --tail=100 $service
    } else {
        Write-Step "Tailing all service logs (Ctrl+C to stop)"
        docker compose logs -f --tail=50
    }
    Pop-Location
}

function Invoke-Status {
    Write-Header "Project Status"
    if (-not (Assert-Docker)) { return }
    $root = Get-ProjectRoot; Push-Location $root
    Write-Host "Docker containers:" -ForegroundColor Cyan
    docker compose ps
    Write-Host ""
    Write-Host "Docker image sizes:" -ForegroundColor Cyan
    docker compose images
    Pop-Location
    Write-Host ""
}

function Invoke-DeleteLogs {
    Write-Header "Delete Log Files"
    $root = Get-ProjectRoot
    $logDir = Join-Path $root "scripts\logs"
    $altLogDir = Join-Path $root "logs"
    $deleted = 0

    foreach ($dir in @($logDir, $altLogDir)) {
        if (Test-Path $dir) {
            $files = Get-ChildItem -Path $dir -Include "*.log","*.txt" -Recurse -ErrorAction SilentlyContinue
            foreach ($f in $files) { Remove-Item $f.FullName -Force; $deleted++ }
        }
    }

    if ($deleted -gt 0) { Write-OK "Deleted $deleted log file(s)." }
    else { Write-Host "No log files found." -ForegroundColor Yellow }
    Write-Host ""
}

# â”€â”€â”€ Help â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

function Show-Help {
    Write-Host ""
    Write-Host "Math Master PowerShell Manager" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "USAGE:" -ForegroundColor Cyan
    Write-Host "  .\manager.ps1 -Task <TaskName> [-Service <name>]" -ForegroundColor White
    Write-Host ""
    Write-Host "TASKS:" -ForegroundColor Cyan
    $tasks = @(
        @{ Name = "ApplyFormat";      Desc = "Run Spotless code formatter" },
        @{ Name = "SortAnnotations";  Desc = "Standardize Lombok annotation order in entity files" },
        @{ Name = "CleanBuild";       Desc = "Maven clean compile (local)" },
        @{ Name = "CleanBuildStart";  Desc = "Start Docker services + clean build + spring-boot:run" },
        @{ Name = "StartRedis";     Desc = "Start only the Redis container" },
        @{ Name = "StopRedis";      Desc = "Stop the Redis container" },
        @{ Name = "RestartRedis";   Desc = "Restart the Redis container" },
        @{ Name = "StartAll";       Desc = "Start all docker-compose services" },
        @{ Name = "StopAll";        Desc = "Stop all docker-compose services (docker compose down)" },
        @{ Name = "RestartAll";     Desc = "Restart all running containers" },
        @{ Name = "DeployLocal";    Desc = "Build images (no-cache) then docker compose up" },
        @{ Name = "DeployFull";     Desc = "Full: down + prune + build no-cache + up" },
        @{ Name = "RecreateDocker"; Desc = "Force-recreate containers without rebuild (-Service redis)" },
        @{ Name = "Logs";           Desc = "Tail logs for all services" },
        @{ Name = "LogsRedis";      Desc = "Tail Redis container logs" },
        @{ Name = "LogsApp";        Desc = "Tail app container logs" },
        @{ Name = "Status";         Desc = "Show container status and image sizes" },
        @{ Name = "DeleteLogs";     Desc = "Delete *.log / *.txt from logs folders" },
        @{ Name = "Help";           Desc = "Show this help" }
    )
    foreach ($t in $tasks) {
        Write-Host ("  {0,-18} {1}" -f $t.Name, $t.Desc) -ForegroundColor White
    }
    Write-Host ""
    Write-Host "EXAMPLES:" -ForegroundColor Cyan
    Write-Host "  .\manager.ps1 -Task StartRedis" -ForegroundColor Green
    Write-Host "  .\manager.ps1 -Task RecreateDocker -Service redis" -ForegroundColor Green
    Write-Host "  .\manager.ps1 -Task DeployFull" -ForegroundColor Green
    Write-Host "  .\manager.ps1 -Task LogsApp" -ForegroundColor Green
    Write-Host ""
}

# â”€â”€â”€ Menu â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

function Show-MainMenu {
    Write-Host ""
    Write-Host "  Math Master Manager" -ForegroundColor Cyan
    Write-Host ("  " + "-" * 35) -ForegroundColor DarkCyan
    Write-Host "  [1]  Format Code (Spotless)"           -ForegroundColor White
    Write-Host "  [2]  Sort Annotations"                  -ForegroundColor White
    Write-Host "  [3]  Maven Clean Build"                 -ForegroundColor White
    Write-Host "  [4]  Clean Build + Start Project"       -ForegroundColor Green
    Write-Host ("  " + "-" * 35) -ForegroundColor DarkGray
    Write-Host "  [5]  Start Redis"                       -ForegroundColor White
    Write-Host "  [6]  Stop Redis"                        -ForegroundColor White
    Write-Host "  [7]  Restart Redis"                     -ForegroundColor White
    Write-Host ("  " + "-" * 35) -ForegroundColor DarkGray
    Write-Host "  [8]  Start All Services"                -ForegroundColor White
    Write-Host "  [9]  Stop All Services"                 -ForegroundColor White
    Write-Host "  [10] Restart All Services"              -ForegroundColor White
    Write-Host ("  " + "-" * 35) -ForegroundColor DarkGray
    Write-Host "  [11] Deploy Local (build + up)"         -ForegroundColor White
    Write-Host "  [12] Deploy Full  (down+build+up)"      -ForegroundColor White
    Write-Host "  [13] Recreate Docker Containers"        -ForegroundColor White
    Write-Host ("  " + "-" * 35) -ForegroundColor DarkGray
    Write-Host "  [14] Tail All Logs"                     -ForegroundColor White
    Write-Host "  [15] Tail Redis Logs"                   -ForegroundColor White
    Write-Host "  [16] Tail App Logs"                     -ForegroundColor White
    Write-Host ("  " + "-" * 35) -ForegroundColor DarkGray
    Write-Host "  [17] Project Status"                    -ForegroundColor White
    Write-Host "  [18] Delete Log Files"                  -ForegroundColor White
    Write-Host "  [19] Help"                              -ForegroundColor White
    Write-Host "  [20] Clear Screen"                      -ForegroundColor White
    Write-Host "  [0]  Exit"                              -ForegroundColor DarkGray
    Write-Host ""
}

# â”€â”€â”€ Entry point â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

if ($Help -or $Task -eq 'Help') { Show-Help; exit 0 }

if ($Task) {
    switch ($Task) {
        'ApplyFormat'      { Invoke-ApplyFormat }
        'SortAnnotations'  { Invoke-SortAnnotations }
        'CleanBuild'       { Invoke-CleanBuild }
        'CleanBuildStart'  { Invoke-CleanBuildStart }
        'StartRedis'     { Invoke-StartRedis }
        'StopRedis'      { Invoke-StopRedis }
        'RestartRedis'   { Invoke-RestartRedis }
        'StartAll'       { Invoke-StartAll }
        'StopAll'        { Invoke-StopAll }
        'RestartAll'     { Invoke-RestartAll }
        'DeployLocal'    { Invoke-DeployLocal }
        'DeployFull'     { Invoke-DeployFull }
        'RecreateDocker' { Invoke-RecreateDocker }
        'Logs'           { Invoke-Logs "" "All Logs" }
        'LogsRedis'      { Invoke-Logs "redis" "Redis Logs" }
        'LogsApp'        { Invoke-Logs "math-master" "App Logs" }
        'Status'         { Invoke-Status }
        'DeleteLogs'     { Invoke-DeleteLogs }
    }
    exit 0
}

while ($true) {
    Show-MainMenu
    $choice = Read-Host "  Select"
    switch ($choice) {
        '1'  { Invoke-ApplyFormat }
        '2'  { Invoke-SortAnnotations }
        '3'  { Invoke-CleanBuild }
        '4'  { Invoke-CleanBuildStart }
        '5'  { Invoke-StartRedis }
        '6'  { Invoke-StopRedis }
        '7'  { Invoke-RestartRedis }
        '8'  { Invoke-StartAll }
        '9'  { Invoke-StopAll }
        '10' { Invoke-RestartAll }
        '11' { Invoke-DeployLocal }
        '12' { Invoke-DeployFull }
        '13' {
            $svc = Read-Host "  Service name (leave blank for all)"
            $Service = $svc.Trim()
            Invoke-RecreateDocker
        }
        '14' { Invoke-Logs "" "All Logs" }
        '15' { Invoke-Logs "redis" "Redis Logs" }
        '16' { Invoke-Logs "math-master" "App Logs" }
        '17' { Invoke-Status }
        '18' { Invoke-DeleteLogs }
        '19' { Show-Help }
        '20' { Clear-Host }
        '0'  { Write-Host ""; Write-Host "  Goodbye!" -ForegroundColor Cyan; exit 0 }
    }
}

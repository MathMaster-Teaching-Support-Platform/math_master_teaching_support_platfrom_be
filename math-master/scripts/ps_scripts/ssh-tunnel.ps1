# SSH Tunnel to Production Database Server
# Creates a secure tunnel to access PostgreSQL on remote server

param(
    [switch]$Help
)

function Show-Help {
    Write-Host ""
    Write-Host "SSH Tunnel Manager" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "USAGE:" -ForegroundColor Cyan
    Write-Host "  .\ssh-tunnel.ps1" -ForegroundColor White
    Write-Host ""
    Write-Host "DESCRIPTION:" -ForegroundColor Cyan
    Write-Host "  Creates SSH tunnel to production database server" -ForegroundColor White
    Write-Host "  Maps remote PostgreSQL (5432) to local port 5432" -ForegroundColor White
    Write-Host ""
    Write-Host "CONNECTION DETAILS:" -ForegroundColor Cyan
    Write-Host "  Reads from .env file:" -ForegroundColor White
    Write-Host "    SSH_TUNNEL_HOST" -ForegroundColor Gray
    Write-Host "    SSH_TUNNEL_USER" -ForegroundColor Gray
    Write-Host "    SSH_TUNNEL_PASSWORD" -ForegroundColor Gray
    Write-Host "    SSH_TUNNEL_PORT" -ForegroundColor Gray
    Write-Host ""
    Write-Host "NOTES:" -ForegroundColor Cyan
    Write-Host "  - Keep this terminal open while working" -ForegroundColor Yellow
    Write-Host "  - Press Ctrl+C to stop the tunnel" -ForegroundColor Yellow
    Write-Host ""
}

if ($Help) {
    Show-Help
    exit 0
}

# Load .env file
$envFile = Join-Path (Split-Path (Split-Path $PSScriptRoot)) ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([^#][^=]*)\s*=\s*(.*)$') {
            $name = $matches[1].Trim()
            $value = $matches[2].Trim()
            Set-Item -Path "env:$name" -Value $value
        }
    }
}

# Read SSH config from environment variables
$sshHost = $env:SSH_TUNNEL_HOST
$sshUser = $env:SSH_TUNNEL_USER
$sshPassword = $env:SSH_TUNNEL_PASSWORD
$sshPort = $env:SSH_TUNNEL_PORT

if (-not $sshHost -or -not $sshUser -or -not $sshPassword) {
    Write-Host "ERROR: Missing SSH configuration in .env file" -ForegroundColor Red
    Write-Host "Required variables: SSH_TUNNEL_HOST, SSH_TUNNEL_USER, SSH_TUNNEL_PASSWORD" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host ("=" * 60) -ForegroundColor DarkCyan
Write-Host "  SSH Tunnel to Production Database" -ForegroundColor Cyan
Write-Host ("=" * 60) -ForegroundColor DarkCyan
Write-Host ""

Write-Host "Connection Details:" -ForegroundColor Cyan
Write-Host "  Server:   $sshHost" -ForegroundColor White
Write-Host "  User:     $sshUser" -ForegroundColor White
Write-Host "  Tunnel:   localhost:$sshPort -> server PostgreSQL:$sshPort" -ForegroundColor White
Write-Host ""

# Check for automatic authentication tools
$sshpassAvailable = Get-Command sshpass -ErrorAction SilentlyContinue
$plinkAvailable = Get-Command plink -ErrorAction SilentlyContinue

if (-not $sshpassAvailable -and -not $plinkAvailable) {
    Write-Host ""
    Write-Host "WARNING: No automatic SSH authentication tool found!" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "PuTTY is required for automatic password entry." -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Options:" -ForegroundColor Cyan
    Write-Host "  [Y] Install PuTTY now (via winget - recommended)" -ForegroundColor Green
    Write-Host "  [N] Continue with manual password entry" -ForegroundColor Yellow
    Write-Host "  [C] Cancel" -ForegroundColor Gray
    Write-Host ""
    $choice = Read-Host "Your choice (Y/N/C)"

    switch ($choice.ToUpper()) {
        'Y' {
            Write-Host ""
            Write-Host "Installing PuTTY via winget..." -ForegroundColor Cyan
            Write-Host "Note: A UAC prompt may appear - please approve it." -ForegroundColor Yellow
            Write-Host ""

            # Check if winget is available
            $wingetAvailable = Get-Command winget -ErrorAction SilentlyContinue
            if (-not $wingetAvailable) {
                Write-Host "ERROR: winget is not available on this system." -ForegroundColor Red
                Write-Host "Please install PuTTY manually from: https://www.putty.org/" -ForegroundColor Yellow
                Write-Host ""
                Write-Host "Continue with manual password entry? (Y/N)" -ForegroundColor Cyan
                $fallback = Read-Host
                if ($fallback.ToUpper() -ne 'Y') {
                    Write-Host "Cancelled." -ForegroundColor Gray
                    exit 0
                }
            } else {
                # Install PuTTY using winget - run synchronously in same window
                try {
                    Write-Host "Installing... (this may take 30-60 seconds)" -ForegroundColor Cyan

                    # Use Invoke-Expression to run winget synchronously
                    $output = & winget install PuTTY.PuTTY --silent --accept-source-agreements --accept-package-agreements 2>&1
                    $exitCode = $LASTEXITCODE

                    Write-Host ""
                    # Exit code 0 = success, -1978335189 = already installed
                    if ($exitCode -eq 0) {
                        Write-Host "PuTTY installed successfully!" -ForegroundColor Green
                    } elseif ($exitCode -eq -1978335189) {
                        Write-Host "PuTTY is already installed!" -ForegroundColor Green
                    } else {
                        Write-Host "Installation returned exit code: $exitCode" -ForegroundColor Yellow
                        Write-Host ""
                        Write-Host "Continue with manual password entry? (Y/N)" -ForegroundColor Cyan
                        $fallback = Read-Host
                        if ($fallback.ToUpper() -ne 'Y') {
                            Write-Host "Cancelled." -ForegroundColor Gray
                            exit 0
                        }
                        # Skip the plink check below
                        $skipPlinkCheck = $true
                    }

                    if (-not $skipPlinkCheck) {
                        Write-Host ""

                        # Refresh PATH in current session
                        $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")

                        # Wait a moment for PATH to update
                        Start-Sleep -Seconds 2

                        # Check if plink is now available
                        $plinkNowAvailable = Get-Command plink -ErrorAction SilentlyContinue

                        if ($plinkNowAvailable) {
                            Write-Host "plink detected! Continuing with automatic authentication..." -ForegroundColor Green
                            $plinkAvailable = $true
                            Write-Host ""
                        } else {
                            Write-Host "Note: plink not detected in current session." -ForegroundColor Yellow
                            Write-Host ""
                            Write-Host "Options:" -ForegroundColor Cyan
                            Write-Host "  [R] Restart this script in a new terminal (recommended)" -ForegroundColor Green
                            Write-Host "  [C] Continue with manual password entry" -ForegroundColor Yellow
                            Write-Host ""
                            $restartChoice = Read-Host "Your choice (R/C)"

                            if ($restartChoice.ToUpper() -eq 'R') {
                                Write-Host ""
                                Write-Host "Restarting script in new terminal..." -ForegroundColor Cyan
                                Start-Process powershell -ArgumentList "-NoExit", "-File", "`"$PSCommandPath`""
                                exit 0
                            } else {
                                Write-Host ""
                                Write-Host "Continuing with manual password entry..." -ForegroundColor Yellow
                            }
                        }
                    }
                } catch {
                    Write-Host ""
                    Write-Host "ERROR: Failed to install PuTTY: $_" -ForegroundColor Red
                    Write-Host "Please install manually from: https://www.putty.org/" -ForegroundColor Yellow
                    Write-Host ""
                    Write-Host "Continue with manual password entry? (Y/N)" -ForegroundColor Cyan
                    $fallback = Read-Host
                    if ($fallback.ToUpper() -ne 'Y') {
                        Write-Host "Cancelled." -ForegroundColor Gray
                        exit 0
                    }
                }
            }
        }
        'N' {
            Write-Host ""
            Write-Host "Continuing with manual password entry..." -ForegroundColor Yellow
        }
        'C' {
            Write-Host "Cancelled." -ForegroundColor Gray
            exit 0
        }
        default {
            Write-Host "Invalid choice. Cancelled." -ForegroundColor Red
            exit 0
        }
    }
}

Write-Host ""
Write-Host "Starting SSH tunnel..." -ForegroundColor Green
Write-Host "Keep this terminal open. Press Ctrl+C to stop." -ForegroundColor Yellow
Write-Host ""

if ($sshpassAvailable) {
    # Use sshpass to auto-fill password
    Write-Host "Using sshpass for automatic authentication..." -ForegroundColor Cyan
    sshpass -p $sshPassword ssh -o StrictHostKeyChecking=no -L ${sshPort}:localhost:${sshPort} ${sshUser}@${sshHost} -N
} elseif ($plinkAvailable) {
    # Use plink with password parameter
    Write-Host "Using plink (PuTTY) for automatic authentication..." -ForegroundColor Cyan
    Write-Host ""
    # -batch: disable interactive prompts (auto-accept host key on first connection)
    # -pw: password
    # -L: port forwarding
    # -N: no shell, just tunnel
    plink -batch -ssh -pw $sshPassword -L ${sshPort}:localhost:${sshPort} ${sshUser}@${sshHost} -N
} else {
    # Fallback: manual password entry
    Write-Host "Manual password entry required." -ForegroundColor Yellow
    Write-Host "Password from .env: $sshPassword" -ForegroundColor Yellow
    Write-Host "Please enter this password when prompted." -ForegroundColor Yellow
    Write-Host ""
    ssh -L ${sshPort}:localhost:${sshPort} ${sshUser}@${sshHost} -N
}

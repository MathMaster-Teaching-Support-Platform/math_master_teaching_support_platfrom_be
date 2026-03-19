# Math Master PowerShell Scripts

**Platform**: Windows PowerShell 5.1+ / PowerShell 7+

---

## Quick Start

```powershell
# Allow script execution (first time only)
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# Navigate to scripts folder and run the manager
cd scripts\ps_scripts
.\manager.ps1
```

---

## File Structure

```
scripts/ps_scripts/
â”œâ”€â”€ manager.ps1          # Main manager (all logic lives here)
â”œâ”€â”€ deploy-on-local.ps1  # Wrapper -> manager -Task DeployLocal
â”œâ”€â”€ apply-format.ps1     # Wrapper -> manager -Task ApplyFormat
â”œâ”€â”€ install.ps1          # One-time alias installer
â””â”€â”€ README_SCRIPTS_PS.md # This file
```

---

## Tasks Reference

Run any task non-interactively with:

```powershell
.\manager.ps1 -Task <TaskName> [-Service <name>]
```

| Task             | Description                                       |
| ---------------- | ------------------------------------------------- |
| `ApplyFormat`    | Run Spotless code formatter (check + apply)       |
| `CleanBuild`     | `mvnw clean compile` locally (no Docker)          |
| `StartRedis`     | `docker compose up -d redis`                      |
| `StopRedis`      | `docker compose stop redis`                       |
| `RestartRedis`   | `docker compose restart redis`                    |
| `StartAll`       | `docker compose up -d` (all services)             |
| `StopAll`        | `docker compose down`                             |
| `RestartAll`     | `docker compose restart`                          |
| `DeployLocal`    | Build images (no-cache) + `docker compose up -d`  |
| `DeployFull`     | `down` + prune + build no-cache + `up -d`         |
| `RecreateDocker` | `--force-recreate` without rebuild (add -Service) |
| `Logs`           | Tail all service logs (`docker compose logs -f`)  |
| `LogsRedis`      | Tail Redis logs only                              |
| `LogsApp`        | Tail math-master app logs only                    |
| `Status`         | `docker compose ps` + image sizes                 |
| `DeleteLogs`     | Delete `*.log` / `*.txt` from logs/ directories   |
| `Help`           | Print task reference                              |

---

## Interactive Menu

```
  Math Master Manager
  -----------------------------------
  [1]  Format Code (Spotless)
  [2]  Maven Clean Build
  -----------------------------------
  [3]  Start Redis
  [4]  Stop Redis
  [5]  Restart Redis
  -----------------------------------
  [6]  Start All Services
  [7]  Stop All Services
  [8]  Restart All Services
  -----------------------------------
  [9]  Deploy Local (build + up)
  [10] Deploy Full  (down+build+up)
  [11] Recreate Docker Containers
  -----------------------------------
  [12] Tail All Logs
  [13] Tail Redis Logs
  [14] Tail App Logs
  -----------------------------------
  [15] Project Status
  [16] Delete Log Files
  [17] Help
  [18] Clear Screen
  [0]  Exit
```

---

## Common Workflows

### First-time local setup

```powershell
.\manager.ps1 -Task DeployFull
```

### Quick Redis restart (e.g. after password change)

```powershell
.\manager.ps1 -Task RecreateDocker -Service redis
```

### Rebuild only the app image

```powershell
.\manager.ps1 -Task DeployLocal
```

### Watch app logs

```powershell
.\manager.ps1 -Task LogsApp
```

### Format code before committing

```powershell
.\manager.ps1 -Task ApplyFormat
```

---

## Troubleshooting

| Error                   | Fix                                                                  |
| ----------------------- | -------------------------------------------------------------------- |
| `ExecutionPolicy` error | `Set-ExecutionPolicy RemoteSigned -Scope CurrentUser`                |
| Docker not running      | Open Docker Desktop and wait for it to start                         |
| `mvnw.cmd not found`    | Make sure you run from inside `ps_scripts/` or project root          |
| `.env` warnings         | Copy `.env.production.example` to `.env` and fill values             |
| Redis auth failed       | Check `REDIS_PASSWORD` matches in both `.env` and `application.yaml` |

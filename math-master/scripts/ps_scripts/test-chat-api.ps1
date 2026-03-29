param(
  [Parameter(Mandatory = $true)]
  [string]$Token,

  [string]$BaseUrl = "http://localhost:8080/api",

  [string]$Title = "FE Smoke Chat",

  [string]$Prompt = "Xin chao AI, day la test tu script FE/BE"
)

$ErrorActionPreference = "Stop"

function Get-ErrorBody {
  param([Parameter(Mandatory = $true)] $Exception)

  if ($null -eq $Exception.Response) {
    return $Exception.Message
  }

  try {
    $stream = $Exception.Response.GetResponseStream()
    if ($null -eq $stream) {
      return $Exception.Message
    }
    $reader = New-Object System.IO.StreamReader($stream)
    return $reader.ReadToEnd()
  }
  catch {
    return $Exception.Message
  }
}

function Invoke-Api {
  param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("GET", "POST", "PATCH", "DELETE")]
    [string]$Method,

    [Parameter(Mandatory = $true)]
    [string]$Path,

    [object]$Body = $null
  )

  $url = "$BaseUrl$Path"
  $headers = @{
    Authorization = "Bearer $Token"
  }

  Write-Host ""
  Write-Host "=== $Method $url ===" -ForegroundColor Cyan

  try {
    if ($null -ne $Body) {
      $json = $Body | ConvertTo-Json -Depth 10
      Write-Host "Request body: $json" -ForegroundColor DarkGray
      $resp = Invoke-RestMethod -Method $Method -Uri $url -Headers $headers -ContentType "application/json" -Body $json
    }
    else {
      $resp = Invoke-RestMethod -Method $Method -Uri $url -Headers $headers
    }

    $respJson = $resp | ConvertTo-Json -Depth 12
    Write-Host "SUCCESS" -ForegroundColor Green
    Write-Host $respJson

    return $resp
  }
  catch {
    $errorBody = Get-ErrorBody -Exception $_.Exception
    Write-Host "FAILED" -ForegroundColor Red
    Write-Host $errorBody
    throw
  }
}

Write-Host "Starting chat API smoke test..." -ForegroundColor Yellow
Write-Host "BaseUrl: $BaseUrl"

# 1) List sessions
Invoke-Api -Method "GET" -Path "/chat-sessions?page=0&size=20"

# 2) Create session
$createResp = Invoke-Api -Method "POST" -Path "/chat-sessions" -Body @{
  title = $Title
  model = "gemini-2.5-flash"
}

$sessionId = $createResp.result.id
if ([string]::IsNullOrWhiteSpace($sessionId)) {
  throw "Cannot continue because create session did not return result.id"
}

Write-Host "Created sessionId: $sessionId" -ForegroundColor Yellow

# 3) Get session detail
Invoke-Api -Method "GET" -Path "/chat-sessions/$sessionId"

# 4) Rename session
Invoke-Api -Method "PATCH" -Path "/chat-sessions/$sessionId" -Body @{
  title = "FE Smoke Chat Renamed"
}

# 5) Send a message
Invoke-Api -Method "POST" -Path "/chat-sessions/$sessionId/messages" -Body @{
  prompt = $Prompt
}

# 6) Get session messages
Invoke-Api -Method "GET" -Path "/chat-sessions/$sessionId/messages?page=0&size=20&sortBy=createdAt&direction=ASC"

# 7) Get memory info
Invoke-Api -Method "GET" -Path "/chat-sessions/$sessionId/memory"

# 8) Archive session
Invoke-Api -Method "PATCH" -Path "/chat-sessions/$sessionId/archive"

# 9) Delete session
Invoke-Api -Method "DELETE" -Path "/chat-sessions/$sessionId"

Write-Host ""
Write-Host "Chat API smoke test completed." -ForegroundColor Green

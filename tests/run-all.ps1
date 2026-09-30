<#
  Runs the whole DigiQ verification suite against a running instance.

      .\tests\run-all.ps1              run everything
      .\tests\run-all.ps1 -Reset       reload the database first (recommended)

  The application must already be up - start it with .\run.ps1 from the project
  root. Node.js is required for suites 2-6.
#>
[CmdletBinding()]
param([switch]$Reset)

$ErrorActionPreference = 'Continue'
$Here    = Split-Path -Parent $MyInvocation.MyCommand.Path
$Project = Split-Path -Parent $Here

$TOOLS = $env:DIGIQ_TOOLCHAIN
if (-not $TOOLS -or -not (Test-Path $TOOLS)) {
    $TOOLS = Join-Path $env:USERPROFILE 'toolchain'
}
$failed  = 0

function Head($t) {
    Write-Host ""
    Write-Host ("=" * 62) -ForegroundColor DarkGray
    Write-Host "  $t" -ForegroundColor White
    Write-Host ("=" * 62) -ForegroundColor DarkGray
}

# ------------------------------------------------------------------ preflight
try {
    $r = Invoke-WebRequest 'http://localhost:8080/digiq/' -UseBasicParsing -TimeoutSec 8
    if ($r.StatusCode -ne 200) { throw "HTTP $($r.StatusCode)" }
} catch {
    Write-Host "  The application is not responding on http://localhost:8080/digiq/" -ForegroundColor Red
    Write-Host "  Start it first:  .\run.ps1" -ForegroundColor Yellow
    exit 1
}

if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    Write-Host "  Node.js is required for suites 2-6. Install it, or run only 01-smoke.ps1." -ForegroundColor Yellow
}

if ($Reset) {
    Head "Resetting the database"
    $mysql = Get-ChildItem "$TOOLS" -Directory -Filter 'mysql-*' -ErrorAction SilentlyContinue |
             Select-Object -First 1
    if ($mysql) {
        Get-Content (Join-Path $Project 'database\schema.sql') -Raw |
            & "$($mysql.FullName)\bin\mysql.exe" -h 127.0.0.1 -P 3306 -u root
        Write-Host "  schema reloaded" -ForegroundColor Green
    } else {
        Write-Host "  could not find MySQL under $TOOLS - skipping reset" -ForegroundColor Yellow
    }
}

# ------------------------------------------------------------------ node deps
if ((Get-Command node -ErrorAction SilentlyContinue) -and -not (Test-Path (Join-Path $Here 'node_modules'))) {
    Head "Installing test dependencies"
    Push-Location $Here
    npm install --silent --no-fund --no-audit
    Pop-Location
}

# ------------------------------------------------------------------ suites
Head "1/6  Static checks (no server needed)"
node (Join-Path $Here 'static-check-imports.js'); if ($LASTEXITCODE -ne 0) { $failed++ }
node (Join-Path $Here 'static-check-el.js')

Head "2/6  HTTP smoke - every page and role"
& (Join-Path $Here '01-smoke.ps1')
if ($LASTEXITCODE -ne 0) { $failed++ }

Head "3/6  WebSocket live events"
node (Join-Path $Here '02-websocket.js'); if ($LASTEXITCODE -ne 0) { $failed++ }

Head "4/6  Features - registration, CRUD, priority, alerts, cancel"
node (Join-Path $Here '03-features.js'); if ($LASTEXITCODE -ne 0) { $failed++ }

Head "5/6  Concurrency - two counters, one queue"
node (Join-Path $Here '04-concurrency.js'); if ($LASTEXITCODE -ne 0) { $failed++ }

Head "6/6  Public access model - open signup, locked dashboard"
node (Join-Path $Here '06-public-access.js'); if ($LASTEXITCODE -ne 0) { $failed++ }

Write-Host ""
Write-Host ("=" * 62) -ForegroundColor DarkGray
if ($failed -eq 0) {
    Write-Host "  ALL SUITES PASSED" -ForegroundColor Green
} else {
    Write-Host "  $failed SUITE(S) FAILED" -ForegroundColor Red
}
Write-Host ("=" * 62) -ForegroundColor DarkGray
Write-Host ""
exit $failed

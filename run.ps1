<#
  DigiQ - one-command start.

      .\run.ps1              build if needed, start MySQL + Tomcat, open the app
      .\run.ps1 -Build       force a rebuild first
      .\run.ps1 -Reset       reload the database from database/schema.sql (wipes data)
      .\run.ps1 -NoBrowser   don't open a browser window

  By default this looks for a portable toolchain (JDK, Maven, Tomcat, MySQL)
  unpacked side by side in one folder, so nothing has to touch Program Files or the
  registry. It is searched for in this order:

      1. the DIGIQ_TOOLCHAIN environment variable
      2. %USERPROFILE%\toolchain
      3. a "toolchain" folder next to this project

  To point at installs you already have instead, set any of:
      DIGIQ_JAVA_HOME  DIGIQ_MAVEN_HOME  DIGIQ_TOMCAT_HOME  DIGIQ_MYSQL_HOME
#>
[CmdletBinding()]
param(
    [switch]$Build,
    [switch]$Reset,
    [switch]$NoBrowser
)

$ErrorActionPreference = 'Stop'
$ProgressPreference    = 'SilentlyContinue'
$Project = Split-Path -Parent $MyInvocation.MyCommand.Path

$TOOLS = $env:DIGIQ_TOOLCHAIN
if (-not $TOOLS -or -not (Test-Path $TOOLS)) {
    foreach ($candidate in @(
        (Join-Path $env:USERPROFILE 'toolchain'),
        (Join-Path (Split-Path -Parent $Project) 'toolchain')
    )) {
        if (Test-Path $candidate) { $TOOLS = $candidate; break }
    }
}
if (-not $TOOLS) { $TOOLS = Join-Path $env:USERPROFILE 'toolchain' }

function Info($m) { Write-Host "  $m" -ForegroundColor Cyan }
function Ok($m)   { Write-Host "  $m" -ForegroundColor Green }
function Warn($m) { Write-Host "  $m" -ForegroundColor Yellow }
function Die($m)  { Write-Host "  $m" -ForegroundColor Red; exit 1 }

function Resolve-Home($envVar, $fallbackGlob, $mustContain, $label) {
    $explicit = [Environment]::GetEnvironmentVariable($envVar)
    if ($explicit -and (Test-Path (Join-Path $explicit $mustContain))) { return $explicit }
    $found = Get-ChildItem -Path $TOOLS -Directory -Filter $fallbackGlob -ErrorAction SilentlyContinue |
             Where-Object { Test-Path (Join-Path $_.FullName $mustContain) } |
             Sort-Object Name -Descending | Select-Object -First 1
    if ($found) { return $found.FullName }
    Die "$label not found. Set $envVar, or install it under $TOOLS."
}

function Test-Port($port) {
    $c = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    return [bool]$c
}

function Wait-For($port, $label, $seconds = 60) {
    for ($i = 0; $i -lt ($seconds * 2); $i++) {
        if (Test-Port $port) { return $true }
        Start-Sleep -Milliseconds 500
    }
    Warn "$label did not come up on port $port within ${seconds}s"
    return $false
}

Write-Host ""
Write-Host "  DigiQ - Digital Queue Management System" -ForegroundColor White
Write-Host "  ---------------------------------------" -ForegroundColor DarkGray

# ---------------------------------------------------------------- toolchain
$JAVA_HOME   = Resolve-Home 'DIGIQ_JAVA_HOME'   'jdk*'            'bin\javac.exe'  'JDK'
$MAVEN_HOME  = Resolve-Home 'DIGIQ_MAVEN_HOME'  'apache-maven-*'  'bin\mvn.cmd'    'Maven'
$TOMCAT_HOME = Resolve-Home 'DIGIQ_TOMCAT_HOME' 'apache-tomcat-*' 'bin\bootstrap.jar' 'Tomcat'
$MYSQL_HOME  = Resolve-Home 'DIGIQ_MYSQL_HOME'  'mysql-*'         'bin\mysqld.exe' 'MySQL'
$DATA_DIR    = Join-Path $TOOLS 'mysqldata'

$env:JAVA_HOME = $JAVA_HOME
$env:PATH      = "$JAVA_HOME\bin;$MAVEN_HOME\bin;$env:PATH"
Info "JDK    $JAVA_HOME"
Info "Tomcat $TOMCAT_HOME"

# ---------------------------------------------------------------- MySQL
if (Test-Port 3306) {
    Ok "MySQL already listening on 3306"
} else {
    Info "starting MySQL..."
    if (-not (Test-Path $DATA_DIR)) {
        Info "initialising a fresh data directory (root with no password)..."
        & "$MYSQL_HOME\bin\mysqld.exe" --initialize-insecure --datadir="$DATA_DIR" --console 2>&1 | Out-Null
    }
    Start-Process -FilePath "$MYSQL_HOME\bin\mysqld.exe" `
        -ArgumentList "--datadir=$DATA_DIR", "--port=3306", "--console" `
        -WindowStyle Hidden `
        -RedirectStandardOutput "$DATA_DIR\out.log" -RedirectStandardError "$DATA_DIR\err.log"
    if (Wait-For 3306 'MySQL') { Ok "MySQL up on 3306" } else { Die "MySQL failed - see $DATA_DIR\err.log" }
}

# ---------------------------------------------------------------- schema
$hasDb = $false
try {
    $out = & "$MYSQL_HOME\bin\mysql.exe" -h 127.0.0.1 -P 3306 -u root -N -B -e "SHOW DATABASES LIKE 'digiq';" 2>$null
    $hasDb = ($out -match 'digiq')
} catch { }

if ($Reset -or -not $hasDb) {
    if ($Reset) { Warn "resetting the database - all current data will be lost" }
    Info "loading database\schema.sql ..."
    Get-Content (Join-Path $Project 'database\schema.sql') -Raw |
        & "$MYSQL_HOME\bin\mysql.exe" -h 127.0.0.1 -P 3306 -u root
    if ($LASTEXITCODE -ne 0) { Die "schema load failed" }
    Ok "database ready (seeded with demo history)"
} else {
    Ok "database 'digiq' already present"
}

# ---------------------------------------------------------------- build
$war = Join-Path $Project 'target\digiq.war'
if ($Build -or -not (Test-Path $war)) {
    Info "building (mvn clean package)..."
    Push-Location $Project
    & "$MAVEN_HOME\bin\mvn.cmd" -B -q clean package
    $code = $LASTEXITCODE
    Pop-Location
    if ($code -ne 0) { Die "build failed - run 'mvn clean package' to see the errors" }
    Ok "built target\digiq.war"
} else {
    Ok "using existing target\digiq.war (pass -Build to rebuild)"
}

# ---------------------------------------------------------------- deploy
Info "deploying to Tomcat..."
Remove-Item -LiteralPath "$TOMCAT_HOME\webapps\digiq" -Recurse -Force -ErrorAction SilentlyContinue
Copy-Item $war "$TOMCAT_HOME\webapps\digiq.war" -Force

# ---------------------------------------------------------------- Tomcat
if (Test-Port 8080) {
    Ok "Tomcat already listening on 8080 (it will pick up the new WAR)"
} else {
    Info "starting Tomcat..."
    Start-Process -FilePath "$JAVA_HOME\bin\java.exe" -ArgumentList @(
        "-Dcatalina.home=$TOMCAT_HOME", "-Dcatalina.base=$TOMCAT_HOME",
        "-Djava.io.tmpdir=$TOMCAT_HOME\temp", "-Dfile.encoding=UTF-8",
        "-cp", "$TOMCAT_HOME\bin\bootstrap.jar;$TOMCAT_HOME\bin\tomcat-juli.jar",
        "org.apache.catalina.startup.Bootstrap", "start"
    ) -WindowStyle Hidden `
      -RedirectStandardOutput "$TOMCAT_HOME\logs\stdout.log" `
      -RedirectStandardError  "$TOMCAT_HOME\logs\stderr.log"
    if (-not (Wait-For 8080 'Tomcat')) { Die "Tomcat failed - see $TOMCAT_HOME\logs\stderr.log" }
}

# ---------------------------------------------------------------- wait for the app
$url = 'http://localhost:8080/digiq/'
$up = $false
for ($i = 0; $i -lt 90; $i++) {
    Start-Sleep -Milliseconds 700
    try {
        if ((Invoke-WebRequest $url -UseBasicParsing -TimeoutSec 5).StatusCode -eq 200) { $up = $true; break }
    } catch { }
}
if (-not $up) { Die "app did not start - see $TOMCAT_HOME\logs\stderr.log" }

Write-Host ""
Ok "DigiQ is running"
Write-Host ""
Write-Host "    Open      $url"                              -ForegroundColor White
Write-Host "    Board     ${url}board   (full-screen display)" -ForegroundColor Gray
Write-Host ""
Write-Host "    Sign in with password  Digiq@123" -ForegroundColor White
Write-Host "      admin@digiq.com   administrator" -ForegroundColor Gray
Write-Host "      grace@digiq.com   counter staff"  -ForegroundColor Gray
Write-Host "      joseph@mail.com   customer"       -ForegroundColor Gray
Write-Host ""
Write-Host "    Stop it with  .\stop.ps1" -ForegroundColor DarkGray
Write-Host ""

if (-not $NoBrowser) { Start-Process $url }

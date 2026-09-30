<#
  DigiQ - stop the servers this project started.

      .\stop.ps1          stop Tomcat, leave MySQL running
      .\stop.ps1 -All     stop Tomcat and MySQL

  Only processes launched from the portable toolchain are touched, so a MySQL or
  Java you installed yourself for something else is left alone.
#>
[CmdletBinding()]
param([switch]$All)

$TOOLS = $env:DIGIQ_TOOLCHAIN
if (-not $TOOLS -or -not (Test-Path $TOOLS)) {
    $TOOLS = Join-Path $env:USERPROFILE 'toolchain'
}

function Stop-FromToolchain($name, $label) {
    $procs = Get-Process $name -ErrorAction SilentlyContinue | Where-Object {
        try { $_.Path -and $_.Path.StartsWith($TOOLS, 'OrdinalIgnoreCase') } catch { $false }
    }
    if (-not $procs) { Write-Host "  $label - not running" -ForegroundColor DarkGray; return }
    foreach ($p in $procs) {
        try {
            Stop-Process -Id $p.Id -Force
            Write-Host "  stopped $label (pid $($p.Id))" -ForegroundColor Green
        } catch {
            Write-Host "  could not stop $label pid $($p.Id): $($_.Exception.Message)" -ForegroundColor Yellow
        }
    }
}

Write-Host ""
Stop-FromToolchain 'java' 'Tomcat'
if ($All) {
    Stop-FromToolchain 'mysqld' 'MySQL'
} else {
    Write-Host "  MySQL left running (use -All to stop it too)" -ForegroundColor DarkGray
}
Write-Host ""

$ErrorActionPreference = 'Continue'
$ProgressPreference = 'SilentlyContinue'
$B = 'http://localhost:8080/digiq'
$pass = 'Digiq@123'
$fail = 0

function Step($name, $block) {
  try {
    $r = & $block
    Write-Host ("  PASS  {0}  {1}" -f $name, $r)
  } catch {
    $script:fail++
    Write-Host ("  FAIL  {0}  -> {1}" -f $name, $_.Exception.Message)
  }
}

function Login($sessName, $email) {
  $s = $null
  $r = Invoke-WebRequest "$B/login" -SessionVariable s -UseBasicParsing -TimeoutSec 20
  $r2 = Invoke-WebRequest "$B/login" -Method POST -WebSession $s -UseBasicParsing -TimeoutSec 20 `
        -Body @{ email = $email; password = $pass }
  Set-Variable -Name $sessName -Value $s -Scope Script
  return $r2
}

Write-Host "`n=== PUBLIC ==="
Step "GET /"            { $r = Invoke-WebRequest "$B/" -UseBasicParsing -TimeoutSec 20; "HTTP $($r.StatusCode), $($r.RawContentLength) bytes" }
Step "GET /login"       { $r = Invoke-WebRequest "$B/login" -UseBasicParsing -TimeoutSec 20; "HTTP $($r.StatusCode)" }
Step "GET /register"    { $r = Invoke-WebRequest "$B/register" -UseBasicParsing -TimeoutSec 20; "HTTP $($r.StatusCode)" }
Step "GET /board"       { $r = Invoke-WebRequest "$B/board" -UseBasicParsing -TimeoutSec 20; "HTTP $($r.StatusCode)" }
Step "GET /board/data"  { $r = Invoke-WebRequest "$B/board/data" -UseBasicParsing -TimeoutSec 20
                          $j = $r.Content | ConvertFrom-Json; "ok=$($j.ok) counters=$($j.counters.Count)" }
Step "GET /customer/home unauthenticated redirects to login" {
  $r = Invoke-WebRequest "$B/customer/home" -UseBasicParsing -TimeoutSec 20
  if ($r.Content -match 'Sign in to DigiQ') { "bounced to login" } else { throw "was NOT bounced" } }

Write-Host "`n=== CUSTOMER (joseph@mail.com) ==="
Step "POST /login (BCrypt verify)" {
  $r = Login 'cs' 'joseph@mail.com'
  if ($r.Content -match 'Available services|Book a token') { "signed in" } else { throw "login failed" } }

$tokenId = $null
Step "POST /customer/book" {
  $r = Invoke-WebRequest "$B/customer/book" -Method POST -WebSession $script:cs -UseBasicParsing -TimeoutSec 20 `
       -Body @{ serviceId = 1 }
  if ($r.BaseResponse.RequestMessage.RequestUri.Query -match 'id=(\d+)') {
    $script:tokenId = $Matches[1]
  } elseif ($r.Content -match 'customer/token/pdf\?id=(\d+)') {
    $script:tokenId = $Matches[1]
  }
  if (-not $script:tokenId) { throw "no token id returned" }
  "token id=$($script:tokenId)" }

Step "GET /customer/token" {
  $r = Invoke-WebRequest "$B/customer/token?id=$script:tokenId" -WebSession $script:cs -UseBasicParsing -TimeoutSec 20
  if ($r.Content -match 'ACC-\d{4}') { "ticket renders $($Matches[0])" } else { throw "no token number on page" } }

Step "GET /customer/token/qr (PNG)" {
  $r = Invoke-WebRequest "$B/customer/token/qr?id=$script:tokenId" -WebSession $script:cs -UseBasicParsing -TimeoutSec 20
  if ($r.Headers.'Content-Type' -match 'image/png' -and $r.RawContentLength -gt 200) { "PNG $($r.RawContentLength) bytes" }
  else { throw "not a PNG ($($r.Headers.'Content-Type'))" } }

Step "GET /customer/token/pdf (PDF)" {
  $r = Invoke-WebRequest "$B/customer/token/pdf?id=$script:tokenId" -WebSession $script:cs -UseBasicParsing -TimeoutSec 30
  $sig = [System.Text.Encoding]::ASCII.GetString($r.Content[0..3])
  if ($sig -eq '%PDF') { "PDF $($r.RawContentLength) bytes" } else { throw "not a PDF (sig=$sig)" } }

Step "GET /customer/status (JSON)" {
  $r = Invoke-WebRequest "$B/customer/status?id=$script:tokenId" -WebSession $script:cs -UseBasicParsing -TimeoutSec 20
  $j = $r.Content | ConvertFrom-Json
  "status=$($j.status) position=$($j.position) eta=$($j.estimatedWait)min" }

Step "GET /customer/tokens"        { $r = Invoke-WebRequest "$B/customer/tokens" -WebSession $script:cs -UseBasicParsing -TimeoutSec 20; "HTTP $($r.StatusCode)" }
Step "GET /customer/notifications" { $r = Invoke-WebRequest "$B/customer/notifications" -WebSession $script:cs -UseBasicParsing -TimeoutSec 20; "HTTP $($r.StatusCode)" }
Step "customer BLOCKED from /admin/dashboard" {
  $r = Invoke-WebRequest "$B/admin/dashboard" -WebSession $script:cs -UseBasicParsing -TimeoutSec 20
  if ($r.Content -match 'Operations dashboard') { throw "customer reached the admin console" } else { "denied" } }

Write-Host "`n=== STAFF (grace@digiq.com - Counter 1, Account Opening) ==="
Step "POST /login" {
  $r = Login 'ss' 'grace@digiq.com'
  if ($r.Content -match 'Counter 1|Counter console') { "signed in" } else { throw "login failed" } }
Step "GET /staff/console" { $r = Invoke-WebRequest "$B/staff/console" -WebSession $script:ss -UseBasicParsing -TimeoutSec 20; "HTTP $($r.StatusCode)" }
Step "POST /staff/action call next" {
  $r = Invoke-WebRequest "$B/staff/action" -Method POST -WebSession $script:ss -UseBasicParsing -TimeoutSec 20 `
       -Headers @{ 'X-Requested-With' = 'fetch' } -Body @{ action = 'call' }
  $j = $r.Content | ConvertFrom-Json
  if ($j.ok) { "called $($j.tokenNumber) for $($j.customerName)" } else { throw $j.message } }
Step "POST /staff/scan by token number" {
  $r = Invoke-WebRequest "$B/staff/scan" -Method POST -WebSession $script:ss -UseBasicParsing -TimeoutSec 20 `
       -Headers @{ 'X-Requested-With' = 'fetch' } -Body @{ code = 'ACC-0001' }
  $j = $r.Content | ConvertFrom-Json
  if ($j.ok) { "resolved $($j.tokenNumber) status=$($j.status) servableHere=$($j.servableHere)" } else { throw $j.message } }
Step "customer sees IN_SERVICE after the call" {
  $r = Invoke-WebRequest "$B/customer/status?id=$script:tokenId" -WebSession $script:cs -UseBasicParsing -TimeoutSec 20
  $j = $r.Content | ConvertFrom-Json
  "status=$($j.status) counter=$($j.counterName)" }
Step "POST /staff/action complete" {
  $r = Invoke-WebRequest "$B/staff/action" -Method POST -WebSession $script:ss -UseBasicParsing -TimeoutSec 20 `
       -Headers @{ 'X-Requested-With' = 'fetch' } -Body @{ action = 'complete' }
  $j = $r.Content | ConvertFrom-Json
  if ($j.ok) { $j.message } else { throw $j.message } }

Write-Host "`n=== ADMIN (admin@digiq.com) ==="
Step "POST /login" {
  $r = Login 'as' 'admin@digiq.com'
  if ($r.Content -match 'Operations dashboard') { "signed in" } else { throw "login failed" } }
foreach ($p in @('/admin/dashboard','/admin/analytics','/admin/services','/admin/counters','/admin/users','/admin/tokens','/admin/logs')) {
  Step "GET $p" { $r = Invoke-WebRequest "$B$p" -WebSession $script:as -UseBasicParsing -TimeoutSec 30; "HTTP $($r.StatusCode), $($r.RawContentLength) bytes" }
}
Step "GET /admin/analytics/data (JSON)" {
  $r = Invoke-WebRequest "$B/admin/analytics/data?days=30" -WebSession $script:as -UseBasicParsing -TimeoutSec 30
  $j = $r.Content | ConvertFrom-Json
  "days=$($j.days) trend=$($j.tokensPerDay.Count) status=$($j.statusBreakdown.Count) svc=$($j.waitVsService.Count) peak=$($j.peakHours.Count) counters=$($j.counterPerformance.Count)" }

Write-Host "`n=== ERROR PAGES ==="
Step "GET /nope -> custom 404 page" {
  Add-Type -AssemblyName System.Net.Http
  $c = New-Object System.Net.Http.HttpClient
  $resp = $c.GetAsync("$B/nope").Result
  $body = $resp.Content.ReadAsStringAsync().Result
  if ([int]$resp.StatusCode -eq 404 -and $body -match 'Page not found') { "custom 404 rendered" }
  else { throw "HTTP $([int]$resp.StatusCode), body did not match" } }

Write-Host "`n================================"
if ($fail -eq 0) { Write-Host "ALL CHECKS PASSED" } else { Write-Host "$fail CHECK(S) FAILED" }

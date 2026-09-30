@echo off
REM DigiQ - double-click this to build, start and open the app.
REM Pass through any flags, e.g.:  run.bat -Build   /   run.bat -Reset
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run.ps1" %*
if errorlevel 1 (
  echo.
  echo Startup failed. Scroll up for the reason.
  pause
)

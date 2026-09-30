@echo off
REM DigiQ - stop the servers. Use "stop.bat -All" to stop MySQL as well.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop.ps1" %*

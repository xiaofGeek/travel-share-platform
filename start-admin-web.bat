@echo off
setlocal EnableExtensions
cd /d "%~dp0admin-web"
powershell.exe -NoProfile -Command "if (Get-NetTCPConnection -LocalPort 5174 -State Listen -ErrorAction SilentlyContinue) { exit 1 }"
if errorlevel 1 echo Port 5174 is already in use. Run stop-all.bat before starting again.& exit /b 1
if not exist "node_modules" call npm.cmd install
if errorlevel 1 exit /b 1
echo Starting admin console at http://localhost:5174
call npm.cmd run dev
endlocal

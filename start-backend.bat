@echo off
setlocal EnableExtensions
cd /d "%~dp0"
powershell.exe -NoProfile -Command "if (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue) { exit 1 }"
if errorlevel 1 echo Port 8080 is already in use. Run stop-all.bat before starting again.& exit /b 1
if exist ".env" for /f "usebackq tokens=1,* delims==" %%A in (".env") do if not "%%A"=="" set "%%A=%%B"
for /f "usebackq delims=" %%L in (`powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\resolve-tools.ps1" -EmitBatch`) do %%L
if not defined JAVA_HOME echo Java not found. Run check-environment.bat first.& exit /b 1
cd backend
echo Starting Spring Boot at http://localhost:8080
call mvnw.cmd spring-boot:run
endlocal

@echo off
setlocal EnableExtensions
cd /d "%~dp0"
call check-environment.bat
echo [1/6] Installing and building user website...
cd user-web
call npm.cmd install
if errorlevel 1 exit /b 1
call npm.cmd run build
if errorlevel 1 exit /b 1
cd ..\admin-web
echo [2/6] Installing and building admin console...
call npm.cmd install
if errorlevel 1 exit /b 1
call npm.cmd run build
if errorlevel 1 exit /b 1
cd ..\backend
echo [3/6] Running backend tests...
call mvnw.cmd clean test
if errorlevel 1 exit /b 1
echo [4/6] Packaging backend...
call mvnw.cmd clean package
if errorlevel 1 exit /b 1
echo [5/6] Checking generated artifacts...
if not exist "target\travel-share-platform.jar" exit /b 1
echo [6/6] Build completed successfully.
echo User website: user-web\dist
echo Admin console: admin-web\dist
echo Backend: backend\target\travel-share-platform.jar
endlocal


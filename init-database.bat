@echo off
setlocal EnableExtensions
cd /d "%~dp0"
if exist ".env" for /f "usebackq tokens=1,* delims==" %%A in (".env") do if not "%%A"=="" set "%%A=%%B"
for /f "usebackq delims=" %%L in (`powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\resolve-tools.ps1" -EmitBatch`) do %%L
if not defined MYSQL_EXE echo MySQL client not found.& exit /b 1
if not defined DB_USERNAME set "DB_USERNAME=root"
if not defined DB_PASSWORD set /p "DB_PASSWORD=Enter MySQL password for %DB_USERNAME%: "
echo Importing schema...
"%MYSQL_EXE%" --default-character-set=utf8mb4 -u"%DB_USERNAME%" -p"%DB_PASSWORD%" < "sql\schema.sql"
if errorlevel 1 exit /b 1
echo Importing base data...
"%MYSQL_EXE%" --default-character-set=utf8mb4 -u"%DB_USERNAME%" -p"%DB_PASSWORD%" travel_share < "sql\base-data.sql"
if errorlevel 1 exit /b 1
echo Importing demo data...
"%MYSQL_EXE%" --default-character-set=utf8mb4 -u"%DB_USERNAME%" -p"%DB_PASSWORD%" travel_share < "sql\demo-data.sql"
if errorlevel 1 exit /b 1
echo Database travel_share initialized successfully.
endlocal


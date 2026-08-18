@echo off
setlocal
cd /d "%~dp0"
echo C端: http://localhost:5173
echo 管理端: http://localhost:5174
echo 后端: http://localhost:8080
start "Travel Share Backend" cmd /k ""%~dp0start-backend.bat""
start "Travel Share User Web" cmd /k ""%~dp0start-user-web.bat""
start "Travel Share Admin Web" cmd /k ""%~dp0start-admin-web.bat""
endlocal


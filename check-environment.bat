@echo off
setlocal EnableExtensions
cd /d "%~dp0"
echo [Travel Share] Checking current terminal commands...
java -version
javac -version
mvn -version
node -v
call npm.cmd -v
mysql --version
where java
where javac
where mvn
where node
where npm
where mysql
echo.
echo [Travel Share] Resolving installed tools without changing PATH...
for /f "usebackq delims=" %%L in (`powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\resolve-tools.ps1" -EmitBatch`) do %%L
if defined JAVA_EXE "%JAVA_EXE%" -version
if defined JAVA_HOME "%JAVA_HOME%\bin\javac.exe" -version
if defined MVN_CMD call "%MVN_CMD%" -version
if defined NODE_EXE "%NODE_EXE%" -v
if defined NPM_CMD call "%NPM_CMD%" -v
if defined MYSQL_EXE "%MYSQL_EXE%" --version
echo.
echo Environment report: docs\环境检测结果.md
endlocal


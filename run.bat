@echo off
REM ========================================================
REM Student Management System - Run Script (Windows CMD)
REM ========================================================
cd /d "%~dp0"

echo [1/2] Compiling Java source files...
if not exist bin mkdir bin
javac -cp "lib\*" -d bin src\com\student\model\*.java src\com\student\dao\*.java src\com\student\web\*.java src\com\student\*.java

if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed!
    pause
    exit /b %errorlevel%
)

echo [2/2] Starting Student Management System...
echo ========================================================
java -cp "bin;lib\*" com.student.Main

pause

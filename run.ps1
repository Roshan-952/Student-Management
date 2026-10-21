# Student Management System - PowerShell Run Script
$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

Write-Host "Compiling Java source files..." -ForegroundColor Cyan
if (-not (Test-Path "bin")) { New-Item -ItemType Directory -Path "bin" | Out-Null }

$javaFiles = Get-ChildItem -Path "src" -Recurse -Filter *.java | ForEach-Object { $_.FullName }
& javac -cp "lib\*" -d bin $javaFiles

if ($LASTEXITCODE -eq 0) {
    Write-Host "Starting Student Management System..." -ForegroundColor Green
    & java -cp "bin;lib\*" com.student.Main
} else {
    Write-Host "Compilation failed with code $LASTEXITCODE" -ForegroundColor Red
}

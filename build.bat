@echo off
setlocal enabledelayedexpansion
echo [MedTrack] Compiling Java backend...

if not exist bin mkdir bin

powershell -NoProfile -Command "Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { '\"' + $_.FullName.Replace('\', '/') + '\"' } | Out-File -Encoding ascii sources.txt"

javac -encoding UTF-8 -cp "lib/*" -d bin @sources.txt
set BUILD_STATUS=%ERRORLEVEL%
if exist sources.txt del sources.txt

if %BUILD_STATUS% NEQ 0 (
    echo [ERROR] Compilation failed with status %BUILD_STATUS%!
    exit /b %BUILD_STATUS%
)

echo [MedTrack] Compilation successful!

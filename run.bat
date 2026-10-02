@echo off
echo ===================================================
echo   Starting MedTrack Healthcare Management System
echo ===================================================

call build.bat
if %ERRORLEVEL% NEQ 0 (
    echo Build failed. Aborting launch.
    pause
    exit /b %ERRORLEVEL%
)

echo Starting Java Application...
java -cp "bin;lib/*" com.medtrack.Main
pause

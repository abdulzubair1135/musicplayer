@echo off
title Z-Music Backend Server
color 0B
cls
echo ========================================================
echo               Z-MUSIC BACKEND SERVER
echo ========================================================
echo.
echo [1/4] Checking Node.js environment...
where node >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Node.js is not installed or not in PATH!
    echo Please install Node.js from https://nodejs.org
    pause
    exit /b 1
)

echo [2/4] Configuring USB / ADB connection for Android phone...
set ADB_PATH=C:\Users\abdul\AppData\Local\Android\Sdk\platform-tools\adb.exe
if exist "%ADB_PATH%" (
    "%ADB_PATH%" reverse tcp:3000 tcp:3000 >nul 2>nul
    echo       [OK] ADB reverse port 3000 mapped to phone successfully!
) else (
    where adb >nul 2>nul
    if %errorlevel% equ 0 (
        adb reverse tcp:3000 tcp:3000 >nul 2>nul
        echo       [OK] ADB reverse port 3000 mapped to phone!
    ) else (
        echo       [INFO] Phone can connect via local Wi-Fi or Render cloud.
    )
)

echo [3/4] Moving to backend folder and verifying dependencies...
cd /d "%~dp0backend"
if not exist node_modules (
    echo Installing backend dependencies, please wait...
    call npm install
)

echo [4/4] Starting Z-Music Streaming Server on http://localhost:3000 ...
echo.
echo --------------------------------------------------------
echo  Server is LIVE! Keep this window OPEN while using
echo  Z-Music locally. Press Ctrl+C to stop the server.
echo --------------------------------------------------------
echo.
node server.js
pause

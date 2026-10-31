@echo off
title Z-Music Backend Server
color 0B
cls
echo ========================================================
echo               Z-MUSIC BACKEND SERVER
echo ========================================================
echo.
echo [1/3] Checking Node.js environment...
where node >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Node.js is not installed or not in PATH!
    echo Please install Node.js from https://nodejs.org
    pause
    exit /b 1
)

echo [2/3] Moving to backend folder and verifying dependencies...
cd /d "%~dp0backend"
if not exist node_modules (
    echo Installing backend dependencies, please wait...
    call npm install
)

echo [3/3] Starting Z-Music Streaming Server on http://localhost:3000 ...
echo.
echo --------------------------------------------------------
echo  Server is starting! Keep this window OPEN while using
echo  Z-Music locally. Press Ctrl+C to stop the server.
echo --------------------------------------------------------
echo.
node server.js
pause

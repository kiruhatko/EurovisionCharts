@echo off
rem Dzerkalo MBTI: alternative tunnel via localhost.run (uses the SSH client built into Windows 10/11).
rem Use this if start.bat fails with "failed to request quick Tunnel".
setlocal
title Dzerkalo MBTI (ssh tunnel)
cd /d "%~dp0"

where py >nul 2>nul
if errorlevel 1 goto nopy
where ssh >nul 2>nul
if errorlevel 1 goto nossh

echo Starting the page on http://localhost:8765 ...
start "Dzerkalo server" /min py -m http.server 8765
timeout /t 2 /nobreak >nul

echo.
echo ================================================================
echo  Find the line with  https://....lhr.life  (or similar) below
echo  and send that link to your friends.
echo  It works only while this window is open.
echo ================================================================
echo.
ssh -o StrictHostKeyChecking=accept-new -o ServerAliveInterval=30 -R 80:localhost:8765 nokey@localhost.run

taskkill /f /t /fi "WINDOWTITLE eq Dzerkalo server*" >nul 2>nul
echo.
echo Sharing stopped.
pause
exit /b 0

:nopy
echo Python launcher "py" was not found.
echo Install Python from https://www.python.org/downloads/ and tick "Add python.exe to PATH",
echo or run:  winget install Python.Python.3.12
pause
exit /b 1

:nossh
echo The SSH client was not found. In Windows Settings open Apps - Optional features
echo and add "OpenSSH Client", then run this file again.
pause
exit /b 1

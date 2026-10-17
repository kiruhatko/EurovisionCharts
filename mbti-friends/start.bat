@echo off
rem Dzerkalo MBTI: local server + temporary public link (Cloudflare quick tunnel).
rem Double-click this file. Close this window (or press Ctrl+C) to stop sharing.
setlocal
title Dzerkalo MBTI
cd /d "%~dp0"

where py >nul 2>nul
if errorlevel 1 goto nopy

set "CF=cloudflared"
where cloudflared >nul 2>nul
if not errorlevel 1 goto havecf
set "CF=%~dp0cloudflared.exe"
if exist "%CF%" goto havecf
echo Downloading cloudflared (one time only, about 40 MB)...
curl.exe -L --fail -o "%CF%" https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe
if errorlevel 1 goto nocf
:havecf

echo Starting the page on http://localhost:8765 ...
start "Dzerkalo server" /min py -m http.server 8765
timeout /t 2 /nobreak >nul

echo.
echo ================================================================
echo  Find the line with  https://....trycloudflare.com  below
echo  and send that link to your friends.
echo  It works only while this window is open.
echo  If Windows Firewall asks about Python, you can click Cancel.
echo ================================================================
echo.
"%CF%" tunnel --url http://localhost:8765

taskkill /f /t /fi "WINDOWTITLE eq Dzerkalo server*" >nul 2>nul
echo.
echo Sharing stopped.
echo If you saw "failed to request quick Tunnel", run start-ssh.bat instead.
pause
exit /b 0

:nopy
echo Python launcher "py" was not found.
echo Install Python from https://www.python.org/downloads/ and tick "Add python.exe to PATH",
echo or run:  winget install Python.Python.3.12
echo Then close this window and double-click start.bat again.
pause
exit /b 1

:nocf
echo Could not download cloudflared. Check your internet connection, or download
echo cloudflared-windows-amd64.exe from https://github.com/cloudflare/cloudflared/releases
echo and save it next to start.bat as cloudflared.exe
pause
exit /b 1

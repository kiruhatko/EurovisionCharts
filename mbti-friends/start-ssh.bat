@echo off
rem Dzerkalo MBTI: temporary public link via localhost.run (uses the SSH client built into Windows 10/11).
rem Reconnects automatically if the tunnel drops; every reconnect prints a NEW link.
setlocal
title Dzerkalo MBTI (localhost.run)
cd /d "%~dp0"
if not exist "index.html" goto badfolder

where py >nul 2>nul
if errorlevel 1 goto nopy
where ssh >nul 2>nul
if errorlevel 1 goto nossh

rem --- serve ONLY a copy of index.html, on this computer only (127.0.0.1) ---
set "WWW=%TEMP%\dzerkalo-www"
call :killsrv
if exist "%WWW%" rd /s /q "%WWW%"
mkdir "%WWW%"
copy /y "index.html" "%WWW%\index.html" >nul
if errorlevel 1 goto badcopy
echo Starting the page on http://127.0.0.1:8765 ...
start "Dzerkalo server" /min py -m http.server 8765 --bind 127.0.0.1 --directory "%WWW%"
set /a TRIES=0
:waitsrv
curl.exe -s -f -o nul --max-time 2 http://127.0.0.1:8765/index.html
if not errorlevel 1 goto srvup
set /a TRIES+=1
if %TRIES% geq 10 goto srvfail
ping -n 2 127.0.0.1 >nul
goto waitsrv
:srvup
echo The page is up.

echo.
echo ================================================================
echo  1. Wait for a line with  https://....lhr.life  below.
echo  2. OPEN THAT https LINK YOURSELF and create the room THERE,
echo     so the invite link you copy is the public one.
echo  3. Send the invite link (or just the room code) to your friends.
echo  If you see TUNNEL DROPPED, the old link is dead: use the NEW one.
echo  Keep this window open and the computer awake. Close it to stop.
echo ================================================================

:again
echo.
echo [%time%] Connecting...
ssh -o StrictHostKeyChecking=accept-new -o ServerAliveInterval=15 -o ServerAliveCountMax=3 -o ConnectTimeout=20 -R 80:127.0.0.1:8765 nokey@localhost.run
echo.
echo ****************************************************************
echo  TUNNEL DROPPED (ssh exit code %errorlevel%). The old link no longer works.
echo  Reconnecting in 5 seconds. A NEW link will appear BELOW.
echo  Rooms keep working for people who already opened the page.
echo ****************************************************************
ping -n 6 127.0.0.1 >nul
goto again

:killsrv
powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort 8765 -State Listen -ErrorAction SilentlyContinue | ForEach-Object { $p = Get-Process -Id $_.OwningProcess -ErrorAction SilentlyContinue; if ($p -and $p.ProcessName -like 'python*') { Stop-Process -Id $p.Id -Force } }" >nul 2>nul
exit /b 0

:badfolder
echo index.html was not found next to this file.
echo Unzip the archive first and run this file from the unzipped folder (not from inside the zip).
pause
exit /b 1

:badcopy
echo Could not copy index.html to %WWW%
pause
exit /b 1

:srvfail
echo The local web server did not start (Python missing, or something else uses port 8765).
echo Close all black windows and run this file again. If it repeats, restart the computer.
pause
exit /b 1

:nopy
echo Python launcher "py" was not found.
echo Install Python from https://www.python.org/downloads/ (keep the "py launcher" option ticked)
echo or run:  winget install Python.Python.3.12
echo Then close ALL windows and double-click this file again.
pause
exit /b 1

:nossh
echo The SSH client was not found. In Windows Settings open Apps - Optional features
echo and add "OpenSSH Client", then run this file again.
pause
exit /b 1

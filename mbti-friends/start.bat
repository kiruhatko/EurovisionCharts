@echo off
rem Dzerkalo MBTI: temporary public link via Cloudflare quick tunnel (downloads cloudflared on first run).
rem If you see "failed to request quick Tunnel", your network cannot reach Cloudflare: use start-ssh.bat.
setlocal
title Dzerkalo MBTI (cloudflare)
pushd "%~dp0"
if not exist "index.html" goto badfolder

rem --- find a working Python 3.7+ (checked by really running it) ---
where py >nul 2>nul
if errorlevel 1 goto trypython
set "PY=py"
py -c "import sys;sys.exit(0 if sys.version_info>=(3,7) else 1)"
if not errorlevel 1 goto havepy
:trypython
set "PY=python"
python -c "import sys;sys.exit(0 if sys.version_info>=(3,7) else 1)"
if not errorlevel 1 goto havepy
goto nopy
:havepy

set "CF=cloudflared"
where cloudflared >nul 2>nul
if not errorlevel 1 goto havecf
set "CF=cloudflared.exe"
if exist "%CF%" goto havecf
echo Downloading cloudflared (one time only, about 55 MB)...
curl.exe -L --fail --retry 5 --retry-delay 3 -o cloudflared.exe.part https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe
if errorlevel 1 goto nocf
move /y cloudflared.exe.part cloudflared.exe >nul
if not exist "cloudflared.exe" goto nocf
:havecf

rem --- serve ONLY a copy of index.html, on this computer only (127.0.0.1) ---
set "WWW=%TEMP%\dzerkalo-www"
call :killsrv
ping -n 2 127.0.0.1 >nul
curl.exe -s -o nul --max-time 2 http://127.0.0.1:8765/
if not errorlevel 1 goto portbusy
if exist "%WWW%" rd /s /q "%WWW%"
mkdir "%WWW%"
copy /y "index.html" "%WWW%\index.html" >nul
if errorlevel 1 goto badcopy
echo Starting the local page...
start "Dzerkalo server" /min /d "%TEMP%" %PY% -m http.server 8765 --bind 127.0.0.1 --directory "%WWW%"
set /a TRIES=0
:waitsrv
curl.exe -s -f -o nul --max-time 2 http://127.0.0.1:8765/index.html
if not errorlevel 1 goto srvup
set /a TRIES+=1
if %TRIES% geq 30 goto srvfail
ping -n 2 127.0.0.1 >nul
goto waitsrv
:srvup
echo The page is ready. Waiting for the public link...

echo.
echo ================================================================
echo  1. Wait for a line with  https://....trycloudflare.com  below.
echo  2. OPEN THAT https LINK YOURSELF and create the room THERE,
echo     so the invite link you copy is the public one.
echo  3. Send friends the invite link (or the link + the room code).
echo  A small window "Dzerkalo server" also runs: do not close it.
echo  To stop everything, close BOTH windows.
echo  If the window title starts with "Select", press Esc or Enter:
echo     clicking in this window pauses it.
echo ================================================================
echo.
"%CF%" tunnel --url http://127.0.0.1:8765
set "RC=%errorlevel%"
echo.
echo Tunnel stopped (exit code %RC%). The link no longer works.
echo If you saw "failed to request quick Tunnel", run start-ssh.bat instead.
call :killsrv
pause
exit /b 0

:killsrv
powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort 8765 -State Listen -ErrorAction SilentlyContinue | ForEach-Object { $p = Get-Process -Id $_.OwningProcess -ErrorAction SilentlyContinue; if ($p -and $p.ProcessName -like 'python*') { Stop-Process -Id $p.Id -Force } }" >nul 2>nul
exit /b 0

:badfolder
echo index.html was not found next to this file.
echo Unzip the archive first and run this file from the unzipped folder (not from inside the zip).
pause
exit /b 1

:badcopy
echo Could not copy index.html to the temporary folder.
pause
exit /b 1

:portbusy
echo Port 8765 is still used by another program (maybe an old copy of this page).
echo Restart the computer and run this file again.
pause
exit /b 1

:srvfail
call :killsrv
echo The local web server did not start in 30 seconds.
echo Run this file again. If it repeats, restart the computer.
pause
exit /b 1

:nopy
echo Python 3.7 or newer was not found (or the "py" launcher cannot start it).
echo Install it with:  winget install Python.Python.3.12
echo or from https://www.python.org/downloads/  - then close ALL windows
echo and double-click this file again. If Python asks a question, answer it.
pause
exit /b 1

:nocf
echo Could not download cloudflared. Check your internet connection, or download
echo cloudflared-windows-amd64.exe from https://github.com/cloudflare/cloudflared/releases
echo and save it next to this file as cloudflared.exe
pause
exit /b 1

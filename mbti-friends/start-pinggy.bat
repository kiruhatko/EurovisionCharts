@echo off
rem Dzerkalo MBTI: temporary public link via Pinggy over port 443 (uses the SSH client built into Windows 10/11).
rem Free Pinggy tunnels last 60 minutes; this file reconnects and prints a NEW link each time.
setlocal
title Dzerkalo MBTI (pinggy)
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
where ssh >nul 2>nul
if errorlevel 1 goto nossh
rem Pinggy may ask for a password on every connect: a throwaway key avoids the prompt on reconnects.
set "KEY=%LOCALAPPDATA%\dzerkalo-pinggy-key"
if not exist "%KEY%" ssh-keygen -q -t ed25519 -N "" -f "%KEY%" >nul 2>nul
set "IDOPT="
if exist "%KEY%" set IDOPT=-i "%KEY%" -o IdentitiesOnly=yes

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
echo  1. Wait for the https:// link that Pinggy prints below.
echo     (If it asks for a password, just press Enter.)
echo  2. OPEN THAT https LINK YOURSELF and create the room THERE,
echo     so the invite link you copy is the public one.
echo     (If a Pinggy page appears first, press its button once.)
echo  3. Send friends the invite link (or the link + the room code).
echo  If you see TUNNEL DROPPED: friends who have NOT joined yet need
echo     the NEW link and the SAME room code. Do NOT refresh your own
echo     page - it keeps working.
echo  A small window "Dzerkalo server" also runs: do not close it.
echo  To stop everything, close BOTH windows.
echo  If the window title starts with "Select", press Esc or Enter:
echo     clicking in this window pauses it.
echo ================================================================

set /a FAILS=0
:again
curl.exe -s -f -o nul --max-time 2 http://127.0.0.1:8765/index.html
if errorlevel 1 start "Dzerkalo server" /min /d "%TEMP%" %PY% -m http.server 8765 --bind 127.0.0.1 --directory "%WWW%"
echo.
echo [%time%] Connecting...
ssh -p 443 %IDOPT% -o StrictHostKeyChecking=no -o UserKnownHostsFile=NUL -o ServerAliveInterval=20 -o ServerAliveCountMax=6 -o ConnectTimeout=20 -R0:127.0.0.1:8765 free.pinggy.io
set "RC=%errorlevel%"
set /a FAILS+=1
echo.
echo ****************************************************************
echo  TUNNEL DROPPED or NOT CONNECTED (ssh exit code %RC%).
echo  Any old link no longer works. Reconnecting in 5 seconds:
echo  a NEW link will appear BELOW (the room code stays the same).
echo  Rooms keep working for people who already opened the page.
echo ****************************************************************
if %FAILS% geq 3 echo  If you NEVER saw an https link, this method may be blocked on your network:
if %FAILS% geq 3 echo  close this window and run start-ssh.bat instead.
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

:nossh
echo The SSH client was not found. In Windows Settings open Apps - Optional features
echo and add "OpenSSH Client", then run this file again.
pause
exit /b 1

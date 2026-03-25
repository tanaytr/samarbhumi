@echo off
title Samarbhumi - War Never Ends
color 0A

echo.
echo  ================================================
echo   SAMARBHUMI - War Never Ends
echo   Java 2D Action Shooter  v1.2
echo  ================================================
echo.

:: ── Step 1: Check / auto-install Java ──────────────────────────────────
set "JAVA_EXE="
set "JAVAC_EXE="

:: Check if java/javac already on PATH
where java  >nul 2>&1 && set "JAVA_EXE=java"
where javac >nul 2>&1 && set "JAVAC_EXE=javac"

:: Also check common install locations
if not defined JAVA_EXE (
    for %%D in (
        "%ProgramFiles%\Eclipse Adoptium"
        "%ProgramFiles%\Java"
        "%ProgramFiles%\Microsoft"
        "%ProgramFiles(x86)%\Java"
        "%LOCALAPPDATA%\Programs\Eclipse Adoptium"
        "%APPDATA%\Local\Programs\Eclipse Adoptium"
    ) do (
        if exist "%%~D" (
            for /d %%J in ("%%~D\jdk*" "%%~D\jre*") do (
                if exist "%%~J\bin\java.exe"  set "JAVA_EXE=%%~J\bin\java.exe"
                if exist "%%~J\bin\javac.exe" set "JAVAC_EXE=%%~J\bin\javac.exe"
            )
        )
    )
)

:: Check portable JDK bundled with game (jdk\ folder)
if exist "%~dp0jdk\bin\java.exe"  set "JAVA_EXE=%~dp0jdk\bin\java.exe"
if exist "%~dp0jdk\bin\javac.exe" set "JAVAC_EXE=%~dp0jdk\bin\javac.exe"

:: If still not found, offer to auto-download
if not defined JAVAC_EXE (
    echo  [!] Java JDK not found on this system.
    echo.
    echo  Samarbhumi needs Java 17+.
    echo  Options:
    echo    A - Auto-download portable Java 21 ^(~185 MB^) - No install needed
    echo    B - Open download page in browser, install manually
    echo    C - Exit
    echo.
    set /p "CHOICE= Enter A, B, or C: "
    echo.

    if /i "!CHOICE!"=="A" goto :autodownload
    if /i "!CHOICE!"=="B" (
        start "" "https://adoptium.net/temurin/releases/?version=21&os=windows&arch=x64&package=jdk"
        echo  Browser opened. Install JDK 21, then run this bat again.
        pause
        exit /b 0
    )
    exit /b 0
)

goto :compile

:autodownload
setlocal enabledelayedexpansion
echo  Downloading portable Java 21 JDK...
echo  ^(This only happens once - saved in jdk\ folder next to game^)
echo.
mkdir "%~dp0jdk" 2>nul
set "JDK_URL=https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.5+11/OpenJDK21U-jdk_x64_windows_hotspot_21.0.5_11.zip"
set "JDK_ZIP=%~dp0jdk\jdk21.zip"
powershell -Command "& {$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri '!JDK_URL!' -OutFile '!JDK_ZIP!'}"
if not exist "!JDK_ZIP!" (
    echo  [ERROR] Download failed. Check your internet connection.
    echo  Try option B to download manually.
    pause
    exit /b 1
)
echo  Extracting...
powershell -Command "Expand-Archive -Path '!JDK_ZIP!' -DestinationPath '%~dp0jdk_extract' -Force"
:: Move contents up one level (zip has a sub-folder)
for /d %%D in ("%~dp0jdk_extract\*") do (
    xcopy "%%D\*" "%~dp0jdk\" /E /I /Q >nul
)
rmdir /s /q "%~dp0jdk_extract" 2>nul
del "!JDK_ZIP!" 2>nul
set "JAVA_EXE=%~dp0jdk\bin\java.exe"
set "JAVAC_EXE=%~dp0jdk\bin\javac.exe"
endlocal & set "JAVA_EXE=%~dp0jdk\bin\java.exe" & set "JAVAC_EXE=%~dp0jdk\bin\javac.exe"
echo  Java installed successfully!
echo.

:compile
:: ── Step 2: Compile ──────────────────────────────────────────────────────
if not exist "%~dp0bin" mkdir "%~dp0bin"
if not exist "%~dp0saves" mkdir "%~dp0saves"

echo  [1/3] Compiling...
"%JAVAC_EXE%" -encoding UTF-8 -d "%~dp0bin" ^
  "%~dp0src\com\samarbhumi\exception\GameException.java" ^
  "%~dp0src\com\samarbhumi\exception\SaveException.java" ^
  "%~dp0src\com\samarbhumi\core\Vec2.java" ^
  "%~dp0src\com\samarbhumi\core\AABB.java" ^
  "%~dp0src\com\samarbhumi\core\GameConstants.java" ^
  "%~dp0src\com\samarbhumi\core\Enums.java" ^
  "%~dp0src\com\samarbhumi\core\InputState.java" ^
  "%~dp0src\com\samarbhumi\physics\PhysicsBody.java" ^
  "%~dp0src\com\samarbhumi\map\GameMap.java" ^
  "%~dp0src\com\samarbhumi\map\MapFactory.java" ^
  "%~dp0src\com\samarbhumi\weapon\Weapon.java" ^
  "%~dp0src\com\samarbhumi\weapon\Projectile.java" ^
  "%~dp0src\com\samarbhumi\weapon\ParticleSystem.java" ^
  "%~dp0src\com\samarbhumi\entity\Player.java" ^
  "%~dp0src\com\samarbhumi\entity\PickupItem.java" ^
  "%~dp0src\com\samarbhumi\ai\BotController.java" ^
  "%~dp0src\com\samarbhumi\progression\PlayerProfile.java" ^
  "%~dp0src\com\samarbhumi\core\GameSession.java" ^
  "%~dp0src\com\samarbhumi\audio\AudioEngine.java" ^
  "%~dp0src\com\samarbhumi\ui\PlayerRenderer.java" ^
  "%~dp0src\com\samarbhumi\ui\MapRenderer.java" ^
  "%~dp0src\com\samarbhumi\ui\HUDRenderer.java" ^
  "%~dp0src\com\samarbhumi\ui\UIRenderer.java" ^
  "%~dp0src\com\samarbhumi\ui\GameScreen.java" ^
  "%~dp0src\com\samarbhumi\ui\Screens.java" ^
  "%~dp0src\com\samarbhumi\ui\GameWindow.java" ^
  "%~dp0src\com\samarbhumi\net\NetManager.java" ^
  "%~dp0src\com\samarbhumi\net\NetworkSession.java" ^
  "%~dp0src\Main.java"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo  [ERROR] Compilation failed. See messages above.
    echo  If you see "cannot find symbol" errors, make sure all .java files are present.
    pause
    exit /b 1
)

:: ── Step 3: Copy resources ───────────────────────────────────────────────
echo  [2/3] Copying resources...
if exist "%~dp0resources\fonts\GameFont-Bold.ttf"    copy /Y "%~dp0resources\fonts\GameFont-Bold.ttf"    "%~dp0bin\" >nul
if exist "%~dp0resources\fonts\GameFont-Regular.ttf" copy /Y "%~dp0resources\fonts\GameFont-Regular.ttf" "%~dp0bin\" >nul

:: ── Step 4: Launch ───────────────────────────────────────────────────────
echo  [3/3] Launching Samarbhumi...
echo.
"%JAVA_EXE%" -Xmx512m -Xms128m ^
             -Dsun.java2d.opengl=false ^
             -Dsun.java2d.d3d=false ^
             -Dsun.java2d.ddoffscreen=false ^
             -Dfile.encoding=UTF-8 ^
             -cp "%~dp0bin" Main

pause
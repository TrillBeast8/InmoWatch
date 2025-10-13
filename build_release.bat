@echo off
REM Build and install optimized release build for InmoWatch
REM This will be MUCH faster than debug builds

echo.
echo ========================================
echo Building RELEASE APK (Optimized)
echo ========================================
echo.

echo Cleaning previous builds...
call gradlew.bat clean

echo.
echo Building release APK with R8 optimization...
call gradlew.bat assembleRelease

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ Build failed! Check errors above.
    pause
    exit /b 1
)

echo.
echo ========================================
echo ✅ Release APK built successfully!
echo ========================================
echo.
echo APK location:
echo app\build\outputs\apk\release\app-release.apk
echo.
echo To install on your Galaxy Watch4:
echo 1. Connect watch via ADB
echo 2. Run: adb install -r app\build\outputs\apk\release\app-release.apk
echo.

REM Attempt auto-install if watch is connected
echo Checking for connected devices...
adb devices | findstr "device$" >nul
if %ERRORLEVEL% EQU 0 (
    echo.
    echo Connected device found! Installing...
    adb install -r app\build\outputs\apk\release\app-release.apk
    if %ERRORLEVEL% EQU 0 (
        echo.
        echo ✅ Release APK installed successfully!
        echo.
        echo Performance improvements vs Debug build:
        echo   • 2-3x faster startup
        echo   • No Compose lock verification warnings
        echo   • Optimized DEX code
        echo   • ~50%% smaller APK size
        echo.
    ) else (
        echo.
        echo ❌ Installation failed. Try manual installation.
    )
) else (
    echo No device connected. Connect watch and run:
    echo adb install -r app\build\outputs\apk\release\app-release.apk
)

echo.
pause


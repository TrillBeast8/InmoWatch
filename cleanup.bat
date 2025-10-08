@echo off
echo Cleaning InmoWatch project...

REM Remove all build directories
if exist "app\build" (
    echo Removing app\build directory...
    rmdir /s /q "app\build"
)

if exist "build" (
    echo Removing root build directory...
    rmdir /s /q "build"
)

REM Remove gradle cache directories
if exist ".gradle" (
    echo Removing .gradle directory...
    rmdir /s /q ".gradle"
)

echo Cleanup complete!
echo You can now run: gradlew build
pause

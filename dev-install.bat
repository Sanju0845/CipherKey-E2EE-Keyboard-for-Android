@echo off
echo [CipherKey] Building and installing...
gradlew.bat installDebug --offline --daemon
if %ERRORLEVEL% EQU 0 (
    echo [CipherKey] Installed successfully!
) else (
    echo [CipherKey] Build failed. Trying with network...
    gradlew.bat installDebug --daemon
)

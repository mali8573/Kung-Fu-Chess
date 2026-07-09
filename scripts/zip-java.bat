@echo off
REM Wrapper to run the PowerShell zip script from Windows cmd
SETLOCAL ENABLEDELAYEDEXPANSION
set SCRIPT_DIR=%~dp0
powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%zip-java.ps1" -SourceDir "%SCRIPT_DIR%.." -OutDir "%SCRIPT_DIR%..\dist"
ENDLOCAL

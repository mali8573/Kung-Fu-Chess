@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion
set JAVA_HOME=C:\Program Files\Android\jdk\jdk-8.0.302.8-hotspot\jdk8u302-b08
set PATH=%JAVA_HOME%\bin;%PATH%
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8

rem This script never builds target\classes itself - only run-server.bat does. That's
rem deliberate: this script is meant to be launched more than once at the same time (once per
rem player), and target\classes is shared - if this script also rebuilt it, two windows started
rem close together (or this window plus a run-server.bat still mid-rebuild) would race each
rem other's rmdir/recompile, and whichever one lost the race would fail with something like
rem "Could not find or load main class NetworkApp" or NoClassDefFoundError from a half-rebuilt
rem folder. Requiring run-server.bat to have already built it avoids that class of bug entirely.
if not exist target\classes\NetworkApp.class (
  echo.
  echo target\classes isn't built yet - run run-server.bat first ^(it builds the project
  echo and starts the server this window connects to^), then try this again.
  echo.
  pause
  exit /b 1
)

echo.
echo Connecting to localhost:8765 - make sure run-server.bat is already running in another window.
echo.
java -cp "target\classes;lib\*" NetworkApp localhost 8765
pause

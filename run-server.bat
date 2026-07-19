@echo off
setlocal EnableDelayedExpansion
set JAVA_HOME=C:\Program Files\Android\jdk\jdk-8.0.302.8-hotspot\jdk8u302-b08
set PATH=%JAVA_HOME%\bin;%PATH%
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8

if exist target\classes rmdir /s /q target\classes
mkdir target\classes

set SRC_FILES=
for /r src %%f in (*.java) do (
  echo %%f | findstr /i /c:"\src\test\" >nul
  if errorlevel 1 (
    set SRC_FILES=!SRC_FILES! "%%f"
  )
)

javac -encoding UTF-8 -cp "lib/*" -d target/classes %SRC_FILES%
if errorlevel 1 (
  echo.
  echo Build failed - see the errors above.
  pause
  exit /b 1
)

echo.
echo Starting the game server on port 8765. Keep this window open while playing.
echo Run run-network-app.bat (twice, once per player) in two other windows to connect.
echo.
java -cp "target\classes;lib\*" server.GameServer 8765
pause

@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion
set JAVA_HOME=C:\Program Files\Android\jdk\jdk-8.0.302.8-hotspot\jdk8u302-b08
set PATH=%JAVA_HOME%\bin;%PATH%
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8

if exist target\classes rmdir /s /q target\classes
if exist target\test-classes rmdir /s /q target\test-classes
mkdir target\classes
mkdir target\test-classes

if exist target\sources.txt del target\sources.txt
for /r src %%f in (*.java) do (
  echo %%f | findstr /i /c:"\src\test\" >nul
  if errorlevel 1 (
    set "srcpath=%%f"
    set "srcpath=!srcpath:\=/!"
    echo "!srcpath!">>target\sources.txt
  )
)

javac -cp "lib/*" -d target/classes @target\sources.txt
if errorlevel 1 exit /b %errorlevel%

if exist target\test-sources.txt del target\test-sources.txt
for /r src\test\java %%f in (*.java) do (
  set "srcpath=%%f"
  set "srcpath=!srcpath:\=/!"
  echo "!srcpath!">>target\test-sources.txt
)

javac -cp "target/classes;lib/*" -d target/test-classes @target\test-sources.txt
if errorlevel 1 exit /b %errorlevel%

java -cp "target/classes;target/test-classes;lib/*" org.junit.platform.console.ConsoleLauncher --scan-class-path

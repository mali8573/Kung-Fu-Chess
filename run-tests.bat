@echo off
setlocal EnableDelayedExpansion
set JAVA_HOME=C:\Program Files\Android\jdk\jdk-8.0.302.8-hotspot\jdk8u302-b08
set PATH=%JAVA_HOME%\bin;%PATH%
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8

set SRC_FILES=
for /r src %%f in (*.java) do (
  set SRC_FILES=!SRC_FILES! "%%f"
)

javac -cp "lib/junit-jupiter-api-5.9.2.jar;lib/junit-jupiter-engine-5.9.2.jar;lib/junit-platform-console-standalone-1.9.2.jar;lib/opentest4j-1.2.0.jar" -d target/classes %SRC_FILES%
if errorlevel 1 exit /b %errorlevel%

set TEST_FILES=
for /r src\test\java %%f in (*.java) do (
  set TEST_FILES=!TEST_FILES! "%%f"
)

javac -cp "target/classes;lib/junit-jupiter-api-5.9.2.jar;lib/junit-jupiter-engine-5.9.2.jar;lib/junit-platform-console-standalone-1.9.2.jar;lib/opentest4j-1.2.0.jar" -d target/test-classes %TEST_FILES%
if errorlevel 1 exit /b %errorlevel%

java -jar "lib/junit-platform-console-standalone-1.9.2.jar" --class-path "target/classes;target/test-classes;lib/junit-jupiter-api-5.9.2.jar;lib/junit-jupiter-engine-5.9.2.jar;lib/opentest4j-1.2.0.jar" --scan-class-path

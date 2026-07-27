@ECHO OFF
SETLOCAL
SET MVNW_VERSION=3.9.16
IF NOT "%MVNW_VERSION_OVERRIDE%"=="" SET MVNW_VERSION=%MVNW_VERSION_OVERRIDE%
SET MVNW_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MVNW_VERSION%
SET MVNW_BIN=%MVNW_HOME%\apache-maven-%MVNW_VERSION%\bin\mvn.cmd
IF EXIST "%MVNW_BIN%" GOTO run

IF NOT EXIST "%MVNW_HOME%" MKDIR "%MVNW_HOME%"
SET ARCHIVE=%MVNW_HOME%\apache-maven-%MVNW_VERSION%-bin.zip
SET URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MVNW_VERSION%/apache-maven-%MVNW_VERSION%-bin.zip
powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing '%URL%' -OutFile '%ARCHIVE%'"
IF ERRORLEVEL 1 EXIT /B 1
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%ARCHIVE%' '%MVNW_HOME%'"
IF ERRORLEVEL 1 EXIT /B 1
DEL "%ARCHIVE%"

:run
CALL "%MVNW_BIN%" %*
ENDLOCAL

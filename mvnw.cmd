@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM Begin all REM://maven.apache.org/download.cgi
@REM and target/maven-wrapper.jar already exists for this project.

@IF "%DEBUG%"=="" @ECHO OFF
@REM Set local scope for the variables with windows NT shell
IF "%OS%"=="Windows_NT" SETLOCAL

SET MAVEN_PROJECTBASEDIR=%~dp0

@REM Find the project base dir, i.e. the directory that contains the folder ".mvn".
@REM Fallback to current working directory if not found.

SET MAVEN_BASEDIR=
SET "WDIR=%CD%"
:findBaseDir
IF EXIST "%WDIR%"\.mvn GOTO baseDirFound
cd ..
IF "%WDIR%"=="%CD%" GOTO baseDirNotFound
SET "WDIR=%CD%"
GOTO findBaseDir

:baseDirFound
SET MAVEN_BASEDIR=%WDIR%
cd "%MAVEN_PROJECTBASEDIR%"
GOTO endDetectBaseDir

:baseDirNotFound
SET MAVEN_BASEDIR=%MAVEN_PROJECTBASEDIR%
cd "%MAVEN_PROJECTBASEDIR%"

:endDetectBaseDir

IF NOT EXIST "%MAVEN_BASEDIR%\.mvn\wrapper\maven-wrapper.jar" (
    SET "WRAPPER_JAR_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
    bitsadmin /transfer wrapper "%WRAPPER_JAR_URL%" "%MAVEN_BASEDIR%\.mvn\wrapper\maven-wrapper.jar" >NUL 2>&1
    IF NOT EXIST "%MAVEN_BASEDIR%\.mvn\wrapper\maven-wrapper.jar" (
        powershell -Command "Invoke-WebRequest -Uri '%WRAPPER_JAR_URL%' -OutFile '%MAVEN_BASEDIR%\.mvn\wrapper\maven-wrapper.jar'"
    )
)

SET WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

@REM Extension to allow automatically downloading the maven-wrapper.jar from Maven-central
@REM This allows using the maven wrapper in projects that prohibit checking in binary data.

"%JAVA_HOME%\bin\java.exe" ^
  %MAVEN_OPTS% ^
  %MAVEN_DEBUG_OPTS% ^
  -classpath "%MAVEN_BASEDIR%\.mvn\wrapper\maven-wrapper.jar" ^
  "-Dmaven.multiModuleProjectDirectory=%MAVEN_BASEDIR%" ^
  %WRAPPER_LAUNCHER% %*

IF ERRORLEVEL 1 GOTO error
GOTO end

:error
SET ERROR_CODE=1

:end
@ENDLOCAL & SET ERROR_CODE=%ERROR_CODE%

CMD /C EXIT /B %ERROR_CODE%

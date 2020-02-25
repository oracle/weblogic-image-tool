@echo off
@rem **************************************************************************
@rem imagetool.cmd
@rem
@rem Copyright (c) 2019, 2020, Oracle and/or its affiliates.
@rem Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

IF "%JAVA_HOME%" == "" (
  ECHO JAVA_HOME not set. Please set JAVA_HOME to jdk home. OpenJDK is not supported.
  EXIT /B 2
)
FOR %%i IN ("%JAVA_HOME%") DO SET JAVA_HOME=%%~fsi
IF %JAVA_HOME:~-1%==\ SET JAVA_HOME=%JAVA_HOME:~0,-1%
IF EXIST %JAVA_HOME%\bin\java.exe (
  FOR %%i IN ("%JAVA_HOME%\bin\java.exe") DO SET JAVA_EXE=%%~fsi
) ELSE (
  ECHO Java executable does not exist at %JAVA_HOME%\bin\java.exe does not exist >&2
  EXIT /B 2
)
FOR /F %%i IN ('%JAVA_EXE% -version 2^>^&1') DO (
  IF "%%i" == "OpenJDK" (
    ECHO JAVA_HOME %JAVA_HOME% contains OpenJDK^, which is not supported >&2
    EXIT /B 2
  )
)
SET IMAGETOOL_HOME=%~dp0%/..
%JAVA_HOME%\bin\java -cp %IMAGETOOL_HOME%\lib\* -Djava.util.logging.config.file=%IMAGETOOL_HOME%\bin\logging.properties com.oracle.weblogic.imagetool.cli.ImageTool %*
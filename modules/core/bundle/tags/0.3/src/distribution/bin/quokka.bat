@echo off
if "%OS%"=="Windows_NT" setlocal
@rem =========================================================================
@rem This script is designed to launch multiple versions of quokka via
@rem quokka's bootstrapping mechanism.
@rem Edit the variables below to match your most commonly used configuration

set _DEFAULT_QUOKKA=0.3
set _DEFAULT_ANT=1.7.1
set _DEFAULT_OPTS=-Xmx1024m
@rem set JAVACMD=</path/to your most commonly used jdk/java>

@rem =========================================================================

set ERROR_CODE=0

@rem === Set QUOKKA_HOME
if "%OS%"=="Windows_NT" goto nt
if "%OS%"=="WINNT" goto nt
if not "%QUOKKA_HOME%"=="" goto homeSet
echo QUOKKA_HOME must be set for Win9x environments
goto error

:nt
set _NT=true
@rem %~dp0 is expanded pathname of the current script under NT
@rem the for loop evaulates it, effectively removing the ..
for %%i in ("%~dp0..") do set QUOKKA_HOME=%%~fi

:homeSet
set _LAUNCHER_CP=%QUOKKA_HOME%\lib\apache.ant_%_DEFAULT_ANT%_launcher_jar.jar
set _QUOKKA_CP=%QUOKKA_HOME%\lib\apache.ant_%_DEFAULT_ANT%_ant_jar.jar
set _QUOKKA_CP=%_QUOKKA_CP%;%QUOKKA_HOME%\lib\quokka.bundle_%_DEFAULT_QUOKKA%_core_jar.jar

@rem TODO: Get rid of this dummy value - needs some value or the escaping fails.
@rem       A trailing space works, but is too easily lost
set _QUOKKA_OPTS=-Ddummy-opts=x
if not "%QUOKKA_OPTS%" == "" set _QUOKKA_OPTS=%QUOKKA_OPTS%
if not "%_DEFAULT_OPTS%" == "" set _QUOKKA_OPTS=%_DEFAULT_OPTS%
set _QUOKKA_ESC_OPTS=%_QUOKKA_OPTS:"=@quot@%

@rem === Get the command line arguments
set _QUOKKA_CMD_LINE_ARGS=
:getArg
if %1a==a goto argsSet
set _QUOKKA_CMD_LINE_ARGS=%_QUOKKA_CMD_LINE_ARGS% %1
shift
goto getArg

:argsSet
if not "%_JAVACMD%" == "" goto runQuokka

@rem === Look for a default java
set _JAVACMD=%JAVACMD%

if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=%JAVA_HOME%\bin\java.exe
goto runQuokka

:noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=java.exe

:runQuokka
set _QUOKKA_STANDARD_OPTS=-Dorg.apache.tools.ant.ProjectHelper=ws.quokka.core.main.ant.ProjectHelper "-Dant.library.dir=%QUOKKA_HOME%\antlib" "-Dquokka.bootstrap.jvmArgs=%_QUOKKA_ESC_OPTS%"
set _QUOKKA_STANDARD_ARGS=-logger org.apache.tools.ant.NoBannerLogger -main ws.quokka.core.main.ant.QuokkaMain -nouserlib
"%_JAVACMD%" %_QUOKKA_STANDARD_OPTS% %QUOKKA_OPTS% -classpath "%_LAUNCHER_CP%" "-Dant.home=%QUOKKA_HOME%" org.apache.tools.ant.launch.Launcher %_QUOKKA_STANDARD_ARGS% %QUOKKA_ARGS% -cp "%_QUOKKA_CP%" %_QUOKKA_CMD_LINE_ARGS%

if ERRORLEVEL 1 goto error
goto end

:error
if "%_NT%"=="true" endlocal
set ERROR_CODE=1

:end
@REM For old DOS remove the set variables from ENV - we assume they were not set
@REM before we started - at least we don't leave any baggage around
set _JAVACMD=
set _QUOKKA_CMD_LINE_ARGS=
set _LAUNCHER_CP=
set _QUOKKA_CP=
set _QUOKKA_STANDARD_OPTS=
set _QUOKKA_STANDARD_ARGS=
set _MAX_MEMORY=
set _QUOKKA_ESC_OPTS=
if "%_NT%"=="true" endlocal

exit /B %ERROR_CODE%

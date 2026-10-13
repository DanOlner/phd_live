@echo off

REM Jar file to execute
set JAR_FILE=lwc.jar

REM File name or path -> fill here (full path, with quotes) -> example :
REM set LATEX_FILE="C:\Documents and Settings\max.lekeux\My documents\file.tex"
set LATEX_FILE=

REM Load jar into JVM
if defined LATEX_FILE (
	start javaw -jar %JAR_FILE% %LATEX_FILE%
) else (
	start javaw -jar %JAR_FILE%
)

REM clean
set JAR_FILE=
set LATEX_FILE=
echo on

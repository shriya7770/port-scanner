@echo off
REM Compiles all Java files in src\ into the out\ folder.
if not exist out mkdir out
javac -d out src\*.java
if %errorlevel% neq 0 (
    echo.
    echo Compilation FAILED. Read the error messages above.
) else (
    echo.
    echo Compilation successful. Class files are in the out folder.
)

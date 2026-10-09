@echo off
REM Starts the port scanner.
REM Interactive:  run_scanner.bat
REM Direct:       run_scanner.bat 127.0.0.1 8075 8085 2000
java -cp out Main %*

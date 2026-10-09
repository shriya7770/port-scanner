@echo off
REM Starts the test server (open ports 8080 and 9090 on 127.0.0.1).
REM To use other ports:  run_server.bat 7000 7001
java -cp out TestServer %*

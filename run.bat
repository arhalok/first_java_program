@echo off
echo Set SOLAR_DB_PASSWORD to your MySQL root password before running.
set /p SOLAR_DB_PASSWORD=MySQL root password: 
set SOLAR_DB_USER=root
mvn clean package
if errorlevel 1 pause & exit /b 1
mvn exec:java -Dexec.mainClass=com.piet.solar.App
pause

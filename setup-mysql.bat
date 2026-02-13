@echo off
REM Windows Setup Script for Money Transfer System MySQL Configuration
REM This script helps set up the MySQL database

setlocal enabledelayedexpansion

echo.
echo Money Transfer System - MySQL Setup
echo ====================================
echo.

REM Check if MySQL is installed
where mysql >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: MySQL is not installed or not in PATH.
    echo Please install MySQL Server and add it to your system PATH.
    pause
    exit /b 1
)

echo MySQL found in system PATH.
echo.

echo Setup Instructions:
echo ===================
echo.
echo 1. Open MySQL Command Line Client or MySQL Workbench
echo.
echo 2. Execute the following SQL commands:
echo.
echo    CREATE DATABASE IF NOT EXISTS money_transfer_system;
echo.
echo    CREATE USER IF NOT EXISTS 'mts_user'^@'localhost' IDENTIFIED BY 'mts_password';
echo    GRANT ALL PRIVILEGES ON money_transfer_system.* TO 'mts_user'^@'localhost';
echo    FLUSH PRIVILEGES;
echo.
echo 3. Verify the database was created:
echo    USE money_transfer_system;
echo    SHOW TABLES;
echo.
echo 4. Update src/main/resources/application.yml with credentials:
echo    spring:
echo      datasource:
echo        url: jdbc:mysql://localhost:3306/money_transfer_system
echo        driver-class-name: com.mysql.cj.jdbc.Driver
echo        username: root
echo        password: ^(your mysql password^)
echo.
echo 5. Run: mvn clean package -DskipTests
echo 6. Run: java -jar target/mts-0.0.1-SNAPSHOT.jar
echo.

REM Option to run MySQL directly
set /p CONTINUE="Do you want to connect to MySQL now? (y/n): "
if /i "%CONTINUE%"=="y" (
    echo.
    echo Connecting to MySQL...
    mysql -u root -p
) else (
    echo.
    echo Setup script completed. Please follow the instructions above.
)

pause

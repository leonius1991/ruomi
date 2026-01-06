@echo off
echo Starting vfinke.fi Application...
echo.

REM Check if Java is installed
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 17 or later
    pause
    exit /b 1
)

REM Check if Maven is installed
mvn -version >nul 2>&1
if errorlevel 1 (
    echo WARNING: Maven is not installed, using Maven wrapper...
    if exist mvnw.cmd (
        echo Using Maven wrapper...
        call mvnw.cmd clean compile
        if errorlevel 1 (
            echo ERROR: Failed to compile project
            pause
            exit /b 1
        )
        echo.
        echo Starting application...
        call mvnw.cmd spring-boot:run
    ) else (
        echo ERROR: Maven wrapper not found
        echo Please install Maven or ensure mvnw.cmd exists
        pause
        exit /b 1
    )
) else (
    echo Using installed Maven...
    mvn clean compile
    if errorlevel 1 (
        echo ERROR: Failed to compile project
        pause
        exit /b 1
    )
    echo.
    echo Starting application...
    mvn spring-boot:run
)

pause




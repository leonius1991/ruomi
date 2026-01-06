@echo off
echo ========================================
echo    NewDoska - Доска объявлений
echo ========================================
echo.

echo Проверка Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo ОШИБКА: Java не установлена или не найдена в PATH
    echo Пожалуйста, установите Java 21 или выше
    pause
    exit /b 1
)

echo Проверка Maven...
mvn -version >nul 2>&1
if errorlevel 1 (
    echo ОШИБКА: Maven не установлен или не найден в PATH
    echo Пожалуйста, установите Maven 3.6 или выше
    pause
    exit /b 1
)

echo Проверка MySQL...
mysql --version >nul 2>&1
if errorlevel 1 (
    echo ПРЕДУПРЕЖДЕНИЕ: MySQL не найден в PATH
    echo Убедитесь, что MySQL запущен и доступен
    echo.
)

echo.
echo Запуск NewDoska...
echo.

echo Компиляция проекта...
mvn clean compile

if errorlevel 1 (
    echo ОШИБКА: Ошибка компиляции
    pause
    exit /b 1
)

echo.
echo Запуск приложения...
echo Приложение будет доступно по адресу: http://localhost:8080
echo Для остановки нажмите Ctrl+C
echo.

mvn spring-boot:run

echo.
echo Приложение остановлено
pause 
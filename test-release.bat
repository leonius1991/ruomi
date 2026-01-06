@echo off
setlocal

REM ========================================
REM    Тест GitHub CLI (без сборки проекта)
REM ========================================

echo ========================================
echo    Тест GitHub CLI
echo ========================================
echo.

REM [1] Проверяем GitHub CLI
echo [1] Проверка GitHub CLI...
gh --version
if errorlevel 1 (
    echo [ERROR] GitHub CLI не установлен
    pause
    exit /b 1
)
echo [OK] GitHub CLI найден
echo.

REM [2] Проверяем авторизацию
echo [2] Проверка авторизации...
gh auth status
if errorlevel 1 (
    echo [ERROR] Вы не авторизованы
    echo Выполните: gh auth login
    pause
    exit /b 1
)
echo [OK] Авторизация успешна
echo.

REM [3] Задаем репозиторий явно (без плясок)
set GITHUB_OWNER=mifedweb
set GITHUB_REPO=ruomi

echo [3] Используем настройки:
echo   Owner: %GITHUB_OWNER%
echo   Repo:  %GITHUB_REPO%
echo.

REM [4] Проверяем доступ к репозиторию
echo [4] Проверка доступа к репозиторию %GITHUB_OWNER%/%GITHUB_REPO%...
gh repo view %GITHUB_OWNER%/%GITHUB_REPO%
if errorlevel 1 (
    echo [ERROR] Не удалось получить доступ к репозиторию
    pause
    exit /b 1
)
echo [OK] Доступ к репозиторию подтвержден
echo.

REM [5] Список релизов
echo [5] Список существующих релизов:
gh release list -R %GITHUB_OWNER%/%GITHUB_REPO%
echo.

echo ========================================
echo Тест завершен успешно!
echo ========================================
pause

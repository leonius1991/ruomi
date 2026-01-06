@echo off
setlocal enabledelayedexpansion

echo ========================================
echo    Автоматическая загрузка релиза
echo ========================================
echo.

REM Включаем отображение всех команд для отладки
set DEBUG=1

REM Проверка наличия Maven
echo [DEBUG] Проверка Maven...
mvn -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Maven не установлен
    echo Установите Maven или используйте mvnw.cmd
    pause
    exit /b 1
)
echo [OK] Maven найден

REM Получаем версию из pom.xml
echo [DEBUG] Получение версии из pom.xml...
for /f "tokens=2 delims=<>" %%a in ('findstr /C:"<version>" pom.xml ^| findstr /V "parent"') do set VERSION=%%a
if "!VERSION!"=="" (
    echo [ERROR] Не удалось получить версию из pom.xml
    pause
    exit /b 1
)
echo [OK] Версия: !VERSION!

REM Запрашиваем тег релиза
set /p TAG="Введите тег релиза (например, v1.0.0 или 1.0.0): "
if "!TAG!"=="" (
    echo ОШИБКА: Тег не может быть пустым
    pause
    exit /b 1
)

REM Запрашиваем название релиза
set /p RELEASE_NAME="Введите название релиза (Enter для использования тега): "
if "!RELEASE_NAME!"=="" set RELEASE_NAME=!TAG!

REM Запрашиваем описание
set /p RELEASE_NOTES="Введите описание релиза (Enter для пропуска): "

echo.
echo ========================================
echo Сборка проекта...
echo ========================================
echo [DEBUG] Запуск: mvn clean package -DskipTests
echo [DEBUG] Это может занять несколько минут...
mvn clean package -DskipTests
set BUILD_EXIT_CODE=%ERRORLEVEL%
echo [DEBUG] Код выхода сборки: !BUILD_EXIT_CODE!
if errorlevel 1 (
    echo [ERROR] Ошибка сборки проекта (код выхода: !BUILD_EXIT_CODE!)
    echo [DEBUG] Проверьте вывод Maven выше для деталей ошибки
    pause
    exit /b 1
)
echo [OK] Сборка завершена успешно
echo [DEBUG] Продолжаем создание релиза...

REM Проверяем наличие JAR файла
set JAR_FILE=target\doska-!VERSION!.jar
echo [DEBUG] Проверка JAR файла: !JAR_FILE!
if not exist "!JAR_FILE!" (
    echo [ERROR] JAR файл не найден: !JAR_FILE!
    echo [DEBUG] Содержимое папки target:
    dir target\*.jar 2>nul
    pause
    exit /b 1
)
echo [OK] JAR файл найден: !JAR_FILE!
for %%F in ("!JAR_FILE!") do echo [DEBUG] Размер файла: %%~zF байт

echo.
echo ========================================
echo Проверка GitHub CLI...
echo ========================================

REM Проверяем наличие GitHub CLI
echo [DEBUG] Проверка наличия GitHub CLI...
gh --version >nul 2>&1
if errorlevel 1 (
    echo [WARNING] GitHub CLI не установлен. Используем curl...
    goto :USE_CURL
)

echo [OK] GitHub CLI найден!
gh --version
echo.

REM Проверяем авторизацию
echo [DEBUG] Проверка авторизации в GitHub CLI...
gh auth status
set AUTH_EXIT_CODE=%ERRORLEVEL%
if errorlevel 1 (
    echo [ERROR] Вы не авторизованы в GitHub CLI (код выхода: !AUTH_EXIT_CODE!)
    echo Выполните: gh auth login
    pause
    exit /b 1
)
echo [OK] Авторизация успешна

REM Читаем настройки из application.properties для проверки
echo [DEBUG] Чтение настроек из application.properties...
for /f "tokens=2 delims==" %%a in ('findstr /C:"app.update.github.owner" src\main\resources\application.properties 2^>nul') do set GITHUB_OWNER=%%a
for /f "tokens=2 delims==" %%a in ('findstr /C:"app.update.github.repo" src\main\resources\application.properties 2^>nul') do set GITHUB_REPO=%%a

REM Убираем пробелы из значений
set GITHUB_OWNER=!GITHUB_OWNER: =!
set GITHUB_REPO=!GITHUB_REPO: =!

if defined GITHUB_OWNER echo [DEBUG] GitHub Owner: !GITHUB_OWNER!
if defined GITHUB_REPO echo [DEBUG] GitHub Repo: !GITHUB_REPO!

REM Проверяем доступ к репозиторию
if defined GITHUB_OWNER if defined GITHUB_REPO (
    echo [DEBUG] Проверка доступа к репозиторию !GITHUB_OWNER!/!GITHUB_REPO!...
    gh repo view !GITHUB_OWNER!/!GITHUB_REPO! >nul 2>&1
    if errorlevel 1 (
        echo [WARNING] Не удалось получить доступ к репозиторию !GITHUB_OWNER!/!GITHUB_REPO!
        echo [DEBUG] Убедитесь, что:
        echo   1. Репозиторий существует
        echo   2. У вас есть права на создание релизов
        echo   3. Вы авторизованы правильным аккаунтом
        echo.
        set /p CONTINUE="Продолжить создание релиза? (y/n): "
        if /i not "!CONTINUE!"=="y" (
            echo Отменено пользователем
            pause
            exit /b 1
        )
    ) else (
        echo [OK] Доступ к репозиторию подтвержден
    )
)

echo.
echo ========================================
echo Создание релиза на GitHub...
echo ========================================

echo [DEBUG] Создание релиза с параметрами:
echo [DEBUG]   Тег: !TAG!
echo [DEBUG]   Название: !RELEASE_NAME!
echo [DEBUG]   Файл: !JAR_FILE!
echo [DEBUG]   Описание: !RELEASE_NOTES!

REM Создаем релиз
echo [DEBUG] Подготовка команды создания релиза...
if "!RELEASE_NOTES!"=="" (
    set RELEASE_NOTES_TEXT=Релиз версии !VERSION!
) else (
    set RELEASE_NOTES_TEXT=!RELEASE_NOTES!
)

echo [DEBUG] Параметры релиза:
echo   Тег: !TAG!
echo   Название: !RELEASE_NAME!
echo   Файл: !JAR_FILE!
echo   Описание: !RELEASE_NOTES_TEXT!
echo.

REM Проверяем, существует ли уже такой тег
echo [DEBUG] Проверка существования тега !TAG!...
gh release view !TAG! >nul 2>&1
if not errorlevel 1 (
    echo [WARNING] Релиз с тегом !TAG! уже существует!
    set /p OVERWRITE="Удалить существующий релиз и создать новый? (y/n): "
    if /i "!OVERWRITE!"=="y" (
        echo [DEBUG] Удаление существующего релиза...
        gh release delete !TAG! --yes 2>nul
        if errorlevel 1 (
            echo [WARNING] Не удалось удалить существующий релиз, попробуем создать новый
        )
    ) else (
        echo Отменено пользователем
        pause
        exit /b 1
    )
)

echo [DEBUG] Выполнение команды создания релиза...
if "!RELEASE_NOTES!"=="" (
    gh release create !TAG! "!JAR_FILE!" --title "!RELEASE_NAME!" --notes "Релиз версии !VERSION!"
) else (
    gh release create !TAG! "!JAR_FILE!" --title "!RELEASE_NAME!" --notes "!RELEASE_NOTES!"
)

set RELEASE_EXIT_CODE=%ERRORLEVEL%
echo [DEBUG] Код выхода создания релиза: !RELEASE_EXIT_CODE!
if errorlevel 1 (
    echo.
    echo [ERROR] ========================================
    echo [ERROR] Не удалось создать релиз!
    echo [ERROR] ========================================
    echo [ERROR] Код выхода: !RELEASE_EXIT_CODE!
    echo.
    echo [DEBUG] Возможные причины:
    echo   1. Релиз с таким тегом уже существует
    echo   2. Нет прав на создание релизов в репозитории
    echo   3. Проблемы с авторизацией
    echo   4. Репозиторий не существует или недоступен
    echo.
    echo [DEBUG] Попробуйте выполнить команду вручную:
    echo   gh release create !TAG! "!JAR_FILE!" --title "!RELEASE_NAME!" --notes "!RELEASE_NOTES_TEXT!"
    echo.
    echo [DEBUG] Проверьте:
    echo   1. Авторизацию: gh auth status
    if defined GITHUB_OWNER if defined GITHUB_REPO (
        echo   2. Доступ к репозиторию: gh repo view !GITHUB_OWNER!/!GITHUB_REPO!
        echo   3. Существующие релизы: gh release list
    )
    echo.
    pause
    exit /b 1
)
echo [OK] Релиз создан успешно!

echo.
echo ========================================
echo Релиз успешно создан!
echo ========================================
echo Тег: !TAG!
echo Файл: !JAR_FILE!
if defined GITHUB_OWNER if defined GITHUB_REPO (
    echo Репозиторий: !GITHUB_OWNER!/!GITHUB_REPO!
    echo URL: https://github.com/!GITHUB_OWNER!/!GITHUB_REPO!/releases/tag/!TAG!
)
echo.
echo [DEBUG] Проверка созданного релиза...
gh release view !TAG! 2>nul
if errorlevel 1 (
    echo [WARNING] Не удалось просмотреть релиз, но он мог быть создан
) else (
    echo [OK] Релиз подтвержден
)
echo.
pause
exit /b 0

:USE_CURL
echo.
echo ========================================
echo Использование GitHub API через curl...
echo ========================================

REM Проверяем наличие curl
echo [DEBUG] Проверка наличия curl...
curl --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] curl не установлен
    echo Установите curl или GitHub CLI (gh)
    pause
    exit /b 1
)
echo [OK] curl найден

REM Запрашиваем GitHub токен
set /p GITHUB_TOKEN="Введите GitHub Personal Access Token: "
if "!GITHUB_TOKEN!"=="" (
    echo ОШИБКА: Токен обязателен для использования curl
    echo Создайте токен: https://github.com/settings/tokens
    pause
    exit /b 1
)

REM Читаем настройки из application.properties
set GITHUB_OWNER=
set GITHUB_REPO=
for /f "tokens=2 delims==" %%a in ('findstr /C:"app.update.github.owner" src\main\resources\application.properties') do set GITHUB_OWNER=%%a
for /f "tokens=2 delims==" %%a in ('findstr /C:"app.update.github.repo" src\main\resources\application.properties') do set GITHUB_REPO=%%a

if "!GITHUB_OWNER!"=="" (
    set /p GITHUB_OWNER="Введите GitHub username: "
)
if "!GITHUB_REPO!"=="" (
    set /p GITHUB_REPO="Введите название репозитория: "
)

if "!GITHUB_OWNER!"=="" (
    echo ОШИБКА: GitHub owner не указан
    pause
    exit /b 1
)
if "!GITHUB_REPO!"=="" (
    echo ОШИБКА: GitHub repo не указан
    pause
    exit /b 1
)

echo.
echo Создание релиза через GitHub API...

REM Создаем JSON для релиза
set RELEASE_JSON={\"tag_name\":\"!TAG!\",\"name\":\"!RELEASE_NAME!\",\"body\":\"!RELEASE_NOTES!\"}

REM Создаем релиз
curl -X POST ^
  -H "Authorization: token !GITHUB_TOKEN!" ^
  -H "Accept: application/vnd.github.v3+json" ^
  -H "Content-Type: application/json" ^
  -d "!RELEASE_JSON!" ^
  https://api.github.com/repos/!GITHUB_OWNER!/!GITHUB_REPO!/releases > release_response.json 2>nul

if errorlevel 1 (
    echo ОШИБКА: Не удалось создать релиз
    pause
    exit /b 1
)

REM Извлекаем upload_url из ответа (упрощенная версия)
for /f "tokens=*" %%a in ('type release_response.json') do set RESPONSE=%%a

REM Загружаем JAR файл
echo Загрузка JAR файла...

REM Получаем upload_url из JSON (требует jq или парсинг вручную)
REM Для упрощения используем стандартный URL
set UPLOAD_URL=https://uploads.github.com/repos/!GITHUB_OWNER!/!GITHUB_REPO!/releases/!TAG!/assets?name=doska-!VERSION!.jar

curl -X POST ^
  -H "Authorization: token !GITHUB_TOKEN!" ^
  -H "Accept: application/vnd.github.v3+json" ^
  -H "Content-Type: application/java-archive" ^
  --data-binary "@!JAR_FILE!" ^
  "!UPLOAD_URL!" >nul 2>nul

if errorlevel 1 (
    echo ПРЕДУПРЕЖДЕНИЕ: Возможно, релиз уже существует или ошибка загрузки
    echo Попробуйте использовать GitHub CLI (gh) для более надежной работы
) else (
    echo Файл успешно загружен!
)

del release_response.json 2>nul

echo.
echo ========================================
echo Релиз создан!
echo ========================================
echo Тег: !TAG!
echo Репозиторий: !GITHUB_OWNER!/!GITHUB_REPO!
echo.
pause
exit /b 0


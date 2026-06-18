#!/bin/bash

# ==============================
# ЛОГИ
# ==============================
LOG_FILE="release.log"
: > "$LOG_FILE"

exec > >(tee -a "$LOG_FILE") 2>&1
trap 'echo "ОШИБКА: скрипт аварийно завершился. См. $LOG_FILE"' ERR

set -e

echo "========================================"
echo "   Автоматическая загрузка релиза"
echo "========================================"
echo "Лог-файл: $LOG_FILE"
echo

# ==============================
# НАСТРОЙКИ
# ==============================
GITHUB_OWNER="leonius1991"
GITHUB_REPO="ruomi"

# Получаем artifactId из pom.xml
echo "[DEBUG] Получение artifactId из pom.xml..."
ARTIFACT_ID=$(mvn help:evaluate -Dexpression=project.artifactId -q -DforceStdout 2>/dev/null)

if [ -z "$ARTIFACT_ID" ]; then
    echo "[WARNING] Не удалось получить artifactId из Maven, используем 'doska'"
    ARTIFACT_ID="doska"
fi

echo "[DEBUG] Artifact ID: $ARTIFACT_ID"
# ==============================
# Проверка Maven
# ==============================
echo "[INFO] Проверка Maven"
if ! command -v mvn &> /dev/null; then
    echo "ОШИБКА: Maven не установлен"
    exit 1
fi

# ==============================
# Версия
# ==============================
echo "[INFO] Чтение версии из pom.xml"
VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

if [ -z "$VERSION" ]; then
    echo "ОШИБКА: Не удалось определить версию"
    exit 1
fi

echo "Версия проекта: $VERSION"

# ==============================
# Данные релиза
# ==============================
read -p "Введите тег релиза (например, v1.0.0): " TAG
if [ -z "$TAG" ]; then
    echo "ОШИБКА: Тег не может быть пустым"
    exit 1
fi

read -p "Введите название релиза (Enter = тег): " RELEASE_NAME
RELEASE_NAME=${RELEASE_NAME:-$TAG}

read -p "Введите описание релиза (Enter = авто): " RELEASE_NOTES
RELEASE_NOTES=${RELEASE_NOTES:-"Релиз версии $VERSION"}

# ==============================
# СБОРКА
# ==============================
echo
echo "========================================"
echo "Сборка проекта (mvn clean package)"
echo "========================================"

mvn clean package -DskipTests

# Ищем JAR файл - сначала по стандартному имени, потом ищем любой JAR в target
JAR_FILE="target/${ARTIFACT_ID}-${VERSION}.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "[WARNING] JAR файл не найден по стандартному пути: $JAR_FILE"
    echo "[DEBUG] Поиск JAR файлов в target..."
    
    # Ищем все JAR файлы в target (кроме .original)
    FOUND_JAR=$(find target -name "*.jar" ! -name "*.original" -type f | head -1)
    
    if [ -n "$FOUND_JAR" ] && [ -f "$FOUND_JAR" ]; then
        JAR_FILE="$FOUND_JAR"
        echo "[OK] Найден JAR файл: $JAR_FILE"
    else
        echo "[ERROR] JAR файл не найден в target/"
        echo "[DEBUG] Содержимое target:"
        ls -la target/ 2>/dev/null || echo "Директория target не существует"
        exit 1
    fi
fi

if [ ! -f "$JAR_FILE" ]; then
    echo "[ERROR] JAR файл не существует: $JAR_FILE"
    exit 1
fi

echo "[OK] JAR найден: $JAR_FILE"
echo "[DEBUG] Размер JAR: $(du -h "$JAR_FILE" | cut -f1)"

# ==============================
# СОЗДАНИЕ ARCHIVE РЕСУРСОВ
# ==============================
echo
echo "========================================"
echo "Создание архива ресурсов"
echo "========================================"

RESOURCES_ZIP="target/resources-${VERSION}.zip"

# Удаляем старый архив если есть
rm -f "$RESOURCES_ZIP"

# Создаем временную директорию с правильной структурой
TEMP_DIR="target/temp_resources"
rm -rf "$TEMP_DIR"
mkdir -p "$TEMP_DIR/templates" "$TEMP_DIR/static"

# Копируем файлы
if [ -d "src/main/resources/templates" ]; then
    cp -r src/main/resources/templates/* "$TEMP_DIR/templates/" 2>/dev/null || true
    echo "[OK] Шаблоны скопированы"
fi

if [ -d "src/main/resources/static" ]; then
    cp -r src/main/resources/static/* "$TEMP_DIR/static/" 2>/dev/null || true
    echo "[OK] Статические файлы скопированы"
fi

# Проверяем что есть файлы для архивации
FILE_COUNT=$(find "$TEMP_DIR" -type f | wc -l)
if [ "$FILE_COUNT" -eq 0 ]; then
    echo "[WARNING] Нет файлов для архивации в $TEMP_DIR"
    echo "[DEBUG] Содержимое временной директории:"
    ls -la "$TEMP_DIR" 2>/dev/null || echo "Директория не существует"
    rm -rf "$TEMP_DIR"
else
    echo "[DEBUG] Найдено файлов для архивации: $FILE_COUNT"
    
    # Создаем ZIP архив
    echo "[DEBUG] Создание ZIP архива..."
    
    # Сохраняем текущую директорию для всех платформ
    ORIGINAL_DIR=$(pwd)
    
    # Определяем операционную систему
    if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" || "$OSTYPE" == "win32" || -n "$WINDIR" || "$(uname -s 2>/dev/null)" == "MINGW"* ]]; then
        # Windows - пробуем сначала Java jar (надежнее), потом PowerShell
        echo "[DEBUG] Обнаружена Windows, используем Java jar для создания ZIP..."
        # Для Windows не переходим в TEMP_DIR заранее, работаем из исходной директории
        
        # Пробуем Java jar утилиту (она точно есть, так как Maven работает)
        JAR_CMD=""
        if [ -n "$JAVA_HOME" ] && [ -f "$JAVA_HOME/bin/jar" ]; then
            JAR_CMD="$JAVA_HOME/bin/jar"
        elif command -v jar &> /dev/null; then
            JAR_CMD="jar"
        fi
        
        # Инициализируем переменную для абсолютного пути ZIP (для проверки после PowerShell)
        ZIP_ABS=""
        
        if [ -n "$JAR_CMD" ]; then
            echo "[DEBUG] Используем Java jar: $JAR_CMD"
            echo "[DEBUG] Исходная директория: $ORIGINAL_DIR"
            echo "[DEBUG] Временная директория: $TEMP_DIR"
            echo "[DEBUG] Путь к ZIP: $RESOURCES_ZIP"
            cd "$TEMP_DIR" || exit 1
            echo "[DEBUG] Перешли в: $(pwd)"
            # Создаем ZIP в родительской директории
            ZIP_RELATIVE_PATH="../resources-${VERSION}.zip"
            echo "[DEBUG] Создаем ZIP: $ZIP_RELATIVE_PATH"
            # Экранируем путь к jar, если он содержит пробелы
            JAR_OUTPUT=$("$JAR_CMD" -cfM "$ZIP_RELATIVE_PATH" . 2>&1)
            JAR_EXIT_CODE=$?
            if [ $JAR_EXIT_CODE -eq 0 ]; then
                echo "[OK] ZIP архив создан через Java jar"
                echo "[DEBUG] Вывод jar: $JAR_OUTPUT"
                # Возвращаемся в исходную директорию
                cd "$ORIGINAL_DIR" || cd - >/dev/null || true
                echo "[DEBUG] Вернулись в: $(pwd)"
                # Проверяем что файл создан (по относительному пути от исходной директории)
                if [ -f "$RESOURCES_ZIP" ]; then
                    echo "[DEBUG] ZIP файл подтвержден: $RESOURCES_ZIP"
                    # Устанавливаем ZIP_ABS для последующей проверки
                    ZIP_ABS=$(powershell -NoProfile -Command "[System.IO.Path]::GetFullPath('$RESOURCES_ZIP')" 2>&1 | tail -1 || echo "$RESOURCES_ZIP")
                    echo "[DEBUG] Абсолютный путь: $ZIP_ABS"
                else
                    echo "[WARNING] ZIP файл не найден по пути $RESOURCES_ZIP после создания через jar"
                    echo "[DEBUG] Текущая директория: $(pwd)"
                    echo "[DEBUG] Содержимое target/:"
                    ls -la target/ 2>/dev/null | grep -i zip || echo "ZIP файлы не найдены"
                    echo "[DEBUG] Пробуем PowerShell..."
                    JAR_CMD=""
                fi
            else
                echo "[WARNING] Не удалось создать ZIP через jar (код выхода: $JAR_EXIT_CODE)"
                echo "[DEBUG] Вывод jar: $JAR_OUTPUT"
                cd "$ORIGINAL_DIR" || cd - >/dev/null || true
                JAR_CMD=""
            fi
        fi
        
        # Если jar не сработал, используем PowerShell
        if [ -z "$JAR_CMD" ] || [ ! -f "$RESOURCES_ZIP" ]; then
            echo "[DEBUG] Используем PowerShell для создания ZIP..."
            
            # Получаем абсолютные пути через PowerShell для правильного формата
            TEMP_ABS=$(powershell -NoProfile -Command "[System.IO.Path]::GetFullPath('$TEMP_DIR')" 2>&1 | tail -1 || echo "$TEMP_DIR")
            ZIP_ABS=$(powershell -NoProfile -Command "[System.IO.Path]::GetFullPath('$RESOURCES_ZIP')" 2>&1 | tail -1 || echo "$RESOURCES_ZIP")
            
            echo "[DEBUG] Временная директория: $TEMP_ABS"
            echo "[DEBUG] Путь к ZIP: $ZIP_ABS"
            
            # Создаем PowerShell команду напрямую (без скрипта)
            PS_CMD="\$ErrorActionPreference='Stop'; \$tempPath='$TEMP_ABS'; \$zipPath='$ZIP_ABS'; if (-not (Test-Path \$tempPath)) { Write-Host '[ERROR] Временная директория не существует'; exit 1 }; \$items = Get-ChildItem -Path \$tempPath -Recurse -File; Write-Host \"[DEBUG] Найдено файлов: \$(\$items.Count)\"; if (\$items.Count -eq 0) { Write-Host '[WARNING] Нет файлов для архивации'; exit 1 }; if (Test-Path \$zipPath) { Remove-Item \$zipPath -Force }; Compress-Archive -Path (Join-Path \$tempPath '*') -DestinationPath \$zipPath -Force; if (Test-Path \$zipPath) { \$size = (Get-Item \$zipPath).Length; Write-Host \"[OK] ZIP создан успешно, размер: \$size байт\" } else { Write-Host '[ERROR] ZIP файл не создан'; exit 1 }"
            
            PS_OUTPUT=$(powershell -NoProfile -ExecutionPolicy Bypass -Command "$PS_CMD" 2>&1)
            PS_EXIT_CODE=$?
            
            # Выводим вывод PowerShell
            echo "$PS_OUTPUT"
            
            # Проверяем существование файла по абсолютному пути (PowerShell создает по абсолютному пути)
            if [ $PS_EXIT_CODE -ne 0 ]; then
                echo "[ERROR] Не удалось создать ZIP архив через PowerShell (код выхода: $PS_EXIT_CODE)"
                rm -rf "$TEMP_DIR" 2>/dev/null || rmdir /s /q "$TEMP_DIR" 2>/dev/null
                exit 1
            fi
            
            # Проверяем файл по абсолютному пути (как создал PowerShell)
            if [ ! -f "$ZIP_ABS" ] && [ ! -f "$RESOURCES_ZIP" ]; then
                echo "[ERROR] ZIP файл не найден ни по абсолютному ($ZIP_ABS), ни по относительному ($RESOURCES_ZIP) пути"
                rm -rf "$TEMP_DIR" 2>/dev/null || rmdir /s /q "$TEMP_DIR" 2>/dev/null
                exit 1
            fi
            
            # Если файл создан по абсолютному пути, но не по относительному, проверяем что он там есть
            if [ -f "$ZIP_ABS" ] && [ ! -f "$RESOURCES_ZIP" ]; then
                echo "[DEBUG] ZIP создан по абсолютному пути, проверяем относительный..."
                # Файл должен быть там же, просто проверим еще раз
                if [ -f "$ZIP_ABS" ]; then
                    echo "[OK] ZIP файл существует по абсолютному пути: $ZIP_ABS"
                fi
            fi
        fi
    elif command -v zip &> /dev/null; then
        # Linux/Mac - используем zip
        # Переходим в TEMP_DIR для Linux/Mac
        cd "$TEMP_DIR" || exit 1
        if zip -r "../resources-${VERSION}.zip" . >/dev/null 2>&1; then
            echo "[OK] ZIP архив создан"
            cd "$ORIGINAL_DIR" || cd - >/dev/null || true
        else
            echo "[ERROR] Ошибка при создании ZIP архива"
            cd "$ORIGINAL_DIR" || cd - >/dev/null || true
            rm -rf "$TEMP_DIR"
            exit 1
        fi
    elif command -v 7z &> /dev/null; then
        # Альтернатива - 7zip
        echo "[DEBUG] Используем 7z для создания архива..."
        cd "$TEMP_DIR" || exit 1
        if 7z a -tzip "../resources-${VERSION}.zip" . >/dev/null 2>&1; then
            echo "[OK] ZIP архив создан через 7z"
            cd "$ORIGINAL_DIR" || cd - >/dev/null || true
        else
            echo "[ERROR] Ошибка при создании ZIP архива через 7z"
            cd "$ORIGINAL_DIR" || cd - >/dev/null || true
            rm -rf "$TEMP_DIR"
            exit 1
        fi
    else
        echo "[ERROR] Не найдено инструментов для создания ZIP архива"
        echo "[DEBUG] Для Linux/Mac установите: sudo apt-get install zip (или brew install zip)"
        echo "[DEBUG] Для Windows убедитесь, что PowerShell доступен"
        cd - >/dev/null || true
        rm -rf "$TEMP_DIR"
        exit 1
    fi
    
    # Возвращаемся в исходную директорию (если еще не вернулись)
    if [ "$(pwd)" != "$ORIGINAL_DIR" ]; then
        cd "$ORIGINAL_DIR" || cd - >/dev/null || true
    fi
    rm -rf "$TEMP_DIR" 2>/dev/null || rmdir /s /q "$TEMP_DIR" 2>/dev/null
    
    # Проверяем существование ZIP файла (может быть по абсолютному или относительному пути)
    ZIP_CHECK_PATH="$RESOURCES_ZIP"
    if [ ! -f "$ZIP_CHECK_PATH" ]; then
        # Пробуем найти по абсолютному пути (если был создан через PowerShell)
        if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" || "$OSTYPE" == "win32" || -n "$WINDIR" || "$(uname -s 2>/dev/null)" == "MINGW"* ]]; then
            ZIP_ABS_CHECK=$(powershell -NoProfile -Command "[System.IO.Path]::GetFullPath('$RESOURCES_ZIP')" 2>&1 | tail -1 || echo "$RESOURCES_ZIP")
            if [ -f "$ZIP_ABS_CHECK" ]; then
                ZIP_CHECK_PATH="$ZIP_ABS_CHECK"
            elif [ -n "$ZIP_ABS" ] && [ -f "$ZIP_ABS" ]; then
                # Используем переменную ZIP_ABS, если она была установлена
                ZIP_CHECK_PATH="$ZIP_ABS"
            fi
        fi
    fi
    
    if [ -f "$ZIP_CHECK_PATH" ]; then
        echo "[OK] Архив ресурсов создан: $ZIP_CHECK_PATH"
        # Пробуем получить размер (может не работать на Windows с du)
        if command -v du &> /dev/null; then
            echo "[DEBUG] Размер архива: $(du -h "$ZIP_CHECK_PATH" | cut -f1)"
        else
            # На Windows используем PowerShell для размера
            SIZE=$(powershell -NoProfile -Command "(Get-Item '$ZIP_CHECK_PATH').Length" 2>/dev/null || echo "неизвестно")
            echo "[DEBUG] Размер архива: $SIZE байт"
        fi
        ls -lh "$ZIP_CHECK_PATH" 2>/dev/null || dir "$ZIP_CHECK_PATH" 2>/dev/null || echo "Файл: $ZIP_CHECK_PATH"
    else
        echo "[ERROR] ZIP файл не найден: $RESOURCES_ZIP"
        echo "[DEBUG] Проверяемые пути:"
        echo "[DEBUG]   Относительный: $RESOURCES_ZIP"
        if [ -n "$ZIP_ABS_CHECK" ]; then
            echo "[DEBUG]   Абсолютный: $ZIP_ABS_CHECK"
        fi
        exit 1
    fi
fi

# ==============================
# GitHub CLI
# ==============================
echo
echo "========================================"
echo "GitHub CLI"
echo "========================================"

if ! command -v gh &> /dev/null; then
    echo "ОШИБКА: GitHub CLI не установлен"
    exit 1
fi

echo "[INFO] Проверка авторизации gh"
gh auth status

echo
echo "[INFO] Создание релиза в $GITHUB_OWNER/$GITHUB_REPO"

# Проверяем что файлы существуют перед загрузкой
if [ ! -f "$JAR_FILE" ]; then
    echo "[ERROR] JAR файл не найден: $JAR_FILE"
    exit 1
fi

echo "[DEBUG] Проверка файлов перед загрузкой:"
echo "[DEBUG]   JAR: $JAR_FILE ($(du -h "$JAR_FILE" | cut -f1))"

# Загружаем JAR и ресурсы (если архив создан)
if [ -f "$RESOURCES_ZIP" ]; then
    echo "[DEBUG]   ZIP: $RESOURCES_ZIP ($(du -h "$RESOURCES_ZIP" | cut -f1))"
    echo "[INFO] Загрузка JAR файла и ресурсов..."
    if gh release create "$TAG" "$JAR_FILE" "$RESOURCES_ZIP" \
        --repo "$GITHUB_OWNER/$GITHUB_REPO" \
        --title "$RELEASE_NAME" \
        --notes "$RELEASE_NOTES"; then
        echo "[OK] Релиз создан успешно с JAR и ресурсами"
    else
        echo "[ERROR] Ошибка при создании релиза"
        exit 1
    fi
else
    echo "[WARNING] ZIP архив не найден, загружаем только JAR"
    echo "[INFO] Загрузка только JAR файла..."
    if gh release create "$TAG" "$JAR_FILE" \
        --repo "$GITHUB_OWNER/$GITHUB_REPO" \
        --title "$RELEASE_NAME" \
        --notes "$RELEASE_NOTES"; then
        echo "[OK] Релиз создан успешно с JAR"
    else
        echo "[ERROR] Ошибка при создании релиза"
        exit 1
    fi
fi

echo
echo "========================================"
echo "Релиз успешно создан"
echo "========================================"
echo "Репозиторий: $GITHUB_OWNER/$GITHUB_REPO"
echo "Тег: $TAG"
echo "Файл: $JAR_FILE"
echo

echo "[INFO] Скрипт завершён успешно"

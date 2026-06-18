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
    cd "$TEMP_DIR" || exit 1
    
    if command -v zip &> /dev/null; then
        if zip -r "../resources-${VERSION}.zip" . >/dev/null 2>&1; then
            echo "[OK] ZIP архив создан"
        else
            echo "[ERROR] Ошибка при создании ZIP архива"
            cd - >/dev/null || true
            rm -rf "$TEMP_DIR"
            exit 1
        fi
    else
        echo "[ERROR] Команда zip не найдена. Установите zip: sudo apt-get install zip"
        cd - >/dev/null || true
        rm -rf "$TEMP_DIR"
        exit 1
    fi
    
    cd - >/dev/null || true
    rm -rf "$TEMP_DIR"
    
    if [ -f "$RESOURCES_ZIP" ]; then
        echo "[OK] Архив ресурсов создан: $RESOURCES_ZIP"
        echo "[DEBUG] Размер архива: $(du -h "$RESOURCES_ZIP" | cut -f1)"
        ls -lh "$RESOURCES_ZIP"
    else
        echo "[ERROR] ZIP файл не создан: $RESOURCES_ZIP"
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

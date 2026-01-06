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
GITHUB_OWNER="mifedweb"
GITHUB_REPO="ruomi"
ARTIFACT_ID=$(mvn help:evaluate -Dexpression=project.artifactId -q -DforceStdout)
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

JAR_FILE="target/${ARTIFACT_ID}-${VERSION}.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "ОШИБКА: JAR файл не найден: $JAR_FILE"
    exit 1
fi

echo "[OK] JAR найден: $JAR_FILE"

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

gh release create "$TAG" "$JAR_FILE" \
    --repo "$GITHUB_OWNER/$GITHUB_REPO" \
    --title "$RELEASE_NAME" \
    --notes "$RELEASE_NOTES"

echo
echo "========================================"
echo "Релиз успешно создан"
echo "========================================"
echo "Репозиторий: $GITHUB_OWNER/$GITHUB_REPO"
echo "Тег: $TAG"
echo "Файл: $JAR_FILE"
echo

echo "[INFO] Скрипт завершён успешно"

#!/bin/bash

# Скрипт для исправления шаблона updates.html на сервере

set -e

APP_NAME="ruomi"
EXTERNAL_TEMPLATE="/opt/$APP_NAME/external-resources/templates/admin/updates.html"
BACKUP_TEMPLATE="/opt/$APP_NAME/external-resources/templates/admin/updates.html.backup"

info() {
    echo "[INFO] $1"
}

if [ "$EUID" -ne 0 ]; then 
    echo "Запустите скрипт с правами root (sudo)"
    exit 1
fi

info "Исправление шаблона updates.html..."

# Создаем директорию если её нет
mkdir -p "$(dirname "$EXTERNAL_TEMPLATE")"

# Создаем резервную копию
if [ -f "$EXTERNAL_TEMPLATE" ]; then
    cp "$EXTERNAL_TEMPLATE" "$BACKUP_TEMPLATE"
    info "Создана резервная копия: $BACKUP_TEMPLATE"
fi

# Исправляем строку 106 - заменяем запрещенное выражение
if [ -f "$EXTERNAL_TEMPLATE" ]; then
    # Ищем проблемную строку и заменяем (более точный паттерн)
    sed -i 's/th:utext="${#strings.replace(latestRelease.releaseNotes, T(java\.lang\.String)\.valueOf.*)}"/th:text="${latestRelease.releaseNotes}"/g' "$EXTERNAL_TEMPLATE"
    
    # Также заменяем span на div и добавляем white-space: pre-line
    sed -i 's/<span th:utext="${#strings.replace.*}"><\/span>/<div style="white-space: pre-line;" th:text="${latestRelease.releaseNotes}"><\/div>/g' "$EXTERNAL_TEMPLATE"
    
    # Добавляем white-space: pre-line если его нет в style
    if ! grep -q "white-space: pre-line" "$EXTERNAL_TEMPLATE"; then
        sed -i 's/style="max-height: 150px; overflow-y: auto;"/style="max-height: 150px; overflow-y: auto; white-space: pre-line;"/g' "$EXTERNAL_TEMPLATE"
        sed -i 's/style="max-height: 150px; overflow-y: auto"/style="max-height: 150px; overflow-y: auto; white-space: pre-line"/g' "$EXTERNAL_TEMPLATE"
    fi
    
    # Устанавливаем права
    chown ruomi:ruomi "$EXTERNAL_TEMPLATE" 2>/dev/null || true
    
    info "Шаблон исправлен!"
    info "Проверьте файл: $EXTERNAL_TEMPLATE"
else
    info "Шаблон не найден: $EXTERNAL_TEMPLATE"
    info "Создайте его вручную или скопируйте из JAR"
    info "Или просто удалите этот файл - приложение будет использовать шаблон из JAR"
fi

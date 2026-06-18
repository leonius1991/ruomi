#!/bin/bash

# Скрипт для обновления внешних ресурсов на сервере

set -e

APP_NAME="ruomi"
EXTERNAL_RESOURCES_PATH="/opt/$APP_NAME/external-resources"
TEMPLATES_SOURCE="/opt/$APP_NAME/target/classes/templates"
STATIC_SOURCE="/opt/$APP_NAME/target/classes/static"

info() {
    echo "[INFO] $1"
}

error() {
    echo "[ERROR] $1"
    exit 1
}

if [ "$EUID" -ne 0 ]; then 
    error "Запустите скрипт с правами root (sudo)"
fi

info "Обновление внешних ресурсов для $APP_NAME..."

# Создаем директории если их нет
mkdir -p "$EXTERNAL_RESOURCES_PATH/templates"
mkdir -p "$EXTERNAL_RESOURCES_PATH/static"

# Копируем шаблоны из JAR (если есть)
if [ -d "$TEMPLATES_SOURCE" ]; then
    info "Копирование шаблонов из $TEMPLATES_SOURCE..."
    cp -r "$TEMPLATES_SOURCE"/* "$EXTERNAL_RESOURCES_PATH/templates/" 2>/dev/null || true
fi

# Копируем статические файлы из JAR (если есть)
if [ -d "$STATIC_SOURCE" ]; then
    info "Копирование статических файлов из $STATIC_SOURCE..."
    cp -r "$STATIC_SOURCE"/* "$EXTERNAL_RESOURCES_PATH/static/" 2>/dev/null || true
fi

# Устанавливаем права
chown -R $APP_NAME:$APP_NAME "$EXTERNAL_RESOURCES_PATH"
chmod -R 755 "$EXTERNAL_RESOURCES_PATH"

info "Внешние ресурсы обновлены!"

# Показываем список обновленных файлов
info "Обновленные файлы:"
find "$EXTERNAL_RESOURCES_PATH" -type f | head -20



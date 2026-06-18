#!/bin/bash

# Скрипт перезапуска приложения для Linux
# Предполагается запуск от root

APP_NAME="ruomi"

echo "Перезапуск приложения $APP_NAME..."

# Проверяем, запущен ли от root
if [ "$EUID" -ne 0 ]; then 
    echo "ПРЕДУПРЕЖДЕНИЕ: Скрипт запущен не от root. Пытаемся использовать sudo..."
    if command -v sudo >/dev/null 2>&1; then
        exec sudo "$0" "$@"
    else
        echo "ОШИБКА: Требуется запуск от root или настройка sudoers"
        exit 1
    fi
fi

# Используем systemd, если доступен
if systemctl list-unit-files | grep -q "^${APP_NAME}.service" 2>/dev/null || \
   systemctl list-unit-files | grep -q "${APP_NAME}.service" 2>/dev/null; then
    echo "Использование systemd для перезапуска..."
    
    # Перезапускаем сервис (от root не нужен sudo)
    echo "Перезапуск сервиса..."
    if systemctl restart $APP_NAME; then
        sleep 3
        if systemctl is-active --quiet $APP_NAME; then
            echo "Сервис успешно перезапущен"
            systemctl status $APP_NAME --no-pager -l | head -20
            exit 0
        else
            echo "ОШИБКА: Сервис не активен после перезапуска"
            systemctl status $APP_NAME --no-pager -l
            exit 1
        fi
    else
        echo "ОШИБКА: Не удалось перезапустить сервис"
        systemctl status $APP_NAME --no-pager -l
        exit 1
    fi
else
    echo "ОШИБКА: Systemd сервис $APP_NAME.service не найден"
    echo "Создайте сервис или используйте ручной перезапуск"
    exit 1
fi

echo "Готово!"


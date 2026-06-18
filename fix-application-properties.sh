#!/bin/bash

# Скрипт для исправления application.properties на сервере

APP_DIR="/opt/ruomi"
PROPERTIES_FILE="$APP_DIR/application.properties"

echo "Исправление application.properties..."

# Проверяем существование файла
if [ ! -f "$PROPERTIES_FILE" ]; then
    echo "ОШИБКА: Файл $PROPERTIES_FILE не найден!"
    exit 1
fi

# Проверяем, есть ли server.servlet.context-path
if ! grep -q "server.servlet.context-path" "$PROPERTIES_FILE"; then
    echo "Добавление server.servlet.context-path..."
    # Добавляем после server.port
    sed -i '/^server.port=/a server.servlet.context-path=/' "$PROPERTIES_FILE"
    echo "  ✓ Добавлено"
else
    echo "  ✓ server.servlet.context-path уже есть"
fi

# Проверяем другие обязательные свойства
echo "Проверка других свойств..."

# Email настройки (если не указаны, добавляем заглушки)
if ! grep -q "spring.mail.username" "$PROPERTIES_FILE"; then
    echo "Добавление email настроек..."
    cat >> "$PROPERTIES_FILE" <<EOF

# Email Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
EOF
    echo "  ✓ Email настройки добавлены (замените на реальные!)"
fi

# JWT секрет
if ! grep -q "jwt.secret" "$PROPERTIES_FILE"; then
    echo "Добавление JWT секрета..."
    cat >> "$PROPERTIES_FILE" <<EOF

# Security Configuration
jwt.secret=your-secret-key-here-change-in-production
jwt.expiration=86400000
EOF
    echo "  ✓ JWT настройки добавлены (замените секрет!)"
fi

echo
echo "Готово! Проверьте файл:"
echo "  $PROPERTIES_FILE"
echo
echo "ВАЖНО: Замените email и JWT секрет на реальные значения!"



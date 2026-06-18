#!/bin/bash

# Скрипт для настройки sudoers для автоматического перезапуска ruomi

APP_NAME="ruomi"
APP_USER="${APP_USER:-ruomi}"

echo "Настройка sudoers для пользователя $APP_USER..."

# Проверяем, запущен ли скрипт от root
if [ "$EUID" -ne 0 ]; then 
    echo "ОШИБКА: Этот скрипт должен быть запущен от root (sudo)"
    echo "Запустите: sudo bash setup-sudoers.sh"
    exit 1
fi

# Создаем правило в sudoers.d
SUDOERS_FILE="/etc/sudoers.d/${APP_NAME}-restart"

cat > "$SUDOERS_FILE" << EOF
# Разрешить пользователю $APP_USER перезапускать сервис $APP_NAME без пароля
$APP_USER ALL=(ALL) NOPASSWD: /bin/systemctl restart $APP_NAME
$APP_USER ALL=(ALL) NOPASSWD: /bin/systemctl stop $APP_NAME
$APP_USER ALL=(ALL) NOPASSWD: /bin/systemctl start $APP_NAME
$APP_USER ALL=(ALL) NOPASSWD: /bin/systemctl status $APP_NAME
EOF

# Устанавливаем правильные права
chmod 0440 "$SUDOERS_FILE"

# Проверяем синтаксис
if visudo -c -f "$SUDOERS_FILE" 2>/dev/null; then
    echo "✓ Sudoers файл создан и настроен: $SUDOERS_FILE"
    echo "✓ Пользователь $APP_USER теперь может перезапускать $APP_NAME без пароля"
    echo ""
    echo "Содержимое файла:"
    cat "$SUDOERS_FILE"
else
    echo "ОШИБКА: Синтаксическая ошибка в sudoers файле"
    rm -f "$SUDOERS_FILE"
    exit 1
fi

echo ""
echo "Готово! Теперь обновления должны работать автоматически."



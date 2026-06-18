#!/bin/bash

# Скрипт для настройки файлового логирования и исправления проблем

set -e

echo "========================================"
echo "   Исправление логирования Ruomi"
echo "========================================"
echo

APP_NAME="ruomi"
APP_DIR="/opt/$APP_NAME"

# Проверка прав root
if [ "$EUID" -ne 0 ]; then 
    echo "ОШИБКА: Запустите скрипт с правами root (sudo)"
    exit 1
fi

# 1. Создание директории для логов
echo "[1] Создание директории для логов..."
mkdir -p $APP_DIR/logs
chown -R $APP_NAME:$APP_NAME $APP_DIR/logs
echo "  ✓ Директория создана: $APP_DIR/logs"

# 2. Обновление systemd сервиса для файлового логирования
echo
echo "[2] Обновление systemd сервиса..."
cat > /etc/systemd/system/${APP_NAME}.service <<EOF
[Unit]
Description=Ruomi Application
After=network.target mysql.service

[Service]
Type=simple
User=$APP_NAME
WorkingDirectory=$APP_DIR
ExecStart=/usr/bin/java -jar -Dspring.config.location=$APP_DIR/application.properties $APP_DIR/app.jar
Restart=always
RestartSec=10
StandardOutput=append:$APP_DIR/logs/app.log
StandardError=append:$APP_DIR/logs/error.log

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
echo "  ✓ Сервис обновлен"

# 3. Проверка конфигурации
echo
echo "[3] Проверка конфигурации..."
if [ -f "$APP_DIR/application.properties" ]; then
    echo "  ✓ application.properties существует"
    
    # Проверяем настройки логирования
    if ! grep -q "logging.file.name" $APP_DIR/application.properties; then
        echo "" >> $APP_DIR/application.properties
        echo "# Logging Configuration" >> $APP_DIR/application.properties
        echo "logging.file.name=$APP_DIR/logs/application.log" >> $APP_DIR/application.properties
        echo "logging.file.max-size=10MB" >> $APP_DIR/application.properties
        echo "logging.file.max-history=30" >> $APP_DIR/application.properties
        echo "  ✓ Настройки логирования добавлены"
    fi
else
    echo "  ❌ application.properties не найден!"
    exit 1
fi

# 4. Проверка прав доступа
echo
echo "[4] Проверка прав доступа..."
chown -R $APP_NAME:$APP_NAME $APP_DIR
echo "  ✓ Права обновлены"

# 5. Проверка Java
echo
echo "[5] Проверка Java..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1)
    echo "  ✓ Java найдена: $JAVA_VERSION"
else
    echo "  ❌ Java не найдена!"
    exit 1
fi

# 6. Проверка JAR файла
echo
echo "[6] Проверка JAR файла..."
if [ -f "$APP_DIR/app.jar" ]; then
    echo "  ✓ JAR файл существует"
    ls -lh $APP_DIR/app.jar
else
    echo "  ❌ JAR файл не найден: $APP_DIR/app.jar"
    exit 1
fi

# 7. Тестовый запуск
echo
echo "[7] Попытка тестового запуска..."
echo "Выполните вручную для просмотра ошибок:"
echo "  cd $APP_DIR"
echo "  sudo -u $APP_NAME java -jar -Dspring.config.location=$APP_DIR/application.properties $APP_DIR/app.jar"
echo

read -p "Запустить сервис сейчас? (y/n): " START_NOW
if [ "$START_NOW" = "y" ]; then
    systemctl start $APP_NAME
    sleep 3
    
    if systemctl is-active --quiet $APP_NAME; then
        echo "  ✓ Сервис запущен успешно!"
        echo
        echo "Логи:"
        echo "  Файл: tail -f $APP_DIR/logs/app.log"
        echo "  Ошибки: tail -f $APP_DIR/logs/error.log"
        echo "  Journal: journalctl -u $APP_NAME -f"
    else
        echo "  ❌ Сервис не запустился"
        echo
        echo "Проверьте логи:"
        echo "  journalctl -u $APP_NAME -n 50"
        echo "  cat $APP_DIR/logs/error.log"
    fi
fi

echo
echo "========================================"
echo "   Готово!"
echo "========================================"
echo
echo "Логи находятся в:"
echo "  $APP_DIR/logs/app.log"
echo "  $APP_DIR/logs/error.log"
echo
echo "Команды для просмотра логов:"
echo "  tail -f $APP_DIR/logs/app.log"
echo "  tail -f $APP_DIR/logs/error.log"
echo "  journalctl -u $APP_NAME -f"
echo



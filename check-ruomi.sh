#!/bin/bash

# Скрипт диагностики проблем с запуском Ruomi

echo "========================================"
echo "   Диагностика Ruomi"
echo "========================================"
echo

APP_NAME="ruomi"
APP_DIR="/opt/$APP_NAME"

# 1. Проверка статуса сервиса
echo "[1] Проверка статуса сервиса..."
systemctl status $APP_NAME --no-pager -l
echo

# 2. Проверка логов через journalctl
echo "[2] Последние логи сервиса (journalctl):"
echo "----------------------------------------"
journalctl -u $APP_NAME -n 50 --no-pager
echo

# 3. Проверка конфигурации systemd
echo "[3] Проверка конфигурации systemd сервиса:"
echo "----------------------------------------"
cat /etc/systemd/system/${APP_NAME}.service
echo

# 4. Проверка существования файлов
echo "[4] Проверка файлов:"
echo "----------------------------------------"
echo "JAR файл:"
ls -lh $APP_DIR/app.jar 2>/dev/null || echo "  ❌ Не найден!"
echo
echo "Конфигурация:"
ls -lh $APP_DIR/application.properties 2>/dev/null || echo "  ❌ Не найден!"
echo
echo "Директория:"
ls -ld $APP_DIR
echo

# 5. Проверка прав доступа
echo "[5] Проверка прав доступа:"
echo "----------------------------------------"
echo "Владелец директории:"
stat -c "%U:%G %a" $APP_DIR
echo
echo "Владелец файлов:"
stat -c "%U:%G %a" $APP_DIR/app.jar 2>/dev/null || echo "  Файл не найден"
stat -c "%U:%G %a" $APP_DIR/application.properties 2>/dev/null || echo "  Файл не найден"
echo

# 6. Проверка Java
echo "[6] Проверка Java:"
echo "----------------------------------------"
if command -v java &> /dev/null; then
    java -version
    echo "Путь к Java: $(which java)"
else
    echo "  ❌ Java не найдена!"
fi
echo

# 7. Проверка порта
echo "[7] Проверка порта 8080:"
echo "----------------------------------------"
netstat -tlnp | grep 8080 || echo "  Порт 8080 не занят"
echo

# 8. Попытка запуска вручную
echo "[8] Попытка запуска вручную (для просмотра ошибок):"
echo "----------------------------------------"
echo "Выполните вручную:"
echo "  cd $APP_DIR"
echo "  sudo -u $APP_NAME java -jar -Dspring.config.location=$APP_DIR/application.properties $APP_DIR/app.jar"
echo

# 9. Проверка конфигурации приложения
echo "[9] Проверка application.properties:"
echo "----------------------------------------"
if [ -f "$APP_DIR/application.properties" ]; then
    echo "Файл существует, первые строки:"
    head -20 $APP_DIR/application.properties
else
    echo "  ❌ Файл не найден!"
fi
echo

# 10. Проверка MySQL
echo "[10] Проверка MySQL:"
echo "----------------------------------------"
if systemctl is-active --quiet mysql 2>/dev/null || systemctl is-active --quiet mysqld 2>/dev/null; then
    echo "  ✓ MySQL запущен"
else
    echo "  ❌ MySQL не запущен!"
fi
echo

echo "========================================"
echo "   Рекомендации"
echo "========================================"
echo
echo "Если сервис не запускается:"
echo "  1. Проверьте логи выше"
echo "  2. Попробуйте запустить вручную (см. пункт 8)"
echo "  3. Проверьте application.properties на ошибки"
echo "  4. Убедитесь, что MySQL запущен и доступен"
echo "  5. Проверьте права доступа к файлам"
echo



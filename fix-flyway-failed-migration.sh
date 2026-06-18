#!/bin/bash
# Исправление failed Flyway-миграции на VPS после ошибки "Duplicate column name"
# Запуск на сервере: sudo bash fix-flyway-failed-migration.sh

set -e

DB_NAME="${DB_NAME:-ruomi}"
DB_USER="${DB_USER:-ruomi}"
DB_PASS="${DB_PASS:-}"

if [ -z "$DB_PASS" ] && [ -f /opt/ruomi/application.properties ]; then
    DB_PASS=$(grep -E '^spring\.datasource\.password=' /opt/ruomi/application.properties | cut -d= -f2- | tr -d '\r')
fi

if [ -z "$DB_PASS" ]; then
    echo "Укажите пароль: DB_PASS=... sudo -E bash $0"
    exit 1
fi

echo "Проверка flyway_schema_history в базе $DB_NAME..."
mysql -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "
SELECT installed_rank, version, description, success
FROM flyway_schema_history
WHERE version >= '6'
ORDER BY installed_rank;
"

echo ""
echo "Удаление записи о неудачной миграции V7 (если есть)..."
mysql -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "
DELETE FROM flyway_schema_history
WHERE version = '7' AND success = 0;
"

echo "Готово. Пересоберите и залейте JAR с исправленным V7, затем:"
echo "  sudo systemctl restart ruomi"

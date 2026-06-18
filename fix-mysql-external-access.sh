#!/bin/bash

# Скрипт для настройки внешнего доступа к MySQL

set -e

echo "========================================"
echo "   Настройка внешнего доступа к MySQL"
echo "========================================"
echo

# Проверка прав root
if [ "$EUID" -ne 0 ]; then 
    echo "ОШИБКА: Запустите скрипт с правами root (sudo)"
    exit 1
fi

# 1. Проверка текущей конфигурации
echo "[1] Проверка текущей конфигурации MySQL..."
MYSQL_BIND=$(grep -E "^bind-address" /etc/mysql/mysql.conf.d/mysqld.cnf 2>/dev/null || grep -E "^bind-address" /etc/mysql/my.cnf 2>/dev/null || echo "bind-address = 127.0.0.1")

if echo "$MYSQL_BIND" | grep -q "127.0.0.1"; then
    echo "  ⚠️ MySQL настроен только на localhost"
    NEED_FIX=true
else
    echo "  ✓ MySQL уже настроен на внешний доступ"
    NEED_FIX=false
fi

# 2. Проверка файрвола
echo
echo "[2] Проверка файрвола..."
if command -v ufw &> /dev/null; then
    if ufw status | grep -q "3306"; then
        echo "  ✓ Порт 3306 открыт в UFW"
    else
        echo "  ⚠️ Порт 3306 не открыт в UFW"
        read -p "Открыть порт 3306 в UFW? (y/n) [y]: " OPEN_FIREWALL
        OPEN_FIREWALL=${OPEN_FIREWALL:-y}
        if [ "$OPEN_FIREWALL" = "y" ]; then
            ufw allow 3306/tcp
            echo "  ✓ Порт 3306 открыт"
        fi
    fi
elif command -v firewall-cmd &> /dev/null; then
    if firewall-cmd --list-ports | grep -q "3306"; then
        echo "  ✓ Порт 3306 открыт в firewalld"
    else
        echo "  ⚠️ Порт 3306 не открыт в firewalld"
        read -p "Открыть порт 3306 в firewalld? (y/n) [y]: " OPEN_FIREWALL
        OPEN_FIREWALL=${OPEN_FIREWALL:-y}
        if [ "$OPEN_FIREWALL" = "y" ]; then
            firewall-cmd --permanent --add-port=3306/tcp
            firewall-cmd --reload
            echo "  ✓ Порт 3306 открыт"
        fi
    fi
else
    echo "  ⚠️ Файрвол не найден (UFW или firewalld)"
fi

# 3. Настройка bind-address
if [ "$NEED_FIX" = "true" ]; then
    echo
    echo "[3] Настройка MySQL для внешнего доступа..."
    
    # Находим файл конфигурации
    MYSQL_CONF=""
    if [ -f "/etc/mysql/mysql.conf.d/mysqld.cnf" ]; then
        MYSQL_CONF="/etc/mysql/mysql.conf.d/mysqld.cnf"
    elif [ -f "/etc/mysql/my.cnf" ]; then
        MYSQL_CONF="/etc/mysql/my.cnf"
    else
        echo "  ❌ Файл конфигурации MySQL не найден"
        exit 1
    fi
    
    echo "  Файл конфигурации: $MYSQL_CONF"
    
    # Создаем резервную копию
    cp "$MYSQL_CONF" "${MYSQL_CONF}.backup.$(date +%Y%m%d_%H%M%S)"
    echo "  ✓ Резервная копия создана"
    
    # Изменяем bind-address
    if grep -q "^bind-address" "$MYSQL_CONF"; then
        sed -i 's/^bind-address.*/bind-address = 0.0.0.0/' "$MYSQL_CONF"
    else
        # Добавляем bind-address в секцию [mysqld]
        if grep -q "^\[mysqld\]" "$MYSQL_CONF"; then
            sed -i '/^\[mysqld\]/a bind-address = 0.0.0.0' "$MYSQL_CONF"
        else
            echo -e "\n[mysqld]\nbind-address = 0.0.0.0" >> "$MYSQL_CONF"
        fi
    fi
    
    echo "  ✓ bind-address изменен на 0.0.0.0"
    
    # Перезапускаем MySQL
    echo
    echo "[4] Перезапуск MySQL..."
    systemctl restart mysql || systemctl restart mysqld
    sleep 3
    
    if systemctl is-active --quiet mysql || systemctl is-active --quiet mysqld; then
        echo "  ✓ MySQL перезапущен"
    else
        echo "  ❌ Ошибка при перезапуске MySQL"
        exit 1
    fi
fi

# 5. Проверка пользователей MySQL
echo
echo "[5] Проверка пользователей MySQL..."
read -p "Введите пароль root MySQL: " -s MYSQL_ROOT_PASSWORD
echo

# Проверяем, есть ли пользователь с доступом извне
MYSQL_USERS=$(mysql -u root -p"$MYSQL_ROOT_PASSWORD" -e "SELECT User, Host FROM mysql.user WHERE Host != 'localhost';" 2>/dev/null || echo "")

if [ -z "$MYSQL_USERS" ] || ! echo "$MYSQL_USERS" | grep -q "%"; then
    echo "  ⚠️ Нет пользователей с доступом извне"
    read -p "Создать пользователя с доступом извне? (y/n) [y]: " CREATE_USER
    CREATE_USER=${CREATE_USER:-y}
    
    if [ "$CREATE_USER" = "y" ]; then
        read -p "Введите имя пользователя БД [ruomi]: " DB_USER
        DB_USER=${DB_USER:-ruomi}
        
        read -p "Введите пароль для пользователя: " -s DB_PASSWORD
        echo
        
        # Создаем пользователя с доступом извне
        mysql -u root -p"$MYSQL_ROOT_PASSWORD" <<EOF
CREATE USER IF NOT EXISTS '${DB_USER}'@'%' IDENTIFIED BY '${DB_PASSWORD}';
GRANT ALL PRIVILEGES ON ruomi.* TO '${DB_USER}'@'%';
FLUSH PRIVILEGES;
EOF
        
        echo "  ✓ Пользователь $DB_USER создан с доступом извне"
    fi
else
    echo "  ✓ Пользователи с доступом извне найдены"
fi

# 6. Проверка подключения
echo
echo "[6] Проверка подключения..."
if netstat -tlnp 2>/dev/null | grep -q ":3306" || ss -tlnp 2>/dev/null | grep -q ":3306"; then
    LISTENING=$(netstat -tlnp 2>/dev/null | grep ":3306" || ss -tlnp 2>/dev/null | grep ":3306")
    if echo "$LISTENING" | grep -q "0.0.0.0:3306"; then
        echo "  ✓ MySQL слушает на 0.0.0.0:3306 (внешний доступ)"
    else
        echo "  ⚠️ MySQL слушает только на localhost"
    fi
else
    echo "  ❌ MySQL не слушает на порту 3306"
fi

echo
echo "========================================"
echo "   Готово!"
echo "========================================"
echo
echo "Важно для безопасности:"
echo "1. Используйте сильные пароли"
echo "2. Ограничьте доступ по IP, если возможно"
echo "3. Используйте SSL для подключений"
echo
echo "Для ограничения доступа по IP выполните:"
echo "  GRANT ALL PRIVILEGES ON ruomi.* TO 'user'@'YOUR_IP' IDENTIFIED BY 'password';"
echo



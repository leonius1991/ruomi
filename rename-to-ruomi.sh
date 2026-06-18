#!/bin/bash

# Скрипт переименования установки с doska на ruomi

set -e

echo "========================================"
echo "   Переименование doska -> ruomi"
echo "========================================"
echo

# Проверка прав root
if [ "$EUID" -ne 0 ]; then 
    echo "ОШИБКА: Запустите скрипт с правами root (sudo)"
    exit 1
fi

OLD_NAME="doska"
NEW_NAME="ruomi"
OLD_DIR="/opt/$OLD_NAME"
NEW_DIR="/opt/$NEW_NAME"

echo "Будет выполнено:"
echo "  1. Остановка сервиса $OLD_NAME"
echo "  2. Переименование пользователя $OLD_NAME -> $NEW_NAME"
echo "  3. Переименование директории $OLD_DIR -> $NEW_DIR"
echo "  4. Переименование systemd сервиса $OLD_NAME.service -> $NEW_NAME.service"
echo "  5. Обновление конфигурации"
echo "  6. Запуск нового сервиса"
echo
read -p "Продолжить? (y/n): " CONFIRM
if [ "$CONFIRM" != "y" ]; then
    echo "Отменено"
    exit 0
fi

# 1. Остановка старого сервиса
echo
echo "[1/6] Остановка сервиса $OLD_NAME..."
if systemctl is-active --quiet $OLD_NAME 2>/dev/null; then
    systemctl stop $OLD_NAME
    echo "  Сервис остановлен"
else
    echo "  Сервис не запущен"
fi

# 2. Отключение старого сервиса
echo
echo "[2/6] Отключение старого сервиса..."
if systemctl is-enabled --quiet $OLD_NAME 2>/dev/null; then
    systemctl disable $OLD_NAME
    echo "  Сервис отключен"
fi

# 3. Переименование пользователя
echo
echo "[3/6] Переименование пользователя..."
if id "$OLD_NAME" &>/dev/null; then
    if id "$NEW_NAME" &>/dev/null; then
        echo "  Пользователь $NEW_NAME уже существует, пропускаем"
    else
        usermod -l $NEW_NAME -d $NEW_DIR -m $OLD_NAME
        groupmod -n $NEW_NAME $OLD_NAME
        echo "  Пользователь переименован: $OLD_NAME -> $NEW_NAME"
    fi
else
    echo "  Пользователь $OLD_NAME не найден"
fi

# 4. Переименование директории
echo
echo "[4/6] Переименование директории..."
if [ -d "$OLD_DIR" ]; then
    if [ -d "$NEW_DIR" ]; then
        echo "  Директория $NEW_DIR уже существует"
        read -p "  Удалить старую директорию $OLD_DIR? (y/n): " DELETE_OLD
        if [ "$DELETE_OLD" = "y" ]; then
            rm -rf "$OLD_DIR"
            echo "  Старая директория удалена"
        fi
    else
        mv "$OLD_DIR" "$NEW_DIR"
        echo "  Директория переименована: $OLD_DIR -> $NEW_DIR"
    fi
    chown -R $NEW_NAME:$NEW_NAME "$NEW_DIR"
    echo "  Права обновлены"
else
    echo "  Директория $OLD_DIR не найдена"
fi

# 5. Обновление конфигурации
echo
echo "[5/6] Обновление конфигурации..."
if [ -f "$NEW_DIR/application.properties" ]; then
    # Обновляем имя сервиса в конфигурации
    sed -i "s/app.update.restart.service=doska/app.update.restart.service=$NEW_NAME/g" "$NEW_DIR/application.properties"
    echo "  application.properties обновлен"
fi

# Обновление restart.sh
if [ -f "$NEW_DIR/restart.sh" ]; then
    cat > "$NEW_DIR/restart.sh" <<EOF
#!/bin/bash
cd $NEW_DIR
sudo systemctl restart $NEW_NAME
EOF
    chmod +x "$NEW_DIR/restart.sh"
    chown $NEW_NAME:$NEW_NAME "$NEW_DIR/restart.sh"
    echo "  restart.sh обновлен"
fi

# 6. Переименование systemd сервиса
echo
echo "[6/6] Переименование systemd сервиса..."
if [ -f "/etc/systemd/system/$OLD_NAME.service" ]; then
    # Обновляем содержимое сервиса
    cat > "/etc/systemd/system/$NEW_NAME.service" <<EOF
[Unit]
Description=Ruomi Application
After=network.target mysql.service

[Service]
Type=simple
User=$NEW_NAME
WorkingDirectory=$NEW_DIR
ExecStart=/usr/bin/java -jar -Dspring.config.location=$NEW_DIR/application.properties $NEW_DIR/app.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF
    
    # Удаляем старый сервис
    rm -f "/etc/systemd/system/$OLD_NAME.service"
    
    # Перезагружаем systemd
    systemctl daemon-reload
    
    # Включаем новый сервис
    systemctl enable $NEW_NAME
    
    echo "  Сервис переименован: $OLD_NAME.service -> $NEW_NAME.service"
else
    echo "  Старый сервис не найден, создаем новый..."
    cat > "/etc/systemd/system/$NEW_NAME.service" <<EOF
[Unit]
Description=Ruomi Application
After=network.target mysql.service

[Service]
Type=simple
User=$NEW_NAME
WorkingDirectory=$NEW_DIR
ExecStart=/usr/bin/java -jar -Dspring.config.location=$NEW_DIR/application.properties $NEW_DIR/app.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF
    systemctl daemon-reload
    systemctl enable $NEW_NAME
    echo "  Новый сервис создан"
fi

echo
echo "========================================"
echo "   Переименование завершено!"
echo "========================================"
echo
echo "Информация:"
echo "  Директория: $NEW_DIR"
echo "  Пользователь: $NEW_NAME"
echo "  Сервис: $NEW_NAME.service"
echo
echo "Команды управления:"
echo "  Запуск:   sudo systemctl start $NEW_NAME"
echo "  Остановка: sudo systemctl stop $NEW_NAME"
echo "  Статус:   sudo systemctl status $NEW_NAME"
echo "  Логи:     sudo journalctl -u $NEW_NAME -f"
echo
read -p "Запустить сервис сейчас? (y/n): " START_NOW
if [ "$START_NOW" = "y" ]; then
    systemctl start $NEW_NAME
    sleep 2
    if systemctl is-active --quiet $NEW_NAME; then
        echo "Сервис запущен успешно!"
    else
        echo "Ошибка при запуске. Проверьте логи: sudo journalctl -u $NEW_NAME -n 50"
    fi
fi
echo



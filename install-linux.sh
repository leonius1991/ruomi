#!/bin/bash

# Скрипт автоматической установки Doska на Linux
# Поддерживает Ubuntu/Debian и CentOS/RHEL

set -e

echo "========================================"
echo "   Автоматическая установка Doska"
echo "========================================"
echo

# Определение дистрибутива
if [ -f /etc/os-release ]; then
    . /etc/os-release
    OS=$ID
    VER=$VERSION_ID
else
    echo "Не удалось определить дистрибутив Linux"
    exit 1
fi

echo "Обнаружен дистрибутив: $OS $VER"
echo

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Функция для вывода сообщений
info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Проверка прав root
if [ "$EUID" -ne 0 ]; then 
    error "Запустите скрипт с правами root (sudo)"
    exit 1
fi

# Установка Java 17
info "Проверка Java..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 17 ]; then
        info "Java уже установлена: $(java -version 2>&1 | head -n 1)"
    else
        warn "Установлена старая версия Java. Требуется обновление."
        INSTALL_JAVA=true
    fi
else
    INSTALL_JAVA=true
fi

if [ "$INSTALL_JAVA" = true ]; then
    info "Установка Java 17..."
    if [ "$OS" = "ubuntu" ] || [ "$OS" = "debian" ]; then
        apt-get update
        apt-get install -y openjdk-17-jdk
    elif [ "$OS" = "centos" ] || [ "$OS" = "rhel" ] || [ "$OS" = "fedora" ]; then
        yum install -y java-17-openjdk-devel
    else
        error "Неподдерживаемый дистрибутив для автоматической установки Java"
        exit 1
    fi
    info "Java установлена"
fi

# Установка MySQL
info "Проверка MySQL..."
if command -v mysql &> /dev/null; then
    info "MySQL уже установлен"
else
    info "Установка MySQL..."
    if [ "$OS" = "ubuntu" ] || [ "$OS" = "debian" ]; then
        apt-get install -y mysql-server
        systemctl start mysql
        systemctl enable mysql
    elif [ "$OS" = "centos" ] || [ "$OS" = "rhel" ] || [ "$OS" = "fedora" ]; then
        yum install -y mysql-server
        systemctl start mysqld
        systemctl enable mysqld
    fi
    info "MySQL установлен"
fi

# Настройка MySQL
info "Настройка базы данных..."
read -p "Введите пароль для root MySQL (Enter для пропуска): " MYSQL_ROOT_PASSWORD
read -p "Введите имя базы данных [newdoska]: " DB_NAME
DB_NAME=${DB_NAME:-newdoska}
read -p "Введите пользователя БД [doska_user]: " DB_USER
DB_USER=${DB_USER:-doska_user}
read -sp "Введите пароль пользователя БД: " DB_PASSWORD
echo

# Создание базы данных и пользователя
MYSQL_CMD="mysql"
if [ -n "$MYSQL_ROOT_PASSWORD" ]; then
    MYSQL_CMD="mysql -uroot -p$MYSQL_ROOT_PASSWORD"
fi

$MYSQL_CMD <<EOF
CREATE DATABASE IF NOT EXISTS ${DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'localhost';
FLUSH PRIVILEGES;
EOF

info "База данных создана"

# Запрос имени приложения
read -p "Введите имя приложения [ruomi]: " APP_NAME
APP_NAME=${APP_NAME:-ruomi}

# Создание пользователя для приложения
info "Создание пользователя для приложения..."
if id "$APP_NAME" &>/dev/null; then
    warn "Пользователь $APP_NAME уже существует"
else
    useradd -r -s /bin/false -d /opt/$APP_NAME $APP_NAME
    info "Пользователь $APP_NAME создан"
fi

# Создание директорий
info "Создание директорий..."
APP_DIR="/opt/$APP_NAME"
mkdir -p $APP_DIR/{updates,backups,external-resources/static,external-resources/templates}
chown -R $APP_NAME:$APP_NAME $APP_DIR
info "Директории созданы: $APP_DIR"

# Загрузка JAR файла
info "Загрузка JAR файла..."
read -p "Введите URL для скачивания JAR (или Enter для использования GitHub Releases): " JAR_URL

if [ -z "$JAR_URL" ]; then
    GITHUB_OWNER="mifedweb"
    GITHUB_REPO="ruomi"
    LATEST_RELEASE=$(curl -s "https://api.github.com/repos/${GITHUB_OWNER}/${GITHUB_REPO}/releases/latest")
    JAR_URL=$(echo $LATEST_RELEASE | grep -o '"browser_download_url": "[^"]*\.jar"' | head -1 | cut -d'"' -f4)
    
    if [ -z "$JAR_URL" ]; then
        error "Не удалось получить URL JAR файла с GitHub"
        read -p "Введите URL вручную: " JAR_URL
    fi
fi

if [ -n "$JAR_URL" ]; then
    cd $APP_DIR
    wget -q "$JAR_URL" -O app.jar || curl -L "$JAR_URL" -o app.jar
    chown $APP_NAME:$APP_NAME app.jar
    info "JAR файл загружен"
else
    warn "JAR файл не загружен. Загрузите его вручную в $APP_DIR/app.jar"
fi

# Создание application.properties
info "Создание конфигурации..."
read -p "Введите порт приложения [8080]: " APP_PORT
APP_PORT=${APP_PORT:-8080}

cat > $APP_DIR/application.properties <<EOF
# Server Configuration
server.port=${APP_PORT}
server.servlet.context-path=/

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/${DB_NAME}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# Thymeleaf Configuration
spring.thymeleaf.cache=false
spring.thymeleaf.encoding=UTF-8

# Application Update Configuration
app.version=0.0.1-SNAPSHOT
app.update.github.owner=mifedweb
app.update.github.repo=ruomi
app.update.download.path=./updates
app.update.backup.path=./backups
app.jar.path=./app.jar
app.update.restart.enabled=true
app.update.restart.script=./restart.sh
app.update.restart.service=${APP_NAME}

# External Resources Configuration
app.resources.use-external=true
app.resources.external.path=./external-resources

# Disable caching
spring.web.resources.cache.period=0
spring.web.resources.chain.cache=false
EOF

chown $APP_NAME:$APP_NAME $APP_DIR/application.properties
info "Конфигурация создана"

# Создание скрипта перезапуска
info "Создание скрипта перезапуска..."
cat > $APP_DIR/restart.sh <<RESTART_EOF
#!/bin/bash
cd $APP_DIR
sudo systemctl restart $APP_NAME
RESTART_EOF

chmod +x $APP_DIR/restart.sh
chown $APP_NAME:$APP_NAME $APP_DIR/restart.sh
info "Скрипт перезапуска создан"

# Создание systemd сервиса
info "Создание systemd сервиса..."
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
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable $APP_NAME
info "Systemd сервис создан: ${APP_NAME}.service"

# Запуск приложения
read -p "Запустить приложение сейчас? (y/n) [y]: " START_APP
START_APP=${START_APP:-y}

if [ "$START_APP" = "y" ]; then
    info "Запуск приложения..."
    systemctl start $APP_NAME
    sleep 3
    
    if systemctl is-active --quiet $APP_NAME; then
        info "Приложение запущено успешно!"
        info "Статус: $(systemctl is-active $APP_NAME)"
        info "Логи: sudo journalctl -u $APP_NAME -f"
    else
        error "Не удалось запустить приложение"
        error "Проверьте логи: sudo journalctl -u $APP_NAME -n 50"
    fi
fi

echo
echo "========================================"
echo "   Установка завершена!"
echo "========================================"
echo
echo "Информация:"
echo "  Директория приложения: $APP_DIR"
echo "  Порт: $APP_PORT"
echo "  База данных: $DB_NAME"
echo "  Пользователь БД: $DB_USER"
echo
echo "Команды управления:"
echo "  Запуск:   sudo systemctl start $APP_NAME"
echo "  Остановка: sudo systemctl stop $APP_NAME"
echo "  Статус:   sudo systemctl status $APP_NAME"
echo "  Логи:     sudo journalctl -u $APP_NAME -f"
echo
echo "Доступ к приложению: http://localhost:${APP_PORT}"
echo


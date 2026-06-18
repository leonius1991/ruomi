#!/bin/bash

# Скрипт настройки домена и Nginx для Ruomi

set -e

echo "========================================"
echo "   Настройка домена для Ruomi"
echo "========================================"
echo

# Проверка прав root
if [ "$EUID" -ne 0 ]; then 
    echo "ОШИБКА: Запустите скрипт с правами root (sudo)"
    exit 1
fi

# Цвета
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Запрос домена
read -p "Введите домен (например, ruomi.fi): " DOMAIN
if [ -z "$DOMAIN" ]; then
    error "Домен не может быть пустым"
    exit 1
fi

# Запрос порта приложения
read -p "Введите порт приложения [8080]: " APP_PORT
APP_PORT=${APP_PORT:-8080}

APP_NAME="ruomi"
APP_DIR="/opt/$APP_NAME"

# 1. Установка Nginx
echo
info "Проверка Nginx..."
if command -v nginx &> /dev/null; then
    info "Nginx уже установлен"
else
    info "Установка Nginx..."
    if [ -f /etc/os-release ]; then
        . /etc/os-release
        OS=$ID
        if [ "$OS" = "ubuntu" ] || [ "$OS" = "debian" ]; then
            apt-get update
            apt-get install -y nginx
        elif [ "$OS" = "centos" ] || [ "$OS" = "rhel" ] || [ "$OS" = "fedora" ]; then
            yum install -y nginx
        fi
    fi
    systemctl enable nginx
    systemctl start nginx
    info "Nginx установлен и запущен"
fi

# 2. Создание временной HTTP конфигурации Nginx (без SSL)
echo
info "Создание временной HTTP конфигурации Nginx..."
NGINX_CONFIG="/etc/nginx/sites-available/$DOMAIN"

# Проверяем, существует ли sites-available
if [ ! -d "/etc/nginx/sites-available" ]; then
    mkdir -p /etc/nginx/sites-available
    mkdir -p /etc/nginx/sites-enabled
    # Добавляем в nginx.conf
    if ! grep -q "include /etc/nginx/sites-enabled/\*;" /etc/nginx/nginx.conf; then
        sed -i '/http {/a\    include /etc/nginx/sites-enabled/*;' /etc/nginx/nginx.conf
    fi
fi

# Создаем временную HTTP конфигурацию (для получения SSL)
cat > $NGINX_CONFIG <<EOF
server {
    listen 80;
    listen [::]:80;
    server_name $DOMAIN www.$DOMAIN;
    
    # Для Let's Encrypt
    location /.well-known/acme-challenge/ {
        root /var/www/html;
    }
    
    # Proxy settings
    location / {
        proxy_pass http://localhost:${APP_PORT};
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_cache_bypass \$http_upgrade;
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Logging
    access_log /var/log/nginx/${DOMAIN}_access.log;
    error_log /var/log/nginx/${DOMAIN}_error.log;

    # Max upload size
    client_max_body_size 10M;
}
EOF

# Активируем конфигурацию
if [ -f "/etc/nginx/sites-enabled/$DOMAIN" ]; then
    rm /etc/nginx/sites-enabled/$DOMAIN
fi
ln -s $NGINX_CONFIG /etc/nginx/sites-enabled/

# Удаляем дефолтную конфигурацию, если есть
if [ -f "/etc/nginx/sites-enabled/default" ]; then
    rm /etc/nginx/sites-enabled/default
fi

info "Временная HTTP конфигурация создана"

# 3. Проверка конфигурации
echo
info "Проверка конфигурации Nginx..."
if nginx -t; then
    info "Конфигурация корректна"
    systemctl reload nginx
else
    error "Ошибка в конфигурации Nginx"
    exit 1
fi

# 4. Установка Certbot для SSL
echo
read -p "Установить SSL сертификат (Let's Encrypt)? (y/n) [y]: " INSTALL_SSL
INSTALL_SSL=${INSTALL_SSL:-y}

if [ "$INSTALL_SSL" = "y" ]; then
    info "Установка Certbot..."
    
    if command -v certbot &> /dev/null; then
        info "Certbot уже установлен"
    else
        if [ -f /etc/os-release ]; then
            . /etc/os-release
            OS=$ID
            if [ "$OS" = "ubuntu" ] || [ "$OS" = "debian" ]; then
                apt-get install -y certbot python3-certbot-nginx
            elif [ "$OS" = "centos" ] || [ "$OS" = "rhel" ] || [ "$OS" = "fedora" ]; then
                yum install -y certbot python3-certbot-nginx
            fi
        fi
        info "Certbot установлен"
    fi
    
    info "Получение SSL сертификата..."
    read -p "Введите email для Let's Encrypt (Enter для пропуска): " EMAIL
    
    # Получаем сертификат
    if [ -n "$EMAIL" ]; then
        certbot certonly --nginx -d $DOMAIN -d www.$DOMAIN --email $EMAIL --agree-tos --non-interactive --redirect || {
            warn "Не удалось получить сертификат автоматически"
            warn "Попробуйте вручную: sudo certbot --nginx -d $DOMAIN -d www.$DOMAIN"
            SSL_INSTALLED=false
        }
    else
        certbot certonly --nginx -d $DOMAIN -d www.$DOMAIN --register-unsafely-without-email --agree-tos --non-interactive --redirect || {
            warn "Не удалось получить сертификат автоматически"
            warn "Попробуйте вручную: sudo certbot --nginx -d $DOMAIN -d www.$DOMAIN"
            SSL_INSTALLED=false
        }
    fi
    
    # Проверяем, получен ли сертификат
    if [ -f "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" ]; then
        SSL_INSTALLED=true
        info "SSL сертификат получен успешно"
        
        # Certbot автоматически обновит конфигурацию, но создадим полную версию
        echo
        info "Создание полной HTTPS конфигурации..."
        
        cat > $NGINX_CONFIG <<EOF
# Redirect HTTP to HTTPS
server {
    listen 80;
    listen [::]:80;
    server_name $DOMAIN www.$DOMAIN;
    
    # Для Let's Encrypt
    location /.well-known/acme-challenge/ {
        root /var/www/html;
    }
    
    location / {
        return 301 https://\$server_name\$request_uri;
    }
}

# HTTPS server
server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name $DOMAIN www.$DOMAIN;

    # SSL certificates
    ssl_certificate /etc/letsencrypt/live/$DOMAIN/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/$DOMAIN/privkey.pem;
    
    # SSL configuration
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # Security headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # Logging
    access_log /var/log/nginx/${DOMAIN}_access.log;
    error_log /var/log/nginx/${DOMAIN}_error.log;

    # Max upload size
    client_max_body_size 10M;

    # Proxy settings
    location / {
        proxy_pass http://localhost:${APP_PORT};
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_cache_bypass \$http_upgrade;
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Static files caching
    location ~* \.(jpg|jpeg|png|gif|ico|css|js|svg|woff|woff2|ttf|eot)$ {
        proxy_pass http://localhost:${APP_PORT};
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}
EOF
        
        # Проверяем конфигурацию
        if nginx -t; then
            systemctl reload nginx
            info "HTTPS конфигурация применена"
        else
            error "Ошибка в HTTPS конфигурации"
            warn "Используется HTTP конфигурация"
        fi
    else
        SSL_INSTALLED=false
        warn "SSL сертификат не получен, используется HTTP конфигурация"
    fi
else
    SSL_INSTALLED=false
fi

# 5. Перезагрузка Nginx
echo
info "Перезагрузка Nginx..."
systemctl reload nginx
info "Nginx перезагружен"

# 6. Настройка автообновления сертификатов
if [ "$INSTALL_SSL" = "y" ] && [ "$SSL_INSTALLED" = "true" ]; then
    echo
    info "Настройка автообновления SSL сертификатов..."
    (crontab -l 2>/dev/null | grep -v "certbot renew"; echo "0 3 * * * certbot renew --quiet --post-hook 'systemctl reload nginx'") | crontab -
    info "Автообновление настроено"
fi

# 7. Настройка Firewall
echo
read -p "Настроить Firewall (открыть порты 80 и 443)? (y/n) [y]: " SETUP_FIREWALL
SETUP_FIREWALL=${SETUP_FIREWALL:-y}

if [ "$SETUP_FIREWALL" = "y" ]; then
    if command -v ufw &> /dev/null; then
        info "Настройка UFW..."
        ufw allow 80/tcp
        ufw allow 443/tcp
        ufw allow 22/tcp
        info "UFW настроен"
    elif command -v firewall-cmd &> /dev/null; then
        info "Настройка firewalld..."
        firewall-cmd --permanent --add-service=http
        firewall-cmd --permanent --add-service=https
        firewall-cmd --reload
        info "Firewalld настроен"
    else
        warn "Firewall не найден, настройте вручную"
    fi
fi

echo
echo "========================================"
echo "   Настройка завершена!"
echo "========================================"
echo
echo "Информация:"
echo "  Домен: $DOMAIN"
echo "  Порт приложения: $APP_PORT"
echo "  Nginx конфигурация: $NGINX_CONFIG"
if [ "$SSL_INSTALLED" = "true" ]; then
    echo "  SSL: Установлен (HTTPS)"
    echo "  Доступ: https://$DOMAIN"
else
    echo "  SSL: Не установлен (HTTP)"
    echo "  Доступ: http://$DOMAIN"
    echo
    echo "  Для установки SSL выполните:"
    echo "    sudo certbot --nginx -d $DOMAIN -d www.$DOMAIN"
fi
echo
echo "Важно:"
if [ "$SSL_INSTALLED" != "true" ]; then
    echo "  1. Убедитесь, что DNS записи настроены:"
    echo "     A запись: $DOMAIN -> IP вашего сервера"
    echo "     A запись: www.$DOMAIN -> IP вашего сервера"
    echo
    echo "  2. Проверьте DNS:"
    echo "     dig $DOMAIN"
    echo "     nslookup $DOMAIN"
    echo
    echo "  3. После настройки DNS установите SSL:"
    echo "     sudo certbot --nginx -d $DOMAIN -d www.$DOMAIN"
fi
echo
echo "Команды управления Nginx:"
echo "  Перезагрузка: sudo systemctl reload nginx"
echo "  Статус:       sudo systemctl status nginx"
echo "  Логи:         sudo tail -f /var/log/nginx/${DOMAIN}_error.log"
echo


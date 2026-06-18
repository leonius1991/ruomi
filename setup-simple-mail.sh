#!/bin/bash

# Упрощенная установка почтового сервера для отправки писем

set -e

echo "========================================"
echo "   Упрощенная установка почтового сервера"
echo "   Только для отправки (SMTP)"
echo "========================================"
echo

if [ "$EUID" -ne 0 ]; then 
    echo "ОШИБКА: Запустите скрипт с правами root (sudo)"
    exit 1
fi

DOMAIN="ruomi.fi"
HOSTNAME="mail.ruomi.fi"

info() {
    echo "[INFO] $1"
}

# 1. Установка Postfix
info "Установка Postfix..."
export DEBIAN_FRONTEND=noninteractive
debconf-set-selections <<< "postfix postfix/mailname string $DOMAIN"
debconf-set-selections <<< "postfix postfix/main_mailer_type string 'Internet Site'"

apt update -y
apt install -y postfix mailutils

# 2. Настройка hostname
info "Настройка hostname..."
hostnamectl set-hostname $HOSTNAME
SERVER_IP=$(hostname -I | awk '{print $1}')
echo "$SERVER_IP $HOSTNAME $DOMAIN" >> /etc/hosts

# 3. Настройка Postfix
info "Настройка Postfix..."
cat > /etc/postfix/main.cf <<EOF
# Основные настройки
myhostname = $HOSTNAME
mydomain = $DOMAIN
myorigin = \$mydomain
inet_interfaces = all
inet_protocols = ipv4

# Сети
mynetworks = 127.0.0.0/8 [::ffff:127.0.0.0]/104 [::1]/128

# Получатели
mydestination = \$myhostname, localhost.\$mydomain, localhost, \$mydomain

# Ограничения
message_size_limit = 10485760
mailbox_size_limit = 1073741824

# Безопасность
smtpd_banner = \$myhostname ESMTP
disable_vrfy_command = yes
smtpd_helo_required = yes

# TLS (используем самоподписанный сертификат)
smtpd_tls_cert_file = /etc/ssl/certs/ssl-cert-snakeoil.pem
smtpd_tls_key_file = /etc/ssl/private/ssl-cert-snakeoil.key
smtpd_use_tls = yes
smtpd_tls_security_level = may

# Ограничения
smtpd_client_restrictions = permit_mynetworks, reject_unknown_client_hostname
smtpd_helo_restrictions = permit_mynetworks, reject_invalid_helo_hostname, reject_non_fqdn_helo_hostname
smtpd_sender_restrictions = permit_mynetworks, reject_non_fqdn_sender, reject_unknown_sender_domain
smtpd_recipient_restrictions = permit_mynetworks, reject_non_fqdn_recipient, reject_unknown_recipient_domain, reject_unauth_destination
EOF

# 4. Настройка Submission (порт 587) - для отправки из приложения
info "Настройка Submission порта..."
# Для простоты разрешаем отправку с localhost без аутентификации
# В продакшене лучше использовать аутентификацию
if ! grep -q "^submission" /etc/postfix/master.cf; then
    cat >> /etc/postfix/master.cf <<EOF

# Submission port (587) - для отправки из приложения
submission inet n       -       y       -       -       smtpd
  -o syslog_name=postfix/submission
  -o smtpd_tls_security_level=may
  -o smtpd_reject_unlisted_recipient=no
  -o smtpd_client_restrictions=permit_mynetworks,reject
  -o smtpd_helo_restrictions=permit_mynetworks,reject_invalid_helo_hostname,reject_non_fqdn_helo_hostname
  -o smtpd_sender_restrictions=permit_mynetworks,reject_non_fqdn_sender,reject_unknown_sender_domain
  -o smtpd_recipient_restrictions=permit_mynetworks,reject_non_fqdn_recipient,reject_unknown_recipient_domain,reject_unauth_destination
EOF
fi

# 5. Создание почтового ящика
info "Создание почтового ящика noreply..."
if [ -z "$MAIL_PASSWORD" ]; then
    read -p "Введите пароль для noreply@ruomi.fi: " -s MAIL_PASSWORD
    echo
fi

if ! id "noreply" &>/dev/null; then
    useradd -m -s /bin/bash noreply
    echo "noreply:$MAIL_PASSWORD" | chpasswd
    info "Почтовый ящик noreply создан"
else
    if [ -n "$MAIL_PASSWORD" ]; then
        echo "noreply:$MAIL_PASSWORD" | chpasswd
        info "Пароль для noreply обновлен"
    fi
fi

# 6. Настройка алиаса
info "Настройка алиаса..."
if ! grep -q "noreply@ruomi.fi" /etc/aliases; then
    echo "noreply@ruomi.fi: noreply" >> /etc/aliases
    newaliases
fi

# 7. Запуск Postfix
info "Запуск Postfix..."
systemctl enable postfix
systemctl restart postfix

# 8. Настройка файрвола
info "Настройка файрвола..."
if command -v ufw &> /dev/null; then
    ufw allow 25/tcp   # SMTP
    ufw allow 587/tcp  # Submission
    info "Порты открыты в UFW"
elif command -v firewall-cmd &> /dev/null; then
    firewall-cmd --permanent --add-port=25/tcp
    firewall-cmd --permanent --add-port=587/tcp
    firewall-cmd --reload
    info "Порты открыты в firewalld"
fi

# 9. Вывод информации
echo
echo "========================================"
echo "   Установка завершена!"
echo "========================================"
echo
echo "Сервер IP: $SERVER_IP"
echo "Hostname: $HOSTNAME"
echo
echo "ВАЖНО: Настройте DNS записи!"
echo
echo "1. A запись для mail.ruomi.fi:"
echo "   mail.ruomi.fi.    A    $SERVER_IP"
echo
echo "2. MX запись:"
echo "   ruomi.fi.    MX    10 mail.ruomi.fi."
echo
echo "3. SPF запись:"
echo "   ruomi.fi.    TXT   \"v=spf1 mx a:mail.ruomi.fi ~all\""
echo
echo "4. PTR запись (обратная DNS):"
echo "   Попросите у провайдера настроить:"
echo "   $SERVER_IP → mail.ruomi.fi"
echo
echo "Настройка в application.properties:"
echo "  spring.mail.host=mail.ruomi.fi"
echo "  spring.mail.port=587"
echo "  spring.mail.username=noreply@ruomi.fi"
echo "  spring.mail.password=$MAIL_PASSWORD"
echo
echo "Тестирование:"
echo "  echo 'Test' | mail -s 'Test' your-email@gmail.com"
echo


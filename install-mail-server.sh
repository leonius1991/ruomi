#!/bin/bash

# Скрипт установки почтового сервера (Postfix + Dovecot) для ruomi.fi

set -e

echo "========================================"
echo "   Установка почтового сервера"
echo "   для домена ruomi.fi"
echo "========================================"
echo

# Проверка прав root
if [ "$EUID" -ne 0 ]; then 
    echo "ОШИБКА: Запустите скрипт с правами root (sudo)"
    exit 1
fi

# Функции для вывода
info() {
    echo "[INFO] $1"
}

warn() {
    echo "[WARN] $1"
}

error() {
    echo "[ERROR] $1"
    exit 1
}

# Переменные
DOMAIN="ruomi.fi"
HOSTNAME="mail.ruomi.fi"
MAIL_USER="noreply"
MAIL_PASSWORD=""

# 1. Проверка системы
info "Проверка системы..."
if [ -f /etc/debian_version ]; then
    OS="debian"
    PKG_MANAGER="apt"
elif [ -f /etc/redhat-release ]; then
    OS="redhat"
    PKG_MANAGER="yum"
else
    error "Неподдерживаемая ОС. Поддерживаются Debian/Ubuntu и CentOS/RHEL"
fi

info "ОС: $OS"

# 2. Обновление системы
info "Обновление системы..."
$PKG_MANAGER update -y
$PKG_MANAGER install -y postfix dovecot-core dovecot-imapd dovecot-pop3d opendkim opendkim-tools

# 3. Настройка hostname
info "Настройка hostname..."
hostnamectl set-hostname $HOSTNAME
echo "$(hostname -I | awk '{print $1}') $HOSTNAME $DOMAIN" >> /etc/hosts

# 4. Настройка Postfix
info "Настройка Postfix..."

# Создаем резервную копию
cp /etc/postfix/main.cf /etc/postfix/main.cf.backup

# Базовая конфигурация Postfix
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

# TLS
smtpd_tls_cert_file = /etc/ssl/certs/ssl-cert-snakeoil.pem
smtpd_tls_key_file = /etc/ssl/private/ssl-cert-snakeoil.key
smtpd_use_tls = yes
smtpd_tls_auth_only = yes
smtpd_tls_security_level = may

# Ограничения для аутентификации
smtpd_sasl_type = dovecot
smtpd_sasl_path = private/auth
smtpd_sasl_auth_enable = yes
smtpd_sasl_security_options = noanonymous
smtpd_sasl_local_domain = \$myhostname

# Ограничения подключений
smtpd_client_restrictions = permit_mynetworks, permit_sasl_authenticated, reject_unknown_client_hostname
smtpd_helo_restrictions = permit_mynetworks, permit_sasl_authenticated, reject_invalid_helo_hostname, reject_non_fqdn_helo_hostname
smtpd_sender_restrictions = permit_mynetworks, permit_sasl_authenticated, reject_non_fqdn_sender, reject_unknown_sender_domain
smtpd_recipient_restrictions = permit_mynetworks, permit_sasl_authenticated, reject_non_fqdn_recipient, reject_unknown_recipient_domain, reject_unauth_destination

# Виртуальные ящики
virtual_mailbox_domains = mysql:/etc/postfix/mysql-virtual-mailbox-domains.cf
virtual_mailbox_maps = mysql:/etc/postfix/mysql-virtual-mailbox-maps.cf
virtual_alias_maps = mysql:/etc/postfix/mysql-virtual-alias-maps.cf
virtual_minimum_uid = 100
virtual_uid_maps = static:5000
virtual_gid_maps = static:5000
virtual_mailbox_base = /var/mail/vhosts

# OpenDKIM
milter_default_action = accept
milter_protocol = 2
smtpd_milters = inet:localhost:8891
non_smtpd_milters = inet:localhost:8891
EOF

# 5. Настройка master.cf для SASL
info "Настройка SASL..."
if ! grep -q "submission" /etc/postfix/master.cf; then
    cat >> /etc/postfix/master.cf <<EOF

# Submission port (587)
submission inet n       -       y       -       -       smtpd
  -o syslog_name=postfix/submission
  -o smtpd_tls_security_level=encrypt
  -o smtpd_sasl_auth_enable=yes
  -o smtpd_tls_auth_only=yes
  -o smtpd_reject_unlisted_recipient=no
  -o smtpd_client_restrictions=\$mua_client_restrictions
  -o smtpd_helo_restrictions=\$mua_helo_restrictions
  -o smtpd_sender_restrictions=\$mua_sender_restrictions
  -o smtpd_recipient_restrictions=permit_sasl_authenticated,reject
  -o milter_macro_daemon_name=ORIGINATING

# SMTPS port (465)
smtps     inet  n       -       y       -       -       smtpd
  -o syslog_name=postfix/smtps
  -o smtpd_tls_wrappermode=yes
  -o smtpd_sasl_auth_enable=yes
  -o smtpd_reject_unlisted_recipient=no
  -o smtpd_client_restrictions=\$mua_client_restrictions
  -o smtpd_helo_restrictions=\$mua_helo_restrictions
  -o smtpd_sender_restrictions=\$mua_sender_restrictions
  -o smtpd_recipient_restrictions=permit_sasl_authenticated,reject
  -o milter_macro_daemon_name=ORIGINATING
EOF
fi

# 6. Создание пользователя для почты
info "Создание пользователя для почты..."
if ! id "vmail" &>/dev/null; then
    useradd -r -u 5000 -g mail -d /var/mail/vhosts -s /sbin/nologin -c "Virtual maildir" vmail
    mkdir -p /var/mail/vhosts/$DOMAIN
    chown -R vmail:mail /var/mail/vhosts
fi

# 6.1. Создание системного пользователя noreply
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

# Настройка алиаса
if ! grep -q "noreply@ruomi.fi" /etc/aliases 2>/dev/null; then
    echo "noreply@ruomi.fi: noreply" >> /etc/aliases
    newaliases 2>/dev/null || true
fi

# 7. Настройка Dovecot
info "Настройка Dovecot..."

# Основная конфигурация
cat > /etc/dovecot/dovecot.conf <<EOF
protocols = imap pop3
listen = *
mail_location = maildir:/var/mail/vhosts/%d/%n
mail_privileged_group = mail
userdb {
    driver = static
    args = uid=vmail gid=mail home=/var/mail/vhosts/%d/%n
}
passdb {
    driver = static
    args = password=changeme
}
EOF

# 10. Настройка Postfix для SASL
info "Настройка Postfix для работы с Dovecot..."
mkdir -p /var/spool/postfix/private
chmod 755 /var/spool/postfix/private
chown root:postfix /var/spool/postfix/private

# Обновляем master.cf для auth socket
sed -i '/^#submission/,$ { /^submission/,/^$/ s/^#// }' /etc/postfix/master.cf || true

# 11. Настройка OpenDKIM
info "Настройка OpenDKIM..."

# Создаем ключи
mkdir -p /etc/opendkim/keys/$DOMAIN
opendkim-genkey -D /etc/opendkim/keys/$DOMAIN -d $DOMAIN -s default
chown -R opendkim:opendkim /etc/opendkim/keys

# Конфигурация OpenDKIM
cat > /etc/opendkim.conf <<EOF
Domain                  $DOMAIN
KeyFile                 /etc/opendkim/keys/$DOMAIN/default.private
Selector                default
Socket                  inet:8891@localhost
PidFile                 /var/run/opendkim/opendkim.pid
UMask                   022
UserID                  opendkim:opendkim
EOF

# 12. Запуск сервисов
info "Запуск сервисов..."
systemctl enable postfix dovecot opendkim
systemctl restart postfix dovecot opendkim

# 13. Настройка файрвола
info "Настройка файрвола..."
if command -v ufw &> /dev/null; then
    ufw allow 25/tcp   # SMTP
    ufw allow 587/tcp  # Submission
    ufw allow 465/tcp  # SMTPS
    ufw allow 143/tcp  # IMAP
    ufw allow 993/tcp  # IMAPS
    ufw allow 110/tcp  # POP3
    ufw allow 995/tcp  # POP3S
    info "Порты открыты в UFW"
elif command -v firewall-cmd &> /dev/null; then
    firewall-cmd --permanent --add-port=25/tcp
    firewall-cmd --permanent --add-port=587/tcp
    firewall-cmd --permanent --add-port=465/tcp
    firewall-cmd --permanent --add-port=143/tcp
    firewall-cmd --permanent --add-port=993/tcp
    firewall-cmd --permanent --add-port=110/tcp
    firewall-cmd --permanent --add-port=995/tcp
    firewall-cmd --reload
    info "Порты открыты в firewalld"
fi

# 14. Вывод информации
echo
echo "========================================"
echo "   Установка завершена!"
echo "========================================"
echo
echo "ВАЖНО: Настройте DNS записи!"
echo
echo "1. MX запись:"
echo "   ruomi.fi.    MX    10 mail.ruomi.fi."
echo
echo "2. A запись для mail.ruomi.fi:"
echo "   mail.ruomi.fi.    A    $(hostname -I | awk '{print $1}')"
echo
echo "3. SPF запись:"
echo "   ruomi.fi.    TXT   \"v=spf1 mx a:mail.ruomi.fi ~all\""
echo
echo "4. DKIM запись:"
echo "   Получите ключ командой:"
echo "   cat /etc/opendkim/keys/$DOMAIN/default.txt"
echo "   Скопируйте значение БЕЗ внешних кавычек!"
echo "   Формат: default._domainkey.ruomi.fi.    TXT    v=DKIM1; h=sha256; k=rsa; p=..."
echo "   ВАЖНО: Если DNS провайдер требует кавычки, используйте ОДИНАРНЫЕ или уберите их совсем"
echo
echo "5. DMARC запись (опционально):"
echo "   _dmarc.ruomi.fi.    TXT   \"v=DMARC1; p=none; rua=mailto:postmaster@ruomi.fi\""
echo
echo "Создайте почтовый ящик:"
echo "  sudo useradd -m -s /bin/bash $MAIL_USER"
echo "  sudo passwd $MAIL_USER"
echo
echo "Или используйте простую конфигурацию без MySQL (см. инструкцию)"
echo


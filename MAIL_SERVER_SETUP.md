# 📧 Установка собственного почтового сервера на Linux

## ⚠️ Важные предупреждения

**Собственный почтовый сервер - это сложно!**

### Проблемы, с которыми вы столкнетесь:

- ❌ **Высокая вероятность попадания в спам** (особенно в начале)
- ❌ **Сложная настройка** DNS записей (SPF, DKIM, DMARC, PTR)
- ❌ **Нужен статический IP** с хорошей репутацией
- ❌ **Требуется регулярное обслуживание** и мониторинг
- ❌ **Проблемы с репутацией IP** - нужно "прогревать" постепенно
- ❌ **Многие провайдеры блокируют порт 25** для новых серверов
- ❌ **Сложная диагностика** проблем с доставкой

### ⭐ Рекомендация

**Для 99% случаев используйте готовые сервисы:**
- **Yandex.Connect** - бесплатно, просто, надежно
- **SendGrid** - надежно, аналитика, масштабируемо
- **Mailgun** - хорошая доставка, API

**Но если вы все же хотите свой сервер**, вот подробная инструкция.

## 🎯 Что будет установлено

- **Postfix** - SMTP сервер для отправки почты
- **Dovecot** - IMAP/POP3 сервер для получения почты
- **OpenDKIM** - для подписи писем (защита от спама)

## 📋 Требования

1. **Статический IP адрес** (обязательно!)
2. **Доступ к DNS** для настройки записей
3. **Открытые порты**: 25, 587, 465, 143, 993
4. **Минимум 2GB RAM**
5. **Ubuntu/Debian** или **CentOS/RHEL**

## 🚀 Установка

### Вариант A: Упрощенная установка (рекомендуется для начала)

Для быстрого старта используйте упрощенный скрипт:

```bash
# Скачайте скрипт на сервер
nano setup-simple-mail.sh
# Скопируйте содержимое из файла setup-simple-mail.sh

chmod +x setup-simple-mail.sh
sudo ./setup-simple-mail.sh
```

Этот скрипт установит только Postfix для отправки писем.

### Вариант B: Полная установка

Для полной функциональности (IMAP, POP3, DKIM):

```bash
# Скачайте скрипт на сервер
nano install-mail-server.sh
# Скопируйте содержимое из файла install-mail-server.sh

chmod +x install-mail-server.sh
sudo ./install-mail-server.sh
```

**Рекомендация:** Начните с упрощенной установки, затем при необходимости переходите к полной.

### Шаг 2: Настройка DNS записей

**КРИТИЧЕСКИ ВАЖНО!** Без правильных DNS записей письма будут попадать в спам.

#### 2.1. A запись для mail.ruomi.fi

```
mail.ruomi.fi.    A    YOUR_SERVER_IP
```

#### 2.2. MX запись

```
ruomi.fi.    MX    10 mail.ruomi.fi.
```

#### 2.3. SPF запись

```
ruomi.fi.    TXT   "v=spf1 mx a:mail.ruomi.fi ~all"
```

#### 2.4. DKIM запись

Получите DKIM ключ:
```bash
cat /etc/opendkim/keys/ruomi.fi/default.txt
```

Добавьте в DNS:
```
default._domainkey.ruomi.fi.    TXT   "v=DKIM1; k=rsa; p=..."
```

#### 2.5. DMARC запись (опционально)

```
_dmarc.ruomi.fi.    TXT   "v=DMARC1; p=none; rua=mailto:postmaster@ruomi.fi"
```

#### 2.6. PTR запись (обратная DNS)

**КРИТИЧЕСКИ ВАЖНО!** Без PTR записи многие почтовые серверы будут отклонять ваши письма!

**Где настраивать:**
PTR запись настраивается у **вашего хостинг-провайдера** (VPS провайдера), который предоставил вам сервер, **НЕ у DNS провайдера домена**.

**Как узнать провайдера:**
```bash
# Узнайте IP адрес
hostname -I

# Определите провайдера
whois YOUR_SERVER_IP
```

**Где настраивать:**
1. В панели управления VPS провайдера (раздел "Reverse DNS" или "PTR Records")
2. Или через техподдержку провайдера

**Что нужно:**
```
IP адрес: YOUR_SERVER_IP
Hostname: mail.ruomi.fi
```

**Проверка:**
```bash
dig -x YOUR_SERVER_IP
# Должно вернуть: mail.ruomi.fi
```

**См. подробную инструкцию:** `PTR_DNS_EXPLAINED.md`

### Шаг 3: Создание почтового ящика

#### Вариант A: Простой системный пользователь (для начала)

```bash
# Создаем пользователя
sudo useradd -m -s /bin/bash noreply
sudo passwd noreply

# Создаем директорию для почты
sudo mkdir -p /var/mail/vhosts/ruomi.fi/noreply
sudo chown -R noreply:noreply /var/mail/vhosts/ruomi.fi/noreply
```

#### Вариант B: Виртуальные ящики (рекомендуется)

Нужно настроить MySQL и виртуальные ящики (см. расширенную инструкцию).

### Шаг 4: Настройка в application.properties

```bash
sudo nano /opt/ruomi/application.properties
```

Добавьте или обновите:

```properties
# Email Configuration для собственного сервера
spring.mail.host=mail.ruomi.fi
spring.mail.port=587
spring.mail.username=noreply@ruomi.fi
spring.mail.password=ваш_пароль_от_ящика
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000
```

**Важно:** Используйте пароль, который вы ввели при создании ящика noreply.

### Шаг 5: Перезапуск приложения

```bash
sudo systemctl restart ruomi
```

### Шаг 6: Проверка работы

```bash
# Проверьте логи
sudo journalctl -u ruomi -f | grep -i mail

# Отправьте тестовое письмо
echo "Test message" | mail -s "Test from ruomi.fi" your-email@gmail.com
```

## 🔧 Расширенная настройка

### Настройка виртуальных ящиков (MySQL)

Если нужно несколько ящиков, лучше использовать виртуальные ящики:

1. Установите MySQL (если еще не установлен)
2. Создайте базу данных для почты
3. Настройте Postfix для работы с MySQL
4. Создайте таблицы для виртуальных ящиков

Подробная инструкция в разделе "Виртуальные ящики" ниже.

### Настройка SSL сертификатов

Для безопасности используйте Let's Encrypt:

```bash
sudo apt install certbot
sudo certbot certonly --standalone -d mail.ruomi.fi

# Обновите /etc/postfix/main.cf:
smtpd_tls_cert_file = /etc/letsencrypt/live/mail.ruomi.fi/fullchain.pem
smtpd_tls_key_file = /etc/letsencrypt/live/mail.ruomi.fi/privkey.pem
```

## 🧪 Тестирование

### Тест 1: Проверка портов

```bash
# SMTP
telnet mail.ruomi.fi 25

# Submission
telnet mail.ruomi.fi 587

# IMAP
telnet mail.ruomi.fi 143
```

### Тест 2: Отправка тестового письма

```bash
# С сервера
echo "Test message" | mail -s "Test" your-email@gmail.com
```

### Тест 3: Проверка DNS

```bash
# MX записи
dig MX ruomi.fi

# SPF
dig TXT ruomi.fi | grep spf

# DKIM
dig TXT default._domainkey.ruomi.fi

# PTR (обратная DNS)
dig -x YOUR_SERVER_IP
```

### Тест 4: Проверка репутации IP

Проверьте ваш IP на:
- https://mxtoolbox.com/blacklists.aspx
- https://www.spamhaus.org/lookup/

Если IP в черных списках, письма будут попадать в спам!

## ⚠️ Проблемы и решения

### Проблема: Письма попадают в спам

**Причины:**
- IP в черных списках
- Нет PTR записи
- Неправильные SPF/DKIM записи
- Нет репутации IP

**Решение:**
1. Проверьте IP на blacklists
2. Настройте PTR запись
3. Проверьте SPF/DKIM
4. Используйте сервисы для "прогрева" IP (постепенное увеличение объема)

### Проблема: Письма не отправляются

**Причины:**
- Порт 25 заблокирован провайдером
- Неправильные настройки Postfix
- Проблемы с аутентификацией

**Решение:**
1. Проверьте логи: `sudo journalctl -u postfix -f`
2. Проверьте доступность порта: `telnet mail.ruomi.fi 587`
3. Проверьте настройки в `/etc/postfix/main.cf`

### Проблема: "Connection refused"

**Причины:**
- Файрвол блокирует порты
- Postfix не запущен
- Неправильный hostname

**Решение:**
```bash
# Проверьте статус
sudo systemctl status postfix

# Проверьте файрвол
sudo ufw status

# Проверьте hostname
hostname
```

## 🔒 Безопасность

### Обязательные меры:

1. **Используйте сильные пароли** для почтовых ящиков
2. **Настройте fail2ban** для защиты от брутфорса
3. **Используйте SSL/TLS** для всех соединений
4. **Регулярно обновляйте** систему
5. **Мониторьте логи** на подозрительную активность

### Установка fail2ban

```bash
sudo apt install fail2ban
sudo systemctl enable fail2ban
sudo systemctl start fail2ban
```

## 📊 Мониторинг

### Просмотр логов

```bash
# Логи Postfix
sudo tail -f /var/log/mail.log

# Логи отправки
sudo grep "sent" /var/log/mail.log

# Логи ошибок
sudo grep "error" /var/log/mail.log
```

### Статистика

```bash
# Количество отправленных писем
sudo grep "status=sent" /var/log/mail.log | wc -l

# Количество отклоненных
sudo grep "status=bounced" /var/log/mail.log | wc -l
```

## 🎯 Рекомендации

### Для продакшена:

1. **Используйте готовые сервисы** (Yandex.Connect, SendGrid)
2. Если нужен свой сервер - наймите специалиста
3. Настройте мониторинг и алерты
4. Регулярно проверяйте репутацию IP
5. Используйте сервисы для прогрева IP

### Альтернатива: Гибридный подход

- **Отправка**: Готовый сервис (SendGrid, Mailgun)
- **Получение**: Собственный сервер (если нужно)

Это дает:
- ✅ Надежную доставку
- ✅ Контроль над получением
- ✅ Меньше проблем со спамом

## 📝 Быстрая настройка для тестирования (localhost)

Если нужно быстро протестировать на localhost:

```bash
# 1. Установите только Postfix
sudo apt install postfix mailutils

# 2. Выберите "Internet Site" при установке
# 3. Укажите домен: ruomi.fi

# 4. Создайте ящик
sudo useradd -m noreply
sudo passwd noreply

# 5. Настройте в application.properties
spring.mail.host=localhost
spring.mail.port=25
spring.mail.username=noreply
spring.mail.password=пароль
spring.mail.properties.mail.smtp.auth=false
```

**⚠️ Это только для тестирования на localhost!** 

Для продакшена:
- Используйте скрипт `setup-simple-mail.sh` или `install-mail-server.sh`
- Настройте DNS записи
- Настройте PTR запись
- Используйте порт 587 с аутентификацией

## ✅ Чеклист

- [ ] Статический IP адрес
- [ ] Установлен Postfix и Dovecot
- [ ] Настроены DNS записи (MX, A, SPF, DKIM)
- [ ] Настроена PTR запись (обратная DNS)
- [ ] Создан почтовый ящик noreply@ruomi.fi
- [ ] Открыты порты в файрволе
- [ ] Настроен SSL/TLS
- [ ] Протестирована отправка писем
- [ ] Проверена репутация IP
- [ ] Настроен fail2ban
- [ ] Настроен мониторинг

## 🆘 Если что-то пошло не так

1. Проверьте логи: `sudo journalctl -u postfix -f`
2. Проверьте DNS: `dig MX ruomi.fi`
3. Проверьте порты: `sudo netstat -tlnp | grep -E '25|587|465'`
4. Проверьте конфигурацию: `sudo postfix check`
5. Перезапустите сервисы: `sudo systemctl restart postfix dovecot`

## 💡 Финальная рекомендация

### ⭐ Лучший вариант: Готовый сервис

**Для 99% случаев используйте готовые сервисы:**
- **Yandex.Connect** - бесплатно, просто, надежно
- **SendGrid** - надежно, аналитика, масштабируемо
- **Mailgun** - хорошая доставка, API

**Преимущества:**
- ✅ Работает сразу
- ✅ Не нужно настраивать DNS
- ✅ Не попадает в спам
- ✅ Не требует обслуживания

### Собственный сервер имеет смысл только если:

- ✅ Нужен полный контроль над данными
- ✅ Есть опыт администрирования почтовых серверов
- ✅ Готовы тратить время на обслуживание
- ✅ Есть статический IP с хорошей репутацией
- ✅ Можете настроить все DNS записи правильно

### Компромисс: Гибридный подход

- **Отправка**: Готовый сервис (SendGrid, Mailgun)
- **Получение**: Собственный сервер (если нужно)

Это дает надежность отправки + контроль получения.

## 📚 Дополнительные ресурсы

- **Mail-in-a-Box**: https://mailinabox.email/ (автоматическая установка)
- **iRedMail**: https://www.iredmail.org/ (полнофункциональный сервер)
- **Poste.io**: https://poste.io/ (Docker-решение)
- **Проверка DNS**: https://mxtoolbox.com/
- **Проверка blacklists**: https://www.spamhaus.org/lookup/


# 🚀 Быстрая установка почтового сервера

## ⚡ Упрощенная установка (только отправка)

Для быстрого старта используйте упрощенный скрипт:

```bash
# 1. Скачайте скрипт на сервер
nano setup-simple-mail.sh
# Скопируйте содержимое из файла setup-simple-mail.sh

# 2. Запустите
chmod +x setup-simple-mail.sh
sudo ./setup-simple-mail.sh
```

Скрипт установит:
- ✅ Postfix (SMTP сервер)
- ✅ Создаст ящик noreply@ruomi.fi
- ✅ Настроит порты 25 и 587
- ✅ Откроет порты в файрволе

## 📝 После установки

### 1. Настройте DNS записи

**КРИТИЧЕСКИ ВАЖНО!** Без правильных DNS записей письма будут попадать в спам или вообще не доставляться.

#### A запись:
```
mail.ruomi.fi.    A    YOUR_SERVER_IP
```

#### MX запись:
```
ruomi.fi.    MX    10 mail.ruomi.fi.
```

#### SPF запись:
```
ruomi.fi.    TXT   "v=spf1 mx a:mail.ruomi.fi ~all"
```

**Примечание:** Некоторые DNS провайдеры требуют кавычки, другие добавляют автоматически. Если ошибка - попробуйте без кавычек.

#### PTR запись (обратная DNS):
**ВАЖНО:** Настраивается у **вашего хостинг-провайдера** (VPS провайдера), а НЕ у DNS провайдера!

**Как узнать провайдера:**
```bash
# Узнайте IP
hostname -I

# Определите провайдера
whois YOUR_SERVER_IP
```

**Где настраивать:**
- В панели управления вашего VPS провайдера (DigitalOcean, Hetzner, Vultr и т.д.)
- Или через техподдержку провайдера

**Что просить:**
```
Настроить обратную DNS (PTR запись):
IP адрес: YOUR_SERVER_IP
Hostname: mail.ruomi.fi
```

**Проверка:**
```bash
dig -x YOUR_SERVER_IP
# Должно вернуть: mail.ruomi.fi
```

**См. подробности:** `PTR_DNS_EXPLAINED.md`

### 2. Настройте в application.properties

```bash
sudo nano /opt/ruomi/application.properties
```

Добавьте:
```properties
# Email Configuration для собственного сервера
# Вариант 1: С localhost (если приложение на том же сервере)
spring.mail.host=localhost
spring.mail.port=25
spring.mail.username=
spring.mail.password=
spring.mail.properties.mail.smtp.auth=false

# Вариант 2: Через mail.ruomi.fi (нужна настройка аутентификации)
spring.mail.host=mail.ruomi.fi
spring.mail.port=587
spring.mail.username=noreply@ruomi.fi
spring.mail.password=ваш_пароль_от_ящика
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
```

**Рекомендация:** Для начала используйте Вариант 1 (localhost:25), это проще.

### 3. Перезапустите приложение

```bash
sudo systemctl restart ruomi
```

### 4. Протестируйте

```bash
# Отправьте тестовое письмо
echo "Test message" | mail -s "Test from ruomi.fi" your-email@gmail.com

# Проверьте логи
sudo journalctl -u ruomi -f | grep -i mail
```

## ⚠️ Важно!

1. **PTR запись обязательна!** Без нее письма будут попадать в спам
2. **Проверьте IP на blacklists** перед использованием
3. **Начните с малых объемов** для "прогрева" IP
4. **Мониторьте логи** на ошибки

## 🔧 Полная установка

Если нужна полная функциональность (IMAP, POP3, виртуальные ящики):

```bash
chmod +x install-mail-server.sh
sudo ./install-mail-server.sh
```

См. подробную инструкцию: `MAIL_SERVER_SETUP.md`


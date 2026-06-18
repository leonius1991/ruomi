# 📧 Настройка почты от собственного домена (noreply@ruomi.fi)

## 🎯 Варианты настройки

### ⭐ Вариант 1: Почтовый сервис с доменом (РЕКОМЕНДУЕТСЯ) ⭐

**Это самый простой и надежный способ!**

**Преимущества:**
- ✅ Не нужно настраивать собственный почтовый сервер
- ✅ Надежность и доставка писем
- ✅ Готовые настройки SPF/DKIM
- ✅ Простая настройка

**Провайдеры:**
1. **Yandex.Connect** (бесплатно до 1000 писем/день)
2. **Mail.ru для бизнеса** (бесплатно)
3. **Google Workspace** (платно, от $6/мес)
4. **Zoho Mail** (бесплатно до 5 пользователей)
5. **SendGrid** (бесплатно до 100 писем/день)
6. **Mailgun** (бесплатно до 5000 писем/мес)

### Вариант 2: Собственный почтовый сервер на Linux

**⚠️ Сложно, но возможно**

**Преимущества:**
- ✅ Полный контроль
- ✅ Нет лимитов на отправку
- ✅ Своя инфраструктура

**Недостатки:**
- ❌ Сложная настройка
- ❌ Высокий риск попадания в спам
- ❌ Требует обслуживания
- ❌ Нужен статический IP
- ❌ Нужна настройка DNS (SPF, DKIM, DMARC, PTR)

**См. подробную инструкцию:** `MAIL_SERVER_SETUP.md`

## 🚀 Рекомендуемое решение: Yandex.Connect

### Почему Yandex.Connect?

1. **Бесплатно** до 1000 писем в день
2. **Простая настройка** DNS записей
3. **Хорошая доставка** писем
4. **Русскоязычная поддержка**
5. **Готовые настройки** для Spring Boot

### Шаг 1: Регистрация в Yandex.Connect

1. Перейдите на https://connect.yandex.ru
2. Зарегистрируйтесь или войдите
3. Добавьте домен `ruomi.fi`
4. Подтвердите владение доменом (через DNS записи)

### Шаг 2: Настройка DNS записей

Добавьте в DNS вашего домена (у регистратора):

```
# MX записи (для получения почты)
ruomi.fi.    MX    10 mx.yandex.ru.

# SPF запись (для отправки)
ruomi.fi.    TXT   "v=spf1 redirect=_spf.yandex.ru"

# DKIM запись (Yandex предоставит)
mail._domainkey.ruomi.fi.  TXT  "v=DKIM1; k=rsa; p=..."

# DMARC запись (опционально)
_dmarc.ruomi.fi.  TXT  "v=DMARC1; p=none; rua=mailto:postmaster@ruomi.fi"
```

### Шаг 3: Создание почтового ящика

1. В Yandex.Connect создайте ящик `noreply@ruomi.fi`
2. Установите пароль для этого ящика
3. Включите SMTP доступ

### Шаг 4: Настройка в application.properties

```properties
# Email Configuration для Yandex
spring.mail.host=smtp.yandex.ru
spring.mail.port=465
spring.mail.username=noreply@ruomi.fi
spring.mail.password=your-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.ssl.enable=true
spring.mail.properties.mail.smtp.ssl.trust=smtp.yandex.ru
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000
```

## 📧 Альтернатива: Zoho Mail (бесплатно)

### Настройка Zoho Mail

1. Регистрация: https://www.zoho.com/mail/
2. Добавьте домен `ruomi.fi`
3. Подтвердите через DNS
4. Создайте ящик `noreply@ruomi.fi`

**Настройки для Zoho:**
```properties
spring.mail.host=smtp.zoho.com
spring.mail.port=465
spring.mail.username=noreply@ruomi.fi
spring.mail.password=your-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.ssl.enable=true
spring.mail.properties.mail.smtp.ssl.trust=smtp.zoho.com
```

## 🔄 Альтернатива: SendGrid (для транзакционных писем)

### Настройка SendGrid

1. Регистрация: https://sendgrid.com
2. Создайте API ключ
3. Настройте домен (опционально)

**Настройки для SendGrid:**
```properties
spring.mail.host=smtp.sendgrid.net
spring.mail.port=587
spring.mail.username=apikey
spring.mail.password=your-sendgrid-api-key
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

## ⚖️ Сравнение вариантов

| Провайдер | Цена | Лимит | Сложность | Доставка |
|-----------|------|-------|-----------|----------|
| **Yandex.Connect** | Бесплатно | 1000/день | ⭐⭐ Легко | ⭐⭐⭐⭐ Отлично |
| **Zoho Mail** | Бесплатно | 5 ящиков | ⭐⭐ Легко | ⭐⭐⭐ Хорошо |
| **SendGrid** | Бесплатно | 100/день | ⭐⭐⭐ Средне | ⭐⭐⭐⭐⭐ Отлично |
| **Mail.ru** | Бесплатно | 1000/день | ⭐⭐ Легко | ⭐⭐⭐ Хорошо |
| **Google Workspace** | $6/мес | Безлимит | ⭐⭐ Легко | ⭐⭐⭐⭐⭐ Отлично |

## 🎯 Рекомендация

### ⭐ Лучший вариант для старта: Yandex.Connect

**Почему:**
- ✅ Бесплатно до 1000 писем/день
- ✅ Простая настройка DNS
- ✅ Хорошая доставка в России и Финляндии
- ✅ Русскоязычная поддержка
- ✅ Готовые настройки для Spring Boot

**Настройка займет ~30 минут:**
1. Регистрация в Yandex.Connect (5 мин)
2. Добавление домена (5 мин)
3. Настройка DNS записей (10 мин)
4. Создание ящика noreply@ruomi.fi (5 мин)
5. Настройка в application.properties (5 мин)

### Для масштабирования

**SendGrid** - если нужно больше писем:
- Бесплатно до 100 писем/день
- Платно от $19.95/мес (50,000 писем)
- Отличная доставка
- Аналитика и статистика

**Google Workspace** - если нужна полноценная почта:
- От $6/мес за пользователя
- Безлимитные письма
- Профессиональный вид
- Интеграция с другими сервисами Google

## 🔄 Быстрая настройка (пошагово)

### 1. Выберите провайдера

**Рекомендуем: Yandex.Connect** (бесплатно, просто)

### 2. Зарегистрируйтесь и настройте домен

Следуйте инструкциям выше для выбранного провайдера.

### 3. Обновите application.properties

```bash
sudo nano /opt/ruomi/application.properties
```

### 4. Перезапустите приложение

```bash
sudo systemctl restart ruomi
```

### 5. Проверьте работу

```bash
# Проверьте логи
sudo journalctl -u ruomi -f | grep -i mail

# Зарегистрируйте тестового пользователя
# Проверьте почту
```

## ✅ Проверка работы

1. Зарегистрируйте тестового пользователя
2. Проверьте почту на письмо
3. Проверьте, что отправитель: `noreply@ruomi.fi`
4. Убедитесь, что письмо не попало в спам

## 🔒 Безопасность

1. **Используйте сильный пароль** для почтового ящика
2. **Не храните пароль в коде** - используйте переменные окружения
3. **Настройте SPF/DKIM** для защиты от подделки
4. **Мониторьте отправку** писем

## 📞 Поддержка

### Если письма не отправляются:

1. **Проверьте DNS записи:**
   ```bash
   # Проверка MX записей
   dig MX ruomi.fi
   
   # Проверка SPF
   dig TXT ruomi.fi | grep spf
   
   # Онлайн проверка
   https://mxtoolbox.com/
   ```

2. **Проверьте логи приложения:**
   ```bash
   sudo journalctl -u ruomi -n 100 | grep -i mail
   ```

3. **Проверьте настройки SMTP:**
   ```bash
   # Проверьте файл конфигурации
   grep mail /opt/ruomi/application.properties
   ```

4. **Проверьте доступность SMTP:**
   ```bash
   telnet smtp.yandex.ru 465
   # или
   nc -zv smtp.yandex.ru 465
   ```

5. **Убедитесь, что домен подтвержден** у провайдера

### Частые ошибки:

- **"Authentication failed"** → Неверный пароль или логин
- **"Connection timeout"** → Файрвол блокирует порт или неверный хост
- **"Connection refused"** → Неверный порт или SMTP сервер недоступен


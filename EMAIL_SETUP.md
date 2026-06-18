# 📧 Настройка почтового сервиса для отправки уведомлений

## 📋 Обзор

Приложение отправляет email уведомления в следующих случаях:
- ✅ **Регистрация** - письмо с подтверждением email
- ✅ **Верификация** - приветственное письмо после подтверждения
- ✅ **Сброс пароля** - письмо со ссылкой для сброса
- ✅ **Личные сообщения** - уведомление о новом сообщении
- ✅ **Одобрение объявления** - уведомление о публикации
- ✅ **Отклонение объявления** - уведомление с причиной

## 🔧 Настройка в application.properties

### Для Gmail

```properties
# Email Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000
```

### Для Yandex

```properties
# Email Configuration
spring.mail.host=smtp.yandex.ru
spring.mail.port=465
spring.mail.username=your-email@yandex.ru
spring.mail.password=your-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.ssl.enable=true
spring.mail.properties.mail.smtp.ssl.trust=smtp.yandex.ru
```

### Для Mail.ru

```properties
# Email Configuration
spring.mail.host=smtp.mail.ru
spring.mail.port=465
spring.mail.username=your-email@mail.ru
spring.mail.password=your-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.ssl.enable=true
spring.mail.properties.mail.smtp.ssl.trust=smtp.mail.ru
```

### Для Outlook/Hotmail

```properties
# Email Configuration
spring.mail.host=smtp-mail.outlook.com
spring.mail.port=587
spring.mail.username=your-email@outlook.com
spring.mail.password=your-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
```

## 🔐 Настройка Gmail (рекомендуется)

### Шаг 1: Создание пароля приложения

1. Войдите в ваш аккаунт Google
2. Перейдите в **Управление аккаунтом Google** → **Безопасность**
3. Включите **Двухэтапную аутентификацию** (если не включена)
4. Перейдите в **Пароли приложений**
5. Выберите **Почта** и **Другое устройство**
6. Введите название: `ruomi.fi`
7. Скопируйте сгенерированный пароль (16 символов)

### Шаг 2: Настройка в application.properties

```properties
spring.mail.username=your-email@gmail.com
spring.mail.password=xxxx xxxx xxxx xxxx
```

**Важно:** Используйте пароль приложения, а не обычный пароль!

## 📝 Настройка на сервере

### 1. Отредактируйте application.properties

```bash
sudo nano /opt/ruomi/application.properties
```

Добавьте или обновите настройки:

```properties
# Email Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
```

### 2. Перезапустите приложение

```bash
sudo systemctl restart ruomi
```

### 3. Проверьте логи

```bash
sudo journalctl -u ruomi -f | grep -i mail
```

## 🧪 Тестирование

### Тест 1: Регистрация нового пользователя

1. Зарегистрируйте нового пользователя
2. Проверьте почту на письмо с подтверждением
3. Перейдите по ссылке для подтверждения
4. Проверьте приветственное письмо

### Тест 2: Отправка личного сообщения

1. Отправьте личное сообщение другому пользователю
2. Проверьте, что получатель получил email уведомление

### Тест 3: Модерация объявления

1. Одобрите или отклоните объявление в админ-панели
2. Проверьте, что автор получил email уведомление

## ⚠️ Частые проблемы

### Проблема: "Authentication failed"

**Причины:**
- Неверный пароль
- Использование обычного пароля вместо пароля приложения (для Gmail)
- Двухэтапная аутентификация не включена

**Решение:**
- Для Gmail: используйте пароль приложения
- Проверьте правильность пароля
- Убедитесь, что двухэтапная аутентификация включена

### Проблема: "Connection timeout"

**Причины:**
- Файрвол блокирует порт
- Неверный хост или порт

**Решение:**
```bash
# Проверьте доступность SMTP сервера
telnet smtp.gmail.com 587

# Или
nc -zv smtp.gmail.com 587
```

### Проблема: Письма попадают в спам

**Решение:**
1. Настройте SPF запись в DNS
2. Настройте DKIM подпись
3. Используйте доменную почту вместо Gmail
4. Попросите пользователей добавить адрес в контакты

### Проблема: "535-5.7.8 Username and Password not accepted"

**Решение:**
- Для Gmail: используйте пароль приложения
- Проверьте, что "Менее безопасные приложения" включены (устаревший способ)
- Или используйте OAuth2

## 🔒 Безопасность

### Рекомендации:

1. **Используйте пароли приложений** для Gmail
2. **Не храните пароли в коде** - используйте переменные окружения
3. **Используйте отдельный email** для отправки уведомлений
4. **Настройте SPF/DKIM** для доменной почты
5. **Ограничьте доступ** к application.properties

### Использование переменных окружения

Вместо хранения пароля в файле:

```properties
spring.mail.password=${MAIL_PASSWORD}
```

И установите переменную:
```bash
export MAIL_PASSWORD=your-password
```

Или в systemd сервисе:
```ini
[Service]
Environment="MAIL_PASSWORD=your-password"
```

## 📊 Мониторинг

### Проверка отправки писем

```bash
# Логи приложения
sudo journalctl -u ruomi -f | grep -i "email\|mail"

# Проверка очереди отправки (если используется)
# В логах ищите:
# "Verification email sent to: ..."
# "Private message notification email sent to: ..."
```

## 🎯 Готовые шаблоны писем

Все шаблоны находятся в `EmailService.java`:

1. **Подтверждение регистрации** - `sendVerificationEmail()`
2. **Приветственное письмо** - `sendWelcomeEmail()`
3. **Сброс пароля** - `sendPasswordResetEmail()`
4. **Личное сообщение** - `sendPrivateMessageNotification()`
5. **Одобрение объявления** - `sendAdvertisementApprovedEmail()`
6. **Отклонение объявления** - `sendAdvertisementRejectedEmail()`

Все письма отправляются автоматически при соответствующих событиях.

## ✅ Чеклист настройки

- [ ] Настроен SMTP сервер в `application.properties`
- [ ] Указан правильный email и пароль
- [ ] Для Gmail создан пароль приложения
- [ ] Приложение перезапущено
- [ ] Протестирована регистрация
- [ ] Протестирована отправка личных сообщений
- [ ] Протестирована модерация объявлений
- [ ] Проверены логи на ошибки

## 📞 Поддержка

Если письма не отправляются:
1. Проверьте логи: `sudo journalctl -u ruomi -n 100`
2. Проверьте настройки в `application.properties`
3. Проверьте доступность SMTP сервера
4. Убедитесь, что пароль правильный



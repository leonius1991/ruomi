# 🔐 Быстрая настройка Telegram авторизации

## ✅ Что уже сделано

- ✅ Бот создан: `ruomi_fi_bot`
- ✅ Токен добавлен в `application.properties`
- ✅ Настройки добавлены в конфигурацию

## 📋 Что нужно сделать

### Шаг 1: Настроить домен в BotFather

1. Откройте Telegram и найдите **@BotFather**
2. Отправьте команду: `/setdomain`
3. BotFather спросит: **"Choose a bot to set domain for"**
4. Введите имя вашего бота: **`ruomi_fi_bot`**
5. BotFather спросит: **"Enter a valid domain"**
6. Введите ваш домен: **`ruomi.fi`**
7. BotFather подтвердит: **"Success! Domain updated."**

**Важно:** 
- Для продакшена используйте: `ruomi.fi` (без http/https и без порта)
- Не используйте `www.ruomi.fi` - только `ruomi.fi`

### Шаг 2: Проверить настройки в application.properties

Убедитесь, что в `/opt/ruomi/application.properties` есть:

```properties
# Telegram Bot Configuration
telegram.bot.username=ruomi_fi_bot
telegram.bot.token=8221655282:AAG6AFmgpXcfPZQedsYIBbcbEHWJNAlWtig

# Telegram Login Widget Configuration
telegram.login.widget.bot.username=ruomi_fi_bot
telegram.login.widget.auth-url=https://ruomi.fi/auth/telegram/callback
```

### Шаг 3: Перезапустить приложение

```bash
sudo systemctl restart ruomi
```

### Шаг 4: Проверить работу

1. Откройте сайт: https://ruomi.fi/login
2. Должна быть кнопка "Login with Telegram"
3. Нажмите на кнопку
4. Должно открыться окно Telegram для авторизации
5. После авторизации вы должны быть автоматически залогинены

## 🔍 Проверка

### Проверка 1: Видна ли кнопка Telegram на странице входа?

Откройте: https://ruomi.fi/login

Должна быть кнопка с логотипом Telegram.

### Проверка 2: Работает ли виджет?

1. Откройте консоль браузера (F12)
2. Нажмите на кнопку "Login with Telegram"
3. Проверьте, нет ли ошибок в консоли

### Проверка 3: Проверка логов

```bash
# На сервере
sudo journalctl -u ruomi -f
# или
tail -f /opt/ruomi/logs/app.log
```

При нажатии на кнопку Telegram должны появиться логи.

## ⚠️ Частые проблемы

### Проблема 1: "Invalid domain"

**Решение:**
- Убедитесь, что в BotFather указан правильный домен: `ruomi.fi` (без www, без http/https)
- Подождите 5-10 минут после настройки домена

### Проблема 2: Кнопка не появляется

**Решение:**
- Проверьте, что в `application.properties` правильно указан `telegram.login.widget.bot.username`
- Проверьте консоль браузера на ошибки JavaScript
- Убедитесь, что страница загружается по HTTPS

### Проблема 3: "Authentication failed"

**Решение:**
- Проверьте токен бота в `application.properties`
- Проверьте логи сервера
- Убедитесь, что URL callback правильный: `https://ruomi.fi/auth/telegram/callback`

### Проблема 4: Виджет открывается, но авторизация не работает

**Решение:**
- Проверьте логи сервера
- Убедитесь, что база данных доступна
- Проверьте, что пользователь создается/находится в БД

## 🧪 Тестирование

### Тест 1: Новая регистрация через Telegram

1. Откройте https://ruomi.fi/login
2. Нажмите "Login with Telegram"
3. Авторизуйтесь в Telegram
4. Должен создаться новый пользователь
5. Вы должны быть автоматически залогинены

### Тест 2: Вход существующего пользователя

1. Создайте пользователя через обычную регистрацию
2. Войдите через Telegram (если username совпадает, аккаунт свяжется автоматически)
3. Или свяжите вручную в профиле

## 📝 Команды для проверки

```bash
# Проверить настройки в application.properties
grep telegram /opt/ruomi/application.properties

# Проверить логи
sudo journalctl -u ruomi -n 100 | grep -i telegram

# Проверить, запущено ли приложение
sudo systemctl status ruomi
```

## ✅ Чеклист

- [ ] Домен настроен в BotFather (`/setdomain` → `ruomi_fi_bot` → `ruomi.fi`)
- [ ] Настройки в `application.properties` правильные
- [ ] Приложение перезапущено
- [ ] Кнопка Telegram видна на странице /login
- [ ] Виджет открывается при клике
- [ ] Авторизация работает
- [ ] Пользователь создается/логинится

## 🎯 Готово!

После выполнения всех шагов авторизация через Telegram должна работать!

Если что-то не работает, проверьте логи и убедитесь, что:
1. Домен правильно настроен в BotFather
2. URL callback правильный (https://ruomi.fi/auth/telegram/callback)
3. Токен бота правильный
4. Приложение перезапущено после изменения настроек



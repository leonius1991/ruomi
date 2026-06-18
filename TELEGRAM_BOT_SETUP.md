# Настройка Telegram бота для vfinke.fi

## 1. Создание бота в Telegram

### Шаг 1: Найдите @BotFather
1. Откройте Telegram
2. Найдите пользователя @BotFather
3. Отправьте команду `/start`

### Шаг 2: Создайте нового бота
1. Отправьте команду `/newbot`
2. Введите имя для вашего бота (например: "vfinke.fi Bot")
3. Введите username для бота (например: "ruomi_fi_bot") - должен заканчиваться на "bot"

### Шаг 3: Получите токен
BotFather выдаст вам токен вида:
```
123456789:ABCdefGHIjklMNOpqrsTUVwxyz
```

## 2. Настройка конфигурации

### Обновите application.properties
Замените значения в файле `src/main/resources/application.properties`:

```properties
# Telegram Bot Configuration
telegram.bot.username=YOUR_BOT_USERNAME
telegram.bot.token=YOUR_BOT_TOKEN
```

Например:
```properties
telegram.bot.username=ruomi_fi_bot
telegram.bot.token=123456789:ABCdefGHIjklMNOpqrsTUVwxyz
```

## 3. Функциональность бота

### Основные команды:
- `/start` - Начало работы с ботом
- 📋 Просмотреть объявления - Показывает последние объявления
- ➕ Добавить объявление - Создание нового объявления
- 🔍 Поиск - Поиск по объявлениям
- 📊 Мои объявления - Ваши созданные объявления
- ℹ️ Помощь - Справка по использованию

### Процесс создания объявления:
1. Выберите "➕ Добавить объявление"
2. Введите заголовок
3. Введите описание
4. Введите цену (только число)
5. Выберите категорию из списка
6. Отправьте фото (опционально)

## 4. Запуск приложения

### Требования:
- Java 17+
- MySQL база данных
- Maven

### Команды для запуска:
```bash
# Сборка проекта
mvn clean compile

# Запуск приложения
mvn spring-boot:run
```

### Проверка работы бота:
1. Найдите вашего бота в Telegram по username
2. Отправьте команду `/start`
3. Должно появиться приветственное сообщение с меню

## 5. Интеграция с веб-сайтом

Бот использует ту же базу данных, что и веб-сайт:
- Объявления, созданные через бота, отображаются на сайте
- Объявления с сайта доступны в боте
- Пользователи автоматически создаются при первом использовании бота

## 6. Возможные проблемы

### Бот не отвечает:
1. Проверьте правильность токена
2. Убедитесь, что приложение запущено
3. Проверьте логи на наличие ошибок

### Ошибки базы данных:
1. Убедитесь, что MySQL запущен
2. Проверьте настройки подключения к БД
3. Убедитесь, что таблицы созданы

### Проблемы с кодировкой:
1. Проверьте настройки UTF-8 в application.properties
2. Убедитесь, что база данных поддерживает UTF-8

## 7. Дополнительные настройки

### Настройка вебхуков (опционально):
Для продакшена рекомендуется использовать вебхуки вместо long polling:

```java
// В TelegramBotAutoConfiguration.java
@PostConstruct
public void registerBot() {
    try {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultWebhook.class);
        botsApi.registerBot(telegramBot);
    } catch (TelegramApiException e) {
        // Обработка ошибок
    }
}
```

### Настройка прокси (если необходимо):
```properties
# В application.properties
telegram.bot.proxy.host=proxy.example.com
telegram.bot.proxy.port=8080
telegram.bot.proxy.type=SOCKS5
```

## 8. Мониторинг и логирование

### Просмотр логов:
```bash
# Запуск с подробными логами
mvn spring-boot:run -Dlogging.level.fi.newdoska.doska.telegram=DEBUG
```

### Метрики:
Приложение предоставляет метрики через Spring Boot Actuator:
- `/actuator/health` - Статус здоровья приложения
- `/actuator/metrics` - Метрики приложения

## 9. Безопасность

### Рекомендации:
1. Не публикуйте токен бота в публичных репозиториях
2. Используйте переменные окружения для токенов
3. Регулярно обновляйте зависимости
4. Настройте rate limiting для API

### Использование переменных окружения:
```properties
telegram.bot.token=${TELEGRAM_BOT_TOKEN}
telegram.bot.username=${TELEGRAM_BOT_USERNAME}
```

## 10. Поддержка

При возникновении проблем:
1. Проверьте логи приложения
2. Убедитесь в правильности конфигурации
3. Проверьте статус Telegram Bot API
4. Обратитесь к документации Spring Boot и Telegram Bot API




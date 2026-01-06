# Инструкции по запуску vfinke.fi

## 🚀 Быстрый запуск

### Вариант 1: Через командную строку (рекомендуется)

1. **Откройте командную строку** в папке проекта
2. **Выполните команды**:
   ```bash
   # Проверка Java
   java -version
   
   # Сборка проекта
   mvn clean compile
   
   # Запуск приложения
   mvn spring-boot:run
   ```

### Вариант 2: Через Maven Wrapper

```bash
# Сборка
./mvnw clean compile

# Запуск
./mvnw spring-boot:run
```

### Вариант 3: Через IDE

1. **Откройте проект** в IntelliJ IDEA или Eclipse
2. **Найдите класс** `DoskaApplication.java`
3. **Запустите** метод `main`

## ⚙️ Настройка перед запуском

### 1. База данных MySQL

**Убедитесь, что MySQL запущен и создайте базу данных:**

```sql
CREATE DATABASE newdoska CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'newdoska_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON newdoska.* TO 'newdoska_user'@'localhost';
FLUSH PRIVILEGES;
```

### 2. Конфигурация приложения

**Отредактируйте файл** `src/main/resources/application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/newdoska?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8
spring.datasource.username=newdoska_user
spring.datasource.password=your_password

# Telegram Bot Configuration (опционально)
telegram.bot.username=your_bot_username
telegram.bot.token=your_bot_token

# Email Configuration (опционально)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

### 3. Telegram бот (опционально)

1. **Найдите @BotFather** в Telegram
2. **Отправьте** `/newbot`
3. **Следуйте инструкциям** для создания бота
4. **Скопируйте токен** и добавьте в `application.properties`

## 🔍 Проверка работы

### Веб-приложение
- **URL**: http://localhost:8080
- **Должна открыться** главная страница с логотипом vfinke.fi

### Telegram бот
- **Найдите бота** по username в Telegram
- **Отправьте** `/start`
- **Должно появиться** приветственное сообщение

## 🐛 Решение проблем

### Ошибка "Port 8080 is already in use"
```bash
# Найдите процесс на порту 8080
netstat -ano | findstr :8080

# Остановите процесс
taskkill /PID <process_id> /F
```

### Ошибка подключения к базе данных
1. **Проверьте**, что MySQL запущен
2. **Убедитесь**, что база данных создана
3. **Проверьте** логин и пароль в `application.properties`

### Ошибка компиляции
```bash
# Очистите и пересоберите проект
mvn clean compile

# Проверьте версию Java (должна быть 17+)
java -version
```

### Telegram бот не отвечает
1. **Проверьте** правильность токена
2. **Убедитесь**, что приложение запущено
3. **Проверьте** логи на наличие ошибок

## 📊 Логи и отладка

### Просмотр логов
```bash
# Запуск с подробными логами
mvn spring-boot:run -Dlogging.level.fi.newdoska.doska=DEBUG

# Логи Telegram бота
mvn spring-boot:run -Dlogging.level.fi.newdoska.doska.telegram=DEBUG
```

### Метрики приложения
- **Health check**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/metrics
- **Info**: http://localhost:8080/actuator/info

## 🐳 Docker (альтернативный способ)

### Запуск через Docker Compose
```bash
# Запуск всех сервисов
docker-compose up -d

# Просмотр логов
docker-compose logs -f

# Остановка
docker-compose down
```

### Сборка Docker образа
```bash
# Сборка
docker build -t newdoska .

# Запуск
docker run -p 8080:8080 newdoska
```

## 📱 Тестирование функций

### Веб-приложение
1. **Откройте** http://localhost:8080
2. **Зарегистрируйтесь** или войдите
3. **Создайте** тестовое объявление
4. **Проверьте** поиск и фильтрацию

### Telegram бот
1. **Найдите бота** в Telegram
2. **Отправьте** `/start`
3. **Протестируйте** создание объявления
4. **Проверьте** просмотр объявлений

## 🔧 Дополнительные настройки

### Настройка прокси (если необходимо)
```properties
# В application.properties
telegram.bot.proxy.host=proxy.example.com
telegram.bot.proxy.port=8080
telegram.bot.proxy.type=SOCKS5
```

### Настройка SSL/HTTPS
```properties
# В application.properties
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=your_password
server.ssl.key-store-type=PKCS12
```

### Настройка кэширования
```properties
# В application.properties
spring.cache.type=caffeine
spring.cache.caffeine.spec=maximumSize=500,expireAfterWrite=600s
```

## 📞 Поддержка

При возникновении проблем:

1. **Проверьте** логи приложения
2. **Убедитесь** в правильности конфигурации
3. **Проверьте** версии Java и Maven
4. **Обратитесь** к документации проекта

### Полезные команды
```bash
# Проверка версий
java -version
mvn -version

# Очистка проекта
mvn clean

# Полная пересборка
mvn clean install

# Запуск тестов
mvn test
```

---

**Удачного запуска! 🎉**




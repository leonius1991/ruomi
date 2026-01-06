# vfinke.fi - Современная доска объявлений для русскоязычного населения в Финляндии

## 🚀 Обзор проекта

vfinke.fi - это полнофункциональная платформа для размещения и поиска объявлений, специально разработанная для русскоязычного населения в Финляндии. Проект включает в себя веб-приложение и Telegram бота, которые работают с единой базой данных.

## ✨ Основные возможности

### 🌐 Веб-приложение
- **Регистрация и авторизация** пользователей
- **Создание и редактирование** объявлений
- **Поиск и фильтрация** по категориям, городам, ценам
- **Премиум и срочные** объявления
- **Система модерации** объявлений
- **Управление профилем** пользователя
- **Система платежей** для премиум-функций

### 🤖 Telegram бот
- **Просмотр объявлений** прямо в Telegram
- **Создание новых объявлений** через бота
- **Поиск по категориям**
- **Управление своими объявлениями**
- **Уведомления** о новых объявлениях
- **Интуитивный интерфейс** с кнопками

## 🛠 Технологический стек

### Backend
- **Java 17** - основной язык программирования
- **Spring Boot 3.5.4** - фреймворк для веб-приложений
- **Spring Security** - аутентификация и авторизация
- **Spring Data JPA** - работа с базой данных
- **MySQL** - реляционная база данных
- **JWT** - токены для аутентификации
- **Thymeleaf** - шаблонизатор для веб-страниц

### Telegram Bot
- **Telegram Bot API** - интеграция с Telegram
- **Spring Boot Starter** для Telegram ботов
- **Long Polling** для получения обновлений

### Frontend
- **Bootstrap 5** - CSS фреймворк
- **Font Awesome** - иконки
- **JavaScript** - интерактивность
- **Responsive Design** - адаптивный дизайн

### Инфраструктура
- **Maven** - управление зависимостями
- **Docker** - контейнеризация
- **Docker Compose** - оркестрация контейнеров

## 📁 Структура проекта

```
doska/
├── src/
│   ├── main/
│   │   ├── java/fi/newdoska/doska/
│   │   │   ├── config/           # Конфигурации
│   │   │   ├── controller/       # Контроллеры
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   ├── entity/          # Сущности JPA
│   │   │   ├── repository/      # Репозитории
│   │   │   ├── service/         # Бизнес-логика
│   │   │   ├── telegram/        # Telegram бот
│   │   │   └── DoskaApplication.java
│   │   └── resources/
│   │       ├── static/          # Статические файлы
│   │       ├── templates/       # HTML шаблоны
│   │       └── application.properties
│   └── test/                    # Тесты
├── docker-compose.yml           # Docker конфигурация
├── Dockerfile                   # Docker образ
├── pom.xml                      # Maven зависимости
└── README.md                    # Документация
```

## 🚀 Быстрый старт

### Предварительные требования
- Java 17 или выше
- MySQL 8.0 или выше
- Maven 3.6 или выше
- Git

### 1. Клонирование репозитория
```bash
git clone https://github.com/your-username/newdoska.git
cd newdoska
```

### 2. Настройка базы данных
```sql
CREATE DATABASE newdoska CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'newdoska_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON newdoska.* TO 'newdoska_user'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Настройка конфигурации
Отредактируйте `src/main/resources/application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/newdoska?useSSL=false&serverTimezone=UTC
spring.datasource.username=newdoska_user
spring.datasource.password=your_password

# Telegram Bot Configuration
telegram.bot.username=your_bot_username
telegram.bot.token=your_bot_token

# Email Configuration (для уведомлений)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

### 4. Запуск приложения

#### Вариант 1: Через Maven
```bash
mvn clean compile
mvn spring-boot:run
```

#### Вариант 2: Через Maven Wrapper
```bash
./mvnw clean compile
./mvnw spring-boot:run
```

#### Вариант 3: Через Docker
```bash
docker-compose up -d
```

### 5. Доступ к приложению
- **Веб-приложение**: http://localhost:8080
- **Telegram бот**: @your_bot_username

## 🤖 Настройка Telegram бота

### 1. Создание бота
1. Найдите @BotFather в Telegram
2. Отправьте `/newbot`
3. Следуйте инструкциям для создания бота
4. Сохраните полученный токен

### 2. Настройка конфигурации
Добавьте в `application.properties`:
```properties
telegram.bot.username=your_bot_username
telegram.bot.token=your_bot_token
```

### 3. Функциональность бота
- `/start` - Начало работы
- 📋 Просмотреть объявления
- ➕ Добавить объявление
- 🔍 Поиск
- 📊 Мои объявления
- ℹ️ Помощь

## 📊 Модель данных

### Основные сущности

#### User (Пользователь)
- Основная информация (имя, email, пароль)
- Telegram ID для интеграции с ботом
- Роли (USER, MODERATOR, ADMIN)
- Статус аккаунта

#### Advertisement (Объявление)
- Заголовок и описание
- Категория и тип
- Цена и местоположение
- Статус модерации
- Премиум и срочные флаги

#### AdvertisementImage (Изображения)
- Связь с объявлением
- Путь к файлу
- Порядок отображения

#### Payment (Платежи)
- Связь с пользователем и объявлением
- Сумма и статус
- Тип платежа

## 🔐 Безопасность

### Аутентификация
- JWT токены для API
- Spring Security для веб-интерфейса
- Верификация email при регистрации

### Авторизация
- Роли пользователей (USER, MODERATOR, ADMIN)
- Проверка прав доступа к объявлениям
- Модерация контента

### Защита данных
- Хеширование паролей (BCrypt)
- Валидация входных данных
- Защита от SQL-инъекций

## 🎨 Дизайн и UX

### Веб-интерфейс
- Современный адаптивный дизайн
- Bootstrap 5 для компонентов
- Анимации и переходы
- Оптимизация для мобильных устройств

### Telegram бот
- Интуитивные кнопки и меню
- Пошаговое создание объявлений
- Красивое форматирование сообщений
- Быстрый доступ к функциям

## 📈 Мониторинг и метрики

### Spring Boot Actuator
- `/actuator/health` - Статус здоровья
- `/actuator/metrics` - Метрики приложения
- `/actuator/info` - Информация о приложении

### Логирование
- Структурированные логи
- Разные уровни логирования
- Ротация логов

## 🚀 Развертывание

### Docker
```bash
# Сборка образа
docker build -t newdoska .

# Запуск контейнера
docker run -p 8080:8080 newdoska
```

### Docker Compose
```bash
# Запуск всех сервисов
docker-compose up -d

# Просмотр логов
docker-compose logs -f
```

### Продакшн
- Использование HTTPS
- Настройка reverse proxy (Nginx)
- Мониторинг и алерты
- Резервное копирование БД

## 🧪 Тестирование

### Unit тесты
```bash
mvn test
```

### Интеграционные тесты
```bash
mvn verify
```

### Тестирование Telegram бота
1. Запустите приложение
2. Найдите бота в Telegram
3. Отправьте `/start`
4. Протестируйте все функции

## 🤝 Вклад в проект

### Установка для разработки
1. Форкните репозиторий
2. Создайте ветку для новой функции
3. Внесите изменения
4. Создайте Pull Request

### Стандарты кода
- Java Code Style
- Javadoc для публичных методов
- Тесты для новой функциональности
- Логирование важных операций

## 📝 Лицензия

MIT License - см. файл [LICENSE](LICENSE) для подробностей.

## 📞 Поддержка

### Контакты
- **Email**: info@newdoska.fi
- **Telegram**: @newdoska_support
- **Веб-сайт**: https://newdoska.fi

### Документация
- [Руководство пользователя](USER_GUIDE.md)
- [API документация](API_DOCS.md)
- [Настройка Telegram бота](TELEGRAM_BOT_SETUP.md)

## 🎯 Roadmap

### Версия 2.0
- [ ] Мобильное приложение (React Native)
- [ ] Система уведомлений
- [ ] Чат между пользователями
- [ ] Система рейтингов и отзывов
- [ ] Интеграция с платежными системами

### Версия 2.1
- [ ] Многоязычная поддержка
- [ ] Расширенная аналитика
- [ ] API для внешних интеграций
- [ ] Система рекомендаций

## 🙏 Благодарности

- Spring Boot команде за отличный фреймворк
- Telegram за предоставление Bot API
- Bootstrap команде за CSS фреймворк
- Всем участникам проекта

---

**Сделано с ❤️ для русскоязычного сообщества в Финляндии**




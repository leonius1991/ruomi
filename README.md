# vfinke.fi - Современная доска объявлений для русскоязычного населения в Финляндии

![vfinke.fi Logo](https://img.shields.io/badge/vfinke.fi-Doska%20объявлений-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.4-green)
![Java](https://img.shields.io/badge/Java-21-orange)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![Bootstrap](https://img.shields.io/badge/Bootstrap-5.3.0-purple)

## 🚀 Описание проекта

vfinke.fi - это современная веб-платформа для размещения и поиска объявлений, специально разработанная для русскоязычного населения в Финляндии. Проект представляет собой полнофункциональный сайт объявлений с современным дизайном, системой регистрации, модерацией контента и платными услугами.

## ✨ Основные возможности

### 🔐 Аутентификация и безопасность
- Регистрация и вход пользователей
- **Telegram аутентификация** - вход и регистрация через Telegram
- **Связывание аккаунтов** - привязка Telegram к существующему аккаунту
- Подтверждение email адреса
- Восстановление пароля
- Роли пользователей (USER, PREMIUM, MODERATOR, ADMIN, SUPER_ADMIN)
- Spring Security интеграция

### 📢 Управление объявлениями
- Создание, редактирование и удаление объявлений
- Категории: недвижимость, транспорт, электроника, мебель, одежда, книги, спорт, услуги, работа
- Типы объявлений: продажа, покупка, аренда, обмен, услуга
- Модерация объявлений
- Статусы: на модерации, одобрено, отклонено, истекло, удалено

### 💰 Платные услуги
- Премиум размещение (€19.99/месяц)
- Срочное размещение (€9.99/неделя)
- Поднятие в топ (€4.99/день)
- Продление срока (€14.99/месяц)

### 🔍 Поиск и фильтрация
- Поиск по тексту
- Фильтрация по категориям
- Фильтрация по городам
- Фильтрация по цене
- Сортировка по приоритету (премиум, срочные, дата)

### 📱 Современный интерфейс
- Адаптивный дизайн для всех устройств
- Bootstrap 5.3.0
- Font Awesome иконки
- Анимации и переходы
- Темная тема для футера

### 🤖 Telegram интеграция
- **Telegram бот** - управление объявлениями через бота
- **Telegram Login Widget** - аутентификация через Telegram
- **Синхронизация аккаунтов** - единая база данных для веб и бота
- **Уведомления** - получение уведомлений через Telegram

## 🛠 Технологический стек

### Backend
- **Java 21** - основной язык программирования
- **Spring Boot 3.5.4** - фреймворк для веб-приложений
- **Spring Security** - безопасность и аутентификация
- **Spring Data JPA** - работа с базой данных
- **Spring Mail** - отправка email уведомлений
- **MySQL 8.0** - реляционная база данных
- **Lombok** - упрощение кода
- **Maven** - управление зависимостями

### Frontend
- **Thymeleaf** - серверные шаблоны
- **Bootstrap 5.3.0** - CSS фреймворк
- **Font Awesome 6.4.0** - иконки
- **Vanilla JavaScript** - интерактивность
- **CSS3** - современные стили

### Инфраструктура
- **JWT** - токены для аутентификации
- **BCrypt** - хеширование паролей
- **Actuator** - мониторинг приложения

## 📋 Требования

### Системные требования
- Java 21 или выше
- MySQL 8.0 или выше
- Maven 3.6 или выше
- Минимум 4GB RAM
- 2GB свободного места на диске

### Поддерживаемые ОС
- Windows 10/11
- macOS 10.15+
- Ubuntu 20.04+
- CentOS 8+

## 🚀 Установка и запуск

### 1. Клонирование репозитория
```bash
git clone https://github.com/yourusername/newdoska.git
cd newdoska
```

### 2. Настройка базы данных
```sql
-- Создание базы данных
CREATE DATABASE newdoska CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Создание пользователя (опционально)
CREATE USER 'newdoska_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON newdoska.* TO 'newdoska_user'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Настройка конфигурации
Отредактируйте файл `src/main/resources/application.properties`:

```properties
# База данных
spring.datasource.url=jdbc:mysql://localhost:3306/newdoska?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8
spring.datasource.username=your_username
spring.datasource.password=your_password

# Email настройки (для Gmail)
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password

# JWT секрет (измените в продакшене)
jwt.secret=your-super-secret-key-change-in-production
```

### 4. Запуск приложения
```bash
# Компиляция и запуск
mvn spring-boot:run

# Или сборка JAR файла
mvn clean package
java -jar target/doska-0.0.1-SNAPSHOT.jar
```

### 5. Доступ к приложению
Откройте браузер и перейдите по адресу: `http://localhost:8080`

### 6. Настройка Telegram интеграции (опционально)

#### Настройка Telegram бота
1. Найдите @BotFather в Telegram
2. Отправьте `/newbot` и следуйте инструкциям
3. Сохраните полученный токен
4. Добавьте в `application.properties`:
```properties
telegram.bot.username=your_bot_username
telegram.bot.token=your_bot_token
```

#### Настройка Telegram аутентификации
1. Отправьте `/setdomain` @BotFather
2. Укажите ваш бот и домен (localhost:8080 для разработки)
3. Обновите `application.properties`:
```properties
telegram.login.widget.bot.username=your_bot_username
```

Подробная инструкция: [TELEGRAM_AUTH_SETUP.md](TELEGRAM_AUTH_SETUP.md)

## 📁 Структура проекта

```
src/
├── main/
│   ├── java/fi/newdoska/doska/
│   │   ├── config/          # Конфигурации Spring
│   │   ├── controller/      # Контроллеры
│   │   ├── dto/            # Data Transfer Objects
│   │   ├── entity/         # JPA сущности
│   │   ├── repository/     # Репозитории
│   │   ├── service/        # Бизнес-логика
│   │   └── DoskaApplication.java
│   └── resources/
│       ├── static/         # Статические файлы (CSS, JS, изображения)
│       └── templates/      # Thymeleaf шаблоны
├── test/                   # Тесты
└── pom.xml                # Maven конфигурация
```

## 🔧 Конфигурация

### Основные настройки
- **Порт сервера**: 8080 (изменяется в `application.properties`)
- **Контекст**: `/` (корневой путь)
- **Кодировка**: UTF-8
- **Временная зона**: UTC

### Email настройки
Для Gmail необходимо:
1. Включить двухфакторную аутентификацию
2. Создать пароль приложения
3. Использовать пароль приложения в конфигурации

### Безопасность
- Пароли хешируются с помощью BCrypt
- JWT токены для аутентификации
- CSRF защита
- Валидация входных данных

## 📊 База данных

### Основные таблицы
- `users` - пользователи системы
- `advertisements` - объявления
- `advertisement_images` - изображения объявлений
- `payments` - платежи за услуги

### Автоматическое создание схемы
Hibernate автоматически создает таблицы при запуске (`spring.jpa.hibernate.ddl-auto=update`)

## 🧪 Тестирование

### Запуск тестов
```bash
# Все тесты
mvn test

# Конкретный тест
mvn test -Dtest=UserServiceTest

# Тесты с отчетом
mvn test jacoco:report
```

### Покрытие кода
Проект включает тесты для основных сервисов и контроллеров.

## 🚀 Развертывание

### Docker (рекомендуется)
```dockerfile
FROM openjdk:21-jdk-slim
COPY target/doska-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
```

### Традиционное развертывание
1. Соберите JAR файл: `mvn clean package`
2. Скопируйте JAR на сервер
3. Запустите: `java -jar doska-0.0.1-SNAPSHOT.jar`

### Переменные окружения
```bash
export SPRING_PROFILES_ACTIVE=production
export SPRING_DATASOURCE_URL=jdbc:mysql://your-db-host:3306/newdoska
export SPRING_DATASOURCE_USERNAME=your_username
export SPRING_DATASOURCE_PASSWORD=your_password
export JWT_SECRET=your-production-secret
```

## 📱 Функциональность

### Для пользователей
- ✅ Регистрация и вход
- ✅ Создание объявлений
- ✅ Загрузка изображений
- ✅ Поиск и фильтрация
- ✅ Платные услуги
- ✅ Личный кабинет

### Для модераторов
- ✅ Модерация объявлений
- ✅ Управление пользователями
- ✅ Статистика

### Для администраторов
- ✅ Полный доступ к системе
- ✅ Управление ролями
- ✅ Системная статистика

## 🔒 Безопасность

### Аутентификация
- Spring Security
- JWT токены
- BCrypt хеширование

### Авторизация
- Ролевая модель доступа
- Защищенные эндпоинты
- CSRF защита

### Валидация
- Валидация на стороне сервера
- Санитизация входных данных
- Защита от SQL инъекций

## 📈 Производительность

### Оптимизации
- Ленивая загрузка изображений
- Пагинация результатов
- Кэширование
- Оптимизированные SQL запросы

### Мониторинг
- Spring Boot Actuator
- Логирование
- Метрики производительности

## 🐛 Известные проблемы

### Текущие ограничения
- Загрузка изображений ограничена 10MB
- Email уведомления требуют настройки SMTP
- JWT токены имеют фиксированное время жизни

### Планы развития
- [ ] Мобильное приложение
- [ ] Push уведомления
- [ ] Чат между пользователями
- [ ] API для внешних интеграций
- [ ] Многоязычность (финский, английский)

## 🤝 Вклад в проект

### Как помочь
1. Форкните репозиторий
2. Создайте ветку для новой функции
3. Внесите изменения
4. Создайте Pull Request

### Требования к коду
- Следуйте Java Code Style
- Добавляйте тесты для новой функциональности
- Обновляйте документацию
- Используйте meaningful commit messages

## 📄 Лицензия

Этот проект распространяется под лицензией MIT. См. файл [LICENSE](LICENSE) для подробностей.

## 👥 Команда

- **Разработчик**: vfinke.fi Team
- **Email**: info@newdoska.fi
- **Веб-сайт**: https://newdoska.fi

## 🙏 Благодарности

- Spring Boot команде за отличный фреймворк
- Bootstrap команде за CSS фреймворк
- Font Awesome за иконки
- Сообществу open source

## 📞 Поддержка

### Способы связи
- **Email**: support@newdoska.fi
- **Telegram**: @newdoska_support
- **Issues**: GitHub Issues

### Часто задаваемые вопросы
См. раздел [FAQ](docs/FAQ.md) для ответов на популярные вопросы.

---

**Сделано с ❤️ в Финляндии для русскоязычного сообщества**

*Если проект вам понравился, поставьте ⭐ звездочку!* 
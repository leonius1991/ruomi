# 🚀 Быстрый запуск NewDoska

## 📋 Предварительные требования

- **Java 21** или выше
- **Maven 3.6** или выше  
- **MySQL 8.0** или выше

## ⚡ Быстрый запуск

### Windows
```bash
# Двойной клик на файл start.bat
# Или в командной строке:
start.bat
```

### Linux/Mac
```bash
# Сделать скрипт исполняемым (только первый раз)
chmod +x start.sh

# Запустить
./start.sh
```

### Ручной запуск
```bash
# Компиляция
mvn clean compile

# Запуск
mvn spring-boot:run
```

## 🌐 Доступ к приложению

После успешного запуска откройте браузер:
**http://localhost:8080**

## 🔧 Настройка базы данных

1. Создайте базу данных:
```sql
CREATE DATABASE newdoska CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. Отредактируйте `src/main/resources/application.properties`:
```properties
spring.datasource.username=your_username
spring.datasource.password=your_password
```

## 🐳 Запуск через Docker

```bash
# Сборка и запуск
docker-compose up -d

# Просмотр логов
docker-compose logs -f

# Остановка
docker-compose down
```

## 📱 Основные функции

- ✅ Регистрация и вход пользователей
- ✅ Создание объявлений
- ✅ Поиск и фильтрация
- ✅ Платные услуги
- ✅ Модерация контента
- ✅ Адаптивный дизайн

## 🆘 Если что-то не работает

1. Проверьте, что MySQL запущен
2. Убедитесь, что порт 8080 свободен
3. Проверьте логи в консоли
4. Убедитесь, что Java 21+ установлена

## 📞 Поддержка

- **Email**: support@newdoska.fi
- **Telegram**: @newdoska_support
- **Issues**: GitHub Issues

---

**Удачного использования! 🎉** 
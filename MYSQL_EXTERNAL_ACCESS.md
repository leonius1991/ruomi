# 🔌 Настройка внешнего доступа к MySQL

## Проблема

Ошибка `Connection refused` при попытке подключиться к MySQL извне означает, что:
1. MySQL слушает только на `localhost` (127.0.0.1)
2. Файрвол блокирует порт 3306
3. Пользователь MySQL не имеет прав на подключение извне

## 🔧 Решение

### Вариант 1: Автоматическая настройка (рекомендуется)

```bash
# Скачайте скрипт на сервер
nano fix-mysql-external-access.sh
# Скопируйте содержимое из файла fix-mysql-external-access.sh

chmod +x fix-mysql-external-access.sh
sudo ./fix-mysql-external-access.sh
```

### Вариант 2: Ручная настройка

#### Шаг 1: Настройка bind-address

1. Найдите файл конфигурации MySQL:
   ```bash
   # Обычно это один из этих файлов:
   /etc/mysql/mysql.conf.d/mysqld.cnf
   /etc/mysql/my.cnf
   /etc/my.cnf
   ```

2. Отредактируйте файл:
   ```bash
   sudo nano /etc/mysql/mysql.conf.d/mysqld.cnf
   ```

3. Найдите строку:
   ```ini
   bind-address = 127.0.0.1
   ```

4. Измените на:
   ```ini
   bind-address = 0.0.0.0
   ```

   Или закомментируйте строку:
   ```ini
   # bind-address = 127.0.0.1
   ```

5. Сохраните файл и перезапустите MySQL:
   ```bash
   sudo systemctl restart mysql
   # или
   sudo systemctl restart mysqld
   ```

#### Шаг 2: Настройка файрвола

**Для UFW:**
```bash
sudo ufw allow 3306/tcp
sudo ufw reload
```

**Для firewalld:**
```bash
sudo firewall-cmd --permanent --add-port=3306/tcp
sudo firewall-cmd --reload
```

**Для iptables:**
```bash
sudo iptables -A INPUT -p tcp --dport 3306 -j ACCEPT
sudo iptables-save
```

#### Шаг 3: Создание пользователя с доступом извне

```bash
mysql -u root -p
```

В MySQL:
```sql
-- Создать пользователя с доступом из любого IP
CREATE USER 'ruomi'@'%' IDENTIFIED BY 'ваш_пароль';
GRANT ALL PRIVILEGES ON ruomi.* TO 'ruomi'@'%';
FLUSH PRIVILEGES;
EXIT;
```

**Для безопасности (рекомендуется):**
```sql
-- Ограничить доступ только с определенного IP
CREATE USER 'ruomi'@'YOUR_IP' IDENTIFIED BY 'ваш_пароль';
GRANT ALL PRIVILEGES ON ruomi.* TO 'ruomi'@'YOUR_IP';
FLUSH PRIVILEGES;
```

#### Шаг 4: Проверка

```bash
# Проверьте, что MySQL слушает на всех интерфейсах
sudo netstat -tlnp | grep 3306
# Должно быть: 0.0.0.0:3306

# Или
sudo ss -tlnp | grep 3306
```

## 🔒 Безопасность

### Рекомендации:

1. **Ограничьте доступ по IP:**
   ```sql
   CREATE USER 'ruomi'@'YOUR_IP' IDENTIFIED BY 'password';
   ```

2. **Используйте SSL:**
   ```sql
   GRANT ALL PRIVILEGES ON ruomi.* TO 'ruomi'@'%' REQUIRE SSL;
   ```

3. **Используйте сильные пароли**

4. **Ограничьте привилегии:**
   ```sql
   -- Вместо ALL PRIVILEGES используйте только необходимые
   GRANT SELECT, INSERT, UPDATE, DELETE ON ruomi.* TO 'ruomi'@'%';
   ```

5. **Используйте VPN или SSH туннель** вместо прямого доступа

## 🔍 Диагностика

### Проверка статуса MySQL:
```bash
sudo systemctl status mysql
```

### Проверка порта:
```bash
sudo netstat -tlnp | grep 3306
# Должно быть: 0.0.0.0:3306
```

### Проверка файрвола:
```bash
# UFW
sudo ufw status

# firewalld
sudo firewall-cmd --list-ports
```

### Проверка пользователей MySQL:
```bash
mysql -u root -p -e "SELECT User, Host FROM mysql.user;"
```

### Тест подключения извне:
```bash
# С другого компьютера
mysql -h YOUR_SERVER_IP -u ruomi -p ruomi
```

## ⚠️ Частые проблемы

### Проблема: "Access denied for user"

**Решение:**
- Проверьте, что пользователь создан с `@'%'` или `@'YOUR_IP'`
- Проверьте пароль
- Выполните `FLUSH PRIVILEGES;`

### Проблема: "Can't connect to MySQL server"

**Решение:**
- Проверьте, что `bind-address = 0.0.0.0`
- Проверьте файрвол
- Проверьте, что MySQL запущен: `sudo systemctl status mysql`

### Проблема: Подключение работает, но медленно

**Решение:**
- Используйте SSL
- Проверьте сетевую задержку
- Рассмотрите использование SSH туннеля

## 📝 Пример конфигурации для SSH туннеля

Вместо прямого доступа можно использовать SSH туннель:

```bash
# На вашем компьютере
ssh -L 3306:localhost:3306 user@your-server.com

# Затем подключайтесь к localhost:3306
```

Это безопаснее, чем открывать порт 3306 в интернет.



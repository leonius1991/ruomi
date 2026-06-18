# 🐧 Установка приложения на Linux сервер

## Предварительные требования

### 1. Установка Java 17+

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-17-jdk

# CentOS/RHEL
sudo yum install java-17-openjdk-devel

# Проверка версии
java -version
```

### 2. Установка MySQL

```bash
# Ubuntu/Debian
sudo apt install mysql-server

# CentOS/RHEL
sudo yum install mysql-server

# Запуск MySQL
sudo systemctl start mysql
sudo systemctl enable mysql

# Создание базы данных
sudo mysql -u root -p
```

В MySQL:
```sql
CREATE DATABASE newdoska CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'doska_user'@'localhost' IDENTIFIED BY 'ваш_пароль';
GRANT ALL PRIVILEGES ON newdoska.* TO 'doska_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### 3. Создание пользователя для приложения

```bash
# Создаем пользователя (опционально, для безопасности)
sudo useradd -r -s /bin/false doska
```

## Установка приложения

### 1. Создание директории

```bash
sudo mkdir -p /opt/doska
sudo chown $USER:$USER /opt/doska
cd /opt/doska
```

### 2. Загрузка JAR файла

```bash
# Скачайте JAR файл с GitHub Releases
wget https://github.com/mifedweb/ruomi/releases/latest/download/doska-0.0.1-SNAPSHOT.jar -O app.jar

# Или используйте curl
curl -L https://github.com/mifedweb/ruomi/releases/latest/download/doska-0.0.1-SNAPSHOT.jar -o app.jar
```

### 3. Создание директорий

```bash
mkdir -p updates backups external-resources/static external-resources/templates
```

### 4. Создание файла конфигурации

```bash
nano application.properties
```

Содержимое:
```properties
# Server Configuration
server.port=8080

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/newdoska?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8
spring.datasource.username=doska_user
spring.datasource.password=ваш_пароль
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update

# Thymeleaf Configuration
spring.thymeleaf.cache=false

# Application Update Configuration
app.version=0.0.1-SNAPSHOT
app.update.github.owner=mifedweb
app.update.github.repo=ruomi
app.update.download.path=./updates
app.update.backup.path=./backups
app.jar.path=./app.jar
app.update.restart.enabled=true
app.update.restart.script=./restart.sh
app.update.restart.service=doska

# External Resources (для редактирования на лету)
app.resources.use-external=false
app.resources.external.path=./external-resources

# Disable caching
spring.web.resources.cache.period=0
```

### 5. Создание скрипта запуска

```bash
nano start.sh
```

Содержимое:
```bash
#!/bin/bash
cd /opt/doska
java -jar -Dspring.config.location=./application.properties app.jar
```

```bash
chmod +x start.sh
```

### 6. Создание systemd сервиса

```bash
sudo nano /etc/systemd/system/doska.service
```

Содержимое:
```ini
[Unit]
Description=Doska Application
After=network.target mysql.service

[Service]
Type=simple
User=doska
WorkingDirectory=/opt/doska
ExecStart=/usr/bin/java -jar -Dspring.config.location=/opt/doska/application.properties /opt/doska/app.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

Активация сервиса:
```bash
sudo systemctl daemon-reload
sudo systemctl enable ruomi
sudo systemctl start ruomi
sudo systemctl status ruomi

### 6. Настройка sudoers для автоматического перезапуска

Для того, чтобы обновления могли автоматически перезапускать приложение, нужно настроить sudoers:

```bash
# Вариант 1: Использовать готовый скрипт
sudo bash setup-sudoers.sh

# Вариант 2: Настроить вручную
sudo visudo -f /etc/sudoers.d/ruomi-restart
```

Добавьте следующие строки (замените `ruomi` на имя пользователя, под которым запущено приложение):

```
ruomi ALL=(ALL) NOPASSWD: /bin/systemctl restart ruomi
ruomi ALL=(ALL) NOPASSWD: /bin/systemctl stop ruomi
ruomi ALL=(ALL) NOPASSWD: /bin/systemctl start ruomi
ruomi ALL=(ALL) NOPASSWD: /bin/systemctl status ruomi
```

Сохраните файл (в visudo: `:wq`).

**Важно:** Убедитесь, что файл имеет правильные права:
```bash
sudo chmod 0440 /etc/sudoers.d/ruomi-restart
```
```

### 7. Настройка скрипта перезапуска

```bash
nano restart.sh
```

Скопируйте содержимое из `restart.sh` в проекте и обновите пути:
```bash
#!/bin/bash
cd /opt/doska
sudo systemctl restart doska
```

```bash
chmod +x restart.sh
```

### 8. Настройка внешних ресурсов (опционально)

Если хотите редактировать HTML/CSS на лету:

```bash
# Распакуйте ресурсы из релиза
unzip resources-*.zip -d external-resources/

# Включите внешние ресурсы в application.properties
# app.resources.use-external=true
```

## Обновление приложения

### Автоматическое обновление через админ-панель

1. Войдите в админ-панель
2. Перейдите в "Обновления"
3. Нажмите "Проверить обновления"
4. Если есть новая версия, нажмите "Обновить"
5. Приложение автоматически обновится и перезапустится

### Ручное обновление

```bash
cd /opt/doska
sudo systemctl stop doska

# Скачайте новую версию
wget https://github.com/mifedweb/ruomi/releases/latest/download/doska-X.X.X.jar -O app.jar.new
mv app.jar app.jar.backup
mv app.jar.new app.jar

# Обновите ресурсы (если нужно)
unzip resources-*.zip -d external-resources/

sudo systemctl start doska
```

## Настройка Nginx (опционально)

```bash
sudo apt install nginx
sudo nano /etc/nginx/sites-available/doska
```

Конфигурация:
```nginx
server {
    listen 80;
    server_name ваш_домен.ru;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Активация:
```bash
sudo ln -s /etc/nginx/sites-available/doska /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

## Настройка SSL (Let's Encrypt)

```bash
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d ваш_домен.ru
```

## Мониторинг

### Просмотр логов

```bash
# Логи systemd
sudo journalctl -u doska -f

# Логи приложения
tail -f /opt/doska/app.log
```

### Проверка статуса

```bash
sudo systemctl status doska
```

## Устранение неполадок

### Приложение не запускается

1. Проверьте логи: `sudo journalctl -u doska -n 50`
2. Проверьте Java: `java -version`
3. Проверьте MySQL: `sudo systemctl status mysql`
4. Проверьте порт: `sudo netstat -tlnp | grep 8080`

### Ошибки базы данных

```bash
# Проверьте подключение
mysql -u doska_user -p newdoska

# Проверьте права
SHOW GRANTS FOR 'doska_user'@'localhost';
```

### Проблемы с обновлением

1. Проверьте права на директории: `ls -la /opt/doska`
2. Проверьте скрипт перезапуска: `./restart.sh`
3. Проверьте systemd сервис: `sudo systemctl status doska`

## Безопасность

### Firewall

```bash
# Разрешить только HTTP/HTTPS
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

### Резервное копирование

```bash
# Создайте скрипт бэкапа
nano /opt/doska/backup.sh
```

```bash
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
mysqldump -u doska_user -p newdoska > /opt/doska/backups/db_$DATE.sql
tar -czf /opt/doska/backups/files_$DATE.tar.gz /opt/doska/app.jar /opt/doska/application.properties
```

Добавьте в crontab:
```bash
crontab -e
# Каждый день в 2:00
0 2 * * * /opt/doska/backup.sh
```

## Готово!

Приложение установлено и готово к работе. Доступ: `http://ваш_сервер:8080`


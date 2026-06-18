# 🔧 Устранение проблем с обновлениями

## ❌ Ошибка: "TemplateInputException" на строке 106

**Проблема:** Внешний файл шаблона на сервере не обновлен.

**Решение:**

### Вариант 1: Исправить вручную на сервере

```bash
# На сервере
sudo nano /opt/ruomi/external-resources/templates/admin/updates.html
```

Найдите строку 106 и замените:
```html
<!-- Было (неправильно): -->
<span th:utext="${#strings.replace(latestRelease.releaseNotes, T(java.lang.String).valueOf('\n'), '<br>')}"></span>

<!-- Стало (правильно): -->
<div style="white-space: pre-line;" th:text="${latestRelease.releaseNotes}"></div>
```

### Вариант 2: Использовать скрипт

```bash
# На сервере
chmod +x fix-updates-template.sh
sudo ./fix-updates-template.sh
```

### Вариант 3: Обновить все внешние ресурсы

```bash
# На сервере
chmod +x update-external-resources.sh
sudo ./update-external-resources.sh
```

## ❌ Обновления не работают

### Проверка 1: Настройки GitHub

Убедитесь, что в `application.properties` правильно настроены:

```properties
app.update.github.owner=mifedweb
app.update.github.repo=ruomi
```

### Проверка 2: Доступ к GitHub API

Проверьте доступность GitHub API:

```bash
curl https://api.github.com/repos/mifedweb/ruomi/releases/latest
```

Должен вернуть JSON с информацией о релизе.

### Проверка 3: Git НЕ нужен!

**Важно:** Для обновления через веб-интерфейс Git НЕ нужен! 

Приложение использует только REST API GitHub для:
- ✅ Проверки новых версий
- ✅ Скачивания JAR файлов
- ✅ Скачивания ресурсов (ZIP)

Git нужен только для:
- Разработки (локально на вашем компьютере)
- Создания релизов (через `release.bat` на вашем компьютере)

**На сервере нужен только:**
- ✅ Java
- ✅ Доступ к интернету
- ✅ Права на запись в директории приложения

### Проверка 4: Логи обновления

Проверьте логи приложения:

```bash
sudo journalctl -u ruomi -f | grep -i update
```

Или логи в файле:

```bash
tail -f /opt/ruomi/logs/application.log | grep -i update
```

### Проверка 5: Права доступа

Убедитесь, что приложение имеет права на запись:

```bash
sudo chown -R ruomi:ruomi /opt/ruomi/updates
sudo chown -R ruomi:ruomi /opt/ruomi/backups
sudo chown -R ruomi:ruomi /opt/ruomi/external-resources
```

### Проверка 6: Путь к JAR

Проверьте, что путь к JAR правильный в `application.properties`:

```properties
app.jar.path=/opt/ruomi/app.jar
```

Или где у вас реально находится JAR файл.

## 🔍 Диагностика

### Проверка статуса обновления

```bash
# Через API
curl http://localhost:8080/admin/updates/api/status

# Или через браузер
# Откройте: http://ruomi.fi/admin/updates/api/status
```

### Проверка доступности релизов

```bash
# Через API
curl http://localhost:8080/admin/updates/api/check

# Или через браузер
# Откройте: http://ruomi.fi/admin/updates/api/check
```

### Проверка директорий

```bash
ls -la /opt/ruomi/updates/
ls -la /opt/ruomi/backups/
ls -la /opt/ruomi/external-resources/
```

## 🛠️ Ручное обновление

Если автоматическое обновление не работает:

1. **Скачайте JAR вручную:**
```bash
cd /opt/ruomi/updates
wget https://github.com/mifedweb/ruomi/releases/latest/download/doska-1.0.0.jar
```

2. **Создайте backup:**
```bash
cp /opt/ruomi/app.jar /opt/ruomi/backups/app-backup-$(date +%Y%m%d-%H%M%S).jar
```

3. **Замените JAR:**
```bash
cp /opt/ruomi/updates/doska-1.0.0.jar /opt/ruomi/app.jar
```

4. **Перезапустите:**
```bash
sudo systemctl restart ruomi
```

## 📝 Частые проблемы

### Проблема: "Не удалось получить информацию о последнем релизе"

**Причины:**
- Неправильные настройки GitHub owner/repo
- Нет доступа к интернету
- GitHub API недоступен
- Репозиторий приватный (нужен токен)

**Решение:**
1. Проверьте настройки в `application.properties`
2. Проверьте доступность: `curl https://api.github.com`
3. Если репозиторий приватный, добавьте токен:
```properties
app.update.github.token=your_github_token
```

### Проблема: "Не удалось скачать JAR файл"

**Причины:**
- Нет доступа к интернету
- Неправильный URL
- Файл не существует в релизе

**Решение:**
1. Проверьте доступность интернета
2. Проверьте, что в релизе есть JAR файл
3. Проверьте логи для деталей

### Проблема: "Не удалось перезапустить приложение"

**Причины:**
- Нет прав на systemctl
- Неправильное имя сервиса
- Скрипт перезапуска не существует

**Решение:**
1. Проверьте имя сервиса в `application.properties`:
```properties
app.update.restart.service=ruomi
```

2. Проверьте, что сервис существует:
```bash
sudo systemctl status ruomi
```

3. Если нужно, настройте sudo без пароля для systemctl (не рекомендуется для продакшена)

## ✅ Чеклист для проверки

- [ ] GitHub owner и repo правильно настроены
- [ ] GitHub API доступен (`curl https://api.github.com`)
- [ ] В релизе есть JAR файл
- [ ] Приложение имеет права на запись в директории
- [ ] Путь к JAR правильный
- [ ] Systemd сервис правильно настроен
- [ ] Логи показывают детали ошибок


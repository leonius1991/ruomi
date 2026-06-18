# ⚡ Быстрое исправление ошибки обновлений

## 🔴 Проблема

Ошибка при открытии `/admin/updates`:
```
TemplateInputException: Instantiation of new objects and access to static classes or parameters is forbidden
(template: "admin/updates" - line 106, col 39)
```

## ✅ Решение (на сервере)

### Вариант 1: Использовать скрипт (рекомендуется)

```bash
# На сервере
cd /opt/ruomi
nano fix-updates-template.sh
# Скопируйте содержимое из файла fix-updates-template.sh

chmod +x fix-updates-template.sh
sudo ./fix-updates-template.sh
```

### Вариант 2: Исправить вручную

```bash
# На сервере
sudo nano /opt/ruomi/external-resources/templates/admin/updates.html
```

Найдите строку 106 (или около неё) и замените:

**Было:**
```html
<span th:utext="${#strings.replace(latestRelease.releaseNotes, T(java.lang.String).valueOf('\n'), '<br>')}"></span>
```

**Стало:**
```html
<div class="border rounded p-2 mt-2" style="max-height: 150px; overflow-y: auto; white-space: pre-line;" th:text="${latestRelease.releaseNotes}">
</div>
```

Или просто удалите проблемную строку и оставьте:
```html
<div style="white-space: pre-line;" th:text="${latestRelease.releaseNotes}"></div>
```

### Вариант 3: Удалить внешний файл (использовать из JAR)

```bash
# На сервере
sudo rm /opt/ruomi/external-resources/templates/admin/updates.html
sudo systemctl restart ruomi
```

Приложение будет использовать шаблон из JAR файла.

## 🔍 Проверка

После исправления:

1. Перезапустите приложение:
```bash
sudo systemctl restart ruomi
```

2. Откройте в браузере:
```
https://ruomi.fi/admin/updates
```

3. Страница должна открыться без ошибок.

## 📝 Про обновления

**Важно:** Git НЕ нужен на сервере для обновлений!

- ✅ Обновления работают через REST API GitHub
- ✅ Нужен только доступ к интернету
- ✅ Git нужен только для разработки (локально)

Если обновления не работают, проверьте:
1. Настройки в `application.properties`:
   ```properties
   app.update.github.owner=mifedweb
   app.update.github.repo=ruomi
   ```

2. Доступность GitHub API:
   ```bash
   curl https://api.github.com/repos/mifedweb/ruomi/releases/latest
   ```

3. Логи приложения:
   ```bash
   sudo journalctl -u ruomi -f | grep -i update
   ```

См. подробности: `UPDATE_TROUBLESHOOTING.md`



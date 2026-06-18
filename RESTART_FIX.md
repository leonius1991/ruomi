# Исправление проблемы с автоматическим перезапуском

## Проблема

При обновлении приложения через админ-панель возникает ошибка:
```
Failed to stop ruomi.service: Interactive authentication required.
Failed to start ruomi.service: Interactive authentication required.
```

Приложение не перезапускается автоматически.

## Решение

### Вариант 1: Настройка sudoers (рекомендуется)

1. **Определите пользователя, под которым запущено приложение:**
   ```bash
   ps aux | grep java | grep ruomi
   ```
   Обычно это пользователь `ruomi`.

2. **Настройте sudoers для автоматического перезапуска:**
   ```bash
   sudo visudo -f /etc/sudoers.d/ruomi-restart
   ```

3. **Добавьте следующие строки (замените `ruomi` на имя вашего пользователя):**
   ```
   ruomi ALL=(ALL) NOPASSWD: /bin/systemctl restart ruomi
   ruomi ALL=(ALL) NOPASSWD: /bin/systemctl stop ruomi
   ruomi ALL=(ALL) NOPASSWD: /bin/systemctl start ruomi
   ruomi ALL=(ALL) NOPASSWD: /bin/systemctl status ruomi
   ```

4. **Сохраните файл** (в visudo: нажмите `Esc`, затем `:wq` и `Enter`)

5. **Установите правильные права:**
   ```bash
   sudo chmod 0440 /etc/sudoers.d/ruomi-restart
   ```

6. **Проверьте синтаксис:**
   ```bash
   sudo visudo -c -f /etc/sudoers.d/ruomi-restart
   ```

### Вариант 2: Использование готового скрипта

1. **Скопируйте скрипт на сервер:**
   ```bash
   # Если скрипт уже есть в проекте
   chmod +x setup-sudoers.sh
   ```

2. **Запустите скрипт от root:**
   ```bash
   sudo bash setup-sudoers.sh
   ```

   Скрипт автоматически:
   - Определит пользователя из переменной окружения `APP_USER` (по умолчанию `ruomi`)
   - Создаст файл `/etc/sudoers.d/ruomi-restart`
   - Установит правильные права
   - Проверит синтаксис

### Вариант 3: Запуск приложения от root (не рекомендуется)

Если приложение запущено от root, то `sudo` не требуется, но это небезопасно.

## Проверка

После настройки sudoers проверьте:

1. **Проверьте, что пользователь может перезапустить сервис:**
   ```bash
   sudo -u ruomi sudo systemctl restart ruomi
   ```

2. **Проверьте статус сервиса:**
   ```bash
   sudo systemctl status ruomi
   ```

3. **Попробуйте обновление через админ-панель:**
   - Перейдите в `/admin/updates`
   - Нажмите "Обновить"
   - Проверьте логи - должно быть "Сервис успешно перезапущен"

## Обновленный скрипт restart.sh

Скрипт `restart.sh` был обновлен и теперь:
- Автоматически пытается использовать `sudo` если доступен
- Проверяет статус сервиса после перезапуска
- Выводит понятные сообщения об ошибках

## Важные замечания

1. **Безопасность:** Файл sudoers должен иметь права `0440` и принадлежать `root:root`

2. **Проверка синтаксиса:** Всегда проверяйте синтаксис sudoers файлов перед сохранением

3. **Логирование:** После настройки sudoers проверьте логи обновления в админ-панели

4. **Альтернатива:** Если настройка sudoers невозможна, можно перезапускать приложение вручную после обновления

## Устранение неполадок

### Ошибка: "sudo: no tty present and no askpass program specified"

Это означает, что sudoers не настроен правильно. Проверьте:
- Правильность синтаксиса в `/etc/sudoers.d/ruomi-restart`
- Права доступа к файлу (должны быть `0440`)
- Имя пользователя в файле совпадает с пользователем приложения

### Ошибка: "systemctl: command not found"

Убедитесь, что systemd установлен и доступен:
```bash
which systemctl
systemctl --version
```

### Сервис не перезапускается, но ошибок нет

Проверьте:
1. Действительно ли сервис перезапустился:
   ```bash
   sudo systemctl status ruomi
   ```
   Посмотрите на время "Active since" - оно должно обновиться

2. Проверьте PID процесса:
   ```bash
   ps aux | grep java | grep ruomi
   ```
   PID должен измениться после перезапуска

3. Проверьте логи приложения:
   ```bash
   sudo journalctl -u ruomi -n 50
   ```



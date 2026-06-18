# 🌐 Настройка DNS для ruomi.fi

## Шаг 1: Получение IP адреса сервера

На вашем Linux сервере выполните:

```bash
curl ifconfig.me
# или
hostname -I
```

Запишите IP адрес вашего сервера.

## Шаг 2: Настройка DNS записей

Войдите в панель управления вашего регистратора домена (где вы купили ruomi.fi).

### Необходимые DNS записи:

#### A записи (IPv4):

| Тип | Имя | Значение | TTL |
|-----|-----|----------|-----|
| A | @ | IP_ВАШЕГО_СЕРВЕРА | 3600 |
| A | www | IP_ВАШЕГО_СЕРВЕРА | 3600 |

**Пример:**
- Имя: `@` или пустое, Значение: `123.45.67.89`
- Имя: `www`, Значение: `123.45.67.89`

#### AAAA записи (IPv6) - опционально:

Если у вашего сервера есть IPv6 адрес:

| Тип | Имя | Значение | TTL |
|-----|-----|----------|-----|
| AAAA | @ | IPv6_АДРЕС | 3600 |
| AAAA | www | IPv6_АДРЕС | 3600 |

## Шаг 3: Популярные регистраторы

### Namecheap

1. Войдите в аккаунт Namecheap
2. Перейдите в "Domain List"
3. Нажмите "Manage" рядом с ruomi.fi
4. Перейдите в "Advanced DNS"
5. Добавьте записи:
   - **Host**: `@`, **Type**: `A Record`, **Value**: `IP_СЕРВЕРА`, **TTL**: Automatic
   - **Host**: `www`, **Type**: `A Record`, **Value**: `IP_СЕРВЕРА`, **TTL**: Automatic

### GoDaddy

1. Войдите в аккаунт GoDaddy
2. Перейдите в "My Products" → "DNS"
3. В разделе "Records" добавьте:
   - **Type**: `A`, **Name**: `@`, **Value**: `IP_СЕРВЕРА`, **TTL**: 600
   - **Type**: `A`, **Name**: `www`, **Value**: `IP_СЕРВЕРА`, **TTL**: 600

### Cloudflare

1. Войдите в Cloudflare
2. Выберите домен ruomi.fi
3. Перейдите в "DNS" → "Records"
4. Добавьте записи:
   - **Type**: `A`, **Name**: `@`, **IPv4 address**: `IP_СЕРВЕРА`, **Proxy status**: Proxied (оранжевое облако)
   - **Type**: `A`, **Name**: `www`, **IPv4 address**: `IP_СЕРВЕРА`, **Proxy status**: Proxied

**Важно для Cloudflare:** Если используете Cloudflare Proxy, убедитесь, что в Nginx настроен правильный IP через `X-Forwarded-For`.

### Freenom

1. Войдите в Freenom
2. Перейдите в "Services" → "My Domains"
3. Нажмите "Manage Domain" для ruomi.fi
4. Перейдите в "Manage Freenom DNS"
5. Добавьте записи:
   - **Type**: `A`, **Name**: `@`, **Target**: `IP_СЕРВЕРА`, **TTL**: 3600
   - **Type**: `A`, **Name**: `www`, **Target**: `IP_СЕРВЕРА`, **TTL**: 3600

## Шаг 4: Проверка DNS

После настройки DNS подождите 5-15 минут и проверьте:

```bash
# На сервере
dig ruomi.fi
nslookup ruomi.fi
host ruomi.fi

# Должен вернуться IP вашего сервера
```

Или используйте онлайн сервисы:
- https://dnschecker.org
- https://www.whatsmydns.net

## Шаг 5: Запуск скрипта настройки

После того, как DNS записи настроены и проверены:

```bash
# На сервере
sudo ./setup-domain.sh
```

Скрипт:
1. Установит Nginx (если не установлен)
2. Создаст конфигурацию для ruomi.fi
3. Установит SSL сертификат (Let's Encrypt)
4. Настроит автообновление сертификатов
5. Настроит Firewall

## Шаг 6: Проверка работы

После выполнения скрипта:

1. Проверьте доступность:
   ```bash
   curl -I https://ruomi.fi
   ```

2. Откройте в браузере:
   - https://ruomi.fi
   - https://www.ruomi.fi

3. Проверьте SSL:
   - https://www.ssllabs.com/ssltest/analyze.html?d=ruomi.fi

## Устранение неполадок

### DNS не обновился

- Подождите до 48 часов (обычно 5-30 минут)
- Очистите DNS кеш: `sudo systemd-resolve --flush-caches`
- Проверьте на разных DNS серверах

### Ошибка "Connection refused"

- Убедитесь, что приложение запущено: `sudo systemctl status ruomi`
- Проверьте порт: `sudo netstat -tlnp | grep 8080`
- Проверьте firewall: `sudo ufw status`

### SSL сертификат не получен

- Убедитесь, что DNS настроен правильно
- Проверьте, что порт 80 открыт
- Выполните вручную: `sudo certbot --nginx -d ruomi.fi -d www.ruomi.fi`

### Nginx ошибки

- Проверьте логи: `sudo tail -f /var/log/nginx/ruomi.fi_error.log`
- Проверьте конфигурацию: `sudo nginx -t`
- Перезагрузите: `sudo systemctl reload nginx`

## Дополнительные настройки

### Поддомены

Если нужны поддомены (например, api.ruomi.fi):

1. Добавьте DNS запись:
   - **Type**: `A`, **Name**: `api`, **Value**: `IP_СЕРВЕРА`

2. Создайте конфигурацию Nginx для поддомена:
   ```bash
   sudo cp /etc/nginx/sites-available/ruomi.fi /etc/nginx/sites-available/api.ruomi.fi
   sudo nano /etc/nginx/sites-available/api.ruomi.fi
   # Измените server_name на api.ruomi.fi
   sudo ln -s /etc/nginx/sites-available/api.ruomi.fi /etc/nginx/sites-enabled/
   sudo certbot --nginx -d api.ruomi.fi
   sudo systemctl reload nginx
   ```

### Email записи (опционально)

Если нужна почта на домене:

| Тип | Имя | Значение | TTL |
|-----|-----|----------|-----|
| MX | @ | mail.ruomi.fi | 3600 |
| A | mail | IP_ПОЧТОВОГО_СЕРВЕРА | 3600 |

## Готово!

После выполнения всех шагов ваш домен ruomi.fi будет работать с SSL сертификатом.



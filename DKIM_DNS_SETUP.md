# 🔐 Настройка DKIM записи в DNS

## ⚠️ Проблема с кавычками

DKIM ключ очень длинный, и DNS провайдеры требуют правильного формата.

## ✅ Правильный способ добавления DKIM

### Вариант 1: Без кавычек (рекомендуется)

DNS провайдеры автоматически добавят кавычки. Просто скопируйте значение **БЕЗ кавычек**:

```
default._domainkey.ruomi.fi.    TXT    v=DKIM1; h=sha256; k=rsa; p=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtoLzdnx480SuQfuUZiDSkl86tKuOvIm3OX3rXyEeG09u5L+zmaypAXTmR9uesBh/uhbqu3dMav48O+q9ktdpp/8hgIVwR+H/f2PXgNMG8I3XiaQPl9LYw+oQnslRQEm2uZBSi0PrqE6YfrUpB6hCz75bjBS/ts4Ksx0+pi1BCzQkjlgmQVw0b1Vu7neoX3puJhx36PB2/CfpYVNK5PXcZEJ7gka2vfArS+YrZqFlmiEQPPinJ1eOaW7IWFMArUD9NuJXsHlC2WQp1YcF/Bz73eE0r+F5c2CA9iO2t3JcCsFCI5kZIKzpCZqsqIVt/Dfb3pTqc2sHUk4y4qDXYuaTgQIDAQAB
```

### Вариант 2: Если нужны кавычки

Если DNS провайдер требует кавычки, используйте **одинарные** кавычки или экранируйте:

```
default._domainkey.ruomi.fi.    TXT    "v=DKIM1; h=sha256; k=rsa; p=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtoLzdnx480SuQfuUZiDSkl86tKuOvIm3OX3rXyEeG09u5L+zmaypAXTmR9uesBh/uhbqu3dMav48O+q9ktdpp/8hgIVwR+H/f2PXgNMG8I3XiaQPl9LYw+oQnslRQEm2uZBSi0PrqE6YfrUpB6hCz75bjBS/ts4Ksx0+pi1BCzQkjlgmQVw0b1Vu7neoX3puJhx36PB2/CfpYVNK5PXcZEJ7gka2vfArS+YrZqFlmiEQPPinJ1eOaW7IWFMArUD9NuJXsHlC2WQp1YcF/Bz73eE0r+F5c2CA9iO2t3JcCsFCI5kZIKzpCZqsqIVt/Dfb3pTqc2sHUk4y4qDXYuaTgQIDAQAB"
```

**Важно:** Не используйте двойные кавычки внутри двойных кавычек!

### Вариант 3: Разбить на несколько строк (если DNS провайдер требует)

Некоторые DNS провайдеры позволяют разбить длинную TXT запись на несколько строк:

```
default._domainkey.ruomi.fi.    TXT    "v=DKIM1; h=sha256; k=rsa; p=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtoLzdnx480SuQfuUZiDSkl86tKuOvIm3OX3rXyEeG09u5L+zmaypAXTmR9uesBh/uhbqu3dMav48O+q9ktdpp/8hgIVwR+H/f2PXgNMG8I3XiaQPl9LYw+oQnslRQEm2uZBSi0PrqE6YfrUpB6hCz75bjBS/ts4Ksx0+pi1BCzQkjlgmQVw0b1Vu7neoX3puJhx36PB2/CfpYV"
default._domainkey.ruomi.fi.    TXT    "NK5PXcZEJ7gka2vfArS+YrZqFlmiEQPPinJ1eOaW7IWFMArUD9NuJXsHlC2WQp1YcF/Bz73eE0r+F5c2CA9iO2t3JcCsFCI5kZIKzpCZqsqIVt/Dfb3pTqc2sHUk4y4qDXYuaTgQIDAQAB"
```

## 📝 Как получить DKIM ключ

После установки почтового сервера:

```bash
# Просмотр DKIM ключа
cat /etc/opendkim/keys/ruomi.fi/default.txt

# Или только значение p=
grep -oP 'p=[^"]+' /etc/opendkim/keys/ruomi.fi/default.txt
```

## 🔍 Проверка после добавления

```bash
# Проверка DKIM записи
dig TXT default._domainkey.ruomi.fi

# Должно вернуть полный ключ
```

## ⚠️ Частые ошибки

1. **Двойные кавычки внутри кавычек** - используйте одинарные или уберите внутренние
2. **Слишком длинная строка** - разбейте на несколько TXT записей
3. **Пробелы в ключе** - убедитесь, что ключ идет без пробелов
4. **Неправильный формат** - должно быть: `v=DKIM1; h=sha256; k=rsa; p=...`



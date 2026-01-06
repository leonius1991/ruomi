# 📦 Руководство по созданию релизов

## Быстрый старт

### Windows
```bash
release.bat
```

### Linux/Mac
```bash
chmod +x release.sh
./release.sh
```

## Предварительные требования

### Вариант 1: GitHub CLI (рекомендуется)

1. Установите GitHub CLI:
   - Windows: `winget install GitHub.cli` или скачайте с [cli.github.com](https://cli.github.com)
   - Linux: `sudo apt install gh` или `sudo yum install gh`
   - Mac: `brew install gh`

2. Авторизуйтесь:
   ```bash
   gh auth login
   ```

### Вариант 2: GitHub Personal Access Token

1. Создайте токен на [github.com/settings/tokens](https://github.com/settings/tokens)
2. Выберите права: `repo` (полный доступ к репозиториям)
3. Скопируйте токен (он понадобится при запуске скрипта)

## Использование

### Автоматический режим (с GitHub CLI)

1. Запустите скрипт:
   ```bash
   # Windows
   release.bat
   
   # Linux/Mac
   ./release.sh
   ```

2. Введите тег релиза (например, `v1.0.0` или `1.0.0`)
3. Введите название релиза (или нажмите Enter для использования тега)
4. Введите описание (или нажмите Enter для пропуска)

Скрипт автоматически:
- ✅ Соберет проект (`mvn clean package`)
- ✅ Создаст релиз на GitHub
- ✅ Загрузит JAR файл в релиз

### Ручной режим (с curl)

Если GitHub CLI не установлен, скрипт попросит:
1. GitHub Personal Access Token
2. GitHub username (если не указан в `application.properties`)
3. Название репозитория (если не указано в `application.properties`)

## Настройка

### Автоматическое определение репозитория

Скрипт автоматически читает настройки из `application.properties`:

```properties
app.update.github.owner=mifedweb
app.update.github.repo=ruomi
```

Если эти настройки не указаны, скрипт запросит их вручную.

## Примеры использования

### Простой релиз
```bash
./release.sh
# Тег: v1.0.0
# Название: (Enter)
# Описание: (Enter)
```

### Релиз с описанием
```bash
./release.sh
# Тег: v1.2.0
# Название: Версия 1.2.0 - Новые функции
# Описание: Добавлена система обновлений, улучшена производительность
```

## Структура релиза

После выполнения скрипта на GitHub будет создан релиз со следующей структурой:

```
v1.0.0
├── Название: v1.0.0 (или указанное вами)
├── Описание: (ваше описание)
└── Assets:
    └── doska-0.0.1-SNAPSHOT.jar
```

## Устранение неполадок

### Ошибка "Maven не установлен"
- Установите Maven или используйте Maven wrapper (`mvnw`)

### Ошибка "GitHub CLI не установлен"
- Установите GitHub CLI или используйте режим с curl (укажите токен)

### Ошибка "Вы не авторизованы в GitHub CLI"
```bash
gh auth login
```

### Ошибка "Токен обязателен"
- Создайте Personal Access Token на GitHub
- Выберите права: `repo`

### Ошибка "Релиз уже существует"
- Используйте другой тег
- Или удалите существующий релиз на GitHub

### Ошибка "JAR файл не найден"
- Убедитесь, что сборка прошла успешно
- Проверьте путь к JAR файлу в `target/`

## Интеграция с CI/CD

### GitHub Actions

Создайте `.github/workflows/release.yml`:

```yaml
name: Release

on:
  push:
    tags:
      - 'v*'

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Build with Maven
        run: mvn clean package -DskipTests
      
      - name: Create Release
        uses: softprops/action-gh-release@v1
        with:
          files: target/doska-*.jar
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

## Безопасность

⚠️ **Важно:**
- Не коммитьте GitHub токены в репозиторий
- Используйте GitHub Secrets для CI/CD
- Храните токены в безопасном месте
- Регулярно обновляйте токены

## Дополнительные возможности

### Автоматическое версионирование

Можно настроить автоматическое увеличение версии в `pom.xml` перед релизом.

### Множественные файлы

Скрипт можно модифицировать для загрузки нескольких файлов (например, JAR + документация).

### Pre-release скрипты

Добавьте проверки перед созданием релиза (тесты, линтеры и т.д.).


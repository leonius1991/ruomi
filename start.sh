#!/bin/bash

# NewDoska - Доска объявлений
# Скрипт запуска для Linux/Mac

echo "========================================"
echo "    NewDoska - Доска объявлений"
echo "========================================"
echo

# Проверка Java
echo "Проверка Java..."
if ! command -v java &> /dev/null; then
    echo "ОШИБКА: Java не установлена или не найдена в PATH"
    echo "Пожалуйста, установите Java 21 или выше"
    exit 1
fi

# Проверка версии Java
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "ОШИБКА: Требуется Java 21 или выше, текущая версия: $JAVA_VERSION"
    exit 1
fi

echo "Java версия: $(java -version 2>&1 | head -n 1)"

# Проверка Maven
echo "Проверка Maven..."
if ! command -v mvn &> /dev/null; then
    echo "ОШИБКА: Maven не установлен или не найден в PATH"
    echo "Пожалуйста, установите Maven 3.6 или выше"
    exit 1
fi

echo "Maven версия: $(mvn -version | head -n 1)"

# Проверка MySQL
echo "Проверка MySQL..."
if ! command -v mysql &> /dev/null; then
    echo "ПРЕДУПРЕЖДЕНИЕ: MySQL не найден в PATH"
    echo "Убедитесь, что MySQL запущен и доступен"
    echo
fi

echo
echo "Запуск NewDoska..."
echo

# Компиляция проекта
echo "Компиляция проекта..."
if ! mvn clean compile; then
    echo "ОШИБКА: Ошибка компиляции"
    exit 1
fi

echo
echo "Запуск приложения..."
echo "Приложение будет доступно по адресу: http://localhost:8080"
echo "Для остановки нажмите Ctrl+C"
echo

# Запуск приложения
mvn spring-boot:run

echo
echo "Приложение остановлено" 
-- SQL скрипт для добавления существующих категорий в БД
-- Выполнить после создания таблиц

-- Добавление категорий
INSERT INTO categories (name, display_name, icon, sort_order, active) VALUES
('REAL_ESTATE', 'Недвижимость', 'fa-house', 1, true),
('VEHICLES', 'Транспорт', 'fa-car', 2, true),
('ELECTRONICS', 'Электроника', 'fa-plug', 3, true),
('SERVICES', 'Услуги', 'fa-briefcase', 4, true),
('JOBS', 'Работа', 'fa-briefcase', 5, true),
('FURNITURE', 'Дом и быт', 'fa-couch', 6, true),
('CLOTHING', 'Одежда', 'fa-tshirt', 7, true),
('BOOKS', 'Книги', 'fa-book', 8, true),
('SPORTS', 'Спорт', 'fa-futbol', 9, true),
('OTHER', 'Прочее', 'fa-box', 10, true)
ON DUPLICATE KEY UPDATE display_name=VALUES(display_name), icon=VALUES(icon), sort_order=VALUES(sort_order);

-- Получаем ID категорий для использования в подкатегориях
SET @real_estate_id = (SELECT id FROM categories WHERE name = 'REAL_ESTATE');
SET @vehicles_id = (SELECT id FROM categories WHERE name = 'VEHICLES');
SET @electronics_id = (SELECT id FROM categories WHERE name = 'ELECTRONICS');
SET @services_id = (SELECT id FROM categories WHERE name = 'SERVICES');
SET @jobs_id = (SELECT id FROM categories WHERE name = 'JOBS');
SET @furniture_id = (SELECT id FROM categories WHERE name = 'FURNITURE');

-- Добавление подкатегорий для Недвижимость
INSERT INTO subcategories (name, display_name, icon, category_id, sort_order, active) VALUES
('apartments', 'Квартиры', 'fa-building', @real_estate_id, 1, true),
('houses', 'Дома', 'fa-home', @real_estate_id, 2, true),
('commercial', 'Коммерческая', 'fa-store', @real_estate_id, 3, true),
('rent', 'Аренда', 'fa-key', @real_estate_id, 4, true)
ON DUPLICATE KEY UPDATE display_name=VALUES(display_name), icon=VALUES(icon), sort_order=VALUES(sort_order);

-- Добавление подкатегорий для Транспорт
INSERT INTO subcategories (name, display_name, icon, category_id, sort_order, active) VALUES
('cars', 'Легковые', 'fa-car', @vehicles_id, 1, true),
('trucks', 'Коммерческие', 'fa-truck', @vehicles_id, 2, true),
('motorcycles', 'Мото/скутеры', 'fa-motorcycle', @vehicles_id, 3, true),
('boats', 'Водный транспорт', 'fa-ship', @vehicles_id, 4, true)
ON DUPLICATE KEY UPDATE display_name=VALUES(display_name), icon=VALUES(icon), sort_order=VALUES(sort_order);

-- Добавление подкатегорий для Электроника
INSERT INTO subcategories (name, display_name, icon, category_id, sort_order, active) VALUES
('smartphones', 'Смартфоны', 'fa-mobile-alt', @electronics_id, 1, true),
('laptops', 'Ноутбуки', 'fa-laptop', @electronics_id, 2, true),
('audio_video', 'Аудио/Видео', 'fa-headphones', @electronics_id, 3, true),
('smart_home', 'Умный дом', 'fa-home', @electronics_id, 4, true)
ON DUPLICATE KEY UPDATE display_name=VALUES(display_name), icon=VALUES(icon), sort_order=VALUES(sort_order);

-- Добавление подкатегорий для Услуги
INSERT INTO subcategories (name, display_name, icon, category_id, sort_order, active) VALUES
('repair_construction', 'Ремонт и стройка', 'fa-tools', @services_id, 1, true),
('beauty_health', 'Красота и здоровье', 'fa-spa', @services_id, 2, true),
('education', 'Образование', 'fa-graduation-cap', @services_id, 3, true),
('it_marketing', 'IT и маркетинг', 'fa-code', @services_id, 4, true)
ON DUPLICATE KEY UPDATE display_name=VALUES(display_name), icon=VALUES(icon), sort_order=VALUES(sort_order);

-- Добавление подкатегорий для Работа
INSERT INTO subcategories (name, display_name, icon, category_id, sort_order, active) VALUES
('job_seeking', 'Ищу работу', 'fa-user-tie', @jobs_id, 1, true),
('job_offering', 'Предлагаю работу', 'fa-briefcase', @jobs_id, 2, true),
('full_time', 'Полная занятость', 'fa-clock', @jobs_id, 3, true),
('part_time', 'Частичная занятость', 'fa-clock', @jobs_id, 4, true),
('remote', 'Удалённо', 'fa-laptop-house', @jobs_id, 5, true),
('internship', 'Стажировки', 'fa-user-graduate', @jobs_id, 6, true),
('contract', 'Контракт', 'fa-file-contract', @jobs_id, 7, true),
('temporary', 'Временная работа', 'fa-calendar-alt', @jobs_id, 8, true)
ON DUPLICATE KEY UPDATE display_name=VALUES(display_name), icon=VALUES(icon), sort_order=VALUES(sort_order);

-- Добавление подкатегорий для Дом и быт
INSERT INTO subcategories (name, display_name, icon, category_id, sort_order, active) VALUES
('furniture', 'Мебель', 'fa-couch', @furniture_id, 1, true),
('appliances', 'Бытовая техника', 'fa-blender', @furniture_id, 2, true),
('decor', 'Декор', 'fa-palette', @furniture_id, 3, true),
('garden', 'Сад и дача', 'fa-seedling', @furniture_id, 4, true)
ON DUPLICATE KEY UPDATE display_name=VALUES(display_name), icon=VALUES(icon), sort_order=VALUES(sort_order);

-- Добавление типов объявлений (для продажи/обмена)
INSERT INTO advertisement_types (name, display_name, category_id, sort_order, active) VALUES
('SALE', 'Продажа', NULL, 1, true),
('EXCHANGE', 'Обмен', NULL, 2, true),
('RENT', 'Аренда', NULL, 3, true),
('GIFT', 'Даром', NULL, 4, true)
ON DUPLICATE KEY UPDATE display_name=VALUES(display_name), sort_order=VALUES(sort_order);

-- Добавление типов для категории Работа
INSERT INTO advertisement_types (name, display_name, category_id, sort_order, active) VALUES
('JOB_SEEKING', 'Ищу работу', @jobs_id, 1, true),
('JOB_OFFERING', 'Предлагаю работу', @jobs_id, 2, true),
('FULL_TIME', 'Полная занятость', @jobs_id, 3, true),
('PART_TIME', 'Частичная занятость', @jobs_id, 4, true),
('REMOTE', 'Удалённо', @jobs_id, 5, true),
('INTERNSHIP', 'Стажировки', @jobs_id, 6, true),
('CONTRACT', 'Контракт', @jobs_id, 7, true),
('TEMPORARY', 'Временная работа', @jobs_id, 8, true)
ON DUPLICATE KEY UPDATE display_name=VALUES(display_name), sort_order=VALUES(sort_order);



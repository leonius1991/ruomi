CREATE TABLE IF NOT EXISTS app_settings (
    setting_key   VARCHAR(64)  PRIMARY KEY,
    setting_value VARCHAR(512) NOT NULL,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO app_settings (setting_key, setting_value) VALUES ('doska.import.enabled', 'true')
ON DUPLICATE KEY UPDATE setting_value = setting_value;

CREATE TABLE IF NOT EXISTS site_news (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(200)  NOT NULL,
    summary     VARCHAR(500),
    content     TEXT          NOT NULL,
    published   TINYINT(1)    NOT NULL DEFAULT 1,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      NULL ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO seo_metadata (page_key, title, description, keywords) VALUES
('home', 'ruomi.fi — объявления для русскоязычных в Финляндии',
 'Купить, продать, снять жильё, найти работу и услуги на русском языке. Доска объявлений ruomi.fi по всей Финляндии.',
 'ruomi, объявления, финляндия, русскоязычные, купить, продать, аренда, работа'),
('advertisements', 'Все объявления — ruomi.fi',
 'Каталог объявлений ruomi.fi: недвижимость, транспорт, электроника, услуги и работа в Финляндии.',
 'объявления финляндия, доска объявлений, ruomi'),
('about', 'О проекте ruomi.fi',
 'ruomi.fi — современная доска объявлений для русскоязычного сообщества в Финляндии.',
 'ruomi, о нас, доска объявлений'),
('contact', 'Контакты — ruomi.fi',
 'Связаться с администрацией ruomi.fi: вопросы, предложения, поддержка пользователей.',
 'контакты ruomi, поддержка'),
('border-queues', 'Очередь на границе Koidula и Luhamaa — ruomi.fi',
 'Live queue и статистика проезда через границу Эstonia–Россия. Koidula, Luhamaa, полосы A/B, BC, C, CE, D.',
 'очередь на границе, koidula, luhamaa, estonia border, запись на границу'),
('help', 'Помощь — ruomi.fi',
 'Как разместить объявление, правила модерации и ответы на частые вопросы ruomi.fi.',
 'помощь ruomi, как разместить объявление'),
('terms', 'Правила использования — ruomi.fi',
 'Условия использования доски объявлений ruomi.fi.',
 'правила ruomi'),
('privacy', 'Политика конфиденциальности — ruomi.fi',
 'Как ruomi.fi обрабатывает персональные данные пользователей.',
 'конфиденциальность ruomi'),
('login', 'Вход — ruomi.fi', 'Войдите в аккаунт ruomi.fi.', 'вход ruomi'),
('register', 'Регистрация — ruomi.fi', 'Создайте аккаунт на ruomi.fi.', 'регистрация ruomi')
ON DUPLICATE KEY UPDATE title = VALUES(title), description = VALUES(description), keywords = VALUES(keywords);

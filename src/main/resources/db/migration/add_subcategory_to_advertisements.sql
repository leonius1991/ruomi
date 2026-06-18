-- Добавление поля subcategory_id в таблицу advertisements
ALTER TABLE advertisements ADD COLUMN subcategory_id BIGINT NULL;
ALTER TABLE advertisements ADD CONSTRAINT fk_advertisement_subcategory 
    FOREIGN KEY (subcategory_id) REFERENCES subcategories(id) ON DELETE SET NULL;



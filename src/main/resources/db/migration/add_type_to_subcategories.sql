-- Добавление поля advertisement_type_id в таблицу subcategories
ALTER TABLE subcategories ADD COLUMN advertisement_type_id BIGINT NULL;
ALTER TABLE subcategories ADD CONSTRAINT fk_subcategory_advertisement_type 
    FOREIGN KEY (advertisement_type_id) REFERENCES advertisement_types(id) ON DELETE SET NULL;

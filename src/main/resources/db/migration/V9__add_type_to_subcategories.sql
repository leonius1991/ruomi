-- Тип объявления для подкатегории (безопасно, если колонка уже есть)
SET @db := DATABASE();

SET @col_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'subcategories'
      AND COLUMN_NAME = 'advertisement_type_id'
);

SET @sql := IF(
    @col_exists = 0,
    'ALTER TABLE subcategories ADD COLUMN advertisement_type_id BIGINT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'subcategories'
      AND CONSTRAINT_NAME = 'fk_subcategory_advertisement_type'
);

SET @sql := IF(
    @fk_exists = 0,
    'ALTER TABLE subcategories ADD CONSTRAINT fk_subcategory_advertisement_type FOREIGN KEY (advertisement_type_id) REFERENCES advertisement_types(id) ON DELETE SET NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

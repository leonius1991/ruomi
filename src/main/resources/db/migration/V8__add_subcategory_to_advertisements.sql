-- Связь объявления с подкатегорией (безопасно, если колонка уже есть)
SET @db := DATABASE();

SET @col_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'advertisements'
      AND COLUMN_NAME = 'subcategory_id'
);

SET @sql := IF(
    @col_exists = 0,
    'ALTER TABLE advertisements ADD COLUMN subcategory_id BIGINT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'advertisements'
      AND CONSTRAINT_NAME = 'fk_advertisement_subcategory'
);

SET @sql := IF(
    @fk_exists = 0,
    'ALTER TABLE advertisements ADD CONSTRAINT fk_advertisement_subcategory FOREIGN KEY (subcategory_id) REFERENCES subcategories(id) ON DELETE SET NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

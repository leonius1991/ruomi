-- Показывать телефон продавца в объявлении (безопасно, если колонка уже есть)

SET @db := DATABASE();



SET @col_exists := (

    SELECT COUNT(*)

    FROM INFORMATION_SCHEMA.COLUMNS

    WHERE TABLE_SCHEMA = @db

      AND TABLE_NAME = 'advertisements'

      AND COLUMN_NAME = 'show_phone'

);



SET @sql := IF(

    @col_exists = 0,

    'ALTER TABLE advertisements ADD COLUMN show_phone BOOLEAN NOT NULL DEFAULT FALSE',

    'SELECT 1'

);

PREPARE stmt FROM @sql;

EXECUTE stmt;

DEALLOCATE PREPARE stmt;


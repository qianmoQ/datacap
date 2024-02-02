# If you are upgrading to 2024.02.1 from a different version, execute the following SQL statement
# Если вы обновляетесь до версии 2024.02.1 с другой версии, выполните следующую инструкцию SQL
# 如果您是通过其他版本升级到 2024.02.1, 请执行以下 SQL 语句

USE `datacap`;

ALTER TABLE `datacap_dataset_column`
    ADD COLUMN `is_primary_key` VARCHAR(100) DEFAULT FALSE;

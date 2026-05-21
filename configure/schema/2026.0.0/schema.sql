USE
`datacap`;

-- Executor 命名统一：去掉冗余的 "Executor" 后缀，与 SPI ExecutorService.name() / ExecutorPlugin.getName() 对齐。
-- 例如 'SeatunnelExecutor' -> 'Seatunnel'，'LocalExecutor' -> 'Local'。
-- Unify executor identifier with SPI's name(): strip the redundant "Executor" suffix.

UPDATE `datacap_dataset`
SET `executor` = 'Local'
WHERE `executor` = 'LocalExecutor';

UPDATE `datacap_dataset`
SET `executor` = 'Seatunnel'
WHERE `executor` = 'SeatunnelExecutor';

ALTER TABLE `datacap_dataset`
    ALTER COLUMN `executor` SET DEFAULT 'Local';

UPDATE `datacap_workflow`
SET `executor` = 'Local'
WHERE `executor` = 'LocalExecutor';

UPDATE `datacap_workflow`
SET `executor` = 'Seatunnel'
WHERE `executor` = 'SeatunnelExecutor';

-- 同步历史新增 总数 / 已完成 / 进度 三列
-- Sync history adds total count / processed count / progress columns
ALTER TABLE `datacap_dataset_history`
    ADD COLUMN `total_count`     BIGINT        DEFAULT NULL COMMENT 'Total rows from source query, NULL if pre-count is disabled',
    ADD COLUMN `processed_count` BIGINT        DEFAULT NULL COMMENT 'Rows successfully written to target',
    ADD COLUMN `progress`        DECIMAL(5, 2) DEFAULT NULL COMMENT 'processed_count / total_count * 100, NULL when total unknown';

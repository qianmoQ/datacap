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

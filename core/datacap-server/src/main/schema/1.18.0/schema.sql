# If you are upgrading to 1.18.0 from a different version, execute the following SQL statement
# Если вы обновляетесь до версии 1.18.0 с другой версии, выполните следующую инструкцию SQL
# 如果您是通过其他版本升级到 1.18.0, 请执行以下 SQL 语句

ALTER TABLE `datacap_user`
    ADD COLUMN `avatar_configure` LONGTEXT COMMENT 'avatar configure';

UPDATE `datacap_user`
SET `avatar_configure` = '{}'
WHERE `avatar_configure` IS NULL;

UPDATE `datacap_user`
SET `editor_configure` = '{}'
WHERE `editor_configure` IS NULL;

ALTER TABLE `datacap_pipeline`
    ADD COLUMN `flow_configure` LONGTEXT COMMENT 'Flow configure';

CREATE TABLE `datacap_report`
(
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name`        VARCHAR(255),
    `active`      BOOLEAN DEFAULT TRUE,
    `create_time` DATETIME,
    `update_time` DATETIME,
    `configure`   LONGTEXT,
    `type`        VARCHAR(255)
);

CREATE TABLE `datacap_report_user_relation`
(
    `report_id` BIGINT,
    `user_id`   BIGINT
);

INSERT INTO `menus` (`name`, `code`, `description`, `url`, `group_name`, `sorted`, `type`, `parent`, `active`, `i18n_key`, `icon`, `create_time`, `update_time`, `redirect`,
                     `is_new`)
VALUES ('全局 - 管理 - 报表', 'REPORT', '全局：所有用户都可以访问
位置：顶部管理一级子菜单', '/admin/report', null, 6, 'VIEW', 3, 1, 'common.report', 'md-analytics', '2023-12-18 13:37:35', null, 0, 1);
INSERT INTO `role_menu_relation` (`role_id`, `menu_id`)
VALUES ('2', '15');

INSERT INTO `menus` (`name`, `code`, `description`, `url`, `group_name`, `sorted`, `type`, `parent`, `active`, `i18n_key`, `icon`, `create_time`, `update_time`, `redirect`,
                     `is_new`)
VALUES ('全局 - 仪表盘', 'DASHBOARD', '全局路由：所有用户都可以访问', '/console/dashboard', null, 2, 'DASHBOARD', 0, 1, 'common.dashboard', 'md-analytics', '2023-12-19 10:26:21',
        null,
        0, 1);
INSERT INTO `role_menu_relation` (`role_id`, `menu_id`)
VALUES ('1', '16');
INSERT INTO `role_menu_relation` (`role_id`, `menu_id`)
VALUES ('2', '16');

CREATE TABLE `datacap_dashboard`
(
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name`        VARCHAR(255),
    `active`      BOOLEAN DEFAULT TRUE,
    `create_time` DATETIME,
    `update_time` DATETIME,
    `configure`   LONGTEXT
);

CREATE TABLE `datacap_dashboard_user_relation`
(
    `dashboard_id` BIGINT,
    `user_id`      BIGINT
);

CREATE TABLE `datacap_dashboard_report_relation`
(
    `dashboard_id` BIGINT,
    `report_id`    BIGINT
);

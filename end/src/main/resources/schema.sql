-- 数据库初始化脚本（手动执行：mysql -uroot -p123456 < schema.sql）
CREATE DATABASE IF NOT EXISTS vue_user DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE vue_user;

CREATE TABLE IF NOT EXISTS `user` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`   VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`   VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 加密存储）',
    `nickname`   VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    `email`      VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `avatar`     VARCHAR(255) DEFAULT NULL COMMENT '头像文件地址',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- 文件表：上传的文件内容（二进制）直接保存到数据库
CREATE TABLE IF NOT EXISTS `file` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT       DEFAULT NULL COMMENT '上传者用户 ID',
    `name`        VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `content_type` VARCHAR(100) DEFAULT NULL COMMENT 'MIME 类型',
    `size`        BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
    `content`     LONGBLOB     NOT NULL COMMENT '文件二进制内容',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='文件表（内容存库）';

-- 任务表：每条任务类似一个订单，记录起止时间与完成奖励
CREATE TABLE IF NOT EXISTS `task` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`      BIGINT        NOT NULL COMMENT '所属用户 ID',
    `name`         VARCHAR(100)  NOT NULL COMMENT '任务名',
    `content`      VARCHAR(1000) DEFAULT NULL COMMENT '任务内容',
    `type`         VARCHAR(20)   NOT NULL DEFAULT 'other' COMMENT '任务类型：exercise-锻炼 work-工作 study-学习 life-生活 other-其他',
    `start_time`   DATETIME      NOT NULL COMMENT '开始时间',
    `end_time`     DATETIME      NOT NULL COMMENT '结束时间',
    `reward`       VARCHAR(100)  NOT NULL DEFAULT '' COMMENT '完成奖励',
    `status`       TINYINT       NOT NULL DEFAULT 0 COMMENT '完成状态：0-进行中 1-已完成 2-已过期',
    `completed_at` DATETIME      DEFAULT NULL COMMENT '完成时间',
    `created_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_status` (`user_id`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='任务表';

-- 已有数据库的增量变更（重复执行会报"列已存在"，可忽略）：
-- ALTER TABLE `task` ADD COLUMN `type` VARCHAR(20) NOT NULL DEFAULT 'other' COMMENT '任务类型' AFTER `content`;
-- ALTER TABLE `task` ADD COLUMN `completed_at` DATETIME DEFAULT NULL COMMENT '完成时间' AFTER `status`;
-- ALTER TABLE `task` MODIFY COLUMN `status` TINYINT NOT NULL DEFAULT 0 COMMENT '完成状态：0-进行中 1-已完成 2-已过期';
-- ALTER TABLE `task` MODIFY COLUMN `reward` VARCHAR(100) NOT NULL DEFAULT '' COMMENT '完成奖励';

-- =====================================================================
-- 团队功能相关表（团队、成员、团队任务、指派、完成记录）
-- 已有数据库请手动执行以下 5 条 CREATE TABLE 语句
-- =====================================================================

CREATE TABLE IF NOT EXISTS `team` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(64) NOT NULL COMMENT '团队名称',
    `leader_id`   BIGINT      NOT NULL COMMENT '队长用户 ID',
    `max_size`    INT         NOT NULL DEFAULT 30 COMMENT '最大人数（创建后不可改）',
    `category`    VARCHAR(20) NOT NULL DEFAULT 'other' COMMENT '团队分类：exercise/work/study/life/other',
    `status`      VARCHAR(16) NOT NULL DEFAULT 'normal' COMMENT 'normal-正常 dissolved-已解散',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_leader` (`leader_id`),
    KEY `idx_category` (`category`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='团队表';

CREATE TABLE IF NOT EXISTS `team_member` (
    `team_id`   BIGINT      NOT NULL COMMENT '团队 ID',
    `user_id`   BIGINT      NOT NULL COMMENT '用户 ID',
    `role`      VARCHAR(16) NOT NULL DEFAULT 'member' COMMENT 'leader-队长 member-成员',
    `status`    VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT 'pending-待审核 approved-已通过 rejected-已拒绝',
    `join_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入/申请时间',
    PRIMARY KEY (`team_id`, `user_id`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='团队成员表（含申请审核）';

CREATE TABLE IF NOT EXISTS `team_task` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `team_id`        BIGINT        NOT NULL COMMENT '团队 ID',
    `assignee_type`  VARCHAR(16)   NOT NULL DEFAULT 'all' COMMENT 'all-全员 assigned-指定',
    `name`           VARCHAR(100)  NOT NULL COMMENT '任务名',
    `content`        VARCHAR(1000) DEFAULT NULL COMMENT '任务内容',
    `type`           VARCHAR(20)   NOT NULL DEFAULT 'other' COMMENT '任务类型',
    `start_time`     DATETIME      NOT NULL COMMENT '开始时间',
    `end_time`       DATETIME      NOT NULL COMMENT '结束时间',
    `reward`         VARCHAR(100)  NOT NULL DEFAULT '' COMMENT '完成奖励',
    `creator_id`     BIGINT        NOT NULL COMMENT '创建者（队长）ID',
    `overall_status` VARCHAR(16)   NOT NULL DEFAULT 'ongoing' COMMENT '整体状态：ongoing-进行中 completed-已完成（不可回退）',
    `is_deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '软删除：0-否 1-是',
    `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_team` (`team_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='团队任务表';

CREATE TABLE IF NOT EXISTS `team_task_assignee` (
    `task_id` BIGINT NOT NULL COMMENT '任务 ID',
    `user_id` BIGINT NOT NULL COMMENT '被指派用户 ID',
    PRIMARY KEY (`task_id`, `user_id`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='团队任务指派表';

CREATE TABLE IF NOT EXISTS `team_task_completion` (
    `task_id`       BIGINT   NOT NULL COMMENT '任务 ID',
    `user_id`       BIGINT   NOT NULL COMMENT '完成成员 ID',
    `complete_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '完成时间',
    PRIMARY KEY (`task_id`, `user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='团队任务完成记录表';

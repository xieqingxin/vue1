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
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT        NOT NULL COMMENT '所属用户 ID',
    `name`        VARCHAR(100)  NOT NULL COMMENT '任务名',
    `content`     VARCHAR(1000) DEFAULT NULL COMMENT '任务内容',
    `start_time`  DATETIME      NOT NULL COMMENT '开始时间',
    `end_time`    DATETIME      NOT NULL COMMENT '结束时间',
    `reward`      DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '完成奖励',
    `status`      TINYINT       NOT NULL DEFAULT 0 COMMENT '完成状态：0-进行中 1-已完成',
    `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_status` (`user_id`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='任务表';

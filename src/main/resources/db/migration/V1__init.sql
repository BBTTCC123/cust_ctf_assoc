-- V1：账号域初始化，共 8 张表
-- user / role / user_role / member_profile / team / team_member / registration_invite / password_reset_token

CREATE TABLE `user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户主键',
  `username` VARCHAR(64) NOT NULL COMMENT '登录名，唯一',
  `password_hash` CHAR(60) NOT NULL COMMENT 'BCrypt 哈希，固定 60 字符',
  `nickname` VARCHAR(64) NOT NULL COMMENT '站内昵称',
  `avatar_path` VARCHAR(500) NULL COMMENT '头像路径',
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / DISABLED',
  `created_at` DATETIME(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `uk_user_username` UNIQUE (`username`),
  CONSTRAINT `chk_user_status` CHECK (`status` IN ('ACTIVE','DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户账号';

CREATE TABLE `role` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `code` VARCHAR(32) NOT NULL COMMENT '角色编码',
  `name` VARCHAR(64) NOT NULL COMMENT '显示名',
  CONSTRAINT `uk_role_code` UNIQUE (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色字典';

CREATE TABLE `user_role` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  CONSTRAINT `uk_user_role` UNIQUE (`user_id`,`role_id`),
  KEY `idx_user_role_role` (`role_id`),
  CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `role`(`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联';

CREATE TABLE `member_profile` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `real_name` VARCHAR(64) NOT NULL,
  `student_no` VARCHAR(32) NULL COMMENT '可空；非空时全站唯一',
  `college` VARCHAR(128) NOT NULL,
  `major` VARCHAR(128) NOT NULL,
  `grade` VARCHAR(32) NOT NULL,
  `phone` VARCHAR(32) NULL,
  `email` VARCHAR(254) NULL,
  `blog` VARCHAR(2048) NULL,
  `show_real_name` TINYINT(1) NOT NULL DEFAULT 0,
  `show_student_no` TINYINT(1) NOT NULL DEFAULT 0,
  `show_phone` TINYINT(1) NOT NULL DEFAULT 0,
  `show_email` TINYINT(1) NOT NULL DEFAULT 0,
  `show_blog` TINYINT(1) NOT NULL DEFAULT 0,
  CONSTRAINT `uk_member_profile_user` UNIQUE (`user_id`),
  CONSTRAINT `uk_member_profile_student_no` UNIQUE (`student_no`),
  CONSTRAINT `fk_member_profile_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实名资料与可见性开关';

CREATE TABLE `team` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `leader_id` BIGINT NOT NULL,
  `intro` TEXT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  CONSTRAINT `uk_team_name` UNIQUE (`name`),
  KEY `idx_team_leader` (`leader_id`),
  CONSTRAINT `fk_team_leader` FOREIGN KEY (`leader_id`) REFERENCES `user`(`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='战队';

CREATE TABLE `team_member` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `team_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `left_at` DATETIME(0) NULL COMMENT 'NULL 表示仍在队',
  `active_key` VARCHAR(80) GENERATED ALWAYS AS
      (IF(`left_at` IS NULL, CONCAT(`team_id`, ':', `user_id`), NULL)) STORED
      COMMENT '在队唯一键：只有 left_at 为空的行才生成值，保证同一人同时只能在一处在队',
  KEY `idx_team_member_team` (`team_id`),
  KEY `idx_team_member_user` (`user_id`),
  CONSTRAINT `uk_team_member_active` UNIQUE (`active_key`),
  CONSTRAINT `fk_team_member_team` FOREIGN KEY (`team_id`) REFERENCES `team`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_team_member_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='战队成员与在队历史';

CREATE TABLE `registration_invite` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `code` VARCHAR(64) NOT NULL,
  `created_by` BIGINT NOT NULL,
  `used_by` BIGINT NULL,
  `expires_at` DATETIME(0) NOT NULL,
  `max_uses` INT NOT NULL DEFAULT 1,
  `used_count` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  CONSTRAINT `uk_registration_invite_code` UNIQUE (`code`),
  KEY `idx_registration_invite_created` (`created_by`),
  CONSTRAINT `chk_registration_invite_usage` CHECK (`max_uses` > 0 AND `used_count` >= 0 AND `used_count` <= `max_uses`),
  CONSTRAINT `fk_registration_invite_created` FOREIGN KEY (`created_by`) REFERENCES `user`(`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_registration_invite_used` FOREIGN KEY (`used_by`) REFERENCES `user`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='注册邀请码';

CREATE TABLE `password_reset_token` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `token_hash` VARCHAR(128) NOT NULL COMMENT '只存哈希，不存明文',
  `expires_at` DATETIME(0) NOT NULL,
  `used_at` DATETIME(0) NULL,
  CONSTRAINT `uk_password_reset_token` UNIQUE (`token_hash`),
  KEY `idx_password_reset_token_user` (`user_id`),
  CONSTRAINT `fk_password_reset_token_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='密码重置令牌';

INSERT INTO `role` (`code`,`name`) VALUES
  ('SUPER_ADMIN','超级管理员'),
  ('CONTENT_ADMIN','内容管理员'),
  ('POINTS_ADMIN','积分管理员'),
  ('PR_ADMIN','宣传管理员'),
  ('TEAM_LEADER','队长'),
  ('USER','普通用户'),
  ('TEACHER','教师');

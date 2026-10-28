-- V8：按问题清单补齐账号、审计、媒体关联、文章互动与数据完整性约束。
-- 本迁移只做增量与数据清理，不改任何历史迁移。

-- ---------------------------------------------------------------
-- 1. 账号：首次登录/重置后强制改密
-- ---------------------------------------------------------------
ALTER TABLE `user`
  ADD COLUMN `must_change_password` TINYINT(1) NOT NULL DEFAULT 0
    COMMENT '为 1 时登录后强制改密（新建、重置、默认口令）';

ALTER TABLE `registration_invite`
  ADD COLUMN `note` VARCHAR(200) NULL COMMENT '备注，仅后台可见';

-- ---------------------------------------------------------------
-- 2. 审计日志：只增不改，记录谁在什么时候对什么做了什么
-- ---------------------------------------------------------------
CREATE TABLE `audit_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `operator_id` BIGINT NULL,
  `operator_name` VARCHAR(64) NULL COMMENT '操作发生时快照，用户改名不影响历史',
  `action` VARCHAR(64) NOT NULL COMMENT '如 ROLE_CHANGE / USER_DISABLE',
  `target_type` VARCHAR(32) NULL COMMENT 'USER / ARTICLE / MEDIA / APPEAL ...',
  `target_id` VARCHAR(64) NULL,
  `detail_before` VARCHAR(1000) NULL,
  `detail_after` VARCHAR(1000) NULL,
  `ip` VARCHAR(64) NULL,
  `request_id` VARCHAR(64) NULL COMMENT '关联一次请求的排障编号',
  `created_at` DATETIME(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_audit_time` (`created_at`),
  KEY `idx_audit_operator` (`operator_id`,`created_at`),
  KEY `idx_audit_target` (`target_type`,`target_id`,`created_at`),
  CONSTRAINT `fk_audit_operator` FOREIGN KEY (`operator_id`) REFERENCES `user`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='后台操作审计日志（只增不改）';

-- ---------------------------------------------------------------
-- 3. 媒体与业务对象关联：文章封面 / 竞赛照片 / 公告附件
-- ---------------------------------------------------------------
CREATE TABLE `media_link` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `media_id` BIGINT NOT NULL,
  `owner_type` VARCHAR(32) NOT NULL COMMENT 'ARTICLE_COVER / COMPETITION_PHOTO / ANNOUNCEMENT_ATTACHMENT',
  `owner_id` BIGINT NOT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_media_link_owner` (`owner_type`,`owner_id`,`sort_order`),
  KEY `idx_media_link_media` (`media_id`),
  CONSTRAINT `chk_media_link_owner` CHECK (`owner_type` IN ('ARTICLE_COVER','COMPETITION_PHOTO','ANNOUNCEMENT_ATTACHMENT')),
  CONSTRAINT `fk_media_link_media` FOREIGN KEY (`media_id`) REFERENCES `media`(`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='媒体引用（删除/下架内容时引用检查）';

-- ---------------------------------------------------------------
-- 4. 文章互动：标签、评论（人工审核）、收藏
-- ---------------------------------------------------------------
CREATE TABLE `article_tag` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `article_id` BIGINT NOT NULL,
  `tag` VARCHAR(32) NOT NULL,
  KEY `idx_article_tag_tag` (`tag`),
  CONSTRAINT `uk_article_tag` UNIQUE (`article_id`,`tag`),
  CONSTRAINT `fk_article_tag_article` FOREIGN KEY (`article_id`) REFERENCES `article`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文章标签';

CREATE TABLE `comment` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `article_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `content` VARCHAR(1000) NOT NULL,
  `state` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / APPROVED / REJECTED',
  `reviewed_by` BIGINT NULL,
  `reviewed_at` DATETIME(0) NULL,
  `created_at` DATETIME(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_comment_article` (`article_id`,`state`,`created_at`),
  KEY `idx_comment_state` (`state`,`created_at`),
  KEY `idx_comment_user` (`user_id`,`created_at`),
  CONSTRAINT `chk_comment_state` CHECK (`state` IN ('PENDING','APPROVED','REJECTED')),
  CONSTRAINT `fk_comment_article` FOREIGN KEY (`article_id`) REFERENCES `article`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_comment_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `user`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文章评论（先审后发）';

CREATE TABLE `favorite` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `article_id` BIGINT NOT NULL,
  `created_at` DATETIME(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `uk_favorite` UNIQUE (`user_id`,`article_id`),
  KEY `idx_favorite_article` (`article_id`),
  CONSTRAINT `fk_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_favorite_article` FOREIGN KEY (`article_id`) REFERENCES `article`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文章收藏';

-- ---------------------------------------------------------------
-- 5. 内容下架：公告 / 荣誉软删除；文章状态增加 OFFLINE；媒体增加 REVOKED
-- ---------------------------------------------------------------
ALTER TABLE `announcement`
  ADD COLUMN `deleted_at` DATETIME(0) NULL COMMENT '非空表示已下架（软删除）';
ALTER TABLE `honor`
  ADD COLUMN `deleted_at` DATETIME(0) NULL COMMENT '非空表示已下架（软删除）';

ALTER TABLE `article` DROP CHECK `chk_article_state`;
ALTER TABLE `article`
  ADD CONSTRAINT `chk_article_state`
  CHECK (`state` IN ('DRAFT','PENDING','PUBLISHED','REJECTED','OFFLINE'));
ALTER TABLE `article` ADD COLUMN `offline_at` DATETIME(0) NULL COMMENT '下架时间';

ALTER TABLE `media` DROP CHECK `chk_media_state`;
ALTER TABLE `media`
  ADD CONSTRAINT `chk_media_state`
  CHECK (`review_state` IN ('PENDING','APPROVED','REJECTED','REVOKED'));

-- ---------------------------------------------------------------
-- 6. 招新报名状态处理：备注、处理人、处理时间
-- ---------------------------------------------------------------
ALTER TABLE `recruit_application`
  ADD COLUMN `note` VARCHAR(500) NULL,
  ADD COLUMN `handled_by` BIGINT NULL,
  ADD COLUMN `handled_at` DATETIME(0) NULL,
  ADD CONSTRAINT `fk_recruit_handler` FOREIGN KEY (`handled_by`) REFERENCES `user`(`id`) ON DELETE SET NULL;

-- ---------------------------------------------------------------
-- 7. 团队奖业务唯一：同一比赛、同一名次的团队奖只能有一条
--    先留去重清单，再删重复行（份额随团队奖级联删除），最后加唯一约束。
-- ---------------------------------------------------------------
CREATE TABLE `team_award_dedupe_log` (
  `id` BIGINT NOT NULL,
  `competition_id` BIGINT NOT NULL,
  `rank_level` VARCHAR(16) NOT NULL,
  `base_points` INT NOT NULL,
  `proof_url` VARCHAR(500) NULL,
  `created_by` BIGINT NOT NULL,
  `created_at` DATETIME(0) NULL,
  `removed_at` DATETIME(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='V8 团队奖重复清理清单';

INSERT INTO `team_award_dedupe_log`
  (`id`,`competition_id`,`rank_level`,`base_points`,`proof_url`,`created_by`,`created_at`)
SELECT t1.`id`, t1.`competition_id`, t1.`rank_level`, t1.`base_points`, t1.`proof_url`, t1.`created_by`, t1.`created_at`
FROM `team_award` t1
JOIN `team_award` t2
  ON t1.`competition_id` = t2.`competition_id`
 AND t1.`rank_level` = t2.`rank_level`
 AND t1.`id` > t2.`id`;

DELETE t1 FROM `team_award` t1
JOIN `team_award` t2
  ON t1.`competition_id` = t2.`competition_id`
 AND t1.`rank_level` = t2.`rank_level`
 AND t1.`id` > t2.`id`;

ALTER TABLE `team_award`
  ADD CONSTRAINT `uk_team_award_comp_rank` UNIQUE (`competition_id`,`rank_level`);

-- ---------------------------------------------------------------
-- 8. 积分请求 / 流水补外键：先清理孤儿数据，再建约束
-- ---------------------------------------------------------------
ALTER TABLE `points_request`
  ADD COLUMN `level` VARCHAR(16) NULL COMMENT '漏洞等级（仅挖洞）' AFTER `scene`;

UPDATE `points_request` pr
LEFT JOIN `competition` c ON pr.`competition_id` = c.`id`
SET pr.`competition_id` = NULL
WHERE pr.`competition_id` IS NOT NULL AND c.`id` IS NULL;

UPDATE `points_request` pr
LEFT JOIN `vulnerability` v ON pr.`vulnerability_id` = v.`id`
SET pr.`vulnerability_id` = NULL
WHERE pr.`vulnerability_id` IS NOT NULL AND v.`id` IS NULL;

UPDATE `points_request` pr
LEFT JOIN `user` u ON pr.`reviewed_by` = u.`id`
SET pr.`reviewed_by` = NULL
WHERE pr.`reviewed_by` IS NOT NULL AND u.`id` IS NULL;

-- submitted_by 是 NOT NULL：孤儿行回填为申请人本人；申请人也不存在的（理论上不可能）直接删除
UPDATE `points_request` pr
LEFT JOIN `user` u ON pr.`submitted_by` = u.`id`
SET pr.`submitted_by` = pr.`user_id`
WHERE u.`id` IS NULL;

DELETE FROM `points_request` WHERE `user_id` NOT IN (SELECT `id` FROM `user`);

ALTER TABLE `points_request`
  ADD CONSTRAINT `fk_request_comp` FOREIGN KEY (`competition_id`) REFERENCES `competition`(`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_request_vuln` FOREIGN KEY (`vulnerability_id`) REFERENCES `vulnerability`(`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_request_submitted` FOREIGN KEY (`submitted_by`) REFERENCES `user`(`id`) ON DELETE RESTRICT,
  ADD CONSTRAINT `fk_request_reviewed` FOREIGN KEY (`reviewed_by`) REFERENCES `user`(`id`) ON DELETE SET NULL,
  ADD KEY `idx_request_comp` (`competition_id`),
  ADD KEY `idx_request_vuln` (`vulnerability_id`);

UPDATE `points_ledger` l
LEFT JOIN `vulnerability` v ON l.`vulnerability_id` = v.`id`
SET l.`vulnerability_id` = NULL
WHERE l.`vulnerability_id` IS NOT NULL AND v.`id` IS NULL;

ALTER TABLE `points_ledger`
  ADD CONSTRAINT `fk_ledger_vuln` FOREIGN KEY (`vulnerability_id`) REFERENCES `vulnerability`(`id`) ON DELETE SET NULL;

ALTER TABLE `points_appeal`
  ADD CONSTRAINT `fk_appeal_handler` FOREIGN KEY (`handled_by`) REFERENCES `user`(`id`) ON DELETE SET NULL;

-- ---------------------------------------------------------------
-- 9. 积分规则启用与补种：规则表成为唯一计分口径
-- ---------------------------------------------------------------
UPDATE `points_rule` SET `published_at` = '2026-01-01 00:00:00' WHERE `published_at` IS NULL;

INSERT INTO `points_rule` (`version`,`direction`,`item_key`,`points`,`effective_from`,`published_at`) VALUES
  (1,'COMPETITION','PROVINCE_FIRST',10,'2026-01-01','2026-01-01 00:00:00'),
  (1,'COMPETITION','PROVINCE_SECOND',7,'2026-01-01','2026-01-01 00:00:00'),
  (1,'COMPETITION','PROVINCE_THIRD',5,'2026-01-01','2026-01-01 00:00:00'),
  (1,'COMPETITION','PROVINCE_HONOR',0,'2026-01-01','2026-01-01 00:00:00'),
  (1,'COMPETITION','NATIONAL_FIRST',15,'2026-01-01','2026-01-01 00:00:00'),
  (1,'COMPETITION','NATIONAL_SECOND',13,'2026-01-01','2026-01-01 00:00:00'),
  (1,'COMPETITION','NATIONAL_THIRD',11,'2026-01-01','2026-01-01 00:00:00'),
  (1,'COMPETITION','NATIONAL_HONOR',0,'2026-01-01','2026-01-01 00:00:00'),
  (1,'COMPETITION','INTERNATIONAL_FIRST',20,'2026-01-01','2026-01-01 00:00:00'),
  (1,'COMPETITION','INTERNATIONAL_SECOND',18,'2026-01-01','2026-01-01 00:00:00'),
  (1,'COMPETITION','INTERNATIONAL_THIRD',16,'2026-01-01','2026-01-01 00:00:00'),
  (1,'COMPETITION','INTERNATIONAL_HONOR',0,'2026-01-01','2026-01-01 00:00:00'),
  (1,'COMPETITION','CAMPUS_FIRST',0,'2026-01-01','2026-01-01 00:00:00'),
  (1,'COMPETITION','CAMPUS_SECOND',0,'2026-01-01','2026-01-01 00:00:00'),
  (1,'COMPETITION','CAMPUS_THIRD',0,'2026-01-01','2026-01-01 00:00:00'),
  (1,'COMPETITION','CAMPUS_HONOR',0,'2026-01-01','2026-01-01 00:00:00'),
  (1,'HW','HW_5000',5,'2026-01-01','2026-01-01 00:00:00'),
  (1,'HW','HW_10000',10,'2026-01-01','2026-01-01 00:00:00'),
  (1,'HW','HW_STEP_5000',2,'2026-01-01','2026-01-01 00:00:00');

-- ---------------------------------------------------------------
-- 10. 移除历史迁移写入的演示荣誉；开发环境演示数据改由 seed 流程加载
-- ---------------------------------------------------------------
DELETE FROM `honor`
WHERE `year` = 2025
  AND (`title`,`description`) IN (
    ('CISCN 全国大学生信息安全竞赛 华北赛区三等奖','首次参赛'),
    ('吉林省大学生网络安全竞赛 二等奖','两人进入前十')
  );

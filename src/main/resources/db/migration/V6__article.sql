-- V6：技术空间。文章先落草稿，提交后进待审；涉漏洞点的文章强制站内可见。
CREATE TABLE `article` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `author_id` BIGINT NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `summary` VARCHAR(500) NULL,
  `content_md` MEDIUMTEXT NOT NULL COMMENT 'Markdown 原文',
  `content_html` MEDIUMTEXT NULL COMMENT '渲染后的 HTML，入库前已转义',
  `visibility` VARCHAR(16) NOT NULL DEFAULT 'INTERNAL' COMMENT 'PUBLIC / INTERNAL',
  `involves_vuln` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '涉及漏洞点，为 1 时不允许公开',
  `state` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT / PENDING / PUBLISHED / REJECTED',
  `reject_reason` VARCHAR(500) NULL,
  `published_at` DATETIME(0) NULL,
  `created_at` DATETIME(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME(0) NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_article_state` (`state`,`created_at`),
  KEY `idx_article_author` (`author_id`),
  CONSTRAINT `chk_article_visibility` CHECK (`visibility` IN ('PUBLIC','INTERNAL')),
  CONSTRAINT `chk_article_state` CHECK (`state` IN ('DRAFT','PENDING','PUBLISHED','REJECTED')),
  CONSTRAINT `fk_article_author` FOREIGN KEY (`author_id`) REFERENCES `user`(`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='技术文章';

CREATE TABLE `sensitive_word` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `word` VARCHAR(64) NOT NULL,
  `level` VARCHAR(16) NOT NULL COMMENT 'BLOCK / REVIEW',
  `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  CONSTRAINT `uk_sensitive_word` UNIQUE (`word`,`level`),
  CONSTRAINT `chk_sensitive_level` CHECK (`level` IN ('BLOCK','REVIEW'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='敏感词（两级）';

INSERT INTO `sensitive_word` (`word`,`level`) VALUES
  ('测试违禁词','BLOCK'),
  ('争议话题','REVIEW');

-- V7：对外内容。公告、荣誉、招新报名。
CREATE TABLE `announcement` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `title` VARCHAR(200) NOT NULL,
  `content_md` MEDIUMTEXT NOT NULL,
  `content_html` MEDIUMTEXT NULL,
  `author_id` BIGINT NOT NULL,
  `published_at` DATETIME(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `pinned` TINYINT(1) NOT NULL DEFAULT 0,
  KEY `idx_announcement_time` (`published_at`),
  CONSTRAINT `fk_announcement_author` FOREIGN KEY (`author_id`) REFERENCES `user`(`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公告';

CREATE TABLE `honor` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `title` VARCHAR(200) NOT NULL,
  `year` SMALLINT NOT NULL,
  `description` VARCHAR(500) NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  KEY `idx_honor_year` (`year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='荣誉墙';

CREATE TABLE `recruit_application` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL,
  `student_no` VARCHAR(32) NOT NULL,
  `college` VARCHAR(128) NOT NULL,
  `major` VARCHAR(128) NOT NULL,
  `contact` VARCHAR(128) NOT NULL,
  `message` VARCHAR(1000) NULL,
  `season_year` SMALLINT NOT NULL,
  `state` VARCHAR(16) NOT NULL DEFAULT 'NEW' COMMENT 'NEW / CONTACTED / ACCEPTED / REJECTED',
  `created_at` DATETIME(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `uk_recruit_student_season` UNIQUE (`student_no`,`season_year`),
  KEY `idx_recruit_state` (`state`,`created_at`),
  CONSTRAINT `chk_recruit_state` CHECK (`state` IN ('NEW','CONTACTED','ACCEPTED','REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='招新报名（游客可填）';

INSERT INTO `honor` (`title`,`year`,`description`,`sort_order`) VALUES
  ('CISCN 全国大学生信息安全竞赛 华北赛区三等奖', 2025, '首次参赛', 10),
  ('吉林省大学生网络安全竞赛 二等奖', 2025, '两人进入前十', 20);

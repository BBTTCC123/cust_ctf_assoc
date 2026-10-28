-- V2：比赛档案。校赛不给分，但仍要留档（个人主页的参赛记录要用）。
CREATE TABLE `competition` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(200) NOT NULL COMMENT '比赛名称',
  `organizer` VARCHAR(200) NOT NULL COMMENT '主办方',
  `level` VARCHAR(32) NOT NULL COMMENT 'PROVINCE / NATIONAL / INTERNATIONAL / CAMPUS',
  `category` VARCHAR(32) NOT NULL COMMENT 'WEB / PWN / REVERSE / CRYPTO / MISC / INCIDENT',
  `start_date` DATE NOT NULL,
  `end_date` DATE NOT NULL,
  `season_year` SMALLINT NOT NULL COMMENT '赛季年份，用于年榜与筛选',
  `created_at` DATETIME(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `uk_competition_name_year` UNIQUE (`name`, `season_year`),
  KEY `idx_competition_year` (`season_year`),
  KEY `idx_competition_level` (`level`),
  CONSTRAINT `chk_competition_level` CHECK (`level` IN ('PROVINCE','NATIONAL','INTERNATIONAL','CAMPUS')),
  CONSTRAINT `chk_competition_category` CHECK (`category` IN ('WEB','PWN','REVERSE','CRYPTO','MISC','INCIDENT')),
  CONSTRAINT `chk_competition_dates` CHECK (`end_date` >= `start_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='比赛档案';

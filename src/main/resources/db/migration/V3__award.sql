-- V3：奖项域。个人奖直接挂人；团队奖挂在团队记录上，再按份额分给个人（合计必须等于该奖的分）。
CREATE TABLE `award` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `competition_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `rank_level` VARCHAR(16) NOT NULL COMMENT 'FIRST / SECOND / THIRD / HONOR',
  `base_points` INT NOT NULL COMMENT '该奖项应得分（由级别+名次算，入库即固定）',
  `proof_url` VARCHAR(500) NULL,
  `verified_by` BIGINT NULL,
  `verified_at` DATETIME(0) NULL,
  `created_at` DATETIME(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `uk_award_comp_user_rank` UNIQUE (`competition_id`,`user_id`,`rank_level`),
  KEY `idx_award_user` (`user_id`),
  CONSTRAINT `chk_award_rank` CHECK (`rank_level` IN ('FIRST','SECOND','THIRD','HONOR')),
  CONSTRAINT `fk_award_comp` FOREIGN KEY (`competition_id`) REFERENCES `competition`(`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_award_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='个人奖项';

CREATE TABLE `team_award` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `competition_id` BIGINT NOT NULL,
  `rank_level` VARCHAR(16) NOT NULL,
  `base_points` INT NOT NULL COMMENT '这个团队奖的总分，分完之后合计必须等于它',
  `proof_url` VARCHAR(500) NULL,
  `created_by` BIGINT NOT NULL,
  `created_at` DATETIME(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `chk_team_award_rank` CHECK (`rank_level` IN ('FIRST','SECOND','THIRD','HONOR')),
  CONSTRAINT `fk_team_award_comp` FOREIGN KEY (`competition_id`) REFERENCES `competition`(`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_team_award_creator` FOREIGN KEY (`created_by`) REFERENCES `user`(`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='团队奖项';

CREATE TABLE `award_share` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `team_award_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `points` INT NOT NULL COMMENT '该成员分到的分，合计必须等于 team_award.base_points',
  CONSTRAINT `uk_award_share_member` UNIQUE (`team_award_id`,`user_id`),
  KEY `idx_award_share_user` (`user_id`),
  CONSTRAINT `chk_award_share_points` CHECK (`points` >= 0),
  CONSTRAINT `fk_award_share_award` FOREIGN KEY (`team_award_id`) REFERENCES `team_award`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_award_share_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='团队奖的成员份额';

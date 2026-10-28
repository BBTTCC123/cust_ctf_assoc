-- V5：积分。规则、漏洞点、申请单、流水、申诉。
-- 三条约束落在库里：流水幂等键唯一、冲抵只能一次、流水表只进不改（权限层面由 README 的约定配合）。

CREATE TABLE `points_rule` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `version` INT NOT NULL,
  `direction` VARCHAR(16) NOT NULL COMMENT 'COMPETITION / BOUNTY / HW',
  `item_key` VARCHAR(64) NOT NULL,
  `points` INT NOT NULL COMMENT '定值，不再是区间',
  `effective_from` DATE NOT NULL,
  `published_at` DATETIME(0) NULL,
  CONSTRAINT `uk_points_rule` UNIQUE (`direction`,`item_key`,`version`),
  CONSTRAINT `chk_points_rule_direction` CHECK (`direction` IN ('COMPETITION','BOUNTY','HW'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='积分规则（带版本）';

CREATE TABLE `vulnerability` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `platform` VARCHAR(32) NOT NULL COMMENT 'EDUSRC / CNVD / 企业SRC 名称',
  `external_id` VARCHAR(64) NOT NULL COMMENT '平台上的编号',
  `level` VARCHAR(16) NOT NULL COMMENT 'CRITICAL / HIGH / MEDIUM / LOW',
  `reporter_user_id` BIGINT NOT NULL,
  `first_seen_on` DATE NOT NULL,
  `created_at` DATETIME(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `uk_vulnerability` UNIQUE (`platform`,`external_id`),
  CONSTRAINT `chk_vulnerability_level` CHECK (`level` IN ('CRITICAL','HIGH','MEDIUM','LOW')),
  CONSTRAINT `fk_vulnerability_user` FOREIGN KEY (`reporter_user_id`) REFERENCES `user`(`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='漏洞点：封顶按它算';

CREATE TABLE `points_request` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `source_type` VARCHAR(16) NOT NULL COMMENT 'COMPETITION / BOUNTY / HW',
  `competition_id` BIGINT NULL,
  `vulnerability_id` BIGINT NULL,
  `scene` VARCHAR(16) NULL COMMENT 'SINGLE / SWEEP / CHARITY（仅挖洞用）',
  `target` VARCHAR(200) NULL COMMENT '目标标识（域名或系统名），按目标计分',
  `occurred_on` DATE NOT NULL COMMENT '比赛结束日 / 提交日',
  `points` INT NOT NULL COMMENT '申请的分（审核时可被截断）',
  `state` VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  `reason` VARCHAR(500) NULL,
  `proof_url` VARCHAR(500) NULL,
  `submitted_by` BIGINT NOT NULL,
  `reviewed_by` BIGINT NULL,
  `reviewed_at` DATETIME(0) NULL,
  `created_at` DATETIME(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_request_state` (`state`,`created_at`),
  KEY `idx_request_user` (`user_id`),
  CONSTRAINT `chk_request_state` CHECK (`state` IN ('PENDING','APPROVED','REJECTED','WITHDRAWN')),
  CONSTRAINT `fk_request_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='积分申请单（管流程，不产生分）';

CREATE TABLE `points_ledger` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `request_id` BIGINT NOT NULL,
  `entry_type` VARCHAR(16) NOT NULL COMMENT 'EARN / REVERSAL',
  `points` INT NOT NULL COMMENT 'REVERSAL 为负',
  `vulnerability_id` BIGINT NULL,
  `period_month` CHAR(7) NOT NULL COMMENT '归属月，入账时锁定',
  `occurred_on` DATE NOT NULL,
  `idempotency_key` VARCHAR(80) NOT NULL,
  `reverses_id` BIGINT NULL,
  `note` VARCHAR(500) NULL,
  `created_at` DATETIME(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `uk_ledger_idem` UNIQUE (`idempotency_key`),
  CONSTRAINT `uk_ledger_reversal` UNIQUE (`reverses_id`),
  KEY `idx_ledger_rank` (`period_month`,`user_id`),
  KEY `idx_ledger_user` (`user_id`,`occurred_on`),
  KEY `idx_ledger_vuln` (`vulnerability_id`),
  CONSTRAINT `chk_ledger_type` CHECK (`entry_type` IN ('EARN','REVERSAL')),
  CONSTRAINT `fk_ledger_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_ledger_request` FOREIGN KEY (`request_id`) REFERENCES `points_request`(`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_ledger_reverses` FOREIGN KEY (`reverses_id`) REFERENCES `points_ledger`(`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='积分流水：只增不改';

CREATE TABLE `points_appeal` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `ledger_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `reason` VARCHAR(500) NOT NULL,
  `state` VARCHAR(16) NOT NULL DEFAULT 'OPEN',
  `handled_by` BIGINT NULL,
  `handled_at` DATETIME(0) NULL,
  `created_at` DATETIME(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_appeal_state` (`state`,`created_at`),
  CONSTRAINT `chk_appeal_state` CHECK (`state` IN ('OPEN','ACCEPTED','REJECTED')),
  CONSTRAINT `fk_appeal_ledger` FOREIGN KEY (`ledger_id`) REFERENCES `points_ledger`(`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_appeal_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='积分申诉';

INSERT INTO `points_rule` (`version`,`direction`,`item_key`,`points`,`effective_from`) VALUES
  (1,'BOUNTY','CRITICAL',8,'2026-01-01'),
  (1,'BOUNTY','HIGH',6,'2026-01-01'),
  (1,'BOUNTY','MEDIUM',4,'2026-01-01'),
  (1,'BOUNTY','LOW',2,'2026-01-01'),
  (1,'HW','BASE_1000',3,'2026-01-01');

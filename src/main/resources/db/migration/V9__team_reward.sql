-- V9：ISS-030 收尾。战队空间、积分兑换。
-- 只做增量：team / team_member 复用 V1，不改历史迁移。
-- 能力雷达是纯查询能力，不需要新表。

-- ---------------------------------------------------------------
-- 1. 战队空间：补创建时间、入队时间与状态约束
-- ---------------------------------------------------------------
ALTER TABLE `team`
  ADD COLUMN `created_at` DATETIME(0) NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER `status`;

-- 加约束前先把历史脏数据归位，否则迁移会在约束处失败
UPDATE `team` SET `status` = 'ACTIVE' WHERE `status` NOT IN ('ACTIVE', 'DISBANDED');
ALTER TABLE `team`
  ADD CONSTRAINT `chk_team_status` CHECK (`status` IN ('ACTIVE', 'DISBANDED')),
  ADD KEY `idx_team_status` (`status`);

ALTER TABLE `team_member`
  ADD COLUMN `joined_at` DATETIME(0) NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入队时间' AFTER `user_id`;

-- ---------------------------------------------------------------
-- 2. 积分兑换：可配置资源表 + 申请/审核流
--    兑换不写 points_ledger（流水只增不改）；可用余额 = 流水合计 - 已通过兑换合计。
-- ---------------------------------------------------------------
CREATE TABLE `reward_item` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `description` VARCHAR(500) NULL,
  `cost_points` INT NOT NULL COMMENT '兑换所需积分，正整数',
  `stock` INT NOT NULL DEFAULT 0 COMMENT '剩余库存',
  `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '0 时前台不可见、不可申请',
  `sort_order` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME(0) NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `uk_reward_item_name` UNIQUE (`name`),
  CONSTRAINT `chk_reward_item_cost` CHECK (`cost_points` > 0),
  CONSTRAINT `chk_reward_item_stock` CHECK (`stock` >= 0),
  KEY `idx_reward_item_enabled` (`enabled`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='兑换资源（后台可配置，不硬编码）';

CREATE TABLE `redeem_request` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `item_id` BIGINT NOT NULL,
  `item_name` VARCHAR(100) NOT NULL COMMENT '申请时快照，资源改名不影响历史',
  `cost_points` INT NOT NULL COMMENT '申请时快照，审核按它扣',
  `state` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / APPROVED / REJECTED / CANCELLED',
  `note` VARCHAR(500) NULL COMMENT '申请人备注',
  `review_note` VARCHAR(500) NULL COMMENT '审核备注',
  `reviewed_by` BIGINT NULL,
  `reviewed_at` DATETIME(0) NULL,
  `created_at` DATETIME(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_redeem_state` (`state`, `created_at`),
  KEY `idx_redeem_user` (`user_id`, `created_at`),
  KEY `idx_redeem_item` (`item_id`, `state`),
  CONSTRAINT `chk_redeem_state` CHECK (`state` IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
  CONSTRAINT `fk_redeem_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_redeem_item` FOREIGN KEY (`item_id`) REFERENCES `reward_item`(`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_redeem_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `user`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='积分兑换申请（审核通过才扣余额与库存）';

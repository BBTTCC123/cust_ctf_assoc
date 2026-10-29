-- V10：第二轮审查修复。
-- 1) 修正战队成员“一人同时一队”的数据库约束；
-- 2) 为待处理申诉、待审核兑换补并发唯一约束；
-- 3) 允许已通过评论下架；
-- 4) 升级已有账号：下次登录必须重新设置密码，覆盖旧版本可能使用默认口令的账号。

-- ---------------------------------------------------------------
-- 1. 战队成员：active_key 之前错误地包含 team_id，无法阻止同一用户同时加入多队。
--    先保留最早的活跃加入记录，其余软退队并写入清理清单，再重建唯一键。
-- ---------------------------------------------------------------
CREATE TABLE `team_member_dedupe_log` (
  `id` BIGINT NOT NULL,
  `team_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `joined_at` DATETIME(0) NULL,
  `left_at_before` DATETIME(0) NULL,
  `removed_at` DATETIME(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='V10 清理重复在队成员时保留的原记录';

INSERT INTO `team_member_dedupe_log`
  (`id`,`team_id`,`user_id`,`joined_at`,`left_at_before`)
SELECT m.`id`, m.`team_id`, m.`user_id`, m.`joined_at`, m.`left_at`
FROM `team_member` m
WHERE m.`left_at` IS NULL
  AND m.`id` NOT IN (
    SELECT keep_id FROM (
      SELECT MIN(`id`) AS keep_id
      FROM `team_member`
      WHERE `left_at` IS NULL
      GROUP BY `user_id`
    ) AS keepers
  );

UPDATE `team_member` m
JOIN `team_member_dedupe_log` d ON d.`id` = m.`id`
SET m.`left_at` = CURRENT_TIMESTAMP
WHERE m.`left_at` IS NULL;

ALTER TABLE `team_member`
  DROP INDEX `uk_team_member_active`,
  DROP COLUMN `active_key`,
  ADD COLUMN `active_user_id` BIGINT GENERATED ALWAYS AS
    (IF(`left_at` IS NULL, `user_id`, NULL)) STORED
    COMMENT '仅活跃成员生成，用于保证同一用户同时只能在一支战队',
  ADD CONSTRAINT `uk_team_member_active_user` UNIQUE (`active_user_id`);

-- ---------------------------------------------------------------
-- 2. 每条流水最多一条待处理申诉；同一用户同一资源最多一条待审核兑换。
--    清理历史重复数据并写入审计，随后用生成列 + 唯一索引兜底并发。
-- ---------------------------------------------------------------
INSERT INTO `audit_log`
  (`operator_id`,`operator_name`,`action`,`target_type`,`target_id`,
   `detail_before`,`detail_after`,`created_at`)
SELECT NULL, 'migration:v10', 'APPEAL_DUPLICATE_CLOSE', 'APPEAL', CAST(a.`id` AS CHAR),
       'OPEN', CONCAT('自动驳回；流水 ', a.`ledger_id`, ' 已有更早的待处理申诉'), CURRENT_TIMESTAMP
FROM `points_appeal` a
WHERE a.`state` = 'OPEN'
  AND a.`id` NOT IN (
    SELECT keep_id FROM (
      SELECT MIN(`id`) AS keep_id
      FROM `points_appeal`
      WHERE `state` = 'OPEN'
      GROUP BY `ledger_id`
    ) AS keepers
  );

UPDATE `points_appeal` a
JOIN `points_appeal` earlier
  ON earlier.`ledger_id` = a.`ledger_id`
 AND earlier.`state` = 'OPEN'
 AND earlier.`id` < a.`id`
SET a.`state` = 'REJECTED',
    a.`handled_at` = CURRENT_TIMESTAMP
WHERE a.`state` = 'OPEN';

ALTER TABLE `points_appeal`
  ADD COLUMN `open_ledger_id` BIGINT GENERATED ALWAYS AS
    (IF(`state` = 'OPEN', `ledger_id`, NULL)) STORED
    COMMENT '仅待处理申诉生成，用于保证每条流水最多一条 OPEN 申诉',
  ADD CONSTRAINT `uk_points_appeal_open_ledger` UNIQUE (`open_ledger_id`);

INSERT INTO `audit_log`
  (`operator_id`,`operator_name`,`action`,`target_type`,`target_id`,
   `detail_before`,`detail_after`,`created_at`)
SELECT NULL, 'migration:v10', 'REDEEM_DUPLICATE_REJECT', 'REDEEM_REQUEST', CAST(r.`id` AS CHAR),
       'PENDING', CONCAT('自动驳回；用户 ', r.`user_id`, ' 与资源 ', r.`item_id`, ' 已有更早的待审核申请'),
       CURRENT_TIMESTAMP
FROM `redeem_request` r
WHERE r.`state` = 'PENDING'
  AND r.`id` NOT IN (
    SELECT keep_id FROM (
      SELECT MIN(`id`) AS keep_id
      FROM `redeem_request`
      WHERE `state` = 'PENDING'
      GROUP BY `user_id`, `item_id`
    ) AS keepers
  );

UPDATE `redeem_request` r
JOIN `redeem_request` earlier
  ON earlier.`user_id` = r.`user_id`
 AND earlier.`item_id` = r.`item_id`
 AND earlier.`state` = 'PENDING'
 AND earlier.`id` < r.`id`
SET r.`state` = 'REJECTED',
    r.`review_note` = CONCAT('迁移清理：已有更早的待审核申请 #', earlier.`id`),
    r.`reviewed_at` = CURRENT_TIMESTAMP
WHERE r.`state` = 'PENDING';

ALTER TABLE `redeem_request`
  ADD COLUMN `pending_user_item` VARCHAR(80) GENERATED ALWAYS AS
    (IF(`state` = 'PENDING', CONCAT(`user_id`, ':', `item_id`), NULL)) STORED
    COMMENT '仅待审核兑换生成，用于并发唯一约束',
  ADD CONSTRAINT `uk_redeem_pending_user_item` UNIQUE (`pending_user_item`);

-- ---------------------------------------------------------------
-- 3. 已通过评论允许下架。
-- ---------------------------------------------------------------
ALTER TABLE `comment` DROP CHECK `chk_comment_state`;
ALTER TABLE `comment`
  ADD CONSTRAINT `chk_comment_state`
  CHECK (`state` IN ('PENDING','APPROVED','REJECTED','REVOKED'));

-- ---------------------------------------------------------------
-- 4. 旧版本升级：已有账号下次登录必须改密。
--    新库此时没有业务用户，因此不会影响首次初始化流程。
-- ---------------------------------------------------------------
UPDATE `user` SET `must_change_password` = 1;

-- V4：媒体。所有上传文件先落 private 区，审核通过且确认公开后才进 public 区。
CREATE TABLE `media` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `owner_user_id` BIGINT NOT NULL COMMENT '上传人',
  `original_name` VARCHAR(255) NOT NULL COMMENT '原始文件名，只用于展示',
  `stored_key` VARCHAR(300) NOT NULL COMMENT '存储键（不含存储根路径）',
  `thumb_key` VARCHAR(300) NULL,
  `mime` VARCHAR(64) NOT NULL,
  `bytes` BIGINT NOT NULL,
  `sha256` CHAR(64) NOT NULL,
  `zone` VARCHAR(16) NOT NULL DEFAULT 'PRIVATE' COMMENT 'PRIVATE / PUBLIC',
  `visibility` VARCHAR(16) NOT NULL DEFAULT 'MEMBER' COMMENT 'PUBLIC / MEMBER / OWNER',
  `review_state` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / APPROVED / REJECTED',
  `created_at` DATETIME(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_media_owner` (`owner_user_id`),
  KEY `idx_media_state` (`review_state`,`created_at`),
  CONSTRAINT `chk_media_zone` CHECK (`zone` IN ('PRIVATE','PUBLIC')),
  CONSTRAINT `chk_media_visibility` CHECK (`visibility` IN ('PUBLIC','MEMBER','OWNER')),
  CONSTRAINT `chk_media_state` CHECK (`review_state` IN ('PENDING','APPROVED','REJECTED')),
  CONSTRAINT `fk_media_owner` FOREIGN KEY (`owner_user_id`) REFERENCES `user`(`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='上传文件';

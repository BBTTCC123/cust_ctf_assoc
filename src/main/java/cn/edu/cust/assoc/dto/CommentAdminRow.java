package cn.edu.cust.assoc.dto;

import java.time.LocalDateTime;

/** 后台评论审核/下架列表行。 */
public record CommentAdminRow(Long id, Long articleId, String articleTitle, String nickname,
                              String content, String state, LocalDateTime createdAt) {
}

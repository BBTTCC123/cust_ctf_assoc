package cn.edu.cust.assoc.dto;

import java.time.LocalDateTime;

/** 待审评论行。 */
public record PendingCommentRow(Long id, Long articleId, String articleTitle, String nickname,
                                String content, LocalDateTime createdAt) {
}

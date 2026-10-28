package cn.edu.cust.assoc.dto;

import java.time.LocalDateTime;

/** 已审核通过的评论。 */
public record CommentView(Long id, String nickname, String content, LocalDateTime createdAt) {
}

package cn.edu.cust.assoc.dto;

import java.time.LocalDateTime;

/** 我的文章行。 */
public record MyArticleRow(Long id, String title, String state, String visibility,
                           boolean involvesVuln, String rejectReason, LocalDateTime updatedAt) {
}

package cn.edu.cust.assoc.dto;

import java.time.LocalDateTime;

/** 后台文章行。 */
public record ArticleAdminRow(Long id, String title, String authorName, String visibility,
                              boolean involvesVuln, String state, String rejectReason,
                              LocalDateTime createdAt) {
}

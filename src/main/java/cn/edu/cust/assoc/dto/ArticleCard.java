package cn.edu.cust.assoc.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 文章列表卡片。 */
public record ArticleCard(Long id, String title, String summary, String visibility,
                          boolean involvesVuln, String authorName, LocalDateTime publishedAt,
                          List<String> tags, Long coverMediaId) {
}

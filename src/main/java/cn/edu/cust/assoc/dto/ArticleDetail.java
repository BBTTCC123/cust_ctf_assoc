package cn.edu.cust.assoc.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 文章详情页视图。 */
public record ArticleDetail(Long id, String title, String summary, String contentHtml,
                            String visibility, boolean involvesVuln, String authorName, Long authorId,
                            LocalDateTime publishedAt, String state, String rejectReason,
                            List<String> tags, Long coverMediaId, List<CommentView> comments,
                            boolean favorited, boolean canEdit) {
}

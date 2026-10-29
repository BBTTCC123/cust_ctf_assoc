package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.common.BizException;
import cn.edu.cust.assoc.domain.ArticleEntity;
import cn.edu.cust.assoc.domain.CommentEntity;
import cn.edu.cust.assoc.domain.MediaEntity;
import cn.edu.cust.assoc.dto.ArticleCard;
import cn.edu.cust.assoc.dto.ArticleDetail;
import cn.edu.cust.assoc.dto.CommentView;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/** 文章页面数据组装：实体转 DTO，模板不直接接触 Entity。 */
@Service
public class ArticleViewService {

    private final ArticleService articleService;
    private final UserDirectoryService userDirectory;
    private final MediaLinkService mediaLinkService;
    private final CommentService commentService;
    private final FavoriteService favoriteService;

    public ArticleViewService(ArticleService articleService, UserDirectoryService userDirectory,
                              MediaLinkService mediaLinkService, CommentService commentService,
                              FavoriteService favoriteService) {
        this.articleService = articleService;
        this.userDirectory = userDirectory;
        this.mediaLinkService = mediaLinkService;
        this.commentService = commentService;
        this.favoriteService = favoriteService;
    }

    @Transactional(readOnly = true)
    public Page<ArticleCard> cards(Page<ArticleEntity> page) {
        List<Long> ids = page.getContent().stream().map(ArticleEntity::getId).toList();
        Map<Long, List<String>> tags = articleService.tagsOf(ids);
        Map<Long, MediaEntity> covers = mediaLinkService.coversOf(ids);
        Map<Long, String> authors = userDirectory.nicknames(
                page.getContent().stream().map(ArticleEntity::getAuthorId).toList());
        return page.map(article -> new ArticleCard(
                article.getId(), article.getTitle(), article.getSummary(), article.getVisibility(),
                Boolean.TRUE.equals(article.getInvolvesVuln()),
                authors.getOrDefault(article.getAuthorId(), "成员"),
                article.getPublishedAt(),
                tags.getOrDefault(article.getId(), List.of()),
                coverId(covers.get(article.getId()))));
    }

    @Transactional(readOnly = true)
    public ArticleDetail detail(Long articleId, Long viewerId) {
        ArticleEntity article = articleService.get(articleId);
        List<CommentView> comments = commentViews(commentService.approvedOf(articleId));
        MediaEntity cover = mediaLinkService.singleOf(MediaLinkService.ARTICLE_COVER, articleId);
        boolean favorited = viewerId != null && favoriteService.isFavorited(viewerId, articleId);
        boolean canEdit = viewerId != null && article.getAuthorId().equals(viewerId)
                && ("DRAFT".equals(article.getState()) || "REJECTED".equals(article.getState()));
        return new ArticleDetail(
                article.getId(), article.getTitle(), article.getSummary(), article.getContentHtml(),
                article.getVisibility(), Boolean.TRUE.equals(article.getInvolvesVuln()),
                userDirectory.nickname(article.getAuthorId()), article.getAuthorId(),
                article.getPublishedAt(), article.getState(), article.getRejectReason(),
                articleService.tagsOf(articleId).stream().map(t -> t.getTag()).toList(),
                coverId(cover), comments, favorited, canEdit);
    }

    private List<CommentView> commentViews(List<CommentEntity> comments) {
        Map<Long, String> names = userDirectory.nicknames(
                comments.stream().map(CommentEntity::getUserId).toList());
        return comments.stream()
                .map(c -> new CommentView(c.getId(), names.getOrDefault(c.getUserId(), "成员"),
                        c.getContent(), c.getCreatedAt()))
                .toList();
    }

    private Long coverId(MediaEntity cover) {
        return cover == null ? null : cover.getId();
    }
}

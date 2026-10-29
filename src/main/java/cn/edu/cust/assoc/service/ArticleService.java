package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.common.BizException;
import cn.edu.cust.assoc.domain.ArticleEntity;
import cn.edu.cust.assoc.domain.ArticleTagEntity;
import cn.edu.cust.assoc.repository.ArticleRepository;
import cn.edu.cust.assoc.repository.ArticleTagRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ArticleService {

    private static final int MAX_TAGS = 8;

    private final ArticleRepository repository;
    private final ArticleTagRepository tagRepository;
    private final MarkdownRenderer renderer;
    private final SensitiveWordService sensitiveWords;
    private final MediaLinkService mediaLinks;
    private final AuditService audit;

    public ArticleService(ArticleRepository repository, ArticleTagRepository tagRepository,
                          MarkdownRenderer renderer, SensitiveWordService sensitiveWords,
                          MediaLinkService mediaLinks, AuditService audit) {
        this.repository = repository;
        this.tagRepository = tagRepository;
        this.renderer = renderer;
        this.sensitiveWords = sensitiveWords;
        this.mediaLinks = mediaLinks;
        this.audit = audit;
    }

    /** 后台直接发布流程用：保存即进入待审。 */
    @Transactional
    public ArticleEntity save(Long authorId, String title, String summary, String contentMd,
                              boolean involvesVuln, String requestedVisibility) {
        return create(authorId, title, summary, contentMd, involvesVuln,
                requestedVisibility, null, null, true);
    }

    /**
     * 成员投稿：save 后是草稿，submit 后进入待审。
     * 两条硬规则：涉漏洞点的文章一律站内可见；一级敏感词直接拒绝。
     */
    @Transactional
    public ArticleEntity create(Long authorId, String title, String summary, String contentMd,
                                boolean involvesVuln, String requestedVisibility,
                                List<String> tags, Long coverMediaId, boolean submit) {
        String cleanTitle = requireText(title, "标题不能为空", 200);
        String cleanContent = requireText(contentMd, "正文不能为空", Integer.MAX_VALUE);
        String visibility = involvesVuln ? "INTERNAL"
                : ("PUBLIC".equals(requestedVisibility) ? "PUBLIC" : "INTERNAL");

        SensitiveWordService.Result result = sensitiveWords.check(cleanTitle + "\n" + summary + "\n" + cleanContent);
        if (result == SensitiveWordService.Result.BLOCK) {
            throw new BizException("包含不允许发布的内容");
        }

        ArticleEntity article = new ArticleEntity();
        article.setAuthorId(authorId);
        article.setTitle(cleanTitle);
        article.setSummary(trimToNull(summary, 500));
        article.setContentMd(cleanContent);
        article.setContentHtml(renderer.render(cleanContent));
        article.setInvolvesVuln(involvesVuln);
        article.setVisibility(visibility);
        article.setState(submit ? "PENDING" : "DRAFT");
        ArticleEntity saved = repository.save(article);
        replaceTags(saved.getId(), tags);
        if (coverMediaId != null) {
            mediaLinks.setSingle(MediaLinkService.ARTICLE_COVER, saved.getId(), coverMediaId);
        }
        if (submit) {
            audit.record("ARTICLE_SUBMIT", "ARTICLE", saved.getId(), "DRAFT", "PENDING");
        }
        return saved;
    }

    /** 作者编辑自己的草稿/被驳回文章；已提交或已发布的不能直接改。 */
    @Transactional
    public ArticleEntity updateOwn(Long authorId, Long articleId, String title, String summary,
                                   String contentMd, boolean involvesVuln, String requestedVisibility,
                                   List<String> tags, Long coverMediaId) {
        ArticleEntity article = get(articleId);
        if (!article.getAuthorId().equals(authorId)) {
            throw new BizException("不能编辑别人的文章");
        }
        if (!"DRAFT".equals(article.getState()) && !"REJECTED".equals(article.getState())) {
            throw new BizException("已提交或已发布的文章不能直接编辑");
        }
        String cleanTitle = requireText(title, "标题不能为空", 200);
        String cleanContent = requireText(contentMd, "正文不能为空", Integer.MAX_VALUE);
        SensitiveWordService.Result result = sensitiveWords.check(cleanTitle + "\n" + summary + "\n" + cleanContent);
        if (result == SensitiveWordService.Result.BLOCK) {
            throw new BizException("包含不允许发布的内容");
        }
        article.setTitle(cleanTitle);
        article.setSummary(trimToNull(summary, 500));
        article.setContentMd(cleanContent);
        article.setContentHtml(renderer.render(cleanContent));
        article.setInvolvesVuln(involvesVuln);
        article.setVisibility(involvesVuln ? "INTERNAL"
                : ("PUBLIC".equals(requestedVisibility) ? "PUBLIC" : "INTERNAL"));
        article.setState("DRAFT");
        article.setRejectReason(null);
        ArticleEntity saved = repository.save(article);
        replaceTags(articleId, tags);
        // null 表示移除封面；媒体存在性、审核态和内容更新在同一事务中完成。
        mediaLinks.setSingle(MediaLinkService.ARTICLE_COVER, articleId, coverMediaId);
        return saved;
    }

    @Transactional
    public void submitOwn(Long authorId, Long articleId) {
        ArticleEntity article = get(articleId);
        if (!article.getAuthorId().equals(authorId)) {
            throw new BizException("不能提交别人的文章");
        }
        if (!"DRAFT".equals(article.getState()) && !"REJECTED".equals(article.getState())) {
            throw new BizException("只有草稿或被驳回的文章才能提交审核");
        }
        assertNoBlockedWords(article);
        article.setState("PENDING");
        article.setRejectReason(null);
        repository.save(article);
        audit.record("ARTICLE_SUBMIT", "ARTICLE", articleId, "DRAFT/REJECTED", "PENDING");
    }

    @Transactional
    public void publish(Long id, String note) {
        ArticleEntity article = get(id);
        if ("PUBLISHED".equals(article.getState())) return;
        if (!"PENDING".equals(article.getState()) && !"OFFLINE".equals(article.getState())) {
            throw new BizException("只有待审或已下架的文章才能发布");
        }
        assertNoBlockedWords(article);
        article.setState("PUBLISHED");
        article.setPublishedAt(LocalDateTime.now());
        article.setRejectReason(null);
        article.setOfflineAt(null);
        repository.save(article);
        audit.record("ARTICLE_PUBLISH", "ARTICLE", id,
                "PENDING/OFFLINE" + (note == null ? "" : " " + note), "PUBLISHED");
    }

    @Transactional
    public void reject(Long id, String reason) {
        ArticleEntity article = get(id);
        if (!"PENDING".equals(article.getState())) {
            throw new BizException("只有待审文章才能驳回");
        }
        article.setState("REJECTED");
        article.setRejectReason(reason);
        repository.save(article);
        audit.record("ARTICLE_REJECT", "ARTICLE", id, "PENDING", "REJECTED " + reason);
    }

    /** 已发布文章下架：保留内容，前台立即不可见。 */
    @Transactional
    public void offline(Long id, String reason) {
        ArticleEntity article = get(id);
        if (!"PUBLISHED".equals(article.getState())) {
            throw new BizException("只有已发布文章才能下架");
        }
        article.setState("OFFLINE");
        article.setOfflineAt(LocalDateTime.now());
        article.setRejectReason(reason);
        repository.save(article);
        audit.record("ARTICLE_OFFLINE", "ARTICLE", id, "PUBLISHED",
                "OFFLINE" + (reason == null ? "" : " " + reason));
    }

    @Transactional(readOnly = true)
    public ArticleEntity get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BizException("文章不存在：" + id));
    }

    @Transactional(readOnly = true)
    public ArticleEntity getOwn(Long authorId, Long articleId) {
        ArticleEntity article = get(articleId);
        if (!article.getAuthorId().equals(authorId)) {
            throw new BizException("不能编辑别人的文章");
        }
        if (!"DRAFT".equals(article.getState()) && !"REJECTED".equals(article.getState())) {
            throw new BizException("只有草稿或被驳回的文章才能编辑");
        }
        return article;
    }

    @Transactional(readOnly = true)
    public Page<ArticleEntity> search(boolean member, String q, String tag, Pageable pageable) {
        return repository.searchPublished(member, blankToNull(q), blankToNull(tag), pageable);
    }

    @Transactional(readOnly = true)
    public List<ArticleEntity> mine(Long authorId) {
        return repository.findByAuthorIdOrderByCreatedAtDesc(authorId);
    }

    @Transactional(readOnly = true)
    public List<ArticleEntity> pending() {
        return repository.findByStateOrderByCreatedAtAsc("PENDING");
    }

    @Transactional(readOnly = true)
    public List<ArticleEntity> published() {
        return repository.findByStateOrderByPublishedAtDesc("PUBLISHED");
    }

    @Transactional(readOnly = true)
    public List<ArticleTagEntity> tagsOf(Long articleId) {
        return tagRepository.findByArticleId(articleId);
    }

    @Transactional(readOnly = true)
    public Map<Long, String> titlesOf(List<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> result = new java.util.HashMap<>();
        repository.findAllById(articleIds).forEach(a -> result.put(a.getId(), a.getTitle()));
        return result;
    }

    @Transactional(readOnly = true)
    public Map<Long, List<String>> tagsOf(List<Long> articleIds) {
        if (articleIds.isEmpty()) {
            return Map.of();
        }
        return tagRepository.findByArticleIdIn(articleIds).stream()
                .collect(Collectors.groupingBy(ArticleTagEntity::getArticleId,
                        Collectors.mapping(ArticleTagEntity::getTag, Collectors.toList())));
    }

    @Transactional(readOnly = true)
    public List<String> allTags() {
        return tagRepository.distinctTags();
    }

    private void replaceTags(Long articleId, List<String> tags) {
        tagRepository.deleteByArticleId(articleId);
        if (tags == null || tags.isEmpty()) {
            return;
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String raw : tags) {
            if (raw == null) continue;
            String tag = raw.trim();
            if (tag.isEmpty()) continue;
            if (tag.length() > 32) {
                throw new BizException("标签不能超过 32 个字符：" + tag);
            }
            unique.add(tag);
        }
        if (unique.size() > MAX_TAGS) {
            throw new BizException("一篇文章最多 " + MAX_TAGS + " 个标签");
        }
        for (String tag : unique) {
            ArticleTagEntity entity = new ArticleTagEntity();
            entity.setArticleId(articleId);
            entity.setTag(tag);
            tagRepository.save(entity);
        }
    }

    private String requireText(String value, String message, int max) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            throw new BizException(message);
        }
        if (text.length() > max) {
            throw new BizException("内容超过长度上限");
        }
        return text;
    }

    private void assertNoBlockedWords(ArticleEntity article) {
        if (sensitiveWords.check(article.getTitle() + "\n" + article.getSummary() + "\n"
                + article.getContentMd()) == SensitiveWordService.Result.BLOCK) {
            throw new BizException("文章包含不允许发布的内容，请修改后再提交或发布");
        }
    }

    private String trimToNull(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String text = value.trim();
        return text.length() > max ? text.substring(0, max) : text;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

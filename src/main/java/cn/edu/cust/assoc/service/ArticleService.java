package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.domain.ArticleEntity;
import cn.edu.cust.assoc.repository.ArticleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ArticleService {

    private final ArticleRepository repository;
    private final MarkdownRenderer renderer;
    private final SensitiveWordService sensitiveWords;

    public ArticleService(ArticleRepository repository, MarkdownRenderer renderer,
                          SensitiveWordService sensitiveWords) {
        this.repository = repository;
        this.renderer = renderer;
        this.sensitiveWords = sensitiveWords;
    }

    /**
     * 保存文章。两条硬规则：
     * 1) 涉漏洞点的文章一律站内可见，作者选的「公开」会被覆盖；
     * 2) 命中一级敏感词直接拒绝，命中二级词的仍然待审（不自动发布）。
     */
    @Transactional
    public ArticleEntity save(Long authorId, String title, String summary, String contentMd,
                              boolean involvesVuln, String requestedVisibility) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("标题不能为空");
        if (contentMd == null || contentMd.isBlank()) throw new IllegalArgumentException("正文不能为空");

        String visibility = involvesVuln ? "INTERNAL" : ("PUBLIC".equals(requestedVisibility) ? "PUBLIC" : "INTERNAL");

        SensitiveWordService.Result result = sensitiveWords.check(title + "\n" + summary + "\n" + contentMd);
        if (result == SensitiveWordService.Result.BLOCK) {
            throw new IllegalArgumentException("包含不允许发布的内容");
        }

        ArticleEntity article = new ArticleEntity();
        article.setAuthorId(authorId);
        article.setTitle(title.trim());
        article.setSummary(summary);
        article.setContentMd(contentMd);
        article.setContentHtml(renderer.render(contentMd));
        article.setInvolvesVuln(involvesVuln);
        article.setVisibility(visibility);
        article.setState("PENDING");
        return repository.save(article);
    }

    @Transactional
    public void publish(Long id, String note) {
        ArticleEntity a = get(id);
        if ("PUBLISHED".equals(a.getState())) return;
        a.setState("PUBLISHED");
        a.setPublishedAt(LocalDateTime.now());
        a.setRejectReason(null);
        repository.save(a);
    }

    @Transactional
    public void reject(Long id, String reason) {
        ArticleEntity a = get(id);
        a.setState("REJECTED");
        a.setRejectReason(reason);
        repository.save(a);
    }

    @Transactional(readOnly = true)
    public ArticleEntity get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("文章不存在：" + id));
    }

    @Transactional(readOnly = true)
    public List<ArticleEntity> publicList() {
        return repository.findByStateAndVisibilityOrderByPublishedAtDesc("PUBLISHED", "PUBLIC");
    }

    @Transactional(readOnly = true)
    public List<ArticleEntity> memberList() {
        return repository.findAll().stream()
                .filter(a -> "PUBLISHED".equals(a.getState()))
                .sorted((x, y) -> {
                    if (x.getPublishedAt() == null) return 1;
                    if (y.getPublishedAt() == null) return -1;
                    return y.getPublishedAt().compareTo(x.getPublishedAt());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ArticleEntity> pending() {
        return repository.findByStateOrderByCreatedAtAsc("PENDING");
    }
}

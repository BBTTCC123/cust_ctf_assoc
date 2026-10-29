package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.common.BizException;
import cn.edu.cust.assoc.domain.ArticleEntity;
import cn.edu.cust.assoc.domain.CommentEntity;
import cn.edu.cust.assoc.repository.ArticleRepository;
import cn.edu.cust.assoc.repository.CommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 文章评论：所有评论先落 PENDING，敏感词 BLOCK 直接拒绝，其余等人工审核。 */
@Service
public class CommentService {

    private final CommentRepository repository;
    private final ArticleRepository articleRepository;
    private final SensitiveWordService sensitiveWords;
    private final AuditService audit;

    public CommentService(CommentRepository repository, ArticleRepository articleRepository,
                          SensitiveWordService sensitiveWords, AuditService audit) {
        this.repository = repository;
        this.articleRepository = articleRepository;
        this.sensitiveWords = sensitiveWords;
        this.audit = audit;
    }

    @Transactional
    public CommentEntity add(Long articleId, Long userId, String content) {
        ArticleEntity article = articleRepository.findById(articleId)
                .orElseThrow(() -> new BizException("文章不存在"));
        if (!"PUBLISHED".equals(article.getState())) {
            throw new BizException("文章还没发布，不能评论");
        }
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) {
            throw new BizException("评论不能为空");
        }
        if (text.length() > 1000) {
            throw new BizException("评论不能超过 1000 字");
        }
        if (sensitiveWords.check(text) == SensitiveWordService.Result.BLOCK) {
            throw new BizException("评论包含不允许发布的内容");
        }
        CommentEntity comment = new CommentEntity();
        comment.setArticleId(articleId);
        comment.setUserId(userId);
        comment.setContent(text);
        comment.setState("PENDING");
        return repository.save(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentEntity> approvedOf(Long articleId) {
        return repository.findByArticleIdAndStateOrderByCreatedAtAsc(articleId, "APPROVED");
    }

    @Transactional(readOnly = true)
    public List<CommentEntity> pending() {
        return repository.findByStateOrderByCreatedAtAsc("PENDING");
    }

    @Transactional(readOnly = true)
    public List<CommentEntity> moderationQueue() {
        return repository.findByStateInOrderByCreatedAtDesc(List.of("PENDING", "APPROVED"));
    }

    @Transactional
    public void approve(Long commentId, Long reviewerId) {
        CommentEntity comment = requirePending(commentId);
        if (sensitiveWords.check(comment.getContent()) == SensitiveWordService.Result.BLOCK) {
            throw new BizException("评论当前命中阻塞词，不能通过审核");
        }
        comment.setState("APPROVED");
        comment.setReviewedBy(reviewerId);
        comment.setReviewedAt(LocalDateTime.now());
        repository.save(comment);
        audit.record("COMMENT_APPROVE", "COMMENT", commentId, "PENDING", "APPROVED");
    }

    @Transactional
    public void reject(Long commentId, Long reviewerId) {
        CommentEntity comment = requirePending(commentId);
        comment.setState("REJECTED");
        comment.setReviewedBy(reviewerId);
        comment.setReviewedAt(LocalDateTime.now());
        repository.save(comment);
        audit.record("COMMENT_REJECT", "COMMENT", commentId, "PENDING", "REJECTED");
    }

    @Transactional
    public void revoke(Long commentId, Long reviewerId) {
        CommentEntity comment = repository.findById(commentId)
                .orElseThrow(() -> new BizException("评论不存在"));
        if (!"APPROVED".equals(comment.getState())) {
            throw new BizException("只有已通过评论才能下架");
        }
        comment.setState("REVOKED");
        comment.setReviewedBy(reviewerId);
        comment.setReviewedAt(LocalDateTime.now());
        repository.save(comment);
        audit.record("COMMENT_REVOKE", "COMMENT", commentId, "APPROVED", "REVOKED");
    }

    private CommentEntity requirePending(Long commentId) {
        CommentEntity comment = repository.findById(commentId)
                .orElseThrow(() -> new BizException("评论不存在"));
        if (!"PENDING".equals(comment.getState())) {
            throw new BizException("这条评论已经处理过了");
        }
        return comment;
    }
}

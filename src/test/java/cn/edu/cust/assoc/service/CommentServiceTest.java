package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.common.BizException;
import cn.edu.cust.assoc.domain.CommentEntity;
import cn.edu.cust.assoc.repository.ArticleRepository;
import cn.edu.cust.assoc.repository.CommentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentServiceTest {

    private final CommentRepository repository = mock(CommentRepository.class);
    private final ArticleRepository articleRepository = mock(ArticleRepository.class);
    private final SensitiveWordService sensitiveWords = mock(SensitiveWordService.class);
    private final AuditService audit = mock(AuditService.class);
    private final CommentService service =
            new CommentService(repository, articleRepository, sensitiveWords, audit);

    @Test
    @DisplayName("词库更新后，待审评论在通过时必须重新检查阻塞词")
    void approveRechecksCurrentSensitiveWords() {
        CommentEntity comment = comment("PENDING");
        when(repository.findById(1L)).thenReturn(Optional.of(comment));
        when(sensitiveWords.check("内容")).thenReturn(SensitiveWordService.Result.BLOCK);

        assertThatThrownBy(() -> service.approve(1L, 9L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("阻塞词");
        assertThat(comment.getState()).isEqualTo("PENDING");
        verify(repository, never()).save(comment);
    }

    @Test
    @DisplayName("已通过评论可以下架并写审计")
    void approvedCommentCanBeRevoked() {
        CommentEntity comment = comment("APPROVED");
        when(repository.findById(1L)).thenReturn(Optional.of(comment));

        service.revoke(1L, 9L);

        assertThat(comment.getState()).isEqualTo("REVOKED");
        assertThat(comment.getReviewedBy()).isEqualTo(9L);
        verify(repository).save(comment);
        verify(audit).record("COMMENT_REVOKE", "COMMENT", 1L, "APPROVED", "REVOKED");
    }

    private CommentEntity comment(String state) {
        CommentEntity comment = new CommentEntity();
        comment.setArticleId(2L);
        comment.setUserId(3L);
        comment.setContent("内容");
        comment.setState(state);
        return comment;
    }
}

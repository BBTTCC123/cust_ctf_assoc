package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.common.BizException;
import cn.edu.cust.assoc.domain.UserEntity;
import cn.edu.cust.assoc.repository.ArticleRepository;
import cn.edu.cust.assoc.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledIf("cn.edu.cust.assoc.support.TestDatabase#isAvailable")
class ContentWorkflowIT extends IntegrationTestBase {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ArticleRepository articleRepository;

    @Test
    void invalidCoverRollsBackArticleCreation() {
        UserEntity author = createActiveUser("contentatomic", "ItPass!2026abc");
        String title = uniqueName("atomic");

        assertThatThrownBy(() -> articleService.create(author.getId(), title, null,
                "正文", false, "INTERNAL", List.of(), 999_999_999L, false))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("媒体不存在");

        assertThat(articleRepository.findAll())
                .noneMatch(article -> title.equals(article.getTitle()));
    }
}

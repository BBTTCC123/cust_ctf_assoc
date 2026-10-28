package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.ArticleTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface ArticleTagRepository extends JpaRepository<ArticleTagEntity, Long> {

    List<ArticleTagEntity> findByArticleId(Long articleId);

    List<ArticleTagEntity> findByArticleIdIn(Collection<Long> articleIds);

    void deleteByArticleId(Long articleId);

    @Query("select distinct t.tag from ArticleTagEntity t order by t.tag")
    List<String> distinctTags();
}

package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.ArticleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleRepository extends JpaRepository<ArticleEntity, Long> {

    List<ArticleEntity> findByStateOrderByCreatedAtAsc(String state);

    List<ArticleEntity> findByStateAndVisibilityOrderByPublishedAtDesc(String state, String visibility);

    List<ArticleEntity> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    List<ArticleEntity> findByStateAndVisibilityAndIdNotOrderByPublishedAtDesc(String state, String visibility, Long id);
}

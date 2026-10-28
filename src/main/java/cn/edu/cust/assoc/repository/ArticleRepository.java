package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.ArticleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ArticleRepository extends JpaRepository<ArticleEntity, Long> {

    List<ArticleEntity> findByStateOrderByCreatedAtAsc(String state);

    Page<ArticleEntity> findByStateOrderByCreatedAtAsc(String state, Pageable pageable);

    List<ArticleEntity> findByStateAndVisibilityOrderByPublishedAtDesc(String state, String visibility);

    Page<ArticleEntity> findByStateAndVisibility(String state, String visibility, Pageable pageable);

    List<ArticleEntity> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    List<ArticleEntity> findByStateAndVisibilityAndIdNotOrderByPublishedAtDesc(String state, String visibility, Long id);

    List<ArticleEntity> findByStateOrderByPublishedAtDesc(String state);

    /**
     * 技术空间列表：关键词检索标题/摘要/正文，按标签筛选，分页。
     * member=true 时包含站内文章；false 时只返回公开文章。
     */
    @Query("""
            select distinct a from ArticleEntity a
            where a.state = 'PUBLISHED'
              and (:member = true or a.visibility = 'PUBLIC')
              and (:q is null or lower(a.title) like lower(concat('%', :q, '%'))
                   or lower(coalesce(a.summary, '')) like lower(concat('%', :q, '%'))
                   or lower(a.contentMd) like lower(concat('%', :q, '%')))
              and (:tag is null or exists (select 1 from ArticleTagEntity t where t.articleId = a.id and t.tag = :tag))
            order by a.publishedAt desc, a.id desc
            """)
    Page<ArticleEntity> searchPublished(@Param("member") boolean member,
                                        @Param("q") String q,
                                        @Param("tag") String tag,
                                        Pageable pageable);
}

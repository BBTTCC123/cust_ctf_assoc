package cn.edu.cust.assoc.domain;

import jakarta.persistence.*;

/** 文章标签。 */
@Entity
@Table(name = "article_tag")
public class ArticleTagEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Column(nullable = false, length = 32)
    private String tag;

    public Long getId() { return id; }
    public Long getArticleId() { return articleId; }
    public void setArticleId(Long articleId) { this.articleId = articleId; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
}

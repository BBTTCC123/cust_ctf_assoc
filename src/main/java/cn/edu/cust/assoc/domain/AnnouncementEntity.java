package cn.edu.cust.assoc.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "announcement")
public class AnnouncementEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(name = "content_md", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String contentMd;
    @Column(name = "content_html", columnDefinition = "MEDIUMTEXT")
    private String contentHtml;
    @Column(name = "author_id", nullable = false)
    private Long authorId;
    @Column(name = "published_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime publishedAt;
    @Column(nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean pinned;

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContentMd() { return contentMd; }
    public void setContentMd(String contentMd) { this.contentMd = contentMd; }
    public String getContentHtml() { return contentHtml; }
    public void setContentHtml(String contentHtml) { this.contentHtml = contentHtml; }
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public Boolean getPinned() { return pinned; }
    public void setPinned(Boolean pinned) { this.pinned = pinned; }
}

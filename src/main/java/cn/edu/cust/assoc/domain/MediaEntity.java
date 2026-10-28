package cn.edu.cust.assoc.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "media")
public class MediaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "stored_key", nullable = false, length = 300)
    private String storedKey;

    @Column(name = "thumb_key", length = 300)
    private String thumbKey;

    @Column(nullable = false, length = 64)
    private String mime;

    @Column(nullable = false)
    private Long bytes;

    // 库里是 CHAR(64)，不写 columnDefinition 的话 Hibernate 校验会拿 VARCHAR 比而启动失败
    @Column(nullable = false, length = 64, columnDefinition = "char(64)")
    private String sha256;

    @Column(nullable = false, length = 16)
    private String zone;

    @Column(nullable = false, length = 16)
    private String visibility;

    @Column(name = "review_state", nullable = false, length = 16)
    private String reviewState;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean isPubliclyViewable() {
        return "APPROVED".equals(reviewState) && "PUBLIC".equals(visibility) && "PUBLIC".equals(zone);
    }

    public Long getId() { return id; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getStoredKey() { return storedKey; }
    public void setStoredKey(String storedKey) { this.storedKey = storedKey; }
    public String getThumbKey() { return thumbKey; }
    public void setThumbKey(String thumbKey) { this.thumbKey = thumbKey; }
    public String getMime() { return mime; }
    public void setMime(String mime) { this.mime = mime; }
    public Long getBytes() { return bytes; }
    public void setBytes(Long bytes) { this.bytes = bytes; }
    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }
    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public String getReviewState() { return reviewState; }
    public void setReviewState(String reviewState) { this.reviewState = reviewState; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
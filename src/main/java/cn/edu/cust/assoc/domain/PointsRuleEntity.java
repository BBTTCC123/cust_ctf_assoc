package cn.edu.cust.assoc.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 积分规则版本。按 occurredOn 选择生效版本，历史流水不受新规则影响。 */
@Entity
@Table(name = "points_rule")
public class PointsRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false, length = 16)
    private String direction;

    @Column(name = "item_key", nullable = false, length = 64)
    private String itemKey;

    @Column(nullable = false)
    private Integer points;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    public Long getId() { return id; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public String getItemKey() { return itemKey; }
    public void setItemKey(String itemKey) { this.itemKey = itemKey; }
    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
}
